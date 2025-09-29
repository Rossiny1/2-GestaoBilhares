package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Log

/**
 * ✅ REPOSITORY CONSOLIDADO - AppRepository
 * Combina todos os repositories em um único arquivo
 * Elimina duplicação e simplifica a arquitetura
 */
class AppRepository(
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
    private val procuraçãoRepresentanteDao: ProcuraçãoRepresentanteDao
) {
    
    // ==================== CLIENTE ====================
    
    fun obterTodosClientes() = clienteDao.obterTodos()
    fun obterClientesPorRota(rotaId: Long) = clienteDao.obterClientesPorRota(rotaId)
    suspend fun obterClientePorId(id: Long) = clienteDao.obterPorId(id)
    suspend fun inserirCliente(cliente: Cliente) = clienteDao.inserir(cliente)
    suspend fun atualizarCliente(cliente: Cliente) = clienteDao.atualizar(cliente)
    suspend fun deletarCliente(cliente: Cliente) = clienteDao.deletar(cliente)
    suspend fun atualizarDebitoAtual(clienteId: Long, novoDebito: Double) = 
        clienteDao.atualizarDebitoAtual(clienteId, novoDebito)
    suspend fun calcularDebitoAtualEmTempoReal(clienteId: Long) = 
        clienteDao.calcularDebitoAtualEmTempoReal(clienteId)
    suspend fun obterClienteComDebitoAtual(clienteId: Long) = 
        clienteDao.obterClienteComDebitoAtual(clienteId)
    
    // ==================== ACERTO ====================
    
    fun obterAcertosPorCliente(clienteId: Long) = acertoDao.buscarPorCliente(clienteId)
    suspend fun obterAcertoPorId(id: Long) = acertoDao.buscarPorId(id)
    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long) = 
        acertoDao.buscarUltimoAcertoPorCliente(clienteId)
    fun obterTodosAcertos() = acertoDao.listarTodos()
    fun buscarAcertosPorCicloId(cicloId: Long) = acertoDao.buscarPorCicloId(cicloId)
    fun buscarClientesPorRota(rotaId: Long) = clienteDao.obterClientesPorRota(rotaId)
    suspend fun buscarRotaPorId(rotaId: Long) = rotaDao.getRotaById(rotaId)
    suspend fun inserirAcerto(acerto: Acerto) = acertoDao.inserir(acerto)
    suspend fun atualizarAcerto(acerto: Acerto) = acertoDao.atualizar(acerto)
    suspend fun deletarAcerto(acerto: Acerto) = acertoDao.deletar(acerto)
    suspend fun buscarUltimoAcertoPorMesa(mesaId: Long) = 
        acertoDao.buscarUltimoAcertoPorMesa(mesaId)
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long) = 
        acertoDao.buscarObservacaoUltimoAcerto(clienteId)
    suspend fun buscarUltimosAcertosPorClientes(clienteIds: List<Long>) =
        acertoDao.buscarUltimosAcertosPorClientes(clienteIds)
    
    // ==================== MESA ====================
    
    suspend fun obterMesaPorId(id: Long) = mesaDao.obterMesaPorId(id)
    fun obterMesasPorCliente(clienteId: Long) = mesaDao.obterMesasPorCliente(clienteId)
    fun obterMesasDisponiveis() = mesaDao.obterMesasDisponiveis()
    suspend fun inserirMesa(mesa: Mesa) = mesaDao.inserir(mesa)
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
        // ✅ CORREÇÃO: Combinar Flow de rotas com Flow de ciclos para atualização em tempo real
        return kotlinx.coroutines.flow.combine(
            rotaDao.getAllRotasAtivas(),
            cicloAcertoDao.listarTodos()
        ) { rotas, ciclos ->
            android.util.Log.d("AppRepository", "🔄 Atualizando resumo de rotas: ${rotas.size} rotas, ${ciclos.size} ciclos")
            
            rotas.map { rota ->
                // Calcular dados reais para cada rota
                val clientesAtivos = calcularClientesAtivosPorRota(rota.id)
                val (cicloAtualNumero, cicloAtualId, dataCicloInicio) = obterCicloAtualRota(rota.id)
                val pendencias = calcularPendenciasReaisPorRota(rota.id)
                val quantidadeMesas = calcularQuantidadeMesasPorRota(rota.id)
                val percentualAcertados = calcularPercentualClientesAcertados(rota.id, cicloAtualId, clientesAtivos)
                val valorAcertado = calcularValorAcertadoPorRotaECiclo(rota.id, cicloAtualId)
                val statusAtual = determinarStatusRotaEmTempoReal(rota.id)
                
                // ✅ NOVO: Obter datas de início e fim do ciclo
                val (dataInicio, dataFim) = obterDatasCicloRota(rota.id)

                val resumo = RotaResumo(
                    rota = rota,
                    clientesAtivos = clientesAtivos,
                    pendencias = pendencias,
                    valorAcertado = valorAcertado,
                    quantidadeMesas = quantidadeMesas,
                    percentualAcertados = percentualAcertados,
                    status = statusAtual,
                    cicloAtual = cicloAtualNumero,
                    dataInicioCiclo = dataInicio,  // ✅ NOVO: Data de início
                    dataFimCiclo = dataFim        // ✅ NOVO: Data de fim
                )
                
                android.util.Log.d("AppRepository", "📊 Rota ${rota.nome}: Ciclo ${cicloAtualNumero}, Status ${statusAtual}")
                resumo
            }
        }
    }
    
    // ✅ NOVO: Métodos auxiliares para calcular dados reais das rotas
    private fun calcularClientesAtivosPorRota(rotaId: Long): Int {
        return try {
            // Usar runBlocking para operações síncronas dentro do Flow
            kotlinx.coroutines.runBlocking {
                clienteDao.obterClientesPorRota(rotaId).first().count { it.ativo }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular clientes ativos da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularPendenciasReaisPorRota(rotaId: Long): Int {
        return try {
            kotlinx.coroutines.runBlocking {
                val clientes = clienteDao.obterClientesPorRota(rotaId).first()
                if (clientes.isEmpty()) return@runBlocking 0
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
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular pendências reais da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularValorAcertadoPorRotaECiclo(rotaId: Long, cicloId: Long?): Double {
        return try {
            if (cicloId == null) return 0.0
            kotlinx.coroutines.runBlocking {
                buscarAcertosPorCicloId(cicloId).first().filter { it.rotaId == rotaId }.sumOf { it.valorRecebido }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular valor acertado da rota $rotaId: ${e.message}")
            0.0
        }
    }
    
    private fun calcularQuantidadeMesasPorRota(rotaId: Long): Int {
        return try {
            kotlinx.coroutines.runBlocking {
                mesaDao.buscarMesasPorRota(rotaId).first().size
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular quantidade de mesas da rota $rotaId: ${e.message}")
            0
        }
    }
    
    private fun calcularPercentualClientesAcertados(rotaId: Long, cicloId: Long?, clientesAtivos: Int): Int {
        return try {
            if (cicloId == null || clientesAtivos == 0) return 0
            kotlinx.coroutines.runBlocking {
                val acertos = buscarAcertosPorCicloId(cicloId).first().filter { it.rotaId == rotaId }
                val distintos = acertos.map { it.clienteId }.distinct().size
                ((distintos.toDouble() / clientesAtivos.toDouble()) * 100).toInt()
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao calcular percentual de clientes acertados da rota $rotaId: ${e.message}")
            0
        }
    }

    private fun obterCicloAtualRota(rotaId: Long): Triple<Int, Long?, Long?> {
        return try {
            kotlinx.coroutines.runBlocking {
                val emAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
                if (emAndamento != null) {
                    // ✅ CORREÇÃO: Ciclo em andamento - mostrar o número atual
                    Triple(emAndamento.numeroCiclo, emAndamento.id, emAndamento.dataInicio.time)
                } else {
                    // ✅ CORREÇÃO: Nenhum ciclo em andamento - mostrar o ÚLTIMO ciclo finalizado
                    val ultimoCiclo = cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)
                    if (ultimoCiclo != null) {
                        android.util.Log.d("AppRepository", "🔄 Rota $rotaId: Nenhum ciclo em andamento, último ciclo finalizado: ${ultimoCiclo.numeroCiclo}")
                        Triple(ultimoCiclo.numeroCiclo, ultimoCiclo.id, ultimoCiclo.dataFim?.time)
                    } else {
                        android.util.Log.d("AppRepository", "🆕 Rota $rotaId: Sem histórico, exibindo 1º ciclo")
                        Triple(1, null, null)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter ciclo atual da rota $rotaId: ${e.message}")
            Triple(1, null, null)
        }
    }

    // ✅ NOVO: Método para obter datas de início e fim do ciclo
    private fun obterDatasCicloRota(rotaId: Long): Pair<Long?, Long?> {
        return try {
            kotlinx.coroutines.runBlocking {
                val emAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
                if (emAndamento != null) {
                    Pair(emAndamento.dataInicio.time, emAndamento.dataFim?.time)
                } else {
                    val ultimo = cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)
                    if (ultimo != null) {
                        Pair(ultimo.dataInicio.time, ultimo.dataFim?.time)
                    } else {
                        Pair(null, null)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao obter datas do ciclo da rota $rotaId: ${e.message}")
            Pair(null, null)
        }
    }

    private fun determinarStatusRotaEmTempoReal(rotaId: Long): StatusRota {
        return try {
            kotlinx.coroutines.runBlocking {
                val emAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
                val status = if (emAndamento != null) {
                    android.util.Log.d("AppRepository", "✅ Rota $rotaId: Ciclo em andamento encontrado (ID: ${emAndamento.id}) -> EM_ANDAMENTO")
                    StatusRota.EM_ANDAMENTO
                } else {
                    android.util.Log.d("AppRepository", "✅ Rota $rotaId: Nenhum ciclo em andamento -> FINALIZADA")
                    StatusRota.FINALIZADA
                }
                status
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ Erro ao determinar status da rota $rotaId: ${e.message}")
            StatusRota.PAUSADA
        }
    }
    
    suspend fun obterRotaPorId(id: Long) = rotaDao.getRotaById(id)
    fun obterRotaPorIdFlow(id: Long) = rotaDao.obterRotaPorId(id)
    suspend fun obterRotaPorNome(nome: String) = rotaDao.getRotaByNome(nome)
    suspend fun inserirRota(rota: Rota) = rotaDao.insertRota(rota)
    suspend fun inserirRotas(rotas: List<Rota>) = rotaDao.insertRotas(rotas)
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
    suspend fun inserirDespesa(despesa: Despesa) = despesaDao.inserir(despesa)
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
    
    fun obterTodosColaboradores() = colaboradorDao?.obterTodos() ?: flowOf(emptyList())
    fun obterColaboradoresAtivos() = colaboradorDao?.obterAtivos() ?: flowOf(emptyList())
    fun obterColaboradoresAprovados() = colaboradorDao?.obterAprovados() ?: flowOf(emptyList())
    fun obterColaboradoresPendentesAprovacao() = colaboradorDao?.obterPendentesAprovacao() ?: flowOf(emptyList())
    fun obterColaboradoresPorNivelAcesso(nivelAcesso: NivelAcesso) = colaboradorDao?.obterPorNivelAcesso(nivelAcesso) ?: flowOf(emptyList())
    
    suspend fun obterColaboradorPorId(id: Long) = colaboradorDao?.obterPorId(id)
    suspend fun obterColaboradorPorEmail(email: String) = colaboradorDao?.obterPorEmail(email)
    suspend fun obterColaboradorPorFirebaseUid(firebaseUid: String) = colaboradorDao?.obterPorFirebaseUid(firebaseUid)
    suspend fun obterColaboradorPorGoogleId(googleId: String) = colaboradorDao?.obterPorGoogleId(googleId)
    
    suspend fun inserirColaborador(colaborador: Colaborador) = colaboradorDao?.inserir(colaborador) ?: 0L
    suspend fun atualizarColaborador(colaborador: Colaborador) = colaboradorDao?.atualizar(colaborador)
    suspend fun deletarColaborador(colaborador: Colaborador) = colaboradorDao?.deletar(colaborador)
    
    suspend fun aprovarColaborador(colaboradorId: Long, dataAprovacao: java.util.Date, aprovadoPor: String) = 
        colaboradorDao?.aprovarColaborador(colaboradorId, dataAprovacao, aprovadoPor)
    
    suspend fun aprovarColaboradorComCredenciais(
        colaboradorId: Long,
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        dataAprovacao: java.util.Date,
        aprovadoPor: String
    ) = colaboradorDao?.aprovarColaboradorComCredenciais(
        colaboradorId, email, senha, nivelAcesso, observacoes, dataAprovacao, aprovadoPor
    )
    suspend fun alterarStatusColaborador(colaboradorId: Long, ativo: Boolean) = 
        colaboradorDao?.alterarStatus(colaboradorId, ativo)
    suspend fun atualizarUltimoAcessoColaborador(colaboradorId: Long, dataUltimoAcesso: java.util.Date) = 
        colaboradorDao?.atualizarUltimoAcesso(colaboradorId, dataUltimoAcesso)
    
    suspend fun contarColaboradoresAtivos() = colaboradorDao?.contarAtivos() ?: 0
    suspend fun contarColaboradoresPendentesAprovacao() = colaboradorDao?.contarPendentesAprovacao() ?: 0
    
    // ==================== META COLABORADOR ====================
    
    fun obterMetasPorColaborador(colaboradorId: Long) = colaboradorDao?.obterMetasPorColaborador(colaboradorId) ?: flowOf(emptyList())
    suspend fun obterMetaAtual(colaboradorId: Long, tipoMeta: TipoMeta) = colaboradorDao?.obterMetaAtual(colaboradorId, tipoMeta)
    suspend fun inserirMeta(meta: MetaColaborador) = colaboradorDao?.inserirMeta(meta) ?: 0L
    suspend fun atualizarMeta(meta: MetaColaborador) = colaboradorDao?.atualizarMeta(meta)
    suspend fun deletarMeta(meta: MetaColaborador) = colaboradorDao?.deletarMeta(meta)
    suspend fun atualizarValorAtualMeta(metaId: Long, valorAtual: Double) = colaboradorDao?.atualizarValorAtualMeta(metaId, valorAtual)
    
    // ==================== METAS POR ROTA ====================
    
    fun obterMetasPorRota(rotaId: Long) = colaboradorDao?.obterMetasPorRota(0L, rotaId) ?: flowOf(emptyList())
    fun obterMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = colaboradorDao?.obterMetasPorCiclo(colaboradorId, cicloId) ?: flowOf(emptyList())
    fun obterMetasPorColaboradorERota(colaboradorId: Long, rotaId: Long) = colaboradorDao?.obterMetasPorRota(colaboradorId, rotaId) ?: flowOf(emptyList())
    fun obterMetasPorColaboradorCicloERota(colaboradorId: Long, cicloId: Long, rotaId: Long) = colaboradorDao?.obterMetasPorCicloERota(colaboradorId, cicloId, rotaId) ?: flowOf(emptyList())
    suspend fun desativarMetasColaborador(colaboradorId: Long) = colaboradorDao?.desativarMetasColaborador(colaboradorId)
    
    // Métodos para metas
    suspend fun buscarMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long) = colaboradorDao?.buscarMetasPorColaboradorECiclo(colaboradorId, cicloId) ?: emptyList()
    suspend fun buscarMetasPorRotaECiclo(rotaId: Long, cicloId: Long) = colaboradorDao?.buscarMetasPorRotaECiclo(rotaId, cicloId) ?: emptyList()
    
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
    
    
    fun buscarMetasAtivasPorColaborador(colaboradorId: Long) = colaboradorDao?.buscarMetasAtivasPorColaborador(colaboradorId) ?: flowOf(emptyList())
    suspend fun buscarMetasPorTipoECiclo(tipoMeta: TipoMeta, cicloId: Long) = colaboradorDao?.buscarMetasPorTipoECiclo(tipoMeta, cicloId) ?: emptyList()
    
    // ==================== COLABORADOR ROTA ====================
    
    fun obterRotasPorColaborador(colaboradorId: Long) = colaboradorDao?.obterRotasPorColaborador(colaboradorId) ?: flowOf(emptyList())
    fun obterColaboradoresPorRota(rotaId: Long) = colaboradorDao?.obterColaboradoresPorRota(rotaId) ?: flowOf(emptyList())
    suspend fun obterRotaPrincipal(colaboradorId: Long) = colaboradorDao?.obterRotaPrincipal(colaboradorId)
    suspend fun inserirColaboradorRota(colaboradorRota: ColaboradorRota) = colaboradorDao?.inserirColaboradorRota(colaboradorRota)
    suspend fun deletarColaboradorRota(colaboradorRota: ColaboradorRota) = colaboradorDao?.deletarColaboradorRota(colaboradorRota)
    suspend fun deletarTodasRotasColaborador(colaboradorId: Long) = colaboradorDao?.deletarTodasRotasColaborador(colaboradorId)
    suspend fun removerResponsavelPrincipal(colaboradorId: Long) = colaboradorDao?.removerResponsavelPrincipal(colaboradorId)
    suspend fun definirResponsavelPrincipal(colaboradorId: Long, rotaId: Long) = colaboradorDao?.definirResponsavelPrincipal(colaboradorId, rotaId)
    
    // Métodos auxiliares para vinculação de colaborador com rotas
    suspend fun removerRotasColaborador(colaboradorId: Long) = colaboradorDao?.deletarTodasRotasColaborador(colaboradorId)
    suspend fun vincularColaboradorRota(colaboradorId: Long, rotaId: Long, responsavelPrincipal: Boolean, dataVinculacao: java.util.Date) {
        val colaboradorRota = ColaboradorRota(
            colaboradorId = colaboradorId,
            rotaId = rotaId,
            responsavelPrincipal = responsavelPrincipal,
            dataVinculacao = dataVinculacao
        )
        colaboradorDao?.inserirColaboradorRota(colaboradorRota)
    }
    
    // ✅ NOVO: Método para inserir rotas de exemplo
    suspend fun inserirRotasExemplo() {
        // Por enquanto, não faz nada
        // TODO: Implementar quando necessário
    }
    
    // ==================== CICLO ACERTO ====================
    
    fun obterTodosCiclos() = cicloAcertoDao.listarTodos()
    
    suspend fun buscarUltimoCicloFinalizadoPorRota(rotaId: Long) = cicloAcertoDao.buscarUltimoCicloFinalizadoPorRota(rotaId)
    suspend fun buscarCiclosPorRotaEAno(rotaId: Long, ano: Int) = cicloAcertoDao.buscarCiclosPorRotaEAno(rotaId, ano)
    
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
                .filter { it.numeroCiclo == numeroCiclo && it.dataInicio.year == ano }
            
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
    
    // Métodos stub para sincronização
    suspend fun syncRotas(rotas: List<Rota>) {
        // TODO: Implementar lógica de sincronização de rotas
        println("Sincronizando rotas (stub): ${rotas.size} rotas")
    }

    suspend fun syncClientes(clientes: List<Cliente>) {
        // TODO: Implementar lógica de sincronização de clientes
        println("Sincronizando clientes (stub): ${clientes.size} clientes")
    }

    suspend fun syncAcertos(acertos: List<Acerto>) {
        // TODO: Implementar lógica de sincronização de acertos
        println("Sincronizando acertos (stub): ${acertos.size} acertos")
    }

    suspend fun syncColaboradores(colaboradores: List<Colaborador>) {
        // TODO: Implementar lógica de sincronização de colaboradores
        println("Sincronizando colaboradores (stub): ${colaboradores.size} colaboradores")
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
    suspend fun inserirContrato(contrato: ContratoLocacao) = contratoLocacaoDao.inserirContrato(contrato)
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
    suspend fun inserirContratoMesa(contratoMesa: ContratoMesa) = contratoLocacaoDao.inserirContratoMesa(contratoMesa)
    suspend fun inserirContratoMesas(contratoMesas: List<ContratoMesa>) = contratoLocacaoDao.inserirContratoMesas(contratoMesas)
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
    suspend fun inserirAditivo(aditivo: AditivoContrato) = aditivoContratoDao.inserirAditivo(aditivo)
    suspend fun atualizarAditivo(aditivo: AditivoContrato) = aditivoContratoDao.atualizarAditivo(aditivo)
    suspend fun excluirAditivo(aditivo: AditivoContrato) = aditivoContratoDao.excluirAditivo(aditivo)
    suspend fun buscarMesasPorAditivo(aditivoId: Long) = aditivoContratoDao.buscarMesasPorAditivo(aditivoId)
    suspend fun inserirAditivoMesas(aditivoMesas: List<AditivoMesa>) = aditivoContratoDao.inserirAditivoMesas(aditivoMesas)
    suspend fun excluirAditivoMesa(aditivoMesa: AditivoMesa) = aditivoContratoDao.excluirAditivoMesa(aditivoMesa)
    suspend fun excluirTodasMesasDoAditivo(aditivoId: Long) = aditivoContratoDao.excluirTodasMesasDoAditivo(aditivoId)
    
    // ==================== ASSINATURA REPRESENTANTE LEGAL ====================
    
    suspend fun obterAssinaturaRepresentanteLegalAtiva() = assinaturaRepresentanteLegalDao.obterAssinaturaAtiva()
    fun obterAssinaturaRepresentanteLegalAtivaFlow() = assinaturaRepresentanteLegalDao.obterAssinaturaAtivaFlow()
    suspend fun obterTodasAssinaturasRepresentanteLegal() = assinaturaRepresentanteLegalDao.obterTodasAssinaturas()
    fun obterTodasAssinaturasRepresentanteLegalFlow() = assinaturaRepresentanteLegalDao.obterTodasAssinaturasFlow()
    suspend fun obterAssinaturaRepresentanteLegalPorId(id: Long) = assinaturaRepresentanteLegalDao.obterAssinaturaPorId(id)
    suspend fun inserirAssinaturaRepresentanteLegal(assinatura: AssinaturaRepresentanteLegal) = assinaturaRepresentanteLegalDao.inserirAssinatura(assinatura)
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
    suspend fun inserirLogAuditoriaAssinatura(log: LogAuditoriaAssinatura) = logAuditoriaAssinaturaDao.inserirLog(log)
    suspend fun contarLogsAuditoriaDesde(dataInicio: java.util.Date) = logAuditoriaAssinaturaDao.contarLogsDesde(dataInicio)
    suspend fun contarUsosAssinaturaAuditoria(idAssinatura: Long) = logAuditoriaAssinaturaDao.contarUsosAssinatura(idAssinatura)
    suspend fun obterLogsAuditoriaNaoValidados() = logAuditoriaAssinaturaDao.obterLogsNaoValidados()
    suspend fun validarLogAuditoria(id: Long, dataValidacao: java.util.Date, validadoPor: String) = logAuditoriaAssinaturaDao.validarLog(id, dataValidacao, validadoPor)
    
    // ==================== PROCURAÇÕES ====================
    
    suspend fun obterProcuraçõesAtivas() = procuraçãoRepresentanteDao.obterProcuraçõesAtivas()
    fun obterProcuraçõesAtivasFlow() = procuraçãoRepresentanteDao.obterProcuraçõesAtivasFlow()
    suspend fun obterProcuraçãoPorUsuario(usuario: String) = procuraçãoRepresentanteDao.obterProcuraçãoPorUsuario(usuario)
    fun obterProcuraçãoPorUsuarioFlow(usuario: String) = procuraçãoRepresentanteDao.obterProcuraçãoPorUsuarioFlow(usuario)
    suspend fun obterProcuraçãoPorCpf(cpf: String) = procuraçãoRepresentanteDao.obterProcuraçãoPorCpf(cpf)
    suspend fun obterTodasProcurações() = procuraçãoRepresentanteDao.obterTodasProcurações()
    fun obterTodasProcuraçõesFlow() = procuraçãoRepresentanteDao.obterTodasProcuraçõesFlow()
    suspend fun obterProcuraçãoPorId(id: Long) = procuraçãoRepresentanteDao.obterProcuraçãoPorId(id)
    suspend fun obterProcuraçãoPorNumero(numero: String) = procuraçãoRepresentanteDao.obterProcuraçãoPorNumero(numero)
    suspend fun inserirProcuração(procuração: ProcuraçãoRepresentante) = procuraçãoRepresentanteDao.inserirProcuração(procuração)
    suspend fun atualizarProcuração(procuração: ProcuraçãoRepresentante) = procuraçãoRepresentanteDao.atualizarProcuração(procuração)
    suspend fun revogarProcuração(id: Long, dataRevogacao: java.util.Date, motivo: String) = procuraçãoRepresentanteDao.revogarProcuração(id, dataRevogacao, motivo)
    suspend fun contarProcuraçõesAtivas() = procuraçãoRepresentanteDao.contarProcuraçõesAtivas()
    suspend fun obterProcuraçõesValidadas() = procuraçãoRepresentanteDao.obterProcuraçõesValidadas()
    suspend fun obterProcuraçõesVencidas(dataAtual: java.util.Date) = procuraçãoRepresentanteDao.obterProcuraçõesVencidas(dataAtual)
    suspend fun validarProcuração(id: Long, dataValidacao: java.util.Date, validadoPor: String) = procuraçãoRepresentanteDao.validarProcuração(id, dataValidacao, validadoPor)
    
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
            // Consideramos "locadas" como mesas ativas vinculadas a clientes da rota
            mesaDao.buscarMesasPorRota(rotaId).first().size
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Erro ao contar mesas locadas: ${e.message}", e)
            0
        }
    }
} 