package com.example.gestaobilhares.sync

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.sync.handlers.ClienteSyncHandler
import com.example.gestaobilhares.data.entities.Cliente
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.Date

/**
 * Testes de Resolução de Conflitos e Reconciliação.
 * Garante que o sistema lida corretamente com colisões de dados e versões do servidor.
 */
class ConflictResolutionTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var appRepository: AppRepository
    @Mock private lateinit var firestore: FirebaseFirestore
    @Mock private lateinit var networkUtils: NetworkUtils
    @Mock private lateinit var userSessionManager: UserSessionManager
    @Mock private lateinit var syncMetadataDao: SyncMetadataDao
    @Mock private lateinit var firebaseImageUploader: FirebaseImageUploader
    
    @Mock private lateinit var collectionReference: CollectionReference
    @Mock private lateinit var querySnapshot: QuerySnapshot
    @Mock private lateinit var documentSnapshot: DocumentSnapshot
    
    @Mock private lateinit var empresasColl: CollectionReference
    @Mock private lateinit var companyDoc: DocumentReference
    @Mock private lateinit var entidadesColl: CollectionReference
    @Mock private lateinit var entityDoc: DocumentReference

    private lateinit var handler: ClienteSyncHandler

    @Before
    fun setup() {
        System.setProperty("isTest", "true")
        MockitoAnnotations.openMocks(this)
        
        whenever(firestore.collection(any())).thenReturn(empresasColl)
        whenever(empresasColl.document(any())).thenReturn(companyDoc)
        whenever(companyDoc.collection(any())).thenReturn(entidadesColl)
        whenever(entidadesColl.document(any())).thenReturn(entityDoc)
        whenever(entityDoc.collection(any())).thenReturn(collectionReference)
        
        // Deep stub for queries
        whenever(collectionReference.whereGreaterThan(any<String>(), any())).thenReturn(collectionReference)
        whenever(collectionReference.orderBy(any<String>(), any())).thenReturn(collectionReference)
        whenever(collectionReference.document(any())).thenReturn(entityDoc)
        
        // Mock default UserSession
        whenever(userSessionManager.getCurrentUserId()).thenReturn(123L)
        whenever(userSessionManager.getCurrentCompanyId()).thenReturn("company_123")
        whenever(userSessionManager.isAdmin()).thenReturn(true)
        whenever(userSessionManager.isApproved()).thenReturn(true)
        whenever(networkUtils.isConnected()).thenReturn(true)

        handler = ClienteSyncHandler(
            context, appRepository, firestore, networkUtils, 
            userSessionManager, firebaseImageUploader, syncMetadataDao
        )
    }

    @Test
    fun pull_shouldUpdateLocalData_whenServerIsNewer() = runTest {
        // GIVEN: Local client updated at 1000ms
        val localCliente = Cliente(id = 1L, nome = "Local Name", rotaId = 1L, dataUltimaAtualizacao = 1000L)
        whenever(appRepository.obterClientePorId(1L)).thenReturn(localCliente)
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        
        // Server client updated at 2000ms (newer)
        whenever(collectionReference.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(listOf(documentSnapshot))
        whenever(documentSnapshot.id).thenReturn("1")
        
        val serverData = mutableMapOf<String, Any>(
            "nome" to "Server Name",
            "rota_id" to 1L,
            "rotaId" to 1L,
            "valor_ficha" to 2.0,
            "valorFicha" to 2.0,
            "comissao_ficha" to 0.5,
            "comissaoFicha" to 0.5,
            "ativo" to true,
            "dataCadastro" to Timestamp(Date(1000L)),
            "data_cadastro" to Timestamp(Date(1000L)),
            "dataUltimaAtualizacao" to Timestamp(Date(2000L)),
            "data_ultima_atualizacao" to Timestamp(Date(2000L))
        )
        whenever(documentSnapshot.data).thenReturn(serverData)
        whenever(documentSnapshot.get("dataUltimaAtualizacao")).thenReturn(Timestamp(Date(2000L)))
        whenever(documentSnapshot.get("data_ultima_atualizacao")).thenReturn(Timestamp(Date(2000L)))
        whenever(documentSnapshot.getTimestamp("dataUltimaAtualizacao")).thenReturn(Timestamp(Date(2000L)))
        whenever(documentSnapshot.getTimestamp("data_ultima_atualizacao")).thenReturn(Timestamp(Date(2000L)))

        // WHEN: Pulling data
        val result = handler.pull()
        println("DEBUG_TEST: Success=${result.isSuccess}, Count=${result.getOrNull()}, Error=${result.exceptionOrNull()?.message}")
        
        // THEN:
        assertTrue("Pull failed: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals("Should have synced 1 client", 1, result.getOrNull())

        // THEN: Local record should be updated
        verify(appRepository, atLeastOnce()).atualizarCliente(any())
    }

    @Test
    fun pull_shouldNotUpdateLocalData_whenLocalIsNewer() = runTest {
        // GIVEN: Local client updated at 3000ms
        val localCliente = Cliente(id = 1L, nome = "Local New Name", rotaId = 1L, dataUltimaAtualizacao = 3000L)
        whenever(appRepository.obterClientePorId(1L)).thenReturn(localCliente)
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        
        // Server client updated at 2000ms (older)
        whenever(collectionReference.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(listOf(documentSnapshot))
        whenever(documentSnapshot.id).thenReturn("1")
        
        val serverData = mutableMapOf<String, Any>(
            "nome" to "Server Old Name",
            "rotaId" to 1L,
            "dataUltimaAtualizacao" to Timestamp(Date(2000L)),
            "dataCadastro" to Timestamp(Date(1000L))
        )
        whenever(documentSnapshot.data).thenReturn(serverData)

        // WHEN: Pulling data
        handler.pull()

        // THEN: Local record should NOT be updated
        verify(appRepository, never()).atualizarCliente(any())
    }

    @Test
    fun pull_shouldReconcileDuplicates_byNameAndRoute() = runTest {
        // GIVEN: Local client with different ID but same name/route, and an OLD timestamp
        val localCliente = Cliente(id = 100L, nome = "Duplicate Name", rotaId = 1L, dataUltimaAtualizacao = 1000L)
        whenever(appRepository.obterClientePorId(555L)).thenReturn(null)
        whenever(appRepository.buscarClientePorNomeERota("Duplicate Name", 1L)).thenReturn(localCliente)
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        
        // Server data with ID 555
        whenever(collectionReference.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(listOf(documentSnapshot))
        whenever(documentSnapshot.id).thenReturn("555")
        
        val serverData = mutableMapOf<String, Any>(
            "nome" to "Duplicate Name",
            "rota_id" to 1L,
            "rotaId" to 1L,
            "valor_ficha" to 2.0,
            "valorFicha" to 2.0,
            "comissao_ficha" to 0.5,
            "comissaoFicha" to 0.5,
            "ativo" to true,
            "dataCadastro" to Timestamp(Date(1000L)),
            "data_cadastro" to Timestamp(Date(1000L)),
            "dataUltimaAtualizacao" to Timestamp(Date(2000L)),
            "data_ultima_atualizacao" to Timestamp(Date(2000L))
        )
        whenever(documentSnapshot.data).thenReturn(serverData)
        whenever(documentSnapshot.get("dataUltimaAtualizacao")).thenReturn(Timestamp(Date(2000L)))
        whenever(documentSnapshot.get("data_ultima_atualizacao")).thenReturn(Timestamp(Date(2000L)))
        whenever(documentSnapshot.getTimestamp("dataUltimaAtualizacao")).thenReturn(Timestamp(Date(2000L)))
        whenever(documentSnapshot.getTimestamp("data_ultima_atualizacao")).thenReturn(Timestamp(Date(2000L)))

        // WHEN: Pulling data
        val result = handler.pull()
        assertTrue("Pull failed: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals("Should have synced 1 reconciled client", 1, result.getOrNull())

        // THEN: 
        verify(appRepository).migrarDadosDeCliente(eq(100L), eq(555L))
        verify(appRepository).deletarCliente(any())
        verify(appRepository).inserirCliente(any())
    }
}
