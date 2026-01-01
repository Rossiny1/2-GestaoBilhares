package com.example.gestaobilhares.ui.metas

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MetasViewModel @Inject constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {
    
    // Repository initialization via constructor

    private val _metasRotas = MutableStateFlow<List<MetaRotaResumo>>(emptyList())
    val metasRotas: StateFlow<List<MetaRotaResumo>> = _metasRotas.asStateFlow()

    // Estados de loading e message já estão no BaseViewModel

    private val _notificacoes = MutableStateFlow<List<NotificacaoMeta>>(emptyList())
    val notificacoes: StateFlow<List<NotificacaoMeta>> = _notificacoes.asStateFlow()

    // Atualização periódica via corrotina
    private var isAutoRefreshEnabled: Boolean = false

    init {
        carregarMetasRotas()
    }

    /**
     * Força o refresh das metas (útil após salvar nova meta)
     */
    fun refreshMetas() {
        logOperation("REFRESH", "Forçando refresh das metas")
        carregarMetasRotas()
    }

    /**
     * Carrega todas as metas das rotas
     */
    fun carregarMetasRotas() {
        viewModelScope.launch {
            try {
                showLoading()
                Timber.d("Carregando metas das rotas")

                // Buscar todas as rotas ativas
                val rotas = appRepository.obterTodasRotas().first().filter { rota -> rota.ativa }
                Timber.d("Rotas ativas: ${rotas.size}")

                val metasRotasList = mutableListOf<MetaRotaResumo>()

                for (rota in rotas) {
                    try {
                        Timber.d("Processando rota: %s (id=%s)", rota.nome, rota.id)
                        
                        // ✅ CORREÇÃO: Sempre mostrar o último ciclo (finalizado ou em andamento)
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
                showError("Erro ao carregar metas: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Cria um resumo de metas para uma rota específica
     */
    private suspend fun criarMetaRotaResumo(rota: Rota): MetaRotaResumo? {
        try {
            Timber.d("════════════════════════════════════════")
            Timber.d("Criando MetaRotaResumo para rota: %s (id=%s)", rota.nome, rota.id)

            // ✅ SIMPLIFICADO: Buscar APENAS ciclo EM_ANDAMENTO
            Timber.d("Buscando ciclo em andamento para rota %s", rota.nome)
            val cicloAtual = appRepository.buscarCicloAtivo(rota.id)
            Timber.d("Resultado buscarCicloAtivo: %s", if (cicloAtual != null) "Ciclo ${cicloAtual.numeroCiclo}/${cicloAtual.ano} (id=${cicloAtual.id}, status=${cicloAtual.status})" else "null")
            
            if (cicloAtual == null) {
                Timber.d("ℹ️ Nenhum ciclo em andamento encontrado para rota %s (id=%s) - não exibindo", rota.nome, rota.id)
                Timber.d("════════════════════════════════════════")
                return null
            }

            Timber.d("✅ Ciclo encontrado: %s/%s (id=%s, status=%s)", cicloAtual.numeroCiclo, cicloAtual.ano, cicloAtual.id, cicloAtual.status)

            // Buscar colaborador responsável principal
            Timber.d("Buscando colaborador responsavel principal para rota %s", rota.nome)
            val colaboradorResponsavel = appRepository.buscarColaboradorResponsavelPrincipal(rota.id)

            if (colaboradorResponsavel != null) {
                Timber.d("Colaborador responsavel encontrado: %s", colaboradorResponsavel.nome)
            } else {
                Timber.w("Nenhum colaborador responsavel encontrado para rota %s", rota.nome)
            }

            // ✅ SIMPLIFICADO: Buscar apenas metas ativas de ciclos em andamento
            Timber.d("Buscando metas para rota %s (id=%s) e ciclo %s/%s (id=%s, status=%s)", rota.nome, rota.id, cicloAtual.numeroCiclo, cicloAtual.ano, cicloAtual.id, cicloAtual.status)
            
            var metas = if (cicloAtual.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO) {
                appRepository.buscarMetasPorRotaECicloAtivo(rota.id, cicloAtual.id)
            } else {
                Timber.d("Ciclo não está em andamento, não exibindo metas")
                emptyList()
            }
            Timber.d("Metas encontradas para ciclo %s: %s", cicloAtual.id, metas.size)
            if (metas.isNotEmpty()) {
                metas.forEach { meta ->
                    Timber.d("  - Meta encontrada: tipo=%s, valorMeta=%.2f, rotaId=%s, cicloId=%s, colaboradorId=%s", 
                        meta.tipoMeta, meta.valorMeta, meta.rotaId, meta.cicloId, meta.colaboradorId)
                }
            }
            
            // ✅ NOVO: Se não encontrou metas no ciclo atual, buscar em todos os ciclos da rota
            if (metas.isEmpty()) {
                Timber.w("⚠️ Nenhuma meta encontrada para ciclo %s, buscando em todos os ciclos da rota", cicloAtual.id)
                val todosCiclos = appRepository.buscarCiclosPorRota(rota.id)
                Timber.d("Total de ciclos encontrados para rota %s: %s", rota.nome, todosCiclos.size)
                
                for (ciclo in todosCiclos) {
                    val metasCiclo = appRepository.buscarMetasPorRotaECiclo(rota.id, ciclo.id)
                    Timber.d("Ciclo %s/%s (id=%s): %s metas", ciclo.numeroCiclo, ciclo.ano, ciclo.id, metasCiclo.size)
                    if (metasCiclo.isNotEmpty()) {
                        Timber.d("✅ Encontradas %s metas no ciclo %s/%s, usando este ciclo", metasCiclo.size, ciclo.numeroCiclo, ciclo.ano)
                        metas = metasCiclo
                        // Atualizar cicloAtual para o ciclo que tem metas e continuar processamento
                        Timber.d("Usando ciclo %s/%s (id=%s) que contém %s metas", ciclo.numeroCiclo, ciclo.ano, ciclo.id, metas.size)
                        
                        // Calcular progresso das metas baseado em dados reais
                        Timber.d("Calculando progresso das metas")
                        val metasComProgresso = calcularProgressoMetas(metas, rota.id, ciclo.id)

                        val metaRotaResumo = MetaRotaResumo(
                            rota = rota,
                            cicloAtual = ciclo.numeroCiclo,
                            anoCiclo = ciclo.ano,
                            statusCiclo = ciclo.status,
                            colaboradorResponsavel = colaboradorResponsavel,
                            metas = metasComProgresso,
                            dataInicioCiclo = ciclo.dataInicio,
                            dataFimCiclo = ciclo.dataFim,
                            ultimaAtualizacao = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual().time
                        )

                        Timber.d("✅ MetaRotaResumo criado para %s com %s metas (ciclo %s/%s)", rota.nome, metasComProgresso.size, ciclo.numeroCiclo, ciclo.ano)
                        Timber.d("════════════════════════════════════════")
                        return metaRotaResumo
                    }
                }
            }

            if (metas.isEmpty()) {
                Timber.w("⚠️ Nenhuma meta encontrada para rota %s em nenhum ciclo", rota.nome)
                Timber.d("════════════════════════════════════════")
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
                    ultimaAtualizacao = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual().time
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
                ultimaAtualizacao = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual().time
            )

            Timber.d("✅ MetaRotaResumo criado para %s com %s metas", rota.nome, metasComProgresso.size)
            Timber.d("════════════════════════════════════════")
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
                    val novasMesas = calcularNovasMesasNoCiclo(rotaId, cicloId)
                    Timber.d("Novas mesas no ciclo: %s", novasMesas)
                    novasMesas
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
     * Calcula a QUANTIDADE de clientes acertados (contagem absoluta, clientes distintos)
     */
    private suspend fun calcularClientesAcertados(rotaId: Long, cicloId: Long): Double {
        return try {
            val clientesAcertados = appRepository.contarClientesAcertadosPorRotaECiclo(rotaId, cicloId)
            clientesAcertados.toDouble()
        } catch (e: Exception) {
            Timber.e(e, "Erro ao calcular clientes acertados: %s", e.message)
            0.0
        }
    }

    /**
     * Calcula a quantidade de mesas locadas
     */
    private suspend fun calcularNovasMesasNoCiclo(rotaId: Long, cicloId: Long): Double {
        return try {
            val novas = appRepository.contarNovasMesasNoCiclo(rotaId, cicloId)
            novas.toDouble()
        } catch (e: Exception) {
            Timber.e(e, "Erro ao calcular novas mesas no ciclo: %s", e.message)
            0.0
        }
    }

    /**
     * Calcula o ticket médio por mesa
     */
    private suspend fun calcularTicketMedio(rotaId: Long, cicloId: Long): Double {
        return try {
            val faturamento = calcularFaturamentoAtual(rotaId, cicloId)
            val clientesAcertados = calcularClientesAcertados(rotaId, cicloId)

            if (clientesAcertados > 0.0) {
                faturamento / clientesAcertados
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
     * Limpa mensagem
     */
    fun limparMensagem() {
        clearMessage()
    }

    /**
     * Retorna metas (sem filtros complexos, apenas rotas em andamento)
     */
    fun getMetasFiltradas(): StateFlow<List<MetaRotaResumo>> {
        return metasRotas.stateIn(
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
 * Data class para notificações de metas
 */
 data class NotificacaoMeta(
    val tipo: TipoNotificacaoMeta,
    val rota: String,
    val meta: String,
    val progresso: Double,
    val mensagem: String,
    val timestamp: Date = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual()
)

/**
 * Enum para tipos de notificação de meta
 */
enum class TipoNotificacaoMeta {
    META_PROXIMA,   // Meta próxima de ser atingida (80%+)
    META_ATINGIDA   // Meta atingida (100%)
}

