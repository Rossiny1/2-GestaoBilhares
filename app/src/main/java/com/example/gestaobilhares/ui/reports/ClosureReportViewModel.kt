package com.example.gestaobilhares.ui.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@HiltViewModel
class ClosureReportViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    data class CicloInfo(val id: Long, val numero: Int, val descricao: String)
    data class LinhaDetalhe(val rota: String, val faturamento: Double, val despesas: Double, val lucro: Double)
    data class Resumo(val faturamentoTotal: Double, val despesasTotal: Double, val lucroLiquido: Double)

    private val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private val _anos = MutableLiveData<List<Int>>()
    val anos: LiveData<List<Int>> = _anos
    private val _ciclos = MutableLiveData<List<CicloInfo>>()
    val ciclos: LiveData<List<CicloInfo>> = _ciclos

    private val _resumo = MutableLiveData<Resumo>()
    val resumo: LiveData<Resumo> = _resumo
    private val _detalhes = MutableLiveData<List<LinhaDetalhe>>()
    val detalhes: LiveData<List<LinhaDetalhe>> = _detalhes

    // ✅ NOVO: total de mesas locadas reais no período selecionado
    private val _totalMesasLocadas = MutableLiveData<Int>()
    val totalMesasLocadas: LiveData<Int> = _totalMesasLocadas

    private var anoSelecionado: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    private var cicloSelecionado: CicloInfo = CicloInfo(0L, 0, "Todos")

    init {
        viewModelScope.launch {
            val anosDisponiveis = repository.obterTodosCiclos().first().map { it.ano }.distinct().sortedDescending()
            _anos.value = if (anosDisponiveis.isEmpty()) listOf(anoSelecionado) else anosDisponiveis
            val ciclos = listOf(CicloInfo(0L, 0, "Todos")) + (1..12).map { n -> CicloInfo(n.toLong(), n, "${n}º Ciclo") }
            _ciclos.value = ciclos
            calcular()
        }
    }

    fun selecionarAno(pos: Int) {
        val lista = _anos.value ?: return
        if (pos in lista.indices) {
            anoSelecionado = lista[pos]
            calcular()
        }
    }

    fun selecionarCiclo(pos: Int) {
        val lista = _ciclos.value ?: return
        if (pos in lista.indices) {
            cicloSelecionado = lista[pos]
            calcular()
        }
    }

    private fun calcular() {
        viewModelScope.launch {
            val rotas: List<Rota> = repository.obterTodasRotas().first()
            val rotaNomePorId = rotas.associateBy({ it.id }, { it.nome })

            val acertos = if (cicloSelecionado.numero == 0) {
                repository.obterTodosAcertos().first().filter { ac ->
                    val cal = java.util.Calendar.getInstance()
                    cal.time = ac.dataAcerto
                    cal.get(java.util.Calendar.YEAR) == anoSelecionado
                }
            } else {
                // Todos os acertos dos ciclos com mesmo número no ano selecionado, para todas as rotas
                val ciclosDoAnoNumero = repository.obterTodosCiclos().first()
                    .filter { it.ano == anoSelecionado && it.numeroCiclo == cicloSelecionado.numero }
                ciclosDoAnoNumero.flatMap { ciclo ->
                    repository.buscarAcertosPorCicloId(ciclo.id).first()
                }
            }

            val faturamentoPorRota = acertos.groupBy { it.rotaId ?: -1L }.mapValues { (_, lista) ->
                lista.sumOf { it.valorRecebido }
            }

            val despesas = if (cicloSelecionado.numero == 0) {
                // Ano inteiro: despesas de todas as rotas (por cicloId) + globais do ano (somatório por mesclagem na camada de apresentação)
                // Usamos o método existente (por ano) que retorna despesas por rota; globais serão consideradas no cálculo final via novo método de AppRepository (ajuste posterior, se necessário).
                repository.getDespesasPorAno(anoSelecionado, 0L)
            } else {
                // Somar despesas de todos os ciclos do mesmo número no ano (por rota)
                val ciclosDoAnoNumero = repository.obterTodosCiclos().first()
                    .filter { it.ano == anoSelecionado && it.numeroCiclo == cicloSelecionado.numero }
                ciclosDoAnoNumero.flatMap { ciclo ->
                    repository.getDespesasPorCiclo(ciclo.id, 0L)
                }
            }

            val despesasPorRota = despesas.groupBy { it.rota }.mapValues { (_, lista) ->
                lista.sumOf { it.valor }
            }

            val detalhes = faturamentoPorRota.map { (rotaId, fat) ->
                val nome = rotaNomePorId[rotaId] ?: if (rotaId == -1L) "Sem Rota" else "Rota ${rotaId}"
                val desp = despesasPorRota[nome] ?: 0.0
                LinhaDetalhe(nome, fat, desp, fat - desp)
            }.sortedByDescending { it.faturamento }

            val totalFaturamento = detalhes.sumOf { it.faturamento }
            // ✅ NOVO: Somar também despesas globais
            val totalDespesasRotas = detalhes.sumOf { it.despesas }
            val totalDespesasGlobais = if (cicloSelecionado.numero == 0) {
                // Ano inteiro: somatório de todos os 12 ciclos do ano para globais
                (1..12).sumOf { numero ->
                    try { repository.somarDespesasGlobaisPorCiclo(anoSelecionado, numero) } catch (_: Exception) { 0.0 }
                }
            } else {
                try { repository.somarDespesasGlobaisPorCiclo(anoSelecionado, cicloSelecionado.numero) } catch (_: Exception) { 0.0 }
            }
            val totalDespesas = totalDespesasRotas + totalDespesasGlobais
            _detalhes.value = detalhes
            _resumo.value = Resumo(totalFaturamento, totalDespesas, totalFaturamento - totalDespesas)

            // ✅ NOVO: calcular total de mesas locadas reais (distintas) no período
            val cicloIds = if (cicloSelecionado.numero == 0) {
                repository.obterTodosCiclos().first().filter { it.ano == anoSelecionado }.map { it.id }
            } else {
                repository.obterTodosCiclos().first()
                    .filter { it.ano == anoSelecionado && it.numeroCiclo == cicloSelecionado.numero }
                    .map { it.id }
            }
            _totalMesasLocadas.value = repository.contarMesasPorCiclos(cicloIds)
        }
    }
}


