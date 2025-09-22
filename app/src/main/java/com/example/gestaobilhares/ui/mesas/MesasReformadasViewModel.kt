package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.repository.MesaReformadaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para a tela de mesas reformadas.
 * Gerencia o estado e operações relacionadas às mesas reformadas.
 */
@HiltViewModel
class MesasReformadasViewModel @Inject constructor(
    private val mesaReformadaRepository: MesaReformadaRepository
) : ViewModel() {

    private val _mesasReformadas = MutableLiveData<List<MesaReformada>>()
    val mesasReformadas: LiveData<List<MesaReformada>> = _mesasReformadas

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun carregarMesasReformadas() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                mesaReformadaRepository.listarTodas().collect { mesas ->
                    _mesasReformadas.value = mesas
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar mesas reformadas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
