package com.example.gestaobilhares.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.viewModels
import kotlinx.coroutines.launch
import com.example.gestaobilhares.ui.databinding.FragmentChangePasswordBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragmento responsável pela tela de alteração de senha obrigatória (primeiro acesso).
 * Exibido quando o usuário faz login pela primeira vez com senha temporária.
 */
@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            android.util.Log.d("ChangePasswordFragment", "=== INICIANDO CHANGEPASSWORDFRAGMENT ===")
            
            // ViewModel injetado via Hilt, não precisa inicializar manualmente
            // authViewModel injetado automaticamente via Hilt
            android.util.Log.d("ChangePasswordFragment", "✅ AuthViewModel injetado via Hilt")
            
            // Configurar observadores
            observeAuthState()
            observeErrorMessage()
            observeLoading()
            observeMessage()
            
            // Configurar campos de texto
            setupTextFields()
            
            // Configurar botão
            binding.changePasswordButton.setOnClickListener {
                changePassword()
            }
            
            android.util.Log.d("ChangePasswordFragment", "✅ ChangePasswordFragment configurado")
            
        } catch (e: Exception) {
            android.util.Log.e("ChangePasswordFragment", "❌ Erro ao configurar fragment: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao inicializar tela: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupTextFields() {
        // Adicionar validação em tempo real
        binding.newPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword()
            }
        })

        binding.confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword()
            }
        })
    }

    private fun validatePassword() {
        val newPassword = binding.newPasswordEditText.text?.toString() ?: ""
        val confirmPassword = binding.confirmPasswordEditText.text?.toString() ?: ""
        
        // Limpar mensagem de erro quando o usuário começar a digitar
        if (newPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
            binding.errorMessageTextView.visibility = View.GONE
        }
        
        // Validar comprimento mínimo
        if (newPassword.isNotEmpty() && newPassword.length < 8) {
            binding.newPasswordLayout.error = "Senha deve ter pelo menos 8 caracteres"
        } else {
            binding.newPasswordLayout.error = null
        }
        
        // Validar confirmação
        if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            binding.confirmPasswordLayout.error = "As senhas não coincidem"
        } else {
            binding.confirmPasswordLayout.error = null
        }
    }

    private fun changePassword() {
        val newPassword = binding.newPasswordEditText.text?.toString() ?: ""
        val confirmPassword = binding.confirmPasswordEditText.text?.toString() ?: ""
        
        // Validações básicas
        if (newPassword.isBlank()) {
            showError("Por favor, informe a nova senha")
            return
        }
        
        if (newPassword.length < 8) {
            showError("A senha deve ter pelo menos 8 caracteres")
            return
        }
        
        if (confirmPassword.isBlank()) {
            showError("Por favor, confirme a nova senha")
            return
        }
        
        if (newPassword != confirmPassword) {
            showError("As senhas não coincidem")
            return
        }
        
        // Chamar ViewModel para alterar senha
        android.util.Log.d("ChangePasswordFragment", "Alterando senha...")
        authViewModel.alterarSenha(newPassword, confirmPassword)
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { authState ->
                    when (authState) {
                        is AuthState.Authenticated -> {
                            android.util.Log.d("ChangePasswordFragment", "✅ Senha alterada com sucesso! Navegando para rotas...")
                            // Navegar para a tela de rotas após alteração bem-sucedida
                            findNavController().navigate(
                                com.example.gestaobilhares.ui.R.id.action_changePasswordFragment_to_routesFragment
                            )
                        }
                        is AuthState.FirstAccessRequired -> {
                            // Manter na tela de alteração de senha
                            android.util.Log.d("ChangePasswordFragment", "Aguardando alteração de senha...")
                        }
                        AuthState.Unauthenticated -> {
                            // ✅ CORREÇÃO: Verificar se há sessão ativa antes de voltar para login
                            // Quando o ChangePasswordFragment é criado, o AuthViewModel pode estar como Unauthenticated
                            // mas a sessão local pode estar ativa (primeiro acesso)
                            val userSessionManager = com.example.gestaobilhares.core.utils.UserSessionManager.getInstance(requireContext())
                            val colaboradorId = userSessionManager.getCurrentUserId()
                            
                            if (colaboradorId == 0L) {
                                android.util.Log.d("ChangePasswordFragment", "⚠️ Nenhuma sessão ativa. Voltando para login...")
                                findNavController().popBackStack()
                            } else {
                                android.util.Log.d("ChangePasswordFragment", "✅ Sessão ativa detectada (ID: $colaboradorId). Mantendo na tela de alteração de senha.")
                                // Não voltar para login se há sessão ativa (primeiro acesso)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeErrorMessage() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.errorMessage.collect { errorMessage ->
                    if (errorMessage != null && errorMessage.isNotEmpty()) {
                        showError(errorMessage)
                        authViewModel.clearErrorMessage()
                    }
                }
            }
        }
    }

    private fun observeLoading() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.changePasswordButton.isEnabled = !isLoading
                }
            }
        }
    }

    private fun observeMessage() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.message.collect { message ->
                    if (message != null && message.isNotEmpty()) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        authViewModel.clearMessage()
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.errorMessageTextView.text = message
        binding.errorMessageTextView.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

