package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.model.ExerciseType

fun gradeExerciseAnswer(exercise: ExerciseContent, userAnswer: String): Boolean =
    when (exercise.type) {
        ExerciseType.PARSONS -> gradeParsons(exercise, userAnswer)
        ExerciseType.PREDICT_OUTPUT -> gradePredictOutput(exercise, userAnswer)
        else -> userAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)
    }

private fun gradeParsons(exercise: ExerciseContent, userAnswer: String): Boolean =
    userAnswer.trim() == exercise.answer.trim()

private fun gradePredictOutput(exercise: ExerciseContent, userAnswer: String): Boolean =
    normalizeOutput(userAnswer) == normalizeOutput(exercise.answer)

private fun normalizeOutput(text: String): String =
    text.trim().lines().joinToString("\n") { it.trimEnd() }
