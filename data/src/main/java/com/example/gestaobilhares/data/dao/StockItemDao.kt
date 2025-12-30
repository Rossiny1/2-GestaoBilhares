package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.StockItem
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de StockItem no banco de dados
 */
@Dao
interface StockItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(stockItem: StockItem): Long

    @Update
    suspend fun atualizar(stockItem: StockItem)

    @Delete
    suspend fun deletar(stockItem: StockItem)

    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun listarTodos(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE id = :id")
    suspend fun buscarPorId(id: Long): StockItem?

    @Query("SELECT * FROM stock_items WHERE category = :category ORDER BY name ASC")
    fun buscarPorCategoria(category: String): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    fun buscarPorNome(search: String): Flow<List<StockItem>>

    @Query("UPDATE stock_items SET quantity = :newQuantity, updated_at = :updatedAt WHERE id = :id")
    suspend fun atualizarQuantidade(id: Long, newQuantity: Int, updatedAt: Long)

    @Query("DELETE FROM stock_items WHERE id = :id")
    suspend fun deletarPorId(id: Long)
}
