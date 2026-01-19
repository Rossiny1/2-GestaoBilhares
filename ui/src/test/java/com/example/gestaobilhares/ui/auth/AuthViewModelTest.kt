package com.example.gestaobilhares.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.core.utils.NetworkUtils
import com.example.gestaobilhares.ui.auth.usecases.CheckAuthStatusUseCase
import com.example.gestaobilhares.ui.auth.usecases.LoginUseCase
import com.example.gestaobilhares.ui.auth.usecases.LogoutUseCase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.doReturn
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var userSessionManager: UserSessionManager

    @Mock
    private lateinit var networkUtils: NetworkUtils

    @Mock
    private lateinit var authValidator: AuthValidator

    @Mock
    private lateinit var loginUseCase: LoginUseCase

    @Mock
    private lateinit var logoutUseCase: LogoutUseCase

    @Mock
    private lateinit var checkAuthStatusUseCase: CheckAuthStatusUseCase

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth
    
    @Mock
    private lateinit var firebaseUser: FirebaseUser

    @Mock
    private lateinit var authResult: AuthResult

    @Mock
    private lateinit var firestore: com.google.firebase.firestore.FirebaseFirestore
    
    @Mock
    private lateinit var crashlytics: com.google.firebase.crashlytics.FirebaseCrashlytics

    private lateinit var viewModel: AuthViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var firebaseAuthStatic: MockedStatic<FirebaseAuth>
    private lateinit var firestoreStatic: MockedStatic<com.google.firebase.firestore.FirebaseFirestore>
    private lateinit var crashlyticsStatic: MockedStatic<com.google.firebase.crashlytics.FirebaseCrashlytics>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock FirebaseAuth.getInstance()
        firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth::class.java)
        firebaseAuthStatic.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(firebaseAuth)

        // Mock FirebaseFirestore.getInstance()
        firestoreStatic = Mockito.mockStatic(com.google.firebase.firestore.FirebaseFirestore::class.java)
        firestoreStatic.`when`<com.google.firebase.firestore.FirebaseFirestore> { com.google.firebase.firestore.FirebaseFirestore.getInstance() }.thenReturn(firestore)
        
        // Mock FirebaseCrashlytics.getInstance()
        crashlyticsStatic = Mockito.mockStatic(com.google.firebase.crashlytics.FirebaseCrashlytics::class.java)
        crashlyticsStatic.`when`<com.google.firebase.crashlytics.FirebaseCrashlytics> { com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance() }.thenReturn(crashlytics)
        
        // Mock Crashlytics methods (they return Unit, not FirebaseCrashlytics)
        whenever(crashlytics.setCustomKey(anyString(), anyString())).then { }
        whenever(crashlytics.setCustomKey(anyString(), any<Boolean>())).then { }
        whenever(crashlytics.setCustomKey(anyString(), any<Int>())).then { }
        whenever(crashlytics.setCustomKey(anyString(), any<Long>())).then { }
        whenever(crashlytics.log(anyString())).then { }
        whenever(crashlytics.recordException(any())).then { }

        // Mock android.util.Log
        try {
            val logMock = org.mockito.Mockito.mockStatic(android.util.Log::class.java)
            logMock.`when`<Int> { android.util.Log.d(any<String>(), any<String>()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.e(any<String>(), any<String>(), any()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.w(any<String>(), any<String>()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.i(any<String>(), any<String>()) }.thenReturn(0)
             logMock.`when`<Int> { android.util.Log.w(any(), any<String>()) }.thenReturn(0)
        } catch (e: Exception) {
            // Ignore if already mocked or in a non-Android environment where it might fail specific calls
        }
        
        // Mock android.util.Patterns
        try {
            val patternsMock = Mockito.mockStatic(android.util.Patterns::class.java)
            val matcher = Mockito.mock(java.util.regex.Matcher::class.java)
            whenever(matcher.matches()).thenReturn(true)
            
            val pattern = Mockito.mock(java.util.regex.Pattern::class.java)
            whenever(pattern.matcher(anyString())).thenReturn(matcher)
            
            patternsMock.`when`<java.util.regex.Pattern> { android.util.Patterns.EMAIL_ADDRESS }.thenReturn(pattern)
        } catch(e: Exception) {
             // Patterns might be stubbed in Robolectric or unit test environment
        }

        // CORRECT INSTANTIATION for Hilt @Inject constructor
        viewModel = AuthViewModel(
            appRepository,
            networkUtils,
            userSessionManager,
            authValidator,
            loginUseCase,
            logoutUseCase,
            checkAuthStatusUseCase
        )
    }
    
    

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        firebaseAuthStatic.close()
        firestoreStatic.close()
        crashlyticsStatic.close()
    }

    @Test
    fun `login falha com email vazio`() = runTest {
        whenever(loginUseCase.validateInput("", "123456")).thenReturn("Email e senha são obrigatórios")
        viewModel.login("", "123456")
        advanceUntilIdle()
        
        viewModel.errorMessage.test {
            val error = awaitItem()
            assertThat(error).isEqualTo("Email e senha são obrigatórios")
        }
    }
    
    /*
    @Test
    fun `login sucesso offline com senha pessoal valida`() = runTest {
        // Arrange
        val email = "test@example.com"
        val senha = "password123"
        val hashedPassword = "password123" // In real app should be hashed
        
        val colaborador = Colaborador(
            id = 1, 
            nome = "Test User", 
            email = email, 
            senhaHash = hashedPassword,
            aprovado = true,
            ativo = true,
            nivelAcesso = NivelAcesso.USER,
            primeiroAcesso = false
        )
        
        whenever(appRepository.obterColaboradorPorEmail(email)).thenReturn(colaborador)
        whenever(userSessionManager.getCurrentUserId()).thenReturn(1L)
        whenever(userSessionManager.getCurrentUserName()).thenReturn("Test User")

        // Act
        viewModel.login(email, senha)
        advanceUntilIdle()

        // Assert
        viewModel.authState.test {
             // Skip initial unauthenticated
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(AuthState.Authenticated::class.java)
        }
    }
    */
}
