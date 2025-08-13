package com.example.gestaobilhares.ui.reports.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.AppRepository.CicloInfo
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class DespesasCategoriaViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _estatisticas = MutableLiveData<EstatisticasDespesas>()
    val estatisticas: LiveData<EstatisticasDespesas> = _estatisticas

    private val _despesasPorCategoria = MutableLiveData<List<DespesaCategoriaItem>>()
    val despesasPorCategoria: LiveData<List<DespesaCategoriaItem>> = _despesasPorCategoria

    private val _ciclos = MutableLiveData<List<CicloInfo>>()
    val ciclos: LiveData<List<CicloInfo>> = _ciclos

    private val _rotas = MutableLiveData<List<Rota>>()
    val rotas: LiveData<List<Rota>> = _rotas

    private val _anos = MutableLiveData<List<Int>>()
    val anos: LiveData<List<Int>> = _anos

    private val _categorias = MutableLiveData<List<String>>()
    val categorias: LiveData<List<String>> = _categorias

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Filtros ativos
    private var filtroTipo: TipoFiltro = TipoFiltro.CICLO_ESPECIFICO
    private var cicloSelecionado: Long = 0
    private var anoSelecionado: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var rotaSelecionada: Long = 0
    private var categoriaSelecionada: String? = null

    init {
        carregarDadosIniciais()
    }

    private fun carregarDadosIniciais() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar dados básicos
                val ciclosData = repository.getCiclos()
                val rotasData = repository.getRotas()
                
                _ciclos.value = ciclosData
                _rotas.value = rotasData
                
                // Gerar lista de anos (últimos 5 anos)
                val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
                val anosList = (anoAtual - 4..anoAtual).toList()
                _anos.value = anosList
                
                // Carregar categorias de despesas
                val categoriasData = repository.getCategoriasDespesas()
                _categorias.value = categoriasData
                
                // Selecionar valores padrão
                if (ciclosData.isNotEmpty()) {
                    selecionarCiclo(ciclosData.first().numero.toLong())
                }
                if (rotasData.isNotEmpty()) {
                    selecionarRota(rotasData.first().id)
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun selecionarTipoFiltro(tipo: TipoFiltro) {
        filtroTipo = tipo
        atualizarRelatorio()
    }

    fun selecionarCiclo(cicloId: Long) {
        cicloSelecionado = cicloId
        atualizarRelatorio()
    }

    fun selecionarAno(ano: Int) {
        anoSelecionado = ano
        atualizarRelatorio()
    }

    fun selecionarRota(rotaId: Long) {
        rotaSelecionada = rotaId
        atualizarRelatorio()
    }

    fun selecionarCategoria(categoria: String?) {
        categoriaSelecionada = categoria
        atualizarRelatorio()
    }

    private fun atualizarRelatorio() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val despesas = when (filtroTipo) {
                    TipoFiltro.CICLO_ESPECIFICO -> {
                        repository.getDespesasPorCiclo(cicloSelecionado, rotaSelecionada)
                    }
                    TipoFiltro.CONSOLIDADO_CICLOS -> {
                        // Buscar todos os ciclos do mesmo número no ano selecionado
                        val numeroCiclo = _ciclos.value?.find { it.numero.toLong() == cicloSelecionado }?.numero
                        if (numeroCiclo != null) {
                            repository.getDespesasConsolidadasCiclos(numeroCiclo, anoSelecionado, rotaSelecionada)
                        } else {
                            emptyList()
                        }
                    }
                    TipoFiltro.ANO_COMPLETO -> {
                        repository.getDespesasPorAno(anoSelecionado, rotaSelecionada)
                    }
                }
                
                // Aplicar filtro de categoria se selecionado
                val despesasFiltradas = if (categoriaSelecionada != null) {
                    despesas.filter { it.categoria == categoriaSelecionada }
                } else {
                    despesas
                }
                
                // Calcular estatísticas
                val estatisticas = calcularEstatisticas(despesasFiltradas)
                _estatisticas.value = estatisticas
                
                // Agrupar por categoria
                val despesasPorCategoria = agruparPorCategoria(despesasFiltradas)
                _despesasPorCategoria.value = despesasPorCategoria
                
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Erro ao atualizar relatório: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun calcularEstatisticas(despesas: List<DespesaRelatorioData>): EstatisticasDespesas {
        val totalDespesas = despesas.sumOf { it.valor }
        val mediaPorCategoria = if (despesas.isNotEmpty()) {
            val categoriasUnicas = despesas.map { it.categoria }.distinct()
            if (categoriasUnicas.isNotEmpty()) totalDespesas / categoriasUnicas.size else 0.0
        } else 0.0
        
        val categoriaMaior = despesas.groupBy { it.categoria }
            .maxByOrNull { it.value.sumOf { despesa -> despesa.valor } }?.key ?: ""
        
        val categoriaMenor = despesas.groupBy { it.categoria }
            .minByOrNull { it.value.sumOf { despesa -> despesa.valor } }?.key ?: ""

        return EstatisticasDespesas(
            totalDespesas = totalDespesas,
            mediaPorCategoria = mediaPorCategoria,
            categoriaMaior = categoriaMaior,
            categoriaMenor = categoriaMenor,
            totalCategorias = despesas.map { it.categoria }.distinct().size
        )
    }

    private fun agruparPorCategoria(despesas: List<DespesaRelatorioData>): List<DespesaCategoriaItem> {
        return despesas.groupBy { it.categoria }
            .map { (categoria, despesasCategoria) ->
                DespesaCategoriaItem(
                    categoria = categoria,
                    total = despesasCategoria.sumOf { it.valor },
                    quantidade = despesasCategoria.size,
                    percentual = if (_estatisticas.value?.totalDespesas ?: 0.0 > 0) {
                        (despesasCategoria.sumOf { it.valor } / (_estatisticas.value?.totalDespesas ?: 1.0)) * 100
                    } else 0.0,
                    despesas = despesasCategoria
                )
            }
            .sortedByDescending { it.total }
    }

    fun exportarRelatorio(): String {
        val estatisticas = _estatisticas.value ?: return ""
        val despesasPorCategoria = _despesasPorCategoria.value ?: emptyList()
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        return buildString {
            appendLine("RELATÓRIO DE DESPESAS POR CATEGORIA")
            appendLine("=".repeat(50))
            appendLine()
            
            appendLine("FILTROS APLICADOS:")
            appendLine("Tipo: ${filtroTipo.descricao}")
            appendLine("Ano: $anoSelecionado")
            if (categoriaSelecionada != null) {
                appendLine("Categoria: $categoriaSelecionada")
            }
            appendLine()
            
            appendLine("ESTATÍSTICAS GERAIS:")
            appendLine("Total de Despesas: ${formatter.format(estatisticas.totalDespesas)}")
            appendLine("Média por Categoria: ${formatter.format(estatisticas.mediaPorCategoria)}")
            appendLine("Categoria Maior: ${estatisticas.categoriaMaior}")
            appendLine("Categoria Menor: ${estatisticas.categoriaMenor}")
            appendLine("Total de Categorias: ${estatisticas.totalCategorias}")
            appendLine()
            
            appendLine("DESPESAS POR CATEGORIA:")
            appendLine("-".repeat(50))
            despesasPorCategoria.forEach { categoria ->
                appendLine("${categoria.categoria}")
                appendLine("  Total: ${formatter.format(categoria.total)}")
                appendLine("  Quantidade: ${categoria.quantidade}")
                appendLine("  Percentual: ${String.format("%.1f", categoria.percentual)}%")
                appendLine()
            }
        }
    }

    enum class TipoFiltro(val descricao: String) {
        CICLO_ESPECIFICO("Ciclo Específico"),
        CONSOLIDADO_CICLOS("Consolidado de Ciclos"),
        ANO_COMPLETO("Ano Completo")
    }

    data class EstatisticasDespesas(
        val totalDespesas: Double,
        val mediaPorCategoria: Double,
        val categoriaMaior: String,
        val categoriaMenor: String,
        val totalCategorias: Int
    )

    data class DespesaCategoriaItem(
        val categoria: String,
        val total: Double,
        val quantidade: Int,
        val percentual: Double,
        val despesas: List<DespesaRelatorioData>
    )

    data class DespesaRelatorio(
        val id: Long,
        val descricao: String,
        val valor: Double,
        val categoria: String,
        val data: String,
        val rota: String,
        val observacoes: String?
    )
}

/**
 * Factory para criar DespesasCategoriaViewModel com dependências necessárias
 */
class DespesasCategoriaViewModelFactory(
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DespesasCategoriaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DespesasCategoriaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Usar o tipo do AppRepository
typealias DespesaRelatorioData = AppRepository.DespesaRelatorio
