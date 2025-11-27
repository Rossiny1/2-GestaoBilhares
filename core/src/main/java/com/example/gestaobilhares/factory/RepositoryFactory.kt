package com.example.gestaobilhares.factory

import android.content.Context
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository

/**
 * Factory para criar instâncias de repositories.
 * Centraliza a criação de repositories para facilitar manutenção.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 * 
 * ✅ MOVIDO para :core sem dependência de :sync:
 * - :core pode depender apenas de :data (sem ciclo)
 * - :ui e :app podem depender de :core
 * - getSyncRepository() deve ser criado diretamente onde necessário (em :app ou :ui)
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
}

