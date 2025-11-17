package com.example.scan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.scan.databinding.ActivityMainBinding
import com.example.scan.database.AppDatabase
import com.example.scan.repository.DataRepository
import com.example.scan.ui.fragments.ResultsFragment
import com.example.scan.ui.fragments.ScannerFragment
import com.example.scan.ui.fragments.SearchFragment
import com.example.scan.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    lateinit var repository: DataRepository

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    private var lastSearchResult: String? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileName = getFileName(uri) ?: "unknown_file"
                mainViewModel.loadDataFromUri(this, uri, fileName)
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

        repository = DataRepository(database.excelDataDao())

        setupObservers()
        setupBottomNavigation()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            mainViewModel.loadingState.collect { state: com.example.scan.repository.LoadingState ->
                when (state) {
                    is com.example.scan.repository.LoadingState.Success -> {
                        if (state.count > 0) {
                            showBottomNavigation(true)
                            replaceFragment(SearchFragment())
                            Toast.makeText(
                                this@MainActivity,
                                "Загружено ${state.count} записей",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is com.example.scan.repository.LoadingState.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            state.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_scanner -> {
                    if (mainViewModel.dataRows.value?.isNotEmpty() == true) {
                        replaceFragment(ScannerFragment())
                    } else {
                        showFilePicker()
                    }
                    true
                }
                R.id.nav_search -> {
                    if (mainViewModel.dataRows.value?.isNotEmpty() == true) {
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

    fun getDataRows(): List<com.example.scan.model.DataRow> {
        return mainViewModel.dataRows.value ?: emptyList()
    }

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

class MainViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}