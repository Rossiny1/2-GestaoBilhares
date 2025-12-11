package com.example.gestaobilhares.ui.metas
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.data.entities.CicloAcerto
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.ui.databinding.FragmentMetaCadastroBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Fragment para cadastro de metas
 * Fluxo: Selecionar Rota → Selecionar Ciclo → Definir Tipo e Valor da Meta
 */
class MetaCadastroFragment : Fragment() {

    private var _binding: FragmentMetaCadastroBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MetaCadastroViewModel

    // Dados selecionados
    private var rotaSelecionada: Rota? = null
    private var cicloSelecionado: com.example.gestaobilhares.data.entities.CicloAcertoEntity? = null
    private var tipoMetaSelecionado: TipoMeta? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetaCadastroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        val appRepository = com.example.gestaobilhares.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = MetaCadastroViewModel(appRepository)
        
        setupUI()
        setupClickListeners()
        observeViewModel()
        
        // Obter rota selecionada dos argumentos
        val rotaId = arguments?.getLong("rota_id")
        android.util.Log.d("MetaCadastroFragment", "Rota ID recebida nos argumentos: $rotaId")
        if (rotaId != null && rotaId != 0L) {
            // Se há rota_id nos argumentos, carregar apenas essa rota
            android.util.Log.d("MetaCadastroFragment", "Carregando rota específica com ID: $rotaId")
            viewModel.carregarRotaPorId(rotaId)
        } else {
            // Se não há rota_id, carregar todas as rotas (para seleção manual)
            android.util.Log.d("MetaCadastroFragment", "Nenhum rota_id fornecido, carregando todas as rotas")
            loadInitialData()
        }
    }

    private fun setupUI() {
        // Configurar dropdown de tipo de meta
        val tiposMeta = TipoMeta.values().map { getTipoMetaFormatado(it) }
        val tipoMetaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposMeta)
        binding.actvTipoMeta.setAdapter(tipoMetaAdapter)

        // Rota será carregada automaticamente via argumentos

        // Configurar Spinner de ciclo
        binding.spinnerCiclo.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) { // Ignorar o primeiro item (placeholder)
                    // ✅ ATUALIZADO: Usar lista filtrada de ciclos EM_ANDAMENTO
                    val ciclos = viewModel.ciclos.value.filter { 
                        it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO 
                    }
                    if (position - 1 < ciclos.size) {
                        cicloSelecionado = ciclos[position - 1]
                        android.util.Log.d("MetaCadastroFragment", "Ciclo selecionado: ${cicloSelecionado?.numeroCiclo}/${cicloSelecionado?.ano}")
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Configurar dropdown de tipo de meta
        binding.actvTipoMeta.setOnItemClickListener { _, _, position, _ ->
            tipoMetaSelecionado = TipoMeta.values()[position]
            binding.actvTipoMeta.setText(getTipoMetaFormatado(tipoMetaSelecionado!!), false)
            updateValorMetaHint()
        }
    }

    private fun setupClickListeners() {
        binding.btnSalvarMeta.setOnClickListener {
            salvarMeta()
        }

        binding.btnVoltar.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        // Observar rota carregada
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rotas.collect { rotas ->
                if (rotas.isNotEmpty()) {
                    // Se há apenas uma rota na lista, significa que foi carregada por ID (argumento)
                    // Se há múltiplas rotas, significa que foram carregadas todas (sem argumento)
                    if (rotas.size == 1) {
                        // Rota específica carregada via argumento
                        rotaSelecionada = rotas.first()
                        binding.etRota.setText(rotaSelecionada!!.nome)
                        android.util.Log.d("MetaCadastroFragment", "Rota selecionada via argumento: ${rotaSelecionada!!.nome}")
                    } else if (rotaSelecionada == null) {
                        // Múltiplas rotas carregadas, mas nenhuma selecionada ainda
                        // Não selecionar automaticamente - deixar o usuário escolher
                        android.util.Log.d("MetaCadastroFragment", "Múltiplas rotas disponíveis (${rotas.size}), aguardando seleção do usuário")
                    }
                }
            }
        }

        // Observar ciclos
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ciclos.collect { ciclos ->
                if (ciclos.isNotEmpty() && rotaSelecionada != null) {
                    configurarSpinnerCiclos(ciclos)
                    
                    // ✅ NOVO: Auto-selecionar ciclo EM_ANDAMENTO
                    val cicloEmAndamento = ciclos.find { 
                        it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO 
                    }
                    
                    if (cicloEmAndamento != null) {
                        val index = ciclos.indexOf(cicloEmAndamento)
                        binding.spinnerCiclo.setSelection(index + 1) // +1 por causa do placeholder
                        cicloSelecionado = cicloEmAndamento
                        android.util.Log.d("MetaCadastroFragment", "✅ Ciclo EM_ANDAMENTO auto-selecionado: ${cicloEmAndamento.numeroCiclo}/${cicloEmAndamento.ano}")
                    }
                } else {
                    configurarSpinnerCiclos(emptyList())
                }
            }
        }

        // Observar mensagens
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.limparMensagem()
                }
            }
        }

        // Observar sucesso
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.metaSalva.collect { sucesso ->
                if (sucesso) {
                    android.util.Log.d("MetaCadastroFragment", "✅ Meta salva com sucesso! Navegando de volta...")
                    Toast.makeText(requireContext(), "Meta salva com sucesso!", Toast.LENGTH_SHORT).show()
                    
                    // Navegar de volta para a tela anterior
                    // O MetasFragment.onResume() irá recarregar as metas automaticamente
                    findNavController().navigateUp()
                }
            }
        }

        // Observar ciclo criado
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cicloCriado.collect { criado ->
                if (criado) {
                    viewModel.resetarCicloCriado()
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModel.carregarRotas()
    }

    private fun configurarSpinnerCiclos(ciclos: List<com.example.gestaobilhares.data.entities.CicloAcertoEntity>) {
        val items = mutableListOf<String>()
        
        // ✅ NOVO: Filtrar apenas ciclos EM_ANDAMENTO
        val ciclosEmAndamento = ciclos.filter { 
            it.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO 
        }
        
        if (ciclosEmAndamento.isEmpty()) {
            items.add("Nenhum ciclo em andamento disponível")
            binding.spinnerCiclo.isEnabled = false
        } else {
            items.add("Selecione um ciclo")
            ciclosEmAndamento.forEach { ciclo ->
                items.add("Ciclo ${ciclo.numeroCiclo}/${ciclo.ano} - Em Andamento")
            }
            binding.spinnerCiclo.isEnabled = true
        }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCiclo.adapter = adapter
        
        android.util.Log.d("MetaCadastroFragment", "Spinner configurado com ${items.size} itens (${ciclosEmAndamento.size} ciclos em andamento)")
    }


    private fun updateValorMetaHint() {
        val hint = when (tipoMetaSelecionado) {
            TipoMeta.FATURAMENTO -> "Ex: 15000.00"
            TipoMeta.CLIENTES_ACERTADOS -> "Ex: 50"
            TipoMeta.MESAS_LOCADAS -> "Ex: 25"
            TipoMeta.TICKET_MEDIO -> "Ex: 300.00"
            null -> "Selecione o tipo de meta primeiro"
        }
        binding.tilValorMeta.hint = hint
        
        // ✅ NOVO: Definir prefixo apenas para metas monetárias
        binding.tilValorMeta.prefixText = when (tipoMetaSelecionado) {
            TipoMeta.FATURAMENTO, TipoMeta.TICKET_MEDIO -> "R$ "
            else -> null
        }
    }

    private fun salvarMeta() {
        if (!validarCampos()) return

        // Normalizar entrada monetária: permitir "15.000" -> 15000.00
        val rawValor = binding.etValorMeta.text.toString().trim()
        val normalized = rawValor.replace(".", "").replace(",", ".")
        val valorMeta = normalized.toDoubleOrNull()
        if (valorMeta == null || valorMeta <= 0) {
            Toast.makeText(requireContext(), "Valor da meta deve ser maior que zero", Toast.LENGTH_SHORT).show()
            return
        }

        val meta = MetaColaborador(
            colaboradorId = 0L, // Será definido pelo sistema baseado na rota
            rotaId = rotaSelecionada!!.id,
            cicloId = cicloSelecionado!!.id,
            tipoMeta = tipoMetaSelecionado!!,
            valorMeta = valorMeta,
            valorAtual = 0.0,
            ativo = true,
            dataCriacao = Date()
        )

        viewModel.salvarMeta(meta)
    }

    private fun validarCampos(): Boolean {
        if (rotaSelecionada == null) {
            Toast.makeText(requireContext(), "Selecione uma rota", Toast.LENGTH_SHORT).show()
            return false
        }

        if (cicloSelecionado == null) {
            Toast.makeText(requireContext(), "Selecione um ciclo", Toast.LENGTH_SHORT).show()
            return false
        }

        if (tipoMetaSelecionado == null) {
            Toast.makeText(requireContext(), "Selecione o tipo de meta", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.etValorMeta.text.toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Informe o valor da meta", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun getTipoMetaFormatado(tipoMeta: TipoMeta): String {
        return when (tipoMeta) {
            TipoMeta.FATURAMENTO -> "Faturamento"
            TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados"
            TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
            TipoMeta.TICKET_MEDIO -> "Ticket Médio"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

