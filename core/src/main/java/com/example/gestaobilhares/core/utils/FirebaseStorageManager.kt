package com.example.gestaobilhares.core.utils

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
     * Upload de foto de manutenção (antes ou depois)
     * @param empresaId ID da empresa
     * @param historicoId ID do histórico de manutenção
     * @param tipo Tipo da foto: "antes" ou "depois"
     * @param caminhoLocal Caminho local da foto
     * @return URL pública da foto no Firebase Storage, ou null se falhar
     */
    suspend fun uploadFotoManutencao(
        empresaId: String,
        historicoId: Long,
        tipo: String,
        caminhoLocal: String?
    ): String? {
        if (caminhoLocal.isNullOrBlank()) return null
        
        return try {
            val arquivo = File(caminhoLocal)
            if (!arquivo.exists()) {
                Log.w(TAG, "Arquivo não existe: $caminhoLocal")
                return null
            }
            
            val storagePath = "empresas/$empresaId/manutencoes/$historicoId/foto_${tipo}_${UUID.randomUUID()}.jpg"
            uploadFile(arquivo, storagePath)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer upload de foto de manutenção ($tipo): ${e.message}", e)
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
            Log.d(TAG, "📤 ========================================")
            Log.d(TAG, "📤 INICIANDO UPLOAD PARA FIREBASE STORAGE")
            Log.d(TAG, "📤 ========================================")
            Log.d(TAG, "📤 Storage path: $storagePath")
            Log.d(TAG, "📤 Arquivo: ${arquivo.absolutePath}")
            Log.d(TAG, "📤 Tamanho: ${arquivo.length()} bytes")
            Log.d(TAG, "📤 Existe: ${arquivo.exists()}")
            
            if (!arquivo.exists()) {
                Log.e(TAG, "❌ ERRO CRÍTICO: Arquivo não existe para upload: ${arquivo.absolutePath}")
                return null
            }
            
            val storageRef: StorageReference = storage.reference.child(storagePath)
            Log.d(TAG, "📤 Storage reference criado: ${storageRef.path}")
            
            val uploadTask = storageRef.putFile(Uri.fromFile(arquivo))
            Log.d(TAG, "📤 Upload task iniciado, aguardando conclusão...")
            
            // Aguardar upload concluir
            val snapshot = uploadTask.await()
            Log.d(TAG, "📤 Upload task concluído!")
            Log.d(TAG, "📤 Bytes transferidos: ${snapshot.bytesTransferred}")
            Log.d(TAG, "📤 Total bytes: ${snapshot.totalByteCount}")
            Log.d(TAG, "📤 Obtendo URL de download...")
            
            // ✅ CORREÇÃO: Obter URL diretamente do storage reference após upload
            // Isso é mais confiável que tentar obter do snapshot
            val downloadUrl = storageRef.downloadUrl.await()
            val urlString = downloadUrl.toString()
            
            Log.d(TAG, "✅ ========================================")
            Log.d(TAG, "✅ UPLOAD CONCLUÍDO COM SUCESSO!")
            Log.d(TAG, "✅ URL: $urlString")
            Log.d(TAG, "✅ ========================================")
            urlString
        } catch (e: Exception) {
            Log.e(TAG, "❌ ========================================")
            Log.e(TAG, "❌ ERRO AO FAZER UPLOAD PARA FIREBASE STORAGE")
            Log.e(TAG, "❌ ========================================")
            Log.e(TAG, "❌ Storage path: $storagePath")
            Log.e(TAG, "❌ Arquivo: ${arquivo.absolutePath}")
            Log.e(TAG, "❌ Erro: ${e.message}")
            Log.e(TAG, "❌ Stack trace:", e)
            Log.e(TAG, "❌ ========================================")
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
                Log.d(TAG, "✅ URL é caminho local existente: $urlFirebase")
                return urlFirebase
            } else {
                // ✅ CRÍTICO: Arquivo local não existe (pode ter sido apagado)
                // Não retornar null, pois isso causaria perda da foto
                // Em vez disso, retornar null e logar o problema
                Log.w(TAG, "⚠️ Caminho local não existe (arquivo pode ter sido apagado): $urlFirebase")
                Log.w(TAG, "⚠️ Isso indica que a foto não foi enviada para Firebase Storage antes da sincronização")
                return null
            }
        }
        
        return try {
            Log.d(TAG, "Iniciando download: $urlFirebase")
            
            // Criar arquivo temporário local
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val nomeArquivo = "${tipoFoto}_${System.currentTimeMillis()}.jpg"
            val arquivoLocal = File(storageDir, nomeArquivo)
            
            // ✅ CORREÇÃO: Usar Firebase Storage SDK para download (mais confiável que HTTP direto)
            if (urlFirebase.contains("firebasestorage.googleapis.com")) {
                try {
                    val storage = FirebaseStorage.getInstance()
                    
                    // Tentar usar getReferenceFromUrl (método mais direto do SDK)
                    try {
                        val storageRef = storage.getReferenceFromUrl(urlFirebase)
                        val bytes = storageRef.getBytes(Long.MAX_VALUE).await()
                        arquivoLocal.writeBytes(bytes)
                        Log.d(TAG, "✅ Download concluído via SDK (getReferenceFromUrl): ${arquivoLocal.absolutePath} (${bytes.size} bytes)")
                        return arquivoLocal.absolutePath
                    } catch (e1: Exception) {
                        Log.w(TAG, "getReferenceFromUrl falhou, tentando parsing manual: ${e1.message}")
                        
                        // Fallback: parsing manual da URL
                        // Formato: https://firebasestorage.googleapis.com/v0/b/BUCKET/o/PATH%2FTO%2FFILE?alt=media&token=TOKEN
                        val uri = android.net.Uri.parse(urlFirebase)
                        val pathSegments = uri.pathSegments
                        
                        if (pathSegments.size >= 4 && pathSegments[0] == "v0" && pathSegments[1] == "b" && pathSegments[3] == "o") {
                            // Extrair caminho completo (decodificado)
                            val caminhoCodificado = pathSegments.subList(4, pathSegments.size).joinToString("/")
                            val caminhoDecodificado = java.net.URLDecoder.decode(caminhoCodificado, "UTF-8")
                            
                            // Usar referência ao caminho decodificado
                            val storageRef = storage.reference.child(caminhoDecodificado)
                            val bytes = storageRef.getBytes(Long.MAX_VALUE).await()
                            arquivoLocal.writeBytes(bytes)
                            
                            Log.d(TAG, "✅ Download concluído via SDK (parsing manual): ${arquivoLocal.absolutePath} (${bytes.size} bytes)")
                            return arquivoLocal.absolutePath
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao baixar via SDK, tentando HTTP direto: ${e.message}", e)
                    // Fallback para HTTP direto
                }
            }
            
            // Fallback: Baixar do Firebase Storage usando HTTP direto
            val url = java.net.URL(urlFirebase)
            val connection = url.openConnection()
            
            // Adicionar timeout
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()
            
            val inputStream = connection.getInputStream()
            var bytesLidos = 0L
            arquivoLocal.outputStream().use { outputStream ->
                val buffer = ByteArray(8192)
                var bytes = inputStream.read(buffer)
                while (bytes >= 0) {
                    outputStream.write(buffer, 0, bytes)
                    bytesLidos += bytes
                    bytes = inputStream.read(buffer)
                }
            }
            inputStream.close()
            
            if (bytesLidos == 0L) {
                throw Exception("Arquivo vazio baixado")
            }
            
            Log.d(TAG, "✅ Download concluído via HTTP: ${arquivoLocal.absolutePath} (${bytesLidos} bytes)")
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

