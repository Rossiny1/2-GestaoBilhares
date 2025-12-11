package com.example.gestaobilhares.ui.metas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.factory.RepositoryFactory
import com.example.gestaobilhares.ui.databinding.FragmentMetaHistoricoBinding
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment para exibir histórico de metas de uma rota
 * Permite selecionar ano e ciclo para filtrar as metas
 */
class MetaHistoricoFragment : Fragment() {

    private var _binding: FragmentMetaHistoricoBinding? = null
    private val binding get() = _binding!!

    private val args: MetaHistoricoFragmentArgs by navArgs()
    private val rotaId: Long by lazy { args.rotaId }

    private lateinit var appRepository: com.example.gestaobilhares.data.repository.AppRepository
    private lateinit var metasAdapter: MetaHistoricoAdapter

    private var anoSelecionado: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var cicloSelecionado: CicloAcertoEntity? = null
    private var ciclosDisponiveis: List<CicloAcertoEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetaHistoricoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar repositório
        appRepository = RepositoryFactory.getAppRepository(requireContext())

        setupToolbar()
        setupRecyclerView()
        setupAnoSpinner()
        setupCicloSpinner()
        setupSwipeRefresh()
        
        // Carregar dados iniciais
        carregarCiclosDisponiveis()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        metasAdapter = MetaHistoricoAdapter(emptyList())
        binding.recyclerViewMetas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = metasAdapter
        }
    }

    private fun setupAnoSpinner() {
        // Criar lista de anos (últimos 5 anos até o ano atual)
        val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
        val anos = (anoAtual downTo anoAtual - 4).map { it.toString() }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, anos)
        binding.actvAno.setAdapter(adapter)
        binding.actvAno.setText(anoAtual.toString(), false)
        
        binding.actvAno.setOnItemClickListener { _, _, position, _ ->
            val novoAno = anos[position].toInt()
            if (novoAno != anoSelecionado) {
                anoSelecionado = novoAno
                carregarCiclosDisponiveis()
            }
        }
    }

    private fun setupCicloSpinner() {
        binding.actvCiclo.setOnItemClickListener { _, _, position, _ ->
            if (position < ciclosDisponiveis.size) {
                cicloSelecionado = ciclosDisponiveis[position]
                carregarMetas()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            carregarMetas()
        }
    }

    private fun carregarCiclosDisponiveis() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                
                // ✅ CORREÇÃO: Buscar apenas ciclos FINALIZADOS para o histórico
                val todosCiclos = appRepository.buscarCiclosPorRotaEAno(rotaId, anoSelecionado)
                ciclosDisponiveis = todosCiclos.filter { 
                    it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO 
                }
                
                // Criar lista de strings para o spinner
                val ciclosStrings = if (ciclosDisponiveis.isEmpty()) {
                    listOf("Nenhum ciclo finalizado encontrado")
                } else {
                    ciclosDisponiveis.map { "Ciclo ${it.numeroCiclo}/${it.ano} - FINALIZADO" }
                }
                
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    ciclosStrings
                )
                binding.actvCiclo.setAdapter(adapter)
                
                // Selecionar o primeiro ciclo por padrão, ou o ciclo passado como argumento
                if (ciclosDisponiveis.isNotEmpty()) {
                    val cicloInicial = if (args.cicloNumero != 0 && args.cicloAno != 0) {
                        ciclosDisponiveis.find { 
                            it.numeroCiclo == args.cicloNumero && it.ano == args.cicloAno 
                        } ?: ciclosDisponiveis.first()
                    } else {
                        ciclosDisponiveis.first()
                    }
                    
                    cicloSelecionado = cicloInicial
                    val posicao = ciclosDisponiveis.indexOf(cicloInicial)
                    if (posicao >= 0) {
                        binding.actvCiclo.setText(ciclosStrings[posicao], false)
                    }
                    
                    carregarMetas()
                } else {
                    binding.progressBar.visibility = View.GONE
                    mostrarEstadoVazio()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MetaHistoricoFragment", "Erro ao carregar ciclos: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao carregar ciclos: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun carregarMetas() {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                binding.progressBar.visibility = View.VISIBLE
                
                if (cicloSelecionado == null) {
                    mostrarEstadoVazio()
                    return@launch
                }
                
                // Buscar metas do ciclo selecionado
                val metas = appRepository.buscarMetasPorRotaECiclo(rotaId, cicloSelecionado!!.id)
                
                // Calcular progresso das metas
                val metasComProgresso = calcularProgressoMetas(metas, rotaId, cicloSelecionado!!.id)
                
                if (metasComProgresso.isEmpty()) {
                    mostrarEstadoVazio()
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewMetas.visibility = View.VISIBLE
                    metasAdapter.atualizarMetas(metasComProgresso)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MetaHistoricoFragment", "Erro ao carregar metas: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao carregar metas: ${e.message}", Toast.LENGTH_SHORT).show()
                mostrarEstadoVazio()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private suspend fun calcularProgressoMetas(
        metas: List<MetaColaborador>,
        rotaId: Long,
        cicloId: Long
    ): List<MetaColaboradorComProgresso> {
        return metas.map { meta ->
            val valorAtual = when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO -> {
                    // Calcular faturamento total do ciclo
                    val acertos = appRepository.buscarAcertosPorRotaECiclo(rotaId, cicloId)
                    acertos.sumOf { it.valorTotal }
                }
                TipoMeta.CLIENTES_ACERTADOS -> {
                    // Calcular quantidade de clientes acertados
                    val clientesAcertados = appRepository.contarClientesAcertadosPorRotaECiclo(rotaId, cicloId)
                    clientesAcertados.toDouble()
                }
                TipoMeta.MESAS_LOCADAS -> {
                    // Contar mesas locadas no ciclo
                    val novas = appRepository.contarNovasMesasNoCiclo(rotaId, cicloId)
                    novas.toDouble()
                }
                TipoMeta.TICKET_MEDIO -> {
                    // Calcular ticket médio
                    val acertos = appRepository.buscarAcertosPorRotaECiclo(rotaId, cicloId)
                    val mesasLocadas = appRepository.contarNovasMesasNoCiclo(rotaId, cicloId)
                    if (mesasLocadas > 0) {
                        acertos.sumOf { it.valorTotal } / mesasLocadas
                    } else {
                        0.0
                    }
                }
            }
            
            val progresso = if (meta.valorMeta > 0) {
                ((valorAtual / meta.valorMeta) * 100).coerceAtMost(100.0)
            } else {
                0.0
            }
            
            MetaColaboradorComProgresso(
                meta = meta,
                valorAtual = valorAtual,
                progresso = progresso
            )
        }
    }

    private fun mostrarEstadoVazio() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewMetas.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Data class para representar uma meta com seu progresso calculado
     */
    data class MetaColaboradorComProgresso(
        val meta: MetaColaborador,
        val valorAtual: Double,
        val progresso: Double
    )
}

