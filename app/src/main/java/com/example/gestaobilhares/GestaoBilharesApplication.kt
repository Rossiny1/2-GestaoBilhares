package com.example.gestaobilhares

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class principal do app.
 * Hilt ativado para injeção de dependências.
 */
@HiltAndroidApp
class GestaoBilharesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Inicialização básica da aplicação
    }
} 