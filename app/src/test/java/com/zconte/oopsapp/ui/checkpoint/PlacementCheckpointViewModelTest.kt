package com.zconte.oopsapp.ui.checkpoint

import androidx.lifecycle.SavedStateHandle
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCase
import com.zconte.oopsapp.domain.usecase.GetLearningPathUseCase
import com.zconte.oopsapp.domain.usecase.GetPlacementCheckpointSessionUseCase
import com.zconte.oopsapp.domain.usecase.GetSkippedUnitsUseCase
import com.zconte.oopsapp.domain.usecase.IsCheckpointRetryUnlockedUseCase
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

class PlacementCheckpointViewModelTest {

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
        targetUnitId: String,
        contentRepository: ContentRepository,
        exerciseRepository: ExerciseRepository,
        progressRepository: ProgressRepository,
        checkpointRepository: CheckpointRepository
    ): PlacementCheckpointViewModel = PlacementCheckpointViewModel(
        savedStateHandle = SavedStateHandle(mapOf("targetUnitId" to targetUnitId)),
        getSkippedUnitsUseCase = GetSkippedUnitsUseCase(
            GetLearningPathUseCase(contentRepository, FakeCheckpointRepository(), IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), exerciseRepository))
        ),
        getPlacementCheckpointSessionUseCase = GetPlacementCheckpointSessionUseCase(exerciseRepository),
        submitAnswerUseCase = SubmitAnswerUseCase(exerciseRepository),
        completeCheckpointUseCase = CompleteCheckpointUseCase(checkpointRepository, contentRepository, exerciseRepository),
        updateStreakUseCase = UpdateStreakUseCase(progressRepository),
        json = json
    )

    @Test
    fun `init loads the skipped units and a placement queue drawn from them`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)))
        )
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-skip-1", "s1-u1")))
        )
        val viewModel = buildViewModel(
            targetUnitId = "s1-u2",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingSkipped)
        assertEquals("s1-u2", state.targetUnit?.id)
        assertEquals(listOf("s1-u1"), state.skippedUnits.map { it.id })
        assertEquals(listOf("ex-skip-1"), state.queue.map { it.id })
    }

    @Test
    fun `startCheckpoint with an empty queue completes immediately with a failing result`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1)))
        )
        val viewModel = buildViewModel(
            targetUnitId = "s1-u1",
            contentRepository = contentRepository,
            exerciseRepository = FakeExerciseRepository(),
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )

        viewModel.startCheckpoint()

        val state = viewModel.uiState.value
        assertTrue(state.hasStarted)
        assertTrue(state.isComplete)
        assertEquals(0, state.result?.scorePct)
        assertFalse(state.result?.passed ?: true)
    }

    @Test
    fun `startCheckpoint with a queue starts on the first exercise`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)))
        )
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-skip-1", "s1-u1")))
        )
        val viewModel = buildViewModel(
            targetUnitId = "s1-u2",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )

        viewModel.startCheckpoint()

        val state = viewModel.uiState.value
        assertTrue(state.hasStarted)
        assertEquals(1, state.currentIndex)
        assertEquals("ex-skip-1", state.currentExercise?.id)
    }

    @Test
    fun `submitAnswer buffers the answer without writing to SM-2 immediately`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)))
        )
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-skip-1", "s1-u1", answer = "42")))
        )
        val viewModel = buildViewModel(
            targetUnitId = "s1-u2",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )
        viewModel.startCheckpoint()

        viewModel.submitAnswer("42")

        assertTrue(viewModel.uiState.value.isAnswered)
        assertTrue(viewModel.uiState.value.isCorrect)
        assertTrue(
            "submitAnswerUseCase must not run until the checkpoint result is known",
            exerciseRepository.savedReviewStates.isEmpty()
        )
    }

    @Test
    fun `nextExercise advances the queue when exercises remain`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)))
        )
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(exercise("ex-skip-1", "s1-u1", answer = "42"), exercise("ex-skip-2", "s1-u1", answer = "42"))
            )
        )
        val viewModel = buildViewModel(
            targetUnitId = "s1-u2",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )
        viewModel.startCheckpoint()
        val firstId = viewModel.uiState.value.currentExercise?.id

        // Both fixture exercises share answer "42", so this grades correctly regardless of
        // which one the (internally shuffled) placement session put first.
        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        assertEquals(2, viewModel.uiState.value.currentIndex)
        assertFalse(viewModel.uiState.value.isAnswered)
        assertTrue(viewModel.uiState.value.currentExercise?.id != firstId)
    }

    @Test
    fun `passing the checkpoint submits every buffered answer to SM-2 and unlocks the skipped units`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)))
        )
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("ex-skip-1", "s1-u1", answer = "42")))
        )
        val checkpointRepository = FakeCheckpointRepository()
        val viewModel = buildViewModel(
            targetUnitId = "s1-u2",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = checkpointRepository
        )
        viewModel.startCheckpoint()

        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertTrue(state.result?.passed ?: false)
        assertEquals(listOf("ex-skip-1"), exerciseRepository.savedReviewStates.map { it.exerciseId })
        assertTrue(contentRepository.completedUnits.any { it.unitId == "s1-u1" && it.completedVia == UnitCompletionSource.PLACEMENT })
        assertEquals(1, checkpointRepository.recordedAttempts.size)
    }

    @Test
    fun `failing the checkpoint never writes any buffered answer to SM-2`() = runTest {
        val contentRepository = FakeContentRepository(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)))
        )
        val exerciseRepository = FakeExerciseRepository(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(exercise("ex-skip-1", "s1-u1", answer = "42"), exercise("ex-skip-2", "s1-u1", answer = "42"))
            )
        )
        val viewModel = buildViewModel(
            targetUnitId = "s1-u2",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )
        viewModel.startCheckpoint()

        // One wrong, one right: 50% < the 68% pass threshold, regardless of which exercise
        // the (internally shuffled) placement session put first -- both share answer "42".
        viewModel.submitAnswer("wrong")
        viewModel.nextExercise()
        viewModel.submitAnswer("42")
        viewModel.nextExercise()

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertFalse(state.result?.passed ?: true)
        assertTrue(
            "a failed placement checkpoint must never leak locked exercises into SM-2",
            exerciseRepository.savedReviewStates.isEmpty()
        )
        assertFalse(contentRepository.completedUnits.any { it.unitId == "s1-u1" })
    }
}
