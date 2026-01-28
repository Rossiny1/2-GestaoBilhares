package com.example.gestaobilhares

import android.app.Application
import timber.log.Timber

/**
 * Application class principal do app.
 * Versão simplificada para testes sem Firebase.
 */
class GestaoBilharesApplication : Application() {
    
    companion object {
        @Volatile
        private var INSTANCE: GestaoBilharesApplication? = null
        
        fun getInstance(): GestaoBilharesApplication {
            return INSTANCE ?: throw IllegalStateException("Application não inicializada")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        
        try {
            // Logging básico para testes
            Timber.d("Application inicializada com sucesso")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao inicializar Application: ${e.message}")
            // Continuar mesmo se houver erro
        }
    }
}