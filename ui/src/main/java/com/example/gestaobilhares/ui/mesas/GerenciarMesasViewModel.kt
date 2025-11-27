package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
data class EstatisticasGerais(
    val totalSinuca: Int = 0,
    val totalJukebox: Int = 0,
    val totalPembolim: Int = 0,
    val depositoSinuca: Int = 0,
    val depositoJukebox: Int = 0,
    val depositoPembolim: Int = 0
)

data class RotaComMesas(
    val rota: Rota,
    val sinuca: Int = 0,
    val jukebox: Int = 0,
    val pembolim: Int = 0
)

class GerenciarMesasViewModel constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _estatisticasGerais = MutableStateFlow(EstatisticasGerais())
    val estatisticasGerais: StateFlow<EstatisticasGerais> = _estatisticasGerais.asStateFlow()

    private val _rotasComMesas = MutableStateFlow<List<RotaComMesas>>(emptyList())
    val rotasComMesas: StateFlow<List<RotaComMesas>> = _rotasComMesas.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        loadDados()
    }

    fun loadDados() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Carregar todas as rotas
                val rotas = repository.obterTodasRotas().first()

                // Carregar todas as mesas
                val todasMesas = repository.obterTodasMesas().first()

                // Separar mesas do depósito (sem clienteId)
                val mesasDeposito = todasMesas.filter { mesa: Mesa -> mesa.clienteId == null }

                // Calcular estatísticas gerais
                val statsGerais = EstatisticasGerais(
                    totalSinuca = todasMesas.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.SINUCA },
                    totalJukebox = todasMesas.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.JUKEBOX },
                    totalPembolim = todasMesas.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.PEMBOLIM },
                    depositoSinuca = mesasDeposito.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.SINUCA },
                    depositoJukebox = mesasDeposito.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.JUKEBOX },
                    depositoPembolim = mesasDeposito.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.PEMBOLIM }
                )
                _estatisticasGerais.value = statsGerais

                // Calcular estatísticas por rota baseado nos clientes e suas mesas
                val rotasComMesas = rotas.map { rota: Rota ->
                    // Buscar mesas desta rota através dos clientes
                    val mesasDaRota = repository.buscarMesasPorRota(rota.id).first()

                    android.util.Log.d("GerenciarMesasViewModel", "=== ROTA ${rota.nome} (ID: ${rota.id}) ===")
                    android.util.Log.d("GerenciarMesasViewModel", "Mesas encontradas: ${mesasDaRota.size}")
                    mesasDaRota.forEach { mesa ->
                        android.util.Log.d("GerenciarMesasViewModel", "Mesa: ${mesa.numero} (Tipo: ${mesa.tipoMesa}, ClienteId: ${mesa.clienteId})")
                    }

                    RotaComMesas(
                        rota = rota,
                        sinuca = mesasDaRota.count { mesa -> mesa.tipoMesa == TipoMesa.SINUCA },
                        jukebox = mesasDaRota.count { mesa -> mesa.tipoMesa == TipoMesa.JUKEBOX },
                        pembolim = mesasDaRota.count { mesa -> mesa.tipoMesa == TipoMesa.PEMBOLIM }
                    )
                }
                _rotasComMesas.value = rotasComMesas

            } catch (e: Exception) {
                // Tratar erro
            } finally {
                _loading.value = false
            }
        }
    }
}

