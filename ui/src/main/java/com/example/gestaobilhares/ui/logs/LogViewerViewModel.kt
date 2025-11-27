
package com.example.gestaobilhares.ui.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.core.utils.AppLogger

class LogViewerViewModel : ViewModel() {
    val logs: LiveData<MutableList<String>> = AppLogger.logs

    fun clearLogs() {
        AppLogger.clearLogs()
    }
}