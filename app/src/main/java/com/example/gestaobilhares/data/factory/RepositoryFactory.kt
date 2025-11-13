package com.example.gestaobilhares.data.factory

import android.content.Context
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository

/**
 * Factory para criar instâncias de repositories.
 * Centraliza a criação de repositories para facilitar manutenção.
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

