package com.example.qr_scan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.qr_scan.R
import com.example.qr_scan.databinding.FragmentResultsBinding

class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем данные из аргументов
        arguments?.let { bundle ->
            val searchResult = bundle.getString("search_result")
            val title = bundle.getString("result_title")

            if (searchResult != null && title != null) {
                displayResults(searchResult, title)
            } else {
                binding.resultsText.text = getString(R.string.no_results_to_display)
            }
        } ?: run {
            binding.resultsText.text = getString(R.string.no_results_available)
        }
    }

    private fun displayResults(result: String, title: String) {
        val resultItems = result.split(";")
        val titleItems = title.split(";")

        val formattedResults = StringBuilder()

        for ((index, item) in titleItems.withIndex()) {
            if (index < resultItems.size) {
                formattedResults.append("$item: ${resultItems[index]}\n\n")
            }
        }

        binding.resultsText.text = formattedResults.toString()
    }

    companion object {
        fun newInstance(result: String? = null, title: String? = null): ResultsFragment {
            return ResultsFragment().apply {
                arguments = Bundle().apply {
                    putString("search_result", result)
                    putString("result_title", title)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}