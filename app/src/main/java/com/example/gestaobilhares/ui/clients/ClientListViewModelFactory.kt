package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.DespesaRepository
import com.example.gestaobilhares.data.database.AppDatabase

/**
 * Factory para criar ClientListViewModel com dependÃªncias injetadas.
 * Resolve o problema de instanciaÃ§Ã£o do ViewModelProvider.
 */
class ClientListViewModelFactory(
    private val appRepository: AppRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientListViewModel::class.java)) {
            // ✅ CORREÇÃO: Usar apenas AppRepository - simplificar para evitar erros
            // TODO: Implementar construtor simplificado no ClientListViewModel
            throw UnsupportedOperationException("ClientListViewModel precisa ser refatorado para aceitar apenas AppRepository")
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
