package com.example.gestaobilhares.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.data.entities.Rota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar configuração de rotas por usuário
 * Implementa a interface de admin para associar rotas aos colaboradores
 */
class RotasConfigViewModel(
    private val userSessionManager: UserSessionManager,
    private val rotaRepository: RotaRepository
) : ViewModel() {

    // Estados da UI
    private val _rotasDisponiveis = MutableStateFlow<List<Rota>>(emptyList())
    val rotasDisponiveis: StateFlow<List<Rota>> = _rotasDisponiveis.asStateFlow()

    private val _colaboradores = MutableStateFlow<List<ColaboradorComRotas>>(emptyList())
    val colaboradores: StateFlow<List<ColaboradorComRotas>> = _colaboradores.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _sucessoMessage = MutableStateFlow<String?>(null)
    val sucessoMessage: StateFlow<String?> = _sucessoMessage.asStateFlow()

    /**
     * Data class para representar colaborador com suas rotas
     */
    data class ColaboradorComRotas(
        val id: Long,
        val nome: String,
        val email: String,
        val rotasPermitidas: List<Long>,
        val rotasAssociadas: List<Rota>
    )

    init {
        carregarDadosIniciais()
    }

    /**
     * Carrega rotas disponíveis e colaboradores existentes
     */
    private fun carregarDadosIniciais() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar rotas disponíveis
                rotaRepository.obterTodasRotas().collect { rotas ->
                    _rotasDisponiveis.value = rotas
                }
                
                // Carregar colaboradores com rotas
                // TODO: Implementar método no repository quando disponível
                // _colaboradores.value = obterColaboradoresComRotas()
                
                _isLoading.value = false
                
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Erro ao carregar dados: ${e.message}"
            }
        }
    }

    /**
     * Associa uma rota a um colaborador
     */
    fun associarRotaAoColaborador(colaboradorId: Long, rotaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _sucessoMessage.value = null
                
                // TODO: Implementar associação no repository
                // rotaRepository.associarRotaAoColaborador(colaboradorId, rotaId)
                
                // Recarregar dados
                carregarDadosIniciais()
                
                _sucessoMessage.value = "Rota associada com sucesso!"
                _isLoading.value = false
                
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Erro ao associar rota: ${e.message}"
            }
        }
    }

    /**
     * Remove associação de rota de um colaborador
     */
    fun removerRotaDoColaborador(colaboradorId: Long, rotaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _sucessoMessage.value = null
                
                // TODO: Implementar remoção no repository
                // rotaRepository.removerRotaDoColaborador(colaboradorId, rotaId)
                
                // Recarregar dados
                carregarDadosIniciais()
                
                _sucessoMessage.value = "Rota removida com sucesso!"
                _isLoading.value = false
                
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Erro ao remover rota: ${e.message}"
            }
        }
    }

    /**
     * Salva rotas permitidas para um colaborador (formato JSON)
     */
    fun salvarRotasPermitidas(colaboradorId: Long, rotasIds: List<Long>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _sucessoMessage.value = null
                
                // Converter para JSON (formato "[1,2,3]")
                val rotasJson = if (rotasIds.isEmpty()) {
                    null // Admin tem acesso a todas
                } else {
                    rotasIds.joinToString(prefix = "[", postfix = "]", separator = ",")
                }
                
                // TODO: Implementar salvamento no repository
                // colaboradorRepository.atualizarRotasPermitidas(colaboradorId, rotasJson)
                
                // Recarregar dados
                carregarDadosIniciais()
                
                _sucessoMessage.value = "Rotas salvas com sucesso!"
                _isLoading.value = false
                
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Erro ao salvar rotas: ${e.message}"
            }
        }
    }

    /**
     * Limpa mensagens de erro e sucesso
     */
    fun limparMensagens() {
        _errorMessage.value = null
        _sucessoMessage.value = null
    }
}
