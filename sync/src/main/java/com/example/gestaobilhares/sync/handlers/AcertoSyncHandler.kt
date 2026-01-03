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
import kotlinx.coroutines.CancellationException
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
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Pull de acertos cancelado")
            throw e
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
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Pull completo de acertos cancelado")
            throw e
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
                        Timber.tag(TAG).d("✅ Acerto inserido: ID=${acerto.id}")
                    } else {
                        Timber.tag(TAG).d("✅ Acerto atualizado: ID=${acerto.id}")
                    }
                    
                    syncCount++
                    
                    // ✅ CORREÇÃO CRÍTICA: Buscar mesas do acerto APÓS garantir que o acerto foi salvo
                    // Isso garante que as mesas possam ser vinculadas corretamente
                    try {
                        pullAcertoMesas(acerto.id)
                        Timber.tag(TAG).d("✅ Mesas do acerto ${acerto.id} processadas")
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "❌ Erro ao buscar mesas do acerto ${acerto.id}, mas acerto foi salvo")
                        // Não falhar o sync do acerto por causa das mesas - continuar processamento
                    }
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
            Timber.tag(TAG).d("🔍 Buscando mesas do acerto $acertoId no Firestore...")
            val collectionRef = getCollectionReference(COLLECTION_ACERTO_MESAS)
            
            // ✅ CORREÇÃO CRÍTICA: Usar "acerto_id" (snake_case) em vez de "acertoId" (camelCase)
            // O Gson está configurado com FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES,
            // então os dados são salvos no Firestore com snake_case
            val snapshot = collectionRef
                .whereEqualTo("acerto_id", acertoId) // ✅ CORREÇÃO: snake_case para corresponder ao Firestore
                .get()
                .await()
            
            Timber.tag(TAG).d("📊 Encontradas ${snapshot.documents.size} mesas do acerto $acertoId no Firestore")
            
            if (snapshot.documents.isEmpty()) {
                Timber.tag(TAG).w("⚠️ Nenhuma mesa encontrada para acerto $acertoId no Firestore")
                // ✅ NOVO: Tentar buscar também com camelCase como fallback (caso dados antigos)
                val snapshotFallback = collectionRef
                    .whereEqualTo("acertoId", acertoId)
                    .get()
                    .await()
                
                if (snapshotFallback.documents.isNotEmpty()) {
                    Timber.tag(TAG).d("✅ Encontradas ${snapshotFallback.documents.size} mesas usando fallback (camelCase)")
                    processAcertoMesas(snapshotFallback.documents, acertoId)
                }
                return
            }
            
            processAcertoMesas(snapshot.documents, acertoId)
            
            // ✅ VERIFICAÇÃO FINAL: Confirmar que as mesas foram salvas corretamente
            val mesasSalvas = appRepository.buscarAcertoMesasPorAcerto(acertoId)
            Timber.tag(TAG).d("✅ Verificação final: ${mesasSalvas.size} mesas salvas no banco local para acerto $acertoId")
            if (mesasSalvas.isEmpty() && snapshot.documents.isNotEmpty()) {
                Timber.tag(TAG).w("⚠️ ATENÇÃO: Mesas foram encontradas no Firestore (${snapshot.documents.size}) mas não foram salvas no banco local!")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no pull de AcertoMesas para acerto $acertoId")
        }
    }
    
    /**
     * ✅ NOVO: Processa documentos de AcertoMesa do Firestore
     * Extraído para evitar duplicação de código
     */
    private suspend fun processAcertoMesas(documents: List<com.google.firebase.firestore.DocumentSnapshot>, acertoId: Long) {
        for (doc in documents) {
            try {
                val data = doc.data ?: continue
                
                Timber.tag(TAG).d("📄 Processando documento ${doc.id} do Firestore")
                Timber.tag(TAG).d("   Campos disponíveis: ${data.keys.joinToString()}")
                
                // ✅ CORREÇÃO: Garantir que acerto_id esteja presente no data
                if (!data.containsKey("acerto_id") && !data.containsKey("acertoId")) {
                    Timber.tag(TAG).w("⚠️ Documento ${doc.id} não possui acerto_id, adicionando...")
                    data["acerto_id"] = acertoId
                    data["acertoId"] = acertoId
                } else if (data.containsKey("acertoId") && !data.containsKey("acerto_id")) {
                    // Converter camelCase para snake_case
                    data["acerto_id"] = data["acertoId"]
                }
                
                // ✅ CORREÇÃO: Extrair ID do documento ou do campo id
                // O documento pode ter ID no formato "acertoId_mesaId" ou ter campo "id" no data
                val acertoMesaId = when {
                    data.containsKey("id") -> (data["id"] as? Number)?.toLong() ?: 0L
                    else -> 0L // Será gerado automaticamente pelo Room
                }
                
                val json = gson.toJson(data)
                var acertoMesa = gson.fromJson(json, AcertoMesa::class.java) ?: continue
                
                // ✅ CORREÇÃO CRÍTICA: Garantir que acertoId esteja correto
                if (acertoMesa.acertoId != acertoId) {
                    Timber.tag(TAG).w("⚠️ AcertoMesa ${doc.id} tem acertoId incorreto (${acertoMesa.acertoId} != $acertoId), corrigindo...")
                    acertoMesa = acertoMesa.copy(acertoId = acertoId)
                }
                
                Timber.tag(TAG).d("📋 Processando AcertoMesa: acertoId=${acertoMesa.acertoId}, mesaId=${acertoMesa.mesaId}, id=$acertoMesaId")
                
                // Validar FKs
                if (!ensureEntityExists("acerto", acertoMesa.acertoId) || !ensureEntityExists("mesa", acertoMesa.mesaId)) {
                    Timber.tag(TAG).w("⏭️ Pulando acerto_mesa ${doc.id} por falha na FK (acerto ou mesa não encontrados).")
                    Timber.tag(TAG).w("   Acerto existe: ${ensureEntityExists("acerto", acertoMesa.acertoId)}")
                    Timber.tag(TAG).w("   Mesa existe: ${ensureEntityExists("mesa", acertoMesa.mesaId)}")
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
                
                // ✅ CORREÇÃO CRÍTICA: Verificar se já existe antes de inserir
                // Usar buscarAcertoMesaPorAcertoEMesa para evitar duplicatas
                val existing = appRepository.buscarAcertoMesaPorAcertoEMesa(acertoMesa.acertoId, acertoMesa.mesaId)
                if (existing != null) {
                    // ✅ Atualizar existente mantendo o ID local
                    val acertoMesaAtualizado = acertoMesa.copy(id = existing.id)
                    appRepository.inserirAcertoMesa(acertoMesaAtualizado)
                    Timber.tag(TAG).d("✅ AcertoMesa atualizado: acertoId=${acertoMesa.acertoId}, mesaId=${acertoMesa.mesaId}, id=${existing.id}")
                } else {
                    // ✅ Inserir novo (ID será gerado automaticamente pelo Room)
                    // Não usar acertoMesaId do Firestore pois pode causar conflitos
                    val novoId = appRepository.inserirAcertoMesa(acertoMesa.copy(id = 0))
                    Timber.tag(TAG).d("✅ AcertoMesa inserido: acertoId=${acertoMesa.acertoId}, mesaId=${acertoMesa.mesaId}, novoId=$novoId")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Erro ao sincronizar AcertoMesa ${doc.id}")
            }
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
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Push de acertos cancelado")
            throw e
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
