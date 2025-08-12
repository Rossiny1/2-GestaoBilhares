package com.example.gestaobilhares.ui.reports.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para relatório consolidado por ciclo com comparação entre anos.
 * Permite comparar o mesmo ciclo de anos diferentes (ex: 1º ciclo 2024 vs 1º ciclo 2025).
 */
class RelatorioConsolidadoCicloViewModel : ViewModel() {
    
    private lateinit var appRepository: AppRepository
    
    // ==================== DADOS OBSERVÁVEIS ====================
    
    // Ciclos disponíveis
    private val _ciclos = MutableLiveData<List<CicloInfo>>()
    val ciclos: LiveData<List<CicloInfo>> = _ciclos
    
    // Anos disponíveis
    private val _anos = MutableLiveData<List<Int>>()
    val anos: LiveData<List<Int>> = _anos
    
    // Dados do ano base
    private val _dadosAnoBase = MutableLiveData<DadosCicloAno>()
    val dadosAnoBase: LiveData<DadosCicloAno> = _dadosAnoBase
    
    // Dados do ano de comparação
    private val _dadosAnoComparacao = MutableLiveData<DadosCicloAno>()
    val dadosAnoComparacao: LiveData<DadosCicloAno> = _dadosAnoComparacao
    
    // Detalhamento por rota
    private val _detalhamentoRotas = MutableLiveData<List<DetalhamentoRota>>()
    val detalhamentoRotas: LiveData<List<DetalhamentoRota>> = _detalhamentoRotas
    
    // Estado de loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Mensagens de erro
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Filtros atuais
    private var cicloSelecionado: Int = 1
    private var anoBase: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var anoComparacao: Int = anoBase - 1
    
    // ==================== INICIALIZAÇÃO ====================
    
    init {
        setupRepository()
        carregarDadosIniciais()
    }
    
    private fun setupRepository() {
        // TODO: Injetar via Hilt ou Dagger
        // Por enquanto, criar manualmente
        // appRepository = AppRepository(...)
    }
    
    // ==================== CARREGAMENTO DE DADOS ====================
    
    fun carregarDadosIniciais() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar ciclos disponíveis
                carregarCiclos()
                
                // Carregar anos disponíveis
                carregarAnos()
                
                // Carregar dados do ano atual
                carregarDadosCiclo(cicloSelecionado, anoBase)
                
                // Carregar dados do ano de comparação
                carregarDadosCiclo(cicloSelecionado, anoComparacao)
                
