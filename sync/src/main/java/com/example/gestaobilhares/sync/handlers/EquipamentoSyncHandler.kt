package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.entities.Equipment
import com.example.gestaobilhares.data.entities.StockItem
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.utils.FirebaseImageUploader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

/**
 * Handler especializado para sincronização de Equipamentos e Itens de Estoque.
 */
class EquipamentoSyncHandler @javax.inject.Inject constructor(
    context: Context,
    appRepository: AppRepository,
    firestore: FirebaseFirestore,
    networkUtils: NetworkUtils,
    userSessionManager: UserSessionManager,
    firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader,
    syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao? = null
) : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {

    override val entityType: String = "equipamentos"

    companion object {
        private const val COLLECTION_EQUIPMENTS = "equipments"
        private const val COLLECTION_STOCK_ITEMS = "stock_items"
    }

    override suspend fun pull(timestampOverride: Long?): Result<Int> {
        var totalCount = 0
        
        // 1. Pull Equipments
        pullEquipments().onSuccess { totalCount += it }
        
        // 2. Pull StockItems
        pullStockItems().onSuccess { totalCount += it }
        
        return Result.success(totalCount)
    }

    private suspend fun pullEquipments(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_EQUIPMENTS
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
                
                val equipment = Equipment(
                    id = id,
                    name = data["name"] as? String ?: data["nome"] as? String ?: "",
                    description = data["description"] as? String ?: data["descricao"] as? String,
                    quantity = (data["quantity"] as? Number)?.toInt() ?: (data["quantidade"] as? Number)?.toInt() ?: 0,
                    location = data["location"] as? String ?: data["localizacao"] as? String
                )
                
                val local = appRepository.obterEquipmentPorId(id)
                if (local == null) appRepository.inserirEquipment(equipment)
                else appRepository.atualizarEquipment(equipment)
                count++
            }
            
            saveSyncMetadata(type, count, System.currentTimeMillis() - startTime)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pullStockItems(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_STOCK_ITEMS
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
                
                val item = StockItem(
                    id = id,
                    name = data["name"] as? String ?: data["nome"] as? String ?: "",
                    category = data["category"] as? String ?: data["categoria"] as? String ?: "",
                    quantity = (data["quantity"] as? Number)?.toInt() ?: (data["quantidade"] as? Number)?.toInt() ?: 0,
                    unitPrice = (data["unitPrice"] as? Number)?.toDouble() ?: (data["precoUnitario"] as? Number)?.toDouble() ?: 0.0,
                    supplier = data["supplier"] as? String ?: "",
                    description = data["description"] as? String ?: data["descricao"] as? String,
                    updatedAt = converterTimestampParaDate(data["updatedAt"]) ?: converterTimestampParaDate(data["dataAtualizacao"]) ?: Date()
                )
                
                val local = appRepository.obterStockItemPorId(id)
                if (local == null) appRepository.inserirStockItem(item)
                else appRepository.atualizarStockItem(item)
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
        pushEquipments().onSuccess { totalCount += it }
        pushStockItems().onSuccess { totalCount += it }
        return Result.success(totalCount)
    }

    private suspend fun pushEquipments(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_EQUIPMENTS
        return try {
            val lastPush = getLastPushTimestamp(type)
            val locais = appRepository.obterTodosEquipments().first()
                // Equipment has no dataAtualizacao, so we push all or use lastPush differently
                // For now, let's just push all if they were modified (placeholder logic)
            
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

    private suspend fun pushStockItems(): Result<Int> {
        val startTime = System.currentTimeMillis()
        val type = COLLECTION_STOCK_ITEMS
        return try {
            val lastPush = getLastPushTimestamp(type)
            val locais = appRepository.obterTodosStockItems().first()
                .filter { it.updatedAt.time > lastPush }
            
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
}
