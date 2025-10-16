package com.example.gestaobilhares.ui.clients

import android.Manifest
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.databinding.FragmentClientDetailBinding
import com.example.gestaobilhares.ui.clients.MesasAdapter
import com.example.gestaobilhares.ui.clients.SettlementHistoryAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.repository.AppRepository
import android.util.Log
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.example.gestaobilhares.ui.dialogs.AdicionarMesaDialogFragment
import com.example.gestaobilhares.ui.dialogs.AdicionarObservacaoDialogFragment
import com.example.gestaobilhares.ui.dialogs.ConfirmarRetiradaMesaDialogFragment
import com.example.gestaobilhares.ui.dialogs.GerarRelatorioDialogFragment
import com.example.gestaobilhares.ui.dialogs.RotaNaoIniciadaDialogFragment
import kotlinx.coroutines.flow.first

class ClientDetailFragment : Fragment(), ConfirmarRetiradaMesaDialogFragment.ConfirmarRetiradaDialogListener, AdicionarMesaDialogFragment.AdicionarMesaDialogListener, AdicionarObservacaoDialogFragment.AdicionarObservacaoDialogListener, GerarRelatorioDialogFragment.GerarRelatorioDialogListener {

    private var _binding: FragmentClientDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ClientDetailFragmentArgs by navArgs()
    private lateinit var viewModel: ClientDetailViewModel
    private lateinit var mesasAdapter: MesasAdapter
    private lateinit var historicoAdapter: SettlementHistoryAdapter
    private var isFabMenuOpen = false
    private lateinit var appRepository: AppRepository
    private var mesaParaRemover: Mesa? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appRepository = RepositoryFactory.getAppRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientDetailBinding.inflate(inflater, container, false)

        val clientId = args.clienteId
        viewModel = ClientDetailViewModel(appRepository)
        setupRecyclerView()
        observeViewModel()
        setupListeners(clientId)

