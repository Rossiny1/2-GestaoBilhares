package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com logs de auditoria de assinaturas
 */
@Dao
interface LogAuditoriaAssinaturaDao {
    
    @Query("SELECT * FROM logs_auditoria_assinatura ORDER BY dataOperacao DESC")
    suspend fun obterTodosLogs(): List<LogAuditoriaAssinatura>
    
    @Query("SELECT * FROM logs_auditoria_assinatura ORDER BY dataOperacao DESC")
    fun obterTodosLogsFlow(): Flow<List<LogAuditoriaAssinatura>>
    
    @Query("SELECT * FROM logs_auditoria_assinatura WHERE idAssinatura = :idAssinatura ORDER BY dataOperacao DESC")
    suspend fun obterLogsPorAssinatura(idAssinatura: Long): List<LogAuditoriaAssinatura>
    
    @Query("SELECT * FROM logs_auditoria_assinatura WHERE idContrato = :idContrato ORDER BY dataOperacao DESC")
    suspend fun obterLogsPorContrato(idContrato: Long): List<LogAuditoriaAssinatura>
    
    @Query("SELECT * FROM logs_auditoria_assinatura WHERE tipoOperacao = :tipoOperacao ORDER BY dataOperacao DESC")
    suspend fun obterLogsPorTipoOperacao(tipoOperacao: String): List<LogAuditoriaAssinatura>
    
    @Query("SELECT * FROM logs_auditoria_assinatura WHERE dataOperacao BETWEEN :dataInicio AND :dataFim ORDER BY dataOperacao DESC")
    suspend fun obterLogsPorPeriodo(dataInicio: Long, dataFim: Long): List<LogAuditoriaAssinatura>
    
    @Query("SELECT * FROM logs_auditoria_assinatura WHERE usuarioExecutou = :usuario ORDER BY dataOperacao DESC")
    suspend fun obterLogsPorUsuario(usuario: String): List<LogAuditoriaAssinatura>
    
    @Query("SELECT * FROM logs_auditoria_assinatura WHERE sucesso = 0 ORDER BY dataOperacao DESC")
    suspend fun obterLogsComErro(): List<LogAuditoriaAssinatura>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirLog(log: LogAuditoriaAssinatura): Long
    
    @Query("SELECT COUNT(*) FROM logs_auditoria_assinatura WHERE dataOperacao >= :dataInicio")
    suspend fun contarLogsDesde(dataInicio: Long): Int
    
    @Query("SELECT COUNT(*) FROM logs_auditoria_assinatura WHERE idAssinatura = :idAssinatura")
    suspend fun contarUsosAssinatura(idAssinatura: Long): Int
    
    @Query("SELECT * FROM logs_auditoria_assinatura WHERE validadoJuridicamente = 0 ORDER BY dataOperacao DESC")
    suspend fun obterLogsNaoValidados(): List<LogAuditoriaAssinatura>
    
    @Query("UPDATE logs_auditoria_assinatura SET validadoJuridicamente = 1, dataValidacao = :dataValidacao, validadoPor = :validadoPor WHERE id = :id")
    suspend fun validarLog(id: Long, dataValidacao: Long, validadoPor: String)
}
