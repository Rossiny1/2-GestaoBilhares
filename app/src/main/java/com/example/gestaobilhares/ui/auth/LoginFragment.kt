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
    private lateinit var authViewModel: AuthViewModel
    
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

        // ✅ CORREÇÃO: Inicializar ViewModel corretamente
        authViewModel = AuthViewModel()
        
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
     * ✅ CORREÇÃO: SEMPRE forçar seleção de conta para permitir múltiplos usuários
     */
    private fun signInWithGoogle() {
        android.util.Log.d("LoginFragment", "=== INICIANDO GOOGLE SIGN-IN ===")
        
        // ✅ CORREÇÃO CRÍTICA: SEMPRE fazer sign out primeiro para forçar seleção de conta
        // Isso garante que o usuário SEMPRE veja a tela de seleção de conta
        googleSignInClient.signOut().addOnCompleteListener {
            android.util.Log.d("LoginFragment", "✅ Sign out realizado - forçando seleção de conta")
            
            // Aguardar um pouco para garantir que o sign out foi processado
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                // Agora iniciar o processo de sign in que SEMPRE mostrará a seleção de conta
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
                android.util.Log.d("LoginFragment", "✅ Intent de seleção de conta iniciado")
            }, 500) // Aguardar 500ms
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
                                        android.util.Log.d("LoginFragment", "=== PROCESSANDO RESULTADO DO GOOGLE SIGN-IN ===")
                
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                
                android.util.Log.d("LoginFragment", "✅ CONTA SELECIONADA:")
                android.util.Log.d("LoginFragment", "   Email: ${account.email}")
                android.util.Log.d("LoginFragment", "   Nome: ${account.displayName}")
                android.util.Log.d("LoginFragment", "   ID: ${account.id}")
                
                // ✅ NOVO: Verificar se é uma conta diferente da anterior
                val lastAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (lastAccount != null && lastAccount.email != account.email) {
                    android.util.Log.d("LoginFragment", "🔄 CONTA DIFERENTE SELECIONADA!")
                    android.util.Log.d("LoginFragment", "   Conta anterior: ${lastAccount.email}")
                    android.util.Log.d("LoginFragment", "   Nova conta: ${account.email}")
                }
                
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
        
        // Observa estado de conexão (status removido da UI conforme solicitado)
        authViewModel.isOnline.observe(viewLifecycleOwner, Observer { isOnline ->
            if (!isOnline) {
                // Apenas mostrar toast quando estiver offline
                Toast.makeText(requireContext(), "Modo offline ativo", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
