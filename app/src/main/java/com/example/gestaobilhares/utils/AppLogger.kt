
package com.example.gestaobilhares.utils

import android.icu.text.SimpleDateFormat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Date
import java.util.Locale

object AppLogger {

    private val _logs = MutableLiveData<MutableList<String>>(mutableListOf())
    val logs: LiveData<MutableList<String>> = _logs

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "$timestamp [$tag] $message"
        
        val currentLogs = _logs.value ?: mutableListOf()
        currentLogs.add(0, logMessage) // Adiciona no topo
        _logs.postValue(currentLogs)
    }

    fun clearLogs() {
        _logs.postValue(mutableListOf())
    }
} 