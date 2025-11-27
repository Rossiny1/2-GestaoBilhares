package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com categorias de despesas
 */
@Dao
interface CategoriaDespesaDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(categoria: CategoriaDespesa): Long
    
    @Update
    suspend fun atualizar(categoria: CategoriaDespesa)
    
    @Delete
    suspend fun deletar(categoria: CategoriaDespesa)
    
    @Query("SELECT * FROM categorias_despesa WHERE id = :id")
    suspend fun buscarPorId(id: Long): CategoriaDespesa?
    
    @Query("SELECT * FROM categorias_despesa WHERE nome = :nome")
    suspend fun buscarPorNome(nome: String): CategoriaDespesa?
    
    @Query("SELECT * FROM categorias_despesa WHERE ativa = 1 ORDER BY nome ASC")
    fun buscarAtivas(): Flow<List<CategoriaDespesa>>
    
    @Query("SELECT * FROM categorias_despesa ORDER BY nome ASC")
    fun buscarTodas(): Flow<List<CategoriaDespesa>>
    
    @Query("SELECT COUNT(*) FROM categorias_despesa WHERE nome = :nome")
    suspend fun contarPorNome(nome: String): Int
    
    @Query("SELECT COUNT(*) FROM categorias_despesa WHERE ativa = 1")
    suspend fun contarAtivas(): Int
} 