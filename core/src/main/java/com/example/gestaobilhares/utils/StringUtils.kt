package com.example.gestaobilhares.core.utils

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * ✅ FASE 4: UTILITÁRIOS DE STRING CENTRALIZADOS
 * 
 * Centraliza todas as operações com strings:
 * - Formatação padronizada
 * - Validações comuns
 * - Transformações de texto
 * - Elimina duplicação de código (~60 linhas)
 */
object StringUtils {
    
    // ==================== VALIDAÇÕES ====================
    
    /**
     * Verifica se string está vazia ou nula
     */
    fun isVazia(texto: String?): Boolean {
        return texto.isNullOrBlank()
    }
    
    /**
     * Verifica se string não está vazia
     */
    fun isNaoVazia(texto: String?): Boolean {
        return !isVazia(texto)
    }
    
    /**
     * Verifica se string tem tamanho mínimo
     */
    fun temTamanhoMinimo(texto: String?, tamanhoMinimo: Int): Boolean {
        return texto?.length ?: 0 >= tamanhoMinimo
    }
    
    /**
     * Verifica se string tem tamanho máximo
     */
    fun temTamanhoMaximo(texto: String?, tamanhoMaximo: Int): Boolean {
        return (texto?.length ?: 0) <= tamanhoMaximo
    }
    
    /**
     * Verifica se string tem tamanho dentro do range
     */
    fun temTamanhoValido(texto: String?, tamanhoMinimo: Int, tamanhoMaximo: Int): Boolean {
        return temTamanhoMinimo(texto, tamanhoMinimo) && temTamanhoMaximo(texto, tamanhoMaximo)
    }
    
    // ==================== FORMATAÇÃO ====================
    
    /**
     * Capitaliza primeira letra de cada palavra
     */
    fun capitalizar(texto: String?): String {
        if (isVazia(texto)) return ""
        
        return texto!!.split(" ")
            .joinToString(" ") { palavra ->
                if (palavra.isNotEmpty()) {
                    palavra.lowercase().replaceFirstChar { it.uppercase() }
                } else {
                    palavra
                }
            }
    }
    
    /**
     * Converte para maiúsculas
     */
    fun paraMaiusculas(texto: String?): String {
        return texto?.uppercase() ?: ""
    }
    
    /**
     * Converte para minúsculas
     */
    fun paraMinusculas(texto: String?): String {
        return texto?.lowercase() ?: ""
    }
    
