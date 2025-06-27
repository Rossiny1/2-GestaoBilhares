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
        val tipos = TipoMesa.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTipoMesa.adapter = adapter

        binding.btnSalvarMesa.setOnClickListener {
            val numero = binding.etNumeroMesa.text.toString().trim()
            val tamanho = binding.etTamanhoMesa.text.toString().trim()
            val tipo = TipoMesa.valueOf(binding.spTipoMesa.selectedItem.toString())
            val estado = binding.etEstadoConservacao.text.toString().trim()
            if (numero.isEmpty() || tamanho.isEmpty() || estado.isEmpty()) {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mesa = Mesa(
                numero = numero,
                clienteId = null,
                fichasInicial = 0,
                fichasFinal = 0,
                tipoMesa = tipo,
                ativa = false,
                observacoes = "Estado: $estado",
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 