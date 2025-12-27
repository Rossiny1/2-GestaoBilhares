package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.Colaborador
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
import com.example.gestaobilhares.core.utils.FirebaseImageUploader

/**
 * Handler especializado para sincronização de Colaboradores.
 */
class ColaboradorSyncHandler @javax.inject.Inject constructor(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = "colaboradores"

    companion object {
        private const val COLLECTION_COLABORADORES = "colaboradores"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando pull de colaboradores...")
            val collectionRef = getCollectionReference(COLLECTION_COLABORADORES)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            if (canUseIncremental) {
                Timber.tag(TAG).d("?? Tentando sincronização INCREMENTAL de colaboradores (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    return incrementalResult
                }
                Timber.tag(TAG).w("?? Sincronização incremental de colaboradores falhou, usando método COMPLETO")
            } else {
                Timber.tag(TAG).d("?? Primeira sincronização de colaboradores - usando método COMPLETO")
            }

            pullComplete(collectionRef, startTime, timestampOverride)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull de colaboradores: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullComplete(
        collectionRef: CollectionReference,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            val documents = snapshot.documents
            Timber.tag(TAG).d("?? Pull COMPLETO de colaboradores - documentos recebidos: ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val colaboradoresCache = appRepository.obterTodosColaboradores().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, colaboradoresCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull completo de colaboradores: ${e.message}", e)
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
            val documents = collectionRef
                .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                .get()
                .await()
                .documents
            
            Timber.tag(TAG).d("?? Colaboradores - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val colaboradoresCache = appRepository.obterTodosColaboradores().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, colaboradoresCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull incremental de colaboradores: ${e.message}", e)
            null
        }
    }

    private suspend fun processDocuments(
        documents: List<DocumentSnapshot>,
        colaboradoresCache: MutableMap<Long, Colaborador>
    ): Triple<Int, Int, Int> {
        var syncCount = 0
        var skippedCount = 0
        var errorCount = 0
        
        documents.forEach { doc ->
            try {
                val result = processColaboradorDocument(doc, colaboradoresCache)
                when (result) {
                    ProcessResult.Synced -> syncCount++
                    ProcessResult.Skipped -> skippedCount++
                    ProcessResult.Error -> errorCount++
                }
            } catch (e: Exception) {
                errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processColaboradorDocument(
        doc: DocumentSnapshot,
        colaboradoresCache: MutableMap<Long, Colaborador>
    ): ProcessResult {
        val colaboradorData = doc.data ?: return ProcessResult.Skipped
        
        val colaboradorEmail = (colaboradorData["email"] as? String) ?: ""
        val docIdIsNumeric = doc.id.toLongOrNull() != null
        
        val colaboradorId = if (!docIdIsNumeric && colaboradorEmail.isNotEmpty()) {
            val colaboradorExistente = appRepository.obterColaboradorPorEmail(colaboradorEmail)
            if (colaboradorExistente != null) {
                colaboradorExistente.id
            } else {
                val todosColaboradores = appRepository.obterTodosColaboradores().first()
                (todosColaboradores.maxOfOrNull { it.id } ?: 0L) + 1L
            }
        } else {
            doc.id.toLongOrNull()
                ?: (colaboradorData["roomId"] as? Number)?.toLong()
                ?: (colaboradorData["id"] as? Number)?.toLong()
                ?: run {
                    if (colaboradorEmail.isNotEmpty()) {
                        val colaboradorExistente = appRepository.obterColaboradorPorEmail(colaboradorEmail)
                        if (colaboradorExistente != null) return@run colaboradorExistente.id
                    }
                    return@run -1L
                }
        }
        
        if (colaboradorId <= 0L) return ProcessResult.Skipped
        
        val colaboradorJson = gson.toJson(colaboradorData)
        val colaboradorFirestore = gson.fromJson(colaboradorJson, Colaborador::class.java)?.copy(id = colaboradorId)
            ?: return ProcessResult.Error
        
        val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
            ?: (colaboradorData["dataUltimaAtualizacao"] as? Timestamp)?.toDate()?.time
            ?: colaboradorFirestore.dataUltimaAtualizacao.time
        
        val localColaborador = colaboradoresCache[colaboradorId]
        val localTimestamp = localColaborador?.dataUltimaAtualizacao?.time ?: 0L
        
        return if (localColaborador == null || serverTimestamp > localTimestamp) {
            val colaboradorParaSalvar = if (localColaborador?.aprovado == true && !colaboradorFirestore.aprovado) {
                colaboradorFirestore.copy(
                    aprovado = true,
                    dataAprovacao = localColaborador.dataAprovacao,
                    aprovadoPor = localColaborador.aprovadoPor
                )
            } else {
                colaboradorFirestore
            }
            
            appRepository.atualizarColaborador(colaboradorParaSalvar)
            if (localColaborador == null) appRepository.inserirColaborador(colaboradorParaSalvar)
            
            colaboradoresCache[colaboradorId] = colaboradorParaSalvar
            ProcessResult.Synced
        } else {
            ProcessResult.Skipped
        }
    }

    override suspend fun push(): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando push INCREMENTAL de colaboradores...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val colaboradoresLocais = appRepository.obterTodosColaboradores().first()
            
            val paraEnviar = if (canUseIncremental) {
                colaboradoresLocais.filter { it.dataUltimaAtualizacao.time > lastPushTimestamp }
            } else {
                colaboradoresLocais
            }
            
            if (paraEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            val collectionRef = getCollectionReference(COLLECTION_COLABORADORES)
            
            paraEnviar.forEach { colab ->
                try {
                    var colabParaEnviar = colab
                    
                    // Se o colaborador tem foto e ela é um caminho local (não URL do Firebase), faz o upload
                    if (!colab.fotoPerfil.isNullOrBlank() && !firebaseImageUploader.isFirebaseStorageUrl(colab.fotoPerfil)) {
                        Timber.tag(TAG).d("?? Fazendo upload da foto local do colaborador: ${colab.id}")
                        val urlRemota = firebaseImageUploader.uploadColaboradorFoto(colab.fotoPerfil!!, colab.id)
                        if (urlRemota != null) {
                            colabParaEnviar = colab.copy(fotoPerfil = urlRemota)
                            // Atualiza localmente para evitar uploads repetidos
                            appRepository.atualizarColaborador(colabParaEnviar)
                        }
                    }

                    val colabMap = entityToMap(colabParaEnviar)
                    colabMap["roomId"] = colabParaEnviar.id
                    colabMap["id"] = colabParaEnviar.id
                    colabMap["lastModified"] = FieldValue.serverTimestamp()
                    colabMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    collectionRef.document(colabParaEnviar.id.toString()).set(colabMap).await()
                    
                    syncCount++
                    bytesUploaded += colabMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no push de colaboradores: ${e.message}", e)
            Result.failure(e)
        }
    }
}
