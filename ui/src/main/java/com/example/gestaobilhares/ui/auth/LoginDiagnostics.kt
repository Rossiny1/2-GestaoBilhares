package com.example.gestaobilhares.ui.auth

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Classe de diagnóstico local para problemas de login
 * Cria logs locais que não dependem do Crashlytics
 */
object LoginDiagnostics {
    
    private const val LOG_TAG = "LoginDiagnostics"
    private val logFile = File("/data/data/com.example.gestaobilhares/files/login_diagnostics.log")
    
    /**
     * Loga uma mensagem tanto no Logcat quanto em arquivo local
     */
    fun log(message: String, level: Int = Log.INFO) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $message"
        
        // Log no Logcat
        when (level) {
            Log.DEBUG -> Timber.d(LOG_TAG, message)
            Log.INFO -> Timber.i(LOG_TAG, message)
            Log.WARN -> Timber.w(LOG_TAG, message)
            Log.ERROR -> Timber.e(LOG_TAG, message)
            else -> Timber.d(LOG_TAG, message)
        }
        
        // Log em arquivo local (tentar, mas não falhar se não conseguir)
        try {
            logFile.parentFile?.mkdirs()
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logMessage)
            }
        } catch (e: Exception) {
            // Ignorar erros de escrita de arquivo
            Timber.w(LOG_TAG, "Não foi possível escrever no arquivo de log: ${e.message}")
        }
    }
    
    /**
     * Testa a busca de colaborador diretamente e retorna resultado detalhado
     */
    suspend fun testarBuscaColaborador(email: String): DiagnosticResult {
        val firestore = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        
        log("=== INICIANDO DIAGNÓSTICO DE BUSCA ===")
        log("Email: $email")
        log("Firebase Auth autenticado: ${firebaseAuth.currentUser != null}")
        log("Firebase UID: ${firebaseAuth.currentUser?.uid ?: "null"}")
        
        val result = DiagnosticResult()
        result.email = email
        result.firebaseAuthAutenticado = firebaseAuth.currentUser != null
        result.firebaseUid = firebaseAuth.currentUser?.uid
        
        // Teste 1: collectionGroup com email
        try {
            log("Teste 1: collectionGroup('items').whereEqualTo('email', '$email')")
            val querySnapshot = firestore.collectionGroup("items")
                .whereEqualTo("email", email)
                .get()
                .await()
            
            result.collectionGroupResult = querySnapshot.size()
            log("   Resultado: ${querySnapshot.size()} documentos")
            
            querySnapshot.documents.forEach { doc ->
                val isColaborador = doc.reference.path.contains("/colaboradores/items/")
                log("   Documento: ${doc.reference.path} (é colaborador: $isColaborador)")
                
                if (isColaborador) {
                    result.colaboradorEncontrado = true
                    result.colaboradorPath = doc.reference.path
                    result.colaboradorId = doc.id
                    
                    // Verificar campos
                    val data = doc.data ?: emptyMap()
                    result.camposPresentes = data.keys.toList()
                    result.aprovado = data["aprovado"] as? Boolean ?: false
                    result.ativo = data["ativo"] as? Boolean ?: false
                    result.nivelAcesso = data["nivel_acesso"]?.toString() ?: data["nivelAcesso"]?.toString()
                    result.temFirebaseUid = data.containsKey("firebase_uid") || data.containsKey("firebaseUid")
                    
                    log("   Aprovado: ${result.aprovado}")
                    log("   Ativo: ${result.ativo}")
                    log("   Nível Acesso: ${result.nivelAcesso}")
                    log("   Campos: ${result.camposPresentes.joinToString(", ")}")
                }
            }
        } catch (e: Exception) {
            result.erroCollectionGroup = e.message
            log("   ❌ ERRO: ${e.message}", Log.ERROR)
            log("   Stack: ${e.stackTraceToString()}", Log.ERROR)
        }
        
        // Teste 2: Busca direta na empresa_001
        try {
            log("Teste 2: Busca direta em empresa_001/entidades/colaboradores/items")
            val collectionRef = firestore.collection("empresas")
                .document("empresa_001")
                .collection("entidades")
                .document("colaboradores")
                .collection("items")
            
            val querySnapshot = collectionRef
                .whereEqualTo("email", email)
                .get()
                .await()
            
            result.buscaDiretaResult = querySnapshot.size()
            log("   Resultado: ${querySnapshot.size()} documentos")
            
            if (querySnapshot.size() > 0) {
                val doc = querySnapshot.documents.first()
                result.colaboradorEncontradoDireto = true
                result.colaboradorPathDireto = doc.reference.path
                log("   ✅ Colaborador encontrado: ${doc.reference.path}")
            }
        } catch (e: Exception) {
            result.erroBuscaDireta = e.message
            log("   ❌ ERRO: ${e.message}", Log.ERROR)
        }
        
        log("=== FIM DO DIAGNÓSTICO ===")
        log("Resultado final: ${if (result.colaboradorEncontrado) "ENCONTRADO" else "NÃO ENCONTRADO"}")
        
        return result
    }
    
    /**
     * Resultado do diagnóstico
     */
    data class DiagnosticResult(
        var email: String = "",
        var firebaseAuthAutenticado: Boolean = false,
        var firebaseUid: String? = null,
        var collectionGroupResult: Int = 0,
        var colaboradorEncontrado: Boolean = false,
        var colaboradorPath: String? = null,
        var colaboradorId: String? = null,
        var camposPresentes: List<String> = emptyList(),
        var aprovado: Boolean = false,
        var ativo: Boolean = false,
        var nivelAcesso: String? = null,
        var temFirebaseUid: Boolean = false,
        var erroCollectionGroup: String? = null,
        var buscaDiretaResult: Int = 0,
        var colaboradorEncontradoDireto: Boolean = false,
        var colaboradorPathDireto: String? = null,
        var erroBuscaDireta: String? = null
    ) {
        fun toSummary(): String {
            return """
                === RESUMO DO DIAGNÓSTICO ===
                Email: $email
                Firebase Auth: ${if (firebaseAuthAutenticado) "Autenticado" else "Não autenticado"}
                Firebase UID: $firebaseUid
                
                CollectionGroup:
                  - Resultado: $collectionGroupResult documentos
                  - Colaborador encontrado: ${if (colaboradorEncontrado) "SIM" else "NÃO"}
                  ${if (colaboradorPath != null) "- Path: $colaboradorPath" else ""}
                  ${if (erroCollectionGroup != null) "- ERRO: $erroCollectionGroup" else ""}
                
                Busca Direta:
                  - Resultado: $buscaDiretaResult documentos
                  - Colaborador encontrado: ${if (colaboradorEncontradoDireto) "SIM" else "NÃO"}
                  ${if (colaboradorPathDireto != null) "- Path: $colaboradorPathDireto" else ""}
                  ${if (erroBuscaDireta != null) "- ERRO: $erroBuscaDireta" else ""}
                
                Dados do Colaborador (se encontrado):
                  - Aprovado: $aprovado
                  - Ativo: $ativo
                  - Nível Acesso: $nivelAcesso
                  - Tem Firebase UID: $temFirebaseUid
                  - Campos: ${camposPresentes.joinToString(", ")}
            """.trimIndent()
        }
    }
}
