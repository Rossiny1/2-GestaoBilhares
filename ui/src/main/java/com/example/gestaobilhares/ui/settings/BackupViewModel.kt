package com.example.gestaobilhares.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            try {
                val json = repository.exportData()
                _uiState.value = BackupUiState.Success(json)
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(e.message ?: "Erro desconhecido ao exportar dados")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = BackupUiState.Idle
    }

    sealed class BackupUiState {
        object Idle : BackupUiState()
        object Loading : BackupUiState()
        data class Success(val jsonData: String) : BackupUiState()
        data class Error(val message: String) : BackupUiState()
    }
}
