package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.entities.Cliente
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * Repository especializado para operações relacionadas a clientes.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class ClienteRepository(
    private val clienteDao: ClienteDao
) {
    
    fun obterTodos(): Flow<List<Cliente>> = clienteDao.obterTodos()
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> = clienteDao.obterClientesPorRota(rotaId)
    suspend fun obterPorId(id: Long) = clienteDao.obterPorId(id)
    
    suspend fun inserir(cliente: Cliente): Long {
        logDbInsertStart("CLIENTE", "Nome=${cliente.nome}, RotaID=${cliente.rotaId}")
        return try {
            // ✅ UPSERT: Tentar inserir (IGNORE). Se retornar -1, fazer update.
            // Isso previne que a atualização do cliente (REPLACE) dispare um DELETE CASCADE
            // que apagaria todos os acertos vinculados a este cliente.
            var id = clienteDao.inserir(cliente)
            if (id == -1L) {
                Log.d("ClienteRepository", "🔄 Cliente ${cliente.id} já existe, atualizando para evitar cascade delete...")
                clienteDao.atualizar(cliente)
                id = cliente.id
            }
            logDbInsertSuccess("CLIENTE", "Nome=${cliente.nome}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("CLIENTE", "Nome=${cliente.nome}", e)
            throw e
        }
    }
    
    suspend fun atualizar(cliente: Cliente): Int = clienteDao.atualizar(cliente)
    suspend fun deletar(cliente: Cliente) = clienteDao.deletar(cliente)
    suspend fun obterDebitoAtual(clienteId: Long) = clienteDao.obterDebitoAtual(clienteId)
    suspend fun atualizarDebitoAtual(clienteId: Long, novoDebito: Double) = 
        clienteDao.atualizarDebitoAtual(clienteId, novoDebito)
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long) = 
        clienteDao.calcularDebitoAtualEmTempoReal(clienteId)
    suspend fun obterClienteComDebitoAtual(clienteId: Long) = 
        clienteDao.obterClienteComDebitoAtual(clienteId)
    
    suspend fun buscarRotaIdPorCliente(clienteId: Long): Long? {
        return try {
            val cliente = obterPorId(clienteId)
            cliente?.rotaId
        } catch (e: Exception) {
            Log.e("ClienteRepository", "Erro ao buscar rota ID por cliente: ${e.message}")
            null
        }
    }
    
    fun obterClientesPorRotaComDebitoAtual(rotaId: Long): Flow<List<Cliente>> = 
        clienteDao.obterClientesPorRota(rotaId)
    
    private fun logDbInsertStart(entity: String, details: String) {
        val stackTrace = Thread.currentThread().stackTrace
        Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
        Log.w("🔍 DB_POPULATION", "🚨 INSERINDO $entity: $details")
        Log.w("🔍 DB_POPULATION", "📍 Chamado por:")
        stackTrace.take(10).forEachIndexed { index, element ->
            Log.w("🔍 DB_POPULATION", "   [$index] $element")
        }
        Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
    }
    
    private fun logDbInsertSuccess(entity: String, details: String) {
        Log.w("🔍 DB_POPULATION", "✅ $entity inserido com sucesso: $details")
    }
    
    private fun logDbInsertError(entity: String, details: String, e: Exception) {
        Log.e("🔍 DB_POPULATION", "❌ Erro ao inserir $entity: $details", e)
    }
}

