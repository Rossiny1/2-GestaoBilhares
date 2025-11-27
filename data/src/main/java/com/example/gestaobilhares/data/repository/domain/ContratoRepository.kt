package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.ContratoLocacaoDao
import com.example.gestaobilhares.data.dao.AditivoContratoDao
import com.example.gestaobilhares.data.entities.AditivoMesa
import com.example.gestaobilhares.data.entities.ContratoMesa
import kotlinx.coroutines.flow.first

/**
 * Repository especializado para operações relacionadas a contratos.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 * 
 * Responsabilidades:
 * - Vinculações aditivo-mesa
 * - Vinculações contrato-mesa
 */
class ContratoRepository(
    private val contratoLocacaoDao: ContratoLocacaoDao,
    private val aditivoContratoDao: AditivoContratoDao
) {
    
    /**
     * Obtém todos os AditivoMesa
     * Para sincronização: busca todos os aditivos e depois todas as mesas de cada um
     */
    suspend fun obterTodosAditivoMesas(): List<AditivoMesa> {
        val aditivos = aditivoContratoDao.buscarTodosAditivos().first()
        return aditivos.flatMap { aditivo ->
            aditivoContratoDao.buscarMesasPorAditivo(aditivo.id)
        }
    }
    
    /**
     * Obtém todos os ContratoMesa
     * Para sincronização: busca todos os contratos e depois todas as mesas de cada um
     */
    suspend fun obterTodosContratoMesas(): List<ContratoMesa> {
        val contratos = contratoLocacaoDao.buscarTodosContratos().first()
        return contratos.flatMap { contrato ->
            contratoLocacaoDao.buscarMesasPorContrato(contrato.id)
        }
    }
}

