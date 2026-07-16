package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity

data class ObjectiveMasteryCount(val objective: String, val masteredCount: Int)

@Dao
interface ReviewStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReviewStateEntity)

    @Query("SELECT * FROM review_state WHERE exerciseId = :exerciseId")
    suspend fun getByExerciseId(exerciseId: String): ReviewStateEntity?

    @Query(
        """
        SELECT topics.certObjective AS objective, COUNT(*) AS masteredCount
        FROM review_state
        INNER JOIN exercises ON review_state.exerciseId = exercises.id
        INNER JOIN topics ON exercises.topicId = topics.id
        WHERE review_state.repetitions >= 2
        GROUP BY topics.certObjective
        """
    )
    suspend fun getMasteredCountByObjective(): List<ObjectiveMasteryCount>
}