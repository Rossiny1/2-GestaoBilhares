package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.TipoDespesa
import com.example.gestaobilhares.data.entities.TipoDespesaComCategoria
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com tipos de despesas
 */
@Dao
interface TipoDespesaDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(tipo: TipoDespesa): Long
    
    @Update
    suspend fun atualizar(tipo: TipoDespesa)
    
    @Delete
    suspend fun deletar(tipo: TipoDespesa)
    
    @Query("SELECT * FROM tipos_despesa WHERE id = :id")
    suspend fun buscarPorId(id: Long): TipoDespesa?
    
    @Query("SELECT * FROM tipos_despesa WHERE categoriaId = :categoriaId AND ativo = 1 ORDER BY nome ASC")
    fun buscarPorCategoria(categoriaId: Long): Flow<List<TipoDespesa>>
    
    @Query("SELECT * FROM tipos_despesa WHERE ativo = 1 ORDER BY nome ASC")
    fun buscarAtivos(): Flow<List<TipoDespesa>>
    
    @Query("SELECT * FROM tipos_despesa ORDER BY nome ASC")
    fun buscarTodos(): Flow<List<TipoDespesa>>
    
    @Query("""
        SELECT t.*, c.nome as categoriaNome 
        FROM tipos_despesa t 
        INNER JOIN categorias_despesa c ON t.categoriaId = c.id 
        WHERE t.ativo = 1 
        ORDER BY c.nome ASC, t.nome ASC
    """)
    fun buscarAtivosComCategoria(): Flow<List<TipoDespesaComCategoria>>
    
    @Query("""
        SELECT t.*, c.nome as categoriaNome 
        FROM tipos_despesa t 
        INNER JOIN categorias_despesa c ON t.categoriaId = c.id 
        WHERE t.categoriaId = :categoriaId AND t.ativo = 1 
        ORDER BY t.nome ASC
    """)
    fun buscarPorCategoriaComCategoria(categoriaId: Long): Flow<List<TipoDespesaComCategoria>>
    
    @Query("SELECT COUNT(*) FROM tipos_despesa WHERE nome = :nome AND categoriaId = :categoriaId")
    suspend fun contarPorNomeECategoria(nome: String, categoriaId: Long): Int
    
    @Query("SELECT COUNT(*) FROM tipos_despesa WHERE categoriaId = :categoriaId AND ativo = 1")
    suspend fun contarAtivosPorCategoria(categoriaId: Long): Int
} 