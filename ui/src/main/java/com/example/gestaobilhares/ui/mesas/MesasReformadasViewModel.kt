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
                // ✅ CORRIGIDO: Combinar com todas as mesas para obter dados atualizados de tipo/tamanho
                combine(
                    appRepository.obterTodasMesasReformadas(),
                    appRepository.obterTodosHistoricoManutencaoMesa(),
                    appRepository.obterTodasMesas(),
                    _filtroNumeroMesa
                ) { reformas: List<MesaReformada>, historico: List<HistoricoManutencaoMesa>, todasMesas: List<com.example.gestaobilhares.data.entities.Mesa>, filtro: String? ->
                    
                    // 1. Identificar todos os identificadores únicos de mesas que têm alguma atividade
                    val idsReformas = reformas.map { if (it.mesaId != 0L) it.mesaId else it.numeroMesa }.toSet()
                    val idsHistorico = historico.map { if (it.mesaId != 0L) it.mesaId else it.numeroMesa }.toSet()
                    val todosIdsComAtividade = idsReformas + idsHistorico
                    
                    // Criar mapa de mesas para consulta rápida de dados atuais
                    val mesaInfoMap = todasMesas.associateBy { it.id }
                    
                    // 2. Agrupar atividades por mesa
                    val mesasAgrupadas = todosIdsComAtividade.map { key ->
                        val reformasDaMesa = reformas.filter { 
                            if (key is Long) it.mesaId == key else it.numeroMesa == key
                        }.sortedByDescending { it.dataReforma.time }
                        
                        val historicoDaMesa = historico.filter { 
                            if (key is Long) it.mesaId == key else it.numeroMesa == key
                        }.sortedByDescending { it.dataManutencao.time }
                        
                        // Determinar número, tipo e tamanho da mesa (preferir dados atuais do banco se houver)
                        val mesaAtual = if (key is Long) mesaInfoMap[key] else null
                        
                        val numeroMesa = mesaAtual?.numero 
                            ?: reformasDaMesa.firstOrNull()?.numeroMesa 
                            ?: historicoDaMesa.firstOrNull()?.numeroMesa 
                            ?: key.toString()
                            
                        val tipoMesa = mesaAtual?.tipoMesa?.name
                            ?: reformasDaMesa.firstOrNull()?.tipoMesa?.name
                            ?: "SINUCA"
                            
                        val tamanhoMesa = mesaAtual?.tamanho?.name
                            ?: reformasDaMesa.firstOrNull()?.tamanhoMesa?.name
                            ?: "GRANDE"
                        
                        MesaReformadaComHistorico(
                            numeroMesa = numeroMesa,
                            mesaId = if (key is Long) key else 0L,
                            tipoMesa = tipoMesa,
                            tamanhoMesa = tamanhoMesa,
                            reformas = reformasDaMesa,
                            historicoManutencoes = historicoDaMesa
                        )
                    }.sortedByDescending { it.dataUltimoEvento?.time ?: 0L }
                    
                    // Aplicar filtro se houver
                    if (filtro.isNullOrBlank()) {
                        mesasAgrupadas
                    } else {
                        mesasAgrupadas.filter { mesa: MesaReformadaComHistorico ->
                            mesa.numeroMesa.contains(filtro, ignoreCase = true) 
                        }
                    }
                }.collect { mesasFiltradas: List<MesaReformadaComHistorico> ->
                    _mesasReformadas.value = mesasFiltradas
                    hideLoading()
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

