package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.MetaColaborador
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
 * Handler especializado para sincronização de Metas de Colaborador.
 */
class MetaColaboradorSyncHandler @javax.inject.Inject constructor(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = "meta_colaborador"

    companion object {
        private const val COLLECTION_META_COLABORADOR = "meta_colaborador"
        private const val FIELD_ROTA_ID = "rotaId"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando pull de meta colaborador...")
            val collectionRef = getCollectionReference(COLLECTION_META_COLABORADOR)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            if (canUseIncremental) {
                Timber.tag(TAG).d("?? Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) return incrementalResult
            }

            pullComplete(collectionRef, startTime, timestampOverride)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull de meta colaborador: ${e.message}", e)
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
            Timber.tag(TAG).d("?? Pull COMPLETO de meta colaborador - documentos recebidos: ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val metasCache = appRepository.obterTodasMetaColaborador().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, metasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, error = if (errorCount > 0) "$errorCount erros" else null, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull completo de meta colaborador: ${e.message}", e)
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
            val documents = fetchDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID, lastSyncTimestamp)
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val metasCache = appRepository.obterTodasMetaColaborador().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, metasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, error = if (errorCount > 0) "$errorCount erros" else null, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull incremental de meta colaborador: ${e.message}", e)
            null
        }
    }

    private suspend fun processDocuments(
        documents: List<DocumentSnapshot>,
        metasCache: MutableMap<Long, MetaColaborador>
    ): Triple<Int, Int, Int> {
        var syncCount = 0
        var skippedCount = 0
        var errorCount = 0
        
        documents.forEach { doc ->
            try {
                val data = doc.data ?: return@forEach
                val metaId = doc.id.toLongOrNull()
                    ?: (data["roomId"] as? Number)?.toLong()
                    ?: (data["id"] as? Number)?.toLong()
                    ?: return@forEach
                
                val json = gson.toJson(data)
                val metaFirestore = gson.fromJson(json, MetaColaborador::class.java)?.copy(id = metaId) ?: return@forEach
                
                if (metaFirestore.rotaId != null && !shouldSyncRouteData(metaFirestore.rotaId, allowUnknown = false)) {
                    skippedCount++
                    return@forEach
                }
                
                val localMeta = metasCache[metaId]
                val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                    ?: converterTimestampParaDate(data["dataCriacao"])?.time ?: 0L
                val localTimestamp = localMeta?.dataCriacao?.time ?: 0L
                
                if (localMeta == null || serverTimestamp > (localTimestamp + 1000)) {
                    if (localMeta == null) {
                        appRepository.inserirMeta(metaFirestore)
                    } else {
                        appRepository.atualizarMeta(metaFirestore)
                    }
                    metasCache[metaId] = metaFirestore
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
            Timber.tag(TAG).d("?? Iniciando push INCREMENTAL de meta colaborador...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val metasLocais = appRepository.obterTodasMetaColaborador().first()
            
            val paraEnviar = if (canUseIncremental) {
                metasLocais.filter { it.dataCriacao.time > lastPushTimestamp }
            } else {
                metasLocais
            }
            
            if (paraEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            val collectionRef = getCollectionReference(COLLECTION_META_COLABORADOR)
            
            paraEnviar.forEach { meta ->
                try {
                    val map = entityToMap(meta)
                    map["roomId"] = meta.id
                    map["id"] = meta.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    map["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    collectionRef.document(meta.id.toString()).set(map).await()
                    
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
            Timber.tag(TAG).e("? Erro no push de meta colaborador: ${e.message}", e)
            Result.failure(e)
        }
    }
}
