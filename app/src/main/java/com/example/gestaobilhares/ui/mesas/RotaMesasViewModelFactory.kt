package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.data.repository.AppRepository

/**
 * Factory para criar RotaMesasViewModel com dependÃªncias injetadas.
 * Resolve o problema de instanciaÃ§Ã£o do ViewModelProvider.
 */
class RotaMesasViewModelFactory(
    private val repository: AppRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RotaMesasViewModel::class.java)) {
            return RotaMesasViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
