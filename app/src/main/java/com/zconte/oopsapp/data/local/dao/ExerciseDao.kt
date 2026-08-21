package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.ExerciseEntity

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises")
    suspend fun clearAll()

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN review_state ON exercises.id = review_state.exerciseId
        WHERE review_state.dueDate <= :today
        ORDER BY review_state.repetitions ASC, review_state.lastReviewedAt DESC,
            review_state.easeFactor ASC, exercises.id ASC
        """
    )
    suspend fun getDue(today: Long): List<ExerciseEntity>

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN review_state ON exercises.id = review_state.exerciseId
        WHERE review_state.dueDate <= :cutoff
        """
    )
    suspend fun getStale(cutoff: Long): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE unitId = :unitId")
    suspend fun getByUnit(unitId: String): List<ExerciseEntity>

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN units ON exercises.unitId = units.id
        WHERE units.sectionId = :sectionId
        """
    )
    suspend fun getBySection(sectionId: String): List<ExerciseEntity>
}
