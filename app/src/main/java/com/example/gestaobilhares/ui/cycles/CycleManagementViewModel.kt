package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Dados do ciclo para gerenciamento
 */
data class CycleManagementData(
    val id: Long,
    val rotaId: Long,
    val titulo: String,
    val dataInicio: Date,
    val dataFim: Date,
    val status: String
)

/**
 * Estatísticas financeiras do ciclo
 */
data class CycleFinancialStats(
    val receitas: Double = 0.0,
    val despesas: Double = 0.0,
    val lucro: Double = 0.0
)

/**
 * ViewModel para gerenciar dados do ciclo em andamento
 */
class CycleManagementViewModel(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _dadosCiclo = MutableStateFlow<CycleManagementData?>(null)
    val dadosCiclo: StateFlow<CycleManagementData?> = _dadosCiclo.asStateFlow()

    private val _estatisticas = MutableStateFlow(CycleFinancialStats())
    val estatisticas: StateFlow<CycleFinancialStats> = _estatisticas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Carrega dados do ciclo
     */
    fun carregarDadosCiclo(cicloId: Long, rotaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Buscar dados do ciclo
                val ciclo = cicloAcertoRepository.buscarCicloPorId(cicloId)
                val rota = cicloAcertoRepository.buscarRotaPorId(rotaId)

                if (ciclo != null && rota != null) {
                    // Mapear para DTO
                    val dadosCiclo = CycleManagementData(
                        id = ciclo.id,
                        rotaId = ciclo.rotaId,
                        titulo = "${ciclo.numeroCiclo}º Ciclo - ${rota.nome}",
                        dataInicio = ciclo.dataInicio,
                        dataFim = ciclo.dataFim,
                        status = ciclo.status.name
                    )
                    _dadosCiclo.value = dadosCiclo

                    // Calcular estatísticas financeiras
                    calcularEstatisticasFinanceiras(cicloId)
                } else {
                    _errorMessage.value = "Ciclo não encontrado"
                }

            } catch (e: Exception) {
                android.util.Log.e("CycleManagementViewModel", "Erro ao carregar dados do ciclo: ${e.message}")
                _errorMessage.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calcula estatísticas financeiras do ciclo
     */
    private suspend fun calcularEstatisticasFinanceiras(cicloId: Long) {
        try {
            // Buscar dados do ciclo
            val ciclo = cicloAcertoRepository.buscarCicloPorId(cicloId)
            
            if (ciclo != null) {
                val receitas = ciclo.valorTotalAcertado
                
                // ✅ CORRIGIDO: Usar o mesmo método que funciona no card de progresso
                val despesasCiclo = appRepository.buscarDespesasPorCicloId(cicloId).first()
                val totalDespesas = despesasCiclo.sumOf { despesa -> despesa.valor }
                
                val lucro = receitas - totalDespesas

                _estatisticas.value = CycleFinancialStats(
                    receitas = receitas,
                    despesas = totalDespesas,
                    lucro = lucro
                )
            }

        } catch (e: Exception) {
            android.util.Log.e("CycleManagementViewModel", "Erro ao calcular estatísticas: ${e.message}")
            _errorMessage.value = "Erro ao calcular estatísticas: ${e.message}"
        }
    }

    /**
     * Adiciona nova despesa ao ciclo
     */
    fun adicionarDespesa(descricao: String, valor: Double, categoria: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val cicloId = _dadosCiclo.value?.id ?: return@launch
                
                // TODO: Implementar adição de despesa quando o repositório estiver pronto
                // Por enquanto, apenas simular sucesso
                android.util.Log.d("CycleManagementViewModel", "Adicionando despesa: $descricao - R$ $valor")
                
                // Recarregar estatísticas
                calcularEstatisticasFinanceiras(cicloId)

            } catch (e: Exception) {
                android.util.Log.e("CycleManagementViewModel", "Erro ao adicionar despesa: ${e.message}")
                _errorMessage.value = "Erro ao adicionar despesa: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Remove despesa do ciclo
     */
    fun removerDespesa(despesaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val cicloId = _dadosCiclo.value?.id ?: return@launch
                
                // TODO: Implementar remoção de despesa quando o repositório estiver pronto
                // Por enquanto, apenas simular sucesso
                android.util.Log.d("CycleManagementViewModel", "Removendo despesa: $despesaId")
                
                // Recarregar estatísticas
                calcularEstatisticasFinanceiras(cicloId)

            } catch (e: Exception) {
                android.util.Log.e("CycleManagementViewModel", "Erro ao remover despesa: ${e.message}")
                _errorMessage.value = "Erro ao remover despesa: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpa mensagem de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }
}

/**
 * Factory para criar CycleManagementViewModel com dependências
 */
class CycleManagementViewModelFactory(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val appRepository: AppRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleManagementViewModel(cicloAcertoRepository, appRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}