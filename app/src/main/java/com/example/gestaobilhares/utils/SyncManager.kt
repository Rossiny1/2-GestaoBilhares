package com.example.gestaobilhares.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gestaobilhares.workers.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * Helper para gerenciar sincronização periódica com WorkManager.
 * Configura e agenda sincronização automática em background.
 */
object SyncManager {
    
    private const val SYNC_INTERVAL_MINUTES = 30L // Sincronizar a cada 30 minutos
    
    /**
     * Agenda sincronização periódica em background.
     * Sincroniza automaticamente quando dispositivo está online.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Manter trabalho existente se já estiver agendado
            syncWorkRequest
        )
    }
    
    /**
     * Cancela sincronização periódica.
     */
    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
    
    /**
     * Executa sincronização única imediata.
     */
    fun syncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }
}

