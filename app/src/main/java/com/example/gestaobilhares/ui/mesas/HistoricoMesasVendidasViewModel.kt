package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.MesaVendida
import com.example.gestaobilhares.data.repository.MesaVendidaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para gerenciar o histórico de mesas vendidas
 * ✅ NOVO: SISTEMA DE VENDA DE MESAS
 */
@HiltViewModel
class HistoricoMesasVendidasViewModel @Inject constructor(
    private val mesaVendidaRepository: MesaVendidaRepository
) : ViewModel() {

    private val _mesasVendidas = MutableStateFlow<List<MesaVendida>>(emptyList())
    val mesasVendidas: StateFlow<List<MesaVendida>> = _mesasVendidas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showVendaDialog = MutableStateFlow(false)
    val showVendaDialog: StateFlow<Boolean> = _showVendaDialog.asStateFlow()

    /**
     * Carrega todas as mesas vendidas
     */
    fun carregarMesasVendidas() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HistoricoMesasVendidasViewModel", "🔍 Iniciando carregamento de mesas vendidas...")
                _isLoading.value = true
                
                // Usar try-catch para capturar cancelamento de job
                try {
                    mesaVendidaRepository.listarTodas().collect { mesas ->
                        android.util.Log.d("HistoricoMesasVendidasViewModel", "✅ Mesas vendidas carregadas: ${mesas.size}")
                        _mesasVendidas.value = mesas
                        
                        // Log detalhado das mesas
                        mesas.forEachIndexed { index, mesa ->
                            android.util.Log.d("HistoricoMesasVendidasViewModel", "Mesa ${index + 1}: ${mesa.numeroMesa} - ${mesa.nomeComprador} - R$ ${mesa.valorVenda}")
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("HistoricoMesasVendidasViewModel", "⚠️ Job cancelado - operação normal")
                    throw e // Re-throw para manter o comportamento de cancelamento
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoricoMesasVendidasViewModel", "❌ Erro ao carregar mesas vendidas: ${e.message}", e)
                _errorMessage.value = "Erro ao carregar histórico: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filtra mesas vendidas por número
     */
    fun filtrarPorNumero(numero: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                if (numero.isBlank()) {
                    carregarMesasVendidas()
                } else {
                    mesaVendidaRepository.buscarPorNumero(numero).collect { mesas ->
                        _mesasVendidas.value = mesas
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoricoMesasVendidasViewModel", "Erro ao filtrar por número: ${e.message}")
                _errorMessage.value = "Erro ao filtrar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filtra mesas vendidas por comprador
     */
    fun filtrarPorComprador(nome: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                if (nome.isBlank()) {
                    carregarMesasVendidas()
                } else {
                    mesaVendidaRepository.buscarPorComprador(nome).collect { mesas ->
                        _mesasVendidas.value = mesas
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoricoMesasVendidasViewModel", "Erro ao filtrar por comprador: ${e.message}")
                _errorMessage.value = "Erro ao filtrar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Abre o dialog de venda de mesa
     */
    fun abrirDialogVendaMesa() {
        _showVendaDialog.value = true
    }

    /**
     * Marca que o dialog de venda foi aberto
     */
    fun dialogVendaAberto() {
        _showVendaDialog.value = false
    }

    /**
     * Limpa mensagens de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }

    /**
     * Recarrega os dados após uma venda
     */
    fun recarregarDados() {
        carregarMesasVendidas()
    }
}
