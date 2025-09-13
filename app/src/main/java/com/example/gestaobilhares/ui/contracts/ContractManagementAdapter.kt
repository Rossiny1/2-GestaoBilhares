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
    private val onContractClick: (ContratoLocacao?) -> Unit,
    private val onViewClick: (ContratoLocacao?) -> Unit,
    private val onShareClick: (ContratoLocacao?) -> Unit
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
            val status = item.status

            // Configurar informações do cliente
            cliente?.let { client ->
                binding.tvClienteNome.text = client.nome
                binding.tvClienteTelefone.text = client.telefone
            }

            // Configurar status do contrato
            binding.chipStatus.text = status
            when (status) {
                "Assinado" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.green_100)
                    binding.chipStatus.setChipStrokeColorResource(R.color.green_600)
                    binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.green_600))
                }
                "Gerado" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.blue_100)
                    binding.chipStatus.setChipStrokeColorResource(R.color.blue_600)
                    binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.blue_600))
                }
                "Sem Contrato" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.orange_100)
                    binding.chipStatus.setChipStrokeColorResource(R.color.orange_600)
                    binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.orange_600))
                }
            }

            // Configurar informações do contrato
            contrato?.let { contract ->
                binding.tvNumeroContrato.text = contract.numeroContrato
                
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
            binding.root.setOnClickListener {
                onContractClick(contrato)
            }

            binding.btnView.setOnClickListener {
                onViewClick(contrato)
            }

            binding.btnShare.setOnClickListener {
                onShareClick(contrato)
            }

            // Mostrar/ocultar botões baseado no status
            if (status == "Sem Contrato") {
                binding.btnView.visibility = android.view.View.GONE
                binding.btnShare.visibility = android.view.View.GONE
            } else {
                binding.btnView.visibility = android.view.View.VISIBLE
                binding.btnShare.visibility = android.view.View.VISIBLE
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
