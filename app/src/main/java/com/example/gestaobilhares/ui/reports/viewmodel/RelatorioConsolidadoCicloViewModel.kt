package com.example.gestaobilhares.ui.reports.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para relatório consolidado por ciclo com comparação entre anos.
 * Permite comparar o mesmo ciclo de anos diferentes (ex: 1º ciclo 2024 vs 1º ciclo 2025).
 */
@HiltViewModel
class RelatorioConsolidadoCicloViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val database: AppDatabase
) : ViewModel() {
    
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
    
    init { carregarDadosIniciais() }
    
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
            val ciclosRepo = appRepository.getCiclos()
            val ciclosVm = ciclosRepo.map { CicloInfo(it.numero, "${it.numero}º Acerto") }
            _ciclos.value = ciclosVm
            
        } catch (e: Exception) {
            android.util.Log.e("RelatorioConsolidadoCicloViewModel", "Erro ao carregar ciclos: ${e.message}")
        }
    }
    
    private suspend fun carregarAnos() {
        try {
            val anos = database.cicloAcertoDao().listarAnosDisponiveis().first().sortedDescending()
            _anos.value = anos
            
        } catch (e: Exception) {
            android.util.Log.e("RelatorioConsolidadoCicloViewModel", "Erro ao carregar anos: ${e.message}")
        }
    }
    
    private suspend fun carregarDadosCiclo(ciclo: Int, ano: Int) {
        try {
            val ciclosAno = database.cicloAcertoDao().listarPorAno(ano).first()
                .filter { it.numeroCiclo == ciclo }
            val faturamento = ciclosAno.sumOf { it.valorTotalAcertado }
            val clientesAcertados = ciclosAno.sumOf { it.clientesAcertados }
            val rotas = appRepository.obterTodasRotas().first()
            val totalClientesAll = rotas.sumOf { rota -> appRepository.obterClientesPorRota(rota.id).first().size }
            val totalMesasAll = rotas.sumOf { rota -> appRepository.buscarMesasPorRota(rota.id).first().size }
            val ticketMedio = if (clientesAcertados > 0) faturamento / clientesAcertados else 0.0
            val dados = DadosCicloAno(
                ciclo = ciclo,
                ano = ano,
                faturamento = faturamento,
                clientesAcertados = clientesAcertados,
                mesasLocadas = totalMesasAll,
                ticketMedio = ticketMedio,
                totalClientes = totalClientesAll,
                totalMesas = totalMesasAll
            )
            if (ano == anoBase) _dadosAnoBase.value = dados else _dadosAnoComparacao.value = dados
            
        } catch (e: Exception) {
            android.util.Log.e("RelatorioConsolidadoCicloViewModel", "Erro ao carregar dados do ciclo: ${e.message}")
        }
    }
    
    private suspend fun carregarDetalhamentoRotas() {
        try {
            val rotas = appRepository.obterTodasRotas().first()
            val dao = database.cicloAcertoDao()
            val atualAnoList = dao.listarPorAno(anoBase).first().filter { it.numeroCiclo == cicloSelecionado }
            val compAnoList = dao.listarPorAno(anoComparacao).first().filter { it.numeroCiclo == cicloSelecionado }

            val detalhamento = rotas.map { rota ->
                val cicloAtual = atualAnoList.firstOrNull { it.rotaId == rota.id }
                val cicloComp = compAnoList.firstOrNull { it.rotaId == rota.id }
                val faturAtual = cicloAtual?.valorTotalAcertado ?: 0.0
                val faturComp = cicloComp?.valorTotalAcertado ?: 0.0
                val clientesAtual = cicloAtual?.clientesAcertados ?: 0
                val clientesComp = cicloComp?.clientesAcertados ?: 0
                val mesasAtual = appRepository.buscarMesasPorRota(rota.id).first().size
                val mesasComp = mesasAtual
                val variacaoFatur = calcularVariacao(faturAtual, faturComp)
                val variacaoClientes = calcularVariacao(clientesAtual.toDouble(), clientesComp.toDouble())
                val variacaoMesas = calcularVariacao(mesasAtual.toDouble(), mesasComp.toDouble())
                DetalhamentoRota(
                    rota = rota,
                    faturamentoAtual = faturAtual,
                    faturamentoComparacao = faturComp,
                    variacaoFaturamento = variacaoFatur,
                    clientesAtual = clientesAtual,
                    clientesComparacao = clientesComp,
                    variacaoClientes = variacaoClientes,
                    mesasAtual = mesasAtual,
                    mesasComparacao = mesasComp,
                    variacaoMesas = variacaoMesas
                )
            }.sortedByDescending { it.faturamentoAtual }

            _detalhamentoRotas.value = detalhamento
            
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
