package com.example.gestaobilhares.sync.di

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.AcertoSyncHandler
import com.example.gestaobilhares.sync.handlers.CicloSyncHandler
import com.example.gestaobilhares.sync.handlers.ClienteSyncHandler
import com.example.gestaobilhares.sync.handlers.ColaboradorRotaSyncHandler
import com.example.gestaobilhares.sync.handlers.ColaboradorSyncHandler
import com.example.gestaobilhares.sync.handlers.ContratoSyncHandler
import com.example.gestaobilhares.sync.handlers.DespesaSyncHandler
import com.example.gestaobilhares.sync.handlers.MesaSyncHandler
import com.example.gestaobilhares.sync.handlers.AssinaturaSyncHandler
import com.example.gestaobilhares.sync.handlers.EquipamentoSyncHandler
import com.example.gestaobilhares.sync.handlers.EstoqueSyncHandler
import com.example.gestaobilhares.sync.handlers.FinancasSyncHandler
import com.example.gestaobilhares.sync.handlers.MetaColaboradorSyncHandler
import com.example.gestaobilhares.sync.handlers.MetaSyncHandler
import com.example.gestaobilhares.sync.handlers.RotaSyncHandler
import com.example.gestaobilhares.sync.handlers.VeiculoSyncHandler
import com.example.gestaobilhares.sync.handlers.SyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.utils.FirebaseImageUploader
import com.example.gestaobilhares.core.utils.FirebaseImageUploader as CoreFirebaseImageUploader
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para injeção de dependências dos SyncHandlers.
 * 
 * Cada handler é injetável via Hilt seguindo o padrão Strategy.
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncHandlersModule {
    
    @Provides
    @Singleton
    fun provideMesaSyncHandler(
        @ApplicationContext context: Context,
        appRepository: AppRepository,
        firestore: FirebaseFirestore,
        networkUtils: NetworkUtils,
        userSessionManager: UserSessionManager,
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
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
        firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader
    ): EstoqueSyncHandler {
        return EstoqueSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader)
    }
    
    /**
     * Provider genérico para obter handler por tipo de entidade.
     * Útil para orquestração no SyncRepository.
     */
    @Provides
    @Singleton
    fun provideSyncHandlers(
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
        estoqueSyncHandler: EstoqueSyncHandler
    ): Map<String, SyncHandler> {
        return mapOf(
            "mesas" to mesaSyncHandler,
            "clientes" to clienteSyncHandler,
            "contratos" to contratoSyncHandler,
            "acertos" to acertoSyncHandler,
            "despesas" to despesaSyncHandler,
            "rotas" to rotaSyncHandler,
            "ciclos" to cicloSyncHandler,
            "colaboradores" to colaboradorSyncHandler,
            "colaborador_rota" to colaboradorRotaSyncHandler,
            "meta_colaborador" to metaColaboradorSyncHandler,
            "metas" to metaSyncHandler,
            "assinaturas" to assinaturaSyncHandler,
            "veiculos" to veiculoSyncHandler,
            "equipamentos" to equipamentoSyncHandler,
            "estoque" to estoqueSyncHandler
        )
    }
}





