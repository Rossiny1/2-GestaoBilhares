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
 * Fragment para tela de Relatórios com navegação para relatórios consolidados.
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
        android.util.Log.d("ReportsFragment", "onViewCreated: Fragment carregado")
        setupUI()
    }
    
    private fun setupUI() {
        android.util.Log.d("ReportsFragment", "setupUI: Configurando UI")
        
        // Verificar se os botões estão sendo encontrados
        try {
            android.util.Log.d("ReportsFragment", "Verificando botões...")
            android.util.Log.d("ReportsFragment", "btnBack: ${binding.btnBack != null}")
            android.util.Log.d("ReportsFragment", "btnRelatorioRotas: ${binding.btnRelatorioRotas != null}")
            android.util.Log.d("ReportsFragment", "btnRelatorioClientes: ${binding.btnRelatorioClientes != null}")
            android.util.Log.d("ReportsFragment", "btnRelatorioAcertos: ${binding.btnRelatorioAcertos != null}")
            android.util.Log.d("ReportsFragment", "btnRelatorioFinanceiro: ${binding.btnRelatorioFinanceiro != null}")
            android.util.Log.d("ReportsFragment", "btnRelatorioDespesas: ${binding.btnRelatorioDespesas != null}")
            android.util.Log.d("ReportsFragment", "btnRelatorioConsolidadoCiclo: ${binding.btnRelatorioConsolidadoCiclo != null}")
            android.util.Log.d("ReportsFragment", "btnRelatorioAnualConsolidado: ${binding.btnRelatorioAnualConsolidado != null}")
            android.util.Log.d("ReportsFragment", "btnDashboardGeral: ${binding.btnDashboardGeral != null}")
            android.util.Log.d("ReportsFragment", "btnRelatorioPerformance: ${binding.btnRelatorioPerformance != null}")
        } catch (e: Exception) {
            android.util.Log.e("ReportsFragment", "Erro ao verificar botões: ${e.message}", e)
        }
        
        // Botão voltar
        binding.btnBack.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão voltar clicado")
            findNavController().popBackStack()
        }
        
        // Botões de relatório básicos - agora navegam para relatórios específicos
        binding.btnRelatorioRotas.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Relatório Rotas clicado")
            android.widget.Toast.makeText(requireContext(), "Relatório Rotas - Teste", android.widget.Toast.LENGTH_SHORT).show()
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_relatorioConsolidadoCicloFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Consolidado Ciclo bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
                android.widget.Toast.makeText(requireContext(), "Erro na navegação: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        
        binding.btnRelatorioClientes.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Relatório Clientes clicado")
            android.widget.Toast.makeText(requireContext(), "Relatório Clientes - Teste", android.widget.Toast.LENGTH_SHORT).show()
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_relatorioClientesFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Relatório Clientes bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
                android.widget.Toast.makeText(requireContext(), "Erro na navegação: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        
        binding.btnRelatorioAcertos.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Relatório Acertos clicado")
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_relatorioConsolidadoCicloFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Consolidado Ciclo bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
            }
        }
        
        binding.btnRelatorioFinanceiro.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Relatório Financeiro clicado")
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_relatorioAnualConsolidadoFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Anual Consolidado bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
            }
        }
        
        binding.btnRelatorioDespesas.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Relatório Despesas clicado")
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_despesasCategoriaFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Despesas Categoria bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
            }
        }
        
        // Novos botões de relatórios consolidados
        binding.btnRelatorioConsolidadoCiclo.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Consolidado Ciclo clicado")
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_relatorioConsolidadoCicloFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Consolidado Ciclo bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
            }
        }
        
        binding.btnRelatorioAnualConsolidado.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Anual Consolidado clicado")
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_relatorioAnualConsolidadoFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Anual Consolidado bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
            }
        }
        
        binding.btnDashboardGeral.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Dashboard Geral clicado")
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_dashboardGeralFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Dashboard Geral bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
            }
        }
        
        binding.btnRelatorioPerformance.setOnClickListener {
            android.util.Log.d("ReportsFragment", "Botão Performance Colaboradores clicado")
            try {
                findNavController().navigate(R.id.action_reportsFragment_to_colaboradorPerformanceFragment)
                android.util.Log.d("ReportsFragment", "Navegação para Performance Colaboradores bem-sucedida")
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Erro na navegação: ${e.message}", e)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 