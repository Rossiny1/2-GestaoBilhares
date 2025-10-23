package com.example.gestaobilhares.network

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * ✅ FASE 4D: Gerenciador de Compressão de Rede
 * Seguindo Android 2025 best practices para otimização de rede
 * 
 * Funcionalidades:
 * - Compressão GZIP para reduzir tamanho de dados
 * - Cache de dados comprimidos
 * - Estatísticas de compressão
 * - Otimização automática baseada no tamanho
 */
class NetworkCompressionManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: NetworkCompressionManager? = null

        fun getInstance(): NetworkCompressionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkCompressionManager().also { INSTANCE = it }
            }
        }

        private const val TAG = "NetworkCompressionManager"
        private const val MIN_COMPRESSION_SIZE = 1024 // 1KB - só comprime se for maior
        private const val MAX_CACHE_SIZE = 100
    }

    // Cache de dados comprimidos
    private val compressionCache = ConcurrentHashMap<String, CompressedData>()
    
    // Estatísticas de compressão
    private val compressionStats = CompressionStats()

    /**
     * Comprime dados usando GZIP
     */
    fun compressData(data: ByteArray, key: String? = null): CompressedData {
        return try {
            // Verificar se deve comprimir baseado no tamanho
            if (data.size < MIN_COMPRESSION_SIZE) {
                Log.d(TAG, "Dados muito pequenos para compressão: ${data.size} bytes")
                return CompressedData(data, false, 0.0)
            }

            // Verificar cache primeiro
            if (key != null) {
                compressionCache[key]?.let { cached ->
                    Log.d(TAG, "Dados comprimidos encontrados no cache: $key")
                    return cached
                }
            }

            // Comprimir dados
            val outputStream = ByteArrayOutputStream()
            val gzipStream = GZIPOutputStream(outputStream)
            gzipStream.write(data)
            gzipStream.close()

            val compressedData = outputStream.toByteArray()
            val compressionRatio = (1.0 - compressedData.size.toDouble() / data.size) * 100

            val result = CompressedData(compressedData, true, compressionRatio)

            // Cachear se especificado
            if (key != null && compressionCache.size < MAX_CACHE_SIZE) {
                compressionCache[key] = result
                Log.d(TAG, "Dados comprimidos cacheados: $key")
            }

            // Atualizar estatísticas
            compressionStats.updateStats(data.size.toLong(), compressedData.size.toLong(), true)

            Log.d(TAG, "Dados comprimidos: ${data.size} -> ${compressedData.size} bytes (${String.format("%.1f", compressionRatio)}% redução)")
            result

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao comprimir dados: ${e.message}", e)
            compressionStats.updateStats(data.size.toLong(), data.size.toLong(), false)
            CompressedData(data, false, 0.0)
        }
    }

    /**
     * Descomprime dados GZIP
     */
    fun decompressData(compressedData: CompressedData): ByteArray {
        return try {
            if (!compressedData.isCompressed) {
                Log.d(TAG, "Dados não estão comprimidos, retornando como estão")
                return compressedData.data
            }

            val inputStream = ByteArrayInputStream(compressedData.data)
            val gzipStream = GZIPInputStream(inputStream)
            val outputStream = ByteArrayOutputStream()

            val buffer = ByteArray(1024)
            var length: Int
            while (gzipStream.read(buffer).also { length = it } != -1) {
                outputStream.write(buffer, 0, length)
            }

            gzipStream.close()
            outputStream.close()

            val decompressedData = outputStream.toByteArray()
            Log.d(TAG, "Dados descomprimidos: ${compressedData.data.size} -> ${decompressedData.size} bytes")
            decompressedData

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao descomprimir dados: ${e.message}", e)
            compressedData.data // Retornar dados originais em caso de erro
        }
    }

    /**
     * Comprime string para bytes
     */
    fun compressString(text: String, key: String? = null): CompressedData {
        return compressData(text.toByteArray(Charsets.UTF_8), key)
    }

    /**
     * Descomprime bytes para string
     */
    fun decompressToString(compressedData: CompressedData): String {
        val decompressedBytes = decompressData(compressedData)
        return String(decompressedBytes, Charsets.UTF_8)
    }

    /**
     * Remove dados do cache
     */
    fun removeFromCache(key: String) {
        compressionCache.remove(key)
        Log.d(TAG, "Dados removidos do cache: $key")
    }

    /**
     * Limpa todo o cache
     */
    fun clearCache() {
        compressionCache.clear()
        Log.d(TAG, "Cache de compressão limpo")
    }

    /**
     * Obtém estatísticas de compressão
     */
    fun getCompressionStats(): CompressionStats {
        return compressionStats.copy()
    }

    /**
     * Obtém informações do cache
     */
    fun getCacheInfo(): CacheInfo {
        return CacheInfo(
            totalEntries = compressionCache.size,
            maxSize = MAX_CACHE_SIZE,
            cacheKeys = compressionCache.keys.toList()
        )
    }

    /**
     * Data class para dados comprimidos
     */
    data class CompressedData(
        val data: ByteArray,
        val isCompressed: Boolean,
        val compressionRatio: Double
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CompressedData

            if (!data.contentEquals(other.data)) return false
            if (isCompressed != other.isCompressed) return false
            if (compressionRatio != other.compressionRatio) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + isCompressed.hashCode()
            result = 31 * result + compressionRatio.hashCode()
            return result
        }
    }

    /**
     * Data class para estatísticas de compressão
     */
    data class CompressionStats(
        var totalOriginalSize: Long = 0,
        var totalCompressedSize: Long = 0,
        var totalCompressions: Int = 0,
        var successfulCompressions: Int = 0,
        var failedCompressions: Int = 0
    ) {
        fun updateStats(originalSize: Long, compressedSize: Long, success: Boolean) {
            totalOriginalSize += originalSize
            totalCompressedSize += compressedSize
            totalCompressions++
            
            if (success) {
                successfulCompressions++
            } else {
                failedCompressions++
            }
        }

        val averageCompressionRatio: Double
            get() = if (totalOriginalSize > 0) {
                (1.0 - totalCompressedSize.toDouble() / totalOriginalSize) * 100
            } else 0.0

        val successRate: Double
            get() = if (totalCompressions > 0) {
                successfulCompressions.toDouble() / totalCompressions * 100
            } else 0.0
    }

    /**
     * Data class para informações do cache
     */
    data class CacheInfo(
        val totalEntries: Int,
        val maxSize: Int,
        val cacheKeys: List<String>
    )
}
