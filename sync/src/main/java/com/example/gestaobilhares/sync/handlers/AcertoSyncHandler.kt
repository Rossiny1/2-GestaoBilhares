package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.util.Date

/**
 * Handler especializado para sincronização de Acertos e AcertoMesas.
 */
class AcertoSyncHandler(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = COLLECTION_ACERTOS

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        if (!networkUtils.isConnected()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("Iniciando pull de acertos...")
            
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                Timber.tag(TAG).d("Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosAcertos().first().size }.getOrDefault(0)
                    
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Timber.tag(TAG).w("Incremental de acertos trouxe 0 registros com base local $localCount - executando pull completo")
                }
            }
            
            pullComplete(startTime, timestampOverride)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no pull de acertos")
            Result.failure(e)
        }
    }

    private suspend fun tryPullIncremental(
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int>? {
        return try {
            val collectionRef = getCollectionReference(COLLECTION_ACERTOS)
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Timber.tag(TAG).w("Erro ao executar query incremental de acertos: ${e.message}")
                return null
            }
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val syncCount = processDocuments(documents)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).w("Erro na sincronização incremental de acertos: ${e.message}")
            null
        }
    }

    private suspend fun pullComplete(startTime: Long, timestampOverride: Long?): Result<Int> {
        return try {
            val collectionRef = getCollectionReference(COLLECTION_ACERTOS)
            val snapshot = collectionRef.get().await()
            val documents = snapshot.documents
            
            val syncCount = processDocuments(documents)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no pull completo de acertos")
            Result.failure(e)
        }
    }

    private suspend fun processDocuments(documents: List<DocumentSnapshot>): Int {
        var syncCount = 0
        for (doc in documents) {
            try {
                val acerto = documentToAcerto(doc) ?: continue
                
                val rotaId = acerto.rotaId ?: getClienteRouteId(acerto.clienteId)
                if (!shouldSyncRouteData(rotaId, clienteId = acerto.clienteId, allowUnknown = false)) {
                    continue
                }
                
                // Validar FKs
                if (ensureEntityExists("cliente", acerto.clienteId) &&
                    ensureEntityExists("colaborador", acerto.colaboradorId ?: 0) &&
                    ensureEntityExists("rota", acerto.rotaId ?: 0)) {
                    
                    // Tenta atualizar primeiro, se não existir insere
                    val updated = appRepository.atualizarAcerto(acerto)
                    if (updated == 0) {
                        appRepository.inserirAcerto(acerto)
                    }
                    
                    syncCount++
                    pullAcertoMesas(acerto.id)
                } else {
                    Timber.tag(TAG).w("⏭️ Pulando acerto ${acerto.id} por falha na FK")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Erro ao processar acerto ${doc.id}")
            }
        }
        return syncCount
    }

    private fun documentToAcerto(doc: DocumentSnapshot): Acerto? {
        val data = doc.data?.toMutableMap() ?: return null
        val acertoId = doc.id.toLongOrNull() ?: (data["id"] as? Number)?.toLong() ?: return null
        
        val clienteId = extrairClienteId(data) ?: return null
        data["clienteId"] = clienteId
        data["cliente_id"] = clienteId
        
        // Converter campos de data para Long se chegarem como Timestamp ou Date do Firestore
        data["data_acerto"] = com.example.gestaobilhares.core.utils.DateUtils.convertToLong(data["data_acerto"]) ?: 
                            com.example.gestaobilhares.core.utils.DateUtils.convertToLong(data["dataAcerto"]) ?: System.currentTimeMillis()
        data["periodo_inicio"] = com.example.gestaobilhares.core.utils.DateUtils.convertToLong(data["periodo_inicio"]) ?: 0L
        data["periodo_fim"] = com.example.gestaobilhares.core.utils.DateUtils.convertToLong(data["periodo_fim"]) ?: 0L
        data["data_criacao"] = com.example.gestaobilhares.core.utils.DateUtils.convertToLong(data["data_criacao"]) ?: System.currentTimeMillis()
        data["data_finalizacao"] = com.example.gestaobilhares.core.utils.DateUtils.convertToLong(data["data_finalizacao"])
        
        val json = gson.toJson(data)
        return gson.fromJson(json, Acerto::class.java)?.copy(
            id = acertoId,
            clienteId = clienteId
        )
    }

    private suspend fun pullAcertoMesas(acertoId: Long) {
        try {
            val collectionRef = getCollectionReference(COLLECTION_ACERTO_MESAS)
            val snapshot = collectionRef
                .whereEqualTo("acertoId", acertoId)
                .get()
                .await()
            
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val json = gson.toJson(data)
                    var acertoMesa = gson.fromJson(json, AcertoMesa::class.java) ?: continue
                    
                    // Validar FKs
                    if (!ensureEntityExists("acerto", acertoMesa.acertoId) || !ensureEntityExists("mesa", acertoMesa.mesaId)) {
                        Timber.tag(TAG).w("⏭️ Pulando acerto_mesa ${doc.id} por falha na FK (acerto ou mesa não encontrados).")
                        continue
                    }

                    // Tratamento de foto
                    val remoteUrl = acertoMesa.fotoRelogioFinal
                    if (!remoteUrl.isNullOrEmpty() && firebaseImageUploader.isFirebaseStorageUrl(remoteUrl)) {
                        val existing = appRepository.buscarAcertoMesaPorAcertoEMesa(acertoMesa.acertoId, acertoMesa.mesaId)
                        val existingFoto = existing?.fotoRelogioFinal
                        
                        val localPath = if (!existingFoto.isNullOrEmpty() && 
                            !firebaseImageUploader.isFirebaseStorageUrl(existingFoto)) {
                            
                            val file = File(existingFoto)
                            if (file.exists()) {
                                existingFoto
                            } else {
                                firebaseImageUploader.downloadMesaRelogio(remoteUrl, acertoMesa.mesaId, acertoMesa.acertoId)
                            }
                        } else {
                            firebaseImageUploader.downloadMesaRelogio(remoteUrl, acertoMesa.mesaId, acertoMesa.acertoId)
                        }
                        
                        if (localPath != null) {
                            acertoMesa = acertoMesa.copy(fotoRelogioFinal = localPath)
                        }
                    }
                    
                    appRepository.inserirAcertoMesa(acertoMesa)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao sincronizar AcertoMesa ${doc.id}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no pull de AcertoMesas para acerto $acertoId")
        }
    }

    override suspend fun push(): Result<Int> {
        if (!networkUtils.isConnected()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        val startTime = System.currentTimeMillis()
        
        return try {
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val acertosLocais = appRepository.obterTodosAcertos().first()
            
            val paraEnviar = acertosLocais.filter { 
                it.dataAcerto > lastPushTimestamp || it.dataCriacao > lastPushTimestamp
            }
            
            if (paraEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            var syncCount = 0
            for (acerto in paraEnviar) {
                try {
                    val map = entityToMap(acerto)
                    map["id"] = acerto.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(COLLECTION_ACERTOS)
                    collectionRef.document(acerto.id.toString()).set(map).await()
                    
                    pushAcertoMesas(acerto.id)
                    syncCount++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar acerto ${acerto.id}")
                }
            }
            
            savePushMetadata(entityType, syncCount, System.currentTimeMillis() - startTime)
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no push de acertos")
            Result.failure(e)
        }
    }

    private suspend fun pushAcertoMesas(acertoId: Long) {
        try {
            val acertoMesas = appRepository.buscarAcertoMesasPorAcerto(acertoId)
            for (am in acertoMesas) {
                try {
                    var fotoUrl = am.fotoRelogioFinal
                    
                    if (!fotoUrl.isNullOrEmpty() && !firebaseImageUploader.isFirebaseStorageUrl(fotoUrl)) {
                        val uploaded = firebaseImageUploader.uploadMesaRelogio(fotoUrl, am.mesaId)
                        if (uploaded != null) {
                            fotoUrl = uploaded
                            val updatedAm = am.copy(fotoRelogioFinal = uploaded)
                            appRepository.inserirAcertoMesa(updatedAm)
                        }
                    }
                    
                    val amParaEnviar = if (fotoUrl != am.fotoRelogioFinal) am.copy(fotoRelogioFinal = fotoUrl) else am
                    val map = entityToMap(amParaEnviar)
                    map["id"] = am.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(COLLECTION_ACERTO_MESAS)
                    collectionRef.document("${am.acertoId}_${am.mesaId}").set(map).await()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar AcertoMesa ${am.id}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no push de AcertoMesas para acerto $acertoId")
        }
    }
}
