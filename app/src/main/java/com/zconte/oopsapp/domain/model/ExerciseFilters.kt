package com.zconte.oopsapp.domain.model

/**
 * Exercises that expect an answer. Excludes non-tracked didactic cards
 * (worked_example intros), which are seeded into the exercises table but must
 * never be counted toward unit completion or sampled as assessment questions.
 * Note: GetTodaySessionUseCase does NOT use this — Phase A must surface intros.
 */
fun List<Exercise>.answerableOnly(): List<Exercise> =
    filter { it.type != ExerciseType.WORKED_EXAMPLE }
