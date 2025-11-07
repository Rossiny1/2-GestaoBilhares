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

    // ✅ FASE 3: @Transaction garante que todas as inserções sejam atômicas
    @Transaction
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

    /**
     * ✅ NOVO: Busca um acerto mesa específico por acerto e mesa
     * @param acertoId ID do acerto
     * @param mesaId ID da mesa
     * @return AcertoMesa específico ou null se não encontrado
     */
    @Query("SELECT * FROM acerto_mesas WHERE acerto_id = :acertoId AND mesa_id = :mesaId LIMIT 1")
    suspend fun buscarAcertoMesaPorAcertoEMesa(acertoId: Long, mesaId: Long): AcertoMesa?

    /**
     * ✅ NOVO: Busca os últimos acertos de uma mesa para calcular média
     * @param mesaId ID da mesa
     * @param limite Máximo de acertos a buscar (padrão 5)
     * @return Lista dos últimos acertos da mesa
     */
    @Query("SELECT * FROM acerto_mesas WHERE mesa_id = :mesaId AND fichas_jogadas > 0 ORDER BY data_criacao DESC LIMIT :limite")
    suspend fun buscarUltimosAcertosMesa(mesaId: Long, limite: Int = 5): List<AcertoMesa>

    @Update
    suspend fun atualizar(acertoMesa: AcertoMesa)

    @Delete
    suspend fun deletar(acertoMesa: AcertoMesa)

    @Query("DELETE FROM acerto_mesas WHERE acerto_id = :acertoId")
    suspend fun deletarPorAcerto(acertoId: Long)
} 