package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.model.CompletedUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForBreakdown(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = emptyList()
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
    override suspend fun getUnitSummary(unitId: String): UnitSummary? = null
}

class GetCheckpointResultBreakdownUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "fill_blank", "{}", 1)

    @Test
    fun `resolves each exercise's section and tallies correctness per section`() = runTest {
        val contentRepository = FakeContentRepositoryForBreakdown(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            )
        )
        val useCase = GetCheckpointResultBreakdownUseCase(contentRepository)
        val results = listOf(
            exercise("ex-1", "s1-u1") to true,
            exercise("ex-2", "s1-u1") to false,
            exercise("ex-3", "s2-u1") to true
        )

        val breakdown = useCase(results)

        assertEquals(2, breakdown.size)
        assertEquals("s1", breakdown[0].section.id)
        assertEquals(1, breakdown[0].correct)
        assertEquals(2, breakdown[0].total)
        assertEquals("s2", breakdown[1].section.id)
        assertEquals(1, breakdown[1].correct)
        assertEquals(1, breakdown[1].total)
    }
}
