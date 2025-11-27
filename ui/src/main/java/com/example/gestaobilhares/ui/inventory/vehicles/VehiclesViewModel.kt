package com.example.gestaobilhares.ui.inventory.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Veiculo
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VehiclesViewModel constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    // ✅ CORRIGIDO: Observar veículos do banco de dados usando Flow
    val vehicles: StateFlow<List<Veiculo>> = appRepository.obterTodosVeiculos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addVehicle(nome: String, placa: String, marca: String, modelo: String, anoModelo: Int, kmAtual: Long) {
        viewModelScope.launch {
            try {
                android.util.Log.d("VehiclesViewModel", "Adicionando veículo: $nome - $placa")
                val veiculo = Veiculo(
                    nome = nome.trim(),
                    placa = placa.trim(),
                    marca = marca.trim(),
                    modelo = modelo.trim(),
                    anoModelo = anoModelo,
                    kmAtual = kmAtual
                )
                val id = appRepository.inserirVeiculo(veiculo)
                android.util.Log.d("VehiclesViewModel", "Veículo inserido com ID: $id")
                // O Flow do banco de dados já irá notificar automaticamente
            } catch (e: Exception) {
                android.util.Log.e("VehiclesViewModel", "Erro ao adicionar veículo: ${e.message}", e)
            }
        }
    }
}

