package com.example.gestaobilhares.ui.routes
import com.example.gestaobilhares.ui.R

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.gestaobilhares.ui.databinding.DialogTransferClientBinding
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
/**
 * DialogFragment para transferir um cliente de uma rota para outra.
 * Exibe informações do cliente e permite selecionar a rota de destino.
 */
class TransferClientDialog : DialogFragment() {

    private var _binding: DialogTransferClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TransferClientViewModel

    private var onTransferSuccessListener: (() -> Unit)? = null

    companion object {
        private const val ARG_CLIENTE = "cliente"
        private const val ARG_ROTA_ORIGEM = "rota_origem"
        private const val ARG_MESAS = "mesas"

        fun newInstance(
            cliente: Cliente,
            rotaOrigem: Rota,
            mesas: List<Mesa>
        ): TransferClientDialog {
            val args = Bundle().apply {
                putSerializable(ARG_CLIENTE, cliente)
                putSerializable(ARG_ROTA_ORIGEM, rotaOrigem)
                putSerializable(ARG_MESAS, mesas.toTypedArray())
            }
            return TransferClientDialog().apply { arguments = args }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTransferClientBinding.inflate(layoutInflater)
        this.isCancelable = true
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
        dialog.setCanceledOnTouchOutside(true)
        
        // Inicializar repositórios e ViewModel
        val appRepository = com.example.gestaobilhares.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = TransferClientViewModel(appRepository)

        setupArguments()
        setupClickListeners()
        observeViewModel()
        setupRotaDestinoSpinner()
        
        return dialog
    }

    private fun setupArguments() {
        @Suppress("DEPRECATION")
        val cliente = arguments?.getSerializable(ARG_CLIENTE) as? Cliente
        @Suppress("DEPRECATION")
        val rotaOrigem = arguments?.getSerializable(ARG_ROTA_ORIGEM) as? Rota
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val mesas = arguments?.getSerializable(ARG_MESAS) as? Array<Mesa>

        if (cliente != null && rotaOrigem != null && mesas != null) {
            // Preencher campos com dados do cliente
            binding.etClienteNome.setText(cliente.nome)
            binding.etClienteCpf.setText(cliente.cpfCnpj ?: "Não informado")
            binding.etRotaOrigem.setText(rotaOrigem.nome)
            
            // Listar mesas vinculadas
            val mesasText = mesas.joinToString(", ") { "Mesa ${it.numero}" }
            binding.etMesasVinculadas.setText(mesasText)

            // Carregar rotas disponíveis para destino
            viewModel.loadRotasDisponiveis(rotaOrigem.id)
        }
    }

    private fun setupRotaDestinoSpinner() {
        viewModel.rotasDisponiveis.observe(this) { rotas ->
            val routeNames = rotas.map { it.nome }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                routeNames
            )
            binding.spinnerRotaDestino.setAdapter(adapter)
        }
    }

    private fun setupClickListeners() {
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }

        binding.btnTransferir.setOnClickListener {
            @Suppress("DEPRECATION")
            val cliente = arguments?.getSerializable(ARG_CLIENTE) as? Cliente
            @Suppress("DEPRECATION")
            val rotaOrigem = arguments?.getSerializable(ARG_ROTA_ORIGEM) as? Rota
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            val mesas = arguments?.getSerializable(ARG_MESAS) as? Array<Mesa>
            
            val rotaDestinoNome = binding.spinnerRotaDestino.text?.toString()?.trim()
            if (rotaDestinoNome.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Selecione uma rota de destino", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cliente != null && rotaOrigem != null && mesas != null) {
                viewModel.transferirCliente(
                    cliente = cliente,
                    rotaOrigem = rotaOrigem,
                    rotaDestinoNome = rotaDestinoNome,
                    mesas = mesas.toList()
                )
            }
        }
    }

    private fun observeViewModel() {
        viewModel.transferSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(
                    requireContext(),
                    "Cliente transferido com sucesso!",
                    Toast.LENGTH_LONG
                ).show()
                onTransferSuccessListener?.invoke()
                dismiss()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun setOnTransferSuccessListener(listener: () -> Unit) {
        onTransferSuccessListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

