package com.example.gestaobilhares.sync.orchestration

import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.core.SyncCore
import com.example.gestaobilhares.sync.handlers.*
import com.example.gestaobilhares.sync.SyncResult
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.never

class SyncOrchestrationTest {

    @Mock
    private lateinit var mesaSyncHandler: MesaSyncHandler

    @Mock
    private lateinit var clienteSyncHandler: ClienteSyncHandler

    @Mock
    private lateinit var contratoSyncHandler: ContratoSyncHandler

    @Mock
    private lateinit var acertoSyncHandler: AcertoSyncHandler

    @Mock
    private lateinit var despesaSyncHandler: DespesaSyncHandler

    @Mock
    private lateinit var rotaSyncHandler: RotaSyncHandler

    @Mock
    private lateinit var cicloSyncHandler: CicloSyncHandler

    @Mock
    private lateinit var colaboradorSyncHandler: ColaboradorSyncHandler

    @Mock
    private lateinit var colaboradorRotaSyncHandler: ColaboradorRotaSyncHandler

    @Mock
    private lateinit var metaColaboradorSyncHandler: MetaColaboradorSyncHandler

    @Mock
    private lateinit var metaSyncHandler: MetaSyncHandler

    @Mock
    private lateinit var assinaturaSyncHandler: AssinaturaSyncHandler

    @Mock
    private lateinit var veiculoSyncHandler: VeiculoSyncHandler

    @Mock
    private lateinit var equipamentoSyncHandler: EquipamentoSyncHandler

    @Mock
    private lateinit var estoqueSyncHandler: EstoqueSyncHandler

    @Mock
    private lateinit var syncCore: SyncCore

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var firestore: FirebaseFirestore

