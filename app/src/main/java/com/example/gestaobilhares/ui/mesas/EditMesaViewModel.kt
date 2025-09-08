package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.entities.EstadoConservacao
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditMesaViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _mesa = MutableStateFlow<Mesa?>(null)
    val mesa: StateFlow<Mesa?> = _mesa.asStateFlow()

    private val _rotas = MutableStateFlow<List<Rota>>(emptyList())
    val rotas: StateFlow<List<Rota>> = _rotas.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun loadMesa(mesaId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Carregar mesa específica
                val todasMesas = repository.obterTodasMesas().first()
                val mesaEncontrada = todasMesas.find { mesa: Mesa -> mesa.id == mesaId }
                _mesa.value = mesaEncontrada

                // Carregar rotas
                val rotasDisponiveis = repository.obterTodasRotas().first()
                _rotas.value = rotasDisponiveis

            } catch (e: Exception) {
                // Tratar erro
            } finally {
                _loading.value = false
            }
        }
    }

    fun salvarMesa(
        numero: String,
        tipo: TipoMesa,
        tamanho: TamanhoMesa,
        estado: EstadoConservacao,
        rota: String?
    ) {
        viewModelScope.launch {
            _saving.value = true
            try {
                val mesaAtual = _mesa.value
                if (mesaAtual != null) {
                    // Atualizar mesa existente
                    val mesaAtualizada = mesaAtual.copy(
                        numero = numero,
                        tipoMesa = tipo,
                        tamanho = tamanho,
                        estadoConservacao = estado
                    )
                    repository.atualizarMesa(mesaAtualizada)
                } else {
                    // Criar nova mesa
                    val novaMesa = Mesa(
                        numero = numero,
                        tipoMesa = tipo,
                        tamanho = tamanho,
                        estadoConservacao = estado,
                        ativa = true
                    )
                    repository.inserirMesa(novaMesa)
                }
            } catch (e: Exception) {
                // Tratar erro
            } finally {
                _saving.value = false
            }
        }
    }

    fun excluirMesa() {
        viewModelScope.launch {
            _saving.value = true
            try {
                _mesa.value?.let { mesa ->
                    repository.deletarMesa(mesa)
                }
            } catch (e: Exception) {
                // Tratar erro
            } finally {
                _saving.value = false
            }
        }
    }
}