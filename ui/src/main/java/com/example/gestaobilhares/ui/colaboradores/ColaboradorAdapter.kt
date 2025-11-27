package com.example.gestaobilhares.ui.colaboradores
import com.example.gestaobilhares.ui.R

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.ui.databinding.ItemColaboradorBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para lista de colaboradores.
 * Exibe informações dos colaboradores com opções de edição e ações.
 */
class ColaboradorAdapter(
    private val onEditClick: (Colaborador) -> Unit,
    private val onMoreClick: (Colaborador) -> Unit
) : ListAdapter<Colaborador, ColaboradorAdapter.ColaboradorViewHolder>(ColaboradorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColaboradorViewHolder {
        val binding = ItemColaboradorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColaboradorViewHolder(binding, onEditClick, onMoreClick)
    }

    override fun onBindViewHolder(holder: ColaboradorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ColaboradorViewHolder(
        private val binding: ItemColaboradorBinding,
        private val onEditClick: (Colaborador) -> Unit,
        private val onMoreClick: (Colaborador) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(colaborador: Colaborador) {
            binding.apply {
                // Nome do colaborador
                tvNome.text = colaborador.nome

                // Email
                tvEmail.text = colaborador.email

                // Status
                val status = when {
                    !colaborador.aprovado -> "PENDENTE"
                    colaborador.ativo -> "ATIVO"
                    else -> "INATIVO"
                }
                tvStatus.text = status

                // Cor do status baseada no estado
                val statusBackground = when {
                    !colaborador.aprovado -> com.example.gestaobilhares.ui.R.drawable.rounded_tag_orange
                    colaborador.ativo -> com.example.gestaobilhares.ui.R.drawable.rounded_tag_green
                    else -> com.example.gestaobilhares.ui.R.drawable.rounded_tag_red
                }
                tvStatus.setBackgroundResource(statusBackground)

                // Nível de acesso
                val nivelAcesso = when (colaborador.nivelAcesso) {
                    com.example.gestaobilhares.data.entities.NivelAcesso.ADMIN -> "Administrador"
                    com.example.gestaobilhares.data.entities.NivelAcesso.USER -> "Usuário"
                }
                tvNivelAcesso.text = nivelAcesso

                // Data de cadastro
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                val dataCadastro = dateFormat.format(colaborador.dataCadastro)
                tvDataCadastro.text = "Cadastrado em $dataCadastro"

                // ✅ CORREÇÃO: Apenas botão "Mais opções" (inclui função de editar)
                btnMore.setOnClickListener {
                    onMoreClick(colaborador)
                }
            }
        }
    }

    /**
     * DiffCallback para calcular diferenças entre listas de colaboradores.
     */
    private class ColaboradorDiffCallback : DiffUtil.ItemCallback<Colaborador>() {
        override fun areItemsTheSame(oldItem: Colaborador, newItem: Colaborador): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Colaborador, newItem: Colaborador): Boolean {
            return oldItem == newItem
        }
    }
}

