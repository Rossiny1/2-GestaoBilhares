package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.ColaboradorRota
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
import timber.log.Timber
import java.util.Date
import com.example.gestaobilhares.utils.FirebaseImageUploader

/**
 * Handler especializado para sincronização de Vinculações Colaborador-Rota.
 */
class ColaboradorRotaSyncHandler(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader) {

    override val entityType: String = "colaborador_rota"

    companion object {
        private const val COLLECTION_COLABORADOR_ROTA = "colaborador_rota"
        private const val FIELD_ROTA_ID = "rotaId"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando pull de colaborador rotas...")
            val collectionRef = getCollectionReference(COLLECTION_COLABORADOR_ROTA)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            if (canUseIncremental) {
                Timber.tag(TAG).d("?? Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) return incrementalResult
            }

            pullComplete(collectionRef, startTime, timestampOverride)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull de colaborador rotas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchColaboradorRotaDocuments(
        collectionRef: CollectionReference,
        lastSyncTimestamp: Long = 0L
    ): List<DocumentSnapshot> {
        return if (userSessionManager.isAdmin()) {
            // Admin busca tudo
            if (lastSyncTimestamp > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get().await().documents
            } else {
                collectionRef.get().await().documents
            }
        } else {
            // Para USER, pull apenas os VÍNCULOS dele
            // ✅ CORREÇÃO: Isso quebra a dependência circular onde precisávamos de RotaIDs para puxar vínculos
            val userId = userSessionManager.getCurrentUserId()
            var query = collectionRef.whereEqualTo("colaboradorId", userId)
            if (lastSyncTimestamp > 0L) {
                query = query.whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            }
            query.get().await().documents
        }
    }

    private suspend fun pullComplete(
        collectionRef: CollectionReference,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int> {
        return try {
            val documents = fetchColaboradorRotaDocuments(collectionRef)
            Timber.tag(TAG).d("?? Pull COMPLETO de colaborador rotas - documentos recebidos: ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val vinculosCache = appRepository.obterTodosColaboradorRotas().associateBy { "${it.colaboradorId}_${it.rotaId}" }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, vinculosCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, error = if (errorCount > 0) "$errorCount erros" else null, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull completo de colaborador rotas: ${e.message}", e)
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
            val documents = fetchColaboradorRotaDocuments(collectionRef, lastSyncTimestamp)
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val vinculosCache = appRepository.obterTodosColaboradorRotas().associateBy { "${it.colaboradorId}_${it.rotaId}" }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, vinculosCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, error = if (errorCount > 0) "$errorCount erros" else null, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull incremental de colaborador rotas: ${e.message}", e)
            null
        }
    }

    private suspend fun processDocuments(
        documents: List<DocumentSnapshot>,
        vinculosCache: MutableMap<String, ColaboradorRota>
    ): Triple<Int, Int, Int> {
        var syncCount = 0
        var skippedCount = 0
        var errorCount = 0
        
        documents.forEach { doc ->
            try {
                val data = doc.data ?: return@forEach
                val colabId = (data["colaboradorId"] as? Number)?.toLong() ?: return@forEach
                val rotaId = (data["rotaId"] as? Number)?.toLong() ?: return@forEach
                
                val json = gson.toJson(data)
                val vinculoFirestore = gson.fromJson(json, ColaboradorRota::class.java) ?: return@forEach
                
                val key = "${colabId}_${rotaId}"
                val localVinculo = vinculosCache[key]
                val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                    ?: converterTimestampParaDate(data["dataVinculacao"])?.time ?: 0L
                val localTimestamp = localVinculo?.dataVinculacao?.time ?: 0L
                
                if (localVinculo == null || serverTimestamp > localTimestamp) {
                    appRepository.vincularColaboradorRota(
                        colabId, 
                        rotaId, 
                        vinculoFirestore.responsavelPrincipal, 
                        vinculoFirestore.dataVinculacao
                    )
                    vinculosCache[key] = vinculoFirestore
                    syncCount++
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    override suspend fun push(): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando push INCREMENTAL de colaborador rotas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val vinculosLocais = appRepository.obterTodosColaboradorRotas()
            
            val paraEnviar = if (canUseIncremental) {
                vinculosLocais.filter { it.dataVinculacao.time > lastPushTimestamp }
            } else {
                vinculosLocais
            }
            
            if (paraEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            val collectionRef = getCollectionReference(COLLECTION_COLABORADOR_ROTA)
            
            paraEnviar.forEach { vinculo ->
                try {
                    val map = entityToMap(vinculo)
                    map["lastModified"] = FieldValue.serverTimestamp()
                    map["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    collectionRef.document("${vinculo.colaboradorId}_${vinculo.rotaId}").set(map).await()
                    
                    syncCount++
                    bytesUploaded += map.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no push de colaborador rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
}
