package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.CicloAcertoDao
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 2: Repository interno para operações de Ciclo
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Este repository é usado internamente pelo AppRepository.
 */
internal class CicloRepositoryInternal(
    private val cicloAcertoDao: CicloAcertoDao
) {
    
    /**
     * Obtém todos os ciclos
     */
    fun obterTodosCiclos(): Flow<List<CicloAcertoEntity>> = cicloAcertoDao.listarTodos()
    
    /**
     * Busca último ciclo finalizado por rota
     */
    suspend fun buscarUltimoCicloFinalizadoPorRota(rotaId: Long) = 
        cicloAcertoDao.buscarUltimoCicloFinalizadoPorRota(rotaId)
    
    /**
     * Busca ciclos por rota e ano
     */
    suspend fun buscarCiclosPorRotaEAno(rotaId: Long, ano: Int) = 
        cicloAcertoDao.buscarCiclosPorRotaEAno(rotaId, ano)
    
    /**
     * Busca ciclos por rota
     */
    suspend fun buscarCiclosPorRota(rotaId: Long) = cicloAcertoDao.buscarCiclosPorRota(rotaId)
    
    /**
     * Busca próximo número de ciclo
     */
    suspend fun buscarProximoNumeroCiclo(rotaId: Long, ano: Int) = 
        cicloAcertoDao.buscarProximoNumeroCiclo(rotaId, ano)
    
    /**
     * Busca ciclo ativo (em andamento)
     */
    suspend fun buscarCicloAtivo(rotaId: Long) = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
    
    /**
     * Insere um novo ciclo com sincronização
     */
    suspend fun inserirCicloAcerto(
        ciclo: CicloAcertoEntity,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("CICLO", "RotaID=${ciclo.rotaId}, Numero=${ciclo.numeroCiclo}, Status=${ciclo.status}")
        return try {
            val id = cicloAcertoDao.inserir(ciclo)
            logDbInsertSuccess("CICLO", "ID=$id, RotaID=${ciclo.rotaId}")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "numeroCiclo": ${ciclo.numeroCiclo},
                        "rotaId": ${ciclo.rotaId},
                        "ano": ${ciclo.ano},
                        "dataInicio": "${ciclo.dataInicio}",
                        "dataFim": "${ciclo.dataFim}",
                        "status": "${ciclo.status.name}",
                        "totalClientes": ${ciclo.totalClientes},
                        "clientesAcertados": ${ciclo.clientesAcertados},
                        "valorTotalAcertado": ${ciclo.valorTotalAcertado},
                        "valorTotalDespesas": ${ciclo.valorTotalDespesas},
                        "lucroLiquido": ${ciclo.lucroLiquido},
                        "debitoTotal": ${ciclo.debitoTotal},
                        "observacoes": "${ciclo.observacoes ?: ""}",
                        "criadoPor": "${ciclo.criadoPor}",
                        "dataCriacao": "${ciclo.dataCriacao}",
                        "dataAtualizacao": "${ciclo.dataAtualizacao}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("CicloAcerto", id, "CREATE", payload, 1)
                logarOperacaoSync("CicloAcerto", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("CicloRepositoryInternal", "Erro ao adicionar ciclo à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CICLO", "RotaID=${ciclo.rotaId}", e)
            throw e
        }
    }
    
    /**
     * Atualiza valores do ciclo
     */
    suspend fun atualizarValoresCiclo(
        cicloId: Long,
        valorTotalAcertado: Double,
        valorTotalDespesas: Double,
        clientesAcertados: Int
    ) = cicloAcertoDao.atualizarValoresCiclo(cicloId, valorTotalAcertado, valorTotalDespesas, clientesAcertados)
    
    /**
     * Busca ciclos que podem ter metas definidas (em andamento ou planejados)
     */
    suspend fun buscarCiclosParaMetas(rotaId: Long): List<CicloAcertoEntity> {
        val cicloEmAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
        val ciclosFuturos = cicloAcertoDao.buscarCiclosFuturosPorRota(rotaId)
        
        val listaCombinada = mutableListOf<CicloAcertoEntity>()
        cicloEmAndamento?.let { listaCombinada.add(it) }
        listaCombinada.addAll(ciclosFuturos)
        
        return listaCombinada
    }
}

