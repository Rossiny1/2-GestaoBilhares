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
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.utils.PdfReportGenerator
import com.example.gestaobilhares.ui.clients.CycleReportDialog
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.gestaobilhares.utils.DateUtils
import com.example.gestaobilhares.utils.StringUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat

/**
 * Fragment para gerenciar ciclos (em andamento e finalizados)
 * Implementa todas as informações do relatório PDF
 */
class CycleManagementFragment : Fragment() {

    private var _binding: FragmentCycleManagementBinding? = null
    private val binding get() = _binding!!
    
    var cicloId: Long = 0L
    var rotaId: Long = 0L
    
    private lateinit var viewModel: CycleManagementViewModel

    // Formatação centralizada via utilitários
    // private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    // private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val database = AppDatabase.getDatabase(requireContext())
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        val cicloAcertoRepository = CicloAcertoRepository(
            database.cicloAcertoDao(),
            DespesaRepository(database.despesaDao()),
            AcertoRepository(database.acertoDao(), database.clienteDao()),
            ClienteRepository(database.clienteDao(), appRepository),
            database.rotaDao()
        )
        viewModel = CycleManagementViewModel(cicloAcertoRepository, appRepository)
        
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
        
        // Botão de impressão
        binding.btnPrint.setOnClickListener {
            gerarRelatorioPDF()
        }
        
