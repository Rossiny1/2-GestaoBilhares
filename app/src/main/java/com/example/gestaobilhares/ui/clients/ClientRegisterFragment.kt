package com.example.gestaobilhares.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.databinding.FragmentClientRegisterBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repositories.ClienteRepository
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Fragment para cadastro de novos clientes
 * FASE 4B - Recursos Avançados
 */
class ClientRegisterFragment : Fragment() {

    private var _binding: FragmentClientRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ClientRegisterViewModel
    private val args: ClientRegisterFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        viewModel = ClientRegisterViewModel(ClienteRepository(AppDatabase.getDatabase(requireContext()).clienteDao()))
        
        setupUI()
        observeViewModel()
        
        // Carregar dados do cliente se estiver editando
        if (args.clienteId != 0L) {
            Log.d("ClientRegisterFragment", "Modo EDIÇÃO - Carregando cliente ID: ${args.clienteId}")
            carregarDadosClienteParaEdicao()
        } else {
            Log.d("ClientRegisterFragment", "Modo NOVO CADASTRO")
        }
    }

    private fun setupUI() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão cancelar
        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão salvar
        binding.btnSave.setOnClickListener {
            saveClient()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.novoClienteId.collect { clienteId ->
                if (clienteId != null && clienteId > 0) {
                    try {
                        if (requireActivity().isFinishing || requireActivity().isDestroyed) {
                            return@collect
                        }
                        
                        if (!isAdded) {
                            return@collect
                        }
                        
                        // Ocultar loading
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        
                        if (parentFragmentManager.isStateSaved) {
                            return@collect
                        }
                        
                        // Mostrar dialog de sucesso para NOVO cadastro
                        if (isAdded && !requireActivity().isFinishing) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("✅ Cliente Cadastrado!")
                                .setMessage("Cliente cadastrado com sucesso!\n\nPróximo passo: Vincular mesas ao cliente.")
                                .setPositiveButton("Adicionar Mesa") { _, _ ->
                                    val action = ClientRegisterFragmentDirections.actionClientRegisterFragmentToMesasDepositoFragment(clienteId)
                                    findNavController().navigate(action)
                                    viewModel.resetNovoClienteId()
                                }
                                .setNegativeButton("Voltar") { _, _ ->
                                    findNavController().popBackStack()
                                    viewModel.resetNovoClienteId()
                                }
                                .show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Usar try-catch para evitar crash
                        try {
                            showErrorDialog("Erro ao exibir confirmação: ${e.localizedMessage}")
                        } catch (dialogException: Exception) {
                            // Se não conseguir mostrar o dialog, pelo menos logar o erro
                            android.util.Log.e("ClientRegister", "Erro ao mostrar dialog: ${dialogException.message}")
                        }
                    }
                }
            }
        }
        
        // ✅ NOVO: Observer para cliente atualizado (edição)
        lifecycleScope.launch {
            viewModel.clienteAtualizado.collect { atualizado ->
                if (atualizado) {
                    try {
                        if (requireActivity().isFinishing || requireActivity().isDestroyed) {
                            return@collect
                        }
                        
                        if (!isAdded) {
                            return@collect
                        }
                        
                        // Ocultar loading
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        
                        if (parentFragmentManager.isStateSaved) {
                            return@collect
                        }
                        
                        // Mostrar dialog de sucesso para EDIÇÃO
                        if (isAdded && !requireActivity().isFinishing) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("✅ Cliente Atualizado!")
                                .setMessage("Dados do cliente foram atualizados com sucesso!")
                                .setPositiveButton("OK") { _, _ ->
                                    findNavController().popBackStack()
                                    viewModel.resetNovoClienteId()
                                }
                                .show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            showErrorDialog("Erro ao exibir confirmação: ${e.localizedMessage}")
                        } catch (dialogException: Exception) {
                            android.util.Log.e("ClientRegister", "Erro ao mostrar dialog: ${dialogException.message}")
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSave.isEnabled = !isLoading
            }
        }
        lifecycleScope.launch {
            viewModel.debitoAtual.collect { debito ->
                try {
                    binding.etDebitoAtual.setText(String.format("R$ %.2f", debito))
                } catch (e: Exception) {
                    // Ignorar erro se a view foi destruída
                }
            }
        }
    }

    private fun showErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Erro")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveClient() {
        try {
            android.util.Log.d("ClientRegister", "Iniciando salvamento do cliente")
            
            // Limpar erros anteriores
            clearErrors()
            
            // Obter valores dos campos
            val name = binding.etClientName.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val valorFicha = binding.etValorFicha.text.toString().trim().toDoubleOrNull() ?: 0.0
            val comissaoFicha = binding.etComissaoFicha.text.toString().trim().toDoubleOrNull() ?: 0.0
            val numeroContrato = binding.etNumeroContrato.text.toString().trim()
            val observations = binding.etObservations.text.toString().trim()
            
            android.util.Log.d("ClientRegister", "Valores obtidos: nome=$name, endereco=$address")
            
            // Validações
            if (name.isEmpty()) {
                binding.etClientName.error = "Nome é obrigatório"
                binding.etClientName.requestFocus()
                return
            }
            
            if (name.length < 3) {
                binding.etClientName.error = "Nome deve ter pelo menos 3 caracteres"
                binding.etClientName.requestFocus()
                return
            }
            
            if (address.isEmpty()) {
                binding.etAddress.error = "Endereço é obrigatório"
                binding.etAddress.requestFocus()
                return
            }
            
            if (address.length < 10) {
                binding.etAddress.error = "Endereço deve ser mais detalhado"
                binding.etAddress.requestFocus()
                return
            }
            
            // Validação opcional de email
            if (email.isNotEmpty() && !isValidEmail(email)) {
                binding.etEmail.error = "E-mail inválido"
                binding.etEmail.requestFocus()
                return
            }
            
            // Validação opcional de telefone
            if (phone.isNotEmpty() && phone.length < 10) {
                binding.etPhone.error = "Telefone inválido"
                binding.etPhone.requestFocus()
                return
            }
            
            android.util.Log.d("ClientRegister", "Validações passaram, criando entidade Cliente")
            
            // Mostrar loading
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSave.isEnabled = false
            
            // Criar entidade Cliente
            val cliente = com.example.gestaobilhares.data.entities.Cliente(
                nome = name,
                endereco = address,
                telefone = phone,
                email = email,
                valorFicha = valorFicha,
                comissaoFicha = comissaoFicha,
                numeroContrato = if (numeroContrato.isNotEmpty()) numeroContrato else null,
                observacoes = observations,
                rotaId = if (args.rotaId != 0L) args.rotaId else {
                    // ✅ CORREÇÃO: Em modo edição, usar rotaId do cliente existente
                    viewModel.clienteParaEdicao.value?.rotaId ?: 1L
                }
            )
            
            android.util.Log.d("ClientRegister", "Entidade Cliente criada, chamando viewModel.cadastrarCliente")
            viewModel.cadastrarCliente(cliente)
            
        } catch (e: Exception) {
            android.util.Log.e("ClientRegister", "Erro ao salvar cliente: ${e.message}", e)
            // Tratar erro e restaurar UI
            binding.progressBar.visibility = View.GONE
            binding.btnSave.isEnabled = true
            showErrorDialog("Erro ao salvar cliente: ${e.localizedMessage}")
        }
    }
    
    private fun clearErrors() {
        binding.etClientName.error = null
        binding.etAddress.error = null
        binding.etPhone.error = null
        binding.etEmail.error = null
        binding.etValorFicha.error = null
        binding.etComissaoFicha.error = null
        binding.etNumeroContrato.error = null
        binding.etObservations.error = null
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun carregarDebitoAtual() {
        // TODO: Implementar busca do débito atual do último acerto
        // Por enquanto, mostrar 0,00
        viewModel.carregarDebitoAtual(null)
    }

    /**
     * ✅ IMPLEMENTADO: Carrega dados do cliente para edição
     */
    private fun carregarDadosClienteParaEdicao() {
        viewModel.carregarClienteParaEdicao(args.clienteId)
        
        // Observer para preencher campos quando dados carregarem
        lifecycleScope.launch {
            viewModel.clienteParaEdicao.collect { cliente ->
                cliente?.let { preencherCamposComDadosCliente(it) }
            }
        }
    }
    
    /**
     * ✅ IMPLEMENTADO: Preenche campos com dados do cliente
     */
    private fun preencherCamposComDadosCliente(cliente: com.example.gestaobilhares.data.entities.Cliente) {
        binding.apply {
            etClientName.setText(cliente.nome)
            etAddress.setText(cliente.endereco ?: "")
            etPhone.setText(cliente.telefone ?: "")
            etEmail.setText(cliente.email ?: "")
            etValorFicha.setText(cliente.valorFicha.toString())
            etComissaoFicha.setText(cliente.comissaoFicha.toString())
            etNumeroContrato.setText(cliente.numeroContrato ?: "")
            etObservations.setText(cliente.observacoes ?: "")
            
            // Alterar título para modo edição
            // TODO: Adicionar TextView para título no layout se não existir
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 