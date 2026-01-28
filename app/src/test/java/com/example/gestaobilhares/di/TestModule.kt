package com.example.gestaobilhares.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.core.utils.NetworkUtils as CoreNetworkUtils
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.SyncRepository
import com.example.gestaobilhares.sync.utils.NetworkUtils as SyncNetworkUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.example.gestaobilhares.core.di.CoreModule
import com.example.gestaobilhares.sync.di.SyncModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.every
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Módulo de teste para fornecer dependências mockadas
 * nos testes unitários do módulo app.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreModule::class, SyncModule::class]
)
object TestModule {
    
    @Provides
    @Singleton
    fun provideTestContext(): Context {
        return ApplicationProvider.getApplicationContext()
    }
    
    @Provides
    @Singleton
    fun provideMockSyncRepository(): SyncRepository {
        return mockk(relaxed = true)
    }
    
    @Provides
    @Singleton
    fun provideMockCoreNetworkUtils(): CoreNetworkUtils {
        return mockk(relaxed = true)
    }
    
    @Provides
    @Singleton
    fun provideMockSyncNetworkUtils(): SyncNetworkUtils {
        return mockk(relaxed = true)
    }
    
    @Provides
    @Singleton
    fun provideMockUserSessionManager(): UserSessionManager {
        return mockk(relaxed = true)
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseImageUploader(@ApplicationContext context: Context): FirebaseImageUploader {
        return mockk(relaxed = true)
    }
}