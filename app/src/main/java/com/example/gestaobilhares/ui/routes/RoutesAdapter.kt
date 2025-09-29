package com.example.gestaobilhares.ui.routes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.databinding.ItemRotaBinding

/**
 * Adapter para a lista de rotas no RecyclerView.
 * Usa ListAdapter com DiffUtil para atualizações eficientes da lista.
 */
class RoutesAdapter(
    private val onItemClick: (RotaResumo) -> Unit
) : ListAdapter<RotaResumo, RoutesAdapter.RotaViewHolder>(RotaDiffCallback()) {

    /**
     * Cria um novo ViewHolder quando necessário.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RotaViewHolder {
        val binding = ItemRotaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RotaViewHolder(binding, onItemClick)
    }

    /**
     * Vincula os dados do item à ViewHolder.
     */
    override fun onBindViewHolder(holder: RotaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder que mantém as views do item da lista.
     */
    class RotaViewHolder(
        private val binding: ItemRotaBinding,
        private val onItemClick: (RotaResumo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Vincula os dados da rota às views.
         */
        fun bind(rotaResumo: RotaResumo) {
            with(binding) {
                // Nome da rota
                rotaNome.text = rotaResumo.rota.nome

                // Status da rota
                rotaStatus.text = when (rotaResumo.status) {
                    StatusRota.EM_ANDAMENTO -> "Em andamento"
                    StatusRota.FINALIZADA -> "Finalizada"
                    StatusRota.PAUSADA -> "Pausada"
                    StatusRota.CONCLUIDA -> "Concluída"
                    else -> "Finalizada" // Fallback para qualquer outro status
                }
                
                // ✅ NOVO: Log para debug do status
                android.util.Log.d("RoutesAdapter", "Rota ${rotaResumo.rota.nome}: Status = ${rotaResumo.status} -> Texto = '${rotaStatus.text}'")

                // Informações do ciclo atual
                rotaCiclo.text = rotaResumo.getCicloFormatado()

                // Informações da rota - inclui percentual e total de clientes
                rotaInfo.text = "${rotaResumo.percentualAcertados}% de ${rotaResumo.clientesAtivos} clientes acertados"

                // Quantidade de mesas da rota
                rotaMesas.text = "${rotaResumo.quantidadeMesas} mesas"

                // Pendências
                rotaPendencias.text = "${rotaResumo.pendencias} pendências"

                // Click listener para o item
                root.setOnClickListener {
                    onItemClick(rotaResumo)
                }

                // Click listener para o botão de ação
                rotaActionButton.setOnClickListener {
                    onItemClick(rotaResumo)
                }
            }
        }
    }

    /**
     * DiffCallback para calcular diferenças entre listas de forma eficiente.
     * Isso permite que o RecyclerView atualize apenas os itens que mudaram.
     */
    private class RotaDiffCallback : DiffUtil.ItemCallback<RotaResumo>() {
        
        /**
         * Verifica se dois itens representam a mesma rota.
         */
        override fun areItemsTheSame(oldItem: RotaResumo, newItem: RotaResumo): Boolean {
            return oldItem.rota.id == newItem.rota.id
        }

        /**
         * Verifica se o conteúdo de dois itens é igual.
         */
        override fun areContentsTheSame(oldItem: RotaResumo, newItem: RotaResumo): Boolean {
            return oldItem == newItem
        }
    }
}