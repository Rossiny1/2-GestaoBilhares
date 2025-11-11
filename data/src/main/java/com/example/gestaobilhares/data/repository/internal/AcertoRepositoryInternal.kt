package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.dao.AcertoMesaDao
import com.example.gestaobilhares.data.entities.Acerto
import kotlinx.coroutines.flow.Flow
import android.util.Log
import com.google.gson.Gson

/**
 * ✅ FASE 12.14 Etapa 2: Repository interno para operações de Acerto
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Este repository é usado internamente pelo AppRepository.
 */
internal class AcertoRepositoryInternal(
    private val acertoDao: AcertoDao,
    private val acertoMesaDao: AcertoMesaDao
) {
    
    /**
     * Obtém acertos por cliente
     */
    fun obterAcertosPorCliente(clienteId: Long): Flow<List<Acerto>> = acertoDao.buscarPorCliente(clienteId)
    
    /**
     * Obtém acerto por ID
     */
    suspend fun obterAcertoPorId(id: Long): Acerto? = acertoDao.buscarPorId(id)
    
    /**
     * Busca último acerto por cliente
     */
    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long): Acerto? = 
        acertoDao.buscarUltimoAcertoPorCliente(clienteId)
    
    /**
     * Obtém todos os acertos
     */
    fun obterTodosAcertos(): Flow<List<Acerto>> = acertoDao.listarTodos()
    
    /**
     * Busca acertos por ciclo ID
     */
    fun buscarAcertosPorCicloId(cicloId: Long): Flow<List<Acerto>> = acertoDao.buscarPorCicloId(cicloId)
    
    /**
     * Busca por rota e ciclo ID
     */
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long): Flow<List<Acerto>> = 
        acertoDao.buscarPorRotaECicloId(rotaId, cicloId)
    
    /**
     * Busca último acerto por mesa
     */
    suspend fun buscarUltimoAcertoPorMesa(mesaId: Long): Acerto? = 
        acertoDao.buscarUltimoAcertoPorMesa(mesaId)
    
    /**
     * Busca último acerto mesa item
     */
    suspend fun buscarUltimoAcertoMesaItem(mesaId: Long) = acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    
    /**
     * Busca observação do último acerto
     */
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long): String? = 
        acertoDao.buscarObservacaoUltimoAcerto(clienteId)
    
    /**
     * Busca últimos acertos por clientes
     */
    suspend fun buscarUltimosAcertosPorClientes(clienteIds: List<Long>): List<Acerto> =
        acertoDao.buscarUltimosAcertosPorClientes(clienteIds)
    
    /**
     * Busca acerto mesa por mesa
     */
    suspend fun buscarAcertoMesaPorMesa(mesaId: Long) = acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    
    /**
     * Busca por ID (alias para obterAcertoPorId)
     */
    suspend fun buscarPorId(id: Long): Acerto? = acertoDao.buscarPorId(id)
    
    /**
     * Insere um novo acerto
     * Nota: Não adiciona à fila de sync aqui - será feito pelo SettlementViewModel após inserir as mesas
     */
    suspend fun inserirAcerto(
        acerto: Acerto,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit
    ): Long {
        logDbInsertStart("ACERTO", "ClienteID=${acerto.clienteId}, RotaID=${acerto.rotaId}, Valor=${acerto.valorRecebido}")
        return try {
            val id = acertoDao.inserir(acerto)
            logDbInsertSuccess("ACERTO", "ClienteID=${acerto.clienteId}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("ACERTO", "ClienteID=${acerto.clienteId}", e)
            throw e
        }
    }
    
    /**
     * Atualiza um acerto com sincronização
     */
    suspend fun atualizarAcerto(
        acerto: Acerto,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("ACERTO", "ID=${acerto.id}, ClienteID=${acerto.clienteId}")
        try {
            acertoDao.atualizar(acerto)
            logDbUpdateSuccess("ACERTO", "ID=${acerto.id}, ClienteID=${acerto.clienteId}")
            
            // ✅ CORREÇÃO: Adicionar operação UPDATE à fila de sincronização
            try {
                val payloadMap = mutableMapOf<String, Any?>(
                    "id" to acerto.id,
                    "clienteId" to acerto.clienteId,
                    "colaboradorId" to acerto.colaboradorId,
                    "dataAcerto" to acerto.dataAcerto,
                    "periodoInicio" to acerto.periodoInicio,
                    "periodoFim" to acerto.periodoFim,
                    "totalMesas" to acerto.totalMesas,
                    "debitoAnterior" to acerto.debitoAnterior,
                    "valorTotal" to acerto.valorTotal,
                    "desconto" to acerto.desconto,
                    "valorComDesconto" to acerto.valorComDesconto,
                    "valorRecebido" to acerto.valorRecebido,
                    "debitoAtual" to acerto.debitoAtual,
                    "status" to acerto.status.name,
                    "observacoes" to acerto.observacoes,
                    "dataCriacao" to acerto.dataCriacao,
                    "dataFinalizacao" to acerto.dataFinalizacao,
                    "representante" to acerto.representante,
                    "tipoAcerto" to acerto.tipoAcerto,
                    "panoTrocado" to acerto.panoTrocado,
                    "numeroPano" to acerto.numeroPano,
                    "rotaId" to acerto.rotaId,
                    "cicloId" to acerto.cicloId,
                    "syncTimestamp" to acerto.syncTimestamp,
                    "syncVersion" to acerto.syncVersion,
                    "syncStatus" to acerto.syncStatus.name
                )
                // Adicionar JSONs que já podem estar formatados
                acerto.metodosPagamentoJson?.let { payloadMap["metodosPagamentoJson"] = it }
                acerto.dadosExtrasJson?.let { payloadMap["dadosExtrasJson"] = it }

                val payload = Gson().toJson(payloadMap)

                adicionarOperacaoSync("Acerto", acerto.id, "UPDATE", payload, 1)
                logarOperacaoSync("Acerto", acerto.id, "UPDATE", "PENDING", null, payload)

            } catch (syncError: Exception) {
                Log.w("AcertoRepositoryInternal", "Erro ao adicionar atualização de acerto à fila de sync: ${syncError.message}")
                // Não falha a operação principal por erro de sync
            }
            
        } catch (e: Exception) {
            logDbUpdateError("ACERTO", "ID=${acerto.id}", e)
            throw e
        }
    }
    
    /**
     * Deleta um acerto
     */
    suspend fun deletarAcerto(acerto: Acerto) = acertoDao.deletar(acerto)
}

