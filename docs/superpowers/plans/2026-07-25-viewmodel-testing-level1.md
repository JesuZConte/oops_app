# ViewModel Testing (Level 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the zero-coverage gap on `SessionViewModel`, `CheckpointViewModel`,
and `PlacementCheckpointViewModel` by adding JVM unit tests for each, per
Level 1 of the accepted testing-strategy ADR.

**Architecture:** No production code changes — this is a pure test-only
addition. Real use-case instances are constructed backed by hand-written
fakes of the 4 repositories they depend on (`ExerciseRepository`,
`ContentRepository`, `ProgressRepository`, `CheckpointRepository`), matching
the "no mocking library" convention already used throughout
`domain/usecase`. A small `MainDispatcherRule` (new, JVM-only, no new
dependency) makes `viewModelScope` runnable in plain JUnit4 tests.

**Tech Stack:** Kotlin, JUnit4, `kotlinx-coroutines-test` (already a
`testImplementation` dependency — no build file changes needed).

**Design doc:** `docs/superpowers/specs/2026-07-25-viewmodel-testing-level1-design.md`

## Global Constraints

- Zero changes to production code (`ui/session`, `ui/checkpoint`) — this
  plan only adds files under `app/src/test/`.
- No Compose/UI tests, no navigation tests — that is Level 2 of the ADR,
  a separate cycle.
