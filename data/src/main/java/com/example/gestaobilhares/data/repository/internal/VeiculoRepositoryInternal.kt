package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 4: Repository interno para operações de Veículos
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Inclui: Veiculo, HistoricoManutencaoVeiculo, HistoricoCombustivelVeiculo
 */
internal class VeiculoRepositoryInternal(
    private val veiculoDao: VeiculoDao,
    private val historicoManutencaoVeiculoDao: HistoricoManutencaoVeiculoDao,
    private val historicoCombustivelVeiculoDao: HistoricoCombustivelVeiculoDao
) {
    
    // ==================== VEICULO ====================
    
    fun obterTodosVeiculos() = veiculoDao.listar()
    
    suspend fun obterVeiculoPorId(id: Long) = veiculoDao.listar().first().find { it.id == id }
    
    /**
     * Insere veículo com sincronização
     */
    suspend fun inserirVeiculo(
        veiculo: Veiculo,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("VEICULO", "Nome=${veiculo.nome}, Placa=${veiculo.placa}")
        return try {
            val id = veiculoDao.inserir(veiculo)
            logDbInsertSuccess("VEICULO", "Nome=${veiculo.nome}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${veiculo.nome}",
                        "placa": "${veiculo.placa}",
                        "marca": "${veiculo.marca}",
                        "modelo": "${veiculo.modelo}",
                        "anoModelo": ${veiculo.anoModelo},
                        "kmAtual": ${veiculo.kmAtual},
                        "dataCompra": ${veiculo.dataCompra?.time ?: "null"},
                        "observacoes": "${veiculo.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Veiculo", id, "CREATE", payload, 1)
                logarOperacaoSync("Veiculo", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("VeiculoRepositoryInternal", "Erro ao adicionar criação de veículo à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("VEICULO", "Nome=${veiculo.nome}", e)
            throw e
        }
    }
    
    /**
     * Atualiza veículo com sincronização
     */
    suspend fun atualizarVeiculo(
        veiculo: Veiculo,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("VEICULO", "ID=${veiculo.id}, Nome=${veiculo.nome}")
        try {
            veiculoDao.atualizar(veiculo)
            logDbUpdateSuccess("VEICULO", "ID=${veiculo.id}")
            
            try {
                val payload = """
                    {
                        "id": ${veiculo.id},
                        "nome": "${veiculo.nome}",
                        "placa": "${veiculo.placa}",
                        "marca": "${veiculo.marca}",
                        "modelo": "${veiculo.modelo}",
                        "anoModelo": ${veiculo.anoModelo},
                        "kmAtual": ${veiculo.kmAtual},
                        "dataCompra": ${veiculo.dataCompra?.time ?: "null"},
                        "observacoes": "${veiculo.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Veiculo", veiculo.id, "UPDATE", payload, 1)
                logarOperacaoSync("Veiculo", veiculo.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("VeiculoRepositoryInternal", "Erro ao adicionar atualização de veículo à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("VEICULO", "ID=${veiculo.id}", e)
            throw e
        }
    }
    
    suspend fun deletarVeiculo(veiculo: Veiculo) = veiculoDao.deletar(veiculo)
    
    // ==================== HISTORICO MANUTENCAO VEICULO ====================
    
    fun obterTodosHistoricoManutencaoVeiculo() = historicoManutencaoVeiculoDao.listarTodos()
    
    suspend fun obterHistoricoManutencaoVeiculoPorId(id: Long) = 
        historicoManutencaoVeiculoDao.listarTodos().first().find { it.id == id }
    
    /**
     * Insere histórico de manutenção de veículo com sincronização
     */
    suspend fun inserirHistoricoManutencaoVeiculo(
        historico: HistoricoManutencaoVeiculo,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("HISTORICO_MANUTENCAO_VEICULO", "Veiculo=${historico.veiculoId}, Tipo=${historico.tipoManutencao}")
        return try {
            val id = historicoManutencaoVeiculoDao.inserir(historico)
            logDbInsertSuccess("HISTORICO_MANUTENCAO_VEICULO", "Veiculo=${historico.veiculoId}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "veiculoId": ${historico.veiculoId},
                        "tipoManutencao": "${historico.tipoManutencao}",
                        "descricao": "${historico.descricao}",
                        "dataManutencao": ${historico.dataManutencao.time},
                        "valor": ${historico.valor},
                        "kmVeiculo": ${historico.kmVeiculo},
                        "responsavel": "${historico.responsavel ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoManutencaoVeiculo", id, "CREATE", payload, 1)
                logarOperacaoSync("HistoricoManutencaoVeiculo", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("VeiculoRepositoryInternal", "Erro ao adicionar criação de histórico manutenção veículo à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("HISTORICO_MANUTENCAO_VEICULO", "Veiculo=${historico.veiculoId}", e)
            throw e
        }
    }
    
    /**
     * Atualiza histórico de manutenção de veículo com sincronização
     */
    suspend fun atualizarHistoricoManutencaoVeiculo(
        historico: HistoricoManutencaoVeiculo,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("HISTORICO_MANUTENCAO_VEICULO", "ID=${historico.id}, Veiculo=${historico.veiculoId}")
        try {
            historicoManutencaoVeiculoDao.atualizar(historico)
            logDbUpdateSuccess("HISTORICO_MANUTENCAO_VEICULO", "ID=${historico.id}")
            
            try {
                val payload = """
                    {
                        "id": ${historico.id},
                        "veiculoId": ${historico.veiculoId},
                        "tipoManutencao": "${historico.tipoManutencao}",
                        "descricao": "${historico.descricao}",
                        "dataManutencao": ${historico.dataManutencao.time},
                        "valor": ${historico.valor},
                        "kmVeiculo": ${historico.kmVeiculo},
                        "responsavel": "${historico.responsavel ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoManutencaoVeiculo", historico.id, "UPDATE", payload, 1)
                logarOperacaoSync("HistoricoManutencaoVeiculo", historico.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("VeiculoRepositoryInternal", "Erro ao adicionar atualização de histórico manutenção veículo à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("HISTORICO_MANUTENCAO_VEICULO", "ID=${historico.id}", e)
            throw e
        }
    }
    
    suspend fun deletarHistoricoManutencaoVeiculo(historico: HistoricoManutencaoVeiculo) = 
        historicoManutencaoVeiculoDao.deletar(historico)
    
    // ==================== HISTORICO COMBUSTIVEL VEICULO ====================
    
    fun obterTodosHistoricoCombustivelVeiculo() = historicoCombustivelVeiculoDao.listarTodos()
    
    suspend fun obterHistoricoCombustivelVeiculoPorId(id: Long) = 
        historicoCombustivelVeiculoDao.listarTodos().first().find { it.id == id }
    
    /**
     * Insere histórico de combustível de veículo com sincronização
     */
    suspend fun inserirHistoricoCombustivelVeiculo(
        historico: HistoricoCombustivelVeiculo,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("HISTORICO_COMBUSTIVEL_VEICULO", "Veiculo=${historico.veiculoId}, Litros=${historico.litros}")
        return try {
            val id = historicoCombustivelVeiculoDao.inserir(historico)
            logDbInsertSuccess("HISTORICO_COMBUSTIVEL_VEICULO", "Veiculo=${historico.veiculoId}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "veiculoId": ${historico.veiculoId},
                        "dataAbastecimento": ${historico.dataAbastecimento.time},
                        "litros": ${historico.litros},
                        "valor": ${historico.valor},
                        "kmVeiculo": ${historico.kmVeiculo},
                        "kmRodado": ${historico.kmRodado},
                        "posto": "${historico.posto ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoCombustivelVeiculo", id, "CREATE", payload, 1)
                logarOperacaoSync("HistoricoCombustivelVeiculo", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("VeiculoRepositoryInternal", "Erro ao adicionar criação de histórico combustível veículo à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("HISTORICO_COMBUSTIVEL_VEICULO", "Veiculo=${historico.veiculoId}", e)
            throw e
        }
    }
    
    /**
     * Atualiza histórico de combustível de veículo com sincronização
     */
    suspend fun atualizarHistoricoCombustivelVeiculo(
        historico: HistoricoCombustivelVeiculo,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("HISTORICO_COMBUSTIVEL_VEICULO", "ID=${historico.id}, Veiculo=${historico.veiculoId}")
        try {
            historicoCombustivelVeiculoDao.atualizar(historico)
            logDbUpdateSuccess("HISTORICO_COMBUSTIVEL_VEICULO", "ID=${historico.id}")
            
            try {
                val payload = """
                    {
                        "id": ${historico.id},
                        "veiculoId": ${historico.veiculoId},
                        "dataAbastecimento": ${historico.dataAbastecimento.time},
                        "litros": ${historico.litros},
                        "valor": ${historico.valor},
                        "kmVeiculo": ${historico.kmVeiculo},
                        "kmRodado": ${historico.kmRodado},
                        "posto": "${historico.posto ?: ""}",
                        "observacoes": "${historico.observacoes ?: ""}",
                        "dataCriacao": ${historico.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoCombustivelVeiculo", historico.id, "UPDATE", payload, 1)
                logarOperacaoSync("HistoricoCombustivelVeiculo", historico.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("VeiculoRepositoryInternal", "Erro ao adicionar atualização de histórico combustível veículo à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("HISTORICO_COMBUSTIVEL_VEICULO", "ID=${historico.id}", e)
            throw e
        }
    }
    
    suspend fun deletarHistoricoCombustivelVeiculo(historico: HistoricoCombustivelVeiculo) = 
        historicoCombustivelVeiculoDao.deletar(historico)
}

