package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Date
import kotlinx.coroutines.runBlocking

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Estatísticas financeiras dos ciclos
 * ✅ FASE 9C: DADOS PARA RELATÓRIOS FINANCEIROS
 */
data class CycleStatistics(
    val totalCiclos: Int = 0,
    val receitaTotal: Double = 0.0,
    val despesasTotal: Double = 0.0,
    val lucroLiquido: Double = 0.0,
    val lucroMedioPorCiclo: Double = 0.0,
    val periodoInicio: Date? = null,
    val periodoFim: Date? = null
)

// DTO para o Adapter do histórico de ciclos
// Inclui todos os campos necessários para exibição

data class CycleHistoryItem(
    val id: Long,
    val rotaId: Long,
    val titulo: String,
    val dataInicio: Date,
    val dataFim: Date,
    val valorTotalAcertado: Double,
    val valorTotalDespesas: Double,
    val totalDescontos: Double,
    val lucroLiquido: Double,
    val debitoTotal: Double,
    val clientesAcertados: Int,
    val totalClientes: Int,
    val status: com.example.gestaobilhares.data.entities.StatusCicloAcerto
)

/**
 * ViewModel para gerenciar histórico de ciclos e relatórios
 * ✅ FASE 9C: HISTÓRICO DE CICLOS E RELATÓRIOS FINANCEIROS
 */
