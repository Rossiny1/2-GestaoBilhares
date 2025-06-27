package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Mesa
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de mesa no banco de dados
 * Inclui métodos para vincular/desvincular mesas a clientes
 */
@Dao
interface MesaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(mesa: Mesa): Long

    @Update
    suspend fun atualizar(mesa: Mesa)

    @Delete
    suspend fun deletar(mesa: Mesa)

    @Query("SELECT * FROM mesas WHERE cliente_id = :clienteId AND ativa = 1 ORDER BY numero ASC")
    fun obterMesasPorCliente(clienteId: Long): Flow<List<Mesa>>

    @Query("SELECT * FROM mesas WHERE ativa = 0 OR cliente_id IS NULL ORDER BY numero ASC")
    fun obterMesasDisponiveis(): Flow<List<Mesa>>

    @Query("UPDATE mesas SET cliente_id = NULL, ativa = 0 WHERE id = :mesaId")
    suspend fun desvincularMesa(mesaId: Long)

    @Query("UPDATE mesas SET cliente_id = :clienteId, ativa = 1 WHERE id = :mesaId")
    suspend fun vincularMesa(mesaId: Long, clienteId: Long)
} 