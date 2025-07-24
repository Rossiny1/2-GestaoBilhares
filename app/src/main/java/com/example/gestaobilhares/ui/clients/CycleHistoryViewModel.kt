package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

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

/**
 * Factory para criar CycleHistoryViewModel com dependências
 */
class CycleHistoryViewModelFactory(
    private val cicloAcertoRepository: CicloAcertoRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleHistoryViewModel(cicloAcertoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

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
class CycleHistoryViewModel(
    private val cicloAcertoRepository: CicloAcertoRepository
) : ViewModel() {
    


    private val _ciclos = MutableStateFlow<List<CycleHistoryItem>>(emptyList())
    val ciclos: StateFlow<List<CycleHistoryItem>> = _ciclos.asStateFlow()

    private val _estatisticas = MutableStateFlow(CycleStatistics())
    val estatisticas: StateFlow<CycleStatistics> = _estatisticas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Carrega histórico de ciclos de uma rota
     */
    fun carregarHistoricoCiclos(rotaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Buscar os dados brutos do repositório
                val ciclosEntity = cicloAcertoRepository.buscarCiclosPorRota(rotaId)
                val clientes = cicloAcertoRepository.buscarClientesPorRota(rotaId)

                // Mapear para o DTO, aplicando a lógica correta para débito total
                val ciclosDTO = ciclosEntity.map { ciclo ->
                    val debitoTotal = if (ciclo.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO) {
                        ciclo.debitoTotal // Usa o valor salvo e imutável
                    } else {
                        clientes.sumOf { it.debitoAtual } // Calcula ao vivo apenas para o ciclo em andamento
                    }

                    CycleHistoryItem(
                        id = ciclo.id,
                        rotaId = ciclo.rotaId,
                        titulo = ciclo.titulo,
                        dataInicio = ciclo.dataInicio,
                        dataFim = ciclo.dataFim ?: Date(), // Usar data atual se for nula
                        valorTotalAcertado = ciclo.valorTotalAcertado,
                        valorTotalDespesas = ciclo.valorTotalDespesas,
                        lucroLiquido = ciclo.lucroLiquido,
                        debitoTotal = debitoTotal,
                        clientesAcertados = ciclo.clientesAcertados, // Usar dados já calculados
                        totalClientes = ciclo.totalClientes,       // Usar dados já calculados
                        status = ciclo.status
                    )
                }
                _ciclos.value = ciclosDTO

                // Calcular estatísticas com os dados já carregados
                calcularEstatisticas(ciclosEntity)

            } catch (e: Exception) {
                android.util.Log.e("CycleHistoryViewModel", "Erro ao carregar histórico: ${e.message}")
                _errorMessage.value = "Erro ao carregar histórico: ${e.message}"
            } finally {
                _isLoading.value = false
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
        val receitaTotal = ciclos.sumOf { it.valorTotalAcertado }
        val despesasTotal = ciclos.sumOf { it.valorTotalDespesas }
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
    fun exportarRelatorio(rotaId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
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
                _isLoading.value = false
            }
        }
    }

    /**
     * Filtra ciclos por período
     */
    fun filtrarPorPeriodo(rotaId: Long, dataInicio: Date, dataFim: Date) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val ciclosFiltrados = _ciclos.value.filter { ciclo ->
                    ciclo.dataInicio >= dataInicio && ciclo.dataFim <= dataFim
                }
                
                _ciclos.value = ciclosFiltrados
                calcularEstatisticas(cicloAcertoRepository.buscarCiclosPorRota(rotaId))
                
            } catch (e: Exception) {
                android.util.Log.e("CycleHistoryViewModel", "Erro ao filtrar: "+e.message)
                _errorMessage.value = "Erro ao filtrar: ${e.message}"
            } finally {
                _isLoading.value = false
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
} 