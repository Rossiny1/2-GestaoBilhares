package com.example.gestaobilhares.ui.auth
import com.example.gestaobilhares.ui.R

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
import com.example.gestaobilhares.ui.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
/**
 * Fragmento responsável pela tela de login com Firebase Authentication.
 * Utiliza ViewBinding, ViewModel e navegação segura.
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("LoginFragment", "🚨 LOGINFRAGMENT ONCREATE CHAMADO")
        Timber.d("LoginFragment", "🚨 SavedInstanceState: ${savedInstanceState != null}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("LoginFragment", "🚨 LOGINFRAGMENT ONCREATEVIEW CHAMADO")
        Timber.d("LoginFragment", "🚨 Container: ${container?.javaClass?.simpleName}")
        Timber.d("LoginFragment", "🚨 SavedInstanceState: ${savedInstanceState != null}")
        
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        Timber.d("LoginFragment", "✅ Binding criado com sucesso")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Timber.d("LoginFragment", "🚨 LOGINFRAGMENT ONVIEWCREATED CHAMADO")
        Timber.d("LoginFragment", "🚨 Context: ${requireContext()}")
        Timber.d("LoginFragment", "🚨 View: ${view.javaClass.simpleName}")

        try {
            Timber.d("LoginFragment", "=== INICIANDO LOGINFRAGMENT ===")
            
            // ✅ CORREÇÃO: ViewModel injetado via Hilt, não precisa inicializar manualmente
            // authViewModel injetado automaticamente pelo Hilt
            Timber.d("LoginFragment", "✅ AuthViewModel injetado via Hilt")
            
            // Não precisa mais chamar initializeRepository - Hilt injeta dependências
            // authViewModel.initializeRepository(requireContext()) // Removido - Hilt injeta dependências
            Timber.d("LoginFragment", "✅ Dependências injetadas via Hilt")
            
            setupClickListeners()
            Timber.d("LoginFragment", "✅ Click listeners configurados")
            
            observeAuthState()
            Timber.d("LoginFragment", "✅ Observers configurados")
            
            Timber.d("LoginFragment", "✅ LoginFragment inicializado com sucesso")
        } catch (e: Exception) {
            Timber.e("LoginFragment", "ERRO CRÍTICO ao inicializar LoginFragment: ${e.message}")
            Timber.e("LoginFragment", "Stack trace: ${e.stackTraceToString()}")
            // Mostrar mensagem de erro para o usuário
            android.widget.Toast.makeText(requireContext(), "Erro crítico ao inicializar o app. Reinicie o aplicativo.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Configura os listeners dos botões
     */
    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            android.util.Log.d("LoginFragment", "═══════════════════════════════════════")
            android.util.Log.d("LoginFragment", "🔘 BOTÃO LOGIN CLICADO")
            android.util.Log.d("LoginFragment", "═══════════════════════════════════════")
            Timber.d("LoginFragment", "🔘 BOTÃO LOGIN CLICADO")
            
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            
            android.util.Log.d("LoginFragment", "Email: $email")
            android.util.Log.d("LoginFragment", "Senha: ${password.length} caracteres")
            Timber.d("LoginFragment", "Email: $email, Senha: ${password.length} caracteres")
            
            android.util.Log.d("LoginFragment", "Chamando authViewModel.login()...")
            Timber.d("LoginFragment", "Chamando authViewModel.login()...")
            
            try {
                authViewModel.login(email, password)
                android.util.Log.d("LoginFragment", "✅ authViewModel.login() chamado com sucesso")
                Timber.d("LoginFragment", "✅ authViewModel.login() chamado com sucesso")
            } catch (e: Exception) {
                android.util.Log.e("LoginFragment", "❌ ERRO ao chamar authViewModel.login(): ${e.message}")
                android.util.Log.e("LoginFragment", "Stack: ${e.stackTraceToString()}")
                Timber.e(e, "LoginFragment", "❌ ERRO ao chamar authViewModel.login(): ${e.message}")
            }
        }

        binding.forgotPasswordTextView.setOnClickListener {
            // ✅ NOVO: Implementar recuperação de senha
            mostrarDialogoRecuperacaoSenha()
        }
    }
    
    /**
     * Observa mudanças no estado de autenticação e UI do login
     */
    private fun observeAuthState() {
        // ✅ REFATORAÇÃO: Observar apenas loginUiState para decisão de acesso
        // A UI não decide aprovação com base em AuthStateListener
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.loginUiState.collect { uiState ->
                    when (uiState) {
                        is LoginUiState.Loading -> {
                            // Estado de loading já é gerenciado pelo ViewModel
                            Timber.d("LoginFragment", "🔄 [UI] Estado: Loading")
                        }
                        is LoginUiState.Aprovado -> {
                            // ✅ Aprovado - navegar para home
                            // ✅ CORREÇÃO CRÍTICA: Verificar se este fragment está visível e ativo antes de navegar
                            if (!viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                Timber.w("LoginFragment", "⚠️ [UI] Fragment não está ativo, ignorando navegação")
                                return@collect
                            }
                            
                            Timber.d("LoginFragment", "✅ [UI] Colaborador APROVADO - navegando para home")
                            try {
                                val navController = findNavController()
                                val currentDestination = navController.currentDestination?.id
                                
                                Timber.d("LoginFragment", "   Destino atual: $currentDestination")
                                android.util.Log.d("LoginFragment", "   Destino atual: $currentDestination")
                                
                                when (currentDestination) {
                                    com.example.gestaobilhares.ui.R.id.loginFragment -> {
                                        // Se estiver no LoginFragment, usar ação direta
                                        Timber.d("LoginFragment", "   Navegando de loginFragment para routesFragment")
                                        android.util.Log.d("LoginFragment", "   Navegando de loginFragment para routesFragment")
                                        navController.navigate(com.example.gestaobilhares.ui.R.id.action_loginFragment_to_routesFragment)
                                    }
                                    com.example.gestaobilhares.ui.R.id.changePasswordFragment -> {
                                        // Se estiver no ChangePasswordFragment, usar ação específica desse fragment
                                        Timber.d("LoginFragment", "   Navegando de changePasswordFragment para routesFragment")
                                        android.util.Log.d("LoginFragment", "   Navegando de changePasswordFragment para routesFragment")
                                        navController.navigate(com.example.gestaobilhares.ui.R.id.action_changePasswordFragment_to_routesFragment)
                                    }
                                    else -> {
                                        Timber.w("LoginFragment", "⚠️ [UI] Destino atual ($currentDestination) não é loginFragment nem changePasswordFragment")
                                        android.util.Log.w("LoginFragment", "⚠️ [UI] Destino atual ($currentDestination) não é loginFragment nem changePasswordFragment")
                                        // ✅ CORREÇÃO: Tentar usar ação global ou popBackStack até encontrar loginFragment
                                        try {
                                            // Primeiro, tentar voltar para loginFragment
                                            val popped = navController.popBackStack(com.example.gestaobilhares.ui.R.id.loginFragment, false)
                                            if (popped) {
                                                Timber.d("LoginFragment", "   Voltou para loginFragment, navegando agora")
                                                navController.navigate(com.example.gestaobilhares.ui.R.id.action_loginFragment_to_routesFragment)
                                            } else {
                                                // Se não conseguiu voltar, tentar navegação direta para routesFragment
                                                Timber.d("LoginFragment", "   Tentando navegação direta para routesFragment")
                                                navController.navigate(com.example.gestaobilhares.ui.R.id.routesFragment)
                                            }
                                        } catch (e2: Exception) {
                                            Timber.e(e2, "LoginFragment", "❌ [UI] Erro na navegação alternativa: ${e2.message}")
                                            android.util.Log.e("LoginFragment", "❌ [UI] Erro na navegação alternativa: ${e2.message}", e2)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "LoginFragment", "❌ [UI] Erro ao navegar para routesFragment: ${e.message}")
                                android.util.Log.e("LoginFragment", "❌ [UI] Erro ao navegar: ${e.message}", e)
                            }
                        }
                        is LoginUiState.Pendente -> {
                            // ✅ Pendente - mostrar mensagem (UI não decide, apenas exibe)
                            Timber.d("LoginFragment", "⏳ [UI] Colaborador PENDENTE - mostrando mensagem")
                            Toast.makeText(
                                requireContext(),
                                "Sua conta está aguardando aprovação do administrador.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is LoginUiState.Erro -> {
                            // ✅ Erro - mostrar mensagem
                            Timber.e("LoginFragment", "❌ [UI] Erro: ${uiState.mensagem}")
                            Toast.makeText(
                                requireContext(),
                                uiState.mensagem,
                                Toast.LENGTH_LONG
                            ).show()
                            uiState.exception?.printStackTrace()
                        }
                        is LoginUiState.PrimeiroAcesso -> {
                            // ✅ Redirecionar para tela de alteração de senha obrigatória
                            Timber.d("LoginFragment", "🔐 [UI] Primeiro acesso detectado via LoginUiState. Navegando para ChangePasswordFragment...")
                            findNavController().navigate(
                                com.example.gestaobilhares.ui.R.id.action_loginFragment_to_changePasswordFragment
                            )
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
                        authViewModel.clearErrorMessage()
                    }
                }
            }
        }

        // Observa mensagens de sucesso (ex: cadastro pendente)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.message.collect { message ->
                    if (!message.isNullOrEmpty()) {
                        // ✅ CORREÇÃO: Diálogo com título apropriado para cadastro
                        val title = if (message.contains("Cadastro realizado", ignoreCase = true) || 
                                       message.contains("Conta criada", ignoreCase = true)) {
                            "Cadastro Realizado"
                        } else {
                            "Informação"
                        }
                        
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle(title)
                            .setMessage(message)
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                authViewModel.clearMessage()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
            }
        }

        // Observa estado de loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.isLoading.collect { isLoading ->
                    binding.loginButton.isEnabled = !isLoading
                    binding.forgotPasswordTextView.isEnabled = !isLoading
                    
                    // Mostrar/esconder loading
                    if (isLoading) {
                        binding.loginButton.text = "Entrando..."
                    } else {
                        binding.loginButton.text = "Entrar"
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

    /**
     * ✅ NOVO: Mostra diálogo para recuperação de senha
     */
    private fun mostrarDialogoRecuperacaoSenha() {
        val editText = android.widget.EditText(requireContext())
        editText.hint = "Digite seu email"
        editText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Recuperar Senha")
            .setMessage("Digite seu email para receber instruções de recuperação de senha")
            .setView(editText)
            .setPositiveButton("Enviar") { _, _ ->
                val email = editText.text.toString().trim()
                if (email.isNotEmpty()) {
                    authViewModel.resetPassword(email)
                } else {
                    Toast.makeText(requireContext(), "Email é obrigatório", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
