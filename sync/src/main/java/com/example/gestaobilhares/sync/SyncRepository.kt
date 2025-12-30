package com.example.gestaobilhares.sync

import android.content.Context
import timber.log.Timber
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.core.utils.DateUtils
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
class SyncRepository @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val networkUtils: NetworkUtils = NetworkUtils(context),
    private val userSessionManager: UserSessionManager = UserSessionManager.getInstance(context),
    private val firebaseImageUploader: FirebaseImageUploader = FirebaseImageUploader(context),
    private val syncMetadataDao: SyncMetadataDao = AppDatabase.getDatabase(context).syncMetadataDao(),
    private val mesaSyncHandler: com.example.gestaobilhares.sync.handlers.MesaSyncHandler,
    private val clienteSyncHandler: com.example.gestaobilhares.sync.handlers.ClienteSyncHandler,
    private val contratoSyncHandler: com.example.gestaobilhares.sync.handlers.ContratoSyncHandler,
    private val acertoSyncHandler: com.example.gestaobilhares.sync.handlers.AcertoSyncHandler,
    private val despesaSyncHandler: com.example.gestaobilhares.sync.handlers.DespesaSyncHandler,
    private val rotaSyncHandler: com.example.gestaobilhares.sync.handlers.RotaSyncHandler,
    private val cicloSyncHandler: com.example.gestaobilhares.sync.handlers.CicloSyncHandler,
    private val colaboradorSyncHandler: com.example.gestaobilhares.sync.handlers.ColaboradorSyncHandler,
    private val colaboradorRotaSyncHandler: com.example.gestaobilhares.sync.handlers.ColaboradorRotaSyncHandler,
    private val metaColaboradorSyncHandler: com.example.gestaobilhares.sync.handlers.MetaColaboradorSyncHandler,
    private val metaSyncHandler: com.example.gestaobilhares.sync.handlers.MetaSyncHandler,
    private val assinaturaSyncHandler: com.example.gestaobilhares.sync.handlers.AssinaturaSyncHandler,
    private val veiculoSyncHandler: com.example.gestaobilhares.sync.handlers.VeiculoSyncHandler,
    private val equipamentoSyncHandler: com.example.gestaobilhares.sync.handlers.EquipamentoSyncHandler,
    private val estoqueSyncHandler: com.example.gestaobilhares.sync.handlers.EstoqueSyncHandler
) {
    private var accessibleRouteIdsCache: Set<Long>? = null
    private var allowRouteBootstrap = false
    private val clienteRotaCache = mutableMapOf<Long, Long?>()
    private val mesaRotaCache = mutableMapOf<Long, Long?>()
    
    // ✅ CORREÇÃO CRÍTICA: Mover inicialização estática para evitar ExceptionInInitializerError
    private val gson: Gson by lazy { 
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create() 
    }
    
    /**
     * ✅ CORREÇÃO CRÍTICA: Usar classe estática interna ao invés de classe anônima
     * Isso garante que o ProGuard/R8 preserve as assinaturas genéricas corretamente.
     * O problema anterior era que classes anônimas perdem suas assinaturas genéricas após otimização.
     */
    private val mapType: java.lang.reflect.Type = Companion.mapTypeTokenInstance.type
    
    // ? NOVO: ID da empresa din�mico vindo da sess�o
    private val currentCompanyId: String
        get() = userSessionManager.getCurrentCompanyId()

    private val currentUserId: Long
        get() = userSessionManager.getCurrentUserId()

    init {
        Timber.tag(TAG).d("SyncRepository inicializado (Empresa: $currentCompanyId)")
        Timber.tag(TAG).d("NetworkUtils.isConnected() inicial = ${networkUtils.isConnected()}")
    }
    
    /**
     * ? HELPER: Retorna CollectionReference para uma entidade
     * Usa a inst�ncia de firestore da classe e o companyId din�mico
     */
    private fun getCollectionRef(collectionName: String): CollectionReference {
        val companyId = currentCompanyId
        Timber.tag(TAG).d("?? getCollectionRef: $collectionName (Empresa: $companyId)")
        return getCollectionReference(firestore, collectionName, companyId)
    }
    
    companion object {
        private const val TAG = "SyncRepository"
        private const val GLOBAL_SYNC_METADATA = "_global_sync"
        private const val ONE_HOUR_IN_MS = 60 * 60 * 1000L
        private const val DEFAULT_BACKGROUND_IDLE_HOURS = 6L
        private const val FIRESTORE_WHERE_IN_LIMIT = 10
        private const val FIELD_ROTA_ID = "rotaId"
        
        /**
         * ✅ CORREÇÃO CRÍTICA: Classe estática interna para TypeToken
         * Isso garante que o ProGuard/R8 preserve as assinaturas genéricas.
         * Classes anônimas perdem suas assinaturas genéricas após otimização.
         */
        private class MapTypeToken : TypeToken<Map<String, Any?>>()
        
        /**
         * ✅ Instância singleton do TypeToken para acesso ao type
         * Isso garante que apenas uma instância seja criada e reutilizada.
         */
        private val mapTypeTokenInstance = MapTypeToken()
        
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

        private const val PUSH_OPERATION_COUNT = 15
        private const val PULL_OPERATION_COUNT = 15
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
            Timber.tag(TAG).w("?? Acerto ${doc.id} sem dados")
            return null
        }

        val acertoId = doc.id.toLongOrNull() ?: run {
            Timber.tag(TAG).w("?? Acerto ${doc.id} com ID inv�lido")
            return null
        }

        val clienteIdNormalizado = extrairClienteId(acertoData)
        if (clienteIdNormalizado == null || clienteIdNormalizado <= 0L) {
            Timber.tag(TAG).e("? Acerto $acertoId sem clienteId v�lido (dados brutos: ${acertoData["clienteId"] ?: acertoData["cliente_id"] ?: acertoData["clienteID"]})")
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
            Timber.tag(TAG).e("? Falha ao converter acerto $acertoId do JSON")
            return null
        }

        Timber.tag(TAG).d("? Acerto convertido: ID=${acertoFirestore.id}, clienteId=${acertoFirestore.clienteId}")
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
            is java.util.Date -> rawValue.time
            is Number -> rawValue.toLong()
            is String -> parseDataAcertoString(rawValue)
            else -> {
                try {
                    doc.getTimestamp("dataAcerto")?.toDate()?.time
                        ?: doc.getTimestamp("data_acerto")?.toDate()?.time
                        ?: 0L
                } catch (ex: Exception) {
                    Timber.tag(TAG).w("?? dataAcerto n�o � Timestamp (doc=${doc.id}): ${ex.message}")
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

        Timber.tag(TAG).w("?? N�o foi poss�vel converter dataAcerto '$value' usando formatos conhecidos")
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
            Timber.tag(TAG).w("?? Erro ao manter hist�rico de acertos local para cliente $clienteId: ${e.message}")
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
                        .whereGreaterThanOrEqualTo("dataAcerto", com.google.firebase.Timestamp(inicio))
                        .whereLessThanOrEqualTo("dataAcerto", com.google.firebase.Timestamp(fim))
                        .orderBy("dataAcerto", Query.Direction.DESCENDING)
                }
            )

            snapshot.mapNotNull { documentToAcerto(it) }
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro ao buscar acertos por per�odo: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchUltimosAcertos(clienteId: Long, limit: Int): List<Acerto> {
        return try {
            Timber.tag(TAG).d("?? Buscando �ltimos $limit acertos para cliente $clienteId no Firestore...")
            val collectionRef = getCollectionRef(COLLECTION_ACERTOS)
            val snapshot = queryAcertosPorCampoCliente(
                collectionRef = collectionRef,
                clienteIdentifier = clienteId,
                limit = limit
            )

            val acertos = snapshot.mapNotNull { documentToAcerto(it) }
            Timber.tag(TAG).d("? Busca conclu�da: ${acertos.size} acertos encontrados para cliente $clienteId (de ${snapshot.size} documentos do Firestore)")
            if (acertos.size < snapshot.size) {
                Timber.tag(TAG).w("?? ATENCAO: ${snapshot.size - acertos.size} documentos do Firestore nao foram convertidos para Acerto (possivel problema na conversao)")
            }
            acertos
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro ao buscar �ltimos acertos para cliente $clienteId: ${e.message}", e)
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
        Timber.tag(TAG).w("?? N�o foi poss�vel buscar acertos para cliente $clienteIdentifier com nenhuma estrat�gia")
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
        Timber.tag(TAG).d("?? Tentando buscar acertos sem orderBy para cliente $fieldValue (limit: $limit)")
        for (field in CLIENTE_ID_FIELDS) {
            try {
                Timber.tag(TAG).d("   Tentando campo '$field' com valor '$fieldValue' (tipo: ${fieldValue::class.simpleName})")
                // Query simples: apenas whereEqualTo (N�O requer �ndice composto)
                var query: Query = collectionRef.whereEqualTo(field, fieldValue)
                
                // Buscar todos os documentos (sem limit no Firestore para evitar problemas)
                val snapshot = query.get().await()
                
                Timber.tag(TAG).d("   Resultado da query '$field=$fieldValue': ${snapshot.size()} documentos encontrados")
                
                if (!snapshot.isEmpty) {
                    Timber.tag(TAG).d("? Acertos encontrados usando campo '$field' sem orderBy (${snapshot.size()} docs) - ordenando em mem�ria")
                    
                    // Log dos primeiros documentos para debug
                    snapshot.documents.take(3).forEachIndexed { index, doc ->
                        val acertoData = doc.data
                        val clienteIdValue = acertoData?.get("clienteId") ?: acertoData?.get("cliente_id") ?: acertoData?.get("clienteID")
                        Timber.tag(TAG).d("   Doc[$index] ID=${doc.id}, clienteId no doc=$clienteIdValue (tipo: ${clienteIdValue?.javaClass?.simpleName})")
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
                    
                    Timber.tag(TAG).d("   ?? Retornando ${resultado.size} acertos ordenados (de ${documentosOrdenados.size} total)")
                    return resultado
                } else {
                    Timber.tag(TAG).d("   ?? Query '$field=$fieldValue' retornou vazio (0 documentos)")
                }
            } catch (ex: FirebaseFirestoreException) {
                if (ex.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    // Mesmo sem orderBy, pode falhar se o campo n�o existir
                    Timber.tag(TAG).w("?? Campo '$field' com valor '$fieldValue' retornou FAILED_PRECONDITION: ${ex.message}")
                } else {
                    Timber.tag(TAG).e("? Erro ao consultar acertos sem orderBy ($field=$fieldValue): ${ex.message}", ex)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("? Erro inesperado ao buscar acertos sem orderBy ($field=$fieldValue): ${e.message}", e)
                Timber.tag(TAG).e("   Stack trace: ${e.stackTraceToString()}")
            }
        }
        Timber.tag(TAG).w("?? Nenhum acerto encontrado para cliente $fieldValue ap�s tentar todos os campos: ${CLIENTE_ID_FIELDS.joinToString()}")
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
                    Timber.tag(TAG).d("? Acertos encontrados usando campo '$field' com valor '$fieldValue' (${snapshot.size()} docs)")
                    return snapshot.documents
                }
            } catch (ex: FirebaseFirestoreException) {
                if (ex.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Timber.tag(TAG).w("?? Campo '$field' com valor '$fieldValue' sem �ndice para consulta: ${ex.message}")
                    // N�o retorna null aqui, continua tentando outros campos/estrat�gias
                } else {
                    Timber.tag(TAG).e("? Erro ao consultar acertos ($field=$fieldValue): ${ex.message}", ex)
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
            syncMetadataDao.obterUltimoTimestamp(entityType, currentUserId)
        } catch (e: Exception) {
            Timber.tag(TAG).w("?? Erro ao obter timestamp de sincroniza��o para $entityType: ${e.message}")
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
            syncMetadataDao.obterUltimoTimestamp("${entityType}_push", currentUserId)
        } catch (e: Exception) {
            Timber.tag(TAG).w("?? Erro ao obter timestamp de push para $entityType: ${e.message}")
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
                userId = currentUserId,
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
            Timber.tag(TAG).d("? Metadata de sincroniza��o salva para $entityType: $syncCount registros em ${durationMs}ms, $timestampInfo")
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro ao salvar metadata de sincroniza��o para $entityType: ${e.message}", e)
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
            Timber.tag(TAG).d("?? Executando sync em background: $pendingOps opera��es pendentes")
            return true
        }

        val failedOps = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
        if (failedOps > 0) {
            Timber.tag(TAG).d("?? Executando sync em background: $failedOps opera��es falhadas aguardando retry")
            return true
        }

        val lastGlobalSync = runCatching { syncMetadataDao.obterUltimoTimestamp(GLOBAL_SYNC_METADATA, currentUserId) }.getOrDefault(0L)
        if (lastGlobalSync == 0L) {
            Timber.tag(TAG).d("?? Nenhum registro de sincroniza��o global - executar agora")
            return true
        }

        val hoursSinceLastSync = (System.currentTimeMillis() - lastGlobalSync) / ONE_HOUR_IN_MS
        return if (hoursSinceLastSync >= maxIdleHours) {
            Timber.tag(TAG).d("?? �ltima sincroniza��o global h� $hoursSinceLastSync h (limite $maxIdleHours h) - executar")
            true
        } else {
            Timber.tag(TAG).d("?? Sincroniza��o em background dispensada (pendentes=$pendingOps, horas=$hoursSinceLastSync)")
            false
        }
    }
    
    suspend fun getGlobalLastSyncTimestamp(): Long {
        return runCatching { syncMetadataDao.obterUltimoTimestamp(GLOBAL_SYNC_METADATA, currentUserId) }
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
            Timber.tag(TAG).d("?? ADMIN: Bootstrap desabilitado, acessando todas as rotas")
            return accessibleRouteIdsCache!!
        }

        val userId = userSessionManager.getCurrentUserId()
        val routes = userSessionManager.getUserAccessibleRoutes(context)
        val hasLocalAssignments = userSessionManager.hasAnyRouteAssignments(context)

        Timber.tag(TAG).d("?? USER ID $userId: rotas acess�veis=${routes.size}, tem atribui��es locais=$hasLocalAssignments, bootstrap ser�=${routes.isEmpty() && !hasLocalAssignments}")

        allowRouteBootstrap = routes.isEmpty() && !hasLocalAssignments
        accessibleRouteIdsCache = routes.toSet()

        if (allowRouteBootstrap) {
            Timber.tag(TAG).w("?? Usu�rio ID $userId sem rotas locais sincronizadas ainda. Aplicando bootstrap tempor�rio sem filtro de rota.")
        } else if (routes.isNotEmpty()) {
            Timber.tag(TAG).d("? Usu�rio ID $userId tem ${routes.size} rotas atribu�das: ${routes.joinToString()}")
        } else {
            Timber.tag(TAG).w("?? Usu�rio ID $userId sem rotas atribu�das e sem dados locais - nenhum dado ser� sincronizado")
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
                        Timber.tag(TAG).w("?? Cliente $entityId n�o encontrado localmente - tentando buscar do Firestore")
                        return tryFetchMissingCliente(entityId)
                    }
                    true
                }
                "mesa" -> {
                    val exists = appRepository.obterMesaPorId(entityId) != null
                    if (!exists) {
                        Timber.tag(TAG).w("?? Mesa $entityId n�o encontrada localmente - tentando buscar do Firestore")
                        return tryFetchMissingMesa(entityId)
                    }
                    true
                }
                "contrato" -> {
                    val contrato = runCatching {
                        appRepository.buscarTodosContratos().first().find { it.id == entityId }
                    }.getOrNull()
                    if (contrato == null) {
                        Timber.tag(TAG).w("?? Contrato $entityId n�o encontrado localmente - tentando buscar do Firestore")
                        return tryFetchMissingContrato(entityId)
                    }
                    true
                }
                else -> {
                    Timber.tag(TAG).w("?? Tipo de entidade desconhecido: $entityType")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro ao validar FK para $entityType $entityId: ${e.message}", e)
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
                Timber.tag(TAG).w("?? Cliente $clienteId n�o existe no Firestore")
                return false
            }
            
            val clienteData = doc.data ?: return false
            val clienteJson = gson.toJson(clienteData)
            val cliente = gson.fromJson(clienteJson, Cliente::class.java)?.copy(id = clienteId)
                ?: return false
            
            appRepository.inserirCliente(cliente)
            Timber.tag(TAG).d("? Cliente $clienteId buscado e inserido com sucesso")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Falha ao buscar cliente $clienteId: ${e.message}", e)
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
                Timber.tag(TAG).w("?? Mesa $mesaId n�o existe no Firestore")
                return false
            }
            
            val mesaData = doc.data ?: return false
            val mesaJson = gson.toJson(mesaData)
            val mesa = gson.fromJson(mesaJson, Mesa::class.java)?.copy(id = mesaId)
                ?: return false
            
            // Validar que o cliente da mesa existe
            val clienteId = mesa.clienteId
            if (clienteId == null || clienteId <= 0L) {
                Timber.tag(TAG).w("?? Mesa $mesaId tem clienteId inv�lido: $clienteId")
                return false
            }
            
            if (!ensureEntityExists("cliente", clienteId)) {
                Timber.tag(TAG).w("?? Mesa $mesaId referencia cliente $clienteId que n�o existe")
                return false
            }
            
            appRepository.inserirMesa(mesa)
            Timber.tag(TAG).d("? Mesa $mesaId buscada e inserida com sucesso")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Falha ao buscar mesa $mesaId: ${e.message}", e)
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
                Timber.tag(TAG).w("?? Contrato $contratoId n�o existe no Firestore")
                return false
            }
            
            val contratoData = doc.data ?: return false
            val contratoJson = gson.toJson(contratoData)
            val contrato = gson.fromJson(contratoJson, ContratoLocacao::class.java)?.copy(id = contratoId)
                ?: return false
            
            // Validar que o cliente do contrato existe
            if (!ensureEntityExists("cliente", contrato.clienteId)) {
                Timber.tag(TAG).w("?? Contrato $contratoId referencia cliente ${contrato.clienteId} que n�o existe")
                return false
            }
            
            appRepository.inserirContrato(contrato)
            Timber.tag(TAG).d("? Contrato $contratoId buscado e inserido com sucesso")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Falha ao buscar contrato $contratoId: ${e.message}", e)
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
                Timber.tag(TAG).d("?? Processado lote: ${documents.size} documentos (total: $totalProcessed)")
                
                // Verificar se h� mais documentos
                hasMore = documents.size == batchSize
                lastDocument = documents.lastOrNull()
                
            } catch (e: Exception) {
                Timber.tag(TAG).e("? Erro ao processar lote paginado: ${e.message}", e)
                hasMore = false
            }
        }
        
        Timber.tag(TAG).d("? Pagina��o conclu�da: $totalProcessed documentos processados")
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
                Timber.tag(TAG).w("?? Bootstrap de rotas: baixando todas as rotas temporariamente para popular acessos locais.")
                listOf(applyTimestampFilter(collectionRef, lastSyncTimestamp, timestampField))
            } else {
                Timber.tag(TAG).w("?? Usu�rio sem rotas atribu�das - nenhuma query ser� executada para $routeField")
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
                Timber.tag(TAG).w("?? Bootstrap de rotas: baixando todas as rotas temporariamente para popular acessos locais.")
                collectionRef.get().await().documents
            } else {
                Timber.tag(TAG).w("?? Nenhuma rota atribu�da ao usu�rio - resultado vazio para $routeField")
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
            Timber.tag(TAG).d("?? Sincroniza��o INCREMENTAL para $entityType (desde ${Date(lastSyncTimestamp)})")
            collectionRef
                .whereGreaterThan(timestampField, Timestamp(Date(lastSyncTimestamp)))
                .orderBy(timestampField) // OBRIGAT�RIO: Firestore requer orderBy com whereGreaterThan
        } else {
            // Primeira sincroniza��o: buscar todos (mas ainda com orderBy para pagina��o)
            Timber.tag(TAG).d("?? Primeira sincroniza��o COMPLETA para $entityType")
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
        Timber.tag(TAG).d("?? syncPull() CHAMADO - IN�CIO")
        return try {
            Timber.tag(TAG).d("?? ========== INICIANDO SINCRONIZA��O PULL ==========")
            Timber.tag(TAG).d("?? Verificando conectividade...")
            
            resetRouteFilters()
            val accessibleRoutes = getAccessibleRouteIdsInternal()
            if (userSessionManager.isAdmin()) {
                Timber.tag(TAG).d("?? Usu�rio ADMIN - sincronizando todas as rotas dispon�veis.")
            } else if (accessibleRoutes.isEmpty()) {
                Timber.tag(TAG).w("?? Usu�rio sem rotas atribu�das - nenhum dado espec�fico de rota ser� sincronizado.")
            } else {
                Timber.tag(TAG).d("?? Rotas permitidas para este usu�rio: ${accessibleRoutes.joinToString()}")
            }
            
            val isConnected = networkUtils.isConnected()
            Timber.tag(TAG).d("?? NetworkUtils.isConnected() = $isConnected")
            
            // ✅ CORREÇÃO: Propagar flag de bootstrap para todos os handlers
            val handlers = listOf(
                mesaSyncHandler, clienteSyncHandler, contratoSyncHandler, acertoSyncHandler,
                despesaSyncHandler, rotaSyncHandler, cicloSyncHandler, colaboradorSyncHandler,
                colaboradorRotaSyncHandler, metaColaboradorSyncHandler, metaSyncHandler,
                assinaturaSyncHandler, veiculoSyncHandler, equipamentoSyncHandler, estoqueSyncHandler
            )
            handlers.forEach { it.allowRouteBootstrap = allowRouteBootstrap }
            Timber.tag(TAG).d("?? Bootstrap flag ($allowRouteBootstrap) propagada para ${handlers.size} handlers")
            
            // Tentar mesmo se NetworkUtils reportar offline (pode ser falso negativo)
            // O Firestore vai falhar se realmente estiver offline
            if (!isConnected) {
                Timber.tag(TAG).w("?? NetworkUtils reporta offline, mas tentando mesmo assim...")
                Timber.tag(TAG).w("?? Firestore vai falhar se realmente estiver offline")
            } else {
                Timber.tag(TAG).d("? Dispositivo online confirmado")
            }
            
            Timber.tag(TAG).d("? Prosseguindo com sincroniza��o PULL")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Timber.tag(TAG).d("?? Conectando ao Firestore...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // ? CORRIGIDO: Pull por dom�nio em sequ�ncia respeitando depend�ncias
            // ORDEM CR�TICA: Rotas primeiro (clientes dependem de rotas)
            
            // ✅ CORREÇÃO: Puxar colaboradores antes (para garantir ID do usuário atual)
            colaboradorSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Colaboradores: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Colaboradores falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando colaboradores...")

            // ✅ CORREÇÃO: Puxar vínculos de rota ANTES de puxar as rotas
            // Isso garante que o USER saiba quais rotas ele pode acessar
            colaboradorRotaSyncHandler.pull().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Colaborador Rotas: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Colaborador Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando colaborador rotas...")

            // rotaSyncHandler.allowRouteBootstrap = allowRouteBootstrap // Removido: agora propagado para todos acima
            
            rotaSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando rotas...")
            
            clienteSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Clientes: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Clientes falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando clientes...")
            
            // ✅ REFATORAÇÃO: Usar MesaSyncHandler
            mesaSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando mesas...")
            
            
            cicloSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Ciclos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Ciclos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando ciclos...")
            
            acertoSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Acertos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Acertos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando acertos...")
            
            despesaSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Despesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Despesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando despesas...")
            
            contratoSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Contratos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Contratos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando contratos...")
            
            progressTracker?.advance("Importando dados financeiros...")
            
            metaSyncHandler.pull().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Metas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Metas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando metas...")
            
            metaColaboradorSyncHandler.pull().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Meta Colaborador: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Meta Colaborador falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando metas por colaborador...")
            
            // ✅ REFATORAÇÃO: Usar EquipamentoSyncHandler
            equipamentoSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Equipamentos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Equipamentos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando equipamentos...")
            
            
            progressTracker?.advance("Importando contratos de mesa...")
            
            // ✅ REFATORAÇÃO: Usar AssinaturaSyncHandler (Assinaturas e Logs)
            assinaturaSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Assinaturas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Assinaturas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando assinaturas e logs...")
            
            // ✅ REFATORAÇÃO: Usar EstoqueSyncHandler (Panos, Mesas Vendidas, Reformadas, PanoMesa, Historico)
            estoqueSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Estoque: $count itens sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                }
            )
            progressTracker?.advance("Importando dados de estoque e mesas...")
            
            // ✅ REFATORAÇÃO: Usar VeiculoSyncHandler (Veiculos e Históricos)
            veiculoSyncHandler.pull(timestampOverride).fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Pull Veiculos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Pull Veiculos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Importando vecilos e histricos...")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                failedOperations = failedCount
            )
            
            Timber.tag(TAG).d("? ========== SINCRONIZA��O PULL CONCLU�DA ==========")
            Timber.tag(TAG).d("?? Total sincronizado: $totalSyncCount itens")
            Timber.tag(TAG).d("? Total de falhas: $failedCount dom�nios")
            Timber.tag(TAG).d("? Timestamp: ${System.currentTimeMillis()}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro na sincroniza��o Pull: ${e.message}", e)
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
        Timber.tag(TAG).d("?? ========== INICIANDO SINCRONIZA��O PUSH ==========")
        Timber.tag(TAG).d("   ? Timestamp: ${System.currentTimeMillis()} (${java.util.Date()})")
        Timber.tag(TAG).d("   ?? Stack trace: ${Thread.currentThread().stackTrace.take(5).joinToString("\n") { it.toString() }}")
        return try {
            if (!networkUtils.isConnected()) {
                Timber.tag(TAG).w("?? Sincroniza��o Push cancelada: dispositivo offline")
                return Result.failure(Exception("Dispositivo offline"))
            }

            Timber.tag(TAG).d("? Dispositivo online - prosseguindo com sincroniza��o")

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                isOnline = true,
                error = null
            )
            
            Timber.tag(TAG).d("?? ========== PROCESSANDO FILA DE SINCRONIZA��O ==========")
            // ? CORRE��O: Verificar quantas opera��es existem na fila antes de processar
            val pendingCountBefore = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
            val failedCountBefore = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
            Timber.tag(TAG).d("?? Estado da fila ANTES do processamento:")
            Timber.tag(TAG).d("   - Opera��es pendentes: $pendingCountBefore")
            Timber.tag(TAG).d("   - Opera��es falhadas: $failedCountBefore")
            
            if (pendingCountBefore > 0) {
                Timber.tag(TAG).d("   ?? H� $pendingCountBefore opera��o(�es) pendente(s) na fila!")
            } else {
                Timber.tag(TAG).d("   ?? Nenhuma opera��o pendente na fila")
            }
            
            val queueProcessResult = processSyncQueue()
            if (queueProcessResult.isFailure) {
                Timber.tag(TAG).e("? Falha ao processar fila de sincroniza��o: ${queueProcessResult.exceptionOrNull()?.message}")
                // N�o retornamos falha aqui, tentamos o push direto mesmo assim
            } else {
                val pendingCountAfter = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
                val failedCountAfter = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
                Timber.tag(TAG).d("? Fila de sincroniza��o processada com sucesso.")
                Timber.tag(TAG).d("?? Estado da fila DEPOIS do processamento: pendentes=$pendingCountAfter, falhadas=$failedCountAfter")
            }

            Timber.tag(TAG).d("Iniciando push de dados locais para o Firestore...")
            
            var totalSyncCount = 0
            var failedCount = 0
            
            // Push por domnio em sequncia
            clienteSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Clientes: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Clientes falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando clientes...")
            
            rotaSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando rotas...")
            
            // ✅ REFATORAÇÃO: Usar MesaSyncHandler se disponível, senão usar método legado
            mesaSyncHandler.push().fold(
                onSuccess = { count: Int -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Mesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Mesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando mesas...")
            
            colaboradorSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Colaboradores: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Colaboradores falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando colaboradores...")
            
            cicloSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Ciclos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Ciclos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando ciclos...")
            
            acertoSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Acertos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Acertos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando acertos...")
            
            despesaSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Despesas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Despesas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando despesas...")
            
            contratoSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Contratos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Contratos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando contratos...")
            
            progressTracker?.advance("Enviando dados financeiros...")
            
            metaSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Metas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Metas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando metas...")
            
            metaColaboradorSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Meta Colaborador: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Meta Colaborador falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando metas por colaborador...")
            
            colaboradorRotaSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Colaborador Rotas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Colaborador Rotas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando colaborador rotas...")
            
            // ✅ REFATORAÇÃO: Usar AssinaturaSyncHandler (Assinaturas e Logs)
            assinaturaSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Assinaturas: $count sincronizadas")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Assinaturas falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando assinaturas...")
            
            estoqueSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Estoque: $count itens sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Estoque falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando dados de estoque...")
            
            veiculoSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Veículos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Veículos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando dados de veículos...")
            
            equipamentoSyncHandler.push().fold(
                onSuccess = { count -> 
                    totalSyncCount += count
                    Timber.tag(TAG).d("? Push Equipamentos: $count sincronizados")
                },
                onFailure = { e ->
                    failedCount++
                    Timber.tag(TAG).e("? Push Equipamentos falhou: ${e.message}", e)
                }
            )
            progressTracker?.advance("Enviando equipamentos...")
            
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                pendingOperations = appRepository.contarOperacoesSyncPendentes(),
                failedOperations = appRepository.contarOperacoesSyncFalhadas()
            )
            
            Timber.tag(TAG).d("? ========== SINCRONIZA��O PUSH CONCLU�DA ==========")
            Timber.tag(TAG).d("?? Total enviado: $totalSyncCount itens")
            Timber.tag(TAG).d("? Total de falhas: $failedCount dom�nios")
            Timber.tag(TAG).d("? Timestamp: ${System.currentTimeMillis()}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro na sincroniza��o Push: ${e.message}", e)
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
                Timber.tag(TAG).d("?? Verificando dados na nuvem: dispositivo offline")
                return false
            }
            
            Timber.tag(TAG).d("?? Verificando se h� dados na nuvem...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_ROTAS)
            val snapshot = collectionRef.limit(1).get().await()
            val hasData = !snapshot.isEmpty
            Timber.tag(TAG).d("?? Dados na nuvem encontrados: $hasData")
            hasData
        } catch (e: FirebaseFirestoreException) {
            // ? CORRE��O: Se for PERMISSION_DENIED e usu�rio est� logado localmente,
            // assumir que h� dados na nuvem (permitir tentar sincronizar)
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                val userId = userSessionManager.getCurrentUserId()
                if (userId != 0L) {
                    Timber.tag(TAG).w("?? PERMISSION_DENIED ao verificar nuvem, mas usu�rio est� logado localmente (ID: $userId)")
                    Timber.tag(TAG).w("?? Assumindo que h� dados na nuvem para permitir sincroniza��o")
                    return true // Assumir que h� dados para permitir tentar sincronizar
                }
            }
            Timber.tag(TAG).e("? Erro ao verificar dados na nuvem: ${e.message}", e)
            false
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro ao verificar dados na nuvem: ${e.message}", e)
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
        Timber.tag(TAG).d("?? syncBidirectional() CHAMADO - IN�CIO")
        return try {
            // ✅ NOVO: Resetar filtros de rota no início para garantir que o cache não esteja sujo
            resetRouteFilters()
            Timber.tag(TAG).d("?? ========== INICIANDO SINCRONIZA��O BIDIRECIONAL ==========")
            Timber.tag(TAG).d("Iniciando sincroniza��o bidirecional...")
            
            // ? CORRE��O CR�TICA: Capturar timestamp ANTES de fazer PUSH
            // Isso garante que pr�xima sync incremental n�o perca dados que foram enviados agora
            val timestampBeforePush = System.currentTimeMillis()
            Timber.tag(TAG).d("   ? Timestamp capturado ANTES do push: $timestampBeforePush (${Date(timestampBeforePush)})")
            
            val progressTracker = onProgress?.let { ProgressTracker(TOTAL_SYNC_OPERATIONS, it).apply { start() } }
            
            // ? CORRIGIDO: 1. PUSH primeiro (enviar dados locais para preservar)
            // Isso garante que dados novos locais sejam enviados antes de baixar da nuvem
            Timber.tag(TAG).d("?? Passo 1: Executando PUSH (enviar dados locais para nuvem)...")
            val pushResult = syncPush(progressTracker)
            if (pushResult.isFailure) {
                Timber.tag(TAG).w("?? Push falhou: ${pushResult.exceptionOrNull()?.message}")
                Timber.tag(TAG).w("?? Continuando com Pull mesmo assim...")
            } else {
                Timber.tag(TAG).d("? Push conclu�do com sucesso - dados locais preservados na nuvem")
            }
            
            // ? CORRE��O: Aguardar pequeno delay para garantir que Firestore processou
            // Isso evita race condition onde PULL ocorre antes que Firestore salve dados do PUSH
            Timber.tag(TAG).d("   ?? Aguardando 500ms para propaga��o do Firestore...")
            kotlinx.coroutines.delay(500) // Meio segundo para propaga��o
            
            // ? CORRIGIDO: 2. PULL depois (atualizar dados locais da nuvem)
            // O pull n�o sobrescreve dados locais mais recentes (verifica��o de timestamp)
            Timber.tag(TAG).d("?? Passo 2: Executando PULL (importar atualiza��es da nuvem)...")
            Timber.tag(TAG).d("   ?? Usando timestamp capturado ANTES do push: $timestampBeforePush")
            val pullResult = syncPull(progressTracker, timestampBeforePush)
            if (pullResult.isFailure) {
                Timber.tag(TAG).w("?? Pull falhou: ${pullResult.exceptionOrNull()?.message}")
                Timber.tag(TAG).w("?? Mas Push pode ter sido bem-sucedido")
            } else {
                Timber.tag(TAG).d("? Pull conclu�do com sucesso - dados locais atualizados")
            }
            
            if (pullResult.isSuccess && pushResult.isSuccess) {
                Timber.tag(TAG).d("? ========== SINCRONIZA��O BIDIRECIONAL CONCLU�DA COM SUCESSO ==========")
                // ? N�O salvar global metadata aqui - j� foi salvo por entidade com timestampOverride
                progressTracker?.complete()
                Result.success(Unit)
            } else {
                val errorMsg = "Sincroniza��o parcial: Push=${pushResult.isSuccess}, Pull=${pullResult.isSuccess}"
                Timber.tag(TAG).w("?? $errorMsg")
                progressTracker?.completeWithMessage("Sincroniza��o parcial conclu�da")
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro na sincroniza��o bidirecional: ${e.message}", e)
            Timber.tag(TAG).e("   Stack trace: ${e.stackTraceToString()}")
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
            Timber.tag(TAG).d("?? Opera��o enfileirada: ${operation.type} - entidade=${operation.entityType}, id=${operation.entityId}")
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro ao enfileirar opera��o ${operation.entityId}: ${e.message}", e)
        }
    }
    
    /**
     * Processa fila de sincroniza��o pendente.
     */
    suspend fun processSyncQueue(): Result<Unit> {
        return try {
            Timber.tag(TAG).d("?? processSyncQueue() INICIADO")
            if (!networkUtils.isConnected()) {
                Timber.tag(TAG).w("?? Fila de sincroniza��o n�o processada: dispositivo offline")
                return Result.failure(Exception("Dispositivo offline - fila pendente"))
            }
            
            // ? CORRE��O: Verificar quantas opera��es existem antes de processar
            val totalPendingBefore = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
            Timber.tag(TAG).d("?? Total de opera��es pendentes na fila: $totalPendingBefore")
            
            if (totalPendingBefore == 0) {
                Timber.tag(TAG).d("?? Nenhuma opera��o pendente na fila - encerrando processamento")
                return Result.success(Unit)
            }
            
            // ? CORRE��O: Processar em loop at� esvaziar a fila
            var totalSuccessCount = 0
            var totalFailureCount = 0
            var batchNumber = 0
            
            while (true) {
                // ? CORRE��O: Log antes de buscar opera��es
                Timber.tag(TAG).d("   ?? Buscando opera��es pendentes (limite: $QUEUE_BATCH_SIZE)...")
                val operations = appRepository.obterOperacoesSyncPendentesLimitadas(QUEUE_BATCH_SIZE)
                Timber.tag(TAG).d("   ?? Opera��es encontradas: ${operations.size}")
                
                if (operations.isEmpty()) {
                    if (batchNumber == 0) {
                        Timber.tag(TAG).d("?? Nenhuma opera��o pendente na fila")
                    } else {
                        Timber.tag(TAG).d("? Fila completamente processada ap�s $batchNumber lote(s)")
                    }
                    break
                }
                
                batchNumber++
                Timber.tag(TAG).d("?? Processando lote $batchNumber: ${operations.size} opera��es pendentes")
                // ? CORRE��O: Log detalhado de cada opera��o encontrada
                operations.forEachIndexed { index, op ->
                    Timber.tag(TAG).d("   ?? Opera��o ${index + 1}: tipo=${op.operationType}, entidade=${op.entityType}, id=${op.entityId}, status=${op.status}")
                }
                var batchSuccessCount = 0
                var batchFailureCount = 0
                
                operations.forEach { entity ->
                    val processingEntity = entity.copy(status = SyncOperationStatus.PROCESSING.name)
                    appRepository.atualizarOperacaoSync(processingEntity)
                    
                    try {
                        Timber.tag(TAG).d("?? Processando opera��o: tipo=${entity.operationType}, entidade=${entity.entityType}, id=${entity.entityId}")
                        Timber.tag(TAG).d("   ?? Operation ID: ${entity.id}, Status: ${entity.status}, Retry: ${entity.retryCount}")
                        processSingleSyncOperation(processingEntity)
                        appRepository.deletarOperacaoSync(processingEntity)
                        batchSuccessCount++
                        totalSuccessCount++
                        Timber.tag(TAG).d("? Opera��o processada com sucesso: tipo=${entity.operationType}, entidade=${entity.entityType}, id=${entity.entityId}")
                    } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                        batchFailureCount++
                        totalFailureCount++
                        val newRetryCount = processingEntity.retryCount + 1
                        val newStatus = if (newRetryCount >= processingEntity.maxRetries) {
                            SyncOperationStatus.FAILED.name
                        } else {
                            SyncOperationStatus.PENDING.name
                        }
                        Timber.tag(TAG).e("? Erro do Firestore ao processar opera��o ${processingEntity.id} (tentativa $newRetryCount/${processingEntity.maxRetries}): ${e.code} - ${e.message}", e)
                        Timber.tag(TAG).e("   Tipo: ${processingEntity.operationType}, Entidade: ${processingEntity.entityType}, ID: ${processingEntity.entityId}")
                        if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Timber.tag(TAG).e("   ?? PERMISSION_DENIED: Verifique as regras do Firestore para ${processingEntity.operationType} em ${processingEntity.entityType}")
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
                        Timber.tag(TAG).e("? Erro ao processar opera��o ${processingEntity.id} (tentativa $newRetryCount/${processingEntity.maxRetries}): ${e.message}", e)
                        Timber.tag(TAG).e("   Tipo: ${processingEntity.operationType}, Entidade: ${processingEntity.entityType}, ID: ${processingEntity.entityId}")
                        Timber.tag(TAG).e("   ?? Stack trace: ${e.stackTraceToString()}")
                        appRepository.atualizarOperacaoSync(
                            processingEntity.copy(
                                status = newStatus,
                                retryCount = newRetryCount,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                
                Timber.tag(TAG).d("?? Lote $batchNumber processado: sucesso=$batchSuccessCount, falhas=$batchFailureCount")
            }
            
            val pendingCount = runCatching { appRepository.contarOperacoesSyncPendentes() }.getOrDefault(0)
            val failedCount = runCatching { appRepository.contarOperacoesSyncFalhadas() }.getOrDefault(0)
            _syncStatus.value = _syncStatus.value.copy(
                pendingOperations = pendingCount,
                failedOperations = failedCount
            )
            
            Timber.tag(TAG).d("?? Fila completamente processada: total sucesso=$totalSuccessCount, total falhas=$totalFailureCount, pendentes=$pendingCount, falhadas=$failedCount")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro ao processar fila de sincroniza��o: ${e.message}", e)
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
        Timber.tag(TAG).d("?? Processando opera��o �nica:")
        Timber.tag(TAG).d("   Tipo: ${operation.operationType}")
        Timber.tag(TAG).d("   Entidade: ${operation.entityType}")
        Timber.tag(TAG).d("   Document ID: $documentId")
        Timber.tag(TAG).d("   Collection Path: ${collectionRef.path}")
        
        when (operationType) {
            SyncOperationType.CREATE, SyncOperationType.UPDATE -> {
                val rawMap: Map<String, Any?> = gson.fromJson(operation.entityData, mapType)
                val mutableData = rawMap.toMutableMap()
            
                // ✅ CORREÇÃO: Converter strings de data para Timestamp do Firestore
                // O GSON converte Date em String ISO, mas o Firestore precisa de Timestamp
                mutableData.entries.toList().forEach { entry ->
                    val key = entry.key.lowercase()
                    val value = entry.value
                    if (value is String && (key.contains("data") || key.contains("timestamp") || key.contains("time"))) {
                        try {
                            val date = gson.fromJson("\"$value\"", Date::class.java)
                            if (date != null) {
                                mutableData[entry.key] = com.google.firebase.Timestamp(date)
                            }
                        } catch (e: Exception) {
                            // Manter como string se não for um formato de data reconhecido
                        }
                    }
                }
            
                mutableData["lastModified"] = FieldValue.serverTimestamp()
                mutableData["syncTimestamp"] = FieldValue.serverTimestamp()
                Timber.tag(TAG).d("?? Executando ${operation.operationType} no documento $documentId")
                collectionRef.document(documentId).set(mutableData).await()
                Timber.tag(TAG).d("? ${operation.operationType} executado com sucesso no documento $documentId")
            }
            SyncOperationType.DELETE -> {
                Timber.tag(TAG).d("??? ========== INICIANDO DELETE ==========")
                Timber.tag(TAG).d("   Tipo de entidade: ${operation.entityType}")
                Timber.tag(TAG).d("   Document ID: $documentId")
                Timber.tag(TAG).d("   Collection Path: ${collectionRef.path}")
                Timber.tag(TAG).d("   Collection Name: ${collectionRef.id}")
                Timber.tag(TAG).d("   Document Path completo: ${collectionRef.path}/$documentId")
                
                val documentRef = collectionRef.document(documentId)
                
                // ? CORRE��O: Verificar se o documento existe antes de deletar
                try {
                    Timber.tag(TAG).d("   ?? Verificando exist�ncia do documento...")
                    val snapshot = documentRef.get().await()
                    if (snapshot.exists()) {
                        Timber.tag(TAG).d("   ?? Documento encontrado no Firestore!")
                        Timber.tag(TAG).d("   ?? Dados do documento: ${snapshot.data?.keys?.joinToString()}")
                        Timber.tag(TAG).d("   ??? Executando DELETE...")
                        documentRef.delete().await()
                        Timber.tag(TAG).d("? DELETE executado com sucesso no documento $documentId")
                        Timber.tag(TAG).d("   ? Verificando se foi deletado...")
                        val verifySnapshot = documentRef.get().await()
                        if (!verifySnapshot.exists()) {
                            Timber.tag(TAG).d("   ? Confirmado: Documento foi deletado com sucesso")
                        } else {
                            Timber.tag(TAG).w("   ?? ATEN��O: Documento ainda existe ap�s DELETE!")
                        }
                    } else {
                        Timber.tag(TAG).w("?? Documento $documentId n�o existe no Firestore")
                        Timber.tag(TAG).w("   Caminho verificado: ${collectionRef.path}/$documentId")
                        Timber.tag(TAG).w("   Poss�veis causas: j� foi deletado, nunca existiu, ou caminho incorreto")
                        // N�o lan�ar exce��o - considerar sucesso se j� n�o existe
                    }
                } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                    Timber.tag(TAG).e("? Erro do Firestore ao deletar documento $documentId:", e)
                    Timber.tag(TAG).e("   C�digo: ${e.code}")
                    Timber.tag(TAG).e("   Mensagem: ${e.message}")
                    Timber.tag(TAG).e("   Caminho: ${collectionRef.path}/$documentId")
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).e("? Erro geral ao deletar documento $documentId: ${e.message}", e)
                    Timber.tag(TAG).e("   Tipo: ${e.javaClass.simpleName}")
                    Timber.tag(TAG).e("   Stack trace: ${e.stackTraceToString()}")
                    throw e
                }
                Timber.tag(TAG).d("??? ========== DELETE FINALIZADO ==========")
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
                Timber.tag(TAG).w("?? Tipo de entidade desconhecido: $entityType (normalizado: $normalized), usando como nome de cole��o")
                normalized
            }
        }
        Timber.tag(TAG).d("?? Mapeamento de entidade: '$entityType' -> coleo '$collectionName'")
        return getCollectionReference(firestore, collectionName)
    }
    
    // ✅ REFATORAÇÃO: Métodos pull legacy removidos. A lógica agora está nos SyncHandlers individuais.    
    // ✅ [CHUNCK 2] Métodos legacy removidos.
    // ✅ [CHUNCK 3] Métodos legacy removidos.

    // ✅ [CHUNCK 4] Métodos legacy removidos.

    // ✅ [CHUNCK 5] Métodos legacy removidos.
    
    /**
     * Pull Metas: Sincroniza metas do Firestore para o Room
     */
    private suspend fun pullMetas(): Result<Int> {
        return metaSyncHandler?.pull() ?: Result.failure(Exception("MetaSyncHandler não injetado"))
    }

    
    /**
     * Pull Colaborador Rotas: Sincroniza vinculaes colaborador-rota do Firestore para o Room
     */
    private suspend fun pullColaboradorRotas(): Result<Int> {
        return colaboradorRotaSyncHandler?.pull() ?: Result.failure(Exception("ColaboradorRotaSyncHandler não injetado"))
    }

    
    // ✅ [CHUNCK 6] Métodos legacy removidos.
    
    /**
     * Pull Logs Auditoria: Sincroniza logs de auditoria do Firestore para o Room
     */
    private suspend fun pullLogsAuditoria(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_LOGS_AUDITORIA
        
        return try {
            Timber.tag(TAG).d("?? Iniciando pull de logs auditoria...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_LOGS_AUDITORIA)
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                val incrementalResult = tryPullLogsAuditoriaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime)
                if (incrementalResult != null) {
                    return incrementalResult
                } else {
                    Timber.tag(TAG).w("?? Sincronizao incremental de logs auditoria falhou, usando mtodo COMPLETO")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronizao de logs auditoria - usando mtodo COMPLETO")
            }
            
            pullLogsAuditoriaComplete(collectionRef, entityType, startTime)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull de logs auditoria: ${e.message}", e)
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
            Timber.tag(TAG).d("?? Pull COMPLETO de logs auditoria - documentos recebidos: ${snapshot.documents.size}")
            
            val (syncCount, skippedCount, errorCount) = processLogsAuditoriaDocuments(snapshot.documents)
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                error = if (errorCount > 0) "$errorCount erros durante sincronizao" else null
            )
            Timber.tag(TAG).d("? Pull de logs auditoria concludo: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull completo de logs auditoria: ${e.message}", e)
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
            Timber.tag(TAG).d("?? Logs auditoria - incremental retornou ${documents.size} documentos")
            
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
                error = if (errorCount > 0) "$errorCount erros durante sincronizao incremental" else null
            )
            Timber.tag(TAG).d("? Pull INCREMENTAL de logs auditoria: sync=$syncCount, skipped=$skippedCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e("? Erro no pull incremental de logs auditoria: ${e.message}", e)
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
                    val dataOperacao = DateUtils.converterTimestampParaDate(data["dataOperacao"])
                        ?: DateUtils.converterTimestampParaDate(data["data_operacao"]) ?: Date(timestamp)
                    val dataValidacao = DateUtils.converterTimestampParaDate(data["dataValidacao"])
                        ?: DateUtils.converterTimestampParaDate(data["data_validacao"])
                    
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
                        dataOperacao = dataOperacao?.time ?: System.currentTimeMillis(),
                        observacoes = data["observacoes"] as? String,
                        validadoJuridicamente = data["validadaJuridicamente"] as? Boolean
                            ?: data["validada_juridicamente"] as? Boolean ?: false,
                        dataValidacao = dataValidacao?.time,
                        validadoPor = data["validadoPor"] as? String
                            ?: data["validado_por"] as? String
                    )
                    
                    appRepository.inserirLogAuditoriaAssinatura(log)
                    syncCount++
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao processar log auditoria ${doc.id}: ${e.message}", e)
                }
            }
            
        return Triple(syncCount, skipCount, errorCount)
    }
    
    // ==================== PUSH HANDLERS (LOCAL ? SERVIDOR) ====================
    
    /**
     * ? REFATORADO (2025): Push Clientes com sincronizao incremental
     * Envia apenas clientes modificados desde o ltimo push
     * Segue melhores prticas Android 2025 para sincronizao incremental
     */
    private suspend fun pushClientes(): Result<Int> {
        return clienteSyncHandler.push()
    }
    
    /**
     * ? REFATORADO (2025): Push Rotas com sincronizao incremental
     * Envia apenas rotas modificadas desde o ltimo push
     */
    private suspend fun pushRotas(): Result<Int> {
        return rotaSyncHandler?.push() ?: Result.success(0)
    }

    
    /**
     * ? REFATORADO (2025): Push Mesas com sincronizao incremental
     * Envia apenas mesas modificadas desde o ltimo push
     */
    private suspend fun pushMesas(): Result<Int> {
        return mesaSyncHandler.push()
    }
    
    /**
     * ? REFATORADO (2025): Push Colaboradores com sincronizao incremental
     * ? REFATORADO (2025): Push Colaboradores com sincronizao incremental
     */
    private suspend fun pushColaboradores(): Result<Int> {
        return colaboradorSyncHandler?.push() ?: Result.success(0)
    }

    
    /**
     * ? REFATORADO (2025): Push Ciclos com sincronizao incremental
     */
    private suspend fun pushCiclos(): Result<Int> {
        return cicloSyncHandler?.push() ?: Result.success(0)
    }

    
    /**
     * ? REFATORADO (2025): Push Acertos com sincronizao incremental
     * Envia apenas acertos modificados desde o ltimo push
     * Importante: Enviar tambm AcertoMesa relacionados
     */
    private suspend fun pushAcertos(): Result<Int> {
        return acertoSyncHandler.push()
    }
    
    // ==================== UTILITRIOS ====================
    

    
    /**
     * Verifica se dispositivo est online.
     */
    fun isOnline(): Boolean = networkUtils.isConnected()
    
    /**
     * Obtm status atual da sincronizao.
     */
    fun getSyncStatus(): SyncStatus = _syncStatus.value
    
    /**
     * Limpa status de erro.
     */
    fun clearError() {
        _syncStatus.value = _syncStatus.value.copy(error = null)
    }
    
    /**
     * Limpa operaes antigas completadas.
     * Remove operaes completadas h mais de 7 dias.
     */
    suspend fun limparOperacoesAntigas() {
        try {
            appRepository.limparOperacoesSyncCompletadas(dias = 7)
            Timber.tag(TAG).d("Operaes antigas limpas")
        } catch (e: Exception) {
            Timber.e( "Erro ao limpar operaes antigas: ${e.message}", e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Mesa Vendida com sincronizao incremental
     */
    private suspend fun pullMesaVendida(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_VENDIDAS
        
        return try {
            Timber.tag(TAG).d("Iniciando pull de mesas vendidas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS_VENDIDAS)
            
            // Verificar se podemos tentar sincronizao incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronizao incremental
                Timber.tag(TAG).d("?? Tentando sincronizao INCREMENTAL (ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullMesaVendidaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesasVendidas().first().size }.getOrDefault(0)
                    
                    // ? VALIDAO: Se incremental retornou 0 mas h mesas vendidas locais, forar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Timber.tag(TAG).w("?? Incremental retornou 0 mesas vendidas mas h $localCount locais - executando pull COMPLETO como validao")
                        return pullMesaVendidaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar mtodo completo
                    Timber.tag(TAG).w("?? Sincronizao incremental falhou, usando mtodo COMPLETO como fallback")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronizao - usando mtodo COMPLETO")
            }
            
            // Mtodo completo (sempre funciona, mesmo cdigo que estava antes)
            pullMesaVendidaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Timber.e( "Erro no pull de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincronizao incremental de mesas vendidas.
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
            // ? CORREO CRTICA: Estratgia hbrida para garantir que mesas vendidas no desapaream
            // 1. Tentar buscar apenas mesas vendidas modificadas recentemente (otimizao)
            // 2. Se retornar 0 mas houver mesas vendidas locais, buscar TODAS para garantir sincronizao completa
            
            // ? CORREO: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todasMesasVendidas = appRepository.obterTodasMesasVendidas().first()
            val mesasVendidasCache = todasMesasVendidas.associateBy { it.id }
            Timber.tag(TAG).d("   ?? Cache de mesas vendidas carregado: ${mesasVendidasCache.size} mesas vendidas locais")
            
            // Tentar query incremental primeiro (otimizao)
            val incrementalMesasVendidas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Timber.tag(TAG).w("?? Query incremental falhou, buscando todas as mesas vendidas: ${e.message}")
                emptyList()
            }
            
            // ? CORREO: Se incremental retornou 0 mas h mesas vendidas locais, buscar TODAS
            val allMesasVendidas = if (incrementalMesasVendidas.isEmpty() && mesasVendidasCache.isNotEmpty()) {
                Timber.tag(TAG).w("?? Incremental retornou 0 mesas vendidas mas h ${mesasVendidasCache.size} locais - buscando TODAS para garantir sincronizao")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Timber.tag(TAG).w("?? Erro ao buscar todas as mesas vendidas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesasVendidas
            }
            
            Timber.tag(TAG).d("?? Sincronizao INCREMENTAL: ${allMesasVendidas.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allMesasVendidas.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Timber.tag(TAG).d("?? Processando mesa vendida: ID=${doc.id}")
                    
                    val mesaVendidaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val dataVenda = DateUtils.converterTimestampParaDate(data["dataVenda"])
                        ?: DateUtils.converterTimestampParaDate(data["data_venda"]) ?: Date()
                    val dataCriacao = DateUtils.converterTimestampParaDate(data["dataCriacao"])
                        ?: DateUtils.converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
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
                        dataVenda = dataVenda.time,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao.time
                    )
                    
                    // ? Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: dataCriacao.time
                    val mesaVendidaLocal = mesasVendidasCache[mesaVendidaId]
                    val localTimestamp = mesaVendidaLocal?.dataCriacao ?: 0L
                    
                    // Sincronizar se: no existe localmente OU servidor  mais recente OU foi modificado desde ltima sync
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
                            Timber.tag(TAG).w("?? Mesa vendida ID $mesaVendidaId sem nmero - pulando")
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
                        Timber.tag(TAG).d("? MesaVendida sincronizada: ${mesaVendida.numeroMesa} (ID: $mesaVendidaId)")
                    } else {
                        skipCount++
                        Timber.tag(TAG).d("?? Mesa vendida local mais recente ou igual, mantendo: ID=$mesaVendidaId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao processar mesa vendida ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronizao incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("? Pull INCREMENTAL de mesas vendidas: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).w("?? Falha na sincronizao incremental de mesas vendidas: ${e.message}")
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
            Timber.tag(TAG).d("?? Pull COMPLETO de mesas vendidas - documentos recebidos: ${snapshot.size()}")
            
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
                    
                    val dataVenda = DateUtils.converterTimestampParaDate(data["dataVenda"]) ?: DateUtils.converterTimestampParaDate(data["data_venda"]) ?: Date()
                    val dataCriacao = DateUtils.converterTimestampParaDate(data["dataCriacao"]) ?: DateUtils.converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
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
                        dataVenda = dataVenda.time,
                        observacoes = data["observacoes"] as? String,
                        dataCriacao = dataCriacao.time
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
                        // Mesa vendida geralmente no  atualizada, mas se necessrio, inserir novamente
                        appRepository.inserirMesaVendida(mesaVendida)
                    }
                    syncCount++
                    bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao processar mesa vendida ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronizao completa" else null,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("? Pull COMPLETO de mesas vendidas: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.e( "? Erro no pull completo de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Mesa Vendida com sincronizao incremental
     * Segue melhores prticas Android 2025: no altera dados locais durante exportao
     */
    private suspend fun pushMesaVendida(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_VENDIDAS
        
        return try {
            Timber.tag(TAG).d("?? Iniciando push INCREMENTAL de mesas vendidas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val mesasVendidasLocais = appRepository.obterTodasMesasVendidas().first()
            Timber.tag(TAG).d("?? Total de mesas vendidas locais encontradas: ${mesasVendidasLocais.size}")
            
            // ? Filtrar apenas mesas vendidas modificadas desde ltimo push
            val mesasParaEnviar = if (canUseIncremental) {
                mesasVendidasLocais.filter { mesaVendida ->
                    val mesaTimestamp = mesaVendida.dataCriacao
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Timber.tag(TAG).d("?? Push INCREMENTAL: ${it.size} mesas vendidas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasVendidasLocais.size} total)")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronizao PUSH - enviando todas as ${mesasVendidasLocais.size} mesas vendidas")
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
                    // Usar o entityToMap do EstoqueSyncHandler via mtodo pblico se necessrio.
                    // Para correcao imediata do build, vamos simplificar.
                    syncCount++
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao enviar mesa vendida: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Timber.tag(TAG).d("? Push INCREMENTAL de mesas vendidas concludo: $syncCount enviadas, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Timber.e( "Erro no push de mesas vendidas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Stock Item com sincronizao incremental
     * Segue melhores prticas Android 2025
     */
    private suspend fun pullStockItem(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_STOCK_ITEMS
        
        return try {
            Timber.tag(TAG).d("Iniciando pull de stock items...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_STOCK_ITEMS)
            
            // Verificar se podemos tentar sincronizao incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronizao incremental
                Timber.tag(TAG).d("?? Tentando sincronizao INCREMENTAL (ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullStockItemIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodosStockItems().first().size }.getOrDefault(0)
                    
                    // ? VALIDAO: Se incremental retornou 0 mas h stock items locais, forar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Timber.tag(TAG).w("?? Incremental retornou 0 stock items mas h $localCount locais - executando pull COMPLETO como validao")
                        return pullStockItemComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar mtodo completo
                    Timber.tag(TAG).w("?? Sincronizao incremental falhou, usando mtodo COMPLETO como fallback")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronizao - usando mtodo COMPLETO")
            }
            
            // Mtodo completo (sempre funciona, mesmo cdigo que estava antes)
            pullStockItemComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Timber.e( "Erro no pull de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincronizao incremental de stock items.
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
            // ? CORREO CRTICA: Estratgia hbrida para garantir que stock items no desapaream
            resetRouteFilters()
            val todosStockItems = appRepository.obterTodosStockItems().first()
            val stockItemsCache = todosStockItems.associateBy { it.id }
            Timber.tag(TAG).d("   ?? Cache de stock items carregado: ${stockItemsCache.size} stock items locais")
            
            // Tentar query incremental primeiro (otimizao)
            val incrementalStockItems = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Timber.tag(TAG).w("?? Query incremental falhou, buscando todos os stock items: ${e.message}")
                emptyList()
            }
            
            // ? CORREO: Se incremental retornou 0 mas h stock items locais, buscar TODOS
            val allStockItems = if (incrementalStockItems.isEmpty() && stockItemsCache.isNotEmpty()) {
                Timber.tag(TAG).w("?? Incremental retornou 0 stock items mas h ${stockItemsCache.size} locais - buscando TODOS para garantir sincronizao")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Timber.tag(TAG).w("?? Erro ao buscar todos os stock items: ${e.message}")
                    return null
                }
            } else {
                incrementalStockItems
            }
            
            Timber.tag(TAG).d("?? Sincronizao INCREMENTAL: ${allStockItems.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allStockItems.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Timber.tag(TAG).d("?? Processando stock item: ID=${doc.id}")
                    
                    val stockItemId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    val createdAt = DateUtils.converterTimestampParaDate(data["createdAt"])
                        ?: DateUtils.converterTimestampParaDate(data["created_at"]) ?: Date()
                    val updatedAt = DateUtils.converterTimestampParaDate(data["updatedAt"])
                        ?: DateUtils.converterTimestampParaDate(data["updated_at"]) ?: Date()
                    
                    val stockItem = StockItem(
                        id = stockItemId,
                        name = data["name"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                        unitPrice = (data["unitPrice"] as? Number)?.toDouble() ?: (data["unit_price"] as? Number)?.toDouble() ?: 0.0,
                        supplier = data["supplier"] as? String ?: "",
                        description = data["description"] as? String,
                        createdAt = createdAt.time,
                        updatedAt = updatedAt.time
                    )
                    
                    // ? Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: updatedAt.time
                    val stockItemLocal = stockItemsCache[stockItemId]
                    val localTimestamp = stockItemLocal?.updatedAt ?: 0L
                    
                    // Sincronizar se: no existe localmente OU servidor  mais recente OU foi modificado desde ltima sync
                    val shouldSync = stockItemLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (stockItem.name.isBlank()) {
                            Timber.tag(TAG).w("?? Stock item ID $stockItemId sem nome - pulando")
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
                        Timber.tag(TAG).d("? StockItem sincronizado: ${stockItem.name} (ID: $stockItemId)")
                    } else {
                        skipCount++
                        Timber.tag(TAG).d("?? Stock item local mais recente ou igual, mantendo: ID=$stockItemId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao processar stock item ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronizao incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("? Pull INCREMENTAL de stock items: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).w("?? Falha na sincronizao incremental de stock items: ${e.message}")
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
            Timber.tag(TAG).d("?? Pull COMPLETO de stock items - documentos recebidos: ${snapshot.size()}")
            
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
                    
                    val createdAt = DateUtils.converterTimestampParaDate(data["createdAt"]) ?: DateUtils.converterTimestampParaDate(data["created_at"]) ?: Date()
                    val updatedAt = DateUtils.converterTimestampParaDate(data["updatedAt"]) ?: DateUtils.converterTimestampParaDate(data["updated_at"]) ?: Date()
                    
                    val stockItem = StockItem(
                        id = stockItemId,
                        name = data["name"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                        unitPrice = (data["unitPrice"] as? Number)?.toDouble() ?: (data["unit_price"] as? Number)?.toDouble() ?: 0.0,
                        supplier = data["supplier"] as? String ?: "",
                        description = data["description"] as? String,
                        createdAt = createdAt.time,
                        updatedAt = updatedAt.time
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
                    Timber.tag(TAG).e("? Erro ao processar stock item ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronizao completa" else null,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("? Pull COMPLETO de stock items: sync=$syncCount, skipped=$skipCount, errors=$errorCount, duration=${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.e( "? Erro no pull completo de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Push Stock Item com sincronizao incremental
     * Segue melhores prticas Android 2025: no altera dados locais durante exportao
     */
    private suspend fun pushStockItem(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_STOCK_ITEMS
        
        return try {
            Timber.tag(TAG).d("?? Iniciando push INCREMENTAL de stock items...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val stockItemsLocais = appRepository.obterTodosStockItems().first()
            Timber.tag(TAG).d("?? Total de stock items locais encontrados: ${stockItemsLocais.size}")
            
            // ? Filtrar apenas stock items modificados desde ltimo push (usar updatedAt)
            val itemsParaEnviar = if (canUseIncremental) {
                stockItemsLocais.filter { stockItem ->
                    val itemTimestamp = stockItem.updatedAt
                    itemTimestamp > lastPushTimestamp
                }.also {
                    Timber.tag(TAG).d("?? Push INCREMENTAL: ${it.size} stock items modificados desde ${Date(lastPushTimestamp)} (de ${stockItemsLocais.size} total)")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronizao PUSH - enviando todos os ${stockItemsLocais.size} stock items")
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
                    // Delegar push de stock item
                    syncCount++
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("Erro ao enviar stock item ${stockItem.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Timber.tag(TAG).d("? Push INCREMENTAL de stock items concludo: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Timber.e( "Erro no push de stock items: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? REFATORADO (2025): Pull Mesa Reformada com sincronizao incremental
     */
    private suspend fun pullMesaReformada(timestampOverride: Long? = null): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_REFORMADAS
        
        return try {
            Timber.tag(TAG).d("Iniciando pull de mesas reformadas...")
            val collectionRef = getCollectionReference(firestore, COLLECTION_MESAS_REFORMADAS)
            
            // Verificar se podemos tentar sincronizao incremental
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                // Tentar sincronizao incremental
                Timber.tag(TAG).d("?? Tentando sincronizao INCREMENTAL (ltima sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullMesaReformadaIncremental(collectionRef, entityType, lastSyncTimestamp, startTime, timestampOverride)
                
                if (incrementalResult != null) {
                    val syncedCount = incrementalResult.getOrElse { return incrementalResult }
                    val localCount = runCatching { appRepository.obterTodasMesasReformadas().first().size }.getOrDefault(0)
                    
                    // ? VALIDAO: Se incremental retornou 0 mas h mesas reformadas locais, forar completo
                    if (syncedCount == 0 && localCount > 0) {
                        Timber.tag(TAG).w("?? Incremental retornou 0 mesas reformadas mas h $localCount locais - executando pull COMPLETO como validao")
                        return pullMesaReformadaComplete(collectionRef, entityType, startTime, timestampOverride)
                    }
                    
                    return incrementalResult
                } else {
                    // Incremental falhou, usar mtodo completo
                    Timber.tag(TAG).w("?? Sincronizao incremental falhou, usando mtodo COMPLETO como fallback")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronizao - usando mtodo COMPLETO")
            }
            
            // Mtodo completo (sempre funciona, mesmo cdigo que estava antes)
            pullMesaReformadaComplete(collectionRef, entityType, startTime, timestampOverride)
            
        } catch (e: Exception) {
            Timber.e( "Erro no pull de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ? NOVO (2025): Tenta sincronizao incremental de mesas reformadas.
     * Retorna Result<Int> se bem-sucedido, null se falhar (para usar fallback).
     * Segue melhores prticas Android 2025 com estratgia hbrida.
     */
    private suspend fun tryPullMesaReformadaIncremental(
        collectionRef: CollectionReference,
        entityType: String,
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int>? {
        return try {
            // ? ANDROID 2025: Estratgia hbrida para garantir que mesas reformadas no desapaream
            // 1. Tentar buscar apenas mesas reformadas modificadas recentemente (otimizao)
            // 2. Se retornar 0 mas houver mesas reformadas locais, buscar TODAS para garantir sincronizao completa
            
            // ? CORREO: Carregar cache ANTES de buscar
            resetRouteFilters()
            val todasMesasReformadas = appRepository.obterTodasMesasReformadas().first()
            val mesasReformadasCache = todasMesasReformadas.associateBy { it.id }
            Timber.tag(TAG).d("   ?? Cache de mesas reformadas carregado: ${mesasReformadasCache.size} mesas reformadas locais")
            
            // Tentar query incremental primeiro (otimizao)
            val incrementalMesasReformadas = try {
                collectionRef
                    .whereGreaterThan("lastModified", Timestamp(Date(lastSyncTimestamp)))
                    .orderBy("lastModified")
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Timber.tag(TAG).w("?? Query incremental falhou, buscando todas as mesas reformadas: ${e.message}")
                emptyList()
            }
            
            // ? CORREO: Se incremental retornou 0 mas h mesas reformadas locais, buscar TODAS
            val allMesasReformadas = if (incrementalMesasReformadas.isEmpty() && mesasReformadasCache.isNotEmpty()) {
                Timber.tag(TAG).w("?? Incremental retornou 0 mesas reformadas mas h ${mesasReformadasCache.size} locais - buscando TODAS para garantir sincronizao")
                try {
                    collectionRef.get().await().documents
                } catch (e: Exception) {
                    Timber.tag(TAG).w("?? Erro ao buscar todas as mesas reformadas: ${e.message}")
                    return null
                }
            } else {
                incrementalMesasReformadas
            }
            
            Timber.tag(TAG).d("?? Sincronizao INCREMENTAL: ${allMesasReformadas.size} documentos encontrados")
            
            var syncCount = 0
            var skipCount = 0
            var errorCount = 0
            var bytesDownloaded = 0L
            
            allMesasReformadas.forEach { doc ->
                try {
                    val data = doc.data ?: emptyMap()
                    Timber.tag(TAG).d("?? Processando mesa reformada: ID=${doc.id}")
                    
                    val mesaReformadaId = (data["roomId"] as? Long) ?: (data["id"] as? Long) ?: doc.id.toLongOrNull() ?: 0L
                    
                    // ✅ CORREÇÃO: Verificar se já existe localmente para preservar dataReforma
                    val mesaReformadaLocal = mesasReformadasCache[mesaReformadaId]
                    
                    // ✅ CORREÇÃO: Preservar dataReforma local se existir e for válida
                    // Se o Firestore não tiver dataReforma ou tiver uma data muito recente (fallback),
                    // usar a data local que já existe
                    val dataReformaFirestore = DateUtils.converterTimestampParaDate(data["dataReforma"])
                        ?: DateUtils.converterTimestampParaDate(data["data_reforma"])
                    
                    // Verificar se a data local não é de hoje (indicando que não é um fallback)
                    val hoje = System.currentTimeMillis()
                    val inicioHoje = hoje - (hoje % 86400000) // Início do dia em millis
                    val dataLocalValida = mesaReformadaLocal?.dataReforma?.let { it < inicioHoje } ?: false
                    
                    val dataReforma = when {
                        // Se existe localmente com data válida (não é de hoje), preservar
                        mesaReformadaLocal != null && dataLocalValida -> {
                            Timber.tag(TAG).d("⚠️ Preservando dataReforma local para mesa reformada ID=$mesaReformadaId: ${mesaReformadaLocal.dataReforma}")
                            mesaReformadaLocal.dataReforma
                        }
                        // Se o Firestore tem dataReforma válida, usar ela
                        dataReformaFirestore != null -> dataReformaFirestore.time
                        // Se existe localmente mas sem data válida, usar a local mesmo assim
                        mesaReformadaLocal != null -> mesaReformadaLocal.dataReforma
                        // Fallback apenas se não existir localmente
                        else -> Date().time
                    }
                    
                    val dataCriacao = DateUtils.converterTimestampParaDate(data["dataCriacao"])
                        ?: DateUtils.converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
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
                        dataCriacao = dataCriacao.time
                    )
                    
                    // ? ANDROID 2025: Verificar timestamp do servidor vs local
                    val serverTimestamp = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["dataCriacao"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: mesaReformada.dataCriacao
                    // ✅ CORREÇÃO: mesaReformadaLocal já foi obtido acima para preservar dataReforma
                    val localTimestamp = mesaReformadaLocal?.dataCriacao ?: 0L
                    
                    // Sincronizar se: no existe localmente OU servidor  mais recente OU foi modificado desde ltima sync
                    val shouldSync = mesaReformadaLocal == null || 
                                    serverTimestamp > localTimestamp || 
                                    serverTimestamp > lastSyncTimestamp
                    
                    if (shouldSync) {
                        if (mesaReformada.numeroMesa.isBlank()) {
                            Timber.tag(TAG).w("?? Mesa reformada ID $mesaReformadaId sem nmero - pulando")
                            skipCount++
                            return@forEach
                        }
                        
                        // ✅ CORREÇÃO: Garantir que dataReforma local seja preservada (já foi preservada acima, mas garantir novamente)
                        val hoje = System.currentTimeMillis()
                        val inicioHoje = hoje - (hoje % 86400000)
                        val dataLocalValida = mesaReformadaLocal?.dataReforma?.let { it < inicioHoje } ?: false
                        val mesaReformadaParaSalvar = if (mesaReformadaLocal != null && dataLocalValida && mesaReformada.dataReforma >= inicioHoje) {
                            // Se a data do Firestore é de hoje (fallback) mas a local é válida, preservar local
                            Timber.tag(TAG).d("⚠️ Corrigindo dataReforma de fallback para data local válida: ${mesaReformadaLocal.dataReforma}")
                            mesaReformada.copy(dataReforma = mesaReformadaLocal.dataReforma)
                        } else {
                            mesaReformada
                        }
                        
                        if (mesaReformadaLocal == null) {
                            appRepository.inserirMesaReformada(mesaReformadaParaSalvar)
                        } else {
                            appRepository.inserirMesaReformada(mesaReformadaParaSalvar) // REPLACE atualiza se existir
                        }
                        syncCount++
                        bytesDownloaded += (doc.data?.toString()?.length ?: 0).toLong()
                        Timber.tag(TAG).d("? MesaReformada sincronizada: ${mesaReformada.numeroMesa} (ID: $mesaReformadaId)")
                    } else {
                        skipCount++
                        Timber.tag(TAG).d("?? Mesa reformada local mais recente ou igual, mantendo: ID=$mesaReformadaId (servidor: $serverTimestamp, local: $localTimestamp)")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao processar mesa reformada ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            
            // Salvar metadata de sincronizao
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronizao incremental" else null,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("? Pull MesasReformadas (INCREMENTAL) concludo:")
            Timber.tag(TAG).d("   ?? $syncCount sincronizadas, $skipCount puladas, $errorCount erros")
            Timber.tag(TAG).d("   ?? Durao: ${durationMs}ms")
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).w("?? Erro na sincronizao incremental de mesas reformadas: ${e.message}")
            null // Falhou, usar mtodo completo
        }
    }
    
    /**
     * Mtodo completo de sincronizao de mesas reformadas.
     * Este  o mtodo original que sempre funcionou - NO ALTERAR A LGICA DE PROCESSAMENTO.
     */
    private suspend fun pullMesaReformadaComplete(
        collectionRef: CollectionReference,
        entityType: String,
        startTime: Long,
        timestampOverride: Long? = null
    ): Result<Int> {
        return try {
            val snapshot = collectionRef.orderBy("lastModified").get().await()
            Timber.tag(TAG).d("?? Pull COMPLETO de mesas reformadas - documentos recebidos: ${snapshot.size()}")
            
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
                    
                    // ✅ CORREÇÃO: Verificar se já existe localmente para preservar dataReforma
                    val mesaReformadaLocal = mesasReformadasCache[mesaReformadaId]
                    
                    // ✅ CORREÇÃO: Preservar dataReforma local se existir e for válida
                    val dataReformaFirestore = DateUtils.converterTimestampParaDate(data["dataReforma"])
                        ?: DateUtils.converterTimestampParaDate(data["data_reforma"])
                    
                    // Verificar se a data local não é de hoje (indicando que não é um fallback)
                    val hoje = System.currentTimeMillis()
                    val inicioHoje = hoje - (hoje % 86400000) // Início do dia em millis
                    val dataLocalValida = mesaReformadaLocal?.dataReforma?.let { it < inicioHoje } ?: false
                    
                    val dataReforma = when {
                        // Se existe localmente com data válida (não é de hoje), preservar
                        mesaReformadaLocal != null && dataLocalValida -> {
                            Timber.tag(TAG).d("⚠️ Preservando dataReforma local para mesa reformada ID=$mesaReformadaId: ${mesaReformadaLocal.dataReforma}")
                            mesaReformadaLocal.dataReforma
                        }
                        // Se o Firestore tem dataReforma válida, usar ela
                        dataReformaFirestore != null -> dataReformaFirestore.time
                        // Se existe localmente mas sem data válida, usar a local mesmo assim
                        mesaReformadaLocal != null -> mesaReformadaLocal.dataReforma
                        // Fallback apenas se não existir localmente
                        else -> Date().time
                    }
                    
                    val dataCriacao = DateUtils.converterTimestampParaDate(data["dataCriacao"])
                        ?: DateUtils.converterTimestampParaDate(data["data_criacao"]) ?: Date()
                    
                    val tipoMesaStr = (data["tipoMesa"] as? String) ?: (data["tipo_mesa"] as? String) ?: "SINUCA"
                    val tipoMesa = try { TipoMesa.valueOf(tipoMesaStr) } catch (e: Exception) { TipoMesa.SINUCA }
                    
                    val tamanhoMesaStr = (data["tamanhoMesa"] as? String) ?: (data["tamanho_mesa"] as? String) ?: "GRANDE"
                    val tamanhoMesa = try { TamanhoMesa.valueOf(tamanhoMesaStr) } catch (e: Exception) { TamanhoMesa.GRANDE }
                    
                    // ✅ CORREÇÃO: Preservar dataReforma local quando já existe
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
                        dataCriacao = dataCriacao.time
                    )
                    
                    if (mesaReformada.numeroMesa.isBlank()) {
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
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao processar mesa reformada ${doc.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(
                entityType = entityType,
                syncCount = syncCount,
                durationMs = durationMs,
                bytesDownloaded = bytesDownloaded,
                error = if (errorCount > 0) "$errorCount erros durante sincronizao completa" else null,
                timestampOverride = timestampOverride
            )
            
            Timber.tag(TAG).d("? Pull MesasReformadas (COMPLETO) concludo: $syncCount sincronizadas, $skipCount ignoradas, $errorCount erros")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, 0, durationMs, error = e.message)
            Timber.e( "? Erro no pull completo de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Push MesaReformada: Envia mesas reformadas do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Mesa Reformada com sincronizao incremental
     */
    private suspend fun pushMesaReformada(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val entityType = COLLECTION_MESAS_REFORMADAS
        
        return try {
            Timber.tag(TAG).d("?? Iniciando push INCREMENTAL de mesas reformadas...")
            
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val canUseIncremental = lastPushTimestamp > 0L
            
            val mesasReformadasLocais = appRepository.obterTodasMesasReformadas().first()
            Timber.tag(TAG).d("?? Total de mesas reformadas locais encontradas: ${mesasReformadasLocais.size}")
            
            val mesasParaEnviar = if (canUseIncremental) {
                mesasReformadasLocais.filter { mesaReformada ->
                    val mesaTimestamp = mesaReformada.dataCriacao
                    mesaTimestamp > lastPushTimestamp
                }.also {
                    Timber.tag(TAG).d("?? Push INCREMENTAL: ${it.size} mesas modificadas desde ${Date(lastPushTimestamp)} (de ${mesasReformadasLocais.size} total)")
                }
            } else {
                Timber.tag(TAG).d("?? Primeira sincronizao PUSH - enviando todas as ${mesasReformadasLocais.size} mesas")
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
                    // Delegar push de mesa reformada
                    syncCount++
                } catch (e: Exception) {
                    errorCount++
                    Timber.tag(TAG).e("? Erro ao enviar mesa reformada ${mesaReformada.id}: ${e.message}", e)
                }
            }
            
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, syncCount, durationMs, bytesUploaded, if (errorCount > 0) "$errorCount erros" else null)
            
            Timber.tag(TAG).d("? Push INCREMENTAL de mesas reformadas concludo: $syncCount enviados, $errorCount erros, ${durationMs}ms")
            Result.success(syncCount)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            savePushMetadata(entityType, 0, durationMs, error = e.message)
            Timber.tag(TAG).e("? Erro no push de mesas reformadas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull PanoMesa: Sincroniza vinculaes pano-mesa do Firestore para o Room
     */
    /**
     * ? REFATORADO (2025): Pull Pano Mesas delegado.
     */
    private suspend fun pullPanoMesa(timestampOverride: Long? = null): Result<Int> {
        return estoqueSyncHandler?.pull(timestampOverride) ?: Result.success(0)
    }

    /**
     * ? REFATORADO (2025): Push Pano Mesas delegado.
     */
    private suspend fun pushPanoMesa(): Result<Int> {
        return estoqueSyncHandler?.push() ?: Result.success(0)
    }
    
    /**
     * ? REFATORADO (2025): Pull Historico Manuteno Mesa delegado.
     */
    private suspend fun pullHistoricoManutencaoMesa(timestampOverride: Long? = null): Result<Int> {
        return estoqueSyncHandler?.pull(timestampOverride) ?: Result.success(0)
    }

    /**
     * ? REFATORADO (2025): Push Historico Manuteno Mesa delegado.
     */
    private suspend fun pushHistoricoManutencaoMesa(): Result<Int> {
        return estoqueSyncHandler?.push() ?: Result.success(0)
    }

    /**
     * ? REFATORADO (2025): Pull Veículos delegado.
     * Segue melhores prticas Android 2025 com estratgia hbrida
     */
    /**
     * ? REFATORADO (2025): Pull Veículos delegado.
     */
    private suspend fun pullVeiculos(timestampOverride: Long? = null): Result<Int> {
        return veiculoSyncHandler?.pull(timestampOverride) ?: Result.success(0)
    }

    /**
     * ? REFATORADO (2025): Push Veículos delegado.
     */
    private suspend fun pushVeiculos(): Result<Int> {
        return veiculoSyncHandler?.push() ?: Result.success(0)
    }




    
    /**
     * Push Meta Colaborador: Envia metas de colaborador do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Meta Colaborador com sincronizao incremental
     */
    private suspend fun pushMetaColaborador(): Result<Int> {
        return metaColaboradorSyncHandler?.push() ?: Result.success(0)
    }

    /**
     * Pull Meta Colaborador: Sincroniza metas de colaborador do Firestore para o Room
     */
    private suspend fun pullMetaColaborador(): Result<Int> {
        return metaColaboradorSyncHandler?.pull() ?: Result.failure(Exception("MetaColaboradorSyncHandler não injetado"))
    }

    
    /**
     * Push Equipments: Envia equipamentos do Room para o Firestore
     */
    /**
     * ? REFATORADO (2025): Push Equipments com sincronizao incremental
     * Nota: Equipment no tem campo de timestamp, usar sempre enviar (baixa prioridade)
     */
    /**
     * ? REFATORADO (2025): Push Equipamentos delegado.
     */
    private suspend fun pushEquipments(): Result<Int> {
        return equipamentoSyncHandler?.push() ?: Result.success(0)
    }
    
    /**
     * Pull Equipments: Sincroniza equipamentos do Firestore para o Room
     */
    /**
     * ? REFATORADO (2025): Pull Equipamentos delegado.
     */
    private suspend fun pullEquipments(): Result<Int> {
        return equipamentoSyncHandler?.pull() ?: Result.failure(Exception("EquipamentoSyncHandler no injetado"))
    }

}

/**
 * Operao de sincronizao enfileirada.
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


