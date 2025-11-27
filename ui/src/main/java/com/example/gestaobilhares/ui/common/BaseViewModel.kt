package com.example.gestaobilhares.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ✅ FASE 1: BASE VIEWMODEL CENTRALIZADA
 * 
 * Centraliza funcionalidades comuns de todos os ViewModels:
 * - Estados de loading, error, message
 * - Logging centralizado
 * - Métodos utilitários comuns
 * - Elimina duplicação de código (~200 linhas)
 */
abstract class BaseViewModel : ViewModel() {
    
    // ==================== ESTADOS COMUNS ====================
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    // ==================== MÉTODOS DE CONTROLE DE ESTADO ====================
    
    /**
     * Mostra estado de loading
     */
    protected fun showLoading() {
        _isLoading.value = true
        logState("LOADING", "Iniciado")
    }
    
    /**
     * Esconde estado de loading
     */
    protected fun hideLoading() {
        _isLoading.value = false
        logState("LOADING", "Finalizado")
    }
    
    /**
     * Mostra erro com mensagem
     */
    protected fun showError(message: String, throwable: Throwable? = null) {
        _error.value = message
        _isLoading.value = false
        logError("ERROR", message, throwable)
    }
    
    /**
     * Limpa erro
     */
    fun clearError() {
        _error.value = null
        logState("ERROR", "Limpo")
    }
    
    /**
     * Mostra mensagem de sucesso
     */
    protected fun showMessage(message: String) {
        _message.value = message
        logState("MESSAGE", message)
    }
    
    /**
     * Limpa mensagem
     */
    fun clearMessage() {
        _message.value = null
        logState("MESSAGE", "Limpo")
    }
    
    /**
     * Limpa todos os estados
     */
    protected fun clearAllStates() {
        _isLoading.value = false
        _error.value = null
        _message.value = null
        logState("ALL_STATES", "Limpos")
    }
    
    // ==================== MÉTODOS UTILITÁRIOS ====================
    
    /**
     * Executa operação com controle de loading automático
     */
    protected fun executeWithLoading(
        operation: suspend () -> Unit,
        onError: (Throwable) -> Unit = { showError("Erro inesperado", it) }
    ) {
        viewModelScope.launch {
            try {
                showLoading()
                operation()
                hideLoading()
            } catch (e: Exception) {
                hideLoading()
                onError(e)
            }
        }
    }
    
    /**
     * Executa operação sem loading (para operações rápidas)
     */
    protected fun execute(
        operation: suspend () -> Unit,
        onError: (Throwable) -> Unit = { showError("Erro inesperado", it) }
    ) {
        viewModelScope.launch {
            try {
                operation()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    
    // ==================== LOGGING CENTRALIZADO ====================
    
    /**
     * Log de estado
     */
    protected fun logState(tag: String, message: String) {
        Timber.d("[$tag] $message")
    }
    
    /**
     * Log de erro
     */
    protected fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.e(throwable, "[$tag] $message")
        } else {
            Timber.e("[$tag] $message")
        }
    }
    
    /**
     * Log de operação
     */
    protected fun logOperation(operation: String, details: String = "") {
        Timber.d("[$operation] $details")
    }
    
    /**
     * Log de dados
     */
    protected fun logData(tag: String, data: Any) {
        Timber.d("[$tag] Dados: $data")
    }
    
    // ==================== MÉTODOS DE CICLO DE VIDA ====================
    
    override fun onCleared() {
        super.onCleared()
        clearAllStates()
        logState("LIFECYCLE", "ViewModel limpo")
    }
}
