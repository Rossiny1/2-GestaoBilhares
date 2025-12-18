package com.example.gestaobilhares.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.SyncRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
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
    fun doWork_whenSyncSucceeds_shouldReturnSuccess() = runBlocking {
        // Arrange
        whenever(syncRepository.shouldRunBackgroundSync(0, 6L)).thenReturn(true)
        whenever(syncRepository.processSyncQueue()).thenReturn(Result.success(Unit))
        whenever(syncRepository.syncBidirectional()).thenReturn(Result.success(Unit))
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return SyncWorker(appContext, workerParameters, appRepository, syncRepository)
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun doWork_whenShouldNotSync_shouldReturnSuccess() = runBlocking {
        // Arrange
        whenever(syncRepository.shouldRunBackgroundSync(0, 6L)).thenReturn(false)
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return SyncWorker(appContext, workerParameters, appRepository, syncRepository)
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun doWork_whenSyncFails_shouldReturnRetry() = runBlocking {
        // Arrange
        whenever(syncRepository.shouldRunBackgroundSync(0, 6L)).thenReturn(true)
        whenever(syncRepository.processSyncQueue()).thenReturn(Result.failure(Exception("Network error")))
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return SyncWorker(appContext, workerParameters, appRepository, syncRepository)
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }
}
