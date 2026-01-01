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
import timber.log.Timber

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettlementDetailViewModel @Inject constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {

    private val _settlementDetail = MutableLiveData<SettlementDetail?>()
    val settlementDetail: LiveData<SettlementDetail?> = _settlementDetail

    // Estados de loading e error já estão no BaseViewModel

    fun loadSettlementDetails(acertoId: Long) {
        viewModelScope.launch {
            showLoading()
            logOperation("SETTLEMENT_DETAIL", "Carregando detalhes do acerto ID: $acertoId")
            
            try {
                val acerto = appRepository.obterAcertoPorId(acertoId)
                if (acerto != null) {
                    Timber.tag("SettlementDetail").d("✅ Acerto encontrado")
                    Timber.tag("SettlementDetail").d("Cliente ID: ${acerto.clienteId}")
                    Timber.tag("SettlementDetail").d("Total mesas esperadas: ${acerto.totalMesas}")
                    Timber.tag("SettlementDetail").d("Valor total: R$ ${acerto.valorTotal}")
                    
                    // ✅ CORREÇÃO CRÍTICA: Buscar dados detalhados por mesa com logs extensos
                    Timber.tag("SettlementDetail").d("=== INICIANDO BUSCA DAS MESAS ===")
                    val acertoMesas = appRepository.buscarAcertoMesasPorAcerto(acertoId)
                    
                    Timber.tag("SettlementDetail").d("=== RESULTADO DA BUSCA ===")
                    Timber.tag("SettlementDetail").d("Acerto ID pesquisado: $acertoId")
                    Timber.tag("SettlementDetail").d("Quantidade de mesas encontradas: ${acertoMesas.size}")
                    Timber.tag("SettlementDetail").d("Quantidade esperada (totalMesas): ${acerto.totalMesas}")
                    
                    if (acertoMesas.isEmpty()) {
                        Timber.tag("SettlementDetail").d("❌ PROBLEMA CRÍTICO: Nenhuma mesa encontrada para o acerto!")
                        Timber.tag("SettlementDetail").d("Verificar se as mesas foram salvas corretamente no banco")
                    } else if (acertoMesas.size.toDouble() != acerto.totalMesas) {
                        Timber.tag("SettlementDetail").d("⚠️ INCONSISTÊNCIA: Quantidade de mesas não confere!")
                        Timber.tag("SettlementDetail").d("Esperado: ${acerto.totalMesas}, Encontrado: ${acertoMesas.size}")
                    }
                    
                    acertoMesas.forEachIndexed { index, acertoMesa ->
                        Timber.tag("SettlementDetail").d("=== MESA ${index + 1} DETALHADA ===")
                        Timber.tag("SettlementDetail").d("Mesa ID: ${acertoMesa.mesaId}")
                        Timber.tag("SettlementDetail").d("Acerto ID vinculado: ${acertoMesa.acertoId}")
                        Timber.tag("SettlementDetail").d("Relógio inicial: ${acertoMesa.relogioInicial}")
                        Timber.tag("SettlementDetail").d("Relógio final: ${acertoMesa.relogioFinal}")
                        Timber.tag("SettlementDetail").d("Fichas jogadas: ${acertoMesa.fichasJogadas}")
                        Timber.tag("SettlementDetail").d("Valor fixo: R$ ${acertoMesa.valorFixo}")
                        Timber.tag("SettlementDetail").d("Subtotal: R$ ${acertoMesa.subtotal}")
                        Timber.tag("SettlementDetail").d("Com defeito: ${acertoMesa.comDefeito}")
                        Timber.tag("SettlementDetail").d("Relógio reiniciou: ${acertoMesa.relogioReiniciou}")
                        // ✅ NOVO: Logs específicos para fotos
                        Timber.tag("SettlementDetail").d("Foto relógio final: ${acertoMesa.fotoRelogioFinal}")
                        Timber.tag("SettlementDetail").d("Data da foto: ${acertoMesa.dataFoto}")
                        Timber.tag("SettlementDetail").d("Foto é nula? ${acertoMesa.fotoRelogioFinal == null}")
                        Timber.tag("SettlementDetail").d("Foto está vazia? ${acertoMesa.fotoRelogioFinal?.isEmpty()}")
                    }
                    
                    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                    val dataFormatada = formatter.format(acerto.dataAcerto)
                    
                    // ✅ CORREÇÃO: Logs detalhados para debug das observações nos detalhes
                    Timber.tag("SettlementDetail").d("=== CARREGANDO DETALHES - DEBUG OBSERVAÇÕES ===")
                    Timber.tag("SettlementDetail").d("Acerto ID: ${acerto.id}")
                    Timber.tag("SettlementDetail").d("Observação no banco: '${acerto.observacoes}'")
                    Timber.tag("SettlementDetail").d("Observação é nula? ${acerto.observacoes == null}")
                    Timber.tag("SettlementDetail").d("Observação é vazia? ${acerto.observacoes?.isEmpty()}")
                    
                    // ✅ CORREÇÃO: Garantir que observação seja exibida corretamente
                    val observacoes = acerto.observacoes
                    val observacaoParaExibir = when {
                        observacoes.isNullOrBlank() -> "Nenhuma observação registrada."
                        else -> observacoes.trim()
                    }
                    
                    Timber.tag("SettlementDetail").d("Observação que será exibida: '$observacaoParaExibir'")

                    // ✅ CORREÇÃO: Processar métodos de pagamento
                    val metodosPagamento: Map<String, Double> = try {
                        acerto.metodosPagamentoJson?.let {
                            Gson().fromJson(it, object : TypeToken<Map<String, Double>>() {}.type)
                        } ?: emptyMap()
                    } catch (e: Exception) {
                        Timber.tag("SettlementDetail").d("Erro ao parsear métodos de pagamento: ${e.message}")
                        emptyMap()
                    }
                    
                    Timber.tag("SettlementDetail").d("=== TODOS OS DADOS CARREGADOS ===")
                    Timber.tag("SettlementDetail").d("Representante: '${acerto.representante}'")
                    Timber.tag("SettlementDetail").d("Tipo de acerto: '${acerto.tipoAcerto}'")
                    Timber.tag("SettlementDetail").d("Pano trocado: ${acerto.panoTrocado}")
                    Timber.tag("SettlementDetail").d("Número do pano: '${acerto.numeroPano}'")
                    Timber.tag("SettlementDetail").d("Métodos de pagamento: $metodosPagamento")
                    Timber.tag("SettlementDetail").d("Data finalização: ${acerto.dataFinalizacao}")

                    // ✅ CORREÇÃO CRÍTICA: Sempre carregar dados do cliente para garantir dados completos
                    val cliente = appRepository.obterClientePorId(acerto.clienteId)
                    val clienteNome = cliente?.nome ?: "Cliente #${acerto.clienteId}"
                    val clienteTelefone = cliente?.telefone
                    val clienteCpf = cliente?.cpfCnpj
                    // ✅ CORREÇÃO: Sempre usar valorFicha do cliente (fonte de verdade)
                    val valorFichaCliente = cliente?.valorFicha ?: 0.0
                    val comissaoFichaCliente = cliente?.comissaoFicha ?: 0.0
                    
                    Timber.tag("SettlementDetail").d("=== DADOS DO CLIENTE CARREGADOS ===")
                    Timber.tag("SettlementDetail").d("Cliente encontrado: ${cliente != null}")
                    Timber.tag("SettlementDetail").d("Nome: '$clienteNome'")
                    Timber.tag("SettlementDetail").d("Telefone: '$clienteTelefone'")
                    Timber.tag("SettlementDetail").d("CPF: '$clienteCpf'")
                    Timber.tag("SettlementDetail").d("ValorFicha: $valorFichaCliente")
                    Timber.tag("SettlementDetail").d("ComissaoFicha: $comissaoFichaCliente")
                    
                    Timber.tag("SettlementDetail").d("=== DADOS DO CLIENTE ===")
                    Timber.tag("SettlementDetail").d("Cliente ID: ${acerto.clienteId}")
                    Timber.tag("SettlementDetail").d("Nome do cliente: $clienteNome")
                    Timber.tag("SettlementDetail").d("Telefone do cliente: $clienteTelefone")
                    
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
                        dataFinalizacao = acerto.dataFinalizacao?.let { Date(it) },
                        // ✅ NOVOS DADOS: Dados do cliente
                        clienteNome = clienteNome,
                        clienteTelefone = clienteTelefone,
                        clienteCpf = clienteCpf,
                        valorFicha = valorFichaCliente,
                        comissaoFicha = comissaoFichaCliente,
                        dataAcerto = Date(acerto.dataAcerto)
                    )
                    
                    _settlementDetail.value = settlementDetail
                    Timber.tag("SettlementDetail").d("Detalhes convertidos: $settlementDetail")
                } else {
                    Timber.tag("SettlementDetail").d("Acerto não encontrado para ID: $acertoId")
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
        return appRepository.obterAcertoPorId(acertoId)
    }
    
    /**
     * ✅ NOVO: Busca o contrato ativo do cliente para exibir no recibo
     */
    suspend fun buscarContratoAtivoPorCliente(clienteId: Long): com.example.gestaobilhares.data.entities.ContratoLocacao? {
        return try {
            appRepository.buscarContratoAtivoPorCliente(clienteId)
        } catch (e: Exception) {
            Timber.tag("SettlementDetailViewModel").d("Erro ao buscar contrato ativo do cliente: ${e.message}")
            null
        }
    }
} 
