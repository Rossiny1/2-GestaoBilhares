// Script para limpar o banco de dados - REMOVER CLIENTES ESPECÍFICOS
// Execute este script no Android Studio ou compile como parte do app

import android.content.Context
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.AcertoMesa
import kotlinx.coroutines.runBlocking

/**
 * Script para limpar o banco de dados removendo clientes específicos
 */
class LimpezaBancoDados(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val clienteDao = database.clienteDao()
    private val acertoDao = database.acertoDao()
    private val mesaDao = database.mesaDao()
    private val acertoMesaDao = database.acertoMesaDao()
    
    /**
     * Remove clientes específicos e todas suas dependências
     */
    suspend fun limparClientesEspecificos() {
        try {
            println("🧹 INICIANDO LIMPEZA DO BANCO DE DADOS...")
            
            // 1. Buscar clientes por nome (usando query direta)
            val clientesParaRemover = listOf("João de Barro", "Guilherme Fiel")
            val clientesEncontrados = mutableListOf<Cliente>()
            
            // Buscar todos os clientes e filtrar por nome
            val todosClientes = clienteDao.obterTodos().first()
            for (nomeCliente in clientesParaRemover) {
                val cliente = todosClientes.find { it.nome.equals(nomeCliente, ignoreCase = true) }
                if (cliente != null) {
                    clientesEncontrados.add(cliente)
                    println("✅ Cliente encontrado: ${cliente.nome} (ID: ${cliente.id})")
                } else {
                    println("⚠️ Cliente não encontrado: $nomeCliente")
                }
            }
            
            if (clientesEncontrados.isEmpty()) {
                println("ℹ️ Nenhum cliente para remover encontrado")
                return
            }
            
            // 2. Para cada cliente, remover dependências
            for (cliente in clientesEncontrados) {
                println("🗑️ Removendo cliente: ${cliente.nome}")
                
                // 2.1 Remover acertos do cliente
                val acertosCliente = acertoDao.buscarPorCliente(cliente.id).first()
                println("   📊 Acertos encontrados: ${acertosCliente.size}")
                
                for (acerto in acertosCliente) {
                    // 2.2 Remover mesas do acerto
                    val mesasAcerto = acertoMesaDao.obterMesasPorAcerto(acerto.id)
                    println("   🎯 Mesas do acerto ${acerto.id}: ${mesasAcerto.size}")
                    
                    // 2.3 Remover vinculações acerto-mesa
                    for (acertoMesa in mesasAcerto) {
                        acertoMesaDao.deletarAcertoMesa(acertoMesa)
                        println("     🗑️ AcertoMesa removido: ${acertoMesa.id}")
                    }
                    
                    // 2.4 Remover acerto
                    acertoDao.deletarAcerto(acerto)
                    println("     🗑️ Acerto removido: ${acerto.id}")
                }
                
                // 2.5 Remover mesas do cliente
                val mesasCliente = mesaDao.obterMesasPorCliente(cliente.id)
                println("   🎯 Mesas do cliente: ${mesasCliente.size}")
                
                for (mesa in mesasCliente) {
                    mesaDao.deletarMesa(mesa)
                    println("     🗑️ Mesa removida: ${mesa.id}")
                }
                
                // 2.6 Remover cliente
                clienteDao.deletarCliente(cliente)
                println("     🗑️ Cliente removido: ${cliente.id}")
            }
            
            println("✅ LIMPEZA CONCLUÍDA COM SUCESSO!")
            println("📊 Resumo:")
            println("   - Clientes removidos: ${clientesEncontrados.size}")
            println("   - Total de acertos removidos: ${clientesEncontrados.sumOf { cliente -> acertoDao.buscarPorCliente(cliente.id).first().size }}")
            println("   - Total de mesas removidas: ${clientesEncontrados.sumOf { cliente -> mesaDao.obterMesasPorCliente(cliente.id).size }}")
            
        } catch (e: Exception) {
            println("❌ ERRO durante a limpeza: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Executa a limpeza de forma síncrona
     */
    fun executarLimpeza() {
        runBlocking {
            limparClientesEspecificos()
        }
    }
}

// Exemplo de uso:
// val limpeza = LimpezaBancoDados(context)
// limpeza.executarLimpeza()
