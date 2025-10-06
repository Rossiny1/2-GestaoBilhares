package com.example.gestaobilhares

import android.app.Application
import timber.log.Timber

/**
 * Application class principal do app.
 * Inicializa Firebase e configurações básicas.
 */
class GestaoBilharesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("Aplicacao iniciada")
    }
} 