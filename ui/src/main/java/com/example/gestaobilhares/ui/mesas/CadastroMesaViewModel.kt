package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CadastroMesaViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    
    /**
     * ✅ CORREÇÃO: Verifica se o número da mesa já existe usando coroutines de forma segura
     */
    suspend fun verificarNumeroMesaExistente(numero: String): Boolean {
        return try {
            android.util.Log.d("CadastroMesaViewModel", "🔍 Verificando se número de mesa '$numero' já existe...")
            
            // Buscar todas as mesas (disponíveis e em uso)
            val todasMesas = appRepository.obterTodasMesas().first()
            val numeroExiste = todasMesas.any { it.numero.equals(numero, ignoreCase = true) }
            
            android.util.Log.d("CadastroMesaViewModel", "Resultado da verificação: $numeroExiste")
            numeroExiste
            
        } catch (e: Exception) {
            android.util.Log.e("CadastroMesaViewModel", "❌ Erro ao verificar número da mesa: ${e.message}", e)
            false // Em caso de erro, permitir o cadastro
        }
    }
    
    /**
     * ✅ MANTIDO: Função com callback para compatibilidade (mas agora mais segura)
     */
    fun verificarNumeroMesaExistente(numero: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val existe = verificarNumeroMesaExistente(numero)
            callback(existe)
        }
    }
    
    fun salvarMesa(mesa: Mesa) {
        viewModelScope.launch {
            android.util.Log.d("CadastroMesaViewModel", "=== SALVANDO MESA ===")
            android.util.Log.d("CadastroMesaViewModel", "Número: ${mesa.numero}")
            android.util.Log.d("CadastroMesaViewModel", "Ativa: ${mesa.ativa}")
            android.util.Log.d("CadastroMesaViewModel", "ClienteId: ${mesa.clienteId}")
            android.util.Log.d("CadastroMesaViewModel", "Tipo: ${mesa.tipoMesa}")
            android.util.Log.d("CadastroMesaViewModel", "Tamanho: ${mesa.tamanho}")
            
            try {
                val mesaId = appRepository.inserirMesa(mesa)
                android.util.Log.d("CadastroMesaViewModel", "✅ Mesa salva com sucesso! ID: $mesaId")
                
                // Verificar se a mesa aparece na lista de disponíveis
                android.util.Log.d("CadastroMesaViewModel", "Verificando se mesa aparece na lista de disponíveis...")
                val mesas = appRepository.obterMesasDisponiveis().first()
                android.util.Log.d("CadastroMesaViewModel", "Mesas disponíveis após salvar: ${mesas.size}")
                mesas.forEach { m ->
                    android.util.Log.d("CadastroMesaViewModel", "Mesa disponível: ${m.numero} (ID: ${m.id})")
                }
            } catch (e: Exception) {
                android.util.Log.e("CadastroMesaViewModel", "❌ Erro ao salvar mesa: ${e.message}", e)
            }
        }
    }
} 
