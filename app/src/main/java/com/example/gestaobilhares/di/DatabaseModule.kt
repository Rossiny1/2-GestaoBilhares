package com.example.gestaobilhares.di

import android.content.Context
import androidx.room.Room
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.dao.AcertoMesaDao
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
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
     * Implementa tratamento robusto de erros para primeira instalação.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return try {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gestao_bilhares_database"
            )
                .fallbackToDestructiveMigration() // Para desenvolvimento inicial
                .addCallback(object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        android.util.Log.d("DatabaseModule", "Banco de dados criado com sucesso")
                    }
                    
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)
                        android.util.Log.d("DatabaseModule", "Banco de dados aberto com sucesso")
                    }
                })
                .build()
        } catch (e: Exception) {
            android.util.Log.e("DatabaseModule", "Erro ao criar banco de dados: ${e.message}")
            // Em caso de erro, tentar criar com configurações mais básicas
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gestao_bilhares_database_fallback"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
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

    @Provides
    fun provideAcertoDao(appDatabase: AppDatabase): AcertoDao = appDatabase.acertoDao()

    @Provides
    fun provideAcertoRepository(acertoDao: AcertoDao): AcertoRepository = AcertoRepository(acertoDao)

    @Provides
    fun provideAcertoMesaDao(appDatabase: AppDatabase): AcertoMesaDao = appDatabase.acertoMesaDao()

    @Provides
    fun provideAcertoMesaRepository(acertoMesaDao: AcertoMesaDao): AcertoMesaRepository = AcertoMesaRepository(acertoMesaDao)
} 