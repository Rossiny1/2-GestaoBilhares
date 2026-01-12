package com.example.gestaobilhares.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.SyncRepository
import com.example.gestaobilhares.sync.SyncResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {

    private lateinit var context: Context
    
    @Mock
    private lateinit var appRepository: AppRepository
    
    @Mock
    private lateinit var syncRepository: SyncRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `doWork when sync succeeds should return success`() = runTest {
        // Arrange
        whenever(syncRepository.hasPendingBackgroundSync()).thenReturn(true)
        whenever(syncRepository.isOnline()).thenReturn(true)
        whenever(syncRepository.syncAllEntities()).thenReturn(
            SyncResult(success = true, syncedCount = 50, errors = emptyList(), durationMs = 5000L)
        )
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return if (workerClassName == SyncWorker::class.java.name) {
                        SyncWorker(
                            appContext,
                            workerParameters,
                            appRepository,
                            syncRepository
                        )
                    } else {
                        null
                    }
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork when no pending sync should return success`() = runTest {
        // Arrange
        whenever(syncRepository.hasPendingBackgroundSync()).thenReturn(false)
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return if (workerClassName == SyncWorker::class.java.name) {
                        SyncWorker(
                            appContext,
                            workerParameters,
                            appRepository,
                            syncRepository
                        )
                    } else {
                        null
                    }
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork when offline should return retry`() = runTest {
        // Arrange
        whenever(syncRepository.hasPendingBackgroundSync()).thenReturn(true)
        whenever(syncRepository.isOnline()).thenReturn(false)
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return if (workerClassName == SyncWorker::class.java.name) {
                        SyncWorker(
                            appContext,
                            workerParameters,
                            appRepository,
                            syncRepository
                        )
                    } else {
                        null
                    }
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `doWork when sync fails should return retry`() = runTest {
        // Arrange
        whenever(syncRepository.hasPendingBackgroundSync()).thenReturn(true)
        whenever(syncRepository.isOnline()).thenReturn(true)
        whenever(syncRepository.syncAllEntities()).thenReturn(
            SyncResult(success = false, syncedCount = 0, errors = listOf("Network error"), durationMs = 3000L)
        )
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return if (workerClassName == SyncWorker::class.java.name) {
                        SyncWorker(
                            appContext,
                            workerParameters,
                            appRepository,
                            syncRepository
                        )
                    } else {
                        null
                    }
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `doWork when exception occurs should return failure`() = runTest {
        // Arrange
        whenever(syncRepository.hasPendingBackgroundSync()).thenReturn(true)
        whenever(syncRepository.isOnline()).thenThrow(RuntimeException("Network error"))
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return if (workerClassName == SyncWorker::class.java.name) {
                        SyncWorker(
                            appContext,
                            workerParameters,
                            appRepository,
                            syncRepository
                        )
                    } else {
                        null
                    }
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `doWork should check connectivity before attempting sync`() = runTest {
        // Arrange
        whenever(syncRepository.hasPendingBackgroundSync()).thenReturn(true)
        whenever(syncRepository.isOnline()).thenReturn(true)
        whenever(syncRepository.syncAllEntities()).thenReturn(
            SyncResult(success = true, syncedCount = 10, errors = emptyList(), durationMs = 2000L)
        )
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return if (workerClassName == SyncWorker::class.java.name) {
                        SyncWorker(
                            appContext,
                            workerParameters,
                            appRepository,
                            syncRepository
                        )
                    } else {
                        null
                    }
                }
            })
            .build()

        // Act
        worker.doWork()

        // Assert
        // Verify that isOnline was called before syncAllEntities
        org.mockito.kotlin.verify(syncRepository).hasPendingBackgroundSync()
        org.mockito.kotlin.verify(syncRepository).isOnline()
        org.mockito.kotlin.verify(syncRepository).syncAllEntities()
    }

    @Test
    fun `doWork should not attempt sync when offline`() = runTest {
        // Arrange
        whenever(syncRepository.hasPendingBackgroundSync()).thenReturn(true)
        whenever(syncRepository.isOnline()).thenReturn(false)
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return if (workerClassName == SyncWorker::class.java.name) {
                        SyncWorker(
                            appContext,
                            workerParameters,
                            appRepository,
                            syncRepository
                        )
                    } else {
                        null
                    }
                }
            })
            .build()

        // Act
        worker.doWork()

        // Assert
        // Verify that syncAllEntities was NOT called when offline
        org.mockito.kotlin.verify(syncRepository).hasPendingBackgroundSync()
        org.mockito.kotlin.verify(syncRepository).isOnline()
        org.mockito.kotlin.verify(syncRepository, org.mockito.kotlin.never()).syncAllEntities()
    }
}
