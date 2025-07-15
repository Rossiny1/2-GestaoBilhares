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
 * Adapter modernizado para lista de clientes
 * Exibe informações dos clientes com design atualizado
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
            
            // Endereço
            binding.tvAddress.text = if (!cliente.endereco.isNullOrBlank()) {
                "${cliente.endereco}, ${cliente.cidade ?: ""}"
            } else {
                "Endereço não informado"
            }
            
            // Configurar status visual
            configurarStatusVisual(cliente)
            
            // Configurar débito atual do último acerto
            configurarDebitoAtual(cliente)
        }
        
        private fun configurarStatusVisual(cliente: Cliente) {
            val context = binding.root.context
            val debito = cliente.debitoAtual
            
            // Calcular dias desde o último acerto (mock por enquanto)
            val diasSemAcerto = 30 // Será calculado dinamicamente quando integrar com histórico
            
            val statusColor = when {
                !cliente.ativo -> context.getColor(R.color.red_600)
                debito > 300.0 -> context.getColor(R.color.red_600)
                diasSemAcerto > 90 -> context.getColor(R.color.orange_600)
                else -> context.getColor(R.color.green_600)
            }
            binding.statusBar.setBackgroundColor(statusColor)
            
            // Tag de status principal
            when {
                !cliente.ativo -> {
                    binding.tvStatusTag.text = "INATIVO"
                    binding.tvStatusTag.setBackgroundResource(R.drawable.rounded_tag_red)
                }
                debito > 300.0 -> {
                    binding.tvStatusTag.text = "DEVEDOR"
                    binding.tvStatusTag.setBackgroundResource(R.drawable.rounded_tag_red)
                }
                diasSemAcerto > 90 -> {
                    binding.tvStatusTag.text = "ATENÇÃO"
                    binding.tvStatusTag.setBackgroundResource(R.drawable.rounded_tag_orange)
                }
                else -> {
                    binding.tvStatusTag.text = "EM DIA"
                    binding.tvStatusTag.setBackgroundResource(R.drawable.rounded_tag_green)
                }
            }
            
            // Tag especial para débito alto
            if (debito > 300.0) {
                binding.tvSpecialTag.text = "ALTO DÉBITO"
                binding.tvSpecialTag.setBackgroundResource(R.drawable.rounded_tag_red)
                binding.tvSpecialTag.visibility = View.VISIBLE
            } else {
                binding.tvSpecialTag.visibility = View.GONE
            }
        }
        
        private fun configurarDebitoAtual(cliente: Cliente) {
            try {
                val context = binding.root.context
                
                // ✅ CORREÇÃO: Usar o débito atual da tabela clientes (atualizado após cada acerto)
                val debitoAtual = cliente.debitoAtual
                
                // Se não há débito, mostrar "Sem Débito"
                if (debitoAtual <= 0) {
                    binding.tvCurrentDebt.text = "Sem Débito"
                } else {
                    binding.tvCurrentDebt.text = String.format("R$ %.2f", debitoAtual)
                }
                
                // Configurar cores baseadas no valor do débito
                val (debtColor, backgroundTint) = when {
                    debitoAtual > 300.0 -> Pair(
                        context.getColor(R.color.red_600),
                        context.getColor(R.color.red_50)
                    )
                    debitoAtual > 100.0 -> Pair(
                        context.getColor(R.color.orange_600),
                        context.getColor(R.color.orange_50)
                    )
                    debitoAtual > 0 -> Pair(
                        context.getColor(R.color.yellow_600),
                        context.getColor(R.color.yellow_50)
                    )
                    else -> Pair(
                        context.getColor(R.color.green_600),
                        context.getColor(R.color.green_50)
                    )
                }
                
                binding.tvCurrentDebt.setTextColor(debtColor)
                binding.ivDebtIcon.setColorFilter(debtColor)
                binding.layoutDebtInfo.backgroundTintList = 
                    android.content.res.ColorStateList.valueOf(backgroundTint)
                
                // O label permanece fixo como "Débito Atual" no layout
                
            } catch (e: Exception) {
                android.util.Log.e("ClientAdapter", "Erro ao configurar débito: ${e.message}")
                // Fallback básico
                binding.tvCurrentDebt.text = "R$ 0,00"
            }
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
