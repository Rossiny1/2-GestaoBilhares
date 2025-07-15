package com.example.gestaobilhares.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.databinding.ItemAcertoMesaDetailBinding
import java.text.NumberFormat
import java.util.*
import android.view.View
import android.util.Log

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
        Log.d("AcertoMesaDetailAdapter", "=== ADAPTER INICIALIZADO ===")
        Log.d("AcertoMesaDetailAdapter", "Total de mesas: ${mesas.size}")
        mesas.forEachIndexed { index, mesa ->
            Log.d("AcertoMesaDetailAdapter", "Mesa $index: ID=${mesa.mesaId}, Subtotal=${mesa.subtotal}")
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
        Log.d("AcertoMesaDetailAdapter", "Criando ViewHolder para posição")
        val binding = ItemAcertoMesaDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AcertoMesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AcertoMesaViewHolder, position: Int) {
        Log.d("AcertoMesaDetailAdapter", "=== BINDING POSIÇÃO $position ===")
        Log.d("AcertoMesaDetailAdapter", "Mesa ID: ${mesas[position].mesaId}")
        Log.d("AcertoMesaDetailAdapter", "Mesa Subtotal: ${mesas[position].subtotal}")
        holder.bind(mesas[position])
    }

    override fun getItemCount(): Int {
        val count = mesas.size
        Log.d("AcertoMesaDetailAdapter", "getItemCount() chamado: $count")
        return count
    }

    inner class AcertoMesaViewHolder(private val binding: ItemAcertoMesaDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: AcertoMesa) {
            Log.d("AcertoMesaDetailAdapter", "=== BINDING MESA ${mesa.mesaId} ===")
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val mesaCompleta = mesasCompletas[mesa.mesaId]
            val numeroMesa = mesaCompleta?.numero ?: mesa.mesaId.toString()
            val tipoMesa = mesaCompleta?.tipo ?: "Sinuca"
            
            Log.d("AcertoMesaDetailAdapter", "Número da mesa: $numeroMesa")
            Log.d("AcertoMesaDetailAdapter", "Tipo da mesa: $tipoMesa")
            Log.d("AcertoMesaDetailAdapter", "Subtotal: ${formatter.format(mesa.subtotal)}")
            
            binding.tvMesaNumero.text = "Mesa $numeroMesa"
            binding.tvTipoMesa.text = tipoMesa

            if (mesa.valorFixo > 0) {
                // Valor fixo: mostrar valor mensal, esconder relógios/fichas
                Log.d("AcertoMesaDetailAdapter", "Configurando para valor fixo: ${formatter.format(mesa.valorFixo)}")
                binding.layoutRelogiosFichas.visibility = View.GONE
                binding.layoutValorMensal.visibility = View.VISIBLE
                binding.tvValorMensal.text = "${formatter.format(mesa.valorFixo)}/mês"
            } else {
                // Fichas: mostrar relógios/fichas, esconder valor mensal
                Log.d("AcertoMesaDetailAdapter", "Configurando para fichas: ${mesa.relogioInicial} → ${mesa.relogioFinal}")
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
            
            Log.d("AcertoMesaDetailAdapter", "Mesa ${mesa.mesaId} configurada com sucesso")
        }
    }
} 