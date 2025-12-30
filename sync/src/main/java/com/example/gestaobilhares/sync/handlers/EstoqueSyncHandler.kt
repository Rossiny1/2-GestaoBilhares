package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.entities.MesaVendida
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.PanoMesa
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.example.gestaobilhares.core.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Handler especializado para sincronização de Estoque e Manutenção de Mesas.
 */
class EstoqueSyncHandler @javax.inject.Inject constructor(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = "estoque"

    companion object {
        private const val COLLECTION_PANOS_ESTOQUE = "panos_estoque"
        private const val COLLECTION_MESAS_VENDIDAS = "mesas_vendidas"
        private const val COLLECTION_MESAS_REFORMADAS = "mesas_reformadas"
        private const val COLLECTION_PANO_MESA = "pano_mesa"
        private const val COLLECTION_MANUTENCAO_MESA = "historico_manutencao_mesa"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        var totalCount = 0
        pullPanoEstoque().onSuccess { totalCount += it }
        pullMesaVendida().onSuccess { totalCount += it }
        pullMesaReformada().onSuccess { totalCount += it }
        pullPanoMesa().onSuccess { totalCount += it }
        pullHistoricoManutencaoMesa().onSuccess { totalCount += it }
        return Result.success(totalCount)
    }

    private suspend fun pullPanoEstoque(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_PANOS_ESTOQUE
        return try {
            val collectionRef = getCollectionReference(type)
            val snapshot = collectionRef.get().await()
            var count = 0
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val item = PanoEstoque(
                    id = (data["roomId"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue,
                    numero = data["numero"] as? String ?: "",
                    cor = data["cor"] as? String ?: "",
                    tamanho = data["tamanho"] as? String ?: "",
                    material = data["material"] as? String ?: "",
                    disponivel = data["disponivel"] as? Boolean ?: true,
                    observacoes = data["observacoes"] as? String
                )
                appRepository.inserirPanoEstoque(item)
                count++
            }
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun pullMesaVendida(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_MESAS_VENDIDAS
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else collectionRef.get().await()
            
            var count = 0
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val tipoMesaStr = data["tipoMesa"] as? String ?: "SINUCA"
                val tamanhoMesaStr = data["tamanhoMesa"] as? String ?: "GRANDE"
                val estadoConservacaoStr = data["estadoConservacao"] as? String ?: "BOM"

                val item = MesaVendida(
                    id = (data["roomId"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue,
                    mesaIdOriginal = (data["mesaIdOriginal"] as? Number)?.toLong() ?: (data["mesaId"] as? Number)?.toLong() ?: 0L,
                    numeroMesa = data["numeroMesa"] as? String ?: "",
                    tipoMesa = try { com.example.gestaobilhares.data.entities.TipoMesa.valueOf(tipoMesaStr) } catch (e: Exception) { com.example.gestaobilhares.data.entities.TipoMesa.SINUCA },
                    tamanhoMesa = try { com.example.gestaobilhares.data.entities.TamanhoMesa.valueOf(tamanhoMesaStr) } catch (e: Exception) { com.example.gestaobilhares.data.entities.TamanhoMesa.GRANDE },
                    estadoConservacao = try { com.example.gestaobilhares.data.entities.EstadoConservacao.valueOf(estadoConservacaoStr) } catch (e: Exception) { com.example.gestaobilhares.data.entities.EstadoConservacao.BOM },
                    nomeComprador = data["nomeComprador"] as? String ?: "",
                    telefoneComprador = data["telefoneComprador"] as? String,
                    cpfCnpjComprador = data["cpfCnpjComprador"] as? String,
                    enderecoComprador = data["enderecoComprador"] as? String,
                    dataVenda = DateUtils.convertToLong(data["dataVenda"]) ?: System.currentTimeMillis(),
                    valorVenda = (data["valorVenda"] as? Number)?.toDouble() ?: 0.0,
                    observacoes = data["observacoes"] as? String
                )
                appRepository.inserirMesaVendida(item)
                count++
            }
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun pullMesaReformada(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_MESAS_REFORMADAS
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else collectionRef.get().await()
            
            var count = 0
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val tipoMesaStr = data["tipoMesa"] as? String ?: "SINUCA"
                val tamanhoMesaStr = data["tamanhoMesa"] as? String ?: "GRANDE"
                
                val item = MesaReformada(
                    id = (data["roomId"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue,
                    mesaId = (data["mesaId"] as? Number)?.toLong() ?: 0L,
                    numeroMesa = data["numeroMesa"] as? String ?: "",
                    tipoMesa = try { com.example.gestaobilhares.data.entities.TipoMesa.valueOf(tipoMesaStr) } catch (e: Exception) { com.example.gestaobilhares.data.entities.TipoMesa.SINUCA },
                    tamanhoMesa = try { com.example.gestaobilhares.data.entities.TamanhoMesa.valueOf(tamanhoMesaStr) } catch (e: Exception) { com.example.gestaobilhares.data.entities.TamanhoMesa.GRANDE },
                    pintura = data["pintura"] as? Boolean ?: false,
                    tabela = data["tabela"] as? Boolean ?: false,
                    panos = data["panos"] as? Boolean ?: false,
                    numeroPanos = data["numeroPanos"] as? String,
                    outros = data["outros"] as? Boolean ?: false,
                    observacoes = data["observacoes"] as? String,
                    fotoReforma = data["fotoReforma"] as? String,
                    dataReforma = DateUtils.convertToLong(data["dataReforma"]) ?: System.currentTimeMillis()
                )
                appRepository.inserirMesaReformada(item)
                count++
            }
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun pullPanoMesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_PANO_MESA
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else collectionRef.get().await()
            
            var count = 0
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val item = PanoMesa(
                    id = (data["roomId"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue,
                    mesaId = (data["mesaId"] as? Number)?.toLong() ?: 0L,
                    panoId = (data["panoId"] as? Number)?.toLong() ?: (data["panoEstoqueId"] as? Number)?.toLong() ?: 0L,
                    dataTroca = DateUtils.convertToLong(data["dataTroca"]) ?: System.currentTimeMillis(),
                    ativo = data["ativo"] as? Boolean ?: true,
                    observacoes = data["observacoes"] as? String
                )
                appRepository.inserirPanoMesa(item)
                count++
            }
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun pullHistoricoManutencaoMesa(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_MANUTENCAO_MESA
        return try {
            val collectionRef = getCollectionReference(type)
            val lastSync = getLastSyncTimestamp(type)
            val snapshot = if (lastSync > 0L) {
                collectionRef.whereGreaterThan("lastModified", Timestamp(Date(lastSync))).get().await()
            } else collectionRef.get().await()
            
            var count = 0
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val tipoManutencaoStr = data["tipoManutencao"] as? String ?: "OUTROS"
                
                val item = HistoricoManutencaoMesa(
                    id = (data["roomId"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue,
                    mesaId = (data["mesaId"] as? Number)?.toLong() ?: 0L,
                    numeroMesa = data["numeroMesa"] as? String ?: "",
                    tipoManutencao = try { com.example.gestaobilhares.data.entities.TipoManutencao.valueOf(tipoManutencaoStr) } catch (e: Exception) { com.example.gestaobilhares.data.entities.TipoManutencao.OUTROS },
                    descricao = data["descricao"] as? String,
                    dataManutencao = DateUtils.convertToLong(data["dataManutencao"]) ?: System.currentTimeMillis(),
                    responsavel = data["responsavel"] as? String,
                    observacoes = data["observacoes"] as? String,
                    custo = (data["custo"] as? Number)?.toDouble() ?: (data["valor"] as? Number)?.toDouble()
                )
                appRepository.inserirHistoricoManutencaoMesa(item)
                count++
            }
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun push(): Result<Int> {
        var totalCount = 0
        pushPanoEstoque().onSuccess { totalCount += it }
        pushMesaVendida().onSuccess { totalCount += it }
        pushMesaReformada().onSuccess { totalCount += it }
        pushPanoMesa().onSuccess { totalCount += it }
        pushHistoricoManutencaoMesa().onSuccess { totalCount += it }
        return Result.success(totalCount)
    }

    private suspend fun pushPanoEstoque(): Result<Int> {
        return pushGeneric(COLLECTION_PANOS_ESTOQUE, { appRepository.obterTodosPanoEstoque().first() }, { it.id })
    }

    private suspend fun pushMesaVendida(): Result<Int> {
        return pushGeneric(COLLECTION_MESAS_VENDIDAS, { appRepository.obterTodasMesasVendidas().first() }, { it.id })
    }

    private suspend fun pushMesaReformada(): Result<Int> {
        return pushGeneric(COLLECTION_MESAS_REFORMADAS, { appRepository.obterTodasMesasReformadas().first() }, { it.id })
    }

    private suspend fun pushPanoMesa(): Result<Int> {
        return pushGeneric(COLLECTION_PANO_MESA, { appRepository.obterTodosPanoMesa() }, { it.id })
    }

    private suspend fun pushHistoricoManutencaoMesa(): Result<Int> {
        return pushGeneric(COLLECTION_MANUTENCAO_MESA, { appRepository.obterTodosHistoricoManutencaoMesa().first() }, { it.id })
    }

    private suspend fun <T : Any> pushGeneric(
        collectionName: String,
        fetchLocals: suspend () -> List<T>,
        getId: (T) -> Long
    ): Result<Int> {
        val startTime = System.currentTimeMillis()
        return try {
            val lastPush = getLastPushTimestamp(collectionName)
            val locais = fetchLocals() // Note: Ideal filtering should be done here if possible, or inside fetchLocals
            
            if (locais.isEmpty()) return Result.success(0)
            
            val collectionRef = getCollectionReference(collectionName)
            var count = 0
            for (item in locais) {
                // Since most of these don't have a reliable dataAtualizacao in the DTO, we might just send all or use a fallback
                // For simplicity in this refactor, we are mimicking the previous behavior
                val map = entityToMap(item)
                map["id"] = getId(item)
                map["lastModified"] = FieldValue.serverTimestamp()
                collectionRef.document(getId(item).toString()).set(map).await()
                count++
            }
            savePushMetadata(collectionName, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) { Result.failure(e) }
    }
}
