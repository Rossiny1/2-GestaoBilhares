package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.cache.AppCacheManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
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
// ✅ FASE 2: Utilitários de data centralizados
import com.example.gestaobilhares.core.utils.DateUtils
// ✅ FASE 12.3: Criptografia de dados sensíveis
import com.example.gestaobilhares.utils.DataEncryption
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
    private val panoMesaDao: com.example.gestaobilhares.data.dao.PanoMesaDao,
    private val mesaVendidaDao: com.example.gestaobilhares.data.dao.MesaVendidaDao,
    private val stockItemDao: com.example.gestaobilhares.data.dao.StockItemDao,
    private val equipmentDao: com.example.gestaobilhares.data.dao.EquipmentDao,
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
    
    // ✅ CACHE TEMPORÁRIO: Armazena URLs do Firebase Storage para AcertoMesa
    // Chave: acertoMesaId (Long), Valor: firebaseUrl (String)
    // Usado para garantir que a URL do Firebase seja preservada entre inserirAcertoMesaSync() e adicionarAcertoComMesasParaSync()
    private val fotoFirebaseUrlCache = mutableMapOf<Long, String>()
    
    // ✅ FASE 12.14 Etapa 2: Repositories internos especializados
    private val clienteRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.ClienteRepositoryInternal(
            clienteDao = clienteDao,
            syncQueueDao = syncQueueDao
        )
    }
    
    private val acertoRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.AcertoRepositoryInternal(
            acertoDao = acertoDao,
            acertoMesaDao = acertoMesaDao
        )
    }
    
    private val mesaRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.MesaRepositoryInternal(
            mesaDao = mesaDao
        )
    }
    
    private val rotaRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.RotaRepositoryInternal(
            rotaDao = rotaDao
        )
    }
    
    // ✅ FASE 12.14 Etapa 2: Repositories internos restantes
    private val despesaRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.DespesaRepositoryInternal(
            despesaDao = despesaDao
        )
    }
    
    private val cicloRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.CicloRepositoryInternal(
            cicloAcertoDao = cicloAcertoDao
        )
    }
    
    private val colaboradorRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.ColaboradorRepositoryInternal(
            colaboradorDao = colaboradorDao,
            syncQueueDao = syncQueueDao
        )
    }
    
    // ✅ FASE 12.14 Etapa 3: Repository interno para contratos
    private val contratoRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.ContratoRepositoryInternal(
            contratoLocacaoDao = contratoLocacaoDao,
            aditivoContratoDao = aditivoContratoDao,
            assinaturaRepresentanteLegalDao = assinaturaRepresentanteLegalDao,
            logAuditoriaAssinaturaDao = logAuditoriaAssinaturaDao,
            syncQueueDao = syncQueueDao
        )
    }
    
    // ✅ FASE 12.14 Etapa 4: Repository interno para veículos
    private val veiculoRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.VeiculoRepositoryInternal(
            veiculoDao = veiculoDao,
            historicoManutencaoVeiculoDao = historicoManutencaoVeiculoDao,
            historicoCombustivelVeiculoDao = historicoCombustivelVeiculoDao
        )
    }
    
    // ✅ FASE 12.14 Etapa 5: Repository interno para estoque
    private val estoqueRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.EstoqueRepositoryInternal(
            panoEstoqueDao = panoEstoqueDao,
            panoMesaDao = panoMesaDao,
            stockItemDao = stockItemDao,
            equipmentDao = equipmentDao,
            mesaVendidaDao = mesaVendidaDao,
            mesaReformadaDao = mesaReformadaDao,
            historicoManutencaoMesaDao = historicoManutencaoMesaDao
        )
    }
    
    // ✅ FASE 12.14 Etapa 6: Repository interno para metas
    private val metaRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.MetaRepositoryInternal(
            colaboradorDao = colaboradorDao
        )
    }
    
    // ✅ FASE 12.14 Etapa 7: Repository interno para categorias e tipos de despesa
    private val categoriaTipoDespesaRepositoryInternal by lazy {
        com.example.gestaobilhares.data.repository.internal.CategoriaTipoDespesaRepositoryInternal(
            categoriaDespesaDao = categoriaDespesaDao,
            tipoDespesaDao = tipoDespesaDao
        )
    }
    
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
    
    // ✅ FASE 12.14 Etapa 7: Delegado para CategoriaTipoDespesaRepositoryInternal
    fun buscarCategoriasAtivas() = categoriaTipoDespesaRepositoryInternal.buscarCategoriasAtivas()
    suspend fun buscarCategoriaPorNome(nome: String) = categoriaTipoDespesaRepositoryInternal.buscarCategoriaPorNome(nome)
    suspend fun categoriaExiste(nome: String): Boolean = categoriaTipoDespesaRepositoryInternal.categoriaExiste(nome)
    
    suspend fun criarCategoria(nova: NovaCategoriaDespesa): Long = 
        categoriaTipoDespesaRepositoryInternal.criarCategoria(
            nova = nova,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    fun buscarTiposPorCategoria(categoriaId: Long) = categoriaTipoDespesaRepositoryInternal.buscarTiposPorCategoria(categoriaId)
    suspend fun buscarTipoPorNome(nome: String) = categoriaTipoDespesaRepositoryInternal.buscarTipoPorNome(nome)
    suspend fun tipoExiste(nome: String, categoriaId: Long): Boolean = 
        categoriaTipoDespesaRepositoryInternal.tipoExiste(nome, categoriaId)
    
    suspend fun criarTipo(novo: NovoTipoDespesa): Long = 
        categoriaTipoDespesaRepositoryInternal.criarTipo(
            novo = novo,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )

    
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
     * ✅ FASE 12.3: Descriptografa dados sensíveis após ler
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    fun obterTodosClientes(): Flow<List<Cliente>> = clienteRepositoryInternal.obterTodosClientes()
    
    /**
     * ✅ MODERNIZADO: Obtém clientes por rota com cache
     * ✅ FASE 12.3: Descriptografa dados sensíveis após ler
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> = clienteRepositoryInternal.obterClientesPorRota(rotaId)
    
    /**
     * ✅ FASE 2A: Método otimizado com débito atual calculado
     * Usa query otimizada que calcula débito atual diretamente no banco
     * ✅ FASE 12.3: Descriptografa dados sensíveis após ler
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    fun obterClientesPorRotaComDebitoAtual(rotaId: Long): Flow<List<Cliente>> = 
        clienteRepositoryInternal.obterClientesPorRotaComDebitoAtual(rotaId)
    
    // ✅ FASE 12.3: Métodos helper para criptografia de dados sensíveis
    
    /**
     * Criptografa dados sensíveis de um Cliente antes de salvar
     */
    private fun encryptCliente(cliente: Cliente): Cliente {
        return cliente.copy(
            cpfCnpj = cliente.cpfCnpj?.let { DataEncryption.encrypt(it) ?: it }
        )
    }
    
    /**
     * Descriptografa dados sensíveis de um Cliente após ler
     */
    private fun decryptCliente(cliente: Cliente?): Cliente? {
        return cliente?.copy(
            cpfCnpj = cliente.cpfCnpj?.let { DataEncryption.decrypt(it) ?: it }
        )
    }
    
    /**
     * Criptografa dados sensíveis de um ContratoLocacao antes de salvar
     */
    private fun encryptContratoLocacao(contrato: ContratoLocacao): ContratoLocacao {
        return contrato.copy(
            locatarioCpf = DataEncryption.encrypt(contrato.locatarioCpf) ?: contrato.locatarioCpf,
            assinaturaLocatario = contrato.assinaturaLocatario?.let { DataEncryption.encrypt(it) ?: it },
            assinaturaLocador = contrato.assinaturaLocador?.let { DataEncryption.encrypt(it) ?: it },
            distratoAssinaturaLocador = contrato.distratoAssinaturaLocador?.let { DataEncryption.encrypt(it) ?: it },
            distratoAssinaturaLocatario = contrato.distratoAssinaturaLocatario?.let { DataEncryption.encrypt(it) ?: it },
            presencaFisicaConfirmadaCpf = contrato.presencaFisicaConfirmadaCpf?.let { DataEncryption.encrypt(it) ?: it }
        )
    }
    
    /**
     * Descriptografa dados sensíveis de um ContratoLocacao após ler
     */
    private fun decryptContratoLocacao(contrato: ContratoLocacao?): ContratoLocacao? {
        return contrato?.copy(
            locatarioCpf = DataEncryption.decrypt(contrato.locatarioCpf) ?: contrato.locatarioCpf,
            assinaturaLocatario = contrato.assinaturaLocatario?.let { DataEncryption.decrypt(it) ?: it },
            assinaturaLocador = contrato.assinaturaLocador?.let { DataEncryption.decrypt(it) ?: it },
            distratoAssinaturaLocador = contrato.distratoAssinaturaLocador?.let { DataEncryption.decrypt(it) ?: it },
            distratoAssinaturaLocatario = contrato.distratoAssinaturaLocatario?.let { DataEncryption.decrypt(it) ?: it },
            presencaFisicaConfirmadaCpf = contrato.presencaFisicaConfirmadaCpf?.let { DataEncryption.decrypt(it) ?: it }
        )
    }
    
    /**
     * Criptografa dados sensíveis de um Colaborador antes de salvar
     */
    private fun encryptColaborador(colaborador: Colaborador): Colaborador {
        return colaborador.copy(
            cpf = colaborador.cpf?.let { DataEncryption.encrypt(it) ?: it }
            // senhaTemporaria já está como hash (Fase 12.1), não precisa criptografar novamente
        )
    }
    
    /**
     * Descriptografa dados sensíveis de um Colaborador após ler
     */
    private fun decryptColaborador(colaborador: Colaborador?): Colaborador? {
        return colaborador?.copy(
            cpf = colaborador.cpf?.let { DataEncryption.decrypt(it) ?: it }
        )
    }
    
    /**
     * Criptografa dados sensíveis de uma MesaVendida antes de salvar
     */
    private fun encryptMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida): com.example.gestaobilhares.data.entities.MesaVendida {
        return mesaVendida.copy(
            cpfCnpjComprador = mesaVendida.cpfCnpjComprador?.let { DataEncryption.encrypt(it) ?: it }
        )
    }
    
    /**
     * Descriptografa dados sensíveis de uma MesaVendida após ler
     */
    private fun decryptMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida?): com.example.gestaobilhares.data.entities.MesaVendida? {
        return mesaVendida?.copy(
            cpfCnpjComprador = mesaVendida.cpfCnpjComprador?.let { DataEncryption.decrypt(it) ?: it }
        )
    }
    
    /**
     * Criptografa dados sensíveis de uma AssinaturaRepresentanteLegal antes de salvar
     */
    private fun encryptAssinaturaRepresentanteLegal(assinatura: com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal): com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal {
        return assinatura.copy(
            cpfRepresentante = DataEncryption.encrypt(assinatura.cpfRepresentante) ?: assinatura.cpfRepresentante,
            assinaturaBase64 = DataEncryption.encrypt(assinatura.assinaturaBase64) ?: assinatura.assinaturaBase64
        )
    }
    
    /**
     * Descriptografa dados sensíveis de uma AssinaturaRepresentanteLegal após ler
     */
    private fun decryptAssinaturaRepresentanteLegal(assinatura: com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal?): com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal? {
        return assinatura?.copy(
            cpfRepresentante = DataEncryption.decrypt(assinatura.cpfRepresentante) ?: assinatura.cpfRepresentante,
            assinaturaBase64 = DataEncryption.decrypt(assinatura.assinaturaBase64) ?: assinatura.assinaturaBase64
        )
    }
    
    /**
     * Criptografa dados sensíveis de um LogAuditoriaAssinatura antes de salvar
     */
    private fun encryptLogAuditoriaAssinatura(log: com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura): com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura {
        return log.copy(
            cpfUsuario = DataEncryption.encrypt(log.cpfUsuario) ?: log.cpfUsuario
        )
    }
    
    /**
     * Descriptografa dados sensíveis de um LogAuditoriaAssinatura após ler
     */
    private fun decryptLogAuditoriaAssinatura(log: com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura?): com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura? {
        return log?.copy(
            cpfUsuario = DataEncryption.decrypt(log.cpfUsuario) ?: log.cpfUsuario
        )
    }
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    suspend fun obterClientePorId(id: Long) = clienteRepositoryInternal.obterClientePorId(id)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    suspend fun inserirCliente(cliente: Cliente): Long = 
        clienteRepositoryInternal.inserirCliente(
            cliente = cliente,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    suspend fun atualizarCliente(cliente: Cliente) = 
        clienteRepositoryInternal.atualizarCliente(
            cliente = cliente,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    suspend fun deletarCliente(cliente: Cliente) = clienteRepositoryInternal.deletarCliente(cliente)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    suspend fun atualizarDebitoAtual(clienteId: Long, novoDebito: Double) = 
        clienteRepositoryInternal.atualizarDebitoAtual(
            clienteId = clienteId,
            novoDebito = novoDebito,
            obterClientePorId = ::obterClientePorId,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long) = 
        clienteRepositoryInternal.calcularDebitoAtualEmTempoReal(clienteId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    suspend fun obterClienteComDebitoAtual(clienteId: Long) = 
        clienteRepositoryInternal.obterClienteComDebitoAtual(clienteId)
    
    /**
     * ✅ NOVO: Busca o ID da rota associada a um cliente
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    suspend fun buscarRotaIdPorCliente(clienteId: Long): Long? = 
        clienteRepositoryInternal.buscarRotaIdPorCliente(clienteId)
    
    // ==================== ACERTO ====================
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    fun obterAcertosPorCliente(clienteId: Long) = acertoRepositoryInternal.obterAcertosPorCliente(clienteId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun obterAcertoPorId(id: Long) = acertoRepositoryInternal.obterAcertoPorId(id)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long) = 
        acertoRepositoryInternal.buscarUltimoAcertoPorCliente(clienteId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    fun obterTodosAcertos() = acertoRepositoryInternal.obterTodosAcertos()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    fun buscarAcertosPorCicloId(cicloId: Long) = acertoRepositoryInternal.buscarAcertosPorCicloId(cicloId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ClienteRepositoryInternal
     */
    fun buscarClientesPorRota(rotaId: Long) = obterClientesPorRota(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun buscarRotaPorId(rotaId: Long) = obterRotaPorId(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun inserirAcerto(acerto: Acerto): Long = 
        acertoRepositoryInternal.inserirAcerto(
            acerto = acerto,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun atualizarAcerto(acerto: Acerto) = 
        acertoRepositoryInternal.atualizarAcerto(
            acerto = acerto,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun deletarAcerto(acerto: Acerto) = acertoRepositoryInternal.deletarAcerto(acerto)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun buscarUltimoAcertoPorMesa(mesaId: Long) = 
        acertoRepositoryInternal.buscarUltimoAcertoPorMesa(mesaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun buscarUltimoAcertoMesaItem(mesaId: Long) =
        acertoRepositoryInternal.buscarUltimoAcertoMesaItem(mesaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long) = 
        acertoRepositoryInternal.buscarObservacaoUltimoAcerto(clienteId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun buscarUltimosAcertosPorClientes(clienteIds: List<Long>) =
        acertoRepositoryInternal.buscarUltimosAcertosPorClientes(clienteIds)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    suspend fun buscarCicloAtivo(rotaId: Long) = cicloRepositoryInternal.buscarCicloAtivo(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = acertoRepositoryInternal.buscarPorRotaECicloId(rotaId, cicloId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun buscarAcertoMesaPorMesa(mesaId: Long) = acertoRepositoryInternal.buscarAcertoMesaPorMesa(mesaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para AcertoRepositoryInternal
     */
    suspend fun buscarPorId(id: Long) = acertoRepositoryInternal.buscarPorId(id)
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    suspend fun atualizarValoresCiclo(
        cicloId: Long,
        valorTotalAcertado: Double,
        valorTotalDespesas: Double,
        clientesAcertados: Int
    ) = cicloRepositoryInternal.atualizarValoresCiclo(cicloId, valorTotalAcertado, valorTotalDespesas, clientesAcertados)
    // ✅ FASE 12.14 Etapa 5: Delegado para EstoqueRepositoryInternal
    suspend fun obterPanoPorId(id: Long) = estoqueRepositoryInternal.obterPanoEstoquePorId(id)
    suspend fun marcarPanoComoUsadoPorNumero(numeroPano: String, motivo: String) = 
        estoqueRepositoryInternal.marcarPanoComoUsadoPorNumero(numeroPano, motivo)
    suspend fun buscarPorNumero(numeroPano: String) = estoqueRepositoryInternal.buscarPorNumero(numeroPano)
    suspend fun marcarPanoComoUsado(panoId: Long, motivo: String) = 
        estoqueRepositoryInternal.marcarPanoComoUsado(panoId, motivo)
    
    // ==================== MESA ====================
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun obterMesaPorId(id: Long) = mesaRepositoryInternal.obterMesaPorId(id)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    fun obterMesasPorCliente(clienteId: Long) = mesaRepositoryInternal.obterMesasPorCliente(clienteId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    fun obterMesasDisponiveis() = mesaRepositoryInternal.obterMesasDisponiveis()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun inserirMesa(mesa: Mesa): Long = 
        mesaRepositoryInternal.inserirMesa(
            mesa = mesa,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun atualizarMesa(mesa: Mesa) = 
        mesaRepositoryInternal.atualizarMesa(
            mesa = mesa,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun deletarMesa(mesa: Mesa) = mesaRepositoryInternal.deletarMesa(mesa)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun vincularMesaACliente(mesaId: Long, clienteId: Long) = 
        mesaRepositoryInternal.vincularMesaACliente(mesaId, clienteId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) = 
        mesaRepositoryInternal.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun desvincularMesaDeCliente(mesaId: Long) = mesaRepositoryInternal.desvincularMesaDeCliente(mesaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun retirarMesa(mesaId: Long) = mesaRepositoryInternal.retirarMesa(mesaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun atualizarRelogioMesa(mesaId: Long, relogioInicial: Int, relogioFinal: Int) = 
        mesaRepositoryInternal.atualizarRelogioMesa(
            mesaId = mesaId,
            relogioInicial = relogioInicial,
            relogioFinal = relogioFinal,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = 
        mesaRepositoryInternal.atualizarRelogioFinal(mesaId, relogioFinal)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun obterMesasPorClienteDireto(clienteId: Long) = 
        mesaRepositoryInternal.obterMesasPorClienteDireto(clienteId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    fun buscarMesasPorRota(rotaId: Long) = mesaRepositoryInternal.buscarMesasPorRota(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    suspend fun contarMesasAtivasPorClientes(clienteIds: List<Long>) =
        mesaRepositoryInternal.contarMesasAtivasPorClientes(clienteIds)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para MesaRepositoryInternal
     */
    fun obterTodasMesas() = mesaRepositoryInternal.obterTodasMesas()
    
    // ==================== ROTA ====================
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    fun obterTodasRotas() = rotaRepositoryInternal.obterTodasRotas()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    fun obterRotasAtivas() = rotaRepositoryInternal.obterRotasAtivas()
    
    // ✅ NOVO: Método para obter resumo de rotas com atualização em tempo real
    // ✅ CORREÇÃO OFICIAL: Usar @OptIn para flatMapLatest e garantir que o Flow seja re-emitido imediatamente
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getRotasResumoComAtualizacaoTempoReal(): Flow<List<RotaResumo>> {
        // ✅ FASE 12.5: Usar flatMapLatest com flow { } para executar cálculos suspensos sem runBlocking
        // ✅ CORREÇÃO: Usar conflate() para garantir que mudanças sejam processadas imediatamente
        return rotaDao.getAllRotasAtivas()
            .conflate() // ✅ CRÍTICO: Processar mudanças imediatamente, sem buffer
            .flatMapLatest { rotas ->
                flow {
                    val rotasResumo = rotas.map { rota ->
                        // Usar dados reais calculados (agora são suspend)
                        val clientesAtivos = calcularClientesAtivosSync(rota.id)
                        val pendencias = calcularPendenciasSync(rota.id)
                        val valorAcertado = calcularValorAcertadoSync(rota.id)
                        val quantidadeMesas = calcularQuantidadeMesasSync(rota.id)
                        val percentualAcertados = calcularPercentualAcertadosSync(rota.id, clientesAtivos)
                        
                        // ✅ CORREÇÃO: Usar status da entidade Rota (já atualizada pelo PULL)
                        val status = rota.statusAtual
                        
                        // ✅ CORREÇÃO: Usar dados da entidade Rota (já atualizada pelo PULL)
                        val cicloAtual = rota.cicloAcertoAtual
                        
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
                    emit(rotasResumo)
                }
            }
    }
    
    // ✅ FASE 12.5: Métodos auxiliares para calcular dados reais das rotas (versões suspend - removido runBlocking)
    // ✅ FASE 12.14 Etapa 2: Usa repository interno
    private suspend fun calcularClientesAtivosSync(rotaId: Long): Int {
        return try {
            obterClientesPorRota(rotaId).first().count { it.ativo }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular clientes ativos da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularPendenciasSync(rotaId: Long): Int {
        return try {
            calcularPendenciasReaisPorRota(rotaId)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular pendências da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularValorAcertadoSync(rotaId: Long): Double {
        return try {
            calcularValorAcertadoPorRotaECiclo(rotaId, obterCicloAtualIdPorRota(rotaId))
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular valor acertado da rota $rotaId: ${e.message}")
            0.0
        }
    }
    
    private suspend fun calcularQuantidadeMesasSync(rotaId: Long): Int {
        return try {
            calcularQuantidadeMesasPorRota(rotaId)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular quantidade de mesas da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularPercentualAcertadosSync(rotaId: Long, clientesAtivos: Int): Int {
        return try {
            calcularPercentualClientesAcertados(rotaId, obterCicloAtualIdPorRota(rotaId), clientesAtivos)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular percentual de clientes acertados da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularCicloAtualReal(rotaId: Long): Int {
        return try {
            obterCicloAtualRota(rotaId).first
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular ciclo atual da rota $rotaId: ${e.message}")
            1
        }
    }
    
    private suspend fun obterDataCicloAtual(rotaId: Long): Long? {
        return try {
            obterCicloAtualRota(rotaId).third
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter data do ciclo atual da rota $rotaId: ${e.message}")
            null
        }
    }
    
    // ✅ FASE 12.14 Etapa 2: Usa repository interno
    private suspend fun calcularClientesAtivosPorRota(rotaId: Long): Int {
        return try {
            obterClientesPorRota(rotaId).first().count { it.ativo }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular clientes ativos da rota $rotaId: ${e.message}")
            0
        }
    }
    
    // ✅ FASE 12.14 Etapa 2: Usa repository interno
    private suspend fun calcularPendenciasReaisPorRota(rotaId: Long): Int {
        return try {
                val clientes = obterClientesPorRota(rotaId).first()
            if (clientes.isEmpty()) return 0
                val clienteIds = clientes.map { it.id }
                val ultimos = buscarUltimosAcertosPorClientes(clienteIds)
                val ultimoPorCliente = ultimos.associateBy({ it.clienteId }, { it.dataAcerto })
                val agora = java.util.Calendar.getInstance()
                clientes.count { cliente ->
                    // Critério alinhado com a UI: considerar pendência quando débito > R$300
                    val debitoAlto = cliente.debitoAtual > 300
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
                buscarMesasPorRota(rotaId).first().size
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
                    Triple(cicloAtual.numeroCiclo, cicloAtual.id, cicloAtual.dataFim.time)
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
     * ✅ FASE 12.14 Etapa 2: Usa repository interno
     */
    suspend fun obterCicloAtualIdPorRota(rotaId: Long): Long? {
        return try {
            val cicloAtual = cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
            if (cicloAtual != null) {
                cicloAtual.id
                } else {
                val rota = obterRotaPorId(rotaId)
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
                Pair(ciclo.dataInicio.time, ciclo.dataFim.time)
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
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun obterRotaPorId(id: Long) = rotaRepositoryInternal.obterRotaPorId(id)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    fun obterRotaPorIdFlow(id: Long) = rotaRepositoryInternal.obterRotaPorIdFlow(id)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun obterRotaPorNome(nome: String) = rotaRepositoryInternal.obterRotaPorNome(nome)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun inserirRota(rota: Rota): Long = 
        rotaRepositoryInternal.inserirRota(
            rota = rota,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun inserirRotas(rotas: List<Rota>): List<Long> = 
        rotaRepositoryInternal.inserirRotas(
            rotas = rotas,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun atualizarRota(rota: Rota) = 
        rotaRepositoryInternal.atualizarRota(
            rota = rota,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun atualizarRotas(rotas: List<Rota>) = rotaRepositoryInternal.atualizarRotas(rotas)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun deletarRota(rota: Rota) = rotaRepositoryInternal.deletarRota(rota)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun desativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaRepositoryInternal.desativarRota(rotaId, timestamp)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun ativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaRepositoryInternal.ativarRota(rotaId, timestamp)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun atualizarStatus(rotaId: Long, status: String, timestamp: Long = System.currentTimeMillis()) = 
        rotaRepositoryInternal.atualizarStatus(rotaId, status, timestamp)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun atualizarCicloAcerto(rotaId: Long, ciclo: Int, timestamp: Long = System.currentTimeMillis()) = 
        rotaRepositoryInternal.atualizarCicloAcerto(rotaId, ciclo, timestamp)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun iniciarCicloRota(rotaId: Long, ciclo: Int, dataInicio: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaRepositoryInternal.iniciarCicloRota(rotaId, ciclo, dataInicio, timestamp)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun finalizarCicloRota(rotaId: Long, dataFim: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaRepositoryInternal.finalizarCicloRota(rotaId, dataFim, timestamp)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun existeRotaComNome(nome: String, excludeId: Long = 0): Int = 
        rotaRepositoryInternal.existeRotaComNome(nome, excludeId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para RotaRepositoryInternal
     */
    suspend fun contarRotasAtivas() = rotaRepositoryInternal.contarRotasAtivas()
    
    // ==================== DESPESA ====================
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    fun obterTodasDespesas() = despesaRepositoryInternal.obterTodasDespesas()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    fun obterDespesasPorRota(rotaId: Long) = despesaRepositoryInternal.obterDespesasPorRota(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun obterDespesaPorId(id: Long) = despesaRepositoryInternal.obterDespesaPorId(id)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun inserirDespesa(despesa: Despesa): Long {
        val uploadFoto = this::uploadFotoSeNecessario
        return despesaRepositoryInternal.inserirDespesa(
            despesa = despesa,
            obterEmpresaId = { obterEmpresaId() },
            uploadFotoSeNecessario = { caminhoLocal, tipo, empresaId, entityId, entityExtraId ->
                uploadFoto(caminhoLocal, tipo, empresaId, entityId, entityExtraId)
            },
            logDbInsertStart = { entity, info -> logDbInsertStart(entity, info) },
            logDbInsertSuccess = { entity, info -> logDbInsertSuccess(entity, info) },
            logDbInsertError = { entity, info, error -> logDbInsertError(entity, info, error) },
            adicionarOperacaoSync = { entityType, entityId, operation, payload, priority ->
                adicionarOperacaoSync(entityType, entityId, operation, payload, priority)
            },
            logarOperacaoSync = { entityType, entityId, operation, status, error, payload ->
                logarOperacaoSync(entityType, entityId, operation, status, error, payload)
            }
        )
    }
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun atualizarDespesa(despesa: Despesa) = despesaRepositoryInternal.atualizarDespesa(
        despesa = despesa,
        obterEmpresaId = { obterEmpresaId() },
        uploadFotoSeNecessario = { caminhoLocal, tipo, empresaId, entityId, entityExtraId ->
            this@AppRepository.uploadFotoSeNecessario(caminhoLocal, tipo, empresaId, entityId, entityExtraId)
        },
        logDbUpdateStart = { entity, info -> logDbUpdateStart(entity, info) },
        logDbUpdateSuccess = { entity, info -> logDbUpdateSuccess(entity, info) },
        logDbUpdateError = { entity, info, error -> logDbUpdateError(entity, info, error) },
        adicionarOperacaoSync = { entityType, entityId, operation, payload, priority ->
            adicionarOperacaoSync(entityType, entityId, operation, payload, priority)
        },
        logarOperacaoSync = { entityType, entityId, operation, status, error, payload ->
            logarOperacaoSync(entityType, entityId, operation, status, error, payload)
        }
    )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun deletarDespesa(despesa: Despesa) = despesaRepositoryInternal.deletarDespesa(despesa)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun calcularTotalPorRota(rotaId: Long) = despesaRepositoryInternal.calcularTotalPorRota(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun calcularTotalGeral() = despesaRepositoryInternal.calcularTotalGeral()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun contarDespesasPorRota(rotaId: Long) = despesaRepositoryInternal.contarDespesasPorRota(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun deletarDespesasPorRota(rotaId: Long) = despesaRepositoryInternal.deletarDespesasPorRota(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    fun buscarDespesasPorCicloId(cicloId: Long) = despesaRepositoryInternal.buscarDespesasPorCicloId(cicloId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    fun buscarDespesasPorRotaECicloId(rotaId: Long, cicloId: Long) = 
        despesaRepositoryInternal.buscarDespesasPorRotaECicloId(rotaId, cicloId)

    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun buscarDespesasGlobaisPorCiclo(ano: Int, numero: Int): List<Despesa> = 
        despesaRepositoryInternal.buscarDespesasGlobaisPorCiclo(ano, numero)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para DespesaRepositoryInternal
     */
    suspend fun somarDespesasGlobaisPorCiclo(ano: Int, numero: Int): Double = 
        despesaRepositoryInternal.somarDespesasGlobaisPorCiclo(ano, numero)

    // ==================== PANO ESTOQUE ====================
    
    // ✅ FASE 12.14 Etapa 5: Delegado para EstoqueRepositoryInternal
    fun obterTodosPanosEstoque() = estoqueRepositoryInternal.obterTodosPanosEstoque()
    fun obterPanosDisponiveis() = estoqueRepositoryInternal.obterPanosDisponiveis()
    suspend fun obterPanoEstoquePorId(id: Long) = estoqueRepositoryInternal.obterPanoEstoquePorId(id)
    
    suspend fun inserirPanoEstoque(pano: com.example.gestaobilhares.data.entities.PanoEstoque): Long = 
        estoqueRepositoryInternal.inserirPanoEstoque(
            pano = pano,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarPanoEstoque(pano: com.example.gestaobilhares.data.entities.PanoEstoque) = 
        estoqueRepositoryInternal.atualizarPanoEstoque(
            pano = pano,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarPanoEstoque(pano: com.example.gestaobilhares.data.entities.PanoEstoque) = 
        estoqueRepositoryInternal.deletarPanoEstoque(pano)
    
    suspend fun atualizarDisponibilidadePano(id: Long, disponivel: Boolean) = 
        estoqueRepositoryInternal.atualizarDisponibilidadePano(id, disponivel)

    // ==================== PANO MESA ====================
    
    // ✅ FASE 12.14 Etapa 5: Delegado para EstoqueRepositoryInternal
    fun obterPanoMesaPorMesa(mesaId: Long) = estoqueRepositoryInternal.obterPanoMesaPorMesa(mesaId)
    suspend fun obterPanoAtualMesa(mesaId: Long) = estoqueRepositoryInternal.obterPanoAtualMesa(mesaId)
    fun obterPanoMesaPorPano(panoId: Long) = estoqueRepositoryInternal.obterPanoMesaPorPano(panoId)
    suspend fun obterUltimaTrocaMesa(mesaId: Long) = estoqueRepositoryInternal.obterUltimaTrocaMesa(mesaId)
    
    suspend fun inserirPanoMesa(panoMesa: com.example.gestaobilhares.data.entities.PanoMesa): Long = 
        estoqueRepositoryInternal.inserirPanoMesa(
            panoMesa = panoMesa,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarPanoMesa(panoMesa: com.example.gestaobilhares.data.entities.PanoMesa) = 
        estoqueRepositoryInternal.atualizarPanoMesa(
            panoMesa = panoMesa,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarPanoMesa(panoMesa: com.example.gestaobilhares.data.entities.PanoMesa) = 
        estoqueRepositoryInternal.deletarPanoMesa(
            panoMesa = panoMesa,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun desativarPanoAtualMesa(mesaId: Long) = estoqueRepositoryInternal.desativarPanoAtualMesa(mesaId)
    suspend fun ativarPanoMesa(mesaId: Long, panoId: Long) = estoqueRepositoryInternal.ativarPanoMesa(mesaId, panoId)
    fun buscarHistoricoTrocasMesa(mesaId: Long) = estoqueRepositoryInternal.buscarHistoricoTrocasMesa(mesaId)

    // ==================== MESA VENDIDA ====================
    
    // ✅ FASE 12.14 Etapa 5: Delegado para EstoqueRepositoryInternal
    fun obterTodasMesasVendidas() = estoqueRepositoryInternal.obterTodasMesasVendidas(::decryptMesaVendida)
    suspend fun obterMesaVendidaPorId(id: Long) = estoqueRepositoryInternal.obterMesaVendidaPorId(id, ::decryptMesaVendida)
    
    suspend fun inserirMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida): Long = 
        estoqueRepositoryInternal.inserirMesaVendida(
            mesaVendida = mesaVendida,
            encryptMesaVendida = ::encryptMesaVendida,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida) = 
        estoqueRepositoryInternal.atualizarMesaVendida(
            mesaVendida = mesaVendida,
            encryptMesaVendida = ::encryptMesaVendida,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida) = 
        estoqueRepositoryInternal.deletarMesaVendida(mesaVendida)

    // ==================== MESA REFORMADA ====================
    
    // ✅ FASE 12.14 Etapa 5: Delegado para EstoqueRepositoryInternal
    suspend fun inserirMesaReformada(mesaReformada: MesaReformada): Long = 
        estoqueRepositoryInternal.inserirMesaReformada(
            mesaReformada = mesaReformada,
            obterEmpresaId = ::obterEmpresaId,
            uploadFotoSeNecessario = ::uploadFotoSeNecessario,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )

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

    // ==================== EQUIPMENT ====================
    
    // ✅ FASE 12.14 Etapa 5: Delegado para EstoqueRepositoryInternal
    fun obterTodosEquipments() = estoqueRepositoryInternal.obterTodosEquipments()
    suspend fun obterEquipmentPorId(id: Long) = estoqueRepositoryInternal.obterEquipmentPorId(id)
    
    suspend fun inserirEquipment(equipment: com.example.gestaobilhares.data.entities.Equipment): Long = 
        estoqueRepositoryInternal.inserirEquipment(
            equipment = equipment,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarEquipment(equipment: com.example.gestaobilhares.data.entities.Equipment) = 
        estoqueRepositoryInternal.atualizarEquipment(
            equipment = equipment,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarEquipment(equipment: com.example.gestaobilhares.data.entities.Equipment) = 
        estoqueRepositoryInternal.deletarEquipment(equipment)

    // ==================== VEICULO ====================
    
    // ✅ FASE 12.14 Etapa 4: Delegado para VeiculoRepositoryInternal
    fun obterTodosVeiculos() = veiculoRepositoryInternal.obterTodosVeiculos()
    suspend fun obterVeiculoPorId(id: Long) = veiculoRepositoryInternal.obterVeiculoPorId(id)
    
    suspend fun inserirVeiculo(veiculo: com.example.gestaobilhares.data.entities.Veiculo): Long = 
        veiculoRepositoryInternal.inserirVeiculo(
            veiculo = veiculo,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarVeiculo(veiculo: com.example.gestaobilhares.data.entities.Veiculo) = 
        veiculoRepositoryInternal.atualizarVeiculo(
            veiculo = veiculo,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarVeiculo(veiculo: com.example.gestaobilhares.data.entities.Veiculo) = 
        veiculoRepositoryInternal.deletarVeiculo(veiculo)

    // ==================== HISTORICO MANUTENCAO MESA ====================
    
    // ✅ FASE 12.14 Etapa 5: Delegado para EstoqueRepositoryInternal
    fun obterTodosHistoricoManutencaoMesa() = estoqueRepositoryInternal.obterTodosHistoricoManutencaoMesa()
    suspend fun obterHistoricoManutencaoMesaPorId(id: Long) = estoqueRepositoryInternal.obterHistoricoManutencaoMesaPorId(id)
    
    suspend fun inserirHistoricoManutencaoMesaSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa): Long = 
        estoqueRepositoryInternal.inserirHistoricoManutencaoMesa(
            historico = historico,
            obterEmpresaId = ::obterEmpresaId,
            uploadFotoSeNecessario = ::uploadFotoSeNecessario,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarHistoricoManutencaoMesaSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa) = 
        estoqueRepositoryInternal.atualizarHistoricoManutencaoMesa(
            historico = historico,
            obterEmpresaId = ::obterEmpresaId,
            uploadFotoSeNecessario = ::uploadFotoSeNecessario,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarHistoricoManutencaoMesa(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa) = 
        estoqueRepositoryInternal.deletarHistoricoManutencaoMesa(historico)

    // ==================== HISTORICO MANUTENCAO VEICULO ====================
    
    // ✅ FASE 12.14 Etapa 4: Delegado para VeiculoRepositoryInternal
    fun obterTodosHistoricoManutencaoVeiculo() = veiculoRepositoryInternal.obterTodosHistoricoManutencaoVeiculo()
    suspend fun obterHistoricoManutencaoVeiculoPorId(id: Long) = veiculoRepositoryInternal.obterHistoricoManutencaoVeiculoPorId(id)
    
    suspend fun inserirHistoricoManutencaoVeiculoSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo): Long = 
        veiculoRepositoryInternal.inserirHistoricoManutencaoVeiculo(
            historico = historico,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarHistoricoManutencaoVeiculoSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo) = 
        veiculoRepositoryInternal.atualizarHistoricoManutencaoVeiculo(
            historico = historico,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarHistoricoManutencaoVeiculo(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo) = 
        veiculoRepositoryInternal.deletarHistoricoManutencaoVeiculo(historico)

    // ==================== HISTORICO COMBUSTIVEL VEICULO ====================
    
    // ✅ FASE 12.14 Etapa 4: Delegado para VeiculoRepositoryInternal
    fun obterTodosHistoricoCombustivelVeiculo() = veiculoRepositoryInternal.obterTodosHistoricoCombustivelVeiculo()
    suspend fun obterHistoricoCombustivelVeiculoPorId(id: Long) = veiculoRepositoryInternal.obterHistoricoCombustivelVeiculoPorId(id)
    
    suspend fun inserirHistoricoCombustivelVeiculoSync(historico: com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo): Long = 
        veiculoRepositoryInternal.inserirHistoricoCombustivelVeiculo(
            historico = historico,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarHistoricoCombustivelVeiculoSync(historico: com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo) = 
        veiculoRepositoryInternal.atualizarHistoricoCombustivelVeiculo(
            historico = historico,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarHistoricoCombustivelVeiculo(historico: com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo) = 
        veiculoRepositoryInternal.deletarHistoricoCombustivelVeiculo(historico)

    // ==================== CATEGORIA DESPESA ====================
    
    // ✅ FASE 12.14 Etapa 7: Delegado para CategoriaTipoDespesaRepositoryInternal
    fun obterTodasCategoriasDespesa() = categoriaTipoDespesaRepositoryInternal.obterTodasCategoriasDespesa()
    suspend fun obterCategoriaDespesaPorId(id: Long) = categoriaTipoDespesaRepositoryInternal.obterCategoriaDespesaPorId(id)
    
    suspend fun inserirCategoriaDespesaSync(categoria: com.example.gestaobilhares.data.entities.CategoriaDespesa): Long = 
        categoriaTipoDespesaRepositoryInternal.inserirCategoriaDespesaSync(
            categoria = categoria,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarCategoriaDespesaSync(categoria: com.example.gestaobilhares.data.entities.CategoriaDespesa) = 
        categoriaTipoDespesaRepositoryInternal.atualizarCategoriaDespesaSync(
            categoria = categoria,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarCategoriaDespesa(categoria: com.example.gestaobilhares.data.entities.CategoriaDespesa) = 
        categoriaTipoDespesaRepositoryInternal.deletarCategoriaDespesa(categoria)

    // ==================== TIPO DESPESA ====================
    
    // ✅ FASE 12.14 Etapa 7: Delegado para CategoriaTipoDespesaRepositoryInternal
    fun obterTodosTiposDespesa() = categoriaTipoDespesaRepositoryInternal.obterTodosTiposDespesa()
    suspend fun obterTipoDespesaPorId(id: Long) = categoriaTipoDespesaRepositoryInternal.obterTipoDespesaPorId(id)
    
    suspend fun inserirTipoDespesaSync(tipo: com.example.gestaobilhares.data.entities.TipoDespesa): Long = 
        categoriaTipoDespesaRepositoryInternal.inserirTipoDespesaSync(
            tipo = tipo,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarTipoDespesaSync(tipo: com.example.gestaobilhares.data.entities.TipoDespesa) = 
        categoriaTipoDespesaRepositoryInternal.atualizarTipoDespesaSync(
            tipo = tipo,
            logDbUpdateStart = ::logDbUpdateStart,
            logDbUpdateSuccess = ::logDbUpdateSuccess,
            logDbUpdateError = ::logDbUpdateError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarTipoDespesa(tipo: com.example.gestaobilhares.data.entities.TipoDespesa) = 
        categoriaTipoDespesaRepositoryInternal.deletarTipoDespesa(tipo)

    // ==================== CONTRATO LOCAÇÃO ====================
    
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal (métodos Sync)
    suspend fun inserirContratoLocacaoSync(contrato: com.example.gestaobilhares.data.entities.ContratoLocacao): Long {
        val obterAssinaturaAtiva: suspend () -> AssinaturaRepresentanteLegal? = {
            try { 
                decryptAssinaturaRepresentanteLegal(assinaturaRepresentanteLegalDao.obterAssinaturaAtiva())
            } catch (_: Exception) { null }
        }
        return contratoRepositoryInternal.inserirContrato(
            contrato = contrato,
            encryptContrato = ::encryptContratoLocacao,
            obterAssinaturaAtiva = obterAssinaturaAtiva,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    }
    
    suspend fun atualizarContratoLocacaoSync(contrato: com.example.gestaobilhares.data.entities.ContratoLocacao) {
        val obterAssinaturaAtiva: suspend () -> AssinaturaRepresentanteLegal? = {
            try { 
                decryptAssinaturaRepresentanteLegal(assinaturaRepresentanteLegalDao.obterAssinaturaAtiva())
            } catch (_: Exception) { null }
        }
        contratoRepositoryInternal.atualizarContrato(
            contrato = contrato,
            encryptContrato = ::encryptContratoLocacao,
            obterAssinaturaAtiva = obterAssinaturaAtiva,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    }

    // ==================== ASSINATURA REPRESENTANTE LEGAL ====================
    
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal (métodos Sync)
    suspend fun inserirAssinaturaRepresentanteLegalSync(assinatura: com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal): Long = 
        contratoRepositoryInternal.inserirAssinaturaRepresentanteLegal(
            assinatura = assinatura,
            encryptAssinatura = ::encryptAssinaturaRepresentanteLegal,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarAssinaturaRepresentanteLegalSync(assinatura: com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal) {
        contratoRepositoryInternal.atualizarAssinaturaRepresentanteLegal(
            assinatura = assinatura,
            encryptAssinatura = ::encryptAssinaturaRepresentanteLegal
        )
        // Sincronização será feita pelo método atualizarContrato quando necessário
    }

    // ==================== LOG AUDITORIA ASSINATURA ====================
    
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal (métodos Sync)
    suspend fun inserirLogAuditoriaAssinaturaSync(log: com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura): Long = 
        contratoRepositoryInternal.inserirLogAuditoriaAssinatura(
            log = log,
            encryptLog = ::encryptLogAuditoriaAssinatura,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )

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
                        "dataInicio": ${cicloFinalizado.dataInicio.time},
                        "dataFim": ${cicloFinalizado.dataFim.time},
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
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    fun obterTodosColaboradores() = colaboradorRepositoryInternal.obterTodosColaboradores()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    fun obterColaboradoresAtivos() = colaboradorRepositoryInternal.obterColaboradoresAtivos()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    fun obterColaboradoresAprovados() = colaboradorRepositoryInternal.obterColaboradoresAprovados()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    fun obterColaboradoresPendentesAprovacao() = colaboradorRepositoryInternal.obterColaboradoresPendentesAprovacao()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    fun obterColaboradoresPorNivelAcesso(nivelAcesso: NivelAcesso) = 
        colaboradorRepositoryInternal.obterColaboradoresPorNivelAcesso(nivelAcesso)

    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun obterColaboradorPorId(id: Long) = colaboradorRepositoryInternal.obterColaboradorPorId(id)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun obterColaboradorPorEmail(email: String) = colaboradorRepositoryInternal.obterColaboradorPorEmail(email)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun obterColaboradorPorFirebaseUid(firebaseUid: String) = 
        colaboradorRepositoryInternal.obterColaboradorPorFirebaseUid(firebaseUid)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun obterColaboradorPorGoogleId(googleId: String) = 
        colaboradorRepositoryInternal.obterColaboradorPorGoogleId(googleId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun inserirColaborador(colaborador: Colaborador): Long = colaboradorRepositoryInternal.inserirColaborador(
        colaborador = colaborador,
        logDbInsertStart = { entity, info -> logDbInsertStart(entity, info) },
        logDbInsertSuccess = { entity, info -> logDbInsertSuccess(entity, info) },
        logDbInsertError = { entity, info, error -> logDbInsertError(entity, info, error) },
        adicionarOperacaoSync = { entityType, entityId, operation, payload, priority ->
            adicionarOperacaoSync(entityType, entityId, operation, payload, priority)
        },
        logarOperacaoSync = { entityType, entityId, operation, status, error, payload ->
            logarOperacaoSync(entityType, entityId, operation, status, error, payload)
        }
    )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun atualizarColaborador(colaborador: Colaborador) = colaboradorRepositoryInternal.atualizarColaborador(
        colaborador = colaborador,
        logDbUpdateStart = { entity, info -> logDbUpdateStart(entity, info) },
        logDbUpdateSuccess = { entity, info -> logDbUpdateSuccess(entity, info) },
        logDbUpdateError = { entity, info, error -> logDbUpdateError(entity, info, error) },
        adicionarOperacaoSync = { entityType, entityId, operation, payload, priority ->
            adicionarOperacaoSync(entityType, entityId, operation, payload, priority)
        },
        logarOperacaoSync = { entityType, entityId, operation, status, error, payload ->
            logarOperacaoSync(entityType, entityId, operation, status, error, payload)
        }
    )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun deletarColaborador(colaborador: Colaborador) = colaboradorRepositoryInternal.deletarColaborador(colaborador)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun aprovarColaborador(colaboradorId: Long, dataAprovacao: java.util.Date, aprovadoPor: String) = 
        colaboradorRepositoryInternal.aprovarColaborador(colaboradorId, dataAprovacao, aprovadoPor)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun aprovarColaboradorComCredenciais(
        colaboradorId: Long,
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        dataAprovacao: java.util.Date,
        aprovadoPor: String
    ) = colaboradorRepositoryInternal.aprovarColaboradorComCredenciais(
        colaboradorId, email, senha, nivelAcesso, observacoes, dataAprovacao, aprovadoPor
    )
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun alterarStatusColaborador(colaboradorId: Long, ativo: Boolean) = 
        colaboradorRepositoryInternal.alterarStatusColaborador(colaboradorId, ativo)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun atualizarUltimoAcessoColaborador(colaboradorId: Long, dataUltimoAcesso: java.util.Date) = 
        colaboradorRepositoryInternal.atualizarUltimoAcessoColaborador(colaboradorId, dataUltimoAcesso)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun contarColaboradoresAtivos() = colaboradorRepositoryInternal.contarColaboradoresAtivos()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para ColaboradorRepositoryInternal
     */
    suspend fun contarColaboradoresPendentesAprovacao() = colaboradorRepositoryInternal.contarColaboradoresPendentesAprovacao()
    
    // ==================== META COLABORADOR ====================
    
    // ✅ FASE 12.14 Etapa 6: Delegado para MetaRepositoryInternal
    fun obterMetasPorColaborador(colaboradorId: Long) = metaRepositoryInternal.obterMetasPorColaborador(colaboradorId)
    suspend fun obterMetaAtual(colaboradorId: Long, tipoMeta: TipoMeta) = metaRepositoryInternal.obterMetaAtual(colaboradorId, tipoMeta)
    
    suspend fun inserirMeta(meta: MetaColaborador): Long = 
        metaRepositoryInternal.inserirMeta(
            meta = meta,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarMeta(meta: MetaColaborador) = 
        metaRepositoryInternal.atualizarMeta(
            meta = meta,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun deletarMeta(meta: MetaColaborador) = 
        metaRepositoryInternal.deletarMeta(
            meta = meta,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    
    suspend fun atualizarValorAtualMeta(metaId: Long, valorAtual: Double) = 
        metaRepositoryInternal.atualizarValorAtualMeta(metaId, valorAtual)
    
    // ==================== METAS POR ROTA ====================
    
    // ✅ FASE 12.14 Etapa 6: Delegado para MetaRepositoryInternal
    fun obterMetasPorRota(rotaId: Long) = metaRepositoryInternal.obterMetasPorRota(rotaId)
    fun obterMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = 
        metaRepositoryInternal.obterMetasPorColaboradorECiclo(colaboradorId, cicloId)
    fun obterMetasPorColaboradorERota(colaboradorId: Long, rotaId: Long) = 
        metaRepositoryInternal.obterMetasPorColaboradorERota(colaboradorId, rotaId)
    fun obterMetasPorColaboradorCicloERota(colaboradorId: Long, cicloId: Long, rotaId: Long) = 
        metaRepositoryInternal.obterMetasPorColaboradorCicloERota(colaboradorId, cicloId, rotaId)
    suspend fun desativarMetasColaborador(colaboradorId: Long) = 
        metaRepositoryInternal.desativarMetasColaborador(colaboradorId)
    
    // Métodos para metas
    suspend fun buscarMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = 
        metaRepositoryInternal.buscarMetasPorColaboradorECiclo(colaboradorId, cicloId)
    suspend fun buscarMetasPorRotaECiclo(rotaId: Long, cicloId: Long) = 
        metaRepositoryInternal.buscarMetasPorRotaECiclo(rotaId, cicloId)
    
    suspend fun existeMetaDuplicada(rotaId: Long, cicloId: Long, tipoMeta: TipoMeta): Boolean = 
        metaRepositoryInternal.existeMetaDuplicada(rotaId, cicloId, tipoMeta)
    
    // ==================== FUNÇÕES PARA SISTEMA DE METAS ====================
    
    /**
     * Busca colaborador responsável principal por uma rota
     */
    /**
     * ✅ FASE 12.14 Etapa 2: Busca colaborador responsável principal usando repository interno
     */
    suspend fun buscarColaboradorResponsavelPrincipal(rotaId: Long): Colaborador? {
        return try {
            // O DAO retorna Colaborador criptografado, então usa o repository interno para descriptografar
            val colaboradorEncrypted = colaboradorDao.buscarColaboradorResponsavelPrincipal(rotaId)
            colaboradorRepositoryInternal.descriptografarColaborador(colaboradorEncrypted)
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
    
    
    // ✅ FASE 12.14 Etapa 6: Delegado para MetaRepositoryInternal
    fun buscarMetasAtivasPorColaborador(colaboradorId: Long) = metaRepositoryInternal.buscarMetasAtivasPorColaborador(colaboradorId)
    suspend fun buscarMetasPorTipoECiclo(tipoMeta: TipoMeta, cicloId: Long) = metaRepositoryInternal.buscarMetasPorTipoECiclo(tipoMeta, cicloId)
    
    // ==================== COLABORADOR ROTA ====================
    
    fun obterRotasPorColaborador(colaboradorId: Long) = colaboradorDao.obterRotasPorColaborador(colaboradorId)
    // ✅ NOTA: obterColaboradoresPorRota retorna ColaboradorRota (relação), não Colaborador
    // ColaboradorRota não contém dados sensíveis, então não precisa descriptografar
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
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    fun obterTodosCiclos() = cicloRepositoryInternal.obterTodosCiclos()
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    suspend fun buscarUltimoCicloFinalizadoPorRota(rotaId: Long) = 
        cicloRepositoryInternal.buscarUltimoCicloFinalizadoPorRota(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    suspend fun buscarCiclosPorRotaEAno(rotaId: Long, ano: Int) = 
        cicloRepositoryInternal.buscarCiclosPorRotaEAno(rotaId, ano)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    suspend fun buscarCiclosPorRota(rotaId: Long) = cicloRepositoryInternal.buscarCiclosPorRota(rotaId)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    suspend fun buscarProximoNumeroCiclo(rotaId: Long, ano: Int) = 
        cicloRepositoryInternal.buscarProximoNumeroCiclo(rotaId, ano)
    
    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    suspend fun inserirCicloAcerto(ciclo: CicloAcertoEntity): Long = cicloRepositoryInternal.inserirCicloAcerto(
        ciclo = ciclo,
        logDbInsertStart = { entity, info -> logDbInsertStart(entity, info) },
        logDbInsertSuccess = { entity, info -> logDbInsertSuccess(entity, info) },
        logDbInsertError = { entity, info, error -> logDbInsertError(entity, info, error) },
        adicionarOperacaoSync = { entityType, entityId, operation, payload, priority ->
            adicionarOperacaoSync(entityType, entityId, operation, payload, priority)
        },
        logarOperacaoSync = { entityType, entityId, operation, status, error, payload ->
            logarOperacaoSync(entityType, entityId, operation, status, error, payload)
        }
    )

    /**
     * ✅ FASE 12.14 Etapa 2: Delegado para CicloRepositoryInternal
     */
    suspend fun buscarCiclosParaMetas(rotaId: Long): List<CicloAcertoEntity> = 
        cicloRepositoryInternal.buscarCiclosParaMetas(rotaId)
    
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
                .filter { ciclo ->
                    if (ciclo.numeroCiclo != numeroCiclo) return@filter false
                    val calendar = java.util.Calendar.getInstance().apply { time = ciclo.dataInicio }
                    calendar.get(java.util.Calendar.YEAR) == ano
                }
            
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
    @Suppress("UNUSED_PARAMETER")
    suspend fun syncRotas(_rotas: List<Rota>) {
        // BLOQUEADO: Sincronização de rotas desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC ROTAS BLOQUEADO - Evitando população automática")
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun syncClientes(_clientes: List<Cliente>) {
        // BLOQUEADO: Sincronização de clientes desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC CLIENTES BLOQUEADO - Evitando população automática")
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun syncAcertos(_acertos: List<Acerto>) {
        // BLOQUEADO: Sincronização de acertos desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC ACERTOS BLOQUEADO - Evitando população automática")
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun syncColaboradores(_colaboradores: List<Colaborador>) {
        // BLOQUEADO: Sincronização de colaboradores desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC COLABORADORES BLOQUEADO - Evitando população automática")
    }
    
    // ==================== CONTRATOS DE LOCAÇÃO ====================
    
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    fun buscarContratosPorCliente(clienteId: Long) = contratoRepositoryInternal.buscarContratosPorCliente(clienteId, ::decryptContratoLocacao)
    suspend fun buscarContratoPorNumero(numeroContrato: String) = contratoRepositoryInternal.buscarContratoPorNumero(numeroContrato, ::decryptContratoLocacao)
    fun buscarContratosAtivos() = contratoRepositoryInternal.buscarContratosAtivos(::decryptContratoLocacao)
    fun buscarTodosContratos() = contratoRepositoryInternal.buscarTodosContratos(::decryptContratoLocacao)
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun contarContratosPorAno(ano: String): Int = contratoRepositoryInternal.contarContratosPorAno(ano)
    suspend fun contarContratosGerados() = contratoRepositoryInternal.contarContratosGerados()
    suspend fun contarContratosAssinados() = contratoRepositoryInternal.contarContratosAssinados()
    suspend fun obterContratosAssinados() = contratoRepositoryInternal.obterContratosAssinados(::decryptContratoLocacao)
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun inserirContrato(contrato: ContratoLocacao): Long {
        val obterAssinaturaAtiva: suspend () -> AssinaturaRepresentanteLegal? = {
            try { 
                decryptAssinaturaRepresentanteLegal(assinaturaRepresentanteLegalDao.obterAssinaturaAtiva())
            } catch (_: Exception) { null }
        }
        return contratoRepositoryInternal.inserirContrato(
            contrato = contrato,
            encryptContrato = ::encryptContratoLocacao,
            obterAssinaturaAtiva = obterAssinaturaAtiva,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    }
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun atualizarContrato(contrato: ContratoLocacao) {
        val obterAssinaturaAtiva: suspend () -> AssinaturaRepresentanteLegal? = {
            try { 
                decryptAssinaturaRepresentanteLegal(assinaturaRepresentanteLegalDao.obterAssinaturaAtiva())
            } catch (_: Exception) { null }
        }
        contratoRepositoryInternal.atualizarContrato(
            contrato = contrato,
            encryptContrato = ::encryptContratoLocacao,
            obterAssinaturaAtiva = obterAssinaturaAtiva,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    }

    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun encerrarContrato(contratoId: Long, clienteId: Long, status: String) {
        contratoRepositoryInternal.encerrarContrato(
            contratoId = contratoId,
            clienteId = clienteId,
            status = status,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    }
    
    suspend fun excluirContrato(contrato: ContratoLocacao) {
        contratoRepositoryInternal.excluirContrato(
            contrato = contrato,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )
    }
    
    suspend fun buscarContratoPorId(contratoId: Long) = contratoRepositoryInternal.buscarContratoPorId(contratoId, ::decryptContratoLocacao)
    suspend fun buscarMesasPorContrato(contratoId: Long) = contratoRepositoryInternal.buscarMesasPorContrato(contratoId)
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun inserirContratoMesa(contratoMesa: ContratoMesa): Long = contratoRepositoryInternal.inserirContratoMesa(
        contratoMesa = contratoMesa,
        logDbInsertStart = ::logDbInsertStart,
        logDbInsertSuccess = ::logDbInsertSuccess,
        logDbInsertError = ::logDbInsertError,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )
    
    suspend fun inserirContratoMesas(contratoMesas: List<ContratoMesa>): List<Long> = contratoRepositoryInternal.inserirContratoMesas(
        contratoMesas = contratoMesas,
        logDbInsertStart = ::logDbInsertStart,
        logDbInsertSuccess = ::logDbInsertSuccess,
        logDbInsertError = ::logDbInsertError,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )
    
    suspend fun excluirContratoMesa(contratoMesa: ContratoMesa) = contratoRepositoryInternal.excluirContratoMesa(
        contratoMesa = contratoMesa,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )
    
    suspend fun excluirMesasPorContrato(contratoId: Long) = contratoRepositoryInternal.excluirMesasPorContrato(contratoId)
    
    // ==================== ADITIVO CONTRATO ====================
    
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    fun buscarAditivosPorContrato(contratoId: Long) = contratoRepositoryInternal.buscarAditivosPorContrato(contratoId)
    suspend fun buscarAditivoPorNumero(numeroAditivo: String) = contratoRepositoryInternal.buscarAditivoPorNumero(numeroAditivo)
    suspend fun buscarAditivoPorId(aditivoId: Long) = contratoRepositoryInternal.buscarAditivoPorId(aditivoId)
    fun buscarTodosAditivos() = contratoRepositoryInternal.buscarTodosAditivos()
    suspend fun contarAditivosPorAno(ano: String): Int = contratoRepositoryInternal.contarAditivosPorAno(ano)
    suspend fun contarAditivosGerados() = contratoRepositoryInternal.contarAditivosGerados()
    suspend fun contarAditivosAssinados() = contratoRepositoryInternal.contarAditivosAssinados()
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun inserirAditivo(aditivo: AditivoContrato): Long = contratoRepositoryInternal.inserirAditivo(
        aditivo = aditivo,
        logDbInsertStart = ::logDbInsertStart,
        logDbInsertSuccess = ::logDbInsertSuccess,
        logDbInsertError = ::logDbInsertError,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )

    suspend fun inserirAditivoMesas(aditivoMesas: List<AditivoMesa>): List<Long> = contratoRepositoryInternal.inserirAditivoMesas(
        aditivoMesas = aditivoMesas,
        logDbInsertStart = ::logDbInsertStart,
        logDbInsertSuccess = ::logDbInsertSuccess,
        logDbInsertError = ::logDbInsertError,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )

    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun inserirAssinaturaRepresentanteLegal(assinatura: AssinaturaRepresentanteLegal): Long = 
        contratoRepositoryInternal.inserirAssinaturaRepresentanteLegal(
            assinatura = assinatura,
            encryptAssinatura = ::encryptAssinaturaRepresentanteLegal,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )

    suspend fun inserirLogAuditoriaAssinatura(log: LogAuditoriaAssinatura): Long = 
        contratoRepositoryInternal.inserirLogAuditoriaAssinatura(
            log = log,
            encryptLog = ::encryptLogAuditoriaAssinatura,
            logDbInsertStart = ::logDbInsertStart,
            logDbInsertSuccess = ::logDbInsertSuccess,
            logDbInsertError = ::logDbInsertError,
            adicionarOperacaoSync = ::adicionarOperacaoSync,
            logarOperacaoSync = ::logarOperacaoSync
        )

    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun atualizarAditivo(aditivo: AditivoContrato) = contratoRepositoryInternal.atualizarAditivo(
        aditivo = aditivo,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )
    
    suspend fun excluirAditivo(aditivo: AditivoContrato) = contratoRepositoryInternal.excluirAditivo(
        aditivo = aditivo,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )
    
    suspend fun buscarMesasPorAditivo(aditivoId: Long) = contratoRepositoryInternal.buscarMesasPorAditivo(aditivoId)
    
    suspend fun excluirAditivoMesa(aditivoMesa: AditivoMesa) = contratoRepositoryInternal.excluirAditivoMesa(
        aditivoMesa = aditivoMesa,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )
    
    suspend fun excluirTodasMesasDoAditivo(aditivoId: Long) = contratoRepositoryInternal.excluirTodasMesasDoAditivo(
        aditivoId = aditivoId,
        adicionarOperacaoSync = ::adicionarOperacaoSync,
        logarOperacaoSync = ::logarOperacaoSync
    )
    
    // ==================== ASSINATURA REPRESENTANTE LEGAL ====================
    
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun obterAssinaturaRepresentanteLegalAtiva() = contratoRepositoryInternal.obterAssinaturaRepresentanteLegalAtiva(::decryptAssinaturaRepresentanteLegal)
    fun obterAssinaturaRepresentanteLegalAtivaFlow() = contratoRepositoryInternal.obterAssinaturaRepresentanteLegalAtivaFlow(::decryptAssinaturaRepresentanteLegal)
    suspend fun obterTodasAssinaturasRepresentanteLegal() = contratoRepositoryInternal.obterTodasAssinaturasRepresentanteLegal(::decryptAssinaturaRepresentanteLegal)
    fun obterTodasAssinaturasRepresentanteLegalFlow() = contratoRepositoryInternal.obterTodasAssinaturasRepresentanteLegalFlow(::decryptAssinaturaRepresentanteLegal)
    suspend fun obterAssinaturaRepresentanteLegalPorId(id: Long) = contratoRepositoryInternal.obterAssinaturaRepresentanteLegalPorId(id, ::decryptAssinaturaRepresentanteLegal)
    suspend fun atualizarAssinaturaRepresentanteLegal(assinatura: AssinaturaRepresentanteLegal) = 
        contratoRepositoryInternal.atualizarAssinaturaRepresentanteLegal(assinatura, ::encryptAssinaturaRepresentanteLegal)
    suspend fun desativarAssinaturaRepresentanteLegal(id: Long) = contratoRepositoryInternal.desativarAssinaturaRepresentanteLegal(id)
    suspend fun incrementarUsoAssinatura(id: Long, dataUso: java.util.Date) = contratoRepositoryInternal.incrementarUsoAssinatura(id, dataUso)
    suspend fun contarAssinaturasRepresentanteLegalAtivas() = contratoRepositoryInternal.contarAssinaturasRepresentanteLegalAtivas()
    suspend fun obterAssinaturasRepresentanteLegalValidadas() = contratoRepositoryInternal.obterAssinaturasRepresentanteLegalValidadas(::decryptAssinaturaRepresentanteLegal)
    
    // ==================== LOGS DE AUDITORIA ====================
    
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun obterTodosLogsAuditoria() = contratoRepositoryInternal.obterTodosLogsAuditoria(::decryptLogAuditoriaAssinatura)
    fun obterTodosLogsAuditoriaFlow() = contratoRepositoryInternal.obterTodosLogsAuditoriaFlow(::decryptLogAuditoriaAssinatura)
    suspend fun obterLogsAuditoriaPorAssinatura(idAssinatura: Long) = contratoRepositoryInternal.obterLogsAuditoriaPorAssinatura(idAssinatura, ::decryptLogAuditoriaAssinatura)
    suspend fun obterLogsAuditoriaPorContrato(idContrato: Long) = contratoRepositoryInternal.obterLogsAuditoriaPorContrato(idContrato, ::decryptLogAuditoriaAssinatura)
    suspend fun obterLogsAuditoriaPorTipoOperacao(tipoOperacao: String) = contratoRepositoryInternal.obterLogsAuditoriaPorTipoOperacao(tipoOperacao, ::decryptLogAuditoriaAssinatura)
    suspend fun obterLogsAuditoriaPorPeriodo(dataInicio: java.util.Date, dataFim: java.util.Date) = contratoRepositoryInternal.obterLogsAuditoriaPorPeriodo(dataInicio, dataFim, ::decryptLogAuditoriaAssinatura)
    suspend fun obterLogsAuditoriaPorUsuario(usuario: String) = contratoRepositoryInternal.obterLogsAuditoriaPorUsuario(usuario, ::decryptLogAuditoriaAssinatura)
    suspend fun obterLogsAuditoriaComErro() = contratoRepositoryInternal.obterLogsAuditoriaComErro(::decryptLogAuditoriaAssinatura)
    suspend fun contarLogsAuditoriaDesde(dataInicio: java.util.Date) = contratoRepositoryInternal.contarLogsAuditoriaDesde(dataInicio)
    suspend fun contarUsosAssinaturaAuditoria(idAssinatura: Long) = contratoRepositoryInternal.contarUsosAssinaturaAuditoria(idAssinatura)
    suspend fun obterLogsAuditoriaNaoValidados() = contratoRepositoryInternal.obterLogsAuditoriaNaoValidados(::decryptLogAuditoriaAssinatura)
    suspend fun validarLogAuditoria(id: Long, dataValidacao: java.util.Date, validadoPor: String) = contratoRepositoryInternal.validarLogAuditoria(id, dataValidacao, validadoPor)
    
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
            val fim = ciclo.dataFim
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
    suspend fun inserirAcertoMesa(acertoMesa: AcertoMesa): Long = inserirAcertoMesaSync(acertoMesa)
    
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

                // ✅ CRÍTICO: Aguardar mais tempo para garantir que uploads de fotos sejam concluídos
                // O delay de 5000ms em SettlementViewModel pode não ser suficiente se o upload demorar
                Log.d("AppRepository", "📷 Aguardando 2 segundos adicionais para garantir uploads completos...")
                kotlinx.coroutines.delay(2000)
                Log.d("AppRepository", "📷 Delay concluído, verificando cache...")
            
            // ✅ CRÍTICO: Buscar mesas do banco novamente para garantir que temos as URLs atualizadas
            // (caso inserirAcertoMesaSync já tenha feito upload)
            val acertoMesas = acertoMesaDao.buscarPorAcertoId(acertoId)
            
            // ✅ CRÍTICO: Verificar se o cache está vazio e tentar fazer upload novamente se necessário
            if (fotoFirebaseUrlCache.isEmpty() && acertoMesas.isNotEmpty()) {
                Log.w("AppRepository", "⚠️ AVISO: Cache de URLs do Firebase está vazio!")
                Log.w("AppRepository", "   Isso pode indicar que o upload não foi feito em inserirAcertoMesaSync()")
                Log.w("AppRepository", "   Tentando fazer upload novamente para ${acertoMesas.size} mesas...")
            }
            android.util.Log.d("AppRepository", "Incluindo ${acertoMesas.size} mesas no payload do acerto $acertoId")
            
            // ✅ OFICIAL FIREBASE: Usar URLs que já foram enviadas em inserirAcertoMesaSync()
            // O upload já foi feito quando inserirAcertoMesaSync() foi chamado
            // O banco já foi atualizado com a URL do Firebase Storage se upload foi bem-sucedido
            Log.d("AppRepository", "📷 adicionarAcertoComMesasParaSync: Processando ${acertoMesas.size} mesas para acerto $acertoId")
            Log.d("AppRepository", "📷 Cache de URLs do Firebase possui ${fotoFirebaseUrlCache.size} entradas")
            fotoFirebaseUrlCache.forEach { (id, url) ->
                Log.d("AppRepository", "   Cache: AcertoMesa ID=$id -> URL='$url'")
            }
            
            acertoMesas.forEachIndexed { index, mesa ->
                val fotoUrl = mesa.fotoRelogioFinal
                val isFirebaseUrl = !fotoUrl.isNullOrBlank() && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)
                val temNoCache = fotoFirebaseUrlCache.containsKey(mesa.id)
                Log.d("AppRepository", "📷 Mesa ${index + 1}: id=${mesa.id}, mesaId=${mesa.mesaId}")
                Log.d("AppRepository", "   Foto no banco: '$fotoUrl' (é URL Firebase: $isFirebaseUrl)")
                Log.d("AppRepository", "   Tem no cache: $temNoCache ${if (temNoCache) "-> URL: '${fotoFirebaseUrlCache[mesa.id]}'" else ""}")
            }
            
            // ✅ ESTRATÉGIA DEFINITIVA: Buscar URLs do Firebase Storage a partir do payload individual de cada mesa
            // Como inserirAcertoMesaSync() já fez upload e adicionou à fila de sync, precisamos buscar
            // a URL do Firebase Storage de outra forma. Vamos verificar se há uma operação de sync pendente
            // ou tentar fazer upload novamente se necessário
            
            val acertoMesasComFotos = acertoMesas.map { acertoMesa ->
                // ✅ ESTRATÉGIA DEFINITIVA: Usar cache primeiro, depois verificar banco
                // 1. Verificar cache (URL do Firebase armazenada em inserirAcertoMesaSync)
                // 2. Se não encontrar, verificar se o banco tem URL do Firebase
                // 3. Se não encontrar, tentar fazer upload do caminho local
                val fotoUrlParaPayload = when {
                    // ✅ PRIORIDADE 1: Cache (URL do Firebase armazenada durante inserção)
                    fotoFirebaseUrlCache.containsKey(acertoMesa.id) -> {
                        val urlDoCache = fotoFirebaseUrlCache[acertoMesa.id]!!
                        Log.d("AppRepository", "📷 ✅ Mesa ${acertoMesa.mesaId}: URL encontrada no cache: '$urlDoCache'")
                        urlDoCache
                    }
                    // ✅ PRIORIDADE 2: Banco já tem URL do Firebase (raro, mas possível)
                    !acertoMesa.fotoRelogioFinal.isNullOrBlank() && 
                    com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(acertoMesa.fotoRelogioFinal) -> {
                        Log.d("AppRepository", "📷 ✅ Mesa ${acertoMesa.mesaId}: URL encontrada no banco: '${acertoMesa.fotoRelogioFinal}'")
                        acertoMesa.fotoRelogioFinal
                    }
                    // ✅ PRIORIDADE 3: Caminho local - tentar fazer upload
                    !acertoMesa.fotoRelogioFinal.isNullOrBlank() -> {
                        Log.d("AppRepository", "📷 Tentando upload para mesa ${acertoMesa.mesaId}: caminho local='${acertoMesa.fotoRelogioFinal}'")
                        val empresaId = obterEmpresaId()
                        val fotoUrl = uploadFotoSeNecessario(
                            caminhoLocal = acertoMesa.fotoRelogioFinal,
                            tipo = "relogio_final",
                            empresaId = empresaId,
                            entityId = acertoMesa.acertoId,
                            entityExtraId = acertoMesa.mesaId
                        )
                        if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                            Log.d("AppRepository", "📷 ✅ Upload bem-sucedido agora: '$fotoUrl'")
                            // ✅ Armazenar no cache para próximas chamadas
                            fotoFirebaseUrlCache[acertoMesa.id] = fotoUrl
                            fotoUrl // Usar URL no payload
                        } else {
                            Log.w("AppRepository", "⚠️ Upload falhou - foto não será sincronizada (caminho local mantido)")
                            "" // String vazia no payload
                        }
                    }
                    // Sem foto
                    else -> {
                        Log.d("AppRepository", "📷 Mesa ${acertoMesa.mesaId}: Sem foto")
                        "" // String vazia no payload
                    }
                }
                
                // ✅ Criar cópia APENAS para o payload (não alterar o banco)
                // O campo fotoRelogioFinal no banco continua com o caminho local
                acertoMesa.copy(fotoRelogioFinal = fotoUrlParaPayload)
            }
            
            // ✅ Limpar cache após usar (para evitar vazamento de memória)
            // O cache será usado apenas durante o processo de sincronização
            // Log.d("AppRepository", "📷 Limpando cache de URLs do Firebase (${fotoFirebaseUrlCache.size} entradas)")
            // fotoFirebaseUrlCache.clear() // ✅ COMENTADO: Manter cache até sincronização completa
            
            Log.d("AppRepository", "📷 ✅ Payload preparado com ${acertoMesasComFotos.size} mesas. Criando payload do Firestore...")
            Log.d("AppRepository", "📷 Cache de URLs do Firebase: ${fotoFirebaseUrlCache.size} entradas para este acerto")
            acertoMesasComFotos.forEachIndexed { index, mesa ->
                val fotoUrl = mesa.fotoRelogioFinal
                Log.d("AppRepository", "📷 Mesa ${index + 1} no payload: mesaId=${mesa.mesaId}, fotoUrl='$fotoUrl' (é URL Firebase: ${!fotoUrl.isNullOrBlank() && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)})")
            }
            
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
                "acertoMesas" to acertoMesasComFotos.map { acertoMesa ->
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
                        "fotoRelogioFinal" to (acertoMesa.fotoRelogioFinal ?: ""),
                        "dataFoto" to (acertoMesa.dataFoto?.time ?: "null"),
                        "dataCriacao" to acertoMesa.dataCriacao.time
                    )
                }
            )
            
            // ✅ LOG CRÍTICO: Verificar payload final antes de enviar
            val payloadFinal = com.google.gson.Gson().toJson(payloadMap)
            Log.d("AppRepository", "📷 ========================================")
            Log.d("AppRepository", "📷 PAYLOAD FINAL DO ACERTO PRONTO PARA SYNC:")
            Log.d("AppRepository", "📷   Acerto ID: $acertoId")
            @Suppress("UNCHECKED_CAST")
            val acertoMesasPayload = payloadMap["acertoMesas"] as? List<Map<String, Any?>>
            acertoMesasPayload?.forEachIndexed { index, mesa ->
                val fotoUrl = mesa["fotoRelogioFinal"] as? String
                val temUrl = !fotoUrl.isNullOrBlank()
                val isFirebaseUrl = temUrl && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)
                Log.d("AppRepository", "📷   Mesa ${index + 1} (ID: ${mesa["mesaId"]}): fotoUrl='$fotoUrl'")
                Log.d("AppRepository", "📷     Tem URL? $temUrl | É URL Firebase? $isFirebaseUrl")
                if (!temUrl || !isFirebaseUrl) {
                    Log.e("AppRepository", "📷     ❌ PROBLEMA: URL não está sendo enviada corretamente!")
                } else {
                    Log.d("AppRepository", "📷     ✅ URL do Firebase será enviada ao Firestore")
                }
            }
            Log.d("AppRepository", "📷 ========================================")
            
            adicionarOperacaoSync("Acerto", acertoId, "CREATE", payloadFinal, priority = 1)
            logarOperacaoSync("Acerto", acertoId, "CREATE", "PENDING", null, payloadFinal)
            
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
    // ✅ CORREÇÃO OFICIAL: Room detecta mudanças em JOIN apenas quando TODAS as tabelas envolvidas são atualizadas
    // A query obterClientesPorRotaComDebitoAtual faz JOIN com acertos, então precisamos atualizar AMBAS as tabelas
    @androidx.room.Transaction
    suspend fun reconciliarDebitosClientes() {
        try {
            Log.d("AppRepository", "🔄 Reconciliando débitos dos clientes com base no último acerto...")
            val clientes = clienteDao.obterTodos().first()
            var atualizados = 0
            val clientesParaAtualizar = mutableListOf<Triple<Long, Double, Acerto?>>()
            
            // ✅ CORREÇÃO: Primeiro, coletar todos os débitos que precisam ser atualizados
            for (cliente in clientes) {
                try {
                    val ultimoAcerto = acertoDao.buscarUltimoAcertoPorCliente(cliente.id)
                    val debitoUltimo = ultimoAcerto?.debitoAtual ?: 0.0
                    if (kotlin.math.abs(debitoUltimo - cliente.debitoAtual) > 0.01) { // Comparação com tolerância para double
                        clientesParaAtualizar.add(Triple(cliente.id, debitoUltimo, ultimoAcerto))
                        Log.d("AppRepository", "📝 Cliente ${cliente.id} (${cliente.nome}): debito_atual ${cliente.debitoAtual} -> $debitoUltimo")
                    }
                } catch (e: Exception) {
                    Log.w("AppRepository", "⚠️ Falha ao calcular débito do cliente ${cliente.id}: ${e.message}")
                }
            }
            
            // ✅ CORREÇÃO OFICIAL: Atualizar clientes E acertos em uma única transação
            // Isso garante que o Room detecte mudanças na query com JOIN
            for ((clienteId, novoDebito, ultimoAcerto) in clientesParaAtualizar) {
                try {
                    // 1. Atualizar cliente
                    val cliente = obterClientePorId(clienteId)
                    if (cliente != null) {
                        val clienteAtualizado = cliente.copy(
                            debitoAtual = novoDebito,
                            dataUltimaAtualizacao = java.util.Date()
                        )
                        clienteDao.atualizar(clienteAtualizado)
                        
                        // 2. ✅ CRÍTICO: Atualizar também o último acerto para forçar o Room a detectar mudança no JOIN
                        if (ultimoAcerto != null) {
                            val acertoAtualizado = ultimoAcerto.copy(
                                syncTimestamp = System.currentTimeMillis()
                            )
                            acertoDao.atualizar(acertoAtualizado)
                        }
                        
                        atualizados++
                        Log.d("AppRepository", "✅ Cliente $clienteId atualizado: debito_atual = $novoDebito (cliente + acerto atualizados)")
                    }
                } catch (e: Exception) {
                    Log.w("AppRepository", "⚠️ Falha ao atualizar cliente $clienteId: ${e.message}")
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
    // ✅ FASE 12.14 Etapa 3: Delegado para ContratoRepositoryInternal
    suspend fun buscarContratoAtivoPorCliente(clienteId: Long) = contratoRepositoryInternal.buscarContratoAtivoPorCliente(clienteId, ::decryptContratoLocacao)
    suspend fun inserirHistoricoManutencaoMesa(historico: HistoricoManutencaoMesa): Long = inserirHistoricoManutencaoMesaSync(historico)

    // ========================================
    // ✅ FASE 3C: MÉTODOS DE SINCRONIZAÇÃO
    // ========================================

    /**
     * Obter empresaId das configurações de sincronização
     */
    private suspend fun obterEmpresaId(): String {
        return try {
            val config = syncConfigDao.buscarSyncConfigPorChave("empresa_id")
            config?.value ?: "empresa_001" // Default se não encontrar
        } catch (e: Exception) {
            Log.w("AppRepository", "Erro ao obter empresa_id, usando default: ${e.message}")
            "empresa_001"
        }
    }
    
    /**
     * Upload de foto antes de sincronizar (helper genérico)
     */
    private suspend fun uploadFotoSeNecessario(
        caminhoLocal: String?,
        tipo: String,
        empresaId: String,
        entityId: Long,
        entityExtraId: Long? = null
    ): String? {
        if (caminhoLocal.isNullOrBlank()) {
            Log.d("AppRepository", "📷 Nenhum caminho de foto fornecido para $tipo (entityId=$entityId)")
            return null
        }
        
        // ✅ Verificar se já é uma URL do Firebase Storage (não fazer upload novamente)
        if (com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(caminhoLocal)) {
            Log.d("AppRepository", "📷 Foto já está no Firebase Storage: $caminhoLocal")
            return caminhoLocal
        }
        
        // ✅ CRÍTICO: Verificar se o arquivo existe antes de fazer upload
        val arquivo = java.io.File(caminhoLocal)
        if (!arquivo.exists()) {
            Log.e("AppRepository", "❌ ERRO CRÍTICO: Arquivo de foto não existe: $caminhoLocal")
            Log.e("AppRepository", "❌ Tipo: $tipo, EntityId: $entityId, EntityExtraId: $entityExtraId")
            return null // Retornar null em vez de caminho local inválido
        }
        
        Log.d("AppRepository", "📷 Iniciando upload de foto:")
        Log.d("AppRepository", "   Tipo: $tipo")
        Log.d("AppRepository", "   Caminho local: $caminhoLocal")
        Log.d("AppRepository", "   Tamanho: ${arquivo.length()} bytes")
        Log.d("AppRepository", "   EntityId: $entityId, EntityExtraId: $entityExtraId")
        
        return try {
            val urlFirebase = when (tipo) {
                "comprovante" -> {
                    Log.d("AppRepository", "📤 Uploading comprovante para despesa $entityId")
                    com.example.gestaobilhares.utils.FirebaseStorageManager.uploadFotoComprovante(
                        empresaId, entityId, caminhoLocal
                    )
                }
                "relogio_final" -> {
                    if (entityExtraId != null) {
                        Log.d("AppRepository", "📤 Uploading foto relógio final para acerto $entityId, mesa $entityExtraId")
                        com.example.gestaobilhares.utils.FirebaseStorageManager.uploadFotoRelogioFinal(
                            empresaId, entityId, entityExtraId, caminhoLocal
                        )
                    } else {
                        Log.e("AppRepository", "❌ entityExtraId é null para relogio_final")
                        null
                    }
                }
                "foto_reforma" -> {
                    Log.d("AppRepository", "📤 Uploading foto reforma para mesa reformada $entityId")
                    com.example.gestaobilhares.utils.FirebaseStorageManager.uploadFotoReforma(
                        empresaId, entityId, caminhoLocal
                    )
                }
                "foto_antes", "foto_depois" -> {
                    Log.d("AppRepository", "📤 Uploading foto $tipo para manutenção $entityId")
                    com.example.gestaobilhares.utils.FirebaseStorageManager.uploadFotoManutencao(
                        empresaId, entityId, tipo.replace("foto_", ""), caminhoLocal
                    )
                }
                else -> {
                    Log.e("AppRepository", "❌ Tipo de foto desconhecido: $tipo")
                    null
                }
            }
            
            if (urlFirebase != null) {
                Log.d("AppRepository", "✅ SUCESSO: Foto enviada para Firebase Storage: $urlFirebase")
                urlFirebase // ✅ CRÍTICO: Retornar URL do Firebase, não caminho local
            } else {
                Log.e("AppRepository", "❌ FALHA: Upload de foto retornou null")
                Log.e("AppRepository", "❌ Caminho original: $caminhoLocal")
                // ✅ CRÍTICO: NÃO retornar caminho local como fallback - isso causaria o problema
                // Se o upload falhar, retornar null e não sincronizar a foto
                null
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ EXCEÇÃO ao fazer upload de foto: ${e.message}", e)
            // ✅ CRÍTICO: NÃO retornar caminho local em caso de erro
            null
        }
    }
    
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
     * ✅ FASE 12.5: Removido runBlocking - usando flow builder
     */
    fun buscarRotasComCache(): Flow<List<Rota>> {
        val cacheKey = "rotas_ativas"
        
        return flow {
            val cached = cacheManager.get<List<Rota>>(cacheKey)
            if (cached != null) {
                emit(cached)
            } else {
                Log.d("AppRepository", "Cache MISS: $cacheKey - Carregando do banco")
                val rotas = rotaDao.getAllRotasAtivas().first()
                cacheManager.put(cacheKey, rotas, TimeUnit.MINUTES.toMillis(2))
                emit(rotas)
            }
        }
    }
    
    /**
     * ✅ FASE 4A: Buscar clientes por rota com cache inteligente
     * Cache TTL: 1 minuto (dados de cliente mudam mais frequentemente)
     * ✅ FASE 12.5: Removido runBlocking - usando flow builder
     */
    fun buscarClientesPorRotaComCache(rotaId: Long): Flow<List<Cliente>> {
        val cacheKey = "clientes_rota_$rotaId"
        
        return flow {
            val cached = cacheManager.get<List<Cliente>>(cacheKey)
            if (cached != null) {
                emit(cached)
            } else {
                Log.d("AppRepository", "Cache MISS: $cacheKey - Carregando do banco")
                val clientes = clienteDao.obterClientesPorRota(rotaId).first()
                cacheManager.put(cacheKey, clientes, TimeUnit.MINUTES.toMillis(1))
                emit(clientes)
            }
        }
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
        Log.d("AppRepository", "📷 ========================================")
        Log.d("AppRepository", "📷 inserirAcertoMesaSync: INÍCIO")
        Log.d("AppRepository", "📷 AcertoMesa inserido com ID: $id")
        Log.d("AppRepository", "📷 Mesa ID: ${acertoMesa.mesaId}, Acerto ID: ${acertoMesa.acertoId}")
        Log.d("AppRepository", "📷 Foto original: '${acertoMesa.fotoRelogioFinal}'")
        
        try {
            // ✅ CRÍTICO: Verificar se há foto antes de tentar upload
            if (acertoMesa.fotoRelogioFinal.isNullOrBlank()) {
                Log.w("AppRepository", "📷 ⚠️ Nenhuma foto para mesa ${acertoMesa.mesaId}")
                fotoFirebaseUrlCache.remove(id) // Garantir que não há cache antigo
                return id
            }
            
            // ✅ CRÍTICO: Verificar se o arquivo existe antes de fazer upload
            val arquivo = java.io.File(acertoMesa.fotoRelogioFinal)
            if (!arquivo.exists()) {
                Log.e("AppRepository", "📷 ❌ ERRO CRÍTICO: Arquivo não existe: '${acertoMesa.fotoRelogioFinal}'")
                Log.e("AppRepository", "📷   Caminho absoluto: ${arquivo.absolutePath}")
                Log.e("AppRepository", "📷   Arquivo existe? ${arquivo.exists()}")
                fotoFirebaseUrlCache.remove(id)
                return id
            }
            
            Log.d("AppRepository", "📷 ✅ Arquivo existe: ${arquivo.length()} bytes")
            
            // ✅ NOVO: Upload de foto para Firebase Storage ANTES de sincronizar
            val empresaId = obterEmpresaId()
            Log.d("AppRepository", "📷 Iniciando upload para Firebase Storage...")
            Log.d("AppRepository", "   Empresa ID: $empresaId")
            Log.d("AppRepository", "   Tipo: relogio_final")
            Log.d("AppRepository", "   Entity ID (Acerto): ${acertoMesa.acertoId}")
            Log.d("AppRepository", "   Entity Extra ID (Mesa): ${acertoMesa.mesaId}")
            
            // ✅ CRÍTICO: Fazer upload de forma síncrona e aguardar conclusão
            val fotoUrl = try {
                Log.d("AppRepository", "📷 Iniciando upload de forma síncrona...")
                uploadFotoSeNecessario(
                    caminhoLocal = acertoMesa.fotoRelogioFinal,
                    tipo = "relogio_final",
                    empresaId = empresaId,
                    entityId = acertoMesa.acertoId,
                    entityExtraId = acertoMesa.mesaId
                ).also { url ->
                    Log.d("AppRepository", "📷 Upload concluído, resultado: ${if (url.isNullOrBlank()) "NULL/VAZIO" else "URL: $url"}")
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "📷 ❌ EXCEÇÃO durante upload: ${e.message}", e)
                Log.e("AppRepository", "📷 Stack trace: ${e.stackTraceToString()}")
                null
            }
            
            Log.d("AppRepository", "📷 ========================================")
            Log.d("AppRepository", "📷 RESULTADO DO UPLOAD:")
            Log.d("AppRepository", "📷   fotoUrl retornada: '$fotoUrl'")
            Log.d("AppRepository", "📷   fotoUrl é null? ${fotoUrl == null}")
            Log.d("AppRepository", "📷   fotoUrl é vazio? ${fotoUrl.isNullOrBlank()}")
            
            if (fotoUrl != null && !fotoUrl.isBlank()) {
                val isFirebaseUrl = com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)
                Log.d("AppRepository", "📷   fotoUrl é URL do Firebase? $isFirebaseUrl")
                if (!isFirebaseUrl) {
                    Log.e("AppRepository", "📷   ❌ ERRO: fotoUrl não é uma URL válida do Firebase Storage!")
                    Log.e("AppRepository", "📷   Valor retornado: '$fotoUrl'")
                    Log.e("AppRepository", "📷   Tamanho da string: ${fotoUrl.length}")
                    Log.e("AppRepository", "📷   Primeiros 50 caracteres: ${fotoUrl.take(50)}")
                }
            } else {
                Log.e("AppRepository", "📷   ❌❌❌ ERRO CRÍTICO: Upload retornou null ou vazio!")
                Log.e("AppRepository", "📷   O upload pode ter falhado silenciosamente")
                Log.e("AppRepository", "📷   Verifique os logs do FirebaseStorageManager acima")
            }
            
            // ✅ ESTRATÉGIA DEFINITIVA: MANTER CAMINHO LOCAL NO BANCO SEMPRE
            // - O banco local SEMPRE mantém o caminho local (para uso da UI local)
            // - A URL do Firebase é armazenada em cache temporário para uso no payload completo
            // - Isso garante que o botão "Ver Foto" funcione localmente e a URL seja preservada para sync
            
            // ✅ CRÍTICO: Garantir que o ID correto seja usado (retornado pelo insert)
            val acertoMesaComId = acertoMesa.copy(id = id)
            
            // ✅ CRÍTICO: Armazenar URL do Firebase no cache se upload foi bem-sucedido
            if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                Log.d("AppRepository", "📷 ✅✅✅ UPLOAD BEM-SUCEDIDO ✅✅✅")
                Log.d("AppRepository", "📷 ✅ URL do Firebase: '$fotoUrl'")
                Log.d("AppRepository", "📷 ✅ Armazenando URL no cache para AcertoMesa ID: $id")
                fotoFirebaseUrlCache[id] = fotoUrl
                Log.d("AppRepository", "📷 ✅ Cache atualizado. Total de entradas: ${fotoFirebaseUrlCache.size}")
                Log.d("AppRepository", "📷 ✅ Mantendo caminho local no banco: '${acertoMesa.fotoRelogioFinal}'")
                Log.d("AppRepository", "📷 ✅ URL do Firebase será usada no payload de sincronização via cache")
            } else if (fotoUrl == null && !acertoMesa.fotoRelogioFinal.isNullOrBlank()) {
                Log.e("AppRepository", "📷 ❌❌❌ ERRO CRÍTICO: Upload falhou - fotoUrl retornou null ❌❌❌")
                Log.e("AppRepository", "📷   Caminho local: '${acertoMesa.fotoRelogioFinal}'")
                Log.e("AppRepository", "📷   AcertoMesa ID: $id, Mesa ID: ${acertoMesa.mesaId}, Acerto ID: ${acertoMesa.acertoId}")
                Log.e("AppRepository", "📷   Arquivo existe? ${java.io.File(acertoMesa.fotoRelogioFinal).exists()}")
                Log.w("AppRepository", "📷 ⚠️ Mantendo caminho local no banco para uso local")
                // Remover do cache se existir (upload falhou)
                fotoFirebaseUrlCache.remove(id)
            } else if (fotoUrl != null && !com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                Log.e("AppRepository", "📷 ❌❌❌ ERRO CRÍTICO: Upload retornou URL inválida ❌❌❌")
                Log.e("AppRepository", "📷   Valor: '$fotoUrl'")
                Log.e("AppRepository", "📷   Não é uma URL do Firebase Storage")
                fotoFirebaseUrlCache.remove(id)
            } else {
                Log.d("AppRepository", "📷 Sem foto para AcertoMesa ID: $id")
                // Sem foto - remover do cache se existir
                fotoFirebaseUrlCache.remove(id)
            }
            
            // ✅ Usar URL do Firebase Storage no payload de sync (se disponível)
            // Se não houver URL do Firebase, usar string vazia (não sincronizar foto)
            val fotoUrlParaPayload = if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                fotoUrl // URL do Firebase para sincronização
            } else {
                "" // String vazia - foto não será sincronizada
            }
            
            Log.d("AppRepository", "📷 Payload individual de ACERTOMESA:")
            Log.d("AppRepository", "📷   fotoUrlParaPayload: '$fotoUrlParaPayload'")
            
            val payload = mapOf(
                "id" to id,
                "acertoId" to acertoMesaComId.acertoId,
                "mesaId" to acertoMesaComId.mesaId,
                "relogioInicial" to acertoMesaComId.relogioInicial,
                "relogioFinal" to acertoMesaComId.relogioFinal,
                "valorFicha" to acertoMesaComId.valorFicha,
                "comissaoFicha" to acertoMesaComId.comissaoFicha,
                "subtotal" to acertoMesaComId.subtotal,
                "fotoRelogioFinal" to fotoUrlParaPayload, // URL do Firebase Storage para sync (ou vazio)
                "dataFoto" to (acertoMesaComId.dataFoto?.time ?: "null")
            )
            
            // ✅ NOTA: O payload individual de ACERTOMESA é usado apenas para sincronização individual
            // O payload completo do ACERTO (com mesas aninhadas) é criado em adicionarAcertoComMesasParaSync()
            // e tem prioridade sobre o payload individual
            adicionarOperacaoSync("ACERTOMESA", id, "INSERT", Gson().toJson(payload))
            logarOperacaoSync("ACERTOMESA", id, "INSERT", "Adicionado à fila de sync (URL do Firebase no cache)")
            
            Log.d("AppRepository", "📷 inserirAcertoMesaSync: FIM")
            Log.d("AppRepository", "📷 ========================================")
            
        } catch (e: Exception) {
            Log.e("AppRepository", "📷 ❌ ERRO em inserirAcertoMesaSync: ${e.message}", e)
            e.printStackTrace()
        }
        
        return id
    }
    
    /**
     * Atualizar AcertoMesa com sincronização
     */
    suspend fun atualizarAcertoMesaSync(acertoMesa: AcertoMesa) {
        acertoMesaDao.atualizar(acertoMesa)
        
        try {
            // ✅ NOVO: Upload de foto para Firebase Storage antes de sincronizar
            val empresaId = obterEmpresaId()
            Log.d("AppRepository", "📷 atualizarAcertoMesaSync: Processando foto para mesa ${acertoMesa.mesaId}, acerto ${acertoMesa.acertoId}")
            Log.d("AppRepository", "📷 Foto original: '${acertoMesa.fotoRelogioFinal}'")
            
            val fotoUrl = uploadFotoSeNecessario(
                caminhoLocal = acertoMesa.fotoRelogioFinal,
                tipo = "relogio_final",
                empresaId = empresaId,
                entityId = acertoMesa.acertoId,
                entityExtraId = acertoMesa.mesaId
            )
            
            Log.d("AppRepository", "📷 atualizarAcertoMesaSync: Resultado upload para mesa ${acertoMesa.mesaId}: fotoUrl='$fotoUrl'")
            
            // ✅ ESTRATÉGIA DEFINITIVA: MANTER CAMINHO LOCAL NO BANCO SEMPRE
            // - O banco local SEMPRE mantém o caminho local (para uso da UI local)
            // - A URL do Firebase é usada APENAS no payload de sincronização
            // - Isso garante que o botão "Ver Foto" funcione localmente mesmo sem internet
            
            // ✅ NUNCA substituir o caminho local pela URL do Firebase no banco
            val acertoMesaAtualizado = acertoMesa
            
            if (fotoUrl == null && !acertoMesa.fotoRelogioFinal.isNullOrBlank()) {
                Log.w("AppRepository", "⚠️ Upload falhou - mantendo caminho local no banco para uso local")
                Log.w("AppRepository", "   Caminho local será mantido: '${acertoMesa.fotoRelogioFinal}'")
            } else if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                Log.d("AppRepository", "📷 ✅ Upload bem-sucedido - URL do Firebase: '$fotoUrl'")
                Log.d("AppRepository", "📷 ✅ Mantendo caminho local no banco: '${acertoMesa.fotoRelogioFinal}'")
                Log.d("AppRepository", "📷 ✅ URL do Firebase será usada APENAS no payload de sincronização")
            }
            
            // ✅ Usar URL do Firebase Storage no payload de sync (se disponível)
            val fotoUrlParaPayload = if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                fotoUrl // URL do Firebase para sincronização
            } else {
                "" // String vazia - foto não será sincronizada
            }
            
            // Criar payload com URL da foto
            val payloadMap = mutableMapOf<String, Any?>(
                "id" to acertoMesaAtualizado.id,
                "acertoId" to acertoMesaAtualizado.acertoId,
                "mesaId" to acertoMesaAtualizado.mesaId,
                "relogioInicial" to acertoMesaAtualizado.relogioInicial,
                "relogioFinal" to acertoMesaAtualizado.relogioFinal,
                "valorFicha" to acertoMesaAtualizado.valorFicha,
                "comissaoFicha" to acertoMesaAtualizado.comissaoFicha,
                "subtotal" to acertoMesaAtualizado.subtotal,
                "fotoRelogioFinal" to fotoUrlParaPayload, // URL do Firebase Storage para sync (ou vazio)
                "dataFoto" to (acertoMesaAtualizado.dataFoto?.time ?: "null")
            )
            
            adicionarOperacaoSync("ACERTOMESA", acertoMesaAtualizado.id, "UPDATE", Gson().toJson(payloadMap))
            logarOperacaoSync("ACERTOMESA", acertoMesaAtualizado.id, "UPDATE", "Adicionado à fila de sync")
            
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao adicionar atualização de AcertoMesa à fila de sync: ${e.message}")
        }
    }
    
    /**
     * Atualizar MesaReformada com sincronização (método já existe, apenas adicionando sync)
     */
    // ✅ FASE 12.14 Etapa 5: Delegado para EstoqueRepositoryInternal
    suspend fun atualizarMesaReformadaSync(mesaReformada: MesaReformada) = 
        estoqueRepositoryInternal.atualizarMesaReformada(
            mesaReformada = mesaReformada,
            obterEmpresaId = ::obterEmpresaId,
            uploadFotoSeNecessario = ::uploadFotoSeNecessario,
            adicionarOperacaoSync = ::adicionarOperacaoSync
        )

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


