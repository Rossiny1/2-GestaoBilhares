package com.example.gestaobilhares.ui.cycles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentCycleManagementBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.DespesaRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment para gerenciar ciclos em andamento
 * Permite editar, apagar e adicionar despesas, ver clientes acertados e valores recebidos
 */
class CycleManagementFragment : Fragment() {

    private var _binding: FragmentCycleManagementBinding? = null
    private val binding get() = _binding!!
    
    private var cicloId: Long = 0L
    private var rotaId: Long = 0L
    
    private val viewModel: CycleManagementViewModel by viewModels {
        CycleManagementViewModelFactory(
            CicloAcertoRepository(
                AppDatabase.getDatabase(requireContext()).cicloAcertoDao(),
                DespesaRepository(AppDatabase.getDatabase(requireContext()).despesaDao()),
                AcertoRepository(AppDatabase.getDatabase(requireContext()).acertoDao(), AppDatabase.getDatabase(requireContext()).clienteDao()),
                ClienteRepository(AppDatabase.getDatabase(requireContext()).clienteDao()),
                AppDatabase.getDatabase(requireContext()).rotaDao()
            ),
            AppRepository(
                AppDatabase.getDatabase(requireContext()).clienteDao(),
                AppDatabase.getDatabase(requireContext()).acertoDao(),
                AppDatabase.getDatabase(requireContext()).mesaDao(),
                AppDatabase.getDatabase(requireContext()).rotaDao(),
                AppDatabase.getDatabase(requireContext()).despesaDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obter parâmetros dos argumentos
        cicloId = arguments?.getLong("cicloId", 0L) ?: 0L
        rotaId = arguments?.getLong("rotaId", 0L) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCycleManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupViewPager()
        setupObservers()
        
        // Carregar dados do ciclo
        viewModel.carregarDadosCiclo(cicloId, rotaId)
    }

    private fun setupViews() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão menu (futuro)
        binding.btnMenu.setOnClickListener {
            // TODO: Implementar menu de opções
        }
        
        // Botão adicionar despesa
        binding.fabAddExpense.setOnClickListener {
            mostrarDialogoAdicionarDespesa()
        }
    }

    /**
     * Mostra diálogo para adicionar nova despesa
     */
    private fun mostrarDialogoAdicionarDespesa() {
        // TODO: Implementar diálogo completo para adicionar despesa
        // Por enquanto, mostrar mensagem
        mostrarFeedback("Funcionalidade de adicionar despesa será implementada em breve", Snackbar.LENGTH_LONG)
    }

    private fun mostrarFeedback(mensagem: String, duracao: Int) {
        Snackbar.make(binding.root, mensagem, duracao)
            .setBackgroundTint(requireContext().getColor(R.color.purple_600))
            .setTextColor(requireContext().getColor(R.color.white))
            .show()
    }

    private fun setupViewPager() {
        val pagerAdapter = CycleManagementPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        // Conectar TabLayout com ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Despesas"
                1 -> "Clientes"
                2 -> "Resumo"
                else -> ""
            }
        }.attach()
    }

    private fun setupObservers() {
        // Observar dados do ciclo
        lifecycleScope.launch {
            viewModel.dadosCiclo.collect { dados ->
                dados?.let { ciclo ->
                    atualizarInformacoesCiclo(ciclo)
                }
            }
        }
        
        // Observar estatísticas financeiras
        lifecycleScope.launch {
            viewModel.estatisticas.collect { stats ->
                atualizarEstatisticas(stats)
            }
        }
        
        // Observar estado de carregamento
        lifecycleScope.launch {
            viewModel.isLoading.collect { carregando ->
                // TODO: Mostrar/esconder loading
            }
        }
        
        // Observar mensagens de erro
        lifecycleScope.launch {
            viewModel.errorMessage.collect { mensagem ->
                mensagem?.let {
                    // TODO: Mostrar mensagem de erro
                    viewModel.limparErro()
                }
            }
        }
    }

    private fun atualizarInformacoesCiclo(ciclo: CycleManagementData) {
        binding.apply {
            tvCycleTitle.text = ciclo.titulo
            tvCyclePeriod.text = "${formatarData(ciclo.dataInicio)} - ${formatarData(ciclo.dataFim)}"
        }
    }

    private fun atualizarEstatisticas(stats: CycleFinancialStats) {
        binding.apply {
            tvTotalRevenue.text = formatarMoeda(stats.receitas)
            tvTotalExpenses.text = formatarMoeda(stats.despesas)
            tvTotalProfit.text = formatarMoeda(stats.lucro)
            
            // Cor do lucro
            val profitColor = if (stats.lucro >= 0) {
                requireContext().getColor(R.color.success_green)
            } else {
                requireContext().getColor(R.color.error_red)
            }
            tvTotalProfit.setTextColor(profitColor)
        }
    }

    private fun formatarData(data: java.util.Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(data)
    }

    private fun formatarMoeda(valor: Double): String {
        return java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter para o ViewPager com as seções
     */
    private inner class CycleManagementPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CycleExpensesFragment.newInstance(cicloId)
                1 -> CycleClientsFragment.newInstance(cicloId, rotaId)
                2 -> PlaceholderFragment.newInstance("Resumo")
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }

    /**
     * Fragment placeholder temporário
     */
    class PlaceholderFragment : Fragment() {
        companion object {
            private const val ARG_SECTION_NAME = "section_name"
            
            fun newInstance(sectionName: String): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putString(ARG_SECTION_NAME, sectionName)
                fragment.arguments = args
                return fragment
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val rootView = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
                
                addView(TextView(requireContext()).apply {
                    text = arguments?.getString(ARG_SECTION_NAME) ?: "Seção"
                    textSize = 18f
                    setTextColor(requireContext().getColor(R.color.white))
                    gravity = Gravity.CENTER
                })
                
                addView(TextView(requireContext()).apply {
                    text = "Será implementado em breve"
                    textSize = 14f
                    setTextColor(requireContext().getColor(R.color.text_secondary))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 16
                    }
                })
            }
            return rootView
        }
    }
}