- Do not re-test `SchedulerSm2` or `computeCheckpointResult`'s internal
  formulas — they already have their own coverage in `domain/usecase`.
  Tests here only verify the ViewModel invokes them (directly or via a
  use case) with the right inputs, observed through real resulting state
  (e.g. a saved `ReviewState`'s `repetitions`), not through spies.
- Fakes are constructed explicitly per test (no default-parameter "god
  builder") — every test passes exactly the fakes it configured, so it
  can assert on those same references afterward.
- The single most important behavior in this plan: **a failed placement
  checkpoint must never call `SubmitAnswerUseCase`** (Task 4, last two
  tests). This is the invariant the entire Fase 2.1b defer-on-fail design
  depends on.

---

### Task 1: Test infrastructure — `MainDispatcherRule` and shared repository fakes

**Files:**
- Create: `app/src/test/java/com/zconte/oopsapp/testutil/MainDispatcherRule.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/testutil/FakeExerciseRepository.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/testutil/FakeContentRepository.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/testutil/FakeProgressRepository.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/testutil/FakeCheckpointRepository.kt`

**Interfaces:**
- Consumes: `ExerciseRepository`, `ContentRepository`, `ProgressRepository`,
  `CheckpointRepository` (existing interfaces, unchanged).
- Produces: `MainDispatcherRule` (a JUnit4 `TestWatcher`, use via
  `@get:Rule val mainDispatcherRule = MainDispatcherRule()`),
  `FakeExerciseRepository`, `FakeContentRepository`,
  `FakeProgressRepository`, `FakeCheckpointRepository` — all consumed by
  Tasks 2, 3, and 4.

This task has no behavior of its own to TDD (it's test infrastructure,
not production logic) — verification is that the project compiles and
each fake correctly implements its interface.

- [ ] **Step 1: Create `MainDispatcherRule`**

Create `app/src/test/java/com/zconte/oopsapp/testutil/MainDispatcherRule.kt`:

```kotlin
package com.zconte.oopsapp.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Installs a coroutine dispatcher as [Dispatchers.Main] for the duration of a test, so
 * `viewModelScope` (which defaults to `Dispatchers.Main.immediate`) can run in plain JUnit4
 * tests. Uses [UnconfinedTestDispatcher] by default: launched coroutines run eagerly, so
 * assertions right after a ViewModel call see the coroutine's effects without an explicit
 * `advanceUntilIdle()`.
 */
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

- [ ] **Step 2: Create `FakeExerciseRepository`**

Create `app/src/test/java/com/zconte/oopsapp/testutil/FakeExerciseRepository.kt`:

```kotlin
package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate

class FakeExerciseRepository(
    private val dueExercises: List<Exercise> = emptyList(),
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val exercisesBySection: Map<String, List<Exercise>> = emptyMap()
) : ExerciseRepository {

    val savedReviewStates = mutableListOf<ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = dueExercises.take(limit)

    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()

    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = exercisesBySection[sectionId] ?: emptyList()

    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        savedReviewStates.find { it.exerciseId == exerciseId }

    override suspend fun saveReviewState(state: ReviewState) {
        savedReviewStates.removeAll { it.exerciseId == state.exerciseId }
        savedReviewStates.add(state)
    }

    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        exerciseIds.filter { id -> savedReviewStates.any { it.exerciseId == id } }
}
```

- [ ] **Step 3: Create `FakeContentRepository`**

Create `app/src/test/java/com/zconte/oopsapp/testutil/FakeContentRepository.kt`:

```kotlin
package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import java.time.LocalDate

class FakeContentRepository(
    private val sections: List<Section> = emptyList(),
    private val unitsBySection: Map<String, List<LearningUnit>> = emptyMap(),
    initialCompletedUnits: List<CompletedUnit> = emptyList()
) : ContentRepository {

    val completedUnits = initialCompletedUnits.toMutableList()

    override suspend fun getSections(): List<Section> = sections

    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()

    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits

    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {
        completedUnits.removeAll { it.unitId == unitId }
        completedUnits.add(CompletedUnit(unitId, via))
    }
}
```

- [ ] **Step 4: Create `FakeProgressRepository`**

Create `app/src/test/java/com/zconte/oopsapp/testutil/FakeProgressRepository.kt`:

```kotlin
package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository

class FakeProgressRepository(
    initialStats: UserStats = UserStats(streak = 0, xp = 0, lastStudyDate = null)
) : ProgressRepository {

    var stats: UserStats = initialStats
        private set

    override suspend fun getUserStats(): UserStats = stats

    override suspend fun saveUserStats(stats: UserStats) {
        this.stats = stats
    }
}
```

- [ ] **Step 5: Create `FakeCheckpointRepository`**

Create `app/src/test/java/com/zconte/oopsapp/testutil/FakeCheckpointRepository.kt`:

```kotlin
package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate

class FakeCheckpointRepository : CheckpointRepository {

    data class RecordedAttempt(
        val sectionId: String,
        val kind: String,
        val scorePct: Int,
        val passed: Boolean,
        val takenAt: LocalDate
    )

    val recordedAttempts = mutableListOf<RecordedAttempt>()

    override suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate) {
        recordedAttempts.add(RecordedAttempt(sectionId, kind, scorePct, passed, takenAt))
    }
}
```

- [ ] **Step 6: Verify the project compiles**

Run: `./gradlew :app:compileTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/test/java/com/zconte/oopsapp/testutil/MainDispatcherRule.kt \
        app/src/test/java/com/zconte/oopsapp/testutil/FakeExerciseRepository.kt \
        app/src/test/java/com/zconte/oopsapp/testutil/FakeContentRepository.kt \
        app/src/test/java/com/zconte/oopsapp/testutil/FakeProgressRepository.kt \
        app/src/test/java/com/zconte/oopsapp/testutil/FakeCheckpointRepository.kt
git commit -m "test: add MainDispatcherRule and shared repository fakes for ViewModel tests"
```

---

### Task 2: `SessionViewModelTest`

**Files:**
- Create: `app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt`

**Interfaces:**
- Consumes: `MainDispatcherRule`, `FakeExerciseRepository`,
  `FakeContentRepository`, `FakeProgressRepository` (Task 1, unchanged).
- Produces: nothing consumed by later tasks — this file is independent of
  Tasks 3 and 4.

- [ ] **Step 1: Write the test file**

Create `app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt`:

```kotlin
package com.zconte.oopsapp.ui.session

import androidx.lifecycle.SavedStateHandle
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.usecase.GetCurrentUnitUseCase
import com.zconte.oopsapp.domain.usecase.GetLearningPathUseCase
import com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCase
import com.zconte.oopsapp.domain.usecase.GetUnitSessionUseCase
import com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
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
        progressRepository: ProgressRepository
    ): SessionViewModel = SessionViewModel(
        savedStateHandle = SavedStateHandle(unitId?.let { mapOf("unitId" to it) } ?: emptyMap()),
        getTodaySessionUseCase = GetTodaySessionUseCase(exerciseRepository, GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository))),
        getUnitSessionUseCase = GetUnitSessionUseCase(exerciseRepository),
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
}
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.session.SessionViewModelTest"`
Expected: PASS (8 tests). If it fails, the failure is either a fixture
mismatch in this file or a real bug in `SessionViewModel` — check the
assertion against the actual source of `SessionViewModel.kt` before
changing anything, since this task must not change production code (see
Global Constraints).

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt
git commit -m "test: add SessionViewModel coverage (init, grading, SM-2, unit progress, completion)"
```

---

### Task 3: `CheckpointViewModelTest`

**Files:**
- Create: `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt`

**Interfaces:**
- Consumes: `MainDispatcherRule`, `FakeExerciseRepository`,
  `FakeContentRepository`, `FakeProgressRepository`,
  `FakeCheckpointRepository` (Task 1, unchanged).
- Produces: nothing consumed by later tasks — independent of Tasks 2 and 4.

- [ ] **Step 1: Write the test file**

Create `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt`:

```kotlin
package com.zconte.oopsapp.ui.checkpoint

