package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.ColaboradorDao
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.TipoMeta
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 6: Repository interno para operações de MetaColaborador
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Inclui: MetaColaborador e todos os métodos relacionados
 */
internal class MetaRepositoryInternal(
    private val colaboradorDao: ColaboradorDao
) {
    
    // ==================== META COLABORADOR ====================
    
    fun obterMetasPorColaborador(colaboradorId: Long) = colaboradorDao.obterMetasPorColaborador(colaboradorId)
    
    suspend fun obterMetaAtual(colaboradorId: Long, tipoMeta: TipoMeta) = 
        colaboradorDao.obterMetaAtual(colaboradorId, tipoMeta)
    
    /**
     * Insere meta com sincronização
     */
    suspend fun inserirMeta(
        meta: MetaColaborador,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("META", "ColaboradorID=${meta.colaboradorId}, Tipo=${meta.tipoMeta}, Valor=${meta.valorMeta}")
        return try {
            val id = colaboradorDao.inserirMeta(meta)
            logDbInsertSuccess("META", "ColaboradorID=${meta.colaboradorId}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "colaboradorId": ${meta.colaboradorId},
                        "rotaId": ${if (meta.rotaId != null) meta.rotaId else "null"},
                        "cicloId": ${meta.cicloId},
                        "tipoMeta": "${meta.tipoMeta}",
                        "valorMeta": ${meta.valorMeta},
                        "valorAtual": ${meta.valorAtual},
                        "ativo": ${meta.ativo},
                        "dataCriacao": ${meta.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("MetaColaborador", id, "CREATE", payload, 1)
                logarOperacaoSync("MetaColaborador", id, "CREATE", "PENDING", null, payload)
            } catch (e: Exception) {
                Log.w("MetaRepositoryInternal", "Erro ao enfileirar META: ${e.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("META", "ColaboradorID=${meta.colaboradorId}", e)
            throw e
        }
    }
    
    /**
     * Atualiza meta com sincronização
     */
    suspend fun atualizarMeta(
        meta: MetaColaborador,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        colaboradorDao.atualizarMeta(meta)
        
        try {
            val payload = """
                {
                    "id": ${meta.id},
                    "colaboradorId": ${meta.colaboradorId},
                    "rotaId": ${if (meta.rotaId != null) meta.rotaId else "null"},
                    "cicloId": ${meta.cicloId},
                    "tipoMeta": "${meta.tipoMeta}",
                    "valorMeta": ${meta.valorMeta},
                    "valorAtual": ${meta.valorAtual},
                    "ativo": ${meta.ativo},
                    "dataCriacao": ${meta.dataCriacao.time}
                }
            """.trimIndent()
            
            adicionarOperacaoSync("MetaColaborador", meta.id, "UPDATE", payload, 1)
            logarOperacaoSync("MetaColaborador", meta.id, "UPDATE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("MetaRepositoryInternal", "Erro ao enfileirar UPDATE META: ${e.message}")
        }
    }
    
    /**
     * Deleta meta com sincronização
     */
    suspend fun deletarMeta(
        meta: MetaColaborador,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        colaboradorDao.deletarMeta(meta)
        
        try {
            val payload = """{ "id": ${meta.id} }"""
            adicionarOperacaoSync("MetaColaborador", meta.id, "DELETE", payload, 1)
            logarOperacaoSync("MetaColaborador", meta.id, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("MetaRepositoryInternal", "Erro ao enfileirar DELETE META: ${e.message}")
        }
    }
    
    suspend fun atualizarValorAtualMeta(metaId: Long, valorAtual: Double) = 
        colaboradorDao.atualizarValorAtualMeta(metaId, valorAtual)
    
    // ==================== METAS POR ROTA ====================
    
    fun obterMetasPorRota(rotaId: Long) = colaboradorDao.obterMetasPorRota(0L, rotaId)
    
    fun obterMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = 
        colaboradorDao.obterMetasPorCiclo(colaboradorId, cicloId)
    
    fun obterMetasPorColaboradorERota(colaboradorId: Long, rotaId: Long) = 
        colaboradorDao.obterMetasPorRota(colaboradorId, rotaId)
    
    fun obterMetasPorColaboradorCicloERota(colaboradorId: Long, cicloId: Long, rotaId: Long) = 
        colaboradorDao.obterMetasPorCicloERota(colaboradorId, cicloId, rotaId)
    
    suspend fun desativarMetasColaborador(colaboradorId: Long) = 
        colaboradorDao.desativarMetasColaborador(colaboradorId)
    
    // Métodos para metas
    suspend fun buscarMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = 
        colaboradorDao.buscarMetasPorColaboradorECiclo(colaboradorId, cicloId)
    
    suspend fun buscarMetasPorRotaECiclo(rotaId: Long, cicloId: Long) = 
        colaboradorDao.buscarMetasPorRotaECiclo(rotaId, cicloId)
    
    suspend fun existeMetaDuplicada(rotaId: Long, cicloId: Long, tipoMeta: TipoMeta): Boolean {
        val count = colaboradorDao.contarMetasPorRotaCicloETipo(rotaId, cicloId, tipoMeta)
        return count > 0
    }
    
    fun buscarMetasAtivasPorColaborador(colaboradorId: Long) = 
        colaboradorDao.buscarMetasAtivasPorColaborador(colaboradorId)
    
    suspend fun buscarMetasPorTipoECiclo(tipoMeta: TipoMeta, cicloId: Long) = 
        colaboradorDao.buscarMetasPorTipoECiclo(tipoMeta, cicloId)
}

