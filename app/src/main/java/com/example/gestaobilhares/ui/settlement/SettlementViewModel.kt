package com.example.gestaobilhares.ui.settlement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.entities.Acerto
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import com.example.gestaobilhares.data.repository.HistoricoManutencaoMesaRepository
import com.example.gestaobilhares.data.repository.PanoEstoqueRepository
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel para SettlementFragment
 * FASE 4A - Implementação básica para desbloqueio
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val mesaRepository: MesaRepository,
    private val clienteRepository: ClienteRepository,
    private val acertoRepository: AcertoRepository,
    private val acertoMesaRepository: AcertoMesaRepository,
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val historicoManutencaoMesaRepository: HistoricoManutencaoMesaRepository,
    private val panoEstoqueRepository: PanoEstoqueRepository
) : ViewModel() {

    /**
     * ✅ NOVA CLASSE: Resultado específico para salvamento de acerto
     */
    sealed class ResultadoSalvamento {
        data class Sucesso(val acertoId: Long) : ResultadoSalvamento()
        data class Erro(val mensagem: String) : ResultadoSalvamento()
        data class AcertoJaExiste(val acertoExistente: Acerto) : ResultadoSalvamento()
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _clientName = MutableStateFlow("")
    val clientName: StateFlow<String> = _clientName.asStateFlow()

    private val _clientAddress = MutableStateFlow("")
    val clientAddress: StateFlow<String> = _clientAddress.asStateFlow()

    private val _mesasCliente = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCliente: StateFlow<List<Mesa>> = _mesasCliente.asStateFlow()

    private val _resultadoSalvamento = MutableStateFlow<ResultadoSalvamento?>(null)
    val resultadoSalvamento: StateFlow<ResultadoSalvamento?> = _resultadoSalvamento.asStateFlow()

    private val _historicoAcertos = MutableStateFlow<List<Acerto>>(emptyList())
    val historicoAcertos: StateFlow<List<Acerto>> = _historicoAcertos.asStateFlow()

    private val _debitoAnterior = MutableStateFlow(0.0)
    val debitoAnterior: StateFlow<Double> = _debitoAnterior.asStateFlow()

    data class DadosAcerto(
        val mesas: List<MesaAcerto>,
        val representante: String,
        val panoTrocado: Boolean,
        val numeroPano: String?,
        val tipoAcerto: String,
        val observacao: String,
        val justificativa: String?,
        val metodosPagamento: Map<String, Double>
    )
    
    /**
     * ✅ NOVO: Classe específica para mesas no acerto, incluindo campo comDefeito
     */
    data class MesaAcerto(
        val id: Long,
        val numero: String,
        val fichasInicial: Int = 0,
        val fichasFinal: Int = 0,
        val valorFixo: Double = 0.0,
        val tipoMesa: com.example.gestaobilhares.data.entities.TipoMesa,
        val comDefeito: Boolean = false,
        val relogioReiniciou: Boolean = false,
        // ✅ NOVO: Campos para fotos
        val fotoRelogioFinal: String? = null,
        val dataFoto: java.util.Date? = null
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
     * ✅ FUNÇÃO CRÍTICA: Prepara as mesas para acerto, definindo relógios iniciais
     * @param mesasCliente Lista de mesas do cliente
     * @param acertoIdParaEdicao ID do acerto sendo editado (null se for novo acerto)
     */
    suspend fun prepararMesasParaAcerto(mesasCliente: List<Mesa>, acertoIdParaEdicao: Long? = null): List<Mesa> {
        Log.d("SettlementViewModel", "=== PREPARANDO MESAS PARA ACERTO ===")
        Log.d("SettlementViewModel", "Mesas recebidas: ${mesasCliente.size}, Modo edição: ${acertoIdParaEdicao != null}")
        
        return mesasCliente.map { mesa ->
            try {
                Log.d("SettlementViewModel", "Processando mesa ${mesa.numero} (ID: ${mesa.id})")
                
                if (acertoIdParaEdicao != null) {
                    // ✅ MODO EDIÇÃO: Carregar dados do acerto sendo editado
                    val acertoMesa = acertoMesaRepository.buscarAcertoMesaPorAcertoEMesa(acertoIdParaEdicao, mesa.id)
                    if (acertoMesa != null) {
                        // Usar o relógio inicial do acerto sendo editado
                        val relogioInicial = acertoMesa.relogioInicial
                        val relogioFinal = acertoMesa.relogioFinal
                        Log.d("SettlementViewModel", "Mesa ${mesa.numero}: MODO EDIÇÃO - relógio inicial: $relogioInicial, relógio final: $relogioFinal")
                        mesa.copy(
                            fichasInicial = relogioInicial,
                            fichasFinal = relogioFinal
                        )
                    } else {
                        // Fallback: usar dados da mesa
                        val relogioInicial = mesa.fichasInicial ?: 0
                        Log.d("SettlementViewModel", "Mesa ${mesa.numero}: MODO EDIÇÃO - acerto não encontrado, usando dados da mesa: $relogioInicial")
                        mesa.copy(fichasInicial = relogioInicial)
                    }
                } else {
                    // ✅ MODO NOVO ACERTO: Usar lógica original
                    val ultimoAcertoMesa = acertoMesaRepository.buscarUltimoAcertoMesa(mesa.id)
                    
                    if (ultimoAcertoMesa != null) {
                        // Usar o relógio final do último acerto como inicial do próximo
                        val relogioInicial = ultimoAcertoMesa.relogioFinal
                        Log.d("SettlementViewModel", "Mesa ${mesa.numero}: MODO NOVO ACERTO - relógio final: ${ultimoAcertoMesa.relogioFinal} -> novo relógio inicial: $relogioInicial")
                        mesa.copy(fichasInicial = relogioInicial)
                    } else {
                        // Primeiro acerto - usar relógio inicial cadastrado ou 0
                        val relogioInicial = mesa.fichasInicial ?: 0
                        Log.d("SettlementViewModel", "Mesa ${mesa.numero}: MODO NOVO ACERTO - primeiro acerto, usando relógio inicial cadastrado: $relogioInicial")
                        mesa.copy(fichasInicial = relogioInicial)
                    }
                }
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao preparar mesa ${mesa.numero}: ${e.message}")
                val relogioInicial = mesa.fichasInicial ?: 0
                mesa.copy(fichasInicial = relogioInicial)
            }
        }.also { mesasPreparadas ->
            Log.d("SettlementViewModel", "=== MESAS PREPARADAS ===")
            mesasPreparadas.forEach { mesa ->
                Log.d("SettlementViewModel", "Mesa ${mesa.numero}: relógio inicial=${mesa.fichasInicial}, relógio final=${mesa.fichasFinal}")
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
     * ✅ FUNÇÃO CRÍTICA: Busca o débito para usar como débito anterior
     * @param clienteId ID do cliente
     * @param acertoIdParaEdicao ID do acerto sendo editado (null se for novo acerto)
     */
    fun buscarDebitoAnterior(clienteId: Long, acertoIdParaEdicao: Long? = null) {
        viewModelScope.launch {
            try {
                if (acertoIdParaEdicao != null) {
                    // ✅ MODO EDIÇÃO: Usar débito ANTERIOR do acerto sendo editado (não o atual!)
                    val acertoParaEdicao = acertoRepository.buscarPorId(acertoIdParaEdicao)
                    if (acertoParaEdicao != null) {
                        _debitoAnterior.value = acertoParaEdicao.debitoAnterior
                        Log.d("SettlementViewModel", "MODO EDIÇÃO: Débito ANTERIOR do acerto sendo editado: R$ ${acertoParaEdicao.debitoAnterior}")
                    } else {
                        _debitoAnterior.value = 0.0
                        Log.w("SettlementViewModel", "MODO EDIÇÃO: Acerto para edição não encontrado, débito anterior: R$ 0,00")
                    }
                } else {
                    // ✅ MODO NOVO ACERTO: Usar débito do último acerto como anterior
                    val ultimoAcerto = acertoRepository.buscarUltimoAcertoPorCliente(clienteId)
                    if (ultimoAcerto != null) {
                        _debitoAnterior.value = ultimoAcerto.debitoAtual
                        Log.d("SettlementViewModel", "MODO NOVO ACERTO: Débito anterior carregado: R$ ${ultimoAcerto.debitoAtual}")
                    } else {
                        _debitoAnterior.value = 0.0
                        Log.d("SettlementViewModel", "MODO NOVO ACERTO: Nenhum acerto anterior encontrado, débito anterior: R$ 0,00")
                    }
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
     * @param acertoIdParaEdicao ID do acerto sendo editado (null se for novo acerto)
     */
    fun salvarAcerto(clienteId: Long, dadosAcerto: DadosAcerto, metodosPagamento: Map<String, Double>, desconto: Double = 0.0, acertoIdParaEdicao: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("SettlementViewModel", "Salvando acerto com clienteId=$clienteId, mesas=${dadosAcerto.mesas.map { it.numero }}, modoEdicao=${acertoIdParaEdicao != null}")
                
                // Buscar cliente uma única vez
                val cliente = clienteRepository.obterPorId(clienteId) ?: throw IllegalStateException("Cliente não encontrado para o ID: $clienteId")
                val rotaId = cliente.rotaId
                
                // Buscar ciclo atual da rota
                val cicloAtivo = cicloAcertoRepository.buscarCicloAtivo(rotaId)
                val cicloId = cicloAtivo?.id ?: run {
                    Log.w("SettlementViewModel", "Nenhum ciclo ativo encontrado para a rota $rotaId. Tentando buscar o último ciclo existente.")
                    val ultimoCiclo = cicloAcertoRepository.buscarEstatisticasRota(rotaId)
                    ultimoCiclo?.id ?: throw IllegalStateException("Nenhum ciclo encontrado para a rota $rotaId.")
                }
                
                android.util.Log.d("DEBUG_DIAG", "[SALVAR_ACERTO] cicloId usado: $cicloId | rotaId: $rotaId | status ciclo ativo: ${cicloAtivo?.status} | modoEdicao: ${acertoIdParaEdicao != null}")

                // ✅ CORREÇÃO: Validação apenas para novos acertos (não para edição)
                if (acertoIdParaEdicao == null) {
                    val acertoExistente = acertoRepository.verificarAcertoExistente(clienteId, cicloId)
                    if (acertoExistente != null) {
                        Log.w("SettlementViewModel", "⚠️ ACERTO JÁ EXISTE: Cliente $clienteId já possui acerto (ID: ${acertoExistente.id}) no ciclo $cicloId")
                        _resultadoSalvamento.value = ResultadoSalvamento.AcertoJaExiste(acertoExistente)
                        _isLoading.value = false
                        return@launch
                    }
                } else {
                    Log.d("SettlementViewModel", "✅ Modo edição ativo (acertoId: $acertoIdParaEdicao). Pulando validação de acerto único.")
                }

                // Calcular valores do acerto
                val valorRecebido = metodosPagamento.values.sum()
                val debitoAnterior = _debitoAnterior.value
                val valorTotal = dadosAcerto.mesas.sumOf { mesa ->
                    if (mesa.valorFixo > 0) {
                        mesa.valorFixo
                    } else {
                        val fichasJogadas = (mesa.fichasFinal - mesa.fichasInicial).coerceAtLeast(0)
                        fichasJogadas * (cliente.comissaoFicha)
                    }
                }
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

                // ✅ CORREÇÃO CRÍTICA: Vínculos com rota e ciclo
                android.util.Log.d("SettlementViewModel", "=== VINCULANDO ACERTO À ROTA E CICLO ===")
                android.util.Log.d("SettlementViewModel", "Cliente ID: $clienteId")
                android.util.Log.d("SettlementViewModel", "Rota ID do cliente: $rotaId")
                android.util.Log.d("SettlementViewModel", "Ciclo atual: $cicloId")
                
                // ✅ CORREÇÃO: Lógica diferente para edição vs. novo acerto
                val acertoId: Long
                if (acertoIdParaEdicao != null) {
                    // MODO EDIÇÃO: Atualizar acerto existente
                    Log.d("SettlementViewModel", "🔄 MODO EDIÇÃO: Atualizando acerto existente ID: $acertoIdParaEdicao")
                    
                    // Buscar acerto existente
                    val acertoExistente = acertoRepository.buscarPorId(acertoIdParaEdicao)
                    if (acertoExistente == null) {
                        Log.e("SettlementViewModel", "❌ Acerto para edição não encontrado: ID $acertoIdParaEdicao")
                        _resultadoSalvamento.value = ResultadoSalvamento.Erro("Acerto para edição não encontrado")
                        _isLoading.value = false
                        return@launch
                    }
                    
                    // Atualizar dados do acerto existente
                    val acertoAtualizado = acertoExistente.copy(
                        totalMesas = dadosAcerto.mesas.size.toDouble(),
                        debitoAnterior = debitoAnterior,
                        valorTotal = valorTotal,
                        desconto = desconto,
                        valorComDesconto = valorComDesconto,
                        valorRecebido = valorRecebido,
                        debitoAtual = debitoAtual,
                        observacoes = observacaoParaSalvar,
                        dataFinalizacao = java.util.Date(),
                        metodosPagamentoJson = metodosPagamentoJson,
                        representante = dadosAcerto.representante,
                        tipoAcerto = dadosAcerto.tipoAcerto,
                        panoTrocado = dadosAcerto.panoTrocado,
                        numeroPano = dadosAcerto.numeroPano,
                        dadosExtrasJson = dadosExtrasJson
                    )
                    
                    acertoRepository.atualizar(acertoAtualizado)
                    acertoId = acertoIdParaEdicao
                    Log.d("SettlementViewModel", "✅ Acerto atualizado com sucesso! ID: $acertoId")
                    
                } else {
                    // MODO NOVO ACERTO: Criar novo acerto
                    Log.d("SettlementViewModel", "🆕 MODO NOVO ACERTO: Criando novo acerto")
                    
                    val acerto = Acerto(
                        clienteId = clienteId,
                        colaboradorId = null,
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
                        dataFinalizacao = java.util.Date(),
                        metodosPagamentoJson = metodosPagamentoJson,
                        representante = dadosAcerto.representante,
                        tipoAcerto = dadosAcerto.tipoAcerto,
                        panoTrocado = dadosAcerto.panoTrocado,
                        numeroPano = dadosAcerto.numeroPano,
                        dadosExtrasJson = dadosExtrasJson,
                        rotaId = rotaId,
                        cicloId = cicloId
                    )
                    
                    acertoId = acertoRepository.salvarAcerto(acerto)
                    Log.d("SettlementViewModel", "✅ Novo acerto salvo com ID: $acertoId")
                }
                
                // NOVO: Atualizar valores do ciclo após salvar acerto
                // Buscar todos os acertos e despesas ANTERIORES do ciclo para calcular os totais
                val acertosAnteriores = acertoRepository.buscarPorRotaECicloId(rotaId, cicloId).first().filter { it.id != acertoId }
                val despesasDoCiclo = cicloAcertoRepository.buscarDespesasPorCicloId(cicloId)

                // ✅ CORREÇÃO: Verificar se realmente foi salvo
                val acertoSalvo = acertoRepository.buscarPorId(acertoId)
                Log.d("SettlementViewModel", "🔍 VERIFICAÇÃO: Observação no banco após salvamento: '${acertoSalvo?.observacoes}'")

                // Somar os valores anteriores com o valor do acerto ATUAL
                val valorTotalAcertado = acertosAnteriores.sumOf { it.valorRecebido } + (acertoSalvo?.valorRecebido ?: 0.0)
                val valorTotalDespesas = despesasDoCiclo.sumOf { it.valor }
                val clientesAcertados = (acertosAnteriores.map { it.clienteId } + (acertoSalvo?.clienteId ?: 0L)).distinct().size
                
                Log.d("SettlementViewModel", "=== ATUALIZANDO VALORES DO CICLO $cicloId ===")
                Log.d("SettlementViewModel", "Total Acertado: $valorTotalAcertado (Anteriores: ${acertosAnteriores.sumOf { it.valorRecebido }} + Atual: ${acertoSalvo?.valorRecebido})")
                Log.d("SettlementViewModel", "Total Despesas: $valorTotalDespesas")
                Log.d("SettlementViewModel", "Clientes Acertados: $clientesAcertados")

                cicloAcertoRepository.atualizarValoresCiclo(
                    cicloId = cicloId,
                    valorTotalAcertado = valorTotalAcertado,
                    valorTotalDespesas = valorTotalDespesas,
                    clientesAcertados = clientesAcertados
                )
                
                // ✅ CORREÇÃO CRÍTICA: Salvar dados detalhados de cada mesa do acerto com logs
                Log.d("SettlementViewModel", "=== SALVANDO MESAS DO ACERTO ===")
                Log.d("SettlementViewModel", "Total de mesas recebidas: ${dadosAcerto.mesas.size}")
                Log.d("SettlementViewModel", "Cliente encontrado: ${cliente.nome}")
                Log.d("SettlementViewModel", "Valor ficha do cliente: R$ ${cliente.valorFicha}")
                Log.d("SettlementViewModel", "Comissão ficha do cliente: R$ ${cliente.comissaoFicha}")
                
                // Garantir que não há duplicidade de mesaId
                val mesaIds = dadosAcerto.mesas.map { it.id }
                val duplicados = mesaIds.groupBy { it }.filter { it.value.size > 1 }.keys
                if (duplicados.isNotEmpty()) {
                    Log.e("SettlementViewModel", "DUPLICIDADE DETECTADA nos IDs das mesas: $duplicados")
                }
                val mesasUnicas = dadosAcerto.mesas.distinctBy { it.id }
                if (mesasUnicas.size != dadosAcerto.mesas.size) {
                    Log.w("SettlementViewModel", "Removendo mesas duplicadas antes de salvar. Total antes: ${dadosAcerto.mesas.size}, depois: ${mesasUnicas.size}")
                }
                val acertoMesas = mesasUnicas.mapIndexed { index, mesa ->
                    val fichasJogadas = if (mesa.valorFixo > 0) {
                        0 // Mesa de valor fixo não tem fichas jogadas
                    } else {
                        (mesa.fichasFinal - mesa.fichasInicial).coerceAtLeast(0)
                    }
                    
                    val subtotal = if (mesa.valorFixo > 0) {
                        mesa.valorFixo
                    } else {
                        fichasJogadas * (cliente.comissaoFicha)
                    }
                    
                    Log.d("SettlementViewModel", "=== MESA ${index + 1} ===")
                    Log.d("SettlementViewModel", "ID da mesa: ${mesa.id}")
                    Log.d("SettlementViewModel", "Número da mesa: ${mesa.numero}")
                    Log.d("SettlementViewModel", "Relógio inicial: ${mesa.fichasInicial}")
                    Log.d("SettlementViewModel", "Relógio final: ${mesa.fichasFinal}")
                    Log.d("SettlementViewModel", "Fichas jogadas: $fichasJogadas")
                    Log.d("SettlementViewModel", "Valor fixo: R$ ${mesa.valorFixo}")
                    Log.d("SettlementViewModel", "Subtotal calculado: R$ $subtotal")
                    Log.d("SettlementViewModel", "Com defeito: ${mesa.comDefeito}")
                    Log.d("SettlementViewModel", "Relógio reiniciou: ${mesa.relogioReiniciou}")
                    
                    com.example.gestaobilhares.data.entities.AcertoMesa(
                        acertoId = acertoId,
                        mesaId = mesa.id,
                        relogioInicial = mesa.fichasInicial,
                        relogioFinal = mesa.fichasFinal,
                        fichasJogadas = fichasJogadas,
                        valorFixo = mesa.valorFixo,
                        valorFicha = cliente.valorFicha,
                        comissaoFicha = cliente.comissaoFicha,
                        subtotal = subtotal,
                        comDefeito = mesa.comDefeito,
                        relogioReiniciou = mesa.relogioReiniciou,
                        observacoes = null,
                        // ✅ CORREÇÃO CRÍTICA: Incluir campos de foto
                        fotoRelogioFinal = mesa.fotoRelogioFinal,
                        dataFoto = mesa.dataFoto
                    )
                }
                
                Log.d("SettlementViewModel", "=== INSERINDO MESAS NO BANCO ===")
                Log.d("SettlementViewModel", "Total de AcertoMesa a inserir: ${acertoMesas.size}")
                acertoMesas.forEachIndexed { index, acertoMesa ->
                    Log.d("SettlementViewModel", "AcertoMesa ${index + 1}: Mesa ${acertoMesa.mesaId} - Subtotal: R$ ${acertoMesa.subtotal}")
                }
                
                acertoMesaRepository.inserirLista(acertoMesas)
                Log.d("SettlementViewModel", "✅ Dados de ${acertoMesas.size} mesas salvos para o acerto $acertoId")
                
                // ✅ NOVO: Registrar troca de pano no histórico de manutenção
                if (dadosAcerto.panoTrocado && !dadosAcerto.numeroPano.isNullOrBlank()) {
                    registrarTrocaPanoNoHistorico(dadosAcerto.mesas.map { mesa ->
                        com.example.gestaobilhares.ui.settlement.MesaDTO(
                            id = mesa.id,
                            numero = mesa.numero,
                            tipoMesa = mesa.tipoMesa,
                            tamanho = com.example.gestaobilhares.data.entities.TamanhoMesa.MEDIA,
                            estadoConservacao = com.example.gestaobilhares.data.entities.EstadoConservacao.BOM,
                            fichasInicial = mesa.fichasInicial,
                            fichasFinal = mesa.fichasFinal,
                            valorFixo = mesa.valorFixo,
                            valorFicha = 0.0,
                            comissaoFicha = 0.0,
                            ativa = true
                        )
                    }, dadosAcerto.numeroPano)
                }
                
                // ✅ CRÍTICO: Atualizar o débito atual na tabela de clientes
                clienteRepository.atualizarDebitoAtual(clienteId, debitoAtual)
                Log.d("SettlementViewModel", "Débito atual atualizado na tabela clientes: R$ $debitoAtual")
                
                // ✅ NOVO: Verificar se a atualização foi bem-sucedida
                val clienteAtualizado = clienteRepository.obterPorId(clienteId)
                Log.d("SettlementViewModel", "🔍 VERIFICAÇÃO: Débito atual na tabela clientes após atualização: R$ ${clienteAtualizado?.debitoAtual}")
                
                _resultadoSalvamento.value = ResultadoSalvamento.Sucesso(acertoId)
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao salvar acerto: ${e.localizedMessage}", e)
                _resultadoSalvamento.value = ResultadoSalvamento.Erro(e.localizedMessage ?: "Erro desconhecido")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetarResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }

    /**
     * Registra a troca de pano no histórico de manutenção das mesas.
     */
    private suspend fun registrarTrocaPanoNoHistorico(mesas: List<com.example.gestaobilhares.ui.settlement.MesaDTO>, numeroPano: String) {
        try {
            Log.d("SettlementViewModel", "Registrando troca de pano no histórico: $numeroPano")
            
            mesas.forEach { mesa ->
                val historico = HistoricoManutencaoMesa(
                    mesaId = mesa.id,
                    numeroMesa = mesa.numero,
                    tipoManutencao = TipoManutencao.TROCA_PANO,
                    descricao = "Troca de pano durante acerto - Número: $numeroPano",
                    responsavel = "Sistema de Acerto",
                    observacoes = "Troca de pano registrada automaticamente durante o acerto",
                    dataManutencao = java.util.Date()
                )
                
                historicoManutencaoMesaRepository.inserir(historico)
                Log.d("SettlementViewModel", "Histórico de troca de pano registrado para mesa ${mesa.numero}")
            }
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao registrar troca de pano no histórico: ${e.message}", e)
        }
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

    /**
     * ✅ NOVO: Busca uma mesa específica por ID para obter o número real
     */
    suspend fun buscarMesaPorId(mesaId: Long): Mesa? {
        return try {
            mesaRepository.buscarPorId(mesaId)
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao buscar mesa por ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * ✅ NOVO: Busca um cliente específico por ID para obter dados como comissão da ficha
     */
    suspend fun obterClientePorId(clienteId: Long): com.example.gestaobilhares.data.entities.Cliente? {
        return try {
            clienteRepository.obterPorId(clienteId)
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao buscar cliente por ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * ✅ NOVO: Calcula a média de fichas jogadas dos últimos acertos de uma mesa
     * @param mesaId ID da mesa
     * @param limite Máximo de acertos a considerar (padrão 5)
     * @return Média de fichas jogadas, ou 0 se não houver acertos anteriores
     */
    suspend fun calcularMediaFichasJogadas(mesaId: Long, limite: Int = 5): Double {
        return try {
            acertoMesaRepository.calcularMediaFichasJogadas(mesaId, limite)
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao calcular média de fichas: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * ✅ NOVO: Marca um pano como usado no estoque
     */
    suspend fun marcarPanoComoUsado(numeroPano: String, motivo: String = "Usado no acerto") {
        try {
            panoEstoqueRepository.marcarPanoComoUsadoPorNumero(numeroPano, motivo)
            Log.d("SettlementViewModel", "Pano $numeroPano marcado como usado: $motivo")
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao marcar pano como usado: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Troca o pano na mesa e marca como usado no estoque
     */
    suspend fun trocarPanoNaMesa(numeroPano: String, motivo: String = "Usado no acerto") {
        try {
            // 1. Buscar o pano no estoque
            val pano = panoEstoqueRepository.buscarPorNumero(numeroPano)
            if (pano == null) {
                Log.e("SettlementViewModel", "Pano $numeroPano não encontrado no estoque")
                return
            }
            
            // 2. Marcar pano como usado no estoque
            panoEstoqueRepository.marcarPanoComoUsado(pano.id, motivo)
            
            // 3. TODO: Vincular pano à mesa (precisa do ID da mesa)
            // Por enquanto, apenas log
            Log.d("SettlementViewModel", "Pano $numeroPano trocado na mesa: $motivo")
            
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao trocar pano na mesa: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Troca o pano em uma mesa específica
     */
    suspend fun trocarPanoNaMesa(mesaId: Long, numeroPano: String, motivo: String = "Usado no acerto") {
        try {
            // 1. Buscar o pano no estoque
            val pano = panoEstoqueRepository.buscarPorNumero(numeroPano)
            if (pano == null) {
                Log.e("SettlementViewModel", "Pano $numeroPano não encontrado no estoque")
                return
            }
            
            // 2. Marcar pano como usado no estoque
            panoEstoqueRepository.marcarPanoComoUsado(pano.id, motivo)
            
            // 3. Atualizar mesa com novo pano
            atualizarPanoDaMesa(mesaId, pano.id)
            
            Log.d("SettlementViewModel", "Pano $numeroPano trocado na mesa $mesaId: $motivo")
            
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao trocar pano na mesa: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Atualiza o pano atual de uma mesa
     */
    private suspend fun atualizarPanoDaMesa(mesaId: Long, panoId: Long) {
        try {
            // Buscar a mesa atual
            val mesa = mesaRepository.buscarPorId(mesaId)
            if (mesa != null) {
                // Atualizar mesa com novo pano e data
                val mesaAtualizada = mesa.copy(
                    panoAtualId = panoId,
                    dataUltimaTrocaPano = Date()
                )
                mesaRepository.atualizar(mesaAtualizada)
                Log.d("SettlementViewModel", "Mesa $mesaId atualizada com pano $panoId")
            }
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao atualizar pano da mesa: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Carrega o pano atual de uma mesa
     */
    suspend fun carregarPanoAtualDaMesa(mesaId: Long): PanoEstoque? {
        return try {
            // 1. Buscar a mesa
            val mesa = mesaRepository.buscarPorId(mesaId)
            if (mesa?.panoAtualId == null) {
                Log.d("SettlementViewModel", "Mesa $mesaId não possui pano atual")
                return null
            }
            
            // 2. Buscar o pano atual
            val pano = panoEstoqueRepository.buscarPorId(mesa.panoAtualId)
            if (pano == null) {
                Log.w("SettlementViewModel", "Pano ${mesa.panoAtualId} não encontrado no estoque")
                return null
            }
            
            Log.d("SettlementViewModel", "Pano atual da mesa $mesaId: ${pano.numero}")
            pano
            
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao carregar pano atual da mesa: ${e.message}", e)
            null
        }
    }
} 