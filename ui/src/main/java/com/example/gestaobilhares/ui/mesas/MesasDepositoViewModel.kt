package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EstatisticasDeposito(
    val totalMesas: Int = 0,
    val mesasSinuca: Int = 0,
    val mesasMaquina: Int = 0,
    val mesasPembolim: Int = 0,
    val mesasOutros: Int = 0,
    val mesasPequenas: Int = 0,
    val mesasMedias: Int = 0,
    val mesasGrandes: Int = 0
)

class MesasDepositoViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    private val _mesasDisponiveis = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasDisponiveis: StateFlow<List<Mesa>> = _mesasDisponiveis.asStateFlow()

    private val _estatisticas = MutableStateFlow(EstatisticasDeposito())
    val estatisticas: StateFlow<EstatisticasDeposito> = _estatisticas.asStateFlow()

    // ✅ ESTADO DE BUSCA POR NÚMERO
    private val _queryNumero = MutableStateFlow("")
    val queryNumero: StateFlow<String> = _queryNumero.asStateFlow()

    // ✅ LISTA FILTRADA REATIVA
    val mesasFiltradas: StateFlow<List<Mesa>> = combine(_mesasDisponiveis, _queryNumero) { mesas, query ->
        val q = query.trim()
        if (q.isEmpty()) mesas else mesas.filter { it.numero.contains(q, ignoreCase = true) }
    }.let { flow ->
        // Converter para StateFlow mantendo último valor
        val state = MutableStateFlow<List<Mesa>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state.asStateFlow()
    }

    fun loadMesasDisponiveis() {
        viewModelScope.launch {
            try {
                appRepository.obterMesasDisponiveis().collect { mesas ->
                    _mesasDisponiveis.value = mesas
                    calcularEstatisticas(mesas)
                }
            } catch (e: Exception) {
                // Silenciar erros para não poluir o log
            }
        }
    }

    // ✅ ATUALIZA A QUERY DE BUSCA
    fun atualizarBuscaNumero(query: String) {
        _queryNumero.value = query
    }

    private fun calcularEstatisticas(mesas: List<Mesa>) {
        val stats = EstatisticasDeposito(
            totalMesas = mesas.size,
            mesasSinuca = mesas.count { it.tipoMesa == TipoMesa.SINUCA },
            mesasMaquina = mesas.count { it.tipoMesa == TipoMesa.JUKEBOX },
            mesasPembolim = mesas.count { it.tipoMesa == TipoMesa.PEMBOLIM },
            mesasOutros = mesas.count { it.tipoMesa == TipoMesa.OUTROS },
            mesasPequenas = mesas.count { it.tamanho == TamanhoMesa.PEQUENA },
            mesasMedias = mesas.count { it.tamanho == TamanhoMesa.MEDIA },
            mesasGrandes = mesas.count { it.tamanho == TamanhoMesa.GRANDE }
        )
        _estatisticas.value = stats
    }

    fun vincularMesaAoCliente(mesaId: Long, clienteId: Long, tipoFixo: Boolean, valorFixo: Double?) {
        viewModelScope.launch {
            try {
                if (tipoFixo && valorFixo != null) {
                    appRepository.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
                } else {
                    appRepository.vincularMesaACliente(mesaId, clienteId)
                }
                loadMesasDisponiveis()
            } catch (e: Exception) {
                // Silenciar erros
            }
        }
    }
    
    suspend fun obterTodasMesasVinculadasAoCliente(clienteId: Long): List<Mesa> {
        return try {
            appRepository.obterMesasPorClienteDireto(clienteId)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun verificarContratoAtivo(clienteId: Long): com.example.gestaobilhares.data.entities.ContratoLocacao? {
        return try {
            val contratos = appRepository.buscarContratosPorCliente(clienteId).first()
            contratos.find { it.status.equals("ATIVO", ignoreCase = true) }
        } catch (e: Exception) {
            null
        }
    }
} 
