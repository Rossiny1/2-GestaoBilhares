package com.example.gestaobilhares.ui.mesas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.databinding.ItemMesaReformadaBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para a lista de cards de reforma.
 * ✅ NOVO: Exibe cards unificados de reformas e acertos
 */
class MesasReformadasAdapter(
    private val onItemClick: (ReformaCard) -> Unit
) : ListAdapter<ReformaCard, MesasReformadasAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMesaReformadaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMesaReformadaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: ReformaCard) {
            binding.apply {
                when (card.origem) {
                    "HEADER_MESA" -> {
                        // ✅ TRATAMENTO ESPECIAL PARA HEADER
                        tvNumeroMesa.text = card.descricao  // "🏓 Mesa X - Y manutenção(ões)"
                        tvNumeroMesa.textSize = 18f
                        tvNumeroMesa.setTypeface(null, android.graphics.Typeface.BOLD)
                        
                        // Ocultar campos irrelevantes no header
                        tvDataReforma.visibility = View.GONE
                        tvTipoMesa.visibility = View.GONE
                        tvItensReformados.visibility = View.GONE
                        tvObservacoes.visibility = View.GONE
                        
                        // ✅ Header AGORA é clicável para navegação
                        root.setOnClickListener { onItemClick(card) }
                        root.isClickable = true
                    }
                    
                    "NOVA_REFORMA" -> {
                        // ✅ REFORMA MANUAL
                        tvNumeroMesa.text = "Mesa ${card.numeroMesa}"
                        tvNumeroMesa.textSize = 16f
                        tvNumeroMesa.setTypeface(null, android.graphics.Typeface.NORMAL)
                        tvDataReforma.text = dateTimeFormat.format(Date(card.data))
                        tvDataReforma.visibility = View.VISIBLE
                        tvTipoMesa.text = "Reforma Manual"
                        tvTipoMesa.visibility = View.VISIBLE
                        tvItensReformados.text = card.descricao
                        tvItensReformados.visibility = View.VISIBLE
                        
                        // Observações
                        if (!card.observacoes.isNullOrBlank()) {
                            tvObservacoes.text = "Obs: ${card.observacoes}"
                            tvObservacoes.visibility = View.VISIBLE
                        } else {
                            tvObservacoes.visibility = View.GONE
                        }
                        
                        root.setOnClickListener { onItemClick(card) }
                        root.isClickable = true
                    }
                    
                    "ACERTO" -> {
                        // ✅ ACERTO ESTRUTURADO (com responsável)
                        tvNumeroMesa.text = "Mesa ${card.numeroMesa}"
                        tvNumeroMesa.textSize = 16f
                        tvNumeroMesa.setTypeface(null, android.graphics.Typeface.NORMAL)
                        tvDataReforma.text = dateTimeFormat.format(Date(card.data))
                        tvDataReforma.visibility = View.VISIBLE
                        
                        // Mostrar responsável se disponível
                        if (!card.responsavel.isNullOrBlank()) {
                            tvTipoMesa.text = "Acerto - ${card.responsavel}"
                        } else {
                            tvTipoMesa.text = "Acerto"
                        }
                        tvTipoMesa.visibility = View.VISIBLE
                        
                        tvItensReformados.text = card.descricao
                        tvItensReformados.visibility = View.VISIBLE
                        
                        // Observações
                        if (!card.observacoes.isNullOrBlank()) {
                            tvObservacoes.text = "Obs: ${card.observacoes}"
                            tvObservacoes.visibility = View.VISIBLE
                        } else {
                            tvObservacoes.visibility = View.GONE
                        }
                        
                        root.setOnClickListener { onItemClick(card) }
                        root.isClickable = true
                    }
                    
                    "ACERTO_LEGACY" -> {
                        // ✅ ACERTO LEGACY (texto)
                        tvNumeroMesa.text = "Mesa ${card.numeroMesa}"
                        tvNumeroMesa.textSize = 16f
                        tvNumeroMesa.setTypeface(null, android.graphics.Typeface.NORMAL)
                        tvDataReforma.text = dateTimeFormat.format(Date(card.data))
                        tvDataReforma.visibility = View.VISIBLE
                        tvTipoMesa.text = "Acerto (Legacy)"
                        tvTipoMesa.visibility = View.VISIBLE
                        tvItensReformados.text = card.descricao
                        tvItensReformados.visibility = View.VISIBLE
                        
                        // Observações
                        if (!card.observacoes.isNullOrBlank()) {
                            tvObservacoes.text = "Obs: ${card.observacoes}"
                            tvObservacoes.visibility = View.VISIBLE
                        } else {
                            tvObservacoes.visibility = View.GONE
                        }
                        
                        root.setOnClickListener { onItemClick(card) }
                        root.isClickable = true
                    }
                    
                    else -> {
                        // ✅ FALLBACK (não deveria acontecer)
                        tvNumeroMesa.text = "Mesa ${card.numeroMesa}"
                        tvNumeroMesa.textSize = 16f
                        tvNumeroMesa.setTypeface(null, android.graphics.Typeface.NORMAL)
                        tvDataReforma.text = dateTimeFormat.format(Date(card.data))
                        tvDataReforma.visibility = View.VISIBLE
                        tvTipoMesa.text = "Tipo: ${card.origem}"
                        tvTipoMesa.visibility = View.VISIBLE
                        tvItensReformados.text = card.descricao
                        tvItensReformados.visibility = View.VISIBLE
                        tvObservacoes.visibility = View.GONE
                        
                        root.setOnClickListener { onItemClick(card) }
                        root.isClickable = true
                    }
                }
                
                // Total de reformas sempre invisível (não aplicável para cards individuais)
                tvTotalReformas.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReformaCard>() {
        override fun areItemsTheSame(oldItem: ReformaCard, newItem: ReformaCard): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReformaCard, newItem: ReformaCard): Boolean {
            return oldItem == newItem
        }
    }
}
