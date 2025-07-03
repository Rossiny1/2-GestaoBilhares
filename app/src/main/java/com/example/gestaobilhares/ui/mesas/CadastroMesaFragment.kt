package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.entities.EstadoConservacao
import com.example.gestaobilhares.databinding.FragmentCadastroMesaBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class CadastroMesaFragment : Fragment() {
    private var _binding: FragmentCadastroMesaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CadastroMesaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadastroMesaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Configurar spinner de tipos
        val tipos = TipoMesa.values().map { formatTipoMesa(it) }
        val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTipoMesa.adapter = tipoAdapter

        // Configurar spinner de tamanhos
        val tamanhos = TamanhoMesa.values().map { formatTamanhoMesa(it) }
        val tamanhoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tamanhos)
        tamanhoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTamanhoMesa.adapter = tamanhoAdapter

        // Configurar spinner de estado de conservação
        val estados = EstadoConservacao.values().map { formatEstadoConservacao(it) }
        val estadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spEstadoConservacao.adapter = estadoAdapter

        binding.btnSalvarMesa.setOnClickListener {
            val numero = binding.etNumeroMesa.text.toString().trim()
            val tipoIndex = binding.spTipoMesa.selectedItemPosition
            val tamanhoIndex = binding.spTamanhoMesa.selectedItemPosition
            val estadoIndex = binding.spEstadoConservacao.selectedItemPosition
            val relogioStr = binding.etRelogio.text?.toString()?.trim()
            
            if (numero.isEmpty() || relogioStr.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val relogio = relogioStr.toIntOrNull()
            if (relogio == null) {
                binding.tilRelogio.error = "Valor inválido"
                return@setOnClickListener
            } else {
                binding.tilRelogio.error = null
            }
            
            val mesa = Mesa(
                numero = numero,
                clienteId = null,
                fichasInicial = relogio,
                fichasFinal = relogio,
                relogioInicial = relogio,
                relogioFinal = relogio,
                tipoMesa = TipoMesa.values()[tipoIndex],
                tamanho = TamanhoMesa.values()[tamanhoIndex],
                estadoConservacao = EstadoConservacao.values()[estadoIndex],
                ativa = true,
                observacoes = binding.etObservacoes.text?.toString()?.takeIf { it.isNotBlank() },
                dataInstalacao = Date(),
                dataUltimaLeitura = Date()
            )
            
            viewModel.salvarMesa(mesa)
            Toast.makeText(requireContext(), "Mesa cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
        
        binding.btnCancelar.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun formatTipoMesa(tipo: TipoMesa): String {
        return when (tipo) {
            TipoMesa.SINUCA -> "Sinuca"
            TipoMesa.MAQUINA_MUSICA -> "Máquina de Música"
            TipoMesa.PEMBOLIM -> "Pembolim"
            TipoMesa.SNOOKER -> "Snooker"
            TipoMesa.POOL -> "Pool"
            TipoMesa.BILHAR -> "Bilhar"
            TipoMesa.OUTROS -> "Outros"
        }
    }

    private fun formatTamanhoMesa(tamanho: TamanhoMesa): String {
        return when (tamanho) {
            TamanhoMesa.PEQUENA -> "Pequena"
            TamanhoMesa.GRANDE -> "Grande"
        }
    }

    private fun formatEstadoConservacao(estado: EstadoConservacao): String {
        return when (estado) {
            EstadoConservacao.OTIMO -> "Ótimo"
            EstadoConservacao.BOM -> "Bom"
            EstadoConservacao.RUIM -> "Ruim"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 