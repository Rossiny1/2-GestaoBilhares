package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Cliente
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de cliente no banco de dados
 */
@Dao
interface ClienteDao {

    @Query("SELECT * FROM clientes WHERE rota_id = :rotaId ORDER BY nome ASC")
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>>

    @Insert
    suspend fun inserir(cliente: Cliente): Long

    @Update
    suspend fun atualizar(cliente: Cliente)

    @Delete
    suspend fun deletar(cliente: Cliente)

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun obterPorId(id: Long): Cliente?

    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun obterTodos(): Flow<List<Cliente>>
} 
