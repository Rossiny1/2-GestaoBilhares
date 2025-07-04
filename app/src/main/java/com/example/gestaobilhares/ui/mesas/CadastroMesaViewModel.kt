package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.MesaRepository
import kotlinx.coroutines.launch

class CadastroMesaViewModel(
    private val mesaRepository: MesaRepository
) : ViewModel() {
    fun salvarMesa(mesa: Mesa) {
        viewModelScope.launch {
            android.util.Log.d("CadastroMesaViewModel", "=== SALVANDO MESA ===")
            android.util.Log.d("CadastroMesaViewModel", "Número: ${mesa.numero}")
            android.util.Log.d("CadastroMesaViewModel", "Ativa: ${mesa.ativa}")
            android.util.Log.d("CadastroMesaViewModel", "ClienteId: ${mesa.clienteId}")
            android.util.Log.d("CadastroMesaViewModel", "Tipo: ${mesa.tipoMesa}")
            android.util.Log.d("CadastroMesaViewModel", "Tamanho: ${mesa.tamanho}")
            
            try {
                val mesaId = mesaRepository.inserir(mesa)
                android.util.Log.d("CadastroMesaViewModel", "✅ Mesa salva com sucesso! ID: $mesaId")
                
                // Verificar se a mesa aparece na lista de disponíveis
                android.util.Log.d("CadastroMesaViewModel", "Verificando se mesa aparece na lista de disponíveis...")
                mesaRepository.obterMesasDisponiveis().collect { mesas ->
                    android.util.Log.d("CadastroMesaViewModel", "Mesas disponíveis após salvar: ${mesas.size}")
                    mesas.forEach { m ->
                        android.util.Log.d("CadastroMesaViewModel", "Mesa disponível: ${m.numero} (ID: ${m.id})")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CadastroMesaViewModel", "❌ Erro ao salvar mesa: ${e.message}", e)
            }
        }
    }
} 