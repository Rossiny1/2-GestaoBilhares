package com.example.gestaobilhares.sync

import android.content.Context
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.sync.utils.SyncUtils
import com.example.gestaobilhares.sync.core.SyncCore
import com.example.gestaobilhares.sync.orchestration.SyncOrchestration
import com.example.gestaobilhares.sync.SyncResult
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.check
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.Mockito.mock

class SyncRepositoryTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var networkUtils: NetworkUtils

    @Mock
    private lateinit var userSessionManager: UserSessionManager

    @Mock
    private lateinit var firebaseImageUploader: FirebaseImageUploader

    @Mock
    private lateinit var syncUtils: SyncUtils

    @Mock
    private lateinit var syncCore: SyncCore

    @Mock
    private lateinit var syncOrchestration: SyncOrchestration

    @Mock
    private lateinit var firestore: FirebaseFirestore

    private lateinit var syncRepository: SyncRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        syncRepository = SyncRepository(
            context,
            appRepository,
            networkUtils,
            userSessionManager,
            firebaseImageUploader,
            syncUtils,
            syncCore,
            syncOrchestration
        )
    }

    @Test
    fun `syncAllEntities should delegate to SyncOrchestration`() = runTest {
        // Given
        val expectedResult = SyncResult(
            success = true,
            syncedCount = 50,
            durationMs = 5000L,
            errors = emptyList()
        )
        val orchestrationResult = SyncOrchestration.SyncResult(
            success = true,
            syncedCount = 50,
            durationMs = 5000L,
            errors = emptyList()
        )
        whenever(syncOrchestration.syncAll()).thenReturn(orchestrationResult)

        // When
        val result = syncRepository.syncAllEntities()

        // Then
        assertThat(result).isEqualTo(expectedResult)
        verify(syncOrchestration).syncAll()
    }

    @Test
    fun `pushAllEntities should delegate to SyncOrchestration`() = runTest {
        // Given
        val expectedResult = SyncResult(
            success = true,
            syncedCount = 25,
            durationMs = 3000L,
            errors = emptyList()
        )
        val orchestrationResult = SyncOrchestration.SyncResult(
            success = true,
            syncedCount = 25,
            durationMs = 3000L,
            errors = emptyList()
        )
        whenever(syncOrchestration.pushAll()).thenReturn(orchestrationResult)

        // When
        val result = syncRepository.pushAllEntities()

        // Then
        assertThat(result).isEqualTo(expectedResult)
        verify(syncOrchestration).pushAll()
    }


    @Test
    fun `isOnline should delegate to NetworkUtils`() {
        // Given
        whenever(networkUtils.isConnected()).thenReturn(true)

        // When
        val result = syncRepository.isOnline()

        // Then
        assertThat(result).isTrue()
        verify(networkUtils).isConnected()
    }

    @Test
    fun `isOnline should return false on NetworkUtils exception`() {
        // Given
        whenever(networkUtils.isConnected()).thenThrow(RuntimeException("Network error"))

        // When
        val result = syncRepository.isOnline()

        // Then
        assertThat(result).isFalse()
        verify(networkUtils).isConnected()
    }

    @Test
    fun `getLastSyncTimestamp should delegate to SyncCore`() = runTest {
        // Given
        val entityType = "acertos"
        val expectedTimestamp = System.currentTimeMillis() - 3600000L
        whenever(syncCore.getLastSyncTimestamp(entityType)).thenReturn(expectedTimestamp)

        // When
        val result = syncRepository.getLastSyncTimestamp(entityType)

        // Then
        assertThat(result).isEqualTo(expectedTimestamp)
        verify(syncCore).getLastSyncTimestamp(entityType)
    }

    
    @Test
    fun `getAccessibleRouteIds should delegate to SyncCore`() = runTest {
        // Given
        val expectedRouteIds = setOf(1L, 2L, 3L)
        whenever(syncCore.getAccessibleRouteIds()).thenReturn(expectedRouteIds)

        // When
        val result = syncRepository.getAccessibleRouteIds()

        // Then
        assertThat(result).isEqualTo(expectedRouteIds)
        verify(syncCore).getAccessibleRouteIds()
    }

    @Test
    fun `shouldSyncRouteData should delegate to SyncCore`() = runTest {
        // Given
        val rotaId = 1L
        val clienteId = 100L
        whenever(syncCore.shouldSyncRouteData(rotaId, clienteId)).thenReturn(true)

        // When
        val result = syncRepository.shouldSyncRouteData(rotaId, clienteId)

        // Then
        assertThat(result).isTrue()
        verify(syncCore).shouldSyncRouteData(rotaId, clienteId)
    }

    @Test
    fun `hasPendingBackgroundSync should return false by default`() {
        // When
        val result = syncRepository.hasPendingBackgroundSync()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `entityToMap should delegate to SyncUtils`() {
        // Given
        val testData: MutableMap<String, Any> = mutableMapOf("id" to 1L, "name" to "test")
        whenever(syncUtils.entityToMap(any<MutableMap<String, Any>>())).thenReturn(testData)

        // When
        val result = syncRepository.entityToMap(testData)

        // Then
        assertThat(result).isEqualTo(testData)
        verify(syncUtils).entityToMap(testData)
    }

    @Test
    fun `documentToAcerto should delegate to SyncUtils`() {
        // Given
        val mockDocument = mock(DocumentSnapshot::class.java)
        whenever(syncUtils.documentToAcerto(any())).thenReturn(null)

        // When
        val result = syncRepository.documentToAcerto(mockDocument)

        // Then
        assertThat(result).isNull()
        verify(syncUtils).documentToAcerto(any())
    }

    @Test
    fun `saveSyncMetadata should delegate to SyncCore`() = runTest {
        // Given
        val entityType = "acertos"
        val syncCount = 10
        val durationMs = 5000L

        // When
        syncRepository.saveSyncMetadata(entityType, syncCount, durationMs)

        // Then
        verify(syncCore).saveSyncMetadata(eq(entityType), eq(syncCount), eq(durationMs), isNull(), isNull())
    }

    @Test
    fun `saveSyncMetadata with error should delegate to SyncCore`() = runTest {
        // Given
        val entityType = "acertos"
        val syncCount = 10
        val durationMs = 5000L
        val error = "Network error"

        // When
        syncRepository.saveSyncMetadata(entityType, syncCount, durationMs, error)

        // Then
        verify(syncCore).saveSyncMetadata(eq(entityType), eq(syncCount), eq(durationMs), isNull(), eq(error))
    }

    @Test
    fun `savePushMetadata should delegate to SyncCore`() = runTest {
        // Given
        val entityType = "acertos"
        val pushCount = 5
        val durationMs = 3000L

        // When
        syncRepository.savePushMetadata(entityType, pushCount, durationMs)

        // Then
        verify(syncCore).savePushMetadata(eq(entityType), eq(pushCount), eq(durationMs), isNull(), isNull())
    }

    @Test
    fun `savePushMetadata with error should delegate to SyncCore`() = runTest {
        // Given
        val entityType = "acertos"
        val pushCount = 5
        val durationMs = 3000L
        val error = "Upload error"

        // When
        syncRepository.savePushMetadata(entityType, pushCount, durationMs, error)

        // Then
        verify(syncCore).savePushMetadata(eq(entityType), eq(pushCount), eq(durationMs), isNull(), eq(error))
    }

    @Test
    fun `getSyncSummary should return summary with current timestamp`() = runTest {
        // When
        val result = syncRepository.getSyncSummary()

        // Then
        assertThat(result.lastSyncTime).isGreaterThan(0L)
    }

    }
