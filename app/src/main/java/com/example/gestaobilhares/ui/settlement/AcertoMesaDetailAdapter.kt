package com.example.gestaobilhares.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.databinding.ItemAcertoMesaDetailBinding
import java.text.NumberFormat
import java.util.*

class AcertoMesaDetailAdapter(
    private val mesas: List<AcertoMesa>,
    private val tipoAcerto: String = "Presencial",
    private val panoTrocado: Boolean = false,
    private val numeroPano: String? = null
) : RecyclerView.Adapter<AcertoMesaDetailAdapter.AcertoMesaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AcertoMesaViewHolder {
        val binding = ItemAcertoMesaDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AcertoMesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AcertoMesaViewHolder, position: Int) {
        holder.bind(mesas[position])
    }

    override fun getItemCount(): Int = mesas.size

    inner class AcertoMesaViewHolder(private val binding: ItemAcertoMesaDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: AcertoMesa) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            
            // Informações básicas da mesa
            binding.tvMesaNumero.text = "Mesa ${mesa.mesaId}"
            binding.tvTipoMesa.text = "" // Não há tipoMesa em AcertoMesa
            
            // Relógios e fichas
            binding.tvRelogioInicial.text = mesa.relogioInicial.toString()
            binding.tvRelogioFinal.text = mesa.relogioFinal.toString()
            binding.tvFichasJogadas.text = mesa.fichasJogadas.toString()
            
            // Valores
            binding.tvValorFixo.text = formatter.format(mesa.valorFixo)
            binding.tvSubtotal.text = formatter.format(mesa.subtotal)
            
            // Informações do acerto
            binding.tvTipoAcerto.text = "Tipo: $tipoAcerto"
            binding.tvPanoTrocado.text = if (panoTrocado) {
                "Pano: ${numeroPano ?: "N/A"}"
            } else {
                "Pano: Não trocado"
            }
        }
    }
} 