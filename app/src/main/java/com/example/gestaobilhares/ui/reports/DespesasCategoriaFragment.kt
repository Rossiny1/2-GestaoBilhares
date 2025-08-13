package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentDespesasCategoriaBinding
import com.example.gestaobilhares.ui.reports.viewmodel.DespesasCategoriaViewModel
import com.example.gestaobilhares.ui.reports.viewmodel.DespesasCategoriaViewModelFactory
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Fragment para relatório de despesas por categoria.
 */
class DespesasCategoriaFragment : Fragment() {
    
    private var _binding: FragmentDespesasCategoriaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DespesasCategoriaViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        DespesasCategoriaViewModelFactory(
            AppRepository(
                database.clienteDao(),
                database.acertoDao(),
                database.mesaDao(),
                database.rotaDao(),
                database.despesaDao(),
                database.colaboradorDao(),
                database.cicloAcertoDao()
            )
        )
    }
    
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
        setupObservers()
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
            showFilterDialog()
        }
        
        // Configurar RecyclerView
        binding.recyclerViewCategorias.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Implementar adapter para categorias
        
        setupSpinners()
    }
    
    private fun setupSpinners() {
        // Spinner Tipo de Filtro
        val tiposFiltro = listOf(
            "Ciclo Específico",
            "Consolidado de Ciclos", 
            "Ano Completo"
        )
        val adapterTipoFiltro = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposFiltro)
        binding.spinnerTipoFiltro.setAdapter(adapterTipoFiltro)
        
        binding.spinnerTipoFiltro.setOnItemClickListener { _, _, position, _ ->
            val tipo = when (position) {
                0 -> DespesasCategoriaViewModel.TipoFiltro.CICLO_ESPECIFICO
                1 -> DespesasCategoriaViewModel.TipoFiltro.CONSOLIDADO_CICLOS
                2 -> DespesasCategoriaViewModel.TipoFiltro.ANO_COMPLETO
                else -> DespesasCategoriaViewModel.TipoFiltro.CICLO_ESPECIFICO
            }
            viewModel.selecionarTipoFiltro(tipo)
        }
        
        // Spinner Ciclo
        binding.spinnerCiclo.setOnItemClickListener { _, _, position, _ ->
            viewModel.ciclos.value?.getOrNull(position)?.let { ciclo ->
                viewModel.selecionarCiclo(ciclo.numero.toLong())
            }
        }
        
        // Spinner Ano
        binding.spinnerAno.setOnItemClickListener { _, _, position, _ ->
            viewModel.anos.value?.getOrNull(position)?.let { ano ->
                viewModel.selecionarAno(ano)
            }
        }
        
        // Spinner Rota
        binding.spinnerRota.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                // "Todas as Rotas"
                viewModel.selecionarRota(0)
            } else {
                viewModel.rotas.value?.getOrNull(position - 1)?.let { rota ->
                    viewModel.selecionarRota(rota.id)
                }
            }
        }
        
        // Spinner Categoria
        binding.spinnerCategoria.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                // "Todas as Categorias"
                viewModel.selecionarCategoria(null)
            } else {
                viewModel.categorias.value?.getOrNull(position - 1)?.let { categoria ->
                    viewModel.selecionarCategoria(categoria)
                }
            }
        }
    }
    
    private fun setupObservers() {
        // Observar estatísticas
        viewModel.estatisticas.observe(viewLifecycleOwner) { estatisticas ->
            updateEstatisticas(estatisticas)
        }
        
        // Observar ciclos
        viewModel.ciclos.observe(viewLifecycleOwner) { ciclos ->
            val ciclosList = ciclos.map { "${it.numero}\u00BA Acerto" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ciclosList)
            binding.spinnerCiclo.setAdapter(adapter)
        }
        
        // Observar anos
        viewModel.anos.observe(viewLifecycleOwner) { anos ->
            val anosList = anos.map { it.toString() }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, anosList)
            binding.spinnerAno.setAdapter(adapter)
        }
        
        // Observar rotas
        viewModel.rotas.observe(viewLifecycleOwner) { rotas ->
            val rotasList = listOf("Todas") + rotas.map { it.nome }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rotasList)
            binding.spinnerRota.setAdapter(adapter)
        }
        
        // Observar categorias
        viewModel.categorias.observe(viewLifecycleOwner) { categorias ->
            val categoriasList = listOf("Todas as Categorias") + categorias
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoriasList)
            binding.spinnerCategoria.setAdapter(adapter)
        }
        
        // Observar loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observar erro
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                showErrorDialog(error)
            }
        }
    }
    
    private fun updateEstatisticas(estatisticas: DespesasCategoriaViewModel.EstatisticasDespesas) {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
        
        binding.txtTotalDespesas.text = formatter.format(estatisticas.totalDespesas)
        binding.txtMediaCategoria.text = formatter.format(estatisticas.mediaPorCategoria)
        binding.txtCategoriaMaior.text = estatisticas.categoriaMaior
        binding.txtCategoriaMenor.text = estatisticas.categoriaMenor
    }
    
    private fun showFilterDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtros Avançados")
            .setMessage("""
                Filtros disponíveis:
                
                • Ciclo Específico: Despesas de um ciclo específico
                • Consolidado de Ciclos: Soma de todos os ciclos do mesmo número (ex: todos os 1º ciclos)
                • Ano Completo: Todas as despesas do ano selecionado
                
                Use os filtros na tela para aplicar as seleções.
            """.trimIndent())
            .setPositiveButton("Entendi") { _, _ -> }
            .show()
    }
    
    private fun showErrorDialog(error: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Erro")
            .setMessage(error)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
