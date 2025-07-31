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
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
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
    
    val viewModel: CycleManagementViewModel by viewModels {
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

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

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
                else -> "Desconhecido"
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
            viewModel.isLoading.collect { isLoading ->
                // TODO: Implementar loading se necessário
            }
        }
        
        lifecycleScope.launch {
            // Observar erros
            viewModel.errorMessage.collect { error ->
                error?.let {
                    mostrarFeedback(it, Snackbar.LENGTH_LONG)
                }
            }
        }
    }

    private fun atualizarInterfaceCiclo(dados: CycleManagementData) {
        binding.tvCycleTitle.text = dados.titulo
        
        val periodo = if (dados.dataFim != null) {
            "${dateFormatter.format(dados.dataInicio)} - ${dateFormatter.format(dados.dataFim)}"
        } else {
            "Iniciado em ${dateFormatter.format(dados.dataInicio)}"
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
    }

    private fun atualizarEstatisticasFinanceiras(stats: CycleFinancialStats) {
        binding.tvTotalRecebido.text = currencyFormatter.format(stats.totalRecebido)
        binding.tvDespesasViagem.text = currencyFormatter.format(stats.despesasViagem)
        binding.tvSubtotal.text = currencyFormatter.format(stats.subtotal)
        binding.tvComissaoMotorista.text = currencyFormatter.format(stats.comissaoMotorista)
        binding.tvComissaoIltair.text = currencyFormatter.format(stats.comissaoIltair)
        binding.tvSomaPix.text = currencyFormatter.format(stats.somaPix)
        binding.tvSomaDespesas.text = currencyFormatter.format(stats.somaDespesas)
        binding.tvCheques.text = currencyFormatter.format(stats.cheques)
        binding.tvTotalGeral.text = currencyFormatter.format(stats.totalGeral)
    }

    private fun atualizarEstatisticasModalidade(stats: PaymentMethodStats) {
        binding.tvPix.text = currencyFormatter.format(stats.pix)
        binding.tvCartao.text = currencyFormatter.format(stats.cartao)
        binding.tvCheque.text = currencyFormatter.format(stats.cheque)
        binding.tvDinheiro.text = currencyFormatter.format(stats.dinheiro)
        binding.tvTotalRecebidoModalidade.text = currencyFormatter.format(stats.totalRecebido)
    }

    private fun gerarRelatorioPDF() {
        lifecycleScope.launch {
            try {
                mostrarFeedback("Gerando relatório PDF...", Snackbar.LENGTH_SHORT)
                
                // Buscar dados completos
                val dadosCiclo = viewModel.dadosCiclo.value
                if (dadosCiclo == null) {
                    mostrarFeedback("Erro: Dados do ciclo não encontrados", Snackbar.LENGTH_LONG)
                    return@launch
                }
                
                val ciclo = viewModel.buscarCicloPorId(dadosCiclo.id)
                val rota = viewModel.buscarRotaPorId(dadosCiclo.rotaId)
                val acertos = viewModel.buscarAcertosPorCiclo(dadosCiclo.id)
                val despesas = viewModel.buscarDespesasPorCiclo(dadosCiclo.id)
                val clientes = viewModel.buscarClientesPorRota(dadosCiclo.rotaId)
                
                if (ciclo != null && rota != null) {
                    // Gerar PDF
                    val pdfGenerator = PdfReportGenerator(requireContext())
                    val pdfFile = pdfGenerator.generateCycleReport(ciclo, rota, acertos, despesas, clientes)
                    
                    // TODO: Implementar compartilhamento do PDF
                    mostrarFeedback("Relatório PDF gerado com sucesso!", Snackbar.LENGTH_LONG)
                    
                } else {
                    mostrarFeedback("Erro ao carregar dados para o relatório", Snackbar.LENGTH_LONG)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CycleManagementFragment", "Erro ao gerar relatório: ${e.message}")
                mostrarFeedback("Erro ao gerar relatório: ${e.message}", Snackbar.LENGTH_LONG)
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
    
    override fun getItemCount(): Int = 2
    
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
            else -> throw IllegalArgumentException("Position inválida: $position")
        }
    }
}