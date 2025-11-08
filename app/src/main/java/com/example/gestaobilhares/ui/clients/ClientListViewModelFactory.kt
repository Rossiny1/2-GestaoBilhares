package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.utils.UserSessionManager
import android.content.Context

/**
 * Factory para criar ClientListViewModel com dependências injetadas.
 * Resolve o problema de instanciação do ViewModelProvider.
 */
class ClientListViewModelFactory(
    private val appRepository: AppRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientListViewModel::class.java)) {
            val userSessionManager = UserSessionManager.getInstance(context)
            return ClientListViewModel(appRepository, userSessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
