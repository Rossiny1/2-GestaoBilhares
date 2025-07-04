package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Colaborador
import kotlinx.coroutines.flow.Flow

/**
 * ✅ DAO SIMPLIFICADO - ColaboradorDao
 * Mantém apenas métodos essenciais para o negócio
 * Remove métodos desnecessários e duplicados
 */
@Dao
interface ColaboradorDao {
    
    @Query("SELECT * FROM colaboradores ORDER BY nome ASC")
    fun obterTodos(): Flow<List<Colaborador>>
    
    @Query("SELECT * FROM colaboradores WHERE ativo = 1 ORDER BY nome ASC")
    fun obterAtivos(): Flow<List<Colaborador>>
    
    @Query("SELECT * FROM colaboradores WHERE id = :id")
    suspend fun obterPorId(id: Long): Colaborador?
    
    @Query("SELECT * FROM colaboradores WHERE email = :email")
    suspend fun obterPorEmail(email: String): Colaborador?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(colaborador: Colaborador): Long
    
    @Update
    suspend fun atualizar(colaborador: Colaborador)
    
    @Delete
    suspend fun deletar(colaborador: Colaborador)
    
    @Query("SELECT COUNT(*) FROM colaboradores WHERE ativo = 1")
    suspend fun contarAtivos(): Int
} 