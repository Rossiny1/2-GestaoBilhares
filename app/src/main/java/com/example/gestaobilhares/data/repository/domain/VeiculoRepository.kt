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
}

