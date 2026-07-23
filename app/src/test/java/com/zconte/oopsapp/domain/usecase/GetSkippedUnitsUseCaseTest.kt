package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.ContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForSkipped(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetSkippedUnitsUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun played(unitId: String) = CompletedUnit(unitId, UnitCompletionSource.PLAYED)

    @Test
    fun `skipping a locked unit within the current section returns only that gap`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2), unit("s1-u3", "s1", 3))
            ),
            completedUnits = emptyList()
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("s1-u3")

        assertEquals(listOf("s1-u1", "s1-u2"), result.skippedUnits.map { it.id })
        assertEquals("s1-u3", result.targetUnit?.id)
    }

    @Test
    fun `skipping an entire locked section includes all of it plus the remainder of the current one`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)),
                "s2" to listOf(unit("s2-u1", "s2", 1), unit("s2-u2", "s2", 2))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("s2-u2")

        assertEquals(listOf("s1-u2", "s2-u1"), result.skippedUnits.map { it.id })
    }

    @Test
    fun `skipping across several fully locked sections includes every unit before the target`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1), section("s2", 2), section("s3", 3)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1)),
                "s3" to listOf(unit("s3-u1", "s3", 1))
            ),
            completedUnits = emptyList()
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("s3-u1")

        assertEquals(listOf("s1-u1", "s2-u1"), result.skippedUnits.map { it.id })
    }

    @Test
    fun `a target that is already unlocked has nothing to skip`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("s1-u2")

        assertTrue(result.skippedUnits.isEmpty())
    }

    @Test
    fun `an unknown target id resolves to no target and nothing to skip`() = runTest {
        val repository = FakeContentRepositoryForSkipped(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val useCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))

        val result = useCase("does-not-exist")

        assertNull(result.targetUnit)
        assertTrue(result.skippedUnits.isEmpty())
    }
}
