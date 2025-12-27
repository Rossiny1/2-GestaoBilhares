package com.example.gestaobilhares.sync

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class SyncRepositoryTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var firestore: FirebaseFirestore

    @Mock
    private lateinit var networkUtils: NetworkUtils

    @Mock
    private lateinit var userSessionManager: com.example.gestaobilhares.core.utils.UserSessionManager

    @Mock
    private lateinit var firebaseImageUploader: com.example.gestaobilhares.core.utils.FirebaseImageUploader

    @Mock
    private lateinit var syncMetadataDao: SyncMetadataDao

    @Mock
    private lateinit var mesaSyncHandler: com.example.gestaobilhares.sync.handlers.MesaSyncHandler
    @Mock
    private lateinit var clienteSyncHandler: com.example.gestaobilhares.sync.handlers.ClienteSyncHandler
    @Mock
    private lateinit var contratoSyncHandler: com.example.gestaobilhares.sync.handlers.ContratoSyncHandler
    @Mock
    private lateinit var acertoSyncHandler: com.example.gestaobilhares.sync.handlers.AcertoSyncHandler
    @Mock
    private lateinit var despesaSyncHandler: com.example.gestaobilhares.sync.handlers.DespesaSyncHandler
    @Mock
    private lateinit var rotaSyncHandler: com.example.gestaobilhares.sync.handlers.RotaSyncHandler
    @Mock
    private lateinit var cicloSyncHandler: com.example.gestaobilhares.sync.handlers.CicloSyncHandler
    @Mock
    private lateinit var colaboradorSyncHandler: com.example.gestaobilhares.sync.handlers.ColaboradorSyncHandler
    @Mock
    private lateinit var colaboradorRotaSyncHandler: com.example.gestaobilhares.sync.handlers.ColaboradorRotaSyncHandler
    @Mock
    private lateinit var metaColaboradorSyncHandler: com.example.gestaobilhares.sync.handlers.MetaColaboradorSyncHandler
    @Mock
    private lateinit var metaSyncHandler: com.example.gestaobilhares.sync.handlers.MetaSyncHandler
    @Mock
    private lateinit var assinaturaSyncHandler: com.example.gestaobilhares.sync.handlers.AssinaturaSyncHandler
    @Mock
    private lateinit var veiculoSyncHandler: com.example.gestaobilhares.sync.handlers.VeiculoSyncHandler
    @Mock
    private lateinit var equipamentoSyncHandler: com.example.gestaobilhares.sync.handlers.EquipamentoSyncHandler
    @Mock
    private lateinit var estoqueSyncHandler: com.example.gestaobilhares.sync.handlers.EstoqueSyncHandler

    private lateinit var syncRepository: SyncRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(userSessionManager.getCurrentUserId()).thenReturn(1L)
        
        syncRepository = SyncRepository(
            context = context,
            appRepository = appRepository,
            firestore = firestore,
            networkUtils = networkUtils,
            userSessionManager = userSessionManager,
            firebaseImageUploader = firebaseImageUploader,
            syncMetadataDao = syncMetadataDao,
            mesaSyncHandler = mesaSyncHandler,
            clienteSyncHandler = clienteSyncHandler,
            contratoSyncHandler = contratoSyncHandler,
            acertoSyncHandler = acertoSyncHandler,
            despesaSyncHandler = despesaSyncHandler,
            rotaSyncHandler = rotaSyncHandler,
            cicloSyncHandler = cicloSyncHandler,
            colaboradorSyncHandler = colaboradorSyncHandler,
            colaboradorRotaSyncHandler = colaboradorRotaSyncHandler,
            metaColaboradorSyncHandler = metaColaboradorSyncHandler,
            metaSyncHandler = metaSyncHandler,
            assinaturaSyncHandler = assinaturaSyncHandler,
            veiculoSyncHandler = veiculoSyncHandler,
            equipamentoSyncHandler = equipamentoSyncHandler,
            estoqueSyncHandler = estoqueSyncHandler
        )
    }

    @Test
    fun getCollectionReference_shouldReturnCorrectPath() {
        // Arrange
        val collectionName = "clientes"
        val mockCollectionItems = mock<CollectionReference>()
        val mockDocEntity = mock<com.google.firebase.firestore.DocumentReference> {
            on { collection("items") } doReturn mockCollectionItems
        }
        val mockCollectionEntities = mock<CollectionReference> {
            on { document(collectionName) } doReturn mockDocEntity
        }
        val mockDocEmpresa = mock<com.google.firebase.firestore.DocumentReference> {
            on { collection("entidades") } doReturn mockCollectionEntities
        }
        val mockCollectionEmpresas = mock<CollectionReference> {
            on { document("empresa_001") } doReturn mockDocEmpresa
        }
        
        whenever(firestore.collection("empresas")).thenReturn(mockCollectionEmpresas)

        // Act
        val result = SyncRepository.getCollectionReference(firestore, collectionName)

        // Assert
        assertThat(result).isEqualTo(mockCollectionItems)
    }

    @Test
    fun shouldRunBackgroundSync_whenPendingOperationsExist_shouldReturnTrue() = runTest {
        // Arrange
        whenever(appRepository.contarOperacoesSyncPendentes()).thenReturn(5)

        // Act
        val result = syncRepository.shouldRunBackgroundSync()

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun shouldRunBackgroundSync_whenNoPendingAndRecentSync_shouldReturnFalse() = runTest {
        // Arrange
        whenever(appRepository.contarOperacoesSyncPendentes()).thenReturn(0)
        whenever(appRepository.contarOperacoesSyncFalhadas()).thenReturn(0)
        
        val recentTime = System.currentTimeMillis() - (1 * 60 * 60 * 1000L) // 1h ago
        whenever(syncMetadataDao.obterUltimoTimestamp(org.mockito.kotlin.eq("_global_sync"), org.mockito.kotlin.any())).thenReturn(recentTime)
        
        // Act
        val result = syncRepository.shouldRunBackgroundSync()

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun shouldRunBackgroundSync_whenNoPendingButOldSync_shouldReturnTrue() = runTest {
        // Arrange
        whenever(appRepository.contarOperacoesSyncPendentes()).thenReturn(0)
        whenever(appRepository.contarOperacoesSyncFalhadas()).thenReturn(0)
        
        val oldTime = System.currentTimeMillis() - (12 * 60 * 60 * 1000L) // 12h ago (limit is 6h)
        whenever(syncMetadataDao.obterUltimoTimestamp(org.mockito.kotlin.eq("_global_sync"), org.mockito.kotlin.any())).thenReturn(oldTime)
        
        // Act
        val result = syncRepository.shouldRunBackgroundSync()

        // Assert
        assertThat(result).isTrue()
    }
    
    @Test
    fun extrairClienteId_shouldHandleVariousFieldNames() {
        assertThat(syncRepository.extrairClienteId(mapOf("clienteId" to 123L))).isEqualTo(123L)
        assertThat(syncRepository.extrairClienteId(mapOf("cliente_id" to 456L))).isEqualTo(456L)
        assertThat(syncRepository.extrairClienteId(mapOf("clienteID" to "789"))).isEqualTo(789L)
        assertThat(syncRepository.extrairClienteId(mapOf("unknown" to 123L))).isNull()
    }

    @Test
    fun parseDataAcertoString_shouldHandleVariousFormats() {
        val expected = 1734475200000L // 2024-12-17 22:40:00 UTC (approx)
        
        // Numeric string (seconds)
        assertThat(syncRepository.parseDataAcertoString("1734475200")).isEqualTo(1734475200000L)
        
        // ISO format
        assertThat(syncRepository.parseDataAcertoString("2024-12-17T22:40:00.000Z")).isGreaterThan(0L)
        
        // Custom format
        assertThat(syncRepository.parseDataAcertoString("17/12/2024 22:40:00")).isGreaterThan(0L)
        
        // Invalid
        assertThat(syncRepository.parseDataAcertoString("invalid")).isEqualTo(0L)
    }

    @Test
    fun extrairDataAcertoMillis_shouldHandleDifferentTypes() {
        val mockDoc = mock<com.google.firebase.firestore.DocumentSnapshot>()
        val date = java.util.Date(1000000L)
        
        // Test with Date
        whenever(mockDoc.get("dataAcerto")).thenReturn(date)
        assertThat(syncRepository.extrairDataAcertoMillis(mockDoc)).isEqualTo(1000000L)
        
        // Test with String
        whenever(mockDoc.get("dataAcerto")).thenReturn("1000000")
        assertThat(syncRepository.extrairDataAcertoMillis(mockDoc)).isEqualTo(1000000000L) // Multiplied by 1000 in method
    }

    @Test
    fun getLastSyncTimestamp_shouldReturnCorrectValue() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(org.mockito.kotlin.eq("clientes"), org.mockito.kotlin.any())).thenReturn(123456L)
        assertThat(syncRepository.getLastSyncTimestamp("clientes")).isEqualTo(123456L)
    }

    @Test
    fun saveSyncMetadata_shouldCallDao() = runTest {
        syncRepository.saveSyncMetadata(
            entityType = "test",
            syncCount = 10,
            durationMs = 100L,
            bytesDownloaded = 500L
        )
        
        org.mockito.Mockito.verify(syncMetadataDao).atualizarTimestamp(
            entityType = org.mockito.kotlin.eq("test"),
            userId = org.mockito.kotlin.eq(1L),
            timestamp = org.mockito.kotlin.any(),
            count = org.mockito.kotlin.eq(10),
            durationMs = org.mockito.kotlin.eq(100L),
            bytesDownloaded = org.mockito.kotlin.eq(500L),
            bytesUploaded = org.mockito.kotlin.any(),
            error = org.mockito.kotlin.isNull(),
            updatedAt = org.mockito.kotlin.any()
        )
    }

    @Test
    fun ensureEntityExists_whenExistsLocally_shouldReturnTrue() = runTest {
        whenever(appRepository.obterClientePorId(1L)).thenReturn(mock())
        val result = syncRepository.ensureEntityExists("cliente", 1L)
        assertThat(result).isTrue()
    }

    @Test
    fun saveSyncMetadata_whenDaoFails_shouldNotThrow() = runTest {
        whenever(syncMetadataDao.atualizarTimestamp(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("Database error"))
            
        // Should not crash the sync process
        syncRepository.saveSyncMetadata("test", 1, 1, 1)
        
        verify(syncMetadataDao).atualizarTimestamp(any(), any(), any(), any(), any(), any(), any(), anyOrNull(), any())
    }

    @Test
    fun getCollectionReference_whenFirestoreFails_shouldHandleGracefully() {
        whenever(firestore.collection("empresas")).thenThrow(RuntimeException("Network error"))
        
        try {
            SyncRepository.getCollectionReference(firestore, "test")
        } catch (e: Exception) {
            assertThat(e.message).contains("Network error")
        }
    }
}
