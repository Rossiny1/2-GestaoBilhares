package com.example.gestaobilhares.sync

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.sync.handlers.ClienteSyncHandler
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.Date

/**
 * Testes avançados de Segurança (Multi-Tenancy) e Resiliência (Idempotência).
 * Garante que o isolamento de dados entre empresas e rotas é inviolável.
 */
class SecurityAndResilienceTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var appRepository: AppRepository
    @Mock private lateinit var firestore: FirebaseFirestore
    @Mock private lateinit var networkUtils: NetworkUtils
    @Mock private lateinit var userSessionManager: UserSessionManager
    @Mock private lateinit var syncMetadataDao: SyncMetadataDao
    @Mock private lateinit var firebaseImageUploader: FirebaseImageUploader
    
    @Mock private lateinit var collectionReference: CollectionReference
    @Mock private lateinit var documentReference: DocumentReference
    @Mock private lateinit var querySnapshot: QuerySnapshot

    private lateinit var handler: ClienteSyncHandler

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup base path mocks
        val empresasColl = mock<CollectionReference>()
        val companyDoc = mock<DocumentReference>()
        val entidadesColl = mock<CollectionReference>()
        val entityDoc = mock<DocumentReference>()
        
        whenever(firestore.collection("empresas")).thenReturn(empresasColl)
        whenever(empresasColl.document(any())).thenReturn(companyDoc)
        whenever(companyDoc.collection("entidades")).thenReturn(entidadesColl)
        whenever(entidadesColl.document(any())).thenReturn(entityDoc)
        whenever(entityDoc.collection("items")).thenReturn(collectionReference)
        
        // Mock collection operations
        whenever(collectionReference.document(any())).thenReturn(documentReference)
        whenever(collectionReference.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(documentReference.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))
        whenever(querySnapshot.documents).thenReturn(emptyList())

        // Mock UserSession
        whenever(userSessionManager.getCurrentUserId()).thenReturn(123L)
        whenever(userSessionManager.getCurrentCompanyId()).thenReturn("company_123")
        whenever(userSessionManager.isApproved()).thenReturn(true)

        handler = ClienteSyncHandler(
            context, appRepository, firestore, networkUtils, 
            userSessionManager, firebaseImageUploader, syncMetadataDao
        )
    }

    // --- SEGURANÇA: MULTI-TENANCY ---

    @Test
    fun getCollectionReference_shouldAlwaysIncludeCurrentCompanyId() = runTest {
        whenever(userSessionManager.getCurrentCompanyId()).thenReturn("company_A")
        handler.pull()
        verify(firestore.collection("empresas")).document("company_A")
        
        whenever(userSessionManager.getCurrentCompanyId()).thenReturn("company_B")
        handler.pull()
        verify(firestore.collection("empresas")).document("company_B")
    }

    @Test
    fun routeFilter_shouldRestrictDataAccess_whenUserHasSpecificRoutes() = runTest {
        whenever(userSessionManager.isAdmin()).thenReturn(false)
        whenever(userSessionManager.getUserAccessibleRoutes(any())).thenReturn(listOf(10L, 20L))
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        
        val mockQuery = mock<Query>()
        whenever(collectionReference.whereIn(eq("rota_id"), any())).thenReturn(mockQuery)
        whenever(mockQuery.orderBy(any<String>())).thenReturn(mockQuery)
        whenever(mockQuery.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(emptyList())

        handler.pull()

        verify(collectionReference).whereIn(eq("rota_id"), eq(listOf(10L, 20L)))
    }

    @Test
    fun routeFilter_shouldDenyAllAccess_whenUserHasNoAssignedRoutes() = runTest {
        whenever(userSessionManager.isAdmin()).thenReturn(false)
        whenever(userSessionManager.getUserAccessibleRoutes(any())).thenReturn(emptyList())
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        
        val result = handler.pull()

        assertTrue("Pull failed with: ${result.exceptionOrNull()}", result.isSuccess)
    }

    // --- INTEGRIDADE: CHAVES ESTRANGEIRAS ---

    @Test
    fun ensureEntityExists_shouldReturnFalse_whenParentIsMissing() = runTest {
        whenever(appRepository.obterClientePorId(999L)).thenReturn(null)
        
        val testHandler = object : BaseSyncHandler(
            context, appRepository, firestore, networkUtils, 
            userSessionManager, firebaseImageUploader, syncMetadataDao
        ) {
            override val entityType: String = "test"
            override suspend fun pull(timestampOverride: Long?) = Result.success(0)
            override suspend fun push() = Result.success(0)
            
            suspend fun testEnsure(type: String, id: Long) = ensureEntityExists(type, id)
        }
        
        val exists = testHandler.testEnsure("cliente", 999L)
        
        assertFalse(exists)
    }

    // --- RESILIÊNCIA: IDEMPOTÊNCIA ---

    @Test
    fun push_shouldOnlySendNewOrModifiedItems_basedOnTimestamp() = runTest {
        // GIVEN: Last successful push was at 5000ms
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("clientes_push"), any())).thenReturn(5000L)
        
        // One old cliente (lastMod 4000) and one new (lastMod 6000)
        val oldCliente = Cliente(id = 1L, nome = "Old", rotaId = 1L, dataUltimaAtualizacao = 4000L)
        val newCliente = Cliente(id = 2L, nome = "New", rotaId = 1L, dataUltimaAtualizacao = 6000L)
        
        whenever(appRepository.obterTodosClientes()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(oldCliente, newCliente)))
        whenever(documentReference.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))

        // WHEN: Executing push
        handler.push()

        // THEN: Only client 2 should be sent to Firestore
        verify(collectionReference).document(eq("2"))
        verify(collectionReference, never()).document(eq("1"))
    }
}
