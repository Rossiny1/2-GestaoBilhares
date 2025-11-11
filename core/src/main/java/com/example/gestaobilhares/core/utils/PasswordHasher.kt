package com.example.gestaobilhares.core.utils

import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * ✅ FASE 12.1: Utilitário para hash seguro de senhas
 * 
 * Implementa PBKDF2 com SHA-256 para hash de senhas.
 * PBKDF2 é recomendado pelo NIST e é seguro para armazenamento de senhas.
 * 
 * Características:
 * - Usa salt aleatório para cada senha
 * - 10.000 iterações (balanceamento entre segurança e performance)
 * - Hash de 256 bits (32 bytes)
 * - Armazena salt junto com hash para validação
 */
object PasswordHasher {
    
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256 // bits
    private const val SALT_LENGTH = 16 // bytes
    
    /**
     * Gera hash seguro de uma senha
     * 
     * @param password Senha em texto plano
     * @return String no formato "salt:hash" (ambos em Base64)
     */
    fun hashPassword(password: String): String {
        if (password.isBlank()) {
            throw IllegalArgumentException("Senha não pode ser vazia")
        }
        
        // Gerar salt aleatório
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        
        // Gerar hash usando PBKDF2
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )
        
        val keyFactory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = keyFactory.generateSecret(spec).encoded
        
        // Retornar salt e hash em Base64 separados por ":"
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)
        
        return "$saltBase64:$hashBase64"
    }
    
    /**
     * Verifica se uma senha corresponde ao hash armazenado
     * 
     * @param password Senha em texto plano a ser verificada
     * @param storedHash Hash armazenado no formato "salt:hash"
     * @return true se a senha corresponde ao hash, false caso contrário
     */
    fun verifyPassword(password: String, storedHash: String?): Boolean {
        if (password.isBlank() || storedHash.isNullOrBlank()) {
            return false
        }
        
        try {
            // Separar salt e hash
            val parts = storedHash.split(":")
            if (parts.size != 2) {
                Log.w("PasswordHasher", "Formato de hash inválido")
                return false
            }
            
            val salt = Base64.getDecoder().decode(parts[0])
            val storedHashBytes = Base64.getDecoder().decode(parts[1])
            
            // Gerar hash da senha fornecida usando o mesmo salt
            val spec = PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
            )
            
            val keyFactory = SecretKeyFactory.getInstance(ALGORITHM)
            val computedHash = keyFactory.generateSecret(spec).encoded
            
            // Comparar hashes de forma segura (timing-safe)
            return MessageDigest.isEqual(storedHashBytes, computedHash)
            
        } catch (e: Exception) {
            Log.e("PasswordHasher", "Erro ao verificar senha: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Verifica se um hash está no formato correto
     * 
     * @param hash Hash a ser verificado
     * @return true se o formato está correto, false caso contrário
     */
    fun isValidHashFormat(hash: String?): Boolean {
        if (hash.isNullOrBlank()) {
            return false
        }
        
        val parts = hash.split(":")
        if (parts.size != 2) {
            return false
        }
        
        try {
            Base64.getDecoder().decode(parts[0])
            Base64.getDecoder().decode(parts[1])
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

