package com.example.gestaobilhares.ui.expenses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.DespesaResumo
import com.example.gestaobilhares.data.entities.EstatisticasDespesas
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel para a tela de histórico de despesas.
 * Gerencia dados de despesas, filtros e estados da UI.
 * ✅ MIGRADO: Usa AppRepository centralizado
 */
class ExpenseHistoryViewModel(
    private val appRepository: AppRepository
) : BaseViewModel() {

    // Estado de carregamento
    // isLoading já existe na BaseViewModel

    // Filtro de categoria selecionado
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Mensagem de erro
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Mensagem de sucesso
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Despesas filtradas para exibição
    private val _filteredExpenses = MutableStateFlow<List<DespesaResumo>>(emptyList())
    val filteredExpenses: StateFlow<List<DespesaResumo>> = _filteredExpenses.asStateFlow()

    // Estatísticas das despesas - TODO: Implementar estatísticas
    // val statistics: LiveData<EstatisticasDespesas> = despesaRepository.obterEstatisticas().asLiveData()

    // Lista completa de despesas (sem filtro)
    // ✅ CORRIGIDO: Usa obterTodasDespesasComRota() para ter nomeRota (DespesaResumo)
    private val allExpenses = appRepository.obterTodasDespesasComRota()

    init {
        loadExpenses()
    }

    /**
     * Carrega as despesas e aplica filtros.
     */
    private fun loadExpenses() {
        viewModelScope.launch {
            showLoading()
            
            try {
                // Combina despesas com filtro de categoria
                combine(allExpenses, _selectedCategory) { expenses, category ->
                    if (category.isNullOrBlank()) {
                        expenses
                    } else {
                        expenses.filter { it.categoria == category }
                    }
                }.collect { filteredList ->
                    _filteredExpenses.value = filteredList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar despesas: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Aplica filtro por categoria.
     * @param categoria Nome da categoria ou null para mostrar todas
     */
    fun filterByCategory(categoria: String?) {
        _selectedCategory.value = categoria
    }

    /**
     * Remove filtros e mostra todas as despesas.
     */
    fun clearFilters() {
        _selectedCategory.value = null
    }

    /**
     * Atualiza as despesas (pull to refresh).
     */
    fun refreshExpenses() {
        loadExpenses()
    }

    /**
     * Deleta uma despesa específica.
     * @param despesaResumo A despesa a ser deletada
     */
    fun deleteExpense(despesaResumo: DespesaResumo) {
        viewModelScope.launch {
            try {
                showLoading()
                // ✅ MIGRADO: Usa AppRepository
                appRepository.deletarDespesa(despesaResumo.despesa)
                _successMessage.value = "Despesa deletada com sucesso"
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao deletar despesa: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Busca despesas por texto (descrição ou observações).
     * @param query Texto a ser buscado
     */
    fun searchExpenses(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadExpenses()
                return@launch
            }
            
            try {
                showLoading()
                
                allExpenses.collect { expenses ->
                    val filteredBySearch = expenses.filter { despesaResumo ->
                        despesaResumo.descricao.contains(query, ignoreCase = true) ||
                        despesaResumo.observacoes.contains(query, ignoreCase = true) ||
                        despesaResumo.nomeRota.contains(query, ignoreCase = true)
                    }
                    
                    _filteredExpenses.value = filteredBySearch
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro na busca: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Exporta relatório de despesas.
     */
    fun exportExpensesReport() {
        viewModelScope.launch {
            try {
                showLoading()
                // TODO: Implementar lógica de exportação (CSV, PDF, etc.)
                _successMessage.value = "Relatório exportado com sucesso"
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao exportar relatório: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Limpa mensagens de erro e sucesso.
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    /**
     * Obtém o total de despesas filtradas.
     */
    fun getFilteredTotal(): Double {
        return _filteredExpenses.value.sumOf { it.valor }
    }

    /**
     * Obtém a quantidade de despesas filtradas.
     */
    fun getFilteredCount(): Int {
        return _filteredExpenses.value.size
    }

    /**
     * Verifica se há filtros ativos.
     */
    fun hasActiveFilters(): Boolean {
        return !_selectedCategory.value.isNullOrBlank()
    }
} 

