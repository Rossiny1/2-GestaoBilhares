package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Cliente
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de cliente no banco de dados
 */
@Dao
interface ClienteDao {

    @Query("SELECT * FROM clientes WHERE rota_id = :rotaId ORDER BY nome ASC")
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(cliente: Cliente): Long

    @Update
    suspend fun atualizar(cliente: Cliente): Int

    @Delete
    suspend fun deletar(cliente: Cliente)

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun obterPorId(id: Long): Cliente?

    @Query("SELECT * FROM clientes WHERE id = :id")
    fun obterPorIdFlow(id: Long): Flow<Cliente?>

    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun obterTodos(): Flow<List<Cliente>>

    @Query("SELECT debito_atual FROM clientes WHERE id = :clienteId")
    suspend fun obterDebitoAtual(clienteId: Long): Double

    @Query("UPDATE clientes SET debito_atual = :novoDebito WHERE id = :clienteId")
    suspend fun atualizarDebitoAtual(clienteId: Long, novoDebito: Double)

    /**
     * ✅ CORRIGIDO: Calcula o débito atual em tempo real baseado no último acerto
     * Esta query calcula o débito atual diretamente no banco, garantindo consistência
     * CORREÇÃO: Buscar o debito_atual (não debito_anterior) do último acerto
     */
    @Query("""
        SELECT COALESCE(
            (SELECT debito_atual 
             FROM acertos 
             WHERE cliente_id = :clienteId 
             ORDER BY data_acerto DESC 
             LIMIT 1), 
            0.0
        ) as debito_atual_calculado
    """)
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long): Double

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT c.*, 
               COALESCE(ultimo_acerto.debito_atual, 0.0) as debito_atual
        FROM clientes c
        LEFT JOIN (
            SELECT a1.cliente_id, a1.debito_atual
            FROM acertos a1
            INNER JOIN (
                SELECT cliente_id, MAX(data_acerto) as max_data
                FROM acertos
                GROUP BY cliente_id
            ) a2 ON a1.cliente_id = a2.cliente_id AND a1.data_acerto = a2.max_data
        ) ultimo_acerto ON c.id = ultimo_acerto.cliente_id
        WHERE c.rota_id = :rotaId
        ORDER BY c.nome ASC
    """)
    fun obterClientesPorRotaComDebitoCalculado(rotaId: Long): Flow<List<Cliente>>

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT c.*, 
               COALESCE(ultimo_acerto.debito_atual, 0.0) as debito_atual
        FROM clientes c
        LEFT JOIN (
            SELECT cliente_id, debito_atual, data_acerto
            FROM acertos 
            WHERE cliente_id = :clienteId 
            ORDER BY data_acerto DESC 
            LIMIT 1
        ) ultimo_acerto ON c.id = ultimo_acerto.cliente_id
        WHERE c.id = :clienteId
    """)
    suspend fun obterClienteComDebitoAtual(clienteId: Long): Cliente?

    /**
     * ✅ NOVO: Busca um cliente pelo nome e rota para reconciliação durante sync.
     */
    @Query("SELECT * FROM clientes WHERE nome = :nome AND rota_id = :rotaId LIMIT 1")
    suspend fun buscarPorNomeERota(nome: String, rotaId: Long): Cliente?

    /**
     * ✅ NOVO: Migra mesas de um ID de cliente para outro.
     */
    @Query("UPDATE mesas SET cliente_id = :idNovo WHERE cliente_id = :idAntigo")
    suspend fun migrarMesas(idAntigo: Long, idNovo: Long): Int

    /**
     * ✅ NOVO: Migra acertos de um ID de cliente para outro.
     */
    @Query("UPDATE acertos SET cliente_id = :idNovo WHERE cliente_id = :idAntigo")
    suspend fun migrarAcertos(idAntigo: Long, idNovo: Long): Int

    /**
     * ✅ NOVO: Migra contratos de um ID de cliente para outro.
     */
    @Query("UPDATE contratos_locacao SET clienteId = :idNovo WHERE clienteId = :idAntigo")
    suspend fun migrarContratos(idAntigo: Long, idNovo: Long): Int
} 
