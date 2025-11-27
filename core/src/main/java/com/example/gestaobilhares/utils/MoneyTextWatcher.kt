package com.example.gestaobilhares.core.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.*

/**
 * TextWatcher para formatação automática de valores monetários em Real (BRL)
 */
class MoneyTextWatcher(private val editText: EditText) : TextWatcher {
    
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var isFormatting = false
    private var currentValue = 0.0
    
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    
    override fun afterTextChanged(s: Editable?) {
        if (isFormatting) return
        
        isFormatting = true
        
        try {
            val cleanString = s.toString().replace(Regex("[^\\d]"), "")
            
            if (cleanString.isEmpty()) {
                currentValue = 0.0
                editText.setText("")
                editText.setSelection(0)
            } else {
                val value = cleanString.toDouble() / 100
                currentValue = value
                
                val formatted = String.format(Locale("pt", "BR"), "%.2f", value)
                editText.setText(formatted)
                editText.setSelection(formatted.length)
            }
        } catch (e: Exception) {
            // Em caso de erro, manter o valor atual
        }
        
        isFormatting = false
    }
    
    /**
     * Obtém o valor numérico atual
     */
    fun getValue(): Double = currentValue
    
    /**
     * Define um valor inicial
     */
    fun setValue(value: Double) {
        if (value == 0.0) {
            editText.setText("")
            currentValue = 0.0
        } else {
            val formatted = String.format(Locale("pt", "BR"), "%.2f", value)
            editText.setText(formatted)
            currentValue = value
        }
    }
    
    companion object {
        /**
         * Converte uma string formatada para double
         */
        fun parseValue(text: String): Double {
            return try {
                val cleanString = text.replace(Regex("[^\\d,]"), "").replace(",", ".")
                cleanString.toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                0.0
            }
        }
        
        /**
         * Formata um valor double para string monetária brasileira
         */
        fun formatValue(value: Double): String {
            return String.format(Locale("pt", "BR"), "%.2f", value)
        }
    }
}
