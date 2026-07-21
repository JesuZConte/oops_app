package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps a content pack into a section entity, unit entities, and exercise entities`() {
        val pack = ContentPack(
            sectionId = "java-streams",
            name = "Streams y lambdas",
            orderIndex = 2,
            examVersion = "java21",
            units = listOf(
                UnitPack(
                    unitId = "streams-terminal",
                    name = "Operaciones terminales",
                    certObjective = "streams-lambdas",
                    orderIndex = 1,
                    exercises = listOf(
                        ExerciseContent(
                            id = "streams-01",
                            type = "fill_blank",
                            difficulty = 2,
                            prompt = "prompt",
                            code = "code",
                            answer = "collect",
                            distractors = listOf("map"),
                            explanation = "explanation"
                        )
                    )
                )
            )
        )

        val entities = pack.toEntities(json)

        assertEquals("java-streams", entities.section.id)
        assertEquals("java21", entities.section.examVersion)
        assertEquals(1, entities.units.size)
        assertEquals("streams-terminal", entities.units.first().id)
        assertEquals("java-streams", entities.units.first().sectionId)
        assertEquals("streams-lambdas", entities.units.first().certObjective)
        assertEquals(1, entities.exercises.size)
        assertEquals("streams-01", entities.exercises.first().id)
        assertEquals("streams-terminal", entities.exercises.first().unitId)
        assertEquals("java21", entities.exercises.first().examVersion)
        assertEquals("fill_blank", entities.exercises.first().type)
        assertEquals(2, entities.exercises.first().difficulty)

        val decoded = json.decodeFromString(ExerciseContent.serializer(), entities.exercises.first().payload)
        assertEquals("collect", decoded.answer)
    }
}