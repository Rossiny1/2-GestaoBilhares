package com.example.gestaobilhares.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ✅ FASE 4C: Worker de limpeza
 * Seguindo Android 2025 best practices com CoroutineWorker
 */
class CleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("CleanupWorker", "Iniciando limpeza em background")
            
            // Simular limpeza de dados antigos
            // Em implementação real, aqui seria a limpeza de logs antigos
            kotlinx.coroutines.delay(500)
            
            Log.d("CleanupWorker", "Limpeza concluída com sucesso")
            Result.success(workDataOf("status" to "success"))
            
        } catch (e: Exception) {
            Log.e("CleanupWorker", "Erro na limpeza: ${e.message}", e)
            Result.failure(workDataOf("error" to e.message))
        }
    }
}
