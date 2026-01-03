package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

/**
 * Testes unitários para ColaboradorAuthService
 * 
 * Testa:
 * - Processamento de colaborador durante login
 * - Preservação de status de aprovação
 * - Sincronização com Firestore
 * - Resolução de conflitos (local vs Firestore)
 */
class ColaboradorAuthServiceTest {
    
    private lateinit var colaboradorRepository: ColaboradorRepository
    private lateinit var colaboradorFirestoreRepository: ColaboradorFirestoreRepository
    private lateinit var service: ColaboradorAuthService
    
    @Before
    fun setup() {
        colaboradorRepository = mock()
        colaboradorFirestoreRepository = mock()
        service = ColaboradorAuthService(colaboradorRepository, colaboradorFirestoreRepository)
    }
    
    @Test
    fun `processarColaboradorNoLogin retorna colaborador local quando existe e está aprovado`() = runTest {
        // Arrange
        val empresaId = "empresa_001"
        val uid = "test-uid-123"
        val email = "teste@exemplo.com"
        val colaboradorLocal = criarColaboradorTeste(
            id = 1L,
            uid = uid,
            email = email,
            aprovado = true
        )
        
        whenever(colaboradorRepository.obterPorFirebaseUid(uid)).thenReturn(colaboradorLocal)
        whenever(colaboradorFirestoreRepository.getColaboradorByUid(empresaId, uid))
            .thenReturn(criarColaboradorTeste(uid = uid, aprovado = false))
        
        // Act
        val resultado = service.processarColaboradorNoLogin(empresaId, uid, email)
        
        // Assert
        assertNotNull(resultado)
        assertEquals(uid, resultado.firebaseUid)
        assertEquals(true, resultado.aprovado)
        verify(colaboradorFirestoreRepository).atualizarStatusAprovacao(
            eq(empresaId), eq(uid), eq(true), anyOrNull(), anyOrNull()
        )
    }
    
    @Test
    fun `processarColaboradorNoLogin cria pendente quando não existe`() = runTest {
        // Arrange
        val empresaId = "empresa_001"
        val uid = "test-uid-123"
        val email = "novo@exemplo.com"
        val colaboradorPendente = criarColaboradorTeste(
            id = 1L,
            uid = uid,
            email = email,
            aprovado = false
        )
        
        whenever(colaboradorRepository.obterPorFirebaseUid(uid)).thenReturn(null)
        whenever(colaboradorRepository.obterPorEmail(email)).thenReturn(null)
        whenever(colaboradorFirestoreRepository.getColaboradorByUid(empresaId, uid))
            .thenReturn(null)
        whenever(colaboradorRepository.criarColaboradorPendenteLocal(uid, email))
            .thenReturn(colaboradorPendente)
        
        // Act
        val resultado = service.processarColaboradorNoLogin(empresaId, uid, email)
        
        // Assert
        assertNotNull(resultado)
        assertEquals(uid, resultado.firebaseUid)
        assertEquals(false, resultado.aprovado)
        verify(colaboradorFirestoreRepository).criarColaboradorNoFirestore(
            colaboradorPendente, empresaId, uid
        )
    }
    
    @Test
    fun `processarColaboradorNoLogin preserva aprovação local quando Firestore tem false`() = runTest {
        // Arrange
        val empresaId = "empresa_001"
        val uid = "test-uid-123"
        val email = "teste@exemplo.com"
        val colaboradorLocal = criarColaboradorTeste(
            id = 1L,
            uid = uid,
            email = email,
            aprovado = true
        )
        val colaboradorFirestore = criarColaboradorTeste(
            uid = uid,
            email = email,
            aprovado = false
        )
        
        whenever(colaboradorRepository.obterPorFirebaseUid(uid)).thenReturn(colaboradorLocal)
        whenever(colaboradorFirestoreRepository.getColaboradorByUid(empresaId, uid))
            .thenReturn(colaboradorFirestore)
        
        // Act
        val resultado = service.processarColaboradorNoLogin(empresaId, uid, email)
        
        // Assert
        assertNotNull(resultado)
        assertEquals(true, resultado.aprovado) // Deve preservar aprovação local
        verify(colaboradorFirestoreRepository).atualizarStatusAprovacao(
            eq(empresaId), eq(uid), eq(true), anyOrNull(), anyOrNull()
        )
    }
    
    @Test
    fun `processarColaboradorNoLogin salva do Firestore quando não existe localmente`() = runTest {
        // Arrange
        val empresaId = "empresa_001"
        val uid = "test-uid-123"
        val email = "teste@exemplo.com"
        val colaboradorFirestore = criarColaboradorTeste(
            id = 0L,
            uid = uid,
            email = email,
            aprovado = true
        )
        
        whenever(colaboradorRepository.obterPorFirebaseUid(uid)).thenReturn(null)
        whenever(colaboradorRepository.obterPorEmail(email)).thenReturn(null)
        whenever(colaboradorFirestoreRepository.getColaboradorByUid(empresaId, uid))
            .thenReturn(colaboradorFirestore)
        whenever(colaboradorRepository.inserirColaborador(colaboradorFirestore)).thenReturn(1L)
        
        // Act
        val resultado = service.processarColaboradorNoLogin(empresaId, uid, email)
        
        // Assert
        assertNotNull(resultado)
        assertEquals(uid, resultado.firebaseUid)
        verify(colaboradorRepository).inserirColaborador(colaboradorFirestore)
    }
    
    @Test
    fun `processarColaboradorNoLogin atualiza firebaseUid quando necessário`() = runTest {
        // Arrange
        val empresaId = "empresa_001"
        val uid = "test-uid-123"
        val email = "teste@exemplo.com"
        val colaboradorLocal = criarColaboradorTeste(
            id = 1L,
            uid = null, // Sem UID
            email = email,
            aprovado = true
        )
        
        whenever(colaboradorRepository.obterPorFirebaseUid(uid)).thenReturn(null)
        whenever(colaboradorRepository.obterPorEmail(email)).thenReturn(colaboradorLocal)
        
        // Act
        val resultado = service.processarColaboradorNoLogin(empresaId, uid, email)
        
        // Assert
        assertNotNull(resultado)
        assertEquals(uid, resultado.firebaseUid)
        verify(colaboradorRepository).atualizarColaborador(any())
    }
    
    // Helper
    private fun criarColaboradorTeste(
        id: Long = 1L,
        nome: String = "Teste",
        email: String = "teste@exemplo.com",
        uid: String? = "test-uid",
        aprovado: Boolean = false
    ): Colaborador {
        return Colaborador(
            id = id,
            nome = nome,
            email = email,
            firebaseUid = uid,
            nivelAcesso = NivelAcesso.USER,
            aprovado = aprovado,
            ativo = true,
            primeiroAcesso = !aprovado,
            dataCadastro = System.currentTimeMillis(),
            dataUltimaAtualizacao = System.currentTimeMillis(),
            dataAprovacao = if (aprovado) System.currentTimeMillis() else null,
            aprovadoPor = if (aprovado) "Admin" else null
        )
    }
}
