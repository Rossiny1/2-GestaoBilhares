package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.databinding.FragmentDespesasCategoriaBinding

/**
 * Fragment para relatório de despesas por categoria.
 */
class DespesasCategoriaFragment : Fragment() {
    
    private var _binding: FragmentDespesasCategoriaBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDespesasCategoriaBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("DespesasCategoriaFragment", "onViewCreated: Fragment carregado")
        setupUI()
    }
    
    private fun setupUI() {
        android.util.Log.d("DespesasCategoriaFragment", "setupUI: Configurando UI")
        
        // Botão voltar
        binding.btnBack.setOnClickListener {
            android.util.Log.d("DespesasCategoriaFragment", "Botão voltar clicado")
            findNavController().popBackStack()
        }
        
        // Botão filtro
        binding.btnFilter.setOnClickListener {
            android.util.Log.d("DespesasCategoriaFragment", "Botão filtro clicado")
            // TODO: Implementar filtros
        }
        
        // TODO: Implementar lógica do relatório de despesas por categoria
        // Por enquanto, apenas configurar os elementos básicos
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
