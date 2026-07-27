package com.zconte.oopsapp.ui.checkpoint

import androidx.lifecycle.SavedStateHandle
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCase
import com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class CheckpointViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private fun payload(id: String, answer: String = "42") =
        """{"id":"$id","type":"fill_blank","difficulty":1,"prompt":"prompt","answer":"$answer","explanation":"explanation"}"""

    private fun exercise(id: String, unitId: String, answer: String = "42") =
        Exercise(id = id, unitId = unitId, type = "fill_blank", payload = payload(id, answer), difficulty = 1)

    private fun buildViewModel(
        sectionId: String,
        contentRepository: ContentRepository,
        exerciseRepository: ExerciseRepository,
        progressRepository: ProgressRepository,
        checkpointRepository: CheckpointRepository
    ): CheckpointViewModel = CheckpointViewModel(
        savedStateHandle = SavedStateHandle(mapOf("sectionId" to sectionId)),
        getCheckpointSessionUseCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository),
        isCheckpointRetryUnlockedUseCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository),
        submitAnswerUseCase = SubmitAnswerUseCase(exerciseRepository),
        completeCheckpointUseCase = CompleteCheckpointUseCase(checkpointRepository, contentRepository, exerciseRepository),
        updateStreakUseCase = UpdateStreakUseCase(progressRepository),
        markUnitProgressUseCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository),
        json = json
    )

    @Test
    fun `init loads the checkpoint session and shows the intro before any question`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf("s1" to listOf(exercise("ex-1", "s1-u1")))
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )

        val state = viewModel.uiState.value
        assertTrue(state.showIntro)
        assertFalse(state.isRetryLocked)
        assertEquals(1, state.totalExercises)
        assertEquals(null, state.currentExercise)
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun `startCheckpoint reveals the first question and leaves the intro`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf("s1" to listOf(exercise("ex-1", "s1-u1")))
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )

        viewModel.startCheckpoint()

        val state = viewModel.uiState.value
        assertFalse(state.showIntro)
        assertEquals(1, state.currentIndex)
        assertEquals("ex-1", state.currentExercise?.id)
    }

    @Test
    fun `init for an unknown section completes immediately with a failing result`() = runTest {
        val viewModel = buildViewModel(
            sectionId = "unknown-section",
            contentRepository = FakeContentRepository(),
            exerciseRepository = FakeExerciseRepository(),
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertEquals(0, state.result?.scorePct)
        assertFalse(state.result?.passed ?: true)
    }

    @Test
    fun `init when the retry gate is locked shows isRetryLocked without loading a session`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf("s1" to listOf(exercise("ex-1", "s1-u1")))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false,
            takenAt = LocalDate.of(2026, 7, 20), failedExerciseIds = listOf("ex-1")
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = checkpointRepository
        )

        val state = viewModel.uiState.value
        assertTrue(state.isRetryLocked)
        assertFalse(state.showIntro)
        assertTrue(state.queue.isEmpty())
    }

    @Test
    fun `init when the retry gate is unlocked after re-study loads a fresh session`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf("s1" to listOf(exercise("ex-1", "s1-u1")))
        )
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1, repetitions = 1,
                dueDate = LocalDate.of(2026, 7, 22), lastReviewedAt = LocalDate.of(2026, 7, 21)
            )
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false,
            takenAt = LocalDate.of(2026, 7, 20), failedExerciseIds = listOf("ex-1")
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = checkpointRepository
        )

        val state = viewModel.uiState.value
        assertFalse(state.isRetryLocked)
        assertTrue(state.showIntro)
        assertEquals(1, state.totalExercises)
    }

    @Test
    fun `submitAnswer with a correct answer submits SM-2 and marks unit progress`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val sharedExercise = exercise("ex-1", "s1-u1", answer = "42")
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf("s1" to listOf(sharedExercise)),
            exercisesByUnit = mapOf("s1-u1" to listOf(sharedExercise))
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )
        viewModel.startCheckpoint()

        viewModel.submitAnswer("42")

        assertTrue(viewModel.uiState.value.isCorrect)
        val savedState = exerciseRepository.savedReviewStates.find { it.exerciseId == "ex-1" }
        assertEquals(1, savedState?.repetitions)
        assertTrue(contentRepository.completedUnits.any { it.unitId == "s1-u1" })
    }

    @Test
    fun `submitAnswer with an incorrect answer submits a low SM-2 quality`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf("s1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )
        viewModel.startCheckpoint()

        viewModel.submitAnswer("wrong")

        assertFalse(viewModel.uiState.value.isCorrect)
        val savedState = exerciseRepository.savedReviewStates.find { it.exerciseId == "ex-1" }
        assertEquals(0, savedState?.repetitions)
    }

    @Test
    fun `nextExercise advances the queue and increments currentIndex when exercises remain`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf(
                "s1" to listOf(exercise("ex-1", "s1-u1", answer = "42"), exercise("ex-2", "s1-u1", answer = "42"))
            )
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )
        viewModel.startCheckpoint()
        val firstId = viewModel.uiState.value.currentExercise?.id

        // Both fixture exercises share answer "42", so this grades correctly regardless of
        // which one the (internally shuffled) checkpoint session put first.
        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        assertEquals(2, viewModel.uiState.value.currentIndex)
        assertFalse(viewModel.uiState.value.isAnswered)
        assertTrue(viewModel.uiState.value.currentExercise?.id != firstId)
    }

    @Test
    fun `nextExercise on the last exercise updates the streak and completes the checkpoint with the right score`() = runTest {
        val progressRepository = FakeProgressRepository()
        val checkpointRepository = FakeCheckpointRepository()
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf(
                "s1" to listOf(exercise("ex-1", "s1-u1", answer = "42"), exercise("ex-2", "s1-u1", answer = "42"))
            )
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = progressRepository,
            checkpointRepository = checkpointRepository
        )
        viewModel.startCheckpoint()

        // One wrong, one right: 50% score, regardless of which exercise the fake session put first.
        viewModel.submitAnswer("wrong")
        viewModel.nextExercise()
        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertEquals(50, state.result?.scorePct)
        assertFalse(state.result?.passed ?: true)
        assertEquals(1, progressRepository.stats.streak)
        assertEquals(1, checkpointRepository.recordedAttempts.size)
    }

    @Test
    fun `a wrong answer is recorded as a failed exercise id on the completed attempt`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf("s1" to listOf(exercise("ex-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = checkpointRepository
        )
        viewModel.startCheckpoint()

        viewModel.submitAnswer("wrong")
        viewModel.nextExercise()

        assertEquals(listOf("ex-1"), checkpointRepository.recordedAttempts.first().failedExerciseIds)
    }
}
