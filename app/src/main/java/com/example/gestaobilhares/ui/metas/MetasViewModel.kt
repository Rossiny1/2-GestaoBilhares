package com.example.gestaobilhares.ui.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MetasViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _metasRotas = MutableStateFlow<List<MetaRotaResumo>>(emptyList())
    val metasRotas: StateFlow<List<MetaRotaResumo>> = _metasRotas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _filtroTipoMeta = MutableStateFlow<TipoMeta?>(null)
    val filtroTipoMeta: StateFlow<TipoMeta?> = _filtroTipoMeta.asStateFlow()

    private val _filtroStatus = MutableStateFlow<StatusFiltroMeta?>(null)
    val filtroStatus: StateFlow<StatusFiltroMeta?> = _filtroStatus.asStateFlow()

    private val _mostrarHistorico = MutableStateFlow(false)
    val mostrarHistorico: StateFlow<Boolean> = _mostrarHistorico.asStateFlow()

    private val _notificacoes = MutableStateFlow<List<NotificacaoMeta>>(emptyList())
    val notificacoes: StateFlow<List<NotificacaoMeta>> = _notificacoes.asStateFlow()

    // Atualização periódica via corrotina
    private var isAutoRefreshEnabled: Boolean = true

    init {
        carregarMetasRotas()
        iniciarAutoRefresh()
    }

    /**
     * Carrega todas as metas das rotas
     */
    fun carregarMetasRotas() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Timber.d("Carregando metas das rotas")

                // Buscar todas as rotas ativas
                val rotas = appRepository.obterTodasRotas().first().filter { rota -> rota.ativa }
                Timber.d("Rotas ativas: ${rotas.size}")

                val metasRotasList = mutableListOf<MetaRotaResumo>()

                for (rota in rotas) {
                    try {
                        Timber.d("Processando rota: %s (id=%s)", rota.nome, rota.id)
                        val metaRota = criarMetaRotaResumo(rota)
                        if (metaRota != null) {
                            Timber.d("MetaRota criada para %s: %s metas", rota.nome, metaRota.metas.size)
                            metasRotasList.add(metaRota)
                        } else {
                            Timber.w("MetaRota nao criada para %s", rota.nome)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Erro ao carregar metas da rota %s: %s", rota.nome, e.message)
                    }
                }

                Timber.d("Total de MetaRotas: %s", metasRotasList.size)
                _metasRotas.value = metasRotasList

                // Gerar notificações para metas próximas
                gerarNotificacoesMetas(metasRotasList)
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar metas: %s", e.message)
                _message.value = "Erro ao carregar metas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cria um resumo de metas para uma rota específica
     */
    private suspend fun criarMetaRotaResumo(rota: Rota): MetaRotaResumo? {
        try {
            Timber.d("Criando MetaRotaResumo para rota: %s (id=%s)", rota.nome, rota.id)

            // Buscar ciclo atual ou histórico
            val cicloAtual = if (_mostrarHistorico.value) {
                Timber.d("Buscando ultimo ciclo finalizado para rota %s", rota.nome)
                appRepository.buscarUltimoCicloFinalizadoPorRota(rota.id)
            } else {
                Timber.d("Buscando ciclo atual para rota %s", rota.nome)
                appRepository.buscarCicloAtualPorRota(rota.id)
            }

            if (cicloAtual == null) {
                Timber.w("Nenhum ciclo encontrado para rota %s", rota.nome)
                return null
            }

            Timber.d("Ciclo encontrado: %s/%s (id=%s)", cicloAtual.numeroCiclo, cicloAtual.ano, cicloAtual.id)

            // Buscar colaborador responsável principal
            Timber.d("Buscando colaborador responsavel principal para rota %s", rota.nome)
            val colaboradorResponsavel = appRepository.buscarColaboradorResponsavelPrincipal(rota.id)

            if (colaboradorResponsavel != null) {
                Timber.d("Colaborador responsavel encontrado: %s", colaboradorResponsavel.nome)
            } else {
                Timber.w("Nenhum colaborador responsavel encontrado para rota %s", rota.nome)
            }

            // Buscar metas do ciclo atual
            Timber.d("Buscando metas para rota %s e ciclo %s", rota.id, cicloAtual.id)
            val metas = appRepository.buscarMetasPorRotaECiclo(rota.id, cicloAtual.id)
            Timber.d("Metas encontradas: %s", metas.size)

            if (metas.isEmpty()) {
                Timber.w("Nenhuma meta encontrada para rota %s e ciclo %s", rota.nome, cicloAtual.id)
                // Retornar MetaRotaResumo mesmo sem metas para mostrar a rota
                return MetaRotaResumo(
                    rota = rota,
                    cicloAtual = cicloAtual.numeroCiclo,
                    anoCiclo = cicloAtual.ano,
                    statusCiclo = cicloAtual.status,
                    colaboradorResponsavel = colaboradorResponsavel,
                    metas = emptyList(),
                    dataInicioCiclo = cicloAtual.dataInicio,
                    dataFimCiclo = cicloAtual.dataFim,
                    ultimaAtualizacao = Date()
                )
            }

            // Calcular progresso das metas baseado em dados reais
            Timber.d("Calculando progresso das metas")
            val metasComProgresso = calcularProgressoMetas(metas, rota.id, cicloAtual.id)

            val metaRotaResumo = MetaRotaResumo(
                rota = rota,
                cicloAtual = cicloAtual.numeroCiclo,
                anoCiclo = cicloAtual.ano,
                statusCiclo = cicloAtual.status,
                colaboradorResponsavel = colaboradorResponsavel,
                metas = metasComProgresso,
                dataInicioCiclo = cicloAtual.dataInicio,
                dataFimCiclo = cicloAtual.dataFim,
                ultimaAtualizacao = Date()
            )

            Timber.d("MetaRotaResumo criado para %s", rota.nome)
            return metaRotaResumo
        } catch (e: Exception) {
            Timber.e(e, "Erro ao criar resumo de metas para rota %s: %s", rota.nome, e.message)
            return null
        }
    }

    /**
     * Calcula o progresso real das metas baseado nos dados do sistema
     */
    private suspend fun calcularProgressoMetas(
        metas: List<MetaColaborador>,
        rotaId: Long,
        cicloId: Long
    ): List<MetaColaborador> {
        Timber.d("Calculando progresso para %s metas (rota=%s, ciclo=%s)", metas.size, rotaId, cicloId)
        val metasAtualizadas = mutableListOf<MetaColaborador>()

        for (meta in metas) {
            Timber.d("Processando meta: %s (id=%s)", meta.tipoMeta, meta.id)

            val valorAtual = when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO -> {
                    val faturamento = calcularFaturamentoAtual(rotaId, cicloId)
                    Timber.d("Faturamento calculado: %s", faturamento)
                    faturamento
                }
                TipoMeta.CLIENTES_ACERTADOS -> {
                    val clientes = calcularClientesAcertados(rotaId, cicloId)
                    Timber.d("Clientes acertados: %s", clientes)
                    clientes
                }
                TipoMeta.MESAS_LOCADAS -> {
                    val mesas = calcularMesasLocadas(rotaId)
                    Timber.d("Mesas locadas: %s", mesas)
                    mesas
                }
                TipoMeta.TICKET_MEDIO -> {
                    val ticket = calcularTicketMedio(rotaId, cicloId)
                    Timber.d("Ticket medio: %s", ticket)
                    ticket
                }
            }

            val metaAtualizada = if (meta.valorAtual != valorAtual) meta.copy(valorAtual = valorAtual) else meta
            metasAtualizadas.add(metaAtualizada)

            if (meta.valorAtual != valorAtual) {
                Timber.d("Meta atualizada: %s atual=%s meta=%s", meta.tipoMeta, valorAtual, meta.valorMeta)
            }

            // Atualizar no banco de dados apenas se houve mudança
            try {
                if (meta.valorAtual != valorAtual) {
                    appRepository.atualizarValorAtualMeta(meta.id, valorAtual)
                    Timber.d("Meta %s persistida", meta.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao atualizar meta %s no banco: %s", meta.id, e.message)
            }
        }

        Timber.d("Progresso calculado para %s metas", metasAtualizadas.size)
        return metasAtualizadas
    }

    /**
     * Calcula o faturamento atual da rota no ciclo
     */
    private suspend fun calcularFaturamentoAtual(rotaId: Long, cicloId: Long): Double {
        return try {
            val acertos = appRepository.buscarAcertosPorRotaECiclo(rotaId, cicloId)
            acertos.sumOf { it.valorTotal }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao calcular faturamento: %s", e.message)
            0.0
        }
    }

    /**
     * Calcula o percentual de clientes acertados
     */
    private suspend fun calcularClientesAcertados(rotaId: Long, cicloId: Long): Double {
        return try {
            val totalClientes = appRepository.contarClientesAtivosPorRota(rotaId)
            val clientesAcertados = appRepository.contarClientesAcertadosPorRotaECiclo(rotaId, cicloId)

            if (totalClientes > 0) {
                (clientesAcertados.toDouble() / totalClientes.toDouble()) * 100.0
            } else 0.0
        } catch (e: Exception) {
            Timber.e(e, "Erro ao calcular clientes acertados: %s", e.message)
            0.0
        }
    }

    /**
     * Calcula a quantidade de mesas locadas
     */
    private suspend fun calcularMesasLocadas(rotaId: Long): Double {
        return try {
            val mesasLocadas = appRepository.contarMesasLocadasPorRota(rotaId)
            mesasLocadas.toDouble()
        } catch (e: Exception) {
            Timber.e(e, "Erro ao calcular mesas locadas: %s", e.message)
            0.0
        }
    }

    /**
     * Calcula o ticket médio por mesa
     */
    private suspend fun calcularTicketMedio(rotaId: Long, cicloId: Long): Double {
        return try {
            val faturamento = calcularFaturamentoAtual(rotaId, cicloId)
            val mesasLocadas = calcularMesasLocadas(rotaId)

            if (mesasLocadas > 0) {
                faturamento / mesasLocadas
            } else 0.0
        } catch (e: Exception) {
            Timber.e(e, "Erro ao calcular ticket medio: %s", e.message)
            0.0
        }
    }

    /**
     * Inicia atualização periódica (a cada 30 segundos)
     */
    private fun iniciarAutoRefresh() {
        viewModelScope.launch {
            while (isAutoRefreshEnabled) {
                try {
                    carregarMetasRotas()
                } catch (_: Exception) {
                }
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    /**
     * Para a atualização periódica
     */
    fun pararAtualizacaoTempoReal() {
        isAutoRefreshEnabled = false
    }

    /**
     * Aplica filtro por tipo de meta
     */
    fun aplicarFiltroTipoMeta(tipoMeta: TipoMeta?) {
        _filtroTipoMeta.value = tipoMeta
    }

    /**
     * Aplica filtro por status
     */
    fun aplicarFiltroStatus(status: StatusFiltroMeta?) {
        _filtroStatus.value = status
    }

    /**
     * Alterna exibição do histórico
     */
    fun alternarHistorico() {
        _mostrarHistorico.value = !_mostrarHistorico.value
        // Recarregar metas com a nova configuração
        carregarMetasRotas()
    }

    /**
     * Limpa mensagem
     */
    fun limparMensagem() {
        _message.value = null
    }

    /**
     * Retorna metas filtradas
     */
    fun getMetasFiltradas(): StateFlow<List<MetaRotaResumo>> {
        return combine(
            metasRotas,
            filtroTipoMeta,
            filtroStatus
        ) { metas, tipoFiltro, statusFiltro ->
            var resultado = metas

            // Aplicar filtro por tipo de meta
            if (tipoFiltro != null) {
                resultado = resultado.map { metaRota ->
                    metaRota.copy(
                        metas = metaRota.metas.filter { it.tipoMeta == tipoFiltro }
                    )
                }.filter { it.metas.isNotEmpty() }
            }

            // Aplicar filtro por status
            if (statusFiltro != null) {
                resultado = resultado.filter { metaRota ->
                    when (statusFiltro) {
                        StatusFiltroMeta.PROXIMAS -> metaRota.temMetasProximas()
                        StatusFiltroMeta.ATINGIDAS -> metaRota.temMetasAtingidas()
                        StatusFiltroMeta.EM_ANDAMENTO -> metaRota.statusCiclo == StatusCicloAcerto.EM_ANDAMENTO
                        StatusFiltroMeta.FINALIZADAS -> metaRota.statusCiclo == StatusCicloAcerto.FINALIZADO
                    }
                }
            }

            resultado
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Gera notificações para metas próximas de serem atingidas
     */
    private fun gerarNotificacoesMetas(metasRotas: List<MetaRotaResumo>) {
        val notificacoes = mutableListOf<NotificacaoMeta>()

        metasRotas.forEach { metaRota ->
            metaRota.metas.forEach { meta ->
                val progresso = when (meta.tipoMeta) {
                    TipoMeta.FATURAMENTO -> calcularProgressoFaturamento(meta)
                    TipoMeta.CLIENTES_ACERTADOS -> calcularProgressoClientesAcertados(meta)
                    TipoMeta.MESAS_LOCADAS -> calcularProgressoMesasLocadas(meta)
                    TipoMeta.TICKET_MEDIO -> calcularProgressoTicketMedio(meta)
                }

                when {
                    progresso >= 100.0 -> {
                        notificacoes.add(
                            NotificacaoMeta(
                                tipo = TipoNotificacaoMeta.META_ATINGIDA,
                                rota = metaRota.rota.nome,
                                meta = getTipoMetaFormatado(meta.tipoMeta),
                                progresso = progresso,
                                mensagem = "Meta de ${getTipoMetaFormatado(meta.tipoMeta)} atingida na rota ${metaRota.rota.nome}!"
                            )
                        )
                    }
                    progresso >= 90.0 -> {
                        notificacoes.add(
                            NotificacaoMeta(
                                tipo = TipoNotificacaoMeta.META_PROXIMA,
                                rota = metaRota.rota.nome,
                                meta = getTipoMetaFormatado(meta.tipoMeta),
                                progresso = progresso,
                                mensagem = "Meta de ${getTipoMetaFormatado(meta.tipoMeta)} está 90% completa na rota ${metaRota.rota.nome}!"
                            )
                        )
                    }
                    progresso >= 80.0 -> {
                        notificacoes.add(
                            NotificacaoMeta(
                                tipo = TipoNotificacaoMeta.META_PROXIMA,
                                rota = metaRota.rota.nome,
                                meta = getTipoMetaFormatado(meta.tipoMeta),
                                progresso = progresso,
                                mensagem = "Meta de ${getTipoMetaFormatado(meta.tipoMeta)} está 80% completa na rota ${metaRota.rota.nome}!"
                            )
                        )
                    }
                }
            }
        }

        _notificacoes.value = notificacoes
    }

    private fun calcularProgressoFaturamento(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }

    private fun calcularProgressoClientesAcertados(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }

    private fun calcularProgressoMesasLocadas(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }

    private fun calcularProgressoTicketMedio(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }

    private fun getTipoMetaFormatado(tipoMeta: TipoMeta): String {
        return when (tipoMeta) {
            TipoMeta.FATURAMENTO -> "Faturamento"
            TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados"
            TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
            TipoMeta.TICKET_MEDIO -> "Ticket Médio"
        }
    }

    override fun onCleared() {
        super.onCleared()
        pararAtualizacaoTempoReal()
    }
}

/**
 * Enum para filtros de status das metas
 */
enum class StatusFiltroMeta {
    PROXIMAS,      // Metas próximas de serem atingidas (80%+)
    ATINGIDAS,     // Metas já atingidas (100%)
    EM_ANDAMENTO,  // Ciclos em andamento
    FINALIZADAS    // Ciclos finalizados
}

/**
 * Data class para notificações de metas
 */
 data class NotificacaoMeta(
    val tipo: TipoNotificacaoMeta,
    val rota: String,
    val meta: String,
    val progresso: Double,
    val mensagem: String,
    val timestamp: Date = Date()
)

/**
 * Enum para tipos de notificação de meta
 */
enum class TipoNotificacaoMeta {
    META_PROXIMA,   // Meta próxima de ser atingida (80%+)
    META_ATINGIDA   // Meta atingida (100%)
}
