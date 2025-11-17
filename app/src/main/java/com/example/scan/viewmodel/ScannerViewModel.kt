package com.example.scan.viewmodel

import androidx.lifecycle.ViewModel
import com.example.scan.model.DataRow
import com.example.scan.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScannerViewModel(private val repository: DataRepository) : ViewModel() {

    val dataRows: StateFlow<List<DataRow>> = repository.dataRows

    private val _scanResult = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanResult: StateFlow<ScanState> = _scanResult

    fun processScanResult(scanResult: String) {
        val cleanedResult = cleanScanResult(scanResult)

        if (cleanedResult.isBlank()) {
            _scanResult.value = ScanState.Error("Код не распознан")
            return
        }

        val results = repository.searchData(cleanedResult)

        when {
            results.isEmpty() -> {
                _scanResult.value = ScanState.Error("По отсканированному коду '$cleanedResult' ничего не найдено")
            }
            results.size == 1 -> {
                _scanResult.value = ScanState.SingleResult(results.first())
            }
            results.size <= 10 -> {
                _scanResult.value = ScanState.MultipleResults(results)
            }
            else -> {
                _scanResult.value = ScanState.Error("Найдено ${results.size} совпадений. Уточните запрос.")
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = ScanState.Idle
    }

    private fun cleanScanResult(scanResult: String): String {
        var cleaned = scanResult.replace(";", "")
        cleaned = cleaned.trim()
        return cleaned
    }
}

sealed class ScanState {
    object Idle : ScanState()
    data class Error(val message: String) : ScanState()
    data class SingleResult(val dataRow: DataRow) : ScanState()
    data class MultipleResults(val results: List<DataRow>) : ScanState()
}