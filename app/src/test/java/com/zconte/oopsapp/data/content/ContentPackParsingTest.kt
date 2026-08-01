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

    @Test
    fun `parsons exercise parses lines field`() {
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
                      "id": "streams-parsons-01",
                      "type": "parsons",
                      "difficulty": 2,
                      "prompt": "Ordena las lineas:",
                      "lines": ["numeros.stream()", ".filter(n -> n % 2 == 0)", ".count()"],
                      "answer": "numeros.stream()\n.filter(n -> n % 2 == 0)\n.count()",
                      "code": "numeros.stream()\n.filter(n -> n % 2 == 0)\n.count()",
                      "explanation": "Cuenta los pares del stream."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals(
            listOf("numeros.stream()", ".filter(n -> n % 2 == 0)", ".count()"),
            pack.units.first().exercises.first().lines
        )
    }

    @Test
    fun `exercise without lines field defaults to empty list`() {
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

        assertEquals(emptyList<String>(), pack.units.first().exercises.first().lines)
    }

    @Test
    fun `unit summary parses text and optional code`() {
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
                  "summary": {
                    "text": "Un Stream se crea a partir de una fuente de datos.",
                    "code": "lista.stream()"
                  },
                  "exercises": []
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals("Un Stream se crea a partir de una fuente de datos.", pack.units.first().summary?.text)
        assertEquals("lista.stream()", pack.units.first().summary?.code)
    }

    @Test
    fun `unit without a summary field parses with a null summary`() {
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
                  "exercises": []
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)

        assertEquals(null, pack.units.first().summary)
    }

    @Test
    fun `exercise parses ladder fields conceptId role pathOrder dependsOn`() {
        val raw = """
            {
              "sectionId": "java-streams",
              "name": "Streams y lambdas",
              "orderIndex": 2,
              "examVersion": "java21",
              "units": [
                {
                  "unitId": "streams-collectors",
                  "name": "Collectors avanzados",
                  "certObjective": "streams-lambdas",
                  "orderIndex": 4,
                  "exercises": [
                    {
                      "id": "gb-solo",
                      "type": "fill_blank",
                      "difficulty": 3,
                      "prompt": "Agrupa por longitud:",
                      "code": "stream.collect(Collectors._____(String::length))",
                      "answer": "groupingBy",
                      "explanation": "groupingBy agrupa en un Map.",
                      "conceptId": "collectors-groupingby",
                      "role": "solo",
                      "pathOrder": 2
                    },
                    {
                      "id": "combo-solo",
                      "type": "fill_blank",
                      "difficulty": 4,
                      "prompt": "Particiona y agrupa:",
                      "code": "...",
                      "answer": "x",
                      "explanation": "composicion.",
                      "conceptId": "collectors-partition-then-group",
                      "role": "solo",
                      "pathOrder": 8,
                      "dependsOn": ["collectors-groupingby", "collectors-partitioningby"]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)
        val exercises = pack.units.first().exercises

        assertEquals("collectors-groupingby", exercises[0].conceptId)
        assertEquals("solo", exercises[0].role)
        assertEquals(2, exercises[0].pathOrder)
        assertEquals(emptyList<String>(), exercises[0].dependsOn)
        assertEquals(
            listOf("collectors-groupingby", "collectors-partitioningby"),
            exercises[1].dependsOn
        )
    }

    @Test
    fun `legacy exercise without ladder fields parses with null defaults`() {
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
                      "id": "legacy-01",
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
        val ex = pack.units.first().exercises.first()

        assertEquals(null, ex.conceptId)
        assertEquals(null, ex.role)
        assertEquals(null, ex.pathOrder)
        assertEquals(emptyList<String>(), ex.dependsOn)
    }
}