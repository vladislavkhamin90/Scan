package com.example.scan

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.scan.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataRows: List<DataRow>

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

        dataRows = (activity as MainActivity).getDataRows()

        updateUI()

        binding.searchButton.setOnClickListener {
            val searchText = binding.edSearch.text.toString().trim()
            if (searchText.length >= 2) {
                performSearch(searchText)
            } else {
                binding.searchResult.text = "Введите минимум 2 символа для поиска"
            }
        }

        binding.loadFileButton.setOnClickListener {
            (activity as MainActivity).changeDataFile()
        }
    }

    private fun updateUI() {
        if (dataRows.isEmpty()) {
            binding.searchResult.text = "Файл с данными не загружен\nНажмите кнопку для выбора файла"
            binding.loadFileButton.visibility = View.VISIBLE
            binding.searchButton.isEnabled = false
            binding.edSearch.isEnabled = false
        } else {
            binding.searchResult.text = "Готов к поиску. В базе ${dataRows.size} записей\nФайл: ${getCurrentFileName()}"
            binding.loadFileButton.visibility = View.VISIBLE // Всегда видна!
            binding.searchButton.isEnabled = true
            binding.edSearch.isEnabled = true
        }
    }

    private fun getCurrentFileName(): String {
        return if (dataRows.isNotEmpty()) {
            dataRows.first().fileName
        } else {
            "не указан"
        }
    }

    private fun performSearch(searchString: String) {
        Log.d("SearchFragment", "Searching for: '$searchString'")

        val results = dataRows.filter { it.containsText(searchString) }
        Log.d("SearchFragment", "Found ${results.size} results")

        when {
            results.isEmpty() -> {
                binding.searchResult.text = "По запросу '$searchString' ничего не найдено"
            }
            results.size == 1 -> {
                val result = results.first()
                val formattedResult = result.getFormattedResult()
                Log.d("SearchFragment", "Single result found: $formattedResult")

                (activity as? MainActivity)?.setLastSearchResult(formattedResult)
                showResult(result)
            }
            results.size <= 10 -> {
                Log.d("SearchFragment", "Multiple results found: ${results.size}")
                showMultipleResultsDialog(results)
            }
            else -> {
                binding.searchResult.text = "Найдено слишком много результатов (${results.size}). Уточните запрос."
            }
        }
    }

    private fun showResult(dataRow: DataRow) {
        val formattedResult = dataRow.getFormattedResult()
        Log.d("SearchFragment", "Showing result: $formattedResult")

        (activity as MainActivity).replaceFragment(
            ResultsFragment.newInstance(formattedResult)
        )
        (activity as MainActivity).findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottom_navigation
        )?.selectedItemId = R.id.nav_results
    }

    private fun showMultipleResultsDialog(results: List<DataRow>) {
        val items = results.mapIndexed { index, row ->
            "${index + 1}. ${buildShortDescription(row)}"
        }.toTypedArray()

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Найдено ${results.size} совпадений:")
            setItems(items) { _, which ->
                val selectedResult = results[which]
                val formattedResult = selectedResult.getFormattedResult()

                (activity as? MainActivity)?.setLastSearchResult(formattedResult)
                showResult(selectedResult)
            }
            setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun buildShortDescription(row: DataRow): String {
        return if (row.searchText.isNotBlank()) {
            row.searchText
        } else {
            row.rowData.values.firstOrNull { it.isNotBlank() } ?: "Элемент без данных"
        }
    }

    override fun onResume() {
        super.onResume()
        dataRows = (activity as MainActivity).getDataRows()
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}