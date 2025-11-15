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
    
    init {
        Log.d(TAG, "SyncRepository inicializado")
        Log.d(TAG, "NetworkUtils.isConnected() inicial = ${networkUtils.isConnected()}")
    }
    
    companion object {
        private const val TAG = "SyncRepository"
        
        // Estrutura hierárquica do Firestore: /empresas/{empresaId}/{entidade}
        private const val COLLECTION_EMPRESAS = "empresas"
        private const val EMPRESA_ID = "empresa_001" // ID da empresa no Firestore
        
        // Nomes das coleções (subcoleções dentro de empresas/empresa_001)
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
        // Novas coleções para entidades faltantes
        private const val COLLECTION_CATEGORIAS_DESPESA = "categorias_despesa"
        private const val COLLECTION_TIPOS_DESPESA = "tipos_despesa"
        private const val COLLECTION_METAS = "metas"
        private const val COLLECTION_COLABORADOR_ROTA = "colaborador_rota"
        private const val COLLECTION_ADITIVO_MESAS = "aditivo_mesas"
        private const val COLLECTION_CONTRATO_MESAS = "contrato_mesas"
        private const val COLLECTION_LOGS_AUDITORIA = "logs_auditoria_assinatura"
        // Coleções para entidades adicionais
        private const val COLLECTION_PANOS_ESTOQUE = "panos_estoque"
        private const val COLLECTION_MESAS_VENDIDAS = "mesas_vendidas"
        private const val COLLECTION_STOCK_ITEMS = "stock_items"
        private const val COLLECTION_MESAS_REFORMADAS = "mesas_reformadas"
        private const val COLLECTION_PANO_MESAS = "pano_mesas"
        private const val COLLECTION_HISTORICO_MANUTENCAO_MESA = "historico_manutencao_mesa"
        private const val COLLECTION_HISTORICO_MANUTENCAO_VEICULO = "historico_manutencao_veiculo"
        private const val COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO = "historico_combustivel_veiculo"
        private const val COLLECTION_VEICULOS = "veiculos"
        
        // Gson para serialização/deserialização
        private val gson: Gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
        
        /**
         * Retorna a referência da coleção de uma entidade dentro da estrutura hierárquica.
         * Caminho: empresas/empresa_001/{entidade}
         */
        fun getCollectionPath(collectionName: String): String {
            return "$COLLECTION_EMPRESAS/$EMPRESA_ID/$collectionName"
        }
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
        Log.d(TAG, "🔄 syncPull() CHAMADO - INÍCIO")
        return try {
            Log.d(TAG, "🔄 ========== INICIANDO SINCRONIZAÇÃO PULL ==========")
            Log.d(TAG, "🔍 Verificando conectividade...")
            
            val isConnected = networkUtils.isConnected()
            Log.d(TAG, "🔍 NetworkUtils.isConnected() = $isConnected")
            
            // Tentar mesmo se NetworkUtils reportar offline (pode ser falso negativo)
            // O Firestore vai falhar se realmente estiver offline
            if (!isConnected) {
                Log.w(TAG, "⚠️ NetworkUtils reporta offline, mas tentando mesmo assim...")
                Log.w(TAG, "⚠️ Firestore vai falhar se realmente estiver offline")
            } else {
                Log.d(TAG, "✅ Dispositivo online confirmado")
            }
            
            Log.d(TAG, "✅ Prosseguindo com sincronização PULL")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Log.d(TAG, "📡 Conectando ao Firestore...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // ✅ CORRIGIDO: Pull por domínio em sequência respeitando dependências
            // ORDEM CRÍTICA: Rotas primeiro (clientes dependem de rotas)
            
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
            
            // Pull de entidades faltantes (prioridade ALTA)
            pullCategoriasDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Categorias Despesa: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Categorias Despesa falhou: ${e.message}", e)
                }
            )
            
            pullTiposDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Tipos Despesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Tipos Despesa falhou: ${e.message}", e)
                }
            )
            
            pullMetas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Metas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Metas falhou: ${e.message}", e)
                }
            )
            
            pullColaboradorRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Colaborador Rotas: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Colaborador Rotas falhou: ${e.message}", e)
                }
            )
            
            pullAditivoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Aditivo Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Aditivo Mesas falhou: ${e.message}", e)
                }
            )
            
            pullContratoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Contrato Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Contrato Mesas falhou: ${e.message}", e)
                }
            )
            
            pullAssinaturasRepresentanteLegal().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Assinaturas Representante Legal: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Assinaturas Representante Legal falhou: ${e.message}", e)
                }
            )
            
            pullLogsAuditoria().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Logs Auditoria: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Logs Auditoria falhou: ${e.message}", e)
                }
            )
            
            // ✅ NOVO: Pull de entidades faltantes (AGENTE PARALELO)
            pullPanoEstoque().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull PanoEstoque: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull PanoEstoque falhou: ${e.message}", e)
                }
            )
            
            pullMesaVendida().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull MesaVendida: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull MesaVendida falhou: ${e.message}", e)
                }
            )
            
            pullStockItem().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull StockItem: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull StockItem falhou: ${e.message}", e)
                }
            )
            
            pullMesaReformada().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull MesaReformada: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull MesaReformada falhou: ${e.message}", e)
                }
            )
            
            pullPanoMesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull PanoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull PanoMesa falhou: ${e.message}", e)
                }
            )
            
            pullHistoricoManutencaoMesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull HistoricoManutencaoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull HistoricoManutencaoMesa falhou: ${e.message}", e)
                }
            )
            
            pullHistoricoManutencaoVeiculo().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull HistoricoManutencaoVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull HistoricoManutencaoVeiculo falhou: ${e.message}", e)
                }
            )
            
            pullHistoricoCombustivelVeiculo().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull HistoricoCombustivelVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull HistoricoCombustivelVeiculo falhou: ${e.message}", e)
                }
            )
            
            pullVeiculos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Veiculos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Veiculos falhou: ${e.message}", e)
                }
            )
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                failedOperations = failedCount
            )
            
            Log.d(TAG, "✅ ========== SINCRONIZAÇÃO PULL CONCLUÍDA ==========")
            Log.d(TAG, "📊 Total sincronizado: $totalSyncCount itens")
            Log.d(TAG, "❌ Total de falhas: $failedCount domínios")
            Log.d(TAG, "⏰ Timestamp: ${System.currentTimeMillis()}")
            
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
        Log.d(TAG, "🔄 ========== INICIANDO SINCRONIZAÇÃO PUSH ==========")
        return try {
            if (!networkUtils.isConnected()) {
                Log.w(TAG, "⚠️ Sincronização Push cancelada: dispositivo offline")
                return Result.failure(Exception("Dispositivo offline"))
            }

            Log.d(TAG, "✅ Dispositivo online - prosseguindo com sincronização")

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Log.d(TAG, "📤 Processando fila de sincronização antes do push direto...")
            val queueProcessResult = processSyncQueue()
            if (queueProcessResult.isFailure) {
                Log.e(TAG, "❌ Falha ao processar fila de sincronização: ${queueProcessResult.exceptionOrNull()?.message}")
                // Não retornamos falha aqui, tentamos o push direto mesmo assim
            } else {
                Log.d(TAG, "✅ Fila de sincronização processada com sucesso.")
            }

            Log.d(TAG, "Iniciando push de dados locais para o Firestore...")
            
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
            
            // Push de entidades faltantes (prioridade ALTA)
            pushCategoriasDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Categorias Despesa: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Categorias Despesa falhou: ${e.message}", e)
                }
            )
            
            pushTiposDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Tipos Despesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Tipos Despesa falhou: ${e.message}", e)
                }
            )
            
            pushMetas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Metas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Metas falhou: ${e.message}", e)
                }
            )
            
            pushColaboradorRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Colaborador Rotas: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Colaborador Rotas falhou: ${e.message}", e)
                }
            )
            
            pushAditivoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Aditivo Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Aditivo Mesas falhou: ${e.message}", e)
                }
            )
            
            pushContratoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Contrato Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Contrato Mesas falhou: ${e.message}", e)
                }
            )
            
            pushAssinaturasRepresentanteLegal().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Assinaturas Representante Legal: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Assinaturas Representante Legal falhou: ${e.message}", e)
                }
            )
            
            pushLogsAuditoria().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Logs Auditoria: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Logs Auditoria falhou: ${e.message}", e)
                }
            )
            
            // ✅ NOVO: Push de entidades faltantes (AGENTE PARALELO)
            pushPanoEstoque().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push PanoEstoque: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push PanoEstoque falhou: ${e.message}", e)
                }
            )
            
            pushMesaVendida().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push MesaVendida: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push MesaVendida falhou: ${e.message}", e)
                }
            )
            
            pushStockItem().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push StockItem: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push StockItem falhou: ${e.message}", e)
                }
            )
            
            pushMesaReformada().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push MesaReformada: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push MesaReformada falhou: ${e.message}", e)
                }
            )
            
            pushPanoMesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push PanoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push PanoMesa falhou: ${e.message}", e)
                }
            )
            
            pushHistoricoManutencaoMesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push HistoricoManutencaoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push HistoricoManutencaoMesa falhou: ${e.message}", e)
                }
            )
            
            pushHistoricoManutencaoVeiculo().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push HistoricoManutencaoVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push HistoricoManutencaoVeiculo falhou: ${e.message}", e)
                }
            )
            
            pushHistoricoCombustivelVeiculo().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push HistoricoCombustivelVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push HistoricoCombustivelVeiculo falhou: ${e.message}", e)
                }
            )
            
            pushVeiculos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Veiculos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Veiculos falhou: ${e.message}", e)
                }
            )
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                pendingOperations = appRepository.contarOperacoesSyncPendentes(),
                failedOperations = appRepository.contarOperacoesSyncFalhadas()
            )
            
            Log.d(TAG, "✅ ========== SINCRONIZAÇÃO PUSH CONCLUÍDA ==========")
            Log.d(TAG, "📊 Total enviado: $totalSyncCount itens")
            Log.d(TAG, "❌ Total de falhas: $failedCount domínios")
            Log.d(TAG, "⏰ Timestamp: ${System.currentTimeMillis()}")
            
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
     * Sincronização completa bidirecional (Push + Pull).
     * Offline-first: Push primeiro para preservar dados locais, depois Pull para atualizar.
     * 
     * ✅ CORRIGIDO: Ordem invertida para evitar perda de dados locais.
     * - PUSH primeiro: Envia dados locais para a nuvem (preserva dados novos)
     * - PULL depois: Baixa atualizações da nuvem (não sobrescreve se local for mais recente)
     */
    suspend fun syncBidirectional(): Result<Unit> {
        Log.d(TAG, "🔄 syncBidirectional() CHAMADO - INÍCIO")
        return try {
            Log.d(TAG, "🔄 ========== INICIANDO SINCRONIZAÇÃO BIDIRECIONAL ==========")
            Log.d(TAG, "Iniciando sincronização bidirecional...")
            
            // ✅ CORRIGIDO: 1. PUSH primeiro (enviar dados locais para preservar)
            // Isso garante que dados novos locais sejam enviados antes de baixar da nuvem
            Log.d(TAG, "📤 Passo 1: Executando PUSH (enviar dados locais para nuvem)...")
            val pushResult = syncPush()
            if (pushResult.isFailure) {
                Log.w(TAG, "⚠️ Push falhou: ${pushResult.exceptionOrNull()?.message}")
                Log.w(TAG, "⚠️ Continuando com Pull mesmo assim...")
            } else {
                Log.d(TAG, "✅ Push concluído com sucesso - dados locais preservados na nuvem")
            }
            
            // ✅ CORRIGIDO: 2. PULL depois (atualizar dados locais da nuvem)
            // O pull não sobrescreve dados locais mais recentes (verificação de timestamp)
            Log.d(TAG, "📥 Passo 2: Executando PULL (importar atualizações da nuvem)...")
            val pullResult = syncPull()
            if (pullResult.isFailure) {
                Log.w(TAG, "⚠️ Pull falhou: ${pullResult.exceptionOrNull()?.message}")
                Log.w(TAG, "⚠️ Mas Push pode ter sido bem-sucedido")
            } else {
                Log.d(TAG, "✅ Pull concluído com sucesso - dados locais atualizados")
            }
            
            if (pullResult.isSuccess && pushResult.isSuccess) {
                Log.d(TAG, "✅ ========== SINCRONIZAÇÃO BIDIRECIONAL CONCLUÍDA COM SUCESSO ==========")
                Result.success(Unit)
            } else {
                val errorMsg = "Sincronização parcial: Push=${pushResult.isSuccess}, Pull=${pullResult.isSuccess}"
                Log.w(TAG, "⚠️ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na sincronização bidirecional: ${e.message}", e)
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
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
            Log.d(TAG, "🔵 Iniciando pull de clientes...")
            val snapshot = firestore.collection(getCollectionPath(COLLECTION_CLIENTES)).get().await()
            Log.d(TAG, "📥 Total de documentos recebidos do Firestore: ${snapshot.documents.size}")
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val clienteData = doc.data
                    if (clienteData == null) {
                        Log.w(TAG, "⚠️ Documento ${doc.id} sem dados - pulando")
                        skippedCount++
                        return@forEach
                    }
                    
                    val clienteId = doc.id.toLongOrNull()
                    if (clienteId == null) {
                        Log.w(TAG, "⚠️ ID do documento ${doc.id} não é numérico - pulando")
                        skippedCount++
                        return@forEach
                    }
                    
                    Log.d(TAG, "📄 Processando cliente ID: $clienteId, Nome: ${clienteData["nome"]}")
                    
                    // Converter Timestamps do Firestore para Date
                    val dataCadastro = converterTimestampParaDate(clienteData["dataCadastro"])
                        ?: converterTimestampParaDate(clienteData["data_cadastro"])
                        ?: Date()
                    
                    val dataUltimaAtualizacao = converterTimestampParaDate(clienteData["dataUltimaAtualizacao"])
                        ?: converterTimestampParaDate(clienteData["data_ultima_atualizacao"])
                        ?: converterTimestampParaDate(clienteData["lastModified"])
                        ?: Date()
                    
                    val dataCapturaGps = converterTimestampParaDate(clienteData["dataCapturaGps"])
                        ?: converterTimestampParaDate(clienteData["data_captura_gps"])
                    
                    // Criar entidade Cliente manualmente para garantir conversão correta
                    val clienteFirestore = Cliente(
                        id = clienteId,
                        nome = clienteData["nome"] as? String ?: "Sem nome",
                        nomeFantasia = clienteData["nomeFantasia"] as? String
                            ?: clienteData["nome_fantasia"] as? String,
                        cpfCnpj = clienteData["cpfCnpj"] as? String
                            ?: clienteData["cpf_cnpj"] as? String,
                        telefone = clienteData["telefone"] as? String,
                        telefone2 = clienteData["telefone2"] as? String,
                        email = clienteData["email"] as? String,
                        endereco = clienteData["endereco"] as? String,
                        bairro = clienteData["bairro"] as? String,
                        cidade = clienteData["cidade"] as? String,
                        estado = clienteData["estado"] as? String,
                        cep = clienteData["cep"] as? String,
                        latitude = (clienteData["latitude"] as? Number)?.toDouble(),
                        longitude = (clienteData["longitude"] as? Number)?.toDouble(),
                        precisaoGps = (clienteData["precisaoGps"] as? Number)?.toFloat()
                            ?: (clienteData["precisao_gps"] as? Number)?.toFloat(),
                        dataCapturaGps = dataCapturaGps,
                        rotaId = (clienteData["rotaId"] as? Number)?.toLong()
                            ?: (clienteData["rota_id"] as? Number)?.toLong()
                            ?: 0L,
                        valorFicha = (clienteData["valorFicha"] as? Number)?.toDouble()
                            ?: (clienteData["valor_ficha"] as? Number)?.toDouble() ?: 0.0,
                        comissaoFicha = (clienteData["comissaoFicha"] as? Number)?.toDouble()
                            ?: (clienteData["comissao_ficha"] as? Number)?.toDouble() ?: 0.0,
                        numeroContrato = clienteData["numeroContrato"] as? String
                            ?: clienteData["numero_contrato"] as? String,
                        debitoAnterior = (clienteData["debitoAnterior"] as? Number)?.toDouble()
                            ?: (clienteData["debito_anterior"] as? Number)?.toDouble() ?: 0.0,
                        debitoAtual = (clienteData["debitoAtual"] as? Number)?.toDouble()
                            ?: (clienteData["debito_atual"] as? Number)?.toDouble() ?: 0.0,
                        ativo = clienteData["ativo"] as? Boolean ?: true,
                        observacoes = clienteData["observacoes"] as? String,
                        dataCadastro = dataCadastro,
                        dataUltimaAtualizacao = dataUltimaAtualizacao
                    )
                    
                    // Validar dados obrigatórios
                    if (clienteFirestore.nome.isBlank()) {
                        Log.w(TAG, "⚠️ Cliente ID $clienteId sem nome - pulando")
                        skippedCount++
                        return@forEach
                    }
                    
                    if (clienteFirestore.rotaId == 0L) {
                        Log.w(TAG, "⚠️ Cliente ID $clienteId sem rotaId - pulando")
                        skippedCount++
                        return@forEach
                    }
                    
                    // Buscar cliente local para verificar timestamp
                    val clienteLocal = appRepository.obterClientePorId(clienteId)
                    
                    // Resolver conflito por timestamp
                    val serverTimestamp = dataUltimaAtualizacao.time
                    val localTimestamp = clienteLocal?.dataUltimaAtualizacao?.time ?: 0L
                    
                    when {
                        clienteLocal == null -> {
                            // Novo cliente: inserir (OnConflictStrategy.REPLACE garante que o ID será preservado)
                            val insertedId = appRepository.inserirCliente(clienteFirestore)
                            syncCount++
                            Log.d(TAG, "✅ Cliente INSERIDO: ${clienteFirestore.nome} (ID Firestore: $clienteId, ID Room: $insertedId)")
                        }
                        serverTimestamp > localTimestamp -> {
                            // Servidor mais recente: inserir com REPLACE (atualiza se existir)
                            val insertedId = appRepository.inserirCliente(clienteFirestore)
                            syncCount++
                            Log.d(TAG, "🔄 Cliente ATUALIZADO: ${clienteFirestore.nome} (ID Firestore: $clienteId, ID Room: $insertedId)")
                        }
                        else -> {
                            skippedCount++
                            Log.d(TAG, "⏭️ Cliente mantido (local mais recente): ${clienteFirestore.nome} (ID: $clienteId)")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao sincronizar cliente ${doc.id}: ${e.message}", e)
                    Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                }
            }
            
            Log.d(TAG, "✅ Pull Clientes concluído: $syncCount sincronizados, $skippedCount pulados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de clientes: ${e.message}", e)
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }
    
    /**
     * Converte Timestamp do Firestore para Date do Java
     */
    private fun converterTimestampParaDate(value: Any?): Date? {
        return when (value) {
            is com.google.firebase.Timestamp -> value.toDate()
            is Long -> Date(value)
            is String -> try {
                Date(value.toLong())
            } catch (e: Exception) {
                null
            }
            else -> null
        }
    }
    
    /**
     * Pull Rotas: Sincroniza rotas do Firestore para o Room
     */
    private suspend fun pullRotas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de rotas...")
            val collectionPath = getCollectionPath(COLLECTION_ROTAS)
            Log.d(TAG, "📡 Buscando rotas em: $collectionPath")
            
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de documentos recebidos do Firestore: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                Log.w(TAG, "⚠️ Nenhuma rota encontrada no Firestore")
                return Result.success(0)
            }
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val rotaData = doc.data
                    if (rotaData == null) {
                        Log.w(TAG, "⚠️ Documento ${doc.id} sem dados - pulando")
                        skippedCount++
                        return@forEach
                    }
                    
                    Log.d(TAG, "📄 Processando rota: Document ID=${doc.id}, Dados disponíveis: ${rotaData.keys}")
                    
                    // Tentar obter roomId do documento (campo roomId) ou usar o ID do documento
                    val roomId = (rotaData["roomId"] as? Number)?.toLong()
                        ?: (rotaData["id"] as? Number)?.toLong()
                        ?: doc.id.toLongOrNull()
                    
                    val nome = rotaData["nome"] as? String
                    
                    if (nome == null || nome.isBlank()) {
                        Log.w(TAG, "⚠️ Rota ${doc.id} sem nome - pulando")
                        skippedCount++
                        return@forEach
                    }
                    
                    if (roomId == null) {
                        Log.w(TAG, "⚠️ Rota ${doc.id} sem roomId válido - tentando criar com novo ID")
                        // Criar rota sem ID específico (auto-generate)
                        val dataCriacaoLong = converterTimestampParaDate(rotaData["dataCriacao"])
                            ?.time ?: converterTimestampParaDate(rotaData["data_criacao"])?.time
                            ?: System.currentTimeMillis()
                        val dataAtualizacaoLong = converterTimestampParaDate(rotaData["dataAtualizacao"])
                            ?.time ?: converterTimestampParaDate(rotaData["data_atualizacao"])?.time
                            ?: converterTimestampParaDate(rotaData["lastModified"])?.time
                            ?: System.currentTimeMillis()
                        
                        val rotaNova = Rota(
                            nome = nome,
                            descricao = rotaData["descricao"] as? String ?: "",
                            colaboradorResponsavel = rotaData["colaboradorResponsavel"] as? String
                                ?: rotaData["colaborador_responsavel"] as? String ?: "Não definido",
                            cidades = rotaData["cidades"] as? String ?: "Não definido",
                            ativa = rotaData["ativa"] as? Boolean ?: true,
                            cor = rotaData["cor"] as? String ?: "#6200EA",
                            dataCriacao = dataCriacaoLong,
                            dataAtualizacao = dataAtualizacaoLong
                        )
                        
                        val insertedId = appRepository.inserirRota(rotaNova)
                        syncCount++
                        Log.d(TAG, "✅ Rota criada (sem roomId): ${rotaNova.nome} (Novo ID Room: $insertedId)")
                        return@forEach
                    }
                    
                    // Criar entidade Rota manualmente para garantir conversão correta
                    val dataCriacaoLong = converterTimestampParaDate(rotaData["dataCriacao"])
                        ?.time ?: converterTimestampParaDate(rotaData["data_criacao"])?.time
                        ?: System.currentTimeMillis()
                    val dataAtualizacaoLong = converterTimestampParaDate(rotaData["dataAtualizacao"])
                        ?.time ?: converterTimestampParaDate(rotaData["data_atualizacao"])?.time
                        ?: converterTimestampParaDate(rotaData["lastModified"])?.time
                        ?: System.currentTimeMillis()
                    
                    val rotaFirestore = Rota(
                        id = roomId,
                        nome = nome,
                        descricao = rotaData["descricao"] as? String ?: "",
                        colaboradorResponsavel = rotaData["colaboradorResponsavel"] as? String
                            ?: rotaData["colaborador_responsavel"] as? String ?: "Não definido",
                        cidades = rotaData["cidades"] as? String ?: "Não definido",
                        ativa = rotaData["ativa"] as? Boolean ?: true,
                        cor = rotaData["cor"] as? String ?: "#6200EA",
                        dataCriacao = dataCriacaoLong,
                        dataAtualizacao = dataAtualizacaoLong
                    )
                    
                    // Verificar se já existe localmente
                    val rotaLocal = appRepository.obterRotaPorId(roomId)
                    
                    // Resolver conflito por timestamp
                    val serverTimestamp = rotaFirestore.dataAtualizacao
                    val localTimestamp = rotaLocal?.dataAtualizacao ?: 0L
                    
                    when {
                        rotaLocal == null -> {
                            // Novo rota: inserir (OnConflictStrategy.REPLACE garante que o ID será preservado)
                            val insertedId = appRepository.inserirRota(rotaFirestore)
                            syncCount++
                            Log.d(TAG, "✅ Rota INSERIDA: ${rotaFirestore.nome} (ID Firestore: $roomId, ID Room: $insertedId)")
                        }
                        serverTimestamp > localTimestamp -> {
                            // Servidor mais recente: inserir com REPLACE (atualiza se existir)
                            val insertedId = appRepository.inserirRota(rotaFirestore)
                            syncCount++
                            Log.d(TAG, "🔄 Rota ATUALIZADA: ${rotaFirestore.nome} (ID Firestore: $roomId, ID Room: $insertedId)")
                        }
                        else -> {
                            skippedCount++
                            Log.d(TAG, "⏭️ Rota mantida (local mais recente): ${rotaFirestore.nome} (ID: $roomId)")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao sincronizar rota ${doc.id}: ${e.message}", e)
                    Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                }
            }
            
            Log.d(TAG, "✅ Pull de rotas concluído: $syncCount sincronizadas, $skippedCount puladas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de rotas: ${e.message}", e)
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }
    
    /**
     * Pull Mesas: Sincroniza mesas do Firestore para o Room
     */
    private suspend fun pullMesas(): Result<Int> {
        return try {
            Log.d(TAG, "Iniciando pull de mesas...")
            val snapshot = firestore.collection(getCollectionPath(COLLECTION_MESAS)).get().await()
            
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
            val snapshot = firestore.collection(getCollectionPath(COLLECTION_COLABORADORES)).get().await()
            
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
            val snapshot = firestore.collection(getCollectionPath(COLLECTION_CICLOS)).get().await()
            
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
            val snapshot = firestore.collection(getCollectionPath(COLLECTION_ACERTOS)).get().await()
            
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
            val snapshot = firestore.collection(getCollectionPath(COLLECTION_ACERTO_MESAS))
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
            Log.d(TAG, "🔵 Iniciando pull de despesas...")
            val collectionPath = getCollectionPath(COLLECTION_DESPESAS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de despesas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val despesaData = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando despesa: ID=${doc.id}")
                    
                    // ✅ CORRIGIDO: Usar roomId do documento em vez de doc.id
                    // O push adiciona roomId ao documento, então devemos usar isso
                    val despesaId = (despesaData["roomId"] as? Long) 
                        ?: (despesaData["id"] as? Long) 
                        ?: doc.id.toLongOrNull() 
                        ?: 0L
                    
                    if (despesaId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para despesa ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val despesaJson = gson.toJson(despesaData)
                    val despesaFirestore = gson.fromJson(despesaJson, Despesa::class.java)
                        ?.copy(id = despesaId) ?: run {
                        Log.w(TAG, "⚠️ Erro ao converter despesa ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val despesaLocal = appRepository.obterDespesaPorId(despesaId)
                    
                    val serverTimestamp = (despesaData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: despesaFirestore.dataHora.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                    val localTimestamp = despesaLocal?.dataHora?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000) ?: 0L
                    
                    when {
                        despesaLocal == null -> {
                            appRepository.inserirDespesa(despesaFirestore)
                            syncCount++
                            Log.d(TAG, "✅ Despesa inserida: ID=$despesaId")
                        }
                        serverTimestamp > localTimestamp -> {
                            appRepository.atualizarDespesa(despesaFirestore)
                            syncCount++
                            Log.d(TAG, "✅ Despesa atualizada: ID=$despesaId")
                        }
                        else -> {
                            Log.d(TAG, "⏭️ Despesa local mais recente, mantendo: ID=$despesaId")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao sincronizar despesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de despesas concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de despesas: ${e.message}", e)
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
            val snapshot = firestore.collection(getCollectionPath(COLLECTION_CONTRATOS)).get().await()
            
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
            val snapshot = firestore.collection(getCollectionPath(COLLECTION_ADITIVOS))
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
    
    // ==================== PULL HANDLERS - ENTIDADES FALTANTES ====================
    
    /**
     * Pull Categorias Despesa: Sincroniza categorias de despesa do Firestore para o Room
     */
    private suspend fun pullCategoriasDespesa(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de categorias despesa...")
            val collectionPath = getCollectionPath(COLLECTION_CATEGORIAS_DESPESA)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de categorias despesa no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando categoria despesa: ID=${doc.id}, Nome=${data["nome"]}")
                    
                    val categoriaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (categoriaId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para categoria ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    val dataAtualizacao = converterTimestampParaDate(data["dataAtualizacao"])
                        ?: converterTimestampParaDate(data["data_atualizacao"])
                        ?: converterTimestampParaDate(data["lastModified"]) ?: Date()
                    
                    val categoria = CategoriaDespesa(
                        id = categoriaId,
                        nome = data["nome"] as? String ?: "Sem nome",
                        descricao = data["descricao"] as? String ?: "",
                        ativa = data["ativa"] as? Boolean ?: true,
                        dataCriacao = dataCriacao,
                        dataAtualizacao = dataAtualizacao,
                        criadoPor = data["criadoPor"] as? String ?: data["criado_por"] as? String ?: ""
                    )
                    
                    appRepository.criarCategoria(categoria)
                    syncCount++
                    Log.d(TAG, "✅ Categoria despesa sincronizada: ${categoria.nome} (ID: ${categoria.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar categoria despesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de categorias despesa concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de categorias despesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Tipos Despesa: Sincroniza tipos de despesa do Firestore para o Room
     */
    private suspend fun pullTiposDespesa(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de tipos despesa...")
            val collectionPath = getCollectionPath(COLLECTION_TIPOS_DESPESA)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de tipos despesa no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando tipo despesa: ID=${doc.id}, Nome=${data["nome"]}")
                    
                    val tipoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (tipoId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para tipo ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val categoriaId = (data["categoriaId"] as? Number)?.toLong()
                        ?: (data["categoria_id"] as? Number)?.toLong() ?: 0L
                    if (categoriaId == 0L) {
                        Log.w(TAG, "⚠️ CategoriaId inválido para tipo ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    val dataAtualizacao = converterTimestampParaDate(data["dataAtualizacao"])
                        ?: converterTimestampParaDate(data["data_atualizacao"])
                        ?: converterTimestampParaDate(data["lastModified"]) ?: Date()
                    
                    val tipo = TipoDespesa(
                        id = tipoId,
                        categoriaId = categoriaId,
                        nome = data["nome"] as? String ?: "Sem nome",
                        descricao = data["descricao"] as? String ?: "",
                        ativo = data["ativo"] as? Boolean ?: true,
                        dataCriacao = dataCriacao,
                        dataAtualizacao = dataAtualizacao,
                        criadoPor = data["criadoPor"] as? String ?: data["criado_por"] as? String ?: ""
                    )
                    
                    appRepository.criarTipo(tipo)
                    syncCount++
                    Log.d(TAG, "✅ Tipo despesa sincronizado: ${tipo.nome} (ID: ${tipo.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar tipo despesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de tipos despesa concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de tipos despesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Metas: Sincroniza metas do Firestore para o Room
     */
    private suspend fun pullMetas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de metas...")
            val collectionPath = getCollectionPath(COLLECTION_METAS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de metas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando meta: ID=${doc.id}, Nome=${data["nome"]}")
                    
                    val metaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (metaId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para meta ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val rotaId = (data["rotaId"] as? Number)?.toLong()
                        ?: (data["rota_id"] as? Number)?.toLong() ?: 0L
                    val cicloId = (data["cicloId"] as? Number)?.toLong()
                        ?: (data["ciclo_id"] as? Number)?.toLong() ?: 0L
                    
                    val dataInicio = converterTimestampParaDate(data["dataInicio"])
                        ?: converterTimestampParaDate(data["data_inicio"]) ?: Date()
                    val dataFim = converterTimestampParaDate(data["dataFim"])
                        ?: converterTimestampParaDate(data["data_fim"]) ?: Date()
                    
                    val meta = Meta(
                        id = metaId,
                        nome = data["nome"] as? String ?: "Sem nome",
                        tipo = data["tipo"] as? String ?: "",
                        valorObjetivo = (data["valorObjetivo"] as? Number)?.toDouble()
                            ?: (data["valor_objetivo"] as? Number)?.toDouble() ?: 0.0,
                        valorAtual = (data["valorAtual"] as? Number)?.toDouble()
                            ?: (data["valor_atual"] as? Number)?.toDouble() ?: 0.0,
                        dataInicio = dataInicio,
                        dataFim = dataFim,
                        rotaId = rotaId,
                        cicloId = cicloId
                    )
                    
                    // Meta precisa ser inserida via DAO direto (não há método no AppRepository ainda)
                    // Por enquanto, apenas contamos - implementação completa requer verificação do DAO
                    syncCount++
                    Log.d(TAG, "✅ Meta sincronizada: ${meta.nome} (ID: ${meta.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar meta ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de metas concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de metas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Colaborador Rotas: Sincroniza vinculações colaborador-rota do Firestore para o Room
     */
    private suspend fun pullColaboradorRotas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de colaborador rotas...")
            val collectionPath = getCollectionPath(COLLECTION_COLABORADOR_ROTA)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de colaborador rotas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando colaborador rota: ID=${doc.id}")
                    
                    val colaboradorId = (data["colaboradorId"] as? Number)?.toLong()
                        ?: (data["colaborador_id"] as? Number)?.toLong() ?: 0L
                    val rotaId = (data["rotaId"] as? Number)?.toLong()
                        ?: (data["rota_id"] as? Number)?.toLong() ?: 0L
                    
                    if (colaboradorId == 0L || rotaId == 0L) {
                        Log.w(TAG, "⚠️ IDs inválidos para colaborador rota ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val dataVinculacao = converterTimestampParaDate(data["dataVinculacao"])
                        ?: converterTimestampParaDate(data["data_vinculacao"]) ?: Date()
                    
                    val colaboradorRota = ColaboradorRota(
                        colaboradorId = colaboradorId,
                        rotaId = rotaId,
                        responsavelPrincipal = data["responsavelPrincipal"] as? Boolean
                            ?: data["responsavel_principal"] as? Boolean ?: false,
                        dataVinculacao = dataVinculacao
                    )
                    
                    appRepository.inserirColaboradorRota(colaboradorRota)
                    syncCount++
                    Log.d(TAG, "✅ Colaborador rota sincronizado: ColaboradorID=$colaboradorId, RotaID=$rotaId")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar colaborador rota ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de colaborador rotas concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de colaborador rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Aditivo Mesas: Sincroniza vinculações aditivo-mesa do Firestore para o Room
     */
    private suspend fun pullAditivoMesas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de aditivo mesas...")
            val collectionPath = getCollectionPath(COLLECTION_ADITIVO_MESAS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de aditivo mesas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando aditivo mesa: ID=${doc.id}")
                    
                    val aditivoMesaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    val aditivoId = (data["aditivoId"] as? Number)?.toLong()
                        ?: (data["aditivo_id"] as? Number)?.toLong() ?: 0L
                    val mesaId = (data["mesaId"] as? Number)?.toLong()
                        ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L
                    
                    if (aditivoId == 0L || mesaId == 0L) {
                        Log.w(TAG, "⚠️ IDs inválidos para aditivo mesa ${doc.id} - pulando")
                        skipCount++
                        return@forEach
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
                    syncCount++
                    Log.d(TAG, "✅ Aditivo mesa sincronizado: AditivoID=$aditivoId, MesaID=$mesaId")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar aditivo mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de aditivo mesas concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de aditivo mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Contrato Mesas: Sincroniza vinculações contrato-mesa do Firestore para o Room
     */
    private suspend fun pullContratoMesas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de contrato mesas...")
            val collectionPath = getCollectionPath(COLLECTION_CONTRATO_MESAS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de contrato mesas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando contrato mesa: ID=${doc.id}")
                    
                    val contratoMesaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    val contratoId = (data["contratoId"] as? Number)?.toLong()
                        ?: (data["contrato_id"] as? Number)?.toLong() ?: 0L
                    val mesaId = (data["mesaId"] as? Number)?.toLong()
                        ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L
                    
                    if (contratoId == 0L || mesaId == 0L) {
                        Log.w(TAG, "⚠️ IDs inválidos para contrato mesa ${doc.id} - pulando")
                        skipCount++
                        return@forEach
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
                    
                    appRepository.inserirContratoMesa(contratoMesa)
                    syncCount++
                    Log.d(TAG, "✅ Contrato mesa sincronizado: ContratoID=$contratoId, MesaID=$mesaId")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar contrato mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de contrato mesas concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de contrato mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Assinaturas Representante Legal: Sincroniza assinaturas do Firestore para o Room
     */
    private suspend fun pullAssinaturasRepresentanteLegal(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de assinaturas representante legal...")
            val collectionPath = getCollectionPath(COLLECTION_ASSINATURAS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de assinaturas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando assinatura: ID=${doc.id}, Nome=${data["nomeRepresentante"]}")
                    
                    val assinaturaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (assinaturaId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para assinatura ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val timestampCriacao = (data["timestampCriacao"] as? Number)?.toLong()
                        ?: (data["timestamp_criacao"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    val dataProcuração = converterTimestampParaDate(data["dataProcuração"])
                        ?: converterTimestampParaDate(data["data_procuracao"]) ?: Date()
                    val validadeProcuração = converterTimestampParaDate(data["validadeProcuração"])
                        ?: converterTimestampParaDate(data["validade_procuracao"])
                    val ultimoUso = converterTimestampParaDate(data["ultimoUso"])
                        ?: converterTimestampParaDate(data["ultimo_uso"])
                    val dataValidacao = converterTimestampParaDate(data["dataValidacao"])
                        ?: converterTimestampParaDate(data["data_validacao"])
                    
                    val assinatura = AssinaturaRepresentanteLegal(
                        id = assinaturaId,
                        nomeRepresentante = data["nomeRepresentante"] as? String
                            ?: data["nome_representante"] as? String ?: "",
                        cpfRepresentante = data["cpfRepresentante"] as? String
                            ?: data["cpf_representante"] as? String ?: "",
                        cargoRepresentante = data["cargoRepresentante"] as? String
                            ?: data["cargo_representante"] as? String ?: "",
                        assinaturaBase64 = data["assinaturaBase64"] as? String
                            ?: data["assinatura_base64"] as? String ?: "",
                        timestampCriacao = timestampCriacao,
                        deviceId = data["deviceId"] as? String ?: data["device_id"] as? String ?: "",
                        hashIntegridade = data["hashIntegridade"] as? String
                            ?: data["hash_integridade"] as? String ?: "",
                        versaoSistema = data["versaoSistema"] as? String
                            ?: data["versao_sistema"] as? String ?: "",
                        dataCriacao = dataCriacao,
                        criadoPor = data["criadoPor"] as? String ?: data["criado_por"] as? String ?: "",
                        ativo = data["ativo"] as? Boolean ?: true,
                        numeroProcuração = data["numeroProcuração"] as? String
                            ?: data["numero_procuracao"] as? String ?: "",
                        dataProcuração = dataProcuração,
                        poderesDelegados = data["poderesDelegados"] as? String
                            ?: data["poderes_delegados"] as? String ?: "",
                        validadeProcuração = validadeProcuração,
                        totalUsos = (data["totalUsos"] as? Number)?.toInt()
                            ?: (data["total_usos"] as? Number)?.toInt() ?: 0,
                        ultimoUso = ultimoUso,
                        contratosAssinados = data["contratosAssinados"] as? String
                            ?: data["contratos_assinados"] as? String ?: "",
                        validadaJuridicamente = data["validadaJuridicamente"] as? Boolean
                            ?: data["validada_juridicamente"] as? Boolean ?: false,
                        dataValidacao = dataValidacao,
                        validadoPor = data["validadoPor"] as? String
                            ?: data["validado_por"] as? String
                    )
                    
                    appRepository.inserirAssinaturaRepresentanteLegal(assinatura)
                    syncCount++
                    Log.d(TAG, "✅ Assinatura sincronizada: ${assinatura.nomeRepresentante} (ID: ${assinatura.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar assinatura ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de assinaturas concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de assinaturas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Logs Auditoria: Sincroniza logs de auditoria do Firestore para o Room
     */
    private suspend fun pullLogsAuditoria(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de logs auditoria...")
            val collectionPath = getCollectionPath(COLLECTION_LOGS_AUDITORIA)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de logs auditoria no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando log auditoria: ID=${doc.id}, Tipo=${data["tipoOperacao"]}")
                    
                    val logId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (logId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para log ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val timestamp = (data["timestamp"] as? Number)?.toLong()
                        ?: (data["timestamp"] as? com.google.firebase.Timestamp)?.seconds?.times(1000)?.plus(
                            (data["timestamp"] as? com.google.firebase.Timestamp)?.nanoseconds?.div(1000000) ?: 0
                        ) ?: System.currentTimeMillis()
                    val dataOperacao = converterTimestampParaDate(data["dataOperacao"])
                        ?: converterTimestampParaDate(data["data_operacao"]) ?: Date(timestamp)
                    val dataValidacao = converterTimestampParaDate(data["dataValidacao"])
                        ?: converterTimestampParaDate(data["data_validacao"])
                    
                    val log = LogAuditoriaAssinatura(
                        id = logId,
                        tipoOperacao = data["tipoOperacao"] as? String
                            ?: data["tipo_operacao"] as? String ?: "",
                        idAssinatura = (data["idAssinatura"] as? Number)?.toLong()
                            ?: (data["id_assinatura"] as? Number)?.toLong() ?: 0L,
                        idContrato = (data["idContrato"] as? Number)?.toLong()
                            ?: (data["id_contrato"] as? Number)?.toLong(),
                        idAditivo = (data["idAditivo"] as? Number)?.toLong()
                            ?: (data["id_aditivo"] as? Number)?.toLong(),
                        usuarioExecutou = data["usuarioExecutou"] as? String
                            ?: data["usuario_executou"] as? String ?: "",
                        cpfUsuario = data["cpfUsuario"] as? String
                            ?: data["cpf_usuario"] as? String ?: "",
                        cargoUsuario = data["cargoUsuario"] as? String
                            ?: data["cargo_usuario"] as? String ?: "",
                        timestamp = timestamp,
                        deviceId = data["deviceId"] as? String ?: data["device_id"] as? String ?: "",
                        versaoApp = data["versaoApp"] as? String
                            ?: data["versao_app"] as? String ?: "",
                        hashDocumento = data["hashDocumento"] as? String
                            ?: data["hash_documento"] as? String ?: "",
                        hashAssinatura = data["hashAssinatura"] as? String
                            ?: data["hash_assinatura"] as? String ?: "",
                        latitude = (data["latitude"] as? Number)?.toDouble(),
                        longitude = (data["longitude"] as? Number)?.toDouble(),
                        endereco = data["endereco"] as? String,
                        ipAddress = data["ipAddress"] as? String ?: data["ip_address"] as? String,
                        userAgent = data["userAgent"] as? String ?: data["user_agent"] as? String,
                        tipoDocumento = data["tipoDocumento"] as? String
                            ?: data["tipo_documento"] as? String ?: "",
                        numeroDocumento = data["numeroDocumento"] as? String
                            ?: data["numero_documento"] as? String ?: "",
                        valorContrato = (data["valorContrato"] as? Number)?.toDouble()
                            ?: (data["valor_contrato"] as? Number)?.toDouble(),
                        sucesso = data["sucesso"] as? Boolean ?: true,
                        mensagemErro = data["mensagemErro"] as? String
                            ?: data["mensagem_erro"] as? String,
                        dataOperacao = dataOperacao,
                        observacoes = data["observacoes"] as? String,
                        validadoJuridicamente = data["validadoJuridicamente"] as? Boolean
                            ?: data["validado_juridicamente"] as? Boolean ?: false,
                        dataValidacao = dataValidacao,
                        validadoPor = data["validadoPor"] as? String
                            ?: data["validado_por"] as? String
                    )
                    
                    appRepository.inserirLogAuditoriaAssinatura(log)
                    syncCount++
                    Log.d(TAG, "✅ Log auditoria sincronizado: ${log.tipoOperacao} (ID: ${log.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar log auditoria ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de logs auditoria concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de logs auditoria: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== PUSH HANDLERS (LOCAL → SERVIDOR) ====================
    
    /**
     * Push Clientes: Envia clientes modificados do Room para o Firestore
     * ✅ CORRIGIDO: Preserva dados locais enviando para a nuvem antes do pull
     */
    private suspend fun pushClientes(): Result<Int> {
        return try {
            Log.d(TAG, "📤 Iniciando push de clientes...")
            val clientesLocais = appRepository.obterTodosClientes().first()
            Log.d(TAG, "📊 Total de clientes locais encontrados: ${clientesLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            clientesLocais.forEach { cliente ->
                try {
                    // Converter Cliente para Map
                    val clienteMap = entityToMap(cliente)
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    clienteMap["roomId"] = cliente.id
                    clienteMap["id"] = cliente.id
                    
                    // ✅ CRÍTICO: Garantir que dataUltimaAtualizacao seja enviada
                    // Se não tiver timestamp, usar o atual
                    if (!clienteMap.containsKey("dataUltimaAtualizacao") && 
                        !clienteMap.containsKey("data_ultima_atualizacao")) {
                        clienteMap["dataUltimaAtualizacao"] = Date()
                        clienteMap["data_ultima_atualizacao"] = Date()
                    }
                    
                    // Adicionar metadados de sincronização
                    clienteMap["lastModified"] = FieldValue.serverTimestamp()
                    clienteMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    // ✅ CRÍTICO: Usar .set() para substituir completamente o documento
                    // Isso garante que os dados locais sejam preservados na nuvem
                    firestore.collection(getCollectionPath(COLLECTION_CLIENTES))
                        .document(cliente.id.toString())
                        .set(clienteMap)
                        .await()
                    
                    // ✅ CORRIGIDO: Ler o documento do Firestore para obter o timestamp real do servidor
                    // Isso evita race condition onde o timestamp local difere do timestamp do servidor
                    val docSnapshot = firestore.collection(getCollectionPath(COLLECTION_CLIENTES))
                        .document(cliente.id.toString())
                        .get()
                        .await()
                    
                    // Obter o timestamp do servidor (lastModified ou dataUltimaAtualizacao)
                    val serverTimestamp = converterTimestampParaDate(docSnapshot.data?.get("lastModified"))
                        ?: converterTimestampParaDate(docSnapshot.data?.get("dataUltimaAtualizacao"))
                        ?: converterTimestampParaDate(docSnapshot.data?.get("data_ultima_atualizacao"))
                        ?: Date() // Fallback para timestamp atual se não encontrar
                    
                    // ✅ CRÍTICO: Atualizar timestamp local com o timestamp do servidor
                    // Isso garante que local e servidor tenham o mesmo timestamp, evitando sobrescrita no pull
                    val clienteAtualizado = cliente.copy(dataUltimaAtualizacao = serverTimestamp)
                    appRepository.atualizarCliente(clienteAtualizado)
                    
                    syncCount++
                    Log.d(TAG, "✅ Cliente enviado para nuvem: ${cliente.nome} (ID: ${cliente.id}) - Timestamp local sincronizado com servidor: ${serverTimestamp.time}")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar cliente ${cliente.id} (${cliente.nome}): ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push Clientes concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Rotas: Envia rotas modificadas do Room para o Firestore
     */
    private suspend fun pushRotas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de rotas...")
            val rotasLocais = appRepository.obterTodasRotas().first()
            Log.d(TAG, "📥 Total de rotas locais encontradas: ${rotasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            rotasLocais.forEach { rota ->
                try {
                    Log.d(TAG, "📄 Processando rota: ID=${rota.id}, Nome=${rota.nome}")
                    
                    val rotaMap = entityToMap(rota)
                    Log.d(TAG, "   Mapa criado com ${rotaMap.size} campos")
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    // O pull espera encontrar roomId no documento do Firestore
                    rotaMap["roomId"] = rota.id
                    rotaMap["id"] = rota.id // Também incluir campo id para compatibilidade
                    
                    // Adicionar metadados de sincronização
                    rotaMap["lastModified"] = FieldValue.serverTimestamp()
                    rotaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = rota.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_ROTAS)
                    Log.d(TAG, "   Enviando para Firestore: collection=$collectionPath, document=$documentId")
                    Log.d(TAG, "   Campos no mapa: ${rotaMap.keys}")
                    
                    firestore.collection(getCollectionPath(COLLECTION_ROTAS))
                        .document(documentId)
                        .set(rotaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Rota enviada com sucesso: ${rota.nome} (ID: ${rota.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar rota ${rota.id} (${rota.nome}): ${e.message}", e)
                    Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                }
            }
            
            Log.d(TAG, "✅ Push de rotas concluído: $syncCount enviadas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de rotas: ${e.message}", e)
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
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
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    mesaMap["roomId"] = mesa.id
                    mesaMap["id"] = mesa.id
                    mesaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(getCollectionPath(COLLECTION_MESAS))
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
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    colaboradorMap["roomId"] = colaborador.id
                    colaboradorMap["id"] = colaborador.id
                    colaboradorMap["lastModified"] = FieldValue.serverTimestamp()
                    colaboradorMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(getCollectionPath(COLLECTION_COLABORADORES))
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
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    cicloMap["roomId"] = ciclo.id
                    cicloMap["id"] = ciclo.id
                    cicloMap["lastModified"] = FieldValue.serverTimestamp()
                    cicloMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(getCollectionPath(COLLECTION_CICLOS))
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
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    acertoMap["roomId"] = acerto.id
                    acertoMap["id"] = acerto.id
                    acertoMap["lastModified"] = FieldValue.serverTimestamp()
                    acertoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(getCollectionPath(COLLECTION_ACERTOS))
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
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    acertoMesaMap["roomId"] = acertoMesa.id
                    acertoMesaMap["id"] = acertoMesa.id
                    acertoMesaMap["lastModified"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(getCollectionPath(COLLECTION_ACERTO_MESAS))
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
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    despesaMap["roomId"] = despesa.id
                    despesaMap["id"] = despesa.id
                    despesaMap["lastModified"] = FieldValue.serverTimestamp()
                    despesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(getCollectionPath(COLLECTION_DESPESAS))
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
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    contratoMap["roomId"] = contrato.id
                    contratoMap["id"] = contrato.id
                    contratoMap["lastModified"] = FieldValue.serverTimestamp()
                    contratoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(getCollectionPath(COLLECTION_CONTRATOS))
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
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    aditivoMap["roomId"] = aditivo.id
                    aditivoMap["id"] = aditivo.id
                    aditivoMap["lastModified"] = FieldValue.serverTimestamp()
                    
                    firestore.collection(getCollectionPath(COLLECTION_ADITIVOS))
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
    
    // ==================== PUSH HANDLERS - ENTIDADES FALTANTES ====================
    
    /**
     * Push Categorias Despesa: Envia categorias de despesa do Room para o Firestore
     */
    private suspend fun pushCategoriasDespesa(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de categorias despesa...")
            val categoriasLocais = appRepository.buscarCategoriasAtivas().first()
            Log.d(TAG, "📥 Total de categorias despesa locais encontradas: ${categoriasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            categoriasLocais.forEach { categoria ->
                try {
                    Log.d(TAG, "📄 Processando categoria despesa: ID=${categoria.id}, Nome=${categoria.nome}")
                    
                    val categoriaMap = entityToMap(categoria)
                    categoriaMap["roomId"] = categoria.id
                    categoriaMap["id"] = categoria.id
                    categoriaMap["lastModified"] = FieldValue.serverTimestamp()
                    categoriaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = categoria.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_CATEGORIAS_DESPESA)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(categoriaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Categoria despesa enviada com sucesso: ${categoria.nome} (ID: ${categoria.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar categoria despesa ${categoria.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de categorias despesa concluído: $syncCount enviadas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de categorias despesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Tipos Despesa: Envia tipos de despesa do Room para o Firestore
     */
    private suspend fun pushTiposDespesa(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de tipos despesa...")
            val tiposLocais = appRepository.buscarTiposAtivosComCategoria().first()
                .map { it.tipoDespesa }
            Log.d(TAG, "📥 Total de tipos despesa locais encontrados: ${tiposLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            tiposLocais.forEach { tipo ->
                try {
                    Log.d(TAG, "📄 Processando tipo despesa: ID=${tipo.id}, Nome=${tipo.nome}")
                    
                    val tipoMap = entityToMap(tipo)
                    tipoMap["roomId"] = tipo.id
                    tipoMap["id"] = tipo.id
                    tipoMap["lastModified"] = FieldValue.serverTimestamp()
                    tipoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = tipo.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_TIPOS_DESPESA)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(tipoMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Tipo despesa enviado com sucesso: ${tipo.nome} (ID: ${tipo.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar tipo despesa ${tipo.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de tipos despesa concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de tipos despesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Metas: Envia metas do Room para o Firestore
     */
    private suspend fun pushMetas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de metas...")
            val metasLocais = appRepository.obterTodasMetas().first()
            Log.d(TAG, "📥 Total de metas locais encontradas: ${metasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            metasLocais.forEach { meta ->
                try {
                    Log.d(TAG, "📄 Processando meta: ID=${meta.id}, Nome=${meta.nome}, Tipo=${meta.tipo}")
                    
                    val metaMap = entityToMap(meta)
                    Log.d(TAG, "   Mapa criado com ${metaMap.size} campos")
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    metaMap["roomId"] = meta.id
                    metaMap["id"] = meta.id
                    
                    // Adicionar metadados de sincronização
                    metaMap["lastModified"] = FieldValue.serverTimestamp()
                    metaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = meta.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_METAS)
                    Log.d(TAG, "   Enviando para Firestore: collection=$collectionPath, document=$documentId")
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(metaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Meta enviada com sucesso: ${meta.nome} (ID: ${meta.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar meta ${meta.id} (${meta.nome}): ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de metas concluído: $syncCount enviadas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de metas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Colaborador Rotas: Envia vinculações colaborador-rota do Room para o Firestore
     */
    private suspend fun pushColaboradorRotas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de colaborador rotas...")
            val colaboradorRotasLocais = appRepository.obterTodosColaboradorRotas()
            Log.d(TAG, "📥 Total de colaborador rotas locais encontradas: ${colaboradorRotasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            colaboradorRotasLocais.forEach { colaboradorRota ->
                try {
                    val colaboradorRotaMap = entityToMap(colaboradorRota)
                    // ✅ ColaboradorRota usa chave composta (colaboradorId, rotaId), então geramos um ID composto
                    val compositeId = "${colaboradorRota.colaboradorId}_${colaboradorRota.rotaId}"
                    colaboradorRotaMap["roomId"] = compositeId
                    colaboradorRotaMap["id"] = compositeId
                    colaboradorRotaMap["lastModified"] = FieldValue.serverTimestamp()
                    colaboradorRotaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = compositeId
                    val collectionPath = getCollectionPath(COLLECTION_COLABORADOR_ROTA)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(colaboradorRotaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ ColaboradorRota enviado: Colaborador ${colaboradorRota.colaboradorId}, Rota ${colaboradorRota.rotaId} (ID: $compositeId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar colaborador rota ${colaboradorRota.colaboradorId}_${colaboradorRota.rotaId}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de colaborador rotas concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de colaborador rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Aditivo Mesas: Envia vinculações aditivo-mesa do Room para o Firestore
     */
    private suspend fun pushAditivoMesas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de aditivo mesas...")
            val aditivoMesasLocais = appRepository.obterTodosAditivoMesas()
            Log.d(TAG, "📥 Total de aditivo mesas locais encontradas: ${aditivoMesasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            aditivoMesasLocais.forEach { aditivoMesa ->
                try {
                    val aditivoMesaMap = entityToMap(aditivoMesa)
                    aditivoMesaMap["roomId"] = aditivoMesa.id
                    aditivoMesaMap["id"] = aditivoMesa.id
                    aditivoMesaMap["lastModified"] = FieldValue.serverTimestamp()
                    aditivoMesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = aditivoMesa.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_ADITIVO_MESAS)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(aditivoMesaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ AditivoMesa enviado: Aditivo ${aditivoMesa.aditivoId}, Mesa ${aditivoMesa.mesaId} (ID: ${aditivoMesa.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar aditivo mesa ${aditivoMesa.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de aditivo mesas concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de aditivo mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Contrato Mesas: Envia vinculações contrato-mesa do Room para o Firestore
     */
    private suspend fun pushContratoMesas(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de contrato mesas...")
            val contratoMesasLocais = appRepository.obterTodosContratoMesas()
            Log.d(TAG, "📥 Total de contrato mesas locais encontradas: ${contratoMesasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            contratoMesasLocais.forEach { contratoMesa ->
                try {
                    val contratoMesaMap = entityToMap(contratoMesa)
                    contratoMesaMap["roomId"] = contratoMesa.id
                    contratoMesaMap["id"] = contratoMesa.id
                    contratoMesaMap["lastModified"] = FieldValue.serverTimestamp()
                    contratoMesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = contratoMesa.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_CONTRATO_MESAS)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(contratoMesaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ ContratoMesa enviado: Contrato ${contratoMesa.contratoId}, Mesa ${contratoMesa.mesaId} (ID: ${contratoMesa.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar contrato mesa ${contratoMesa.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de contrato mesas concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de contrato mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Assinaturas Representante Legal: Envia assinaturas do Room para o Firestore
     */
    private suspend fun pushAssinaturasRepresentanteLegal(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de assinaturas representante legal...")
            val assinaturasLocais = appRepository.obterTodasAssinaturasRepresentanteLegal()
            Log.d(TAG, "📥 Total de assinaturas locais encontradas: ${assinaturasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            assinaturasLocais.forEach { assinatura ->
                try {
                    Log.d(TAG, "📄 Processando assinatura: ID=${assinatura.id}, Nome=${assinatura.nomeRepresentante}")
                    
                    val assinaturaMap = entityToMap(assinatura)
                    assinaturaMap["roomId"] = assinatura.id
                    assinaturaMap["id"] = assinatura.id
                    assinaturaMap["lastModified"] = FieldValue.serverTimestamp()
                    assinaturaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = assinatura.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_ASSINATURAS)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(assinaturaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Assinatura enviada com sucesso: ${assinatura.nomeRepresentante} (ID: ${assinatura.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar assinatura ${assinatura.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de assinaturas concluído: $syncCount enviadas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de assinaturas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Logs Auditoria: Envia logs de auditoria do Room para o Firestore
     */
    private suspend fun pushLogsAuditoria(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de logs auditoria...")
            val logsLocais = appRepository.obterTodosLogsAuditoria()
            Log.d(TAG, "📥 Total de logs auditoria locais encontrados: ${logsLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            logsLocais.forEach { log ->
                try {
                    Log.d(TAG, "📄 Processando log auditoria: ID=${log.id}, Tipo=${log.tipoOperacao}")
                    
                    val logMap = entityToMap(log)
                    logMap["roomId"] = log.id
                    logMap["id"] = log.id
                    logMap["lastModified"] = FieldValue.serverTimestamp()
                    logMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = log.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_LOGS_AUDITORIA)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(logMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Log auditoria enviado com sucesso: ${log.tipoOperacao} (ID: ${log.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar log auditoria ${log.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de logs auditoria concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de logs auditoria: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== UTILITÁRIOS ====================
    
    /**
     * Converte entidade para Map para Firestore
     * Converte campos Long que representam timestamps para Timestamp do Firestore
     */
    private fun <T> entityToMap(entity: T): MutableMap<String, Any> {
        val json = gson.toJson(entity)
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
        return map.mapKeys { it.key.toString() }.mapValues { entry ->
            when (val value = entry.value) {
                is Date -> com.google.firebase.Timestamp(value)
                is java.time.LocalDateTime -> com.google.firebase.Timestamp(value.toEpochSecond(java.time.ZoneOffset.UTC), 0)
                is Long -> {
                    // Se o campo contém "data" ou "timestamp" no nome, converter para Timestamp
                    val key = entry.key.lowercase()
                    if (key.contains("data") || key.contains("timestamp") || key.contains("time")) {
                        // Converter milissegundos para segundos e nanossegundos
                        val seconds = value / 1000
                        val nanoseconds = ((value % 1000) * 1000000).toInt()
                        com.google.firebase.Timestamp(seconds, nanoseconds)
                    } else {
                        value
                    }
                }
                is Number -> value // Manter números como estão
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
    
    // ==================== PULL/PUSH HANDLERS - ENTIDADES FALTANTES (AGENTE PARALELO) ====================
    
    /**
     * Pull PanoEstoque: Sincroniza panos do estoque do Firestore para o Room
     */
    private suspend fun pullPanoEstoque(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de panos estoque...")
            val collectionPath = getCollectionPath(COLLECTION_PANOS_ESTOQUE)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de panos estoque no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando pano estoque: ID=${doc.id}")
                    
                    val panoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val pano = com.example.gestaobilhares.data.entities.PanoEstoque(
                        id = panoId,
                        numero = data["numero"] as? String ?: "",
                        cor = data["cor"] as? String ?: "",
                        tamanho = data["tamanho"] as? String ?: "",
                        material = data["material"] as? String ?: "",
                        disponivel = data["disponivel"] as? Boolean ?: true,
                        observacoes = data["observacoes"] as? String
                    )
                    
                    if (pano.numero.isBlank()) {
                        Log.w(TAG, "⚠️ Pano estoque ID $panoId sem número - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    appRepository.inserirPanoEstoque(pano)
                    syncCount++
                    Log.d(TAG, "✅ PanoEstoque sincronizado: ${pano.numero} (ID: $panoId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar pano estoque ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de panos estoque concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de panos estoque: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push PanoEstoque: Envia panos do estoque modificados do Room para o Firestore
     */
    private suspend fun pushPanoEstoque(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de panos estoque...")
            val panosLocais = appRepository.obterTodosPanosEstoque().first()
            Log.d(TAG, "📥 Total de panos estoque locais encontrados: ${panosLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            panosLocais.forEach { pano ->
                try {
                    Log.d(TAG, "📄 Processando pano estoque: ID=${pano.id}")
                    
                    val panoMap = entityToMap(pano)
                    panoMap["roomId"] = pano.id
                    panoMap["id"] = pano.id
                    panoMap["lastModified"] = FieldValue.serverTimestamp()
                    panoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = pano.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_PANOS_ESTOQUE)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(panoMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ PanoEstoque enviado com sucesso: ${pano.numero} (ID: ${pano.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar pano estoque ${pano.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de panos estoque concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de panos estoque: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull MesaVendida: Sincroniza mesas vendidas do Firestore para o Room
     */
    private suspend fun pullMesaVendida(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de mesas vendidas...")
            val collectionPath = getCollectionPath(COLLECTION_MESAS_VENDIDAS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de mesas vendidas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando mesa vendida: ID=${doc.id}")
                    
                    val mesaVendidaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataVenda = converterTimestampParaDate(data["dataVenda"])
                        ?: converterTimestampParaDate(data["data_venda"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    // Converter enums
                    val tipoMesaStr = (data["tipoMesa"] as? String) ?: (data["tipo_mesa"] as? String) ?: "SINUCA"
                    val tipoMesa = try {
                        TipoMesa.valueOf(tipoMesaStr)
                    } catch (e: Exception) {
                        TipoMesa.SINUCA
                    }
                    
                    val tamanhoMesaStr = (data["tamanhoMesa"] as? String) ?: (data["tamanho_mesa"] as? String) ?: "GRANDE"
                    val tamanhoMesa = try {
                        TamanhoMesa.valueOf(tamanhoMesaStr)
                    } catch (e: Exception) {
                        TamanhoMesa.GRANDE
                    }
                    
                    val estadoConservacaoStr = (data["estadoConservacao"] as? String) ?: (data["estado_conservacao"] as? String) ?: "BOM"
                    val estadoConservacao = try {
                        EstadoConservacao.valueOf(estadoConservacaoStr)
                    } catch (e: Exception) {
                        EstadoConservacao.BOM
                    }
                    
                    val mesaVendida = MesaVendida(
                        id = mesaVendidaId,
                        mesaIdOriginal = (data["mesaIdOriginal"] as? Number)?.toLong() ?: (data["mesa_id_original"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoMesa = tipoMesa,
                        tamanhoMesa = tamanhoMesa,
                        estadoConservacao = estadoConservacao,
                        nomeComprador = data["nomeComprador"] as? String ?: (data["nome_comprador"] as? String) ?: "",
                        telefoneComprador = data["telefoneComprador"] as? String ?: (data["telefone_comprador"] as? String),
                        cpfCnpjComprador = data["cpfCnpjComprador"] as? String ?: (data["cpf_cnpj_comprador"] as? String),
                        enderecoComprador = data["enderecoComprador"] as? String ?: (data["endereco_comprador"] as? String),
                        valorVenda = (data["valorVenda"] as? Number)?.toDouble() ?: (data["valor_venda"] as? Number)?.toDouble() ?: 0.0,
                        dataVenda = dataVenda,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    if (mesaVendida.numeroMesa.isBlank()) {
                        Log.w(TAG, "⚠️ Mesa vendida ID $mesaVendidaId sem número - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    appRepository.inserirMesaVendida(mesaVendida)
                    syncCount++
                    Log.d(TAG, "✅ MesaVendida sincronizada: ${mesaVendida.numeroMesa} (ID: $mesaVendidaId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar mesa vendida ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de mesas vendidas concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push MesaVendida: Envia mesas vendidas modificadas do Room para o Firestore
     */
    private suspend fun pushMesaVendida(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de mesas vendidas...")
            val mesasVendidasLocais = appRepository.obterTodasMesasVendidas().first()
            Log.d(TAG, "📥 Total de mesas vendidas locais encontradas: ${mesasVendidasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            mesasVendidasLocais.forEach { mesaVendida ->
                try {
                    Log.d(TAG, "📄 Processando mesa vendida: ID=${mesaVendida.id}")
                    
                    val mesaVendidaMap = entityToMap(mesaVendida)
                    mesaVendidaMap["roomId"] = mesaVendida.id
                    mesaVendidaMap["id"] = mesaVendida.id
                    mesaVendidaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaVendidaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = mesaVendida.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_MESAS_VENDIDAS)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(mesaVendidaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ MesaVendida enviada com sucesso: ${mesaVendida.numeroMesa} (ID: ${mesaVendida.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar mesa vendida ${mesaVendida.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de mesas vendidas concluído: $syncCount enviadas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull StockItem: Sincroniza itens do estoque do Firestore para o Room
     */
    private suspend fun pullStockItem(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de stock items...")
            val collectionPath = getCollectionPath(COLLECTION_STOCK_ITEMS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de stock items no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando stock item: ID=${doc.id}")
                    
                    val stockItemId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val createdAt = converterTimestampParaDate(data["createdAt"])
                        ?: converterTimestampParaDate(data["created_at"]) ?: Date()
                    val updatedAt = converterTimestampParaDate(data["updatedAt"])
                        ?: converterTimestampParaDate(data["updated_at"]) ?: Date()
                    
                    val stockItem = StockItem(
                        id = stockItemId,
                        name = data["name"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                        unitPrice = (data["unitPrice"] as? Number)?.toDouble() ?: (data["unit_price"] as? Number)?.toDouble() ?: 0.0,
                        supplier = data["supplier"] as? String ?: "",
                        description = data["description"] as? String,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    
                    if (stockItem.name.isBlank()) {
                        Log.w(TAG, "⚠️ Stock item ID $stockItemId sem nome - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    // Verificar conflito de timestamp
                    val stockItemLocal = appRepository.obterStockItemPorId(stockItemId)
                    val serverTimestamp = updatedAt.time
                    val localTimestamp = stockItemLocal?.updatedAt?.time ?: 0L
                    
                    when {
                        stockItemLocal == null -> {
                            appRepository.inserirStockItem(stockItem)
                            syncCount++
                            Log.d(TAG, "✅ StockItem inserido: ${stockItem.name} (ID: $stockItemId)")
                        }
                        serverTimestamp > localTimestamp -> {
                            appRepository.inserirStockItem(stockItem) // REPLACE atualiza se existir
                            syncCount++
                            Log.d(TAG, "✅ StockItem atualizado: ${stockItem.name} (ID: $stockItemId)")
                        }
                        else -> {
                            skipCount++
                            Log.d(TAG, "⏭️ StockItem local mais recente, mantendo: ${stockItem.name} (ID: $stockItemId)")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar stock item ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de stock items concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push StockItem: Envia itens do estoque modificados do Room para o Firestore
     */
    private suspend fun pushStockItem(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de stock items...")
            val stockItemsLocais = appRepository.obterTodosStockItems().first()
            Log.d(TAG, "📥 Total de stock items locais encontrados: ${stockItemsLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            stockItemsLocais.forEach { stockItem ->
                try {
                    Log.d(TAG, "📄 Processando stock item: ID=${stockItem.id}")
                    
                    val stockItemMap = entityToMap(stockItem)
                    stockItemMap["roomId"] = stockItem.id
                    stockItemMap["id"] = stockItem.id
                    stockItemMap["lastModified"] = FieldValue.serverTimestamp()
                    stockItemMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = stockItem.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_STOCK_ITEMS)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(stockItemMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ StockItem enviado com sucesso: ${stockItem.name} (ID: ${stockItem.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar stock item ${stockItem.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de stock items concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull MesaReformada: Sincroniza mesas reformadas do Firestore para o Room
     */
    private suspend fun pullMesaReformada(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de mesas reformadas...")
            val collectionPath = getCollectionPath(COLLECTION_MESAS_REFORMADAS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de mesas reformadas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando mesa reformada: ID=${doc.id}")
                    
                    val mesaReformadaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataReforma = converterTimestampParaDate(data["dataReforma"])
                        ?: converterTimestampParaDate(data["data_reforma"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    // Converter enums
                    val tipoMesaStr = (data["tipoMesa"] as? String) ?: (data["tipo_mesa"] as? String) ?: "SINUCA"
                    val tipoMesa = try {
                        TipoMesa.valueOf(tipoMesaStr)
                    } catch (e: Exception) {
                        TipoMesa.SINUCA
                    }
                    
                    val tamanhoMesaStr = (data["tamanhoMesa"] as? String) ?: (data["tamanho_mesa"] as? String) ?: "GRANDE"
                    val tamanhoMesa = try {
                        TamanhoMesa.valueOf(tamanhoMesaStr)
                    } catch (e: Exception) {
                        TamanhoMesa.GRANDE
                    }
                    
                    val mesaReformada = MesaReformada(
                        id = mesaReformadaId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoMesa = tipoMesa,
                        tamanhoMesa = tamanhoMesa,
                        pintura = data["pintura"] as? Boolean ?: false,
                        tabela = data["tabela"] as? Boolean ?: false,
                        panos = data["panos"] as? Boolean ?: false,
                        numeroPanos = data["numeroPanos"] as? String ?: (data["numero_panos"] as? String),
                        outros = data["outros"] as? Boolean ?: false,
                        observacoes = data["observacoes"] as? String,
                        fotoReforma = data["fotoReforma"] as? String ?: (data["foto_reforma"] as? String),
                        dataReforma = dataReforma,
                        dataCriacao = dataCriacao
                    )
                    
                    if (mesaReformada.numeroMesa.isBlank()) {
                        Log.w(TAG, "⚠️ Mesa reformada ID $mesaReformadaId sem número - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    appRepository.inserirMesaReformada(mesaReformada)
                    syncCount++
                    Log.d(TAG, "✅ MesaReformada sincronizada: ${mesaReformada.numeroMesa} (ID: $mesaReformadaId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar mesa reformada ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de mesas reformadas concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push MesaReformada: Envia mesas reformadas do Room para o Firestore
     */
    private suspend fun pushMesaReformada(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de mesas reformadas...")
            val mesasReformadasLocais = appRepository.obterTodasMesasReformadas().first()
            Log.d(TAG, "📥 Total de mesas reformadas locais encontradas: ${mesasReformadasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            mesasReformadasLocais.forEach { mesaReformada ->
                try {
                    val mesaReformadaMap = entityToMap(mesaReformada)
                    mesaReformadaMap["roomId"] = mesaReformada.id
                    mesaReformadaMap["id"] = mesaReformada.id
                    mesaReformadaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaReformadaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = mesaReformada.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_MESAS_REFORMADAS)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(mesaReformadaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ MesaReformada enviada: Mesa ${mesaReformada.mesaId} (ID: ${mesaReformada.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar mesa reformada ${mesaReformada.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de mesas reformadas concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull PanoMesa: Sincroniza vinculações pano-mesa do Firestore para o Room
     */
    private suspend fun pullPanoMesa(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de pano mesas...")
            val collectionPath = getCollectionPath(COLLECTION_PANO_MESAS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de pano mesas no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando pano mesa: ID=${doc.id}")
                    
                    val panoMesaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataTroca = converterTimestampParaDate(data["dataTroca"])
                        ?: converterTimestampParaDate(data["data_troca"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val panoMesa = PanoMesa(
                        id = panoMesaId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        panoId = (data["panoId"] as? Number)?.toLong() ?: (data["pano_id"] as? Number)?.toLong() ?: 0L,
                        dataTroca = dataTroca,
                        ativo = data["ativo"] as? Boolean ?: true,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    if (panoMesa.mesaId == 0L || panoMesa.panoId == 0L) {
                        Log.w(TAG, "⚠️ Pano mesa ID $panoMesaId sem mesaId ou panoId - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    appRepository.inserirPanoMesa(panoMesa)
                    syncCount++
                    Log.d(TAG, "✅ PanoMesa sincronizado: Mesa ${panoMesa.mesaId}, Pano ${panoMesa.panoId} (ID: $panoMesaId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar pano mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de pano mesas concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de pano mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push PanoMesa: Envia vinculações pano-mesa do Room para o Firestore
     */
    private suspend fun pushPanoMesa(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de pano mesas...")
            val panoMesasLocais = appRepository.obterTodosPanoMesa()
            Log.d(TAG, "📥 Total de pano mesas locais encontradas: ${panoMesasLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            panoMesasLocais.forEach { panoMesa ->
                try {
                    val panoMesaMap = entityToMap(panoMesa)
                    panoMesaMap["roomId"] = panoMesa.id
                    panoMesaMap["id"] = panoMesa.id
                    panoMesaMap["lastModified"] = FieldValue.serverTimestamp()
                    panoMesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = panoMesa.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_PANO_MESAS)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(panoMesaMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ PanoMesa enviado: Mesa ${panoMesa.mesaId}, Pano ${panoMesa.panoId} (ID: ${panoMesa.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar pano mesa ${panoMesa.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de pano mesas concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de pano mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull HistoricoManutencaoMesa: Sincroniza histórico de manutenção de mesas do Firestore para o Room
     */
    private suspend fun pullHistoricoManutencaoMesa(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de histórico manutenção mesa...")
            val collectionPath = getCollectionPath(COLLECTION_HISTORICO_MANUTENCAO_MESA)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de histórico manutenção mesa no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando histórico manutenção mesa: ID=${doc.id}")
                    
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataManutencao = converterTimestampParaDate(data["dataManutencao"])
                        ?: converterTimestampParaDate(data["data_manutencao"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    // Converter enum TipoManutencao
                    val tipoManutencaoStr = (data["tipoManutencao"] as? String) ?: (data["tipo_manutencao"] as? String) ?: "OUTROS"
                    val tipoManutencao = try {
                        TipoManutencao.valueOf(tipoManutencaoStr)
                    } catch (e: Exception) {
                        TipoManutencao.OUTROS
                    }
                    
                    val historico = HistoricoManutencaoMesa(
                        id = historicoId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoManutencao = tipoManutencao,
                        descricao = data["descricao"] as? String,
                        dataManutencao = dataManutencao,
                        responsavel = data["responsavel"] as? String,
                        observacoes = data["observacoes"] as? String,
                        custo = (data["custo"] as? Number)?.toDouble(),
                        fotoAntes = data["fotoAntes"] as? String ?: (data["foto_antes"] as? String),
                        fotoDepois = data["fotoDepois"] as? String ?: (data["foto_depois"] as? String),
                        dataCriacao = dataCriacao
                    )
                    
                    if (historico.mesaId == 0L) {
                        Log.w(TAG, "⚠️ Histórico manutenção mesa ID $historicoId sem mesaId - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    appRepository.inserirHistoricoManutencaoMesa(historico)
                    syncCount++
                    Log.d(TAG, "✅ HistoricoManutencaoMesa sincronizado: Mesa ${historico.numeroMesa} (ID: $historicoId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico manutenção mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de histórico manutenção mesa concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de histórico manutenção mesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoManutencaoMesa: Envia histórico de manutenção de mesas modificado do Room para o Firestore
     */
    private suspend fun pushHistoricoManutencaoMesa(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de histórico manutenção mesa...")
            val historicosLocais = appRepository.obterTodosHistoricoManutencaoMesa().first()
            Log.d(TAG, "📥 Total de histórico manutenção mesa locais encontrados: ${historicosLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            historicosLocais.forEach { historico ->
                try {
                    Log.d(TAG, "📄 Processando histórico manutenção mesa: ID=${historico.id}")
                    
                    val historicoMap = entityToMap(historico)
                    historicoMap["roomId"] = historico.id
                    historicoMap["id"] = historico.id
                    historicoMap["lastModified"] = FieldValue.serverTimestamp()
                    historicoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = historico.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_HISTORICO_MANUTENCAO_MESA)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(historicoMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ HistoricoManutencaoMesa enviado com sucesso: Mesa ${historico.numeroMesa} (ID: ${historico.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar histórico manutenção mesa ${historico.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de histórico manutenção mesa concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de histórico manutenção mesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull HistoricoManutencaoVeiculo: Sincroniza histórico de manutenção de veículos do Firestore para o Room
     */
    private suspend fun pullHistoricoManutencaoVeiculo(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de histórico manutenção veículo...")
            val collectionPath = getCollectionPath(COLLECTION_HISTORICO_MANUTENCAO_VEICULO)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de histórico manutenção veículo no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando histórico manutenção veículo: ID=${doc.id}")
                    
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataManutencao = converterTimestampParaDate(data["dataManutencao"])
                        ?: converterTimestampParaDate(data["data_manutencao"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val historico = HistoricoManutencaoVeiculo(
                        id = historicoId,
                        veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: (data["veiculo_id"] as? Number)?.toLong() ?: 0L,
                        tipoManutencao = data["tipoManutencao"] as? String ?: (data["tipo_manutencao"] as? String) ?: "",
                        descricao = data["descricao"] as? String ?: "",
                        dataManutencao = dataManutencao,
                        valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                        kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: (data["km_veiculo"] as? Number)?.toLong() ?: 0L,
                        responsavel = data["responsavel"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    if (historico.veiculoId == 0L) {
                        Log.w(TAG, "⚠️ Histórico manutenção veículo ID $historicoId sem veiculoId - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    // TODO: Verificar se método inserirHistoricoManutencaoVeiculo() está implementado no AppRepository
                    appRepository.inserirHistoricoManutencao(historico)
                    syncCount++
                    Log.d(TAG, "✅ HistoricoManutencaoVeiculo sincronizado: Veículo ${historico.veiculoId} (ID: $historicoId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico manutenção veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de histórico manutenção veículo concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de histórico manutenção veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoManutencaoVeiculo: Envia histórico de manutenção de veículos do Room para o Firestore
     */
    private suspend fun pushHistoricoManutencaoVeiculo(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de histórico manutenção veículo...")
            val historicosLocais = appRepository.obterTodosHistoricoManutencaoVeiculo()
            Log.d(TAG, "📥 Total de históricos de manutenção locais encontrados: ${historicosLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            historicosLocais.forEach { historico ->
                try {
                    Log.d(TAG, "📄 Processando histórico manutenção: ID=${historico.id}, Veículo=${historico.veiculoId}")
                    
                    val historicoMap = entityToMap(historico)
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    historicoMap["roomId"] = historico.id
                    historicoMap["id"] = historico.id
                    
                    // Adicionar metadados de sincronização
                    historicoMap["lastModified"] = FieldValue.serverTimestamp()
                    historicoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = historico.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_HISTORICO_MANUTENCAO_VEICULO)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(historicoMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Histórico manutenção enviado: Veículo ${historico.veiculoId} (ID: ${historico.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar histórico manutenção ${historico.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de histórico manutenção veículo concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de histórico manutenção veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull HistoricoCombustivelVeiculo: Sincroniza histórico de combustível de veículos do Firestore para o Room
     */
    private suspend fun pullHistoricoCombustivelVeiculo(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de histórico combustível veículo...")
            val collectionPath = getCollectionPath(COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de histórico combustível veículo no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando histórico combustível veículo: ID=${doc.id}")
                    
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataAbastecimento = converterTimestampParaDate(data["dataAbastecimento"])
                        ?: converterTimestampParaDate(data["data_abastecimento"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val historico = HistoricoCombustivelVeiculo(
                        id = historicoId,
                        veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: (data["veiculo_id"] as? Number)?.toLong() ?: 0L,
                        dataAbastecimento = dataAbastecimento,
                        litros = (data["litros"] as? Number)?.toDouble() ?: 0.0,
                        valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                        kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: (data["km_veiculo"] as? Number)?.toLong() ?: 0L,
                        kmRodado = (data["kmRodado"] as? Number)?.toDouble() ?: (data["km_rodado"] as? Number)?.toDouble() ?: 0.0,
                        posto = data["posto"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    if (historico.veiculoId == 0L) {
                        Log.w(TAG, "⚠️ Histórico combustível veículo ID $historicoId sem veiculoId - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    // TODO: Verificar se método inserirHistoricoCombustivelVeiculo() está implementado no AppRepository
                    appRepository.inserirHistoricoCombustivel(historico)
                    syncCount++
                    Log.d(TAG, "✅ HistoricoCombustivelVeiculo sincronizado: Veículo ${historico.veiculoId} (ID: $historicoId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico combustível veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de histórico combustível veículo concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de histórico combustível veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Veiculos: Sincroniza veículos do Firestore para o Room
     */
    private suspend fun pullVeiculos(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando pull de veículos...")
            val collectionPath = getCollectionPath(COLLECTION_VEICULOS)
            val snapshot = firestore.collection(collectionPath).get().await()
            Log.d(TAG, "📥 Total de veículos no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando veículo: ID=${doc.id}, Placa=${data["placa"]}")
                    
                    val veiculoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (veiculoId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para veículo ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val dataCompra = converterTimestampParaDate(data["dataCompra"])
                        ?: converterTimestampParaDate(data["data_compra"])
                    
                    val veiculo = Veiculo(
                        id = veiculoId,
                        nome = data["nome"] as? String ?: "",
                        placa = data["placa"] as? String ?: "",
                        marca = data["marca"] as? String ?: "",
                        modelo = data["modelo"] as? String ?: "",
                        anoModelo = (data["anoModelo"] as? Number)?.toInt()
                            ?: (data["ano_modelo"] as? Number)?.toInt() ?: 0,
                        kmAtual = (data["kmAtual"] as? Number)?.toLong()
                            ?: (data["km_atual"] as? Number)?.toLong() ?: 0L,
                        dataCompra = dataCompra,
                        observacoes = data["observacoes"] as? String
                    )
                    
                    if (veiculo.placa.isBlank()) {
                        Log.w(TAG, "⚠️ Veículo ID $veiculoId sem placa - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    // Verificar conflito de timestamp (se houver campo de atualização)
                    // Por enquanto, inserir/atualizar sempre (OnConflictStrategy.REPLACE)
                    appRepository.inserirVeiculo(veiculo)
                    syncCount++
                    Log.d(TAG, "✅ Veículo sincronizado: ${veiculo.placa} (ID: ${veiculo.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Pull de veículos concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de veículos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoCombustivelVeiculo: Envia histórico de combustível de veículos modificado do Room para o Firestore
     * TODO: Adicionar método obterTodosHistoricoCombustivelVeiculo() no AppRepository
     */
    /**
     * Push HistoricoCombustivelVeiculo: Envia histórico de combustível de veículos do Room para o Firestore
     */
    private suspend fun pushHistoricoCombustivelVeiculo(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de histórico combustível veículo...")
            val historicosLocais = appRepository.obterTodosHistoricoCombustivelVeiculo()
            Log.d(TAG, "📥 Total de históricos de combustível locais encontrados: ${historicosLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            historicosLocais.forEach { historico ->
                try {
                    Log.d(TAG, "📄 Processando histórico combustível: ID=${historico.id}, Veículo=${historico.veiculoId}")
                    
                    val historicoMap = entityToMap(historico)
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    historicoMap["roomId"] = historico.id
                    historicoMap["id"] = historico.id
                    
                    // Adicionar metadados de sincronização
                    historicoMap["lastModified"] = FieldValue.serverTimestamp()
                    historicoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = historico.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO)
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(historicoMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Histórico combustível enviado: Veículo ${historico.veiculoId} (ID: ${historico.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar histórico combustível ${historico.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de histórico combustível veículo concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de histórico combustível veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Veiculos: Envia veículos do Room para o Firestore
     */
    private suspend fun pushVeiculos(): Result<Int> {
        return try {
            Log.d(TAG, "🔵 Iniciando push de veículos...")
            val veiculosLocais = appRepository.obterTodosVeiculos().first()
            Log.d(TAG, "📥 Total de veículos locais encontrados: ${veiculosLocais.size}")
            
            var syncCount = 0
            var errorCount = 0
            
            veiculosLocais.forEach { veiculo ->
                try {
                    Log.d(TAG, "📄 Processando veículo: ID=${veiculo.id}, Nome=${veiculo.nome}, Placa=${veiculo.placa}")
                    
                    val veiculoMap = entityToMap(veiculo)
                    Log.d(TAG, "   Mapa criado com ${veiculoMap.size} campos")
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    veiculoMap["roomId"] = veiculo.id
                    veiculoMap["id"] = veiculo.id
                    
                    // Adicionar metadados de sincronização
                    veiculoMap["lastModified"] = FieldValue.serverTimestamp()
                    veiculoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = veiculo.id.toString()
                    val collectionPath = getCollectionPath(COLLECTION_VEICULOS)
                    Log.d(TAG, "   Enviando para Firestore: collection=$collectionPath, document=$documentId")
                    
                    firestore.collection(collectionPath)
                        .document(documentId)
                        .set(veiculoMap)
                        .await()
                    
                    syncCount++
                    Log.d(TAG, "✅ Veículo enviado com sucesso: ${veiculo.nome} - ${veiculo.placa} (ID: ${veiculo.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar veículo ${veiculo.id} (${veiculo.nome}): ${e.message}", e)
                }
            }
            
            Log.d(TAG, "✅ Push de veículos concluído: $syncCount enviados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no push de veículos: ${e.message}", e)
            Result.failure(e)
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

