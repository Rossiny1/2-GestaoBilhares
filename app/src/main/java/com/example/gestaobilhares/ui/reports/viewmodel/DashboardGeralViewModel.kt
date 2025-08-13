package com.example.gestaobilhares.ui.reports.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardGeralViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val cicloRepo: CicloAcertoRepository,
    private val database: AppDatabase
) : ViewModel() {

    data class Metrics(
        val faturamentoTotal: Double = 0.0,
        val totalClientes: Int = 0,
        val mesasAtivas: Int = 0,
        val ticketMedio: Double = 0.0,
        val acertosRealizados: Int = 0,
        val taxaConversao: Double = 0.0,
        val deltaFaturamentoPercent: Double = 0.0,
        val deltaTicketMedioPercent: Double = 0.0,
        val deltaAcertosPercent: Double = 0.0,
        val deltaTaxaConversaoPercent: Double = 0.0
    )

    private val _metrics = MutableLiveData<Metrics>()
    val metrics: LiveData<Metrics> = _metrics

    private val _detalhamentoRotas = MutableLiveData<List<DetalhamentoRota>>()
    val detalhamentoRotas: LiveData<List<DetalhamentoRota>> = _detalhamentoRotas

    private val currency: NumberFormat = NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))

    private val _anos = MutableLiveData<List<Int>>()
    val anos: LiveData<List<Int>> = _anos

    init {
        viewModelScope.launch {
            try {
                val anosDisponiveis = database.cicloAcertoDao().listarAnosDisponiveis().first().sortedDescending()
                _anos.value = if (anosDisponiveis.isNotEmpty()) anosDisponiveis else listOf(Calendar.getInstance().get(Calendar.YEAR))
                val anoBase = _anos.value!!.first()
                val anoComp = _anos.value!!.getOrNull(1) ?: (anoBase - 1)
                carregarDados(1, anoBase, anoComp)
            } catch (_: Exception) { /* ignore */ }
        }
    }

    fun carregarDados(cicloNumero: Int, anoBase: Int = Calendar.getInstance().get(Calendar.YEAR), anoComparacao: Int? = null) {
        viewModelScope.launch {
            try {
                // Rotas
                val rotas: List<Rota> = appRepository.obterTodasRotas().first()

                // Agregadores globais
                var faturamentoTotal = 0.0
                var faturamentoTotalComp = 0.0
                var totalClientes = 0
                var mesasAtivas = 0
                var acertosRealizados = 0
                var acertosRealizadosComp = 0
                var somaTickets = 0.0
                var contTickets = 0
                var somaTicketsComp = 0.0
                var contTicketsComp = 0

                val detalhamento = mutableListOf<DetalhamentoRota>()

                for (rota in rotas) {
                    // Encontrar ciclo da rota para o número e ano
                    val ciclosRota = database.cicloAcertoDao().listarPorRota(rota.id).first()
                    val ciclo = ciclosRota.firstOrNull { it.numeroCiclo == cicloNumero && it.ano == anoBase }
                    val cicloComp = if (anoComparacao != null) ciclosRota.firstOrNull { it.numeroCiclo == cicloNumero && it.ano == anoComparacao } else null

                    val faturamentoRota = ciclo?.valorTotalAcertado ?: 0.0
                    val faturamentoComp = cicloComp?.valorTotalAcertado ?: 0.0
                    val clientesAcertados = ciclo?.clientesAcertados ?: 0
                    val clientesComp = cicloComp?.clientesAcertados ?: 0

                    // Total clientes da rota
                    val clientesRota = appRepository.obterClientesPorRota(rota.id).first()
                    val totalClientesRota = clientesRota.size

                    // Mesas ativas na rota (qualquer mesa vinculada a clientes da rota)
                    val mesasRota = appRepository.buscarMesasPorRota(rota.id).first()
                    val mesasAtivasRota = mesasRota.size
                    val mesasComp = mesasAtivasRota // sem histórico por ano

                    // Acertos realizados no ciclo
                    val acertosCiclo = if (ciclo != null) database.acertoDao().buscarPorCicloId(ciclo.id).first() else emptyList()
                    val acertosCicloComp = if (cicloComp != null) database.acertoDao().buscarPorCicloId(cicloComp.id).first() else emptyList()
                    val acertosRealizadosRota = acertosCiclo.size
                    val acertosRealizadosRotaComp = acertosCicloComp.size

                    // Ticket médio da rota: faturamento / max(clientesAcertados,1)
                    val ticketMedioRota = if (clientesAcertados > 0) faturamentoRota / clientesAcertados else 0.0

                    // Variáveis globais
                    faturamentoTotal += faturamentoRota
                    faturamentoTotalComp += faturamentoComp
                    totalClientes += totalClientesRota
                    mesasAtivas += mesasAtivasRota
                    acertosRealizados += acertosRealizadosRota
                    acertosRealizadosComp += acertosRealizadosRotaComp
                    if (ticketMedioRota > 0) {
                        somaTickets += ticketMedioRota
                        contTickets += 1
                    }
                    val ticketMedioRotaComp = if (clientesComp > 0) faturamentoComp / clientesComp else 0.0
                    if (ticketMedioRotaComp > 0) {
                        somaTicketsComp += ticketMedioRotaComp
                        contTicketsComp += 1
                    }

                    // Detalhamento por rota (sem comparação por ano aqui)
                    detalhamento += DetalhamentoRota(
                        rota = rota,
                        faturamentoAtual = faturamentoRota,
                        faturamentoComparacao = faturamentoComp,
                        variacaoFaturamento = calcularVariacao(faturamentoRota, faturamentoComp),
                        clientesAtual = clientesAcertados,
                        clientesComparacao = clientesComp,
                        variacaoClientes = calcularVariacao(clientesAcertados.toDouble(), clientesComp.toDouble()),
                        mesasAtual = mesasAtivasRota,
                        mesasComparacao = mesasComp,
                        variacaoMesas = 0.0
                    )
                }

                val ticketMedioGlobal = if (contTickets > 0) somaTickets / contTickets else 0.0
                val ticketMedioGlobalComp = if (contTicketsComp > 0) somaTicketsComp / contTicketsComp else 0.0
                // Taxa de conversão: média ponderada não trivial; usamos (soma clientesAcertados) / totalClientes
                val clientesAcertadosSoma = detalhamento.sumOf { it.clientesAtual }
                val taxaConversao = if (totalClientes > 0) (clientesAcertadosSoma.toDouble() / totalClientes) * 100.0 else 0.0
                val clientesAcertadosSomaComp = detalhamento.sumOf { it.clientesComparacao }
                val taxaConversaoComp = if (totalClientes > 0) (clientesAcertadosSomaComp.toDouble() / totalClientes) * 100.0 else 0.0

                val deltaFaturamento = calcularVariacao(faturamentoTotal, faturamentoTotalComp)
                val deltaTicket = calcularVariacao(ticketMedioGlobal, ticketMedioGlobalComp)
                val deltaAcertos = calcularVariacao(acertosRealizados.toDouble(), acertosRealizadosComp.toDouble())
                val deltaConversao = calcularVariacao(taxaConversao, taxaConversaoComp)

                _metrics.value = Metrics(
                    faturamentoTotal = faturamentoTotal,
                    totalClientes = totalClientes,
                    mesasAtivas = mesasAtivas,
                    ticketMedio = ticketMedioGlobal,
                    acertosRealizados = acertosRealizados,
                    taxaConversao = taxaConversao,
                    deltaFaturamentoPercent = deltaFaturamento,
                    deltaTicketMedioPercent = deltaTicket,
                    deltaAcertosPercent = deltaAcertos,
                    deltaTaxaConversaoPercent = deltaConversao
                )

                _detalhamentoRotas.value = detalhamento.sortedByDescending { it.faturamentoAtual }
            } catch (e: Exception) {
                // Em caso de erro, publicar valores default
                _metrics.value = Metrics()
                _detalhamentoRotas.value = emptyList()
            }
        }
    }

    private fun calcularVariacao(valorAtual: Double, valorComparacao: Double): Double {
        return if (valorComparacao > 0.0) ((valorAtual - valorComparacao) / valorComparacao) * 100.0 else 0.0
    }
}
