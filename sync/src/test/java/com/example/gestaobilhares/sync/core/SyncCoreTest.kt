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

        // When
        syncCore.saveSyncMetadata(entityType, syncCount, durationMs, bytesTransferred)

        // Then
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
        val entityType = "clientes"
        val syncCount = 8
        val durationMs = 4000L
        val error = "Network timeout"

        // When
        syncCore.saveSyncMetadata(entityType, syncCount, durationMs, null, error)

        // Then
        verify(syncMetadataDao).inserirOuAtualizar(check {
            assertThat(it.entityType).isEqualTo(entityType)
            assertThat(it.userId).isEqualTo(123L)
            assertThat(it.lastSyncCount).isEqualTo(syncCount)
            assertThat(it.lastSyncDurationMs).isEqualTo(durationMs)
            assertThat(it.lastSyncBytesDownloaded).isEqualTo(0L)
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
        assertThat(result).isEqualTo(expectedRouteIds.toSet())
        verify(userSessionManager).getRotasPermitidas()
    }

    @Test
    fun `shouldSyncRouteData should return true when user can access route`() = runTest {
        // Given
        val rotaId = 1L
        val clienteId = 100L
        // O método canAccessRoute é interno ao SyncCore, não precisa de mock
        // Vamos mockar o UserSessionManager para retornar que tem acesso
        whenever(userSessionManager.isAdmin()).thenReturn(false)
        whenever(userSessionManager.getRotasPermitidas()).thenReturn(listOf(rotaId))

        // When
        val result = syncCore.shouldSyncRouteData(rotaId, clienteId)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `shouldSyncRouteData should return false when user cannot access route`() = runTest {
        // Given
        val rotaId = 999L
        val clienteId = 100L
        // Mockar para não ter acesso à rota
        whenever(userSessionManager.isAdmin()).thenReturn(false)
        whenever(userSessionManager.getRotasPermitidas()).thenReturn(listOf(1L, 2L)) // sem a rota 999

        // When
        val result = syncCore.shouldSyncRouteData(rotaId, clienteId)

        // Then
        assertThat(result).isFalse()
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
