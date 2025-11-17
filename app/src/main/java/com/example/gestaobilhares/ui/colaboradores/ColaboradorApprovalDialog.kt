package com.example.gestaobilhares.ui.colaboradores

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.databinding.DialogApproveColaboradorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

/**
 * Diálogo para aprovação de colaboradores com configuração de credenciais.
 * Permite definir email, senha temporária e nível de acesso.
 * ✅ CORREÇÃO: Convertido para DialogFragment para evitar erros de recursos XML
 */
class ColaboradorApprovalDialog : DialogFragment() {

    private var _binding: DialogApproveColaboradorBinding? = null
    private val binding get() = _binding!!

    private lateinit var colaborador: Colaborador
    private var onApprovalConfirmed: ((email: String, senha: String, nivelAcesso: NivelAcesso, observacoes: String) -> Unit)? = null

    companion object {
        private const val ARG_COLABORADOR = "colaborador"

        fun newInstance(
            colaborador: Colaborador,
            onApprovalConfirmed: (email: String, senha: String, nivelAcesso: NivelAcesso, observacoes: String) -> Unit
        ): ColaboradorApprovalDialog {
            return ColaboradorApprovalDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_COLABORADOR, colaborador)
                }
                this.onApprovalConfirmed = onApprovalConfirmed
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            colaborador = it.getSerializable(ARG_COLABORADOR) as? Colaborador
                ?: throw IllegalStateException("Colaborador não fornecido")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogApproveColaboradorBinding.inflate(layoutInflater)
        
        // Definir título dinâmico
        val title = "Aprovar ${colaborador.nome}"
        binding.root.findViewById<android.widget.TextView>(R.id.tvTitle)?.text = title
        
        setupDropdowns()
        setupClickListeners()
        preencherDadosIniciais()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupDropdowns() {
        // Configurar dropdown de nível de acesso
        val niveisAcesso = NivelAcesso.values().map { nivel ->
            when (nivel) {
                NivelAcesso.ADMIN -> "Administrador"
                NivelAcesso.USER -> "Colaborador"
            }
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, niveisAcesso)
        binding.spinnerNivelAcesso.setAdapter(adapter)
        binding.spinnerNivelAcesso.setText("Colaborador", false) // Padrão
    }

    private fun setupClickListeners() {
        // Botão Cancelar
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }

        // Botão Aprovar
        binding.btnAprovar.setOnClickListener {
            if (validarCampos()) {
                val email = binding.etEmailAcesso.text.toString().trim()
                val senha = binding.etSenhaTemporaria.text.toString().trim()
                val nivelAcesso = obterNivelAcessoSelecionado()
                val observacoes = binding.etObservacoes.text.toString().trim()
                
                onApprovalConfirmed?.invoke(email, senha, nivelAcesso, observacoes)
                dismiss()
            }
        }
    }

    private fun preencherDadosIniciais() {
        // Preencher email com sugestão baseada no nome
        val emailSugerido = gerarEmailSugerido(colaborador.nome)
        binding.etEmailAcesso.setText(emailSugerido)
        
        // Gerar senha temporária
        val senhaTemporaria = gerarSenhaTemporaria()
        binding.etSenhaTemporaria.setText(senhaTemporaria)
        
        // Preencher observações padrão
        val observacoesPadrao = "Colaborador aprovado em ${Date()}. Credenciais temporárias geradas automaticamente."
        binding.etObservacoes.setText(observacoesPadrao)
    }

    private fun validarCampos(): Boolean {
        val email = binding.etEmailAcesso.text.toString().trim()
        val senha = binding.etSenhaTemporaria.text.toString().trim()
        val nivelAcesso = binding.spinnerNivelAcesso.text.toString()

        if (email.isEmpty()) {
            binding.etEmailAcesso.error = "Email é obrigatório"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmailAcesso.error = "Email inválido"
            return false
        }

        if (senha.isEmpty()) {
            binding.etSenhaTemporaria.error = "Senha é obrigatória"
            return false
        }

        if (senha.length < 6) {
            binding.etSenhaTemporaria.error = "Senha deve ter pelo menos 6 caracteres"
            return false
        }

        if (nivelAcesso.isEmpty()) {
            binding.spinnerNivelAcesso.error = "Nível de acesso é obrigatório"
            return false
        }

        return true
    }

    private fun obterNivelAcessoSelecionado(): NivelAcesso {
        return when (binding.spinnerNivelAcesso.text.toString()) {
            "Administrador" -> NivelAcesso.ADMIN
            else -> NivelAcesso.USER
        }
    }

    private fun gerarEmailSugerido(nome: String): String {
        val nomeLimpo = nome.lowercase()
            .replace(" ", ".")
            .replace(Regex("[^a-z.]"), "")
        
        return "$nomeLimpo@gestaobilhares.com"
    }

    private fun gerarSenhaTemporaria(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val senha = StringBuilder()
        val random = Random()
        
        // Garantir pelo menos uma letra maiúscula, uma minúscula e um número
        senha.append(('A'..'Z').random())
        senha.append(('a'..'z').random())
        senha.append(('0'..'9').random())
        
        // Completar com caracteres aleatórios
        repeat(5) {
            senha.append(caracteres[random.nextInt(caracteres.length)])
        }
        
        return senha.toString()
    }
}
