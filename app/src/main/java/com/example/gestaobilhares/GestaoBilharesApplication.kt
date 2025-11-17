package com.example.gestaobilhares

import android.app.Application
import com.google.firebase.FirebaseApp
import timber.log.Timber

/**
 * Application class principal do app.
 * Inicializa Firebase e configurações básicas.
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
            // Inicializar Firebase de forma segura
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Timber.d("Firebase inicializado com sucesso")
            } else {
                Timber.d("Firebase já estava inicializado")
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao inicializar Firebase: ${e.message}")
            // Continuar mesmo se Firebase falhar (modo offline)
        }
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("Aplicacao iniciada com sucesso")
        
        // Agendar sincronização periódica em background
        try {
            com.example.gestaobilhares.utils.SyncManager.schedulePeriodicSync(this)
            Timber.d("Sincronização periódica agendada")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao agendar sincronização periódica: ${e.message}")
            // Continuar mesmo se falhar (modo offline)
        }
    }
} 