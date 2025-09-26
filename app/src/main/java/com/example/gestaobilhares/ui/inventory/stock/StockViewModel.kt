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
                stockItemRepository.listarTodos().collect { stockItems ->
                    _stockItems.value = stockItems.map { entity ->
                        StockItem(
                            id = entity.id,
                            name = entity.name,
                            category = entity.category,
                            quantity = entity.quantity,
                            unitPrice = entity.unitPrice,
                            supplier = entity.supplier
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StockViewModel", "Erro ao carregar itens do estoque: ${e.message}", e)
                // Fallback para dados de exemplo
                _stockItems.value = getSampleStockItems()
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
        return panos.groupBy { pano ->
            "${pano.cor}|${pano.tamanho}|${pano.material}"
        }.map { (_, panosGrupo) ->
            val primeiroPano = panosGrupo.first()
            PanoGroup(
                cor = primeiroPano.cor,
                tamanho = primeiroPano.tamanho,
                material = primeiroPano.material,
                panos = panosGrupo,
                quantidadeDisponivel = panosGrupo.count { it.disponivel },
                quantidadeTotal = panosGrupo.size
            )
        }.sortedWith(compareBy<PanoGroup> { it.cor }.thenBy { it.tamanho }.thenBy { it.material })
    }

    private fun getSampleStockItems(): List<StockItem> {
        return listOf(
            StockItem(
                id = 1L,
                name = "Taco de Sinuca",
                category = "Acessórios",
                quantity = 10,
                unitPrice = 25.0,
                supplier = "Fornecedor A"
            ),
            StockItem(
                id = 2L,
                name = "Bolas de Sinuca",
                category = "Acessórios",
                quantity = 50,
                unitPrice = 5.0,
                supplier = "Fornecedor B"
            )
        )
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
                val entity = StockItemEntity(
                    name = stockItem.name,
                    category = stockItem.category,
                    quantity = stockItem.quantity,
                    unitPrice = stockItem.unitPrice,
                    supplier = stockItem.supplier
                )
                stockItemRepository.inserir(entity)
                // Recarregar itens do estoque
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
