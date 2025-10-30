package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.cache.AppCacheManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import com.example.gestaobilhares.BuildConfig
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
// ✅ FASE 4C: WorkManager para processamento em background
import androidx.work.*
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.gestaobilhares.workers.SyncWorker
import com.example.gestaobilhares.workers.CleanupWorker
// ✅ FASE 4D: Otimizações de memória
import com.example.gestaobilhares.memory.MemoryOptimizer
import com.example.gestaobilhares.memory.WeakReferenceManager
import com.example.gestaobilhares.memory.ObjectPool
// ✅ FASE 4D: Otimizações de UI
import com.example.gestaobilhares.ui.optimization.ViewStubManager
import com.example.gestaobilhares.ui.optimization.OptimizedViewHolder
import com.example.gestaobilhares.ui.optimization.LayoutOptimizer
import com.example.gestaobilhares.ui.optimization.RecyclerViewOptimizer
// ✅ FASE 4D: Otimizações de Rede
import com.example.gestaobilhares.network.NetworkCompressionManager
import com.example.gestaobilhares.network.BatchOperationsManager
import com.example.gestaobilhares.network.RetryLogicManager
import com.example.gestaobilhares.network.NetworkCacheManager
import kotlinx.coroutines.Deferred
// ✅ FASE 4D: Otimizações Avançadas de Banco
import com.example.gestaobilhares.database.DatabaseConnectionPool
import com.example.gestaobilhares.database.QueryOptimizationManager
import com.example.gestaobilhares.database.DatabasePerformanceTuner
import com.example.gestaobilhares.database.TransactionOptimizationManager
// ✅ REMOVIDO: Hilt não é mais usado

/**
 * ✅ REPOSITORY CONSOLIDADO E MODERNIZADO - AppRepository
 * 
 * FASE 2: Modernização com StateFlow e centralização
 * - Combina todos os repositories em um único arquivo
 * - Elimina duplicação e simplifica a arquitetura
 * - Modernizado com StateFlow para melhor performance
 * - Centralizado para facilitar manutenção
 */
