package com.example.gestaobilhares.ui.clients
import com.example.gestaobilhares.ui.R

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.databinding.ItemSettlementHistoryBinding
// Using data class from ClientDetailViewModel
import java.text.NumberFormat
import java.util.*

/**
 * Adapter para histórico de acertos do cliente
 * FASE 4A - Implementação crítica
 */
class SettlementHistoryAdapter(
    private val onItemClick: (AcertoResumo) -> Unit
) : ListAdapter<AcertoResumo, SettlementHistoryAdapter.SettlementViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettlementViewHolder {
        val binding = ItemSettlementHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        android.util.Log.d("SettlementHistoryAdapter", "Criando ViewHolder para posição $viewType")
        return SettlementViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SettlementViewHolder, position: Int) {
        val item = getItem(position)
        android.util.Log.d("SettlementHistoryAdapter", "Binding item $position: ID=${item.id}, Data=${item.data}, Valor=${item.valorTotal}")
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        android.util.Log.d("SettlementHistoryAdapter", "getItemCount() retornou: $count")
        return count
    }

    inner class SettlementViewHolder(
        private val binding: ItemSettlementHistoryBinding,
        private val onItemClick: (AcertoResumo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(acerto: AcertoResumo) {
            android.util.Log.d("SettlementHistoryAdapter", "ViewHolder.bind() - Aplicando dados: ID=${acerto.id}, Data=${acerto.data}, Valor=${acerto.valorTotal}")
            
            binding.apply {
                // Data do acerto
                tvSettlementDate.text = acerto.data
                
                // Valor formatado em Real usando StringUtils
                tvSettlementValue.text = com.example.gestaobilhares.core.utils.StringUtils.formatarMoedaComSeparadores(acerto.valorTotal)
                
                // Status com cores
                tvSettlementStatus.text = acerto.status.uppercase()
                val statusColor = when (acerto.status.lowercase()) {
                    "finalizado", "pago" -> com.example.gestaobilhares.ui.R.color.green_600
                    "pendente" -> com.example.gestaobilhares.ui.R.color.orange_600
                    "atrasado" -> com.example.gestaobilhares.ui.R.color.red_600
                    else -> com.example.gestaobilhares.ui.R.color.gray_600
                }
                tvSettlementStatus.setTextColor(ContextCompat.getColor(root.context, statusColor))
                
                // Mesas acertadas
                tvTablesCount.text = "${acerto.mesasAcertadas} mesa${if (acerto.mesasAcertadas != 1) "s" else ""}"
                
                // ID do acerto
                tvSettlementId.text = "#${acerto.id.toString().padStart(4, '0')}"
                
                // Débito atual usando StringUtils
                tvDebitoAtual.text = com.example.gestaobilhares.core.utils.StringUtils.formatarMoedaComSeparadores(acerto.debitoAtual)
                
                // ✅ CORREÇÃO: Observação do acerto com logs detalhados
                android.util.Log.d("SettlementHistoryAdapter", "=== EXIBINDO OBSERVAÇÃO NO HISTÓRICO ===")
                android.util.Log.d("SettlementHistoryAdapter", "Acerto ID: ${acerto.id}")
                android.util.Log.d("SettlementHistoryAdapter", "Observação recebida: '${acerto.observacao}'")
                android.util.Log.d("SettlementHistoryAdapter", "Observação é nula? ${acerto.observacao == null}")
                android.util.Log.d("SettlementHistoryAdapter", "Observação é vazia? ${com.example.gestaobilhares.core.utils.StringUtils.isVazia(acerto.observacao)}")
                android.util.Log.d("SettlementHistoryAdapter", "Observação é blank? ${com.example.gestaobilhares.core.utils.StringUtils.isVazia(acerto.observacao)}")
                
                if (com.example.gestaobilhares.core.utils.StringUtils.isNaoVazia(acerto.observacao)) {
                    tvObservacaoAcerto.visibility = android.view.View.VISIBLE
                    tvObservacaoAcerto.text = "📝 ${acerto.observacao}"
                    android.util.Log.d("SettlementHistoryAdapter", "✅ Observação EXIBIDA: '${acerto.observacao}'")
                } else {
                    tvObservacaoAcerto.visibility = android.view.View.VISIBLE // ✅ CORREÇÃO: Sempre mostrar
                    tvObservacaoAcerto.text = "📝 Sem observações"
                    android.util.Log.d("SettlementHistoryAdapter", "⚠️ Observação VAZIA - mostrando placeholder")
                }
                
                // Click listener
                root.setOnClickListener {
                    onItemClick(acerto)
                }
            }
            
            android.util.Log.d("SettlementHistoryAdapter", "ViewHolder.bind() - Dados aplicados com sucesso")
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AcertoResumo>() {
        override fun areItemsTheSame(oldItem: AcertoResumo, newItem: AcertoResumo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AcertoResumo, newItem: AcertoResumo): Boolean {
            return oldItem == newItem
        }
    }
} 

