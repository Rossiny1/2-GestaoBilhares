package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Mesa
import kotlinx.coroutines.flow.Flow
import java.util.Date

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

    @Query("""
        SELECT * FROM mesas 
        WHERE (ativa = 1 AND cliente_id IS NULL) 
        ORDER BY numero ASC
    """)
    fun obterMesasDisponiveis(): Flow<List<Mesa>>

    @Query("UPDATE mesas SET cliente_id = NULL, ativa = 1 WHERE id = :mesaId")
    suspend fun desvincularMesa(mesaId: Long)

    @Query("UPDATE mesas SET cliente_id = :clienteId, ativa = 1 WHERE id = :mesaId")
    suspend fun vincularMesa(mesaId: Long, clienteId: Long)

    @Query("UPDATE mesas SET cliente_id = :clienteId, ativa = 1, valor_fixo = :valorFixo WHERE id = :mesaId")
    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double)

    @Query("""
        UPDATE mesas 
        SET cliente_id = NULL, 
            ativa = 1,
            relogio_inicial = relogio_final
        WHERE id = :mesaId
    """)
    suspend fun retirarMesa(mesaId: Long)
    
    @Query("""
        UPDATE mesas 
        SET relogio_inicial = :relogioInicial,
            relogio_final = :relogioFinal
        WHERE id = :mesaId
    """)
    suspend fun atualizarRelogioMesa(
        mesaId: Long, 
        relogioInicial: Int, 
        relogioFinal: Int
    )
    
    @Query("UPDATE mesas SET relogio_final = :relogioFinal WHERE id = :mesaId")
    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int)
    
    @Query("SELECT * FROM mesas WHERE id = :mesaId")
    suspend fun obterMesaPorId(mesaId: Long): Mesa?

    @Query("SELECT * FROM mesas WHERE cliente_id = :clienteId AND ativa = 1 ORDER BY numero ASC")
    suspend fun obterMesasPorClienteDireto(clienteId: Long): List<Mesa>
    
    /**
     * Retorna a mesa de depósito (cliente nulo) por número, se existir.
     */
    @Query("SELECT * FROM mesas WHERE cliente_id IS NULL AND numero = :numero LIMIT 1")
    suspend fun obterPorNumeroENullCliente(numero: String): Mesa?
    
    @Query("""
        SELECT m.* FROM mesas m
        INNER JOIN clientes c ON m.cliente_id = c.id
        WHERE c.rota_id = :rotaId AND m.ativa = 1
        ORDER BY m.numero ASC
    """)
    fun buscarMesasPorRota(rotaId: Long): Flow<List<Mesa>>

    /**
     * ✅ NOVA FUNÇÃO: Obtém todas as mesas (disponíveis e em uso)
     */
    @Query("SELECT * FROM mesas ORDER BY numero ASC")
    fun obterTodasMesas(): Flow<List<Mesa>>

    /**
     * Retorna a contagem de mesas ativas por lista de clientes (consulta em lote)
     */
    @Query("""
        SELECT cliente_id AS clienteId, COUNT(*) AS total
        FROM mesas
        WHERE ativa = 1 AND cliente_id IN (:clienteIds)
        GROUP BY cliente_id
    """)
    suspend fun contarMesasAtivasPorClientes(clienteIds: List<Long>): List<MesaCountCliente>

    /**
     * Conta novas mesas instaladas em uma rota dentro de um período (data_instalacao no intervalo)
     */
    @Query(
        """
        SELECT COUNT(*) FROM mesas m
        INNER JOIN clientes c ON m.cliente_id = c.id
        WHERE c.rota_id = :rotaId
          AND m.ativa = 1
          AND m.data_instalacao BETWEEN :dataInicio AND :dataFim
        """
    )
    suspend fun contarNovasMesasInstaladas(rotaId: Long, dataInicio: Date, dataFim: Date): Int
} 

data class MesaCountCliente(
    val clienteId: Long,
    val total: Int
)