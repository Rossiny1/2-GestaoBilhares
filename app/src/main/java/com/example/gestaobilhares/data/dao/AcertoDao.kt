package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Acerto
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de Acerto no banco de dados
 */
@Dao
interface AcertoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(acerto: Acerto): Long

    @Query("SELECT * FROM acertos WHERE cliente_id = :clienteId ORDER BY data_acerto DESC")
    fun buscarPorCliente(clienteId: Long): Flow<List<Acerto>>

    @Query("SELECT * FROM acertos ORDER BY data_acerto DESC")
    fun listarTodos(): Flow<List<Acerto>>

    @Query("SELECT * FROM acertos WHERE id = :id")
    suspend fun buscarPorId(id: Long): Acerto?

    @Query("SELECT * FROM acertos WHERE cliente_id = :clienteId ORDER BY data_acerto DESC LIMIT 1")
    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long): Acerto?

    @Update
    suspend fun atualizar(acerto: Acerto)

    @Delete
    suspend fun deletar(acerto: Acerto)
} 