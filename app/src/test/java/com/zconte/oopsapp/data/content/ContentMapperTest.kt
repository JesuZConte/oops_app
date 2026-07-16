package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps a content pack into a topic entity and exercise entities`() {
        val pack = ContentPack(
            topicId = "java-streams",
            name = "Streams y lambdas",
            certObjective = "streams-lambdas",
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

        val (topic, exercises) = pack.toEntities(json)

        assertEquals("java-streams", topic.id)
        assertEquals("streams-lambdas", topic.certObjective)
        assertEquals(1, exercises.size)
        assertEquals("streams-01", exercises.first().id)
        assertEquals("java-streams", exercises.first().topicId)
        assertEquals("fill_blank", exercises.first().type)
        assertEquals(2, exercises.first().difficulty)

        val decoded = json.decodeFromString(ExerciseContent.serializer(), exercises.first().payload)
        assertEquals("collect", decoded.answer)
    }
}