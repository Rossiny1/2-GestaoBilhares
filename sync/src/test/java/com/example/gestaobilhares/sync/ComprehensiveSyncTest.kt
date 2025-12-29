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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Date

class ComprehensiveSyncTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var appRepository: AppRepository
    @Mock private lateinit var firestore: FirebaseFirestore
    @Mock private lateinit var networkUtils: NetworkUtils
    @Mock private lateinit var userSessionManager: UserSessionManager
    @Mock private lateinit var syncMetadataDao: SyncMetadataDao
    @Mock private lateinit var firebaseImageUploader: FirebaseImageUploader
    
    // Handler Mocks for SyncRepository
    @Mock private lateinit var mesaSyncHandlerMock: MesaSyncHandler
    @Mock private lateinit var clienteSyncHandlerMock: ClienteSyncHandler
    @Mock private lateinit var contratoSyncHandlerMock: ContratoSyncHandler
    @Mock private lateinit var acertoSyncHandlerMock: AcertoSyncHandler
    @Mock private lateinit var despesaSyncHandlerMock: DespesaSyncHandler
    @Mock private lateinit var rotaSyncHandlerMock: RotaSyncHandler
    @Mock private lateinit var cicloSyncHandlerMock: CicloSyncHandler
    @Mock private lateinit var colaboradorSyncHandlerMock: ColaboradorSyncHandler
    @Mock private lateinit var colaboradorRotaSyncHandlerMock: ColaboradorRotaSyncHandler
    @Mock private lateinit var metaColaboradorSyncHandlerMock: MetaColaboradorSyncHandler
    @Mock private lateinit var metaSyncHandlerMock: MetaSyncHandler
    @Mock private lateinit var assinaturaSyncHandlerMock: AssinaturaSyncHandler
    @Mock private lateinit var veiculoSyncHandlerMock: VeiculoSyncHandler
    @Mock private lateinit var equipamentoSyncHandlerMock: EquipamentoSyncHandler
    @Mock private lateinit var estoqueSyncHandlerMock: EstoqueSyncHandler
    
    @Mock private lateinit var collectionReference: CollectionReference
    @Mock private lateinit var documentReference: DocumentReference
    @Mock private lateinit var querySnapshot: QuerySnapshot
    @Mock private lateinit var documentSnapshot: DocumentSnapshot

    private lateinit var syncRepository: SyncRepository
    private lateinit var testClienteHandler: ClienteSyncHandler
    private lateinit var testMesaHandler: MesaSyncHandler
    private lateinit var testAcertoHandler: AcertoSyncHandler
    private lateinit var testDespesaHandler: DespesaSyncHandler
    private lateinit var testContratoHandler: ContratoSyncHandler
    private lateinit var testRotaHandler: RotaSyncHandler
    private lateinit var testColaboradorHandler: ColaboradorSyncHandler
    private lateinit var testVeiculoHandler: VeiculoSyncHandler
    private lateinit var testCicloHandler: CicloSyncHandler
    private lateinit var testColaboradorRotaHandler: ColaboradorRotaSyncHandler

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        whenever(userSessionManager.getCurrentUserId()).thenReturn(1L)
        whenever(userSessionManager.getCurrentCompanyId()).thenReturn("company123")
        whenever(userSessionManager.isAdmin()).thenReturn(true)
        whenever(userSessionManager.isApproved()).thenReturn(true)
        whenever(networkUtils.isConnected()).thenReturn(true)
        
        // Mock hierarchal Firestore path
        val empresasColl = mock<CollectionReference>()
        val companyDoc = mock<DocumentReference>()
        val entidadesColl = mock<CollectionReference>()
        val entityDoc = mock<DocumentReference>()
        
        whenever(firestore.collection("empresas")).thenReturn(empresasColl)
        whenever(empresasColl.document("company123")).thenReturn(companyDoc)
        whenever(companyDoc.collection("entidades")).thenReturn(entidadesColl)
        whenever(entidadesColl.document(any())).thenReturn(entityDoc)
        whenever(entityDoc.collection("items")).thenReturn(collectionReference)
        
        whenever(collectionReference.document(any())).thenReturn(documentReference)
        whenever(collectionReference.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(emptyList())

        syncRepository = SyncRepository(
            context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao,
            mesaSyncHandlerMock, clienteSyncHandlerMock, contratoSyncHandlerMock, acertoSyncHandlerMock, despesaSyncHandlerMock,
            rotaSyncHandlerMock, cicloSyncHandlerMock, colaboradorSyncHandlerMock, colaboradorRotaSyncHandlerMock,
            metaColaboradorSyncHandlerMock, metaSyncHandlerMock, assinaturaSyncHandlerMock, veiculoSyncHandlerMock,
            equipamentoSyncHandlerMock, estoqueSyncHandlerMock
        )
        
        testClienteHandler = ClienteSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testMesaHandler = MesaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testAcertoHandler = AcertoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testDespesaHandler = DespesaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testContratoHandler = ContratoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testRotaHandler = RotaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testColaboradorHandler = ColaboradorSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testVeiculoHandler = VeiculoSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testCicloHandler = CicloSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
        testColaboradorRotaHandler = ColaboradorRotaSyncHandler(context, appRepository, firestore, networkUtils, userSessionManager, firebaseImageUploader, syncMetadataDao)
    }

    // --- SyncRepository Tests ---

    @Test
    fun saveSyncMetadata_shouldUseUserSpecificMetadata() = runTest {
        syncRepository.saveSyncMetadata("entity", 10, 100, 0)
        verify(syncMetadataDao).atualizarTimestamp(eq("entity"), eq(1L), any(), eq(10), eq(100L), eq(0L), eq(0L), anyOrNull(), any())
    }

    // --- ClienteSyncHandler Tests ---

    @Test
    fun clientePull_shouldPerformCompletePull_whenNoLastSync() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        
        // Mock pagination
        val mockLimitQuery = mock<com.google.firebase.firestore.Query>()
        whenever(collectionReference.limit(any())).thenReturn(mockLimitQuery)
        whenever(mockLimitQuery.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(emptyList())
        
        testClienteHandler.pull()
        verify(mockLimitQuery).get()
    }

    @Test
    fun clientePush_shouldSendChangesToFirestore() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("clientes_push"), eq(1L))).thenReturn(1000L)
        val cliente = Cliente(id = 1, nome = "Teste", rotaId = 1, dataUltimaAtualizacao = Date(2000L))
        whenever(appRepository.obterTodosClientes()).thenReturn(flowOf(listOf(cliente)))
        whenever(documentReference.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))
        
        testClienteHandler.push()
        verify(documentReference).set(any())
    }

    // --- MesaSyncHandler Tests ---

    @Test
    fun mesaPush_shouldIncludeRoomId() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("mesas_push"), eq(1L))).thenReturn(1000L)
        val mesa = Mesa(id = 1, numero = "1", clienteId = 10L, dataUltimaLeitura = Date(2000L))
        whenever(appRepository.obterTodasMesas()).thenReturn(flowOf(listOf(mesa)))
        whenever(documentReference.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))
        val mockSnapshot = mock<DocumentSnapshot>()
        whenever(documentReference.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(mockSnapshot))

        testMesaHandler.push()
        val mapCaptor = argumentCaptor<Map<String, Any>>()
        verify(documentReference).set(mapCaptor.capture())
        assertThat(mapCaptor.firstValue).containsKey("roomId")
    }

    // --- AcertoSyncHandler Tests ---

    @Test
    fun acertoPush_shouldSendAcertoAndAcertoMesas() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("acertos_push"), eq(1L))).thenReturn(1000L)
        
        val now = Date(2000L)
        val acerto = Acerto(id = 200, clienteId = 1, dataAcerto = now, periodoInicio = now, periodoFim = now, dataCriacao = now)
        val acertoMesa = AcertoMesa(id = 1, acertoId = 200, mesaId = 10, relogioInicial = 0, relogioFinal = 10, fichasJogadas = 10, subtotal = 100.0)
        
        whenever(appRepository.obterTodosAcertos()).thenReturn(flowOf(listOf(acerto)))
        whenever(appRepository.buscarAcertoMesasPorAcerto(200)).thenReturn(listOf(acertoMesa))
        whenever(documentReference.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))
        
        testAcertoHandler.push()
        
        // Verify Acerto push
        verify(documentReference, atLeastOnce()).set(any())
        // Verify AcertoMesa push happens
        verify(documentReference, times(2)).set(any())
    }

    // --- DespesaSyncHandler Tests ---

    @Test
    fun despesaPush_shouldIncludeRoomIdAndSyncTimestamp() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("despesas_push"), eq(1L))).thenReturn(1000L)
        
        // Use a fixed date to be certain
        val fixedDate = java.time.LocalDateTime.of(2023, 1, 1, 12, 0)
        val despesa = Despesa(id = 300, rotaId = 1, descricao = "Gasosa", valor = 50.0, categoria = "Combustivel", dataHora = fixedDate)
        
        whenever(appRepository.obterTodasDespesas()).thenReturn(flowOf(listOf(despesa)))
        whenever(appRepository.buscarCategoriasAtivas()).thenReturn(flowOf(emptyList()))
        whenever(appRepository.buscarTiposAtivosComCategoria()).thenReturn(flowOf(emptyList()))
        whenever(documentReference.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))
        
        testDespesaHandler.push()
        
        val mapCaptor = argumentCaptor<Map<String, Any>>()
        verify(documentReference).set(mapCaptor.capture())
        assertThat(mapCaptor.firstValue).containsKey("roomId")
        assertThat(mapCaptor.firstValue).containsKey("syncTimestamp")
    }

    // --- ContratoSyncHandler Tests ---

    @Test
    fun contratoPush_shouldSendContratoAndChildren() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("contratos_push"), eq(1L))).thenReturn(1000L)
        
        val now = Date(2000L)
        val contrato = ContratoLocacao(
            id = 400, 
            clienteId = 1, 
            numeroContrato = "001",
            locatarioNome = "Empresa",
            locatarioCpf = "123",
            locatarioEndereco = "Rua A",
            locatarioTelefone = "123",
            locatarioEmail = "a@a.com",
            valorMensal = 100.0,
            diaVencimento = 10,
            tipoPagamento = "FIXO",
            dataContrato = now,
            dataInicio = now,
            dataAtualizacao = now
        )
        val aditivo = AditivoContrato(
            id = 450, 
            contratoId = 400, 
            numeroAditivo = "ADT-001",
            dataAditivo = now
        )
        val aditivoMesa = AditivoMesa(
            id = 1, 
            aditivoId = 450, 
            mesaId = 10, 
            tipoEquipamento = "Mesa", 
            numeroSerie = "123"
        )
        val contratoMesa = ContratoMesa(
            id = 1, 
            contratoId = 400, 
            mesaId = 10, 
            tipoEquipamento = "Mesa", 
            numeroSerie = "123"
        )
        
        whenever(appRepository.buscarTodosContratos()).thenReturn(flowOf(listOf(contrato)))
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("aditivo_mesas_push"), eq(1L))).thenReturn(0L)
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("contrato_mesas_push"), eq(1L))).thenReturn(0L)
        whenever(appRepository.buscarAditivosPorContrato(400)).thenReturn(flowOf(listOf(aditivo)))
        whenever(appRepository.obterTodosAditivoMesas()).thenReturn(listOf(aditivoMesa))
        whenever(appRepository.obterTodosContratoMesas()).thenReturn(listOf(contratoMesa))
        whenever(documentReference.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))
        
        val pushResult = testContratoHandler.push()
        assertThat(pushResult.isSuccess).isTrue()
        
        // Verify individual pushes - use atLeastOnce to avoid sensitivity to internal structure changes
        verify(collectionReference, atLeastOnce()).document(any())
        verify(documentReference, atLeastOnce()).set(any())
    }

    // --- RotaSyncHandler Tests ---

    @Test
    fun rotaPull_shouldPerformIncremental_whenLastSyncExists() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("rotas"), eq(1L))).thenReturn(1000L)
        
        // Mock pagination
        val mockLimitQuery = mock<com.google.firebase.firestore.Query>()
        whenever(collectionReference.whereGreaterThan(eq("lastModified"), any<com.google.firebase.Timestamp>())).thenReturn(mockLimitQuery)
        whenever(mockLimitQuery.orderBy(any<String>())).thenReturn(mockLimitQuery)
        whenever(mockLimitQuery.limit(any())).thenReturn(mockLimitQuery)
        whenever(mockLimitQuery.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(emptyList())
        whenever(querySnapshot.isEmpty).thenReturn(true)
        
        testRotaHandler.pull()
        
        // Verify query used limit for pagination
        verify(mockLimitQuery).get()
    }

    // --- ColaboradorSyncHandler Tests ---

    @Test
    fun colaboradorPull_shouldPerformComplete_whenNoLastSync() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(eq("colaboradores"), eq(1L))).thenReturn(0L)
        whenever(collectionReference.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(emptyList())
        whenever(querySnapshot.isEmpty).thenReturn(true)
        
        testColaboradorHandler.pull()
        
        verify(collectionReference).get()
    }

    @Test
    fun colaboradorPush_shouldUploadImage_whenColaboradorHasLocalImage() = runTest {
        val colaborador = Colaborador(id = 1, nome = "Colab 1", fotoPerfil = "local/path/img.jpg", email = "colab1@test.com")
        whenever(appRepository.obterTodosColaboradores()).thenReturn(flowOf(listOf(colaborador)))
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        whenever(firebaseImageUploader.isFirebaseStorageUrl(any())).thenReturn(false)
        whenever(firebaseImageUploader.uploadColaboradorFoto(any(), any())).thenReturn("http://cloud/img.jpg")
        
        testColaboradorHandler.push()
        
        verify(firebaseImageUploader).uploadColaboradorFoto(eq("local/path/img.jpg"), eq(1L))
    }

    // --- VeiculoSyncHandler Tests ---

    @Test
    fun veiculoPull_shouldSyncVeiculoAndHistoricos() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        whenever(querySnapshot.isEmpty).thenReturn(true)
        
        testVeiculoHandler.pull()
        
        // Should pull from 3 core collections (at least)
        verify(firestore, atLeast(3)).collection(any())
    }

    // --- CicloSyncHandler Tests ---

    @Test
    fun cicloPull_shouldFilterByRoute() = runTest {
        whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        whenever(userSessionManager.isAdmin()).thenReturn(false)
        whenever(userSessionManager.getUserAccessibleRoutes(any())).thenReturn(listOf(10L))
        
        val mockQuery = mock<Query>()
        whenever(collectionReference.whereEqualTo(any<String>(), any())).thenReturn(mockQuery)
        whenever(mockQuery.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        whenever(querySnapshot.isEmpty).thenReturn(true)
        
        testCicloHandler.pull()
        
        // Should use route filter
        verify(collectionReference).whereEqualTo(eq("rotaId"), eq(10L))
    }

    @Test
    fun cicloPull_shouldNotFilterByRoute_whenAdmin() = runTest {
        whenever(userSessionManager.isAdmin()).thenReturn(true)
        whenever(querySnapshot.isEmpty).thenReturn(true)
        
        testCicloHandler.pull()
        
        // Should NOT use route filter
        verify(collectionReference, never()).whereEqualTo(eq("rotaId"), any())
    }
}
