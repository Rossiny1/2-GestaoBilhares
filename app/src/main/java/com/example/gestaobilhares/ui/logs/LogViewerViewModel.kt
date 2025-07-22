
package com.example.gestaobilhares.ui.logs

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.utils.AppLogger

class LogViewerViewModel : ViewModel() {
    val logs = AppLogger.logs

    fun clearLogs() {
        AppLogger.clearLogs()
    }
} 