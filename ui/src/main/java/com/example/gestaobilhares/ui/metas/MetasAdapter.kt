package com.example.gestaobilhares.ui.metas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.MetaRotaResumo
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.ui.databinding.ItemMetaDetalheBinding
import com.example.gestaobilhares.ui.databinding.ItemMetaRotaBinding
import java.text.NumberFormat
import java.util.Locale

class MetasAdapter(private val onDetailsClick: (MetaRotaResumo) -> Unit) :
    ListAdapter<MetaRotaResumo, MetasAdapter.MetaRotaViewHolder>(MetaRotaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaRotaViewHolder {
        val binding = ItemMetaRotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MetaRotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetaRotaViewHolder, position: Int) {
        val metaRota = getItem(position)
        holder.bind(metaRota)
    }

    inner class MetaRotaViewHolder(private val binding: ItemMetaRotaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(metaRota: MetaRotaResumo) {
            binding.tvRotaNome.text = metaRota.rota.nome
            binding.tvPeriodoCiclo.text = "Ciclo ${metaRota.cicloAtual} - ${metaRota.anoCiclo}"

            // RecyclerView aninhado para metas
            val detalheAdapter = MetaDetalheAdapter()
            binding.rvMetas.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = detalheAdapter
                isNestedScrollingEnabled = false
            }
            detalheAdapter.submitList(metaRota.metas)
            
            android.util.Log.d("MetasAdapter", "Configurando RecyclerView com ${metaRota.metas.size} metas para rota ${metaRota.rota.nome}")

            binding.btnDetalhes.setOnClickListener {
                onDetailsClick(metaRota)
            }
        }
    }

    private class MetaRotaDiffCallback : DiffUtil.ItemCallback<MetaRotaResumo>() {
        override fun areItemsTheSame(oldItem: MetaRotaResumo, newItem: MetaRotaResumo): Boolean {
            return oldItem.rota.id == newItem.rota.id &&
                   oldItem.cicloAtual == newItem.cicloAtual &&
                   oldItem.anoCiclo == newItem.anoCiclo
        }

        override fun areContentsTheSame(oldItem: MetaRotaResumo, newItem: MetaRotaResumo): Boolean {
            return oldItem == newItem
        }
    }
}
