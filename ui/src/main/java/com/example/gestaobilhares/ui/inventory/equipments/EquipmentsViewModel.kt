package com.example.gestaobilhares.ui.inventory.equipments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar a lista de equipamentos.
 * Carrega equipamentos do banco de dados e permite adicionar novos.
 * 
 * ✅ CORRIGIDO: Agora usa banco de dados Room com Flow reativo
 */
class EquipmentsViewModel constructor(
    private val appRepository: AppRepository
) : ViewModel() {
    
    // ✅ CORRIGIDO: Observar Flow reativo do banco de dados
    val equipments: StateFlow<List<Equipment>> = appRepository.obterTodosEquipments()
        .map { entities ->
            entities.map { entity ->
                Equipment(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description ?: "",
                    quantity = entity.quantity,
                    location = entity.location ?: ""
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    /**
     * Adiciona um novo equipamento ao banco de dados.
     * ✅ CORRIGIDO: Salva no banco e o Flow atualiza automaticamente
     */
    fun adicionarEquipment(equipment: Equipment) {
        viewModelScope.launch {
            try {
                android.util.Log.d("EquipmentsViewModel", "Adicionando equipamento: ${equipment.name}")
                
                val entity = com.example.gestaobilhares.data.entities.Equipment(
                    name = equipment.name,
                    description = equipment.description.ifEmpty { null },
                    quantity = equipment.quantity,
                    location = equipment.location.ifEmpty { null }
                )
                val id = appRepository.inserirEquipment(entity)
                android.util.Log.d("EquipmentsViewModel", "Equipamento salvo com ID: $id")
                
            } catch (e: Exception) {
                android.util.Log.e("EquipmentsViewModel", "Erro ao adicionar equipamento: ${e.message}", e)
            }
        }
    }

    /**
     * Recarrega todos os equipamentos.
     * Não precisa fazer nada, pois o Flow já atualiza automaticamente
     */
    fun refreshData() {
        android.util.Log.d("EquipmentsViewModel", "refreshData chamado - Flow já atualiza automaticamente")
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

