package com.example.gestaobilhares.ui.clients

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.databinding.ItemClientBinding

/**
 * Adapter para lista de clientes - FASE 3 IMPLEMENTADA! ✅
 * Exibe informações dos clientes com indicadores visuais adequados
 */
class ClientAdapter(
    private val onClientClick: (Cliente) -> Unit
) : ListAdapter<Cliente, ClientAdapter.ClientViewHolder>(ClientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemClientBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ClientViewHolder(binding, onClientClick)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClientViewHolder(
        private val binding: ItemClientBinding,
        private val onClientClick: (Cliente) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cliente: Cliente) {
            // Configurar click do item
            binding.root.setOnClickListener { onClientClick(cliente) }
            
            // Dados básicos do cliente
            binding.tvClientName.text = cliente.nome
            
            // Nome fantasia - só exibe se diferente do nome
            if (!cliente.nomeFantasia.isNullOrBlank() && cliente.nomeFantasia != cliente.nome) {
                binding.tvBusinessName.text = cliente.nomeFantasia
                binding.tvBusinessName.visibility = View.VISIBLE
            } else {
                binding.tvBusinessName.visibility = View.GONE
            }
            
            // Informações das mesas (mock por enquanto)
            binding.tvTableCount.text = "2 mesas ativas"
            
            // Valor da ficha
            binding.tvTokenValue.text = String.format("R$ %.2f/ficha", cliente.valorFicha)
            
            // Último acerto (mock por enquanto)
            binding.tvLastSettlement.text = "Último acerto há 3 dias"
            
            // Configurar status visual
            configurarStatusVisual(cliente)
            
            // Configurar débito
            configurarDebito(cliente)
        }
        
        private fun configurarStatusVisual(cliente: Cliente) {
            val context = binding.root.context
            
            // Barra de status lateral
            val statusColor = if (cliente.ativo) {
                context.getColor(R.color.green_600)
            } else {
                context.getColor(R.color.red_600)
            }
            binding.statusBar.setBackgroundColor(statusColor)
            
            // Tag de status principal
            binding.tvStatusTag.text = if (cliente.ativo) "ATIVO" else "INATIVO"
            binding.tvStatusTag.setBackgroundResource(
                if (cliente.ativo) R.drawable.rounded_tag_green else R.drawable.rounded_tag_red
            )
            
            // Tag especial se há débito alto
            if (cliente.debitoAnterior > 100.0) {
                binding.tvSpecialTag.text = "DEVEDOR"
                binding.tvSpecialTag.setBackgroundResource(R.drawable.rounded_tag_red)
                binding.tvSpecialTag.visibility = View.VISIBLE
            } else {
                binding.tvSpecialTag.visibility = View.GONE
            }
        }
        
        private fun configurarDebito(cliente: Cliente) {
            if (cliente.debitoAnterior > 0) {
                binding.tvDebtAmount.text = String.format("R$ %.2f", cliente.debitoAnterior)
                binding.layoutExtraInfo.visibility = View.VISIBLE
                
                // Cor baseada no valor do débito
                val context = binding.root.context
                val debtColor = when {
                    cliente.debitoAnterior > 100.0 -> context.getColor(R.color.red_500)
                    cliente.debitoAnterior > 50.0 -> context.getColor(R.color.orange_600)
                    else -> context.getColor(R.color.yellow_600)
                }
                binding.tvDebtAmount.setTextColor(debtColor)
                binding.ivDebtIcon.setColorFilter(debtColor)
            } else {
                binding.layoutExtraInfo.visibility = View.GONE
            }
            
            // Valor pendente (escondido por enquanto)
            binding.tvPendingAmount.visibility = View.GONE
        }
    }

    class ClientDiffCallback : DiffUtil.ItemCallback<Cliente>() {
        override fun areItemsTheSame(oldItem: Cliente, newItem: Cliente): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Cliente, newItem: Cliente): Boolean {
            return oldItem == newItem
        }
    }
} 
