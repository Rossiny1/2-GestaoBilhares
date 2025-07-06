package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.repository.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para ClientRegisterFragment
 */
class ClientRegisterViewModel(
    private val clienteRepository: ClienteRepository
) : ViewModel() {
    private val _novoClienteId = MutableStateFlow<Long?>(null)
    val novoClienteId: StateFlow<Long?> = _novoClienteId.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _debitoAtual = MutableStateFlow(0.0)
    val debitoAtual: StateFlow<Double> = _debitoAtual.asStateFlow()
    
    private val _clienteParaEdicao = MutableStateFlow<Cliente?>(null)
    val clienteParaEdicao: StateFlow<Cliente?> = _clienteParaEdicao.asStateFlow()
    
    // ✅ NOVO: Estado para indicar se é edição
    private val _clienteAtualizado = MutableStateFlow<Boolean>(false)
    val clienteAtualizado: StateFlow<Boolean> = _clienteAtualizado.asStateFlow()
    
    fun carregarDebitoAtual(clienteId: Long?) {
        if (clienteId == null) {
            _debitoAtual.value = 0.0
            return
        }
        
        viewModelScope.launch {
            try {
                val debito = clienteRepository.obterDebitoAtual(clienteId)
                _debitoAtual.value = debito
            } catch (e: Exception) {
                _debitoAtual.value = 0.0
            }
        }
    }
    
    fun cadastrarCliente(cliente: Cliente) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ClientRegisterViewModel", "Iniciando cadastro do cliente: ${cliente.nome}")
                _isLoading.value = true
                
                // ✅ CORRIGIDO: Detectar se é edição ou novo cadastro
                val clienteExistente = _clienteParaEdicao.value
                
                if (clienteExistente != null) {
                    // MODO EDIÇÃO - Atualizar cliente existente
                    android.util.Log.d("ClientRegisterViewModel", "Modo EDIÇÃO - Atualizando cliente ID: ${clienteExistente.id}")
                    android.util.Log.d("ClientRegisterViewModel", "Dados originais: ${clienteExistente.nome}")
                    android.util.Log.d("ClientRegisterViewModel", "Dados novos: ${cliente.nome}")
                    
                    val clienteAtualizado = cliente.copy(
                        id = clienteExistente.id,
                        dataCadastro = clienteExistente.dataCadastro, // Preservar data original
                        dataUltimaAtualizacao = java.util.Date() // Atualizar data de modificação
                    )
                    
                    android.util.Log.d("ClientRegisterViewModel", "Cliente atualizado preparado: ${clienteAtualizado.nome}")
                    clienteRepository.atualizar(clienteAtualizado)
                    
                    // Verificar se a atualização foi bem-sucedida
                    val clienteVerificado = clienteRepository.obterPorId(clienteExistente.id)
                    android.util.Log.d("ClientRegisterViewModel", "Cliente após atualização: ${clienteVerificado?.nome}")
                    
                    _clienteAtualizado.value = true
                    android.util.Log.d("ClientRegisterViewModel", "Cliente atualizado com sucesso!")
                } else {
                    // MODO NOVO CADASTRO
                    android.util.Log.d("ClientRegisterViewModel", "Modo NOVO CADASTRO - Inserindo cliente")
                    val id = clienteRepository.inserir(cliente)
                    _novoClienteId.value = id
                    android.util.Log.d("ClientRegisterViewModel", "Cliente inserido com sucesso, ID: $id")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ClientRegisterViewModel", "Erro ao salvar cliente: ${e.message}", e)
                _novoClienteId.value = null
                _clienteAtualizado.value = false
            } finally {
                android.util.Log.d("ClientRegisterViewModel", "Finalizando operação, isLoading = false")
                _isLoading.value = false
            }
        }
    }
    
    fun resetNovoClienteId() {
        _novoClienteId.value = null
        _clienteAtualizado.value = false
    }
    
    /**
     * ✅ IMPLEMENTADO: Carrega dados do cliente para edição
     */
    fun carregarClienteParaEdicao(clienteId: Long) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ClientRegisterViewModel", "Carregando cliente para edição: $clienteId")
                _isLoading.value = true
                
                val cliente = clienteRepository.obterPorId(clienteId)
                _clienteParaEdicao.value = cliente
                
                // Carregar débito atual também
                if (cliente != null) {
                    carregarDebitoAtual(clienteId)
                }
                
                android.util.Log.d("ClientRegisterViewModel", "Cliente carregado: ${cliente?.nome}")
            } catch (e: Exception) {
                android.util.Log.e("ClientRegisterViewModel", "Erro ao carregar cliente: ${e.message}", e)
                _clienteParaEdicao.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarCliente(clienteId: Long) {
        // ✅ IMPLEMENTADO: Método para compatibilidade
        carregarClienteParaEdicao(clienteId)
    }
} 