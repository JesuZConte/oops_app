package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.model.ExerciseType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeExerciseAnswerUseCaseTest {

    private fun mcqExercise(answer: String) = ExerciseContent(
        id = "ex-mcq", type = ExerciseType.MCQ, difficulty = 1, prompt = "p",
        answer = answer, explanation = "e"
    )

    private fun fillBlankExercise(answer: String) = ExerciseContent(
        id = "ex-fill", type = ExerciseType.FILL_BLANK, difficulty = 1, prompt = "p",
        answer = answer, explanation = "e"
    )

    private fun parsonsExercise(answer: String) = ExerciseContent(
        id = "ex-parsons", type = ExerciseType.PARSONS, difficulty = 1, prompt = "p",
        answer = answer, explanation = "e"
    )

    private fun predictOutputExercise(answer: String) = ExerciseContent(
        id = "ex-predict", type = ExerciseType.PREDICT_OUTPUT, difficulty = 1, prompt = "p",
        answer = answer, explanation = "e"
    )

    @Test
    fun `mcq is case-insensitive and trims, unchanged from prior behavior`() {
        assertTrue(gradeExerciseAnswer(mcqExercise("Java Virtual Machine"), "  java virtual machine  "))
        assertFalse(gradeExerciseAnswer(mcqExercise("Java Virtual Machine"), "Java Verified Method"))
    }

    @Test
    fun `fill_blank is case-insensitive and trims, unchanged from prior behavior`() {
        assertTrue(gradeExerciseAnswer(fillBlankExercise("javac"), " Javac "))
        assertFalse(gradeExerciseAnswer(fillBlankExercise("javac"), "java"))
    }

    @Test
    fun `parsons requires exact order and is case-sensitive`() {
        val answer = "list.stream()\n.filter(x -> x > 0)\n.count()"
        assertTrue(gradeExerciseAnswer(parsonsExercise(answer), answer))
        assertFalse(gradeExerciseAnswer(parsonsExercise(answer), "list.stream()\n.count()\n.filter(x -> x > 0)"))
        assertFalse(gradeExerciseAnswer(parsonsExercise(answer), answer.replace("list", "List")))
    }

    @Test
    fun `predict_output is case-sensitive but tolerates trailing whitespace and blank edges`() {
        val exercise = predictOutputExercise("a\nb")
        assertTrue(gradeExerciseAnswer(exercise, "a\nb"))
        assertTrue(gradeExerciseAnswer(exercise, "a \nb  \n"))
        assertTrue(gradeExerciseAnswer(exercise, "\na\nb\n"))
        assertFalse(gradeExerciseAnswer(exercise, "A\nb"))
        assertFalse(gradeExerciseAnswer(exercise, "a\nb\nc"))
    }
}
