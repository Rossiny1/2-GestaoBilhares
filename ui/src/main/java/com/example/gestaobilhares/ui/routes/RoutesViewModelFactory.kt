package com.example.gestaobilhares.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager

/**
 * Factory para criar RoutesViewModel com dependências injetadas.
 * Resolve o problema de instanciação do ViewModelProvider.
 */
class RoutesViewModelFactory(
    private val appRepository: AppRepository,
    private val userSessionManager: UserSessionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutesViewModel::class.java)) {
            return RoutesViewModel(appRepository, userSessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

