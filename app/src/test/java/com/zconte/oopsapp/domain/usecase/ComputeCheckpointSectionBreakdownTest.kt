package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeCheckpointSectionBreakdownTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String) = LearningUnit(id, sectionId, id, "objective", 1)
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "mcq", "{}", 1)

    @Test
    fun `groups correctness by section and sorts by section order`() {
        val unitsById = mapOf(
            "s1-u1" to unit("s1-u1", "s1"),
            "s2-u1" to unit("s2-u1", "s2")
        )
        val sectionsById = mapOf(
            "s1" to section("s1", 1),
            "s2" to section("s2", 2)
        )
        val results = listOf(
            exercise("s1-ex-1", "s1-u1") to true,
            exercise("s1-ex-2", "s1-u1") to false,
            exercise("s2-ex-1", "s2-u1") to true,
            exercise("s2-ex-2", "s2-u1") to true,
            exercise("s2-ex-3", "s2-u1") to false
        )

        val breakdown = computeCheckpointSectionBreakdown(results, unitsById, sectionsById)

        assertEquals(2, breakdown.size)
        assertEquals("s1", breakdown[0].section.id)
        assertEquals(1, breakdown[0].correct)
        assertEquals(2, breakdown[0].total)
        assertEquals("s2", breakdown[1].section.id)
        assertEquals(2, breakdown[1].correct)
        assertEquals(3, breakdown[1].total)
    }

    @Test
    fun `an exercise whose unit or section cannot be resolved is silently excluded`() {
        val results = listOf(Exercise("orphan-ex", "unknown-unit", "mcq", "{}", 1) to true)

        val breakdown = computeCheckpointSectionBreakdown(results, unitsById = emptyMap(), sectionsById = emptyMap())

        assertEquals(0, breakdown.size)
    }
}
