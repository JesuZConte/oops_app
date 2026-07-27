# Mandatory Cumulative Checkpoint — UI Wiring (Plan 2 of 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire Plan 1's dormant domain/persistence pieces into the UI: a
timer on the checkpoint, a results breakdown on failure, the retry-lock
reflected in `CheckpointViewModel`/`ProgressScreen`, and the "ESTUDIAR HOY"
routing fix found during Plan 1's on-device QA.

**Architecture:** `SectionPath` gains a `checkpointStatus` enum (additive,
computed in `GetLearningPathUseCase` using Plan 1's
`IsCheckpointRetryUnlockedUseCase`). A new `GetNextStudyStepUseCase`
decides where "ESTUDIAR HOY" should go. `HomeViewModel` and
`ProgressScreen` consume `checkpointStatus` to fix the stale "TU RUTA"
card and the "opcional" copy. `CheckpointViewModel` gains an explicit
state machine (retry-locked / intro / in-progress / complete), a small
injectable `Clock` abstraction for a testable timer, and a results
breakdown wrapper — reusing Plan 1's `computeCheckpointTimeBudgetSeconds`
and `computeCheckpointSectionBreakdown`, which shipped tested but unused.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, JUnit4 +
`kotlinx-coroutines-test` (hand-written fakes, no mocking library — same
convention as the rest of this codebase), reusing the Level 1 ViewModel
test infrastructure in `app/src/test/java/com/zconte/oopsapp/testutil/`.

**Design doc:** `docs/superpowers/specs/2026-07-26-mandatory-checkpoint-ui-wiring-design.md`

## Global Constraints

- **No schema/migration changes.** This plan is pure ViewModel/UI wiring
  plus one small new domain-layer abstraction (`Clock`, Task 6) — no Room
  entity, DAO, or migration is touched.
- **`checkpointStatus` is additive** — `SectionPath.checkpointSatisfied`
  and the `previousSectionFullyDone` gating logic inside
  `GetLearningPathUseCase` (Plan 1, proven on-device) are not touched.
  `checkpointStatus == SATISFIED` must always agree with
  `checkpointSatisfied == true`.
- **`HomeViewModel` is not directly unit-tested in this plan.** Its `init`
  calls `contentSeeder.seedIfNeeded()`, and `ContentSeeder` is a concrete
  class with Room DAO dependencies (no interface to fake) — the same
  reason `HomeViewModel` has no test today (Level 1 ViewModel testing
  never covered it). Task 3 avoids this entirely by extracting the new
  decision logic (which section is "current", whether its checkpoint is
  pending) into a pure, dependency-free function
  (`summarizeCurrentSection`) that is fully unit-tested on its own; the
  ViewModel itself is verified only by compilation and manual QA, exactly
  like today.
- **No new Compose UI tests.** This project's Level 2 (Compose smoke
  tests) is not started (`docs/adrs/2026-07-24-viewmodel-and-smoke-testing-strategy.md`).
  Every Compose file this plan touches (`HomeScreen.kt`, `ProgressScreen.kt`,
  `CheckpointScreen.kt`) is verified via `./gradlew :app:compileDebugKotlin`
  and manual on-device QA, not automated UI tests — consistent with every
  prior UI change in this project.
- **The retry-locked message is generic, not section-specific.** The
  design doc's "implementation note" about naming which section(s) to
  review was considered and dropped: doing so would require a new
  `ExerciseRepository` method to resolve section names from bare
  `failedExerciseIds` (Plan 1's persisted failure record only stores IDs,
  not `Exercise` objects) — out of scope for a plan that promises zero
  persistence-layer changes. The message explains *why* (you failed
  exercises you haven't re-studied yet) and *how* (they unlock
  automatically once re-answered in daily practice), without naming them.
- **Timer correctness invariant (already resolved by construction, verify
  it stays true):** `failedExerciseIds` — the list that feeds
  `IsCheckpointRetryUnlockedUseCase` — must only ever contain exercises the
  player explicitly answered wrong via `submitAnswer`. An exercise never
  reached before timeout must never appear in it (Plan 1's retry gate would
  otherwise dead-lock permanently — see the design doc's section 6). Tasks
  5 and 6 achieve this simply by construction: the tracking list is only
  appended to inside `submitAnswer`, never for unanswered items — there is
  no separate "mark timed-out items as failed" code path to get wrong.
- **Numeric/behavioral parameters carried over from Plan 1, unchanged by
  this plan:** checkpoint size (floor 8/+2 per section/ceiling 20), time
  budget (`round(questionCount × 1.8)` minutes), 68% pass threshold, streak
  decoupling.

---

### Task 1: `SectionPath.checkpointStatus` — the gate's UI-facing status

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt`

**Interfaces:**
- Consumes: `IsCheckpointRetryUnlockedUseCase` (Plan 1, already
  `@Inject`-constructible with `CheckpointRepository` + `ExerciseRepository`).
- Produces: `SectionPath.checkpointStatus: CheckpointStatus` — the enum
  `PENDING | RETRY_LOCKED | RETRY_AVAILABLE | SATISFIED`, consumed by
  Task 2 (`GetNextStudyStepUseCase`), Task 3 (`HomeViewModel`), and Task 4
  (`ProgressScreen`'s `CheckpointRow`).

- [ ] **Step 1: Write the failing tests in `GetLearningPathUseCaseTest.kt`**

Add these 4 tests to the existing `GetLearningPathUseCaseTest` class (after
the last existing test, `a unit completed via a placement checkpoint
surfaces that source`):

```kotlin

    @Test
    fun `checkpointStatus is PENDING for a section whose units are not yet complete`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository(), retryUnlockedUseCase(FakeCheckpointRepository()))

        val path = useCase()

        assertEquals(CheckpointStatus.PENDING, path.first().checkpointStatus)
    }

    @Test
    fun `checkpointStatus is PENDING for a completed section with no attempt on record`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository(), retryUnlockedUseCase(FakeCheckpointRepository()))

        val path = useCase()

        assertEquals(CheckpointStatus.PENDING, path.first().checkpointStatus)
    }

    @Test
    fun `checkpointStatus is RETRY_LOCKED for a failed attempt whose failures were never re-studied`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false,
            takenAt = LocalDate.of(2026, 7, 20), failedExerciseIds = listOf("ex-1")
        )
        val useCase = GetLearningPathUseCase(repository, checkpointRepository, retryUnlockedUseCase(checkpointRepository))

        val path = useCase()

        assertEquals(CheckpointStatus.RETRY_LOCKED, path.first().checkpointStatus)
    }

    @Test
    fun `checkpointStatus is RETRY_AVAILABLE once every failed exercise was re-studied`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false,
            takenAt = LocalDate.of(2026, 7, 20), failedExerciseIds = listOf("ex-1")
        )
        val exerciseRepository = FakeExerciseRepository()
        exerciseRepository.saveReviewState(
            com.zconte.oopsapp.domain.model.ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1, repetitions = 1,
                dueDate = LocalDate.of(2026, 7, 22), lastReviewedAt = LocalDate.of(2026, 7, 21)
            )
        )
        val useCase = GetLearningPathUseCase(repository, checkpointRepository, IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository))

        val path = useCase()

        assertEquals(CheckpointStatus.RETRY_AVAILABLE, path.first().checkpointStatus)
    }
```

Add these imports to `GetLearningPathUseCaseTest.kt` (alongside the
existing ones):

```kotlin
import com.zconte.oopsapp.testutil.FakeExerciseRepository
```

Add this private helper to the test class, right after the existing
`played(unitId: String)` helper — it exists purely so the 3 tests that
don't care about retry-unlock behavior don't need to spell out
`IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository())`
every time:

```kotlin
    private fun retryUnlockedUseCase(checkpointRepository: FakeCheckpointRepository) =
        IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository())
