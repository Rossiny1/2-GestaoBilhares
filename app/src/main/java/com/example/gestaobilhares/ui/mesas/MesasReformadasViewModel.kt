package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.repository.MesaReformadaRepository
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * ✅ NOVO: Data class para agrupar reformas por mesa com histórico
 */
data class MesaReformadaComHistorico(
    val numeroMesa: String,
    val mesaId: Long,
    val tipoMesa: String,
    val tamanhoMesa: String,
    val reformas: List<MesaReformada>,
    val historicoManutencoes: List<HistoricoManutencaoMesa>
) {
    // Data da última reforma
    val dataUltimaReforma = reformas.maxByOrNull { it.dataReforma.time }?.dataReforma
    
    // Total de reformas
    val totalReformas = reformas.size
}

/**
 * ViewModel para a tela de mesas reformadas.
 * Gerencia o estado e operações relacionadas às mesas reformadas.
 * ✅ NOVO: Agrupa reformas por mesa e inclui histórico de manutenções
 */
class MesasReformadasViewModel constructor(
    private val mesaReformadaRepository: MesaReformadaRepository,
    private val appRepository: AppRepository
) : BaseViewModel() {

    private val _mesasReformadas = MutableStateFlow<List<MesaReformadaComHistorico>>(emptyList())
    val mesasReformadas: StateFlow<List<MesaReformadaComHistorico>> = _mesasReformadas.asStateFlow()

    // isLoading já existe na BaseViewModel

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarMesasReformadas() {
        viewModelScope.launch {
            try {
                showLoading()
                // ✅ NOVO: Combinar reformas e histórico, agrupando por mesa
                combine(
                    mesaReformadaRepository.listarTodas(),
                    appRepository.obterTodosHistoricoManutencaoMesa()
                ) { mesasReformadas, historicoManutencoes ->
                    // Agrupar reformas por número da mesa
                    val reformasPorMesa = mesasReformadas.groupBy { it.numeroMesa }
                    
                    // Criar lista de MesaReformadaComHistorico
                    reformasPorMesa.map { (numeroMesa, reformas) ->
                        val primeiraReforma = reformas.first()
                        val historicoDaMesa = historicoManutencoes.filter { 
                            it.numeroMesa == numeroMesa 
                        }.sortedByDescending { it.dataManutencao.time }
                        
                        MesaReformadaComHistorico(
                            numeroMesa = numeroMesa,
                            mesaId = primeiraReforma.mesaId,
                            tipoMesa = primeiraReforma.tipoMesa.name,
                            tamanhoMesa = primeiraReforma.tamanhoMesa.name,
                            reformas = reformas.sortedByDescending { it.dataReforma.time },
                            historicoManutencoes = historicoDaMesa
                        )
                    }.sortedByDescending { it.dataUltimaReforma?.time ?: 0L }
                }.collect { mesasAgrupadas ->
                    _mesasReformadas.value = mesasAgrupadas
                    hideLoading() // Ocultar loading após primeira coleta
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar mesas reformadas: ${e.message}"
                hideLoading()
            }
        }
    }

    // clearError já existe na BaseViewModel
}

