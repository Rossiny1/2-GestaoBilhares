package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.gson.GsonBuilder
import com.example.gestaobilhares.core.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

import com.example.gestaobilhares.core.utils.FirebaseImageUploader

/**
 * Handler especializado para sincronização de Mesas.
 * 
 * Responsabilidades:
 * - Pull: Sincroniza mesas do Firestore para Room (incremental e completo)
 * - Push: Sincroniza mesas do Room para Firestore (incremental)
 * - Proteção contra perda de dados (clienteId)
 * - Validação de timestamps para evitar sobrescrita de dados mais recentes
 */
class MesaSyncHandler(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {
    
    override val entityType: String = "mesas"
    
    companion object {
        private const val COLLECTION_MESAS = "mesas"
    }
    
    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("Iniciando pull de mesas...")
            val collectionRef = getCollectionReference(COLLECTION_MESAS)
            
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                Timber.tag(TAG).d("🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(collectionRef, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesas().first().size }.getOrDefault(0)
                    
                    // Validação: Se incremental retornou 0 mas há mesas locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Timber.tag(TAG).w("⚠️ Incremental retornou 0 mesas mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullComplete(collectionRef, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    Timber.tag(TAG).w("⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Timber.tag(TAG).d("🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            pullComplete(collectionRef, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no pull de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Tenta sincronização incremental de mesas.
     */
    private suspend fun tryPullIncremental(
        collectionRef: CollectionReference,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int>? {
        return try {
            // Carregar cache de mesas locais
            val todasMesas = appRepository.obterTodasMesas().first()
            val mesasCache = todasMesas.associateBy { it.id }
            Timber.tag(TAG).d("   📦 Cache de mesas carregado: ${mesasCache.size} mesas locais")
            
            // Tentar query incremental
            val incrementalMesas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Timber.tag(TAG).w("⚠️ Query incremental falhou, buscando todas as mesas: ${e.message}")
                emptyList()
            }
            
            // Se incremental retornou 0 mas há mesas locais, buscar TODAS
            val allMesas = if (incrementalMesas.isEmpty() && mesasCache.isNotEmpty()) {
                Timber.tag(TAG).w("⚠️ Incremental retornou 0 mesas mas há ${mesasCache.size} locais - buscando TODAS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Timber.tag(TAG).w("⚠️ Erro ao buscar todas as mesas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesas
            }
            
            Timber.tag(TAG).d("🔄 Sincronização INCREMENTAL: ${allMesas.size} documentos encontrados")
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            allMesas.forEach { doc ->
                try {
                    val mesaData = doc.data ?: run {
                        errorCount++
                        return@forEach
                    }
                    val mesaId = doc.id.toLongOrNull() ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    val mesaJson = gson.toJson(mesaData)
                    val mesaFirestore = gson.fromJson(mesaJson, Mesa::class.java)
                        ?.copy(id = mesaId) ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    // Verificar se deve sincronizar baseado na rota do cliente
                    val rotaId = getClienteRouteId(mesaFirestore.clienteId)
                    if (!shouldSyncRouteData(rotaId)) {
                        skippedCount++
                        return@forEach
                    }
                    
                    // Validar FK cliente (opcional para mesas)
                    if (!ensureEntityExists("cliente", mesaFirestore.clienteId)) {
                        Timber.tag(TAG).w("⏭️ Pulando mesa ${mesaFirestore.id} por falha na FK cliente ${mesaFirestore.clienteId}")
                        return@forEach
                    }
                    
                    // Buscar versão local para preservar dados se necessário
                    val mesaLocal = mesasCache[mesaId]
                    
                    // Verificar timestamp do servidor vs local
                    val serverTimestamp = com.example.gestaobilhares.core.utils.DateUtils.convertToLong(mesaData["lastModified"])
                        ?: com.example.gestaobilhares.core.utils.DateUtils.convertToLong(mesaData["dataUltimaLeitura"])
                        ?: mesaFirestore.dataUltimaLeitura
                    val localTimestamp = mesaLocal?.dataUltimaLeitura ?: mesaLocal?.dataInstalacao ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: Sincronizar APENAS se servidor é mais recente que local
                    Timber.tag(TAG).w("🔍 [PULL MESA] Mesa ID=$mesaId")
                    Timber.tag(TAG).w("   📥 SERVIDOR: clienteId=${mesaFirestore.clienteId}, numero=${mesaFirestore.numero}")
                    Timber.tag(TAG).w("   💾 LOCAL: clienteId=${mesaLocal?.clienteId}, numero=${mesaLocal?.numero}")
                    Timber.tag(TAG).w("   ⏰ TIMESTAMPS: servidor=${Date(serverTimestamp)}, local=${Date(localTimestamp)}")
                    
                    val shouldSync = mesaLocal == null || serverTimestamp > localTimestamp
                    
                    /* 
                     * ✅ REMOVIDO: ser tão restritivo com a perda de clienteId.
                     * Se o servidor diz que a mesa não tem cliente, e o servidor é mais recente,
                     * devemos aceitar a verdade do servidor. 
                     */
                    val wouldLoseCliente = false 
                    
                    if (wouldLoseCliente) {
                        Timber.tag(TAG).e("🚨 [PULL MESA] BLOQUEADO: Mesa $mesaId perderia clienteId (local=${mesaLocal?.clienteId}, servidor=${mesaFirestore.clienteId})")
                        Timber.tag(TAG).e("   ⚠️ Mesa local tem cliente mas servidor não - PRESERVANDO dados locais")
                        skippedCount++
                        return@forEach
                    }
                    
                    if (!shouldSync && mesaLocal != null) {
                        Timber.tag(TAG).d("⏭️ Mesa ${mesaId} skipada: local (${Date(localTimestamp)}) é mais recente que servidor (${Date(serverTimestamp)})")
                        skippedCount++
                        return@forEach
                    }
                    
                    if (shouldSync) {
                        val clienteIdAntes = mesaLocal?.clienteId
                        Timber.tag(TAG).w("   ✅ SINCRONIZANDO: clienteId ANTES=$clienteIdAntes, DEPOIS=${mesaFirestore.clienteId}")
                        
                        if (mesaLocal == null) {
                            appRepository.inserirMesa(mesaFirestore)
                            Timber.tag(TAG).w("   ➕ Mesa $mesaId INSERIDA (nova)")
                        } else {
                            appRepository.atualizarMesa(mesaFirestore)
                            Timber.tag(TAG).w("   🔄 Mesa $mesaId ATUALIZADA")
                            
                            // ✅ LOG APÓS ATUALIZAR - Verificar se clienteId foi perdido
                            val mesaAposUpdate = appRepository.obterMesaPorId(mesaId)
                            if (clienteIdAntes != null && clienteIdAntes > 0L && (mesaAposUpdate?.clienteId == null || mesaAposUpdate.clienteId == 0L)) {
                                Timber.tag(TAG).e("🚨 [PULL MESA] ERRO CRÍTICO: Mesa $mesaId PERDEU clienteId após atualização!")
                                Timber.tag(TAG).e("   ANTES: clienteId=$clienteIdAntes")
                                Timber.tag(TAG).e("   DEPOIS: clienteId=${mesaAposUpdate?.clienteId}")
                            }
                        }
                        syncCount++
                    } else {
                        skippedCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("❌ Erro ao sincronizar mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("✅ Pull Mesas (INCREMENTAL) concluído: $syncCount sincronizadas, $skippedCount puladas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).w("⚠️ Erro na sincronização incremental: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de mesas.
     */
    private suspend fun pullComplete(
        collectionRef: CollectionReference,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val mesaData = doc.data ?: run {
                        errorCount++
                        return@forEach
                    }
                    val mesaId = doc.id.toLongOrNull() ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    val mesaJson = gson.toJson(mesaData)
                    val mesaFirestore = gson.fromJson(mesaJson, Mesa::class.java)
                        ?.copy(id = mesaId) ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    val rotaId = getClienteRouteId(mesaFirestore.clienteId)
                    if (!shouldSyncRouteData(rotaId, clienteId = mesaFirestore.clienteId, allowUnknown = true)) {
                        skippedCount++
                        return@forEach
                    }
                    
                    val mesaLocal = appRepository.obterMesaPorId(mesaId)
                    
                    /* 
                     * ✅ REMOVIDO: ser tão restritivo no pull completo.
                     */
                    val wouldLoseCliente = false
                    
                    if (wouldLoseCliente) {
                        Timber.tag(TAG).e("🚨 [PULL MESA COMPLETO] BLOQUEADO: Mesa $mesaId perderia clienteId (local=${mesaLocal?.clienteId}, servidor=${mesaFirestore.clienteId})")
                        Timber.tag(TAG).e("   ⚠️ Mesa local tem cliente mas servidor não - PRESERVANDO dados locais")
                        skippedCount++
                        return@forEach
                    }
                    
                    val clienteIdAntes = mesaLocal?.clienteId
                    when {
                        mesaLocal == null -> {
                            appRepository.inserirMesa(mesaFirestore)
                            Timber.tag(TAG).w("   ➕ Mesa $mesaId INSERIDA (nova)")
                            syncCount++
                        }
                        else -> {
                            appRepository.atualizarMesa(mesaFirestore)
                            Timber.tag(TAG).w("   🔄 Mesa $mesaId ATUALIZADA")
                            
                            // ✅ LOG APÓS ATUALIZAR - Verificar se clienteId foi perdido
                            val mesaAposUpdate = appRepository.obterMesaPorId(mesaId)
                            if (clienteIdAntes != null && clienteIdAntes > 0L && (mesaAposUpdate?.clienteId == null || mesaAposUpdate.clienteId == 0L)) {
                                Timber.tag(TAG).e("🚨 [PULL MESA COMPLETO] ERRO CRÍTICO: Mesa $mesaId PERDEU clienteId após atualização!")
                                Timber.tag(TAG).e("   ANTES: clienteId=$clienteIdAntes")
                                Timber.tag(TAG).e("   DEPOIS: clienteId=${mesaAposUpdate?.clienteId}")
                            }
                            syncCount++
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("Erro ao sincronizar mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("✅ Pull Mesas (COMPLETO) concluído: $syncCount sincronizadas, $skippedCount puladas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro no pull de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun push(): Result<Int> {
        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("Iniciando push INCREMENTAL de mesas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val mesasLocais = appRepository.obterTodasMesas().first()
            
            // Filtrar apenas mesas modificadas (usar maxOf para considerar dataInstalacao também)
            val mesasParaEnviar = if (canUseIncremental) {
                mesasLocais.filter { mesa ->
                    val mesaTimestamp = maxOf(mesa.dataUltimaLeitura, mesa.dataInstalacao)
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Timber.tag(TAG).d("📤 Push INCREMENTAL: ${it.size} mesas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasLocais.size} total)")
                }
            } else {
                Timber.tag(TAG).d("📤 Primeira sincronização PUSH - enviando todas as ${mesasLocais.size} mesas")
                mesasLocais
            }
            
            if (mesasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var bytesUploaded = 0L
            var errorCount = 0
            var maxServerTimestamp = 0L
            
            mesasParaEnviar.forEach { mesa ->
                try {
                    val mesaMap = entityToMap(mesa)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    mesaMap["roomId"] = mesa.id
                    mesaMap["id"] = mesa.id
                    mesaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    Timber.tag(TAG).d("📤 [DIAGNOSTICO] Enviando Mesa ${mesa.id}. clienteId=${mesa.clienteId}, lastModified definido como serverTimestamp()")
                    
                    val collectionRef = getCollectionReference(COLLECTION_MESAS)
                    val docRef = collectionRef.document(mesa.id.toString())
                    
                    // 1. Escrever
                    docRef.set(mesaMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    if (serverTimestamp > maxServerTimestamp) {
                        maxServerTimestamp = serverTimestamp
                    }
                    
                    Timber.tag(TAG).d("✅ Mesa ${mesa.id} exportada com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += mesaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("Erro ao enviar mesa ${mesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Timber.tag(TAG).d("✅ Push INCREMENTAL de mesas concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms. MaxServerTimestamp: ${Date(maxServerTimestamp)}")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Timber.e("Erro no push de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
}

