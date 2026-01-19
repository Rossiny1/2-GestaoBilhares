package com.example.gestaobilhares.ui.auth

import android.util.Patterns
import javax.inject.Inject

class AuthValidator @Inject constructor() {
    fun validateLoginInput(email: String, senha: String): String? {
        if (email.isBlank() || senha.isBlank()) {
            return "Email e senha são obrigatórios"
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Email inválido"
        }
        if (senha.length < 6) {
            return "Senha deve ter pelo menos 6 caracteres"
        }
        return null
    }

    fun validateResetEmail(email: String): String? {
        if (email.isBlank()) {
            return "Email é obrigatório"
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Email inválido"
        }
        return null
    }
}
