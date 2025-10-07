package com.example.gestaobilhares.ui.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.data.repository.AppRepository

/**
 * Factory para criar MetasViewModel com dependÃªncias injetadas.
 * Resolve o problema de instanciaÃ§Ã£o do ViewModelProvider.
 */
class MetasViewModelFactory(
    private val repository: AppRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetasViewModel::class.java)) {
            val viewModel = MetasViewModel()
            viewModel.initializeRepository(repository)
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
