package com.example.gestaobilhares.ui.reports.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para relatório de performance dos colaboradores.
 * Baseado em ciclos e rotas com métricas detalhadas.
 */
class ColaboradorPerformanceViewModel : ViewModel() {
    
    private lateinit var appRepository: AppRepository
    
    // ==================== DADOS OBSERVÁVEIS ====================
    
    // Ciclos disponíveis
    private val _ciclos = MutableLiveData<List<CicloInfo>>()
    val ciclos: LiveData<List<CicloInfo>> = _ciclos
    
    // Rotas disponíveis
    private val _rotas = MutableLiveData<List<Rota>>()
    val rotas: LiveData<List<Rota>> = _rotas
    
    // Estatísticas gerais
    private val _estatisticasGerais = MutableLiveData<EstatisticasGerais>()
    val estatisticasGerais: LiveData<EstatisticasGerais> = _estatisticasGerais
    
    // Performance dos colaboradores
    private val _colaboradoresPerformance = MutableLiveData<List<ColaboradorPerformance>>()
    val colaboradoresPerformance: LiveData<List<ColaboradorPerformance>> = _colaboradoresPerformance
    
    // Estado de loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Mensagens de erro
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Filtros atuais
    private var cicloSelecionado: Long? = null
    private var rotaSelecionada: Long? = null
    
    // ==================== INICIALIZAÇÃO ====================
    
    init {
        setupRepository()
    }
    
    private fun setupRepository() {
        // TODO: Injetar via Hilt ou Dagger
        // Por enquanto, criar manualmente
        // appRepository = AppRepository(...)
    }
    
    // ==================== CARREGAMENTO DE DADOS ====================
    
    fun carregarDados() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar ciclos
                carregarCiclos()
                
                // Carregar rotas
                carregarRotas()
                
                // Carregar estatísticas e performance
                carregarEstatisticasEPerformance()
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun carregarCiclos() {
        try {
            // TODO: Implementar quando AppRepository estiver disponível
            // val ciclos = appRepository.obterTodosCiclos().first()
            // _ciclos.value = ciclos.map { CicloInfo(it.id, it.numero, formatarPeriodo(it.dataInicio, it.dataFim)) }
            
            // Dados mock para demonstração
            val ciclosMock = listOf(
                CicloInfo(1, "01/01/2025 - 31/01/2025"),
                CicloInfo(2, "01/02/2025 - 28/02/2025"),
                CicloInfo(3, "01/03/2025 - 31/03/2025")
            )
            _ciclos.value = ciclosMock
            
        } catch (e: Exception) {
            android.util.Log.e("ColaboradorPerformanceViewModel", "Erro ao carregar ciclos: ${e.message}")
        }
    }
    
    private suspend fun carregarRotas() {
        try {
            // TODO: Implementar quando AppRepository estiver disponível
            // val rotas = appRepository.obterTodasRotas().first()
            // _rotas.value = rotas
            
            // Dados mock para demonstração
            val rotasMock = listOf(
                Rota(
                    id = 1L,
                    nome = "Rota Zona Sul",
                    descricao = "Zona Sul da cidade",
                    ativa = true,
                    dataCriacao = System.currentTimeMillis(),
                    dataAtualizacao = System.currentTimeMillis()
                ),
                Rota(
                    id = 2L,
                    nome = "Rota Zona Norte",
                    descricao = "Zona Norte da cidade",
                    ativa = true,
                    dataCriacao = System.currentTimeMillis(),
                    dataAtualizacao = System.currentTimeMillis()
                ),
                Rota(
                    id = 3L,
                    nome = "Rota Centro",
                    descricao = "Centro da cidade",
                    ativa = true,
                    dataCriacao = System.currentTimeMillis(),
                    dataAtualizacao = System.currentTimeMillis()
                )
            )
            _rotas.value = rotasMock
            
        } catch (e: Exception) {
            android.util.Log.e("ColaboradorPerformanceViewModel", "Erro ao carregar rotas: ${e.message}")
        }
    }
    
    private suspend fun carregarEstatisticasEPerformance() {
        try {
            // TODO: Implementar quando AppRepository estiver disponível
            // val colaboradores = appRepository.obterTodosColaboradores().first()
            // val metas = appRepository.obterTodasMetas().first()
            
            // Calcular estatísticas gerais
            val stats = calcularEstatisticasGerais()
            _estatisticasGerais.value = stats
            
            // Calcular performance dos colaboradores
            val performance = calcularPerformanceColaboradores()
            _colaboradoresPerformance.value = performance
            
        } catch (e: Exception) {
            android.util.Log.e("ColaboradorPerformanceViewModel", "Erro ao carregar performance: ${e.message}")
        }
    }
    
