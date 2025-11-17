package com.example.scan.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.scan.MainActivity
import com.example.scan.databinding.FragmentResultsBinding

class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!
    private var resultData: String = ""

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

        resultData = arguments?.getString("result_data") ?: ""
        Log.d("ResultsFragment", "Received result data: ${resultData.take(50)}...")

        if (resultData.isNotBlank()) {
            binding.resultsText.text = resultData
            Log.d("ResultsFragment", "Text set successfully, length: ${resultData.length}")
        } else {
            binding.resultsText.text = "Нет данных для отображения"
            Log.d("ResultsFragment", "No data received")
        }

        binding.editButton.setOnClickListener {
            val editFragment = EditFragment.newInstance(resultData)
            (activity as MainActivity).replaceFragment(editFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(resultData: String): ResultsFragment {
            Log.d("ResultsFragment", "Creating new instance with data length: ${resultData.length}")
            return ResultsFragment().apply {
                arguments = Bundle().apply {
                    putString("result_data", resultData)
                }
            }
        }
    }
}