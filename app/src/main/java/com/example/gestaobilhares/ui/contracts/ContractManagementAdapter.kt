package com.example.gestaobilhares.ui.contracts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.databinding.ItemContractManagementBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para lista de contratos no gerenciamento
 */
class ContractManagementAdapter(
    private val onContractClick: (ContractManagementViewModel.ContractItem) -> Unit,
    private val onViewClick: (ContractManagementViewModel.ContractItem) -> Unit
) : ListAdapter<ContractManagementViewModel.ContractItem, ContractManagementAdapter.ContractViewHolder>(
    ContractDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
        val binding = ItemContractManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContractViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContractViewHolder(
        private val binding: ItemContractManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContractManagementViewModel.ContractItem) {
            val contrato = item.contrato
            val cliente = item.cliente
            val rota = item.rota
            val mesas = item.mesas
            val aditivos = item.aditivos
            val status = item.status

            // Configurar informações do cliente
            cliente?.let { client ->
                binding.tvClienteNome.text = client.nome
                binding.tvClienteTelefone.text = client.telefone
            }

            // Configurar status do contrato
            binding.chipStatus.text = status
            // Exibir quantidade de aditivos (se houver)
            if (aditivos.isNotEmpty()) {
                binding.tvNumeroContrato.append("  •  ${aditivos.size} aditivo(s)")
            }
            when {
                status.startsWith("Ativo") -> {
                    if (status.contains("Assinado")) {
                        binding.chipStatus.setChipBackgroundColorResource(R.color.green_100)
                        binding.chipStatus.setChipStrokeColorResource(R.color.green_600)
                        binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.green_600))
                    } else {
                        binding.chipStatus.setChipBackgroundColorResource(R.color.blue_100)
                        binding.chipStatus.setChipStrokeColorResource(R.color.blue_600)
                        binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.blue_600))
                    }
                }
                status.startsWith("Inativo") -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.red_100)
                    binding.chipStatus.setChipStrokeColorResource(R.color.red_600)
                    binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.red_600))
                }
                status == "Sem Contrato" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.orange_100)
                    binding.chipStatus.setChipStrokeColorResource(R.color.orange_600)
                    binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.orange_600))
                }
                else -> {
                    // Status padrão
                    binding.chipStatus.setChipBackgroundColorResource(R.color.gray_100)
                    binding.chipStatus.setChipStrokeColorResource(R.color.gray_600)
                    binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.gray_600))
                }
            }

            // Configurar informações do contrato
            contrato?.let { contract ->
                val baseNumero = contract.numeroContrato ?: "N/A"
                val badges = mutableListOf<String>()
                if (aditivos.isNotEmpty()) badges.add("${aditivos.size} aditivo(s)")
                if (item.aditivosRetiradaCount > 0) badges.add("${item.aditivosRetiradaCount} retirada(s)")
                if (item.hasDistrato) badges.add("distrato")
                val withBadge = if (badges.isNotEmpty()) "$baseNumero  •  ${badges.joinToString("  •  ")}" else baseNumero
                binding.tvNumeroContrato.text = withBadge
                
                // Formatar data de criação
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.tvDataCriacao.text = dateFormat.format(contract.dataCriacao)
            } ?: run {
                binding.tvNumeroContrato.text = "N/A"
                binding.tvDataCriacao.text = "N/A"
            }

            // Configurar rota
            rota?.let { route ->
                binding.tvRota.text = route.nome
            } ?: run {
                binding.tvRota.text = "N/A"
            }

            // Configurar mesas
            if (mesas.isNotEmpty()) {
                val mesaText = formatMesasText(mesas)
                binding.tvMesas.text = mesaText
            } else {
                binding.tvMesas.text = "Nenhuma mesa"
            }

            // Configurar cliques
            binding.root.setOnClickListener { onContractClick(item) }

            // ✅ NOVO: Botão visualizar no local do compartilhar
            binding.btnView.setOnClickListener { onViewClick(item) }

            // ✅ REMOVIDO: Botão compartilhar
            binding.btnShare.visibility = android.view.View.GONE

            // Mostrar/ocultar botão visualizar baseado no status
            if (status == "Sem Contrato") {
                binding.btnView.visibility = android.view.View.GONE
            } else {
                binding.btnView.visibility = android.view.View.VISIBLE
            }
        }

        private fun formatMesasText(mesas: List<com.example.gestaobilhares.data.entities.Mesa>): String {
            val mesaCounts = mesas.groupBy { it.tipoMesa }.mapValues { it.value.size }
            
            return mesaCounts.entries.joinToString(", ") { (tipo, count) ->
                val tipoStr = tipo.name.lowercase()
                when (tipoStr) {
                    "sinuca" -> if (count == 1) "1 Sinuca" else "$count Sinucas"
                    "pembolim" -> if (count == 1) "1 Pembolim" else "$count Pembolins"
                    "jukebox" -> if (count == 1) "1 Jukebox" else "$count Jukeboxes"
                    else -> if (count == 1) "1 $tipoStr" else "$count ${tipoStr}s"
                }
            }
        }
    }

    /**
     * Callback para comparação de itens
     */
    class ContractDiffCallback : DiffUtil.ItemCallback<ContractManagementViewModel.ContractItem>() {
        override fun areItemsTheSame(
            oldItem: ContractManagementViewModel.ContractItem,
            newItem: ContractManagementViewModel.ContractItem
        ): Boolean {
            return oldItem.contrato?.id == newItem.contrato?.id &&
                   oldItem.cliente?.id == newItem.cliente?.id
        }

        override fun areContentsTheSame(
            oldItem: ContractManagementViewModel.ContractItem,
            newItem: ContractManagementViewModel.ContractItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
