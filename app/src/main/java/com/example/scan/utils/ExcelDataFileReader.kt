package com.example.scan.utils

import android.util.Log
import com.example.scan.model.DataRow
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.DataFormatter
import java.io.InputStream

object ExcelDataFileReader {

    fun readExcel(inputStream: InputStream, fileName: String): List<DataRow> {
        val rows = mutableListOf<DataRow>()

        try {
            val workbook = WorkbookFactory.create(inputStream)
            val formatter = DataFormatter()

            val sheet = workbook.getSheetAt(0)
            var headers = emptyList<String>()
            var isFirstRow = true

            for (row in sheet) {
                val values = mutableListOf<String>()

                for (cell in row) {
                    val cellValue = formatter.formatCellValue(cell)
                    values.add(cellValue)
                }

                if (isFirstRow) {
                    headers = values
                    isFirstRow = false
                    Log.d("ExcelReader", "Headers: ${headers.joinToString()}")
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

            workbook.close()
            Log.d("ExcelReader", "Total Excel rows read: ${rows.size}")

        } catch (e: Exception) {
            Log.e("ExcelDataFileReader", "Error parsing Excel", e)
        }

        return rows
    }
}