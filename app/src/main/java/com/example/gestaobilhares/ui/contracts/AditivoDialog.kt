package com.example.gestaobilhares.ui.contracts

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.databinding.DialogAditivoEquipamentosBinding

/**
 * Diálogo para confirmar a geração de aditivo contratual
 * quando um cliente já possui contrato ativo e deseja adicionar novos equipamentos
 */
class AditivoDialog : DialogFragment() {
    
    private var _binding: DialogAditivoEquipamentosBinding? = null
    private val binding get() = _binding!!
    
    private var onGerarAditivoClickListener: ((ContratoLocacao) -> Unit)? = null
    private var onCancelarClickListener: (() -> Unit)? = null
    
    private var contrato: ContratoLocacao? = null
    
    companion object {
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
        setStyle(STYLE_NORMAL, R.style.DarkDialogTheme)
        
        arguments?.let { args ->
            contrato = args.getSerializable(ARG_CONTRATO) as? ContratoLocacao
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAditivoEquipamentosBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        contrato?.let { contrato ->
            binding.tvContratoInfo.text = "Contrato: ${contrato.numeroContrato}"
        }
    }
    
    private fun setupClickListeners() {
        binding.btnGerarAditivo.setOnClickListener {
            contrato?.let { contrato ->
                onGerarAditivoClickListener?.invoke(contrato)
            }
            dismiss()
        }
        
        binding.btnCancelar.setOnClickListener {
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
