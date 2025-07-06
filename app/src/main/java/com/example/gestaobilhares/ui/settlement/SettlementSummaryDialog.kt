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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AlertDialog
import android.app.ProgressDialog

class SettlementSummaryDialog : DialogFragment() {
    interface OnAcertoCompartilhadoListener {
        fun onAcertoCompartilhado()
    }
    var acertoCompartilhadoListener: OnAcertoCompartilhadoListener? = null
    private val REQUEST_BLUETOOTH_PERMISSIONS = 101
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

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
            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions()
                return@setOnClickListener
            }
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
            // Diálogo de seleção de impressora
            val deviceList = pairedDevices.toList()
            val deviceNames = deviceList.map { it.name ?: it.address }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Selecione a impressora")
                .setItems(deviceNames) { _, which ->
                    val printerDevice = deviceList[which]
                    val progressDialog = ProgressDialog(requireContext())
                    progressDialog.setMessage("Conectando e imprimindo...")
                    progressDialog.setCancelable(false)
                    progressDialog.show()
                    Thread {
                        var erro: String? = null
                        try {
                            val printerHelper = BluetoothPrinterHelper(printerDevice)
                            if (printerHelper.connect()) {
                                // Geração do recibo com layout profissional
                                val recibo = gerarTextoReciboProfissional(clienteNome, mesas, total)
                                val boldLines = listOf(0, recibo.lines().size - 2) // Título e total
                                val centerLines = listOf(0, recibo.lines().size - 1) // Título e rodapé
                                printerHelper.printText(recibo, boldLines, centerLines)
                                printerHelper.disconnect()
                            } else {
                                erro = "Falha ao conectar à impressora."
                            }
                        } catch (e: Exception) {
                            erro = when {
                                e.message?.contains("socket") == true -> "Impressora desligada ou fora de alcance."
                                e.message?.contains("broken pipe") == true -> "Falha ao enviar dados. Impressora pode estar desconectada."
                                else -> "Erro inesperado: ${e.message}"
                            }
                        }
                        requireActivity().runOnUiThread {
                            progressDialog.dismiss()
                            if (erro == null) {
                                android.widget.Toast.makeText(requireContext(), "Recibo enviado para impressão!", android.widget.Toast.LENGTH_SHORT).show()
                                dismiss()
                            } else {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Erro na impressão")
                                    .setMessage(erro)
                                    .setPositiveButton("Tentar novamente") { _, _ ->
                                        view?.findViewById<MaterialButton>(R.id.btnImprimir)?.performClick()
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .show()
                            }
                        }
                    }.start()
                }
                .setNegativeButton("Cancelar", null)
                .show()
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

    private fun hasBluetoothPermissions(): Boolean {
        return bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), bluetoothPermissions, REQUEST_BLUETOOTH_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissão concedida, pode tentar imprimir novamente
                view?.findViewById<MaterialButton>(R.id.btnImprimir)?.performClick()
            } else {
                android.widget.Toast.makeText(requireContext(), "Permissão Bluetooth necessária para imprimir.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // Novo método para recibo profissional
    private fun gerarTextoReciboProfissional(
        clienteNome: String,
        mesas: List<Mesa>,
        total: Double
    ): String {
        val sb = StringBuilder()
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
        val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        
        sb.appendLine("********* RECIBO DE ACERTO *********")
        sb.appendLine("--------------------------------")
        sb.appendLine("Cliente: $clienteNome")
        sb.appendLine("Data: $dataAtual")
        sb.appendLine("--------------------------------")
        sb.appendLine("Mesas:")
        mesas.forEach { mesa ->
            val fichasJogadas = (mesa.fichasFinal ?: 0) - (mesa.fichasInicial ?: 0)
            sb.appendLine("- Mesa ${mesa.numero}: ${fichasJogadas} fichas")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("Total: ${formatter.format(total)}")
        sb.appendLine("--------------------------------")
        sb.appendLine("Obrigado por confiar!")
        return sb.toString()
    }
} 