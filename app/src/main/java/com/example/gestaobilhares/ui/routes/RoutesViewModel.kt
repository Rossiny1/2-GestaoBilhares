package com.example.gestaobilhares.ui.routes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.repository.RotaRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel para a tela de rotas.
 * Gerencia o estado da UI e coordena com o Repository para obter dados.
 * Segue o padrão MVVM para separar a lógica de negócio da UI.
 * 
 * FASE 3: Inclui controle de acesso administrativo e cálculo de valores acertados.
 */
class RoutesViewModel(
    private val rotaRepository: RotaRepository
) : ViewModel() {

    // LiveData privado para controlar o estado de loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData privado para mensagens de erro
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData privado para mensagens de sucesso
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // LiveData privado para controlar a navegação
    private val _navigateToClients = MutableLiveData<Long?>()
    val navigateToClients: LiveData<Long?> = _navigateToClients

    // LiveData privado para mostrar diálogo de nova rota
    private val _showAddRouteDialog = MutableLiveData<Boolean>()
    val showAddRouteDialog: LiveData<Boolean> = _showAddRouteDialog

    // FASE 3: Controle de acesso administrativo
    private val _isAdmin = MutableLiveData<Boolean>()
    val isAdmin: LiveData<Boolean> = _isAdmin

    // FASE 3: Eventos para geração de relatório
    private val _generateReport = MutableLiveData<Boolean>()
    val generateReport: LiveData<Boolean> = _generateReport

    // Observa as rotas resumo do repository
    val rotasResumo: LiveData<List<RotaResumo>> = rotaRepository.getRotasResumoComAtualizacaoTempoReal().asLiveData()

    // Estatísticas gerais calculadas a partir das rotas
    val estatisticas: LiveData<EstatisticasGerais> = combine(
        rotaRepository.getRotasResumoComAtualizacaoTempoReal()
    ) { rotas ->
        calcularEstatisticas(rotas.first())
    }.asLiveData()

    init {
        // Insere rotas de exemplo se necessário
        inserirRotasExemploSeNecessario()
        
        // FASE 3: Verifica se o usuário é admin
        checkAdminAccess()
    }

    /**
     * FASE 3: Verifica se o usuário atual tem acesso administrativo.
     * Por enquanto simula verificação. Na implementação final,
     * deve verificar no banco de dados pelo Firebase UID.
     */
    private fun checkAdminAccess() {
        viewModelScope.launch {
            try {
                // TODO: Implementar verificação real com Firebase UID
                // Por enquanto, assume que tem acesso admin para demonstração
                // Em produção, fazer:
                // val currentUser = FirebaseAuth.getInstance().currentUser
                // val colaborador = colaboradorRepository.getByFirebaseUid(currentUser?.uid)
                // _isAdmin.value = colaborador?.nivelAcesso == NivelAcesso.ADMIN
                
                _isAdmin.value = true // Temporário para demonstração
            } catch (e: Exception) {
                _isAdmin.value = false
                _errorMessage.value = "Erro ao verificar permissões: ${e.message}"
            }
        }
    }

    /**
     * FASE 3: Calcula estatísticas gerais das rotas incluindo valores acertados não finalizados.
     */
    private fun calcularEstatisticas(rotas: List<RotaResumo>): EstatisticasGerais {
        return EstatisticasGerais(
            totalClientesAtivos = rotas.sumOf { it.clientesAtivos },
            totalPendencias = rotas.sumOf { it.pendencias },
            totalMesas = rotas.sumOf { it.quantidadeMesas },
            valorTotalAcertado = rotas.sumOf { it.valorAcertado },
            // FASE 3: Calcular apenas valores não finalizados
            valorAcertadoNaoFinalizado = calcularValorAcertadoNaoFinalizado(rotas)
        )
    }

    /**
     * FASE 3: Calcula a somatória dos valores acertados que ainda não foram finalizados.
     */
    private fun calcularValorAcertadoNaoFinalizado(rotas: List<RotaResumo>): Double {
        // TODO: Implementar cálculo real quando houver dados de acertos
        // Por enquanto, simula valores baseados nas rotas
        return rotas.sumOf { rotaResumo ->
            // Simula que 70% dos valores ainda não foram finalizados
            rotaResumo.valorAcertado * 0.7
        }
    }

    /**
     * Navega para a lista de clientes de uma rota específica.
     */
    fun navigateToClients(rotaResumo: RotaResumo) {
        _navigateToClients.value = rotaResumo.rota.id
    }

    /**
     * Limpa o estado de navegação após navegar.
     */
    fun navigationToClientsCompleted() {
        _navigateToClients.value = null
    }

    /**
     * Mostra o diálogo para adicionar nova rota.
     * FASE 3: Verifica se o usuário é admin antes de permitir.
     */
    fun showAddRouteDialog() {
        if (_isAdmin.value == true) {
            _showAddRouteDialog.value = true
        } else {
            _errorMessage.value = "Apenas administradores podem adicionar rotas"
        }
    }

    /**
     * Esconde o diálogo de adicionar rota.
     */
    fun hideAddRouteDialog() {
        _showAddRouteDialog.value = false
    }

    /**
     * FASE 3: Inicia a geração de relatório de fechamento de rota.
     */
    fun generateRouteClosureReport() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // ✅ CORREÇÃO: Implementar relatório visual em vez de PDF
                // Buscar dados das rotas para o relatório
                val rotas = rotaRepository.getRotasResumo().first()
                
                if (rotas.isEmpty()) {
                    _errorMessage.value = "Nenhuma rota encontrada para gerar relatório"
                    return@launch
                }
                
                // Simular processamento do relatório visual
                kotlinx.coroutines.delay(1000) // Processamento mais rápido
                
                // Calcular estatísticas do relatório
                val totalClientes = rotas.sumOf { it.clientesAtivos }
                val totalPendencias = rotas.sumOf { it.pendencias }
                val valorTotal = rotas.sumOf { it.valorAcertado }
                
                // Gerar relatório visual (não PDF)
                val relatorioInfo = """
                    RELATÓRIO DE FECHAMENTO
                    
                    Total de Rotas: ${rotas.size}
                    Clientes Ativos: $totalClientes
                    Pendências: $totalPendencias
                    Valor Total: R$ ${String.format("%.2f", valorTotal)}
                    
                    Gerado em: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date())}
                """.trimIndent()
                
                _successMessage.value = "Relatório visual gerado com sucesso!"
                _generateReport.value = true
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao gerar relatório: ${e.message}"
                e.printStackTrace() // Log para debug
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * FASE 3: Limpa o estado de geração de relatório.
     */
    fun reportGenerationCompleted() {
        _generateReport.value = false
    }

    /**
     * Adiciona uma nova rota.
     */
    fun addNewRoute(nome: String, descricao: String = "", cor: String = "#6200EA") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val novaRota = Rota(
                    nome = nome.trim(),
                    descricao = descricao.trim(),
                    cor = cor,
                    ativa = true
                )

                val rotaId = rotaRepository.insertRota(novaRota)
                
                if (rotaId != null) {
                    _successMessage.value = "Rota '$nome' criada com sucesso!"
                    hideAddRouteDialog()
                } else {
                    _errorMessage.value = "Já existe uma rota com esse nome!"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao criar rota: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpa mensagens de erro e sucesso.
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    /**
     * Recarrega os dados das rotas.
     */
    fun refresh() {
        // Os dados são atualizados automaticamente via Flow/LiveData
        // Aqui poderíamos adicionar lógica para sincronização com servidor
    }

    /**
     * Insere rotas de exemplo se não existirem.
     */
    private fun inserirRotasExemploSeNecessario() {
        viewModelScope.launch {
            try {
                rotaRepository.inserirRotasExemplo()
            } catch (e: Exception) {
                // Ignora erros ao inserir dados de exemplo
            }
        }
    }

    /**
     * FASE 3: Data class para estatísticas gerais incluindo valores não finalizados.
     */
    data class EstatisticasGerais(
        val totalClientesAtivos: Int,
        val totalPendencias: Int,
        val totalMesas: Int,
        val valorTotalAcertado: Double,
        val valorAcertadoNaoFinalizado: Double // FASE 3: Valores não finalizados
    )
} 
