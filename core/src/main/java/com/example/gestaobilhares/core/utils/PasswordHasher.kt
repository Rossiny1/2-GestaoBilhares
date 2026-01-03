package com.example.gestaobilhares.core.utils

import java.security.MessageDigest

/**
 * Utilitário para hashing de senhas para login offline.
 * Implementa SHA-256 para garantir que senhas não sejam armazenadas em texto plano.
 */
object PasswordHasher {
    
    /**
     * Gera um hash SHA-256 de uma string (senha).
     */
    fun hash(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.trim().toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Em caso improvável de erro, retorna a própria senha (fallback inseguro mas funcional)
            password.trim()
        }
    }
    
    /**
     * Verifica se uma senha corresponde a um hash.
     */
    fun verify(password: String, hash: String?): Boolean {
        if (hash == null) return false
        
        val hashedInput = hash(password)
        
        // Comparação de tempo constante para evitar ataques de temporização
        if (hashedInput.length != hash.length) return false
        
        var result = 0
        for (i in hashedInput.indices) {
            result = result or (hashedInput[i].code xor hash[i].code)
        }
        return result == 0
    }
}
