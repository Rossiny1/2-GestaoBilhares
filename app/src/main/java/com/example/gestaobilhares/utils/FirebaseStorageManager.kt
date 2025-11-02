package com.example.gestaobilhares.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * Gerenciador centralizado para upload e download de imagens no Firebase Storage
 * 
 * Estratégia:
 * - Upload: Converte caminho local → Upload para Firebase Storage → Retorna URL pública
 * - Download: Baixa URL do Firebase Storage → Salva localmente → Retorna caminho local
 * - Fallback: Mantém compatibilidade com caminhos locais existentes
 */
object FirebaseStorageManager {
    
    private const val TAG = "FirebaseStorageManager"
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    
    /**
     * Estrutura de pastas no Firebase Storage:
     * - empresas/{empresaId}/despesas/{despesaId}/comprovante.jpg
     * - empresas/{empresaId}/acertos/{acertoId}/mesas/{mesaId}/relogio_final.jpg
     * - empresas/{empresaId}/reformas/{reformaId}/foto_reforma.jpg
     */
    
    /**
     * Upload de foto de comprovante de despesa
     * @param empresaId ID da empresa
     * @param despesaId ID da despesa
     * @param caminhoLocal Caminho local da foto
     * @return URL pública da foto no Firebase Storage, ou null se falhar
     */
    suspend fun uploadFotoComprovante(
        empresaId: String,
        despesaId: Long,
        caminhoLocal: String?
    ): String? {
        if (caminhoLocal.isNullOrBlank()) return null
        
        return try {
            val arquivo = File(caminhoLocal)
            if (!arquivo.exists()) {
                Log.w(TAG, "Arquivo não existe: $caminhoLocal")
                return null
            }
            
            val storagePath = "empresas/$empresaId/despesas/$despesaId/comprovante_${UUID.randomUUID()}.jpg"
            uploadFile(arquivo, storagePath)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer upload de foto de comprovante: ${e.message}", e)
            null
        }
    }
    
    /**
     * Upload de foto de relógio final (AcertoMesa)
     * @param empresaId ID da empresa
     * @param acertoId ID do acerto
     * @param mesaId ID da mesa
     * @param caminhoLocal Caminho local da foto
     * @return URL pública da foto no Firebase Storage, ou null se falhar
     */
    suspend fun uploadFotoRelogioFinal(
        empresaId: String,
        acertoId: Long,
        mesaId: Long,
        caminhoLocal: String?
    ): String? {
        if (caminhoLocal.isNullOrBlank()) return null
        
        return try {
            val arquivo = File(caminhoLocal)
            if (!arquivo.exists()) {
                Log.w(TAG, "Arquivo não existe: $caminhoLocal")
                return null
            }
            
            val storagePath = "empresas/$empresaId/acertos/$acertoId/mesas/$mesaId/relogio_final_${UUID.randomUUID()}.jpg"
            uploadFile(arquivo, storagePath)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer upload de foto de relógio final: ${e.message}", e)
            null
        }
    }
    
    /**
     * Upload de foto de reforma de mesa
     * @param empresaId ID da empresa
     * @param reformaId ID da reforma
     * @param caminhoLocal Caminho local da foto
     * @return URL pública da foto no Firebase Storage, ou null se falhar
     */
    suspend fun uploadFotoReforma(
        empresaId: String,
        reformaId: Long,
        caminhoLocal: String?
    ): String? {
        if (caminhoLocal.isNullOrBlank()) return null
        
        return try {
            val arquivo = File(caminhoLocal)
            if (!arquivo.exists()) {
                Log.w(TAG, "Arquivo não existe: $caminhoLocal")
                return null
            }
            
            val storagePath = "empresas/$empresaId/reformas/$reformaId/foto_reforma_${UUID.randomUUID()}.jpg"
            uploadFile(arquivo, storagePath)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer upload de foto de reforma: ${e.message}", e)
            null
        }
    }
    
    /**
     * Upload genérico de arquivo para Firebase Storage
     * @param arquivo Arquivo local a ser enviado
     * @param storagePath Caminho no Firebase Storage
     * @return URL pública do arquivo, ou null se falhar
     */
    private suspend fun uploadFile(arquivo: File, storagePath: String): String? {
        return try {
            Log.d(TAG, "Iniciando upload: $storagePath (${arquivo.length()} bytes)")
            
            val storageRef: StorageReference = storage.reference.child(storagePath)
            val uploadTask = storageRef.putFile(Uri.fromFile(arquivo))
            
            // Aguardar upload concluir
            val snapshot = uploadTask.await()
            
            // Obter URL de download
            val downloadUrl = snapshot.storage.downloadUrl.await()
            val urlString = downloadUrl.toString()
            
            Log.d(TAG, "✅ Upload concluído: $urlString")
            urlString
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no upload de $storagePath: ${e.message}", e)
            null
        }
    }
    
    /**
     * Download de foto do Firebase Storage para armazenamento local
     * @param context Contexto da aplicação
     * @param urlFirebase URL pública da foto no Firebase Storage
     * @param tipoFoto Tipo da foto (comprovante, relogio_final, foto_reforma)
     * @return Caminho local do arquivo baixado, ou null se falhar
     */
    suspend fun downloadFoto(
        context: Context,
        urlFirebase: String?,
        tipoFoto: String
    ): String? {
        if (urlFirebase.isNullOrBlank()) return null
        
        // ✅ Verificar se já é um caminho local (compatibilidade com dados antigos)
        if (!urlFirebase.startsWith("http://") && !urlFirebase.startsWith("https://")) {
            // Se não é uma URL HTTP, assume que é caminho local
            val arquivo = File(urlFirebase)
            if (arquivo.exists()) {
                Log.d(TAG, "URL é caminho local existente: $urlFirebase")
                return urlFirebase
            }
            return null
        }
        
        return try {
            Log.d(TAG, "Iniciando download: $urlFirebase")
            
            // Criar arquivo temporário local
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val nomeArquivo = "${tipoFoto}_${System.currentTimeMillis()}.jpg"
            val arquivoLocal = File(storageDir, nomeArquivo)
            
            // Baixar do Firebase Storage usando HTTP direto (mais confiável)
            val url = java.net.URL(urlFirebase)
            val connection = url.openConnection()
            connection.connect()
            
            val inputStream = connection.getInputStream()
            arquivoLocal.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            Log.d(TAG, "✅ Download concluído: ${arquivoLocal.absolutePath}")
            arquivoLocal.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no download de $urlFirebase: ${e.message}", e)
            null
        }
    }
    
    /**
     * Verifica se uma string é uma URL do Firebase Storage
     */
    fun isFirebaseStorageUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("https://firebasestorage.googleapis.com/") ||
               url.startsWith("http://firebasestorage.googleapis.com/")
    }
    
    /**
     * Verifica se uma string é um caminho local
     */
    fun isLocalPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return !path.startsWith("http://") && !path.startsWith("https://")
    }
}

