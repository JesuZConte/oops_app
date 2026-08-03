package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.json.Json

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val reviewStateDao: ReviewStateDao,
    private val json: Json
) : ExerciseRepository {

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> =
        exerciseDao.getDue(today.toEpochDay()).take(limit).map { it.toDomain(json) }

    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> =
        exerciseDao.getByUnit(unitId).map { it.toDomain(json) }

    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> =
        exerciseDao.getBySection(sectionId).map { it.toDomain(json) }

    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        reviewStateDao.getByExerciseId(exerciseId)?.toDomain()

    override suspend fun saveReviewState(state: ReviewState) {
        reviewStateDao.upsert(state.toEntity())
    }

    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        if (exerciseIds.isEmpty()) emptyList() else reviewStateDao.getExistingIds(exerciseIds)
}

internal fun ExerciseEntity.toDomain(json: Json): Exercise {
    val content = json.decodeFromString(ExerciseContent.serializer(), payload)
    return Exercise(
        id = id,
        unitId = unitId,
        type = type,
        payload = payload,
        difficulty = difficulty,
        examVersion = examVersion,
        conceptId = content.conceptId,
        role = content.role,
        pathOrder = content.pathOrder,
        dependsOn = content.dependsOn
    )
}

private fun ReviewStateEntity.toDomain() = ReviewState(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = LocalDate.ofEpochDay(dueDate),
    lastReviewedAt = LocalDate.ofEpochDay(lastReviewedAt)
)

private fun ReviewState.toEntity() = ReviewStateEntity(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = dueDate.toEpochDay(),
    lastReviewedAt = lastReviewedAt.toEpochDay()
)
