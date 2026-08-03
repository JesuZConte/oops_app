package com.zconte.oopsapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseContent(
    val id: String,
    val type: String,
    val difficulty: Int,
    val prompt: String,
    val code: String? = null,
    val answer: String,
    val distractors: List<String> = emptyList(),
    val lines: List<String> = emptyList(),
    val explanation: String,
    // Ladder metadata (payload-only; all optional so legacy content still parses):
    val conceptId: String? = null,
    val role: String? = null,
    val pathOrder: Int? = null,
    val dependsOn: List<String> = emptyList()
)