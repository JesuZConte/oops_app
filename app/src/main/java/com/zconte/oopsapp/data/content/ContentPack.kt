package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.Serializable

@Serializable
data class ContentPack(
    val sectionId: String,
    val name: String,
    val orderIndex: Int,
    val examVersion: String,
    val units: List<UnitPack>
)

@Serializable
data class UnitPack(
    val unitId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int,
    val exercises: List<ExerciseContent>
)