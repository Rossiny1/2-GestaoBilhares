package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Dados de cliente para exibição
 */
data class CycleClientItem(
    val id: Long,
    val nome: String,
    val valorAcertado: Double,
    val dataAcerto: Date,
    val observacoes: String? = null
)

/**
 * ViewModel para gerenciar clientes do ciclo
 */
/**
 * ViewModel para gerenciar clientes do ciclo
 */
@HiltViewModel
class CycleClientsViewModel @Inject constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {

    private val _clientes = MutableStateFlow<List<CycleClientItem>>(emptyList())
    val clientes: StateFlow<List<CycleClientItem>> = _clientes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Carrega clientes do ciclo
     */
    fun carregarClientes(cicloId: Long, rotaId: Long) {
        viewModelScope.launch {
            try {
                showLoading()
                _errorMessage.value = null

                // Buscar acertos reais do ciclo
                val acertosReais = appRepository.buscarAcertosPorCicloId(cicloId).first()
                
                // Buscar clientes para obter os nomes
                val clientes = appRepository.buscarClientesPorRota(rotaId).first()
                val mapaClientes = clientes.associateBy { it.id }
                
                // Mapear para o formato de exibição
                val clientesDTO = acertosReais.map { acerto ->
                    val cliente = mapaClientes[acerto.clienteId]
                    CycleClientItem(
                        id = acerto.clienteId,
                        nome = cliente?.nome ?: "Cliente ${acerto.clienteId}",
                        valorAcertado = acerto.valorRecebido,
                        dataAcerto = acerto.dataAcerto,
                        observacoes = acerto.observacoes
                    )
                }

                _clientes.value = clientesDTO

            } catch (e: Exception) {
                android.util.Log.e("CycleClientsViewModel", "Erro ao carregar clientes: ${e.message}")
                _errorMessage.value = "Erro ao carregar clientes: ${e.message}"
            } finally {
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
