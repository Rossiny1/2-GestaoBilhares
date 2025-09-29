package com.example.gestaobilhares

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class principal do app.
 * Inicializa Firebase e configurações básicas.
 */
@HiltAndroidApp
class GestaoBilharesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("GestaoBilharesApp", "Aplicacao iniciada")
    }
} 