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
    private val appRepositoryTest: AppRepository? = null,
    private val syncRepositoryTest: SyncRepository? = null
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_worker"
        private const val MAX_IDLE_HOURS = 6L
    }
    
    override suspend fun doWork(): Result {
        return try {
            Timber.d("Iniciando sincronização em background...")
            
            // Usar repositories injetados (testes) ou criar novos (produção)
            val database = AppDatabase.getDatabase(applicationContext)
            val appRepo = appRepositoryTest ?: AppRepository.create(database)
            val syncRepo = syncRepositoryTest ?: throw IllegalStateException("SyncRepository must be provided for background sync")


            // Verificar se há necessidade real de sincronizar
            if (!syncRepo.shouldRunBackgroundSync(maxIdleHours = MAX_IDLE_HOURS)) {
                Timber.d("Nenhuma sincronização necessária no momento. Encerrando job em background.")
                return Result.success()
            }
            
            // 1. Processar fila primeiro (operações pendentes)
            val queueResult = syncRepo.processSyncQueue()
            if (queueResult.isFailure) {
                Timber.w("Processamento da fila falhou: ${queueResult.exceptionOrNull()?.message}")
                return Result.retry()
            }
            
            // 2. Executar sincronização bidirecional
            val syncResult = syncRepo.syncBidirectional()
            
            // 3. Limpar operações antigas completadas
            syncRepo.limparOperacoesAntigas()
            
            return if (syncResult.isSuccess) {
                Timber.d("Sincronização em background concluída com sucesso")
                Result.success()
            } else {
                Timber.w("Sincronização em background falhou: ${syncResult.exceptionOrNull()?.message}")
                Result.retry() // Tentar novamente mais tarde
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Erro na sincronização em background")
            Result.retry() // Tentar novamente mais tarde
        }
    }
}




