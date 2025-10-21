package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import com.example.gestaobilhares.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
// ✅ REMOVIDO: Hilt não é mais usado

/**
 * ✅ REPOSITORY CONSOLIDADO E MODERNIZADO - AppRepository
 * 
 * FASE 2: Modernização com StateFlow e centralização
 * - Combina todos os repositories em um único arquivo
 * - Elimina duplicação e simplifica a arquitetura
 * - Modernizado com StateFlow para melhor performance
 * - Centralizado para facilitar manutenção
 */
class AppRepository constructor(
    private val clienteDao: ClienteDao,
    private val acertoDao: AcertoDao,
    private val mesaDao: MesaDao,
    private val rotaDao: RotaDao,
    private val despesaDao: DespesaDao,
    private val colaboradorDao: ColaboradorDao,
    private val cicloAcertoDao: CicloAcertoDao,
    private val acertoMesaDao: com.example.gestaobilhares.data.dao.AcertoMesaDao,
    private val contratoLocacaoDao: ContratoLocacaoDao,
    private val aditivoContratoDao: AditivoContratoDao,
    private val assinaturaRepresentanteLegalDao: AssinaturaRepresentanteLegalDao,
    private val logAuditoriaAssinaturaDao: LogAuditoriaAssinaturaDao,
    private val panoEstoqueDao: com.example.gestaobilhares.data.dao.PanoEstoqueDao,
    private val categoriaDespesaDao: CategoriaDespesaDao,
    private val tipoDespesaDao: TipoDespesaDao,
    private val historicoManutencaoVeiculoDao: HistoricoManutencaoVeiculoDao,
    private val historicoCombustivelVeiculoDao: HistoricoCombustivelVeiculoDao,
    private val historicoManutencaoMesaDao: HistoricoManutencaoMesaDao
) {
    // ==================== CATEGORIAS E TIPOS DE DESPESA ====================
    fun buscarCategoriasAtivas() = categoriaDespesaDao.buscarAtivas()
    suspend fun buscarCategoriaPorNome(nome: String) = categoriaDespesaDao.buscarPorNome(nome)
    suspend fun categoriaExiste(nome: String): Boolean = categoriaDespesaDao.contarPorNome(nome) > 0
    suspend fun criarCategoria(nova: NovaCategoriaDespesa): Long {
        val entity = CategoriaDespesa(
            nome = nova.nome,
            descricao = nova.descricao,
            criadoPor = nova.criadoPor
        )
        return categoriaDespesaDao.inserir(entity)
    }
    fun buscarTiposPorCategoria(categoriaId: Long) = tipoDespesaDao.buscarPorCategoria(categoriaId)
    suspend fun buscarTipoPorNome(nome: String) = tipoDespesaDao.buscarPorNome(nome)
    suspend fun tipoExiste(nome: String, categoriaId: Long): Boolean {
        val tipo = tipoDespesaDao.buscarPorNome(nome)
        return tipo != null && tipo.categoriaId == categoriaId
    }
    suspend fun criarTipo(novo: NovoTipoDespesa): Long {
        val entity = TipoDespesa(
            categoriaId = novo.categoriaId,
            nome = novo.nome,
            descricao = novo.descricao,
            criadoPor = novo.criadoPor
        )
        return tipoDespesaDao.inserir(entity)
    }

    
    // ==================== STATEFLOW CACHE (MODERNIZAÇÃO 2025) ====================
    
    /**
     * Cache de clientes para performance
     */
    private val _clientesCache = MutableStateFlow<List<Cliente>>(emptyList())
    val clientesCache: StateFlow<List<Cliente>> = _clientesCache.asStateFlow()
    
    /**
     * Cache de rotas para performance
     */
    private val _rotasCache = MutableStateFlow<List<Rota>>(emptyList())
    val rotasCache: StateFlow<List<Rota>> = _rotasCache.asStateFlow()
    
    /**
     * Cache de mesas para performance
     */
    private val _mesasCache = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCache: StateFlow<List<Mesa>> = _mesasCache.asStateFlow()
    
    // ==================== CLIENTE ====================
    
    /**
     * ✅ MODERNIZADO: Obtém todos os clientes com cache StateFlow
     */
    fun obterTodosClientes(): Flow<List<Cliente>> = clienteDao.obterTodos()
    
    /**
     * ✅ MODERNIZADO: Obtém clientes por rota com cache
     */
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> = clienteDao.obterClientesPorRota(rotaId)
    suspend fun obterClientePorId(id: Long) = clienteDao.obterPorId(id)
    suspend fun inserirCliente(cliente: Cliente): Long {
        logDbInsertStart("CLIENTE", "Nome=${cliente.nome}, RotaID=${cliente.rotaId}")
        return try {
            val id = clienteDao.inserir(cliente)
            logDbInsertSuccess("CLIENTE", "Nome=${cliente.nome}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("CLIENTE", "Nome=${cliente.nome}", e)
            throw e
        }
    }
    suspend fun atualizarCliente(cliente: Cliente) = clienteDao.atualizar(cliente)
    suspend fun deletarCliente(cliente: Cliente) = clienteDao.deletar(cliente)
    suspend fun atualizarDebitoAtual(clienteId: Long, novoDebito: Double) = 
        clienteDao.atualizarDebitoAtual(clienteId, novoDebito)
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long) = 
        clienteDao.calcularDebitoAtualEmTempoReal(clienteId)
    suspend fun obterClienteComDebitoAtual(clienteId: Long) = 
        clienteDao.obterClienteComDebitoAtual(clienteId)
    
    /**
     * ✅ NOVO: Busca o ID da rota associada a um cliente
     */
                suspend fun buscarRotaIdPorCliente(clienteId: Long): Long? {
                    return try {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("AppRepository", "Buscando cliente ID: $clienteId")
                        }
                        val cliente = obterClientePorId(clienteId)
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("AppRepository", "Cliente encontrado: ${cliente?.nome}, rotaId: ${cliente?.rotaId}")
                        }
                        cliente?.rotaId
                    } catch (e: Exception) {
                        android.util.Log.e("AppRepository", "Erro ao buscar rota ID por cliente: ${e.message}", e)
                        null
                    }
                }
    
    // ==================== ACERTO ====================
    
    fun obterAcertosPorCliente(clienteId: Long) = acertoDao.buscarPorCliente(clienteId)
    suspend fun obterAcertoPorId(id: Long) = acertoDao.buscarPorId(id)
    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long) = 
        acertoDao.buscarUltimoAcertoPorCliente(clienteId)
    fun obterTodosAcertos() = acertoDao.listarTodos()
    fun buscarAcertosPorCicloId(cicloId: Long) = acertoDao.buscarPorCicloId(cicloId)
    fun buscarClientesPorRota(rotaId: Long) = clienteDao.obterClientesPorRota(rotaId)
    suspend fun buscarRotaPorId(rotaId: Long) = rotaDao.getRotaById(rotaId)
    suspend fun inserirAcerto(acerto: Acerto): Long {
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
    suspend fun atualizarAcerto(acerto: Acerto) = acertoDao.atualizar(acerto)
    suspend fun deletarAcerto(acerto: Acerto) = acertoDao.deletar(acerto)
    suspend fun buscarUltimoAcertoPorMesa(mesaId: Long) = 
        acertoDao.buscarUltimoAcertoPorMesa(mesaId)
    suspend fun buscarUltimoAcertoMesaItem(mesaId: Long) =
        acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long) = 
        acertoDao.buscarObservacaoUltimoAcerto(clienteId)
    suspend fun buscarUltimosAcertosPorClientes(clienteIds: List<Long>) =
        acertoDao.buscarUltimosAcertosPorClientes(clienteIds)
    suspend fun buscarCicloAtivo(rotaId: Long) = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = acertoDao.buscarPorRotaECicloId(rotaId, cicloId)
    suspend fun buscarAcertoMesaPorMesa(mesaId: Long) = acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    suspend fun salvarAcerto(acerto: Acerto): Long = acertoDao.inserir(acerto)
    suspend fun buscarPorId(id: Long) = acertoDao.buscarPorId(id)
    suspend fun atualizarValoresCiclo(
        cicloId: Long,
        valorTotalAcertado: Double,
        valorTotalDespesas: Double,
        clientesAcertados: Int
    ) = cicloAcertoDao.atualizarValoresCiclo(cicloId, valorTotalAcertado, valorTotalDespesas, clientesAcertados)
    suspend fun obterPanoPorId(id: Long) = panoEstoqueDao.buscarPorId(id)
    suspend fun marcarPanoComoUsadoPorNumero(numeroPano: String, motivo: String) {
        val pano = panoEstoqueDao.buscarPorNumero(numeroPano)
        if (pano != null) {
            panoEstoqueDao.atualizarDisponibilidade(pano.id, false)
        }
    }
    suspend fun buscarPorNumero(numeroPano: String) = panoEstoqueDao.buscarPorNumero(numeroPano)
    suspend fun marcarPanoComoUsado(panoId: Long, motivo: String) = panoEstoqueDao.atualizarDisponibilidade(panoId, false)
    
    // ==================== MESA ====================
    
    suspend fun obterMesaPorId(id: Long) = mesaDao.obterMesaPorId(id)
    fun obterMesasPorCliente(clienteId: Long) = mesaDao.obterMesasPorCliente(clienteId)
    fun obterMesasDisponiveis() = mesaDao.obterMesasDisponiveis()
    suspend fun inserirMesa(mesa: Mesa): Long {
        logDbInsertStart("MESA", "Numero=${mesa.numero}, ClienteID=${mesa.clienteId}")
        return try {
            val id = mesaDao.inserir(mesa)
            logDbInsertSuccess("MESA", "Numero=${mesa.numero}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("MESA", "Numero=${mesa.numero}", e)
            throw e
        }
    }
    suspend fun atualizarMesa(mesa: Mesa) = mesaDao.atualizar(mesa)
    suspend fun deletarMesa(mesa: Mesa) = mesaDao.deletar(mesa)
    suspend fun vincularMesaACliente(mesaId: Long, clienteId: Long) = 
        mesaDao.vincularMesa(mesaId, clienteId)
    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) = 
        mesaDao.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
    suspend fun desvincularMesaDeCliente(mesaId: Long) = mesaDao.desvincularMesa(mesaId)
    suspend fun retirarMesa(mesaId: Long) = mesaDao.retirarMesa(mesaId)
    suspend fun atualizarRelogioMesa(mesaId: Long, relogioInicial: Int, relogioFinal: Int, fichasInicial: Int, fichasFinal: Int) = 
        mesaDao.atualizarRelogioMesa(mesaId, relogioInicial, relogioFinal, fichasInicial, fichasFinal)
    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = 
        mesaDao.atualizarRelogioFinal(mesaId, relogioFinal)
    suspend fun obterMesasPorClienteDireto(clienteId: Long) = 
        mesaDao.obterMesasPorClienteDireto(clienteId)
    fun buscarMesasPorRota(rotaId: Long) = mesaDao.buscarMesasPorRota(rotaId).also {
        android.util.Log.d("AppRepository", "Buscando mesas para rota $rotaId")
    }
    suspend fun contarMesasAtivasPorClientes(clienteIds: List<Long>) =
        mesaDao.contarMesasAtivasPorClientes(clienteIds)
    fun obterTodasMesas() = mesaDao.obterTodasMesas()
    
    // ==================== ROTA ====================
    
    fun obterTodasRotas() = rotaDao.getAllRotas()
    fun obterRotasAtivas() = rotaDao.getAllRotasAtivas()
    
    // ✅ NOVO: Método para obter resumo de rotas com atualização em tempo real
    fun getRotasResumoComAtualizacaoTempoReal(): Flow<List<RotaResumo>> {
        return rotaDao.getAllRotasAtivas().map { rotas ->
            rotas.map { rota ->
                // Usar dados reais calculados
                val clientesAtivos = calcularClientesAtivosSync(rota.id)
                val pendencias = calcularPendenciasSync(rota.id)
                val valorAcertado = calcularValorAcertadoSync(rota.id)
                val quantidadeMesas = calcularQuantidadeMesasSync(rota.id)
                val percentualAcertados = calcularPercentualAcertadosSync(rota.id, clientesAtivos)
                
                // ✅ ATUALIZAÇÃO EM TEMPO REAL: Status baseado no estado atual dos ciclos
                val status = determinarStatusRotaEmTempoReal(rota.id)
                
                // ✅ CORREÇÃO: Usar ciclo atual real baseado nos acertos
                val cicloAtual = calcularCicloAtualReal(rota.id)
                val dataCiclo = obterDataCicloAtual(rota.id)
                
                // ✅ NOVO: Obter datas de início e fim do ciclo
                val (dataInicio, dataFim) = obterDatasCicloRota(rota.id)
                
                RotaResumo(
                    rota = rota,
                    clientesAtivos = clientesAtivos,
                    pendencias = pendencias,
                    valorAcertado = valorAcertado,
                    quantidadeMesas = quantidadeMesas,
                    percentualAcertados = percentualAcertados,
                    status = status,
                    cicloAtual = cicloAtual,
                    dataInicioCiclo = dataInicio,  // ✅ NOVO: Data de início
                    dataFimCiclo = dataFim        // ✅ NOVO: Data de fim
                )
            }
        }
    }
    
    // ✅ NOVO: Métodos auxiliares para calcular dados reais das rotas (versões sync)
    private fun calcularClientesAtivosSync(rotaId: Long): Int {
        return try {
            runBlocking { clienteDao.obterClientesPorRota(rotaId).first().count { it.ativo } }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular clientes ativos da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularPendenciasSync(rotaId: Long): Int {
        return try {
            runBlocking { calcularPendenciasReaisPorRota(rotaId) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular pendências da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularValorAcertadoSync(rotaId: Long): Double {
        return try {
            runBlocking { calcularValorAcertadoPorRotaECiclo(rotaId, obterCicloAtualIdPorRota(rotaId)) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular valor acertado da rota $rotaId: ${e.message}")
            0.0
        }
    }
    
    private fun calcularQuantidadeMesasSync(rotaId: Long): Int {
        return try {
            runBlocking { calcularQuantidadeMesasPorRota(rotaId) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular quantidade de mesas da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularPercentualAcertadosSync(rotaId: Long, clientesAtivos: Int): Int {
        return try {
            runBlocking { calcularPercentualClientesAcertados(rotaId, obterCicloAtualIdPorRota(rotaId), clientesAtivos) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular percentual de clientes acertados da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularCicloAtualReal(rotaId: Long): Int {
        return try {
            runBlocking { obterCicloAtualRota(rotaId).first }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular ciclo atual da rota $rotaId: ${e.message}")
            1
        }
    }
    
    private fun obterDataCicloAtual(rotaId: Long): Long? {
        return try {
            runBlocking { obterCicloAtualRota(rotaId).third }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter data do ciclo atual da rota $rotaId: ${e.message}")
            null
        }
    }
    
    private suspend fun calcularClientesAtivosPorRota(rotaId: Long): Int {
        return try {
            clienteDao.obterClientesPorRota(rotaId).first().count { it.ativo }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular clientes ativos da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularPendenciasReaisPorRota(rotaId: Long): Int {
        return try {
            val clientes = clienteDao.obterClientesPorRota(rotaId).first()
            if (clientes.isEmpty()) return 0
            val clienteIds = clientes.map { it.id }
            val ultimos = buscarUltimosAcertosPorClientes(clienteIds)
            val ultimoPorCliente = ultimos.associateBy({ it.clienteId }, { it.dataAcerto })
            val agora = java.util.Calendar.getInstance()
            clientes.count { cliente ->
                val debitoAlto = cliente.debitoAtual > 400
                val dataUltimo = ultimoPorCliente[cliente.id]
                val semAcerto4Meses = if (dataUltimo == null) {
                    true
                } else {
                    val cal = java.util.Calendar.getInstance(); cal.time = dataUltimo
                    val anos = agora.get(java.util.Calendar.YEAR) - cal.get(java.util.Calendar.YEAR)
                    val meses = anos * 12 + (agora.get(java.util.Calendar.MONTH) - cal.get(java.util.Calendar.MONTH))
                    meses >= 4
                }
                debitoAlto || semAcerto4Meses
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular pendências reais da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularValorAcertadoPorRotaECiclo(rotaId: Long, cicloId: Long?): Double {
        return try {
            if (cicloId == null) return 0.0
            buscarAcertosPorCicloId(cicloId).first().filter { it.rotaId == rotaId }.sumOf { it.valorRecebido }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular valor acertado da rota $rotaId: ${e.message}")
            0.0
        }
    }
    
    private suspend fun calcularQuantidadeMesasPorRota(rotaId: Long): Int {
        return try {
            mesaDao.buscarMesasPorRota(rotaId).first().size
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular quantidade de mesas da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private suspend fun calcularPercentualClientesAcertados(rotaId: Long, cicloId: Long?, clientesAtivos: Int): Int {
        return try {
            if (cicloId == null || clientesAtivos == 0) return 0
            val acertos = buscarAcertosPorCicloId(cicloId).first().filter { it.rotaId == rotaId }
            val distintos = acertos.map { it.clienteId }.distinct().size
            ((distintos.toDouble() / clientesAtivos.toDouble()) * 100).toInt()
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular percentual de clientes acertados da rota $rotaId: ${e.message}")
            0
        }
    }

    private suspend fun obterCicloAtualRota(rotaId: Long): Triple<Int, Long?, Long?> {
        return try {
            android.util.Log.d("AppRepository", "🔍 CALCULANDO CICLO ATUAL REAL para rota $rotaId")
            val cicloAtual = cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
            android.util.Log.d(
                "AppRepository",
                "🔍 Ciclo atual encontrado: ${cicloAtual?.let { "ID=${it.id}, Numero=${it.numeroCiclo}, Status=${it.status}" } ?: "null"}"
            )
            if (cicloAtual != null) {
                if (cicloAtual.status == StatusCicloAcerto.EM_ANDAMENTO) {
                    android.util.Log.d("AppRepository", "✅ Usando ciclo EM_ANDAMENTO: ${cicloAtual.numeroCiclo}")
                    Triple(cicloAtual.numeroCiclo, cicloAtual.id, cicloAtual.dataInicio.time)
                } else {
                    // ✅ CORREÇÃO: Retornar o ciclo finalizado, não o próximo
                    android.util.Log.d("AppRepository", "✅ Usando ciclo FINALIZADO: ${cicloAtual.numeroCiclo}")
                    Triple(cicloAtual.numeroCiclo, cicloAtual.id, cicloAtual.dataFim?.time)
                }
            } else {
                android.util.Log.d("AppRepository", "✅ Primeiro ciclo: 1")
                Triple(1, null, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter ciclo atual da rota $rotaId: ${e.message}")
            Triple(1, null, null)
        }
    }

    /**
     * Exposição pública do ciclo atual da rota no formato usado antes da refatoração.
     * Retorna apenas o ID do ciclo para vínculo de acertos.
     */
    suspend fun obterCicloAtualIdPorRota(rotaId: Long): Long? {
        return try {
            val cicloAtual = cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
            if (cicloAtual != null) {
                cicloAtual.id
            } else {
                val rota = rotaDao.getRotaById(rotaId)
                if (rota != null && rota.cicloAcertoAtual != 0 && rota.anoCiclo != 0) {
                    val cicloDaRota = cicloAcertoDao.buscarPorRotaNumeroEAno(rotaId, rota.cicloAcertoAtual, rota.anoCiclo)
                    cicloDaRota?.id
                } else {
                    val (_, cicloId, _) = obterCicloAtualRota(rotaId)
                    cicloId
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter ID do ciclo atual para rota $rotaId: ${e.message}", e)
            null
        }
    }

    // ✅ NOVO: Método para obter datas de início e fim do ciclo
    private suspend fun obterDatasCicloRota(rotaId: Long): Pair<Long?, Long?> {
        return try {
            val ciclo = cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
            if (ciclo != null) {
                Pair(ciclo.dataInicio.time, ciclo.dataFim?.time)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter datas do ciclo da rota $rotaId: ${e.message}")
            Pair(null, null)
        }
    }

    private suspend fun determinarStatusRotaEmTempoReal(rotaId: Long): StatusRota {
        return try {
            val emAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
            if (emAndamento != null) {
                StatusRota.EM_ANDAMENTO
            } else {
                StatusRota.FINALIZADA
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao determinar status da rota $rotaId: ${e.message}")
            StatusRota.PAUSADA
        }
    }
    
    suspend fun obterRotaPorId(id: Long) = rotaDao.getRotaById(id)
    fun obterRotaPorIdFlow(id: Long) = rotaDao.obterRotaPorId(id)
    suspend fun obterRotaPorNome(nome: String) = rotaDao.getRotaByNome(nome)
    suspend fun inserirRota(rota: Rota): Long {
        logDbInsertStart("ROTA", "Nome=${rota.nome}")
        return try {
            val id = rotaDao.insertRota(rota)
            logDbInsertSuccess("ROTA", "Nome=${rota.nome}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("ROTA", "Nome=${rota.nome}", e)
            throw e
        }
    }
    suspend fun inserirRotas(rotas: List<Rota>): List<Long> {
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
    suspend fun atualizarRota(rota: Rota) = rotaDao.updateRota(rota)
    suspend fun atualizarRotas(rotas: List<Rota>) = rotaDao.updateRotas(rotas)
    suspend fun deletarRota(rota: Rota) = rotaDao.deleteRota(rota)
    suspend fun desativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.desativarRota(rotaId, timestamp)
    suspend fun ativarRota(rotaId: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.ativarRota(rotaId, timestamp)
    suspend fun atualizarStatus(rotaId: Long, status: String, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.atualizarStatus(rotaId, status, timestamp)
    suspend fun atualizarCicloAcerto(rotaId: Long, ciclo: Int, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.atualizarCicloAcerto(rotaId, ciclo, timestamp)
    suspend fun iniciarCicloRota(rotaId: Long, ciclo: Int, dataInicio: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.iniciarCicloRota(rotaId, ciclo, dataInicio, timestamp)
    suspend fun finalizarCicloRota(rotaId: Long, dataFim: Long, timestamp: Long = System.currentTimeMillis()) = 
        rotaDao.finalizarCicloRota(rotaId, dataFim, timestamp)
    suspend fun existeRotaComNome(nome: String, excludeId: Long = 0) = 
        rotaDao.existeRotaComNome(nome, excludeId)
    suspend fun contarRotasAtivas() = rotaDao.contarRotasAtivas()
    
    // ==================== DESPESA ====================
    
    fun obterTodasDespesas() = despesaDao.buscarTodasComRota()
    fun obterDespesasPorRota(rotaId: Long) = despesaDao.buscarPorRota(rotaId)
    suspend fun obterDespesaPorId(id: Long) = despesaDao.buscarPorId(id)
    suspend fun inserirDespesa(despesa: Despesa): Long {
        logDbInsertStart("DESPESA", "Descricao=${despesa.descricao}, RotaID=${despesa.rotaId}")
        return try {
            val id = despesaDao.inserir(despesa)
            logDbInsertSuccess("DESPESA", "Descricao=${despesa.descricao}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("DESPESA", "Descricao=${despesa.descricao}", e)
            throw e
        }
    }
    suspend fun atualizarDespesa(despesa: Despesa) = despesaDao.atualizar(despesa)
    suspend fun deletarDespesa(despesa: Despesa) = despesaDao.deletar(despesa)
    suspend fun calcularTotalPorRota(rotaId: Long) = despesaDao.calcularTotalPorRota(rotaId)
    suspend fun calcularTotalGeral() = despesaDao.calcularTotalGeral()
    suspend fun contarDespesasPorRota(rotaId: Long) = despesaDao.contarPorRota(rotaId)
    suspend fun deletarDespesasPorRota(rotaId: Long) = despesaDao.deletarPorRota(rotaId)
    fun buscarDespesasPorCicloId(cicloId: Long) = despesaDao.buscarPorCicloId(cicloId)
    fun buscarDespesasPorRotaECicloId(rotaId: Long, cicloId: Long) = despesaDao.buscarPorRotaECicloId(rotaId, cicloId)

    // ✅ NOVO: despesas globais
    suspend fun buscarDespesasGlobaisPorCiclo(ano: Int, numero: Int): List<Despesa> = despesaDao.buscarGlobaisPorCiclo(ano, numero)
    suspend fun somarDespesasGlobaisPorCiclo(ano: Int, numero: Int): Double = despesaDao.somarGlobaisPorCiclo(ano, numero)

    // ✅ NOVO: obter mesas por ciclo (a partir dos acertos do ciclo)
    suspend fun contarMesasPorCiclo(cicloId: Long): Int {
        return try {
            val acertos = buscarAcertosPorCicloId(cicloId).first()
            if (acertos.isEmpty()) return 0
            val mesas = mutableSetOf<Long>()
            for (acerto in acertos) {
                val itens = acertoMesaDao.buscarPorAcertoId(acerto.id)
                itens.forEach { mesas.add(it.mesaId) }
            }
            mesas.size
        } catch (e: Exception) { 0 }
    }

    /**
     * ✅ NOVO: Finaliza o ciclo EM_ANDAMENTO da rota persistindo dados consolidados
     * - Atualiza CicloAcertoEntity: status=FINALIZADO, dataFim, valores agregados e debitoTotal
     * - Atualiza status/data da Rota
     */
    suspend fun finalizarCicloAtualComDados(rotaId: Long): Boolean {
        return try {
            val cicloAtual = cicloAcertoDao.buscarCicloEmAndamento(rotaId) ?: return false

            // Buscar dados necessários para consolidar
            val acertos = acertoDao.buscarPorCicloId(cicloAtual.id).first()
            val despesas = despesaDao.buscarPorCicloId(cicloAtual.id).first()
            val clientes = clienteDao.obterClientesPorRota(rotaId).first()

            val valorTotalAcertado = acertos.sumOf { it.valorRecebido }
            val valorTotalDespesas = despesas.sumOf { it.valor }
            val lucroLiquido = valorTotalAcertado - valorTotalDespesas
            val clientesAcertados = acertos.map { it.clienteId }.distinct().size
            val totalClientes = clientes.size
            val debitoTotal = clientes.sumOf { it.debitoAtual }

            val agora = java.util.Date()
            val cicloFinalizado = cicloAtual.copy(
                status = StatusCicloAcerto.FINALIZADO,
                dataFim = agora,
                valorTotalAcertado = valorTotalAcertado,
                valorTotalDespesas = valorTotalDespesas,
                lucroLiquido = lucroLiquido,
                clientesAcertados = clientesAcertados,
                totalClientes = totalClientes,
                debitoTotal = debitoTotal
            )
            cicloAcertoDao.atualizar(cicloFinalizado)

            // Atualizar também status/data da rota para consistência
            rotaDao.finalizarCicloRota(rotaId, agora.time)
            true
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao finalizar ciclo atual da rota $rotaId: ${e.message}", e)
            false
        }
    }

    // ✅ NOVO: contar mesas distintas a partir de vários ciclos
    suspend fun contarMesasPorCiclos(cicloIds: List<Long>): Int {
        return try {
            if (cicloIds.isEmpty()) return 0
            val mesas = mutableSetOf<Long>()
            for (cicloId in cicloIds) {
                val acertos = buscarAcertosPorCicloId(cicloId).first()
                for (acerto in acertos) {
                    val itens = acertoMesaDao.buscarPorAcertoId(acerto.id)
                    itens.forEach { mesas.add(it.mesaId) }
                }
            }
            mesas.size
        } catch (e: Exception) { 0 }
    }
    
    // ✅ NOVO: calcular total de descontos por ciclo
    suspend fun calcularTotalDescontosPorCiclo(cicloId: Long): Double {
        return try {
            val acertos = buscarAcertosPorCicloId(cicloId).first()
            val totalDescontos = acertos.sumOf { it.desconto }
            android.util.Log.d("AppRepository", "✅ Total de descontos calculado para ciclo $cicloId: R$ $totalDescontos")
            totalDescontos
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular total de descontos para ciclo $cicloId: ${e.message}")
            0.0
        }
    }

    // ✅ NOVO: calcular comissões de motorista e Iltair por ciclo
    suspend fun calcularComissoesPorCiclo(cicloId: Long): Pair<Double, Double> {
        val acertos = buscarAcertosPorCicloId(cicloId).first()
        val despesas = buscarDespesasPorCicloId(cicloId).first()
        
        val totalRecebido = acertos.sumOf { it.valorRecebido }
        val despesasViagem = despesas.filter { it.categoria.equals("Viagem", ignoreCase = true) }.sumOf { it.valor }
        val subtotal = totalRecebido - despesasViagem
        
        val comissaoMotorista = subtotal * 0.03 // 3% do subtotal
        val comissaoIltair = totalRecebido * 0.02 // 2% do faturamento total
        
        return Pair(comissaoMotorista, comissaoIltair)
    }
    
    // ✅ NOVO: calcular comissões por ano e número de ciclo
    suspend fun calcularComissoesPorAnoECiclo(ano: Int, numeroCiclo: Int): Pair<Double, Double> {
        val ciclos = obterTodosCiclos().first()
            .filter { it.ano == ano && it.numeroCiclo == numeroCiclo }
        
        var totalComissaoMotorista = 0.0
        var totalComissaoIltair = 0.0
        
        for (ciclo in ciclos) {
            val (comissaoMotorista, comissaoIltair) = calcularComissoesPorCiclo(ciclo.id)
            totalComissaoMotorista += comissaoMotorista
            totalComissaoIltair += comissaoIltair
        }
        
        return Pair(totalComissaoMotorista, totalComissaoIltair)
    }
    
    // ✅ NOVO: calcular comissões por ano (todos os ciclos)
    suspend fun calcularComissoesPorAno(ano: Int): Pair<Double, Double> {
        val ciclos = obterTodosCiclos().first()
            .filter { it.ano == ano }
        
        var totalComissaoMotorista = 0.0
        var totalComissaoIltair = 0.0
        
        for (ciclo in ciclos) {
            val (comissaoMotorista, comissaoIltair) = calcularComissoesPorCiclo(ciclo.id)
            totalComissaoMotorista += comissaoMotorista
            totalComissaoIltair += comissaoIltair
        }
        
        return Pair(totalComissaoMotorista, totalComissaoIltair)
    }
    
    // ==================== COLABORADOR ====================
    
    fun obterTodosColaboradores() = colaboradorDao.obterTodos()
    fun obterColaboradoresAtivos() = colaboradorDao.obterAtivos()
    fun obterColaboradoresAprovados() = colaboradorDao.obterAprovados()
    fun obterColaboradoresPendentesAprovacao() = colaboradorDao.obterPendentesAprovacao()
    fun obterColaboradoresPorNivelAcesso(nivelAcesso: NivelAcesso) = colaboradorDao.obterPorNivelAcesso(nivelAcesso)
    
    suspend fun obterColaboradorPorId(id: Long) = colaboradorDao.obterPorId(id)
    suspend fun obterColaboradorPorEmail(email: String) = colaboradorDao.obterPorEmail(email)
    suspend fun obterColaboradorPorFirebaseUid(firebaseUid: String) = colaboradorDao.obterPorFirebaseUid(firebaseUid)
    suspend fun obterColaboradorPorGoogleId(googleId: String) = colaboradorDao.obterPorGoogleId(googleId)
    
    suspend fun inserirColaborador(colaborador: Colaborador): Long {
        logDbInsertStart("COLABORADOR", "Nome=${colaborador.nome}, Email=${colaborador.email}, Nivel=${colaborador.nivelAcesso}")
        return try {
            val id = colaboradorDao.inserir(colaborador)
            logDbInsertSuccess("COLABORADOR", "Email=${colaborador.email}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("COLABORADOR", "Email=${colaborador.email}", e)
            throw e
        }
    }
    suspend fun atualizarColaborador(colaborador: Colaborador) = colaboradorDao.atualizar(colaborador)
    suspend fun deletarColaborador(colaborador: Colaborador) = colaboradorDao.deletar(colaborador)
    
    suspend fun aprovarColaborador(colaboradorId: Long, dataAprovacao: java.util.Date, aprovadoPor: String) = 
        colaboradorDao.aprovarColaborador(colaboradorId, dataAprovacao, aprovadoPor)
    
    suspend fun aprovarColaboradorComCredenciais(
        colaboradorId: Long,
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        dataAprovacao: java.util.Date,
        aprovadoPor: String
    ) = colaboradorDao.aprovarColaboradorComCredenciais(
        colaboradorId, email, senha, nivelAcesso, observacoes, dataAprovacao, aprovadoPor
    )
    suspend fun alterarStatusColaborador(colaboradorId: Long, ativo: Boolean) = 
        colaboradorDao.alterarStatus(colaboradorId, ativo)
    suspend fun atualizarUltimoAcessoColaborador(colaboradorId: Long, dataUltimoAcesso: java.util.Date) = 
        colaboradorDao.atualizarUltimoAcesso(colaboradorId, dataUltimoAcesso)
    
    suspend fun contarColaboradoresAtivos() = colaboradorDao.contarAtivos()
    suspend fun contarColaboradoresPendentesAprovacao() = colaboradorDao.contarPendentesAprovacao()
    
    // ==================== META COLABORADOR ====================
    
    fun obterMetasPorColaborador(colaboradorId: Long) = colaboradorDao.obterMetasPorColaborador(colaboradorId)
    suspend fun obterMetaAtual(colaboradorId: Long, tipoMeta: TipoMeta) = colaboradorDao.obterMetaAtual(colaboradorId, tipoMeta)
    suspend fun inserirMeta(meta: MetaColaborador): Long {
        logDbInsertStart("META", "ColaboradorID=${meta.colaboradorId}, Tipo=${meta.tipoMeta}, Valor=${meta.valorMeta}")
        return try {
            val id = colaboradorDao.inserirMeta(meta)
            logDbInsertSuccess("META", "ColaboradorID=${meta.colaboradorId}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("META", "ColaboradorID=${meta.colaboradorId}", e)
            throw e
        }
    }
    suspend fun atualizarMeta(meta: MetaColaborador) = colaboradorDao.atualizarMeta(meta)
    suspend fun deletarMeta(meta: MetaColaborador) = colaboradorDao.deletarMeta(meta)
    suspend fun atualizarValorAtualMeta(metaId: Long, valorAtual: Double) = colaboradorDao.atualizarValorAtualMeta(metaId, valorAtual)
    
    // ==================== METAS POR ROTA ====================
    
    fun obterMetasPorRota(rotaId: Long) = colaboradorDao.obterMetasPorRota(0L, rotaId)
    fun obterMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = colaboradorDao.obterMetasPorCiclo(colaboradorId, cicloId)
    fun obterMetasPorColaboradorERota(colaboradorId: Long, rotaId: Long) = colaboradorDao.obterMetasPorRota(colaboradorId, rotaId)
    fun obterMetasPorColaboradorCicloERota(colaboradorId: Long, cicloId: Long, rotaId: Long) = colaboradorDao.obterMetasPorCicloERota(colaboradorId, cicloId, rotaId)
    suspend fun desativarMetasColaborador(colaboradorId: Long) = colaboradorDao.desativarMetasColaborador(colaboradorId)
    
    // Métodos para metas
    suspend fun buscarMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = colaboradorDao.buscarMetasPorColaboradorECiclo(colaboradorId, cicloId)
    suspend fun buscarMetasPorRotaECiclo(rotaId: Long, cicloId: Long) = colaboradorDao.buscarMetasPorRotaECiclo(rotaId, cicloId)

    suspend fun existeMetaDuplicada(rotaId: Long, cicloId: Long, tipoMeta: TipoMeta): Boolean {
        val count = colaboradorDao.contarMetasPorRotaCicloETipo(rotaId, cicloId, tipoMeta)
        return count > 0
    }
    
    // ==================== FUNÇÕES PARA SISTEMA DE METAS ====================
    
    /**
     * Busca colaborador responsável principal por uma rota
     */
    suspend fun buscarColaboradorResponsavelPrincipal(rotaId: Long): Colaborador? {
        return try {
            colaboradorDao?.buscarColaboradorResponsavelPrincipal(rotaId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao buscar colaborador responsável: ${e.message}", e)
            null
        }
    }
    
    /**
     * Busca ciclo atual (em andamento) para uma rota
     */
    suspend fun buscarCicloAtualPorRota(rotaId: Long): CicloAcertoEntity? {
        return try {
            cicloAcertoDao.buscarCicloAtualPorRota(rotaId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao buscar ciclo atual: ${e.message}", e)
            null
        }
    }
    
    /**
     * Busca ciclos futuros (planejados) para uma rota
     */
    suspend fun buscarCiclosFuturosPorRota(rotaId: Long): List<CicloAcertoEntity> {
        return try {
            cicloAcertoDao.buscarCiclosFuturosPorRota(rotaId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Erro ao buscar ciclos futuros: ${e.message}", e)
            emptyList()
        }
    }
    
    
    fun buscarMetasAtivasPorColaborador(colaboradorId: Long) = colaboradorDao.buscarMetasAtivasPorColaborador(colaboradorId)
    suspend fun buscarMetasPorTipoECiclo(tipoMeta: TipoMeta, cicloId: Long) = colaboradorDao.buscarMetasPorTipoECiclo(tipoMeta, cicloId)
    
    // ==================== COLABORADOR ROTA ====================
    
    fun obterRotasPorColaborador(colaboradorId: Long) = colaboradorDao.obterRotasPorColaborador(colaboradorId)
    fun obterColaboradoresPorRota(rotaId: Long) = colaboradorDao.obterColaboradoresPorRota(rotaId)
    suspend fun obterRotaPrincipal(colaboradorId: Long) = colaboradorDao.obterRotaPrincipal(colaboradorId)
    suspend fun inserirColaboradorRota(colaboradorRota: ColaboradorRota): Long {
        logDbInsertStart(
            "COLABORADOR_ROTA",
            "ColaboradorID=${colaboradorRota.colaboradorId}, RotaID=${colaboradorRota.rotaId}, Responsavel=${colaboradorRota.responsavelPrincipal}"
        )

        return try {
            val id = colaboradorDao.inserirColaboradorRota(colaboradorRota)
            logDbInsertSuccess(
                "COLABORADOR_ROTA",
                "ColaboradorID=${colaboradorRota.colaboradorId}, RotaID=${colaboradorRota.rotaId}, ID=$id"
            )
            id
        } catch (e: Exception) {
            logDbInsertError(
                "COLABORADOR_ROTA",
                "ColaboradorID=${colaboradorRota.colaboradorId}, RotaID=${colaboradorRota.rotaId}",
                e
            )
            throw e
        }
    }

    /**
     * Busca acertos de um cliente filtrando pelo cicloId (consulta direta no DAO)
     */
    fun buscarAcertosPorClienteECicloId(clienteId: Long, cicloId: Long) = acertoDao.buscarPorClienteECicloId(clienteId, cicloId)
    suspend fun deletarColaboradorRota(colaboradorRota: ColaboradorRota) = colaboradorDao.deletarColaboradorRota(colaboradorRota)
    suspend fun deletarTodasRotasColaborador(colaboradorId: Long) = colaboradorDao.deletarTodasRotasColaborador(colaboradorId)
    suspend fun removerResponsavelPrincipal(colaboradorId: Long) = colaboradorDao.removerResponsavelPrincipal(colaboradorId)
    suspend fun definirResponsavelPrincipal(colaboradorId: Long, rotaId: Long) = colaboradorDao.definirResponsavelPrincipal(colaboradorId, rotaId)
    
    // Métodos auxiliares para vinculação de colaborador com rotas
    suspend fun removerRotasColaborador(colaboradorId: Long) = colaboradorDao.deletarTodasRotasColaborador(colaboradorId)
    suspend fun vincularColaboradorRota(colaboradorId: Long, rotaId: Long, responsavelPrincipal: Boolean, dataVinculacao: java.util.Date) {
        val colaboradorRota = ColaboradorRota(
            colaboradorId = colaboradorId,
            rotaId = rotaId,
            responsavelPrincipal = responsavelPrincipal,
            dataVinculacao = dataVinculacao
        )
        colaboradorDao.inserirColaboradorRota(colaboradorRota)
    }
    
    
    // ==================== CICLO ACERTO ====================
    
    fun obterTodosCiclos() = cicloAcertoDao.listarTodos()
    
    suspend fun buscarUltimoCicloFinalizadoPorRota(rotaId: Long) = cicloAcertoDao.buscarUltimoCicloFinalizadoPorRota(rotaId)
    suspend fun buscarCiclosPorRotaEAno(rotaId: Long, ano: Int) = cicloAcertoDao.buscarCiclosPorRotaEAno(rotaId, ano)
    
    suspend fun buscarCiclosPorRota(rotaId: Long) = cicloAcertoDao.buscarCiclosPorRota(rotaId)
    suspend fun buscarProximoNumeroCiclo(rotaId: Long, ano: Int) = cicloAcertoDao.buscarProximoNumeroCiclo(rotaId, ano)
    suspend fun inserirCicloAcerto(ciclo: CicloAcertoEntity): Long {
        logDbInsertStart("CICLO", "RotaID=${ciclo.rotaId}, Numero=${ciclo.numeroCiclo}, Status=${ciclo.status}")
        return try {
            val id = cicloAcertoDao.inserir(ciclo)
            logDbInsertSuccess("CICLO", "ID=$id, RotaID=${ciclo.rotaId}")
            id
        } catch (e: Exception) {
            logDbInsertError("CICLO", "RotaID=${ciclo.rotaId}", e)
            throw e
        }
    }

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
    
    // ==================== MÉTODOS PARA RELATÓRIOS ====================
    
    // Métodos para relatórios de despesas
    suspend fun getDespesasPorCiclo(cicloId: Long, rotaId: Long): List<DespesaRelatorio> {
        return try {
            val despesas = if (rotaId == 0L) {
                despesaDao.buscarPorCicloId(cicloId).first()
            } else {
                despesaDao.buscarPorRotaECicloId(rotaId, cicloId).first()
            }
	            // Evita chamar função suspend dentro de map
	            val rotasMap = rotaDao.getAllRotas().first().associateBy { it.id }
            
            despesas.map { despesa ->
	                val rotaNome = rotasMap[despesa.rotaId]?.nome ?: "Rota não encontrada"
                DespesaRelatorio(
                    id = despesa.id,
                    descricao = despesa.descricao,
                    valor = despesa.valor,
                    categoria = despesa.categoria,
                    data = despesa.dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
	                    rota = rotaNome,
                    observacoes = despesa.observacoes.takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getDespesasConsolidadasCiclos(numeroCiclo: Int, ano: Int, rotaId: Long): List<DespesaRelatorio> {
        return try {
            // Buscar todos os ciclos do mesmo número no ano
            val ciclos = cicloAcertoDao.listarTodos().first()
                .filter { it.numeroCiclo == numeroCiclo && it.dataInicio.year + 1900 == ano }
            
            val despesas = mutableListOf<DespesaRelatorio>()
            
            for (ciclo in ciclos) {
                val despesasCiclo = if (rotaId == 0L) {
                    despesaDao.buscarPorCicloId(ciclo.id).first()
                } else {
                    despesaDao.buscarPorRotaECicloId(rotaId, ciclo.id).first()
                }
	                // Evita função suspend dentro de map
	                val rotasMap = rotaDao.getAllRotas().first().associateBy { it.id }
                
                despesas.addAll(despesasCiclo.map { despesa ->
	                    val rotaNome = rotasMap[despesa.rotaId]?.nome ?: "Rota não encontrada"
                    DespesaRelatorio(
                        id = despesa.id,
                        descricao = despesa.descricao,
                        valor = despesa.valor,
                        categoria = despesa.categoria,
                        data = despesa.dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
	                        rota = rotaNome,
                        observacoes = despesa.observacoes.takeIf { it.isNotBlank() }
                    )
                })
            }
            
            despesas
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getDespesasPorAno(ano: Int, rotaId: Long): List<DespesaRelatorio> {
        return try {
            val dataInicio = java.time.LocalDateTime.of(ano, 1, 1, 0, 0)
            val dataFim = java.time.LocalDateTime.of(ano, 12, 31, 23, 59)
            
	            val despesas: List<Despesa> = if (rotaId == 0L) {
	                // Converte DespesaResumo -> Despesa para unificar o tipo
	                despesaDao.buscarPorPeriodo(dataInicio, dataFim).first().map { it.despesa }
            } else {
                despesaDao.buscarPorRotaEPeriodo(rotaId, dataInicio, dataFim)
            }
            
	            // Evita chamar função suspend dentro de map
	            val rotasMap = rotaDao.getAllRotas().first().associateBy { it.id }
            
            despesas.map { despesa ->
	                val rotaNome = rotasMap[despesa.rotaId]?.nome ?: "Rota não encontrada"
                DespesaRelatorio(
                    id = despesa.id,
                    descricao = despesa.descricao,
                    valor = despesa.valor,
                    categoria = despesa.categoria,
                    data = despesa.dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
	                    rota = rotaNome,
                    observacoes = despesa.observacoes.takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getCategoriasDespesas(): List<String> {
        return try {
            val despesas = despesaDao.buscarTodasComRota().first()
            despesas.map { it.categoria }.distinct().sorted()
        } catch (e: Exception) {
            listOf("Combustível", "Alimentação", "Transporte", "Manutenção", "Materiais", "Outros")
        }
    }
    
    // Data class para relatórios
    data class DespesaRelatorio(
        val id: Long,
        val descricao: String,
        val valor: Double,
        val categoria: String,
        val data: String,
        val rota: String,
        val observacoes: String?
    )
    
    // Métodos stub para sincronização - BLOQUEADOS para evitar população automática
    suspend fun syncRotas(_rotas: List<Rota>) {
        // BLOQUEADO: Sincronização de rotas desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC ROTAS BLOQUEADO - Evitando população automática")
    }

    suspend fun syncClientes(_clientes: List<Cliente>) {
        // BLOQUEADO: Sincronização de clientes desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC CLIENTES BLOQUEADO - Evitando população automática")
    }

    suspend fun syncAcertos(_acertos: List<Acerto>) {
        // BLOQUEADO: Sincronização de acertos desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC ACERTOS BLOQUEADO - Evitando população automática")
    }

    suspend fun syncColaboradores(_colaboradores: List<Colaborador>) {
        // BLOQUEADO: Sincronização de colaboradores desabilitada para evitar população automática
        android.util.Log.d("AppRepository", "SYNC COLABORADORES BLOQUEADO - Evitando população automática")
    }
    
    // ==================== CONTRATOS DE LOCAÇÃO ====================
    
    fun buscarContratosPorCliente(clienteId: Long) = contratoLocacaoDao.buscarContratosPorCliente(clienteId)
    suspend fun buscarContratoPorNumero(numeroContrato: String) = contratoLocacaoDao.buscarContratoPorNumero(numeroContrato)
    fun buscarContratosAtivos() = contratoLocacaoDao.buscarContratosAtivos()
    fun buscarTodosContratos() = contratoLocacaoDao.buscarTodosContratos()
    suspend fun contarContratosPorAno(ano: String) = contratoLocacaoDao.contarContratosPorAno(ano)
    suspend fun contarContratosGerados() = contratoLocacaoDao.contarContratosGerados()
    suspend fun contarContratosAssinados() = contratoLocacaoDao.contarContratosAssinados()
    suspend fun obterContratosAssinados() = contratoLocacaoDao.obterContratosAssinados()
    suspend fun inserirContrato(contrato: ContratoLocacao): Long {
        logDbInsertStart("CONTRATO", "Numero=${contrato.numeroContrato}, ClienteID=${contrato.clienteId}")
        return try {
            val id = contratoLocacaoDao.inserirContrato(contrato)
            logDbInsertSuccess("CONTRATO", "Numero=${contrato.numeroContrato}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("CONTRATO", "Numero=${contrato.numeroContrato}", e)
            throw e
        }
    }
    suspend fun atualizarContrato(contrato: ContratoLocacao) {
        try {
            Log.d("RepoUpdate", "Atualizando contrato id=${contrato.id} cliente=${contrato.clienteId} status=${contrato.status} encerramento=${contrato.dataEncerramento}")
            contratoLocacaoDao.atualizarContrato(contrato)
            // Leitura de verificação (apenas diagnóstico)
            try {
                val apos = contratoLocacaoDao.buscarContratosPorCliente(contrato.clienteId).first()
                val resumo = apos.joinToString { c -> "id=${'$'}{c.id},status=${'$'}{c.status},enc=${'$'}{c.dataEncerramento}" }
                Log.d("RepoContracts", "Após atualizar: cliente=${contrato.clienteId} contratos=${apos.size} -> ${'$'}resumo")
            } catch (e: Exception) {
                Log.e("RepoContracts", "Falha ao ler contratos após atualizar", e)
            }
        } catch (e: Exception) {
            Log.e("RepoUpdate", "Erro ao atualizar contrato id=${contrato.id}", e)
            throw e
        }
    }

    // ✅ NOVO: Encerrar contrato (UPDATE direto)
    suspend fun encerrarContrato(contratoId: Long, clienteId: Long, status: String) {
        val agora = java.util.Date()
        Log.d("RepoUpdate", "Encerrar direto contrato id=${contratoId} status=${status} em ${agora}")
        contratoLocacaoDao.encerrarContrato(contratoId, status, agora, agora)
        val apos = contratoLocacaoDao.buscarContratosPorCliente(clienteId).first()
        val resumo = apos.joinToString { c -> "id=${'$'}{c.id},status=${'$'}{c.status},enc=${'$'}{c.dataEncerramento}" }
        Log.d("RepoContracts", "Após encerrar direto: cliente=${clienteId} contratos=${apos.size} -> ${'$'}resumo")
    }
    suspend fun excluirContrato(contrato: ContratoLocacao) = contratoLocacaoDao.excluirContrato(contrato)
    suspend fun buscarContratoPorId(contratoId: Long) = contratoLocacaoDao.buscarContratoPorId(contratoId)
    suspend fun buscarMesasPorContrato(contratoId: Long) = contratoLocacaoDao.buscarMesasPorContrato(contratoId)
    suspend fun inserirContratoMesa(contratoMesa: ContratoMesa): Long {
        logDbInsertStart("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}, MesaID=${contratoMesa.mesaId}")
        return try {
            val id = contratoLocacaoDao.inserirContratoMesa(contratoMesa)
            logDbInsertSuccess("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}", e)
            throw e
        }
    }
    suspend fun inserirContratoMesas(contratoMesas: List<ContratoMesa>): List<Long> {
        logDbInsertStart("CONTRATO_MESAS", "Quantidade=${contratoMesas.size}")
        return try {
            val ids = contratoLocacaoDao.inserirContratoMesas(contratoMesas)
            logDbInsertSuccess("CONTRATO_MESAS", "IDs=${ids.joinToString()}")
            ids
        } catch (e: Exception) {
            logDbInsertError("CONTRATO_MESAS", "Quantidade=${contratoMesas.size}", e)
            throw e
        }
    }
    suspend fun excluirContratoMesa(contratoMesa: ContratoMesa) = contratoLocacaoDao.excluirContratoMesa(contratoMesa)
    suspend fun excluirMesasPorContrato(contratoId: Long) = contratoLocacaoDao.excluirMesasPorContrato(contratoId)
    
    // ==================== ADITIVO CONTRATO ====================
    
    fun buscarAditivosPorContrato(contratoId: Long) = aditivoContratoDao.buscarAditivosPorContrato(contratoId)
    suspend fun buscarAditivoPorNumero(numeroAditivo: String) = aditivoContratoDao.buscarAditivoPorNumero(numeroAditivo)
    suspend fun buscarAditivoPorId(aditivoId: Long) = aditivoContratoDao.buscarAditivoPorId(aditivoId)
    fun buscarTodosAditivos() = aditivoContratoDao.buscarTodosAditivos()
    suspend fun contarAditivosPorAno(ano: String) = aditivoContratoDao.contarAditivosPorAno(ano)
    suspend fun contarAditivosGerados() = aditivoContratoDao.contarAditivosGerados()
    suspend fun contarAditivosAssinados() = aditivoContratoDao.contarAditivosAssinados()
    suspend fun inserirAditivo(aditivo: AditivoContrato): Long {
        logDbInsertStart("ADITIVO", "ContratoID=${aditivo.contratoId}, Numero=${aditivo.numeroAditivo}")
        return try {
            val id = aditivoContratoDao.inserirAditivo(aditivo)
            logDbInsertSuccess("ADITIVO", "ContratoID=${aditivo.contratoId}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("ADITIVO", "ContratoID=${aditivo.contratoId}", e)
            throw e
        }
    }

    suspend fun inserirAditivoMesas(aditivoMesas: List<AditivoMesa>): List<Long> {
        logDbInsertStart("ADITIVO_MESAS", "Quantidade=${aditivoMesas.size}")
        return try {
            val ids = aditivoContratoDao.inserirAditivoMesas(aditivoMesas)
            logDbInsertSuccess("ADITIVO_MESAS", "IDs=${ids.joinToString()}")
            ids
        } catch (e: Exception) {
            logDbInsertError("ADITIVO_MESAS", "Quantidade=${aditivoMesas.size}", e)
            throw e
        }
    }

    suspend fun inserirAssinaturaRepresentanteLegal(assinatura: AssinaturaRepresentanteLegal): Long {
        logDbInsertStart(
            "ASSINATURA",
            "Representante=${assinatura.nomeRepresentante}, NumeroProcuração=${assinatura.numeroProcuração}"
        )
        return try {
            val id = assinaturaRepresentanteLegalDao.inserirAssinatura(assinatura)
            logDbInsertSuccess(
                "ASSINATURA",
                "Representante=${assinatura.nomeRepresentante}, ID=$id"
            )
            id
        } catch (e: Exception) {
            logDbInsertError(
                "ASSINATURA",
                "Representante=${assinatura.nomeRepresentante}",
                e
            )
            throw e
        }
    }

    suspend fun inserirLogAuditoriaAssinatura(log: LogAuditoriaAssinatura): Long {
        logDbInsertStart(
            "LOG_ASSINATURA",
            "Tipo=${log.tipoOperacao}, ContratoID=${log.idContrato ?: "N/A"}"
        )
        return try {
            val id = logAuditoriaAssinaturaDao.inserirLog(log)
            logDbInsertSuccess(
                "LOG_ASSINATURA",
                "Tipo=${log.tipoOperacao}, ID=$id"
            )
            id
        } catch (e: Exception) {
            logDbInsertError(
                "LOG_ASSINATURA",
                "Tipo=${log.tipoOperacao}, ContratoID=${log.idContrato ?: "N/A"}",
                e
            )
            throw e
        }
    }

    // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    // suspend fun inserirProcuração(procuração: ProcuraçãoRepresentante): Long {
    //     logDbInsertStart("PROCURACAO", "Representante=${procuração.representanteOutorgadoNome}, Empresa=${procuração.empresaNome}")
    //     return try {
    //         val id = .inserirProcuração(procuração)
    //         logDbInsertSuccess("PROCURACAO", "Representante=${procuração.representanteOutorgadoNome}, ID=$id")
    //         id
    //     } catch (e: Exception) {
    //         logDbInsertError("PROCURACAO", "Representante=${procuração.representanteOutorgadoNome}", e)
    //         throw e
    //     }
    // }

    suspend fun atualizarAditivo(aditivo: AditivoContrato) = aditivoContratoDao.atualizarAditivo(aditivo)
    suspend fun excluirAditivo(aditivo: AditivoContrato) = aditivoContratoDao.excluirAditivo(aditivo)
    suspend fun buscarMesasPorAditivo(aditivoId: Long) = aditivoContratoDao.buscarMesasPorAditivo(aditivoId)
    suspend fun excluirAditivoMesa(aditivoMesa: AditivoMesa) = aditivoContratoDao.excluirAditivoMesa(aditivoMesa)
    suspend fun excluirTodasMesasDoAditivo(aditivoId: Long) = aditivoContratoDao.excluirTodasMesasDoAditivo(aditivoId)
    
    // ==================== ASSINATURA REPRESENTANTE LEGAL ====================
    
    suspend fun obterAssinaturaRepresentanteLegalAtiva() = assinaturaRepresentanteLegalDao.obterAssinaturaAtiva()
    fun obterAssinaturaRepresentanteLegalAtivaFlow() = assinaturaRepresentanteLegalDao.obterAssinaturaAtivaFlow()
    suspend fun obterTodasAssinaturasRepresentanteLegal() = assinaturaRepresentanteLegalDao.obterTodasAssinaturas()
    fun obterTodasAssinaturasRepresentanteLegalFlow() = assinaturaRepresentanteLegalDao.obterTodasAssinaturasFlow()
    suspend fun obterAssinaturaRepresentanteLegalPorId(id: Long) = assinaturaRepresentanteLegalDao.obterAssinaturaPorId(id)
    suspend fun atualizarAssinaturaRepresentanteLegal(assinatura: AssinaturaRepresentanteLegal) = assinaturaRepresentanteLegalDao.atualizarAssinatura(assinatura)
    suspend fun desativarAssinaturaRepresentanteLegal(id: Long) = assinaturaRepresentanteLegalDao.desativarAssinatura(id)
    suspend fun incrementarUsoAssinatura(id: Long, dataUso: java.util.Date) = assinaturaRepresentanteLegalDao.incrementarUso(id, dataUso)
    suspend fun contarAssinaturasRepresentanteLegalAtivas() = assinaturaRepresentanteLegalDao.contarAssinaturasAtivas()
    suspend fun obterAssinaturasRepresentanteLegalValidadas() = assinaturaRepresentanteLegalDao.obterAssinaturasValidadas()
    
    // ==================== LOGS DE AUDITORIA ====================
    
    suspend fun obterTodosLogsAuditoria() = logAuditoriaAssinaturaDao.obterTodosLogs()
    fun obterTodosLogsAuditoriaFlow() = logAuditoriaAssinaturaDao.obterTodosLogsFlow()
    suspend fun obterLogsAuditoriaPorAssinatura(idAssinatura: Long) = logAuditoriaAssinaturaDao.obterLogsPorAssinatura(idAssinatura)
    suspend fun obterLogsAuditoriaPorContrato(idContrato: Long) = logAuditoriaAssinaturaDao.obterLogsPorContrato(idContrato)
    suspend fun obterLogsAuditoriaPorTipoOperacao(tipoOperacao: String) = logAuditoriaAssinaturaDao.obterLogsPorTipoOperacao(tipoOperacao)
    suspend fun obterLogsAuditoriaPorPeriodo(dataInicio: java.util.Date, dataFim: java.util.Date) = logAuditoriaAssinaturaDao.obterLogsPorPeriodo(dataInicio, dataFim)
    suspend fun obterLogsAuditoriaPorUsuario(usuario: String) = logAuditoriaAssinaturaDao.obterLogsPorUsuario(usuario)
    suspend fun obterLogsAuditoriaComErro() = logAuditoriaAssinaturaDao.obterLogsComErro()
    suspend fun contarLogsAuditoriaDesde(dataInicio: java.util.Date) = logAuditoriaAssinaturaDao.contarLogsDesde(dataInicio)
    suspend fun contarUsosAssinaturaAuditoria(idAssinatura: Long) = logAuditoriaAssinaturaDao.contarUsosAssinatura(idAssinatura)
    suspend fun obterLogsAuditoriaNaoValidados() = logAuditoriaAssinaturaDao.obterLogsNaoValidados()
    suspend fun validarLogAuditoria(id: Long, dataValidacao: java.util.Date, validadoPor: String) = logAuditoriaAssinaturaDao.validarLog(id, dataValidacao, validadoPor)
    
    // ==================== PROCURAÇÕES ====================
    
    // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    // suspend fun obter.obterProcuraçõesAtivas()
    // fun obter.obterProcuraçõesAtivasFlow()
    // suspend fun obter.obterProcuraçãoPorUsuario(usuario)
    // fun obter.obterProcuraçãoPorUsuarioFlow(usuario)
    // suspend fun obter.obterProcuraçãoPorCpf(cpf)
    // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    // suspend fun obterTodas.obterTodasProcurações()
    // fun obterTodas.obterTodasProcuraçõesFlow()
    // suspend fun obter.obterProcuraçãoPorId(id)
    // suspend fun obter.obterProcuraçãoPorNumero(numero)
    // suspend fun atualizar.atualizarProcuração(procuração)
    // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    // suspend fun revogar.revogarProcuração(id, dataRevogacao, motivo)
    // suspend fun contar.contarProcuraçõesAtivas()
    // suspend fun obter.obterProcuraçõesValidadas()
    // suspend fun obter.obterProcuraçõesVencidas(dataAtual)
    // suspend fun validar.validarProcuração(id, dataValidacao, validadoPor)
    
    // ==================== MÉTODOS PARA CÁLCULO DE METAS ====================
    
    /**
     * Busca acertos por rota e ciclo
     */
    suspend fun buscarAcertosPorRotaECiclo(rotaId: Long, cicloId: Long): List<Acerto> {
        return try {
            acertoDao.buscarPorRotaECicloId(rotaId, cicloId).first()
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao buscar acertos por rota e ciclo: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Conta clientes ativos por rota
     */
    suspend fun contarClientesAtivosPorRota(rotaId: Long): Int {
        return try {
            clienteDao.obterClientesPorRota(rotaId).first().count { it.ativo }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar clientes ativos por rota: ${e.message}", e)
            0
        }
    }
    
    /**
     * Conta clientes acertados por rota e ciclo
     */
    suspend fun contarClientesAcertadosPorRotaECiclo(rotaId: Long, cicloId: Long): Int {
        return try {
            acertoDao.buscarPorRotaECicloId(rotaId, cicloId).first().size
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar clientes acertados: ${e.message}", e)
            0
        }
    }
    
    /**
     * Conta mesas locadas por rota
     */
    suspend fun contarMesasLocadasPorRota(rotaId: Long): Int {
        return try {
            mesaDao.buscarMesasPorRota(rotaId).first().size
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar mesas locadas: ${e.message}", e)
            0
        }
    }

    /**
     * Conta novas mesas (instaladas) no período do ciclo em uma rota
     */
    suspend fun contarNovasMesasNoCiclo(rotaId: Long, cicloId: Long): Int {
        return try {
            val ciclo = cicloAcertoDao.buscarPorId(cicloId) ?: return 0
            val inicio = ciclo.dataInicio
            val fim = ciclo.dataFim ?: java.util.Date()
            mesaDao.contarNovasMesasInstaladas(rotaId, inicio, fim)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar novas mesas no ciclo: ${e.message}", e)
            0
        }
    }

    private fun logDbInsertStart(entity: String, details: String) {
        val stackTrace = Thread.currentThread().stackTrace
        Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
        Log.w("🔍 DB_POPULATION", "🚨 INSERINDO $entity: $details")
        Log.w("🔍 DB_POPULATION", "📍 Chamado por:")
        stackTrace.drop(3).take(8).forEachIndexed { index, element ->
            Log.w("🔍 DB_POPULATION", "   [${index}] $element")
        }
        Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
    }

    private fun logDbInsertSuccess(entity: String, details: String) {
        Log.w("🔍 DB_POPULATION", "✅ $entity INSERIDO COM SUCESSO: $details")
    }

    private fun logDbInsertError(entity: String, details: String, throwable: Throwable) {
        Log.e("🔍 DB_POPULATION", "❌ ERRO AO INSERIR $entity: $details", throwable)
    }
    
    // ==================== CACHE MANAGEMENT (MODERNIZAÇÃO 2025) ====================
    
    /**
     * ✅ MODERNIZADO: Atualiza cache de clientes
     */
    suspend fun refreshClientesCache() {
        try {
            val clientes = obterTodosClientes().first()
            _clientesCache.value = clientes
            Log.d("AppRepository", "✅ Cache de clientes atualizado: ${clientes.size} itens")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Erro ao atualizar cache de clientes", e)
        }
    }
    
    /**
     * ✅ MODERNIZADO: Atualiza cache de rotas
     */
    suspend fun refreshRotasCache() {
        try {
            val rotas = obterTodasRotas().first()
            _rotasCache.value = rotas
            Log.d("AppRepository", "✅ Cache de rotas atualizado: ${rotas.size} itens")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Erro ao atualizar cache de rotas", e)
        }
    }
    
    /**
     * ✅ MODERNIZADO: Atualiza cache de mesas
     */
    suspend fun refreshMesasCache() {
        try {
            val mesas = obterTodasMesas().first()
            _mesasCache.value = mesas
            Log.d("AppRepository", "✅ Cache de mesas atualizado: ${mesas.size} itens")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Erro ao atualizar cache de mesas", e)
        }
    }
    
    /**
     * ✅ MODERNIZADO: Atualiza todos os caches
     */
    suspend fun refreshAllCaches() {
        Log.d("AppRepository", "🔄 Atualizando todos os caches...")
        refreshClientesCache()
        refreshRotasCache()
        refreshMesasCache()
        Log.d("AppRepository", "✅ Todos os caches atualizados com sucesso")
    }
    
    // ==================== MÉTODOS ADICIONAIS PARA CORREÇÃO DE ERROS ====================
    
    suspend fun inserirHistoricoManutencao(historico: HistoricoManutencaoVeiculo): Long = historicoManutencaoVeiculoDao.inserir(historico)
    suspend fun inserirHistoricoCombustivel(historico: HistoricoCombustivelVeiculo): Long = historicoCombustivelVeiculoDao.inserir(historico)
    suspend fun inserirAcertoMesa(acertoMesa: AcertoMesa): Long = acertoMesaDao.inserir(acertoMesa)
    suspend fun calcularMediaFichasJogadas(mesaId: Long, limite: Int): Double {
        val acertos = acertoMesaDao.buscarPorMesa(mesaId).first().take(limite)
        return if (acertos.isNotEmpty()) {
            acertos.map { acerto -> acerto.fichasJogadas }.average()
        } else 0.0
    }
    suspend fun buscarAcertoMesasPorAcerto(acertoId: Long) = acertoMesaDao.buscarPorAcerto(acertoId)
    suspend fun buscarContratoAtivoPorCliente(clienteId: Long) = contratoLocacaoDao.buscarContratoAtivoPorCliente(clienteId)
    suspend fun inserirHistoricoManutencaoMesa(historico: HistoricoManutencaoMesa): Long = historicoManutencaoMesaDao.inserir(historico)
} 


