package com.example.gestaobilhares.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
// Firestore/Auth/Coroutines
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
// JSON
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ✅ FASE 3C: Gerenciador de Sincronização V2
 * Utiliza as novas entidades SyncLog, SyncQueue e SyncConfig
 * Seguindo melhores práticas Android 2025
 */
class SyncManagerV2(
    private val context: Context,
    private val appRepository: AppRepository,
    private val database: AppDatabase
) {
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isSyncing = AtomicBoolean(false)
    
    // DAOs das novas entidades
    private val syncLogDao = database.syncLogDao()
    private val syncQueueDao = database.syncQueueDao()
    private val syncConfigDao = database.syncConfigDao()
    
    // LiveData para status
    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus
    
    private val _pendingOperationsCount = MutableLiveData<Int>()
    val pendingOperationsCount: LiveData<Int> = _pendingOperationsCount
    
    private val _lastSyncTime = MutableLiveData<Long>()
    val lastSyncTime: LiveData<Long> = _lastSyncTime

    // Firebase Firestore e utilitários
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val gson: Gson by lazy { Gson() }

    init {
        syncScope.launch {
            initializeSyncConfig()
        }
        startPeriodicSync()
        observePendingOperations()
    }

    /**
     * Inicializar configurações padrão de sincronização
     */
    private suspend fun initializeSyncConfig() {
        try {
            syncConfigDao.inicializarConfiguracoesPadrao(System.currentTimeMillis())
            
            // Garantir que exista um empresa_id padrão
            val empresaConfig = syncConfigDao.buscarSyncConfigPorChave("empresa_id")
            if (empresaConfig == null) {
                val now = System.currentTimeMillis()
                // empresa_001 é o padrão visto no console do Firestore
                syncConfigDao.atualizarValorConfig("empresa_id", "empresa_001", now)
                android.util.Log.d("SyncManagerV2", "✅ empresa_id configurado como 'empresa_001'")
            } else {
                android.util.Log.d("SyncManagerV2", "✅ empresa_id já configurado: ${empresaConfig.value}")
            }
            
            android.util.Log.d("SyncManagerV2", "Configurações de sincronização inicializadas")
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro ao inicializar configurações: ${e.message}")
        }
    }

    /**
     * Observar contagem de operações pendentes
     */
    private fun observePendingOperations() {
        syncScope.launch {
            syncQueueDao.contarOperacoesPendentes().let { count ->
                _pendingOperationsCount.postValue(count)
            }
        }
    }

    /**
     * Adicionar operação à fila de sincronização
     */
    suspend fun addToSyncQueue(
        entityType: String,
        entityId: Long,
        operation: String,
        payload: String,
        priority: Int = 0
    ) {
        try {
            val syncQueue = SyncQueue(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                createdAt = Date(),
                scheduledFor = Date(), // Processar imediatamente
                retryCount = 0,
                status = "PENDING",
                priority = priority
            )
            
            syncQueueDao.inserirSyncQueue(syncQueue)
            
            // Log da operação
            logSyncOperation(entityType, entityId, operation, "PENDING", null, payload)
            
            // Atualizar contagem
            observePendingOperations()
            
            android.util.Log.d("SyncManagerV2", "Operação adicionada à fila: $entityType:$entityId")
            
            // Tentar sincronizar se online
            if (isOnline()) {
                processSyncQueue()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "Erro ao adicionar à fila: ${e.message}")
        }
    }

    /**
     * Processar fila de sincronização
     */
    suspend fun processSyncQueue() {
        if (isSyncing.get() || !isOnline()) return
        if (!isAuthenticated()) {
            android.util.Log.w("SyncManagerV2", "Ignorando sync: usuário não autenticado no Firebase")
            return
        }
        
        isSyncing.set(true)
        _syncStatus.postValue(SyncStatus.SYNCING)
        
        try {
            val currentTime = System.currentTimeMillis()
            val operations = syncQueueDao.buscarOperacoesAgendadas(currentTime).first()
            
            android.util.Log.d("SyncManagerV2", "📋 Processando ${operations.size} operações")
            if (operations.isEmpty()) {
                android.util.Log.w("SyncManagerV2", "⚠️ Nenhuma operação pendente na fila de sincronização")
                return
            }
            
            operations.forEachIndexed { index, op ->
                android.util.Log.d("SyncManagerV2", "   [$index] ${op.entityType}:${op.entityId} - ${op.operation} (${op.status})")
            }
            
            for (operation in operations) {
                try {
                    // Marcar como processando
                    syncQueueDao.marcarComoProcessando(operation.id)
                    
                    // Aplicar operação real no Firestore
                    val success = applyOperationToFirestore(operation)
                    
                    if (success) {
                        // Marcar como concluída
                        syncQueueDao.marcarComoConcluida(operation.id)
                        logSyncOperation(
                            operation.entityType,
                            operation.entityId,
                            operation.operation,
                            "SUCCESS",
                            null,
                            operation.payload
                        )
                    } else {
                        // Marcar como falhou e agendar retry
                        val nextRetry = currentTime + (30000 * (operation.retryCount + 1)) // 30s, 60s, 90s
                        syncQueueDao.marcarComoFalhou(operation.id, nextRetry)
                        logSyncOperation(
                            operation.entityType,
                            operation.entityId,
                            operation.operation,
                            "FAILED",
                            "Falha ao aplicar operação no Firestore",
                            operation.payload
                        )
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "Erro ao processar operação ${operation.id}: ${e.message}")
                    syncQueueDao.marcarComoFalhou(operation.id, currentTime + 60000)
                }
            }
            
            // Atualizar timestamp da última sincronização
            updateLastSyncTimestamp()
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "Erro ao processar fila: ${e.message}")
            _syncStatus.postValue(SyncStatus.ERROR)
        } finally {
            isSyncing.set(false)
            _syncStatus.postValue(SyncStatus.SYNCED)
            observePendingOperations()
        }
    }

    /**
     * Aplicar operação no Firestore usando payload JSON armazenado na fila.
     * Mantém o app offline-first: Room continua fonte da verdade, Firestore é o espelho.
     */
    private suspend fun applyOperationToFirestore(operation: SyncQueue): Boolean {
        return try {
            val empresaId = getEmpresaId()
            val collection = getCollectionName(operation.entityType)
            val docId = operation.entityId.toString()

            android.util.Log.d("SyncManagerV2", "🔄 Aplicando operação no Firestore:")
            android.util.Log.d("SyncManagerV2", "   Empresa ID: $empresaId")
            android.util.Log.d("SyncManagerV2", "   Collection: $collection")
            android.util.Log.d("SyncManagerV2", "   Document ID: $docId")
            android.util.Log.d("SyncManagerV2", "   Operation: ${operation.operation}")
            android.util.Log.d("SyncManagerV2", "   Payload: ${operation.payload}")

            // Converter o payload JSON em Map<String, Any?> para enviar ao Firestore
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val payloadMap: Map<String, Any?> = try {
                gson.fromJson(operation.payload, mapType) ?: emptyMap()
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "Payload inválido para ${operation.entityType}:${operation.entityId} -> ${e.message}")
                emptyMap()
            }

            android.util.Log.d("SyncManagerV2", "   Payload Map: $payloadMap")

            // ✅ CORREÇÃO: Usar ID do Room como campo, não como documento ID
            val docRef = firestore
                .collection("empresas")
                .document(empresaId)
                .collection(collection)
                .document() // Deixar Firestore gerar ID automático

            android.util.Log.d("SyncManagerV2", "   Firestore Path: empresas/$empresaId/$collection/[AUTO_ID]")

            when (operation.operation.uppercase(Locale.getDefault())) {
                "CREATE", "UPDATE" -> {
                    // ✅ CORREÇÃO: Adicionar roomId ao payload para referência
                    val payloadWithRoomId = payloadMap.toMutableMap().apply {
                        put("roomId", operation.entityId)
                        put("syncTimestamp", System.currentTimeMillis())
                    }
                    
                    // Merge para não sobrescrever campos inexistentes
                    android.util.Log.d("SyncManagerV2", "   Executando SET com merge...")
                    android.util.Log.d("SyncManagerV2", "   Payload final: $payloadWithRoomId")
                    docRef.set(payloadWithRoomId, SetOptions.merge()).await()
                    android.util.Log.d("SyncManagerV2", "   ✅ SET executado com sucesso")
                }
                "DELETE" -> {
                    android.util.Log.d("SyncManagerV2", "   Executando DELETE...")
                    docRef.delete().await()
                    android.util.Log.d("SyncManagerV2", "   ✅ DELETE executado com sucesso")
                }
                else -> {
                    android.util.Log.w("SyncManagerV2", "Operação desconhecida: ${operation.operation}")
                    return false
                }
            }
            android.util.Log.d("SyncManagerV2", "✅ Operação ${operation.operation} concluída com sucesso")
            true
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Falha no Firestore: ${e.message}", e)
            android.util.Log.e("SyncManagerV2", "   Stack trace: ${e.stackTraceToString()}")
            false
        }
    }

    /**
     * Log de operação de sincronização
     */
    private suspend fun logSyncOperation(
        entityType: String,
        entityId: Long,
        operation: String,
        status: String,
        errorMessage: String?,
        payload: String?
    ) {
        try {
            val syncLog = SyncLog(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                syncStatus = status,
                timestamp = Date(),
                errorMessage = errorMessage,
                payload = payload
            )
            
            syncLogDao.inserirSyncLog(syncLog)
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro ao logar operação: ${e.message}")
        }
    }

    /**
     * Verificar se está online
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /** Verifica se há usuário autenticado no Firebase (regras exigem auth) */
    private fun isAuthenticated(): Boolean {
        return try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (_: Exception) { false }
    }

    /** Obtém o ID da empresa para particionar os dados no Firestore */
    private suspend fun getEmpresaId(): String {
        return try {
            val cfg = syncConfigDao.buscarSyncConfigPorChave("empresa_id")
            val empresaId = cfg?.value ?: "empresa_001"
            android.util.Log.d("SyncManagerV2", "🏢 Empresa ID obtido: $empresaId (config: ${cfg?.key})")
            empresaId
        } catch (e: Exception) { 
            android.util.Log.w("SyncManagerV2", "Erro ao obter empresa_id: ${e.message}, usando padrão")
            "empresa_001" 
        }
    }

    /** Mapeia tipos de entidades para coleções do Firestore */
    private fun getCollectionName(entityType: String): String = when (entityType.lowercase(Locale.getDefault())) {
        "cliente" -> "clientes"
        "acerto" -> "acertos"
        "mesa" -> "mesas"
        "rota" -> "rotas"
        "colaborador" -> "colaboradores"
        else -> entityType.lowercase(Locale.getDefault()) + "s"
    }

    /**
     * Iniciar sincronização periódica
     */
    private fun startPeriodicSync() {
        syncScope.launch {
            while (isActive) {
                delay(300000) // 5 minutos
                
                if (isOnline() && !isSyncing.get()) {
                    processSyncQueue()
                }
            }
        }
    }

    /**
     * Atualizar timestamp da última sincronização
     */
    private suspend fun updateLastSyncTimestamp() {
        try {
            val currentTime = System.currentTimeMillis()
            syncConfigDao.atualizarUltimoTimestampSync("last_sync_timestamp_global", currentTime.toString(), currentTime)
            _lastSyncTime.postValue(currentTime)
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro ao atualizar timestamp: ${e.message}")
        }
    }

    /**
     * Forçar sincronização manual (PUSH + PULL)
     */
    fun forceSync() {
        syncScope.launch {
            try {
                android.util.Log.d("SyncManagerV2", "🚀 INICIANDO SINCRONIZAÇÃO COMPLETA (PUSH + PULL)")
                
                // 1. PUSH: Enviar dados pendentes para Firestore
                android.util.Log.d("SyncManagerV2", "📤 Fase 1: PUSH SYNC (App → Firestore)")
                processSyncQueue()
                
                // Aguardar um pouco para garantir que PUSH termine
                delay(1000)
                
                // 2. PULL: Baixar dados do Firestore para o app
                android.util.Log.d("SyncManagerV2", "📥 Fase 2: PULL SYNC (Firestore → App)")
                pullFromFirestore()
                
                android.util.Log.d("SyncManagerV2", "✅ SINCRONIZAÇÃO COMPLETA FINALIZADA")
                
            } catch (e: Exception) {
                android.util.Log.e("SyncManagerV2", "❌ Erro na sincronização completa: ${e.message}", e)
            }
        }
    }

    /**
     * PULL SYNC: Baixar dados do Firestore para o app
     */
    private suspend fun pullFromFirestore() {
        android.util.Log.d("SyncManagerV2", "🔍 Verificando condições para PULL SYNC...")
        android.util.Log.d("SyncManagerV2", "   Online: ${isOnline()}")
        android.util.Log.d("SyncManagerV2", "   Autenticado: ${isAuthenticated()}")
        
        if (!isOnline()) {
            android.util.Log.w("SyncManagerV2", "❌ PULL SYNC cancelado: Sem conexão")
            return
        }
        
        if (!isAuthenticated()) {
            android.util.Log.w("SyncManagerV2", "❌ PULL SYNC cancelado: Usuário não autenticado")
            return
        }
        
        try {
            android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL SYNC do Firestore")
            
            val empresaId = getEmpresaId()
            android.util.Log.d("SyncManagerV2", "🏢 Empresa ID para PULL: $empresaId")
            
            // 1. PRIMEIRO: Baixar rotas do Firestore (dependência dos clientes)
            android.util.Log.d("SyncManagerV2", "🔄 Fase 1: Sincronizando ROTAS...")
            pullRotasFromFirestore(empresaId)
            delay(500) // Aguardar rotas serem inseridas
            
            // 2. SEGUNDO: Baixar clientes do Firestore (dependem das rotas)
            android.util.Log.d("SyncManagerV2", "🔄 Fase 2: Sincronizando CLIENTES...")
            pullClientesFromFirestore(empresaId)
            delay(500) // Aguardar clientes serem inseridos
            
            // 3. TERCEIRO: Baixar mesas do Firestore (dependem dos clientes)
            android.util.Log.d("SyncManagerV2", "🔄 Fase 3: Sincronizando MESAS...")
            pullMesasFromFirestore(empresaId)
            delay(500) // Aguardar mesas serem inseridas
            
            // 4. QUARTO: Baixar acertos do Firestore (dependem dos clientes)
            android.util.Log.d("SyncManagerV2", "🔄 Fase 4: Sincronizando ACERTOS...")
            pullAcertosFromFirestore(empresaId)
            
            android.util.Log.d("SyncManagerV2", "✅ PULL SYNC concluído com sucesso")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro no PULL SYNC: ${e.message}", e)
        }
    }
    
    /**
     * Baixar clientes do Firestore
     */
    private suspend fun pullClientesFromFirestore(empresaId: String) {
        try {
            android.util.Log.d("SyncManagerV2", "📥 Baixando clientes do Firestore...")
            android.util.Log.d("SyncManagerV2", "   Caminho: empresas/$empresaId/clientes")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("clientes")
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📊 Encontrados ${snapshot.size()} clientes no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("SyncManagerV2", "⚠️ Nenhum cliente encontrado no Firestore")
                return
            }
            
            var clientesSincronizados = 0
            var clientesExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val nome = data["nome"] as? String
                    
                    android.util.Log.d("SyncManagerV2", "🔍 Processando cliente: $nome (Room ID: $roomId)")
                    
                    if (roomId != null && nome != null) {
                        // Verificar se já existe no Room
                        val clienteExistente = appRepository.obterClientePorId(roomId)
                        
                        if (clienteExistente == null) {
                            // Criar cliente no Room baseado nos dados do Firestore
                            val cliente = com.example.gestaobilhares.data.entities.Cliente(
                                id = roomId,
                                nome = nome,
                                telefone = data["telefone"] as? String,
                                endereco = data["endereco"] as? String ?: "",
                                rotaId = (data["rotaId"] as? Double)?.toLong() ?: 1L,
                                ativo = data["ativo"] as? Boolean ?: true,
                                dataCadastro = java.util.Date() // Usar data atual como fallback
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val clienteDao = database.clienteDao()
                            clienteDao.inserir(cliente)
                            
                            clientesSincronizados++
                            android.util.Log.d("SyncManagerV2", "✅ Cliente sincronizado: ${cliente.nome} (ID: $roomId)")
                        } else {
                            clientesExistentes++
                            android.util.Log.d("SyncManagerV2", "⏭️ Cliente já existe: ${clienteExistente.nome} (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("SyncManagerV2", "⚠️ Cliente sem roomId ou nome: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar cliente ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Clientes:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $clientesSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $clientesExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar clientes: ${e.message}", e)
        }
    }
    
    /**
     * Baixar acertos do Firestore
     */
    private suspend fun pullAcertosFromFirestore(empresaId: String) {
        try {
            android.util.Log.d("SyncManagerV2", "📥 Baixando acertos do Firestore...")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("acertos")
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📊 Encontrados ${snapshot.size()} acertos no Firestore")
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    
                    if (roomId != null) {
                        // Verificar se já existe no Room
                        val acertoExistente = appRepository.obterAcertoPorId(roomId)
                        
                        if (acertoExistente == null) {
                            // Criar acerto no Room baseado nos dados do Firestore
                            val acerto = com.example.gestaobilhares.data.entities.Acerto(
                                id = roomId,
                                clienteId = (data["clienteId"] as? Double)?.toLong() ?: 0L,
                                periodoInicio = java.util.Date(),
                                periodoFim = java.util.Date(),
                                valorRecebido = (data["valorRecebido"] as? Double) ?: 0.0,
                                debitoAtual = (data["debitoAtual"] as? Double) ?: 0.0,
                                dataAcerto = java.util.Date(),
                                observacoes = data["observacoes"] as? String,
                                metodosPagamentoJson = data["metodosPagamentoJson"] as? String
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val acertoDao = database.acertoDao()
                            acertoDao.inserir(acerto)
                            
                            android.util.Log.d("SyncManagerV2", "✅ Acerto sincronizado: ID $roomId")
                        } else {
                            android.util.Log.d("SyncManagerV2", "⏭️ Acerto já existe: ID $roomId")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "Erro ao processar acerto ${document.id}: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "Erro ao baixar acertos: ${e.message}", e)
        }
    }
    
    /**
     * Baixar mesas do Firestore
     */
    private suspend fun pullMesasFromFirestore(empresaId: String) {
        try {
            android.util.Log.d("SyncManagerV2", "📥 Baixando mesas do Firestore...")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("mesas")
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📊 Encontradas ${snapshot.size()} mesas no Firestore")
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    
                    if (roomId != null) {
                        // Verificar se já existe no Room
                        val mesaExistente = appRepository.obterMesaPorId(roomId)
                        
                        if (mesaExistente == null) {
                            // Criar mesa no Room baseado nos dados do Firestore
                            val mesa = com.example.gestaobilhares.data.entities.Mesa(
                                id = roomId,
                                numero = (data["numero"] as? String) ?: "0",
                                clienteId = (data["clienteId"] as? Double)?.toLong(),
                                ativa = data["ativa"] as? Boolean ?: true
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val mesaDao = database.mesaDao()
                            mesaDao.inserir(mesa)
                            
                            android.util.Log.d("SyncManagerV2", "✅ Mesa sincronizada: ${mesa.numero} (ID: $roomId)")
                        } else {
                            android.util.Log.d("SyncManagerV2", "⏭️ Mesa já existe: ${mesaExistente.numero} (ID: $roomId)")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "Erro ao processar mesa ${document.id}: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "Erro ao baixar mesas: ${e.message}", e)
        }
    }
    
    /**
     * Baixar rotas do Firestore
     */
    private suspend fun pullRotasFromFirestore(empresaId: String) {
        try {
            android.util.Log.d("SyncManagerV2", "📥 Baixando rotas do Firestore...")
            android.util.Log.d("SyncManagerV2", "   Caminho: empresas/$empresaId/rotas")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("rotas")
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📊 Encontradas ${snapshot.size()} rotas no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("SyncManagerV2", "⚠️ Nenhuma rota encontrada no Firestore")
                return
            }
            
            var rotasSincronizadas = 0
            var rotasExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val nome = data["nome"] as? String
                    
                    android.util.Log.d("SyncManagerV2", "🔍 Processando rota: $nome (Room ID: $roomId)")
                    
                    if (roomId != null && nome != null) {
                        // Verificar se já existe no Room
                        val rotaExistente = appRepository.buscarRotaPorId(roomId)
                        
                        if (rotaExistente == null) {
                            // Criar rota no Room baseado nos dados do Firestore
                            val rota = com.example.gestaobilhares.data.entities.Rota(
                                id = roomId,
                                nome = nome,
                                descricao = data["descricao"] as? String ?: "",
                                ativa = data["ativa"] as? Boolean ?: true,
                                dataCriacao = System.currentTimeMillis()
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val rotaDao = database.rotaDao()
                            rotaDao.insertRota(rota)
                            
                            rotasSincronizadas++
                            android.util.Log.d("SyncManagerV2", "✅ Rota sincronizada: ${rota.nome} (ID: $roomId)")
                        } else {
                            rotasExistentes++
                            android.util.Log.d("SyncManagerV2", "⏭️ Rota já existe: ${rotaExistente.nome} (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("SyncManagerV2", "⚠️ Rota sem roomId ou nome: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar rota ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Rotas:")
            android.util.Log.d("SyncManagerV2", "   Sincronizadas: $rotasSincronizadas")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $rotasExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar rotas: ${e.message}", e)
        }
    }

    /**
     * Limpar logs antigos
     */
    suspend fun cleanupOldLogs() {
        try {
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 dias
            val deletedLogs = syncLogDao.deletarSyncLogsAntigos(cutoffTime)
            val deletedQueue = syncQueueDao.limparOperacoesConcluidas(cutoffTime)
            
            android.util.Log.d("SyncManagerV2", "Limpeza: $deletedLogs logs, $deletedQueue operações removidas")
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro na limpeza: ${e.message}")
        }
    }

    /**
     * Obter estatísticas de sincronização
     */
    suspend fun getSyncStats(): SyncStats {
        return try {
            val pendingCount = syncQueueDao.contarOperacoesPendentes()
            val failedCount = syncQueueDao.contarOperacoesPorStatus("FAILED")
            val completedCount = syncQueueDao.contarOperacoesPorStatus("COMPLETED")
            
            android.util.Log.d("SyncManagerV2", "📊 Estatísticas de sincronização:")
            android.util.Log.d("SyncManagerV2", "   Pendentes: $pendingCount")
            android.util.Log.d("SyncManagerV2", "   Falhas: $failedCount")
            android.util.Log.d("SyncManagerV2", "   Concluídas: $completedCount")
            android.util.Log.d("SyncManagerV2", "   Online: ${isOnline()}")
            android.util.Log.d("SyncManagerV2", "   Sincronizando: ${isSyncing.get()}")
            
            SyncStats(
                pendingOperations = pendingCount,
                failedOperations = failedCount,
                completedOperations = completedCount,
                isOnline = isOnline(),
                isSyncing = isSyncing.get()
            )
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro ao obter estatísticas: ${e.message}")
            SyncStats(0, 0, 0, false, false)
        }
    }

    /**
     * Debug: Listar todas as operações na fila de sincronização
     */
    suspend fun debugSyncQueue() {
        try {
            val allOperations = syncQueueDao.buscarOperacoesPorStatus("PENDING").first()
            val totalPending = syncQueueDao.contarOperacoesPendentes()
            val todasOperacoes = syncQueueDao.buscarTodasOperacoes(50).first()
            
            android.util.Log.d("SyncManagerV2", "🔍 DEBUG - Fila de sincronização:")
            android.util.Log.d("SyncManagerV2", "   Total de operações PENDING (Flow): ${allOperations.size}")
            android.util.Log.d("SyncManagerV2", "   Total de operações PENDING (Count): $totalPending")
            android.util.Log.d("SyncManagerV2", "   Total de operações na fila: ${todasOperacoes.size}")
            
            todasOperacoes.forEachIndexed { index, op ->
                android.util.Log.d("SyncManagerV2", "   [$index] ID: ${op.id}")
                android.util.Log.d("SyncManagerV2", "        Tipo: ${op.entityType}")
                android.util.Log.d("SyncManagerV2", "        Entity ID: ${op.entityId}")
                android.util.Log.d("SyncManagerV2", "        Operação: ${op.operation}")
                android.util.Log.d("SyncManagerV2", "        Status: ${op.status}")
                android.util.Log.d("SyncManagerV2", "        Payload: ${op.payload}")
                android.util.Log.d("SyncManagerV2", "        Criado: ${op.createdAt}")
                android.util.Log.d("SyncManagerV2", "        Agendado: ${op.scheduledFor}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "Erro ao debugar fila: ${e.message}", e)
        }
    }

    /**
     * Destruir recursos
     */
    fun destroy() {
        syncScope.cancel()
    }
}

/**
 * Status de sincronização
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR
}

/**
 * Estatísticas de sincronização
 */
data class SyncStats(
    val pendingOperations: Int,
    val failedOperations: Int,
    val completedOperations: Int,
    val isOnline: Boolean,
    val isSyncing: Boolean
)
