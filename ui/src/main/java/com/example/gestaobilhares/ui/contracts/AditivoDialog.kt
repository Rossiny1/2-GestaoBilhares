package com.example.gestaobilhares.ui.contracts

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.ui.databinding.DialogAditivoEquipamentosBinding
import androidx.appcompat.app.AlertDialog
import timber.log.Timber

/**
 * Diálogo para confirmar a geração de aditivo contratual
 * quando um cliente já possui contrato ativo e deseja adicionar novos equipamentos
 * ✅ VERSÃO REFEITA: Focada em estabilidade absoluta e resolução de temas.
 */
class AditivoDialog : DialogFragment() {
    
    private var _binding: DialogAditivoEquipamentosBinding? = null
    private val binding get() = _binding!!
    
    private var onGerarAditivoClickListener: ((ContratoLocacao) -> Unit)? = null
    private var onCancelarClickListener: (() -> Unit)? = null
    
    private var contrato: ContratoLocacao? = null
    
    companion object {
        private const val TAG = "AditivoDialog"
        private const val ARG_CONTRATO = "contrato"
        
        fun newInstance(contrato: ContratoLocacao): AditivoDialog {
            val args = Bundle().apply {
                putSerializable(ARG_CONTRATO, contrato)
            }
            return AditivoDialog().apply {
                arguments = args
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ Definir o estilo do diálogo para o tema robusto local
        setStyle(STYLE_NO_TITLE, R.style.AditivoDialogTheme)
        
        arguments?.let { args ->
            contrato = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getSerializable(ARG_CONTRATO, ContratoLocacao::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getSerializable(ARG_CONTRATO) as? ContratoLocacao
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.i("[$TAG] Criando diálogo de aditivo - Versão Estabilidade Máxima")
        
        // Usar o layoutInflater do diálogo (que já possui o tema setado no setStyle)
        _binding = DialogAditivoEquipamentosBinding.inflate(layoutInflater)
        
        setupUI()
        setupClickListeners()
        
        // ✅ CORREÇÃO: Usar AlertDialog padrão ao invés de MaterialAlertDialogBuilder
        // Isso evita problemas de compatibilidade com tema AppCompat e é mais estável
        return AlertDialog.Builder(requireContext(), R.style.AditivoDialogTheme)
            .setView(binding.root)
            .setCancelable(false)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setCanceledOnTouchOutside(false)
            }
    }
    
    private fun setupUI() {
        contrato?.let { contrato ->
            binding.tvContratoInfo.text = "Contrato: ${contrato.numeroContrato}"
        }
    }
    
    private fun setupClickListeners() {
        binding.btnGerarAditivo.setOnClickListener {
            contrato?.let { contrato ->
                Timber.i("[$TAG] Botão 'Gerar Aditivo' clicado")
                onGerarAditivoClickListener?.invoke(contrato)
            }
        }
        
        binding.btnCancelar.setOnClickListener {
            Timber.i("[$TAG] Botão 'Cancelar' clicado")
            onCancelarClickListener?.invoke()
            dismiss()
        }
    }
    
    fun setOnGerarAditivoClickListener(listener: (ContratoLocacao) -> Unit) {
        onGerarAditivoClickListener = listener
    }
    
    fun setOnCancelarClickListener(listener: () -> Unit) {
        onCancelarClickListener = listener
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
