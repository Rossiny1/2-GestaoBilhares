package com.example.gestaobilhares.data.dao

import androidx.room.*
import androidx.room.ColumnInfo
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO para operações com CicloAcertoEntity.
 * Gerencia histórico de ciclos de acerto finalizados.
 * 
 * ✅ FASE 8A: NOVO DAO PARA HISTÓRICO DE CICLOS
 */
@Dao
interface CicloAcertoDao {
    
    /**
     * Insere um novo ciclo de acerto.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(cicloAcerto: CicloAcertoEntity): Long
    
    /**
     * Atualiza um ciclo de acerto existente.
     */
    @Update
    suspend fun atualizar(cicloAcerto: CicloAcertoEntity)
    
    /**
     * Deleta um ciclo de acerto.
     */
    @Delete
    suspend fun deletar(cicloAcerto: CicloAcertoEntity)
    
    /**
     * Busca um ciclo por ID.
     */
    @Query("SELECT * FROM ciclos_acerto WHERE id = :id")
    suspend fun buscarPorId(id: Long): CicloAcertoEntity?
    
    /**
     * Lista todos os ciclos de uma rota específica.
     */
    @Query("SELECT * FROM ciclos_acerto WHERE rota_id = :rotaId ORDER BY ano DESC, numero_ciclo DESC")
    fun listarPorRota(rotaId: Long): Flow<List<CicloAcertoEntity>>
    
    /**
     * Lista todos os ciclos disponíveis.
     */
    @Query("SELECT * FROM ciclos_acerto ORDER BY ano DESC, numero_ciclo DESC")
    fun listarTodos(): Flow<List<CicloAcertoEntity>>
    
    /**
     * Lista todos os ciclos de um ano específico.
     */
    @Query("SELECT * FROM ciclos_acerto WHERE ano = :ano ORDER BY rota_id, numero_ciclo DESC")
    fun listarPorAno(ano: Int): Flow<List<CicloAcertoEntity>>
    
    /**
     * Lista todos os ciclos finalizados.
     */
    @Query("SELECT * FROM ciclos_acerto WHERE status = 'FINALIZADO' ORDER BY ano DESC, numero_ciclo DESC")
    fun listarFinalizados(): Flow<List<CicloAcertoEntity>>
    
    /**
     * Busca o último ciclo de uma rota.
     */
    @Query("SELECT * FROM ciclos_acerto WHERE rota_id = :rotaId ORDER BY ano DESC, numero_ciclo DESC LIMIT 1")
    suspend fun buscarUltimoCicloPorRota(rotaId: Long): CicloAcertoEntity?
    
    /**
     * Busca o próximo número de ciclo para uma rota e ano.
     */
    @Query("SELECT COALESCE(MAX(numero_ciclo), 0) + 1 FROM ciclos_acerto WHERE rota_id = :rotaId AND ano = :ano")
    suspend fun buscarProximoNumeroCiclo(rotaId: Long, ano: Int): Int
    
    /**
     * Verifica se existe um ciclo em andamento para uma rota.
     */
    @Query("SELECT COUNT(*) FROM ciclos_acerto WHERE rota_id = :rotaId AND status = 'EM_ANDAMENTO'")
    suspend fun existeCicloEmAndamento(rotaId: Long): Int
    
    /**
     * Busca o ciclo em andamento de uma rota.
     * ✅ CORREÇÃO: Adicionado ORDER BY para garantir consistência se houver múltiplos (pegar o mais recente)
     */
    @Query("SELECT * FROM ciclos_acerto WHERE rota_id = :rotaId AND status = 'EM_ANDAMENTO' ORDER BY ano DESC, numero_ciclo DESC LIMIT 1")
    suspend fun buscarCicloEmAndamento(rotaId: Long): CicloAcertoEntity?
    
    /**
     * Busca o ciclo em andamento de uma rota como Flow.
     * ✅ CORREÇÃO: Adicionado ORDER BY
     */
    @Query("SELECT * FROM ciclos_acerto WHERE rota_id = :rotaId AND status = 'EM_ANDAMENTO' ORDER BY ano DESC, numero_ciclo DESC LIMIT 1")
    fun observarCicloEmAndamento(rotaId: Long): Flow<CicloAcertoEntity?>
    
