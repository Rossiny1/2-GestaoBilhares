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
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.MesaVendidaRepository
import com.example.gestaobilhares.data.repository.MesaReformadaRepository
import com.example.gestaobilhares.data.repository.HistoricoManutencaoMesaRepository
import com.example.gestaobilhares.data.repository.VeiculoRepository
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
    @Provides fun provideAditivoContratoDao(db: AppDatabase): com.example.gestaobilhares.data.dao.AditivoContratoDao = db.aditivoContratoDao()
    
    @Provides fun provideAssinaturaRepresentanteLegalDao(db: AppDatabase): AssinaturaRepresentanteLegalDao = db.assinaturaRepresentanteLegalDao()
    @Provides fun provideLogAuditoriaAssinaturaDao(db: AppDatabase): LogAuditoriaAssinaturaDao = db.logAuditoriaAssinaturaDao()
    @Provides fun provideProcuraçãoRepresentanteDao(db: AppDatabase): ProcuraçãoRepresentanteDao = db.procuraçãoRepresentanteDao()
    
    // ✅ NOVO: SISTEMA DE VENDA DE MESAS
    @Provides fun provideMesaVendidaDao(db: AppDatabase): com.example.gestaobilhares.data.dao.MesaVendidaDao = db.mesaVendidaDao()
    
    // ✅ NOVO: SISTEMA DE REFORMA DE MESAS
    @Provides fun provideMesaReformadaDao(db: AppDatabase): com.example.gestaobilhares.data.dao.MesaReformadaDao = db.mesaReformadaDao()
    @Provides fun providePanoEstoqueDao(db: AppDatabase): com.example.gestaobilhares.data.dao.PanoEstoqueDao = db.panoEstoqueDao()
    
    // ✅ NOVO: SISTEMA DE HISTÓRICO DE MANUTENÇÃO DAS MESAS
    @Provides fun provideHistoricoManutencaoMesaDao(db: AppDatabase): com.example.gestaobilhares.data.dao.HistoricoManutencaoMesaDao = db.historicoManutencaoMesaDao()
    // ✅ NOVO: DAO de Veículos
    @Provides fun provideVeiculoDao(db: AppDatabase): com.example.gestaobilhares.data.dao.VeiculoDao = db.veiculoDao()
    
    // ✅ NOVO: DAOs de Histórico de Veículos
    @Provides fun provideHistoricoManutencaoVeiculoDao(db: AppDatabase): com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao = db.historicoManutencaoVeiculoDao()
    @Provides fun provideHistoricoCombustivelVeiculoDao(db: AppDatabase): com.example.gestaobilhares.data.dao.HistoricoCombustivelVeiculoDao = db.historicoCombustivelVeiculoDao()

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
        contratoLocacaoDao: com.example.gestaobilhares.data.dao.ContratoLocacaoDao,
        aditivoContratoDao: com.example.gestaobilhares.data.dao.AditivoContratoDao,
        assinaturaRepresentanteLegalDao: AssinaturaRepresentanteLegalDao,
        logAuditoriaAssinaturaDao: LogAuditoriaAssinaturaDao,
        procuraçãoRepresentanteDao: ProcuraçãoRepresentanteDao
    ): AppRepository = AppRepository(
        clienteDao,
        acertoDao,
        mesaDao,
        rotaDao,
        despesaDao,
        colaboradorDao,
        cicloAcertoDao,
        acertoMesaDao,
        contratoLocacaoDao,
        aditivoContratoDao,
        assinaturaRepresentanteLegalDao,
        logAuditoriaAssinaturaDao,
        procuraçãoRepresentanteDao
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

    // ✅ NOVO: SISTEMA DE VENDA DE MESAS - Repositories
    @Provides
    @Singleton
    fun provideMesaRepository(
        mesaDao: MesaDao
    ): MesaRepository = MesaRepository(mesaDao)

    @Provides
    @Singleton
    fun provideMesaVendidaRepository(
        mesaVendidaDao: com.example.gestaobilhares.data.dao.MesaVendidaDao
    ): MesaVendidaRepository = MesaVendidaRepository(mesaVendidaDao)

    // ✅ NOVO: SISTEMA DE REFORMA DE MESAS - Repositories
    @Provides
    @Singleton
    fun provideMesaReformadaRepository(
        mesaReformadaDao: com.example.gestaobilhares.data.dao.MesaReformadaDao
    ): MesaReformadaRepository = MesaReformadaRepository(mesaReformadaDao)

    // ✅ NOVO: SISTEMA DE HISTÓRICO DE MANUTENÇÃO DAS MESAS - Repositories
    @Provides
    @Singleton
    fun provideHistoricoManutencaoMesaRepository(
        historicoManutencaoMesaDao: com.example.gestaobilhares.data.dao.HistoricoManutencaoMesaDao
    ): HistoricoManutencaoMesaRepository = HistoricoManutencaoMesaRepository(historicoManutencaoMesaDao)

    // ✅ NOVO: Repositório de Veículos
    @Provides
    @Singleton
    fun provideVeiculoRepository(
        veiculoDao: com.example.gestaobilhares.data.dao.VeiculoDao
    ): VeiculoRepository = VeiculoRepository(veiculoDao)
    
    // ✅ NOVO: Repositórios de Histórico de Veículos
    @Provides
    @Singleton
    fun provideHistoricoManutencaoVeiculoRepository(
        historicoManutencaoVeiculoDao: com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao
    ): com.example.gestaobilhares.data.repository.HistoricoManutencaoVeiculoRepository = 
        com.example.gestaobilhares.data.repository.HistoricoManutencaoVeiculoRepository(historicoManutencaoVeiculoDao)
    
    @Provides
    @Singleton
    fun provideHistoricoCombustivelVeiculoRepository(
        historicoCombustivelVeiculoDao: com.example.gestaobilhares.data.dao.HistoricoCombustivelVeiculoDao
    ): com.example.gestaobilhares.data.repository.HistoricoCombustivelVeiculoRepository = 
        com.example.gestaobilhares.data.repository.HistoricoCombustivelVeiculoRepository(historicoCombustivelVeiculoDao)
}


