package com.example.gestaobilhares.sync.orchestration

import com.example.gestaobilhares.sync.core.SyncCore
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

/**
 * SyncCore fake para testes
 */
class FakeSyncCore(
    private val userSessionManager: UserSessionManager,
    private val appRepository: AppRepository,
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {
    suspend fun saveSyncMetadata(
        entityType: String,
        syncCount: Int,
        durationMs: Long,
        bytesTransferred: Long? = null,
        error: String? = null
    ) {
        // Não faz nada nos testes
    }
}
