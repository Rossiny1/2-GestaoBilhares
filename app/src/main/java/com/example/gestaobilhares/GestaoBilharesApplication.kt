package com.example.gestaobilhares

import android.app.Application
import com.google.firebase.FirebaseApp
import timber.log.Timber
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class principal do app.
 * Inicializa Firebase e configurações básicas.
 */
@HiltAndroidApp
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
        
        // ✅ PRODUÇÃO: Configurar Timber com integração Crashlytics
        if (BuildConfig.DEBUG) {
            // Em DEBUG: Usar DebugTree para logs locais + CrashlyticsTree para testar integração
            Timber.plant(Timber.DebugTree())
            Timber.plant(CrashlyticsTree()) // Também em debug para testar integração
            Timber.i("Timber configurado: DebugTree + CrashlyticsTree (DEBUG)")
            
            // ✅ TESTE: Enviar um log de informação para o Crashlytics para confirmar conexão
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("App iniciado em modo DEBUG - Crashlytics Ativo")
        } else {
            // Em RELEASE: Usar CrashlyticsTree para enviar logs para produção
            Timber.plant(CrashlyticsTree())
            Timber.d("Timber configurado: CrashlyticsTree (RELEASE)")
        }
        Timber.d("Aplicacao iniciada com sucesso")
        
        // Agendar sincronização periódica em background
        try {
            com.example.gestaobilhares.sync.SyncManager.schedulePeriodicSync(this)
            Timber.d("Sincronização periódica agendada")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao agendar sincronização periódica: ${e.message}")
            // Continuar mesmo se falhar (modo offline)
        }
    }
} 
