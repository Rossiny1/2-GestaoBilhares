package com.example.gestaobilhares.ui.cycles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.databinding.ItemCycleReceiptBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter para a lista de recebimentos do ciclo
 */
class CycleReceiptsAdapter(
    private val isCicloFinalizado: Boolean,
    private val onItemClick: (CycleReceiptItem) -> Unit
) : ListAdapter<CycleReceiptItem, CycleReceiptsAdapter.ReceiptViewHolder>(ReceiptDiffCallback()) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemCycleReceiptBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReceiptViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReceiptViewHolder(
        private val binding: ItemCycleReceiptBinding,
        private val onItemClick: (CycleReceiptItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CycleReceiptItem) {
            with(binding) {
                // Nome do cliente
                tvClienteNome.text = item.clienteNome

                // Data do acerto
                tvDataAcerto.text = item.dataAcerto

                // Tipo de pagamento
                tvTipoPagamento.text = item.tipoPagamento

                // Valor recebido
                tvValorRecebido.text = currencyFormatter.format(item.valorRecebido)

                // Débito atual
                tvDebitoAtual.text = currencyFormatter.format(item.debitoAtual)

                // Click listener (apenas para ciclos em andamento)
                if (!isCicloFinalizado) {
                    root.setOnClickListener {
                        onItemClick(item)
                    }
                } else {
                    root.setOnClickListener(null)
                }
            }
        }
    }

    private class ReceiptDiffCallback : DiffUtil.ItemCallback<CycleReceiptItem>() {
        override fun areItemsTheSame(oldItem: CycleReceiptItem, newItem: CycleReceiptItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CycleReceiptItem, newItem: CycleReceiptItem): Boolean {
            return oldItem == newItem
        }
    }
} 