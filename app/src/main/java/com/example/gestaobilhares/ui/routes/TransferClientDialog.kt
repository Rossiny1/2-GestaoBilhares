package com.example.gestaobilhares.ui.routes

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.DialogTransferClientBinding
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.data.repository.MesaRepository
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransferClientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Transferir Cliente")
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialização manual do ViewModel com repositórios necessários
        val database = AppDatabase.getDatabase(requireContext())
        val clienteRepository = ClienteRepository(database.clienteDao(),
            com.example.gestaobilhares.data.repository.AppRepository(
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
                database.logAuditoriaAssinaturaDao()
            )
        )
        val rotaRepository = RotaRepository(database.rotaDao())
        val mesaRepository = MesaRepository(database.mesaDao())
        viewModel = TransferClientViewModel(clienteRepository, rotaRepository, mesaRepository)

        setupArguments()
        setupClickListeners()
        observeViewModel()
        setupRotaDestinoSpinner()
    }

    private fun setupArguments() {
        val cliente = arguments?.getSerializable(ARG_CLIENTE) as? Cliente
        val rotaOrigem = arguments?.getSerializable(ARG_ROTA_ORIGEM) as? Rota
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
        viewModel.rotasDisponiveis.observe(viewLifecycleOwner) { rotas ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                rotas.map { it.nome }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRotaDestino.adapter = adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }

        binding.btnTransferir.setOnClickListener {
            val cliente = arguments?.getSerializable(ARG_CLIENTE) as? Cliente
            val rotaOrigem = arguments?.getSerializable(ARG_ROTA_ORIGEM) as? Rota
            val mesas = arguments?.getSerializable(ARG_MESAS) as? Array<Mesa>
            
            val selectedPosition = binding.spinnerRotaDestino.selectedItemPosition
            if (selectedPosition == Spinner.INVALID_POSITION) {
                Toast.makeText(requireContext(), "Selecione uma rota de destino", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val rotaDestinoNome = binding.spinnerRotaDestino.selectedItem.toString()

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
        viewModel.transferSuccess.observe(viewLifecycleOwner) { success ->
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

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
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

