package com.example.gestaobilhares.ui.routes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel para gerenciar a seleção de clientes para transferência.
 */
@HiltViewModel
class ClientSelectionViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _clientes = MutableLiveData<List<ClientSelectionAdapter.ClientSelectionItem>>()
    val clientes: LiveData<List<ClientSelectionAdapter.ClientSelectionItem>> = _clientes

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Carrega todos os clientes com suas rotas e mesas.
     */
    fun loadAllClients() {
        viewModelScope.launch {
            try {
                // Usar first() para obter a lista de clientes do Flow de forma assíncrona
                val clientes = appRepository.obterTodosClientes().first()
                
                val clientesComDetalhes = mutableListOf<ClientSelectionAdapter.ClientSelectionItem>()

                for (cliente in clientes) {
                    val rota = appRepository.obterRotaPorId(cliente.rotaId)
                    val mesas = appRepository.obterMesasPorClienteDireto(cliente.id)

                    if (rota != null) {
                        clientesComDetalhes.add(
                            ClientSelectionAdapter.ClientSelectionItem(
                                cliente = cliente,
                                rota = rota,
                                mesas = mesas
                            )
                        )
                    }
                }

                _clientes.value = clientesComDetalhes
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar clientes: ${e.message}"
            }
        }
    }

    /**
     * Busca clientes por nome.
     */
    fun searchClients(query: String) {
        viewModelScope.launch {
            try {
                // Usar first() para obter a lista de clientes do Flow de forma assíncrona
                val clientes = appRepository.obterTodosClientes().first()
                
                val clientesFiltrados = clientes.filter { 
                    it.nome.contains(query, ignoreCase = true) 
                }
                
                val clientesComDetalhes = mutableListOf<ClientSelectionAdapter.ClientSelectionItem>()

                for (cliente in clientesFiltrados) {
                    val rota = appRepository.obterRotaPorId(cliente.rotaId)
                    val mesas = appRepository.obterMesasPorClienteDireto(cliente.id)

                    if (rota != null) {
                        clientesComDetalhes.add(
                            ClientSelectionAdapter.ClientSelectionItem(
                                cliente = cliente,
                                rota = rota,
                                mesas = mesas
                            )
                        )
                    }
                }

                _clientes.value = clientesComDetalhes
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao buscar clientes: ${e.message}"
            }
        }
    }

    /**
     * Limpa as mensagens de erro.
     */
    fun clearMessages() {
        _errorMessage.value = null
    }
}

