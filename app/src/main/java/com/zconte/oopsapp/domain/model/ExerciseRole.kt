package com.zconte.oopsapp.domain.model

/** Role of an exercise within a concept's first-exposure ladder. */
object ExerciseRole {
    const val INTRO = "intro"       // non-tracked worked_example card
    const val GUIDED = "guided"
    const val SOLO = "solo"
    const val PRACTICE = "practice"
}