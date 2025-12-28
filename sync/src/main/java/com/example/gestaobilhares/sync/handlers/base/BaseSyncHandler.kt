package com.example.gestaobilhares.sync.handlers.base

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.SyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.FieldNamingPolicy
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Classe base abstrata para handlers de sincronização.
 * Fornece funcionalidades comuns compartilhadas por todos os handlers:
 * - Acesso ao Firestore, AppRepository, SyncMetadataDao
 * - Helpers para timestamps e metadados
 * - Logging padronizado
 * - Gerenciamento de rotas
 */
abstract class BaseSyncHandler(
    protected val context: Context,
    protected val appRepository: AppRepository,
    protected val firestore: FirebaseFirestore,
    protected val networkUtils: NetworkUtils,
    protected val userSessionManager: UserSessionManager,
    protected val firebaseImageUploader: FirebaseImageUploader,
    private val injectedSyncMetadataDao: SyncMetadataDao? = null
) : SyncHandler {

    protected val syncMetadataDao: SyncMetadataDao by lazy {
        injectedSyncMetadataDao ?: AppDatabase.getDatabase(context).syncMetadataDao()
    }

    override var allowRouteBootstrap: Boolean = false

    protected companion object {
        const val COLLECTION_CLIENTES = "clientes"
        const val COLLECTION_CONTRATOS = "contratos"
        const val COLLECTION_ADITIVOS = "aditivos"
        const val COLLECTION_MESAS = "mesas"
        const val COLLECTION_ACERTOS = "acertos"
        const val COLLECTION_ACERTO_MESAS = "acerto_mesas"
        const val COLLECTION_DESPESAS = "despesas"
        const val COLLECTION_CATEGORIAS_DESPESA = "categorias_despesa"
        const val COLLECTION_TIPOS_DESPESA = "tipos_despesa"
        const val COLLECTION_ADITIVO_MESAS = "aditivo_mesas"
        const val COLLECTION_CONTRATO_MESAS = "contrato_mesas"
        const val COLLECTION_LOGS_AUDITORIA = "logs_auditoria_assinatura"
        
        // Caminho raiz no Firestore
        const val COLLECTION_EMPRESAS = "empresas"
        
        const val FIELD_ROTA_ID = "rota_id"
        const val FIELD_LAST_MODIFIED = "lastModified"
        const val FIRESTORE_WHERE_IN_LIMIT = 10
    }
    
    protected val TAG: String
        get() = "${javaClass.simpleName}"
    
    protected val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(java.time.LocalDateTime::class.java, com.google.gson.JsonSerializer<java.time.LocalDateTime> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        })
        .registerTypeAdapter(java.time.LocalDateTime::class.java, com.google.gson.JsonDeserializer<java.time.LocalDateTime> { json, _, _ ->
            java.time.LocalDateTime.parse(json.asString, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        })
        .registerTypeAdapter(Date::class.java, com.google.gson.JsonSerializer<Date> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.time)
        })
        .registerTypeAdapter(Date::class.java, com.google.gson.JsonDeserializer<Date> { json, _, _ ->
            if (json.isJsonPrimitive) {
                if (json.asJsonPrimitive.isNumber) return@JsonDeserializer Date(json.asLong)
                if (json.asJsonPrimitive.isString) {
                    return@JsonDeserializer try {
                        Date(json.asString.toLong())
                    } catch (e: Exception) {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).parse(json.asString)
                    }
                }
            } else if (json.isJsonObject) {
                val obj = json.asJsonObject
                if (obj.has("seconds")) {
                    return@JsonDeserializer Date(obj.get("seconds").asLong * 1000L + (obj.get("nanoseconds")?.asLong ?: 0L) / 1000000L)
                }
            }
            null
        })
        .registerTypeAdapter(com.google.firebase.Timestamp::class.java, com.google.gson.JsonSerializer<com.google.firebase.Timestamp> { src, _, context ->
            context.serialize(src.toDate())
        })
        .registerTypeAdapter(com.google.firebase.Timestamp::class.java, com.google.gson.JsonDeserializer<com.google.firebase.Timestamp> { json, _, context ->
            val date = context.deserialize<Date>(json, Date::class.java)
            if (date != null) com.google.firebase.Timestamp(date) else null
        })
        .create()
    
    protected val currentCompanyId: String
        get() = userSessionManager.getCurrentCompanyId()
    
    protected val currentUserId: Long
        get() = userSessionManager.getCurrentUserId()
    
    /**
     * Retorna a CollectionReference do Firestore para esta entidade.
     */
    protected fun getCollectionReference(collectionName: String): CollectionReference {
        val companyId = currentCompanyId
        Timber.tag(TAG).d("getCollectionRef: $collectionName (Empresa: $companyId)")
        return getCollectionReference(firestore, collectionName, companyId)
    }
    
    /**
     * Helper estático para obter CollectionReference com companyId.
     * Segue o padrão: companies/{companyId}/entidades/{collectionName}/items
     */
    protected fun getCollectionReference(
        firestore: FirebaseFirestore,
        collectionName: String,
        companyId: String
    ): CollectionReference {
        return firestore
            .collection(COLLECTION_EMPRESAS)
            .document(companyId)
            .collection("entidades")
            .document(collectionName)
            .collection("items")
    }
    
    /**
     * Obtém o último timestamp de sincronização (pull) para esta entidade.
     */
    protected suspend fun getLastSyncTimestamp(entityType: String): Long {
        return syncMetadataDao.obterUltimoTimestamp(entityType, currentUserId)
    }
    
    /**
     * Obtém o último timestamp de push para esta entidade.
     */
    protected suspend fun getLastPushTimestamp(entityType: String): Long {
        return syncMetadataDao.obterUltimoTimestamp("${entityType}_push", currentUserId)
    }
    
    /**
     * Salva metadados de sincronização (pull).
     */
    protected suspend fun saveSyncMetadata(
        entityType: String,
        syncCount: Int,
        durationMs: Long,
        bytesDownloaded: Long = 0L,
        error: String? = null,
        timestampOverride: Long? = null
    ) {
        val timestamp = timestampOverride ?: System.currentTimeMillis()
        syncMetadataDao.atualizarTimestamp(
            entityType = entityType,
            userId = currentUserId,
            timestamp = timestamp,
            count = syncCount,
            durationMs = durationMs,
            bytesDownloaded = bytesDownloaded,
            bytesUploaded = 0L,
            error = error,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Salva metadados de push.
     */
    protected suspend fun savePushMetadata(
        entityType: String,
        syncCount: Int,
        durationMs: Long,
        bytesUploaded: Long = 0L,
        error: String? = null
    ) {
        val pushEntityType = "${entityType}_push"
        syncMetadataDao.atualizarTimestamp(
            entityType = pushEntityType,
            userId = currentUserId,
            timestamp = System.currentTimeMillis(),
            count = syncCount,
            durationMs = durationMs,
            bytesDownloaded = 0L,
            bytesUploaded = bytesUploaded,
            error = error,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Converte entidade para Map para Firestore.
     * Converte campos Long que representam timestamps para Timestamp do Firestore.
     */
    protected fun <T> entityToMap(entity: T): MutableMap<String, Any> {
        val json = gson.toJson(entity)
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
        
        return map.mapKeys { it.key.toString() }.mapValues { entry ->
            val key = entry.key.lowercase()
            val value = entry.value
            
            when {
                // 1. Já é uma Date (difícil via GSON Map, mas possível se injetado manualmente)
                value is Date -> com.google.firebase.Timestamp(value)
                
                // 2. É uma String que pode ser uma Data (GSON serializa Date como String ISO)
                value is String && (key.contains("data") || key.contains("timestamp") || key.contains("time")) -> {
                    try {
                        // Tentar parsear como LocalDateTime primeiro se tiver o formato ISO
                        if (value.contains("T")) {
                            val ldt = java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            val instant = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant()
                            com.google.firebase.Timestamp(instant.epochSecond, instant.nano)
                        } else {
                            // Tentar parsear a string usando o formato do GSON
                            val date = gson.fromJson("\"$value\"", Date::class.java)
                            if (date != null) {
                                com.google.firebase.Timestamp(date)
                            } else {
                                value
                            }
                        }
                    } catch (e: Exception) {
                        value // Fallback para string se não for data válida
                    }
                }
                
                // 3. É um Long que representa um timestamp
                value is Long || (value is Double && value % 1 == 0.0) -> {
                    val longValue = if (value is Double) value.toLong() else value as Long
                    if (key.contains("data") || key.contains("timestamp") || key.contains("time")) {
                        // Converter milissegundos para segundos e nanossegundos
                        val seconds = longValue / 1000
                        val nanoseconds = ((longValue % 1000) * 1000000).toInt()
                        com.google.firebase.Timestamp(seconds, nanoseconds)
                    } else {
                        longValue
                    }
                }
                
                // 4. Manter outros tipos (Number, Boolean, etc.)
                else -> value
            }
        }.toMutableMap()
    }

    /**
     * Converte timestamp do Firestore para LocalDateTime.
     * Necessário para campos dataHora da entidade Despesa.
     */
    protected fun converterTimestampParaLocalDateTime(value: Any?): LocalDateTime? {
        return when (value) {
            is Timestamp -> {
                value.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
            is Long -> {
                Date(value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
            is String -> {
                try {
                    // Tentar parsear como ISO string ou timestamp
                    if (value.contains("T") || value.contains("-")) {
                        LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
                    } else {
                        Date(value.toLong()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }
    
    /**
     * Obtém o ID da rota de um cliente.
     */
    protected suspend fun getClienteRouteId(clienteId: Long?): Long? {
        if (clienteId == null || clienteId == 0L) return null
        val cliente = runCatching { appRepository.obterClientePorId(clienteId) }.getOrNull()
        return cliente?.rotaId
    }
    
    /**
     * Verifica se deve sincronizar dados baseado na rota.
     */
    protected suspend fun shouldSyncRouteData(
        rotaId: Long?,
        clienteId: Long? = null,
        allowUnknown: Boolean = true
    ): Boolean {
        if (userSessionManager.isAdmin()) return true
        val accessibleRoutes = getAccessibleRouteIds()
        if (accessibleRoutes.isEmpty()) {
            return allowRouteBootstrap // ✅ CORREÇÃO: Permitir se bootstrap estiver habilitado
        }
        val resolvedRouteId = when {
            rotaId != null && rotaId != 0L -> rotaId
            clienteId != null && clienteId != 0L -> getClienteRouteId(clienteId)
            else -> null
        }
        return when {
            resolvedRouteId == null -> allowUnknown
            else -> accessibleRoutes.contains(resolvedRouteId)
        }
    }
    
    /**
     * Obtém as rotas acessíveis do usuário.
     */
    private suspend fun getAccessibleRouteIds(): Set<Long> {
        if (userSessionManager.isAdmin()) {
            return emptySet() // Admin acessa todas
        }
        return userSessionManager.getUserAccessibleRoutes(context).toSet()
    }

    /**
     * Valida que uma entidade referenciada existe localmente.
     * Se não existir, tenta buscar do Firestore.
     */
    protected suspend fun ensureEntityExists(
        entityType: String,
        entityId: Long
    ): Boolean {
        return try {
            when (entityType) {
                "cliente" -> {
                    val exists = appRepository.obterClientePorId(entityId) != null
                    if (!exists) {
                        Timber.tag(TAG).w("Cliente $entityId não encontrado localmente - falha na FK")
                        // Nota: pull de clientes ausentes pode ser implementado aqui se necessário
                        false
                    } else true
                }
                "mesa" -> {
                    val exists = appRepository.obterMesaPorId(entityId) != null
                    if (!exists) {
                        Timber.tag(TAG).w("Mesa $entityId não encontrada localmente - falha na FK")
                        false
                    } else true
                }
                else -> {
                    Timber.tag(TAG).w("Tipo de entidade desconhecido para validação: $entityType")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao validar FK para $entityType $entityId")
            false
        }
    }

    /**
     * Busca documentos aplicando filtro de rota se necessário.
     * Implementa PAGINAÇÃO para lidar com grandes volumes de dados.
     */
    protected suspend fun fetchDocumentsWithRouteFilter(
        collectionRef: CollectionReference,
        routeField: String?,
        lastSyncTimestamp: Long,
        timestampField: String = FIELD_LAST_MODIFIED
    ): List<DocumentSnapshot> {
        val queries = buildRouteAwareQueries(collectionRef, routeField, lastSyncTimestamp, timestampField)
        if (queries.isEmpty()) return emptyList()
        
        val documents = mutableListOf<DocumentSnapshot>()
        for (query in queries) {
            // ✅ CORREÇÃO: Usar paginação para cada query (especialmente importante para tabelas grandes como clientes/acertos)
            executePaginatedQuery(query) { batch ->
                documents += batch
            }
        }
        return documents
    }

    /**
     * Constrói queries cientes de rota.
     */
    protected suspend fun buildRouteAwareQueries(
        collectionRef: CollectionReference,
        routeField: String?,
        lastSyncTimestamp: Long,
        timestampField: String
    ): List<Query> {
        if (routeField == null || userSessionManager.isAdmin()) {
            return listOf(applyTimestampFilter(collectionRef, lastSyncTimestamp, timestampField))
        }
        
        val accessibleRoutes = getAccessibleRouteIds()
        if (accessibleRoutes.isEmpty()) {
            // Nota: Bootstrap removido por simplicidade e segurança nos handlers individuais
            Timber.tag(TAG).w(" Usuário sem rotas atribuídas - nenhuma query será executada para $routeField")
            return emptyList()
        }
        
        val routeChunks = accessibleRoutes.toList().chunked(FIRESTORE_WHERE_IN_LIMIT)
        return routeChunks.map { chunk ->
            val baseQuery: Query = if (chunk.size == 1) {
                collectionRef.whereEqualTo(routeField, chunk.first())
            } else {
                collectionRef.whereIn(routeField, chunk)
            }
            applyTimestampFilter(baseQuery, lastSyncTimestamp, timestampField)
        }
    }

    /**
     * Aplica filtro de timestamp em uma query.
     */
    protected fun applyTimestampFilter(
        query: Query,
        lastSyncTimestamp: Long,
        timestampField: String
    ): Query {
        return if (lastSyncTimestamp > 0L) {
            query.whereGreaterThan(timestampField, Timestamp(Date(lastSyncTimestamp)))
                .orderBy(timestampField)
        } else {
            query.orderBy(timestampField)
        }
    }

    /**
     * Extrai clienteId de um mapa de dados, lidando com diferentes formatos (cID, cliente_id, etc).
     */
    protected fun extrairClienteId(data: Map<String, Any?>): Long? {
        val rawValue = data["clienteId"]
            ?: data["cliente_id"]
            ?: data["clienteID"]

        return when (rawValue) {
            is Number -> rawValue.toLong()
            is String -> rawValue.trim().toLongOrNull()
            else -> null
        }
    }

    /**
     * Busca todos os documentos aplicando filtro de rota se necessário com PAGINAÇÃO.
     */
    protected suspend fun fetchAllDocumentsWithRouteFilter(
        collectionRef: CollectionReference,
        routeField: String?
    ): List<DocumentSnapshot> {
        if (routeField == null || userSessionManager.isAdmin()) {
            val documents = mutableListOf<DocumentSnapshot>()
            executePaginatedQuery(collectionRef) { batch ->
                documents += batch
            }
            return documents
        }
        
        val accessibleRoutes = getAccessibleRouteIds()
        if (accessibleRoutes.isEmpty()) {
            Timber.tag(TAG).w(" Nenhuma rota atribuída ao usuário - resultado vazio para $routeField")
            return emptyList()
        }
        
        val documents = mutableListOf<DocumentSnapshot>()
        for (chunk in accessibleRoutes.toList().chunked(FIRESTORE_WHERE_IN_LIMIT)) {
            val query = if (chunk.size == 1) {
                collectionRef.whereEqualTo(routeField, chunk.first())
            } else {
                collectionRef.whereIn(routeField, chunk)
            }
            executePaginatedQuery(query) { batch ->
                documents += batch
            }
        }
        return documents
    }

    /**
     * ✅ NOVO (Phase 6): Executa query Firestore com paginação automática.
     * Processa documentos em lotes para evitar problemas de memória e timeout.
     */
    protected suspend fun executePaginatedQuery(
        query: Query,
        batchSize: Int = 500,
        processor: suspend (List<DocumentSnapshot>) -> Unit
    ): Int {
        var lastDocument: DocumentSnapshot? = null
        var hasMore = true
        var totalProcessed = 0
        
        while (hasMore) {
            try {
                var paginatedQuery = query.limit(batchSize.toLong())
                if (lastDocument != null) {
                    paginatedQuery = paginatedQuery.startAfter(lastDocument)
                }
                
                val snapshot = paginatedQuery.get().await()
                val documents = snapshot.documents
                
                if (documents.isEmpty()) {
                    break
                }
                
                processor(documents)
                
                totalProcessed += documents.size
                Timber.tag(TAG).d("📦 Processado lote sync: ${documents.size} documentos (total: $totalProcessed)")
                
                hasMore = documents.size == batchSize
                lastDocument = documents.lastOrNull()
                
            } catch (e: Exception) {
                Timber.tag(TAG).e("❌ Erro ao processar lote paginado: ${e.message}", e)
                hasMore = false
                throw e // Propagar erro para o handler tratar (geralmente vai retornar Result.failure)
            }
        }
        
        if (totalProcessed > batchSize) {
             Timber.tag(TAG).i("✅ Paginação concluída: $totalProcessed documentos processados em múltiplos lotes")
        }
        return totalProcessed
    }

    /**
     * Converte Timestamp do Firestore para Date do Java.
     */
    protected fun converterTimestampParaDate(value: Any?): Date? {
        return when (value) {
            is Timestamp -> value.toDate()
            is Long -> Date(value)
            is String -> try {
                Date(value.toLong())
            } catch (e: Exception) {
                null
            }
            else -> null
        }
    }
}

