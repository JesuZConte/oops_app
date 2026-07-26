package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForCurrentUnit(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetCurrentUnitUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun played(unitId: String) = CompletedUnit(unitId, UnitCompletionSource.PLAYED)

    @Test
    fun `current unit is the first unlocked, incomplete unit`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = emptyList()
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository, FakeCheckpointRepository()))

        val current = useCase()

        assertEquals("s1-u1", current?.id)
    }

    @Test
    fun `current unit advances once the previous unit is completed`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository, FakeCheckpointRepository()))

        val current = useCase()

        assertEquals("s1-u2", current?.id)
    }

    @Test
    fun `current unit crosses into the next section once the previous one is fully complete`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 80, passed = true,
            takenAt = LocalDate.of(2026, 7, 20), failedExerciseIds = emptyList()
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository, checkpointRepository))

        val current = useCase()

        assertEquals("s2-u1", current?.id)
    }

    @Test
    fun `a unit completed via placement checkpoint is skipped when picking the current unit`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(CompletedUnit("s1-u1", UnitCompletionSource.PLACEMENT))
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository, FakeCheckpointRepository()))

        val current = useCase()

        assertEquals("s1-u2", current?.id)
    }

    @Test
    fun `no current unit once every section is fully complete`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository, FakeCheckpointRepository()))

        val current = useCase()

        assertNull(current)
    }
}
