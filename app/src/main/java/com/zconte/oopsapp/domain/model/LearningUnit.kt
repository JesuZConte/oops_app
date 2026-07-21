package com.zconte.oopsapp.domain.model

data class LearningUnit(
    val id: String,
    val sectionId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int
)
