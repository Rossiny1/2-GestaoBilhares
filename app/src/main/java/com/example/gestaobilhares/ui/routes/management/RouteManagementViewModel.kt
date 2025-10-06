package com.example.gestaobilhares.ui.routes.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciamento de rotas.
 * Implementa CRUD de rotas com controle de acesso administrativo.
 */
class RouteManagementViewModel(
    private val rotaRepository: RotaRepository
) : BaseViewModel() {

    // Lista de rotas observável
    private val _rotas = MutableStateFlow<List<Rota>>(emptyList())
    val rotas: StateFlow<List<Rota>> = _rotas.asStateFlow()

    // Mensagens de erro
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Mensagens de sucesso
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Estado de loading - removido pois já existe na BaseViewModel

    // Controle de acesso admin
    private val _hasAdminAccess = MutableStateFlow<Boolean>(false)
    val hasAdminAccess: StateFlow<Boolean> = _hasAdminAccess.asStateFlow()

    /**
     * Verifica se o usuário atual tem acesso de administrador.
     * Por enquanto, simula verificação. Na implementação final,
     * deve verificar no banco de dados pelo Firebase UID.
     */
    fun checkAdminAccess() {
        viewModelScope.launch {
            try {
                // TODO: Implementar verificação real com Firebase UID
                // Por enquanto, assume que tem acesso admin para demonstração
                // Em produção, fazer:
                // val currentUser = FirebaseAuth.getInstance().currentUser
                // val colaborador = colaboradorRepository.getByFirebaseUid(currentUser?.uid)
                // _hasAdminAccess.value = colaborador?.nivelAcesso == NivelAcesso.ADMIN
                
                _hasAdminAccess.value = true // Temporário para demonstração
            } catch (e: Exception) {
                _hasAdminAccess.value = false
                _errorMessage.value = "Erro ao verificar permissões: ${e.message}"
            }
        }
    }

    /**
     * Cria uma nova rota.
     */
    fun createRoute(nome: String, colaboradorResponsavel: String, cidades: String) {
        viewModelScope.launch {
            try {
                showLoading()
                
                // Verificar se já existe uma rota com o mesmo nome
                val existingRoute = rotaRepository.getRotaByNome(nome)
                if (existingRoute != null) {
                    _errorMessage.value = "Já existe uma rota com este nome"
                    return@launch
                }

                val novaRota = Rota(
                    nome = nome,
                    colaboradorResponsavel = colaboradorResponsavel.ifBlank { "Não definido" },
                    cidades = cidades.ifBlank { "Não definido" },
                    dataCriacao = System.currentTimeMillis(),
                    dataAtualizacao = System.currentTimeMillis()
                )

                val rotaId = rotaRepository.insertRota(novaRota)
                
                if (rotaId != null) {
                    _successMessage.value = "Rota \"$nome\" criada com sucesso"
                } else {
                    _errorMessage.value = "Erro ao criar rota. Verifique se já existe uma rota com este nome."
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao criar rota: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Atualiza uma rota existente.
     */
    fun updateRoute(rota: Rota) {
        viewModelScope.launch {
            try {
                showLoading()
                
                val success = rotaRepository.updateRota(rota)
                
                if (success) {
                    _successMessage.value = "Rota \"${rota.nome}\" atualizada com sucesso"
                } else {
                    _errorMessage.value = "Erro ao atualizar rota. Verifique se já existe uma rota com este nome."
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar rota: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Exclui uma rota.
     */
    fun deleteRoute(rota: Rota) {
        viewModelScope.launch {
            try {
                showLoading()
                
                // TODO: Verificar se a rota tem clientes associados
                // Em uma implementação completa, deveria:
                // val clientesAssociados = clienteRepository.getClientesByRotaId(rota.id)
                // if (clientesAssociados.isNotEmpty()) {
                //     _errorMessage.value = "Não é possível excluir uma rota que possui clientes associados"
                //     return@launch
                // }

                // Por enquanto, fazemos desativação ao invés de delete
                val success = rotaRepository.desativarRota(rota.id)
                
                if (success) {
                    _successMessage.value = "Rota \"${rota.nome}\" excluída com sucesso"
                } else {
                    _errorMessage.value = "Erro ao excluir rota"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir rota: ${e.message}"
            } finally {
                hideLoading()
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
} 
