package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.Meta
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
 * Handler especializado para sincronização de Metas Gerais (Rotas/Ciclos).
 */
class MetaSyncHandler(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader) {

    override val entityType: String = "metas"

    companion object {
        private const val COLLECTION_METAS = "metas"
        private const val FIELD_ROTA_ID = "rotaId"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando pull de metas...")
            val collectionRef = getCollectionReference(COLLECTION_METAS)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            if (canUseIncremental) {
                Timber.tag(TAG).d("?? Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) return incrementalResult
            }

            pullComplete(collectionRef, startTime, timestampOverride)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull de metas: ${e.message}", e)
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
            Timber.tag(TAG).d("?? Pull COMPLETO de metas - documentos recebidos: ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val metasCache = mutableMapOf<Long, Meta>()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, metasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, error = if (errorCount > 0) "$errorCount erros" else null, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull completo de metas: ${e.message}", e)
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
            
            val metasCache = mutableMapOf<Long, Meta>()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, metasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, error = if (errorCount > 0) "$errorCount erros" else null, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull incremental de metas: ${e.message}", e)
            null
        }
    }

    private suspend fun processDocuments(
        documents: List<DocumentSnapshot>,
        metasCache: MutableMap<Long, Meta>
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
                val metaFirestore = gson.fromJson(json, Meta::class.java)?.copy(id = metaId) ?: return@forEach
                
                if (!shouldSyncRouteData(metaFirestore.rotaId, allowUnknown = false)) {
                    skippedCount++
                    return@forEach
                }
                
                val localMeta = appRepository.obterMetaPorId(metaId)
                val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                    ?: converterTimestampParaDate(data["dataInicio"])?.time ?: 0L
                val localTimestamp = localMeta?.dataInicio?.time ?: 0L
                
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
        // Metas gerais são normalmente pushadas via painel admin, mas mantemos o contrato
        return Result.success(0)
    }
}
