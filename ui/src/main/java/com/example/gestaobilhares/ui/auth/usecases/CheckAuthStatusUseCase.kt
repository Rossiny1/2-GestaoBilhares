package com.example.gestaobilhares.ui.auth.usecases

import com.example.gestaobilhares.ui.auth.AuthState
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class CheckAuthStatusUseCase @Inject constructor() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun execute(): AuthState {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser != null) {
            AuthState.Authenticated(currentUser, true)
        } else {
            AuthState.Unauthenticated
        }
    }
}
