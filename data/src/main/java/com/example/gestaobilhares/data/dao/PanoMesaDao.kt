package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.PanoMesa
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de PanoMesa no banco de dados
 */
@Dao
interface PanoMesaDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(panoMesa: PanoMesa): Long
    
    @Update
    suspend fun atualizar(panoMesa: PanoMesa)
    
    @Delete
    suspend fun deletar(panoMesa: PanoMesa)

    @Query("SELECT * FROM pano_mesas WHERE id = :id")
    suspend fun buscarPorId(id: Long): PanoMesa?
    
    @Query("SELECT * FROM pano_mesas WHERE mesa_id = :mesaId ORDER BY data_troca DESC")
    fun buscarPorMesa(mesaId: Long): Flow<List<PanoMesa>>
    
    @Query("SELECT * FROM pano_mesas WHERE mesa_id = :mesaId AND ativo = 1 LIMIT 1")
    suspend fun buscarPanoAtualMesa(mesaId: Long): PanoMesa?
    
    @Query("SELECT * FROM pano_mesas WHERE pano_id = :panoId")
    fun buscarPorPano(panoId: Long): Flow<List<PanoMesa>>
    
    @Query("SELECT * FROM pano_mesas WHERE mesa_id = :mesaId ORDER BY data_troca DESC LIMIT 1")
    suspend fun buscarUltimaTrocaMesa(mesaId: Long): PanoMesa?
    
    /**
     * Desativa o pano atual da mesa e ativa o novo
     */
    @Query("""
        UPDATE pano_mesas 
        SET ativo = 0 
        WHERE mesa_id = :mesaId AND ativo = 1
    """)
    suspend fun desativarPanoAtualMesa(mesaId: Long)
    
    /**
     * Ativa um pano específico para uma mesa
     */
    @Query("""
        UPDATE pano_mesas 
        SET ativo = 1 
        WHERE mesa_id = :mesaId AND pano_id = :panoId
    """)
    suspend fun ativarPanoMesa(mesaId: Long, panoId: Long)
    
    /**
     * Busca histórico de trocas de pano de uma mesa
     */
    @Query("""
        SELECT * FROM pano_mesas 
        WHERE mesa_id = :mesaId 
        ORDER BY data_troca DESC
    """)
    fun buscarHistoricoTrocasMesa(mesaId: Long): Flow<List<PanoMesa>>
    
    /**
     * Conta quantas vezes um pano foi usado
     */
    @Query("SELECT COUNT(*) FROM pano_mesas WHERE pano_id = :panoId")
    suspend fun contarUsoPano(panoId: Long): Int
}