```

Also update every existing call to
`GetLearningPathUseCase(repository, FakeCheckpointRepository())` (and the
one at `checkpointRepository)` in `a section unlocks once every unit is
complete AND its checkpoint is approved`) in this file to pass a third
argument, e.g. `GetLearningPathUseCase(repository, checkpointRepository,
retryUnlockedUseCase(checkpointRepository))`. Run
`grep -n "GetLearningPathUseCase(repository" app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`
first to see the exact current count and line numbers before editing —
do not rely on a specific number here; replace every match this grep
returns (7 call sites use the inline `FakeCheckpointRepository()` form,
plus 1 uses the named `checkpointRepository` var, as of the last time this
plan was written — but re-verify with the grep, since Task 1 Step 1 above
already added 4 new tests to this same file that must NOT be touched by
this step, only the pre-existing ones).

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetLearningPathUseCaseTest"`
Expected: FAIL — compile error, `checkpointStatus` doesn't exist yet and
`GetLearningPathUseCase`'s constructor doesn't take a third argument yet.

- [ ] **Step 3: Add `CheckpointStatus` and the `checkpointStatus` field**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

enum class CheckpointStatus { PENDING, RETRY_LOCKED, RETRY_AVAILABLE, SATISFIED }

data class UnitProgress(
    val unit: LearningUnit,
    val completed: Boolean,
    val unlocked: Boolean,
    val completedVia: String = UnitCompletionSource.PLAYED
)

data class SectionPath(
    val section: Section,
    val unlocked: Boolean,
    val units: List<UnitProgress>,
    val completed: Boolean,
    val checkpointSatisfied: Boolean,
    val checkpointStatus: CheckpointStatus
)
```

- [ ] **Step 4: Extend `GetLearningPathUseCase` to compute it**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointStatus
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.model.UnitProgress
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

class GetLearningPathUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
    private val checkpointRepository: CheckpointRepository,
    private val isCheckpointRetryUnlockedUseCase: IsCheckpointRetryUnlockedUseCase
) {
    suspend operator fun invoke(): List<SectionPath> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val completedUnits = contentRepository.getCompletedUnits().associateBy { it.unitId }

        var previousSectionFullyDone = true
        return sections.map { section ->
            val units = contentRepository.getUnitsBySection(section.id).sortedBy { it.orderIndex }
            val sectionUnlocked = previousSectionFullyDone

            var previousUnitComplete = true
            val unitProgress = units.map { unit ->
                val record = completedUnits[unit.id]
                val completed = record != null
                val unlocked = sectionUnlocked && previousUnitComplete
                previousUnitComplete = completed
                UnitProgress(unit, completed, unlocked, record?.completedVia ?: UnitCompletionSource.PLAYED)
            }

            val sectionComplete = units.isNotEmpty() && units.all { it.id in completedUnits }
            val checkpointSatisfied = sectionComplete && (
                checkpointRepository.hasApprovedAttempt(section.id, CheckpointKind.REVIEW) ||
                    unitProgress.all { it.completedVia == UnitCompletionSource.PLACEMENT }
                )
            val checkpointStatus = computeCheckpointStatus(section.id, sectionComplete, checkpointSatisfied)
            previousSectionFullyDone = checkpointSatisfied

            SectionPath(section, sectionUnlocked, unitProgress, sectionComplete, checkpointSatisfied, checkpointStatus)
        }
    }

    private suspend fun computeCheckpointStatus(
        sectionId: String,
        sectionComplete: Boolean,
        checkpointSatisfied: Boolean
    ): CheckpointStatus = when {
        checkpointSatisfied -> CheckpointStatus.SATISFIED
        !sectionComplete -> CheckpointStatus.PENDING
        checkpointRepository.getLatestFailedAttempt(sectionId, CheckpointKind.REVIEW) == null -> CheckpointStatus.PENDING
        isCheckpointRetryUnlockedUseCase(sectionId, CheckpointKind.REVIEW) -> CheckpointStatus.RETRY_AVAILABLE
        else -> CheckpointStatus.RETRY_LOCKED
    }
}
```

- [ ] **Step 5: Run the tests to verify the new ones pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetLearningPathUseCaseTest"`
Expected: PASS (12 tests: 8 existing + 4 new).

- [ ] **Step 6: Mechanically fix the 5 other test files that construct `GetLearningPathUseCase` directly**

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCaseTest.kt`:
add `import com.zconte.oopsapp.domain.model.CheckpointKind` is NOT needed
(no attempts recorded in this file), but add:
```kotlin
import com.zconte.oopsapp.testutil.FakeExerciseRepository
```
Then replace all 5 occurrences of the exact string
`GetLearningPathUseCase(repository, FakeCheckpointRepository())` with
`GetLearningPathUseCase(repository, FakeCheckpointRepository(), IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), FakeExerciseRepository()))`.
(Using a second, separate `FakeCheckpointRepository()` instance for the
retry-unlock check is safe here: none of this file's 5 tests ever record a
checkpoint attempt, so both instances are permanently empty and behave
identically.)

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`:
replace all 5 occurrences of the exact string
`GetLearningPathUseCase(contentRepository, FakeCheckpointRepository())` with
`GetLearningPathUseCase(contentRepository, FakeCheckpointRepository(), IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), exerciseRepository))`.
(`exerciseRepository` is already in scope at every one of these 5 call
sites — each test constructs it a few lines above. Reusing it here is
harmless: these tests never populate a failed checkpoint attempt, so
`IsCheckpointRetryUnlockedUseCase` is never exercised meaningfully.)

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt`:
add:
```kotlin
import com.zconte.oopsapp.testutil.FakeExerciseRepository
```
Replace the 4 occurrences of the exact string
`GetLearningPathUseCase(repository, FakeCheckpointRepository())` with
`GetLearningPathUseCase(repository, FakeCheckpointRepository(), IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), FakeExerciseRepository()))`.
Then, for the one remaining occurrence that uses a named
`checkpointRepository` variable (in `current unit crosses into the next
section once the previous one is fully complete`), change:
```kotlin
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository, checkpointRepository))
```
to:
```kotlin
        val useCase = GetCurrentUnitUseCase(
            GetLearningPathUseCase(repository, checkpointRepository, IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository()))
        )
```
(This one reuses the same `checkpointRepository` instance that
`recordAttempt` was just called on — required for the shared-state
`hasApprovedAttempt` check inside `GetLearningPathUseCase` to see that
recorded attempt.)

In `app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt`,
inside `buildViewModel`, change:
```kotlin
        getTodaySessionUseCase = GetTodaySessionUseCase(exerciseRepository, GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository()))),
```
to:
```kotlin
        getTodaySessionUseCase = GetTodaySessionUseCase(
            exerciseRepository,
            GetCurrentUnitUseCase(
                GetLearningPathUseCase(contentRepository, FakeCheckpointRepository(), IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), exerciseRepository))
            )
        ),
```
(`exerciseRepository` is already a parameter of `buildViewModel`.)

In `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt`,
inside `buildViewModel`, change:
```kotlin
        getSkippedUnitsUseCase = GetSkippedUnitsUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository())),
```
to:
```kotlin
        getSkippedUnitsUseCase = GetSkippedUnitsUseCase(
            GetLearningPathUseCase(contentRepository, FakeCheckpointRepository(), IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), exerciseRepository))
        ),
```
(`exerciseRepository` is already a parameter of `buildViewModel`.)

- [ ] **Step 7: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. All pre-existing tests in the 6 touched files
still pass unchanged in behavior (only constructor call sites changed),
plus `GetLearningPathUseCaseTest`'s 4 new tests.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt \
        app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt
git commit -m "feat: add SectionPath.checkpointStatus (pending/retry-locked/retry-available/satisfied)"
```

