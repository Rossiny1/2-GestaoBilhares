package com.example.gestaobilhares.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * ✅ FASE 12.3: Utilitário para criptografia de dados sensíveis usando Android Keystore
 * 
 * Características:
 * - Usa Android Keystore para proteção de chaves (hardware quando disponível)
 * - Algoritmo: AES-GCM (256 bits) - recomendado pelo Android
 * - Criptografia/descriptografia automática de dados sensíveis
 * - Chaves protegidas pelo sistema operacional
 * 
 * Dados que devem ser criptografados:
 * - CPF/CNPJ
 * - Assinaturas (Base64)
 * - Senhas temporárias (já são hash, mas podem ser criptografadas também)
 */
object DataEncryption {
    
    private const val TAG = "DataEncryption"
    private const val KEYSTORE_ALIAS = "GestaoBilhares_Encryption_Key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12 // 12 bytes para GCM
    private const val GCM_TAG_LENGTH = 128 // 128 bits para autenticação
    
    /**
     * Obtém ou cria a chave de criptografia no Android Keystore
     */
    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            // Tentar obter chave existente
            val existingKey = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existingKey != null) {
                Log.d(TAG, "✅ Chave de criptografia encontrada no Keystore")
                return existingKey.secretKey
            }
            
            // Criar nova chave se não existir
            Log.d(TAG, "🔑 Criando nova chave de criptografia no Keystore...")
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256) // 256 bits
                .setUserAuthenticationRequired(false) // Não requer autenticação biométrica (pode ser habilitado)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            val secretKey = keyGenerator.generateKey()
            Log.d(TAG, "✅ Chave de criptografia criada com sucesso")
            secretKey
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao obter/criar chave de criptografia: ${e.message}", e)
            null
        }
    }
    
    /**
     * Criptografa uma string usando AES-GCM
     * 
     * @param plaintext Texto a ser criptografado
     * @return String Base64 com IV + dados criptografados, ou null em caso de erro
     */
    fun encrypt(plaintext: String?): String? {
        if (plaintext.isNullOrBlank()) {
            return plaintext // Retornar null ou vazio se não houver dados
        }
        
        return try {
            val secretKey = getOrCreateSecretKey() ?: run {
                Log.e(TAG, "❌ Não foi possível obter chave de criptografia")
                return null
            }
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            // Obter IV gerado pelo cipher
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combinar IV + dados criptografados
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            // Retornar como Base64
            val encryptedBase64 = Base64.encodeToString(combined, Base64.NO_WRAP)
            Log.d(TAG, "✅ Dados criptografados com sucesso (${plaintext.length} -> ${encryptedBase64.length} chars)")
            encryptedBase64
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao criptografar dados: ${e.message}", e)
            null
        }
    }
    
    /**
     * Descriptografa uma string usando AES-GCM
     * 
     * @param encryptedBase64 String Base64 com IV + dados criptografados
     * @return Texto descriptografado, ou null em caso de erro
     */
    fun decrypt(encryptedBase64: String?): String? {
        if (encryptedBase64.isNullOrBlank()) {
            return encryptedBase64 // Retornar null ou vazio se não houver dados
        }
        
        return try {
            val secretKey = getOrCreateSecretKey() ?: run {
                Log.e(TAG, "❌ Não foi possível obter chave de criptografia")
                return null
            }
            
            // Decodificar Base64
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            
            // Separar IV e dados criptografados
            if (combined.size < GCM_IV_LENGTH) {
                Log.e(TAG, "❌ Dados criptografados inválidos (tamanho insuficiente)")
                return null
            }
            
            val iv = ByteArray(GCM_IV_LENGTH)
            val encryptedBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)
            
            // Descriptografar
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val plaintext = String(decryptedBytes, Charsets.UTF_8)
            Log.d(TAG, "✅ Dados descriptografados com sucesso")
            plaintext
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao descriptografar dados: ${e.message}", e)
            // ⚠️ IMPORTANTE: Se falhar, pode ser que os dados não estejam criptografados (dados antigos)
            // Retornar o valor original para compatibilidade com dados legados
            Log.w(TAG, "⚠️ Tentando retornar valor original (pode ser dado legado não criptografado)")
            encryptedBase64
        }
    }
    
    /**
     * Verifica se uma string está criptografada
     * (heurística: verifica se é Base64 válido e tem tamanho mínimo esperado)
     */
    fun isEncrypted(value: String?): Boolean {
        if (value.isNullOrBlank()) {
            return false
        }
        
        return try {
            val decoded = Base64.decode(value, Base64.NO_WRAP)
            // Dados criptografados devem ter pelo menos IV (12 bytes) + alguns bytes de dados
            decoded.size >= GCM_IV_LENGTH + 16
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Migra dados legados (não criptografados) para formato criptografado
     * 
     * @param plaintext Dados em texto plano
     * @return Dados criptografados, ou o texto original se já estiver criptografado
     */
    fun migrateToEncrypted(plaintext: String?): String? {
        if (plaintext.isNullOrBlank()) {
            return plaintext
        }
        
        // Se já estiver criptografado, retornar como está
        if (isEncrypted(plaintext)) {
            return plaintext
        }
        
        // Criptografar dados legados
        return encrypt(plaintext) ?: plaintext
    }
}

