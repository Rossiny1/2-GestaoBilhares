package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ViewModel para gerenciar recebimentos (acertos) do ciclo
 */
class CycleReceiptsViewModel(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val appRepository: AppRepository
) : BaseViewModel() {

    private val _receipts = MutableStateFlow<List<CycleReceiptItem>>(emptyList())
    val receipts: StateFlow<List<CycleReceiptItem>> = _receipts.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Carrega recebimentos (acertos) do ciclo
     */
    fun carregarRecebimentos(cicloId: Long) {
        viewModelScope.launch {
            try {
                showLoading()
                _errorMessage.value = null

                // Buscar acertos reais do ciclo
                val acertosReais = appRepository.buscarAcertosPorCicloId(cicloId).first()
                
                // Buscar clientes para mapear nomes
                val clientes = mutableMapOf<Long, com.example.gestaobilhares.data.entities.Cliente>()
                acertosReais.forEach { acerto ->
                    if (!clientes.containsKey(acerto.clienteId)) {
                        val cliente = appRepository.obterClientePorId(acerto.clienteId)
                        if (cliente != null) {
                            clientes[acerto.clienteId] = cliente
                        }
                    }
                }
                
                // Mapear para o formato de exibição
                val receiptsDTO = acertosReais.map { acerto ->
                    val cliente = clientes[acerto.clienteId]
                    val metodosPagamento = processarMetodosPagamento(acerto.metodosPagamentoJson)
                    val tipoPagamentoTexto = formatarTiposPagamento(metodosPagamento)
                    
                    CycleReceiptItem(
                        id = acerto.id,
                        clienteNome = cliente?.nome ?: "Cliente #${acerto.clienteId}",
                        dataAcerto = formatarData(acerto.dataAcerto),
                        tipoPagamento = tipoPagamentoTexto,
                        valorRecebido = acerto.valorRecebido,
                        debitoAtual = acerto.debitoAtual,
                        metodosPagamento = metodosPagamento
                    )
                }

                _receipts.value = receiptsDTO

            } catch (e: Exception) {
                android.util.Log.e("CycleReceiptsViewModel", "Erro ao carregar recebimentos: ${e.message}")
                _errorMessage.value = "Erro ao carregar recebimentos: ${e.message}"
            } finally {
                hideLoading()
            }
        }
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
            android.util.Log.e("CycleReceiptsViewModel", "Erro ao processar métodos de pagamento: ${e.message}")
            mapOf("Dinheiro" to 0.0)
        }
    }

    /**
     * Formata tipos de pagamento para exibição
     */
    private fun formatarTiposPagamento(metodosPagamento: Map<String, Double>): String {
        val currencyFormatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
        return if (metodosPagamento.size == 1) {
            metodosPagamento.keys.first()
        } else {
            metodosPagamento.entries
                .filter { it.value > 0 }
                .joinToString(", ") { "${it.key}: ${currencyFormatter.format(it.value)}" }
                .ifEmpty { "Não informado" }
        }
    }

    /**
     * Formata data para exibição
     */
    private fun formatarData(data: Date): String {
        val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
        return dateFormatter.format(data)
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
class CycleReceiptsViewModelFactory(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val appRepository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleReceiptsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleReceiptsViewModel(cicloAcertoRepository, appRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
