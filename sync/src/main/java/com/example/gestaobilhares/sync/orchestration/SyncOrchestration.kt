package com.example.gestaobilhares.sync.orchestration

import com.example.gestaobilhares.sync.handlers.*
import com.example.gestaobilhares.sync.core.SyncCore
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.utils.SyncUtils
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber
import kotlinx.coroutines.flow.first

/**
 * Orquestração dos handlers de sincronização.
 * Contém a lógica de coordenação entre os diferentes handlers.
 */
class SyncOrchestration(
    private val mesaSyncHandler: MesaSyncHandler,
    private val clienteSyncHandler: ClienteSyncHandler,
    private val contratoSyncHandler: ContratoSyncHandler,
    private val acertoSyncHandler: AcertoSyncHandler,
    private val despesaSyncHandler: DespesaSyncHandler,
    private val rotaSyncHandler: RotaSyncHandler,
    private val cicloSyncHandler: CicloSyncHandler,
    private val colaboradorSyncHandler: ColaboradorSyncHandler,
    private val colaboradorRotaSyncHandler: ColaboradorRotaSyncHandler,
    private val metaColaboradorSyncHandler: MetaColaboradorSyncHandler,
    private val metaSyncHandler: MetaSyncHandler,
    private val assinaturaSyncHandler: AssinaturaSyncHandler,
    private val veiculoSyncHandler: VeiculoSyncHandler,
    private val equipamentoSyncHandler: EquipamentoSyncHandler,
    private val estoqueSyncHandler: EstoqueSyncHandler,
    private val syncCore: SyncCore,
    private val appRepository: AppRepository,
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val TAG = "SyncOrchestration"
        
        // Coleções do Firestore
        private const val COLLECTION_MESAS = "mesas"
        private const val COLLECTION_CLIENTES = "clientes"
        private const val COLLECTION_CONTRATOS = "contratos"
        private const val COLLECTION_ACERTOS = "acertos"
        private const val COLLECTION_DESPESAS = "despesas"
        private const val COLLECTION_ROTAS = "rotas"
        private const val COLLECTION_CICLOS = "ciclos"
        private const val COLLECTION_COLABORADORES = "colaboradores"
        private const val COLLECTION_COLABORADOR_ROTAS = "colaborador_rotas"
        private const val COLLECTION_META_COLABORADOR = "meta_colaborador"
        private const val COLLECTION_METAS = "metas"
        private const val COLLECTION_ASSINATURAS = "assinaturas"
        private const val COLLECTION_VEICULOS = "veiculos"
        private const val COLLECTION_EQUIPAMENTOS = "equipamentos"
        private const val COLLECTION_ESTOQUE = "estoque"
    }
    
    /**
     * Executa sincronização completa de todos os handlers.
     */
    suspend fun syncAll(): SyncResult {
        val startTime = System.currentTimeMillis()
        var totalSynced = 0
        var errors = mutableListOf<String>()
        
        try {
            Timber.tag(TAG).d("🚀 Iniciando sincronização completa...")
            
            // Obter companyId atual
            val companyId = getCurrentCompanyId()
            
            // Sincronizar na ordem de dependência
            val handlers = listOf(
                SyncHandlerEntry("rotas", rotaSyncHandler),
                SyncHandlerEntry("colaboradores", colaboradorSyncHandler),
                SyncHandlerEntry("clientes", clienteSyncHandler),
                SyncHandlerEntry("mesas", mesaSyncHandler),
                SyncHandlerEntry("contratos", contratoSyncHandler),
                SyncHandlerEntry("acertos", acertoSyncHandler),
                SyncHandlerEntry("despesas", despesaSyncHandler),
                SyncHandlerEntry("ciclos", cicloSyncHandler),
                SyncHandlerEntry("colaborador_rotas", colaboradorRotaSyncHandler),
                SyncHandlerEntry("meta_colaborador", metaColaboradorSyncHandler),
                SyncHandlerEntry("metas", metaSyncHandler),
                SyncHandlerEntry("assinaturas", assinaturaSyncHandler),
                SyncHandlerEntry("veiculos", veiculoSyncHandler),
                SyncHandlerEntry("equipamentos", equipamentoSyncHandler),
                SyncHandlerEntry("estoque", estoqueSyncHandler)
            )
            
            for (handlerEntry in handlers) {
                try {
                    Timber.tag(TAG).d("📥 Sincronizando ${handlerEntry.name}...")
                    
                    // Pull: buscar dados do servidor
                    val pullResult = syncHandler(handlerEntry.name, handlerEntry.handler)
                    totalSynced += pullResult.syncedCount
                    
                    // Push: enviar dados locais para o servidor
                    val pushResult = pushHandler(handlerEntry.name, handlerEntry.handler)
                    totalSynced += pushResult.pushedCount
                    
                    if (pullResult.error != null) {
                        errors.add("${handlerEntry.name} (pull): ${pullResult.error}")
                    }
                    
                    if (pushResult.error != null) {
                        errors.add("${handlerEntry.name} (push): ${pushResult.error}")
                    }
                    
                } catch (e: Exception) {
                    val error = "Erro em ${handlerEntry.name}: ${e.message}"
                    Timber.tag(TAG).e(e, error)
                    errors.add(error)
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            // Salvar metadados globais
            syncCore.saveSyncMetadata(
                entityType = "global_sync",
                syncCount = totalSynced,
                durationMs = duration,
                error = if (errors.isNotEmpty()) errors.joinToString("; ") else null
            )
            
            Timber.tag(TAG).d("✅ Sincronização completa: $totalSynced registros em ${duration}ms")
            
            // Se houver erros, log detalhado
            if (errors.isNotEmpty()) {
                Timber.tag(TAG).e("Erros na sincronização: ${errors.joinToString("; ")}")
            }
            
            return SyncResult(
                success = errors.isEmpty(),
                syncedCount = totalSynced,
                durationMs = duration,
                errors = errors
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val error = "Erro na sincronização completa: ${e.message}"
            Timber.tag(TAG).e(e, error)
            
            return SyncResult(
                success = false,
                syncedCount = totalSynced,
                durationMs = duration,
                errors = listOf(error)
            )
        }
    }
    
    /**
     * Sincroniza um handler específico.
     */
    private suspend fun syncHandler(
        entityType: String,
        handler: Any
    ): HandlerSyncResult {
        val startTime = System.currentTimeMillis()
        
        try {
            Timber.tag(TAG).d("📥 Sincronizando $entityType...")
            
            // Obter timestamp da última sincronização
            val lastSyncTimestamp = syncCore.getLastSyncTimestamp(entityType)
            
            // Executar sincronização do handler
            val result = when (handler) {
                is MesaSyncHandler -> handler.pull(lastSyncTimestamp)
                is ClienteSyncHandler -> handler.pull(lastSyncTimestamp)
                is ContratoSyncHandler -> handler.pull(lastSyncTimestamp)
                is AcertoSyncHandler -> handler.pull(lastSyncTimestamp)
                is DespesaSyncHandler -> handler.pull(lastSyncTimestamp)
                is RotaSyncHandler -> handler.pull(lastSyncTimestamp)
                is CicloSyncHandler -> handler.pull(lastSyncTimestamp)
                is ColaboradorSyncHandler -> handler.pull(lastSyncTimestamp)
                is ColaboradorRotaSyncHandler -> handler.pull(lastSyncTimestamp)
                is MetaColaboradorSyncHandler -> handler.pull(lastSyncTimestamp)
                is MetaSyncHandler -> handler.pull(lastSyncTimestamp)
                is AssinaturaSyncHandler -> handler.pull(lastSyncTimestamp)
                is VeiculoSyncHandler -> handler.pull(lastSyncTimestamp)
                is EquipamentoSyncHandler -> handler.pull(lastSyncTimestamp)
                is EstoqueSyncHandler -> handler.pull(lastSyncTimestamp)
                else -> throw IllegalArgumentException("Handler não suportado: ${handler::class.java}")
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            // Salvar metadados
            // syncCore.saveSyncMetadata(
            //     entityType = entityType,
            //     syncCount = result.getOrNull() ?: 0,
            //     durationMs = duration,
            //     error = if (result.isSuccess) null else result.exceptionOrNull()?.message
            // )
            
            return HandlerSyncResult(
                success = result.isSuccess,
                syncedCount = result.getOrNull() ?: 0,
                durationMs = duration,
                error = if (result.isSuccess) null else result.exceptionOrNull()?.message
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val error = "Erro ao sincronizar $entityType: ${e.message}"
            Timber.tag(TAG).e(e, error)
            
            return HandlerSyncResult(
                success = false,
                syncedCount = 0,
                durationMs = duration,
                error = error
            )
        }
    }
    
    /**
     * Executa push de dados locais para o Firestore.
     */
    suspend fun pushAll(): SyncResult {
        val startTime = System.currentTimeMillis()
        var totalPushed = 0
        var errors = mutableListOf<String>()
        
        try {
            Timber.tag(TAG).d("🚀 Iniciando push completo...")
            
            val companyId = getCurrentCompanyId()
            
            // Push na ordem inversa (dependências primeiro)
            val handlers = listOf(
                SyncHandlerEntry("estoque", estoqueSyncHandler),
                SyncHandlerEntry("equipamentos", equipamentoSyncHandler),
                SyncHandlerEntry("veiculos", veiculoSyncHandler),
                SyncHandlerEntry("assinaturas", assinaturaSyncHandler),
                SyncHandlerEntry("metas", metaSyncHandler),
                SyncHandlerEntry("meta_colaborador", metaColaboradorSyncHandler),
                SyncHandlerEntry("colaborador_rotas", colaboradorRotaSyncHandler),
                SyncHandlerEntry("ciclos", cicloSyncHandler),
                SyncHandlerEntry("despesas", despesaSyncHandler),
                SyncHandlerEntry("acertos", acertoSyncHandler),
                SyncHandlerEntry("contratos", contratoSyncHandler),
                SyncHandlerEntry("mesas", mesaSyncHandler),
                SyncHandlerEntry("clientes", clienteSyncHandler),
                SyncHandlerEntry("colaboradores", colaboradorSyncHandler),
                SyncHandlerEntry("rotas", rotaSyncHandler)
            )
            
            for (handlerEntry in handlers) {
                try {
                    Timber.tag(TAG).d("📤 Push ${handlerEntry.name}...")
                    val result = pushHandler(handlerEntry.name, handlerEntry.handler)
                    totalPushed += result.pushedCount
                    
                    if (result.error != null) {
                        errors.add("${handlerEntry.name}: ${result.error}")
                    }
                    
                } catch (e: Exception) {
                    val error = "Erro no push ${handlerEntry.name}: ${e.message}"
                    Timber.tag(TAG).e(e, error)
                    errors.add(error)
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            Timber.tag(TAG).d("✅ Push completo: $totalPushed registros em ${duration}ms")
            
            return SyncResult(
                success = errors.isEmpty(),
                syncedCount = totalPushed,
                durationMs = duration,
                errors = errors
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val error = "Erro no push completo: ${e.message}"
            Timber.tag(TAG).e(e, error)
            
            return SyncResult(
                success = false,
                syncedCount = totalPushed,
                durationMs = duration,
                errors = listOf(error)
            )
        }
    }
    
    /**
     * Executa push de um handler específico.
     */
    private suspend fun pushHandler(
        entityType: String,
        handler: Any
    ): HandlerPushResult {
        val startTime = System.currentTimeMillis()
        
        try {
            Timber.tag(TAG).d("📤 Push $entityType...")
            
            // Executar push do handler
            val result = when (handler) {
                is MesaSyncHandler -> handler.push()
                is ClienteSyncHandler -> handler.push()
                is ContratoSyncHandler -> handler.push()
                is AcertoSyncHandler -> handler.push()
                is DespesaSyncHandler -> handler.push()
                is RotaSyncHandler -> handler.push()
                is CicloSyncHandler -> handler.push()
                is ColaboradorSyncHandler -> handler.push()
                is ColaboradorRotaSyncHandler -> handler.push()
                is MetaColaboradorSyncHandler -> handler.push()
                is MetaSyncHandler -> handler.push()
                is AssinaturaSyncHandler -> handler.push()
                is VeiculoSyncHandler -> handler.push()
                is EquipamentoSyncHandler -> handler.push()
                is EstoqueSyncHandler -> handler.push()
                else -> throw IllegalArgumentException("Handler não suportado: ${handler::class.java}")
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            // Salvar metadados de push
            syncCore.savePushMetadata(
                entityType = entityType,
                syncCount = result.getOrNull() ?: 0,
                durationMs = duration,
                error = if (result.isSuccess) null else result.exceptionOrNull()?.message
            )
            
            return HandlerPushResult(
                success = result.isSuccess,
                pushedCount = result.getOrNull() ?: 0,
                durationMs = duration,
                error = if (result.isSuccess) null else result.exceptionOrNull()?.message
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val error = "Erro no push $entityType: ${e.message}"
            Timber.tag(TAG).e(e, error)
            
            return HandlerPushResult(
                success = false,
                pushedCount = 0,
                durationMs = duration,
                error = error
            )
        }
    }
    
    /**
     * Obtém o companyId atual.
     */
    private fun getCurrentCompanyId(): String {
        // Implementar lógica para obter companyId atual
        // Por enquanto, usa valor padrão
        return "empresa_001"
    }
    
    /**
     * Entrada de handler para orquestração.
     */
    private data class SyncHandlerEntry(
        val name: String,
        val handler: Any
    )
    
    /**
     * Resultado da sincronização completa.
     */
    data class SyncResult(
        val success: Boolean,
        val syncedCount: Int,
        val durationMs: Long,
        val errors: List<String>
    )
    
    /**
     * Resultado da sincronização de um handler.
     */
    data class HandlerSyncResult(
        val success: Boolean,
        val syncedCount: Int,
        val durationMs: Long,
        val error: String?
    )
    
    /**
     * Resultado do push de um handler.
     */
    data class HandlerPushResult(
        val success: Boolean,
        val pushedCount: Int,
        val durationMs: Long,
        val error: String?
    )
}
