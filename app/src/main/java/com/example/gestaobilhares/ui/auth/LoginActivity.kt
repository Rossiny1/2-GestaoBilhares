package com.example.gestaobilhares.ui.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gestaobilhares.ui.theme.GestaoBilharesTheme

// ✅ FASE 4: Activity Compose para migração conservadora
// Permite usar Compose junto com Fragments existentes

class LoginActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GestaoBilharesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreenSimple(
                        onNavigateToRoutes = {
                            // Navegar para RoutesFragment
                            finish()
                        },
                        onNavigateToForgotPassword = {
                            // Navegar para ForgotPasswordFragment
                            // TODO: Implementar navegação
                        }
                    )
                }
            }
        }
    }
}
