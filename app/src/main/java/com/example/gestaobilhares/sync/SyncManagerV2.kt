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
        "despesa" -> "despesas"
        "panoestoque" -> "panosEstoque"
        "mesavendida" -> "mesasVendidas"
        "stockitem" -> "stockItems"
        "veiculo" -> "veiculos"
        "historicomanutencaomesa" -> "historicoManutencaoMesa"
        "historicomanutencaoveiculo" -> "historicoManutencaoVeiculo"
        "historicocombustivelveiculo" -> "historicoCombustivelVeiculo"
        "categoriadespesa" -> "categoriasDespesa"
        "tipodespesa" -> "tiposDespesa"
        "contratolocacao" -> "contratosLocacao"
        "contratomesa" -> "contratoMesas"
        "metacolaborador" -> "metas"
        "colaboradorrota" -> "colaboradoresRotas"
        "assinaturarepresentantelegal" -> "assinaturasRepresentanteLegal"
        "logauditoriaassinatura" -> "logsAuditoriaAssinatura"
        "acertomesa" -> "acertoMesa"
        "mesareformada" -> "mesasReformadas"
        "aditivocontrato" -> "aditivosContrato"
        "aditivomesa" -> "aditivoMesas"
        "panomesa" -> "panoMesas"
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
            android.util.Log.w("SyncManagerV2", "⚠️ Nenhuma rota encontrada no Room. Os clientes precisam de uma rota para serem sincronizados.")
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
        
        // 5.1. ATUALIZAR ROTAS: Alinhar status das rotas com os ciclos importados
        android.util.Log.d("SyncManagerV2", "🔄 Fase 5.1: Atualizando status das rotas com ciclos importados...")
        atualizarRotasComCiclosImportados()
        delay(500) // Aguardar atualizações das rotas
        
        // 6. SEXTO: Baixar colaboradores do Firestore
        android.util.Log.d("SyncManagerV2", "🔄 Fase 6: Sincronizando COLABORADORES...")
        pullColaboradoresFromFirestore(empresaId)
        delay(500) // Aguardar colaboradores serem inseridos
        
        // 7. SÉTIMO: Baixar despesas do Firestore
        android.util.Log.d("SyncManagerV2", "🔄 Fase 7: Sincronizando DESPESAS...")
        pullDespesasFromFirestore(empresaId)
        delay(500) // Aguardar despesas serem inseridas
        
        // 8. OITAVO: Baixar panos estoque do Firestore
        android.util.Log.d("SyncManagerV2", "🔄 Fase 8: Sincronizando PANOS ESTOQUE...")
        pullPanoEstoqueFromFirestore(empresaId)
        delay(500) // Aguardar panos serem inseridos
        
        // 9. NONO: Baixar mesas vendidas do Firestore
        android.util.Log.d("SyncManagerV2", "🔄 Fase 9: Sincronizando MESAS VENDIDAS...")
        pullMesaVendidaFromFirestore(empresaId)
        delay(500) // Aguardar mesas vendidas serem inseridas
        
        // 10. DÉCIMO: Sincronizar StockItems
        android.util.Log.d("SyncManagerV2", "🔄 Fase 10: Sincronizando STOCK ITEMS...")
        pullStockItemsFromFirestore(empresaId)
        delay(500) // Aguardar stock items serem inseridos
        
        // 11. DÉCIMO PRIMEIRO: Sincronizar Veículos
        android.util.Log.d("SyncManagerV2", "🔄 Fase 11: Sincronizando VEÍCULOS...")
        pullVeiculosFromFirestore(empresaId)
        delay(500) // Aguardar veículos serem inseridos
        
        // 12. DÉCIMO SEGUNDO: Sincronizar Histórico Manutenção Mesa
        android.util.Log.d("SyncManagerV2", "🔄 Fase 12: Sincronizando HISTÓRICO MANUTENÇÃO MESA...")
        pullHistoricoManutencaoMesaFromFirestore(empresaId)
        delay(500) // Aguardar histórico mesa serem inseridos
        
        // 13. DÉCIMO TERCEIRO: Sincronizar Histórico Manutenção Veículo
        android.util.Log.d("SyncManagerV2", "🔄 Fase 13: Sincronizando HISTÓRICO MANUTENÇÃO VEÍCULO...")
        pullHistoricoManutencaoVeiculoFromFirestore(empresaId)
        delay(500) // Aguardar histórico veículo serem inseridos
        
        // 14. DÉCIMO QUARTO: Sincronizar Histórico Combustível Veículo
        android.util.Log.d("SyncManagerV2", "🔄 Fase 14: Sincronizando HISTÓRICO COMBUSTÍVEL VEÍCULO...")
        pullHistoricoCombustivelVeiculoFromFirestore(empresaId)
        delay(500) // Aguardar histórico combustível serem inseridos
        
        // 15. DÉCIMO QUINTO: Sincronizar Categorias Despesa
        android.util.Log.d("SyncManagerV2", "🔄 Fase 15: Sincronizando CATEGORIAS DESPESA...")
        pullCategoriasDespesaFromFirestore(empresaId)
        delay(500) // Aguardar categorias serem inseridas
        
        // 16. DÉCIMO SEXTO: Sincronizar Tipos Despesa
        android.util.Log.d("SyncManagerV2", "🔄 Fase 16: Sincronizando TIPOS DESPESA...")
        pullTiposDespesaFromFirestore(empresaId)
        delay(500) // Aguardar tipos serem inseridos
        
        // 17. DÉCIMO SÉTIMO: Sincronizar Contratos Locação
        android.util.Log.d("SyncManagerV2", "🔄 Fase 17: Sincronizando CONTRATOS LOCAÇÃO...")
        pullContratosLocacaoFromFirestore(empresaId)
        delay(500) // Aguardar contratos serem inseridos
        
        // 17.0.1: Sincronizar Metas (dependem de colaboradores/rotas e opcionalmente ciclo)
        android.util.Log.d("SyncManagerV2", "🔄 Fase 17.0.1: Sincronizando METAS...")
        pullMetasFromFirestore(empresaId)
        delay(300)

        // 17.0.2: Sincronizar ColaboradorRota
        android.util.Log.d("SyncManagerV2", "🔄 Fase 17.0.2: Sincronizando COLABORADOR_ROTA...")
        pullColaboradoresRotasFromFirestore(empresaId)
        delay(300)

        // 17.1: Sincronizar Aditivos de Contrato
        android.util.Log.d("SyncManagerV2", "🔄 Fase 17.1: Sincronizando ADITIVOS DE CONTRATO...")
        pullAditivosContratoFromFirestore(empresaId)
        delay(300)
        
        // 17.2: Sincronizar Aditivo Mesas
        android.util.Log.d("SyncManagerV2", "🔄 Fase 17.2: Sincronizando ADITIVO MESAS...")
        pullAditivoMesasFromFirestore(empresaId)
        delay(300)

        // 17.3: Sincronizar Contrato Mesas
        android.util.Log.d("SyncManagerV2", "🔄 Fase 17.3: Sincronizando CONTRATO MESAS...")
        pullContratoMesasFromFirestore(empresaId)
        delay(300)
        
        // 18. DÉCIMO OITAVO: Sincronizar Assinaturas Representante Legal
        android.util.Log.d("SyncManagerV2", "🔄 Fase 18: Sincronizando ASSINATURAS REPRESENTANTE LEGAL...")
        pullAssinaturasRepresentanteLegalFromFirestore(empresaId)
        delay(500) // Aguardar assinaturas serem inseridas
        
        // 19. DÉCIMO NONO: Sincronizar Logs Auditoria Assinatura
        android.util.Log.d("SyncManagerV2", "🔄 Fase 19: Sincronizando LOGS AUDITORIA ASSINATURA...")
        pullLogsAuditoriaAssinaturaFromFirestore(empresaId)
        delay(500) // Aguardar logs serem inseridos
        
        // 20. VIGÉSIMO: Sincronizar AcertoMesa
        android.util.Log.d("SyncManagerV2", "🔄 Fase 20: Sincronizando ACERTO MESA...")
        pullAcertoMesaFromFirestore(empresaId)
        delay(500) // Aguardar acerto mesa serem inseridos
        
        // 21. VIGÉSIMO PRIMEIRO: Sincronizar MesaReformada
        android.util.Log.d("SyncManagerV2", "🔄 Fase 21: Sincronizando MESA REFORMADA...")
        pullMesaReformadaFromFirestore(empresaId)
        delay(500) // Aguardar mesa reformada serem inseridas
        
        // 22. VIGÉSIMO SEGUNDO: Sincronizar PanoMesa
        android.util.Log.d("SyncManagerV2", "🔄 Fase 22: Sincronizando PANO MESA...")
        pullPanoMesaFromFirestore(empresaId)
        delay(500) // Aguardar pano mesa serem inseridos
        
        // 23. VIGÉSIMO TERCEIRO: (REMOVIDO) NÃO criar ciclos automaticamente após PULL
        // Motivo: Garantir espelhamento 1:1 com a nuvem. Se a nuvem já contém os ciclos
        // corretos (ex.: 3º e 4º em andamento), criar localmente pode introduzir
        // inconsistências (ex.: 1º finalizado). Mantemos apenas os ciclos vindos do Firestore.
        android.util.Log.d("SyncManagerV2", "⏭️ Fase 22: Criação automática de ciclos desativada para manter espelho 1:1")

            // ✅ NOVO PASSO: Remapear acertos importados para o ciclo local correto (numero/ano -> id)
            try {
                android.util.Log.d("SyncManagerV2", "🔄 Remapeando acertos importados para cicloId local...")
                remapearCicloIdDosAcertosParaIdsLocais()
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "⚠️ Erro ao remapear cicloId dos acertos: ${e.message}")
            }

            // ✅ NOVO: Alinhar campos da tabela de rotas (ciclo atual e datas) com o último ciclo importado
            try {
                android.util.Log.d("SyncManagerV2", "🔄 Alinhando rotas com ciclo atual importado...")
                alinharRotasComCicloAtualImportado()
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "⚠️ Erro ao alinhar rotas com ciclo atual: ${e.message}")
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
     * Após importar ciclos do Firestore, garantimos que os campos da rota reflitam o ciclo atual
     * para que telas que dependem de `Rota.cicloAcertoAtual/anoCiclo/dataInicioCiclo/dataFimCiclo`
     * exibam exatamente o que veio da nuvem (espelho 1:1).
     */
    private suspend fun alinharRotasComCicloAtualImportado() {
        val rotaDao = database.rotaDao()
        val cicloDao = database.cicloAcertoDao()

        // Buscar todas as rotas atuais
        val rotas = appRepository.obterTodasRotas().first()
        var rotasAtualizadas = 0

        for (rota in rotas) {
            try {
                val cicloAtual = cicloDao.buscarCicloAtualPorRota(rota.id)
                if (cicloAtual != null) {
                    val statusRota = if (cicloAtual.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO) {
                        com.example.gestaobilhares.data.entities.StatusRota.EM_ANDAMENTO
                    } else {
                        com.example.gestaobilhares.data.entities.StatusRota.FINALIZADA
                    }

                    val rotaAtualizada = rota.copy(
                        statusAtual = statusRota,
                        cicloAcertoAtual = cicloAtual.numeroCiclo,
                        anoCiclo = cicloAtual.ano,
                        dataInicioCiclo = cicloAtual.dataInicio.time,
                        dataFimCiclo = cicloAtual.dataFim?.time
                    )
                    rotaDao.updateRota(rotaAtualizada)
                    rotasAtualizadas++
                    android.util.Log.d(
                        "SyncManagerV2",
                        "✅ Rota '${rota.nome}' alinhada com ciclo ${cicloAtual.numeroCiclo} (${cicloAtual.status})"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "⚠️ Falha ao alinhar rota ${rota.nome}: ${e.message}")
            }
        }

        android.util.Log.d("SyncManagerV2", "📊 Rotas alinhadas: $rotasAtualizadas de ${rotas.size}")
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
                                dataCadastro = java.util.Date(), // Usar data atual como fallback
                                valorFicha = (data["valorFicha"] as? Double) ?: 0.0,
                                comissaoFicha = (data["comissaoFicha"] as? Double) ?: 0.0
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
                                debitoAnterior = (data["debitoAnterior"] as? Double) ?: 0.0, // ✅ CORREÇÃO: Adicionar debitoAnterior
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
                                            fotoRelogioFinal = null, // Será preenchido após download
                                            dataFoto = null, // Será preenchido após processar timestamp
                                            dataCriacao = java.util.Date()
                                        )
                                        
                                        // ✅ NOVO: Download de foto do Firebase Storage se for URL
                                        val fotoUrlFirebaseMesa = mesaData["fotoRelogioFinal"] as? String
                                        val fotoRelogioLocalMesa = if (!fotoUrlFirebaseMesa.isNullOrBlank()) {
                                            try {
                                                val caminhoLocal = com.example.gestaobilhares.utils.FirebaseStorageManager.downloadFoto(
                                                    context = context,
                                                    urlFirebase = fotoUrlFirebaseMesa,
                                                    tipoFoto = "relogio_final"
                                                )
                                                if (caminhoLocal != null) {
                                                    android.util.Log.d("SyncManagerV2", "✅ Foto de relógio final (mesa) baixada: $caminhoLocal")
                                                }
                                                caminhoLocal
                                            } catch (e: Exception) {
                                                android.util.Log.e("SyncManagerV2", "Erro ao baixar foto de relógio final (mesa): ${e.message}")
                                                fotoUrlFirebaseMesa // Fallback: manter URL se download falhar
                                            }
                                        } else null
                                        
                                        // Processar dataFoto do timestamp
                                        val dataFotoTimestamp = (mesaData["dataFoto"] as? Number)?.toLong()
                                        
                                        // Atualizar acertoMesa com foto e dataFoto
                                        val acertoMesaComFoto = acertoMesa.copy(
                                            fotoRelogioFinal = fotoRelogioLocalMesa,
                                            dataFoto = dataFotoTimestamp?.let { java.util.Date(it) }
                                        )
                                        
                                        acertoMesaDao.inserir(acertoMesaComFoto)
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
            android.util.Log.d("CYCLE_PULL", "INIT pullCiclosFromFirestore empresa=$empresaId")
            android.util.Log.d("SyncManagerV2", "   Caminho: empresas/$empresaId/ciclos")
            
            // Tentar coleções compatíveis: primeiro "ciclos"; se vazio, tentar "cicloacertos"
            var snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("ciclos")
                .get()
                .await()

            if (snapshot.isEmpty) {
                android.util.Log.w("CYCLE_PULL", "Coleção 'ciclos' vazia. Tentando 'cicloacertos'...")
                snapshot = firestore
                    .collection("empresas")
                    .document(empresaId)
                    .collection("cicloacertos")
                    .get()
                    .await()
            }

            // Fallback 2: ciclos aninhados por rota: empresas/{empresaId}/rotas/{rotaId}/ciclos
            if (snapshot.isEmpty) {
                android.util.Log.w("CYCLE_PULL", "Coleções 'ciclos' e 'cicloacertos' vazias. Tentando ciclos aninhados por rota...")
                // Buscar rotas importadas e tentar baixar ciclos por rota
                val rotasImportadas = appRepository.obterTodasRotas().first()
                var insertedNested = 0
                for (rota in rotasImportadas) {
                    try {
                        val nested = firestore
                            .collection("empresas")
                            .document(empresaId)
                            .collection("rotas")
                            .document(rota.id.toString())
                            .collection("ciclos")
                            .get()
                            .await()

                        if (!nested.isEmpty) {
                            android.util.Log.d("CYCLE_PULL", "NESTED rotaId=${rota.id} count=${nested.size()}")
                            for (doc in nested.documents) {
                                try {
                                    val data = doc.data ?: continue
                                    val numeroCiclo = (data["numeroCiclo"] as? Double)?.toInt() ?: (data["numeroCiclo"] as? Long)?.toInt()
                                    val statusStr = (data["status"] as? String) ?: "FINALIZADO"
                                    val ano = ((data["ano"] as? Double)?.toInt()) ?: ((data["ano"] as? Long)?.toInt()) ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                    val di = when (val v = data["dataInicio"]) { is Number -> java.util.Date(v.toLong()); is String -> if (v.isNotEmpty()) java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(v) else null; else -> null } ?: java.util.Date()
                                    val df = when (val v = data["dataFim"]) { is Number -> java.util.Date(v.toLong()); is String -> if (v.isNotEmpty()) java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(v) else null; else -> null } ?: java.util.Date()
                                    if (numeroCiclo != null) {
                                        val ciclo = com.example.gestaobilhares.data.entities.CicloAcertoEntity(
                                            rotaId = rota.id,
                                            numeroCiclo = numeroCiclo,
                                            ano = ano,
                                            dataInicio = di,
                                            dataFim = df,
                                            status = com.example.gestaobilhares.data.entities.StatusCicloAcerto.valueOf(statusStr),
                                            criadoPor = "Importado"
                                        )
                                        database.cicloAcertoDao().inserir(ciclo)
                                        insertedNested++
                                        android.util.Log.d("CYCLE_PULL", "NESTED_INSERT rota=${rota.id} numero=${numeroCiclo} status=${statusStr}")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("CYCLE_PULL", "NESTED_ERROR rota=${rota.id} doc=${doc.id} error=${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("CYCLE_PULL", "NESTED_FETCH_ERROR rota=${rota.id} error=${e.message}")
                    }
                }
                android.util.Log.d("CYCLE_PULL", "NESTED_SUMMARY inserted=$insertedNested")
            }
            
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
                    val roomId = (data["roomId"] as? Long) ?: (data["roomId"] as? Double)?.toLong()
                    val numeroCiclo = (data["numeroCiclo"] as? Double)?.toInt() ?: (data["numeroCiclo"] as? Long)?.toInt()
                    val rotaId = (data["rotaId"] as? Double)?.toLong() ?: (data["rotaId"] as? Long)
                    
                    android.util.Log.d("SyncManagerV2", "🔍 Processando ciclo: ${numeroCiclo}º (Room ID: $roomId, Rota ID: $rotaId)")
                    android.util.Log.d("SyncManagerV2", "   Dados originais: numeroCiclo=${data["numeroCiclo"]}, rotaId=${data["rotaId"]}")
                    android.util.Log.d("CYCLE_PULL", "DOC id=${document.id} roomId=$roomId rotaId=$rotaId numero=$numeroCiclo ano=${data["ano"]} dataInicio=${data["dataInicio"]} dataFim=${data["dataFim"]} status=${data["status"]}")
                    
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
                            // IMPORTANTE: Preservar numeroCiclo exatamente como foi exportado
                            val ciclo = com.example.gestaobilhares.data.entities.CicloAcertoEntity(
                                id = roomId,
                                rotaId = rotaId,
                                numeroCiclo = numeroCiclo,
                                ano = ((data["ano"] as? Double)?.toInt())
                                    ?: ((data["ano"] as? Long)?.toInt())
                                    ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                                dataInicio = try {
                                    when (val di = data["dataInicio"]) {
                                        is Number -> java.util.Date(di.toLong())
                                        is String -> if (di.isNotEmpty()) java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(di) else null
                                        else -> null
                                    } ?: java.util.Date()
                                } catch (e: Exception) { java.util.Date() },
                                dataFim = try {
                                    when (val df = data["dataFim"]) {
                                        is Number -> java.util.Date(df.toLong())
                                        is String -> if (df.isNotEmpty()) java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(df) else null
                                        else -> null
                                    } ?: java.util.Date()
                                } catch (e: Exception) { java.util.Date() },
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
                            android.util.Log.d("CYCLE_PULL", "INSERT ciclo id=$roomId rotaId=$rotaId numero=${ciclo.numeroCiclo} ano=${ciclo.ano} di=${ciclo.dataInicio?.time} df=${ciclo.dataFim?.time} status=${ciclo.status}")
                        } else {
                            ciclosExistentes++
                            android.util.Log.d("SyncManagerV2", "⏭️ Ciclo já existe: ${cicloExistente.numeroCiclo}º (ID: $roomId)")
                            android.util.Log.d("CYCLE_PULL", "SKIP_EXISTING ciclo id=$roomId numero=${cicloExistente.numeroCiclo} status=${cicloExistente.status}")
                        }
                    } else {
                        android.util.Log.w("SyncManagerV2", "⚠️ Ciclo sem roomId, numeroCiclo ou rotaId: ${document.id}")
                        android.util.Log.w("CYCLE_PULL", "INVALID ciclo docId=${document.id} keys=${data.keys}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar ciclo ${document.id}: ${e.message}")
                    android.util.Log.w("CYCLE_PULL", "ERROR_PROCESS docId=${document.id} error=${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Ciclos:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $ciclosSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $ciclosExistentes")
            android.util.Log.d("CYCLE_PULL", "SUMMARY inserted=$ciclosSincronizados existing=$ciclosExistentes")
            
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
     * Atualizar status das rotas baseado nos ciclos importados
     */
    private suspend fun atualizarRotasComCiclosImportados() {
        try {
            android.util.Log.d("SyncManagerV2", "🔄 Atualizando status das rotas com ciclos importados...")
            
            // Buscar todas as rotas
            val rotas = appRepository.obterTodasRotas().first()
            android.util.Log.d("SyncManagerV2", "📊 Encontradas ${rotas.size} rotas para atualizar")
            
            for (rota in rotas) {
                try {
                    android.util.Log.d("SyncManagerV2", "🔍 Processando rota: ${rota.nome} (ID: ${rota.id})")
                    
                    // Buscar ciclos da rota e aplicar a mesma regra local:
                    // 1) Se existir EM_ANDAMENTO, usar o mais recente
                    // 2) Caso contrário, usar o FINALIZADO mais recente
                    val (cicloMaisRecente, origem) = try {
                        runBlocking {
                            val cicloDao = database.cicloAcertoDao()
                            val ciclos = cicloDao.buscarCiclosPorRota(rota.id)
                            val emAndamento = ciclos
                                .filter { it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO }
                                .maxWithOrNull(
                                    compareBy<com.example.gestaobilhares.data.entities.CicloAcertoEntity> { it.numeroCiclo }
                                        .thenBy { it.dataInicio?.time ?: 0L }
                                )
                            if (emAndamento != null) {
                                Pair(emAndamento, "EM_ANDAMENTO")
                            } else {
                                val finalizado = ciclos
                                    .filter { it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO }
                                    .maxWithOrNull(
                                        compareBy<com.example.gestaobilhares.data.entities.CicloAcertoEntity> { it.numeroCiclo }
                                            .thenBy { it.dataFim?.time ?: it.dataInicio?.time ?: 0L }
                                    )
                                Pair(finalizado, "FINALIZADO")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("SyncManagerV2", "❌ Erro ao buscar ciclos da rota ${rota.nome}: ${e.message}")
                        Pair(null, "NONE")
                    }
                    
                    if (cicloMaisRecente != null) {
                        android.util.Log.d("SyncManagerV2", "✅ Ciclo encontrado para rota ${rota.nome}: ${cicloMaisRecente.numeroCiclo}º (Status: ${cicloMaisRecente.status})")
                        
                        // Atualizar a rota com os dados do ciclo
                        val rotaAtualizada = rota.copy(
                            statusAtual = when (cicloMaisRecente.status) {
                                com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO -> com.example.gestaobilhares.data.entities.StatusRota.EM_ANDAMENTO
                                else -> com.example.gestaobilhares.data.entities.StatusRota.FINALIZADA
                            },
                            cicloAcertoAtual = cicloMaisRecente.numeroCiclo,
                            anoCiclo = cicloMaisRecente.ano,
                            dataInicioCiclo = cicloMaisRecente.dataInicio.time,
                            dataFimCiclo = if (cicloMaisRecente.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO) {
                                cicloMaisRecente.dataFim.time
                            } else {
                                null
                            }
                        )
                        android.util.Log.d("SyncManagerV2", "✅ Rota ${rota.nome} alinhada com ciclo ${cicloMaisRecente.numeroCiclo} (${origem})")
                        
                        // Atualizar no banco
                        val rotaDao = database.rotaDao()
                        rotaDao.updateRota(rotaAtualizada)
                        
                        android.util.Log.d("SyncManagerV2", "✅ Rota ${rota.nome} atualizada: ${cicloMaisRecente.numeroCiclo}º Acerto ${if (cicloMaisRecente.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO) "em andamento" else "finalizado"}")
                    } else {
                        android.util.Log.w("SyncManagerV2", "⚠️ Nenhum ciclo encontrado para rota ${rota.nome}")
                        
                        // Se não há ciclo, definir como pausada
                        val rotaAtualizada = rota.copy(
                            statusAtual = com.example.gestaobilhares.data.entities.StatusRota.FINALIZADA,
                            cicloAcertoAtual = 1,
                            anoCiclo = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                            dataInicioCiclo = null,
                            dataFimCiclo = null
                        )
                        
                        val rotaDao = database.rotaDao()
                        rotaDao.updateRota(rotaAtualizada)
                        
                        android.util.Log.d("SyncManagerV2", "✅ Rota ${rota.nome} definida como pausada (sem ciclos)")
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao atualizar rota ${rota.nome}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "✅ Atualização das rotas com ciclos importados concluída")
            
            // ✅ NOVO: Forçar atualização das telas após PULL
            try {
                android.util.Log.d("SyncManagerV2", "🔄 Forçando atualização das telas após PULL...")
                
                // Invalidar cache para forçar recálculo
                invalidarCacheRotas()
                
                // ✅ NOVO: Forçar atualização do Flow de rotas para notificar ViewModels
                try {
                    android.util.Log.d("SyncManagerV2", "🔄 Forçando atualização do Flow de rotas...")
                    
                    // Buscar todas as rotas e forçar recálculo
                    val rotas = appRepository.obterTodasRotas().first()
                    android.util.Log.d("SyncManagerV2", "📊 Forçando recálculo de ${rotas.size} rotas")
                    
                    // Forçar invalidação do cache de cada rota
                    for (rota in rotas) {
                        try {
                            appRepository.invalidarCacheRota(rota.id)
                            android.util.Log.d("SyncManagerV2", "✅ Cache invalidado para rota ${rota.nome}")
                        } catch (e: Exception) {
                            android.util.Log.w("SyncManagerV2", "❌ Erro ao invalidar cache da rota ${rota.nome}: ${e.message}")
                        }
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao forçar atualização do Flow: ${e.message}")
                }
                
                // Aguardar um pouco para garantir que as atualizações sejam processadas
                kotlinx.coroutines.delay(1000)
                
                android.util.Log.d("SyncManagerV2", "✅ Atualização das telas forçada com sucesso")
                
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "⚠️ Erro ao forçar atualização das telas: ${e.message}")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao atualizar rotas com ciclos importados: ${e.message}", e)
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
     * PULL SYNC: Baixar colaboradores do Firestore para o app
     */
    private suspend fun pullColaboradoresFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Colaboradores do Firestore...")
        
        try {
            val colaboradoresCollection = firestore.collection("empresas/$empresaId/colaboradores")
            val snapshot = colaboradoresCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} colaboradores no Firestore")
            
            var colaboradoresSincronizados = 0
            var colaboradoresExistentes = 0
            
            val colaboradorDao = database.colaboradorDao()
            val colaboradoresExistentesList = colaboradorDao.obterTodos().first()
            
            for (document in snapshot) {
                try {
                    val data = document.data
                    val roomId = document.id.toLongOrNull() ?: continue
                    
                    // Verificar se já existe
                    val jaExiste = colaboradoresExistentesList.any { colaborador -> colaborador.id == roomId }
                    if (jaExiste) {
                        colaboradoresExistentes++
                        android.util.Log.d("SyncManagerV2", "⏭️ Colaborador já existe: ${data["nome"]} (ID: $roomId)")
                        continue
                    }
                    
                    // Converter dados do Firestore para entidade Room
                    val colaborador = com.example.gestaobilhares.data.entities.Colaborador(
                        id = roomId,
                        nome = data["nome"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        telefone = data["telefone"] as? String,
                        cpf = data["cpf"] as? String,
                        dataNascimento = null, // TODO: Implementar conversão de data se necessário
                        endereco = data["endereco"] as? String,
                        bairro = data["bairro"] as? String,
                        cidade = data["cidade"] as? String,
                        estado = data["estado"] as? String,
                        cep = data["cep"] as? String,
                        rg = data["rg"] as? String,
                        orgaoEmissor = data["orgaoEmissor"] as? String,
                        estadoCivil = data["estadoCivil"] as? String,
                        nomeMae = data["nomeMae"] as? String,
                        nomePai = data["nomePai"] as? String,
                        fotoPerfil = data["fotoPerfil"] as? String,
                        nivelAcesso = try {
                            com.example.gestaobilhares.data.entities.NivelAcesso.valueOf(
                                data["nivelAcesso"] as? String ?: "USER"
                            )
                        } catch (e: Exception) {
                            com.example.gestaobilhares.data.entities.NivelAcesso.USER
                        },
                        ativo = data["ativo"] as? Boolean ?: true,
                        aprovado = data["aprovado"] as? Boolean ?: false,
                        dataAprovacao = null, // TODO: Implementar conversão de data se necessário
                        aprovadoPor = data["aprovadoPor"] as? String,
                        firebaseUid = data["firebaseUid"] as? String,
                        googleId = data["googleId"] as? String,
                        senhaTemporaria = data["senhaTemporaria"] as? String,
                        emailAcesso = data["emailAcesso"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCadastro = java.util.Date(),
                        dataUltimoAcesso = null, // TODO: Implementar conversão de data se necessário
                        dataUltimaAtualizacao = java.util.Date()
                    )
                    
                    // Inserir no Room
                    colaboradorDao.inserir(colaborador)
                    colaboradoresSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Colaborador sincronizado: ${colaborador.nome} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar colaborador ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Colaboradores:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $colaboradoresSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $colaboradoresExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar colaboradores: ${e.message}", e)
        }
    }

    /**
     * PULL SYNC: Baixar despesas do Firestore para o app
     */
    private suspend fun pullDespesasFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Despesas do Firestore...")
        
        try {
            val despesasCollection = firestore.collection("empresas/$empresaId/despesas")
            val snapshot = despesasCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontradas ${snapshot.size()} despesas no Firestore")
            
            var despesasSincronizadas = 0
            var despesasExistentes = 0
            
            val despesaDao = database.despesaDao()
            val despesasExistentesList = despesaDao.buscarTodasComRota().first()
            
            for (document in snapshot) {
                try {
                    val data = document.data
                    val roomId = document.id.toLongOrNull() ?: continue
                    
                    // Verificar se já existe
                    val jaExiste = despesasExistentesList.any { it.id == roomId }
                    if (jaExiste) {
                        despesasExistentes++
                        android.util.Log.d("SyncManagerV2", "⏭️ Despesa já existe: ${data["descricao"]} (ID: $roomId)")
                        continue
                    }
                    
                    // Converter dados do Firestore para entidade Room
                    val despesa = com.example.gestaobilhares.data.entities.Despesa(
                        id = roomId,
                        rotaId = (data["rotaId"] as? Double)?.toLong() ?: 0L,
                        descricao = data["descricao"] as? String ?: "",
                        valor = (data["valor"] as? Double) ?: 0.0,
                        categoria = data["categoria"] as? String ?: "",
                        tipoDespesa = data["tipoDespesa"] as? String ?: "",
                        dataHora = try {
                            // Converter string para LocalDateTime se necessário
                            java.time.LocalDateTime.parse(data["dataHora"] as? String ?: java.time.LocalDateTime.now().toString())
                        } catch (e: Exception) {
                            java.time.LocalDateTime.now()
                        },
                        observacoes = data["observacoes"] as? String ?: "",
                        criadoPor = data["criadoPor"] as? String ?: "",
                        cicloId = (data["cicloId"] as? Double)?.toLong(),
                        origemLancamento = data["origemLancamento"] as? String ?: "ROTA",
                        cicloAno = (data["cicloAno"] as? Double)?.toInt(),
                        cicloNumero = (data["cicloNumero"] as? Double)?.toInt(),
                        fotoComprovante = null, // Será preenchido após download
                        veiculoId = (data["veiculoId"] as? Double)?.toLong(),
                        kmRodado = (data["kmRodado"] as? Double)?.toLong(),
                        litrosAbastecidos = data["litrosAbastecidos"] as? Double
                    )
                    
                    // ✅ NOVO: Download de foto do Firebase Storage se for URL
                    val fotoUrlFirebase = data["fotoComprovante"] as? String
                    val fotoComprovanteLocal = if (!fotoUrlFirebase.isNullOrBlank()) {
                        try {
                            val caminhoLocal = com.example.gestaobilhares.utils.FirebaseStorageManager.downloadFoto(
                                context = context,
                                urlFirebase = fotoUrlFirebase,
                                tipoFoto = "comprovante"
                            )
                            if (caminhoLocal != null) {
                                android.util.Log.d("SyncManagerV2", "✅ Foto de comprovante baixada: $caminhoLocal")
                            } else {
                                android.util.Log.w("SyncManagerV2", "⚠️ Falha ao baixar foto de comprovante: $fotoUrlFirebase")
                            }
                            caminhoLocal
                        } catch (e: Exception) {
                            android.util.Log.e("SyncManagerV2", "Erro ao baixar foto de comprovante: ${e.message}")
                            fotoUrlFirebase // Fallback: manter URL se download falhar
                        }
                    } else null
                    
                    // Atualizar despesa com caminho local da foto
                    val despesaComFoto = despesa.copy(fotoComprovante = fotoComprovanteLocal)
                    
                    // Inserir no Room
                    despesaDao.inserir(despesaComFoto)
                    despesasSincronizadas++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Despesa sincronizada: ${despesa.descricao} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar despesa ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Despesas:")
            android.util.Log.d("SyncManagerV2", "   Sincronizadas: $despesasSincronizadas")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $despesasExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar despesas: ${e.message}", e)
        }
    }

    /**
     * PULL SYNC: Baixar panos estoque do Firestore para o app
     */
    private suspend fun pullPanoEstoqueFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Panos Estoque do Firestore...")
        
        try {
            val panosCollection = firestore.collection("empresas/$empresaId/panosEstoque")
            val snapshot = panosCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} panos no Firestore")
            
            var panosSincronizados = 0
            var panosExistentes = 0
            
            val panoEstoqueDao = database.panoEstoqueDao()
            val panosExistentesList = panoEstoqueDao.listarTodos().first()
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    // ✅ CORREÇÃO: Ler roomId do campo "id" do documento (padrão dos outros PULLs)
                    val roomId = ((data["id"] as? Number)?.toLong()
                        ?: (data["roomId"] as? Number)?.toLong()
                        ?: document.id.toLongOrNull()) ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔍 Processando pano: ${data["numero"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = panosExistentesList.any { pano -> pano.id == roomId }
                    if (jaExiste) {
                        panosExistentes++
                        android.util.Log.d("SyncManagerV2", "⏭️ Pano já existe: ${data["numero"]} (ID: $roomId)")
                        continue
                    }
                    
                    // Converter dados do Firestore para entidade Room
                    val pano = com.example.gestaobilhares.data.entities.PanoEstoque(
                        id = roomId,
                        numero = data["numero"] as? String ?: "",
                        cor = data["cor"] as? String ?: "",
                        tamanho = data["tamanho"] as? String ?: "",
                        material = data["material"] as? String ?: "",
                        disponivel = data["disponivel"] as? Boolean ?: true,
                        observacoes = data["observacoes"] as? String
                    )
                    
                    // Inserir no Room
                    panoEstoqueDao.inserir(pano)
                    panosSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Pano sincronizado: ${pano.numero} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar pano ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Panos Estoque:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $panosSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $panosExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar panos estoque: ${e.message}", e)
        }
    }

    /**
     * PULL SYNC: Baixar mesas vendidas do Firestore para o app
     */
    private suspend fun pullMesaVendidaFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Mesas Vendidas do Firestore...")
        
        try {
            val mesasCollection = firestore.collection("empresas/$empresaId/mesasVendidas")
            val snapshot = mesasCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontradas ${snapshot.size()} mesas vendidas no Firestore")
            
            var mesasSincronizadas = 0
            var mesasExistentes = 0
            
            val mesaVendidaDao = database.mesaVendidaDao()
            val mesasExistentesList = mesaVendidaDao.listarTodas().first()
            
            for (document in snapshot) {
                try {
                    val data = document.data
                    val roomId = document.id.toLongOrNull() ?: continue
                    
                    // Verificar se já existe
                    val jaExiste = mesasExistentesList.any { mesa -> mesa.id == roomId }
                    if (jaExiste) {
                        mesasExistentes++
                        android.util.Log.d("SyncManagerV2", "⏭️ Mesa vendida já existe: ${data["numeroMesa"]} (ID: $roomId)")
                        continue
                    }
                    
                    // Converter dados do Firestore para entidade Room
                    val mesaVendida = com.example.gestaobilhares.data.entities.MesaVendida(
                        id = roomId,
                        mesaIdOriginal = (data["mesaIdOriginal"] as? Double)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: "",
                        tipoMesa = try {
                            com.example.gestaobilhares.data.entities.TipoMesa.valueOf(
                                data["tipoMesa"] as? String ?: "SINUCA"
                            )
                        } catch (e: Exception) {
                            com.example.gestaobilhares.data.entities.TipoMesa.SINUCA
                        },
                        tamanhoMesa = try {
                            com.example.gestaobilhares.data.entities.TamanhoMesa.valueOf(
                                data["tamanhoMesa"] as? String ?: "MEDIA"
                            )
                        } catch (e: Exception) {
                            com.example.gestaobilhares.data.entities.TamanhoMesa.MEDIA
                        },
                        estadoConservacao = try {
                            com.example.gestaobilhares.data.entities.EstadoConservacao.valueOf(
                                data["estadoConservacao"] as? String ?: "BOM"
                            )
                        } catch (e: Exception) {
                            com.example.gestaobilhares.data.entities.EstadoConservacao.BOM
                        },
                        nomeComprador = data["nomeComprador"] as? String ?: "",
                        telefoneComprador = data["telefoneComprador"] as? String,
                        cpfCnpjComprador = data["cpfCnpjComprador"] as? String,
                        enderecoComprador = data["enderecoComprador"] as? String,
                        valorVenda = (data["valorVenda"] as? Double) ?: 0.0,
                        dataVenda = try {
                            java.util.Date(data["dataVenda"] as? String ?: java.util.Date().toString())
                        } catch (e: Exception) {
                            java.util.Date()
                        },
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = try {
                            java.util.Date(data["dataCriacao"] as? String ?: java.util.Date().toString())
                        } catch (e: Exception) {
                            java.util.Date()
                        }
                    )
                    
                    // Inserir no Room
                    mesaVendidaDao.inserir(mesaVendida)
                    mesasSincronizadas++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Mesa vendida sincronizada: ${mesaVendida.numeroMesa} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.w("SyncManagerV2", "❌ Erro ao processar mesa vendida ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Mesas Vendidas:")
            android.util.Log.d("SyncManagerV2", "   Sincronizadas: $mesasSincronizadas")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $mesasExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar mesas vendidas: ${e.message}", e)
        }
    }

    /**
     * Baixar StockItems do Firestore
     */
    private suspend fun pullStockItemsFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Stock Items do Firestore...")
        
        try {
            val stockItemsCollection = firestore.collection("empresas/$empresaId/stockItems")
            val snapshot = stockItemsCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} stock items no Firestore")
            
            val stockItemDao = database.stockItemDao()
            val itemsExistentesList = stockItemDao.listarTodos().first()
            
            var itemsSincronizados = 0
            var itemsExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    // Aceitar id em diferentes chaves e formatos (sem usar run { continue })
                    val roomIdCandidate = ((data["id"] as? Number)?.toLong()
                        ?: (data["roomId"] as? Number)?.toLong()
                        ?: (data["id"] as? String)?.toLongOrNull()
                        ?: (data["roomId"] as? String)?.toLongOrNull())
                    if (roomIdCandidate == null) {
                        android.util.Log.w("CONTRACT_PULL", "INVALID contrato id: doc=${document.id} dataKeys=${data.keys}")
                        continue
                    }
                    val roomId = roomIdCandidate
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando stock item: ${data["name"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = itemsExistentesList.any { item -> item.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Stock item já existe: ${data["name"]} (ID: $roomId)")
                        itemsExistentes++
                        continue
                    }
                    
                    // Criar entidade StockItem
                    val stockItem = com.example.gestaobilhares.data.entities.StockItem(
                        id = roomId,
                        name = data["name"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                        unitPrice = (data["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                        supplier = data["supplier"] as? String ?: "",
                        description = data["description"] as? String,
                        createdAt = java.util.Date((data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        updatedAt = java.util.Date((data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    
                    // Inserir no banco local
                    stockItemDao.inserir(stockItem)
                    itemsSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Stock item sincronizado: ${stockItem.name} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar stock item ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Stock Items:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $itemsSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $itemsExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar stock items: ${e.message}", e)
        }
    }

    /**
     * Baixar Veículos do Firestore
     */
    private suspend fun pullVeiculosFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Veículos do Firestore...")
        
        try {
            val veiculosCollection = firestore.collection("empresas/$empresaId/veiculos")
            val snapshot = veiculosCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} veículos no Firestore")
            
            val veiculoDao = database.veiculoDao()
            val veiculosExistentesList = veiculoDao.listar().first()
            
            var veiculosSincronizados = 0
            var veiculosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando veículo: ${data["nome"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = veiculosExistentesList.any { veiculo -> veiculo.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Veículo já existe: ${data["nome"]} (ID: $roomId)")
                        veiculosExistentes++
                        continue
                    }
                    
                    // Criar entidade Veiculo
                    val veiculo = com.example.gestaobilhares.data.entities.Veiculo(
                        id = roomId,
                        nome = data["nome"] as? String ?: "",
                        placa = data["placa"] as? String ?: "",
                        marca = data["marca"] as? String ?: "",
                        modelo = data["modelo"] as? String ?: "",
                        anoModelo = (data["anoModelo"] as? Number)?.toInt() ?: 0,
                        kmAtual = (data["kmAtual"] as? Number)?.toLong() ?: 0L,
                        dataCompra = if (data["dataCompra"] != null) {
                            java.util.Date((data["dataCompra"] as? Number)?.toLong() ?: System.currentTimeMillis())
                        } else null,
                        observacoes = data["observacoes"] as? String
                    )
                    
                    // Inserir no banco local
                    veiculoDao.inserir(veiculo)
                    veiculosSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Veículo sincronizado: ${veiculo.nome} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar veículo ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Veículos:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $veiculosSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $veiculosExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar veículos: ${e.message}", e)
        }
    }

    /**
     * Baixar Histórico Manutenção Mesa do Firestore
     */
    private suspend fun pullHistoricoManutencaoMesaFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Histórico Manutenção Mesa do Firestore...")
        
        try {
            val historicoCollection = firestore.collection("empresas/$empresaId/historicoManutencaoMesa")
            val snapshot = historicoCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} históricos de manutenção mesa no Firestore")
            
            val historicoDao = database.historicoManutencaoMesaDao()
            val historicosExistentesList = historicoDao.listarTodos().first()
            
            var historicosSincronizados = 0
            var historicosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando histórico mesa: ${data["numeroMesa"]} - ${data["tipoManutencao"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = historicosExistentesList.any { historico -> historico.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Histórico mesa já existe: ${data["numeroMesa"]} (ID: $roomId)")
                        historicosExistentes++
                        continue
                    }
                    
                    // Criar entidade HistoricoManutencaoMesa
                    val historico = com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa(
                        id = roomId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: "",
                        tipoManutencao = com.example.gestaobilhares.data.entities.TipoManutencao.valueOf(data["tipoManutencao"] as? String ?: "OUTROS"),
                        descricao = data["descricao"] as? String,
                        dataManutencao = java.util.Date((data["dataManutencao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        responsavel = data["responsavel"] as? String,
                        observacoes = data["observacoes"] as? String,
                        custo = (data["custo"] as? Number)?.toDouble(),
                        fotoAntes = data["fotoAntes"] as? String,
                        fotoDepois = data["fotoDepois"] as? String,
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    
                    // Inserir no banco local
                    historicoDao.inserir(historico)
                    historicosSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Histórico mesa sincronizado: ${historico.numeroMesa} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar histórico mesa ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Histórico Manutenção Mesa:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $historicosSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $historicosExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar histórico manutenção mesa: ${e.message}", e)
        }
    }

    /**
     * Baixar Histórico Manutenção Veículo do Firestore
     */
    private suspend fun pullHistoricoManutencaoVeiculoFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Histórico Manutenção Veículo do Firestore...")
        
        try {
            val historicoCollection = firestore.collection("empresas/$empresaId/historicoManutencaoVeiculo")
            val snapshot = historicoCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} históricos de manutenção veículo no Firestore")
            
            val historicoDao = database.historicoManutencaoVeiculoDao()
            val historicosExistentesList = historicoDao.listarPorVeiculo(0L).first() // Lista todos
            
            var historicosSincronizados = 0
            var historicosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando histórico veículo: ${data["veiculoId"]} - ${data["tipoManutencao"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = historicosExistentesList.any { historico -> historico.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Histórico veículo já existe: ${data["veiculoId"]} (ID: $roomId)")
                        historicosExistentes++
                        continue
                    }
                    
                    // Criar entidade HistoricoManutencaoVeiculo
                    val historico = com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo(
                        id = roomId,
                        veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: 0L,
                        tipoManutencao = data["tipoManutencao"] as? String ?: "",
                        descricao = data["descricao"] as? String ?: "",
                        dataManutencao = java.util.Date((data["dataManutencao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                        kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: 0L,
                        responsavel = data["responsavel"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    
                    // Inserir no banco local
                    historicoDao.inserir(historico)
                    historicosSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Histórico veículo sincronizado: ${historico.veiculoId} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar histórico veículo ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Histórico Manutenção Veículo:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $historicosSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $historicosExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar histórico manutenção veículo: ${e.message}", e)
        }
    }

    /**
     * Baixar Histórico Combustível Veículo do Firestore
     */
    private suspend fun pullHistoricoCombustivelVeiculoFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Histórico Combustível Veículo do Firestore...")
        
        try {
            val historicoCollection = firestore.collection("empresas/$empresaId/historicoCombustivelVeiculo")
            val snapshot = historicoCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} históricos de combustível veículo no Firestore")
            
            val historicoDao = database.historicoCombustivelVeiculoDao()
            val historicosExistentesList = historicoDao.listarPorVeiculo(0L).first() // Lista todos
            
            var historicosSincronizados = 0
            var historicosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando histórico combustível: ${data["veiculoId"]} - ${data["litros"]}L (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = historicosExistentesList.any { historico -> historico.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Histórico combustível já existe: ${data["veiculoId"]} (ID: $roomId)")
                        historicosExistentes++
                        continue
                    }
                    
                    // Criar entidade HistoricoCombustivelVeiculo
                    val historico = com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo(
                        id = roomId,
                        veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: 0L,
                        dataAbastecimento = java.util.Date((data["dataAbastecimento"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        litros = (data["litros"] as? Number)?.toDouble() ?: 0.0,
                        valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                        kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: 0L,
                        kmRodado = (data["kmRodado"] as? Number)?.toDouble() ?: 0.0,
                        posto = data["posto"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    
                    // Inserir no banco local
                    historicoDao.inserir(historico)
                    historicosSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Histórico combustível sincronizado: ${historico.veiculoId} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar histórico combustível ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Histórico Combustível Veículo:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $historicosSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $historicosExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar histórico combustível veículo: ${e.message}", e)
        }
    }

    /**
     * Baixar Categorias Despesa do Firestore
     */
    private suspend fun pullCategoriasDespesaFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Categorias Despesa do Firestore...")
        
        try {
            val categoriasCollection = firestore.collection("empresas/$empresaId/categoriasDespesa")
            val snapshot = categoriasCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontradas ${snapshot.size()} categorias despesa no Firestore")
            
            val categoriaDao = database.categoriaDespesaDao()
            val categoriasExistentesList = categoriaDao.buscarTodas().first()
            
            var categoriasSincronizadas = 0
            var categoriasExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando categoria: ${data["nome"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = categoriasExistentesList.any { categoria -> categoria.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Categoria já existe: ${data["nome"]} (ID: $roomId)")
                        categoriasExistentes++
                        continue
                    }
                    
                    // Criar entidade CategoriaDespesa
                    val categoria = com.example.gestaobilhares.data.entities.CategoriaDespesa(
                        id = roomId,
                        nome = data["nome"] as? String ?: "",
                        descricao = data["descricao"] as? String ?: "",
                        ativa = (data["ativa"] as? Boolean) ?: true,
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        dataAtualizacao = java.util.Date((data["dataAtualizacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        criadoPor = data["criadoPor"] as? String ?: ""
                    )
                    
                    // Inserir no banco local
                    categoriaDao.inserir(categoria)
                    categoriasSincronizadas++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Categoria sincronizada: ${categoria.nome} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar categoria ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Categorias Despesa:")
            android.util.Log.d("SyncManagerV2", "   Sincronizadas: $categoriasSincronizadas")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $categoriasExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar categorias despesa: ${e.message}", e)
        }
    }

    /**
     * Baixar Tipos Despesa do Firestore
     */
    private suspend fun pullTiposDespesaFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Tipos Despesa do Firestore...")
        
        try {
            val tiposCollection = firestore.collection("empresas/$empresaId/tiposDespesa")
            val snapshot = tiposCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} tipos despesa no Firestore")
            
            val tipoDao = database.tipoDespesaDao()
            val tiposExistentesList = tipoDao.buscarTodos().first()
            
            var tiposSincronizados = 0
            var tiposExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando tipo: ${data["nome"]} - Categoria ${data["categoriaId"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = tiposExistentesList.any { tipo -> tipo.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Tipo já existe: ${data["nome"]} (ID: $roomId)")
                        tiposExistentes++
                        continue
                    }
                    
                    // Criar entidade TipoDespesa
                    val tipo = com.example.gestaobilhares.data.entities.TipoDespesa(
                        id = roomId,
                        categoriaId = (data["categoriaId"] as? Number)?.toLong() ?: 0L,
                        nome = data["nome"] as? String ?: "",
                        descricao = data["descricao"] as? String ?: "",
                        ativo = (data["ativo"] as? Boolean) ?: true,
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        dataAtualizacao = java.util.Date((data["dataAtualizacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        criadoPor = data["criadoPor"] as? String ?: ""
                    )
                    
                    // Inserir no banco local
                    tipoDao.inserir(tipo)
                    tiposSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Tipo sincronizado: ${tipo.nome} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar tipo ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Tipos Despesa:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $tiposSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $tiposExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar tipos despesa: ${e.message}", e)
        }
    }

    /**
     * Baixar Contratos Locação do Firestore
     */
    private suspend fun pullContratosLocacaoFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Contratos Locação do Firestore...")
        android.util.Log.d("CONTRACT_PULL", "INIT contratos empresa=$empresaId")
        try {
            val base = firestore.collection("empresas").document(empresaId)
            val candidates = listOf("contratosLocacao", "contratolocacao", "contratoslocacao", "contratos")
            var usedName = ""
            var snapshot: com.google.firebase.firestore.QuerySnapshot? = null
            for (name in candidates) {
                val snap = base.collection(name).get().await()
                android.util.Log.d("CONTRACT_PULL", "TRY '$name' size=${snap.size()}")
                if (!snap.isEmpty) { usedName = name; snapshot = snap; break }
            }
            if (snapshot == null || snapshot.isEmpty) {
                android.util.Log.w("CONTRACT_PULL", "EMPTY all top-level candidates; trying nested under clientes/rotas...")

                val baseEmpresa = firestore.collection("empresas").document(empresaId)
                var nestedSynced = 0
                val contratoDaoNested = database.contratoLocacaoDao()

                // 1) clientes/{clienteId}/contratos
                val clientesSnap = baseEmpresa.collection("clientes").get().await()
                for (clienteDoc in clientesSnap.documents) {
                    try {
                        val nested = clienteDoc.reference.collection("contratos").get().await()
                        android.util.Log.d("CONTRACT_PULL", "TRY nested cliente contratos cliente=${clienteDoc.id} size=${nested.size()}")
                        for (document in nested.documents) {
                            val data = document.data ?: continue
                            val roomIdCandidate = ((data["id"] as? Number)?.toLong()
                                ?: (data["roomId"] as? Number)?.toLong()
                                ?: (data["id"] as? String)?.toLongOrNull()
                                ?: (data["roomId"] as? String)?.toLongOrNull())
                            if (roomIdCandidate == null) { android.util.Log.w("CONTRACT_PULL", "INVALID nested cliente contrato id: ${document.id}"); continue }
                            val contrato = com.example.gestaobilhares.data.entities.ContratoLocacao(
                                id = roomIdCandidate,
                                numeroContrato = (data["numeroContrato"] as? String) ?: (data["numero"] as? String) ?: "",
                                clienteId = ((data["clienteId"] as? Number)?.toLong()
                                    ?: (data["clienteId"] as? String)?.toLongOrNull() ?: clienteDoc.id.toLongOrNull() ?: 0L),
                                locadorNome = data["locadorNome"] as? String ?: "BILHAR GLOBO R & A LTDA",
                                locadorCnpj = data["locadorCnpj"] as? String ?: "34.994.884/0001-69",
                                locadorEndereco = data["locadorEndereco"] as? String ?: "Rua João Pinheiro, nº 765, Bairro Centro, Montes Claros, MG",
                                locadorCep = data["locadorCep"] as? String ?: "39.400-093",
                                locatarioNome = data["locatarioNome"] as? String ?: "",
                                locatarioCpf = data["locatarioCpf"] as? String ?: "",
                                locatarioEndereco = data["locatarioEndereco"] as? String ?: "",
                                locatarioTelefone = data["locatarioTelefone"] as? String ?: "",
                                locatarioEmail = data["locatarioEmail"] as? String ?: "",
                                valorMensal = (data["valorMensal"] as? Number)?.toDouble() ?: 0.0,
                                diaVencimento = (data["diaVencimento"] as? Number)?.toInt() ?: 1,
                                tipoPagamento = data["tipoPagamento"] as? String ?: "FIXO",
                                percentualReceita = (data["percentualReceita"] as? Number)?.toDouble(),
                                dataContrato = java.util.Date((data["dataContrato"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                                dataInicio = java.util.Date((data["dataInicio"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                                status = data["status"] as? String ?: "ATIVO",
                                dataEncerramento = ((data["dataEncerramento"] as? Number)
                                    ?: (data["dataEncerramento"] as? String)?.toLongOrNull())?.let { java.util.Date(it.toLong()) },
                                assinaturaLocador = data["assinaturaLocador"] as? String,
                                assinaturaLocatario = data["assinaturaLocatario"] as? String,
                                distratoAssinaturaLocador = data["distratoAssinaturaLocador"] as? String,
                                distratoAssinaturaLocatario = data["distratoAssinaturaLocatario"] as? String,
                                distratoDataAssinatura = ((data["distratoDataAssinatura"] as? Number)
                                    ?: (data["distratoDataAssinatura"] as? String)?.toLongOrNull())?.let { java.util.Date(it.toLong()) },
                                dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                                dataAtualizacao = java.util.Date((data["dataAtualizacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                            )
                            contratoDaoNested.inserirContrato(contrato)
                            nestedSynced++
                            android.util.Log.d("CONTRACT_PULL", "INSERT_NESTED_CLIENTE id=${contrato.id} numero=${contrato.numeroContrato}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CONTRACT_PULL", "ERROR_NESTED_CLIENTE contratos cliente=${clienteDoc.id} ${e.message}", e)
                    }
                }

                // 2) rotas/{rotaId}/contratos
                val rotasSnap = baseEmpresa.collection("rotas").get().await()
                for (rotaDoc in rotasSnap.documents) {
                    try {
                        val nested = rotaDoc.reference.collection("contratos").get().await()
                        android.util.Log.d("CONTRACT_PULL", "TRY nested rota contratos rota=${rotaDoc.id} size=${nested.size()}")
                        for (document in nested.documents) {
                            val data = document.data ?: continue
                            val roomIdCandidate = ((data["id"] as? Number)?.toLong()
                                ?: (data["roomId"] as? Number)?.toLong()
                                ?: (data["id"] as? String)?.toLongOrNull()
                                ?: (data["roomId"] as? String)?.toLongOrNull())
                            if (roomIdCandidate == null) { android.util.Log.w("CONTRACT_PULL", "INVALID nested rota contrato id: ${document.id}"); continue }
                            val contrato = com.example.gestaobilhares.data.entities.ContratoLocacao(
                                id = roomIdCandidate,
                                numeroContrato = (data["numeroContrato"] as? String) ?: (data["numero"] as? String) ?: "",
                                clienteId = ((data["clienteId"] as? Number)?.toLong()
                                    ?: (data["clienteId"] as? String)?.toLongOrNull() ?: 0L),
                                locadorNome = data["locadorNome"] as? String ?: "BILHAR GLOBO R & A LTDA",
                                locadorCnpj = data["locadorCnpj"] as? String ?: "34.994.884/0001-69",
                                locadorEndereco = data["locadorEndereco"] as? String ?: "Rua João Pinheiro, nº 765, Bairro Centro, Montes Claros, MG",
                                locadorCep = data["locadorCep"] as? String ?: "39.400-093",
                                locatarioNome = data["locatarioNome"] as? String ?: "",
                                locatarioCpf = data["locatarioCpf"] as? String ?: "",
                                locatarioEndereco = data["locatarioEndereco"] as? String ?: "",
                                locatarioTelefone = data["locatarioTelefone"] as? String ?: "",
                                locatarioEmail = data["locatarioEmail"] as? String ?: "",
                                valorMensal = (data["valorMensal"] as? Number)?.toDouble() ?: 0.0,
                                diaVencimento = (data["diaVencimento"] as? Number)?.toInt() ?: 1,
                                tipoPagamento = data["tipoPagamento"] as? String ?: "FIXO",
                                percentualReceita = (data["percentualReceita"] as? Number)?.toDouble(),
                                dataContrato = java.util.Date((data["dataContrato"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                                dataInicio = java.util.Date((data["dataInicio"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                                status = data["status"] as? String ?: "ATIVO",
                                dataEncerramento = ((data["dataEncerramento"] as? Number)
                                    ?: (data["dataEncerramento"] as? String)?.toLongOrNull())?.let { java.util.Date(it.toLong()) },
                                assinaturaLocador = data["assinaturaLocador"] as? String,
                                assinaturaLocatario = data["assinaturaLocatario"] as? String,
                                distratoAssinaturaLocador = data["distratoAssinaturaLocador"] as? String,
                                distratoAssinaturaLocatario = data["distratoAssinaturaLocatario"] as? String,
                                distratoDataAssinatura = ((data["distratoDataAssinatura"] as? Number)
                                    ?: (data["distratoDataAssinatura"] as? String)?.toLongOrNull())?.let { java.util.Date(it.toLong()) },
                                dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                                dataAtualizacao = java.util.Date((data["dataAtualizacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                            )
                            contratoDaoNested.inserirContrato(contrato)
                            nestedSynced++
                            android.util.Log.d("CONTRACT_PULL", "INSERT_NESTED_ROTA id=${contrato.id} numero=${contrato.numeroContrato}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CONTRACT_PULL", "ERROR_NESTED_ROTA contratos rota=${rotaDoc.id} ${e.message}", e)
                    }
                }

                android.util.Log.d("CONTRACT_PULL", "SUMMARY_NESTED contratos synced=$nestedSynced")
                return
            }
            android.util.Log.d("CONTRACT_PULL", "USING '$usedName' size=${snapshot.size()}")
            
            val contratoDao = database.contratoLocacaoDao()
            val contratosExistentesList = contratoDao.buscarTodosContratos().first()
            
            var contratosSincronizados = 0
            var contratosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("CONTRACT_PULL", "DOC id=${document.id} numero=${data["numeroContrato"]} cliente=${data["clienteId"]} roomId=$roomId")
                    
                    // Verificar se já existe
                    val jaExiste = contratosExistentesList.any { contrato -> contrato.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("CONTRACT_PULL", "SKIP_EXISTING numero=${data["numeroContrato"]} id=$roomId")
                        contratosExistentes++
                        continue
                    }
                    
                    // Criar entidade ContratoLocacao
                    val contrato = com.example.gestaobilhares.data.entities.ContratoLocacao(
                        id = roomId,
                        numeroContrato = (data["numeroContrato"] as? String)
                            ?: (data["numero"] as? String) ?: "",
                        clienteId = ((data["clienteId"] as? Number)?.toLong()
                            ?: (data["clienteId"] as? String)?.toLongOrNull()) ?: 0L,
                        locadorNome = data["locadorNome"] as? String ?: "BILHAR GLOBO R & A LTDA",
                        locadorCnpj = data["locadorCnpj"] as? String ?: "34.994.884/0001-69",
                        locadorEndereco = data["locadorEndereco"] as? String ?: "Rua João Pinheiro, nº 765, Bairro Centro, Montes Claros, MG",
                        locadorCep = data["locadorCep"] as? String ?: "39.400-093",
                        locatarioNome = data["locatarioNome"] as? String ?: "",
                        locatarioCpf = data["locatarioCpf"] as? String ?: "",
                        locatarioEndereco = data["locatarioEndereco"] as? String ?: "",
                        locatarioTelefone = data["locatarioTelefone"] as? String ?: "",
                        locatarioEmail = data["locatarioEmail"] as? String ?: "",
                        valorMensal = (data["valorMensal"] as? Number)?.toDouble() ?: 0.0,
                        diaVencimento = (data["diaVencimento"] as? Number)?.toInt() ?: 1,
                        tipoPagamento = data["tipoPagamento"] as? String ?: "FIXO",
                        percentualReceita = (data["percentualReceita"] as? Number)?.toDouble(),
                        dataContrato = java.util.Date((data["dataContrato"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        dataInicio = java.util.Date((data["dataInicio"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        status = data["status"] as? String ?: "ATIVO",
                        dataEncerramento = ((data["dataEncerramento"] as? Number)
                            ?: (data["dataEncerramento"] as? String)?.toLongOrNull())?.let { java.util.Date(it.toLong()) },
                        assinaturaLocador = data["assinaturaLocador"] as? String,
                        assinaturaLocatario = data["assinaturaLocatario"] as? String,
                        distratoAssinaturaLocador = data["distratoAssinaturaLocador"] as? String,
                        distratoAssinaturaLocatario = data["distratoAssinaturaLocatario"] as? String,
                        distratoDataAssinatura = ((data["distratoDataAssinatura"] as? Number)
                            ?: (data["distratoDataAssinatura"] as? String)?.toLongOrNull())?.let { java.util.Date(it.toLong()) },
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        dataAtualizacao = java.util.Date((data["dataAtualizacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    
                    // Inserir no banco local
                    contratoDao.inserirContrato(contrato)
                    contratosSincronizados++
                    android.util.Log.d("CONTRACT_PULL", "INSERT id=$roomId numero=${contrato.numeroContrato}")
                    
                } catch (e: Exception) {
                    android.util.Log.e("CONTRACT_PULL", "ERROR_PROCESS doc=${document.id} msg=${e.message}", e)
                }
            }
            android.util.Log.d("CONTRACT_PULL", "SUMMARY contratos synced=$contratosSincronizados existing=$contratosExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("CONTRACT_PULL", "ERROR ${e.message}", e)
        }
    }

    /**
     * Baixar Aditivos de Contrato do Firestore
     */
    private suspend fun pullAditivosContratoFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Aditivos de Contrato do Firestore...")
        try {
            val collectionName = getCollectionName("AditivoContrato")
            val snapshot = firestore.collection("empresas").document(empresaId).collection(collectionName).get().await()
            val dao = database.aditivoContratoDao()
            val existentes = dao.buscarTodosAditivos().first()
            var countNew = 0
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    if (existentes.any { it.id == roomId }) continue
                    val entity = com.example.gestaobilhares.data.entities.AditivoContrato(
                        id = roomId,
                        numeroAditivo = data["numeroAditivo"] as? String ?: "",
                        contratoId = (data["contratoId"] as? Number)?.toLong() ?: 0L,
                        dataAditivo = java.util.Date((data["dataAditivo"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        observacoes = data["observacoes"] as? String,
                        tipo = data["tipo"] as? String ?: "INCLUSAO",
                        assinaturaLocador = data["assinaturaLocador"] as? String,
                        assinaturaLocatario = data["assinaturaLocatario"] as? String,
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        dataAtualizacao = java.util.Date((data["dataAtualizacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    dao.inserirAditivo(entity)
                    countNew++
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "Erro ao processar AditivoContrato ${doc.id}: ${e.message}", e)
                }
            }
            android.util.Log.d("SyncManagerV2", "✅ Aditivos sincronizados: $countNew")
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro no PULL Aditivos de Contrato: ${e.message}", e)
        }
    }

    /**
     * Baixar Aditivo Mesas do Firestore
     */
    private suspend fun pullAditivoMesasFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL AditivoMesas do Firestore...")
        try {
            val collectionName = getCollectionName("AditivoMesa")
            val snapshot = firestore.collection("empresas").document(empresaId).collection(collectionName).get().await()
            val dao = database.aditivoContratoDao()
            var countNew = 0
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    // Não buscamos existentes por performance; REPLACE na inserção do Room
                    val entity = com.example.gestaobilhares.data.entities.AditivoMesa(
                        id = roomId,
                        aditivoId = (data["aditivoId"] as? Number)?.toLong() ?: 0L,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: 0L,
                        tipoEquipamento = data["tipoEquipamento"] as? String ?: "SINUCA",
                        numeroSerie = data["numeroSerie"] as? String ?: "",
                        valorFicha = (data["valorFicha"] as? Number)?.toDouble(),
                        valorFixo = (data["valorFixo"] as? Number)?.toDouble()
                    )
                    dao.inserirAditivoMesas(listOf(entity))
                    countNew++
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "Erro ao processar AditivoMesa ${doc.id}: ${e.message}", e)
                }
            }
            android.util.Log.d("SyncManagerV2", "✅ AditivoMesas sincronizadas: $countNew")
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro no PULL AditivoMesas: ${e.message}", e)
        }
    }

    /**
     * Baixar Contrato Mesas do Firestore
     */
    private suspend fun pullContratoMesasFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL ContratoMesas do Firestore...")
        android.util.Log.d("CONTRACT_PULL", "INIT contratoMesas empresa=$empresaId")
        try {
            val base = firestore.collection("empresas").document(empresaId)
            val candidates = listOf(
                getCollectionName("ContratoMesa"),
                "contratoMesas",
                "contratosMesas",
                "contratosmesas",
                "contratomesas"
            )
            var usedName = ""
            var snapshot: com.google.firebase.firestore.QuerySnapshot? = null
            for (name in candidates) {
                val snap = base.collection(name).get().await()
                android.util.Log.d("CONTRACT_PULL", "TRY mesas '$name' size=${snap.size()}")
                if (!snap.isEmpty) { usedName = name; snapshot = snap; break }
            }
            if (snapshot == null || snapshot.isEmpty) {
                android.util.Log.w("CONTRACT_PULL", "EMPTY contratoMesas in top-level; trying nested under each contrato...")
                // Fallback: ler subcoleções dentro de cada contrato
                val baseContratos = firestore.collection("empresas").document(empresaId)
                val contratosSnap = baseContratos.collection("contratosLocacao").get().await()
                var nestedInserted = 0
                val daoNested = database.contratoLocacaoDao()
                for (contratoDoc in contratosSnap.documents) {
                    try {
                        val nestedCandidates = listOf("mesas", "contratoMesas", "contratosMesas")
                        for (nc in nestedCandidates) {
                            val nested = contratoDoc.reference.collection(nc).get().await()
                            android.util.Log.d("CONTRACT_PULL", "TRY nested '$nc' contrato=${contratoDoc.id} size=${nested.size()}")
                            if (nested.isEmpty) continue
                            for (doc in nested.documents) {
                                val data = doc.data ?: continue
                                val contratoMesaIdCandidate = ((data["id"] as? Number)?.toLong()
                                    ?: (data["roomId"] as? Number)?.toLong()
                                    ?: (data["id"] as? String)?.toLongOrNull()
                                    ?: (data["roomId"] as? String)?.toLongOrNull())
                                if (contratoMesaIdCandidate == null) {
                                    android.util.Log.w("CONTRACT_PULL", "INVALID nested contratoMesa id: doc=${doc.id} keys=${data.keys}")
                                    continue
                                }
                                val entity = com.example.gestaobilhares.data.entities.ContratoMesa(
                                    id = contratoMesaIdCandidate,
                                    contratoId = ((data["contratoId"] as? Number)?.toLong()
                                        ?: (data["contratoId"] as? String)?.toLongOrNull()
                                        ?: contratoDoc.id.toLongOrNull() ?: 0L),
                                    mesaId = ((data["mesaId"] as? Number)?.toLong()
                                        ?: (data["mesaId"] as? String)?.toLongOrNull()) ?: 0L,
                                    tipoEquipamento = data["tipoEquipamento"] as? String ?: "SINUCA",
                                    numeroSerie = data["numeroSerie"] as? String ?: "",
                                    valorFicha = (data["valorFicha"] as? Number)?.toDouble(),
                                    valorFixo = (data["valorFixo"] as? Number)?.toDouble()
                                )
                                daoNested.inserirContratoMesa(entity)
                                nestedInserted++
                                android.util.Log.d("CONTRACT_PULL", "INSERT_MESA_NESTED id=${entity.id} contratoId=${entity.contratoId} mesaId=${entity.mesaId}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CONTRACT_PULL", "ERROR_NESTED_MESAS contrato=${contratoDoc.id} ${e.message}", e)
                    }
                }
                android.util.Log.d("CONTRACT_PULL", "SUMMARY_MESAS_NESTED inserted=$nestedInserted")
                return
            }
            android.util.Log.d("CONTRACT_PULL", "USING mesas '$usedName' size=${snapshot.size()}")
            val dao = database.contratoLocacaoDao()
            var countNew = 0
            val contratosExistentes = dao.buscarTodosContratos().first()
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    // Aceitar id em diferentes chaves e formatos para ContratoMesa (sem run { continue })
                    val contratoMesaIdCandidate = ((data["id"] as? Number)?.toLong()
                        ?: (data["roomId"] as? Number)?.toLong()
                        ?: (data["id"] as? String)?.toLongOrNull()
                        ?: (data["roomId"] as? String)?.toLongOrNull())
                    if (contratoMesaIdCandidate == null) {
                        android.util.Log.w("CONTRACT_PULL", "INVALID contratoMesa id: doc=${doc.id} dataKeys=${data.keys}")
                        continue
                    }
                    val roomId = contratoMesaIdCandidate
                    val entity = com.example.gestaobilhares.data.entities.ContratoMesa(
                        id = roomId,
                        contratoId = ((data["contratoId"] as? Number)?.toLong()
                            ?: (data["contratoId"] as? String)?.toLongOrNull()) ?: 0L,
                        mesaId = ((data["mesaId"] as? Number)?.toLong()
                            ?: (data["mesaId"] as? String)?.toLongOrNull()) ?: 0L,
                        tipoEquipamento = data["tipoEquipamento"] as? String ?: "SINUCA",
                        numeroSerie = data["numeroSerie"] as? String ?: "",
                        valorFicha = (data["valorFicha"] as? Number)?.toDouble(),
                        valorFixo = (data["valorFixo"] as? Number)?.toDouble()
                    )
                    // Proteger contra FK inválida: pular se contrato pai não existir localmente
                    val paiExiste = contratosExistentes.any { it.id == entity.contratoId }
                    if (!paiExiste) {
                        android.util.Log.w("CONTRACT_PULL", "SKIP_MESA_NO_PARENT_CONTRACT id=$roomId contratoId=${entity.contratoId}. Parent not found; pulando para evitar FK 787")
                        continue
                    }
                    dao.inserirContratoMesa(entity)
                    countNew++
                    android.util.Log.d("CONTRACT_PULL", "INSERT_MESA id=$roomId contratoId=${entity.contratoId} mesaId=${entity.mesaId}")
                } catch (e: Exception) {
                    android.util.Log.e("CONTRACT_PULL", "ERROR_PROCESS_MESA doc=${doc.id} msg=${e.message}", e)
                }
            }
            android.util.Log.d("CONTRACT_PULL", "SUMMARY_MESAS synced=$countNew")
        } catch (e: Exception) {
            android.util.Log.e("CONTRACT_PULL", "ERROR_MESAS ${e.message}", e)
        }
    }

    /**
     * Baixar Metas de Colaborador do Firestore
     */
    private suspend fun pullMetasFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Metas do Firestore...")
        try {
            val collectionName = getCollectionName("MetaColaborador")
            val snapshot = firestore.collection("empresas").document(empresaId).collection(collectionName).get().await()
            val dao = database.colaboradorDao()
            var countNew = 0
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    val entity = com.example.gestaobilhares.data.entities.MetaColaborador(
                        id = roomId,
                        colaboradorId = (data["colaboradorId"] as? Number)?.toLong() ?: 0L,
                        rotaId = (data["rotaId"] as? Number)?.toLong(),
                        cicloId = (data["cicloId"] as? Number)?.toLong() ?: 0L,
                        tipoMeta = com.example.gestaobilhares.data.entities.TipoMeta.valueOf((data["tipoMeta"] as? String) ?: "RECEITA"),
                        valorMeta = (data["valorMeta"] as? Number)?.toDouble() ?: 0.0,
                        valorAtual = (data["valorAtual"] as? Number)?.toDouble() ?: 0.0,
                        ativo = data["ativo"] as? Boolean ?: true,
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    dao.inserirMeta(entity)
                    countNew++
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "Erro ao processar Meta ${doc.id}: ${e.message}", e)
                }
            }
            android.util.Log.d("SyncManagerV2", "✅ Metas sincronizadas: $countNew")
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro no PULL Metas: ${e.message}", e)
        }
    }

    /**
     * Baixar vinculações Colaborador-Rota do Firestore
     */
    private suspend fun pullColaboradoresRotasFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL ColaboradorRota do Firestore...")
        try {
            val collectionName = getCollectionName("ColaboradorRota")
            val snapshot = firestore.collection("empresas").document(empresaId).collection(collectionName).get().await()
            val dao = database.colaboradorDao()
            var countNew = 0
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val entity = com.example.gestaobilhares.data.entities.ColaboradorRota(
                        colaboradorId = (data["colaboradorId"] as? Number)?.toLong() ?: 0L,
                        rotaId = (data["rotaId"] as? Number)?.toLong() ?: 0L,
                        responsavelPrincipal = data["responsavelPrincipal"] as? Boolean ?: false,
                        dataVinculacao = java.util.Date((data["dataVinculacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    dao.inserirColaboradorRota(entity)
                    countNew++
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "Erro ao processar ColaboradorRota ${doc.id}: ${e.message}", e)
                }
            }
            android.util.Log.d("SyncManagerV2", "✅ ColaboradorRota sincronizados: $countNew")
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro no PULL ColaboradorRota: ${e.message}", e)
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
     * PULL: Baixar Assinaturas Representante Legal do Firestore
     */
    private suspend fun pullAssinaturasRepresentanteLegalFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Assinaturas Representante Legal do Firestore...")
        
        try {
            val assinaturasCollection = firestore.collection("empresas/$empresaId/assinaturasRepresentanteLegal")
            val snapshot = assinaturasCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontradas ${snapshot.size()} assinaturas no Firestore")
            
            val assinaturaDao = database.assinaturaRepresentanteLegalDao()
            val assinaturasExistentesList = assinaturaDao.obterTodasAssinaturas()
            
            var assinaturasSincronizadas = 0
            var assinaturasExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando assinatura: ${data["nomeRepresentante"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = assinaturasExistentesList.any { assinatura -> assinatura.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Assinatura já existe: ${data["nomeRepresentante"]} (ID: $roomId)")
                        assinaturasExistentes++
                        continue
                    }
                    
                    // Criar entidade AssinaturaRepresentanteLegal
                    val assinatura = com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal(
                        id = roomId,
                        nomeRepresentante = data["nomeRepresentante"] as? String ?: "",
                        cpfRepresentante = data["cpfRepresentante"] as? String ?: "",
                        cargoRepresentante = data["cargoRepresentante"] as? String ?: "",
                        assinaturaBase64 = data["assinaturaBase64"] as? String ?: "",
                        timestampCriacao = (data["timestampCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        deviceId = data["deviceId"] as? String ?: "",
                        hashIntegridade = data["hashIntegridade"] as? String ?: "",
                        versaoSistema = data["versaoSistema"] as? String ?: "",
                        dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        criadoPor = data["criadoPor"] as? String ?: "",
                        ativo = (data["ativo"] as? Boolean) ?: true,
                        numeroProcuração = data["numeroProcuração"] as? String ?: "",
                        dataProcuração = java.util.Date((data["dataProcuração"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        poderesDelegados = data["poderesDelegados"] as? String ?: "",
                        validadeProcuração = (data["validadeProcuração"] as? Number)?.let { java.util.Date(it.toLong()) },
                        totalUsos = (data["totalUsos"] as? Number)?.toInt() ?: 0,
                        ultimoUso = (data["ultimoUso"] as? Number)?.let { java.util.Date(it.toLong()) },
                        contratosAssinados = data["contratosAssinados"] as? String ?: "",
                        validadaJuridicamente = (data["validadaJuridicamente"] as? Boolean) ?: false,
                        dataValidacao = (data["dataValidacao"] as? Number)?.let { java.util.Date(it.toLong()) },
                        validadoPor = data["validadoPor"] as? String
                    )
                    
                    // Inserir no banco local
                    assinaturaDao.inserirAssinatura(assinatura)
                    assinaturasSincronizadas++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Assinatura sincronizada: ${assinatura.nomeRepresentante} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar assinatura ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Assinaturas Representante Legal:")
            android.util.Log.d("SyncManagerV2", "   Sincronizadas: $assinaturasSincronizadas")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $assinaturasExistentes")
            // ✅ Alinhar assinatura do representante aos contratos sem assinaturaLocador
            try {
                val contratoDao = database.contratoLocacaoDao()
                val contratos = contratoDao.buscarTodosContratos().first()
                val todasAssinaturas = assinaturaDao.obterTodasAssinaturas()
                val assinaturaMaisRecente = todasAssinaturas.maxByOrNull { it.timestampCriacao }
                var atualizados = 0
                if (assinaturaMaisRecente != null) {
                    for (contrato in contratos) {
                        if (contrato.assinaturaLocador.isNullOrEmpty()) {
                            val atualizado = contrato.copy(
                                assinaturaLocador = assinaturaMaisRecente.assinaturaBase64,
                                dataAtualizacao = java.util.Date()
                            )
                            contratoDao.atualizarContrato(atualizado)
                            atualizados++
                        }
                    }
                }
                android.util.Log.d("CONTRACT_PULL", "ALIGN_REP_SIGNATURE updated=$atualizados")
            } catch (e: Exception) {
                android.util.Log.w("CONTRACT_PULL", "ALIGN_REP_SIGNATURE failed: ${e.message}")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar assinaturas representante legal: ${e.message}", e)
        }
    }

    /**
     * PULL: Baixar Logs Auditoria Assinatura do Firestore
     */
    private suspend fun pullLogsAuditoriaAssinaturaFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL Logs Auditoria Assinatura do Firestore...")
        
        try {
            val logsCollection = firestore.collection("empresas/$empresaId/logsAuditoriaAssinatura")
            val snapshot = logsCollection.get().await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} logs no Firestore")
            
            val logDao = database.logAuditoriaAssinaturaDao()
            val logsExistentesList = logDao.obterTodosLogs()
            
            var logsSincronizados = 0
            var logsExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["id"] as? Number)?.toLong() ?: continue
                    
                    android.util.Log.d("SyncManagerV2", "🔄 Processando log: ${data["tipoOperacao"]} - ${data["usuarioExecutou"]} (Room ID: $roomId)")
                    
                    // Verificar se já existe
                    val jaExiste = logsExistentesList.any { log -> log.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "✅ Log já existe: ${data["tipoOperacao"]} (ID: $roomId)")
                        logsExistentes++
                        continue
                    }
                    
                    // Criar entidade LogAuditoriaAssinatura
                    val log = com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura(
                        id = roomId,
                        tipoOperacao = data["tipoOperacao"] as? String ?: "",
                        idAssinatura = (data["idAssinatura"] as? Number)?.toLong() ?: 0L,
                        idContrato = (data["idContrato"] as? Number)?.toLong(),
                        idAditivo = (data["idAditivo"] as? Number)?.toLong(),
                        usuarioExecutou = data["usuarioExecutou"] as? String ?: "",
                        cpfUsuario = data["cpfUsuario"] as? String ?: "",
                        cargoUsuario = data["cargoUsuario"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        deviceId = data["deviceId"] as? String ?: "",
                        versaoApp = data["versaoApp"] as? String ?: "",
                        hashDocumento = data["hashDocumento"] as? String ?: "",
                        hashAssinatura = data["hashAssinatura"] as? String ?: "",
                        latitude = (data["latitude"] as? Number)?.toDouble(),
                        longitude = (data["longitude"] as? Number)?.toDouble(),
                        endereco = data["endereco"] as? String,
                        ipAddress = data["ipAddress"] as? String,
                        userAgent = data["userAgent"] as? String,
                        tipoDocumento = data["tipoDocumento"] as? String ?: "",
                        numeroDocumento = data["numeroDocumento"] as? String ?: "",
                        valorContrato = (data["valorContrato"] as? Number)?.toDouble(),
                        sucesso = (data["sucesso"] as? Boolean) ?: true,
                        mensagemErro = data["mensagemErro"] as? String,
                        dataOperacao = java.util.Date((data["dataOperacao"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                        observacoes = data["observacoes"] as? String,
                        validadoJuridicamente = (data["validadoJuridicamente"] as? Boolean) ?: false,
                        dataValidacao = (data["dataValidacao"] as? Number)?.let { java.util.Date(it.toLong()) },
                        validadoPor = data["validadoPor"] as? String
                    )
                    
                    // Inserir no banco local
                    logDao.inserirLog(log)
                    logsSincronizados++
                    
                    android.util.Log.d("SyncManagerV2", "✅ Log sincronizado: ${log.tipoOperacao} (ID: $roomId)")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar log ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL Logs Auditoria Assinatura:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $logsSincronizados")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $logsExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar logs auditoria assinatura: ${e.message}", e)
        }
    }

    /**
     * Sincronizar AcertoMesa do Firestore
     */
    private suspend fun pullAcertoMesaFromFirestore(empresaId: String) {
        try {
            android.util.Log.d("SyncManagerV2", "🔄 Sincronizando AcertoMesa do Firestore...")
            
            val collectionName = getCollectionName("acertoMesa")
            val snapshot = firestore.collection("empresas")
                .document(empresaId)
                .collection(collectionName)
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} AcertoMesa no Firestore")
            
            val acertoMesaDao = database.acertoMesaDao()
            val acertoMesasExistentes = acertoMesaDao.buscarPorAcertoId(0L) // Busca vazia para obter lista
            
            for (document in snapshot) {
                try {
                    val data = document.data
                    val roomId = document.id.toLongOrNull() ?: continue
                    
                    // Verificar se já existe
                    val jaExiste = acertoMesasExistentes.any { acertoMesa -> acertoMesa.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "⏭️ AcertoMesa $roomId já existe, pulando...")
                        continue
                    }
                    
                    // ✅ NOVO: Download de foto do Firebase Storage se for URL
                    val fotoUrlFirebase = data["fotoRelogioFinal"] as? String
                    val fotoRelogioLocal = if (!fotoUrlFirebase.isNullOrBlank()) {
                        try {
                            val caminhoLocal = com.example.gestaobilhares.utils.FirebaseStorageManager.downloadFoto(
                                context = context,
                                urlFirebase = fotoUrlFirebase,
                                tipoFoto = "relogio_final"
                            )
                            if (caminhoLocal != null) {
                                android.util.Log.d("SyncManagerV2", "✅ Foto de relógio final baixada: $caminhoLocal")
                            } else {
                                android.util.Log.w("SyncManagerV2", "⚠️ Falha ao baixar foto de relógio final: $fotoUrlFirebase")
                            }
                            caminhoLocal
                        } catch (e: Exception) {
                            android.util.Log.e("SyncManagerV2", "Erro ao baixar foto de relógio final: ${e.message}")
                            fotoUrlFirebase // Fallback: manter URL se download falhar
                        }
                    } else null
                    
                    val dataFotoTimestamp = (data["dataFoto"] as? Number)?.toLong()
                    
                    val acertoMesa = AcertoMesa(
                        id = roomId,
                        acertoId = (data["acertoId"] as? Number)?.toLong() ?: 0L,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: 0L,
                        relogioInicial = (data["relogioInicial"] as? Number)?.toInt() ?: 0,
                        relogioFinal = (data["relogioFinal"] as? Number)?.toInt() ?: 0,
                        fichasJogadas = (data["fichasJogadas"] as? Number)?.toInt() ?: 0,
                        valorFicha = (data["valorFicha"] as? Number)?.toDouble() ?: 0.0,
                        comissaoFicha = (data["comissaoFicha"] as? Number)?.toDouble() ?: 0.0,
                        subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                        fotoRelogioFinal = fotoRelogioLocal,
                        dataFoto = dataFotoTimestamp?.let { Date(it) }
                    )
                    
                    acertoMesaDao.inserir(acertoMesa)
                    android.util.Log.d("SyncManagerV2", "✅ AcertoMesa $roomId inserido")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar AcertoMesa ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "✅ Sincronização de AcertoMesa concluída")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao sincronizar AcertoMesa: ${e.message}")
        }
    }

    /**
     * Sincronizar MesaReformada do Firestore
     */
    private suspend fun pullMesaReformadaFromFirestore(empresaId: String) {
        try {
            android.util.Log.d("SyncManagerV2", "🔄 Sincronizando MesaReformada do Firestore...")
            
            val collectionName = getCollectionName("mesaReformada")
            val snapshot = firestore.collection("empresas")
                .document(empresaId)
                .collection(collectionName)
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} MesaReformada no Firestore")
            
            val mesaReformadaDao = database.mesaReformadaDao()
            val mesasReformadasExistentes = runBlocking { mesaReformadaDao.listarTodas().first() }
            
            for (document in snapshot) {
                try {
                    val data = document.data
                    val roomId = document.id.toLongOrNull() ?: continue
                    
                    // Verificar se já existe
                    val jaExiste = mesasReformadasExistentes.any { mesaReformada -> mesaReformada.id == roomId }
                    if (jaExiste) {
                        android.util.Log.d("SyncManagerV2", "⏭️ MesaReformada $roomId já existe, pulando...")
                        continue
                    }
                    
                    // ✅ NOVO: Download de foto do Firebase Storage se for URL
                    val fotoUrlFirebase = data["fotoReforma"] as? String
                    val fotoReformaLocal = if (!fotoUrlFirebase.isNullOrBlank()) {
                        try {
                            val caminhoLocal = com.example.gestaobilhares.utils.FirebaseStorageManager.downloadFoto(
                                context = context,
                                urlFirebase = fotoUrlFirebase,
                                tipoFoto = "foto_reforma"
                            )
                            if (caminhoLocal != null) {
                                android.util.Log.d("SyncManagerV2", "✅ Foto de reforma baixada: $caminhoLocal")
                            } else {
                                android.util.Log.w("SyncManagerV2", "⚠️ Falha ao baixar foto de reforma: $fotoUrlFirebase")
                            }
                            caminhoLocal
                        } catch (e: Exception) {
                            android.util.Log.e("SyncManagerV2", "Erro ao baixar foto de reforma: ${e.message}")
                            fotoUrlFirebase // Fallback: manter URL se download falhar
                        }
                    } else null
                    
                    val mesaReformada = MesaReformada(
                        id = roomId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: "",
                        tipoMesa = TipoMesa.valueOf(data["tipoMesa"] as? String ?: "SINUCA"),
                        tamanhoMesa = TamanhoMesa.valueOf(data["tamanhoMesa"] as? String ?: "MEDIA"),
                        pintura = (data["pintura"] as? Boolean) ?: false,
                        tabela = (data["tabela"] as? Boolean) ?: false,
                        panos = (data["panos"] as? Boolean) ?: false,
                        numeroPanos = data["numeroPanos"] as? String,
                        outros = (data["outros"] as? Boolean) ?: false,
                        observacoes = data["observacoes"] as? String,
                        fotoReforma = fotoReformaLocal,
                        dataReforma = Date((data["dataReforma"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    )
                    
                    mesaReformadaDao.inserir(mesaReformada)
                    android.util.Log.d("SyncManagerV2", "✅ MesaReformada $roomId inserido")
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar MesaReformada ${document.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("SyncManagerV2", "✅ Sincronização de MesaReformada concluída")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao sincronizar MesaReformada: ${e.message}")
        }
    }

    /**
     * PULL: Baixar PanoMesa do Firestore
     */
    private suspend fun pullPanoMesaFromFirestore(empresaId: String) {
        android.util.Log.d("SyncManagerV2", "🔄 Iniciando PULL PanoMesa do Firestore...")
        
        try {
            val collectionName = getCollectionName("panomesa")
            val snapshot = firestore.collection("empresas")
                .document(empresaId)
                .collection(collectionName)
                .get()
                .await()
            
            android.util.Log.d("SyncManagerV2", "📥 Encontrados ${snapshot.size()} PanoMesa no Firestore")
            
            val panoMesaDao = database.panoMesaDao()
            
            var panoMesasSincronizadas = 0
            var panoMesasExistentes = 0
            
            // ✅ ESTRATÉGIA: Buscar todas as mesas e verificar PanoMesa existentes através delas
            val todasMesas = database.mesaDao().obterTodasMesas().first()
            val panoMesasExistentesIds = mutableSetOf<Long>()
            for (mesa in todasMesas) {
                try {
                    val panoAtual = panoMesaDao.buscarPanoAtualMesa(mesa.id)
                    panoAtual?.let { panoMesasExistentesIds.add(it.id) }
                    // Buscar histórico também
                    val historico = panoMesaDao.buscarHistoricoTrocasMesa(mesa.id).first()
                    historico.forEach { panoMesasExistentesIds.add(it.id) }
                } catch (_: Exception) {}
            }
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    // ✅ Ler roomId do campo "id" do documento (padrão dos outros PULLs)
                    val roomId = ((data["id"] as? Number)?.toLong()
                        ?: (data["roomId"] as? Number)?.toLong()
                        ?: document.id.toLongOrNull()) ?: continue
                    
                    val mesaId = ((data["mesaId"] as? Number)?.toLong() ?: continue).toLong()
                    val panoId = ((data["panoId"] as? Number)?.toLong() ?: continue).toLong()
                    
                    android.util.Log.d("SyncManagerV2", "🔍 Processando PanoMesa: MesaID=$mesaId PanoID=$panoId (Room ID: $roomId)")
                    
                    // Verificar se já existe pelo ID
                    val jaExiste = panoMesasExistentesIds.contains(roomId)
                    
                    if (!jaExiste) {
                        // Criar entidade PanoMesa
                        val panoMesa = com.example.gestaobilhares.data.entities.PanoMesa(
                            id = roomId,
                            mesaId = mesaId,
                            panoId = panoId,
                            dataTroca = java.util.Date((data["dataTroca"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                            ativo = (data["ativo"] as? Boolean) ?: true,
                            observacoes = data["observacoes"] as? String,
                            dataCriacao = java.util.Date((data["dataCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis())
                        )
                        
                        // Inserir no Room (OnConflictStrategy.REPLACE já está configurado no DAO)
                        panoMesaDao.inserir(panoMesa)
                        panoMesasSincronizadas++
                        panoMesasExistentesIds.add(roomId) // Adicionar ao set para próximas iterações
                        android.util.Log.d("SyncManagerV2", "✅ PanoMesa sincronizado: MesaID=$mesaId PanoID=$panoId (ID: $roomId)")
                    } else {
                        panoMesasExistentes++
                        android.util.Log.d("SyncManagerV2", "⏭️ PanoMesa já existe: MesaID=$mesaId PanoID=$panoId (ID: $roomId)")
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "❌ Erro ao processar PanoMesa ${document.id}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("SyncManagerV2", "📊 Resumo PULL PanoMesa:")
            android.util.Log.d("SyncManagerV2", "   Sincronizados: $panoMesasSincronizadas")
            android.util.Log.d("SyncManagerV2", "   Já existentes: $panoMesasExistentes")
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "❌ Erro ao baixar PanoMesa: ${e.message}", e)
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
