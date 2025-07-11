package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentReportsBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment básico para tela de Relatórios.
 * Implementação temporária para evitar crash na navegação.
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
        
        // Botões de relatório (implementação básica)
        binding.btnRelatorioRotas.setOnClickListener {
            Snackbar.make(binding.root, "Relatório de Rotas será implementado em breve", Snackbar.LENGTH_SHORT).show()
        }
        
        binding.btnRelatorioClientes.setOnClickListener {
            Snackbar.make(binding.root, "Relatório de Clientes será implementado em breve", Snackbar.LENGTH_SHORT).show()
        }
        
        binding.btnRelatorioAcertos.setOnClickListener {
            Snackbar.make(binding.root, "Relatório de Acertos será implementado em breve", Snackbar.LENGTH_SHORT).show()
        }
        
        binding.btnRelatorioFinanceiro.setOnClickListener {
            Snackbar.make(binding.root, "Relatório Financeiro será implementado em breve", Snackbar.LENGTH_SHORT).show()
        }
        
        binding.btnRelatorioDespesas.setOnClickListener {
            Snackbar.make(binding.root, "Relatório de Despesas será implementado em breve", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 