package com.example.gestaobilhares.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import com.example.gestaobilhares.utils.AppLogger
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettlementDetailViewModel(
    private val acertoRepository: AcertoRepository,
    private val acertoMesaRepository: AcertoMesaRepository
) : ViewModel() {

    private val _settlementDetail = MutableLiveData<SettlementDetail?>()
    val settlementDetail: LiveData<SettlementDetail?> = _settlementDetail

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadSettlementDetails(acertoId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            AppLogger.log("SettlementDetail", "=== CARREGANDO DETALHES DO ACERTO ===")
            AppLogger.log("SettlementDetail", "Acerto ID: $acertoId")
            
            try {
                val acerto = acertoRepository.buscarPorId(acertoId)
                if (acerto != null) {
                    AppLogger.log("SettlementDetail", "✅ Acerto encontrado")
                    AppLogger.log("SettlementDetail", "Cliente ID: ${acerto.clienteId}")
                    AppLogger.log("SettlementDetail", "Total mesas esperadas: ${acerto.totalMesas}")
                    AppLogger.log("SettlementDetail", "Valor total: R$ ${acerto.valorTotal}")
                    
                    // ✅ CORREÇÃO CRÍTICA: Buscar dados detalhados por mesa com logs extensos
                    AppLogger.log("SettlementDetail", "=== INICIANDO BUSCA DAS MESAS ===")
                    val acertoMesas = acertoMesaRepository.buscarPorAcertoId(acertoId)
                    
                    AppLogger.log("SettlementDetail", "=== RESULTADO DA BUSCA ===")
                    AppLogger.log("SettlementDetail", "Acerto ID pesquisado: $acertoId")
                    AppLogger.log("SettlementDetail", "Quantidade de mesas encontradas: ${acertoMesas.size}")
                    AppLogger.log("SettlementDetail", "Quantidade esperada (totalMesas): ${acerto.totalMesas}")
                    
                    if (acertoMesas.isEmpty()) {
                        AppLogger.log("SettlementDetail", "❌ PROBLEMA CRÍTICO: Nenhuma mesa encontrada para o acerto!")
                        AppLogger.log("SettlementDetail", "Verificar se as mesas foram salvas corretamente no banco")
                    } else if (acertoMesas.size.toDouble() != acerto.totalMesas) {
                        AppLogger.log("SettlementDetail", "⚠️ INCONSISTÊNCIA: Quantidade de mesas não confere!")
                        AppLogger.log("SettlementDetail", "Esperado: ${acerto.totalMesas}, Encontrado: ${acertoMesas.size}")
                    }
                    
                    acertoMesas.forEachIndexed { index, acertoMesa ->
                        AppLogger.log("SettlementDetail", "=== MESA ${index + 1} DETALHADA ===")
                        AppLogger.log("SettlementDetail", "Mesa ID: ${acertoMesa.mesaId}")
                        AppLogger.log("SettlementDetail", "Acerto ID vinculado: ${acertoMesa.acertoId}")
                        AppLogger.log("SettlementDetail", "Relógio inicial: ${acertoMesa.relogioInicial}")
                        AppLogger.log("SettlementDetail", "Relógio final: ${acertoMesa.relogioFinal}")
                        AppLogger.log("SettlementDetail", "Fichas jogadas: ${acertoMesa.fichasJogadas}")
                        AppLogger.log("SettlementDetail", "Valor fixo: R$ ${acertoMesa.valorFixo}")
                        AppLogger.log("SettlementDetail", "Subtotal: R$ ${acertoMesa.subtotal}")
                        AppLogger.log("SettlementDetail", "Com defeito: ${acertoMesa.comDefeito}")
                        AppLogger.log("SettlementDetail", "Relógio reiniciou: ${acertoMesa.relogioReiniciou}")
                    }
                    
                    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                    val dataFormatada = formatter.format(acerto.dataAcerto)
                    
                    // ✅ CORREÇÃO: Logs detalhados para debug das observações nos detalhes
                    AppLogger.log("SettlementDetail", "=== CARREGANDO DETALHES - DEBUG OBSERVAÇÕES ===")
                    AppLogger.log("SettlementDetail", "Acerto ID: ${acerto.id}")
                    AppLogger.log("SettlementDetail", "Observação no banco: '${acerto.observacoes}'")
                    AppLogger.log("SettlementDetail", "Observação é nula? ${acerto.observacoes == null}")
                    AppLogger.log("SettlementDetail", "Observação é vazia? ${acerto.observacoes?.isEmpty()}")
                    
                    // ✅ CORREÇÃO: Garantir que observação seja exibida corretamente
                    val observacaoParaExibir = when {
                        acerto.observacoes.isNullOrBlank() -> "Nenhuma observação registrada."
                        else -> acerto.observacoes.trim()
                    }
                    
                    AppLogger.log("SettlementDetail", "Observação que será exibida: '$observacaoParaExibir'")

                    // ✅ CORREÇÃO: Processar métodos de pagamento
                    val metodosPagamento: Map<String, Double> = try {
                        acerto.metodosPagamentoJson?.let {
                            Gson().fromJson(it, object : TypeToken<Map<String, Double>>() {}.type)
                        } ?: emptyMap()
                    } catch (e: Exception) {
                        AppLogger.log("SettlementDetail", "Erro ao parsear métodos de pagamento: ${e.message}")
                        emptyMap()
                    }
                    
                    AppLogger.log("SettlementDetail", "=== TODOS OS DADOS CARREGADOS ===")
                    AppLogger.log("SettlementDetail", "Representante: '${acerto.representante}'")
                    AppLogger.log("SettlementDetail", "Tipo de acerto: '${acerto.tipoAcerto}'")
                    AppLogger.log("SettlementDetail", "Pano trocado: ${acerto.panoTrocado}")
                    AppLogger.log("SettlementDetail", "Número do pano: '${acerto.numeroPano}'")
                    AppLogger.log("SettlementDetail", "Métodos de pagamento: $metodosPagamento")
                    AppLogger.log("SettlementDetail", "Data finalização: ${acerto.dataFinalizacao}")

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
                    
                    _settlementDetail.value = settlementDetail
                    AppLogger.log("SettlementDetail", "Detalhes convertidos: $settlementDetail")
                } else {
                    AppLogger.log("SettlementDetail", "Acerto não encontrado para ID: $acertoId")
                    _settlementDetail.value = null
                }
            } catch (e: Exception) {
                AppLogger.log("SettlementDetail", "Erro ao carregar detalhes do acerto: ${e.message}")
                _settlementDetail.value = null
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

    /**
     * ✅ NOVA FUNCIONALIDADE: Busca um acerto por ID
     */
    suspend fun buscarAcertoPorId(acertoId: Long): Acerto? {
        return acertoRepository.buscarPorId(acertoId)
    }
} 