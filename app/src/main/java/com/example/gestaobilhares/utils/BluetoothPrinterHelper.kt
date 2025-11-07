package com.example.gestaobilhares.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import java.util.*
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * ✅ FASE 1: Movido de ui/settlement/ para utils/ (é utilitário, não UI)
 * Helper para comunicação Bluetooth com impressoras térmicas
 */
class BluetoothPrinterHelper(private val device: BluetoothDevice) {
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun connect(): Boolean {
        return try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            socket?.connect()
            outputStream = socket?.outputStream
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun printText(text: String, boldLines: List<Int> = emptyList(), centerLines: List<Int> = emptyList()) {
        try {
            val lines = text.lines()
            // ESC/POS: Reset
            outputStream?.write(byteArrayOf(0x1B, 0x40))
            for ((i, line) in lines.withIndex()) {
                // Centralizar
                if (i in centerLines) {
                    outputStream?.write(byteArrayOf(0x1B, 0x61, 0x01))
                } else {
                    outputStream?.write(byteArrayOf(0x1B, 0x61, 0x00))
                }
                // Negrito
                if (i in boldLines) {
                    outputStream?.write(byteArrayOf(0x1B, 0x45, 0x01))
                }
                outputStream?.write(line.toByteArray(Charsets.UTF_8))
                outputStream?.write(byteArrayOf(0x0A))
                if (i in boldLines) {
                    outputStream?.write(byteArrayOf(0x1B, 0x45, 0x00))
                }
            }
            // Corte de papel (se suportado)
            outputStream?.write(byteArrayOf(0x1D, 0x56, 0x01))
            outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Imprime um bitmap em modo ESC/POS 8-dot single density (compatível com KP-1025)
     * - Redimensiona para 384px de largura
     * - Converte para preto e branco puro
     * - Envia linha a linha usando ESC * m nL nH [dados]
     */
    fun printBitmapEscPos(bitmap: android.graphics.Bitmap) {
        try {
            // 1. Redimensionar para 384px de largura
            val targetWidth = 384
            val scale = targetWidth.toFloat() / bitmap.width
            val targetHeight = (bitmap.height * scale).toInt()
            val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

            // 2. Converter para preto e branco puro (threshold)
            val bwBitmap = android.graphics.Bitmap.createBitmap(targetWidth, targetHeight, android.graphics.Bitmap.Config.ARGB_8888)
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

            // 3. Enviar linha a linha (8 pixels de altura por vez)
            val bytesPerLine = targetWidth / 8
            val ESC = 0x1B.toByte()
            val GS = 0x1D.toByte()
            for (y in 0 until targetHeight step 8) {
                val cmd = byteArrayOf(ESC, 0x2A, 0x00, (bytesPerLine and 0xFF).toByte(), (bytesPerLine shr 8).toByte())
                outputStream?.write(cmd)
                for (x in 0 until targetWidth) {
                    var byte = 0
                    for (bit in 0 until 8) {
                        val yy = y + bit
                        if (yy < targetHeight) {
                            val color = bwBitmap.getPixel(x, yy)
                            if (color == android.graphics.Color.BLACK) {
                                byte = byte or (1 shl (7 - bit))
                            }
                        }
                    }
                    outputStream?.write(byte)
                }
                // Avança uma linha
                outputStream?.write(byteArrayOf(0x0A))
            }
            // Feed extra para garantir saída
            outputStream?.write(byteArrayOf(0x0A, 0x0A))
            outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Imprime um bitmap usando GS v 0 (modo raster), compatível com impressoras Knup/Xprinter.
     * - Redimensiona para 384px de largura
     * - Converte para preto e branco puro
     * - Envia o bitmap inteiro em modo raster
     */
    fun printBitmapRasterGS(bitmap: android.graphics.Bitmap) {
        try {
            val targetWidth = 384
            val scale = targetWidth.toFloat() / bitmap.width
            val targetHeight = (bitmap.height * scale).toInt()
            val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

            // Converter para preto e branco puro (threshold)
            val bwBitmap = android.graphics.Bitmap.createBitmap(targetWidth, targetHeight, android.graphics.Bitmap.Config.ARGB_8888)
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

            val bytesPerLine = targetWidth / 8
            val height = targetHeight
            val xL = (bytesPerLine and 0xFF).toByte()
            val xH = ((bytesPerLine shr 8) and 0xFF).toByte()
            val yL = (height and 0xFF).toByte()
            val yH = ((height shr 8) and 0xFF).toByte()
            val GS = 0x1D.toByte()
            val v = 0x76.toByte()
            val zero = 0x30.toByte()
            val m = 0x00.toByte() // 0: normal, 1: double width, 2: double height, 3: both

            // ESC @ (reset)
            outputStream?.write(byteArrayOf(0x1B, 0x40))
            // GS v 0 m xL xH yL yH [dados]
            outputStream?.write(byteArrayOf(GS, v, zero, m, xL, xH, yL, yH))

            // Enviar os dados do bitmap
            for (y in 0 until height) {
                for (x in 0 until bytesPerLine) {
                    var byte = 0
                    for (bit in 0 until 8) {
                        val pixelX = x * 8 + bit
                        val color = bwBitmap.getPixel(pixelX, y)
                        if (color == android.graphics.Color.BLACK) {
                            byte = byte or (1 shl (7 - bit))
                        }
                    }
                    outputStream?.write(byte)
                }
            }
            // Feed extra
            outputStream?.write(byteArrayOf(0x0A, 0x0A))
            outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Imprime um recibo a partir de um layout preenchido dinamicamente (View -> Bitmap -> GS v 0)
     */
    fun printReciboLayoutBitmap(view: android.view.View) {
        // Medir e desenhar o layout em um bitmap
        val width = 384 // largura máxima da impressora
        val specWidth = android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY)
        val specHeight = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        view.measure(specWidth, specHeight)
        val measuredWidth = view.measuredWidth
        val measuredHeight = view.measuredHeight
        view.layout(0, 0, measuredWidth, measuredHeight)
        // Forçar layout completo
        view.isDrawingCacheEnabled = true
        val bitmap = android.graphics.Bitmap.createBitmap(measuredWidth, measuredHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)
        view.isDrawingCacheEnabled = false
        // Imprimir usando GS v 0
        printBitmapRasterGS(bitmap)
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

