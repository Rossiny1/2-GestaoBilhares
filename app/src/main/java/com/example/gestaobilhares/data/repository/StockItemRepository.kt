package com.example.gestaobilhares.data.repository

import android.util.Log
import com.example.gestaobilhares.data.dao.StockItemDao
import com.example.gestaobilhares.data.entities.StockItem
import kotlinx.coroutines.flow.Flow
/**
 * Repository para operações de StockItem
 */
class StockItemRepository constructor(
    private val stockItemDao: StockItemDao
) {
    
    fun listarTodos(): Flow<List<StockItem>> = stockItemDao.listarTodos()
    
    suspend fun buscarPorId(id: Long): StockItem? = stockItemDao.buscarPorId(id)
    
    fun buscarPorCategoria(category: String): Flow<List<StockItem>> = stockItemDao.buscarPorCategoria(category)
    
    fun buscarPorNome(search: String): Flow<List<StockItem>> = stockItemDao.buscarPorNome(search)
    
    suspend fun inserir(stockItem: StockItem): Long {
        return try {
            Log.d("StockItemRepository", "Inserindo item: ${stockItem.name}")
            val id = stockItemDao.inserir(stockItem)
            Log.d("StockItemRepository", "Item inserido com ID: $id")
            id
        } catch (e: Exception) {
            Log.e("StockItemRepository", "Erro ao inserir item: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun atualizar(stockItem: StockItem) {
        try {
            Log.d("StockItemRepository", "Atualizando item: ${stockItem.name}")
            stockItemDao.atualizar(stockItem)
            Log.d("StockItemRepository", "Item atualizado com sucesso")
        } catch (e: Exception) {
            Log.e("StockItemRepository", "Erro ao atualizar item: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun deletar(stockItem: StockItem) {
        try {
            Log.d("StockItemRepository", "Deletando item: ${stockItem.name}")
            stockItemDao.deletar(stockItem)
            Log.d("StockItemRepository", "Item deletado com sucesso")
        } catch (e: Exception) {
            Log.e("StockItemRepository", "Erro ao deletar item: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun atualizarQuantidade(id: Long, newQuantity: Int) {
        try {
            Log.d("StockItemRepository", "Atualizando quantidade do item $id para $newQuantity")
            stockItemDao.atualizarQuantidade(id, newQuantity, java.util.Date())
            Log.d("StockItemRepository", "Quantidade atualizada com sucesso")
        } catch (e: Exception) {
            Log.e("StockItemRepository", "Erro ao atualizar quantidade: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun deletarPorId(id: Long) {
        try {
            Log.d("StockItemRepository", "Deletando item por ID: $id")
            stockItemDao.deletarPorId(id)
            Log.d("StockItemRepository", "Item deletado por ID com sucesso")
        } catch (e: Exception) {
            Log.e("StockItemRepository", "Erro ao deletar item por ID: ${e.message}", e)
            throw e
        }
    }
}

