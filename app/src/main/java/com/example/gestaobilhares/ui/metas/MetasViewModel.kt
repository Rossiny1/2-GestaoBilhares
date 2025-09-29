package com.example.gestaobilhares.ui.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
                android.util.Log.d("MetasViewModel", "Carregando metas das rotas")

                // Buscar todas as rotas ativas
                val rotas = appRepository.obterTodasRotas().first().filter { rota -> rota.ativa }
                android.util.Log.d("MetasViewModel", "Rotas ativas: ${rotas.size}")

                val metasRotasList = mutableListOf<MetaRotaResumo>()

                for (rota in rotas) {
                    try {
                        android.util.Log.d("MetasViewModel", "Processando rota: ${rota.nome} (id=${rota.id})")
                        val metaRota = criarMetaRotaResumo(rota)
                        if (metaRota != null) {
                            android.util.Log.d("MetasViewModel", "MetaRota criada para ${rota.nome}: ${metaRota.metas.size} metas")
                            metasRotasList.add(metaRota)
                        } else {
                            android.util.Log.w("MetasViewModel", "MetaRota nao criada para ${rota.nome}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MetasViewModel", "Erro ao carregar metas da rota ${rota.nome}: ${e.message}", e)
                    }
                }

                android.util.Log.d("MetasViewModel", "Total de MetaRotas: ${metasRotasList.size}")
                _metasRotas.value = metasRotasList

                // Gerar notificações para metas próximas
                gerarNotificacoesMetas(metasRotasList)
            } catch (e: Exception) {
                android.util.Log.e("MetasViewModel", "Erro ao carregar metas: ${e.message}", e)
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
            android.util.Log.d("MetasViewModel", "Criando MetaRotaResumo para rota: ${rota.nome} (id=${rota.id})")

            // Buscar ciclo atual ou histórico
            val cicloAtual = if (_mostrarHistorico.value) {
                android.util.Log.d("MetasViewModel", "Buscando ultimo ciclo finalizado para rota ${rota.nome}")
                appRepository.buscarUltimoCicloFinalizadoPorRota(rota.id)
            } else {
                android.util.Log.d("MetasViewModel", "Buscando ciclo atual para rota ${rota.nome}")
                appRepository.buscarCicloAtualPorRota(rota.id)
            }

            if (cicloAtual == null) {
                android.util.Log.w("MetasViewModel", "Nenhum ciclo encontrado para rota ${rota.nome}")
                return null
            }

            android.util.Log.d("MetasViewModel", "Ciclo encontrado: ${cicloAtual.numeroCiclo}/${cicloAtual.ano} (id=${cicloAtual.id})")

            // Buscar colaborador responsável principal
            android.util.Log.d("MetasViewModel", "Buscando colaborador responsavel principal para rota ${rota.nome}")
            val colaboradorResponsavel = appRepository.buscarColaboradorResponsavelPrincipal(rota.id)

            if (colaboradorResponsavel != null) {
                android.util.Log.d("MetasViewModel", "Colaborador responsavel encontrado: ${colaboradorResponsavel.nome}")
            } else {
                android.util.Log.w("MetasViewModel", "Nenhum colaborador responsavel encontrado para rota ${rota.nome}")
            }

            // Buscar metas do ciclo atual
            android.util.Log.d("MetasViewModel", "Buscando metas para rota ${rota.id} e ciclo ${cicloAtual.id}")
            val metas = appRepository.buscarMetasPorRotaECiclo(rota.id, cicloAtual.id)
            android.util.Log.d("MetasViewModel", "Metas encontradas: ${metas.size}")

            if (metas.isEmpty()) {
                android.util.Log.w("MetasViewModel", "Nenhuma meta encontrada para rota ${rota.nome} e ciclo ${cicloAtual.id}")
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
            android.util.Log.d("MetasViewModel", "Calculando progresso das metas")
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

            android.util.Log.d("MetasViewModel", "MetaRotaResumo criado para ${rota.nome}")
            return metaRotaResumo
        } catch (e: Exception) {
            android.util.Log.e("MetasViewModel", "Erro ao criar resumo de metas para rota ${rota.nome}: ${e.message}", e)
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
        android.util.Log.d("MetasViewModel", "Calculando progresso para ${metas.size} metas (rota=$rotaId, ciclo=$cicloId)")
        val metasAtualizadas = mutableListOf<MetaColaborador>()

        for (meta in metas) {
            android.util.Log.d("MetasViewModel", "Processando meta: ${meta.tipoMeta} (id=${meta.id})")

            val valorAtual = when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO -> {
                    val faturamento = calcularFaturamentoAtual(rotaId, cicloId)
                    android.util.Log.d("MetasViewModel", "Faturamento calculado: $faturamento")
                    faturamento
                }
                TipoMeta.CLIENTES_ACERTADOS -> {
                    val clientes = calcularClientesAcertados(rotaId, cicloId)
                    android.util.Log.d("MetasViewModel", "Clientes acertados: $clientes")
                    clientes
                }
                TipoMeta.MESAS_LOCADAS -> {
                    val mesas = calcularMesasLocadas(rotaId)
                    android.util.Log.d("MetasViewModel", "Mesas locadas: $mesas")
                    mesas
                }
                TipoMeta.TICKET_MEDIO -> {
                    val ticket = calcularTicketMedio(rotaId, cicloId)
                    android.util.Log.d("MetasViewModel", "Ticket medio: $ticket")
                    ticket
                }
            }

            val metaAtualizada = if (meta.valorAtual != valorAtual) meta.copy(valorAtual = valorAtual) else meta
            metasAtualizadas.add(metaAtualizada)

            if (meta.valorAtual != valorAtual) {
                android.util.Log.d("MetasViewModel", "Meta atualizada: ${meta.tipoMeta} atual=$valorAtual meta=${meta.valorMeta}")
            }

            // Atualizar no banco de dados apenas se houve mudança
            try {
                if (meta.valorAtual != valorAtual) {
                    appRepository.atualizarValorAtualMeta(meta.id, valorAtual)
                    android.util.Log.d("MetasViewModel", "Meta ${meta.id} persistida")
                }
            } catch (e: Exception) {
                android.util.Log.e("MetasViewModel", "Erro ao atualizar meta ${meta.id} no banco: ${e.message}")
            }
        }

        android.util.Log.d("MetasViewModel", "Progresso calculado para ${metasAtualizadas.size} metas")
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
            android.util.Log.e("MetasViewModel", "Erro ao calcular faturamento: ${e.message}", e)
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
            android.util.Log.e("MetasViewModel", "Erro ao calcular clientes acertados: ${e.message}", e)
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
            android.util.Log.e("MetasViewModel", "Erro ao calcular mesas locadas: ${e.message}", e)
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
            android.util.Log.e("MetasViewModel", "Erro ao calcular ticket médio: ${e.message}", e)
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
