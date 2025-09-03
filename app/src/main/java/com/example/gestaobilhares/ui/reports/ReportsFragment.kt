package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentReportsBinding
import android.widget.Toast

/**
 * Fragment para tela de Relatórios com navegação para relatórios específicos.
 */
class ReportsFragment : Fragment() {
    
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }
    
    private fun setupUI() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Configurar listeners para os botões
        binding.btnRelatorioRotas.setOnClickListener {
            Toast.makeText(requireContext(), "Relatório de Rotas - Implementação futura", Toast.LENGTH_SHORT).show()
        }

        binding.btnRelatorioClientes.setOnClickListener {
            findNavController().navigate(R.id.action_reportsFragment_to_relatorioClientesFragment)
        }

        binding.btnRelatorioAcertos.setOnClickListener {
            Toast.makeText(requireContext(), "Relatório de Acertos - Implementação futura", Toast.LENGTH_SHORT).show()
        }

        binding.btnRelatorioFinanceiro.setOnClickListener {
            Toast.makeText(requireContext(), "Relatório Financeiro - Implementação futura", Toast.LENGTH_SHORT).show()
        }

        binding.btnRelatorioDespesas.setOnClickListener {
            findNavController().navigate(R.id.action_reportsFragment_to_despesasCategoriaFragment)
        }

        binding.btnRelatorioPerformance.setOnClickListener {
            findNavController().navigate(R.id.action_reportsFragment_to_colaboradorPerformanceFragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 