        return binding.root
    }

    private fun setupRecyclerView() {
        mesasAdapter = MesasAdapter(
            onRetirarMesa = { mesa ->
                mesaParaRemover = mesa
                ConfirmarRetiradaMesaDialogFragment.newInstance().show(parentFragmentManager, ConfirmarRetiradaMesaDialogFragment.TAG)
            }
        )
        binding.rvMesasCliente.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mesasAdapter
        }

        historicoAdapter = SettlementHistoryAdapter { acerto ->
            val action = ClientDetailFragmentDirections.actionClientDetailFragmentToSettlementDetailFragment(acerto.id)
            findNavController().navigate(action)
        }
        binding.rvSettlementHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historicoAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cliente.collect { cliente ->
                        cliente?.let { updateClientUI(it) }
                    }
                }
                launch {
                    viewModel.mesasCliente.collect { mesas ->
                        mesasAdapter.submitList(mesas)
                        binding.tvTotalMesasAtivas.text = mesas.size.toString()
                    }
                }
                launch {
                    viewModel.settlementHistory.collect { historico ->
                        historicoAdapter.submitList(historico)
                    }
                }
            }
        }
    }

    private fun setupListeners(clientId: Long) {
        binding.fabMain.setOnClickListener { toggleFabMenu() }

        binding.fabEdit.setOnClickListener {
            val action = ClientDetailFragmentDirections.actionClientDetailFragmentToClientRegisterFragment(clientId)
            findNavController().navigate(action)
            recolherFabMenu()
        }

        binding.fabAddTableContainer.setOnClickListener {
            AdicionarMesaDialogFragment.newInstance(clientId).show(parentFragmentManager, AdicionarMesaDialogFragment.TAG)
            recolherFabMenu()
        }

        binding.fabNewSettlementContainer.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val rotaId = viewModel.buscarRotaIdPorCliente(clientId) ?: -1L
                    if (rotaId == -1L) {
                        Toast.makeText(requireContext(), "Cliente não associado a uma rota.", Toast.LENGTH_SHORT).show()
                        recolherFabMenu()
                        return@launch
                    }

                    val cicloEmAndamento = viewModel.buscarCicloAtualPorRota(rotaId)
                    if (cicloEmAndamento == null || !cicloEmAndamento.estaEmAndamento) {
                        RotaNaoIniciadaDialogFragment().show(parentFragmentManager, RotaNaoIniciadaDialogFragment.TAG)
                        recolherFabMenu()
                        return@launch
                    }

                    val mesasCliente = viewModel.mesasCliente.first()
                    val mesasDTO = mesasCliente.map { mesa ->
                        com.example.gestaobilhares.ui.settlement.MesaDTO(
                            id = mesa.id,
                            numero = mesa.numero,
                            fichasInicial = mesa.fichasInicial,
                            fichasFinal = mesa.fichasFinal,
                            tipoMesa = mesa.tipoMesa,
                            tamanho = mesa.tamanho,
                            estadoConservacao = mesa.estadoConservacao,
                            ativa = mesa.ativa,
                            valorFixo = mesa.valorFixo,
                            valorFicha = 0.0,
                            comissaoFicha = 0.0
                        )
                    }.toTypedArray()

                    val action = ClientDetailFragmentDirections.actionClientDetailFragmentToSettlementFragment(
                        clienteId = clientId,
                        acertoIdParaEdicao = 0L,
                        mesasDTO = mesasDTO
                    )
                    findNavController().navigate(action)
                    recolherFabMenu()

                } catch (e: Exception) {
                    Log.e("ClientDetailFragment", "Erro ao iniciar novo acerto: ${e.message}")
                    Toast.makeText(requireContext(), "Erro ao iniciar novo acerto.", Toast.LENGTH_SHORT).show()
                    recolherFabMenu()
                }
            }
        }

        binding.fabContractContainer.setOnClickListener {
            Toast.makeText(requireContext(), "Geração de contrato será habilitada em seguida.", Toast.LENGTH_SHORT).show()
            recolherFabMenu()
        }

        binding.fabWhatsApp.setOnClickListener {
            abrirWhatsApp()
            recolherFabMenu()
        }
    }

    private fun updateClientUI(cliente: Cliente) {
        binding.tvClientName.text = cliente.nome
        binding.tvClientAddress.text = cliente.endereco ?: ""
        val formattedDebt = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(cliente.debitoAtual)
        binding.tvClientCurrentDebt.text = formattedDebt
        binding.tvLastVisit.text = "N/A"
    }

    private fun toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen
        if (isFabMenuOpen) {
            expandirFabMenu()
        } else {
            recolherFabMenu()
        }
    }

    private fun expandirFabMenu() {
        ObjectAnimator.ofFloat(binding.fabMain, "rotation", 0f, 45f).setDuration(300).start()
        binding.fabExpandedContainer.visibility = View.VISIBLE
        animateFabContainer(binding.fabAddTableContainer, 0, 50)
        animateFabContainer(binding.fabContractContainer, 1, 100)
        animateFabContainer(binding.fabNewSettlementContainer, 2, 150)
    }

    private fun recolherFabMenu() {
        ObjectAnimator.ofFloat(binding.fabMain, "rotation", 45f, 0f).setDuration(300).start()
        animateFabContainer(binding.fabNewSettlementContainer, 2, 150, false)
        animateFabContainer(binding.fabContractContainer, 1, 100, false)
        animateFabContainer(binding.fabAddTableContainer, 0, 50, false) {
            binding.fabExpandedContainer.visibility = View.GONE
        }
    }

    private fun animateFabContainer(container: View, index: Int, startDelay: Long, show: Boolean = true, onEndAction: (() -> Unit)? = null) {
        val translationY = if (show) 0f else 100f
        val alpha = if (show) 1f else 0f
        container.animate()
            .translationY(translationY)
            .alpha(alpha)
            .setDuration(200)
            .setStartDelay(startDelay)
            .withEndAction { onEndAction?.invoke() }
            .start()
    }

    override fun onMesaAdicionada(novaMesa: Mesa) {
        viewModel.adicionarMesaAoCliente(novaMesa.id, args.clienteId)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        mesaParaRemover?.let {
            viewModel.retirarMesaDoCliente(it.id, args.clienteId, relogioFinal = it.relogioFinal, valorRecebido = 0.0)
            mesaParaRemover = null
        }
    }

    override fun onGerarRelatorioUltimoAcerto() {
        Toast.makeText(requireContext(), "Relatório de acerto será habilitado em seguida.", Toast.LENGTH_SHORT).show()
    }

    override fun onGerarRelatorioAnual() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecione o Ano")
            .build()
        datePicker.addOnPositiveButtonClickListener {
            Toast.makeText(requireContext(), "Relatório anual será habilitado em seguida.", Toast.LENGTH_SHORT).show()
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    override fun onObservacaoAdicionada(textoObservacao: String) {
        Log.d("ClientDetailFragment", "Observação adicionada: $textoObservacao")
    }

    private fun abrirWhatsApp() {
        val cliente = viewModel.cliente.value
        val telefone = cliente?.telefone
        if (!telefone.isNullOrBlank()) {
            val numeroLimpo = telefone.filter { it.isDigit() }
            val ddi = "55"
            val url = "https://api.whatsapp.com/send?phone=$ddi$numeroLimpo"
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Não foi possível abrir o WhatsApp.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Cliente não possui número de telefone cadastrado.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
