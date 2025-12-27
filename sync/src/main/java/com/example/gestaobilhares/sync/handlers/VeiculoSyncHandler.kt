package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.Veiculo
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.utils.FirebaseImageUploader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp

/**
 * Handler especializado para sincronização de Veículos e Históricos (Manutenção e Combustível).
 */
class VeiculoSyncHandler @javax.inject.Inject constructor(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = "veiculos"

    companion object {
        private const val COLLECTION_VEICULOS = "veiculos"
        private const val COLLECTION_MANUTENCAO = "historico_manutencao_veiculo"
        private const val COLLECTION_COMBUSTIVEL = "historico_combustivel_veiculo"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        var totalCount = 0
        
        // 1. Pull Veiculos
        pullVeiculos().onSuccess { totalCount += it }
        
        // 2. Pull Manutencao
        pullManutencao().onSuccess { totalCount += it }
        
        // 3. Pull Combustivel
        pullCombustivel().onSuccess { totalCount += it }
        
        return Result.success(totalCount)
    }

    private suspend fun pullVeiculos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_VEICULOS
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
                
                val veiculo = Veiculo(
                    id = id,
                    nome = data["nome"] as? String ?: "",
                    placa = data["placa"] as? String ?: "",
                    modelo = data["modelo"] as? String ?: "",
                    marca = data["marca"] as? String ?: "",
                    anoModelo = (data["ano"] as? Number)?.toInt() ?: (data["anoModelo"] as? Number)?.toInt() ?: 0,
                    kmAtual = (data["kmAtual"] as? Number)?.toLong() ?: 0L,
                    dataCompra = converterTimestampParaDate(data["dataCompra"] ?: data["dataUltimaRevisao"]),
                    observacoes = data["observacoes"] as? String
                )
                
                val local = appRepository.obterVeiculoPorId(id)
                if (local == null) appRepository.inserirVeiculo(veiculo)
                else appRepository.atualizarVeiculo(veiculo)
                count++
            }
            
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pullManutencao(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_MANUTENCAO
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
                
                val historico = HistoricoManutencaoVeiculo(
                    id = id,
                    veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: 0L,
                    tipoManutencao = data["tipoManutencao"] as? String ?: "Manutenção",
                    descricao = data["descricao"] as? String ?: "",
                    dataManutencao = converterTimestampParaDate(data["dataManutencao"]) ?: Date(),
                    valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                    kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: (data["kmNoMomento"] as? Number)?.toLong() ?: 0L,
                    observacoes = data["observacoes"] as? String
                )
                
                val local = appRepository.obterHistoricoManutencaoVeiculoPorId(id)
                if (local == null) appRepository.inserirHistoricoManutencao(historico)
                else appRepository.atualizarHistoricoManutencaoVeiculo(historico)
                count++
            }
            
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pullCombustivel(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_COMBUSTIVEL
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
                
                val historico = HistoricoCombustivelVeiculo(
                    id = id,
                    veiculoId = (data["veiculoId"] as? Number)?.toLong() ?: 0L,
                    dataAbastecimento = converterTimestampParaDate(data["dataAbastecimento"]) ?: Date(),
                    litros = (data["litros"] as? Number)?.toDouble() ?: 0.0,
                    valor = (data["valor"] as? Number)?.toDouble() ?: 0.0,
                    kmVeiculo = (data["kmVeiculo"] as? Number)?.toLong() ?: (data["kmNoMomento"] as? Number)?.toLong() ?: 0L,
                    kmRodado = (data["kmRodado"] as? Number)?.toDouble() ?: 0.0,
                    posto = data["posto"] as? String ?: "",
                    observacoes = data["observacoes"] as? String
                )
                
                val local = appRepository.obterHistoricoCombustivelVeiculoPorId(id)
                if (local == null) appRepository.inserirHistoricoCombustivel(historico)
                else appRepository.atualizarHistoricoCombustivelVeiculo(historico)
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
        pushVeiculos().onSuccess { totalCount += it }
        pushManutencao().onSuccess { totalCount += it }
        pushCombustivel().onSuccess { totalCount += it }
        return Result.success(totalCount)
    }

    private suspend fun pushVeiculos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_VEICULOS
        return try {
            val lastPush = getLastPushTimestamp(type)
            val locais = appRepository.obterTodosVeiculos().first()
                .filter { (it.dataCompra?.time ?: 0L) > lastPush }
            
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

    private suspend fun pushManutencao(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_MANUTENCAO
        return try {
            val lastPush = getLastPushTimestamp(type)
            val locais = appRepository.obterTodosHistoricoManutencaoVeiculo()
                // HistoricoManutencaoVeiculo doesn't have dataAtualizacao in the provided snippets, using dataManutencao as fallback
                .filter { it.dataManutencao.time > lastPush }
            
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

    private suspend fun pushCombustivel(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_COMBUSTIVEL
        return try {
            val lastPush = getLastPushTimestamp(type)
            val todasMatenutencoes = appRepository.obterTodosHistoricoCombustivelVeiculo()
            val filtrados = todasMatenutencoes.filter { it.dataAbastecimento.time > lastPush }
            
            if (filtrados.isEmpty()) return Result.success(0)
            
            val collectionRef = getCollectionReference(type)
            var count = 0
            for (item in filtrados) {
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
}
