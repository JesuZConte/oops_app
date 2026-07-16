package com.zconte.oopsapp.data.content

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentPackParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a content pack with one exercise`() {
        val raw = """
            {
              "topicId": "java-streams",
              "name": "Streams y lambdas",
              "certObjective": "streams-lambdas",
              "exercises": [
                {
                  "id": "streams-collect-01",
                  "type": "fill_blank",
                  "difficulty": 2,
                  "prompt": "Convierte un Stream<String> en List<String>:",
                  "code": "stream._____(Collectors.toList())",
                  "answer": "collect",
                  "distractors": ["map", "reduce", "forEach"],
                  "explanation": "collect() es una operacion terminal que acumula elementos."
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals("java-streams", pack.topicId)
        assertEquals("streams-lambdas", pack.certObjective)
        assertEquals(1, pack.exercises.size)
        assertEquals("collect", pack.exercises.first().answer)
        assertEquals(listOf("map", "reduce", "forEach"), pack.exercises.first().distractors)
    }

    @Test
    fun `exercise without code field parses with null code`() {
        val raw = """
            {
              "topicId": "java-streams",
              "name": "Streams y lambdas",
              "certObjective": "streams-lambdas",
              "exercises": [
                {
                  "id": "streams-mcq-01",
                  "type": "mcq",
                  "difficulty": 1,
                  "prompt": "Que metodo crea un Stream desde una List?",
                  "answer": "stream",
                  "distractors": ["toStream", "asStream", "of"],
                  "explanation": "List.stream() crea el Stream."
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals(null, pack.exercises.first().code)
    }
}