package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first

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
    private val cicloAcertoDao: CicloAcertoDao
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
    fun buscarMesasPorRota(rotaId: Long) = mesaDao.buscarMesasPorRota(rotaId)
    
    // ==================== ROTA ====================
    
    fun obterTodasRotas() = rotaDao.getAllRotas()
    fun obterRotasAtivas() = rotaDao.getAllRotasAtivas()
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
    suspend fun atualizarValorAtualMeta(metaId: Long, valorAtual: Double) = colaboradorDao?.atualizarValorAtual(metaId, valorAtual)
    suspend fun desativarMetasColaborador(colaboradorId: Long) = colaboradorDao?.desativarMetasColaborador(colaboradorId)
    
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
    
    // ==================== CICLO ACERTO ====================
    
    fun obterTodosCiclos() = cicloAcertoDao.listarTodos()
    
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
    
    // Métodos para relatórios gerais
    suspend fun getCiclos(): List<CicloInfo> {
        return try {
            val ciclos = cicloAcertoDao.listarTodos().first()
            ciclos.map { ciclo ->
                CicloInfo(
                    numero = ciclo.numeroCiclo,
                    descricao = "${ciclo.numeroCiclo} Ciclo - ${ciclo.dataInicio.year}"
                )
            }.distinctBy { it.numero }.sortedBy { it.numero }
        } catch (e: Exception) {
            (1..12).map { CicloInfo(it, "$it Ciclo") }
        }
    }
    
    suspend fun getRotas(): List<Rota> {
        return try {
            val rotas = rotaDao.getAllRotas().first()
            rotas
        } catch (e: Exception) {
            emptyList()
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
    
    data class CicloInfo(
        val numero: Int,
        val descricao: String
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
} 