                // Carregar detalhamento por rota
                carregarDetalhamentoRotas()
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun carregarCiclos() {
        try {
            // TODO: Implementar quando AppRepository estiver disponível
            // val ciclos = appRepository.obterCiclosDisponiveis().first()
            
            // Dados mock para demonstração
            val ciclosMock = listOf(
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
            _ciclos.value = ciclosMock
            
        } catch (e: Exception) {
            android.util.Log.e("RelatorioConsolidadoCicloViewModel", "Erro ao carregar ciclos: ${e.message}")
        }
    }
    
    private suspend fun carregarAnos() {
        try {
            // TODO: Implementar quando AppRepository estiver disponível
            // val anos = appRepository.obterAnosDisponiveis().first()
            
            // Dados mock para demonstração
            val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
            val anosMock = (anoAtual - 5..anoAtual).toList().reversed()
            _anos.value = anosMock
            
        } catch (e: Exception) {
            android.util.Log.e("RelatorioConsolidadoCicloViewModel", "Erro ao carregar anos: ${e.message}")
        }
    }
    
    private suspend fun carregarDadosCiclo(ciclo: Int, ano: Int) {
        try {
            // TODO: Implementar quando AppRepository estiver disponível
            // val dados = appRepository.obterDadosCicloAno(ciclo, ano).first()
            
            // Dados mock para demonstração
            val dadosMock = DadosCicloAno(
                ciclo = ciclo,
                ano = ano,
                faturamento = 150000.0 + (Math.random() * 50000).toDouble(),
                clientesAcertados = 80 + (Math.random() * 20).toInt(),
                mesasLocadas = 120 + (Math.random() * 30).toInt(),
                ticketMedio = 150.0 + (Math.random() * 50).toDouble(),
                totalClientes = 100 + (Math.random() * 20).toInt(),
                totalMesas = 150 + (Math.random() * 30).toInt()
            )
            
            if (ano == anoBase) {
                _dadosAnoBase.value = dadosMock
            } else {
                _dadosAnoComparacao.value = dadosMock
            }
            
        } catch (e: Exception) {
            android.util.Log.e("RelatorioConsolidadoCicloViewModel", "Erro ao carregar dados do ciclo: ${e.message}")
        }
    }
    
    private suspend fun carregarDetalhamentoRotas() {
        try {
            // TODO: Implementar quando AppRepository estiver disponível
            // val detalhamento = appRepository.obterDetalhamentoRotasCiclo(cicloSelecionado, anoBase, anoComparacao).first()
            
            // Dados mock para demonstração
            val detalhamentoMock = listOf(
                DetalhamentoRota(
                    rota = Rota(
                        id = 1L,
                        nome = "Rota Zona Sul",
                        descricao = "Zona Sul da cidade",
                        ativa = true,
                        dataCriacao = System.currentTimeMillis(),
                        dataAtualizacao = System.currentTimeMillis()
                    ),
                    faturamentoAtual = 45000.0,
                    faturamentoComparacao = 42000.0,
                    variacaoFaturamento = 7.14,
                    clientesAtual = 25,
                    clientesComparacao = 23,
                    variacaoClientes = 8.70,
                    mesasAtual = 35,
                    mesasComparacao = 32,
                    variacaoMesas = 9.38
                ),
                DetalhamentoRota(
                    rota = Rota(
                        id = 2L,
                        nome = "Rota Zona Norte",
                        descricao = "Zona Norte da cidade",
                        ativa = true,
                        dataCriacao = System.currentTimeMillis(),
                        dataAtualizacao = System.currentTimeMillis()
                    ),
                    faturamentoAtual = 38000.0,
                    faturamentoComparacao = 35000.0,
                    variacaoFaturamento = 8.57,
                    clientesAtual = 20,
                    clientesComparacao = 18,
                    variacaoClientes = 11.11,
                    mesasAtual = 28,
                    mesasComparacao = 25,
                    variacaoMesas = 12.0
                ),
                DetalhamentoRota(
                    rota = Rota(
                        id = 3L,
                        nome = "Rota Centro",
                        descricao = "Centro da cidade",
                        ativa = true,
                        dataCriacao = System.currentTimeMillis(),
                        dataAtualizacao = System.currentTimeMillis()
                    ),
                    faturamentoAtual = 67000.0,
                    faturamentoComparacao = 73000.0,
                    variacaoFaturamento = -8.22,
                    clientesAtual = 35,
                    clientesComparacao = 39,
                    variacaoClientes = -10.26,
                    mesasAtual = 57,
                    mesasComparacao = 63,
                    variacaoMesas = -9.52
                )
            )
            _detalhamentoRotas.value = detalhamentoMock
            
        } catch (e: Exception) {
            android.util.Log.e("RelatorioConsolidadoCicloViewModel", "Erro ao carregar detalhamento: ${e.message}")
        }
    }
    
    // ==================== FILTROS ====================
    
    fun selecionarCiclo(ciclo: Int) {
        cicloSelecionado = ciclo
        aplicarFiltros()
    }
    
    fun selecionarAnoBase(ano: Int) {
        anoBase = ano
        aplicarFiltros()
    }
    
    fun selecionarAnoComparacao(ano: Int) {
        anoComparacao = ano
        aplicarFiltros()
    }
    
    private fun aplicarFiltros() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Recarregar dados com filtros aplicados
                carregarDadosCiclo(cicloSelecionado, anoBase)
                carregarDadosCiclo(cicloSelecionado, anoComparacao)
                carregarDetalhamentoRotas()
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao aplicar filtros: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ==================== CÁLCULOS ====================
    
    fun calcularVariacao(valorAtual: Double, valorComparacao: Double): Double {
        return if (valorComparacao > 0) {
            ((valorAtual - valorComparacao) / valorComparacao) * 100
        } else {
            0.0
        }
    }
    
    fun formatarVariacao(variacao: Double): String {
        val sinal = if (variacao >= 0) "+" else ""
        return "$sinal${String.format("%.1f", variacao)}%"
    }
    
    fun formatarMoeda(valor: Double): String {
        return "R$ ${String.format("%.2f", valor).replace(".", ",")}"
    }
}

// ==================== DATA CLASSES ====================

data class CicloInfo(
    val numero: Int,
    val descricao: String
)

data class DadosCicloAno(
    val ciclo: Int,
    val ano: Int,
    val faturamento: Double,
    val clientesAcertados: Int,
    val mesasLocadas: Int,
    val ticketMedio: Double,
    val totalClientes: Int,
    val totalMesas: Int
) {
    val percentualClientesAcertados: Double
        get() = if (totalClientes > 0) (clientesAcertados.toDouble() / totalClientes) * 100 else 0.0
    
    val percentualMesasLocadas: Double
        get() = if (totalMesas > 0) (mesasLocadas.toDouble() / totalMesas) * 100 else 0.0
}

data class DetalhamentoRota(
    val rota: Rota,
    val faturamentoAtual: Double,
    val faturamentoComparacao: Double,
    val variacaoFaturamento: Double,
    val clientesAtual: Int,
    val clientesComparacao: Int,
    val variacaoClientes: Double,
    val mesasAtual: Int,
    val mesasComparacao: Int,
    val variacaoMesas: Double
)
