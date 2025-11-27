package com.example.gestaobilhares.ui.routes.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciamento de rotas.
 * Implementa CRUD de rotas com controle de acesso administrativo.
 */
class RouteManagementViewModel(
    private val appRepository: AppRepository,
    private val userSessionManager: com.example.gestaobilhares.core.utils.UserSessionManager? = null
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
     * ✅ NOVO: Carrega todas as rotas do banco de dados
     */
    fun loadRotas() {
        viewModelScope.launch {
            try {
                showLoading()
                // ✅ CORREÇÃO: Usar first() para obter a primeira emissão do Flow
                val rotasList = appRepository.obterRotasAtivas().first()
                _rotas.value = rotasList
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar rotas: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Verifica se o usuário atual tem acesso de administrador.
     * ✅ FASE 12.7: Usar UserSessionManager para verificação real
     */
    fun checkAdminAccess() {
        viewModelScope.launch {
            try {
                _hasAdminAccess.value = userSessionManager?.isAdmin() ?: false
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
                val existingRoute = appRepository.obterRotaPorNome(nome)
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

                appRepository.inserirRota(novaRota)
                
                _successMessage.value = "Rota \"$nome\" criada com sucesso"
                // ✅ NOVO: Recarregar lista de rotas após criação
                loadRotas()
                
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
                
                appRepository.atualizarRota(rota)
                
                _successMessage.value = "Rota \"${rota.nome}\" atualizada com sucesso"
                // ✅ NOVO: Recarregar lista de rotas após atualização
                loadRotas()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar rota: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Desativa uma rota (soft delete).
     */
    fun deleteRoute(rota: Rota) {
        viewModelScope.launch {
            try {
                showLoading()
                
                appRepository.desativarRota(rota.id)
                
                _successMessage.value = "Rota \"${rota.nome}\" desativada com sucesso"
                // ✅ NOVO: Recarregar lista de rotas após desativação
                loadRotas()
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar rota: ${e.message}"
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

