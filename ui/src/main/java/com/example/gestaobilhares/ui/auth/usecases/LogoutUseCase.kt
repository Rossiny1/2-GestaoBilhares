package com.example.gestaobilhares.ui.auth.usecases

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class LogoutUseCase @Inject constructor() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun execute() {
        firebaseAuth.signOut()
    }
}
