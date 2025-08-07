package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.repository.MesaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val mesaRepository: MesaRepository
) : ViewModel() {
    private val _mesasDisponiveis = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasDisponiveis: StateFlow<List<Mesa>> = _mesasDisponiveis.asStateFlow()

    private val _estatisticas = MutableStateFlow(EstatisticasDeposito())
    val estatisticas: StateFlow<EstatisticasDeposito> = _estatisticas.asStateFlow()

    fun loadMesasDisponiveis() {
        viewModelScope.launch {
            android.util.Log.d("MesasDepositoViewModel", "=== CARREGANDO MESAS DISPONÍVEIS ===")
            mesaRepository.obterMesasDisponiveis().collect { mesas ->
                android.util.Log.d("MesasDepositoViewModel", "📊 Mesas recebidas do repositório: ${mesas.size}")
                mesas.forEach { mesa ->
                    android.util.Log.d("MesasDepositoViewModel", "Mesa: ${mesa.numero} | ID: ${mesa.id} | Ativa: ${mesa.ativa} | ClienteId: ${mesa.clienteId}")
                }
                _mesasDisponiveis.value = mesas
                android.util.Log.d("MesasDepositoViewModel", "✅ Lista atualizada no StateFlow: ${_mesasDisponiveis.value.size} mesas")
                calcularEstatisticas(mesas)
            }
        }
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
                    // Vincular mesa com valor fixo
                    mesaRepository.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
                } else {
                    // Vincular mesa normal (fichas jogadas)
                    mesaRepository.vincularMesa(mesaId, clienteId)
                }
                loadMesasDisponiveis()
            } catch (e: Exception) {
                android.util.Log.e("MesasDepositoViewModel", "Erro ao vincular mesa: ${e.message}", e)
            }
        }
    }
} 