    /**
     * Remove acentos de uma string
     */
    fun removerAcentos(texto: String?): String {
        if (isVazia(texto)) return ""
        
        return Normalizer.normalize(texto!!, Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
    }
    
    /**
     * Remove caracteres especiais, mantendo apenas letras, números e espaços
     */
    fun removerCaracteresEspeciais(texto: String?): String {
        if (isVazia(texto)) return ""
        
        return texto!!.replace(Regex("[^a-zA-Z0-9\\s]"), "")
    }
    
    /**
     * Remove espaços extras
     */
    fun removerEspacosExtras(texto: String?): String {
        if (isVazia(texto)) return ""
        
        return texto!!.trim().replace(Regex("\\s+"), " ")
    }
    
    // ==================== FORMATAÇÃO ESPECÍFICA ====================
    
    /**
     * Formata CPF (000.000.000-00)
     */
    fun formatarCPF(cpf: String?): String {
        if (isVazia(cpf)) return ""
        
        val cpfLimpo = cpf!!.replace(Regex("[^0-9]"), "")
        if (cpfLimpo.length != 11) return cpf
        
        return "${cpfLimpo.substring(0, 3)}.${cpfLimpo.substring(3, 6)}.${cpfLimpo.substring(6, 9)}-${cpfLimpo.substring(9)}"
    }
    
    /**
     * Formata CNPJ (00.000.000/0000-00)
     */
    fun formatarCNPJ(cnpj: String?): String {
        if (isVazia(cnpj)) return ""
        
        val cnpjLimpo = cnpj!!.replace(Regex("[^0-9]"), "")
        if (cnpjLimpo.length != 14) return cnpj
        
        return "${cnpjLimpo.substring(0, 2)}.${cnpjLimpo.substring(2, 5)}.${cnpjLimpo.substring(5, 8)}/${cnpjLimpo.substring(8, 12)}-${cnpjLimpo.substring(12)}"
    }
    
    /**
     * Formata telefone brasileiro
     */
    fun formatarTelefone(telefone: String?): String {
        if (isVazia(telefone)) return ""
        
        val telefoneLimpo = telefone!!.replace(Regex("[^0-9]"), "")
        
        return when (telefoneLimpo.length) {
            10 -> "(${telefoneLimpo.substring(0, 2)}) ${telefoneLimpo.substring(2, 6)}-${telefoneLimpo.substring(6)}"
            11 -> "(${telefoneLimpo.substring(0, 2)}) ${telefoneLimpo.substring(2, 7)}-${telefoneLimpo.substring(7)}"
            else -> telefone
        }
    }
    
    /**
     * Formata CEP (00000-000)
     */
    fun formatarCEP(cep: String?): String {
        if (isVazia(cep)) return ""
        
        val cepLimpo = cep!!.replace(Regex("[^0-9]"), "")
        if (cepLimpo.length != 8) return cep
        
        return "${cepLimpo.substring(0, 5)}-${cepLimpo.substring(5)}"
    }
    
    // ==================== LIMPEZA ====================
    
    /**
     * Remove apenas números de uma string
     */
    fun removerNumeros(texto: String?): String {
        if (isVazia(texto)) return ""
        
        return texto!!.replace(Regex("[0-9]"), "")
    }
    
    /**
     * Remove apenas letras de uma string
     */
    fun removerLetras(texto: String?): String {
        if (isVazia(texto)) return ""
        
        return texto!!.replace(Regex("[a-zA-Z]"), "")
    }
    
    /**
     * Remove tudo exceto números
     */
    fun manterApenasNumeros(texto: String?): String {
        if (isVazia(texto)) return ""
        
        return texto!!.replace(Regex("[^0-9]"), "")
    }
    
    /**
     * Remove tudo exceto letras e números
     */
    fun manterApenasAlfanumericos(texto: String?): String {
        if (isVazia(texto)) return ""
        
        return texto!!.replace(Regex("[^a-zA-Z0-9]"), "")
    }
    
    // ==================== TRUNCAMENTO ====================
    
    /**
     * Trunca string com reticências
     */
    fun truncar(texto: String?, tamanhoMaximo: Int): String {
        if (isVazia(texto)) return ""
        if (texto!!.length <= tamanhoMaximo) return texto
        
        return texto.substring(0, tamanhoMaximo - 3) + "..."
    }
    
    /**
     * Trunca string no meio
     */
    fun truncarNoMeio(texto: String?, tamanhoMaximo: Int): String {
        if (isVazia(texto)) return ""
        if (texto!!.length <= tamanhoMaximo) return texto
        
        val meio = tamanhoMaximo / 2
        val inicio = texto.substring(0, meio - 2)
        val fim = texto.substring(texto.length - meio + 2)
        
        return "$inicio...$fim"
    }
    
    // ==================== CONVERSÃO ====================
    
    /**
     * Converte string para Double seguro
     */
    fun paraDouble(texto: String?, valorPadrao: Double = 0.0): Double {
        return try {
            texto?.replace(",", ".")?.toDouble() ?: valorPadrao
        } catch (e: NumberFormatException) {
            valorPadrao
        }
    }
    
    /**
     * Converte string para Int seguro
     */
    fun paraInt(texto: String?, valorPadrao: Int = 0): Int {
        return try {
            texto?.toInt() ?: valorPadrao
        } catch (e: NumberFormatException) {
            valorPadrao
        }
    }
    
    /**
     * Converte string para Long seguro
     */
    fun paraLong(texto: String?, valorPadrao: Long = 0L): Long {
        return try {
            texto?.toLong() ?: valorPadrao
        } catch (e: NumberFormatException) {
            valorPadrao
        }
    }
    
    // ==================== FORMATAÇÃO DE VALORES ====================
    
    /**
     * Formata valor monetário brasileiro
     */
    fun formatarMoeda(valor: Double): String {
        return String.format("R$ %.2f", valor).replace(".", ",")
    }
    
    /**
     * Formata valor monetário com separadores de milhares
     */
    fun formatarMoedaComSeparadores(valor: Double): String {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
        return formatter.format(valor)
    }
    
    /**
     * Formata valor monetário sem símbolo
     */
    fun formatarValor(valor: Double): String {
        return String.format("%.2f", valor).replace(".", ",")
    }
    
    /**
     * Formata número com separadores de milhares
     */
    fun formatarNumero(numero: Long): String {
        return String.format("%,d", numero).replace(",", ".")
    }
}
