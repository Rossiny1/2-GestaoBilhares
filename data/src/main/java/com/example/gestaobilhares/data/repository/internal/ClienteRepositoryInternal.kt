package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.SyncQueueDao
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.core.utils.DataEncryption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 2: Repository interno para operações de Cliente
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Este repository é usado internamente pelo AppRepository.
 */
internal class ClienteRepositoryInternal(
    private val clienteDao: ClienteDao,
    private val syncQueueDao: SyncQueueDao
) {
    
    /**
     * Obtém todos os clientes com descriptografia de dados sensíveis
     */
    fun obterTodosClientes(): Flow<List<Cliente>> = clienteDao.obterTodos().map { clientes ->
        clientes.map { decryptCliente(it) ?: it }
    }
    
    /**
     * Obtém clientes por rota com descriptografia de dados sensíveis
     */
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> = 
        clienteDao.obterClientesPorRota(rotaId).map { clientes ->
            clientes.map { decryptCliente(it) ?: it }
        }
    
    /**
     * Obtém clientes por rota com débito atual calculado
     * Usa conflate() para garantir que mudanças sejam processadas imediatamente
     */
    fun obterClientesPorRotaComDebitoAtual(rotaId: Long): Flow<List<Cliente>> = 
        clienteDao.obterClientesPorRotaComDebitoAtual(rotaId)
            .conflate() // ✅ CRÍTICO: Processar mudanças imediatamente, sem buffer
            .map { clientes ->
                clientes.map { decryptCliente(it) ?: it }
            }
    
    /**
     * Obtém cliente por ID com descriptografia
     */
    suspend fun obterClientePorId(id: Long): Cliente? = decryptCliente(clienteDao.obterPorId(id))
    
    /**
     * Insere um novo cliente com criptografia e sincronização
     */
    suspend fun inserirCliente(
        cliente: Cliente,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("CLIENTE", "Nome=${cliente.nome}, RotaID=${cliente.rotaId}")
        return try {
            // ✅ FASE 12.3: Criptografar dados sensíveis antes de salvar
            val clienteEncrypted = encryptCliente(cliente)
            val id = clienteDao.inserir(clienteEncrypted)
            logDbInsertSuccess("CLIENTE", "Nome=${cliente.nome}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${cliente.nome}",
                        "telefone": "${cliente.telefone}",
                        "endereco": "${cliente.endereco}",
                        "rotaId": ${cliente.rotaId},
                        "ativo": ${cliente.ativo},
                        "dataCadastro": "${cliente.dataCadastro}",
                        "valorFicha": ${cliente.valorFicha},
                        "comissaoFicha": ${cliente.comissaoFicha}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Cliente", id, "CREATE", payload, 1)
                logarOperacaoSync("Cliente", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("ClienteRepositoryInternal", "Erro ao adicionar cliente à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CLIENTE", "Nome=${cliente.nome}", e)
            throw e
        }
    }
    
    /**
     * Atualiza um cliente com criptografia e sincronização
     */
    suspend fun atualizarCliente(
        cliente: Cliente,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("CLIENTE", "ID=${cliente.id}, Nome=${cliente.nome}")
        try {
            // ✅ FASE 12.3: Criptografar dados sensíveis antes de salvar
            val clienteEncrypted = encryptCliente(cliente)
            clienteDao.atualizar(clienteEncrypted)
            logDbUpdateSuccess("CLIENTE", "ID=${cliente.id}, Nome=${cliente.nome}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${cliente.id},
                        "nome": "${cliente.nome}",
                        "telefone": "${cliente.telefone}",
                        "endereco": "${cliente.endereco}",
                        "rotaId": ${cliente.rotaId},
                        "ativo": ${cliente.ativo},
                        "dataCadastro": "${cliente.dataCadastro}",
                        "debitoAtual": ${cliente.debitoAtual},
                        "valorFicha": ${cliente.valorFicha},
                        "comissaoFicha": ${cliente.comissaoFicha}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Cliente", cliente.id, "UPDATE", payload, 1)
                logarOperacaoSync("Cliente", cliente.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("ClienteRepositoryInternal", "Erro ao adicionar atualização de cliente à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("CLIENTE", "ID=${cliente.id}", e)
            throw e
        }
    }
    
    /**
     * Deleta um cliente
     */
    suspend fun deletarCliente(cliente: Cliente) = clienteDao.deletar(cliente)
    
    /**
     * Atualiza débito atual do cliente
     * Atualiza também dataUltimaAtualizacao para forçar Room a re-emitir Flow
     */
    suspend fun atualizarDebitoAtual(
        clienteId: Long,
        novoDebito: Double,
        obterClientePorId: suspend (Long) -> Cliente?,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("CLIENTE_DEBITO", "ClienteID=$clienteId, NovoDebito=$novoDebito")
        try {
            // ✅ CORREÇÃO CRÍTICA: Atualizar débito e dataUltimaAtualizacao em uma única operação
            // Isso garante que o Room detecte a mudança e re-emita o Flow imediatamente
            val cliente = obterClientePorId(clienteId)
            if (cliente != null) {
                // Atualizar débito e dataUltimaAtualizacao simultaneamente
                val clienteAtualizado = cliente.copy(
                    debitoAtual = novoDebito,
                    dataUltimaAtualizacao = java.util.Date()
                )
                clienteDao.atualizar(clienteAtualizado)
                Log.d("ClienteRepositoryInternal", "✅ Débito atualizado para cliente $clienteId: $novoDebito - Flow será re-emitido imediatamente")
            } else {
                // Fallback: usar método direto se cliente não for encontrado
                clienteDao.atualizarDebitoAtual(clienteId, novoDebito)
                Log.w("ClienteRepositoryInternal", "⚠️ Cliente $clienteId não encontrado, usando atualização direta")
            }
            
            logDbUpdateSuccess("CLIENTE_DEBITO", "ClienteID=$clienteId, NovoDebito=$novoDebito")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val clienteParaSync = obterClientePorId(clienteId)
                if (clienteParaSync != null) {
                    val payload = """
                        {
                            "id": ${clienteParaSync.id},
                            "nome": "${clienteParaSync.nome}",
                            "telefone": "${clienteParaSync.telefone}",
                            "endereco": "${clienteParaSync.endereco}",
                            "rotaId": ${clienteParaSync.rotaId},
                            "ativo": ${clienteParaSync.ativo},
                            "dataCadastro": "${clienteParaSync.dataCadastro}",
                            "debitoAtual": $novoDebito,
                            "valorFicha": ${clienteParaSync.valorFicha},
                            "comissaoFicha": ${clienteParaSync.comissaoFicha}
                        }
                    """.trimIndent()
                    
                    adicionarOperacaoSync("Cliente", clienteId, "UPDATE", payload, 1)
                    logarOperacaoSync("Cliente", clienteId, "UPDATE", "PENDING", null, payload)
                }
                
            } catch (syncError: Exception) {
                Log.w("ClienteRepositoryInternal", "Erro ao adicionar atualização de débito à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("CLIENTE_DEBITO", "ClienteID=$clienteId", e)
            throw e
        }
    }
    
    /**
     * Calcula débito atual em tempo real
     */
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long) = 
        clienteDao.calcularDebitoAtualEmTempoReal(clienteId)
    
    /**
     * Obtém cliente com débito atual
     */
    suspend fun obterClienteComDebitoAtual(clienteId: Long) = 
        clienteDao.obterClienteComDebitoAtual(clienteId)
    
    /**
     * Busca o ID da rota associada a um cliente
     */
    suspend fun buscarRotaIdPorCliente(clienteId: Long): Long? {
        return try {
            val cliente = obterClientePorId(clienteId)
            cliente?.rotaId
        } catch (e: Exception) {
            Log.e("ClienteRepositoryInternal", "Erro ao buscar rota ID por cliente: ${e.message}", e)
            null
        }
    }
    
    // ==================== MÉTODOS PRIVADOS DE CRIPTOGRAFIA ====================
    
    /**
     * Criptografa dados sensíveis de um Cliente antes de salvar
     */
    private fun encryptCliente(cliente: Cliente): Cliente {
        return cliente.copy(
            cpfCnpj = cliente.cpfCnpj?.let { DataEncryption.encrypt(it) ?: it }
        )
    }
    
    /**
     * Descriptografa dados sensíveis de um Cliente após ler
     */
    private fun decryptCliente(cliente: Cliente?): Cliente? {
        return cliente?.copy(
            cpfCnpj = cliente.cpfCnpj?.let { DataEncryption.decrypt(it) ?: it }
        )
    }
}

