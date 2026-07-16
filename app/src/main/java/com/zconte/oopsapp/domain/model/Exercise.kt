package com.zconte.oopsapp.domain.model

data class Exercise(
    val id: String,
    val topicId: String,
    val type: String,
    val payload: String,
    val difficulty: Int
)