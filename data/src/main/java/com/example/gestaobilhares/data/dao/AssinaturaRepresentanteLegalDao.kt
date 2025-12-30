package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com assinaturas do representante legal
 */
@Dao
interface AssinaturaRepresentanteLegalDao {
    
    @Query("SELECT * FROM assinatura_representante_legal WHERE ativo = 1 ORDER BY dataCriacao DESC LIMIT 1")
    suspend fun obterAssinaturaAtiva(): AssinaturaRepresentanteLegal?
    
    @Query("SELECT * FROM assinatura_representante_legal WHERE ativo = 1 ORDER BY dataCriacao DESC LIMIT 1")
    fun obterAssinaturaAtivaFlow(): Flow<AssinaturaRepresentanteLegal?>
    
    @Query("SELECT * FROM assinatura_representante_legal ORDER BY dataCriacao DESC")
    suspend fun obterTodasAssinaturas(): List<AssinaturaRepresentanteLegal>
    
    @Query("SELECT * FROM assinatura_representante_legal ORDER BY dataCriacao DESC")
    fun obterTodasAssinaturasFlow(): Flow<List<AssinaturaRepresentanteLegal>>
    
    @Query("SELECT * FROM assinatura_representante_legal WHERE id = :id")
    suspend fun obterAssinaturaPorId(id: Long): AssinaturaRepresentanteLegal?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirAssinatura(assinatura: AssinaturaRepresentanteLegal): Long
    
    @Update
    suspend fun atualizarAssinatura(assinatura: AssinaturaRepresentanteLegal)
    
    @Query("UPDATE assinatura_representante_legal SET ativo = 0 WHERE id = :id")
    suspend fun desativarAssinatura(id: Long)
    
    @Query("UPDATE assinatura_representante_legal SET totalUsos = totalUsos + 1, ultimoUso = :dataUso WHERE id = :id")
    suspend fun incrementarUso(id: Long, dataUso: Long)
    
    @Query("SELECT COUNT(*) FROM assinatura_representante_legal WHERE ativo = 1")
    suspend fun contarAssinaturasAtivas(): Int
    
    @Query("SELECT * FROM assinatura_representante_legal WHERE validadaJuridicamente = 1")
    suspend fun obterAssinaturasValidadas(): List<AssinaturaRepresentanteLegal>
}
