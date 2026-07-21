package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity

@Dao
interface ReviewStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReviewStateEntity)

    @Query("SELECT * FROM review_state WHERE exerciseId = :exerciseId")
    suspend fun getByExerciseId(exerciseId: String): ReviewStateEntity?

    @Query("SELECT exerciseId FROM review_state WHERE exerciseId IN (:exerciseIds)")
    suspend fun getExistingIds(exerciseIds: List<String>): List<String>
}
