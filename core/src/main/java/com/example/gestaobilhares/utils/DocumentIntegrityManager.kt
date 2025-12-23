package com.example.gestaobilhares.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
/**
 * Gerenciador de integridade de documentos para garantir validade jurídica
 * Implementa hash SHA-256 para verificação de integridade conforme Lei 14.063/2020
 */
class DocumentIntegrityManager constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "DocumentIntegrity"
        private const val HASH_ALGORITHM = "SHA-256"
    }
    
    /**
     * Gera hash SHA-256 de um documento PDF
     * Garante integridade do documento conforme requisitos jurídicos
     */
    fun generateDocumentHash(pdfBytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(pdfBytes)
            val hashString = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
            
            Timber.tag(TAG).d( "Hash do documento gerado: ${hashString.take(20)}...")
            hashString
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao gerar hash do documento")
            ""
        }
    }
    
    /**
     * Gera hash SHA-256 de uma assinatura (Bitmap)
     * Garante integridade da assinatura conforme requisitos jurídicos
     */
    fun generateSignatureHash(signatureBitmap: Bitmap): String {
        return try {
            val byteArray = bitmapToByteArray(signatureBitmap)
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(byteArray)
            val hashString = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
            
            Timber.tag(TAG).d( "Hash da assinatura gerado: ${hashString.take(20)}...")
            hashString
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao gerar hash da assinatura")
            ""
        }
    }
    
    /**
     * Gera hash SHA-256 de dados de assinatura (pontos de toque)
     * Para captura de metadados da assinatura
     */
    fun generateSignatureDataHash(signatureData: List<SignaturePoint>): String {
        return try {
            val dataString = signatureData.joinToString("|") { point ->
                "${point.x},${point.y},${point.pressure},${point.timestamp},${point.velocity}"
            }
            
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(dataString.toByteArray())
            val hashString = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
            
            Timber.tag(TAG).d( "Hash dos dados de assinatura gerado: ${hashString.take(20)}...")
            hashString
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao gerar hash dos dados de assinatura")
            ""
        }
    }
    
    /**
     * Verifica integridade de um documento comparando hashes
     * Retorna true se o documento não foi alterado
     */
    fun verifyDocumentIntegrity(
        originalHash: String,
        currentHash: String
    ): Boolean {
        val isValid = originalHash == currentHash
        
        Timber.tag(TAG).d("Verificação de integridade do documento: $isValid")
        Timber.tag(TAG).d("Hash original: ${originalHash.take(20)}...")
        Timber.tag(TAG).d("Hash atual: ${currentHash.take(20)}...")
        
        return isValid
    }
    
    /**
     * Verifica integridade de uma assinatura comparando hashes
     * Retorna true se a assinatura não foi alterada
     */
    fun verifySignatureIntegrity(
        originalHash: String,
        currentHash: String
    ): Boolean {
        val isValid = originalHash == currentHash
        
        Timber.tag(TAG).d("Verificação de integridade da assinatura: $isValid")
        Timber.tag(TAG).d("Hash original: ${originalHash.take(20)}...")
        Timber.tag(TAG).d("Hash atual: ${currentHash.take(20)}...")
        
        return isValid
    }
    
    /**
     * Gera hash combinado de documento + assinatura
     * Para verificação de integridade completa do contrato
     */
    fun generateCombinedHash(
        documentHash: String,
        signatureHash: String,
        metadataHash: String
    ): String {
        return try {
            val combinedData = "$documentHash|$signatureHash|$metadataHash"
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(combinedData.toByteArray())
            val hashString = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
            
            Timber.tag(TAG).d( "Hash combinado gerado: ${hashString.take(20)}...")
            hashString
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao gerar hash combinado")
            ""
        }
    }
    
    /**
     * Converte Bitmap para ByteArray para processamento de hash
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    
    /**
     * Gera hash de metadados do dispositivo
     * Para identificação única do dispositivo
     */
    fun generateDeviceHash(
        deviceId: String,
        timestamp: Long,
        userAgent: String
    ): String {
        return try {
            val deviceData = "$deviceId|$timestamp|$userAgent"
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(deviceData.toByteArray())
            val hashString = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
            
            Timber.tag(TAG).d( "Hash do dispositivo gerado: ${hashString.take(20)}...")
            hashString
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao gerar hash do dispositivo")
            ""
        }
    }
    
    /**
     * Valida se um hash está no formato correto
     * Verifica se é um hash SHA-256 válido
     */
    fun isValidHash(hash: String): Boolean {
        return try {
            // Hash SHA-256 em Base64 deve ter 44 caracteres
            hash.length == 44 && hash.matches(Regex("[A-Za-z0-9+/=]+"))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao validar hash")
            false
        }
    }
}

/**
 * Ponto de assinatura com metadados para análise de autenticidade
 */
data class SignaturePoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long,
    val velocity: Float
)

