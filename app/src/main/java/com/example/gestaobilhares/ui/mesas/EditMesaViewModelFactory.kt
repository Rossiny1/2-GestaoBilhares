package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.data.repository.AppRepository

/**
 * Factory para criar EditMesaViewModel com dependÃªncias injetadas.
 * Resolve o problema de instanciaÃ§Ã£o do ViewModelProvider.
 */
class EditMesaViewModelFactory(
    private val repository: AppRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditMesaViewModel::class.java)) {
            return EditMesaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
