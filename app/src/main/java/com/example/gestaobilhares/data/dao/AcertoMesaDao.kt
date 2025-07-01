package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.AcertoMesa
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de AcertoMesa no banco de dados
 */
@Dao
interface AcertoMesaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(acertoMesa: AcertoMesa): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirLista(acertoMesas: List<AcertoMesa>)

    @Query("SELECT * FROM acerto_mesas WHERE acerto_id = :acertoId")
    fun buscarPorAcerto(acertoId: Long): Flow<List<AcertoMesa>>

    @Query("SELECT * FROM acerto_mesas WHERE mesa_id = :mesaId ORDER BY data_criacao DESC")
    fun buscarPorMesa(mesaId: Long): Flow<List<AcertoMesa>>

    @Query("SELECT * FROM acerto_mesas WHERE mesa_id = :mesaId ORDER BY data_criacao DESC LIMIT 1")
    suspend fun buscarUltimoAcertoMesa(mesaId: Long): AcertoMesa?

    @Query("SELECT * FROM acerto_mesas WHERE acerto_id = :acertoId")
    suspend fun buscarPorAcertoId(acertoId: Long): List<AcertoMesa>

    @Update
    suspend fun atualizar(acertoMesa: AcertoMesa)

    @Delete
    suspend fun deletar(acertoMesa: AcertoMesa)

    @Query("DELETE FROM acerto_mesas WHERE acerto_id = :acertoId")
    suspend fun deletarPorAcerto(acertoId: Long)
} 