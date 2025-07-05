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
                        
                        // Atualizar relógio inicial
                        state.relogioInicial = binding.etRelogioInicial.text.toString().toIntOrNull() ?: 0
                        // Atualizar relógio final
                        state.relogioFinal = binding.etRelogioFinal.text.toString().toIntOrNull() ?: 0
                        
                        // Recalcular subtotal
                        updateSubtotal(state)
                        onDataChanged()
                    }
                }
            }
            
            // TextWatcher específico para relógio inicial
            binding.etRelogioInicial.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val mesa = getItem(position)
                        val state = mesaStates.getOrPut(mesa.id) { MesaAcertoState(mesaId = mesa.id) }
                        state.relogioInicial = s.toString().toIntOrNull() ?: 0
                        updateSubtotal(state)
                        onDataChanged()
                    }
                }
            })
            
            // TextWatcher específico para relógio final
            binding.etRelogioFinal.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val mesa = getItem(position)
                        val state = mesaStates.getOrPut(mesa.id) { MesaAcertoState(mesaId = mesa.id) }
                        state.relogioFinal = s.toString().toIntOrNull() ?: 0
                        updateSubtotal(state)
                        onDataChanged()
                    }
                }
            })
            
            binding.cbRelogioDefeito.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val mesa = getItem(position)
                    val state = mesaStates.getOrPut(mesa.id) { MesaAcertoState(mesaId = mesa.id) }
                    state.comDefeito = isChecked
                    
                    if (isChecked) {
                        // Mostrar campo de média
                        binding.layoutMediaFichas.visibility = View.VISIBLE
                        
                        // Calcular média de forma assíncrona
                        val mediaFichas = onCalcularMedia(mesa.id)
                        state.mediaFichasJogadas = mediaFichas
                        
                        // Atualizar exibição da média
                        binding.tvMediaFichas.text = mediaFichas.toInt().toString()
                        binding.tvFichasJogadas.text = "Fichas Jogadas (Média): ${mediaFichas.toInt()}"
                        
                        Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} - Média calculada: $mediaFichas fichas")
                    } else {
                        // Ocultar campo de média
                        binding.layoutMediaFichas.visibility = View.GONE
                        state.mediaFichasJogadas = 0.0
                        val fichasJogadas = calcularFichasJogadas(state.relogioInicial, state.relogioFinal, state.relogioReiniciou)
                        binding.tvFichasJogadas.text = "Fichas Jogadas: $fichasJogadas"
                    }
                    
                    updateSubtotal(state)
                    onDataChanged()
                }
            }
            
            // ✅ NOVO: Listener para checkbox "Relógio Reiniciou"
            binding.cbRelogioReiniciou.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val mesa = getItem(position)
                    val state = mesaStates.getOrPut(mesa.id) { MesaAcertoState(mesaId = mesa.id) }
                    state.relogioReiniciou = isChecked
                    
                    // Recalcular fichas jogadas
                    val fichasJogadas = calcularFichasJogadas(state.relogioInicial, state.relogioFinal, state.relogioReiniciou)
                    state.fichasJogadas = fichasJogadas
                    
                    if (isChecked) {
                        binding.tvFichasJogadas.text = "Fichas Jogadas (Ajustado): $fichasJogadas"
                        Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} - Relógio reiniciou ativado, fichas ajustadas: $fichasJogadas")
                    } else {
                        binding.tvFichasJogadas.text = "Fichas Jogadas: $fichasJogadas"
                        Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} - Relógio reiniciou desativado, fichas normais: $fichasJogadas")
                    }
                    
                    updateSubtotal(state)
                    onDataChanged()
                }
            }
        }

        fun bind(mesa: MesaDTO) {
            Log.d("MesasAcertoAdapter", "=== BIND MESA ${mesa.numero} ===")
            Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} - fichasInicial recebido: ${mesa.fichasInicial}")
            
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
            
            if (state.relogioInicial != mesa.fichasInicial) {
                Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero}: Atualizando relógio inicial de ${state.relogioInicial} para ${mesa.fichasInicial}")
                state.relogioInicial = mesa.fichasInicial
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
                binding.cbRelogioReiniciou.isChecked = state.relogioReiniciou
                
                // Exibir informações de ficha
                binding.tvValorFicha.text = "Valor Ficha: R$ %.2f".format(mesa.valorFicha)
                binding.tvComissaoFicha.text = "Comissão: R$ %.2f".format(mesa.comissaoFicha)
                
                // Configurar exibição da média de fichas
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
                    val subtotalCalculado = state.mediaFichasJogadas * mesa.comissaoFicha
                    Log.d("MesasAcertoAdapter", "✅ SUBTOTAL COM MÉDIA (BIND): ${state.mediaFichasJogadas} × R$ ${mesa.comissaoFicha} = R$ $subtotalCalculado")
                    subtotalCalculado
                } else {
                    val fichasJogadas = calcularFichasJogadas(state.relogioInicial, state.relogioFinal, state.relogioReiniciou)
                    val subtotalCalculado = fichasJogadas * mesa.comissaoFicha
                    Log.d("MesasAcertoAdapter", "✅ SUBTOTAL NORMAL (BIND): $fichasJogadas × R$ ${mesa.comissaoFicha} = R$ $subtotalCalculado")
                    subtotalCalculado
                }
                
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(subtotal)
                state.subtotal = subtotal
                
                Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero} configurada como FICHAS JOGADAS")
                Log.d("MesasAcertoAdapter", "Relógio inicial: ${state.relogioInicial}, final: ${state.relogioFinal}")
                Log.d("MesasAcertoAdapter", "Com defeito: ${state.comDefeito}, Média: ${state.mediaFichasJogadas}")
                Log.d("MesasAcertoAdapter", "Subtotal: R$ %.2f".format(subtotal))
            }
            
            Log.d("MesasAcertoAdapter", "=== BIND CONCLUÍDO MESA ${mesa.numero} ===")
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
        return mesaStates.values.sumOf { it.subtotal }
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