package com.example.gestaobilhares.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gestaobilhares.data.factory.RepositoryFactory

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
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando sincronização em background...")
            
            val syncRepository = RepositoryFactory.getSyncRepository(applicationContext)
            
            // 1. Processar fila primeiro (operações pendentes)
            syncRepository.processSyncQueue()
            
            // 2. Executar sincronização bidirecional
            val result = syncRepository.syncBidirectional()
            
            // 3. Limpar operações antigas completadas
            syncRepository.limparOperacoesAntigas()
            
            if (result.isSuccess) {
                Log.d(TAG, "Sincronização em background concluída com sucesso")
                Result.success()
            } else {
                Log.w(TAG, "Sincronização em background falhou: ${result.exceptionOrNull()?.message}")
                Result.retry() // Tentar novamente mais tarde
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização em background: ${e.message}", e)
            Result.retry() // Tentar novamente mais tarde
        }
    }
}

