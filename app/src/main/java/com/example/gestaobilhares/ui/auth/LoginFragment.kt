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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("LoginFragment", "🚨 LOGINFRAGMENT ONCREATE CHAMADO")
        android.util.Log.d("LoginFragment", "🚨 SavedInstanceState: ${savedInstanceState != null}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        android.util.Log.d("LoginFragment", "🚨 LOGINFRAGMENT ONCREATEVIEW CHAMADO")
        android.util.Log.d("LoginFragment", "🚨 Container: ${container?.javaClass?.simpleName}")
        android.util.Log.d("LoginFragment", "🚨 SavedInstanceState: ${savedInstanceState != null}")
        
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        android.util.Log.d("LoginFragment", "✅ Binding criado com sucesso")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("LoginFragment", "🚨 LOGINFRAGMENT ONVIEWCREATED CHAMADO")
        android.util.Log.d("LoginFragment", "🚨 Context: ${requireContext()}")
        android.util.Log.d("LoginFragment", "🚨 View: ${view.javaClass.simpleName}")

        try {
            android.util.Log.d("LoginFragment", "=== INICIANDO LOGINFRAGMENT ===")
            
            // ✅ CORREÇÃO: Inicializar ViewModel corretamente
            authViewModel = AuthViewModel()
            android.util.Log.d("LoginFragment", "✅ AuthViewModel criado")
            
            // Inicializar repositório local de forma segura
            android.util.Log.d("LoginFragment", "🔧 CHAMANDO initializeRepository...")
            authViewModel.initializeRepository(requireContext())
            android.util.Log.d("LoginFragment", "✅ Repositório inicializado")
            
            // Configurar Google Sign-In
            setupGoogleSignIn()
            android.util.Log.d("LoginFragment", "✅ Google Sign-In configurado")
            
            setupClickListeners()
            android.util.Log.d("LoginFragment", "✅ Click listeners configurados")
            
            observeAuthState()
            android.util.Log.d("LoginFragment", "✅ Observers configurados")
            
            android.util.Log.d("LoginFragment", "✅ LoginFragment inicializado com sucesso")
        } catch (e: Exception) {
            android.util.Log.e("LoginFragment", "ERRO CRÍTICO ao inicializar LoginFragment: ${e.message}")
            android.util.Log.e("LoginFragment", "Stack trace: ${e.stackTraceToString()}")
            // Mostrar mensagem de erro para o usuário
            android.widget.Toast.makeText(requireContext(), "Erro crítico ao inicializar o app. Reinicie o aplicativo.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Configura o Google Sign-In
     */
    private fun setupGoogleSignIn() {
        try {
            android.util.Log.d("LoginFragment", "=== CONFIGURANDO GOOGLE SIGN-IN ===")
            
            // ✅ CORREÇÃO: Configuração mais simples e robusta
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("1089459035145-d55o1h307gaedp4v03cuchr6s6nn2lhg.apps.googleusercontent.com")
                .build()
            
            android.util.Log.d("LoginFragment", "✅ GoogleSignInOptions criado")
            
            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            
            android.util.Log.d("LoginFragment", "✅ GoogleSignInClient criado")
            android.util.Log.d("LoginFragment", "Google Sign-In configurado com sucesso")
            android.util.Log.d("LoginFragment", "Web Client ID: 1089459035145-d55o1h307gaedp4v03cuchr6s6nn2lhg.apps.googleusercontent.com")
        } catch (e: Exception) {
            android.util.Log.e("LoginFragment", "ERRO ao configurar Google Sign-In: ${e.message}")
            android.util.Log.e("LoginFragment", "Stack trace: ${e.stackTraceToString()}")
            // Continuar sem Google Sign-In (modo offline)
        }
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
     * ✅ CORREÇÃO: Método mais simples e robusto
     */
    private fun signInWithGoogle() {
        try {
            android.util.Log.d("LoginFragment", "=== INICIANDO GOOGLE SIGN-IN ===")
            
            // ✅ CORREÇÃO: Método mais simples - apenas iniciar o sign in
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
            android.util.Log.d("LoginFragment", "✅ Intent de seleção de conta iniciado")
        } catch (e: Exception) {
            android.util.Log.e("LoginFragment", "ERRO ao iniciar Google Sign-In: ${e.message}")
            Toast.makeText(requireContext(), "Erro ao iniciar login com Google", Toast.LENGTH_SHORT).show()
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
        // ✅ MODERNIZADO: Observa o estado de autenticação com StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { authState ->
                    when (authState) {
                        is AuthState.Authenticated -> {
                            // Navegar para a tela de rotas em caso de sucesso
                            findNavController().navigate(R.id.action_loginFragment_to_routesFragment)
                        }
                        AuthState.Unauthenticated -> {
                            // Manter na tela de login
                        }
                    }
                }
            }
        }

        // ✅ MODERNIZADO: Observa mensagens de erro com StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.errorMessage.collect { message ->
                    if (!message.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Observa estado de loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.isLoading.collect { isLoading ->
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
                }
            }
        }
        
        // ✅ MODERNIZADO: Observa estado de conexão com StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.isOnline.collect { isOnline ->
                    if (!isOnline) {
                        // Apenas mostrar toast quando estiver offline
                        Toast.makeText(requireContext(), "Modo offline ativo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
