package com.example.gestaobilhares.ui.routes

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.databinding.DialogSelectClientBinding
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.AppRepository
/**
 * DialogFragment para selecionar um cliente para transferência.
 * Inclui busca por nome e exibe informações do cliente, rota e mesas.
 */
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels

/**
 * DialogFragment para selecionar um cliente para transferência.
 * Inclui busca por nome e exibe informações do cliente, rota e mesas.
 */
@AndroidEntryPoint
class ClientSelectionDialog : DialogFragment() {

    private var _binding: DialogSelectClientBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ClientSelectionViewModel by viewModels()

    private lateinit var clientAdapter: ClientSelectionAdapter

    private var onClientSelectedListener: ((Cliente, Rota, List<Mesa>) -> Unit)? = null

    companion object {
        fun newInstance(): ClientSelectionDialog {
            return ClientSelectionDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSelectClientBinding.inflate(layoutInflater)
        this.isCancelable = true
        
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
        dialog.setCanceledOnTouchOutside(true)
        
        // ✅ NOVO: Tornar o diálogo mais largo (90% da largura da tela)
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        
        // Inicializar repositórios e ViewModel
        
        setupRecyclerView()
        setupClickListeners()
        setupSearchField()
        observeViewModel()

        // Carregar todos os clientes inicialmente
        viewModel.loadAllClients()
        
        return dialog
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
        viewModel.clientes.observe(this) { clientes ->
            clientAdapter.submitList(clientes)
            
            // ✅ NOVO: Mostrar/ocultar estado vazio baseado nos resultados
            // Usar findViewById como fallback caso o binding não tenha sido atualizado
            try {
                if (clientes.isEmpty()) {
                    binding.rvClientes.visibility = View.GONE
                    val llEmptyState = binding.root.findViewById<View>(com.example.gestaobilhares.ui.R.id.llEmptyState)
                    val tvEmptyState = binding.root.findViewById<android.widget.TextView>(com.example.gestaobilhares.ui.R.id.tvEmptyState)
                    llEmptyState?.visibility = View.VISIBLE
                    tvEmptyState?.text = if (binding.etBuscarCliente.text.toString().trim().isNotEmpty()) {
                        "Nenhum cliente encontrado"
                    } else {
                        "Digite o nome do cliente para buscar"
                    }
                } else {
                    binding.rvClientes.visibility = View.VISIBLE
                    val llEmptyState = binding.root.findViewById<View>(com.example.gestaobilhares.ui.R.id.llEmptyState)
                    llEmptyState?.visibility = View.GONE
                }
            } catch (e: Exception) {
                // Se houver erro ao acessar os views, apenas mostrar/ocultar a lista
                binding.rvClientes.visibility = if (clientes.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewModel.errorMessage.observe(this) { message ->
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

