package com.example.gestaobilhares.ui.settlement

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.databinding.ItemMesaAcertoBinding
import com.example.gestaobilhares.ui.viewmodel.MesaAcertoState
import java.text.NumberFormat
import java.util.Locale

class MesasAcertoAdapter(
    private val onDataChanged: () -> Unit
) : ListAdapter<MesaDTO, MesasAcertoAdapter.MesaAcertoViewHolder>(MesaDTODiffCallback()) {

    // Lista para manter o estado dos campos de entrada de cada mesa
    private val mesaStates = mutableMapOf<Long, MesaAcertoState>()
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaAcertoViewHolder {
        val binding = ItemMesaAcertoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesaAcertoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaAcertoViewHolder, position: Int) {
        val mesa = getItem(position)
        Log.d("MesasAcertoAdapter", "Binding mesa na posição $position: Mesa ${mesa.numero} (ID: ${mesa.id})")
        holder.bind(mesa)
    }

    override fun submitList(list: List<MesaDTO>?) {
        Log.d("MesasAcertoAdapter", "=== SUBMITLIST CHAMADO ===")
        Log.d("MesasAcertoAdapter", "Lista recebida: ${list?.size ?: 0} itens")
        list?.forEachIndexed { index, mesa ->
            Log.d("MesasAcertoAdapter", "Item $index: Mesa ${mesa.numero} (ID: ${mesa.id}, Tipo: ${mesa.tipoMesa})")
        }
        super.submitList(list)
        Log.d("MesasAcertoAdapter", "=== SUBMITLIST CONCLUÍDO ===")
    }

    inner class MesaAcertoViewHolder(private val binding: ItemMesaAcertoBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Adiciona TextWatchers para atualizar o estado e notificar o fragment
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val mesa = getItem(position)
                        val state = mesaStates.getOrPut(mesa.id) { MesaAcertoState(mesaId = mesa.id) }
                        state.relogioFinal = binding.etRelogioFinal.text.toString().toIntOrNull() ?: state.relogioInicial
                        updateSubtotal(state)
                        onDataChanged()
                    }
                }
            }
            binding.etRelogioInicial.addTextChangedListener(textWatcher)
            binding.etRelogioFinal.addTextChangedListener(textWatcher)
            binding.cbRelogioDefeito.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val mesa = getItem(position)
                    val state = mesaStates.getOrPut(mesa.id) { MesaAcertoState(mesaId = mesa.id) }
                    state.comDefeito = isChecked
                    onDataChanged()
                }
            }
        }

        fun bind(mesa: MesaDTO) {
            Log.d("MesasAcertoAdapter", "=== BIND MESA ${mesa.numero} ===")
            
            val state = mesaStates.getOrPut(mesa.id) {
                Log.d("MesasAcertoAdapter", "Criando novo estado para mesa ${mesa.numero}")
                MesaAcertoState(mesaId = mesa.id, relogioInicial = mesa.fichasInicial)
            }
            
            binding.tvNumeroMesa.text = "Mesa ${mesa.numero}"
            binding.tvTipoMesa.text = mesa.tipoMesa
            Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} - Tipo: ${mesa.tipoMesa}")

            // Exibe o layout correto conforme o tipo da mesa
            // Supondo que mesas de valor fixo têm fichasInicial == fichasFinal == 0
            val isValorFixo = (mesa.fichasInicial == 0 && mesa.fichasFinal == 0)
            Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} - É valor fixo: $isValorFixo (fichasInicial: ${mesa.fichasInicial}, fichasFinal: ${mesa.fichasFinal})")
            
            if (isValorFixo) {
                binding.layoutFichas.visibility = View.GONE
                binding.layoutValorFixo.visibility = View.VISIBLE
                val valorFixo = 0.0 // Não temos campo valorFixo, apenas exibição
                binding.tvValorFixo.text = "Valor Mensal: R$ %.2f".format(valorFixo)
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(valorFixo)
                state.subtotal = valorFixo
                Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} configurada como VALOR FIXO")
            } else {
                binding.layoutFichas.visibility = View.VISIBLE
                binding.layoutValorFixo.visibility = View.GONE
                binding.etRelogioInicial.setText(state.relogioInicial.toString())
                binding.etRelogioFinal.setText(if (state.relogioFinal > 0) state.relogioFinal.toString() else "")
                binding.cbRelogioDefeito.isChecked = state.comDefeito
                
                // Subtotal é calculado ao editar os campos
                val fichasJogadas = (state.relogioFinal - state.relogioInicial).coerceAtLeast(0)
                val subtotal = fichasJogadas * 0.5 // Valor fictício
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(subtotal)
                state.subtotal = subtotal
                
                Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} configurada como FICHAS JOGADAS")
                Log.d("MesasAcertoAdapter", "Relógio inicial: ${state.relogioInicial}, final: ${state.relogioFinal}")
                Log.d("MesasAcertoAdapter", "Fichas jogadas: $fichasJogadas, Subtotal: R$ %.2f".format(subtotal))
            }
            
            Log.d("MesasAcertoAdapter", "=== BIND CONCLUÍDO MESA ${mesa.numero} ===")
        }

        private fun updateSubtotal(state: MesaAcertoState) {
             val fichasJogadas = state.relogioFinal - state.relogioInicial
             val subtotal = if (fichasJogadas > 0) {
                 fichasJogadas * 0.50 // TODO: Usar valorFicha dinâmico
             } else {
                 0.0
             }
             state.subtotal = subtotal
             binding.tvSubtotal.text = formatter.format(subtotal)
        }
    }
    
    // --- Métodos públicos para o Fragment ---

    fun getSubtotal(): Double {
        return mesaStates.values.sumOf { it.subtotal }
    }

    fun isDataValid(): Boolean {
        // Verifica se para todas as mesas o relógio final é maior ou igual ao inicial
        return mesaStates.values.all { it.relogioFinal >= it.relogioInicial }
    }

    fun getMesasAcerto(): List<MesaAcertoState> {
        return mesaStates.values.toList()
    }
}

class MesaDTODiffCallback : DiffUtil.ItemCallback<MesaDTO>() {
    override fun areItemsTheSame(oldItem: MesaDTO, newItem: MesaDTO): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MesaDTO, newItem: MesaDTO): Boolean {
        return oldItem == newItem
    }
} 