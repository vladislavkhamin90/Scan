package com.example.scan.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.scan.MainActivity
import com.example.scan.databinding.FragmentScannerBinding
import com.example.scan.model.DataRow
import com.example.scan.viewmodel.ScanState
import com.example.scan.viewmodel.ScannerViewModel
import com.example.scan.viewmodel.ScannerViewModelFactory
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ScannerFragment : Fragment() {
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val scannerViewModel: ScannerViewModel by viewModels {
        ScannerViewModelFactory((requireActivity() as MainActivity).repository)
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scanResult = result.contents
            scannerViewModel.processScanResult(scanResult)
        } else {
            (activity as? MainActivity)?.supportFragmentManager?.popBackStack()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchScanner()
        } else {
            showAlert("Доступ к камере запрещен", "Для сканирования QR-кодов необходимо разрешение на использование камеры") {
                (activity as? MainActivity)?.supportFragmentManager?.popBackStack()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        checkDataAndPermissions()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            scannerViewModel.dataRows.collect { dataRows ->
                if (dataRows.isEmpty()) {
                    showAlert("Файл не загружен", "Сначала выберите файл с данными") {
                        (activity as? MainActivity)?.supportFragmentManager?.popBackStack()
                    }
                }
            }
        }

        lifecycleScope.launch {
            scannerViewModel.scanResult.collect { state ->
                when (state) {
                    is ScanState.Error -> {
                        showAlert("Ошибка", state.message) {
                            launchScanner()
                        }
                    }
                    is ScanState.SingleResult -> {
                        showResult(state.dataRow)
                    }
                    is ScanState.MultipleResults -> {
                        showMultipleResultsDialog(state.results)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun checkDataAndPermissions() {
        if (scannerViewModel.dataRows.value?.isNotEmpty() == true) {
            checkCameraPermission()
        } else {
            showAlert("Файл не загружен", "Сначала выберите файл с данными") {
                (activity as? MainActivity)?.supportFragmentManager?.popBackStack()
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchScanner()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt("Наведите камеру на QR-код или штрих-код")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }
        barcodeLauncher.launch(options)
    }

    private fun showResult(dataRow: DataRow) {
        val formattedResult = dataRow.getFormattedResult()
        (activity as? MainActivity)?.setLastSearchResult(formattedResult)

        (activity as MainActivity).replaceFragment(
            ResultsFragment.newInstance(formattedResult)
        )
        (activity as MainActivity).findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            com.example.scan.R.id.bottom_navigation
        )?.selectedItemId = com.example.scan.R.id.nav_results
    }

    private fun showMultipleResultsDialog(results: List<DataRow>) {
        val items = results.mapIndexed { index, row ->
            "${index + 1}. ${buildShortDescription(row)}"
        }.toTypedArray()

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Найдено ${results.size} совпадений:")
            setItems(items) { _, which ->
                showResult(results[which])
            }
            setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
                launchScanner()
            }
            setOnDismissListener {
                launchScanner()
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

    private fun showAlert(title: String, message: String, onDismiss: (() -> Unit)? = null) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
            setOnDismissListener {
                onDismiss?.invoke()
            }
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}