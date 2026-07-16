package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.ExerciseEntity

data class ObjectiveTotalCount(val objective: String, val totalCount: Int)

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN review_state ON exercises.id = review_state.exerciseId
        WHERE review_state.dueDate <= :today
        """
    )
    suspend fun getDue(today: Long): List<ExerciseEntity>

    @Query(
        """
        SELECT * FROM exercises
        WHERE id NOT IN (SELECT exerciseId FROM review_state)
        LIMIT :limit
        """
    )
    suspend fun getNew(limit: Int): List<ExerciseEntity>

    @Query(
        """
        SELECT topics.certObjective AS objective, COUNT(*) AS totalCount
        FROM exercises
        INNER JOIN topics ON exercises.topicId = topics.id
        GROUP BY topics.certObjective
        """
    )
    suspend fun getTotalCountByObjective(): List<ObjectiveTotalCount>
}