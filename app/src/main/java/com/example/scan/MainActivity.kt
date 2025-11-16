package com.example.scan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.scan.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private lateinit var excelDataDao: ExcelDataDao
    private var dataRows: List<DataRow> = emptyList()
    private var lastSearchResult: String? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileName = getFileName(uri) ?: "unknown_file"
                loadDataFromUri(uri, fileName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "excel_database"
        ).build()
        excelDataDao = database.excelDataDao()

        checkExistingData()

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_scanner -> {
                    if (dataRows.isNotEmpty()) {
                        replaceFragment(ScannerFragment())
                    } else {
                        showFilePicker()
                    }
                    true
                }
                R.id.nav_search -> {
                    if (dataRows.isNotEmpty()) {
                        replaceFragment(SearchFragment())
                    } else {
                        showFilePicker()
                    }
                    true
                }
                R.id.nav_results -> {
                    if (lastSearchResult != null) {
                        replaceFragment(ResultsFragment.newInstance(lastSearchResult!!))
                    } else {
                        replaceFragment(ResultsFragment.newInstance("Нет данных для отображения\n\nВыполните поиск для отображения результатов"))
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun checkExistingData() {
        lifecycleScope.launch {
            try {
                val existingData = loadDataFromDatabase()
                if (existingData.isNotEmpty()) {
                    dataRows = existingData
                    replaceFragment(SearchFragment())
                    showBottomNavigation(true)
                    Log.d("MainActivity", "Loaded ${dataRows.size} rows from database")
                } else {
                    showFilePicker()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading from database", e)
                showFilePicker()
            }
        }
    }

    private suspend fun loadDataFromDatabase(): List<DataRow> {
        val entities = excelDataDao.getAll()
        return entities.map { entity ->
            DataRow(
                searchText = entity.searchText,
                rowData = Json.decodeFromString(entity.value),
                fileName = entity.fileName
            )
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

    private fun loadDataFromUri(uri: Uri, fileName: String) {
        lifecycleScope.launch {
            try {
                val fileData = DataFileReader.readDataFromUri(this@MainActivity, uri, fileName)
                if (fileData.isNotEmpty()) {
                    saveDataToDatabase(fileData, fileName)
                    dataRows = fileData
                    showBottomNavigation(true)
                    replaceFragment(SearchFragment())

                    Toast.makeText(
                        this@MainActivity,
                        "Загружено ${fileData.size} записей",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("MainActivity", "Saved ${fileData.size} rows to database")
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Файл пуст или не содержит данных",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading data from file", e)
                Toast.makeText(
                    this@MainActivity,
                    "Ошибка загрузки файла",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
        Log.d("MainActivity", "Fragment replaced: ${fragment::class.java.simpleName}")
    }

    fun setLastSearchResult(result: String) {
        lastSearchResult = result
        Log.d("MainActivity", "Last search result saved: ${result.take(50)}...")
    }

    fun getDataRows(): List<DataRow> = dataRows

    fun showBottomNavigation(show: Boolean) {
        binding.bottomNavigation.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun showFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
        }
        filePickerLauncher.launch(intent)
    }

    fun changeDataFile() {
        showFilePicker()
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex("_display_name")
                    if (displayNameIndex != -1) {
                        cursor.getString(displayNameIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting file name", e)
            null
        }
    }
}