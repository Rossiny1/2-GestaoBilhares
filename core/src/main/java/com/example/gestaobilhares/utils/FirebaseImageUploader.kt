package com.example.gestaobilhares.core.utils

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
     * Faz download de uma imagem do Firebase Storage e salva localmente
     * @param firebaseUrl URL da imagem no Firebase Storage
     * @param destinationFolder Pasta de destino local (ex: cacheDir, filesDir)
     * @param fileName Nome do arquivo local (opcional, será gerado se não fornecido)
     * @return Caminho local do arquivo baixado ou null se houver erro
     */
    suspend fun downloadImage(
        firebaseUrl: String,
        destinationFolder: File,
        fileName: String? = null
    ): String? {
        return try {
            Log.d(TAG, "Iniciando download de imagem: $firebaseUrl")
            
            if (!isFirebaseStorageUrl(firebaseUrl)) {
                Log.e(TAG, "URL não é do Firebase Storage: $firebaseUrl")
                return null
            }
            
            // Gerar nome do arquivo se não fornecido
            val finalFileName = fileName ?: generateFileName()
            
            // Criar arquivo de destino
            val destinationFile = File(destinationFolder, finalFileName)
            
            // ✅ CORREÇÃO: Fazer download direto pela URL HTTP
            // O Firebase Storage fornece URLs públicas que podem ser baixadas diretamente
            val url = java.net.URL(firebaseUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 30000 // 30 segundos
            connection.readTimeout = 30000
            connection.doInput = true
            connection.connect()
            
            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Erro HTTP ao fazer download: ${connection.responseCode}")
                return null
            }
            
            // Fazer download e salvar no arquivo
            connection.inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "✅ Download concluído com sucesso: ${destinationFile.absolutePath}")
            destinationFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer download da imagem: ${e.message}", e)
            null
        }
    }
    
    /**
     * Faz download de foto de relógio de mesa do Firebase Storage
     * @param firebaseUrl URL da foto no Firebase Storage
     * @param mesaId ID da mesa
     * @param acertoId ID do acerto (opcional, usado para nome fixo do arquivo)
     * @return Caminho local do arquivo baixado ou null se houver erro
     */
    suspend fun downloadMesaRelogio(
        firebaseUrl: String,
        mesaId: Long,
        acertoId: Long? = null
    ): String? {
        // ✅ CORREÇÃO: Usar nome fixo baseado em acertoId e mesaId para evitar duplicatas
        val fileName = if (acertoId != null) {
            "relogio_acerto_${acertoId}_mesa_${mesaId}.jpg"
        } else {
            "relogio_mesa_${mesaId}_${generateFileName()}"
        }
        
        val destinationFolder = File(context.filesDir, "mesas_relogios")
        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs()
        }
        
        // ✅ NOVO: Verificar se arquivo já existe antes de baixar
        val destinationFile = File(destinationFolder, fileName)
        if (destinationFile.exists()) {
            Log.d(TAG, "✅ Arquivo já existe, reutilizando: ${destinationFile.absolutePath}")
            return destinationFile.absolutePath
        }
        
        return downloadImage(firebaseUrl, destinationFolder, fileName)
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

