package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.entities.TipoDespesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.core.utils.DateUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

/**
 * Handler especializado para sincronização de Despesas.
 */
class DespesaSyncHandler(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = COLLECTION_DESPESAS

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        if (!networkUtils.isConnected()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        val startTime = System.currentTimeMillis()
        
        return try {
            Timber.tag(TAG).d("Iniciando pull de despesas...")
            
            val lastSyncTimestamp = getLastSyncTimestamp(entityType)
            val canUseIncremental = lastSyncTimestamp > 0L
            
            if (canUseIncremental) {
                Timber.tag(TAG).d("Tentando sincronização INCREMENTAL (última sync: ${Date(lastSyncTimestamp)})")
                val incrementalResult = tryPullIncremental(lastSyncTimestamp, startTime, timestampOverride)
                if (incrementalResult != null) {
                    return incrementalResult
                }
                Timber.tag(TAG).w("Sincronização incremental de despesas falhou, usando método COMPLETO")
            }
            
            pullComplete(startTime, timestampOverride)
            
            // 2. Pull Categorias
            pullCategoriasDespesa().onSuccess { count ->
                Timber.tag(TAG).d("? Pull Categorias: $count sincronizados")
            }
            
            // 3. Pull Tipos
            pullTiposDespesa().onSuccess { count ->
                Timber.tag(TAG).d("? Pull Tipos: $count sincronizados")
            }
            
            Result.success(0) // Return value will be ignored by orchestrator if using individual counts
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no pull de despesas")
            Result.failure(e)
        }
    }

    private suspend fun tryPullIncremental(
        lastSyncTimestamp: Long,
        startTime: Long,
        timestampOverride: Long?
    ): Result<Int>? {
        return try {
            val collectionRef = getCollectionReference(COLLECTION_DESPESAS)
            val documents = try {
                fetchDocumentsWithRouteFilter(
                    collectionRef = collectionRef,
                    routeField = FIELD_ROTA_ID,
                    lastSyncTimestamp = lastSyncTimestamp
                )
            } catch (e: Exception) {
                Timber.tag(TAG).w("Erro ao executar query incremental de despesas: ${e.message}")
                return null
            }
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val syncCount = processDocuments(documents)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).w("Erro na sincronização incremental de despesas: ${e.message}")
            null
        }
    }

    private suspend fun pullComplete(startTime: Long, timestampOverride: Long?): Result<Int> {
        return try {
            val collectionRef = getCollectionReference(COLLECTION_DESPESAS)
            val documents = fetchAllDocumentsWithRouteFilter(collectionRef, FIELD_ROTA_ID)
            
            if (documents.isEmpty()) {
                saveSyncMetadata(entityType, 0, System.currentTimeMillis() - startTime, timestampOverride = timestampOverride)
                return Result.success(0)
            }
            
            val syncCount = processDocuments(documents)
            
            val durationMs = System.currentTimeMillis() - startTime
            saveSyncMetadata(entityType, syncCount, durationMs, timestampOverride = timestampOverride)
            
            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no pull completo de despesas")
            Result.failure(e)
        }
    }

    private suspend fun processDocuments(documents: List<DocumentSnapshot>): Int {
        var syncCount = 0
        for (doc in documents) {
            try {
                val data = doc.data ?: continue
                val despesaId = (data["roomId"] as? Long) 
                    ?: (data["id"] as? Long) 
                    ?: doc.id.toLongOrNull() 
                    ?: continue
                
                val rotaId = (data["rotaId"] as? Number)?.toLong() 
                    ?: (data["rota_id"] as? Number)?.toLong() 
                    ?: 0L
                
                if (!shouldSyncRouteData(rotaId, allowUnknown = false)) continue
                
                val dataHoraLocalDateTime = converterTimestampParaLocalDateTime(data["dataHora"])
                    ?: converterTimestampParaLocalDateTime(data["data_hora"])
                    ?: LocalDateTime.now()
                
                val dataHoraLong = dataHoraLocalDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                
                val despesaFirestore = Despesa(
                    id = despesaId,
                    rotaId = rotaId,
                    descricao = (data["descricao"] as? String) ?: "",
                    valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                    categoria = (data["categoria"] as? String) ?: "",
                    tipoDespesa = (data["tipoDespesa"] as? String) ?: (data["tipo_despesa"] as? String) ?: "",
                    dataHora = dataHoraLong,
                    observacoes = (data["observacoes"] as? String) ?: "",
                    criadoPor = (data["criadoPor"] as? String) ?: (data["criado_por"] as? String) ?: "",
                    cicloId = (data["cicloId"] as? Number)?.toLong() ?: (data["ciclo_id"] as? Number)?.toLong(),
                    origemLancamento = (data["origemLancamento"] as? String) ?: (data["origem_lancamento"] as? String) ?: "ROTA",
                    cicloAno = (data["cicloAno"] as? Number)?.toInt() ?: (data["ciclo_ano"] as? Number)?.toInt(),
                    cicloNumero = (data["cicloNumero"] as? Number)?.toInt() ?: (data["ciclo_numero"] as? Number)?.toInt(),
                    fotoComprovante = (data["fotoComprovante"] as? String) ?: (data["foto_comprovante"] as? String),
                    dataFotoComprovante = DateUtils.convertToLong(data["dataFotoComprovante"]) ?: DateUtils.convertToLong(data["data_foto_comprovante"]),
                    veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: (data["veiculo_id"] as? Number)?.toLong(),
                    kmRodado = (data["kmRodado"] as? Number)?.toLong() ?: (data["km_rodado"] as? Number)?.toLong(),
                    litrosAbastecidos = (data["litrosAbastecidos"] as? Number)?.toDouble() ?: (data["litros_abastecidos"] as? Number)?.toDouble()
                )
                
                val despesaLocal = appRepository.obterDespesaPorId(despesaId)
                val serverTimestamp = DateUtils.convertToLong(data["lastModified"])
                    ?: DateUtils.convertToLong(data["syncTimestamp"])
                    ?: dataHoraLong
                
                val localTimestamp = despesaLocal?.dataHora ?: 0L
                
                if (despesaLocal == null) {
                    appRepository.inserirDespesa(despesaFirestore)
                    syncCount++
                } else if (serverTimestamp > (localTimestamp + 1000)) {
                    appRepository.atualizarDespesa(despesaFirestore)
                    syncCount++
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Erro ao processar despesa ${doc.id}")
            }
        }
        return syncCount
    }

    override suspend fun push(): Result<Int> {
        if (!networkUtils.isConnected()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        val startTime = System.currentTimeMillis()
        
        return try {
            val lastPushTimestamp = getLastPushTimestamp(entityType)
            val despesasLocais = appRepository.obterTodasDespesas().first()
            
            val paraEnviar = despesasLocais.filter { despesa ->
                despesa.dataHora > lastPushTimestamp
            }
            
            if (paraEnviar.isEmpty()) {
                savePushMetadata(entityType, 0, System.currentTimeMillis() - startTime)
                return Result.success(0)
            }
            
            var syncCount = 0
            for (despesa in paraEnviar) {
                try {
                    val map = entityToMap(despesa)
                    map["roomId"] = despesa.id
                    map["id"] = despesa.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    map["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    val collectionRef = getCollectionReference(COLLECTION_DESPESAS)
                    collectionRef.document(despesa.id.toString()).set(map).await()
                    
                    // Atualiza timestamp local para evitar sobrescrita imediata per pull
                    val despesaAtualizada = despesa.copy(dataHora = System.currentTimeMillis())
                    appRepository.atualizarDespesa(despesaAtualizada)
                    
                    syncCount++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar despesa ${despesa.id}")
                }
            }
            
            savePushMetadata(entityType, syncCount, System.currentTimeMillis() - startTime)
            
            // 2. Push Categorias
            pushCategoriasDespesa().onSuccess { pCount ->
                Timber.tag(TAG).d("? Push Categorias: $pCount sincronizados")
            }
            
            // 3. Push Tipos
            pushTiposDespesa().onSuccess { pCount ->
                Timber.tag(TAG).d("? Push Tipos: $pCount sincronizados")
            }

            Result.success(syncCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro no push de despesas")
            Result.failure(e)
        }
    }

    /**
     * Pull Categorias Despesa: Sincroniza categorias do Firestore para o Room
     */
    suspend fun pullCategoriasDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_CATEGORIAS_DESPESA
        
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else {
                collectionRef.get().await()
            }
            
            var syncCount = 0
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val id = (data["roomId"] as? Number)?.toLong() 
                        ?: (data["id"] as? Number)?.toLong() 
                        ?: doc.id.toLongOrNull() 
                        ?: continue
                        
                    val categoria = CategoriaDespesa(
                        id = id,
                        nome = data["nome"] as? String ?: "Sem nome",
                        descricao = data["descricao"] as? String ?: "",
                        ativa = data["ativa"] as? Boolean ?: true,
                        dataCriacao = DateUtils.convertToLong(data["dataCriacao"]) ?: System.currentTimeMillis(),
                        dataAtualizacao = DateUtils.convertToLong(data["dataAtualizacao"]) ?: System.currentTimeMillis(),
                        criadoPor = data["criadoPor"] as? String ?: ""
                    )
                    
                    appRepository.criarCategoria(categoria)
                    syncCount++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao processar categoria despesa ${doc.id}")
                }
            }
            
            saveSyncMetadata(type, syncCount, System.currentTimeMillis() - startTime)
            Result.success(syncCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pull Tipos Despesa: Sincroniza tipos do Firestore para o Room
     */
    suspend fun pullTiposDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_TIPOS_DESPESA
        
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else {
                collectionRef.get().await()
            }
            
            var syncCount = 0
            for (doc in snapshot.documents) {
                try {
                    val data = doc.data ?: continue
                    val id = (data["roomId"] as? Number)?.toLong() 
                        ?: (data["id"] as? Number)?.toLong() 
                        ?: doc.id.toLongOrNull() 
                        ?: continue
                    
                    val categoriaId = (data["categoriaId"] as? Number)?.toLong() ?: continue
                        
                    val tipo = TipoDespesa(
                        id = id,
                        categoriaId = categoriaId,
                        nome = data["nome"] as? String ?: "Sem nome",
                        descricao = data["descricao"] as? String ?: "",
                        ativo = data["ativo"] as? Boolean ?: true,
                        dataCriacao = DateUtils.convertToLong(data["dataCriacao"]) ?: System.currentTimeMillis(),
                        dataAtualizacao = DateUtils.convertToLong(data["dataAtualizacao"]) ?: System.currentTimeMillis(),
                        criadoPor = data["criadoPor"] as? String ?: ""
                    )
                    
                    appRepository.criarTipo(tipo)
                    syncCount++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao processar tipo despesa ${doc.id}")
                }
            }
            
            saveSyncMetadata(type, syncCount, System.currentTimeMillis() - startTime)
            Result.success(syncCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Push Categorias Despesa: Envia categorias locais para o Firestore
     */
    suspend fun pushCategoriasDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_CATEGORIAS_DESPESA
        
        return try {
            val lastPush = getLastPushTimestamp(type)
            val categorias = appRepository.buscarCategoriasAtivas().first()
                .filter { it.dataAtualizacao > lastPush }
            
            if (categorias.isEmpty()) return Result.success(0)
            
            var count = 0
            val collectionRef = getCollectionReference(type)
            
            for (item in categorias) {
                try {
                    val map = entityToMap(item)
                    map["id"] = item.id
                    map["roomId"] = item.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    collectionRef.document(item.id.toString()).set(map).await()
                    count++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar categoria ${item.id}")
                }
            }
            
            savePushMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Push Tipos Despesa: Envia tipos locais para o Firestore
     */
    suspend fun pushTiposDespesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_TIPOS_DESPESA
        
        return try {
            val lastPush = getLastPushTimestamp(type)
            val tipos = appRepository.buscarTiposAtivosComCategoria().first()
                .map { it.tipoDespesa }
                .filter { it.dataAtualizacao > lastPush }
            
            if (tipos.isEmpty()) return Result.success(0)
            
            var count = 0
            val collectionRef = getCollectionReference(type)
            
            for (item in tipos) {
                try {
                    val map = entityToMap(item)
                    map["id"] = item.id
                    map["roomId"] = item.id
                    map["lastModified"] = FieldValue.serverTimestamp()
                    collectionRef.document(item.id.toString()).set(map).await()
                    count++
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar tipo ${item.id}")
                }
            }
            
            savePushMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
