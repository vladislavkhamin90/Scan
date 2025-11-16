package com.example.qr_scan

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.qr_scan.R
import com.example.qr_scan.databinding.FragmentSearchBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var title: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем результат сканирования, если он есть
        arguments?.getString("scan_result")?.let { scanResult ->
            binding.edSearch.setText(scanResult.replace(";", ""))
            performSearch(scanResult.replace(";", ""))
        }

        binding.searchButton.setOnClickListener {
            val searchText = binding.edSearch.text.toString()
            if (searchText.isNotEmpty()) {
                performSearch(searchText)
            } else {
                binding.searchResult.text = getString(R.string.empty_search_warning)
            }
        }
    }

    private fun performSearch(searchString: String) {
        val masOfChoose = mutableListOf<String>()
        var line: String

        try {
            // Чтение файла из assets
            requireContext().assets.open("list.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "Cp1251")).use { reader ->
                    while (reader.readLine().also { line = it } != null) {
                        if (title.isEmpty()) {
                            title = line
                        }
                        if (line.contains(searchString, ignoreCase = true)) {
                            masOfChoose.add(line)
                        }
                    }
                }
            }

            when (masOfChoose.size) {
                1 -> {
                    val result = masOfChoose[0].split(";").toTypedArray()
                    addToScreen(result, title)
                }
                in 2..9 -> {
                    showMultipleResultsDialog(masOfChoose.toSet().toTypedArray())
                }
                else -> {
                    binding.searchResult.text = getString(R.string.no_data_in_db)
                }
            }
        } catch (e: Exception) {
            Log.e("Search_error", e.toString())
            binding.searchResult.text = getString(R.string.search_error)
        }
    }

    private fun addToScreen(result: Array<String>, title: String) {
        binding.searchResult.text = ""
        val arrTitle = title.split(";").toTypedArray()
        for ((index, name) in arrTitle.withIndex()) {
            if (index < result.size) {
                binding.searchResult.append("$name: ${result[index]}\n")
            }
        }
    }

    private fun showMultipleResultsDialog(items: Array<String>) {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle).apply {
            setTitle(getString(R.string.multiple_matches_found))
            setCancelable(false)
            setItems(items) { _, which ->
                val result = items[which].split(";").toTypedArray()
                addToScreen(result, title)
            }
            setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            create().apply {
                window?.setBackgroundDrawableResource(R.drawable.alertdialog)
                show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(scanResult: String? = null): SearchFragment {
            return SearchFragment().apply {
                arguments = Bundle().apply {
                    putString("scan_result", scanResult)
                }
            }
        }
    }
}