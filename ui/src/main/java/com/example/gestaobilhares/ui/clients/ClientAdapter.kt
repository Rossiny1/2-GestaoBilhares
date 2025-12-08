package com.example.gestaobilhares.ui.clients
import com.example.gestaobilhares.ui.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.ui.databinding.ItemClientBinding

/**
 * Adapter modernizado para lista de clientes
 * Exibe informações dos clientes com design atualizado
 */
class ClientAdapter(
    private val onClientClick: (Cliente) -> Unit,
    private val verificarNuncaAcertado: suspend (Long) -> Boolean = { false }
) : ListAdapter<Cliente, ClientAdapter.ClientViewHolder>(ClientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemClientBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ClientViewHolder(binding, onClientClick, verificarNuncaAcertado)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClientViewHolder(
        private val binding: ItemClientBinding,
        private val onClientClick: (Cliente) -> Unit,
        private val verificarNuncaAcertado: suspend (Long) -> Boolean
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
            
            // Configurar débito atual do último acerto (com verificação de "Nunca acertado")
            configurarDebitoAtual(cliente, verificarNuncaAcertado)
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
                    color = context.getColor(com.example.gestaobilhares.ui.R.color.red_600),
                    background = com.example.gestaobilhares.ui.R.drawable.rounded_tag_red,
                    showSpecialTag = false
                )
                
                // 2. Débito alto (prioridade alta)
                debito > 300.0 -> StatusInfo(
                    text = "DÉBITO ALTO",
                    color = context.getColor(com.example.gestaobilhares.ui.R.color.red_600),
                    background = com.example.gestaobilhares.ui.R.drawable.rounded_tag_red,
                    showSpecialTag = false
                )
                
                // 3. Em atraso (mais de 4 meses sem acerto)
                diasSemAcerto > 120 -> StatusInfo(
                    text = "EM ATRASO",
                    color = context.getColor(com.example.gestaobilhares.ui.R.color.orange_600),
                    background = com.example.gestaobilhares.ui.R.drawable.rounded_tag_orange,
                    showSpecialTag = false
                )
                
                // 4. Em dia (sem débito e acertou nos últimos 4 meses)
                debito <= 0 && diasSemAcerto <= 120 -> StatusInfo(
                    text = "EM DIA",
                    color = context.getColor(com.example.gestaobilhares.ui.R.color.green_600),
                    background = com.example.gestaobilhares.ui.R.drawable.rounded_tag_green,
                    showSpecialTag = false
                )
                
                // 5. Caso padrão (com débito mas dentro do prazo)
                else -> StatusInfo(
                    text = "COM DÉBITO",
                    color = context.getColor(com.example.gestaobilhares.ui.R.color.yellow_600),
                    background = com.example.gestaobilhares.ui.R.drawable.rounded_tag_orange,
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
        
        private fun configurarDebitoAtual(cliente: Cliente, verificarNuncaAcertado: suspend (Long) -> Boolean) {
            try {
                val context = binding.root.context
                
                // ✅ CORREÇÃO: Usar o débito atual da tabela clientes (atualizado após cada acerto)
                val debitoAtual = cliente.debitoAtual
                
                // ✅ DEBUG: Log detalhado para verificar o débito recebido
                android.util.Log.d("ClientAdapter", "═══════════════════════════════════════")
                android.util.Log.d("ClientAdapter", "💰 CONFIGURANDO DÉBITO DO CLIENTE")
                android.util.Log.d("ClientAdapter", "   Nome: ${cliente.nome}")
                android.util.Log.d("ClientAdapter", "   ID: ${cliente.id}")
                android.util.Log.d("ClientAdapter", "   débitoAtual (campo): R$ $debitoAtual")
                android.util.Log.d("ClientAdapter", "   débitoAtual <= 0? ${debitoAtual <= 0}")
                
                // ✅ NOVO: Se não há débito, verificar se nunca foi acertado
                if (debitoAtual <= 0) {
                    // Verificar de forma síncrona se nunca foi acertado (usando runBlocking apenas quando necessário)
                    val nuncaAcertado = kotlinx.coroutines.runBlocking {
                        verificarNuncaAcertado(cliente.id)
                    }
                    
                    if (nuncaAcertado) {
                        binding.tvCurrentDebt.text = "Nunca acertado"
                        android.util.Log.d("ClientAdapter", "   ✅ Exibindo: 'Nunca acertado'")
                        // Usar cor laranja para indicar pendência
                        val pendenciaColor = context.getColor(com.example.gestaobilhares.ui.R.color.orange_600)
                        binding.tvCurrentDebt.setTextColor(pendenciaColor)
                        binding.ivDebtIcon.setColorFilter(pendenciaColor)
                    } else {
                        binding.tvCurrentDebt.text = "Sem Débito"
                        android.util.Log.d("ClientAdapter", "   ✅ Exibindo: 'Sem Débito'")
                        val debtColor = context.getColor(com.example.gestaobilhares.ui.R.color.green_600)
                        binding.tvCurrentDebt.setTextColor(debtColor)
                        binding.ivDebtIcon.setColorFilter(debtColor)
                    }
                } else {
                    val textoFormatado = String.format("R$ %.2f", debitoAtual)
                    binding.tvCurrentDebt.text = textoFormatado
                    android.util.Log.d("ClientAdapter", "   ✅ Exibindo: '$textoFormatado'")
                    
                    // Configurar cores baseadas no valor do débito
                    val debtColor = when {
                        debitoAtual > 300.0 -> context.getColor(com.example.gestaobilhares.ui.R.color.red_600)
                        debitoAtual > 100.0 -> context.getColor(com.example.gestaobilhares.ui.R.color.orange_600)
                        debitoAtual > 0 -> context.getColor(com.example.gestaobilhares.ui.R.color.yellow_600)
                        else -> context.getColor(com.example.gestaobilhares.ui.R.color.green_600)
                    }
                    
                    binding.tvCurrentDebt.setTextColor(debtColor)
                    binding.ivDebtIcon.setColorFilter(debtColor)
                }
                android.util.Log.d("ClientAdapter", "═══════════════════════════════════════")
                
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

