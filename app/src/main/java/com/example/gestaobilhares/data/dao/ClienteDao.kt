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

    /**
     * ✅ FASE 2A: Query otimizada com débito atual calculado
     * Usa subquery com MAX(data_acerto) em vez de ROW_NUMBER() para compatibilidade com Room
     */
    @Query("""
        SELECT c.*, 
               COALESCE(
                   (SELECT a.debito_atual 
                    FROM acertos a 
                    WHERE a.cliente_id = c.id 
                    AND a.data_acerto = (
                        SELECT MAX(a2.data_acerto) 
                        FROM acertos a2 
                        WHERE a2.cliente_id = c.id
                    )
                   ), 0.0
               ) as debito_atual_calculado
        FROM clientes c 
        WHERE c.rota_id = :rotaId 
        ORDER BY c.nome ASC
    """)
    fun obterClientesPorRotaComDebitoAtual(rotaId: Long): Flow<List<Cliente>>

    @Insert
    suspend fun inserir(cliente: Cliente): Long

    @Update
    suspend fun atualizar(cliente: Cliente)

    @Delete
    suspend fun deletar(cliente: Cliente)

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun obterPorId(id: Long): Cliente?

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
               COALESCE(ultimo_acerto.debito_atual, 0.0) as debito_atual_calculado
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

} 
