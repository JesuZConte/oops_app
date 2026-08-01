package com.zconte.oopsapp.domain.model

data class Exercise(
    val id: String,
    val unitId: String,
    val type: String,
    val payload: String,
    val difficulty: Int,
    val examVersion: String = "core",
    val conceptId: String? = null,
    val role: String? = null,
    val pathOrder: Int? = null,
    val dependsOn: List<String> = emptyList()
)
