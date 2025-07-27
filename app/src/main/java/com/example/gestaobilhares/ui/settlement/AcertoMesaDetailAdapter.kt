package com.example.gestaobilhares.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.databinding.ItemAcertoMesaDetailBinding
import java.text.NumberFormat
import java.util.*
import android.view.View
import com.example.gestaobilhares.utils.AppLogger

/**
 * Adapter melhorado para exibir detalhes das mesas do acerto
 * Suporta dados completos das mesas incluindo numeração e tipo
 */
class AcertoMesaDetailAdapter(
    private val mesas: List<AcertoMesa>,
    private val tipoAcerto: String = "Presencial",
    private val panoTrocado: Boolean = false,
    private val numeroPano: String? = null,
    private val mesasCompletas: Map<Long, MesaCompleta> = emptyMap()
) : RecyclerView.Adapter<AcertoMesaDetailAdapter.AcertoMesaViewHolder>() {

    init {
        AppLogger.log("AcertoMesaDetailAdapter", "=== ADAPTER INICIALIZADO ===")
        AppLogger.log("AcertoMesaDetailAdapter", "Total de mesas recebidas: ${mesas.size}")
        AppLogger.log("AcertoMesaDetailAdapter", "Tipo de acerto: $tipoAcerto")
        AppLogger.log("AcertoMesaDetailAdapter", "Pano trocado: $panoTrocado")
        AppLogger.log("AcertoMesaDetailAdapter", "Número do pano: $numeroPano")
        AppLogger.log("AcertoMesaDetailAdapter", "Mesas completas disponíveis: ${mesasCompletas.size}")
        
        // ✅ VERIFICAÇÃO CRÍTICA: Se não há mesas, isso é um erro
        if (mesas.isEmpty()) {
            AppLogger.log("AcertoMesaDetailAdapter", "❌ PROBLEMA CRÍTICO: Lista de mesas está vazia!")
            AppLogger.log("AcertoMesaDetailAdapter", "O adapter não vai exibir nenhuma mesa")
        }
        
        mesas.forEachIndexed { index, mesa ->
            AppLogger.log("AcertoMesaDetailAdapter", "=== MESA ${index + 1} NO ADAPTER ===")
            AppLogger.log("AcertoMesaDetailAdapter", "Mesa ID: ${mesa.mesaId}")
            AppLogger.log("AcertoMesaDetailAdapter", "Acerto ID: ${mesa.acertoId}")
            AppLogger.log("AcertoMesaDetailAdapter", "Subtotal: R$ ${mesa.subtotal}")
            AppLogger.log("AcertoMesaDetailAdapter", "Valor fixo: R$ ${mesa.valorFixo}")
            AppLogger.log("AcertoMesaDetailAdapter", "Fichas jogadas: ${mesa.fichasJogadas}")
        }
    }

    /**
     * Data class para armazenar dados completos da mesa
     */
    data class MesaCompleta(
        val numero: String,
        val tipo: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AcertoMesaViewHolder {
        AppLogger.log("AcertoMesaDetailAdapter", "Criando ViewHolder para posição")
        val binding = ItemAcertoMesaDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AcertoMesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AcertoMesaViewHolder, position: Int) {
        AppLogger.log("AcertoMesaDetailAdapter", "=== BINDING POSIÇÃO $position ===")
        AppLogger.log("AcertoMesaDetailAdapter", "Mesa ID: ${mesas[position].mesaId}")
        AppLogger.log("AcertoMesaDetailAdapter", "Mesa Subtotal: ${mesas[position].subtotal}")
        holder.bind(mesas[position])
    }

    override fun getItemCount(): Int {
        val count = mesas.size
        AppLogger.log("AcertoMesaDetailAdapter", "getItemCount() chamado: $count")
        return count
    }

    inner class AcertoMesaViewHolder(private val binding: ItemAcertoMesaDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: AcertoMesa) {
            AppLogger.log("AcertoMesaDetailAdapter", "=== BINDING MESA ${mesa.mesaId} ===")
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val mesaCompleta = mesasCompletas[mesa.mesaId]
            val numeroMesa = mesaCompleta?.numero ?: mesa.mesaId.toString()
            val tipoMesa = mesaCompleta?.tipo ?: "Sinuca"
            
            AppLogger.log("AcertoMesaDetailAdapter", "Número da mesa: $numeroMesa")
            AppLogger.log("AcertoMesaDetailAdapter", "Tipo da mesa: $tipoMesa")
            AppLogger.log("AcertoMesaDetailAdapter", "Subtotal: ${formatter.format(mesa.subtotal)}")
            
            binding.tvMesaNumero.text = "Mesa $numeroMesa"
            binding.tvTipoMesa.text = tipoMesa

            if (mesa.valorFixo > 0) {
                // Valor fixo: mostrar valor mensal, esconder relógios/fichas
                AppLogger.log("AcertoMesaDetailAdapter", "Configurando para valor fixo: ${formatter.format(mesa.valorFixo)}")
                binding.layoutRelogiosFichas.visibility = View.GONE
                binding.layoutValorMensal.visibility = View.VISIBLE
                binding.tvValorMensal.text = "${formatter.format(mesa.valorFixo)}/mês"
            } else {
                // Fichas: mostrar relógios/fichas, esconder valor mensal
                AppLogger.log("AcertoMesaDetailAdapter", "Configurando para fichas: ${mesa.relogioInicial} → ${mesa.relogioFinal}")
                binding.layoutRelogiosFichas.visibility = View.VISIBLE
                binding.layoutValorMensal.visibility = View.GONE
                binding.tvRelogioInicial.text = mesa.relogioInicial.toString()
                binding.tvRelogioFinal.text = mesa.relogioFinal.toString()
                binding.tvFichasJogadas.text = mesa.fichasJogadas.toString()
            }
            // Subtotal sempre visível
            binding.tvSubtotal.text = formatter.format(mesa.subtotal)
            // Informações adicionais
            binding.tvTipoAcerto.text = tipoAcerto
            binding.tvPanoTrocado.text = if (panoTrocado) {
                "Pano ${numeroPano ?: "N/A"}"
            } else {
                "Não trocado"
            }
            
            AppLogger.log("AcertoMesaDetailAdapter", "Mesa ${mesa.mesaId} configurada com sucesso")
        }
    }
} 