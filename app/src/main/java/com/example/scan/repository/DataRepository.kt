package com.example.scan.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.asFlow
import com.example.scan.database.ExcelDataDao
import com.example.scan.database.ExcelDataEntity
import com.example.scan.model.DataRow
import com.example.scan.utils.DataFileReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataRepository(private val excelDataDao: ExcelDataDao) {

    private val _dataRows = MutableStateFlow<List<DataRow>>(emptyList())
    val dataRows: StateFlow<List<DataRow>> = _dataRows

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState

    private val _currentFileName = MutableStateFlow<String>("")
    val currentFileName: StateFlow<String> = _currentFileName

    init {
        loadDataFromDatabase()
    }

    private fun loadDataFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _loadingState.value = LoadingState.Loading
                val entities = excelDataDao.getAll()
                val rows = entities.map { entity ->
                    DataRow(
                        searchText = entity.searchText,
                        rowData = Json.decodeFromString(entity.value),
                        fileName = entity.fileName
                    )
                }
                _dataRows.value = rows
                if (rows.isNotEmpty()) {
                    _currentFileName.value = rows.first().fileName
                }
                _loadingState.value = LoadingState.Success(rows.size)
            } catch (e: Exception) {
                Log.e("DataRepository", "Error loading from database", e)
                _loadingState.value = LoadingState.Error("Ошибка загрузки данных")
            }
        }
    }

    fun loadDataFromUri(context: Context, uri: Uri, fileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _loadingState.value = LoadingState.Loading
                val fileData = DataFileReader.readDataFromUri(context, uri, fileName)

                if (fileData.isNotEmpty()) {
                    saveDataToDatabase(fileData, fileName)
                    _dataRows.value = fileData
                    _currentFileName.value = fileName
                    _loadingState.value = LoadingState.Success(fileData.size)
                } else {
                    _loadingState.value = LoadingState.Error("Файл пуст или не содержит данных")
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error loading data from file", e)
                _loadingState.value = LoadingState.Error("Ошибка загрузки файла")
            }
        }
    }

    private suspend fun saveDataToDatabase(data: List<DataRow>, fileName: String) {
        val entities = data.map { dataRow ->
            ExcelDataEntity(
                searchText = dataRow.searchText,
                fileName = fileName,
                value = Json.encodeToString(dataRow.rowData)
            )
        }
        excelDataDao.deleteAll()
        excelDataDao.insertAll(entities)
    }

    fun searchData(query: String): List<DataRow> {
        val currentData = _dataRows.value
        return currentData.filter { it.containsText(query) }
    }

    fun clearData() {
        CoroutineScope(Dispatchers.IO).launch {
            excelDataDao.deleteAll()
            _dataRows.value = emptyList()
            _currentFileName.value = ""
            _loadingState.value = LoadingState.Success(0)
        }
    }
}

sealed class LoadingState {
    object Loading : LoadingState()
    data class Success(val count: Int) : LoadingState()
    data class Error(val message: String) : LoadingState()
    object Idle : LoadingState()
}