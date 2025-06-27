package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.MesaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CadastroMesaViewModel @Inject constructor(
    private val mesaRepository: MesaRepository
) : ViewModel() {
    fun salvarMesa(mesa: Mesa) {
        viewModelScope.launch {
            mesaRepository.inserir(mesa)
        }
    }
} 