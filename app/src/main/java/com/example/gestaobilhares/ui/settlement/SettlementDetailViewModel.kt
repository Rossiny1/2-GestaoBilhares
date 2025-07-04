package com.example.gestaobilhares.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.AcertoMesa
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ViewModel para SettlementDetailFragment
 * Busca dados reais do acerto no banco de dados
 */
class SettlementDetailViewModel(
    private val acertoRepository: AcertoRepository,
    private val acertoMesaRepository: AcertoMesaRepository
) : ViewModel() {

    private val _settlementDetails = MutableStateFlow<SettlementDetail?>(null)
    val settlementDetails: StateFlow<SettlementDetail?> = _settlementDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadSettlementDetails(acertoId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("SettlementDetailViewModel", "Carregando detalhes do acerto: $acertoId")
            
            try {
                val acerto = acertoRepository.buscarPorId(acertoId)
                if (acerto != null) {
                    Log.d("SettlementDetailViewModel", "Acerto encontrado: $acerto")
                    
                    // Buscar dados detalhados por mesa
                    val acertoMesas = acertoMesaRepository.buscarPorAcertoId(acertoId)
                    Log.d("SettlementDetailViewModel", "Encontradas ${acertoMesas.size} mesas para o acerto")
                    
                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                    val dataFormatada = formatter.format(acerto.dataAcerto)
                    
                    // ✅ CORREÇÃO: Logs detalhados para debug das observações nos detalhes
                    Log.d("SettlementDetailViewModel", "=== CARREGANDO DETALHES - DEBUG OBSERVAÇÕES ===")
                    Log.d("SettlementDetailViewModel", "Acerto ID: ${acerto.id}")
                    Log.d("SettlementDetailViewModel", "Observação no banco: '${acerto.observacoes}'")
                    Log.d("SettlementDetailViewModel", "Observação é nula? ${acerto.observacoes == null}")
                    Log.d("SettlementDetailViewModel", "Observação é vazia? ${acerto.observacoes?.isEmpty()}")
                    
                    // ✅ CORREÇÃO: Garantir que observação seja exibida corretamente
                    val observacaoParaExibir = when {
                        acerto.observacoes.isNullOrBlank() -> "Nenhuma observação registrada."
                        else -> acerto.observacoes.trim()
                    }
                    
                    Log.d("SettlementDetailViewModel", "Observação que será exibida: '$observacaoParaExibir'")

                    // ✅ CORREÇÃO: Processar métodos de pagamento
                    val metodosPagamento: Map<String, Double> = try {
                        acerto.metodosPagamentoJson?.let {
                            Gson().fromJson(it, object : TypeToken<Map<String, Double>>() {}.type)
                        } ?: emptyMap()
                    } catch (e: Exception) {
                        Log.e("SettlementDetailViewModel", "Erro ao parsear métodos de pagamento: ${e.message}")
                        emptyMap()
                    }
                    
                    Log.d("SettlementDetailViewModel", "=== TODOS OS DADOS CARREGADOS ===")
                    Log.d("SettlementDetailViewModel", "Representante: '${acerto.representante}'")
                    Log.d("SettlementDetailViewModel", "Tipo de acerto: '${acerto.tipoAcerto}'")
                    Log.d("SettlementDetailViewModel", "Pano trocado: ${acerto.panoTrocado}")
                    Log.d("SettlementDetailViewModel", "Número do pano: '${acerto.numeroPano}'")
                    Log.d("SettlementDetailViewModel", "Métodos de pagamento: $metodosPagamento")
                    Log.d("SettlementDetailViewModel", "Data finalização: ${acerto.dataFinalizacao}")

                    val settlementDetail = SettlementDetail(
                        id = acerto.id,
                        date = dataFormatada,
                        status = acerto.status.name,
                        valorTotal = acerto.valorTotal,
                        valorRecebido = acerto.valorRecebido,
                        desconto = acerto.desconto,
                        debitoAnterior = acerto.debitoAnterior,
                        debitoAtual = acerto.debitoAtual,
                        totalMesas = acerto.totalMesas.toInt(),
                        observacoes = observacaoParaExibir,
                        acertoMesas = acertoMesas,
                        // ✅ NOVOS DADOS: Incluir todos os campos que estavam sendo perdidos
                        representante = acerto.representante ?: "Não informado",
                        tipoAcerto = acerto.tipoAcerto ?: "Presencial",
                        panoTrocado = acerto.panoTrocado,
                        numeroPano = acerto.numeroPano,
                        metodosPagamento = metodosPagamento,
                        dataFinalizacao = acerto.dataFinalizacao
                    )
                    
                    _settlementDetails.value = settlementDetail
                    Log.d("SettlementDetailViewModel", "Detalhes convertidos: $settlementDetail")
                } else {
                    Log.w("SettlementDetailViewModel", "Acerto não encontrado para ID: $acertoId")
                    _settlementDetails.value = null
                }
            } catch (e: Exception) {
                Log.e("SettlementDetailViewModel", "Erro ao carregar detalhes do acerto", e)
                _settlementDetails.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ ATUALIZADA: Data class para representar TODOS os detalhes do acerto
    data class SettlementDetail(
        val id: Long,
        val date: String,
        val status: String,
        val valorTotal: Double,
        val valorRecebido: Double,
        val desconto: Double,
        val debitoAnterior: Double,
        val debitoAtual: Double,
        val totalMesas: Int,
        val observacoes: String,
        val acertoMesas: List<AcertoMesa>,
        // ✅ NOVOS CAMPOS: Resolver problema de dados perdidos
        val representante: String,
        val tipoAcerto: String,
        val panoTrocado: Boolean,
        val numeroPano: String?,
        val metodosPagamento: Map<String, Double>,
        val dataFinalizacao: Date?
    )
} 