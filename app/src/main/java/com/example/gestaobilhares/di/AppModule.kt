package com.example.gestaobilhares.di

import android.content.Context
import androidx.room.Room
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.DespesaRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.utils.UserSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides fun provideClienteDao(db: AppDatabase): ClienteDao = db.clienteDao()
    @Provides fun provideAcertoDao(db: AppDatabase): AcertoDao = db.acertoDao()
    @Provides fun provideMesaDao(db: AppDatabase): MesaDao = db.mesaDao()
    @Provides fun provideRotaDao(db: AppDatabase): RotaDao = db.rotaDao()
    @Provides fun provideDespesaDao(db: AppDatabase): DespesaDao = db.despesaDao()
    @Provides fun provideColaboradorDao(db: AppDatabase): ColaboradorDao = db.colaboradorDao()
    @Provides fun provideCicloAcertoDao(db: AppDatabase): CicloAcertoDao = db.cicloAcertoDao()
    @Provides fun provideAcertoMesaDao(db: AppDatabase): com.example.gestaobilhares.data.dao.AcertoMesaDao = db.acertoMesaDao()
    @Provides fun provideCategoriaDespesaDao(db: AppDatabase): com.example.gestaobilhares.data.dao.CategoriaDespesaDao = db.categoriaDespesaDao()
    @Provides fun provideTipoDespesaDao(db: AppDatabase): com.example.gestaobilhares.data.dao.TipoDespesaDao = db.tipoDespesaDao()
    @Provides fun provideContratoLocacaoDao(db: AppDatabase): com.example.gestaobilhares.data.dao.ContratoLocacaoDao = db.contratoLocacaoDao()

    @Provides
    @Singleton
    fun provideAppRepository(
        clienteDao: ClienteDao,
        acertoDao: AcertoDao,
        mesaDao: MesaDao,
        rotaDao: RotaDao,
        despesaDao: DespesaDao,
        colaboradorDao: ColaboradorDao,
        cicloAcertoDao: CicloAcertoDao,
        acertoMesaDao: com.example.gestaobilhares.data.dao.AcertoMesaDao,
        contratoLocacaoDao: com.example.gestaobilhares.data.dao.ContratoLocacaoDao
    ): AppRepository = AppRepository(
        clienteDao,
        acertoDao,
        mesaDao,
        rotaDao,
        despesaDao,
        colaboradorDao,
        cicloAcertoDao,
        acertoMesaDao,
        contratoLocacaoDao
    )

    // Repositories
    @Provides
    @Singleton
    fun provideClienteRepository(
        clienteDao: ClienteDao
    ): ClienteRepository = ClienteRepository(clienteDao)

    @Provides
    @Singleton
    fun provideAcertoRepository(
        acertoDao: AcertoDao,
        clienteDao: ClienteDao
    ): AcertoRepository = AcertoRepository(acertoDao, clienteDao)

    @Provides
    @Singleton
    fun provideDespesaRepository(
        despesaDao: DespesaDao
    ): DespesaRepository = DespesaRepository(despesaDao)

    @Provides
    @Singleton
    fun provideCicloAcertoRepository(
        cicloAcertoDao: CicloAcertoDao,
        despesaRepository: DespesaRepository,
        acertoRepository: AcertoRepository,
        clienteRepository: ClienteRepository,
        rotaDao: RotaDao
    ): CicloAcertoRepository = CicloAcertoRepository(
        cicloAcertoDao = cicloAcertoDao,
        despesaRepository = despesaRepository,
        acertoRepository = acertoRepository,
        clienteRepository = clienteRepository,
        rotaDao = rotaDao
    )

    @Provides
    @Singleton
    fun provideUserSessionManager(@ApplicationContext context: Context): UserSessionManager =
        UserSessionManager.getInstance(context)
}


