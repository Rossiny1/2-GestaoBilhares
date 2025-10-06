package com.example.gestaobilhares.utils

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper para preencher o layout de recibo de impressão de forma consistente
 * entre SettlementSummaryDialog e SettlementDetailFragment
 */
object ReciboPrinterHelper {
    
    /**
     * Preenche o layout de recibo com os dados fornecidos (versão com informações completas das mesas)
     * @param context Contexto da aplicação
     * @param reciboView View do layout de recibo já inflado
     * @param clienteNome Nome do cliente
     * @param clienteCpf CPF do cliente (opcional)
     * @param mesasCompletas Lista de mesas completas com informações de tipo
     * @param debitoAnterior Débito anterior do cliente
     * @param valorTotalMesas Valor total das mesas
     * @param desconto Desconto aplicado
     * @param metodosPagamento Métodos de pagamento utilizados
     * @param debitoAtual Débito atual após o acerto
     * @param observacao Observações do acerto
     * @param valorFicha Valor da ficha do cliente
     * @param acertoId ID do acerto (opcional, para títulos)
     */
    fun preencherReciboImpressaoCompleto(
        context: Context,
        reciboView: View,
        clienteNome: String,
        clienteCpf: String? = null,
        mesasCompletas: List<Mesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long? = null,
        numeroContrato: String? = null
    ) {
        // Referências dos elementos
        val txtTitulo = reciboView.findViewById<android.widget.TextView>(R.id.txtTituloRecibo)
        val txtClienteValor = reciboView.findViewById<android.widget.TextView>(R.id.txtClienteValor)
        val rowCpfCliente = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowCpfCliente)
        val txtCpfCliente = reciboView.findViewById<android.widget.TextView>(R.id.txtCpfCliente)
        val rowNumeroContrato = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowNumeroContrato)
        val txtNumeroContrato = reciboView.findViewById<android.widget.TextView>(R.id.txtNumeroContrato)
        val txtData = reciboView.findViewById<android.widget.TextView>(R.id.txtData)
        val rowValorFicha = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowValorFicha)
        val txtValorFicha = reciboView.findViewById<android.widget.TextView>(R.id.txtValorFicha)
        val txtMesas = reciboView.findViewById<android.widget.TextView>(R.id.txtMesas)
        val txtFichasJogadas = reciboView.findViewById<android.widget.TextView>(R.id.txtFichasJogadas)
        val txtDebitoAnterior = reciboView.findViewById<android.widget.TextView>(R.id.txtDebitoAnterior)
        val txtSubtotalMesas = reciboView.findViewById<android.widget.TextView>(R.id.txtSubtotalMesas)
        val txtTotal = reciboView.findViewById<android.widget.TextView>(R.id.txtTotal)
        val txtDesconto = reciboView.findViewById<android.widget.TextView>(R.id.txtDesconto)
        val txtValorRecebido = reciboView.findViewById<android.widget.TextView>(R.id.txtValorRecebido)
        val txtDebitoAtual = reciboView.findViewById<android.widget.TextView>(R.id.txtDebitoAtual)
        val txtPagamentos = reciboView.findViewById<android.widget.TextView>(R.id.txtPagamentos)
        val txtObservacoes = reciboView.findViewById<android.widget.TextView>(R.id.txtObservacoes)

        // Formatação
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // Título
        val titulo = if (acertoId != null) {
            "RECIBO DE ACERTO #${acertoId.toString().padStart(4, '0')}"
        } else {
            "RECIBO DE ACERTO"
        }
        txtTitulo.text = titulo

        // ✅ CORREÇÃO: Cliente - SEMPRE exibir (mesma lógica da data)
        txtClienteValor.text = clienteNome
        
        // CPF do cliente
        if (!clienteCpf.isNullOrBlank()) {
            txtCpfCliente.text = clienteCpf
            rowCpfCliente.visibility = View.VISIBLE
        } else {
            rowCpfCliente.visibility = View.GONE
        }
        
        // ✅ CORREÇÃO: Número do recibo - SEMPRE exibir (mesma lógica da data)
        val rowNumeroRecibo = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowNumeroRecibo)
        val txtNumeroRecibo = reciboView.findViewById<android.widget.TextView>(R.id.txtNumeroRecibo)
        txtNumeroRecibo.text = acertoId?.toString() ?: "N/A"
        rowNumeroRecibo.visibility = View.VISIBLE
        
        // ✅ CORREÇÃO: Número do contrato - SEMPRE exibir (mesma lógica da data)
        txtNumeroContrato.text = numeroContrato ?: "N/A"
        rowNumeroContrato.visibility = View.VISIBLE
        
        // Data (apenas o valor, o rótulo já existe no layout)
        txtData.text = dataFormatada

        // Valor da ficha - SEMPRE exibir
        txtValorFicha.text = formatter.format(valorFicha)
        rowValorFicha.visibility = View.VISIBLE

        // Mesas (formatação com tipo do equipamento e número real da mesa)
        val mesasFormatadas = StringBuilder()
        mesasCompletas.forEachIndexed { index, mesa ->
            val fichasJogadas = mesa.fichasFinal - mesa.fichasInicial
            val tipoEquipamento = getTipoEquipamentoNome(mesa.tipoMesa)
            // ✅ CORREÇÃO: Usar número real da mesa, não índice
            mesasFormatadas.append("$tipoEquipamento ${mesa.numero}\n${mesa.fichasInicial} → ${mesa.fichasFinal} (${fichasJogadas} fichas)")
            if (index < mesasCompletas.size - 1) mesasFormatadas.append("\n")
        }
        txtMesas.text = mesasFormatadas.toString()

        // Fichas jogadas
        val totalFichasJogadas = mesasCompletas.sumOf { it.fichasFinal - it.fichasInicial }
        txtFichasJogadas.text = totalFichasJogadas.toString()

        // Resumo Financeiro (sem duplicação e com rótulos únicos)
        txtDebitoAnterior.text = formatter.format(debitoAnterior)
        txtSubtotalMesas.text = formatter.format(valorTotalMesas)
        val valorTotal = valorTotalMesas + debitoAnterior
        txtTotal.text = formatter.format(valorTotal)
        txtDesconto.text = formatter.format(desconto)
        val valorRecebidoSum = metodosPagamento.values.sum()
        txtValorRecebido.text = formatter.format(valorRecebidoSum)
        txtDebitoAtual.text = formatter.format(debitoAtual)

        // Forma de pagamento (formatação limpa)
        val pagamentosFormatados = if (metodosPagamento.isNotEmpty()) {
            metodosPagamento.entries.joinToString("\n") { "${it.key}: ${formatter.format(it.value)}" }
        } else {
            "Não informado"
        }
        txtPagamentos.text = pagamentosFormatados

        // Observações - SEMPRE exibir
        if (observacao.isNullOrBlank()) {
            txtObservacoes.text = "Nenhuma observação registrada."
        } else {
            txtObservacoes.text = observacao
        }

        // Logo
        val imgLogo = reciboView.findViewById<android.widget.ImageView>(R.id.imgLogoRecibo)
        imgLogo.setImageResource(R.drawable.logo_globo1)

        // Ajustar estilos para títulos e valores principais
        txtTitulo.setTypeface(null, Typeface.BOLD)
        txtClienteValor.setTypeface(null, Typeface.BOLD)
        txtMesas.setTypeface(null, Typeface.BOLD)
        txtPagamentos.setTypeface(null, Typeface.BOLD)
        txtObservacoes.setTypeface(null, Typeface.BOLD)
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Gera texto para WhatsApp usando a mesma lógica do recibo impresso
     * FONTE ÚNICA DE VERDADE - Mesmo conteúdo do recibo impresso
     */
    fun gerarTextoWhatsApp(
        clienteNome: String,
        clienteCpf: String? = null,
        mesasCompletas: List<Mesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long? = null,
        numeroContrato: String? = null
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataAtual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val texto = StringBuilder()
        
        // ✅ TÍTULO (mesmo do recibo impresso)
        val titulo = if (acertoId != null) {
            "🎱 *RECIBO DE ACERTO #${acertoId.toString().padStart(4, '0')}*"
        } else {
            "🎱 *RECIBO DE ACERTO*"
        }
        texto.append("$titulo\n")
        texto.append("================================\n\n")
        
        // ✅ CLIENTE E CPF (mesmo do recibo impresso)
        texto.append("👤 *Cliente:* $clienteNome\n")
        if (!clienteCpf.isNullOrBlank()) {
            texto.append("📄 *CPF:* $clienteCpf\n")
        }
        if (!numeroContrato.isNullOrBlank()) {
            texto.append("📋 *Contrato:* $numeroContrato\n")
        }
        // Linha única entre Data e Preço da ficha (sem linha em branco)
        texto.append("📅 *Data:* $dataAtual\n")
        
        // ✅ CORREÇÃO: Sempre exibir preço da ficha, mesmo se for 0
        texto.append("💰 *Preço da ficha:* ${formatter.format(valorFicha)}\n")
        
        // ✅ CORREÇÃO: Quebra dupla antes de MESAS ACERTADAS
        texto.append("\n")
        
        // ✅ MESAS (formatação igual ao recibo impresso - nome da mesa em uma linha, relógios na linha de baixo)
        texto.append("🎯 *MESAS ACERTADAS:*\n")
        var totalFichasJogadas = 0
        mesasCompletas.forEach { mesa ->
            val fichasJogadas = mesa.fichasFinal - mesa.fichasInicial
            totalFichasJogadas += fichasJogadas
            val tipoEquipamento = getTipoEquipamentoNome(mesa.tipoMesa)
            // ✅ CORREÇÃO: Formatação igual ao impresso - nome da mesa em uma linha, relógios na linha de baixo
            texto.append("• *$tipoEquipamento ${mesa.numero}*\n")
            texto.append("  ${mesa.fichasInicial} → ${mesa.fichasFinal} (${fichasJogadas} fichas)\n")
        }
        if (totalFichasJogadas > 0) {
            texto.append("\n*Total de fichas jogadas: $totalFichasJogadas*\n\n")
        }
        
        // ✅ RESUMO FINANCEIRO (sempre exibe todos os campos, como no recibo impresso)
        texto.append("💰 *RESUMO FINANCEIRO:*\n")
        texto.append("• Débito anterior: ${formatter.format(debitoAnterior)}\n")
        texto.append("• Total das mesas: ${formatter.format(valorTotalMesas)}\n")
        texto.append("• Valor da ficha: ${formatter.format(valorFicha)}\n")
        val valorTotal = valorTotalMesas + debitoAnterior
        texto.append("• Valor total: ${formatter.format(valorTotal)}\n")
        texto.append("• Desconto: ${formatter.format(desconto)}\n")
        val valorRecebido = metodosPagamento.values.sum()
        texto.append("• Valor recebido: ${formatter.format(valorRecebido)}\n")
        texto.append("• Débito atual: ${formatter.format(debitoAtual)}\n")
        texto.append("\n")
        
        // ✅ FORMA DE PAGAMENTO (mesmo do recibo impresso)
        texto.append("💳 *FORMA DE PAGAMENTO:*\n")
        if (metodosPagamento.isNotEmpty()) {
            metodosPagamento.forEach { (metodo, valor) ->
                texto.append("• $metodo: ${formatter.format(valor)}\n")
            }
        } else {
            texto.append("Não informado\n")
        }
        texto.append("\n")
        
        // ✅ OBSERVAÇÕES - SEMPRE exibir (mesmo do recibo impresso)
        if (!observacao.isNullOrBlank()) {
            texto.append("📝 *Observações:* $observacao\n\n")
        } else {
            texto.append("📝 *Observações:* Nenhuma observação registrada.\n\n")
        }
        
        texto.append("--------------------------------\n")
        texto.append("✅ Acerto realizado via GestaoBilhares")
        return texto.toString()
    }

    /**
     * Preenche o layout de recibo com os dados fornecidos (versão compatível com AcertoMesa)
     * @param context Contexto da aplicação
     * @param reciboView View do layout de recibo já inflado
     * @param clienteNome Nome do cliente
     * @param mesas Lista de mesas do acerto
     * @param debitoAnterior Débito anterior do cliente
     * @param valorTotalMesas Valor total das mesas
     * @param desconto Desconto aplicado
     * @param metodosPagamento Métodos de pagamento utilizados
     * @param debitoAtual Débito atual após o acerto
     * @param observacao Observações do acerto
     * @param valorFicha Valor da ficha do cliente
     * @param acertoId ID do acerto (opcional, para títulos)
     */
    @Deprecated("Use preencherReciboImpressaoCompleto/gerarTextoWhatsApp com mesas completas para manter a fonte única de verdade e a numeração real das mesas")
    fun preencherReciboImpressao(
        context: Context,
        reciboView: View,
        clienteNome: String,
        mesas: List<AcertoMesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long? = null
    ) {
        // Referências dos elementos
        val txtTitulo = reciboView.findViewById<android.widget.TextView>(R.id.txtTituloRecibo)
        val txtClienteValor = reciboView.findViewById<android.widget.TextView>(R.id.txtClienteValor)
        val txtData = reciboView.findViewById<android.widget.TextView>(R.id.txtData)
        val rowValorFicha = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowValorFicha)
        val txtValorFicha = reciboView.findViewById<android.widget.TextView>(R.id.txtValorFicha)
        val txtMesas = reciboView.findViewById<android.widget.TextView>(R.id.txtMesas)
        val txtFichasJogadas = reciboView.findViewById<android.widget.TextView>(R.id.txtFichasJogadas)
        val txtDebitoAnterior = reciboView.findViewById<android.widget.TextView>(R.id.txtDebitoAnterior)
        val txtSubtotalMesas = reciboView.findViewById<android.widget.TextView>(R.id.txtSubtotalMesas)
        val txtTotal = reciboView.findViewById<android.widget.TextView>(R.id.txtTotal)
        val txtDesconto = reciboView.findViewById<android.widget.TextView>(R.id.txtDesconto)
        val txtValorRecebido = reciboView.findViewById<android.widget.TextView>(R.id.txtValorRecebido)
        val txtDebitoAtual = reciboView.findViewById<android.widget.TextView>(R.id.txtDebitoAtual)
        val txtPagamentos = reciboView.findViewById<android.widget.TextView>(R.id.txtPagamentos)
        val txtObservacoes = reciboView.findViewById<android.widget.TextView>(R.id.txtObservacoes)

        // Formatação
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // Título
        val titulo = if (acertoId != null) {
            "RECIBO DE ACERTO #${acertoId.toString().padStart(4, '0')}"
        } else {
            "RECIBO DE ACERTO"
        }
        txtTitulo.text = titulo

        // Cliente e data
        txtClienteValor.text = clienteNome
        txtData.text = "Data: $dataFormatada"

        // Valor da ficha
        if (valorFicha > 0) {
            txtValorFicha.text = formatter.format(valorFicha)
            rowValorFicha.visibility = View.VISIBLE
        } else {
            rowValorFicha.visibility = View.GONE
        }

        // Mesas (formatação limpa sem quebras extras)
        val mesasFormatadas = StringBuilder()
        mesas.forEachIndexed { index, mesa ->
            val fichasJogadas = mesa.fichasJogadas
            // ✅ CORREÇÃO: Usar número real da mesa, não mesaId
            mesasFormatadas.append("Mesa ${index + 1}\n${mesa.relogioInicial} → ${mesa.relogioFinal} (${fichasJogadas} fichas)")
            if (index < mesas.size - 1) mesasFormatadas.append("\n")
        }
        txtMesas.text = mesasFormatadas.toString()

        // Fichas jogadas
        val totalFichasJogadas = mesas.sumOf { it.fichasJogadas }
        txtFichasJogadas.text = totalFichasJogadas.toString()

        // Resumo Financeiro (sem duplicação e com rótulos únicos)
        txtDebitoAnterior.text = formatter.format(debitoAnterior)
        txtSubtotalMesas.text = formatter.format(valorTotalMesas)
        val valorTotal = valorTotalMesas + debitoAnterior
        txtTotal.text = formatter.format(valorTotal)
        txtDesconto.text = formatter.format(desconto)
        val valorRecebidoSum = metodosPagamento.values.sum()
        txtValorRecebido.text = formatter.format(valorRecebidoSum)
        txtDebitoAtual.text = formatter.format(debitoAtual)

        // Forma de pagamento (formatação limpa)
        val pagamentosFormatados = if (metodosPagamento.isNotEmpty()) {
            metodosPagamento.entries.joinToString("\n") { "${it.key}: ${formatter.format(it.value)}" }
        } else {
            "Não informado"
        }
        txtPagamentos.text = pagamentosFormatados

        // Observações - SEMPRE exibir
        if (observacao.isNullOrBlank()) {
            txtObservacoes.text = "Nenhuma observação registrada."
        } else {
            txtObservacoes.text = observacao
        }

        // Logo
        val imgLogo = reciboView.findViewById<android.widget.ImageView>(R.id.imgLogoRecibo)
        imgLogo.setImageResource(R.drawable.logo_globo1)

        // Ajustar estilos para títulos e valores principais
        txtTitulo.setTypeface(null, Typeface.BOLD)
        txtClienteValor.setTypeface(null, Typeface.BOLD)
        txtMesas.setTypeface(null, Typeface.BOLD)
        txtPagamentos.setTypeface(null, Typeface.BOLD)
        txtObservacoes.setTypeface(null, Typeface.BOLD)
    }

    /**
     * ✅ NOVA FUNÇÃO CENTRALIZADA: Imprime recibo com dados unificados
     * FONTE ÚNICA DE VERDADE para impressão - elimina duplicação de código
     */
    fun imprimirReciboUnificado(
        context: Context,
        clienteNome: String,
        clienteCpf: String? = null,
        clienteTelefone: String? = null,
        mesasCompletas: List<Mesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long? = null,
        numeroContrato: String? = null,
        onSucesso: () -> Unit = {},
        onErro: (String) -> Unit = {}
    ) {
        try {
            // Verificar permissões Bluetooth
            val bluetoothPermissions = arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
            
            val hasPermissions = bluetoothPermissions.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            
            if (!hasPermissions) {
                // ✅ NOVO: Solicitar permissões automaticamente
                if (context is androidx.fragment.app.FragmentActivity) {
                    solicitarPermissoesBluetooth(context, bluetoothPermissions, onSucesso, onErro)
                } else {
                    onErro("Permissões Bluetooth necessárias para impressão. Vá em Configurações > Aplicativos > Gestão Bilhares > Permissões e ative o Bluetooth.")
                }
                return
            }
            
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                onErro("Bluetooth não disponível neste dispositivo")
                return
            }
            
            if (!bluetoothAdapter.isEnabled) {
                onErro("Ative o Bluetooth para imprimir")
                return
            }
            
            val pairedDevices = bluetoothAdapter.bondedDevices
            if (pairedDevices.isEmpty()) {
                onErro("Nenhuma impressora Bluetooth pareada")
                return
            }
            
            // Diálogo de seleção de impressora
            val deviceList = pairedDevices.toList()
            val deviceNames = deviceList.map { it.name ?: it.address }.toTypedArray()
            
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Selecione a impressora")
                .setItems(deviceNames) { _, which ->
                    val printerDevice = deviceList[which]
                    imprimirComImpressoraSelecionada(
                        context = context,
                        printerDevice = printerDevice,
                        clienteNome = clienteNome,
                        clienteCpf = clienteCpf,
                        mesasCompletas = mesasCompletas,
                        debitoAnterior = debitoAnterior,
                        valorTotalMesas = valorTotalMesas,
                        desconto = desconto,
                        metodosPagamento = metodosPagamento,
                        debitoAtual = debitoAtual,
                        observacao = observacao,
                        valorFicha = valorFicha,
                        acertoId = acertoId,
                        numeroContrato = numeroContrato,
                        onSucesso = onSucesso,
                        onErro = onErro
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            onErro("Erro ao preparar impressão: ${e.message}")
        }
    }
    
    /**
     * ✅ NOVA FUNÇÃO CENTRALIZADA: Imprime com impressora selecionada
     */
    private fun imprimirComImpressoraSelecionada(
        context: Context,
        printerDevice: android.bluetooth.BluetoothDevice,
        clienteNome: String,
        clienteCpf: String?,
        mesasCompletas: List<Mesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long?,
        numeroContrato: String?,
        onSucesso: () -> Unit,
        onErro: (String) -> Unit
    ) {
        // Mostrar diálogo de loading
        val loadingDialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setMessage("Imprimindo recibo...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // Executar impressão em thread separada
        Thread {
            var erro: String? = null
            try {
                val printerHelper = com.example.gestaobilhares.ui.settlement.BluetoothPrinterHelper(printerDevice)
                if (printerHelper.connect()) {
                    // Inflar o layout do recibo
                    val inflater = android.view.LayoutInflater.from(context)
                    val reciboView = inflater.inflate(com.example.gestaobilhares.R.layout.layout_recibo_impressao, null) as android.view.ViewGroup
                    
                    // Preencher campos do recibo usando função centralizada
                    preencherReciboImpressaoCompleto(
                        context = context,
                        reciboView = reciboView,
                        clienteNome = clienteNome,
                        clienteCpf = clienteCpf,
                        mesasCompletas = mesasCompletas,
                        debitoAnterior = debitoAnterior,
                        valorTotalMesas = valorTotalMesas,
                        desconto = desconto,
                        metodosPagamento = metodosPagamento,
                        debitoAtual = debitoAtual,
                        observacao = observacao,
                        valorFicha = valorFicha,
                        acertoId = acertoId,
                        numeroContrato = numeroContrato
                    )
                    
                    // Imprimir
                    printerHelper.printReciboLayoutBitmap(reciboView)
                    printerHelper.disconnect()
                } else {
                    erro = "Falha ao conectar à impressora"
                }
            } catch (e: Exception) {
                erro = when {
                    e.message?.contains("socket") == true -> "Impressora desligada ou fora de alcance"
                    e.message?.contains("broken pipe") == true -> "Falha ao enviar dados. Impressora pode estar desconectada"
                    else -> "Erro inesperado: ${e.message ?: "Desconhecido"}"
                }
            }
            
            // Atualizar UI na thread principal
            if (context is android.app.Activity) {
                context.runOnUiThread {
                    loadingDialog.dismiss()
                    if (erro == null) {
                        onSucesso()
                    } else {
                        onErro(erro)
                    }
                }
            }
        }.start()
    }
    
    /**
     * ✅ NOVA FUNÇÃO CENTRALIZADA: Envia via WhatsApp com dados unificados
     * FONTE ÚNICA DE VERDADE para WhatsApp - elimina duplicação de código
     */
    fun enviarWhatsAppUnificado(
        context: Context,
        clienteNome: String,
        clienteCpf: String? = null,
        clienteTelefone: String?,
        mesasCompletas: List<Mesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long? = null,
        numeroContrato: String? = null,
        onSucesso: () -> Unit = {},
        onErro: (String) -> Unit = {}
    ) {
        if (clienteTelefone.isNullOrEmpty()) {
            onErro("Cliente não possui telefone cadastrado")
            return
        }
        
        try {
            // Gerar texto usando função centralizada
            val textoCompleto = gerarTextoWhatsApp(
                clienteNome = clienteNome,
                clienteCpf = clienteCpf,
                mesasCompletas = mesasCompletas,
                debitoAnterior = debitoAnterior,
                valorTotalMesas = valorTotalMesas,
                desconto = desconto,
                metodosPagamento = metodosPagamento,
                debitoAtual = debitoAtual,
                observacao = observacao,
                valorFicha = valorFicha,
                acertoId = acertoId,
                numeroContrato = numeroContrato
            )
            
            // Enviar via WhatsApp
            enviarViaWhatsAppDireto(context, clienteTelefone, textoCompleto, onSucesso, onErro)
            
        } catch (e: Exception) {
            onErro("Erro ao compartilhar via WhatsApp: ${e.message}")
        }
    }
    
    /**
     * ✅ NOVA FUNÇÃO CENTRALIZADA: Envia via WhatsApp direto
     */
    private fun enviarViaWhatsAppDireto(
        context: Context,
        telefone: String,
        texto: String,
        onSucesso: () -> Unit,
        onErro: (String) -> Unit
    ) {
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
            
            // ✅ ESTRATÉGIA 1: Esquema nativo whatsapp://send (FORÇA direcionamento direto)
            try {
                val uri = android.net.Uri.parse("whatsapp://send?phone=$numeroCompleto&text=${android.net.Uri.encode(texto)}")
                val intentWhatsApp = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                    // ✅ CRÍTICO: Força o direcionamento direto sem seletor
                    setPackage("com.whatsapp")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                context.startActivity(intentWhatsApp)
                onSucesso()
                return
            } catch (e: Exception) {
                // Estratégia 1 falhou, tentar próxima
            }
            
            // ✅ ESTRATÉGIA 2: URL wa.me (funciona mesmo sem app instalado)
            try {
                val url = "https://wa.me/$numeroCompleto?text=${android.net.Uri.encode(texto)}"
                val intentUrl = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(url)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                context.startActivity(intentUrl)
                onSucesso()
                return
            } catch (e: Exception) {
                // Estratégia 2 falhou, tentar próxima
            }
            
            // ✅ ESTRATÉGIA 3: Tentar WhatsApp Business via esquema nativo
            try {
                val uri = android.net.Uri.parse("whatsapp://send?phone=$numeroCompleto&text=${android.net.Uri.encode(texto)}")
                val intentBusiness = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                context.startActivity(intentBusiness)
                onSucesso()
                return
            } catch (e: Exception) {
                // Estratégia 3 falhou, tentar próxima
            }
            
            // ✅ ESTRATÉGIA 4: Intent direto com ACTION_SEND mas SEM chooser
            try {
                val intentDirect = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, texto)
                    setPackage("com.whatsapp")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                context.startActivity(intentDirect)
                onSucesso()
                return
            } catch (e: Exception) {
                // Todas as estratégias falharam
            }
            
            // ✅ ÚLTIMA OPÇÃO: Mostrar mensagem de erro
            onErro("Não foi possível abrir o WhatsApp. Verifique se está instalado.")
            
        } catch (e: Exception) {
            onErro("Erro ao abrir WhatsApp: ${e.message}")
        }
    }

    /**
     * Retorna o nome do tipo do equipamento para exibição
     */
    private fun getTipoEquipamentoNome(tipoMesa: TipoMesa): String {
        return when (tipoMesa) {
            TipoMesa.SINUCA -> "Sinuca"
            TipoMesa.PEMBOLIM -> "Pembolim"
            TipoMesa.JUKEBOX -> "Jukebox"
            TipoMesa.OUTROS -> "Equipamento"
        }
    }

    /**
     * ✅ NOVO: Solicita permissões Bluetooth automaticamente
     */
    private fun solicitarPermissoesBluetooth(
        activity: androidx.fragment.app.FragmentActivity,
        permissions: Array<String>,
        onSucesso: () -> Unit,
        onErro: (String) -> Unit
    ) {
        // Verificar se já temos permissões
        val hasPermissions = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(activity, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (hasPermissions) {
            onSucesso()
            return
        }
        
        // Mostrar diálogo explicativo
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("🔗 Permissões Bluetooth Necessárias")
            .setMessage("O app precisa de permissões Bluetooth para imprimir recibos na impressora térmica. Clique em 'Permitir' para continuar.")
            .setPositiveButton("Permitir") { _, _ ->
                // Solicitar permissões
                androidx.core.app.ActivityCompat.requestPermissions(
                    activity,
                    permissions,
                    1001 // REQUEST_BLUETOOTH_PERMISSIONS
                )
            }
            .setNegativeButton("Cancelar") { _, _ ->
                onErro("Permissões Bluetooth necessárias para impressão")
            }
            .setCancelable(false)
            .show()
    }
}
