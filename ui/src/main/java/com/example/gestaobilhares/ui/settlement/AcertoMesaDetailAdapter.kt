package com.example.gestaobilhares.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.ui.databinding.ItemAcertoMesaDetailBinding
import java.text.NumberFormat
import java.util.*
import android.view.View
import timber.log.Timber

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

        Timber.d("=== ADAPTER INICIALIZADO ===")
        Timber.d("Total de mesas recebidas: ${mesas.size}")
        Timber.d("Tipo de acerto: $tipoAcerto")
        Timber.d("Pano trocado: $panoTrocado")
        Timber.d("Número do pano: $numeroPano")
        Timber.d("Mesas completas disponíveis: ${mesasCompletas.size}")
        
        // ✅ VERIFICAÇÃO CRÍTICA: Se não há mesas, isso é um erro
        if (mesas.isEmpty()) {
            Timber.d("❌ PROBLEMA CRÍTICO: Lista de mesas está vazia!")
            Timber.d("O adapter não vai exibir nenhuma mesa")
        }
        
        mesas.forEachIndexed { index, mesa ->
            Timber.d("=== MESA ${index + 1} NO ADAPTER ===")
            Timber.d("Mesa ID: ${mesa.mesaId}")
            Timber.d("Acerto ID: ${mesa.acertoId}")
            Timber.d("Subtotal: R$ ${mesa.subtotal}")
            Timber.d("Valor fixo: R$ ${mesa.valorFixo}")
            Timber.d("Fichas jogadas: ${mesa.fichasJogadas}")
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
        Timber.d("Criando ViewHolder para posição")
        val binding = ItemAcertoMesaDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AcertoMesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AcertoMesaViewHolder, position: Int) {
        Timber.d("=== BINDING POSIÇÃO $position ===")
        Timber.d("Mesa ID: ${mesas[position].mesaId}")
        Timber.d("Mesa Subtotal: ${mesas[position].subtotal}")
        holder.bind(mesas[position])
    }

    override fun getItemCount(): Int {
        val count = mesas.size
        Timber.d("getItemCount() chamado: $count")
        return count
    }

    inner class AcertoMesaViewHolder(private val binding: ItemAcertoMesaDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: AcertoMesa) {
            Timber.d("=== BINDING MESA ${mesa.mesaId} ===")
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val mesaCompleta = mesasCompletas[mesa.mesaId]
            val numeroMesa = mesaCompleta?.numero ?: mesa.mesaId.toString()
            val tipoMesa = mesaCompleta?.tipo ?: "Sinuca"
            
            Timber.d("Número da mesa: $numeroMesa")
            Timber.d("Tipo da mesa: $tipoMesa")
            Timber.d("Subtotal: ${formatter.format(mesa.subtotal)}")
            
            // ✅ NOVO: Usar o tipo da mesa como título principal
            binding.tvMesaNumero.text = "${tipoMesa} $numeroMesa"
            binding.tvTipoMesa.text = tipoMesa

            if (mesa.valorFixo > 0) {
                // Valor fixo: mostrar valor mensal, esconder relógios/fichas
                Timber.d("Configurando para valor fixo: ${formatter.format(mesa.valorFixo)}")
                binding.layoutRelogiosFichas.visibility = View.GONE
                binding.layoutValorMensal.visibility = View.VISIBLE
                binding.tvValorMensal.text = "${formatter.format(mesa.valorFixo)}/mês"
            } else {
                // Fichas: mostrar relógios/fichas, esconder valor mensal
                Timber.d("Configurando para fichas: ${mesa.relogioInicial} → ${mesa.relogioFinal}")
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
            Timber.d("=== DEBUG FOTO MESA ${mesa.mesaId} ===")
            Timber.d("Foto relógio final: '${mesa.fotoRelogioFinal}'")
            Timber.d("Foto é nula? ${mesa.fotoRelogioFinal == null}")
            Timber.d("Foto está vazia? ${mesa.fotoRelogioFinal?.isEmpty()}")
            Timber.d("Foto tem conteúdo? ${!mesa.fotoRelogioFinal.isNullOrEmpty()}")
            
            if (!mesa.fotoRelogioFinal.isNullOrEmpty()) {
                Timber.d("✅ Foto encontrada para mesa ${mesa.mesaId}, mostrando botão")
                binding.layoutFotoRelogio.visibility = View.VISIBLE
                binding.btnVerFoto.setOnClickListener {
                    Timber.d("Botão Ver Foto clicado para mesa ${mesa.mesaId}")
                    onVerFotoRelogio?.invoke(mesa.fotoRelogioFinal!!, mesa.dataFoto)
                }
            } else {
                Timber.d("❌ Nenhuma foto para mesa ${mesa.mesaId}, ocultando botão")
                binding.layoutFotoRelogio.visibility = View.GONE
            }
            
            Timber.d("Mesa ${mesa.mesaId} configurada com sucesso")
        }
    }
} 
