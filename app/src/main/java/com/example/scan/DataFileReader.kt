package com.example.scan

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.serialization.json.Json

object DataFileReader {

    fun readDataFromUri(context: Context, uri: Uri, fileName: String): List<DataRow> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bufferedStream = BufferedInputStream(inputStream)
                bufferedStream.mark(10)
                val signature = ByteArray(4)
                bufferedStream.read(signature)
                bufferedStream.reset()

                if (signature[0] == 0x50.toByte() && signature[1] == 0x4B.toByte() &&
                    signature[2] == 0x03.toByte() && signature[3] == 0x04.toByte()) {
                    Log.d("DataFileReader", "Detected Excel file: $fileName")
                    ExcelDataFileReader.readExcel(bufferedStream, fileName)
                } else {
                    Log.d("DataFileReader", "Detected CSV file: $fileName")
                    readCSV(bufferedStream, fileName)
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("DataFileReader", "Error reading file: $fileName", e)
            emptyList()
        }
    }

    private fun readCSV(inputStream: java.io.InputStream, fileName: String): List<DataRow> {
        val rows = mutableListOf<DataRow>()
        var headers = emptyList<String>()

        BufferedReader(InputStreamReader(inputStream, "Cp1251")).use { reader ->
            var line: String?
            var isFirstLine = true

            while (reader.readLine().also { line = it } != null) {
                val values = parseCSVLine(line ?: "")

                if (isFirstLine) {
                    headers = values
                    isFirstLine = false
                    Log.d("CSVReader", "Headers: ${headers.joinToString()}")
                } else {
                    if (values.isNotEmpty() && values.any { it.isNotBlank() }) {
                        // Предполагаем, что первая колонка - это ФИО для поиска
                        val searchText = values.firstOrNull() ?: ""
                        val dataMap = headers.zip(values).toMap()

                        rows.add(DataRow(
                            searchText = searchText,
                            rowData = dataMap,
                            fileName = fileName
                        ))
                    }
                }
            }
        }
        Log.d("CSVReader", "Total CSV rows read: ${rows.size}")
        return rows
    }

    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ';' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}