import org.junit.Test
import org.junit.Assert.*

class ReformaFilterTest {

    @Test
    fun `filtro identifica troca de pano do acerto - texto padrao`() {
        val observacao = "Troca realizada durante acerto"

        val contemAcerto = observacao.contains("acerto", ignoreCase = true)
        val contemContexto = observacao.contains("durante", ignoreCase = true) ||
                              observacao.contains("via acerto", ignoreCase = true) ||
                              observacao.contains("realizada", ignoreCase = true)

        assertTrue(contemAcerto && contemContexto)
    }

    @Test
    fun `filtro identifica troca de pano do acerto - variacao de texto`() {
        val observacoes = listOf(
            "Troca de pano realizada durante acerto",
            "Troca durante acerto - Pano: P16",
            "Pano trocado via acerto",
            "Acerto - troca realizada"
        )

        observacoes.forEach { obs ->
            val contemAcerto = obs.contains("acerto", ignoreCase = true)
            val contemContexto = obs.contains("durante", ignoreCase = true) ||
                                  obs.contains("via acerto", ignoreCase = true) ||
                                  obs.contains("realizada", ignoreCase = true)

            assertTrue("Falhou para: $obs", contemAcerto && contemContexto)
        }
    }

    @Test
    fun `filtro NAO identifica reforma manual`() {
        val observacoes = listOf(
            "Troca de pano via reforma",
            "Reforma completa da mesa",
            "Manutenção preventiva"
        )

        observacoes.forEach { obs ->
            val contemAcerto = obs.contains("acerto", ignoreCase = true)
            val contemContexto = obs.contains("durante", ignoreCase = true) ||
                                  obs.contains("via acerto", ignoreCase = true) ||
                                  obs.contains("realizada", ignoreCase = true)

            assertFalse("Falhou para: $obs", contemAcerto && contemContexto)
        }
    }

    @Test
    fun `filtro NAO identifica texto com acerto mas sem contexto`() {
        val observacoes = listOf(
            "Acerto de contas",
            "Relatório de acerto",
            "Acerto financeiro"
        )

        observacoes.forEach { obs ->
            val contemAcerto = obs.contains("acerto", ignoreCase = true)
            val contemContexto = obs.contains("durante", ignoreCase = true) ||
                                  obs.contains("via acerto", ignoreCase = true) ||
                                  obs.contains("realizada", ignoreCase = true)

            assertFalse("Deveria rejeitar: $obs", contemAcerto && contemContexto)
        }
    }

    @Test
    fun `filtro lida com observacoes nulas ou vazias`() {
        val observacoes = listOf(null, "", "   ")

        observacoes.forEach { obs ->
            val resultado = obs?.let { 
                val contemAcerto = it.contains("acerto", ignoreCase = true)
                val contemContexto = it.contains("durante", ignoreCase = true) ||
                                      it.contains("via acerto", ignoreCase = true) ||
                                      it.contains("realizada", ignoreCase = true)
                contemAcerto && contemContexto
            } == true

            assertFalse("Deveria rejeitar observação nula/vazia: $obs", resultado)
        }
    }
}
