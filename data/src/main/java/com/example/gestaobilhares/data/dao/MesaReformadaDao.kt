package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.MesaReformada
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de MesaReformada no banco de dados
 */
@Dao
interface MesaReformadaDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(mesaReformada: MesaReformada): Long

    @Query("SELECT * FROM mesas_reformadas ORDER BY data_reforma DESC")
    fun listarTodas(): Flow<List<MesaReformada>>

    @Query("SELECT * FROM mesas_reformadas WHERE id = :id")
    suspend fun buscarPorId(id: Long): MesaReformada?

    @Query("SELECT * FROM mesas_reformadas WHERE mesa_id = :mesaId")
    suspend fun buscarPorMesaId(mesaId: Long): MesaReformada?

    @Query("SELECT * FROM mesas_reformadas WHERE numero_mesa LIKE '%' || :numero || '%' ORDER BY data_reforma DESC")
    fun buscarPorNumero(numero: String): Flow<List<MesaReformada>>

    @Query("SELECT * FROM mesas_reformadas WHERE data_reforma BETWEEN :dataInicio AND :dataFim ORDER BY data_reforma DESC")
    fun buscarPorPeriodo(dataInicio: Long, dataFim: Long): Flow<List<MesaReformada>>

    @Query("SELECT COUNT(*) FROM mesas_reformadas")
    suspend fun contarTotalReformas(): Int

    @Query("SELECT * FROM mesas_reformadas WHERE tipo_mesa = :tipoMesa ORDER BY data_reforma DESC")
    fun buscarPorTipo(tipoMesa: String): Flow<List<MesaReformada>>

    @Update
    suspend fun atualizar(mesaReformada: MesaReformada)

    @Delete
    suspend fun deletar(mesaReformada: MesaReformada)

    @Query("DELETE FROM mesas_reformadas WHERE id = :id")
    suspend fun deletarPorId(id: Long)
}
