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
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_worker"
        private const val MAX_IDLE_HOURS = 6L
    }
    
    override suspend fun doWork(): Result {
        return try {
            Timber.d("Iniciando sincronização em background...")
            
            // Criar repositories diretamente (sem usar RepositoryFactory para evitar dependência circular)
            val database = AppDatabase.getDatabase(applicationContext)
            val appRepository = AppRepository.create(database)
            val syncRepository = SyncRepository(applicationContext, appRepository)

            // Verificar se há necessidade real de sincronizar
            if (!syncRepository.shouldRunBackgroundSync(maxIdleHours = MAX_IDLE_HOURS)) {
                Timber.d("Nenhuma sincronização necessária no momento. Encerrando job em background.")
                return Result.success()
            }
            
            // 1. Processar fila primeiro (operações pendentes)
            syncRepository.processSyncQueue()
            
            // 2. Executar sincronização bidirecional
            val result = syncRepository.syncBidirectional()
            
            // 3. Limpar operações antigas completadas
            syncRepository.limparOperacoesAntigas()
            
            if (result.isSuccess) {
                Timber.d("Sincronização em background concluída com sucesso")
                Result.success()
            } else {
                Timber.w("Sincronização em background falhou: ${result.exceptionOrNull()?.message}")
                Result.retry() // Tentar novamente mais tarde
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Erro na sincronização em background")
            Result.retry() // Tentar novamente mais tarde
        }
    }
}