---

### Task 2: `GetNextStudyStepUseCase` — where "ESTUDIAR HOY" should go

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetNextStudyStepUseCase.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetNextStudyStepUseCaseTest.kt`

**Interfaces:**
- Consumes: `GetTodaySessionUseCase.invoke(today: LocalDate): List<Exercise>`
  (existing), `GetLearningPathUseCase.invoke(): List<SectionPath>`
  (existing, extended by Task 1 with `checkpointStatus`/`checkpointSatisfied`
  — this task only reads `completed` and `checkpointSatisfied`, both
  already present).
- Produces: `NextStudyStep` (sealed class: `DailySession`,
  `Checkpoint(sectionId: String)`, `NothingPending`) and
  `GetNextStudyStepUseCase.invoke(today: LocalDate): NextStudyStep` —
  consumed by Task 3 (`HomeViewModel`).

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetNextStudyStepUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
import com.zconte.oopsapp.testutil.FakeExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForNextStep(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetNextStudyStepUseCaseTest {

    private val today = LocalDate.of(2026, 7, 26)

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "fill_blank", "{}", 1)
    private fun played(unitId: String) = CompletedUnit(unitId, UnitCompletionSource.PLAYED)

    private fun useCase(
        contentRepository: ContentRepository,
        exerciseRepository: FakeExerciseRepository,
        checkpointRepository: FakeCheckpointRepository
    ) = GetNextStudyStepUseCase(
        GetTodaySessionUseCase(exerciseRepository, GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, checkpointRepository, IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository)))),
        GetLearningPathUseCase(contentRepository, checkpointRepository, IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository))
    )

    @Test
    fun `returns DailySession when there is due content today`() = runTest {
        val contentRepository = FakeContentRepositoryForNextStep(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepository(dueExercises = listOf(exercise("due-1", "s1-u1")))

        val result = useCase(contentRepository, exerciseRepository, FakeCheckpointRepository())(today)

        assertEquals(NextStudyStep.DailySession, result)
    }

    @Test
    fun `returns Checkpoint for the first section whose units are done but not checkpoint-satisfied`() = runTest {
        val contentRepository = FakeContentRepositoryForNextStep(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val exerciseRepository = FakeExerciseRepository()

        val result = useCase(contentRepository, exerciseRepository, FakeCheckpointRepository())(today)

        assertEquals(NextStudyStep.Checkpoint("s1"), result)
    }

    @Test
    fun `returns Checkpoint even when its retry is locked -- CheckpointViewModel shows the explanation, not this use case`() = runTest {
        val contentRepository = FakeContentRepositoryForNextStep(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false,
            takenAt = today, failedExerciseIds = listOf("ex-1")
        )
        val exerciseRepository = FakeExerciseRepository()

        val result = useCase(contentRepository, exerciseRepository, checkpointRepository)(today)

        assertEquals(NextStudyStep.Checkpoint("s1"), result)
    }

    @Test
    fun `returns NothingPending when every section is checkpoint-satisfied and nothing is due`() = runTest {
        val contentRepository = FakeContentRepositoryForNextStep(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 90, passed = true,
            takenAt = today, failedExerciseIds = emptyList()
        )
        val exerciseRepository = FakeExerciseRepository()

        val result = useCase(contentRepository, exerciseRepository, checkpointRepository)(today)

        assertEquals(NextStudyStep.NothingPending, result)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetNextStudyStepUseCaseTest"`
Expected: FAIL — compile error, `GetNextStudyStepUseCase` doesn't exist.

- [ ] **Step 3: Create `GetNextStudyStepUseCase`**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetNextStudyStepUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import java.time.LocalDate
import javax.inject.Inject

sealed class NextStudyStep {
    object DailySession : NextStudyStep()
    data class Checkpoint(val sectionId: String) : NextStudyStep()
    object NothingPending : NextStudyStep()
}

/**
 * Decides where the "ESTUDIAR HOY" button should take the player: their normal daily session,
 * a pending section checkpoint (rendible or retry-locked -- CheckpointViewModel tells those
 * apart), or nowhere, if there is truly nothing to do. Exists so that button never opens an
 * empty session that silently bounces back.
 */
