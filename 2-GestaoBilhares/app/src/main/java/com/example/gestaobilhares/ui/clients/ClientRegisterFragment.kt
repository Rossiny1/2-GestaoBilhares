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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment para cadastro de novos clientes
 * FASE 4B - Recursos Avançados
 */
@AndroidEntryPoint
class ClientRegisterFragment : Fragment() {

    private var _binding: FragmentClientRegisterBinding? = null
    private val binding get() = _binding!!

    private val args: ClientRegisterFragmentArgs by navArgs()
    private val viewModel: ClientRegisterViewModel by viewModels()

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
        setupUI()
        observeViewModel()
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
            viewModel.novoClienteId.collect { id ->
                id?.let {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    try {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("\u2705 Cliente Cadastrado!")
                            .setMessage("Cliente cadastrado com sucesso!\n\nPróximo passo: Vincular mesas ao cliente.")
                            .setPositiveButton("Adicionar Mesa") { _, _ ->
                                val action = ClientRegisterFragmentDirections.actionClientRegisterFragmentToMesasDepositoFragment(it)
                                findNavController().navigate(action)
                                viewModel.resetNovoClienteId()
                            }
                            .setNegativeButton("Voltar") { _, _ ->
                                findNavController().popBackStack()
                                viewModel.resetNovoClienteId()
                            }
                            .show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showErrorDialog("Erro ao exibir confirmação: ${e.localizedMessage}")
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
    }

    private fun showErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Erro")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveClient() {
        // Limpar erros anteriores
        clearErrors()
        
        // Obter valores dos campos
        val name = binding.etClientName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val observations = binding.etObservations.text.toString().trim()
        
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
        
        // Mostrar loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        
        // Criar entidade Cliente
        val cliente = com.example.gestaobilhares.data.entities.Cliente(
            nome = name,
            endereco = address,
            telefone = phone,
            email = email,
            observacoes = observations,
            rotaId = args.rotaId,
            valorFicha = 0.0 // ou valor padrão
        )
        viewModel.cadastrarCliente(cliente)
    }
    
    private fun clearErrors() {
        binding.etClientName.error = null
        binding.etAddress.error = null
        binding.etPhone.error = null
        binding.etEmail.error = null
        binding.etObservations.error = null
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 