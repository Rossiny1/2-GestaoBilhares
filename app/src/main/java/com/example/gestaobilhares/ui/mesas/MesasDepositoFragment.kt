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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MesasDepositoFragment : Fragment() {
    private var _binding: FragmentMesasDepositoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MesasDepositoViewModel by viewModels()
    private val args: MesasDepositoFragmentArgs by navArgs()
    private lateinit var adapter: MesasDepositoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMesasDepositoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
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
            showTipoAcertoDialog(mesa)
        }
        binding.rvMesasDeposito.adapter = adapter
        binding.rvMesasDeposito.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnCadastrarMesa.setOnClickListener {
            val action = MesasDepositoFragmentDirections.actionMesasDepositoFragmentToCadastroMesaFragment()
            findNavController().navigate(action)
        }
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
                        // Atualizar cards de estatísticas
                        binding.tvTotalMesas.text = stats.totalMesas.toString()
                        // TODO: Verificar IDs corretos no layout
                        // binding.tvTotalSinuca.text = stats.mesasSinuca.toString()
                        // binding.tvTotalMaquinaMusica.text = stats.mesasMaquina.toString()
                        // binding.tvTotalPequenas.text = stats.mesasPequenas.toString()
                        // binding.tvTotalGrandes.text = stats.mesasGrandes.toString()
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

    private fun vincularMesa(mesa: Mesa, tipoFixo: Boolean, valorFixo: Double?) {
        val clienteId = args.clienteId.takeIf { it != 0L }
        if (clienteId != null) {
            viewModel.vincularMesaAoCliente(mesa.id, clienteId, tipoFixo, valorFixo)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 