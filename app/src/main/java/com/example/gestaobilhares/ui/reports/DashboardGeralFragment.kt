package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.databinding.FragmentDashboardGeralBinding

/**
 * Fragment para dashboard geral.
 */
class DashboardGeralFragment : Fragment() {
    
    private var _binding: FragmentDashboardGeralBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardGeralBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("DashboardGeralFragment", "onViewCreated: Fragment carregado")
        setupUI()
    }
    
    private fun setupUI() {
        android.util.Log.d("DashboardGeralFragment", "setupUI: Configurando UI")
        
        // Botão voltar
        binding.btnBack.setOnClickListener {
            android.util.Log.d("DashboardGeralFragment", "Botão voltar clicado")
            findNavController().popBackStack()
        }
        
        // TODO: Implementar lógica do dashboard geral
        // Por enquanto, apenas configurar os elementos básicos
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