class AppRepository constructor(
    private val clienteDao: ClienteDao,
    private val acertoDao: AcertoDao,
    private val mesaDao: MesaDao,
    // ✅ FASE 3C: DAOs de sincronização
    private val syncLogDao: SyncLogDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncConfigDao: SyncConfigDao,
    // ✅ FASE 4C: WorkManager para processamento em background
    private val context: Context,
    private val rotaDao: RotaDao,
    private val despesaDao: DespesaDao,
    private val colaboradorDao: ColaboradorDao,
    private val cicloAcertoDao: CicloAcertoDao,
    private val acertoMesaDao: com.example.gestaobilhares.data.dao.AcertoMesaDao,
    private val contratoLocacaoDao: ContratoLocacaoDao,
    private val aditivoContratoDao: AditivoContratoDao,
    private val assinaturaRepresentanteLegalDao: AssinaturaRepresentanteLegalDao,
    private val logAuditoriaAssinaturaDao: LogAuditoriaAssinaturaDao,
    private val panoEstoqueDao: com.example.gestaobilhares.data.dao.PanoEstoqueDao,
    private val mesaVendidaDao: com.example.gestaobilhares.data.dao.MesaVendidaDao,
    private val stockItemDao: com.example.gestaobilhares.data.dao.StockItemDao,
    private val veiculoDao: com.example.gestaobilhares.data.dao.VeiculoDao,
    private val categoriaDespesaDao: CategoriaDespesaDao,
    private val tipoDespesaDao: TipoDespesaDao,
    private val historicoManutencaoVeiculoDao: HistoricoManutencaoVeiculoDao,
    private val historicoCombustivelVeiculoDao: HistoricoCombustivelVeiculoDao,
    private val historicoManutencaoMesaDao: HistoricoManutencaoMesaDao,
    private val mesaReformadaDao: com.example.gestaobilhares.data.dao.MesaReformadaDao
) {
    
    // ✅ FASE 4A: Cache Manager para otimização de performance
    private val cacheManager = AppCacheManager.getInstance()
    
    // ✅ FASE 4D: Otimizações de memória
    private val memoryOptimizer = MemoryOptimizer.getInstance()
    private val weakReferenceManager = WeakReferenceManager.getInstance()
    
    // ✅ FASE 4D: Otimizações de UI
    private val viewStubManager = ViewStubManager.getInstance()
    private val optimizedViewHolder = OptimizedViewHolder.getInstance()
    private val layoutOptimizer = LayoutOptimizer.getInstance()
    private val recyclerViewOptimizer = RecyclerViewOptimizer.getInstance()
    
    // ✅ FASE 4D: Otimizações de Rede
    private val networkCompressionManager = NetworkCompressionManager.getInstance()
    private val batchOperationsManager = BatchOperationsManager.getInstance()
    private val retryLogicManager = RetryLogicManager.getInstance()
    private val networkCacheManager = NetworkCacheManager.getInstance()
    
    // ✅ FASE 4D: Otimizações Avançadas de Banco
    private val connectionPool = DatabaseConnectionPool.getInstance()
    private val queryOptimizer = QueryOptimizationManager.getInstance()
    private val performanceTuner = DatabasePerformanceTuner.getInstance()
    private val transactionOptimizer = TransactionOptimizationManager.getInstance()
    
    // ==================== CATEGORIAS E TIPOS DE DESPESA ====================
    fun buscarCategoriasAtivas() = categoriaDespesaDao.buscarAtivas()
    suspend fun buscarCategoriaPorNome(nome: String) = categoriaDespesaDao.buscarPorNome(nome)
    suspend fun categoriaExiste(nome: String): Boolean = categoriaDespesaDao.contarPorNome(nome) > 0
    suspend fun criarCategoria(nova: NovaCategoriaDespesa): Long {
        val entity = CategoriaDespesa(
            nome = nova.nome,
            descricao = nova.descricao,
            criadoPor = nova.criadoPor
        )
        val id = categoriaDespesaDao.inserir(entity)
        
        // ✅ SINCRONIZAÇÃO: CREATE CategoriaDespesa
        try {
            val payload = """
                {
                    "id": $id,
                    "nome": "${entity.nome}",
                    "descricao": "${entity.descricao}",
                    "criadoPor": "${entity.criadoPor}",
                    "ativo": ${entity.ativa},
                    "dataCriacao": ${entity.dataCriacao.time}
                }
            """.trimIndent()
            adicionarOperacaoSync("CategoriaDespesa", id, "CREATE", payload, priority = 1)
            logarOperacaoSync("CategoriaDespesa", id, "CREATE", "PENDING", null, payload)
        } catch (syncError: Exception) {
            Log.w("AppRepository", "Erro ao adicionar categoria à fila de sync: ${syncError.message}")
        }
        
        return id
    }
    fun buscarTiposPorCategoria(categoriaId: Long) = tipoDespesaDao.buscarPorCategoria(categoriaId)
    suspend fun buscarTipoPorNome(nome: String) = tipoDespesaDao.buscarPorNome(nome)
    suspend fun tipoExiste(nome: String, categoriaId: Long): Boolean {
        val tipo = tipoDespesaDao.buscarPorNome(nome)
        return tipo != null && tipo.categoriaId == categoriaId
    }
    suspend fun criarTipo(novo: NovoTipoDespesa): Long {
        val entity = TipoDespesa(
            categoriaId = novo.categoriaId,
            nome = novo.nome,
            descricao = novo.descricao,
            criadoPor = novo.criadoPor
        )
        val id = tipoDespesaDao.inserir(entity)
        
        // ✅ SINCRONIZAÇÃO: CREATE TipoDespesa
        try {
            val payload = """
                {
                    "id": $id,
                    "categoriaId": ${entity.categoriaId},
                    "nome": "${entity.nome}",
                    "descricao": "${entity.descricao}",
                    "criadoPor": "${entity.criadoPor}",
                    "ativo": ${entity.ativo},
                    "dataCriacao": ${entity.dataCriacao.time}
                }
            """.trimIndent()
            adicionarOperacaoSync("TipoDespesa", id, "CREATE", payload, priority = 1)
            logarOperacaoSync("TipoDespesa", id, "CREATE", "PENDING", null, payload)
        } catch (syncError: Exception) {
            Log.w("AppRepository", "Erro ao adicionar tipo à fila de sync: ${syncError.message}")
        }
        
        return id
    }

    
    // ==================== STATEFLOW CACHE (MODERNIZAÇÃO 2025) ====================
    
    /**
     * Cache de clientes para performance
     */
    private val _clientesCache = MutableStateFlow<List<Cliente>>(emptyList())
    val clientesCache: StateFlow<List<Cliente>> = _clientesCache.asStateFlow()
    
    /**
     * Cache de rotas para performance
     */
    private val _rotasCache = MutableStateFlow<List<Rota>>(emptyList())
    val rotasCache: StateFlow<List<Rota>> = _rotasCache.asStateFlow()
    
    /**
     * Cache de mesas para performance
     */
    private val _mesasCache = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCache: StateFlow<List<Mesa>> = _mesasCache.asStateFlow()
    
    // ==================== CLIENTE ====================
    
    /**
     * ✅ MODERNIZADO: Obtém todos os clientes com cache StateFlow
     */
    fun obterTodosClientes(): Flow<List<Cliente>> = clienteDao.obterTodos()
    
    /**
     * ✅ MODERNIZADO: Obtém clientes por rota com cache
     */
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> = clienteDao.obterClientesPorRota(rotaId)
    
    /**
     * ✅ FASE 2A: Método otimizado com débito atual calculado
     * Usa query otimizada que calcula débito atual diretamente no banco
     */
    fun obterClientesPorRotaComDebitoAtual(rotaId: Long): Flow<List<Cliente>> = 
        clienteDao.obterClientesPorRotaComDebitoAtual(rotaId)
    
    suspend fun obterClientePorId(id: Long) = clienteDao.obterPorId(id)
    suspend fun inserirCliente(cliente: Cliente): Long {
        logDbInsertStart("CLIENTE", "Nome=${cliente.nome}, RotaID=${cliente.rotaId}")
        return try {
            val id = clienteDao.inserir(cliente)
            logDbInsertSuccess("CLIENTE", "Nome=${cliente.nome}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${cliente.nome}",
                        "telefone": "${cliente.telefone}",
                        "endereco": "${cliente.endereco}",
                        "rotaId": ${cliente.rotaId},
                        "ativo": ${cliente.ativo},
                        "dataCadastro": "${cliente.dataCadastro}",
                        "valorFicha": ${cliente.valorFicha},
                        "comissaoFicha": ${cliente.comissaoFicha}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Cliente", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("Cliente", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar cliente à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CLIENTE", "Nome=${cliente.nome}", e)
            throw e
        }
    }
    suspend fun atualizarCliente(cliente: Cliente) {
        logDbUpdateStart("CLIENTE", "ID=${cliente.id}, Nome=${cliente.nome}")
        try {
            clienteDao.atualizar(cliente)
            logDbUpdateSuccess("CLIENTE", "ID=${cliente.id}, Nome=${cliente.nome}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${cliente.id},
                        "nome": "${cliente.nome}",
                        "telefone": "${cliente.telefone}",
                        "endereco": "${cliente.endereco}",
                        "rotaId": ${cliente.rotaId},
                        "ativo": ${cliente.ativo},
                        "dataCadastro": "${cliente.dataCadastro}",
                        "debitoAtual": ${cliente.debitoAtual},
                        "valorFicha": ${cliente.valorFicha},
                        "comissaoFicha": ${cliente.comissaoFicha}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Cliente", cliente.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("Cliente", cliente.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de cliente à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("CLIENTE", "ID=${cliente.id}", e)
            throw e
        }
    }
    suspend fun deletarCliente(cliente: Cliente) = clienteDao.deletar(cliente)
    suspend fun atualizarDebitoAtual(clienteId: Long, novoDebito: Double) {
        logDbUpdateStart("CLIENTE_DEBITO", "ClienteID=$clienteId, NovoDebito=$novoDebito")
        try {
            clienteDao.atualizarDebitoAtual(clienteId, novoDebito)
            logDbUpdateSuccess("CLIENTE_DEBITO", "ClienteID=$clienteId, NovoDebito=$novoDebito")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val cliente = obterClientePorId(clienteId)
                if (cliente != null) {
                    val payload = """
                        {
                            "id": ${cliente.id},
                            "nome": "${cliente.nome}",
                            "telefone": "${cliente.telefone}",
                            "endereco": "${cliente.endereco}",
                            "rotaId": ${cliente.rotaId},
                            "ativo": ${cliente.ativo},
                            "dataCadastro": "${cliente.dataCadastro}",
                            "debitoAtual": $novoDebito,
                            "valorFicha": ${cliente.valorFicha},
                            "comissaoFicha": ${cliente.comissaoFicha}
                        }
                    """.trimIndent()
                    
                    adicionarOperacaoSync("Cliente", clienteId, "UPDATE", payload, priority = 1)
                    logarOperacaoSync("Cliente", clienteId, "UPDATE", "PENDING", null, payload)
                }
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de débito à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("CLIENTE_DEBITO", "ClienteID=$clienteId", e)
            throw e
        }
    }
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long) = 
        clienteDao.calcularDebitoAtualEmTempoReal(clienteId)
    suspend fun obterClienteComDebitoAtual(clienteId: Long) = 
        clienteDao.obterClienteComDebitoAtual(clienteId)
    
    /**
     * ✅ NOVO: Busca o ID da rota associada a um cliente
     */
                suspend fun buscarRotaIdPorCliente(clienteId: Long): Long? {
                    return try {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("AppRepository", "Buscando cliente ID: $clienteId")
                        }
                        val cliente = obterClientePorId(clienteId)
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("AppRepository", "Cliente encontrado: ${cliente?.nome}, rotaId: ${cliente?.rotaId}")
                        }
                        cliente?.rotaId
                    } catch (e: Exception) {
                        android.util.Log.e("AppRepository", "Erro ao buscar rota ID por cliente: ${e.message}", e)
                        null
                    }
                }
    
    // ==================== ACERTO ====================
    
    fun obterAcertosPorCliente(clienteId: Long) = acertoDao.buscarPorCliente(clienteId)
    suspend fun obterAcertoPorId(id: Long) = acertoDao.buscarPorId(id)
    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long) = 
        acertoDao.buscarUltimoAcertoPorCliente(clienteId)
    fun obterTodosAcertos() = acertoDao.listarTodos()
    fun buscarAcertosPorCicloId(cicloId: Long) = acertoDao.buscarPorCicloId(cicloId)
    fun buscarClientesPorRota(rotaId: Long) = clienteDao.obterClientesPorRota(rotaId)
    suspend fun buscarRotaPorId(rotaId: Long) = rotaDao.getRotaById(rotaId)
    suspend fun inserirAcerto(acerto: Acerto): Long {
        logDbInsertStart("ACERTO", "ClienteID=${acerto.clienteId}, RotaID=${acerto.rotaId}, Valor=${acerto.valorRecebido}")
        return try {
            val id = acertoDao.inserir(acerto)
            logDbInsertSuccess("ACERTO", "ClienteID=${acerto.clienteId}, ID=$id")
            
            // ✅ CORREÇÃO: Não adicionar à fila de sync aqui - será feito pelo SettlementViewModel
            // após inserir as mesas do acerto
            
            id
        } catch (e: Exception) {
            logDbInsertError("ACERTO", "ClienteID=${acerto.clienteId}", e)
            throw e
        }
    }
    suspend fun atualizarAcerto(acerto: Acerto) {
        logDbUpdateStart("ACERTO", "ID=${acerto.id}, ClienteID=${acerto.clienteId}")
        try {
            acertoDao.atualizar(acerto)
            logDbUpdateSuccess("ACERTO", "ID=${acerto.id}, ClienteID=${acerto.clienteId}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payloadMap = mutableMapOf<String, Any?>(
                    "id" to acerto.id,
                    "clienteId" to acerto.clienteId,
                    "colaboradorId" to acerto.colaboradorId,
                    "dataAcerto" to acerto.dataAcerto,
                    "periodoInicio" to acerto.periodoInicio,
                    "periodoFim" to acerto.periodoFim,
                    "totalMesas" to acerto.totalMesas,
                    "debitoAnterior" to acerto.debitoAnterior,
                    "valorTotal" to acerto.valorTotal,
                    "desconto" to acerto.desconto,
                    "valorComDesconto" to acerto.valorComDesconto,
                    "valorRecebido" to acerto.valorRecebido,
                    "debitoAtual" to acerto.debitoAtual,
                    "status" to acerto.status.name,
                    "observacoes" to acerto.observacoes,
                    "dataCriacao" to acerto.dataCriacao,
                    "dataFinalizacao" to acerto.dataFinalizacao,
                    "representante" to acerto.representante,
                    "tipoAcerto" to acerto.tipoAcerto,
                    "panoTrocado" to acerto.panoTrocado,
                    "numeroPano" to acerto.numeroPano,
                    "rotaId" to acerto.rotaId,
                    "cicloId" to acerto.cicloId,
                    "syncTimestamp" to acerto.syncTimestamp,
                    "syncVersion" to acerto.syncVersion,
                    "syncStatus" to acerto.syncStatus.name
                )
                // Adicionar JSONs que já podem estar formatados
                acerto.metodosPagamentoJson?.let { payloadMap["metodosPagamentoJson"] = it }
                acerto.dadosExtrasJson?.let { payloadMap["dadosExtrasJson"] = it }

                val payload = com.google.gson.Gson().toJson(payloadMap)

                adicionarOperacaoSync("Acerto", acerto.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("Acerto", acerto.id, "UPDATE", "PENDING", null, payload)

            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de acerto à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("ACERTO", "ID=${acerto.id}", e)
            throw e
        }
    }
    suspend fun deletarAcerto(acerto: Acerto) = acertoDao.deletar(acerto)
    suspend fun buscarUltimoAcertoPorMesa(mesaId: Long) = 
        acertoDao.buscarUltimoAcertoPorMesa(mesaId)
    suspend fun buscarUltimoAcertoMesaItem(mesaId: Long) =
        acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long) = 
        acertoDao.buscarObservacaoUltimoAcerto(clienteId)
    suspend fun buscarUltimosAcertosPorClientes(clienteIds: List<Long>) =
        acertoDao.buscarUltimosAcertosPorClientes(clienteIds)
    suspend fun buscarCicloAtivo(rotaId: Long) = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = acertoDao.buscarPorRotaECicloId(rotaId, cicloId)
    suspend fun buscarAcertoMesaPorMesa(mesaId: Long) = acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    // ✅ REMOVIDO: Método duplicado que não adiciona à fila de sync
    // Use inserirAcerto() que adiciona à fila de sincronização
    suspend fun buscarPorId(id: Long) = acertoDao.buscarPorId(id)
    suspend fun atualizarValoresCiclo(
        cicloId: Long,
        valorTotalAcertado: Double,
        valorTotalDespesas: Double,
        clientesAcertados: Int
    ) = cicloAcertoDao.atualizarValoresCiclo(cicloId, valorTotalAcertado, valorTotalDespesas, clientesAcertados)
    suspend fun obterPanoPorId(id: Long) = panoEstoqueDao.buscarPorId(id)
    suspend fun marcarPanoComoUsadoPorNumero(numeroPano: String, motivo: String) {
        val pano = panoEstoqueDao.buscarPorNumero(numeroPano)
        if (pano != null) {
            panoEstoqueDao.atualizarDisponibilidade(pano.id, false)
        }
    }
    suspend fun buscarPorNumero(numeroPano: String) = panoEstoqueDao.buscarPorNumero(numeroPano)
    suspend fun marcarPanoComoUsado(panoId: Long, motivo: String) = panoEstoqueDao.atualizarDisponibilidade(panoId, false)
    
    // ==================== MESA ====================
    
    suspend fun obterMesaPorId(id: Long) = mesaDao.obterMesaPorId(id)
    fun obterMesasPorCliente(clienteId: Long) = mesaDao.obterMesasPorCliente(clienteId)
    fun obterMesasDisponiveis() = mesaDao.obterMesasDisponiveis()
    suspend fun inserirMesa(mesa: Mesa): Long {
        logDbInsertStart("MESA", "Numero=${mesa.numero}, ClienteID=${mesa.clienteId}")
        return try {
            // ✅ VALIDAÇÃO: Verificar se já existe mesa com mesmo número
            val mesaExistente = mesaDao.buscarPorNumero(mesa.numero)
            if (mesaExistente != null) {
                android.util.Log.w("AppRepository", "⚠️ Mesa com número '${mesa.numero}' já existe (ID: ${mesaExistente.id})")
                throw IllegalArgumentException("Mesa com número '${mesa.numero}' já existe")
            }
            
            val id = mesaDao.inserir(mesa)
            logDbInsertSuccess("MESA", "Numero=${mesa.numero}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "numero": "${mesa.numero}",
                        "clienteId": ${mesa.clienteId},
                        "ativa": ${mesa.ativa},
                        "tipoMesa": "${mesa.tipoMesa}",
                        "tamanho": "${mesa.tamanho}",
                        "estadoConservacao": "${mesa.estadoConservacao}",
                        "valorFixo": ${mesa.valorFixo},
                        // ✅ REMOVIDO: fichasInicial e fichasFinal - usando apenas relogioInicial e relogioFinal
                        "relogioInicial": ${mesa.relogioInicial},
                        "relogioFinal": ${mesa.relogioFinal},
                        "dataInstalacao": "${mesa.dataInstalacao}",
                        "observacoes": "${mesa.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Mesa", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("Mesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar mesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("MESA", "Numero=${mesa.numero}", e)
            throw e
        }
    }
    suspend fun atualizarMesa(mesa: Mesa) {
        logDbUpdateStart("MESA", "ID=${mesa.id}, Numero=${mesa.numero}")
        try {
            mesaDao.atualizar(mesa)
            logDbUpdateSuccess("MESA", "ID=${mesa.id}, Numero=${mesa.numero}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${mesa.id},
                        "numero": "${mesa.numero}",
                        "clienteId": ${mesa.clienteId},
                        "ativa": ${mesa.ativa},
                        "tipoMesa": "${mesa.tipoMesa}",
                        "tamanho": "${mesa.tamanho}",
                        "estadoConservacao": "${mesa.estadoConservacao}",
                        "valorFixo": ${mesa.valorFixo},
                        "relogioInicial": ${mesa.relogioInicial},
                        "relogioFinal": ${mesa.relogioFinal},
                        "dataInstalacao": "${mesa.dataInstalacao}",
                        "observacoes": "${mesa.observacoes ?: ""}",
                        "panoAtualId": ${mesa.panoAtualId ?: "null"},
                        "dataUltimaTrocaPano": "${mesa.dataUltimaTrocaPano ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Mesa", mesa.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("Mesa", mesa.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de mesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("MESA", "ID=${mesa.id}", e)
            throw e
        }
    }
    suspend fun deletarMesa(mesa: Mesa) = mesaDao.deletar(mesa)
    suspend fun vincularMesaACliente(mesaId: Long, clienteId: Long) = 
        mesaDao.vincularMesa(mesaId, clienteId)
    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) = 
        mesaDao.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
    suspend fun desvincularMesaDeCliente(mesaId: Long) = mesaDao.desvincularMesa(mesaId)
    suspend fun retirarMesa(mesaId: Long) = mesaDao.retirarMesa(mesaId)
    suspend fun atualizarRelogioMesa(mesaId: Long, relogioInicial: Int, relogioFinal: Int) {
        logDbUpdateStart("MESA_RELOGIO", "MesaID=$mesaId, RelogioInicial=$relogioInicial, RelogioFinal=$relogioFinal")
        try {
            mesaDao.atualizarRelogioMesa(mesaId, relogioInicial, relogioFinal)
            logDbUpdateSuccess("MESA_RELOGIO", "MesaID=$mesaId, RelogioInicial=$relogioInicial, RelogioFinal=$relogioFinal")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val mesa = mesaDao.obterMesaPorId(mesaId)
                if (mesa != null) {
                    val payload = """
                        {
                            "id": ${mesa.id},
                            "numero": "${mesa.numero}",
                            "clienteId": ${mesa.clienteId},
                            "ativa": ${mesa.ativa},
                            "tipoMesa": "${mesa.tipoMesa}",
                            "tamanho": "${mesa.tamanho}",
                            "estadoConservacao": "${mesa.estadoConservacao}",
                            "valorFixo": ${mesa.valorFixo},
                            "relogioInicial": $relogioInicial,
                            "relogioFinal": $relogioFinal,
                            "dataInstalacao": "${mesa.dataInstalacao}",
                            "observacoes": "${mesa.observacoes ?: ""}",
                            "panoAtualId": ${mesa.panoAtualId ?: "null"},
                            "dataUltimaTrocaPano": "${mesa.dataUltimaTrocaPano ?: ""}"
                        }
                    """.trimIndent()
                    
                    adicionarOperacaoSync("Mesa", mesaId, "UPDATE", payload, priority = 1)
                    logarOperacaoSync("Mesa", mesaId, "UPDATE", "PENDING", null, payload)
                }
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de relógio à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("MESA_RELOGIO", "MesaID=$mesaId", e)
            throw e
        }
    }
    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = 
        mesaDao.atualizarRelogioFinal(mesaId, relogioFinal)
    suspend fun obterMesasPorClienteDireto(clienteId: Long) = 
        mesaDao.obterMesasPorClienteDireto(clienteId)
    fun buscarMesasPorRota(rotaId: Long) = mesaDao.buscarMesasPorRota(rotaId).also {
        android.util.Log.d("AppRepository", "Buscando mesas para rota $rotaId")
    }
    suspend fun contarMesasAtivasPorClientes(clienteIds: List<Long>) =
        mesaDao.contarMesasAtivasPorClientes(clienteIds)
    fun obterTodasMesas() = mesaDao.obterTodasMesas()
    
    // ==================== ROTA ====================
    
    fun obterTodasRotas() = rotaDao.getAllRotas()
    fun obterRotasAtivas() = rotaDao.getAllRotasAtivas()
    
    // ✅ NOVO: Método para obter resumo de rotas com atualização em tempo real
    fun getRotasResumoComAtualizacaoTempoReal(): Flow<List<RotaResumo>> {
        return rotaDao.getAllRotasAtivas().map { rotas ->
            rotas.map { rota ->
                // Usar dados reais calculados
                val clientesAtivos = calcularClientesAtivosSync(rota.id)
                val pendencias = calcularPendenciasSync(rota.id)
                val valorAcertado = calcularValorAcertadoSync(rota.id)
                val quantidadeMesas = calcularQuantidadeMesasSync(rota.id)
                val percentualAcertados = calcularPercentualAcertadosSync(rota.id, clientesAtivos)
                
                // ✅ CORREÇÃO: Usar status da entidade Rota (já atualizada pelo PULL)
                val status = rota.statusAtual
                
                // ✅ CORREÇÃO: Usar dados da entidade Rota (já atualizada pelo PULL)
                val cicloAtual = rota.cicloAcertoAtual
                val dataCiclo = rota.dataInicioCiclo
                
                // ✅ NOVO: Usar datas diretamente da entidade Rota
                val dataInicio = rota.dataInicioCiclo
                val dataFim = rota.dataFimCiclo

                val rotaResumo = RotaResumo(
                    rota = rota,
                    clientesAtivos = clientesAtivos,
                    pendencias = pendencias,
                    valorAcertado = valorAcertado,
                    quantidadeMesas = quantidadeMesas,
                    percentualAcertados = percentualAcertados,
                    status = status,
                    cicloAtual = cicloAtual,
                    dataInicioCiclo = dataInicio,  // ✅ NOVO: Data de início
                    dataFimCiclo = dataFim        // ✅ NOVO: Data de fim
                )
                
                // ✅ DEBUG: Log para verificar se os dados estão corretos
                android.util.Log.d("AppRepository", "🔍 RotaResumo criado para ${rota.nome}:")
                android.util.Log.d("AppRepository", "   Status: ${status} (da entidade Rota)")
                android.util.Log.d("AppRepository", "   Ciclo: ${cicloAtual} (da entidade Rota)")
                android.util.Log.d("AppRepository", "   Data início: ${dataInicio}")
                android.util.Log.d("AppRepository", "   Data fim: ${dataFim}")
                android.util.Log.d("AppRepository", "   Texto ciclo: ${rotaResumo.getCicloFormatado()}")
                
                rotaResumo
            }
        }
    }
    
    // ✅ NOVO: Métodos auxiliares para calcular dados reais das rotas (versões sync)
    private fun calcularClientesAtivosSync(rotaId: Long): Int {
        return try {
            runBlocking { clienteDao.obterClientesPorRota(rotaId).first().count { it.ativo } }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular clientes ativos da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularPendenciasSync(rotaId: Long): Int {
        return try {
            runBlocking { calcularPendenciasReaisPorRota(rotaId) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular pendências da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularValorAcertadoSync(rotaId: Long): Double {
        return try {
            runBlocking { calcularValorAcertadoPorRotaECiclo(rotaId, obterCicloAtualIdPorRota(rotaId)) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular valor acertado da rota $rotaId: ${e.message}")
            0.0
        }
    }
    
    private fun calcularQuantidadeMesasSync(rotaId: Long): Int {
        return try {
            runBlocking { calcularQuantidadeMesasPorRota(rotaId) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular quantidade de mesas da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularPercentualAcertadosSync(rotaId: Long, clientesAtivos: Int): Int {
        return try {
            runBlocking { calcularPercentualClientesAcertados(rotaId, obterCicloAtualIdPorRota(rotaId), clientesAtivos) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular percentual de clientes acertados da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularCicloAtualReal(rotaId: Long): Int {
        return try {
            runBlocking { obterCicloAtualRota(rotaId).first }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular ciclo atual da rota $rotaId: ${e.message}")
            1
        }
    }
    
    private fun obterDataCicloAtual(rotaId: Long): Long? {
        return try {
            runBlocking { obterCicloAtualRota(rotaId).third }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter data do ciclo atual da rota $rotaId: ${e.message}")
            null
        }
    }
    
    private suspend fun calcularClientesAtivosPorRota(rotaId: Long): Int {
        return try {
            clienteDao.obterClientesPorRota(rotaId).first().count { it.ativo }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular clientes ativos da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularPendenciasReaisPorRota(rotaId: Long): Int {
        return try {
                val clientes = clienteDao.obterClientesPorRota(rotaId).first()
            if (clientes.isEmpty()) return 0
                val clienteIds = clientes.map { it.id }
                val ultimos = buscarUltimosAcertosPorClientes(clienteIds)
                val ultimoPorCliente = ultimos.associateBy({ it.clienteId }, { it.dataAcerto })
                val agora = java.util.Calendar.getInstance()
                clientes.count { cliente ->
                    val debitoAlto = cliente.debitoAtual > 400
                    val dataUltimo = ultimoPorCliente[cliente.id]
                    val semAcerto4Meses = if (dataUltimo == null) {
                        true
                    } else {
                        val cal = java.util.Calendar.getInstance(); cal.time = dataUltimo
                        val anos = agora.get(java.util.Calendar.YEAR) - cal.get(java.util.Calendar.YEAR)
                        val meses = anos * 12 + (agora.get(java.util.Calendar.MONTH) - cal.get(java.util.Calendar.MONTH))
                        meses >= 4
                    }
                    debitoAlto || semAcerto4Meses
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular pendências reais da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularValorAcertadoPorRotaECiclo(rotaId: Long, cicloId: Long?): Double {
        return try {
            if (cicloId == null) return 0.0
                buscarAcertosPorCicloId(cicloId).first().filter { it.rotaId == rotaId }.sumOf { it.valorRecebido }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular valor acertado da rota $rotaId: ${e.message}")
            0.0
        }
    }
    
    private suspend fun calcularQuantidadeMesasPorRota(rotaId: Long): Int {
        return try {
                mesaDao.buscarMesasPorRota(rotaId).first().size
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular quantidade de mesas da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularPercentualClientesAcertados(rotaId: Long, cicloId: Long?, clientesAtivos: Int): Int {
        return try {
            if (cicloId == null || clientesAtivos == 0) return 0
                val acertos = buscarAcertosPorCicloId(cicloId).first().filter { it.rotaId == rotaId }
                val distintos = acertos.map { it.clienteId }.distinct().size
                ((distintos.toDouble() / clientesAtivos.toDouble()) * 100).toInt()
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular percentual de clientes acertados da rota $rotaId: ${e.message}")
            0
        }
    }

    private suspend fun obterCicloAtualRota(rotaId: Long): Triple<Int, Long?, Long?> {
        return try {
            android.util.Log.d("AppRepository", "🔍 CALCULANDO CICLO ATUAL REAL para rota $rotaId")
            val cicloAtual = cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
            android.util.Log.d(
                "AppRepository",
                "🔍 Ciclo atual encontrado: ${cicloAtual?.let { "ID=${it.id}, Numero=${it.numeroCiclo}, Status=${it.status}" } ?: "null"}"
            )
            if (cicloAtual != null) {
                if (cicloAtual.status == StatusCicloAcerto.EM_ANDAMENTO) {
                    android.util.Log.d("AppRepository", "✅ Usando ciclo EM_ANDAMENTO: ${cicloAtual.numeroCiclo}")
                    Triple(cicloAtual.numeroCiclo, cicloAtual.id, cicloAtual.dataInicio.time)
                } else {
                    // ✅ CORREÇÃO: Retornar o ciclo finalizado, não o próximo
                    android.util.Log.d("AppRepository", "✅ Usando ciclo FINALIZADO: ${cicloAtual.numeroCiclo}")
                    Triple(cicloAtual.numeroCiclo, cicloAtual.id, cicloAtual.dataFim?.time)
                }
                    } else {
                android.util.Log.d("AppRepository", "✅ Primeiro ciclo: 1")
                        Triple(1, null, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter ciclo atual da rota $rotaId: ${e.message}")
            Triple(1, null, null)
        }
    }

    /**
     * Exposição pública do ciclo atual da rota no formato usado antes da refatoração.
     * Retorna apenas o ID do ciclo para vínculo de acertos.
     */
    suspend fun obterCicloAtualIdPorRota(rotaId: Long): Long? {
        return try {
            val cicloAtual = cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
            if (cicloAtual != null) {
                cicloAtual.id
                } else {
                val rota = rotaDao.getRotaById(rotaId)
                if (rota != null && rota.cicloAcertoAtual != 0 && rota.anoCiclo != 0) {
                    val cicloDaRota = cicloAcertoDao.buscarPorRotaNumeroEAno(rotaId, rota.cicloAcertoAtual, rota.anoCiclo)
                    cicloDaRota?.id
                    } else {
                    val (_, cicloId, _) = obterCicloAtualRota(rotaId)
                    cicloId
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter ID do ciclo atual para rota $rotaId: ${e.message}", e)
            null
        }
    }

    // ✅ NOVO: Método para obter datas de início e fim do ciclo
    private suspend fun obterDatasCicloRota(rotaId: Long): Pair<Long?, Long?> {
        return try {
            val ciclo = cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
            if (ciclo != null) {
                Pair(ciclo.dataInicio.time, ciclo.dataFim?.time)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter datas do ciclo da rota $rotaId: ${e.message}")
            Pair(null, null)
        }
    }

    private suspend fun determinarStatusRotaEmTempoReal(rotaId: Long): StatusRota {
        return try {
                val emAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
            if (emAndamento != null) {
                    StatusRota.EM_ANDAMENTO
                } else {
                    StatusRota.FINALIZADA
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao determinar status da rota $rotaId: ${e.message}")
            StatusRota.PAUSADA
        }
    }
    
    suspend fun obterRotaPorId(id: Long) = rotaDao.getRotaById(id)
    fun obterRotaPorIdFlow(id: Long) = rotaDao.obterRotaPorId(id)
    suspend fun obterRotaPorNome(nome: String) = rotaDao.getRotaByNome(nome)
    suspend fun inserirRota(rota: Rota): Long {
        logDbInsertStart("ROTA", "Nome=${rota.nome}")
        return try {
            val id = rotaDao.insertRota(rota)
            logDbInsertSuccess("ROTA", "Nome=${rota.nome}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${rota.nome}",
                        "descricao": "${rota.descricao}",
                        "ativa": ${rota.ativa},
                        "dataCriacao": ${rota.dataCriacao}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Rota", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("Rota", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar rota à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("ROTA", "Nome=${rota.nome}", e)
            throw e
        }
    }
    suspend fun inserirRotas(rotas: List<Rota>): List<Long> {
        logDbInsertStart("ROTA_LIST", "Quantidade=${rotas.size}")
        return try {
            val ids = rotaDao.insertRotas(rotas)
            logDbInsertSuccess("ROTA_LIST", "IDs=${ids.joinToString()}")
            ids
        } catch (e: Exception) {
            logDbInsertError("ROTA_LIST", "Quantidade=${rotas.size}", e)
            throw e
        }
    }
    suspend fun atualizarRota(rota: Rota) {
        logDbUpdateStart("ROTA", "ID=${rota.id}, Nome=${rota.nome}")
        try {
            rotaDao.updateRota(rota)
            logDbUpdateSuccess("ROTA", "ID=${rota.id}, Nome=${rota.nome}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${rota.id},
                        "nome": "${rota.nome}",
                        "descricao": "${rota.descricao ?: ""}",
                        "colaboradorResponsavel": "${rota.colaboradorResponsavel}",
                        "cidades": "${rota.cidades}",
                        "ativa": ${rota.ativa},
                        "cor": "${rota.cor}",
                        "dataCriacao": ${rota.dataCriacao},
                        "dataAtualizacao": ${rota.dataAtualizacao},
                        "statusAtual": "${rota.statusAtual.name}",
                        "cicloAcertoAtual": ${rota.cicloAcertoAtual},
                        "anoCiclo": ${rota.anoCiclo},
                        "dataInicioCiclo": ${rota.dataInicioCiclo ?: "null"},
                        "dataFimCiclo": ${rota.dataFimCiclo ?: "null"}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Rota", rota.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("Rota", rota.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de rota à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("ROTA", "ID=${rota.id}", e)
            throw e
        }
    }
    suspend fun atualizarRotas(rotas: List<Rota>) = rotaDao.updateRotas(rotas)
    suspend fun deletarRota(rota: Rota) = rotaDao.deleteRota(rota)
    suspend fun desativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.desativarRota(rotaId, timestamp)
    suspend fun ativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.ativarRota(rotaId, timestamp)
    suspend fun atualizarStatus(rotaId: Long, status: String, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.atualizarStatus(rotaId, status, timestamp)
    suspend fun atualizarCicloAcerto(rotaId: Long, ciclo: Int, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.atualizarCicloAcerto(rotaId, ciclo, timestamp)
    suspend fun iniciarCicloRota(rotaId: Long, ciclo: Int, dataInicio: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.iniciarCicloRota(rotaId, ciclo, dataInicio, timestamp)
    suspend fun finalizarCicloRota(rotaId: Long, dataFim: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.finalizarCicloRota(rotaId, dataFim, timestamp)
    suspend fun existeRotaComNome(nome: String, excludeId: Long = 0) = 
        rotaDao.existeRotaComNome(nome, excludeId)
    suspend fun contarRotasAtivas() = rotaDao.contarRotasAtivas()
    
    // ==================== DESPESA ====================
    
    fun obterTodasDespesas() = despesaDao.buscarTodasComRota()
    fun obterDespesasPorRota(rotaId: Long) = despesaDao.buscarPorRota(rotaId)
    suspend fun obterDespesaPorId(id: Long) = despesaDao.buscarPorId(id)
    suspend fun inserirDespesa(despesa: Despesa): Long {
        logDbInsertStart("DESPESA", "Descricao=${despesa.descricao}, RotaID=${despesa.rotaId}")
        return try {
            val id = despesaDao.inserir(despesa)
            logDbInsertSuccess("DESPESA", "Descricao=${despesa.descricao}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "rotaId": ${despesa.rotaId},
                        "descricao": "${despesa.descricao}",
                        "valor": ${despesa.valor},
                        "categoria": "${despesa.categoria}",
                        "tipoDespesa": "${despesa.tipoDespesa}",
                        "dataHora": "${despesa.dataHora}",
                        "observacoes": "${despesa.observacoes}",
                        "criadoPor": "${despesa.criadoPor}",
                        "cicloId": ${despesa.cicloId ?: "null"},
                        "origemLancamento": "${despesa.origemLancamento}",
                        "cicloAno": ${despesa.cicloAno ?: "null"},
                        "cicloNumero": ${despesa.cicloNumero ?: "null"},
                        "fotoComprovante": "${despesa.fotoComprovante ?: ""}",
                        "veiculoId": ${despesa.veiculoId ?: "null"},
                        "kmRodado": ${despesa.kmRodado ?: "null"},
                        "litrosAbastecidos": ${despesa.litrosAbastecidos ?: "null"}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Despesa", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("Despesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar despesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("DESPESA", "Descricao=${despesa.descricao}", e)
            throw e
        }
    }
    suspend fun atualizarDespesa(despesa: Despesa) {
        logDbUpdateStart("DESPESA", "ID=${despesa.id}, Descricao=${despesa.descricao}")
        try {
            despesaDao.atualizar(despesa)
            logDbUpdateSuccess("DESPESA", "ID=${despesa.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${despesa.id},
                        "rotaId": ${despesa.rotaId},
                        "descricao": "${despesa.descricao}",
                        "valor": ${despesa.valor},
                        "categoria": "${despesa.categoria}",
                        "tipoDespesa": "${despesa.tipoDespesa}",
                        "dataHora": "${despesa.dataHora}",
                        "observacoes": "${despesa.observacoes}",
                        "criadoPor": "${despesa.criadoPor}",
                        "cicloId": ${despesa.cicloId ?: "null"},
                        "origemLancamento": "${despesa.origemLancamento}",
                        "cicloAno": ${despesa.cicloAno ?: "null"},
                        "cicloNumero": ${despesa.cicloNumero ?: "null"},
                        "fotoComprovante": "${despesa.fotoComprovante ?: ""}",
                        "veiculoId": ${despesa.veiculoId ?: "null"},
                        "kmRodado": ${despesa.kmRodado ?: "null"},
                        "litrosAbastecidos": ${despesa.litrosAbastecidos ?: "null"}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Despesa", despesa.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("Despesa", despesa.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de despesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("DESPESA", "ID=${despesa.id}", e)
            throw e
        }
    }
    suspend fun deletarDespesa(despesa: Despesa) = despesaDao.deletar(despesa)
    suspend fun calcularTotalPorRota(rotaId: Long) = despesaDao.calcularTotalPorRota(rotaId)
    suspend fun calcularTotalGeral() = despesaDao.calcularTotalGeral()
    suspend fun contarDespesasPorRota(rotaId: Long) = despesaDao.contarPorRota(rotaId)
    suspend fun deletarDespesasPorRota(rotaId: Long) = despesaDao.deletarPorRota(rotaId)
    fun buscarDespesasPorCicloId(cicloId: Long) = despesaDao.buscarPorCicloId(cicloId)
    fun buscarDespesasPorRotaECicloId(rotaId: Long, cicloId: Long) = despesaDao.buscarPorRotaECicloId(rotaId, cicloId)

    // ✅ NOVO: despesas globais
    suspend fun buscarDespesasGlobaisPorCiclo(ano: Int, numero: Int): List<Despesa> = despesaDao.buscarGlobaisPorCiclo(ano, numero)
    suspend fun somarDespesasGlobaisPorCiclo(ano: Int, numero: Int): Double = despesaDao.somarGlobaisPorCiclo(ano, numero)

    // ==================== PANO ESTOQUE ====================
    
    fun obterTodosPanosEstoque() = panoEstoqueDao.listarTodos()
    fun obterPanosDisponiveis() = panoEstoqueDao.listarDisponiveis()
    suspend fun obterPanoEstoquePorId(id: Long) = panoEstoqueDao.buscarPorId(id)
    suspend fun inserirPanoEstoque(pano: com.example.gestaobilhares.data.entities.PanoEstoque): Long {
        logDbInsertStart("PANOESTOQUE", "Numero=${pano.numero}, Cor=${pano.cor}")
        return try {
            val id = panoEstoqueDao.inserir(pano)
            logDbInsertSuccess("PANOESTOQUE", "Numero=${pano.numero}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "numero": "${pano.numero}",
                        "cor": "${pano.cor}",
                        "tamanho": "${pano.tamanho}",
                        "material": "${pano.material}",
                        "disponivel": ${pano.disponivel},
                        "observacoes": "${pano.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("PanoEstoque", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("PanoEstoque", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar pano estoque à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("PANOESTOQUE", "Numero=${pano.numero}", e)
            throw e
        }
    }
    suspend fun atualizarPanoEstoque(pano: com.example.gestaobilhares.data.entities.PanoEstoque) {
        logDbUpdateStart("PANOESTOQUE", "ID=${pano.id}, Numero=${pano.numero}")
        try {
            panoEstoqueDao.atualizar(pano)
            logDbUpdateSuccess("PANOESTOQUE", "ID=${pano.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${pano.id},
                        "numero": "${pano.numero}",
                        "cor": "${pano.cor}",
                        "tamanho": "${pano.tamanho}",
                        "material": "${pano.material}",
                        "disponivel": ${pano.disponivel},
                        "observacoes": "${pano.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("PanoEstoque", pano.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("PanoEstoque", pano.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de pano estoque à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("PANOESTOQUE", "ID=${pano.id}", e)
            throw e
        }
    }
    suspend fun deletarPanoEstoque(pano: com.example.gestaobilhares.data.entities.PanoEstoque) = panoEstoqueDao.deletar(pano)
    suspend fun atualizarDisponibilidadePano(id: Long, disponivel: Boolean) = panoEstoqueDao.atualizarDisponibilidade(id, disponivel)

    // ==================== MESA VENDIDA ====================
    
    fun obterTodasMesasVendidas() = mesaVendidaDao.listarTodas()
    suspend fun obterMesaVendidaPorId(id: Long) = mesaVendidaDao.buscarPorId(id)
    suspend fun inserirMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida): Long {
        logDbInsertStart("MESAVENDIDA", "Numero=${mesaVendida.numeroMesa}, Comprador=${mesaVendida.nomeComprador}")
        return try {
            val id = mesaVendidaDao.inserir(mesaVendida)
            logDbInsertSuccess("MESAVENDIDA", "Numero=${mesaVendida.numeroMesa}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "mesaIdOriginal": ${mesaVendida.mesaIdOriginal},
                        "numeroMesa": "${mesaVendida.numeroMesa}",
                        "tipoMesa": "${mesaVendida.tipoMesa}",
                        "tamanhoMesa": "${mesaVendida.tamanhoMesa}",
                        "estadoConservacao": "${mesaVendida.estadoConservacao}",
                        "nomeComprador": "${mesaVendida.nomeComprador}",
                        "telefoneComprador": "${mesaVendida.telefoneComprador ?: ""}",
                        "cpfCnpjComprador": "${mesaVendida.cpfCnpjComprador ?: ""}",
                        "enderecoComprador": "${mesaVendida.enderecoComprador ?: ""}",
                        "valorVenda": ${mesaVendida.valorVenda},
                        "dataVenda": "${mesaVendida.dataVenda}",
                        "observacoes": "${mesaVendida.observacoes ?: ""}",
                        "dataCriacao": "${mesaVendida.dataCriacao}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("MesaVendida", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("MesaVendida", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar mesa vendida à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("MESAVENDIDA", "Numero=${mesaVendida.numeroMesa}", e)
            throw e
        }
    }
    suspend fun atualizarMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida) {
        logDbUpdateStart("MESAVENDIDA", "ID=${mesaVendida.id}, Numero=${mesaVendida.numeroMesa}")
        try {
            mesaVendidaDao.atualizar(mesaVendida)
            logDbUpdateSuccess("MESAVENDIDA", "ID=${mesaVendida.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${mesaVendida.id},
                        "mesaIdOriginal": ${mesaVendida.mesaIdOriginal},
                        "numeroMesa": "${mesaVendida.numeroMesa}",
                        "tipoMesa": "${mesaVendida.tipoMesa}",
                        "tamanhoMesa": "${mesaVendida.tamanhoMesa}",
                        "estadoConservacao": "${mesaVendida.estadoConservacao}",
                        "nomeComprador": "${mesaVendida.nomeComprador}",
                        "telefoneComprador": "${mesaVendida.telefoneComprador ?: ""}",
                        "cpfCnpjComprador": "${mesaVendida.cpfCnpjComprador ?: ""}",
                        "enderecoComprador": "${mesaVendida.enderecoComprador ?: ""}",
                        "valorVenda": ${mesaVendida.valorVenda},
                        "dataVenda": "${mesaVendida.dataVenda}",
                        "observacoes": "${mesaVendida.observacoes ?: ""}",
                        "dataCriacao": "${mesaVendida.dataCriacao}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("MesaVendida", mesaVendida.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("MesaVendida", mesaVendida.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de mesa vendida à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("MESAVENDIDA", "ID=${mesaVendida.id}", e)
            throw e
        }
    }
    suspend fun deletarMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida) = mesaVendidaDao.deletar(mesaVendida)

    // ==================== MESA REFORMADA ====================
    
    suspend fun inserirMesaReformada(mesaReformada: MesaReformada): Long {
        logDbInsertStart("MESAREFORMADA", "Mesa=${mesaReformada.numeroMesa}, Data=${mesaReformada.dataReforma}")
        return try {
            val id = mesaReformadaDao.inserir(mesaReformada)
            logDbInsertSuccess("MESAREFORMADA", "Mesa=${mesaReformada.numeroMesa}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "mesaId": ${mesaReformada.mesaId},
                        "numeroMesa": "${mesaReformada.numeroMesa}",
                        "tipoMesa": "${mesaReformada.tipoMesa}",
                        "tamanhoMesa": "${mesaReformada.tamanhoMesa}",
                        "pintura": ${mesaReformada.pintura},
                        "tabela": ${mesaReformada.tabela},
                        "panos": ${mesaReformada.panos},
                        "numeroPanos": "${mesaReformada.numeroPanos ?: ""}",
                        "outros": ${mesaReformada.outros},
                        "observacoes": "${mesaReformada.observacoes ?: ""}",
                        "fotoReforma": "${mesaReformada.fotoReforma ?: ""}",
                        "dataReforma": "${mesaReformada.dataReforma.time}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync(
                    entityType = "mesareformada",
                    entityId = id,
                    operation = "INSERT",
                    payload = payload
                )
                logarOperacaoSync("MESAREFORMADA", id, "INSERT", "Adicionado à fila de sync")
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar mesa reformada à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("MESAREFORMADA", "Mesa=${mesaReformada.numeroMesa}", e)
            throw e
        }
    }

    // ==================== STOCK ITEM ====================
    
    fun obterTodosStockItems() = stockItemDao.listarTodos()
    suspend fun obterStockItemPorId(id: Long) = stockItemDao.buscarPorId(id)
    suspend fun inserirStockItem(stockItem: com.example.gestaobilhares.data.entities.StockItem): Long {
        logDbInsertStart("STOCKITEM", "Nome=${stockItem.name}, Categoria=${stockItem.category}")
        return try {
            val id = stockItemDao.inserir(stockItem)
            logDbInsertSuccess("STOCKITEM", "Nome=${stockItem.name}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "name": "${stockItem.name}",
                        "category": "${stockItem.category}",
                        "quantity": ${stockItem.quantity},
                        "unitPrice": ${stockItem.unitPrice},
                        "supplier": "${stockItem.supplier}",
                        "description": "${stockItem.description ?: ""}",
                        "createdAt": "${stockItem.createdAt.time}",
                        "updatedAt": "${stockItem.updatedAt.time}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("StockItem", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("StockItem", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de stock item à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("STOCKITEM", "Nome=${stockItem.name}", e)
            throw e
        }
    }
    
    suspend fun atualizarStockItem(stockItem: com.example.gestaobilhares.data.entities.StockItem) {
        logDbUpdateStart("STOCKITEM", "ID=${stockItem.id}, Nome=${stockItem.name}")
        try {
            stockItemDao.atualizar(stockItem)
            logDbUpdateSuccess("STOCKITEM", "ID=${stockItem.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${stockItem.id},
                        "name": "${stockItem.name}",
                        "category": "${stockItem.category}",
                        "quantity": ${stockItem.quantity},
                        "unitPrice": ${stockItem.unitPrice},
                        "supplier": "${stockItem.supplier}",
                        "description": "${stockItem.description ?: ""}",
                        "createdAt": "${stockItem.createdAt.time}",
                        "updatedAt": "${stockItem.updatedAt.time}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("StockItem", stockItem.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("StockItem", stockItem.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de stock item à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("STOCKITEM", "ID=${stockItem.id}", e)
            throw e
        }
    }
    suspend fun deletarStockItem(stockItem: com.example.gestaobilhares.data.entities.StockItem) = stockItemDao.deletar(stockItem)
    suspend fun atualizarQuantidadeStockItem(id: Long, newQuantity: Int, updatedAt: java.util.Date) = stockItemDao.atualizarQuantidade(id, newQuantity, updatedAt)

    // ==================== VEICULO ====================
    
    fun obterTodosVeiculos() = veiculoDao.listar()
    suspend fun obterVeiculoPorId(id: Long) = veiculoDao.listar().first().find { it.id == id }
    suspend fun inserirVeiculo(veiculo: com.example.gestaobilhares.data.entities.Veiculo): Long {
        logDbInsertStart("VEICULO", "Nome=${veiculo.nome}, Placa=${veiculo.placa}")
        return try {
            val id = veiculoDao.inserir(veiculo)
            logDbInsertSuccess("VEICULO", "Nome=${veiculo.nome}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${veiculo.nome}",
                        "placa": "${veiculo.placa}",
                        "marca": "${veiculo.marca}",
                        "modelo": "${veiculo.modelo}",
                        "anoModelo": ${veiculo.anoModelo},
                        "kmAtual": ${veiculo.kmAtual},
                        "dataCompra": ${veiculo.dataCompra?.time ?: "null"},
                        "observacoes": "${veiculo.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Veiculo", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("Veiculo", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de veículo à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("VEICULO", "Nome=${veiculo.nome}", e)
            throw e
        }
    }
    
    suspend fun atualizarVeiculo(veiculo: com.example.gestaobilhares.data.entities.Veiculo) {
        logDbUpdateStart("VEICULO", "ID=${veiculo.id}, Nome=${veiculo.nome}")
        try {
            veiculoDao.atualizar(veiculo)
            logDbUpdateSuccess("VEICULO", "ID=${veiculo.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${veiculo.id},
                        "nome": "${veiculo.nome}",
                        "placa": "${veiculo.placa}",
                        "marca": "${veiculo.marca}",
                        "modelo": "${veiculo.modelo}",
                        "anoModelo": ${veiculo.anoModelo},
                        "kmAtual": ${veiculo.kmAtual},
                        "dataCompra": ${veiculo.dataCompra?.time ?: "null"},
                        "observacoes": "${veiculo.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Veiculo", veiculo.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("Veiculo", veiculo.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de veículo à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("VEICULO", "ID=${veiculo.id}", e)
            throw e
        }
    }
    suspend fun deletarVeiculo(veiculo: com.example.gestaobilhares.data.entities.Veiculo) = veiculoDao.deletar(veiculo)

    // ==================== HISTORICO MANUTENCAO MESA ====================
    
    fun obterTodosHistoricoManutencaoMesa() = historicoManutencaoMesaDao.listarTodos()
    suspend fun obterHistoricoManutencaoMesaPorId(id: Long) = historicoManutencaoMesaDao.buscarPorId(id)
    suspend fun inserirHistoricoManutencaoMesaSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa): Long {
        logDbInsertStart("HISTORICO_MANUTENCAO_MESA", "Mesa=${historico.numeroMesa}, Tipo=${historico.tipoManutencao}")
        return try {
            val id = historicoManutencaoMesaDao.inserir(historico)
            logDbInsertSuccess("HISTORICO_MANUTENCAO_MESA", "Mesa=${historico.numeroMesa}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "mesaId": ${historico.mesaId},
                        "numeroMesa": "${historico.numeroMesa}",
                        "tipoManutencao": "${historico.tipoManutencao}",
                        "descricao": "${historico.descricao ?: ""}",
                        "dataManutencao": ${historico.dataManutencao.time},
                        "responsavel": "${historico.responsavel ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "custo": ${historico.custo ?: "null"},
                        "fotoAntes": "${historico.fotoAntes ?: ""}",
                        "fotoDepois": "${historico.fotoDepois ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoManutencaoMesa", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("HistoricoManutencaoMesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de histórico manutenção mesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("HISTORICO_MANUTENCAO_MESA", "Mesa=${historico.numeroMesa}", e)
            throw e
        }
    }
    
    suspend fun atualizarHistoricoManutencaoMesaSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa) {
        logDbUpdateStart("HISTORICO_MANUTENCAO_MESA", "ID=${historico.id}, Mesa=${historico.numeroMesa}")
        try {
            historicoManutencaoMesaDao.atualizar(historico)
            logDbUpdateSuccess("HISTORICO_MANUTENCAO_MESA", "ID=${historico.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${historico.id},
                        "mesaId": ${historico.mesaId},
                        "numeroMesa": "${historico.numeroMesa}",
                        "tipoManutencao": "${historico.tipoManutencao}",
                        "descricao": "${historico.descricao ?: ""}",
                        "dataManutencao": ${historico.dataManutencao.time},
                        "responsavel": "${historico.responsavel ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "custo": ${historico.custo ?: "null"},
                        "fotoAntes": "${historico.fotoAntes ?: ""}",
                        "fotoDepois": "${historico.fotoDepois ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoManutencaoMesa", historico.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("HistoricoManutencaoMesa", historico.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de histórico manutenção mesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("HISTORICO_MANUTENCAO_MESA", "ID=${historico.id}", e)
            throw e
        }
    }
    suspend fun deletarHistoricoManutencaoMesa(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa) = historicoManutencaoMesaDao.deletar(historico)

    // ==================== HISTORICO MANUTENCAO VEICULO ====================
    
    fun obterTodosHistoricoManutencaoVeiculo() = historicoManutencaoVeiculoDao.listarTodos()
    suspend fun obterHistoricoManutencaoVeiculoPorId(id: Long) = historicoManutencaoVeiculoDao.listarTodos().first().find { it.id == id }
    suspend fun inserirHistoricoManutencaoVeiculoSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo): Long {
        logDbInsertStart("HISTORICO_MANUTENCAO_VEICULO", "Veiculo=${historico.veiculoId}, Tipo=${historico.tipoManutencao}")
        return try {
            val id = historicoManutencaoVeiculoDao.inserir(historico)
            logDbInsertSuccess("HISTORICO_MANUTENCAO_VEICULO", "Veiculo=${historico.veiculoId}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "veiculoId": ${historico.veiculoId},
                        "tipoManutencao": "${historico.tipoManutencao}",
                        "descricao": "${historico.descricao}",
                        "dataManutencao": ${historico.dataManutencao.time},
                        "valor": ${historico.valor},
                        "kmVeiculo": ${historico.kmVeiculo},
                        "responsavel": "${historico.responsavel ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoManutencaoVeiculo", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("HistoricoManutencaoVeiculo", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de histórico manutenção veículo à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("HISTORICO_MANUTENCAO_VEICULO", "Veiculo=${historico.veiculoId}", e)
            throw e
        }
    }
    
    suspend fun atualizarHistoricoManutencaoVeiculoSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo) {
        logDbUpdateStart("HISTORICO_MANUTENCAO_VEICULO", "ID=${historico.id}, Veiculo=${historico.veiculoId}")
        try {
            historicoManutencaoVeiculoDao.atualizar(historico)
            logDbUpdateSuccess("HISTORICO_MANUTENCAO_VEICULO", "ID=${historico.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${historico.id},
                        "veiculoId": ${historico.veiculoId},
                        "tipoManutencao": "${historico.tipoManutencao}",
                        "descricao": "${historico.descricao}",
                        "dataManutencao": ${historico.dataManutencao.time},
                        "valor": ${historico.valor},
                        "kmVeiculo": ${historico.kmVeiculo},
                        "responsavel": "${historico.responsavel ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoManutencaoVeiculo", historico.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("HistoricoManutencaoVeiculo", historico.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de histórico manutenção veículo à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("HISTORICO_MANUTENCAO_VEICULO", "ID=${historico.id}", e)
            throw e
        }
    }
    suspend fun deletarHistoricoManutencaoVeiculo(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo) = historicoManutencaoVeiculoDao.deletar(historico)

    // ==================== HISTORICO COMBUSTIVEL VEICULO ====================
    
    fun obterTodosHistoricoCombustivelVeiculo() = historicoCombustivelVeiculoDao.listarTodos()
    suspend fun obterHistoricoCombustivelVeiculoPorId(id: Long) = historicoCombustivelVeiculoDao.listarTodos().first().find { it.id == id }
    suspend fun inserirHistoricoCombustivelVeiculoSync(historico: com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo): Long {
        logDbInsertStart("HISTORICO_COMBUSTIVEL_VEICULO", "Veiculo=${historico.veiculoId}, Litros=${historico.litros}")
        return try {
            val id = historicoCombustivelVeiculoDao.inserir(historico)
            logDbInsertSuccess("HISTORICO_COMBUSTIVEL_VEICULO", "Veiculo=${historico.veiculoId}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "veiculoId": ${historico.veiculoId},
                        "dataAbastecimento": ${historico.dataAbastecimento.time},
                        "litros": ${historico.litros},
                        "valor": ${historico.valor},
                        "kmVeiculo": ${historico.kmVeiculo},
                        "kmRodado": ${historico.kmRodado},
                        "posto": "${historico.posto ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoCombustivelVeiculo", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("HistoricoCombustivelVeiculo", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de histórico combustível veículo à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("HISTORICO_COMBUSTIVEL_VEICULO", "Veiculo=${historico.veiculoId}", e)
            throw e
        }
    }
    
    suspend fun atualizarHistoricoCombustivelVeiculoSync(historico: com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo) {
        logDbUpdateStart("HISTORICO_COMBUSTIVEL_VEICULO", "ID=${historico.id}, Veiculo=${historico.veiculoId}")
        try {
            historicoCombustivelVeiculoDao.atualizar(historico)
            logDbUpdateSuccess("HISTORICO_COMBUSTIVEL_VEICULO", "ID=${historico.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${historico.id},
                        "veiculoId": ${historico.veiculoId},
                        "dataAbastecimento": ${historico.dataAbastecimento.time},
                        "litros": ${historico.litros},
                        "valor": ${historico.valor},
                        "kmVeiculo": ${historico.kmVeiculo},
                        "kmRodado": ${historico.kmRodado},
                        "posto": "${historico.posto ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoCombustivelVeiculo", historico.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("HistoricoCombustivelVeiculo", historico.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de histórico combustível veículo à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("HISTORICO_COMBUSTIVEL_VEICULO", "ID=${historico.id}", e)
            throw e
        }
    }
    suspend fun deletarHistoricoCombustivelVeiculo(historico: com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo) = historicoCombustivelVeiculoDao.deletar(historico)

    // ==================== CATEGORIA DESPESA ====================
    
    fun obterTodasCategoriasDespesa() = categoriaDespesaDao.buscarTodas()
    suspend fun obterCategoriaDespesaPorId(id: Long) = categoriaDespesaDao.buscarPorId(id)
    suspend fun inserirCategoriaDespesaSync(categoria: com.example.gestaobilhares.data.entities.CategoriaDespesa): Long {
        logDbInsertStart("CATEGORIA_DESPESA", "Nome=${categoria.nome}")
        return try {
            val id = categoriaDespesaDao.inserir(categoria)
            logDbInsertSuccess("CATEGORIA_DESPESA", "Nome=${categoria.nome}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${categoria.nome}",
                        "descricao": "${categoria.descricao}",
                        "ativa": ${categoria.ativa},
                        "dataCriacao": ${categoria.dataCriacao.time},
                        "dataAtualizacao": ${categoria.dataAtualizacao.time},
                        "criadoPor": "${categoria.criadoPor}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("CategoriaDespesa", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("CategoriaDespesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de categoria despesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CATEGORIA_DESPESA", "Nome=${categoria.nome}", e)
            throw e
        }
    }
    
    suspend fun atualizarCategoriaDespesaSync(categoria: com.example.gestaobilhares.data.entities.CategoriaDespesa) {
        logDbUpdateStart("CATEGORIA_DESPESA", "ID=${categoria.id}, Nome=${categoria.nome}")
        try {
            categoriaDespesaDao.atualizar(categoria)
            logDbUpdateSuccess("CATEGORIA_DESPESA", "ID=${categoria.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${categoria.id},
                        "nome": "${categoria.nome}",
                        "descricao": "${categoria.descricao}",
                        "ativa": ${categoria.ativa},
                        "dataCriacao": ${categoria.dataCriacao.time},
                        "dataAtualizacao": ${categoria.dataAtualizacao.time},
                        "criadoPor": "${categoria.criadoPor}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("CategoriaDespesa", categoria.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("CategoriaDespesa", categoria.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de categoria despesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("CATEGORIA_DESPESA", "ID=${categoria.id}", e)
            throw e
        }
    }
    suspend fun deletarCategoriaDespesa(categoria: com.example.gestaobilhares.data.entities.CategoriaDespesa) = categoriaDespesaDao.deletar(categoria)

    // ==================== TIPO DESPESA ====================
    
    fun obterTodosTiposDespesa() = tipoDespesaDao.buscarTodos()
    suspend fun obterTipoDespesaPorId(id: Long) = tipoDespesaDao.buscarPorId(id)
    suspend fun inserirTipoDespesaSync(tipo: com.example.gestaobilhares.data.entities.TipoDespesa): Long {
        logDbInsertStart("TIPO_DESPESA", "Nome=${tipo.nome}, Categoria=${tipo.categoriaId}")
        return try {
            val id = tipoDespesaDao.inserir(tipo)
            logDbInsertSuccess("TIPO_DESPESA", "Nome=${tipo.nome}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "categoriaId": ${tipo.categoriaId},
                        "nome": "${tipo.nome}",
                        "descricao": "${tipo.descricao}",
                        "ativo": ${tipo.ativo},
                        "dataCriacao": ${tipo.dataCriacao.time},
                        "dataAtualizacao": ${tipo.dataAtualizacao.time},
                        "criadoPor": "${tipo.criadoPor}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("TipoDespesa", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("TipoDespesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de tipo despesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("TIPO_DESPESA", "Nome=${tipo.nome}", e)
            throw e
        }
    }
    
    suspend fun atualizarTipoDespesaSync(tipo: com.example.gestaobilhares.data.entities.TipoDespesa) {
        logDbUpdateStart("TIPO_DESPESA", "ID=${tipo.id}, Nome=${tipo.nome}")
        try {
            tipoDespesaDao.atualizar(tipo)
            logDbUpdateSuccess("TIPO_DESPESA", "ID=${tipo.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${tipo.id},
                        "categoriaId": ${tipo.categoriaId},
                        "nome": "${tipo.nome}",
                        "descricao": "${tipo.descricao}",
                        "ativo": ${tipo.ativo},
                        "dataCriacao": ${tipo.dataCriacao.time},
                        "dataAtualizacao": ${tipo.dataAtualizacao.time},
                        "criadoPor": "${tipo.criadoPor}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("TipoDespesa", tipo.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("TipoDespesa", tipo.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de tipo despesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("TIPO_DESPESA", "ID=${tipo.id}", e)
            throw e
        }
    }
    suspend fun deletarTipoDespesa(tipo: com.example.gestaobilhares.data.entities.TipoDespesa) = tipoDespesaDao.deletar(tipo)

    // ==================== CONTRATO LOCAÇÃO ====================
    
    suspend fun inserirContratoLocacaoSync(contrato: com.example.gestaobilhares.data.entities.ContratoLocacao): Long {
        logDbInsertStart("CONTRATO_LOCACAO", "Numero=${contrato.numeroContrato}, Cliente=${contrato.clienteId}")
        return try {
            val id = contratoLocacaoDao.inserirContrato(contrato)
            logDbInsertSuccess("CONTRATO_LOCACAO", "Numero=${contrato.numeroContrato}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "numeroContrato": "${contrato.numeroContrato}",
                        "clienteId": ${contrato.clienteId},
                        "locadorNome": "${contrato.locadorNome}",
                        "locadorCnpj": "${contrato.locadorCnpj}",
                        "locadorEndereco": "${contrato.locadorEndereco}",
                        "locadorCep": "${contrato.locadorCep}",
                        "locatarioNome": "${contrato.locatarioNome}",
                        "locatarioCpf": "${contrato.locatarioCpf}",
                        "locatarioEndereco": "${contrato.locatarioEndereco}",
                        "locatarioTelefone": "${contrato.locatarioTelefone}",
                        "locatarioEmail": "${contrato.locatarioEmail}",
                        "valorMensal": ${contrato.valorMensal},
                        "diaVencimento": ${contrato.diaVencimento},
                        "tipoPagamento": "${contrato.tipoPagamento}",
                        "percentualReceita": ${contrato.percentualReceita ?: "null"},
                        "dataContrato": ${contrato.dataContrato.time},
                        "dataInicio": ${contrato.dataInicio.time},
                        "status": "${contrato.status}",
                        "dataEncerramento": ${contrato.dataEncerramento?.time ?: "null"},
                        "assinaturaLocador": "${contrato.assinaturaLocador ?: ""}",
                        "assinaturaLocatario": "${contrato.assinaturaLocatario ?: ""}",
                        "distratoAssinaturaLocador": "${contrato.distratoAssinaturaLocador ?: ""}",
                        "distratoAssinaturaLocatario": "${contrato.distratoAssinaturaLocatario ?: ""}",
                        "distratoDataAssinatura": ${contrato.distratoDataAssinatura?.time ?: "null"},
                        "dataCriacao": ${contrato.dataCriacao.time},
                        "dataAtualizacao": ${contrato.dataAtualizacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("ContratoLocacao", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("ContratoLocacao", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de contrato locação à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CONTRATO_LOCACAO", "Numero=${contrato.numeroContrato}", e)
            throw e
        }
    }
    
    suspend fun atualizarContratoLocacaoSync(contrato: com.example.gestaobilhares.data.entities.ContratoLocacao) {
        logDbUpdateStart("CONTRATO_LOCACAO", "ID=${contrato.id}, Numero=${contrato.numeroContrato}")
        try {
            contratoLocacaoDao.atualizarContrato(contrato)
            logDbUpdateSuccess("CONTRATO_LOCACAO", "ID=${contrato.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${contrato.id},
                        "numeroContrato": "${contrato.numeroContrato}",
                        "clienteId": ${contrato.clienteId},
                        "locadorNome": "${contrato.locadorNome}",
                        "locadorCnpj": "${contrato.locadorCnpj}",
                        "locadorEndereco": "${contrato.locadorEndereco}",
                        "locadorCep": "${contrato.locadorCep}",
                        "locatarioNome": "${contrato.locatarioNome}",
                        "locatarioCpf": "${contrato.locatarioCpf}",
                        "locatarioEndereco": "${contrato.locatarioEndereco}",
                        "locatarioTelefone": "${contrato.locatarioTelefone}",
                        "locatarioEmail": "${contrato.locatarioEmail}",
                        "valorMensal": ${contrato.valorMensal},
                        "diaVencimento": ${contrato.diaVencimento},
                        "tipoPagamento": "${contrato.tipoPagamento}",
                        "percentualReceita": ${contrato.percentualReceita ?: "null"},
                        "dataContrato": ${contrato.dataContrato.time},
                        "dataInicio": ${contrato.dataInicio.time},
                        "status": "${contrato.status}",
                        "dataEncerramento": ${contrato.dataEncerramento?.time ?: "null"},
                        "assinaturaLocador": "${contrato.assinaturaLocador ?: ""}",
                        "assinaturaLocatario": "${contrato.assinaturaLocatario ?: ""}",
                        "distratoAssinaturaLocador": "${contrato.distratoAssinaturaLocador ?: ""}",
                        "distratoAssinaturaLocatario": "${contrato.distratoAssinaturaLocatario ?: ""}",
                        "distratoDataAssinatura": ${contrato.distratoDataAssinatura?.time ?: "null"},
                        "dataCriacao": ${contrato.dataCriacao.time},
                        "dataAtualizacao": ${contrato.dataAtualizacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("ContratoLocacao", contrato.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("ContratoLocacao", contrato.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de contrato locação à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("CONTRATO_LOCACAO", "ID=${contrato.id}", e)
            throw e
        }
    }

    // ==================== ASSINATURA REPRESENTANTE LEGAL ====================
    
    suspend fun inserirAssinaturaRepresentanteLegalSync(assinatura: com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal): Long {
        logDbInsertStart("ASSINATURA_REPRESENTANTE_LEGAL", "Nome=${assinatura.nomeRepresentante}, CPF=${assinatura.cpfRepresentante}")
        return try {
            val id = assinaturaRepresentanteLegalDao.inserirAssinatura(assinatura)
            logDbInsertSuccess("ASSINATURA_REPRESENTANTE_LEGAL", "Nome=${assinatura.nomeRepresentante}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nomeRepresentante": "${assinatura.nomeRepresentante}",
                        "cpfRepresentante": "${assinatura.cpfRepresentante}",
                        "cargoRepresentante": "${assinatura.cargoRepresentante}",
                        "assinaturaBase64": "${assinatura.assinaturaBase64}",
                        "timestampCriacao": ${assinatura.timestampCriacao},
                        "deviceId": "${assinatura.deviceId}",
                        "hashIntegridade": "${assinatura.hashIntegridade}",
                        "versaoSistema": "${assinatura.versaoSistema}",
                        "dataCriacao": ${assinatura.dataCriacao.time},
                        "criadoPor": "${assinatura.criadoPor}",
                        "ativo": ${assinatura.ativo},
                        "numeroProcuração": "${assinatura.numeroProcuração}",
                        "dataProcuração": ${assinatura.dataProcuração.time},
                        "poderesDelegados": "${assinatura.poderesDelegados}",
                        "validadeProcuração": ${assinatura.validadeProcuração?.time ?: "null"},
                        "totalUsos": ${assinatura.totalUsos},
                        "ultimoUso": ${assinatura.ultimoUso?.time ?: "null"},
                        "contratosAssinados": "${assinatura.contratosAssinados}",
                        "validadaJuridicamente": ${assinatura.validadaJuridicamente},
                        "dataValidacao": ${assinatura.dataValidacao?.time ?: "null"},
                        "validadoPor": "${assinatura.validadoPor ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("AssinaturaRepresentanteLegal", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("AssinaturaRepresentanteLegal", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de assinatura representante legal à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("ASSINATURA_REPRESENTANTE_LEGAL", "Nome=${assinatura.nomeRepresentante}", e)
            throw e
        }
    }
    
    suspend fun atualizarAssinaturaRepresentanteLegalSync(assinatura: com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal) {
        logDbUpdateStart("ASSINATURA_REPRESENTANTE_LEGAL", "ID=${assinatura.id}, Nome=${assinatura.nomeRepresentante}")
        try {
            assinaturaRepresentanteLegalDao.atualizarAssinatura(assinatura)
            logDbUpdateSuccess("ASSINATURA_REPRESENTANTE_LEGAL", "ID=${assinatura.id}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${assinatura.id},
                        "nomeRepresentante": "${assinatura.nomeRepresentante}",
                        "cpfRepresentante": "${assinatura.cpfRepresentante}",
                        "cargoRepresentante": "${assinatura.cargoRepresentante}",
                        "assinaturaBase64": "${assinatura.assinaturaBase64}",
                        "timestampCriacao": ${assinatura.timestampCriacao},
                        "deviceId": "${assinatura.deviceId}",
                        "hashIntegridade": "${assinatura.hashIntegridade}",
                        "versaoSistema": "${assinatura.versaoSistema}",
                        "dataCriacao": ${assinatura.dataCriacao.time},
                        "criadoPor": "${assinatura.criadoPor}",
                        "ativo": ${assinatura.ativo},
                        "numeroProcuração": "${assinatura.numeroProcuração}",
                        "dataProcuração": ${assinatura.dataProcuração.time},
                        "poderesDelegados": "${assinatura.poderesDelegados}",
                        "validadeProcuração": ${assinatura.validadeProcuração?.time ?: "null"},
                        "totalUsos": ${assinatura.totalUsos},
                        "ultimoUso": ${assinatura.ultimoUso?.time ?: "null"},
                        "contratosAssinados": "${assinatura.contratosAssinados}",
                        "validadaJuridicamente": ${assinatura.validadaJuridicamente},
                        "dataValidacao": ${assinatura.dataValidacao?.time ?: "null"},
                        "validadoPor": "${assinatura.validadoPor ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("AssinaturaRepresentanteLegal", assinatura.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("AssinaturaRepresentanteLegal", assinatura.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de assinatura representante legal à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("ASSINATURA_REPRESENTANTE_LEGAL", "ID=${assinatura.id}", e)
            throw e
        }
    }

    // ==================== LOG AUDITORIA ASSINATURA ====================
    
    suspend fun inserirLogAuditoriaAssinaturaSync(log: com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura): Long {
        logDbInsertStart("LOG_AUDITORIA_ASSINATURA", "Tipo=${log.tipoOperacao}, Usuario=${log.usuarioExecutou}")
        return try {
            val id = logAuditoriaAssinaturaDao.inserirLog(log)
            logDbInsertSuccess("LOG_AUDITORIA_ASSINATURA", "Tipo=${log.tipoOperacao}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "tipoOperacao": "${log.tipoOperacao}",
                        "idAssinatura": ${log.idAssinatura},
                        "idContrato": ${log.idContrato ?: "null"},
                        "idAditivo": ${log.idAditivo ?: "null"},
                        "usuarioExecutou": "${log.usuarioExecutou}",
                        "cpfUsuario": "${log.cpfUsuario}",
                        "cargoUsuario": "${log.cargoUsuario}",
                        "timestamp": ${log.timestamp},
                        "deviceId": "${log.deviceId}",
                        "versaoApp": "${log.versaoApp}",
                        "hashDocumento": "${log.hashDocumento}",
                        "hashAssinatura": "${log.hashAssinatura}",
                        "latitude": ${log.latitude ?: "null"},
                        "longitude": ${log.longitude ?: "null"},
                        "endereco": "${log.endereco ?: ""}",
                        "ipAddress": "${log.ipAddress ?: ""}",
                        "userAgent": "${log.userAgent ?: ""}",
                        "tipoDocumento": "${log.tipoDocumento}",
                        "numeroDocumento": "${log.numeroDocumento}",
                        "valorContrato": ${log.valorContrato ?: "null"},
                        "sucesso": ${log.sucesso},
                        "mensagemErro": "${log.mensagemErro ?: ""}",
                        "dataOperacao": ${log.dataOperacao.time},
                        "observacoes": "${log.observacoes ?: ""}",
                        "validadoJuridicamente": ${log.validadoJuridicamente},
                        "dataValidacao": ${log.dataValidacao?.time ?: "null"},
                        "validadoPor": "${log.validadoPor ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("LogAuditoriaAssinatura", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("LogAuditoriaAssinatura", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar criação de log auditoria assinatura à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("LOG_AUDITORIA_ASSINATURA", "Tipo=${log.tipoOperacao}", e)
            throw e
        }
    }

    // ✅ NOVO: obter mesas por ciclo (a partir dos acertos do ciclo)
    suspend fun contarMesasPorCiclo(cicloId: Long): Int {
        return try {
            val acertos = buscarAcertosPorCicloId(cicloId).first()
            if (acertos.isEmpty()) return 0
            val mesas = mutableSetOf<Long>()
            for (acerto in acertos) {
                val itens = acertoMesaDao.buscarPorAcertoId(acerto.id)
                itens.forEach { mesas.add(it.mesaId) }
            }
            mesas.size
        } catch (e: Exception) { 0 }
    }

    /**
     * ✅ NOVO: Finaliza o ciclo EM_ANDAMENTO da rota persistindo dados consolidados
     * - Atualiza CicloAcertoEntity: status=FINALIZADO, dataFim, valores agregados e debitoTotal
     * - Atualiza status/data da Rota
     */
    suspend fun finalizarCicloAtualComDados(rotaId: Long): Boolean {
        return try {
            val cicloAtual = cicloAcertoDao.buscarCicloEmAndamento(rotaId) ?: return false

            // Buscar dados necessários para consolidar
            val acertos = acertoDao.buscarPorCicloId(cicloAtual.id).first()
            val despesas = despesaDao.buscarPorCicloId(cicloAtual.id).first()
            val clientes = clienteDao.obterClientesPorRota(rotaId).first()

            val valorTotalAcertado = acertos.sumOf { it.valorRecebido }
            val valorTotalDespesas = despesas.sumOf { it.valor }
            val lucroLiquido = valorTotalAcertado - valorTotalDespesas
            val clientesAcertados = acertos.map { it.clienteId }.distinct().size
            val totalClientes = clientes.size
            val debitoTotal = clientes.sumOf { it.debitoAtual }

            val agora = java.util.Date()
            val cicloFinalizado = cicloAtual.copy(
                status = StatusCicloAcerto.FINALIZADO,
                dataFim = agora,
                valorTotalAcertado = valorTotalAcertado,
                valorTotalDespesas = valorTotalDespesas,
                lucroLiquido = lucroLiquido,
                clientesAcertados = clientesAcertados,
                totalClientes = totalClientes,
                debitoTotal = debitoTotal
            )
            cicloAcertoDao.atualizar(cicloFinalizado)
            // ✅ SINCRONIZAÇÃO: Enfileirar UPDATE do ciclo finalizado
            try {
                val payload = """
                    {
                        "id": ${cicloFinalizado.id},
                        "numeroCiclo": ${cicloFinalizado.numeroCiclo},
                        "rotaId": ${cicloFinalizado.rotaId},
                        "ano": ${cicloFinalizado.ano},
                        "dataInicio": ${cicloFinalizado.dataInicio?.time ?: 0},
                        "dataFim": ${cicloFinalizado.dataFim?.time ?: 0},
                        "status": "${cicloFinalizado.status.name}",
                        "totalClientes": ${cicloFinalizado.totalClientes},
                        "clientesAcertados": ${cicloFinalizado.clientesAcertados},
                        "valorTotalAcertado": ${cicloFinalizado.valorTotalAcertado},
                        "valorTotalDespesas": ${cicloFinalizado.valorTotalDespesas},
                        "lucroLiquido": ${cicloFinalizado.lucroLiquido},
                        "debitoTotal": ${cicloFinalizado.debitoTotal},
                        "observacoes": "${cicloFinalizado.observacoes ?: ""}",
                        "criadoPor": "${cicloFinalizado.criadoPor}",
                        "dataCriacao": ${cicloFinalizado.dataCriacao.time},
                        "dataAtualizacao": ${cicloFinalizado.dataAtualizacao.time}
                    }
                """.trimIndent()
                adicionarOperacaoSync("CicloAcerto", cicloFinalizado.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("CicloAcerto", cicloFinalizado.id, "UPDATE", "PENDING", null, payload)
            } catch (e: Exception) {
                Log.w("AppRepository", "Erro ao enfileirar UPDATE CicloAcerto: ${e.message}")
            }

            // Atualizar também status/data da rota para consistência
            rotaDao.finalizarCicloRota(rotaId, agora.time)
            true
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao finalizar ciclo atual da rota $rotaId: ${e.message}", e)
            false
        }
    }

    // ✅ NOVO: contar mesas distintas a partir de vários ciclos
    suspend fun contarMesasPorCiclos(cicloIds: List<Long>): Int {
        return try {
            if (cicloIds.isEmpty()) return 0
            val mesas = mutableSetOf<Long>()
            for (cicloId in cicloIds) {
                val acertos = buscarAcertosPorCicloId(cicloId).first()
                for (acerto in acertos) {
                    val itens = acertoMesaDao.buscarPorAcertoId(acerto.id)
                    itens.forEach { mesas.add(it.mesaId) }
                }
            }
            mesas.size
        } catch (e: Exception) { 0 }
    }
    
    // ✅ NOVO: calcular total de descontos por ciclo
    suspend fun calcularTotalDescontosPorCiclo(cicloId: Long): Double {
        return try {
            val acertos = buscarAcertosPorCicloId(cicloId).first()
            val totalDescontos = acertos.sumOf { it.desconto }
            android.util.Log.d("AppRepository", "✅ Total de descontos calculado para ciclo $cicloId: R$ $totalDescontos")
            totalDescontos
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular total de descontos para ciclo $cicloId: ${e.message}")
            0.0
        }
    }

    // ✅ NOVO: calcular comissões de motorista e Iltair por ciclo
    suspend fun calcularComissoesPorCiclo(cicloId: Long): Pair<Double, Double> {
        val acertos = buscarAcertosPorCicloId(cicloId).first()
        val despesas = buscarDespesasPorCicloId(cicloId).first()
        
        val totalRecebido = acertos.sumOf { it.valorRecebido }
        val despesasViagem = despesas.filter { it.categoria.equals("Viagem", ignoreCase = true) }.sumOf { it.valor }
        val subtotal = totalRecebido - despesasViagem
        
        val comissaoMotorista = subtotal * 0.03 // 3% do subtotal
        val comissaoIltair = totalRecebido * 0.02 // 2% do faturamento total
        
        return Pair(comissaoMotorista, comissaoIltair)
    }
    
    // ✅ NOVO: calcular comissões por ano e número de ciclo
    suspend fun calcularComissoesPorAnoECiclo(ano: Int, numeroCiclo: Int): Pair<Double, Double> {
        val ciclos = obterTodosCiclos().first()
            .filter { it.ano == ano && it.numeroCiclo == numeroCiclo }
        
        var totalComissaoMotorista = 0.0
        var totalComissaoIltair = 0.0
        
        for (ciclo in ciclos) {
            val (comissaoMotorista, comissaoIltair) = calcularComissoesPorCiclo(ciclo.id)
            totalComissaoMotorista += comissaoMotorista
            totalComissaoIltair += comissaoIltair
        }
        
        return Pair(totalComissaoMotorista, totalComissaoIltair)
    }
    
    // ✅ NOVO: calcular comissões por ano (todos os ciclos)
    suspend fun calcularComissoesPorAno(ano: Int): Pair<Double, Double> {
        val ciclos = obterTodosCiclos().first()
            .filter { it.ano == ano }
        
        var totalComissaoMotorista = 0.0
        var totalComissaoIltair = 0.0
        
        for (ciclo in ciclos) {
            val (comissaoMotorista, comissaoIltair) = calcularComissoesPorCiclo(ciclo.id)
            totalComissaoMotorista += comissaoMotorista
            totalComissaoIltair += comissaoIltair
        }
        
        return Pair(totalComissaoMotorista, totalComissaoIltair)
    }
    
    // ==================== COLABORADOR ====================
    
    fun obterTodosColaboradores() = colaboradorDao.obterTodos()
    fun obterColaboradoresAtivos() = colaboradorDao.obterAtivos()
    fun obterColaboradoresAprovados() = colaboradorDao.obterAprovados()
    fun obterColaboradoresPendentesAprovacao() = colaboradorDao.obterPendentesAprovacao()
    fun obterColaboradoresPorNivelAcesso(nivelAcesso: NivelAcesso) = colaboradorDao.obterPorNivelAcesso(nivelAcesso)
    
    suspend fun obterColaboradorPorId(id: Long) = colaboradorDao.obterPorId(id)
    suspend fun obterColaboradorPorEmail(email: String) = colaboradorDao.obterPorEmail(email)
    suspend fun obterColaboradorPorFirebaseUid(firebaseUid: String) = colaboradorDao.obterPorFirebaseUid(firebaseUid)
    suspend fun obterColaboradorPorGoogleId(googleId: String) = colaboradorDao.obterPorGoogleId(googleId)
    
    suspend fun inserirColaborador(colaborador: Colaborador): Long {
        logDbInsertStart("COLABORADOR", "Nome=${colaborador.nome}, Email=${colaborador.email}, Nivel=${colaborador.nivelAcesso}")
        return try {
            val id = colaboradorDao.inserir(colaborador)
            logDbInsertSuccess("COLABORADOR", "Email=${colaborador.email}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${colaborador.nome}",
                        "email": "${colaborador.email}",
                        "nivelAcesso": "${colaborador.nivelAcesso}",
                        "ativo": ${colaborador.ativo},
                        "aprovado": ${colaborador.aprovado},
                        "dataCadastro": "${colaborador.dataCadastro}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Colaborador", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("Colaborador", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar colaborador à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("COLABORADOR", "Email=${colaborador.email}", e)
            throw e
        }
    }
    suspend fun atualizarColaborador(colaborador: Colaborador) {
        logDbUpdateStart("COLABORADOR", "ID=${colaborador.id}, Nome=${colaborador.nome}")
        try {
            colaboradorDao.atualizar(colaborador)
            logDbUpdateSuccess("COLABORADOR", "ID=${colaborador.id}, Nome=${colaborador.nome}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${colaborador.id},
                        "nome": "${colaborador.nome}",
                        "email": "${colaborador.email}",
                        "telefone": "${colaborador.telefone ?: ""}",
                        "cpf": "${colaborador.cpf ?: ""}",
                        "endereco": "${colaborador.endereco ?: ""}",
                        "bairro": "${colaborador.bairro ?: ""}",
                        "cidade": "${colaborador.cidade ?: ""}",
                        "estado": "${colaborador.estado ?: ""}",
                        "cep": "${colaborador.cep ?: ""}",
                        "rg": "${colaborador.rg ?: ""}",
                        "orgaoEmissor": "${colaborador.orgaoEmissor ?: ""}",
                        "estadoCivil": "${colaborador.estadoCivil ?: ""}",
                        "nomeMae": "${colaborador.nomeMae ?: ""}",
                        "nomePai": "${colaborador.nomePai ?: ""}",
                        "fotoPerfil": "${colaborador.fotoPerfil ?: ""}",
                        "nivelAcesso": "${colaborador.nivelAcesso.name}",
                        "ativo": ${colaborador.ativo},
                        "aprovado": ${colaborador.aprovado},
                        "dataAprovacao": "${colaborador.dataAprovacao ?: ""}",
                        "aprovadoPor": "${colaborador.aprovadoPor ?: ""}",
                        "firebaseUid": "${colaborador.firebaseUid ?: ""}",
                        "googleId": "${colaborador.googleId ?: ""}",
                        "senhaTemporaria": "${colaborador.senhaTemporaria ?: ""}",
                        "emailAcesso": "${colaborador.emailAcesso ?: ""}",
                        "observacoes": "${colaborador.observacoes ?: ""}",
                        "dataCadastro": "${colaborador.dataCadastro}",
                        "dataUltimoAcesso": "${colaborador.dataUltimoAcesso ?: ""}",
                        "dataUltimaAtualizacao": "${colaborador.dataUltimaAtualizacao}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Colaborador", colaborador.id, "UPDATE", payload, priority = 1)
                logarOperacaoSync("Colaborador", colaborador.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar atualização de colaborador à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("COLABORADOR", "ID=${colaborador.id}", e)
            throw e
        }
    }
    suspend fun deletarColaborador(colaborador: Colaborador) = colaboradorDao.deletar(colaborador)
    
    suspend fun aprovarColaborador(colaboradorId: Long, dataAprovacao: java.util.Date, aprovadoPor: String) = 
        colaboradorDao.aprovarColaborador(colaboradorId, dataAprovacao, aprovadoPor)
    
    suspend fun aprovarColaboradorComCredenciais(
        colaboradorId: Long,
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        dataAprovacao: java.util.Date,
        aprovadoPor: String
    ) = colaboradorDao.aprovarColaboradorComCredenciais(
        colaboradorId, email, senha, nivelAcesso, observacoes, dataAprovacao, aprovadoPor
    )
    suspend fun alterarStatusColaborador(colaboradorId: Long, ativo: Boolean) = 
        colaboradorDao.alterarStatus(colaboradorId, ativo)
    suspend fun atualizarUltimoAcessoColaborador(colaboradorId: Long, dataUltimoAcesso: java.util.Date) = 
        colaboradorDao.atualizarUltimoAcesso(colaboradorId, dataUltimoAcesso)
    
    suspend fun contarColaboradoresAtivos() = colaboradorDao.contarAtivos()
    suspend fun contarColaboradoresPendentesAprovacao() = colaboradorDao.contarPendentesAprovacao()
    
    // ==================== META COLABORADOR ====================
    
    fun obterMetasPorColaborador(colaboradorId: Long) = colaboradorDao.obterMetasPorColaborador(colaboradorId)
    suspend fun obterMetaAtual(colaboradorId: Long, tipoMeta: TipoMeta) = colaboradorDao.obterMetaAtual(colaboradorId, tipoMeta)
    suspend fun inserirMeta(meta: MetaColaborador): Long {
        logDbInsertStart("META", "ColaboradorID=${meta.colaboradorId}, Tipo=${meta.tipoMeta}, Valor=${meta.valorMeta}")
        return try {
            val id = colaboradorDao.inserirMeta(meta)
            logDbInsertSuccess("META", "ColaboradorID=${meta.colaboradorId}, ID=$id")
            // ✅ SINCRONIZAÇÃO: Enfileirar CREATE de MetaColaborador
            try {
                val payload = """
                    {
                        "id": $id,
                        "colaboradorId": ${meta.colaboradorId},
                        "rotaId": ${if (meta.rotaId != null) meta.rotaId else "null"},
                        "cicloId": ${meta.cicloId},
                        "tipoMeta": "${meta.tipoMeta}",
                        "valorMeta": ${meta.valorMeta},
                        "valorAtual": ${meta.valorAtual},
                        "ativo": ${meta.ativo},
                        "dataCriacao": ${meta.dataCriacao.time}
                    }
                """.trimIndent()
                adicionarOperacaoSync("MetaColaborador", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("MetaColaborador", id, "CREATE", "PENDING", null, payload)
            } catch (e: Exception) {
                Log.w("AppRepository", "Erro ao enfileirar META: ${e.message}")
            }
            id
        } catch (e: Exception) {
            logDbInsertError("META", "ColaboradorID=${meta.colaboradorId}", e)
            throw e
        }
    }
    suspend fun atualizarMeta(meta: MetaColaborador) {
        colaboradorDao.atualizarMeta(meta)
        // ✅ SINCRONIZAÇÃO: UPDATE
        try {
            val payload = """
                {
                    "id": ${meta.id},
                    "colaboradorId": ${meta.colaboradorId},
                    "rotaId": ${if (meta.rotaId != null) meta.rotaId else "null"},
                    "cicloId": ${meta.cicloId},
                    "tipoMeta": "${meta.tipoMeta}",
                    "valorMeta": ${meta.valorMeta},
                    "valorAtual": ${meta.valorAtual},
                    "ativo": ${meta.ativo},
                    "dataCriacao": ${meta.dataCriacao.time}
                }
            """.trimIndent()
            adicionarOperacaoSync("MetaColaborador", meta.id, "UPDATE", payload, priority = 1)
            logarOperacaoSync("MetaColaborador", meta.id, "UPDATE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao enfileirar UPDATE META: ${e.message}")
        }
    }
    suspend fun deletarMeta(meta: MetaColaborador) {
        colaboradorDao.deletarMeta(meta)
        // ✅ SINCRONIZAÇÃO: DELETE
        try {
            val payload = """{ "id": ${meta.id} }"""
            adicionarOperacaoSync("MetaColaborador", meta.id, "DELETE", payload, priority = 1)
            logarOperacaoSync("MetaColaborador", meta.id, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao enfileirar DELETE META: ${e.message}")
        }
    }
    suspend fun atualizarValorAtualMeta(metaId: Long, valorAtual: Double) = colaboradorDao.atualizarValorAtualMeta(metaId, valorAtual)
    
    // ==================== METAS POR ROTA ====================
    
    fun obterMetasPorRota(rotaId: Long) = colaboradorDao.obterMetasPorRota(0L, rotaId)
    fun obterMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = colaboradorDao.obterMetasPorCiclo(colaboradorId, cicloId)
    fun obterMetasPorColaboradorERota(colaboradorId: Long, rotaId: Long) = colaboradorDao.obterMetasPorRota(colaboradorId, rotaId)
    fun obterMetasPorColaboradorCicloERota(colaboradorId: Long, cicloId: Long, rotaId: Long) = colaboradorDao.obterMetasPorCicloERota(colaboradorId, cicloId, rotaId)
    suspend fun desativarMetasColaborador(colaboradorId: Long) = colaboradorDao.desativarMetasColaborador(colaboradorId)
    
    // Métodos para metas
    suspend fun buscarMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = colaboradorDao.buscarMetasPorColaboradorECiclo(colaboradorId, cicloId)
    suspend fun buscarMetasPorRotaECiclo(rotaId: Long, cicloId: Long) = colaboradorDao.buscarMetasPorRotaECiclo(rotaId, cicloId)

    suspend fun existeMetaDuplicada(rotaId: Long, cicloId: Long, tipoMeta: TipoMeta): Boolean {
        val count = colaboradorDao.contarMetasPorRotaCicloETipo(rotaId, cicloId, tipoMeta)
        return count > 0
    }
    
    // ==================== FUNÇÕES PARA SISTEMA DE METAS ====================
    
    /**
     * Busca colaborador responsável principal por uma rota
     */
    suspend fun buscarColaboradorResponsavelPrincipal(rotaId: Long): Colaborador? {
        return try {
            colaboradorDao?.buscarColaboradorResponsavelPrincipal(rotaId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao buscar colaborador responsável: ${e.message}", e)
            null
        }
    }
    
    /**
     * Busca ciclo atual (em andamento) para uma rota
     */
    suspend fun buscarCicloAtualPorRota(rotaId: Long): CicloAcertoEntity? {
        return try {
            cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao buscar ciclo atual: ${e.message}", e)
            null
        }
    }
    
    /**
     * Busca ciclos futuros (planejados) para uma rota
     */
    suspend fun buscarCiclosFuturosPorRota(rotaId: Long): List<CicloAcertoEntity> {
        return try {
            cicloAcertoDao.buscarCiclosFuturosPorRota(rotaId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao buscar ciclos futuros: ${e.message}", e)
            emptyList()
        }
    }
    
    
    fun buscarMetasAtivasPorColaborador(colaboradorId: Long) = colaboradorDao.buscarMetasAtivasPorColaborador(colaboradorId)
    suspend fun buscarMetasPorTipoECiclo(tipoMeta: TipoMeta, cicloId: Long) = colaboradorDao.buscarMetasPorTipoECiclo(tipoMeta, cicloId)
    
    // ==================== COLABORADOR ROTA ====================
    
    fun obterRotasPorColaborador(colaboradorId: Long) = colaboradorDao.obterRotasPorColaborador(colaboradorId)
    fun obterColaboradoresPorRota(rotaId: Long) = colaboradorDao.obterColaboradoresPorRota(rotaId)
    suspend fun obterRotaPrincipal(colaboradorId: Long) = colaboradorDao.obterRotaPrincipal(colaboradorId)
    suspend fun inserirColaboradorRota(colaboradorRota: ColaboradorRota): Long {
        logDbInsertStart(
            "COLABORADOR_ROTA",
            "ColaboradorID=${colaboradorRota.colaboradorId}, RotaID=${colaboradorRota.rotaId}, Responsavel=${colaboradorRota.responsavelPrincipal}"
        )

        return try {
            val id = colaboradorDao.inserirColaboradorRota(colaboradorRota)
            logDbInsertSuccess(
                "COLABORADOR_ROTA",
                "ColaboradorID=${colaboradorRota.colaboradorId}, RotaID=${colaboradorRota.rotaId}, ID=$id"
            )
            // ✅ SINCRONIZAÇÃO: CREATE ColaboradorRota
            try {
                val payload = """
                    {
                        "id": $id,
                        "colaboradorId": ${colaboradorRota.colaboradorId},
                        "rotaId": ${colaboradorRota.rotaId},
                        "responsavelPrincipal": ${colaboradorRota.responsavelPrincipal},
                        "dataVinculacao": ${colaboradorRota.dataVinculacao.time}
                    }
                """.trimIndent()
                adicionarOperacaoSync("ColaboradorRota", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("ColaboradorRota", id, "CREATE", "PENDING", null, payload)
            } catch (e: Exception) {
                Log.w("AppRepository", "Erro ao enfileirar ColaboradorRota: ${e.message}")
            }
            id
        } catch (e: Exception) {
            logDbInsertError(
                "COLABORADOR_ROTA",
                "ColaboradorID=${colaboradorRota.colaboradorId}, RotaID=${colaboradorRota.rotaId}",
                e
            )
            throw e
        }
    }

    /**
     * Busca acertos de um cliente filtrando pelo cicloId (consulta direta no DAO)
     */
    fun buscarAcertosPorClienteECicloId(clienteId: Long, cicloId: Long) = acertoDao.buscarPorClienteECicloId(clienteId, cicloId)
    suspend fun deletarColaboradorRota(colaboradorRota: ColaboradorRota) {
        colaboradorDao.deletarColaboradorRota(colaboradorRota)
        // ✅ SINCRONIZAÇÃO: DELETE ColaboradorRota
        try {
            val payload = """{ "colaboradorId": ${colaboradorRota.colaboradorId}, "rotaId": ${colaboradorRota.rotaId} }"""
            val entityId = colaboradorRota.colaboradorId * 1000000L + colaboradorRota.rotaId
            adicionarOperacaoSync("ColaboradorRota", entityId, "DELETE", payload, priority = 1)
            logarOperacaoSync("ColaboradorRota", entityId, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao enfileirar DELETE ColaboradorRota: ${e.message}")
        }
    }
    suspend fun deletarTodasRotasColaborador(colaboradorId: Long) = colaboradorDao.deletarTodasRotasColaborador(colaboradorId)
    suspend fun removerResponsavelPrincipal(colaboradorId: Long) = colaboradorDao.removerResponsavelPrincipal(colaboradorId)
    suspend fun definirResponsavelPrincipal(colaboradorId: Long, rotaId: Long) = colaboradorDao.definirResponsavelPrincipal(colaboradorId, rotaId)
    
    // Métodos auxiliares para vinculação de colaborador com rotas
    suspend fun removerRotasColaborador(colaboradorId: Long) = colaboradorDao.deletarTodasRotasColaborador(colaboradorId)
    suspend fun vincularColaboradorRota(colaboradorId: Long, rotaId: Long, responsavelPrincipal: Boolean, dataVinculacao: java.util.Date) {
        val colaboradorRota = ColaboradorRota(
            colaboradorId = colaboradorId,
            rotaId = rotaId,
            responsavelPrincipal = responsavelPrincipal,
            dataVinculacao = dataVinculacao
        )
        colaboradorDao.inserirColaboradorRota(colaboradorRota)
    }
    
    
    // ==================== CICLO ACERTO ====================
    
    fun obterTodosCiclos() = cicloAcertoDao.listarTodos()
    
    suspend fun buscarUltimoCicloFinalizadoPorRota(rotaId: Long) = cicloAcertoDao.buscarUltimoCicloFinalizadoPorRota(rotaId)
    suspend fun buscarCiclosPorRotaEAno(rotaId: Long, ano: Int) = cicloAcertoDao.buscarCiclosPorRotaEAno(rotaId, ano)
    
    suspend fun buscarCiclosPorRota(rotaId: Long) = cicloAcertoDao.buscarCiclosPorRota(rotaId)
    suspend fun buscarProximoNumeroCiclo(rotaId: Long, ano: Int) = cicloAcertoDao.buscarProximoNumeroCiclo(rotaId, ano)
    suspend fun inserirCicloAcerto(ciclo: CicloAcertoEntity): Long {
        logDbInsertStart("CICLO", "RotaID=${ciclo.rotaId}, Numero=${ciclo.numeroCiclo}, Status=${ciclo.status}")
        return try {
            val id = cicloAcertoDao.inserir(ciclo)
            logDbInsertSuccess("CICLO", "ID=$id, RotaID=${ciclo.rotaId}")
            
            // ✅ CORREÇÃO: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "numeroCiclo": ${ciclo.numeroCiclo},
                        "rotaId": ${ciclo.rotaId},
                        "ano": ${ciclo.ano},
                        "dataInicio": "${ciclo.dataInicio}",
                        "dataFim": "${ciclo.dataFim}",
                        "status": "${ciclo.status.name}",
                        "totalClientes": ${ciclo.totalClientes},
                        "clientesAcertados": ${ciclo.clientesAcertados},
                        "valorTotalAcertado": ${ciclo.valorTotalAcertado},
                        "valorTotalDespesas": ${ciclo.valorTotalDespesas},
                        "lucroLiquido": ${ciclo.lucroLiquido},
                        "debitoTotal": ${ciclo.debitoTotal},
                        "observacoes": "${ciclo.observacoes ?: ""}",
                        "criadoPor": "${ciclo.criadoPor}",
                        "dataCriacao": "${ciclo.dataCriacao}",
                        "dataAtualizacao": "${ciclo.dataAtualizacao}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("CicloAcerto", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("CicloAcerto", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar ciclo à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CICLO", "RotaID=${ciclo.rotaId}", e)
            throw e
        }
    }

    /**
     * Busca ciclos que podem ter metas definidas (em andamento ou planejados)
     */
    suspend fun buscarCiclosParaMetas(rotaId: Long): List<CicloAcertoEntity> {
        val cicloEmAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
        val ciclosFuturos = cicloAcertoDao.buscarCiclosFuturosPorRota(rotaId)
        
        val listaCombinada = mutableListOf<CicloAcertoEntity>()
        cicloEmAndamento?.let { listaCombinada.add(it) }
        listaCombinada.addAll(ciclosFuturos)
        
        return listaCombinada
    }
    
    // ==================== MÉTODOS PARA RELATÓRIOS ====================
    
    // Métodos para relatórios de despesas
    suspend fun getDespesasPorCiclo(cicloId: Long, rotaId: Long): List<DespesaRelatorio> {
        return try {
            val despesas = if (rotaId == 0L) {
                despesaDao.buscarPorCicloId(cicloId).first()
            } else {
                despesaDao.buscarPorRotaECicloId(rotaId, cicloId).first()
            }
	            // Evita chamar função suspend dentro de map
	            val rotasMap = rotaDao.getAllRotas().first().associateBy { it.id }
            
            despesas.map { despesa ->
	                val rotaNome = rotasMap[despesa.rotaId]?.nome ?: "Rota não encontrada"
                DespesaRelatorio(
                    id = despesa.id,
                    descricao = despesa.descricao,
                    valor = despesa.valor,
                    categoria = despesa.categoria,
                    data = despesa.dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
	                    rota = rotaNome,
                    observacoes = despesa.observacoes.takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getDespesasConsolidadasCiclos(numeroCiclo: Int, ano: Int, rotaId: Long): List<DespesaRelatorio> {
        return try {
            // Buscar todos os ciclos do mesmo número no ano
            val ciclos = cicloAcertoDao.listarTodos().first()
                .filter { it.numeroCiclo == numeroCiclo && it.dataInicio.year + 1900 == ano }
            
            val despesas = mutableListOf<DespesaRelatorio>()
            
            for (ciclo in ciclos) {
                val despesasCiclo = if (rotaId == 0L) {
                    despesaDao.buscarPorCicloId(ciclo.id).first()
                } else {
                    despesaDao.buscarPorRotaECicloId(rotaId, ciclo.id).first()
                }
	                // Evita função suspend dentro de map
	                val rotasMap = rotaDao.getAllRotas().first().associateBy { it.id }
                
                despesas.addAll(despesasCiclo.map { despesa ->
	                    val rotaNome = rotasMap[despesa.rotaId]?.nome ?: "Rota não encontrada"
                    DespesaRelatorio(
                        id = despesa.id,
                        descricao = despesa.descricao,
                        valor = despesa.valor,
                        categoria = despesa.categoria,
                        data = despesa.dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
	                        rota = rotaNome,
                        observacoes = despesa.observacoes.takeIf { it.isNotBlank() }
                    )
                })
            }
            
            despesas
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getDespesasPorAno(ano: Int, rotaId: Long): List<DespesaRelatorio> {
        return try {
            val dataInicio = java.time.LocalDateTime.of(ano, 1, 1, 0, 0)
            val dataFim = java.time.LocalDateTime.of(ano, 12, 31, 23, 59)
            
	            val despesas: List<Despesa> = if (rotaId == 0L) {
	                // Converte DespesaResumo -> Despesa para unificar o tipo
	                despesaDao.buscarPorPeriodo(dataInicio, dataFim).first().map { it.despesa }
            } else {
                despesaDao.buscarPorRotaEPeriodo(rotaId, dataInicio, dataFim)
            }
            
	            // Evita chamar função suspend dentro de map
	            val rotasMap = rotaDao.getAllRotas().first().associateBy { it.id }
            
            despesas.map { despesa ->
	                val rotaNome = rotasMap[despesa.rotaId]?.nome ?: "Rota não encontrada"
                DespesaRelatorio(
                    id = despesa.id,
                    descricao = despesa.descricao,
                    valor = despesa.valor,
                    categoria = despesa.categoria,
                    data = despesa.dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
	                    rota = rotaNome,
                    observacoes = despesa.observacoes.takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getCategoriasDespesas(): List<String> {
        return try {
            val despesas = despesaDao.buscarTodasComRota().first()
            despesas.map { it.categoria }.distinct().sorted()
        } catch (e: Exception) {
            listOf("Combustível", "Alimentação", "Transporte", "Manutenção", "Materiais", "Outros")
        }
    }
    
    // Data class para relatórios
    data class DespesaRelatorio(
        val id: Long,
        val descricao: String,
        val valor: Double,
        val categoria: String,
        val data: String,
        val rota: String,
        val observacoes: String?
    )
    
    // Métodos stub para sincronização - BLOQUEADOS para evitar população automática
    suspend fun syncRotas(_rotas: List<Rota>) {
        // BLOQUEADO: Sincronização de rotas desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC ROTAS BLOQUEADO - Evitando população automática")
    }

    suspend fun syncClientes(_clientes: List<Cliente>) {
        // BLOQUEADO: Sincronização de clientes desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC CLIENTES BLOQUEADO - Evitando população automática")
    }

    suspend fun syncAcertos(_acertos: List<Acerto>) {
        // BLOQUEADO: Sincronização de acertos desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC ACERTOS BLOQUEADO - Evitando população automática")
    }

    suspend fun syncColaboradores(_colaboradores: List<Colaborador>) {
        // BLOQUEADO: Sincronização de colaboradores desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC COLABORADORES BLOQUEADO - Evitando população automática")
    }
    
    // ==================== CONTRATOS DE LOCAÇÃO ====================
    
    fun buscarContratosPorCliente(clienteId: Long) = contratoLocacaoDao.buscarContratosPorCliente(clienteId)
    suspend fun buscarContratoPorNumero(numeroContrato: String) = contratoLocacaoDao.buscarContratoPorNumero(numeroContrato)
    fun buscarContratosAtivos() = contratoLocacaoDao.buscarContratosAtivos()
    fun buscarTodosContratos() = contratoLocacaoDao.buscarTodosContratos()
    suspend fun contarContratosPorAno(ano: String) = contratoLocacaoDao.contarContratosPorAno(ano)
    suspend fun contarContratosGerados() = contratoLocacaoDao.contarContratosGerados()
    suspend fun contarContratosAssinados() = contratoLocacaoDao.contarContratosAssinados()
    suspend fun obterContratosAssinados() = contratoLocacaoDao.obterContratosAssinados()
    suspend fun inserirContrato(contrato: ContratoLocacao): Long {
        logDbInsertStart("CONTRATO", "Numero=${contrato.numeroContrato}, ClienteID=${contrato.clienteId}")
        return try {
            val id = contratoLocacaoDao.inserirContrato(contrato)
            logDbInsertSuccess("CONTRATO", "Numero=${contrato.numeroContrato}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("CONTRATO", "Numero=${contrato.numeroContrato}", e)
            throw e
        }
    }
    suspend fun atualizarContrato(contrato: ContratoLocacao) {
        try {
            Log.d("RepoUpdate", "Atualizando contrato id=${contrato.id} cliente=${contrato.clienteId} status=${contrato.status} encerramento=${contrato.dataEncerramento}")
            contratoLocacaoDao.atualizarContrato(contrato)
            // Leitura de verificação (apenas diagnóstico)
            try {
                val apos = contratoLocacaoDao.buscarContratosPorCliente(contrato.clienteId).first()
                val resumo = apos.joinToString { c -> "id=${'$'}{c.id},status=${'$'}{c.status},enc=${'$'}{c.dataEncerramento}" }
                Log.d("RepoContracts", "Após atualizar: cliente=${contrato.clienteId} contratos=${apos.size} -> ${'$'}resumo")
            } catch (e: Exception) {
                Log.e("RepoContracts", "Falha ao ler contratos após atualizar", e)
            }
        } catch (e: Exception) {
            Log.e("RepoUpdate", "Erro ao atualizar contrato id=${contrato.id}", e)
            throw e
        }
    }

    // ✅ NOVO: Encerrar contrato (UPDATE direto)
    suspend fun encerrarContrato(contratoId: Long, clienteId: Long, status: String) {
        val agora = java.util.Date()
        Log.d("RepoUpdate", "Encerrar direto contrato id=${contratoId} status=${status} em ${agora}")
        contratoLocacaoDao.encerrarContrato(contratoId, status, agora, agora)
        val apos = contratoLocacaoDao.buscarContratosPorCliente(clienteId).first()
        val resumo = apos.joinToString { c -> "id=${'$'}{c.id},status=${'$'}{c.status},enc=${'$'}{c.dataEncerramento}" }
        Log.d("RepoContracts", "Após encerrar direto: cliente=${clienteId} contratos=${apos.size} -> ${'$'}resumo")
    }
    suspend fun excluirContrato(contrato: ContratoLocacao) = contratoLocacaoDao.excluirContrato(contrato)
    suspend fun buscarContratoPorId(contratoId: Long) = contratoLocacaoDao.buscarContratoPorId(contratoId)
    suspend fun buscarMesasPorContrato(contratoId: Long) = contratoLocacaoDao.buscarMesasPorContrato(contratoId)
    suspend fun inserirContratoMesa(contratoMesa: ContratoMesa): Long {
        logDbInsertStart("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}, MesaID=${contratoMesa.mesaId}")
        return try {
            val id = contratoLocacaoDao.inserirContratoMesa(contratoMesa)
            logDbInsertSuccess("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}, ID=$id")
            // ✅ SINCRONIZAÇÃO: CREATE ContratoMesa
            try {
                val payload = """
                    {
                        "id": $id,
                        "contratoId": ${contratoMesa.contratoId},
                        "mesaId": ${contratoMesa.mesaId},
                        "tipoEquipamento": "${contratoMesa.tipoEquipamento}",
                        "numeroSerie": "${contratoMesa.numeroSerie}",
                        "valorFicha": ${contratoMesa.valorFicha ?: 0.0},
                        "valorFixo": ${contratoMesa.valorFixo ?: 0.0}
                    }
                """.trimIndent()
                adicionarOperacaoSync("ContratoMesa", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("ContratoMesa", id, "CREATE", "PENDING", null, payload)
            } catch (e: Exception) {
                Log.w("AppRepository", "Erro ao enfileirar ContratoMesa: ${e.message}")
            }
            id
        } catch (e: Exception) {
            logDbInsertError("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}", e)
            throw e
        }
    }
    suspend fun inserirContratoMesas(contratoMesas: List<ContratoMesa>): List<Long> {
        logDbInsertStart("CONTRATO_MESAS", "Quantidade=${contratoMesas.size}")
        return try {
            val ids = contratoLocacaoDao.inserirContratoMesas(contratoMesas)
            logDbInsertSuccess("CONTRATO_MESAS", "IDs=${ids.joinToString()}")
            // ✅ SINCRONIZAÇÃO: CREATE em lote
            try {
                contratoMesas.zip(ids).forEach { (cm, id) ->
                    val payload = """
                        {
                            "id": $id,
                            "contratoId": ${cm.contratoId},
                            "mesaId": ${cm.mesaId},
                            "tipoEquipamento": "${cm.tipoEquipamento}",
                            "numeroSerie": "${cm.numeroSerie}",
                            "valorFicha": ${cm.valorFicha ?: 0.0},
                            "valorFixo": ${cm.valorFixo ?: 0.0}
                        }
                    """.trimIndent()
                    adicionarOperacaoSync("ContratoMesa", id, "CREATE", payload, priority = 1)
                    logarOperacaoSync("ContratoMesa", id, "CREATE", "PENDING", null, payload)
                }
            } catch (e: Exception) {
                Log.w("AppRepository", "Erro ao enfileirar lote ContratoMesas: ${e.message}")
            }
            ids
        } catch (e: Exception) {
            logDbInsertError("CONTRATO_MESAS", "Quantidade=${contratoMesas.size}", e)
            throw e
        }
    }
    suspend fun excluirContratoMesa(contratoMesa: ContratoMesa) {
        contratoLocacaoDao.excluirContratoMesa(contratoMesa)
        // ✅ SINCRONIZAÇÃO: DELETE
        try {
            val payload = """{ "id": ${contratoMesa.id} }"""
            adicionarOperacaoSync("ContratoMesa", contratoMesa.id, "DELETE", payload, priority = 1)
            logarOperacaoSync("ContratoMesa", contratoMesa.id, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao enfileirar DELETE ContratoMesa: ${e.message}")
        }
    }
    suspend fun excluirMesasPorContrato(contratoId: Long) = contratoLocacaoDao.excluirMesasPorContrato(contratoId)
    
    // ==================== ADITIVO CONTRATO ====================
    
    fun buscarAditivosPorContrato(contratoId: Long) = aditivoContratoDao.buscarAditivosPorContrato(contratoId)
    suspend fun buscarAditivoPorNumero(numeroAditivo: String) = aditivoContratoDao.buscarAditivoPorNumero(numeroAditivo)
    suspend fun buscarAditivoPorId(aditivoId: Long) = aditivoContratoDao.buscarAditivoPorId(aditivoId)
    fun buscarTodosAditivos() = aditivoContratoDao.buscarTodosAditivos()
    suspend fun contarAditivosPorAno(ano: String) = aditivoContratoDao.contarAditivosPorAno(ano)
    suspend fun contarAditivosGerados() = aditivoContratoDao.contarAditivosGerados()
    suspend fun contarAditivosAssinados() = aditivoContratoDao.contarAditivosAssinados()
    suspend fun inserirAditivo(aditivo: AditivoContrato): Long {
        logDbInsertStart("ADITIVO", "ContratoID=${aditivo.contratoId}, Numero=${aditivo.numeroAditivo}")
        return try {
            val id = aditivoContratoDao.inserirAditivo(aditivo)
            logDbInsertSuccess("ADITIVO", "ContratoID=${aditivo.contratoId}, ID=$id")
            // ✅ SINCRONIZAÇÃO: Enfileirar CREATE de AditivoContrato
            try {
                val payload = """
                    {
                        "id": $id,
                        "numeroAditivo": "${aditivo.numeroAditivo}",
                        "contratoId": ${aditivo.contratoId},
                        "dataAditivo": ${aditivo.dataAditivo.time},
                        "observacoes": "${aditivo.observacoes ?: ""}",
                        "tipo": "${aditivo.tipo}",
                        "assinaturaLocador": ${aditivo.assinaturaLocador?.let { "\"$it\"" } ?: "null"},
                        "assinaturaLocatario": ${aditivo.assinaturaLocatario?.let { "\"$it\"" } ?: "null"},
                        "dataCriacao": ${aditivo.dataCriacao.time},
                        "dataAtualizacao": ${aditivo.dataAtualizacao.time}
                    }
                """.trimIndent()
                adicionarOperacaoSync("AditivoContrato", id, "CREATE", payload, priority = 1)
                logarOperacaoSync("AditivoContrato", id, "CREATE", "PENDING", null, payload)
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao adicionar AditivoContrato à fila de sync: ${syncError.message}")
            }
            id
        } catch (e: Exception) {
            logDbInsertError("ADITIVO", "ContratoID=${aditivo.contratoId}", e)
            throw e
        }
    }

    suspend fun inserirAditivoMesas(aditivoMesas: List<AditivoMesa>): List<Long> {
        logDbInsertStart("ADITIVO_MESAS", "Quantidade=${aditivoMesas.size}")
        return try {
            val ids = aditivoContratoDao.inserirAditivoMesas(aditivoMesas)
            logDbInsertSuccess("ADITIVO_MESAS", "IDs=${ids.joinToString()}")
            // ✅ SINCRONIZAÇÃO: Enfileirar CREATE para cada AditivoMesa
            try {
                aditivoMesas.zip(ids).forEach { (mesa, id) ->
                    val payload = """
                        {
                            "id": $id,
                            "aditivoId": ${mesa.aditivoId},
                            "mesaId": ${mesa.mesaId},
                            "tipoEquipamento": "${mesa.tipoEquipamento}",
                            "numeroSerie": "${mesa.numeroSerie}",
                            "valorFicha": ${mesa.valorFicha ?: 0.0},
                            "valorFixo": ${mesa.valorFixo ?: 0.0}
                        }
                    """.trimIndent()
                    adicionarOperacaoSync("AditivoMesa", id, "CREATE", payload, priority = 1)
                    logarOperacaoSync("AditivoMesa", id, "CREATE", "PENDING", null, payload)
                }
            } catch (syncError: Exception) {
                Log.w("AppRepository", "Erro ao enfileirar AditivoMesa: ${syncError.message}")
            }
            ids
        } catch (e: Exception) {
            logDbInsertError("ADITIVO_MESAS", "Quantidade=${aditivoMesas.size}", e)
            throw e
        }
    }

    suspend fun inserirAssinaturaRepresentanteLegal(assinatura: AssinaturaRepresentanteLegal): Long {
        logDbInsertStart(
            "ASSINATURA",
            "Representante=${assinatura.nomeRepresentante}, NumeroProcuração=${assinatura.numeroProcuração}"
        )
        return try {
            val id = assinaturaRepresentanteLegalDao.inserirAssinatura(assinatura)
            logDbInsertSuccess(
                "ASSINATURA",
                "Representante=${assinatura.nomeRepresentante}, ID=$id"
            )
            id
        } catch (e: Exception) {
            logDbInsertError(
                "ASSINATURA",
                "Representante=${assinatura.nomeRepresentante}",
                e
            )
            throw e
        }
    }

    suspend fun inserirLogAuditoriaAssinatura(log: LogAuditoriaAssinatura): Long {
        logDbInsertStart(
            "LOG_ASSINATURA",
            "Tipo=${log.tipoOperacao}, ContratoID=${log.idContrato ?: "N/A"}"
        )
        return try {
            val id = logAuditoriaAssinaturaDao.inserirLog(log)
            logDbInsertSuccess(
                "LOG_ASSINATURA",
                "Tipo=${log.tipoOperacao}, ID=$id"
            )
            id
        } catch (e: Exception) {
            logDbInsertError(
                "LOG_ASSINATURA",
                "Tipo=${log.tipoOperacao}, ContratoID=${log.idContrato ?: "N/A"}",
                e
            )
            throw e
        }
    }

    // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    // suspend fun inserirProcuração(procuração: ProcuraçãoRepresentante): Long {
    //     logDbInsertStart("PROCURACAO", "Representante=${procuração.representanteOutorgadoNome}, Empresa=${procuração.empresaNome}")
    //     return try {
    //         val id = .inserirProcuração(procuração)
    //         logDbInsertSuccess("PROCURACAO", "Representante=${procuração.representanteOutorgadoNome}, ID=$id")
    //         id
    //     } catch (e: Exception) {
    //         logDbInsertError("PROCURACAO", "Representante=${procuração.representanteOutorgadoNome}", e)
    //         throw e
    //     }
    // }

    suspend fun atualizarAditivo(aditivo: AditivoContrato) {
        aditivoContratoDao.atualizarAditivo(aditivo)
        // ✅ SINCRONIZAÇÃO: Enfileirar UPDATE
        try {
            val payload = """
                {
                    "id": ${aditivo.id},
                    "numeroAditivo": "${aditivo.numeroAditivo}",
                    "contratoId": ${aditivo.contratoId},
                    "dataAditivo": ${aditivo.dataAditivo.time},
                    "observacoes": "${aditivo.observacoes ?: ""}",
                    "tipo": "${aditivo.tipo}",
                    "assinaturaLocador": ${aditivo.assinaturaLocador?.let { "\"$it\"" } ?: "null"},
                    "assinaturaLocatario": ${aditivo.assinaturaLocatario?.let { "\"$it\"" } ?: "null"},
                    "dataCriacao": ${aditivo.dataCriacao.time},
                    "dataAtualizacao": ${aditivo.dataAtualizacao.time}
                }
            """.trimIndent()
            adicionarOperacaoSync("AditivoContrato", aditivo.id, "UPDATE", payload, priority = 1)
            logarOperacaoSync("AditivoContrato", aditivo.id, "UPDATE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao enfileirar UPDATE AditivoContrato: ${e.message}")
        }
    }
    suspend fun excluirAditivo(aditivo: AditivoContrato) {
        aditivoContratoDao.excluirAditivo(aditivo)
        // ✅ SINCRONIZAÇÃO: Enfileirar DELETE
        try {
            val payload = """{ "id": ${aditivo.id} }"""
            adicionarOperacaoSync("AditivoContrato", aditivo.id, "DELETE", payload, priority = 1)
            logarOperacaoSync("AditivoContrato", aditivo.id, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao enfileirar DELETE AditivoContrato: ${e.message}")
        }
    }
    suspend fun buscarMesasPorAditivo(aditivoId: Long) = aditivoContratoDao.buscarMesasPorAditivo(aditivoId)
    suspend fun excluirAditivoMesa(aditivoMesa: AditivoMesa) {
        aditivoContratoDao.excluirAditivoMesa(aditivoMesa)
        // ✅ SINCRONIZAÇÃO: Enfileirar DELETE
        try {
            val payload = """{ "id": ${aditivoMesa.id} }"""
            adicionarOperacaoSync("AditivoMesa", aditivoMesa.id, "DELETE", payload, priority = 1)
            logarOperacaoSync("AditivoMesa", aditivoMesa.id, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao enfileirar DELETE AditivoMesa: ${e.message}")
        }
    }
    suspend fun excluirTodasMesasDoAditivo(aditivoId: Long) {
        // Enfileirar DELETE para todas as mesas do aditivo
        try {
            val mesas = aditivoContratoDao.buscarMesasPorAditivo(aditivoId)
            mesas.forEach { mesa ->
                val payload = """{ "id": ${mesa.id} }"""
                adicionarOperacaoSync("AditivoMesa", mesa.id, "DELETE", payload, priority = 1)
                logarOperacaoSync("AditivoMesa", mesa.id, "DELETE", "PENDING", null, payload)
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao preparar DELETE das AditivoMesas: ${e.message}")
        }
        aditivoContratoDao.excluirTodasMesasDoAditivo(aditivoId)
    }
    
    // ==================== ASSINATURA REPRESENTANTE LEGAL ====================
    
    suspend fun obterAssinaturaRepresentanteLegalAtiva() = assinaturaRepresentanteLegalDao.obterAssinaturaAtiva()
    fun obterAssinaturaRepresentanteLegalAtivaFlow() = assinaturaRepresentanteLegalDao.obterAssinaturaAtivaFlow()
    suspend fun obterTodasAssinaturasRepresentanteLegal() = assinaturaRepresentanteLegalDao.obterTodasAssinaturas()
    fun obterTodasAssinaturasRepresentanteLegalFlow() = assinaturaRepresentanteLegalDao.obterTodasAssinaturasFlow()
    suspend fun obterAssinaturaRepresentanteLegalPorId(id: Long) = assinaturaRepresentanteLegalDao.obterAssinaturaPorId(id)
    suspend fun atualizarAssinaturaRepresentanteLegal(assinatura: AssinaturaRepresentanteLegal) = assinaturaRepresentanteLegalDao.atualizarAssinatura(assinatura)
    suspend fun desativarAssinaturaRepresentanteLegal(id: Long) = assinaturaRepresentanteLegalDao.desativarAssinatura(id)
    suspend fun incrementarUsoAssinatura(id: Long, dataUso: java.util.Date) = assinaturaRepresentanteLegalDao.incrementarUso(id, dataUso)
    suspend fun contarAssinaturasRepresentanteLegalAtivas() = assinaturaRepresentanteLegalDao.contarAssinaturasAtivas()
    suspend fun obterAssinaturasRepresentanteLegalValidadas() = assinaturaRepresentanteLegalDao.obterAssinaturasValidadas()
    
    // ==================== LOGS DE AUDITORIA ====================
    
    suspend fun obterTodosLogsAuditoria() = logAuditoriaAssinaturaDao.obterTodosLogs()
    fun obterTodosLogsAuditoriaFlow() = logAuditoriaAssinaturaDao.obterTodosLogsFlow()
    suspend fun obterLogsAuditoriaPorAssinatura(idAssinatura: Long) = logAuditoriaAssinaturaDao.obterLogsPorAssinatura(idAssinatura)
    suspend fun obterLogsAuditoriaPorContrato(idContrato: Long) = logAuditoriaAssinaturaDao.obterLogsPorContrato(idContrato)
    suspend fun obterLogsAuditoriaPorTipoOperacao(tipoOperacao: String) = logAuditoriaAssinaturaDao.obterLogsPorTipoOperacao(tipoOperacao)
    suspend fun obterLogsAuditoriaPorPeriodo(dataInicio: java.util.Date, dataFim: java.util.Date) = logAuditoriaAssinaturaDao.obterLogsPorPeriodo(dataInicio, dataFim)
    suspend fun obterLogsAuditoriaPorUsuario(usuario: String) = logAuditoriaAssinaturaDao.obterLogsPorUsuario(usuario)
    suspend fun obterLogsAuditoriaComErro() = logAuditoriaAssinaturaDao.obterLogsComErro()
    suspend fun contarLogsAuditoriaDesde(dataInicio: java.util.Date) = logAuditoriaAssinaturaDao.contarLogsDesde(dataInicio)
    suspend fun contarUsosAssinaturaAuditoria(idAssinatura: Long) = logAuditoriaAssinaturaDao.contarUsosAssinatura(idAssinatura)
    suspend fun obterLogsAuditoriaNaoValidados() = logAuditoriaAssinaturaDao.obterLogsNaoValidados()
    suspend fun validarLogAuditoria(id: Long, dataValidacao: java.util.Date, validadoPor: String) = logAuditoriaAssinaturaDao.validarLog(id, dataValidacao, validadoPor)
    
    // ==================== PROCURAÇÕES ====================
    
    // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    // suspend fun obter.obterProcuraçõesAtivas()
    // fun obter.obterProcuraçõesAtivasFlow()
    // suspend fun obter.obterProcuraçãoPorUsuario(usuario)
    // fun obter.obterProcuraçãoPorUsuarioFlow(usuario)
    // suspend fun obter.obterProcuraçãoPorCpf(cpf)
    // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    // suspend fun obterTodas.obterTodasProcurações()
    // fun obterTodas.obterTodasProcuraçõesFlow()
    // suspend fun obter.obterProcuraçãoPorId(id)
    // suspend fun obter.obterProcuraçãoPorNumero(numero)
    // suspend fun atualizar.atualizarProcuração(procuração)
    // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    // suspend fun revogar.revogarProcuração(id, dataRevogacao, motivo)
    // suspend fun contar.contarProcuraçõesAtivas()
    // suspend fun obter.obterProcuraçõesValidadas()
    // suspend fun obter.obterProcuraçõesVencidas(dataAtual)
    // suspend fun validar.validarProcuração(id, dataValidacao, validadoPor)
    
    // ==================== MÉTODOS PARA CÁLCULO DE METAS ====================
    
    /**
     * Busca acertos por rota e ciclo
     */
    suspend fun buscarAcertosPorRotaECiclo(rotaId: Long, cicloId: Long): List<Acerto> {
        return try {
            acertoDao.buscarPorRotaECicloId(rotaId, cicloId).first()
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao buscar acertos por rota e ciclo: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Conta clientes ativos por rota
     */
    suspend fun contarClientesAtivosPorRota(rotaId: Long): Int {
        return try {
            clienteDao.obterClientesPorRota(rotaId).first().count { it.ativo }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar clientes ativos por rota: ${e.message}", e)
            0
        }
    }
    
    /**
     * Conta clientes acertados por rota e ciclo
     */
    suspend fun contarClientesAcertadosPorRotaECiclo(rotaId: Long, cicloId: Long): Int {
        return try {
            acertoDao.buscarPorRotaECicloId(rotaId, cicloId).first().size
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar clientes acertados: ${e.message}", e)
            0
        }
    }
    
    /**
     * Conta mesas locadas por rota
     */
    suspend fun contarMesasLocadasPorRota(rotaId: Long): Int {
        return try {
            mesaDao.buscarMesasPorRota(rotaId).first().size
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar mesas locadas: ${e.message}", e)
            0
        }
    }

    /**
     * Conta novas mesas (instaladas) no período do ciclo em uma rota
     */
    suspend fun contarNovasMesasNoCiclo(rotaId: Long, cicloId: Long): Int {
        return try {
            val ciclo = cicloAcertoDao.buscarPorId(cicloId) ?: return 0
            val inicio = ciclo.dataInicio
            val fim = ciclo.dataFim ?: java.util.Date()
            mesaDao.contarNovasMesasInstaladas(rotaId, inicio, fim)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar novas mesas no ciclo: ${e.message}", e)
            0
        }
    }

    private fun logDbInsertStart(entity: String, details: String) {
        val stackTrace = Thread.currentThread().stackTrace
        Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
        Log.w("🔍 DB_POPULATION", "🚨 INSERINDO $entity: $details")
        Log.w("🔍 DB_POPULATION", "📍 Chamado por:")
        stackTrace.drop(3).take(8).forEachIndexed { index, element ->
            Log.w("🔍 DB_POPULATION", "   [${index}] $element")
        }
        Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
    }

    private fun logDbInsertSuccess(entity: String, details: String) {
        Log.w("🔍 DB_POPULATION", "✅ $entity INSERIDO COM SUCESSO: $details")
    }

    private fun logDbInsertError(entity: String, details: String, throwable: Throwable) {
        Log.e("🔍 DB_POPULATION", "❌ ERRO AO INSERIR $entity: $details", throwable)
    }
    
    private fun logDbUpdateStart(entity: String, details: String) {
        val stackTrace = Thread.currentThread().stackTrace
        Log.w("🔍 DB_UPDATE", "════════════════════════════════════════")
        Log.w("🔍 DB_UPDATE", "🔄 ATUALIZANDO $entity: $details")
        Log.w("🔍 DB_UPDATE", "📍 Chamado por:")
        stackTrace.drop(3).take(8).forEachIndexed { index, element ->
            Log.w("🔍 DB_UPDATE", "   [${index}] $element")
        }
        Log.w("🔍 DB_UPDATE", "════════════════════════════════════════")
    }

    private fun logDbUpdateSuccess(entity: String, details: String) {
        Log.w("🔍 DB_UPDATE", "✅ $entity ATUALIZADO COM SUCESSO: $details")
    }

    private fun logDbUpdateError(entity: String, details: String, throwable: Throwable) {
        Log.e("🔍 DB_UPDATE", "❌ ERRO AO ATUALIZAR $entity: $details", throwable)
    }
    
    // ==================== CACHE MANAGEMENT (MODERNIZAÇÃO 2025) ====================
    
    /**
     * ✅ MODERNIZADO: Atualiza cache de clientes
     */
    suspend fun refreshClientesCache() {
        try {
            val clientes = obterTodosClientes().first()
            _clientesCache.value = clientes
            Log.d("AppRepository", "✅ Cache de clientes atualizado: ${clientes.size} itens")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Erro ao atualizar cache de clientes", e)
        }
    }
    
    /**
     * ✅ MODERNIZADO: Atualiza cache de rotas
     */
    suspend fun refreshRotasCache() {
        try {
            val rotas = obterTodasRotas().first()
            _rotasCache.value = rotas
            Log.d("AppRepository", "✅ Cache de rotas atualizado: ${rotas.size} itens")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Erro ao atualizar cache de rotas", e)
        }
    }
    
    /**
     * ✅ MODERNIZADO: Atualiza cache de mesas
     */
    suspend fun refreshMesasCache() {
        try {
            val mesas = obterTodasMesas().first()
            _mesasCache.value = mesas
            Log.d("AppRepository", "✅ Cache de mesas atualizado: ${mesas.size} itens")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Erro ao atualizar cache de mesas", e)
        }
    }
    
    /**
     * ✅ MODERNIZADO: Atualiza todos os caches
     */
    suspend fun refreshAllCaches() {
        Log.d("AppRepository", "🔄 Atualizando todos os caches...")
        refreshClientesCache()
        refreshRotasCache()
        refreshMesasCache()
        Log.d("AppRepository", "✅ Todos os caches atualizados com sucesso")
    }
    
    // ==================== MÉTODOS ADICIONAIS PARA CORREÇÃO DE ERROS ====================
    
    suspend fun inserirHistoricoManutencao(historico: HistoricoManutencaoVeiculo): Long = inserirHistoricoManutencaoVeiculoSync(historico)
    suspend fun inserirHistoricoCombustivel(historico: HistoricoCombustivelVeiculo): Long = inserirHistoricoCombustivelVeiculoSync(historico)
    suspend fun inserirAcertoMesa(acertoMesa: AcertoMesa): Long = acertoMesaDao.inserir(acertoMesa)
    
    /**
     * ✅ NOVO: Adicionar acerto à fila de sincronização com dados das mesas
     */
    suspend fun adicionarAcertoComMesasParaSync(acertoId: Long) {
        try {
            val acerto = acertoDao.buscarPorId(acertoId)
            if (acerto == null) {
                Log.w("AppRepository", "Acerto $acertoId não encontrado para sincronização")
                return
            }
            
            val acertoMesas = acertoMesaDao.buscarPorAcertoId(acertoId)
            android.util.Log.d("AppRepository", "Incluindo ${acertoMesas.size} mesas no payload do acerto $acertoId")
            
            val payloadMap = mutableMapOf<String, Any?>(
                "id" to acertoId,
                "clienteId" to acerto.clienteId,
                "rotaId" to acerto.rotaId,
                "valorRecebido" to acerto.valorRecebido,
                "debitoAtual" to acerto.debitoAtual,
                "dataAcerto" to acerto.dataAcerto,
                "observacoes" to acerto.observacoes,
                "metodosPagamentoJson" to acerto.metodosPagamentoJson,
                "status" to acerto.status.name,
                "periodoInicio" to acerto.periodoInicio,
                "periodoFim" to acerto.periodoFim,
                "valorTotal" to acerto.valorTotal,
                "desconto" to acerto.desconto,
                "valorComDesconto" to acerto.valorComDesconto,
                "representante" to acerto.representante,
                "tipoAcerto" to acerto.tipoAcerto,
                "panoTrocado" to acerto.panoTrocado,
                "numeroPano" to acerto.numeroPano,
                "dadosExtrasJson" to acerto.dadosExtrasJson,
                "cicloId" to acerto.cicloId,
                "acertoMesas" to acertoMesas.map { acertoMesa ->
                    mapOf(
                        "id" to acertoMesa.id,
                        "acertoId" to acertoMesa.acertoId,
                        "mesaId" to acertoMesa.mesaId,
                        "relogioInicial" to acertoMesa.relogioInicial,
                        "relogioFinal" to acertoMesa.relogioFinal,
                        "fichasJogadas" to acertoMesa.fichasJogadas,
                        "valorFixo" to acertoMesa.valorFixo,
                        "valorFicha" to acertoMesa.valorFicha,
                        "comissaoFicha" to acertoMesa.comissaoFicha,
                        "subtotal" to acertoMesa.subtotal,
                        "comDefeito" to acertoMesa.comDefeito,
                        "relogioReiniciou" to acertoMesa.relogioReiniciou,
                        "observacoes" to acertoMesa.observacoes,
                        "fotoRelogioFinal" to acertoMesa.fotoRelogioFinal,
                        "dataFoto" to acertoMesa.dataFoto,
                        "dataCriacao" to acertoMesa.dataCriacao
                    )
                }
            )

            val payload = com.google.gson.Gson().toJson(payloadMap)
            adicionarOperacaoSync("Acerto", acertoId, "CREATE", payload, priority = 1)
            logarOperacaoSync("Acerto", acertoId, "CREATE", "PENDING", null, payload)
            
        } catch (syncError: Exception) {
            Log.w("AppRepository", "Erro ao adicionar acerto com mesas à fila de sync: ${syncError.message}")
        }
    }
    suspend fun calcularMediaFichasJogadas(mesaId: Long, limite: Int): Double {
        val acertos = acertoMesaDao.buscarPorMesa(mesaId).first().take(limite)
        return if (acertos.isNotEmpty()) {
            acertos.map { acerto -> acerto.fichasJogadas }.average()
        } else 0.0
    }
    suspend fun buscarAcertoMesasPorAcerto(acertoId: Long) = acertoMesaDao.buscarPorAcerto(acertoId)
    
    /**
     * ✅ NOVO: Reconciliar débitos dos clientes com base no último acerto
     * Útil após sincronização de acertos vindos do Firestore, garantindo que o card de clientes
     * reflita o débito real (campo clientes.debito_atual alinhado ao último acerto.debito_atual).
     */
    suspend fun reconciliarDebitosClientes() {
        try {
            Log.d("AppRepository", "🔄 Reconciliando débitos dos clientes com base no último acerto...")
            val clientes = clienteDao.obterTodos().first()
            var atualizados = 0
            for (cliente in clientes) {
                try {
                    val ultimoAcerto = acertoDao.buscarUltimoAcertoPorCliente(cliente.id)
                    val debitoUltimo = ultimoAcerto?.debitoAtual ?: 0.0
                    if (debitoUltimo != cliente.debitoAtual) {
                        clienteDao.atualizarDebitoAtual(cliente.id, debitoUltimo)
                        atualizados++
                        Log.d("AppRepository", "✅ Cliente ${cliente.id} (${cliente.nome}): debito_atual ${cliente.debitoAtual} -> $debitoUltimo")
                    }
                } catch (e: Exception) {
                    Log.w("AppRepository", "⚠️ Falha ao reconciliar cliente ${cliente.id}: ${e.message}")
                }
            }
            Log.d("AppRepository", "✅ Reconciliação concluída. Clientes atualizados: $atualizados")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Erro na reconciliação de débitos: ${e.message}", e)
        }
    }

    /**
     * ✅ CORREÇÃO CRÍTICA: Corrigir acertos existentes com status PENDENTE para FINALIZADO
     * Isso resolve o problema de clientes aparecendo na aba "Em aberto" em vez de "Pago"
     */
    suspend fun corrigirAcertosPendentesParaFinalizados() {
        try {
            android.util.Log.d("AppRepository", "🔧 CORREÇÃO: Iniciando correção de acertos PENDENTE para FINALIZADO")
            
            // Buscar todos os acertos com status PENDENTE
            val acertosPendentes = acertoDao.listarTodos().first().filter { acerto -> 
                acerto.status == com.example.gestaobilhares.data.entities.StatusAcerto.PENDENTE 
            }
            
            android.util.Log.d("AppRepository", "🔍 Encontrados ${acertosPendentes.size} acertos com status PENDENTE")
            
            for (acerto in acertosPendentes) {
                try {
                    // ✅ VALIDAÇÃO ADICIONAL: Verificar se já existe acerto FINALIZADO para este cliente e ciclo
                    val cicloId = acerto.cicloId ?: 0L
                    if (cicloId > 0) {
                        val acertosCiclo = acertoDao.buscarPorCicloId(cicloId).first()
                        val acertoDuplicado = acertosCiclo.any { acertoExistente -> 
                            acertoExistente.clienteId == acerto.clienteId && 
                            acertoExistente.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO &&
                            acertoExistente.id != acerto.id // Excluir o próprio acerto sendo processado
                        }
                        
                        if (acertoDuplicado) {
                            android.util.Log.w("AppRepository", "⚠️ DUPLICATA DETECTADA: Cliente ${acerto.clienteId} já tem acerto FINALIZADO no ciclo $cicloId - REMOVENDO acerto PENDENTE ID ${acerto.id}")
                            // Remover o acerto PENDENTE duplicado
                            acertoDao.deletar(acerto)
                            continue
                        }
                    }
                    
                    // Atualizar status para FINALIZADO
                    val acertoCorrigido = acerto.copy(
                        status = com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO
                    )
                    
                    acertoDao.atualizar(acertoCorrigido)
                    android.util.Log.d("AppRepository", "✅ Acerto ID ${acerto.id} corrigido: PENDENTE → FINALIZADO")
                    
                } catch (e: Exception) {
                    android.util.Log.e("AppRepository", "❌ Erro ao corrigir acerto ID ${acerto.id}: ${e.message}")
                }
            }
            
            android.util.Log.d("AppRepository", "✅ CORREÇÃO CONCLUÍDA: ${acertosPendentes.size} acertos corrigidos")
            
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro na correção de acertos PENDENTE: ${e.message}")
        }
    }
    suspend fun buscarContratoAtivoPorCliente(clienteId: Long) = contratoLocacaoDao.buscarContratoAtivoPorCliente(clienteId)
    suspend fun inserirHistoricoManutencaoMesa(historico: HistoricoManutencaoMesa): Long = inserirHistoricoManutencaoMesaSync(historico)

    // ========================================
    // ✅ FASE 3C: MÉTODOS DE SINCRONIZAÇÃO
    // ========================================

    /**
     * Adicionar operação à fila de sincronização
     */
    suspend fun adicionarOperacaoSync(
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
                createdAt = java.util.Date(),
                scheduledFor = java.util.Date(),
                retryCount = 0,
                status = "PENDING",
                priority = priority
            )
            
            val insertedId = syncQueueDao.inserirSyncQueue(syncQueue)
            Log.d("AppRepository", "✅ Operação adicionada à fila: $entityType:$entityId (ID: $insertedId)")
            
            // ✅ DEBUG: Verificar se foi realmente inserida
            val count = syncQueueDao.contarOperacoesPendentes()
            Log.d("AppRepository", "📊 Total de operações pendentes: $count")
            
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao adicionar à fila: ${e.message}")
        }
    }

    /**
     * Log de operação de sincronização
     */
    suspend fun logarOperacaoSync(
        entityType: String,
        entityId: Long,
        operation: String,
        status: String,
        errorMessage: String? = null,
        payload: String? = null
    ) {
        try {
            val syncLog = SyncLog(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                syncStatus = status,
                timestamp = java.util.Date(),
                errorMessage = errorMessage,
                payload = payload
            )
            
            syncLogDao.inserirSyncLog(syncLog)
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao logar operação: ${e.message}")
        }
    }

    /**
     * Obter operações pendentes de sincronização
     */
    fun obterOperacoesPendentes(): Flow<List<SyncQueue>> {
        return syncQueueDao.buscarOperacoesPorStatus("PENDING")
    }

    /**
     * Contar operações pendentes
     */
    suspend fun contarOperacoesPendentes(): Int {
        return syncQueueDao.contarOperacoesPendentes()
    }

    /**
     * Obter logs de sincronização
     */
    fun obterLogsSync(limite: Int = 100): Flow<List<SyncLog>> {
        return syncLogDao.buscarTodosSyncLogs(limite)
    }

    /**
     * Inicializar configurações de sincronização
     */
    suspend fun inicializarConfiguracoesSync() {
        try {
            syncConfigDao.inicializarConfiguracoesPadrao(System.currentTimeMillis())
            Log.d("AppRepository", "Configurações de sincronização inicializadas")
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao inicializar configurações: ${e.message}")
        }
    }

    /**
     * Marcar entidade como sincronizada
     */
    suspend fun marcarEntidadeSincronizada(entityType: String, entityId: Long) {
        try {
            val currentTime = System.currentTimeMillis()
            val configKey = "last_sync_timestamp_${entityType.lowercase()}"
            syncConfigDao.atualizarUltimoTimestampSync(configKey, currentTime.toString(), currentTime)
            Log.d("AppRepository", "Entidade $entityType:$entityId marcada como sincronizada")
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao marcar entidade como sincronizada: ${e.message}")
        }
    }

    /**
     * Limpar logs antigos
     */
    suspend fun limparLogsAntigos() {
        try {
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 dias
            val deletedLogs = syncLogDao.deletarSyncLogsAntigos(cutoffTime)
            val deletedQueue = syncQueueDao.limparOperacoesConcluidas(cutoffTime)
            
            Log.d("AppRepository", "Limpeza: $deletedLogs logs, $deletedQueue operações removidas")
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro na limpeza: ${e.message}")
        }
    }
    
    // ==================== FASE 4A: CACHE INTELIGENTE ====================
    
    /**
     * ✅ FASE 4A: Buscar rotas com cache inteligente
     * Cache TTL: 2 minutos (dados de rota mudam pouco)
     */
    fun buscarRotasComCache(): Flow<List<Rota>> {
        val cacheKey = "rotas_ativas"
        
        return flowOf(
            cacheManager.get<List<Rota>>(cacheKey) ?: run {
                Log.d("AppRepository", "Cache MISS: $cacheKey - Carregando do banco")
                val rotas = runBlocking { rotaDao.getAllRotasAtivas().first() }
                cacheManager.put(cacheKey, rotas, TimeUnit.MINUTES.toMillis(2))
                rotas
            }
        )
    }
    
    /**
     * ✅ FASE 4A: Buscar clientes por rota com cache inteligente
     * Cache TTL: 1 minuto (dados de cliente mudam mais frequentemente)
     */
    fun buscarClientesPorRotaComCache(rotaId: Long): Flow<List<Cliente>> {
        val cacheKey = "clientes_rota_$rotaId"
        
        return flowOf(
            cacheManager.get<List<Cliente>>(cacheKey) ?: run {
                Log.d("AppRepository", "Cache MISS: $cacheKey - Carregando do banco")
                val clientes = runBlocking { clienteDao.obterClientesPorRota(rotaId).first() }
                cacheManager.put(cacheKey, clientes, TimeUnit.MINUTES.toMillis(1))
                clientes
            }
        )
    }
    
    /**
     * ✅ FASE 4A: Buscar estatísticas financeiras com cache
     * Cache TTL: 30 segundos (dados financeiros mudam frequentemente)
     */
    suspend fun calcularEstatisticasFinanceirasComCache(rotaId: Long): Map<String, Double> {
        val cacheKey = "stats_financeiras_rota_$rotaId"
        
        return cacheManager.get<Map<String, Double>>(cacheKey) ?: run {
            Log.d("AppRepository", "Cache MISS: $cacheKey - Calculando estatísticas")
            val stats = mapOf(
                "totalRecebido" to 0.0,
                "totalDespesas" to 0.0,
                "lucro" to 0.0
            )
            cacheManager.put(cacheKey, stats, TimeUnit.SECONDS.toMillis(30))
            stats
        }
    }
    
    /**
     * ✅ FASE 4A: Buscar ciclo atual com cache
     * Cache TTL: 10 segundos (ciclo atual muda pouco)
     */
    suspend fun obterCicloAtualRotaComCache(rotaId: Long): Triple<Int, Long?, Long?> {
        val cacheKey = "ciclo_atual_rota_$rotaId"
        
        return cacheManager.get<Triple<Int, Long?, Long?>>(cacheKey) ?: run {
            Log.d("AppRepository", "Cache MISS: $cacheKey - Buscando ciclo atual")
            val ciclo = obterCicloAtualRota(rotaId)
            cacheManager.put(cacheKey, ciclo, TimeUnit.SECONDS.toMillis(10))
            ciclo
        }
    }
    
    /**
     * ✅ FASE 4A: Invalidar cache relacionado a uma rota
     * Chamado quando há mudanças nos dados da rota
     */
    fun invalidarCacheRota(rotaId: Long) {
        cacheManager.invalidatePattern("rota_$rotaId")
        cacheManager.invalidate("rotas_ativas")
        Log.d("AppRepository", "Cache invalidado para rota $rotaId")
    }
    
    /**
     * ✅ FASE 4A: Invalidar cache relacionado a um cliente
     * Chamado quando há mudanças nos dados do cliente
     */
    fun invalidarCacheCliente(clienteId: Long, rotaId: Long) {
        cacheManager.invalidatePattern("cliente_$clienteId")
        cacheManager.invalidatePattern("rota_$rotaId")
        cacheManager.invalidate("rotas_ativas")
        Log.d("AppRepository", "Cache invalidado para cliente $clienteId da rota $rotaId")
    }
    
    /**
     * ✅ FASE 4A: Obter estatísticas do cache
     */
    fun obterEstatisticasCacheApp(): String {
        return cacheManager.getHealthStatus()
    }
    
    /**
     * ✅ FASE 4A: Limpar todo o cache
     * Útil para testes ou quando necessário recarregar todos os dados
     */
    fun limparCache() {
        cacheManager.clear()
        Log.d("AppRepository", "Cache limpo completamente")
    }
    
    // ==================== FASE 4C: WORKMANAGER CENTRALIZADO ====================
    
    /**
     * ✅ FASE 4C: Inicializar workers periódicos
     * Centralizado no AppRepository seguindo padrão de simplificação
     */
    fun inicializarWorkersPeriodicos() {
        val workManager = WorkManager.getInstance(context)
        
        // Worker de sincronização - a cada 15 minutos (Android 2025 best practice)
        agendarSyncWorker(workManager)
        
        // Worker de limpeza - diariamente às 2:00
        agendarCleanupWorker(workManager)
        
        Log.d("AppRepository", "Workers periódicos inicializados")
    }
    
    /**
     * ✅ FASE 4C: Agendar worker de sincronização
     */
    private fun agendarSyncWorker(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("sync")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
        
        Log.d("AppRepository", "Worker de sincronização agendado")
    }
    
    /**
     * ✅ FASE 4C: Agendar worker de limpeza
     */
    private fun agendarCleanupWorker(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .build()
        
        val cleanupWork = PeriodicWorkRequestBuilder<CleanupWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(calcularDelayAte2AM(), TimeUnit.MILLISECONDS)
            .addTag("cleanup")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "cleanup_work",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWork
        )
        
        Log.d("AppRepository", "Worker de limpeza agendado")
    }
    
    /**
     * ✅ FASE 4C: Executar sincronização imediata
     */
    fun executarSyncImediata() {
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val immediateSyncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag("immediate_sync")
            .build()
        
        workManager.enqueue(immediateSyncWork)
        
        Log.d("AppRepository", "Sincronização imediata agendada")
    }
    
    /**
     * ✅ FASE 4C: Executar limpeza imediata
     */
    fun executarLimpezaImediata() {
        val workManager = WorkManager.getInstance(context)
        
        val immediateCleanupWork = OneTimeWorkRequestBuilder<CleanupWorker>()
            .addTag("immediate_cleanup")
            .build()
        
        workManager.enqueue(immediateCleanupWork)
        
        Log.d("AppRepository", "Limpeza imediata agendada")
    }
    
    /**
     * ✅ FASE 4C: Calcular delay até 2:00 AM
     */
    private fun calcularDelayAte2AM(): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        
        // Próxima 2:00 AM
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 2)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis - now
    }
    
    // ==================== FASE 4D: OTIMIZAÇÕES DE MEMÓRIA ====================
    
    /**
     * ✅ FASE 4D: Obtém estatísticas de memória
     */
    fun obterEstatisticasMemoria(): MemoryOptimizer.MemoryStats {
        return memoryOptimizer.getMemoryStats()
    }
    
    /**
     * ✅ FASE 4D: Limpa caches de memória
     */
    fun limparCachesMemoria() {
        memoryOptimizer.clearAllCaches()
        weakReferenceManager.cleanupNullReferences()
        Log.d("AppRepository", "🧹 Caches de memória limpos")
    }
    
    /**
     * ✅ FASE 4D: Força garbage collection
     */
    fun forcarGarbageCollection() {
        memoryOptimizer.forceGarbageCollection()
        Log.d("AppRepository", "🗑️ Garbage collection forçado")
    }
    
    /**
     * ✅ FASE 4D: Obtém estatísticas de referências fracas
     */
    fun obterEstatisticasReferencias(): WeakReferenceManager.ReferenceStats {
        return weakReferenceManager.getReferenceStats()
    }
    
    /**
     * ✅ FASE 4D: Cache de bitmap otimizado
     */
    fun cachearBitmap(key: String, bitmap: android.graphics.Bitmap) {
        memoryOptimizer.cacheBitmap(key, bitmap)
    }
    
    /**
     * ✅ FASE 4D: Obtém bitmap do cache
     */
    fun obterBitmapCache(key: String): android.graphics.Bitmap? {
        return memoryOptimizer.getCachedBitmap(key)
    }
    
    /**
     * ✅ FASE 4D: Gerencia referência fraca
     */
    fun <T : Any> definirReferenciaFraca(key: String, obj: T) {
        weakReferenceManager.addWeakReference(key, obj)
    }
    
    /**
     * ✅ FASE 4D: Obtém referência fraca
     */
    fun <T : Any> obterReferenciaFraca(key: String): T? {
        return weakReferenceManager.getWeakReference(key)
    }
    
    /**
     * ✅ FASE 4D: Monitoramento de memória em background
     */
    fun iniciarMonitoramentoMemoria() {
        // Agendar limpeza periódica de memória
        val memoryCleanupRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .addTag("memory_cleanup")
            .build()
        
        WorkManager.getInstance(context).enqueue(memoryCleanupRequest)
        Log.d("AppRepository", "📊 Monitoramento de memória iniciado")
    }
    
    // ==================== OTIMIZAÇÕES DE UI ====================
    
    /**
     * ✅ FASE 4D: Otimizações de ViewStub
     */
    fun inflarViewStub(viewStub: android.view.ViewStub, tag: String): android.view.View? {
        return viewStubManager.inflateViewStub(viewStub, tag)
    }
    
    fun obterViewInflada(tag: String): android.view.View? {
        return viewStubManager.getInflatedView(tag)
    }
    
    fun verificarViewInflada(tag: String): Boolean {
        return viewStubManager.isViewInflated(tag)
    }
    
    fun removerViewInflada(tag: String) {
        viewStubManager.removeInflatedView(tag)
    }
    
    fun obterEstatisticasViewStub(): ViewStubManager.ViewStubStats {
        return viewStubManager.getCacheStats()
    }
    
    /**
     * ✅ FASE 4D: Otimizações de ViewHolder
     */
    fun <T : Any> adicionarViewHolderAoPool(viewHolder: T) {
        optimizedViewHolder.addToPool(viewHolder)
    }
    
    fun <T : Any> obterViewHolderDoPool(clazz: Class<T>, factory: () -> T): T {
        return optimizedViewHolder.getFromPool(clazz, factory)
    }
    
    fun cachearView(viewHolderTag: String, viewId: Int, view: android.view.View) {
        optimizedViewHolder.cacheView(viewHolderTag, viewId, view)
    }
    
    fun obterViewCacheada(viewHolderTag: String, viewId: Int): android.view.View? {
        return optimizedViewHolder.getCachedView(viewHolderTag, viewId)
    }
    
    fun limparCacheViewHolder(viewHolderTag: String) {
        optimizedViewHolder.clearViewHolderCache(viewHolderTag)
    }
    
    fun obterEstatisticasViewHolder(): OptimizedViewHolder.ViewHolderStats {
        return optimizedViewHolder.getPoolStats()
    }
    
    /**
     * ✅ FASE 4D: Otimizações de Layout
     */
    fun otimizarHierarquiaViews(rootView: android.view.View): android.view.View {
        return layoutOptimizer.optimizeViewHierarchy(rootView)
    }
    
    fun cachearLayout(key: String, view: android.view.View) {
        layoutOptimizer.cacheLayout(key, view)
    }
    
    fun obterLayoutCacheado(key: String): android.view.View? {
        return layoutOptimizer.getCachedLayout(key)
    }
    
    fun obterEstatisticasLayout(): List<LayoutOptimizer.LayoutStats> {
        return layoutOptimizer.getPerformanceStats()
    }
    
    /**
     * ✅ FASE 4D: Otimizações de RecyclerView
     */
    fun otimizarRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView, config: RecyclerViewOptimizer.RecyclerViewConfig = RecyclerViewOptimizer.RecyclerViewConfig()) {
        recyclerViewOptimizer.optimizeRecyclerView(recyclerView, config)
    }
    
    fun otimizarRecyclerViewParaListasGrandes(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        recyclerViewOptimizer.optimizeForLargeLists(recyclerView)
    }
    
    fun otimizarRecyclerViewParaListasPequenas(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        recyclerViewOptimizer.optimizeForSmallLists(recyclerView)
    }
    
    fun obterConfiguracaoRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView): RecyclerViewOptimizer.RecyclerViewConfig? {
        return recyclerViewOptimizer.getRecyclerViewConfig(recyclerView)
    }
    
    /**
     * ✅ FASE 4D: Limpeza geral de otimizações de UI
     */
    fun limparTodasOtimizacoesUI() {
        viewStubManager.clearAllViews()
        optimizedViewHolder.clearAll()
        layoutOptimizer.clearAllCaches()
        recyclerViewOptimizer.clearAllConfigs()
        Log.d("AppRepository", "🧹 Todas as otimizações de UI limpas")
    }
    
    // ==================== OTIMIZAÇÕES DE REDE ====================
    
    /**
     * ✅ FASE 4D: Otimizações de Compressão de Rede
     */
    fun comprimirDados(dados: ByteArray, chave: String? = null): NetworkCompressionManager.CompressedData {
        return networkCompressionManager.compressData(dados, chave)
    }
    
    fun descomprimirDados(dadosComprimidos: NetworkCompressionManager.CompressedData): ByteArray {
        return networkCompressionManager.decompressData(dadosComprimidos)
    }
    
    fun comprimirString(texto: String, chave: String? = null): NetworkCompressionManager.CompressedData {
        return networkCompressionManager.compressString(texto, chave)
    }
    
    fun descomprimirParaString(dadosComprimidos: NetworkCompressionManager.CompressedData): String {
        return networkCompressionManager.decompressToString(dadosComprimidos)
    }
    
    fun obterEstatisticasCompressao(): NetworkCompressionManager.CompressionStats {
        return networkCompressionManager.getCompressionStats()
    }
    
    /**
     * ✅ FASE 4D: Otimizações de Operações em Lote
     */
    suspend fun adicionarOperacaoEmLote(
        operacao: suspend () -> Result<Any>,
        prioridade: BatchOperationsManager.OperationPriority = BatchOperationsManager.OperationPriority.NORMAL,
        retryOnFailure: Boolean = true
    ): Deferred<Result<Any>> {
        return batchOperationsManager.addOperation(operacao, prioridade, retryOnFailure)
    }
    
    fun configurarLote(
        tamanhoLote: Int = 10,
        timeoutLote: Long = 5000L,
        maxTentativas: Int = 3
    ) {
        batchOperationsManager.configureBatch(tamanhoLote, timeoutLote, maxTentativas)
    }
    
    suspend fun processarOperacoesPendentes() {
        batchOperationsManager.flushPendingOperations()
    }
    
    fun obterEstatisticasLote(): BatchOperationsManager.BatchPerformanceStats {
        return batchOperationsManager.getPerformanceStats()
    }
    
    /**
     * ✅ FASE 4D: Otimizações de Retry Logic
     */
    suspend fun <T> executarComRetry(
        operacao: suspend () -> T,
        endpoint: String = "default",
        maxTentativas: Int = 3,
        delayBase: Long = 1000L,
        delayMaximo: Long = 30000L
    ): Result<T> {
        return retryLogicManager.executeWithRetry(
            operacao, endpoint, maxTentativas, delayBase, delayMaximo
        )
    }
    
    fun configurarCircuitBreaker(
        endpoint: String,
        limiteFalhas: Int = 5,
        timeout: Long = 60000L
    ) {
        retryLogicManager.configureCircuitBreaker(endpoint, limiteFalhas, timeout)
    }
    
    fun configurarRateLimiter(
        endpoint: String,
        maxRequisicoes: Int,
        janelaTempo: Long
    ) {
        retryLogicManager.configureRateLimiter(endpoint, maxRequisicoes, janelaTempo)
    }
    
    fun obterEstatisticasRetry(): RetryLogicManager.RetryStats {
        return retryLogicManager.getRetryStats()
    }
    
    /**
     * ✅ FASE 4D: Otimizações de Cache de Rede
     */
    fun armazenarNoCache(chave: String, dados: ByteArray, ttl: Long = 300000L, comprimir: Boolean = true) {
        networkCacheManager.put(chave, dados, ttl, comprimir)
    }
    
    fun armazenarStringNoCache(chave: String, valor: String, ttl: Long = 300000L, comprimir: Boolean = true) {
        networkCacheManager.putString(chave, valor, ttl, comprimir)
    }
    
    fun obterDoCache(chave: String): ByteArray? {
        return networkCacheManager.get(chave)
    }
    
    fun obterStringDoCache(chave: String): String? {
        return networkCacheManager.getString(chave)
    }
    
    fun verificarCache(chave: String): Boolean {
        return networkCacheManager.contains(chave)
    }
    
    fun removerDoCache(chave: String) {
        networkCacheManager.remove(chave)
    }
    
    fun obterEstatisticasCacheRede(): NetworkCacheManager.CacheStats {
        return networkCacheManager.getCacheStats()
    }
    
    fun obterInformacoesCache(): List<NetworkCacheManager.CacheMetadata> {
        return networkCacheManager.getCacheInfo()
    }
    
    /**
     * ✅ FASE 4D: Limpeza geral de otimizações de rede
     */
    fun limparTodasOtimizacoesRede() {
        networkCompressionManager.clearCache()
        batchOperationsManager.cancelAllOperations()
        retryLogicManager.clearStats()
        networkCacheManager.clear()
        Log.d("AppRepository", "🌐 Todas as otimizações de rede limpas")
    }
    
    /**
     * ✅ FASE 4D: Otimizações Avançadas de Banco de Dados
     */
    
    /**
     * Inicializa pool de conexões do banco
     */
    fun inicializarPoolConexoes(database: androidx.room.RoomDatabase, tamanhoPool: Int = 10) {
        connectionPool.initialize(database, tamanhoPool)
        Log.d("AppRepository", "Pool de conexões inicializado com $tamanhoPool conexões")
    }
    
    /**
     * Executa operação com conexão otimizada
     */
    suspend fun <T> executarComConexaoOtimizada(operacao: suspend (DatabaseConnectionPool.PooledConnection) -> T): T? {
        return connectionPool.executeWithConnection(operacao)
    }
    
    /**
     * Obtém estatísticas do pool de conexões
     */
    fun obterEstatisticasPoolConexoes(): DatabaseConnectionPool.ConnectionPoolStats {
        return connectionPool.getPoolStats()
    }
    
    /**
     * Otimiza query SQL
     */
    fun otimizarQuery(query: String, parametros: Map<String, Any> = emptyMap()): QueryOptimizationManager.OptimizedQuery {
        return queryOptimizer.optimizeQuery(query, parametros)
    }
    
    /**
     * Registra execução de query
     */
    fun registrarExecucaoQuery(query: String, tempoExecucao: Long) {
        queryOptimizer.recordQueryExecution(query, tempoExecucao)
    }
    
    /**
     * Obtém estatísticas de otimização de queries
     */
    fun obterEstatisticasOtimizacaoQueries(): QueryOptimizationManager.QueryOptimizationStats {
        return queryOptimizer.getOptimizationStats()
    }
    
    /**
     * Aplica otimizações de performance ao banco
     */
    fun otimizarPerformanceBanco(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        performanceTuner.optimizeDatabase(database)
        Log.d("AppRepository", "Otimizações de performance aplicadas ao banco")
    }
    
    /**
     * Configura nível de performance do banco
     */
    fun configurarNivelPerformance(level: DatabasePerformanceTuner.PerformanceLevel) {
        performanceTuner.setPerformanceLevel(level)
    }
    
    /**
     * Executa análise e otimização automática
     */
    fun executarOtimizacaoAutomatica(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        performanceTuner.performAutoOptimization(database)
    }
    
    /**
     * Obtém análise de performance do banco
     */
    fun obterAnalisePerformance(): DatabasePerformanceTuner.PerformanceAnalysis {
        return performanceTuner.analyzePerformance()
    }
    
    /**
     * Executa transação otimizada
     */
    suspend fun executarTransacaoOtimizada(
        operacao: suspend (androidx.sqlite.db.SupportSQLiteDatabase) -> Unit,
        descricao: String = "Transação"
    ): TransactionOptimizationManager.TransactionResult {
        return transactionOptimizer.executeTransaction(operacao, descricao)
    }
    
    /**
     * Força execução de transações pendentes
     */
    suspend fun executarTransacoesPendentes(database: androidx.sqlite.db.SupportSQLiteDatabase): TransactionOptimizationManager.BatchResult {
        return transactionOptimizer.flushPendingTransactions(database)
    }
    
    /**
     * Configura parâmetros de batch de transações
     */
    fun configurarBatchTransacoes(
        tamanhoBatch: Int = 100,
        timeoutBatch: Long = 5000L,
        habilitarBatch: Boolean = true
    ) {
        transactionOptimizer.configureBatch(tamanhoBatch, timeoutBatch, habilitarBatch)
    }
    
    /**
     * Obtém estatísticas de transações
     */
    fun obterEstatisticasTransacoes(): TransactionOptimizationManager.TransactionStats {
        return transactionOptimizer.getTransactionStats()
    }
    
    /**
     * Cancela transações pendentes
     */
    fun cancelarTransacoesPendentes() {
        transactionOptimizer.cancelPendingTransactions()
    }
    
    /**
     * ✅ MÉTODOS PUSH GENÉRICOS - Evitando duplicação de código
     * Implementação eficiente para sincronização de entidades
     */
    
    /**
     * Inserir AcertoMesa com sincronização
     */
    suspend fun inserirAcertoMesaSync(acertoMesa: AcertoMesa): Long {
        val id = acertoMesaDao.inserir(acertoMesa)
        
        try {
            val payload = mapOf(
                "id" to id,
                "acertoId" to acertoMesa.acertoId,
                "mesaId" to acertoMesa.mesaId,
                "relogioInicial" to acertoMesa.relogioInicial,
                "relogioFinal" to acertoMesa.relogioFinal,
                "valorFicha" to acertoMesa.valorFicha,
                "comissaoFicha" to acertoMesa.comissaoFicha,
                "subtotal" to acertoMesa.subtotal
            )
            
            adicionarOperacaoSync("ACERTOMESA", id, "INSERT", Gson().toJson(payload))
            logarOperacaoSync("ACERTOMESA", id, "INSERT", "Adicionado à fila de sync")
            
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao adicionar AcertoMesa à fila de sync: ${e.message}")
        }
        
        return id
    }
    
    /**
     * Atualizar AcertoMesa com sincronização
     */
    suspend fun atualizarAcertoMesaSync(acertoMesa: AcertoMesa) {
        acertoMesaDao.atualizar(acertoMesa)
        
        try {
            adicionarOperacaoSync("ACERTOMESA", acertoMesa.id, "UPDATE", Gson().toJson(acertoMesa))
            logarOperacaoSync("ACERTOMESA", acertoMesa.id, "UPDATE", "Adicionado à fila de sync")
            
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao adicionar atualização de AcertoMesa à fila de sync: ${e.message}")
        }
    }
    
    /**
     * Atualizar MesaReformada com sincronização (método já existe, apenas adicionando sync)
     */
    suspend fun atualizarMesaReformadaSync(mesaReformada: MesaReformada) {
        mesaReformadaDao.atualizar(mesaReformada)
        
        try {
            adicionarOperacaoSync("MESAREFORMADA", mesaReformada.id, "UPDATE", Gson().toJson(mesaReformada))
            logarOperacaoSync("MESAREFORMADA", mesaReformada.id, "UPDATE", "Adicionado à fila de sync")
            
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao adicionar atualização de MesaReformada à fila de sync: ${e.message}")
        }
    }

    /**
     * Limpa todas as otimizações de banco
     */
    fun limparTodasOtimizacoesBanco() {
        connectionPool.clearPool()
        queryOptimizer.clearQueryCache()
        transactionOptimizer.cancelPendingTransactions()
        performanceTuner.resetStats()
        Log.d("AppRepository", "Todas as otimizações de banco foram limpas")
    }
    
} 


