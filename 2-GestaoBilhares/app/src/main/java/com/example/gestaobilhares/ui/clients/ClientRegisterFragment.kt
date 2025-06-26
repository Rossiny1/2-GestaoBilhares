package com.example.gestaobilhares.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.databinding.FragmentClientRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment para cadastro de novos clientes
 * FASE 4B - Recursos Avançados
 */
@AndroidEntryPoint
class ClientRegisterFragment : Fragment() {

    private var _binding: FragmentClientRegisterBinding? = null
    private val binding get() = _binding!!

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
        
        setupUI()
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
        
        // Simular salvamento (TODO: Implementar com ViewModel e Repository)
        saveClientData(name, address, phone, email, observations)
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun saveClientData(name: String, address: String, phone: String, email: String, observations: String) {
        // TODO: Salvar observations no banco de dados
        // TODO: Implementar salvamento completo incluindo observations
        // Simular delay de rede
        binding.root.postDelayed({
            binding.progressBar.visibility = View.GONE
            binding.btnSave.isEnabled = true
            
            // Mostrar sucesso
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("✅ Cliente Cadastrado!")
                .setMessage("""
                    Cliente cadastrado com sucesso!
                    
                    📝 Nome: $name
                    📍 Endereço: $address
                    ${if (phone.isNotEmpty()) "📞 Telefone: $phone\n" else ""}
                    ${if (email.isNotEmpty()) "📧 E-mail: $email\n" else ""}
                    ${if (observations.isNotEmpty()) "📋 Observações: $observations\n" else ""}
                    
                    🚀 Próximo passo: Vincular mesas ao cliente
                """.trimIndent())
                .setPositiveButton("Concluir") { _, _ ->
                    findNavController().popBackStack()
                }
                .setNegativeButton("Cadastrar Outro", null)
                .show()
        }, 1500)
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