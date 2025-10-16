package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentMesasDepositoBinding
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.utils.UserSessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

// Hilt removido - usando instanciação direta
class MesasDepositoFragment : Fragment() {
    private var _binding: FragmentMesasDepositoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MesasDepositoViewModel
    private val args: MesasDepositoFragmentArgs by navArgs()
    private lateinit var adapter: MesasDepositoAdapter
    private lateinit var userSessionManager: UserSessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMesasDepositoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar gerenciador de sessão
        userSessionManager = UserSessionManager.getInstance(requireContext())
        
        val database = AppDatabase.getDatabase(requireContext())
        val appRepository = AppRepository(
            database.clienteDao(),
            database.acertoDao(),
            database.mesaDao(),
            database.rotaDao(),
            database.despesaDao(),
            database.colaboradorDao(),
            database.cicloAcertoDao(),
            database.acertoMesaDao(),
            database.contratoLocacaoDao(),
            database.aditivoContratoDao(),
            database.assinaturaRepresentanteLegalDao(),
            database.logAuditoriaAssinaturaDao(),
            database.panoEstoqueDao(),
            database.categoriaDespesaDao(),
            database.tipoDespesaDao(),
            database.historicoManutencaoVeiculoDao(),
            database.historicoCombustivelVeiculoDao(),
            database.historicoManutencaoMesaDao()
        )
        viewModel = MesasDepositoViewModel(MesaRepository(database.mesaDao()), appRepository)
        setupRecyclerView()
        setupListeners()
        setupAccessControl()
        observeViewModel()
        viewModel.loadMesasDisponiveis()
    }

    override fun onResume() {
        super.onResume()
        // ✅ CORREÇÃO: Recarregar mesas disponíveis sempre que voltar para a tela
        viewModel.loadMesasDisponiveis()
    }

    private fun setupRecyclerView() {
        adapter = MesasDepositoAdapter { mesa ->
            // ✅ NOVA LÓGICA: Decidir comportamento baseado na origem
            if (args.isFromGerenciarMesas) {
                // Veio do Gerenciar Mesas - abrir edição da mesa
                navigateToEditMesa(mesa)
            } else {
                // Veio de Detalhes do Cliente - mostrar dialog de tipo de acerto
                showTipoAcertoDialog(mesa)
            }
        }
        binding.rvMesasDeposito.adapter = adapter
        // ✅ NOVO: Usar GridLayoutManager com 2 colunas
        binding.rvMesasDeposito.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.fabAddMesa.setOnClickListener {
            // ✅ NOVO: Verificar permissão antes de navegar
            if (!userSessionManager.canManageTables()) {
                Toast.makeText(requireContext(), "Acesso negado: Apenas administradores podem cadastrar mesas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val action = MesasDepositoFragmentDirections.actionMesasDepositoFragmentToCadastroMesaFragment()
            findNavController().navigate(action)
        }
        
        // ✅ NOVO: Listener para o card Sinuca
        binding.cardSinuca.setOnClickListener {
            showDetalhesSinucaDialog()
        }

        // ✅ BUSCA POR NÚMERO: Botão e ação do teclado
        binding.btnBuscarMesa.setOnClickListener {
            val query = binding.etSearchNumero.text?.toString().orEmpty()
            viewModel.atualizarBuscaNumero(query)
        }
        binding.etSearchNumero.setOnEditorActionListener { v, actionId, event ->
            val text = binding.etSearchNumero.text?.toString().orEmpty()
            viewModel.atualizarBuscaNumero(text)
            true
        }

        // ❌ REMOVIDO: FAB de venda de mesa do local incorreto
        // A venda de mesa deve ser feita na tela "Mesas Vendidas"
    }
    
    /**
     * ✅ NOVO: Configura controle de acesso baseado no nível do usuário
     */
    private fun setupAccessControl() {
        val canManageTables = userSessionManager.canManageTables()
        
        // Ocultar FAB "Adicionar Mesa" para usuários USER
        binding.fabAddMesa.visibility = if (canManageTables) View.VISIBLE else View.GONE
        
        android.util.Log.d("MesasDepositoFragment", 
            "🔒 Controle de acesso aplicado - Usuário: ${userSessionManager.getCurrentUserName()}, " +
            "Pode gerenciar mesas: $canManageTables, FAB visível: ${binding.fabAddMesa.visibility == View.VISIBLE}")
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.mesasFiltradas.collect { mesas ->
                android.util.Log.d("MesasDepositoFragment", "=== MESAS RECEBIDAS NO FRAGMENT ===")
                android.util.Log.d("MesasDepositoFragment", "📱 Mesas recebidas do ViewModel: ${mesas.size}")
                mesas.forEach { mesa ->
                    android.util.Log.d("MesasDepositoFragment", "Mesa: ${mesa.numero} | ID: ${mesa.id} | Ativa: ${mesa.ativa} | ClienteId: ${mesa.clienteId}")
                }
                
                _binding?.let { binding ->
                    android.util.Log.d("MesasDepositoFragment", "🔄 Atualizando adapter com ${mesas.size} mesas")
                    adapter.submitList(mesas)
                    
                    val isEmpty = mesas.isEmpty()
                    android.util.Log.d("MesasDepositoFragment", "📊 Lista vazia: $isEmpty")
                    
                    binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.rvMesasDeposito.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    
                    android.util.Log.d("MesasDepositoFragment", "✅ UI atualizada - EmptyState: ${binding.tvEmptyState.visibility}, RecyclerView: ${binding.rvMesasDeposito.visibility}")
                }
            }
        }
        
        // Observer para estatísticas
        lifecycleScope.launch {
            viewModel.estatisticas.collect { stats ->
                _binding?.let { binding ->
                    try {
                        // ✅ ATUALIZADO: Atualizar cards de estatísticas por tipo
                        binding.tvTotalSinuca.text = stats.mesasSinuca.toString()
                        binding.tvTotalJukebox.text = stats.mesasMaquina.toString()
                        binding.tvTotalPembolim.text = stats.mesasPembolim.toString()
                    } catch (e: Exception) {
                        // Log do erro para debug posterior
                        android.util.Log.e("MesasDepositoFragment", "Erro ao atualizar estatísticas", e)
                    }
                }
            }
        }
    }

    private fun showTipoAcertoDialog(mesa: Mesa) {
        val tipos = arrayOf("Fichas Jogadas", "Valor Fixo")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tipo de Acerto")
            .setItems(tipos) { _, which ->
                if (which == 0) {
                    vincularMesa(mesa, tipoFixo = false, valorFixo = null)
                } else {
                    showValorFixoDialog(mesa)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showValorFixoDialog(mesa: Mesa) {
        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Valor Fixo do Aluguel")
            .setView(input)
            .setPositiveButton("Vincular") { _, _ ->
                val valor = input.text.toString().toDoubleOrNull()
                if (valor != null) {
                    vincularMesa(mesa, tipoFixo = true, valorFixo = valor)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * ✅ NOVA FUNÇÃO: Navegar para tela de edição da mesa
     * Usado quando acessado pelo Gerenciar Mesas
     */
    private fun navigateToEditMesa(mesa: Mesa) {
        android.util.Log.d("MesasDepositoFragment", "Navegando para edição da mesa: ${mesa.numero} (ID: ${mesa.id})")

        try {
            val action = MesasDepositoFragmentDirections.actionMesasDepositoFragmentToEditMesaFragment(
                mesaId = mesa.id
            )
            findNavController().navigate(action)
        } catch (e: Exception) {
            android.util.Log.e("MesasDepositoFragment", "Erro ao navegar para edição da mesa: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao abrir edição da mesa", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ NOVA FUNÇÃO: Mostra diálogo com detalhes das mesas de sinuca
     */
    private fun showDetalhesSinucaDialog() {
        val mesas = viewModel.mesasDisponiveis.value
        val mesasSinuca = mesas.filter { it.tipoMesa == com.example.gestaobilhares.data.entities.TipoMesa.SINUCA }
        
        val pequenas = mesasSinuca.count { it.tamanho == com.example.gestaobilhares.data.entities.TamanhoMesa.PEQUENA }
        val medias = mesasSinuca.count { it.tamanho == com.example.gestaobilhares.data.entities.TamanhoMesa.MEDIA }
        val grandes = mesasSinuca.count { it.tamanho == com.example.gestaobilhares.data.entities.TamanhoMesa.GRANDE }
        
        val mensagem = """
            📊 Detalhamento das Mesas de Sinuca:
            
            🟢 Pequenas: $pequenas
            🟡 Médias: $medias
            🔴 Grandes: $grandes
            
            📋 Total Sinuca: ${mesasSinuca.size} mesas
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Detalhes das Mesas de Sinuca")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun vincularMesa(mesa: Mesa, tipoFixo: Boolean, valorFixo: Double?) {
        val clienteId = args.clienteId.takeIf { it != 0L }
        if (clienteId != null) {
            // ✅ CORREÇÃO CRÍTICA: Verificar status ANTES de vincular a mesa
            viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        android.util.Log.d("MesasDepositoFragment", "Decidindo diálogo pós-vinculação para cliente $clienteId / mesa ${mesa.id}")
                        
                        val db = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(requireContext())
                        
                        // ✅ FORÇAR REFRESH DO BANCO ANTES DA DECISÃO
                        kotlinx.coroutines.delay(100) // Pequeno delay para garantir sincronização
                        val todos = db.contratoLocacaoDao().buscarContratosPorCliente(clienteId).first()
                        android.util.Log.d("MesasDepositoFragment", "Total contratos cliente $clienteId: ${todos.size}")
                        
                        // ✅ REGRA DEFINITIVA: Verificação completa do status do cliente
                        android.util.Log.d("MesasDepositoFragment", "=== ANÁLISE COMPLETA DOS CONTRATOS ===")
                        todos.forEachIndexed { idx, c ->
                            android.util.Log.d("MesasDepositoFragment", 
                                "Contrato #$idx -> id=${c.id} status='${c.status}' " +
                                "criacao=${c.dataCriacao} encerramento=${c.dataEncerramento}")
                        }
                        
                        // Verificar se existe algum contrato com status diferente de ATIVO (indica distrato)
                        val temContratoEncerrado = todos.any { c -> 
                            val statusNormalized = c.status.uppercase().trim()
                            statusNormalized in listOf("ENCERRADO_QUITADO", "RESCINDIDO_COM_DIVIDA", "RESCINDIDO")
                        }
                        
                        // Verificar se existe algum contrato com dataEncerramento (indica distrato físico)
                        val temDistratoFisico = todos.any { c -> c.dataEncerramento != null }
                        
                        // Buscar último documento por data (priorizar dataEncerramento se existe)
                        val latest = todos.maxByOrNull { c -> 
                            c.dataEncerramento?.time ?: c.dataCriacao.time 
                        }
                        
                        android.util.Log.d("MesasDepositoFragment", 
                            "=== ANÁLISE DE STATUS ===\n" +
                            "temContratoEncerrado=$temContratoEncerrado\n" +
                            "temDistratoFisico=$temDistratoFisico\n" +
                            "latest.id=${latest?.id} latest.status='${latest?.status}'\n" +
                            "latest.dataEncerramento=${latest?.dataEncerramento}")
                        
                        if (latest != null) {
                            val isLatestActive = latest.status.equals("ATIVO", ignoreCase = true)
                            val hasEncerramento = latest.dataEncerramento != null
                            
                            // ✅ VERIFICAÇÃO ADICIONAL: Mesas ativas do cliente
                            val mesasAtivasCliente = db.mesaDao().obterMesasPorClienteDireto(clienteId).filter { it.ativa }
                            val temMesasAtivas = mesasAtivasCliente.isNotEmpty()
                            
                            android.util.Log.d("MesasDepositoFragment", 
                                "Mesas ativas cliente $clienteId: ${mesasAtivasCliente.size} -> $temMesasAtivas")
                            
                            // ✅ CORREÇÃO: Lógica simplificada - se o último contrato é ATIVO, permite aditivo
                            // Só abre aditivo se:
                            // 1. Último documento é ATIVO
                            // 2. Não tem dataEncerramento (não foi distratado)
                            // 3. Cliente tem mesas ativas
                            val deveAbrirAditivo = isLatestActive && !hasEncerramento && temMesasAtivas
                            
                            android.util.Log.d("MesasDepositoFragment", 
                                "=== DECISÃO FINAL ===\n" +
                                "isLatestActive=$isLatestActive\n" +
                                "hasEncerramento=$hasEncerramento\n" +
                                "temMesasAtivas=$temMesasAtivas\n" +
                                "RESULTADO: deveAbrirAditivo=$deveAbrirAditivo")
                            
                            if (deveAbrirAditivo) {
                                android.util.Log.d("MesasDepositoFragment", "ABRINDO ADITIVO: Documento mais recente é ATIVO sem encerramento")
                                
                                // ✅ VINCULAR MESA APENAS QUANDO FOR ADITIVO
                                viewModel.vincularMesaAoCliente(mesa.id, clienteId, tipoFixo, valorFixo)
                                
                                val dialog = com.example.gestaobilhares.ui.contracts.AditivoDialog.newInstance(latest)
                                dialog.setOnGerarAditivoClickListener { contrato ->
                                    val mesasIds = longArrayOf(mesa.id)
                                    val action = MesasDepositoFragmentDirections
                                        .actionMesasDepositoFragmentToAditivoSignatureFragment(
                                            contratoId = contrato.id,
                                            mesasVinculadas = mesasIds
                                        )
                                    findNavController().navigate(action)
                                }
                                dialog.setOnCancelarClickListener {
                                    findNavController().popBackStack()
                                }
                                dialog.show(parentFragmentManager, "AditivoDialog")
                            } else {
                                android.util.Log.d("MesasDepositoFragment", 
                                    "ABRINDO NOVO CONTRATO: Documento mais recente não é ATIVO vigente " +
                                    "(status=${latest.status}, hasEncerramento=$hasEncerramento)")
                                
                                // ✅ VINCULAR MESA ANTES DO NOVO CONTRATO
                                android.util.Log.d("MesasDepositoFragment", "Vinculando mesa ${mesa.id} ao cliente $clienteId antes do novo contrato")
                                
                                // Executar vinculação de forma síncrona
                                try {
                                    if (tipoFixo && valorFixo != null) {
                                        viewModel.mesaRepository.vincularMesaComValorFixo(mesa.id, clienteId, valorFixo)
                                    } else {
                                        viewModel.mesaRepository.vincularMesa(mesa.id, clienteId)
                                    }
                                    android.util.Log.d("MesasDepositoFragment", "Mesa vinculada com sucesso")
                                    
                                    // Aguardar um pouco para sincronização
                                    kotlinx.coroutines.delay(200)
                                    
                                    val todasMesasVinculadas = viewModel.obterTodasMesasVinculadasAoCliente(clienteId)
                                    val mesasIds = todasMesasVinculadas.map { it.id }
                                    android.util.Log.d("MesasDepositoFragment", "Mesas vinculadas encontradas: ${mesasIds.size} -> $mesasIds")
                                    val dialog = com.example.gestaobilhares.ui.contracts.ContractFinalizationDialog.newInstance(
                                        clienteId = clienteId,
                                        mesasVinculadas = mesasIds,
                                        tipoFixo = tipoFixo,
                                        valorFixo = valorFixo ?: 0.0
                                    )
                                    dialog.show(parentFragmentManager, "ContractFinalizationDialog")
                                } catch (e: Exception) {
                                    android.util.Log.e("MesasDepositoFragment", "Erro ao vincular mesa para novo contrato", e)
                                    // Usar apenas a mesa atual se houver erro
                                    val mesasIds = listOf(mesa.id)
                                    val dialog = com.example.gestaobilhares.ui.contracts.ContractFinalizationDialog.newInstance(
                                        clienteId = clienteId,
                                        mesasVinculadas = mesasIds,
                                        tipoFixo = tipoFixo,
                                        valorFixo = valorFixo ?: 0.0
                                    )
                                    dialog.show(parentFragmentManager, "ContractFinalizationDialog")
                                }
                            }
                        } else {
                            android.util.Log.d("MesasDepositoFragment", "ABRINDO NOVO CONTRATO: Nenhum contrato encontrado para cliente")
                            
                            // ✅ VINCULAR MESA ANTES DO NOVO CONTRATO
                            viewModel.vincularMesaAoCliente(mesa.id, clienteId, tipoFixo, valorFixo)
                            
                            val dialog = com.example.gestaobilhares.ui.contracts.ContractFinalizationDialog.newInstance(
                                clienteId = clienteId,
                                mesasVinculadas = listOf(mesa.id),
                                tipoFixo = tipoFixo,
                                valorFixo = valorFixo ?: 0.0
                            )
                            dialog.show(parentFragmentManager, "ContractFinalizationDialog")
                        }
                        
                    } catch (e: Exception) {
                        android.util.Log.e("MesasDepositoFragment", "Erro ao decidir diálogo pós-vinculação", e)
                        
                        // ✅ VINCULAR MESA EM CASO DE ERRO (FALLBACK)
                        viewModel.vincularMesaAoCliente(mesa.id, clienteId, tipoFixo, valorFixo)
                        
                        val dialog = com.example.gestaobilhares.ui.contracts.ContractFinalizationDialog.newInstance(
                            clienteId = clienteId,
                            mesasVinculadas = listOf(mesa.id),
                            tipoFixo = tipoFixo,
                            valorFixo = valorFixo ?: 0.0
                        )
                        dialog.show(parentFragmentManager, "ContractFinalizationDialog")
                    }
                }
        }
    }

    // ❌ REMOVIDO: Função de venda de mesa do local incorreto

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 





