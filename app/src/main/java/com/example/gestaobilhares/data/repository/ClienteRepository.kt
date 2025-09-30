package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.entities.Cliente
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para operações relacionadas a clientes
 * 
 * Implementa o padrão Repository para abstrair a camada de dados
 * e fornecer uma interface limpa para os ViewModels.
 */
@Singleton
class ClienteRepository @Inject constructor(
    private val clienteDao: ClienteDao
) {

    /**
     * Obtém todos os clientes por rota
     */
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> {
        return clienteDao.obterClientesPorRota(rotaId)
    }

    /**
     * Insere um novo cliente
     */
    suspend fun inserir(cliente: Cliente): Long {
        // ✅ LOG DETALHADO PARA RASTREAR INSERÇÃO DE CLIENTES
        val stackTrace = Thread.currentThread().stackTrace
        android.util.Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
        android.util.Log.w("🔍 DB_POPULATION", "🚨 INSERINDO CLIENTE: ${cliente.nome} (Rota ID: ${cliente.rotaId})")
        android.util.Log.w("🔍 DB_POPULATION", "📍 Chamado por:")
        stackTrace.take(10).forEachIndexed { index, element ->
            android.util.Log.w("🔍 DB_POPULATION", "   [$index] $element")
        }
        android.util.Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
        
        return try {
            android.util.Log.d("ClienteRepository", "Iniciando inserção do cliente: ${cliente.nome}")
            val id = clienteDao.inserir(cliente)
            android.util.Log.w("🔍 DB_POPULATION", "✅ CLIENTE INSERIDO COM SUCESSO: ${cliente.nome} (ID: $id, Rota: ${cliente.rotaId})")
            android.util.Log.d("ClienteRepository", "Cliente inserido com sucesso, ID: $id")
            id
        } catch (e: Exception) {
            android.util.Log.e("🔍 DB_POPULATION", "❌ ERRO AO INSERIR CLIENTE: ${cliente.nome}", e)
            android.util.Log.e("ClienteRepository", "Erro ao inserir cliente: ${e.message}", e)
            throw e
        }
    }

    /**
     * Atualiza um cliente
     */
    suspend fun atualizar(cliente: Cliente) {
        clienteDao.atualizar(cliente)
    }

    /**
     * Deleta um cliente
     */
    suspend fun deletar(cliente: Cliente) {
        clienteDao.deletar(cliente)
    }

    /**
     * Obtém cliente por ID
     */
    suspend fun obterPorId(id: Long): Cliente? {
        return clienteDao.obterPorId(id)
    }

    /**
     * Obtém todos os clientes
     */
    fun obterTodos(): Flow<List<Cliente>> {
        return clienteDao.obterTodos()
    }

    suspend fun atualizarObservacao(clienteId: Long, observacao: String) {
        val cliente = obterPorId(clienteId)
        if (cliente != null) {
            val atualizado = cliente.copy(observacoes = observacao)
            atualizar(atualizado)
        }
    }

    /**
     * Obtém o débito atual do cliente
     */
    suspend fun obterDebitoAtual(clienteId: Long): Double {
        return clienteDao.obterDebitoAtual(clienteId)
    }
    
    /**
     * Atualiza o débito atual do cliente
     */
    suspend fun atualizarDebitoAtual(clienteId: Long, novoDebito: Double) {
        clienteDao.atualizarDebitoAtual(clienteId, novoDebito)
    }
    
    /**
     * ✅ NOVO: Calcula o débito atual em tempo real diretamente do banco
     * Garante consistência total com os dados salvos
     */
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long): Double {
        return try {
            val debitoCalculado = clienteDao.calcularDebitoAtualEmTempoReal(clienteId)
            android.util.Log.d("ClienteRepository", "Débito atual calculado no banco: R$ $debitoCalculado")
            debitoCalculado
        } catch (e: Exception) {
            android.util.Log.e("ClienteRepository", "Erro ao calcular débito atual: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * Obtém cliente com débito atual calculado
     */
    suspend fun obterClienteComDebitoAtual(clienteId: Long): Cliente? {
        return clienteDao.obterClienteComDebitoAtual(clienteId)
    }

    /**
     * Obtém o débito total de todos os clientes de uma rota
     */
    suspend fun obterDebitoTotalPorRota(rotaId: Long): Double {
        return try {
            clienteDao.obterClientesPorRota(rotaId).first().sumOf { it.debitoAtual }
        } catch (e: Exception) {
            android.util.Log.e("ClienteRepository", "Erro ao calcular débito total: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * ✅ NOVO: Busca o ID da rota associada a um cliente
     */
    suspend fun buscarRotaIdPorCliente(clienteId: Long): Long? {
        return try {
            val cliente = obterPorId(clienteId)
            cliente?.rotaId
        } catch (e: Exception) {
            android.util.Log.e("ClienteRepository", "Erro ao buscar rota ID por cliente: ${e.message}", e)
            null
        }
    }
} 
