package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.repositories.ClienteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientRegisterViewModel @Inject constructor(
    private val clienteRepository: ClienteRepository
) : ViewModel() {
    private val _novoClienteId = MutableStateFlow<Long?>(null)
    val novoClienteId: StateFlow<Long?> = _novoClienteId.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    fun cadastrarCliente(cliente: Cliente) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = clienteRepository.inserir(cliente)
                _novoClienteId.value = id
            } catch (e: Exception) {
                _novoClienteId.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun resetNovoClienteId() {
        _novoClienteId.value = null
    }
} 