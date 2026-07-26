# Ruta / SM-2 Progress Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the disconnect between the daily SM-2 engine ("Estudiar hoy")
and Ruta's unit-completion tracking, root-caused via on-device DB
inspection: a player can answer 100% of all exercises via the daily
session and Ruta still shows 0% progress, because completion is only
ever recorded when a unit is played through Ruta directly.

**Architecture:** No Room schema change. Two independent-but-related
changes: (1) a new use case narrows "new" exercises in the daily session
to the single current Ruta unit; (2) unit-completion detection moves from
"only at the end of a unit-specific session" to "after every answer, in
any session that isn't the placement checkpoint."

**Tech Stack:** Kotlin (domain/use-case layer + 2 ViewModels), JUnit4 +
kotlinx-coroutines-test (existing project conventions, hand-written fakes).

**Design doc:** `docs/superpowers/specs/2026-07-25-ruta-sm2-progress-sync-design.md`

## Global Constraints

- `PlacementCheckpointViewModel` is **not** touched by this plan. It keeps
  its existing defer-on-fail mechanism (Fase 2.1b): buffered SM-2 writes,
  and `CompleteCheckpointUseCase.unlockSkippedUnits()` already marks
  skipped units complete via `completedVia = "placement"` on pass. Adding
  per-answer unit-completion marking there would write state before the
  checkpoint result is known, breaking that guarantee.
- `getDueExercises` keeps its current unrestricted scope (any section,
  including ones fed by a passed placement checkpoint) — only the "new"
  exercise selection changes.
- No backfill/migration for existing on-device data. This plan does not
  need to preserve or recompute progress from any pre-existing
  `review_state` rows — the device this is tested on will be reinstalled
  clean first.
- This project has no ViewModel/Compose UI tests (see
  `docs/adrs/2026-07-24-viewmodel-and-smoke-testing-strategy.md` — adding
  that infrastructure is separately scoped, not part of this plan).
  `SessionViewModel`/`CheckpointViewModel` changes are verified by
  compilation + manual on-device QA, matching this project's existing
  pattern for ViewModel changes.

---

### Task 1: `GetCurrentUnitUseCase`

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt`

**Interfaces:**
- Consumes: `GetLearningPathUseCase` (existing, unchanged).
- Produces: `class GetCurrentUnitUseCase { suspend operator fun
  invoke(): LearningUnit? }` — consumed by Task 2.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.ContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForCurrentUnit(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetCurrentUnitUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun played(unitId: String) = CompletedUnit(unitId, UnitCompletionSource.PLAYED)

    @Test
    fun `current unit is the first unlocked, incomplete unit`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = emptyList()
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository))

        val current = useCase()

        assertEquals("s1-u1", current?.id)
    }

    @Test
    fun `current unit advances once the previous unit is completed`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository))

        val current = useCase()

        assertEquals("s1-u2", current?.id)
    }

    @Test
    fun `current unit crosses into the next section once the previous one is fully complete`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository))

        val current = useCase()

        assertEquals("s2-u1", current?.id)
    }

    @Test
    fun `a unit completed via placement checkpoint is skipped when picking the current unit`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(CompletedUnit("s1-u1", UnitCompletionSource.PLACEMENT))
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository))

        val current = useCase()

        assertEquals("s1-u2", current?.id)
    }

    @Test
    fun `no current unit once every section is fully complete`() = runTest {
        val repository = FakeContentRepositoryForCurrentUnit(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository))

        val current = useCase()

        assertNull(current)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetCurrentUnitUseCaseTest"`
Expected: FAIL — compile error, `GetCurrentUnitUseCase` is unresolved.

- [ ] **Step 3: Implement `GetCurrentUnitUseCase`**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.LearningUnit
import javax.inject.Inject

