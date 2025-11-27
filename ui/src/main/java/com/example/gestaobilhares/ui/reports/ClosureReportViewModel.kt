package com.example.gestaobilhares.ui.reports

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

class ClosureReportViewModel constructor(
    private val repository: AppRepository
) : ViewModel() {

    data class AcertoInfo(val id: Long, val numero: Int, val descricao: String)
    data class LinhaDetalhe(val rota: String, val faturamento: Double, val despesas: Double, val lucro: Double)
    data class Resumo(
        val faturamentoTotal: Double, 
        val despesasTotal: Double, 
        val lucroLiquido: Double,
        val lucroRossiny: Double,
        val lucroPetrina: Double,
        val despesasRotas: Double,
        val despesasGlobais: Double,
        val comissaoMotorista: Double,
        val comissaoIltair: Double,
        val totalDescontos: Double
    ) : java.io.Serializable

    // Estrutura para dados dos gráficos
    data class ChartData(
        val faturamentoPorRota: Map<String, Double>,
        val despesasPorTipo: Map<String, Double>
    ) : java.io.Serializable

    private val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private val _anos = MutableLiveData<List<Int>>()
    val anos: LiveData<List<Int>> = _anos
    private val _acertos = MutableLiveData<List<AcertoInfo>>()
    val acertos: LiveData<List<AcertoInfo>> = _acertos

    private val _resumo = MutableLiveData<Resumo>()
    val resumo: LiveData<Resumo> = _resumo
    private val _detalhes = MutableLiveData<List<LinhaDetalhe>>()
    val detalhes: LiveData<List<LinhaDetalhe>> = _detalhes

    // ✅ NOVO: total de mesas locadas reais no período selecionado
    private val _totalMesasLocadas = MutableLiveData<Int>()
    val totalMesasLocadas: LiveData<Int> = _totalMesasLocadas

    private var anoSelecionado: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    private var acertoSelecionado: AcertoInfo = AcertoInfo(0L, 0, "Todos")

    init {
        viewModelScope.launch {
            val anosDisponiveis = repository.obterTodosCiclos().first().map { it.ano }.distinct().sortedDescending()
            _anos.value = if (anosDisponiveis.isEmpty()) listOf(anoSelecionado) else anosDisponiveis
            val acertos = listOf(AcertoInfo(0L, 0, "Todos")) + (1..12).map { n -> AcertoInfo(n.toLong(), n, "${n}º Acerto") }
            _acertos.value = acertos
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

    fun selecionarAcerto(pos: Int) {
        val lista = _acertos.value ?: return
        if (pos in lista.indices) {
            acertoSelecionado = lista[pos]
            calcular()
        }
    }

    private fun calcular() {
        viewModelScope.launch {
            val rotas: List<Rota> = repository.obterTodasRotas().first()
            val rotaNomePorId = rotas.associateBy({ it.id }, { it.nome })

            val acertos = if (acertoSelecionado.numero == 0) {
                repository.obterTodosAcertos().first().filter { ac ->
                    val cal = java.util.Calendar.getInstance()
                    cal.time = ac.dataAcerto
                    cal.get(java.util.Calendar.YEAR) == anoSelecionado
                }
            } else {
                // Todos os acertos dos ciclos com mesmo número no ano selecionado, para todas as rotas
                val ciclosDoAnoNumero = repository.obterTodosCiclos().first()
                    .filter { it.ano == anoSelecionado && it.numeroCiclo == acertoSelecionado.numero }
                ciclosDoAnoNumero.flatMap { ciclo ->
                    repository.buscarAcertosPorCicloId(ciclo.id).first()
                }
            }

            val faturamentoPorRota = acertos.groupBy { it.rotaId ?: -1L }.mapValues { (_, lista) ->
                lista.sumOf { it.valorRecebido }
            }

            val despesas = if (acertoSelecionado.numero == 0) {
                // Ano inteiro: despesas de todas as rotas (por cicloId) + globais do ano (somatório por mesclagem na camada de apresentação)
                // Usamos o método existente (por ano) que retorna despesas por rota; globais serão consideradas no cálculo final via novo método de AppRepository (ajuste posterior, se necessário).
                repository.getDespesasPorAno(anoSelecionado, 0L)
            } else {
                // Somar despesas de todos os ciclos do mesmo número no ano (por rota)
                val ciclosDoAnoNumero = repository.obterTodosCiclos().first()
                    .filter { it.ano == anoSelecionado && it.numeroCiclo == acertoSelecionado.numero }
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
            val totalDespesasGlobais = if (acertoSelecionado.numero == 0) {
                // Ano inteiro: somatório de todos os 12 ciclos do ano para globais
                (1..12).sumOf { numero ->
                    try { repository.somarDespesasGlobaisPorCiclo(anoSelecionado, numero) } catch (_: Exception) { 0.0 }
                }
            } else {
                try { repository.somarDespesasGlobaisPorCiclo(anoSelecionado, acertoSelecionado.numero) } catch (_: Exception) { 0.0 }
            }
            // ✅ NOVO: Calcular comissões de motorista e Iltair
            val (totalComissaoMotorista, totalComissaoIltair) = if (acertoSelecionado.numero == 0) {
                // Ano inteiro: somar comissões de todos os ciclos do ano
                repository.calcularComissoesPorAno(anoSelecionado)
            } else {
                // Acerto específico: somar comissões de todos os ciclos com mesmo número no ano
                repository.calcularComissoesPorAnoECiclo(anoSelecionado, acertoSelecionado.numero)
            }
            
            // ✅ NOVO: Calcular total de descontos
            val totalDescontos = if (acertoSelecionado.numero == 0) {
                // Ano inteiro: somar descontos de todos os ciclos do ano
                val ciclosDoAno = repository.obterTodosCiclos().first().filter { it.ano == anoSelecionado }
                ciclosDoAno.sumOf { ciclo ->
                    try { repository.calcularTotalDescontosPorCiclo(ciclo.id) } catch (_: Exception) { 0.0 }
                }
            } else {
                // Acerto específico: somar descontos de todos os ciclos com mesmo número no ano
                val ciclosDoAnoNumero = repository.obterTodosCiclos().first()
                    .filter { it.ano == anoSelecionado && it.numeroCiclo == acertoSelecionado.numero }
                ciclosDoAnoNumero.sumOf { ciclo ->
                    try { repository.calcularTotalDescontosPorCiclo(ciclo.id) } catch (_: Exception) { 0.0 }
                }
            }
            
            val totalDespesas = totalDespesasRotas + totalDespesasGlobais + totalComissaoMotorista + totalComissaoIltair
            val lucroLiquido = totalFaturamento - totalDespesas
            val lucroRossiny = lucroLiquido * 0.6
            val lucroPetrina = lucroLiquido * 0.4
            
            _detalhes.value = detalhes
            _resumo.value = Resumo(
                faturamentoTotal = totalFaturamento,
                despesasTotal = totalDespesas,
                lucroLiquido = lucroLiquido,
                lucroRossiny = lucroRossiny,
                lucroPetrina = lucroPetrina,
                despesasRotas = totalDespesasRotas,
                despesasGlobais = totalDespesasGlobais,
                comissaoMotorista = totalComissaoMotorista,
                comissaoIltair = totalComissaoIltair,
                totalDescontos = totalDescontos
            )

            // ✅ NOVO: calcular total de mesas locadas reais (distintas) no período
            val cicloIds = if (acertoSelecionado.numero == 0) {
                repository.obterTodosCiclos().first().filter { it.ano == anoSelecionado }.map { it.id }
            } else {
                repository.obterTodosCiclos().first()
                    .filter { it.ano == anoSelecionado && it.numeroCiclo == acertoSelecionado.numero }
                    .map { it.id }
            }
            _totalMesasLocadas.value = repository.contarMesasPorCiclos(cicloIds)
        }
    }

    /**
     * Gera dados para os gráficos do relatório
     */
    suspend fun generateChartData(): ChartData {
        android.util.Log.d("ChartData", "Gerando dados para acerto: ${acertoSelecionado.numero}, ano: $anoSelecionado")
        
        val acertos = if (acertoSelecionado.numero == 0) {
            // Para relatório anual, buscar todos os acertos do ano
            val todosAcertos = repository.obterTodosAcertos().first()
            android.util.Log.d("ChartData", "Total de acertos encontrados: ${todosAcertos.size}")
            todosAcertos.filter { ac ->
                val cal = java.util.Calendar.getInstance()
                cal.time = ac.dataAcerto
                val anoAcerto = cal.get(java.util.Calendar.YEAR)
                android.util.Log.d("ChartData", "Acerto ano: $anoAcerto, filtro: $anoSelecionado, valor: ${ac.valorRecebido}")
                anoAcerto == anoSelecionado
            }
        } else {
            // Para relatório por acerto específico
            val ciclosDoAnoNumero = repository.obterTodosCiclos().first()
                .filter { it.ano == anoSelecionado && it.numeroCiclo == acertoSelecionado.numero }
            android.util.Log.d("ChartData", "Ciclos encontrados: ${ciclosDoAnoNumero.size}")
            ciclosDoAnoNumero.flatMap { ciclo ->
                val acertosCiclo = repository.buscarAcertosPorCicloId(ciclo.id).first()
                android.util.Log.d("ChartData", "Acertos do ciclo ${ciclo.id}: ${acertosCiclo.size}")
                acertosCiclo
            }
        }
        
        android.util.Log.d("ChartData", "Acertos filtrados: ${acertos.size}")

        // 1. Faturamento por rota
        val rotas = repository.obterTodasRotas().first()
        val rotaNomePorId = rotas.associateBy({ it.id }, { it.nome })
        
        val faturamentoPorRota = acertos.groupBy { it.rotaId ?: -1L }.mapValues { (_, lista) ->
            lista.sumOf { it.valorRecebido }
        }.mapKeys { (rotaId, _) ->
            rotaNomePorId[rotaId] ?: if (rotaId == -1L) "Sem Rota" else "Rota ${rotaId}"
        }
        
        android.util.Log.d("ChartData", "Faturamento por rota: $faturamentoPorRota")

        // 2. Despesas por tipo
        val despesas = if (acertoSelecionado.numero == 0) {
            val despesasAno = repository.getDespesasPorAno(anoSelecionado, 0L)
            android.util.Log.d("ChartData", "Despesas por ano: ${despesasAno.size}")
            despesasAno
        } else {
            val ciclosDoAnoNumero = repository.obterTodosCiclos().first()
                .filter { it.ano == anoSelecionado && it.numeroCiclo == acertoSelecionado.numero }
            val despesasCiclo = ciclosDoAnoNumero.flatMap { ciclo ->
                repository.getDespesasPorCiclo(ciclo.id, 0L)
            }
            android.util.Log.d("ChartData", "Despesas por ciclo: ${despesasCiclo.size}")
            despesasCiclo
        }

        android.util.Log.d("ChartData", "Total de despesas: ${despesas.size}")
        val despesasPorTipo = despesas.groupBy { it.categoria.ifEmpty { "Não categorizado" } }
            .mapValues { (_, lista) -> lista.sumOf { it.valor } }
        
        android.util.Log.d("ChartData", "Despesas por tipo: $despesasPorTipo")

        // 3. Adicionar comissões como tipo "Comissões"
        val (totalComissaoMotorista, totalComissaoIltair) = if (acertoSelecionado.numero == 0) {
            repository.calcularComissoesPorAno(anoSelecionado)
        } else {
            repository.calcularComissoesPorAnoECiclo(anoSelecionado, acertoSelecionado.numero)
        }

        val totalComissoes = totalComissaoMotorista + totalComissaoIltair
        val despesasPorTipoComComissoes = despesasPorTipo.toMutableMap()
        if (totalComissoes > 0) {
            despesasPorTipoComComissoes["Comissões"] = totalComissoes
        }

        return ChartData(
            faturamentoPorRota = faturamentoPorRota,
            despesasPorTipo = despesasPorTipoComComissoes
        )
    }
}