    // ==================== FILTROS ====================
    
    fun selecionarCiclo(cicloId: Long) {
        cicloSelecionado = cicloId
        aplicarFiltros()
    }
    
    fun selecionarRota(rotaId: Long?) {
        rotaSelecionada = rotaId
        aplicarFiltros()
    }
    
    private fun aplicarFiltros() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Recarregar dados com filtros aplicados
                carregarEstatisticasEPerformance()
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao aplicar filtros: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ==================== CÁLCULOS ====================
    
    private fun calcularEstatisticasGerais(): EstatisticasGerais {
        // TODO: Implementar cálculos reais baseados nos dados do banco
        return EstatisticasGerais(
            totalColaboradores = 8,
            colaboradoresAtivos = 6,
            percentualMetaAtingida = 75,
            pendentesAprovacao = 2
        )
    }
    
    private fun calcularPerformanceColaboradores(): List<ColaboradorPerformance> {
        // TODO: Implementar cálculos reais baseados nos dados do banco
        return listOf(
            ColaboradorPerformance(
                colaborador = Colaborador(
                    id = 1L,
                    nome = "João Silva",
                    email = "joao@email.com",
                    telefone = "11999999999",
                    cpf = "12345678901",
                    dataNascimento = Date(),
                    endereco = "Rua A, 123",
                    nivelAcesso = NivelAcesso.USER,
                    ativo = true,
                    aprovado = true,
                    dataAprovacao = Date(),
                    dataCadastro = Date()
                ),
                faturamentoMeta = 15000.0,
                faturamentoRealizado = 14250.0,
                percentualFaturamento = 95.0,
                clientesAcertadosMeta = 80.0,
                clientesAcertadosRealizado = 76.0,
                percentualClientesAcertados = 95.0,
                mesasLocadasMeta = 10,
                mesasLocadasRealizado = 9,
                percentualMesasLocadas = 90.0,
                ticketMedioMeta = 150.0,
                ticketMedioRealizado = 158.33,
                percentualTicketMedio = 105.6,
                statusGeral = StatusPerformance.EXCELENTE
            ),
            ColaboradorPerformance(
                colaborador = Colaborador(
                    id = 2L,
                    nome = "Maria Santos",
                    email = "maria@email.com",
                    telefone = "11888888888",
                    cpf = "98765432109",
                    dataNascimento = Date(),
                    endereco = "Rua B, 456",
                    nivelAcesso = NivelAcesso.USER,
                    ativo = true,
                    aprovado = true,
                    dataAprovacao = Date(),
                    dataCadastro = Date()
                ),
                faturamentoMeta = 12000.0,
                faturamentoRealizado = 10800.0,
                percentualFaturamento = 90.0,
                clientesAcertadosMeta = 70.0,
                clientesAcertadosRealizado = 63.0,
                percentualClientesAcertados = 90.0,
                mesasLocadasMeta = 8,
                mesasLocadasRealizado = 7,
                percentualMesasLocadas = 87.5,
                ticketMedioMeta = 140.0,
                ticketMedioRealizado = 154.29,
                percentualTicketMedio = 110.2,
                statusGeral = StatusPerformance.BOM
            )
        )
    }
    
    private fun formatarPeriodo(dataInicio: Date, dataFim: Date): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        return "${formatter.format(dataInicio)} - ${formatter.format(dataFim)}"
    }
}

// ==================== DATA CLASSES ====================

data class EstatisticasGerais(
    val totalColaboradores: Int,
    val colaboradoresAtivos: Int,
    val percentualMetaAtingida: Int,
    val pendentesAprovacao: Int
)

data class ColaboradorPerformance(
    val colaborador: Colaborador,
    val faturamentoMeta: Double,
    val faturamentoRealizado: Double,
    val percentualFaturamento: Double,
    val clientesAcertadosMeta: Double,
    val clientesAcertadosRealizado: Double,
    val percentualClientesAcertados: Double,
    val mesasLocadasMeta: Int,
    val mesasLocadasRealizado: Int,
    val percentualMesasLocadas: Double,
    val ticketMedioMeta: Double,
    val ticketMedioRealizado: Double,
    val percentualTicketMedio: Double,
    val statusGeral: StatusPerformance
)

enum class StatusPerformance {
    EXCELENTE,   // > 95%
    BOM,         // 85-95%
    REGULAR,     // 70-85%
    RUIM,        // < 70%
    PENDENTE     // Sem dados suficientes
}
