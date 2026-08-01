package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseEntityMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun entity(payload: String) = ExerciseEntity(
        id = "e1", unitId = "u1", type = "fill_blank",
        payload = payload, difficulty = 2, examVersion = "core"
    )

    @Test
    fun `maps ladder fields from the payload onto the domain exercise`() {
        val payload = """
            {"id":"e1","type":"fill_blank","difficulty":2,"prompt":"p",
             "answer":"a","explanation":"x",
             "conceptId":"collectors-groupingby","role":"solo","pathOrder":2,
             "dependsOn":["dep-a","dep-b"]}
        """.trimIndent()

        val domain = entity(payload).toDomain(json)

        assertEquals("collectors-groupingby", domain.conceptId)
        assertEquals("solo", domain.role)
        assertEquals(2, domain.pathOrder)
        assertEquals(listOf("dep-a", "dep-b"), domain.dependsOn)
    }

    @Test
    fun `legacy payload without ladder fields maps to null defaults`() {
        val payload = """
            {"id":"e1","type":"fill_blank","difficulty":2,"prompt":"p",
             "answer":"a","explanation":"x"}
        """.trimIndent()

        val domain = entity(payload).toDomain(json)

        assertEquals(null, domain.conceptId)
        assertEquals(null, domain.role)
        assertEquals(null, domain.pathOrder)
        assertEquals(emptyList<String>(), domain.dependsOn)
    }
}