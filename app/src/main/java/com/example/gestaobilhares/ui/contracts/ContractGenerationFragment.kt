package com.example.gestaobilhares.ui.contracts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.example.gestaobilhares.databinding.FragmentContractGenerationBinding
import com.example.gestaobilhares.utils.ContractPdfGenerator
// ✅ CORREÇÃO: Imports para Factory
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.launch

class ContractGenerationFragment : Fragment() {
    
    private var _binding: FragmentContractGenerationBinding? = null
    private val binding get() = _binding!!
    
    // ✅ CORREÇÃO: ViewModel com Factory customizada
    private lateinit var viewModel: ContractGenerationViewModel
    
    // ✅ PROTEÇÃO: Flag para evitar múltiplos cliques
    private var isGenerating = false
    
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
        _binding = FragmentContractGenerationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ LOG CRASH: Início da tela
        android.util.Log.d("LOG_CRASH", "ContractGenerationFragment.onViewCreated - INÍCIO")
        
        // ✅ CORREÇÃO: Inicializar ViewModel antes de usar
        try {
            // ✅ LOG CRASH: Inicializando ViewModel
            android.util.Log.d("LOG_CRASH", "ContractGenerationFragment.onViewCreated - Inicializando ViewModel")
            val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
            viewModel = ContractGenerationViewModel(appRepository)
            
            val clienteId = arguments?.getLong("cliente_id") ?: 0L
            val mesasVinculadas = arguments?.getLongArray("mesas_vinculadas")?.toList() ?: emptyList()
            val tipoFixo = arguments?.getBoolean("tipo_fixo") ?: false
            val valorFixo = arguments?.getDouble("valor_fixo") ?: 0.0
            
            // ✅ CORREÇÃO CRÍTICA: Validar clienteId antes de carregar (evita carregar cliente inexistente)
            if (clienteId != 0L) {
                viewModel.carregarDados(clienteId, mesasVinculadas, tipoFixo, valorFixo)
                setupObservers()
                setupClickListeners()
                preencherCamposAutomaticamente(tipoFixo, valorFixo)
            } else {
                // Mostrar erro se clienteId não foi fornecido
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Erro")
                    .setMessage("ID do cliente não fornecido. Não é possível carregar os dados.")
                    .setPositiveButton("OK") { _, _ ->
                        findNavController().popBackStack()
                    }
                    .show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ContractGenerationFragment", "Erro ao inicializar ViewModel: ${e.message}")
            // Mostrar erro para o usuário
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Erro")
                .setMessage("Erro ao inicializar tela de contrato. Tente novamente.")
                .setPositiveButton("OK") { _, _ ->
                    findNavController().popBackStack()
                }
                .show()
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cliente.collect { cliente ->
                cliente?.let {
                    binding.apply {
                        tvClienteNome.text = it.nome
                        tvClienteCpf.text = it.cpfCnpj
                        tvClienteEndereco.text = it.endereco
                        tvClienteTelefone.text = it.telefone
                        tvClienteEmail.text = it.email
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mesasVinculadas.collect { mesas ->
                binding.tvMesasVinculadas.text = mesas.joinToString(", ") { "Mesa ${it.numero} (${it.tipoMesa.name})" }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contrato.collect { contrato ->
                contrato?.let {
                    binding.tvNumeroContrato.text = it.numeroContrato
                    binding.btnAssinar.isEnabled = true
                    // ✅ Reabilitar botão quando contrato for gerado com sucesso
                    if (isGenerating) {
                        resetButton()
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                binding.btnGerarContrato.isEnabled = !loading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    // ✅ Reabilitar botão quando houver erro
                    if (isGenerating) {
                        resetButton()
                    }
                }
            }
        }
    }
    
    private fun preencherCamposAutomaticamente(tipoFixo: Boolean, valorFixo: Double) {
        binding.apply {
            // Preencher dia de vencimento com o dia atual
            val diaAtual = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            etDiaVencimento.setText(diaAtual.toString())
            
            // Configurar tipo de pagamento baseado no tipo de acerto
            if (tipoFixo) {
                rbValorFixo.isChecked = true
                etValorMensal.setText(valorFixo.toString())
                tilPercentualReceita.visibility = View.GONE
                etValorMensal.hint = "Valor Mensal (R$)"
                // Mostrar campos de valor fixo e dia de vencimento
                tilValorMensal.visibility = View.VISIBLE
                tilDiaVencimento.visibility = View.VISIBLE
            } else {
                rbPercentual.isChecked = true
                tilPercentualReceita.visibility = View.VISIBLE
                // Preencher percentual padrão de 40%
                etPercentualReceita.setText("40")
                // Para fichas jogadas, ocultar campos desnecessários
                tilValorMensal.visibility = View.GONE
                tilDiaVencimento.visibility = View.GONE
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.apply {
            btnGerarContrato.setOnClickListener {
                if (isGenerating) {
                    android.util.Log.d("ContractGeneration", "Tentativa de clique duplo bloqueada")
                    return@setOnClickListener
                }
                gerarContrato()
            }
            
            btnAssinar.setOnClickListener {
                abrirTelaAssinatura()
            }
            
            // Listener para mostrar/ocultar campos baseado no tipo de pagamento
            rgTipoPagamento.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.rbValorFixo -> {
                        tilPercentualReceita.visibility = View.GONE
                        tilValorMensal.visibility = View.VISIBLE
                        tilDiaVencimento.visibility = View.VISIBLE
                        etValorMensal.hint = "Valor Mensal (R$)"
                        // Preencher dia de vencimento com o dia atual se estiver vazio
                        if (etDiaVencimento.text.isNullOrEmpty()) {
                            val diaAtual = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
                            etDiaVencimento.setText(diaAtual.toString())
                        }
                    }
                    R.id.rbPercentual -> {
                        tilPercentualReceita.visibility = View.VISIBLE
                        tilValorMensal.visibility = View.GONE
                        tilDiaVencimento.visibility = View.GONE
                        etValorMensal.hint = "Valor Base (R$)"
                    }
                }
            }
        }
    }
    
    private fun gerarContrato() {
        // ✅ LOG CRASH: Início da geração de contrato
        android.util.Log.d("LOG_CRASH", "ContractGenerationFragment.gerarContrato - INÍCIO")
        
        if (isGenerating) {
            android.util.Log.d("LOG_CRASH", "ContractGenerationFragment.gerarContrato - Geração já em andamento, ignorando clique")
            android.util.Log.d("ContractGeneration", "Geração já em andamento, ignorando clique")
            return
        }
        
        isGenerating = true
        binding.btnGerarContrato.isEnabled = false
        binding.btnGerarContrato.text = "Gerando..."
        android.util.Log.d("LOG_CRASH", "ContractGenerationFragment.gerarContrato - Botão desabilitado, iniciando geração")
        android.util.Log.d("ContractGeneration", "Iniciando geração de contrato")
        
        val tipoPagamento = if (binding.rbValorFixo.isChecked) "FIXO" else "PERCENTUAL"
        
        // Valores padrão para fichas jogadas
        var valorMensal = 0.0
        var diaVencimento = 1
        
        // Validações específicas para valor fixo
        if (tipoPagamento == "FIXO") {
            valorMensal = binding.etValorMensal.text.toString().toDoubleOrNull() ?: 0.0
            diaVencimento = binding.etDiaVencimento.text.toString().toIntOrNull() ?: 1
            
            if (valorMensal <= 0) {
                Toast.makeText(requireContext(), "Valor deve ser maior que zero", Toast.LENGTH_SHORT).show()
                resetButton()
                return
            }
            
            if (diaVencimento < 1 || diaVencimento > 31) {
                Toast.makeText(requireContext(), "Dia de vencimento deve estar entre 1 e 31", Toast.LENGTH_SHORT).show()
                resetButton()
                return
            }
        }
        
        // Validação para percentual (fichas jogadas)
        val percentualReceita = if (tipoPagamento == "PERCENTUAL") {
            binding.etPercentualReceita.text.toString().toDoubleOrNull()
        } else null
        
        if (tipoPagamento == "PERCENTUAL" && (percentualReceita == null || percentualReceita <= 0 || percentualReceita > 100)) {
            Toast.makeText(requireContext(), "Percentual deve estar entre 1 e 100", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }
        
        // ✅ LOG CRASH: Chamando ViewModel para gerar contrato
        android.util.Log.d("LOG_CRASH", "ContractGenerationFragment.gerarContrato - Chamando ViewModel para gerar contrato")
        
        // Gerar contrato - a reabilitação do botão será feita pelo observer do contrato
        try {
            viewModel.gerarContrato(valorMensal, diaVencimento, tipoPagamento, percentualReceita)
            android.util.Log.d("LOG_CRASH", "ContractGenerationFragment.gerarContrato - ViewModel chamado com sucesso")
        } catch (e: Exception) {
            android.util.Log.e("LOG_CRASH", "ContractGenerationFragment.gerarContrato - ERRO ao chamar ViewModel: ${e.message}", e)
            resetButton()
        }
    }
    
    private fun resetButton() {
        isGenerating = false
        binding.btnGerarContrato.isEnabled = true
        binding.btnGerarContrato.text = "Gerar Contrato"
        android.util.Log.d("ContractGeneration", "Botão reabilitado")
    }
    
    private fun abrirTelaAssinatura() {
        val contrato = viewModel.contrato.value ?: return
        
        val bundle = Bundle().apply {
            putLong("contrato_id", contrato.id)
        }
        findNavController().navigate(com.example.gestaobilhares.R.id.signatureCaptureFragment, bundle)
    }
    
    private fun enviarContratoViaWhatsApp() {
        val contrato = viewModel.contrato.value ?: return
        // ✅ FASE 12.7: Variável removida (não utilizada)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Gerar PDF do contrato
                val pdfGenerator = ContractPdfGenerator(requireContext())
                
                // ✅ NOVO: Obter assinatura do representante legal automaticamente
                val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
                
                // ✅ FASE 12.5: generateContractPdf agora retorna Pair<File, String?> (hash)
                val (_, documentoHash) = pdfGenerator.generateContractPdf(contrato, viewModel.mesasVinculadas.value, assinaturaRepresentante)
                
                // ✅ FASE 12.5: Salvar hash de forma assíncrona (removido runBlocking)
                documentoHash?.let { hash ->
                    pdfGenerator.salvarHashDocumento(contrato, hash)
                }
                
                // ✅ FASE 12.7: Envio via WhatsApp será implementado quando necessário
                // TODO: Implementar envio via WhatsApp quando funcionalidade for solicitada
                
                Toast.makeText(requireContext(), "Contrato enviado via WhatsApp!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao enviar contrato: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

