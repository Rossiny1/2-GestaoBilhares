package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.Date
import com.example.gestaobilhares.core.utils.FirebaseImageUploader

/**
 * Handler especializado para sincronização de Ciclos de Acerto.
 */
class CicloSyncHandler @javax.inject.Inject constructor(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = "ciclos"

    companion object {
        private const val COLLECTION_CICLOS = "ciclos"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando pull de ciclos...")
            val collectionRef = getCollectionReference(COLLECTION_CICLOS)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            if (canUseIncremental) {
                Timber.tag(TAG).d("?? Tentando sincronização INCREMENTAL de ciclos (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosCiclos().first().size }.getOrDefault(0)
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Timber.tag(TAG).w("?? Incremental de ciclos trouxe $syncedCount registros e base local possui $localCount - executando pull COMPLETO")
                } else {
                    Timber.tag(TAG).w("?? Sincronização incremental de ciclos falhou, usando método COMPLETO")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronização de ciclos - usando método COMPLETO")
            }

            pullComplete(collectionRef, startTime, timestampOverride)
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Pull de ciclos cancelado")
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no pull de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullComplete(
        collectionRef: CollectionReference,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Timber.tag(TAG).d("?? Pull COMPLETO de ciclos - documentos recebidos: ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val ciclosCache = runCatching { appRepository.obterTodosCiclos().first() }.getOrDefault(emptyList())
                .associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, ciclosCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Result.success(syncCount)
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Pull completo de ciclos cancelado")
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no pull completo de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullIncremental(
        collectionRef: CollectionReference,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int>? {
        return try {
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Timber.tag(TAG).w("?? Falha ao executar query incremental para ciclos: ${e.message}")
                return null
            }
            Timber.tag(TAG).d("?? Ciclos - incremental retornou ${documents.size} documentos (após filtro de rota)")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val ciclosCache = runCatching { appRepository.obterTodosCiclos().first() }.getOrDefault(emptyList())
                .associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, ciclosCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Result.success(syncCount)
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Pull incremental de ciclos cancelado")
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no pull incremental de ciclos: ${e.message}", e)
            null
        }
    }

    private suspend fun processDocuments(
        documents: List<DocumentSnapshot>,
        ciclosCache: MutableMap<Long, CicloAcertoEntity>
    ): Triple<Int, Int, Int> {
        var syncCount = 0
        var skippedCount = 0
        var errorCount = 0
        
        documents.forEach { doc ->
            try {
                val cicloData = doc.data ?: run {
                    skippedCount++
                    return@forEach
                }
                val cicloId = doc.id.toLongOrNull()
                    ?: (cicloData["roomId"] as? Number)?.toLong()
                    ?: (cicloData["id"] as? Number)?.toLong()
                    ?: run {
                        skippedCount++
                        return@forEach
                    }
                
                val cicloJson = gson.toJson(cicloData)
                val cicloFirestore = gson.fromJson(cicloJson, CicloAcertoEntity::class.java)?.copy(id = cicloId)
                    ?: run {
                        errorCount++
                        return@forEach
                    }
                
                if (!shouldSyncRouteData(cicloFirestore.rotaId, allowUnknown = false)) {
                    skippedCount++
                    return@forEach
                }
                
                val serverTimestamp = com.example.gestaobilhares.core.utils.DateUtils.convertToLong(cicloData["last_modified"])
                    ?: com.example.gestaobilhares.core.utils.DateUtils.convertToLong(cicloData["lastModified"])
                    ?: com.example.gestaobilhares.core.utils.DateUtils.convertToLong(cicloData["dataAtualizacao"])
                    ?: cicloFirestore.dataAtualizacao
                val localTimestamp = ciclosCache[cicloId]?.dataAtualizacao ?: 0L
                
                if (ciclosCache[cicloId] == null || serverTimestamp > localTimestamp) {
                    appRepository.inserirCicloAcerto(cicloFirestore)
                    ciclosCache[cicloId] = cicloFirestore
                    syncCount++
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("Erro ao processar ciclo ${doc.id}: ${e.message}", e)
                errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    override suspend fun push(): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando push INCREMENTAL de ciclos...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val ciclosLocais = appRepository.obterTodosCiclos().first()
            
            val ciclosParaEnviar = if (canUseIncremental) {
                ciclosLocais.filter { it.dataAtualizacao > lastPushTimestamp }
            } else {
                ciclosLocais
            }
            
            if (ciclosParaEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            val collectionRef = getCollectionReference(COLLECTION_CICLOS)
            
            ciclosParaEnviar.forEach { ciclo ->
                try {
                    val cicloMap = entityToMap(ciclo)
                    cicloMap["roomId"] = ciclo.id
                    cicloMap["id"] = ciclo.id
                    cicloMap["lastModified"] = FieldValue.serverTimestamp()
                    cicloMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    collectionRef.document(ciclo.id.toString()).set(cicloMap).await()
                    
                    syncCount++
                    bytesUploaded += cicloMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao enviar ciclo ${ciclo.id}: ${e.message}")
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Result.success(syncCount)
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Push de ciclos cancelado")
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no push de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }
}
