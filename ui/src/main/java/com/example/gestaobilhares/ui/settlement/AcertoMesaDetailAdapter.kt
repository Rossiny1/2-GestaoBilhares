package com.example.gestaobilhares.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.ui.databinding.ItemAcertoMesaDetailBinding
import java.text.NumberFormat
import java.util.*
import android.view.View

/**
 * Adapter melhorado para exibir detalhes das mesas do acerto
 * Suporta dados completos das mesas incluindo numeração e tipo
 */
class AcertoMesaDetailAdapter(
    private val mesas: List<AcertoMesa>,
    private val tipoAcerto: String = "Presencial",
    private val panoTrocado: Boolean = false,
    private val numeroPano: String? = null,
    private val mesasCompletas: Map<Long, MesaCompleta> = emptyMap(),
    private val onVerFotoRelogio: ((String, Date?) -> Unit)? = null
) : RecyclerView.Adapter<AcertoMesaDetailAdapter.AcertoMesaViewHolder>() {

    init {
        android.util.Log.d("AcertoMesaDetailAdapter", "=== ADAPTER INICIALIZADO ===")
        android.util.Log.d("AcertoMesaDetailAdapter", "Total de mesas recebidas: ${mesas.size}")
        android.util.Log.d("AcertoMesaDetailAdapter", "Tipo de acerto: $tipoAcerto")
        android.util.Log.d("AcertoMesaDetailAdapter", "Pano trocado: $panoTrocado")
        android.util.Log.d("AcertoMesaDetailAdapter", "Número do pano: $numeroPano")
        android.util.Log.d("AcertoMesaDetailAdapter", "Mesas completas disponíveis: ${mesasCompletas.size}")
        
        // ✅ VERIFICAÇÃO CRÍTICA: Se não há mesas, isso é um erro
        if (mesas.isEmpty()) {
            android.util.Log.d("AcertoMesaDetailAdapter", "❌ PROBLEMA CRÍTICO: Lista de mesas está vazia!")
            android.util.Log.d("AcertoMesaDetailAdapter", "O adapter não vai exibir nenhuma mesa")
        }
        
        mesas.forEachIndexed { index, mesa ->
            android.util.Log.d("AcertoMesaDetailAdapter", "=== MESA ${index + 1} NO ADAPTER ===")
            android.util.Log.d("AcertoMesaDetailAdapter", "Mesa ID: ${mesa.mesaId}")
            android.util.Log.d("AcertoMesaDetailAdapter", "Acerto ID: ${mesa.acertoId}")
            android.util.Log.d("AcertoMesaDetailAdapter", "Subtotal: R$ ${mesa.subtotal}")
            android.util.Log.d("AcertoMesaDetailAdapter", "Valor fixo: R$ ${mesa.valorFixo}")
            android.util.Log.d("AcertoMesaDetailAdapter", "Fichas jogadas: ${mesa.fichasJogadas}")
        }
    }

    /**
     * Data class para armazenar dados completos da mesa
     */
    data class MesaCompleta(
        val numero: String,
        val tipo: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AcertoMesaViewHolder {
        android.util.Log.d("AcertoMesaDetailAdapter", "Criando ViewHolder para posição")
        val binding = ItemAcertoMesaDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AcertoMesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AcertoMesaViewHolder, position: Int) {
        android.util.Log.d("AcertoMesaDetailAdapter", "=== BINDING POSIÇÃO $position ===")
        android.util.Log.d("AcertoMesaDetailAdapter", "Mesa ID: ${mesas[position].mesaId}")
        android.util.Log.d("AcertoMesaDetailAdapter", "Mesa Subtotal: ${mesas[position].subtotal}")
        holder.bind(mesas[position])
    }

    override fun getItemCount(): Int {
        val count = mesas.size
        android.util.Log.d("AcertoMesaDetailAdapter", "getItemCount() chamado: $count")
        return count
    }

    inner class AcertoMesaViewHolder(private val binding: ItemAcertoMesaDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: AcertoMesa) {
            android.util.Log.d("AcertoMesaDetailAdapter", "=== BINDING MESA ${mesa.mesaId} ===")
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val mesaCompleta = mesasCompletas[mesa.mesaId]
            val numeroMesa = mesaCompleta?.numero ?: mesa.mesaId.toString()
            val tipoMesa = mesaCompleta?.tipo ?: "Sinuca"
            
            android.util.Log.d("AcertoMesaDetailAdapter", "Número da mesa: $numeroMesa")
            android.util.Log.d("AcertoMesaDetailAdapter", "Tipo da mesa: $tipoMesa")
            android.util.Log.d("AcertoMesaDetailAdapter", "Subtotal: ${formatter.format(mesa.subtotal)}")
            
            // ✅ NOVO: Usar o tipo da mesa como título principal
            binding.tvMesaNumero.text = "${tipoMesa} $numeroMesa"
            binding.tvTipoMesa.text = tipoMesa

            if (mesa.valorFixo > 0) {
                // Valor fixo: mostrar valor mensal, esconder relógios/fichas
                android.util.Log.d("AcertoMesaDetailAdapter", "Configurando para valor fixo: ${formatter.format(mesa.valorFixo)}")
                binding.layoutRelogiosFichas.visibility = View.GONE
                binding.layoutValorMensal.visibility = View.VISIBLE
                binding.tvValorMensal.text = "${formatter.format(mesa.valorFixo)}/mês"
            } else {
                // Fichas: mostrar relógios/fichas, esconder valor mensal
                android.util.Log.d("AcertoMesaDetailAdapter", "Configurando para fichas: ${mesa.relogioInicial} → ${mesa.relogioFinal}")
                binding.layoutRelogiosFichas.visibility = View.VISIBLE
                binding.layoutValorMensal.visibility = View.GONE
                binding.tvRelogioInicial.text = mesa.relogioInicial.toString()
                binding.tvRelogioFinal.text = mesa.relogioFinal.toString()
                binding.tvFichasJogadas.text = mesa.fichasJogadas.toString()
            }
            // Subtotal sempre visível
            binding.tvSubtotal.text = formatter.format(mesa.subtotal)
            // Informações adicionais
            binding.tvTipoAcerto.text = tipoAcerto
            binding.tvPanoTrocado.text = if (panoTrocado) {
                "Pano ${numeroPano ?: "N/A"}"
            } else {
                "Não trocado"
            }
            
            // ✅ NOVO: Configurar botão de visualizar foto do relógio final
            android.util.Log.d("AcertoMesaDetailAdapter", "=== DEBUG FOTO MESA ${mesa.mesaId} ===")
            android.util.Log.d("AcertoMesaDetailAdapter", "Foto relógio final: '${mesa.fotoRelogioFinal}'")
            android.util.Log.d("AcertoMesaDetailAdapter", "Foto é nula? ${mesa.fotoRelogioFinal == null}")
            android.util.Log.d("AcertoMesaDetailAdapter", "Foto está vazia? ${mesa.fotoRelogioFinal?.isEmpty()}")
            android.util.Log.d("AcertoMesaDetailAdapter", "Foto tem conteúdo? ${!mesa.fotoRelogioFinal.isNullOrEmpty()}")
            
            if (!mesa.fotoRelogioFinal.isNullOrEmpty()) {
                android.util.Log.d("AcertoMesaDetailAdapter", "✅ Foto encontrada para mesa ${mesa.mesaId}, mostrando botão")
                binding.layoutFotoRelogio.visibility = View.VISIBLE
                binding.btnVerFoto.setOnClickListener {
                    android.util.Log.d("AcertoMesaDetailAdapter", "Botão Ver Foto clicado para mesa ${mesa.mesaId}")
                    onVerFotoRelogio?.invoke(mesa.fotoRelogioFinal!!, mesa.dataFoto)
                }
            } else {
                android.util.Log.d("AcertoMesaDetailAdapter", "❌ Nenhuma foto para mesa ${mesa.mesaId}, ocultando botão")
                binding.layoutFotoRelogio.visibility = View.GONE
            }
            
            android.util.Log.d("AcertoMesaDetailAdapter", "Mesa ${mesa.mesaId} configurada com sucesso")
        }
    }
} 
