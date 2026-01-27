package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.CicloAcertoDao
import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Calendar
import com.example.gestaobilhares.data.repository.ClienteRepository
import java.time.ZoneId
import javax.inject.Inject

/**
 * Repositório para operações de ciclos de acerto
 * ✅ FASE 9C: REPOSITÓRIO PARA HISTÓRICO DE CICLOS
 * 
 * @deprecated Use AppRepository ao invés deste repository individual.
 * Este repository será removido em versão futura. Migre para AppRepository
 * obtido via RepositoryFactory.getAppRepository(context).
 */
@Deprecated(
    message = "Use AppRepository ao invés de CicloAcertoRepository. " +
            "Obtenha via RepositoryFactory.getAppRepository(context).",
    replaceWith = ReplaceWith(
        "RepositoryFactory.getAppRepository(context)",
        "com.example.gestaobilhares.data.factory.RepositoryFactory"
    ),
    level = DeprecationLevel.WARNING
)
class CicloAcertoRepository @Inject constructor(
    private val cicloAcertoDao: CicloAcertoDao,
    private val despesaDao: DespesaDao, // ✅ CORRIGIDO: Usar DespesaDao diretamente em vez de DespesaRepository
    private val acertoRepository: com.example.gestaobilhares.data.repository.AcertoRepository,
    private val clienteRepository: ClienteRepository, // NOVO
    private val rotaDao: com.example.gestaobilhares.data.dao.RotaDao? = null, // NOVO: Para relatórios
    private val colaboradorDao: com.example.gestaobilhares.data.dao.ColaboradorDao? = null // NOVO: Para finalizar metas
) {

    /**
     * Busca todos os ciclos de uma rota
     */
    suspend fun buscarCiclosPorRota(rotaId: Long): List<CicloAcertoEntity> {
        val ciclos = cicloAcertoDao.listarPorRota(rotaId).first()
        Timber.d(
            "Buscando ciclos da rota $rotaId: encontrados ${ciclos.size} ciclos. Dados: " +
                    ciclos.joinToString(" | ") { c -> "id=${c.id}, total=${c.valorTotalAcertado}, despesas=${c.valorTotalDespesas}, lucro=${c.lucroLiquido}, clientes=${c.clientesAcertados}" }
        )
        return ciclos
    }

    /**
     * Busca ciclos por período
     */
    suspend fun buscarCiclosPorPeriodo(
        @Suppress("UNUSED_PARAMETER") rotaId: Long,
        dataInicio: Long,
        dataFim: Long
    ): List<CicloAcertoEntity> {
        return cicloAcertoDao.listarPorPeriodo(dataInicio, dataFim).first()
    }

    /**
     * Busca ciclo por ID
     */
    suspend fun buscarCicloPorId(cicloId: Long): CicloAcertoEntity? {
        return cicloAcertoDao.buscarPorId(cicloId)
    }

    /**
     * Busca ciclo ativo de uma rota
     */
    suspend fun buscarCicloAtivo(rotaId: Long): CicloAcertoEntity? {
        return cicloAcertoDao.buscarCicloEmAndamento(rotaId)
    }

    /**
     * ✅ NOVO: Busca o último ciclo de uma rota (finalizado ou em andamento)
     */
    suspend fun buscarUltimoCicloPorRota(rotaId: Long): CicloAcertoEntity? {
        return cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)
    }

    /**
     * Insere ou atualiza um ciclo
     */
    suspend fun inserirOuAtualizarCiclo(ciclo: CicloAcertoEntity): Long {
        return cicloAcertoDao.inserir(ciclo)
    }

    /**
     * Atualiza status do ciclo
     */
    suspend fun atualizarStatusCiclo(cicloId: Long, status: StatusCicloAcerto) {
        val ciclo = buscarCicloPorId(cicloId)
        ciclo?.let {
            val cicloAtualizado = it.copy(status = status)
            cicloAcertoDao.atualizar(cicloAtualizado)
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
    ) {
        val ciclo = buscarCicloPorId(cicloId)
        ciclo?.let {
            val cicloAtualizado = it.copy(
                valorTotalAcertado = valorTotalAcertado,
                valorTotalDespesas = valorTotalDespesas,
                clientesAcertados = clientesAcertados,
                lucroLiquido = valorTotalAcertado - valorTotalDespesas
            )
            cicloAcertoDao.atualizar(cicloAtualizado)
            Timber.d(
                "Ciclo atualizado: cicloId=$cicloId, valorTotalAcertado=$valorTotalAcertado, valorTotalDespesas=$valorTotalDespesas, clientesAcertados=$clientesAcertados, lucroLiquido=${valorTotalAcertado - valorTotalDespesas}"
            )
        }
    }

    /**
     * Recalcula e atualiza todos os campos agregados do ciclo a partir dos acertos e despesas do ciclo
     */
    suspend fun atualizarValoresCicloComRecalculo(cicloId: Long) {
        val ciclo = buscarCicloPorId(cicloId)
        ciclo?.let {
            val acertos = acertoRepository.buscarPorCicloId(cicloId).first()
            val despesas = despesaDao.buscarPorCicloId(cicloId).first() // ✅ CORRIGIDO: Usar despesaDao diretamente
            val valorTotalAcertado: Double = acertos.sumOf { it.valorRecebido }
            val valorTotalDespesas: Double = despesas.sumOf { it.valor }
            val clientesAcertados = acertos.map { it.clienteId }.distinct().size
            val lucroLiquido: Double = valorTotalAcertado - valorTotalDespesas
            val cicloAtualizado = it.copy(
                valorTotalAcertado = valorTotalAcertado,
                valorTotalDespesas = valorTotalDespesas,
                clientesAcertados = clientesAcertados,
                lucroLiquido = lucroLiquido
            )
            cicloAcertoDao.atualizar(cicloAtualizado)
            Timber.d(
                "[RECALCULO] Ciclo $cicloId atualizado: arrecadado=$valorTotalAcertado, despesas=$valorTotalDespesas, clientes=$clientesAcertados, lucro=$lucroLiquido"
            )
        }
    }

    /**
     * Exclui um ciclo
     */
    suspend fun excluirCiclo(cicloId: Long) {
        val ciclo = buscarCicloPorId(cicloId)
        ciclo?.let {
            cicloAcertoDao.deletar(it)
        }
    }

    /**
     * Busca estatísticas de uma rota
     */
    suspend fun buscarEstatisticasRota(rotaId: Long): CicloAcertoEntity? {
        return cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)
    }

    /**
     * Busca todos os ciclos como Flow
     */
    fun buscarCiclosPorRotaFlow(rotaId: Long): Flow<List<CicloAcertoEntity>> {
        return cicloAcertoDao.listarPorRota(rotaId)
    }

    /**
     * ✅ NOVO: Lista ciclos de uma rota a partir de uma data inicial.
     */
    fun buscarCiclosPorRotaAposData(rotaId: Long, dataInicio: Long): Flow<List<CicloAcertoEntity>> {
        return cicloAcertoDao.listarPorRotaAposData(rotaId, dataInicio)
    }

    /**
     * ✅ NOVO: Lista ciclos de uma rota por período (data início/fim).
     */
    fun buscarCiclosPorRotaPeriodo(rotaId: Long, dataInicio: Long, dataFim: Long): Flow<List<CicloAcertoEntity>> {
        return cicloAcertoDao.listarPorRotaPeriodo(rotaId, dataInicio, dataFim)
    }

    /**
     * ✅ NOVO: Lista todos os ciclos existentes (todas as rotas)
     */
    fun listarTodosCiclos(): Flow<List<CicloAcertoEntity>> {
        return cicloAcertoDao.listarTodos()
    }

    /**
     * Busca ciclos por status
     */
    suspend fun buscarCiclosPorStatus(
        rotaId: Long,
        status: StatusCicloAcerto
    ): List<CicloAcertoEntity> {
        return cicloAcertoDao.listarPorRota(rotaId).first().filter { it.status == status }
    }

    /**
     * Finaliza um ciclo
     */
    suspend fun finalizarCiclo(cicloId: Long, dataFim: Long) {
        val ciclo = buscarCicloPorId(cicloId)
        ciclo?.let {
            val rotaId = it.rotaId
            // Realiza o cálculo final de todos os dados agregados
            val acertos = acertoRepository.buscarPorCicloId(cicloId).first()
            
            // ✅ CORRIGIDO: Buscar despesas por cicloId
            val despesas = despesaDao.buscarPorCicloId(cicloId).first()
            
            val clientes = clienteRepository.obterClientesPorRota(rotaId).first()

            val valorTotalAcertado: Double = acertos.sumOf { a -> a.valorRecebido }
            val valorTotalDespesas: Double = despesas.sumOf { d -> d.valor }
            val lucroLiquido: Double = valorTotalAcertado - valorTotalDespesas
            val clientesAcertados = acertos.map { a -> a.clienteId }.distinct().size
            val totalClientes = clientes.size
            val debitoTotal: Double = clientes.sumOf { c -> c.debitoAtual }

            // Cria a versão final e imutável do ciclo
            val cicloFinalizado = it.copy(
                dataFim = dataFim,
                status = StatusCicloAcerto.FINALIZADO,
                valorTotalAcertado = valorTotalAcertado,
                valorTotalDespesas = valorTotalDespesas,
                lucroLiquido = lucroLiquido,
                clientesAcertados = clientesAcertados,
                totalClientes = totalClientes,
                debitoTotal = debitoTotal // Salva o valor "congelado"
            )
            cicloAcertoDao.atualizar(cicloFinalizado)
            Timber.d("Ciclo $cicloId finalizado com dados completos salvos.")
            
            // ✅ NOVO: Finalizar todas as metas associadas a este ciclo
            Timber.d("🔄 Iniciando finalização de metas para o ciclo $cicloId")
            finalizarMetasDoCiclo(cicloId)
            Timber.d("✅ Finalização de metas concluída para o ciclo $cicloId")
        }
    }
    
    /**
     * Finaliza todas as metas associadas a um ciclo (marca como inativas)
     */
    private suspend fun finalizarMetasDoCiclo(cicloId: Long) {
        try {
            Timber.d("🔍 Verificando ColaboradorDao para ciclo $cicloId")
            if (colaboradorDao == null) {
                Timber.e("❌ ColaboradorDao não disponível, pulando finalização de metas")
                return
            }
            
            Timber.d("✅ ColaboradorDao disponível, buscando metas do ciclo $cicloId")
            // Buscar todas as metas ativas do ciclo
            val metas = colaboradorDao.buscarTodasMetasPorCiclo(cicloId)
            Timber.d("📊 Encontradas ${metas.size} metas ativas para o ciclo $cicloId")
            
            if (metas.isNotEmpty()) {
                Timber.d("🔄 Finalizando ${metas.size} metas do ciclo $cicloId")
                
                // Marcar todas as metas como inativas (finalizadas)
                var sucessoCount = 0
                var erroCount = 0
                
                metas.forEachIndexed { index, meta ->
                    try {
                        Timber.d("  - Finalizando meta ${index + 1}/${metas.size}: ID=${meta.id}, Tipo=${meta.tipoMeta}, RotaId=${meta.rotaId}, Ativo=${meta.ativo}")
                        
                        // Criar cópia com ativo = false
                        val metaFinalizada = meta.copy(ativo = false)
                        
                        // Atualizar no banco
                        colaboradorDao.atualizarMeta(metaFinalizada)
                        
                        // Verificar se foi atualizado corretamente (read-your-writes)
                        // Nota: buscarTodasMetasPorCiclo só retorna metas ativas, então não podemos usar para verificar
                        // A atualização foi feita, então assumimos sucesso
                        Timber.d("  ✅ Meta ${meta.id} finalizada (ativo = false)")
                        sucessoCount++
                    } catch (e: Exception) {
                        Timber.e(e, "  ❌ Erro ao finalizar meta ${meta.id}: ${e.message}")
                        erroCount++
                    }
                }
                
                Timber.d("✅ Finalização concluída: $sucessoCount sucesso, $erroCount erros de ${metas.size} metas")
            } else {
                Timber.w("⚠️ Nenhuma meta encontrada para o ciclo $cicloId")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Erro ao finalizar metas do ciclo $cicloId: ${e.message}")
        }
    }

    /**
     * Cancela um ciclo
     */
    suspend fun cancelarCiclo(cicloId: Long) {
        atualizarStatusCiclo(cicloId, StatusCicloAcerto.CANCELADO)
    }

    /**
     * Busca o próximo número de ciclo para uma rota e ano
     */
    suspend fun buscarProximoNumeroCiclo(rotaId: Long, ano: Int): Int {
        return cicloAcertoDao.buscarProximoNumeroCiclo(rotaId, ano)
    }

    /**
     * Observa o ciclo ativo de uma rota como Flow.
     */
    fun observarCicloAtivo(rotaId: Long): Flow<CicloAcertoEntity?> {
        return cicloAcertoDao.observarCicloEmAndamento(rotaId)
    }

    suspend fun iniciarNovoCiclo(rotaId: Long): Long {
        Timber.d("Iniciando novo ciclo para rotaId: $rotaId")
        return withContext(Dispatchers.IO) {
            val cicloAnterior = cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)
            if (cicloAnterior != null && cicloAnterior.status == StatusCicloAcerto.EM_ANDAMENTO) {
                Timber.d("AVISO: Tentativa de iniciar novo ciclo, mas o ciclo ${cicloAnterior.id} já está em andamento.")
                return@withContext cicloAnterior.id
            }

            val calendar = Calendar.getInstance()
            val anoAtual = calendar.get(Calendar.YEAR)
            val proximoNumeroCiclo = cicloAcertoDao.buscarProximoNumeroCiclo(rotaId, anoAtual)
            Timber.d("Novo número de ciclo calculado: $proximoNumeroCiclo para o ano $anoAtual")
            val novoCiclo = CicloAcertoEntity(
                rotaId = rotaId,
                numeroCiclo = proximoNumeroCiclo,
                ano = anoAtual,
                dataInicio = System.currentTimeMillis(),
                dataFim = System.currentTimeMillis(), // Será atualizado ao finalizar
                status = StatusCicloAcerto.EM_ANDAMENTO
            )
            val id = cicloAcertoDao.inserir(novoCiclo)
            Timber.d("Novo ciclo inserido com sucesso! ID: $id")
            id
        }
    }

    suspend fun getNumeroCicloAtual(rotaId: Long): Int {
        Timber.d("Buscando número do ciclo atual para rotaId: $rotaId")
        val ciclo = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
        val numeroCiclo = ciclo?.numeroCiclo ?: 1 // Se não houver ciclo, assume-se que é o primeiro
        Timber.d("Número do ciclo encontrado: $numeroCiclo (Ciclo ID: ${ciclo?.id ?: "Nenhum"})")
        return numeroCiclo
    }

    suspend fun finalizarCicloAtual(rotaId: Long) {
        Timber.d("Tentando finalizar ciclo atual para rotaId: $rotaId")
        withContext(Dispatchers.IO) {
            val cicloAtual = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
            if (cicloAtual != null) {
                val cicloFinalizado = cicloAtual.copy(
                    dataFim = System.currentTimeMillis(),
                    status = StatusCicloAcerto.FINALIZADO
                )
                cicloAcertoDao.atualizar(cicloFinalizado)
                Timber.d("Ciclo ${cicloAtual.id} finalizado com sucesso.")
            } else {
                Timber.d("AVISO: Nenhum ciclo em andamento encontrado para finalizar na rota $rotaId.")
            }
        }
    }

    // NOVO: Buscar despesas por cicloId
    suspend fun buscarDespesasPorCicloId(cicloId: Long): List<com.example.gestaobilhares.data.entities.Despesa> {
        return despesaDao.buscarPorCicloId(cicloId).first()
    }

    // NOVO: Buscar clientes de uma rota
    suspend fun buscarClientesPorRota(rotaId: Long): List<com.example.gestaobilhares.data.entities.Cliente> {
        return clienteRepository.obterClientesPorRota(rotaId).first()
    }

    // NOVO: Buscar acertos de uma rota e ciclo
    suspend fun buscarAcertosPorRotaECiclo(rotaId: Long, cicloId: Long): List<com.example.gestaobilhares.data.entities.Acerto> {
        return acertoRepository.buscarPorRotaECicloId(rotaId, cicloId).first()
    }

    /**
     * ✅ NOVO: Busca rota por ID para relatório
     */
    suspend fun buscarRotaPorId(rotaId: Long): com.example.gestaobilhares.data.entities.Rota? {
        return try {
            rotaDao?.getRotaById(rotaId)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar rota: ${e.message}")
            null
        }
    }

    /**
     * ✅ NOVO: Busca acertos por ciclo para relatório
     */
    suspend fun buscarAcertosPorCiclo(cicloId: Long): List<com.example.gestaobilhares.data.entities.Acerto> {
        return try {
            acertoRepository.buscarPorCicloId(cicloId).first()
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar acertos: ${e.message}")
            emptyList()
        }
    }

    /**
     * ✅ NOVO: Busca despesas por ciclo para relatório
     */
    suspend fun buscarDespesasPorCiclo(cicloId: Long): List<com.example.gestaobilhares.data.entities.Despesa> {
        return try {
            despesaDao.buscarPorCicloId(cicloId).first()
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar despesas: ${e.message}")
            emptyList()
        }
    }
} 