class GetCurrentUnitUseCase @Inject constructor(
    private val getLearningPathUseCase: GetLearningPathUseCase
) {
    suspend operator fun invoke(): LearningUnit? =
        getLearningPathUseCase()
            .flatMap { it.units }
            .firstOrNull { it.unlocked && !it.completed }
            ?.unit
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetCurrentUnitUseCaseTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt
git commit -m "feat: add GetCurrentUnitUseCase to identify the next Ruta unit to play"
```

---

### Task 2: Gate "new" exercises in the daily session to the current unit

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`
- Modify (mechanical, one line each — removing a now-nonexistent interface
  override): `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt`,
  `GetUnitSessionUseCaseTest.kt`, `GetCheckpointSessionUseCaseTest.kt`,
  `SubmitAnswerUseCaseTest.kt`, `MarkUnitProgressUseCaseTest.kt`,
  `CompleteCheckpointUseCaseTest.kt`

**Interfaces:**
- Consumes: `GetCurrentUnitUseCase` (Task 1), `ExerciseRepository.getExercisesByUnit`
  and `.getAnsweredExerciseIds` (existing, unchanged).
- Produces: nothing new consumed by later tasks — `GetTodaySessionUseCase`'s
  public signature (`invoke(today: LocalDate, newExercisesLimit: Int =
  5): List<Exercise>`) does not change, only its internal behavior.

`getNewExercises`/`ExerciseDao.getNew` becomes dead code once this task
lands (its only real caller was `GetTodaySessionUseCase`) — this task
removes it along with the behavior change, rather than leaving unused code
behind.

- [ ] **Step 1: Write the failing tests**

Replace the full content of
`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForTodaySession(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

private class FakeExerciseRepositoryForSession(
    private val due: List<Exercise> = emptyList(),
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val answeredIds: Set<String> = emptySet()
) : ExerciseRepository {
    val savedStates = mutableListOf<ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = exercisesByUnit[unitId] ?: emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? =
        savedStates.find { it.exerciseId == exerciseId }
    override suspend fun saveReviewState(state: ReviewState) {
        savedStates.removeAll { it.exerciseId == state.exerciseId }
        savedStates.add(state)
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        exerciseIds.filter { it in answeredIds }
}

class GetTodaySessionUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    private fun exercise(id: String, unitId: String = "s1-u1") = Exercise(id, unitId, "fill_blank", "{}", 1)
    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)

    @Test
    fun `session lists due exercises before new ones`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            due = listOf(exercise("due-1"), exercise("due-2")),
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("new-1")))
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository))
        )

        val result = useCase(today)

        assertEquals(listOf("due-1", "due-2", "new-1"), result.map { it.id })
    }

    @Test
    fun `new exercises are limited to the requested count`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(exercise("new-1"), exercise("new-2"), exercise("new-3"))
            )
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository))
        )

        val result = useCase(today, newExercisesLimit = 2)

        assertEquals(listOf("new-1", "new-2"), result.map { it.id })
    }

    @Test
    fun `new exercises never come from a unit other than the current one`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))
            ),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(exercise("current-1", "s1-u1")),
                "s1-u2" to listOf(exercise("other-unit-1", "s1-u2"))
            )
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository))
        )

        val result = useCase(today)

        assertEquals(listOf("current-1"), result.map { it.id })
    }

    @Test
    fun `already-answered exercises in the current unit are not offered again as new`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("answered-1"), exercise("unanswered-1"))),
            answeredIds = setOf("answered-1")
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository))
        )

        val result = useCase(today)

        assertEquals(listOf("unanswered-1"), result.map { it.id })
    }

    @Test
    fun `no new exercises once every section is fully complete`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(CompletedUnit("s1-u1", com.zconte.oopsapp.domain.model.UnitCompletionSource.PLAYED))
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            due = listOf(exercise("due-1"))
        )
        val useCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository))
        )

        val result = useCase(today)

        assertEquals(listOf("due-1"), result.map { it.id })
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCaseTest"`
Expected: FAIL — compile error (`GetTodaySessionUseCase` doesn't yet take
a `GetCurrentUnitUseCase` parameter, `ExerciseRepository` doesn't yet drop
`getNewExercises`).

- [ ] **Step 3: Update `GetTodaySessionUseCase`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class GetTodaySessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val getCurrentUnitUseCase: GetCurrentUnitUseCase
) {
    suspend operator fun invoke(today: LocalDate, newExercisesLimit: Int = 5): List<Exercise> {
        val due = exerciseRepository.getDueExercises(today, limit = Int.MAX_VALUE)
        val currentUnit = getCurrentUnitUseCase()
        val new = currentUnit?.let { unit ->
            val unitExercises = exerciseRepository.getExercisesByUnit(unit.id)
            val answeredIds = exerciseRepository.getAnsweredExerciseIds(unitExercises.map { it.id }).toSet()
            unitExercises.filterNot { it.id in answeredIds }.take(newExercisesLimit)
        } ?: emptyList()
        return due + new
    }
}
```

- [ ] **Step 4: Remove `getNewExercises` from the repository interface, impl, and DAO**

In `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`,
remove line 9:

```kotlin
    suspend fun getNewExercises(limit: Int): List<Exercise>
