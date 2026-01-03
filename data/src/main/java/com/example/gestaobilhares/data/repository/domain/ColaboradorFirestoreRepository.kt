package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.entities.Colaborador
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.FieldNamingPolicy
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository especializado para operações Firestore relacionadas a colaboradores.
 * 
 * Responsabilidades:
 * - Criar colaborador no Firestore
 * - Buscar colaborador do Firestore
 * - Atualizar status de aprovação no Firestore
 * - Sincronizar dados entre local e Firestore
 */
@Singleton
class ColaboradorFirestoreRepository @Inject constructor() {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Gson para serialização/deserialização - padrão LOWER_CASE_WITH_UNDERSCORES para Firestore
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    /**
     * Obtém o documento do colaborador no Firestore (força leitura do servidor)
     */
    suspend fun getColaboradorDoc(empresaId: String, uid: String): com.google.firebase.firestore.DocumentSnapshot {
        val docRef = firestore
            .collection("empresas")
            .document(empresaId)
            .collection("colaboradores")
            .document(uid)
        
        // ✅ FORÇAR LEITURA DO SERVIDOR para ignorar cache
        return docRef.get(Source.SERVER).await()
    }
    
    /**
     * Busca colaborador pelo UID no Firestore
     * Caminho: empresas/{empresaId}/colaboradores/{uid}
     * 
     * @param empresaId ID da empresa (padrão: "empresa_001")
     * @param uid Firebase UID do usuário
     * @return Colaborador se encontrado, null caso contrário
     */
    suspend fun getColaboradorByUid(empresaId: String, uid: String): Colaborador? {
        return try {
            Timber.d("ColaboradorFirestoreRepository", "🔍 [FIRESTORE] Buscando colaborador por UID")
            Timber.d("ColaboradorFirestoreRepository", "   UID: $uid, Empresa: $empresaId")
            
            val doc = getColaboradorDoc(empresaId, uid)
            
            Timber.d("ColaboradorFirestoreRepository", "   Path: ${doc.reference.path}")
            Timber.d("ColaboradorFirestoreRepository", "   Exists: ${doc.exists()}")
            
            if (!doc.exists()) {
                Timber.d("ColaboradorFirestoreRepository", "⚠️ Documento não existe")
                return null
            }
            
            val data = doc.data
            if (data == null) {
                Timber.e("ColaboradorFirestoreRepository", "❌ Documento existe mas data é null")
                return null
            }
            
            // ✅ LOGS OBRIGATÓRIOS: Valores diretos do Firestore
            val aprovadoDireto = doc.getBoolean("aprovado") ?: false
            val ativoDireto = doc.getBoolean("ativo") ?: true
            
            Timber.d("ColaboradorFirestoreRepository", "📋 [FIRESTORE] Valores diretos:")
            Timber.d("ColaboradorFirestoreRepository", "   aprovado: $aprovadoDireto")
            Timber.d("ColaboradorFirestoreRepository", "   ativo: $ativoDireto")
            
            // Converter Timestamps para Date
            val dataConvertida = data.toMutableMap()
            
            val dateFields = listOf(
                "data_cadastro", "data_ultima_atualizacao", "data_aprovacao", 
                "data_ultimo_acesso", "data_nascimento"
            )
            
            dateFields.forEach { field ->
                if (data.containsKey(field)) {
                    val v = data[field]
                    val dateValue = when {
                        v is Timestamp -> v.toDate()
                        v is Date -> v
                        v is Long -> Date(v)
                        else -> null
                    }
                    if (dateValue != null) {
                        dataConvertida[field] = dateValue.time // Converter para Long (millis)
                    }
                }
            }
            
            // Converter Map para JSON e depois para Colaborador
            val json = gson.toJson(dataConvertida)
            val colaborador = gson.fromJson(json, Colaborador::class.java)
            
            // ✅ GARANTIR que campos boolean estão corretos (usar valores diretos do Firestore)
            val colaboradorCorrigido = colaborador.copy(
                aprovado = aprovadoDireto,
                ativo = ativoDireto
            )
            
            Timber.d("ColaboradorFirestoreRepository", "✅ Colaborador convertido: ${colaboradorCorrigido.nome}")
            Timber.d("ColaboradorFirestoreRepository", "   Aprovado: ${colaboradorCorrigido.aprovado}")
            
            colaboradorCorrigido
        } catch (e: Exception) {
            Timber.e(e, "❌ [FIRESTORE] Erro ao buscar colaborador: ${e.message}")
            null
        }
    }
    
