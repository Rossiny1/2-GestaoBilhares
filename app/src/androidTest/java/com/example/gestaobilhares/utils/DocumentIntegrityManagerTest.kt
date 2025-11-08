package com.example.gestaobilhares.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ✅ FASE 12.2: Testes instrumentados para DocumentIntegrityManager
 * 
 * Testa funcionalidades de integridade de documentos:
 * - Geração de hash SHA-256 para documentos PDF
 * - Geração de hash para assinaturas (Bitmap)
 * - Geração de hash para dados de assinatura (pontos)
 * - Verificação de integridade
 * - Validação de formato de hash
 */
@RunWith(AndroidJUnit4::class)
class DocumentIntegrityManagerTest {

    private lateinit var context: Context
    private lateinit var integrityManager: DocumentIntegrityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        integrityManager = DocumentIntegrityManager(context)
    }

    @Test
    fun `generateDocumentHash deve gerar hash válido para PDF`() {
        val pdfBytes = "PDF_CONTENT_TEST_123".toByteArray()
        val hash = integrityManager.generateDocumentHash(pdfBytes)
        
        assertNotNull("Hash não deve ser nulo", hash)
        assertTrue("Hash não deve estar vazio", hash.isNotEmpty())
        assertTrue("Hash deve ser válido", integrityManager.isValidHash(hash))
    }

    @Test
    fun `generateDocumentHash deve gerar hash consistente para mesmo conteúdo`() {
        val pdfBytes = "PDF_CONTENT_TEST_123".toByteArray()
        val hash1 = integrityManager.generateDocumentHash(pdfBytes)
        val hash2 = integrityManager.generateDocumentHash(pdfBytes)
        
        assertEquals("Hash deve ser consistente para mesmo conteúdo", hash1, hash2)
    }

    @Test
    fun `generateDocumentHash deve gerar hash diferente para conteúdo diferente`() {
        val pdfBytes1 = "PDF_CONTENT_TEST_123".toByteArray()
        val pdfBytes2 = "PDF_CONTENT_TEST_456".toByteArray()
        
        val hash1 = integrityManager.generateDocumentHash(pdfBytes1)
        val hash2 = integrityManager.generateDocumentHash(pdfBytes2)
        
        assertNotEquals("Hash deve ser diferente para conteúdo diferente", hash1, hash2)
    }

    @Test
    fun `generateSignatureHash deve gerar hash válido para Bitmap`() {
        val bitmap = createTestBitmap()
        val hash = integrityManager.generateSignatureHash(bitmap)
        
        assertNotNull("Hash não deve ser nulo", hash)
        assertTrue("Hash não deve estar vazio", hash.isNotEmpty())
        assertTrue("Hash deve ser válido", integrityManager.isValidHash(hash))
    }

    @Test
    fun `generateSignatureHash deve gerar hash consistente para mesmo Bitmap`() {
        val bitmap = createTestBitmap()
        val hash1 = integrityManager.generateSignatureHash(bitmap)
        val hash2 = integrityManager.generateSignatureHash(bitmap)
        
        assertEquals("Hash deve ser consistente para mesmo Bitmap", hash1, hash2)
    }

    @Test
    fun `generateSignatureDataHash deve gerar hash válido para pontos de assinatura`() {
        val signaturePoints = listOf(
            SignaturePoint(10f, 20f, 0.5f, 1000L, 5.0f),
            SignaturePoint(30f, 40f, 0.7f, 2000L, 6.0f),
            SignaturePoint(50f, 60f, 0.9f, 3000L, 7.0f)
        )
        
        val hash = integrityManager.generateSignatureDataHash(signaturePoints)
        
        assertNotNull("Hash não deve ser nulo", hash)
        assertTrue("Hash não deve estar vazio", hash.isNotEmpty())
        assertTrue("Hash deve ser válido", integrityManager.isValidHash(hash))
    }

    @Test
    fun `generateSignatureDataHash deve gerar hash consistente para mesmos pontos`() {
        val signaturePoints = listOf(
            SignaturePoint(10f, 20f, 0.5f, 1000L, 5.0f),
            SignaturePoint(30f, 40f, 0.7f, 2000L, 6.0f)
        )
        
        val hash1 = integrityManager.generateSignatureDataHash(signaturePoints)
        val hash2 = integrityManager.generateSignatureDataHash(signaturePoints)
        
        assertEquals("Hash deve ser consistente para mesmos pontos", hash1, hash2)
    }

    @Test
    fun `generateSignatureDataHash deve gerar hash diferente para pontos diferentes`() {
        val points1 = listOf(
            SignaturePoint(10f, 20f, 0.5f, 1000L, 5.0f)
        )
        val points2 = listOf(
            SignaturePoint(30f, 40f, 0.7f, 2000L, 6.0f)
        )
        
        val hash1 = integrityManager.generateSignatureDataHash(points1)
        val hash2 = integrityManager.generateSignatureDataHash(points2)
        
        assertNotEquals("Hash deve ser diferente para pontos diferentes", hash1, hash2)
    }

    @Test
    fun `verifyDocumentIntegrity deve retornar true para hashes iguais`() {
        val pdfBytes = "PDF_CONTENT_TEST".toByteArray()
        val hash = integrityManager.generateDocumentHash(pdfBytes)
        
        val isValid = integrityManager.verifyDocumentIntegrity(hash, hash)
        
        assertTrue("Integridade deve ser válida para hashes iguais", isValid)
    }

    @Test
    fun `verifyDocumentIntegrity deve retornar false para hashes diferentes`() {
        val pdfBytes1 = "PDF_CONTENT_TEST_1".toByteArray()
        val pdfBytes2 = "PDF_CONTENT_TEST_2".toByteArray()
        
        val hash1 = integrityManager.generateDocumentHash(pdfBytes1)
        val hash2 = integrityManager.generateDocumentHash(pdfBytes2)
        
        val isValid = integrityManager.verifyDocumentIntegrity(hash1, hash2)
        
        assertFalse("Integridade deve ser inválida para hashes diferentes", isValid)
    }

    @Test
    fun `verifySignatureIntegrity deve retornar true para hashes iguais`() {
        val bitmap = createTestBitmap()
        val hash = integrityManager.generateSignatureHash(bitmap)
        
        val isValid = integrityManager.verifySignatureIntegrity(hash, hash)
        
        assertTrue("Integridade da assinatura deve ser válida para hashes iguais", isValid)
    }

    @Test
    fun `verifySignatureIntegrity deve retornar false para hashes diferentes`() {
        val bitmap1 = createTestBitmap(100, 100)
        val bitmap2 = createTestBitmap(200, 200)
        
        val hash1 = integrityManager.generateSignatureHash(bitmap1)
        val hash2 = integrityManager.generateSignatureHash(bitmap2)
        
        val isValid = integrityManager.verifySignatureIntegrity(hash1, hash2)
        
        assertFalse("Integridade da assinatura deve ser inválida para hashes diferentes", isValid)
    }

    @Test
    fun `generateCombinedHash deve gerar hash válido`() {
        val documentHash = integrityManager.generateDocumentHash("DOC".toByteArray())
        val signatureHash = integrityManager.generateSignatureHash(createTestBitmap())
        val metadataHash = integrityManager.generateSignatureDataHash(
            listOf(SignaturePoint(10f, 20f, 0.5f, 1000L, 5.0f))
        )
        
        val combinedHash = integrityManager.generateCombinedHash(
            documentHash,
            signatureHash,
            metadataHash
        )
        
        assertNotNull("Hash combinado não deve ser nulo", combinedHash)
        assertTrue("Hash combinado não deve estar vazio", combinedHash.isNotEmpty())
        assertTrue("Hash combinado deve ser válido", integrityManager.isValidHash(combinedHash))
    }

    @Test
    fun `generateCombinedHash deve gerar hash consistente para mesmos inputs`() {
        val documentHash = integrityManager.generateDocumentHash("DOC".toByteArray())
        val signatureHash = integrityManager.generateSignatureHash(createTestBitmap())
        val metadataHash = integrityManager.generateSignatureDataHash(
            listOf(SignaturePoint(10f, 20f, 0.5f, 1000L, 5.0f))
        )
        
        val hash1 = integrityManager.generateCombinedHash(documentHash, signatureHash, metadataHash)
        val hash2 = integrityManager.generateCombinedHash(documentHash, signatureHash, metadataHash)
        
        assertEquals("Hash combinado deve ser consistente", hash1, hash2)
    }

    @Test
    fun `generateDeviceHash deve gerar hash válido`() {
        val deviceId = "TEST_DEVICE_ID"
        val timestamp = System.currentTimeMillis()
        val userAgent = "TestUserAgent/1.0"
        
        val hash = integrityManager.generateDeviceHash(deviceId, timestamp, userAgent)
        
        assertNotNull("Hash do dispositivo não deve ser nulo", hash)
        assertTrue("Hash do dispositivo não deve estar vazio", hash.isNotEmpty())
        assertTrue("Hash do dispositivo deve ser válido", integrityManager.isValidHash(hash))
    }

    @Test
    fun `generateDeviceHash deve gerar hash diferente para timestamps diferentes`() {
        val deviceId = "TEST_DEVICE_ID"
        val timestamp1 = 1000L
        val timestamp2 = 2000L
        val userAgent = "TestUserAgent/1.0"
        
        val hash1 = integrityManager.generateDeviceHash(deviceId, timestamp1, userAgent)
        val hash2 = integrityManager.generateDeviceHash(deviceId, timestamp2, userAgent)
        
        assertNotEquals("Hash deve ser diferente para timestamps diferentes", hash1, hash2)
    }

    @Test
    fun `isValidHash deve retornar true para hash válido`() {
        val pdfBytes = "TEST".toByteArray()
        val hash = integrityManager.generateDocumentHash(pdfBytes)
        
        assertTrue("Hash gerado deve ser válido", integrityManager.isValidHash(hash))
    }

    @Test
    fun `isValidHash deve retornar false para string vazia`() {
        assertFalse("Hash vazio deve ser inválido", integrityManager.isValidHash(""))
    }

    @Test
    fun `isValidHash deve retornar false para string muito curta`() {
        assertFalse("Hash muito curto deve ser inválido", integrityManager.isValidHash("abc"))
    }

    @Test
    fun `isValidHash deve retornar false para string com caracteres inválidos`() {
        assertFalse("Hash com caracteres inválidos deve ser inválido", 
            integrityManager.isValidHash("INVALID_HASH_WITH_SPECIAL_CHARS!@#"))
    }

    /**
     * Cria um Bitmap de teste para uso nos testes
     */
    private fun createTestBitmap(width: Int = 100, height: Int = 100): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 5f
        }
        canvas.drawLine(10f, 10f, 90f, 90f, paint)
        return bitmap
    }
}

