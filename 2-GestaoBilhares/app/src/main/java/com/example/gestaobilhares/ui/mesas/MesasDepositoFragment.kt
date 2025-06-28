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
                _binding?.let { binding ->
                    adapter.submitList(mesas)
                    binding.tvEmptyState.visibility = if (mesas.isEmpty()) View.VISIBLE else View.GONE
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