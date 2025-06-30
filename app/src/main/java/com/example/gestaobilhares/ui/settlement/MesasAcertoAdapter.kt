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
                MesaAcertoState(
                    mesaId = mesa.id, 
                    relogioInicial = mesa.fichasInicial,
                    valorFixo = mesa.valorFixo,
                    valorFicha = mesa.valorFicha,
                    comissaoFicha = mesa.comissaoFicha
                )
            }
            
            binding.tvNumeroMesa.text = "Mesa ${mesa.numero}"
            binding.tvTipoMesa.text = mesa.tipoMesa
            Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} - Tipo: ${mesa.tipoMesa}")

            // Exibe o layout correto conforme o tipo da mesa
            // Mesas de valor fixo têm valorFixo > 0
            val isValorFixo = mesa.valorFixo > 0
            Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} - É valor fixo: $isValorFixo (valorFixo: ${mesa.valorFixo})")
            
            if (isValorFixo) {
                binding.layoutFichas.visibility = View.GONE
                binding.layoutValorFixo.visibility = View.VISIBLE
                binding.tvValorFixo.text = "Valor Mensal: R$ %.2f".format(mesa.valorFixo)
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(mesa.valorFixo)
                state.subtotal = mesa.valorFixo
                Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} configurada como VALOR FIXO")
            } else {
                binding.layoutFichas.visibility = View.VISIBLE
                binding.layoutValorFixo.visibility = View.GONE
                binding.etRelogioInicial.setText(state.relogioInicial.toString())
                binding.etRelogioFinal.setText(if (state.relogioFinal > 0) state.relogioFinal.toString() else "")
                binding.cbRelogioDefeito.isChecked = state.comDefeito
                
                // Exibir informações de ficha
                binding.tvValorFicha.text = "Valor Ficha: R$ %.2f".format(mesa.valorFicha)
                binding.tvComissaoFicha.text = "Comissão: R$ %.2f".format(mesa.comissaoFicha)
                
                // Calcular e exibir fichas jogadas
                val fichasJogadas = (state.relogioFinal - state.relogioInicial).coerceAtLeast(0)
                state.fichasJogadas = fichasJogadas
                binding.tvFichasJogadas.text = "Fichas Jogadas: $fichasJogadas"
                
                // Calcular subtotal baseado na comissão da ficha (não no valor da ficha)
                val subtotal = fichasJogadas * mesa.comissaoFicha
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(subtotal)
                state.subtotal = subtotal
                
                Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} configurada como FICHAS JOGADAS")
                Log.d("MesasAcertoAdapter", "Relógio inicial: ${state.relogioInicial}, final: ${state.relogioFinal}")
                Log.d("MesasAcertoAdapter", "Fichas jogadas: $fichasJogadas, Subtotal: R$ %.2f".format(subtotal))
            }
            
            Log.d("MesasAcertoAdapter", "=== BIND CONCLUÍDO MESA ${mesa.numero} ===")
        }

        private fun updateSubtotal(state: MesaAcertoState) {
            val mesa = getItem(adapterPosition)
            if (mesa.valorFixo > 0) {
                // Mesa de valor fixo
                state.subtotal = mesa.valorFixo
            } else {
                // Mesa de fichas jogadas
                val fichasJogadas = (state.relogioFinal - state.relogioInicial).coerceAtLeast(0)
                state.fichasJogadas = fichasJogadas
                val subtotal = fichasJogadas * mesa.comissaoFicha
                state.subtotal = subtotal
                
                // Atualizar exibição das fichas jogadas
                binding.tvFichasJogadas.text = "Fichas Jogadas: $fichasJogadas"
            }
            binding.tvSubtotal.text = formatter.format(state.subtotal)
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