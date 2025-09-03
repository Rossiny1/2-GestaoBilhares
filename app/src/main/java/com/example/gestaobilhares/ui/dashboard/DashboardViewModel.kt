package com.example.gestaobilhares.ui.dashboard

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
class DashboardViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    data class CicloInfo(val id: Long, val numero: Int, val descricao: String)

    data class Fatia(val label: String, val valor: Double)

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
        carregarCiclos()
    }

    private fun carregarCiclos() {
        viewModelScope.launch {
            // Ciclos 1..12; id do ciclo será resolvido via repository quando houver
            val ciclosFixos = (1..12).map { n -> CicloInfo(id = n.toLong(), numero = n, descricao = "${n}º Ciclo") }
            _ciclos.value = ciclosFixos
            _cicloSelecionado.value = ciclosFixos.firstOrNull()
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
            // Faturamento por rota no ciclo (mock até termos soma real por rota/ciclo)
            val rotas: List<Rota> = repository.obterTodasRotas().first()
            val faturamento = if (rotas.isEmpty()) {
                emptyList()
            } else {
                // TODO: substituir quando houver soma de acertos por rota/ciclo
                rotas.map { r -> Fatia(r.nome, 0.0) }
            }
            _faturamentoPorRota.value = faturamento

            // Despesas por categoria (ciclo)
            val despesas = repository.buscarDespesasPorCicloId(ciclo.id).first()
            val porCategoria = despesas.groupBy { it.categoria }.map { (cat, lista) ->
                Fatia(cat, lista.sumOf { it.valor })
            }
            _despesaPorCategoria.value = porCategoria
        }
    }
}


