package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.SectionEntity

@Dao
interface SectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<SectionEntity>)

    @Query("SELECT * FROM sections ORDER BY orderIndex")
    suspend fun getAll(): List<SectionEntity>

    @Query("DELETE FROM sections")
    suspend fun clearAll()
}
