package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.ProcuraçãoRepresentante
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO para operações com procurações de representantes
 */
@Dao
interface ProcuraçãoRepresentanteDao {
    
    @Query("SELECT * FROM procuracoes_representantes WHERE ativa = 1 ORDER BY dataProcuração DESC")
    suspend fun obterProcuraçõesAtivas(): List<ProcuraçãoRepresentante>
    
    @Query("SELECT * FROM procuracoes_representantes WHERE ativa = 1 ORDER BY dataProcuração DESC")
    fun obterProcuraçõesAtivasFlow(): Flow<List<ProcuraçãoRepresentante>>
    
    @Query("SELECT * FROM procuracoes_representantes WHERE representanteOutorgadoUsuario = :usuario AND ativa = 1")
    suspend fun obterProcuraçãoPorUsuario(usuario: String): ProcuraçãoRepresentante?
    
    @Query("SELECT * FROM procuracoes_representantes WHERE representanteOutorgadoUsuario = :usuario AND ativa = 1")
    fun obterProcuraçãoPorUsuarioFlow(usuario: String): Flow<ProcuraçãoRepresentante?>
    
    @Query("SELECT * FROM procuracoes_representantes WHERE representanteOutorgadoCpf = :cpf AND ativa = 1")
    suspend fun obterProcuraçãoPorCpf(cpf: String): ProcuraçãoRepresentante?
    
    @Query("SELECT * FROM procuracoes_representantes ORDER BY dataProcuração DESC")
    suspend fun obterTodasProcurações(): List<ProcuraçãoRepresentante>
    
    @Query("SELECT * FROM procuracoes_representantes ORDER BY dataProcuração DESC")
    fun obterTodasProcuraçõesFlow(): Flow<List<ProcuraçãoRepresentante>>
    
    @Query("SELECT * FROM procuracoes_representantes WHERE id = :id")
    suspend fun obterProcuraçãoPorId(id: Long): ProcuraçãoRepresentante?
    
    @Query("SELECT * FROM procuracoes_representantes WHERE numeroProcuração = :numero")
    suspend fun obterProcuraçãoPorNumero(numero: String): ProcuraçãoRepresentante?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirProcuração(procuração: ProcuraçãoRepresentante): Long
    
    @Update
    suspend fun atualizarProcuração(procuração: ProcuraçãoRepresentante)
    
    @Query("UPDATE procuracoes_representantes SET ativa = 0, dataRevogacao = :dataRevogacao, motivoRevogacao = :motivo WHERE id = :id")
    suspend fun revogarProcuração(id: Long, dataRevogacao: Date, motivo: String)
    
    @Query("SELECT COUNT(*) FROM procuracoes_representantes WHERE ativa = 1")
    suspend fun contarProcuraçõesAtivas(): Int
    
    @Query("SELECT * FROM procuracoes_representantes WHERE validadaJuridicamente = 1 AND ativa = 1")
    suspend fun obterProcuraçõesValidadas(): List<ProcuraçãoRepresentante>
    
    @Query("SELECT * FROM procuracoes_representantes WHERE dataValidade IS NOT NULL AND dataValidade < :dataAtual AND ativa = 1")
    suspend fun obterProcuraçõesVencidas(dataAtual: Date): List<ProcuraçãoRepresentante>
    
    @Query("UPDATE procuracoes_representantes SET validadaJuridicamente = 1, dataValidacaoJuridica = :dataValidacao, validadaPor = :validadoPor WHERE id = :id")
    suspend fun validarProcuração(id: Long, dataValidacao: Date, validadoPor: String)
}
