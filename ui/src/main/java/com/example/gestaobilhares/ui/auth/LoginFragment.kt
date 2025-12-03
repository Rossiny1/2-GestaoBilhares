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
/**
 * Fragmento responsável pela tela de login com Firebase Authentication.
 * Utiliza ViewBinding, ViewModel e navegação segura.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var authViewModel: AuthViewModel

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
     * Configura os listeners dos botões
     */
    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            authViewModel.login(email, password)
        }

        // ✅ NOVO: Botão de cadastro
        binding.registerButton.setOnClickListener {
            mostrarDialogoCadastro()
        }

        binding.forgotPasswordTextView.setOnClickListener {
            // ✅ NOVO: Implementar recuperação de senha
            mostrarDialogoRecuperacaoSenha()
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
                            findNavController().navigate(com.example.gestaobilhares.ui.R.id.action_loginFragment_to_routesFragment)
                        }
                        is AuthState.FirstAccessRequired -> {
                            // ✅ NOVO: Redirecionar para tela de alteração de senha obrigatória
                            android.util.Log.d("LoginFragment", "Primeiro acesso detectado. Navegando para ChangePasswordFragment...")
                            findNavController().navigate(
                                com.example.gestaobilhares.ui.R.id.action_loginFragment_to_changePasswordFragment
                            )
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
     * ✅ NOVO: Mostra diálogo para cadastro de novo usuário
     */
    private fun mostrarDialogoCadastro() {
        val dialogView = layoutInflater.inflate(com.example.gestaobilhares.ui.R.layout.dialog_register, null)
        
        val nomeEditText = dialogView.findViewById<android.widget.EditText>(com.example.gestaobilhares.ui.R.id.nomeEditText)
        val emailEditText = dialogView.findViewById<android.widget.EditText>(com.example.gestaobilhares.ui.R.id.emailEditText)
        val senhaEditText = dialogView.findViewById<android.widget.EditText>(com.example.gestaobilhares.ui.R.id.senhaEditText)
        val confirmarSenhaEditText = dialogView.findViewById<android.widget.EditText>(com.example.gestaobilhares.ui.R.id.confirmarSenhaEditText)
        
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Criar Conta")
            .setView(dialogView)
            .setPositiveButton("Cadastrar", null) // Definir listener depois para evitar fechamento automático
            .setNegativeButton("Cancelar", null)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val nome = nomeEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val senha = senhaEditText.text.toString()
                val confirmarSenha = confirmarSenhaEditText.text.toString()
                
                if (email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                    Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(requireContext(), "Email inválido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (senha.length < 6) {
                    Toast.makeText(requireContext(), "Senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (senha != confirmarSenha) {
                    Toast.makeText(requireContext(), "Senhas não coincidem", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                authViewModel.register(email, senha, confirmarSenha, nome)
                dialog.dismiss()
            }
        }
        
        dialog.show()
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
