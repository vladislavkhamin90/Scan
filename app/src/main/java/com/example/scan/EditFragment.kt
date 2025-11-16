package com.example.scan

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.scan.databinding.FragmentEditBinding
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class EditFragment : Fragment() {
    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!
    private var resultData: String = ""
    private val editTexts = mutableListOf<EditText>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultData = arguments?.getString("result_data") ?: ""
        Log.d("EditFragment", "Editing data: ${resultData.take(50)}...")

        setupForm()
        setupSaveButton()
    }

    private fun setupForm() {
        val lines = resultData.split("\n\n").filter { it.isNotBlank() }
        val container = binding.fieldsContainer

        lines.forEach { line ->
            val parts = line.split(": ", limit = 2)
            if (parts.size == 2) {
                val label = parts[0]
                val value = parts[1]

                val labelView = TextView(requireContext()).apply {
                    text = label
                    textSize = 16f
                    setTextColor(resources.getColor(android.R.color.white, null))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 16, 0, 8)
                    }
                }
                container.addView(labelView)

                val editText = EditText(requireContext()).apply {
                    setText(value)
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                container.addView(editText)
                editTexts.add(editText)
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        try {
            val lines = resultData.split("\n\n").filter { it.isNotBlank() }
            val updatedData = StringBuilder()

            lines.forEachIndexed { index, line ->
                val parts = line.split(": ", limit = 2)
                if (parts.size == 2 && index < editTexts.size) {
                    val label = parts[0]
                    val newValue = editTexts[index].text.toString()
                    updatedData.append("$label: $newValue\n\n")
                }
            }

            val finalData = updatedData.toString().trim()
            val success = saveToDocuments(finalData)

            if (success) {
                val resultsFragment = ResultsFragment.newInstance(finalData)
                (activity as MainActivity).replaceFragment(resultsFragment)
            }

        } catch (e: Exception) {
            Log.e("EditFragment", "Error saving changes", e)
        }
    }

    private fun saveToDocuments(updatedData: String): Boolean {
        return try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            val newFile = File(documentsDir, "new.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val output = StringBuilder()
            output.append("=== Изменения от $timestamp ===\n")
            output.append(updatedData)
            output.append("\n\n")

            if (newFile.exists()) {
                FileWriter(newFile, true).use { writer ->
                    writer.write(output.toString())
                }
                Log.d("EditFragment", "Changes appended to: ${newFile.absolutePath}")
            } else {
                FileWriter(newFile).use { writer ->
                    writer.write(output.toString())
                }
                Log.d("EditFragment", "New file created: ${newFile.absolutePath}")
            }

            true
        } catch (e: Exception) {
            Log.e("EditFragment", "Error writing to Documents folder", e)
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(resultData: String): EditFragment {
            return EditFragment().apply {
                arguments = Bundle().apply {
                    putString("result_data", resultData)
                }
            }
        }
    }
}