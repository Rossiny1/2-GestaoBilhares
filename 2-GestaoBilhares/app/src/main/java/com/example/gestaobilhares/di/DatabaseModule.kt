package com.example.gestaobilhares.di

import android.content.Context
import androidx.room.Room
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.repository.MesaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo do Hilt responsável por configurar e fornecer as dependências
 * relacionadas ao banco de dados Room.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Fornece a instância singleton do banco de dados Room.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "gestao_bilhares_database"
        )
            .fallbackToDestructiveMigration() // Para desenvolvimento inicial
            .build()
    }

    /**
     * Fornece o DAO de rotas.
     */
    @Provides
    fun provideRotaDao(database: AppDatabase): RotaDao {
        return database.rotaDao()
    }

    /**
     * Fornece o DAO de clientes.
     */
    @Provides
    fun provideClienteDao(database: AppDatabase): ClienteDao {
        return database.clienteDao()
    }

    /**
     * Fornece o DAO de despesas.
     */
    @Provides
    fun provideDespesaDao(database: AppDatabase): DespesaDao {
        return database.despesaDao()
    }

    @Provides
    fun provideMesaDao(appDatabase: AppDatabase): MesaDao = appDatabase.mesaDao()

    @Provides
    fun provideMesaRepository(mesaDao: MesaDao): MesaRepository = MesaRepository(mesaDao)
} 