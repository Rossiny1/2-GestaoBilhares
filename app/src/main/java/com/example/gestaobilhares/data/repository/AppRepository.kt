package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.flowOf

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
    private val colaboradorDao: ColaboradorDao
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
} 