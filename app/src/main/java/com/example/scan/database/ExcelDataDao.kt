package com.example.scan.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExcelDataDao {
    @Insert
    suspend fun insertAll(data: List<ExcelDataEntity>)

    @Query("SELECT * FROM excel_data")
    suspend fun getAll(): List<ExcelDataEntity>

    @Query("DELETE FROM excel_data")
    suspend fun deleteAll()

    @Query("SELECT * FROM excel_data WHERE searchText LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<ExcelDataEntity>
}