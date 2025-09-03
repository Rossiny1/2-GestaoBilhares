package com.example.gestaobilhares.ui.reports.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.*

@HiltViewModel
class ColaboradorPerformanceViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    // Data class local para CicloInfo
    data class CicloInfo(
        val numero: Int,
        val descricao: String
    )

    // Data class simplificada para PerformanceColaborador
    data class PerformanceColaborador(
        val id: Long,
        val nome: String,
        val faturamento: Double,
        val clientesAcertados: Int,
        val mesasLocadas: Int,
        val status: String
    )

    // Data class simplificada para EstatisticasPerformance
    data class EstatisticasPerformance(
        val totalColaboradores: Int,
        val colaboradoresAtivos: Int,
        val faturamentoTotal: Double,
        val mediaClientesAcertados: Double
    )

    // LiveData para UI
    private val _performanceData = MutableLiveData<List<PerformanceColaborador>>()
    val performanceData: LiveData<List<PerformanceColaborador>> = _performanceData

    private val _estatisticas = MutableLiveData<EstatisticasPerformance>()
    val estatisticas: LiveData<EstatisticasPerformance> = _estatisticas

    private val _ciclos = MutableLiveData<List<CicloInfo>>()
    val ciclos: LiveData<List<CicloInfo>> = _ciclos

    private val _rotas = MutableLiveData<List<Rota>>()
    val rotas: LiveData<List<Rota>> = _rotas

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Estado dos filtros
    private var cicloSelecionado: Long = 0
    private var rotaSelecionada: Long = 0

    init {
        carregarDadosIniciais()
    }

    private fun carregarDadosIniciais() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar dados básicos - usar métodos alternativos
                val ciclosData = listOf(
                    CicloInfo(1, "1º Ciclo"),
                    CicloInfo(2, "2º Ciclo"),
                    CicloInfo(3, "3º Ciclo"),
                    CicloInfo(4, "4º Ciclo"),
                    CicloInfo(5, "5º Ciclo"),
                    CicloInfo(6, "6º Ciclo"),
                    CicloInfo(7, "7º Ciclo"),
                    CicloInfo(8, "8º Ciclo"),
                    CicloInfo(9, "9º Ciclo"),
                    CicloInfo(10, "10º Ciclo"),
                    CicloInfo(11, "11º Ciclo"),
                    CicloInfo(12, "12º Ciclo")
                )
                val rotasData = repository.obterTodasRotas().first()
                
                _ciclos.value = ciclosData
                _rotas.value = rotasData
                
                // Selecionar valores padrão
                if (ciclosData.isNotEmpty()) {
                    selecionarCiclo(ciclosData.first().numero.toLong())
                }
                if (rotasData.isNotEmpty()) {
                    selecionarRota(rotasData.first().id)
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun selecionarCiclo(cicloId: Long) {
        cicloSelecionado = cicloId
        aplicarFiltros()
    }
    
    fun selecionarRota(rotaId: Long?) {
        rotaSelecionada = rotaId ?: 0
        aplicarFiltros()
    }
    
    fun aplicarFiltros() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Dados mock para demonstração
                val performanceMock = listOf(
                    PerformanceColaborador(1L, "João Silva", 14250.0, 76, 9, "Excelente"),
                    PerformanceColaborador(2L, "Maria Santos", 10800.0, 63, 7, "Bom"),
                    PerformanceColaborador(3L, "Pedro Costa", 9500.0, 45, 6, "Regular")
                )
                
                _performanceData.value = performanceMock
                
                val estatisticas = EstatisticasPerformance(
                    totalColaboradores = 3,
                    colaboradoresAtivos = 3,
                    faturamentoTotal = performanceMock.sumOf { it.faturamento },
                    mediaClientesAcertados = performanceMock.map { it.clientesAcertados }.average()
                )
                
                _estatisticas.value = estatisticas
                _isLoading.value = false
                
            } catch (e: Exception) {
                _error.value = "Erro ao aplicar filtros: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}
