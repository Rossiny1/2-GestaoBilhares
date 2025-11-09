package com.example.gestaobilhares.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utilitário para compressão de imagens
 * Otimiza fotos para reduzir espaço no banco de dados sem comprometer a qualidade
 */
class ImageCompressionUtils(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ImageCompression"
        
        // ✅ CONFIGURAÇÕES OTIMIZADAS PARA 100KB
        private const val MAX_WIDTH = 800  // Reduzido para economizar espaço
        private const val MAX_HEIGHT = 800 // Reduzido para economizar espaço
        private const val INITIAL_COMPRESSION_QUALITY = 90 // Qualidade inicial alta
        private const val MAX_FILE_SIZE_KB = 100 // ✅ NOVO: Máximo 100KB por foto
        private const val MIN_QUALITY = 20 // Qualidade mínima para evitar perda excessiva
    }
    
    /**
     * Comprime uma imagem a partir de um URI
     * @param uri URI da imagem original
     * @return Caminho do arquivo comprimido ou null se houver erro
     */
    fun compressImageFromUri(uri: Uri): String? {
        return try {
            Log.d(TAG, "Iniciando compressão de imagem: $uri")
            
            // Ler a imagem original
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Não foi possível abrir InputStream para URI: $uri")
                return null
            }
            
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) {
                Log.e(TAG, "Não foi possível decodificar a imagem")
                return null
            }
            
            Log.d(TAG, "Imagem original: ${originalBitmap.width}x${originalBitmap.height}")
            
            // Comprimir a imagem
            val compressedBitmap = compressBitmap(originalBitmap)
            originalBitmap.recycle()
            
            // Salvar a imagem comprimida
            val compressedFile = saveCompressedBitmap(compressedBitmap)
            compressedBitmap.recycle()
            
            compressedFile?.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao comprimir imagem: ${e.message}", e)
            null
        }
    }
    
    /**
     * Comprime uma imagem a partir de um caminho de arquivo
     * @param filePath Caminho do arquivo original
     * @return Caminho do arquivo comprimido ou null se houver erro
     */
    fun compressImageFromPath(filePath: String): String? {
        return try {
            Log.d(TAG, "Iniciando compressão de imagem: $filePath")
            
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Arquivo não existe: $filePath")
                return null
            }
            
            // Ler a imagem original
            val originalBitmap = BitmapFactory.decodeFile(filePath)
            if (originalBitmap == null) {
                Log.e(TAG, "Não foi possível decodificar a imagem")
                return null
            }
            
            Log.d(TAG, "Imagem original: ${originalBitmap.width}x${originalBitmap.height}")
            Log.d(TAG, "Tamanho original: ${file.length() / 1024}KB")
            
            // Comprimir a imagem
            val compressedBitmap = compressBitmap(originalBitmap)
            originalBitmap.recycle()
            
            // Salvar a imagem comprimida
            val compressedFile = saveCompressedBitmap(compressedBitmap)
            compressedBitmap.recycle()
            
            compressedFile?.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao comprimir imagem: ${e.message}", e)
            null
        }
    }
    
    /**
     * Comprime um Bitmap
     * @param originalBitmap Bitmap original
     * @return Bitmap comprimido
     */
    private fun compressBitmap(originalBitmap: Bitmap): Bitmap {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        
        // Calcular dimensões mantendo proporção
        val (newWidth, newHeight) = calculateDimensions(originalWidth, originalHeight)
        
        Log.d(TAG, "Redimensionando para: ${newWidth}x${newHeight}")
        
        // Redimensionar se necessário
        val resizedBitmap = if (newWidth != originalWidth || newHeight != originalHeight) {
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }
        
        // Aplicar rotação se necessário
        val rotatedBitmap = applyRotation(resizedBitmap)
        
        return rotatedBitmap
    }
    
    /**
     * Calcula as dimensões finais mantendo proporção
     */
    private fun calculateDimensions(originalWidth: Int, originalHeight: Int): Pair<Int, Int> {
        var newWidth = originalWidth
        var newHeight = originalHeight
        
        // Redimensionar apenas se exceder os limites
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            val ratio = minOf(
                MAX_WIDTH.toFloat() / originalWidth,
                MAX_HEIGHT.toFloat() / originalHeight
            )
            
            newWidth = (originalWidth * ratio).toInt()
            newHeight = (originalHeight * ratio).toInt()
        }
        
        return Pair(newWidth, newHeight)
    }
    
    /**
     * Aplica rotação baseada na orientação EXIF
     */
    private fun applyRotation(bitmap: Bitmap): Bitmap {
        return try {
            // Por simplicidade, retornamos o bitmap sem rotação
            // Em uma implementação mais avançada, poderíamos ler os dados EXIF
            bitmap
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao aplicar rotação: ${e.message}")
            bitmap
        }
    }
    
    /**
     * Salva o bitmap comprimido em um arquivo
     */
    private fun saveCompressedBitmap(bitmap: Bitmap): File? {
        return try {
            // Criar arquivo temporário
            // ✅ CORREÇÃO: Variáveis timeStamp e fileName não usadas - removidas
            val tempFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
            
            // ✅ ESTRATÉGIA INTELIGENTE: Compressão adaptativa para 100KB
            val result = compressToTargetSize(bitmap, tempFile)
            
            if (result != null) {
                val fileSizeKB = result.length() / 1024
                Log.d(TAG, "Imagem comprimida salva: ${result.absolutePath}")
                Log.d(TAG, "Tamanho final: ${fileSizeKB}KB")
                result
            } else {
                Log.e(TAG, "Falha ao comprimir bitmap para 100KB")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar bitmap comprimido: ${e.message}", e)
            null
        }
    }
    
    /**
     * Tenta compressão adicional com qualidade menor
     */
    private fun compressWithLowerQuality(bitmap: Bitmap, file: File): File? {
        return try {
            var quality = INITIAL_COMPRESSION_QUALITY - 10
            var fileSizeKB = 0L
            
            while (quality > 30 && fileSizeKB > MAX_FILE_SIZE_KB) {
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.close()
                
                fileSizeKB = file.length() / 1024
                Log.d(TAG, "Tentativa com qualidade $quality%: ${fileSizeKB}KB")
                
                if (fileSizeKB <= MAX_FILE_SIZE_KB) {
                    Log.d(TAG, "Compressão bem-sucedida com qualidade $quality%")
                    return file
                }
                
                quality -= 10
            }
            
            Log.w(TAG, "Não foi possível comprimir abaixo de ${MAX_FILE_SIZE_KB}KB")
            file
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na compressão adicional: ${e.message}", e)
            null
        }
    }
    
    /**
     * ✅ NOVO: Compressão inteligente adaptativa para atingir 100KB
     * Usa estratégia de busca binária para encontrar a melhor qualidade
     */
    private fun compressToTargetSize(bitmap: Bitmap, file: File): File? {
        return try {
            var bestQuality = INITIAL_COMPRESSION_QUALITY
            var bestFile: File? = null
            var minQuality = MIN_QUALITY
            var maxQuality = INITIAL_COMPRESSION_QUALITY
            
            Log.d(TAG, "Iniciando compressão adaptativa para 100KB...")
            
            // Estratégia 1: Busca binária para encontrar qualidade ideal
            while (minQuality <= maxQuality) {
                val currentQuality = (minQuality + maxQuality) / 2
                
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                outputStream.close()
                
                val fileSizeKB = file.length() / 1024
                Log.d(TAG, "Qualidade $currentQuality%: ${fileSizeKB}KB")
                
                if (fileSizeKB <= MAX_FILE_SIZE_KB) {
                    // Tamanho aceitável, tentar qualidade maior
                    bestQuality = currentQuality
                    bestFile = File(file.absolutePath)
                    minQuality = currentQuality + 1
                } else {
                    // Muito grande, reduzir qualidade
                    maxQuality = currentQuality - 1
                }
            }
            
            // Estratégia 2: Se ainda não conseguiu, tentar redimensionar
            if (bestFile == null || bestFile.length() / 1024 > MAX_FILE_SIZE_KB) {
                Log.d(TAG, "Tentando redimensionamento adicional...")
                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap, 
                    (bitmap.width * 0.8).toInt(), 
                    (bitmap.height * 0.8).toInt(), 
                    true
                )
                
                val outputStream = FileOutputStream(file)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, bestQuality, outputStream)
                outputStream.close()
                
                val finalSizeKB = file.length() / 1024
                Log.d(TAG, "Após redimensionamento: ${finalSizeKB}KB")
                
                if (finalSizeKB <= MAX_FILE_SIZE_KB) {
                    bestFile = file
                }
                
                resizedBitmap.recycle()
            }
            
            if (bestFile != null) {
                val finalSizeKB = bestFile.length() / 1024
                Log.d(TAG, "✅ Compressão bem-sucedida: ${finalSizeKB}KB com qualidade $bestQuality%")
            } else {
                Log.w(TAG, "⚠️ Não foi possível comprimir abaixo de 100KB, usando melhor resultado")
            }
            
            bestFile ?: file
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na compressão adaptativa: ${e.message}")
            null
        }
    }
    
    /**
     * Verifica se uma imagem precisa ser comprimida
     */
    fun needsCompression(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            
            val fileSizeKB = file.length() / 1024
            val bitmap = BitmapFactory.decodeFile(filePath)
            
            if (bitmap == null) return false
            
            val needsResize = bitmap.width > MAX_WIDTH || bitmap.height > MAX_HEIGHT
            val needsCompression = fileSizeKB > MAX_FILE_SIZE_KB
            
            bitmap.recycle()
            
            needsResize || needsCompression
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar necessidade de compressão: ${e.message}")
            true // Em caso de erro, assumir que precisa comprimir
        }
    }
    
    /**
     * Obtém informações de uma imagem
     */
    fun getImageInfo(filePath: String): ImageInfo? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            
            val bitmap = BitmapFactory.decodeFile(filePath)
            if (bitmap == null) return null
            
            val info = ImageInfo(
                width = bitmap.width,
                height = bitmap.height,
                fileSizeKB = file.length() / 1024,
                needsCompression = needsCompression(filePath)
            )
            
            bitmap.recycle()
            info
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter informações da imagem: ${e.message}")
            null
        }
    }
    
    /**
     * Data class para informações da imagem
     */
    data class ImageInfo(
        val width: Int,
        val height: Int,
        val fileSizeKB: Long,
        val needsCompression: Boolean
    )
}
