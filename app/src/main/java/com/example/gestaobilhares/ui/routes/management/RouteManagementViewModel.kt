package com.example.gestaobilhares.ui.routes.management

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.data.repository.RotaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para gerenciamento de rotas.
 * Implementa CRUD de rotas com controle de acesso administrativo.
 */
@HiltViewModel
class RouteManagementViewModel @Inject constructor(
    private val rotaRepository: RotaRepository
) : ViewModel() {

    // Lista de rotas observável
    val rotas: LiveData<List<Rota>> = rotaRepository.getAllRotas().asLiveData()

    // Mensagens de erro
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Mensagens de sucesso
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Estado de loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Controle de acesso admin
    private val _hasAdminAccess = MutableLiveData<Boolean>()
    val hasAdminAccess: LiveData<Boolean> = _hasAdminAccess

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
                _isLoading.value = true
                
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
                _isLoading.value = false
            }
        }
    }

    /**
     * Atualiza uma rota existente.
     */
    fun updateRoute(rota: Rota) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val success = rotaRepository.updateRota(rota)
                
                if (success) {
                    _successMessage.value = "Rota \"${rota.nome}\" atualizada com sucesso"
                } else {
                    _errorMessage.value = "Erro ao atualizar rota. Verifique se já existe uma rota com este nome."
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar rota: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Exclui uma rota.
     */
    fun deleteRoute(rota: Rota) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
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
} 
