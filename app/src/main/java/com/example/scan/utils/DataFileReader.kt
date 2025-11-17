package com.example.scan.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.scan.model.DataRow
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayList

object DataFileReader {

    fun readDataFromUri(context: Context, uri: Uri, fileName: String): List<DataRow> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                readDataFromStream(inputStream, fileName)
            } else {
                ArrayList()
            }
        } catch (e: Exception) {
            Log.e("DataFileReader", "Error reading file: $fileName", e)
            ArrayList()
        }
    }

    private fun readDataFromStream(inputStream: java.io.InputStream, fileName: String): List<DataRow> {
        return try {
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
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
                Log.e("DataFileReader", "Error closing stream", e)
            }
        }
    }

    private fun readCSV(inputStream: java.io.InputStream, fileName: String): List<DataRow> {
        val rows = ArrayList<DataRow>()
        var headers = ArrayList<String>()

        val reader = BufferedReader(InputStreamReader(inputStream, "Cp1251"))
        try {
            var line: String?
            var isFirstLine = true

            while (reader.readLine().also { line = it } != null) {
                val values = parseCSVLine(line ?: "")

                if (isFirstLine) {
                    headers = ArrayList(values)
                    isFirstLine = false
                    Log.d("CSVReader", "Headers: ${headers.joinToString()}")
                } else {
                    if (values.isNotEmpty() && values.any { it.isNotBlank() }) {
                        val searchText = values.firstOrNull() ?: ""
                        val dataMap = headers.zip(values).toMap()

                        rows.add(
                            DataRow(
                            searchText = searchText,
                            rowData = dataMap,
                            fileName = fileName
                        )
                        )
                    }
                }
            }
        } finally {
            reader.close()
        }

        Log.d("CSVReader", "Total CSV rows read: ${rows.size}")
        return rows
    }

    private fun parseCSVLine(line: String): List<String> {
        val result = ArrayList<String>()
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