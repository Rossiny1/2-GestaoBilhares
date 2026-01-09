package com.example.gestaobilhares.sync.utils

import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.StatusAcerto
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.Date

class SyncUtilsTest {

    @Mock
    private lateinit var documentSnapshot: DocumentSnapshot

    @org.junit.Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `entityToMap should convert entity to Map correctly`() {
        // Given
        val acerto = Acerto(
            id = 1L,
            clienteId = 100L,
            colaboradorId = 200L,
            dataAcerto = System.currentTimeMillis(),
            periodoInicio = System.currentTimeMillis() - 86400000L,
            periodoFim = System.currentTimeMillis(),
            totalMesas = 10.0,
            debitoAnterior = 50.0,
            valorTotal = 150.0,
            desconto = 10.0,
            valorComDesconto = 140.0,
            valorRecebido = 140.0,
            debitoAtual = 0.0,
            status = StatusAcerto.PENDENTE,
            observacoes = "Test acerto",
            dataCriacao = System.currentTimeMillis(),
            dataFinalizacao = null,
            representante = null,
            tipoAcerto = null,
            panoTrocado = false,
            numeroPano = null,
            dadosExtrasJson = null,
            rotaId = 300L,
            cicloId = 400L
        )

        // When
        val result = SyncUtils().entityToMap(acerto)

        // Then
        assertThat(result).isNotNull()
        assertThat(result["id"]).isEqualTo(1L)
        assertThat(result["cliente_id"]).isEqualTo(100L)
        assertThat(result["colaborador_id"]).isEqualTo(200L)
        assertThat(result["rota_id"]).isEqualTo(300L)
        assertThat(result["ciclo_id"]).isEqualTo(400L)
        assertThat(result["valor_total"]).isEqualTo(150.0)
        assertThat(result["status"]).isEqualTo(StatusAcerto.PENDENTE)
    }

    @Test
    fun `entityToMap should handle null values correctly`() {
        // Given
        val acerto = Acerto(
            id = 1L,
            clienteId = 100L,
            colaboradorId = null,
            dataAcerto = System.currentTimeMillis(),
            periodoInicio = System.currentTimeMillis() - 86400000L,
            periodoFim = System.currentTimeMillis(),
            totalMesas = 0.0,
            debitoAnterior = 0.0,
            valorTotal = 0.0,
            desconto = 0.0,
            valorComDesconto = 0.0,
            valorRecebido = 0.0,
            debitoAtual = 0.0,
            status = StatusAcerto.PENDENTE,
            observacoes = null,
            dataCriacao = System.currentTimeMillis(),
            dataFinalizacao = null,
            representante = null,
            tipoAcerto = null,
            panoTrocado = false,
            numeroPano = null,
            dadosExtrasJson = null,
            rotaId = null,
            cicloId = null
        )

        // When
        val result = SyncUtils().entityToMap(acerto)

        // Then
        assertThat(result).isNotNull()
        assertThat(result["colaborador_id"]).isNull()
        assertThat(result["rota_id"]).isNull()
        assertThat(result["ciclo_id"]).isNull()
        assertThat(result["observacoes"]).isNull()
    }

    @Test
    fun `documentToAcerto should convert Firestore document to Acerto correctly`() {
        // Given
        val testData = mapOf(
            "id" to 1L,
            "cliente_id" to 100L,
            "colaborador_id" to 200L,
            "rota_id" to 300L,
            "ciclo_id" to 400L,
            "valor_total" to 150.0,
            "status" to "PENDENTE"
        )
        
        whenever(documentSnapshot.data).thenReturn(testData)
        whenever(documentSnapshot.id).thenReturn("1")
        whenever(documentSnapshot.contains("dataAcerto")).thenReturn(true)
        whenever(documentSnapshot.getDate("dataAcerto")).thenReturn(Date())

        // When
        val result = SyncUtils().documentToAcerto(documentSnapshot)

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(1L)
        assertThat(result.clienteId).isEqualTo(100L)
        assertThat(result.colaboradorId).isEqualTo(200L)
        assertThat(result.rotaId).isEqualTo(300L)
        assertThat(result.cicloId).isEqualTo(400L)
        assertThat(result.valorTotal).isEqualTo(150.0)
        assertThat(result.status).isEqualTo(StatusAcerto.PENDENTE)
        assertThat(result.dataAcerto).isNotNull()
    }

    @Test
    fun `documentToAcerto should return null for invalid document`() {
        // Given
        whenever(documentSnapshot.data).thenReturn(null)

        // When
        val result = SyncUtils().documentToAcerto(documentSnapshot)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `documentToAcerto should handle missing optional fields`() {
        // Given
        val testData = mapOf(
            "id" to 1L,
            "cliente_id" to 100L,
            "valor_total" to 150.0,
            "status" to "PENDENTE"
        )
        
        whenever(documentSnapshot.data).thenReturn(testData)
        whenever(documentSnapshot.id).thenReturn("1")
        whenever(documentSnapshot.contains("dataAcerto")).thenReturn(false)

        // When
        val result = SyncUtils().documentToAcerto(documentSnapshot)

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(1L)
        assertThat(result.clienteId).isEqualTo(100L)
        assertThat(result.colaboradorId).isNull()
        assertThat(result.valorTotal).isEqualTo(150.0)
        assertThat(result.rotaId).isNull()
        assertThat(result.cicloId).isNull()
        assertThat(result.dataAcerto).isNotNull() // Default value
    }

    @Test
    fun `entityToMap should handle Date conversion`() {
        // Given
        val testTimestamp = 1640995200000L // 2022-01-01 00:00:00 UTC
        val acerto = Acerto(
            id = 1L,
            clienteId = 100L,
            colaboradorId = 200L,
            dataAcerto = testTimestamp,
            periodoInicio = testTimestamp - 86400000L,
            periodoFim = testTimestamp,
            totalMesas = 10.0,
            debitoAnterior = 50.0,
            valorTotal = 150.0,
            desconto = 10.0,
            valorComDesconto = 140.0,
            valorRecebido = 140.0,
            debitoAtual = 0.0,
            status = StatusAcerto.PENDENTE,
            observacoes = "Test acerto",
            dataCriacao = testTimestamp,
            dataFinalizacao = null,
            representante = null,
            tipoAcerto = null,
            panoTrocado = false,
            numeroPano = null,
            dadosExtrasJson = null,
            rotaId = 300L,
            cicloId = 400L
        )

        // When
        val result = SyncUtils().entityToMap(acerto)

        // Then
        assertThat(result).isInstanceOf(Map::class.java)
        assertThat(result["data_acerto"]).isEqualTo(testTimestamp)
        assertThat(result["data_criacao"]).isEqualTo(testTimestamp)
    }
}
