package com.example.gestaobilhares.ui.inventory.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Veiculo
import com.example.gestaobilhares.data.repository.VeiculoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    private val repository: VeiculoRepository
) : ViewModel() {

    val vehicles: StateFlow<List<Veiculo>> = repository.listar()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addVehicle(nome: String, placa: String, marca: String, modelo: String, anoModelo: Int, kmAtual: Long) {
        viewModelScope.launch {
            repository.inserir(
                Veiculo(
                    nome = nome.trim(),
                    placa = placa.trim(),
                    marca = marca.trim(),
                    modelo = modelo.trim(),
                    anoModelo = anoModelo,
                    kmAtual = kmAtual
                )
            )
        }
    }
}


