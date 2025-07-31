package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
class CycleManagementViewModel(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _dadosCiclo = MutableStateFlow<CycleManagementData?>(null)
    val dadosCiclo: StateFlow<CycleManagementData?> = _dadosCiclo.asStateFlow()

    private val _estatisticas = MutableStateFlow(CycleFinancialStats())
    val estatisticas: StateFlow<CycleFinancialStats> = _estatisticas.asStateFlow()

    private val _estatisticasModalidade = MutableStateFlow(PaymentMethodStats())
    val estatisticasModalidade: StateFlow<PaymentMethodStats> = _estatisticasModalidade.asStateFlow()

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
                        titulo = "${ciclo.numeroCiclo}º Acerto - ${rota.nome}",
                        dataInicio = ciclo.dataInicio,
                        dataFim = ciclo.dataFim,
                        status = ciclo.status
                    )
                    _dadosCiclo.value = dadosCiclo

                    // Calcular estatísticas financeiras
                    calcularEstatisticasFinanceiras(cicloId, rotaId)
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
     * Calcula estatísticas financeiras do ciclo (como no PDF)
     */
    private suspend fun calcularEstatisticasFinanceiras(cicloId: Long, rotaId: Long) {
        try {
            // Buscar dados
            val acertos = buscarAcertosPorCiclo(cicloId)
            val despesas = buscarDespesasPorCiclo(cicloId)

            // Calcular valores conforme especificação do PDF
            val totalRecebido = acertos.sumOf { it.valorRecebido }
            val despesasViagem = despesas.filter { it.categoria.equals("Viagem", ignoreCase = true) }.sumOf { it.valor }
            val subtotal = totalRecebido - despesasViagem
            val comissaoMotorista = subtotal * 0.03 // 3% do subtotal
            val comissaoIltair = totalRecebido * 0.02 // 2% do faturamento total

            // Calcular totais por modalidade
            val totaisPorModalidade = calcularTotaisPorModalidade(acertos)
            val somaPix = totaisPorModalidade["PIX"] ?: 0.0
            val somaCartao = totaisPorModalidade["Cartão"] ?: 0.0
            val totalCheques = totaisPorModalidade["Cheque"] ?: 0.0

            // Soma despesas = Total geral das despesas - despesas de viagem
            val totalGeralDespesas = despesas.sumOf { it.valor }
            val somaDespesas = totalGeralDespesas - despesasViagem

            val totalGeral = subtotal - comissaoMotorista - comissaoIltair - somaPix - somaCartao - somaDespesas - totalCheques

            // Atualizar estatísticas financeiras
            _estatisticas.value = CycleFinancialStats(
                totalRecebido = totalRecebido,
                despesasViagem = despesasViagem,
                subtotal = subtotal,
                comissaoMotorista = comissaoMotorista,
                comissaoIltair = comissaoIltair,
                somaPix = somaPix,
                somaDespesas = somaDespesas,
                cheques = totalCheques,
                totalGeral = totalGeral
            )

            // Atualizar estatísticas por modalidade
            _estatisticasModalidade.value = PaymentMethodStats(
                pix = somaPix,
                cartao = somaCartao,
                cheque = totalCheques,
                dinheiro = totaisPorModalidade["Dinheiro"] ?: 0.0,
                totalRecebido = totalRecebido
            )

        } catch (e: Exception) {
            android.util.Log.e("CycleManagementViewModel", "Erro ao calcular estatísticas: ${e.message}")
        }
    }

    /**
     * Busca acertos por ciclo
     */
    suspend fun buscarAcertosPorCiclo(cicloId: Long): List<Acerto> {
        return try {
            appRepository.buscarAcertosPorCicloId(cicloId).first()
        } catch (e: Exception) {
            android.util.Log.e("CycleManagementViewModel", "Erro ao buscar acertos: ${e.message}")
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
            android.util.Log.e("CycleManagementViewModel", "Erro ao buscar despesas: ${e.message}")
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
            android.util.Log.e("CycleManagementViewModel", "Erro ao buscar clientes: ${e.message}")
            emptyList()
        }
    }

    /**
     * Busca ciclo por ID
     */
    suspend fun buscarCicloPorId(cicloId: Long): CicloAcertoEntity? {
        return try {
            cicloAcertoRepository.buscarCicloPorId(cicloId)
        } catch (e: Exception) {
            android.util.Log.e("CycleManagementViewModel", "Erro ao buscar ciclo: ${e.message}")
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
            android.util.Log.e("CycleManagementViewModel", "Erro ao buscar rota: ${e.message}")
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
            if (metodosPagamentoJson.isNullOrBlank()) {
                mapOf("Dinheiro" to 0.0)
            } else {
                val tipo = object : TypeToken<Map<String, Double>>() {}.type
                Gson().fromJson(metodosPagamentoJson, tipo) ?: mapOf("Dinheiro" to 0.0)
            }
        } catch (e: Exception) {
            android.util.Log.e("CycleManagementViewModel", "Erro ao processar métodos de pagamento: ${e.message}")
            mapOf("Dinheiro" to 0.0)
        }
    }

    /**
     * Recarrega estatísticas
     */
    fun recarregarEstatisticas() {
        val dadosAtuais = _dadosCiclo.value
        if (dadosAtuais != null) {
            viewModelScope.launch {
                calcularEstatisticasFinanceiras(dadosAtuais.id, dadosAtuais.rotaId)
            }
        }
    }
}

/**
 * Factory para o ViewModel
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