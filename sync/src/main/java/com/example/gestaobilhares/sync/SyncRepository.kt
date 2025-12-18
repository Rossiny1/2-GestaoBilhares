package com.example.gestaobilhares.sync

import android.content.Context
import android.util.Log
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.core.utils.UserSessionManager
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
 * Repository especializado para sincroniza��o de dados.
 * Segue arquitetura h�brida modular: AppRepository como Facade.
 * 
 * Responsabilidades:
 * - Sincroniza��o bidirecional (Pull/Push) com Firebase Firestore
 * - Fila de sincroniza��o offline-first
 * - Gerenciamento de conflitos
 * - Status de sincroniza��o
 */
class SyncRepository(
    private val context: Context,
    private val appRepository: AppRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val networkUtils: NetworkUtils = NetworkUtils(context),
    private val userSessionManager: UserSessionManager = UserSessionManager.getInstance(context),
    private val firebaseImageUploader: FirebaseImageUploader = FirebaseImageUploader(context),
    private val syncMetadataDao: SyncMetadataDao = AppDatabase.getDatabase(context).syncMetadataDao()
) {
    private var accessibleRouteIdsCache: Set<Long>? = null
    private var allowRouteBootstrap = false
    private val clienteRotaCache = mutableMapOf<Long, Long?>()
    private val mesaRotaCache = mutableMapOf<Long, Long?>()
    
    // ? FIX: Mover inicializa��o est�tica para evitar ExceptionInInitializerError
    private val gson: Gson by lazy { 
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create() 
    }
    private val mapType: java.lang.reflect.Type by lazy { 
        object : TypeToken<Map<String, Any?>>() {}.type 
    }
    
    // ? NOVO: ID da empresa din�mico vindo da sess�o
    private val currentCompanyId: String
        get() = userSessionManager.getCurrentCompanyId()

    init {
        Log.d(TAG, "SyncRepository inicializado (Empresa: $currentCompanyId)")
        Log.d(TAG, "NetworkUtils.isConnected() inicial = ${networkUtils.isConnected()}")
    }
    
    /**
     * ? HELPER: Retorna CollectionReference para uma entidade
     * Usa a inst�ncia de firestore da classe e o companyId din�mico
     */
    private fun getCollectionRef(collectionName: String): CollectionReference {
        val companyId = currentCompanyId
        Log.d(TAG, "?? getCollectionRef: $collectionName (Empresa: $companyId)")
        return getCollectionReference(firestore, collectionName, companyId)
    }
    
    companion object {
        private const val TAG = "SyncRepository"
        private const val GLOBAL_SYNC_METADATA = "_global_sync"
        private const val ONE_HOUR_IN_MS = 60 * 60 * 1000L
        private const val DEFAULT_BACKGROUND_IDLE_HOURS = 6L
        private const val FIRESTORE_WHERE_IN_LIMIT = 10
        private const val FIELD_ROTA_ID = "rotaId"
        
        // Estrutura hier�rquica do Firestore: /empresas/{empresaId}/{entidade}
        private const val COLLECTION_EMPRESAS = "empresas"
        
        // Nomes das cole��es (subcole��es dentro de empresas/{empresa_id})
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
        // Novas cole��es para entidades faltantes
        private const val COLLECTION_CATEGORIAS_DESPESA = "categorias_despesa"
        private const val COLLECTION_TIPOS_DESPESA = "tipos_despesa"
        private const val COLLECTION_METAS = "metas"
        private const val COLLECTION_COLABORADOR_ROTA = "colaborador_rota"
        private const val COLLECTION_ADITIVO_MESAS = "aditivo_mesas"
        private const val COLLECTION_CONTRATO_MESAS = "contrato_mesas"
        private const val COLLECTION_LOGS_AUDITORIA = "logs_auditoria_assinatura"
        // Cole��es para entidades adicionais
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
        
        /**
         * Retorna a refer�ncia da cole��o de uma entidade dentro da estrutura hier�rquica.
         * Caminho: empresas/{companyId}/entidades/{collectionName}/items
         */
        fun getCollectionReference(
            firestore: FirebaseFirestore, 
            collectionName: String, 
            companyId: String = "empresa_001"
        ): CollectionReference {
            return firestore
                .collection(COLLECTION_EMPRESAS)
                .document(companyId)
                .collection("entidades")
                .document(collectionName)
                .collection("items")
        }
        
        /**
         * ? M�TODO LEGADO: Mantido para compatibilidade
         */
        @Deprecated("Use getCollectionReference() com companyId", ReplaceWith("getCollectionReference(firestore, collectionName, companyId)"))
        fun getCollectionPath(collectionName: String, companyId: String = "empresa_001"): String {
            return "$COLLECTION_EMPRESAS/$companyId/entidades/$collectionName"
        }
    }

    internal fun documentToAcerto(doc: DocumentSnapshot): Acerto? {
        val acertoData = doc.data?.toMutableMap() ?: run {
            Log.w(TAG, "?? Acerto ${doc.id} sem dados")
            return null
        }

        val acertoId = doc.id.toLongOrNull() ?: run {
            Log.w(TAG, "?? Acerto ${doc.id} com ID inv�lido")
            return null
        }

        val clienteIdNormalizado = extrairClienteId(acertoData)
        if (clienteIdNormalizado == null || clienteIdNormalizado <= 0L) {
            Log.e(TAG, "? Acerto $acertoId sem clienteId v�lido (dados brutos: ${acertoData["clienteId"] ?: acertoData["cliente_id"] ?: acertoData["clienteID"]})")
            return null
        }

        // ? Garantir compatibilidade: manter ambas as chaves (camelCase e snake_case)
        acertoData["clienteId"] = clienteIdNormalizado
        acertoData["cliente_id"] = clienteIdNormalizado

        val acertoJson = gson.toJson(acertoData)
        val acertoFirestore = gson.fromJson(acertoJson, Acerto::class.java)?.copy(
            id = acertoId,
            clienteId = clienteIdNormalizado
        )

        if (acertoFirestore == null) {
            Log.e(TAG, "? Falha ao converter acerto $acertoId do JSON")
            return null
        }

        Log.d(TAG, "? Acerto convertido: ID=${acertoFirestore.id}, clienteId=${acertoFirestore.clienteId}")
        return acertoFirestore
    }

    internal fun extrairDataAcertoMillis(doc: DocumentSnapshot): Long {
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
                    Log.w(TAG, "?? dataAcerto n�o � Timestamp (doc=${doc.id}): ${ex.message}")
                    0L
                }
            }
        }
    }

    internal fun parseDataAcertoString(value: String): Long {
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

        Log.w(TAG, "?? N�o foi poss�vel converter dataAcerto '$value' usando formatos conhecidos")
        return 0L
    }

    internal fun extrairClienteId(acertoData: Map<String, Any?>): Long? {
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
            Log.w(TAG, "?? Erro ao manter hist�rico de acertos local para cliente $clienteId: ${e.message}")
        }
    }

    suspend fun fetchAcertosPorPeriodo(clienteId: Long, inicio: Date, fim: Date): List<Acerto> {
        return try {
            val collectionRef = getCollectionRef(COLLECTION_ACERTOS)
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
            Log.e(TAG, "? Erro ao buscar acertos por per�odo: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchUltimosAcertos(clienteId: Long, limit: Int): List<Acerto> {
        return try {
            Log.d(TAG, "?? Buscando �ltimos $limit acertos para cliente $clienteId no Firestore...")
            val collectionRef = getCollectionRef(COLLECTION_ACERTOS)
            val snapshot = queryAcertosPorCampoCliente(
                collectionRef = collectionRef,
                clienteIdentifier = clienteId,
                limit = limit
            )

            val acertos = snapshot.mapNotNull { documentToAcerto(it) }
            Log.d(TAG, "? Busca conclu�da: ${acertos.size} acertos encontrados para cliente $clienteId (de ${snapshot.size} documentos do Firestore)")
            if (acertos.size < snapshot.size) {
                Log.w(TAG, "?? ATENCAO: ${snapshot.size - acertos.size} documentos do Firestore nao foram convertidos para Acerto (possivel problema na conversao)")
            }
            acertos
        } catch (e: Exception) {
            Log.e(TAG, "? Erro ao buscar �ltimos acertos para cliente $clienteId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * ? MELHORADO (2025): Estrat�gia de fallback robusta para buscar acertos por cliente.
     * 
     * Estrat�gia:
     * 1. Tenta query com orderBy (requer �ndice composto) - mais eficiente
     * 2. Se falhar, tenta query sem orderBy (n�o requer �ndice) - busca todos e ordena em mem�ria
     * 3. Tenta m�ltiplos campos de cliente (clienteId, cliente_id, clienteID)
     * 4. Tenta valores num�ricos e string
     * 
     * Isso garante que sempre funciona, mesmo sem �ndices no Firestore.
     */
    private suspend fun queryAcertosPorCampoCliente(
        collectionRef: CollectionReference,
        clienteIdentifier: Long,
        limit: Int?,
        builder: ((Query) -> Query)? = null
    ): List<DocumentSnapshot> {
        // 1) Tentar campos num�ricos com orderBy (requer �ndice, mas � mais eficiente)
        queryAcertosComCampos(collectionRef, clienteIdentifier, limit, builder)?.let { return it }
        
        // 2) Tentar campos num�ricos SEM orderBy (n�o requer �ndice, ordena em mem�ria)
        queryAcertosSemOrderBy(collectionRef, clienteIdentifier, limit)?.let { return it }
        
        // 3) Fallback para campos armazenados como string com orderBy
        queryAcertosComCampos(collectionRef, clienteIdentifier.toString(), limit, builder)?.let { return it }
        
        // 4) Fallback para campos string SEM orderBy
        queryAcertosSemOrderBy(collectionRef, clienteIdentifier.toString(), limit)?.let { return it }
        
        // Se tudo falhar, retorna vazio
        Log.w(TAG, "?? N�o foi poss�vel buscar acertos para cliente $clienteIdentifier com nenhuma estrat�gia")
        return emptyList()
    }

    /**
     * ? NOVO: Busca acertos SEM orderBy (n�o requer �ndice composto).
     * Busca todos os acertos do cliente e ordena em mem�ria.
     * 
     * Esta � uma estrat�gia de fallback quando a query com orderBy falha por falta de �ndice.
     */
    private suspend fun queryAcertosSemOrderBy(
        collectionRef: CollectionReference,
        fieldValue: Any,
        limit: Int?
    ): List<DocumentSnapshot>? {
        Log.d(TAG, "?? Tentando buscar acertos sem orderBy para cliente $fieldValue (limit: $limit)")
        for (field in CLIENTE_ID_FIELDS) {
            try {
                Log.d(TAG, "   Tentando campo '$field' com valor '$fieldValue' (tipo: ${fieldValue::class.simpleName})")
                // Query simples: apenas whereEqualTo (N�O requer �ndice composto)
                var query: Query = collectionRef.whereEqualTo(field, fieldValue)
                
                // Buscar todos os documentos (sem limit no Firestore para evitar problemas)
                val snapshot = query.get().await()
                
                Log.d(TAG, "   Resultado da query '$field=$fieldValue': ${snapshot.size()} documentos encontrados")
                
                if (!snapshot.isEmpty) {
                    Log.d(TAG, "? Acertos encontrados usando campo '$field' sem orderBy (${snapshot.size()} docs) - ordenando em mem�ria")
                    
                    // Log dos primeiros documentos para debug
                    snapshot.documents.take(3).forEachIndexed { index, doc ->
                        val acertoData = doc.data
                        val clienteIdValue = acertoData?.get("clienteId") ?: acertoData?.get("cliente_id") ?: acertoData?.get("clienteID")
                        Log.d(TAG, "   Doc[$index] ID=${doc.id}, clienteId no doc=$clienteIdValue (tipo: ${clienteIdValue?.javaClass?.simpleName})")
                    }
                    
                    // Ordenar em mem�ria por dataAcerto (descendente)
                    val documentosOrdenados = snapshot.documents.sortedByDescending { doc ->
                        extrairDataAcertoMillis(doc)
                    }
                    
                    // Aplicar limit ap�s ordena��o
                    val resultado = if (limit != null && limit > 0) {
                        documentosOrdenados.take(limit)
                    } else {
                        documentosOrdenados
                    }
                    
                    Log.d(TAG, "   ?? Retornando ${resultado.size} acertos ordenados (de ${documentosOrdenados.size} total)")
                    return resultado
                } else {
                    Log.d(TAG, "   ?? Query '$field=$fieldValue' retornou vazio (0 documentos)")
                }
            } catch (ex: FirebaseFirestoreException) {
                if (ex.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    // Mesmo sem orderBy, pode falhar se o campo n�o existir
                    Log.w(TAG, "?? Campo '$field' com valor '$fieldValue' retornou FAILED_PRECONDITION: ${ex.message}")
                } else {
                    Log.e(TAG, "? Erro ao consultar acertos sem orderBy ($field=$fieldValue): ${ex.message}", ex)
                }
            } catch (e: Exception) {
                Log.e(TAG, "? Erro inesperado ao buscar acertos sem orderBy ($field=$fieldValue): ${e.message}", e)
                Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
            }
        }
        Log.w(TAG, "?? Nenhum acerto encontrado para cliente $fieldValue ap�s tentar todos os campos: ${CLIENTE_ID_FIELDS.joinToString()}")
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
                    Log.d(TAG, "? Acertos encontrados usando campo '$field' com valor '$fieldValue' (${snapshot.size()} docs)")
                    return snapshot.documents
                }
            } catch (ex: FirebaseFirestoreException) {
                if (ex.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.w(TAG, "?? Campo '$field' com valor '$fieldValue' sem �ndice para consulta: ${ex.message}")
                    // N�o retorna null aqui, continua tentando outros campos/estrat�gias
                } else {
                    Log.e(TAG, "? Erro ao consultar acertos ($field=$fieldValue): ${ex.message}", ex)
                }
            }
        }
        return null
    }
    
    // ==================== STATEFLOW - STATUS DE SINCRONIZA��O ====================
    
    /**
     * Status atual da sincroniza��o
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
    
    // ==================== HELPERS PARA SINCRONIZA��O INCREMENTAL (2025) ====================
    
    /**
     * ? NOVO (2025): Obt�m timestamp da �ltima sincroniza��o para um tipo de entidade.
     * Retorna 0L se nunca foi sincronizado (primeira sincroniza��o completa).
     * 
     * Segue melhores pr�ticas Android 2025 para sincroniza��o incremental.
     */
    internal suspend fun getLastSyncTimestamp(entityType: String): Long {
        return try {
            syncMetadataDao.obterUltimoTimestamp(entityType)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro ao obter timestamp de sincroniza��o para $entityType: ${e.message}")
            0L // Retorna 0 para primeira sincroniza��o completa
        }
    }
    
    /**
     * ? NOVO (2025): Obt�m timestamp da �ltima sincroniza��o PUSH para um tipo de entidade.
     * Usa sufixo "_push" para diferenciar de PULL.
     * Retorna 0L se nunca foi feito push (primeira sincroniza��o completa).
     * 
     * Segue melhores pr�ticas Android 2025 para sincroniza��o incremental.
     */
    private suspend fun getLastPushTimestamp(entityType: String): Long {
        return try {
            syncMetadataDao.obterUltimoTimestamp("${entityType}_push")
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro ao obter timestamp de push para $entityType: ${e.message}")
            0L // Retorna 0 para primeira sincroniza��o completa
        }
    }
    
    /**
     * ? NOVO (2025): Salva metadata de sincroniza��o PUSH ap�s sincroniza��o bem-sucedida.
     * Usa sufixo "_push" para diferenciar de PULL.
     * 
     * @param entityType Tipo da entidade (ex: "clientes", "mesas")
     * @param syncCount Quantidade de registros sincronizados
     * @param durationMs Dura��o da sincroniza��o em milissegundos
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
     * ? NOVO (2025): Salva metadata de sincroniza��o ap�s sincroniza��o bem-sucedida.
     * 
     * @param entityType Tipo da entidade (ex: "clientes", "mesas")
     * @param syncCount Quantidade de registros sincronizados
     * @param durationMs Dura��o da sincroniza��o em milissegundos
     * @param bytesDownloaded Bytes baixados (opcional)
     * @param bytesUploaded Bytes enviados (opcional)
     * @param error Erro ocorrido, se houver (null se sucesso)
     */
    internal suspend fun saveSyncMetadata(
        entityType: String,
        syncCount: Int,
        durationMs: Long,
        bytesDownloaded: Long = 0L,
        bytesUploaded: Long = 0L,
        error: String? = null,
        timestampOverride: Long? = null // ? NOVO: permite for�ar timestamp espec�fico (capturado antes do push)
    ) {
        try {
            // ? CORRE��O CR�TICA: Usar timestampOverride se fornecido, caso contr�rio usar atual
            // Isso resolve o problema onde timestamp era salvo AP�S pull, perdendo dados do push
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
            Log.d(TAG, "? Metadata de sincroniza��o salva para $entityType: $syncCount registros em ${durationMs}ms, $timestampInfo")
        } catch (e: Exception) {
            Log.e(TAG, "? Erro ao salvar metadata de sincroniza��o para $entityType: ${e.message}", e)
        }
    }

    /**
     * Determina se vale a pena acionar a sincroniza��o em background.
     * Crit�rios:
     * - Existem opera��es pendentes/falhadas na fila
     * - �ltima sincroniza��o global ocorreu h� mais de [maxIdleHours]
     */
    suspend fun shouldRunBackgroundSync(
        pendingThreshold: Int = 0,
        maxIdleHours: Long = DEFAULT_BACKGROUND_IDLE_HOURS
    ): Boolean {
        val pendingOps = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
        if (pendingOps > pendingThreshold) {
            Log.d(TAG, "?? Executando sync em background: $pendingOps opera��es pendentes")
            return true
        }

        val failedOps = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
        if (failedOps > 0) {
            Log.d(TAG, "?? Executando sync em background: $failedOps opera��es falhadas aguardando retry")
            return true
        }

        val lastGlobalSync = runCatching { syncMetadataDao.obterUltimoTimestamp(GLOBAL_SYNC_METADATA) }.getOrDefault(0L)
        if (lastGlobalSync == 0L) {
            Log.d(TAG, "?? Nenhum registro de sincroniza��o global - executar agora")
            return true
        }

        val hoursSinceLastSync = (System.currentTimeMillis() - lastGlobalSync) / ONE_HOUR_IN_MS
        return if (hoursSinceLastSync >= maxIdleHours) {
            Log.d(TAG, "?? �ltima sincroniza��o global h� $hoursSinceLastSync h (limite $maxIdleHours h) - executar")
            true
        } else {
            Log.d(TAG, "?? Sincroniza��o em background dispensada (pendentes=$pendingOps, horas=$hoursSinceLastSync)")
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
            Log.d(TAG, "?? ADMIN: Bootstrap desabilitado, acessando todas as rotas")
            return accessibleRouteIdsCache!!
        }

        val userId = userSessionManager.getCurrentUserId()
        val routes = userSessionManager.getUserAccessibleRoutes(context)
        val hasLocalAssignments = userSessionManager.hasAnyRouteAssignments(context)

        Log.d(TAG, "?? USER ID $userId: rotas acess�veis=${routes.size}, tem atribui��es locais=$hasLocalAssignments, bootstrap ser�=${routes.isEmpty() && !hasLocalAssignments}")

        allowRouteBootstrap = routes.isEmpty() && !hasLocalAssignments
        accessibleRouteIdsCache = routes.toSet()

        if (allowRouteBootstrap) {
            Log.w(TAG, "?? Usu�rio ID $userId sem rotas locais sincronizadas ainda. Aplicando bootstrap tempor�rio sem filtro de rota.")
        } else if (routes.isNotEmpty()) {
            Log.d(TAG, "? Usu�rio ID $userId tem ${routes.size} rotas atribu�das: ${routes.joinToString()}")
        } else {
            Log.w(TAG, "?? Usu�rio ID $userId sem rotas atribu�das e sem dados locais - nenhum dado ser� sincronizado")
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
        // ? CORRE��O: Durante bootstrap, permitir todas as rotas temporariamente
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
     * ? NOVO (2025): Valida que uma entidade referenciada existe localmente.
     * Se n�o existir, tenta buscar do Firestore.
     * 
     * @param entityType Tipo da entidade ("cliente", "mesa", "contrato")
     * @param entityId ID da entidade
     * @return true se a entidade existe localmente, false caso contr�rio
     */
    internal suspend fun ensureEntityExists(
        entityType: String,
        entityId: Long
    ): Boolean {
        return try {
            when (entityType) {
                "cliente" -> {
                    val exists = appRepository.obterClientePorId(entityId) != null
                    if (!exists) {
                        Log.w(TAG, "?? Cliente $entityId n�o encontrado localmente - tentando buscar do Firestore")
                        return tryFetchMissingCliente(entityId)
                    }
                    true
                }
                "mesa" -> {
                    val exists = appRepository.obterMesaPorId(entityId) != null
                    if (!exists) {
                        Log.w(TAG, "?? Mesa $entityId n�o encontrada localmente - tentando buscar do Firestore")
                        return tryFetchMissingMesa(entityId)
                    }
                    true
                }
                "contrato" -> {
                    val contrato = runCatching {
                        appRepository.buscarTodosContratos().first().find { it.id == entityId }
                    }.getOrNull()
                    if (contrato == null) {
                        Log.w(TAG, "?? Contrato $entityId n�o encontrado localmente - tentando buscar do Firestore")
                        return tryFetchMissingContrato(entityId)
                    }
                    true
                }
                else -> {
                    Log.w(TAG, "?? Tipo de entidade desconhecido: $entityType")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "? Erro ao validar FK para $entityType $entityId: ${e.message}", e)
            false
        }
    }
    
    private suspend fun tryFetchMissingCliente(clienteId: Long): Boolean {
        return try {
            val doc = getCollectionRef(COLLECTION_CLIENTES)
                .document(clienteId.toString())
                .get()
                .await()
            
            if (!doc.exists()) {
                Log.w(TAG, "?? Cliente $clienteId n�o existe no Firestore")
                return false
            }
            
            val clienteData = doc.data ?: return false
            val clienteJson = gson.toJson(clienteData)
            val cliente = gson.fromJson(clienteJson, Cliente::class.java)?.copy(id = clienteId)
                ?: return false
            
            appRepository.inserirCliente(cliente)
            Log.d(TAG, "? Cliente $clienteId buscado e inserido com sucesso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "? Falha ao buscar cliente $clienteId: ${e.message}", e)
            false
        }
    }
    
    private suspend fun tryFetchMissingMesa(mesaId: Long): Boolean {
        return try {
            val doc = getCollectionRef(COLLECTION_MESAS)
                .document(mesaId.toString())
                .get()
                .await()
            
            if (!doc.exists()) {
                Log.w(TAG, "?? Mesa $mesaId n�o existe no Firestore")
                return false
            }
            
            val mesaData = doc.data ?: return false
            val mesaJson = gson.toJson(mesaData)
            val mesa = gson.fromJson(mesaJson, Mesa::class.java)?.copy(id = mesaId)
                ?: return false
            
            // Validar que o cliente da mesa existe
            val clienteId = mesa.clienteId
            if (clienteId == null || clienteId <= 0L) {
                Log.w(TAG, "?? Mesa $mesaId tem clienteId inv�lido: $clienteId")
                return false
            }
            
            if (!ensureEntityExists("cliente", clienteId)) {
                Log.w(TAG, "?? Mesa $mesaId referencia cliente $clienteId que n�o existe")
                return false
            }
            
            appRepository.inserirMesa(mesa)
            Log.d(TAG, "? Mesa $mesaId buscada e inserida com sucesso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "? Falha ao buscar mesa $mesaId: ${e.message}", e)
            false
        }
    }
    
    private suspend fun tryFetchMissingContrato(contratoId: Long): Boolean {
        return try {
            val doc = getCollectionReference(firestore, COLLECTION_CONTRATOS)
                .document(contratoId.toString())
                .get()
                .await()
            
            if (!doc.exists()) {
                Log.w(TAG, "?? Contrato $contratoId n�o existe no Firestore")
                return false
            }
            
            val contratoData = doc.data ?: return false
            val contratoJson = gson.toJson(contratoData)
            val contrato = gson.fromJson(contratoJson, ContratoLocacao::class.java)?.copy(id = contratoId)
                ?: return false
            
            // Validar que o cliente do contrato existe
            if (!ensureEntityExists("cliente", contrato.clienteId)) {
                Log.w(TAG, "?? Contrato $contratoId referencia cliente ${contrato.clienteId} que n�o existe")
                return false
            }
            
            appRepository.inserirContrato(contrato)
            Log.d(TAG, "? Contrato $contratoId buscado e inserido com sucesso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "? Falha ao buscar contrato $contratoId: ${e.message}", e)
            false
        }
    }
    

    /**
     * ? NOVO (2025): Executa query Firestore com pagina��o autom�tica.
     * Processa documentos em lotes para evitar problemas de mem�ria e timeout.
     * 
     * @param query Query base do Firestore (pode ter filtros, ordena��o, etc)
     * @param batchSize Tamanho do lote (padr�o: 500, m�ximo recomendado pelo Firestore)
     * @param processor Fun��o para processar cada lote de documentos
     * @return Total de documentos processados
     * 
     * Segue melhores pr�ticas Android 2025 para pagina��o de queries grandes.
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
                // Construir query com pagina��o
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
                Log.d(TAG, "?? Processado lote: ${documents.size} documentos (total: $totalProcessed)")
                
                // Verificar se h� mais documentos
                hasMore = documents.size == batchSize
                lastDocument = documents.lastOrNull()
                
            } catch (e: Exception) {
                Log.e(TAG, "? Erro ao processar lote paginado: ${e.message}", e)
                hasMore = false
            }
        }
        
        Log.d(TAG, "? Pagina��o conclu�da: $totalProcessed documentos processados")
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
                Log.w(TAG, "?? Bootstrap de rotas: baixando todas as rotas temporariamente para popular acessos locais.")
                listOf(applyTimestampFilter(collectionRef, lastSyncTimestamp, timestampField))
            } else {
                Log.w(TAG, "?? Usu�rio sem rotas atribu�das - nenhuma query ser� executada para $routeField")
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
                Log.w(TAG, "?? Bootstrap de rotas: baixando todas as rotas temporariamente para popular acessos locais.")
                collectionRef.get().await().documents
            } else {
                Log.w(TAG, "?? Nenhuma rota atribu�da ao usu�rio - resultado vazio para $routeField")
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
     * ? NOVO (2025): Cria query incremental para sincroniza��o.
     * Retorna query que busca apenas documentos modificados desde a �ltima sincroniza��o.
     * 
     * @param collectionRef Refer�ncia da cole��o
     * @param entityType Tipo da entidade (para obter timestamp)
     * @param timestampField Nome do campo de timestamp no Firestore (padr�o: "lastModified")
     * @return Query incremental ou null se primeira sincroniza��o (retorna todos)
     * 
     * IMPORTANTE: Firestore requer �ndice composto para queries com whereGreaterThan + orderBy.
     * Certifique-se de criar o �ndice no Firestore Console se necess�rio.
     */
    private suspend fun createIncrementalQuery(
        collectionRef: CollectionReference,
        entityType: String,
        timestampField: String = "lastModified"
    ): Query {
        val lastSyncTimestamp = getLastSyncTimestamp(entityType)
        
        return if (lastSyncTimestamp > 0L) {
            // Sincroniza��o incremental: apenas documentos modificados desde a �ltima sync
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL para $entityType (desde ${Date(lastSyncTimestamp)})")
            collectionRef
                .whereGreaterThan(timestampField, Timestamp(Date(lastSyncTimestamp)))
                .orderBy(timestampField) // OBRIGAT�RIO: Firestore requer orderBy com whereGreaterThan
        } else {
            // Primeira sincroniza��o: buscar todos (mas ainda com orderBy para pagina��o)
            Log.d(TAG, "?? Primeira sincroniza��o COMPLETA para $entityType")
            collectionRef.orderBy(timestampField)
        }
    }
    
    // ==================== SINCRONIZA��O PULL (SERVIDOR ? LOCAL) ====================
    
    /**
     * Sincroniza dados do servidor para o local (Pull).
     * Offline-first: Funciona apenas quando online.
     */
    suspend fun syncPull(
        progressTracker: ProgressTracker? = null,
        timestampOverride: Long? = null // ? NOVO: timestamp capturado antes do push (propagado para todas as entidades)
    ): Result<Unit> {
        Log.d(TAG, "?? syncPull() CHAMADO - IN�CIO")
        return try {
            Log.d(TAG, "?? ========== INICIANDO SINCRONIZA��O PULL ==========")
            Log.d(TAG, "?? Verificando conectividade...")
            
            resetRouteFilters()
            val accessibleRoutes = getAccessibleRouteIdsInternal()
            if (userSessionManager.isAdmin()) {
                Log.d(TAG, "?? Usu�rio ADMIN - sincronizando todas as rotas dispon�veis.")
            } else if (accessibleRoutes.isEmpty()) {
                Log.w(TAG, "?? Usu�rio sem rotas atribu�das - nenhum dado espec�fico de rota ser� sincronizado.")
            } else {
                Log.d(TAG, "?? Rotas permitidas para este usu�rio: ${accessibleRoutes.joinToString()}")
            }
            
            val isConnected = networkUtils.isConnected()
            Log.d(TAG, "?? NetworkUtils.isConnected() = $isConnected")
            
            // Tentar mesmo se NetworkUtils reportar offline (pode ser falso negativo)
            // O Firestore vai falhar se realmente estiver offline
            if (!isConnected) {
                Log.w(TAG, "?? NetworkUtils reporta offline, mas tentando mesmo assim...")
                Log.w(TAG, "?? Firestore vai falhar se realmente estiver offline")
            } else {
                Log.d(TAG, "? Dispositivo online confirmado")
            }
            
            Log.d(TAG, "? Prosseguindo com sincroniza��o PULL")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Log.d(TAG, "?? Conectando ao Firestore...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // ? CORRIGIDO: Pull por dom�nio em sequ�ncia respeitando depend�ncias
            // ORDEM CR�TICA: Rotas primeiro (clientes dependem de rotas)
            
            pullRotas(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando rotas...")
            
            pullClientes(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Clientes: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Clientes falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando clientes...")
            
            pullMesas(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando mesas...")
            
            pullColaboradores(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Colaboradores: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Colaboradores falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando colaboradores...")
            
            pullCiclos(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Ciclos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Ciclos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando ciclos...")
            
            pullAcertos(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Acertos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Acertos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando acertos...")
            
            pullDespesas(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Despesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Despesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando despesas...")
            
            pullContratos(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Contratos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Contratos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando contratos...")
            
            // Pull de entidades faltantes (prioridade ALTA)
            pullCategoriasDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Categorias Despesa: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Categorias Despesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando categorias de despesa...")
            
            pullTiposDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Tipos Despesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Tipos Despesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando tipos de despesa...")
            
            pullMetas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Metas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Metas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando metas...")
            
            pullMetaColaborador().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Meta Colaborador: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Meta Colaborador falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando metas por colaborador...")
            
            pullEquipments().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Equipments: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Equipments falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando equipamentos...")
            
            pullColaboradorRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Colaborador Rotas: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Colaborador Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando colaborador rotas...")
            
            pullAditivoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Aditivo Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Aditivo Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando aditivos de mesa...")
            
            pullContratoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Contrato Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Contrato Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando contratos de mesa...")
            
            pullAssinaturasRepresentanteLegal().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Assinaturas Representante Legal: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Assinaturas Representante Legal falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando assinaturas do representante legal...")
            
            pullLogsAuditoria().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Logs Auditoria: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Logs Auditoria falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando logs de auditoria...")
            
            // ? NOVO: Pull de entidades faltantes (AGENTE PARALELO)
            pullPanoEstoque().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull PanoEstoque: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull PanoEstoque falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando panos em estoque...")
            
            pullMesaVendida().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull MesaVendida: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull MesaVendida falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando mesas vendidas...")
            
            pullStockItem().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull StockItem: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull StockItem falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando itens de estoque...")
            
            pullMesaReformada(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull MesaReformada: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull MesaReformada falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando mesas reformadas...")
            
            pullPanoMesa(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull PanoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull PanoMesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando panos de mesa...")
            
            pullHistoricoManutencaoMesa(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull HistoricoManutencaoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull HistoricoManutencaoMesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando hist�rico de manuten��o das mesas...")
            
            pullHistoricoManutencaoVeiculo(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull HistoricoManutencaoVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull HistoricoManutencaoVeiculo falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando hist�rico de manuten��o de ve�culos...")
            
            pullHistoricoCombustivelVeiculo(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull HistoricoCombustivelVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull HistoricoCombustivelVeiculo falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando hist�rico de combust�vel dos ve�culos...")
            
            pullVeiculos(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Pull Veiculos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Pull Veiculos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando ve�culos...")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                failedOperations = failedCount
            )
            
            Log.d(TAG, "? ========== SINCRONIZA��O PULL CONCLU�DA ==========")
            Log.d(TAG, "?? Total sincronizado: $totalSyncCount itens")
            Log.d(TAG, "? Total de falhas: $failedCount dom�nios")
            Log.d(TAG, "? Timestamp: ${System.currentTimeMillis()}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincroniza��o Pull: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    // ==================== SINCRONIZA��O PUSH (LOCAL ? SERVIDOR) ====================
    
    /**
     * Sincroniza dados do local para o servidor (Push).
     * Offline-first: Enfileira opera��es quando offline.
     */
    suspend fun syncPush(progressTracker: ProgressTracker? = null): Result<Unit> {
        Log.d(TAG, "?? ========== INICIANDO SINCRONIZA��O PUSH ==========")
        Log.d(TAG, "   ? Timestamp: ${System.currentTimeMillis()} (${java.util.Date()})")
        Log.d(TAG, "   ?? Stack trace: ${Thread.currentThread().stackTrace.take(5).joinToString("\n") { it.toString() }}")
        return try {
            if (!networkUtils.isConnected()) {
                Log.w(TAG, "?? Sincroniza��o Push cancelada: dispositivo offline")
                return Result.failure(Exception("Dispositivo offline"))
            }

            Log.d(TAG, "? Dispositivo online - prosseguindo com sincroniza��o")

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Log.d(TAG, "?? ========== PROCESSANDO FILA DE SINCRONIZA��O ==========")
            // ? CORRE��O: Verificar quantas opera��es existem na fila antes de processar
            val pendingCountBefore = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
            val failedCountBefore = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
            Log.d(TAG, "?? Estado da fila ANTES do processamento:")
            Log.d(TAG, "   - Opera��es pendentes: $pendingCountBefore")
            Log.d(TAG, "   - Opera��es falhadas: $failedCountBefore")
            
            if (pendingCountBefore > 0) {
                Log.d(TAG, "   ?? H� $pendingCountBefore opera��o(�es) pendente(s) na fila!")
            } else {
                Log.d(TAG, "   ?? Nenhuma opera��o pendente na fila")
            }
            
            val queueProcessResult = processSyncQueue()
            if (queueProcessResult.isFailure) {
                Log.e(TAG, "? Falha ao processar fila de sincroniza��o: ${queueProcessResult.exceptionOrNull()?.message}")
                // N�o retornamos falha aqui, tentamos o push direto mesmo assim
            } else {
                val pendingCountAfter = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
                val failedCountAfter = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
                Log.d(TAG, "? Fila de sincroniza��o processada com sucesso.")
                Log.d(TAG, "?? Estado da fila DEPOIS do processamento: pendentes=$pendingCountAfter, falhadas=$failedCountAfter")
            }

            Log.d(TAG, "Iniciando push de dados locais para o Firestore...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // Push por dom�nio em sequ�ncia
            pushClientes().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Clientes: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Clientes falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando clientes...")
            
            pushRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando rotas...")
            
            pushMesas().fold(
                onSuccess = { count: Int -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando mesas...")
            
            pushColaboradores().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Colaboradores: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Colaboradores falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando colaboradores...")
            
            pushCiclos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Ciclos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Ciclos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando ciclos...")
            
            pushAcertos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Acertos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Acertos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando acertos...")
            
            pushDespesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Despesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Despesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando despesas...")
            
            pushContratos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Contratos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Contratos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando contratos...")
            
            // Push de entidades faltantes (prioridade ALTA)
            pushCategoriasDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Categorias Despesa: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Categorias Despesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando categorias de despesa...")
            
            pushTiposDespesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Tipos Despesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Tipos Despesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando tipos de despesa...")
            
            pushMetas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Metas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Metas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando metas...")
            
            pushColaboradorRotas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Colaborador Rotas: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Colaborador Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando colaborador rotas...")
            
            pushAditivoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Aditivo Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Aditivo Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando aditivos de mesa...")
            
            pushContratoMesas().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Contrato Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Contrato Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando contratos de mesa...")
            
            pushAssinaturasRepresentanteLegal().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Assinaturas Representante Legal: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Assinaturas Representante Legal falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando assinaturas do representante legal...")
            
            pushLogsAuditoria().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Logs Auditoria: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Logs Auditoria falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando logs de auditoria...")
            
            // ? NOVO: Push de entidades faltantes (AGENTE PARALELO)
            pushPanoEstoque().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push PanoEstoque: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push PanoEstoque falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando panos em estoque...")
            
            pushMesaVendida().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push MesaVendida: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push MesaVendida falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando mesas vendidas...")
            
            pushStockItem().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push StockItem: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push StockItem falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando itens de estoque...")
            
            pushMesaReformada().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push MesaReformada: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push MesaReformada falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando mesas reformadas...")
            
            pushPanoMesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push PanoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push PanoMesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando panos de mesa...")
            
            pushHistoricoManutencaoMesa().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push HistoricoManutencaoMesa: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push HistoricoManutencaoMesa falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando hist�rico de manuten��o das mesas...")
            
            pushHistoricoManutencaoVeiculo().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push HistoricoManutencaoVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push HistoricoManutencaoVeiculo falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando hist�rico de manuten��o de ve�culos...")
            
            pushHistoricoCombustivelVeiculo().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push HistoricoCombustivelVeiculo: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push HistoricoCombustivelVeiculo falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando hist�rico de combust�vel dos ve�culos...")
            
            pushVeiculos().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Veiculos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Veiculos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando ve�culos...")
            
            pushMetaColaborador().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Meta Colaborador: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Meta Colaborador falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando metas por colaborador...")
            
            pushEquipments().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Log.d(TAG, "? Push Equipments: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Log.e(TAG, "? Push Equipments falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando equipamentos...")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                pendingOperations = appRepository.contarOperacoesSyncPendentes(),
                failedOperations = appRepository.contarOperacoesSyncFalhadas()
            )
            
            Log.d(TAG, "? ========== SINCRONIZA��O PUSH CONCLU�DA ==========")
            Log.d(TAG, "?? Total enviado: $totalSyncCount itens")
            Log.d(TAG, "? Total de falhas: $failedCount dom�nios")
            Log.d(TAG, "? Timestamp: ${System.currentTimeMillis()}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincroniza��o Push: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    // ==================== SINCRONIZA��O BIDIRECIONAL ====================
    
    /**
     * Verifica se h� dados na nuvem quando o banco local est� vazio.
     * Retorna true se encontrar pelo menos uma rota no Firestore.
     */
    suspend fun hasDataInCloud(): Boolean {
        return try {
            if (!networkUtils.isConnected()) {
                Log.d(TAG, "?? Verificando dados na nuvem: dispositivo offline")
                return false
            }
            
            Log.d(TAG, "?? Verificando se h� dados na nuvem...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ROTAS)
            val snapshot = collectionRef.limit(1).get().await()
            val hasData = !snapshot.isEmpty
            Log.d(TAG, "?? Dados na nuvem encontrados: $hasData")
            hasData
        } catch (e: FirebaseFirestoreException) {
            // ? CORRE��O: Se for PERMISSION_DENIED e usu�rio est� logado localmente,
            // assumir que h� dados na nuvem (permitir tentar sincronizar)
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                val userId = userSessionManager.getCurrentUserId()
                if (userId != 0L) {
                    Log.w(TAG, "?? PERMISSION_DENIED ao verificar nuvem, mas usu�rio est� logado localmente (ID: $userId)")
                    Log.w(TAG, "?? Assumindo que h� dados na nuvem para permitir sincroniza��o")
                    return true // Assumir que h� dados para permitir tentar sincronizar
                }
            }
            Log.e(TAG, "? Erro ao verificar dados na nuvem: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "? Erro ao verificar dados na nuvem: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sincroniza��o completa bidirecional (Push + Pull).
     * Offline-first: Push primeiro para preservar dados locais, depois Pull para atualizar.
     * 
     * ? CORRIGIDO: Ordem invertida para evitar perda de dados locais.
     * - PUSH primeiro: Envia dados locais para a nuvem (preserva dados novos)
     * - PULL depois: Baixa atualiza��es da nuvem (n�o sobrescreve se local for mais recente)
     */
    suspend fun syncBidirectional(onProgress: ((SyncProgress) -> Unit)? = null): Result<Unit> {
        Log.d(TAG, "?? syncBidirectional() CHAMADO - IN�CIO")
        return try {
            Log.d(TAG, "?? ========== INICIANDO SINCRONIZA��O BIDIRECIONAL ==========")
            Log.d(TAG, "Iniciando sincroniza��o bidirecional...")
            
            // ? CORRE��O CR�TICA: Capturar timestamp ANTES de fazer PUSH
            // Isso garante que pr�xima sync incremental n�o perca dados que foram enviados agora
            val timestampBeforePush = System.currentTimeMillis()
            Log.d(TAG, "   ? Timestamp capturado ANTES do push: $timestampBeforePush (${Date(timestampBeforePush)})")
            
            val progressTracker = onProgress?.let { ProgressTracker(TOTAL_SYNC_OPERATIONS, it).apply { start() } }
            
            // ? CORRIGIDO: 1. PUSH primeiro (enviar dados locais para preservar)
            // Isso garante que dados novos locais sejam enviados antes de baixar da nuvem
            Log.d(TAG, "?? Passo 1: Executando PUSH (enviar dados locais para nuvem)...")
            val pushResult = syncPush(progressTracker)
            if (pushResult.isFailure) {
                Log.w(TAG, "?? Push falhou: ${pushResult.exceptionOrNull()?.message}")
                Log.w(TAG, "?? Continuando com Pull mesmo assim...")
            } else {
                Log.d(TAG, "? Push conclu�do com sucesso - dados locais preservados na nuvem")
            }
            
            // ? CORRE��O: Aguardar pequeno delay para garantir que Firestore processou
            // Isso evita race condition onde PULL ocorre antes que Firestore salve dados do PUSH
            Log.d(TAG, "   ?? Aguardando 500ms para propaga��o do Firestore...")
            kotlinx.coroutines.delay(500) // Meio segundo para propaga��o
            
            // ? CORRIGIDO: 2. PULL depois (atualizar dados locais da nuvem)
            // O pull n�o sobrescreve dados locais mais recentes (verifica��o de timestamp)
            Log.d(TAG, "?? Passo 2: Executando PULL (importar atualiza��es da nuvem)...")
            Log.d(TAG, "   ?? Usando timestamp capturado ANTES do push: $timestampBeforePush")
            val pullResult = syncPull(progressTracker, timestampBeforePush)
            if (pullResult.isFailure) {
                Log.w(TAG, "?? Pull falhou: ${pullResult.exceptionOrNull()?.message}")
                Log.w(TAG, "?? Mas Push pode ter sido bem-sucedido")
            } else {
                Log.d(TAG, "? Pull conclu�do com sucesso - dados locais atualizados")
            }
            
            if (pullResult.isSuccess && pushResult.isSuccess) {
                Log.d(TAG, "? ========== SINCRONIZA��O BIDIRECIONAL CONCLU�DA COM SUCESSO ==========")
                // ? N�O salvar global metadata aqui - j� foi salvo por entidade com timestampOverride
                progressTracker?.complete()
                Result.success(Unit)
            } else {
                val errorMsg = "Sincroniza��o parcial: Push=${pushResult.isSuccess}, Pull=${pullResult.isSuccess}"
                Log.w(TAG, "?? $errorMsg")
                progressTracker?.completeWithMessage("Sincroniza��o parcial conclu�da")
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "? Erro na sincroniza��o bidirecional: ${e.message}", e)
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
            onProgress?.invoke(SyncProgress(100, "Sincroniza��o falhou"))
            Result.failure(e)
        }
    }
    
    // ==================== FILA DE SINCRONIZA��O ====================
    
    /**
     * Adiciona opera��o � fila de sincroniza��o.
     * Opera��es s�o processadas quando dispositivo estiver online.
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
            Log.d(TAG, "?? Opera��o enfileirada: ${operation.type} - entidade=${operation.entityType}, id=${operation.entityId}")
        } catch (e: Exception) {
            Log.e(TAG, "? Erro ao enfileirar opera��o ${operation.entityId}: ${e.message}", e)
        }
    }
    
    /**
     * Processa fila de sincroniza��o pendente.
     */
    suspend fun processSyncQueue(): Result<Unit> {
        return try {
            Log.d(TAG, "?? processSyncQueue() INICIADO")
            if (!networkUtils.isConnected()) {
                Log.w(TAG, "?? Fila de sincroniza��o n�o processada: dispositivo offline")
                return Result.failure(Exception("Dispositivo offline - fila pendente"))
            }
            
            // ? CORRE��O: Verificar quantas opera��es existem antes de processar
            val totalPendingBefore = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
            Log.d(TAG, "?? Total de opera��es pendentes na fila: $totalPendingBefore")
            
            if (totalPendingBefore == 0) {
                Log.d(TAG, "?? Nenhuma opera��o pendente na fila - encerrando processamento")
                return Result.success(Unit)
            }
            
            // ? CORRE��O: Processar em loop at� esvaziar a fila
            var totalSuccessCount = 0
            var totalFailureCount = 0
            var batchNumber = 0
            
            while (true) {
                // ? CORRE��O: Log antes de buscar opera��es
                Log.d(TAG, "   ?? Buscando opera��es pendentes (limite: $QUEUE_BATCH_SIZE)...")
                val operations = appRepository.obterOperacoesSyncPendentesLimitadas(QUEUE_BATCH_SIZE)
                Log.d(TAG, "   ?? Opera��es encontradas: ${operations.size}")
                
                if (operations.isEmpty()) {
                    if (batchNumber == 0) {
                        Log.d(TAG, "?? Nenhuma opera��o pendente na fila")
                    } else {
                        Log.d(TAG, "? Fila completamente processada ap�s $batchNumber lote(s)")
                    }
                    break
                }
                
                batchNumber++
                Log.d(TAG, "?? Processando lote $batchNumber: ${operations.size} opera��es pendentes")
                // ? CORRE��O: Log detalhado de cada opera��o encontrada
                operations.forEachIndexed { index, op ->
                    Log.d(TAG, "   ?? Opera��o ${index + 1}: tipo=${op.operationType}, entidade=${op.entityType}, id=${op.entityId}, status=${op.status}")
                }
                var batchSuccessCount = 0
                var batchFailureCount = 0
                
                operations.forEach { entity ->
                    val processingEntity = entity.copy(status = SyncOperationStatus.PROCESSING.name)
                    appRepository.atualizarOperacaoSync(processingEntity)
                    
                    try {
                        Log.d(TAG, "?? Processando opera��o: tipo=${entity.operationType}, entidade=${entity.entityType}, id=${entity.entityId}")
                        Log.d(TAG, "   ?? Operation ID: ${entity.id}, Status: ${entity.status}, Retry: ${entity.retryCount}")
                        processSingleSyncOperation(processingEntity)
                        appRepository.deletarOperacaoSync(processingEntity)
                        batchSuccessCount++
                        totalSuccessCount++
                        Log.d(TAG, "? Opera��o processada com sucesso: tipo=${entity.operationType}, entidade=${entity.entityType}, id=${entity.entityId}")
                    } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                        batchFailureCount++
                        totalFailureCount++
                        val newRetryCount = processingEntity.retryCount + 1
                        val newStatus = if (newRetryCount >= processingEntity.maxRetries) {
                            SyncOperationStatus.FAILED.name
                        } else {
                            SyncOperationStatus.PENDING.name
                        }
                        Log.e(TAG, "? Erro do Firestore ao processar opera��o ${processingEntity.id} (tentativa $newRetryCount/${processingEntity.maxRetries}): ${e.code} - ${e.message}", e)
                        Log.e(TAG, "   Tipo: ${processingEntity.operationType}, Entidade: ${processingEntity.entityType}, ID: ${processingEntity.entityId}")
                        if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Log.e(TAG, "   ?? PERMISSION_DENIED: Verifique as regras do Firestore para ${processingEntity.operationType} em ${processingEntity.entityType}")
                        }
                        appRepository.atualizarOperacaoSync(
                            processingEntity.copy(
                                status = newStatus,
                                retryCount = newRetryCount,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } catch (e: Exception) {
                        batchFailureCount++
                        totalFailureCount++
                        val newRetryCount = processingEntity.retryCount + 1
                        val newStatus = if (newRetryCount >= processingEntity.maxRetries) {
                            SyncOperationStatus.FAILED.name
                        } else {
                            SyncOperationStatus.PENDING.name
                        }
                        Log.e(TAG, "? Erro ao processar opera��o ${processingEntity.id} (tentativa $newRetryCount/${processingEntity.maxRetries}): ${e.message}", e)
                        Log.e(TAG, "   Tipo: ${processingEntity.operationType}, Entidade: ${processingEntity.entityType}, ID: ${processingEntity.entityId}")
                        Log.e(TAG, "   ?? Stack trace: ${e.stackTraceToString()}")
                        appRepository.atualizarOperacaoSync(
                            processingEntity.copy(
                                status = newStatus,
                                retryCount = newRetryCount,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                
                Log.d(TAG, "?? Lote $batchNumber processado: sucesso=$batchSuccessCount, falhas=$batchFailureCount")
            }
            
            val pendingCount = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
            val failedCount = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
            _syncStatus.value = _syncStatus.value.copy(
                pendingOperations = pendingCount,
                failedOperations = failedCount
            )
            
            Log.d(TAG, "?? Fila completamente processada: total sucesso=$totalSuccessCount, total falhas=$totalFailureCount, pendentes=$pendingCount, falhadas=$failedCount")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro ao processar fila de sincroniza��o: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun processSingleSyncOperation(operation: SyncOperationEntity) {
        val operationType = runCatching { SyncOperationType.valueOf(operation.operationType) }.getOrElse {
            throw IllegalArgumentException("Tipo de opera��o inv�lido: ${operation.operationType}")
        }
        val collectionRef = resolveCollectionReference(operation.entityType)
        val documentId = operation.entityId.ifBlank {
            throw IllegalArgumentException("entityId vazio para opera��o ${operation.id}")
        }
        
        // ? CORRE��O: Logs detalhados para debug
        Log.d(TAG, "?? Processando opera��o �nica:")
        Log.d(TAG, "   Tipo: ${operation.operationType}")
        Log.d(TAG, "   Entidade: ${operation.entityType}")
        Log.d(TAG, "   Document ID: $documentId")
        Log.d(TAG, "   Collection Path: ${collectionRef.path}")
        
        when (operationType) {
            SyncOperationType.CREATE, SyncOperationType.UPDATE -> {
                val rawMap: Map<String, Any?> = gson.fromJson(operation.entityData, mapType)
                val mutableData = rawMap.toMutableMap()
                mutableData["lastModified"] = FieldValue.serverTimestamp()
                mutableData["syncTimestamp"] = FieldValue.serverTimestamp()
                Log.d(TAG, "?? Executando ${operation.operationType} no documento $documentId")
                collectionRef.document(documentId).set(mutableData).await()
                Log.d(TAG, "? ${operation.operationType} executado com sucesso no documento $documentId")
            }
            SyncOperationType.DELETE -> {
                Log.d(TAG, "??? ========== INICIANDO DELETE ==========")
                Log.d(TAG, "   Tipo de entidade: ${operation.entityType}")
                Log.d(TAG, "   Document ID: $documentId")
                Log.d(TAG, "   Collection Path: ${collectionRef.path}")
                Log.d(TAG, "   Collection Name: ${collectionRef.id}")
                Log.d(TAG, "   Document Path completo: ${collectionRef.path}/$documentId")
                
                val documentRef = collectionRef.document(documentId)
                
                // ? CORRE��O: Verificar se o documento existe antes de deletar
                try {
                    Log.d(TAG, "   ?? Verificando exist�ncia do documento...")
                    val snapshot = documentRef.get().await()
                    if (snapshot.exists()) {
                        Log.d(TAG, "   ?? Documento encontrado no Firestore!")
                        Log.d(TAG, "   ?? Dados do documento: ${snapshot.data?.keys?.joinToString()}")
                        Log.d(TAG, "   ??? Executando DELETE...")
                        documentRef.delete().await()
                        Log.d(TAG, "? DELETE executado com sucesso no documento $documentId")
                        Log.d(TAG, "   ? Verificando se foi deletado...")
                        val verifySnapshot = documentRef.get().await()
                        if (!verifySnapshot.exists()) {
                            Log.d(TAG, "   ? Confirmado: Documento foi deletado com sucesso")
                        } else {
                            Log.w(TAG, "   ?? ATEN��O: Documento ainda existe ap�s DELETE!")
                        }
                    } else {
                        Log.w(TAG, "?? Documento $documentId n�o existe no Firestore")
                        Log.w(TAG, "   Caminho verificado: ${collectionRef.path}/$documentId")
                        Log.w(TAG, "   Poss�veis causas: j� foi deletado, nunca existiu, ou caminho incorreto")
                        // N�o lan�ar exce��o - considerar sucesso se j� n�o existe
                    }
                } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                    Log.e(TAG, "? Erro do Firestore ao deletar documento $documentId:", e)
                    Log.e(TAG, "   C�digo: ${e.code}")
                    Log.e(TAG, "   Mensagem: ${e.message}")
                    Log.e(TAG, "   Caminho: ${collectionRef.path}/$documentId")
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "? Erro geral ao deletar documento $documentId: ${e.message}", e)
                    Log.e(TAG, "   Tipo: ${e.javaClass.simpleName}")
                    Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                    throw e
                }
                Log.d(TAG, "??? ========== DELETE FINALIZADO ==========")
            }
        }
    }
    
    private fun resolveCollectionReference(entityType: String): CollectionReference {
        val normalized = entityType.lowercase(Locale.getDefault())
        val collectionName = when (normalized) {
            "cliente" -> COLLECTION_CLIENTES
            "acerto" -> COLLECTION_ACERTOS
            "despesa" -> COLLECTION_DESPESAS  // ? CORRE��O: Mapear "despesa" (singular) para "despesas" (plural)
            "mesa" -> COLLECTION_MESAS
            "rota" -> COLLECTION_ROTAS
            "colaborador" -> COLLECTION_COLABORADORES
            "mesareformada" -> COLLECTION_MESAS_REFORMADAS
            "mesavendida" -> COLLECTION_MESAS_VENDIDAS
            "equipment" -> COLLECTION_EQUIPMENTS
            "ciclo" -> COLLECTION_CICLOS
            "meta" -> COLLECTION_METAS
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
            else -> {
                Log.w(TAG, "?? Tipo de entidade desconhecido: $entityType (normalizado: $normalized), usando como nome de cole��o")
                normalized
            }
        }
        Log.d(TAG, "?? Mapeamento de entidade: '$entityType' -> cole��o '$collectionName'")
        return getCollectionReference(firestore, collectionName)
    }
    
    // ==================== PULL HANDLERS (SERVIDOR ? LOCAL) ====================
    
    /**
     * Pull Clientes: Sincroniza clientes do Firestore para o Room.
     * 
     * ESTRAT�GIA SEGURA:
     * 1. Tenta sincroniza��o incremental se houver metadata (timestamp > 0)
     * 2. Se incremental falhar de qualquer forma, usa m�todo completo (que sempre funciona)
     * 3. Sempre salva metadata ap�s sincroniza��o bem-sucedida
     * 4. Garante que o m�todo completo nunca seja quebrado
     */
    private suspend fun pullClientes(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CLIENTES
        
        return try {
            Log.d(TAG, "?? Iniciando pull de clientes...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_CLIENTES)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullClientesIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosClientes().first().size }
                        .getOrDefault(0)
                    if (syncedCount > 0 || localCount > 0) {
                        // Incremental adicionou registros ou j� existe base local
                        return incrementalResult
                    }
                    Log.w(TAG, "?? Incremental retornou 0 clientes e base local est� vazia - executando pull COMPLETO como fallback")
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullClientesComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de clientes.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * 
     * Este m�todo � seguro: se qualquer coisa falhar, retorna null e o m�todo completo � usado.
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
                Log.w(TAG, "?? Erro ao executar query incremental de clientes: ${e.message}")
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
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL (clientes) com filtro de rota: $totalDocuments documentos")
            
            val cacheStartTime = System.currentTimeMillis()
            val todosClientes = appRepository.obterTodosClientes().first()
            val clientesCache = todosClientes.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de clientes carregado: ${clientesCache.size} em ${System.currentTimeMillis() - cacheStartTime}ms")
            
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
                    Log.d(TAG, "   ?? Progresso: $processedCount/$totalDocuments documentos processados (${elapsed}ms)")
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Clientes (INCREMENTAL) conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, docs=$totalDocuments")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de clientes.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullClientesComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "?? Total de clientes no Firestore (ap�s filtro de rota): ${documents.size}")
            
            // ? OTIMIZADO: Carregar todos os clientes uma vez e criar cache em mem�ria
            val cacheStartTime = System.currentTimeMillis()
            val todosClientes = appRepository.obterTodosClientes().first()
            val clientesCache = todosClientes.associateBy { it.id }
            val cacheDuration = System.currentTimeMillis() - cacheStartTime
            Log.d(TAG, "   ?? Cache de clientes carregado: ${clientesCache.size} clientes (${cacheDuration}ms)")
            
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
                    Log.d(TAG, "   ?? Progresso: $processedCount/${documents.size} documentos processados (${elapsed}ms)")
                }
            }
            val processDuration = System.currentTimeMillis() - processStartTime
            Log.d(TAG, "   ?? Processamento de documentos: ${processDuration}ms")
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Clientes (COMPLETO) conclu�do: $syncCount sincronizados, $skippedCount pulados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Processa um documento de cliente do Firestore.
     * L�gica centralizada e reutiliz�vel para ambos os m�todos (incremental e completo).
     */
    private sealed class ProcessResult {
        object Synced : ProcessResult()
        object Skipped : ProcessResult()
        object Error : ProcessResult()
    }
    
    /**
     * ? OTIMIZADO: Processa documento de cliente usando cache em mem�ria.
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
            
            // Validar dados obrigat�rios
            if (clienteFirestore.nome.isBlank() || clienteFirestore.rotaId == 0L) {
                return ProcessResult.Skipped
            }
            
            // ? OTIMIZADO: Usar cache em mem�ria em vez de consulta ao banco
            val clienteLocal = clientesCache[clienteId]
                    val serverTimestamp = dataUltimaAtualizacao.time
                    val localTimestamp = clienteLocal?.dataUltimaAtualizacao?.time ?: 0L
                    
                    when {
                    clienteLocal == null -> {
                        // Inserir novo cliente (Insert)
                        appRepository.inserirCliente(clienteFirestore)
                        ProcessResult.Synced
                    }
                    serverTimestamp > localTimestamp -> {
                        // ? ESTRAT�GIA SEGURA (2025): Tentar UPDATE primeiro
                        val updateCount = appRepository.atualizarCliente(clienteFirestore)
                        if (updateCount == 0) {
                            // Se update n�o encontrou (raro, pois local != null), inserir
                             appRepository.inserirCliente(clienteFirestore)
                        }
                        ProcessResult.Synced
                    }
                    else -> {
                        ProcessResult.Skipped
                    }
                    }
                } catch (e: Exception) {
            Log.e(TAG, "? Erro ao processar cliente ${doc.id}: ${e.message}", e)
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
     * ? NOVO: Converte timestamp do Firestore para LocalDateTime
     * Necess�rio para campos dataHora da entidade Despesa
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
            Log.d(TAG, "?? Iniciando pull de rotas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ROTAS)

            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L

            Log.d(TAG, "?? Rotas: lastSyncTimestamp=$lastSyncTimestamp, canUseIncremental=$canUseIncremental, allowRouteBootstrap=$allowRouteBootstrap")

            var incrementalExecutado = false
            if (canUseIncremental) {
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL de rotas (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullRotasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                incrementalExecutado = incrementalResult != null
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasRotas().first().size }.getOrDefault(0)
                    Log.d(TAG, "?? Rotas incremental: syncedCount=$syncedCount, localCount=$localCount")
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Log.w(TAG, "?? Rotas: incremental trouxe $syncedCount registros com base local $localCount - executando pull completo")
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de rotas falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de rotas - usando m�todo COMPLETO")
            }

            return pullRotasComplete(collectionRef, entityType, startTime, incrementalExecutado, timestampOverride)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de rotas: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de rotas - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                Log.w(TAG, "?? Nenhuma rota encontrada no Firestore")
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull de rotas conclu�do (fallback incremental=$incrementalFallback): sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de rotas: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao criar query incremental para rotas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "?? Rotas - incremental retornou ${documents.size} documentos")
            
            if (documents.isEmpty()) {
                Log.d(TAG, "? Nenhuma rota nova/alterada desde a �ltima sincroniza��o")
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de rotas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de rotas: ${e.message}", e)
            null // For�ar fallback para m�todo completo
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
                    
            // ? CORRE��O: Durante bootstrap, permitir todas as rotas temporariamente
            if (roomId != null && !allowRouteBootstrap && !shouldSyncRouteData(roomId, allowUnknown = false)) {
                Log.d(TAG, "?? Rota ignorada por falta de acesso: ID=$roomId")
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
                Log.w(TAG, "?? Rota ${doc.id} sem roomId v�lido - criando registro local com ID autogerado")
                        val rotaNova = Rota(
                            nome = nome,
                            descricao = rotaData["descricao"] as? String ?: "",
                            colaboradorResponsavel = rotaData["colaboradorResponsavel"] as? String
                                ?: rotaData["colaborador_responsavel"] as? String ?: "N�o definido",
                            cidades = rotaData["cidades"] as? String ?: "N�o definido",
                            ativa = rotaData["ativa"] as? Boolean ?: true,
                            cor = rotaData["cor"] as? String ?: "#6200EA",
                            dataCriacao = dataCriacaoLong,
                            dataAtualizacao = dataAtualizacaoLong
                        )
                        val insertedId = appRepository.inserirRota(rotaNova)
                rotasCache[insertedId] = rotaNova.copy(id = insertedId)
                Log.d(TAG, "? Rota criada sem roomId: ${rotaNova.nome} (ID Room: $insertedId)")
                ProcessResult.Synced
            } else {
                    val rotaFirestore = Rota(
                        id = roomId,
                        nome = nome,
                        descricao = rotaData["descricao"] as? String ?: "",
                        colaboradorResponsavel = rotaData["colaboradorResponsavel"] as? String
                            ?: rotaData["colaborador_responsavel"] as? String ?: "N�o definido",
                        cidades = rotaData["cidades"] as? String ?: "N�o definido",
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
                    Log.d(TAG, "?? Rota sincronizada: ${rotaFirestore.nome} (ID=$roomId)")
                    ProcessResult.Synced
                } else {
                    Log.d(TAG, "?? Rota mantida localmente (mais recente): ${rotaFirestore.nome} (ID=$roomId)")
                    ProcessResult.Skipped
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "? Erro ao processar rota ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Mesas: Sincroniza mesas do Firestore para o Room
     * ? NOVO (2025): Implementa sincroniza��o incremental seguindo padr�o de Clientes
     */
    private suspend fun pullMesas(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS
        
        return try {
            Log.d(TAG, "Iniciando pull de mesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? [DIAGNOSTICO] Iniciando Incremental Mesas")
                Log.d(TAG, "?? [DIAGNOSTICO] lastSyncTimestamp (Long): $lastSyncTimestamp")
                Log.d(TAG, "?? [DIAGNOSTICO] lastSyncTimestamp (Date): ${Date(lastSyncTimestamp)}")
                Log.d(TAG, "?? [DIAGNOSTICO] timestampOverride: $timestampOverride")
                Log.d(TAG, "?? [DIAGNOSTICO] CurrentTime: ${System.currentTimeMillis()}")
                
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullMesasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesas().first().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� mesas locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 mesas mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullMesasComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullMesasComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de mesas.
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
            // ? CORRE��O CR�TICA: Estrat�gia h�brida para garantir que mesas n�o desapare�am
            // 1. Tentar buscar apenas mesas modificadas recentemente (otimiza��o)
            // 2. Se retornar 0 mas houver mesas locais, buscar TODAS para garantir sincroniza��o completa
            
            // ? CORRE��O: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todasMesas = appRepository.obterTodasMesas().first()
            val mesasCache = todasMesas.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de mesas carregado: ${mesasCache.size} mesas locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            Log.d(TAG, "?? [DIAGNOSTICO] Executando Query: collectionRef.whereGreaterThan('lastModified', ${Date(lastSyncTimestamp)})")
            val incrementalMesas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todas as mesas: ${e.message}")
                emptyList()
            }
            
            Log.d(TAG, "?? [DIAGNOSTICO] Query retornou ${incrementalMesas.size} documentos")
            incrementalMesas.forEach { doc ->
                val lm = doc.getTimestamp("lastModified")?.toDate()
                Log.d(TAG, "?? [DIAGNOSTICO] Doc encontrado: ${doc.id}, lastModified: $lm (${lm?.time})")
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� mesas locais, buscar TODAS
            val allMesas = if (incrementalMesas.isEmpty() && mesasCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 mesas mas h� ${mesasCache.size} locais - buscando TODAS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todas as mesas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesas
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allMesas.size} documentos encontrados")
            
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
                    
                    // ? CORRE��O: Verificar timestamp do servidor vs local
                    // dataUltimaLeitura.time e dataInstalacao.time s�o Long (n�o nullable)
                    val serverTimestamp = (mesaData["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (mesaData["dataUltimaLeitura"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: mesaFirestore.dataUltimaLeitura.time
                    val localTimestamp = mesaLocal?.dataUltimaLeitura?.time
                        ?: mesaLocal?.dataInstalacao?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
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
                    Log.e(TAG, "? Erro ao sincronizar mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val processDuration = System.currentTimeMillis() - processStartTime
            Log.d(TAG, "   ?? Processamento de documentos: ${processDuration}ms")
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Mesas (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizadas, $skippedCount puladas, $errorCount erros")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de mesas.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
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
                    
                    // Mesas geralmente n�o t�m timestamp, usar sempre atualizar se existir
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
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Mesas (COMPLETO) conclu�do: $syncCount sincronizadas, $skippedCount puladas, $errorCount erros")
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

            Log.d(TAG, "?? Colaboradores: lastSyncTimestamp=$lastSyncTimestamp, canUseIncremental=$canUseIncremental, allowRouteBootstrap=$allowRouteBootstrap")

            var incrementalExecutado = false
            if (canUseIncremental) {
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL de colaboradores (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullColaboradoresIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                incrementalExecutado = incrementalResult != null
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosColaboradores().first().size }.getOrDefault(0)
                    Log.d(TAG, "?? Colaboradores incremental: syncedCount=$syncedCount, localCount=$localCount")
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Log.w(TAG, "?? Colaboradores: incremental trouxe $syncedCount registros com base local $localCount - executando pull COMPLETO")
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de colaboradores falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de colaboradores - usando m�todo COMPLETO")
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
            Log.d(TAG, "?? Pull COMPLETO de colaboradores - documentos recebidos: ${snapshot.documents.size}")
            
            if (snapshot.isEmpty) {
                Log.w(TAG, "?? Nenhum colaborador encontrado no Firestore")
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            // ? LOG DETALHADO: Listar todos os documentos recebidos
            Log.d(TAG, "?? Documentos de colaboradores recebidos:")
            snapshot.documents.forEachIndexed { index, doc ->
                val email = (doc.data?.get("email") as? String) ?: "sem email"
                val nome = (doc.data?.get("nome") as? String) ?: "sem nome"
                Log.d(TAG, "   ${index + 1}. ID=${doc.id}, Email=$email, Nome=$nome")
            }
            
            val colaboradoresCache = appRepository.obterTodosColaboradores().first().associateBy { it.id }.toMutableMap()
            Log.d(TAG, "?? Cache local de colaboradores: ${colaboradoresCache.size} colaboradores")
            colaboradoresCache.values.forEach { col ->
                Log.d(TAG, "   - ID=${col.id}, Email=${col.email}, Nome=${col.nome}")
            }
            
            val (syncCount, skippedCount, errorCount) = processColaboradoresDocuments(snapshot.documents, colaboradoresCache)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull de colaboradores conclu�do (fallback incremental=$incrementalFallback): sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
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
                Log.w(TAG, "?? Falha ao criar query incremental para colaboradores: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "?? Colaboradores - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de colaboradores: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
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
        
        Log.d(TAG, "?? Processando ${documents.size} documentos de colaboradores...")
        
        documents.forEachIndexed { index, doc ->
            val email = (doc.data?.get("email") as? String) ?: "sem email"
            Log.d(TAG, "   [${index + 1}/${documents.size}] Processando: Email=$email, DocID=${doc.id}")
            
            when (processColaboradorDocument(doc, colaboradoresCache)) {
                ProcessResult.Synced -> {
                    syncCount++
                    Log.d(TAG, "   ? Sincronizado: Email=$email")
                }
                ProcessResult.Skipped -> {
                    skippedCount++
                    Log.d(TAG, "   ?? Pulado: Email=$email")
                }
                ProcessResult.Error -> {
                    errorCount++
                    Log.e(TAG, "   ? Erro: Email=$email")
                }
            }
        }
        
        Log.d(TAG, "?? Resultado do processamento: sync=$syncCount, skipped=$skippedCount, errors=$errorCount")
        return Triple(syncCount, skippedCount, errorCount)
    }

    private suspend fun processColaboradorDocument(
        doc: DocumentSnapshot,
        colaboradoresCache: MutableMap<Long, Colaborador>
    ): ProcessResult {
        return try {
            val colaboradorData = doc.data ?: run {
                Log.w(TAG, "?? Colaborador ${doc.id} sem dados - pulando")
                return ProcessResult.Skipped
            }
            
            val colaboradorId = doc.id.toLongOrNull()
                ?: (colaboradorData["roomId"] as? Number)?.toLong()
                ?: (colaboradorData["id"] as? Number)?.toLong()
                ?: run {
                    Log.w(TAG, "?? Colaborador ${doc.id} sem ID v�lido - pulando")
                    Log.w(TAG, "   doc.id: ${doc.id}")
                    Log.w(TAG, "   roomId: ${colaboradorData["roomId"]}")
                    Log.w(TAG, "   id: ${colaboradorData["id"]}")
                    return ProcessResult.Skipped
                }
            
            val colaboradorEmail = (colaboradorData["email"] as? String) ?: ""
            Log.d(TAG, "?? Processando colaborador: ID=$colaboradorId, Email=$colaboradorEmail")
            
            val colaboradorJson = gson.toJson(colaboradorData)
            val colaboradorFirestore = gson.fromJson(colaboradorJson, Colaborador::class.java)?.copy(id = colaboradorId)
                ?: run {
                    Log.e(TAG, "? Erro ao converter colaborador ${doc.id} do Firestore para objeto")
                    Log.e(TAG, "   JSON gerado: $colaboradorJson")
                    return ProcessResult.Error
                }
            
            val serverTimestamp = doc.getTimestamp("lastModified")?.toDate()?.time
                ?: (colaboradorData["dataUltimaAtualizacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                ?: colaboradorFirestore.dataUltimaAtualizacao.time
            
            // ? CORRE��O: Verificar duplicata por ID primeiro
            val localColaborador = colaboradoresCache[colaboradorId]
            val localTimestamp = localColaborador?.dataUltimaAtualizacao?.time ?: 0L
            
            // ? CORRE��O: Se n�o encontrou por ID, verificar por email para evitar duplicatas
            val localColaboradorPorEmail = if (localColaborador == null && colaboradorFirestore.email.isNotEmpty()) {
                try {
                    val encontrado = appRepository.obterColaboradorPorEmail(colaboradorFirestore.email)
                    if (encontrado != null) {
                        Log.d(TAG, "?? Colaborador encontrado por email: ${colaboradorFirestore.email} (ID local: ${encontrado.id})")
                    }
                    encontrado
                } catch (e: Exception) {
                    Log.e(TAG, "? Erro ao buscar colaborador por email: ${e.message}", e)
                    null
                }
            } else {
                null
            }
            
            return when {
                // Se encontrou por ID, usar l�gica normal de atualiza��o
                localColaborador != null -> {
                    if (serverTimestamp > localTimestamp) {
                        appRepository.atualizarColaborador(colaboradorFirestore)
                        colaboradoresCache[colaboradorId] = colaboradorFirestore
                        ProcessResult.Synced
                    } else {
                        ProcessResult.Skipped
                    }
                }
                // ? CORRE��O: Se encontrou por email mas com ID diferente, atualizar o existente
                localColaboradorPorEmail != null -> {
                    Log.d(TAG, "?? Colaborador duplicado encontrado por email: ${colaboradorFirestore.email} (ID local: ${localColaboradorPorEmail.id}, ID Firestore: $colaboradorId)")
                    // Atualizar o colaborador existente com os dados do Firestore, mantendo o ID local
                    val colaboradorAtualizado = colaboradorFirestore.copy(id = localColaboradorPorEmail.id)
                    if (serverTimestamp > localColaboradorPorEmail.dataUltimaAtualizacao.time) {
                        appRepository.atualizarColaborador(colaboradorAtualizado)
                        colaboradoresCache[localColaboradorPorEmail.id] = colaboradorAtualizado
                        ProcessResult.Synced
                    } else {
                        ProcessResult.Skipped
                    }
                }
                // Se n�o encontrou nem por ID nem por email, inserir novo
                else -> {
                    Log.d(TAG, "? Inserindo novo colaborador: ID=$colaboradorId, Email=$colaboradorEmail")
                    try {
                        val insertedId = appRepository.inserirColaborador(colaboradorFirestore)
                        Log.d(TAG, "? Colaborador inserido com sucesso: ID inserido=$insertedId, Email=$colaboradorEmail")
                        // Atualizar cache com o ID real inserido (pode ser diferente se o banco gerou novo ID)
                        val colaboradorInserido = colaboradorFirestore.copy(id = insertedId)
                        colaboradoresCache[insertedId] = colaboradorInserido
                        ProcessResult.Synced
                    } catch (e: Exception) {
                        Log.e(TAG, "? ERRO ao inserir colaborador: ${e.message}", e)
                        Log.e(TAG, "   Colaborador: ID=$colaboradorId, Email=$colaboradorEmail")
                        Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                        ProcessResult.Error
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "? ERRO ao processar colaborador ${doc.id}: ${e.message}", e)
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
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
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL de ciclos (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullCiclosIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosCiclos().first().size }.getOrDefault(0)
                    if (syncedCount > 0 || localCount > 0) {
                        return incrementalResult
                    }
                    Log.w(TAG, "?? Incremental de ciclos trouxe $syncedCount registros e base local possui $localCount - executando pull COMPLETO")
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de ciclos falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de ciclos - usando m�todo COMPLETO")
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
            // ? CORRE��O CR�TICA: Garantir que busca funcione mesmo sem rotas (bootstrap)
            // Se n�o h� rotas atribu�das e n�o est� em bootstrap, for�ar bootstrap temporariamente
            resetRouteFilters()
            val accessibleRoutes = getAccessibleRouteIdsInternal()
            val needsBootstrap = accessibleRoutes.isEmpty() && !allowRouteBootstrap
            
            if (needsBootstrap) {
                Log.w(TAG, "?? Bootstrap necess�rio para ciclos: habilitando temporariamente")
                allowRouteBootstrap = true
            }
            
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "?? Pull COMPLETO de ciclos - documentos recebidos: ${documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull de ciclos conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
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
                Log.w(TAG, "?? Falha ao executar query incremental para ciclos: ${e.message}")
                return null
            }
            Log.d(TAG, "?? Ciclos - incremental retornou ${documents.size} documentos (ap�s filtro de rota)")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de ciclos: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
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
     * ? NOVO (2025): Implementa sincroniza��o incremental seguindo padr�o de Clientes
     * Importante: Sincronizar tamb�m AcertoMesa relacionados
     */
    private suspend fun pullAcertos(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ACERTOS
        
        return try {
            Log.d(TAG, "?? Iniciando pull de acertos...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ACERTOS)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullAcertosIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    val resultadoIncremental = incrementalResult.fold(
                        onSuccess = { syncedCount ->
                            val localCount = runCatching { appRepository.obterTodosAcertos().first().size }.getOrDefault(0)
                            if (syncedCount > 0 || localCount > 0) {
                                Result.success(syncedCount)
                            } else {
                                Log.w(TAG, "?? Incremental de acertos trouxe $syncedCount registros com base local $localCount - executando pull completo")
                                null // Indica que precisa fazer pull completo
                            }
                        },
                        onFailure = { error ->
                            Result.failure(error)
                        }
                    )
                    // Se o resultado incremental foi bem-sucedido, retornar
                    if (resultadoIncremental != null) {
                        return resultadoIncremental
                    }
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de acertos falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de acertos - usando m�todo COMPLETO")
            }

            return pullAcertosComplete(collectionRef, entityType, startTime, timestampOverride)

        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de acertos: ${e.message}", e)
            return Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de acertos.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * 
     * Este m�todo � seguro: se qualquer coisa falhar, retorna null e o m�todo completo � usado.
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
                Log.w(TAG, "?? Erro ao executar query incremental de acertos: ${e.message}")
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
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL de acertos (ap�s filtro de rota): $totalDocuments documentos")
            
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
                    
                    // ? ESTRAT�GIA SEGURA (2025): Tentar UPDATE primeiro
                val updateCount = appRepository.atualizarAcerto(acertoFirestore)
                
                if (updateCount > 0) {
                    Log.d(TAG, "?? (Incremental) Acerto atualizado ID: ${acertoFirestore.id}")
                    // ? REMOVIDO: maintainLocalAcertoHistory - PULL n�o deve deletar dados locais (offline-first)
                    syncCount++
                    pullAcertoMesas(acertoFirestore.id)
                } else {
                    Log.d(TAG, "? (Incremental) Acerto novo inserido ID: ${acertoFirestore.id}")
                    appRepository.inserirAcerto(acertoFirestore)
                    // ? REMOVIDO: maintainLocalAcertoHistory - PULL n�o deve deletar dados locais (offline-first)
                    syncCount++
                    pullAcertoMesas(acertoFirestore.id)
                }
                    processedCount++
                    if (processedCount % 50 == 0) {
                        val elapsed = System.currentTimeMillis() - processStartTime
                        Log.d(TAG, "   ?? Progresso: $processedCount/$totalDocuments documentos processados (${elapsed}ms)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao sincronizar acerto ${doc.id}: ${e.message}", e)
                }
            }
            val processDuration = System.currentTimeMillis() - processStartTime
            Log.d(TAG, "   ?? Processamento de documentos: ${processDuration}ms")
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Acertos (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizados, $skippedCount pulados, $errorCount erros")
            Log.d(TAG, "   ?? $totalDocuments documentos processados")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de acertos.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullAcertosComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "?? Total de acertos no Firestore (ap�s filtro de rota): ${documents.size}")
            
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
                    
                // ? ESTRAT�GIA SEGURA (2025): Tentar UPDATE primeiro para evitar CASCADE DELETE dos filhos
                // Se o update retornar 0, significa que n�o existe, ent�o fazemos INSERT
                val updateCount = appRepository.atualizarAcerto(acertoFirestore)
                
                if (updateCount > 0) {
                    Log.d(TAG, "?? Acerto atualizado com sucesso ID: ${acertoFirestore.id}")
                    // ? REMOVIDO: maintainLocalAcertoHistory - PULL n�o deve deletar dados locais (offline-first)
                    syncCount++
                    pullAcertoMesas(acertoFirestore.id)
                } else {
                    Log.d(TAG, "? Acerto n�o encontrado para update, inserindo novo ID: ${acertoFirestore.id}")
                    appRepository.inserirAcerto(acertoFirestore)
                    // ? REMOVIDO: maintainLocalAcertoHistory - PULL n�o deve deletar dados locais (offline-first)
                    syncCount++
                    pullAcertoMesas(acertoFirestore.id)
                }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao sincronizar acerto ${doc.id}: ${e.message}", e)
                    Log.e(TAG, "? Stack trace: ${e.stackTraceToString()}")
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Acertos (COMPLETO) conclu�do: $syncCount sincronizados, $skippedCount pulados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de acertos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull AcertoMesas: Sincroniza mesas de acerto relacionadas
     * ? CORRE��O: Faz download de fotos do Firebase Storage quando necess�rio
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
                    
                    // ? CORRE��O: Verificar se j� existe foto local antes de baixar novamente
                    val fotoRelogioFinal = acertoMesa.fotoRelogioFinal
                    if (!fotoRelogioFinal.isNullOrEmpty() && 
                        firebaseImageUploader.isFirebaseStorageUrl(fotoRelogioFinal)) {
                        
                        // ? NOVO: Verificar se j� existe um AcertoMesa local com foto para esta mesa/acerto
                        val acertoMesaExistente = appRepository.buscarAcertoMesaPorAcertoEMesa(
                            acertoMesa.acertoId,
                            acertoMesa.mesaId
                        )
                        
                        // Se j� existe e tem foto local v�lida, reutilizar
                        val caminhoLocal = if (acertoMesaExistente != null && 
                                               !acertoMesaExistente.fotoRelogioFinal.isNullOrEmpty() &&
                                               !firebaseImageUploader.isFirebaseStorageUrl(acertoMesaExistente.fotoRelogioFinal)) {
                            val arquivoExistente = java.io.File(acertoMesaExistente.fotoRelogioFinal!!)
                            if (arquivoExistente.exists()) {
                                Log.d(TAG, "? Reutilizando foto local existente: ${acertoMesaExistente.fotoRelogioFinal}")
                                acertoMesaExistente.fotoRelogioFinal
                            } else {
                                // Arquivo foi deletado, baixar novamente
                                Log.d(TAG, "?? Arquivo local n�o existe mais, baixando novamente para mesa ${acertoMesa.mesaId}")
                                firebaseImageUploader.downloadMesaRelogio(
                                    fotoRelogioFinal,
                                    acertoMesa.mesaId,
                                    acertoMesa.acertoId // ? NOVO: Usar acertoId para nome fixo
                                )
                            }
                        } else {
                            // N�o existe foto local, baixar
                            Log.d(TAG, "?? Fazendo download de foto do rel�gio para mesa ${acertoMesa.mesaId}")
                            firebaseImageUploader.downloadMesaRelogio(
                                fotoRelogioFinal,
                                acertoMesa.mesaId,
                                acertoMesa.acertoId // ? NOVO: Usar acertoId para nome fixo
                            )
                        }
                        
                        if (caminhoLocal != null) {
                            // ? Atualizar AcertoMesa com o caminho local
                            acertoMesa = acertoMesa.copy(
                                fotoRelogioFinal = caminhoLocal
                            )
                            Log.d(TAG, "? Foto salva localmente: $caminhoLocal")
                        } else {
                            Log.w(TAG, "?? Falha ao baixar foto, mantendo URL do Firebase: $fotoRelogioFinal")
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
     * ? NOVO (2025): Implementa sincroniza��o incremental seguindo padr�o de Clientes
     */
    private suspend fun pullDespesas(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_DESPESAS
        
        return try {
            Log.d(TAG, "?? Iniciando pull de despesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_DESPESAS)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullDespesasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    // Incremental funcionou!
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullDespesasComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de despesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de despesas.
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
                Log.w(TAG, "?? Erro ao executar query incremental de despesas: ${e.message}")
                return null
            }
            val totalDocuments = documents.size
            
            if (totalDocuments == 0) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL de despesas (ap�s filtro de rota): $totalDocuments documentos")
                
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
                            Log.d(TAG, "   ?? Progresso: $processedCount/$totalDocuments documentos processados (${elapsed}ms)")
                        }
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "? Erro ao sincronizar despesa ${doc.id}: ${e.message}", e)
                    }
                }
                val processDuration = System.currentTimeMillis() - processStartTime
                Log.d(TAG, "   ?? Processamento de documentos: ${processDuration}ms")
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Despesas (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Log.d(TAG, "   ?? $totalDocuments documentos processados")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de despesas.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullDespesasComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            Log.d(TAG, "?? Total de despesas no Firestore (ap�s filtro de rota): ${documents.size}")
            
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
                    Log.d(TAG, "?? Processando despesa: ID=${doc.id}")
                    
                    val despesaId = (despesaData["roomId"] as? Long) 
                        ?: (despesaData["id"] as? Long) 
                        ?: doc.id.toLongOrNull() 
                        ?: 0L
                    
                    if (despesaId == 0L) {
                        Log.w(TAG, "?? ID inv�lido para despesa ${doc.id} - pulando")
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
                        Log.w(TAG, "?? Despesa ${doc.id} com dados inv�lidos - pulando")
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
                            Log.d(TAG, "? Despesa inserida: ID=$despesaId, Descri��o=${despesaFirestore.descricao}, CicloId=${despesaFirestore.cicloId}")
                        }
                        serverTimestamp > (localTimestamp + 1000) -> {
                            appRepository.atualizarDespesa(despesaFirestore)
                            syncCount++
                            Log.d(TAG, "? Despesa atualizada: ID=$despesaId (servidor: $serverTimestamp, local: $localTimestamp)")
                        }
                        else -> {
                            skipCount++
                            Log.d(TAG, "?? Despesa local mais recente ou igual, mantendo: ID=$despesaId (servidor: $serverTimestamp, local: $localTimestamp)")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao sincronizar despesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = 0L,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Despesas (COMPLETO) conclu�do: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de despesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull Contratos: Sincroniza contratos do Firestore para o Room
     * Importante: Sincronizar tamb�m Aditivos e Assinaturas relacionados
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
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� contratos locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 contratos mas h� $localCount locais - executando pull COMPLETO como valida��o")
                    return pullContratosComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de contratos falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de contratos - usando m�todo COMPLETO")
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
            Log.d(TAG, "?? Pull COMPLETO de contratos - documentos recebidos: ${snapshot.documents.size}")
            
            val (syncCount, skippedCount, errorCount) = processContratosDocuments(snapshot.documents)
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null,
                timestampOverride = timestampOverride
            )
            Log.d(TAG, "? Pull de contratos conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
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
            // ? CORRE��O CR�TICA: Estrat�gia h�brida para garantir que contratos n�o desapare�am
            // 1. Tentar buscar apenas contratos modificados recentemente (otimiza��o)
            // 2. Se retornar 0 mas houver contratos locais, buscar TODOS para garantir sincroniza��o completa
            
            // ? CORRE��O: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todosContratos = appRepository.buscarTodosContratos().first()
            val contratosCache = todosContratos.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de contratos carregado: ${contratosCache.size} contratos locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalContratos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todos os contratos: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� contratos locais, buscar TODOS
            val allContratos = if (incrementalContratos.isEmpty() && contratosCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 contratos mas h� ${contratosCache.size} locais - buscando TODOS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todos os contratos: ${e.message}")
                    return null
                }
            } else {
                incrementalContratos
            }
            
            Log.d(TAG, "?? Contratos - incremental: ${allContratos.size} documentos encontrados (antes do filtro de rota)")
            
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
                    
                    // ? CORRE��O: Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
                    val shouldSync = contratoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        // ? VALIDAR FK: Verificar que o cliente existe antes de inserir
                        if (!ensureEntityExists("cliente", contratoFirestore.clienteId)) {
                            Log.w(TAG, "? Contrato $contratoId skipado: cliente ${contratoFirestore.clienteId} n�o existe")
                            skippedCount++
                            return@forEach
                        }
                        
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            Log.d(TAG, "? Pull INCREMENTAL de contratos: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
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
                    // ? VALIDAR FK: Verificar que o cliente existe antes de inserir
                    if (!ensureEntityExists("cliente", contratoFirestore.clienteId)) {
                        Log.w(TAG, "? Contrato $contratoId skipado: cliente ${contratoFirestore.clienteId} n�o existe")
                        errorCount++
                        return@forEach
                    }
                    
                    try {
                        if (contratoLocal == null) {
                            appRepository.inserirContrato(contratoFirestore)
                        } else {
                            appRepository.atualizarContrato(contratoFirestore)
                        }
                            pullAditivosContrato(contratoId)
                        syncCount++
                    } catch (e: Exception) {
                        errorCount++
                        val isFkError = e.message?.contains("FOREIGN KEY", ignoreCase = true) == true
                        if (isFkError) {
                            Log.e(TAG, "?? FK CONSTRAINT VIOLATION ao inserir contrato $contratoId: ${e.message}")
                            Log.e(TAG, "   ? Verifique se cliente ${contratoFirestore.clienteId} existe localmente")
                        } else {
                            Log.e(TAG, "? Erro ao inserir contrato $contratoId: ${e.message}", e)
                        }
                    }
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
            Log.d(TAG, "?? Iniciando pull de categorias despesa...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_CATEGORIAS_DESPESA)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullCategoriasDespesaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de categorias falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de categorias - usando m�todo COMPLETO")
            }
            
            pullCategoriasDespesaComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de categorias despesa: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de categorias - documentos recebidos: ${snapshot.documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de categorias conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de categorias: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao criar query incremental para categorias: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "?? Categorias - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de categorias: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de categorias: ${e.message}", e)
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
                    Log.e(TAG, "? Erro ao processar categoria despesa ${doc.id}: ${e.message}", e)
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
            Log.d(TAG, "?? Iniciando pull de tipos despesa...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_TIPOS_DESPESA)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullTiposDespesaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de tipos falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de tipos - usando m�todo COMPLETO")
            }
            
            pullTiposDespesaComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de tipos despesa: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de tipos - documentos recebidos: ${snapshot.documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de tipos conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de tipos despesa: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao criar query incremental para tipos: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "?? Tipos - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de tipos: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de tipos despesa: ${e.message}", e)
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
                    Log.e(TAG, "? Erro ao processar tipo despesa ${doc.id}: ${e.message}", e)
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
            Log.d(TAG, "?? Iniciando pull de metas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_METAS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullMetasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de metas falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de metas - usando m�todo COMPLETO")
            }
            
            pullMetasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de metas: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de metas - documentos recebidos: ${documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de metas conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de metas: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao executar query incremental para metas: ${e.message}")
                return null
            }
            
            Log.d(TAG, "?? Metas - incremental retornou ${documents.size} documentos (ap�s filtro de rota)")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de metas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de metas: ${e.message}", e)
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
                    Log.e(TAG, "? Erro ao processar meta ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Colaborador Rotas: Sincroniza vincula��es colaborador-rota do Firestore para o Room
     */
    private suspend fun pullColaboradorRotas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_COLABORADOR_ROTA
        
        return try {
            Log.d(TAG, "?? Iniciando pull de colaborador rotas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_COLABORADOR_ROTA)
            
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL de colaborador rotas (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullColaboradorRotasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de colaborador rotas falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de colaborador rotas - usando m�todo COMPLETO")
            }
            
            pullColaboradorRotasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de colaborador rotas: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de colaborador rotas - documentos recebidos: ${documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de colaborador rotas conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de colaborador rotas: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao executar query incremental para colaborador rotas: ${e.message}")
                return null
            }
            Log.d(TAG, "?? Colaborador rotas - incremental retornou ${documents.size} documentos (ap�s filtro de rota)")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de colaborador rotas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de colaborador rotas: ${e.message}", e)
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
                    Log.e(TAG, "? Erro ao processar colaborador rota ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }

    private fun colaboradorRotaKey(colaboradorId: Long, rotaId: Long): String =
        "${colaboradorId}_${rotaId}"
    
    /**
     * Pull Aditivo Mesas: Sincroniza vincula��es aditivo-mesa do Firestore para o Room
     */
    private suspend fun pullAditivoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ADITIVO_MESAS
        
        return try {
            Log.d(TAG, "?? Iniciando pull de aditivo mesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ADITIVO_MESAS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullAditivoMesasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de aditivo mesas falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de aditivo mesas - usando m�todo COMPLETO")
            }
            
            pullAditivoMesasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de aditivo mesas: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de aditivo mesas - documentos recebidos: ${snapshot.documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de aditivo mesas conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de aditivo mesas: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao criar query incremental para aditivo mesas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "?? Aditivo mesas - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de aditivo mesas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de aditivo mesas: ${e.message}", e)
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
                    Log.e(TAG, "? Erro ao processar aditivo mesa ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Pull Contrato Mesas: Sincroniza vincula��es contrato-mesa do Firestore para o Room
     */
    private suspend fun pullContratoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CONTRATO_MESAS
        
        return try {
            Log.d(TAG, "?? Iniciando pull de contrato mesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_CONTRATO_MESAS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullContratoMesasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de contrato mesas falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de contrato mesas - usando m�todo COMPLETO")
            }
            
            pullContratoMesasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de contrato mesas: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de contrato mesas - documentos recebidos: ${snapshot.documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de contrato mesas conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de contrato mesas: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao criar query incremental para contrato mesas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "?? Contrato mesas - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de contrato mesas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de contrato mesas: ${e.message}", e)
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
                    
            // ? VALIDAR FK: Verificar que contrato e mesa existem antes de inserir
            if (!ensureEntityExists("contrato", contratoId)) {
                Log.w(TAG, "? ContratoMesa $contratoMesaId skipada: contrato $contratoId n�o existe")
                return ProcessResult.Skipped
            }
            if (!ensureEntityExists("mesa", mesaId)) {
                Log.w(TAG, "? ContratoMesa $contratoMesaId skipada: mesa $mesaId n�o existe")
                return ProcessResult.Skipped
            }
            
            val existing = contratoMesasCache[contratoMesaId]
            return try {
                appRepository.inserirContratoMesa(contratoMesa)
                contratoMesasCache[contratoMesaId] = contratoMesa
                ProcessResult.Synced
            } catch (e: Exception) {
                val isFkError = e.message?.contains("FOREIGN KEY", ignoreCase = true) == true
                if (isFkError) {
                    Log.e(TAG, "?? FK CONSTRAINT VIOLATION ao inserir contrato mesa $contratoMesaId: ${e.message}")
                    Log.e(TAG, "   ? Contrato: $contratoId, Mesa: $mesaId")
                } else {
                    Log.e(TAG, "? Erro ao inserir contrato mesa $contratoMesaId: ${e.message}", e)
                }
                ProcessResult.Error
            }
                } catch (e: Exception) {
                    Log.e(TAG, "? Erro ao processar contrato mesa ${doc.id}: ${e.message}", e)
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
            Log.d(TAG, "?? Iniciando pull de assinaturas representante legal...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ASSINATURAS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullAssinaturasIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de assinaturas falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de assinaturas - usando m�todo COMPLETO")
            }
            
            pullAssinaturasComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de assinaturas: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de assinaturas - documentos recebidos: ${snapshot.documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de assinaturas conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de assinaturas: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao criar query incremental para assinaturas: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "?? Assinaturas - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de assinaturas: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de assinaturas: ${e.message}", e)
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
            val dataProcuracao = converterTimestampParaDate(data["dataProcura��o"])
                        ?: converterTimestampParaDate(data["data_procuracao"]) ?: Date()
            val validadeProc = converterTimestampParaDate(data["validadeProcura��o"])
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
                        numeroProcuração = data["numeroProcuracao"] as? String
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
                    Log.e(TAG, "? Erro ao processar assinatura ${doc.id}: ${e.message}", e)
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
            Log.d(TAG, "?? Iniciando pull de logs auditoria...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_LOGS_AUDITORIA)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullLogsAuditoriaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de logs auditoria falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de logs auditoria - usando m�todo COMPLETO")
            }
            
            pullLogsAuditoriaComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de logs auditoria: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de logs auditoria - documentos recebidos: ${snapshot.documents.size}")
            
            val (syncCount, skippedCount, errorCount) = processLogsAuditoriaDocuments(snapshot.documents)
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            Log.d(TAG, "? Pull de logs auditoria conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de logs auditoria: ${e.message}", e)
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
            Log.d(TAG, "?? Logs auditoria - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            Log.d(TAG, "? Pull INCREMENTAL de logs auditoria: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de logs auditoria: ${e.message}", e)
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
                    Log.e(TAG, "? Erro ao processar log auditoria ${doc.id}: ${e.message}", e)
                }
            }
            
        return Triple(syncCount, skipCount, errorCount)
    }
    
    // ==================== PUSH HANDLERS (LOCAL ? SERVIDOR) ====================
    
    /**
     * ? REFATORADO (2025): Push Clientes com sincroniza��o incremental
     * Envia apenas clientes modificados desde o �ltimo push
     * Segue melhores pr�ticas Android 2025 para sincroniza��o incremental
     */
    private suspend fun pushClientes(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CLIENTES
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de clientes...")
            
            // ? NOVO: Obter �ltimo timestamp de push para filtrar apenas modificados
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val clientesLocais = appRepository.obterTodosClientes().first()
            Log.d(TAG, "?? Total de clientes locais encontrados: ${clientesLocais.size}")
            
            // ? NOVO: Filtrar apenas clientes modificados desde �ltimo push
            val clientesParaEnviar = if (canUseIncremental) {
                clientesLocais.filter { cliente ->
                    val clienteTimestamp = cliente.dataUltimaAtualizacao.time
                    clienteTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} clientes modificados desde ${Date(lastPushTimestamp)} (de ${clientesLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${clientesLocais.size} clientes")
                clientesLocais
            }
            
            if (clientesParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                Log.d(TAG, "? Nenhum cliente para enviar - push conclu�do")
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            clientesParaEnviar.forEach { cliente ->
                try {
                    // Converter Cliente para Map
                    val clienteMap = entityToMap(cliente)
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    clienteMap["roomId"] = cliente.id
                    clienteMap["id"] = cliente.id
                    
                    // ? CR�TICO: Garantir que dataUltimaAtualizacao seja enviada
                    // Se n�o tiver timestamp, usar o atual
                    if (!clienteMap.containsKey("dataUltimaAtualizacao") && 
                        !clienteMap.containsKey("data_ultima_atualizacao")) {
                        clienteMap["dataUltimaAtualizacao"] = Date()
                        clienteMap["data_ultima_atualizacao"] = Date()
                    }
                    
                    // Adicionar metadados de sincroniza��o
                    clienteMap["lastModified"] = FieldValue.serverTimestamp()
                    clienteMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    // ? CR�TICO: Usar .set() para substituir completamente o documento
                    // Isso garante que os dados locais sejam preservados na nuvem
                    val collectionRef = getCollectionReference(firestore, COLLECTION_CLIENTES)
                    collectionRef
                        .document(cliente.id.toString())
                        .set(clienteMap)
                        .await()
                    
                    // ? CORRIGIDO: Ler o documento do Firestore para obter o timestamp real do servidor
                    // Isso evita race condition onde o timestamp local difere do timestamp do servidor
                    val docSnapshot = collectionRef
                        .document(cliente.id.toString())
                        .get()
                        .await()
                    
                    // Obter o timestamp do servidor (lastModified ou dataUltimaAtualizacao)
                    val serverTimestamp = converterTimestampParaDate(docSnapshot.data?.get("lastModified"))
                        ?: converterTimestampParaDate(docSnapshot.data?.get("dataUltimaAtualizacao"))
                        ?: converterTimestampParaDate(docSnapshot.data?.get("data_ultima_atualizacao"))
                        ?: Date() // Fallback para timestamp atual se n�o encontrar
                    
                    // ? CR�TICO: Atualizar timestamp local com o timestamp do servidor
                    // Isso garante que local e servidor tenham o mesmo timestamp, evitando sobrescrita no pull
                    val clienteAtualizado = cliente.copy(dataUltimaAtualizacao = serverTimestamp)
                    appRepository.atualizarCliente(clienteAtualizado)
                    
                    syncCount++
                    bytesUploaded += clienteMap.toString().length.toLong() // Estimativa de bytes
                    Log.d(TAG, "? Cliente enviado para nuvem: ${cliente.nome} (ID: ${cliente.id}) - Timestamp local sincronizado com servidor: ${serverTimestamp.time}")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar cliente ${cliente.id} (${cliente.nome}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // ? NOVO: Salvar metadata de push ap�s sincroniza��o bem-sucedida
            savePushMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesUploaded = bytesUploaded,
                error = if (errorCount > 0) "$errorCount erros durante push" else null
            )
            
            Log.d(TAG, "? Push INCREMENTAL de clientes conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de clientes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Rotas com sincroniza��o incremental
     * Envia apenas rotas modificadas desde o �ltimo push
     */
    private suspend fun pushRotas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ROTAS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de rotas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val rotasLocais = appRepository.obterTodasRotas().first()
            Log.d(TAG, "?? Total de rotas locais encontradas: ${rotasLocais.size}")
            
            // Filtrar apenas rotas modificadas desde �ltimo push
            val rotasParaEnviar = if (canUseIncremental) {
                rotasLocais.filter { rota ->
                    rota.dataAtualizacao > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} rotas modificadas desde ${Date(lastPushTimestamp)} (de ${rotasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${rotasLocais.size} rotas")
                rotasLocais
            }
            
            if (rotasParaEnviar.isEmpty()) {
                val durationMs = System.currentTimeMillis() - startTime
                savePushMetadata(entityType, 0, durationMs)
                Log.d(TAG, "? Nenhuma rota para enviar - push conclu�do")
                return Result.success(0)
            }
            
            var syncCount = 0
            var errorCount = 0
            var bytesUploaded = 0L
            
            rotasParaEnviar.forEach { rota ->
                try {
                    Log.d(TAG, "?? Processando rota: ID=${rota.id}, Nome=${rota.nome}")
                    
                    val rotaMap = entityToMap(rota)
                    Log.d(TAG, "   Mapa criado com ${rotaMap.size} campos")
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    // O pull espera encontrar roomId no documento do Firestore
                    rotaMap["roomId"] = rota.id
                    rotaMap["id"] = rota.id // Tamb�m incluir campo id para compatibilidade
                    
                    // Adicionar metadados de sincroniza��o
                    rotaMap["lastModified"] = FieldValue.serverTimestamp()
                    rotaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = rota.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_ROTAS)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$currentCompanyId/entidades/${COLLECTION_ROTAS}/items, document=$documentId")
                    Log.d(TAG, "   Campos no mapa: ${rotaMap.keys}")
                    collectionRef
                        .document(documentId)
                        .set(rotaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += rotaMap.toString().length.toLong()
                    Log.d(TAG, "? Rota enviada com sucesso: ${rota.nome} (ID: ${rota.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar rota ${rota.id} (${rota.nome}): ${e.message}", e)
                    Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de rotas conclu�do: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Mesas com sincroniza��o incremental
     * Envia apenas mesas modificadas desde o �ltimo push
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
                    val mesaTimestamp = mesa.dataUltimaLeitura.time
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} mesas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${mesasLocais.size} mesas")
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
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    mesaMap["roomId"] = mesa.id
                    mesaMap["id"] = mesa.id
                    mesaMap["lastModified"] = FieldValue.serverTimestamp()
                    mesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    Log.d(TAG, "?? [DIAGNOSTICO] Enviando Mesa ${mesa.id}. lastModified definido como serverTimestamp()")
                    
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
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? Mesa ${mesa.id} exportada com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += mesaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar mesa ${mesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de mesas conclu�do: $syncCount enviadas, $errorCount erros, ${durationMs}ms. MaxServerTimestamp: ${Date(maxServerTimestamp)}")
            
            // Retornar o maior timestamp encontrado (ou 0 se nenhum)
            // Usamos um Result customizado ou passamos via Pair? 
            // Por enquanto, vamos manter a assinatura Result<Int> mas precisamos propagar esse timestamp.
            // VOU ALTERAR A ASSINATURA DEPOIS. Por enquanto, vou salvar o metadata aqui mesmo se for maior que o atual?
            // N�o, o ideal � retornar. Mas para n�o quebrar tudo agora, vou salvar um metadado tempor�rio ou apenas logar.
            // A estrat�gia correta � mudar a assinatura de syncPush para retornar Result<Long> ou Result<SyncResult>.
            
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Colaboradores com sincroniza��o incremental
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
                    val colaboradorTimestamp = colaborador.dataUltimaAtualizacao.time
                    colaboradorTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} colaboradores modificados desde ${Date(lastPushTimestamp)} (de ${colaboradoresLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${colaboradoresLocais.size} colaboradores")
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
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
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
            
            Log.d(TAG, "? Push INCREMENTAL de colaboradores conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
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
     * ? REFATORADO (2025): Push Ciclos com sincroniza��o incremental
     */
    private suspend fun pushCiclos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CICLOS
        
        return try {
            Log.d(TAG, "?? ===== INICIANDO PUSH DE CICLOS =====")
            Log.d(TAG, "Iniciando push INCREMENTAL de ciclos...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            // ? CORRE��O: Buscar TODOS os ciclos locais e mostrar detalhes
            val ciclosLocais = try {
                appRepository.obterTodosCiclos().first()
            } catch (e: Exception) {
                Log.w(TAG, "M�todo obterTodosCiclos n�o dispon�vel, tentando alternativa...")
                emptyList<CicloAcertoEntity>()
            }
            
            Log.d(TAG, "   ?? Total de ciclos locais: ${ciclosLocais.size}")
            ciclosLocais.forEach { ciclo ->
                Log.d(TAG, "      - Ciclo ${ciclo.numeroCiclo}/${ciclo.ano}: status=${ciclo.status}, dataInicio=${ciclo.dataInicio}, dataAtualizacao=${ciclo.dataAtualizacao}")
            }
            
            val ciclosParaEnviar = if (canUseIncremental) {
                ciclosLocais.filter { ciclo ->
                    val cicloTimestamp = ciclo.dataAtualizacao.time
                    cicloTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} ciclos modificados desde ${Date(lastPushTimestamp)} (de ${ciclosLocais.size} total)")
                    it.forEach { ciclo ->
                        Log.d(TAG, "      ? Enviando ciclo ${ciclo.numeroCiclo}/${ciclo.ano}")
                    }
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${ciclosLocais.size} ciclos")
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
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    cicloMap["roomId"] = ciclo.id
                    cicloMap["id"] = ciclo.id
                    cicloMap["lastModified"] = FieldValue.serverTimestamp()
                    cicloMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_CICLOS)
                    collectionRef
                        .document(ciclo.id.toString())
                        .set(cicloMap)
                        .await()
                    
                    // ? READ-YOUR-WRITES: Ler de volta para pegar o timestamp real do servidor
                    val snapshot = collectionRef.document(ciclo.id.toString()).get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: System.currentTimeMillis()
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? Ciclo ${ciclo.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += cicloMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar ciclo ${ciclo.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            Log.d(TAG, "? Push INCREMENTAL de ciclos conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de ciclos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Acertos com sincroniza��o incremental
     * Envia apenas acertos modificados desde o �ltimo push
     * Importante: Enviar tamb�m AcertoMesa relacionados
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
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} acertos modificados desde ${Date(lastPushTimestamp)} (de ${acertosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${acertosLocais.size} acertos")
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
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
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
            
            // ? CORRIGIDO: Manter hist�rico AP�S todos os uploads (fora do loop)
            // Chamar apenas UMA VEZ por cliente �nico, evitando m�ltiplas execu��es
            // que podem causar race conditions e deletar dados incorretamente
            val clientesAfetados = acertosParaEnviar.map { it.clienteId }.distinct()
            Log.d(TAG, "?? Limpando hist�rico para ${clientesAfetados.size} cliente(s) �nico(s)...")
            clientesAfetados.forEach { clienteId ->
                try {
                    maintainLocalAcertoHistory(clienteId, limit = 15)
                    Log.d(TAG, "   ? Hist�rico mantido para cliente $clienteId (�ltimos 15 acertos)")
                } catch (e: Exception) {
                    Log.e(TAG, "   ? Erro ao manter hist�rico do cliente $clienteId: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de acertos conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
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
     * ? CORRE��O: Faz upload de fotos locais para Firebase Storage antes de enviar
     */
    private suspend fun pushAcertoMesas(acertoId: Long) {
        try {
            val acertoMesas = appRepository.buscarAcertoMesasPorAcerto(acertoId) // Retorna List<AcertoMesa>
            
            acertoMesas.forEach { acertoMesa: AcertoMesa ->
                try {
                    var fotoParaEnviar = acertoMesa.fotoRelogioFinal
                    
                    // ? NOVO: Se houver foto local mas n�o for URL do Firebase, fazer upload
                    if (!fotoParaEnviar.isNullOrEmpty() && 
                        !firebaseImageUploader.isFirebaseStorageUrl(fotoParaEnviar)) {
                        
                        Log.d(TAG, "?? Fazendo upload de foto local para Firebase Storage (mesa ${acertoMesa.mesaId})")
                        try {
                            val uploadedUrl = firebaseImageUploader.uploadMesaRelogio(
                                fotoParaEnviar,
                                acertoMesa.mesaId
                            )
                            
                            if (uploadedUrl != null) {
                                fotoParaEnviar = uploadedUrl
                                Log.d(TAG, "? Foto enviada para Firebase Storage: $uploadedUrl")
                                
                                // ? Atualizar o AcertoMesa local com a URL do Firebase
                                val acertoMesaAtualizado = acertoMesa.copy(
                                    fotoRelogioFinal = uploadedUrl
                                )
                                appRepository.inserirAcertoMesa(acertoMesaAtualizado)
                            } else {
                                Log.w(TAG, "?? Falha no upload da foto, enviando caminho local")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro ao fazer upload da foto: ${e.message}", e)
                            // Continuar mesmo se o upload falhar, enviando o caminho local
                        }
                    }
                    
                    // ? Usar Gson para converter AcertoMesa para Map
                    // Se a foto foi atualizada, usar o objeto atualizado
                    val acertoMesaParaEnviar = if (fotoParaEnviar != acertoMesa.fotoRelogioFinal) {
                        acertoMesa.copy(fotoRelogioFinal = fotoParaEnviar)
                    } else {
                        acertoMesa
                    }
                    
                    val acertoMesaJson = gson.toJson(acertoMesaParaEnviar)
                    @Suppress("UNCHECKED_CAST")
                    val acertoMesaMap = gson.fromJson(acertoMesaJson, Map::class.java) as Map<String, Any>
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    val mutableMap = acertoMesaMap.toMutableMap()
                    mutableMap["roomId"] = acertoMesa.id
                    mutableMap["id"] = acertoMesa.id
                    mutableMap["lastModified"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_ACERTO_MESAS)
                    collectionRef
                        .document("${acertoMesa.acertoId}_${acertoMesa.mesaId}")
                        .set(mutableMap)
                        .await()
                    
                    Log.d(TAG, "? AcertoMesa ${acertoMesa.acertoId}_${acertoMesa.mesaId} enviado com foto: ${if (fotoParaEnviar != null) "sim" else "n�o"}")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar AcertoMesa ${acertoMesa.acertoId}_${acertoMesa.mesaId}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no push de AcertoMesas para acerto $acertoId: ${e.message}", e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Despesas com sincroniza��o incremental
     * Envia apenas despesas modificadas desde o �ltimo push
     */
    private suspend fun pushDespesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_DESPESAS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de despesas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val despesasLocais = appRepository.obterTodasDespesas().first()
            Log.d(TAG, "?? Total de despesas locais encontradas: ${despesasLocais.size}")
            
            // Filtrar apenas despesas modificadas (usar dataHora convertida para timestamp)
            val despesasParaEnviar = if (canUseIncremental) {
                despesasLocais.filter { despesa ->
                    val despesaTimestamp = despesa.dataHora.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    despesaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} despesas modificadas desde ${Date(lastPushTimestamp)} (de ${despesasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${despesasLocais.size} despesas")
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
                    Log.d(TAG, "?? Processando despesa: ID=${despesa.id}, Descri��o=${despesa.descricao}, CicloId=${despesa.cicloId}")
                    
                    val despesaMap = entityToMap(despesa)
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    despesaMap["roomId"] = despesa.id
                    despesaMap["id"] = despesa.id
                    despesaMap["lastModified"] = FieldValue.serverTimestamp()
                    despesaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_DESPESAS)
                    collectionRef
                        .document(despesa.id.toString())
                        .set(despesaMap)
                        .await()
                    
                    // ? CR�TICO: Atualizar timestamp local ap�s push bem-sucedido
                    // Isso evita que o pull sobrescreva os dados locais na pr�xima sincroniza��o
                    // Como Despesa usa LocalDateTime, precisamos atualizar o dataHora
                    val despesaAtualizada = despesa.copy(
                        dataHora = java.time.LocalDateTime.now()
                    )
                    appRepository.atualizarDespesa(despesaAtualizada)
                    
                    syncCount++
                    bytesUploaded += despesaMap.toString().length.toLong()
                    Log.d(TAG, "? Despesa enviada para nuvem: ${despesa.descricao} (ID: ${despesa.id}) - Timestamp local atualizado")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar despesa ${despesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de despesas conclu�do: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de despesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Contratos com sincroniza��o incremental
     * Envia apenas contratos modificados desde o �ltimo push
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
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} contratos modificados desde ${Date(lastPushTimestamp)} (de ${contratosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${contratosLocais.size} contratos")
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
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    contratoMap["roomId"] = contrato.id
                    contratoMap["id"] = contrato.id
                    contratoMap["lastModified"] = FieldValue.serverTimestamp()
                    contratoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(firestore, COLLECTION_CONTRATOS)
                    collectionRef
                        .document(contrato.id.toString())
                        .set(contratoMap)
                        .await()
                    
                    // ? READ-YOUR-WRITES: Ler de volta para pegar o timestamp real do servidor
                    val snapshot = collectionRef.document(contrato.id.toString()).get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: System.currentTimeMillis()
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? Contrato ${contrato.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
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
            
            Log.d(TAG, "? Push INCREMENTAL de contratos conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
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
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
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
     * ? REFATORADO (2025): Push Categorias Despesa com sincroniza��o incremental
     */
    private suspend fun pushCategoriasDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CATEGORIAS_DESPESA
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de categorias despesa...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val categoriasLocais = appRepository.buscarCategoriasAtivas().first()
            Log.d(TAG, "?? Total de categorias despesa locais encontradas: ${categoriasLocais.size}")
            
            val categoriasParaEnviar = if (canUseIncremental) {
                categoriasLocais.filter { categoria ->
                    val categoriaTimestamp = categoria.dataAtualizacao.time
                    categoriaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} categorias modificadas desde ${Date(lastPushTimestamp)} (de ${categoriasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${categoriasLocais.size} categorias")
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
                    Log.d(TAG, "?? Processando categoria despesa: ID=${categoria.id}, Nome=${categoria.nome}")
                    
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
                    Log.d(TAG, "? Categoria despesa enviada com sucesso: ${categoria.nome} (ID: ${categoria.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar categoria despesa ${categoria.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de categorias despesa conclu�do: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de categorias despesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Tipos Despesa com sincroniza��o incremental
     */
    private suspend fun pushTiposDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_TIPOS_DESPESA
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de tipos despesa...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val tiposLocais = appRepository.buscarTiposAtivosComCategoria().first()
                .map { it.tipoDespesa }
            Log.d(TAG, "?? Total de tipos despesa locais encontrados: ${tiposLocais.size}")
            
            val tiposParaEnviar = if (canUseIncremental) {
                tiposLocais.filter { tipo ->
                    val tipoTimestamp = tipo.dataAtualizacao.time
                    tipoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} tipos modificados desde ${Date(lastPushTimestamp)} (de ${tiposLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${tiposLocais.size} tipos")
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
                    Log.d(TAG, "?? Processando tipo despesa: ID=${tipo.id}, Nome=${tipo.nome}")
                    
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
                    Log.d(TAG, "? Tipo despesa enviado com sucesso: ${tipo.nome} (ID: ${tipo.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar tipo despesa ${tipo.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de tipos despesa conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de tipos despesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Metas com sincroniza��o incremental
     */
    private suspend fun pushMetas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_METAS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de metas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val metasLocais = appRepository.obterTodasMetas().first()
            Log.d(TAG, "?? Total de metas locais encontradas: ${metasLocais.size}")
            
            // Meta n�o tem dataAtualizacao, usar dataInicio como proxy
            val metasParaEnviar = if (canUseIncremental) {
                metasLocais.filter { meta ->
                    val metaTimestamp = meta.dataInicio.time
                    metaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} metas modificadas desde ${Date(lastPushTimestamp)} (de ${metasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${metasLocais.size} metas")
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
                    Log.d(TAG, "?? Processando meta: ID=${meta.id}, Nome=${meta.nome}, Tipo=${meta.tipo}")
                    
                    val metaMap = entityToMap(meta)
                    Log.d(TAG, "   Mapa criado com ${metaMap.size} campos")
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    metaMap["roomId"] = meta.id
                    metaMap["id"] = meta.id
                    
                    // Adicionar metadados de sincroniza��o
                    metaMap["lastModified"] = FieldValue.serverTimestamp()
                    metaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = meta.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_METAS)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$currentCompanyId/entidades/${COLLECTION_METAS}/items, document=$documentId")
                    
                    collectionRef
                        .document(documentId)
                        .set(metaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += metaMap.toString().length.toLong()
                    Log.d(TAG, "? Meta enviada com sucesso: ${meta.nome} (ID: ${meta.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar meta ${meta.id} (${meta.nome}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de metas conclu�do: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de metas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Colaborador Rotas com sincroniza��o incremental
     */
    private suspend fun pushColaboradorRotas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_COLABORADOR_ROTA
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de colaborador rotas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val colaboradorRotasLocais = appRepository.obterTodosColaboradorRotas()
            Log.d(TAG, "?? Total de colaborador rotas locais encontradas: ${colaboradorRotasLocais.size}")
            
            val colaboradorRotasParaEnviar = if (canUseIncremental) {
                colaboradorRotasLocais.filter { colaboradorRota ->
                    val vinculacaoTimestamp = colaboradorRota.dataVinculacao.time
                    vinculacaoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} vincula��es modificadas desde ${Date(lastPushTimestamp)} (de ${colaboradorRotasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${colaboradorRotasLocais.size} vincula��es")
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
                    // ? ColaboradorRota usa chave composta (colaboradorId, rotaId), ent�o geramos um ID composto
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
                    Log.d(TAG, "? ColaboradorRota enviado: Colaborador ${colaboradorRota.colaboradorId}, Rota ${colaboradorRota.rotaId} (ID: $compositeId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar colaborador rota ${colaboradorRota.colaboradorId}_${colaboradorRota.rotaId}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de colaborador rotas conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de colaborador rotas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Aditivo Mesas com sincroniza��o incremental
     * Nota: AditivoMesa n�o tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    private suspend fun pushAditivoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ADITIVO_MESAS
        
        return try {
            Log.d(TAG, "?? Iniciando push de aditivo mesas...")
            // Nota: AditivoMesa n�o tem campo de timestamp, ent�o sempre enviar todos
            // (geralmente s�o poucos registros, impacto baixo)
            val aditivoMesasLocais = appRepository.obterTodosAditivoMesas()
            Log.d(TAG, "?? Total de aditivo mesas locais encontradas: ${aditivoMesasLocais.size}")
            
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
                    Log.d(TAG, "? AditivoMesa enviado: Aditivo ${aditivoMesa.aditivoId}, Mesa ${aditivoMesa.mesaId} (ID: ${aditivoMesa.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar aditivo mesa ${aditivoMesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push de aditivo mesas conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de aditivo mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Contrato Mesas com sincroniza��o incremental
     * Nota: ContratoMesa n�o tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    private suspend fun pushContratoMesas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_CONTRATO_MESAS
        
        return try {
            Log.d(TAG, "?? Iniciando push de contrato mesas...")
            // Nota: ContratoMesa n�o tem campo de timestamp, ent�o sempre enviar todos
            // (geralmente s�o poucos registros, impacto baixo)
            val contratoMesasLocais = appRepository.obterTodosContratoMesas()
            Log.d(TAG, "?? Total de contrato mesas locais encontradas: ${contratoMesasLocais.size}")
            
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
                    Log.d(TAG, "? ContratoMesa enviado: Contrato ${contratoMesa.contratoId}, Mesa ${contratoMesa.mesaId} (ID: ${contratoMesa.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar contrato mesa ${contratoMesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push de contrato mesas conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de contrato mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Assinaturas Representante Legal com sincroniza��o incremental
     */
    private suspend fun pushAssinaturasRepresentanteLegal(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_ASSINATURAS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de assinaturas representante legal...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val assinaturasLocais = appRepository.obterTodasAssinaturasRepresentanteLegal()
            Log.d(TAG, "?? Total de assinaturas locais encontradas: ${assinaturasLocais.size}")
            
            val assinaturasParaEnviar = if (canUseIncremental) {
                assinaturasLocais.filter { assinatura ->
                    // Usar timestampCriacao (Long) ou dataCriacao como fallback
                    val assinaturaTimestamp = assinatura.timestampCriacao.takeIf { it > 0L } 
                        ?: assinatura.dataCriacao.time
                    assinaturaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} assinaturas modificadas desde ${Date(lastPushTimestamp)} (de ${assinaturasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${assinaturasLocais.size} assinaturas")
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
                    Log.d(TAG, "?? Processando assinatura: ID=${assinatura.id}, Nome=${assinatura.nomeRepresentante}")
                    
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
                    Log.d(TAG, "? Assinatura enviada com sucesso: ${assinatura.nomeRepresentante} (ID: ${assinatura.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar assinatura ${assinatura.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de assinaturas conclu�do: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de assinaturas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Logs Auditoria: Envia logs de auditoria do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Logs Auditoria com sincroniza��o incremental
     */
    private suspend fun pushLogsAuditoria(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_LOGS_AUDITORIA
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de logs auditoria...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val logsLocais = appRepository.obterTodosLogsAuditoria()
            Log.d(TAG, "?? Total de logs auditoria locais encontrados: ${logsLocais.size}")
            
            val logsParaEnviar = if (canUseIncremental) {
                logsLocais.filter { log ->
                    // Usar timestamp (Long) ou dataOperacao como fallback
                    val logTimestamp = log.timestamp.takeIf { it > 0L } 
                        ?: log.dataOperacao.time
                    logTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} logs modificados desde ${Date(lastPushTimestamp)} (de ${logsLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${logsLocais.size} logs")
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
                    Log.d(TAG, "?? Processando log auditoria: ID=${log.id}, Tipo=${log.tipoOperacao}")
                    
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
                    Log.d(TAG, "? Log auditoria enviado com sucesso: ${log.tipoOperacao} (ID: ${log.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar log auditoria ${log.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de logs auditoria conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de logs auditoria: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== UTILIT�RIOS ====================
    
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
                    // Se o campo cont�m "data" ou "timestamp" no nome, converter para Timestamp
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
                is Number -> value // Manter n�meros como est�o
                else -> value
            }
        }.toMutableMap()
    }
    
    /**
     * Verifica se dispositivo est� online.
     */
    fun isOnline(): Boolean = networkUtils.isConnected()
    
    /**
     * Obt�m status atual da sincroniza��o.
     */
    fun getSyncStatus(): SyncStatus = _syncStatus.value
    
    /**
     * Limpa status de erro.
     */
    fun clearError() {
        _syncStatus.value = _syncStatus.value.copy(error = null)
    }
    
    /**
     * Limpa opera��es antigas completadas.
     * Remove opera��es completadas h� mais de 7 dias.
     */
    suspend fun limparOperacoesAntigas() {
        try {
            appRepository.limparOperacoesSyncCompletadas(dias = 7)
            Log.d(TAG, "Opera��es antigas limpas")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar opera��es antigas: ${e.message}", e)
        }
    }
    
    // ==================== PULL/PUSH HANDLERS - ENTIDADES FALTANTES (AGENTE PARALELO) ====================
    
    /**
     * Pull PanoEstoque: Sincroniza panos do estoque do Firestore para o Room
     */
    /**
     * ? REFATORADO (2025): Pull Pano Estoque com sincroniza��o incremental
     * Nota: PanoEstoque n�o tem campo de timestamp, ent�o sempre buscar todos
     */
    private suspend fun pullPanoEstoque(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_PANOS_ESTOQUE
        
        return try {
            Log.d(TAG, "?? Iniciando pull de panos estoque...")
            // Nota: PanoEstoque n�o tem campo de timestamp, ent�o sempre buscar todos
            val collectionRef = getCollectionReference(firestore, COLLECTION_PANOS_ESTOQUE)
            val snapshot = collectionRef.get().await()
            Log.d(TAG, "?? Total de panos estoque no Firestore: ${snapshot.size()}")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando pano estoque: ID=${doc.id}")
                    
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
                        Log.w(TAG, "?? Pano estoque ID $panoId sem n�mero - pulando")
                        skipCount++
                        return@forEach
                    }
                    
                    appRepository.inserirPanoEstoque(pano)
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                    Log.d(TAG, "? PanoEstoque sincronizado: ${pano.numero} (ID: $panoId)")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar pano estoque ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, bytesDownloaded = bytesDownloaded, error = if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Pull de panos estoque conclu�do: $syncCount sincronizados, $skipCount ignorados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no pull de panos estoque: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push PanoEstoque: Envia panos do estoque modificados do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Pano Estoque com sincroniza��o incremental
     * Nota: PanoEstoque n�o tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    private suspend fun pushPanoEstoque(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_PANOS_ESTOQUE
        
        return try {
            Log.d(TAG, "?? Iniciando push de panos estoque...")
            // Nota: PanoEstoque n�o tem campo de timestamp, ent�o sempre enviar todos
            // (geralmente s�o poucos registros, impacto baixo)
            val panosLocais = appRepository.obterTodosPanosEstoque().first()
            Log.d(TAG, "?? Total de panos estoque locais encontrados: ${panosLocais.size}")
            
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
                    Log.d(TAG, "?? Processando pano estoque: ID=${pano.id}")
                    
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
                    Log.d(TAG, "? PanoEstoque enviado com sucesso: ${pano.numero} (ID: ${pano.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar pano estoque ${pano.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de panos estoque conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de panos estoque: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Mesa Vendida com sincroniza��o incremental
     */
    private suspend fun pullMesaVendida(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_VENDIDAS
        
        return try {
            Log.d(TAG, "Iniciando pull de mesas vendidas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS_VENDIDAS)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullMesaVendidaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesasVendidas().first().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� mesas vendidas locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 mesas vendidas mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullMesaVendidaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullMesaVendidaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de mesas vendidas.
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
            // ? CORRE��O CR�TICA: Estrat�gia h�brida para garantir que mesas vendidas n�o desapare�am
            // 1. Tentar buscar apenas mesas vendidas modificadas recentemente (otimiza��o)
            // 2. Se retornar 0 mas houver mesas vendidas locais, buscar TODAS para garantir sincroniza��o completa
            
            // ? CORRE��O: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todasMesasVendidas = appRepository.obterTodasMesasVendidas().first()
            val mesasVendidasCache = todasMesasVendidas.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de mesas vendidas carregado: ${mesasVendidasCache.size} mesas vendidas locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalMesasVendidas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todas as mesas vendidas: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� mesas vendidas locais, buscar TODAS
            val allMesasVendidas = if (incrementalMesasVendidas.isEmpty() && mesasVendidasCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 mesas vendidas mas h� ${mesasVendidasCache.size} locais - buscando TODAS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todas as mesas vendidas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesasVendidas
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allMesasVendidas.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allMesasVendidas.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando mesa vendida: ID=${doc.id}")
                    
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
                    
                    // ? Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: dataCriacao.time
                    val mesaVendidaLocal = mesasVendidasCache[mesaVendidaId]
                    val localTimestamp = mesaVendidaLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
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
                            Log.w(TAG, "?? Mesa vendida ID $mesaVendidaId sem n�mero - pulando")
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
                        Log.d(TAG, "? MesaVendida sincronizada: ${mesaVendida.numeroMesa} (ID: $mesaVendidaId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "?? Mesa vendida local mais recente ou igual, mantendo: ID=$mesaVendidaId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar mesa vendida ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de mesas vendidas: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Falha na sincroniza��o incremental de mesas vendidas: ${e.message}")
            return null // Retorna null para usar fallback completo
        }
    }
    
    /**
     * ? NOVO (2025): Pull completo de mesas vendidas (fallback)
     */
    private suspend fun pullMesaVendidaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "?? Pull COMPLETO de mesas vendidas - documentos recebidos: ${snapshot.size()}")
            
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
                        // Mesa vendida geralmente n�o � atualizada, mas se necess�rio, inserir novamente
                        appRepository.inserirMesaVendida(mesaVendida)
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar mesa vendida ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull COMPLETO de mesas vendidas: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Mesa Vendida com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025: n�o altera dados locais durante exporta��o
     */
    private suspend fun pushMesaVendida(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_VENDIDAS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de mesas vendidas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val mesasVendidasLocais = appRepository.obterTodasMesasVendidas().first()
            Log.d(TAG, "?? Total de mesas vendidas locais encontradas: ${mesasVendidasLocais.size}")
            
            // ? Filtrar apenas mesas vendidas modificadas desde �ltimo push
            val mesasParaEnviar = if (canUseIncremental) {
                mesasVendidasLocais.filter { mesaVendida ->
                    val mesaTimestamp = mesaVendida.dataCriacao.time
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} mesas vendidas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasVendidasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${mesasVendidasLocais.size} mesas vendidas")
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
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
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
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? Mesa vendida ${mesaVendida.id} exportada com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += mesaVendidaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar mesa vendida ${mesaVendida.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de mesas vendidas conclu�do: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Stock Item com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025
     */
    private suspend fun pullStockItem(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_STOCK_ITEMS
        
        return try {
            Log.d(TAG, "Iniciando pull de stock items...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_STOCK_ITEMS)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullStockItemIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosStockItems().first().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� stock items locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 stock items mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullStockItemComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullStockItemComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de stock items.
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
            // ? CORRE��O CR�TICA: Estrat�gia h�brida para garantir que stock items n�o desapare�am
            resetRouteFilters()
            val todosStockItems = appRepository.obterTodosStockItems().first()
            val stockItemsCache = todosStockItems.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de stock items carregado: ${stockItemsCache.size} stock items locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalStockItems = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todos os stock items: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� stock items locais, buscar TODOS
            val allStockItems = if (incrementalStockItems.isEmpty() && stockItemsCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 stock items mas h� ${stockItemsCache.size} locais - buscando TODOS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todos os stock items: ${e.message}")
                    return null
                }
            } else {
                incrementalStockItems
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allStockItems.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allStockItems.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando stock item: ID=${doc.id}")
                    
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
                    
                    // ? Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: updatedAt.time
                    val stockItemLocal = stockItemsCache[stockItemId]
                    val localTimestamp = stockItemLocal?.updatedAt?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
                    val shouldSync = stockItemLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (stockItem.name.isBlank()) {
                            Log.w(TAG, "?? Stock item ID $stockItemId sem nome - pulando")
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
                        Log.d(TAG, "? StockItem sincronizado: ${stockItem.name} (ID: $stockItemId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "?? Stock item local mais recente ou igual, mantendo: ID=$stockItemId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar stock item ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de stock items: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Falha na sincroniza��o incremental de stock items: ${e.message}")
            return null // Retorna null para usar fallback completo
        }
    }
    
    /**
     * ? NOVO (2025): Pull completo de stock items (fallback)
     */
    private suspend fun pullStockItemComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "?? Pull COMPLETO de stock items - documentos recebidos: ${snapshot.size()}")
            
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
                    Log.e(TAG, "? Erro ao processar stock item ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull COMPLETO de stock items: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Stock Item com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025: n�o altera dados locais durante exporta��o
     */
    private suspend fun pushStockItem(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_STOCK_ITEMS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de stock items...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val stockItemsLocais = appRepository.obterTodosStockItems().first()
            Log.d(TAG, "?? Total de stock items locais encontrados: ${stockItemsLocais.size}")
            
            // ? Filtrar apenas stock items modificados desde �ltimo push (usar updatedAt)
            val itemsParaEnviar = if (canUseIncremental) {
                stockItemsLocais.filter { stockItem ->
                    val itemTimestamp = stockItem.updatedAt.time
                    itemTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} stock items modificados desde ${Date(lastPushTimestamp)} (de ${stockItemsLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${stockItemsLocais.size} stock items")
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
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
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
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? Stock item ${stockItem.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += stockItemMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Erro ao enviar stock item ${stockItem.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de stock items conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "Erro no push de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Mesa Reformada com sincroniza��o incremental
     */
    private suspend fun pullMesaReformada(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_REFORMADAS
        
        return try {
            Log.d(TAG, "Iniciando pull de mesas reformadas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS_REFORMADAS)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullMesaReformadaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesasReformadas().first().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� mesas reformadas locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 mesas reformadas mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullMesaReformadaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullMesaReformadaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de mesas reformadas.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida.
     */
    private suspend fun tryPullMesaReformadaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ? ANDROID 2025: Estrat�gia h�brida para garantir que mesas reformadas n�o desapare�am
            // 1. Tentar buscar apenas mesas reformadas modificadas recentemente (otimiza��o)
            // 2. Se retornar 0 mas houver mesas reformadas locais, buscar TODAS para garantir sincroniza��o completa
            
            // ? CORRE��O: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todasMesasReformadas = appRepository.obterTodasMesasReformadas().first()
            val mesasReformadasCache = todasMesasReformadas.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de mesas reformadas carregado: ${mesasReformadasCache.size} mesas reformadas locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalMesasReformadas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todas as mesas reformadas: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� mesas reformadas locais, buscar TODAS
            val allMesasReformadas = if (incrementalMesasReformadas.isEmpty() && mesasReformadasCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 mesas reformadas mas h� ${mesasReformadasCache.size} locais - buscando TODAS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todas as mesas reformadas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesasReformadas
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allMesasReformadas.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allMesasReformadas.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando mesa reformada: ID=${doc.id}")
                    
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
                    
                    // ? ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: mesaReformada.dataCriacao.time
                    val mesaReformadaLocal = mesasReformadasCache[mesaReformadaId]
                    val localTimestamp = mesaReformadaLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
                    val shouldSync = mesaReformadaLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (mesaReformada.numeroMesa.isBlank()) {
                            Log.w(TAG, "?? Mesa reformada ID $mesaReformadaId sem n�mero - pulando")
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
                        Log.d(TAG, "? MesaReformada sincronizada: ${mesaReformada.numeroMesa} (ID: $mesaReformadaId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "?? Mesa reformada local mais recente ou igual, mantendo: ID=$mesaReformadaId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar mesa reformada ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull MesasReformadas (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizadas, $skipCount puladas, $errorCount erros")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental de mesas reformadas: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de mesas reformadas.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullMesaReformadaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "?? Pull COMPLETO de mesas reformadas - documentos recebidos: ${snapshot.size()}")
            
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
                    Log.e(TAG, "? Erro ao processar mesa reformada ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull MesasReformadas (COMPLETO) conclu�do: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no pull completo de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push MesaReformada: Envia mesas reformadas do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Mesa Reformada com sincroniza��o incremental
     */
    private suspend fun pushMesaReformada(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_REFORMADAS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de mesas reformadas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val mesasReformadasLocais = appRepository.obterTodasMesasReformadas().first()
            Log.d(TAG, "?? Total de mesas reformadas locais encontradas: ${mesasReformadasLocais.size}")
            
            val mesasParaEnviar = if (canUseIncremental) {
                mesasReformadasLocais.filter { mesaReformada ->
                    val mesaTimestamp = mesaReformada.dataCriacao.time
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} mesas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasReformadasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${mesasReformadasLocais.size} mesas")
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
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? MesaReformada ${mesaReformada.id} exportada com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += mesaReformadaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar mesa reformada ${mesaReformada.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de mesas reformadas conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull PanoMesa: Sincroniza vincula��es pano-mesa do Firestore para o Room
     */
    /**
     * ? REFATORADO (2025): Pull Pano Mesa com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida
     */
    private suspend fun pullPanoMesa(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_PANO_MESAS
        
        return try {
            Log.d(TAG, "Iniciando pull de pano mesas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_PANO_MESAS)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullPanoMesaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosPanoMesa().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� pano mesas locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 pano mesas mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullPanoMesaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullPanoMesaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de pano mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de pano mesas.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida.
     */
    private suspend fun tryPullPanoMesaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ? ANDROID 2025: Estrat�gia h�brida para garantir que pano mesas n�o desapare�am
            resetRouteFilters()
            val todosPanoMesas = appRepository.obterTodosPanoMesa()
            val panoMesasCache = todosPanoMesas.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de pano mesas carregado: ${panoMesasCache.size} pano mesas locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalPanoMesas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todas as pano mesas: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� pano mesas locais, buscar TODAS
            val allPanoMesas = if (incrementalPanoMesas.isEmpty() && panoMesasCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 pano mesas mas h� ${panoMesasCache.size} locais - buscando TODAS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todas as pano mesas: ${e.message}")
                    return null
                }
            } else {
                incrementalPanoMesas
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allPanoMesas.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allPanoMesas.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando pano mesa: ID=${doc.id}")
                    
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
                    
                    // ? ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: panoMesa.dataCriacao.time
                    val panoMesaLocal = panoMesasCache[panoMesaId]
                    val localTimestamp = panoMesaLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
                    val shouldSync = panoMesaLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (panoMesa.mesaId == 0L || panoMesa.panoId == 0L) {
                            Log.w(TAG, "?? Pano mesa ID $panoMesaId sem mesaId ou panoId - pulando")
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
                        Log.d(TAG, "? PanoMesa sincronizado: Mesa ${panoMesa.mesaId}, Pano ${panoMesa.panoId} (ID: $panoMesaId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "?? Pano mesa local mais recente ou igual, mantendo: ID=$panoMesaId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar pano mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull PanoMesas (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental de pano mesas: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de pano mesas.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullPanoMesaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "?? Pull COMPLETO de pano mesas - documentos recebidos: ${snapshot.size()}")
            
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
                    Log.e(TAG, "? Erro ao processar pano mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull PanoMesas (COMPLETO) conclu�do: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no pull completo de pano mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push PanoMesa com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025: n�o altera dados locais durante exporta��o
     */
    private suspend fun pushPanoMesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_PANO_MESAS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de pano mesas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val panoMesasLocais = appRepository.obterTodosPanoMesa()
            Log.d(TAG, "?? Total de pano mesas locais encontrados: ${panoMesasLocais.size}")
            
            // ? Filtrar apenas pano mesas modificados desde �ltimo push (usar dataCriacao)
            val panoMesasParaEnviar = if (canUseIncremental) {
                panoMesasLocais.filter { panoMesa: PanoMesa ->
                    val panoMesaTimestamp = panoMesa.dataCriacao.time
                    panoMesaTimestamp > lastPushTimestamp
                }.also { filteredList ->
                    Log.d(TAG, "?? Push INCREMENTAL: ${filteredList.size} pano mesas modificados desde ${Date(lastPushTimestamp)} (de ${panoMesasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${panoMesasLocais.size} pano mesas")
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
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? PanoMesa ${panoMesa.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += panoMesaMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar pano mesa ${panoMesa.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de pano mesas conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de pano mesas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Historico Manuten��o Mesa com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida
     */
    private suspend fun pullHistoricoManutencaoMesa(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_MANUTENCAO_MESA
        
        return try {
            Log.d(TAG, "Iniciando pull de hist�rico manuten��o mesa...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_MANUTENCAO_MESA)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullHistoricoManutencaoMesaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosHistoricoManutencaoMesa().first().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� hist�ricos locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 hist�ricos mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullHistoricoManutencaoMesaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullHistoricoManutencaoMesaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de hist�rico manuten��o mesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de hist�rico manuten��o mesa.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida.
     */
    private suspend fun tryPullHistoricoManutencaoMesaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ? ANDROID 2025: Estrat�gia h�brida para garantir que hist�ricos n�o desapare�am
            resetRouteFilters()
            val todosHistoricos = appRepository.obterTodosHistoricoManutencaoMesa().first()
            val historicosCache = todosHistoricos.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de hist�ricos manuten��o mesa carregado: ${historicosCache.size} hist�ricos locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalHistoricos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todos os hist�ricos: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� hist�ricos locais, buscar TODOS
            val allHistoricos = if (incrementalHistoricos.isEmpty() && historicosCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 hist�ricos mas h� ${historicosCache.size} locais - buscando TODOS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todos os hist�ricos: ${e.message}")
                    return null
                }
            } else {
                incrementalHistoricos
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allHistoricos.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allHistoricos.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando hist�rico manuten��o mesa: ID=${doc.id}")
                    
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
                    
                    // ? ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: historico.dataCriacao.time
                    val historicoLocal = historicosCache[historicoId]
                    val localTimestamp = historicoLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
                    val shouldSync = historicoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (historico.mesaId == 0L) {
                            Log.w(TAG, "?? Hist�rico manuten��o mesa ID $historicoId sem mesaId - pulando")
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
                        Log.d(TAG, "? HistoricoManutencaoMesa sincronizado: Mesa ${historico.numeroMesa} (ID: $historicoId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "?? Hist�rico local mais recente ou igual, mantendo: ID=$historicoId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar hist�rico manuten��o mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull HistoricoManutencaoMesa (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental de hist�rico manuten��o mesa: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de hist�rico manuten��o mesa.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullHistoricoManutencaoMesaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "?? Pull COMPLETO de hist�rico manuten��o mesa - documentos recebidos: ${snapshot.size()}")
            
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
                    Log.e(TAG, "? Erro ao processar hist�rico manuten��o mesa ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull HistoricoManutencaoMesa (COMPLETO) conclu�do: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no pull completo de hist�rico manuten��o mesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoManutencaoMesa: Envia hist�rico de manuten��o de mesas modificado do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Historico Manuten��o Mesa com sincroniza��o incremental
     */
    private suspend fun pushHistoricoManutencaoMesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_MANUTENCAO_MESA
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de hist�rico manuten��o mesa...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val historicosLocais = appRepository.obterTodosHistoricoManutencaoMesa().first()
            Log.d(TAG, "?? Total de hist�rico manuten��o mesa locais encontrados: ${historicosLocais.size}")
            
            val historicosParaEnviar = if (canUseIncremental) {
                historicosLocais.filter { historico ->
                    val historicoTimestamp = historico.dataCriacao.time
                    historicoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} hist�ricos modificados desde ${Date(lastPushTimestamp)} (de ${historicosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${historicosLocais.size} hist�ricos")
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
                    Log.d(TAG, "?? Processando hist�rico manuten��o mesa: ID=${historico.id}")
                    
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
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? HistoricoManutencaoMesa ${historico.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += historicoMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar hist�rico manuten��o mesa ${historico.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de hist�rico manuten��o mesa conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de hist�rico manuten��o mesa: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Historico Manuten��o Ve�culo com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida
     */
    private suspend fun pullHistoricoManutencaoVeiculo(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_MANUTENCAO_VEICULO
        
        return try {
            Log.d(TAG, "Iniciando pull de hist�rico manuten��o ve�culo...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_MANUTENCAO_VEICULO)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullHistoricoManutencaoVeiculoIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosHistoricoManutencaoVeiculo().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� hist�ricos locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 hist�ricos mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullHistoricoManutencaoVeiculoComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullHistoricoManutencaoVeiculoComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de hist�rico manuten��o ve�culo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de hist�rico manuten��o ve�culo.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida.
     */
    private suspend fun tryPullHistoricoManutencaoVeiculoIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ? ANDROID 2025: Estrat�gia h�brida para garantir que hist�ricos n�o desapare�am
            resetRouteFilters()
            val todosHistoricos = appRepository.obterTodosHistoricoManutencaoVeiculo()
            val historicosCache = todosHistoricos.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de hist�ricos manuten��o ve�culo carregado: ${historicosCache.size} hist�ricos locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalHistoricos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todos os hist�ricos: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� hist�ricos locais, buscar TODOS
            val allHistoricos = if (incrementalHistoricos.isEmpty() && historicosCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 hist�ricos mas h� ${historicosCache.size} locais - buscando TODOS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todos os hist�ricos: ${e.message}")
                    return null
                }
            } else {
                incrementalHistoricos
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allHistoricos.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allHistoricos.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando hist�rico manuten��o ve�culo: ID=${doc.id}")
                    
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
                    
                    // ? ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: historico.dataCriacao.time
                    val historicoLocal = historicosCache[historicoId]
                    val localTimestamp = historicoLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
                    val shouldSync = historicoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (historico.veiculoId == 0L) {
                            Log.w(TAG, "?? Hist�rico manuten��o ve�culo ID $historicoId sem veiculoId - pulando")
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
                        Log.d(TAG, "? HistoricoManutencaoVeiculo sincronizado: Ve�culo ${historico.veiculoId} (ID: $historicoId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "?? Hist�rico local mais recente ou igual, mantendo: ID=$historicoId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar hist�rico manuten��o ve�culo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull HistoricoManutencaoVeiculo (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental de hist�rico manuten��o ve�culo: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de hist�rico manuten��o ve�culo.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullHistoricoManutencaoVeiculoComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "?? Pull COMPLETO de hist�rico manuten��o ve�culo - documentos recebidos: ${snapshot.size()}")
            
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
                    Log.e(TAG, "? Erro ao processar hist�rico manuten��o ve�culo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull HistoricoManutencaoVeiculo (COMPLETO) conclu�do: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no pull completo de hist�rico manuten��o ve�culo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoManutencaoVeiculo: Envia hist�rico de manuten��o de ve�culos do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Historico Manuten��o Ve�culo com sincroniza��o incremental
     */
    private suspend fun pushHistoricoManutencaoVeiculo(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_MANUTENCAO_VEICULO
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de hist�rico manuten��o ve�culo...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val historicosLocais = appRepository.obterTodosHistoricoManutencaoVeiculo()
            Log.d(TAG, "?? Total de hist�ricos de manuten��o locais encontrados: ${historicosLocais.size}")
            
            val historicosParaEnviar = if (canUseIncremental) {
                historicosLocais.filter { historico ->
                    val historicoTimestamp = historico.dataCriacao.time
                    historicoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} hist�ricos modificados desde ${Date(lastPushTimestamp)} (de ${historicosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${historicosLocais.size} hist�ricos")
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
                    Log.d(TAG, "?? Processando hist�rico manuten��o: ID=${historico.id}, Ve�culo=${historico.veiculoId}")
                    
                    val historicoMap = entityToMap(historico)
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    historicoMap["roomId"] = historico.id
                    historicoMap["id"] = historico.id
                    
                    // Adicionar metadados de sincroniza��o
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
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? HistoricoManutencaoVeiculo ${historico.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += historicoMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar hist�rico manuten��o ${historico.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de hist�rico manuten��o ve�culo conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de hist�rico manuten��o ve�culo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Historico Combust�vel Ve�culo com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida
     */
    private suspend fun pullHistoricoCombustivelVeiculo(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO
        
        return try {
            Log.d(TAG, "Iniciando pull de hist�rico combust�vel ve�culo...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullHistoricoCombustivelVeiculoIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosHistoricoCombustivelVeiculo().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� hist�ricos locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 hist�ricos mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullHistoricoCombustivelVeiculoComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullHistoricoCombustivelVeiculoComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de hist�rico combust�vel ve�culo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de hist�rico combust�vel ve�culo.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida.
     */
    private suspend fun tryPullHistoricoCombustivelVeiculoIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ? ANDROID 2025: Estrat�gia h�brida para garantir que hist�ricos n�o desapare�am
            resetRouteFilters()
            val todosHistoricos = appRepository.obterTodosHistoricoCombustivelVeiculo()
            val historicosCache = todosHistoricos.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de hist�ricos combust�vel ve�culo carregado: ${historicosCache.size} hist�ricos locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalHistoricos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todos os hist�ricos: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� hist�ricos locais, buscar TODOS
            val allHistoricos = if (incrementalHistoricos.isEmpty() && historicosCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 hist�ricos mas h� ${historicosCache.size} locais - buscando TODOS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todos os hist�ricos: ${e.message}")
                    return null
                }
            } else {
                incrementalHistoricos
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allHistoricos.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allHistoricos.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando hist�rico combust�vel ve�culo: ID=${doc.id}")
                    
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
                    
                    // ? ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: historico.dataCriacao.time
                    val historicoLocal = historicosCache[historicoId]
                    val localTimestamp = historicoLocal?.dataCriacao?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
                    val shouldSync = historicoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (historico.veiculoId == 0L) {
                            Log.w(TAG, "?? Hist�rico combust�vel ve�culo ID $historicoId sem veiculoId - pulando")
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
                        Log.d(TAG, "? HistoricoCombustivelVeiculo sincronizado: Ve�culo ${historico.veiculoId} (ID: $historicoId)")
                    } else {
                        skipCount++
                        Log.d(TAG, "?? Hist�rico local mais recente ou igual, mantendo: ID=$historicoId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar hist�rico combust�vel ve�culo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull HistoricoCombustivelVeiculo (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental de hist�rico combust�vel ve�culo: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de hist�rico combust�vel ve�culo.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullHistoricoCombustivelVeiculoComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "?? Pull COMPLETO de hist�rico combust�vel ve�culo - documentos recebidos: ${snapshot.size()}")
            
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
                    Log.e(TAG, "? Erro ao processar hist�rico combust�vel ve�culo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull HistoricoCombustivelVeiculo (COMPLETO) conclu�do: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no pull completo de hist�rico combust�vel ve�culo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Ve�culos com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida
     */
    private suspend fun pullVeiculos(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_VEICULOS
        
        return try {
            Log.d(TAG, "Iniciando pull de ve�culos...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_VEICULOS)
            
            // Verificar se podemos tentar sincroniza��o incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincroniza��o incremental
                Log.d(TAG, "?? Tentando sincroniza��o INCREMENTAL (�ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullVeiculosIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosVeiculos().first().size }.getOrDefault(0)
                    
                    // ? VALIDA��O: Se incremental retornou 0 mas h� ve�culos locais, for�ar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Log.w(TAG, "?? Incremental retornou 0 ve�culos mas h� $localCount locais - executando pull COMPLETO como valida��o")
                        return pullVeiculosComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar m�todo completo
                    Log.w(TAG, "?? Sincroniza��o incremental falhou, usando m�todo COMPLETO como fallback")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o - usando m�todo COMPLETO")
            }
            
            // M�todo completo (sempre funciona, mesmo c�digo que estava antes)
            pullVeiculosComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pull de ve�culos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincroniza��o incremental de ve�culos.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores pr�ticas Android 2025 com estrat�gia h�brida.
     */
    private suspend fun tryPullVeiculosIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ? ANDROID 2025: Estrat�gia h�brida para garantir que ve�culos n�o desapare�am
            resetRouteFilters()
            val todosVeiculos = appRepository.obterTodosVeiculos().first()
            val veiculosCache = todosVeiculos.associateBy { it.id }
            Log.d(TAG, "   ?? Cache de ve�culos carregado: ${veiculosCache.size} ve�culos locais")
            
            // Tentar query incremental primeiro (otimiza��o)
            val incrementalVeiculos = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "?? Query incremental falhou, buscando todos os ve�culos: ${e.message}")
                emptyList()
            }
            
            // ? CORRE��O: Se incremental retornou 0 mas h� ve�culos locais, buscar TODOS
            val allVeiculos = if (incrementalVeiculos.isEmpty() && veiculosCache.isNotEmpty()) {
                Log.w(TAG, "?? Incremental retornou 0 ve�culos mas h� ${veiculosCache.size} locais - buscando TODOS para garantir sincroniza��o")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Log.w(TAG, "?? Erro ao buscar todos os ve�culos: ${e.message}")
                    return null
                }
            } else {
                incrementalVeiculos
            }
            
            Log.d(TAG, "?? Sincroniza��o INCREMENTAL: ${allVeiculos.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allVeiculos.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Log.d(TAG, "?? Processando ve�culo: ID=${doc.id}, Placa=${data["placa"]}")
                    
                    val veiculoId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    if (veiculoId == 0L) {
                        Log.w(TAG, "?? ID inv�lido para ve�culo ${doc.id} - pulando")
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
                    
                    // ? ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCompra"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: veiculo.dataCompra?.time ?: System.currentTimeMillis()
                    val veiculoLocal = veiculosCache[veiculoId]
                    val localTimestamp = veiculoLocal?.dataCompra?.time ?: 0L
                    
                    // Sincronizar se: n�o existe localmente OU servidor � mais recente OU foi modificado desde �ltima sync
                    val shouldSync = veiculoLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (veiculo.placa.isBlank()) {
                            Log.w(TAG, "?? Ve�culo ID $veiculoId sem placa - pulando")
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
                        Log.d(TAG, "? Ve�culo sincronizado: ${veiculo.placa} (ID: ${veiculo.id})")
                    } else {
                        skipCount++
                        Log.d(TAG, "?? Ve�culo local mais recente ou igual, mantendo: ID=$veiculoId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao processar ve�culo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincroniza��o
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Veiculos (INCREMENTAL) conclu�do:")
            Log.d(TAG, "   ?? $syncCount sincronizados, $skipCount pulados, $errorCount erros")
            Log.d(TAG, "   ?? Dura��o: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.w(TAG, "?? Erro na sincroniza��o incremental de ve�culos: ${e.message}")
            null // Falhou, usar m�todo completo
        }
    }
    
    /**
     * M�todo completo de sincroniza��o de ve�culos.
     * Este � o m�todo original que sempre funcionou - N�O ALTERAR A L�GICA DE PROCESSAMENTO.
     */
    private suspend fun pullVeiculosComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Log.d(TAG, "?? Pull COMPLETO de ve�culos - documentos recebidos: ${snapshot.size()}")
            
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
                    Log.e(TAG, "? Erro ao processar ve�culo ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o completa" else null,
                timestampOverride = timestampOverride
            )
            
            Log.d(TAG, "? Pull Veiculos (COMPLETO) conclu�do: $syncCount sincronizados, $skipCount ignorados, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no pull completo de ve�culos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push HistoricoCombustivelVeiculo: Envia hist�rico de combust�vel de ve�culos modificado do Room para o Firestore
     * TODO: Adicionar m�todo obterTodosHistoricoCombustivelVeiculo() no AppRepository
     */
    /**
     * Push HistoricoCombustivelVeiculo: Envia hist�rico de combust�vel de ve�culos do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Historico Combust�vel Ve�culo com sincroniza��o incremental
     */
    private suspend fun pushHistoricoCombustivelVeiculo(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_HISTORICO_COMBUSTIVEL_VEICULO
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de hist�rico combust�vel ve�culo...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val historicosLocais = appRepository.obterTodosHistoricoCombustivelVeiculo()
            Log.d(TAG, "?? Total de hist�ricos de combust�vel locais encontrados: ${historicosLocais.size}")
            
            val historicosParaEnviar = if (canUseIncremental) {
                historicosLocais.filter { historico ->
                    val historicoTimestamp = historico.dataCriacao.time
                    historicoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} hist�ricos modificados desde ${Date(lastPushTimestamp)} (de ${historicosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${historicosLocais.size} hist�ricos")
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
                    Log.d(TAG, "?? Processando hist�rico combust�vel: ID=${historico.id}, Ve�culo=${historico.veiculoId}")
                    
                    val historicoMap = entityToMap(historico)
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    historicoMap["roomId"] = historico.id
                    historicoMap["id"] = historico.id
                    
                    // Adicionar metadados de sincroniza��o
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
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? HistoricoCombustivelVeiculo ${historico.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += historicoMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar hist�rico combust�vel ${historico.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de hist�rico combust�vel ve�culo conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de hist�rico combust�vel ve�culo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Veiculos: Envia ve�culos do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Veiculos com sincroniza��o incremental
     * Segue melhores pr�ticas Android 2025: n�o altera dados locais durante exporta��o
     */
    private suspend fun pushVeiculos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_VEICULOS
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de ve�culos...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val veiculosLocais = appRepository.obterTodosVeiculos().first()
            Log.d(TAG, "?? Total de ve�culos locais encontrados: ${veiculosLocais.size}")
            
            // ? Filtrar apenas ve�culos modificados desde �ltimo push (usar dataCompra ou timestamp)
            val veiculosParaEnviar = if (canUseIncremental) {
                veiculosLocais.filter { veiculo ->
                    val veiculoTimestamp = veiculo.dataCompra?.time ?: System.currentTimeMillis()
                    veiculoTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} ve�culos modificados desde ${Date(lastPushTimestamp)} (de ${veiculosLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todos os ${veiculosLocais.size} ve�culos")
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
                    Log.d(TAG, "?? Processando ve�culo: ID=${veiculo.id}, Nome=${veiculo.nome}, Placa=${veiculo.placa}")
                    
                    val veiculoMap = entityToMap(veiculo)
                    Log.d(TAG, "   Mapa criado com ${veiculoMap.size} campos")
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    veiculoMap["roomId"] = veiculo.id
                    veiculoMap["id"] = veiculo.id
                    
                    // Adicionar metadados de sincroniza��o
                    veiculoMap["lastModified"] = FieldValue.serverTimestamp()
                    veiculoMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = veiculo.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_VEICULOS)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$currentCompanyId/entidades/${COLLECTION_VEICULOS}/items, document=$documentId")
                    
                    val docRef = collectionRef.document(documentId)
                    
                    // 1. Escrever
                    docRef.set(veiculoMap).await()
                    
                    // 2. Ler de volta para pegar o timestamp real do servidor (Read-Your-Writes)
                    val snapshot = docRef.get().await()
                    val serverTimestamp = snapshot.getTimestamp("lastModified")?.toDate()?.time ?: 0L
                    
                    // ? CORRE��O CR�TICA: N�O alterar dados locais durante exporta��o (push)
                    // Os dados locais devem permanecer inalterados na exporta��o
                    // A atualiza��o dos dados locais acontece apenas na importa��o (pull)
                    // quando h� dados novos no servidor que devem ser sincronizados
                    Log.d(TAG, "? Veiculo ${veiculo.id} exportado com sucesso para nuvem (timestamp servidor: $serverTimestamp). Dados locais preservados.")
                    
                    syncCount++
                    bytesUploaded += veiculoMap.toString().length.toLong()
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar ve�culo ${veiculo.id} (${veiculo.nome}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de ve�culos conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de ve�culos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push Meta Colaborador: Envia metas de colaborador do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Meta Colaborador com sincroniza��o incremental
     */
    private suspend fun pushMetaColaborador(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_META_COLABORADOR
        
        return try {
            Log.d(TAG, "?? Iniciando push INCREMENTAL de meta colaborador...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val metasLocais = appRepository.obterTodasMetaColaborador().first()
            Log.d(TAG, "?? Total de meta colaborador locais encontradas: ${metasLocais.size}")
            
            val metasParaEnviar = if (canUseIncremental) {
                metasLocais.filter { meta ->
                    val metaTimestamp = meta.dataCriacao.time
                    metaTimestamp > lastPushTimestamp
                }.also {
                    Log.d(TAG, "?? Push INCREMENTAL: ${it.size} metas modificadas desde ${Date(lastPushTimestamp)} (de ${metasLocais.size} total)")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o PUSH - enviando todas as ${metasLocais.size} metas")
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
                    Log.d(TAG, "?? Processando meta colaborador: ID=${meta.id}, Tipo=${meta.tipoMeta}, ColaboradorId=${meta.colaboradorId}")
                    
                    val metaMap = entityToMap(meta)
                    Log.d(TAG, "   Mapa criado com ${metaMap.size} campos")
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    metaMap["roomId"] = meta.id
                    metaMap["id"] = meta.id
                    
                    // Adicionar metadados de sincroniza��o
                    metaMap["lastModified"] = FieldValue.serverTimestamp()
                    metaMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = meta.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_META_COLABORADOR)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$currentCompanyId/entidades/${COLLECTION_META_COLABORADOR}/items, document=$documentId")
                    
                    collectionRef
                        .document(documentId)
                        .set(metaMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += metaMap.toString().length.toLong()
                    Log.d(TAG, "? Meta colaborador enviada com sucesso: ${meta.tipoMeta} (ID: ${meta.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar meta colaborador ${meta.id} (${meta.tipoMeta}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push INCREMENTAL de meta colaborador conclu�do: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de meta colaborador: ${e.message}", e)
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
            Log.d(TAG, "?? Iniciando pull de meta colaborador...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_META_COLABORADOR)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullMetaColaboradorIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de meta colaborador falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de meta colaborador - usando m�todo COMPLETO")
            }
            
            pullMetaColaboradorComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de meta colaborador: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de meta colaborador - documentos recebidos: ${documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de meta colaborador conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de meta colaborador: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao executar query incremental para meta colaborador: ${e.message}")
                return null
            }
            Log.d(TAG, "?? Meta colaborador - incremental retornou ${documents.size} documentos (ap�s filtro de rota)")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de meta colaborador: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de meta colaborador: ${e.message}", e)
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
                    Log.e(TAG, "? Erro ao processar meta colaborador ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
    
    /**
     * Push Equipments: Envia equipamentos do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Equipments com sincroniza��o incremental
     * Nota: Equipment n�o tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    private suspend fun pushEquipments(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_EQUIPMENTS
        
        return try {
            Log.d(TAG, "?? Iniciando push de equipamentos...")
            // Nota: Equipment n�o tem campo de timestamp, ent�o sempre enviar todos
            // (geralmente s�o poucos registros, impacto baixo)
            val equipmentsLocais = appRepository.obterTodosEquipments().first()
            Log.d(TAG, "?? Total de equipamentos locais encontrados: ${equipmentsLocais.size}")
            
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
                    Log.d(TAG, "?? Processando equipamento: ID=${equipment.id}, Nome=${equipment.name}")
                    
                    val equipmentMap = entityToMap(equipment)
                    Log.d(TAG, "   Mapa criado com ${equipmentMap.size} campos")
                    
                    // ? CR�TICO: Adicionar roomId para compatibilidade com pull
                    equipmentMap["roomId"] = equipment.id
                    equipmentMap["id"] = equipment.id
                    
                    // Adicionar metadados de sincroniza��o
                    equipmentMap["lastModified"] = FieldValue.serverTimestamp()
                    equipmentMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val documentId = equipment.id.toString()
                    val collectionRef = getCollectionReference(firestore, COLLECTION_EQUIPMENTS)
                    Log.d(TAG, "   Enviando para Firestore: empresas/$currentCompanyId/entidades/${COLLECTION_EQUIPMENTS}/items, document=$documentId")
                    
                    collectionRef
                        .document(documentId)
                        .set(equipmentMap)
                        .await()
                    
                    syncCount++
                    bytesUploaded += equipmentMap.toString().length.toLong()
                    Log.d(TAG, "? Equipamento enviado com sucesso: ${equipment.name} (ID: ${equipment.id})")
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "? Erro ao enviar equipamento ${equipment.id} (${equipment.name}): ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Log.d(TAG, "? Push de equipamentos conclu�do: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Log.e(TAG, "? Erro no push de equipamentos: ${e.message}", e)
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
            Log.d(TAG, "?? Iniciando pull de equipamentos...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_EQUIPMENTS)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullEquipmentsIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Log.w(TAG, "?? Sincroniza��o incremental de equipamentos falhou, usando m�todo COMPLETO")
                }
            } else {
                Log.d(TAG, "?? Primeira sincroniza��o de equipamentos - usando m�todo COMPLETO")
            }
            
            pullEquipmentsComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull de equipamentos: ${e.message}", e)
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
            Log.d(TAG, "?? Pull COMPLETO de equipamentos - documentos recebidos: ${snapshot.documents.size}")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o" else null
            )
            
            Log.d(TAG, "? Pull de equipamentos conclu�do: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull completo de equipamentos: ${e.message}", e)
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
                Log.w(TAG, "?? Falha ao criar query incremental para equipamentos: ${e.message}")
                return null
            }
            
            val snapshot = incrementalQuery.get().await()
            val documents = snapshot.documents
            Log.d(TAG, "?? Equipamentos - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincroniza��o incremental" else null
            )
            
            Log.d(TAG, "? Pull INCREMENTAL de equipamentos: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Log.e(TAG, "? Erro no pull incremental de equipamentos: ${e.message}", e)
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
                    Log.e(TAG, "? Erro ao processar equipamento ${doc.id}: ${e.message}", e)
            ProcessResult.Error
        }
    }
}

/**
 * Opera��o de sincroniza��o enfileirada.
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
 * Tipos de opera��o de sincroniza��o.
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
 * Classe utilit�ria para rastrear o progresso da sincroniza��o.
 */
class ProgressTracker(
    private val totalSteps: Int,
    private val listener: ((SyncProgress) -> Unit)?
) {
    private var completedSteps = 0

    fun start() {
        listener?.invoke(SyncProgress(0, "Preparando sincroniza��o..."))
    }

    fun advance(message: String) {
        if (totalSteps == 0) return
        completedSteps++
        val percent = ((completedSteps.toDouble() / totalSteps) * 100).roundToInt().coerceIn(0, 100)
        listener?.invoke(SyncProgress(percent, message))
    }

    fun complete() {
        listener?.invoke(SyncProgress(100, "Sincroniza��o conclu�da"))
    }

    fun completeWithMessage(message: String) {
        listener?.invoke(SyncProgress(100, message))
    }
}


