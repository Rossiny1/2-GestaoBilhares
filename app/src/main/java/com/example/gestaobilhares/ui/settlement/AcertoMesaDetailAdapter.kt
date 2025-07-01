package com.example.gestaobilhares.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.databinding.ItemAcertoMesaDetailBinding
import java.text.NumberFormat
import java.util.*

class AcertoMesaDetailAdapter(
    private val mesas: List<AcertoMesa>
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
            binding.tvMesaNumero.text = "Mesa ${mesa.mesaId}"
            binding.tvTipoMesa.text = "" // Não há tipoMesa em AcertoMesa
            binding.tvRelogioInicial.text = mesa.relogioInicial.toString()
            binding.tvRelogioFinal.text = mesa.relogioFinal.toString()
            binding.tvFichasJogadas.text = mesa.fichasJogadas.toString()
            binding.tvValorFixo.text = formatter.format(mesa.valorFixo)
            binding.tvSubtotal.text = formatter.format(mesa.subtotal)
        }
    }
} 