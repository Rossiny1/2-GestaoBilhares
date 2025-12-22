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
import timber.log.Timber

/**
 * ViewModel para gerenciar a transferência de clientes entre rotas.
 */
@HiltViewModel
class TransferClientViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _rotasDisponiveis = MutableLiveData<List<Rota>>()
    val rotasDisponiveis: LiveData<List<Rota>> = _rotasDisponiveis

    private val _transferSuccess = MutableLiveData<Boolean>()
    val transferSuccess: LiveData<Boolean> = _transferSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Carrega todas as rotas disponíveis exceto a rota de origem.
     */
    fun loadRotasDisponiveis(rotaOrigemId: Long) {
        viewModelScope.launch {
            try {
                // Usar first() para obter a lista de rotas do Flow de forma assíncrona
                val todasRotas = appRepository.obterTodasRotas().first()
                val rotasDisponiveis = todasRotas.filter { it.id != rotaOrigemId }
                _rotasDisponiveis.value = rotasDisponiveis
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar rotas: ${e.message}"
            }
        }
    }

    /**
     * Transfere um cliente de uma rota para outra.
     */
    fun transferirCliente(
        cliente: Cliente,
        rotaOrigem: Rota,
        rotaDestinoNome: String,
        mesas: List<Mesa>
    ) {
        viewModelScope.launch {
            try {
                // Buscar a rota de destino pelo nome
                val rotaDestino = appRepository.obterRotaPorNome(rotaDestinoNome)
                    ?: throw Exception("Rota de destino não encontrada")

                // Atualizar o cliente com a nova rota
                val clienteAtualizado = cliente.copy(rotaId = rotaDestino.id)
                appRepository.atualizarCliente(clienteAtualizado)

                // ✅ NOVO: Log para debug da transferência
                Timber.d("TransferClientViewModel", "✅ Cliente '${cliente.nome}' transferido de '${rotaOrigem.nome}' para '${rotaDestino.nome}'")
                Timber.d("TransferClientViewModel", "📊 Mesas transferidas: ${mesas.size} mesas")

                // As mesas não precisam ser atualizadas pois já estão vinculadas ao cliente
                // que foi transferido para a nova rota

                _transferSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Erro na transferência: ${e.message}"
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

