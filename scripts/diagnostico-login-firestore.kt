/**
 * Script de diagnóstico para testar busca de colaborador no Firestore
 * Executa diretamente na VM para identificar problemas sem depender do Crashlytics
 */

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

fun main() {
    println("=== DIAGNÓSTICO DE LOGIN FIRESTORE ===")
    println()
    
    // Inicializar Firebase
    val firestore = FirebaseFirestore.getInstance()
    val firebaseAuth = FirebaseAuth.getInstance()
    
    // Email do colaborador para testar
    val emailTeste = "user@teste.com" // Substituir pelo email real do User
    
    println("1. Verificando autenticação Firebase Auth...")
    println("   Autenticado: ${firebaseAuth.currentUser != null}")
    println("   UID: ${firebaseAuth.currentUser?.uid ?: "null"}")
    println()
    
    runBlocking {
        println("2. Testando busca via collectionGroup (sem autenticação)...")
        try {
            val querySnapshot = firestore.collectionGroup("items")
                .whereEqualTo("email", emailTeste)
                .get()
                .await()
            
            println("   ✅ Query executada com sucesso!")
            println("   Documentos encontrados: ${querySnapshot.size()}")
            
            querySnapshot.documents.forEach { doc ->
                println("   Documento encontrado:")
                println("      Path: ${doc.reference.path}")
                println("      ID: ${doc.id}")
                println("      Dados: ${doc.data}")
                
                // Verificar se é colaborador
                if (doc.reference.path.contains("/colaboradores/items/")) {
                    println("      ✅ É um colaborador!")
                    val aprovado = doc.getBoolean("aprovado") ?: false
                    val ativo = doc.getBoolean("ativo") ?: false
                    val nivelAcesso = doc.getString("nivel_acesso") ?: "null"
                    
                    println("      Aprovado: $aprovado")
                    println("      Ativo: $ativo")
                    println("      Nível Acesso: $nivelAcesso")
                    
                    // Verificar campos
                    println("      Campos presentes:")
                    doc.data.keys.forEach { key ->
                        println("         - $key: ${doc.data[key]}")
                    }
                } else {
                    println("      ⚠️ NÃO é um colaborador (outro tipo de item)")
                }
            }
        } catch (e: Exception) {
            println("   ❌ ERRO na busca collectionGroup:")
            println("      Tipo: ${e.javaClass.simpleName}")
            println("      Mensagem: ${e.message}")
            println("      Stack trace: ${e.stackTraceToString()}")
        }
        println()
        
        println("3. Testando busca direta na empresa_001...")
        try {
            val collectionRef = firestore.collection("empresas")
                .document("empresa_001")
                .collection("entidades")
                .document("colaboradores")
                .collection("items")
            
            val querySnapshot = collectionRef
                .whereEqualTo("email", emailTeste)
                .get()
                .await()
            
            println("   ✅ Query executada com sucesso!")
            println("   Documentos encontrados: ${querySnapshot.size()}")
            
            querySnapshot.documents.forEach { doc ->
                println("   Documento encontrado:")
                println("      Path: ${doc.reference.path}")
                println("      ID: ${doc.id}")
                println("      Dados: ${doc.data}")
            }
        } catch (e: Exception) {
            println("   ❌ ERRO na busca direta:")
            println("      Tipo: ${e.javaClass.simpleName}")
            println("      Mensagem: ${e.message}")
            println("      Stack trace: ${e.stackTraceToString()}")
        }
        println()
        
        println("4. Verificando estrutura dos dados salvos...")
        try {
            // Buscar TODOS os colaboradores para ver a estrutura
            val allColaboradores = firestore.collection("empresas")
                .document("empresa_001")
                .collection("entidades")
                .document("colaboradores")
                .collection("items")
                .limit(5)
                .get()
                .await()
            
            println("   Total de colaboradores (primeiros 5): ${allColaboradores.size()}")
            allColaboradores.documents.forEach { doc ->
                println("   Colaborador: ${doc.getString("nome")} (${doc.getString("email")})")
                println("      Campos: ${doc.data.keys.joinToString(", ")}")
                println("      Aprovado: ${doc.getBoolean("aprovado")}")
                println("      Ativo: ${doc.getBoolean("ativo")}")
                println("      Nível Acesso (nivel_acesso): ${doc.getString("nivel_acesso")}")
                println("      Nível Acesso (nivelAcesso): ${doc.getString("nivelAcesso")}")
                println()
            }
        } catch (e: Exception) {
            println("   ❌ ERRO ao buscar colaboradores:")
            println("      Tipo: ${e.javaClass.simpleName}")
            println("      Mensagem: ${e.message}")
        }
    }
    
    println("=== FIM DO DIAGNÓSTICO ===")
}
