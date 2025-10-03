package com.example.gestaobilhares.ui.contracts

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentAditivoSignatureBinding
import com.example.gestaobilhares.utils.DocumentIntegrityManager
import com.example.gestaobilhares.utils.LegalLogger
import com.example.gestaobilhares.utils.SignatureMetadataCollector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class AditivoSignatureFragment : Fragment() {
    
    companion object {
        private const val TAG = "AditivoSignatureFragment"
    }
    
    private var _binding: FragmentAditivoSignatureBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AditivoSignatureViewModel by viewModels()
    
    private var contratoId: Long = 0L
    
    @Inject
    lateinit var legalLogger: LegalLogger
    
    @Inject
    lateinit var documentIntegrityManager: DocumentIntegrityManager
    
    @Inject
    lateinit var metadataCollector: SignatureMetadataCollector
    
    // Assinatura será capturada diretamente do SignatureView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAditivoSignatureBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
        observeViewModel()
        
        // Carregar dados do aditivo
        contratoId = arguments?.getLong("contratoId") ?: 0L
        val mesasIds = arguments?.getLongArray("mesasVinculadas") ?: longArrayOf()
        val tipo = arguments?.getString("aditivoTipo")
        tipo?.let { viewModel.setAditivoTipo(it) }
        
        if (contratoId != 0L && mesasIds.isNotEmpty()) {
            viewModel.carregarDados(contratoId, mesasIds)
        }
    }
    
    private fun setupUI() {
        binding.tvTitle.text = "Assinatura do Aditivo"
        binding.tvInstructions.text = "Assine na área abaixo para confirmar o aditivo contratual"
        // Mesmas configs da tela de contrato
        binding.signatureCanvas.setBackgroundColor(android.graphics.Color.WHITE)
    }
    
    private fun setupClickListeners() {
        binding.btnClearSignature.setOnClickListener {
            clearSignature()
        }
        
        binding.btnConfirmSignature.setOnClickListener {
            confirmSignature()
        }
        
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEnviarWhatsApp.setOnClickListener {
            enviarAditivoViaWhatsApp()
        }
    }
    
    private fun clearSignature() {
        binding.signatureCanvas.clear()
    }
    
    private fun confirmSignature() {
        if (isSignatureEmpty()) {
            Toast.makeText(requireContext(), "Por favor, assine o documento", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Converter assinatura para Base64
                val bitmap = binding.signatureCanvas.getSignatureBitmap()
                if (bitmap == null) {
                    Toast.makeText(requireContext(), "Assinatura inválida, tente novamente", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val signatureBase64 = bitmapToBase64(bitmap)
                
                // Coletar metadados da assinatura
                val documentHash = documentIntegrityManager.generateDocumentHash(signatureBase64.toByteArray())
                val signatureHash = documentIntegrityManager.generateDocumentHash(signatureBase64.toByteArray())
                val metadata = metadataCollector.collectSignatureMetadata(
                    documentHash,
                    signatureHash
                )
                
                // Log jurídico da assinatura
                legalLogger.logSignatureEvent(
                    contratoId = contratoId,
                    userId = "SISTEMA",
                    action = "ASSINATURA_ADITIVO",
                    metadata = metadata
                )
                
                // Gerar aditivo primeiro
                viewModel.gerarAditivo()
                
                // Salvar assinatura no ViewModel
                viewModel.adicionarAssinaturaLocatario(signatureBase64)
                
                Toast.makeText(requireContext(), "Assinatura confirmada com sucesso!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao confirmar assinatura", e)
                Toast.makeText(requireContext(), "Erro ao confirmar assinatura: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun isSignatureEmpty(): Boolean {
        return !binding.signatureCanvas.hasSignature()
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    private fun observeViewModel() {
        // ✅ LOGS PARA MONITORAR CARREGAMENTO DO CONTRATO
        viewModel.contrato.observe(viewLifecycleOwner) { contrato ->
            contrato?.let {
                android.util.Log.d("AditivoSignatureFragment", "📋 CONTRATO CARREGADO:")
                android.util.Log.d("AditivoSignatureFragment", "  - ID: ${it.id}")
                android.util.Log.d("AditivoSignatureFragment", "  - Número: ${it.numeroContrato}")
                android.util.Log.d("AditivoSignatureFragment", "  - Cliente ID: ${it.clienteId}")
                android.util.Log.d("AditivoSignatureFragment", "  - Status: ${it.status}")
            } ?: run {
                android.util.Log.w("AditivoSignatureFragment", "⚠️ Contrato é null no observer")
            }
        }
        
        viewModel.aditivo.observe(viewLifecycleOwner) { aditivo ->
            // Habilitar botão assim que existir aditivo; idealmente após assinatura
            binding.btnEnviarWhatsApp.isEnabled = aditivo?.assinaturaLocatario != null
            if (aditivo?.assinaturaLocatario != null) {
                android.util.Log.d("AditivoSignatureFragment", "✅ Aditivo assinado - botão habilitado")
                Toast.makeText(requireContext(), "Aditivo assinado. Você pode enviar pelo WhatsApp.", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                android.util.Log.e("AditivoSignatureFragment", "❌ Erro no ViewModel: $error")
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Nada a inicializar; SignatureView gerencia seu próprio desenho
    }

    private fun enviarAditivoViaWhatsApp() {
        // ✅ LOGS PARA MONITORAR ESTADO ANTES DO ENVIO
        android.util.Log.d("AditivoSignatureFragment", "=== INÍCIO ENVIO ADITIVO VIA WHATSAPP ===")
        android.util.Log.d("AditivoSignatureFragment", "Timestamp: ${System.currentTimeMillis()}")
        android.util.Log.d("AditivoSignatureFragment", "Fragment ativo: isAdded=$isAdded, isDetached=$isDetached, isRemoving=$isRemoving")
        
        val contratoAtual = viewModel.contrato.value
        android.util.Log.d("AditivoSignatureFragment", "📋 ESTADO DO CONTRATO ANTES DO ENVIO:")
        android.util.Log.d("AditivoSignatureFragment", "  - Contrato: $contratoAtual")
        android.util.Log.d("AditivoSignatureFragment", "  - Cliente ID: ${contratoAtual?.clienteId}")
        android.util.Log.d("AditivoSignatureFragment", "  - Número: ${contratoAtual?.numeroContrato}")
        
        lifecycleScope.launch {
            try {
                val aditivo = viewModel.aditivo.value
                val contrato = viewModel.contrato.value
                val mesas = viewModel.mesas.value
                if (aditivo == null || contrato == null || mesas == null) {
                    Toast.makeText(requireContext(), "Aditivo ainda não disponível para envio.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val pdfGenerator = com.example.gestaobilhares.utils.AditivoPdfGenerator(requireContext())
                
                // ✅ NOVO: Obter assinatura do representante legal automaticamente
                val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
                
                val pdfFile = pdfGenerator.generateAditivoPdf(aditivo, contrato, mesas, assinaturaRepresentante)
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    Toast.makeText(requireContext(), "Erro ao gerar o PDF do aditivo.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val message = "Aditivo contratual ${aditivo.numeroAditivo} assinado com sucesso!"
                val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    pdfFile
                )
                // ✅ CORREÇÃO: Usar sempre seletor como no distrato (que funciona)
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                    putExtra(android.content.Intent.EXTRA_TEXT, message)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = android.content.ClipData.newUri(requireContext().contentResolver, "Aditivo", pdfUri)
                }
                startActivity(android.content.Intent.createChooser(intent, "Enviar aditivo via"))
                
                // ✅ NOVO: Navegar para tela de detalhes do cliente após envio do aditivo
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ 
                    // ✅ LOGS DETALHADOS PARA DIAGNÓSTICO
                    android.util.Log.d("AditivoSignatureFragment", "=== INÍCIO NAVEGAÇÃO APÓS ENVIO ADITIVO ===")
                    android.util.Log.d("AditivoSignatureFragment", "Timestamp: ${System.currentTimeMillis()}")
                    android.util.Log.d("AditivoSignatureFragment", "Fragment ativo: isAdded=$isAdded, isDetached=$isDetached, isRemoving=$isRemoving")
                    
                    // Verificar se o Fragment ainda está ativo
                    if (!isAdded || isDetached || isRemoving) {
                        android.util.Log.w("AditivoSignatureFragment", "❌ Fragment não está mais ativo - cancelando navegação")
                        android.util.Log.w("AditivoSignatureFragment", "isAdded: $isAdded, isDetached: $isDetached, isRemoving: $isRemoving")
                        return@postDelayed
                    }
                    
                    // ✅ CORREÇÃO: Obter clienteId de múltiplas fontes para garantir robustez
                    var clienteId = viewModel.contrato.value?.clienteId ?: 0L
                    val contratoNumero = viewModel.contrato.value?.numeroContrato
                    val contratoId = viewModel.contrato.value?.id
                    
                    // ✅ FALLBACK: Se clienteId for 0, tentar obter dos argumentos ou do aditivo
                    if (clienteId == 0L) {
                        // Tentar obter do contratoId se disponível
                        if (contratoId != null && contratoId > 0L) {
                            try {
                                // ✅ CORREÇÃO: Usar lifecycleScope para chamada suspensa
                                lifecycleScope.launch {
                                    val db = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(requireContext())
                                    val repo = com.example.gestaobilhares.data.repository.AppRepository(
                                        db.clienteDao(), db.acertoDao(), db.mesaDao(), db.rotaDao(), db.despesaDao(),
                                        db.colaboradorDao(), db.cicloAcertoDao(), db.acertoMesaDao(), db.contratoLocacaoDao(), db.aditivoContratoDao(),
                                        db.assinaturaRepresentanteLegalDao(), db.logAuditoriaAssinaturaDao(), db.procuraçãoRepresentanteDao()
                                    )
                                    val contratoCompleto = repo.buscarContratoPorId(contratoId)
                                    val novoClienteId = contratoCompleto?.clienteId ?: 0L
                                    android.util.Log.d("AditivoSignatureFragment", "✅ ClienteId obtido do banco: $novoClienteId")
                                    
                                    // Se conseguiu obter o clienteId, tentar navegação novamente
                                    if (novoClienteId > 0L) {
                                        try {
                                            val bundle = android.os.Bundle().apply {
                                                putLong("clienteId", novoClienteId)
                                            }
                                            android.util.Log.d("AditivoSignatureFragment", "📦 Tentando navegação com bundle após fallback: $bundle")
                                            findNavController().navigate(
                                                com.example.gestaobilhares.R.id.clientDetailFragment, 
                                                bundle
                                            )
                                            android.util.Log.d("AditivoSignatureFragment", "✅ Navegação com bundle após fallback executada com sucesso!")
                                            return@launch
                                        } catch (e: Exception) {
                                            android.util.Log.w("AditivoSignatureFragment", "⚠️ Navegação com bundle após fallback falhou: ${e.message}")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("AditivoSignatureFragment", "⚠️ Erro ao obter clienteId do banco: ${e.message}")
                            }
                        }
                    }
                    
                    android.util.Log.d("AditivoSignatureFragment", "📊 DADOS DO CONTRATO/ADITIVO:")
                    android.util.Log.d("AditivoSignatureFragment", "  - clienteId: $clienteId")
                    android.util.Log.d("AditivoSignatureFragment", "  - contratoNumero: $contratoNumero")
                    android.util.Log.d("AditivoSignatureFragment", "  - contratoId: $contratoId")
                    
                    // ✅ CORREÇÃO SIMPLES: Navegar diretamente para ClientDetailFragment
                    // O ClientDetailFragment já tem lógica para voltar para a rota correta
                    try {
                        android.util.Log.d("AditivoSignatureFragment", "🚀 NAVEGANDO DIRETAMENTE PARA ClientDetailFragment")
                        
                        if (clienteId > 0) {
                            val bundle = android.os.Bundle().apply {
                                putLong("clienteId", clienteId)
                            }
                            android.util.Log.d("AditivoSignatureFragment", "📦 Navegando com bundle: $bundle")
                            
                            findNavController().navigate(
                                com.example.gestaobilhares.R.id.clientDetailFragment, 
                                bundle
                            )
                            android.util.Log.d("AditivoSignatureFragment", "✅ Navegação executada com sucesso!")
                        } else {
                            android.util.Log.w("AditivoSignatureFragment", "⚠️ ClienteId inválido: $clienteId")
                            findNavController().popBackStack()
                        }
                        
                    } catch (e: Exception) {
                        android.util.Log.e("AditivoSignatureFragment", "❌ Erro na navegação: ${e.message}", e)
                        findNavController().popBackStack()
                    }
                    
                    android.util.Log.d("AditivoSignatureFragment", "=== FIM NAVEGAÇÃO APÓS ENVIO ADITIVO ===")
                }, 2000)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao enviar aditivo", e)
                Toast.makeText(requireContext(), "Erro ao enviar aditivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
