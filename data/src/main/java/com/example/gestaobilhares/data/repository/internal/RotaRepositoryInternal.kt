package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.entities.Rota
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 2: Repository interno para operações de Rota
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Este repository é usado internamente pelo AppRepository.
 * 
 * Nota: Métodos de cálculo complexos (como getRotasResumoComAtualizacaoTempoReal)
 * que dependem de múltiplos DAOs permanecem no AppRepository por enquanto.
 */
internal class RotaRepositoryInternal(
    private val rotaDao: RotaDao
) {
    
    /**
     * Obtém todas as rotas
     */
    fun obterTodasRotas(): Flow<List<Rota>> = rotaDao.getAllRotas()
    
    /**
     * Obtém rotas ativas
     */
    fun obterRotasAtivas(): Flow<List<Rota>> = rotaDao.getAllRotasAtivas()
    
    /**
     * Obtém rota por ID
     */
    suspend fun obterRotaPorId(id: Long): Rota? = rotaDao.getRotaById(id)
    
    /**
     * Obtém rota por ID (Flow)
     */
    fun obterRotaPorIdFlow(id: Long): Flow<Rota?> = rotaDao.obterRotaPorId(id)
    
    /**
     * Obtém rota por nome
     */
    suspend fun obterRotaPorNome(nome: String): Rota? = rotaDao.getRotaByNome(nome)
    
    /**
     * Verifica se existe rota com nome (retorna Int - 0 se não existe, >0 se existe)
     */
    suspend fun existeRotaComNome(nome: String, excludeId: Long = 0): Int = 
        rotaDao.existeRotaComNome(nome, excludeId)
    
    /**
     * Conta rotas ativas
     */
    suspend fun contarRotasAtivas(): Int = rotaDao.contarRotasAtivas()
    
    /**
     * Insere uma nova rota com sincronização
     */
    suspend fun inserirRota(
        rota: Rota,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("ROTA", "Nome=${rota.nome}")
        return try {
            val id = rotaDao.insertRota(rota)
            logDbInsertSuccess("ROTA", "Nome=${rota.nome}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${rota.nome}",
                        "descricao": "${rota.descricao}",
                        "ativa": ${rota.ativa},
                        "dataCriacao": ${rota.dataCriacao}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Rota", id, "CREATE", payload, 1)
                logarOperacaoSync("Rota", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("RotaRepositoryInternal", "Erro ao adicionar rota à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("ROTA", "Nome=${rota.nome}", e)
            throw e
        }
    }
    
    /**
     * Insere múltiplas rotas
     */
    suspend fun inserirRotas(
        rotas: List<Rota>,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit
    ): List<Long> {
        logDbInsertStart("ROTA_LIST", "Quantidade=${rotas.size}")
        return try {
            val ids = rotaDao.insertRotas(rotas)
            logDbInsertSuccess("ROTA_LIST", "IDs=${ids.joinToString()}")
            ids
        } catch (e: Exception) {
            logDbInsertError("ROTA_LIST", "Quantidade=${rotas.size}", e)
            throw e
        }
    }
    
    /**
     * Atualiza uma rota com sincronização
     */
    suspend fun atualizarRota(
        rota: Rota,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("ROTA", "ID=${rota.id}, Nome=${rota.nome}")
        try {
            rotaDao.updateRota(rota)
            logDbUpdateSuccess("ROTA", "ID=${rota.id}, Nome=${rota.nome}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${rota.id},
                        "nome": "${rota.nome}",
                        "descricao": "${rota.descricao ?: ""}",
                        "colaboradorResponsavel": "${rota.colaboradorResponsavel}",
                        "cidades": "${rota.cidades}",
                        "ativa": ${rota.ativa},
                        "cor": "${rota.cor}",
                        "dataCriacao": ${rota.dataCriacao},
                        "dataAtualizacao": ${rota.dataAtualizacao},
                        "statusAtual": "${rota.statusAtual.name}",
                        "cicloAcertoAtual": ${rota.cicloAcertoAtual},
                        "anoCiclo": ${rota.anoCiclo},
                        "dataInicioCiclo": ${rota.dataInicioCiclo ?: "null"},
                        "dataFimCiclo": ${rota.dataFimCiclo ?: "null"}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Rota", rota.id, "UPDATE", payload, 1)
                logarOperacaoSync("Rota", rota.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("RotaRepositoryInternal", "Erro ao adicionar atualização de rota à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("ROTA", "ID=${rota.id}", e)
            throw e
        }
    }
    
    /**
     * Atualiza múltiplas rotas
     */
    suspend fun atualizarRotas(rotas: List<Rota>) = rotaDao.updateRotas(rotas)
    
    /**
     * Deleta uma rota
     */
    suspend fun deletarRota(rota: Rota) = rotaDao.deleteRota(rota)
    
    /**
     * Desativa uma rota
     */
    suspend fun desativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.desativarRota(rotaId, timestamp)
    
    /**
     * Ativa uma rota
     */
    suspend fun ativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.ativarRota(rotaId, timestamp)
    
    /**
     * Atualiza status da rota
     */
    suspend fun atualizarStatus(rotaId: Long, status: String, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.atualizarStatus(rotaId, status, timestamp)
    
    /**
     * Atualiza ciclo de acerto da rota
     */
    suspend fun atualizarCicloAcerto(rotaId: Long, ciclo: Int, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.atualizarCicloAcerto(rotaId, ciclo, timestamp)
    
    /**
     * Inicia ciclo da rota
     */
    suspend fun iniciarCicloRota(rotaId: Long, ciclo: Int, dataInicio: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.iniciarCicloRota(rotaId, ciclo, dataInicio, timestamp)
    
    /**
     * Finaliza ciclo da rota
     */
    suspend fun finalizarCicloRota(rotaId: Long, dataFim: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.finalizarCicloRota(rotaId, dataFim, timestamp)
}

