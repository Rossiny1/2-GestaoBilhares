package com.example.gestaobilhares.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SignatureCaptureViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    
    private val _contrato = MutableStateFlow<ContratoLocacao?>(null)
    val contrato: StateFlow<ContratoLocacao?> = _contrato.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _assinaturaSalva = MutableStateFlow(false)
    val assinaturaSalva: StateFlow<Boolean> = _assinaturaSalva.asStateFlow()
    
    suspend fun getMesasVinculadas(): List<com.example.gestaobilhares.data.entities.Mesa> {
        val contratoId = _contrato.value?.id ?: 0L
        val contratoMesas = repository.buscarMesasPorContrato(contratoId)
        val mesas = mutableListOf<com.example.gestaobilhares.data.entities.Mesa>()
        
        contratoMesas.forEach { contratoMesa ->
            val mesa = repository.obterMesaPorId(contratoMesa.mesaId)
            if (mesa != null) {
                mesas.add(mesa)
            }
        }
        
        return mesas
    }
    
    fun carregarContrato(contratoId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val contrato = repository.buscarContratoPorId(contratoId)
                _contrato.value = contrato
            } catch (e: Exception) {
                _error.value = "Erro ao carregar contrato: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun salvarAssinatura(assinaturaBase64: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val contrato = _contrato.value ?: throw Exception("Contrato não encontrado")
                
                val contratoAtualizado = contrato.copy(
                    assinaturaLocatario = assinaturaBase64,
                    dataAtualizacao = Date()
                )
                
                repository.atualizarContrato(contratoAtualizado)
                _contrato.value = contratoAtualizado
                _assinaturaSalva.value = true
                
            } catch (e: Exception) {
                _error.value = "Erro ao salvar assinatura: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
