package com.example.gestaobilhares

import android.app.Application
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.google.firebase.FirebaseApp
import timber.log.Timber

/**
 * Application class principal do app.
 * Inicializa Firebase e configurações básicas.
 */
class GestaoBilharesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
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
        
        // ✅ FASE 4C: Inicializar workers em background (centralizado)
        try {
            val appRepository = RepositoryFactory.getAppRepository(this)
            appRepository.inicializarWorkersPeriodicos()
            Timber.d("Workers de background inicializados com sucesso")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao inicializar workers: ${e.message}")
            // Continuar mesmo se workers falharem
        }
        
        // ✅ FASE 4D: Inicializar monitoramento de memória
        try {
            val appRepository = RepositoryFactory.getAppRepository(this)
            appRepository.iniciarMonitoramentoMemoria()
            Timber.d("Monitoramento de memória inicializado com sucesso")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao inicializar monitoramento de memória: ${e.message}")
            // Continuar mesmo se monitoramento falhar
        }
        
        Timber.d("Aplicacao iniciada com sucesso")
    }
} 