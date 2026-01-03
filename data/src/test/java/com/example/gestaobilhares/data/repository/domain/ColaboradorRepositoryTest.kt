package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.ColaboradorDao
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

/**
 * Testes unitários para ColaboradorRepository
 * 
 * Testa:
 * - Busca de colaboradores (por UID, email, ID)
 * - Criação de colaboradores pendentes
 * - Inserção e atualização
 * - Verificação de existência
 */
class ColaboradorRepositoryTest {
    
    private lateinit var colaboradorDao: ColaboradorDao
    private lateinit var repository: ColaboradorRepository
    
    @Before
    fun setup() {
        colaboradorDao = mock()
        repository = ColaboradorRepository(colaboradorDao)
    }
    
    @Test
    fun `obterPorFirebaseUid retorna colaborador quando existe`() = runTest {
        // Arrange
        val uid = "test-uid-123"
        val colaborador = criarColaboradorTeste(uid = uid)
        whenever(colaboradorDao.obterPorFirebaseUid(uid)).thenReturn(colaborador)
        
        // Act
        val resultado = repository.obterPorFirebaseUid(uid)
        
        // Assert
        assertNotNull(resultado)
        assertEquals(uid, resultado?.firebaseUid)
        verify(colaboradorDao).obterPorFirebaseUid(uid)
    }
    
    @Test
    fun `obterPorFirebaseUid retorna null quando não existe`() = runTest {
        // Arrange
        val uid = "test-uid-123"
        whenever(colaboradorDao.obterPorFirebaseUid(uid)).thenReturn(null)
        
        // Act
        val resultado = repository.obterPorFirebaseUid(uid)
        
        // Assert
        assertNull(resultado)
    }
    
    @Test
    fun `obterPorEmail retorna colaborador quando existe`() = runTest {
        // Arrange
        val email = "teste@exemplo.com"
        val colaborador = criarColaboradorTeste(email = email)
        whenever(colaboradorDao.obterPorEmail(email)).thenReturn(colaborador)
        
        // Act
        val resultado = repository.obterPorEmail(email)
        
        // Assert
        assertNotNull(resultado)
        assertEquals(email, resultado?.email)
    }
    
    @Test
    fun `inserirColaborador nao duplica quando ja existe`() = runTest {
        // Arrange
        val colaborador = criarColaboradorTeste(uid = "test-uid", email = "teste@exemplo.com")
        whenever(colaboradorDao.obterPorFirebaseUid("test-uid")).thenReturn(colaborador)
        
        // Act
        val resultado = repository.inserirColaborador(colaborador)
        
        // Assert
        assertEquals(colaborador.id, resultado)
        verify(colaboradorDao, never()).inserir(any())
    }
    
    @Test
    fun `inserirColaborador insere quando não existe`() = runTest {
        // Arrange
        val colaborador = criarColaboradorTeste(uid = "test-uid", email = "teste@exemplo.com")
        whenever(colaboradorDao.obterPorFirebaseUid("test-uid")).thenReturn(null)
        whenever(colaboradorDao.obterPorEmail("teste@exemplo.com")).thenReturn(null)
        whenever(colaboradorDao.inserir(colaborador)).thenReturn(1L)
        
        // Act
        val resultado = repository.inserirColaborador(colaborador)
        
        // Assert
        assertEquals(1L, resultado)
        verify(colaboradorDao).inserir(colaborador)
    }
    
    @Test
    fun `criarColaboradorPendenteLocal cria colaborador pendente`() = runTest {
        // Arrange
        val uid = "test-uid-123"
        val email = "novo@exemplo.com"
        whenever(colaboradorDao.obterPorFirebaseUid(uid)).thenReturn(null)
        whenever(colaboradorDao.obterPorEmail(email)).thenReturn(null)
        whenever(colaboradorDao.inserir(any())).thenReturn(1L)
        
        // Act
        val resultado = repository.criarColaboradorPendenteLocal(uid, email)
        
        // Assert
        assertNotNull(resultado)
        resultado?.let {
            assertEquals(uid, it.firebaseUid)
            assertEquals(email, it.email)
            assertEquals(false, it.aprovado)
            assertEquals(true, it.ativo)
            assertEquals(true, it.primeiroAcesso)
            assertEquals(NivelAcesso.USER, it.nivelAcesso)
        }
        verify(colaboradorDao).inserir(any())
    }
    
    @Test
    fun `criarColaboradorPendenteLocal cria superadmin quando email e rossinys`() = runTest {
        // Arrange
        val uid = "test-uid-123"
        val email = "rossinys@gmail.com"
        whenever(colaboradorDao.obterPorFirebaseUid(uid)).thenReturn(null)
        whenever(colaboradorDao.obterPorEmail(email)).thenReturn(null)
        whenever(colaboradorDao.inserir(any())).thenReturn(1L)
        
        // Act
        val resultado = repository.criarColaboradorPendenteLocal(uid, email)
        
        // Assert
        assertNotNull(resultado)
        assertEquals(true, resultado?.aprovado)
        assertEquals(NivelAcesso.ADMIN, resultado?.nivelAcesso)
        assertEquals(false, resultado?.primeiroAcesso)
    }
    
    @Test
    fun `existeLocalmente retorna true quando existe por UID`() = runTest {
        // Arrange
        val uid = "test-uid-123"
        val colaborador = criarColaboradorTeste(uid = uid)
        whenever(colaboradorDao.obterPorFirebaseUid(uid)).thenReturn(colaborador)
        
        // Act
        val resultado = repository.existeLocalmente(uid, "teste@exemplo.com")
        
        // Assert
        assertTrue(resultado)
    }
    
    @Test
    fun `existeLocalmente retorna true quando existe por email`() = runTest {
        // Arrange
        val email = "teste@exemplo.com"
        val colaborador = criarColaboradorTeste(email = email)
        whenever(colaboradorDao.obterPorFirebaseUid(any())).thenReturn(null)
        whenever(colaboradorDao.obterPorEmail(email)).thenReturn(colaborador)
        
        // Act
        val resultado = repository.existeLocalmente("", email)
        
        // Assert
        assertTrue(resultado)
    }
    
    @Test
    fun `atualizarColaborador chama dao atualizar`() = runTest {
        // Arrange
        val colaborador = criarColaboradorTeste()
        
        // Act
        repository.atualizarColaborador(colaborador)
        
        // Assert
        verify(colaboradorDao).atualizar(colaborador)
    }
    
    // Helper
    private fun criarColaboradorTeste(
        id: Long = 1L,
        nome: String = "Teste",
        email: String = "teste@exemplo.com",
        uid: String? = "test-uid"
    ): Colaborador {
        return Colaborador(
            id = id,
            nome = nome,
            email = email,
            firebaseUid = uid,
            nivelAcesso = NivelAcesso.USER,
            aprovado = false,
            ativo = true,
            primeiroAcesso = true,
            dataCadastro = System.currentTimeMillis(),
            dataUltimaAtualizacao = System.currentTimeMillis()
        )
    }
}
