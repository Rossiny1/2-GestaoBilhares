package com.example.gestaobilhares.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gestaobilhares.ui.components.GestaoBilharesButton
import com.example.gestaobilhares.ui.components.GestaoBilharesLoadingIndicator
import com.example.gestaobilhares.ui.components.GestaoBilharesTextField
import com.example.gestaobilhares.ui.components.ButtonVariant

// ✅ FASE 4: Tela de Login em Compose (Versão Simplificada)
// Sem Hilt para evitar conflitos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenSimple(
    onNavigateToRoutes: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    // Estados locais
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/Título
        Text(
            text = "Gestão Bilhares",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Faça login para continuar",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Campo de Email
        GestaoBilharesTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage?.contains("email", ignoreCase = true) == true,
            errorMessage = if (errorMessage?.contains("email", ignoreCase = true) == true) errorMessage else null
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Campo de Senha
        GestaoBilharesTextField(
            value = password,
            onValueChange = { password = it },
            label = "Senha",
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage?.contains("senha", ignoreCase = true) == true,
            errorMessage = if (errorMessage?.contains("senha", ignoreCase = true) == true) errorMessage else null,
            supportingText = "Digite sua senha"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botão de Login
        GestaoBilharesButton(
            text = "Entrar",
            onClick = {
                isLoading = true
                // Simular login
                onNavigateToRoutes()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botão Google Sign-In
        GestaoBilharesButton(
            text = "Entrar com Google",
            onClick = {
                isLoading = true
                // Simular login Google
                onNavigateToRoutes()
            },
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Secondary,
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Link Esqueci Senha
        TextButton(
            onClick = onNavigateToForgotPassword,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Esqueci minha senha",
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Mostrar loading
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            GestaoBilharesLoadingIndicator()
        }
    }
}
