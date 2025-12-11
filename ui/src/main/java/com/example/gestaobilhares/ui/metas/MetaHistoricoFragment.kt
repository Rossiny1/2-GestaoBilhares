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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.MetaRotaResumo
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.factory.RepositoryFactory
import com.example.gestaobilhares.ui.databinding.FragmentMetaHistoricoBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Fragment para exibir histórico de metas de TODAS as rotas
 * Permite selecionar ano e ciclo para filtrar as metas
 * ✅ REFATORADO: Agora mostra TODAS as rotas do ciclo selecionado (não apenas uma rota)
 */
class MetaHistoricoFragment : Fragment() {

    private var _binding: FragmentMetaHistoricoBinding? = null
    private val binding get() = _binding!!

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
                carregarMetasTodasRotas()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            carregarMetasTodasRotas()
        }
    }

    /**
     * ✅ REFATORADO: Busca ciclos finalizados de TODAS as rotas (não apenas uma rota)
     */
    private fun carregarCiclosDisponiveis() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                
                // ✅ MUDANÇA: Buscar todos os ciclos e filtrar finalizados do ano selecionado
                val todasRotas = appRepository.obterTodasRotas().first().filter { it.ativa }
                val todosCiclosSet = mutableSetOf<CicloAcertoEntity>()
                
                // Buscar ciclos de cada rota e agregar
                for (rota in todasRotas) {
                    val ciclosDaRota = appRepository.buscarCiclosPorRota(rota.id)
                        .filter { it.ano == anoSelecionado && it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO }
                    todosCiclosSet.addAll(ciclosDaRota)
                }
                
                ciclosDisponiveis = todosCiclosSet
                    .distinctBy { it.numeroCiclo } // Pegar apenas um ciclo de cada número
                    .sortedByDescending { it.numeroCiclo } // Ordenar do mais recente para o mais antigo
                
                // Criar lista de strings para o spinner
                val ciclosStrings = if (ciclosDisponiveis.isEmpty()) {
                    listOf("Nenhum ciclo finalizado encontrado")
                } else {
                    ciclosDisponiveis.map { "${it.numeroCiclo}º Acerto - ${it.ano}" }
                }
                
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    ciclosStrings
                )
                binding.actvCiclo.setAdapter(adapter)
                
                // ✅ Seleção padrão: último ciclo finalizado (primeiro da lista ordenada)
                if (ciclosDisponiveis.isNotEmpty()) {
                    cicloSelecionado = ciclosDisponiveis.first()
                    binding.actvCiclo.setText(ciclosStrings.first(), false)
                    carregarMetasTodasRotas()
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

    /**
     * ✅ REFATORADO: Carrega metas de TODAS as rotas para o ciclo selecionado
     */
    private fun carregarMetasTodasRotas() {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                binding.progressBar.visibility = View.VISIBLE
                
                if (cicloSelecionado == null) {
                    mostrarEstadoVazio()
                    return@launch
                }
                
                // ✅ MUDANÇA CRÍTICA: Buscar TODAS as rotas ativas
                val rotasAtivas = appRepository.obterTodasRotas().first().filter { it.ativa }
                
                val metasPorRota = mutableListOf<MetaRotaResumo>()
                
                // Para cada rota, buscar as metas do ciclo selecionado
                for (rota in rotasAtivas) {
                    // Buscar ciclos da rota e encontrar o que corresponde ao número do ciclo selecionado
                    val ciclosDaRota = appRepository.buscarCiclosPorRota(rota.id)
                    val cicloDaRota = ciclosDaRota.find { 
                        it.numeroCiclo == cicloSelecionado!!.numeroCiclo && 
                        it.ano == cicloSelecionado!!.ano 
                    }
                    
                    if (cicloDaRota != null) {
                        // Buscar metas deste ciclo
                        val metas = appRepository.buscarMetasPorRotaECiclo(rota.id, cicloDaRota.id)
                        
                        // Calcular progresso das metas
                        val metasComProgresso = calcularProgressoMetas(metas, rota.id, cicloDaRota.id)
                        
                        // Buscar colaborador responsável
                        val colaborador = appRepository.buscarColaboradorResponsavelPrincipal(rota.id)
                        
                        // Criar resumo da rota
                        val metaRotaResumo = MetaRotaResumo(
                            rota = rota,
                            cicloAtual = cicloDaRota.numeroCiclo,
                            anoCiclo = cicloDaRota.ano,
                            statusCiclo = cicloDaRota.status,
                            colaboradorResponsavel = colaborador,
                            metas = metasComProgresso,
                            dataInicioCiclo = cicloDaRota.dataInicio,
                            dataFimCiclo = cicloDaRota.dataFim,
                            ultimaAtualizacao = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual()
                        )
                        
                        metasPorRota.add(metaRotaResumo)
                    }
                }
                
                if (metasPorRota.isEmpty()) {
                    mostrarEstadoVazio()
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewMetas.visibility = View.VISIBLE
                    metasAdapter.atualizarMetas(metasPorRota)
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
    ): List<MetaColaborador> {
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
            
            // Retornar meta com valor atual atualizado
            meta.copy(valorAtual = valorAtual)
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
}
