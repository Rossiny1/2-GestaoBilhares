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
            
            // ✅ CORREÇÃO: Calcular dias desde o último acerto (4 meses = 120 dias)
            // Por enquanto usando mock, mas será integrado com histórico real
            val diasSemAcerto = 30 // TODO: Calcular dinamicamente quando integrar com histórico
            
            // ✅ NOVA LÓGICA: Priorizar status corretamente
            val statusInfo = when {
                // 1. Cliente inativo (prioridade máxima)
                !cliente.ativo -> StatusInfo(
                    text = "INATIVO",
                    color = context.getColor(R.color.red_600),
                    background = R.drawable.rounded_tag_red,
                    showSpecialTag = false
                )
                
                // 2. Débito alto (prioridade alta)
                debito > 300.0 -> StatusInfo(
                    text = "DÉBITO ALTO",
                    color = context.getColor(R.color.red_600),
                    background = R.drawable.rounded_tag_red,
                    showSpecialTag = false
                )
                
                // 3. Em atraso (mais de 4 meses sem acerto)
                diasSemAcerto > 120 -> StatusInfo(
                    text = "EM ATRASO",
                    color = context.getColor(R.color.orange_600),
                    background = R.drawable.rounded_tag_orange,
                    showSpecialTag = false
                )
                
                // 4. Em dia (sem débito e acertou nos últimos 4 meses)
                debito <= 0 && diasSemAcerto <= 120 -> StatusInfo(
                    text = "EM DIA",
                    color = context.getColor(R.color.green_600),
                    background = R.drawable.rounded_tag_green,
                    showSpecialTag = false
                )
                
                // 5. Caso padrão (com débito mas dentro do prazo)
                else -> StatusInfo(
                    text = "COM DÉBITO",
                    color = context.getColor(R.color.yellow_600),
                    background = R.drawable.rounded_tag_orange,
                    showSpecialTag = false
                )
            }
            
            // Aplicar configurações de status
            binding.statusBar.setBackgroundColor(statusInfo.color)
            binding.tvStatusTag.text = statusInfo.text
            binding.tvStatusTag.setBackgroundResource(statusInfo.background)
            
            // Configurar tag especial de débito alto
            binding.tvSpecialTag.visibility = if (statusInfo.showSpecialTag) View.VISIBLE else View.GONE
        }
        
        // ✅ NOVO: Classe auxiliar para organizar informações de status
        private data class StatusInfo(
            val text: String,
            val color: Int,
            val background: Int,
            val showSpecialTag: Boolean
        )
        
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
