package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.data.entities.TipoDespesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

/**
 * Handler especializado para sincronização de Finanças (Categorias e Tipos de Despesa).
 */
class FinancasSyncHandler @javax.inject.Inject constructor(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = "financas"

    companion object {
        private const val COLLECTION_CATEGORIAS = "categorias_despesa"
        private const val COLLECTION_TIPOS = "tipos_despesa"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        var totalCount = 0
        
        // 1. Pull Categorias
        pullCategorias().onSuccess { totalCount += it }
        
        // 2. Pull Tipos
        pullTipos().onSuccess { totalCount += it }
        
        return Result.success(totalCount)
    }

    private suspend fun pullCategorias(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = "categorias_despesa"
        return try {
            val collectionRef = getCollectionReference(COLLECTION_CATEGORIAS)
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
                
                val categoria = CategoriaDespesa(
                    id = id,
                    nome = data["nome"] as? String ?: "",
                    descricao = data["descricao"] as? String ?: "",
                    ativa = data["ativo"] as? Boolean ?: data["ativa"] as? Boolean ?: true,
                    dataAtualizacao = converterTimestampParaDate(data["dataAtualizacao"]) ?: Date()
                )
                
                val local = appRepository.obterCategoriaDespesaPorId(id)
                if (local == null) appRepository.inserirCategoriaDespesa(categoria)
                else appRepository.atualizarCategoriaDespesa(categoria)
                count++
            }
            
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro pull categorias: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun pullTipos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = "tipos_despesa"
        return try {
            val collectionRef = getCollectionReference(COLLECTION_TIPOS)
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
                
                val tipo = TipoDespesa(
                    id = id,
                    categoriaId = (data["categoriaId"] as? Number)?.toLong() ?: (data["categoriaDespesaId"] as? Number)?.toLong() ?: 0L,
                    nome = data["nome"] as? String ?: "",
                    descricao = data["descricao"] as? String ?: "",
                    ativo = data["ativo"] as? Boolean ?: true,
                    dataAtualizacao = converterTimestampParaDate(data["dataAtualizacao"]) ?: Date()
                )
                
                val local = appRepository.obterTipoDespesaPorId(id)
                if (local == null) appRepository.inserirTipoDespesa(tipo)
                else appRepository.atualizarTipoDespesa(tipo)
                count++
            }
            
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Timber.tag(TAG).e("Erro pull tipos: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun push(): Result<Int> {
        var totalCount = 0
        pushCategorias().onSuccess { totalCount += it }
        pushTipos().onSuccess { totalCount += it }
        return Result.success(totalCount)
    }

    private suspend fun pushCategorias(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = "categorias_despesa"
        return try {
            val lastPush = getLastPushTimestamp(type)
            val locais = appRepository.obterTodasCategoriasDespesa().first()
                .filter { (it.dataAtualizacao.time) > lastPush }
            
            if (locais.isEmpty()) return Result.success(0)
            
            val collectionRef = getCollectionReference(COLLECTION_CATEGORIAS)
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

    private suspend fun pushTipos(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = "tipos_despesa"
        return try {
            val lastPush = getLastPushTimestamp(type)
            val locais = appRepository.obterTodosTiposDespesa().first()
                .filter { it.dataAtualizacao.time > lastPush }
            
            if (locais.isEmpty()) return Result.success(0)
            
            val collectionRef = getCollectionReference(COLLECTION_TIPOS)
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
}
