package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.Date

import com.example.gestaobilhares.core.utils.FirebaseImageUploader

/**
 * Handler especializado para sincronização de Clientes.
 * 
 * Responsabilidades:
 * - Pull: Sincroniza clientes do Firestore para Room (incremental e completo)
 * - Push: Sincroniza clientes do Room para Firestore (incremental)
 * - Filtro por rota (multi-tenancy)
 * - Validação de timestamps
 */
class ClienteSyncHandler(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {
    
    override val entityType: String = "clientes"
    
    companion object {
        private const val COLLECTION_CLIENTES = "clientes"
    }
    
    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("Iniciando pull de clientes...")
            val collectionRef = getCollectionReference(COLLECTION_CLIENTES)
            
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            val result = if (canUseIncremental) {
                Timber.tag(TAG).d("🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                pullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
            } else {
                Timber.tag(TAG).d("🔄 Primeira sincronização - usando método COMPLETO")
                pullComplete(collectionRef, startTime, timestampOverride)
            }
            
            result
        } catch (e: CancellationException) {
            // ✅ CORREÇÃO: CancellationException deve ser re-lançada para propagar cancelamento
            Timber.tag(TAG).d("⏹️ Pull de clientes cancelado")
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no pull de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun pullIncremental(
        collectionRef: CollectionReference,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int> {
        return try {
            val documents = fetchDocumentsWithRouteFilter(
                collectionRef = collectionRef,
                routeField = "rota_id",
                lastSyncTimestamp = lastSyncTimestamp,
                timestampField = "dataUltimaAtualizacao"
            )
                
            val syncCount = processSnapshot(documents)
            val durationMs = System.currentTimeMillis() - startTime
            
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                timestampOverride = timestampOverride
            )
            
            Result.success(syncCount)
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Pull incremental de clientes cancelado")
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w("⚠️ Incremental falhou, tentando completo: ${e.message}")
            pullComplete(collectionRef, startTime, timestampOverride)
        }
    }
    
    private suspend fun pullComplete(
        collectionRef: CollectionReference,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, "rota_id")
            val syncCount = processSnapshot(documents)
            val durationMs = System.currentTimeMillis() - startTime
            
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                timestampOverride = timestampOverride
            )
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no pull completo de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun processSnapshot(documents: List<com.google.firebase.firestore.DocumentSnapshot>): Int {
        var count = 0
        for (doc in documents) {
            try {
                val data = doc.data ?: continue
                val id = doc.id.toLongOrNull() ?: continue
                val json = gson.toJson(data)
                
                val cliente = gson.fromJson(json, Cliente::class.java)?.copy(id = id)
                if (cliente == null) {
                    continue
                }
                
                // ✅ RESILIÊNCIA: Garantir rotaId independente do case (Firestore costuma ser snake_case)
                val effectiveRotaId = (data["rota_id"] as? Number)?.toLong() 
                    ?: (data["rotaId"] as? Number)?.toLong() 
                    ?: cliente.rotaId

                // Filtro de rota
                if (!shouldSyncRouteData(effectiveRotaId, allowUnknown = true)) {
                    continue
                }
                
                var local = appRepository.obterClientePorId(id)
                val serverTime = com.example.gestaobilhares.core.utils.DateUtils.convertToLong(data["dataUltimaAtualizacao"])
                    ?: com.example.gestaobilhares.core.utils.DateUtils.convertToLong(data["data_ultima_atualizacao"])
                    ?: 0L
                val localTime = local?.dataUltimaAtualizacao ?: 0L
                
                
                
                // ✅ LÓGICA DE RECONCILIAÇÃO: Se não achou pelo ID, tenta pelo Nome + Rota
                if (local == null) {
                    val clientePorNome = appRepository.buscarClientePorNomeERota(cliente.nome, effectiveRotaId)
                    if (clientePorNome != null && clientePorNome.id != id) {
                        Timber.tag(TAG).w("🚨 [RECONCILIAÇÃO] Detectada duplicação por colisão de ID para '${cliente.nome}'")
                        Timber.tag(TAG).w("   - ID Local: ${clientePorNome.id}, ID Servidor: $id")
                        
                        // Migrar mesas, acertos e contratos do ID antigo para o ID novo (servidor)
                        appRepository.migrarDadosDeCliente(clientePorNome.id, id)
                        
                        // Remover o cliente local duplicado para que o novo seja inserido com o ID correto
                        appRepository.deletarCliente(clientePorNome)
                        Timber.tag(TAG).d("   - Cliente local antigo removido.")
                        
                        // Agora marcamos como 'local' nulo para forçar a inserção do novo ID
                        local = null
                    }
                }

                if (local == null || serverTime > localTime) {
                    if (local == null) {
                        Timber.tag(TAG).d("📥 Inserindo novo cliente: ${cliente.nome} (ID $id)")
                        appRepository.inserirCliente(cliente)
                    } else {
                        Timber.tag(TAG).d("🆙 Atualizando cliente: ${cliente.nome} (ID $id)")
                        appRepository.atualizarCliente(cliente)
                    }
                    count++
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("Erro ao processar cliente ${doc.id}: ${e.message}")
            }
        }
        return count
    }
    
    override suspend fun push(): Result<Int> {
        val startTime = System.currentTimeMillis()
        return try {
            Timber.tag(TAG).d("Iniciando push de clientes...")
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val clientesLocais = appRepository.obterTodosClientes().first()
            
            val paraEnviar = clientesLocais.filter { it.dataUltimaAtualizacao > lastPushTimestamp }
            
            if (paraEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            var count = 0
            val collectionRef = getCollectionReference(COLLECTION_CLIENTES)
            
            for (cliente in paraEnviar) {
                try {
                    val map = entityToMap(cliente)
                    map["id"] = cliente.id
                    map["dataUltimaAtualizacao"] = FieldValue.serverTimestamp()
                    
                    collectionRef.document(cliente.id.toString()).set(map).await()
                    count++
                } catch (e: Exception) {
                    Timber.tag(TAG).e("Erro ao enviar cliente ${cliente.id}: ${e.message}")
                }
            }
            
            savePushMetadata(entityType, count, System.currentTimeMillis() - startTime)
            Timber.tag(TAG).d("✅ Push Clientes concluído: $count enviados")
            Result.success(count)
        } catch (e: CancellationException) {
            Timber.tag(TAG).d("⏹️ Push de clientes cancelado")
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no push de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
}
