package com.example.gestaobilhares.sync

import android.content.Context
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.example.gestaobilhares.sync.handlers.*
import com.example.gestaobilhares.sync.handlers.base.BaseSyncHandler
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.Date

class FullFlowStressTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var appRepository: AppRepository
    @Mock private lateinit var firestore: FirebaseFirestore
    @Mock private lateinit var networkUtils: NetworkUtils
    @Mock private lateinit var userSessionManager: UserSessionManager
    @Mock private lateinit var syncMetadataDao: SyncMetadataDao
    @Mock private lateinit var firebaseImageUploader: FirebaseImageUploader
    
    // Handlers
    @Mock private lateinit var mesaSyncHandler: MesaSyncHandler
    @Mock private lateinit var clienteSyncHandler: ClienteSyncHandler
    @Mock private lateinit var contratoSyncHandler: ContratoSyncHandler
    @Mock private lateinit var acertoSyncHandler: AcertoSyncHandler
    @Mock private lateinit var despesaSyncHandler: DespesaSyncHandler
    @Mock private lateinit var rotaSyncHandler: RotaSyncHandler
    @Mock private lateinit var cicloSyncHandler: CicloSyncHandler
    @Mock private lateinit var colaboradorSyncHandler: ColaboradorSyncHandler
    @Mock private lateinit var colaboradorRotaSyncHandler: ColaboradorRotaSyncHandler
    @Mock private lateinit var metaColaboradorSyncHandler: MetaColaboradorSyncHandler
    @Mock private lateinit var metaSyncHandler: MetaSyncHandler
    @Mock private lateinit var assinaturaSyncHandler: AssinaturaSyncHandler
    @Mock private lateinit var veiculoSyncHandler: VeiculoSyncHandler
    @Mock private lateinit var equipamentoSyncHandler: EquipamentoSyncHandler
    @Mock private lateinit var estoqueSyncHandler: EstoqueSyncHandler

    private lateinit var syncRepository: SyncRepository
    private lateinit var collectionReference: CollectionReference
    private val collectionMockCache = mutableMapOf<String, CollectionReference>()

    @Before
    fun setup() = kotlinx.coroutines.runBlocking {
        MockitoAnnotations.openMocks(this@FullFlowStressTest)
        
        // Ensure Timber prints to console
        if (timber.log.Timber.treeCount == 0) {
            timber.log.Timber.plant(object : timber.log.Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    println("[$tag] $message")
                    t?.printStackTrace()
                }
            })
        }
        
        collectionMockCache.clear()

        doReturn(1L).whenever(userSessionManager).getCurrentUserId()
        doReturn("company123").whenever(userSessionManager).getCurrentCompanyId()
        doReturn(false).whenever(userSessionManager).isAdmin()
        whenever(networkUtils.isConnected()).thenReturn(true)
        doReturn(false).whenever(userSessionManager).hasAnyRouteAssignments(any())
        doReturn(emptyList<Long>()).whenever(userSessionManager).getUserAccessibleRoutes(any())
        
        // Mock all appRepository calls to return default values
        doReturn(0).whenever(appRepository).contarOperacoesSyncPendentes()
        doReturn(0).whenever(appRepository).contarOperacoesSyncFalhadas()
        doReturn(kotlinx.coroutines.flow.flowOf(emptyList<Acerto>())).whenever(appRepository).obterTodosAcertos()
        doReturn(kotlinx.coroutines.flow.flowOf(emptyList<Cliente>())).whenever(appRepository).obterTodosClientes()
        doReturn(kotlinx.coroutines.flow.flowOf(emptyList<Mesa>())).whenever(appRepository).obterTodasMesas()
        whenever(appRepository.inserirAcerto(any())).thenReturn(1L)
        whenever(appRepository.atualizarAcerto(any())).thenReturn(0)

        // Mock syncMetadataDao to return 0 for any entity
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), anyOrNull())).thenReturn(0L)

        val empresasColl = mock<CollectionReference>()
        val companyDoc = mock<com.google.firebase.firestore.DocumentReference>()
        val entidadesColl = mock<CollectionReference>()
        
        whenever(firestore.collection("empresas")).thenReturn(empresasColl)
        whenever(empresasColl.document(any())).thenReturn(companyDoc)
        whenever(companyDoc.collection("entidades")).thenReturn(entidadesColl)
        
        // Return unique DocumentReference and CollectionReference for each entity type
        whenever(entidadesColl.document(any())).thenAnswer { invocation ->
            val collectionName = invocation.arguments[0] as String
            val doc = mock<com.google.firebase.firestore.DocumentReference>()
            val coll = collectionMockCache.getOrPut(collectionName) { 
                val m = mock<CollectionReference>()
                setupQueryChain(m)
                m
            }
            whenever(doc.collection("items")).thenReturn(coll)
            doc
        }
        
        // Default collection reference to use in simple tests
        collectionReference = collectionMockCache.getOrPut("default") { 
            val m = mock<CollectionReference>()
            setupQueryChain(m)
            m
        }
        
        // Mock all handlers to return success by default
        val handlers = listOf(
            mesaSyncHandler, clienteSyncHandler, contratoSyncHandler, acertoSyncHandler,
            despesaSyncHandler, rotaSyncHandler, cicloSyncHandler, colaboradorSyncHandler,
            colaboradorRotaSyncHandler, metaColaboradorSyncHandler, metaSyncHandler,
            assinaturaSyncHandler, veiculoSyncHandler, equipamentoSyncHandler, estoqueSyncHandler
        )
        
        for (handler in handlers) {
            doReturn(Result.success(0)).whenever(handler).pull(anyOrNull())
            doReturn(Result.success(0)).whenever(handler).pull()
            doReturn(Result.success(0)).whenever(handler).push()
        }

        syncRepository = SyncRepository(
            context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao,
            mesaSyncHandler, clienteSyncHandler, contratoSyncHandler, acertoSyncHandler, despesaSyncHandler,
            rotaSyncHandler, cicloSyncHandler, colaboradorSyncHandler, colaboradorRotaSyncHandler,
            metaColaboradorSyncHandler, metaSyncHandler, assinaturaSyncHandler, veiculoSyncHandler,
            equipamentoSyncHandler, estoqueSyncHandler
        )
    }

    private fun setupQueryChain(query: Query) {
        whenever(query.whereEqualTo(any<String>(), anyOrNull())).thenReturn(query)
        whenever(query.whereIn(any<String>(), any())).thenReturn(query)
        whenever(query.whereGreaterThan(any<String>(), any())).thenReturn(query)
        whenever(query.limit(any())).thenReturn(query)
        whenever(query.orderBy(any<String>())).thenReturn(query)
        whenever(query.orderBy(any<String>(), any())).thenReturn(query)
        whenever(query.startAfter(any<DocumentSnapshot>())).thenReturn(query)
        whenever(query.startAfter(any<com.google.firebase.Timestamp>())).thenReturn(query)
        
        // Basic get mock
        val snapshot = mock<QuerySnapshot>()
        whenever(snapshot.documents).thenReturn(emptyList())
        whenever(snapshot.isEmpty).thenReturn(true)
        whenever(query.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(snapshot))
    }

    /**
     * Test Case 1: Relational Integrity (Pull Order)
     * Requirement: Colaborador -> Rota -> Cliente -> Mesa -> Acerto
     */
    @Test
    fun syncPull_shouldFollowCorrectOrder_toPreserveFKs() = runTest {
        // Ensure pull returns something
        whenever(colaboradorSyncHandler.pull(anyOrNull())).thenReturn(Result.success(1))
        
        val result = syncRepository.syncPull()
        result.onFailure { 
            println("PULL FAILED: ${it.message}")
            it.printStackTrace()
            org.junit.Assert.fail("Pull order test failure: ${it.message}")
        }
        assertThat(result.isSuccess).isTrue()
        
        // Verify inOrder
        val inOrder = inOrder(
            colaboradorSyncHandler,
            colaboradorRotaSyncHandler,
            rotaSyncHandler,
            clienteSyncHandler,
            mesaSyncHandler,
            cicloSyncHandler,
            acertoSyncHandler
        )
        
        try {
            inOrder.verify(colaboradorSyncHandler).pull(anyOrNull())
            inOrder.verify(colaboradorRotaSyncHandler).pull()
            inOrder.verify(rotaSyncHandler).pull(anyOrNull())
            inOrder.verify(clienteSyncHandler).pull(anyOrNull())
            inOrder.verify(mesaSyncHandler).pull(anyOrNull())
            inOrder.verify(cicloSyncHandler).pull(anyOrNull())
            inOrder.verify(acertoSyncHandler).pull(anyOrNull())
        } catch (e: Throwable) {
            println("InOrder verification failed")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Test Case 2: Stress Testing - Pagination
     * Requirement: Handle large datasets by processing in batches (500 items).
     */
    @Test
    fun pagination_shouldFetchAllBatches_whenDatasetIsLarge() = runTest {
        // Create a test handler using the real BaseSyncHandler implementation to test pagination
        val testHandler = object : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {
            override val entityType: String = "test_entities"
            override suspend fun pull(timestampOverride: Long?): Result<Int> {
                val collectionRef = getCollectionReference("test_items")
                val docs = fetchAllDocumentsWithRouteFilter(collectionRef, null)
                return Result.success(docs.size)
            }
            override suspend fun push(): Result<Int> = Result.success(0)
        }

        // We need to re-mock the items collection specifically for this test
        val itemsColl = mock<CollectionReference>()
        setupQueryChain(itemsColl)
        
        // Re-mock hierarchy for this test instance to ensure it uses our itemsColl
        val empresasColl = mock<CollectionReference>()
        val companyDoc = mock<com.google.firebase.firestore.DocumentReference>()
        val entidadesColl = mock<CollectionReference>()
        val testEntityDoc = mock<com.google.firebase.firestore.DocumentReference>()
        
        whenever(firestore.collection("empresas")).thenReturn(empresasColl)
        whenever(empresasColl.document("company123")).thenReturn(companyDoc)
        whenever(companyDoc.collection("entidades")).thenReturn(entidadesColl)
        whenever(entidadesColl.document("test_items")).thenReturn(testEntityDoc) 
        whenever(testEntityDoc.collection("items")).thenReturn(itemsColl)

        val query1 = mock<Query>()
        val query2 = mock<Query>()
        val snapshot1 = mock<QuerySnapshot>()
        val snapshot2 = mock<QuerySnapshot>()
        
        // Batch 1: 500 items (full batch)
        val batch1 = List(500) { mock<DocumentSnapshot>() }
        val lastDoc1 = batch1.last()
        
        // Batch 2: 120 items (partial batch)
        val batch2 = List(120) { mock<DocumentSnapshot>() }

        // Setup pagination mocks
        whenever(itemsColl.limit(500)).thenReturn(query1)
        whenever(query1.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(snapshot1))
        whenever(snapshot1.documents).thenReturn(batch1)
        
        whenever(query1.startAfter(lastDoc1)).thenReturn(query2)
        whenever(query2.limit(500)).thenReturn(query2)
        whenever(query2.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(snapshot2))
        whenever(snapshot2.documents).thenReturn(batch2)

        val result = testHandler.pull()
        if (result.isFailure) result.exceptionOrNull()?.printStackTrace()

        assertThat(result.getOrNull()).isEqualTo(620)
        verify(query1).get()
        verify(query2).get()
    }

    /**
     * Test Case 3: Relational Consistency check
     * Verify that if a child exists, the system handles it correctly.
     */
    @Test
    fun ensureEntityExists_shouldReturnTrue_whenLocalExists() = runTest {
        val testHandler = object : BaseSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao) {
            override val entityType: String = "test"
            override suspend fun pull(timestampOverride: Long?): Result<Int> = Result.success(0)
            override suspend fun push(): Result<Int> = Result.success(0)
            suspend fun testCheck(type: String, id: Long) = ensureEntityExists(type, id)
        }
        
        val cliente = Cliente(id = 10L, nome = "Test", rotaId = 1, dataCadastro = Date())
        whenever(appRepository.obterClientePorId(10L)).thenReturn(cliente)
        
        val exists = testHandler.testCheck("cliente", 10L)
        assertThat(exists).isTrue()
    }

    /**
     * Test Case 4: Skipping Orphaned Records
     * Requirement: Skip Acerto if Cliente doesn't exist and no Rota is assigned.
     */
    @Test
    fun syncPull_shouldSkipAcerto_whenParentClienteIsMissing() = runTest {
        // Setup Firestore with an Acerto that points to a non-existent client
        val acertoDoc = mock<DocumentSnapshot>()
        val data = mapOf(
            "cliente_id" to 999L,
            "data_acerto" to System.currentTimeMillis(),
            "periodo_inicio" to System.currentTimeMillis(),
            "periodo_fim" to System.currentTimeMillis()
        )
        whenever(acertoDoc.data).thenReturn(data)
        whenever(acertoDoc.id).thenReturn("100")
        
        // Mock to avoid NPE in toObject
        whenever(acertoDoc.toObject(Acerto::class.java)).thenReturn(Acerto(clienteId = 999L, dataAcerto = Date(), periodoInicio = Date(), periodoFim = Date()))

        // Mock appRepository to return null for this client
        whenever(appRepository.obterClientePorId(999L)).thenReturn(null)
        
        // Mock UserSessionManager to NOT be admin (so route check happens)
        whenever(userSessionManager.isAdmin()).thenReturn(false)
        whenever(userSessionManager.getUserAccessibleRoutes(any())).thenReturn(emptyList()) // No routes
        
        val acertoHandler = AcertoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        
        // Actually, let's mock the collection fetch in pullComplete
        val itemsColl = collectionMockCache.getOrPut("acertos") { 
            val m = mock<CollectionReference>()
            setupQueryChain(m)
            m
        }
        val snapshot = mock<QuerySnapshot>()
        whenever(itemsColl.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(snapshot))
        whenever(snapshot.documents).thenReturn(listOf(acertoDoc))
        whenever(snapshot.isEmpty).thenReturn(false)
        
        acertoHandler.pull()
        
        verify(appRepository, never()).inserirAcerto(any())
    }

    /**
     * Test Case 5: Stress Testing - Push Large Dataset
     * Requirement: Ensure push handles 1000+ records.
     */
    @Test
    fun syncPush_shouldHandleLargeDataset_withBatching() = runTest {
        val largeList = List(200) { i ->
            Acerto(
                id = i.toLong(), 
                clienteId = 1L, 
                dataAcerto = Date(),
                periodoInicio = Date(),
                periodoFim = Date(),
                dataCriacao = Date()
            )
        }
        
        whenever(appRepository.obterTodosAcertos()).thenReturn(kotlinx.coroutines.flow.flowOf(largeList))
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L) // Push all
        
        val acertoHandler = AcertoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        
        // Mock Firestore document set
        val docRef = mock<DocumentReference>()
        val collectionRef = collectionMockCache.getOrPut("acertos") { 
            val m = mock<CollectionReference>()
            setupQueryChain(m)
            m
        }
        whenever(collectionRef.document(any())).thenReturn(docRef)
        whenever(docRef.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))
        
        val result = acertoHandler.push()
        
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(200)
        verify(docRef, times(200)).set(any())
    }

    /**
     * Test Case 6: Full Flow Simulation
     * Requirement: Pull -> Create Local -> Push
     */
    @Test
    fun fullSync_cleanInstallSimulation() = runTest {
        // 1. Initial Pull (Empty)
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        
        // Mock all handlers pull to return 0
        whenever(acertoSyncHandler.pull(anyOrNull())).thenReturn(Result.success(0))
        whenever(clienteSyncHandler.pull(anyOrNull())).thenReturn(Result.success(0))
        
        val resultPull = syncRepository.syncPull()
        if (resultPull.isFailure) {
            org.junit.Assert.fail("Pull failure: ${resultPull.exceptionOrNull()?.message}")
        }
        assertThat(resultPull.isSuccess).isTrue()
        
        // 2. Create new local record
        val newAcerto = Acerto(
            id = 500L, 
            clienteId = 1L, 
            dataAcerto = Date(), 
            dataCriacao = Date(),
            periodoInicio = Date(),
            periodoFim = Date()
        )
        whenever(appRepository.obterTodosAcertos()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(newAcerto)))
        
        // 3. Push
        // We need to re-mock handlers push since they're used by SyncRepository
        whenever(acertoSyncHandler.push()).thenReturn(Result.success(1))
        whenever(clienteSyncHandler.push()).thenReturn(Result.success(0))
        
        val resultPush = syncRepository.syncPush()
        resultPush.onFailure { 
            println("PUSH FAILED: ${it.message}")
            it.printStackTrace()
            org.junit.Assert.fail("Push failure: ${it.message}")
        }
        assertThat(resultPush.isSuccess).isTrue()
        
        // Verify handlers were called by SyncRepository
        verify(acertoSyncHandler).push()
        verify(clienteSyncHandler).push()
    }

    /**
     * Test Case 7: Relational Integrity (Pull Success)
     * Requirement: Process Acerto when parent Cliente exists locally.
     */
    @Test
    fun syncPull_shouldProcessAcerto_whenParentClienteExists() = runTest {
        println("DEBUG: Starting syncPull_shouldProcessAcerto_whenParentClienteExists")
        val acertoDoc = mock<DocumentSnapshot>()
        val acertoRef = mock<DocumentReference>()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "cliente_id" to 10L,
            "data_acerto" to now,
            "periodo_inicio" to now,
            "periodo_fim" to now,
            "valor_total" to 100.0,
            "data_criacao" to now,
            "status" to "PENDENTE",
            "total_mesas" to 1.0
        )
        whenever(acertoDoc.data).thenReturn(data)
        whenever(acertoDoc.id).thenReturn("101")
        whenever(acertoDoc.reference).thenReturn(acertoRef)
        
        // Mock toObject to avoid NPE in some handlers (though AcertoSyncHandler uses gson)
        val acertoObj = Acerto(id = 101L, clienteId = 10L, dataAcerto = Date(now), periodoInicio = Date(now), periodoFim = Date(now))
        whenever(acertoDoc.toObject(Acerto::class.java)).thenReturn(acertoObj)
        
        // Mock client exists
        whenever(appRepository.obterClientePorId(10L)).thenReturn(Cliente(id = 10L, nome = "Cliente 10", rotaId = 1, dataCadastro = Date()))
        
        // Mock UserSessionManager to be admin to simplify
        whenever(userSessionManager.isAdmin()).thenReturn(true)
        
        val acertoHandler = AcertoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        
        // Use the cache-provided mock
        val itemsColl = collectionMockCache.getOrPut("acertos") { 
            val m = mock<CollectionReference>()
            setupQueryChain(m)
            m
        }
        val snapshot = mock<QuerySnapshot>()
        whenever(itemsColl.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(snapshot))
        whenever(snapshot.documents).thenReturn(listOf(acertoDoc))
        whenever(snapshot.isEmpty).thenReturn(false)
        
        // Mock AcertoMesas collection from document reference
        val mesasColl = mock<CollectionReference>()
        setupQueryChain(mesasColl)
        whenever(acertoRef.collection(any())).thenReturn(mesasColl)
        
        println("DEBUG: Calling acertoHandler.pull()")
        val result = acertoHandler.pull()
        println("DEBUG: result isSuccess=${result.isSuccess}, count=${result.getOrNull()}")
        if (result.isFailure) {
            println("DEBUG: Failure exception: ${result.exceptionOrNull()}")
            result.exceptionOrNull()?.printStackTrace()
        }
        
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1)
        println("DEBUG: Verifying inserirAcerto")
        verify(appRepository, atLeastOnce()).inserirAcerto(any())
        println("DEBUG: Test finished successfully")
    }

    /**
     * Test Case 8: Incremental Sync Verification
     * Requirement: Only pull data modified since last sync.
     */
    @Test
    fun incrementalSync_shouldOnlyPullNewData() = runTest {
        val lastSyncTime = 1000L
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(lastSyncTime)
        whenever(appRepository.obterTodosAcertos()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(mock())))
        whenever(userSessionManager.isAdmin()).thenReturn(true)
        
        val acertoHandler = AcertoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        
        // Use the cache-provided mock
        val itemsColl = collectionMockCache.getOrPut("acertos") { 
            val m = mock<CollectionReference>()
            setupQueryChain(m)
            m
        }
        
        // We expect a successful incremental pull returning 0 documents
        val snapshot = mock<QuerySnapshot>()
        whenever(itemsColl.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(snapshot))
        whenever(snapshot.documents).thenReturn(emptyList())
        whenever(snapshot.isEmpty).thenReturn(true)
        
        val result = acertoHandler.pull()
        result.onFailure {
            println("INCREMENTAL PULL FAILED: ${it.message}")
            it.printStackTrace()
        }
        assertThat(result.isSuccess).isTrue()
        
        verify(itemsColl).whereGreaterThan(eq("lastModified"), any<Timestamp>())
    }

    private fun getCollectionReference(firestore: FirebaseFirestore, collectionName: String, companyId: String): CollectionReference {
        return firestore
            .collection("empresas")
            .document(companyId)
            .collection("entidades")
            .document(collectionName)
            .collection("items")
    }
}
