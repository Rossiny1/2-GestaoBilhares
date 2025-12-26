package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.Rota
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
 * Handler especializado para sincronização de Rotas.
 */
class RotaSyncHandler(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: FirebaseImageUploader
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader) {

    override val entityType: String = "rotas"
    
    // Flag para permitir bootstrap (sincronizar todas as rotas se não houver nenhuma atribuída localmente)
    var allowRouteBootstrap: Boolean = false

    companion object {
        private const val COLLECTION_ROTAS = "rotas"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando pull de rotas...")
            val collectionRef = getCollectionReference(COLLECTION_ROTAS)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            Timber.tag(TAG).d("?? Rotas: lastSyncTimestamp=$lastSyncTimestamp, canUseIncremental=$canUseIncremental, allowRouteBootstrap=$allowRouteBootstrap")

            if (canUseIncremental) {
                Timber.tag(TAG).d("?? Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasRotas().first().size }.getOrDefault(0)
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Timber.tag(TAG).w("?? Rotas: incremental trouxe $syncedCount registros com base local $localCount - executando pull completo")
                } else {
                    Timber.tag(TAG).w("?? Sincronização incremental de rotas falhou, usando método COMPLETO")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronização de rotas - usando método COMPLETO")
            }

            pullComplete(collectionRef, startTime, timestampOverride)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull de rotas: ${e.message}", e)
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
            Timber.tag(TAG).d("?? Pull COMPLETO de rotas - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val rotasCache = appRepository.obterTodasRotas().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(snapshot.documents, rotasCache)
            
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
            Timber.tag(TAG).e("? Erro no pull completo de rotas: ${e.message}", e)
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
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                Timber.tag(TAG).w("?? Falha ao criar query incremental para rotas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val rotasCache = appRepository.obterTodasRotas().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processDocuments(documents, rotasCache)
            
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
            Timber.tag(TAG).e("? Erro no pull incremental de rotas: ${e.message}", e)
            null
        }
    }

    private suspend fun processDocuments(
        documents: List<DocumentSnapshot>,
        rotasCache: MutableMap<Long, Rota>
    ): Triple<Int, Int, Int> {
        var syncCount = 0
        var skippedCount = 0
        var errorCount = 0
        
        documents.forEach { doc ->
            try {
                val rotaData = doc.data ?: run {
                    skippedCount++
                    return@forEach
                }
                val nome = (rotaData["nome"] as? String)?.takeIf { it.isNotBlank() } ?: run {
                    skippedCount++
                    return@forEach
                }
                
                val roomId = (rotaData["roomId"] as? Number)?.toLong()
                    ?: (rotaData["id"] as? Number)?.toLong()
                    ?: doc.id.toLongOrNull()
                
                // ? CORREÇÃO: Durante bootstrap, permitir todas as rotas temporariamente
                if (roomId != null && !allowRouteBootstrap && !shouldSyncRouteData(roomId, allowUnknown = false)) {
                    Timber.tag(TAG).d("?? Rota ignorada por falta de acesso: ID=$roomId")
                    skippedCount++
                    return@forEach
                }
                
                val dataCriacaoLong = converterTimestampParaDate(rotaData["dataCriacao"])?.time 
                    ?: converterTimestampParaDate(rotaData["data_criacao"])?.time
                    ?: System.currentTimeMillis()
                val dataAtualizacaoLong = converterTimestampParaDate(rotaData["dataAtualizacao"])?.time
                    ?: converterTimestampParaDate(rotaData["data_atualizacao"])?.time
                    ?: converterTimestampParaDate(rotaData["lastModified"])?.time
                    ?: System.currentTimeMillis()
                
                if (roomId == null) {
                    Timber.tag(TAG).w("?? Rota ${doc.id} sem roomId válido - criando registro local")
                    val rotaNova = Rota(
                        nome = nome,
                        descricao = rotaData["descricao"] as? String ?: "",
                        colaboradorResponsavel = rotaData["colaboradorResponsavel"] as? String
                            ?: rotaData["colaborador_responsavel"] as? String ?: "Não definido",
                        cidades = rotaData["cidades"] as? String ?: "Não definido",
                        ativa = rotaData["ativa"] as? Boolean ?: true,
                        cor = rotaData["cor"] as? String ?: "#6200EA",
                        dataCriacao = dataCriacaoLong,
                        dataAtualizacao = dataAtualizacaoLong
                    )
                    val insertedId = appRepository.inserirRota(rotaNova)
                    rotasCache[insertedId] = rotaNova.copy(id = insertedId)
                    syncCount++
                } else {
                    val rotaFirestore = Rota(
                        id = roomId,
                        nome = nome,
                        descricao = rotaData["descricao"] as? String ?: "",
                        colaboradorResponsavel = rotaData["colaboradorResponsavel"] as? String
                            ?: rotaData["colaborador_responsavel"] as? String ?: "Não definido",
                        cidades = rotaData["cidades"] as? String ?: "Não definido",
                        ativa = rotaData["ativa"] as? Boolean ?: true,
                        cor = rotaData["cor"] as? String ?: "#6200EA",
                        dataCriacao = dataCriacaoLong,
                        dataAtualizacao = dataAtualizacaoLong
                    )
                    
                    val localRota = rotasCache[roomId]
                    val localTimestamp = localRota?.dataAtualizacao ?: 0L
                    val serverTimestamp = rotaFirestore.dataAtualizacao
                    
                    if (localRota == null || serverTimestamp > localTimestamp) {
                        appRepository.inserirRota(rotaFirestore)
                        rotasCache[roomId] = rotaFirestore
                        syncCount++
                    } else {
                        skippedCount++
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("? Erro ao processar rota ${doc.id}: ${e.message}", e)
                errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    override suspend fun push(): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("?? Iniciando push INCREMENTAL de rotas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val rotasLocais = appRepository.obterTodasRotas().first()
            
            val rotasParaEnviar = if (canUseIncremental) {
                rotasLocais.filter { it.dataAtualizacao > lastPushTimestamp }
            } else {
                rotasLocais
            }
            
            if (rotasParaEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            rotasParaEnviar.forEach { rota ->
                try {
                    val rotaMap = entityToMap(rota)
                    rotaMap["roomId"] = rota.id
                    rotaMap["id"] = rota.id
                    rotaMap["lastModified"] = FieldValue.serverTimestamp()
                    rotaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(COLLECTION_ROTAS)
                    collectionRef.document(rota.id.toString()).set(rotaMap).await()
                    
                    syncCount++
                    bytesUploaded += rotaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao enviar rota ${rota.id}: ${e.message}")
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no push de rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
}
