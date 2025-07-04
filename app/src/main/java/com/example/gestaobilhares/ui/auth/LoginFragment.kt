package com.example.gestaobilhares.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentLoginBinding
/**
 * Fragmento responsável pela tela de login com Firebase Authentication.
 * Utiliza ViewBinding, ViewModel e navegação segura.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel = AuthViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeAuthState()
    }

    /**
     * Configura os listeners dos botões
     */
    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            authViewModel.login(email, password)
        }

        binding.forgotPasswordTextView.setOnClickListener {
            // TODO: Implementar recuperação de senha
            Toast.makeText(requireContext(), "Recuperação de senha será implementada em breve", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Observa mudanças no estado de autenticação
     */
    private fun observeAuthState() {
        // Observa o estado de autenticação
        authViewModel.authState.observe(viewLifecycleOwner, Observer { authState ->
            when (authState) {
                is AuthState.Authenticated -> {
                    // Navegar para a tela de rotas em caso de sucesso
                    findNavController().navigate(R.id.action_loginFragment_to_routesFragment)
                }
                AuthState.Unauthenticated -> {
                    // Manter na tela de login
                }
            }
        })

        // Observa mensagens de erro
        authViewModel.errorMessage.observe(viewLifecycleOwner, Observer { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        })

        // Observa estado de loading
        authViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.loginButton.isEnabled = !isLoading
            binding.forgotPasswordTextView.isEnabled = !isLoading
            
            // Mostrar/esconder loading
            if (isLoading) {
                binding.loginButton.text = "Entrando..."
            } else {
                binding.loginButton.text = "Entrar"
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
