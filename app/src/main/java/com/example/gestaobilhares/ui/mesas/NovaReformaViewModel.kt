package com.example.gestaobilhares.ui.mesas

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.repository.MesaReformadaRepository
import com.example.gestaobilhares.data.repository.PanoEstoqueRepository
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
// import javax.inject.Inject // REMOVIDO: Hilt nao e mais usado

/**
 * ViewModel para a tela de nova reforma.
 */
class NovaReformaViewModel constructor(
    private val appRepository: AppRepository,
    private val mesaReformadaRepository: MesaReformadaRepository,
    private val panoEstoqueRepository: PanoEstoqueRepository
) : BaseViewModel() {

    private val _mesasDisponiveis = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasDisponiveis: StateFlow<List<Mesa>> = _mesasDisponiveis.asStateFlow()

    // isLoading já existe na BaseViewModel

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun carregarMesasDisponiveis() {
        viewModelScope.launch {
            try {
                showLoading()
                val mesas = appRepository.obterMesasDisponiveis().first()
                _mesasDisponiveis.value = mesas
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar mesas: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    fun salvarReforma(mesaReformada: MesaReformada) {
        viewModelScope.launch {
            try {
                showLoading()
                mesaReformadaRepository.inserir(mesaReformada)
                _successMessage.value = "Reforma salva com sucesso!"
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar reforma: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    // clearError já existe na BaseViewModel

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

    /**
     * ✅ NOVO: Atualiza a mesa com o pano selecionado na reforma
     * Garante que `panoAtualId` e `dataUltimaTrocaPano` sejam persistidos na entidade `Mesa`,
     * mesmo que a mesa esteja no depósito e só seja locada depois.
     */
    fun atualizarPanoDaMesaEmReforma(mesaId: Long, panoId: Long) {
        viewModelScope.launch {
            try {
                Log.d("NovaReformaViewModel", "Atualizando mesa $mesaId com pano $panoId (reforma)")
                val mesa = appRepository.obterMesaPorId(mesaId)
                if (mesa == null) {
                    Log.e("NovaReformaViewModel", "Mesa $mesaId não encontrada para atualizar pano em reforma")
                    return@launch
                }
                val atualizada = mesa.copy(
                    panoAtualId = panoId,
                    dataUltimaTrocaPano = Date()
                )
                appRepository.atualizarMesa(atualizada)
                Log.d("NovaReformaViewModel", "Mesa $mesaId atualizada com pano $panoId (reforma) com sucesso")
            } catch (e: Exception) {
                Log.e("NovaReformaViewModel", "Erro ao atualizar pano da mesa em reforma: ${e.message}", e)
                _errorMessage.value = "Erro ao atualizar pano da mesa: ${e.message}"
            }
        }
    }
}

