
package com.example.gestaobilhares.ui.logs
import com.example.gestaobilhares.ui.R

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import java.util.ArrayList

class LogViewerFragment : Fragment() {

    private lateinit var viewModel: LogViewerViewModel
    private lateinit var logsListView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(com.example.gestaobilhares.ui.R.layout.fragment_log_viewer, container, false)

        logsListView = view.findViewById(com.example.gestaobilhares.ui.R.id.logsRecyclerView)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        logsListView.adapter = adapter

        val copyButton: Button = view.findViewById(com.example.gestaobilhares.ui.R.id.copyButton)
        copyButton.setOnClickListener {
            copyLogsToClipboard()
        }

        val clearButton: Button = view.findViewById(com.example.gestaobilhares.ui.R.id.clearButton)
        clearButton.setOnClickListener {
            viewModel.clearLogs()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[LogViewerViewModel::class.java]
        viewModel.logs.observe(viewLifecycleOwner, Observer { logs ->
            adapter.clear()
            adapter.addAll(ArrayList(logs))
            adapter.notifyDataSetChanged()
        })
    }

    private fun copyLogsToClipboard() {
        val logsText = viewModel.logs.value?.joinToString("\n") ?: ""
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("App Logs", logsText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Logs copiados para a área de transferência", Toast.LENGTH_SHORT).show()
    }
} 