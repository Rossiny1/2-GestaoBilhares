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

    fun printText(text: String) {
        try {
            // ESC/POS: Reset + alinhar à esquerda
            outputStream?.write(byteArrayOf(0x1B, 0x40))
            outputStream?.write(byteArrayOf(0x1B, 0x61, 0x00))
            outputStream?.write(text.toByteArray(Charsets.UTF_8))
            outputStream?.write(byteArrayOf(0x0A, 0x0A, 0x0A)) // 3 linhas para corte manual
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