    /**
     * Cria colaborador no Firestore
     * Caminho: empresas/{empresaId}/colaboradores/{uid}
     * 
     * @param colaborador Colaborador a ser criado
     * @param empresaId ID da empresa
     * @param uid Firebase UID do usuário
     */
    suspend fun criarColaboradorNoFirestore(
        colaborador: Colaborador,
        empresaId: String,
        uid: String
    ) {
        try {
            Timber.d("ColaboradorFirestoreRepository", "🔧 [FIRESTORE] Criando colaborador: $uid")
            
            val docRef = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("colaboradores")
                .document(uid)
            
            // Converter para Map usando Gson (snake_case)
            val colaboradorJson = gson.toJson(colaborador)
            @Suppress("UNCHECKED_CAST")
            val colaboradorMap = gson.fromJson(colaboradorJson, Map::class.java) as? MutableMap<String, Any?> 
                ?: mutableMapOf()
            
            // Adicionar campos adicionais
            colaboradorMap["room_id"] = colaborador.id
            colaboradorMap["id"] = colaborador.id
            colaboradorMap["last_modified"] = FieldValue.serverTimestamp()
            colaboradorMap["sync_timestamp"] = FieldValue.serverTimestamp()
            
            // Converter datas para Timestamp
            colaboradorMap["data_cadastro"] = Timestamp(Date(colaborador.dataCadastro))
            colaboradorMap["data_ultima_atualizacao"] = Timestamp(Date(colaborador.dataUltimaAtualizacao))
            colaborador.dataAprovacao?.let { colaboradorMap["data_aprovacao"] = Timestamp(Date(it)) }
            colaborador.dataUltimoAcesso?.let { colaboradorMap["data_ultimo_acesso"] = Timestamp(Date(it)) }
            
            // ✅ GARANTIR campos boolean corretos
            colaboradorMap["aprovado"] = colaborador.aprovado
            colaboradorMap["ativo"] = colaborador.ativo
            colaboradorMap["primeiro_acesso"] = colaborador.primeiroAcesso
            colaboradorMap["nivel_acesso"] = colaborador.nivelAcesso.name
            
            // ✅ GARANTIR campos obrigatórios
            colaboradorMap["nome"] = colaborador.nome
            colaboradorMap["email"] = colaborador.email
            colaboradorMap["firebase_uid"] = uid
            colaboradorMap["firebaseUid"] = uid
            
            // ✅ MULTI-TENANCY
            colaboradorMap["empresa_id"] = empresaId
            colaboradorMap["companyId"] = empresaId
            
            Timber.d("ColaboradorFirestoreRepository", "📋 Campos boolean:")
            Timber.d("ColaboradorFirestoreRepository", "   aprovado: ${colaboradorMap["aprovado"]}")
            Timber.d("ColaboradorFirestoreRepository", "   ativo: ${colaboradorMap["ativo"]}")
            
            // ✅ Usar merge para não sobrescrever campos existentes
            docRef.set(colaboradorMap, com.google.firebase.firestore.SetOptions.merge()).await()
            
            Timber.d("ColaboradorFirestoreRepository", "✅ Colaborador criado no Firestore: ${colaborador.nome}")
        } catch (e: Exception) {
            Timber.e(e, "❌ [FIRESTORE] Erro ao criar colaborador: ${e.message}")
            throw e
        }
    }
    
