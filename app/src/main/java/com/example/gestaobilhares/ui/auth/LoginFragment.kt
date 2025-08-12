package com.example.gestaobilhares.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
/**
 * Fragmento responsável pela tela de login com Firebase Authentication.
 * Utiliza ViewBinding, ViewModel e navegação segura.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel = AuthViewModel()
    
    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar repositório local
        authViewModel.initializeRepository(requireContext())
        
        // Configurar Google Sign-In
        setupGoogleSignIn()
        
        setupClickListeners()
        observeAuthState()
    }

    /**
     * Configura o Google Sign-In
     */
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1089459035145-d55o1h307gaedp4v03cuchr6s6nn2lhg.apps.googleusercontent.com")
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        
        android.util.Log.d("LoginFragment", "Google Sign-In configurado")
        android.util.Log.d("LoginFragment", "Web Client ID: 1089459035145-d55o1h307gaedp4v03cuchr6s6nn2lhg.apps.googleusercontent.com")
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

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.forgotPasswordTextView.setOnClickListener {
            // TODO: Implementar recuperação de senha
            Toast.makeText(requireContext(), "Recuperação de senha será implementada em breve", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Inicia o processo de login com Google
     * Sempre força a seleção de conta para permitir múltiplos usuários no mesmo dispositivo
     */
    private fun signInWithGoogle() {
        // Verificar se já há uma conta logada no Google
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        
        if (account != null) {
            // Se há uma conta logada, fazer sign out para forçar seleção
            googleSignInClient.signOut().addOnCompleteListener {
                android.util.Log.d("LoginFragment", "Sign out realizado - forçando seleção de conta")
                
                // Agora iniciar o processo de sign in que sempre mostrará a seleção de conta
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        } else {
            // Se não há conta logada, iniciar diretamente
            android.util.Log.d("LoginFragment", "Nenhuma conta Google logada - iniciando seleção")
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }
    
    /**
     * Processa o resultado do Google Sign-In
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        android.util.Log.d("LoginFragment", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        
        if (requestCode == RC_SIGN_IN) {
            try {
                android.util.Log.d("LoginFragment", "Processando resultado do Google Sign-In...")
                
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                
                android.util.Log.d("LoginFragment", "Conta obtida: ${account.email}")
                
                // Chamar o ViewModel para processar o login
                authViewModel.signInWithGoogle(account)
                
            } catch (e: ApiException) {
                android.util.Log.e("LoginFragment", "ApiException no Google Sign-In: ${e.statusCode}")
                
                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Login cancelado pelo usuário"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Erro de rede. Verifique sua conexão."
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Conta inválida"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Login necessário"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Falha no login. Tente novamente."
                    GoogleSignInStatusCodes.TIMEOUT -> "Timeout na conexão"
                    else -> "Erro no login com Google (Código: ${e.statusCode})"
                }
                
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                android.util.Log.e("LoginFragment", "Erro geral no Google Sign-In: ${e.message}")
                Toast.makeText(requireContext(), "Erro inesperado no login com Google", Toast.LENGTH_LONG).show()
            }
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
            binding.googleSignInButton.isEnabled = !isLoading
            binding.forgotPasswordTextView.isEnabled = !isLoading
            
            // Mostrar/esconder loading
            if (isLoading) {
                binding.loginButton.text = "Entrando..."
                binding.googleSignInButton.text = "Entrando..."
            } else {
                binding.loginButton.text = "Entrar"
                binding.googleSignInButton.text = "Entrar com Google"
            }
        })
        
        // Observa estado de conexão
        authViewModel.isOnline.observe(viewLifecycleOwner, Observer { isOnline ->
            if (isOnline) {
                binding.statusText.text = "Online"
                binding.statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                binding.statusIcon.setImageResource(android.R.drawable.ic_menu_share)
            } else {
                binding.statusText.text = "Offline"
                binding.statusText.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                binding.statusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                Toast.makeText(requireContext(), "Modo offline ativo", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