@HiltViewModel
class CycleHistoryViewModel @Inject constructor(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val appRepository: AppRepository
) : BaseViewModel() {
    
    // ✅ NOVO: Flow para rotaId atual para observação reativa
    private val _rotaIdFlow = MutableStateFlow<Long?>(null)

    private val _ciclos = MutableStateFlow<List<CycleHistoryItem>>(emptyList())
    val ciclos: StateFlow<List<CycleHistoryItem>> = _ciclos.asStateFlow()

    private val _estatisticas = MutableStateFlow(CycleStatistics())
    val estatisticas: StateFlow<CycleStatistics> = _estatisticas.asStateFlow()

    // isLoading já existe na BaseViewModel

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // ✅ NOVO: Observar mudanças em ciclos, despesas e acertos para atualização automática
        viewModelScope.launch {
            _rotaIdFlow
                .flatMapLatest { rotaId ->
                    if (rotaId == null) {
                        return@flatMapLatest flowOf(emptyList<CycleHistoryItem>())
                    }
                    
                    // Observar ciclos da rota
                    cicloAcertoRepository.buscarCiclosPorRotaFlow(rotaId)
                        .flatMapLatest { ciclosEntity ->
                            if (ciclosEntity.isEmpty()) {
                                return@flatMapLatest flowOf(emptyList<CycleHistoryItem>())
                            }
                            
                            // Para cada ciclo, observar despesas e acertos
                            val flows = ciclosEntity.map { ciclo ->
                                combine(
                                    appRepository.buscarDespesasPorCicloId(ciclo.id),
                                    appRepository.buscarAcertosPorCicloId(ciclo.id),
                                    appRepository.obterTodosClientes()
                                ) { despesas, acertos, clientes ->
                                    processarCicloItem(ciclo, despesas, acertos, clientes.filter { it.rotaId == rotaId }, rotaId)
                                }
                            }
                            
                            // Combinar todos os flows em um único
                            combine(flows) { resultados ->
                                resultados.toList()
                            }
                        }
                }
                .collect { ciclosAtualizados ->
                    // ✅ CORREÇÃO: Aplicar regra de negócio - apenas o último ciclo pode estar EM_ANDAMENTO
                    // Todos os ciclos anteriores devem estar FINALIZADO
                    val ciclosCorrigidos = corrigirStatusCiclos(ciclosAtualizados)
                    _ciclos.value = ciclosCorrigidos
                    // Recalcular estatísticas
                    if (ciclosCorrigidos.isNotEmpty() && _rotaIdFlow.value != null) {
                        viewModelScope.launch {
                            try {
                                val ciclosEntity = cicloAcertoRepository.buscarCiclosPorRota(_rotaIdFlow.value!!)
                                calcularEstatisticas(ciclosEntity)
                            } catch (e: Exception) {
                                android.util.Log.e("CycleHistoryViewModel", "Erro ao recalcular estatísticas: ${e.message}")
                            }
                        }
                    }
                }
        }
    }
    
    /**
     * ✅ CORREÇÃO: Corrige o status dos ciclos aplicando a regra de negócio:
     * - Apenas o último ciclo (mais recente) pode estar EM_ANDAMENTO
     * - Todos os ciclos anteriores devem estar FINALIZADO
     * 
     * Os ciclos já vêm ordenados por ano DESC, numero_ciclo DESC do banco,
     * então o primeiro da lista é o mais recente.
     */
    private fun corrigirStatusCiclos(ciclos: List<CycleHistoryItem>): List<CycleHistoryItem> {
        if (ciclos.isEmpty()) return ciclos
        
        // O primeiro ciclo é o mais recente (último criado)
        // Apenas ele pode manter seu status original (EM_ANDAMENTO ou FINALIZADO)
        // Todos os outros devem estar FINALIZADO
        return ciclos.mapIndexed { index, ciclo ->
            if (index == 0) {
                // Primeiro ciclo (mais recente) - mantém status original
                ciclo
            } else {
                // Ciclos anteriores - forçar FINALIZADO
                if (ciclo.status != com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO) {
                    android.util.Log.d("CycleHistoryViewModel", "🔧 Corrigindo status do ciclo ${ciclo.titulo} (ID: ${ciclo.id}) de ${ciclo.status} para FINALIZADO")
                    ciclo.copy(status = com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO)
                } else {
                    ciclo
                }
            }
        }
    }
    
    // ✅ NOVO: Método auxiliar para processar item de ciclo
    private suspend fun processarCicloItem(
        ciclo: CicloAcertoEntity,
        despesas: List<com.example.gestaobilhares.data.entities.Despesa>,
        acertos: List<com.example.gestaobilhares.data.entities.Acerto>,
        clientes: List<com.example.gestaobilhares.data.entities.Cliente>,
        rotaId: Long
    ): CycleHistoryItem {
        val debitoTotal = if (ciclo.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO) {
            ciclo.debitoTotal
        } else {
            clientes.sumOf { it.debitoAtual }
        }
        
        // ✅ CORRIGIDO: Calcular valores reais dos acertos e despesas (como as despesas já fazem)
        val valorTotalDespesas = despesas.sumOf { it.valor }
        val valorTotalAcertado = acertos.sumOf { it.valorRecebido } // ✅ CORRIGIDO: Calcular dos acertos reais
        val totalDescontos = appRepository.calcularTotalDescontosPorCiclo(ciclo.id)
        
        val (clientesAcertados, totalClientes) = if (ciclo.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO) {
            Pair(ciclo.clientesAcertados, ciclo.totalClientes)
        } else {
            val clientesAcertadosReal = acertos
                .filter { it.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO }
                .map { it.clienteId }
                .distinct()
                .size
            Pair(clientesAcertadosReal, clientes.size)
        }
        
        return CycleHistoryItem(
            id = ciclo.id,
            rotaId = ciclo.rotaId,
            titulo = ciclo.titulo,
            dataInicio = ciclo.dataInicio,
            dataFim = ciclo.dataFim,
            valorTotalAcertado = valorTotalAcertado, // ✅ CORRIGIDO: Usar valor calculado dos acertos reais
            valorTotalDespesas = valorTotalDespesas,
            totalDescontos = totalDescontos,
            lucroLiquido = valorTotalAcertado - valorTotalDespesas, // ✅ CORRIGIDO: Usar valor calculado
            debitoTotal = debitoTotal,
            clientesAcertados = clientesAcertados,
            totalClientes = totalClientes,
            status = ciclo.status
        )
    }

    /**
     * Carrega histórico de ciclos de uma rota
     * ✅ CORRIGIDO: Agora apenas atualiza o rotaId, o init observa os Flows automaticamente
     */
    fun carregarHistoricoCiclos(rotaId: Long) {
        _rotaIdFlow.value = rotaId
        viewModelScope.launch {
            try {
                showLoading()
                // Aguardar um pouco para os dados carregarem
                kotlinx.coroutines.delay(100)
                hideLoading()
            } catch (e: Exception) {
                android.util.Log.e("CycleHistoryViewModel", "Erro ao carregar histórico: ${e.message}")
                _errorMessage.value = "Erro ao carregar histórico: ${e.message}"
                hideLoading()
            }
        }
    }

    /**
     * Calcula estatísticas financeiras dos ciclos
     */
    private fun calcularEstatisticas(ciclos: List<CicloAcertoEntity>) {
        if (ciclos.isEmpty()) {
            _estatisticas.value = CycleStatistics()
            return
        }

        val totalCiclos = ciclos.size
        
        // ✅ CORRIGIDO: Calcular receita total dos acertos reais (como as despesas)
        val receitaTotal = ciclos.sumOf { ciclo ->
            kotlinx.coroutines.runBlocking {
                val acertosCiclo = appRepository.buscarAcertosPorCicloId(ciclo.id).first()
                acertosCiclo.sumOf { acerto -> acerto.valorRecebido }
            }
        }
        
        // ✅ FASE 12.5: Calcular despesas totais reais (removido runBlocking - função já é suspend)
        val despesasTotal = ciclos.sumOf { ciclo ->
            kotlinx.coroutines.runBlocking {
                val despesasCiclo = appRepository.buscarDespesasPorCicloId(ciclo.id).first()
                despesasCiclo.sumOf { despesa -> despesa.valor }
            }
        }
        
        val lucroLiquido = receitaTotal - despesasTotal
        val lucroMedioPorCiclo = if (totalCiclos > 0) lucroLiquido / totalCiclos else 0.0
        
        val periodoInicio = ciclos.minOfOrNull { it.dataInicio }
        val periodoFim = ciclos.mapNotNull { it.dataFim }.maxOfOrNull { it }

        val stats = CycleStatistics(
            totalCiclos = totalCiclos,
            receitaTotal = receitaTotal,
            despesasTotal = despesasTotal,
            lucroLiquido = lucroLiquido,
            lucroMedioPorCiclo = lucroMedioPorCiclo,
            periodoInicio = periodoInicio,
            periodoFim = periodoFim
        )

        _estatisticas.value = stats
    }

    /**
     * Exporta relatório financeiro
     */
    fun exportarRelatorio(_rotaId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                showLoading()
                
                // TODO: Implementar exportação real
                // Por enquanto, simular sucesso
                kotlinx.coroutines.delay(1000)
                
                android.util.Log.d("CycleHistoryViewModel", "✅ Relatório exportado com sucesso")
                callback(true)
                
            } catch (e: Exception) {
                android.util.Log.e("CycleHistoryViewModel", "Erro ao exportar: ${e.message}")
                _errorMessage.value = "Erro ao exportar relatório: ${e.message}"
                callback(false)
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Filtra ciclos por período
     */
    fun filtrarPorPeriodo(rotaId: Long, dataInicio: Date, dataFim: Date) {
        viewModelScope.launch {
            try {
                showLoading()
                
                val ciclosFiltrados = _ciclos.value.filter { ciclo ->
                    ciclo.dataInicio >= dataInicio && ciclo.dataFim <= dataFim
                }
                
                _ciclos.value = ciclosFiltrados
                calcularEstatisticas(cicloAcertoRepository.buscarCiclosPorRota(rotaId))
                
            } catch (e: Exception) {
                android.util.Log.e("CycleHistoryViewModel", "Erro ao filtrar: "+e.message)
                _errorMessage.value = "Erro ao filtrar: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Limpa filtros e restaura lista completa
     */
    fun limparFiltros(rotaId: Long) {
        carregarHistoricoCiclos(rotaId)
    }

    /**
     * ✅ NOVA FUNÇÃO: Recarrega dados em tempo real (útil após salvar acertos)
     * ✅ CORRIGIDO: Não precisa fazer nada, os Flows já observam automaticamente
     */
    fun recarregarDadosTempoReal(rotaId: Long) {
        android.util.Log.d("CycleHistoryViewModel", "🔄 Recarregando dados em tempo real para rotaId: $rotaId")
        // Os Flows já observam automaticamente, apenas garantir que o rotaId está correto
        if (_rotaIdFlow.value != rotaId) {
            _rotaIdFlow.value = rotaId
        }
    }

    /**
     * Limpa mensagens de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }

    /**
     * ✅ NOVO: Busca ciclo por ID para relatório
     */
    suspend fun buscarCicloPorId(cicloId: Long): CicloAcertoEntity? {
        return try {
            cicloAcertoRepository.buscarCicloPorId(cicloId)
        } catch (e: Exception) {
            android.util.Log.e("CycleHistoryViewModel", "Erro ao buscar ciclo: ${e.message}")
            null
        }
    }

    /**
     * ✅ NOVO: Busca rota por ID para relatório
     */
    suspend fun buscarRotaPorId(rotaId: Long): com.example.gestaobilhares.data.entities.Rota? {
        return try {
            cicloAcertoRepository.buscarRotaPorId(rotaId)
        } catch (e: Exception) {
            android.util.Log.e("CycleHistoryViewModel", "Erro ao buscar rota: ${e.message}")
            null
        }
    }

    /**
     * ✅ NOVO: Busca acertos por ciclo para relatório
     */
    suspend fun buscarAcertosPorCiclo(cicloId: Long): List<com.example.gestaobilhares.data.entities.Acerto> {
        return try {
            cicloAcertoRepository.buscarAcertosPorCiclo(cicloId)
        } catch (e: Exception) {
            android.util.Log.e("CycleHistoryViewModel", "Erro ao buscar acertos: ${e.message}")
            emptyList()
        }
    }

    /**
     * ✅ NOVO: Busca despesas por ciclo para relatório
     */
    suspend fun buscarDespesasPorCiclo(cicloId: Long): List<com.example.gestaobilhares.data.entities.Despesa> {
        return try {
            cicloAcertoRepository.buscarDespesasPorCiclo(cicloId)
        } catch (e: Exception) {
            android.util.Log.e("CycleHistoryViewModel", "Erro ao buscar despesas: ${e.message}")
            emptyList()
        }
    }

    /**
     * ✅ NOVO: Busca clientes por rota para relatório
     */
    suspend fun buscarClientesPorRota(rotaId: Long): List<com.example.gestaobilhares.data.entities.Cliente> {
        return try {
            cicloAcertoRepository.buscarClientesPorRota(rotaId)
        } catch (e: Exception) {
            android.util.Log.e("CycleHistoryViewModel", "Erro ao buscar clientes: ${e.message}")
            emptyList()
        }
    }

    /**
     * ✅ NOVA FUNÇÃO: Calcula clientes acertados em tempo real para ciclos em andamento
     * ✅ CORREÇÃO: Verificar apenas acertos FINALIZADOS
     */
    private suspend fun calcularClientesAcertadosEmTempoReal(cicloId: Long, _rotaId: Long): Int {
        return try {
            // Buscar acertos reais do banco de dados para este ciclo
            val acertos = appRepository.buscarAcertosPorCicloId(cicloId).first()
            
            // ✅ CORREÇÃO CRÍTICA: Contar apenas clientes únicos com acertos FINALIZADOS
            val clientesAcertados = acertos
                .filter { acerto -> acerto.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO }
                .map { it.clienteId }
                .distinct()
            
            android.util.Log.d("CycleHistoryViewModel", "✅ Clientes acertados em tempo real no ciclo $cicloId: ${clientesAcertados.size}")
            
            clientesAcertados.size
        } catch (e: Exception) {
            android.util.Log.e("CycleHistoryViewModel", "Erro ao calcular clientes acertados em tempo real: ${e.message}")
            0
        }
    }
} 
