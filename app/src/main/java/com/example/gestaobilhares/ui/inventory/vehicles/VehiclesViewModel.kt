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

    // TODO: Implementar quando métodos de veículos estiverem disponíveis no AppRepository
    val vehicles: StateFlow<List<Veiculo>> = flowOf(emptyList<Veiculo>())
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addVehicle(nome: String, placa: String, marca: String, modelo: String, anoModelo: Int, kmAtual: Long) {
        viewModelScope.launch {
            // TODO: Implementar quando métodos de veículos estiverem disponíveis
            // appRepository.inserirVeiculo(
            //     Veiculo(
            //         nome = nome.trim(),
            //         placa = placa.trim(),
            //         marca = marca.trim(),
            //         modelo = modelo.trim(),
            //         anoModelo = anoModelo,
            //         kmAtual = kmAtual
            //     )
            // )
        }
    }
}



