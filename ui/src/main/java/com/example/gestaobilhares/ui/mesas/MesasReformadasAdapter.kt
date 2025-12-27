package com.example.gestaobilhares.ui.mesas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.ui.databinding.ItemMesaReformadaBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para a lista de mesas reformadas.
 * ✅ NOVO: Agrupa reformas por mesa e exibe histórico de manutenções
 */
class MesasReformadasAdapter(
    private val onItemClick: (MesaReformadaComHistorico) -> Unit
) : ListAdapter<MesaReformadaComHistorico, MesasReformadasAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMesaReformadaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMesaReformadaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mesaComHistorico: MesaReformadaComHistorico) {
            val ultimaReforma = mesaComHistorico.reformas.firstOrNull()
            val ultimaManutencao = mesaComHistorico.historicoManutencoes.firstOrNull()
            
            // Número da mesa
            binding.tvNumeroMesa.text = "Mesa ${mesaComHistorico.numeroMesa}"
            
            // ✅ CORREÇÃO: Usar Data do último evento (reforma ou manutenção)
            val dataExibicao = mesaComHistorico.dataUltimoEvento
            if (dataExibicao != null) {
                binding.tvDataReforma.text = dateFormat.format(dataExibicao)
                binding.tvDataReforma.visibility = View.VISIBLE
            } else {
                binding.tvDataReforma.visibility = View.GONE
            }
            
            // Tipo da mesa
            binding.tvTipoMesa.text = "${mesaComHistorico.tipoMesa} - ${mesaComHistorico.tamanhoMesa}"
            
            // Total de reformas
            if (mesaComHistorico.totalReformas > 1) {
                binding.tvTotalReformas.text = "${mesaComHistorico.totalReformas} reformas realizadas"
                binding.tvTotalReformas.visibility = View.VISIBLE
            } else if (mesaComHistorico.totalReformas == 1) {
                binding.tvTotalReformas.text = "1 reforma realizada"
                binding.tvTotalReformas.visibility = View.VISIBLE
            } else {
                // Se não houver reformas, mostrar que tem histórico
                val totalManutencoes = mesaComHistorico.historicoManutencoes.size
                if (totalManutencoes > 0) {
                    binding.tvTotalReformas.text = "$totalManutencoes manutenções registradas"
                    binding.tvTotalReformas.visibility = View.VISIBLE
                } else {
                    binding.tvTotalReformas.visibility = View.GONE
                }
            }
            
            // Itens reformados ou última manutenção
            if (ultimaReforma != null) {
                val itensReformados = buildString {
                    val itens = mutableListOf<String>()
                    if (ultimaReforma.pintura) itens.add("Pintura")
                    if (ultimaReforma.tabela) itens.add("Tabela")
                    if (ultimaReforma.panos) {
                        val panosText = if (ultimaReforma.numeroPanos != null) {
                            "Panos (${ultimaReforma.numeroPanos})"
                        } else {
                            "Panos"
                        }
                        itens.add(panosText)
                    }
                    if (ultimaReforma.outros) itens.add("Outros")
                    
                    append(itens.joinToString(", "))
                }
                binding.tvItensReformados.text = itensReformados
                
                // Observações da última reforma
                if (!ultimaReforma.observacoes.isNullOrBlank()) {
                    binding.tvObservacoes.text = "Obs: ${ultimaReforma.observacoes}"
                    binding.tvObservacoes.visibility = View.VISIBLE
                } else {
                    binding.tvObservacoes.visibility = View.GONE
                }
            } else if (ultimaManutencao != null) {
                // Fallback: Mostrar dados da última manutenção se não houver reforma
                binding.tvItensReformados.text = "Manutenção: ${formatarTipoManutencao(ultimaManutencao.tipoManutencao)}"
                
                if (!ultimaManutencao.descricao.isNullOrBlank()) {
                    binding.tvObservacoes.text = "Obs: ${ultimaManutencao.descricao}"
                    binding.tvObservacoes.visibility = View.VISIBLE
                } else {
                    binding.tvObservacoes.visibility = View.GONE
                }
            } else {
                binding.tvItensReformados.text = "Sem atividades registradas"
                binding.tvObservacoes.visibility = View.GONE
            }
            
            // Click listener
            binding.root.setOnClickListener {
                onItemClick(mesaComHistorico)
            }
        }
        
        private fun formatarTipoManutencao(tipo: TipoManutencao): String {
            return when (tipo) {
                TipoManutencao.PINTURA -> "Pintura"
                TipoManutencao.TROCA_PANO -> "Troca de Pano"
                TipoManutencao.TROCA_TABELA -> "Troca de Tabela"
                TipoManutencao.REPARO_ESTRUTURAL -> "Reparo Estrutural"
                TipoManutencao.LIMPEZA -> "Limpeza"
                TipoManutencao.OUTROS -> "Outros"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MesaReformadaComHistorico>() {
        override fun areItemsTheSame(oldItem: MesaReformadaComHistorico, newItem: MesaReformadaComHistorico): Boolean {
            return oldItem.mesaId == newItem.mesaId && oldItem.numeroMesa == newItem.numeroMesa
        }

        override fun areContentsTheSame(oldItem: MesaReformadaComHistorico, newItem: MesaReformadaComHistorico): Boolean {
            return oldItem == newItem
        }
    }
}

