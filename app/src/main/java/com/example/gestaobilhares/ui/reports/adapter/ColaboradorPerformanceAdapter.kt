package com.example.gestaobilhares.ui.reports.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemColaboradorPerformanceBinding
import com.example.gestaobilhares.ui.reports.viewmodel.ColaboradorPerformanceViewModel.PerformanceColaborador
import java.text.NumberFormat
import java.util.*

class ColaboradorPerformanceAdapter : ListAdapter<PerformanceColaborador, ColaboradorPerformanceAdapter.ViewHolder>(PerformanceDiffCallback()) {

    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemColaboradorPerformanceBinding.inflate(
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
        private val binding: ItemColaboradorPerformanceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(colaborador: PerformanceColaborador) {
            binding.apply {
                txtNomeColaborador.text = colaborador.nome
                txtFaturamento.text = formatter.format(colaborador.faturamento)
                txtClientesAcertados.text = colaborador.clientesAcertados.toString()
                txtMesasLocadas.text = colaborador.mesasLocadas.toString()
                txtStatusPerformance.text = colaborador.status
                
                // Definir cor do status baseado no texto
                val statusColor = when (colaborador.status.lowercase()) {
                    "excelente" -> android.graphics.Color.parseColor("#4CAF50")
                    "bom" -> android.graphics.Color.parseColor("#8BC34A")
                    "regular" -> android.graphics.Color.parseColor("#FF9800")
                    "ruim" -> android.graphics.Color.parseColor("#F44336")
                    else -> android.graphics.Color.parseColor("#9E9E9E")
                }
                txtStatusPerformance.setTextColor(statusColor)
            }
        }
    }

    private class PerformanceDiffCallback : DiffUtil.ItemCallback<PerformanceColaborador>() {
        override fun areItemsTheSame(oldItem: PerformanceColaborador, newItem: PerformanceColaborador): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PerformanceColaborador, newItem: PerformanceColaborador): Boolean {
            return oldItem == newItem
        }
    }
}
