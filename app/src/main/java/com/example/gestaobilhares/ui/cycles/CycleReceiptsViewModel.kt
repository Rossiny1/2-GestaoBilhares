package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * ViewModel para CycleReceiptsFragment
 * TODO: Implementar funcionalidade completa quando necessário
 */
class CycleReceiptsViewModel(
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val appRepository: AppRepository
) : BaseViewModel() {
    
    private val _receipts = MutableStateFlow<List<CycleReceiptItem>>(emptyList())
    val receipts: StateFlow<List<CycleReceiptItem>> = _receipts.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // isLoading já existe na BaseViewModel
    
    /**
     * Carrega recebimentos do ciclo
     * TODO: Implementar lógica completa de carregamento
     */
    fun carregarRecebimentos(cicloId: Long) {
        viewModelScope.launch {
            try {
                showLoading()
                // TODO: Implementar carregamento real dos recebimentos
                _receipts.value = emptyList()
                hideLoading()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar recebimentos: ${e.message}"
                hideLoading()
            }
        }
    }
    
    /**
     * Limpa mensagem de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }
}

