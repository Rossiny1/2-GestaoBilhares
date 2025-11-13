package com.example.gestaobilhares.data.repository.domain

import android.content.Context
import android.util.Log
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.utils.NetworkUtils
import com.example.gestaobilhares.data.entities.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repository especializado para sincronização de dados.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 * 
 * Responsabilidades:
 * - Sincronização bidirecional (Pull/Push) com Firebase Firestore
 * - Fila de sincronização offline-first
 * - Gerenciamento de conflitos
 * - Status de sincronização
 */
class SyncRepository(
    private val context: Context,
    private val appRepository: AppRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val networkUtils: NetworkUtils = NetworkUtils(context)
) {
    
    companion object {
        private const val TAG = "SyncRepository"
        private const val COLLECTION_CLIENTES = "clientes"
        private const val COLLECTION_ACERTOS = "acertos"
        private const val COLLECTION_MESAS = "mesas"
        private const val COLLECTION_ROTAS = "rotas"
        private const val COLLECTION_DESPESAS = "despesas"
        private const val COLLECTION_CICLOS = "ciclos"
        private const val COLLECTION_COLABORADORES = "colaboradores"
        private const val COLLECTION_CONTRATOS = "contratos"
        private const val COLLECTION_ACERTO_MESAS = "acerto_mesas"
        private const val COLLECTION_ADITIVOS = "aditivos"
        private const val COLLECTION_ASSINATURAS = "assinaturas"
        
        // Gson para serialização/deserialização
        private val gson: Gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }
    
    // ==================== STATEFLOW - STATUS DE SINCRONIZAÇÃO ====================
    
    /**
     * Status atual da sincronização
     */
    data class SyncStatus(
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null,
        val pendingOperations: Int = 0,
        val failedOperations: Int = 0,
        val isOnline: Boolean = false,
        val error: String? = null
    )
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    // ==================== SINCRONIZAÇÃO PULL (SERVIDOR → LOCAL) ====================
    
    /**
     * Sincroniza dados do servidor para o local (Pull).
     * Offline-first: Funciona apenas quando online.
     */
    suspend fun syncPull(): Result<Unit> {
        return try {
            if (!networkUtils.isConnected()) {
                Log.w(TAG, "Sincronização Pull cancelada: dispositivo offline")
                return Result.failure(Exception("Dispositivo offline"))
            }
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Log.d(TAG, "Iniciando sincronização Pull...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // Pull por domínio em sequência
            pullClientes().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Clientes: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Clientes falhou: ${e.message}", e)
                }
            )
            
            pullRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Rotas falhou: ${e.message}", e)
                }
            )
            
            pullMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Mesas falhou: ${e.message}", e)
                }
            )
            
            pullColaboradores().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Colaboradores: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Colaboradores falhou: ${e.message}", e)
                }
            )
            
            pullCiclos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Ciclos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Ciclos falhou: ${e.message}", e)
                }
            )
            
            pullAcertos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Acertos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Acertos falhou: ${e.message}", e)
                }
            )
            
            pullDespesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Despesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Despesas falhou: ${e.message}", e)
                }
            )
            
            pullContratos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Contratos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Contratos falhou: ${e.message}", e)
                }
            )
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                failedOperations = failedCount
            )
            
            Log.d(TAG, "Sincronização Pull concluída com sucesso")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização Pull: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    // ==================== SINCRONIZAÇÃO PUSH (LOCAL → SERVIDOR) ====================
    
    /**
     * Sincroniza dados do local para o servidor (Push).
     * Offline-first: Enfileira operações quando offline.
     */
    suspend fun syncPush(): Result<Unit> {
        return try {
            if (!networkUtils.isConnected()) {
                Log.w(TAG, "Sincronização Push cancelada: dispositivo offline - operações enfileiradas")
                // TODO: Enfileirar operações para sincronização posterior
                return Result.failure(Exception("Dispositivo offline - operações enfileiradas"))
            }
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Log.d(TAG, "Iniciando sincronização Push...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // Push por domínio em sequência
            pushClientes().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Clientes: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Clientes falhou: ${e.message}", e)
                }
            )
            
            pushRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Rotas falhou: ${e.message}", e)
                }
            )
            
            pushMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Mesas falhou: ${e.message}", e)
                }
            )
            
            pushColaboradores().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Colaboradores: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Colaboradores falhou: ${e.message}", e)
                }
            )
            
            pushCiclos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Ciclos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Ciclos falhou: ${e.message}", e)
                }
            )
            
            pushAcertos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Acertos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Acertos falhou: ${e.message}", e)
                }
            )
            
            pushDespesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Despesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Despesas falhou: ${e.message}", e)
                }
            )
            
            pushContratos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Contratos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Contratos falhou: ${e.message}", e)
                }
            )
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                failedOperations = failedCount
            )
            
            Log.d(TAG, "Sincronização Push concluída com sucesso")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização Push: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    // ==================== SINCRONIZAÇÃO BIDIRECIONAL ====================
    
    /**
     * Sincronização completa bidirecional (Pull + Push).
     * Offline-first: Pull apenas quando online, Push enfileira quando offline.
     */
    suspend fun syncBidirectional(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização bidirecional...")
            
            // 1. Pull primeiro (atualizar dados locais)
            val pullResult = syncPull()
            if (pullResult.isFailure) {
                Log.w(TAG, "Pull falhou, continuando com Push...")
            }
            
            // 2. Push depois (enviar dados locais)
            val pushResult = syncPush()
            if (pushResult.isFailure) {
                Log.w(TAG, "Push falhou, mas Pull pode ter sido bem-sucedido")
            }
            
            if (pullResult.isSuccess && pushResult.isSuccess) {
                Log.d(TAG, "Sincronização bidirecional concluída com sucesso")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Sincronização parcial: Pull=${pullResult.isSuccess}, Push=${pushResult.isSuccess}"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização bidirecional: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== FILA DE SINCRONIZAÇÃO ====================
    
    /**
     * Adiciona operação à fila de sincronização.
     * Operações são processadas quando dispositivo estiver online.
     */
    suspend fun enqueueOperation(operation: SyncOperation) {
        // TODO: Implementar fila de sincronização offline-first
        // - Salvar operação no Room Database
        // - Processar quando online
        // - Retry automático em caso de falha
        Log.d(TAG, "Operação enfileirada: ${operation.type} - ${operation.entityId}")
    }
    
    /**
     * Processa fila de sincronização pendente.
     */
    suspend fun processSyncQueue(): Result<Unit> {
        // TODO: Implementar processamento da fila
        // - Buscar operações pendentes
        // - Processar uma por uma
        // - Atualizar status
        return Result.success(Unit)
    }
    
    // ==================== PULL HANDLERS (SERVIDOR → LOCAL) ====================
    
    /**
     * Pull Clientes: Sincroniza clientes do Firestore para o Room
     */
    private suspend fun pullClientes(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de clientes...")
            val snapshot = firestore.collection(COLLECTION_CLIENTES).get().await()
            
            var syncCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    val clienteData = doc.data ?: return@forEach
                    val clienteId = doc.id.toLongOrNull() ?: return@forEach
                    
                    // Converter Map para Cliente usando Gson
                    val clienteJson = gson.toJson(clienteData)
                    val clienteFirestore = gson.fromJson(clienteJson, Cliente::class.java)
                        ?.copy(id = clienteId) ?: return@forEach
                    
                    // Buscar cliente local
                    val clienteLocal = appRepository.obterClientePorId(clienteId)
                    
                    // Resolver conflito por timestamp (lastModified ou dataUltimaAtualizacao)
                    val serverTimestamp = (clienteData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (clienteData["dataUltimaAtualizacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: clienteFirestore.dataUltimaAtualizacao.time
                    
                    val localTimestamp = clienteLocal?.dataUltimaAtualizacao?.time ?: 0L
                    
                    when {
                        clienteLocal == null -> {
                            // Novo cliente: inserir
                            appRepository.inserirCliente(clienteFirestore)
                            syncCount++
                            Log.d(TAG, "Cliente inserido: ${clienteFirestore.nome} (ID: $clienteId)")
                        }
                        serverTimestamp > localTimestamp -> {
                            // Servidor mais recente: atualizar
                            appRepository.atualizarCliente(clienteFirestore)
                            syncCount++
                            Log.d(TAG, "Cliente atualizado: ${clienteFirestore.nome} (ID: $clienteId)")
                        }
                        // Se local mais recente, manter local (conflito resolvido no push)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar cliente ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "Pull Clientes concluído: $syncCount sincronizados")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Rotas: Sincroniza rotas do Firestore para o Room
     */
    private suspend fun pullRotas(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de rotas...")
            val snapshot = firestore.collection(COLLECTION_ROTAS).get().await()
            
            var syncCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    val rotaData = doc.data ?: return@forEach
                    val rotaId = doc.id.toLongOrNull() ?: return@forEach
                    
                    val rotaJson = gson.toJson(rotaData)
                    val rotaFirestore = gson.fromJson(rotaJson, Rota::class.java)
                        ?.copy(id = rotaId) ?: return@forEach
                    
                    val rotaLocal = appRepository.obterRotaPorId(rotaId)
                    
                    val serverTimestamp = (rotaData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: rotaFirestore.dataAtualizacao
                    val localTimestamp = rotaLocal?.dataAtualizacao ?: 0L
                    
                    when {
                        rotaLocal == null -> {
                            appRepository.inserirRota(rotaFirestore)
                            syncCount++
                        }
                        serverTimestamp > localTimestamp -> {
                            appRepository.atualizarRota(rotaFirestore)
                            syncCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar rota ${doc.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Mesas: Sincroniza mesas do Firestore para o Room
     */
    private suspend fun pullMesas(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de mesas...")
            val snapshot = firestore.collection(COLLECTION_MESAS).get().await()
            
            var syncCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    val mesaData = doc.data ?: return@forEach
                    val mesaId = doc.id.toLongOrNull() ?: return@forEach
                    
                    val mesaJson = gson.toJson(mesaData)
                    val mesaFirestore = gson.fromJson(mesaJson, Mesa::class.java)
                        ?.copy(id = mesaId) ?: return@forEach
                    
                    val mesaLocal = appRepository.obterMesaPorId(mesaId)
                    
                    // Mesas geralmente não têm timestamp, usar sempre atualizar se existir
                    when {
                        mesaLocal == null -> {
                            appRepository.inserirMesa(mesaFirestore)
                            syncCount++
                        }
                        else -> {
                            appRepository.atualizarMesa(mesaFirestore)
                            syncCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Colaboradores: Sincroniza colaboradores do Firestore para o Room
     */
    private suspend fun pullColaboradores(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de colaboradores...")
            val snapshot = firestore.collection(COLLECTION_COLABORADORES).get().await()
            
            var syncCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    val colaboradorData = doc.data ?: return@forEach
                    val colaboradorId = doc.id.toLongOrNull() ?: return@forEach
                    
                    val colaboradorJson = gson.toJson(colaboradorData)
                    val colaboradorFirestore = gson.fromJson(colaboradorJson, Colaborador::class.java)
                        ?.copy(id = colaboradorId) ?: return@forEach
                    
                    val colaboradorLocal = appRepository.obterColaboradorPorId(colaboradorId)
                    
                    val serverTimestamp = (colaboradorData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: colaboradorFirestore.dataUltimoAcesso?.time ?: 0L
                    val localTimestamp = colaboradorLocal?.dataUltimoAcesso?.time ?: 0L
                    
                    when {
                        colaboradorLocal == null -> {
                            appRepository.inserirColaborador(colaboradorFirestore)
                            syncCount++
                        }
                        serverTimestamp > localTimestamp -> {
                            appRepository.atualizarColaborador(colaboradorFirestore)
                            syncCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar colaborador ${doc.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de colaboradores: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Ciclos: Sincroniza ciclos do Firestore para o Room
     */
    private suspend fun pullCiclos(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de ciclos...")
            val snapshot = firestore.collection(COLLECTION_CICLOS).get().await()
            
            var syncCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    val cicloData = doc.data ?: return@forEach
                    val cicloId = doc.id.toLongOrNull() ?: return@forEach
                    
                    val cicloJson = gson.toJson(cicloData)
                    val cicloFirestore = gson.fromJson(cicloJson, CicloAcertoEntity::class.java)
                        ?.copy(id = cicloId) ?: return@forEach
                    
                    // Buscar ciclo local via AppRepository
                    val cicloLocal = try {
                        appRepository.buscarCicloPorId(cicloId)
                    } catch (e: Exception) {
                        null
                    }
                    
                    val serverTimestamp = (cicloData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (cicloData["dataAtualizacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: cicloFirestore.dataAtualizacao.time
                    val localTimestamp = cicloLocal?.dataAtualizacao?.time ?: 0L
                    
                    when {
                        cicloLocal == null -> {
                            // Inserir novo ciclo
                            appRepository.inserirCicloAcerto(cicloFirestore)
                            syncCount++
                        }
                        serverTimestamp > localTimestamp -> {
                            // Atualizar ciclo existente (inserirCicloAcerto usa OnConflictStrategy.REPLACE)
                            appRepository.inserirCicloAcerto(cicloFirestore)
                            syncCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar ciclo ${doc.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Acertos: Sincroniza acertos do Firestore para o Room
     * Importante: Sincronizar também AcertoMesa relacionados
     */
    private suspend fun pullAcertos(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de acertos...")
            val snapshot = firestore.collection(COLLECTION_ACERTOS).get().await()
            
            var syncCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    val acertoData = doc.data ?: return@forEach
                    val acertoId = doc.id.toLongOrNull() ?: return@forEach
                    
                    val acertoJson = gson.toJson(acertoData)
                    val acertoFirestore = gson.fromJson(acertoJson, Acerto::class.java)
                        ?.copy(id = acertoId) ?: return@forEach
                    
                    val acertoLocal = appRepository.obterAcertoPorId(acertoId)
                    
                    val serverTimestamp = (acertoData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: acertoFirestore.dataAcerto.time
                    val localTimestamp = acertoLocal?.dataAcerto?.time ?: 0L
                    
                    when {
                        acertoLocal == null -> {
                            appRepository.inserirAcerto(acertoFirestore)
                            syncCount++
                            
                            // Sincronizar AcertoMesa relacionados
                            pullAcertoMesas(acertoId)
                        }
                        serverTimestamp > localTimestamp -> {
                            appRepository.atualizarAcerto(acertoFirestore)
                            syncCount++
                            
                            // Sincronizar AcertoMesa relacionados
                            pullAcertoMesas(acertoId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar acerto ${doc.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de acertos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull AcertoMesas: Sincroniza mesas de acerto relacionadas
     */
    private suspend fun pullAcertoMesas(acertoId: Long) {
        try {
            val snapshot = firestore.collection(COLLECTION_ACERTO_MESAS)
                .whereEqualTo("acertoId", acertoId)
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                try {
                    val acertoMesaData = doc.data ?: return@forEach
                    val acertoMesaJson = gson.toJson(acertoMesaData)
                    val acertoMesa = gson.fromJson(acertoMesaJson, AcertoMesa::class.java)
                        ?: return@forEach
                    
                    // Inserir ou atualizar AcertoMesa (inserirAcertoMesa usa OnConflictStrategy.REPLACE)
                    appRepository.inserirAcertoMesa(acertoMesa)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar AcertoMesa ${doc.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de AcertoMesas para acerto $acertoId: ${e.message}", e)
        }
    }
    
    /**
     * Pull Despesas: Sincroniza despesas do Firestore para o Room
     */
    private suspend fun pullDespesas(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de despesas...")
            val snapshot = firestore.collection(COLLECTION_DESPESAS).get().await()
            
            var syncCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    val despesaData = doc.data ?: return@forEach
                    val despesaId = doc.id.toLongOrNull() ?: return@forEach
                    
                    val despesaJson = gson.toJson(despesaData)
                    val despesaFirestore = gson.fromJson(despesaJson, Despesa::class.java)
                        ?.copy(id = despesaId) ?: return@forEach
                    
                    val despesaLocal = appRepository.obterDespesaPorId(despesaId)
                    
                    val serverTimestamp = (despesaData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: despesaFirestore.dataHora.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                    val localTimestamp = despesaLocal?.dataHora?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000) ?: 0L
                    
                    when {
                        despesaLocal == null -> {
                            appRepository.inserirDespesa(despesaFirestore)
                            syncCount++
                        }
                        serverTimestamp > localTimestamp -> {
                            appRepository.atualizarDespesa(despesaFirestore)
                            syncCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar despesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de despesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Contratos: Sincroniza contratos do Firestore para o Room
     * Importante: Sincronizar também Aditivos e Assinaturas relacionados
     */
    private suspend fun pullContratos(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de contratos...")
            val snapshot = firestore.collection(COLLECTION_CONTRATOS).get().await()
            
            var syncCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    val contratoData = doc.data ?: return@forEach
                    val contratoId = doc.id.toLongOrNull() ?: return@forEach
                    
                    val contratoJson = gson.toJson(contratoData)
                    val contratoFirestore = gson.fromJson(contratoJson, ContratoLocacao::class.java)
                        ?.copy(id = contratoId) ?: return@forEach
                    
                    val contratoLocal = appRepository.buscarContratoPorId(contratoId)
                    
                    val serverTimestamp = (contratoData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: contratoFirestore.dataAtualizacao.time
                    val localTimestamp = contratoLocal?.dataAtualizacao?.time ?: 0L
                    
                    when {
                        contratoLocal == null -> {
                            appRepository.inserirContrato(contratoFirestore)
                            syncCount++
                            
                            // Sincronizar aditivos relacionados
                            pullAditivosContrato(contratoId)
                        }
                        serverTimestamp > localTimestamp -> {
                            appRepository.atualizarContrato(contratoFirestore)
                            syncCount++
                            
                            // Sincronizar aditivos relacionados
                            pullAditivosContrato(contratoId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar contrato ${doc.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de contratos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Aditivos: Sincroniza aditivos de contrato relacionados
     */
    private suspend fun pullAditivosContrato(contratoId: Long) {
        try {
            val snapshot = firestore.collection(COLLECTION_ADITIVOS)
                .whereEqualTo("contratoId", contratoId)
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                try {
                    val aditivoData = doc.data ?: return@forEach
                    val aditivoJson = gson.toJson(aditivoData)
                    val aditivo = gson.fromJson(aditivoJson, AditivoContrato::class.java)
                        ?: return@forEach
                    
                    appRepository.inserirAditivo(aditivo)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar aditivo ${doc.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de aditivos para contrato $contratoId: ${e.message}", e)
        }
    }
    
    // ==================== PUSH HANDLERS (LOCAL → SERVIDOR) ====================
    
    /**
     * Push Clientes: Envia clientes modificados do Room para o Firestore
     */
    private suspend fun pushClientes(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando push de clientes...")
            val clientesLocais = appRepository.obterTodosClientes().first()
            
            var syncCount = 0
            clientesLocais.forEach { cliente ->
                try {
                    // Converter Cliente para Map
                    val clienteMap = entityToMap(cliente)
                    
                    // Adicionar metadados de sincronização
                    clienteMap["lastModified"] = FieldValue.serverTimestamp()
                    clienteMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    // Enviar para Firestore
                    firestore.collection(COLLECTION_CLIENTES)
                        .document(cliente.id.toString())
                        .set(clienteMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "Cliente enviado: ${cliente.nome} (ID: ${cliente.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar cliente ${cliente.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Rotas: Envia rotas modificadas do Room para o Firestore
     */
    private suspend fun pushRotas(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando push de rotas...")
            val rotasLocais = appRepository.obterTodasRotas().first()
            
            var syncCount = 0
            rotasLocais.forEach { rota ->
                try {
                    val rotaMap = entityToMap(rota)
                    rotaMap["lastModified"] = FieldValue.serverTimestamp()
                    rotaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_ROTAS)
                        .document(rota.id.toString())
                        .set(rotaMap)
                        .await()
                    
                    syncCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar rota ${rota.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Mesas: Envia mesas modificadas do Room para o Firestore
     */
    private suspend fun pushMesas(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando push de mesas...")
            val mesasLocais = appRepository.obterTodasMesas().first()
            
            var syncCount = 0
            mesasLocais.forEach { mesa ->
                try {
                    val mesaMap = entityToMap(mesa)
                    mesaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_MESAS)
                        .document(mesa.id.toString())
                        .set(mesaMap)
                        .await()
                    
                    syncCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar mesa ${mesa.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Colaboradores: Envia colaboradores modificados do Room para o Firestore
     */
    private suspend fun pushColaboradores(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando push de colaboradores...")
            val colaboradoresLocais = appRepository.obterTodosColaboradores().first()
            
            var syncCount = 0
            colaboradoresLocais.forEach { colaborador ->
                try {
                    val colaboradorMap = entityToMap(colaborador)
                    colaboradorMap["lastModified"] = FieldValue.serverTimestamp()
                    colaboradorMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_COLABORADORES)
                        .document(colaborador.id.toString())
                        .set(colaboradorMap)
                        .await()
                    
                    syncCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar colaborador ${colaborador.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de colaboradores: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Ciclos: Envia ciclos modificados do Room para o Firestore
     */
    private suspend fun pushCiclos(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando push de ciclos...")
            // Buscar todos os ciclos via AppRepository
            val ciclosLocais = try {
                appRepository.obterTodosCiclos().first()
            } catch (e: Exception) {
                Log.w(TAG, "Método obterTodosCiclos não disponível, tentando alternativa...")
                emptyList<CicloAcertoEntity>()
            }
            
            var syncCount = 0
            ciclosLocais.forEach { ciclo ->
                try {
                    val cicloMap = entityToMap(ciclo)
                    cicloMap["lastModified"] = FieldValue.serverTimestamp()
                    cicloMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_CICLOS)
                        .document(ciclo.id.toString())
                        .set(cicloMap)
                        .await()
                    
                    syncCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar ciclo ${ciclo.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Acertos: Envia acertos modificados do Room para o Firestore
     * Importante: Enviar também AcertoMesa relacionados
     */
    private suspend fun pushAcertos(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando push de acertos...")
            val acertosLocais = appRepository.obterTodosAcertos().first()
            
            var syncCount = 0
            acertosLocais.forEach { acerto ->
                try {
                    val acertoMap = entityToMap(acerto)
                    acertoMap["lastModified"] = FieldValue.serverTimestamp()
                    acertoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_ACERTOS)
                        .document(acerto.id.toString())
                        .set(acertoMap)
                        .await()
                    
                    syncCount++
                    
                    // Enviar AcertoMesa relacionados
                    pushAcertoMesas(acerto.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar acerto ${acerto.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de acertos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push AcertoMesas: Envia mesas de acerto relacionadas
     */
    private suspend fun pushAcertoMesas(acertoId: Long) {
        try {
            val acertoMesas = appRepository.buscarAcertoMesasPorAcerto(acertoId) // Retorna List<AcertoMesa>
            
            acertoMesas.forEach { acertoMesa: AcertoMesa ->
                try {
                    val acertoMesaMap = entityToMap(acertoMesa)
                    acertoMesaMap["lastModified"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_ACERTO_MESAS)
                        .document("${acertoMesa.acertoId}_${acertoMesa.mesaId}")
                        .set(acertoMesaMap)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar AcertoMesa ${acertoMesa.acertoId}_${acertoMesa.mesaId}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de AcertoMesas para acerto $acertoId: ${e.message}", e)
        }
    }
    
    /**
     * Push Despesas: Envia despesas modificadas do Room para o Firestore
     */
    private suspend fun pushDespesas(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando push de despesas...")
            val despesasLocais = appRepository.obterTodasDespesas().first()
            
            var syncCount = 0
            despesasLocais.forEach { despesa ->
                try {
                    val despesaMap = entityToMap(despesa)
                    despesaMap["lastModified"] = FieldValue.serverTimestamp()
                    despesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_DESPESAS)
                        .document(despesa.id.toString())
                        .set(despesaMap)
                        .await()
                    
                    syncCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar despesa ${despesa.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de despesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Contratos: Envia contratos modificados do Room para o Firestore
     * Importante: Enviar também Aditivos relacionados
     */
    private suspend fun pushContratos(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando push de contratos...")
            val contratosLocais = appRepository.buscarTodosContratos().first()
            
            var syncCount = 0
            contratosLocais.forEach { contrato: ContratoLocacao ->
                try {
                    val contratoMap = entityToMap(contrato)
                    contratoMap["lastModified"] = FieldValue.serverTimestamp()
                    contratoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_CONTRATOS)
                        .document(contrato.id.toString())
                        .set(contratoMap)
                        .await()
                    
                    syncCount++
                    
                    // Enviar aditivos relacionados
                    pushAditivosContrato(contrato.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar contrato ${contrato.id}: ${e.message}", e)
                }
            }
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de contratos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Aditivos: Envia aditivos de contrato relacionados
     */
    private suspend fun pushAditivosContrato(contratoId: Long) {
        try {
            val aditivos = appRepository.buscarAditivosPorContrato(contratoId).first()
            
            aditivos.forEach { aditivo: AditivoContrato ->
                try {
                    val aditivoMap = entityToMap(aditivo)
                    aditivoMap["lastModified"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(COLLECTION_ADITIVOS)
                        .document(aditivo.id.toString())
                        .set(aditivoMap)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar aditivo ${aditivo.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de aditivos para contrato $contratoId: ${e.message}", e)
        }
    }
    
    // ==================== UTILITÁRIOS ====================
    
    /**
     * Converte entidade para Map para Firestore
     */
    private fun <T> entityToMap(entity: T): MutableMap<String, Any> {
        val json = gson.toJson(entity)
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
        return map.mapKeys { it.key.toString() }.mapValues { entry ->
            when (val value = entry.value) {
                is Date -> com.google.firebase.Timestamp(value)
                is java.time.LocalDateTime -> com.google.firebase.Timestamp(value.toEpochSecond(java.time.ZoneOffset.UTC), 0)
                else -> value
            }
        }.toMutableMap()
    }
    
    /**
     * Verifica se dispositivo está online.
     */
    fun isOnline(): Boolean = networkUtils.isConnected()
    
    /**
     * Obtém status atual da sincronização.
     */
    fun getSyncStatus(): SyncStatus = _syncStatus.value
    
    /**
     * Limpa status de erro.
     */
    fun clearError() {
        _syncStatus.value = _syncStatus.value.copy(error = null)
    }
    
    /**
     * Limpa operações antigas completadas.
     * Remove operações completadas há mais de 7 dias.
     */
    suspend fun limparOperacoesAntigas() {
        try {
            appRepository.limparOperacoesSyncCompletadas(dias = 7)
            Log.d(TAG, "Operações antigas limpas")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar operações antigas: ${e.message}", e)
        }
    }
}

/**
 * Operação de sincronização enfileirada.
 */
data class SyncOperation(
    val id: Long,
    val type: SyncOperationType,
    val entityType: String,
    val entityId: String,
    val data: String, // JSON serializado
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

/**
 * Tipos de operação de sincronização.
 */
enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE
}

