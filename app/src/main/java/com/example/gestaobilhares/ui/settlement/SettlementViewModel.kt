package com.example.gestaobilhares.ui.settlement

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import android.util.Log
import com.example.gestaobilhares.BuildConfig
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.entities.Acerto
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
/**
 * ViewModel para SettlementFragment
 * FASE 4A - Implementação básica para desbloqueio
 */
class SettlementViewModel constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {

    /**
     * ✅ NOVA CLASSE: Resultado específico para salvamento de acerto
     */
    sealed class ResultadoSalvamento {
        data class Sucesso(val acertoId: Long) : ResultadoSalvamento()
        data class Erro(val mensagem: String) : ResultadoSalvamento()
        data class AcertoJaExiste(val acertoExistente: Acerto) : ResultadoSalvamento()
    }

    // Estados de loading e error já estão no BaseViewModel

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
                showLoading()
            try {
                val cliente = appRepository.obterClientePorId(clienteId)
                if (cliente != null) {
                    _clientName.value = cliente.nome
                    _clientAddress.value = cliente.endereco ?: "---"
                    logOperation("SETTLEMENT", "Nome do cliente carregado: ${cliente.nome}, endereço: ${cliente.endereco}")
                } else {
                    _clientName.value = "Cliente não encontrado"
                    _clientAddress.value = "---"
                    logOperation("SETTLEMENT", "Cliente não encontrado para ID: $clienteId")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _clientName.value = "Erro ao carregar cliente"
                _clientAddress.value = "---"
            } finally {
                hideLoading()
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
        Log.d("SettlementViewModel", "Acerto ID para edição: $acertoIdParaEdicao")
        
        return mesasCliente.map { mesa ->
            try {
                logOperation("SETTLEMENT", "Processando mesa ${mesa.numero} (ID: ${mesa.id})")
                
                if (acertoIdParaEdicao != null) {
                    // ✅ MODO EDIÇÃO: Carregar dados do acerto sendo editado
                    logOperation("SETTLEMENT", "Mesa ${mesa.numero}: Buscando dados do acerto ID: $acertoIdParaEdicao")
                    val acertoMesas = appRepository.buscarAcertoMesasPorAcerto(acertoIdParaEdicao).first()
                    val acertoMesa = acertoMesas.find { it.mesaId == mesa.id }
                    if (acertoMesa != null) {
                        // Usar o relógio inicial e final do acerto sendo editado
                        val relogioInicial = acertoMesa.relogioInicial
                        val relogioFinal = acertoMesa.relogioFinal
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: MODO EDIÇÃO - relógio inicial: $relogioInicial, relógio final: $relogioFinal")
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: AcertoMesa encontrado - ID: ${acertoMesa.id}, AcertoID: ${acertoMesa.acertoId}")
                        mesa.copy(
                            fichasInicial = relogioInicial,
                            fichasFinal = relogioFinal
                        )
                    } else {
                        // Fallback: usar dados da mesa
                        val relogioInicial = mesa.fichasInicial
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: MODO EDIÇÃO - acerto não encontrado, usando dados da mesa: $relogioInicial")
                        mesa.copy(fichasInicial = relogioInicial)
                    }
                } else {
                    // ✅ MODO NOVO ACERTO: Usar lógica original
                    val ultimoAcertoMesa = appRepository.buscarUltimoAcertoMesaItem(mesa.id)
                    
                    if (ultimoAcertoMesa != null) {
                        // Usar o relógio final do último acerto como inicial do próximo
                        val relogioInicial = ultimoAcertoMesa.relogioFinal
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: MODO NOVO ACERTO - relógio final: ${ultimoAcertoMesa.relogioFinal} -> novo relógio inicial: $relogioInicial")
                        mesa.copy(fichasInicial = relogioInicial)
                    } else {
                        // Primeiro acerto - usar relógio inicial cadastrado ou 0
                        val relogioInicial = mesa.fichasInicial
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: MODO NOVO ACERTO - primeiro acerto, usando relógio inicial cadastrado: $relogioInicial")
                        mesa.copy(fichasInicial = relogioInicial)
                    }
                }
            } catch (e: Exception) {
                logError("SETTLEMENT", "Erro ao preparar mesa ${mesa.numero}: ${e.message}")
                val relogioInicial = mesa.fichasInicial
                mesa.copy(fichasInicial = relogioInicial)
            }
        }.also { mesasPreparadas ->
            Log.d("SettlementViewModel", "=== MESAS PREPARADAS ===")
            mesasPreparadas.forEach { mesa ->
                logOperation("SETTLEMENT", "Mesa ${mesa.numero}: relógio inicial=${mesa.fichasInicial}, relógio final=${mesa.fichasFinal}")
            }
        }
    }

    fun carregarDadosCliente(clienteId: Long, callback: (com.example.gestaobilhares.data.entities.Cliente?) -> Unit) {
        viewModelScope.launch {
            try {
                val cliente = appRepository.obterClientePorId(clienteId)
                callback(cliente)
            } catch (e: Exception) {
                logError("SETTLEMENT", "Erro ao carregar dados do cliente: ${e.localizedMessage}", e)
                callback(null)
            }
        }
    }

    fun loadMesasCliente(clienteId: Long) {
        viewModelScope.launch {
            appRepository.obterMesasPorCliente(clienteId).collect { mesas: List<Mesa> ->
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
            appRepository.obterMesasPorClienteDireto(clienteId)
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao carregar mesas direto: ${e.message}")
            emptyList()
        }
    }

    fun carregarHistoricoAcertos(clienteId: Long) {
        viewModelScope.launch {
            appRepository.obterAcertosPorCliente(clienteId).collect { acertos: List<Acerto> ->
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
                    val acertoParaEdicao = appRepository.buscarPorId(acertoIdParaEdicao)
                    if (acertoParaEdicao != null) {
                        _debitoAnterior.value = acertoParaEdicao.debitoAnterior
                        logOperation("SETTLEMENT", "MODO EDIÇÃO: Débito ANTERIOR do acerto sendo editado: R$ ${acertoParaEdicao.debitoAnterior}")
                    } else {
                        _debitoAnterior.value = 0.0
                        logError("SETTLEMENT", "MODO EDIÇÃO: Acerto para edição não encontrado, débito anterior: R$ 0,00")
                    }
                } else {
                    // ✅ MODO NOVO ACERTO: Usar débito do último acerto como anterior
                    val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(clienteId)
                    if (ultimoAcerto != null) {
                        _debitoAnterior.value = ultimoAcerto.debitoAtual
                        logOperation("SETTLEMENT", "MODO NOVO ACERTO: Débito anterior carregado: R$ ${ultimoAcerto.debitoAtual}")
                    } else {
                        _debitoAnterior.value = 0.0
                        logOperation("SETTLEMENT", "MODO NOVO ACERTO: Nenhum acerto anterior encontrado, débito anterior: R$ 0,00")
                    }
                }
            } catch (e: Exception) {
                logError("SETTLEMENT", "Erro ao buscar débito anterior: ${e.message}")
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
                showLoading()
            try {
                logOperation("SETTLEMENT", "Salvando acerto com clienteId=$clienteId, mesas=${dadosAcerto.mesas.map { it.numero }}, modoEdicao=${acertoIdParaEdicao != null}")
                
                // Buscar cliente uma única vez
                val cliente = appRepository.obterClientePorId(clienteId) ?: throw IllegalStateException("Cliente não encontrado para o ID: $clienteId")
                val rotaId = cliente.rotaId ?: throw IllegalStateException("Cliente $clienteId não está vinculado a uma rota. Não é possível criar acerto.")
                
                // Buscar ciclo como no pré-refatoração: fonte única (obterCicloAtualRota)
                val cicloId = appRepository.obterCicloAtualIdPorRota(rotaId)
                    ?: throw IllegalStateException("Nenhum ciclo encontrado para a rota $rotaId.")
                
                logOperation("SETTLEMENT", "[SALVAR_ACERTO] cicloId usado (pré-ref): $cicloId | rotaId: $rotaId | modoEdicao: ${acertoIdParaEdicao != null}")
                
                // ✅ DEBUG: Verificar se o ciclo está realmente ativo
                val cicloAtual = appRepository.buscarCicloAtualPorRota(rotaId)
                logOperation("SETTLEMENT", "🔍 DEBUG CICLO: Ciclo encontrado - ID: ${cicloAtual?.id}, Status: ${cicloAtual?.status}, Número: ${cicloAtual?.numeroCiclo}")
                
                if (cicloAtual?.status != com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO) {
                    logError("SETTLEMENT", "❌ PROBLEMA: Ciclo não está EM_ANDAMENTO! Status atual: ${cicloAtual?.status}")
                    _resultadoSalvamento.value = ResultadoSalvamento.Erro("Ciclo não está ativo. Finalize o ciclo anterior e inicie um novo.")
                    hideLoading()
                    return@launch
                }

                // ✅ Usar SEMPRE o ID do ciclo ativo obtido acima para validação e salvamento
                val cicloIdEfetivo = cicloAtual.id

                // ✅ CORREÇÃO: Validação apenas para novos acertos (não para edição)
                if (acertoIdParaEdicao == null) {
                    // ✅ DEBUG DETALHADO: Verificar todos os acertos do cliente no ciclo
                    val acertosDoClienteNoCiclo = appRepository.buscarAcertosPorClienteECicloId(clienteId, cicloIdEfetivo).first()
                    logOperation("SETTLEMENT", "🔍 DEBUG VALIDAÇÃO: Cliente $clienteId no ciclo $cicloIdEfetivo")
                    logOperation("SETTLEMENT", "🔍 Total de acertos encontrados: ${acertosDoClienteNoCiclo.size}")
                    
                    acertosDoClienteNoCiclo.forEachIndexed { index, acerto ->
                        logOperation("SETTLEMENT", "🔍 Acerto $index: ID=${acerto.id}, Status=${acerto.status}, Data=${acerto.dataAcerto}")
                    }
                    
                    // Verificar se já existe acerto FINALIZADO para este cliente no ciclo ATUAL
                    // ✅ CORREÇÃO CRÍTICA: Verificar apenas acertos FINALIZADOS (não PENDENTES ou CANCELADOS)
                    val acertoFinalizado = acertosDoClienteNoCiclo.firstOrNull { acerto -> 
                        acerto.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO 
                    }
                    
                    if (acertoFinalizado != null) {
                        logError("SETTLEMENT", "ACERTO JÁ EXISTE: Cliente $clienteId já possui acerto FINALIZADO (ID: ${acertoFinalizado.id}) no ciclo $cicloIdEfetivo")
                        _resultadoSalvamento.value = ResultadoSalvamento.AcertoJaExiste(acertoFinalizado)
                        hideLoading()
                        return@launch
                    }
                    
                    logOperation("SETTLEMENT", "✅ Validação passou: Cliente $clienteId pode criar novo acerto no ciclo $cicloIdEfetivo (nenhum acerto FINALIZADO encontrado)")
                } else {
                    logOperation("SETTLEMENT", "✅ Modo edição ativo (acertoId: $acertoIdParaEdicao). Pulando validação de acerto único.")
                }

                // ✅ FASE 1: Usar FinancialCalculator centralizado
                val valorRecebido = com.example.gestaobilhares.utils.FinancialCalculator.calcularValorRecebido(metodosPagamento)
                val debitoAnterior = _debitoAnterior.value
                
                // Converter mesas para formato do FinancialCalculator
                val mesasCalculo = dadosAcerto.mesas.map { mesa ->
                    com.example.gestaobilhares.utils.FinancialCalculator.MesaAcertoCalculo(
                        fichasInicial = mesa.fichasInicial,
                        fichasFinal = mesa.fichasFinal,
                        valorFixo = mesa.valorFixo
                    )
                }
                
                val valorTotal = com.example.gestaobilhares.utils.FinancialCalculator.calcularValorTotalMesas(
                    mesas = mesasCalculo,
                    comissaoFicha = cliente.comissaoFicha
                )
                
                val valorComDesconto = com.example.gestaobilhares.utils.FinancialCalculator.calcularValorComDesconto(
                    valorTotal = valorTotal,
                    desconto = desconto
                )
                
                val debitoAtual = com.example.gestaobilhares.utils.FinancialCalculator.calcularDebitoAtual(
                    debitoAnterior = debitoAnterior,
                    valorTotal = valorTotal,
                    desconto = desconto,
                    valorRecebido = valorRecebido
                )
                
                // ✅ CORREÇÃO: Logs detalhados para debug do cálculo do débito
                logOperation("SETTLEMENT", "=== CÁLCULO DO DÉBITO ATUAL ===")
                logOperation("SETTLEMENT", "Débito anterior: R$ $debitoAnterior")
                logOperation("SETTLEMENT", "Valor total das mesas: R$ $valorTotal")
                logOperation("SETTLEMENT", "Desconto aplicado: R$ $desconto")
                logOperation("SETTLEMENT", "Valor com desconto: R$ $valorComDesconto")
                logOperation("SETTLEMENT", "Valor recebido: R$ $valorRecebido")
                logOperation("SETTLEMENT", "Débito atual calculado: R$ $debitoAtual")
                logOperation("SETTLEMENT", "Fórmula: $debitoAnterior + $valorComDesconto - $valorRecebido = $debitoAtual")
                
                val metodosPagamentoJson = Gson().toJson(metodosPagamento)
                // ✅ CORREÇÃO: Logs detalhados para debug das observações
                logOperation("SETTLEMENT", "=== SALVANDO ACERTO NO BANCO - DEBUG OBSERVAÇÕES ===")
                logOperation("SETTLEMENT", "Observação recebida dos dados: '${dadosAcerto.observacao}'")
                logOperation("SETTLEMENT", "Observação é nula? ${dadosAcerto.observacao == null}")
                logOperation("SETTLEMENT", "Observação é vazia? ${dadosAcerto.observacao.isEmpty()}")
                logOperation("SETTLEMENT", "Observação é blank? ${dadosAcerto.observacao.isBlank()}")
                
                // ✅ CORREÇÃO: Observação será apenas manual, sem preenchimento automático
                val observacaoParaSalvar = dadosAcerto.observacao.trim()
                
                logOperation("SETTLEMENT", "Observação que será salva no banco: '$observacaoParaSalvar'")

                // ✅ CORREÇÃO: Criar dados extras JSON para campos adicionais
                val dadosExtras = mapOf(
                    "justificativa" to dadosAcerto.justificativa,
                    "versaoApp" to "1.0.0"
                )
                val dadosExtrasJson = Gson().toJson(dadosExtras)
                
                logOperation("SETTLEMENT", "=== SALVANDO TODOS OS DADOS ===")
                logOperation("SETTLEMENT", "Representante: '${dadosAcerto.representante}'")
                logOperation("SETTLEMENT", "Tipo de acerto: '${dadosAcerto.tipoAcerto}'")
                logOperation("SETTLEMENT", "Pano trocado: ${dadosAcerto.panoTrocado}")
                logOperation("SETTLEMENT", "Número do pano: '${dadosAcerto.numeroPano}'")
                logOperation("SETTLEMENT", "Métodos de pagamento: $metodosPagamento")

                // ✅ CORREÇÃO CRÍTICA: Vínculos com rota e ciclo
                android.util.Log.d("SettlementViewModel", "=== VINCULANDO ACERTO À ROTA E CICLO ===")
                android.util.Log.d("SettlementViewModel", "Cliente ID: $clienteId")
                android.util.Log.d("SettlementViewModel", "Rota ID do cliente: $rotaId")
                android.util.Log.d("SettlementViewModel", "Ciclo atual: $cicloIdEfetivo")
                
                // ✅ CORREÇÃO: Lógica diferente para edição vs. novo acerto
                val acertoId: Long
                if (acertoIdParaEdicao != null) {
                    // MODO EDIÇÃO: Atualizar acerto existente
                    logOperation("SETTLEMENT", "🔄 MODO EDIÇÃO: Atualizando acerto existente ID: $acertoIdParaEdicao")
                    
                    // Buscar acerto existente
                    val acertoExistente = appRepository.buscarPorId(acertoIdParaEdicao)
                    if (acertoExistente == null) {
                        logError("SETTLEMENT", "❌ Acerto para edição não encontrado: ID $acertoIdParaEdicao")
                        _resultadoSalvamento.value = ResultadoSalvamento.Erro("Acerto para edição não encontrado")
                        hideLoading()
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
                        dataFinalizacao = com.example.gestaobilhares.utils.DateUtils.obterDataAtual(),
                        metodosPagamentoJson = metodosPagamentoJson,
                        representante = dadosAcerto.representante,
                        tipoAcerto = dadosAcerto.tipoAcerto,
                        panoTrocado = dadosAcerto.panoTrocado,
                        numeroPano = dadosAcerto.numeroPano,
                        dadosExtrasJson = dadosExtrasJson
                    )
                    
                    appRepository.atualizarAcerto(acertoAtualizado)
                    acertoId = acertoIdParaEdicao
                    logOperation("SETTLEMENT", "✅ Acerto atualizado com sucesso! ID: $acertoId")
                    
                } else {
                    // MODO NOVO ACERTO: Criar novo acerto
                    logOperation("SETTLEMENT", "🆕 MODO NOVO ACERTO: Criando novo acerto")
                    
                    val acerto = Acerto(
                        clienteId = clienteId,
                        colaboradorId = null,
                        periodoInicio = com.example.gestaobilhares.utils.DateUtils.obterDataAtual(),
                        periodoFim = com.example.gestaobilhares.utils.DateUtils.obterDataAtual(),
                        totalMesas = dadosAcerto.mesas.size.toDouble(),
                        debitoAnterior = debitoAnterior,
                        valorTotal = valorTotal,
                        desconto = desconto,
                        valorComDesconto = valorComDesconto,
                        valorRecebido = valorRecebido,
                        debitoAtual = debitoAtual,
                        status = com.example.gestaobilhares.data.entities.StatusAcerto.PENDENTE,
                        observacoes = observacaoParaSalvar,
                        dataFinalizacao = com.example.gestaobilhares.utils.DateUtils.obterDataAtual(),
                        metodosPagamentoJson = metodosPagamentoJson,
                        representante = dadosAcerto.representante,
                        tipoAcerto = dadosAcerto.tipoAcerto,
                        panoTrocado = dadosAcerto.panoTrocado,
                        numeroPano = dadosAcerto.numeroPano,
                        dadosExtrasJson = dadosExtrasJson,
                        rotaId = rotaId,
                        cicloId = cicloIdEfetivo
                    )
                    
                    acertoId = appRepository.salvarAcerto(acerto)
                    logOperation("SETTLEMENT", "✅ Novo acerto salvo com ID: $acertoId")
                }
                
                // NOVO: Atualizar valores do ciclo após salvar acerto
                // Buscar todos os acertos e despesas ANTERIORES do ciclo para calcular os totais
                val acertosAnteriores = appRepository.buscarPorRotaECicloId(rotaId, cicloIdEfetivo).first().filter { acerto: Acerto -> acerto.id != acertoId }
                val despesasDoCiclo = appRepository.buscarDespesasPorCicloId(cicloIdEfetivo)

                // ✅ CORREÇÃO: Verificar se realmente foi salvo
                val acertoSalvo = appRepository.buscarPorId(acertoId)
                logOperation("SETTLEMENT", "🔍 VERIFICAÇÃO: Observação no banco após salvamento: '${acertoSalvo?.observacoes}'")

                // Somar os valores anteriores com o valor do acerto ATUAL
                val valorTotalAcertado = acertosAnteriores.sumOf { acerto: Acerto -> acerto.valorRecebido } + (acertoSalvo?.valorRecebido ?: 0.0)
                val valorTotalDespesas = despesasDoCiclo.first().sumOf { despesa -> despesa.valor }
                val clientesAcertados = (acertosAnteriores.map { acerto: Acerto -> acerto.clienteId } + (acertoSalvo?.clienteId ?: 0L)).distinct().size
                
                logOperation("SETTLEMENT", "=== ATUALIZANDO VALORES DO CICLO $cicloIdEfetivo ===")
                logOperation("SETTLEMENT", "Total Acertado: $valorTotalAcertado (Anteriores: ${acertosAnteriores.sumOf { acerto: Acerto -> acerto.valorRecebido }} + Atual: ${acertoSalvo?.valorRecebido})")
                logOperation("SETTLEMENT", "Total Despesas: $valorTotalDespesas")
                logOperation("SETTLEMENT", "Clientes Acertados: $clientesAcertados")

                appRepository.atualizarValoresCiclo(
                    cicloId = cicloIdEfetivo,
                    valorTotalAcertado = valorTotalAcertado,
                    valorTotalDespesas = valorTotalDespesas,
                    clientesAcertados = clientesAcertados
                )
                
                // ✅ CORREÇÃO CRÍTICA: Salvar dados detalhados de cada mesa do acerto com logs
                logOperation("SETTLEMENT", "=== SALVANDO MESAS DO ACERTO ===")
                logOperation("SETTLEMENT", "Total de mesas recebidas: ${dadosAcerto.mesas.size}")
                logOperation("SETTLEMENT", "Cliente encontrado: ${cliente.nome}")
                logOperation("SETTLEMENT", "Valor ficha do cliente: R$ ${cliente.valorFicha}")
                logOperation("SETTLEMENT", "Comissão ficha do cliente: R$ ${cliente.comissaoFicha}")
                
                // Garantir que não há duplicidade de mesaId
                val mesaIds = dadosAcerto.mesas.map { it.id }
                val duplicados = mesaIds.groupBy { it }.filter { it.value.size > 1 }.keys
                if (duplicados.isNotEmpty()) {
                    logError("SETTLEMENT", "DUPLICIDADE DETECTADA nos IDs das mesas: $duplicados")
                }
                val mesasUnicas = dadosAcerto.mesas.distinctBy { it.id }
                if (mesasUnicas.size != dadosAcerto.mesas.size) {
                    logError("SETTLEMENT", "Removendo mesas duplicadas antes de salvar. Total antes: ${dadosAcerto.mesas.size}, depois: ${mesasUnicas.size}")
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
                    
                    logOperation("SETTLEMENT", "=== MESA ${index + 1} ===")
                    logOperation("SETTLEMENT", "ID da mesa: ${mesa.id}")
                    logOperation("SETTLEMENT", "Número da mesa: ${mesa.numero}")
                    logOperation("SETTLEMENT", "Relógio inicial: ${mesa.fichasInicial}")
                    logOperation("SETTLEMENT", "Relógio final: ${mesa.fichasFinal}")
                    logOperation("SETTLEMENT", "Fichas jogadas: $fichasJogadas")
                    logOperation("SETTLEMENT", "Valor fixo: R$ ${mesa.valorFixo}")
                    logOperation("SETTLEMENT", "Subtotal calculado: R$ $subtotal")
                    logOperation("SETTLEMENT", "Com defeito: ${mesa.comDefeito}")
                    logOperation("SETTLEMENT", "Relógio reiniciou: ${mesa.relogioReiniciou}")
                    
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
                
                logOperation("SETTLEMENT", "=== INSERINDO MESAS NO BANCO ===")
                logOperation("SETTLEMENT", "Total de AcertoMesa a inserir: ${acertoMesas.size}")
                acertoMesas.forEachIndexed { index, acertoMesa ->
                    logOperation("SETTLEMENT", "AcertoMesa ${index + 1}: Mesa ${acertoMesa.mesaId} - Subtotal: R$ ${acertoMesa.subtotal}")
                }
                
                acertoMesas.forEach { appRepository.inserirAcertoMesa(it) }
                logOperation("SETTLEMENT", "✅ Dados de ${acertoMesas.size} mesas salvos para o acerto $acertoId")
                
                // ✅ NOVO: Registrar troca de pano no histórico de manutenção
                if (dadosAcerto.panoTrocado && com.example.gestaobilhares.utils.StringUtils.isNaoVazia(dadosAcerto.numeroPano)) {
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
                    }, dadosAcerto.numeroPano ?: "")
                }
                
                // ✅ CRÍTICO: Atualizar o débito atual na tabela de clientes
                appRepository.atualizarDebitoAtual(clienteId, debitoAtual)
                logOperation("SETTLEMENT", "Débito atual atualizado na tabela clientes: R$ $debitoAtual")
                
                // ✅ NOVO: Verificar se a atualização foi bem-sucedida
                val clienteAtualizado = appRepository.obterClientePorId(clienteId)
                logOperation("SETTLEMENT", "🔍 VERIFICAÇÃO: Débito atual na tabela clientes após atualização: R$ ${clienteAtualizado?.debitoAtual}")
                
                _resultadoSalvamento.value = ResultadoSalvamento.Sucesso(acertoId)
            } catch (e: Exception) {
                logError("SETTLEMENT", "Erro ao salvar acerto: ${e.localizedMessage}", e)
                _resultadoSalvamento.value = ResultadoSalvamento.Erro(e.localizedMessage ?: "Erro desconhecido")
            } finally {
                hideLoading()
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
                    dataManutencao = com.example.gestaobilhares.utils.DateUtils.obterDataAtual()
                )
                
                appRepository.inserirHistoricoManutencaoMesa(historico)
                logOperation("SETTLEMENT", "Histórico de troca de pano registrado para mesa ${mesa.numero}")
            }
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao registrar troca de pano no histórico: ${e.message}", e)
        }
    }
    
