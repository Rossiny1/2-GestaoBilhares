package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.entities.StatusRota
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository para gerenciar dados das rotas.
 * Atua como uma única fonte de verdade para os dados das rotas.
 * Coordena entre o banco de dados local e futuras fontes remotas.
 */
class RotaRepository(
    private val rotaDao: RotaDao
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
     * Obtém um resumo de todas as rotas com estatísticas simuladas.
     * TODO: Implementar cálculos reais quando as outras entidades estiverem prontas.
     */
    fun getRotasResumo(): Flow<List<RotaResumo>> {
        return getAllRotasAtivas().map { rotas ->
            rotas.mapIndexed { index, rota ->
                // Dados simulados até implementarmos os cálculos reais
                RotaResumo(
                    rota = rota,
                    clientesAtivos = when (index % 4) {
                        0 -> 32
                        1 -> 28  
                        2 -> 35
                        else -> 25
                    },
                    pendencias = when (index % 3) {
                        0 -> 8
                        1 -> 5
                        else -> 3
                    },
                    valorAcertado = when (index % 5) {
                        0 -> 2850.50
                        1 -> 1920.00
                        2 -> 3210.75
                        3 -> 1550.25
                        else -> 2100.00
                    },
                    quantidadeMesas = when (index % 6) {
                        0 -> 45  // Rota com muitas mesas
                        1 -> 32  // Rota média
                        2 -> 68  // Rota grande
                        3 -> 28  // Rota pequena
                        4 -> 52  // Rota média-grande
                        else -> 38  // Rota padrão
                    },
                    percentualAcertados = when (index % 7) {
                        0 -> 85  // 85% dos clientes acertaram
                        1 -> 78  // 78% dos clientes acertaram
                        2 -> 92  // 92% dos clientes acertaram
                        3 -> 67  // 67% dos clientes acertaram
                        4 -> 88  // 88% dos clientes acertaram
                        5 -> 73  // 73% dos clientes acertaram
                        else -> 81  // 81% dos clientes acertaram
                    },
                    status = when (index % 3) {
                        0 -> StatusRota.EM_ANDAMENTO
                        1 -> StatusRota.CONCLUIDA
                        else -> StatusRota.EM_ANDAMENTO
                    },
                    cicloAtual = rota.cicloAcertoAtual,
                    dataCiclo = rota.dataInicioCiclo
                )
            }
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
