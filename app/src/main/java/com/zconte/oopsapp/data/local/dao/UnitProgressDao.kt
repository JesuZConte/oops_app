package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity

@Dao
interface UnitProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UnitProgressEntity)

    @Query("SELECT * FROM unit_progress WHERE unitId = :unitId")
    suspend fun getByUnit(unitId: String): UnitProgressEntity?

    @Query("SELECT * FROM unit_progress WHERE completed = 1")
    suspend fun getCompleted(): List<UnitProgressEntity>
}
