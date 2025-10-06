package com.example.gestaobilhares.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DashboardViewModel constructor(
    private val repository: AppRepository
) : ViewModel() {

    data class CicloInfo(val id: Long, val numero: Int, val descricao: String)

    data class Fatia(val label: String, val valor: Double)

    private val _anos = MutableLiveData<List<Int>>()
    val anos: LiveData<List<Int>> = _anos

    private val _anoSelecionado = MutableLiveData<Int>()
    val anoSelecionado: LiveData<Int> = _anoSelecionado

    private val _ciclos = MutableLiveData<List<CicloInfo>>()
    val ciclos: LiveData<List<CicloInfo>> = _ciclos

    private val _cicloSelecionado = MutableLiveData<CicloInfo?>()
    val cicloSelecionado: LiveData<CicloInfo?> = _cicloSelecionado

    private val _faturamentoPorRota = MutableLiveData<List<Fatia>>()
    val faturamentoPorRota: LiveData<List<Fatia>> = _faturamentoPorRota

    private val _despesaPorCategoria = MutableLiveData<List<Fatia>>()
    val despesaPorCategoria: LiveData<List<Fatia>> = _despesaPorCategoria

    private val moeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    init {
        carregarAnosECiclos()
    }

    private fun carregarAnosECiclos() {
        viewModelScope.launch {
            val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val anosDisponiveis = repository.obterTodosCiclos().first().map { it.ano }.distinct().sortedDescending()
            val anosLista = if (anosDisponiveis.isEmpty()) listOf(anoAtual) else anosDisponiveis
            _anos.value = anosLista
            _anoSelecionado.value = anosLista.first()

            val ciclosFixos = listOf(CicloInfo(0L, 0, "Todos")) + (1..12).map { n -> CicloInfo(id = n.toLong(), numero = n, descricao = "${n}º Ciclo") }
            _ciclos.value = ciclosFixos
            _cicloSelecionado.value = ciclosFixos.first()
            carregarGraficos(ciclosFixos.first())
        }
    }

    fun selecionarAno(pos: Int) {
        val lista = _anos.value ?: return
        if (pos in lista.indices) {
            _anoSelecionado.value = lista[pos]
            // Recalcular gráficos para o ano selecionado com ciclo atual
            _cicloSelecionado.value?.let { carregarGraficos(it) }
        }
    }

    fun selecionarCiclo(pos: Int) {
        val lista = _ciclos.value ?: return
        if (pos in lista.indices) {
            _cicloSelecionado.value = lista[pos]
            carregarGraficos(lista[pos])
        }
    }

    private fun carregarGraficos(ciclo: CicloInfo) {
        viewModelScope.launch {
            val rotas: List<Rota> = repository.obterTodasRotas().first()
            val rotaNomePorId = rotas.associateBy({ it.id }, { it.nome })
            val ano = _anoSelecionado.value ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

            val acertos = if (ciclo.id == 0L) {
                // Todos os ciclos do ano selecionado: filtra por dataAcerto
                repository.obterTodosAcertos().first().filter { ac ->
                    val cal = java.util.Calendar.getInstance()
                    cal.time = ac.dataAcerto
                    cal.get(java.util.Calendar.YEAR) == ano
                }
            } else {
                repository.buscarAcertosPorCicloId(ciclo.id).first()
            }

            val faturamento = acertos.groupBy { it.rotaId ?: -1L }
                .map { (rotaId, lista) ->
                    val total = lista.sumOf { it.valorRecebido }
                    val label = rotaNomePorId[rotaId] ?: if (rotaId == -1L) "Sem Rota" else "Rota ${rotaId}"
                    Fatia(label, total)
                }
                .sortedByDescending { it.valor }
            _faturamentoPorRota.value = faturamento

            // Despesas por categoria (ciclo)
            val despesas = if (ciclo.id == 0L) {
                // Todos os ciclos do ano: buscar por período de ano
                val dataInicio = java.time.LocalDateTime.of(ano, 1, 1, 0, 0)
                val dataFim = java.time.LocalDateTime.of(ano, 12, 31, 23, 59)
                repository.getDespesasPorAno(ano, 0L).map { rel ->
                    com.example.gestaobilhares.data.entities.Despesa(
                        id = rel.id,
                        rotaId = 0L,
                        descricao = rel.descricao,
                        valor = rel.valor,
                        categoria = rel.categoria,
                        dataHora = java.time.LocalDateTime.of(ano, 1, 1, 0, 0)
                    )
                }
            } else {
                repository.buscarDespesasPorCicloId(ciclo.id).first()
            }
            val porCategoria = despesas.groupBy { it.categoria }.map { (cat, lista) ->
                Fatia(cat, lista.sumOf { it.valor })
            }
            _despesaPorCategoria.value = porCategoria
        }
    }
}



