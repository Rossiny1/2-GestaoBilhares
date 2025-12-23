package com.example.gestaobilhares.core.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import java.io.IOException
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Helper para conexão e impressão em impressoras térmicas via Bluetooth
 * Suporta comandos ESC/POS padrão
 */
class BluetoothPrinterHelper(private val device: BluetoothDevice) {
    
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val TAG = "BluetoothPrinter"
    
    /**
     * Conecta à impressora Bluetooth
     * @return true se conectou com sucesso, false caso contrário
     */
    fun connect(): Boolean {
        // Método 1: Tentar com UUID padrão SPP
        val conectado = try {
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            outputStream = socket?.outputStream
            
            if (socket?.isConnected == true && outputStream != null) {
                Timber.tag(TAG).d("Conectado à impressora: ${device.name}")
                true
            } else {
                false
            }
        } catch (e: IOException) {
            Timber.tag(TAG).w("Método padrão falhou, tentando método alternativo: ${e.message}")
            disconnect()
            false
        } catch (e: Exception) {
            Timber.tag(TAG).w("Erro no método padrão: ${e.message}")
            disconnect()
            false
        }
        
        // Se já conectou, retornar
        if (conectado) {
            return true
        }
        
        // Método 2: Tentar conexão insegura (fallback para algumas impressoras)
        return try {
            val method = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", UUID::class.java)
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            socket = method.invoke(device, uuid) as? BluetoothSocket
            socket?.connect()
            outputStream = socket?.outputStream
            
            if (socket?.isConnected == true && outputStream != null) {
                Timber.tag(TAG).d( "Conectado à impressora (método inseguro): ${device.name}")
                true
            } else {
                Timber.tag(TAG).e( "Falha ao conectar mesmo com método alternativo")
                disconnect()
                false
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao conectar: ${e.message}")
            disconnect()
            false
        }
    }
    
    /**
     * Desconecta da impressora
     * ✅ CORREÇÃO: Garante limpeza completa do buffer e conexão
     */
    fun disconnect() {
        try {
            // Tentar enviar comando de reset antes de desconectar (se ainda conectado)
            if (isConnected()) {
                try {
                    // Reset da impressora para limpar buffer
                    outputStream?.write(byteArrayOf(0x1B, 0x40))
                    outputStream?.flush()
                    Thread.sleep(100) // Pequeno delay para garantir que o comando foi processado
                } catch (e: Exception) {
                    Timber.tag(TAG).w( "Erro ao resetar antes de desconectar: ${e.message}")
                }
            }
            
            // Fechar streams e socket
            outputStream?.flush()
            outputStream?.close()
            socket?.close()
            outputStream = null
            socket = null
            Timber.tag(TAG).d( "Desconectado da impressora")
            
            // Pequeno delay para garantir que a conexão foi completamente fechada
            Thread.sleep(200)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao desconectar: ${e.message}")
            // Forçar limpeza mesmo em caso de erro
            outputStream = null
            socket = null
        }
    }
    
    /**
     * Verifica se está conectado
     */
    fun isConnected(): Boolean {
        return socket?.isConnected == true && outputStream != null
    }
    
    /**
     * Envia dados para a impressora
     * ✅ CORREÇÃO: Valida conexão antes de enviar
     */
    private fun sendData(data: ByteArray): Boolean {
        if (!isConnected()) {
            Timber.tag(TAG).e( "Tentativa de enviar dados sem conexão")
            return false
        }
        return try {
            outputStream?.write(data)
            outputStream?.flush()
            true
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Erro ao enviar dados: ${e.message}")
            false
        }
    }
    
    /**
     * ✅ NOVO: Reseta completamente a impressora (limpa buffer e estados)
     */
    private fun resetPrinter(): Boolean {
        if (!isConnected()) {
            return false
        }
        return try {
            // Reset completo (ESC @)
            sendData(EscPos.INIT)
            Thread.sleep(50) // Pequeno delay para garantir processamento
            
            // Resetar formatação
            sendData(EscPos.TEXT_NORMAL)
            sendData(EscPos.ALIGN_LEFT)
            
            // Limpar buffer com feed de linha
            sendData(EscPos.LINE_FEED)
            Thread.sleep(50)
            
            Timber.tag(TAG).d( "Impressora resetada com sucesso")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao resetar impressora: ${e.message}")
            false
        }
    }
    
    /**
     * Converte string para bytes com encoding compatível com impressoras térmicas brasileiras
     * Tenta usar CP850 (comum em impressoras térmicas) ou fallback para ISO-8859-1
     */
    private fun stringToBytes(text: String): ByteArray {
        return try {
            // Tentar CP850 primeiro (comum em impressoras térmicas brasileiras)
            text.toByteArray(Charsets.ISO_8859_1)
        } catch (e: Exception) {
            // Fallback para UTF-8 e depois converter caracteres problemáticos
            text.toByteArray(Charsets.UTF_8)
        }
    }
    
    /**
     * Comandos ESC/POS básicos
     */
    private object EscPos {
        // Inicializar impressora
        val INIT = byteArrayOf(0x1B, 0x40)
        
        // Alinhamento
        val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        
        // Formatação de texto
        val TEXT_NORMAL = byteArrayOf(0x1B, 0x21, 0x00) // Normal
        val TEXT_BOLD = byteArrayOf(0x1B, 0x21, 0x08) // Negrito
        val TEXT_LARGE = byteArrayOf(0x1D, 0x21, 0x11) // Duplo tamanho (altura e largura)
        val TEXT_DOUBLE_WIDTH = byteArrayOf(0x1B, 0x21, 0x20) // Dupla largura
        val TEXT_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10) // Dupla altura
        val TEXT_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11) // Duplo tamanho (altura e largura)
        val TEXT_MEDIUM = byteArrayOf(0x1B, 0x21, 0x01) // Médio (altura dupla)
        
