package com.example.gestaobilhares.ui.routes

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.DialogSelectClientBinding
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.data.repository.MesaRepository
/**
 * DialogFragment para selecionar um cliente para transferência.
 * Inclui busca por nome e exibe informações do cliente, rota e mesas.
 */
class ClientSelectionDialog : DialogFragment() {

    private var _binding: DialogSelectClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ClientSelectionViewModel

    private lateinit var clientAdapter: ClientSelectionAdapter

    private var onClientSelectedListener: ((Cliente, Rota, List<Mesa>) -> Unit)? = null

    companion object {
        fun newInstance(): ClientSelectionDialog {
            return ClientSelectionDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSelectClientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Selecionar Cliente")
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar repositórios e ViewModel
        val database = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(requireContext())
        val clienteRepository = com.example.gestaobilhares.data.repository.ClienteRepository(
            database.clienteDao(),
            com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        )
        val rotaRepository = RotaRepository(database.rotaDao())
        val mesaRepository = MesaRepository(database.mesaDao())
        viewModel = ClientSelectionViewModel(clienteRepository, rotaRepository, mesaRepository)

        setupRecyclerView()
        setupClickListeners()
        setupSearchField()
        observeViewModel()

        // Carregar todos os clientes inicialmente
        viewModel.loadAllClients()
    }

    private fun setupRecyclerView() {
        clientAdapter = ClientSelectionAdapter { cliente, rota, mesas ->
            onClientSelectedListener?.invoke(cliente, rota, mesas)
            dismiss()
        }

        binding.rvClientes.apply {
            adapter = clientAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }
    }

    private fun setupSearchField() {
        binding.etBuscarCliente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isBlank()) {
                    viewModel.loadAllClients()
                } else {
                    viewModel.searchClients(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.clientes.observe(viewLifecycleOwner) { clientes ->
            clientAdapter.submitList(clientes)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // Mostrar erro se necessário
                android.util.Log.e("ClientSelectionDialog", it)
            }
        }
    }

    fun setOnClientSelectedListener(listener: (Cliente, Rota, List<Mesa>) -> Unit) {
        onClientSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

