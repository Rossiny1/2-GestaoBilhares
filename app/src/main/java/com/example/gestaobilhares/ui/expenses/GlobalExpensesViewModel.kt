package com.example.gestaobilhares.ui.expenses

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
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
) : BaseViewModel() {

    // Estados da UI
    private val _globalExpenses = MutableStateFlow<List<Despesa>>(emptyList())
    val globalExpenses: StateFlow<List<Despesa>> = _globalExpenses.asStateFlow()
    
    private val _availableCycles = MutableStateFlow<List<CicloAcertoEntity>>(emptyList())
    val availableCycles: StateFlow<List<CicloAcertoEntity>> = _availableCycles.asStateFlow()

    private val _selectedCycle = MutableStateFlow<CicloAcertoEntity?>(null)
    val selectedCycle: StateFlow<CicloAcertoEntity?> = _selectedCycle.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _totalExpenses = MutableStateFlow<Double>(0.0)
    val totalExpenses: StateFlow<Double> = _totalExpenses.asStateFlow()

    private val _expensesCount = MutableStateFlow<Int>(0)
    val expensesCount: StateFlow<Int> = _expensesCount.asStateFlow()

    // isLoading já existe na BaseViewModel

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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
                showLoading()
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
                hideLoading()
            }
        }
    }

    /**
     * Filtra despesas globais por ciclo específico
     */
    fun filterByCycle(cycle: CicloAcertoEntity) {
        viewModelScope.launch {
            try {
                showLoading()
                _selectedCycle.value = cycle
                
                val expenses = despesaRepository.buscarGlobaisPorCiclo(cycle.ano, cycle.numeroCiclo)
                
                _globalExpenses.value = expenses
                updateSummary(expenses)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao filtrar despesas: ${e.message}"
            } finally {
                hideLoading()
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
    // clearError já existe na BaseViewModel

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
