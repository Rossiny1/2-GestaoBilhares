package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel para ClientRegisterFragment
 */
@HiltViewModel
class ClientRegisterViewModel @Inject constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {
    private val _novoClienteId = MutableStateFlow<Long?>(null)
    val novoClienteId: StateFlow<Long?> = _novoClienteId.asStateFlow()
    // isLoading já existe na BaseViewModel
    private val _debitoAtual = MutableStateFlow(0.0)
    val debitoAtual: StateFlow<Double> = _debitoAtual.asStateFlow()
    
    private val _clienteParaEdicao = MutableStateFlow<Cliente?>(null)
    val clienteParaEdicao: StateFlow<Cliente?> = _clienteParaEdicao.asStateFlow()
    
    // ✅ NOVO: Estado para indicar se é edição
    private val _clienteAtualizado = MutableStateFlow<Boolean>(false)
    val clienteAtualizado: StateFlow<Boolean> = _clienteAtualizado.asStateFlow()

    // ✅ NOVO: Estado para erro de cliente duplicado
    private val _clienteDuplicado = MutableStateFlow<Boolean>(false)
    val clienteDuplicado: StateFlow<Boolean> = _clienteDuplicado.asStateFlow()
    
    fun carregarDebitoAtual(clienteId: Long?) {
        if (clienteId == null) {
            _debitoAtual.value = 0.0
            return
        }
        
        viewModelScope.launch {
            try {
                val debito = appRepository.obterDebitoAtual(clienteId)
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
                showLoading()
                
                // ✅ CORRIGIDO: Detectar se é edição ou novo cadastro
                val clienteExistente = _clienteParaEdicao.value
                
                if (clienteExistente != null) {
                    // MODO EDIÇÃO - Atualizar cliente existente
                    android.util.Log.d("ClientRegisterViewModel", "Modo EDIÇÃO - Atualizando cliente ID: ${clienteExistente.id}")
                    
                    // ✅ NOVO: Verificar se o NOVO nome já existe em OUTRO cliente na mesma rota
                    if (cliente.nome != clienteExistente.nome) {
                        val existente = appRepository.buscarClientePorNomeERota(cliente.nome, cliente.rotaId)
                        if (existente != null && existente.id != clienteExistente.id) {
                            android.util.Log.w("ClientRegisterViewModel", "ALERTA: Tentativa de mudar nome para '${cliente.nome}', que já existe (ID: ${existente.id})")
                            _clienteDuplicado.value = true
                            return@launch
                        }
                    }

                    val clienteAtualizado = cliente.copy(
                        id = clienteExistente.id,
                        dataCadastro = clienteExistente.dataCadastro, // Preservar data original
                        dataUltimaAtualizacao = System.currentTimeMillis() // Atualizar data de modificação
                    )
                    
                    android.util.Log.d("ClientRegisterViewModel", "Cliente atualizado preparado: ${clienteAtualizado.nome}")
                    appRepository.atualizarCliente(clienteAtualizado)
                    
                    // Verificar se a atualização foi bem-sucedida
                    val clienteVerificado = appRepository.obterClientePorId(clienteExistente.id)
                    android.util.Log.d("ClientRegisterViewModel", "Cliente após atualização: ${clienteVerificado?.nome}")
                    
                    _clienteAtualizado.value = true
                    android.util.Log.d("ClientRegisterViewModel", "Cliente atualizado com sucesso!")
                } else {
                    // MODO NOVO CADASTRO
                    android.util.Log.d("ClientRegisterViewModel", "Modo NOVO CADASTRO - Verificando duplicados")
                    
                    // ✅ NOVO: Verificar se já existe um cliente com este nome na mesma rota
                    val existente = appRepository.buscarClientePorNomeERota(cliente.nome, cliente.rotaId)
                    if (existente != null) {
                        android.util.Log.w("ClientRegisterViewModel", "ALERTA: Cliente '${cliente.nome}' já existe na rota ${cliente.rotaId} (ID: ${existente.id})")
                        _clienteDuplicado.value = true
                        return@launch
                    }

                    android.util.Log.d("ClientRegisterViewModel", "Nenhum duplicado encontrado, inserindo cliente")
                    val id = appRepository.inserirCliente(cliente)
                    _novoClienteId.value = id
                    android.util.Log.d("ClientRegisterViewModel", "Cliente inserido com sucesso, ID: $id")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ClientRegisterViewModel", "Erro ao salvar cliente: ${e.message}", e)
                _novoClienteId.value = null
                _clienteAtualizado.value = false
            } finally {
                android.util.Log.d("ClientRegisterViewModel", "Finalizando operação, isLoading = false")
                hideLoading()
            }
        }
    }
    
    fun resetNovoClienteId() {
        _novoClienteId.value = null
        _clienteAtualizado.value = false
        _clienteDuplicado.value = false
    }

    fun resetStatusDuplicado() {
        _clienteDuplicado.value = false
    }
    
    /**
     * ✅ IMPLEMENTADO: Carrega dados do cliente para edição
     */
    fun carregarClienteParaEdicao(clienteId: Long) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ClientRegisterViewModel", "Carregando cliente para edição: $clienteId")
                showLoading()
                
                val cliente = appRepository.obterClientePorId(clienteId)
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
                hideLoading()
            }
        }
    }

    fun carregarCliente(clienteId: Long) {
        // ✅ IMPLEMENTADO: Método para compatibilidade
        carregarClienteParaEdicao(clienteId)
    }
} 
