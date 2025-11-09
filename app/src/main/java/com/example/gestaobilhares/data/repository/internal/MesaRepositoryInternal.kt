package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.entities.Mesa
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 2: Repository interno para operações de Mesa
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Este repository é usado internamente pelo AppRepository.
 */
internal class MesaRepositoryInternal(
    private val mesaDao: MesaDao
) {
    
    /**
     * Obtém mesa por ID
     */
    suspend fun obterMesaPorId(id: Long): Mesa? = mesaDao.obterMesaPorId(id)
    
    /**
     * Obtém mesas por cliente
     */
    fun obterMesasPorCliente(clienteId: Long): Flow<List<Mesa>> = mesaDao.obterMesasPorCliente(clienteId)
    
    /**
     * Obtém mesas disponíveis
     */
    fun obterMesasDisponiveis(): Flow<List<Mesa>> = mesaDao.obterMesasDisponiveis()
    
    /**
     * Busca mesas por rota
     */
    fun buscarMesasPorRota(rotaId: Long): Flow<List<Mesa>> = mesaDao.buscarMesasPorRota(rotaId).also {
        Log.d("MesaRepositoryInternal", "Buscando mesas para rota $rotaId")
    }
    
    /**
     * Obtém mesas por cliente (direto)
     */
    suspend fun obterMesasPorClienteDireto(clienteId: Long): List<Mesa> = 
        mesaDao.obterMesasPorClienteDireto(clienteId)
    
    /**
     * Conta mesas ativas por clientes
     */
    suspend fun contarMesasAtivasPorClientes(clienteIds: List<Long>) =
        mesaDao.contarMesasAtivasPorClientes(clienteIds)
    
    /**
     * Insere uma nova mesa com validação e sincronização
     */
    suspend fun inserirMesa(
        mesa: Mesa,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("MESA", "Numero=${mesa.numero}, ClienteID=${mesa.clienteId}")
        return try {
            // ✅ VALIDAÇÃO: Verificar se já existe mesa com mesmo número
            val mesaExistente = mesaDao.buscarPorNumero(mesa.numero)
            if (mesaExistente != null) {
                Log.w("MesaRepositoryInternal", "⚠️ Mesa com número '${mesa.numero}' já existe (ID: ${mesaExistente.id})")
                throw IllegalArgumentException("Mesa com número '${mesa.numero}' já existe")
            }
            
            val id = mesaDao.inserir(mesa)
            logDbInsertSuccess("MESA", "Numero=${mesa.numero}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "numero": "${mesa.numero}",
                        "clienteId": ${mesa.clienteId},
                        "ativa": ${mesa.ativa},
                        "tipoMesa": "${mesa.tipoMesa}",
                        "tamanho": "${mesa.tamanho}",
                        "estadoConservacao": "${mesa.estadoConservacao}",
                        "valorFixo": ${mesa.valorFixo},
                        "relogioInicial": ${mesa.relogioInicial},
                        "relogioFinal": ${mesa.relogioFinal},
                        "dataInstalacao": "${mesa.dataInstalacao}",
                        "observacoes": "${mesa.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Mesa", id, "CREATE", payload, 1)
                logarOperacaoSync("Mesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("MesaRepositoryInternal", "Erro ao adicionar mesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("MESA", "Numero=${mesa.numero}", e)
            throw e
        }
    }
    
    /**
     * Atualiza uma mesa com sincronização
     */
    suspend fun atualizarMesa(
        mesa: Mesa,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("MESA", "ID=${mesa.id}, Numero=${mesa.numero}")
        try {
            mesaDao.atualizar(mesa)
            logDbUpdateSuccess("MESA", "ID=${mesa.id}, Numero=${mesa.numero}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${mesa.id},
                        "numero": "${mesa.numero}",
                        "clienteId": ${mesa.clienteId},
                        "ativa": ${mesa.ativa},
                        "tipoMesa": "${mesa.tipoMesa}",
                        "tamanho": "${mesa.tamanho}",
                        "estadoConservacao": "${mesa.estadoConservacao}",
                        "valorFixo": ${mesa.valorFixo},
                        "relogioInicial": ${mesa.relogioInicial},
                        "relogioFinal": ${mesa.relogioFinal},
                        "dataInstalacao": "${mesa.dataInstalacao}",
                        "observacoes": "${mesa.observacoes ?: ""}",
                        "panoAtualId": ${mesa.panoAtualId ?: "null"},
                        "dataUltimaTrocaPano": "${mesa.dataUltimaTrocaPano ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Mesa", mesa.id, "UPDATE", payload, 1)
                logarOperacaoSync("Mesa", mesa.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("MesaRepositoryInternal", "Erro ao adicionar atualização de mesa à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("MESA", "ID=${mesa.id}", e)
            throw e
        }
    }
    
    /**
     * Deleta uma mesa
     */
    suspend fun deletarMesa(mesa: Mesa) = mesaDao.deletar(mesa)
    
    /**
     * Vincula mesa a cliente
     */
    suspend fun vincularMesaACliente(mesaId: Long, clienteId: Long) = 
        mesaDao.vincularMesa(mesaId, clienteId)
    
    /**
     * Vincula mesa com valor fixo
     */
    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) = 
        mesaDao.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
    
    /**
     * Desvincula mesa de cliente
     */
    suspend fun desvincularMesaDeCliente(mesaId: Long) = mesaDao.desvincularMesa(mesaId)
    
    /**
     * Retira mesa
     */
    suspend fun retirarMesa(mesaId: Long) = mesaDao.retirarMesa(mesaId)
    
    /**
     * Atualiza relógio da mesa com sincronização
     */
    suspend fun atualizarRelogioMesa(
        mesaId: Long,
        relogioInicial: Int,
        relogioFinal: Int,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("MESA_RELOGIO", "MesaID=$mesaId, RelogioInicial=$relogioInicial, RelogioFinal=$relogioFinal")
        try {
            mesaDao.atualizarRelogioMesa(mesaId, relogioInicial, relogioFinal)
            logDbUpdateSuccess("MESA_RELOGIO", "MesaID=$mesaId, RelogioInicial=$relogioInicial, RelogioFinal=$relogioFinal")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val mesa = mesaDao.obterMesaPorId(mesaId)
                if (mesa != null) {
                    val payload = """
                        {
                            "id": ${mesa.id},
                            "numero": "${mesa.numero}",
                            "clienteId": ${mesa.clienteId},
                            "ativa": ${mesa.ativa},
                            "tipoMesa": "${mesa.tipoMesa}",
                            "tamanho": "${mesa.tamanho}",
                            "estadoConservacao": "${mesa.estadoConservacao}",
                            "valorFixo": ${mesa.valorFixo},
                            "relogioInicial": $relogioInicial,
                            "relogioFinal": $relogioFinal,
                            "dataInstalacao": "${mesa.dataInstalacao}",
                            "observacoes": "${mesa.observacoes ?: ""}",
                            "panoAtualId": ${mesa.panoAtualId ?: "null"},
                            "dataUltimaTrocaPano": "${mesa.dataUltimaTrocaPano ?: ""}"
                        }
                    """.trimIndent()
                    
                    adicionarOperacaoSync("Mesa", mesaId, "UPDATE", payload, 1)
                    logarOperacaoSync("Mesa", mesaId, "UPDATE", "PENDING", null, payload)
                }
                
            } catch (syncError: Exception) {
                Log.w("MesaRepositoryInternal", "Erro ao adicionar atualização de relógio à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("MESA_RELOGIO", "MesaID=$mesaId", e)
            throw e
        }
    }
    
    /**
     * Atualiza relógio final
     */
    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = 
        mesaDao.atualizarRelogioFinal(mesaId, relogioFinal)
}

