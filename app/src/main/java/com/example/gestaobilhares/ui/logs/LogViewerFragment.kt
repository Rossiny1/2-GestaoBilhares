
package com.example.gestaobilhares.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gestaobilhares.R

class LogViewerFragment : Fragment() {

    private lateinit var viewModel: LogViewerViewModel
    private lateinit var logsListView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log_viewer, container, false)

        logsListView = view.findViewById(R.id.logsRecyclerView)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        logsListView.adapter = adapter

        val copyButton: Button = view.findViewById(R.id.copyButton)
        copyButton.setOnClickListener {
            copyLogsToClipboard()
        }

        val clearButton: Button = view.findViewById(R.id.clearButton)
        clearButton.setOnClickListener {
            viewModel.clearLogs()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        viewModel = LogViewerViewModel() // Construtor padrão
        
        viewModel.logs.observe(viewLifecycleOwner) { logs ->
            adapter.clear()
            adapter.addAll(logs)
            adapter.notifyDataSetChanged()
        }
    }

    private fun copyLogsToClipboard() {
        val logsText = viewModel.logs.value?.joinToString("\n") ?: ""
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("App Logs", logsText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Logs copiados para a área de transferência", Toast.LENGTH_SHORT).show()
    }
} 