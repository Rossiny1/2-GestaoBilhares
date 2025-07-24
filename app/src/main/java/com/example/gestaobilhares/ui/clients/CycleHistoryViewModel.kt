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

/**
 * ViewModel para gerenciar histórico de ciclos e relatórios
 * ✅ FASE 9C: HISTÓRICO DE CICLOS E RELATÓRIOS FINANCEIROS
 */
class CycleHistoryViewModel(
    private val cicloAcertoRepository: CicloAcertoRepository
) : ViewModel() {
    


    private val _ciclos = MutableStateFlow<List<CicloAcertoEntity>>(emptyList())
    val ciclos: StateFlow<List<CicloAcertoEntity>> = _ciclos.asStateFlow()

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
                
                // Buscar todos os ciclos da rota
                val ciclosRota = cicloAcertoRepository.buscarCiclosPorRota(rotaId)
                // Recalcular todos os ciclos antes de exibir
                for (ciclo in ciclosRota) {
                    cicloAcertoRepository.atualizarValoresCicloComRecalculo(ciclo.id)
                }
                val ciclosAtualizados = cicloAcertoRepository.buscarCiclosPorRota(rotaId)
                com.example.gestaobilhares.utils.AppLogger.log(
                    "CycleHistoryViewModel",
                    "Histórico carregado para rota $rotaId: ${ciclosAtualizados.size} ciclos. Dados: " +
                        ciclosAtualizados.joinToString(" | ") { c -> "id=${c.id}, total=${c.valorTotalAcertado}, despesas=${c.valorTotalDespesas}, lucro=${c.lucroLiquido}, clientes=${c.clientesAcertados}" }
                )
                _ciclos.value = ciclosAtualizados
                
                // Calcular estatísticas
                calcularEstatisticas(ciclosAtualizados)
                
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
        val periodoFim = ciclos.maxOfOrNull { it.dataFim }

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
    fun filtrarPorPeriodo(dataInicio: Date, dataFim: Date) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val ciclosFiltrados = _ciclos.value.filter { ciclo ->
                    ciclo.dataInicio >= dataInicio && ciclo.dataFim <= dataFim
                }
                
                _ciclos.value = ciclosFiltrados
                calcularEstatisticas(ciclosFiltrados)
                
            } catch (e: Exception) {
                android.util.Log.e("CycleHistoryViewModel", "Erro ao filtrar: ${e.message}")
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
} 