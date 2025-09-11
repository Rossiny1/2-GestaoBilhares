package com.example.gestaobilhares.ui.contracts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentSignatureCaptureBinding
import com.example.gestaobilhares.utils.DocumentIntegrityManager
import com.example.gestaobilhares.utils.LegalLogger
import com.example.gestaobilhares.utils.SignatureMetadataCollector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class SignatureCaptureFragment : Fragment() {
    
    companion object {
        private const val TAG = "SignatureCaptureFragment"
    }
    
    private var _binding: FragmentSignatureCaptureBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SignatureCaptureViewModel by viewModels()
    
    @Inject
    lateinit var legalLogger: LegalLogger
    
    @Inject
    lateinit var documentIntegrityManager: DocumentIntegrityManager
    
    @Inject
    lateinit var metadataCollector: SignatureMetadataCollector
    
    private var signatureBitmap: Bitmap? = null
    
    
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
        // A SignatureView já tem seu próprio onTouchEvent implementado
        // Não precisamos de um OnTouchListener adicional
        
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
        binding.signatureView.clear()
        signatureBitmap = null
    }
    
    private fun salvarAssinatura() {
        if (!binding.signatureView.hasSignature()) {
            Toast.makeText(requireContext(), "Por favor, assine o contrato", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Verificar se a assinatura é válida
        val statistics = binding.signatureView.getSignatureStatistics()
        
        // Log para debug
        Log.d(TAG, "Estatísticas da assinatura:")
        Log.d(TAG, "Total de pontos: ${statistics.totalPoints}")
        Log.d(TAG, "Duração: ${statistics.duration}ms")
        Log.d(TAG, "Pressão média: ${statistics.averagePressure}")
        Log.d(TAG, "Velocidade média: ${statistics.averageVelocity}")
        Log.d(TAG, "Assinatura válida: ${statistics.isValidSignature()}")
        
        if (!statistics.isValidSignature()) {
            Toast.makeText(requireContext(), "Assinatura muito simples. Por favor, assine novamente com mais detalhes.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Capturar assinatura como bitmap
        signatureBitmap = binding.signatureView.getSignatureBitmap()
        
        // Gerar hash da assinatura
        val signatureHash = documentIntegrityManager.generateSignatureHash(signatureBitmap!!)
        
        // Coletar metadados jurídicos
        val contrato = viewModel.contrato.value
        if (contrato != null) {
            val documentHash = documentIntegrityManager.generateDocumentHash(ByteArray(0)) // TODO: Obter hash real do documento
            val metadata = metadataCollector.collectSignatureMetadata(documentHash, signatureHash)
            
            // Registrar log jurídico
            viewLifecycleOwner.lifecycleScope.launch {
                legalLogger.logSignatureEvent(
                    contratoId = contrato.id,
                    userId = contrato.clienteId.toString(),
                    action = "ASSINATURA_SALVA",
                    metadata = metadata
                )
                
                Log.d(TAG, "Log jurídico registrado para contrato ${contrato.id}")
                Log.d(TAG, statistics.generateSummary())
            }
        }
        
        // Converter para Base64
        val assinaturaBase64 = bitmapToBase64(signatureBitmap!!)
        
        // Salvar no ViewModel
        viewModel.salvarAssinatura(assinaturaBase64)
    }
    
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    private fun verificarPermissaoEEnviar() {
        // Para WhatsApp, não precisamos de permissões especiais
        // O WhatsApp pode ser aberto via Intent sem permissões
        enviarContratoViaWhatsApp()
    }
    
    private fun enviarContratoViaWhatsApp() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val contrato = viewModel.contrato.value ?: return@launch
                
                // Gerar PDF do contrato
                val pdfGenerator = com.example.gestaobilhares.utils.ContractPdfGenerator(requireContext())
                val mesas = viewModel.getMesasVinculadas()
                val pdfFile = pdfGenerator.generateContractPdf(contrato, mesas)
                
                // Verificar se o arquivo foi criado corretamente
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    Toast.makeText(requireContext(), "Erro: Arquivo PDF não foi gerado corretamente", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // Preparar dados para envio
                val message = "Contrato de locação ${contrato.numeroContrato} assinado com sucesso!"
                
                // Criar URI usando FileProvider para compartilhamento seguro
                val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    pdfFile
                )
                
                // Criar Intent para WhatsApp
                val whatsappIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    setPackage("com.whatsapp")
                    putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                    putExtra(android.content.Intent.EXTRA_TEXT, message)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // Verificar se WhatsApp está instalado
                if (whatsappIntent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(whatsappIntent)
                    Toast.makeText(requireContext(), "Abrindo WhatsApp...", Toast.LENGTH_SHORT).show()
                } else {
                    // Se WhatsApp não estiver instalado, usar Intent genérico
                    val genericIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                        putExtra(android.content.Intent.EXTRA_TEXT, message)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(android.content.Intent.createChooser(genericIntent, "Enviar contrato via"))
                }
                
                // Navegar de volta após um delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    findNavController().popBackStack()
                }, 2000)
                
            } catch (e: Exception) {
                android.util.Log.e("SignatureCaptureFragment", "Erro ao enviar contrato", e)
                Toast.makeText(requireContext(), "Erro ao enviar contrato: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
