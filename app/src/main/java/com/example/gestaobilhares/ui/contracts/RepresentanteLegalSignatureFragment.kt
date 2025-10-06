package com.example.gestaobilhares.ui.contracts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
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
import com.example.gestaobilhares.databinding.FragmentRepresentanteLegalSignatureBinding
import com.example.gestaobilhares.ui.contracts.SignatureView
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Fragment para captura da assinatura digital do representante legal
 * Implementa todos os requisitos de segurança da Cláusula 9.3 do contrato
 */
class RepresentanteLegalSignatureFragment : Fragment() {
    
    private var _binding: FragmentRepresentanteLegalSignatureBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RepresentanteLegalSignatureViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permissão concedida, continuar com a captura
        } else {
            Toast.makeText(requireContext(), "Permissão necessária para capturar assinatura", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRepresentanteLegalSignatureBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        checkPermissions()
    }
    
    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Configurar botões
        binding.btnLimparAssinatura.setOnClickListener {
            binding.signatureView.clear()
        }
        
        binding.btnSalvarAssinatura.setOnClickListener {
            salvarAssinatura()
        }
        
        binding.btnValidarJuridicamente.setOnClickListener {
            validarJuridicamente()
        }
        
        binding.btnDesativarAssinatura.setOnClickListener {
            desativarAssinatura()
        }
        
        // Configurar campos de entrada
        binding.etNomeRepresentante.setText("BILHAR GLOBO R & A LTDA")
        binding.etCargoRepresentante.setText("Sócio-Administrador")
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assinaturaAtiva.collect { assinatura ->
                if (assinatura != null) {
                    binding.layoutAssinaturaExistente.visibility = View.VISIBLE
                    binding.layoutCapturaAssinatura.visibility = View.GONE
                    
                    binding.tvNomeRepresentante.text = assinatura.nomeRepresentante
                    binding.tvCpfRepresentante.text = assinatura.cpfRepresentante
                    binding.tvCargoRepresentante.text = assinatura.cargoRepresentante
                    binding.tvDataCriacao.text = "Criada em: ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", assinatura.dataCriacao)}"
                    binding.tvTotalUsos.text = "Total de usos: ${assinatura.totalUsos}"
                    binding.tvValidadaJuridicamente.text = if (assinatura.validadaJuridicamente) "✅ Validada juridicamente" else "❌ Não validada"
                    
                    // Mostrar/ocultar botões baseado no status
                    binding.btnValidarJuridicamente.visibility = if (assinatura.validadaJuridicamente) View.GONE else View.VISIBLE
                    binding.btnDesativarAssinatura.visibility = View.VISIBLE
                } else {
                    binding.layoutAssinaturaExistente.visibility = View.GONE
                    binding.layoutCapturaAssinatura.visibility = View.VISIBLE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                binding.btnSalvarAssinatura.isEnabled = !loading
                binding.btnValidarJuridicamente.isEnabled = !loading
                binding.btnDesativarAssinatura.isEnabled = !loading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.limparMensagens()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.success.collect { success ->
                success?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.limparMensagens()
                }
            }
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    private fun salvarAssinatura() {
        // Validar campos obrigatórios
        val nomeRepresentante = binding.etNomeRepresentante.text.toString().trim()
        val cpfRepresentante = binding.etCpfRepresentante.text.toString().trim()
        val cargoRepresentante = binding.etCargoRepresentante.text.toString().trim()
        
        if (nomeRepresentante.isEmpty()) {
            binding.etNomeRepresentante.error = "Nome é obrigatório"
            return
        }
        
        if (cpfRepresentante.isEmpty()) {
            binding.etCpfRepresentante.error = "CPF é obrigatório"
            return
        }
        
        if (cargoRepresentante.isEmpty()) {
            binding.etCargoRepresentante.error = "Cargo é obrigatório"
            return
        }
        
        // Verificar se há assinatura capturada
        if (!binding.signatureView.hasSignature()) {
            Toast.makeText(requireContext(), "Por favor, capture a assinatura", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Capturar assinatura em Base64
        val assinaturaBase64 = binding.signatureView.getSignatureBase64()
        
        // Obter metadados de segurança
        val deviceId = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        val versaoSistema = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        
        // Criar poderes delegados (JSON)
        val poderesDelegados = JSONObject().apply {
            put("locar_mesas", true)
            put("encerrar_contratos", true)
            put("receber_pagamentos", true)
            put("assinar_contratos", true)
            put("assinar_aditivos", true)
            put("assinar_distratos", true)
            put("gerenciar_acertos", true)
            put("aprovar_despesas", true)
        }.toString()
        
        // Gerar número da procuração
        val numeroProcuração = "PROC-${System.currentTimeMillis()}"
        
        // Salvar assinatura
        viewModel.salvarAssinaturaRepresentante(
            nomeRepresentante = nomeRepresentante,
            cpfRepresentante = cpfRepresentante,
            cargoRepresentante = cargoRepresentante,
            assinaturaBase64 = assinaturaBase64,
            deviceId = deviceId,
            versaoSistema = versaoSistema,
            criadoPor = "ADMIN", // TODO: Obter do usuário logado
            numeroProcuração = numeroProcuração,
            poderesDelegados = poderesDelegados
        )
    }
    
    private fun validarJuridicamente() {
        // TODO: Implementar validação jurídica (pode ser um diálogo para inserir dados do advogado)
        viewModel.validarAssinaturaJuridicamente("Advogado Responsável")
    }
    
    private fun desativarAssinatura() {
        // TODO: Implementar confirmação antes de desativar
        viewModel.desativarAssinatura()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