    private lateinit var syncOrchestration: SyncOrchestration

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        syncOrchestration = SyncOrchestration(
            mesaSyncHandler,
            clienteSyncHandler,
            contratoSyncHandler,
            acertoSyncHandler,
            despesaSyncHandler,
            rotaSyncHandler,
            cicloSyncHandler,
            colaboradorSyncHandler,
            colaboradorRotaSyncHandler,
            metaColaboradorSyncHandler,
            metaSyncHandler,
            assinaturaSyncHandler,
            veiculoSyncHandler,
            equipamentoSyncHandler,
            estoqueSyncHandler,
            syncCore,
            appRepository,
            firestore
        )
    }

    @Test
    fun `syncAllEntities should call all handlers and return success`() = runTest {
        // Given - Setup all handlers to return success
        whenever(mesaSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(mesaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(clienteSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(clienteSyncHandler.push()).thenReturn(Result.success(3))
        whenever(contratoSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(contratoSyncHandler.push()).thenReturn(Result.success(3))
        whenever(acertoSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(acertoSyncHandler.push()).thenReturn(Result.success(3))
        whenever(despesaSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(despesaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(rotaSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(rotaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(cicloSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(cicloSyncHandler.push()).thenReturn(Result.success(3))
        whenever(colaboradorSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(colaboradorSyncHandler.push()).thenReturn(Result.success(3))
        whenever(colaboradorRotaSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(colaboradorRotaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(metaColaboradorSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(metaColaboradorSyncHandler.push()).thenReturn(Result.success(3))
        whenever(metaSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(metaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(assinaturaSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(assinaturaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(veiculoSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(veiculoSyncHandler.push()).thenReturn(Result.success(3))
        whenever(equipamentoSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(equipamentoSyncHandler.push()).thenReturn(Result.success(3))
        whenever(estoqueSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(estoqueSyncHandler.push()).thenReturn(Result.success(3))

        // When
        val result = try {
            syncOrchestration.syncAll()
        } catch (e: Exception) {
            println("DEBUG: Exception caught: ${e.message}")
            throw e
        }

        // Then
        println("DEBUG: result.success=${result.success}, syncedCount=${result.syncedCount}, errors=${result.errors}")
        println("DEBUG: errors.size=${result.errors.size}")
        result.errors.forEachIndexed { index, error ->
            println("DEBUG: error[$index]=$error")
        }
        assertThat(result.success).isTrue()
        assertThat(result.syncedCount).isEqualTo(120) // 5 pull + 3 push per handler * 15 handlers = 120
        assertThat(result.errors).isEmpty()

        // Verify all handlers were called
        val allHandlers = listOf(
            mesaSyncHandler, clienteSyncHandler, contratoSyncHandler, acertoSyncHandler,
            despesaSyncHandler, rotaSyncHandler, cicloSyncHandler, colaboradorSyncHandler,
            colaboradorRotaSyncHandler, metaColaboradorSyncHandler, metaSyncHandler,
            assinaturaSyncHandler, veiculoSyncHandler, equipamentoSyncHandler, estoqueSyncHandler
        )
        allHandlers.forEach { handler ->
            verify(handler).pull(null)
            verify(handler).push()
        }
    }

    @Test
    fun `syncAllEntities should handle partial failures gracefully`() = runTest {
        // Given
        whenever(mesaSyncHandler.pull(null)).thenReturn(Result.success(5))
        whenever(mesaSyncHandler.push()).thenReturn(Result.success(3))
        
        whenever(clienteSyncHandler.pull(null)).thenReturn(Result.failure(Exception("Network error")))
        whenever(clienteSyncHandler.push()).thenReturn(Result.success(2))
        
        whenever(acertoSyncHandler.pull(null)).thenReturn(Result.success(8))
        whenever(acertoSyncHandler.push()).thenReturn(Result.failure(Exception("Firebase timeout")))

        // Other handlers succeed
        val otherHandlers = listOf(
            contratoSyncHandler, despesaSyncHandler, rotaSyncHandler, cicloSyncHandler,
            colaboradorSyncHandler, colaboradorRotaSyncHandler, metaColaboradorSyncHandler,
            metaSyncHandler, assinaturaSyncHandler, veiculoSyncHandler,
            equipamentoSyncHandler, estoqueSyncHandler
        )
        otherHandlers.forEach { handler ->
            whenever(handler.pull(null)).thenReturn(Result.success(4))
            whenever(handler.push()).thenReturn(Result.success(2))
        }

        // When
        val result = syncOrchestration.syncAll()

        // Then
        assertThat(result.success).isFalse()
        assertThat(result.syncedCount).isEqualTo(84) // mesa:8 + cliente:2 + acerto:8 + 11*6 = 8+2+8+66 = 84
        assertThat(result.errors).hasSize(2)
        assertThat(result.errors).contains("Network error")
        assertThat(result.errors).contains("Firebase timeout")
    }

    @Test
    fun `pushAllEntities should call push on all handlers`() = runTest {
        // Given
        whenever(mesaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(clienteSyncHandler.push()).thenReturn(Result.success(3))
        whenever(contratoSyncHandler.push()).thenReturn(Result.success(3))
        whenever(acertoSyncHandler.push()).thenReturn(Result.success(3))
        whenever(despesaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(rotaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(cicloSyncHandler.push()).thenReturn(Result.success(3))
        whenever(colaboradorSyncHandler.push()).thenReturn(Result.success(3))
        whenever(colaboradorRotaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(metaColaboradorSyncHandler.push()).thenReturn(Result.success(3))
        whenever(metaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(assinaturaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(veiculoSyncHandler.push()).thenReturn(Result.success(3))
        whenever(equipamentoSyncHandler.push()).thenReturn(Result.success(3))
        whenever(estoqueSyncHandler.push()).thenReturn(Result.success(3))

        // When
        val result = syncOrchestration.pushAll()

        // Then
        assertThat(result.success).isTrue()
        assertThat(result.syncedCount).isEqualTo(45) // 3 push per handler * 15 handlers
        assertThat(result.errors).isEmpty()

        // Verify all handlers were called for push only
        val allHandlers = listOf(
            mesaSyncHandler, clienteSyncHandler, contratoSyncHandler, acertoSyncHandler,
            despesaSyncHandler, rotaSyncHandler, cicloSyncHandler, colaboradorSyncHandler,
            colaboradorRotaSyncHandler, metaColaboradorSyncHandler, metaSyncHandler,
            assinaturaSyncHandler, veiculoSyncHandler, equipamentoSyncHandler, estoqueSyncHandler
        )
        allHandlers.forEach { handler ->
            verify(handler).push()
            verify(handler, never()).pull()
        }
    }

    @Test
    fun `pushAllEntities should handle push failures`() = runTest {
        // Given
        whenever(mesaSyncHandler.push()).thenReturn(Result.success(3))
        whenever(clienteSyncHandler.push()).thenReturn(Result.failure(Exception("Push failed")))
        whenever(acertoSyncHandler.push()).thenReturn(Result.success(5))

        // Other handlers succeed
        val otherHandlers = listOf(
            contratoSyncHandler, despesaSyncHandler, rotaSyncHandler, cicloSyncHandler,
            colaboradorSyncHandler, colaboradorRotaSyncHandler, metaColaboradorSyncHandler,
            metaSyncHandler, assinaturaSyncHandler, veiculoSyncHandler,
            equipamentoSyncHandler, estoqueSyncHandler
        )
        otherHandlers.forEach { handler ->
            whenever(handler.push()).thenReturn(Result.success(2))
        }

        // When
        val result = syncOrchestration.pushAll()

        // Then
        assertThat(result.success).isFalse()
        assertThat(result.syncedCount).isEqualTo(30) // mesa:3 + acerto:5 + 11*2 = 3+5+22 = 30
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors).contains("Push failed")
    }

    @Test
    fun `error handling should not stop other handlers`() = runTest {
        // Given - First handler fails, others succeed
        whenever(mesaSyncHandler.pull(null)).thenReturn(Result.failure(Exception("First error")))
        whenever(mesaSyncHandler.push()).thenReturn(Result.failure(Exception("Push error")))
        
        val otherHandlers = listOf(
            clienteSyncHandler, contratoSyncHandler, acertoSyncHandler, despesaSyncHandler,
            rotaSyncHandler, cicloSyncHandler, colaboradorSyncHandler, colaboradorRotaSyncHandler,
            metaColaboradorSyncHandler, metaSyncHandler, assinaturaSyncHandler, veiculoSyncHandler,
            equipamentoSyncHandler, estoqueSyncHandler
        )
        otherHandlers.forEach { handler ->
            whenever(handler.pull(null)).thenReturn(Result.success(4))
            whenever(handler.push()).thenReturn(Result.success(2))
        }

        // When
        val result = syncOrchestration.syncAll()

        // Then
        assertThat(result.success).isFalse() // Overall fails due to errors
        assertThat(result.syncedCount).isEqualTo(84) // 14 handlers * (4 pull + 2 push) = 84
        assertThat(result.errors).hasSize(2)
        assertThat(result.errors).contains("First error")
        assertThat(result.errors).contains("Push error")

        // Verify all handlers were called despite errors
        verify(mesaSyncHandler).pull(null)
        verify(mesaSyncHandler).push()
        otherHandlers.forEach { handler ->
            verify(handler).pull(null)
            verify(handler).push()
        }
    }
}
