package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal
import com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.example.gestaobilhares.core.utils.DateUtils
import java.util.Date

/**
 * Handler especializado para sincronização de Assinaturas e Logs de Auditoria.
 */
class AssinaturaSyncHandler @javax.inject.Inject constructor(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = COLLECTION_ASSINATURAS

    private val logsEntityType: String = COLLECTION_LOGS_AUDITORIA

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        var totalCount = 0
        
        // 1. Pull Assinaturas
        pullAssinaturas().onSuccess { totalCount += it }
        
        // 2. Pull Logs
        pullLogs().onSuccess { totalCount += it }
        
        return Result.success(totalCount)
    }

    private suspend fun pullAssinaturas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = entityType
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else {
                collectionRef.get().await()
            }
            
            var count = 0
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val id = (data["roomId"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue
                
                val assinatura = AssinaturaRepresentanteLegal(
                    id = id,
                    nomeRepresentante = data["nomeRepresentante"] as? String ?: "",
                    cpfRepresentante = data["cpfRepresentante"] as? String ?: "",
                    cargoRepresentante = data["cargoRepresentante"] as? String ?: "",
                    assinaturaBase64 = data["assinaturaBase64"] as? String ?: "",
                    timestampCriacao = (data["timestampCriacao"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    deviceId = data["deviceId"] as? String ?: "",
                    hashIntegridade = data["hashIntegridade"] as? String ?: "",
                    versaoSistema = data["versaoSistema"] as? String ?: "",
                    dataCriacao = DateUtils.convertToLong(data["dataCriacao"]) ?: System.currentTimeMillis(),
                    criadoPor = data["criadoPor"] as? String ?: "",
                    ativo = data["ativo"] as? Boolean ?: true,
                    numeroProcuração = data["numeroProcuração"] as? String ?: "",
                    dataProcuração = DateUtils.convertToLong(data["dataProcuração"]) ?: System.currentTimeMillis(),
                    poderesDelegados = data["poderesDelegados"] as? String ?: "",
                    validadeProcuração = DateUtils.convertToLong(data["validadeProcuração"]),
                    totalUsos = (data["totalUsos"] as? Number)?.toInt() ?: 0,
                    ultimoUso = DateUtils.convertToLong(data["ultimoUso"]),
                    contratosAssinados = data["contratosAssinados"] as? String ?: "",
                    validadaJuridicamente = data["validadaJuridicamente"] as? Boolean ?: false,
                    dataValidacao = DateUtils.convertToLong(data["dataValidacao"]),
                    validadoPor = data["validadoPor"] as? String
                )
                
                val local = appRepository.obterAssinaturaRepresentanteLegalPorId(id)
                if (local == null) appRepository.inserirAssinaturaRepresentanteLegal(assinatura)
                else appRepository.atualizarAssinaturaRepresentanteLegal(assinatura)
                count++
            }
            
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pullLogs(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = logsEntityType
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else {
                collectionRef.get().await()
            }
            
            var count = 0
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val id = (data["roomId"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue
                
                val log = LogAuditoriaAssinatura(
                    id = id,
                    tipoOperacao = data["tipoOperacao"] as? String ?: "",
                    idAssinatura = (data["idAssinatura"] as? Number)?.toLong() ?: 0L,
                    idContrato = (data["idContrato"] as? Number)?.toLong(),
                    idAditivo = (data["idAditivo"] as? Number)?.toLong(),
                    usuarioExecutou = data["usuarioExecutou"] as? String ?: "",
                    cpfUsuario = data["cpfUsuario"] as? String ?: "",
                    cargoUsuario = data["cargoUsuario"] as? String ?: "",
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    deviceId = data["deviceId"] as? String ?: "",
                    versaoApp = data["versaoApp"] as? String ?: "",
                    hashDocumento = data["hashDocumento"] as? String ?: "",
                    hashAssinatura = data["hashAssinatura"] as? String ?: "",
                    latitude = (data["latitude"] as? Number)?.toDouble(),
                    longitude = (data["longitude"] as? Number)?.toDouble(),
                    endereco = data["endereco"] as? String,
                    ipAddress = data["ipAddress"] as? String,
                    userAgent = data["userAgent"] as? String,
                    tipoDocumento = data["tipoDocumento"] as? String ?: "",
                    numeroDocumento = data["numeroDocumento"] as? String ?: "",
                    valorContrato = (data["valorContrato"] as? Number)?.toDouble(),
                    sucesso = data["sucesso"] as? Boolean ?: true,
                    mensagemErro = data["mensagemErro"] as? String,
                    dataOperacao = DateUtils.convertToLong(data["dataOperacao"]) ?: System.currentTimeMillis(),
                    observacoes = data["observacoes"] as? String,
                    validadoJuridicamente = data["validadaJuridicamente"] as? Boolean ?: false,
                    dataValidacao = DateUtils.convertToLong(data["dataValidacao"]),
                    validadoPor = data["validadoPor"] as? String
                )
                
                appRepository.inserirLogAuditoriaAssinatura(log)
                count++
            }
            
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun push(): Result<Int> {
        var totalCount = 0
        pushAssinaturas().onSuccess { totalCount += it }
        pushLogs().onSuccess { totalCount += it }
        return Result.success(totalCount)
    }

    private suspend fun pushAssinaturas(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = entityType
        return try {
            val lastPush = getLastPushTimestamp(type)
            val locais = appRepository.obterTodasAssinaturasRepresentanteLegal()
                .filter { it.dataCriacao > lastPush }
            
            if (locais.isEmpty()) return Result.success(0)
            
            val collectionRef = getCollectionReference(type)
            var count = 0
            for (item in locais) {
                val map = entityToMap(item)
                map["id"] = item.id
                map["lastModified"] = FieldValue.serverTimestamp()
                collectionRef.document(item.id.toString()).set(map).await()
                count++
            }
            
            savePushMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pushLogs(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = logsEntityType
        return try {
            val lastPush = getLastPushTimestamp(type)
            val locais = appRepository.obterTodosLogsAuditoria()
                .filter { it.dataOperacao > lastPush }
            
            if (locais.isEmpty()) return Result.success(0)
            
            val collectionRef = getCollectionReference(type)
            var count = 0
            for (item in locais) {
                val map = entityToMap(item)
                map["id"] = item.id
                map["lastModified"] = FieldValue.serverTimestamp()
                collectionRef.document(item.id.toString()).set(map).await()
                count++
            }
            
            savePushMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val COLLECTION_ASSINATURAS = "assinaturas"
        private const val COLLECTION_LOGS_AUDITORIA = "logs_auditoria_assinatura"
    }
}
