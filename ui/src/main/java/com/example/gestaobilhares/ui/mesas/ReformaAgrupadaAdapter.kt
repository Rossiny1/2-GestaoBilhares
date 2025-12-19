package com.example.gestaobilhares.ui.mesas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.ui.databinding.ItemReformaAgrupadaBinding
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class para representar uma reforma agrupada com suas manutenções
 */
data class ReformaAgrupada(
    val dataReforma: Date,
    val reforma: MesaReformada?,
    val manutencoes: List<HistoricoManutencaoMesa>
)

/**
 * Adapter para exibir reformas agrupadas por data
 */
class ReformaAgrupadaAdapter(
    private val reformasAgrupadas: List<ReformaAgrupada>,
    private val nomeUsuarioLogado: String = ""
) : RecyclerView.Adapter<ReformaAgrupadaAdapter.ReformaAgrupadaViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd-MM-yy", Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReformaAgrupadaViewHolder {
        val binding = ItemReformaAgrupadaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReformaAgrupadaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReformaAgrupadaViewHolder, position: Int) {
        holder.bind(reformasAgrupadas[position])
    }

    override fun getItemCount(): Int = reformasAgrupadas.size

    inner class ReformaAgrupadaViewHolder(
        private val binding: ItemReformaAgrupadaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reformaAgrupada: ReformaAgrupada) {
            // Data da reforma
            binding.tvDataReforma.text = dateFormat.format(reformaAgrupada.dataReforma)

            // Itens da reforma (da reforma + manutenções)
            val itensReforma = mutableListOf<String>()
            
            // Adicionar itens da reforma
            reformaAgrupada.reforma?.let { reforma ->
                if (reforma.pintura) itensReforma.add("Pintura")
                if (reforma.tabela) itensReforma.add("Tabela")
                if (reforma.panos) {
                    val panosText = if (reforma.numeroPanos != null) {
                        "Troca de Pano (${reforma.numeroPanos})"
                    } else {
                        "Troca de Pano"
                    }
                    itensReforma.add(panosText)
                }
                if (reforma.outros) itensReforma.add("Outros")
            }
            
            // Adicionar tipos de manutenção
            reformaAgrupada.manutencoes.forEach { manutencao ->
                val tipoTexto = when (manutencao.tipoManutencao) {
                    TipoManutencao.PINTURA -> "Pintura"
                    TipoManutencao.TROCA_PANO -> "Troca de Pano"
                    TipoManutencao.TROCA_TABELA -> "Troca de Tabela"
                    TipoManutencao.REPARO_ESTRUTURAL -> "Reparo Estrutural"
                    TipoManutencao.LIMPEZA -> "Limpeza"
                    TipoManutencao.OUTROS -> "Outros"
                }
                if (!itensReforma.contains(tipoTexto)) {
                    itensReforma.add(tipoTexto)
                }
            }
            
            binding.tvItensReforma.text = itensReforma.joinToString(" e ")

            // Responsável: sempre usar nome do usuário logado (sem duplicar "Responsável:" pois já está no layout)
            val responsavel = if (nomeUsuarioLogado.isNotBlank()) nomeUsuarioLogado
                else "Não informado"
            
            binding.tvResponsavel.text = responsavel

            // Observações (se houver)
            val observacoes = reformaAgrupada.reforma?.observacoes
                ?: reformaAgrupada.manutencoes.firstOrNull()?.observacoes
            
            if (!observacoes.isNullOrBlank()) {
                binding.tvObservacoes.text = observacoes
                binding.tvObservacoes.visibility = View.VISIBLE
            } else {
                binding.tvObservacoes.visibility = View.GONE
            }
        }
    }
}

