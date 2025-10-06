package com.example.gestaobilhares.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
/**
 * Coletor de metadados para assinatura eletrônica simples
 * Implementa requisitos da Lei 14.063/2020 para identificação do signatário
 */
class SignatureMetadataCollector constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SignatureMetadata"
    }
    
    /**
     * Coleta todos os metadados necessários para validade jurídica
     */
    fun collectSignatureMetadata(
        documentHash: String,
        signatureHash: String
    ): SignatureMetadata {
        val timestamp = System.currentTimeMillis()
        val deviceId = getDeviceId()
        val ipAddress = getIpAddress()
        val geolocation = getGeolocation()
        val userAgent = getUserAgent()
        val screenResolution = getScreenResolution()
        
        val metadata = SignatureMetadata(
            timestamp = timestamp,
            deviceId = deviceId,
            ipAddress = ipAddress,
            geolocation = geolocation,
            documentHash = documentHash,
            signatureHash = signatureHash,
            userAgent = userAgent,
            screenResolution = screenResolution
        )
        
        Log.d(TAG, "Metadados coletados para assinatura:")
        Log.d(TAG, "Device ID: ${metadata.deviceId}")
        Log.d(TAG, "IP: ${metadata.ipAddress}")
        Log.d(TAG, "Timestamp: ${metadata.timestamp}")
        Log.d(TAG, "Document Hash: ${metadata.documentHash.take(20)}...")
        Log.d(TAG, "Signature Hash: ${metadata.signatureHash.take(20)}...")
        
        return metadata
    }
    
    /**
     * Obtém ID único do dispositivo
     */
    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter Device ID", e)
            "error_device"
        }
    }
    
    /**
     * Obtém endereço IP do dispositivo
     */
    private fun getIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        return address.hostAddress ?: "unknown_ip"
                    }
                }
            }
            "unknown_ip"
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter IP", e)
            "error_ip"
        }
    }
    
    /**
     * Obtém geolocalização (simplificada - sem GPS)
     */
    private fun getGeolocation(): String? {
        return try {
            // Para implementação completa, seria necessário usar LocationManager
            // Por enquanto, retornamos null para não solicitar permissões
            null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter geolocalização", e)
            null
        }
    }
    
    /**
     * Gera User Agent do dispositivo
     */
    private fun getUserAgent(): String {
        return try {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            val version = Build.VERSION.RELEASE
            val sdk = Build.VERSION.SDK_INT
            
            "Android App GestaoBilhares/$manufacturer $model Android $version (SDK $sdk)"
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar User Agent", e)
            "Android App GestaoBilhares/Unknown"
        }
    }
    
    /**
     * Obtém resolução da tela
     */
    private fun getScreenResolution(): String {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter resolução da tela", e)
            "unknown_resolution"
        }
    }
    
    /**
     * Valida se os metadados estão completos
     */
    fun validateMetadata(metadata: SignatureMetadata): Boolean {
        val isValid = metadata.deviceId.isNotBlank() &&
                metadata.ipAddress.isNotBlank() &&
                metadata.documentHash.isNotBlank() &&
                metadata.signatureHash.isNotBlank() &&
                metadata.userAgent.isNotBlank() &&
                metadata.screenResolution.isNotBlank() &&
                metadata.timestamp > 0
        
        Log.d(TAG, "Validação dos metadados: $isValid")
        
        if (!isValid) {
            Log.w(TAG, "Metadados incompletos:")
            Log.w(TAG, "Device ID: ${metadata.deviceId.isNotBlank()}")
            Log.w(TAG, "IP: ${metadata.ipAddress.isNotBlank()}")
            Log.w(TAG, "Document Hash: ${metadata.documentHash.isNotBlank()}")
            Log.w(TAG, "Signature Hash: ${metadata.signatureHash.isNotBlank()}")
            Log.w(TAG, "User Agent: ${metadata.userAgent.isNotBlank()}")
            Log.w(TAG, "Screen Resolution: ${metadata.screenResolution.isNotBlank()}")
            Log.w(TAG, "Timestamp: ${metadata.timestamp > 0}")
        }
        
        return isValid
    }
    
    /**
     * Gera resumo dos metadados para logs
     */
    fun generateMetadataSummary(metadata: SignatureMetadata): String {
        return buildString {
            appendLine("=== RESUMO METADADOS ASSINATURA ===")
            appendLine("Timestamp: ${Date(metadata.timestamp)}")
            appendLine("Device ID: ${metadata.deviceId}")
            appendLine("IP: ${metadata.ipAddress}")
            appendLine("Geolocalização: ${metadata.geolocation ?: "N/A"}")
            appendLine("User Agent: ${metadata.userAgent}")
            appendLine("Resolução: ${metadata.screenResolution}")
            appendLine("Hash Documento: ${metadata.documentHash.take(20)}...")
            appendLine("Hash Assinatura: ${metadata.signatureHash.take(20)}...")
            appendLine("=== FIM RESUMO ===")
        }
    }
}

