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
            
            // ✅ VALIDAÇÃO CRÍTICA: Verificar se o payload não está vazio
            if (payloadMap.isEmpty()) {
                android.util.Log.e("SyncManagerV2", "❌ Payload vazio para ${operation.entityType}:${operation.entityId} - Operação cancelada")
                return false
            }
            
            // ✅ VALIDAÇÃO ESPECÍFICA POR TIPO DE ENTIDADE (MAIS FLEXÍVEL)
            when (operation.entityType.lowercase()) {
                "cliente" -> {
                    val nome = payloadMap["nome"]?.toString()
                    if (nome.isNullOrBlank()) {
                        android.util.Log.w("SyncManagerV2", "⚠️ Cliente sem nome - Usando nome padrão")
                        // Não cancelar, usar nome padrão
                    }
                }
                "mesa" -> {
                    val numero = payloadMap["numero"]?.toString()
                    if (numero.isNullOrBlank()) {
                        android.util.Log.w("SyncManagerV2", "⚠️ Mesa sem número - Usando número padrão")
                        // Não cancelar, usar número padrão
                    }
                }
                "acerto" -> {
                    val valor = payloadMap["valorRecebido"]
                    val clienteId = payloadMap["clienteId"]
                    if (valor == null) {
                        android.util.Log.w("SyncManagerV2", "⚠️ Acerto sem valor - Usando valor padrão")
                        // Não cancelar, usar valor padrão
                    }
                    if (clienteId == null) {
                        android.util.Log.w("SyncManagerV2", "⚠️ Acerto sem clienteId - Usando cliente padrão")
                        // Não cancelar, usar cliente padrão
                    }
                }
                "rota" -> {
                    val nome = payloadMap["nome"]?.toString()
                    if (nome.isNullOrBlank()) {
                        android.util.Log.w("SyncManagerV2", "⚠️ Rota sem nome - Usando nome padrão")
                        // Não cancelar, usar nome padrão
                    }
                }
            }

            // ✅ CORREÇÃO: Usar roomId como documento ID para evitar duplicatas
            val docRef = firestore
                .collection("empresas")
                .document(empresaId)
                .collection(collection)
                .document(operation.entityId.toString()) // Usar roomId como documento ID

            android.util.Log.d("SyncManagerV2", "   Firestore Path: empresas/$empresaId/$collection/${operation.entityId}")

            when (operation.operation.uppercase(Locale.getDefault())) {
                "CREATE", "UPDATE" -> {
                    // ✅ CORREÇÃO: Adicionar roomId ao payload para referência
                    val payloadWithRoomId = payloadMap.toMutableMap().apply {
                        put("roomId", operation.entityId)
                        put("syncTimestamp", System.currentTimeMillis())
                    }
                    
                    // ✅ NOVO: Para operações UPDATE, usar merge para não sobrescrever
                    // Para operações CREATE, usar set para criar novo documento
                    if (operation.operation.uppercase(Locale.getDefault()) == "UPDATE") {
                        android.util.Log.d("SyncManagerV2", "   Executando UPDATE com merge...")
                        docRef.set(payloadWithRoomId, SetOptions.merge()).await()
                    } else {
                        android.util.Log.d("SyncManagerV2", "   Executando CREATE com set...")
                        docRef.set(payloadWithRoomId).await()
                    }
                    android.util.Log.d("SyncManagerV2", "   Payload final: $payloadWithRoomId")
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

        // Verificar se existe pelo menos uma rota antes de sincronizar clientes
        val rotasExistentes = appRepository.obterTodasRotas().first()
        if (rotasExistentes.isEmpty()) {
            android.util.Log.w("SyncManagerV2", "⚠️ Nenhuma rota encontrada no Room. Criando rota padrão...")
            try {
                val rotaPadrao = com.example.gestaobilhares.data.entities.Rota(
                    nome = "Rota Padrão",
                    descricao = "Rota criada automaticamente",
                    ativa = true,
                    dataCriacao = System.currentTimeMillis()
                )
                val rotaDao = database.rotaDao()
                val rotaId = rotaDao.insertRota(rotaPadrao)
                android.util.Log.d("SyncManagerV2", "✅ Rota padrão criada: ID $rotaId")
            } catch (e: Exception) {
                android.util.Log.e("SyncManagerV2", "❌ Erro ao criar rota padrão: ${e.message}")
            }
        } else {
            android.util.Log.d("SyncManagerV2", "✅ Encontradas ${rotasExistentes.size} rotas no Room")
        }
            
            pullClientesFromFirestore(empresaId)
            delay(500) // Aguardar clientes serem inseridos
            
            // 3. TERCEIRO: Baixar mesas do Firestore (dependem dos clientes)
            android.util.Log.d("SyncManagerV2", "🔄 Fase 3: Sincronizando MESAS...")
            pullMesasFromFirestore(empresaId)
            delay(500) // Aguardar mesas serem inseridas
            
            // 4. QUARTO: Baixar acertos do Firestore (dependem dos clientes)
            android.util.Log.d("SyncManagerV2", "🔄 Fase 4: Sincronizando ACERTOS...")
            pullAcertosFromFirestore(empresaId)
            delay(500) // Aguardar acertos serem inseridos
            
            // 5. QUINTO: Baixar ciclos do Firestore (dependem dos acertos)
            android.util.Log.d("SyncManagerV2", "🔄 Fase 5: Sincronizando CICLOS...")
            pullCiclosFromFirestore(empresaId)
            delay(500) // Aguardar ciclos serem inseridos
            
            // 6. SEXTO: Criar ciclos automaticamente baseados nos acertos sincronizados
            android.util.Log.d("SyncManagerV2", "🔄 Fase 6: Criando ciclos automaticamente...")
            criarCiclosAutomaticamente()

            // ✅ NOVO PASSO: Remapear acertos importados para o ciclo local correto (numero/ano -> id)
            try {
                android.util.Log.d("SyncManagerV2", "🔄 Remapeando acertos importados para cicloId local...")
                remapearCicloIdDosAcertosParaIdsLocais()
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "⚠️ Erro ao remapear cicloId dos acertos: ${e.message}")
            }
            
            // ✅ CORREÇÃO CRÍTICA: Corrigir acertos existentes com status PENDENTE
            android.util.Log.d("SyncManagerV2", "🔧 CORREÇÃO: Corrigindo acertos PENDENTE para FINALIZADO")
            appRepository.corrigirAcertosPendentesParaFinalizados()
            
            // ✅ NOVO: Reconciliar débitos dos clientes com base no último acerto importado
            try {
                android.util.Log.d("SyncManagerV2", "🔄 Reconciliando débitos dos clientes pós-sync...")
                appRepository.reconciliarDebitosClientes()
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "⚠️ Erro ao reconciliar débitos: ${e.message}")
            }
            
            // 7. SÉTIMO: Invalidar cache das rotas para forçar recálculo dos dados
            android.util.Log.d("SyncManagerV2", "🔄 Fase 7: Invalidando cache das rotas...")
            invalidarCacheRotas()
            
            android.util.Log.d("SyncManagerV2", "✅ PULL SYNC concluído com sucesso")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro no PULL SYNC: ${e.message}", e)
        }
    }

    /**
     * ✅ NOVO: Após baixar/criar ciclos, alinhar acertos importados cujo campo cicloId pode conter o número do ciclo (ou 0)
     * com o ID real do ciclo local (Room PK). Isso garante que:
     * - Classificação Pago/Em aberto funcione (consulta por cicloId real)
     * - Validação de 2º acerto por ciclo funcione para dados importados
     */
    private suspend fun remapearCicloIdDosAcertosParaIdsLocais() {
        val cicloDao = database.cicloAcertoDao()
        val acertoDao = database.acertoDao()

        // Buscar todas as rotas e seus ciclos
        val rotas = appRepository.obterTodasRotas().first()
        for (rota in rotas) {
            try {
                val ciclosRota = cicloDao.buscarCiclosPorRota(rota.id)
                if (ciclosRota.isEmpty()) continue

                // Mapa auxiliar: numeroCiclo -> cicloId (pegando o mais recente por numero se houver)
                val numeroParaId = ciclosRota
                    .groupBy { it.numeroCiclo }
                    .mapValues { entry -> entry.value.maxByOrNull { it.dataAtualizacao.time }!!.id }

                // Para cada ciclo local, alinhar acertos que possam ter vindo com cicloId = numero
                for ((numero, cicloIdReal) in numeroParaId) {
                    // Buscar acertos deste ciclo por duas vias:
                    // 1) já com cicloId = cicloIdReal (ok)
                    // 2) com cicloId igual ao número do ciclo (importação antiga)
                    val acertosComNumero = try {
                        acertoDao.buscarPorCicloId(numero.toLong()).first()
                    } catch (_: Exception) { emptyList<com.example.gestaobilhares.data.entities.Acerto>() }

                    for (ac in acertosComNumero) {
                        // Atualizar somente se rota bater e for claramente um mapeamento de número
                        if (ac.rotaId == rota.id && ac.cicloId != cicloIdReal) {
                            val atualizado = ac.copy(cicloId = cicloIdReal)
                            acertoDao.atualizar(atualizado)
                            android.util.Log.d("SyncManagerV2", "✅ Remapeado acerto ${ac.id}: cicloId ${ac.cicloId} -> $cicloIdReal (rota ${rota.id}, nº $numero)")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "⚠️ Falha ao remapear acertos para rota ${rota.nome}: ${e.message}")
            }
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
                            // Obter rotaId válido
                            val rotaIdCliente = (data["rotaId"] as? Double)?.toLong()
                            val rotaIdFinal = if (rotaIdCliente != null) {
                                // Verificar se a rota existe
                                val rotaExiste = appRepository.buscarRotaPorId(rotaIdCliente)
                                if (rotaExiste != null) {
                                    rotaIdCliente
                                } else {
                                    // Usar primeira rota disponível
                                    val rotas = appRepository.obterTodasRotas().first()
                                    if (rotas.isNotEmpty()) {
                                        android.util.Log.w("SyncManagerV2", "⚠️ Rota $rotaIdCliente não existe. Usando primeira rota disponível: ${rotas.first().id}")
                                        rotas.first().id
                                    } else {
                                        android.util.Log.w("SyncManagerV2", "⚠️ Nenhuma rota disponível. Usando ID 1")
                                        1L
                                    }
                                }
                            } else {
                                // Usar primeira rota disponível
                                val rotas = appRepository.obterTodasRotas().first()
                                if (rotas.isNotEmpty()) {
                                    android.util.Log.w("SyncManagerV2", "⚠️ Cliente sem rotaId. Usando primeira rota disponível: ${rotas.first().id}")
                                    rotas.first().id
                                } else {
                                    android.util.Log.w("SyncManagerV2", "⚠️ Nenhuma rota disponível. Usando ID 1")
                                    1L
                                }
                            }
                            
                            // Criar cliente no Room baseado nos dados do Firestore
                            val cliente = com.example.gestaobilhares.data.entities.Cliente(
                                id = roomId,
                                nome = nome,
                                telefone = data["telefone"] as? String,
                                endereco = data["endereco"] as? String ?: "",
                                rotaId = rotaIdFinal,
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
            android.util.Log.d("SyncManagerV2", "   Caminho: empresas/$empresaId/acertos")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("acertos")
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📊 Encontrados ${snapshot.size()} acertos no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("SyncManagerV2", "⚠️ Nenhum acerto encontrado no Firestore")
                return
            }
            
            var acertosSincronizados = 0
            var acertosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val valorRecebido = data["valorRecebido"] as? Double
                    
                    android.util.Log.d("SyncManagerV2", "🔍 Processando acerto: Valor $valorRecebido (Room ID: $roomId)")
                    android.util.Log.d("SyncManagerV2", "   Dados do acerto: $data")
                    
                    if (roomId != null) {
                        // Verificar se já existe no Room
                        val acertoExistente = appRepository.obterAcertoPorId(roomId)
                        
                        if (acertoExistente == null) {
                            // ✅ CORREÇÃO CRÍTICA: Acertos sincronizados do Firestore devem ser FINALIZADOS
                            val statusFirestore = data["status"] as? String
                            val statusFinal = if (statusFirestore == "PENDENTE") {
                                // Se está no Firestore, significa que foi processado - forçar FINALIZADO
                                android.util.Log.d("SyncManagerV2", "🔄 Convertendo acerto PENDENTE para FINALIZADO (ID: $roomId)")
                                com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO
                            } else {
                                com.example.gestaobilhares.data.entities.StatusAcerto.valueOf(statusFirestore ?: "FINALIZADO")
                            }
                            
                            // ✅ VALIDAÇÃO CRÍTICA: Verificar se já existe acerto FINALIZADO para este cliente e ciclo
                            val clienteId = (data["clienteId"] as? Double)?.toLong() ?: 0L
                            val cicloId = (data["cicloId"] as? Double)?.toLong() ?: 0L
                            
                            if (clienteId > 0 && cicloId > 0) {
                                val acertosExistentes = appRepository.buscarAcertosPorCicloId(cicloId).first()
                                val acertoDuplicado = acertosExistentes.any { acertoExistente -> 
                                    acertoExistente.clienteId == clienteId && 
                                    acertoExistente.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO &&
                                    acertoExistente.id != roomId // Excluir o próprio acerto sendo processado
                                }
                                
                                if (acertoDuplicado) {
                                    android.util.Log.w("SyncManagerV2", "⚠️ DUPLICATA DETECTADA: Cliente $clienteId já tem acerto FINALIZADO no ciclo $cicloId - PULANDO")
                                    continue // Pular este acerto para evitar duplicata
                                }
                            }
                            
                            // Criar acerto no Room baseado nos dados do Firestore
                            val acerto = com.example.gestaobilhares.data.entities.Acerto(
                                id = roomId,
                                clienteId = (data["clienteId"] as? Double)?.toLong() ?: 0L,
                                rotaId = (data["rotaId"] as? Double)?.toLong() ?: 0L,
                                periodoInicio = java.util.Date(),
                                periodoFim = java.util.Date(),
                                valorRecebido = valorRecebido ?: 0.0,
                                debitoAtual = (data["debitoAtual"] as? Double) ?: 0.0,
                                valorTotal = (data["valorTotal"] as? Double) ?: 0.0,
                                desconto = (data["desconto"] as? Double) ?: 0.0,
                                valorComDesconto = (data["valorComDesconto"] as? Double) ?: 0.0,
                                dataAcerto = java.util.Date(),
                                observacoes = data["observacoes"] as? String,
                                metodosPagamentoJson = data["metodosPagamentoJson"] as? String,
                                status = statusFinal,
                                representante = data["representante"] as? String ?: "",
                                tipoAcerto = data["tipoAcerto"] as? String ?: "Presencial",
                                panoTrocado = data["panoTrocado"] as? Boolean ?: false,
                                numeroPano = data["numeroPano"] as? String,
                                dadosExtrasJson = data["dadosExtrasJson"] as? String,
                                cicloId = (data["cicloId"] as? Double)?.toLong() ?: 0L,
                                totalMesas = (data["totalMesas"] as? Double) ?: 0.0
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val acertoDao = database.acertoDao()
                            acertoDao.inserir(acerto)
                            
                            // ✅ CORREÇÃO CRÍTICA: Processar dados das mesas incluídos no payload
                            val acertoMesasData = data["acertoMesas"] as? List<Map<String, Any>>
                            if (acertoMesasData != null && acertoMesasData.isNotEmpty()) {
                                android.util.Log.d("SyncManagerV2", "📋 Processando ${acertoMesasData.size} mesas do acerto $roomId")
                                
                                val acertoMesaDao = database.acertoMesaDao()
                                acertoMesasData.forEach { mesaData ->
                                    try {
                                        val acertoMesa = com.example.gestaobilhares.data.entities.AcertoMesa(
                                            id = (mesaData["id"] as? Double)?.toLong() ?: 0L,
                                            acertoId = roomId,
                                            mesaId = (mesaData["mesaId"] as? Double)?.toLong() ?: 0L,
                                            relogioInicial = (mesaData["relogioInicial"] as? Double)?.toInt() ?: 0,
                                            relogioFinal = (mesaData["relogioFinal"] as? Double)?.toInt() ?: 0,
                                            fichasJogadas = (mesaData["fichasJogadas"] as? Double)?.toInt() ?: 0,
                                            valorFixo = (mesaData["valorFixo"] as? Double) ?: 0.0,
                                            valorFicha = (mesaData["valorFicha"] as? Double) ?: 0.0,
                                            comissaoFicha = (mesaData["comissaoFicha"] as? Double) ?: 0.0,
                                            subtotal = (mesaData["subtotal"] as? Double) ?: 0.0,
                                            comDefeito = mesaData["comDefeito"] as? Boolean ?: false,
                                            relogioReiniciou = mesaData["relogioReiniciou"] as? Boolean ?: false,
                                            observacoes = mesaData["observacoes"] as? String,
                                            fotoRelogioFinal = mesaData["fotoRelogioFinal"] as? String,
                                            dataFoto = null, // ✅ CORREÇÃO: dataFoto é Date?, não String
                                            dataCriacao = java.util.Date()
                                        )
                                        
                                        acertoMesaDao.inserir(acertoMesa)
                                        android.util.Log.d("SyncManagerV2", "✅ Mesa ${acertoMesa.mesaId} sincronizada para acerto $roomId")
                                    } catch (e: Exception) {
                                        android.util.Log.w("SyncManagerV2", "❌ Erro ao processar mesa do acerto: ${e.message}")
                                    }
                                }
                            } else {
                                android.util.Log.w("SyncManagerV2", "⚠️ Acerto $roomId não possui dados de mesas")
                            }
                            
                            acertosSincronizados++
                            android.util.Log.d("SyncManagerV2", "✅ Acerto sincronizado: Valor ${acerto.valorRecebido} (ID: $roomId)")
                        } else {
                            acertosExistentes++
                            android.util.Log.d("SyncManagerV2", "⏭️ Acerto já existe: Valor ${acertoExistente.valorRecebido} (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("SyncManagerV2", "⚠️ Acerto sem roomId: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar acerto ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Acertos:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $acertosSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $acertosExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar acertos: ${e.message}", e)
        }
    }
    
    /**
     * Baixar ciclos do Firestore
     */
    private suspend fun pullCiclosFromFirestore(empresaId: String) {
        try {
            android.util.Log.d("SyncManagerV2", "📥 Baixando ciclos do Firestore...")
            android.util.Log.d("SyncManagerV2", "   Caminho: empresas/$empresaId/ciclos")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("ciclos")
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📊 Encontrados ${snapshot.size()} ciclos no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("SyncManagerV2", "⚠️ Nenhum ciclo encontrado no Firestore")
                return
            }
            
            var ciclosSincronizados = 0
            var ciclosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val numeroCiclo = data["numeroCiclo"] as? Double
                    val rotaId = data["rotaId"] as? Double
                    
                    android.util.Log.d("SyncManagerV2", "🔍 Processando ciclo: ${numeroCiclo}º (Room ID: $roomId)")
                    
                    if (roomId != null && numeroCiclo != null && rotaId != null) {
                        // Verificar se já existe no Room
                        val cicloExistente = try {
                            runBlocking { 
                                val cicloDao = database.cicloAcertoDao()
                                cicloDao.buscarPorId(roomId)
                            }
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (cicloExistente == null) {
                            // Criar ciclo no Room baseado nos dados do Firestore
                            val ciclo = com.example.gestaobilhares.data.entities.CicloAcertoEntity(
                                id = roomId,
                                rotaId = rotaId.toLong(),
                                numeroCiclo = numeroCiclo.toInt(),
                                ano = (data["ano"] as? Double)?.toInt() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                                dataInicio = try {
                                    val dataInicioStr = data["dataInicio"] as? String
                                    if (dataInicioStr != null) {
                                        java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(dataInicioStr)
                                    } else {
                                        java.util.Date()
                                    }
                                } catch (e: Exception) {
                                    java.util.Date()
                                },
                                dataFim = try {
                                    val dataFimStr = data["dataFim"] as? String
                                    if (dataFimStr != null && dataFimStr.isNotEmpty()) {
                                        java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(dataFimStr)
                                    } else {
                                        java.util.Date()
                                    }
                                } catch (e: Exception) {
                                    java.util.Date()
                                },
                                status = com.example.gestaobilhares.data.entities.StatusCicloAcerto.valueOf(
                                    (data["status"] as? String) ?: "FINALIZADO"
                                ),
                                totalClientes = (data["totalClientes"] as? Double)?.toInt() ?: 0,
                                clientesAcertados = (data["clientesAcertados"] as? Double)?.toInt() ?: 0,
                                valorTotalAcertado = (data["valorTotalAcertado"] as? Double) ?: 0.0,
                                valorTotalDespesas = (data["valorTotalDespesas"] as? Double) ?: 0.0,
                                lucroLiquido = (data["lucroLiquido"] as? Double) ?: 0.0,
                                debitoTotal = (data["debitoTotal"] as? Double) ?: 0.0,
                                observacoes = data["observacoes"] as? String,
                                criadoPor = data["criadoPor"] as? String ?: "Sistema",
                                dataCriacao = try {
                                    val dataCriacaoStr = data["dataCriacao"] as? String
                                    if (dataCriacaoStr != null) {
                                        java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(dataCriacaoStr)
                                    } else {
                                        java.util.Date()
                                    }
                                } catch (e: Exception) {
                                    java.util.Date()
                                },
                                dataAtualizacao = java.util.Date()
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val cicloDao = database.cicloAcertoDao()
                            cicloDao.inserir(ciclo)
                            
                            ciclosSincronizados++
                            android.util.Log.d("SyncManagerV2", "✅ Ciclo sincronizado: ${ciclo.numeroCiclo}º (ID: $roomId)")
                        } else {
                            ciclosExistentes++
                            android.util.Log.d("SyncManagerV2", "⏭️ Ciclo já existe: ${cicloExistente.numeroCiclo}º (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("SyncManagerV2", "⚠️ Ciclo sem roomId, numeroCiclo ou rotaId: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar ciclo ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Ciclos:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $ciclosSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $ciclosExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar ciclos: ${e.message}", e)
        }
    }
    
    /**
     * Criar ciclos automaticamente baseados nos acertos sincronizados
     */
    private suspend fun criarCiclosAutomaticamente() {
        try {
            android.util.Log.d("SyncManagerV2", "🔄 Criando ciclos automaticamente baseados nos acertos...")
            
            // Buscar todas as rotas
            val rotas = appRepository.obterTodasRotas().first()
            
            for (rota in rotas) {
                try {
                    // Verificar se já existe ciclo para esta rota
                    val cicloExistente = appRepository.buscarCicloAtualPorRota(rota.id)
                    
                    if (cicloExistente == null) {
                        // Buscar acertos desta rota para determinar o ciclo
                        val acertos = try {
                            runBlocking { 
                                // Buscar clientes da rota primeiro
                                val clienteDao = database.clienteDao()
                                val clientes = clienteDao.obterClientesPorRota(rota.id).first()
                                val clienteIds = clientes.map { cliente -> cliente.id }
                                
                                if (clienteIds.isNotEmpty()) {
                                    // Buscar acertos dos clientes desta rota
                                    val acertoDao = database.acertoDao()
                                    acertoDao.buscarUltimosAcertosPorClientes(clienteIds)
                                } else {
                                    emptyList()
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("SyncManagerV2", "Erro ao buscar acertos da rota ${rota.nome}: ${e.message}")
                            emptyList()
                        }
                        
                        if (acertos.isNotEmpty()) {
                            // Determinar o número do ciclo baseado nos acertos
                            val numeroCiclo = acertos.maxOfOrNull { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.cicloId ?: 1L }?.toInt() ?: 1
                            
                            android.util.Log.d("SyncManagerV2", "🔄 Criando ciclo $numeroCiclo para rota ${rota.nome}")
                            
                            // Determinar status do ciclo baseado na data dos acertos
                            val dataAtual = java.util.Date()
                            val dataUltimoAcerto = acertos.maxByOrNull { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.dataAcerto }?.dataAcerto ?: dataAtual
                            
                            // Se o último acerto foi há mais de 7 dias, considerar ciclo finalizado
                            val diasDiferenca = (dataAtual.time - dataUltimoAcerto.time) / (1000 * 60 * 60 * 24)
                            val statusCiclo = if (diasDiferenca > 7) {
                                com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO
                            } else {
                                com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO
                            }
                            
                            android.util.Log.d("SyncManagerV2", "📊 Status do ciclo determinado: $statusCiclo (último acerto há $diasDiferenca dias)")
                            
                            // Criar ciclo baseado nos acertos
                            val novoCiclo = com.example.gestaobilhares.data.entities.CicloAcertoEntity(
                                rotaId = rota.id,
                                numeroCiclo = numeroCiclo,
                                ano = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                                dataInicio = acertos.minByOrNull { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.dataAcerto }?.dataAcerto ?: java.util.Date(),
                                dataFim = if (statusCiclo == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO) {
                                    dataUltimoAcerto
                                } else {
                                    java.util.Date() // Ciclo em andamento, dataFim será atualizada quando finalizar
                                },
                                status = statusCiclo,
                                totalClientes = acertos.map { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.clienteId }.distinct().size,
                                clientesAcertados = acertos.map { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.clienteId }.distinct().size,
                                valorTotalAcertado = acertos.sumOf { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.valorRecebido },
                                valorTotalDespesas = 0.0, // Será calculado depois
                                lucroLiquido = acertos.sumOf { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.valorRecebido },
                                debitoTotal = acertos.sumOf { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.debitoAtual },
                                observacoes = "Ciclo criado automaticamente após sincronização",
                                criadoPor = "Sistema",
                                dataCriacao = java.util.Date(),
                                dataAtualizacao = java.util.Date()
                            )
                            
                            val cicloId = appRepository.inserirCicloAcerto(novoCiclo)
                            android.util.Log.d("SyncManagerV2", "✅ Ciclo $numeroCiclo criado para rota ${rota.nome} (ID: $cicloId)")
                        }
                    } else {
                        android.util.Log.d("SyncManagerV2", "⏭️ Ciclo já existe para rota ${rota.nome}: ${cicloExistente.numeroCiclo}º")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao criar ciclo para rota ${rota.nome}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "✅ Criação automática de ciclos concluída")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao criar ciclos automaticamente: ${e.message}", e)
        }
    }
    
    /**
     * Invalidar cache das rotas para forçar recálculo dos dados
     */
    private suspend fun invalidarCacheRotas() {
        try {
            android.util.Log.d("SyncManagerV2", "🔄 Invalidando cache das rotas...")
            
            // Buscar todas as rotas e invalidar cache de cada uma
            val rotas = appRepository.obterTodasRotas().first()
            
            for (rota in rotas) {
                try {
                    appRepository.invalidarCacheRota(rota.id)
                    android.util.Log.d("SyncManagerV2", "✅ Cache invalidado para rota ${rota.nome}")
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao invalidar cache da rota ${rota.nome}: ${e.message}")
                }
            }
            
            // ✅ CORREÇÃO CRÍTICA: Forçar invalidação completa do cache global
            try {
                android.util.Log.d("SyncManagerV2", "🔄 Forçando invalidação completa do cache global...")
                
                // Invalidar cache de ciclos também
                val ciclos = try {
                    runBlocking { 
                        val cicloDao = database.cicloAcertoDao()
                        // Buscar ciclos de todas as rotas
                        val rotas = appRepository.obterTodasRotas().first()
                        val todosCiclos = mutableListOf<com.example.gestaobilhares.data.entities.CicloAcertoEntity>()
                        
                        for (rota in rotas) {
                            try {
                                val ciclosRota = cicloDao.buscarCiclosPorRota(rota.id)
                                todosCiclos.addAll(ciclosRota)
                            } catch (e: Exception) {
                                android.util.Log.w("SyncManagerV2", "❌ Erro ao buscar ciclos da rota ${rota.nome}: ${e.message}")
                            }
                        }
                        
                        todosCiclos
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao buscar ciclos: ${e.message}")
                    emptyList<com.example.gestaobilhares.data.entities.CicloAcertoEntity>()
                }
                
                for (ciclo in ciclos) {
                    try {
                        // Forçar recálculo do ciclo
                        android.util.Log.d("SyncManagerV2", "🔄 Invalidando cache do ciclo ${ciclo.numeroCiclo}")
                    } catch (e: Exception) {
                        android.util.Log.w("SyncManagerV2", "❌ Erro ao invalidar cache do ciclo: ${e.message}")
                    }
                }
                
                // ✅ NOVO: Forçar refresh das estatísticas das rotas
                android.util.Log.d("SyncManagerV2", "🔄 Forçando refresh das estatísticas das rotas...")
                
                // Aguardar um pouco para garantir que todas as operações sejam processadas
                kotlinx.coroutines.delay(1000)
                
                android.util.Log.d("SyncManagerV2", "✅ Invalidação completa do cache global concluída")
                
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "❌ Erro na invalidação completa do cache: ${e.message}")
            }
            
            android.util.Log.d("SyncManagerV2", "✅ Cache das rotas invalidado com sucesso")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao invalidar cache das rotas: ${e.message}", e)
        }
    }
    
    /**
     * Baixar mesas do Firestore
     */
    private suspend fun pullMesasFromFirestore(empresaId: String) {
        try {
            android.util.Log.d("SyncManagerV2", "📥 Baixando mesas do Firestore...")
            android.util.Log.d("SyncManagerV2", "   Caminho: empresas/$empresaId/mesas")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("mesas")
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📊 Encontradas ${snapshot.size()} mesas no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("SyncManagerV2", "⚠️ Nenhuma mesa encontrada no Firestore")
                return
            }
            
            var mesasSincronizadas = 0
            var mesasExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val numero = data["numero"] as? String
                    
                    android.util.Log.d("SyncManagerV2", "🔍 Processando mesa: $numero (Room ID: $roomId)")
                    android.util.Log.d("SyncManagerV2", "   Dados da mesa: $data")
                    
                    if (roomId != null && numero != null) {
                        // Verificar se já existe no Room
                        val mesaExistente = appRepository.obterMesaPorId(roomId)
                        
                        if (mesaExistente == null) {
                            // Criar mesa no Room baseado nos dados do Firestore
                            val mesa = com.example.gestaobilhares.data.entities.Mesa(
                                id = roomId,
                                numero = numero,
                                clienteId = (data["clienteId"] as? Double)?.toLong(),
                                ativa = data["ativa"] as? Boolean ?: true,
                                tipoMesa = com.example.gestaobilhares.data.entities.TipoMesa.valueOf(
                                    (data["tipoMesa"] as? String) ?: "SINUCA"
                                ),
                                tamanho = com.example.gestaobilhares.data.entities.TamanhoMesa.valueOf(
                                    (data["tamanho"] as? String) ?: "PEQUENA"
                                ),
                                estadoConservacao = com.example.gestaobilhares.data.entities.EstadoConservacao.valueOf(
                                    (data["estadoConservacao"] as? String) ?: "OTIMO"
                                ),
                                valorFixo = (data["valorFixo"] as? Double) ?: 0.0,
                                relogioInicial = (data["relogioInicial"] as? Double)?.toInt() ?: 0,
                                relogioFinal = (data["relogioFinal"] as? Double)?.toInt() ?: 0,
                                dataInstalacao = try {
                                    val dataInstalacaoStr = data["dataInstalacao"] as? String
                                    if (dataInstalacaoStr != null) {
                                        java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(dataInstalacaoStr)
                                    } else {
                                        java.util.Date()
                                    }
                                } catch (e: Exception) {
                                    java.util.Date()
                                },
                                observacoes = data["observacoes"] as? String,
                                panoAtualId = (data["panoAtualId"] as? Double)?.toLong(),
                                dataUltimaTrocaPano = try {
                                    val dataTrocaStr = data["dataUltimaTrocaPano"] as? String
                                    if (dataTrocaStr != null && dataTrocaStr.isNotEmpty()) {
                                        java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(dataTrocaStr)
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val mesaDao = database.mesaDao()
                            mesaDao.inserir(mesa)
                            
                            mesasSincronizadas++
                            android.util.Log.d("SyncManagerV2", "✅ Mesa sincronizada: ${mesa.numero} (ID: $roomId)")
                        } else {
                            mesasExistentes++
                            android.util.Log.d("SyncManagerV2", "⏭️ Mesa já existe: ${mesaExistente.numero} (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("SyncManagerV2", "⚠️ Mesa sem roomId ou numero: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar mesa ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Mesas:")
            android.util.Log.d("SyncManagerV2", "   Sincronizadas: $mesasSincronizadas")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $mesasExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar mesas: ${e.message}", e)
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
                        android.util.Log.w("SyncManagerV2", "   Dados disponíveis: ${data.keys}")
                        
                        // Tentar criar rota com dados mínimos se não tiver roomId
                        if (roomId == null && nome != null) {
                            android.util.Log.d("SyncManagerV2", "🔄 Tentando criar rota sem roomId: $nome")
                            try {
                                val rota = com.example.gestaobilhares.data.entities.Rota(
                                    nome = nome,
                                    descricao = data["descricao"] as? String ?: "",
                                    ativa = data["ativa"] as? Boolean ?: true,
                                    dataCriacao = System.currentTimeMillis()
                                )
                                
                                val rotaDao = database.rotaDao()
                                val novoId = rotaDao.insertRota(rota)
                                
                                rotasSincronizadas++
                                android.util.Log.d("SyncManagerV2", "✅ Rota criada sem roomId: ${rota.nome} (Novo ID: $novoId)")
                            } catch (e: Exception) {
                                android.util.Log.e("SyncManagerV2", "❌ Erro ao criar rota sem roomId: ${e.message}")
                            }
                        } else if (roomId == null && nome == null) {
                            // Rota completamente vazia - verificar se já existe uma rota com nome similar
                            android.util.Log.d("SyncManagerV2", "🔄 Rota completamente vazia. Verificando se já existe rota similar...")
                            
                            // Verificar se já existe uma rota com nome baseado no ID do documento
                            val nomeExtraido = document.id.takeIf { it.isNotBlank() } ?: "Rota Importada"
                            val rotasExistentes = appRepository.obterTodasRotas().first()
                            val rotaSimilar = rotasExistentes.find { it.nome.contains(nomeExtraido) || nomeExtraido.contains(it.nome) }
                            
                            if (rotaSimilar == null) {
                                try {
                                    val rota = com.example.gestaobilhares.data.entities.Rota(
                                        nome = nomeExtraido,
                                        descricao = "Rota importada do Firestore",
                                        ativa = true,
                                        dataCriacao = System.currentTimeMillis()
                                    )
                                    
                                    val rotaDao = database.rotaDao()
                                    val novoId = rotaDao.insertRota(rota)
                                    
                                    rotasSincronizadas++
                                    android.util.Log.d("SyncManagerV2", "✅ Rota criada com nome extraído: ${rota.nome} (Novo ID: $novoId)")
                                } catch (e: Exception) {
                                    android.util.Log.e("SyncManagerV2", "❌ Erro ao criar rota com nome extraído: ${e.message}")
                                }
                            } else {
                                android.util.Log.d("SyncManagerV2", "⏭️ Rota similar já existe: ${rotaSimilar.nome} (ID: ${rotaSimilar.id})")
                            }
                        }
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
