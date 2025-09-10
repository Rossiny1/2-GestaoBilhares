package com.example.gestaobilhares.ui.contracts

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentSignatureCaptureBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class SignatureCaptureFragment : Fragment() {
    
    private var _binding: FragmentSignatureCaptureBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SignatureCaptureViewModel by viewModels()
    
    private var signatureBitmap: Bitmap? = null
    private var signaturePath = android.graphics.Path()
    private var signaturePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enviarContratoViaWhatsApp()
        } else {
            Toast.makeText(requireContext(), "Permissão necessária para enviar via WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignatureCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val contratoId = arguments?.getLong("contrato_id") ?: 0L
        
        viewModel.carregarContrato(contratoId)
        setupObservers()
        setupSignatureView()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contrato.collect { contrato ->
                contrato?.let {
                    binding.tvContratoInfo.text = "Contrato: ${it.numeroContrato}\nLocatário: ${it.locatarioNome}"
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                binding.btnSalvarAssinatura.isEnabled = !loading
                binding.btnEnviarWhatsApp.isEnabled = !loading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assinaturaSalva.collect { salva ->
                if (salva) {
                    binding.btnEnviarWhatsApp.isEnabled = true
                    Toast.makeText(requireContext(), "Assinatura salva com sucesso!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupSignatureView() {
        binding.signatureView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    signaturePath.moveTo(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    signaturePath.lineTo(event.x, event.y)
                    binding.signatureView.invalidate()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    signaturePath.lineTo(event.x, event.y)
                    binding.signatureView.invalidate()
                    true
                }
                else -> false
            }
        }
        
        // Customizar a view de assinatura
        binding.signatureView.setBackgroundColor(Color.WHITE)
    }
    
    private fun setupClickListeners() {
        binding.apply {
            btnLimparAssinatura.setOnClickListener {
                limparAssinatura()
            }
            
            btnSalvarAssinatura.setOnClickListener {
                salvarAssinatura()
            }
            
            btnEnviarWhatsApp.setOnClickListener {
                verificarPermissaoEEnviar()
            }
        }
    }
    
    private fun limparAssinatura() {
        signaturePath.reset()
        binding.signatureView.invalidate()
        signatureBitmap = null
    }
    
    private fun salvarAssinatura() {
        if (signaturePath.isEmpty) {
            Toast.makeText(requireContext(), "Por favor, assine o contrato", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Capturar assinatura como bitmap
        signatureBitmap = capturarAssinaturaComoBitmap()
        
        // Converter para Base64
        val assinaturaBase64 = bitmapToBase64(signatureBitmap!!)
        
        // Salvar no ViewModel
        viewModel.salvarAssinatura(assinaturaBase64)
    }
    
    private fun capturarAssinaturaComoBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            binding.signatureView.width,
            binding.signatureView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawPath(signaturePath, signaturePaint)
        return bitmap
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    private fun verificarPermissaoEEnviar() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enviarContratoViaWhatsApp()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }
    
    private fun enviarContratoViaWhatsApp() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val contrato = viewModel.contrato.value ?: return@launch
                
                // Gerar PDF do contrato
                val pdfGenerator = com.example.gestaobilhares.utils.ContractPdfGenerator(requireContext())
                val pdfFile = pdfGenerator.generateContractPdf(contrato, emptyList())
                
                // Preparar dados para envio
                val whatsappNumber = contrato.locatarioTelefone.replace(Regex("[^0-9]"), "")
                val message = "Contrato de locação ${contrato.numeroContrato} assinado com sucesso!"
                
                // TODO: Implementar envio via WhatsApp
                // Por enquanto, mostrar mensagem de sucesso
                Toast.makeText(requireContext(), "Contrato pronto para envio via WhatsApp!", Toast.LENGTH_LONG).show()
                
                // Navegar de volta
                findNavController().popBackStack()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao preparar envio: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
