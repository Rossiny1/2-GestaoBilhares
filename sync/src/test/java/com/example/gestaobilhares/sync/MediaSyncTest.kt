package com.example.gestaobilhares.sync

import android.content.Context
import android.net.Uri
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.sync.handlers.ColaboradorSyncHandler
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.sync.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.io.File
import java.util.Date

/**
 * Testes de Sincronização de Mídia (Imagens e Assinaturas).
 * Foca na resiliência de uploads falhos e integração com FirebaseStorage.
 */
class MediaSyncTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var appRepository: AppRepository
    @Mock private lateinit var firestore: FirebaseFirestore
    @Mock private lateinit var networkUtils: NetworkUtils
    @Mock private lateinit var userSessionManager: UserSessionManager
    @Mock private lateinit var firebaseImageUploader: FirebaseImageUploader
    @Mock private lateinit var syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao
    
    @Mock private lateinit var collectionReference: CollectionReference
    @Mock private lateinit var querySnapshot: QuerySnapshot

    private lateinit var collaboratorHandler: ColaboradorSyncHandler

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock default UserSession
        whenever(userSessionManager.getCurrentUserId()).thenReturn(1L)
        whenever(userSessionManager.getCurrentCompanyId()).thenReturn("company_123")
        whenever(userSessionManager.isAdmin()).thenReturn(true)
        whenever(networkUtils.isConnected()).thenReturn(true)

        // Setup base path mocks
        val empresasColl = mock<CollectionReference>()
        val companyDoc = mock<DocumentReference>()
        val entidadesColl = mock<CollectionReference>()
        val entityDoc = mock<DocumentReference>()
        
        whenever(firestore.collection(any())).thenReturn(empresasColl)
        whenever(empresasColl.document(any())).thenReturn(companyDoc)
        whenever(companyDoc.collection(any())).thenReturn(entidadesColl)
        whenever(entidadesColl.document(any())).thenReturn(entityDoc)
        whenever(entityDoc.collection(any())).thenReturn(collectionReference)

        kotlinx.coroutines.runBlocking {
            whenever(syncMetadataDao.obterUltimoTimestamp(any(), any())).thenReturn(0L)
        }

        collaboratorHandler = ColaboradorSyncHandler(
            context, appRepository, firestore, networkUtils, 
            userSessionManager, firebaseImageUploader, syncMetadataDao
        )
    }

    @Test
    fun push_shouldUploadPhoto_whenLocalPathIsPresent() = runTest {
        // GIVEN: A collaborator with a local photo path
        val localPhotoPath = "/data/user/0/com.example.gestaobilhares/files/photo.jpg"
        val collaborator = Colaborador(
            id = 1L,
            nome = "John Doe",
            email = "john@example.com",
            fotoPerfil = localPhotoPath,
            dataUltimaAtualizacao = System.currentTimeMillis()
        )
        
        whenever(appRepository.obterTodosColaboradores()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(collaborator)))
        whenever(firebaseImageUploader.uploadColaboradorFoto(any(), any())).thenReturn("https://firebasestorage.../photo.jpg")
        
        // Mock successful firestore write
        val docRef = mock<DocumentReference>()
        whenever(collectionReference.document(any())).thenReturn(docRef)
        whenever(docRef.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))

        // WHEN: Pushing data
        val result = collaboratorHandler.push()
        
        // THEN: Upload should be triggered
        if (!result.isSuccess) throw Exception("PUSH_FAILED: " + result.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" })
        if (result.getOrNull() == 0) throw Exception("PUSH_0: success but 0 synced")
        
        assertTrue(result.isSuccess)
        verify(firebaseImageUploader).uploadColaboradorFoto(eq(localPhotoPath), eq(1L))
    }

    @Test
    fun push_shouldNotUpload_whenUrlIsAlreadyFirebaseStorage() = runTest {
        // GIVEN: A collaborator with an already uploaded photo
        val remoteUrl = "https://firebasestorage.googleapis.com/v0/b/project.appspot.com/..."
        val collaborator = Colaborador(
            id = 1L,
            nome = "John Doe",
            email = "john@example.com",
            fotoPerfil = remoteUrl,
            dataUltimaAtualizacao = System.currentTimeMillis()
        )
        
        whenever(appRepository.obterTodosColaboradores()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(collaborator)))
        whenever(firebaseImageUploader.isFirebaseStorageUrl(remoteUrl)).thenReturn(true)
        
        // Mock successful firestore write
        val docRef = mock<DocumentReference>()
        whenever(collectionReference.document(any())).thenReturn(docRef)
        whenever(docRef.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))

        // WHEN: Pushing data
        collaboratorHandler.push()
        
        // THEN: Upload should NOT be triggered
        verify(firebaseImageUploader, never()).uploadColaboradorFoto(any(), any())
    }

    @Test
    fun push_shouldHandleUploadFailure_andContinueSyncWithoutUrl() = runTest {
        // GIVEN: Upload fails
        val localPhotoPath = "/local/path/img.jpg"
        val collaborator = Colaborador(id = 1L, nome = "John Doe", email = "john@example.com", fotoPerfil = localPhotoPath)
        
        whenever(appRepository.obterTodosColaboradores()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(collaborator)))
        whenever(firebaseImageUploader.uploadColaboradorFoto(any(), any())).thenThrow(RuntimeException("Upload failed"))
        
        // Mock successful firestore write
        val docRef = mock<DocumentReference>()
        whenever(collectionReference.document(any())).thenReturn(docRef)
        whenever(docRef.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))

        // WHEN: Pushing data
        val result = collaboratorHandler.push()
        
        // THEN: Sync should still succeed (resilience) but maybe without the photo URL
        assertTrue(result.isSuccess)
        verify(collectionReference).document("1")
    }
}
