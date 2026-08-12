package com.zconte.oopsapp.ui.session

import androidx.lifecycle.SavedStateHandle
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseRole
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.usecase.GetCurrentUnitUseCase
import com.zconte.oopsapp.domain.usecase.GetLearningPathUseCase
import com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCase
import com.zconte.oopsapp.domain.usecase.GetUnitReviewSessionUseCase
import com.zconte.oopsapp.domain.usecase.GetUnitSessionUseCase
import com.zconte.oopsapp.domain.usecase.IsCheckpointRetryUnlockedUseCase
import com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
import com.zconte.oopsapp.testutil.FakeContentRepository
import com.zconte.oopsapp.testutil.FakeExerciseRepository
import com.zconte.oopsapp.testutil.FakeProgressRepository
import com.zconte.oopsapp.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private fun payload(id: String, answer: String = "42") =
        """{"id":"$id","type":"fill_blank","difficulty":1,"prompt":"prompt","answer":"$answer","explanation":"explanation"}"""

    private fun exercise(id: String, unitId: String, answer: String = "42") =
        Exercise(id = id, unitId = unitId, type = "fill_blank", payload = payload(id, answer), difficulty = 1)

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)

    private fun buildViewModel(
        unitId: String?,
        contentRepository: ContentRepository,
        exerciseRepository: ExerciseRepository,
        progressRepository: ProgressRepository,
        isReview: Boolean = false
    ): SessionViewModel = SessionViewModel(
        savedStateHandle = SavedStateHandle(
            buildMap {
                unitId?.let { put("unitId", it) }
                if (isReview) put("isReview", true)
            }
        ),
        getTodaySessionUseCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(
                GetLearningPathUseCase(contentRepository, FakeCheckpointRepository(), IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), exerciseRepository))
            )
        ),
        getUnitSessionUseCase = GetUnitSessionUseCase(exerciseRepository),
        getUnitReviewSessionUseCase = GetUnitReviewSessionUseCase(exerciseRepository),
        submitAnswerUseCase = SubmitAnswerUseCase(exerciseRepository),
        updateStreakUseCase = UpdateStreakUseCase(progressRepository),
        markUnitProgressUseCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository),
        json = json
    )

    @Test
    fun `init without a unitId loads todays session from the current Ruta unit`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1)))
        )
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1")))
        )
        val viewModel = buildViewModel(
            unitId = null,
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        assertEquals(listOf("ex-1"), viewModel.uiState.value.queue.map { it.id })
        assertEquals(1, viewModel.uiState.value.totalExercises)
        assertEquals("ex-1", viewModel.uiState.value.currentExercise?.id)
    }

    @Test
    fun `init with a unitId loads that units session directly, bypassing the current-unit gate`() = runTest {
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1"), exercise("ex-2", "s1-u1")))
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        assertEquals(listOf("ex-1", "ex-2"), viewModel.uiState.value.queue.map { it.id })
    }

    @Test
    fun `init with an empty queue marks the session complete immediately`() = runTest {
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = FakeExerciseRepository(),
            progressRepository = FakeProgressRepository()
        )

        assertTrue(viewModel.uiState.value.isSessionComplete)
        assertNull(viewModel.uiState.value.currentExercise)
    }

    @Test
    fun `submitAnswer with a correct answer grades correctly, submits SM-2 and marks unit progress`() = runTest {
        val contentRepository = FakeContentRepository()
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        viewModel.submitAnswer("42")

        assertTrue(viewModel.uiState.value.isAnswered)
        assertTrue(viewModel.uiState.value.isCorrect)
        val savedState = exerciseRepository.savedReviewStates.find { it.exerciseId == "ex-1" }
        // quality=5 on a fresh ReviewState bumps repetitions to 1 (SchedulerSm2) -- the real,
        // observable proof that submitAnswerUseCase ran with the correct-answer quality.
        assertEquals(1, savedState?.repetitions)
        assertTrue(contentRepository.completedUnits.any { it.unitId == "s1-u1" })
    }

    @Test
    fun `submitAnswer with an incorrect answer grades as incorrect and submits a low SM-2 quality`() = runTest {
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        viewModel.submitAnswer("wrong")

        assertTrue(viewModel.uiState.value.isAnswered)
        assertFalse(viewModel.uiState.value.isCorrect)
        val savedState = exerciseRepository.savedReviewStates.find { it.exerciseId == "ex-1" }
        // quality=2 (< 3) always resets repetitions to 0 (SchedulerSm2).
        assertEquals(0, savedState?.repetitions)
    }

    @Test
    fun `submitAnswer is a no-op once the current exercise is already answered`() = runTest {
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        viewModel.submitAnswer("42")
        viewModel.submitAnswer("wrong")

        assertTrue(viewModel.uiState.value.isCorrect)
        assertEquals("42", viewModel.uiState.value.selectedAnswer)
    }

    @Test
    fun `nextExercise advances the queue and resets answer state when exercises remain`() = runTest {
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42"), exercise("ex-2", "s1-u1", answer = "42"))
            )
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        assertEquals("ex-2", viewModel.uiState.value.currentExercise?.id)
        assertFalse(viewModel.uiState.value.isAnswered)
    }

    @Test
    fun `nextExercise on the last exercise updates the streak and completes the session`() = runTest {
        val progressRepository = FakeProgressRepository()
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = progressRepository
        )

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        assertTrue(viewModel.uiState.value.isSessionComplete)
        assertEquals(1, progressRepository.stats.streak)
    }

    @Test
    fun `nextExercise on the last exercise of a unit tap extends the queue as a dependsOn chain unblocks each following concept`() = runTest {
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(
                    Exercise(
                        id = "concept-a-solo",
                        unitId = "s1-u1",
                        type = "fill_blank",
                        payload = payload("concept-a-solo", "42"),
                        difficulty = 1,
                        conceptId = "concept-a",
                        role = ExerciseRole.SOLO,
                        pathOrder = 1
                    ),
                    // concept-b has an intro card ahead of its solo -- mirrors real content,
                    // where a newly-unlocked concept's batch starts with a worked_example
                    // (CONTINUAR advances the queue directly, without submitAnswer()).
                    Exercise(
                        id = "concept-b-intro",
                        unitId = "s1-u1",
                        type = "worked_example",
                        payload = payload("concept-b-intro", "n/a"),
                        difficulty = 1,
                        conceptId = "concept-b",
                        role = ExerciseRole.INTRO,
                        pathOrder = 2,
                        dependsOn = listOf("concept-a")
                    ),
                    Exercise(
                        id = "concept-b-solo",
                        unitId = "s1-u1",
                        type = "fill_blank",
                        payload = payload("concept-b-solo", "42"),
                        difficulty = 1,
                        conceptId = "concept-b",
                        role = ExerciseRole.SOLO,
                        pathOrder = 3,
                        dependsOn = listOf("concept-a")
                    ),
                    Exercise(
                        id = "concept-c-solo",
                        unitId = "s1-u1",
                        type = "fill_blank",
                        payload = payload("concept-c-solo", "42"),
                        difficulty = 1,
                        conceptId = "concept-c",
                        role = ExerciseRole.SOLO,
                        pathOrder = 4,
                        dependsOn = listOf("concept-b")
                    )
                )
            )
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        // Only concept-a's exercise is a candidate up front: concept-b and concept-c are
        // dependsOn-gated.
        assertEquals(listOf("concept-a-solo"), viewModel.uiState.value.queue.map { it.id })
        assertEquals(1, viewModel.uiState.value.totalExercises)

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        // Answering concept-a's solo exercise borns concept-a, which unblocks concept-b's
        // whole batch (intro + solo) in one extension -- the session must keep going instead
        // of ending here.
        assertFalse(viewModel.uiState.value.isSessionComplete)
        assertEquals("concept-b-intro", viewModel.uiState.value.currentExercise?.id)
        assertEquals(3, viewModel.uiState.value.totalExercises)

        // CONTINUAR on the intro: advances within the still-live batch, no submitAnswer(),
        // no re-query -- concept-b-solo is already queued.
        viewModel.nextExercise()
        assertEquals("concept-b-solo", viewModel.uiState.value.currentExercise?.id)
        assertFalse(viewModel.uiState.value.isSessionComplete)

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        // Answering concept-b's solo borns concept-b, unblocking concept-c -- second
        // extension hop, proving the loop isn't limited to a single re-query.
        assertFalse(viewModel.uiState.value.isSessionComplete)
        assertEquals("concept-c-solo", viewModel.uiState.value.currentExercise?.id)
        assertEquals(4, viewModel.uiState.value.totalExercises)

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        assertTrue(viewModel.uiState.value.isSessionComplete)
    }

    @Test
    fun `nextExercise on the last exercise of a unit tap completes the session when no new concept unblocks`() = runTest {
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        assertTrue(viewModel.uiState.value.isSessionComplete)
    }

    @Test
    fun `nextExercise extension does not apply to the daily session (no unitId)`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1)))
        )
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            unitId = null,
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository()
        )

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        assertTrue(viewModel.uiState.value.isSessionComplete)
    }

    @Test
    fun `a review session loads every exercise in the unit, regardless of prior progress`() = runTest {
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1"), exercise("ex-2", "s1-u1")))
        )
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-1",
                easeFactor = 2.5,
                intervalDays = 1,
                repetitions = 1,
                dueDate = LocalDate.now()
            )
        )

        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            isReview = true
        )

        assertEquals(listOf("ex-1", "ex-2"), viewModel.uiState.value.queue.map { it.id })
    }

    @Test
    fun `submitAnswer in a review session grades locally but never persists review_state or unit progress`() = runTest {
        val contentRepository = FakeContentRepository()
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            isReview = true
        )

        viewModel.submitAnswer("42")

        assertTrue(viewModel.uiState.value.isAnswered)
        assertTrue(viewModel.uiState.value.isCorrect)
        assertTrue(exerciseRepository.savedReviewStates.isEmpty())
        assertTrue(contentRepository.completedUnits.isEmpty())
    }

    @Test
    fun `completing a review session does not update the streak`() = runTest {
        val progressRepository = FakeProgressRepository()
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            unitId = "s1-u1",
            contentRepository = FakeContentRepository(),
            exerciseRepository = exerciseRepository,
            progressRepository = progressRepository,
            isReview = true
        )

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        assertTrue(viewModel.uiState.value.isSessionComplete)
        assertEquals(0, progressRepository.stats.streak)
    }
}
