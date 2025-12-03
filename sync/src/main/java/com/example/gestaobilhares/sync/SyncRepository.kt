package com.example.gestaobilhares.sync

import android.content.Context
import android.util.Log
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.utils.UserSessionManager
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.io.File
import kotlin.math.roundToInt

/**
 * Repository especializado para sincronização de dados.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 * 
 * Responsabilidades:
 * - Sincronização bidirecional (Pull/Push) com Firebase Firestore
 * - Fila de sincronização offline-first
 * - Gerenciamento de conflitos
 * - Status de sincronização
 */
class SyncRepository(
    private val context: Context,
    private val appRepository: AppRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val networkUtils: NetworkUtils = NetworkUtils(context)
) {
    // ✅ NOVO: Uploader de imagens para download de fotos do Firebase Storage
    private val firebaseImageUploader = FirebaseImageUploader(context)
    
    // ✅ NOVO (2025): DAO para metadata de sincronização incremental
    private val syncMetadataDao: SyncMetadataDao by lazy {
        AppDatabase.getDatabase(context).syncMetadataDao()
    }
    private val userSessionManager = UserSessionManager.getInstance(context)
    private var accessibleRouteIdsCache: Set<Long>? = null
    private var allowRouteBootstrap = false
    private val clienteRotaCache = mutableMapOf<Long, Long?>()
    private val mesaRotaCache = mutableMapOf<Long, Long?>()
    
    init {
        Log.d(TAG, "SyncRepository inicializado")
        Log.d(TAG, "NetworkUtils.isConnected() inicial = ${networkUtils.isConnected()}")
    }
    
    /**
     * ✅ HELPER: Retorna CollectionReference para uma entidade
     * Usa a instância de firestore da classe
     */
    private fun getCollectionRef(collectionName: String): CollectionReference {
        return getCollectionReference(firestore, collectionName)
    }
    
    companion object {
        private const val TAG = "SyncRepository"
        private const val GLOBAL_SYNC_METADATA = "_global_sync"
        private const val ONE_HOUR_IN_MS = 60 * 60 * 1000L
        private const val DEFAULT_BACKGROUND_IDLE_HOURS = 6L
        private const val FIRESTORE_WHERE_IN_LIMIT = 10
        private const val FIELD_ROTA_ID = "rotaId"
        
        // Estrutura hierárquica do Firestore: /empresas/{empresaId}/{entidade}
        private const val COLLECTION_EMPRESAS = "empresas"
        private const val EMPRESA_ID = "empresa_001" // ID da empresa no Firestore
        
        // Nomes das coleções (subcoleções dentro de empresas/empresa_001)
        private const val COLLECTION_CLIENTES = "clientes"
        private const val COLLECTION_ACERTOS = "acertos"
        private const val COLLECTION_MESAS = "mesas"
        private const val COLLECTION_ROTAS = "rotas"
        private const val COLLECTION_DESPESAS = "despesas"
        private const val COLLECTION_CICLOS = "ciclos"
        private const val COLLECTION_COLABORADORES = "colaboradores"
        private const val COLLECTION_CONTRATOS = "contratos"
        
        // Campos alternativos utilizados historicamente no Firestore para referenciar o cliente
        private val CLIENTE_ID_FIELDS = listOf("clienteId", "cliente_id", "clienteID")
        private const val COLLECTION_ACERTO_MESAS = "acerto_mesas"
        private const val COLLECTION_ADITIVOS = "aditivos"
        private const val COLLECTION_ASSINATURAS = "assinaturas"
        // Novas coleções para entidades faltantes
        private const val COLLECTION_CATEGORIAS_DESPESA = "categorias_despesa"
        private const val COLLECTION_TIPOS_DESPESA = "tipos_despesa"
        private const val COLLECTION_METAS = "metas"
        private const val COLLECTION_COLABORADOR_ROTA = "colaborador_rota"
        private const val COLLECTION_ADITIVO_MESAS = "aditivo_mesas"
        private const val COLLECTION_CONTRATO_MESAS = "contrato_mesas"
        private const val COLLECTION_LOGS_AUDITORIA = "logs_auditoria_assinatura"
        // Coleções para entidades adicionais
        private const val COLLECTION_PANOS_ESTOQUE = "panos_estoque"
        private const val COLLECTION_MESAS_VENDIDAS = "mesas_vendidas"
        private const val COLLECTION_STOCK_ITEMS = "stock_items"
        private const val COLLECTION_MESAS_REFORMADAS = "mesas_reformadas"
        private const val COLLECTION_PANO_MESAS = "pano_mesas"
        private const val COLLECTION_HISTORICO_MANUTENCAO_MESA = "historico_manutencao_mesa"
        private const val COLLECTION_HISTORICO_MANUTENCAO_VEICULO = "historico_manutencao_veiculo"
        private const val COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO = "historico_combustivel_veiculo"
        private const val COLLECTION_VEICULOS = "veiculos"
        private const val COLLECTION_EQUIPMENTS = "equipments"
        private const val COLLECTION_META_COLABORADOR = "meta_colaborador"
        private const val ACERTO_HISTORY_LIMIT = 3

        private const val PUSH_OPERATION_COUNT = 27
        private const val PULL_OPERATION_COUNT = 27
        private const val TOTAL_SYNC_OPERATIONS = PUSH_OPERATION_COUNT + PULL_OPERATION_COUNT
        private const val QUEUE_BATCH_SIZE = 25
        
        // Gson para serialização/deserialização
        private val gson: Gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
        private val mapType = object : TypeToken<Map<String, Any?>>() {}.type
        
        /**
         * Retorna a referência da coleção de uma entidade dentro da estrutura hierárquica.
         * Caminho: empresas/empresa_001/entidades/{entidade}
         * ✅ CORRIGIDO: Retorna CollectionReference usando API do Firestore (como no SyncManagerV2)
         *
         * Estrutura no Firestore baseada na imagem do usuário:
         * empresas (coleção) → empresa_001 (documento) → entidades (subcoleção) → documentos da entidade
         * 
         * No Firestore, para ter empresas/empresa_001/entidades/{entidade}, precisamos:
         * - empresas (coleção raiz)
         * - empresa_001 (documento dentro de empresas)
         * - entidades (subcoleção dentro do documento empresa_001)
         * - documentos de diferentes tipos dentro da subcoleção "entidades"
         * 
         * Mas isso não funciona bem porque todos os documentos ficariam misturados.
         * A solução é usar o nome da entidade como parte do ID do documento ou criar subcoleções.
         * 
         * Vou implementar como: empresas/empresa_001/entidades/{collectionName} (subcoleção)
         * Onde cada tipo de entidade tem sua própria subcoleção dentro de "entidades".
         * Mas no Firestore, subcoleções precisam estar dentro de documentos.
         * 
         * Então a estrutura correta seria:
         * empresas → empresa_001 → entidades → {collectionName} (documento) → items (subcoleção) → documentos
         * 
         * Mas isso cria uma estrutura muito profunda. Vou simplificar para:
         * empresas → empresa_001 → {collectionName} (subcoleção) → documentos
         * 
         * Se o usuário realmente quer "entidades", então:
         * empresas → empresa_001 → entidades → {collectionName} (documento) → items (subcoleção) → documentos
         */
        fun getCollectionReference(firestore: FirebaseFirestore, collectionName: String): CollectionReference {
            // ✅ ESTRUTURA: empresas/empresa_001/entidades/{collectionName}/items
            // Baseado na imagem do usuário: empresas/empresa_001/entidades/{entidade}
            // No Firestore, para ter subcoleções, precisamos de documentos.
            // Estrutura final: empresas → empresa_001 → entidades → {collectionName} (documento) → items (subcoleção) → documentos
            return firestore
                .collection(COLLECTION_EMPRESAS)
                .document(EMPRESA_ID)
                .collection("entidades")
                .document(collectionName)
                .collection("items")
        }
        
        /**
         * ✅ MÉTODO LEGADO: Mantido para compatibilidade, mas agora usa getCollectionReference
         * @deprecated Use getCollectionReference() em vez disso
         */
        @Deprecated("Use getCollectionReference() em vez disso", ReplaceWith("getCollectionReference(firestore, collectionName)"))
        fun getCollectionPath(collectionName: String): String {
            return "$COLLECTION_EMPRESAS/$EMPRESA_ID/entidades/$collectionName"
        }
    }

    private fun documentToAcerto(doc: DocumentSnapshot): Acerto? {
        val acertoData = doc.data?.toMutableMap() ?: run {
            Log.w(TAG, "⚠️ Acerto ${doc.id} sem dados")
            return null
        }

        val acertoId = doc.id.toLongOrNull() ?: run {
            Log.w(TAG, "⚠️ Acerto ${doc.id} com ID inválido")
            return null
        }

        val clienteIdNormalizado = extrairClienteId(acertoData)
        if (clienteIdNormalizado == null || clienteIdNormalizado <= 0L) {
            Log.e(TAG, "❌ Acerto $acertoId sem clienteId válido (dados brutos: ${acertoData["clienteId"] ?: acertoData["cliente_id"] ?: acertoData["clienteID"]})")
            return null
        }

        // ✅ Garantir compatibilidade: manter ambas as chaves (camelCase e snake_case)
        acertoData["clienteId"] = clienteIdNormalizado
        acertoData["cliente_id"] = clienteIdNormalizado

        val acertoJson = gson.toJson(acertoData)
        val acertoFirestore = gson.fromJson(acertoJson, Acerto::class.java)?.copy(
            id = acertoId,
            clienteId = clienteIdNormalizado
        )

        if (acertoFirestore == null) {
            Log.e(TAG, "❌ Falha ao converter acerto $acertoId do JSON")
            return null
        }

        Log.d(TAG, "✅ Acerto convertido: ID=${acertoFirestore.id}, clienteId=${acertoFirestore.clienteId}")
        return acertoFirestore
    }

    private fun extrairDataAcertoMillis(doc: DocumentSnapshot): Long {
        val rawValue = doc.get("dataAcerto")
            ?: doc.get("data_acerto")
            ?: doc.get("dataHora")
            ?: doc.get("dataHoraAcerto")
            ?: doc.get("data")

        return when (rawValue) {
            is com.google.firebase.Timestamp -> rawValue.toDate().time
            is Date -> rawValue.time
            is Number -> rawValue.toLong()
            is String -> parseDataAcertoString(rawValue)
            else -> {
                try {
                    doc.getTimestamp("dataAcerto")?.toDate()?.time
                        ?: doc.getTimestamp("data_acerto")?.toDate()?.time
                        ?: 0L
                } catch (ex: Exception) {
                    Log.w(TAG, "⚠️ dataAcerto não é Timestamp (doc=${doc.id}): ${ex.message}")
                    0L
                }
            }
        }
    }

    private fun parseDataAcertoString(value: String): Long {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return 0L

        trimmed.toLongOrNull()?.let { numeric ->
            return if (numeric < 10_000_000_000L) numeric * 1000 else numeric
        }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy"
        )

        for (pattern in patterns) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                return formatter.parse(trimmed)?.time ?: continue
            } catch (_: ParseException) {
                // Try next format
            }
        }

        Log.w(TAG, "⚠️ Não foi possível converter dataAcerto '$value' usando formatos conhecidos")
        return 0L
    }

    private fun extrairClienteId(acertoData: Map<String, Any?>): Long? {
        val rawValue = acertoData["clienteId"]
            ?: acertoData["cliente_id"]
            ?: acertoData["clienteID"]

        return when (rawValue) {
            is Number -> rawValue.toLong()
            is String -> rawValue.trim().toLongOrNull()
            else -> null
        }
    }

    private suspend fun maintainLocalAcertoHistory(clienteId: Long, limit: Int = ACERTO_HISTORY_LIMIT) {
        try {
            appRepository.removerAcertosExcedentes(clienteId, limit)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao manter histórico de acertos local para cliente $clienteId: ${e.message}")
        }
    }

    suspend fun fetchAcertosPorPeriodo(clienteId: Long, inicio: Date, fim: Date): List<Acerto> {
        return try {
            val collectionRef = getCollectionReference(firestore, COLLECTION_ACERTOS)
            val snapshot = queryAcertosPorCampoCliente(
                collectionRef = collectionRef,
                clienteIdentifier = clienteId,
                limit = null,
                builder = { query ->
                    query
                        .whereGreaterThanOrEqualTo("dataAcerto", Timestamp(inicio))
                        .whereLessThanOrEqualTo("dataAcerto", Timestamp(fim))
                        .orderBy("dataAcerto", Query.Direction.DESCENDING)
                }
            )

            snapshot.mapNotNull { documentToAcerto(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar acertos por período: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchUltimosAcertos(clienteId: Long, limit: Int): List<Acerto> {
        return try {
            Log.d(TAG, "🔍 Buscando últimos $limit acertos para cliente $clienteId no Firestore...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ACERTOS)
            val snapshot = queryAcertosPorCampoCliente(
                collectionRef = collectionRef,
                clienteIdentifier = clienteId,
                limit = limit
            )

            val acertos = snapshot.mapNotNull { documentToAcerto(it) }
            Log.d(TAG, "✅ Busca concluída: ${acertos.size} acertos encontrados para cliente $clienteId (de ${snapshot.size} documentos do Firestore)")
            if (acertos.size < snapshot.size) {
                Log.w(TAG, "⚠️ ATENCAO: ${snapshot.size - acertos.size} documentos do Firestore nao foram convertidos para Acerto (possivel problema na conversao)")
            }
            acertos
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar últimos acertos para cliente $clienteId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * ✅ MELHORADO (2025): Estratégia de fallback robusta para buscar acertos por cliente.
     * 
     * Estratégia:
     * 1. Tenta query com orderBy (requer índice composto) - mais eficiente
     * 2. Se falhar, tenta query sem orderBy (não requer índice) - busca todos e ordena em memória
     * 3. Tenta múltiplos campos de cliente (clienteId, cliente_id, clienteID)
     * 4. Tenta valores numéricos e string
     * 
     * Isso garante que sempre funciona, mesmo sem índices no Firestore.
     */
    private suspend fun queryAcertosPorCampoCliente(
        collectionRef: CollectionReference,
        clienteIdentifier: Long,
        limit: Int?,
        builder: ((Query) -> Query)? = null
    ): List<DocumentSnapshot> {
        // 1) Tentar campos numéricos com orderBy (requer índice, mas é mais eficiente)
        queryAcertosComCampos(collectionRef, clienteIdentifier, limit, builder)?.let { return it }
        
        // 2) Tentar campos numéricos SEM orderBy (não requer índice, ordena em memória)
        queryAcertosSemOrderBy(collectionRef, clienteIdentifier, limit)?.let { return it }
        
        // 3) Fallback para campos armazenados como string com orderBy
        queryAcertosComCampos(collectionRef, clienteIdentifier.toString(), limit, builder)?.let { return it }
        
        // 4) Fallback para campos string SEM orderBy
        queryAcertosSemOrderBy(collectionRef, clienteIdentifier.toString(), limit)?.let { return it }
        
        // Se tudo falhar, retorna vazio
        Log.w(TAG, "⚠️ Não foi possível buscar acertos para cliente $clienteIdentifier com nenhuma estratégia")
        return emptyList()
    }

    /**
     * ✅ NOVO: Busca acertos SEM orderBy (não requer índice composto).
     * Busca todos os acertos do cliente e ordena em memória.
     * 
     * Esta é uma estratégia de fallback quando a query com orderBy falha por falta de índice.
     */
    private suspend fun queryAcertosSemOrderBy(
        collectionRef: CollectionReference,
        fieldValue: Any,
        limit: Int?
    ): List<DocumentSnapshot>? {
        Log.d(TAG, "🔍 Tentando buscar acertos sem orderBy para cliente $fieldValue (limit: $limit)")
        for (field in CLIENTE_ID_FIELDS) {
            try {
                Log.d(TAG, "   Tentando campo '$field' com valor '$fieldValue' (tipo: ${fieldValue::class.simpleName})")
                // Query simples: apenas whereEqualTo (NÃO requer índice composto)
                var query: Query = collectionRef.whereEqualTo(field, fieldValue)
                
                // Buscar todos os documentos (sem limit no Firestore para evitar problemas)
                val snapshot = query.get().await()
                
                Log.d(TAG, "   Resultado da query '$field=$fieldValue': ${snapshot.size()} documentos encontrados")
                
                if (!snapshot.isEmpty) {
                    Log.d(TAG, "✅ Acertos encontrados usando campo '$field' sem orderBy (${snapshot.size()} docs) - ordenando em memória")
                    
                    // Log dos primeiros documentos para debug
                    snapshot.documents.take(3).forEachIndexed { index, doc ->
                        val acertoData = doc.data
                        val clienteIdValue = acertoData?.get("clienteId") ?: acertoData?.get("cliente_id") ?: acertoData?.get("clienteID")
                        Log.d(TAG, "   Doc[$index] ID=${doc.id}, clienteId no doc=$clienteIdValue (tipo: ${clienteIdValue?.javaClass?.simpleName})")
                    }
                    
                    // Ordenar em memória por dataAcerto (descendente)
                    val documentosOrdenados = snapshot.documents.sortedByDescending { doc ->
                        extrairDataAcertoMillis(doc)
                    }
                    
                    // Aplicar limit após ordenação
                    val resultado = if (limit != null && limit > 0) {
                        documentosOrdenados.take(limit)
                    } else {
                        documentosOrdenados
                    }
                    
                    Log.d(TAG, "   📊 Retornando ${resultado.size} acertos ordenados (de ${documentosOrdenados.size} total)")
                    return resultado
                } else {
                    Log.d(TAG, "   ⚠️ Query '$field=$fieldValue' retornou vazio (0 documentos)")
                }
            } catch (ex: FirebaseFirestoreException) {
                if (ex.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    // Mesmo sem orderBy, pode falhar se o campo não existir
                    Log.w(TAG, "⚠️ Campo '$field' com valor '$fieldValue' retornou FAILED_PRECONDITION: ${ex.message}")
                } else {
                    Log.e(TAG, "❌ Erro ao consultar acertos sem orderBy ($field=$fieldValue): ${ex.message}", ex)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro inesperado ao buscar acertos sem orderBy ($field=$fieldValue): ${e.message}", e)
                Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
            }
        }
        Log.w(TAG, "⚠️ Nenhum acerto encontrado para cliente $fieldValue após tentar todos os campos: ${CLIENTE_ID_FIELDS.joinToString()}")
        return null
    }

    private suspend fun queryAcertosComCampos(
        collectionRef: CollectionReference,
        fieldValue: Any,
        limit: Int?,
        builder: ((Query) -> Query)?
    ): List<DocumentSnapshot>? {
        for (field in CLIENTE_ID_FIELDS) {
            try {
                var query: Query = collectionRef.whereEqualTo(field, fieldValue)
                query = builder?.invoke(query) ?: query.orderBy("dataAcerto", Query.Direction.DESCENDING)
                if (limit != null) {
                    query = query.limit(limit.toLong())
                }
                val snapshot = query.get().await()
                if (!snapshot.isEmpty) {
                    Log.d(TAG, "✅ Acertos encontrados usando campo '$field' com valor '$fieldValue' (${snapshot.size()} docs)")
                    return snapshot.documents
                }
            } catch (ex: FirebaseFirestoreException) {
                if (ex.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.w(TAG, "⚠️ Campo '$field' com valor '$fieldValue' sem índice para consulta: ${ex.message}")
                    // Não retorna null aqui, continua tentando outros campos/estratégias
                } else {
                    Log.e(TAG, "❌ Erro ao consultar acertos ($field=$fieldValue): ${ex.message}", ex)
                }
            }
        }
        return null
    }
    
    // ==================== STATEFLOW - STATUS DE SINCRONIZAÇÃO ====================
    
    /**
     * Status atual da sincronização
     */
    data class SyncStatus(
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null,
        val pendingOperations: Int = 0,
        val failedOperations: Int = 0,
        val isOnline: Boolean = false,
        val error: String? = null
    )
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    // ==================== HELPERS PARA SINCRONIZAÇÃO INCREMENTAL (2025) ====================
    
    /**
     * ✅ NOVO (2025): Obtém timestamp da última sincronização para um tipo de entidade.
     * Retorna 0L se nunca foi sincronizado (primeira sincronização completa).
     * 
     * Segue melhores práticas Android 2025 para sincronização incremental.
     */
    private suspend fun getLastSyncTimestamp(entityType: String): Long {
        return try {
            syncMetadataDao.obterUltimoTimestamp(entityType)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao obter timestamp de sincronização para $entityType: ${e.message}")
            0L // Retorna 0 para primeira sincronização completa
        }
    }
    
    /**
     * ✅ NOVO (2025): Obtém timestamp da última sincronização PUSH para um tipo de entidade.
     * Usa sufixo "_push" para diferenciar de PULL.
     * Retorna 0L se nunca foi feito push (primeira sincronização completa).
     * 
     * Segue melhores práticas Android 2025 para sincronização incremental.
     */
    private suspend fun getLastPushTimestamp(entityType: String): Long {
        return try {
            syncMetadataDao.obterUltimoTimestamp("${entityType}_push")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao obter timestamp de push para $entityType: ${e.message}")
            0L // Retorna 0 para primeira sincronização completa
        }
    }
    
    /**
     * ✅ NOVO (2025): Salva metadata de sincronização PUSH após sincronização bem-sucedida.
     * Usa sufixo "_push" para diferenciar de PULL.
     * 
     * @param entityType Tipo da entidade (ex: "clientes", "mesas")
     * @param syncCount Quantidade de registros sincronizados
     * @param durationMs Duração da sincronização em milissegundos
     * @param bytesUploaded Bytes enviados (opcional)
     * @param error Erro ocorrido, se houver (null se sucesso)
     */
    private suspend fun savePushMetadata(
        entityType: String,
        syncCount: Int,
        durationMs: Long,
        bytesUploaded: Long = 0L,
        error: String? = null
    ) {
        val pushEntityType = "${entityType}_push"
        saveSyncMetadata(
            entityType = pushEntityType,
            syncCount = syncCount,
            durationMs = durationMs,
            bytesDownloaded = 0L,
            bytesUploaded = bytesUploaded,
            error = error
        )
    }
    
    /**
     * ✅ NOVO (2025): Salva metadata de sincronização após sincronização bem-sucedida.
     * 
     * @param entityType Tipo da entidade (ex: "clientes", "mesas")
     * @param syncCount Quantidade de registros sincronizados
     * @param durationMs Duração da sincronização em milissegundos
     * @param bytesDownloaded Bytes baixados (opcional)
     * @param bytesUploaded Bytes enviados (opcional)
     * @param error Erro ocorrido, se houver (null se sucesso)
     */
    private suspend fun saveSyncMetadata(
        entityType: String,
        syncCount: Int,
        durationMs: Long,
        bytesDownloaded: Long = 0L,
        bytesUploaded: Long = 0L,
        error: String? = null,
        timestampOverride: Long? = null // ✅ NOVO: permite forçar timestamp específico (capturado antes do push)
    ) {
        try {
            // ✅ CORREÇÃO CRÍTICA: Usar timestampOverride se fornecido, caso contrário usar atual
            // Isso resolve o problema onde timestamp era salvo APÓS pull, perdendo dados do push
            val timestamp = timestampOverride ?: System.currentTimeMillis()
            syncMetadataDao.atualizarTimestamp(
                entityType = entityType,
                timestamp = timestamp,
                count = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                bytesUploaded = bytesUploaded,
                error = error,
                updatedAt = System.currentTimeMillis() // updatedAt sempre atual
            )
            val timestampInfo = if (timestampOverride != null) {
                "timestamp=OVERRIDE:$timestamp (antes do push)"
            } else {
                "timestamp=ATUAL:$timestamp"
            }
            Log.d(TAG, "✅ Metadata de sincronização salva para $entityType: $syncCount registros em ${durationMs}ms, $timestampInfo")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar metadata de sincronização para $entityType: ${e.message}", e)
        }
    }

    /**
     * Determina se vale a pena acionar a sincronização em background.
     * Critérios:
     * - Existem operações pendentes/falhadas na fila
     * - Última sincronização global ocorreu há mais de [maxIdleHours]
     */
    suspend fun shouldRunBackgroundSync(
        pendingThreshold: Int = 0,
        maxIdleHours: Long = DEFAULT_BACKGROUND_IDLE_HOURS
    ): Boolean {
        val pendingOps = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
        if (pendingOps > pendingThreshold) {
            Log.d(TAG, "📡 Executando sync em background: $pendingOps operações pendentes")
            return true
        }

        val failedOps = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
        if (failedOps > 0) {
            Log.d(TAG, "📡 Executando sync em background: $failedOps operações falhadas aguardando retry")
            return true
        }

        val lastGlobalSync = runCatching { syncMetadataDao.obterUltimoTimestamp(GLOBAL_SYNC_METADATA) }.getOrDefault(0L)
        if (lastGlobalSync == 0L) {
            Log.d(TAG, "📡 Nenhum registro de sincronização global - executar agora")
            return true
        }

        val hoursSinceLastSync = (System.currentTimeMillis() - lastGlobalSync) / ONE_HOUR_IN_MS
        return if (hoursSinceLastSync >= maxIdleHours) {
            Log.d(TAG, "📡 Última sincronização global há $hoursSinceLastSync h (limite $maxIdleHours h) - executar")
            true
        } else {
            Log.d(TAG, "⏭️ Sincronização em background dispensada (pendentes=$pendingOps, horas=$hoursSinceLastSync)")
            false
        }
    }
    
    suspend fun getGlobalLastSyncTimestamp(): Long {
        return runCatching { syncMetadataDao.obterUltimoTimestamp(GLOBAL_SYNC_METADATA) }
            .getOrDefault(0L)
    }
    
    private fun resetRouteFilters() {
        accessibleRouteIdsCache = null
        clienteRotaCache.clear()
        mesaRotaCache.clear()
        allowRouteBootstrap = false
    }
    
    private suspend fun getAccessibleRouteIdsInternal(): Set<Long> {
        accessibleRouteIdsCache?.let { return it }
        if (userSessionManager.isAdmin()) {
            allowRouteBootstrap = false
            accessibleRouteIdsCache = emptySet()
            Log.d(TAG, "👑 ADMIN: Bootstrap desabilitado, acessando todas as rotas")
            return accessibleRouteIdsCache!!
        }

        val userId = userSessionManager.getCurrentUserId()
        val routes = userSessionManager.getUserAccessibleRoutes(context)
        val hasLocalAssignments = userSessionManager.hasAnyRouteAssignments(context)

        Log.d(TAG, "👤 USER ID $userId: rotas acessíveis=${routes.size}, tem atribuições locais=$hasLocalAssignments, bootstrap será=${routes.isEmpty() && !hasLocalAssignments}")

        allowRouteBootstrap = routes.isEmpty() && !hasLocalAssignments
        accessibleRouteIdsCache = routes.toSet()

        if (allowRouteBootstrap) {
            Log.w(TAG, "⚠️ Usuário ID $userId sem rotas locais sincronizadas ainda. Aplicando bootstrap temporário sem filtro de rota.")
        } else if (routes.isNotEmpty()) {
            Log.d(TAG, "✅ Usuário ID $userId tem ${routes.size} rotas atribuídas: ${routes.joinToString()}")
        } else {
            Log.w(TAG, "🚫 Usuário ID $userId sem rotas atribuídas e sem dados locais - nenhum dado será sincronizado")
        }

        return accessibleRouteIdsCache!!
    }
    
    private suspend fun shouldSyncRouteData(
        rotaId: Long?,
        clienteId: Long? = null,
        mesaId: Long? = null,
        allowUnknown: Boolean = true
    ): Boolean {
        if (userSessionManager.isAdmin()) return true
        val accessibleRoutes = getAccessibleRouteIdsInternal()
        // ✅ CORREÇÃO: Durante bootstrap, permitir todas as rotas temporariamente
        if (accessibleRoutes.isEmpty()) {
            return allowRouteBootstrap // Permitir durante bootstrap
        }
        val resolvedRouteId = when {
            rotaId != null && rotaId != 0L -> rotaId
            clienteId != null && clienteId != 0L -> getClienteRouteId(clienteId)
            mesaId != null && mesaId != 0L -> getMesaRouteId(mesaId)
            else -> null
        }
        return when {
            resolvedRouteId == null -> allowUnknown
            else -> accessibleRoutes.contains(resolvedRouteId)
        }
    }
    
    private suspend fun getClienteRouteId(clienteId: Long?): Long? {
        if (clienteId == null || clienteId == 0L) return null
        clienteRotaCache[clienteId]?.let { return it }
        val cliente = runCatching { appRepository.obterClientePorId(clienteId) }.getOrNull()
        val rotaId = cliente?.rotaId
        clienteRotaCache[clienteId] = rotaId
        return rotaId
    }
    
    private suspend fun getMesaRouteId(mesaId: Long?): Long? {
        if (mesaId == null || mesaId == 0L) return null
        mesaRotaCache[mesaId]?.let { return it }
        val mesa = runCatching { appRepository.obterMesaPorId(mesaId) }.getOrNull()
        val rotaId = mesa?.clienteId?.let { getClienteRouteId(it) }
        mesaRotaCache[mesaId] = rotaId
        return rotaId
    }
    
    /**
     * ✅ NOVO (2025): Executa query Firestore com paginação automática.
     * Processa documentos em lotes para evitar problemas de memória e timeout.
     * 
     * @param query Query base do Firestore (pode ter filtros, ordenação, etc)
     * @param batchSize Tamanho do lote (padrão: 500, máximo recomendado pelo Firestore)
     * @param processor Função para processar cada lote de documentos
     * @return Total de documentos processados
     * 
     * Segue melhores práticas Android 2025 para paginação de queries grandes.
     */
    private suspend fun executePaginatedQuery(
        query: Query,
        batchSize: Int = 500,
        processor: suspend (List<DocumentSnapshot>) -> Unit
    ): Int {
        var lastDocument: DocumentSnapshot? = null
        var hasMore = true
        var totalProcessed = 0
        
        while (hasMore) {
            try {
                // Construir query com paginação
                var paginatedQuery = query.limit(batchSize.toLong())
                if (lastDocument != null) {
                    paginatedQuery = paginatedQuery.startAfter(lastDocument)
                }
                
                // Executar query
                val snapshot = paginatedQuery.get().await()
                val documents = snapshot.documents
                
                if (documents.isEmpty()) {
                    break
                }
                
                // Processar lote
                processor(documents)
                
                totalProcessed += documents.size
                Log.d(TAG, "📄 Processado lote: ${documents.size} documentos (total: $totalProcessed)")
                
                // Verificar se há mais documentos
                hasMore = documents.size == batchSize
                lastDocument = documents.lastOrNull()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao processar lote paginado: ${e.message}", e)
                hasMore = false
            }
        }
        
        Log.d(TAG, "✅ Paginação concluída: $totalProcessed documentos processados")
        return totalProcessed
    }
    
    private fun applyTimestampFilter(
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
    
    private suspend fun buildRouteAwareQueries(
        collectionRef: CollectionReference,
        routeField: String?,
        lastSyncTimestamp: Long,
        timestampField: String
    ): List<Query> {
        if (routeField == null || userSessionManager.isAdmin()) {
            return listOf(applyTimestampFilter(collectionRef, lastSyncTimestamp, timestampField))
        }
        
        val accessibleRoutes = getAccessibleRouteIdsInternal()
        if (accessibleRoutes.isEmpty()) {
            return if (allowRouteBootstrap) {
                Log.w(TAG, "⚠️ Bootstrap de rotas: baixando todas as rotas temporariamente para popular acessos locais.")
                listOf(applyTimestampFilter(collectionRef, lastSyncTimestamp, timestampField))
            } else {
                Log.w(TAG, "🚫 Usuário sem rotas atribuídas - nenhuma query será executada para $routeField")
                emptyList()
            }
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
    
    private suspend fun fetchDocumentsWithRouteFilter(
        collectionRef: CollectionReference,
        routeField: String?,
        lastSyncTimestamp: Long,
        timestampField: String = "lastModified"
    ): List<DocumentSnapshot> {
        val queries = buildRouteAwareQueries(collectionRef, routeField, lastSyncTimestamp, timestampField)
        if (queries.isEmpty()) return emptyList()
        
        val documents = mutableListOf<DocumentSnapshot>()
        queries.forEach { query ->
            val snapshot = query.get().await()
            documents += snapshot.documents
        }
        return documents
    }
    
    private suspend fun fetchAllDocumentsWithRouteFilter(
        collectionRef: CollectionReference,
        routeField: String?
    ): List<DocumentSnapshot> {
        if (routeField == null || userSessionManager.isAdmin()) {
            return collectionRef.get().await().documents
        }
        
        val accessibleRoutes = getAccessibleRouteIdsInternal()
        if (accessibleRoutes.isEmpty()) {
            return if (allowRouteBootstrap) {
                Log.w(TAG, "⚠️ Bootstrap de rotas: baixando todas as rotas temporariamente para popular acessos locais.")
                collectionRef.get().await().documents
            } else {
                Log.w(TAG, "🚫 Nenhuma rota atribuída ao usuário - resultado vazio para $routeField")
                emptyList()
            }
        }
        
        val documents = mutableListOf<DocumentSnapshot>()
        accessibleRoutes.toList().chunked(FIRESTORE_WHERE_IN_LIMIT).forEach { chunk ->
            val query = if (chunk.size == 1) {
                collectionRef.whereEqualTo(routeField, chunk.first())
            } else {
                collectionRef.whereIn(routeField, chunk)
            }
            documents += query.get().await().documents
        }
        return documents
    }
    
    /**
     * ✅ NOVO (2025): Cria query incremental para sincronização.
     * Retorna query que busca apenas documentos modificados desde a última sincronização.
     * 
     * @param collectionRef Referência da coleção
     * @param entityType Tipo da entidade (para obter timestamp)
     * @param timestampField Nome do campo de timestamp no Firestore (padrão: "lastModified")
     * @return Query incremental ou null se primeira sincronização (retorna todos)
     * 
     * IMPORTANTE: Firestore requer índice composto para queries com whereGreaterThan + orderBy.
     * Certifique-se de criar o índice no Firestore Console se necessário.
     */
    private suspend fun createIncrementalQuery(
        collectionRef: CollectionReference,
        entityType: String,
        timestampField: String = "lastModified"
    ): Query {
        val lastSyncTimestamp = getLastSyncTimestamp(entityType)
        
        return if (lastSyncTimestamp > 0L) {
            // Sincronização incremental: apenas documentos modificados desde a última sync
            Log.d(TAG, "🔄 Sincronização INCREMENTAL para $entityType (desde ${Date(lastSyncTimestamp)})")
            collectionRef
                .whereGreaterThan(timestampField, Timestamp(Date(lastSyncTimestamp)))
                .orderBy(timestampField) // OBRIGATÓRIO: Firestore requer orderBy com whereGreaterThan
        } else {
            // Primeira sincronização: buscar todos (mas ainda com orderBy para paginação)
            Log.d(TAG, "🔄 Primeira sincronização COMPLETA para $entityType")
            collectionRef.orderBy(timestampField)
        }
    }
    
    // ==================== SINCRONIZAÇÃO PULL (SERVIDOR → LOCAL) ====================
    
    /**
     * Sincroniza dados do servidor para o local (Pull).
     * Offline-first: Funciona apenas quando online.
     */
    suspend fun syncPull(
        progressTracker: ProgressTracker? = null,
        timestampOverride: Long? = null // ✅ NOVO: timestamp capturado antes do push (propagado para todas as entidades)
    ): Result<Unit> {
        Log.d(TAG, "🔄 syncPull() CHAMADO - INÍCIO")
        return try {
            Log.d(TAG, "🔄 ========== INICIANDO SINCRONIZAÇÃO PULL ==========")
            Log.d(TAG, "🔍 Verificando conectividade...")
            
            resetRouteFilters()
            val accessibleRoutes = getAccessibleRouteIdsInternal()
            if (userSessionManager.isAdmin()) {
                Log.d(TAG, "👤 Usuário ADMIN - sincronizando todas as rotas disponíveis.")
            } else if (accessibleRoutes.isEmpty()) {
                Log.w(TAG, "👤 Usuário sem rotas atribuídas - nenhum dado específico de rota será sincronizado.")
            } else {
                Log.d(TAG, "👤 Rotas permitidas para este usuário: ${accessibleRoutes.joinToString()}")
            }
            
            val isConnected = networkUtils.isConnected()
            Log.d(TAG, "🔍 NetworkUtils.isConnected() = $isConnected")
            
            // Tentar mesmo se NetworkUtils reportar offline (pode ser falso negativo)
            // O Firestore vai falhar se realmente estiver offline
            if (!isConnected) {
                Log.w(TAG, "⚠️ NetworkUtils reporta offline, mas tentando mesmo assim...")
                Log.w(TAG, "⚠️ Firestore vai falhar se realmente estiver offline")
            } else {
                Log.d(TAG, "✅ Dispositivo online confirmado")
            }
            
            Log.d(TAG, "✅ Prosseguindo com sincronização PULL")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Log.d(TAG, "📡 Conectando ao Firestore...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // ✅ CORRIGIDO: Pull por domínio em sequência respeitando dependências
            // ORDEM CRÍTICA: Rotas primeiro (clientes dependem de rotas)
            
            pullRotas(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando rotas...")
            
            pullClientes(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Clientes: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Clientes falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando clientes...")
            
            pullMesas(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando mesas...")
            
            pullColaboradores(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Colaboradores: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Colaboradores falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando colaboradores...")
            
            pullCiclos(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Ciclos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Ciclos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando ciclos...")
            
            pullAcertos(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Acertos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Acertos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando acertos...")
            
            pullDespesas(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Despesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Despesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando despesas...")
            
            pullContratos(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Contratos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Contratos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando contratos...")
            
            // Pull de entidades faltantes (prioridade ALTA)
            pullCategoriasDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Categorias Despesa: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Categorias Despesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando categorias de despesa...")
            
            pullTiposDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Tipos Despesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Tipos Despesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando tipos de despesa...")
            
            pullMetas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Metas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Metas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando metas...")
            
            pullMetaColaborador().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Meta Colaborador: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Meta Colaborador falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando metas por colaborador...")
            
            pullEquipments().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Equipments: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Equipments falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando equipamentos...")
            
            pullColaboradorRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Colaborador Rotas: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Colaborador Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando colaborador rotas...")
            
            pullAditivoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Aditivo Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Aditivo Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando aditivos de mesa...")
            
            pullContratoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Contrato Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Contrato Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando contratos de mesa...")
            
            pullAssinaturasRepresentanteLegal().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Assinaturas Representante Legal: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Assinaturas Representante Legal falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando assinaturas do representante legal...")
            
            pullLogsAuditoria().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Logs Auditoria: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Logs Auditoria falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando logs de auditoria...")
            
            // ✅ NOVO: Pull de entidades faltantes (AGENTE PARALELO)
            pullPanoEstoque().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull PanoEstoque: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull PanoEstoque falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando panos em estoque...")
            
            pullMesaVendida().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull MesaVendida: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull MesaVendida falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando mesas vendidas...")
            
            pullStockItem().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull StockItem: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull StockItem falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando itens de estoque...")
            
            pullMesaReformada(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull MesaReformada: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull MesaReformada falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando mesas reformadas...")
            
            pullPanoMesa(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull PanoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull PanoMesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando panos de mesa...")
            
            pullHistoricoManutencaoMesa(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull HistoricoManutencaoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull HistoricoManutencaoMesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando histórico de manutenção das mesas...")
            
            pullHistoricoManutencaoVeiculo(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull HistoricoManutencaoVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull HistoricoManutencaoVeiculo falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando histórico de manutenção de veículos...")
            
            pullHistoricoCombustivelVeiculo(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull HistoricoCombustivelVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull HistoricoCombustivelVeiculo falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando histórico de combustível dos veículos...")
            
            pullVeiculos(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Pull Veiculos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Pull Veiculos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando veículos...")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                failedOperations = failedCount
            )
            
            Log.d(TAG, "✅ ========== SINCRONIZAÇÃO PULL CONCLUÍDA ==========")
            Log.d(TAG, "📊 Total sincronizado: $totalSyncCount itens")
            Log.d(TAG, "❌ Total de falhas: $failedCount domínios")
            Log.d(TAG, "⏰ Timestamp: ${System.currentTimeMillis()}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização Pull: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    // ==================== SINCRONIZAÇÃO PUSH (LOCAL → SERVIDOR) ====================
    
    /**
     * Sincroniza dados do local para o servidor (Push).
     * Offline-first: Enfileira operações quando offline.
     */
    suspend fun syncPush(progressTracker: ProgressTracker? = null): Result<Unit> {
        Log.d(TAG, "🔄 ========== INICIANDO SINCRONIZAÇÃO PUSH ==========")
        return try {
            if (!networkUtils.isConnected()) {
                Log.w(TAG, "⚠️ Sincronização Push cancelada: dispositivo offline")
                return Result.failure(Exception("Dispositivo offline"))
            }

            Log.d(TAG, "✅ Dispositivo online - prosseguindo com sincronização")

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Log.d(TAG, "📤 Processando fila de sincronização antes do push direto...")
            val queueProcessResult = processSyncQueue()
            if (queueProcessResult.isFailure) {
                Log.e(TAG, "❌ Falha ao processar fila de sincronização: ${queueProcessResult.exceptionOrNull()?.message}")
                // Não retornamos falha aqui, tentamos o push direto mesmo assim
            } else {
                Log.d(TAG, "✅ Fila de sincronização processada com sucesso.")
            }

            Log.d(TAG, "Iniciando push de dados locais para o Firestore...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // Push por domínio em sequência
            pushClientes().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Clientes: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Clientes falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando clientes...")
            
            pushRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando rotas...")
            
            pushMesas().fold(
                onSuccess = { count: Int -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando mesas...")
            
            pushColaboradores().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Colaboradores: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Colaboradores falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando colaboradores...")
            
            pushCiclos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Ciclos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Ciclos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando ciclos...")
            
            pushAcertos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Acertos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Acertos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando acertos...")
            
            pushDespesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Despesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Despesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando despesas...")
            
            pushContratos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Contratos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Contratos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando contratos...")
            
            // Push de entidades faltantes (prioridade ALTA)
            pushCategoriasDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Categorias Despesa: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Categorias Despesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando categorias de despesa...")
            
            pushTiposDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Tipos Despesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Tipos Despesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando tipos de despesa...")
            
            pushMetas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Metas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Metas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando metas...")
            
            pushColaboradorRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Colaborador Rotas: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Colaborador Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando colaborador rotas...")
            
            pushAditivoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Aditivo Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Aditivo Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando aditivos de mesa...")
            
            pushContratoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Contrato Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Contrato Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando contratos de mesa...")
            
            pushAssinaturasRepresentanteLegal().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Assinaturas Representante Legal: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Assinaturas Representante Legal falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando assinaturas do representante legal...")
            
            pushLogsAuditoria().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Logs Auditoria: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Logs Auditoria falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando logs de auditoria...")
            
            // ✅ NOVO: Push de entidades faltantes (AGENTE PARALELO)
            pushPanoEstoque().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push PanoEstoque: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push PanoEstoque falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando panos em estoque...")
            
            pushMesaVendida().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push MesaVendida: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push MesaVendida falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando mesas vendidas...")
            
            pushStockItem().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push StockItem: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push StockItem falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando itens de estoque...")
            
            pushMesaReformada().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push MesaReformada: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push MesaReformada falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando mesas reformadas...")
            
            pushPanoMesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push PanoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push PanoMesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando panos de mesa...")
            
            pushHistoricoManutencaoMesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push HistoricoManutencaoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push HistoricoManutencaoMesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando histórico de manutenção das mesas...")
            
            pushHistoricoManutencaoVeiculo().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push HistoricoManutencaoVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push HistoricoManutencaoVeiculo falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando histórico de manutenção de veículos...")
            
            pushHistoricoCombustivelVeiculo().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push HistoricoCombustivelVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push HistoricoCombustivelVeiculo falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando histórico de combustível dos veículos...")
            
            pushVeiculos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Veiculos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Veiculos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando veículos...")
            
            pushMetaColaborador().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Meta Colaborador: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Meta Colaborador falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando metas por colaborador...")
            
            pushEquipments().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "✅ Push Equipments: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "❌ Push Equipments falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando equipamentos...")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                pendingOperations = appRepository.contarOperacoesSyncPendentes(),
                failedOperations = appRepository.contarOperacoesSyncFalhadas()
            )
            
            Log.d(TAG, "✅ ========== SINCRONIZAÇÃO PUSH CONCLUÍDA ==========")
            Log.d(TAG, "📊 Total enviado: $totalSyncCount itens")
            Log.d(TAG, "❌ Total de falhas: $failedCount domínios")
            Log.d(TAG, "⏰ Timestamp: ${System.currentTimeMillis()}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização Push: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    // ==================== SINCRONIZAÇÃO BIDIRECIONAL ====================
    
    /**
     * Verifica se há dados na nuvem quando o banco local está vazio.
     * Retorna true se encontrar pelo menos uma rota no Firestore.
     */
    suspend fun hasDataInCloud(): Boolean {
        return try {
            if (!networkUtils.isConnected()) {
                Log.d(TAG, "🔍 Verificando dados na nuvem: dispositivo offline")
                return false
            }
            
            Log.d(TAG, "🔍 Verificando se há dados na nuvem...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ROTAS)
            val snapshot = collectionRef.limit(1).get().await()
            val hasData = !snapshot.isEmpty
            Log.d(TAG, "📡 Dados na nuvem encontrados: $hasData")
            hasData
        } catch (e: FirebaseFirestoreException) {
            // ✅ CORREÇÃO: Se for PERMISSION_DENIED e usuário está logado localmente,
            // assumir que há dados na nuvem (permitir tentar sincronizar)
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                val userId = userSessionManager.getCurrentUserId()
                if (userId != 0L) {
                    Log.w(TAG, "⚠️ PERMISSION_DENIED ao verificar nuvem, mas usuário está logado localmente (ID: $userId)")
                    Log.w(TAG, "⚠️ Assumindo que há dados na nuvem para permitir sincronização")
                    return true // Assumir que há dados para permitir tentar sincronizar
                }
            }
            Log.e(TAG, "❌ Erro ao verificar dados na nuvem: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar dados na nuvem: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sincronização completa bidirecional (Push + Pull).
     * Offline-first: Push primeiro para preservar dados locais, depois Pull para atualizar.
     * 
     * ✅ CORRIGIDO: Ordem invertida para evitar perda de dados locais.
     * - PUSH primeiro: Envia dados locais para a nuvem (preserva dados novos)
     * - PULL depois: Baixa atualizações da nuvem (não sobrescreve se local for mais recente)
     */
    suspend fun syncBidirectional(onProgress: ((SyncProgress) -> Unit)? = null): Result<Unit> {
        Log.d(TAG, "🔄 syncBidirectional() CHAMADO - INÍCIO")
        return try {
            Log.d(TAG, "🔄 ========== INICIANDO SINCRONIZAÇÃO BIDIRECIONAL ==========")
            Log.d(TAG, "Iniciando sincronização bidirecional...")
            
            // ✅ CORREÇÃO CRÍTICA: Capturar timestamp ANTES de fazer PUSH
            // Isso garante que próxima sync incremental não perca dados que foram enviados agora
            val timestampBeforePush = System.currentTimeMillis()
            Log.d(TAG, "   ⏰ Timestamp capturado ANTES do push: $timestampBeforePush (${Date(timestampBeforePush)})")
            
            val progressTracker = onProgress?.let { ProgressTracker(TOTAL_SYNC_OPERATIONS, it).apply { start() } }
            
            // ✅ CORRIGIDO: 1. PUSH primeiro (enviar dados locais para preservar)
            // Isso garante que dados novos locais sejam enviados antes de baixar da nuvem
            Log.d(TAG, "📤 Passo 1: Executando PUSH (enviar dados locais para nuvem)...")
            val pushResult = syncPush(progressTracker)
            if (pushResult.isFailure) {
                Log.w(TAG, "⚠️ Push falhou: ${pushResult.exceptionOrNull()?.message}")
                Log.w(TAG, "⚠️ Continuando com Pull mesmo assim...")
            } else {
                Log.d(TAG, "✅ Push concluído com sucesso - dados locais preservados na nuvem")
            }
            
            // ✅ CORREÇÃO: Aguardar pequeno delay para garantir que Firestore processou
            // Isso evita race condition onde PULL ocorre antes que Firestore salve dados do PUSH
            Log.d(TAG, "   ⏱️ Aguardando 500ms para propagação do Firestore...")
            kotlinx.coroutines.delay(500) // Meio segundo para propagação
            
            // ✅ CORRIGIDO: 2. PULL depois (atualizar dados locais da nuvem)
            // O pull não sobrescreve dados locais mais recentes (verificação de timestamp)
            Log.d(TAG, "📥 Passo 2: Executando PULL (importar atualizações da nuvem)...")
            Log.d(TAG, "   📌 Usando timestamp capturado ANTES do push: $timestampBeforePush")
            val pullResult = syncPull(progressTracker, timestampBeforePush)
            if (pullResult.isFailure) {
                Log.w(TAG, "⚠️ Pull falhou: ${pullResult.exceptionOrNull()?.message}")
                Log.w(TAG, "⚠️ Mas Push pode ter sido bem-sucedido")
            } else {
                Log.d(TAG, "✅ Pull concluído com sucesso - dados locais atualizados")
            }
            
            if (pullResult.isSuccess && pushResult.isSuccess) {
                Log.d(TAG, "✅ ========== SINCRONIZAÇÃO BIDIRECIONAL CONCLUÍDA COM SUCESSO ==========")
                // ✅ NÃO salvar global metadata aqui - já foi salvo por entidade com timestampOverride
                progressTracker?.complete()
                Result.success(Unit)
            } else {
                val errorMsg = "Sincronização parcial: Push=${pushResult.isSuccess}, Pull=${pullResult.isSuccess}"
                Log.w(TAG, "⚠️ $errorMsg")
                progressTracker?.completeWithMessage("Sincronização parcial concluída")
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na sincronização bidirecional: ${e.message}", e)
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
            onProgress?.invoke(SyncProgress(100, "Sincronização falhou"))
            Result.failure(e)
        }
    }
    
    // ==================== FILA DE SINCRONIZAÇÃO ====================
    
    /**
     * Adiciona operação à fila de sincronização.
     * Operações são processadas quando dispositivo estiver online.
     */
    suspend fun enqueueOperation(operation: SyncOperation) {
        try {
            val entity = SyncOperationEntity(
                operationType = operation.type.name,
                entityType = operation.entityType,
                entityId = operation.entityId,
                entityData = operation.data,
                timestamp = operation.timestamp,
                retryCount = operation.retryCount,
                status = SyncOperationStatus.PENDING.name
            )
            appRepository.inserirOperacaoSync(entity)
            val pendingCount = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(_syncStatus.value.pendingOperations + 1)
            _syncStatus.value = _syncStatus.value.copy(pendingOperations = pendingCount)
            Log.d(TAG, "📥 Operação enfileirada: ${operation.type} - entidade=${operation.entityType}, id=${operation.entityId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enfileirar operação ${operation.entityId}: ${e.message}", e)
        }
    }
    
    /**
     * Processa fila de sincronização pendente.
     */
    suspend fun processSyncQueue(): Result<Unit> {
        return try {
            if (!networkUtils.isConnected()) {
                Log.w(TAG, "⚠️ Fila de sincronização não processada: dispositivo offline")
                return Result.failure(Exception("Dispositivo offline - fila pendente"))
            }
            
            val operations = appRepository.obterOperacoesSyncPendentesLimitadas(QUEUE_BATCH_SIZE)
            if (operations.isEmpty()) {
                Log.d(TAG, "📭 Nenhuma operação pendente na fila")
        return Result.success(Unit)
            }
            
            Log.d(TAG, "📦 Processando ${operations.size} operações pendentes")
            var successCount = 0
            var failureCount = 0
            
            operations.forEach { entity ->
                val processingEntity = entity.copy(status = SyncOperationStatus.PROCESSING.name)
                appRepository.atualizarOperacaoSync(processingEntity)
                
                try {
                    processSingleSyncOperation(processingEntity)
                    appRepository.deletarOperacaoSync(processingEntity)
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                    val newRetryCount = processingEntity.retryCount + 1
                    val newStatus = if (newRetryCount >= processingEntity.maxRetries) {
                        SyncOperationStatus.FAILED.name
                    } else {
                        SyncOperationStatus.PENDING.name
                    }
                    Log.e(TAG, "❌ Erro ao processar operação ${processingEntity.id}: ${e.message}", e)
                    appRepository.atualizarOperacaoSync(
                        processingEntity.copy(
                            status = newStatus,
                            retryCount = newRetryCount,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            
            val pendingCount = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
            val failedCount = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
            _syncStatus.value = _syncStatus.value.copy(
                pendingOperations = pendingCount,
                failedOperations = failedCount
            )
            
            Log.d(TAG, "📊 Fila processada: sucesso=$successCount, falhas=$failureCount, pendentes=$pendingCount, falhadas=$failedCount")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar fila de sincronização: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun processSingleSyncOperation(operation: SyncOperationEntity) {
        val operationType = runCatching { SyncOperationType.valueOf(operation.operationType) }.getOrElse {
            throw IllegalArgumentException("Tipo de operação inválido: ${operation.operationType}")
        }
        val collectionRef = resolveCollectionReference(operation.entityType)
        val documentId = operation.entityId.ifBlank {
            throw IllegalArgumentException("entityId vazio para operação ${operation.id}")
        }
        
        when (operationType) {
            SyncOperationType.CREATE, SyncOperationType.UPDATE -> {
                val rawMap: Map<String, Any?> = gson.fromJson(operation.entityData, mapType)
                val mutableData = rawMap.toMutableMap()
                mutableData["lastModified"] = FieldValue.serverTimestamp()
                mutableData["syncTimestamp"] = FieldValue.serverTimestamp()
                collectionRef.document(documentId).set(mutableData).await()
            }
            SyncOperationType.DELETE -> {
                collectionRef.document(documentId).delete().await()
            }
        }
    }
    
    private fun resolveCollectionReference(entityType: String): CollectionReference {
        val normalized = entityType.lowercase(Locale.getDefault())
        val collectionName = when (normalized) {
            COLLECTION_CLIENTES -> COLLECTION_CLIENTES
            COLLECTION_ACERTOS -> COLLECTION_ACERTOS
            COLLECTION_DESPESAS -> COLLECTION_DESPESAS
            COLLECTION_MESAS -> COLLECTION_MESAS
            COLLECTION_ROTAS -> COLLECTION_ROTAS
            COLLECTION_COLABORADORES -> COLLECTION_COLABORADORES
            COLLECTION_MESAS_REFORMADAS -> COLLECTION_MESAS_REFORMADAS
            COLLECTION_MESAS_VENDIDAS -> COLLECTION_MESAS_VENDIDAS
            COLLECTION_EQUIPMENTS -> COLLECTION_EQUIPMENTS
            COLLECTION_CICLOS -> COLLECTION_CICLOS
            COLLECTION_METAS -> COLLECTION_METAS
            else -> normalized
        }
        return getCollectionReference(firestore, collectionName)
    }
    
    // ==================== PULL HANDLERS (SERVIDOR → LOCAL) ====================
    
    /**
     * Pull Clientes: Sincroniza clientes do Firestore para o Room.
     * 
     * ESTRATÉGIA SEGURA:
     * 1. Tenta sincronização incremental se houver metadata (timestamp > 0)
     * 2. Se incremental falhar de qualquer forma, usa método completo (que sempre funciona)
     * 3. Sempre salva metadata após sincronização bem-sucedida
     * 4. Garante que o método completo nunca seja quebrado
     */
    private suspend fun pullClientes(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CLIENTES
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de clientes...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_CLIENTES)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullClientesIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosClientes().first().size }
                        .getOrDefault(0)
                    if (syncedCount > 0 || localCount > 0) {
                        // Incremental adicionou registros ou já existe base local
                        return incrementalResult
                    }
                    Log.w(TAG, "⚠️ Incremental retornou 0 clientes e base local está vazia - executando pull COMPLETO como fallback")
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullClientesComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de clientes.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * 
     * Este método é seguro: se qualquer coisa falhar, retorna null e o método completo é usado.
     */
    private suspend fun tryPullClientesIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro ao executar query incremental de clientes: ${e.message}")
                return null
            }
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            val totalDocuments = documents.size
            
            if (totalDocuments == 0) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL (clientes) com filtro de rota: $totalDocuments documentos")
            
            val cacheStartTime = System.currentTimeMillis()
            val todosClientes = appRepository.obterTodosClientes().first()
            val clientesCache = todosClientes.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de clientes carregado: ${clientesCache.size} em ${System.currentTimeMillis() - cacheStartTime}ms")
            
            val processStartTime = System.currentTimeMillis()
            var processedCount = 0
            documents.forEach { doc ->
                val result = processClienteDocument(doc, clientesCache)
                when (result) {
                    is ProcessResult.Synced -> syncCount++
                    is ProcessResult.Skipped -> skippedCount++
                    is ProcessResult.Error -> errorCount++
                }
                processedCount++
                if (processedCount % 50 == 0) {
                    val elapsed = System.currentTimeMillis() - processStartTime
                    Log.d(TAG, "   📊 Progresso: $processedCount/$totalDocuments documentos processados (${elapsed}ms)")
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Clientes (INCREMENTAL) concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, docs=$totalDocuments")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de clientes.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullClientesComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "📥 Total de clientes no Firestore (após filtro de rota): ${documents.size}")
            
            // ✅ OTIMIZADO: Carregar todos os clientes uma vez e criar cache em memória
            val cacheStartTime = System.currentTimeMillis()
            val todosClientes = appRepository.obterTodosClientes().first()
            val clientesCache = todosClientes.associateBy { it.id }
            val cacheDuration = System.currentTimeMillis() - cacheStartTime
            Log.d(TAG, "   📦 Cache de clientes carregado: ${clientesCache.size} clientes (${cacheDuration}ms)")
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            val processStartTime = System.currentTimeMillis()
            var processedCount = 0
            documents.forEach { doc ->
                val result = processClienteDocument(doc, clientesCache)
                when (result) {
                    is ProcessResult.Synced -> syncCount++
                    is ProcessResult.Skipped -> skippedCount++
                    is ProcessResult.Error -> errorCount++
                }
                processedCount++
                // Log de progresso a cada 50 documentos
                if (processedCount % 50 == 0) {
                    val elapsed = System.currentTimeMillis() - processStartTime
                    Log.d(TAG, "   📊 Progresso: $processedCount/${documents.size} documentos processados (${elapsed}ms)")
                }
            }
            val processDuration = System.currentTimeMillis() - processStartTime
            Log.d(TAG, "   ⏱️ Processamento de documentos: ${processDuration}ms")
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Clientes (COMPLETO) concluído: $syncCount sincronizados, $skippedCount pulados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Processa um documento de cliente do Firestore.
     * Lógica centralizada e reutilizável para ambos os métodos (incremental e completo).
     */
    private sealed class ProcessResult {
        object Synced : ProcessResult()
        object Skipped : ProcessResult()
        object Error : ProcessResult()
    }
    
    /**
     * ✅ OTIMIZADO: Processa documento de cliente usando cache em memória.
     * Evita consultas repetidas ao banco de dados.
     */
    private suspend fun processClienteDocument(
        doc: DocumentSnapshot,
        clientesCache: Map<Long, Cliente>
    ): ProcessResult {
        return try {
                    val clienteData = doc.data
                    if (clienteData == null) {
                return ProcessResult.Skipped
                    }
                    
                    val clienteId = doc.id.toLongOrNull()
                    if (clienteId == null) {
                return ProcessResult.Skipped
                    }
                    
                    // Converter Timestamps do Firestore para Date
                    val dataCadastro = converterTimestampParaDate(clienteData["dataCadastro"])
                        ?: converterTimestampParaDate(clienteData["data_cadastro"])
                        ?: Date()
                    
                    val dataUltimaAtualizacao = converterTimestampParaDate(clienteData["dataUltimaAtualizacao"])
                        ?: converterTimestampParaDate(clienteData["data_ultima_atualizacao"])
                        ?: converterTimestampParaDate(clienteData["lastModified"])
                        ?: Date()
                    
                    val dataCapturaGps = converterTimestampParaDate(clienteData["dataCapturaGps"])
                        ?: converterTimestampParaDate(clienteData["data_captura_gps"])
                    
            // Criar entidade Cliente
                    val clienteFirestore = Cliente(
                        id = clienteId,
                        nome = clienteData["nome"] as? String ?: "Sem nome",
                        nomeFantasia = clienteData["nomeFantasia"] as? String
                            ?: clienteData["nome_fantasia"] as? String,
                        cpfCnpj = clienteData["cpfCnpj"] as? String
                            ?: clienteData["cpf_cnpj"] as? String,
                        telefone = clienteData["telefone"] as? String,
                        telefone2 = clienteData["telefone2"] as? String,
                        email = clienteData["email"] as? String,
                        endereco = clienteData["endereco"] as? String,
                        bairro = clienteData["bairro"] as? String,
                        cidade = clienteData["cidade"] as? String,
                        estado = clienteData["estado"] as? String,
                        cep = clienteData["cep"] as? String,
                        latitude = (clienteData["latitude"] as? Number)?.toDouble(),
                        longitude = (clienteData["longitude"] as? Number)?.toDouble(),
                        precisaoGps = (clienteData["precisaoGps"] as? Number)?.toFloat()
                            ?: (clienteData["precisao_gps"] as? Number)?.toFloat(),
                        dataCapturaGps = dataCapturaGps,
                        rotaId = (clienteData["rotaId"] as? Number)?.toLong()
                            ?: (clienteData["rota_id"] as? Number)?.toLong()
                            ?: 0L,
                        valorFicha = (clienteData["valorFicha"] as? Number)?.toDouble()
                            ?: (clienteData["valor_ficha"] as? Number)?.toDouble() ?: 0.0,
                        comissaoFicha = (clienteData["comissaoFicha"] as? Number)?.toDouble()
                            ?: (clienteData["comissao_ficha"] as? Number)?.toDouble() ?: 0.0,
                        numeroContrato = clienteData["numeroContrato"] as? String
                            ?: clienteData["numero_contrato"] as? String,
                        debitoAnterior = (clienteData["debitoAnterior"] as? Number)?.toDouble()
                            ?: (clienteData["debito_anterior"] as? Number)?.toDouble() ?: 0.0,
                        debitoAtual = (clienteData["debitoAtual"] as? Number)?.toDouble()
                            ?: (clienteData["debito_atual"] as? Number)?.toDouble() ?: 0.0,
                        ativo = clienteData["ativo"] as? Boolean ?: true,
                        observacoes = clienteData["observacoes"] as? String,
                        dataCadastro = dataCadastro,
                        dataUltimaAtualizacao = dataUltimaAtualizacao
                    )
                    
            if (!shouldSyncRouteData(clienteFirestore.rotaId, allowUnknown = false)) {
                return ProcessResult.Skipped
            }
            clienteRotaCache[clienteId] = clienteFirestore.rotaId
            
            // Validar dados obrigatórios
            if (clienteFirestore.nome.isBlank() || clienteFirestore.rotaId == 0L) {
                return ProcessResult.Skipped
            }
            
            // ✅ OTIMIZADO: Usar cache em memória em vez de consulta ao banco
            val clienteLocal = clientesCache[clienteId]
                    val serverTimestamp = dataUltimaAtualizacao.time
                    val localTimestamp = clienteLocal?.dataUltimaAtualizacao?.time ?: 0L
                    
                    when {
                clienteLocal == null || serverTimestamp > localTimestamp -> {
                    appRepository.inserirCliente(clienteFirestore)
                    ProcessResult.Synced
                        }
                        else -> {
                    ProcessResult.Skipped
                        }
                    }
                } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar cliente ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Converte Timestamp do Firestore para Date do Java
     */
    private fun converterTimestampParaDate(value: Any?): Date? {
        return when (value) {
            is com.google.firebase.Timestamp -> value.toDate()
            is Long -> Date(value)
            is String -> try {
                Date(value.toLong())
            } catch (e: Exception) {
                null
            }
            else -> null
        }
    }
    
    /**
     * ✅ NOVO: Converte timestamp do Firestore para LocalDateTime
     * Necessário para campos dataHora da entidade Despesa
     */
    private fun converterTimestampParaLocalDateTime(value: Any?): java.time.LocalDateTime? {
        return when (value) {
            is com.google.firebase.Timestamp -> {
                value.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            }
            is Long -> {
                Date(value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            }
            is String -> {
                try {
                    // Tentar parsear como ISO string ou timestamp
                    if (value.contains("T") || value.contains("-")) {
                        java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ISO_DATE_TIME)
                    } else {
                        Date(value.toLong()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                    }
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }
    
    /**
     * Pull Rotas: Sincroniza rotas do Firestore para o Room
     */
    private suspend fun pullRotas(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ROTAS

        return try {
            Log.d(TAG, "🔵 Iniciando pull de rotas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ROTAS)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            Log.d(TAG, "📊 Rotas: lastSyncTimestamp=$lastSyncTimestamp, canUseIncremental=$canUseIncremental, allowRouteBootstrap=$allowRouteBootstrap")

            var incrementalExecutado = false
            if (canUseIncremental) {
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL de rotas (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullRotasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                incrementalExecutado = incrementalResult != null
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasRotas().first().size }.getOrDefault(0)
                    Log.d(TAG, "📊 Rotas incremental: syncedCount=$syncedCount, localCount=$localCount")
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Log.w(TAG, "⚠️ Rotas: incremental trouxe $syncedCount registros com base local $localCount - executando pull completo")
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de rotas falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de rotas - usando método COMPLETO")
            }

            return pullRotasComplete(collectionRef, entityType, startTime, incrementalExecutado, timestampOverride)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de rotas: ${e.message}", e)
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }

    private suspend fun pullRotasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        incrementalFallback: Boolean = false,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de rotas - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                Log.w(TAG, "⚠️ Nenhuma rota encontrada no Firestore")
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val rotasCache = appRepository.obterTodasRotas().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processRotasDocuments(snapshot.documents, rotasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull de rotas concluído (fallback incremental=$incrementalFallback): sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de rotas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullRotasIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao criar query incremental para rotas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Rotas - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                Log.d(TAG, "✅ Nenhuma rota nova/alterada desde a última sincronização")
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val rotasCache = appRepository.obterTodasRotas().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processRotasDocuments(documents, rotasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de rotas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de rotas: ${e.message}", e)
            null // Forçar fallback para método completo
        }
    }

    private suspend fun processRotasDocuments(
        documents: List<DocumentSnapshot>,
        rotasCache: MutableMap<Long, Rota>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processRotaDocument(doc, rotasCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processRotaDocument(
        doc: DocumentSnapshot,
        rotasCache: MutableMap<Long, Rota>
    ): ProcessResult {
        return try {
            val rotaData = doc.data ?: return ProcessResult.Skipped
            val nome = (rotaData["nome"] as? String)?.takeIf { it.isNotBlank() } ?: return ProcessResult.Skipped
            
                    val roomId = (rotaData["roomId"] as? Number)?.toLong()
                        ?: (rotaData["id"] as? Number)?.toLong()
                        ?: doc.id.toLongOrNull()
                    
            // ✅ CORREÇÃO: Durante bootstrap, permitir todas as rotas temporariamente
            if (roomId != null && !allowRouteBootstrap && !shouldSyncRouteData(roomId, allowUnknown = false)) {
                Log.d(TAG, "⏭️ Rota ignorada por falta de acesso: ID=$roomId")
                return ProcessResult.Skipped
            }
            
                        val dataCriacaoLong = converterTimestampParaDate(rotaData["dataCriacao"])
                            ?.time ?: converterTimestampParaDate(rotaData["data_criacao"])?.time
                            ?: System.currentTimeMillis()
                        val dataAtualizacaoLong = converterTimestampParaDate(rotaData["dataAtualizacao"])
                            ?.time ?: converterTimestampParaDate(rotaData["data_atualizacao"])?.time
                            ?: converterTimestampParaDate(rotaData["lastModified"])?.time
                            ?: System.currentTimeMillis()
                        
            if (roomId == null) {
                Log.w(TAG, "⚠️ Rota ${doc.id} sem roomId válido - criando registro local com ID autogerado")
                        val rotaNova = Rota(
                            nome = nome,
                            descricao = rotaData["descricao"] as? String ?: "",
                            colaboradorResponsavel = rotaData["colaboradorResponsavel"] as? String
                                ?: rotaData["colaborador_responsavel"] as? String ?: "Não definido",
                            cidades = rotaData["cidades"] as? String ?: "Não definido",
                            ativa = rotaData["ativa"] as? Boolean ?: true,
                            cor = rotaData["cor"] as? String ?: "#6200EA",
                            dataCriacao = dataCriacaoLong,
                            dataAtualizacao = dataAtualizacaoLong
                        )
                        val insertedId = appRepository.inserirRota(rotaNova)
                rotasCache[insertedId] = rotaNova.copy(id = insertedId)
                Log.d(TAG, "✅ Rota criada sem roomId: ${rotaNova.nome} (ID Room: $insertedId)")
                ProcessResult.Synced
            } else {
                    val rotaFirestore = Rota(
                        id = roomId,
                        nome = nome,
                        descricao = rotaData["descricao"] as? String ?: "",
                        colaboradorResponsavel = rotaData["colaboradorResponsavel"] as? String
                            ?: rotaData["colaborador_responsavel"] as? String ?: "Não definido",
                        cidades = rotaData["cidades"] as? String ?: "Não definido",
                        ativa = rotaData["ativa"] as? Boolean ?: true,
                        cor = rotaData["cor"] as? String ?: "#6200EA",
                        dataCriacao = dataCriacaoLong,
                        dataAtualizacao = dataAtualizacaoLong
                    )
                    
                val localRota = rotasCache[roomId]
                val localTimestamp = localRota?.dataAtualizacao ?: 0L
                    val serverTimestamp = rotaFirestore.dataAtualizacao
                
                return if (localRota == null || serverTimestamp > localTimestamp) {
                    appRepository.inserirRota(rotaFirestore)
                    rotasCache[roomId] = rotaFirestore
                    Log.d(TAG, "🔄 Rota sincronizada: ${rotaFirestore.nome} (ID=$roomId)")
                    ProcessResult.Synced
                } else {
                    Log.d(TAG, "⏭️ Rota mantida localmente (mais recente): ${rotaFirestore.nome} (ID=$roomId)")
                    ProcessResult.Skipped
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar rota ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Mesas: Sincroniza mesas do Firestore para o Room
     * ✅ NOVO (2025): Implementa sincronização incremental seguindo padrão de Clientes
     */
    private suspend fun pullMesas(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS
        
        return try {
            Log.d(TAG, "Iniciando pull de mesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔍 [DIAGNOSTICO] Iniciando Incremental Mesas")
                Log.d(TAG, "🔍 [DIAGNOSTICO] lastSyncTimestamp (Long): $lastSyncTimestamp")
                Log.d(TAG, "🔍 [DIAGNOSTICO] lastSyncTimestamp (Date): ${Date(lastSyncTimestamp)}")
                Log.d(TAG, "🔍 [DIAGNOSTICO] timestampOverride: $timestampOverride")
                Log.d(TAG, "🔍 [DIAGNOSTICO] CurrentTime: ${System.currentTimeMillis()}")
                
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullMesasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesas().first().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há mesas locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 mesas mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullMesasComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullMesasComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de mesas.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     */
    private suspend fun tryPullMesasIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ CORREÇÃO CRÍTICA: Estratégia híbrida para garantir que mesas não desapareçam
            // 1. Tentar buscar apenas mesas modificadas recentemente (otimização)
            // 2. Se retornar 0 mas houver mesas locais, buscar TODAS para garantir sincronização completa
            
            // ✅ CORREÇÃO: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todasMesas = appRepository.obterTodasMesas().first()
            val mesasCache = todasMesas.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de mesas carregado: ${mesasCache.size} mesas locais")
            
            // Tentar query incremental primeiro (otimização)
            Log.d(TAG, "🔍 [DIAGNOSTICO] Executando Query: collectionRef.whereGreaterThan('lastModified', ${Date(lastSyncTimestamp)})")
            val incrementalMesas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todas as mesas: ${e.message}")
                emptyList()
            }
            
            Log.d(TAG, "🔍 [DIAGNOSTICO] Query retornou ${incrementalMesas.size} documentos")
            incrementalMesas.forEach { doc ->
                val lm = doc.getTimestamp("lastModified")?.toDate()
                Log.d(TAG, "🔍 [DIAGNOSTICO] Doc encontrado: ${doc.id}, lastModified: $lm (${lm?.time})")
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há mesas locais, buscar TODAS
            val allMesas = if (incrementalMesas.isEmpty() && mesasCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 mesas mas há ${mesasCache.size} locais - buscando TODAS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todas as mesas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesas
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allMesas.size} documentos encontrados")
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            // Processar documentos com filtro de rota
            val processStartTime = System.currentTimeMillis()
            allMesas.forEach { doc ->
                try {
                    val mesaData = doc.data ?: run {
                        errorCount++
                        return@forEach
                    }
                    val mesaId = doc.id.toLongOrNull() ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    val mesaJson = gson.toJson(mesaData)
                    val mesaFirestore = gson.fromJson(mesaJson, Mesa::class.java)
                        ?.copy(id = mesaId) ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    // Verificar se deve sincronizar baseado na rota do cliente
                    val rotaId = getClienteRouteId(mesaFirestore.clienteId)
                    if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                        skippedCount++
                        return@forEach
                    }
                    mesaRotaCache[mesaId] = rotaId
                    
                    val mesaLocal = mesasCache[mesaId]
                    
                    // ✅ CORREÇÃO: Verificar timestamp do servidor vs local
                    val serverTimestamp = (mesaData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (mesaData["dataUltimaLeitura"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: mesaFirestore.dataUltimaLeitura?.time
                        ?: mesaFirestore.dataInstalacao.time
                    val localTimestamp = mesaLocal?.dataUltimaLeitura?.time
                        ?: mesaLocal?.dataInstalacao?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = mesaLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (mesaLocal == null) {
                            appRepository.inserirMesa(mesaFirestore)
                        } else {
                            appRepository.atualizarMesa(mesaFirestore)
                        }
                        syncCount++
                    } else {
                        skippedCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao sincronizar mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val processDuration = System.currentTimeMillis() - processStartTime
            Log.d(TAG, "   ⏱️ Processamento de documentos: ${processDuration}ms")
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Mesas (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizadas, $skippedCount puladas, $errorCount erros")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de mesas.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullMesasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val mesaData = doc.data ?: run {
                        errorCount++
                        return@forEach
                    }
                    val mesaId = doc.id.toLongOrNull() ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    val mesaJson = gson.toJson(mesaData)
                    val mesaFirestore = gson.fromJson(mesaJson, Mesa::class.java)
                        ?.copy(id = mesaId) ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    val rotaId = getClienteRouteId(mesaFirestore.clienteId)
                    if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                        skippedCount++
                        return@forEach
                    }
                    mesaRotaCache[mesaId] = rotaId
                    
                    val mesaLocal = appRepository.obterMesaPorId(mesaId)
                    
                    // Mesas geralmente não têm timestamp, usar sempre atualizar se existir
                    when {
                        mesaLocal == null -> {
                            appRepository.inserirMesa(mesaFirestore)
                            syncCount++
                        }
                        else -> {
                            appRepository.atualizarMesa(mesaFirestore)
                            syncCount++
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao sincronizar mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Mesas (COMPLETO) concluído: $syncCount sincronizadas, $skippedCount puladas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Colaboradores: Sincroniza colaboradores do Firestore para o Room
     */
    private suspend fun pullColaboradores(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_COLABORADORES

        return try {
            Log.d(TAG, "Iniciando pull de colaboradores...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_COLABORADORES)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            Log.d(TAG, "📊 Colaboradores: lastSyncTimestamp=$lastSyncTimestamp, canUseIncremental=$canUseIncremental, allowRouteBootstrap=$allowRouteBootstrap")

            var incrementalExecutado = false
            if (canUseIncremental) {
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL de colaboradores (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullColaboradoresIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                incrementalExecutado = incrementalResult != null
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosColaboradores().first().size }.getOrDefault(0)
                    Log.d(TAG, "📊 Colaboradores incremental: syncedCount=$syncedCount, localCount=$localCount")
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Log.w(TAG, "⚠️ Colaboradores: incremental trouxe $syncedCount registros com base local $localCount - executando pull COMPLETO")
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de colaboradores falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de colaboradores - usando método COMPLETO")
            }

            return pullColaboradoresComplete(collectionRef, entityType, startTime, incrementalExecutado, timestampOverride)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de colaboradores: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullColaboradoresComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        incrementalFallback: Boolean = false,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de colaboradores - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val colaboradoresCache = appRepository.obterTodosColaboradores().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processColaboradoresDocuments(snapshot.documents, colaboradoresCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull de colaboradores concluído (fallback incremental=$incrementalFallback): sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull completo de colaboradores: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullColaboradoresIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao criar query incremental para colaboradores: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Colaboradores - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val colaboradoresCache = appRepository.obterTodosColaboradores().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processColaboradoresDocuments(documents, colaboradoresCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de colaboradores: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull incremental de colaboradores: ${e.message}", e)
            null
        }
    }

    private suspend fun processColaboradoresDocuments(
        documents: List<DocumentSnapshot>,
        colaboradoresCache: MutableMap<Long, Colaborador>
    ): Triple<Int, Int, Int> {
        var syncCount = 0
        var skippedCount = 0
        var errorCount = 0
        
        documents.forEach { doc ->
            when (processColaboradorDocument(doc, colaboradoresCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processColaboradorDocument(
        doc: DocumentSnapshot,
        colaboradoresCache: MutableMap<Long, Colaborador>
    ): ProcessResult {
        return try {
            val colaboradorData = doc.data ?: return ProcessResult.Skipped
            val colaboradorId = doc.id.toLongOrNull()
                ?: (colaboradorData["roomId"] as? Number)?.toLong()
                ?: (colaboradorData["id"] as? Number)?.toLong()
                ?: return ProcessResult.Skipped
            
            val colaboradorJson = gson.toJson(colaboradorData)
            val colaboradorFirestore = gson.fromJson(colaboradorJson, Colaborador::class.java)?.copy(id = colaboradorId)
                ?: return ProcessResult.Error
            
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                ?: (colaboradorData["dataUltimaAtualizacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                ?: colaboradorFirestore.dataUltimaAtualizacao.time
            
            // ✅ CORREÇÃO: Verificar duplicata por ID primeiro
            val localColaborador = colaboradoresCache[colaboradorId]
            val localTimestamp = localColaborador?.dataUltimaAtualizacao?.time ?: 0L
            
            // ✅ CORREÇÃO: Se não encontrou por ID, verificar por email para evitar duplicatas
            val localColaboradorPorEmail = if (localColaborador == null && colaboradorFirestore.email.isNotEmpty()) {
                appRepository.obterColaboradorPorEmail(colaboradorFirestore.email)
            } else {
                null
            }
            
            return when {
                // Se encontrou por ID, usar lógica normal de atualização
                localColaborador != null -> {
                    if (serverTimestamp > localTimestamp) {
                        appRepository.atualizarColaborador(colaboradorFirestore)
                        colaboradoresCache[colaboradorId] = colaboradorFirestore
                        ProcessResult.Synced
                    } else {
                        ProcessResult.Skipped
                    }
                }
                // ✅ CORREÇÃO: Se encontrou por email mas com ID diferente, atualizar o existente
                localColaboradorPorEmail != null -> {
                    Log.d(TAG, "⚠️ Colaborador duplicado encontrado por email: ${colaboradorFirestore.email} (ID local: ${localColaboradorPorEmail.id}, ID Firestore: $colaboradorId)")
                    // Atualizar o colaborador existente com os dados do Firestore, mantendo o ID local
                    val colaboradorAtualizado = colaboradorFirestore.copy(id = localColaboradorPorEmail.id)
                    if (serverTimestamp > (localColaboradorPorEmail.dataUltimaAtualizacao?.time ?: 0L)) {
                        appRepository.atualizarColaborador(colaboradorAtualizado)
                        colaboradoresCache[localColaboradorPorEmail.id] = colaboradorAtualizado
                        ProcessResult.Synced
                    } else {
                        ProcessResult.Skipped
                    }
                }
                // Se não encontrou nem por ID nem por email, inserir novo
                else -> {
                    appRepository.inserirColaborador(colaboradorFirestore)
                    colaboradoresCache[colaboradorId] = colaboradorFirestore
                    ProcessResult.Synced
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar colaborador ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Ciclos: Sincroniza ciclos do Firestore para o Room
     */
    private suspend fun pullCiclos(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CICLOS
        
        return try {
            Log.d(TAG, "Iniciando pull de ciclos...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_CICLOS)
            
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL de ciclos (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullCiclosIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosCiclos().first().size }.getOrDefault(0)
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Log.w(TAG, "⚠️ Incremental de ciclos trouxe $syncedCount registros e base local possui $localCount - executando pull COMPLETO")
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de ciclos falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de ciclos - usando método COMPLETO")
            }
            
            pullCiclosComplete(collectionRef, entityType, startTime, timestampOverride)
                    } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullCiclosComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            // ✅ CORREÇÃO CRÍTICA: Garantir que busca funcione mesmo sem rotas (bootstrap)
            // Se não há rotas atribuídas e não está em bootstrap, forçar bootstrap temporariamente
            resetRouteFilters()
            val accessibleRoutes = getAccessibleRouteIdsInternal()
            val needsBootstrap = accessibleRoutes.isEmpty() && !allowRouteBootstrap
            
            if (needsBootstrap) {
                Log.w(TAG, "⚠️ Bootstrap necessário para ciclos: habilitando temporariamente")
                allowRouteBootstrap = true
            }
            
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "📥 Pull COMPLETO de ciclos - documentos recebidos: ${documents.size}")
            
            // Restaurar estado de bootstrap
            if (needsBootstrap) {
                allowRouteBootstrap = false
            }
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val ciclosCache = runCatching { appRepository.obterTodosCiclos().first() }.getOrDefault(emptyList())
                .associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processCiclosDocuments(documents, ciclosCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull de ciclos concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull completo de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullCiclosIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao executar query incremental para ciclos: ${e.message}")
                return null
            }
            Log.d(TAG, "📥 Ciclos - incremental retornou ${documents.size} documentos (após filtro de rota)")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val ciclosCache = runCatching { appRepository.obterTodosCiclos().first() }.getOrDefault(emptyList())
                .associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processCiclosDocuments(documents, ciclosCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de ciclos: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull incremental de ciclos: ${e.message}", e)
            null
        }
    }

    private suspend fun processCiclosDocuments(
        documents: List<DocumentSnapshot>,
        ciclosCache: MutableMap<Long, CicloAcertoEntity>
    ): Triple<Int, Int, Int> {
        var syncCount = 0
        var skippedCount = 0
        var errorCount = 0
        
        documents.forEach { doc ->
            when (processCicloDocument(doc, ciclosCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processCicloDocument(
        doc: DocumentSnapshot,
        ciclosCache: MutableMap<Long, CicloAcertoEntity>
    ): ProcessResult {
        return try {
            val cicloData = doc.data ?: return ProcessResult.Skipped
            val cicloId = doc.id.toLongOrNull()
                ?: (cicloData["roomId"] as? Number)?.toLong()
                ?: (cicloData["id"] as? Number)?.toLong()
                ?: return ProcessResult.Skipped
            
            val cicloJson = gson.toJson(cicloData)
            val cicloFirestore = gson.fromJson(cicloJson, CicloAcertoEntity::class.java)?.copy(id = cicloId)
                ?: return ProcessResult.Error
            
            if (!shouldSyncRouteData(cicloFirestore.rotaId, allowUnknown = false)) {
                return ProcessResult.Skipped
            }
            
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                ?: (cicloData["dataAtualizacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                ?: cicloFirestore.dataAtualizacao.time
            val localTimestamp = ciclosCache[cicloId]?.dataAtualizacao?.time ?: 0L
            
            return if (ciclosCache[cicloId] == null) {
                appRepository.inserirCicloAcerto(cicloFirestore)
                ciclosCache[cicloId] = cicloFirestore
                ProcessResult.Synced
            } else if (serverTimestamp > localTimestamp) {
                appRepository.inserirCicloAcerto(cicloFirestore)
                ciclosCache[cicloId] = cicloFirestore
                ProcessResult.Synced
            } else {
                ProcessResult.Skipped
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar ciclo ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Acertos: Sincroniza acertos do Firestore para o Room
     * ✅ NOVO (2025): Implementa sincronização incremental seguindo padrão de Clientes
     * Importante: Sincronizar também AcertoMesa relacionados
     */
    private suspend fun pullAcertos(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ACERTOS
        
        return try {
            Log.d(TAG, "🔄 Iniciando pull de acertos...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ACERTOS)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullAcertosIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosAcertos().first().size }.getOrDefault(0)
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Log.w(TAG, "⚠️ Incremental de acertos trouxe $syncedCount registros com base local $localCount - executando pull completo")
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de acertos falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de acertos - usando método COMPLETO")
            }

            return pullAcertosComplete(collectionRef, entityType, startTime, timestampOverride)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de acertos: ${e.message}", e)
            return Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de acertos.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * 
     * Este método é seguro: se qualquer coisa falhar, retorna null e o método completo é usado.
     */
    private suspend fun tryPullAcertosIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro ao executar query incremental de acertos: ${e.message}")
                return null
            }
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            val totalDocuments = documents.size
            
            if (totalDocuments == 0) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL de acertos (após filtro de rota): $totalDocuments documentos")
            
            val processStartTime = System.currentTimeMillis()
            var processedCount = 0
            documents.forEach { doc ->
                try {
                    val acertoFirestore = documentToAcerto(doc) ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    val rotaId = acertoFirestore.rotaId ?: getClienteRouteId(acertoFirestore.clienteId)
                    if (!shouldSyncRouteData(rotaId, clienteId = acertoFirestore.clienteId, allowUnknown = false)) {
                        skippedCount++
                        return@forEach
                    }
                    
                    val acertoLocal = appRepository.obterAcertoPorId(acertoFirestore.id)
                    
                    val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                        ?: acertoFirestore.dataAcerto.time
                    val localTimestamp = acertoLocal?.dataAcerto?.time ?: 0L
                    
                    when {
                        acertoLocal == null -> {
                            Log.d(TAG, "➕ Inserindo novo acerto ID: ${acertoFirestore.id}, clienteId: ${acertoFirestore.clienteId}")
                            appRepository.inserirAcerto(acertoFirestore)
                            maintainLocalAcertoHistory(acertoFirestore.clienteId)
                            syncCount++
                            
                            pullAcertoMesas(acertoFirestore.id)
                        }
                        serverTimestamp > localTimestamp -> {
                            Log.d(TAG, "🔄 Atualizando acerto existente ID: ${acertoFirestore.id}")
                            appRepository.atualizarAcerto(acertoFirestore)
                            maintainLocalAcertoHistory(acertoFirestore.clienteId)
                            syncCount++
                            
                            pullAcertoMesas(acertoFirestore.id)
                        }
                        else -> {
                            skippedCount++
                            Log.d(TAG, "⏭️ Acerto ${acertoFirestore.id} já está atualizado")
                        }
                    }
                    processedCount++
                    if (processedCount % 50 == 0) {
                        val elapsed = System.currentTimeMillis() - processStartTime
                        Log.d(TAG, "   📊 Progresso: $processedCount/$totalDocuments documentos processados (${elapsed}ms)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao sincronizar acerto ${doc.id}: ${e.message}", e)
                }
            }
            val processDuration = System.currentTimeMillis() - processStartTime
            Log.d(TAG, "   ⏱️ Processamento de documentos: ${processDuration}ms")
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Acertos (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizados, $skippedCount pulados, $errorCount erros")
            Log.d(TAG, "   📥 $totalDocuments documentos processados")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de acertos.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullAcertosComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "📊 Total de acertos no Firestore (após filtro de rota): ${documents.size}")
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            documents.forEach { doc ->
                try {
                    val acertoFirestore = documentToAcerto(doc) ?: run {
                        errorCount++
                        return@forEach
                    }
                    
                    val rotaId = acertoFirestore.rotaId ?: getClienteRouteId(acertoFirestore.clienteId)
                    if (!shouldSyncRouteData(rotaId, clienteId = acertoFirestore.clienteId, allowUnknown = false)) {
                        skippedCount++
                        return@forEach
                    }
                    
                    val acertoLocal = appRepository.obterAcertoPorId(acertoFirestore.id)
                    
                    val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                        ?: acertoFirestore.dataAcerto.time
                    val localTimestamp = acertoLocal?.dataAcerto?.time ?: 0L
                    
                    when {
                        acertoLocal == null -> {
                            Log.d(TAG, "➕ Inserindo novo acerto ID: ${acertoFirestore.id}, clienteId: ${acertoFirestore.clienteId}")
                            appRepository.inserirAcerto(acertoFirestore)
                            maintainLocalAcertoHistory(acertoFirestore.clienteId)
                            syncCount++
                            
                            // Sincronizar AcertoMesa relacionados
                            pullAcertoMesas(acertoFirestore.id)
                        }
                        serverTimestamp > localTimestamp -> {
                            Log.d(TAG, "🔄 Atualizando acerto existente ID: ${acertoFirestore.id}")
                            appRepository.atualizarAcerto(acertoFirestore)
                            maintainLocalAcertoHistory(acertoFirestore.clienteId)
                            syncCount++
                            
                            // Sincronizar AcertoMesa relacionados
                            pullAcertoMesas(acertoFirestore.id)
                        }
                        else -> {
                            skippedCount++
                            Log.d(TAG, "⏭️ Acerto ${acertoFirestore.id} já está atualizado")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao sincronizar acerto ${doc.id}: ${e.message}", e)
                    Log.e(TAG, "❌ Stack trace: ${e.stackTraceToString()}")
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Acertos (COMPLETO) concluído: $syncCount sincronizados, $skippedCount pulados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de acertos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull AcertoMesas: Sincroniza mesas de acerto relacionadas
     * ✅ CORREÇÃO: Faz download de fotos do Firebase Storage quando necessário
     */
    private suspend fun pullAcertoMesas(acertoId: Long) {
        try {
            val collectionRef = getCollectionReference(firestore, COLLECTION_ACERTO_MESAS)
            val snapshot = collectionRef
                .whereEqualTo("acertoId", acertoId)
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                try {
                    val acertoMesaData = doc.data ?: return@forEach
                    val acertoMesaJson = gson.toJson(acertoMesaData)
                    var acertoMesa = gson.fromJson(acertoMesaJson, AcertoMesa::class.java)
                        ?: return@forEach
                    
                    // ✅ CORREÇÃO: Verificar se já existe foto local antes de baixar novamente
                    val fotoRelogioFinal = acertoMesa.fotoRelogioFinal
                    if (!fotoRelogioFinal.isNullOrEmpty() && 
                        firebaseImageUploader.isFirebaseStorageUrl(fotoRelogioFinal)) {
                        
                        // ✅ NOVO: Verificar se já existe um AcertoMesa local com foto para esta mesa/acerto
                        val acertoMesaExistente = appRepository.buscarAcertoMesaPorAcertoEMesa(
                            acertoMesa.acertoId,
                            acertoMesa.mesaId
                        )
                        
                        // Se já existe e tem foto local válida, reutilizar
                        val caminhoLocal = if (acertoMesaExistente != null && 
                                               !acertoMesaExistente.fotoRelogioFinal.isNullOrEmpty() &&
                                               !firebaseImageUploader.isFirebaseStorageUrl(acertoMesaExistente.fotoRelogioFinal)) {
                            val arquivoExistente = java.io.File(acertoMesaExistente.fotoRelogioFinal!!)
                            if (arquivoExistente.exists()) {
                                Log.d(TAG, "✅ Reutilizando foto local existente: ${acertoMesaExistente.fotoRelogioFinal}")
                                acertoMesaExistente.fotoRelogioFinal
                            } else {
                                // Arquivo foi deletado, baixar novamente
                                Log.d(TAG, "📥 Arquivo local não existe mais, baixando novamente para mesa ${acertoMesa.mesaId}")
                                firebaseImageUploader.downloadMesaRelogio(
                                    fotoRelogioFinal,
                                    acertoMesa.mesaId,
                                    acertoMesa.acertoId // ✅ NOVO: Usar acertoId para nome fixo
                                )
                            }
                        } else {
                            // Não existe foto local, baixar
                            Log.d(TAG, "📥 Fazendo download de foto do relógio para mesa ${acertoMesa.mesaId}")
                            firebaseImageUploader.downloadMesaRelogio(
                                fotoRelogioFinal,
                                acertoMesa.mesaId,
                                acertoMesa.acertoId // ✅ NOVO: Usar acertoId para nome fixo
                            )
                        }
                        
                        if (caminhoLocal != null) {
                            // ✅ Atualizar AcertoMesa com o caminho local
                            acertoMesa = acertoMesa.copy(
                                fotoRelogioFinal = caminhoLocal
                            )
                            Log.d(TAG, "✅ Foto salva localmente: $caminhoLocal")
                        } else {
                            Log.w(TAG, "⚠️ Falha ao baixar foto, mantendo URL do Firebase: $fotoRelogioFinal")
                        }
                    }
                    
                    // Inserir ou atualizar AcertoMesa (inserirAcertoMesa usa OnConflictStrategy.REPLACE)
                    appRepository.inserirAcertoMesa(acertoMesa)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar AcertoMesa ${doc.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de AcertoMesas para acerto $acertoId: ${e.message}", e)
        }
    }
    
    /**
     * Pull Despesas: Sincroniza despesas do Firestore para o Room
     * ✅ NOVO (2025): Implementa sincronização incremental seguindo padrão de Clientes
     */
    private suspend fun pullDespesas(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_DESPESAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de despesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_DESPESAS)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullDespesasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    // Incremental funcionou!
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullDespesasComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de despesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de despesas.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     */
    private suspend fun tryPullDespesasIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro ao executar query incremental de despesas: ${e.message}")
                return null
            }
            val totalDocuments = documents.size
            
            if (totalDocuments == 0) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL de despesas (após filtro de rota): $totalDocuments documentos")
                
                // Processar documentos
                val processStartTime = System.currentTimeMillis()
                var processedCount = 0
            documents.forEach { doc ->
                    try {
                        val despesaData = doc.data ?: emptyMap()
                        
                        val despesaId = (despesaData["roomId"] as? Long) 
                            ?: (despesaData["id"] as? Long) 
                            ?: doc.id.toLongOrNull() 
                            ?: 0L
                        
                        if (despesaId == 0L) {
                            skipCount++
                            return@forEach
                        }
                        
                        val dataHora = converterTimestampParaLocalDateTime(despesaData["dataHora"])
                            ?: converterTimestampParaLocalDateTime(despesaData["data_hora"])
                            ?: java.time.LocalDateTime.now()
                        
                        val despesaFirestore = Despesa(
                            id = despesaId,
                            rotaId = (despesaData["rotaId"] as? Number)?.toLong() ?: (despesaData["rota_id"] as? Number)?.toLong() ?: 0L,
                            descricao = (despesaData["descricao"] as? String) ?: "",
                            valor = (despesaData["valor"] as? Number)?.toDouble() ?: 0.0,
                            categoria = (despesaData["categoria"] as? String) ?: "",
                            tipoDespesa = (despesaData["tipoDespesa"] as? String) ?: (despesaData["tipo_despesa"] as? String) ?: "",
                            dataHora = dataHora,
                            observacoes = (despesaData["observacoes"] as? String) ?: "",
                            criadoPor = (despesaData["criadoPor"] as? String) ?: (despesaData["criado_por"] as? String) ?: "",
                            cicloId = (despesaData["cicloId"] as? Number)?.toLong() ?: (despesaData["ciclo_id"] as? Number)?.toLong(),
                            origemLancamento = (despesaData["origemLancamento"] as? String) ?: (despesaData["origem_lancamento"] as? String) ?: "ROTA",
                            cicloAno = (despesaData["cicloAno"] as? Number)?.toInt() ?: (despesaData["ciclo_ano"] as? Number)?.toInt(),
                            cicloNumero = (despesaData["cicloNumero"] as? Number)?.toInt() ?: (despesaData["ciclo_numero"] as? Number)?.toInt(),
                            fotoComprovante = (despesaData["fotoComprovante"] as? String) ?: (despesaData["foto_comprovante"] as? String),
                            dataFotoComprovante = converterTimestampParaDate(despesaData["dataFotoComprovante"]) ?: converterTimestampParaDate(despesaData["data_foto_comprovante"]),
                            veiculoId = (despesaData["veiculoId"] as? Number)?.toLong() ?: (despesaData["veiculo_id"] as? Number)?.toLong(),
                            kmRodado = (despesaData["kmRodado"] as? Number)?.toLong() ?: (despesaData["km_rodado"] as? Number)?.toLong(),
                            litrosAbastecidos = (despesaData["litrosAbastecidos"] as? Number)?.toDouble() ?: (despesaData["litros_abastecidos"] as? Number)?.toDouble()
                        )
                        
                        if (!shouldSyncRouteData(despesaFirestore.rotaId, allowUnknown = false)) {
                            skipCount++
                            return@forEach
                        }
                        
                        if (despesaFirestore.descricao.isBlank() || despesaFirestore.rotaId == 0L) {
                            skipCount++
                            return@forEach
                        }
                        
                        val despesaLocal = appRepository.obterDespesaPorId(despesaId)
                        
                        val serverTimestamp = (despesaData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                            ?: (despesaData["syncTimestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                            ?: dataHora.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                        
                        val localTimestamp = despesaLocal?.dataHora?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000) ?: 0L
                        
                        when {
                            despesaLocal == null -> {
                                appRepository.inserirDespesa(despesaFirestore)
                                syncCount++
                            }
                            serverTimestamp > (localTimestamp + 1000) -> {
                                appRepository.atualizarDespesa(despesaFirestore)
                                syncCount++
                            }
                            else -> {
                                skipCount++
                            }
                        }
                        processedCount++
                        if (processedCount % 50 == 0) {
                            val elapsed = System.currentTimeMillis() - processStartTime
                            Log.d(TAG, "   📊 Progresso: $processedCount/$totalDocuments documentos processados (${elapsed}ms)")
                        }
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "❌ Erro ao sincronizar despesa ${doc.id}: ${e.message}", e)
                    }
                }
                val processDuration = System.currentTimeMillis() - processStartTime
                Log.d(TAG, "   ⏱️ Processamento de documentos: ${processDuration}ms")
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Despesas (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Log.d(TAG, "   📥 $totalDocuments documentos processados")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de despesas.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullDespesasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "📥 Total de despesas no Firestore (após filtro de rota): ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
            documents.forEach { doc ->
                try {
                    val despesaData = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando despesa: ID=${doc.id}")
                    
                    val despesaId = (despesaData["roomId"] as? Long) 
                        ?: (despesaData["id"] as? Long) 
                        ?: doc.id.toLongOrNull() 
                        ?: 0L
                    
                    if (despesaId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para despesa ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val dataHora = converterTimestampParaLocalDateTime(despesaData["dataHora"])
                        ?: converterTimestampParaLocalDateTime(despesaData["data_hora"])
                        ?: java.time.LocalDateTime.now()
                    
                    val despesaFirestore = Despesa(
                        id = despesaId,
                        rotaId = (despesaData["rotaId"] as? Number)?.toLong() ?: (despesaData["rota_id"] as? Number)?.toLong() ?: 0L,
                        descricao = (despesaData["descricao"] as? String) ?: "",
                        valor = (despesaData["valor"] as? Number)?.toDouble() ?: 0.0,
                        categoria = (despesaData["categoria"] as? String) ?: "",
                        tipoDespesa = (despesaData["tipoDespesa"] as? String) ?: (despesaData["tipo_despesa"] as? String) ?: "",
                        dataHora = dataHora,
                        observacoes = (despesaData["observacoes"] as? String) ?: "",
                        criadoPor = (despesaData["criadoPor"] as? String) ?: (despesaData["criado_por"] as? String) ?: "",
                        cicloId = (despesaData["cicloId"] as? Number)?.toLong() ?: (despesaData["ciclo_id"] as? Number)?.toLong(),
                        origemLancamento = (despesaData["origemLancamento"] as? String) ?: (despesaData["origem_lancamento"] as? String) ?: "ROTA",
                        cicloAno = (despesaData["cicloAno"] as? Number)?.toInt() ?: (despesaData["ciclo_ano"] as? Number)?.toInt(),
                        cicloNumero = (despesaData["cicloNumero"] as? Number)?.toInt() ?: (despesaData["ciclo_numero"] as? Number)?.toInt(),
                        fotoComprovante = (despesaData["fotoComprovante"] as? String) ?: (despesaData["foto_comprovante"] as? String),
                        dataFotoComprovante = converterTimestampParaDate(despesaData["dataFotoComprovante"]) ?: converterTimestampParaDate(despesaData["data_foto_comprovante"]),
                        veiculoId = (despesaData["veiculoId"] as? Number)?.toLong() ?: (despesaData["veiculo_id"] as? Number)?.toLong(),
                        kmRodado = (despesaData["kmRodado"] as? Number)?.toLong() ?: (despesaData["km_rodado"] as? Number)?.toLong(),
                        litrosAbastecidos = (despesaData["litrosAbastecidos"] as? Number)?.toDouble() ?: (despesaData["litros_abastecidos"] as? Number)?.toDouble()
                    )
                    
                    if (!shouldSyncRouteData(despesaFirestore.rotaId, allowUnknown = false)) {
                        skipCount++
                        return@forEach
                    }
                    
                    if (despesaFirestore.descricao.isBlank() || despesaFirestore.rotaId == 0L) {
                        Log.w(TAG, "⚠️ Despesa ${doc.id} com dados inválidos - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val despesaLocal = appRepository.obterDespesaPorId(despesaId)
                    
                    val serverTimestamp = (despesaData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (despesaData["syncTimestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: dataHora.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                    
                    val localTimestamp = despesaLocal?.dataHora?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000) ?: 0L
                    
                    when {
                        despesaLocal == null -> {
                            appRepository.inserirDespesa(despesaFirestore)
                            syncCount++
                            Log.d(TAG, "✅ Despesa inserida: ID=$despesaId, Descrição=${despesaFirestore.descricao}, CicloId=${despesaFirestore.cicloId}")
                        }
                        serverTimestamp > (localTimestamp + 1000) -> {
                            appRepository.atualizarDespesa(despesaFirestore)
                            syncCount++
                            Log.d(TAG, "✅ Despesa atualizada: ID=$despesaId (servidor: $serverTimestamp, local: $localTimestamp)")
                        }
                        else -> {
                            skipCount++
                            Log.d(TAG, "⏭️ Despesa local mais recente ou igual, mantendo: ID=$despesaId (servidor: $serverTimestamp, local: $localTimestamp)")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao sincronizar despesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Despesas (COMPLETO) concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de despesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Contratos: Sincroniza contratos do Firestore para o Room
     * Importante: Sincronizar também Aditivos e Assinaturas relacionados
     */
    private suspend fun pullContratos(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CONTRATOS
        
        return try {
            Log.d(TAG, "Iniciando pull de contratos...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_CONTRATOS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullContratosIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.buscarTodosContratos().first().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há contratos locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 contratos mas há $localCount locais - executando pull COMPLETO como validação")
                    return pullContratosComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de contratos falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de contratos - usando método COMPLETO")
            }
            
            pullContratosComplete(collectionRef, entityType, startTime, timestampOverride)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de contratos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullContratosComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de contratos - documentos recebidos: ${snapshot.documents.size}")
            
            val (syncCount, skippedCount, errorCount) = processContratosDocuments(snapshot.documents)
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null,
                timestampOverride = timestampOverride
            )
            Log.d(TAG, "✅ Pull de contratos concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull completo de contratos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullContratosIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ CORREÇÃO CRÍTICA: Estratégia híbrida para garantir que contratos não desapareçam
            // 1. Tentar buscar apenas contratos modificados recentemente (otimização)
            // 2. Se retornar 0 mas houver contratos locais, buscar TODOS para garantir sincronização completa
            
            // ✅ CORREÇÃO: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todosContratos = appRepository.buscarTodosContratos().first()
            val contratosCache = todosContratos.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de contratos carregado: ${contratosCache.size} contratos locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalContratos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todos os contratos: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há contratos locais, buscar TODOS
            val allContratos = if (incrementalContratos.isEmpty() && contratosCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 contratos mas há ${contratosCache.size} locais - buscando TODOS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todos os contratos: ${e.message}")
                    return null
                }
            } else {
                incrementalContratos
            }
            
            Log.d(TAG, "📥 Contratos - incremental: ${allContratos.size} documentos encontrados (antes do filtro de rota)")
            
            var syncCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            allContratos.forEach { doc ->
                try {
                    val contratoData = doc.data ?: run {
                        skippedCount++
                        return@forEach
                    }
                    val contratoId = doc.id.toLongOrNull() ?: run {
                        skippedCount++
                        return@forEach
                    }
                    
                    val contratoJson = gson.toJson(contratoData)
                    val contratoFirestore = gson.fromJson(contratoJson, ContratoLocacao::class.java)?.copy(id = contratoId)
                        ?: run {
                            skippedCount++
                            return@forEach
                        }
                    
                    // Verificar se deve sincronizar baseado na rota do cliente
                    if (!shouldSyncRouteData(null, clienteId = contratoFirestore.clienteId, allowUnknown = false)) {
                        skippedCount++
                        return@forEach
                    }
                    
                    val contratoLocal = contratosCache[contratoId]
                    val serverTimestamp = (contratoData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: contratoFirestore.dataAtualizacao.time
                    val localTimestamp = contratoLocal?.dataAtualizacao?.time ?: 0L
                    
                    // ✅ CORREÇÃO: Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = contratoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (contratoLocal == null) {
                            appRepository.inserirContrato(contratoFirestore)
                        } else {
                            appRepository.atualizarContrato(contratoFirestore)
                        }
                        pullAditivosContrato(contratoId)
                        syncCount++
                    } else {
                        skippedCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao sincronizar contrato ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            Log.d(TAG, "✅ Pull INCREMENTAL de contratos: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull incremental de contratos: ${e.message}", e)
            null
        }
    }

    private suspend fun processContratosDocuments(
        documents: List<DocumentSnapshot>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skipCount = 0
        var errorCount = 0
        
        documents.forEach { doc ->
            try {
                val contratoData = doc.data ?: run {
                    skipCount++
                    return@forEach
                }
                val contratoId = doc.id.toLongOrNull()
                if (contratoId == null) {
                    skipCount++
                    return@forEach
                }
                    
                    val contratoJson = gson.toJson(contratoData)
                val contratoFirestore = gson.fromJson(contratoJson, ContratoLocacao::class.java)?.copy(id = contratoId)
                    ?: run {
                        skipCount++
                        return@forEach
                    }
                
                if (!shouldSyncRouteData(null, clienteId = contratoFirestore.clienteId, allowUnknown = false)) {
                    skipCount++
                    return@forEach
                }
                    
                    val contratoLocal = appRepository.buscarContratoPorId(contratoId)
                    val serverTimestamp = (contratoData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: contratoFirestore.dataAtualizacao.time
                    val localTimestamp = contratoLocal?.dataAtualizacao?.time ?: 0L
                    
                val shouldSync = contratoLocal == null || serverTimestamp > localTimestamp
                if (shouldSync) {
                    if (contratoLocal == null) {
                            appRepository.inserirContrato(contratoFirestore)
                    } else {
                            appRepository.atualizarContrato(contratoFirestore)
                    }
                            pullAditivosContrato(contratoId)
                    syncCount++
                } else {
                    skipCount++
                    }
                } catch (e: Exception) {
                errorCount++
                    Log.e(TAG, "Erro ao sincronizar contrato ${doc.id}: ${e.message}", e)
                }
            }
            
        return Triple(syncCount, skipCount, errorCount)
    }
    
    /**
     * Pull Aditivos: Sincroniza aditivos de contrato relacionados
     */
    private suspend fun pullAditivosContrato(contratoId: Long) {
        try {
            val collectionRef = getCollectionReference(firestore, COLLECTION_ADITIVOS)
            val snapshot = collectionRef
                .whereEqualTo("contratoId", contratoId)
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                try {
                    val aditivoData = doc.data ?: return@forEach
                    val aditivoJson = gson.toJson(aditivoData)
                    val aditivo = gson.fromJson(aditivoJson, AditivoContrato::class.java)
                        ?: return@forEach
                    
                    appRepository.inserirAditivo(aditivo)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar aditivo ${doc.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de aditivos para contrato $contratoId: ${e.message}", e)
        }
    }
    
    // ==================== PULL HANDLERS - ENTIDADES FALTANTES ====================
    
    /**
     * Pull Categorias Despesa: Sincroniza categorias de despesa do Firestore para o Room
     */
    private suspend fun pullCategoriasDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CATEGORIAS_DESPESA
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de categorias despesa...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_CATEGORIAS_DESPESA)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullCategoriasDespesaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de categorias falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de categorias - usando método COMPLETO")
            }
            
            pullCategoriasDespesaComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de categorias despesa: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullCategoriasDespesaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de categorias - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val categoriasCache = mutableMapOf<Long, CategoriaDespesa>()
            val (syncCount, skippedCount, errorCount) = processCategoriasDespesaDocuments(snapshot.documents, categoriasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de categorias concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de categorias: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullCategoriasDespesaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao criar query incremental para categorias: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Categorias - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val categoriasCache = mutableMapOf<Long, CategoriaDespesa>()
            val (syncCount, skippedCount, errorCount) = processCategoriasDespesaDocuments(documents, categoriasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de categorias: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de categorias: ${e.message}", e)
            null
        }
    }

    private suspend fun processCategoriasDespesaDocuments(
        documents: List<DocumentSnapshot>,
        categoriasCache: MutableMap<Long, CategoriaDespesa>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processCategoriaDespesaDocument(doc, categoriasCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processCategoriaDespesaDocument(
        doc: DocumentSnapshot,
        categoriasCache: MutableMap<Long, CategoriaDespesa>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
            val categoriaId = (data["roomId"] as? Number)?.toLong()
                ?: (data["id"] as? Number)?.toLong()
                ?: doc.id.toLongOrNull()
                ?: return ProcessResult.Skipped
                    
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    val dataAtualizacao = converterTimestampParaDate(data["dataAtualizacao"])
                        ?: converterTimestampParaDate(data["data_atualizacao"])
                        ?: converterTimestampParaDate(data["lastModified"]) ?: Date()
                    
                    val categoria = CategoriaDespesa(
                        id = categoriaId,
                        nome = data["nome"] as? String ?: "Sem nome",
                        descricao = data["descricao"] as? String ?: "",
                        ativa = data["ativa"] as? Boolean ?: true,
                        dataCriacao = dataCriacao,
                        dataAtualizacao = dataAtualizacao,
                        criadoPor = data["criadoPor"] as? String ?: data["criado_por"] as? String ?: ""
                    )
                    
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                ?: dataAtualizacao.time
            val localCategoria = categoriasCache[categoriaId] ?: run {
                val fetched = runCatching { appRepository.buscarCategoriaPorId(categoriaId) }.getOrNull()
                if (fetched != null) categoriasCache[categoriaId] = fetched
                fetched
            }
            val localTimestamp = localCategoria?.dataAtualizacao?.time ?: 0L
            
            return if (localCategoria == null || serverTimestamp > localTimestamp) {
                    appRepository.criarCategoria(categoria)
                categoriasCache[categoriaId] = categoria
                ProcessResult.Synced
            } else {
                ProcessResult.Skipped
            }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar categoria despesa ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Tipos Despesa: Sincroniza tipos de despesa do Firestore para o Room
     */
    private suspend fun pullTiposDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_TIPOS_DESPESA
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de tipos despesa...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_TIPOS_DESPESA)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullTiposDespesaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de tipos falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de tipos - usando método COMPLETO")
            }
            
            pullTiposDespesaComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de tipos despesa: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullTiposDespesaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de tipos - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val tiposCache = mutableMapOf<Long, TipoDespesa>()
            val (syncCount, skippedCount, errorCount) = processTiposDespesaDocuments(snapshot.documents, tiposCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de tipos concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de tipos despesa: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullTiposDespesaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao criar query incremental para tipos: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Tipos - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val tiposCache = mutableMapOf<Long, TipoDespesa>()
            val (syncCount, skippedCount, errorCount) = processTiposDespesaDocuments(documents, tiposCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de tipos: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de tipos despesa: ${e.message}", e)
            null
        }
    }

    private suspend fun processTiposDespesaDocuments(
        documents: List<DocumentSnapshot>,
        tiposCache: MutableMap<Long, TipoDespesa>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processTipoDespesaDocument(doc, tiposCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processTipoDespesaDocument(
        doc: DocumentSnapshot,
        tiposCache: MutableMap<Long, TipoDespesa>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
            val tipoId = (data["roomId"] as? Number)?.toLong()
                ?: (data["id"] as? Number)?.toLong()
                ?: doc.id.toLongOrNull()
                ?: return ProcessResult.Skipped
                    val categoriaId = (data["categoriaId"] as? Number)?.toLong()
                ?: (data["categoria_id"] as? Number)?.toLong()
                ?: return ProcessResult.Skipped
                    
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    val dataAtualizacao = converterTimestampParaDate(data["dataAtualizacao"])
                        ?: converterTimestampParaDate(data["data_atualizacao"])
                        ?: converterTimestampParaDate(data["lastModified"]) ?: Date()
                    
                    val tipo = TipoDespesa(
                        id = tipoId,
                        categoriaId = categoriaId,
                        nome = data["nome"] as? String ?: "Sem nome",
                        descricao = data["descricao"] as? String ?: "",
                        ativo = data["ativo"] as? Boolean ?: true,
                        dataCriacao = dataCriacao,
                        dataAtualizacao = dataAtualizacao,
                        criadoPor = data["criadoPor"] as? String ?: data["criado_por"] as? String ?: ""
                    )
                    
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                ?: dataAtualizacao.time
            val localTipo = tiposCache[tipoId] ?: run {
                val fetched = runCatching { appRepository.buscarTipoPorId(tipoId) }.getOrNull()
                if (fetched != null) tiposCache[tipoId] = fetched
                fetched
            }
            val localTimestamp = localTipo?.dataAtualizacao?.time ?: 0L
            
            return if (localTipo == null || serverTimestamp > localTimestamp) {
                    appRepository.criarTipo(tipo)
                tiposCache[tipoId] = tipo
                ProcessResult.Synced
            } else {
                ProcessResult.Skipped
            }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar tipo despesa ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Metas: Sincroniza metas do Firestore para o Room
     */
    private suspend fun pullMetas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_METAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de metas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_METAS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullMetasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de metas falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de metas - usando método COMPLETO")
            }
            
            pullMetasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de metas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullMetasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "📥 Pull COMPLETO de metas - documentos recebidos: ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val metasCache = mutableMapOf<Long, Meta>()
            val (syncCount, skippedCount, errorCount) = processMetasDocuments(documents, metasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de metas concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de metas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullMetasIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao executar query incremental para metas: ${e.message}")
                return null
            }
            
            Log.d(TAG, "📥 Metas - incremental retornou ${documents.size} documentos (após filtro de rota)")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val metasCache = mutableMapOf<Long, Meta>()
            val (syncCount, skippedCount, errorCount) = processMetasDocuments(documents, metasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de metas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de metas: ${e.message}", e)
            null
        }
    }

    private suspend fun processMetasDocuments(
        documents: List<DocumentSnapshot>,
        metasCache: MutableMap<Long, Meta>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processMetaDocument(doc, metasCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processMetaDocument(
        doc: DocumentSnapshot,
        metasCache: MutableMap<Long, Meta>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
            val metaId = (data["roomId"] as? Number)?.toLong()
                ?: (data["id"] as? Number)?.toLong()
                ?: doc.id.toLongOrNull()
                ?: return ProcessResult.Skipped
                    val rotaId = (data["rotaId"] as? Number)?.toLong()
                ?: (data["rota_id"] as? Number)?.toLong()
                ?: return ProcessResult.Skipped
                    val cicloId = (data["cicloId"] as? Number)?.toLong()
                ?: (data["ciclo_id"] as? Number)?.toLong()
                ?: 0L
            
            if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                return ProcessResult.Skipped
            }
                    
                    val dataInicio = converterTimestampParaDate(data["dataInicio"])
                        ?: converterTimestampParaDate(data["data_inicio"]) ?: Date()
                    val dataFim = converterTimestampParaDate(data["dataFim"])
                        ?: converterTimestampParaDate(data["data_fim"]) ?: Date()
                    
                    val meta = Meta(
                        id = metaId,
                        nome = data["nome"] as? String ?: "Sem nome",
                        tipo = data["tipo"] as? String ?: "",
                        valorObjetivo = (data["valorObjetivo"] as? Number)?.toDouble()
                            ?: (data["valor_objetivo"] as? Number)?.toDouble() ?: 0.0,
                        valorAtual = (data["valorAtual"] as? Number)?.toDouble()
                            ?: (data["valor_atual"] as? Number)?.toDouble() ?: 0.0,
                        dataInicio = dataInicio,
                        dataFim = dataFim,
                        rotaId = rotaId,
                        cicloId = cicloId
                    )
                    
                    if (meta.nome.isBlank() || meta.rotaId == 0L) {
                return ProcessResult.Skipped
            }
            
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                        ?: (data["syncTimestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: dataInicio.time
            val metaLocal = metasCache[metaId] ?: run {
                val fetched = runCatching { appRepository.obterMetaPorId(metaId) }.getOrNull()
                if (fetched != null) metasCache[metaId] = fetched
                fetched
            }
                    val localTimestamp = metaLocal?.dataInicio?.time ?: 0L
                    
            return if (metaLocal == null || serverTimestamp > (localTimestamp + 1000)) {
                if (metaLocal == null) {
                            appRepository.inserirMeta(meta)
                } else {
                            appRepository.atualizarMeta(meta)
                }
                metasCache[metaId] = meta
                ProcessResult.Synced
            } else {
                ProcessResult.Skipped
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar meta ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Colaborador Rotas: Sincroniza vinculações colaborador-rota do Firestore para o Room
     */
    private suspend fun pullColaboradorRotas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_COLABORADOR_ROTA
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de colaborador rotas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_COLABORADOR_ROTA)
            
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL de colaborador rotas (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullColaboradorRotasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de colaborador rotas falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de colaborador rotas - usando método COMPLETO")
            }
            
            pullColaboradorRotasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de colaborador rotas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullColaboradorRotasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "📥 Pull COMPLETO de colaborador rotas - documentos recebidos: ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val vinculosCache = appRepository.obterTodosColaboradorRotas()
                .associateBy { colaboradorRotaKey(it.colaboradorId, it.rotaId) }
                .toMutableMap()
            val (syncCount, skippedCount, errorCount) = processColaboradorRotasDocuments(documents, vinculosCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de colaborador rotas concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de colaborador rotas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullColaboradorRotasIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao executar query incremental para colaborador rotas: ${e.message}")
                return null
            }
            Log.d(TAG, "📥 Colaborador rotas - incremental retornou ${documents.size} documentos (após filtro de rota)")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val vinculosCache = appRepository.obterTodosColaboradorRotas()
                .associateBy { colaboradorRotaKey(it.colaboradorId, it.rotaId) }
                .toMutableMap()
            val (syncCount, skippedCount, errorCount) = processColaboradorRotasDocuments(documents, vinculosCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de colaborador rotas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de colaborador rotas: ${e.message}", e)
            null
        }
    }

    private suspend fun processColaboradorRotasDocuments(
        documents: List<DocumentSnapshot>,
        vinculosCache: MutableMap<String, ColaboradorRota>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processColaboradorRotaDocument(doc, vinculosCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processColaboradorRotaDocument(
        doc: DocumentSnapshot,
        vinculosCache: MutableMap<String, ColaboradorRota>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
                    val colaboradorId = (data["colaboradorId"] as? Number)?.toLong()
                ?: (data["colaborador_id"] as? Number)?.toLong()
                ?: doc.id.substringBefore("_").toLongOrNull()
                ?: return ProcessResult.Skipped
                    val rotaId = (data["rotaId"] as? Number)?.toLong()
                ?: (data["rota_id"] as? Number)?.toLong()
                ?: doc.id.substringAfterLast("_").toLongOrNull()
                ?: return ProcessResult.Skipped
            
            if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                return ProcessResult.Skipped
                    }
                    
                    val dataVinculacao = converterTimestampParaDate(data["dataVinculacao"])
                        ?: converterTimestampParaDate(data["data_vinculacao"]) ?: Date()
            val responsavelPrincipal = data["responsavelPrincipal"] as? Boolean
                ?: data["responsavel_principal"] as? Boolean ?: false
                    
                    val colaboradorRota = ColaboradorRota(
                        colaboradorId = colaboradorId,
                        rotaId = rotaId,
                responsavelPrincipal = responsavelPrincipal,
                        dataVinculacao = dataVinculacao
                    )
                    
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                ?: dataVinculacao.time
            val key = colaboradorRotaKey(colaboradorId, rotaId)
            val local = vinculosCache[key]
            val localTimestamp = local?.dataVinculacao?.time ?: 0L
            
            return if (local == null || serverTimestamp > localTimestamp) {
                    appRepository.inserirColaboradorRota(colaboradorRota)
                vinculosCache[key] = colaboradorRota
                ProcessResult.Synced
            } else {
                ProcessResult.Skipped
            }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar colaborador rota ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }

    private fun colaboradorRotaKey(colaboradorId: Long, rotaId: Long): String =
        "${colaboradorId}_${rotaId}"
    
    /**
     * Pull Aditivo Mesas: Sincroniza vinculações aditivo-mesa do Firestore para o Room
     */
    private suspend fun pullAditivoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ADITIVO_MESAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de aditivo mesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ADITIVO_MESAS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullAditivoMesasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de aditivo mesas falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de aditivo mesas - usando método COMPLETO")
            }
            
            pullAditivoMesasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de aditivo mesas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullAditivoMesasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de aditivo mesas - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val aditivoMesasCache = appRepository.obterTodosAditivoMesas().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processAditivoMesasDocuments(snapshot.documents, aditivoMesasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de aditivo mesas concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de aditivo mesas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullAditivoMesasIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao criar query incremental para aditivo mesas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Aditivo mesas - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val aditivoMesasCache = appRepository.obterTodosAditivoMesas().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processAditivoMesasDocuments(documents, aditivoMesasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de aditivo mesas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de aditivo mesas: ${e.message}", e)
            null
        }
    }

    private suspend fun processAditivoMesasDocuments(
        documents: List<DocumentSnapshot>,
        aditivoMesasCache: MutableMap<Long, AditivoMesa>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processAditivoMesaDocument(doc, aditivoMesasCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processAditivoMesaDocument(
        doc: DocumentSnapshot,
        aditivoMesasCache: MutableMap<Long, AditivoMesa>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
            val aditivoMesaId = (data["roomId"] as? Number)?.toLong()
                ?: (data["id"] as? Number)?.toLong()
                ?: doc.id.toLongOrNull()
                ?: return ProcessResult.Skipped
                    val aditivoId = (data["aditivoId"] as? Number)?.toLong()
                ?: (data["aditivo_id"] as? Number)?.toLong() ?: return ProcessResult.Skipped
                    val mesaId = (data["mesaId"] as? Number)?.toLong()
                ?: (data["mesa_id"] as? Number)?.toLong() ?: return ProcessResult.Skipped
            
            val rotaId = getMesaRouteId(mesaId)
            if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                return ProcessResult.Skipped
                    }
                    
                    val aditivoMesa = AditivoMesa(
                        id = aditivoMesaId,
                        aditivoId = aditivoId,
                        mesaId = mesaId,
                        tipoEquipamento = data["tipoEquipamento"] as? String
                            ?: data["tipo_equipamento"] as? String ?: "",
                        numeroSerie = data["numeroSerie"] as? String
                            ?: data["numero_serie"] as? String ?: ""
                    )
                    
            val existing = aditivoMesasCache[aditivoMesaId]
            return if (existing == null) {
                    appRepository.inserirAditivoMesas(listOf(aditivoMesa))
                aditivoMesasCache[aditivoMesaId] = aditivoMesa
                ProcessResult.Synced
            } else {
                appRepository.inserirAditivoMesas(listOf(aditivoMesa))
                aditivoMesasCache[aditivoMesaId] = aditivoMesa
                ProcessResult.Synced
            }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar aditivo mesa ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Contrato Mesas: Sincroniza vinculações contrato-mesa do Firestore para o Room
     */
    private suspend fun pullContratoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CONTRATO_MESAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de contrato mesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_CONTRATO_MESAS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullContratoMesasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de contrato mesas falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de contrato mesas - usando método COMPLETO")
            }
            
            pullContratoMesasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de contrato mesas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullContratoMesasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de contrato mesas - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val contratoMesasCache = appRepository.obterTodosContratoMesas().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processContratoMesasDocuments(snapshot.documents, contratoMesasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de contrato mesas concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de contrato mesas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullContratoMesasIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao criar query incremental para contrato mesas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Contrato mesas - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val contratoMesasCache = appRepository.obterTodosContratoMesas().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processContratoMesasDocuments(documents, contratoMesasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de contrato mesas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de contrato mesas: ${e.message}", e)
            null
        }
    }

    private suspend fun processContratoMesasDocuments(
        documents: List<DocumentSnapshot>,
        contratoMesasCache: MutableMap<Long, ContratoMesa>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processContratoMesaDocument(doc, contratoMesasCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processContratoMesaDocument(
        doc: DocumentSnapshot,
        contratoMesasCache: MutableMap<Long, ContratoMesa>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
            val contratoMesaId = (data["roomId"] as? Number)?.toLong()
                ?: (data["id"] as? Number)?.toLong()
                ?: doc.id.toLongOrNull()
                ?: return ProcessResult.Skipped
                    val contratoId = (data["contratoId"] as? Number)?.toLong()
                ?: (data["contrato_id"] as? Number)?.toLong() ?: return ProcessResult.Skipped
                    val mesaId = (data["mesaId"] as? Number)?.toLong()
                ?: (data["mesa_id"] as? Number)?.toLong() ?: return ProcessResult.Skipped
            
            val rotaId = getMesaRouteId(mesaId)
            if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                return ProcessResult.Skipped
                    }
                    
                    val contratoMesa = ContratoMesa(
                        id = contratoMesaId,
                        contratoId = contratoId,
                        mesaId = mesaId,
                        tipoEquipamento = data["tipoEquipamento"] as? String
                            ?: data["tipo_equipamento"] as? String ?: "",
                        numeroSerie = data["numeroSerie"] as? String
                            ?: data["numero_serie"] as? String ?: "",
                        valorFicha = (data["valorFicha"] as? Number)?.toDouble()
                            ?: (data["valor_ficha"] as? Number)?.toDouble(),
                        valorFixo = (data["valorFixo"] as? Number)?.toDouble()
                            ?: (data["valor_fixo"] as? Number)?.toDouble()
                    )
                    
            val existing = contratoMesasCache[contratoMesaId]
            return if (existing == null) {
                    appRepository.inserirContratoMesa(contratoMesa)
                contratoMesasCache[contratoMesaId] = contratoMesa
                ProcessResult.Synced
            } else {
                appRepository.inserirContratoMesa(contratoMesa)
                contratoMesasCache[contratoMesaId] = contratoMesa
                ProcessResult.Synced
            }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar contrato mesa ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Assinaturas Representante Legal: Sincroniza assinaturas do Firestore para o Room
     */
    private suspend fun pullAssinaturasRepresentanteLegal(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ASSINATURAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de assinaturas representante legal...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ASSINATURAS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullAssinaturasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de assinaturas falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de assinaturas - usando método COMPLETO")
            }
            
            pullAssinaturasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de assinaturas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullAssinaturasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de assinaturas - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val assinaturasCache = appRepository.obterTodasAssinaturasRepresentanteLegal().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processAssinaturasDocuments(snapshot.documents, assinaturasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de assinaturas concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de assinaturas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullAssinaturasIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao criar query incremental para assinaturas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Assinaturas - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val assinaturasCache = appRepository.obterTodasAssinaturasRepresentanteLegal().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processAssinaturasDocuments(documents, assinaturasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de assinaturas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de assinaturas: ${e.message}", e)
            null
        }
    }

    private suspend fun processAssinaturasDocuments(
        documents: List<DocumentSnapshot>,
        assinaturasCache: MutableMap<Long, AssinaturaRepresentanteLegal>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processAssinaturaDocument(doc, assinaturasCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processAssinaturaDocument(
        doc: DocumentSnapshot,
        assinaturasCache: MutableMap<Long, AssinaturaRepresentanteLegal>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
            val assinaturaId = (data["roomId"] as? Number)?.toLong()
                ?: (data["id"] as? Number)?.toLong()
                ?: doc.id.toLongOrNull()
                ?: return ProcessResult.Skipped
                    
                    val timestampCriacao = (data["timestampCriacao"] as? Number)?.toLong()
                        ?: (data["timestamp_criacao"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
            val dataProcuracao = converterTimestampParaDate(data["dataProcuração"])
                        ?: converterTimestampParaDate(data["data_procuracao"]) ?: Date()
            val validadeProc = converterTimestampParaDate(data["validadeProcuração"])
                        ?: converterTimestampParaDate(data["validade_procuracao"])
                    val ultimoUso = converterTimestampParaDate(data["ultimoUso"])
                        ?: converterTimestampParaDate(data["ultimo_uso"])
                    val dataValidacao = converterTimestampParaDate(data["dataValidacao"])
                        ?: converterTimestampParaDate(data["data_validacao"])
                    
                    val assinatura = AssinaturaRepresentanteLegal(
                        id = assinaturaId,
                        nomeRepresentante = data["nomeRepresentante"] as? String
                            ?: data["nome_representante"] as? String ?: "",
                        cpfRepresentante = data["cpfRepresentante"] as? String
                            ?: data["cpf_representante"] as? String ?: "",
                        cargoRepresentante = data["cargoRepresentante"] as? String
                            ?: data["cargo_representante"] as? String ?: "",
                        assinaturaBase64 = data["assinaturaBase64"] as? String
                            ?: data["assinatura_base64"] as? String ?: "",
                        timestampCriacao = timestampCriacao,
                        deviceId = data["deviceId"] as? String ?: data["device_id"] as? String ?: "",
                        hashIntegridade = data["hashIntegridade"] as? String
                            ?: data["hash_integridade"] as? String ?: "",
                        versaoSistema = data["versaoSistema"] as? String
                            ?: data["versao_sistema"] as? String ?: "",
                        dataCriacao = dataCriacao,
                        criadoPor = data["criadoPor"] as? String ?: data["criado_por"] as? String ?: "",
                        ativo = data["ativo"] as? Boolean ?: true,
                        numeroProcuração = data["numeroProcuração"] as? String
                            ?: data["numero_procuracao"] as? String ?: "",
                dataProcuração = dataProcuracao,
                        poderesDelegados = data["poderesDelegados"] as? String
                            ?: data["poderes_delegados"] as? String ?: "",
                validadeProcuração = validadeProc,
                        totalUsos = (data["totalUsos"] as? Number)?.toInt()
                            ?: (data["total_usos"] as? Number)?.toInt() ?: 0,
                        ultimoUso = ultimoUso,
                        contratosAssinados = data["contratosAssinados"] as? String
                            ?: data["contratos_assinados"] as? String ?: "",
                        validadaJuridicamente = data["validadaJuridicamente"] as? Boolean
                            ?: data["validada_juridicamente"] as? Boolean ?: false,
                        dataValidacao = dataValidacao,
                        validadoPor = data["validadoPor"] as? String
                            ?: data["validado_por"] as? String
                    )
                    
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                ?: dataCriacao.time
            val localAssinatura = assinaturasCache[assinaturaId]
            val localTimestamp = localAssinatura?.dataCriacao?.time ?: 0L
            
            return if (localAssinatura == null || serverTimestamp > localTimestamp) {
                    appRepository.inserirAssinaturaRepresentanteLegal(assinatura)
                assinaturasCache[assinaturaId] = assinatura
                ProcessResult.Synced
            } else {
                ProcessResult.Skipped
            }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar assinatura ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Logs Auditoria: Sincroniza logs de auditoria do Firestore para o Room
     */
    private suspend fun pullLogsAuditoria(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_LOGS_AUDITORIA
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de logs auditoria...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_LOGS_AUDITORIA)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullLogsAuditoriaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de logs auditoria falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de logs auditoria - usando método COMPLETO")
            }
            
            pullLogsAuditoriaComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de logs auditoria: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullLogsAuditoriaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de logs auditoria - documentos recebidos: ${snapshot.documents.size}")
            
            val (syncCount, skippedCount, errorCount) = processLogsAuditoriaDocuments(snapshot.documents)
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            Log.d(TAG, "✅ Pull de logs auditoria concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de logs auditoria: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullLogsAuditoriaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Logs auditoria - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val (syncCount, skippedCount, errorCount) = processLogsAuditoriaDocuments(documents)
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            Log.d(TAG, "✅ Pull INCREMENTAL de logs auditoria: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de logs auditoria: ${e.message}", e)
            null
        }
    }

    private suspend fun processLogsAuditoriaDocuments(
        documents: List<DocumentSnapshot>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            try {
                val data = doc.data ?: run {
                    skipCount++
                    return@forEach
                }
                val logId = (data["roomId"] as? Number)?.toLong()
                    ?: (data["id"] as? Number)?.toLong()
                    ?: doc.id.toLongOrNull()
                    ?: run {
                        skipCount++
                        return@forEach
                    }
                    
                    val timestamp = (data["timestamp"] as? Number)?.toLong()
                        ?: (data["timestamp"] as? com.google.firebase.Timestamp)?.seconds?.times(1000)?.plus(
                            (data["timestamp"] as? com.google.firebase.Timestamp)?.nanoseconds?.div(1000000) ?: 0
                        ) ?: System.currentTimeMillis()
                    val dataOperacao = converterTimestampParaDate(data["dataOperacao"])
                        ?: converterTimestampParaDate(data["data_operacao"]) ?: Date(timestamp)
                    val dataValidacao = converterTimestampParaDate(data["dataValidacao"])
                        ?: converterTimestampParaDate(data["data_validacao"])
                    
                    val log = LogAuditoriaAssinatura(
                        id = logId,
                        tipoOperacao = data["tipoOperacao"] as? String
                            ?: data["tipo_operacao"] as? String ?: "",
                        idAssinatura = (data["idAssinatura"] as? Number)?.toLong()
                            ?: (data["id_assinatura"] as? Number)?.toLong() ?: 0L,
                        idContrato = (data["idContrato"] as? Number)?.toLong()
                            ?: (data["id_contrato"] as? Number)?.toLong(),
                        idAditivo = (data["idAditivo"] as? Number)?.toLong()
                            ?: (data["id_aditivo"] as? Number)?.toLong(),
                        usuarioExecutou = data["usuarioExecutou"] as? String
                            ?: data["usuario_executou"] as? String ?: "",
                        cpfUsuario = data["cpfUsuario"] as? String
                            ?: data["cpf_usuario"] as? String ?: "",
                        cargoUsuario = data["cargoUsuario"] as? String
                            ?: data["cargo_usuario"] as? String ?: "",
                        timestamp = timestamp,
                        deviceId = data["deviceId"] as? String ?: data["device_id"] as? String ?: "",
                        versaoApp = data["versaoApp"] as? String
                            ?: data["versao_app"] as? String ?: "",
                        hashDocumento = data["hashDocumento"] as? String
                            ?: data["hash_documento"] as? String ?: "",
                        hashAssinatura = data["hashAssinatura"] as? String
                            ?: data["hash_assinatura"] as? String ?: "",
                        latitude = (data["latitude"] as? Number)?.toDouble(),
                        longitude = (data["longitude"] as? Number)?.toDouble(),
                        endereco = data["endereco"] as? String,
                        ipAddress = data["ipAddress"] as? String ?: data["ip_address"] as? String,
                        userAgent = data["userAgent"] as? String ?: data["user_agent"] as? String,
                        tipoDocumento = data["tipoDocumento"] as? String
                            ?: data["tipo_documento"] as? String ?: "",
                        numeroDocumento = data["numeroDocumento"] as? String
                            ?: data["numero_documento"] as? String ?: "",
                        valorContrato = (data["valorContrato"] as? Number)?.toDouble()
                            ?: (data["valor_contrato"] as? Number)?.toDouble(),
                        sucesso = data["sucesso"] as? Boolean ?: true,
                        mensagemErro = data["mensagemErro"] as? String
                            ?: data["mensagem_erro"] as? String,
                        dataOperacao = dataOperacao,
                        observacoes = data["observacoes"] as? String,
                    validadoJuridicamente = data["validadaJuridicamente"] as? Boolean
                        ?: data["validada_juridicamente"] as? Boolean ?: false,
                        dataValidacao = dataValidacao,
                        validadoPor = data["validadoPor"] as? String
                            ?: data["validado_por"] as? String
                    )
                    
                    appRepository.inserirLogAuditoriaAssinatura(log)
                    syncCount++
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar log auditoria ${doc.id}: ${e.message}", e)
                }
            }
            
        return Triple(syncCount, skipCount, errorCount)
    }
    
    // ==================== PUSH HANDLERS (LOCAL → SERVIDOR) ====================
    
    /**
     * ✅ REFATORADO (2025): Push Clientes com sincronização incremental
     * Envia apenas clientes modificados desde o último push
     * Segue melhores práticas Android 2025 para sincronização incremental
     */
    private suspend fun pushClientes(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CLIENTES
        
        return try {
            Log.d(TAG, "📤 Iniciando push INCREMENTAL de clientes...")
            
            // ✅ NOVO: Obter último timestamp de push para filtrar apenas modificados
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val clientesLocais = appRepository.obterTodosClientes().first()
            Log.d(TAG, "📊 Total de clientes locais encontrados: ${clientesLocais.size}")
            
            // ✅ NOVO: Filtrar apenas clientes modificados desde último push
            val clientesParaEnviar = if (canUseIncremental) {
                clientesLocais.filter { cliente ->
                    val clienteTimestamp = cliente.dataUltimaAtualizacao?.time ?: cliente.dataCadastro.time
                    clienteTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} clientes modificados desde ${Date(lastPushTimestamp)} (de ${clientesLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${clientesLocais.size} clientes")
                clientesLocais
            }
            
            if (clientesParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                Log.d(TAG, "✅ Nenhum cliente para enviar - push concluído")
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            clientesParaEnviar.forEach { cliente ->
                try {
                    // Converter Cliente para Map
                    val clienteMap = entityToMap(cliente)
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    clienteMap["roomId"] = cliente.id
                    clienteMap["id"] = cliente.id
                    
                    // ✅ CRÍTICO: Garantir que dataUltimaAtualizacao seja enviada
                    // Se não tiver timestamp, usar o atual
                    if (!clienteMap.containsKey("dataUltimaAtualizacao") && 
                        !clienteMap.containsKey("data_ultima_atualizacao")) {
                        clienteMap["dataUltimaAtualizacao"] = Date()
                        clienteMap["data_ultima_atualizacao"] = Date()
                    }
                    
                    // Adicionar metadados de sincronização
                    clienteMap["lastModified"] = FieldValue.serverTimestamp()
                    clienteMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    // ✅ CRÍTICO: Usar .set() para substituir completamente o documento
                    // Isso garante que os dados locais sejam preservados na nuvem
                    val collectionRef = getCollectionReference(firestore, COLLECTION_CLIENTES)
                    collectionRef
                        .document(cliente.id.toString())
                        .set(clienteMap)
                        .await()
                    
                    // ✅ CORRIGIDO: Ler o documento do Firestore para obter o timestamp real do servidor
                    // Isso evita race condition onde o timestamp local difere do timestamp do servidor
                    val docSnapshot = collectionRef
                        .document(cliente.id.toString())
                        .get()
                        .await()
                    
                    // Obter o timestamp do servidor (lastModified ou dataUltimaAtualizacao)
                    val serverTimestamp = converterTimestampParaDate(docSnapshot.data?.get("lastModified"))
                        ?: converterTimestampParaDate(docSnapshot.data?.get("dataUltimaAtualizacao"))
                        ?: converterTimestampParaDate(docSnapshot.data?.get("data_ultima_atualizacao"))
                        ?: Date() // Fallback para timestamp atual se não encontrar
                    
                    // ✅ CRÍTICO: Atualizar timestamp local com o timestamp do servidor
                    // Isso garante que local e servidor tenham o mesmo timestamp, evitando sobrescrita no pull
                    val clienteAtualizado = cliente.copy(dataUltimaAtualizacao = serverTimestamp)
                    appRepository.atualizarCliente(clienteAtualizado)
                    
                    syncCount++
                    bytesUploaded += clienteMap.toString().length.toLong() // Estimativa de bytes
                    Log.d(TAG, "✅ Cliente enviado para nuvem: ${cliente.nome} (ID: ${cliente.id}) - Timestamp local sincronizado com servidor: ${serverTimestamp.time}")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar cliente ${cliente.id} (${cliente.nome}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // ✅ NOVO: Salvar metadata de push após sincronização bem-sucedida
            savePushMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesUploaded = bytesUploaded,
                error = if (errorCount > 0) "$errorCount erros durante push" else null
            )
            
            Log.d(TAG, "✅ Push INCREMENTAL de clientes concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Rotas com sincronização incremental
     * Envia apenas rotas modificadas desde o último push
     */
    private suspend fun pushRotas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ROTAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de rotas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val rotasLocais = appRepository.obterTodasRotas().first()
            Log.d(TAG, "📥 Total de rotas locais encontradas: ${rotasLocais.size}")
            
            // Filtrar apenas rotas modificadas desde último push
            val rotasParaEnviar = if (canUseIncremental) {
                rotasLocais.filter { rota ->
                    rota.dataAtualizacao > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} rotas modificadas desde ${Date(lastPushTimestamp)} (de ${rotasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${rotasLocais.size} rotas")
                rotasLocais
            }
            
            if (rotasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                Log.d(TAG, "✅ Nenhuma rota para enviar - push concluído")
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            rotasParaEnviar.forEach { rota ->
                try {
                    Log.d(TAG, "📄 Processando rota: ID=${rota.id}, Nome=${rota.nome}")
                    
                    val rotaMap = entityToMap(rota)
                    Log.d(TAG, "   Mapa criado com ${rotaMap.size} campos")
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    // O pull espera encontrar roomId no documento do Firestore
                    rotaMap["roomId"] = rota.id
                    rotaMap["id"] = rota.id // Também incluir campo id para compatibilidade
                    
                    // Adicionar metadados de sincronização
                    rotaMap["lastModified"] = FieldValue.serverTimestamp()
                    rotaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = rota.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_ROTAS)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$EMPRESA_ID/entidades/${COLLECTION_ROTAS}/items, document=$documentId")
                    Log.d(TAG, "   Campos no mapa: ${rotaMap.keys}")
                    collectionRef
                        .document(documentId)
                        .set(rotaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += rotaMap.toString().length.toLong()
                    Log.d(TAG, "✅ Rota enviada com sucesso: ${rota.nome} (ID: ${rota.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar rota ${rota.id} (${rota.nome}): ${e.message}", e)
                    Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de rotas concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Mesas com sincronização incremental
     * Envia apenas mesas modificadas desde o último push
     */
    private suspend fun pushMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS
        
        return try {
            Log.d(TAG, "Iniciando push INCREMENTAL de mesas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val mesasLocais = appRepository.obterTodasMesas().first()
            
            // Filtrar apenas mesas modificadas (usar dataUltimaLeitura como proxy)
            val mesasParaEnviar = if (canUseIncremental) {
                mesasLocais.filter { mesa ->
                    val mesaTimestamp = mesa.dataUltimaLeitura?.time ?: mesa.dataInstalacao.time
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} mesas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${mesasLocais.size} mesas")
                mesasLocais
            }
            
            if (mesasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var bytesUploaded = 0L
            var errorCount = 0
            var maxServerTimestamp = 0L
            
            mesasParaEnviar.forEach { mesa ->
                try {
                    val mesaMap = entityToMap(mesa)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    mesaMap["roomId"] = mesa.id
                    mesaMap["id"] = mesa.id
                    mesaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    Log.d(TAG, "🔍 [DIAGNOSTICO] Enviando Mesa ${mesa.id}. lastModified definido como serverTimestamp()")
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS)
                    val docRef = collectionRef.document(mesa.id.toString())
                    
                    // 1. Escrever
                    docRef.set(mesaMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    if (serverTimestamp > maxServerTimestamp) {
                        maxServerTimestamp = serverTimestamp
                    }
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ Mesa ${mesa.id} exportada com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += mesaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar mesa ${mesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de mesas concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms. MaxServerTimestamp: ${Date(maxServerTimestamp)}")
            
            // Retornar o maior timestamp encontrado (ou 0 se nenhum)
            // Usamos um Result customizado ou passamos via Pair? 
            // Por enquanto, vamos manter a assinatura Result<Int> mas precisamos propagar esse timestamp.
            // VOU ALTERAR A ASSINATURA DEPOIS. Por enquanto, vou salvar o metadata aqui mesmo se for maior que o atual?
            // Não, o ideal é retornar. Mas para não quebrar tudo agora, vou salvar um metadado temporário ou apenas logar.
            // A estratégia correta é mudar a assinatura de syncPush para retornar Result<Long> ou Result<SyncResult>.
            
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Colaboradores com sincronização incremental
     */
    private suspend fun pushColaboradores(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_COLABORADORES
        
        return try {
            Log.d(TAG, "Iniciando push INCREMENTAL de colaboradores...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val colaboradoresLocais = appRepository.obterTodosColaboradores().first()
            
            val colaboradoresParaEnviar = if (canUseIncremental) {
                colaboradoresLocais.filter { colaborador ->
                    val colaboradorTimestamp = colaborador.dataUltimaAtualizacao?.time ?: System.currentTimeMillis()
                    colaboradorTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} colaboradores modificados desde ${Date(lastPushTimestamp)} (de ${colaboradoresLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${colaboradoresLocais.size} colaboradores")
                colaboradoresLocais
            }
            
            if (colaboradoresParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            colaboradoresParaEnviar.forEach { colaborador ->
                try {
                    val colaboradorMap = entityToMap(colaborador)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    colaboradorMap["roomId"] = colaborador.id
                    colaboradorMap["id"] = colaborador.id
                    colaboradorMap["lastModified"] = FieldValue.serverTimestamp()
                    colaboradorMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_COLABORADORES)
                    collectionRef
                        .document(colaborador.id.toString())
                        .set(colaboradorMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += colaboradorMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar colaborador ${colaborador.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de colaboradores concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de colaboradores: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Ciclos: Envia ciclos modificados do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Ciclos com sincronização incremental
     */
    private suspend fun pushCiclos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CICLOS
        
        return try {
            Log.d(TAG, "🔵 ===== INICIANDO PUSH DE CICLOS =====")
            Log.d(TAG, "Iniciando push INCREMENTAL de ciclos...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            // ✅ CORREÇÃO: Buscar TODOS os ciclos locais e mostrar detalhes
            val ciclosLocais = try {
                appRepository.obterTodosCiclos().first()
            } catch (e: Exception) {
                Log.w(TAG, "Método obterTodosCiclos não disponível, tentando alternativa...")
                emptyList<CicloAcertoEntity>()
            }
            
            Log.d(TAG, "   📊 Total de ciclos locais: ${ciclosLocais.size}")
            ciclosLocais.forEach { ciclo ->
                Log.d(TAG, "      - Ciclo ${ciclo.numeroCiclo}/${ciclo.ano}: status=${ciclo.status}, dataInicio=${ciclo.dataInicio}, dataAtualizacao=${ciclo.dataAtualizacao}")
            }
            
            val ciclosParaEnviar = if (canUseIncremental) {
                ciclosLocais.filter { ciclo ->
                    val cicloTimestamp = ciclo.dataAtualizacao.time
                    cicloTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} ciclos modificados desde ${Date(lastPushTimestamp)} (de ${ciclosLocais.size} total)")
                    it.forEach { ciclo ->
                        Log.d(TAG, "      → Enviando ciclo ${ciclo.numeroCiclo}/${ciclo.ano}")
                    }
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${ciclosLocais.size} ciclos")
                ciclosLocais
            }
            
            if (ciclosParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            ciclosParaEnviar.forEach { ciclo ->
                try {
                    val cicloMap = entityToMap(ciclo)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    cicloMap["roomId"] = ciclo.id
                    cicloMap["id"] = ciclo.id
                    cicloMap["lastModified"] = FieldValue.serverTimestamp()
                    cicloMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_CICLOS)
                    collectionRef
                        .document(ciclo.id.toString())
                        .set(cicloMap)
                        .await()
                    
                    // ✅ READ-YOUR-WRITES: Ler de volta para pegar o timestamp real do servidor
                    val snapshot = collectionRef.document(ciclo.id.toString()).get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: System.currentTimeMillis()
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ Ciclo ${ciclo.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += cicloMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar ciclo ${ciclo.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            Log.d(TAG, "✅ Push INCREMENTAL de ciclos concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Acertos com sincronização incremental
     * Envia apenas acertos modificados desde o último push
     * Importante: Enviar também AcertoMesa relacionados
     */
    private suspend fun pushAcertos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ACERTOS
        
        return try {
            Log.d(TAG, "Iniciando push INCREMENTAL de acertos...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val acertosLocais = appRepository.obterTodosAcertos().first()
            
            // Filtrar apenas acertos modificados (usar dataAcerto ou dataCriacao)
            val acertosParaEnviar = if (canUseIncremental) {
                acertosLocais.filter { acerto ->
                    val acertoTimestamp = acerto.dataAcerto.time
                    acertoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} acertos modificados desde ${Date(lastPushTimestamp)} (de ${acertosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${acertosLocais.size} acertos")
                acertosLocais
            }
            
            if (acertosParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            acertosParaEnviar.forEach { acerto ->
                try {
                    val acertoMap = entityToMap(acerto)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    acertoMap["roomId"] = acerto.id
                    acertoMap["id"] = acerto.id
                    acertoMap["lastModified"] = FieldValue.serverTimestamp()
                    acertoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_ACERTOS)
                    collectionRef
                        .document(acerto.id.toString())
                        .set(acertoMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += acertoMap.toString().length.toLong()
                    
                    // Enviar AcertoMesa relacionados
                    pushAcertoMesas(acerto.id)
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar acerto ${acerto.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de acertos concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de acertos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push AcertoMesas: Envia mesas de acerto relacionadas
     * ✅ CORREÇÃO: Faz upload de fotos locais para Firebase Storage antes de enviar
     */
    private suspend fun pushAcertoMesas(acertoId: Long) {
        try {
            val acertoMesas = appRepository.buscarAcertoMesasPorAcerto(acertoId) // Retorna List<AcertoMesa>
            
            acertoMesas.forEach { acertoMesa: AcertoMesa ->
                try {
                    var fotoParaEnviar = acertoMesa.fotoRelogioFinal
                    
                    // ✅ NOVO: Se houver foto local mas não for URL do Firebase, fazer upload
                    if (!fotoParaEnviar.isNullOrEmpty() && 
                        !firebaseImageUploader.isFirebaseStorageUrl(fotoParaEnviar)) {
                        
                        Log.d(TAG, "📤 Fazendo upload de foto local para Firebase Storage (mesa ${acertoMesa.mesaId})")
                        try {
                            val uploadedUrl = firebaseImageUploader.uploadMesaRelogio(
                                fotoParaEnviar,
                                acertoMesa.mesaId
                            )
                            
                            if (uploadedUrl != null) {
                                fotoParaEnviar = uploadedUrl
                                Log.d(TAG, "✅ Foto enviada para Firebase Storage: $uploadedUrl")
                                
                                // ✅ Atualizar o AcertoMesa local com a URL do Firebase
                                val acertoMesaAtualizado = acertoMesa.copy(
                                    fotoRelogioFinal = uploadedUrl
                                )
                                appRepository.inserirAcertoMesa(acertoMesaAtualizado)
                            } else {
                                Log.w(TAG, "⚠️ Falha no upload da foto, enviando caminho local")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro ao fazer upload da foto: ${e.message}", e)
                            // Continuar mesmo se o upload falhar, enviando o caminho local
                        }
                    }
                    
                    // ✅ Usar Gson para converter AcertoMesa para Map
                    // Se a foto foi atualizada, usar o objeto atualizado
                    val acertoMesaParaEnviar = if (fotoParaEnviar != acertoMesa.fotoRelogioFinal) {
                        acertoMesa.copy(fotoRelogioFinal = fotoParaEnviar)
                    } else {
                        acertoMesa
                    }
                    
                    val acertoMesaJson = gson.toJson(acertoMesaParaEnviar)
                    @Suppress("UNCHECKED_CAST")
                    val acertoMesaMap = gson.fromJson(acertoMesaJson, Map::class.java) as Map<String, Any>
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    val mutableMap = acertoMesaMap.toMutableMap()
                    mutableMap["roomId"] = acertoMesa.id
                    mutableMap["id"] = acertoMesa.id
                    mutableMap["lastModified"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_ACERTO_MESAS)
                    collectionRef
                        .document("${acertoMesa.acertoId}_${acertoMesa.mesaId}")
                        .set(mutableMap)
                        .await()
                    
                    Log.d(TAG, "✅ AcertoMesa ${acertoMesa.acertoId}_${acertoMesa.mesaId} enviado com foto: ${if (fotoParaEnviar != null) "sim" else "não"}")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar AcertoMesa ${acertoMesa.acertoId}_${acertoMesa.mesaId}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de AcertoMesas para acerto $acertoId: ${e.message}", e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Despesas com sincronização incremental
     * Envia apenas despesas modificadas desde o último push
     */
    private suspend fun pushDespesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_DESPESAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de despesas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val despesasLocais = appRepository.obterTodasDespesas().first()
            Log.d(TAG, "📥 Total de despesas locais encontradas: ${despesasLocais.size}")
            
            // Filtrar apenas despesas modificadas (usar dataHora convertida para timestamp)
            val despesasParaEnviar = if (canUseIncremental) {
                despesasLocais.filter { despesa ->
                    val despesaTimestamp = despesa.dataHora.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    despesaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} despesas modificadas desde ${Date(lastPushTimestamp)} (de ${despesasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${despesasLocais.size} despesas")
                despesasLocais
            }
            
            if (despesasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            despesasParaEnviar.forEach { despesa ->
                try {
                    Log.d(TAG, "📄 Processando despesa: ID=${despesa.id}, Descrição=${despesa.descricao}, CicloId=${despesa.cicloId}")
                    
                    val despesaMap = entityToMap(despesa)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    despesaMap["roomId"] = despesa.id
                    despesaMap["id"] = despesa.id
                    despesaMap["lastModified"] = FieldValue.serverTimestamp()
                    despesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_DESPESAS)
                    collectionRef
                        .document(despesa.id.toString())
                        .set(despesaMap)
                        .await()
                    
                    // ✅ CRÍTICO: Atualizar timestamp local após push bem-sucedido
                    // Isso evita que o pull sobrescreva os dados locais na próxima sincronização
                    // Como Despesa usa LocalDateTime, precisamos atualizar o dataHora
                    val despesaAtualizada = despesa.copy(
                        dataHora = java.time.LocalDateTime.now()
                    )
                    appRepository.atualizarDespesa(despesaAtualizada)
                    
                    syncCount++
                    bytesUploaded += despesaMap.toString().length.toLong()
                    Log.d(TAG, "✅ Despesa enviada para nuvem: ${despesa.descricao} (ID: ${despesa.id}) - Timestamp local atualizado")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar despesa ${despesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de despesas concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de despesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Contratos com sincronização incremental
     * Envia apenas contratos modificados desde o último push
     */
    private suspend fun pushContratos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CONTRATOS
        
        return try {
            Log.d(TAG, "Iniciando push INCREMENTAL de contratos...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val contratosLocais = appRepository.buscarTodosContratos().first()
            
            val contratosParaEnviar = if (canUseIncremental) {
                contratosLocais.filter { contrato ->
                    val contratoTimestamp = contrato.dataAtualizacao.time
                    contratoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} contratos modificados desde ${Date(lastPushTimestamp)} (de ${contratosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${contratosLocais.size} contratos")
                contratosLocais
            }
            
            if (contratosParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            contratosParaEnviar.forEach { contrato: ContratoLocacao ->
                try {
                    val contratoMap = entityToMap(contrato)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    contratoMap["roomId"] = contrato.id
                    contratoMap["id"] = contrato.id
                    contratoMap["lastModified"] = FieldValue.serverTimestamp()
                    contratoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_CONTRATOS)
                    collectionRef
                        .document(contrato.id.toString())
                        .set(contratoMap)
                        .await()
                    
                    // ✅ READ-YOUR-WRITES: Ler de volta para pegar o timestamp real do servidor
                    val snapshot = collectionRef.document(contrato.id.toString()).get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: System.currentTimeMillis()
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ Contrato ${contrato.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += contratoMap.toString().length.toLong()
                    
                    // Enviar aditivos relacionados
                    pushAditivosContrato(contrato.id)
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar contrato ${contrato.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de contratos concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de contratos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Aditivos: Envia aditivos de contrato relacionados
     */
    private suspend fun pushAditivosContrato(contratoId: Long) {
        try {
            val aditivos = appRepository.buscarAditivosPorContrato(contratoId).first()
            
            aditivos.forEach { aditivo: AditivoContrato ->
                try {
                    val aditivoMap = entityToMap(aditivo)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    aditivoMap["roomId"] = aditivo.id
                    aditivoMap["id"] = aditivo.id
                    aditivoMap["lastModified"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_ADITIVOS)
                    collectionRef
                        .document(aditivo.id.toString())
                        .set(aditivoMap)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar aditivo ${aditivo.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de aditivos para contrato $contratoId: ${e.message}", e)
        }
    }
    
    // ==================== PUSH HANDLERS - ENTIDADES FALTANTES ====================
    
    /**
     * ✅ REFATORADO (2025): Push Categorias Despesa com sincronização incremental
     */
    private suspend fun pushCategoriasDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CATEGORIAS_DESPESA
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de categorias despesa...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val categoriasLocais = appRepository.buscarCategoriasAtivas().first()
            Log.d(TAG, "📥 Total de categorias despesa locais encontradas: ${categoriasLocais.size}")
            
            val categoriasParaEnviar = if (canUseIncremental) {
                categoriasLocais.filter { categoria ->
                    val categoriaTimestamp = categoria.dataAtualizacao.time
                    categoriaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} categorias modificadas desde ${Date(lastPushTimestamp)} (de ${categoriasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${categoriasLocais.size} categorias")
                categoriasLocais
            }
            
            if (categoriasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            categoriasParaEnviar.forEach { categoria ->
                try {
                    Log.d(TAG, "📄 Processando categoria despesa: ID=${categoria.id}, Nome=${categoria.nome}")
                    
                    val categoriaMap = entityToMap(categoria)
                    categoriaMap["roomId"] = categoria.id
                    categoriaMap["id"] = categoria.id
                    categoriaMap["lastModified"] = FieldValue.serverTimestamp()
                    categoriaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = categoria.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_CATEGORIAS_DESPESA)
                    
                    collectionRef
                        .document(documentId)
                        .set(categoriaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += categoriaMap.toString().length.toLong()
                    Log.d(TAG, "✅ Categoria despesa enviada com sucesso: ${categoria.nome} (ID: ${categoria.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar categoria despesa ${categoria.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de categorias despesa concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de categorias despesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Tipos Despesa com sincronização incremental
     */
    private suspend fun pushTiposDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_TIPOS_DESPESA
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de tipos despesa...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val tiposLocais = appRepository.buscarTiposAtivosComCategoria().first()
                .map { it.tipoDespesa }
            Log.d(TAG, "📥 Total de tipos despesa locais encontrados: ${tiposLocais.size}")
            
            val tiposParaEnviar = if (canUseIncremental) {
                tiposLocais.filter { tipo ->
                    val tipoTimestamp = tipo.dataAtualizacao.time
                    tipoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} tipos modificados desde ${Date(lastPushTimestamp)} (de ${tiposLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${tiposLocais.size} tipos")
                tiposLocais
            }
            
            if (tiposParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            tiposParaEnviar.forEach { tipo ->
                try {
                    Log.d(TAG, "📄 Processando tipo despesa: ID=${tipo.id}, Nome=${tipo.nome}")
                    
                    val tipoMap = entityToMap(tipo)
                    tipoMap["roomId"] = tipo.id
                    tipoMap["id"] = tipo.id
                    tipoMap["lastModified"] = FieldValue.serverTimestamp()
                    tipoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = tipo.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_TIPOS_DESPESA)
                    
                    collectionRef
                        .document(documentId)
                        .set(tipoMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += tipoMap.toString().length.toLong()
                    Log.d(TAG, "✅ Tipo despesa enviado com sucesso: ${tipo.nome} (ID: ${tipo.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar tipo despesa ${tipo.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de tipos despesa concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de tipos despesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Metas com sincronização incremental
     */
    private suspend fun pushMetas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_METAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de metas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val metasLocais = appRepository.obterTodasMetas().first()
            Log.d(TAG, "📥 Total de metas locais encontradas: ${metasLocais.size}")
            
            // Meta não tem dataAtualizacao, usar dataInicio como proxy
            val metasParaEnviar = if (canUseIncremental) {
                metasLocais.filter { meta ->
                    val metaTimestamp = meta.dataInicio.time
                    metaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} metas modificadas desde ${Date(lastPushTimestamp)} (de ${metasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${metasLocais.size} metas")
                metasLocais
            }
            
            if (metasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            metasParaEnviar.forEach { meta ->
                try {
                    Log.d(TAG, "📄 Processando meta: ID=${meta.id}, Nome=${meta.nome}, Tipo=${meta.tipo}")
                    
                    val metaMap = entityToMap(meta)
                    Log.d(TAG, "   Mapa criado com ${metaMap.size} campos")
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    metaMap["roomId"] = meta.id
                    metaMap["id"] = meta.id
                    
                    // Adicionar metadados de sincronização
                    metaMap["lastModified"] = FieldValue.serverTimestamp()
                    metaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = meta.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_METAS)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$EMPRESA_ID/entidades/${COLLECTION_METAS}/items, document=$documentId")
                    
                    collectionRef
                        .document(documentId)
                        .set(metaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += metaMap.toString().length.toLong()
                    Log.d(TAG, "✅ Meta enviada com sucesso: ${meta.nome} (ID: ${meta.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar meta ${meta.id} (${meta.nome}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de metas concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de metas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Colaborador Rotas com sincronização incremental
     */
    private suspend fun pushColaboradorRotas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_COLABORADOR_ROTA
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de colaborador rotas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val colaboradorRotasLocais = appRepository.obterTodosColaboradorRotas()
            Log.d(TAG, "📥 Total de colaborador rotas locais encontradas: ${colaboradorRotasLocais.size}")
            
            val colaboradorRotasParaEnviar = if (canUseIncremental) {
                colaboradorRotasLocais.filter { colaboradorRota ->
                    val vinculacaoTimestamp = colaboradorRota.dataVinculacao.time
                    vinculacaoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} vinculações modificadas desde ${Date(lastPushTimestamp)} (de ${colaboradorRotasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${colaboradorRotasLocais.size} vinculações")
                colaboradorRotasLocais
            }
            
            if (colaboradorRotasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            colaboradorRotasParaEnviar.forEach { colaboradorRota ->
                try {
                    val colaboradorRotaMap = entityToMap(colaboradorRota)
                    // ✅ ColaboradorRota usa chave composta (colaboradorId, rotaId), então geramos um ID composto
                    val compositeId = "${colaboradorRota.colaboradorId}_${colaboradorRota.rotaId}"
                    colaboradorRotaMap["roomId"] = compositeId
                    colaboradorRotaMap["id"] = compositeId
                    colaboradorRotaMap["lastModified"] = FieldValue.serverTimestamp()
                    colaboradorRotaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = compositeId
                    val collectionRef = getCollectionReference(firestore, COLLECTION_COLABORADOR_ROTA)
                    
                    collectionRef
                        .document(documentId)
                        .set(colaboradorRotaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += colaboradorRotaMap.toString().length.toLong()
                    Log.d(TAG, "✅ ColaboradorRota enviado: Colaborador ${colaboradorRota.colaboradorId}, Rota ${colaboradorRota.rotaId} (ID: $compositeId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar colaborador rota ${colaboradorRota.colaboradorId}_${colaboradorRota.rotaId}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de colaborador rotas concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de colaborador rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Aditivo Mesas com sincronização incremental
     * Nota: AditivoMesa não tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    private suspend fun pushAditivoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ADITIVO_MESAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando push de aditivo mesas...")
            // Nota: AditivoMesa não tem campo de timestamp, então sempre enviar todos
            // (geralmente são poucos registros, impacto baixo)
            val aditivoMesasLocais = appRepository.obterTodosAditivoMesas()
            Log.d(TAG, "📥 Total de aditivo mesas locais encontradas: ${aditivoMesasLocais.size}")
            
            if (aditivoMesasLocais.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            aditivoMesasLocais.forEach { aditivoMesa ->
                try {
                    val aditivoMesaMap = entityToMap(aditivoMesa)
                    aditivoMesaMap["roomId"] = aditivoMesa.id
                    aditivoMesaMap["id"] = aditivoMesa.id
                    aditivoMesaMap["lastModified"] = FieldValue.serverTimestamp()
                    aditivoMesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = aditivoMesa.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_ADITIVO_MESAS)
                    
                    collectionRef
                        .document(documentId)
                        .set(aditivoMesaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += aditivoMesaMap.toString().length.toLong()
                    Log.d(TAG, "✅ AditivoMesa enviado: Aditivo ${aditivoMesa.aditivoId}, Mesa ${aditivoMesa.mesaId} (ID: ${aditivoMesa.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar aditivo mesa ${aditivoMesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push de aditivo mesas concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de aditivo mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Contrato Mesas com sincronização incremental
     * Nota: ContratoMesa não tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    private suspend fun pushContratoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CONTRATO_MESAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando push de contrato mesas...")
            // Nota: ContratoMesa não tem campo de timestamp, então sempre enviar todos
            // (geralmente são poucos registros, impacto baixo)
            val contratoMesasLocais = appRepository.obterTodosContratoMesas()
            Log.d(TAG, "📥 Total de contrato mesas locais encontradas: ${contratoMesasLocais.size}")
            
            if (contratoMesasLocais.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            contratoMesasLocais.forEach { contratoMesa ->
                try {
                    val contratoMesaMap = entityToMap(contratoMesa)
                    contratoMesaMap["roomId"] = contratoMesa.id
                    contratoMesaMap["id"] = contratoMesa.id
                    contratoMesaMap["lastModified"] = FieldValue.serverTimestamp()
                    contratoMesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = contratoMesa.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_CONTRATO_MESAS)
                    
                    collectionRef
                        .document(documentId)
                        .set(contratoMesaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += contratoMesaMap.toString().length.toLong()
                    Log.d(TAG, "✅ ContratoMesa enviado: Contrato ${contratoMesa.contratoId}, Mesa ${contratoMesa.mesaId} (ID: ${contratoMesa.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar contrato mesa ${contratoMesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push de contrato mesas concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de contrato mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Assinaturas Representante Legal com sincronização incremental
     */
    private suspend fun pushAssinaturasRepresentanteLegal(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ASSINATURAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de assinaturas representante legal...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val assinaturasLocais = appRepository.obterTodasAssinaturasRepresentanteLegal()
            Log.d(TAG, "📥 Total de assinaturas locais encontradas: ${assinaturasLocais.size}")
            
            val assinaturasParaEnviar = if (canUseIncremental) {
                assinaturasLocais.filter { assinatura ->
                    // Usar timestampCriacao (Long) ou dataCriacao como fallback
                    val assinaturaTimestamp = assinatura.timestampCriacao.takeIf { it > 0L } 
                        ?: assinatura.dataCriacao.time
                    assinaturaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} assinaturas modificadas desde ${Date(lastPushTimestamp)} (de ${assinaturasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${assinaturasLocais.size} assinaturas")
                assinaturasLocais
            }
            
            if (assinaturasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            assinaturasParaEnviar.forEach { assinatura ->
                try {
                    Log.d(TAG, "📄 Processando assinatura: ID=${assinatura.id}, Nome=${assinatura.nomeRepresentante}")
                    
                    val assinaturaMap = entityToMap(assinatura)
                    assinaturaMap["roomId"] = assinatura.id
                    assinaturaMap["id"] = assinatura.id
                    assinaturaMap["lastModified"] = FieldValue.serverTimestamp()
                    assinaturaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = assinatura.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_ASSINATURAS)
                    
                    collectionRef
                        .document(documentId)
                        .set(assinaturaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += assinaturaMap.toString().length.toLong()
                    Log.d(TAG, "✅ Assinatura enviada com sucesso: ${assinatura.nomeRepresentante} (ID: ${assinatura.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar assinatura ${assinatura.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de assinaturas concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de assinaturas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Logs Auditoria: Envia logs de auditoria do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Logs Auditoria com sincronização incremental
     */
    private suspend fun pushLogsAuditoria(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_LOGS_AUDITORIA
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de logs auditoria...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val logsLocais = appRepository.obterTodosLogsAuditoria()
            Log.d(TAG, "📥 Total de logs auditoria locais encontrados: ${logsLocais.size}")
            
            val logsParaEnviar = if (canUseIncremental) {
                logsLocais.filter { log ->
                    // Usar timestamp (Long) ou dataOperacao como fallback
                    val logTimestamp = log.timestamp.takeIf { it > 0L } 
                        ?: log.dataOperacao.time
                    logTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} logs modificados desde ${Date(lastPushTimestamp)} (de ${logsLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${logsLocais.size} logs")
                logsLocais
            }
            
            if (logsParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            logsParaEnviar.forEach { log ->
                try {
                    Log.d(TAG, "📄 Processando log auditoria: ID=${log.id}, Tipo=${log.tipoOperacao}")
                    
                    val logMap = entityToMap(log)
                    logMap["roomId"] = log.id
                    logMap["id"] = log.id
                    logMap["lastModified"] = FieldValue.serverTimestamp()
                    logMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = log.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_LOGS_AUDITORIA)
                    
                    collectionRef
                        .document(documentId)
                        .set(logMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += logMap.toString().length.toLong()
                    Log.d(TAG, "✅ Log auditoria enviado com sucesso: ${log.tipoOperacao} (ID: ${log.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar log auditoria ${log.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de logs auditoria concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de logs auditoria: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== UTILITÁRIOS ====================
    
    /**
     * Converte entidade para Map para Firestore
     * Converte campos Long que representam timestamps para Timestamp do Firestore
     */
    private fun <T> entityToMap(entity: T): MutableMap<String, Any> {
        val json = gson.toJson(entity)
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
        return map.mapKeys { it.key.toString() }.mapValues { entry ->
            when (val value = entry.value) {
                is Date -> com.google.firebase.Timestamp(value)
                is java.time.LocalDateTime -> com.google.firebase.Timestamp(value.toEpochSecond(java.time.ZoneOffset.UTC), 0)
                is Long -> {
                    // Se o campo contém "data" ou "timestamp" no nome, converter para Timestamp
                    val key = entry.key.lowercase()
                    if (key.contains("data") || key.contains("timestamp") || key.contains("time")) {
                        // Converter milissegundos para segundos e nanossegundos
                        val seconds = value / 1000
                        val nanoseconds = ((value % 1000) * 1000000).toInt()
                        com.google.firebase.Timestamp(seconds, nanoseconds)
                    } else {
                        value
                    }
                }
                is Number -> value // Manter números como estão
                else -> value
            }
        }.toMutableMap()
    }
    
    /**
     * Verifica se dispositivo está online.
     */
    fun isOnline(): Boolean = networkUtils.isConnected()
    
    /**
     * Obtém status atual da sincronização.
     */
    fun getSyncStatus(): SyncStatus = _syncStatus.value
    
    /**
     * Limpa status de erro.
     */
    fun clearError() {
        _syncStatus.value = _syncStatus.value.copy(error = null)
    }
    
    /**
     * Limpa operações antigas completadas.
     * Remove operações completadas há mais de 7 dias.
     */
    suspend fun limparOperacoesAntigas() {
        try {
            appRepository.limparOperacoesSyncCompletadas(dias = 7)
            Log.d(TAG, "Operações antigas limpas")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar operações antigas: ${e.message}", e)
        }
    }
    
    // ==================== PULL/PUSH HANDLERS - ENTIDADES FALTANTES (AGENTE PARALELO) ====================
    
    /**
     * Pull PanoEstoque: Sincroniza panos do estoque do Firestore para o Room
     */
    /**
     * ✅ REFATORADO (2025): Pull Pano Estoque com sincronização incremental
     * Nota: PanoEstoque não tem campo de timestamp, então sempre buscar todos
     */
    private suspend fun pullPanoEstoque(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_PANOS_ESTOQUE
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de panos estoque...")
            // Nota: PanoEstoque não tem campo de timestamp, então sempre buscar todos
            val collectionRef = getCollectionReference(firestore, COLLECTION_PANOS_ESTOQUE)
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Total de panos estoque no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando pano estoque: ID=${doc.id}")
                    
                    val panoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val pano = com.example.gestaobilhares.data.entities.PanoEstoque(
                        id = panoId,
                        numero = data["numero"] as? String ?: "",
                        cor = data["cor"] as? String ?: "",
                        tamanho = data["tamanho"] as? String ?: "",
                        material = data["material"] as? String ?: "",
                        disponivel = data["disponivel"] as? Boolean ?: true,
                        observacoes = data["observacoes"] as? String
                    )
                    
                    if (pano.numero.isBlank()) {
                        Log.w(TAG, "⚠️ Pano estoque ID $panoId sem número - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    appRepository.inserirPanoEstoque(pano)
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                    Log.d(TAG, "✅ PanoEstoque sincronizado: ${pano.numero} (ID: $panoId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar pano estoque ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, bytesDownloaded = bytesDownloaded, error = if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Pull de panos estoque concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no pull de panos estoque: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push PanoEstoque: Envia panos do estoque modificados do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Pano Estoque com sincronização incremental
     * Nota: PanoEstoque não tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    private suspend fun pushPanoEstoque(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_PANOS_ESTOQUE
        
        return try {
            Log.d(TAG, "🔵 Iniciando push de panos estoque...")
            // Nota: PanoEstoque não tem campo de timestamp, então sempre enviar todos
            // (geralmente são poucos registros, impacto baixo)
            val panosLocais = appRepository.obterTodosPanosEstoque().first()
            Log.d(TAG, "📥 Total de panos estoque locais encontrados: ${panosLocais.size}")
            
            if (panosLocais.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            panosLocais.forEach { pano ->
                try {
                    Log.d(TAG, "📄 Processando pano estoque: ID=${pano.id}")
                    
                    val panoMap = entityToMap(pano)
                    panoMap["roomId"] = pano.id
                    panoMap["id"] = pano.id
                    panoMap["lastModified"] = FieldValue.serverTimestamp()
                    panoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = pano.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_PANOS_ESTOQUE)
                    
                    collectionRef
                        .document(documentId)
                        .set(panoMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += panoMap.toString().length.toLong()
                    Log.d(TAG, "✅ PanoEstoque enviado com sucesso: ${pano.numero} (ID: ${pano.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar pano estoque ${pano.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de panos estoque concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de panos estoque: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Pull Mesa Vendida com sincronização incremental
     */
    private suspend fun pullMesaVendida(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_VENDIDAS
        
        return try {
            Log.d(TAG, "Iniciando pull de mesas vendidas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS_VENDIDAS)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullMesaVendidaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesasVendidas().first().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há mesas vendidas locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 mesas vendidas mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullMesaVendidaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullMesaVendidaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de mesas vendidas.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     */
    private suspend fun tryPullMesaVendidaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ CORREÇÃO CRÍTICA: Estratégia híbrida para garantir que mesas vendidas não desapareçam
            // 1. Tentar buscar apenas mesas vendidas modificadas recentemente (otimização)
            // 2. Se retornar 0 mas houver mesas vendidas locais, buscar TODAS para garantir sincronização completa
            
            // ✅ CORREÇÃO: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todasMesasVendidas = appRepository.obterTodasMesasVendidas().first()
            val mesasVendidasCache = todasMesasVendidas.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de mesas vendidas carregado: ${mesasVendidasCache.size} mesas vendidas locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalMesasVendidas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todas as mesas vendidas: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há mesas vendidas locais, buscar TODAS
            val allMesasVendidas = if (incrementalMesasVendidas.isEmpty() && mesasVendidasCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 mesas vendidas mas há ${mesasVendidasCache.size} locais - buscando TODAS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todas as mesas vendidas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesasVendidas
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allMesasVendidas.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allMesasVendidas.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando mesa vendida: ID=${doc.id}")
                    
                    val mesaVendidaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataVenda = converterTimestampParaDate(data["dataVenda"])
                        ?: converterTimestampParaDate(data["data_venda"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    // Converter enums
                    val tipoMesaStr = (data["tipoMesa"] as? String) ?: (data["tipo_mesa"] as? String) ?: "SINUCA"
                    val tipoMesa = try {
                        TipoMesa.valueOf(tipoMesaStr)
                    } catch (e: Exception) {
                        TipoMesa.SINUCA
                    }
                    
                    val tamanhoMesaStr = (data["tamanhoMesa"] as? String) ?: (data["tamanho_mesa"] as? String) ?: "GRANDE"
                    val tamanhoMesa = try {
                        TamanhoMesa.valueOf(tamanhoMesaStr)
                    } catch (e: Exception) {
                        TamanhoMesa.GRANDE
                    }
                    
                    val estadoConservacaoStr = (data["estadoConservacao"] as? String) ?: (data["estado_conservacao"] as? String) ?: "BOM"
                    val estadoConservacao = try {
                        EstadoConservacao.valueOf(estadoConservacaoStr)
                    } catch (e: Exception) {
                        EstadoConservacao.BOM
                    }
                    
                    val mesaVendida = MesaVendida(
                        id = mesaVendidaId,
                        mesaIdOriginal = (data["mesaIdOriginal"] as? Number)?.toLong() ?: (data["mesa_id_original"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoMesa = tipoMesa,
                        tamanhoMesa = tamanhoMesa,
                        estadoConservacao = estadoConservacao,
                        nomeComprador = data["nomeComprador"] as? String ?: (data["nome_comprador"] as? String) ?: "",
                        telefoneComprador = data["telefoneComprador"] as? String ?: (data["telefone_comprador"] as? String),
                        cpfCnpjComprador = data["cpfCnpjComprador"] as? String ?: (data["cpf_cnpj_comprador"] as? String),
                        enderecoComprador = data["enderecoComprador"] as? String ?: (data["endereco_comprador"] as? String),
                        valorVenda = (data["valorVenda"] as? Number)?.toDouble() ?: (data["valor_venda"] as? Number)?.toDouble() ?: 0.0,
                        dataVenda = dataVenda,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    // ✅ Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: dataCriacao.time
                    val mesaVendidaLocal = mesasVendidasCache[mesaVendidaId]
                    val localTimestamp = mesaVendidaLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = mesaVendidaLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        val rotaId = getMesaRouteId(mesaVendida.mesaIdOriginal)
                        if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                            skipCount++
                            return@forEach
                        }
                        
                        if (mesaVendida.numeroMesa.isBlank()) {
                            Log.w(TAG, "⚠️ Mesa vendida ID $mesaVendidaId sem número - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        if (mesaVendidaLocal == null) {
                            appRepository.inserirMesaVendida(mesaVendida)
                        } else {
                            appRepository.inserirMesaVendida(mesaVendida) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Log.d(TAG, "✅ MesaVendida sincronizada: ${mesaVendida.numeroMesa} (ID: $mesaVendidaId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "⏭️ Mesa vendida local mais recente ou igual, mantendo: ID=$mesaVendidaId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar mesa vendida ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de mesas vendidas: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Falha na sincronização incremental de mesas vendidas: ${e.message}")
            return null // Retorna null para usar fallback completo
        }
    }
    
    /**
     * ✅ NOVO (2025): Pull completo de mesas vendidas (fallback)
     */
    private suspend fun pullMesaVendidaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "📥 Pull COMPLETO de mesas vendidas - documentos recebidos: ${snapshot.size()}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val todasMesasVendidas = appRepository.obterTodasMesasVendidas().first()
            val mesasVendidasCache = todasMesasVendidas.associateBy { it.id }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    val mesaVendidaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataVenda = converterTimestampParaDate(data["dataVenda"]) ?: converterTimestampParaDate(data["data_venda"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"]) ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val tipoMesaStr = (data["tipoMesa"] as? String) ?: (data["tipo_mesa"] as? String) ?: "SINUCA"
                    val tipoMesa = try { TipoMesa.valueOf(tipoMesaStr) } catch (e: Exception) { TipoMesa.SINUCA }
                    
                    val tamanhoMesaStr = (data["tamanhoMesa"] as? String) ?: (data["tamanho_mesa"] as? String) ?: "GRANDE"
                    val tamanhoMesa = try { TamanhoMesa.valueOf(tamanhoMesaStr) } catch (e: Exception) { TamanhoMesa.GRANDE }
                    
                    val estadoConservacaoStr = (data["estadoConservacao"] as? String) ?: (data["estado_conservacao"] as? String) ?: "BOM"
                    val estadoConservacao = try { EstadoConservacao.valueOf(estadoConservacaoStr) } catch (e: Exception) { EstadoConservacao.BOM }
                    
                    val mesaVendida = MesaVendida(
                        id = mesaVendidaId,
                        mesaIdOriginal = (data["mesaIdOriginal"] as? Number)?.toLong() ?: (data["mesa_id_original"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoMesa = tipoMesa,
                        tamanhoMesa = tamanhoMesa,
                        estadoConservacao = estadoConservacao,
                        nomeComprador = data["nomeComprador"] as? String ?: (data["nome_comprador"] as? String) ?: "",
                        telefoneComprador = data["telefoneComprador"] as? String ?: (data["telefone_comprador"] as? String),
                        cpfCnpjComprador = data["cpfCnpjComprador"] as? String ?: (data["cpf_cnpj_comprador"] as? String),
                        enderecoComprador = data["enderecoComprador"] as? String ?: (data["endereco_comprador"] as? String),
                        valorVenda = (data["valorVenda"] as? Number)?.toDouble() ?: (data["valor_venda"] as? Number)?.toDouble() ?: 0.0,
                        dataVenda = dataVenda,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    val rotaId = getMesaRouteId(mesaVendida.mesaIdOriginal)
                    if (!shouldSyncRouteData(rotaId, allowUnknown = false)) {
                        skipCount++
                        return@forEach
                    }
                    
                    if (mesaVendida.numeroMesa.isBlank()) {
                        skipCount++
                        return@forEach
                    }
                    
                    val mesaVendidaLocal = mesasVendidasCache[mesaVendidaId]
                    if (mesaVendidaLocal == null) {
                        appRepository.inserirMesaVendida(mesaVendida)
                    } else {
                        // Mesa vendida geralmente não é atualizada, mas se necessário, inserir novamente
                        appRepository.inserirMesaVendida(mesaVendida)
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar mesa vendida ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull COMPLETO de mesas vendidas: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Mesa Vendida com sincronização incremental
     * Segue melhores práticas Android 2025: não altera dados locais durante exportação
     */
    private suspend fun pushMesaVendida(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_VENDIDAS
        
        return try {
            Log.d(TAG, "📤 Iniciando push INCREMENTAL de mesas vendidas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val mesasVendidasLocais = appRepository.obterTodasMesasVendidas().first()
            Log.d(TAG, "📊 Total de mesas vendidas locais encontradas: ${mesasVendidasLocais.size}")
            
            // ✅ Filtrar apenas mesas vendidas modificadas desde último push
            val mesasParaEnviar = if (canUseIncremental) {
                mesasVendidasLocais.filter { mesaVendida ->
                    val mesaTimestamp = mesaVendida.dataCriacao.time
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} mesas vendidas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasVendidasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${mesasVendidasLocais.size} mesas vendidas")
                mesasVendidasLocais
            }
            
            if (mesasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            mesasParaEnviar.forEach { mesaVendida ->
                try {
                    val mesaVendidaMap = entityToMap(mesaVendida)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    mesaVendidaMap["roomId"] = mesaVendida.id
                    mesaVendidaMap["id"] = mesaVendida.id
                    mesaVendidaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaVendidaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS_VENDIDAS)
                    val docRef = collectionRef.document(mesaVendida.id.toString())
                    
                    // 1. Escrever
                    docRef.set(mesaVendidaMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ Mesa vendida ${mesaVendida.id} exportada com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += mesaVendidaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar mesa vendida ${mesaVendida.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de mesas vendidas concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Pull Stock Item com sincronização incremental
     * Segue melhores práticas Android 2025
     */
    private suspend fun pullStockItem(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_STOCK_ITEMS
        
        return try {
            Log.d(TAG, "Iniciando pull de stock items...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_STOCK_ITEMS)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullStockItemIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosStockItems().first().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há stock items locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 stock items mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullStockItemComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullStockItemComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de stock items.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     */
    private suspend fun tryPullStockItemIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ CORREÇÃO CRÍTICA: Estratégia híbrida para garantir que stock items não desapareçam
            resetRouteFilters()
            val todosStockItems = appRepository.obterTodosStockItems().first()
            val stockItemsCache = todosStockItems.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de stock items carregado: ${stockItemsCache.size} stock items locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalStockItems = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todos os stock items: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há stock items locais, buscar TODOS
            val allStockItems = if (incrementalStockItems.isEmpty() && stockItemsCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 stock items mas há ${stockItemsCache.size} locais - buscando TODOS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todos os stock items: ${e.message}")
                    return null
                }
            } else {
                incrementalStockItems
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allStockItems.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allStockItems.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando stock item: ID=${doc.id}")
                    
                    val stockItemId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val createdAt = converterTimestampParaDate(data["createdAt"])
                        ?: converterTimestampParaDate(data["created_at"]) ?: Date()
                    val updatedAt = converterTimestampParaDate(data["updatedAt"])
                        ?: converterTimestampParaDate(data["updated_at"]) ?: Date()
                    
                    val stockItem = StockItem(
                        id = stockItemId,
                        name = data["name"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                        unitPrice = (data["unitPrice"] as? Number)?.toDouble() ?: (data["unit_price"] as? Number)?.toDouble() ?: 0.0,
                        supplier = data["supplier"] as? String ?: "",
                        description = data["description"] as? String,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    
                    // ✅ Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: updatedAt.time
                    val stockItemLocal = stockItemsCache[stockItemId]
                    val localTimestamp = stockItemLocal?.updatedAt?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = stockItemLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (stockItem.name.isBlank()) {
                            Log.w(TAG, "⚠️ Stock item ID $stockItemId sem nome - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        if (stockItemLocal == null) {
                            appRepository.inserirStockItem(stockItem)
                        } else {
                            appRepository.inserirStockItem(stockItem) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Log.d(TAG, "✅ StockItem sincronizado: ${stockItem.name} (ID: $stockItemId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "⏭️ Stock item local mais recente ou igual, mantendo: ID=$stockItemId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar stock item ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de stock items: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Falha na sincronização incremental de stock items: ${e.message}")
            return null // Retorna null para usar fallback completo
        }
    }
    
    /**
     * ✅ NOVO (2025): Pull completo de stock items (fallback)
     */
    private suspend fun pullStockItemComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "📥 Pull COMPLETO de stock items - documentos recebidos: ${snapshot.size()}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val todosStockItems = appRepository.obterTodosStockItems().first()
            val stockItemsCache = todosStockItems.associateBy { it.id }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    val stockItemId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val createdAt = converterTimestampParaDate(data["createdAt"]) ?: converterTimestampParaDate(data["created_at"]) ?: Date()
                    val updatedAt = converterTimestampParaDate(data["updatedAt"]) ?: converterTimestampParaDate(data["updated_at"]) ?: Date()
                    
                    val stockItem = StockItem(
                        id = stockItemId,
                        name = data["name"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                        unitPrice = (data["unitPrice"] as? Number)?.toDouble() ?: (data["unit_price"] as? Number)?.toDouble() ?: 0.0,
                        supplier = data["supplier"] as? String ?: "",
                        description = data["description"] as? String,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    
                    if (stockItem.name.isBlank()) {
                        skipCount++
                        return@forEach
                    }
                    
                    val stockItemLocal = stockItemsCache[stockItemId]
                    if (stockItemLocal == null) {
                        appRepository.inserirStockItem(stockItem)
                    } else {
                        appRepository.inserirStockItem(stockItem) // REPLACE atualiza se existir
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar stock item ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull COMPLETO de stock items: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push Stock Item com sincronização incremental
     * Segue melhores práticas Android 2025: não altera dados locais durante exportação
     */
    private suspend fun pushStockItem(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_STOCK_ITEMS
        
        return try {
            Log.d(TAG, "📤 Iniciando push INCREMENTAL de stock items...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val stockItemsLocais = appRepository.obterTodosStockItems().first()
            Log.d(TAG, "📊 Total de stock items locais encontrados: ${stockItemsLocais.size}")
            
            // ✅ Filtrar apenas stock items modificados desde último push (usar updatedAt)
            val itemsParaEnviar = if (canUseIncremental) {
                stockItemsLocais.filter { stockItem ->
                    val itemTimestamp = stockItem.updatedAt.time
                    itemTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} stock items modificados desde ${Date(lastPushTimestamp)} (de ${stockItemsLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${stockItemsLocais.size} stock items")
                stockItemsLocais
            }
            
            if (itemsParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            itemsParaEnviar.forEach { stockItem ->
                try {
                    val stockItemMap = entityToMap(stockItem)
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    stockItemMap["roomId"] = stockItem.id
                    stockItemMap["id"] = stockItem.id
                    stockItemMap["lastModified"] = FieldValue.serverTimestamp()
                    stockItemMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_STOCK_ITEMS)
                    val docRef = collectionRef.document(stockItem.id.toString())
                    
                    // 1. Escrever
                    docRef.set(stockItemMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ Stock item ${stockItem.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += stockItemMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar stock item ${stockItem.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de stock items concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Pull Mesa Reformada com sincronização incremental
     */
    private suspend fun pullMesaReformada(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_REFORMADAS
        
        return try {
            Log.d(TAG, "Iniciando pull de mesas reformadas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS_REFORMADAS)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullMesaReformadaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesasReformadas().first().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há mesas reformadas locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 mesas reformadas mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullMesaReformadaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullMesaReformadaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de mesas reformadas.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores práticas Android 2025 com estratégia híbrida.
     */
    private suspend fun tryPullMesaReformadaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ ANDROID 2025: Estratégia híbrida para garantir que mesas reformadas não desapareçam
            // 1. Tentar buscar apenas mesas reformadas modificadas recentemente (otimização)
            // 2. Se retornar 0 mas houver mesas reformadas locais, buscar TODAS para garantir sincronização completa
            
            // ✅ CORREÇÃO: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todasMesasReformadas = appRepository.obterTodasMesasReformadas().first()
            val mesasReformadasCache = todasMesasReformadas.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de mesas reformadas carregado: ${mesasReformadasCache.size} mesas reformadas locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalMesasReformadas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todas as mesas reformadas: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há mesas reformadas locais, buscar TODAS
            val allMesasReformadas = if (incrementalMesasReformadas.isEmpty() && mesasReformadasCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 mesas reformadas mas há ${mesasReformadasCache.size} locais - buscando TODAS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todas as mesas reformadas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesasReformadas
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allMesasReformadas.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allMesasReformadas.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando mesa reformada: ID=${doc.id}")
                    
                    val mesaReformadaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataReforma = converterTimestampParaDate(data["dataReforma"])
                        ?: converterTimestampParaDate(data["data_reforma"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    // Converter enums
                    val tipoMesaStr = (data["tipoMesa"] as? String) ?: (data["tipo_mesa"] as? String) ?: "SINUCA"
                    val tipoMesa = try {
                        TipoMesa.valueOf(tipoMesaStr)
                    } catch (e: Exception) {
                        TipoMesa.SINUCA
                    }
                    
                    val tamanhoMesaStr = (data["tamanhoMesa"] as? String) ?: (data["tamanho_mesa"] as? String) ?: "GRANDE"
                    val tamanhoMesa = try {
                        TamanhoMesa.valueOf(tamanhoMesaStr)
                    } catch (e: Exception) {
                        TamanhoMesa.GRANDE
                    }
                    
                    val mesaReformada = MesaReformada(
                        id = mesaReformadaId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoMesa = tipoMesa,
                        tamanhoMesa = tamanhoMesa,
                        pintura = data["pintura"] as? Boolean ?: false,
                        tabela = data["tabela"] as? Boolean ?: false,
                        panos = data["panos"] as? Boolean ?: false,
                        numeroPanos = data["numeroPanos"] as? String ?: (data["numero_panos"] as? String),
                        outros = data["outros"] as? Boolean ?: false,
                        observacoes = data["observacoes"] as? String,
                        fotoReforma = data["fotoReforma"] as? String ?: (data["foto_reforma"] as? String),
                        dataReforma = dataReforma,
                        dataCriacao = dataCriacao
                    )
                    
                    // ✅ ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: mesaReformada.dataCriacao.time
                    val mesaReformadaLocal = mesasReformadasCache[mesaReformadaId]
                    val localTimestamp = mesaReformadaLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = mesaReformadaLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (mesaReformada.numeroMesa.isBlank()) {
                            Log.w(TAG, "⚠️ Mesa reformada ID $mesaReformadaId sem número - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        if (mesaReformadaLocal == null) {
                            appRepository.inserirMesaReformada(mesaReformada)
                        } else {
                            appRepository.inserirMesaReformada(mesaReformada) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Log.d(TAG, "✅ MesaReformada sincronizada: ${mesaReformada.numeroMesa} (ID: $mesaReformadaId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "⏭️ Mesa reformada local mais recente ou igual, mantendo: ID=$mesaReformadaId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar mesa reformada ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull MesasReformadas (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizadas, $skipCount puladas, $errorCount erros")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental de mesas reformadas: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de mesas reformadas.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullMesaReformadaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "📥 Pull COMPLETO de mesas reformadas - documentos recebidos: ${snapshot.size()}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val todasMesasReformadas = appRepository.obterTodasMesasReformadas().first()
            val mesasReformadasCache = todasMesasReformadas.associateBy { it.id }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    val mesaReformadaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataReforma = converterTimestampParaDate(data["dataReforma"])
                        ?: converterTimestampParaDate(data["data_reforma"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val tipoMesaStr = (data["tipoMesa"] as? String) ?: (data["tipo_mesa"] as? String) ?: "SINUCA"
                    val tipoMesa = try { TipoMesa.valueOf(tipoMesaStr) } catch (e: Exception) { TipoMesa.SINUCA }
                    
                    val tamanhoMesaStr = (data["tamanhoMesa"] as? String) ?: (data["tamanho_mesa"] as? String) ?: "GRANDE"
                    val tamanhoMesa = try { TamanhoMesa.valueOf(tamanhoMesaStr) } catch (e: Exception) { TamanhoMesa.GRANDE }
                    
                    val mesaReformada = MesaReformada(
                        id = mesaReformadaId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoMesa = tipoMesa,
                        tamanhoMesa = tamanhoMesa,
                        pintura = data["pintura"] as? Boolean ?: false,
                        tabela = data["tabela"] as? Boolean ?: false,
                        panos = data["panos"] as? Boolean ?: false,
                        numeroPanos = data["numeroPanos"] as? String ?: (data["numero_panos"] as? String),
                        outros = data["outros"] as? Boolean ?: false,
                        observacoes = data["observacoes"] as? String,
                        fotoReforma = data["fotoReforma"] as? String ?: (data["foto_reforma"] as? String),
                        dataReforma = dataReforma,
                        dataCriacao = dataCriacao
                    )
                    
                    if (mesaReformada.numeroMesa.isBlank()) {
                        skipCount++
                        return@forEach
                    }
                    
                    val mesaReformadaLocal = mesasReformadasCache[mesaReformadaId]
                    if (mesaReformadaLocal == null) {
                        appRepository.inserirMesaReformada(mesaReformada)
                    } else {
                        appRepository.inserirMesaReformada(mesaReformada) // REPLACE atualiza se existir
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar mesa reformada ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull MesasReformadas (COMPLETO) concluído: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no pull completo de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push MesaReformada: Envia mesas reformadas do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Mesa Reformada com sincronização incremental
     */
    private suspend fun pushMesaReformada(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_REFORMADAS
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de mesas reformadas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val mesasReformadasLocais = appRepository.obterTodasMesasReformadas().first()
            Log.d(TAG, "📥 Total de mesas reformadas locais encontradas: ${mesasReformadasLocais.size}")
            
            val mesasParaEnviar = if (canUseIncremental) {
                mesasReformadasLocais.filter { mesaReformada ->
                    val mesaTimestamp = mesaReformada.dataCriacao.time
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} mesas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasReformadasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${mesasReformadasLocais.size} mesas")
                mesasReformadasLocais
            }
            
            if (mesasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            mesasParaEnviar.forEach { mesaReformada ->
                try {
                    val mesaReformadaMap = entityToMap(mesaReformada)
                    mesaReformadaMap["roomId"] = mesaReformada.id
                    mesaReformadaMap["id"] = mesaReformada.id
                    mesaReformadaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaReformadaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = mesaReformada.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS_REFORMADAS)
                    
                    val docRef = collectionRef.document(documentId)
                    
                    // 1. Escrever
                    docRef.set(mesaReformadaMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ MesaReformada ${mesaReformada.id} exportada com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += mesaReformadaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar mesa reformada ${mesaReformada.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de mesas reformadas concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull PanoMesa: Sincroniza vinculações pano-mesa do Firestore para o Room
     */
    /**
     * ✅ REFATORADO (2025): Pull Pano Mesa com sincronização incremental
     * Segue melhores práticas Android 2025 com estratégia híbrida
     */
    private suspend fun pullPanoMesa(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_PANO_MESAS
        
        return try {
            Log.d(TAG, "Iniciando pull de pano mesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_PANO_MESAS)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullPanoMesaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosPanoMesa().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há pano mesas locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 pano mesas mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullPanoMesaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullPanoMesaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de pano mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de pano mesas.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores práticas Android 2025 com estratégia híbrida.
     */
    private suspend fun tryPullPanoMesaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ ANDROID 2025: Estratégia híbrida para garantir que pano mesas não desapareçam
            resetRouteFilters()
            val todosPanoMesas = appRepository.obterTodosPanoMesa()
            val panoMesasCache = todosPanoMesas.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de pano mesas carregado: ${panoMesasCache.size} pano mesas locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalPanoMesas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todas as pano mesas: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há pano mesas locais, buscar TODAS
            val allPanoMesas = if (incrementalPanoMesas.isEmpty() && panoMesasCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 pano mesas mas há ${panoMesasCache.size} locais - buscando TODAS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todas as pano mesas: ${e.message}")
                    return null
                }
            } else {
                incrementalPanoMesas
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allPanoMesas.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allPanoMesas.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando pano mesa: ID=${doc.id}")
                    
                    val panoMesaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataTroca = converterTimestampParaDate(data["dataTroca"])
                        ?: converterTimestampParaDate(data["data_troca"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val panoMesa = PanoMesa(
                        id = panoMesaId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        panoId = (data["panoId"] as? Number)?.toLong() ?: (data["pano_id"] as? Number)?.toLong() ?: 0L,
                        dataTroca = dataTroca,
                        ativo = data["ativo"] as? Boolean ?: true,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    // ✅ ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: panoMesa.dataCriacao.time
                    val panoMesaLocal = panoMesasCache[panoMesaId]
                    val localTimestamp = panoMesaLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = panoMesaLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (panoMesa.mesaId == 0L || panoMesa.panoId == 0L) {
                            Log.w(TAG, "⚠️ Pano mesa ID $panoMesaId sem mesaId ou panoId - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        if (panoMesaLocal == null) {
                            appRepository.inserirPanoMesa(panoMesa)
                        } else {
                            appRepository.inserirPanoMesa(panoMesa) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Log.d(TAG, "✅ PanoMesa sincronizado: Mesa ${panoMesa.mesaId}, Pano ${panoMesa.panoId} (ID: $panoMesaId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "⏭️ Pano mesa local mais recente ou igual, mantendo: ID=$panoMesaId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar pano mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull PanoMesas (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental de pano mesas: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de pano mesas.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullPanoMesaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "📥 Pull COMPLETO de pano mesas - documentos recebidos: ${snapshot.size()}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val todosPanoMesas = appRepository.obterTodosPanoMesa()
            val panoMesasCache = todosPanoMesas.associateBy { it.id }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    val panoMesaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataTroca = converterTimestampParaDate(data["dataTroca"])
                        ?: converterTimestampParaDate(data["data_troca"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val panoMesa = PanoMesa(
                        id = panoMesaId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        panoId = (data["panoId"] as? Number)?.toLong() ?: (data["pano_id"] as? Number)?.toLong() ?: 0L,
                        dataTroca = dataTroca,
                        ativo = data["ativo"] as? Boolean ?: true,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    if (panoMesa.mesaId == 0L || panoMesa.panoId == 0L) {
                        skipCount++
                        return@forEach
                    }
                    
                    val panoMesaLocal = panoMesasCache[panoMesaId]
                    if (panoMesaLocal == null) {
                        appRepository.inserirPanoMesa(panoMesa)
                    } else {
                        appRepository.inserirPanoMesa(panoMesa) // REPLACE atualiza se existir
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar pano mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull PanoMesas (COMPLETO) concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no pull completo de pano mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Push PanoMesa com sincronização incremental
     * Segue melhores práticas Android 2025: não altera dados locais durante exportação
     */
    private suspend fun pushPanoMesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_PANO_MESAS
        
        return try {
            Log.d(TAG, "📤 Iniciando push INCREMENTAL de pano mesas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val panoMesasLocais = appRepository.obterTodosPanoMesa()
            Log.d(TAG, "📊 Total de pano mesas locais encontrados: ${panoMesasLocais.size}")
            
            // ✅ Filtrar apenas pano mesas modificados desde último push (usar dataCriacao)
            val panoMesasParaEnviar = if (canUseIncremental) {
                panoMesasLocais.filter { panoMesa: PanoMesa ->
                    val panoMesaTimestamp = panoMesa.dataCriacao.time
                    panoMesaTimestamp > lastPushTimestamp
                }.also { filteredList ->
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${filteredList.size} pano mesas modificados desde ${Date(lastPushTimestamp)} (de ${panoMesasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${panoMesasLocais.size} pano mesas")
                panoMesasLocais
            }
            
            if (panoMesasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            panoMesasParaEnviar.forEach { panoMesa: PanoMesa ->
                try {
                    val panoMesaMap = entityToMap(panoMesa)
                    panoMesaMap["roomId"] = panoMesa.id
                    panoMesaMap["id"] = panoMesa.id
                    panoMesaMap["lastModified"] = FieldValue.serverTimestamp()
                    panoMesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = panoMesa.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_PANO_MESAS)
                    val docRef = collectionRef.document(documentId)
                    
                    // 1. Escrever
                    docRef.set(panoMesaMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ PanoMesa ${panoMesa.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += panoMesaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar pano mesa ${panoMesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de pano mesas concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de pano mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Pull Historico Manutenção Mesa com sincronização incremental
     * Segue melhores práticas Android 2025 com estratégia híbrida
     */
    private suspend fun pullHistoricoManutencaoMesa(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_MANUTENCAO_MESA
        
        return try {
            Log.d(TAG, "Iniciando pull de histórico manutenção mesa...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_MANUTENCAO_MESA)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullHistoricoManutencaoMesaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosHistoricoManutencaoMesa().first().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há históricos locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 históricos mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullHistoricoManutencaoMesaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullHistoricoManutencaoMesaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de histórico manutenção mesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de histórico manutenção mesa.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores práticas Android 2025 com estratégia híbrida.
     */
    private suspend fun tryPullHistoricoManutencaoMesaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ ANDROID 2025: Estratégia híbrida para garantir que históricos não desapareçam
            resetRouteFilters()
            val todosHistoricos = appRepository.obterTodosHistoricoManutencaoMesa().first()
            val historicosCache = todosHistoricos.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de históricos manutenção mesa carregado: ${historicosCache.size} históricos locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalHistoricos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todos os históricos: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há históricos locais, buscar TODOS
            val allHistoricos = if (incrementalHistoricos.isEmpty() && historicosCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 históricos mas há ${historicosCache.size} locais - buscando TODOS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todos os históricos: ${e.message}")
                    return null
                }
            } else {
                incrementalHistoricos
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allHistoricos.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allHistoricos.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando histórico manutenção mesa: ID=${doc.id}")
                    
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataManutencao = converterTimestampParaDate(data["dataManutencao"])
                        ?: converterTimestampParaDate(data["data_manutencao"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    // Converter enum TipoManutencao
                    val tipoManutencaoStr = (data["tipoManutencao"] as? String) ?: (data["tipo_manutencao"] as? String) ?: "OUTROS"
                    val tipoManutencao = try {
                        TipoManutencao.valueOf(tipoManutencaoStr)
                    } catch (e: Exception) {
                        TipoManutencao.OUTROS
                    }
                    
                    val historico = HistoricoManutencaoMesa(
                        id = historicoId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoManutencao = tipoManutencao,
                        descricao = data["descricao"] as? String,
                        dataManutencao = dataManutencao,
                        responsavel = data["responsavel"] as? String,
                        observacoes = data["observacoes"] as? String,
                        custo = (data["custo"] as? Number)?.toDouble(),
                        fotoAntes = data["fotoAntes"] as? String ?: (data["foto_antes"] as? String),
                        fotoDepois = data["fotoDepois"] as? String ?: (data["foto_depois"] as? String),
                        dataCriacao = dataCriacao
                    )
                    
                    // ✅ ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: historico.dataCriacao.time
                    val historicoLocal = historicosCache[historicoId]
                    val localTimestamp = historicoLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = historicoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (historico.mesaId == 0L) {
                            Log.w(TAG, "⚠️ Histórico manutenção mesa ID $historicoId sem mesaId - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        if (historicoLocal == null) {
                            appRepository.inserirHistoricoManutencaoMesa(historico)
                        } else {
                            appRepository.inserirHistoricoManutencaoMesa(historico) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Log.d(TAG, "✅ HistoricoManutencaoMesa sincronizado: Mesa ${historico.numeroMesa} (ID: $historicoId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "⏭️ Histórico local mais recente ou igual, mantendo: ID=$historicoId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico manutenção mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull HistoricoManutencaoMesa (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental de histórico manutenção mesa: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de histórico manutenção mesa.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullHistoricoManutencaoMesaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "📥 Pull COMPLETO de histórico manutenção mesa - documentos recebidos: ${snapshot.size()}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val todosHistoricos = appRepository.obterTodosHistoricoManutencaoMesa().first()
            val historicosCache = todosHistoricos.associateBy { it.id }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataManutencao = converterTimestampParaDate(data["dataManutencao"])
                        ?: converterTimestampParaDate(data["data_manutencao"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val tipoManutencaoStr = (data["tipoManutencao"] as? String) ?: (data["tipo_manutencao"] as? String) ?: "OUTROS"
                    val tipoManutencao = try { TipoManutencao.valueOf(tipoManutencaoStr) } catch (e: Exception) { TipoManutencao.OUTROS }
                    
                    val historico = HistoricoManutencaoMesa(
                        id = historicoId,
                        mesaId = (data["mesaId"] as? Number)?.toLong() ?: (data["mesa_id"] as? Number)?.toLong() ?: 0L,
                        numeroMesa = data["numeroMesa"] as? String ?: (data["numero_mesa"] as? String) ?: "",
                        tipoManutencao = tipoManutencao,
                        descricao = data["descricao"] as? String,
                        dataManutencao = dataManutencao,
                        responsavel = data["responsavel"] as? String,
                        observacoes = data["observacoes"] as? String,
                        custo = (data["custo"] as? Number)?.toDouble(),
                        fotoAntes = data["fotoAntes"] as? String ?: (data["foto_antes"] as? String),
                        fotoDepois = data["fotoDepois"] as? String ?: (data["foto_depois"] as? String),
                        dataCriacao = dataCriacao
                    )
                    
                    if (historico.mesaId == 0L) {
                        skipCount++
                        return@forEach
                    }
                    
                    val historicoLocal = historicosCache[historicoId]
                    if (historicoLocal == null) {
                        appRepository.inserirHistoricoManutencaoMesa(historico)
                    } else {
                        appRepository.inserirHistoricoManutencaoMesa(historico) // REPLACE atualiza se existir
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico manutenção mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull HistoricoManutencaoMesa (COMPLETO) concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no pull completo de histórico manutenção mesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoManutencaoMesa: Envia histórico de manutenção de mesas modificado do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Historico Manutenção Mesa com sincronização incremental
     */
    private suspend fun pushHistoricoManutencaoMesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_MANUTENCAO_MESA
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de histórico manutenção mesa...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val historicosLocais = appRepository.obterTodosHistoricoManutencaoMesa().first()
            Log.d(TAG, "📥 Total de histórico manutenção mesa locais encontrados: ${historicosLocais.size}")
            
            val historicosParaEnviar = if (canUseIncremental) {
                historicosLocais.filter { historico ->
                    val historicoTimestamp = historico.dataCriacao.time
                    historicoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} históricos modificados desde ${Date(lastPushTimestamp)} (de ${historicosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${historicosLocais.size} históricos")
                historicosLocais
            }
            
            if (historicosParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            historicosParaEnviar.forEach { historico ->
                try {
                    Log.d(TAG, "📄 Processando histórico manutenção mesa: ID=${historico.id}")
                    
                    val historicoMap = entityToMap(historico)
                    historicoMap["roomId"] = historico.id
                    historicoMap["id"] = historico.id
                    historicoMap["lastModified"] = FieldValue.serverTimestamp()
                    historicoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = historico.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_MANUTENCAO_MESA)
                    
                    val docRef = collectionRef.document(documentId)
                    
                    // 1. Escrever
                    docRef.set(historicoMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ HistoricoManutencaoMesa ${historico.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += historicoMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar histórico manutenção mesa ${historico.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de histórico manutenção mesa concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de histórico manutenção mesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Pull Historico Manutenção Veículo com sincronização incremental
     * Segue melhores práticas Android 2025 com estratégia híbrida
     */
    private suspend fun pullHistoricoManutencaoVeiculo(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_MANUTENCAO_VEICULO
        
        return try {
            Log.d(TAG, "Iniciando pull de histórico manutenção veículo...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_MANUTENCAO_VEICULO)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullHistoricoManutencaoVeiculoIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosHistoricoManutencaoVeiculo().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há históricos locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 históricos mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullHistoricoManutencaoVeiculoComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullHistoricoManutencaoVeiculoComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de histórico manutenção veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de histórico manutenção veículo.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores práticas Android 2025 com estratégia híbrida.
     */
    private suspend fun tryPullHistoricoManutencaoVeiculoIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ ANDROID 2025: Estratégia híbrida para garantir que históricos não desapareçam
            resetRouteFilters()
            val todosHistoricos = appRepository.obterTodosHistoricoManutencaoVeiculo()
            val historicosCache = todosHistoricos.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de históricos manutenção veículo carregado: ${historicosCache.size} históricos locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalHistoricos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todos os históricos: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há históricos locais, buscar TODOS
            val allHistoricos = if (incrementalHistoricos.isEmpty() && historicosCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 históricos mas há ${historicosCache.size} locais - buscando TODOS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todos os históricos: ${e.message}")
                    return null
                }
            } else {
                incrementalHistoricos
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allHistoricos.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allHistoricos.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando histórico manutenção veículo: ID=${doc.id}")
                    
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataManutencao = converterTimestampParaDate(data["dataManutencao"])
                        ?: converterTimestampParaDate(data["data_manutencao"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val historico = HistoricoManutencaoVeiculo(
                        id = historicoId,
                        veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: (data["veiculo_id"] as? Number)?.toLong() ?: 0L,
                        tipoManutencao = data["tipoManutencao"] as? String ?: (data["tipo_manutencao"] as? String) ?: "",
                        descricao = data["descricao"] as? String ?: "",
                        dataManutencao = dataManutencao,
                        valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                        kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: (data["km_veiculo"] as? Number)?.toLong() ?: 0L,
                        responsavel = data["responsavel"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    // ✅ ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: historico.dataCriacao.time
                    val historicoLocal = historicosCache[historicoId]
                    val localTimestamp = historicoLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = historicoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (historico.veiculoId == 0L) {
                            Log.w(TAG, "⚠️ Histórico manutenção veículo ID $historicoId sem veiculoId - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        if (historicoLocal == null) {
                            appRepository.inserirHistoricoManutencao(historico)
                        } else {
                            appRepository.inserirHistoricoManutencao(historico) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Log.d(TAG, "✅ HistoricoManutencaoVeiculo sincronizado: Veículo ${historico.veiculoId} (ID: $historicoId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "⏭️ Histórico local mais recente ou igual, mantendo: ID=$historicoId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico manutenção veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull HistoricoManutencaoVeiculo (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental de histórico manutenção veículo: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de histórico manutenção veículo.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullHistoricoManutencaoVeiculoComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "📥 Pull COMPLETO de histórico manutenção veículo - documentos recebidos: ${snapshot.size()}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val todosHistoricos = appRepository.obterTodosHistoricoManutencaoVeiculo()
            val historicosCache = todosHistoricos.associateBy { it.id }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataManutencao = converterTimestampParaDate(data["dataManutencao"])
                        ?: converterTimestampParaDate(data["data_manutencao"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val historico = HistoricoManutencaoVeiculo(
                        id = historicoId,
                        veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: (data["veiculo_id"] as? Number)?.toLong() ?: 0L,
                        tipoManutencao = data["tipoManutencao"] as? String ?: (data["tipo_manutencao"] as? String) ?: "",
                        descricao = data["descricao"] as? String ?: "",
                        dataManutencao = dataManutencao,
                        valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                        kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: (data["km_veiculo"] as? Number)?.toLong() ?: 0L,
                        responsavel = data["responsavel"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    if (historico.veiculoId == 0L) {
                        skipCount++
                        return@forEach
                    }
                    
                    val historicoLocal = historicosCache[historicoId]
                    if (historicoLocal == null) {
                        appRepository.inserirHistoricoManutencao(historico)
                    } else {
                        appRepository.inserirHistoricoManutencao(historico) // REPLACE atualiza se existir
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico manutenção veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull HistoricoManutencaoVeiculo (COMPLETO) concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no pull completo de histórico manutenção veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoManutencaoVeiculo: Envia histórico de manutenção de veículos do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Historico Manutenção Veículo com sincronização incremental
     */
    private suspend fun pushHistoricoManutencaoVeiculo(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_MANUTENCAO_VEICULO
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de histórico manutenção veículo...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val historicosLocais = appRepository.obterTodosHistoricoManutencaoVeiculo()
            Log.d(TAG, "📥 Total de históricos de manutenção locais encontrados: ${historicosLocais.size}")
            
            val historicosParaEnviar = if (canUseIncremental) {
                historicosLocais.filter { historico ->
                    val historicoTimestamp = historico.dataCriacao.time
                    historicoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} históricos modificados desde ${Date(lastPushTimestamp)} (de ${historicosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${historicosLocais.size} históricos")
                historicosLocais
            }
            
            if (historicosParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            historicosParaEnviar.forEach { historico ->
                try {
                    Log.d(TAG, "📄 Processando histórico manutenção: ID=${historico.id}, Veículo=${historico.veiculoId}")
                    
                    val historicoMap = entityToMap(historico)
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    historicoMap["roomId"] = historico.id
                    historicoMap["id"] = historico.id
                    
                    // Adicionar metadados de sincronização
                    historicoMap["lastModified"] = FieldValue.serverTimestamp()
                    historicoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = historico.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_MANUTENCAO_VEICULO)
                    
                    val docRef = collectionRef.document(documentId)
                    
                    // 1. Escrever
                    docRef.set(historicoMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ HistoricoManutencaoVeiculo ${historico.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += historicoMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar histórico manutenção ${historico.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de histórico manutenção veículo concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de histórico manutenção veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Pull Historico Combustível Veículo com sincronização incremental
     * Segue melhores práticas Android 2025 com estratégia híbrida
     */
    private suspend fun pullHistoricoCombustivelVeiculo(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO
        
        return try {
            Log.d(TAG, "Iniciando pull de histórico combustível veículo...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullHistoricoCombustivelVeiculoIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosHistoricoCombustivelVeiculo().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há históricos locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 históricos mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullHistoricoCombustivelVeiculoComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullHistoricoCombustivelVeiculoComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de histórico combustível veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de histórico combustível veículo.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores práticas Android 2025 com estratégia híbrida.
     */
    private suspend fun tryPullHistoricoCombustivelVeiculoIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ ANDROID 2025: Estratégia híbrida para garantir que históricos não desapareçam
            resetRouteFilters()
            val todosHistoricos = appRepository.obterTodosHistoricoCombustivelVeiculo()
            val historicosCache = todosHistoricos.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de históricos combustível veículo carregado: ${historicosCache.size} históricos locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalHistoricos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todos os históricos: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há históricos locais, buscar TODOS
            val allHistoricos = if (incrementalHistoricos.isEmpty() && historicosCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 históricos mas há ${historicosCache.size} locais - buscando TODOS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todos os históricos: ${e.message}")
                    return null
                }
            } else {
                incrementalHistoricos
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allHistoricos.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allHistoricos.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando histórico combustível veículo: ID=${doc.id}")
                    
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataAbastecimento = converterTimestampParaDate(data["dataAbastecimento"])
                        ?: converterTimestampParaDate(data["data_abastecimento"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val historico = HistoricoCombustivelVeiculo(
                        id = historicoId,
                        veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: (data["veiculo_id"] as? Number)?.toLong() ?: 0L,
                        dataAbastecimento = dataAbastecimento,
                        litros = (data["litros"] as? Number)?.toDouble() ?: 0.0,
                        valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                        kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: (data["km_veiculo"] as? Number)?.toLong() ?: 0L,
                        kmRodado = (data["kmRodado"] as? Number)?.toDouble() ?: (data["km_rodado"] as? Number)?.toDouble() ?: 0.0,
                        posto = data["posto"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    // ✅ ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: historico.dataCriacao.time
                    val historicoLocal = historicosCache[historicoId]
                    val localTimestamp = historicoLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = historicoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (historico.veiculoId == 0L) {
                            Log.w(TAG, "⚠️ Histórico combustível veículo ID $historicoId sem veiculoId - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        if (historicoLocal == null) {
                            appRepository.inserirHistoricoCombustivel(historico)
                        } else {
                            appRepository.inserirHistoricoCombustivel(historico) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Log.d(TAG, "✅ HistoricoCombustivelVeiculo sincronizado: Veículo ${historico.veiculoId} (ID: $historicoId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "⏭️ Histórico local mais recente ou igual, mantendo: ID=$historicoId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico combustível veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull HistoricoCombustivelVeiculo (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental de histórico combustível veículo: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de histórico combustível veículo.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullHistoricoCombustivelVeiculoComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "📥 Pull COMPLETO de histórico combustível veículo - documentos recebidos: ${snapshot.size()}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val todosHistoricos = appRepository.obterTodosHistoricoCombustivelVeiculo()
            val historicosCache = todosHistoricos.associateBy { it.id }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    val historicoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataAbastecimento = converterTimestampParaDate(data["dataAbastecimento"])
                        ?: converterTimestampParaDate(data["data_abastecimento"]) ?: Date()
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val historico = HistoricoCombustivelVeiculo(
                        id = historicoId,
                        veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: (data["veiculo_id"] as? Number)?.toLong() ?: 0L,
                        dataAbastecimento = dataAbastecimento,
                        litros = (data["litros"] as? Number)?.toDouble() ?: 0.0,
                        valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                        kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: (data["km_veiculo"] as? Number)?.toLong() ?: 0L,
                        kmRodado = (data["kmRodado"] as? Number)?.toDouble() ?: (data["km_rodado"] as? Number)?.toDouble() ?: 0.0,
                        posto = data["posto"] as? String,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao
                    )
                    
                    if (historico.veiculoId == 0L) {
                        skipCount++
                        return@forEach
                    }
                    
                    val historicoLocal = historicosCache[historicoId]
                    if (historicoLocal == null) {
                        appRepository.inserirHistoricoCombustivel(historico)
                    } else {
                        appRepository.inserirHistoricoCombustivel(historico) // REPLACE atualiza se existir
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar histórico combustível veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull HistoricoCombustivelVeiculo (COMPLETO) concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no pull completo de histórico combustível veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ REFATORADO (2025): Pull Veículos com sincronização incremental
     * Segue melhores práticas Android 2025 com estratégia híbrida
     */
    private suspend fun pullVeiculos(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_VEICULOS
        
        return try {
            Log.d(TAG, "Iniciando pull de veículos...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_VEICULOS)
            
            // Verificar se podemos tentar sincronização incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronização incremental
                Log.d(TAG, "🔄 Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullVeiculosIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosVeiculos().first().size }.getOrDefault(0)
                    
                    // ✅ VALIDAÇÃO: Se incremental retornou 0 mas há veículos locais, forçar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "⚠️ Incremental retornou 0 veículos mas há $localCount locais - executando pull COMPLETO como validação")
                        return pullVeiculosComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar método completo
                    Log.w(TAG, "⚠️ Sincronização incremental falhou, usando método COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização - usando método COMPLETO")
            }
            
            // Método completo (sempre funciona, mesmo código que estava antes)
            pullVeiculosComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de veículos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ✅ NOVO (2025): Tenta sincronização incremental de veículos.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores práticas Android 2025 com estratégia híbrida.
     */
    private suspend fun tryPullVeiculosIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ✅ ANDROID 2025: Estratégia híbrida para garantir que veículos não desapareçam
            resetRouteFilters()
            val todosVeiculos = appRepository.obterTodosVeiculos().first()
            val veiculosCache = todosVeiculos.associateBy { it.id }
            Log.d(TAG, "   📦 Cache de veículos carregado: ${veiculosCache.size} veículos locais")
            
            // Tentar query incremental primeiro (otimização)
            val incrementalVeiculos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Query incremental falhou, buscando todos os veículos: ${e.message}")
                emptyList()
            }
            
            // ✅ CORREÇÃO: Se incremental retornou 0 mas há veículos locais, buscar TODOS
            val allVeiculos = if (incrementalVeiculos.isEmpty() && veiculosCache.isNotEmpty()) {
                Log.w(TAG, "⚠️ Incremental retornou 0 veículos mas há ${veiculosCache.size} locais - buscando TODOS para garantir sincronização")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar todos os veículos: ${e.message}")
                    return null
                }
            } else {
                incrementalVeiculos
            }
            
            Log.d(TAG, "📥 Sincronização INCREMENTAL: ${allVeiculos.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allVeiculos.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "📄 Processando veículo: ID=${doc.id}, Placa=${data["placa"]}")
                    
                    val veiculoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (veiculoId == 0L) {
                        Log.w(TAG, "⚠️ ID inválido para veículo ${doc.id} - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    val dataCompra = converterTimestampParaDate(data["dataCompra"])
                        ?: converterTimestampParaDate(data["data_compra"])
                    
                    val veiculo = Veiculo(
                        id = veiculoId,
                        nome = data["nome"] as? String ?: "",
                        placa = data["placa"] as? String ?: "",
                        marca = data["marca"] as? String ?: "",
                        modelo = data["modelo"] as? String ?: "",
                        anoModelo = (data["anoModelo"] as? Number)?.toInt()
                            ?: (data["ano_modelo"] as? Number)?.toInt() ?: 0,
                        kmAtual = (data["kmAtual"] as? Number)?.toLong()
                            ?: (data["km_atual"] as? Number)?.toLong() ?: 0L,
                        dataCompra = dataCompra,
                        observacoes = data["observacoes"] as? String
                    )
                    
                    // ✅ ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCompra"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: veiculo.dataCompra?.time ?: System.currentTimeMillis()
                    val veiculoLocal = veiculosCache[veiculoId]
                    val localTimestamp = veiculoLocal?.dataCompra?.time ?: 0L
                    
                    // Sincronizar se: não existe localmente OU servidor é mais recente OU foi modificado desde última sync
                    val shouldSync = veiculoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (veiculo.placa.isBlank()) {
                            Log.w(TAG, "⚠️ Veículo ID $veiculoId sem placa - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        if (veiculoLocal == null) {
                            appRepository.inserirVeiculo(veiculo)
                        } else {
                            appRepository.inserirVeiculo(veiculo) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Log.d(TAG, "✅ Veículo sincronizado: ${veiculo.placa} (ID: ${veiculo.id})")
                    } else {
                        skipCount++
                        Log.d(TAG, "⏭️ Veículo local mais recente ou igual, mantendo: ID=$veiculoId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronização
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Veiculos (INCREMENTAL) concluído:")
            Log.d(TAG, "   📊 $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ⏱️ Duração: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro na sincronização incremental de veículos: ${e.message}")
            null // Falhou, usar método completo
        }
    }
    
    /**
     * Método completo de sincronização de veículos.
     * Este é o método original que sempre funcionou - NÃO ALTERAR A LÓGICA DE PROCESSAMENTO.
     */
    private suspend fun pullVeiculosComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "📥 Pull COMPLETO de veículos - documentos recebidos: ${snapshot.size()}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val todosVeiculos = appRepository.obterTodosVeiculos().first()
            val veiculosCache = todosVeiculos.associateBy { it.id }
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    val veiculoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (veiculoId == 0L) {
                        skipCount++
                        return@forEach
                    }
                    
                    val dataCompra = converterTimestampParaDate(data["dataCompra"])
                        ?: converterTimestampParaDate(data["data_compra"])
                    
                    val veiculo = Veiculo(
                        id = veiculoId,
                        nome = data["nome"] as? String ?: "",
                        placa = data["placa"] as? String ?: "",
                        marca = data["marca"] as? String ?: "",
                        modelo = data["modelo"] as? String ?: "",
                        anoModelo = (data["anoModelo"] as? Number)?.toInt()
                            ?: (data["ano_modelo"] as? Number)?.toInt() ?: 0,
                        kmAtual = (data["kmAtual"] as? Number)?.toLong()
                            ?: (data["km_atual"] as? Number)?.toLong() ?: 0L,
                        dataCompra = dataCompra,
                        observacoes = data["observacoes"] as? String
                    )
                    
                    if (veiculo.placa.isBlank()) {
                        skipCount++
                        return@forEach
                    }
                    
                    val veiculoLocal = veiculosCache[veiculoId]
                    if (veiculoLocal == null) {
                        appRepository.inserirVeiculo(veiculo)
                    } else {
                        appRepository.inserirVeiculo(veiculo) // REPLACE atualiza se existir
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao processar veículo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronização completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "✅ Pull Veiculos (COMPLETO) concluído: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no pull completo de veículos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoCombustivelVeiculo: Envia histórico de combustível de veículos modificado do Room para o Firestore
     * TODO: Adicionar método obterTodosHistoricoCombustivelVeiculo() no AppRepository
     */
    /**
     * Push HistoricoCombustivelVeiculo: Envia histórico de combustível de veículos do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Historico Combustível Veículo com sincronização incremental
     */
    private suspend fun pushHistoricoCombustivelVeiculo(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de histórico combustível veículo...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val historicosLocais = appRepository.obterTodosHistoricoCombustivelVeiculo()
            Log.d(TAG, "📥 Total de históricos de combustível locais encontrados: ${historicosLocais.size}")
            
            val historicosParaEnviar = if (canUseIncremental) {
                historicosLocais.filter { historico ->
                    val historicoTimestamp = historico.dataCriacao.time
                    historicoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} históricos modificados desde ${Date(lastPushTimestamp)} (de ${historicosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${historicosLocais.size} históricos")
                historicosLocais
            }
            
            if (historicosParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            historicosParaEnviar.forEach { historico ->
                try {
                    Log.d(TAG, "📄 Processando histórico combustível: ID=${historico.id}, Veículo=${historico.veiculoId}")
                    
                    val historicoMap = entityToMap(historico)
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    historicoMap["roomId"] = historico.id
                    historicoMap["id"] = historico.id
                    
                    // Adicionar metadados de sincronização
                    historicoMap["lastModified"] = FieldValue.serverTimestamp()
                    historicoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = historico.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO)
                    
                    val docRef = collectionRef.document(documentId)
                    
                    // 1. Escrever
                    docRef.set(historicoMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ HistoricoCombustivelVeiculo ${historico.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += historicoMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar histórico combustível ${historico.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de histórico combustível veículo concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de histórico combustível veículo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Veiculos: Envia veículos do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Veiculos com sincronização incremental
     * Segue melhores práticas Android 2025: não altera dados locais durante exportação
     */
    private suspend fun pushVeiculos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_VEICULOS
        
        return try {
            Log.d(TAG, "📤 Iniciando push INCREMENTAL de veículos...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val veiculosLocais = appRepository.obterTodosVeiculos().first()
            Log.d(TAG, "📊 Total de veículos locais encontrados: ${veiculosLocais.size}")
            
            // ✅ Filtrar apenas veículos modificados desde último push (usar dataCompra ou timestamp)
            val veiculosParaEnviar = if (canUseIncremental) {
                veiculosLocais.filter { veiculo ->
                    val veiculoTimestamp = veiculo.dataCompra?.time ?: System.currentTimeMillis()
                    veiculoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} veículos modificados desde ${Date(lastPushTimestamp)} (de ${veiculosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todos os ${veiculosLocais.size} veículos")
                veiculosLocais
            }
            
            if (veiculosParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            veiculosParaEnviar.forEach { veiculo ->
                try {
                    Log.d(TAG, "📄 Processando veículo: ID=${veiculo.id}, Nome=${veiculo.nome}, Placa=${veiculo.placa}")
                    
                    val veiculoMap = entityToMap(veiculo)
                    Log.d(TAG, "   Mapa criado com ${veiculoMap.size} campos")
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    veiculoMap["roomId"] = veiculo.id
                    veiculoMap["id"] = veiculo.id
                    
                    // Adicionar metadados de sincronização
                    veiculoMap["lastModified"] = FieldValue.serverTimestamp()
                    veiculoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = veiculo.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_VEICULOS)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$EMPRESA_ID/entidades/${COLLECTION_VEICULOS}/items, document=$documentId")
                    
                    val docRef = collectionRef.document(documentId)
                    
                    // 1. Escrever
                    docRef.set(veiculoMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ✅ CORREÇÃO CRÍTICA: NÃO alterar dados locais durante exportação (push)
                    // Os dados locais devem permanecer inalterados na exportação
                    // A atualização dos dados locais acontece apenas na importação (pull)
                    // quando há dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "✅ Veiculo ${veiculo.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += veiculoMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar veículo ${veiculo.id} (${veiculo.nome}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de veículos concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de veículos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Meta Colaborador: Envia metas de colaborador do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Meta Colaborador com sincronização incremental
     */
    private suspend fun pushMetaColaborador(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_META_COLABORADOR
        
        return try {
            Log.d(TAG, "🔵 Iniciando push INCREMENTAL de meta colaborador...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val metasLocais = appRepository.obterTodasMetaColaborador().first()
            Log.d(TAG, "📥 Total de meta colaborador locais encontradas: ${metasLocais.size}")
            
            val metasParaEnviar = if (canUseIncremental) {
                metasLocais.filter { meta ->
                    val metaTimestamp = meta.dataCriacao.time
                    metaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "🔄 Push INCREMENTAL: ${it.size} metas modificadas desde ${Date(lastPushTimestamp)} (de ${metasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização PUSH - enviando todas as ${metasLocais.size} metas")
                metasLocais
            }
            
            if (metasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            metasParaEnviar.forEach { meta ->
                try {
                    Log.d(TAG, "📄 Processando meta colaborador: ID=${meta.id}, Tipo=${meta.tipoMeta}, ColaboradorId=${meta.colaboradorId}")
                    
                    val metaMap = entityToMap(meta)
                    Log.d(TAG, "   Mapa criado com ${metaMap.size} campos")
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    metaMap["roomId"] = meta.id
                    metaMap["id"] = meta.id
                    
                    // Adicionar metadados de sincronização
                    metaMap["lastModified"] = FieldValue.serverTimestamp()
                    metaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = meta.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_META_COLABORADOR)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$EMPRESA_ID/entidades/${COLLECTION_META_COLABORADOR}/items, document=$documentId")
                    
                    collectionRef
                        .document(documentId)
                        .set(metaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += metaMap.toString().length.toLong()
                    Log.d(TAG, "✅ Meta colaborador enviada com sucesso: ${meta.tipoMeta} (ID: ${meta.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar meta colaborador ${meta.id} (${meta.tipoMeta}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push INCREMENTAL de meta colaborador concluído: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de meta colaborador: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Meta Colaborador: Sincroniza metas de colaborador do Firestore para o Room
     */
    private suspend fun pullMetaColaborador(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_META_COLABORADOR
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de meta colaborador...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_META_COLABORADOR)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullMetaColaboradorIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de meta colaborador falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de meta colaborador - usando método COMPLETO")
            }
            
            pullMetaColaboradorComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de meta colaborador: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullMetaColaboradorComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "📥 Pull COMPLETO de meta colaborador - documentos recebidos: ${documents.size}")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val metasCache = appRepository.obterTodasMetaColaborador().first()
                .associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processMetaColaboradorDocuments(documents, metasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de meta colaborador concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de meta colaborador: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullMetaColaboradorIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao executar query incremental para meta colaborador: ${e.message}")
                return null
            }
            Log.d(TAG, "📥 Meta colaborador - incremental retornou ${documents.size} documentos (após filtro de rota)")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val metasCache = appRepository.obterTodasMetaColaborador().first()
                .associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processMetaColaboradorDocuments(documents, metasCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de meta colaborador: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de meta colaborador: ${e.message}", e)
            null
        }
    }

    private suspend fun processMetaColaboradorDocuments(
        documents: List<DocumentSnapshot>,
        metasCache: MutableMap<Long, MetaColaborador>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processMetaColaboradorDocument(doc, metasCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processMetaColaboradorDocument(
        doc: DocumentSnapshot,
        metasCache: MutableMap<Long, MetaColaborador>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
            val metaId = (data["roomId"] as? Number)?.toLong()
                ?: (data["id"] as? Number)?.toLong()
                ?: doc.id.toLongOrNull()
                ?: return ProcessResult.Skipped
                    val colaboradorId = (data["colaboradorId"] as? Number)?.toLong()
                ?: (data["colaborador_id"] as? Number)?.toLong()
                ?: return ProcessResult.Skipped
                    val cicloId = (data["cicloId"] as? Number)?.toLong()
                        ?: (data["ciclo_id"] as? Number)?.toLong() ?: 0L
                    val rotaId = (data["rotaId"] as? Number)?.toLong()
                        ?: (data["rota_id"] as? Number)?.toLong()
                    
            if (!shouldSyncRouteData(rotaId, allowUnknown = rotaId == null)) {
                return ProcessResult.Skipped
            }
                    
            val tipoMetaStr = data["tipoMeta"] as? String ?: data["tipo_meta"] as? String ?: return ProcessResult.Skipped
                    val tipoMeta = try {
                TipoMeta.valueOf(tipoMetaStr)
            } catch (_: Exception) {
                return ProcessResult.Skipped
                    }
                    
                    val dataCriacao = converterTimestampParaDate(data["dataCriacao"])
                        ?: converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
            val meta = MetaColaborador(
                        id = metaId,
                        colaboradorId = colaboradorId,
                        tipoMeta = tipoMeta,
                        valorMeta = (data["valorMeta"] as? Number)?.toDouble()
                            ?: (data["valor_meta"] as? Number)?.toDouble() ?: 0.0,
                        cicloId = cicloId,
                        rotaId = rotaId,
                        valorAtual = (data["valorAtual"] as? Number)?.toDouble()
                            ?: (data["valor_atual"] as? Number)?.toDouble() ?: 0.0,
                        ativo = data["ativo"] as? Boolean ?: true,
                        dataCriacao = dataCriacao
                    )
                    
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                ?: dataCriacao.time
            val metaLocal = metasCache[metaId]
            val localTimestamp = metaLocal?.dataCriacao?.time ?: 0L
            
            return if (metaLocal == null || serverTimestamp > localTimestamp) {
                    appRepository.inserirMeta(meta)
                metasCache[metaId] = meta
                ProcessResult.Synced
            } else {
                ProcessResult.Skipped
            }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar meta colaborador ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Push Equipments: Envia equipamentos do Room para o Firestore
     */
    /**
     * ✅ REFATORADO (2025): Push Equipments com sincronização incremental
     * Nota: Equipment não tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    private suspend fun pushEquipments(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_EQUIPMENTS
        
        return try {
            Log.d(TAG, "🔵 Iniciando push de equipamentos...")
            // Nota: Equipment não tem campo de timestamp, então sempre enviar todos
            // (geralmente são poucos registros, impacto baixo)
            val equipmentsLocais = appRepository.obterTodosEquipments().first()
            Log.d(TAG, "📥 Total de equipamentos locais encontrados: ${equipmentsLocais.size}")
            
            if (equipmentsLocais.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            equipmentsLocais.forEach { equipment ->
                try {
                    Log.d(TAG, "📄 Processando equipamento: ID=${equipment.id}, Nome=${equipment.name}")
                    
                    val equipmentMap = entityToMap(equipment)
                    Log.d(TAG, "   Mapa criado com ${equipmentMap.size} campos")
                    
                    // ✅ CRÍTICO: Adicionar roomId para compatibilidade com pull
                    equipmentMap["roomId"] = equipment.id
                    equipmentMap["id"] = equipment.id
                    
                    // Adicionar metadados de sincronização
                    equipmentMap["lastModified"] = FieldValue.serverTimestamp()
                    equipmentMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = equipment.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_EQUIPMENTS)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$EMPRESA_ID/entidades/${COLLECTION_EQUIPMENTS}/items, document=$documentId")
                    
                    collectionRef
                        .document(documentId)
                        .set(equipmentMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += equipmentMap.toString().length.toLong()
                    Log.d(TAG, "✅ Equipamento enviado com sucesso: ${equipment.name} (ID: ${equipment.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Erro ao enviar equipamento ${equipment.id} (${equipment.name}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "✅ Push de equipamentos concluído: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "❌ Erro no push de equipamentos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Equipments: Sincroniza equipamentos do Firestore para o Room
     */
    private suspend fun pullEquipments(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_EQUIPMENTS
        
        return try {
            Log.d(TAG, "🔵 Iniciando pull de equipamentos...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_EQUIPMENTS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullEquipmentsIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "⚠️ Sincronização incremental de equipamentos falhou, usando método COMPLETO")
                }
            } else {
                Log.d(TAG, "🔄 Primeira sincronização de equipamentos - usando método COMPLETO")
            }
            
            pullEquipmentsComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull de equipamentos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pullEquipmentsComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "📥 Pull COMPLETO de equipamentos - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val equipmentsCache = appRepository.obterTodosEquipments().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processEquipmentsDocuments(snapshot.documents, equipmentsCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização" else null
            )
            
            Log.d(TAG, "✅ Pull de equipamentos concluído: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull completo de equipamentos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryPullEquipmentsIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long
    ): Result<Int>? {
        return try {
            val incrementalQuery = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao criar query incremental para equipamentos: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "📥 Equipamentos - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            val equipmentsCache = appRepository.obterTodosEquipments().first().associateBy { it.id }.toMutableMap()
            val (syncCount, skippedCount, errorCount) = processEquipmentsDocuments(documents, equipmentsCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronização incremental" else null
            )
            
            Log.d(TAG, "✅ Pull INCREMENTAL de equipamentos: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no pull incremental de equipamentos: ${e.message}", e)
            null
        }
    }

    private suspend fun processEquipmentsDocuments(
        documents: List<DocumentSnapshot>,
        equipmentsCache: MutableMap<Long, com.example.gestaobilhares.data.entities.Equipment>
    ): Triple<Int, Int, Int> {
            var syncCount = 0
        var skippedCount = 0
            var errorCount = 0
            
        documents.forEach { doc ->
            when (processEquipmentDocument(doc, equipmentsCache)) {
                ProcessResult.Synced -> syncCount++
                ProcessResult.Skipped -> skippedCount++
                ProcessResult.Error -> errorCount++
            }
        }
        
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processEquipmentDocument(
        doc: DocumentSnapshot,
        equipmentsCache: MutableMap<Long, com.example.gestaobilhares.data.entities.Equipment>
    ): ProcessResult {
        return try {
            val data = doc.data ?: return ProcessResult.Skipped
            val equipmentId = (data["roomId"] as? Number)?.toLong()
                ?: (data["id"] as? Number)?.toLong()
                ?: doc.id.toLongOrNull()
                ?: return ProcessResult.Skipped
                    
                    val equipment = com.example.gestaobilhares.data.entities.Equipment(
                        id = equipmentId,
                        name = data["name"] as? String ?: "",
                        description = data["description"] as? String,
                        quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                        location = data["location"] as? String
                    )
                    
                    if (equipment.name.isBlank()) {
                return ProcessResult.Skipped
                    }
                    
                    appRepository.inserirEquipment(equipment)
            equipmentsCache[equipmentId] = equipment
            ProcessResult.Synced
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao processar equipamento ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
}

/**
 * Operação de sincronização enfileirada.
 */
data class SyncOperation(
    val id: Long,
    val type: SyncOperationType,
    val entityType: String,
    val entityId: String,
    val data: String, // JSON serializado
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

/**
 * Tipos de operação de sincronização.
 */
enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE
}

data class SyncProgress(
    val percent: Int,
    val message: String
)

/**
 * Classe utilitária para rastrear o progresso da sincronização.
 */
class ProgressTracker(
    private val totalSteps: Int,
    private val listener: ((SyncProgress) -> Unit)?
) {
    private var completedSteps = 0

    fun start() {
        listener?.invoke(SyncProgress(0, "Preparando sincronização..."))
    }

    fun advance(message: String) {
        if (totalSteps == 0) return
        completedSteps++
        val percent = ((completedSteps.toDouble() / totalSteps) * 100).roundToInt().coerceIn(0, 100)
        listener?.invoke(SyncProgress(percent, message))
    }

    fun complete() {
        listener?.invoke(SyncProgress(100, "Sincronização concluída"))
    }

    fun completeWithMessage(message: String) {
        listener?.invoke(SyncProgress(100, message))
    }
}


