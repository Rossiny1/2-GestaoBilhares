package com.example.gestaobilhares.core.utils

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import timber.log.Timber
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
        @Suppress("UNUSED_PARAMETER") context: Context,
        @Suppress("UNUSED_PARAMETER") reciboView: View,
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
        // ✅ RESTAURADO: Usar getIdentifier para acessar recursos do layout
        val resources = context.resources
        val packageName = context.packageName
        
        // Função auxiliar para obter ID de recurso
        fun getResId(name: String, type: String): Int {
            return resources.getIdentifier(name, type, packageName)
        }
        
        // Referências dos elementos usando getIdentifier
        val txtTitulo = reciboView.findViewById<android.widget.TextView>(getResId("txtTituloRecibo", "id"))
        val txtClienteValor = reciboView.findViewById<android.widget.TextView>(getResId("txtClienteValor", "id"))
        val rowCpfCliente = reciboView.findViewById<android.widget.LinearLayout>(getResId("rowCpfCliente", "id"))
        val txtCpfCliente = reciboView.findViewById<android.widget.TextView>(getResId("txtCpfCliente", "id"))
        val rowNumeroContrato = reciboView.findViewById<android.widget.LinearLayout>(getResId("rowNumeroContrato", "id"))
        val txtNumeroContrato = reciboView.findViewById<android.widget.TextView>(getResId("txtNumeroContrato", "id"))
        val txtData = reciboView.findViewById<android.widget.TextView>(getResId("txtData", "id"))
        val rowValorFicha = reciboView.findViewById<android.widget.LinearLayout>(getResId("rowValorFicha", "id"))
        val txtValorFicha = reciboView.findViewById<android.widget.TextView>(getResId("txtValorFicha", "id"))
        val txtMesas = reciboView.findViewById<android.widget.TextView>(getResId("txtMesas", "id"))
        val txtFichasJogadas = reciboView.findViewById<android.widget.TextView>(getResId("txtFichasJogadas", "id"))
        val txtDebitoAnterior = reciboView.findViewById<android.widget.TextView>(getResId("txtDebitoAnterior", "id"))
        val txtSubtotalMesas = reciboView.findViewById<android.widget.TextView>(getResId("txtSubtotalMesas", "id"))
        val txtTotal = reciboView.findViewById<android.widget.TextView>(getResId("txtTotal", "id"))
        val txtDesconto = reciboView.findViewById<android.widget.TextView>(getResId("txtDesconto", "id"))
        val txtValorRecebido = reciboView.findViewById<android.widget.TextView>(getResId("txtValorRecebido", "id"))
        val txtDebitoAtual = reciboView.findViewById<android.widget.TextView>(getResId("txtDebitoAtual", "id"))
        val txtPagamentos = reciboView.findViewById<android.widget.TextView>(getResId("txtPagamentos", "id"))
        val txtObservacoes = reciboView.findViewById<android.widget.TextView>(getResId("txtObservacoes", "id"))
        val rowNumeroRecibo = reciboView.findViewById<android.widget.LinearLayout>(getResId("rowNumeroRecibo", "id"))
        val txtNumeroRecibo = reciboView.findViewById<android.widget.TextView>(getResId("txtNumeroRecibo", "id"))
        val imgLogo = reciboView.findViewById<android.widget.ImageView>(getResId("imgLogoRecibo", "id"))
        val txtAgradecimento = reciboView.findViewById<android.widget.TextView>(getResId("txtAgradecimento", "id"))

        // Formatação
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // Título - SEMPRE sem número (com emoji como no layout original)
        txtTitulo?.text = "📋 RECIBO DE ACERTO"

        // ✅ CORREÇÃO: Cliente - SEMPRE exibir (mesma lógica da data)
        txtClienteValor?.text = clienteNome
        
        // CPF do cliente
        if (!clienteCpf.isNullOrBlank()) {
            txtCpfCliente?.text = clienteCpf
            rowCpfCliente?.visibility = View.VISIBLE
        } else {
            rowCpfCliente?.visibility = View.GONE
        }
        
        // ✅ CORREÇÃO: Número do recibo - MANTER visível embaixo dos clientes
        txtNumeroRecibo?.text = acertoId?.toString() ?: "N/A"
        rowNumeroRecibo?.visibility = View.VISIBLE
        
        // ✅ CORREÇÃO: Número do contrato - SEMPRE exibir (mesma lógica da data)
        txtNumeroContrato?.text = numeroContrato ?: "N/A"
        rowNumeroContrato?.visibility = View.VISIBLE
        
        // Data (apenas o valor, o rótulo já existe no layout)
        txtData?.text = dataFormatada

        // Valor da ficha - SEMPRE exibir
        txtValorFicha?.text = formatter.format(valorFicha)
        rowValorFicha?.visibility = View.VISIBLE

        // Mesas (formatação com tipo do equipamento e número real da mesa - igual ao layout original)
        val mesasFormatadas = StringBuilder()
        mesasCompletas.forEachIndexed { index, mesa ->
            val fichasJogadas = mesa.relogioFinal - mesa.relogioInicial
            val tipoEquipamento = getTipoEquipamentoNome(mesa.tipoMesa)
            // ✅ CORREÇÃO: Formatação igual ao layout original - nome da mesa em uma linha, relógios na linha de baixo
            mesasFormatadas.append("$tipoEquipamento ${mesa.numero}\n${mesa.relogioInicial} → ${mesa.relogioFinal} (${fichasJogadas} fichas)")
            if (index < mesasCompletas.size - 1) mesasFormatadas.append("\n")
        }
        txtMesas?.text = mesasFormatadas.toString()

        // Fichas jogadas
        val totalFichasJogadas = mesasCompletas.sumOf { it.relogioFinal - it.relogioInicial }
        txtFichasJogadas?.text = totalFichasJogadas.toString()

        // Resumo Financeiro (sem duplicação e com rótulos únicos)
        txtDebitoAnterior?.text = formatter.format(debitoAnterior)
        txtSubtotalMesas?.text = formatter.format(valorTotalMesas)
        val valorTotal = valorTotalMesas + debitoAnterior
        txtTotal?.text = formatter.format(valorTotal)
        txtDesconto?.text = formatter.format(desconto)
        val valorRecebidoSum = metodosPagamento.values.sum()
        txtValorRecebido?.text = formatter.format(valorRecebidoSum)
        txtDebitoAtual?.text = formatter.format(debitoAtual)

        // Forma de pagamento (formatação limpa)
        val pagamentosFormatados = if (metodosPagamento.isNotEmpty()) {
            metodosPagamento.entries.joinToString("\n") { "${it.key}: ${formatter.format(it.value)}" }
        } else {
            "Não informado"
        }
        txtPagamentos?.text = pagamentosFormatados

        // Observações - SEMPRE exibir (sem delay)
        if (observacao.isNullOrBlank()) {
            txtObservacoes?.text = "Nenhuma observação registrada."
        } else {
            txtObservacoes?.text = observacao
        }

        // ✅ CORREÇÃO: Agradecimento no final do recibo
        txtAgradecimento?.text = "Acerto realizado via\nGestaoBilhares\nObrigado por confiar!"

        // Logo - carregar do recurso
        val logoId = getResId("logo_globo1", "drawable")
        if (logoId != 0) {
            imgLogo?.setImageResource(logoId)
        }

        // Ajustar estilos para títulos e valores principais
        txtTitulo?.setTypeface(null, Typeface.BOLD)
        txtClienteValor?.setTypeface(null, Typeface.BOLD)
        txtMesas?.setTypeface(null, Typeface.BOLD)
        txtPagamentos?.setTypeface(null, Typeface.BOLD)
        txtObservacoes?.setTypeface(null, Typeface.BOLD)
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
        @Suppress("UNUSED_PARAMETER") acertoId: Long? = null,
        numeroContrato: String? = null
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataAtual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val texto = StringBuilder()
        
        // ✅ TÍTULO (mesmo do recibo impresso - sem número)
        texto.append("🎱 *RECIBO DE ACERTO*\n")
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
            val fichasJogadas = mesa.relogioFinal - mesa.relogioInicial
            totalFichasJogadas += fichasJogadas
            val tipoEquipamento = getTipoEquipamentoNome(mesa.tipoMesa)
            // ✅ CORREÇÃO: Formatação igual ao impresso - nome da mesa em uma linha, relógios na linha de baixo
            texto.append("• *$tipoEquipamento ${mesa.numero}*\n")
            texto.append("  ${mesa.relogioInicial} → ${mesa.relogioFinal} (${fichasJogadas} fichas)\n")
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
     * ✅ NOVA FUNÇÃO CENTRALIZADA: Imprime recibo com dados unificados
     * FONTE ÚNICA DE VERDADE para impressão - elimina duplicação de código
     * 
     * @param context Context ou Fragment - se for Fragment, será usado para solicitar permissões
     */
    fun imprimirReciboUnificado(
        context: Any, // ✅ CORREÇÃO: Aceita Context ou Fragment
        clienteNome: String,
        clienteCpf: String? = null,
        @Suppress("UNUSED_PARAMETER") clienteTelefone: String? = null,
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
            // ✅ CORREÇÃO: Obter Context real e Fragment (se disponível)
            val realContext = when (context) {
                is Context -> context
                is androidx.fragment.app.Fragment -> context.requireContext()
                else -> throw IllegalArgumentException("Context deve ser Context ou Fragment")
            }
            
            val fragment = when (context) {
                is androidx.fragment.app.Fragment -> context
                else -> null
            }
            
            // Verificar permissões Bluetooth
            val bluetoothPermissions = arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
            
            val hasPermissions = bluetoothPermissions.all {
                androidx.core.content.ContextCompat.checkSelfPermission(realContext, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            
            if (!hasPermissions) {
                // ✅ CORREÇÃO: Solicitar permissões automaticamente (suporta Activity e Fragment)
                val activity = when {
                    realContext is androidx.fragment.app.FragmentActivity -> realContext
                    fragment != null -> fragment.activity as? androidx.fragment.app.FragmentActivity
                    else -> null
                }
                
                if (activity != null && fragment != null) {
                    // ✅ NOVO: Para Fragment, usar método que armazena callback no Fragment
                    solicitarPermissoesBluetoothComRetryFragment(
                        fragment = fragment,
                        activity = activity,
                        permissions = bluetoothPermissions,
                        onPermissoesConcedidas = {
                            // Tentar imprimir novamente após permissões concedidas
                            imprimirReciboUnificado(
                                context = context,
                                clienteNome = clienteNome,
                                clienteCpf = clienteCpf,
                                clienteTelefone = null,
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
                        },
                        onErro = onErro
                    )
                } else if (activity != null) {
                    // Para Activity apenas
                    solicitarPermissoesBluetoothComRetry(
                        activity = activity,
                        fragment = null,
                        permissions = bluetoothPermissions,
                        onPermissoesConcedidas = {
                            imprimirReciboUnificado(
                                context = context,
                                clienteNome = clienteNome,
                                clienteCpf = clienteCpf,
                                clienteTelefone = null,
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
                        },
                        onErro = onErro
                    )
                } else {
                    onErro("Permissões Bluetooth necessárias para impressão. Vá em Configurações > Aplicativos > Gestão Bilhares > Permissões e ative o Bluetooth.")
                }
                return
            }
            
            // ✅ CORREÇÃO: getDefaultAdapter deprecated - usar BluetoothManager
            @Suppress("DEPRECATION")
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
            
            androidx.appcompat.app.AlertDialog.Builder(realContext)
                .setTitle("Selecione a impressora")
                .setItems(deviceNames) { _, which ->
                    val printerDevice = deviceList[which]
                    imprimirComImpressoraSelecionada(
                        context = realContext,
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
        // ✅ CORREÇÃO CRÍTICA: Usar Handler para garantir que o dialog seja sempre fechado
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var loadingDialog: androidx.appcompat.app.AlertDialog? = null
        
        // Mostrar diálogo de loading na thread principal
        handler.post {
            loadingDialog = androidx.appcompat.app.AlertDialog.Builder(context)
                .setMessage("Imprimindo recibo...")
                .setCancelable(true) // ✅ Permitir cancelar para evitar travamento
                .setOnCancelListener {
                    // Se o usuário cancelar, tratar como erro
                    onErro("Impressão cancelada pelo usuário")
                }
                .create()
            loadingDialog?.show()
        }
        
        // ✅ CORREÇÃO: Timeout para evitar travamento indefinido (30 segundos)
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            handler.post {
                loadingDialog?.dismiss()
                onErro("Timeout: A impressão está demorando muito. Verifique se a impressora está conectada e funcionando.")
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 30000) // 30 segundos
        
        // Executar impressão em thread separada
        Thread {
            var erro: String? = null
            var sucesso = false
            var printerHelper: BluetoothPrinterHelper? = null
            try {
                // ✅ CRÍTICO: Criar nova instância e conectar para cada impressão
                // Isso garante que não há dados residuais de impressões anteriores
                printerHelper = BluetoothPrinterHelper(printerDevice)
                
                // ✅ CRÍTICO: Garantir que não há conexão anterior ativa
                // Se houver, desconectar primeiro
                try {
                    printerHelper.disconnect()
                    Thread.sleep(300) // Aguardar desconexão completa
                } catch (e: Exception) {
                    // Ignorar erro se não havia conexão
                }
                
                // Conectar à impressora
                if (printerHelper.connect()) {
                    // ✅ CRÍTICO: Aguardar estabilização da conexão
                    Thread.sleep(200)
                    // ✅ RESTAURADO: Abordagem original - inflar layout XML e converter em bitmap
                    try {
                        // Tentar inflar o layout do recibo
                        val resources = context.resources
                        val layoutId = resources.getIdentifier("layout_recibo_impressao", "layout", context.packageName)
                        
                        if (layoutId != 0) {
                            // Inflar o layout
                            val inflater = android.view.LayoutInflater.from(context)
                            val reciboView = inflater.inflate(layoutId, null) as? android.view.ViewGroup
                            
                            if (reciboView != null) {
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
                                
                                // Imprimir usando a abordagem original (View -> Bitmap)
                                val impressaoOk = printerHelper.printReciboLayoutBitmap(reciboView)
                                
                                // ✅ CRÍTICO: Aguardar um pouco antes de desconectar
                                // Isso garante que a impressora terminou de processar
                                Thread.sleep(500)
                                
                                // Desconectar
                                printerHelper.disconnect()
                                
                                if (impressaoOk) {
                                    sucesso = true
                                } else {
                                    erro = "Falha ao enviar dados para impressora"
                                }
                            } else {
                                erro = "Erro ao inflar layout do recibo"
                            }
                        } else {
                            // Fallback: usar impressão direta com comandos ESC/POS
                            Timber.w("ReciboPrinterHelper", "Layout não encontrado, usando impressão direta")
                            
                            // Carregar logo do recibo
                            var logoBitmap: android.graphics.Bitmap? = null
                            try {
                                val logoId = resources.getIdentifier("logo_globo1", "drawable", context.packageName)
                                if (logoId != 0) {
                                    logoBitmap = android.graphics.BitmapFactory.decodeResource(resources, logoId)
                                }
                            } catch (e: Exception) {
                                Timber.w("ReciboPrinterHelper", "Não foi possível carregar o logo: ${e.message}")
                            }
                            
                            // Imprimir recibo usando comandos ESC/POS diretos
                            val impressaoOk = printerHelper.printRecibo(
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
                                logoBitmap = logoBitmap
                            )
                            
                            // Desconectar
                            printerHelper.disconnect()
                            
                            if (impressaoOk) {
                                sucesso = true
                            } else {
                                erro = "Falha ao enviar dados para impressora"
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Erro ao imprimir layout: %s", e.message)
                        erro = "Erro ao processar layout do recibo: ${e.message}"
                        printerHelper.disconnect()
                    }
                } else {
                    erro = "Falha ao conectar à impressora"
                }
            } catch (e: Exception) {
                erro = when {
                    e.message?.contains("socket", ignoreCase = true) == true -> "Impressora desligada ou fora de alcance"
                    e.message?.contains("broken pipe", ignoreCase = true) == true -> "Falha ao enviar dados. Impressora pode estar desconectada"
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Timeout ao conectar. Verifique se a impressora está ligada e pareada"
                    e.message?.contains("permission", ignoreCase = true) == true -> "Permissões Bluetooth necessárias"
                    else -> "Erro inesperado: ${e.message ?: "Desconhecido"}"
                }
                Timber.e(e, "Erro na impressão")
            } finally {
                // ✅ CRÍTICO: Sempre desconectar e limpar recursos, mesmo em caso de erro
                try {
                    printerHelper?.disconnect()
                } catch (e: Exception) {
                    Timber.w("ReciboPrinterHelper", "Erro ao desconectar impressora: ${e.message}")
                }
                
                // ✅ CRÍTICO: Cancelar timeout se a operação terminou antes
                timeoutHandler.removeCallbacks(timeoutRunnable)
                
                // ✅ CRÍTICO: Sempre fechar o dialog e atualizar UI na thread principal
                handler.post {
                    try {
                        loadingDialog?.dismiss()
                    } catch (e: Exception) {
                        Timber.w("ReciboPrinterHelper", "Erro ao fechar dialog: ${e.message}")
                    }
                    
                    if (sucesso) {
                        onSucesso()
                    } else {
                        onErro(erro ?: "Erro desconhecido na impressão")
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
     * ✅ CORREÇÃO: Solicita permissões Bluetooth para Fragment (usa callback do Fragment)
     */
    private fun solicitarPermissoesBluetoothComRetryFragment(
        fragment: androidx.fragment.app.Fragment,
        activity: androidx.fragment.app.FragmentActivity,
        permissions: Array<String>,
        onPermissoesConcedidas: () -> Unit,
        onErro: (String) -> Unit
    ) {
        // Verificar se já temos permissões
        val hasPermissions = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(activity, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (hasPermissions) {
            onPermissoesConcedidas()
            return
        }
        
        // ✅ CORREÇÃO: Tentar usar método do Fragment se disponível (SettlementDetailFragment)
        try {
            val setCallbackMethod = fragment.javaClass.getMethod("setPendingPrintCallback", kotlin.jvm.functions.Function0::class.java)
            setCallbackMethod.invoke(fragment, onPermissoesConcedidas)
            
            // Mostrar diálogo explicativo
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("🔗 Permissões Bluetooth Necessárias")
                .setMessage("O app precisa de permissões Bluetooth para imprimir recibos na impressora térmica. Clique em 'Permitir' para continuar.")
                .setPositiveButton("Permitir") { _, _ ->
                    // Solicitar permissões usando método do Fragment
                    @Suppress("DEPRECATION")
                    fragment.requestPermissions(permissions, 1001)
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    onErro("Permissões Bluetooth necessárias para impressão")
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            // Fallback: usar método genérico
            Timber.w("ReciboPrinterHelper", "Fragment não suporta setPendingPrintCallback, usando fallback")
            solicitarPermissoesBluetoothComRetry(activity, fragment, permissions, onPermissoesConcedidas, onErro)
        }
    }
    
    /**
     * ✅ CORREÇÃO: Solicita permissões Bluetooth automaticamente com suporte a Fragment
     * Após permissões concedidas, chama callback para tentar novamente
     */
    private fun solicitarPermissoesBluetoothComRetry(
        activity: androidx.fragment.app.FragmentActivity,
        fragment: androidx.fragment.app.Fragment?,
        permissions: Array<String>,
        onPermissoesConcedidas: () -> Unit,
        onErro: (String) -> Unit
    ) {
        // Verificar se já temos permissões
        val hasPermissions = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(activity, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (hasPermissions) {
            onPermissoesConcedidas()
            return
        }
        
        // ✅ CORREÇÃO: Usar método que funciona tanto para Activity quanto Fragment
        // Mostrar diálogo explicativo primeiro
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("🔗 Permissões Bluetooth Necessárias")
            .setMessage("O app precisa de permissões Bluetooth para imprimir recibos na impressora térmica. Clique em 'Permitir' para continuar.")
            .setPositiveButton("Permitir") { _, _ ->
                // Solicitar permissões usando método compatível
                if (fragment != null) {
                    // ✅ CORREÇÃO: Para Fragment, usar requestPermissions diretamente
                    @Suppress("DEPRECATION")
                    fragment.requestPermissions(permissions, 1001)
                    
                    // ✅ CORREÇÃO: Verificar resultado após um delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val granted = permissions.all {
                            androidx.core.content.ContextCompat.checkSelfPermission(activity, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                        if (granted) {
                            onPermissoesConcedidas()
                        } else {
                            onErro("Permissões Bluetooth necessárias para impressão. Vá em Configurações > Aplicativos > Gestão Bilhares > Permissões e ative o Bluetooth.")
                        }
                    }, 500)
                } else {
                    // Para Activity, usar ActivityCompat
                    androidx.core.app.ActivityCompat.requestPermissions(
                        activity,
                        permissions,
                        1001 // REQUEST_BLUETOOTH_PERMISSIONS
                    )
                    // ✅ CORREÇÃO: Verificar resultado após um delay (Activity precisa processar)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val granted = permissions.all {
                            androidx.core.content.ContextCompat.checkSelfPermission(activity, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                        if (granted) {
                            onPermissoesConcedidas()
                        } else {
                            onErro("Permissões Bluetooth necessárias para impressão. Vá em Configurações > Aplicativos > Gestão Bilhares > Permissões e ative o Bluetooth.")
                        }
                    }, 500)
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                onErro("Permissões Bluetooth necessárias para impressão")
            }
            .setCancelable(false)
            .show()
    }
}
