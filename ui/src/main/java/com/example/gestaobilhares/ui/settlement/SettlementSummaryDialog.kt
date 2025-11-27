package com.example.gestaobilhares.ui.settlement
import com.example.gestaobilhares.ui.R

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
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
import com.example.gestaobilhares.core.utils.ReciboPrinterHelper

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
            clienteCpf: String?,
            mesas: List<Mesa>,
            total: Double,
            metodosPagamento: Map<String, Double>,
            observacao: String?,
            debitoAtual: Double = 0.0,
            debitoAnterior: Double = 0.0,
            desconto: Double = 0.0,
            valorTotalMesas: Double = 0.0,
            valorFicha: Double = 0.0,
            comissaoFicha: Double = 0.0,
            acertoId: Long? = null,
            numeroContrato: String? = null
        ): SettlementSummaryDialog {
            val args = Bundle().apply {
                putString("clienteNome", clienteNome)
                putString("clienteTelefone", clienteTelefone)
                putString("clienteCpf", clienteCpf)
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
                acertoId?.let { putLong("acertoId", it) }
                numeroContrato?.let { putString("numeroContrato", it) }
            }
            val fragment = SettlementSummaryDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(com.example.gestaobilhares.ui.R.layout.dialog_settlement_summary, null)
        val clienteNome = arguments?.getString("clienteNome") ?: ""
        val clienteTelefone = arguments?.getString("clienteTelefone")
        val clienteCpf = arguments?.getString("clienteCpf")
        val mesas = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList("mesas", Mesa::class.java) ?: emptyList<Mesa>()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList<Mesa>("mesas") ?: emptyList<Mesa>()
        }
        // ✅ CORREÇÃO: Variável 'total' removida - não estava sendo usada
        val metodosPagamento = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            @Suppress("UNCHECKED_CAST")
            arguments?.getSerializable("metodosPagamento", HashMap::class.java) as? HashMap<String, Double> ?: hashMapOf()
        } else {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            arguments?.getSerializable("metodosPagamento") as? HashMap<String, Double> ?: hashMapOf()
        }
        val observacao = arguments?.getString("observacao") ?: ""
        val debitoAtual = arguments?.getDouble("debitoAtual") ?: 0.0
        val debitoAnterior = arguments?.getDouble("debitoAnterior") ?: 0.0
        val desconto = arguments?.getDouble("desconto") ?: 0.0
        val valorTotalMesas = arguments?.getDouble("valorTotalMesas") ?: 0.0
        val valorFicha = arguments?.getDouble("valorFicha") ?: 0.0
        val comissaoFicha = arguments?.getDouble("comissaoFicha") ?: 0.0
        val acertoId = arguments?.getLong("acertoId")
        val numeroContrato = arguments?.getString("numeroContrato")
        val valorFichaExibir = if (valorFicha > 0) valorFicha else if (comissaoFicha > 0) comissaoFicha else 0.0

        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        // Informações do cliente
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoCliente).text = clienteNome
        
        // Detalhes das mesas com fichas jogadas
        val mesasDetalhes = StringBuilder()
        var totalFichasJogadas = 0
        mesas.forEachIndexed { index, mesa ->
            // ✅ CORREÇÃO: relogioFinal e relogioInicial são Int (não nullable), elvis operator desnecessário
            val fichasJogadas = mesa.relogioFinal - mesa.relogioInicial
            totalFichasJogadas += fichasJogadas
            mesasDetalhes.append("Mesa ${mesa.numero}\n${mesa.relogioInicial} → ${mesa.relogioFinal} (${fichasJogadas} fichas)")
            if (index < mesas.size - 1) mesasDetalhes.append("\n")
        }
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoMesas).text = mesasDetalhes.toString()
        
        // ✅ RESUMO FINANCEIRO COMPLETO (igual ao recibo impresso)
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoDebitoAnterior).text = formatter.format(debitoAnterior)
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoTotalMesas).text = formatter.format(valorTotalMesas)
        
        val valorTotalCalculado = valorTotalMesas + debitoAnterior
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoTotal).text = formatter.format(valorTotalCalculado)
        
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoDesconto).text = formatter.format(desconto)
        
        val valorRecebido = metodosPagamento.values.sum()
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoValorRecebido).text = formatter.format(valorRecebido)
        
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoDebitoAtual).text = formatter.format(debitoAtual)
        
        // Métodos de pagamento formatados
        val pagamentosText = if (metodosPagamento.isNotEmpty()) {
            metodosPagamento.entries.joinToString("\n") { "${it.key}: ${formatter.format(it.value)}" }
        } else {
            "Não informado"
        }
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoPagamentos).text = pagamentosText
        
        // Observação
        view.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvResumoObservacao).text = observacao

        // ✅ BOTÃO IMPRIMIR UNIFICADO: Usa função centralizada
        view.findViewById<MaterialButton>(com.example.gestaobilhares.ui.R.id.btnImprimir).setOnClickListener {
            ReciboPrinterHelper.imprimirReciboUnificado(
                context = requireContext(),
                clienteNome = clienteNome,
                clienteCpf = clienteCpf,
                clienteTelefone = clienteTelefone,
                mesasCompletas = mesas,
                debitoAnterior = debitoAnterior,
                valorTotalMesas = valorTotalMesas,
                desconto = desconto,
                metodosPagamento = metodosPagamento,
                debitoAtual = debitoAtual,
                observacao = observacao,
                valorFicha = valorFichaExibir,
                acertoId = acertoId,
                numeroContrato = numeroContrato,
                onSucesso = {
                    android.widget.Toast.makeText(requireContext(), getString(com.example.gestaobilhares.ui.R.string.impressao_sucesso), android.widget.Toast.LENGTH_SHORT).show()
                    dismiss()
                    acertoCompartilhadoListener?.onAcertoCompartilhado()
                },
                onErro = { erro ->
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(getString(com.example.gestaobilhares.ui.R.string.impressao_erro_titulo))
                        .setMessage(erro)
                        .setPositiveButton(getString(com.example.gestaobilhares.ui.R.string.impressao_erro_tentar_novamente)) { _, _ ->
                            view?.findViewById<MaterialButton>(com.example.gestaobilhares.ui.R.id.btnImprimir)?.performClick()
                        }
                        .setNegativeButton(getString(com.example.gestaobilhares.ui.R.string.impressao_erro_cancelar), null)
                        .show()
                }
            )
        }
        
        // ✅ BOTÃO WHATSAPP UNIFICADO: Usa função centralizada
        view.findViewById<MaterialButton>(com.example.gestaobilhares.ui.R.id.btnWhatsapp).setOnClickListener {
            ReciboPrinterHelper.enviarWhatsAppUnificado(
                context = requireContext(),
                clienteNome = clienteNome,
                clienteCpf = clienteCpf,
                clienteTelefone = clienteTelefone,
                mesasCompletas = mesas,
                debitoAnterior = debitoAnterior,
                valorTotalMesas = valorTotalMesas,
                desconto = desconto,
                metodosPagamento = metodosPagamento,
                debitoAtual = debitoAtual,
                observacao = observacao,
                valorFicha = valorFichaExibir,
                acertoId = acertoId,
                numeroContrato = numeroContrato,
                onSucesso = {
                    dismiss()
                    acertoCompartilhadoListener?.onAcertoCompartilhado()
                },
                onErro = { erro ->
                    android.widget.Toast.makeText(requireContext(), erro, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
        
        return android.app.AlertDialog.Builder(requireContext())
            .setTitle("📋 Resumo do Acerto")
            .setView(view)
            .setCancelable(true)
            .create()
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

    // ✅ CORREÇÃO: onRequestPermissionsResult deprecated - usar registerForActivityResult
    @Deprecated("Deprecated in Java", ReplaceWith("registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())"))
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissão concedida, pode tentar imprimir novamente
                view?.findViewById<MaterialButton>(com.example.gestaobilhares.ui.R.id.btnImprimir)?.performClick()
            } else {
                android.widget.Toast.makeText(requireContext(), getString(com.example.gestaobilhares.ui.R.string.impressao_permissao_necessaria), android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

}

// Classe LoadingDialog movida para fora, agora pública e de nível superior
class LoadingDialog : androidx.fragment.app.DialogFragment() {
    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): android.app.Dialog {
        val view = android.view.LayoutInflater.from(context).inflate(com.example.gestaobilhares.ui.R.layout.dialog_loading_impressao, null)
        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
    }
} 
