package com.example.gestaobilhares.ui.settlement

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Suppress("DEPRECATION")
class SettlementDetailViewModel(
    private val acertoRepository: AcertoRepository,
    private val appRepository: AppRepository,
    private val clienteRepository: com.example.gestaobilhares.data.repository.ClienteRepository
) : BaseViewModel() {

    private val _settlementDetail = MutableLiveData<SettlementDetail?>()
    val settlementDetail: LiveData<SettlementDetail?> = _settlementDetail

    // Estados de loading e error já estão no BaseViewModel

    fun loadSettlementDetails(acertoId: Long) {
        viewModelScope.launch {
            showLoading()
            logOperation("SETTLEMENT_DETAIL", "Carregando detalhes do acerto ID: $acertoId")
            
            try {
                val acerto = acertoRepository.buscarPorId(acertoId)
                if (acerto != null) {
                    android.util.Log.d("SettlementDetail", "✅ Acerto encontrado")
                    android.util.Log.d("SettlementDetail", "Cliente ID: ${acerto.clienteId}")
                    android.util.Log.d("SettlementDetail", "Total mesas esperadas: ${acerto.totalMesas}")
                    android.util.Log.d("SettlementDetail", "Valor total: R$ ${acerto.valorTotal}")
                    
                    // ✅ CORREÇÃO CRÍTICA: Buscar dados detalhados por mesa com logs extensos
                    android.util.Log.d("SettlementDetail", "=== INICIANDO BUSCA DAS MESAS ===")
                    val acertoMesas = appRepository.buscarAcertoMesasPorAcerto(acertoId)
                    
                    android.util.Log.d("SettlementDetail", "=== RESULTADO DA BUSCA ===")
                    android.util.Log.d("SettlementDetail", "Acerto ID pesquisado: $acertoId")
                    android.util.Log.d("SettlementDetail", "Quantidade de mesas encontradas: ${acertoMesas.size}")
                    android.util.Log.d("SettlementDetail", "Quantidade esperada (totalMesas): ${acerto.totalMesas}")
                    
                    if (acertoMesas.isEmpty()) {
                        android.util.Log.d("SettlementDetail", "❌ PROBLEMA CRÍTICO: Nenhuma mesa encontrada para o acerto!")
                        android.util.Log.d("SettlementDetail", "Verificar se as mesas foram salvas corretamente no banco")
                    } else if (acertoMesas.size.toDouble() != acerto.totalMesas) {
                        android.util.Log.d("SettlementDetail", "⚠️ INCONSISTÊNCIA: Quantidade de mesas não confere!")
                        android.util.Log.d("SettlementDetail", "Esperado: ${acerto.totalMesas}, Encontrado: ${acertoMesas.size}")
                    }
                    
                    acertoMesas.forEachIndexed { index, acertoMesa ->
                        android.util.Log.d("SettlementDetail", "=== MESA ${index + 1} DETALHADA ===")
                        android.util.Log.d("SettlementDetail", "Mesa ID: ${acertoMesa.mesaId}")
                        android.util.Log.d("SettlementDetail", "Acerto ID vinculado: ${acertoMesa.acertoId}")
                        android.util.Log.d("SettlementDetail", "Relógio inicial: ${acertoMesa.relogioInicial}")
                        android.util.Log.d("SettlementDetail", "Relógio final: ${acertoMesa.relogioFinal}")
                        android.util.Log.d("SettlementDetail", "Fichas jogadas: ${acertoMesa.fichasJogadas}")
                        android.util.Log.d("SettlementDetail", "Valor fixo: R$ ${acertoMesa.valorFixo}")
                        android.util.Log.d("SettlementDetail", "Subtotal: R$ ${acertoMesa.subtotal}")
                        android.util.Log.d("SettlementDetail", "Com defeito: ${acertoMesa.comDefeito}")
                        android.util.Log.d("SettlementDetail", "Relógio reiniciou: ${acertoMesa.relogioReiniciou}")
                        // ✅ NOVO: Logs específicos para fotos
                        android.util.Log.d("SettlementDetail", "Foto relógio final: ${acertoMesa.fotoRelogioFinal}")
                        android.util.Log.d("SettlementDetail", "Data da foto: ${acertoMesa.dataFoto}")
                        android.util.Log.d("SettlementDetail", "Foto é nula? ${acertoMesa.fotoRelogioFinal == null}")
                        android.util.Log.d("SettlementDetail", "Foto está vazia? ${acertoMesa.fotoRelogioFinal?.isEmpty()}")
                    }
                    
                    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                    val dataFormatada = formatter.format(acerto.dataAcerto)
                    
                    // ✅ CORREÇÃO: Logs detalhados para debug das observações nos detalhes
                    android.util.Log.d("SettlementDetail", "=== CARREGANDO DETALHES - DEBUG OBSERVAÇÕES ===")
                    android.util.Log.d("SettlementDetail", "Acerto ID: ${acerto.id}")
                    android.util.Log.d("SettlementDetail", "Observação no banco: '${acerto.observacoes}'")
                    android.util.Log.d("SettlementDetail", "Observação é nula? ${acerto.observacoes == null}")
                    android.util.Log.d("SettlementDetail", "Observação é vazia? ${acerto.observacoes?.isEmpty()}")
                    
                    // ✅ CORREÇÃO: Garantir que observação seja exibida corretamente
                    val observacoes = acerto.observacoes
                    val observacaoParaExibir = when {
                        observacoes.isNullOrBlank() -> "Nenhuma observação registrada."
                        else -> observacoes.trim()
                    }
                    
                    android.util.Log.d("SettlementDetail", "Observação que será exibida: '$observacaoParaExibir'")

                    // ✅ CORREÇÃO: Processar métodos de pagamento
                    val metodosPagamento: Map<String, Double> = try {
                        acerto.metodosPagamentoJson?.let {
                            Gson().fromJson(it, object : TypeToken<Map<String, Double>>() {}.type)
                        } ?: emptyMap()
                    } catch (e: Exception) {
                        android.util.Log.d("SettlementDetail", "Erro ao parsear métodos de pagamento: ${e.message}")
                        emptyMap()
                    }
                    
                    android.util.Log.d("SettlementDetail", "=== TODOS OS DADOS CARREGADOS ===")
                    android.util.Log.d("SettlementDetail", "Representante: '${acerto.representante}'")
                    android.util.Log.d("SettlementDetail", "Tipo de acerto: '${acerto.tipoAcerto}'")
                    android.util.Log.d("SettlementDetail", "Pano trocado: ${acerto.panoTrocado}")
                    android.util.Log.d("SettlementDetail", "Número do pano: '${acerto.numeroPano}'")
                    android.util.Log.d("SettlementDetail", "Métodos de pagamento: $metodosPagamento")
                    android.util.Log.d("SettlementDetail", "Data finalização: ${acerto.dataFinalizacao}")

                    // ✅ CORREÇÃO CRÍTICA: Sempre carregar dados do cliente para garantir dados completos
                    val cliente = clienteRepository.obterPorId(acerto.clienteId)
                    val clienteNome = cliente?.nome ?: "Cliente #${acerto.clienteId}"
                    val clienteTelefone = cliente?.telefone
                    val clienteCpf = cliente?.cpfCnpj
                    // ✅ CORREÇÃO: Sempre usar valorFicha do cliente (fonte de verdade)
                    val valorFichaCliente = cliente?.valorFicha ?: 0.0
                    val comissaoFichaCliente = cliente?.comissaoFicha ?: 0.0
                    
                    android.util.Log.d("SettlementDetail", "=== DADOS DO CLIENTE CARREGADOS ===")
                    android.util.Log.d("SettlementDetail", "Cliente encontrado: ${cliente != null}")
                    android.util.Log.d("SettlementDetail", "Nome: '$clienteNome'")
                    android.util.Log.d("SettlementDetail", "Telefone: '$clienteTelefone'")
                    android.util.Log.d("SettlementDetail", "CPF: '$clienteCpf'")
                    android.util.Log.d("SettlementDetail", "ValorFicha: $valorFichaCliente")
                    android.util.Log.d("SettlementDetail", "ComissaoFicha: $comissaoFichaCliente")
                    
                    android.util.Log.d("SettlementDetail", "=== DADOS DO CLIENTE ===")
                    android.util.Log.d("SettlementDetail", "Cliente ID: ${acerto.clienteId}")
                    android.util.Log.d("SettlementDetail", "Nome do cliente: $clienteNome")
                    android.util.Log.d("SettlementDetail", "Telefone do cliente: $clienteTelefone")
                    
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
                        dataFinalizacao = acerto.dataFinalizacao,
                        // ✅ NOVOS DADOS: Dados do cliente
                        clienteNome = clienteNome,
                        clienteTelefone = clienteTelefone,
                        clienteCpf = clienteCpf,
                        valorFicha = valorFichaCliente,
                        comissaoFicha = comissaoFichaCliente,
                        dataAcerto = acerto.dataAcerto
                    )
                    
                    _settlementDetail.value = settlementDetail
                    android.util.Log.d("SettlementDetail", "Detalhes convertidos: $settlementDetail")
                } else {
                    android.util.Log.d("SettlementDetail", "Acerto não encontrado para ID: $acertoId")
                    _settlementDetail.value = null
                }
            } catch (e: Exception) {
                logError("SETTLEMENT_DETAIL", "Erro ao carregar detalhes do acerto: ${e.message}", e)
                showError("Erro ao carregar detalhes: ${e.message}", e)
                _settlementDetail.value = null
            } finally {
                hideLoading()
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
        val dataFinalizacao: Date?,
        // ✅ NOVOS CAMPOS: Dados do cliente
        val clienteNome: String,
        val clienteTelefone: String?,
        val clienteCpf: String?,
        val valorFicha: Double,
        val comissaoFicha: Double,
        // ✅ CORREÇÃO CRÍTICA: Adicionar dataAcerto que estava faltando
        val dataAcerto: Date
    )

    /**
     * ✅ NOVA FUNCIONALIDADE: Busca um acerto por ID
     */
    suspend fun buscarAcertoPorId(acertoId: Long): Acerto? {
        return acertoRepository.buscarPorId(acertoId)
    }
    
    /**
     * ✅ NOVO: Busca o contrato ativo do cliente para exibir no recibo
     */
    suspend fun buscarContratoAtivoPorCliente(clienteId: Long): com.example.gestaobilhares.data.entities.ContratoLocacao? {
        return try {
            clienteRepository.buscarContratoAtivoPorCliente(clienteId)
        } catch (e: Exception) {
            android.util.Log.d("SettlementDetailViewModel", "Erro ao buscar contrato ativo do cliente: ${e.message}")
            null
        }
    }
} 
