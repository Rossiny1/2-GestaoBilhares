package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.AditivoContrato
import com.example.gestaobilhares.data.entities.AditivoMesa
import kotlinx.coroutines.flow.Flow

@Dao
interface AditivoContratoDao {
    
    @Query("SELECT * FROM aditivos_contrato WHERE contratoId = :contratoId ORDER BY dataCriacao DESC")
    fun buscarAditivosPorContrato(contratoId: Long): Flow<List<AditivoContrato>>
    
    @Query("SELECT * FROM aditivos_contrato WHERE numeroAditivo = :numeroAditivo")
    suspend fun buscarAditivoPorNumero(numeroAditivo: String): AditivoContrato?
    
    @Query("SELECT * FROM aditivos_contrato WHERE id = :aditivoId")
    suspend fun buscarAditivoPorId(aditivoId: Long): AditivoContrato?
    
    @Query("SELECT * FROM aditivos_contrato ORDER BY dataCriacao DESC")
    fun buscarTodosAditivos(): Flow<List<AditivoContrato>>
    
    @Query("SELECT COUNT(*) FROM aditivos_contrato WHERE strftime('%Y', dataCriacao/1000, 'unixepoch') = :ano")
    suspend fun contarAditivosPorAno(ano: String): Int
    
    @Query("SELECT COUNT(*) FROM aditivos_contrato")
    suspend fun contarAditivosGerados(): Int
    
    @Query("SELECT COUNT(*) FROM aditivos_contrato WHERE assinaturaLocatario IS NOT NULL")
    suspend fun contarAditivosAssinados(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirAditivo(aditivo: AditivoContrato): Long
    
    @Update
    suspend fun atualizarAditivo(aditivo: AditivoContrato)
    
    @Delete
    suspend fun excluirAditivo(aditivo: AditivoContrato)
    
    // DAO para AditivoMesa
    @Query("SELECT * FROM aditivo_mesas WHERE aditivoId = :aditivoId")
    suspend fun buscarMesasPorAditivo(aditivoId: Long): List<AditivoMesa>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirAditivoMesas(aditivoMesas: List<AditivoMesa>): List<Long>
    
    @Delete
    suspend fun excluirAditivoMesa(aditivoMesa: AditivoMesa)
    
    @Query("DELETE FROM aditivo_mesas WHERE aditivoId = :aditivoId")
    suspend fun excluirTodasMesasDoAditivo(aditivoId: Long)
}
