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
    private val onDataChanged: () -> Unit,
    private val onCalcularMedia: (Long) -> Double = { 0.0 }
) : ListAdapter<MesaDTO, MesasAcertoAdapter.MesaAcertoViewHolder>(MesaDTODiffCallback()) {

    // Lista para manter o estado dos campos de entrada de cada mesa
    private val mesaStates = mutableMapOf<Long, MesaAcertoState>()
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /**
     * ✅ NOVO: Calcula as fichas jogadas considerando reinício do relógio
     * @param relogioInicial Relógio inicial
     * @param relogioFinal Relógio final
     * @param relogioReiniciou Se o relógio reiniciou
     * @return Número de fichas jogadas
     */
    private fun calcularFichasJogadas(relogioInicial: Int, relogioFinal: Int, relogioReiniciou: Boolean): Int {
        Log.d("MesasAcertoAdapter", "=== CÁLCULO FICHAS JOGADAS ===")
        Log.d("MesasAcertoAdapter", "Relógio inicial: $relogioInicial")
        Log.d("MesasAcertoAdapter", "Relógio final: $relogioFinal")
        Log.d("MesasAcertoAdapter", "Relógio reiniciou: $relogioReiniciou")
        
        if (relogioReiniciou) {
            // Determinar o fator de ajuste baseado no número de dígitos do relógio inicial
            val fatorAjuste = when {
                relogioInicial >= 10000 -> 100000 // 5 dígitos
                relogioInicial >= 1000 -> 10000   // 4 dígitos
                else -> 0 // Não aplicável
            }
            
            Log.d("MesasAcertoAdapter", "Fator de ajuste calculado: $fatorAjuste")
            
            if (fatorAjuste > 0) {
                val relogioFinalAjustado = relogioFinal + fatorAjuste
                val fichasJogadas = relogioFinalAjustado - relogioInicial
                Log.d("MesasAcertoAdapter", "✅ RELÓGIO REINICIOU: $relogioFinal + $fatorAjuste = $relogioFinalAjustado")
                Log.d("MesasAcertoAdapter", "✅ FICHAS CALCULADAS: $relogioFinalAjustado - $relogioInicial = $fichasJogadas")
                return fichasJogadas.coerceAtLeast(0)
            } else {
                Log.d("MesasAcertoAdapter", "⚠️ Relógio reiniciou ativado mas fator de ajuste é 0")
            }
        }
        
        // Cálculo normal
        val fichasJogadas = relogioFinal - relogioInicial
        Log.d("MesasAcertoAdapter", "✅ CÁLCULO NORMAL: $relogioFinal - $relogioInicial = $fichasJogadas")
        return fichasJogadas.coerceAtLeast(0)
    }

    /**
     * ✅ NOVO: Valida se o relógio final é válido considerando os checkboxes
     * @param relogioInicial Relógio inicial
     * @param relogioFinal Relógio final
     * @param comDefeito Se o relógio tem defeito
     * @param relogioReiniciou Se o relógio reiniciou
     * @return true se válido, false caso contrário
     */
    private fun validarRelogioFinal(relogioInicial: Int, relogioFinal: Int, comDefeito: Boolean, relogioReiniciou: Boolean): Boolean {
        // Se algum checkbox está marcado, permitir qualquer valor
        if (comDefeito || relogioReiniciou) {
            return true
        }
        
        // Validação normal: relógio final deve ser maior ou igual ao inicial
        return relogioFinal >= relogioInicial
    }

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
        Log.d("MesasAcertoAdapter", "Estados existentes antes do submit: ${mesaStates.size}")
        
        list?.forEachIndexed { index, mesa ->
            Log.d("MesasAcertoAdapter", "Item $index: Mesa ${mesa.numero} (ID: ${mesa.id}, Tipo: ${mesa.tipoMesa})")
        }
        
        super.submitList(list)
        
        Log.d("MesasAcertoAdapter", "Estados existentes após o submit: ${mesaStates.size}")
        Log.d("MesasAcertoAdapter", "=== SUBMITLIST CONCLUÍDO ===")
    }

    inner class MesaAcertoViewHolder(private val binding: ItemMesaAcertoBinding) : RecyclerView.ViewHolder(binding.root) {
        // TextWatchers como variáveis de instância
        private var relogioInicialWatcher: TextWatcher? = null
        private var relogioFinalWatcher: TextWatcher? = null

        fun bind(mesa: MesaDTO) {
            Log.d("MesasAcertoAdapter", "=== BIND MESA ${mesa.numero} ===")
            val state = mesaStates.getOrPut(mesa.id) {
                MesaAcertoState(
                    mesaId = mesa.id,
                    relogioInicial = mesa.fichasInicial,
                    valorFixo = mesa.valorFixo,
                    valorFicha = mesa.valorFicha,
                    comissaoFicha = mesa.comissaoFicha
                )
            }

            // Remover TextWatchers antes de setar texto
            binding.etRelogioInicial.removeTextChangedListener(relogioInicialWatcher)
            binding.etRelogioFinal.removeTextChangedListener(relogioFinalWatcher)

            // Atualizar campos apenas se valor mudou
            val atualInicial = binding.etRelogioInicial.text.toString()
            val novoInicial = state.relogioInicial.toString()
            if (atualInicial != novoInicial) {
                binding.etRelogioInicial.setText(novoInicial)
                binding.etRelogioInicial.setSelection(novoInicial.length)
            }
            val atualFinal = binding.etRelogioFinal.text.toString()
            val novoFinal = if (state.relogioFinal > 0) state.relogioFinal.toString() else ""
            if (atualFinal != novoFinal) {
                binding.etRelogioFinal.setText(novoFinal)
                binding.etRelogioFinal.setSelection(novoFinal.length)
            }
            binding.cbRelogioDefeito.isChecked = state.comDefeito
            binding.cbRelogioReiniciou.isChecked = state.relogioReiniciou

            // Adicionar TextWatchers novamente
            relogioInicialWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val valor = s.toString().toIntOrNull() ?: 0
                    if (state.relogioInicial != valor) {
                        state.relogioInicial = valor
                        updateSubtotal(state)
                        onDataChanged()
                        notifyItemChanged(adapterPosition)
                    }
                }
            }
            relogioFinalWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val valor = s.toString().toIntOrNull() ?: 0
                    if (state.relogioFinal != valor) {
                        state.relogioFinal = valor
                        updateSubtotal(state)
                        onDataChanged()
                        notifyItemChanged(adapterPosition)
                    }
                }
            }
            binding.etRelogioInicial.addTextChangedListener(relogioInicialWatcher)
            binding.etRelogioFinal.addTextChangedListener(relogioFinalWatcher)

            // Layouts e textos
            binding.tvNumeroMesa.text = "Mesa ${mesa.numero}"
            binding.tvTipoMesa.text = mesa.tipoMesa
            val isValorFixo = mesa.valorFixo > 0
            if (isValorFixo) {
                binding.layoutFichas.visibility = View.GONE
                binding.layoutValorFixo.visibility = View.VISIBLE
                binding.tvValorFixo.text = "Valor Mensal: R$ %.2f".format(mesa.valorFixo)
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(mesa.valorFixo)
                state.subtotal = mesa.valorFixo
            } else {
                binding.layoutFichas.visibility = View.VISIBLE
                binding.layoutValorFixo.visibility = View.GONE
                binding.tvValorFicha.text = "Valor Ficha: R$ %.2f".format(mesa.valorFicha)
                binding.tvComissaoFicha.text = "Comissão: R$ %.2f".format(mesa.comissaoFicha)
                if (state.comDefeito && state.mediaFichasJogadas > 0) {
                    binding.layoutMediaFichas.visibility = View.VISIBLE
                    binding.tvMediaFichas.text = state.mediaFichasJogadas.toInt().toString()
                    binding.tvFichasJogadas.text = "Fichas Jogadas (Média): ${state.mediaFichasJogadas.toInt()}"
                } else {
                    binding.layoutMediaFichas.visibility = View.GONE
                    val fichasJogadas = calcularFichasJogadas(state.relogioInicial, state.relogioFinal, state.relogioReiniciou)
                    state.fichasJogadas = fichasJogadas
                    if (state.relogioReiniciou) {
                        binding.tvFichasJogadas.text = "Fichas Jogadas (Ajustado): $fichasJogadas"
                    } else {
                        binding.tvFichasJogadas.text = "Fichas Jogadas: $fichasJogadas"
                    }
                }
                val subtotal = if (state.comDefeito && state.mediaFichasJogadas > 0) {
                    state.mediaFichasJogadas * mesa.comissaoFicha
                } else {
                    val fichasJogadas = calcularFichasJogadas(state.relogioInicial, state.relogioFinal, state.relogioReiniciou)
                    fichasJogadas * mesa.comissaoFicha
                }
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(subtotal)
                state.subtotal = subtotal
            }
        }

        private fun updateSubtotal(state: MesaAcertoState) {
            val mesa = getItem(adapterPosition)
            if (mesa.valorFixo > 0) {
                // Mesa de valor fixo
                state.subtotal = mesa.valorFixo
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(state.subtotal)
            } else {
                // Mesa de fichas jogadas
                val subtotal = if (state.comDefeito && state.mediaFichasJogadas > 0) {
                    val subtotalCalculado = state.mediaFichasJogadas * mesa.comissaoFicha
                    Log.d("MesasAcertoAdapter", "✅ SUBTOTAL COM MÉDIA: ${state.mediaFichasJogadas} × R$ ${mesa.comissaoFicha} = R$ $subtotalCalculado")
                    subtotalCalculado
                } else {
                    val fichasJogadas = calcularFichasJogadas(state.relogioInicial, state.relogioFinal, state.relogioReiniciou)
                    state.fichasJogadas = fichasJogadas
                    val subtotalCalculado = fichasJogadas * mesa.comissaoFicha
                    Log.d("MesasAcertoAdapter", "✅ SUBTOTAL NORMAL: $fichasJogadas × R$ ${mesa.comissaoFicha} = R$ $subtotalCalculado")
                    subtotalCalculado
                }
                
                state.subtotal = subtotal
                
                // Atualizar exibição das fichas jogadas e média
                if (state.comDefeito && state.mediaFichasJogadas > 0) {
                    binding.layoutMediaFichas.visibility = View.VISIBLE
                    binding.tvMediaFichas.text = state.mediaFichasJogadas.toInt().toString()
                    binding.tvFichasJogadas.text = "Fichas Jogadas (Média): ${state.mediaFichasJogadas.toInt()}"
                } else {
                    binding.layoutMediaFichas.visibility = View.GONE
                    val fichasJogadas = calcularFichasJogadas(state.relogioInicial, state.relogioFinal, state.relogioReiniciou)
                    
                    if (state.relogioReiniciou) {
                        binding.tvFichasJogadas.text = "Fichas Jogadas (Ajustado): $fichasJogadas"
                    } else {
                        binding.tvFichasJogadas.text = "Fichas Jogadas: $fichasJogadas"
                    }
                }
                
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(subtotal)
            }
        }
    }
    
    // --- Métodos públicos para o Fragment ---

    fun getSubtotal(): Double {
        val subtotal = mesaStates.values.sumOf { it.subtotal }
        Log.d("MesasAcertoAdapter", "=== GETSUBTOTAL CHAMADO ===")
        Log.d("MesasAcertoAdapter", "Total de estados: ${mesaStates.size}")
        mesaStates.forEach { (mesaId, state) ->
            Log.d("MesasAcertoAdapter", "Mesa ID $mesaId: subtotal=${state.subtotal}")
        }
        Log.d("MesasAcertoAdapter", "Subtotal total calculado: $subtotal")
        return subtotal
    }

    fun isDataValid(): Boolean {
        return mesaStates.values.all { state ->
            val mesa = currentList.find { it.id == state.mesaId }
            if (mesa?.valorFixo ?: 0.0 > 0) {
                true
            } else if (state.comDefeito && state.mediaFichasJogadas > 0) {
                true
            } else {
                validarRelogioFinal(state.relogioInicial, state.relogioFinal, state.comDefeito, state.relogioReiniciou)
            }
        }
    }

    fun getMesasAcerto(): List<MesaAcertoState> {
        Log.d("MesasAcertoAdapter", "=== GETMESASACERTO CHAMADO ===")
        Log.d("SettlementFragment", "Total de estados de mesa: ${mesaStates.size}")
        mesaStates.forEach { (mesaId, state) ->
            Log.d("MesasAcertoAdapter", "Mesa ID $mesaId: relógio inicial=${state.relogioInicial}, final=${state.relogioFinal}, subtotal=${state.subtotal}")
        }
        return mesaStates.values.toList()
    }

    /**
     * Método para compatibilidade - atualiza a lista de mesas
     */
    fun updateMesas(mesas: List<MesaDTO>) {
        submitList(mesas)
    }
    
    /**
     * ✅ NOVO: Atualiza a média de fichas jogadas de uma mesa específica
     * @param mesaId ID da mesa
     * @param media Média calculada
     */
    fun atualizarMediaMesa(mesaId: Long, media: Double) {
        val state = mesaStates[mesaId]
        if (state != null && state.comDefeito) {
            state.mediaFichasJogadas = media
            Log.d("MesasAcertoAdapter", "Média atualizada para mesa $mesaId: $media")
            
            // Notificar mudança para atualizar a UI
            onDataChanged()
        }
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