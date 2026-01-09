package com.example.gestaobilhares.sync.di

import android.content.Context
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.*
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.sync.utils.SyncUtils
import com.example.gestaobilhares.sync.core.SyncCore
import com.example.gestaobilhares.sync.orchestration.SyncOrchestration
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
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    // Sync Handlers
    @Provides
    @Singleton
    fun provideMesaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): MesaSyncHandler {
        return MesaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideClienteSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): ClienteSyncHandler {
        return ClienteSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideContratoSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): ContratoSyncHandler {
        return ContratoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideAcertoSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): AcertoSyncHandler {
        return AcertoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideDespesaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): DespesaSyncHandler {
        return DespesaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideRotaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): RotaSyncHandler {
        return RotaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideCicloSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): CicloSyncHandler {
        return CicloSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideColaboradorSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): ColaboradorSyncHandler {
        return ColaboradorSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideColaboradorRotaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): ColaboradorRotaSyncHandler {
        return ColaboradorRotaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideMetaColaboradorSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): MetaColaboradorSyncHandler {
        return MetaColaboradorSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideMetaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): MetaSyncHandler {
        return MetaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideAssinaturaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): AssinaturaSyncHandler {
        return AssinaturaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideVeiculoSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): VeiculoSyncHandler {
        return VeiculoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideEquipamentoSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): EquipamentoSyncHandler {
        return EquipamentoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideEstoqueSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncMetadataDao: SyncMetadataDao
    ): EstoqueSyncHandler {
        return EstoqueSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }
    
    // Providers para classes de refatoração
    @Provides
    @Singleton
    fun provideSyncUtils(): SyncUtils {
        return SyncUtils()
    }
    
    @Provides
    @Singleton
    fun provideSyncCore(
        syncMetadataDao: SyncMetadataDao,
        userSessionManager: UserSessionManager,
        appRepository: AppRepository
    ): SyncCore {
        return SyncCore(syncMetadataDao, userSessionManager, appRepository)
    }
    
    @Provides
    @Singleton
    fun provideSyncOrchestration(
        mesaSyncHandler: MesaSyncHandler,
        clienteSyncHandler: ClienteSyncHandler,
        contratoSyncHandler: ContratoSyncHandler,
        acertoSyncHandler: AcertoSyncHandler,
        despesaSyncHandler: DespesaSyncHandler,
        rotaSyncHandler: RotaSyncHandler,
        cicloSyncHandler: CicloSyncHandler,
        colaboradorSyncHandler: ColaboradorSyncHandler,
        colaboradorRotaSyncHandler: ColaboradorRotaSyncHandler,
        metaColaboradorSyncHandler: MetaColaboradorSyncHandler,
        metaSyncHandler: MetaSyncHandler,
        assinaturaSyncHandler: AssinaturaSyncHandler,
        veiculoSyncHandler: VeiculoSyncHandler,
        equipamentoSyncHandler: EquipamentoSyncHandler,
        estoqueSyncHandler: EstoqueSyncHandler,
        syncCore: SyncCore,
        appRepository: AppRepository,
        firestore: FirebaseFirestore
    ): SyncOrchestration {
        return SyncOrchestration(
            mesaSyncHandler,
            clienteSyncHandler,
            contratoSyncHandler,
            acertoSyncHandler,
            despesaSyncHandler,
            rotaSyncHandler,
            cicloSyncHandler,
            colaboradorSyncHandler,
            colaboradorRotaSyncHandler,
            metaColaboradorSyncHandler,
            metaSyncHandler,
            assinaturaSyncHandler,
            veiculoSyncHandler,
            equipamentoSyncHandler,
            estoqueSyncHandler,
            syncCore,
            appRepository,
            firestore
        )
    }
}
