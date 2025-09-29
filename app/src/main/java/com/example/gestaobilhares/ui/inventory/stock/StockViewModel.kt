package com.example.gestaobilhares.ui.inventory.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.entities.StockItem as StockItemEntity
import com.example.gestaobilhares.data.repository.PanoEstoqueRepository
import com.example.gestaobilhares.data.repository.StockItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockViewModel @Inject constructor(
    private val panoEstoqueRepository: PanoEstoqueRepository,
    private val stockItemRepository: StockItemRepository
) : ViewModel() {
    
    private val _stockItems = MutableStateFlow<List<StockItem>>(emptyList())
    val stockItems: StateFlow<List<StockItem>> = _stockItems.asStateFlow()
    
    private val _panosEstoque = MutableStateFlow<List<PanoEstoque>>(emptyList())
    val panosEstoque: StateFlow<List<PanoEstoque>> = _panosEstoque.asStateFlow()
    
    private val _panoGroups = MutableStateFlow<List<PanoGroup>>(emptyList())
    val panoGroups: StateFlow<List<PanoGroup>> = _panoGroups.asStateFlow()

    init {
        loadStockItems()
        loadPanosEstoque()
    }

    private fun loadStockItems() {
        viewModelScope.launch {
            try {
                // Tentar carregar itens do banco de dados
                val itemsFromDb = stockItemRepository.listarTodos().first()
                if (itemsFromDb.isNotEmpty()) {
                    _stockItems.value = itemsFromDb.map { entity ->
                        StockItem(
                            id = entity.id,
                            name = entity.name,
                            category = entity.category,
                            quantity = entity.quantity,
                            unitPrice = entity.unitPrice,
                            supplier = entity.supplier
                        )
                    }
                } else {
                    // Se o banco estiver vazio, não fazer nada
                    _stockItems.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("StockViewModel", "Erro ao carregar itens do estoque: ${e.message}", e)
                _stockItems.value = emptyList()
            }
        }
    }
    
    private fun loadPanosEstoque() {
        viewModelScope.launch {
            try {
                panoEstoqueRepository.listarTodos().collect { panos ->
                    _panosEstoque.value = panos
                    // Agrupar panos por características
                    _panoGroups.value = agruparPanos(panos)
                }
            } catch (e: Exception) {
                android.util.Log.e("StockViewModel", "Erro ao carregar panos: ${e.message}", e)
            }
        }
    }
    
    /**
     * ✅ NOVO: Agrupa panos por cor, tamanho e material
     */
    private fun agruparPanos(panos: List<PanoEstoque>): List<PanoGroup> {
        android.util.Log.d("StockViewModel", "Agrupando ${panos.size} panos")
        
        // Log dos panos antes do agrupamento
        panos.forEach { pano ->
            android.util.Log.d("StockViewModel", "Pano ${pano.numero}: disponivel=${pano.disponivel}, cor=${pano.cor}, tamanho=${pano.tamanho}")
        }
        
        val grupos = panos.groupBy { pano ->
            "${pano.cor}|${pano.tamanho}|${pano.material}"
        }.map { (_, panosGrupo) ->
            val primeiroPano = panosGrupo.first()
            val grupo = PanoGroup(
                cor = primeiroPano.cor,
                tamanho = primeiroPano.tamanho,
                material = primeiroPano.material,
                panos = panosGrupo,
                quantidadeDisponivel = panosGrupo.count { it.disponivel },
                quantidadeTotal = panosGrupo.size
            )
            
            // Log do grupo criado
            android.util.Log.d("StockViewModel", "Grupo ${grupo.cor}-${grupo.tamanho}: ${grupo.quantidadeDisponivel}/${grupo.quantidadeTotal} disponíveis")
            grupo.panos.forEach { pano ->
                android.util.Log.d("StockViewModel", "  Pano ${pano.numero}: disponivel=${pano.disponivel}")
            }
            
            grupo
        }.sortedWith(compareBy<PanoGroup> { it.cor }.thenBy { it.tamanho }.thenBy { it.material })
        
        android.util.Log.d("StockViewModel", "Total de grupos criados: ${grupos.size}")
        return grupos
    }

    /**
     * ✅ NOVO: Adiciona panos em lote ao estoque
     */
    fun adicionarPanosLote(panos: List<PanoEstoque>) {
        viewModelScope.launch {
            try {
                panoEstoqueRepository.inserirLote(panos)
                // Recarregar panos do estoque
                loadPanosEstoque()
            } catch (e: Exception) {
                android.util.Log.e("StockViewModel", "Erro ao adicionar panos em lote: ${e.message}", e)
            }
        }
    }
    
    /**
     * ✅ NOVO: Adiciona um item genérico ao estoque
     */
    fun adicionarItemEstoque(stockItem: StockItem) {
        viewModelScope.launch {
            try {
                android.util.Log.d("StockViewModel", "Adicionando item ao estoque: ${stockItem.name}")
                val entity = StockItemEntity(
                    name = stockItem.name,
                    category = stockItem.category,
                    quantity = stockItem.quantity,
                    unitPrice = stockItem.unitPrice,
                    supplier = stockItem.supplier
                )
                val id = stockItemRepository.inserir(entity)
                android.util.Log.d("StockViewModel", "Item inserido com ID: $id")
                
                // Forçar atualização imediata da lista
                android.util.Log.d("StockViewModel", "Forçando atualização da lista...")
                val currentItems = _stockItems.value.toMutableList()
                val newItem = stockItem.copy(id = id)
                currentItems.add(newItem)
                _stockItems.value = currentItems
                android.util.Log.d("StockViewModel", "Lista atualizada com ${currentItems.size} itens")
                
                // Recarregar itens do estoque para sincronizar com o banco
                loadStockItems()
            } catch (e: Exception) {
                android.util.Log.e("StockViewModel", "Erro ao adicionar item ao estoque: ${e.message}", e)
            }
        }
    }
}

data class StockItem(
    val id: Long,
    val name: String,
    val category: String,
    val quantity: Int,
    val unitPrice: Double,
    val supplier: String
)
