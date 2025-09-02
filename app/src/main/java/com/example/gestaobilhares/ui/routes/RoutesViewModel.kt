package com.example.gestaobilhares.ui.routes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.utils.UserSessionManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel para a tela de rotas.
 * Gerencia o estado da UI e coordena com o Repository para obter dados.
 * Segue o padrão MVVM para separar a lógica de negócio da UI.
 * 
 * FASE 3: Inclui controle de acesso administrativo e cálculo de valores acertados.
 * ✅ NOVO: Controle de acesso baseado em nível de usuário e rotas responsáveis.
 */
class RoutesViewModel(
    private val rotaRepository: RotaRepository,
    private val userSessionManager: UserSessionManager
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





    // ✅ NOVO: Rotas filtradas baseado no acesso do usuário
    private val _rotasResumoFiltradas = MutableLiveData<List<RotaResumo>>()
    val rotasResumo: LiveData<List<RotaResumo>> = _rotasResumoFiltradas
    
    // Observa as rotas resumo do repository e aplica filtro de acesso
    private val rotasResumoOriginal: LiveData<List<RotaResumo>> = rotaRepository.getRotasResumoComAtualizacaoTempoReal().asLiveData()

    // Estatísticas gerais calculadas a partir das rotas
    val estatisticas: LiveData<EstatisticasGerais> = combine(
        rotaRepository.getRotasResumoComAtualizacaoTempoReal()
    ) { rotas ->
        calcularEstatisticas(rotas.first())
    }.asLiveData()

    init {
        // Observar mudanças nas rotas originais e aplicar filtro de acesso
        rotasResumoOriginal.observeForever { rotas ->
            aplicarFiltroAcesso(rotas)
        }
        
        // Insere rotas de exemplo se necessário
        inserirRotasExemploSeNecessario()
    }

    /**
     * ✅ NOVO: Aplica filtro de acesso às rotas baseado no nível do usuário
     */
    private fun aplicarFiltroAcesso(rotas: List<RotaResumo>) {
        val isAdmin = userSessionManager.isAdmin()
        val userName = userSessionManager.getCurrentUserName()
        val userEmail = userSessionManager.getCurrentUserEmail()
        
        android.util.Log.d("RoutesViewModel", "🔍 Aplicando filtro de rotas:")
        android.util.Log.d("RoutesViewModel", "   Usuário: $userName")
        android.util.Log.d("RoutesViewModel", "   Email: $userEmail")
        android.util.Log.d("RoutesViewModel", "   É Admin: $isAdmin")
        android.util.Log.d("RoutesViewModel", "   Total de rotas: ${rotas.size}")
        
        if (isAdmin) {
            // Admin vê todas as rotas
            _rotasResumoFiltradas.value = rotas
            android.util.Log.d("RoutesViewModel", "✅ ADMIN - Mostrando todas as ${rotas.size} rotas")
        } else {
            // USER vê apenas rotas onde é responsável
            // Por enquanto, mostrar todas até implementar busca de rotas responsáveis
            _rotasResumoFiltradas.value = rotas
            android.util.Log.d("RoutesViewModel", "⚠️ USER - Mostrando todas as rotas (filtro de responsabilidade não implementado)")
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
