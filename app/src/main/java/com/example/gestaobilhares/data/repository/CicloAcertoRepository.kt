package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.CicloAcertoDao
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Calendar
import com.example.gestaobilhares.data.repository.ClienteRepository
import java.time.ZoneId
/**
 * Repositório para operações de ciclos de acerto
 * ✅ FASE 9C: REPOSITÓRIO PARA HISTÓRICO DE CICLOS
 */
class CicloAcertoRepository constructor(
    private val cicloAcertoDao: CicloAcertoDao,
    private val despesaRepository: DespesaRepository,
    private val acertoRepository: com.example.gestaobilhares.data.repository.AcertoRepository,
    private val clienteRepository: ClienteRepository, // NOVO
    private val rotaDao: com.example.gestaobilhares.data.dao.RotaDao? = null, // NOVO: Para relatórios
    private val appRepository: AppRepository // NOVO: Para sincronização
) {

    /**
     * Busca todos os ciclos de uma rota
     */
    suspend fun buscarCiclosPorRota(rotaId: Long): List<CicloAcertoEntity> {
        val ciclos = cicloAcertoDao.listarPorRota(rotaId).first()
        com.example.gestaobilhares.utils.AppLogger.log(
            "CicloAcertoRepo",
            "Buscando ciclos da rota $rotaId: encontrados ${ciclos.size} ciclos. Dados: " +
                    ciclos.joinToString(" | ") { c -> "id=${c.id}, total=${c.valorTotalAcertado}, despesas=${c.valorTotalDespesas}, lucro=${c.lucroLiquido}, clientes=${c.clientesAcertados}" }
        )
        return ciclos
    }

    /**
     * Busca ciclos por período
     */
    suspend fun buscarCiclosPorPeriodo(
        _rotaId: Long,
        dataInicio: Date,
        dataFim: Date
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
        return appRepository.inserirCicloAcerto(ciclo)
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
            com.example.gestaobilhares.utils.AppLogger.log(
                "CicloAcertoRepo",
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
            val despesas = despesaRepository.buscarPorCicloId(cicloId).first()
            val valorTotalAcertado = acertos.sumOf { it.valorRecebido }
            val valorTotalDespesas = despesas.sumOf { it.valor }
            val clientesAcertados = acertos.map { it.clienteId }.distinct().size
            val cicloAtualizado = it.copy(
                valorTotalAcertado = valorTotalAcertado,
                valorTotalDespesas = valorTotalDespesas,
                clientesAcertados = clientesAcertados,
                lucroLiquido = valorTotalAcertado - valorTotalDespesas
            )
            cicloAcertoDao.atualizar(cicloAtualizado)
            com.example.gestaobilhares.utils.AppLogger.log(
                "CicloAcertoRepo",
                "[RECALCULO] Ciclo $cicloId atualizado: arrecadado=$valorTotalAcertado, despesas=$valorTotalDespesas, clientes=$clientesAcertados, lucro=${valorTotalAcertado - valorTotalDespesas}"
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
    suspend fun finalizarCiclo(cicloId: Long, dataFim: Date) {
        val ciclo = buscarCicloPorId(cicloId)
        ciclo?.let {
            val rotaId = it.rotaId
            // Realiza o cálculo final de todos os dados agregados
            val acertos = acertoRepository.buscarPorCicloId(cicloId).first()
            
            // ✅ CORRIGIDO: Buscar despesas por cicloId
            val despesas = despesaRepository.buscarPorCicloId(cicloId).first()
            
            val clientes = clienteRepository.obterClientesPorRota(rotaId).first()

            val valorTotalAcertado = acertos.sumOf { a -> a.valorRecebido }
            val valorTotalDespesas = despesas.sumOf { d -> d.valor }
            val lucroLiquido = valorTotalAcertado - valorTotalDespesas
            val clientesAcertados = acertos.map { a -> a.clienteId }.distinct().size
            val totalClientes = clientes.size
            val debitoTotal = clientes.sumOf { c -> c.debitoAtual }

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
            AppLogger.log("CicloAcertoRepo", "Ciclo $cicloId finalizado com dados completos salvos.")
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
        AppLogger.log("CicloAcertoRepo", "Iniciando novo ciclo para rotaId: $rotaId")
        return withContext(Dispatchers.IO) {
            val cicloAnterior = cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)
            if (cicloAnterior != null && cicloAnterior.status == StatusCicloAcerto.EM_ANDAMENTO) {
                AppLogger.log("CicloAcertoRepo", "AVISO: Tentativa de iniciar novo ciclo, mas o ciclo ${cicloAnterior.id} já está em andamento.")
                return@withContext cicloAnterior.id
            }

            val calendar = Calendar.getInstance()
            val anoAtual = calendar.get(Calendar.YEAR)
            val proximoNumeroCiclo = cicloAcertoDao.buscarProximoNumeroCiclo(rotaId, anoAtual)
            AppLogger.log("CicloAcertoRepo", "Novo número de ciclo calculado: $proximoNumeroCiclo para o ano $anoAtual")
            val novoCiclo = CicloAcertoEntity(
                rotaId = rotaId,
                numeroCiclo = proximoNumeroCiclo,
                ano = anoAtual,
                dataInicio = Date(),
                dataFim = Date(), // Será atualizado ao finalizar
                status = StatusCicloAcerto.EM_ANDAMENTO
            )
            val id = appRepository.inserirCicloAcerto(novoCiclo)
            AppLogger.log("CicloAcertoRepo", "Novo ciclo inserido com sucesso! ID: $id")
            id
        }
    }

    suspend fun getNumeroCicloAtual(rotaId: Long): Int {
        AppLogger.log("CicloAcertoRepo", "Buscando número do ciclo atual para rotaId: $rotaId")
        val ciclo = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
        val numeroCiclo = ciclo?.numeroCiclo ?: 1 // Se não houver ciclo, assume-se que é o primeiro
        AppLogger.log("CicloAcertoRepo", "Número do ciclo encontrado: $numeroCiclo (Ciclo ID: ${ciclo?.id ?: "Nenhum"})")
        return numeroCiclo
    }

    suspend fun finalizarCicloAtual(rotaId: Long) {
        AppLogger.log("CicloAcertoRepo", "Tentando finalizar ciclo atual para rotaId: $rotaId")
        withContext(Dispatchers.IO) {
            val cicloAtual = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
            if (cicloAtual != null) {
                val cicloFinalizado = cicloAtual.copy(
                    dataFim = Date(),
                    status = StatusCicloAcerto.FINALIZADO
                )
                cicloAcertoDao.atualizar(cicloFinalizado)
                AppLogger.log("CicloAcertoRepo", "Ciclo ${cicloAtual.id} finalizado com sucesso.")
            } else {
                AppLogger.log("CicloAcertoRepo", "AVISO: Nenhum ciclo em andamento encontrado para finalizar na rota $rotaId.")
            }
        }
    }

    // NOVO: Buscar despesas por cicloId
    suspend fun buscarDespesasPorCicloId(cicloId: Long): List<com.example.gestaobilhares.data.entities.Despesa> {
        return despesaRepository.buscarPorCicloId(cicloId).first()
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
            android.util.Log.e("CicloAcertoRepository", "Erro ao buscar rota: ${e.message}")
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
            android.util.Log.e("CicloAcertoRepository", "Erro ao buscar acertos: ${e.message}")
            emptyList()
        }
    }

    /**
     * ✅ NOVO: Busca despesas por ciclo para relatório
     */
    suspend fun buscarDespesasPorCiclo(cicloId: Long): List<com.example.gestaobilhares.data.entities.Despesa> {
        return try {
            despesaRepository.buscarPorCicloId(cicloId).first()
        } catch (e: Exception) {
            android.util.Log.e("CicloAcertoRepository", "Erro ao buscar despesas: ${e.message}")
            emptyList()
        }
    }
} 
