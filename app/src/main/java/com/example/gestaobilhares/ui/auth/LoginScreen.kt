package com.example.gestaobilhares.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gestaobilhares.ui.components.GestaoBilharesButton
import com.example.gestaobilhares.ui.components.GestaoBilharesLoadingIndicator
import com.example.gestaobilhares.ui.components.GestaoBilharesTextField
import com.example.gestaobilhares.ui.components.ButtonVariant

// ✅ FASE 4: Tela de Login em Compose
// Mantém o design atual para migração conservadora

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRoutes: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: AuthViewModel
) {
    // Estados locais
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Estados do ViewModel
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    
    // Navegação baseada no estado
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                onNavigateToRoutes()
            }
            AuthState.Unauthenticated -> {
                // Manter na tela de login
            }
        }
    }
    
    // Mostrar loading
    if (isLoading) {
        GestaoBilharesLoadingIndicator()
        return
    }
    
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
                viewModel.login(email, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.isNotBlank()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botão Google Sign-In
        GestaoBilharesButton(
            text = "Entrar com Google",
            onClick = {
                // TODO: Implementar login com Google
                // viewModel.signInWithGoogle(account)
            },
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Secondary
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
        
        // Status de conexão
        if (!isOnline) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Modo offline ativo",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
