package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.UnitEntity

@Dao
interface UnitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitEntity>)

    @Query("SELECT * FROM units WHERE sectionId = :sectionId ORDER BY orderIndex")
    suspend fun getBySection(sectionId: String): List<UnitEntity>

    @Query("SELECT * FROM units ORDER BY orderIndex")
    suspend fun getAll(): List<UnitEntity>

    @Query("DELETE FROM units")
    suspend fun clearAll()
}
