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
        networkUtils: NetworkUtils
    ): SyncRepository {
        return SyncRepository(context, appRepository, firestore, networkUtils)
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }
}
