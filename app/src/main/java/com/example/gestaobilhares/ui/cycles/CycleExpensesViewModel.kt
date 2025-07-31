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
import com.example.gestaobilhares.data.repository.DespesaRepository
import com.example.gestaobilhares.data.database.AppDatabase

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
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val despesaRepository: DespesaRepository
) : ViewModel() {

    private val _despesas = MutableStateFlow<List<CycleExpenseItem>>(emptyList())
    val despesas: StateFlow<List<CycleExpenseItem>> = _despesas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ✅ NOVO: Evento para notificar mudanças nas despesas
    private val _despesaModificada = MutableStateFlow<Boolean>(false)
    val despesaModificada: StateFlow<Boolean> = _despesaModificada.asStateFlow()

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
     * ✅ CORREÇÃO: Persistir no banco de dados
     */
    fun removerDespesa(despesaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // ✅ CORREÇÃO: Buscar despesa real no banco e remover
                val despesaExistente = despesaRepository.buscarPorId(despesaId)
                
                if (despesaExistente != null) {
                    // Remover despesa do banco
                    despesaRepository.deletar(despesaExistente)
                    
                    // Recarregar lista de despesas
                    carregarDespesas(despesaExistente.cicloId ?: 0L)
                    
                    android.util.Log.d("CycleExpensesViewModel", "✅ Despesa $despesaId removida com sucesso do banco")
                } else {
                    _errorMessage.value = "Despesa não encontrada"
                }

            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesViewModel", "Erro ao remover despesa: ${e.message}")
                _errorMessage.value = "Erro ao remover despesa: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Edita uma despesa existente
     * ✅ CORREÇÃO: Persistir no banco de dados
     */
    fun editarDespesa(despesaId: Long, descricao: String, valor: Double, categoria: String, observacoes: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // ✅ CORREÇÃO: Buscar despesa real no banco e atualizar
                val despesaExistente = despesaRepository.buscarPorId(despesaId)
                
                if (despesaExistente != null) {
                    // Atualizar despesa no banco
                    val despesaAtualizada = despesaExistente.copy(
                        descricao = descricao,
                        valor = valor,
                        categoria = categoria,
                        observacoes = observacoes
                    )
                    
                    despesaRepository.atualizar(despesaAtualizada)
                    
                    // Recarregar lista de despesas
                    carregarDespesas(despesaExistente.cicloId ?: 0L)
                    
                    android.util.Log.d("CycleExpensesViewModel", "✅ Despesa $despesaId editada com sucesso no banco")
                } else {
                    _errorMessage.value = "Despesa não encontrada"
                }

            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesViewModel", "Erro ao editar despesa: ${e.message}")
                _errorMessage.value = "Erro ao editar despesa: ${e.message}"
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

    /**
     * ✅ NOVO: Limpa notificação de mudança
     */
    fun limparNotificacaoMudanca() {
        _despesaModificada.value = false
    }
}

/**
 * Factory para o ViewModel
 */
class CycleExpensesViewModelFactory(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val despesaRepository: DespesaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleExpensesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleExpensesViewModel(cicloAcertoRepository, despesaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}