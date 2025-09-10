package com.example.gestaobilhares.ui.contracts

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.DialogContractFinalizationBinding
import com.example.gestaobilhares.ui.mesas.MesasDepositoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContractFinalizationDialog : DialogFragment() {
    
    private var _binding: DialogContractFinalizationBinding? = null
    private val binding get() = _binding!!
    
    private val mesasViewModel: MesasDepositoViewModel by activityViewModels()
    
    companion object {
        private const val ARG_CLIENTE_ID = "cliente_id"
        private const val ARG_MESAS_VINCULADAS = "mesas_vinculadas"
        
        fun newInstance(clienteId: Long, mesasVinculadas: List<Long>): ContractFinalizationDialog {
            return ContractFinalizationDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_CLIENTE_ID, clienteId)
                    putLongArray(ARG_MESAS_VINCULADAS, mesasVinculadas.toLongArray())
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogContractFinalizationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        binding.apply {
            tvDialogTitle.text = "Finalizar Locação"
            tvDialogMessage.text = "Para finalizar a locação é necessário gerar o contrato de locação."
            
            btnAddAnotherMesa.text = "Adicionar outra mesa"
            btnGenerateContract.text = "Gerar contrato"
        }
    }
    
    private fun setupClickListeners() {
        binding.apply {
            btnAddAnotherMesa.setOnClickListener {
                val clienteId = arguments?.getLong(ARG_CLIENTE_ID) ?: return@setOnClickListener
                
                // Voltar para a tela de depósito de mesas com os argumentos corretos
                val bundle = Bundle().apply {
                    putLong("clienteId", clienteId)
                    putBoolean("isFromClientRegister", true)
                    putBoolean("isFromGerenciarMesas", false)
                }
                dismiss()
                findNavController().navigate(com.example.gestaobilhares.R.id.mesasDepositoFragment, bundle)
            }
            
            btnGenerateContract.setOnClickListener {
                val clienteId = arguments?.getLong(ARG_CLIENTE_ID) ?: return@setOnClickListener
                val mesasVinculadas = arguments?.getLongArray(ARG_MESAS_VINCULADAS)?.toList() ?: emptyList()
                
                // Navegar para a tela de geração de contrato
                val bundle = Bundle().apply {
                    putLong("cliente_id", clienteId)
                    putLongArray("mesas_vinculadas", mesasVinculadas.toLongArray())
                }
                findNavController().navigate(com.example.gestaobilhares.R.id.contractGenerationFragment, bundle)
                dismiss()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
