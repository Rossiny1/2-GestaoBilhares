package com.example.gestaobilhares.ui.mesas.usecases

import android.util.Log
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.repository.AppRepository
import javax.inject.Inject

enum class OrigemTrocaPano {
    NOVA_REFORMA,
    ACERTO
}

data class TrocaPanoParams(
    val mesaId: Long,
    val numeroMesa: String,
    val panoNovoId: Long?,
    val dataManutencao: Long,
    val origem: OrigemTrocaPano,
    val descricao: String?,
    val observacao: String?,
    val nomeUsuario: String? = null // ✅ NOVO: Nome do usuário logado
)

class RegistrarTrocaPanoUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(params: TrocaPanoParams) {
        Log.d("DEBUG_CARDS", "════════════════════════════════════════")
        Log.d("DEBUG_CARDS", "🔵 USE CASE INICIADO")
        Log.d("DEBUG_CARDS", "   Mesa: ${params.numeroMesa} (ID: ${params.mesaId})")
        Log.d("DEBUG_CARDS", "   Origem: ${params.origem}")
        Log.d("DEBUG_CARDS", "   Pano ID: ${params.panoNovoId}")
        Log.d("DEBUG_CARDS", "   Descrição: ${params.descricao}")

        try {
            // 1. Buscar mesa
            Log.d("DEBUG_CARDS", "🔍 Buscando mesa ${params.mesaId}...")
            val mesa = appRepository.obterMesaPorId(params.mesaId)

            if (mesa == null) {
                Log.e("DEBUG_CARDS", "❌ ERRO: Mesa ${params.mesaId} não encontrada!")
                throw IllegalArgumentException("Mesa ${params.mesaId} não encontrada")
            }

            Log.d("DEBUG_CARDS", "✅ Mesa encontrada: ${mesa.numero} (Tipo: ${mesa.tipoMesa})")

            when (params.origem) {
                OrigemTrocaPano.NOVA_REFORMA -> {
                    // ✅ Fluxo atual: insere em MesaReformada
                    Log.d("DEBUG_CARDS", "📋 NOVA_REFORMA: Inserindo em MesaReformada")

                    val numeroPanoExtraido = extrairNumeroPano(params.descricao)
                    Log.d("DEBUG_CARDS", "🔍 Número pano extraído: $numeroPanoExtraido")

                    val mesaReformada = MesaReformada(
                        mesaId = params.mesaId,
                        numeroMesa = params.numeroMesa,
                        tipoMesa = mesa.tipoMesa,
                        tamanhoMesa = mesa.tamanho ?: TamanhoMesa.GRANDE,
                        pintura = false,
                        tabela = false,
                        panos = true,
                        numeroPanos = numeroPanoExtraido ?: params.panoNovoId?.toString() ?: "",
                        outros = false,
                        observacoes = params.observacao ?: "Troca de pano via reforma",
                        fotoReforma = null,
                        dataReforma = params.dataManutencao
                    )

                    Log.d("DEBUG_CARDS", "📝 MesaReformada criada:")
                    Log.d("DEBUG_CARDS", "   - mesaId: ${mesaReformada.mesaId}")
                    Log.d("DEBUG_CARDS", "   - numeroMesa: ${mesaReformada.numeroMesa}")
                    Log.d("DEBUG_CARDS", "   - panos: ${mesaReformada.panos}")
                    Log.d("DEBUG_CARDS", "   - numeroPanos: ${mesaReformada.numeroPanos}")
                    Log.d("DEBUG_CARDS", "   - observacoes: ${mesaReformada.observacoes}")
                    Log.d("DEBUG_CARDS", "   - dataReforma: ${mesaReformada.dataReforma}")

                    val idReforma = appRepository.inserirMesaReformada(mesaReformada)
                    Log.d("DEBUG_CARDS", "✅ MesaReformada inserida com ID: $idReforma")
                }

                OrigemTrocaPano.ACERTO -> {
                    // 🆕 NOVO FLUXO: insere em HistoricoManutencaoMesa
                    Log.d("DEBUG_CARDS", "📋 ACERTO: Inserindo em HistoricoManutencaoMesa")
                    Log.d("DEBUG_CARDS", "🔍 ANTES DO INSERT - Thread: ${Thread.currentThread().name}")

                    val historico = com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa(
                        mesaId = params.mesaId,
                        numeroMesa = params.numeroMesa.toString(),
                        tipoManutencao = com.example.gestaobilhares.data.entities.TipoManutencao.TROCA_PANO, // ✅ ESTRUTURADO
                        descricao = params.descricao,
                        dataManutencao = params.dataManutencao,
                        responsavel = params.nomeUsuario ?: "Acerto", // ✅ CORREÇÃO: Usa usuário real ou fallback
                        observacoes = params.observacao
                    )

                    Log.d("DEBUG_CARDS", "🔍 Dados do histórico:")
                    Log.d("DEBUG_CARDS", "   - mesaId: ${historico.mesaId}")
                    Log.d("DEBUG_CARDS", "   - numeroMesa: ${historico.numeroMesa}")
                    Log.d("DEBUG_CARDS", "   - tipoManutencao: ${historico.tipoManutencao}")
                    Log.d("DEBUG_CARDS", "   - responsavel: '${historico.responsavel}'")
                    Log.d("DEBUG_CARDS", "   - descricao: ${historico.descricao}")

                    val idHistorico = appRepository.inserirHistoricoManutencaoMesa(historico)
                    
                    Log.d("DEBUG_CARDS", "🔍 DEPOIS DO INSERT - Thread: ${Thread.currentThread().name}")
                    Log.d("DEBUG_CARDS", "✅ HistoricoManutencaoMesa inserido com ID: $idHistorico")
                    Log.d("DEBUG_CARDS", "🔍 ID válido? ${idHistorico > 0}")
                    Log.d("DEBUG_CARDS", "   - tipoManutencao: ${com.example.gestaobilhares.data.entities.TipoManutencao.TROCA_PANO}")
                    Log.d("DEBUG_CARDS", "   - responsavel: Acerto")
                }
            }

            // Atualizar pano atual da mesa (comum para ambos os fluxos)
            if (params.panoNovoId != null) {
                Log.d("DEBUG_CARDS", "🔄 Atualizando mesa com novo pano...")
                val mesaAtualizada = mesa.copy(
                    panoAtualId = params.panoNovoId,
                    dataUltimaTrocaPano = params.dataManutencao
                )
                appRepository.atualizarMesa(mesaAtualizada)
                Log.d("DEBUG_CARDS", "✅ Mesa atualizada com novo pano")
            }

            Log.d("DEBUG_CARDS", "🎉 USE CASE CONCLUÍDO COM SUCESSO!")
            Log.d("DEBUG_CARDS", "════════════════════════════════════════")

        } catch (e: Exception) {
            Log.e("DEBUG_CARDS", "❌❌❌ ERRO NO USE CASE ❌❌❌")
            Log.e("DEBUG_CARDS", "Mesa: ${params.numeroMesa}")
            Log.e("DEBUG_CARDS", "Origem: ${params.origem}")
            Log.e("DEBUG_CARDS", "Exception: ${e.javaClass.simpleName}")
            Log.e("DEBUG_CARDS", "Message: ${e.message}")
            Log.e("DEBUG_CARDS", "StackTrace:", e)
            Log.e("DEBUG_CARDS", "════════════════════════════════════════")
            throw e
        }
    }

    // Helper para extrair número do pano da descrição (ex: "Troca de pano - Pano: P123" -> "P123")
    private fun extrairNumeroPano(descricao: String?): String? {
        if (descricao == null) return null
        return Regex("""Pano:\s*(\w+)""").find(descricao)?.groupValues?.get(1)
    }
}
