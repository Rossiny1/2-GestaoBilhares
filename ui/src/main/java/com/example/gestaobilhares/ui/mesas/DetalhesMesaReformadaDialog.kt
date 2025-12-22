package com.example.gestaobilhares.ui.mesas

import android.app.Dialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.ui.databinding.DialogDetalhesMesaReformadaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
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
            // ✅ CORREÇÃO: Usar nova API para getSerializable (Android 13+)
            @Suppress("DEPRECATION")
            mesaReformada = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_MESA_REFORMADA, MesaReformada::class.java)!!
            } else {
                it.getSerializable(ARG_MESA_REFORMADA) as MesaReformada
            }
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
            Timber.d("DetalhesMesaReformadaDialog", "=== CARREGANDO FOTO ===")
            Timber.d("DetalhesMesaReformadaDialog", "Caminho da foto: $caminhoFoto")
            
            // ✅ CORREÇÃO: Verificar se é URL do Firebase Storage
            val isFirebaseUrl = caminhoFoto.startsWith("https://") && 
                                (caminhoFoto.contains("firebasestorage.googleapis.com") || 
                                 caminhoFoto.contains("firebase"))
            
            // ✅ CORREÇÃO: PRIORIDADE 1 - Verificar se é arquivo local e existe
            if (!isFirebaseUrl && !caminhoFoto.startsWith("content://")) {
                val fotoFile = File(caminhoFoto)
                if (fotoFile.exists() && fotoFile.isFile) {
                    Timber.d("DetalhesMesaReformadaDialog", "✅ Carregando foto local: ${fotoFile.absolutePath}")
                    val bitmap = BitmapFactory.decodeFile(fotoFile.absolutePath)
                    if (bitmap != null) {
                        binding.ivFotoMesa.setImageBitmap(bitmap)
                        binding.cardFoto.visibility = View.VISIBLE
                        Timber.d("DetalhesMesaReformadaDialog", "Foto carregada com sucesso")
                        return
                    } else {
                        Timber.e("DetalhesMesaReformadaDialog", "Erro ao decodificar bitmap")
                    }
                } else {
                    Timber.w("DetalhesMesaReformadaDialog", "⚠️ Arquivo local não existe: ${fotoFile.absolutePath}")
                }
            }
            
            // ✅ CORREÇÃO: PRIORIDADE 2 - Se for URI content://, tentar carregar do content provider
            if (caminhoFoto.startsWith("content://")) {
                try {
                    val uri = Uri.parse(caminhoFoto)
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        Timber.d("DetalhesMesaReformadaDialog", "✅ Carregando foto do content provider")
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        if (bitmap != null) {
                            binding.ivFotoMesa.setImageBitmap(bitmap)
                            binding.cardFoto.visibility = View.VISIBLE
                            return
                        }
                    }
                } catch (e: Exception) {
                    Timber.e("DetalhesMesaReformadaDialog", "Erro ao carregar do URI: ${e.message}")
                }
            }
            
            // ✅ CORREÇÃO: PRIORIDADE 3 - Se for URL do Firebase Storage, fazer download
            if (isFirebaseUrl) {
                Timber.d("DetalhesMesaReformadaDialog", "Detectada URL do Firebase Storage, fazendo download...")
                binding.cardFoto.visibility = View.VISIBLE
                binding.ivFotoMesa.setImageResource(android.R.drawable.ic_menu_gallery) // Placeholder
                
                lifecycleScope.launch {
                    try {
                        val bitmap = downloadImageFromUrl(caminhoFoto)
                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                binding.ivFotoMesa.setImageBitmap(bitmap)
                                Timber.d("DetalhesMesaReformadaDialog", "✅ Foto carregada do Firebase Storage")
                            } else {
                                Timber.e("DetalhesMesaReformadaDialog", "Erro ao baixar foto do Firebase")
                                binding.cardFoto.visibility = View.GONE
                                Toast.makeText(requireContext(), "Erro ao carregar foto do servidor", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("DetalhesMesaReformadaDialog", "Erro ao fazer download: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            binding.cardFoto.visibility = View.GONE
                            Toast.makeText(requireContext(), "Erro ao carregar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                return
            }
            
            // Se chegou aqui, não conseguiu carregar a foto
            Timber.e("DetalhesMesaReformadaDialog", "❌ Não foi possível carregar a foto: $caminhoFoto")
            binding.cardFoto.visibility = View.GONE
            
        } catch (e: Exception) {
            Timber.e("DetalhesMesaReformadaDialog", "Erro ao carregar foto: ${e.message}", e)
            binding.cardFoto.visibility = View.GONE
            Toast.makeText(requireContext(), "Erro ao carregar foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * ✅ NOVO: Faz download de imagem de uma URL
     */
    private suspend fun downloadImageFromUrl(urlString: String): android.graphics.Bitmap? {
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()
            
            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()
            bitmap
        } catch (e: Exception) {
            Timber.e("DetalhesMesaReformadaDialog", "Erro ao fazer download da imagem: ${e.message}", e)
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

