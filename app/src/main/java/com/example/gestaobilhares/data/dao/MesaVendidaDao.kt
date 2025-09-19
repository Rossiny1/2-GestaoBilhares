package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.MesaVendida
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de MesaVendida no banco de dados
 */
@Dao
interface MesaVendidaDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(mesaVendida: MesaVendida): Long

    @Query("SELECT * FROM mesas_vendidas ORDER BY data_venda DESC")
    fun listarTodas(): Flow<List<MesaVendida>>

    @Query("SELECT * FROM mesas_vendidas WHERE id = :id")
    suspend fun buscarPorId(id: Long): MesaVendida?

    @Query("SELECT * FROM mesas_vendidas WHERE mesa_id_original = :mesaIdOriginal")
    suspend fun buscarPorMesaOriginal(mesaIdOriginal: Long): MesaVendida?

    @Query("SELECT * FROM mesas_vendidas WHERE numero_mesa LIKE '%' || :numero || '%' ORDER BY data_venda DESC")
    fun buscarPorNumero(numero: String): Flow<List<MesaVendida>>

    @Query("SELECT * FROM mesas_vendidas WHERE nome_comprador LIKE '%' || :nome || '%' ORDER BY data_venda DESC")
    fun buscarPorComprador(nome: String): Flow<List<MesaVendida>>

    @Query("SELECT * FROM mesas_vendidas WHERE data_venda BETWEEN :dataInicio AND :dataFim ORDER BY data_venda DESC")
    fun buscarPorPeriodo(dataInicio: Long, dataFim: Long): Flow<List<MesaVendida>>

    @Query("SELECT SUM(valor_venda) FROM mesas_vendidas")
    suspend fun calcularTotalVendas(): Double?

    @Query("SELECT COUNT(*) FROM mesas_vendidas")
    suspend fun contarTotalVendas(): Int

    @Query("SELECT * FROM mesas_vendidas WHERE tipo_mesa = :tipoMesa ORDER BY data_venda DESC")
    fun buscarPorTipo(tipoMesa: String): Flow<List<MesaVendida>>

    @Update
    suspend fun atualizar(mesaVendida: MesaVendida)

    @Delete
    suspend fun deletar(mesaVendida: MesaVendida)

    @Query("DELETE FROM mesas_vendidas WHERE id = :id")
    suspend fun deletarPorId(id: Long)
}
