package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.dao.CicloAcertoDao
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.entities.StatusRota
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para gerenciar dados das rotas.
 * Atua como uma única fonte de verdade para os dados das rotas.
 * Coordena entre o banco de dados local e futuras fontes remotas.
 */
@Singleton
class RotaRepository @Inject constructor(
    private val rotaDao: RotaDao,
    private val clienteDao: ClienteDao? = null,
    private val mesaDao: MesaDao? = null,
    private val acertoDao: AcertoDao? = null,
    private val cicloAcertoDao: CicloAcertoDao? = null
) {
    
    /**
     * Obtém todas as rotas ativas como Flow.
     * O Flow permite observar mudanças em tempo real.
     */
    fun getAllRotasAtivas(): Flow<List<Rota>> {
        return rotaDao.getAllRotasAtivas()
    }
    
    /**
     * Obtém todas as rotas (ativas e inativas).
     */
    fun getAllRotas(): Flow<List<Rota>> {
        return rotaDao.getAllRotas()
    }
    
    /**
     * Obtém um resumo de todas as rotas com estatísticas reais.
     * Calcula dados reais de clientes, mesas e acertos.
     * ✅ CORREÇÃO: Usa ciclo atual real baseado nos acertos
     */
    fun getRotasResumo(): Flow<List<RotaResumo>> {
        return getAllRotasAtivas().map { rotas ->
            rotas.map { rota ->
                // Usar dados reais calculados
                val clientesAtivos = calcularClientesAtivosSync(rota.id)
                val pendencias = calcularPendenciasSync(rota.id)
                val valorAcertado = calcularValorAcertadoSync(rota.id)
                val quantidadeMesas = calcularQuantidadeMesasSync(rota.id)
                val percentualAcertados = calcularPercentualAcertadosSync(rota.id, clientesAtivos)
                val status = determinarStatusRota(rota)
                
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

    /**
     * ✅ CORREÇÃO: Obtém resumo de rotas com atualização em tempo real baseada em mudanças de ciclos
     * Usa ciclo atual real baseado nos acertos
     */
    fun getRotasResumoComAtualizacaoTempoReal(): Flow<List<RotaResumo>> {
        return getAllRotasAtivas().map { rotas ->
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

    /**
     * ✅ NOVO: Sistema de notificação para mudanças de status
     * Notifica quando o status de uma rota deve ser atualizado
     */
    fun notificarMudancaStatusRota(rotaId: Long) {
        android.util.Log.d("RotaRepository", "🔄 Notificando mudança de status para rota: $rotaId")
        // O Flow irá automaticamente reagir às mudanças nos dados
    }
    
    /**
     * Calcula o número de clientes ativos de uma rota (versão síncrona)
     */
    private fun calcularClientesAtivosSync(rotaId: Long): Int {
        return try {
            clienteDao?.let { dao ->
                runBlocking { dao.obterClientesPorRota(rotaId).first().size }
            } ?: 0
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao calcular clientes ativos: ${e.message}")
            0
        }
    }
    
    /**
     * Calcula o número de pendências de uma rota (versão síncrona)
     * ✅ CORREÇÃO: Usar mesma lógica do ClientListViewModel (débito > R$300)
     */
    private fun calcularPendenciasSync(rotaId: Long): Int {
        return try {
            clienteDao?.let { dao ->
                val clientes = runBlocking { dao.obterClientesPorRota(rotaId).first() }
                // ✅ CORREÇÃO: Usar mesma lógica do card de progresso
                clientes.count { it.debitoAtual > 300.0 }
            } ?: 0
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao calcular pendências: ${e.message}")
            0
        }
    }
    
    /**
     * Calcula o valor total acertado de uma rota (versão síncrona)
     */
    private fun calcularValorAcertadoSync(rotaId: Long): Double {
        return try {
            acertoDao?.let { dao ->
                val clientes = clienteDao?.let { clienteDao ->
                    runBlocking { clienteDao.obterClientesPorRota(rotaId).first() }
                } ?: emptyList()
                val acertos = clientes.flatMap { cliente ->
                    runBlocking { dao.buscarPorCliente(cliente.id).first() }
                }
                acertos.sumOf { it.valorRecebido }
            } ?: 0.0
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao calcular valor acertado: ${e.message}")
            0.0
        }
    }
    
    /**
     * Calcula a quantidade de mesas de uma rota (versão síncrona)
     */
    private fun calcularQuantidadeMesasSync(rotaId: Long): Int {
        return try {
            mesaDao?.let { dao ->
                runBlocking { dao.buscarMesasPorRota(rotaId).first().size }
            } ?: 0
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao calcular quantidade de mesas: ${e.message}")
            0
        }
    }
    
    /**
     * Calcula o percentual de clientes que acertaram (versão síncrona)
     */
    private fun calcularPercentualAcertadosSync(rotaId: Long, totalClientes: Int): Int {
        return try {
            if (totalClientes == 0) return 0
            
            acertoDao?.let { dao ->
                val clientes = clienteDao?.let { clienteDao ->
                    runBlocking { clienteDao.obterClientesPorRota(rotaId).first() }
                } ?: emptyList()
                val clientesComAcerto = clientes.count { cliente ->
                    runBlocking { dao.buscarPorCliente(cliente.id).first().isNotEmpty() }
                }
                ((clientesComAcerto.toDouble() / totalClientes) * 100).toInt()
            } ?: 0
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao calcular percentual acertados: ${e.message}")
            0
        }
    }
    
    /**
     * ✅ NOVO: Calcula o ciclo atual real baseado nos acertos existentes
     */
    private fun calcularCicloAtualReal(rotaId: Long): Int {
        return try {
            // Primeiro, verificar se há ciclo em andamento
            val cicloAtivo = runBlocking {
                cicloAcertoDao?.buscarCicloEmAndamento(rotaId)
            }
            
            if (cicloAtivo != null) {
                // Se há ciclo em andamento, usar o número dele
                cicloAtivo.numeroCiclo
            } else {
                // Se não há ciclo em andamento, buscar o último ciclo finalizado
                val ultimoCiclo = runBlocking {
                    cicloAcertoDao?.buscarUltimoCicloPorRota(rotaId)
                }
                
                if (ultimoCiclo != null) {
                    // Se há ciclos finalizados, o próximo ciclo seria o último + 1
                    ultimoCiclo.numeroCiclo + 1
                } else {
                    // Se não há nenhum ciclo, começar do 1
                    1
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao calcular ciclo atual: ${e.message}")
            1
        }
    }

    /**
     * ✅ NOVO: Obtém a data do ciclo atual real
     */
    private fun obterDataCicloAtual(rotaId: Long): Long? {
        return try {
            // Primeiro, verificar se há ciclo em andamento
            val cicloAtivo = runBlocking {
                cicloAcertoDao?.buscarCicloEmAndamento(rotaId)
            }
            
            if (cicloAtivo != null) {
                // Se há ciclo em andamento, usar a data de início dele
                cicloAtivo.dataInicio?.time
            } else {
                // Se não há ciclo em andamento, buscar o último ciclo finalizado
                val ultimoCiclo = runBlocking {
                    cicloAcertoDao?.buscarUltimoCicloPorRota(rotaId)
                }
                
                if (ultimoCiclo != null) {
                    // Se há ciclos finalizados, usar a data de início do último
                    ultimoCiclo.dataInicio?.time
                } else {
                    // Se não há nenhum ciclo, retornar null
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao obter data do ciclo: ${e.message}")
            null
        }
    }

    // ✅ NOVO: Método para obter datas de início e fim do ciclo
    private fun obterDatasCicloRota(rotaId: Long): Pair<Long?, Long?> {
        return try {
            // Primeiro, verificar se há ciclo em andamento
            val cicloAtivo = runBlocking {
                cicloAcertoDao?.buscarCicloEmAndamento(rotaId)
            }
            
            if (cicloAtivo != null) {
                // Se há ciclo em andamento, usar as datas dele
                Pair(cicloAtivo.dataInicio?.time, cicloAtivo.dataFim?.time)
            } else {
                // Se não há ciclo em andamento, buscar o último ciclo finalizado
                val ultimoCiclo = runBlocking {
                    cicloAcertoDao?.buscarUltimoCicloPorRota(rotaId)
                }
                
                if (ultimoCiclo != null) {
                    // Se há ciclos finalizados, usar as datas do último
                    Pair(ultimoCiclo.dataInicio?.time, ultimoCiclo.dataFim?.time)
                } else {
                    // Se não há nenhum ciclo, retornar null
                    Pair(null, null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao obter datas do ciclo: ${e.message}")
            Pair(null, null)
        }
    }

    /**
     * ✅ CORREÇÃO: Determina o status da rota baseado em seus dados
     * Agora apenas 2 status: EM_ANDAMENTO ou FINALIZADA
     */
    private fun determinarStatusRota(rota: Rota): StatusRota {
        return try {
            // Verificar se há um ciclo em andamento para esta rota
            val temCicloAtivo = runBlocking {
                val cicloAtivo = cicloAcertoDao?.buscarCicloEmAndamento(rota.id)
                cicloAtivo != null
            }
            
            if (temCicloAtivo) {
                StatusRota.EM_ANDAMENTO
            } else {
                // Se não há ciclo em andamento, verificar se há ciclos finalizados
                val temCiclosFinalizados = runBlocking {
                    val ciclos = cicloAcertoDao?.listarPorRota(rota.id)?.first() ?: emptyList()
                    ciclos.any { it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO }
                }
                
                if (temCiclosFinalizados) {
                    StatusRota.FINALIZADA
                } else {
                    // Se não há nenhum ciclo, considerar como finalizada (não pausada)
                    StatusRota.FINALIZADA
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao determinar status: ${e.message}")
            StatusRota.FINALIZADA
        }
    }

    /**
     * ✅ CORREÇÃO: Determina o status da rota em tempo real baseado nos ciclos ativos
     * Agora apenas 2 status: EM_ANDAMENTO ou FINALIZADA
     */
    private fun determinarStatusRotaEmTempoReal(rotaId: Long): StatusRota {
        return try {
            // Verificar se há um ciclo em andamento para esta rota
            val temCicloAtivo = runBlocking {
                val cicloAtivo = cicloAcertoDao?.buscarCicloEmAndamento(rotaId)
                cicloAtivo != null
            }
            
            if (temCicloAtivo) {
                StatusRota.EM_ANDAMENTO
            } else {
                // Se não há ciclo em andamento, verificar se há ciclos finalizados
                val temCiclosFinalizados = runBlocking {
                    val ciclos = cicloAcertoDao?.listarPorRota(rotaId)?.first() ?: emptyList()
                    ciclos.any { it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO }
                }
                
                if (temCiclosFinalizados) {
                    StatusRota.FINALIZADA
                } else {
                    // Se não há nenhum ciclo, considerar como finalizada (não pausada)
                    StatusRota.FINALIZADA
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao determinar status em tempo real: ${e.message}")
            StatusRota.FINALIZADA
        }
    }
    
    /**
     * Obtém uma rota específica por ID.
     */
    suspend fun getRotaById(rotaId: Long): Rota? {
        return rotaDao.getRotaById(rotaId)
    }
    
    /**
     * Obtém uma rota específica por ID como Flow.
     */
    fun obterRotaPorId(rotaId: Long): Flow<Rota?> {
        return rotaDao.obterRotaPorId(rotaId)
    }
    
    /**
     * Obtém uma rota por nome (útil para validação).
     */
    suspend fun getRotaByNome(nome: String): Rota? {
        return rotaDao.getRotaByNome(nome)
    }
    
    /**
     * Insere uma nova rota.
     * @param rota A rota a ser inserida
     * @return O ID da rota inserida ou null se houve erro
     */
    suspend fun insertRota(rota: Rota): Long? {
        // ✅ LOG DETALHADO PARA RASTREAR INSERÇÃO DE ROTAS
        val stackTrace = Thread.currentThread().stackTrace
        android.util.Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
        android.util.Log.w("🔍 DB_POPULATION", "🚨 INSERINDO ROTA: ${rota.nome}")
        android.util.Log.w("🔍 DB_POPULATION", "📍 Chamado por:")
        stackTrace.take(10).forEachIndexed { index, element ->
            android.util.Log.w("🔍 DB_POPULATION", "   [$index] $element")
        }
        android.util.Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
        
        return try {
            // Verifica se já existe uma rota com o mesmo nome
            if (rotaDao.existeRotaComNome(rota.nome) > 0) {
                android.util.Log.w("🔍 DB_POPULATION", "⚠️ ROTA JÁ EXISTE: ${rota.nome}")
                return null // Rota já existe
            }
            
            val rotaComTimestamp = rota.copy(
                dataCriacao = System.currentTimeMillis(),
                dataAtualizacao = System.currentTimeMillis()
            )
            
            val id = rotaDao.insertRota(rotaComTimestamp)
            android.util.Log.w("🔍 DB_POPULATION", "✅ ROTA INSERIDA COM SUCESSO: ${rota.nome} (ID: $id)")
            id
        } catch (e: Exception) {
            android.util.Log.e("🔍 DB_POPULATION", "❌ ERRO AO INSERIR ROTA: ${rota.nome}", e)
            null
        }
    }
    
    /**
     * Atualiza uma rota existente.
     */
    suspend fun updateRota(rota: Rota): Boolean {
        return try {
            // Verifica se existe outra rota com o mesmo nome
            if (rotaDao.existeRotaComNome(rota.nome, rota.id) > 0) {
                return false // Já existe outra rota com esse nome
            }
            
            val rotaAtualizada = rota.copy(
                dataAtualizacao = System.currentTimeMillis()
            )
            
            rotaDao.updateRota(rotaAtualizada)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Desativa uma rota (soft delete).
     */
    suspend fun desativarRota(rotaId: Long): Boolean {
        return try {
            rotaDao.desativarRota(rotaId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Ativa uma rota novamente.
     */
    suspend fun ativarRota(rotaId: Long): Boolean {
        return try {
            rotaDao.ativarRota(rotaId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verifica se uma rota com o nome especificado já existe.
     */
    suspend fun existeRotaComNome(nome: String, excludeId: Long = 0): Boolean {
        return rotaDao.existeRotaComNome(nome, excludeId) > 0
    }
    
    /**
     * Conta o total de rotas ativas.
     */
    suspend fun contarRotasAtivas(): Int {
        return rotaDao.contarRotasAtivas()
    }
    
    
    /**
     * Atualiza o status da rota.
     */
    suspend fun atualizarStatusRota(rotaId: Long, status: StatusRota): Boolean {
        return try {
            rotaDao.atualizarStatus(rotaId, status.name)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Inicia um novo ciclo de acerto para a rota.
     */
    suspend fun iniciarCicloRota(rotaId: Long, numeroCiclo: Int): Boolean {
        return try {
            val dataInicio = System.currentTimeMillis()
            rotaDao.iniciarCicloRota(rotaId, numeroCiclo, dataInicio)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Finaliza o ciclo atual da rota.
     */
    suspend fun finalizarCicloRota(rotaId: Long): Boolean {
        return try {
            val dataFim = System.currentTimeMillis()
            rotaDao.finalizarCicloRota(rotaId, dataFim)
            true
        } catch (e: Exception) {
            false
        }
    }
} 
