package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.entities.ContratoMesa
import kotlinx.coroutines.flow.Flow

@Dao
interface ContratoLocacaoDao {
    
    @Query("SELECT * FROM contratos_locacao WHERE clienteId = :clienteId ORDER BY dataCriacao DESC")
    fun buscarContratosPorCliente(clienteId: Long): Flow<List<ContratoLocacao>>
    
    @Query("SELECT * FROM contratos_locacao WHERE numeroContrato = :numeroContrato")
    suspend fun buscarContratoPorNumero(numeroContrato: String): ContratoLocacao?
    
    @Query("SELECT * FROM contratos_locacao WHERE id = :contratoId")
    suspend fun buscarContratoPorId(contratoId: Long): ContratoLocacao?
    
    @Query("SELECT * FROM contratos_locacao WHERE status = 'ATIVO' ORDER BY dataCriacao DESC")
    fun buscarContratosAtivos(): Flow<List<ContratoLocacao>>
    
    @Query("SELECT * FROM contratos_locacao ORDER BY dataCriacao DESC")
    fun buscarTodosContratos(): Flow<List<ContratoLocacao>>
    
    @Query("SELECT COUNT(*) FROM contratos_locacao WHERE strftime('%Y', dataCriacao/1000, 'unixepoch') = :ano")
    suspend fun contarContratosPorAno(ano: String): Int
    
    @Query("SELECT COUNT(*) FROM contratos_locacao")
    suspend fun contarContratosGerados(): Int
    
    @Query("SELECT COUNT(*) FROM contratos_locacao WHERE assinaturaLocatario IS NOT NULL")
    suspend fun contarContratosAssinados(): Int
    
    @Query("SELECT * FROM contratos_locacao WHERE assinaturaLocatario IS NOT NULL ORDER BY dataCriacao DESC")
    suspend fun obterContratosAssinados(): List<ContratoLocacao>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirContrato(contrato: ContratoLocacao): Long
    
    @Update
    suspend fun atualizarContrato(contrato: ContratoLocacao)
    
    // ✅ NOVO: Encerrar contrato via UPDATE direto (diagnóstico robusto)
    @Query("UPDATE contratos_locacao SET status = :status, dataEncerramento = :dataEncerramento, dataAtualizacao = :dataAtualizacao WHERE id = :contratoId")
    suspend fun encerrarContrato(contratoId: Long, status: String, dataEncerramento: java.util.Date?, dataAtualizacao: java.util.Date?)
    
    @Delete
    suspend fun excluirContrato(contrato: ContratoLocacao)
    
    // DAO para ContratoMesa
    @Query("SELECT * FROM contrato_mesas WHERE contratoId = :contratoId")
    suspend fun buscarMesasPorContrato(contratoId: Long): List<ContratoMesa>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirContratoMesa(contratoMesa: ContratoMesa): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirContratoMesas(contratoMesas: List<ContratoMesa>): List<Long>
    
    @Delete
    suspend fun excluirContratoMesa(contratoMesa: ContratoMesa)
    
    @Query("DELETE FROM contrato_mesas WHERE contratoId = :contratoId")
    suspend fun excluirMesasPorContrato(contratoId: Long)
}
