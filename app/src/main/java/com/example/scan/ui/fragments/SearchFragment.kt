package com.example.scan.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.scan.MainActivity
import com.example.scan.R
import com.example.scan.databinding.FragmentSearchBinding
import com.example.scan.model.DataRow
import com.example.scan.viewmodel.SearchState
import com.example.scan.viewmodel.SearchViewModel
import com.example.scan.viewmodel.SearchViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: SearchViewModel by viewModels {
        SearchViewModelFactory((requireActivity() as MainActivity).repository)
    }

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

        setupObservers()
        setupListeners()
        updateUI()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            searchViewModel.dataRows.collect { dataRows ->
                updateUI(dataRows.isNotEmpty())
                if (dataRows.isNotEmpty()) {
                    binding.searchResult.text = "Готов к поиску. В базе ${dataRows.size} записей"
                }
            }
        }

        lifecycleScope.launch {
            searchViewModel.currentFileName.collect { fileName ->
                if (fileName.isNotEmpty()) {
                    binding.searchResult.text = "Готов к поиску. Файл: $fileName"
                }
            }
        }

        lifecycleScope.launch {
            searchViewModel.searchState.collect { state ->
                when (state) {
                    is SearchState.Error -> {
                        binding.searchResult.text = state.message
                    }
                    is SearchState.SingleResult -> {
                        showResult(state.dataRow)
                    }
                    is SearchState.MultipleResults -> {
                        showMultipleResultsDialog(state.results)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupListeners() {
        binding.searchButton.setOnClickListener {
            val searchText = binding.edSearch.text.toString().trim()
            searchViewModel.performSearch(searchText)
        }

        binding.loadFileButton.setOnClickListener {
            (activity as MainActivity).changeDataFile()
        }
    }

    private fun updateUI(hasData: Boolean = false) {
        val actualHasData = searchViewModel.dataRows.value?.isNotEmpty() == true
        binding.loadFileButton.visibility = View.VISIBLE
        binding.searchButton.isEnabled = actualHasData
        binding.edSearch.isEnabled = actualHasData

        if (!actualHasData) {
            binding.searchResult.text = "Файл с данными не загружен\nНажмите кнопку для выбора файла"
        }
    }

    private fun showResult(dataRow: DataRow) {
        val formattedResult = dataRow.getFormattedResult()
        Log.d("SearchFragment", "Showing result: $formattedResult")

        (activity as MainActivity).setLastSearchResult(formattedResult)
        (activity as MainActivity).replaceFragment(
            ResultsFragment.newInstance(formattedResult)
        )
        (activity as MainActivity).findViewById<BottomNavigationView>(
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
        return row.searchText.ifBlank {
            row.rowData.values.firstOrNull { it.isNotBlank() } ?: "Элемент без данных"
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}