package com.example.gestaobilhares.sync.di

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.SyncRepository
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncRepository(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        mesaSyncHandler: com.example.gestaobilhares.sync.handlers.MesaSyncHandler,
        clienteSyncHandler: com.example.gestaobilhares.sync.handlers.ClienteSyncHandler,
        contratoSyncHandler: com.example.gestaobilhares.sync.handlers.ContratoSyncHandler,
        acertoSyncHandler: com.example.gestaobilhares.sync.handlers.AcertoSyncHandler,
        despesaSyncHandler: com.example.gestaobilhares.sync.handlers.DespesaSyncHandler,
        rotaSyncHandler: com.example.gestaobilhares.sync.handlers.RotaSyncHandler,
        cicloSyncHandler: com.example.gestaobilhares.sync.handlers.CicloSyncHandler,
        colaboradorSyncHandler: com.example.gestaobilhares.sync.handlers.ColaboradorSyncHandler,
        colaboradorRotaSyncHandler: com.example.gestaobilhares.sync.handlers.ColaboradorRotaSyncHandler,
        metaColaboradorSyncHandler: com.example.gestaobilhares.sync.handlers.MetaColaboradorSyncHandler,
        metaSyncHandler: com.example.gestaobilhares.sync.handlers.MetaSyncHandler,
        assinaturaSyncHandler: com.example.gestaobilhares.sync.handlers.AssinaturaSyncHandler,
        veiculoSyncHandler: com.example.gestaobilhares.sync.handlers.VeiculoSyncHandler,
        equipamentoSyncHandler: com.example.gestaobilhares.sync.handlers.EquipamentoSyncHandler,
        estoqueSyncHandler: com.example.gestaobilhares.sync.handlers.EstoqueSyncHandler
    ): SyncRepository {
        return SyncRepository(
            context, 
            appRepository, 
            firestore, 
            networkUtils, 
            mesaSyncHandler = mesaSyncHandler,
            clienteSyncHandler = clienteSyncHandler,
            contratoSyncHandler = contratoSyncHandler,
            acertoSyncHandler = acertoSyncHandler,
            despesaSyncHandler = despesaSyncHandler,
            rotaSyncHandler = rotaSyncHandler,
            cicloSyncHandler = cicloSyncHandler,
            colaboradorSyncHandler = colaboradorSyncHandler,
            colaboradorRotaSyncHandler = colaboradorRotaSyncHandler,
            metaColaboradorSyncHandler = metaColaboradorSyncHandler,
            metaSyncHandler = metaSyncHandler,
            assinaturaSyncHandler = assinaturaSyncHandler,
            veiculoSyncHandler = veiculoSyncHandler,
            equipamentoSyncHandler = equipamentoSyncHandler,
            estoqueSyncHandler = estoqueSyncHandler
        )
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }
}
