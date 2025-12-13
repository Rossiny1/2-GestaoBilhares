package com.example.gestaobilhares.data.di

import android.content.Context
import androidx.room.Room
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideClienteDao(database: AppDatabase): ClienteDao {
        return database.clienteDao()
    }

    @Provides
    fun provideRotaDao(database: AppDatabase): RotaDao {
        return database.rotaDao()
    }
    
    @Provides
    fun provideAcertoDao(database: AppDatabase): AcertoDao {
        return database.acertoDao()
    }
    
    @Provides
    fun provideDespesaDao(database: AppDatabase): DespesaDao {
        return database.despesaDao()
    }
    
    @Provides
    fun provideColaboradorDao(database: AppDatabase): ColaboradorDao {
        return database.colaboradorDao()
    }
    
    @Provides
    fun provideCicloDao(database: AppDatabase): CicloAcertoDao {
        return database.cicloAcertoDao()
    }
    
    @Provides
    fun provideMesaDao(database: AppDatabase): MesaDao {
        return database.mesaDao()
    }
    
    @Provides
    fun provideVeiculoDao(database: AppDatabase): VeiculoDao {
        return database.veiculoDao()
    }
    
    @Provides
    fun provideContratoLocacaoDao(database: AppDatabase): ContratoLocacaoDao {
        return database.contratoLocacaoDao()
    }
    
    @Provides
    fun provideMetaDao(database: AppDatabase): MetaDao {
        return database.metaDao()
    }
    
    /*
    @Provides
    fun providePanoDao(database: AppDatabase): PanoDao {
        return database.panoDao()
    }
    */

    @Provides
    fun provideEquipmentDao(database: AppDatabase): EquipmentDao {
        return database.equipmentDao()
    }

    /*
    @Provides
    fun provideMetaColaboradorDao(database: AppDatabase): MetaColaboradorDao {
        return database.metaColaboradorDao()
    }
    */
    
    @Provides
    fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}