class GetNextStudyStepUseCase @Inject constructor(
    private val getTodaySessionUseCase: GetTodaySessionUseCase,
    private val getLearningPathUseCase: GetLearningPathUseCase
) {
    suspend operator fun invoke(today: LocalDate): NextStudyStep {
        if (getTodaySessionUseCase(today).isNotEmpty()) return NextStudyStep.DailySession

        val pendingSection = getLearningPathUseCase().firstOrNull { it.completed && !it.checkpointSatisfied }
        return pendingSection?.let { NextStudyStep.Checkpoint(it.section.id) } ?: NextStudyStep.NothingPending
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetNextStudyStepUseCaseTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetNextStudyStepUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetNextStudyStepUseCaseTest.kt
git commit -m "feat: add GetNextStudyStepUseCase to decide where ESTUDIAR HOY routes"
```

---

### Task 3: Home — pending-checkpoint display + "ESTUDIAR HOY" routing

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/SummarizeCurrentSectionUseCase.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/domain/usecase/SummarizeCurrentSectionUseCaseTest.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`

**Interfaces:**
- Consumes: `GetNextStudyStepUseCase` (Task 2), `SectionPath.completed` /
  `checkpointSatisfied` (existing).
- Produces: `summarizeCurrentSection(sections: List<SectionPath>):
  HomeSectionSummary` (pure function — consumed only by `HomeViewModel`);
  `HomeUiState.isCheckpointPending: Boolean` and
  `HomeUiState.nextStudyStep: NextStudyStep`; `HomeScreen`'s new
  `onOpenCheckpoint: (String) -> Unit` parameter.

- [ ] **Step 1: Write the failing test for the pure summary function**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/SummarizeCurrentSectionUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointStatus
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.model.UnitProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummarizeCurrentSectionUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)

    private fun sectionPath(
        id: String,
        order: Int,
        completed: Boolean,
        checkpointSatisfied: Boolean,
        status: CheckpointStatus
    ) = SectionPath(
        section = section(id, order),
        unlocked = true,
        units = listOf(UnitProgress(unit("$id-u1", id, 1), completed, true, UnitCompletionSource.PLAYED)),
        completed = completed,
        checkpointSatisfied = checkpointSatisfied,
        checkpointStatus = status
    )

    @Test
    fun `an in-progress section is current, and its checkpoint is not pending`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = false, checkpointSatisfied = false, status = CheckpointStatus.PENDING)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s1", summary.currentSection?.section?.id)
        assertFalse(summary.isCheckpointPending)
    }

    @Test
    fun `a section done with units but not checkpoint-satisfied stays current, with its checkpoint pending`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = true, checkpointSatisfied = false, status = CheckpointStatus.PENDING),
            sectionPath("s2", 2, completed = false, checkpointSatisfied = false, status = CheckpointStatus.PENDING)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s1", summary.currentSection?.section?.id)
        assertTrue(summary.isCheckpointPending)
    }

    @Test
    fun `once satisfied, the next section becomes current`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = true, checkpointSatisfied = true, status = CheckpointStatus.SATISFIED),
            sectionPath("s2", 2, completed = false, checkpointSatisfied = false, status = CheckpointStatus.PENDING)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s2", summary.currentSection?.section?.id)
        assertFalse(summary.isCheckpointPending)
    }

    @Test
    fun `when every section is satisfied, the last one is current with nothing pending`() {
        val sections = listOf(
            sectionPath("s1", 1, completed = true, checkpointSatisfied = true, status = CheckpointStatus.SATISFIED)
        )

        val summary = summarizeCurrentSection(sections)

        assertEquals("s1", summary.currentSection?.section?.id)
        assertFalse(summary.isCheckpointPending)
    }

    @Test
    fun `an empty roadmap has no current section`() {
        val summary = summarizeCurrentSection(emptyList())

        assertEquals(null, summary.currentSection)
        assertFalse(summary.isCheckpointPending)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.SummarizeCurrentSectionUseCaseTest"`
Expected: FAIL — compile error, `summarizeCurrentSection` doesn't exist.

- [ ] **Step 3: Create the pure function**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/SummarizeCurrentSectionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.SectionPath

data class HomeSectionSummary(
    val currentSection: SectionPath?,
    val isCheckpointPending: Boolean
)

/**
 * Picks which section Home's "TU RUTA" card should describe, and whether that section is
 * waiting on its checkpoint. `!checkpointSatisfied` (not `!completed`) is what makes this
 * correct: a section whose units are all done but whose checkpoint isn't approved must stay
 * "current" rather than have the card jump ahead to the next, still-locked section.
 */
fun summarizeCurrentSection(sections: List<SectionPath>): HomeSectionSummary {
    val currentSection = sections.firstOrNull { !it.checkpointSatisfied } ?: sections.lastOrNull()
    val isCheckpointPending = currentSection?.let { it.completed && !it.checkpointSatisfied } ?: false
    return HomeSectionSummary(currentSection, isCheckpointPending)
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.SummarizeCurrentSectionUseCaseTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Wire it into `HomeViewModel`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.data.content.ContentSeeder
import com.zconte.oopsapp.domain.repository.ProgressRepository
import com.zconte.oopsapp.domain.usecase.GetLearningPathUseCase
import com.zconte.oopsapp.domain.usecase.GetNextStudyStepUseCase
import com.zconte.oopsapp.domain.usecase.NextStudyStep
import com.zconte.oopsapp.domain.usecase.summarizeCurrentSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val streak: Int = 0,
    val xp: Int = 0,
    val isReady: Boolean = false,
    val currentSectionName: String = "",
    val currentSectionProgress: Float = 0f,
    val isCheckpointPending: Boolean = false,
    val nextStudyStep: NextStudyStep = NextStudyStep.NothingPending
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val getLearningPathUseCase: GetLearningPathUseCase,
    private val getNextStudyStepUseCase: GetNextStudyStepUseCase,
    private val contentSeeder: ContentSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contentSeeder.seedIfNeeded()
            refreshStats()
            _uiState.update { it.copy(isReady = true) }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshStats() }
    }

    private suspend fun refreshStats() {
        val stats = progressRepository.getUserStats()
        val summary = summarizeCurrentSection(getLearningPathUseCase())
        val progress = summary.currentSection?.let { section ->
            if (section.units.isEmpty()) 0f else section.units.count { it.completed }.toFloat() / section.units.size
        } ?: 0f

        _uiState.update {
            it.copy(
                streak = stats.streak,
                xp = stats.xp,
                currentSectionName = summary.currentSection?.section?.name ?: "",
                currentSectionProgress = progress,
                isCheckpointPending = summary.isCheckpointPending,
                nextStudyStep = getNextStudyStepUseCase(LocalDate.now())
            )
        }
    }
}
```

- [ ] **Step 6: Update `HomeScreen` to show the pending-checkpoint state and route correctly**

In `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`, add this
import:

```kotlin
import com.zconte.oopsapp.domain.usecase.NextStudyStep
```

Change the function signature:

```kotlin
@Composable
fun HomeScreen(
    onStudyClick: () -> Unit,
    onOpenCheckpoint: (String) -> Unit,
    onProgressClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
```

Find the "TU RUTA" card's inner `Row` + `LinearProgressIndicator` block,
currently:

```kotlin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.currentSectionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(uiState.currentSectionProgress * 100).toInt()}% ▶",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { uiState.currentSectionProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
```

Replace it with:

```kotlin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.currentSectionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uiState.isCheckpointPending) {
                            "Checkpoint pendiente ▶"
                        } else {
                            "${(uiState.currentSectionProgress * 100).toInt()}% ▶"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!uiState.isCheckpointPending) {
                    LinearProgressIndicator(
                        progress = { uiState.currentSectionProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
```

Find the "ESTUDIAR HOY" `Button`, currently:

```kotlin
        Button(
            onClick = onStudyClick,
            enabled = uiState.isReady,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("ESTUDIAR HOY", style = MaterialTheme.typography.titleMedium)
        }
```

Replace its `onClick` (everything else in the block is unchanged):

```kotlin
        Button(
            onClick = {
                when (val step = uiState.nextStudyStep) {
                    is NextStudyStep.Checkpoint -> onOpenCheckpoint(step.sectionId)
                    else -> onStudyClick()
                }
            },
            enabled = uiState.isReady,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("ESTUDIAR HOY", style = MaterialTheme.typography.titleMedium)
        }
```

- [ ] **Step 7: Wire the new callback in `OopsNavHost`**

In `app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt`,
change the `HOME` composable:

```kotlin
        composable(OopsDestinations.HOME) {
            HomeScreen(
                onStudyClick = { navController.navigate(OopsDestinations.SESSION) },
                onOpenCheckpoint = { sectionId -> navController.navigate("checkpoint/$sectionId") },
                onProgressClick = {
                    navController.navigate(OopsDestinations.PROGRESS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
```

- [ ] **Step 8: Verify everything compiles**

Run: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL. (Per Global Constraints, `HomeViewModel` has
no direct unit test — `contentSeeder.seedIfNeeded()` needs Room DAOs with
no fake-able interface. `summarizeCurrentSection`, the one piece of new
decision logic, is fully covered by Step 1-4's test.)

- [ ] **Step 9: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/SummarizeCurrentSectionUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/SummarizeCurrentSectionUseCaseTest.kt \
        app/src/main/java/com/zconte/oopsapp/ui/home/HomeViewModel.kt \
        app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt \
        app/src/main/java/com/zconte/oopsapp/navigation/OopsNavHost.kt
git commit -m "feat: fix TU RUTA card and ESTUDIAR HOY routing for pending checkpoints"
```

---

### Task 4: Ver Ruta — `CheckpointRow`'s 3 visual states

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `SectionPath.checkpointStatus` (Task 1).
- Produces: nothing consumed by later tasks — this is a leaf UI change.

- [ ] **Step 1: Replace `CheckpointRow` and its call site**

In `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`,
add this import:

```kotlin
import com.zconte.oopsapp.domain.model.CheckpointStatus
```

Change the call site inside `SectionPathBlock` from:

```kotlin
        if (sectionPath.completed) {
            CheckpointRow(onClick = { onOpenCheckpoint(sectionPath.section.id) })
        }
```

to:

```kotlin
        if (sectionPath.checkpointStatus != CheckpointStatus.SATISFIED) {
            CheckpointRow(
                status = sectionPath.checkpointStatus,
                onClick = { onOpenCheckpoint(sectionPath.section.id) }
            )
        }
```

Replace the `CheckpointRow` composable:

```kotlin
@Composable
private fun CheckpointRow(status: CheckpointStatus, onClick: () -> Unit) {
    val extended = OopsTheme.extendedColors
    val isWarning = status == CheckpointStatus.RETRY_LOCKED
    val dotColor = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val subtitle = when (status) {
        CheckpointStatus.PENDING -> "Checkpoint obligatorio"
        CheckpointStatus.RETRY_LOCKED -> "Repasa lo fallado para reintentar"
        CheckpointStatus.RETRY_AVAILABLE -> "Reinténtalo ahora"
        CheckpointStatus.SATISFIED -> "" // unreachable: the call site hides this row when SATISFIED
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Column {
            Text(
                text = "CHECKPOINT",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P),
                color = dotColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isWarning) MaterialTheme.colorScheme.error else extended.lockedText
            )
        }
    }
}
```

Note: `sectionPath.completed` is no longer referenced by this block (the
gate is now `checkpointStatus != SATISFIED`, which — per Task 1's design —
is `PENDING` for a section whose units aren't done yet too. That's fine:
`CheckpointRow` would only ever render for such a section if `PENDING` were
reachable pre-completion, but it isn't shown at all until then because...).
Actually verify this carefully in Step 2 below before assuming it's fine.

- [ ] **Step 2: Confirm the visibility condition still excludes not-yet-reached sections**

This is a manual review step, not a test (Compose has no test coverage
here per Global Constraints). Read `GetLearningPathUseCase.computeCheckpointStatus`
(Task 1): for a section whose units are not all complete
(`sectionComplete == false`), `checkpointStatus` is `PENDING`. Since
`PENDING != SATISFIED`, the naive condition `checkpointStatus !=
CheckpointStatus.SATISFIED` would show the row **too early** — before the
section's units are even done. Fix the call site to also require
`sectionPath.completed`:

```kotlin
        if (sectionPath.completed && sectionPath.checkpointStatus != CheckpointStatus.SATISFIED) {
            CheckpointRow(
                status = sectionPath.checkpointStatus,
                onClick = { onOpenCheckpoint(sectionPath.section.id) }
            )
        }
```

This matches the original condition (`if (sectionPath.completed)`) plus
the new status-based hide-when-satisfied behavior — exactly Task 4's
scope, nothing more.

- [ ] **Step 3: Verify everything compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (this task touches no JVM-tested code).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt
git commit -m "feat: CheckpointRow shows 3 distinct states and hides once satisfied"
```

---

### Task 5: `CheckpointViewModel` — retry-lock gate + intro state

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt`

**Interfaces:**
- Consumes: `IsCheckpointRetryUnlockedUseCase` (Plan 1),
  `computeCheckpointTimeBudgetSeconds` (Plan 1, dormant until now).
- Produces: `CheckpointUiState.isRetryLocked`, `.showIntro`,
  `.timeBudgetSeconds`; `CheckpointViewModel.startCheckpoint()`. Task 6
  extends this same state/method with the actual timer; Task 7 extends
  the completion path with the results breakdown.

- [ ] **Step 1: Write the failing tests**

Replace the full content of
`app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.checkpoint.CheckpointViewModelTest"`
Expected: FAIL — compile error, `isCheckpointRetryUnlockedUseCase` isn't a
constructor param yet, `showIntro`/`isRetryLocked`/`startCheckpoint()`
don't exist.

- [ ] **Step 3: Implement the state machine**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`:

```kotlin
package com.zconte.oopsapp.ui.checkpoint

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseContent
import com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCase
import com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCase
import com.zconte.oopsapp.domain.usecase.IsCheckpointRetryUnlockedUseCase
import com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCase
import com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCase
import com.zconte.oopsapp.domain.usecase.UpdateStreakUseCase
import com.zconte.oopsapp.domain.usecase.computeCheckpointTimeBudgetSeconds
import com.zconte.oopsapp.domain.usecase.gradeExerciseAnswer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

data class CheckpointUiState(
    val queue: List<Exercise> = emptyList(),
    val currentExercise: ExerciseContent? = null,
    val currentIndex: Int = 0,
    val totalExercises: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val result: CheckpointResult? = null,
    val isCompleting: Boolean = false,
    val isRetryLocked: Boolean = false,
    val showIntro: Boolean = false,
    val timeBudgetSeconds: Int = 0
)

@HiltViewModel
class CheckpointViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCheckpointSessionUseCase: GetCheckpointSessionUseCase,
    private val isCheckpointRetryUnlockedUseCase: IsCheckpointRetryUnlockedUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val completeCheckpointUseCase: CompleteCheckpointUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val markUnitProgressUseCase: MarkUnitProgressUseCase,
    private val json: Json
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])

    private val _uiState = MutableStateFlow(CheckpointUiState())
    val uiState: StateFlow<CheckpointUiState> = _uiState.asStateFlow()

    private val answeredResults = mutableListOf<Pair<Exercise, Boolean>>()
    private var pendingAnswerJob: Job? = null

    init {
        viewModelScope.launch {
            if (!isCheckpointRetryUnlockedUseCase(sectionId)) {
                _uiState.update { it.copy(isRetryLocked = true) }
                return@launch
            }
            val queue = getCheckpointSessionUseCase(sectionId, LocalDate.now())
            if (queue.isEmpty()) {
                _uiState.update { it.copy(isComplete = true, result = CheckpointResult(0, false)) }
            } else {
                _uiState.update {
                    it.copy(
                        queue = queue,
                        totalExercises = queue.size,
                        showIntro = true,
                        timeBudgetSeconds = computeCheckpointTimeBudgetSeconds(queue.size)
                    )
                }
            }
        }
    }

    fun startCheckpoint() {
        val state = _uiState.value
        if (!state.showIntro) return
        _uiState.update {
            it.copy(
                showIntro = false,
                currentIndex = 1,
                currentExercise = decode(it.queue.first())
            )
        }
    }

    fun submitAnswer(userAnswer: String) {
        val current = _uiState.value
        if (current.isAnswered) return
        val exercise = current.currentExercise ?: return
        val queuedExercise = current.queue.first()
        val correct = gradeExerciseAnswer(exercise, userAnswer)
        answeredResults.add(queuedExercise to correct)

        _uiState.update { it.copy(isAnswered = true, isCorrect = correct, selectedAnswer = userAnswer) }

        pendingAnswerJob = viewModelScope.launch {
            submitAnswerUseCase(queuedExercise.id, quality = if (correct) 5 else 2, today = LocalDate.now())
            markUnitProgressUseCase(queuedExercise.unitId, LocalDate.now())
        }
    }

    fun nextExercise() {
        if (_uiState.value.isCompleting) return
        val remaining = _uiState.value.queue.drop(1)
        if (remaining.isEmpty()) {
            _uiState.update { it.copy(isCompleting = true) }
            viewModelScope.launch { finishCheckpoint() }
        } else {
            _uiState.update {
                it.copy(
                    queue = remaining,
                    currentIndex = it.currentIndex + 1,
                    currentExercise = decode(remaining.first()),
                    isAnswered = false,
                    isCorrect = false,
                    selectedAnswer = null
                )
            }
        }
    }

    private suspend fun finishCheckpoint() {
        pendingAnswerJob?.join()
        updateStreakUseCase(LocalDate.now())
        val correctCount = answeredResults.count { it.second }
        val failedExerciseIds = answeredResults.filter { !it.second }.map { it.first.id }
        val result = completeCheckpointUseCase(
            sectionId = sectionId,
            kind = CheckpointKind.REVIEW,
            correctCount = correctCount,
            totalCount = _uiState.value.totalExercises,
            today = LocalDate.now(),
            failedExerciseIds = failedExerciseIds
        )
        _uiState.update { it.copy(isComplete = true, result = result) }
    }

    private fun decode(exercise: Exercise): ExerciseContent =
        json.decodeFromString(ExerciseContent.serializer(), exercise.payload)
}
```

Note on the `failedExerciseIds` / timeout invariant (Global Constraints):
`answeredResults` is only appended to inside `submitAnswer`. Task 6's
timeout path calls `finishCheckpoint()` directly without ever calling
`submitAnswer` for the remaining queue — so unanswered items are counted
in `totalCount` (lowering `correctCount`'s share, i.e. scored as wrong)
but never appear in `failedExerciseIds`. This is exactly the dead-lock fix
the design doc requires, and it falls out of this structure for free.

- [ ] **Step 4: Add the intro and retry-locked views to `CheckpointScreen`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt`:

```kotlin
package com.zconte.oopsapp.ui.checkpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.ui.components.ExerciseAnswerCard
import com.zconte.oopsapp.ui.components.ExerciseAnswerState
import com.zconte.oopsapp.ui.theme.OopsTheme

@Composable
fun CheckpointScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckpointViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isRetryLocked) {
        RetryLockedView(onBack = onFinished, modifier = modifier)
        return
    }

    if (uiState.isComplete) {
        CheckpointResultView(result = uiState.result, onContinue = onFinished, modifier = modifier)
        return
    }

    if (uiState.showIntro) {
        CheckpointIntroView(
            questionCount = uiState.totalExercises,
            timeBudgetSeconds = uiState.timeBudgetSeconds,
            onStart = viewModel::startCheckpoint,
            modifier = modifier
        )
        return
    }

    val exercise = uiState.currentExercise
    if (exercise == null) {
        Text(
            "Cargando checkpoint...",
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    ExerciseAnswerCard(
        state = ExerciseAnswerState(
            exercise = exercise,
            currentIndex = uiState.currentIndex,
            totalExercises = uiState.totalExercises,
            isAnswered = uiState.isAnswered,
            isCorrect = uiState.isCorrect,
            selectedAnswer = uiState.selectedAnswer
        ),
        onSubmit = viewModel::submitAnswer,
        onNext = viewModel::nextExercise,
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp)
    )
}

@Composable
private fun RetryLockedView(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "Todavía no puedes reintentar",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Fallaste ejercicios que aún no has vuelto a responder. " +
                "En cuanto vuelvan a aparecerte en tu práctica diaria y los respondas, " +
                "podrás reintentar el checkpoint.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("VOLVER", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CheckpointIntroView(
    questionCount: Int,
    timeBudgetSeconds: Int,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = timeBudgetSeconds / 60
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "Checkpoint",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$questionCount preguntas · $minutes minutos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("COMENZAR", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CheckpointResultView(result: CheckpointResult?, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val passed = result?.passed == true
    val extended = OopsTheme.extendedColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = if (passed) "¡Checkpoint superado!" else "Casi lo logras",
            style = MaterialTheme.typography.headlineSmall,
            color = if (passed) extended.success else MaterialTheme.colorScheme.error
        )
        Text(
            text = "${result?.scorePct ?: 0}% (necesitas 68% para aprobar)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("CONTINUAR", style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

(Task 6 adds the timer's live countdown to `ExerciseAnswerCard`'s header;
Task 7 adds the section breakdown to `CheckpointResultView`. Neither is
in scope here.)

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.checkpoint.CheckpointViewModelTest"`
Expected: PASS (10 tests).

- [ ] **Step 6: Verify everything compiles**

Run: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt \
        app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt
git commit -m "feat: CheckpointViewModel gains a retry-locked state and an intro screen before starting"
```

---

### Task 6: Timer — injectable `Clock`, countdown, timeout auto-complete

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/util/Clock.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/util/SystemClock.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/testutil/FakeClock.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt`

**Interfaces:**
- Produces: `Clock.nowMillis(): Long`, `CheckpointViewModel.tick()`,
  `CheckpointUiState.timeRemainingSeconds`,
  `ExerciseAnswerState.timeRemainingLabel: String?` (consumed only by
  `CheckpointScreen`; `null` for `SessionScreen`/`PlacementCheckpointScreen`,
  zero behavior change there).

- [ ] **Step 1: Create the `Clock` abstraction**

Create `app/src/main/java/com/zconte/oopsapp/domain/util/Clock.kt`:

```kotlin
package com.zconte.oopsapp.domain.util

/** Seam for "now", so a countdown timer can be driven by a fake clock in JVM tests. */
interface Clock {
    fun nowMillis(): Long
}
```

Create `app/src/main/java/com/zconte/oopsapp/data/util/SystemClock.kt`:

```kotlin
package com.zconte.oopsapp.data.util

import com.zconte.oopsapp.domain.util.Clock
import javax.inject.Inject

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
```

Create `app/src/test/java/com/zconte/oopsapp/testutil/FakeClock.kt`:

```kotlin
package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.util.Clock

class FakeClock(private var millis: Long = 0L) : Clock {
    override fun nowMillis(): Long = millis
    fun advanceBy(deltaMillis: Long) {
        millis += deltaMillis
    }
}
```

In `app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt`, add the
binding (this module already binds every other interface-to-impl pair in
the project, so `Clock` belongs alongside them despite the file's name —
adding a 6th, unrelated `di/UtilModule.kt` purely for one binding is not
worth the extra file):

```kotlin
import com.zconte.oopsapp.data.util.SystemClock
import com.zconte.oopsapp.domain.util.Clock
```

and inside `abstract class RepositoryModule`:

```kotlin
    @Binds
    abstract fun bindClock(impl: SystemClock): Clock
```

- [ ] **Step 2: Write the failing timer tests**

Add these tests to `CheckpointViewModelTest.kt`, and update its
`buildViewModel` helper to accept a `Clock` (with a default, so none of
Task 5's existing tests need to change — they'll get `FakeClock()`
automatically). First, add these imports:

```kotlin
import com.zconte.oopsapp.domain.util.Clock
import com.zconte.oopsapp.testutil.FakeClock
```

Change `buildViewModel`'s signature and body to accept a `Clock`:

```kotlin
    private fun buildViewModel(
        sectionId: String,
        contentRepository: ContentRepository,
        exerciseRepository: ExerciseRepository,
        progressRepository: ProgressRepository,
        checkpointRepository: CheckpointRepository,
        clock: Clock = FakeClock()
    ): CheckpointViewModel = CheckpointViewModel(
        savedStateHandle = SavedStateHandle(mapOf("sectionId" to sectionId)),
        getCheckpointSessionUseCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository),
        isCheckpointRetryUnlockedUseCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository),
        submitAnswerUseCase = SubmitAnswerUseCase(exerciseRepository),
        completeCheckpointUseCase = CompleteCheckpointUseCase(checkpointRepository, contentRepository, exerciseRepository),
        updateStreakUseCase = UpdateStreakUseCase(progressRepository),
        markUnitProgressUseCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository),
        json = json,
        clock = clock
    )
```

Add these 3 new tests at the end of the class, right before the final
closing `}`:

```kotlin

    @Test
    fun `startCheckpoint sets the initial time remaining to the full budget`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        // GetCheckpointSessionUseCase (Plan 1) caps a lone first section's session at the size
        // floor (8), regardless of how many exercises are available -- so exactly 8 fixture
        // exercises here means the queue (and therefore the time budget) is fully predictable.
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf(
                "s1" to (1..8).map { exercise("ex-$it", "s1-u1") }
            )
        )
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository()
        )
        val budget = viewModel.uiState.value.timeBudgetSeconds

        viewModel.startCheckpoint()

        assertEquals(budget, viewModel.uiState.value.timeRemainingSeconds)
        assertEquals(840, budget) // 8 questions * 1.8 min = 14.4 -> rounds to 14 min = 840s, pinning the Plan 1 formula
    }

    @Test
    fun `tick counts down as the clock advances`() = runTest {
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf("s1" to listOf(exercise("ex-1", "s1-u1")))
        )
        val clock = FakeClock()
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = FakeCheckpointRepository(),
            clock = clock
        )
        viewModel.startCheckpoint()
        val budget = viewModel.uiState.value.timeBudgetSeconds

        clock.advanceBy(5_000L)
        viewModel.tick()

        assertEquals(budget - 5, viewModel.uiState.value.timeRemainingSeconds)
    }

    @Test
    fun `tick past the deadline auto-completes without marking the unanswered exercise as failed`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        val contentRepository = FakeContentRepository(sections = listOf(Section("s1", "s1", 1, "core")))
        val exerciseRepository = FakeExerciseRepository(
            exercisesBySection = mapOf(
                "s1" to listOf(exercise("ex-1", "s1-u1", answer = "42"), exercise("ex-2", "s1-u1", answer = "42"))
            )
        )
        val clock = FakeClock()
        val viewModel = buildViewModel(
            sectionId = "s1",
            contentRepository = contentRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = FakeProgressRepository(),
            checkpointRepository = checkpointRepository,
            clock = clock
        )
        viewModel.startCheckpoint()
        val budgetSeconds = viewModel.uiState.value.timeBudgetSeconds

        // Answer only the first of 2 exercises correctly; the second is never reached.
        viewModel.submitAnswer("42")
        clock.advanceBy(budgetSeconds * 1000L + 1_000L)
        viewModel.tick()

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertEquals(50, state.result?.scorePct) // 1 correct out of 2 total -- the unanswered one counts against the score
        assertTrue(checkpointRepository.recordedAttempts.first().failedExerciseIds.isEmpty()) // but NOT against the retry gate
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.checkpoint.CheckpointViewModelTest"`
Expected: FAIL — compile error, `CheckpointViewModel` doesn't take a
`clock` parameter yet, `tick()` and `timeRemainingSeconds` don't exist.

- [ ] **Step 4: Add the timer to `CheckpointViewModel`**

In `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`,
add this import:

```kotlin
import com.zconte.oopsapp.domain.util.Clock
```

Add `timeRemainingSeconds` to `CheckpointUiState`:

```kotlin
data class CheckpointUiState(
    val queue: List<Exercise> = emptyList(),
    val currentExercise: ExerciseContent? = null,
    val currentIndex: Int = 0,
    val totalExercises: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val result: CheckpointResult? = null,
    val isCompleting: Boolean = false,
    val isRetryLocked: Boolean = false,
    val showIntro: Boolean = false,
    val timeBudgetSeconds: Int = 0,
    val timeRemainingSeconds: Int = 0
)
```

Add a `clock: Clock` constructor parameter and a `savedStateHandle`
field (currently unnamed/unstored — the primary constructor parameter is
used inline for `sectionId` and discarded):

```kotlin
@HiltViewModel
class CheckpointViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCheckpointSessionUseCase: GetCheckpointSessionUseCase,
    private val isCheckpointRetryUnlockedUseCase: IsCheckpointRetryUnlockedUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val completeCheckpointUseCase: CompleteCheckpointUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val markUnitProgressUseCase: MarkUnitProgressUseCase,
    private val json: Json,
    private val clock: Clock
) : ViewModel() {
```

Add a top-level private constant right below the imports:

```kotlin
private const val CHECKPOINT_DEADLINE_KEY = "checkpointDeadlineMillis"
```

Replace `startCheckpoint()`:

```kotlin
    fun startCheckpoint() {
        val state = _uiState.value
        if (!state.showIntro) return
        val deadline = clock.nowMillis() + state.timeBudgetSeconds * 1000L
        savedStateHandle[CHECKPOINT_DEADLINE_KEY] = deadline
        _uiState.update {
            it.copy(
                showIntro = false,
                currentIndex = 1,
                currentExercise = decode(it.queue.first()),
                timeRemainingSeconds = state.timeBudgetSeconds
            )
        }
    }

    fun tick() {
        val state = _uiState.value
        if (state.showIntro || state.isComplete || state.isRetryLocked || state.isCompleting) return
        val deadline = savedStateHandle.get<Long>(CHECKPOINT_DEADLINE_KEY) ?: return
        val remainingMillis = deadline - clock.nowMillis()
        if (remainingMillis <= 0) {
            _uiState.update { it.copy(timeRemainingSeconds = 0, isCompleting = true) }
            viewModelScope.launch { finishCheckpoint() }
        } else {
            _uiState.update { it.copy(timeRemainingSeconds = (remainingMillis / 1000).toInt()) }
        }
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.checkpoint.CheckpointViewModelTest"`
Expected: PASS (13 tests).

- [ ] **Step 6: Show the countdown in the UI**

In `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`,
add `timeRemainingLabel` to `ExerciseAnswerState`:

```kotlin
data class ExerciseAnswerState(
    val exercise: ExerciseContent,
    val currentIndex: Int,
    val totalExercises: Int,
    val isAnswered: Boolean,
    val isCorrect: Boolean,
    val selectedAnswer: String?,
    val timeRemainingLabel: String? = null
)
```

Find the header `Text` that renders "n/total", currently:

```kotlin
            Text(
                text = "${state.currentIndex}/${state.totalExercises}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
```

Replace it with:

```kotlin
            Text(
                text = if (state.timeRemainingLabel != null) {
                    "${state.currentIndex}/${state.totalExercises} · ${state.timeRemainingLabel}"
                } else {
                    "${state.currentIndex}/${state.totalExercises}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
```

In `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt`,
add a private formatting helper and pass the label:

```kotlin
private fun formatRemaining(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
```

and in the `ExerciseAnswerCard(...)` call inside `CheckpointScreen`, add:

```kotlin
            timeRemainingLabel = formatRemaining(uiState.timeRemainingSeconds)
```

as the last named argument of the `ExerciseAnswerState(...)` construction.

Add these two imports to `CheckpointScreen.kt`:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
```

Add a ticking effect right after `val uiState by viewModel.uiState...` in
`CheckpointScreen`:

```kotlin
    LaunchedEffect(uiState.showIntro, uiState.isComplete, uiState.isRetryLocked) {
        if (uiState.showIntro || uiState.isComplete || uiState.isRetryLocked) return@LaunchedEffect
        while (true) {
            delay(1000)
            viewModel.tick()
        }
    }
```

- [ ] **Step 7: Verify everything compiles**

Run: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/util/Clock.kt \
        app/src/main/java/com/zconte/oopsapp/data/util/SystemClock.kt \
        app/src/test/java/com/zconte/oopsapp/testutil/FakeClock.kt \
        app/src/main/java/com/zconte/oopsapp/di/RepositoryModule.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt \
        app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt \
        app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt
git commit -m "feat: add a testable timer to the checkpoint, with safe timeout auto-complete"
```

---

### Task 7: Results breakdown on failure

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointResultBreakdownUseCase.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointResultBreakdownUseCaseTest.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt`

**Interfaces:**
- Consumes: `computeCheckpointSectionBreakdown` (Plan 1, dormant until now).
- Produces: `CheckpointUiState.sectionBreakdown: List<CheckpointSectionBreakdown>`,
  shown in `CheckpointResultView` only when `!result.passed`.

- [ ] **Step 1: Write the failing test for the wrapper use case**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointResultBreakdownUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.model.CompletedUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForBreakdown(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = emptyList()
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetCheckpointResultBreakdownUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "fill_blank", "{}", 1)

    @Test
    fun `resolves each exercise's section and tallies correctness per section`() = runTest {
        val contentRepository = FakeContentRepositoryForBreakdown(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            )
        )
        val useCase = GetCheckpointResultBreakdownUseCase(contentRepository)
        val results = listOf(
            exercise("ex-1", "s1-u1") to true,
            exercise("ex-2", "s1-u1") to false,
            exercise("ex-3", "s2-u1") to true
        )

        val breakdown = useCase(results)

        assertEquals(2, breakdown.size)
        assertEquals("s1", breakdown[0].section.id)
        assertEquals(1, breakdown[0].correct)
        assertEquals(2, breakdown[0].total)
        assertEquals("s2", breakdown[1].section.id)
        assertEquals(1, breakdown[1].correct)
        assertEquals(1, breakdown[1].total)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetCheckpointResultBreakdownUseCaseTest"`
Expected: FAIL — compile error, `GetCheckpointResultBreakdownUseCase`
doesn't exist.

- [ ] **Step 3: Create the wrapper use case**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointResultBreakdownUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

/**
 * Resolves the section each answered exercise belongs to (via its unit) and hands off to the
 * pure [computeCheckpointSectionBreakdown]. Kept separate from that pure function so the
 * repository lookups it needs stay out of `CheckpointViewModel` and out of the pure-function
 * unit tests Plan 1 already wrote for the tally logic itself.
 */
class GetCheckpointResultBreakdownUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(results: List<Pair<Exercise, Boolean>>): List<CheckpointSectionBreakdown> {
        val sections = contentRepository.getSections()
        val sectionsById = sections.associateBy { it.id }
        val unitsById = sections.flatMap { contentRepository.getUnitsBySection(it.id) }.associateBy { it.id }
        return computeCheckpointSectionBreakdown(results, unitsById, sectionsById)
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetCheckpointResultBreakdownUseCaseTest"`
Expected: PASS (1 test).

- [ ] **Step 5: Write the failing ViewModel test**

Add this import to `CheckpointViewModelTest.kt`:

```kotlin
import com.zconte.oopsapp.domain.usecase.GetCheckpointResultBreakdownUseCase
```

The `CheckpointViewModel(...)` call inside `buildViewModel` currently ends
with `json = json,` then `clock = clock` as its final line (no trailing
comma, per Task 6). Add a comma to the `clock = clock` line and one new
line after it:

```kotlin
        json = json,
        clock = clock,
        getCheckpointResultBreakdownUseCase = GetCheckpointResultBreakdownUseCase(contentRepository)
    )
```

Add this test at the end of the class, right before the final closing `}`:

```kotlin

    @Test
    fun `a failed checkpoint includes a per-section breakdown; a passed one does not`() = runTest {
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
            progressRepository = FakeProgressRepository(),
            checkpointRepository = checkpointRepository
        )
        viewModel.startCheckpoint()

        viewModel.submitAnswer("wrong")
        viewModel.nextExercise()
        viewModel.submitAnswer("wrong")
        viewModel.nextExercise()

        val state = viewModel.uiState.value
        assertFalse(state.result?.passed ?: true)
        assertEquals(1, state.sectionBreakdown.size)
        assertEquals("s1", state.sectionBreakdown.first().section.id)
        assertEquals(0, state.sectionBreakdown.first().correct)
        assertEquals(2, state.sectionBreakdown.first().total)
    }
```

- [ ] **Step 6: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.checkpoint.CheckpointViewModelTest"`
Expected: FAIL — compile error, `CheckpointViewModel` doesn't take a
`getCheckpointResultBreakdownUseCase` parameter yet, `sectionBreakdown`
doesn't exist on `CheckpointUiState`.

- [ ] **Step 7: Wire the breakdown into `CheckpointViewModel`**

In `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`,
add this import:

```kotlin
import com.zconte.oopsapp.domain.usecase.CheckpointSectionBreakdown
import com.zconte.oopsapp.domain.usecase.GetCheckpointResultBreakdownUseCase
```

Add `sectionBreakdown` to `CheckpointUiState`:

```kotlin
    val sectionBreakdown: List<CheckpointSectionBreakdown> = emptyList()
```

The constructor's last line is currently `private val clock: Clock` (added
in Task 6), with no trailing comma. Add a comma to it and a new
constructor parameter right after it:

```kotlin
    private val clock: Clock,
    private val getCheckpointResultBreakdownUseCase: GetCheckpointResultBreakdownUseCase
```

Replace `finishCheckpoint()`:

```kotlin
    private suspend fun finishCheckpoint() {
        pendingAnswerJob?.join()
        updateStreakUseCase(LocalDate.now())
        val correctCount = answeredResults.count { it.second }
        val failedExerciseIds = answeredResults.filter { !it.second }.map { it.first.id }
        val result = completeCheckpointUseCase(
            sectionId = sectionId,
            kind = CheckpointKind.REVIEW,
            correctCount = correctCount,
            totalCount = _uiState.value.totalExercises,
            today = LocalDate.now(),
            failedExerciseIds = failedExerciseIds
        )
        val breakdown = if (!result.passed) getCheckpointResultBreakdownUseCase(answeredResults) else emptyList()
        _uiState.update { it.copy(isComplete = true, result = result, sectionBreakdown = breakdown) }
    }
```

- [ ] **Step 8: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.ui.checkpoint.CheckpointViewModelTest"`
Expected: PASS (14 tests).

- [ ] **Step 9: Show the breakdown in `CheckpointResultView`**

In `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt`,
add this import (`fillMaxWidth` is already imported since Task 5's
rewrite of this file — do not add it again):

```kotlin
import com.zconte.oopsapp.domain.usecase.CheckpointSectionBreakdown
```

Change the `CheckpointScreen` function's call to `CheckpointResultView` to
pass the breakdown:

```kotlin
    if (uiState.isComplete) {
        CheckpointResultView(
            result = uiState.result,
            sectionBreakdown = uiState.sectionBreakdown,
            onContinue = onFinished,
            modifier = modifier
        )
        return
    }
```

Replace `CheckpointResultView`:

```kotlin
@Composable
private fun CheckpointResultView(
    result: CheckpointResult?,
    sectionBreakdown: List<CheckpointSectionBreakdown>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passed = result?.passed == true
    val extended = OopsTheme.extendedColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = if (passed) "¡Checkpoint superado!" else "Casi lo logras",
            style = MaterialTheme.typography.headlineSmall,
            color = if (passed) extended.success else MaterialTheme.colorScheme.error
        )
        Text(
            text = "${result?.scorePct ?: 0}% (necesitas 68% para aprobar)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!passed && sectionBreakdown.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sectionBreakdown.forEach { item ->
                    Text(
                        text = "${item.section.name}: ${item.correct}/${item.total} (${(item.correct * 100) / item.total}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("CONTINUAR", style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

- [ ] **Step 10: Verify everything compiles**

Run: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 12: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointResultBreakdownUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointResultBreakdownUseCaseTest.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointScreen.kt \
        app/src/test/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModelTest.kt
git commit -m "feat: show a per-section results breakdown when the checkpoint is failed"
```

---

## After all tasks: what changes on-device

Once all 7 tasks are merged, on a device already running Plan 1:

- The checkpoint gains an intro screen ("N preguntas · M minutos ·
  COMENZAR") and, once started, a live countdown next to the question
  counter. Running out of time auto-submits and scores unanswered
  questions as wrong, without blocking a future retry over them.
- Failing a checkpoint shows a per-section breakdown of what was weak.
- Retrying a checkpoint before re-studying the failed exercises in daily
  practice shows an explanation screen instead of the questions.
- "TU RUTA" on Home correctly shows "Checkpoint pendiente" instead of a
  stale percentage once a section's units are done.
- "ESTUDIAR HOY" never opens an empty session that silently bounces back —
  it goes to the pending checkpoint instead, if there is one.
- Ver Ruta's checkpoint row shows one of 3 states (obligatorio / repasa lo
  fallado / reinténtalo ahora) and disappears once approved.

No QA checklist beyond what's already in each task's steps is required
for the individual tasks; a full on-device pass (clean install optional —
no schema changed) exercising all of the above is Luis's job once this
merges, same as Plan 1.
