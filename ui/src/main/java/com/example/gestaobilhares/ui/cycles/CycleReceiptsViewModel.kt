package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ViewModel para CycleReceiptsFragment
 * ✅ IMPLEMENTADO: Carrega recebimentos (acertos) do ciclo reativamente, similar ao CycleExpensesViewModel
 */
class CycleReceiptsViewModel(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val appRepository: AppRepository
) : BaseViewModel() {
    
    // ✅ NOVO: Flow para cicloId atual para observação reativa
    private val _cicloIdFlow = MutableStateFlow<Long?>(null)
    
    private val _receipts = MutableStateFlow<List<CycleReceiptItem>>(emptyList())
    val receipts: StateFlow<List<CycleReceiptItem>> = _receipts.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // isLoading já existe na BaseViewModel
    
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val gson = Gson()
    
    init {
        // ✅ NOVO: Observar mudanças em acertos para atualização automática
        viewModelScope.launch {
            _cicloIdFlow
                .flatMapLatest { cicloId ->
                    if (cicloId == null) {
                        return@flatMapLatest flowOf(emptyList<CycleReceiptItem>())
                    }
                    
                    // Observar acertos do ciclo (Flow reativo)
                    appRepository.buscarAcertosPorCicloId(cicloId)
                        .map { acertosReais ->
                            processarAcertosParaReceipts(acertosReais)
                        }
                }
                .collect { receiptsDTO ->
                    _receipts.value = receiptsDTO
                    android.util.Log.d("CycleReceiptsViewModel", "✅ Recebimentos atualizados: ${receiptsDTO.size} itens")
                }
        }
    }
    
    /**
     * Carrega recebimentos do ciclo
     * ✅ CORRIGIDO: Agora apenas atualiza o cicloId, o init observa o Flow automaticamente
     */
    fun carregarRecebimentos(cicloId: Long) {
        _cicloIdFlow.value = cicloId
        android.util.Log.d("CycleReceiptsViewModel", "🔄 Carregando recebimentos para ciclo: $cicloId")
    }
    
    /**
     * Processa lista de acertos e converte para CycleReceiptItem
     */
    private suspend fun processarAcertosParaReceipts(acertosReais: List<com.example.gestaobilhares.data.entities.Acerto>): List<CycleReceiptItem> {
        return try {
            android.util.Log.d("CycleReceiptsViewModel", "📊 Processando ${acertosReais.size} acertos")
            
            acertosReais.map { acerto ->
                // Buscar nome do cliente
                val cliente = appRepository.obterClientePorId(acerto.clienteId)
                val clienteNome = cliente?.nome ?: "Cliente #${acerto.clienteId}"
                
                // Processar métodos de pagamento do JSON
                val metodosPagamento = processarMetodosPagamento(acerto.metodosPagamentoJson)
                
                // Formatar tipo de pagamento (mostrar métodos principais)
                val tipoPagamento = formatarTipoPagamento(metodosPagamento)
                
                // Formatar data do acerto
                val dataAcertoFormatada = dateFormatter.format(acerto.dataAcerto)
                
                CycleReceiptItem(
                    id = acerto.id,
                    clienteNome = clienteNome,
                    dataAcerto = dataAcertoFormatada,
                    tipoPagamento = tipoPagamento,
                    valorRecebido = acerto.valorRecebido,
                    debitoAtual = acerto.debitoAtual,
                    metodosPagamento = metodosPagamento
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("CycleReceiptsViewModel", "Erro ao processar acertos: ${e.message}", e)
            _errorMessage.value = "Erro ao processar acertos: ${e.message}"
            emptyList()
        }
    }
    
    /**
     * Processa JSON dos métodos de pagamento
     */
    private fun processarMetodosPagamento(metodosPagamentoJson: String?): Map<String, Double> {
        return try {
            if (metodosPagamentoJson.isNullOrBlank()) {
                emptyMap()
            } else {
                val tipo = object : TypeToken<Map<String, Double>>() {}.type
                gson.fromJson<Map<String, Double>>(metodosPagamentoJson, tipo) ?: emptyMap()
            }
        } catch (e: Exception) {
            android.util.Log.e("CycleReceiptsViewModel", "Erro ao processar métodos de pagamento: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Formata tipo de pagamento para exibição
     */
    private fun formatarTipoPagamento(metodosPagamento: Map<String, Double>): String {
        if (metodosPagamento.isEmpty()) {
            return "Dinheiro"
        }
        
        val metodos = metodosPagamento.keys.filter { metodosPagamento[it] ?: 0.0 > 0.0 }
        return if (metodos.isEmpty()) {
            "Dinheiro"
        } else {
            metodos.joinToString(", ")
        }
    }
    
    /**
     * Limpa mensagem de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }
}