```

In `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`,
remove lines 21-22:

```kotlin
    override suspend fun getNewExercises(limit: Int): List<Exercise> =
        exerciseDao.getNew(limit).map { it.toDomain() }
```

In `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`,
remove lines 26-33:

```kotlin
    @Query(
        """
        SELECT * FROM exercises
        WHERE id NOT IN (SELECT exerciseId FROM review_state)
        LIMIT :limit
        """
    )
    suspend fun getNew(limit: Int): List<ExerciseEntity>
```

- [ ] **Step 5: Remove the now-invalid `getNewExercises` override from the 6 other fake repositories**

Each of these files has a fake class implementing `ExerciseRepository`
with this exact line — removing it is required because the interface no
longer declares the method (leaving it in place would be a compile error:
"member is not an override"). Remove this single line from each:

```kotlin
    override suspend fun getNewExercises(limit: Int): List<Exercise> = emptyList()
```

- `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt:16`
- `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt:15`
- `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt:20`
- `app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt:15`
- `app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt:22`
- `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt:47`

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass — this is the full-suite run,
since Step 5 touched 6 other test files and needs the regression check.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt \
        app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt \
        app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt \
        app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt
git commit -m "fix: scope daily session's new exercises to the current Ruta unit"
```

---

### Task 3: Mark unit progress per answer in Session and voluntary Checkpoint

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`

**Interfaces:**
- Consumes: `MarkUnitProgressUseCase` (existing, unchanged) — already
  injected into `SessionViewModel`; newly injected into `CheckpointViewModel`.
- Produces: nothing consumed by later tasks — this is the last task in the
  plan.

No automated test for this task — this project has no ViewModel tests
(see Global Constraints). Verification is compilation + the manual
on-device QA at the end of this plan.

- [ ] **Step 1: `SessionViewModel` — mark progress after every answer, not just at unit-session end**

In `app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt`,
replace the `submitAnswer` function:

```kotlin
    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        if (current.isAnswered) return
        val exercise = current.currentExercise ?: return
        val exerciseId = current.queue.first().id
        val correct = gradeExerciseAnswer(exercise, userAnswer)

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(exerciseId, quality = if (correct) 5 else 2, today = LocalDate.now())
        }
    }
```

with:

```kotlin
    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        if (current.isAnswered) return
        val exercise = current.currentExercise ?: return
        val queuedExercise = current.queue.first()
        val correct = gradeExerciseAnswer(exercise, userAnswer)

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(queuedExercise.id, quality = if (correct) 5 else 2, today = LocalDate.now())
            markUnitProgressUseCase(queuedExercise.unitId, LocalDate.now())
        }
    }
