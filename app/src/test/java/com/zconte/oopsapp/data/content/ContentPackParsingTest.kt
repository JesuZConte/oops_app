package com.zconte.oopsapp.data.content

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentPackParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a content pack with one unit and one exercise`() {
        val raw = """
            {
              "sectionId": "java-streams",
              "name": "Streams y lambdas",
              "orderIndex": 2,
              "examVersion": "java21",
              "units": [
                {
                  "unitId": "streams-terminal",
                  "name": "Operaciones terminales",
                  "certObjective": "streams-lambdas",
                  "orderIndex": 1,
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
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals("java-streams", pack.sectionId)
        assertEquals("java21", pack.examVersion)
        assertEquals(1, pack.units.size)
        assertEquals("streams-lambdas", pack.units.first().certObjective)
        assertEquals(1, pack.units.first().exercises.size)
        assertEquals("collect", pack.units.first().exercises.first().answer)
        assertEquals(listOf("map", "reduce", "forEach"), pack.units.first().exercises.first().distractors)
    }

    @Test
    fun `exercise without code field parses with null code`() {
        val raw = """
            {
              "sectionId": "java-streams",
              "name": "Streams y lambdas",
              "orderIndex": 2,
              "examVersion": "java21",
              "units": [
                {
                  "unitId": "streams-creation",
                  "name": "Creacion de streams",
                  "certObjective": "streams-lambdas",
                  "orderIndex": 0,
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
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals(null, pack.units.first().exercises.first().code)
    }
}