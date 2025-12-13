package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.domain.VeiculoRepository
import com.example.gestaobilhares.data.repository.domain.MesaRepository
import com.example.gestaobilhares.data.repository.domain.ContratoRepository
import com.example.gestaobilhares.data.repository.domain.ColaboradorRepository
import com.example.gestaobilhares.data.repository.domain.ClienteRepository
import com.example.gestaobilhares.data.repository.domain.AcertoRepository
import com.example.gestaobilhares.data.repository.domain.RotaRepository
import com.example.gestaobilhares.data.repository.domain.DespesaRepository
import com.example.gestaobilhares.data.repository.domain.CicloRepository
import com.example.gestaobilhares.data.repository.domain.MetaRepository
import com.example.gestaobilhares.data.repository.domain.PanoRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import android.util.Log
import java.util.concurrent.TimeUnit
import java.util.Date
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ REPOSITORY CONSOLIDADO E MODERNIZADO - AppRepository
 * 
 * FASE 3: Arquitetura Híbrida Modular - AppRepository como Facade
 * - Delega para repositories especializados em domain/
 * - Mantém compatibilidade com ViewModels existentes
 * - Centralizado para facilitar manutenção
 * - Meta: ~200-300 linhas (delegando para especializados)
 */
@Singleton
class AppRepository @Inject constructor(
    private val clienteDao: ClienteDao,
    private val acertoDao: AcertoDao,
    private val mesaDao: MesaDao,
    private val rotaDao: RotaDao,
    private val despesaDao: DespesaDao,
    private val colaboradorDao: ColaboradorDao,
    private val cicloAcertoDao: CicloAcertoDao,
    private val acertoMesaDao: com.example.gestaobilhares.data.dao.AcertoMesaDao,
    private val contratoLocacaoDao: ContratoLocacaoDao,
    private val aditivoContratoDao: AditivoContratoDao,
    private val assinaturaRepresentanteLegalDao: AssinaturaRepresentanteLegalDao,
    private val logAuditoriaAssinaturaDao: LogAuditoriaAssinaturaDao,
    private val categoriaDespesaDao: com.example.gestaobilhares.data.dao.CategoriaDespesaDao? = null,
    private val tipoDespesaDao: com.example.gestaobilhares.data.dao.TipoDespesaDao? = null,
    private val panoEstoqueDao: com.example.gestaobilhares.data.dao.PanoEstoqueDao? = null,
    private val stockItemDao: com.example.gestaobilhares.data.dao.StockItemDao? = null,
    private val mesaReformadaDao: com.example.gestaobilhares.data.dao.MesaReformadaDao? = null,
    private val mesaVendidaDao: com.example.gestaobilhares.data.dao.MesaVendidaDao? = null,
    private val historicoManutencaoMesaDao: com.example.gestaobilhares.data.dao.HistoricoManutencaoMesaDao? = null,
    private val veiculoDao: com.example.gestaobilhares.data.dao.VeiculoDao? = null,
    private val historicoManutencaoVeiculoDao: com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao? = null,
    private val historicoCombustivelVeiculoDao: com.example.gestaobilhares.data.dao.HistoricoCombustivelVeiculoDao? = null,
    private val panoMesaDao: com.example.gestaobilhares.data.dao.PanoMesaDao? = null,
    private val metaDao: MetaDao? = null,
    private val equipmentDao: com.example.gestaobilhares.data.dao.EquipmentDao? = null,
    private val syncOperationDao: SyncOperationDao? = null,
    // private val  // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
) {
    
    // ✅ NOVO: Repositories especializados (arquitetura modular)
    private val clienteRepository: ClienteRepository by lazy {
        ClienteRepository(clienteDao)
    }
    
    private val acertoRepository: AcertoRepository by lazy {
        AcertoRepository(acertoDao)
    }
    
    private val mesaRepository: MesaRepository by lazy {
        MesaRepository(
            mesaDao,
            mesaReformadaDao,
            panoMesaDao
        )
    }
    
    private val rotaRepository: RotaRepository by lazy {
        RotaRepository(
            rotaDao,
            clienteDao,
            acertoDao,
            cicloAcertoDao
        )
    }
    
    private val despesaRepository: DespesaRepository by lazy {
        DespesaRepository(despesaDao)
    }
    
    private val cicloRepository: CicloRepository by lazy {
        CicloRepository(cicloAcertoDao)
    }
    
    // ✅ NOVO: Instâncias dos repositórios antigos para usar no CicloAcertoRepository
    private val acertoRepositoryLegacy: com.example.gestaobilhares.data.repository.AcertoRepository by lazy {
        com.example.gestaobilhares.data.repository.AcertoRepository(acertoDao, clienteDao)
    }
    
    private val clienteRepositoryLegacy: com.example.gestaobilhares.data.repository.ClienteRepository by lazy {
        com.example.gestaobilhares.data.repository.ClienteRepository(clienteDao, this@AppRepository)
    }
    
    // ✅ NOVO: Instância do CicloAcertoRepository para usar métodos como finalizarCiclo
    private val cicloAcertoRepository: CicloAcertoRepository by lazy {
        CicloAcertoRepository(
            cicloAcertoDao,
            despesaDao,
            acertoRepositoryLegacy,
            clienteRepositoryLegacy,
            rotaDao,
            colaboradorDao // ✅ NOVO: Adicionar ColaboradorDao para finalizar metas
        )
    }
    
    private val veiculoRepository: VeiculoRepository by lazy {
        VeiculoRepository(
            veiculoDao,
            historicoManutencaoVeiculoDao,
            historicoCombustivelVeiculoDao
        )
    }
    
    private val metaRepository: MetaRepository by lazy {
        MetaRepository(metaDao)
    }
    
    private val panoRepository: PanoRepository by lazy {
        PanoRepository(panoEstoqueDao)
    }
    
    private val contratoRepository: ContratoRepository by lazy {
        ContratoRepository(
            contratoLocacaoDao,
            aditivoContratoDao
        )
    }
    
    private val colaboradorRepository: ColaboradorRepository by lazy {
        ColaboradorRepository(colaboradorDao)
    }
    
    companion object {
        fun create(database: com.example.gestaobilhares.data.database.AppDatabase): AppRepository {
            return AppRepository(
                database.clienteDao(),
                database.acertoDao(),
                database.mesaDao(),
                database.rotaDao(),
                database.despesaDao(),
                database.colaboradorDao(),
                database.cicloAcertoDao(),
                database.acertoMesaDao(),
                database.contratoLocacaoDao(),
                database.aditivoContratoDao(),
                database.assinaturaRepresentanteLegalDao(),
                database.logAuditoriaAssinaturaDao(),
                database.categoriaDespesaDao(),
                database.tipoDespesaDao(),
                database.panoEstoqueDao(),
                database.stockItemDao(),
                database.mesaReformadaDao(),
                database.mesaVendidaDao(),
                database.historicoManutencaoMesaDao(),
                database.veiculoDao(),
                database.historicoManutencaoVeiculoDao(),
                database.historicoCombustivelVeiculoDao(),
                database.panoMesaDao(),
                database.metaDao(),
                database.equipmentDao(),
                database.syncOperationDao()
            )
        }
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
    // ✅ DELEGAÇÃO: Usa ClienteRepository especializado
    
    fun obterTodosClientes(): Flow<List<Cliente>> = clienteRepository.obterTodos()
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> = clienteRepository.obterClientesPorRota(rotaId)
    suspend fun obterClientePorId(id: Long) = clienteRepository.obterPorId(id)
    suspend fun inserirCliente(cliente: Cliente): Long = clienteRepository.inserir(cliente)
    suspend fun atualizarCliente(cliente: Cliente): Int = clienteRepository.atualizar(cliente)
    suspend fun deletarCliente(cliente: Cliente) {
        // ✅ CORREÇÃO: Deletar do banco local
        clienteRepository.deletar(cliente)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "Cliente",
                entityId = cliente.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para Cliente: ${cliente.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de Cliente: ${e.message}", e)
        }
    }
    suspend fun obterDebitoAtual(clienteId: Long) = clienteRepository.obterDebitoAtual(clienteId)
    suspend fun atualizarDebitoAtual(clienteId: Long, novoDebito: Double) = clienteRepository.atualizarDebitoAtual(clienteId, novoDebito)
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long) = clienteRepository.calcularDebitoAtualEmTempoReal(clienteId)
    suspend fun obterClienteComDebitoAtual(clienteId: Long) = clienteRepository.obterClienteComDebitoAtual(clienteId)
    suspend fun buscarRotaIdPorCliente(clienteId: Long): Long? = clienteRepository.buscarRotaIdPorCliente(clienteId)
    fun obterClientesPorRotaComDebitoAtual(rotaId: Long): Flow<List<Cliente>> = clienteRepository.obterClientesPorRotaComDebitoAtual(rotaId)
    
    fun buscarClientesPorRotaComCache(rotaId: Long): Flow<List<Cliente>> {
        return _clientesCache.map { cache ->
            cache.filter { it.rotaId == rotaId }
        }
    }
    
    // ==================== ACERTO ====================
    // ✅ DELEGAÇÃO: Usa AcertoRepository especializado
    
    fun obterAcertosPorCliente(clienteId: Long) = acertoRepository.obterPorCliente(clienteId)
    fun obterAcertosRecentesPorCliente(clienteId: Long, limit: Int) = acertoRepository.obterRecentesPorCliente(clienteId, limit)
    suspend fun obterAcertoPorId(id: Long) = acertoRepository.obterPorId(id)
    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long) = acertoRepository.buscarUltimoPorCliente(clienteId)
    fun obterTodosAcertos() = acertoRepository.obterTodos()
    fun buscarAcertosPorCicloId(cicloId: Long) = acertoRepository.buscarPorCicloId(cicloId)
    fun buscarAcertosPorRotaECicloId(rotaId: Long, cicloId: Long) = acertoRepository.buscarPorRotaECicloId(rotaId, cicloId)
    fun buscarClientesPorRota(rotaId: Long) = clienteRepository.obterClientesPorRota(rotaId)
    suspend fun buscarRotaPorId(rotaId: Long) = rotaRepository.obterPorId(rotaId)
    suspend fun inserirAcerto(acerto: Acerto): Long = acertoRepository.inserir(acerto)
    suspend fun atualizarAcerto(acerto: Acerto): Int = acertoRepository.atualizar(acerto)
    suspend fun deletarAcerto(acerto: Acerto) {
        // ✅ CORREÇÃO: Deletar do banco local
        acertoRepository.deletar(acerto)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "Acerto",
                entityId = acerto.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para Acerto: ${acerto.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de Acerto: ${e.message}", e)
        }
    }
    suspend fun buscarUltimoAcertoPorMesa(mesaId: Long) = acertoRepository.buscarUltimoPorMesa(mesaId)
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long) = acertoRepository.buscarObservacaoUltimoAcerto(clienteId)
    suspend fun buscarUltimosAcertosPorClientes(clienteIds: List<Long>) = acertoRepository.buscarUltimosPorClientes(clienteIds)
    suspend fun removerAcertosExcedentes(clienteId: Long, limit: Int) = acertoRepository.removerAcertosExcedentes(clienteId, limit)
    suspend fun buscarAcertosPorPeriodo(clienteId: Long, inicio: Date, fim: Date) = acertoRepository.buscarPorPeriodo(clienteId, inicio, fim)
    
    // ==================== MESA ====================
    // ✅ DELEGAÇÃO: Usa MesaRepository especializado
    
    suspend fun obterMesaPorId(id: Long) = mesaRepository.obterPorId(id)
    fun obterMesasPorCliente(clienteId: Long) = mesaRepository.obterPorCliente(clienteId)
    fun obterMesasDisponiveis() = mesaRepository.obterDisponiveis()
    suspend fun inserirMesa(mesa: Mesa): Long = mesaRepository.inserir(mesa)
    suspend fun atualizarMesa(mesa: Mesa) = mesaRepository.atualizar(mesa)
    suspend fun deletarMesa(mesa: Mesa) {
        // ✅ CORREÇÃO: Deletar do banco local
        mesaRepository.deletar(mesa)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "Mesa",
                entityId = mesa.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para Mesa: ${mesa.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de Mesa: ${e.message}", e)
        }
    }
    suspend fun vincularMesaACliente(mesaId: Long, clienteId: Long) = mesaRepository.vincularACliente(mesaId, clienteId)
    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) = mesaRepository.vincularComValorFixo(mesaId, clienteId, valorFixo)
    suspend fun desvincularMesaDeCliente(mesaId: Long) = mesaRepository.desvincularDeCliente(mesaId)
    suspend fun retirarMesa(mesaId: Long) = mesaRepository.retirar(mesaId)
    suspend fun atualizarRelogioMesa(mesaId: Long, relogioInicial: Int, relogioFinal: Int, fichasInicial: Int, fichasFinal: Int) = 
        mesaRepository.atualizarRelogio(mesaId, relogioInicial, relogioFinal, fichasInicial, fichasFinal)
    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = mesaRepository.atualizarRelogioFinal(mesaId, relogioFinal)
    suspend fun obterMesasPorClienteDireto(clienteId: Long) = mesaRepository.obterPorClienteDireto(clienteId)
    fun buscarMesasPorRota(rotaId: Long) = mesaRepository.buscarPorRota(rotaId).also {
        android.util.Log.d("AppRepository", "Buscando mesas para rota $rotaId")
    }
    suspend fun contarMesasAtivasPorClientes(clienteIds: List<Long>) = mesaRepository.contarAtivasPorClientes(clienteIds)
    fun obterTodasMesas() = mesaRepository.obterTodas()
    
    // ==================== ROTA ====================
    // ✅ DELEGAÇÃO: Usa RotaRepository especializado
    
    fun obterTodasRotas() = rotaRepository.obterTodas()
    fun obterRotasAtivas() = rotaRepository.obterAtivas()
    
    // ✅ NOVO: Método para obter resumo de rotas com atualização em tempo real
    fun getRotasResumoComAtualizacaoTempoReal(): Flow<List<RotaResumo>> {
        return rotaRepository.getRotasResumoComAtualizacaoTempoReal(
            calcularClientesAtivos = { calcularClientesAtivosPorRota(it) },
            obterCicloAtualRota = { obterCicloAtualRota(it) },
            calcularPendenciasReais = { calcularPendenciasReaisPorRota(it) },
            calcularQuantidadeMesas = { calcularQuantidadeMesasPorRota(it) },
            calcularPercentualAcertados = { rotaId, cicloId, clientesAtivos -> 
                calcularPercentualClientesAcertados(rotaId, cicloId, clientesAtivos) 
            },
            calcularValorAcertado = { rotaId, cicloId -> calcularValorAcertadoPorRotaECiclo(rotaId, cicloId) },
            determinarStatus = { determinarStatusRotaEmTempoReal(it) },
            obterDatasCiclo = { obterDatasCicloRota(it) }
        )
    }
    
    // ✅ NOVO: Métodos auxiliares para calcular dados reais das rotas
    private fun calcularClientesAtivosPorRota(rotaId: Long): Int {
        return try {
            // Usar runBlocking para operações síncronas dentro do Flow
            kotlinx.coroutines.runBlocking {
                clienteDao.obterClientesPorRota(rotaId).first().count { it.ativo }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular clientes ativos da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularPendenciasReaisPorRota(rotaId: Long): Int {
        return try {
            kotlinx.coroutines.runBlocking {
                val clientes = clienteDao.obterClientesPorRota(rotaId).first()
                if (clientes.isEmpty()) return@runBlocking 0
                val clienteIds = clientes.map { it.id }
                val ultimos = buscarUltimosAcertosPorClientes(clienteIds)
                val ultimoPorCliente = ultimos.associateBy({ it.clienteId }, { it.dataAcerto })
                val agora = java.util.Calendar.getInstance()
                val pendencias = clientes.count { cliente ->
                    // ✅ CORRIGIDO: Usar >= 300 para incluir débitos de 300 reais ou mais
                    val debitoAlto = cliente.debitoAtual >= 300.0
                    val dataUltimo = ultimoPorCliente[cliente.id]
                    val semAcerto4Meses = if (dataUltimo == null) {
                        true
                    } else {
                        val cal = java.util.Calendar.getInstance(); cal.time = dataUltimo
                        val anos = agora.get(java.util.Calendar.YEAR) - cal.get(java.util.Calendar.YEAR)
                        val meses = anos * 12 + (agora.get(java.util.Calendar.MONTH) - cal.get(java.util.Calendar.MONTH))
                        meses >= 4
                    }
                    val temPendencia = debitoAlto || semAcerto4Meses
                    if (temPendencia) {
                        android.util.Log.d("AppRepository", "📋 Cliente ${cliente.nome} (ID: ${cliente.id}) tem pendência: débito=${cliente.debitoAtual}, semAcerto4Meses=$semAcerto4Meses")
                    }
                    temPendencia
                }
                android.util.Log.d("AppRepository", "📊 Rota $rotaId: $pendencias pendências de ${clientes.size} clientes")
                pendencias
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular pendências reais da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularValorAcertadoPorRotaECiclo(rotaId: Long, cicloId: Long?): Double {
        return try {
            if (cicloId == null) return 0.0
            kotlinx.coroutines.runBlocking {
                buscarAcertosPorCicloId(cicloId).first().filter { it.rotaId == rotaId }.sumOf { it.valorRecebido }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular valor acertado da rota $rotaId: ${e.message}")
            0.0
        }
    }
    
    private fun calcularQuantidadeMesasPorRota(rotaId: Long): Int {
        return try {
            kotlinx.coroutines.runBlocking {
                mesaDao.buscarMesasPorRota(rotaId).first().size
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular quantidade de mesas da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularPercentualClientesAcertados(rotaId: Long, cicloId: Long?, clientesAtivos: Int): Int {
        return try {
            if (cicloId == null || clientesAtivos == 0) return 0
            kotlinx.coroutines.runBlocking {
                val acertos = buscarAcertosPorCicloId(cicloId).first().filter { it.rotaId == rotaId }
                val distintos = acertos.map { it.clienteId }.distinct().size
                ((distintos.toDouble() / clientesAtivos.toDouble()) * 100).toInt()
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular percentual de clientes acertados da rota $rotaId: ${e.message}")
            0
        }
    }

    private fun obterCicloAtualRota(rotaId: Long): Triple<Int, Long?, Long?> {
        return try {
            kotlinx.coroutines.runBlocking {
                val emAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
                val ultimoCiclo = cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)
                
                // Lógica solicitada: Pegar o maior número de ciclo
                // Se houver um último ciclo com número maior que o em andamento, usa o último
                if (ultimoCiclo != null && (emAndamento == null || ultimoCiclo.numeroCiclo > emAndamento.numeroCiclo)) {
                    android.util.Log.d("AppRepository", "🔄 Rota $rotaId: Exibindo maior ciclo encontrado: ${ultimoCiclo.numeroCiclo} (Status: ${ultimoCiclo.status})")
                    // Se estiver finalizado, usa dataFim, senão dataInicio (fallback)
                    val dataRef = if (ultimoCiclo.status == StatusCicloAcerto.FINALIZADO) ultimoCiclo.dataFim else ultimoCiclo.dataInicio
                    Triple(ultimoCiclo.numeroCiclo, ultimoCiclo.id, dataRef.time)
                } else if (emAndamento != null) {
                    // Se o em andamento for maior ou igual (ou único), usa ele
                    Triple(emAndamento.numeroCiclo, emAndamento.id, emAndamento.dataInicio.time)
                } else {
                    android.util.Log.d("AppRepository", "🆕 Rota $rotaId: Sem histórico, exibindo 1º ciclo")
                    Triple(1, null, null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter ciclo atual da rota $rotaId: ${e.message}")
            Triple(1, null, null)
        }
    }

    // ✅ NOVO: Método para obter datas de início e fim do ciclo
    private fun obterDatasCicloRota(rotaId: Long): Pair<Long?, Long?> {
        return try {
            kotlinx.coroutines.runBlocking {
                val emAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
                if (emAndamento != null) {
                    Pair(emAndamento.dataInicio.time, emAndamento.dataFim.time)
                } else {
                    val ultimo = cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)
                    if (ultimo != null) {
                        Pair(ultimo.dataInicio.time, ultimo.dataFim.time)
                    } else {
                        Pair(null, null)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter datas do ciclo da rota $rotaId: ${e.message}")
            Pair(null, null)
        }
    }

    private fun determinarStatusRotaEmTempoReal(rotaId: Long): StatusRota {
        return try {
            kotlinx.coroutines.runBlocking {
                val emAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
                val status = if (emAndamento != null) {
                    android.util.Log.d("AppRepository", "✅ Rota $rotaId: Ciclo em andamento encontrado (ID: ${emAndamento.id}) -> EM_ANDAMENTO")
                    StatusRota.EM_ANDAMENTO
                } else {
                    android.util.Log.d("AppRepository", "✅ Rota $rotaId: Nenhum ciclo em andamento -> FINALIZADA")
                    StatusRota.FINALIZADA
                }
                status
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
            // ✅ UPSERT: Delegado para RotaRepository (lógica centralizada)
            val id = rotaRepository.inserir(rota)
            logDbInsertSuccess("ROTA", "Nome=${rota.nome}, ID=$id")
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
    suspend fun atualizarRota(rota: Rota) = rotaDao.updateRota(rota)
    suspend fun atualizarRotas(rotas: List<Rota>) = rotaDao.updateRotas(rotas)
    suspend fun deletarRota(rota: Rota) {
        // ✅ CORREÇÃO: Deletar do banco local
        rotaDao.deleteRota(rota)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "Rota",
                entityId = rota.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para Rota: ${rota.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de Rota: ${e.message}", e)
        }
    }
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
    // ✅ DELEGAÇÃO: Usa DespesaRepository especializado
    
    // ✅ CORRIGIDO: Método para sincronização (retorna Despesa diretamente)
    fun obterTodasDespesas() = despesaRepository.obterTodasDespesas()
    
    // Método para exibição (retorna DespesaResumo com JOIN)
    fun obterTodasDespesasComRota() = despesaRepository.obterTodas()
    fun obterDespesasPorRota(rotaId: Long) = despesaRepository.obterPorRota(rotaId)
    suspend fun obterDespesaPorId(id: Long) = despesaRepository.obterPorId(id)
    suspend fun inserirDespesa(despesa: Despesa): Long = despesaRepository.inserir(despesa)
    suspend fun atualizarDespesa(despesa: Despesa) = despesaRepository.atualizar(despesa)
    suspend fun deletarDespesa(despesa: Despesa) {
        // ✅ CORREÇÃO: Deletar do banco local
        despesaRepository.deletar(despesa)
        android.util.Log.d("AppRepository", "🗑️ Despesa deletada localmente: ID=${despesa.id}")
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        // O ID local é usado como documentId no Firestore
        if (syncOperationDao == null) {
            android.util.Log.e("AppRepository", "❌ CRÍTICO: SyncOperationDao é null! Operação DELETE não será enfileirada para Despesa: ${despesa.id}")
            return
        }
        
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "Despesa",
                entityId = despesa.id.toString(),
                entityData = "{}", // Para DELETE, não precisamos dos dados
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            val operationId = inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para Despesa: ID=${despesa.id}, OperationID=$operationId")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ ERRO CRÍTICO ao enfileirar DELETE de Despesa ${despesa.id}: ${e.message}", e)
            android.util.Log.e("AppRepository", "   Stack trace: ${e.stackTraceToString()}")
            // Não lança exceção para não impedir a exclusão local
        }
    }
    suspend fun calcularTotalPorRota(rotaId: Long) = despesaRepository.calcularTotalPorRota(rotaId)
    suspend fun calcularTotalGeral() = despesaRepository.calcularTotalGeral()
    suspend fun contarDespesasPorRota(rotaId: Long) = despesaRepository.contarPorRota(rotaId)
    suspend fun deletarDespesasPorRota(rotaId: Long) = despesaRepository.deletarPorRota(rotaId)
    fun buscarDespesasPorCicloId(cicloId: Long) = despesaRepository.buscarPorCicloId(cicloId)
    fun buscarDespesasPorRotaECicloId(rotaId: Long, cicloId: Long) = despesaRepository.buscarPorRotaECicloId(rotaId, cicloId)
    suspend fun buscarDespesasGlobaisPorCiclo(ano: Int, numero: Int): List<Despesa> = despesaRepository.buscarGlobaisPorCiclo(ano, numero)
    suspend fun somarDespesasGlobaisPorCiclo(ano: Int, numero: Int): Double = despesaRepository.somarGlobaisPorCiclo(ano, numero)

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
    
    suspend fun inserirColaborador(colaborador: Colaborador): Long {
        logDbInsertStart("COLABORADOR", "Nome=${colaborador.nome}, Email=${colaborador.email}, Nivel=${colaborador.nivelAcesso}")
        return try {
            val id = colaboradorDao.inserir(colaborador)
            logDbInsertSuccess("COLABORADOR", "Email=${colaborador.email}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("COLABORADOR", "Email=${colaborador.email}", e)
            throw e
        }
    }
    suspend fun atualizarColaborador(colaborador: Colaborador) = colaboradorDao.atualizar(colaborador)
    suspend fun deletarColaborador(colaborador: Colaborador) {
        // ✅ CORREÇÃO: Deletar do banco local
        colaboradorDao.deletar(colaborador)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "Colaborador",
                entityId = colaborador.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para Colaborador: ${colaborador.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de Colaborador: ${e.message}", e)
        }
    }
    
    suspend fun aprovarColaborador(colaboradorId: Long, dataAprovacao: java.util.Date, aprovadoPor: String) = 
        colaboradorDao.aprovarColaborador(colaboradorId, dataAprovacao, aprovadoPor)
    
    suspend fun aprovarColaboradorComCredenciais(
        colaboradorId: Long,
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        dataAprovacao: java.util.Date,
        aprovadoPor: String,
        firebaseUid: String? = null
    ) = colaboradorDao.aprovarColaboradorComCredenciais(
        colaboradorId, email, senha, nivelAcesso, observacoes, dataAprovacao, aprovadoPor, firebaseUid
    )
    
    suspend fun marcarPrimeiroAcessoConcluido(colaboradorId: Long, senhaHash: String) = 
        colaboradorDao.marcarPrimeiroAcessoConcluido(colaboradorId, senhaHash)
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
            id
        } catch (e: Exception) {
            logDbInsertError("META", "ColaboradorID=${meta.colaboradorId}", e)
            throw e
        }
    }
    suspend fun atualizarMeta(meta: MetaColaborador) = colaboradorDao.atualizarMeta(meta)
    suspend fun deletarMeta(meta: MetaColaborador) {
        // ✅ CORREÇÃO: Deletar do banco local
        colaboradorDao.deletarMeta(meta)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "MetaColaborador",
                entityId = meta.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para MetaColaborador: ${meta.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de MetaColaborador: ${e.message}", e)
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
    suspend fun buscarMetasPorRotaECiclo(rotaId: Long, cicloId: Long): List<MetaColaborador> {
        // Verificar se o ciclo está finalizado
        val ciclo = buscarCicloPorId(cicloId)
        val cicloFinalizado = ciclo?.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO
        
        // Se o ciclo está finalizado, buscar todas as metas (ativas e finalizadas) - para histórico
        if (cicloFinalizado) {
            val metasAtivas = colaboradorDao.buscarMetasPorRotaECiclo(rotaId, cicloId)
            val metasFinalizadas = colaboradorDao.buscarMetasPorRotaECicloFinalizadas(rotaId, cicloId)
            android.util.Log.d("AppRepository", "Ciclo finalizado: buscando ${metasAtivas.size} metas ativas e ${metasFinalizadas.size} metas finalizadas")
            return (metasAtivas + metasFinalizadas).distinctBy { it.id }
        }
        
        // Para ciclos em andamento, buscar APENAS metas ativas
        // Tentar busca principal primeiro (apenas ativas)
        val metas = colaboradorDao.buscarMetasPorRotaECiclo(rotaId, cicloId)
        if (metas.isNotEmpty()) {
            return metas
        }
        // Fallback: busca direta por rotaId (para casos onde colaboradorId = 0)
        return colaboradorDao.buscarMetasPorRotaECicloDireto(rotaId, cicloId)
    }
    
    /**
     * Busca metas APENAS de ciclos em andamento (para tela principal)
     * Não retorna metas de ciclos finalizados
     */
    suspend fun buscarMetasPorRotaECicloAtivo(rotaId: Long, cicloId: Long): List<MetaColaborador> {
        // Verificar se o ciclo está em andamento
        val ciclo = buscarCicloPorId(cicloId)
        val cicloEmAndamento = ciclo?.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO
        
        // Se o ciclo não está em andamento, retornar lista vazia
        if (!cicloEmAndamento) {
            android.util.Log.d("AppRepository", "Ciclo $cicloId não está em andamento (status=${ciclo?.status}), retornando lista vazia")
            return emptyList()
        }
        
        // Buscar apenas metas ativas
        val metas = colaboradorDao.buscarMetasPorRotaECiclo(rotaId, cicloId)
        if (metas.isNotEmpty()) {
            return metas
        }
        // Fallback: busca direta por rotaId
        return colaboradorDao.buscarMetasPorRotaECicloDireto(rotaId, cicloId)
    }

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
            colaboradorDao.buscarColaboradorResponsavelPrincipal(rotaId)
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
    suspend fun deletarColaboradorRota(colaboradorRota: ColaboradorRota) = colaboradorDao.deletarColaboradorRota(colaboradorRota)
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
    // ✅ DELEGAÇÃO: Usa CicloRepository especializado
    
    fun obterTodosCiclos() = cicloRepository.obterTodos()
    suspend fun buscarUltimoCicloFinalizadoPorRota(rotaId: Long) = cicloRepository.buscarUltimoFinalizadoPorRota(rotaId)
    suspend fun buscarUltimoCicloPorRota(rotaId: Long) = cicloRepository.buscarUltimoCicloPorRota(rotaId)
    suspend fun buscarCiclosPorRotaEAno(rotaId: Long, ano: Int) = cicloRepository.buscarPorRotaEAno(rotaId, ano)
    suspend fun buscarCiclosPorRota(rotaId: Long) = cicloRepository.buscarPorRota(rotaId)
    suspend fun buscarProximoNumeroCiclo(rotaId: Long, ano: Int) = cicloRepository.buscarProximoNumero(rotaId, ano)
    suspend fun inserirCicloAcerto(ciclo: CicloAcertoEntity): Long = cicloRepository.inserir(ciclo)
    suspend fun buscarCiclosParaMetas(rotaId: Long): List<CicloAcertoEntity> = cicloRepository.buscarParaMetas(rotaId)
    
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
            val calendar = java.util.Calendar.getInstance()
            val ciclos = cicloAcertoDao.listarTodos().first()
                .filter { ciclo ->
                    calendar.time = ciclo.dataInicio
                    ciclo.numeroCiclo == numeroCiclo && calendar.get(java.util.Calendar.YEAR) == ano
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
    
    fun buscarContratosPorCliente(clienteId: Long) = contratoLocacaoDao.buscarContratosPorCliente(clienteId)
    suspend fun buscarContratoPorNumero(numeroContrato: String) = contratoLocacaoDao.buscarContratoPorNumero(numeroContrato)
    suspend fun buscarContratoAtivoPorCliente(clienteId: Long) = contratoLocacaoDao.buscarContratoAtivoPorCliente(clienteId)
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
                val resumo = apos.joinToString { _ -> "id=${'$'}{it.id},status=${'$'}{it.status},enc=${'$'}{it.dataEncerramento}" }
                Log.d("RepoContracts", "Após atualizar: cliente=${contrato.clienteId} contratos=${apos.size} -> $resumo")
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
        val resumo = apos.joinToString { _ -> "id=${'$'}{it.id},status=${'$'}{it.status},enc=${'$'}{it.dataEncerramento}" }
        Log.d("RepoContracts", "Após encerrar direto: cliente=${clienteId} contratos=${apos.size} -> $resumo")
    }
    suspend fun excluirContrato(contrato: ContratoLocacao) = contratoLocacaoDao.excluirContrato(contrato)
    suspend fun buscarContratoPorId(contratoId: Long) = contratoLocacaoDao.buscarContratoPorId(contratoId)
    suspend fun buscarMesasPorContrato(contratoId: Long) = contratoLocacaoDao.buscarMesasPorContrato(contratoId)
    suspend fun inserirContratoMesa(contratoMesa: ContratoMesa): Long {
        logDbInsertStart("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}, MesaID=${contratoMesa.mesaId}")
        return try {
            val id = contratoLocacaoDao.inserirContratoMesa(contratoMesa)
            logDbInsertSuccess("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}, ID=$id")
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
            ids
        } catch (e: Exception) {
            logDbInsertError("CONTRATO_MESAS", "Quantidade=${contratoMesas.size}", e)
            throw e
        }
    }
    suspend fun excluirContratoMesa(contratoMesa: ContratoMesa) = contratoLocacaoDao.excluirContratoMesa(contratoMesa)
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

    suspend fun atualizarAditivo(aditivo: AditivoContrato) = aditivoContratoDao.atualizarAditivo(aditivo)
    suspend fun excluirAditivo(aditivo: AditivoContrato) = aditivoContratoDao.excluirAditivo(aditivo)
    suspend fun buscarMesasPorAditivo(aditivoId: Long) = aditivoContratoDao.buscarMesasPorAditivo(aditivoId)
    suspend fun excluirAditivoMesa(aditivoMesa: AditivoMesa) = aditivoContratoDao.excluirAditivoMesa(aditivoMesa)
    suspend fun excluirTodasMesasDoAditivo(aditivoId: Long) = aditivoContratoDao.excluirTodasMesasDoAditivo(aditivoId)
    
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
            val fim = ciclo.dataFim // dataFim é não-nullable em CicloAcertoEntity
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
    
    // ==================== CATEGORIAS E TIPOS DE DESPESA ====================
    
    fun buscarCategoriasAtivas() = categoriaDespesaDao?.buscarAtivas() ?: flowOf(emptyList())
    suspend fun buscarCategoriaPorNome(nome: String) = categoriaDespesaDao?.buscarPorNome(nome)
    suspend fun buscarCategoriaPorId(id: Long) = categoriaDespesaDao?.buscarPorId(id)
    suspend fun criarCategoria(categoria: CategoriaDespesa): Long = categoriaDespesaDao?.inserir(categoria) ?: 0L
    suspend fun criarCategoria(dados: NovaCategoriaDespesa): Long {
        val categoria = CategoriaDespesa(
            nome = dados.nome.trim(),
            descricao = dados.descricao.trim(),
            criadoPor = dados.criadoPor
        )
        return categoriaDespesaDao?.inserir(categoria) ?: 0L
    }
    suspend fun atualizarCategoria(categoria: CategoriaDespesa) {
        categoriaDespesaDao?.atualizar(categoria)
    }
    suspend fun editarCategoria(dados: EdicaoCategoriaDespesa) {
        val categoriaExistente = buscarCategoriaPorId(dados.id)
        categoriaExistente?.let {
            val categoriaAtualizada = it.copy(
                nome = dados.nome.trim(),
                descricao = dados.descricao.trim(),
                ativa = dados.ativa,
                dataAtualizacao = java.util.Date()
            )
            atualizarCategoria(categoriaAtualizada)
        }
    }
    suspend fun deletarCategoria(categoria: CategoriaDespesa) {
        // ✅ CORREÇÃO: Deletar do banco local
        categoriaDespesaDao?.deletar(categoria)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "CategoriaDespesa",
                entityId = categoria.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para CategoriaDespesa: ${categoria.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de CategoriaDespesa: ${e.message}", e)
        }
    }
    suspend fun categoriaExiste(nome: String): Boolean = categoriaDespesaDao?.contarPorNome(nome) ?: 0 > 0
    
    suspend fun buscarTipoPorNome(nome: String) = tipoDespesaDao?.buscarPorNome(nome)
    suspend fun buscarTipoPorId(id: Long) = tipoDespesaDao?.buscarPorId(id)
    fun buscarTiposPorCategoria(categoriaId: Long) = tipoDespesaDao?.buscarPorCategoria(categoriaId) ?: flowOf(emptyList())
    fun buscarTiposAtivosComCategoria() = tipoDespesaDao?.buscarAtivosComCategoria() ?: flowOf(emptyList())
    suspend fun tipoExiste(nome: String, categoriaId: Long): Boolean = tipoDespesaDao?.contarPorNomeECategoria(nome, categoriaId) ?: 0 > 0
    suspend fun criarTipo(tipo: TipoDespesa): Long = tipoDespesaDao?.inserir(tipo) ?: 0L
    suspend fun criarTipo(dados: NovoTipoDespesa): Long {
        val tipo = TipoDespesa(
            categoriaId = dados.categoriaId,
            nome = dados.nome.trim(),
            descricao = dados.descricao.trim(),
            criadoPor = dados.criadoPor
        )
        return tipoDespesaDao?.inserir(tipo) ?: 0L
    }
    suspend fun atualizarTipo(tipo: TipoDespesa) {
        tipoDespesaDao?.atualizar(tipo)
    }
    suspend fun editarTipo(dados: EdicaoTipoDespesa) {
        val tipoExistente = buscarTipoPorId(dados.id)
        tipoExistente?.let {
            val tipoAtualizado = it.copy(
                categoriaId = dados.categoriaId,
                nome = dados.nome.trim(),
                descricao = dados.descricao.trim(),
                ativo = dados.ativo,
                dataAtualizacao = java.util.Date()
            )
            atualizarTipo(tipoAtualizado)
        }
    }
    suspend fun deletarTipo(tipo: TipoDespesa) {
        // ✅ CORREÇÃO: Deletar do banco local
        tipoDespesaDao?.deletar(tipo)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "TipoDespesa",
                entityId = tipo.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para TipoDespesa: ${tipo.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de TipoDespesa: ${e.message}", e)
        }
    }
    
    // ==================== ACERTO MESA ====================
    
    suspend fun buscarAcertoMesasPorAcerto(acertoId: Long) = acertoMesaDao.buscarPorAcertoId(acertoId)
    fun buscarAcertoMesasPorAcertoFlow(acertoId: Long) = acertoMesaDao.buscarPorAcerto(acertoId)
    suspend fun buscarUltimoAcertoMesaItem(mesaId: Long) = acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    suspend fun buscarAcertoMesaPorAcertoEMesa(acertoId: Long, mesaId: Long) = acertoMesaDao.buscarAcertoMesaPorAcertoEMesa(acertoId, mesaId)
    suspend fun inserirAcertoMesa(acertoMesa: com.example.gestaobilhares.data.entities.AcertoMesa): Long = acertoMesaDao.inserir(acertoMesa)
    
    // ==================== CICLO ====================
    // ✅ DELEGAÇÃO: Usa repositories especializados
    
    suspend fun buscarCicloAtivo(rotaId: Long) = cicloRepository.buscarAtivo(rotaId)
    suspend fun buscarCicloPorId(cicloId: Long) = cicloRepository.buscarPorId(cicloId)
    suspend fun obterCicloAtualIdPorRota(rotaId: Long): Long? = cicloRepository.buscarAtivo(rotaId)?.id
    fun buscarAcertosPorClienteECicloId(clienteId: Long, cicloId: Long) = acertoRepository.buscarPorClienteECicloId(clienteId, cicloId)
    suspend fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = acertoRepository.buscarPorRotaECicloId(rotaId, cicloId).first()
    suspend fun buscarPorId(acertoId: Long) = acertoRepository.obterPorId(acertoId)
    @Suppress("UNUSED_PARAMETER")
    suspend fun atualizarValoresCiclo(_cicloId: Long) {
        // TODO: Implementar lógica de atualização de valores do ciclo
    }
    
    /**
     * ✅ NOVO: Finaliza o ciclo atual de uma rota com dados consolidados
     * ✅ CORREÇÃO: Usa o método finalizarCiclo do CicloAcertoRepository que salva o debitoTotal
     */
    suspend fun finalizarCicloAtualComDados(rotaId: Long) {
        try {
            val cicloAtual = buscarCicloAtivo(rotaId)
            if (cicloAtual != null) {
                Log.d("AppRepository", "🔄 Iniciando finalização do ciclo ${cicloAtual.id} da rota $rotaId")
                
                // ✅ CORREÇÃO: Finalizar o ciclo na rota primeiro
                val dataFim = System.currentTimeMillis()
                finalizarCicloRota(rotaId, dataFim)
                
                // ✅ CORREÇÃO: Usar o método finalizarCiclo do CicloAcertoRepository que calcula e salva todos os valores,
                // incluindo o debitoTotal "congelado" no ciclo finalizado E finaliza as metas automaticamente
                Log.d("AppRepository", "📋 Chamando finalizarCiclo do CicloAcertoRepository para ciclo ${cicloAtual.id}")
                cicloAcertoRepository.finalizarCiclo(cicloAtual.id, java.util.Date(dataFim))
                
                Log.d("AppRepository", "✅ Ciclo ${cicloAtual.id} finalizado com debitoTotal preservado e metas finalizadas")
            } else {
                Log.w("AppRepository", "⚠️ Nenhum ciclo ativo encontrado para rota $rotaId")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Erro ao finalizar ciclo atual: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    // ✅ NOVO: Método auxiliar para buscar despesas por ciclo (retorna List diretamente)
    suspend fun buscarDespesasPorCiclo(cicloId: Long): List<Despesa> {
        return buscarDespesasPorCicloId(cicloId).first()
    }
    
    // ==================== PANO ESTOQUE ====================
    // ✅ DELEGAÇÃO: Usa PanoRepository especializado
    
    fun obterPanosDisponiveis() = panoRepository.obterDisponiveis()
    fun obterTodosPanosEstoque() = panoRepository.obterTodos()
    suspend fun buscarPorNumero(numero: String) = panoRepository.buscarPorNumero(numero)
    suspend fun obterPanoPorId(id: Long) = panoRepository.obterPorId(id)
    suspend fun inserirPanoEstoque(pano: com.example.gestaobilhares.data.entities.PanoEstoque): Long = panoRepository.inserir(pano)
    suspend fun marcarPanoComoUsado(id: Long) = panoRepository.marcarComoUsado(id)
    suspend fun marcarPanoComoUsadoPorNumero(numero: String) = panoRepository.marcarComoUsadoPorNumero(numero)
    
    // ==================== MESA REFORMADA ====================
    // ✅ DELEGAÇÃO: Usa MesaRepository especializado
    
    fun obterTodasMesasReformadas() = mesaRepository.obterTodasMesasReformadas()
    suspend fun inserirMesaReformada(mesaReformada: com.example.gestaobilhares.data.entities.MesaReformada): Long = mesaRepository.inserirMesaReformada(mesaReformada)
    
    // ==================== MESA VENDIDA ====================
    
    fun obterTodasMesasVendidas() = mesaVendidaDao?.listarTodas() ?: flowOf(emptyList())
    suspend fun inserirMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida): Long {
        return mesaVendidaDao?.inserir(mesaVendida) ?: 0L
    }
    suspend fun buscarMesaVendidaPorId(id: Long) = mesaVendidaDao?.buscarPorId(id)
    
    // ==================== HISTÓRICO MANUTENÇÃO MESA ====================
    
    fun obterTodosHistoricoManutencaoMesa() = historicoManutencaoMesaDao?.listarTodos() ?: flowOf(emptyList())
    suspend fun inserirHistoricoManutencaoMesa(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa): Long = historicoManutencaoMesaDao?.inserir(historico) ?: 0L
    suspend fun inserirHistoricoManutencaoMesaSync(historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa): Long = inserirHistoricoManutencaoMesa(historico)
    
    // ==================== VEÍCULOS ====================
    
    fun obterTodosVeiculos() = veiculoDao?.listar() ?: flowOf(emptyList())
    suspend fun inserirVeiculo(veiculo: Veiculo): Long = veiculoDao?.inserir(veiculo) ?: 0L
    suspend fun atualizarVeiculo(veiculo: Veiculo) = veiculoDao?.atualizar(veiculo)
    suspend fun deletarVeiculo(veiculo: Veiculo) {
        // ✅ CORREÇÃO: Deletar do banco local
        veiculoDao?.deletar(veiculo)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "Veiculo",
                entityId = veiculo.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para Veiculo: ${veiculo.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de Veiculo: ${e.message}", e)
        }
    }
    @Suppress("UNUSED_PARAMETER")
    suspend fun obterVeiculoPorId(id: Long) = veiculoDao?.let { 
        // VeiculoDao não tem método obterPorId, precisamos criar ou usar listar().first()
        null // TODO: Adicionar método obterPorId no VeiculoDao se necessário
    }
    
    // ==================== METAS ====================
    // ✅ DELEGAÇÃO: Usa MetaRepository especializado
    
    fun obterTodasMetas() = metaRepository.obterTodas()
    suspend fun inserirMeta(meta: Meta): Long = metaRepository.inserir(meta)
    suspend fun atualizarMeta(meta: Meta) = metaRepository.atualizar(meta)
    suspend fun obterMetaPorId(id: Long) = metaRepository.obterPorId(id)
    
    // ==================== HISTÓRICO COMBUSTÍVEL E MANUTENÇÃO VEÍCULO ====================
    // ✅ DELEGAÇÃO: Usa VeiculoRepository especializado
    
    suspend fun obterTodosHistoricoManutencaoVeiculo(): List<com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo> {
        return veiculoRepository.obterTodosHistoricoManutencaoVeiculo()
    }
    
    suspend fun obterTodosHistoricoCombustivelVeiculo(): List<com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo> {
        return veiculoRepository.obterTodosHistoricoCombustivelVeiculo()
    }
    
    suspend fun inserirHistoricoCombustivel(_historico: com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo): Long {
        return veiculoRepository.inserirHistoricoCombustivel(_historico)
    }
    
    suspend fun inserirHistoricoManutencao(_historico: com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo): Long {
        return veiculoRepository.inserirHistoricoManutencao(_historico)
    }
    
    // ✅ NOVO: Métodos Flow reativos para observação automática em ViewModels
    // Baseado no código antigo que funcionava - retorna todos e filtra no ViewModel
    fun obterTodosHistoricoManutencaoVeiculoFlow() = veiculoRepository.obterTodosHistoricoManutencaoVeiculoFlow()
    fun obterTodosHistoricoCombustivelVeiculoFlow() = veiculoRepository.obterTodosHistoricoCombustivelVeiculoFlow()
    
    fun obterHistoricoManutencaoPorVeiculo(veiculoId: Long) = veiculoRepository.obterHistoricoManutencaoPorVeiculo(veiculoId)
    fun obterHistoricoCombustivelPorVeiculo(veiculoId: Long) = veiculoRepository.obterHistoricoCombustivelPorVeiculo(veiculoId)
    fun obterHistoricoManutencaoPorVeiculoEAno(veiculoId: Long, ano: String) = veiculoRepository.obterHistoricoManutencaoPorVeiculoEAno(veiculoId, ano)
    fun obterHistoricoCombustivelPorVeiculoEAno(veiculoId: Long, ano: String) = veiculoRepository.obterHistoricoCombustivelPorVeiculoEAno(veiculoId, ano)
    
    // ==================== PANO MESA ====================
    // ✅ DELEGAÇÃO: Usa MesaRepository especializado
    
    suspend fun obterTodosPanoMesa(): List<com.example.gestaobilhares.data.entities.PanoMesa> {
        return mesaRepository.obterTodosPanoMesa()
    }
    
    suspend fun inserirPanoMesa(panoMesa: com.example.gestaobilhares.data.entities.PanoMesa): Long {
        return mesaRepository.inserirPanoMesa(panoMesa)
    }
    
    // ==================== COLABORADOR ROTA ====================
    // ✅ DELEGAÇÃO: Usa ColaboradorRepository especializado
    
    suspend fun obterTodosColaboradorRotas(): List<ColaboradorRota> {
        return colaboradorRepository.obterTodosColaboradorRotas()
    }
    
    // ==================== ADITIVO MESA ====================
    // ✅ DELEGAÇÃO: Usa ContratoRepository especializado
    
    suspend fun obterTodosAditivoMesas(): List<AditivoMesa> {
        return contratoRepository.obterTodosAditivoMesas()
    }
    
    // ==================== CONTRATO MESA ====================
    // ✅ DELEGAÇÃO: Usa ContratoRepository especializado
    
    suspend fun obterTodosContratoMesas(): List<ContratoMesa> {
        return contratoRepository.obterTodosContratoMesas()
    }
    
    // ==================== STOCK ITEM ====================
    
    // ==================== FILA DE SINCRONIZAÇÃO ====================
    
    /**
     * Insere uma operação na fila de sincronização
     */
    suspend fun inserirOperacaoSync(operation: SyncOperationEntity): Long {
        if (syncOperationDao == null) {
            android.util.Log.e("AppRepository", "❌ CRÍTICO: SyncOperationDao é null ao tentar inserir operação!")
            android.util.Log.e("AppRepository", "   Tipo: ${operation.operationType}, Entidade: ${operation.entityType}, ID: ${operation.entityId}")
            throw IllegalStateException("SyncOperationDao não inicializado")
        }
        val operationId = syncOperationDao.inserir(operation)
        android.util.Log.d("AppRepository", "✅ Operação inserida na fila: ID=$operationId, Tipo=${operation.operationType}, Entidade=${operation.entityType}")
        return operationId
    }
    
    /**
     * Obtém todas as operações pendentes como Flow
     */
    fun obterOperacoesSyncPendentes(): Flow<List<SyncOperationEntity>> {
        return syncOperationDao?.obterOperacoesPendentes() ?: flowOf(emptyList())
    }
    
    /**
     * Obtém operações pendentes limitadas (para processamento em lotes)
     */
    suspend fun obterOperacoesSyncPendentesLimitadas(limit: Int): List<SyncOperationEntity> {
        return syncOperationDao?.obterOperacoesPendentesLimitadas(limit) ?: emptyList()
    }
    
    /**
     * Atualiza uma operação de sincronização
     */
    suspend fun atualizarOperacaoSync(operation: SyncOperationEntity) {
        syncOperationDao?.atualizar(operation) ?: throw IllegalStateException("SyncOperationDao não inicializado")
    }
    
    /**
     * Deleta uma operação de sincronização
     */
    suspend fun deletarOperacaoSync(operation: SyncOperationEntity) {
        syncOperationDao?.deletar(operation) ?: throw IllegalStateException("SyncOperationDao não inicializado")
    }
    
    /**
     * Conta operações pendentes
     */
    suspend fun contarOperacoesSyncPendentes(): Int {
        return syncOperationDao?.contarOperacoesPendentes() ?: 0
    }
    
    /**
     * Conta operações falhadas
     */
    suspend fun contarOperacoesSyncFalhadas(): Int {
        return syncOperationDao?.contarOperacoesFalhadas() ?: 0
    }
    
    /**
     * Limpa operações completadas antigas (após X dias)
     */
    suspend fun limparOperacoesSyncCompletadas(dias: Int = 7) {
        val beforeTimestamp = System.currentTimeMillis() - (dias * 24 * 60 * 60 * 1000L)
        syncOperationDao?.limparOperacoesCompletadas(beforeTimestamp)
    }
    
    fun obterTodosStockItems() = stockItemDao?.listarTodos() ?: flowOf(emptyList())
    suspend fun inserirStockItem(item: com.example.gestaobilhares.data.entities.StockItem): Long = stockItemDao?.inserir(item) ?: 0L
    suspend fun obterStockItemPorId(id: Long) = stockItemDao?.buscarPorId(id)
    
    // ==================== EQUIPMENT ====================
    
    fun obterTodosEquipments() = equipmentDao?.listar() ?: flowOf(emptyList())
    suspend fun inserirEquipment(equipment: com.example.gestaobilhares.data.entities.Equipment): Long = equipmentDao?.inserir(equipment) ?: 0L
    suspend fun atualizarEquipment(equipment: com.example.gestaobilhares.data.entities.Equipment) = equipmentDao?.atualizar(equipment)
    suspend fun deletarEquipment(equipment: com.example.gestaobilhares.data.entities.Equipment) {
        // ✅ CORREÇÃO: Deletar do banco local
        equipmentDao?.deletar(equipment)
        
        // ✅ CORREÇÃO: Registrar operação de DELETE na fila de sincronização
        try {
            val operation = SyncOperationEntity(
                operationType = "DELETE",
                entityType = "Equipment",
                entityId = equipment.id.toString(),
                entityData = "{}",
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = "PENDING"
            )
            inserirOperacaoSync(operation)
            android.util.Log.d("AppRepository", "✅ Operação DELETE enfileirada para Equipment: ${equipment.id}")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao enfileirar DELETE de Equipment: ${e.message}", e)
        }
    }
    
    // ==================== META COLABORADOR (TODAS) ====================
    
    fun obterTodasMetaColaborador() = colaboradorDao.obterTodasMetaColaborador()
    
    // ==================== SYNC ====================
    // TODO: SyncRepository será integrado via delegação quando implementado
    // Por enquanto, métodos mantidos para compatibilidade
    
    @Suppress("UNUSED_PARAMETER")
    suspend fun adicionarAcertoComMesasParaSync(_acerto: Acerto, _mesas: List<com.example.gestaobilhares.data.entities.AcertoMesa>) {
        // TODO: Implementar quando SyncRepository estiver disponível
        // syncRepository.enqueueOperation(SyncOperation(...))
    }
    
    // ==================== CÁLCULOS ====================
    
    @Suppress("UNUSED_PARAMETER")
    suspend fun calcularMediaFichasJogadas(_mesaId: Long, _periodoDias: Int): Double {
        // TODO: Implementar cálculo de média de fichas jogadas
        return 0.0
    }
} 


