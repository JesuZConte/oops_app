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
