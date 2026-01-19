package com.example.gestaobilhares.ui.auth.usecases

import com.example.gestaobilhares.core.utils.NetworkUtils
import com.example.gestaobilhares.ui.auth.AuthValidator
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authValidator: AuthValidator,
    private val networkUtils: NetworkUtils
) {
    fun validateInput(email: String, senha: String): String? {
        return authValidator.validateLoginInput(email, senha)
    }

    fun isNetworkAvailable(): Boolean {
        return networkUtils.isConnected()
    }
}