    /**
     * Lista ciclos por período (para relatórios).
     */
    @Query("SELECT * FROM ciclos_acerto WHERE data_inicio >= :dataInicio AND data_fim <= :dataFim ORDER BY ano DESC, numero_ciclo DESC")
    fun listarPorPeriodo(dataInicio: Long, dataFim: Long): Flow<List<CicloAcertoEntity>>
    
    /**
     * Calcula estatísticas por rota e ano.
     */
    @Query("""
        SELECT 
            rota_id,
            ano,
            COUNT(*) as total_ciclos,
            SUM(valor_total_acertado) as valor_total_acertado,
            SUM(valor_total_despesas) as valor_total_despesas,
            SUM(lucro_liquido) as lucro_total
        FROM ciclos_acerto 
        WHERE rota_id = :rotaId AND ano = :ano AND status = 'FINALIZADO'
        GROUP BY rota_id, ano
    """)
    suspend fun calcularEstatisticasPorRotaAno(rotaId: Long, ano: Int): EstatisticasCiclo?
    
    /**
     * Lista os anos disponíveis para relatórios.
     */
    @Query("SELECT DISTINCT ano FROM ciclos_acerto ORDER BY ano DESC")
    fun listarAnosDisponiveis(): Flow<List<Int>>
    
    /**
     * Conta total de ciclos por status.
     */
    @Query("SELECT status, COUNT(*) as total FROM ciclos_acerto GROUP BY status")
    suspend fun contarPorStatus(): List<StatusCount>
    
    // ==================== QUERIES PARA METAS ====================
    
    /**
     * Busca o ciclo atual de uma rota (em andamento ou último finalizado)
     */
    @Query("""
        SELECT * FROM ciclos_acerto 
        WHERE rota_id = :rotaId 
        ORDER BY 
            ano DESC, 
            numero_ciclo DESC 
        LIMIT 1
    """)
    suspend fun buscarCicloAtualPorRota(rotaId: Long): CicloAcertoEntity?
    
    /**
     * Busca o último ciclo finalizado de uma rota
     */
    @Query("""
        SELECT * FROM ciclos_acerto 
        WHERE rota_id = :rotaId AND status = 'FINALIZADO'
        ORDER BY ano DESC, numero_ciclo DESC 
        LIMIT 1
    """)
    suspend fun buscarUltimoCicloFinalizadoPorRota(rotaId: Long): CicloAcertoEntity?
    
    /**
     * Busca ciclos por rota e ano
     */
    @Query("""
        SELECT * FROM ciclos_acerto 
        WHERE rota_id = :rotaId AND ano = :ano
        ORDER BY numero_ciclo DESC
    """)
    suspend fun buscarCiclosPorRotaEAno(rotaId: Long, ano: Int): List<CicloAcertoEntity>
    
    /**
     * Busca ciclos futuros (planejados) para uma rota
     */
    @Query("""
        SELECT * FROM ciclos_acerto 
        WHERE rota_id = :rotaId AND status = 'PLANEJADO'
        ORDER BY ano ASC, numero_ciclo ASC
    """)
    suspend fun buscarCiclosFuturosPorRota(rotaId: Long): List<CicloAcertoEntity>
    
    /**
     * Busca todos os ciclos de uma rota
     */
    @Query("""
        SELECT * FROM ciclos_acerto 
        WHERE rota_id = :rotaId
        ORDER BY ano DESC, numero_ciclo DESC
    """)
    suspend fun buscarCiclosPorRota(rotaId: Long): List<CicloAcertoEntity>
}

/**
 * Data class para estatísticas de ciclo por rota e ano.
 */
data class EstatisticasCiclo(
    @ColumnInfo(name = "rota_id")
    val rotaId: Long,
    val ano: Int,
    @ColumnInfo(name = "total_ciclos")
    val totalCiclos: Int,
    @ColumnInfo(name = "valor_total_acertado")
    val valorTotalAcertado: Double,
    @ColumnInfo(name = "valor_total_despesas")
    val valorTotalDespesas: Double,
    @ColumnInfo(name = "lucro_total")
    val lucroTotal: Double
)

/**
 * Data class para contagem por status.
 */
data class StatusCount(
    val status: StatusCicloAcerto,
    val total: Int
) 