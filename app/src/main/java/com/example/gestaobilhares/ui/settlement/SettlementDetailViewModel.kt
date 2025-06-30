package com.example.gestaobilhares.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.entities.Acerto
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para SettlementDetailFragment
 * Busca dados reais do acerto no banco de dados
 */
@HiltViewModel
class SettlementDetailViewModel @Inject constructor(
    private val acertoRepository: AcertoRepository
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
                    
                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                    val dataFormatada = formatter.format(acerto.dataAcerto)
                    
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
                        observacoes = acerto.observacoes ?: "Nenhuma observação registrada."
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

    // Data class para representar os detalhes do acerto
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
        val observacoes: String
    )
} 