    /**
     * Atualiza o status de aprovação no Firestore
     * 
     * @param empresaId ID da empresa
     * @param uid Firebase UID do usuário
     * @param aprovado Status de aprovação
     * @param dataAprovacao Data de aprovação (opcional)
     * @param aprovadoPor Quem aprovou (opcional)
     */
    suspend fun atualizarStatusAprovacao(
        empresaId: String,
        uid: String,
        aprovado: Boolean,
        dataAprovacao: Long? = null,
        aprovadoPor: String? = null
    ) {
        try {
            Timber.d("ColaboradorFirestoreRepository", "🔧 [FIRESTORE] Atualizando status aprovação: $uid")
            Timber.d("ColaboradorFirestoreRepository", "   Aprovado: $aprovado")
            
            val docRef = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("colaboradores")
                .document(uid)
            
            val updateMap = mutableMapOf<String, Any>(
                "aprovado" to aprovado,
                "last_modified" to FieldValue.serverTimestamp()
            )
            
            if (dataAprovacao != null) {
                updateMap["data_aprovacao"] = Timestamp(Date(dataAprovacao))
            }
            
            if (aprovadoPor != null) {
                updateMap["aprovado_por"] = aprovadoPor
            }
            
            docRef.update(updateMap).await()
            
            Timber.d("ColaboradorFirestoreRepository", "✅ Status aprovação atualizado no Firestore")
        } catch (e: Exception) {
            Timber.e(e, "❌ [FIRESTORE] Erro ao atualizar status aprovação: ${e.message}")
            throw e
        }
    }
    
    /**
     * Sincroniza colaborador completo para o Firestore (preservando aprovado=true se necessário)
     * 
     * @param colaborador Colaborador a ser sincronizado
     * @param empresaId ID da empresa
     * @param uid Firebase UID do usuário
     * @param preservarAprovado Se true, garante que aprovado=true seja mantido
     */
    suspend fun sincronizarColaboradorCompleto(
        colaborador: Colaborador,
        empresaId: String,
        uid: String,
        preservarAprovado: Boolean = false
    ) {
        try {
            Timber.d("ColaboradorFirestoreRepository", "🔄 [FIRESTORE] Sincronizando colaborador completo: $uid")
            
            val docRef = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("colaboradores")
                .document(uid)
            
            // Converter para Map
            val colaboradorJson = gson.toJson(colaborador)
            @Suppress("UNCHECKED_CAST")
            val colaboradorMap = gson.fromJson(colaboradorJson, Map::class.java) as? MutableMap<String, Any?> 
                ?: mutableMapOf()
            
            // Adicionar campos adicionais
            colaboradorMap["room_id"] = colaborador.id
            colaboradorMap["id"] = colaborador.id
            colaboradorMap["last_modified"] = FieldValue.serverTimestamp()
            colaboradorMap["sync_timestamp"] = FieldValue.serverTimestamp()
            colaboradorMap["data_cadastro"] = Timestamp(Date(colaborador.dataCadastro))
            colaboradorMap["data_ultima_atualizacao"] = Timestamp(Date(colaborador.dataUltimaAtualizacao))
            colaborador.dataAprovacao?.let { colaboradorMap["data_aprovacao"] = Timestamp(Date(it)) }
            colaboradorMap["aprovado"] = if (preservarAprovado) true else colaborador.aprovado
            colaboradorMap["ativo"] = colaborador.ativo
            colaboradorMap["primeiro_acesso"] = colaborador.primeiroAcesso
            colaboradorMap["nivel_acesso"] = colaborador.nivelAcesso.name
            colaboradorMap["nome"] = colaborador.nome
            colaboradorMap["email"] = colaborador.email
            colaboradorMap["firebase_uid"] = uid
            colaboradorMap["firebaseUid"] = uid
            colaboradorMap["empresa_id"] = empresaId
            colaboradorMap["companyId"] = empresaId
            
            // Usar merge para não sobrescrever campos existentes
            docRef.set(colaboradorMap, com.google.firebase.firestore.SetOptions.merge()).await()
            
            // ✅ Se preservarAprovado=true, garantir após merge
            if (preservarAprovado) {
                docRef.update("aprovado", true).await()
            }
            
            Timber.d("ColaboradorFirestoreRepository", "✅ Colaborador sincronizado no Firestore")
        } catch (e: Exception) {
            Timber.e(e, "❌ [FIRESTORE] Erro ao sincronizar colaborador: ${e.message}")
            throw e
        }
    }
}