import androidx.lifecycle.SavedStateHandle
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCase
import com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCase
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
        submitAnswerUseCase = SubmitAnswerUseCase(exerciseRepository),
        completeCheckpointUseCase = CompleteCheckpointUseCase(checkpointRepository, contentRepository, exerciseRepository),
        updateStreakUseCase = UpdateStreakUseCase(progressRepository),
        markUnitProgressUseCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository),
        json = json
    )

    @Test
    fun `init loads the checkpoint session for the given section`() = runTest {
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
        assertEquals(listOf("ex-1"), state.queue.map { it.id })
        assertEquals(1, state.totalExercises)
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
}
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.checkpoint.CheckpointViewModelTest"`
Expected: PASS (6 tests). Same rule as Task 2: do not change
`CheckpointViewModel.kt` to make a test pass — investigate the assertion
against the real source first.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt
git commit -m "test: add CheckpointViewModel coverage (init, grading, SM-2, unit progress, scoring)"
```

---

### Task 4: `PlacementCheckpointViewModelTest`

**Files:**
- Create: `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt`

**Interfaces:**
- Consumes: `MainDispatcherRule`, `FakeExerciseRepository`,
  `FakeContentRepository`, `FakeProgressRepository`,
  `FakeCheckpointRepository` (Task 1, unchanged).
- Produces: nothing — last task in the plan.

This is the most important file in this plan: it covers the
defer-on-fail buffering that the rest of Fase 2.1b's placement-checkpoint
design depends on (see Global Constraints).

- [ ] **Step 1: Write the test file**

Create `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt`:

```kotlin
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
        getSkippedUnitsUseCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(contentRepository)),
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
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.checkpoint.PlacementCheckpointViewModelTest"`
Expected: PASS (7 tests). Same rule as Tasks 2-3: do not change
`PlacementCheckpointViewModel.kt` to make a test pass — if the last two
tests (pass/fail buffering) don't behave as expected, that is a genuine
regression in the defer-on-fail guarantee and must be escalated, not
patched around.

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — full regression check across all 4 tasks'
tests plus every pre-existing test.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt
git commit -m "test: add PlacementCheckpointViewModel coverage, including the defer-on-fail SM-2 guarantee"
```
