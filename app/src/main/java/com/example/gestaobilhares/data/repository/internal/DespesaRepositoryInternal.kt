package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.entities.DespesaResumo
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 2: Repository interno para operações de Despesa
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Este repository é usado internamente pelo AppRepository.
 */
internal class DespesaRepositoryInternal(
    private val despesaDao: DespesaDao
) {
    
    /**
     * Obtém todas as despesas
     */
    fun obterTodasDespesas() = despesaDao.buscarTodasComRota()
    
    /**
     * Obtém despesas por rota
     */
    fun obterDespesasPorRota(rotaId: Long): Flow<List<Despesa>> = despesaDao.buscarPorRota(rotaId)
    
    /**
     * Obtém despesa por ID
     */
    suspend fun obterDespesaPorId(id: Long): Despesa? = despesaDao.buscarPorId(id)
    
    /**
     * Insere uma nova despesa com upload de foto e sincronização
     */
    suspend fun inserirDespesa(
        despesa: Despesa,
        obterEmpresaId: suspend () -> String,
        uploadFotoSeNecessario: suspend (String?, String, String, Long, Long?) -> String?,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("DESPESA", "Descricao=${despesa.descricao}, RotaID=${despesa.rotaId}")
        return try {
            val id = despesaDao.inserir(despesa)
            logDbInsertSuccess("DESPESA", "Descricao=${despesa.descricao}, ID=$id")
            
            // ✅ NOVO: Upload de foto para Firebase Storage antes de sincronizar
            val empresaId = obterEmpresaId()
            Log.d("DespesaRepositoryInternal", "📷 Processando foto de comprovante para despesa $id: foto='${despesa.fotoComprovante}'")
            
            val fotoUrl = uploadFotoSeNecessario(
                despesa.fotoComprovante,
                "comprovante",
                empresaId,
                id,
                null
            )
            
            Log.d("DespesaRepositoryInternal", "📷 Resultado upload despesa: fotoUrl='$fotoUrl' (original: '${despesa.fotoComprovante}')")
            
            val despesaAtualizada = despesa.copy(id = id)
            
            // ✅ Se upload falhou e havia foto, remover do banco para não sincronizar caminho inválido
            if (fotoUrl == null && !despesa.fotoComprovante.isNullOrBlank()) {
                Log.w("DespesaRepositoryInternal", "⚠️ Upload falhou - removendo foto do banco para não sincronizar caminho inválido")
                val despesaSemFoto = despesaAtualizada.copy(fotoComprovante = null)
                despesaDao.atualizar(despesaSemFoto)
            }
            
            // ✅ FASE 3C: Adicionar à fila de sincronização com URL da foto (se upload foi bem-sucedido)
            try {
                val fotoUrlParaPayload = if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                    fotoUrl
                } else {
                    ""
                }
                
                val payload = """
                    {
                        "id": $id,
                        "rotaId": ${despesaAtualizada.rotaId},
                        "descricao": "${despesaAtualizada.descricao}",
                        "valor": ${despesaAtualizada.valor},
                        "categoria": "${despesaAtualizada.categoria}",
                        "tipoDespesa": "${despesaAtualizada.tipoDespesa}",
                        "dataHora": "${despesaAtualizada.dataHora}",
                        "observacoes": "${despesaAtualizada.observacoes}",
                        "criadoPor": "${despesaAtualizada.criadoPor}",
                        "cicloId": ${despesaAtualizada.cicloId ?: "null"},
                        "origemLancamento": "${despesaAtualizada.origemLancamento}",
                        "cicloAno": ${despesaAtualizada.cicloAno ?: "null"},
                        "cicloNumero": ${despesaAtualizada.cicloNumero ?: "null"},
                        "fotoComprovante": "$fotoUrlParaPayload",
                        "veiculoId": ${despesaAtualizada.veiculoId ?: "null"},
                        "kmRodado": ${despesaAtualizada.kmRodado ?: "null"},
                        "litrosAbastecidos": ${despesaAtualizada.litrosAbastecidos ?: "null"}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Despesa", id, "CREATE", payload, 1)
                logarOperacaoSync("Despesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("DespesaRepositoryInternal", "Erro ao adicionar despesa à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("DESPESA", "Descricao=${despesa.descricao}", e)
            throw e
        }
    }
    
    /**
     * Atualiza uma despesa com upload de foto e sincronização
     */
    suspend fun atualizarDespesa(
        despesa: Despesa,
        obterEmpresaId: suspend () -> String,
        uploadFotoSeNecessario: suspend (String?, String, String, Long, Long?) -> String?,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("DESPESA", "ID=${despesa.id}, Descricao=${despesa.descricao}")
        try {
            despesaDao.atualizar(despesa)
            logDbUpdateSuccess("DESPESA", "ID=${despesa.id}")
            
            // ✅ NOVO: Upload de foto para Firebase Storage antes de sincronizar
            val empresaId = obterEmpresaId()
            Log.d("DespesaRepositoryInternal", "📷 Processando foto de comprovante para despesa ${despesa.id} (UPDATE): foto='${despesa.fotoComprovante}'")
            
            val fotoUrl = uploadFotoSeNecessario(
                despesa.fotoComprovante,
                "comprovante",
                empresaId,
                despesa.id,
                null
            )
            
            Log.d("DespesaRepositoryInternal", "📷 Resultado upload despesa (UPDATE): fotoUrl='$fotoUrl' (original: '${despesa.fotoComprovante}')")
            
            val despesaAtualizada = despesa
            
            // ✅ Se upload falhou e havia foto, remover do banco para não sincronizar caminho inválido
            if (fotoUrl == null && !despesa.fotoComprovante.isNullOrBlank()) {
                Log.w("DespesaRepositoryInternal", "⚠️ Upload falhou - removendo foto do banco para não sincronizar caminho inválido")
                val despesaSemFoto = despesaAtualizada.copy(fotoComprovante = null)
                despesaDao.atualizar(despesaSemFoto)
            }
            
            // ✅ FASE 3C: Adicionar à fila de sincronização com URL da foto (se upload foi bem-sucedido)
            try {
                val fotoUrlParaPayload = if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                    fotoUrl
                } else {
                    ""
                }
                
                val payload = """
                    {
                        "id": ${despesaAtualizada.id},
                        "rotaId": ${despesaAtualizada.rotaId},
                        "descricao": "${despesaAtualizada.descricao}",
                        "valor": ${despesaAtualizada.valor},
                        "categoria": "${despesaAtualizada.categoria}",
                        "tipoDespesa": "${despesaAtualizada.tipoDespesa}",
                        "dataHora": "${despesaAtualizada.dataHora}",
                        "observacoes": "${despesaAtualizada.observacoes}",
                        "criadoPor": "${despesaAtualizada.criadoPor}",
                        "cicloId": ${despesaAtualizada.cicloId ?: "null"},
                        "origemLancamento": "${despesaAtualizada.origemLancamento}",
                        "cicloAno": ${despesaAtualizada.cicloAno ?: "null"},
                        "cicloNumero": ${despesaAtualizada.cicloNumero ?: "null"},
                        "fotoComprovante": "$fotoUrlParaPayload",
                        "veiculoId": ${despesaAtualizada.veiculoId ?: "null"},
                        "kmRodado": ${despesaAtualizada.kmRodado ?: "null"},
                        "litrosAbastecidos": ${despesaAtualizada.litrosAbastecidos ?: "null"}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Despesa", despesaAtualizada.id, "UPDATE", payload, 1)
                logarOperacaoSync("Despesa", despesaAtualizada.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("DespesaRepositoryInternal", "Erro ao adicionar atualização de despesa à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("DESPESA", "ID=${despesa.id}", e)
            throw e
        }
    }
    
    /**
     * Deleta uma despesa
     */
    suspend fun deletarDespesa(despesa: Despesa) = despesaDao.deletar(despesa)
    
    /**
     * Calcula total de despesas por rota
     */
    suspend fun calcularTotalPorRota(rotaId: Long): Double = despesaDao.calcularTotalPorRota(rotaId)
    
    /**
     * Calcula total geral de despesas
     */
    suspend fun calcularTotalGeral(): Double = despesaDao.calcularTotalGeral()
    
    /**
     * Conta despesas por rota
     */
    suspend fun contarDespesasPorRota(rotaId: Long): Int = despesaDao.contarPorRota(rotaId)
    
    /**
     * Deleta despesas por rota
     */
    suspend fun deletarDespesasPorRota(rotaId: Long) = despesaDao.deletarPorRota(rotaId)
    
    /**
     * Busca despesas por ciclo ID
     */
    fun buscarDespesasPorCicloId(cicloId: Long): Flow<List<Despesa>> = despesaDao.buscarPorCicloId(cicloId)
    
    /**
     * Busca despesas por rota e ciclo ID
     */
    fun buscarDespesasPorRotaECicloId(rotaId: Long, cicloId: Long): Flow<List<Despesa>> = 
        despesaDao.buscarPorRotaECicloId(rotaId, cicloId)
    
    /**
     * Busca despesas globais por ciclo
     */
    suspend fun buscarDespesasGlobaisPorCiclo(ano: Int, numero: Int): List<Despesa> = 
        despesaDao.buscarGlobaisPorCiclo(ano, numero)
    
    /**
     * Soma despesas globais por ciclo
     */
    suspend fun somarDespesasGlobaisPorCiclo(ano: Int, numero: Int): Double = 
        despesaDao.somarGlobaisPorCiclo(ano, numero)
}

