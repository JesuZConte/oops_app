package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
import com.zconte.oopsapp.testutil.FakeExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForNextStep(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetNextStudyStepUseCaseTest {

    private val today = LocalDate.of(2026, 7, 26)

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "fill_blank", "{}", 1)
    private fun played(unitId: String) = CompletedUnit(unitId, UnitCompletionSource.PLAYED)

    private fun useCase(
        contentRepository: ContentRepository,
        exerciseRepository: FakeExerciseRepository,
        checkpointRepository: FakeCheckpointRepository
    ) = GetNextStudyStepUseCase(
        GetTodaySessionUseCase(exerciseRepository, GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, checkpointRepository, IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository)))),
        GetLearningPathUseCase(contentRepository, checkpointRepository, IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository))
    )

    @Test
    fun `returns DailySession when there is due content today`() = runTest {
        val contentRepository = FakeContentRepositoryForNextStep(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepository(dueExercises = listOf(exercise("due-1", "s1-u1")))

        val result = useCase(contentRepository, exerciseRepository, FakeCheckpointRepository())(today)

        assertEquals(NextStudyStep.DailySession, result)
    }

    @Test
    fun `returns Checkpoint for the first section whose units are done but not checkpoint-satisfied`() = runTest {
        val contentRepository = FakeContentRepositoryForNextStep(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val exerciseRepository = FakeExerciseRepository()

        val result = useCase(contentRepository, exerciseRepository, FakeCheckpointRepository())(today)

        assertEquals(NextStudyStep.Checkpoint("s1"), result)
    }

    @Test
    fun `returns Checkpoint even when its retry is locked -- CheckpointViewModel shows the explanation, not this use case`() = runTest {
        val contentRepository = FakeContentRepositoryForNextStep(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false,
            takenAt = today, failedExerciseIds = listOf("ex-1")
        )
        val exerciseRepository = FakeExerciseRepository()

        val result = useCase(contentRepository, exerciseRepository, checkpointRepository)(today)

        assertEquals(NextStudyStep.Checkpoint("s1"), result)
    }

    @Test
    fun `returns NothingPending when every section is checkpoint-satisfied and nothing is due`() = runTest {
        val contentRepository = FakeContentRepositoryForNextStep(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 90, passed = true,
            takenAt = today, failedExerciseIds = emptyList()
        )
        val exerciseRepository = FakeExerciseRepository()

        val result = useCase(contentRepository, exerciseRepository, checkpointRepository)(today)

        assertEquals(NextStudyStep.NothingPending, result)
    }
}
