package com.example.gestaobilhares.ui.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentRotasConfigBinding
import com.example.gestaobilhares.ui.routes.RotasConfigViewModel
import kotlinx.coroutines.launch

/**
 * Fragment para configuração de rotas por colaborador
 * Permite ao admin associar/desassociar rotas aos colaboradores
 */
class RotasConfigFragment : Fragment() {

    private var _binding: FragmentRotasConfigBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RotasConfigViewModel by viewModels()

    private lateinit var rotasAdapter: RotasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRotasConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Carregar dados iniciais
        viewModel.carregarDadosIniciais()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Configura o RecyclerView de rotas disponíveis
     */
    private fun setupRecyclerView() {
        rotasAdapter = RotasAdapter { rota ->
            // TODO: Implementar diálogo de associação
            Toast.makeText(requireContext(), "Clicado na rota: ${rota.nome}", Toast.LENGTH_SHORT).show()
        }
        
        binding.recyclerViewRotas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rotasAdapter
        }
    }

    /**
     * Configura os observers do ViewModel
     */
    private fun setupObservers() {
        // Observer para rotas disponíveis
        viewModel.rotasDisponiveis.observe(viewLifecycleOwner) { rotas ->
            rotasAdapter.submitList(rotas)
        }

        // Observer para loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observer para mensagens de erro
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                binding.textViewError.text = errorMessage
                binding.textViewError.visibility = View.VISIBLE
            } else {
                binding.textViewError.visibility = View.GONE
            }
        }

        // Observer para mensagens de sucesso
        viewModel.sucessoMessage.observe(viewLifecycleOwner) { sucessoMessage ->
            if (sucessoMessage != null) {
                Toast.makeText(requireContext(), sucessoMessage, Toast.LENGTH_LONG).show()
                viewModel.limparMensagens()
            }
        }
    }

    /**
     * Configura os listeners de click
     */
    private fun setupClickListeners() {
        // TODO: Implementar botões de ação quando tiver os adapters
        // Ex: botão para salvar associações, novo colaborador, etc.
    }

    /**
     * Adapter para RecyclerView de rotas disponíveis
     */
    private class RotasAdapter(
        private val onRotaClick: (Rota) -> Unit
    ) : RecyclerView.Adapter<RotasAdapter.RotaViewHolder>() {

        private var rotas = emptyList<Rota>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RotaViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_rota_config, parent, false)
            return RotaViewHolder(view)
        }

        override fun onBindViewHolder(holder: RotaViewHolder, position: Int) {
            val rota = rotas[position]
            holder.bind(rota)
        }

        override fun getItemCount(): Int = rotas.size

        fun submitList(newRotas: List<Rota>) {
            rotas = newRotas
            notifyDataSetChanged()
        }

        class RotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val binding = ItemRotaConfigBinding.bind(itemView)

            fun bind(rota: Rota) {
                binding.textViewRotaNome.text = rota.nome
                binding.textViewRotaStatus.text = if (rota.ativo) "Ativa" else "Inativa"
                
                // Click listener
                itemView.setOnClickListener {
                    onRotaClick(rota)
                }
            }
        }
    }
}
