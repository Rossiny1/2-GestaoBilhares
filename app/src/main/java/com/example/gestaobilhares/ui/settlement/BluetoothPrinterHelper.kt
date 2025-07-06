package com.example.gestaobilhares.ui.settlement

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import java.util.*

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

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
} 