```

Then replace the `nextExercise` function's completion branch:

```kotlin
    fun nextExercise() {
        if (_uiState.value.isCompleting) return
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            _uiState.update { it.copy(isCompleting = true) }
            viewModelScope.launch {
                // Wait for the last exercise's answer write before completing, so navigating
                // away (and clearing this ViewModel's scope) can't cancel it mid-flight.
                pendingAnswerJob?.join()
                updateStreakUseCase(LocalDate.now())
                unitId?.let { markUnitProgressUseCase(it, LocalDate.now()) }
                _uiState.update { it.copy(isSessionComplete = true) }
            }
        } else {
```

with:

```kotlin
    fun nextExercise() {
        if (_uiState.value.isCompleting) return
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            _uiState.update { it.copy(isCompleting = true) }
            viewModelScope.launch {
                // Wait for the last exercise's answer write before completing, so navigating
                // away (and clearing this ViewModel's scope) can't cancel it mid-flight.
                pendingAnswerJob?.join()
                updateStreakUseCase(LocalDate.now())
                _uiState.update { it.copy(isSessionComplete = true) }
            }
        } else {
```

(the `unitId?.let { markUnitProgressUseCase(it, LocalDate.now()) }` line is
removed — it's now subsumed by the per-answer call in `submitAnswer`,
which covers both unit-specific sessions and "Estudiar hoy" uniformly).

- [ ] **Step 2: `CheckpointViewModel` — inject `MarkUnitProgressUseCase` and mark progress after every answer**

In `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`,
add the import (alongside the existing `domain.usecase` imports):

```kotlin
import com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCase
```

Add a new constructor parameter (alongside the existing ones):

```kotlin
    private val markUnitProgressUseCase: MarkUnitProgressUseCase,
```

Replace the `submitAnswer` function:

```kotlin
    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        if (current.isAnswered) return
        val exercise = current.currentExercise ?: return
        val exerciseId = current.queue.first().id
        val correct = gradeExerciseAnswer(exercise, userAnswer)
        if (correct) correctCount++

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(exerciseId, quality = if (correct) 5 else 2, today = LocalDate.now())
        }
    }
```

with:

```kotlin
    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        if (current.isAnswered) return
        val exercise = current.currentExercise ?: return
        val queuedExercise = current.queue.first()
        val correct = gradeExerciseAnswer(exercise, userAnswer)
        if (correct) correctCount++

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(queuedExercise.id, quality = if (correct) 5 else 2, today = LocalDate.now())
            markUnitProgressUseCase(queuedExercise.unitId, LocalDate.now())
        }
    }
```

- [ ] **Step 3: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — regression check for Tasks 1-2's tests plus
everything else.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/session/SessionViewModel.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt
git commit -m "fix: mark unit progress per answer in Session and voluntary Checkpoint, not just at unit-session end"
```

---

## After all tasks: manual on-device QA (clean reinstall)

Per the design spec's decision, no backfill exists — reinstall clean
before testing. No automated UI test covers this end-to-end flow.

1. Uninstall + reinstall, confirm Ruta shows Fundamentos de Java's first
   unit as the only unlocked/current position (0% everywhere else).
2. Tap "Estudiar hoy" from Home repeatedly across a few sessions (or reset
   the device date forward between sessions) and confirm: (a) it never
   offers exercises from a locked section/unit; (b) once every exercise of
   the current unit has been answered — note this will typically take
   **more than one "Estudiar hoy" session**, since `newExercisesLimit = 5`
   caps new exercises per session while units have 6-7 — Ruta's
   current-position indicator advances to the next unit, matching what
   playing that unit directly from Ruta would have shown. Needing a second
   session (or a second day) to clear one unit is correct SRS pacing, not
   a bug — don't misdiagnose it as the fix not working.
3. Confirm the end-of-section voluntary checkpoint now appears after
   finishing a section's units via "Estudiar hoy" (previously it likely
   never triggered, since section-complete depended on the same
   `unit_progress` data).
4. Confirm the placement/skip checkpoint (Fase 2.1b) still behaves
   identically to before — tap into a locked section ahead of your
   current position, confirm the skip-checkpoint offer still appears and
   passing/failing it behaves the same as previously verified.
