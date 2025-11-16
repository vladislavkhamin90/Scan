package com.example.scan

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DataRow(
    val searchText: String,
    val rowData: Map<String, String>,
    val fileName: String = ""
) {
    fun getFormattedResult(): String {
        val result = StringBuilder()
        rowData.forEach { (header, value) ->
            if (value.isNotBlank()) {
                result.append("$header: $value\n\n")
            }
        }
        return result.toString().trim()
    }

    fun containsText(text: String): Boolean {
        return searchText.contains(text, ignoreCase = true) ||
                rowData.values.any { value ->
                    value.contains(text, ignoreCase = true)
                }
    }
}