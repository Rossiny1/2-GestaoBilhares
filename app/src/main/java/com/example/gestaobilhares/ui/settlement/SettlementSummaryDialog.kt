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
import android.util.Log

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
            debitoAtual: Double = 0.0,
            debitoAnterior: Double = 0.0,
            desconto: Double = 0.0,
            valorTotalMesas: Double = 0.0
        ): SettlementSummaryDialog {
            val args = Bundle().apply {
                putString("clienteNome", clienteNome)
                putParcelableArrayList("mesas", ArrayList(mesas))
                putDouble("total", total)
                putSerializable("metodosPagamento", HashMap(metodosPagamento))
                putString("observacao", observacao)
                putDouble("debitoAtual", debitoAtual)
                putDouble("debitoAnterior", debitoAnterior)
                putDouble("desconto", desconto)
                putDouble("valorTotalMesas", valorTotalMesas)
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
        val debitoAnterior = arguments?.getDouble("debitoAnterior") ?: 0.0
        val desconto = arguments?.getDouble("desconto") ?: 0.0
        val valorTotalMesas = arguments?.getDouble("valorTotalMesas") ?: 0.0

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
            // 1. Selecionar dispositivo Bluetooth
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                android.widget.Toast.makeText(requireContext(), "Bluetooth não disponível neste dispositivo.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!bluetoothAdapter.isEnabled) {
                android.widget.Toast.makeText(requireContext(), "Ative o Bluetooth para imprimir.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val pairedDevices = bluetoothAdapter.bondedDevices
            if (pairedDevices.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Nenhuma impressora Bluetooth pareada.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Simples: pega o primeiro dispositivo pareado (ideal: mostrar lista para o usuário)
            val printerDevice = pairedDevices.first()
            val printerHelper = BluetoothPrinterHelper(printerDevice)
            if (printerHelper.connect()) {
                val textoRecibo = gerarTextoReciboImpressao(clienteNome, mesas, total, metodosPagamento, observacao, debitoAtual, debitoAnterior, desconto, valorTotalMesas)
                printerHelper.printText(textoRecibo)
                printerHelper.disconnect()
                android.widget.Toast.makeText(requireContext(), "Recibo enviado para impressão!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(requireContext(), "Falha ao conectar à impressora.", android.widget.Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        // Botão WhatsApp
        view.findViewById<MaterialButton>(R.id.btnWhatsapp).setOnClickListener {
            val textoCompleto = gerarTextoResumo(clienteNome, mesas, total, metodosPagamento, observacao, debitoAtual, debitoAnterior, desconto, valorTotalMesas)
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
        debitoAtual: Double,
        debitoAnterior: Double,
        desconto: Double,
        valorTotalMesas: Double
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
            // ✅ CORREÇÃO: Usar o número real da mesa em negrito
            texto.append("• *Mesa ${mesa.numero}*: ${mesa.fichasInicial} → ${mesa.fichasFinal} (${fichasJogadas} fichas)\n")
        }
        texto.append("\n*Total de fichas jogadas: $totalFichasJogadas*\n\n")

        texto.append("💰 *RESUMO FINANCEIRO:*\n")
        
        // ✅ CORREÇÃO: Reorganizar campos conforme solicitado - Débito anterior primeiro
        if (debitoAnterior > 0) {
            texto.append("• Débito anterior: ${formatter.format(debitoAnterior)}\n")
        }
        
        // ✅ CORREÇÃO: Usar valor total das mesas do banco de dados
        texto.append("• Total das mesas: ${formatter.format(valorTotalMesas)}\n")
        
        if (desconto > 0) {
            texto.append("• Desconto: ${formatter.format(desconto)}\n")
        }
        
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
     * Usa a mesma estratégia robusta do ClientDetailFragment
     */
    private fun enviarViaWhatsApp(texto: String) {
        try {
            // ✅ ESTRATÉGIA 1: Tentar WhatsApp nativo primeiro
            try {
                val intentWhatsApp = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, texto)
                    setPackage("com.whatsapp")
                }
                
                if (intentWhatsApp.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intentWhatsApp)
                    Log.d("SettlementSummaryDialog", "✅ WhatsApp nativo aberto com sucesso")
                    return
                }
            } catch (e: Exception) {
                Log.d("SettlementSummaryDialog", "WhatsApp nativo não disponível: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 2: Tentar WhatsApp Business
            try {
                val intentBusiness = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, texto)
                    setPackage("com.whatsapp.w4b")
                }
                
                if (intentBusiness.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intentBusiness)
                    Log.d("SettlementSummaryDialog", "✅ WhatsApp Business aberto com sucesso")
                    return
                }
            } catch (e: Exception) {
                Log.d("SettlementSummaryDialog", "WhatsApp Business não disponível: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 3: Compartilhamento genérico
            try {
                val intentGeneric = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, texto)
                }
                
                val chooser = Intent.createChooser(intentGeneric, "Compartilhar resumo do acerto")
                startActivity(chooser)
                Log.d("SettlementSummaryDialog", "✅ Compartilhamento genérico aberto")
                return
            } catch (e: Exception) {
                Log.d("SettlementSummaryDialog", "Compartilhamento genérico falhou: ${e.message}")
            }
            
            // ✅ ÚLTIMA OPÇÃO: Mostrar mensagem de erro
            android.widget.Toast.makeText(requireContext(), "Não foi possível abrir o WhatsApp. Verifique se está instalado.", android.widget.Toast.LENGTH_LONG).show()
            Log.e("SettlementSummaryDialog", "❌ Todas as estratégias falharam")
            
        } catch (e: Exception) {
            Log.e("SettlementSummaryDialog", "Erro geral ao abrir WhatsApp: ${e.message}", e)
            android.widget.Toast.makeText(requireContext(), "Erro ao compartilhar: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Gera texto formatado do recibo para impressão térmica 58mm
     */
    private fun gerarTextoReciboImpressao(
        clienteNome: String,
        mesas: List<Mesa>,
        total: Double,
        metodosPagamento: Map<String, Double>,
        observacao: String,
        debitoAtual: Double,
        debitoAnterior: Double,
        desconto: Double,
        valorTotalMesas: Double
    ): String {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
        val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val texto = StringBuilder()
        texto.append(centerText("ACERTO DE BILHAR", 32)).append("\n")
        texto.append("--------------------------------\n")
        texto.append("Cliente: $clienteNome\n")
        texto.append("Data: $dataAtual\n")
        texto.append("\nMESAS:\n")
        var totalFichasJogadas = 0
        mesas.forEach { mesa ->
            val fichasJogadas = (mesa.fichasFinal ?: 0) - (mesa.fichasInicial ?: 0)
            totalFichasJogadas += fichasJogadas
            texto.append("Mesa ${mesa.numero}: ${mesa.fichasInicial}->${mesa.fichasFinal} (${fichasJogadas})\n")
        }
        texto.append("Total fichas: $totalFichasJogadas\n")
        texto.append("\nRESUMO:\n")
        if (debitoAnterior > 0) texto.append("Débito ant.: ${formatter.format(debitoAnterior)}\n")
        texto.append("Total mesas: ${formatter.format(valorTotalMesas)}\n")
        if (desconto > 0) texto.append("Desconto: ${formatter.format(desconto)}\n")
        texto.append("Valor total: ${formatter.format(total)}\n")
        if (metodosPagamento.isNotEmpty()) {
            val valorRecebido = metodosPagamento.values.sum()
            texto.append("Recebido: ${formatter.format(valorRecebido)}\n")
        }
        if (debitoAtual > 0) texto.append("Débito atual: ${formatter.format(debitoAtual)}\n")
        texto.append("\nPAGAMENTO:\n")
        metodosPagamento.forEach { (metodo, valor) ->
            texto.append("$metodo: ${formatter.format(valor)}\n")
        }
        if (observacao.isNotBlank()) {
            texto.append("Obs: $observacao\n")
        }
        texto.append("\n-------------------------------\n")
        texto.append(centerText("GestaoBilhares", 32)).append("\n\n")
        return texto.toString()
    }

    // Função utilitária para centralizar texto na largura da impressora
    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text
        val left = ((width - text.length) / 2.0).toInt()
        return " ".repeat(left) + text
    }
} 