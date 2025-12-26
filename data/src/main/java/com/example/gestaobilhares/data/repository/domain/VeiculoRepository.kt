package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.VeiculoDao
import com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao
import com.example.gestaobilhares.data.dao.HistoricoCombustivelVeiculoDao
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * Repository especializado para operações relacionadas a veículos.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 * 
 * Responsabilidades:
 * - Histórico de manutenção de veículos
 * - Histórico de combustível de veículos
 */
class VeiculoRepository(
    private val veiculoDao: VeiculoDao?,
    private val historicoManutencaoVeiculoDao: HistoricoManutencaoVeiculoDao?,
    private val historicoCombustivelVeiculoDao: HistoricoCombustivelVeiculoDao?
) {
    
    /**
     * Obtém veículo de por ID
     */
    suspend fun obterPorId(id: Long): com.example.gestaobilhares.data.entities.Veiculo? {
        return veiculoDao?.buscarPorId(id)
    }
    
    /**
     * Obtém todos os históricos de manutenção de veículos
     * Para sincronização: busca todos os veículos e depois todos os históricos de cada um
     */
    suspend fun obterTodosHistoricoManutencaoVeiculo(): List<HistoricoManutencaoVeiculo> {
        return if (historicoManutencaoVeiculoDao != null && veiculoDao != null) {
            val veiculos = veiculoDao.listar().first()
            veiculos.flatMap { veiculo ->
                historicoManutencaoVeiculoDao.listarPorVeiculo(veiculo.id).first()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Obtém todos os históricos de combustível de veículos
     * Para sincronização: busca todos os veículos e depois todos os históricos de cada um
     */
    suspend fun obterTodosHistoricoCombustivelVeiculo(): List<HistoricoCombustivelVeiculo> {
        return if (historicoCombustivelVeiculoDao != null && veiculoDao != null) {
            val veiculos = veiculoDao.listar().first()
            veiculos.flatMap { veiculo ->
                historicoCombustivelVeiculoDao.listarPorVeiculo(veiculo.id).first()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Insere histórico de combustível
     */
    suspend fun inserirHistoricoCombustivel(historico: HistoricoCombustivelVeiculo): Long {
        return historicoCombustivelVeiculoDao?.inserir(historico) ?: 0L
    }
    
    /**
     * Insere histórico de manutenção
     */
    suspend fun inserirHistoricoManutencao(historico: HistoricoManutencaoVeiculo): Long {
        return historicoManutencaoVeiculoDao?.inserir(historico) ?: 0L
    }

    /**
     * Obtém veículo de manutenção por ID
     */
    suspend fun obterHistoricoManutencaoPorId(id: Long): HistoricoManutencaoVeiculo? {
        return historicoManutencaoVeiculoDao?.buscarPorId(id)
    }

    /**
     * Atualiza histórico de manutenção
     */
    suspend fun atualizarHistoricoManutencao(historico: HistoricoManutencaoVeiculo) {
        historicoManutencaoVeiculoDao?.atualizar(historico)
    }

    /**
     * Obtém histórico de combustível por ID
     */
    suspend fun obterHistoricoCombustivelPorId(id: Long): HistoricoCombustivelVeiculo? {
        return historicoCombustivelVeiculoDao?.buscarPorId(id)
    }

    /**
     * Atualiza histórico de combustível
     */
    suspend fun atualizarHistoricoCombustivel(historico: HistoricoCombustivelVeiculo) {
        historicoCombustivelVeiculoDao?.atualizar(historico)
    }
    
    /**
     * ✅ NOVO: Obtém todos os históricos de manutenção como Flow reativo
     * Baseado no código antigo que funcionava - retorna todos e filtra no ViewModel
     */
    fun obterTodosHistoricoManutencaoVeiculoFlow(): Flow<List<HistoricoManutencaoVeiculo>> {
        return historicoManutencaoVeiculoDao?.listarTodos() ?: flowOf(emptyList())
    }
    
    /**
     * ✅ NOVO: Obtém todos os históricos de combustível como Flow reativo
     * Baseado no código antigo que funcionava - retorna todos e filtra no ViewModel
     */
    fun obterTodosHistoricoCombustivelVeiculoFlow(): Flow<List<HistoricoCombustivelVeiculo>> {
        return historicoCombustivelVeiculoDao?.listarTodos() ?: flowOf(emptyList())
    }
    
    /**
     * ✅ NOVO: Obtém histórico de manutenção por veículo como Flow reativo
     * Para uso em ViewModels que precisam observar mudanças automaticamente
     */
    fun obterHistoricoManutencaoPorVeiculo(veiculoId: Long): Flow<List<HistoricoManutencaoVeiculo>> {
        return historicoManutencaoVeiculoDao?.listarPorVeiculo(veiculoId) ?: flowOf(emptyList())
    }
    
    /**
     * ✅ NOVO: Obtém histórico de combustível por veículo como Flow reativo
     * Para uso em ViewModels que precisam observar mudanças automaticamente
     */
    fun obterHistoricoCombustivelPorVeiculo(veiculoId: Long): Flow<List<HistoricoCombustivelVeiculo>> {
        return historicoCombustivelVeiculoDao?.listarPorVeiculo(veiculoId) ?: flowOf(emptyList())
    }
    
    /**
     * ✅ NOVO: Obtém histórico de manutenção por veículo e ano como Flow reativo
     */
    fun obterHistoricoManutencaoPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoManutencaoVeiculo>> {
        return historicoManutencaoVeiculoDao?.listarPorVeiculoEAno(veiculoId, ano) ?: flowOf(emptyList())
    }
    
    /**
     * ✅ NOVO: Obtém histórico de combustível por veículo e ano como Flow reativo
     */
    fun obterHistoricoCombustivelPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoCombustivelVeiculo>> {
        return historicoCombustivelVeiculoDao?.listarPorVeiculoEAno(veiculoId, ano) ?: flowOf(emptyList())
    }
}

