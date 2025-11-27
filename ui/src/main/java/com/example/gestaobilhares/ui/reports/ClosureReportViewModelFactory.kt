package com.example.gestaobilhares.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.data.repository.AppRepository

/**
 * Factory para criar ClosureReportViewModel com dependÃªncias injetadas.
 * Resolve o problema de instanciaÃ§Ã£o do ViewModelProvider.
 */
class ClosureReportViewModelFactory(
    private val repository: AppRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClosureReportViewModel::class.java)) {
            return ClosureReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

