package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

private const val TARGET_SIZE = 12
private const val PRIOR_SAMPLE_SIZE = 3

class GetCheckpointSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(sectionId: String): List<Exercise> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val currentSection = sections.find { it.id == sectionId } ?: return emptyList()
        val earlierSectionIds = sections.filter { it.orderIndex < currentSection.orderIndex }.map { it.id }

        val currentPool = exerciseRepository.getExercisesBySection(sectionId)
        val priorPool = earlierSectionIds.flatMap { exerciseRepository.getExercisesBySection(it) }

        val priorSample = priorPool.shuffled().take(minOf(PRIOR_SAMPLE_SIZE, priorPool.size))
        val currentSampleSize = (TARGET_SIZE - priorSample.size).coerceAtMost(currentPool.size)
        val currentSample = currentPool.shuffled().take(currentSampleSize)

        return (currentSample + priorSample).shuffled()
    }
}
