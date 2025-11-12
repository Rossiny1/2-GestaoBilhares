package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first

/**
 * ✅ FASE 12.14: Handler base para operações de pull do Firestore
 * 
 * Fornece funcionalidades comuns para todos os handlers de pull
 */
abstract class BasePullHandler(
    protected val appRepository: AppRepository,
    protected val database: AppDatabase,
    protected val firestore: FirebaseFirestore,
    protected val context: Context
) {
    
    /**
     * Obtém o ID da empresa do usuário autenticado
     */
    protected fun getEmpresaId(): String {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        return user?.email?.split("@")?.get(0) ?: "default"
    }
    
    /**
     * Executa o pull da entidade específica
     */
    abstract suspend fun pull(empresaId: String)
    
    /**
     * Log de início de pull
     */
    protected fun logPullStart(entityName: String, empresaId: String, path: String) {
        android.util.Log.d("BasePullHandler", "📥 Baixando $entityName do Firestore...")
        android.util.Log.d("BasePullHandler", "   Caminho: $path")
        android.util.Log.d("BasePullHandler", "   Empresa ID: $empresaId")
    }
    
    /**
     * Log de resumo de pull
     */
    protected fun logPullSummary(entityName: String, sincronizados: Int, existentes: Int) {
        android.util.Log.d("BasePullHandler", "📊 Resumo PULL $entityName:")
        android.util.Log.d("BasePullHandler", "   Sincronizados: $sincronizados")
        android.util.Log.d("BasePullHandler", "   Já existentes: $existentes")
    }
    
    /**
     * Log de erro
     */
    protected fun logError(entityName: String, message: String, throwable: Throwable? = null) {
        android.util.Log.e("BasePullHandler", "❌ Erro ao baixar $entityName: $message", throwable)
    }
}