        // Quebra de linha
        val LINE_FEED = byteArrayOf(0x0A)
        
        // Cortar papel
        val CUT = byteArrayOf(0x1D, 0x56, 0x41, 0x00)
        
        // Abrir gaveta (se disponível)
        val OPEN_DRAWER = byteArrayOf(0x10, 0x14, 0x01, 0x00, 0x00)
    }
    
    /**
     * Imprime recibo formatado
     */
    fun printRecibo(
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
        numeroContrato: String? = null,
        logoBitmap: Bitmap? = null
    ): Boolean {
        if (!isConnected()) {
            Timber.tag(TAG).e( "Impressora não conectada")
            return false
        }
        
        return try {
            // ✅ CRÍTICO: Reset completo antes de cada impressão
            if (!resetPrinter()) {
                Timber.tag(TAG).w( "Aviso: Reset da impressora falhou, continuando mesmo assim")
            }
            Thread.sleep(100) // Delay para garantir que o reset foi processado
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            
            // Inicializar impressora
            sendData(EscPos.INIT)
            
            // Divisor superior
            sendData(EscPos.ALIGN_CENTER)
            sendData(EscPos.TEXT_NORMAL)
            sendData(stringToBytes("────────────\n"))
            sendData(EscPos.LINE_FEED)
            
            // Logo (imprimir bitmap se disponível)
            if (logoBitmap != null) {
                printBitmap(logoBitmap)
                sendData(EscPos.LINE_FEED)
            } else {
                // Fallback para texto ASCII se logo não disponível
                sendData(stringToBytes("  BILHAR GLOBO\n"))
                sendData(stringToBytes("     R&A\n"))
                sendData(EscPos.LINE_FEED)
            }
            
            // Divisor após logo
            sendData(stringToBytes("────────────\n"))
            sendData(EscPos.LINE_FEED)
            
            // Título com emoji e tamanho maior
            sendData(EscPos.TEXT_BOLD)
            sendData(EscPos.TEXT_MEDIUM)
            sendData(stringToBytes("📋 RECIBO DE ACERTO\n"))
            sendData(EscPos.TEXT_NORMAL)
            sendData(EscPos.LINE_FEED)
            
            // Alinhar ao centro para dados do cliente
            sendData(EscPos.ALIGN_CENTER)
            
            // Cliente
            sendData(EscPos.TEXT_BOLD)
            sendData(stringToBytes("Cliente: "))
            sendData(EscPos.TEXT_NORMAL)
            sendData(stringToBytes("$clienteNome\n"))
            
            // CPF
            if (!clienteCpf.isNullOrBlank()) {
                sendData(EscPos.TEXT_BOLD)
                sendData(stringToBytes("CPF: "))
                sendData(EscPos.TEXT_NORMAL)
                sendData(stringToBytes("$clienteCpf\n"))
            }
            
            // Contrato
            if (!numeroContrato.isNullOrBlank()) {
                sendData(EscPos.TEXT_BOLD)
                sendData(stringToBytes("Contrato: "))
                sendData(EscPos.TEXT_NORMAL)
                sendData(stringToBytes("$numeroContrato\n"))
            }
            
            // Data
            sendData(EscPos.TEXT_BOLD)
            sendData(stringToBytes("Data: "))
            sendData(EscPos.TEXT_NORMAL)
            sendData(stringToBytes("$dataFormatada\n"))
            
            // Valor da ficha
            sendData(EscPos.TEXT_BOLD)
            sendData(stringToBytes("Preco da ficha: "))
            sendData(EscPos.TEXT_NORMAL)
            sendData(stringToBytes("${formatter.format(valorFicha)}\n"))
            
            sendData(EscPos.LINE_FEED)
            
            // MESAS ACERTADAS com emoji e tamanho maior
            sendData(EscPos.TEXT_BOLD)
            sendData(EscPos.TEXT_MEDIUM)
            sendData(stringToBytes("🎱 MESAS ACERTADAS\n"))
            sendData(EscPos.TEXT_NORMAL)
            
            var totalFichasJogadas = 0
            mesasCompletas.forEach { mesa ->
                val fichasJogadas = mesa.relogioFinal - mesa.relogioInicial
                totalFichasJogadas += fichasJogadas
                val tipoEquipamento = getTipoEquipamentoNome(mesa.tipoMesa)
                sendData(stringToBytes("$tipoEquipamento ${mesa.numero}\n"))
                sendData(stringToBytes("${mesa.relogioInicial} -> ${mesa.relogioFinal} (${fichasJogadas} fichas)\n"))
            }
            
            if (totalFichasJogadas > 0) {
                sendData(EscPos.TEXT_BOLD)
                sendData(stringToBytes("Fichas jogadas: "))
                sendData(EscPos.TEXT_NORMAL)
                sendData(stringToBytes("$totalFichasJogadas\n"))
            }
            
            sendData(EscPos.LINE_FEED)
            
            // RESUMO FINANCEIRO com emoji e tamanho maior
            sendData(EscPos.TEXT_BOLD)
            sendData(EscPos.TEXT_MEDIUM)
            sendData(stringToBytes("💰 RESUMO FINANCEIRO\n"))
            sendData(EscPos.TEXT_NORMAL)
            sendData(stringToBytes("Debito anterior: ${formatter.format(debitoAnterior)}\n"))
            sendData(stringToBytes("Total das mesas: ${formatter.format(valorTotalMesas)}\n"))
            val valorTotal = valorTotalMesas + debitoAnterior
            sendData(stringToBytes("Valor total: ${formatter.format(valorTotal)}\n"))
            sendData(stringToBytes("Desconto: ${formatter.format(desconto)}\n"))
            val valorRecebido = metodosPagamento.values.sum()
            sendData(stringToBytes("Valor recebido: ${formatter.format(valorRecebido)}\n"))
            sendData(stringToBytes("Debito atual: ${formatter.format(debitoAtual)}\n"))
            
            sendData(EscPos.LINE_FEED)
            
            // FORMA DE PAGAMENTO com emoji e tamanho maior
            sendData(EscPos.TEXT_BOLD)
            sendData(EscPos.TEXT_MEDIUM)
            sendData(stringToBytes("💳 FORMA DE PAGAMENTO\n"))
            sendData(EscPos.TEXT_NORMAL)
            if (metodosPagamento.isNotEmpty()) {
                metodosPagamento.forEach { (metodo, valor) ->
                    sendData(stringToBytes("$metodo: ${formatter.format(valor)}\n"))
                }
            } else {
                sendData(stringToBytes("Nao informado\n"))
            }
            
            sendData(EscPos.LINE_FEED)
            
            // OBSERVAÇÕES com emoji e tamanho maior
            sendData(EscPos.TEXT_BOLD)
            sendData(EscPos.TEXT_MEDIUM)
            sendData(stringToBytes("📝 OBSERVACOES\n"))
            sendData(EscPos.TEXT_NORMAL)
            // ✅ CORREÇÃO: Enviar observação sem delay - enviar tudo de uma vez
            val observacaoTexto = if (!observacao.isNullOrBlank()) {
                "$observacao\n"
            } else {
                "Nenhuma observacao registrada.\n"
            }
            sendData(stringToBytes(observacaoTexto))
            
            sendData(EscPos.LINE_FEED)
            
            // Divisor inferior
            sendData(stringToBytes("────────────\n"))
            sendData(EscPos.LINE_FEED)
            
            // ✅ CORREÇÃO: Agradecimento no final (garantir que aparece completo)
            sendData(EscPos.ALIGN_CENTER)
            sendData(EscPos.TEXT_BOLD)
            // ✅ CRÍTICO: Enviar agradecimento completo em um único bloco para evitar corte
            val agradecimentoCompleto = "Acerto realizado via\nGestaoBilhares\nObrigado por confiar!\n"
            sendData(stringToBytes(agradecimentoCompleto))
            sendData(EscPos.TEXT_NORMAL)
            sendData(EscPos.ALIGN_LEFT)
            
            // ✅ CRÍTICO: Feed de linha extra antes de cortar para garantir que tudo foi impresso
            sendData(EscPos.LINE_FEED)
            sendData(EscPos.LINE_FEED)
            sendData(EscPos.LINE_FEED)
            
            // Cortar papel (sem delay antes)
            sendData(EscPos.CUT)
            
            // ✅ CRÍTICO: Flush final para garantir que todos os dados foram enviados de uma vez
            outputStream?.flush()
            // ✅ REMOVIDO: Delays removidos para impressão contínua sem pausas
            
            // ✅ CRÍTICO: Reset após impressão para limpar estado
            sendData(byteArrayOf(0x1B, 0x40))
            sendData(EscPos.TEXT_NORMAL)
            sendData(EscPos.ALIGN_LEFT)
            outputStream?.flush()
            
            Timber.tag(TAG).d( "Recibo impresso com sucesso")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao imprimir recibo: ${e.message}")
            false
        }
    }
    
    /**
     * Retorna o nome do tipo do equipamento
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
     * Imprime bitmap na impressora térmica usando comandos ESC/POS
     * Converte o bitmap para formato raster compatível com impressoras térmicas
     */
    private fun printBitmap(bitmap: Bitmap): Boolean {
        return printBitmapRasterGS(bitmap)
    }
    
    /**
     * Imprime um bitmap usando GS v 0 (modo raster), compatível com impressoras Knup/Xprinter.
     * - Redimensiona para 384px de largura
     * - Converte para preto e branco puro
     * - Envia o bitmap inteiro em modo raster
     * ✅ CORREÇÃO: Melhorado para evitar caracteres aleatórios
     */
    private fun printBitmapRasterGS(bitmap: Bitmap): Boolean {
        if (!isConnected()) {
            Timber.tag(TAG).e( "Impressora não conectada para imprimir bitmap")
            return false
        }
        
        return try {
            // ✅ RESTAURADO: Reset simples antes de processar o bitmap
            sendData(byteArrayOf(0x1B, 0x40)) // ESC @ - Reset
            Thread.sleep(100)
            
            val targetWidth = 384
            val scale = targetWidth.toFloat() / bitmap.width
            val targetHeight = (bitmap.height * scale).toInt()
            val resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            
            // Converter para preto e branco puro (threshold)
            val bwBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bwBitmap)
            val paint = android.graphics.Paint()
            val colorMatrix = android.graphics.ColorMatrix()
            colorMatrix.setSaturation(0f)
            val filter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            paint.colorFilter = filter
            canvas.drawBitmap(resized, 0f, 0f, paint)
            
            for (y in 0 until targetHeight) {
                for (x in 0 until targetWidth) {
                    val pixel = bwBitmap.getPixel(x, y)
                    val gray = android.graphics.Color.red(pixel)
                    val bw = if (gray < 128) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    bwBitmap.setPixel(x, y, bw)
                }
            }
            
            // ✅ CORREÇÃO CRÍTICA: Calcular bytesPerLine corretamente (arredondar para cima)
            // Cada byte representa 8 pixels, então precisamos de (width + 7) / 8 bytes
            val bytesPerLine = (targetWidth + 7) / 8
            val height = targetHeight
            val xL = (bytesPerLine and 0xFF).toByte()
            val xH = ((bytesPerLine shr 8) and 0xFF).toByte()
            val yL = (height and 0xFF).toByte()
            val yH = ((height shr 8) and 0xFF).toByte()
            val GS = 0x1D.toByte()
            val v = 0x76.toByte()
            val zero = 0x30.toByte()
            val m = 0x00.toByte() // 0: normal, 1: double width, 2: double height, 3: both
            
            // ✅ CRÍTICO: GS v 0 m xL xH yL yH [dados] - formato correto ESC/POS
            // IMPORTANTE: Este comando deve ser enviado imediatamente após o reset
            val gsCommand = byteArrayOf(GS, v, zero, m, xL, xH, yL, yH)
            if (!sendData(gsCommand)) {
                Timber.tag(TAG).e( "Erro ao enviar comando GS v 0")
                return false
            }
            
            // ✅ CRÍTICO: Enviar dados do bitmap linha por linha
            // IMPORTANTE: Os dados devem ser enviados imediatamente após o comando GS v 0
            // Sem delays ou comandos intermediários
            val lineBuffer = ByteArray(bytesPerLine)
            for (y in 0 until height) {
                for (x in 0 until bytesPerLine) {
                    var byte = 0
                    for (bit in 0 until 8) {
                        val pixelX = x * 8 + bit
                        if (pixelX < targetWidth) {
                            val color = bwBitmap.getPixel(pixelX, y)
                            if (color == android.graphics.Color.BLACK) {
                                byte = byte or (1 shl (7 - bit))
                            }
                        }
                    }
                    lineBuffer[x] = byte.toByte()
                }
                // ✅ CRÍTICO: Enviar linha completa diretamente no outputStream
                // Não usar sendData aqui para evitar flush intermediário que pode corromper
                try {
                    outputStream?.write(lineBuffer)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Erro ao enviar linha $y do bitmap: ${e.message}")
                    return false
                }
            }
            
            // ✅ CRÍTICO: Flush APENAS após todos os dados serem enviados
            outputStream?.flush()
            Thread.sleep(200) // Delay para garantir que a impressão foi concluída
            
            // ✅ CRÍTICO: Feed de linha após bitmap (não reset ainda)
            sendData(byteArrayOf(0x0A))
            outputStream?.flush()
            Thread.sleep(100)
            
            Timber.tag(TAG).d( "Bitmap impresso com sucesso")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao imprimir bitmap raster: ${e.message}")
            false
        }
    }
    
    /**
     * Imprime um recibo a partir de um layout preenchido dinamicamente (View -> Bitmap -> GS v 0)
     * Esta é a função original que convertia o layout XML em bitmap e imprimia
     * ✅ CORREÇÃO CRÍTICA: Reset completo e limpeza de buffer antes de cada impressão
     */
    fun printReciboLayoutBitmap(view: android.view.View): Boolean {
        if (!isConnected()) {
            Timber.tag(TAG).e( "Impressora não conectada para imprimir layout")
            return false
        }
        
        return try {
            // ✅ RESTAURADO: Versão simples que funcionava corretamente
            // Reset será feito dentro de printBitmapRasterGS se necessário
            // Medir e desenhar o layout em um bitmap
            val width = 384 // largura máxima da impressora
            val specWidth = android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY)
            val specHeight = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            view.measure(specWidth, specHeight)
            val measuredWidth = view.measuredWidth
            var measuredHeight = view.measuredHeight
            
            // ✅ CORREÇÃO: Adicionar margem extra apenas para garantir que o agradecimento apareça completo
            // Aumentar 50 pixels para garantir espaço suficiente para o texto "Obrigado por confiar!"
            measuredHeight += 50
            
            view.layout(0, 0, measuredWidth, measuredHeight)
            
            // Criar bitmap e desenhar a view
            val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            view.draw(canvas)
            
            // Imprimir usando GS v 0
            val success = printBitmapRasterGS(bitmap)
            
            // ✅ CRÍTICO: Garantir que todos os dados foram enviados
            if (success) {
                outputStream?.flush()
                Thread.sleep(300) // Delay maior para garantir que a impressão foi concluída
            }
            
            success
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro ao imprimir layout bitmap: ${e.message}")
            false
        }
    }
}

