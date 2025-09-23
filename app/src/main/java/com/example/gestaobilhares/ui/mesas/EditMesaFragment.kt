package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.databinding.FragmentEditMesaBinding
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.entities.EstadoConservacao
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class EditMesaFragment : Fragment() {

    private var _binding: FragmentEditMesaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditMesaViewModel by viewModels()
    private val historicoViewModel: HistoricoManutencaoMesaViewModel by viewModels()
    private val args: EditMesaFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditMesaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
        observeViewModel()

        // Carregar dados da mesa
        viewModel.loadMesa(args.mesaId)
        
        // Carregar histórico de manutenção
        historicoViewModel.carregarHistoricoMesa(args.mesaId)
    }

    private fun setupSpinners() {
        // Tipo da Mesa
        val tipoOptions = listOf("Sinuca", "Jukebox", "Pembolim", "Outros")
        val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipoOptions)
        binding.actvTipoMesa.setAdapter(tipoAdapter)

        // Tamanho da Mesa
        val tamanhoOptions = listOf("Pequena", "Média", "Grande")
        val tamanhoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tamanhoOptions)
        binding.actvTamanhoMesa.setAdapter(tamanhoAdapter)

        // Estado de Conservação
        val estadoOptions = listOf("Ótimo", "Bom", "Ruim")
        val estadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estadoOptions)
        binding.actvEstadoMesa.setAdapter(estadoAdapter)

        // Status
        val statusOptions = listOf("Ativa", "Inativa")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statusOptions)
        binding.actvStatusMesa.setAdapter(statusAdapter)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSalvar.setOnClickListener {
            salvarMesa()
        }

        // Removed: btnCancelar was removed from layout

        binding.btnVerHistoricoCompleto.setOnClickListener {
            // Navegar para o histórico completo
            val action = EditMesaFragmentDirections.actionEditMesaFragmentToHistoricoManutencaoMesaFragment(args.mesaId)
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mesa.collect { mesa ->
                mesa?.let { preencherCampos(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rotas.collect { rotas ->
                val rotaOptions = listOf("Depósito") + rotas.map { it.nome }
                val rotaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rotaOptions)
                binding.actvRotaMesa.setAdapter(rotaAdapter)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saving.collect { saving ->
                binding.btnSalvar.isEnabled = !saving
            }
        }

        // ✅ NOVO: Observar histórico de manutenção (LiveData)
        historicoViewModel.ultimaPintura.observe(viewLifecycleOwner) { pintura ->
            binding.tvDataPintura.text = if (pintura != null) {
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                dateFormat.format(pintura.dataManutencao)
            } else {
                "Nunca pintada"
            }
        }

        historicoViewModel.ultimaTrocaPano.observe(viewLifecycleOwner) { pano ->
            binding.tvPanoAtual.text = if (pano != null) {
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val numeroPano = pano.descricao ?: "N/A"
                "$numeroPano - ${dateFormat.format(pano.dataManutencao)}"
            } else {
                "Nunca trocado"
            }
        }

        historicoViewModel.ultimaTrocaTabela.observe(viewLifecycleOwner) { tabela ->
            binding.tvDataTrocaTabela.text = if (tabela != null) {
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                dateFormat.format(tabela.dataManutencao)
            } else {
                "Nunca trocada"
            }
        }

        historicoViewModel.outrasManutencoes.observe(viewLifecycleOwner) { outras ->
            binding.tvOutrasManutencoes.text = if (!outras.isNullOrEmpty()) {
                "${outras.size} manutenção(ões)"
            } else {
                "Nenhuma"
            }
        }
    }

    private fun preencherCampos(mesa: com.example.gestaobilhares.data.entities.Mesa) {
        binding.etNumeroMesa.setText(mesa.numero)

        // Tipo
        val tipoText = when (mesa.tipoMesa) {
            TipoMesa.SINUCA -> "Sinuca"
            TipoMesa.JUKEBOX -> "Jukebox"
            TipoMesa.PEMBOLIM -> "Pembolim"
            TipoMesa.OUTROS -> "Outros"
        }
        binding.actvTipoMesa.setText(tipoText, false)

        // Tamanho
        val tamanhoText = when (mesa.tamanho) {
            TamanhoMesa.PEQUENA -> "Pequena"
            TamanhoMesa.MEDIA -> "Média"
            TamanhoMesa.GRANDE -> "Grande"
        }
        binding.actvTamanhoMesa.setText(tamanhoText, false)

        // Estado
        val estadoText = when (mesa.estadoConservacao) {
            EstadoConservacao.OTIMO -> "Ótimo"
            EstadoConservacao.BOM -> "Bom"
            EstadoConservacao.RUIM -> "Ruim"
        }
        binding.actvEstadoMesa.setText(estadoText, false)

        // Status
        binding.actvStatusMesa.setText(if (mesa.ativa) "Ativa" else "Inativa", false)

        // Rota
        if (mesa.clienteId != null) {
            // Mesa está locada para um cliente
            binding.actvRotaMesa.setText("Locada", false)
        } else {
            // Mesa está no depósito
            binding.actvRotaMesa.setText("Depósito", false)
        }

        // Cliente (se locada)
        if (mesa.clienteId != null) {
            binding.etClienteMesa.setText("Cliente ID: ${mesa.clienteId}")
            binding.etClienteMesa.visibility = View.VISIBLE
        } else {
            binding.etClienteMesa.visibility = View.GONE
        }
    }

    private fun salvarMesa() {
        val numero = binding.etNumeroMesa.text.toString().trim()
        if (numero.isEmpty()) {
            Toast.makeText(requireContext(), "Número da mesa é obrigatório", Toast.LENGTH_SHORT).show()
            return
        }

        val tipoText = binding.actvTipoMesa.text.toString()
        val tipo = when (tipoText) {
            "Sinuca" -> TipoMesa.SINUCA
            "Jukebox" -> TipoMesa.JUKEBOX
            "Pembolim" -> TipoMesa.PEMBOLIM
            "Outros" -> TipoMesa.OUTROS
            else -> TipoMesa.SINUCA
        }

        val tamanhoText = binding.actvTamanhoMesa.text.toString()
        val tamanho = when (tamanhoText) {
            "Pequena" -> TamanhoMesa.PEQUENA
            "Média" -> TamanhoMesa.MEDIA
            "Grande" -> TamanhoMesa.GRANDE
            else -> TamanhoMesa.MEDIA
        }

        val estadoText = binding.actvEstadoMesa.text.toString()
        val estado = when (estadoText) {
            "Ótimo" -> EstadoConservacao.OTIMO
            "Bom" -> EstadoConservacao.BOM
            "Ruim" -> EstadoConservacao.RUIM
            else -> EstadoConservacao.BOM
        }

        val rotaText = binding.actvRotaMesa.text.toString()
        val rota = if (rotaText == "Depósito") null else rotaText

        viewModel.salvarMesa(numero, tipo, tamanho, estado, rota)

        Toast.makeText(requireContext(), "Mesa salva com sucesso!", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}