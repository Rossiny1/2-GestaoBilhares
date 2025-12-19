package com.example.gestaobilhares.core.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * TextWatcher para formatação automática de CPF/CNPJ
 * Detecta automaticamente se é CPF (11 dígitos) ou CNPJ (14 dígitos)
 */
class CpfCnpjTextWatcher(private val editText: EditText) : TextWatcher {
    
    private var isFormatting = false
    
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    
    override fun afterTextChanged(s: Editable?) {
        if (isFormatting) return
        
        isFormatting = true
        
        try {
            val cleanString = s.toString().replace(Regex("[^0-9]"), "")
            
            if (cleanString.isEmpty()) {
                editText.setText("")
                editText.setSelection(0)
            } else {
                val formatted = when {
                    cleanString.length <= 11 -> formatarCPF(cleanString)
                    else -> formatarCNPJ(cleanString)
                }
                
                editText.setText(formatted)
                editText.setSelection(formatted.length)
            }
        } catch (e: Exception) {
            // Em caso de erro, manter o valor atual
        }
        
        isFormatting = false
    }
    
    /**
     * Formata CPF (000.000.000-00)
     */
    private fun formatarCPF(cpf: String): String {
        return when (cpf.length) {
            0 -> ""
            1, 2, 3 -> cpf
            4, 5, 6 -> "${cpf.substring(0, 3)}.${cpf.substring(3)}"
            7, 8, 9 -> "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6)}"
            10 -> "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9)}"
            11 -> "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9, 11)}"
            else -> cpf.substring(0, 11).let { 
                "${it.substring(0, 3)}.${it.substring(3, 6)}.${it.substring(6, 9)}-${it.substring(9)}"
            }
        }
    }
    
    /**
     * Formata CNPJ (00.000.000/0000-00)
     */
    private fun formatarCNPJ(cnpj: String): String {
        return when (cnpj.length) {
            0 -> ""
            1, 2 -> cnpj
            3, 4, 5 -> "${cnpj.substring(0, 2)}.${cnpj.substring(2)}"
            6, 7, 8 -> "${cnpj.substring(0, 2)}.${cnpj.substring(2, 5)}.${cnpj.substring(5)}"
            9, 10, 11, 12 -> "${cnpj.substring(0, 2)}.${cnpj.substring(2, 5)}.${cnpj.substring(5, 8)}/${cnpj.substring(8)}"
            13 -> "${cnpj.substring(0, 2)}.${cnpj.substring(2, 5)}.${cnpj.substring(5, 8)}/${cnpj.substring(8, 12)}-${cnpj.substring(12)}"
            14 -> "${cnpj.substring(0, 2)}.${cnpj.substring(2, 5)}.${cnpj.substring(5, 8)}/${cnpj.substring(8, 12)}-${cnpj.substring(12, 14)}"
            else -> cnpj.substring(0, 14).let {
                "${it.substring(0, 2)}.${it.substring(2, 5)}.${it.substring(5, 8)}/${it.substring(8, 12)}-${it.substring(12)}"
            }
        }
    }
}

