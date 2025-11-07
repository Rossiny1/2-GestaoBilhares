package com.example.gestaobilhares.ui.inventory.equipments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Equipment as EquipmentEntity
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar a lista de equipamentos.
 * Carrega equipamentos do banco de dados e permite adicionar novos.
 */
class EquipmentsViewModel constructor(
    private val appRepository: AppRepository
) : ViewModel() {
    
    private val _equipments = MutableStateFlow<List<Equipment>>(emptyList())
    val equipments: StateFlow<List<Equipment>> = _equipments.asStateFlow()

    init {
        loadEquipments()
    }

    /**
     * Carrega todos os equipamentos do banco de dados.
     * Observa mudanças automaticamente através do Flow.
     */
    private fun loadEquipments() {
        viewModelScope.launch {
            try {
                appRepository.obterTodosEquipments().collect { equipmentsFromDb ->
                    android.util.Log.d("EquipmentsViewModel", "Carregando ${equipmentsFromDb.size} equipamentos do banco")
                    _equipments.value = equipmentsFromDb.map { entity ->
                        Equipment(
                            id = entity.id,
                            name = entity.name,
                            description = entity.description ?: "",
                            quantity = entity.quantity,
                            location = entity.location ?: ""
                        )
                    }
                    android.util.Log.d("EquipmentsViewModel", "Lista atualizada com ${_equipments.value.size} equipamentos")
                }
            } catch (e: Exception) {
                android.util.Log.e("EquipmentsViewModel", "Erro ao carregar equipamentos: ${e.message}", e)
                _equipments.value = emptyList()
            }
        }
    }

    /**
     * Adiciona um novo equipamento ao banco de dados.
     * O Flow irá atualizar automaticamente a lista.
     */
    fun adicionarEquipment(equipment: Equipment) {
        viewModelScope.launch {
            try {
                android.util.Log.d("EquipmentsViewModel", "Adicionando equipamento: ${equipment.name}")
                val entity = EquipmentEntity(
                    name = equipment.name,
                    description = equipment.description.ifEmpty { null },
                    quantity = equipment.quantity,
                    location = equipment.location.ifEmpty { null }
                )
                val id = appRepository.inserirEquipment(entity)
                android.util.Log.d("EquipmentsViewModel", "Equipamento inserido com ID: $id")
                // O Flow do banco de dados já irá notificar automaticamente
                android.util.Log.d("EquipmentsViewModel", "Equipamento adicionado com sucesso - Flow irá atualizar automaticamente")
            } catch (e: Exception) {
                android.util.Log.e("EquipmentsViewModel", "Erro ao adicionar equipamento: ${e.message}", e)
            }
        }
    }

    /**
     * Recarrega todos os equipamentos do banco de dados.
     */
    fun refreshData() {
        android.util.Log.d("EquipmentsViewModel", "Recarregando dados dos equipamentos...")
        loadEquipments()
    }
}

/**
 * Data class para representar um equipamento na UI.
 * Campos: Nome, Descrição, Quantidade e Localização.
 */
data class Equipment(
    val id: Long = 0,
    val name: String,
    val description: String,
    val quantity: Int,
    val location: String
)

