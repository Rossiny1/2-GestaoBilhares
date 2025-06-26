package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Rota
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operações com a entidade Rota.
 * Define todas as operações de banco de dados relacionadas às rotas.
 */
@Dao
interface RotaDao {
    
    /**
     * Obtém todas as rotas ativas ordenadas por nome.
     * Retorna um Flow para observar mudanças em tempo real.
     */
    @Query("SELECT * FROM rotas WHERE ativa = 1 ORDER BY nome ASC")
    fun getAllRotasAtivas(): Flow<List<Rota>>
    
    /**
     * Obtém todas as rotas (ativas e inativas) ordenadas por nome.
     */
    @Query("SELECT * FROM rotas ORDER BY nome ASC")
    fun getAllRotas(): Flow<List<Rota>>
    
    /**
     * Obtém uma rota específica por ID.
     */
    @Query("SELECT * FROM rotas WHERE id = :rotaId")
    suspend fun getRotaById(rotaId: Long): Rota?
    
    /**
     * Obtém uma rota por nome (útil para validação).
     */
    @Query("SELECT * FROM rotas WHERE nome = :nome LIMIT 1")
    suspend fun getRotaByNome(nome: String): Rota?
    
    /**
     * Insere uma nova rota no banco de dados.
     * @return O ID da rota inserida
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRota(rota: Rota): Long
    
    /**
     * Insere múltiplas rotas de uma vez.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRotas(rotas: List<Rota>): List<Long>
    
    /**
     * Atualiza uma rota existente.
     */
    @Update
    suspend fun updateRota(rota: Rota)
    
    /**
     * Atualiza múltiplas rotas.
     */
    @Update
    suspend fun updateRotas(rotas: List<Rota>)
    
    /**
     * Deleta uma rota específica.
     */
    @Delete
    suspend fun deleteRota(rota: Rota)
    
    /**
     * Desativa uma rota (soft delete) ao invés de deletá-la.
     */
    @Query("UPDATE rotas SET ativa = 0, dataAtualizacao = :timestamp WHERE id = :rotaId")
    suspend fun desativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Ativa uma rota novamente.
     */
    @Query("UPDATE rotas SET ativa = 1, dataAtualizacao = :timestamp WHERE id = :rotaId")
    suspend fun ativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Verifica se existe uma rota com o nome especificado (para validação).
     */
    @Query("SELECT COUNT(*) FROM rotas WHERE nome = :nome AND id != :excludeId")
    suspend fun existeRotaComNome(nome: String, excludeId: Long = 0): Int
    
    /**
     * Conta o total de rotas ativas.
     */
    @Query("SELECT COUNT(*) FROM rotas WHERE ativa = 1")
    suspend fun contarRotasAtivas(): Int
} 
