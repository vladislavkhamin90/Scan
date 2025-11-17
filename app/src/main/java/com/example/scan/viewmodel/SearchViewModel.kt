package com.example.scan.viewmodel

import androidx.lifecycle.ViewModel
import com.example.scan.model.DataRow
import com.example.scan.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SearchViewModel(private val repository: DataRepository) : ViewModel() {

    val dataRows: StateFlow<List<DataRow>> = repository.dataRows
    val currentFileName: StateFlow<String> = repository.currentFileName

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    fun performSearch(query: String) {
        if (query.length < 2) {
            _searchState.value = SearchState.Error("Введите минимум 2 символа для поиска")
            return
        }

        val results = repository.searchData(query)

        when {
            results.isEmpty() -> {
                _searchState.value = SearchState.Error("По запросу '$query' ничего не найдено")
            }
            results.size == 1 -> {
                _searchState.value = SearchState.SingleResult(results.first())
            }
            results.size <= 10 -> {
                _searchState.value = SearchState.MultipleResults(results)
            }
            else -> {
                _searchState.value = SearchState.Error("Найдено слишком много результатов (${results.size}). Уточните запрос.")
            }
        }
    }

    fun clearSearch() {
        _searchState.value = SearchState.Idle
    }
}

sealed class SearchState {
    object Idle : SearchState()
    data class Error(val message: String) : SearchState()
    data class SingleResult(val dataRow: DataRow) : SearchState()
    data class MultipleResults(val results: List<DataRow>) : SearchState()
}