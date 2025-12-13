package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Date
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Dados do ciclo para gerenciamento
 */
data class CycleManagementData(
    val id: Long,
    val rotaId: Long,
    val titulo: String,
    val dataInicio: Date,
    val dataFim: Date?,
    val status: StatusCicloAcerto
)

/**
 * Estatísticas financeiras do ciclo (como no PDF)
 */
data class CycleFinancialStats(
    val totalRecebido: Double = 0.0,
    val despesasViagem: Double = 0.0,
    val subtotal: Double = 0.0,
    val comissaoMotorista: Double = 0.0,
    val comissaoIltair: Double = 0.0,
    val somaPix: Double = 0.0,
    val somaDespesas: Double = 0.0,
    val cheques: Double = 0.0,
    val totalGeral: Double = 0.0
)

/**
 * Estatísticas por modalidade de pagamento
 */
data class PaymentMethodStats(
    val pix: Double = 0.0,
    val cartao: Double = 0.0,
    val cheque: Double = 0.0,
    val dinheiro: Double = 0.0,
    val totalRecebido: Double = 0.0
)

/**
 * ViewModel para gerenciar dados do ciclo
 */

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CycleManagementViewModel @Inject constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {

    // ✅ NOVO: Flow para cicloId e rotaId atual para observação reativa
    private val _cicloIdFlow = MutableStateFlow<Long?>(null)
    private val _rotaIdFlow = MutableStateFlow<Long?>(null)

    private val _dadosCiclo = MutableStateFlow<CycleManagementData?>(null)
    val dadosCiclo: StateFlow<CycleManagementData?> = _dadosCiclo.asStateFlow()

    private val _estatisticas = MutableStateFlow(CycleFinancialStats())
    val estatisticas: StateFlow<CycleFinancialStats> = _estatisticas.asStateFlow()

    private val _estatisticasModalidade = MutableStateFlow(PaymentMethodStats())
    val estatisticasModalidade: StateFlow<PaymentMethodStats> = _estatisticasModalidade.asStateFlow()

    // Estados de loading e error já estão no BaseViewModel
    
    init {
        // ✅ NOVO: Observar mudanças em despesas e acertos para atualização automática
        viewModelScope.launch {
            combine(
                _cicloIdFlow,
                _rotaIdFlow
            ) { cicloId, rotaId ->
                Pair(cicloId, rotaId)
            }
            .flatMapLatest { (cicloId, rotaId) ->
                if (cicloId == null || rotaId == null) {
                    return@flatMapLatest flowOf(Unit)
                }
                
                // Observar despesas e acertos simultaneamente
                combine(
                    appRepository.buscarDespesasPorCicloId(cicloId),
                    appRepository.buscarAcertosPorCicloId(cicloId)
                ) { despesas, acertos ->
                    // Buscar ciclo e rota (são suspend, então buscamos uma vez)
                    val ciclo = appRepository.buscarCicloPorId(cicloId)
                    val rota = appRepository.buscarRotaPorId(rotaId)
                    
                    if (ciclo != null && rota != null) {
                        // Atualizar dados do ciclo
                        _dadosCiclo.value = CycleManagementData(
                            id = ciclo.id,
                            rotaId = ciclo.rotaId,
                            titulo = "${ciclo.numeroCiclo}º Acerto - ${rota.nome}",
                            dataInicio = ciclo.dataInicio,
                            dataFim = ciclo.dataFim,
                            status = ciclo.status
                        )
                        
                        // Recalcular estatísticas
                        calcularEstatisticasFinanceirasReativo(acertos, despesas)
                    }
                    Unit
                }
            }
            .collect { }
        }
    }
    
    // ✅ NOVO: Método para calcular estatísticas de forma reativa
    private fun calcularEstatisticasFinanceirasReativo(acertos: List<Acerto>, despesas: List<Despesa>) {
        try {
            val estatisticas = com.example.gestaobilhares.core.utils.FinancialCalculator.calcularEstatisticasCiclo(
                acertos = acertos,
                despesas = despesas
            )
            
            _estatisticas.value = CycleFinancialStats(
                totalRecebido = estatisticas.totalRecebido,
                despesasViagem = estatisticas.despesasViagem,
                subtotal = estatisticas.subtotal,
                comissaoMotorista = estatisticas.comissaoMotorista,
                comissaoIltair = estatisticas.comissaoIltair,
                somaPix = estatisticas.somaPix,
                somaDespesas = estatisticas.somaDespesas,
                cheques = estatisticas.cheques,
                totalGeral = estatisticas.totalGeral
            )
            
            _estatisticasModalidade.value = PaymentMethodStats(
                pix = estatisticas.somaPix,
                cartao = estatisticas.somaCartao,
                cheque = estatisticas.cheques,
                dinheiro = estatisticas.dinheiro,
                totalRecebido = estatisticas.totalRecebido
            )
        } catch (e: Exception) {
            logError("STATS_CALC_REACTIVE", "Erro ao calcular estatísticas: ${e.message}", e)
        }
    }

    /**
     * Carrega dados do ciclo
     * ✅ CORRIGIDO: Agora apenas atualiza os Flows, o init observa automaticamente
     */
    fun carregarDadosCiclo(cicloId: Long, rotaId: Long) {
        _cicloIdFlow.value = cicloId
        _rotaIdFlow.value = rotaId
        viewModelScope.launch {
            try {
                showLoading()
                // Aguardar um pouco para os dados carregarem
                kotlinx.coroutines.delay(100)
                hideLoading()
            } catch (e: Exception) {
                logError("CYCLE_LOAD", "Erro ao carregar dados do ciclo: ${e.message}", e)
                showError("Erro ao carregar dados: ${e.message}", e)
                hideLoading()
            }
        }
    }

    /**
     * ✅ FASE 1: Calcula estatísticas financeiras do ciclo usando FinancialCalculator centralizado
     */
    private suspend fun calcularEstatisticasFinanceiras(cicloId: Long, @Suppress("UNUSED_PARAMETER") rotaId: Long) {
        try {
            android.util.Log.d("CycleManagementViewModel", "🔍 Calculando estatísticas financeiras para ciclo: $cicloId")
            
            // Buscar dados
            val acertos = buscarAcertosPorCiclo(cicloId)
            val despesas = buscarDespesasPorCiclo(cicloId)
            
            android.util.Log.d("CycleManagementViewModel", "📊 Dados encontrados: ${acertos.size} acertos, ${despesas.size} despesas")

            // ✅ FASE 1: Usar FinancialCalculator centralizado
            val estatisticas = com.example.gestaobilhares.core.utils.FinancialCalculator.calcularEstatisticasCiclo(
                acertos = acertos,
                despesas = despesas
            )
            
            android.util.Log.d("CycleManagementViewModel", "💰 Estatísticas calculadas: totalRecebido=${estatisticas.totalRecebido}, despesasViagem=${estatisticas.despesasViagem}, subtotal=${estatisticas.subtotal}")

            // Atualizar estatísticas financeiras
            _estatisticas.value = CycleFinancialStats(
                totalRecebido = estatisticas.totalRecebido,
                despesasViagem = estatisticas.despesasViagem,
                subtotal = estatisticas.subtotal,
                comissaoMotorista = estatisticas.comissaoMotorista,
                comissaoIltair = estatisticas.comissaoIltair,
                somaPix = estatisticas.somaPix,
                somaDespesas = estatisticas.somaDespesas,
                cheques = estatisticas.cheques,
                totalGeral = estatisticas.totalGeral
            )

            // Atualizar estatísticas por modalidade
            _estatisticasModalidade.value = PaymentMethodStats(
                pix = estatisticas.somaPix,
                cartao = estatisticas.somaCartao,
                cheque = estatisticas.cheques,
                dinheiro = estatisticas.dinheiro,
                totalRecebido = estatisticas.totalRecebido
            )

        } catch (e: Exception) {
            logError("STATS_CALC", "Erro ao calcular estatísticas: ${e.message}", e)
        }
    }

    /**
     * Busca acertos por ciclo
     */
    suspend fun buscarAcertosPorCiclo(cicloId: Long): List<Acerto> {
        return try {
            appRepository.buscarAcertosPorCicloId(cicloId).first()
        } catch (e: Exception) {
            logError("ACERTOS_SEARCH", "Erro ao buscar acertos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Busca despesas por ciclo
     */
    suspend fun buscarDespesasPorCiclo(cicloId: Long): List<Despesa> {
        return try {
            appRepository.buscarDespesasPorCicloId(cicloId).first()
        } catch (e: Exception) {
            logError("DESPESAS_SEARCH", "Erro ao buscar despesas: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Busca clientes por rota
     */
    suspend fun buscarClientesPorRota(rotaId: Long): List<Cliente> {
        return try {
            appRepository.buscarClientesPorRota(rotaId).first()
        } catch (e: Exception) {
            logError("CLIENTES_SEARCH", "Erro ao buscar clientes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Busca ciclo por ID
     */
    suspend fun buscarCicloPorId(cicloId: Long): CicloAcertoEntity? {
        return try {
            appRepository.buscarCicloPorId(cicloId)
        } catch (e: Exception) {
            logError("CICLO_SEARCH", "Erro ao buscar ciclo: ${e.message}", e)
            null
        }
    }

    /**
     * Busca rota por ID
     */
    suspend fun buscarRotaPorId(rotaId: Long): Rota? {
        return try {
            appRepository.buscarRotaPorId(rotaId)
        } catch (e: Exception) {
            logError("ROTA_SEARCH", "Erro ao buscar rota: ${e.message}", e)
            null
        }
    }

    /**
     * Calcula totais por modalidade de pagamento
     */
    private fun calcularTotaisPorModalidade(acertos: List<Acerto>): Map<String, Double> {
        val totais = mutableMapOf(
            "PIX" to 0.0,
            "Cartão" to 0.0,
            "Cheque" to 0.0,
            "Dinheiro" to 0.0
        )
        
        acertos.forEach { acerto ->
            val metodos = processarMetodosPagamento(acerto.metodosPagamentoJson)
            metodos.forEach { (metodo, valor) ->
                when (metodo.uppercase()) {
                    "PIX" -> totais["PIX"] = (totais["PIX"] ?: 0.0) + valor
                    "CARTÃO", "CARTAO" -> totais["Cartão"] = (totais["Cartão"] ?: 0.0) + valor
                    "CHEQUE" -> totais["Cheque"] = (totais["Cheque"] ?: 0.0) + valor
                    "DINHEIRO" -> totais["Dinheiro"] = (totais["Dinheiro"] ?: 0.0) + valor
                    else -> {
                        // Para métodos não mapeados, adicionar como dinheiro
                        totais["Dinheiro"] = (totais["Dinheiro"] ?: 0.0) + valor
                    }
                }
            }
        }
        
        return totais
    }

    /**
     * Processa JSON dos métodos de pagamento
     */
    private fun processarMetodosPagamento(metodosPagamentoJson: String?): Map<String, Double> {
        return try {
            if (com.example.gestaobilhares.core.utils.StringUtils.isVazia(metodosPagamentoJson)) {
                mapOf("Dinheiro" to 0.0)
            } else {
                val tipo = object : TypeToken<Map<String, Double>>() {}.type
                Gson().fromJson(metodosPagamentoJson, tipo) ?: mapOf("Dinheiro" to 0.0)
            }
        } catch (e: Exception) {
            logError("PAYMENT_METHODS", "Erro ao processar métodos de pagamento: ${e.message}", e)
            mapOf("Dinheiro" to 0.0)
        }
    }

    /**
     * Recarrega estatísticas
     * ✅ CORRIGIDO: Não precisa fazer nada, os Flows já observam automaticamente
     */
    fun recarregarEstatisticas() {
        val dadosAtuais = _dadosCiclo.value
        if (dadosAtuais != null) {
            // Os Flows já observam automaticamente, apenas garantir que os IDs estão corretos
            if (_cicloIdFlow.value != dadosAtuais.id) {
                _cicloIdFlow.value = dadosAtuais.id
            }
            if (_rotaIdFlow.value != dadosAtuais.rotaId) {
                _rotaIdFlow.value = dadosAtuais.rotaId
            }
        }
    }
}

/**
 * Factory para o ViewModel
 */


