package com.example.gestaobilhares.ui.settlement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repositories.ClienteRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.entities.Acerto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ViewModel para SettlementFragment
 * FASE 4A - Implementação básica para desbloqueio
 */
class SettlementViewModel(
    private val mesaRepository: MesaRepository,
    private val clienteRepository: ClienteRepository,
    private val acertoRepository: AcertoRepository,
    private val acertoMesaRepository: AcertoMesaRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _clientName = MutableStateFlow("")
    val clientName: StateFlow<String> = _clientName.asStateFlow()

    private val _clientAddress = MutableStateFlow("")
    val clientAddress: StateFlow<String> = _clientAddress.asStateFlow()

    private val _mesasCliente = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCliente: StateFlow<List<Mesa>> = _mesasCliente.asStateFlow()

    private val _resultadoSalvamento = MutableStateFlow<Result<Long>?>(null)
    val resultadoSalvamento: StateFlow<Result<Long>?> = _resultadoSalvamento.asStateFlow()

    private val _historicoAcertos = MutableStateFlow<List<Acerto>>(emptyList())
    val historicoAcertos: StateFlow<List<Acerto>> = _historicoAcertos.asStateFlow()

    private val _debitoAnterior = MutableStateFlow(0.0)
    val debitoAnterior: StateFlow<Double> = _debitoAnterior.asStateFlow()




    data class DadosAcerto(
        val mesas: List<Mesa>,
        val representante: String,
        val panoTrocado: Boolean,
        val numeroPano: String?,
        val tipoAcerto: String,
        val observacao: String,
        val justificativa: String?,
        val metodosPagamento: Map<String, Double>
    )

    fun loadClientForSettlement(clienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cliente = clienteRepository.obterPorId(clienteId)
                if (cliente != null) {
                    _clientName.value = cliente.nome
                    _clientAddress.value = cliente.endereco ?: "---"
                    Log.d("SettlementViewModel", "Nome do cliente carregado: ${cliente.nome}, endereço: ${cliente.endereco}")
                } else {
                    _clientName.value = "Cliente não encontrado"
                    _clientAddress.value = "---"
                    Log.d("SettlementViewModel", "Cliente não encontrado para ID: $clienteId")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _clientName.value = "Erro ao carregar cliente"
                _clientAddress.value = "---"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Prepara as mesas para acerto, definindo relógios iniciais baseados no último acerto
     */
    suspend fun prepararMesasParaAcerto(mesasCliente: List<Mesa>): List<Mesa> {
        return mesasCliente.map { mesa ->
            try {
                // Buscar o último acerto desta mesa
                val ultimoAcertoMesa = acertoMesaRepository.buscarUltimoAcertoMesa(mesa.id)
                
                if (ultimoAcertoMesa != null) {
                    // Usar o relógio final do último acerto como inicial do próximo
                    mesa.copy(fichasInicial = ultimoAcertoMesa.relogioFinal)
                } else {
                    // Primeiro acerto - usar relógio inicial cadastrado ou 0
                    mesa.copy(fichasInicial = mesa.fichasInicial ?: 0)
                }
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao preparar mesa ${mesa.numero}: ${e.message}")
                mesa.copy(fichasInicial = mesa.fichasInicial ?: 0)
            }
        }
    }

    fun carregarDadosCliente(clienteId: Long, callback: (com.example.gestaobilhares.data.entities.Cliente?) -> Unit) {
        viewModelScope.launch {
            try {
                val cliente = clienteRepository.obterPorId(clienteId)
                callback(cliente)
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao carregar dados do cliente: ${e.localizedMessage}", e)
                callback(null)
            }
        }
    }

    fun loadMesasCliente(clienteId: Long) {
        viewModelScope.launch {
            mesaRepository.obterMesasPorCliente(clienteId).collect { mesas ->
                _mesasCliente.value = mesas
            }
        }
    }

    /**
     * ✅ FUNÇÃO FALLBACK: Carrega mesas diretamente sem usar Flow
     */
    suspend fun carregarMesasClienteDireto(clienteId: Long): List<Mesa> {
        return try {
            Log.d("SettlementViewModel", "Carregando mesas diretamente para cliente $clienteId")
            mesaRepository.obterMesasPorClienteDireto(clienteId)
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao carregar mesas direto: ${e.message}")
            emptyList()
        }
    }

    fun carregarHistoricoAcertos(clienteId: Long) {
        viewModelScope.launch {
            acertoRepository.buscarPorCliente(clienteId).collect { acertos ->
                _historicoAcertos.value = acertos
            }
        }
    }

    /**
     * Busca o débito atual do último acerto do cliente para usar como débito anterior
     */
    fun buscarDebitoAnterior(clienteId: Long) {
        viewModelScope.launch {
            try {
                val ultimoAcerto = acertoRepository.buscarUltimoAcertoPorCliente(clienteId)
                if (ultimoAcerto != null) {
                    _debitoAnterior.value = ultimoAcerto.debitoAtual
                    Log.d("SettlementViewModel", "Débito anterior carregado: R$ ${ultimoAcerto.debitoAtual}")
                } else {
                    _debitoAnterior.value = 0.0
                    Log.d("SettlementViewModel", "Nenhum acerto anterior encontrado, débito anterior: R$ 0,00")
                }
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao buscar débito anterior: ${e.message}")
                _debitoAnterior.value = 0.0
            }
        }
    }





    /**
     * Salva o acerto, agora recebendo os valores discriminados por método de pagamento.
     * @param clienteId ID do cliente
     * @param dadosAcerto Dados principais do acerto
     * @param metodosPagamento Mapa de método para valor recebido
     * @param desconto Valor do desconto aplicado
     */
    fun salvarAcerto(clienteId: Long, dadosAcerto: DadosAcerto, metodosPagamento: Map<String, Double>, desconto: Double = 0.0) {
        viewModelScope.launch {
            try {
                Log.d("SettlementViewModel", "Salvando acerto com clienteId=$clienteId, mesas=${dadosAcerto.mesas.map { it.numero }}")
                
                // Calcular valores do acerto
                val valorRecebido = metodosPagamento.values.sum()
                val debitoAnterior = _debitoAnterior.value
                val valorTotal = dadosAcerto.mesas.sumOf { mesa ->
                    if (mesa.valorFixo > 0) {
                        mesa.valorFixo // Mesa de valor fixo
                    } else {
                        // Mesa de fichas jogadas - calcular baseado no cliente
                        val cliente = clienteRepository.obterPorId(clienteId)
                        val fichasJogadas = (mesa.fichasFinal - mesa.fichasInicial).coerceAtLeast(0)
                        fichasJogadas * (cliente?.comissaoFicha ?: 0.0)
                    }
                }
                // ✅ CORREÇÃO: Usar o desconto passado como parâmetro
                val valorComDesconto = valorTotal - desconto
                val debitoAtual = debitoAnterior + valorComDesconto - valorRecebido
                
                // ✅ CORREÇÃO: Logs detalhados para debug do cálculo do débito
                Log.d("SettlementViewModel", "=== CÁLCULO DO DÉBITO ATUAL ===")
                Log.d("SettlementViewModel", "Débito anterior: R$ $debitoAnterior")
                Log.d("SettlementViewModel", "Valor total das mesas: R$ $valorTotal")
                Log.d("SettlementViewModel", "Desconto aplicado: R$ $desconto")
                Log.d("SettlementViewModel", "Valor com desconto: R$ $valorComDesconto")
                Log.d("SettlementViewModel", "Valor recebido: R$ $valorRecebido")
                Log.d("SettlementViewModel", "Débito atual calculado: R$ $debitoAtual")
                Log.d("SettlementViewModel", "Fórmula: $debitoAnterior + $valorComDesconto - $valorRecebido = $debitoAtual")
                
                val metodosPagamentoJson = Gson().toJson(metodosPagamento)
                // ✅ CORREÇÃO: Logs detalhados para debug das observações
                Log.d("SettlementViewModel", "=== SALVANDO ACERTO NO BANCO - DEBUG OBSERVAÇÕES ===")
                Log.d("SettlementViewModel", "Observação recebida dos dados: '${dadosAcerto.observacao}'")
                Log.d("SettlementViewModel", "Observação é nula? ${dadosAcerto.observacao == null}")
                Log.d("SettlementViewModel", "Observação é vazia? ${dadosAcerto.observacao?.isEmpty()}")
                Log.d("SettlementViewModel", "Observação é blank? ${dadosAcerto.observacao?.isBlank()}")
                
                // ✅ CORREÇÃO: Garantir que observação nunca seja nula ou vazia
                val observacaoParaSalvar = if (dadosAcerto.observacao.isNullOrBlank()) {
                    "Acerto realizado via app"
                } else {
                    dadosAcerto.observacao.trim()
                }
                
                Log.d("SettlementViewModel", "Observação que será salva no banco: '$observacaoParaSalvar'")

                // ✅ CORREÇÃO: Criar dados extras JSON para campos adicionais
                val dadosExtras = mapOf(
                    "justificativa" to dadosAcerto.justificativa,
                    "versaoApp" to "1.0.0"
                )
                val dadosExtrasJson = Gson().toJson(dadosExtras)
                
                Log.d("SettlementViewModel", "=== SALVANDO TODOS OS DADOS ===")
                Log.d("SettlementViewModel", "Representante: '${dadosAcerto.representante}'")
                Log.d("SettlementViewModel", "Tipo de acerto: '${dadosAcerto.tipoAcerto}'")
                Log.d("SettlementViewModel", "Pano trocado: ${dadosAcerto.panoTrocado}")
                Log.d("SettlementViewModel", "Número do pano: '${dadosAcerto.numeroPano}'")
                Log.d("SettlementViewModel", "Métodos de pagamento: $metodosPagamento")

                val acerto = Acerto(
                    clienteId = clienteId,
                    colaboradorId = null, // ✅ CORREÇÃO: Usar null para evitar foreign key constraint
                    periodoInicio = java.util.Date(),
                    periodoFim = java.util.Date(),
                    totalMesas = dadosAcerto.mesas.size.toDouble(),
                    debitoAnterior = debitoAnterior,
                    valorTotal = valorTotal,
                    desconto = desconto,
                    valorComDesconto = valorComDesconto,
                    valorRecebido = valorRecebido,
                    debitoAtual = debitoAtual,
                    status = com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO,
                    observacoes = observacaoParaSalvar,
                    dataFinalizacao = java.util.Date(), // ✅ CORREÇÃO: Preencher data de finalização
                    metodosPagamentoJson = metodosPagamentoJson,
                    // ✅ NOVOS CAMPOS: Resolver problema de dados perdidos
                    representante = dadosAcerto.representante,
                    tipoAcerto = dadosAcerto.tipoAcerto,
                    panoTrocado = dadosAcerto.panoTrocado,
                    numeroPano = dadosAcerto.numeroPano,
                    dadosExtrasJson = dadosExtrasJson
                )
                
                val acertoId = acertoRepository.inserir(acerto)
                Log.d("SettlementViewModel", "✅ Acerto salvo com ID: $acertoId")
                Log.d("SettlementViewModel", "✅ Observações CONFIRMADAS no banco: '$observacaoParaSalvar'")
                
                // ✅ CORREÇÃO: Verificar se realmente foi salvo
                val acertoSalvo = acertoRepository.buscarPorId(acertoId)
                Log.d("SettlementViewModel", "🔍 VERIFICAÇÃO: Observação no banco após salvamento: '${acertoSalvo?.observacoes}'")
                
                // Salvar dados detalhados de cada mesa do acerto
                val cliente = clienteRepository.obterPorId(clienteId)
                val acertoMesas = dadosAcerto.mesas.map { mesa ->
                    val fichasJogadas = if (mesa.valorFixo > 0) {
                        0 // Mesa de valor fixo não tem fichas jogadas
                    } else {
                        (mesa.fichasFinal - mesa.fichasInicial).coerceAtLeast(0)
                    }
                    
                    val subtotal = if (mesa.valorFixo > 0) {
                        mesa.valorFixo
                    } else {
                        fichasJogadas * (cliente?.comissaoFicha ?: 0.0)
                    }
                    
                    com.example.gestaobilhares.data.entities.AcertoMesa(
                        acertoId = acertoId,
                        mesaId = mesa.id,
                        relogioInicial = mesa.fichasInicial ?: 0,
                        relogioFinal = mesa.fichasFinal ?: 0,
                        fichasJogadas = fichasJogadas,
                        valorFixo = mesa.valorFixo ?: 0.0,
                        valorFicha = cliente?.valorFicha ?: 0.0,
                        comissaoFicha = cliente?.comissaoFicha ?: 0.0,
                        subtotal = subtotal,
                        comDefeito = false, // TODO: pegar do formulário
                        observacoes = null
                    )
                }
                
                acertoMesaRepository.inserirLista(acertoMesas)
                Log.d("SettlementViewModel", "Dados de ${acertoMesas.size} mesas salvos para o acerto $acertoId")
                
                // ✅ CRÍTICO: Atualizar o débito atual na tabela de clientes
                clienteRepository.atualizarDebitoAtual(clienteId, debitoAtual)
                Log.d("SettlementViewModel", "Débito atual atualizado na tabela clientes: R$ $debitoAtual")
                
                // ✅ NOVO: Verificar se a atualização foi bem-sucedida
                val clienteAtualizado = clienteRepository.obterPorId(clienteId)
                Log.d("SettlementViewModel", "🔍 VERIFICAÇÃO: Débito atual na tabela clientes após atualização: R$ ${clienteAtualizado?.debitoAtual}")
                
                _resultadoSalvamento.value = Result.success(acertoId)
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao salvar acerto: ${e.localizedMessage}", e)
                _resultadoSalvamento.value = Result.failure(e)
            }
        }
    }

    fun resetarResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }
    
    fun limparResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }

    suspend fun buscarAcertoPorId(acertoId: Long): Acerto? {
        return acertoRepository.buscarPorId(acertoId)
    }

    suspend fun buscarMesasDoAcerto(acertoId: Long): List<com.example.gestaobilhares.data.entities.AcertoMesa> {
        return acertoMesaRepository.buscarPorAcertoId(acertoId)
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
} 