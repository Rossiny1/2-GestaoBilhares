package com.example.gestaobilhares

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp

/**
 * Application class principal do app.
 * Inicializa Firebase e configurações básicas.
 */
@HiltAndroidApp
class GestaoBilharesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase
        try {
            FirebaseApp.initializeApp(this)
            android.util.Log.d("GestaoBilharesApp", "✅ Firebase inicializado com sucesso")
        } catch (e: Exception) {
            android.util.Log.e("GestaoBilharesApp", "❌ Erro ao inicializar Firebase: ${e.message}", e)
        }
        
        android.util.Log.d("GestaoBilharesApp", "=== APLICAÇÃO INICIADA ===")
    }
} 