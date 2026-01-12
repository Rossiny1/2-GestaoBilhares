package com.example.gestaobilhares.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.SyncRepository
import timber.log.Timber

/**
 * Worker para sincronização periódica em background.
 * Usa WorkManager para executar sincronização mesmo quando app está em background.
 * 
 * Configuração recomendada:
 * - PeriodicWorkRequest com intervalo de 15-30 minutos
 * - Constraints: RequireNetworkType.CONNECTED
 * - BackoffPolicy: EXPONENTIAL
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val appRepository: AppRepository,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_worker"
        private const val MAX_IDLE_HOURS = 6L
    }
    
    override suspend fun doWork(): Result {
        return try {
            Timber.d("Iniciando sincronização em background...")
            
            // Usar repositories injetados
            val appRepo = appRepository
            val syncRepo = syncRepository

            // Verificar se há sincronização pendente
            if (!syncRepo.hasPendingBackgroundSync()) {
                Timber.d("Nenhuma sincronização pendente no momento. Encerrando job em background.")
                return Result.success()
            }
            
            // Verificar conectividade
            if (!syncRepo.isOnline()) {
                Timber.d("Dispositivo offline. Agendando retry.")
                return Result.retry()
            }
            
            // Executar sincronização completa
            val syncResult = syncRepo.syncAllEntities()
            
            if (syncResult.success) {
                Timber.i("Sincronização em background concluída com sucesso: ${syncResult.syncedCount} entidades")
                Result.success()
            } else {
                Timber.w("Sincronização em background falhou: ${syncResult.errors.joinToString(", ")}")
                Result.retry()
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Erro crítico na sincronização em background")
            Result.failure()
        }
    }
}




