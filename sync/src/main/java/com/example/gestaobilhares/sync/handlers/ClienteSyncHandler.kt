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
import timber.log.Timber
import java.util.Date

import com.example.gestaobilhares.utils.FirebaseImageUploader

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
    firebaseImageUploader: FirebaseImageUploader
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader) {
    
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
            val snapshot = collectionRef
                .whereGreaterThan("dataUltimaAtualizacao", Timestamp(Date(lastSyncTimestamp)))
                .get()
                .await()
                
            val syncCount = processSnapshot(snapshot.documents)
            val durationMs = System.currentTimeMillis() - startTime
            
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("✅ Pull Clientes (INCREMENTAL) concluído: $syncCount sincronizados")
            Result.success(syncCount)
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
            val snapshot = collectionRef.get().await()
            val syncCount = processSnapshot(snapshot.documents)
            val durationMs = System.currentTimeMillis() - startTime
            
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("✅ Pull Clientes (COMPLETO) concluído: $syncCount sincronizados")
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
                val cliente = gson.fromJson(json, Cliente::class.java)?.copy(id = id) ?: continue
                
                // Filtro de rota
                if (!shouldSyncRouteData(cliente.rotaId, allowUnknown = true)) {
                    continue
                }
                
                val local = appRepository.obterClientePorId(id)
                val serverTime = (data["dataUltimaAtualizacao"] as? Timestamp)?.toDate()?.time ?: 0L
                val localTime = local?.dataUltimaAtualizacao?.time ?: 0L
                
                if (local == null || serverTime > localTime) {
                    if (local == null) appRepository.inserirCliente(cliente)
                    else appRepository.atualizarCliente(cliente)
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
            
            val paraEnviar = clientesLocais.filter { it.dataUltimaAtualizacao.time > lastPushTimestamp }
            
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
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no push de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
}
