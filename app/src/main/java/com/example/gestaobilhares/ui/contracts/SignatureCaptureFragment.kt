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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
class SignatureCaptureFragment : Fragment() {
    
    companion object {
        private const val TAG = "SignatureCaptureFragment"
    }
    
    private var _binding: FragmentSignatureCaptureBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SignatureCaptureViewModel by viewModels()
    
    lateinit var legalLogger: LegalLogger
    
    lateinit var documentIntegrityManager: DocumentIntegrityManager
    
    lateinit var metadataCollector: SignatureMetadataCollector
    
    private var signatureBitmap: Bitmap? = null
    private var assinaturaContexto: String? = null // null|"CONTRATO"|"DISTRATO"
    
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
        assinaturaContexto = arguments?.getString("assinatura_contexto")
        
        viewModel.carregarContrato(contratoId)
        setupObservers()
        setupSignatureView()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contrato.collect { contrato ->
                contrato?.let {
                    val titulo = if (assinaturaContexto == "DISTRATO") "Assinatura do Distrato" else "Assinatura do Contrato"
                    binding.tvContratoInfo.text = "$titulo\nContrato: ${it.numeroContrato}\nLocatário: ${it.locatarioNome}"
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
        binding.signatureView.setBackgroundColor(Color.WHITE)
    }
    
    private fun setupClickListeners() {
        binding.apply {
            btnLimparAssinatura.setOnClickListener { limparAssinatura() }
            btnSalvarAssinatura.setOnClickListener { salvarAssinatura() }
            btnEnviarWhatsApp.setOnClickListener { verificarPermissaoEEnviar() }
        }
    }
    
    private fun limparAssinatura() {
        binding.signatureView.clear()
        signatureBitmap = null
    }
    
    private fun salvarAssinatura() {
        if (!binding.signatureView.hasSignature()) {
            Toast.makeText(requireContext(), "Por favor, assine", Toast.LENGTH_SHORT).show()
            return
        }
        
        val statistics = binding.signatureView.getSignatureStatistics()
        Log.d(TAG, "Assinatura válida: ${statistics.isValidSignature()}")
        if (!statistics.isValidSignature()) {
            Toast.makeText(requireContext(), "Assinatura muito simples. Por favor, assine novamente.", Toast.LENGTH_LONG).show()
            return
        }
        
        signatureBitmap = binding.signatureView.getSignatureBitmap()
        val signatureHash = documentIntegrityManager.generateSignatureHash(signatureBitmap!!)
        val contrato = viewModel.contrato.value
        if (contrato != null) {
            val documentHash = documentIntegrityManager.generateDocumentHash(ByteArray(0))
            val metadata = metadataCollector.collectSignatureMetadata(documentHash, signatureHash)
            viewLifecycleOwner.lifecycleScope.launch {
                legalLogger.logSignatureEvent(
                    contratoId = contrato.id,
                    userId = contrato.clienteId.toString(),
                    action = if (assinaturaContexto == "DISTRATO") "ASSINATURA_DISTRATO" else "ASSINATURA_CONTRATO",
                    metadata = metadata
                )
            }
        }
        
        val assinaturaBase64 = bitmapToBase64(signatureBitmap!!)
        if (assinaturaContexto == "DISTRATO") {
            viewModel.salvarAssinaturaDistrato(assinaturaBase64)
            
            // ✅ CORREÇÃO CRÍTICA: Atualizar status do contrato SEMPRE que distrato for assinado
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val contrato = viewModel.contrato.value ?: return@launch
                    val fechamento = viewModel.getFechamentoResumoDistrato()
                    
                    val db = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(requireContext())
                    val repo = com.example.gestaobilhares.data.repository.AppRepository(
                        db.clienteDao(),
                        db.acertoDao(),
                        db.mesaDao(),
                        db.rotaDao(),
                        db.despesaDao(),
                        db.colaboradorDao(),
                        db.cicloAcertoDao(),
                        db.acertoMesaDao(),
                        db.contratoLocacaoDao(),
                        db.aditivoContratoDao(),
                        db.assinaturaRepresentanteLegalDao(),
                        db.logAuditoriaAssinaturaDao()
                    )
                    val novoStatus = if (fechamento.saldoApurado > 0.0) "RESCINDIDO_COM_DIVIDA" else "ENCERRADO_QUITADO"
                    val agora = java.util.Date()
                    android.util.Log.d("DistratoFlow", "✅ ATUALIZAR STATUS ao salvar assinatura: contrato ${contrato.id} para $novoStatus em $agora")
                    repo.encerrarContrato(contrato.id, contrato.clienteId, novoStatus)
                    
                    // Verificação imediata
                    try {
                        val apos = repo.buscarContratosPorCliente(contrato.clienteId).first()
                        val resumo = apos.joinToString { c -> "id=${'$'}{c.id},status=${'$'}{c.status},enc=${'$'}{c.dataEncerramento}" }
                        android.util.Log.d("DistratoFlow", "✅ Após salvar assinatura distrato: ${'$'}resumo")
                    } catch (e: Exception) {
                        android.util.Log.e("DistratoFlow", "Falha verificação pós-assinatura distrato", e)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DistratoFlow", "Falha ao atualizar status na assinatura do distrato", e)
                }
            }
        } else {
            viewModel.salvarAssinatura(assinaturaBase64)
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    private fun verificarPermissaoEEnviar() {
        if (assinaturaContexto == "DISTRATO") enviarDistratoViaWhatsApp() else enviarContratoViaWhatsApp()
    }
    
    private fun enviarContratoViaWhatsApp() {
        // ✅ LOGS PARA MONITORAR ESTADO ANTES DO ENVIO
        android.util.Log.d("SignatureCaptureFragment", "=== INÍCIO ENVIO CONTRATO VIA WHATSAPP ===")
        android.util.Log.d("SignatureCaptureFragment", "Timestamp: ${System.currentTimeMillis()}")
        android.util.Log.d("SignatureCaptureFragment", "Fragment ativo: isAdded=$isAdded, isDetached=$isDetached, isRemoving=$isRemoving")
        
        val contratoAtual = viewModel.contrato.value
        android.util.Log.d("SignatureCaptureFragment", "📋 ESTADO DO CONTRATO ANTES DO ENVIO:")
        android.util.Log.d("SignatureCaptureFragment", "  - Contrato: $contratoAtual")
        android.util.Log.d("SignatureCaptureFragment", "  - Cliente ID: ${contratoAtual?.clienteId}")
        android.util.Log.d("SignatureCaptureFragment", "  - Número: ${contratoAtual?.numeroContrato}")
        android.util.Log.d("SignatureCaptureFragment", "  - Contexto: $assinaturaContexto")
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val contrato = viewModel.contrato.value ?: return@launch
                val pdfGenerator = com.example.gestaobilhares.utils.ContractPdfGenerator(requireContext())
                val mesas = viewModel.getMesasVinculadas()
                
                // ✅ NOVO: Obter assinatura do representante legal automaticamente
                val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
                
                val pdfFile = pdfGenerator.generateContractPdf(contrato, mesas, assinaturaRepresentante)
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    Toast.makeText(requireContext(), "Erro: Arquivo PDF não foi gerado corretamente", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val message = "Contrato de locação ${contrato.numeroContrato} assinado com sucesso!"
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
                    clipData = android.content.ClipData.newUri(requireContext().contentResolver, "Contrato", pdfUri)
                }
                startActivity(android.content.Intent.createChooser(intent, "Enviar contrato via"))
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ 
                    // ✅ LOGS DETALHADOS PARA DIAGNÓSTICO
                    android.util.Log.d("SignatureCaptureFragment", "=== INÍCIO NAVEGAÇÃO APÓS ENVIO CONTRATO ===")
                    android.util.Log.d("SignatureCaptureFragment", "Timestamp: ${System.currentTimeMillis()}")
                    android.util.Log.d("SignatureCaptureFragment", "Fragment ativo: isAdded=$isAdded, isDetached=$isDetached, isRemoving=$isRemoving")
                    
                    // Verificar se o Fragment ainda está ativo
                    if (!isAdded || isDetached || isRemoving) {
                        android.util.Log.w("SignatureCaptureFragment", "❌ Fragment não está mais ativo - cancelando navegação")
                        android.util.Log.w("SignatureCaptureFragment", "isAdded: $isAdded, isDetached: $isDetached, isRemoving: $isRemoving")
                        return@postDelayed
                    }
                    
                    // Navegar para a tela de detalhes do cliente em vez de voltar para geração de contrato
                    val clienteId = viewModel.contrato.value?.clienteId ?: 0L
                    val contratoNumero = viewModel.contrato.value?.numeroContrato
                    val contratoId = viewModel.contrato.value?.id
                    
                    android.util.Log.d("SignatureCaptureFragment", "📊 DADOS DO CONTRATO:")
                    android.util.Log.d("SignatureCaptureFragment", "  - clienteId: $clienteId")
                    android.util.Log.d("SignatureCaptureFragment", "  - contratoNumero: $contratoNumero")
                    android.util.Log.d("SignatureCaptureFragment", "  - contratoId: $contratoId")
                    android.util.Log.d("SignatureCaptureFragment", "  - assinaturaContexto: $assinaturaContexto")
                    
                    // ✅ CORREÇÃO SIMPLES: Navegar diretamente para ClientDetailFragment
                    // O ClientDetailFragment já tem lógica para voltar para a rota correta
                    try {
                        android.util.Log.d("SignatureCaptureFragment", "🚀 NAVEGANDO DIRETAMENTE PARA ClientDetailFragment")
                        
                        if (clienteId > 0) {
                            val bundle = android.os.Bundle().apply {
                                putLong("clienteId", clienteId)
                            }
                            android.util.Log.d("SignatureCaptureFragment", "📦 Navegando com bundle: $bundle")
                            
                            findNavController().navigate(
                                com.example.gestaobilhares.R.id.clientDetailFragment, 
                                bundle
                            )
                            android.util.Log.d("SignatureCaptureFragment", "✅ Navegação executada com sucesso!")
                        } else {
                            android.util.Log.w("SignatureCaptureFragment", "⚠️ ClienteId inválido: $clienteId")
                            findNavController().popBackStack()
                        }
                        
                    } catch (e: Exception) {
                        android.util.Log.e("SignatureCaptureFragment", "❌ Erro na navegação: ${e.message}", e)
                        findNavController().popBackStack()
                    }
                    
                    android.util.Log.d("SignatureCaptureFragment", "=== FIM NAVEGAÇÃO APÓS ENVIO CONTRATO ===")
                }, 2000)
            } catch (e: Exception) {
                android.util.Log.e("SignatureCaptureFragment", "Erro ao enviar contrato", e)
                Toast.makeText(requireContext(), "Erro ao enviar contrato: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun enviarDistratoViaWhatsApp() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val contrato = viewModel.contrato.value ?: return@launch
                val mesas = viewModel.getMesasParaDistrato()
                val fechamento = viewModel.getFechamentoResumoDistrato()
                // ✅ NOVO: Obter assinatura do representante legal automaticamente
                val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
                
                val pdf = com.example.gestaobilhares.utils.ContractPdfGenerator(requireContext())
                    .generateDistratoPdf(
                        contrato = contrato,
                        mesas = mesas,
                        fechamento = fechamento,
                        confissaoDivida = if (fechamento.saldoApurado > 0.0) Pair(fechamento.saldoApurado, java.util.Date()) else null,
                        assinaturaRepresentante = assinaturaRepresentante
                    )
                if (!pdf.exists() || pdf.length() == 0L) {
                    Toast.makeText(requireContext(), "Erro ao gerar o PDF do distrato.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // ✅ NOVO: Persistir status de encerramento do contrato
                try {
                    val db = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(requireContext())
                    val repo = com.example.gestaobilhares.data.repository.AppRepository(
                        db.clienteDao(),
                        db.acertoDao(),
                        db.mesaDao(),
                        db.rotaDao(),
                        db.despesaDao(),
                        db.colaboradorDao(),
                        db.cicloAcertoDao(),
                        db.acertoMesaDao(),
                        db.contratoLocacaoDao(),
                        db.aditivoContratoDao(),
                        db.assinaturaRepresentanteLegalDao(),
                        db.logAuditoriaAssinaturaDao()
                    )
                    val novoStatus = if (fechamento.saldoApurado > 0.0) "RESCINDIDO_COM_DIVIDA" else "ENCERRADO_QUITADO"
                    val agora = java.util.Date()
                    android.util.Log.d("DistratoFlow", "Encerrar direto contrato ${contrato.id} para $novoStatus em $agora")
                    repo.encerrarContrato(contrato.id, contrato.clienteId, novoStatus)
                    // Verificação imediata (diagnóstico)
                    try {
                        val apos = repo.buscarContratosPorCliente(contrato.clienteId).first()
                        val resumo = apos.joinToString { c -> "id=${'$'}{c.id},status=${'$'}{c.status},enc=${'$'}{c.dataEncerramento}" }
                        android.util.Log.d("DistratoFlow", "Após atualizar (SignatureCapture): ${'$'}resumo")
                    } catch (e: Exception) {
                        android.util.Log.e("DistratoFlow", "Falha verificação pós-atualização (SignatureCapture)", e)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DistratoFlow", "Falha ao atualizar contrato como encerrado", e)
                }

                val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    pdf
                )
                val message = "Distrato do contrato ${contrato.numeroContrato} assinado."
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                    putExtra(android.content.Intent.EXTRA_TEXT, message)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = android.content.ClipData.newUri(requireContext().contentResolver, "Distrato", pdfUri)
                }
                startActivity(android.content.Intent.createChooser(intent, "Enviar distrato via"))
                
                // ✅ CORREÇÃO SIMPLES: Navegar diretamente para ClientDetailFragment após envio do distrato
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ 
                    val clienteId = contrato.clienteId
                    if (clienteId > 0) {
                        val bundle = android.os.Bundle().apply {
                            putLong("clienteId", clienteId)
                        }
                        findNavController().navigate(
                            com.example.gestaobilhares.R.id.clientDetailFragment, 
                            bundle
                        )
                    } else {
                        findNavController().popBackStack()
                    }
                }, 2000)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Erro ao enviar distrato", e)
                Toast.makeText(requireContext(), "Erro ao enviar distrato: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




