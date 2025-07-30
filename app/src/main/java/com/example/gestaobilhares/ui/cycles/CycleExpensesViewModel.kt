package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.time.ZoneId

/**
 * Extensão para converter LocalDateTime para Date
 */
fun java.time.LocalDateTime.toDate(): Date {
    return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}

/**
 * Dados de despesa para exibição
 */
data class CycleExpenseItem(
    val id: Long,
    val descricao: String,
    val valor: Double,
    val categoria: String,
    val data: Date,
    val observacoes: String? = null
)

/**
 * ViewModel para gerenciar despesas do ciclo
 */
class CycleExpensesViewModel(
    private val cicloAcertoRepository: CicloAcertoRepository
) : ViewModel() {

    private val _despesas = MutableStateFlow<List<CycleExpenseItem>>(emptyList())
    val despesas: StateFlow<List<CycleExpenseItem>> = _despesas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Carrega despesas do ciclo
     */
    fun carregarDespesas(cicloId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Buscar despesas reais do ciclo
                val despesasReais = cicloAcertoRepository.buscarDespesasPorCiclo(cicloId)
                
                // Mapear para o formato de exibição
                val despesasDTO = despesasReais.map { despesa ->
                    CycleExpenseItem(
                        id = despesa.id,
                        descricao = despesa.descricao,
                        valor = despesa.valor,
                        categoria = despesa.categoria,
                        data = despesa.dataHora.toDate(), // Converter LocalDateTime para Date
                        observacoes = despesa.observacoes
                    )
                }

                _despesas.value = despesasDTO

            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesViewModel", "Erro ao carregar despesas: ${e.message}")
                _errorMessage.value = "Erro ao carregar despesas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Remove uma despesa
     */
    fun removerDespesa(despesaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // TODO: Implementar remoção real quando o repositório estiver pronto
                android.util.Log.d("CycleExpensesViewModel", "Removendo despesa: $despesaId")
                
                // Simular remoção
                val despesasAtuais = _despesas.value.toMutableList()
                despesasAtuais.removeAll { it.id == despesaId }
                _despesas.value = despesasAtuais

            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesViewModel", "Erro ao remover despesa: ${e.message}")
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
 * Factory para o ViewModel
 */
class CycleExpensesViewModelFactory(
    private val cicloAcertoRepository: CicloAcertoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleExpensesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleExpensesViewModel(cicloAcertoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}