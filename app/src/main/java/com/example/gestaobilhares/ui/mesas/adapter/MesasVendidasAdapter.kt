package com.example.gestaobilhares.ui.mesas.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.MesaVendida
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.databinding.ItemMesaVendidaBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para lista de mesas vendidas
 * ✅ NOVO: SISTEMA DE VENDA DE MESAS
 */
class MesasVendidasAdapter(
    private val onMesaVendidaClick: (MesaVendida) -> Unit
) : ListAdapter<MesaVendida, MesasVendidasAdapter.MesaVendidaViewHolder>(MesaVendidaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaVendidaViewHolder {
        val binding = ItemMesaVendidaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MesaVendidaViewHolder(binding, onMesaVendidaClick)
    }

    override fun onBindViewHolder(holder: MesaVendidaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateData(newList: List<MesaVendida>) {
        submitList(newList)
    }

    class MesaVendidaViewHolder(
        private val binding: ItemMesaVendidaBinding,
        private val onMesaVendidaClick: (MesaVendida) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        private val currencyFormatter = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        fun bind(mesaVendida: MesaVendida) {
            binding.apply {
                // Informações da mesa
                tvNumeroMesa.text = mesaVendida.numeroMesa
                tvTipoMesa.text = getTipoMesaNome(mesaVendida.tipoMesa)
                tvTamanhoMesa.text = mesaVendida.tamanhoMesa.name
                tvEstadoConservacao.text = mesaVendida.estadoConservacao.name

                // Informações do comprador
                tvNomeComprador.text = mesaVendida.nomeComprador
                tvTelefoneComprador.text = mesaVendida.telefoneComprador ?: "Não informado"
                tvCpfCnpjComprador.text = mesaVendida.cpfCnpjComprador ?: "Não informado"

                // Informações da venda
                tvValorVenda.text = currencyFormatter.format(mesaVendida.valorVenda)
                tvDataVenda.text = dateFormatter.format(mesaVendida.dataVenda)

                // Observações
                if (!mesaVendida.observacoes.isNullOrBlank()) {
                    tvObservacoes.text = mesaVendida.observacoes
                    tvObservacoes.visibility = android.view.View.VISIBLE
                } else {
                    tvObservacoes.visibility = android.view.View.GONE
                }

                // Click listener
                root.setOnClickListener {
                    onMesaVendidaClick(mesaVendida)
                }
            }
        }

        private fun getTipoMesaNome(tipoMesa: TipoMesa): String {
            return when (tipoMesa) {
                TipoMesa.SINUCA -> "Sinuca"
                TipoMesa.PEMBOLIM -> "Pembolim"
                TipoMesa.JUKEBOX -> "Jukebox"
                TipoMesa.OUTROS -> "Outros"
            }
        }
    }

    private class MesaVendidaDiffCallback : DiffUtil.ItemCallback<MesaVendida>() {
        override fun areItemsTheSame(oldItem: MesaVendida, newItem: MesaVendida): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MesaVendida, newItem: MesaVendida): Boolean {
            return oldItem == newItem
        }
    }
}
