package com.example.gestaobilhares.data.factory

import android.content.Context
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.repository.domain.SyncRepository

/**
 * Factory para criar instâncias de repositories.
 * Centraliza a criação de repositories para facilitar manutenção.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
object RepositoryFactory {
    
    /**
     * Obtém uma instância do AppRepository.
     * Cria o banco de dados e retorna o repository configurado.
     */
    fun getAppRepository(context: Context): AppRepository {
        val database = AppDatabase.getDatabase(context)
        return AppRepository.create(database)
    }
    
    /**
     * Obtém uma instância do SyncRepository.
     * Cria o repository de sincronização com AppRepository como dependência.
     */
    fun getSyncRepository(context: Context): SyncRepository {
        val appRepository = getAppRepository(context)
        return SyncRepository(context, appRepository)
    }
}

