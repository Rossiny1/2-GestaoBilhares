package com.example.gestaobilhares.sync

import android.content.Context
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
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class MediaSyncTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var appRepository: AppRepository
    @Mock private lateinit var firestore: FirebaseFirestore
    @Mock private lateinit var networkUtils: NetworkUtils
    @Mock private lateinit var userSessionManager: UserSessionManager
    @Mock private lateinit var firebaseImageUploader: FirebaseImageUploader
    @Mock private lateinit var syncMetadataDao: com.example.gestaobilhares.data.dao.SyncMetadataDao
    
    @Mock private lateinit var colaboradoresColl: CollectionReference
    @Mock private lateinit var documentReference: DocumentReference
    @Mock private lateinit var querySnapshot: QuerySnapshot
    @Mock private lateinit var documentSnapshot: DocumentSnapshot

    private lateinit var collaboratorHandler: ColaboradorSyncHandler

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        whenever(userSessionManager.getCurrentUserId()).thenReturn(1L)
        whenever(userSessionManager.getCurrentCompanyId()).thenReturn("company_123")
        whenever(userSessionManager.isAdmin()).thenReturn(true)
        whenever(networkUtils.isConnected()).thenReturn(true)

        val empresasColl = mock<CollectionReference>()
        val companyDoc = mock<DocumentReference>()
        
        whenever(firestore.collection("empresas")).thenReturn(empresasColl)
        whenever(empresasColl.document(any())).thenReturn(companyDoc)
        whenever(companyDoc.collection("colaboradores")).thenReturn(colaboradoresColl)
        
        whenever(colaboradoresColl.document(any())).thenReturn(documentReference)
        whenever(colaboradoresColl.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(querySnapshot))
        
        whenever(documentReference.set(any())).thenReturn(com.google.android.gms.tasks.Tasks.forResult(null))
        whenever(documentReference.get()).thenReturn(com.google.android.gms.tasks.Tasks.forResult(documentSnapshot))
        
        whenever(querySnapshot.documents).thenReturn(emptyList())

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
        val localPhotoPath = "/data/user/0/com.example.gestaobilhares/files/photo.jpg"
        val collaborator = Colaborador(
            id = 1L,
            nome = "John Doe",
            email = "john@example.com",
            fotoPerfil = localPhotoPath,
            firebaseUid = "uid_123",
            dataUltimaAtualizacao = System.currentTimeMillis()
        )
        
        whenever(appRepository.obterTodosColaboradores()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(collaborator)))
        whenever(firebaseImageUploader.isFirebaseStorageUrl(localPhotoPath)).thenReturn(false)
        whenever(firebaseImageUploader.uploadColaboradorFoto(any(), any())).thenReturn("https://firebasestorage.../photo.jpg")
        
        val result = collaboratorHandler.push()
        
        assertTrue("Push should be successful, but was: ${result.exceptionOrNull()}", result.isSuccess)
        assertTrue("Push should have synced 1 item, but synced: ${result.getOrNull()}", (result.getOrNull() ?: 0) > 0)
        
        verify(firebaseImageUploader).uploadColaboradorFoto(eq(localPhotoPath), eq(1L))
        verify(documentReference).set(any())
    }

    @Test
    fun push_shouldNotUpload_whenUrlIsAlreadyFirebaseStorage() = runTest {
        val remoteUrl = "https://firebasestorage.googleapis.com/v0/b/project.appspot.com/..."
        val collaborator = Colaborador(
            id = 1L,
            nome = "John Doe",
            email = "john@example.com",
            fotoPerfil = remoteUrl,
            firebaseUid = "uid_123",
            dataUltimaAtualizacao = System.currentTimeMillis()
        )
        
        whenever(appRepository.obterTodosColaboradores()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(collaborator)))
        whenever(firebaseImageUploader.isFirebaseStorageUrl(remoteUrl)).thenReturn(true)
        
        collaboratorHandler.push()
        
        verify(firebaseImageUploader, never()).uploadColaboradorFoto(any(), any())
        verify(documentReference).set(any())
    }

    @Test
    fun push_shouldHandleUploadFailure_andContinueSyncWithoutUrl() = runTest {
        val localPhotoPath = "/local/path/img.jpg"
        val collaborator = Colaborador(
            id = 1L, nome = "John Doe", email = "john@example.com", 
            fotoPerfil = localPhotoPath, firebaseUid = "uid_123",
            dataUltimaAtualizacao = System.currentTimeMillis()
        )
        
        whenever(appRepository.obterTodosColaboradores()).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(collaborator)))
        whenever(firebaseImageUploader.isFirebaseStorageUrl(localPhotoPath)).thenReturn(false)
        whenever(firebaseImageUploader.uploadColaboradorFoto(any(), any())).thenThrow(RuntimeException("Upload failed"))
        
        val result = collaboratorHandler.push()
        
        assertTrue(result.isSuccess)
        verify(documentReference).set(any())
    }
}
