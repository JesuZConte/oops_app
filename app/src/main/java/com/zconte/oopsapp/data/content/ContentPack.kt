package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.Serializable

@Serializable
data class ContentPack(
    val topicId: String,
    val name: String,
    val certObjective: String,
    val exercises: List<ExerciseContent>
)