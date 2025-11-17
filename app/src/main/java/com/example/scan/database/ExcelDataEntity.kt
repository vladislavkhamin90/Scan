package com.example.scan.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excel_data")
data class ExcelDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val searchText: String,
    val fileName: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)