    fun limparResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }

    suspend fun buscarAcertoPorId(acertoId: Long): Acerto? {
        return appRepository.buscarPorId(acertoId)
    }

    suspend fun buscarMesasDoAcerto(acertoId: Long): List<com.example.gestaobilhares.data.entities.AcertoMesa> {
        return appRepository.buscarAcertoMesasPorAcerto(acertoId).first()
    }

    fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    /**
     * ✅ NOVO: Busca uma mesa específica por ID para obter o número real
     */
    suspend fun buscarMesaPorId(mesaId: Long): Mesa? {
        return try {
            appRepository.obterMesaPorId(mesaId)
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
            appRepository.obterClientePorId(clienteId)
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao buscar cliente por ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * ✅ NOVO: Busca o contrato ativo do cliente para exibir no recibo
     */
    suspend fun buscarContratoAtivoPorCliente(clienteId: Long): com.example.gestaobilhares.data.entities.ContratoLocacao? {
        return try {
            // Usar o AppRepository através do ClienteRepository
            appRepository.buscarContratoAtivoPorCliente(clienteId)
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao buscar contrato ativo do cliente: ${e.message}", e)
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
            appRepository.calcularMediaFichasJogadas(mesaId, limite)
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
            Log.d("SettlementViewModel", "Marcando pano $numeroPano como usado: $motivo")
            appRepository.marcarPanoComoUsadoPorNumero(numeroPano, motivo)
            Log.d("SettlementViewModel", "Pano $numeroPano marcado como usado com sucesso")
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
            val pano = appRepository.buscarPorNumero(numeroPano)
            if (pano == null) {
                logError("SETTLEMENT", "Pano $numeroPano não encontrado no estoque")
                return
            }
            
            // 2. Marcar pano como usado no estoque
            appRepository.marcarPanoComoUsado(pano.id, motivo)
            
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
            Log.d("SettlementViewModel", "Iniciando troca de pano $numeroPano na mesa $mesaId")
            
            // 1. Buscar o pano no estoque
            val pano = appRepository.buscarPorNumero(numeroPano)
            if (pano == null) {
                logError("SETTLEMENT", "Pano $numeroPano não encontrado no estoque")
                return
            }
            
            Log.d("SettlementViewModel", "Pano encontrado: ${pano.numero} (ID: ${pano.id})")
            
            // 2. Marcar pano como usado no estoque
            appRepository.marcarPanoComoUsado(pano.id, motivo)
            Log.d("SettlementViewModel", "Pano ${pano.id} marcado como usado no estoque")
            
            // 3. Atualizar mesa com novo pano
            atualizarPanoDaMesa(mesaId, pano.id)
            
            Log.d("SettlementViewModel", "Pano $numeroPano trocado na mesa $mesaId com sucesso: $motivo")
            
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao trocar pano na mesa: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Atualiza o pano atual de uma mesa
     */
    private suspend fun atualizarPanoDaMesa(mesaId: Long, panoId: Long) {
        try {
            Log.d("SettlementViewModel", "Atualizando pano da mesa $mesaId com pano $panoId")
            
            // Buscar a mesa atual
            val mesa = appRepository.obterMesaPorId(mesaId)
            if (mesa != null) {
                logOperation("SETTLEMENT", "Mesa encontrada: ${mesa.numero}")
                
                // Atualizar mesa com novo pano e data
                val mesaAtualizada = mesa.copy(
                    panoAtualId = panoId,
                    dataUltimaTrocaPano = com.example.gestaobilhares.utils.DateUtils.obterDataAtual()
                )
                appRepository.atualizarMesa(mesaAtualizada)
                logOperation("SETTLEMENT", "Mesa $mesaId atualizada com pano $panoId com sucesso")
            } else {
                logError("SETTLEMENT", "Mesa $mesaId não encontrada")
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
            val mesa = appRepository.obterMesaPorId(mesaId)
            if (mesa?.panoAtualId == null) {
                logOperation("SETTLEMENT", "Mesa $mesaId não possui pano atual")
                return null
            }
            
            // 2. Buscar o pano atual
            val pano = appRepository.obterPanoPorId(mesa.panoAtualId)
            if (pano == null) {
                logError("SETTLEMENT", "Pano ${mesa.panoAtualId} não encontrado no estoque")
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
