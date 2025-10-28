package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel para o histórico de manutenção de uma mesa.
 */
class HistoricoManutencaoMesaViewModel constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {

    private val _mesa = MutableStateFlow<Mesa?>(null)
    val mesa: StateFlow<Mesa?> = _mesa.asStateFlow()

    private val _historicoManutencao = MutableStateFlow<List<HistoricoManutencaoMesa>>(emptyList())
    val historicoManutencao: StateFlow<List<HistoricoManutencaoMesa>> = _historicoManutencao.asStateFlow()

    // isLoading já existe na BaseViewModel

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Dados resumidos para exibição no EditMesaFragment
    private val _ultimaPintura = MutableStateFlow<HistoricoManutencaoMesa?>(null)
    val ultimaPintura: StateFlow<HistoricoManutencaoMesa?> = _ultimaPintura.asStateFlow()

    private val _ultimaTrocaPano = MutableStateFlow<HistoricoManutencaoMesa?>(null)
    val ultimaTrocaPano: StateFlow<HistoricoManutencaoMesa?> = _ultimaTrocaPano.asStateFlow()

    private val _ultimaTrocaTabela = MutableStateFlow<HistoricoManutencaoMesa?>(null)
    val ultimaTrocaTabela: StateFlow<HistoricoManutencaoMesa?> = _ultimaTrocaTabela.asStateFlow()

    private val _outrasManutencoes = MutableStateFlow<List<HistoricoManutencaoMesa>>(emptyList())
    val outrasManutencoes: StateFlow<List<HistoricoManutencaoMesa>> = _outrasManutencoes.asStateFlow()

    fun carregarHistoricoMesa(mesaId: Long) {
        viewModelScope.launch {
            try {
                showLoading()
                
                // Carregar histórico completo
                val historico = appRepository.obterTodosHistoricoManutencaoMesa().first().filter { it.mesaId == mesaId }
                _historicoManutencao.value = historico
                
                // Carregar dados específicos
                val ultimaPintura = historico.filter { it.tipoManutencao == TipoManutencao.PINTURA }.maxByOrNull { it.dataManutencao }
                _ultimaPintura.value = ultimaPintura
                
                val ultimaTrocaPano = historico.filter { it.tipoManutencao == TipoManutencao.TROCA_PANO }.maxByOrNull { it.dataManutencao }
                _ultimaTrocaPano.value = ultimaTrocaPano
                
                val ultimaTrocaTabela = historico.filter { it.tipoManutencao == TipoManutencao.TROCA_TABELA }.maxByOrNull { it.dataManutencao }
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
                hideLoading()
            }
        }
    }

    fun carregarHistoricoPorNumeroMesa(numeroMesa: String) {
        viewModelScope.launch {
            try {
                showLoading()
                
                val historico = appRepository.obterTodosHistoricoManutencaoMesa().first().filter { it.numeroMesa == numeroMesa }
                _historicoManutencao.value = historico
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar histórico: ${e.message}"
            } finally {
                hideLoading()
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
                showLoading()
                
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
                
                appRepository.inserirHistoricoManutencaoMesaSync(historico)
                
                // Recarregar dados
                carregarHistoricoMesa(mesaId)
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao registrar manutenção: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    // clearError já existe na BaseViewModel
}

