package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class EstatisticasRota(
    val totalSinuca: Int = 0,
    val totalJukebox: Int = 0,
    val totalPembolim: Int = 0
)

@HiltViewModel
class RotaMesasViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _mesasRota = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasRota: StateFlow<List<Mesa>> = _mesasRota.asStateFlow()

    private val _estatisticas = MutableStateFlow(EstatisticasRota())
    val estatisticas: StateFlow<EstatisticasRota> = _estatisticas.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadMesasRota(rotaId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Carregar mesas específicas da rota via JOIN com clientes
                // Isso garante que apenas mesas associadas a clientes daquela rota apareçam
                val mesasDaRota = repository.buscarMesasPorRota(rotaId).first()

                android.util.Log.d("RotaMesasViewModel", "=== MESAS DA ROTA $rotaId ===")
                android.util.Log.d("RotaMesasViewModel", "Encontradas ${mesasDaRota.size} mesas para a rota")
                mesasDaRota.forEach { mesa ->
                    android.util.Log.d("RotaMesasViewModel", "Mesa: ${mesa.numero} | ID: ${mesa.id} | ClienteId: ${mesa.clienteId} | Tipo: ${mesa.tipoMesa}")
                }

                _mesasRota.value = mesasDaRota

                // Calcular estatísticas
                val stats = EstatisticasRota(
                    totalSinuca = mesasDaRota.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.SINUCA },
                    totalJukebox = mesasDaRota.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.JUKEBOX },
                    totalPembolim = mesasDaRota.count { mesa: Mesa -> mesa.tipoMesa == TipoMesa.PEMBOLIM }
                )

                android.util.Log.d("RotaMesasViewModel", "Estatísticas: Sinuca=${stats.totalSinuca}, Jukebox=${stats.totalJukebox}, Pembolim=${stats.totalPembolim}")

                _estatisticas.value = stats

            } catch (e: Exception) {
                android.util.Log.e("RotaMesasViewModel", "Erro ao carregar mesas da rota: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }
}

