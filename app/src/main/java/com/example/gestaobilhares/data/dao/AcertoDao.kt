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

    @Query("SELECT * FROM acertos WHERE id IN (SELECT acerto_id FROM acerto_mesas WHERE mesa_id = :mesaId) ORDER BY data_acerto DESC LIMIT 1")
    suspend fun buscarUltimoAcertoPorMesa(mesaId: Long): Acerto?

    @Query("SELECT observacoes FROM acertos WHERE cliente_id = :clienteId ORDER BY data_acerto DESC LIMIT 1")
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long): String?

    @Update
    suspend fun atualizar(acerto: Acerto)

    @Delete
    suspend fun deletar(acerto: Acerto)

    // =============================
    // ✅ FASE 8B: NOVAS QUERIES POR ROTA E CICLO
    // =============================

    /**
     * Busca todos os acertos de uma rota e ciclo específico.
     */
    @Query("SELECT * FROM acertos WHERE rota_id = :rotaId AND ciclo_acerto = :cicloAcerto ORDER BY data_acerto DESC")
    fun buscarPorRotaECiclo(rotaId: Long, cicloAcerto: Int): Flow<List<Acerto>>

    /**
     * Busca todos os acertos de um ciclo específico (todas as rotas).
     */
    @Query("SELECT * FROM acertos WHERE ciclo_acerto = :cicloAcerto ORDER BY data_acerto DESC")
    fun buscarPorCiclo(cicloAcerto: Int): Flow<List<Acerto>>

    /**
     * Busca todos os acertos de uma rota, agrupados por ciclo.
     */
    @Query("SELECT * FROM acertos WHERE rota_id = :rotaId ORDER BY ciclo_acerto DESC, data_acerto DESC")
    fun buscarPorRotaAgrupadoPorCiclo(rotaId: Long): Flow<List<Acerto>>

    /**
     * Busca todos os acertos de um cliente em um ciclo específico.
     */
    @Query("SELECT * FROM acertos WHERE cliente_id = :clienteId AND ciclo_acerto = :cicloAcerto ORDER BY data_acerto DESC")
    fun buscarPorClienteECiclo(clienteId: Long, cicloAcerto: Int): Flow<List<Acerto>>
} 