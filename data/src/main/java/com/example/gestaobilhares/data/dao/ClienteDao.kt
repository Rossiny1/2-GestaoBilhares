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
     * ✅ NOVO: Busca clientes ATIVOS (com mesa OU com débito)
     * Cliente é considerado ativo se tem mesa locada OU tem débito atual > 0
     */
    @Query("""
        SELECT DISTINCT c.* 
        FROM clientes c
        LEFT JOIN mesas m ON c.id = m.cliente_id AND m.ativa = 1
        LEFT JOIN acertos a ON c.id = a.cliente_id AND a.debito_atual > 0
        WHERE c.rota_id = :rotaId
        AND (m.id IS NOT NULL OR a.id IS NOT NULL)
        ORDER BY c.nome ASC
    """)
    fun buscarClientesAtivos(rotaId: Long): Flow<List<Cliente>>

    /**
     * ✅ NOVO: Busca clientes INATIVOS (sem mesa E sem débito)
     * Cliente é considerado inativo se não tem mesa locada E não tem débito atual
     */
    @Query("""
        SELECT c.* 
        FROM clientes c
        WHERE c.rota_id = :rotaId
        AND c.id NOT IN (SELECT cliente_id FROM mesas WHERE ativa = 1)
        AND c.id NOT IN (SELECT cliente_id FROM acertos WHERE debito_atual > 0)
        ORDER BY c.nome ASC
    """)
    fun buscarClientesInativos(rotaId: Long): Flow<List<Cliente>>

    /**
     * ✅ NOVO: Migra contratos de um ID de cliente para outro.
     */
    @Query("UPDATE contratos_locacao SET clienteId = :idNovo WHERE clienteId = :idAntigo")
    suspend fun migrarContratos(idAntigo: Long, idNovo: Long): Int
} 
