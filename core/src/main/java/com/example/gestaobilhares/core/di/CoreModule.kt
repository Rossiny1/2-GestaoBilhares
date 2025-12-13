package com.example.gestaobilhares.core.di

import android.content.Context
import com.example.gestaobilhares.core.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.UserSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    @Provides
    @Singleton
    fun provideUserSessionManager(@ApplicationContext context: Context): UserSessionManager {
        return UserSessionManager.getInstance(context)
    }
}
