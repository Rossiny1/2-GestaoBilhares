package com.example.gestaobilhares.ui.routes.management

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemRouteManagementBinding
import com.example.gestaobilhares.data.entities.Rota
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para gerenciamento de rotas com opções de editar e excluir.
 * Usa ListAdapter com DiffUtil para atualizações eficientes.
 */
class RouteManagementAdapter(
    private val onEditClick: (Rota) -> Unit,
    private val onDeleteClick: (Rota) -> Unit
) : ListAdapter<Rota, RouteManagementAdapter.RouteViewHolder>(RouteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RouteViewHolder(
        private val binding: ItemRouteManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(rota: Rota) {
            binding.apply {
                // Nome da rota
                routeNameText.text = rota.nome
                
                // Colaborador responsável
                collaboratorText.text = "Responsável: ${rota.colaboradorResponsavel}"
                
                // Cidades da rota
                citiesText.text = "Cidades: ${rota.cidades}"
                
                // Data de criação formatada
                val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
                createdDateText.text = "Criado em: ${dateFormat.format(Date(rota.dataCriacao))}"
                
                // Data da última atualização
                lastUpdatedText.text = "Atualizado em: ${dateFormat.format(Date(rota.dataAtualizacao))}"
                
                // Click listeners para os botões
                editButton.setOnClickListener {
                    onEditClick(rota)
                }
                
                deleteButton.setOnClickListener {
                    onDeleteClick(rota)
                }
            }
        }
    }

    /**
     * DiffCallback para calcular diferenças entre listas de rotas.
     */
    private class RouteDiffCallback : DiffUtil.ItemCallback<Rota>() {
        override fun areItemsTheSame(oldItem: Rota, newItem: Rota): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Rota, newItem: Rota): Boolean {
            return oldItem == newItem
        }
    }
} 
