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
        
        // Configurações de compressão
        private const val MAX_WIDTH = 1024
        private const val MAX_HEIGHT = 1024
        private const val COMPRESSION_QUALITY = 85 // 85% de qualidade (balanceado)
        private const val MAX_FILE_SIZE_KB = 500 // Máximo 500KB por foto
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
            val timeStamp = System.currentTimeMillis()
            val fileName = "compressed_${timeStamp}.jpg"
            val tempFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
            
            // Comprimir e salvar
            val outputStream = FileOutputStream(tempFile)
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            outputStream.close()
            
            if (success) {
                val fileSizeKB = tempFile.length() / 1024
                Log.d(TAG, "Imagem comprimida salva: ${tempFile.absolutePath}")
                Log.d(TAG, "Tamanho final: ${fileSizeKB}KB")
                
                // Verificar se o tamanho está dentro do limite
                if (fileSizeKB <= MAX_FILE_SIZE_KB) {
                    tempFile
                } else {
                    Log.w(TAG, "Arquivo ainda muito grande (${fileSizeKB}KB), tentando compressão adicional")
                    // Tentar compressão adicional com qualidade menor
                    compressWithLowerQuality(bitmap, tempFile)
                }
            } else {
                Log.e(TAG, "Falha ao comprimir bitmap")
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
            var quality = COMPRESSION_QUALITY - 10
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
