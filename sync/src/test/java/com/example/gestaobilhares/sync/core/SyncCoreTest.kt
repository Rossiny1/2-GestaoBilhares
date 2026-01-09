package com.example.gestaobilhares.sync.core

import com.example.gestaobilhares.data.entities.SyncMetadata
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.check

class SyncCoreTest {

    @Mock
    private lateinit var syncMetadataDao: SyncMetadataDao

    @Mock
    private lateinit var userSessionManager: UserSessionManager

    @Mock
    private lateinit var appRepository: AppRepository

    private lateinit var syncCore: SyncCore

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        syncCore = SyncCore(syncMetadataDao, userSessionManager, appRepository)
        
        // Setup default user session
        whenever(userSessionManager.getCurrentUserId()).thenReturn(123L)
        whenever(userSessionManager.isLoggedIn).thenReturn(MutableStateFlow(true))
    }

    @Test
    fun `getLastSyncTimestamp should return timestamp from metadata`() = runTest {
        // Given
        val entityType = "acertos"
        val expectedTimestamp = System.currentTimeMillis() - 3600000L // 1 hour ago
        whenever(syncMetadataDao.obterUltimoTimestamp(entityType, 123L))
            .thenReturn(expectedTimestamp)

        // When
        val result = syncCore.getLastSyncTimestamp(entityType)

        // Then
        assertThat(result).isEqualTo(expectedTimestamp)
        verify(syncMetadataDao).obterUltimoTimestamp(entityType, 123L)
    }

    @Test
    fun `getLastSyncTimestamp should return 0 when no metadata exists`() = runTest {
        // Given
        val entityType = "acertos"
        whenever(syncMetadataDao.obterUltimoTimestamp(entityType, 123L))
            .thenReturn(null)

        // When
        val result = syncCore.getLastSyncTimestamp(entityType)

        // Then
        assertThat(result).isEqualTo(0L)
        verify(syncMetadataDao).obterUltimoTimestamp(entityType, 123L)
    }

    @Test
    fun `saveSyncMetadata should insert new metadata when none exists`() = runTest {
        // Given
        val entityType = "acertos"
        val syncCount = 15
        val durationMs = 8000L
        val bytesTransferred = 2048L
        
        whenever(syncMetadataDao.obterUltimoTimestamp(entityType, 123L))
            .thenReturn(null)

        // When
        syncCore.saveSyncMetadata(entityType, syncCount, durationMs, bytesTransferred)

        // Then
        verify(syncMetadataDao).obterUltimoTimestamp(entityType, 123L)
        verify(syncMetadataDao).inserirOuAtualizar(check {
            assertThat(it.entityType).isEqualTo(entityType)
            assertThat(it.userId).isEqualTo(123L)
            assertThat(it.lastSyncCount).isEqualTo(syncCount)
            assertThat(it.lastSyncDurationMs).isEqualTo(durationMs)
            assertThat(it.lastSyncBytesDownloaded).isEqualTo(bytesTransferred)
            assertThat(it.lastError).isNull()
        })
    }

    @Test
    fun `saveSyncMetadata should update existing metadata`() = runTest {
        // Given
        val entityType = "acertos"
        val syncCount = 20
        val durationMs = 10000L
        val bytesTransferred = 4096L
        val error = "Network timeout"
        
        val existingTimestamp = System.currentTimeMillis() - 3600000L
        
        whenever(syncMetadataDao.obterUltimoTimestamp(entityType, 123L))
            .thenReturn(existingTimestamp)

        // When
        syncCore.saveSyncMetadata(entityType, syncCount, durationMs, bytesTransferred, error)

        // Then
        verify(syncMetadataDao).obterUltimoTimestamp(entityType, 123L)
        verify(syncMetadataDao).inserirOuAtualizar(check {
            assertThat(it.entityType).isEqualTo(entityType)
            assertThat(it.userId).isEqualTo(123L)
            assertThat(it.lastSyncCount).isEqualTo(syncCount)
            assertThat(it.lastSyncDurationMs).isEqualTo(durationMs)
            assertThat(it.lastSyncBytesDownloaded).isEqualTo(bytesTransferred)
            assertThat(it.lastError).isEqualTo(error)
        })
    }

    @Test
    fun `getAccessibleRouteIds should return user's accessible routes`() = runTest {
        // Given
        val expectedRouteIds = listOf(1L, 2L, 3L)
        whenever(userSessionManager.getRotasPermitidas()).thenReturn(expectedRouteIds)

        // When
        val result = syncCore.getAccessibleRouteIds()

        // Then
        assertThat(result).isEqualTo(expectedRouteIds)
        verify(userSessionManager).getRotasPermitidas()
    }

    @Test
    fun `shouldSyncRouteData should return true when user can access route`() = runTest {
        // Given
        val rotaId = 1L
        val clienteId = 100L
        whenever(userSessionManager.canAccessRota(rotaId)).thenReturn(true)

        // When
        val result = syncCore.shouldSyncRouteData(rotaId, clienteId)

        // Then
        assertThat(result).isTrue()
        verify(userSessionManager).canAccessRota(rotaId)
    }

    @Test
    fun `shouldSyncRouteData should return false when user cannot access route`() = runTest {
        // Given
        val rotaId = 999L
        val clienteId = 100L
        whenever(userSessionManager.canAccessRota(rotaId)).thenReturn(false)

        // When
        val result = syncCore.shouldSyncRouteData(rotaId, clienteId)

        // Then
        assertThat(result).isFalse()
        verify(userSessionManager).canAccessRota(rotaId)
    }

    @Test
    fun `shouldSyncRouteData should return true when no specific route provided`() = runTest {
        // Given
        val rotaId = null
        val clienteId = 100L

        // When
        val result = syncCore.shouldSyncRouteData(rotaId, clienteId)

        // Then
        assertThat(result).isTrue()
        // Should not call canAccessRota when rotaId is null
    }
}
