package com.example.gestaobilhares.ui.settlement

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Mesa
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.*

class SettlementSummaryDialog : DialogFragment() {
    interface OnAcertoCompartilhadoListener {
        fun onAcertoCompartilhado()
    }
    var acertoCompartilhadoListener: OnAcertoCompartilhadoListener? = null
    companion object {
        fun newInstance(
            clienteNome: String,
            mesas: List<Mesa>,
            total: Double,
            metodosPagamento: Map<String, Double>,
            observacao: String?,
            debitoAtual: Double = 0.0
        ): SettlementSummaryDialog {
            val args = Bundle().apply {
                putString("clienteNome", clienteNome)
                putParcelableArrayList("mesas", ArrayList(mesas))
                putDouble("total", total)
                putSerializable("metodosPagamento", HashMap(metodosPagamento))
                putString("observacao", observacao)
                putDouble("debitoAtual", debitoAtual)
            }
            val fragment = SettlementSummaryDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_settlement_summary, null)
        val clienteNome = arguments?.getString("clienteNome") ?: ""
        val mesas = arguments?.getParcelableArrayList<Mesa>("mesas") ?: emptyList<Mesa>()
        val total = arguments?.getDouble("total") ?: 0.0
        val metodosPagamento = arguments?.getSerializable("metodosPagamento") as? HashMap<String, Double> ?: hashMapOf()
        val observacao = arguments?.getString("observacao") ?: ""
        val debitoAtual = arguments?.getDouble("debitoAtual") ?: 0.0

        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        // Informações do cliente
        view.findViewById<TextView>(R.id.tvResumoCliente).text = clienteNome
        
        // Detalhes das mesas com fichas jogadas
        val mesasDetalhes = StringBuilder()
        var totalFichasJogadas = 0
        mesas.forEachIndexed { index, mesa ->
            val fichasJogadas = (mesa.fichasFinal ?: 0) - (mesa.fichasInicial ?: 0)
            totalFichasJogadas += fichasJogadas
            mesasDetalhes.append("${mesa.numero}: ${mesa.fichasInicial} → ${mesa.fichasFinal} (${fichasJogadas} fichas)")
            if (index < mesas.size - 1) mesasDetalhes.append("\n")
        }
        view.findViewById<TextView>(R.id.tvResumoMesas).text = mesasDetalhes.toString()
        
        // Total formatado
        view.findViewById<TextView>(R.id.tvResumoTotal).text = formatter.format(total)
        
        // Métodos de pagamento formatados
        val pagamentosText = if (metodosPagamento.isNotEmpty()) {
            metodosPagamento.entries.joinToString("\n") { "${it.key}: ${formatter.format(it.value)}" }
        } else {
            "Não informado"
        }
        view.findViewById<TextView>(R.id.tvResumoPagamentos).text = pagamentosText
        
        // Observação
        view.findViewById<TextView>(R.id.tvResumoObservacao).text = observacao

        // Botão Imprimir
        view.findViewById<MaterialButton>(R.id.btnImprimir).setOnClickListener {
            // TODO: Implementar impressão térmica 58mm
            android.widget.Toast.makeText(requireContext(), "Funcionalidade de impressão será implementada em breve!", android.widget.Toast.LENGTH_SHORT).show()
            dismiss()
        }
        
        // Botão WhatsApp
        view.findViewById<MaterialButton>(R.id.btnWhatsapp).setOnClickListener {
            val textoCompleto = gerarTextoResumo(clienteNome, mesas, total, metodosPagamento, observacao, debitoAtual)
            enviarViaWhatsApp(textoCompleto)
            dismiss()
            acertoCompartilhadoListener?.onAcertoCompartilhado()
        }
        
        return android.app.AlertDialog.Builder(requireContext())
            .setTitle("📋 Resumo do Acerto")
            .setView(view)
            .setCancelable(true)
            .create()
    }
    
    /**
     * Gera texto formatado do resumo para compartilhamento
     */
    private fun gerarTextoResumo(
        clienteNome: String,
        mesas: List<Mesa>,
        total: Double,
        metodosPagamento: Map<String, Double>,
        observacao: String,
        debitoAtual: Double
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val texto = StringBuilder()
        texto.append("🎱 *ACERTO DE BILHAR*\n")
        texto.append("================================\n\n")
        texto.append("👤 *Cliente:* $clienteNome\n")
        texto.append("📅 *Data:* $dataAtual\n\n")

        texto.append("🎯 *MESAS ACERTADAS:*\n")
        var totalFichasJogadas = 0
        mesas.forEach { mesa ->
            val fichasJogadas = (mesa.fichasFinal ?: 0) - (mesa.fichasInicial ?: 0)
            totalFichasJogadas += fichasJogadas
            texto.append("• Mesa ${mesa.numero}: ${mesa.fichasInicial} → ${mesa.fichasFinal} (${fichasJogadas} fichas)\n")
        }
        texto.append("\n*Total de fichas jogadas: $totalFichasJogadas*\n\n")

        texto.append("💰 *RESUMO FINANCEIRO:*\n")
        texto.append("• *Valor total: ${formatter.format(total)}*\n")
        if (metodosPagamento.isNotEmpty()) {
            val valorRecebido = metodosPagamento.values.sum()
            texto.append("• Valor recebido: ${formatter.format(valorRecebido)}\n")
        }
        if (debitoAtual > 0) {
            texto.append("• Débito atual: ${formatter.format(debitoAtual)}\n")
        }
        texto.append("\n")

        if (metodosPagamento.isNotEmpty()) {
            texto.append("💳 *FORMA DE PAGAMENTO:*\n")
            metodosPagamento.forEach { (metodo, valor) ->
                texto.append("• $metodo: ${formatter.format(valor)}\n")
            }
            texto.append("\n")
        }

        if (observacao.isNotBlank()) {
            texto.append("📝 *Observações:* $observacao\n\n")
        }

        texto.append("--------------------------------\n")
        texto.append("✅ Acerto realizado via GestaoBilhares")
        return texto.toString()
    }
    
    /**
     * ✅ CORRIGIDO: Envia o resumo via WhatsApp nativo
     */
    private fun enviarViaWhatsApp(texto: String) {
        try {
            // ✅ CORREÇÃO: Usar intent específico para WhatsApp nativo
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, texto)
                type = "text/plain"
                setPackage("com.whatsapp") // Força abertura no app nativo
            }
            
            if (sendIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(sendIntent)
            } else {
                // WhatsApp não instalado, tentar com WhatsApp Business
                val intentBusiness = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, texto)
                    type = "text/plain"
                    setPackage("com.whatsapp.w4b") // WhatsApp Business
                }
                
                if (intentBusiness.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intentBusiness)
                } else {
                    // Nenhum WhatsApp instalado, usar compartilhamento genérico
                    val genericIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, texto)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(genericIntent, "Compartilhar resumo do acerto"))
                }
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "Erro ao compartilhar: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
} 