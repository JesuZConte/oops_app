package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val reviewStateDao: ReviewStateDao
) : ExerciseRepository {

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> =
        exerciseDao.getDue(today.toEpochDay()).take(limit).map { it.toDomain() }

    override suspend fun getNewExercises(limit: Int): List<Exercise> =
        exerciseDao.getNew(limit).map { it.toDomain() }

    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> =
        exerciseDao.getByUnit(unitId).map { it.toDomain() }

    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> =
        exerciseDao.getBySection(sectionId).map { it.toDomain() }

    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        reviewStateDao.getByExerciseId(exerciseId)?.toDomain()

    override suspend fun saveReviewState(state: ReviewState) {
        reviewStateDao.upsert(state.toEntity())
    }

    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        if (exerciseIds.isEmpty()) emptyList() else reviewStateDao.getExistingIds(exerciseIds)
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id, unitId = unitId, type = type, payload = payload, difficulty = difficulty, examVersion = examVersion
)

private fun ReviewStateEntity.toDomain() = ReviewState(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = LocalDate.ofEpochDay(dueDate)
)

private fun ReviewState.toEntity() = ReviewStateEntity(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = dueDate.toEpochDay()
)
