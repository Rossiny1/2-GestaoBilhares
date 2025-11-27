package com.example.gestaobilhares.ui.routes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.ui.databinding.ItemClientSelectionBinding

/**
 * Adapter para a lista de seleção de clientes na transferência.
 */
class ClientSelectionAdapter(
    private val onClientSelected: (Cliente, Rota, List<Mesa>) -> Unit
) : ListAdapter<ClientSelectionAdapter.ClientSelectionItem, ClientSelectionAdapter.ClientViewHolder>(ClientDiffCallback()) {

    data class ClientSelectionItem(
        val cliente: Cliente,
        val rota: Rota,
        val mesas: List<Mesa>
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemClientSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ClientViewHolder(
        private val binding: ItemClientSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ClientSelectionItem) {
            val cliente = item.cliente
            val rota = item.rota
            val mesas = item.mesas

            // Nome do cliente
            binding.txtNomeCliente.text = cliente.nome

            // CPF formatado
            binding.txtCpfCliente.text = "CPF: ${cliente.cpfCnpj ?: "Não informado"}"

            // Rota atual
            binding.txtRotaAtual.text = "Rota: ${rota.nome}"

            // Mesas vinculadas
            val mesasText = if (mesas.isNotEmpty()) {
                "Mesas: ${mesas.joinToString(", ") { "Mesa ${it.numero}" }}"
            } else {
                "Nenhuma mesa vinculada"
            }
            binding.txtMesasVinculadas.text = mesasText

            // Click listener
            binding.root.setOnClickListener {
                onClientSelected(cliente, rota, mesas)
            }
        }
    }

    class ClientDiffCallback : DiffUtil.ItemCallback<ClientSelectionItem>() {
        override fun areItemsTheSame(
            oldItem: ClientSelectionItem,
            newItem: ClientSelectionItem
        ): Boolean {
            return oldItem.cliente.id == newItem.cliente.id
        }

        override fun areContentsTheSame(
            oldItem: ClientSelectionItem,
            newItem: ClientSelectionItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}

