package com.example.gestaobilhares.ui.mesas
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.ui.databinding.FragmentMesasReformadasBinding
// TODO: MesaReformadaRepository não existe - usar AppRepository quando método estiver disponível
// import com.example.gestaobilhares.data.database.AppDatabase
// import com.example.gestaobilhares.data.repository.MesaReformadaRepository

/**
 * Fragment que exibe a lista de mesas reformadas.
 * Permite visualizar o histórico de reformas e adicionar novas reformas.
 */
class MesasReformadasFragment : Fragment() {

    private var _binding: FragmentMesasReformadasBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MesasReformadasViewModel
    private lateinit var adapter: MesasReformadasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMesasReformadasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        // TODO: MesaReformadaRepository não existe - usar AppRepository quando método estiver disponível
        val appRepository = com.example.gestaobilhares.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = MesasReformadasViewModel(appRepository)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Carregar dados
        viewModel.carregarMesasReformadas()
    }

    private fun setupRecyclerView() {
        adapter = MesasReformadasAdapter { mesaComHistorico ->
            // ✅ NOVO: Mostrar diálogo com histórico completo
            mostrarDetalhesMesaComHistorico(mesaComHistorico)
        }

        binding.rvMesasReformadas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MesasReformadasFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabNovaReforma.setOnClickListener {
            // Navegar para a tela de nova reforma
            findNavController().navigate(com.example.gestaobilhares.ui.R.id.novaReformaFragment)
        }
        
        // ✅ NOVO: Botão de filtro
        binding.btnFiltrar.setOnClickListener {
            mostrarDialogoFiltro()
        }
    }
    
    /**
     * ✅ NOVO: Mostra diálogo para filtrar por número da mesa
     */
    private fun mostrarDialogoFiltro() {
        val input = EditText(requireContext())
        input.hint = "Digite o número da mesa"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        
        // Obter filtro atual e preencher o campo
        viewLifecycleOwner.lifecycleScope.launch {
            val filtroAtual = viewModel.filtroNumeroMesa.first()
            if (filtroAtual != null) {
                input.setText(filtroAtual)
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filtrar por Número da Mesa")
                .setView(input)
                .setPositiveButton("Filtrar") { _, _ ->
                    val numero = input.text.toString().trim()
                    if (numero.isNotEmpty()) {
                        viewModel.filtrarPorNumero(numero)
                    } else {
                        viewModel.removerFiltro()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .apply {
                    // Mostrar botão "Limpar Filtro" apenas se houver filtro ativo
                    if (filtroAtual != null) {
                        setNeutralButton("Limpar Filtro") { _, _ ->
                            viewModel.removerFiltro()
                        }
                    }
                }
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mesasReformadas.collect { mesas ->
                    adapter.submitList(mesas)
                    
                    // Mostrar/ocultar estado vazio
                    binding.emptyStateLayout.visibility = if (mesas.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // TODO: Implementar loading state se necessário
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { message ->
                    message?.let {
                        // TODO: Mostrar erro
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun mostrarDetalhesMesaReformada(mesaReformada: MesaReformada) {
        try {
            val dialog = DetalhesMesaReformadaDialog.newInstance(mesaReformada)
            dialog.show(parentFragmentManager, "DetalhesMesaReformadaDialog")
        } catch (e: Exception) {
            // Log do erro se necessário
        }
    }
    
    /**
     * ✅ NOVO: Mostra diálogo com detalhes da mesa e histórico completo
     */
    private fun mostrarDetalhesMesaComHistorico(mesaComHistorico: MesaReformadaComHistorico) {
        try {
            val dialog = DetalhesMesaReformadaComHistoricoDialog.newInstance(mesaComHistorico)
            dialog.show(parentFragmentManager, "DetalhesMesaReformadaComHistoricoDialog")
        } catch (e: Exception) {
            android.util.Log.e("MesasReformadasFragment", "Erro ao mostrar detalhes: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao exibir detalhes: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

