package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.entities.StatusRota
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.runBlocking

/**
 * Repository para gerenciar dados das rotas.
 * Atua como uma única fonte de verdade para os dados das rotas.
 * Coordena entre o banco de dados local e futuras fontes remotas.
 */
class RotaRepository(
    private val rotaDao: RotaDao,
    private val clienteDao: ClienteDao? = null,
    private val mesaDao: MesaDao? = null,
    private val acertoDao: AcertoDao? = null
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
                
                RotaResumo(
                    rota = rota,
                    clientesAtivos = clientesAtivos,
                    pendencias = pendencias,
                    valorAcertado = valorAcertado,
                    quantidadeMesas = quantidadeMesas,
                    percentualAcertados = percentualAcertados,
                    status = status,
                    cicloAtual = rota.cicloAcertoAtual,
                    dataCiclo = rota.dataInicioCiclo
                )
            }
        }
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
     */
    private fun calcularPendenciasSync(rotaId: Long): Int {
        return try {
            clienteDao?.let { dao ->
                val clientes = runBlocking { dao.obterClientesPorRota(rotaId).first() }
                clientes.count { it.debitoAtual > 0 }
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
     * Determina o status da rota baseado em seus dados
     */
    private fun determinarStatusRota(rota: Rota): StatusRota {
        return try {
            // Por enquanto, usar lógica simples baseada no ciclo
            if (rota.cicloAcertoAtual > 0) {
                StatusRota.EM_ANDAMENTO
            } else {
                StatusRota.CONCLUIDA
            }
        } catch (e: Exception) {
            android.util.Log.e("RotaRepository", "Erro ao determinar status: ${e.message}")
            StatusRota.EM_ANDAMENTO
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
        return try {
            // Verifica se já existe uma rota com o mesmo nome
            if (rotaDao.existeRotaComNome(rota.nome) > 0) {
                return null // Rota já existe
            }
            
            val rotaComTimestamp = rota.copy(
                dataCriacao = System.currentTimeMillis(),
                dataAtualizacao = System.currentTimeMillis()
            )
            
            rotaDao.insertRota(rotaComTimestamp)
        } catch (e: Exception) {
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
     * Insere rotas de exemplo para demonstração.
     * Este método deve ser chamado apenas para popular o banco com dados iniciais.
     */
    suspend fun inserirRotasExemplo() {
        val rotasExemplo = listOf(
            Rota(nome = "Zona Sul", descricao = "Região sul da cidade", cor = "#6200EA"),
            Rota(nome = "Zona Norte", descricao = "Região norte da cidade", cor = "#03DAC6"),
            Rota(nome = "Centro", descricao = "Região central da cidade", cor = "#FF6200"),
            Rota(nome = "Zona Oeste", descricao = "Região oeste da cidade", cor = "#9C27B0"),
            Rota(nome = "Zona Leste", descricao = "Região leste da cidade", cor = "#4CAF50")
        )
        
        // Verifica se já existem rotas antes de inserir
        if (contarRotasAtivas() == 0) {
            try {
                rotaDao.insertRotas(rotasExemplo)
            } catch (e: Exception) {
                // Ignora erro se as rotas já existirem
            }
        }
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
