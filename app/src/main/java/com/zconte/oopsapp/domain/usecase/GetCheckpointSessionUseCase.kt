package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.answerableOnly
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

private const val SIZE_FLOOR = 8
private const val SIZE_GROWTH_PER_SECTION = 2
private const val SIZE_CEILING = 20

/**
 * How many questions a section's mandatory checkpoint has, given how many sections the player
 * has traversed (1-based, including the section the checkpoint is for). Grows toward the real
 * exam's scale without becoming it -- see the design spec's floor/growth/ceiling decision.
 */
fun checkpointSize(sectionsTraversed: Int): Int =
    (SIZE_FLOOR + SIZE_GROWTH_PER_SECTION * (sectionsTraversed - 1)).coerceAtMost(SIZE_CEILING)

class GetCheckpointSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(sectionId: String, today: LocalDate): List<Exercise> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val currentIndex = sections.indexOfFirst { it.id == sectionId }
        if (currentIndex < 0) return emptyList()

        val targetSize = checkpointSize(sectionsTraversed = currentIndex + 1)

        val currentPool = exerciseRepository.getExercisesBySection(sectionId).answerableOnly()
        val priorPool = sections.take(currentIndex)
            .flatMap { exerciseRepository.getExercisesBySection(it.id) }
            .answerableOnly()

        val priorTargetSize = (targetSize / 2).coerceAtMost(priorPool.size)
        val currentTargetSize = (targetSize - priorTargetSize).coerceAtMost(currentPool.size)

        val dueIds = exerciseRepository.getDueExercises(today, Int.MAX_VALUE).map { it.id }.toSet()
        val (duePrior, restPrior) = priorPool.partition { it.id in dueIds }
        val priorSample = (duePrior.shuffled() + restPrior.shuffled()).take(priorTargetSize)

        val currentSample = currentPool.shuffled().take(currentTargetSize)

        return (currentSample + priorSample).shuffled()
    }
}
