package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
// TODO: MesaReformadaRepository não existe - usar AppRepository quando método estiver disponível
// import com.example.gestaobilhares.data.repository.MesaReformadaRepository
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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
@HiltViewModel
class MesasReformadasViewModel @Inject constructor(
    // TODO: MesaReformadaRepository não existe - usar AppRepository quando método estiver disponível
    // private val mesaReformadaRepository: MesaReformadaRepository,
    private val appRepository: AppRepository
) : BaseViewModel() {

    private val _mesasReformadas = MutableStateFlow<List<MesaReformadaComHistorico>>(emptyList())
    val mesasReformadas: StateFlow<List<MesaReformadaComHistorico>> = _mesasReformadas.asStateFlow()

    // ✅ NOVO: Filtro por número da mesa
    private val _filtroNumeroMesa = MutableStateFlow<String?>(null)
    val filtroNumeroMesa: StateFlow<String?> = _filtroNumeroMesa.asStateFlow()

    // isLoading já existe na BaseViewModel

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarMesasReformadas() {
        viewModelScope.launch {
            try {
                showLoading()
                // ✅ CORRIGIDO: Usar Flow reativo do banco de dados (igual VehiclesViewModel)
                // O Flow emite automaticamente quando há mudanças no banco
                combine(
                    appRepository.obterTodasMesasReformadas(), // ✅ CORRIGIDO: Flow reativo do banco
                    appRepository.obterTodosHistoricoManutencaoMesa(),
                    _filtroNumeroMesa
                ) { mesasReformadas: List<MesaReformada>, historicoManutencoes: List<HistoricoManutencaoMesa>, filtro: String? ->
                    // Agrupar reformas por número da mesa
                    val reformasPorMesa = mesasReformadas.groupBy { it.numeroMesa }
                    
                    // Criar lista de MesaReformadaComHistorico
                    val mesasAgrupadas = reformasPorMesa.map { (numeroMesa: String, reformas: List<MesaReformada>) ->
                        val primeiraReforma = reformas.first()
                        val historicoDaMesa = historicoManutencoes.filter { historico: HistoricoManutencaoMesa ->
                            historico.numeroMesa == numeroMesa 
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
                    
                    // ✅ NOVO: Aplicar filtro se houver
                    if (filtro.isNullOrBlank()) {
                        mesasAgrupadas
                    } else {
                        mesasAgrupadas.filter { mesa: MesaReformadaComHistorico ->
                            mesa.numeroMesa.contains(filtro, ignoreCase = true) 
                        }
                    }
                }.collect { mesasFiltradas: List<MesaReformadaComHistorico> ->
                    _mesasReformadas.value = mesasFiltradas
                    hideLoading() // Ocultar loading após primeira coleta
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar mesas reformadas: ${e.message}"
                hideLoading()
            }
        }
    }
    
    /**
     * ✅ NOVO: Define o filtro por número da mesa
     */
    fun filtrarPorNumero(numero: String?) {
        _filtroNumeroMesa.value = numero?.trim()?.takeIf { it.isNotEmpty() }
    }
    
    /**
     * ✅ NOVO: Remove o filtro
     */
    fun removerFiltro() {
        _filtroNumeroMesa.value = null
    }

    // clearError já existe na BaseViewModel
}

