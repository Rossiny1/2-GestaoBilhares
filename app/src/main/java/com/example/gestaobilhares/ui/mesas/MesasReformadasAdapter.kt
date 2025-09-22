package com.example.gestaobilhares.ui.mesas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.databinding.ItemMesaReformadaBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para a lista de mesas reformadas.
 */
class MesasReformadasAdapter(
    private val onItemClick: (MesaReformada) -> Unit
) : ListAdapter<MesaReformada, MesasReformadasAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

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

        fun bind(mesa: MesaReformada) {
            // Número da mesa
            binding.tvNumeroMesa.text = "Mesa ${mesa.numeroMesa}"
            
            // Data da reforma
            binding.tvDataReforma.text = dateFormat.format(mesa.dataReforma)
            
            // Tipo da mesa
            binding.tvTipoMesa.text = "${mesa.tipoMesa} - ${mesa.tamanhoMesa}"
            
            // Itens reformados
            val itensReformados = buildString {
                val itens = mutableListOf<String>()
                if (mesa.pintura) itens.add("Pintura")
                if (mesa.tabela) itens.add("Tabela")
                if (mesa.panos) {
                    val panosText = if (mesa.numeroPanos != null) {
                        "Panos (${mesa.numeroPanos})"
                    } else {
                        "Panos"
                    }
                    itens.add(panosText)
                }
                if (mesa.outros) itens.add("Outros")
                
                append(itens.joinToString(", "))
            }
            binding.tvItensReformados.text = itensReformados
            
            // Observações
            if (!mesa.observacoes.isNullOrBlank()) {
                binding.tvObservacoes.text = "Observações: ${mesa.observacoes}"
                binding.tvObservacoes.visibility = View.VISIBLE
            } else {
                binding.tvObservacoes.visibility = View.GONE
            }
            
            // Click listener
            binding.root.setOnClickListener {
                onItemClick(mesa)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MesaReformada>() {
        override fun areItemsTheSame(oldItem: MesaReformada, newItem: MesaReformada): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MesaReformada, newItem: MesaReformada): Boolean {
            return oldItem == newItem
        }
    }
}
