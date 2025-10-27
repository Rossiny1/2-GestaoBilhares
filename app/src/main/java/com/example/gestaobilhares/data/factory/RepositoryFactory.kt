package com.example.gestaobilhares.data.factory

import android.content.Context
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository

/**
 * Factory para criação centralizada do AppRepository
 * Segue o padrão Factory recomendado pelas práticas oficiais do Android
 * 
 * Benefícios:
 * - Elimina duplicação de código
 * - Centraliza a inicialização
 * - Facilita manutenção
 * - Segue Single Responsibility Principle
 */
object RepositoryFactory {
    
    private var _appRepository: AppRepository? = null
    
    /**
     * Obtém uma instância do AppRepository
     * Usa Singleton pattern para evitar múltiplas instâncias
     */
    fun getAppRepository(context: Context): AppRepository {
        return _appRepository ?: createAppRepository(context).also { 
            _appRepository = it 
        }
    }
    
    /**
     * Cria uma nova instância do AppRepository
     * Método privado para encapsular a lógica de criação
     */
    private fun createAppRepository(context: Context): AppRepository {
        val database = AppDatabase.getDatabase(context)
        return AppRepository(
            clienteDao = database.clienteDao(),
            acertoDao = database.acertoDao(),
            mesaDao = database.mesaDao(),
            // ✅ FASE 3C: DAOs de sincronização
            syncLogDao = database.syncLogDao(),
            syncQueueDao = database.syncQueueDao(),
            syncConfigDao = database.syncConfigDao(),
            // ✅ FASE 4C: Context para WorkManager
            context = context,
            rotaDao = database.rotaDao(),
            despesaDao = database.despesaDao(),
            colaboradorDao = database.colaboradorDao(),
            cicloAcertoDao = database.cicloAcertoDao(),
            acertoMesaDao = database.acertoMesaDao(),
            contratoLocacaoDao = database.contratoLocacaoDao(),
            aditivoContratoDao = database.aditivoContratoDao(),
            assinaturaRepresentanteLegalDao = database.assinaturaRepresentanteLegalDao(),
            logAuditoriaAssinaturaDao = database.logAuditoriaAssinaturaDao(),
            panoEstoqueDao = database.panoEstoqueDao(),
            mesaVendidaDao = database.mesaVendidaDao(),
            stockItemDao = database.stockItemDao(),
            veiculoDao = database.veiculoDao(),
            categoriaDespesaDao = database.categoriaDespesaDao(),
            tipoDespesaDao = database.tipoDespesaDao(),
            historicoManutencaoVeiculoDao = database.historicoManutencaoVeiculoDao(),
            historicoCombustivelVeiculoDao = database.historicoCombustivelVeiculoDao(),
            historicoManutencaoMesaDao = database.historicoManutencaoMesaDao()
        )
    }
    
    /**
     * Limpa a instância do repositório
     * Útil para testes ou quando necessário recriar
     */
    fun clearRepository() {
        _appRepository = null
    }
}
