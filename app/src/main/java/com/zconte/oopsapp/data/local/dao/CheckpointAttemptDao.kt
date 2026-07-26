package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptFailureEntity

@Dao
interface CheckpointAttemptDao {
    @Insert
    suspend fun insert(attempt: CheckpointAttemptEntity): Long

    @Insert
    suspend fun insertFailures(failures: List<CheckpointAttemptFailureEntity>)

    // Returns attempts of every kind (review and placement) for this section -- callers that
    // only want one kind must filter the result by `kind` themselves.
    @Query("SELECT * FROM checkpoint_attempts WHERE sectionId = :sectionId ORDER BY takenAt DESC")
    suspend fun getBySection(sectionId: String): List<CheckpointAttemptEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM checkpoint_attempts WHERE sectionId = :sectionId AND kind = :kind AND passed = 1)")
    suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean

    @Query("SELECT * FROM checkpoint_attempts WHERE sectionId = :sectionId AND kind = :kind ORDER BY id DESC LIMIT 1")
    suspend fun getLatestAttempt(sectionId: String, kind: String): CheckpointAttemptEntity?

    @Query("SELECT exerciseId FROM checkpoint_attempt_failures WHERE attemptId = :attemptId")
    suspend fun getFailedExerciseIds(attemptId: Long): List<String>
}
