package com.example.gestaobilhares.ui.mesas

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.repository.MesaReformadaRepository
import com.example.gestaobilhares.data.repository.PanoEstoqueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para a tela de nova reforma.
 */
@HiltViewModel
class NovaReformaViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val mesaReformadaRepository: MesaReformadaRepository,
    private val panoEstoqueRepository: PanoEstoqueRepository
) : ViewModel() {

    private val _mesasDisponiveis = MutableLiveData<List<Mesa>>()
    val mesasDisponiveis: LiveData<List<Mesa>> = _mesasDisponiveis

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    fun carregarMesasDisponiveis() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val mesas = appRepository.obterMesasDisponiveis().first()
                _mesasDisponiveis.value = mesas
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar mesas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun salvarReforma(mesaReformada: MesaReformada) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                mesaReformadaRepository.inserir(mesaReformada)
                _successMessage.value = "Reforma salva com sucesso!"
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar reforma: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
    
    /**
     * ✅ NOVO: Marca um pano como usado no estoque
     */
    fun marcarPanoComoUsado(panoId: Long, motivo: String = "Usado em reforma") {
        viewModelScope.launch {
            try {
                Log.d("NovaReformaViewModel", "Marcando pano $panoId como usado: $motivo")
                panoEstoqueRepository.marcarPanoComoUsado(panoId, motivo)
                Log.d("NovaReformaViewModel", "Pano $panoId marcado como usado com sucesso")
            } catch (e: Exception) {
                Log.e("NovaReformaViewModel", "Erro ao marcar pano como usado: ${e.message}", e)
                _errorMessage.value = "Erro ao marcar pano como usado: ${e.message}"
            }
        }
    }
}
