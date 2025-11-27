package com.example.gestaobilhares.ui.inventory.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.entities.StockItem as StockItemEntity
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class StockViewModel constructor(
    private val appRepository: AppRepository
) : ViewModel() {
    
    // ✅ CORRIGIDO: Observar diretamente o Flow do banco de dados
    // Isso garante que mudanças no banco sejam refletidas automaticamente na UI
    val stockItems: StateFlow<List<StockItem>> = appRepository.obterTodosStockItems()
        .map { itemsFromDb ->
            android.util.Log.d("StockViewModel", "Mapeando ${itemsFromDb.size} itens do banco")
            itemsFromDb.map { entity ->
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
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _panosEstoque = MutableStateFlow<List<PanoEstoque>>(emptyList())
    val panosEstoque: StateFlow<List<PanoEstoque>> = _panosEstoque.asStateFlow()
    
    // ✅ CORRIGIDO: Observar diretamente o Flow do banco e agrupar panos
    val panoGroups: StateFlow<List<PanoGroup>> = appRepository.obterTodosPanosEstoque()
        .map { panos ->
            android.util.Log.d("StockViewModel", "Agrupando ${panos.size} panos")
            agruparPanos(panos)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // ✅ CORRIGIDO: Observar panos em background para manter _panosEstoque atualizado
        viewModelScope.launch {
            appRepository.obterTodosPanosEstoque().collect { panos ->
                _panosEstoque.value = panos
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
                // Usar AppRepository para inserir com sincronização
                panos.forEach { pano ->
                    appRepository.inserirPanoEstoque(pano)
                }
                // ✅ CORREÇÃO: O Flow do banco de dados já atualiza automaticamente
                // Não precisamos recarregar manualmente
                android.util.Log.d("StockViewModel", "Panos adicionados em lote - Flow irá atualizar automaticamente")
            } catch (e: Exception) {
                android.util.Log.e("StockViewModel", "Erro ao adicionar panos em lote: ${e.message}", e)
            }
        }
    }
    
    /**
     * ✅ REMOVIDO: refreshData() não é mais necessário
     * O Flow do banco de dados já atualiza automaticamente quando há mudanças
     */
    
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
                val id = appRepository.inserirStockItem(entity)
                android.util.Log.d("StockViewModel", "Item inserido com ID: $id")
                
                // ✅ CORREÇÃO: O Flow do banco de dados já irá notificar automaticamente
                // Não precisamos fazer atualização manual nem recarregar
                android.util.Log.d("StockViewModel", "Item adicionado com sucesso - Flow irá atualizar automaticamente")
                
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

