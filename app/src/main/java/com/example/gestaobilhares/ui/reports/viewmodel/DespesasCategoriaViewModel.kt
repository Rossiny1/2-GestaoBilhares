package com.example.gestaobilhares.ui.reports.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.*

@HiltViewModel
class DespesasCategoriaViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    // Data class local para CicloInfo
    data class CicloInfo(
        val numero: Int,
        val descricao: String
    )

    // Data class para DespesaCategoria
    data class DespesaCategoria(
        val categoria: String,
        val valor: Double,
        val quantidade: Int,
        val percentual: Double
    )

    // Enums para filtros
    enum class TipoFiltro {
        CICLO_ESPECIFICO,
        CONSOLIDADO_CICLOS,
        ANO_COMPLETO
    }

    // LiveData para UI
    private val _despesasPorCategoria = MutableLiveData<List<DespesaCategoria>>()
    val despesasPorCategoria: LiveData<List<DespesaCategoria>> = _despesasPorCategoria

    private val _estatisticas = MutableLiveData<EstatisticasDespesas>()
    val estatisticas: LiveData<EstatisticasDespesas> = _estatisticas

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

    // Estado dos filtros
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
                
                // Carregar dados básicos - usar métodos alternativos
                val ciclosData = listOf(
                    CicloInfo(1, "1º Ciclo"),
                    CicloInfo(2, "2º Ciclo"),
                    CicloInfo(3, "3º Ciclo"),
                    CicloInfo(4, "4º Ciclo"),
                    CicloInfo(5, "5º Ciclo"),
                    CicloInfo(6, "6º Ciclo"),
                    CicloInfo(7, "7º Ciclo"),
                    CicloInfo(8, "8º Ciclo"),
                    CicloInfo(9, "9º Ciclo"),
                    CicloInfo(10, "10º Ciclo"),
                    CicloInfo(11, "11º Ciclo"),
                    CicloInfo(12, "12º Ciclo")
                )
                val rotasData = repository.obterTodasRotas().first()
                
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

    private fun agruparPorCategoria(despesas: List<DespesaRelatorioData>): List<DespesaCategoria> {
        return despesas.groupBy { it.categoria }
            .map { (categoria, despesasCategoria) ->
                DespesaCategoria(
                    categoria = categoria,
                    valor = despesasCategoria.sumOf { it.valor },
                    quantidade = despesasCategoria.size,
                    percentual = if (_estatisticas.value?.totalDespesas ?: 0.0 > 0) {
                        (despesasCategoria.sumOf { it.valor } / (_estatisticas.value?.totalDespesas ?: 1.0)) * 100
                    } else 0.0
                )
            }
            .sortedByDescending { it.valor }
    }

    fun exportarRelatorio(): String {
        val estatisticas = _estatisticas.value ?: return ""
        val despesas = _despesasPorCategoria.value ?: emptyList()
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        return buildString {
            appendLine("RELATÓRIO DE DESPESAS POR CATEGORIA")
            appendLine("=".repeat(50))
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
            despesas.forEach { despesa ->
                appendLine("${despesa.categoria}")
                appendLine("  Valor: ${formatter.format(despesa.valor)} | Quantidade: ${despesa.quantidade}")
                appendLine("  Percentual: ${String.format("%.1f", despesa.percentual)}%")
                appendLine()
            }
        }
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

    private fun aplicarFiltros() {
        if (cicloSelecionado == 0L && rotaSelecionada == 0L) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // TODO: Implementar lógica real de filtragem
                // Por enquanto, dados mock para demonstração
                val despesasMock = listOf(
                    DespesaCategoria("Combustível", 1500.0, 15, 30.0),
                    DespesaCategoria("Manutenção", 1000.0, 8, 20.0),
                    DespesaCategoria("Alimentação", 800.0, 12, 16.0),
                    DespesaCategoria("Outros", 700.0, 5, 14.0)
                )
                
                _despesasPorCategoria.value = despesasMock
                
                val total = despesasMock.sumOf { it.valor }
                val estatisticas = EstatisticasDespesas(
                    totalDespesas = total,
                    mediaPorCategoria = total / despesasMock.size,
                    categoriaMaior = despesasMock.maxByOrNull { it.valor }?.categoria ?: "",
                    categoriaMenor = despesasMock.minByOrNull { it.valor }?.categoria ?: "",
                    totalCategorias = despesasMock.size
                )
                
                _estatisticas.value = estatisticas
                _isLoading.value = false
                
            } catch (e: Exception) {
                _error.value = "Erro ao aplicar filtros: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}

// Usar o tipo do AppRepository
typealias DespesaRelatorioData = AppRepository.DespesaRelatorio
