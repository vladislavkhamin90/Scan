package com.example.scan

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ExcelDataEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun excelDataDao(): ExcelDataDao
}