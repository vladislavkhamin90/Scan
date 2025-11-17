package com.example.scan.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scan.repository.DataRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: DataRepository) : ViewModel() {

    val dataRows: StateFlow<List<com.example.scan.model.DataRow>> = repository.dataRows
    val loadingState: StateFlow<com.example.scan.repository.LoadingState> = repository.loadingState
    val currentFileName: StateFlow<String> = repository.currentFileName

    fun loadDataFromUri(context: Context, uri: Uri, fileName: String) {
        repository.loadDataFromUri(context, uri, fileName)
    }

    fun changeDataFile(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            repository.clearData()
            repository.loadDataFromUri(context, uri, fileName)
        }
    }
}