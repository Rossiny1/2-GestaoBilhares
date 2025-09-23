package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.data.repository.HistoricoManutencaoMesaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para o histórico de manutenção de uma mesa.
 */
@HiltViewModel
class HistoricoManutencaoMesaViewModel @Inject constructor(
    private val historicoManutencaoMesaRepository: HistoricoManutencaoMesaRepository
) : ViewModel() {

    private val _mesa = MutableLiveData<Mesa?>()
    val mesa: LiveData<Mesa?> = _mesa

    private val _historicoManutencao = MutableLiveData<List<HistoricoManutencaoMesa>>()
    val historicoManutencao: LiveData<List<HistoricoManutencaoMesa>> = _historicoManutencao

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Dados resumidos para exibição no EditMesaFragment
    private val _ultimaPintura = MutableLiveData<HistoricoManutencaoMesa?>()
    val ultimaPintura: LiveData<HistoricoManutencaoMesa?> = _ultimaPintura

    private val _ultimaTrocaPano = MutableLiveData<HistoricoManutencaoMesa?>()
    val ultimaTrocaPano: LiveData<HistoricoManutencaoMesa?> = _ultimaTrocaPano

    private val _ultimaTrocaTabela = MutableLiveData<HistoricoManutencaoMesa?>()
    val ultimaTrocaTabela: LiveData<HistoricoManutencaoMesa?> = _ultimaTrocaTabela

    private val _outrasManutencoes = MutableLiveData<List<HistoricoManutencaoMesa>>()
    val outrasManutencoes: LiveData<List<HistoricoManutencaoMesa>> = _outrasManutencoes

    fun carregarHistoricoMesa(mesaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar histórico completo
                val historico = historicoManutencaoMesaRepository.buscarPorMesaId(mesaId).first()
                _historicoManutencao.value = historico
                
                // Carregar dados específicos
                val ultimaPintura = historicoManutencaoMesaRepository.obterUltimaPintura(mesaId)
                _ultimaPintura.value = ultimaPintura
                
                val ultimaTrocaPano = historicoManutencaoMesaRepository.obterUltimaTrocaPano(mesaId)
                _ultimaTrocaPano.value = ultimaTrocaPano
                
                val ultimaTrocaTabela = historicoManutencaoMesaRepository.obterUltimaTrocaTabela(mesaId)
                _ultimaTrocaTabela.value = ultimaTrocaTabela
                
                // Outras manutenções (excluindo pintura, pano e tabela)
                val outras = historico.filter { 
                    it.tipoManutencao != TipoManutencao.PINTURA && 
                    it.tipoManutencao != TipoManutencao.TROCA_PANO && 
                    it.tipoManutencao != TipoManutencao.TROCA_TABELA 
                }
                _outrasManutencoes.value = outras
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar histórico: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarHistoricoPorNumeroMesa(numeroMesa: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val historico = historicoManutencaoMesaRepository.buscarPorNumeroMesa(numeroMesa).first()
                _historicoManutencao.value = historico
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar histórico: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registrarManutencao(
        mesaId: Long,
        numeroMesa: String,
        tipoManutencao: TipoManutencao,
        descricao: String?,
        responsavel: String?,
        observacoes: String?,
        custo: Double? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val historico = HistoricoManutencaoMesa(
                    mesaId = mesaId,
                    numeroMesa = numeroMesa,
                    tipoManutencao = tipoManutencao,
                    descricao = descricao,
                    responsavel = responsavel,
                    observacoes = observacoes,
                    custo = custo,
                    dataManutencao = java.util.Date()
                )
                
                historicoManutencaoMesaRepository.inserir(historico)
                
                // Recarregar dados
                carregarHistoricoMesa(mesaId)
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao registrar manutenção: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
