package com.example.gestaobilhares.ui.expenses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.DespesaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel para gerenciar despesas globais
 * Controla listagem, filtros por ciclo e resumos
 */
@HiltViewModel
class GlobalExpensesViewModel @Inject constructor(
    private val despesaRepository: DespesaRepository,
    private val cicloAcertoRepository: CicloAcertoRepository
) : ViewModel() {

    // Estados da UI
    private val _globalExpenses = MutableStateFlow<List<Despesa>>(emptyList())
    val globalExpenses: StateFlow<List<Despesa>> = _globalExpenses.asStateFlow()

    private val _availableCycles = MutableStateFlow<List<CicloAcertoEntity>>(emptyList())
    val availableCycles: StateFlow<List<CicloAcertoEntity>> = _availableCycles.asStateFlow()

    private val _selectedCycle = MutableLiveData<CicloAcertoEntity?>()
    val selectedCycle: LiveData<CicloAcertoEntity?> = _selectedCycle

    private val _selectedYear = MutableLiveData<Int>()
    val selectedYear: LiveData<Int> = _selectedYear

    private val _totalExpenses = MutableLiveData<Double>()
    val totalExpenses: LiveData<Double> = _totalExpenses

    private val _expensesCount = MutableLiveData<Int>()
    val expensesCount: LiveData<Int> = _expensesCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        // Inicializar com o ano atual
        _selectedYear.value = Calendar.getInstance().get(Calendar.YEAR)
        loadAvailableCycles()
        loadAllGlobalExpenses()
    }

    /**
     * Carrega todos os ciclos disponíveis para filtro
     */
    private fun loadAvailableCycles() {
        viewModelScope.launch {
            try {
                val currentYear = _selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR)
                
                // Buscar ciclos de todas as rotas para o ano atual
                // Vamos criar uma lista de ciclos baseada nos números de ciclo disponíveis
                val cycles = mutableListOf<CicloAcertoEntity>()
                
                // Criar ciclos de 1 a 12 para o ano atual (assumindo que podem existir até 12 ciclos por ano)
                for (numeroCiclo in 1..12) {
                    val ciclo = CicloAcertoEntity(
                        id = 0L, // ID temporário
                        rotaId = 0L, // Rota global
                        numeroCiclo = numeroCiclo,
                        ano = currentYear,
                        dataInicio = java.util.Date(),
                        dataFim = java.util.Date(),
                        status = com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO,
                        totalClientes = 0,
                        clientesAcertados = 0,
                        valorTotalAcertado = 0.0,
                        valorTotalDespesas = 0.0,
                        lucroLiquido = 0.0,
                        debitoTotal = 0.0,
                        observacoes = "Ciclo global $numeroCiclo",
                        criadoPor = "Sistema",
                        dataCriacao = java.util.Date(),
                        dataAtualizacao = java.util.Date()
                    )
                    cycles.add(ciclo)
                }
                
                _availableCycles.value = cycles
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar ciclos: ${e.message}"
            }
        }
    }

    /**
     * Carrega todas as despesas globais
     */
    fun loadAllGlobalExpenses() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _selectedCycle.value = null
                
                val currentYear = _selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR)
                
                // Buscar todas as despesas globais do ano atual
                val allExpenses = mutableListOf<Despesa>()
                
                // Buscar despesas de todos os ciclos do ano atual
                for (numeroCiclo in 1..12) {
                    val expenses = despesaRepository.buscarGlobaisPorCiclo(currentYear, numeroCiclo)
                    allExpenses.addAll(expenses)
                }
                
                _globalExpenses.value = allExpenses
                updateSummary(allExpenses)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar despesas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filtra despesas globais por ciclo específico
     */
    fun filterByCycle(cycle: CicloAcertoEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _selectedCycle.value = cycle
                
                val expenses = despesaRepository.buscarGlobaisPorCiclo(cycle.ano, cycle.numeroCiclo)
                
                _globalExpenses.value = expenses
                updateSummary(expenses)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao filtrar despesas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Atualiza o resumo das despesas
     */
    private fun updateSummary(expenses: List<Despesa>) {
        val total = expenses.sumOf { it.valor }
        val count = expenses.size
        
        _totalExpenses.value = total
        _expensesCount.value = count
    }

    /**
     * Remove uma despesa global
     */
    fun deleteGlobalExpense(expense: Despesa) {
        viewModelScope.launch {
            try {
                despesaRepository.deletar(expense)
                
                // Recarregar dados após exclusão
                if (_selectedCycle.value != null) {
                    filterByCycle(_selectedCycle.value!!)
                } else {
                    loadAllGlobalExpenses()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir despesa: ${e.message}"
            }
        }
    }

    /**
     * Limpa mensagens de erro
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Obtém o texto formatado do ciclo selecionado
     */
    fun getSelectedCycleText(): String {
        val cycle = _selectedCycle.value
        return if (cycle != null) {
            "${cycle.numeroCiclo}º Acerto - ${cycle.ano}"
        } else {
            "Todos os ciclos"
        }
    }

    /**
     * Verifica se há filtro ativo
     */
    fun hasActiveFilter(): Boolean {
        return _selectedCycle.value != null
    }

    /**
     * Atualiza o ano selecionado
     */
    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
        loadAvailableCycles()
        loadAllGlobalExpenses()
    }
}
