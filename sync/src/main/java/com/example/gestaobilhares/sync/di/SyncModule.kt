package com.example.gestaobilhares.sync.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.sync.utils.SyncUtils
import com.example.gestaobilhares.sync.core.SyncCore
import com.example.gestaobilhares.sync.orchestration.SyncOrchestration
import com.example.gestaobilhares.sync.SyncRepository
import com.example.gestaobilhares.sync.handlers.*

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de injeção de dependências para sincronização.
 * Versão simplificada que fornece apenas o necessário para compilar.
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    // ==================== PROVIDERS BÁSICOS ====================

    
    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    
    
    // ==================== PROVIDERS DE SYNC HANDLERS ====================

    @Provides
    @Singleton
    fun provideMesaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): MesaSyncHandler {
        return MesaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideClienteSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): ClienteSyncHandler {
        return ClienteSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideContratoSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): ContratoSyncHandler {
        return ContratoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideAcertoSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): AcertoSyncHandler {
        return AcertoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideDespesaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): DespesaSyncHandler {
        return DespesaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideRotaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): RotaSyncHandler {
        return RotaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideCicloSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): CicloSyncHandler {
        return CicloSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideColaboradorSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): ColaboradorSyncHandler {
        return ColaboradorSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideColaboradorRotaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): ColaboradorRotaSyncHandler {
        return ColaboradorRotaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideMetaColaboradorSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): MetaColaboradorSyncHandler {
        return MetaColaboradorSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideMetaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): MetaSyncHandler {
        return MetaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideAssinaturaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): AssinaturaSyncHandler {
        return AssinaturaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideVeiculoSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): VeiculoSyncHandler {
        return VeiculoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideEquipamentoSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): EquipamentoSyncHandler {
        return EquipamentoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    @Provides
    @Singleton
    fun provideEstoqueSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader
    ): EstoqueSyncHandler {
        return EstoqueSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }

    // ==================== PROVIDERS DAS CLASSES ESPECIALIZADAS ====================

    @Provides
    @Singleton
    fun provideSyncUtils(
        @ApplicationContext context: Context
    ): SyncUtils {
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

    @Provides
    @Singleton
    fun provideSyncRepository(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: FirebaseImageUploader,
        syncUtils: SyncUtils,
        syncCore: SyncCore,
        syncOrchestration: SyncOrchestration
    ): SyncRepository {
        return SyncRepository(
            context,
            appRepository,
            networkUtils,
            userSessionManager,
            firebaseImageUploader,
            syncUtils,
            syncCore,
            syncOrchestration
        )
    }
}
