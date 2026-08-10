package com.zconte.oopsapp.data.content

import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards against a real defect found on-device 2026-08-07: a fill_blank
 * solo/practice exercise asked the player to recall a method name
 * (StringBuilder.reverse()) that its own concept's intro/guided steps
 * never showed -- first exposure to an unfamiliar identifier can't be a
 * graded fill-in-the-blank. Scoped to fill_blank because its answer is a
 * single recallable identifier; mcq/predict_output answers are full
 * sentences or derived output, not a token this check can meaningfully
 * match against taught text.
 */
class ContentCorpusLadderConsistencyTest {

    @Test
    fun `every fill_blank solo or practice answer was already shown by its own concept's intro or guided step`() {
        val json = Json { ignoreUnknownKeys = true }
        val violations = mutableListOf<String>()

        for (assetPath in ContentPackRegistry.assetPaths) {
            val file = File("src/main/assets/$assetPath")
            val pack = json.decodeFromString(ContentPack.serializer(), file.readText())

            for (unit in pack.units) {
                val byConcept = unit.exercises.filter { it.conceptId != null }.groupBy { it.conceptId }

                for ((conceptId, exercises) in byConcept) {
                    val taughtText = exercises
                        .filter { it.role == "intro" || it.role == "guided" }
                        .joinToString(" ") { "${it.prompt} ${it.code.orEmpty()} ${it.explanation}" }
                        .lowercase()

                    exercises
                        .filter { it.type == "fill_blank" && (it.role == "solo" || it.role == "practice") }
                        .forEach { exercise ->
                            val token = exercise.answer.trim().removeSuffix("()").lowercase()
                            // Operators/symbols (e.g. "<", "!=") are general syntax, not an
                            // API surface that needs to be taught first -- only check
                            // word-like answers (identifiers, keywords).
                            val isWordLike = token.length > 1 && token.any { it.isLetter() }
                            if (isWordLike && token !in taughtText) {
                                violations += "${pack.sectionId}/${unit.unitId}/${exercise.id}: " +
                                    "answer '${exercise.answer}' (concept '$conceptId') never appears " +
                                    "in that concept's own intro/guided text"
                            }
                        }
                }
            }
        }

        assertTrue(
            "Fill_blank solo/practice exercises testing an identifier their own concept never taught:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
