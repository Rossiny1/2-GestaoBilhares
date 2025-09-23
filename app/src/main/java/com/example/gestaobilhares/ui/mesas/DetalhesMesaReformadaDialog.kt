package com.example.gestaobilhares.ui.mesas

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.databinding.DialogDetalhesMesaReformadaBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog para exibir os detalhes de uma mesa reformada, incluindo a foto se existir.
 */
class DetalhesMesaReformadaDialog : DialogFragment() {

    private var _binding: DialogDetalhesMesaReformadaBinding? = null
    private val binding get() = _binding!!

    private lateinit var mesaReformada: MesaReformada

    companion object {
        private const val ARG_MESA_REFORMADA = "mesa_reformada"

        fun newInstance(mesaReformada: MesaReformada): DetalhesMesaReformadaDialog {
            val args = Bundle()
            args.putSerializable(ARG_MESA_REFORMADA, mesaReformada)
            val fragment = DetalhesMesaReformadaDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
        
        arguments?.let {
            mesaReformada = it.getSerializable(ARG_MESA_REFORMADA) as MesaReformada
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDetalhesMesaReformadaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }

    private fun setupUI() {
        // Número da mesa
        binding.tvNumeroMesa.text = "Mesa ${mesaReformada.numeroMesa}"

        // Tipo da mesa
        binding.tvTipoMesa.text = "${mesaReformada.tipoMesa} - ${mesaReformada.tamanhoMesa}"

        // Data da reforma
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvDataReforma.text = dateFormat.format(mesaReformada.dataReforma)

        // Itens reformados
        val itensReformados = buildString {
            val itens = mutableListOf<String>()
            if (mesaReformada.pintura) itens.add("Pintura")
            if (mesaReformada.tabela) itens.add("Tabela")
            if (mesaReformada.panos) {
                val panosText = if (mesaReformada.numeroPanos != null) {
                    "Panos (${mesaReformada.numeroPanos})"
                } else {
                    "Panos"
                }
                itens.add(panosText)
            }
            if (mesaReformada.outros) itens.add("Outros")

            append(itens.joinToString(", "))
        }
        binding.tvItensReformados.text = itensReformados

        // Observações
        if (!mesaReformada.observacoes.isNullOrBlank()) {
            binding.tvObservacoes.text = mesaReformada.observacoes
            binding.cardObservacoes.visibility = View.VISIBLE
        } else {
            binding.cardObservacoes.visibility = View.GONE
        }

        // Foto da mesa
        if (!mesaReformada.fotoReforma.isNullOrBlank()) {
            carregarFoto(mesaReformada.fotoReforma!!)
        } else {
            binding.cardFoto.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnFechar.setOnClickListener {
            dismiss()
        }
    }

    private fun carregarFoto(caminhoFoto: String) {
        try {
            Log.d("DetalhesMesaReformadaDialog", "Carregando foto: $caminhoFoto")
            
            val fotoFile = File(caminhoFoto)
            if (fotoFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(fotoFile.absolutePath)
                if (bitmap != null) {
                    binding.ivFotoMesa.setImageBitmap(bitmap)
                    binding.cardFoto.visibility = View.VISIBLE
                    Log.d("DetalhesMesaReformadaDialog", "Foto carregada com sucesso")
                } else {
                    Log.e("DetalhesMesaReformadaDialog", "Erro ao decodificar bitmap")
                    binding.cardFoto.visibility = View.GONE
                }
            } else {
                Log.e("DetalhesMesaReformadaDialog", "Arquivo de foto não existe: $caminhoFoto")
                binding.cardFoto.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("DetalhesMesaReformadaDialog", "Erro ao carregar foto: ${e.message}", e)
            binding.cardFoto.visibility = View.GONE
            Toast.makeText(requireContext(), "Erro ao carregar foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