        // Botão adicionar despesa (inicialmente oculto)
        binding.fabAddExpense.setOnClickListener {
            // TODO: Implementar adição de despesa
            mostrarFeedback("Funcionalidade de adicionar despesa será implementada em breve", Snackbar.LENGTH_LONG)
        }
    }

    private fun setupViewPager() {
        val isCicloFinalizado = viewModel.dadosCiclo.value?.status == StatusCicloAcerto.FINALIZADO
        val pagerAdapter = CycleManagementPagerAdapter(this, cicloId, rotaId, isCicloFinalizado)
        binding.viewPager.adapter = pagerAdapter
        
        // Configurar tabs
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Recebimentos"
                1 -> "Despesas"
                else -> "Resumo"
            }
        }.attach()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Observar dados do ciclo
            viewModel.dadosCiclo.collect { dados ->
                dados?.let { atualizarInterfaceCiclo(it) }
            }
        }
        
        lifecycleScope.launch {
            // Observar estatísticas financeiras
            viewModel.estatisticas.collect { stats ->
                atualizarEstatisticasFinanceiras(stats)
            }
        }
        
        lifecycleScope.launch {
            // Observar estatísticas por modalidade
            viewModel.estatisticasModalidade.collect { stats ->
                atualizarEstatisticasModalidade(stats)
            }
        }
        
        lifecycleScope.launch {
            // Observar loading
            viewModel.isLoading.collect { _ ->
                // TODO: Implementar loading se necessário
            }
        }
        
        lifecycleScope.launch {
            // Observar erros
            viewModel.error.collect { error ->
                error?.let {
                    mostrarFeedback(it, Snackbar.LENGTH_LONG)
                }
            }
        }
    }

    private fun atualizarInterfaceCiclo(dados: CycleManagementData) {
        binding.tvCycleTitle.text = dados.titulo
        
        val periodo = if (dados.dataFim != null) {
            "${DateUtils.formatarDataBrasileira(dados.dataInicio)} - ${DateUtils.formatarDataBrasileira(dados.dataFim)}"
        } else {
            "Iniciado em ${DateUtils.formatarDataBrasileira(dados.dataInicio)}"
        }
        binding.tvCyclePeriod.text = periodo
        
        // Status do ciclo
        val statusText = when (dados.status) {
            StatusCicloAcerto.EM_ANDAMENTO -> "Em andamento"
            StatusCicloAcerto.FINALIZADO -> "Finalizado"
            else -> "Desconhecido"
        }
        binding.tvCycleStatus.text = statusText
        
        // Mostrar/ocultar FAB baseado no status
        binding.fabAddExpense.visibility = if (dados.status == StatusCicloAcerto.EM_ANDAMENTO) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // ✅ NOVA LÓGICA: Configurar botão de impressão baseado no status
        if (dados.status == StatusCicloAcerto.EM_ANDAMENTO) {
            // Ciclo em andamento - botão desabilitado com tooltip
            binding.btnPrint.isEnabled = false
            binding.btnPrint.alpha = 0.5f
            binding.btnPrint.contentDescription = "Geração de relatório disponível apenas para ciclos finalizados"
        } else {
            // Ciclo finalizado - botão habilitado
            binding.btnPrint.isEnabled = true
            binding.btnPrint.alpha = 1.0f
            binding.btnPrint.contentDescription = "Gerar relatório detalhado"
        }
    }

    private fun atualizarEstatisticasFinanceiras(@Suppress("UNUSED_PARAMETER") stats: CycleFinancialStats) {
        // Remover referências às views que foram movidas para o CycleSummaryFragment
        // Essas views agora são atualizadas diretamente no fragment de resumo
    }

    private fun atualizarEstatisticasModalidade(@Suppress("UNUSED_PARAMETER") stats: PaymentMethodStats) {
        // Remover referências às views que foram movidas para o CycleSummaryFragment
        // Essas views agora são atualizadas diretamente no fragment de resumo
    }

    private fun gerarRelatorioPDF() {
        lifecycleScope.launch {
            try {
                // Buscar dados completos
                val dadosCiclo = viewModel.dadosCiclo.value
                if (dadosCiclo == null) {
                    mostrarFeedback("Erro: Dados do ciclo não encontrados", Snackbar.LENGTH_LONG)
                    return@launch
                }
                
                // ✅ NOVA VALIDAÇÃO: Verificar se o ciclo está finalizado
                if (dadosCiclo.status != StatusCicloAcerto.FINALIZADO) {
                    mostrarFeedback("A geração de relatório somente é possível após finalizar o acerto.", Snackbar.LENGTH_LONG)
                    return@launch
                }
                
                val ciclo = viewModel.buscarCicloPorId(dadosCiclo.id)
                val rota = viewModel.buscarRotaPorId(dadosCiclo.rotaId)
                val acertos = viewModel.buscarAcertosPorCiclo(dadosCiclo.id)
                val despesas = viewModel.buscarDespesasPorCiclo(dadosCiclo.id)
                val clientes = viewModel.buscarClientesPorRota(dadosCiclo.rotaId)
                
                if (ciclo != null && rota != null) {
                    // ✅ NOVA LÓGICA: Mostrar diálogo de relatório em vez de gerar PDF diretamente
                    val dialog = CycleReportDialog.newInstance(
                        ciclo = ciclo,
                        rota = rota,
                        acertos = acertos,
                        despesas = despesas,
                        clientes = clientes
                    )
                    dialog.show(parentFragmentManager, "CycleReportDialog")
                } else {
                    mostrarFeedback("Erro ao carregar dados para o relatório", Snackbar.LENGTH_LONG)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CycleManagementFragment", "Erro ao carregar dados do relatório: ${e.message}")
                mostrarFeedback("Erro ao carregar dados: ${e.message}", Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun mostrarFeedback(mensagem: String, duracao: Int) {
        Snackbar.make(binding.root, mensagem, duracao)
            .setBackgroundTint(requireContext().getColor(R.color.purple_600))
            .setTextColor(requireContext().getColor(R.color.white))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adapter para o ViewPager
 */
class CycleManagementPagerAdapter(
    fragment: Fragment,
    private val cicloId: Long,
    private val rotaId: Long,
    private val isCicloFinalizado: Boolean
) : FragmentStateAdapter(fragment) {
    
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CycleReceiptsFragment.newInstance(
                cicloId,
                rotaId,
                isCicloFinalizado
            )
            1 -> CycleExpensesFragment.newInstance(
                cicloId,
                rotaId,
                isCicloFinalizado
            )
            2 -> CycleSummaryFragment.newInstance(
                cicloId,
                rotaId,
                isCicloFinalizado
            )
            else -> throw IllegalArgumentException("Position inválida: $position")
        }
    }
}


