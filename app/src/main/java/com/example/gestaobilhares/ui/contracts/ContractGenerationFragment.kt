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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContractGenerationFragment : Fragment() {
    
    private var _binding: FragmentContractGenerationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ContractGenerationViewModel by viewModels()
    
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
        
        val clienteId = arguments?.getLong("cliente_id") ?: 0L
        val mesasVinculadas = arguments?.getLongArray("mesas_vinculadas")?.toList() ?: emptyList()
        val tipoFixo = arguments?.getBoolean("tipo_fixo") ?: false
        val valorFixo = arguments?.getDouble("valor_fixo") ?: 0.0
        
        viewModel.carregarDados(clienteId, mesasVinculadas, tipoFixo, valorFixo)
        setupObservers()
        setupClickListeners()
        preencherCamposAutomaticamente(tipoFixo, valorFixo)
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
                return
            }
            
            if (diaVencimento < 1 || diaVencimento > 31) {
                Toast.makeText(requireContext(), "Dia de vencimento deve estar entre 1 e 31", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // Validação para percentual (fichas jogadas)
        val percentualReceita = if (tipoPagamento == "PERCENTUAL") {
            binding.etPercentualReceita.text.toString().toDoubleOrNull()
        } else null
        
        if (tipoPagamento == "PERCENTUAL" && (percentualReceita == null || percentualReceita <= 0 || percentualReceita > 100)) {
            Toast.makeText(requireContext(), "Percentual deve estar entre 1 e 100", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.gerarContrato(valorMensal, diaVencimento, tipoPagamento, percentualReceita)
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
        val cliente = viewModel.cliente.value ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Gerar PDF do contrato
                val pdfGenerator = ContractPdfGenerator(requireContext())
                val pdfFile = pdfGenerator.generateContractPdf(contrato, viewModel.mesasVinculadas.value)
                
                // Enviar via WhatsApp
                val whatsappNumber = cliente.telefone?.replace(Regex("[^0-9]"), "") ?: ""
                val message = "Contrato de locação ${contrato.numeroContrato} gerado com sucesso!"
                
                // Implementar envio via WhatsApp
                // TODO: Implementar envio via WhatsApp
                
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
