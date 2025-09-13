package com.example.gestaobilhares.ui.colaboradores

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.databinding.FragmentColaboradorMetasBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment para gerenciamento de metas dos colaboradores.
 * Permite cadastrar, editar e acompanhar metas de performance.
 */
class ColaboradorMetasFragment : Fragment() {

    private var _binding: FragmentColaboradorMetasBinding? = null
    private val binding get() = _binding!!

    private lateinit var appRepository: AppRepository
    private var colaboradorId: Long? = null
    private var cicloSelecionado: Long? = null
    private var rotaSelecionada: Long? = null

    // Formatadores de data
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColaboradorMetasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRepository()
        setupToolbar()
        setupDropdowns()
        setupCiclosERotas()
        setupClickListeners()
        
        // Verificar se é edição
        colaboradorId = arguments?.getLong("colaborador_id", -1L).takeIf { it != -1L }
        if (colaboradorId != null) {
            carregarMetasColaborador(colaboradorId!!)
        }
    }

    private fun setupRepository() {
        val database = AppDatabase.getDatabase(requireContext())
        appRepository = AppRepository(
            database.clienteDao(),
            database.acertoDao(),
            database.mesaDao(),
            database.rotaDao(),
            database.despesaDao(),
            database.colaboradorDao(),
            database.cicloAcertoDao(),
            database.acertoMesaDao(),
            database.contratoLocacaoDao(),
            database.aditivoContratoDao()
        )
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Atualizar título se for edição
        if (colaboradorId != null) {
            binding.toolbar.title = "Editar Metas"
        } else {
            binding.toolbar.title = "Nova Meta"
        }
    }

    private fun setupDropdowns() {
        // Tipos de meta
        val tiposMeta = TipoMeta.values().map { 
            when (it) {
                TipoMeta.FATURAMENTO -> "Faturamento (R$)"
                TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados (%)"
                TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
                TipoMeta.TICKET_MEDIO -> "Ticket Médio por Mesa (R$)"
            }
        }
        
        val tiposAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposMeta)
        binding.autoCompleteTipoMeta.setAdapter(tiposAdapter)
        
        // Configurar listener para mudar hint do valor da meta
        binding.autoCompleteTipoMeta.setOnItemClickListener { _, _, position, _ ->
            val tipoSelecionado = TipoMeta.values()[position]
            when (tipoSelecionado) {
                TipoMeta.CLIENTES_ACERTADOS -> {
                    binding.tilValorMeta.hint = "Percentual de Clientes Acertados (%)"
                }
                TipoMeta.FATURAMENTO -> {
                    binding.tilValorMeta.hint = "Valor da Meta (R$)"
                }
                TipoMeta.MESAS_LOCADAS -> {
                    binding.tilValorMeta.hint = "Quantidade de Mesas"
                }
                TipoMeta.TICKET_MEDIO -> {
                    binding.tilValorMeta.hint = "Ticket Médio (R$)"
                }
            }
        }
    }

    private fun setupCiclosERotas() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Carregar ciclos
                val ciclos = withContext(Dispatchers.IO) {
                    appRepository.obterTodosCiclos().first()
                }
                
                val ciclosAdapter = ArrayAdapter(
                    requireContext(), 
                    android.R.layout.simple_dropdown_item_1line, 
                    ciclos.map { "Ciclo ${it.id} - ${dateFormatter.format(it.dataInicio)} a ${dateFormatter.format(it.dataFim)}" }
                )
                binding.autoCompleteCiclo.setAdapter(ciclosAdapter)
                
                // Carregar rotas
                val rotas = withContext(Dispatchers.IO) {
                    appRepository.obterTodasRotas().first()
                }
                
                val rotasAdapter = ArrayAdapter(
                    requireContext(), 
                    android.R.layout.simple_dropdown_item_1line, 
                    listOf("Todas as Rotas") + rotas.map { it.nome }
                )
                binding.autoCompleteRota.setAdapter(rotasAdapter)
                
                // Configurar listeners
                binding.autoCompleteCiclo.setOnItemClickListener { _, _, position, _ ->
                    cicloSelecionado = ciclos[position].id
                }
                
                binding.autoCompleteRota.setOnItemClickListener { _, _, position, _ ->
                    rotaSelecionada = if (position == 0) null else rotas[position - 1].id
                }
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar ciclos e rotas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCancelar.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnSalvar.setOnClickListener {
            salvarMeta()
        }
    }

    private fun carregarMetasColaborador(id: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val metas = withContext(Dispatchers.IO) {
                    appRepository.obterMetasPorColaborador(id).first()
                }
                
                // TODO: Implementar carregamento de metas existentes
                // Por enquanto, apenas mostra que o colaborador foi selecionado
                Toast.makeText(requireContext(), "Carregando metas do colaborador ID: $id", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar metas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun salvarMeta() {
        val tipoMetaText = binding.autoCompleteTipoMeta.text.toString()
        val valorMetaText = binding.editTextValorMeta.text.toString().trim()
        
        // Validações básicas
        if (tipoMetaText.isEmpty()) {
            Toast.makeText(requireContext(), "Selecione o tipo de meta", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (valorMetaText.isEmpty()) {
            Toast.makeText(requireContext(), "Informe o valor da meta", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (cicloSelecionado == null) {
            Toast.makeText(requireContext(), "Selecione um ciclo", Toast.LENGTH_SHORT).show()
            return
        }
        
        val valorMeta = valorMetaText.toDoubleOrNull()
        if (valorMeta == null || valorMeta <= 0) {
            Toast.makeText(requireContext(), "Valor da meta deve ser um número positivo", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Converter texto para enum
        val tipoMeta = when (tipoMetaText) {
            "Faturamento (R$)" -> TipoMeta.FATURAMENTO
            "Clientes Acertados (%)" -> TipoMeta.CLIENTES_ACERTADOS
            "Mesas Locadas" -> TipoMeta.MESAS_LOCADAS
            "Ticket Médio por Mesa (R$)" -> TipoMeta.TICKET_MEDIO
            else -> {
                Toast.makeText(requireContext(), "Tipo de meta inválido", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val meta = MetaColaborador(
                    colaboradorId = colaboradorId ?: 0,
                    tipoMeta = tipoMeta,
                    valorMeta = valorMeta,
                    cicloId = cicloSelecionado!!,
                    rotaId = rotaSelecionada,
                    valorAtual = 0.0,
                    ativo = true
                )
                
                withContext(Dispatchers.IO) {
                    appRepository.inserirMeta(meta)
                }
                
                Toast.makeText(requireContext(), "Meta salva com sucesso!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao salvar meta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
