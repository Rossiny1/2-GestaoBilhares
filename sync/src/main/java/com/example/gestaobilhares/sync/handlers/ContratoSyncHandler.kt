package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.AditivoContrato
import com.example.gestaobilhares.data.entities.AditivoMesa
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.entities.ContratoMesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

import com.example.gestaobilhares.utils.FirebaseImageUploader

class ContratoSyncHandler(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: FirebaseImageUploader
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader) {

    override val entityType: String = "contratos"
    private val aditivosEntityType: String = "aditivos"
    private val aditivoMesasEntityType: String = "aditivo_mesas"
    private val contratoMesasEntityType: String = "contrato_mesas"

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        if (!networkUtils.isConnected()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        val startTime = System.currentTimeMillis()
        val lastSyncTimestamp = getLastSyncTimestamp(entityType)
        val canUseIncremental = lastSyncTimestamp > 0L

        val collectionRef = getCollectionReference(entityType)

        return try {
            val result = if (canUseIncremental) {
                pullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
            } else {
                pullComplete(collectionRef, startTime, timestampOverride)
            }
            
            // 2. Pull Aditivo Mesas
            pullAditivoMesas().onSuccess { count -> 
                Timber.tag(TAG).d("? Pull AditivoMesas: $count sincronizados")
            }
            
            // 3. Pull Contrato Mesas
            pullContratoMesas().onSuccess { count ->
                Timber.tag(TAG).d("? Pull ContratoMesas: $count sincronizados")
            }
            
            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao processar pull de contratos")
            Result.failure(e)
        }
    }

    private suspend fun pullIncremental(
        collectionRef: CollectionReference,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int> {
        Timber.tag(TAG).d("Iniciando pull INCREMENTAL de contratos...")
        
        // Estratégia híbrida: se incremental falhar ou retornar 0 com dados locais, tenta completo
        val localCount = runCatching { appRepository.buscarTodosContratos().first().size }.getOrDefault(0)

        val incrementalDocuments = try {
            collectionRef
                .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                .orderBy("lastModified")
                .get()
                .await()
                .documents
        } catch (e: Exception) {
            Timber.tag(TAG).w("Query incremental falhou, tentando fallback completo: ${e.message}")
            return pullComplete(collectionRef, startTime, timestampOverride)
        }

        if (incrementalDocuments.isEmpty() && localCount > 0) {
            Timber.tag(TAG).w("Incremental retornou 0 mas há $localCount locais. Executando pull COMPLETO.")
            return pullComplete(collectionRef, startTime, timestampOverride)
        }

        val syncCount = processSnapshot(incrementalDocuments)
        
        saveSyncMetadata(
            entityType = entityType,
            syncCount = syncCount,
            durationMs = System.currentTimeMillis() - startTime,
            timestampOverride = timestampOverride
        )

        return Result.success(syncCount)
    }

    private suspend fun pullComplete(
        collectionRef: CollectionReference,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int> {
        Timber.tag(TAG).d("Iniciando pull COMPLETO de contratos...")
        
        val snapshot = collectionRef.get().await()
        val syncCount = processSnapshot(snapshot.documents)
        
        saveSyncMetadata(
            entityType = entityType,
            syncCount = syncCount,
            durationMs = System.currentTimeMillis() - startTime,
            timestampOverride = timestampOverride
        )

        return Result.success(syncCount)
    }

    private suspend fun processSnapshot(documents: List<com.google.firebase.firestore.DocumentSnapshot>): Int {
        var count = 0
        val todosContratos = appRepository.buscarTodosContratos().first()
        val localCache = todosContratos.associateBy { it.id }

        for (doc in documents) {
            try {
                val data = doc.data ?: continue
                val id = doc.id.toLongOrNull() ?: continue

                val json = gson.toJson(data)
                val server = gson.fromJson(json, ContratoLocacao::class.java)?.copy(id = id) ?: continue

                // Filtro de rota
                if (!shouldSyncRouteData(null, clienteId = server.clienteId, allowUnknown = false)) {
                    continue
                }

                val local = localCache[id]
                val serverTime = (data["lastModified"] as? Timestamp)?.toDate()?.time ?: server.dataAtualizacao.time
                val localTime = local?.dataAtualizacao?.time ?: 0L

                if (local == null || serverTime > localTime) {
                    // Validar FK cliente
                    if (ensureEntityExists("cliente", server.clienteId)) {
                        if (local == null) appRepository.inserirContrato(server)
                        else appRepository.atualizarContrato(server)
                        
                        // Sincronizar aditivos relacionados
                        pullAditivos(id)
                        count++
                    } else {
                        Timber.tag(TAG).w("Contrato $id ignorado: cliente ${server.clienteId} não existe localmente")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Erro ao processar documento de contrato ${doc.id}")
            }
        }
        return count
    }

    private suspend fun pullAditivos(contratoId: Long) {
        try {
            val collectionRef = getCollectionReference(COLLECTION_ADITIVOS)
            val snapshot = collectionRef
                .whereEqualTo("contratoId", contratoId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val json = gson.toJson(data)
                    val aditivo = gson.fromJson(json, AditivoContrato::class.java) ?: continue
                    
                    appRepository.inserirAditivo(aditivo)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao sincronizar aditivo ${doc.id}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no pull de aditivos para contrato $contratoId")
        }
    }

    override suspend fun push(): Result<Int> {
        if (!networkUtils.isConnected()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        val startTime = System.currentTimeMillis()
        val lastPushTimestamp = getLastPushTimestamp(entityType)
        
        return try {
            val locais = appRepository.buscarTodosContratos().first()
            val paraEnviar = locais.filter { it.dataAtualizacao.time > lastPushTimestamp }

            if (paraEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }

            var count = 0
            val collectionRef = getCollectionReference(entityType)

            for (contrato in paraEnviar) {
                try {
                    val map = entityToMap(contrato)
                    map["id"] = contrato.id
                    map["roomId"] = contrato.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    map["syncTimestamp"] = FieldValue.serverTimestamp()

                    collectionRef.document(contrato.id.toString()).set(map).await()
                    
                    // Push aditivos relacionados
                    pushAditivos(contrato.id)
                    count++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar contrato ${contrato.id}")
                }
            }

            savePushMetadata(entityType, count, System.currentTimeMillis() - startTime)
            
            // 2. Push Aditivo Mesas
            pushAditivoMesas().onSuccess { pCount ->
                Timber.tag(TAG).d("? Push AditivoMesas: $pCount sincronizados")
            }
            
            // 3. Push Contrato Mesas
            pushContratoMesas().onSuccess { pCount ->
                Timber.tag(TAG).d("? Push ContratoMesas: $pCount sincronizados")
            }

            Result.success(count)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no push de contratos")
            Result.failure(e)
        }
    }

    private suspend fun pushAditivos(contratoId: Long) {
        try {
            val aditivos = appRepository.buscarAditivosPorContrato(contratoId).first()
            val collectionRef = getCollectionReference(COLLECTION_ADITIVOS)

            for (aditivo in aditivos) {
                try {
                    val map = entityToMap(aditivo)
                    map["id"] = aditivo.id
                    map["roomId"] = aditivo.id
                    map["lastModified"] = FieldValue.serverTimestamp()

                    collectionRef.document(aditivo.id.toString()).set(map).await()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar aditivo ${aditivo.id}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no push de aditivos para contrato $contratoId")
        }
    }

    /**
     * Pull Aditivo Mesas: Sincroniza vínculos aditivo-mesa do Firestore para o Room
     */
    suspend fun pullAditivoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = aditivoMesasEntityType
        
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else {
                collectionRef.get().await()
            }
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(type, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val aditivoMesasCache = appRepository.obterTodosAditivoMesas().associateBy { it.id }.toMutableMap()
            var syncCount = 0
            
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val aditivoMesaId = (data["roomId"] as? Number)?.toLong()
                        ?: (data["id"] as? Number)?.toLong()
                        ?: doc.id.toLongOrNull()
                        ?: continue
                        
                    val aditivoId = (data["aditivoId"] as? Number)?.toLong()
                        ?: (data["aditivo_id"] as? Number)?.toLong() ?: continue
                    val mesaId = (data["mesaId"] as? Number)?.toLong()
                        ?: (data["mesa_id"] as? Number)?.toLong() ?: continue
                    
                    val rotaId = getMesaRouteId(mesaId)
                    if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                        continue
                    }
                    
                    val aditivoMesa = AditivoMesa(
                        id = aditivoMesaId,
                        aditivoId = aditivoId,
                        mesaId = mesaId,
                        tipoEquipamento = data["tipoEquipamento"] as? String
                            ?: data["tipo_equipamento"] as? String ?: "",
                        numeroSerie = data["numeroSerie"] as? String
                            ?: data["numero_serie"] as? String ?: ""
                    )
                    
                    appRepository.inserirAditivoMesas(listOf(aditivoMesa))
                    aditivoMesasCache[aditivoMesaId] = aditivoMesa
                    syncCount++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao processar aditivo mesa ${doc.id}")
                }
            }
            
            saveSyncMetadata(type, syncCount, System.currentTimeMillis() - startTime)
            Result.success(syncCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pull Contrato Mesas: Sincroniza vínculos contrato-mesa do Firestore para o Room
     */
    suspend fun pullContratoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = contratoMesasEntityType
        
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else {
                collectionRef.get().await()
            }
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(type, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val contratoMesasCache = appRepository.obterTodosContratoMesas().associateBy { it.id }.toMutableMap()
            var syncCount = 0
            
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val contratoMesaId = (data["roomId"] as? Number)?.toLong()
                        ?: (data["id"] as? Number)?.toLong()
                        ?: doc.id.toLongOrNull()
                        ?: continue
                        
                    val contratoId = (data["contratoId"] as? Number)?.toLong()
                        ?: (data["contrato_id"] as? Number)?.toLong() ?: continue
                    val mesaId = (data["mesaId"] as? Number)?.toLong()
                        ?: (data["mesa_id"] as? Number)?.toLong() ?: continue
                    
                    val rotaId = getMesaRouteId(mesaId)
                    if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                        continue
                    }
                    
                    val contratoMesa = ContratoMesa(
                        id = contratoMesaId,
                        contratoId = contratoId,
                        mesaId = mesaId,
                        tipoEquipamento = data["tipoEquipamento"] as? String
                            ?: data["tipo_equipamento"] as? String ?: "",
                        numeroSerie = data["numeroSerie"] as? String
                            ?: data["numero_serie"] as? String ?: "",
                        valorFicha = (data["valorFicha"] as? Number)?.toDouble()
                            ?: (data["valor_ficha"] as? Number)?.toDouble(),
                        valorFixo = (data["valorFixo"] as? Number)?.toDouble()
                            ?: (data["valor_fixo"] as? Number)?.toDouble()
                    )
                    
                    // Validar FK
                    if (ensureEntityExists("contrato", contratoId) && ensureEntityExists("mesa", mesaId)) {
                        appRepository.inserirContratoMesa(contratoMesa)
                        contratoMesasCache[contratoMesaId] = contratoMesa
                        syncCount++
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao processar contrato mesa ${doc.id}")
                }
            }
            
            saveSyncMetadata(type, syncCount, System.currentTimeMillis() - startTime)
            Result.success(syncCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getMesaRouteId(mesaId: Long): Long? {
        val mesa = appRepository.obterMesaPorId(mesaId) ?: return null
        val cliente = appRepository.obterClientePorId(mesa.clienteId ?: 0L)
        return cliente?.rotaId
    }

    /**
     * Push Aditivo Mesas: Envia vínculos aditivo-mesa locais para o Firestore
     */
    suspend fun pushAditivoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = aditivoMesasEntityType
        
        return try {
            val lastPush = getLastPushTimestamp(type)
            // Como não temos lastModified em AditivoMesa, enviamos os que não estão sincronizados ou foram criados recentemente
            // Por simplicidade, enviamos todos os aditivos de contratos que foram alterados recentemente, ou usamos a lógica original do repo
            val aditivoMesas = appRepository.obterTodosAditivoMesas().filter {
                // Aqui poderíamos ter um campo de status de sincronização, mas seguindo a lógica do SyncRepository:
                true // No SyncRepository ele não filtrava por timestamp no pushAditivoMesas legado
            }
            
            if (aditivoMesas.isEmpty()) return Result.success(0)
            
            var count = 0
            val collectionRef = getCollectionReference(type)
            
            for (am in aditivoMesas) {
                try {
                    val map = entityToMap(am)
                    map["id"] = am.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    collectionRef.document("${am.aditivoId}_${am.mesaId}").set(map).await()
                    count++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar aditivo mesa ${am.id}")
                }
            }
            
            savePushMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Push Contrato Mesas: Envia vínculos contrato-mesa locais para o Firestore
     */
    suspend fun pushContratoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = contratoMesasEntityType
        
        return try {
            val lastPush = getLastPushTimestamp(type)
            val contratoMesas = appRepository.obterTodosContratoMesas().filter {
                true // Lógica similar ao legados
            }
            
            if (contratoMesas.isEmpty()) return Result.success(0)
            
            var count = 0
            val collectionRef = getCollectionReference(type)
            
            for (cm in contratoMesas) {
                try {
                    val map = entityToMap(cm)
                    map["id"] = cm.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    collectionRef.document("${cm.contratoId}_${cm.mesaId}").set(map).await()
                    count++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar contrato mesa ${cm.id}")
                }
            }
            
            savePushMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ContratoSyncHandler"
        private const val COLLECTION_ADITIVOS = "aditivos"
    }
}
