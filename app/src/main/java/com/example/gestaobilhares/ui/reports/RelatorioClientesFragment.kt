package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.databinding.FragmentRelatorioClientesBinding

class RelatorioClientesFragment : Fragment() {

    private var _binding: FragmentRelatorioClientesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelatorioClientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Configurar botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Configurar botão de filtro
        binding.btnFilter.setOnClickListener {
            // TODO: Implementar filtros avançados
        }

        // TODO: Implementar lógica do relatório de clientes
        // - Carregar dados de clientes
        // - Configurar filtros
        // - Exibir estatísticas
        // - Listar clientes com performance
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
