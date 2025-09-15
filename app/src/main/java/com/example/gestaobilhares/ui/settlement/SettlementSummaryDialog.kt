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
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.graphics.Typeface

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
            clienteTelefone: String?,
            mesas: List<Mesa>,
            total: Double,
            metodosPagamento: Map<String, Double>,
            observacao: String?,
            debitoAtual: Double = 0.0,
            debitoAnterior: Double = 0.0,
            desconto: Double = 0.0,
            valorTotalMesas: Double = 0.0,
            valorFicha: Double = 0.0,
            comissaoFicha: Double = 0.0
        ): SettlementSummaryDialog {
            val args = Bundle().apply {
                putString("clienteNome", clienteNome)
                putString("clienteTelefone", clienteTelefone)
                putParcelableArrayList("mesas", ArrayList(mesas))
                putDouble("total", total)
                putSerializable("metodosPagamento", HashMap(metodosPagamento))
                putString("observacao", observacao)
                putDouble("debitoAtual", debitoAtual)
                putDouble("debitoAnterior", debitoAnterior)
                putDouble("desconto", desconto)
                putDouble("valorTotalMesas", valorTotalMesas)
                putDouble("valorFicha", valorFicha)
                putDouble("comissaoFicha", comissaoFicha)
            }
            val fragment = SettlementSummaryDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_settlement_summary, null)
        val clienteNome = arguments?.getString("clienteNome") ?: ""
        val clienteTelefone = arguments?.getString("clienteTelefone")
        val mesas = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList("mesas", Mesa::class.java) ?: emptyList<Mesa>()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList<Mesa>("mesas") ?: emptyList<Mesa>()
        }
        val total = arguments?.getDouble("total") ?: 0.0
        val metodosPagamento = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("metodosPagamento", HashMap::class.java) as? HashMap<String, Double> ?: hashMapOf()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("metodosPagamento") as? HashMap<String, Double> ?: hashMapOf()
        }
        val observacao = arguments?.getString("observacao") ?: ""
        val debitoAtual = arguments?.getDouble("debitoAtual") ?: 0.0
        val debitoAnterior = arguments?.getDouble("debitoAnterior") ?: 0.0
        val desconto = arguments?.getDouble("desconto") ?: 0.0
        val valorTotalMesas = arguments?.getDouble("valorTotalMesas") ?: 0.0
        val valorFicha = arguments?.getDouble("valorFicha") ?: 0.0
        val comissaoFicha = arguments?.getDouble("comissaoFicha") ?: 0.0
        val valorFichaExibir = if (valorFicha > 0) valorFicha else if (comissaoFicha > 0) comissaoFicha else 0.0

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
        
        // ✅ RESUMO FINANCEIRO COMPLETO (igual ao recibo impresso)
        view.findViewById<TextView>(R.id.tvResumoDebitoAnterior).text = formatter.format(debitoAnterior)
        view.findViewById<TextView>(R.id.tvResumoTotalMesas).text = formatter.format(valorTotalMesas)
        
        val valorTotalCalculado = valorTotalMesas + debitoAnterior
        view.findViewById<TextView>(R.id.tvResumoTotal).text = formatter.format(valorTotalCalculado)
        
        view.findViewById<TextView>(R.id.tvResumoDesconto).text = formatter.format(desconto)
        
        val valorRecebido = metodosPagamento.values.sum()
        view.findViewById<TextView>(R.id.tvResumoValorRecebido).text = formatter.format(valorRecebido)
        
        view.findViewById<TextView>(R.id.tvResumoDebitoAtual).text = formatter.format(debitoAtual)
        
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
                android.widget.Toast.makeText(requireContext(), getString(R.string.impressao_bluetooth_nao_disponivel), android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!bluetoothAdapter.isEnabled) {
                android.widget.Toast.makeText(requireContext(), getString(R.string.impressao_bluetooth_ativar), android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val pairedDevices = bluetoothAdapter.bondedDevices
            if (pairedDevices.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), getString(R.string.impressao_nenhuma_impressora), android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Diálogo de seleção de impressora
            val deviceList = pairedDevices.toList()
            val deviceNames = deviceList.map { it.name ?: it.address }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.impressao_selecione_impressora))
                .setItems(deviceNames) { _, which ->
                    val printerDevice = deviceList[which]
                    val loadingDialog = LoadingDialog()
                    loadingDialog.show(childFragmentManager, "loading")
                    Thread {
                        var erro: String? = null
                        try {
                            val printerHelper = BluetoothPrinterHelper(printerDevice)
                            if (printerHelper.connect()) {
                                // Inflar o layout do recibo
                                val inflater = LayoutInflater.from(requireContext())
                                val reciboView = inflater.inflate(R.layout.layout_recibo_impressao, null) as ViewGroup
                                // Preencher campos
                                val txtTitulo = reciboView.findViewById<TextView>(R.id.txtTituloRecibo)
                                val txtClienteValor = reciboView.findViewById<TextView>(R.id.txtClienteValor)
                                val txtData = reciboView.findViewById<TextView>(R.id.txtData)
                                val txtMesas = reciboView.findViewById<TextView>(R.id.txtMesas)
                                val txtFichasJogadas = reciboView.findViewById<TextView>(R.id.txtFichasJogadas)
                                val txtDebitoAnterior = reciboView.findViewById<TextView>(R.id.txtDebitoAnterior)
                                val txtSubtotalMesas = reciboView.findViewById<TextView>(R.id.txtSubtotalMesas)
                                val txtTotal = reciboView.findViewById<TextView>(R.id.txtTotal)
                                val txtDesconto = reciboView.findViewById<TextView>(R.id.txtDesconto)
                                val txtValorRecebido = reciboView.findViewById<TextView>(R.id.txtValorRecebido)
                                val txtDebitoAtual = reciboView.findViewById<TextView>(R.id.txtDebitoAtual)
                                val txtPagamentos = reciboView.findViewById<TextView>(R.id.txtPagamentos)
                                val txtObservacoes = reciboView.findViewById<TextView>(R.id.txtObservacoes)
                                val imgLogo = reciboView.findViewById<ImageView>(R.id.imgLogoRecibo)
                                val txtValorFichaImpressao = reciboView.findViewById<TextView>(R.id.txtValorFicha)
                                val rowValorFicha = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowValorFicha)

                                // Preencher campos do recibo
                                // Cliente
                                txtClienteValor.text = clienteNome
                                // Data (apenas data, sem horário)
                                val dataFormatada = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                txtData.text = dataFormatada
                                // Mesas (formatação limpa sem quebras extras)
                                val mesasFormatadas = StringBuilder()
                                mesas.forEachIndexed { index, mesa ->
                                    val fichasJogadas = (mesa.fichasFinal ?: 0) - (mesa.fichasInicial ?: 0)
                                    mesasFormatadas.append("Mesa ${mesa.numero}\n${mesa.fichasInicial} → ${mesa.fichasFinal} (${fichasJogadas} fichas)")
                                    if (index < mesas.size - 1) mesasFormatadas.append("\n")
                                }
                                txtMesas.text = mesasFormatadas.toString()
                                // Fichas jogadas
                                val totalFichasJogadas = mesas.sumOf { (it.fichasFinal ?: 0) - (it.fichasInicial ?: 0) }
                                txtFichasJogadas.text = totalFichasJogadas.toString()
                                // Resumo Financeiro (sem duplicação e com rótulos únicos)
                                val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
                                txtDebitoAnterior.text = formatter.format(debitoAnterior)
                                txtSubtotalMesas.text = formatter.format(valorTotalMesas)
                                val valorTotal = valorTotalMesas + debitoAnterior
                                txtTotal.text = formatter.format(valorTotal)
                                txtDesconto.text = formatter.format(desconto)
                                val valorRecebidoSum = metodosPagamento.values.sum()
                                txtValorRecebido.text = formatter.format(valorRecebidoSum)
                                txtDebitoAtual.text = formatter.format(debitoAtual)
                                // Valor da ficha
                                if (valorFichaExibir > 0) {
                                    txtValorFichaImpressao.text = formatter.format(valorFichaExibir)
                                    rowValorFicha.visibility = android.view.View.VISIBLE
                                } else {
                                    rowValorFicha.visibility = android.view.View.GONE
                                }
                                // Forma de pagamento (formatação limpa)
                                val pagamentosFormatados = if (metodosPagamento.isNotEmpty()) {
                                    metodosPagamento.entries.joinToString("\n") { "${it.key}: ${formatter.format(it.value)}" }
                                } else {
                                    "Não informado"
                                }
                                txtPagamentos.text = pagamentosFormatados
                                // Observações
                                if (observacao.isNullOrBlank()) {
                                    txtObservacoes.text = "-"
                                } else {
                                    txtObservacoes.text = observacao
                                }
                                // Logo
                                imgLogo.setImageResource(R.drawable.logo_globo1)

                                // Ajustar estilos para títulos e valores principais
                                txtTitulo.setTypeface(null, Typeface.BOLD)
                                txtClienteValor.setTypeface(null, Typeface.BOLD)
                                txtMesas.setTypeface(null, Typeface.BOLD)
                                txtPagamentos.setTypeface(null, Typeface.BOLD)
                                txtObservacoes.setTypeface(null, Typeface.BOLD)
                                // Fontes menores já estão no layout XML
                                // Imprimir
                                printerHelper.printReciboLayoutBitmap(reciboView)
                                printerHelper.disconnect()
                            } else {
                                erro = getString(R.string.impressao_erro_conexao)
                            }
                        } catch (e: Exception) {
                            erro = when {
                                e.message?.contains("socket") == true -> getString(R.string.impressao_erro_desligada)
                                e.message?.contains("broken pipe") == true -> getString(R.string.impressao_erro_envio)
                                else -> getString(R.string.impressao_erro_generico, e.message ?: "Desconhecido")
                            }
                        }
                        requireActivity().runOnUiThread {
                            loadingDialog.dismiss()
                            if (erro == null) {
                                android.widget.Toast.makeText(requireContext(), getString(R.string.impressao_sucesso), android.widget.Toast.LENGTH_SHORT).show()
                                dismiss()
                                acertoCompartilhadoListener?.onAcertoCompartilhado()
                            } else {
                                AlertDialog.Builder(requireContext())
                                    .setTitle(getString(R.string.impressao_erro_titulo))
                                    .setMessage(erro)
                                    .setPositiveButton(getString(R.string.impressao_erro_tentar_novamente)) { _, _ ->
                                        view?.findViewById<MaterialButton>(R.id.btnImprimir)?.performClick()
                                    }
                                    .setNegativeButton(getString(R.string.impressao_erro_cancelar), null)
                                    .show()
                            }
                        }
                    }.start()
                }
                .setNegativeButton(getString(R.string.impressao_erro_cancelar), null)
                .show()
        }
        
        // Botão WhatsApp
        view.findViewById<MaterialButton>(R.id.btnWhatsapp).setOnClickListener {
            val textoCompleto = gerarTextoResumo(clienteNome, mesas, total, metodosPagamento, observacao, debitoAtual, debitoAnterior, desconto, valorTotalMesas, valorFichaExibir, comissaoFicha)
            enviarViaWhatsAppDireto(clienteTelefone, textoCompleto)
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
        valorTotalMesas: Double,
        valorFicha: Double,
        comissaoFicha: Double
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val texto = StringBuilder()
        texto.append("🎱 *ACERTO DE BILHAR*\n")
        texto.append("================================\n\n")
        texto.append("👤 *Cliente:* $clienteNome\n")
        texto.append("📅 *Data:* $dataAtual\n\n")
        // Mostrar também no cabeçalho para destaque
        if (valorFicha > 0) {
            texto.append("*Valor da ficha:* ${formatter.format(valorFicha)}\n")
        }

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
        
        if (debitoAnterior > 0) {
            texto.append("• Débito anterior: ${formatter.format(debitoAnterior)}\n")
        }
        
        texto.append("• Total das mesas: ${formatter.format(valorTotalMesas)}\n")
        if (valorFicha > 0) {
            texto.append("• Valor da ficha: ${formatter.format(valorFicha)}\n")
        }
        
        val valorTotal = valorTotalMesas + debitoAnterior
        texto.append("• Valor total: ${formatter.format(valorTotal)}\n")
        
        if (desconto > 0) {
            texto.append("• Desconto: ${formatter.format(desconto)}\n")
        }
        
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
     * ✅ SOLUÇÃO DEFINITIVA: Abre WhatsApp diretamente com o número do cliente
     * Baseado na documentação oficial WhatsApp e Android Intents
     * ELIMINA COMPLETAMENTE o seletor de apps
     */
    private fun enviarViaWhatsAppDireto(telefone: String?, texto: String) {
        if (telefone.isNullOrEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Cliente não possui telefone cadastrado", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Limpar formatação do telefone
            val numeroLimpo = telefone.replace(Regex("[^0-9]"), "")
            
            // Adicionar código do país se necessário (Brasil +55)
            val numeroCompleto = if (numeroLimpo.length == 11) {
                "55$numeroLimpo" // Adiciona código do Brasil
            } else if (numeroLimpo.length == 10) {
                "55$numeroLimpo" // Adiciona código do Brasil
            } else {
                numeroLimpo
            }
            
            Log.d("SettlementSummaryDialog", "Número original: $telefone")
            Log.d("SettlementSummaryDialog", "Número limpo: $numeroLimpo")
            Log.d("SettlementSummaryDialog", "Número completo: $numeroCompleto")
            
            // ✅ ESTRATÉGIA 1: Esquema nativo whatsapp://send (FORÇA direcionamento direto)
            try {
                val uri = android.net.Uri.parse("whatsapp://send?phone=$numeroCompleto&text=${android.net.Uri.encode(texto)}")
                val intentWhatsApp = Intent(Intent.ACTION_VIEW, uri).apply {
                    // ✅ CRÍTICO: Força o direcionamento direto sem seletor
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentWhatsApp)
                Log.d("SettlementSummaryDialog", "✅ WhatsApp aberto diretamente via esquema nativo")
                return
            } catch (e: Exception) {
                Log.d("SettlementSummaryDialog", "Esquema nativo não funcionou: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 2: URL wa.me (funciona mesmo sem app instalado)
            try {
                val url = "https://wa.me/$numeroCompleto?text=${android.net.Uri.encode(texto)}"
                val intentUrl = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(url)
                    // ✅ CRÍTICO: Força o direcionamento direto
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentUrl)
                Log.d("SettlementSummaryDialog", "✅ WhatsApp aberto via URL wa.me")
                return
            } catch (e: Exception) {
                Log.d("SettlementSummaryDialog", "URL wa.me não funcionou: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 3: Tentar WhatsApp Business via esquema nativo
            try {
                val uri = android.net.Uri.parse("whatsapp://send?phone=$numeroCompleto&text=${android.net.Uri.encode(texto)}")
                val intentBusiness = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentBusiness)
                Log.d("SettlementSummaryDialog", "✅ WhatsApp Business aberto via esquema nativo")
                return
            } catch (e: Exception) {
                Log.d("SettlementSummaryDialog", "WhatsApp Business não disponível: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 4: Intent direto com ACTION_SEND mas SEM chooser
            try {
                val intentDirect = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, texto)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentDirect)
                Log.d("SettlementSummaryDialog", "✅ WhatsApp aberto via intent direto")
                return
            } catch (e: Exception) {
                Log.d("SettlementSummaryDialog", "Intent direto falhou: ${e.message}")
            }
            
            // ✅ ÚLTIMA OPÇÃO: Mostrar mensagem de erro
            android.widget.Toast.makeText(requireContext(), "Não foi possível abrir o WhatsApp. Verifique se está instalado.", android.widget.Toast.LENGTH_LONG).show()
            Log.e("SettlementSummaryDialog", "❌ Todas as estratégias falharam")
            
        } catch (e: Exception) {
            Log.e("SettlementSummaryDialog", "Erro geral ao abrir WhatsApp: ${e.message}", e)
            android.widget.Toast.makeText(requireContext(), "Erro ao abrir WhatsApp: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ MANTIDO: Método original para compatibilidade
     */
    private fun enviarViaWhatsApp(texto: String) {
        enviarViaWhatsAppDireto(null, texto)
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
        texto.append(centerText(getString(R.string.recibo_titulo), 32)).append("\n")
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
        if (debitoAnterior > 0) texto.append("Débito anterior: ${formatter.format(debitoAnterior)}\n")
        texto.append("Total das mesas: ${formatter.format(valorTotalMesas)}\n")
        if (desconto > 0) texto.append("Desconto: ${formatter.format(desconto)}\n")
        val valorTotal = valorTotalMesas + debitoAnterior
        texto.append("Valor total: ${formatter.format(valorTotal)}\n")
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
        texto.append(centerText(getString(R.string.recibo_agradecimento), 32)).append("\n\n")
        return texto.toString()
    }

    // Funções utilitárias para alinhamento (publicas para testes)
    fun padRight(text: String, length: Int): String = text.padEnd(length, ' ')
    fun padLeft(text: String, length: Int): String = text.padStart(length, ' ')

    // Função utilitária para centralizar texto na largura da impressora (publica para testes)
    fun centerText(text: String, width: Int): String {
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
                android.widget.Toast.makeText(requireContext(), getString(R.string.impressao_permissao_necessaria), android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // Novo método para recibo profissional - Versão completa (mesmos dados do WhatsApp)
    private fun gerarTextoReciboProfissional(
        clienteNome: String,
        mesas: List<Mesa>,
        total: Double,
        metodosPagamento: Map<String, Double>,
        observacao: String,
        debitoAtual: Double,
        debitoAnterior: Double,
        desconto: Double,
        valorTotalMesas: Double,
        valorFicha: Double = 0.0,
        comissaoFicha: Double = 0.0
    ): String {
        val sb = StringBuilder()
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
        val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        
        sb.appendLine("********* RECIBO DE ACERTO *********")
        sb.appendLine("=====================================")
        sb.appendLine("Cliente: $clienteNome")
        if (valorFicha > 0) {
            sb.appendLine("Valor da Ficha: ${formatter.format(valorFicha)}")
        }
        sb.appendLine("Data: $dataAtual")
        sb.appendLine("")
        sb.appendLine("MESAS ACERTADAS:")
        var totalFichasJogadas = 0
        mesas.forEach { mesa ->
            val fichasJogadas = (mesa.fichasFinal ?: 0) - (mesa.fichasInicial ?: 0)
            totalFichasJogadas += fichasJogadas
            sb.appendLine("Mesa ${mesa.numero}: ${mesa.fichasInicial} -> ${mesa.fichasFinal} (${fichasJogadas} fichas)")
        }
        sb.appendLine("Total de fichas jogadas: $totalFichasJogadas")
        sb.appendLine("")
        sb.appendLine("RESUMO FINANCEIRO:")
        if (debitoAnterior > 0) {
            sb.appendLine("Débito anterior: ${formatter.format(debitoAnterior)}")
        }
        sb.appendLine("Total das mesas: ${formatter.format(valorTotalMesas)}")
        if (desconto > 0) {
            sb.appendLine("Desconto: ${formatter.format(desconto)}")
        }
        sb.appendLine("Valor total: ${formatter.format(total)}")
        if (metodosPagamento.isNotEmpty()) {
            val valorRecebido = metodosPagamento.values.sum()
            sb.appendLine("Valor recebido: ${formatter.format(valorRecebido)}")
        }
        if (debitoAtual > 0) {
            sb.appendLine("Débito atual: ${formatter.format(debitoAtual)}")
        }
        sb.appendLine("")
        if (metodosPagamento.isNotEmpty()) {
            sb.appendLine("FORMA DE PAGAMENTO:")
            metodosPagamento.forEach { (metodo, valor) ->
                sb.appendLine("$metodo: ${formatter.format(valor)}")
            }
            sb.appendLine("")
        }
        if (observacao.isNotBlank()) {
            sb.appendLine("Observações: $observacao")
            sb.appendLine("")
        }
        sb.appendLine("=====================================")
        sb.appendLine("Acerto realizado via GestaoBilhares")
        sb.appendLine("Obrigado por confiar!")
        return sb.toString()
    }
}

// Classe LoadingDialog movida para fora, agora pública e de nível superior
class LoadingDialog : androidx.fragment.app.DialogFragment() {
    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): android.app.Dialog {
        val view = android.view.LayoutInflater.from(context).inflate(com.example.gestaobilhares.R.layout.dialog_loading_impressao, null)
        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
    }
} 