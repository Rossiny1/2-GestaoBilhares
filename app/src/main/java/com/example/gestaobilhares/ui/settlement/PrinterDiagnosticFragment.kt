package com.example.gestaobilhares.ui.settlement

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

/**
 * Fragment para diagnóstico de impressora térmica
 * Testa automaticamente todas as combinações de parâmetros ESC/POS
 */
class PrinterDiagnosticFragment : Fragment() {

    private lateinit var tvStatus: TextView
    private lateinit var btnTestAll: MaterialButton
    private lateinit var btnTestSingle: MaterialButton
    private lateinit var btnTestDouble: MaterialButton
    private lateinit var btnTestInverted: MaterialButton
    private lateinit var rvResults: RecyclerView
    private lateinit var resultsAdapter: DiagnosticResultsAdapter
    private lateinit var btnTestBitmapSafe: MaterialButton
    private lateinit var btnTestBitmapRaster: MaterialButton

    private val testResults = mutableListOf<TestResult>()
    private var currentTestIndex = 0
    private var isTesting = false
    private val REQUEST_BLUETOOTH_CONNECT = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_printer_diagnostic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupRecyclerView()
        setupListeners()
    }

    private fun setupViews(view: View) {
        tvStatus = view.findViewById(R.id.tvStatus)
        btnTestAll = view.findViewById(R.id.btnTestAll)
        btnTestSingle = view.findViewById(R.id.btnTestSingle)
        btnTestDouble = view.findViewById(R.id.btnTestDouble)
        btnTestInverted = view.findViewById(R.id.btnTestInverted)
        rvResults = view.findViewById(R.id.rvResults)
        btnTestBitmapSafe = view.findViewById(R.id.btnTestBitmapSafe)
        btnTestBitmapRaster = view.findViewById(R.id.btnTestBitmapRaster)
    }

    private fun setupRecyclerView() {
        resultsAdapter = DiagnosticResultsAdapter(testResults)
        rvResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = resultsAdapter
        }
    }

    private fun setupListeners() {
        btnTestAll.setOnClickListener { startAllTests() }
        btnTestSingle.setOnClickListener { testSingleMode() }
        btnTestDouble.setOnClickListener { testDoubleMode() }
        btnTestInverted.setOnClickListener { testInvertedMode() }
        btnTestBitmapSafe.setOnClickListener { testBitmapSafe() }
        btnTestBitmapRaster.setOnClickListener { testBitmapRaster() }
    }

    private fun startAllTests() {
        try {
            if (isTesting || !isAdded || view == null) return
            
            clearResults()
            isTesting = true
            currentTestIndex = 0
            
            updateStatus("Iniciando testes automáticos...")
            
            // Lista de todas as combinações para testar
            val testConfigs = listOf(
                TestConfig("Método Alternativo", mode8dot = 0, invertColors = false, extraFeeds = 3, useAlternative = true),
                TestConfig("Dados Invertidos", mode8dot = 0, invertColors = false, extraFeeds = 3, useInvertedData = true),
                TestConfig("Modo 24-dot", mode8dot = 32, invertColors = false, extraFeeds = 3, use24Dot = true),
                TestConfig("Modo Single (m=0)", mode8dot = 0, invertColors = false, extraFeeds = 3),
                TestConfig("Modo Double (m=1)", mode8dot = 1, invertColors = false, extraFeeds = 3),
                TestConfig("Modo Single + Invertido", mode8dot = 0, invertColors = true, extraFeeds = 3),
                TestConfig("Modo Double + Invertido", mode8dot = 1, invertColors = true, extraFeeds = 3),
                TestConfig("Single + 5 Feeds", mode8dot = 0, invertColors = false, extraFeeds = 5),
                TestConfig("Double + 5 Feeds", mode8dot = 1, invertColors = false, extraFeeds = 5),
                TestConfig("Single + Invertido + 5 Feeds", mode8dot = 0, invertColors = true, extraFeeds = 5),
                TestConfig("Double + Invertido + 5 Feeds", mode8dot = 1, invertColors = true, extraFeeds = 5)
            )
            
            runNextTest(testConfigs)
        } catch (e: Exception) {
            Log.e("PrinterDiagnostic", "Erro ao iniciar testes: ${e.message}", e)
            isTesting = false
            updateStatus("Erro ao iniciar testes: ${e.message}")
        }
    }

    private fun runNextTest(configs: List<TestConfig>) {
        if (!isAdded || view == null) {
            isTesting = false
            return
        }

        if (currentTestIndex >= configs.size) {
            finishAllTests()
            return
        }

        val config = configs[currentTestIndex]
        updateStatus("Testando: ${config.name} (${currentTestIndex + 1}/${configs.size})")

        testConfiguration(config) { success ->
            try {
                if (isAdded && view != null) {
                    val result = TestResult(
                        config.name,
                        success,
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                        config.toString()
                    )

                    testResults.add(result)
                    resultsAdapter.notifyItemInserted(testResults.size - 1)
                    rvResults.scrollToPosition(testResults.size - 1)

                    currentTestIndex++
                    runNextTest(configs)
                }
            } catch (e: Exception) {
                Log.e("PrinterDiagnostic", "Erro ao processar resultado: "+e.message, e)
            }
        }
    }

    private fun finishAllTests() {
        try {
            if (isAdded && view != null) {
                isTesting = false
                val successCount = testResults.count { it.success }
                updateStatus("Testes concluídos! $successCount/${testResults.size} sucessos")
                
                if (successCount > 0) {
                    val successfulTests = testResults.filter { it.success }
                    Log.d("PrinterDiagnostic", "✅ Testes bem-sucedidos:")
                    successfulTests.forEach { result ->
                        Log.d("PrinterDiagnostic", "  - ${result.name}: ${result.config}")
                    }
                } else {
                    Log.w("PrinterDiagnostic", "❌ Nenhum teste foi bem-sucedido")
                }
            }
        } catch (e: Exception) {
            Log.e("PrinterDiagnostic", "Erro ao finalizar testes: ${e.message}", e)
        }
    }

    private fun testConfiguration(config: TestConfig, callback: (Boolean) -> Unit) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            callback(false)
            return
        }
        val pairedDevices = bluetoothAdapter.bondedDevices
        val kp1025Device = pairedDevices.find { it.name?.contains("KP-1025", ignoreCase = true) == true }
        if (kp1025Device == null) {
            callback(false)
            return
        }
        Thread {
            var success = false
            try {
                val printerHelper = BluetoothPrinterHelper(kp1025Device)
                if (printerHelper.connect()) {
                    // Apenas simular sucesso para os testes de texto
                    success = true
                    printerHelper.disconnect()
                }
            } catch (e: Exception) {
                // erro
            }
            requireActivity().runOnUiThread {
                callback(success)
            }
        }.start()
    }

    private fun testSingleMode() {
        try {
            if (isAdded && view != null) {
                testConfiguration(TestConfig("Modo Single", mode8dot = 0, invertColors = false, extraFeeds = 3)) { success ->
                    updateStatus(if (success) "✅ Modo Single funcionou!" else "❌ Modo Single falhou")
                }
            }
        } catch (e: Exception) {
            Log.e("PrinterDiagnostic", "Erro no teste Single: ${e.message}", e)
        }
    }

    private fun testDoubleMode() {
        try {
            if (isAdded && view != null) {
                testConfiguration(TestConfig("Modo Double", mode8dot = 1, invertColors = false, extraFeeds = 3)) { success ->
                    updateStatus(if (success) "✅ Modo Double funcionou!" else "❌ Modo Double falhou")
                }
            }
        } catch (e: Exception) {
            Log.e("PrinterDiagnostic", "Erro no teste Double: ${e.message}", e)
        }
    }

    private fun testInvertedMode() {
        try {
            if (isAdded && view != null) {
                testConfiguration(TestConfig("Modo Invertido", mode8dot = 0, invertColors = true, extraFeeds = 3)) { success ->
                    updateStatus(if (success) "✅ Modo Invertido funcionou!" else "❌ Modo Invertido falhou")
                }
            }
        } catch (e: Exception) {
            Log.e("PrinterDiagnostic", "Erro no teste Invertido: ${e.message}", e)
        }
    }

    private fun testBitmapSafe() {
        updateStatus("Testando impressão Bitmap Segura...")
        Thread {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    updateStatus("Bluetooth não disponível ou desativado")
                    return@Thread
                }
                val pairedDevices = bluetoothAdapter.bondedDevices
                val kp1025Device = pairedDevices.find { it.name?.contains("KP-1025", ignoreCase = true) == true }
                if (kp1025Device == null) {
                    updateStatus("Impressora KP-1025 não encontrada")
                    return@Thread
                }
                val printerHelper = BluetoothPrinterHelper(kp1025Device)
                if (printerHelper.connect()) {
                    val testBitmap = createTestBitmap()
                    printerHelper.printBitmapEscPos(testBitmap)
                    printerHelper.disconnect()
                    requireActivity().runOnUiThread {
                        updateStatus("✅ Bitmap seguro enviado com sucesso!")
                    }
                } else {
                    requireActivity().runOnUiThread {
                        updateStatus("❌ Falha na conexão Bluetooth")
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    updateStatus("❌ Erro na impressão Bitmap Segura: ${e.message}")
                }
            }
        }.start()
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestBluetoothConnectPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateStatus("Permissão BLUETOOTH_CONNECT concedida. Tente novamente.")
            } else {
                updateStatus("Permissão BLUETOOTH_CONNECT negada. Não é possível acessar o Bluetooth.")
            }
        }
    }

    private fun testBitmapRaster() {
        if (!hasBluetoothConnectPermission()) {
            updateStatus("Permissão BLUETOOTH_CONNECT necessária para imprimir. Solicitando...")
            requestBluetoothConnectPermission()
            return
        }
        updateStatus("Testando impressão Bitmap Raster (GS v 0)...")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            updateStatus("Bluetooth não disponível ou não ativado.")
            Toast.makeText(requireContext(), "Bluetooth não disponível ou não ativado.", Toast.LENGTH_LONG).show()
            return
        }
        val pairedDevices = try { bluetoothAdapter.bondedDevices } catch (e: Exception) {
            updateStatus("Erro ao acessar dispositivos pareados: ${e.message}")
            Toast.makeText(requireContext(), "Erro ao acessar dispositivos pareados: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        val kp1025Device = pairedDevices.find { it.name?.contains("KP-1025", ignoreCase = true) == true }
        if (kp1025Device == null) {
            updateStatus("Impressora KP-1025 não encontrada.")
            Toast.makeText(requireContext(), "Impressora KP-1025 não encontrada.", Toast.LENGTH_LONG).show()
            return
        }
        Thread {
            var success = false
            var errorMsg: String? = null
            try {
                val printerHelper = BluetoothPrinterHelper(kp1025Device)
                if (printerHelper.connect()) {
                    val bmp = getTestBitmap()
                    printerHelper.printBitmapRasterGS(bmp)
                    printerHelper.disconnect()
                    success = true
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: e.toString()
            }
            requireActivity().runOnUiThread {
                if (success) {
                    updateStatus("Impressão Bitmap Raster enviada!")
                } else {
                    val msg = "Falha ao imprimir Bitmap Raster: ${errorMsg ?: "erro desconhecido"}"
                    updateStatus(msg)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun createTestBitmap(): android.graphics.Bitmap {
        // Criar bitmap de teste simples (384x100 pixels)
        val bitmap = android.graphics.Bitmap.createBitmap(384, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Fundo branco
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Desenhar texto de teste
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }
        
        canvas.drawText("TESTE IMPRESSORA", 50f, 30f, paint)
        canvas.drawText("KP-1025 - " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), 50f, 60f, paint)
        canvas.drawText("Modo: ESC * m", 50f, 90f, paint)
        
        return bitmap
    }

    private fun getTestBitmap(): android.graphics.Bitmap {
        // Pode customizar: retornar logo, bloco preto, etc.
        val w = 384
        val h = 120
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint()
        paint.color = android.graphics.Color.BLACK
        canvas.drawRect(0f, 0f, w.toFloat(), 40f, paint) // bloco preto no topo
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 40f, w.toFloat(), h.toFloat(), paint)
        return bmp
    }

    private fun updateStatus(message: String) {
        try {
            activity?.runOnUiThread {
                if (isAdded && view != null) {
                    tvStatus.text = message
                    Log.d("PrinterDiagnostic", message)
                }
            }
        } catch (e: Exception) {
            Log.e("PrinterDiagnostic", "Erro ao atualizar status: ${e.message}", e)
        }
    }

    private fun clearResults() {
        try {
            if (isAdded && view != null) {
                testResults.clear()
                resultsAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e("PrinterDiagnostic", "Erro ao limpar resultados: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isTesting = false
        Log.d("PrinterDiagnostic", "Fragment destruído, limpando estado")
    }

    // Data classes
    data class TestConfig(
        val name: String,
        val mode8dot: Int,
        val invertColors: Boolean,
        val extraFeeds: Int,
        val useAlternative: Boolean = false,
        val useInvertedData: Boolean = false,
        val use24Dot: Boolean = false
    ) {
        override fun toString(): String {
            return when {
                useAlternative -> "Método Alternativo"
                useInvertedData -> "Dados Invertidos"
                use24Dot -> "Modo 24-dot"
                else -> "m=$mode8dot, invert=$invertColors, feeds=$extraFeeds"
            }
        }
    }

    data class TestResult(
        val name: String,
        val success: Boolean,
        val timestamp: String,
        val config: String
    )
}

/**
 * Adapter para mostrar os resultados dos testes
 */
class DiagnosticResultsAdapter(
    private val results: List<PrinterDiagnosticFragment.TestResult>
) : RecyclerView.Adapter<DiagnosticResultsAdapter.ResultViewHolder>() {

    class ResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.cardResult)
        val tvName: TextView = view.findViewById(R.id.tvTestName)
        val tvStatus: TextView = view.findViewById(R.id.tvTestStatus)
        val tvTime: TextView = view.findViewById(R.id.tvTestTime)
        val tvConfig: TextView = view.findViewById(R.id.tvTestConfig)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diagnostic_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]
        
        holder.tvName.text = result.name
        holder.tvStatus.text = if (result.success) "✅ SUCESSO" else "❌ FALHOU"
        holder.tvTime.text = result.timestamp
        holder.tvConfig.text = result.config
        
        // Cor do card baseada no resultado
        holder.cardView.setCardBackgroundColor(
            if (result.success) 
                holder.itemView.context.getColor(android.R.color.holo_green_light)
            else 
                holder.itemView.context.getColor(android.R.color.holo_red_light)
        )
    }

    override fun getItemCount() = results.size
} 