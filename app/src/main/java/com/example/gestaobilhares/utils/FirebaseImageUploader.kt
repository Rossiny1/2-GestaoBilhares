package com.example.gestaobilhares.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilitário para upload de imagens para o Firebase Storage.
 * ✅ CORREÇÃO: Restaurado após modularização para sincronizar fotos corretamente.
 */
class FirebaseImageUploader(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "FirebaseImageUploader"
        
        // Pastas no Firebase Storage
        private const val FOLDER_DESPESAS = "despesas"
        private const val FOLDER_MESAS_RELOGIOS = "mesas_relogios"
        private const val FOLDER_MESAS_REFORMAS = "mesas_reformas"
        private const val FOLDER_COLABORADORES = "colaboradores"
        
        // Instância singleton do Firebase Storage (usa configuração padrão do projeto)
        private val storage: FirebaseStorage by lazy {
            FirebaseStorage.getInstance()
        }
    }
    
    /**
     * Faz upload de uma imagem para o Firebase Storage
     * @param filePath Caminho local do arquivo de imagem
     * @param folder Pasta de destino no Firebase Storage
     * @param fileName Nome do arquivo (opcional, será gerado se não fornecido)
     * @return URL pública da imagem no Firebase Storage ou null se houver erro
     */
    suspend fun uploadImage(
        filePath: String,
        folder: String,
        fileName: String? = null
    ): String? {
        return try {
            Log.d(TAG, "Iniciando upload de imagem: $filePath para pasta: $folder")
            
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Arquivo não existe: $filePath")
                return null
            }
            
            // Gerar nome do arquivo se não fornecido
            val finalFileName = fileName ?: generateFileName()
            
            // Criar referência no Firebase Storage
            val storageRef: StorageReference = storage.reference
            val imageRef: StorageReference = storageRef.child("$folder/$finalFileName")
            
            // Fazer upload do arquivo
            val uploadTask = imageRef.putFile(Uri.fromFile(file))
            uploadTask.await()
            
            // Obter URL pública
            val downloadUrl = imageRef.downloadUrl.await()
            val url = downloadUrl.toString()
            
            Log.d(TAG, "✅ Upload concluído com sucesso: $url")
            url
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer upload da imagem: ${e.message}", e)
            null
        }
    }
    
    /**
     * Faz upload de uma imagem a partir de um URI
     * @param uri URI da imagem
     * @param folder Pasta de destino no Firebase Storage
     * @param fileName Nome do arquivo (opcional)
     * @return URL pública da imagem no Firebase Storage ou null se houver erro
     */
    suspend fun uploadImageFromUri(
        uri: Uri,
        folder: String,
        fileName: String? = null
    ): String? {
        return try {
            Log.d(TAG, "Iniciando upload de imagem a partir de URI: $uri para pasta: $folder")
            
            // Gerar nome do arquivo se não fornecido
            val finalFileName = fileName ?: generateFileName()
            
            // Criar referência no Firebase Storage
            val storageRef: StorageReference = storage.reference
            val imageRef: StorageReference = storageRef.child("$folder/$finalFileName")
            
            // Fazer upload do arquivo
            val uploadTask = imageRef.putFile(uri)
            uploadTask.await()
            
            // Obter URL pública
            val downloadUrl = imageRef.downloadUrl.await()
            val url = downloadUrl.toString()
            
            Log.d(TAG, "✅ Upload concluído com sucesso: $url")
            url
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer upload da imagem: ${e.message}", e)
            null
        }
    }
    
    /**
     * Faz upload de foto de comprovante de despesa
     */
    suspend fun uploadDespesaComprovante(filePath: String): String? {
        return uploadImage(filePath, FOLDER_DESPESAS)
    }
    
    /**
     * Faz upload de foto de relógio de mesa
     */
    suspend fun uploadMesaRelogio(filePath: String, mesaId: Long): String? {
        val fileName = "mesa_${mesaId}_${generateFileName()}"
        return uploadImage(filePath, FOLDER_MESAS_RELOGIOS, fileName)
    }
    
    /**
     * Faz upload de foto de reforma de mesa
     */
    suspend fun uploadMesaReforma(filePath: String, mesaId: Long): String? {
        val fileName = "reforma_mesa_${mesaId}_${generateFileName()}"
        return uploadImage(filePath, FOLDER_MESAS_REFORMAS, fileName)
    }
    
    /**
     * Faz upload de foto de perfil de colaborador
     */
    suspend fun uploadColaboradorFoto(filePath: String, colaboradorId: Long): String? {
        val fileName = "colaborador_${colaboradorId}_${generateFileName()}"
        return uploadImage(filePath, FOLDER_COLABORADORES, fileName)
    }
    
    /**
     * Verifica se uma URL já é do Firebase Storage
     */
    fun isFirebaseStorageUrl(url: String?): Boolean {
        return url != null && (
            url.contains("firebasestorage.googleapis.com") ||
            url.contains("firebase")
        )
    }
    
    /**
     * Gera um nome de arquivo único baseado em timestamp
     */
    private fun generateFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val random = Random().nextInt(10000)
        return "IMG_${timeStamp}_${random}.jpg"
    }
}

