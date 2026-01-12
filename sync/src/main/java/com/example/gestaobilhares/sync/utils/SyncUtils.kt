package com.example.gestaobilhares.sync.utils

import android.content.Context
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.StatusAcerto
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilitários para sincronização de dados.
 * Contém métodos de conversão e manipulação de dados.
 */
class SyncUtils {
    
    companion object {
        private const val TAG = "SyncUtils"
        
        // ✅ CORREÇÃO CRÍTICA: Usar IDENTITY para manter camelCase dos campos
        val gson: Gson by lazy { 
            GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.IDENTITY)
                .create() 
        }
        
        /**
         * ✅ CORREÇÃO CRÍTICA: Usar classe estática interna ao invés de classe anônima
         * Isso garante que o ProGuard/R8 preserve as assinaturas genéricas corretamente.
         */
        private val mapType: java.lang.reflect.Type = MapTypeTokenInstance.type
        
        object MapTypeTokenInstance {
            val type = object : com.google.gson.reflect.TypeToken<MutableMap<String, Any>>() {}.type
        }
    }
    
    /**
     * Converte entidade para Map para Firestore.
     * Similar ao método do BaseSyncHandler, mas adaptado para SyncRepository.
     */
    fun <T> entityToMap(entity: T): MutableMap<String, Any> {
        val json = gson.toJson(entity)
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
        
        return map.mapValues { entry ->
            val key = entry.key
            val value = entry.value
            
            when {
                // 1. Já é uma Date
                value is Date -> com.google.firebase.Timestamp(value)
                
                // 2. É uma String que pode ser uma Data
                value is String && (key.lowercase().contains("data") || key.lowercase().contains("timestamp") || key.lowercase().contains("time")) -> {
                    try {
                        if (value.contains("T")) {
                            val ldt = java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            val instant = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant()
                            com.google.firebase.Timestamp(instant.epochSecond, instant.nano)
                        } else {
                            val date = gson.fromJson("\"$value\"", Date::class.java)
                            if (date != null) {
                                com.google.firebase.Timestamp(date)
                            } else {
                                value
                            }
                        }
                    } catch (e: Exception) {
                        value
                    }
                }
                
                // 3. É um Long que representa um timestamp
                (value is Long || (value is Double && value % 1 == 0.0)) -> {
                    val longValue = if (value is Double) value.toLong() else value as Long
                    if (key.lowercase().contains("data") || key.lowercase().contains("timestamp") || key.lowercase().contains("time")) {
                        val seconds = longValue / 1000
                        val nanoseconds = ((longValue % 1000) * 1000000).toInt()
                        com.google.firebase.Timestamp(seconds, nanoseconds)
                    } else {
                        longValue
                    }
                }
                
                // 4. Manter outros tipos
                else -> value
            }
        }.toMutableMap()
    }
    
    /**
     * Retorna a referência da coleção de uma entidade dentro da estrutura hierárquica.
     * Caminho: empresas/{companyId}/entidades/{collectionName}/items
     */
    fun getCollectionReference(
        firestore: FirebaseFirestore, 
        collectionName: String, 
        companyId: String = "empresa_001"
    ): CollectionReference {
        val collectionPath = "empresas/$companyId/entidades/$collectionName/items"
        Timber.tag(TAG).d("📂 getCollectionReference: $collectionPath")
        return firestore.collection(collectionPath)
    }
    
    /**
     * 🗑️ MÉTODO LEGADO: Mantido para compatibilidade
     */
    @Deprecated("Use getCollectionReference() com companyId", ReplaceWith("getCollectionReference(firestore, collectionName, companyId)"))
    fun getCollectionPath(collectionName: String, companyId: String = "empresa_001"): String {
        return "empresas/$companyId/entidades/$collectionName"
    }
    
    /**
     * Converte DocumentSnapshot do Firestore para entidade Acerto.
     */
    fun documentToAcerto(doc: DocumentSnapshot): Acerto? {
        val acertoData = doc.data?.toMutableMap() ?: run {
            Timber.tag(TAG).w("⚠️ Acerto ${doc.id} sem dados")
            return null
        }
        
        try {
            // Converter dados do Firestore para Acerto
            val acertoFirestore = convertMapToAcerto(acertoData, doc.id)
            Timber.tag(TAG).d("✅ Acerto ${doc.id} convertido: ${acertoFirestore.dataAcerto}")
            return acertoFirestore
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao converter DocumentSnapshot para Acerto: ${doc.id}")
            return null
        }
    }
    
    /**
     * Extrai timestamp do campo de data do acerto.
     */
    fun extrairDataAcertoMillis(doc: DocumentSnapshot): Long {
        val rawValue = doc.get("dataAcerto")
            ?: doc.get("data_acerto")
            ?: doc.get("dataHora")
            ?: doc.get("data")
            ?: return 0L
        
        return when (rawValue) {
            is Timestamp -> rawValue.toDate().time
            is Date -> rawValue.time
            is String -> parseDataAcertoString(rawValue)
            is Long -> rawValue
            is Double -> rawValue.toLong()
            else -> 0L
        }
    }
    
    /**
     * Parse de string de data para timestamp.
     */
    fun parseDataAcertoString(value: String): Long {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return 0L
        
        // Tentar diferentes formatos de data
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy"
        )
        
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(trimmed)
                if (date != null) {
                    return date.time
                }
            } catch (e: ParseException) {
                // Continuar para o próximo formato
            }
        }
        
        Timber.tag(TAG).w("⚠️ Não foi possível parsear data: $value")
        return 0L
    }
    
    /**
     * Extrai clienteId dos dados do acerto.
     */
    fun extrairClienteId(acertoData: Map<String, Any?>): Long? {
        val rawValue = acertoData["clienteId"]
            ?: acertoData["cliente_id"]
            ?: acertoData["clienteID"]
            ?: return null
        
        return when (rawValue) {
            is Number -> rawValue.toLong()
            is String -> rawValue.toLongOrNull()
            else -> null
        }
    }
    
    /**
     * Converte Map para entidade Acerto.
     */
    private fun convertMapToAcerto(data: MutableMap<String, Any?>, documentId: String): Acerto {
        // Implementar conversão específica para Acerto
        // Este método precisa ser adaptado conforme a estrutura da entidade Acerto
        return Acerto(
            id = documentId.toLongOrNull() ?: 0L,
            dataAcerto = extrairDataAcertoMillisFromMap(data),
            clienteId = extrairClienteId(data) ?: 0L,
            colaboradorId = data["colaborador_id"] as? Long,
            rotaId = data["rota_id"] as? Long,
            cicloId = data["ciclo_id"] as? Long,
            periodoInicio = extrairDataAcertoMillisFromMap(data),
            periodoFim = extrairDataAcertoMillisFromMap(data),
            totalMesas = data["total_mesas"] as? Double ?: 0.0,
            debitoAnterior = data["debito_anterior"] as? Double ?: 0.0,
            valorTotal = data["valor_total"] as? Double ?: 0.0,
            desconto = data["desconto"] as? Double ?: 0.0,
            valorComDesconto = data["valor_com_desconto"] as? Double ?: 0.0,
            valorRecebido = data["valor_recebido"] as? Double ?: 0.0,
            debitoAtual = data["debito_atual"] as? Double ?: 0.0,
            status = when (val status = data["status"] as? String) {
                "PENDENTE" -> StatusAcerto.PENDENTE
                "FINALIZADO" -> StatusAcerto.FINALIZADO
                "CANCELADO" -> StatusAcerto.CANCELADO
                else -> StatusAcerto.PENDENTE
            },
            observacoes = data["observacoes"] as? String,
            dataCriacao = extrairDataAcertoMillisFromMap(data),
            dataFinalizacao = null,
            representante = null,
            tipoAcerto = null,
            panoTrocado = false,
            numeroPano = null,
            dadosExtrasJson = null
        )
    }
    
    /**
     * Extrai dataAcerto do Map.
     */
    private fun extrairDataAcertoMillisFromMap(data: Map<String, Any?>): Long {
        val rawValue = data["dataAcerto"]
            ?: data["data_acerto"]
            ?: data["dataHora"]
            ?: data["data"]
            ?: return System.currentTimeMillis()
        
        return when (rawValue) {
            is Timestamp -> rawValue.toDate().time
            is Date -> rawValue.time
            is String -> parseDataAcertoString(rawValue)
            is Long -> rawValue
            is Double -> rawValue.toLong()
            else -> System.currentTimeMillis()
        }
    }
}
