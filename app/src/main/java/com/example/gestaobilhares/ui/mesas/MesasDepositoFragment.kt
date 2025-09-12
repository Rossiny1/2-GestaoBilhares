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
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.utils.UserSessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.Toast
import kotlinx.coroutines.launch

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
        
        viewModel = MesasDepositoViewModel(MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao()))
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
        binding.btnCadastrarMesa.setOnClickListener {
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
    }
    
    /**
     * ✅ NOVO: Configura controle de acesso baseado no nível do usuário
     */
    private fun setupAccessControl() {
        val canManageTables = userSessionManager.canManageTables()
        
        // Ocultar botão "Cadastrar Mesa" para usuários USER
        binding.btnCadastrarMesa.visibility = if (canManageTables) View.VISIBLE else View.GONE
        
        android.util.Log.d("MesasDepositoFragment", 
            "🔒 Controle de acesso aplicado - Usuário: ${userSessionManager.getCurrentUserName()}, " +
            "Pode gerenciar mesas: $canManageTables, Botão visível: ${binding.btnCadastrarMesa.visibility == View.VISIBLE}")
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.mesasDisponiveis.collect { mesas ->
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
            viewModel.vincularMesaAoCliente(mesa.id, clienteId, tipoFixo, valorFixo)
            
            // ✅ NOVO: Mostrar diálogo de finalização de contrato
            android.util.Log.d("MesasDepositoFragment", "isFromClientRegister: ${args.isFromClientRegister}")
            
            if (args.isFromClientRegister) {
                // Veio do cadastro de cliente - mostrar diálogo de contrato
                android.util.Log.d("MesasDepositoFragment", "Vindo do cadastro - mostrando diálogo de contrato")
                
                // ✅ CORREÇÃO: Buscar TODAS as mesas já vinculadas ao cliente
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val todasMesasVinculadas = viewModel.obterTodasMesasVinculadasAoCliente(clienteId)
                        android.util.Log.d("MesasDepositoFragment", "Total de mesas vinculadas ao cliente: ${todasMesasVinculadas.size}")
                        todasMesasVinculadas.forEach { mesa ->
                            android.util.Log.d("MesasDepositoFragment", "Mesa vinculada: ID=${mesa.id}, Número=${mesa.numero}, Tipo=${mesa.tipoMesa}")
                        }
                        
                        val mesasIds = todasMesasVinculadas.map { it.id }
                        android.util.Log.d("MesasDepositoFragment", "IDs das mesas para o diálogo: $mesasIds")
                        
                        val dialog = com.example.gestaobilhares.ui.contracts.ContractFinalizationDialog.newInstance(
                            clienteId = clienteId,
                            mesasVinculadas = mesasIds,
                            tipoFixo = tipoFixo,
                            valorFixo = valorFixo ?: 0.0
                        )
                        dialog.show(parentFragmentManager, "ContractFinalizationDialog")
                    } catch (e: Exception) {
                        android.util.Log.e("MesasDepositoFragment", "Erro ao buscar mesas vinculadas", e)
                        // Fallback: usar apenas a mesa atual
                        val dialog = com.example.gestaobilhares.ui.contracts.ContractFinalizationDialog.newInstance(
                            clienteId = clienteId,
                            mesasVinculadas = listOf(mesa.id),
                            tipoFixo = tipoFixo,
                            valorFixo = valorFixo ?: 0.0
                        )
                        dialog.show(parentFragmentManager, "ContractFinalizationDialog")
                    }
                }
            } else {
                // Veio de outro lugar (provavelmente ClientDetailFragment) - voltar normalmente
                android.util.Log.d("MesasDepositoFragment", "Vindo de outro lugar - voltando normalmente")
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 