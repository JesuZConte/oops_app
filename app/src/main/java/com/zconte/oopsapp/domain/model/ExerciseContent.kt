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
    val explanation: String
)