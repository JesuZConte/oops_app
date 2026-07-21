package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity

@Dao
interface CheckpointAttemptDao {
    @Insert
    suspend fun insert(attempt: CheckpointAttemptEntity)

    @Query("SELECT * FROM checkpoint_attempts WHERE sectionId = :sectionId ORDER BY takenAt DESC")
    suspend fun getBySection(sectionId: String): List<CheckpointAttemptEntity>
}
