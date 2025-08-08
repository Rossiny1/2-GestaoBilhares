package com.example.gestaobilhares.ui.settlement

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.getDisplayName
import com.example.gestaobilhares.databinding.ItemMesaAcertoBinding
import com.example.gestaobilhares.ui.viewmodel.MesaAcertoState
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MesasAcertoAdapter(
    private val onDataChanged: () -> Unit,
    private val onCalcularMedia: (Long) -> Double = { 0.0 },
    private val onFotoCapturada: (Long, String, Date) -> Unit = { _, _, _ -> },
    private val onSolicitarCapturaFoto: ((Long) -> Unit)? = null
) : ListAdapter<MesaDTO, MesasAcertoAdapter.MesaAcertoViewHolder>(MesaDTODiffCallback()) {

    // Lista para manter o estado dos campos de entrada de cada mesa
    private val mesaStates = mutableMapOf<Long, MesaAcertoState>()
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    // ✅ NOVO: Flag para controlar se estamos no processo de binding
    private var isBinding = false
    
    // ✅ NOVO: Handler para agendar notificações de mudança
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // ✅ NOVO: Variáveis para captura de foto
    private var currentPhotoUri: Uri? = null
    private var currentMesaId: Long = 0L

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

    /**
     * ✅ NOVO: Método seguro para notificar mudanças no adapter
     */
    private fun safeNotifyItemChanged(position: Int) {
        if (position != RecyclerView.NO_POSITION && !isBinding) {
            try {
                notifyItemChanged(position)
            } catch (e: IllegalStateException) {
                Log.w("MesasAcertoAdapter", "Tentativa de notificar mudança durante layout, agendando para depois")
                // Usar Handler para agendar a notificação para depois do layout
                mainHandler.post {
                    try {
                        if (position < itemCount) {
                            notifyItemChanged(position)
                        }
                    } catch (e: Exception) {
                        Log.e("MesasAcertoAdapter", "Erro ao notificar mudança agendada: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaAcertoViewHolder {
        val binding = ItemMesaAcertoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesaAcertoViewHolder(binding)
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d("MesasAcertoAdapter", "getItemCount() retornou: $count")
        Log.d("MesasAcertoAdapter", "currentList.size: ${currentList.size}")
        return count
    }

    override fun onBindViewHolder(holder: MesaAcertoViewHolder, position: Int) {
        isBinding = true
        try {
            val mesa = getItem(position)
            Log.d("MesasAcertoAdapter", "Binding mesa na posição $position: Mesa ${mesa.numero} (ID: ${mesa.id})")
            Log.d("MesasAcertoAdapter", "ViewHolder: ${holder.hashCode()}")
            holder.bind(mesa)
        } finally {
            isBinding = false
        }
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

            // ✅ CORREÇÃO: Remover listeners antes de configurar
            binding.etRelogioInicial.removeTextChangedListener(relogioInicialWatcher)
            binding.etRelogioFinal.removeTextChangedListener(relogioFinalWatcher)
            binding.cbRelogioDefeito.setOnCheckedChangeListener(null)
            binding.cbRelogioReiniciou.setOnCheckedChangeListener(null)

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
            
            // ✅ CORREÇÃO: Setar checkboxes sem triggerar listeners
            binding.cbRelogioDefeito.isChecked = state.comDefeito
            binding.cbRelogioReiniciou.isChecked = state.relogioReiniciou

            // ✅ CORREÇÃO: Adicionar TextWatchers com verificações de segurança
            relogioInicialWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val valor = s.toString().toIntOrNull() ?: 0
                    if (state.relogioInicial != valor) {
                        state.relogioInicial = valor
                        updateSubtotal(state)
                        onDataChanged()
                        safeNotifyItemChanged(adapterPosition)
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
                        safeNotifyItemChanged(adapterPosition)
                    }
                }
            }
            binding.etRelogioInicial.addTextChangedListener(relogioInicialWatcher)
            binding.etRelogioFinal.addTextChangedListener(relogioFinalWatcher)

            // ✅ CORREÇÃO: Listeners dos checkboxes com verificações de segurança
            binding.cbRelogioDefeito.setOnCheckedChangeListener { _, isChecked ->
                Log.d("MesasAcertoAdapter", "=== CHECKBOX RELÓGIO COM DEFEITO ===")
                Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero}: Relógio com defeito = $isChecked")
                
                if (state.comDefeito != isChecked) {
                    state.comDefeito = isChecked
                    
                    if (isChecked) {
                        // Calcular média se marcado
                        val media = onCalcularMedia(mesa.id)
                        state.mediaFichasJogadas = media
                        Log.d("MesasAcertoAdapter", "Média calculada para mesa ${mesa.numero}: $media")
                    } else {
                        // Limpar média se desmarcado
                        state.mediaFichasJogadas = 0.0
                        Log.d("MesasAcertoAdapter", "Média limpa para mesa ${mesa.numero}")
                    }
                    
                    updateSubtotal(state)
                    onDataChanged()
                    safeNotifyItemChanged(adapterPosition)
                }
            }
            
            binding.cbRelogioReiniciou.setOnCheckedChangeListener { _, isChecked ->
                Log.d("MesasAcertoAdapter", "=== CHECKBOX RELÓGIO REINICIOU ===")
                Log.d("MesasAcertoAdapter", "Mesa ${mesa.numero}: Relógio reiniciou = $isChecked")
                
                if (state.relogioReiniciou != isChecked) {
                    state.relogioReiniciou = isChecked
                    updateSubtotal(state)
                    onDataChanged()
                    safeNotifyItemChanged(adapterPosition)
                }
            }

            // Layouts e textos
            // ✅ NOVO: Usar o tipo da mesa como título principal
            binding.tvNumeroMesa.text = "${mesa.tipoMesa.getDisplayName()} ${mesa.numero}"
            binding.tvTipoMesa.text = mesa.tipoMesa.getDisplayName()
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
            
            // ✅ NOVO: Configurar botão de câmera
            binding.btnCameraRelogio.setOnClickListener {
                capturarFotoRelogio(mesa.id)
            }
            
            // ✅ NOVO: Configurar botão de remover foto
            binding.btnRemoverFoto.setOnClickListener {
                removerFotoRelogio(mesa.id)
            }
            
            // ✅ NOVO: Mostrar foto se existir
            if (state.fotoRelogioFinal != null) {
                mostrarFotoRelogio(state.fotoRelogioFinal!!, state.dataFoto)
            } else {
                binding.layoutFotoRelogio.visibility = View.GONE
            }
        }
        
        /**
         * Captura foto do relógio final da mesa
         */
        private fun capturarFotoRelogio(mesaId: Long) {
            currentMesaId = mesaId
            // Notificar o Fragment sobre a necessidade de capturar foto
            Log.d("MesasAcertoAdapter", "Solicitando captura de foto para mesa ID: $mesaId")
            
            // Usar o callback
            onSolicitarCapturaFoto?.invoke(mesaId)
        }
        
        /**
         * Remove foto do relógio final da mesa
         */
        private fun removerFotoRelogio(mesaId: Long) {
            val state = mesaStates[mesaId]
            state?.let {
                it.fotoRelogioFinal = null
                it.dataFoto = null
                binding.layoutFotoRelogio.visibility = View.GONE
                onDataChanged()
                safeNotifyItemChanged(adapterPosition)
            }
        }
        
        /**
         * Mostra foto do relógio final da mesa
         */
        private fun mostrarFotoRelogio(caminhoFoto: String, dataFoto: Date?) {
            try {
                val file = File(caminhoFoto)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(caminhoFoto)
                    binding.ivFotoRelogio.setImageBitmap(bitmap)
                    binding.layoutFotoRelogio.visibility = View.VISIBLE
                    
                    // Mostrar data da foto
                    dataFoto?.let {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                        binding.tvDataFoto.text = "Capturada em: ${dateFormat.format(it)}"
                    }
                } else {
                    binding.layoutFotoRelogio.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("MesasAcertoAdapter", "Erro ao carregar foto: ${e.message}")
                binding.layoutFotoRelogio.visibility = View.GONE
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
                
                binding.tvSubtotal.text = "Subtotal: R$ %.2f".format(state.subtotal)
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
    
    // ✅ NOVO: Métodos para captura de foto
    
    /**
     * Define foto capturada para uma mesa específica
     */
    fun setFotoRelogio(mesaId: Long, caminhoFoto: String) {
        val state = mesaStates[mesaId]
        state?.let {
            it.fotoRelogioFinal = caminhoFoto
            it.dataFoto = Date()
            onFotoCapturada(mesaId, caminhoFoto, it.dataFoto!!)
            // Encontrar a posição da mesa na lista
            val position = currentList.indexOfFirst { mesa -> mesa.id == mesaId }
            if (position != -1) {
                safeNotifyItemChanged(position)
            }
        }
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