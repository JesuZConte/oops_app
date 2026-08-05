# Checkpoint-Satisfied Permanence Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix a real bug found during on-device QA: once a section's review
checkpoint is approved, it must stay satisfied forever, even if new units
are later added to that section by a content-authoring cycle (as every
retrofit sub-cycle in this project's active roadmap does). Currently,
`checkpointSatisfied` is recomputed from scratch on every call and requires
`sectionComplete` (ALL current units done) — so adding even one new,
not-yet-played unit to an already-approved section silently un-satisfies
its checkpoint, which cascades forward and locks not-yet-completed units
in every *later* section too, even ones completely unrelated to the new
content. Confirmed live: Luis approved the "Fundamentos de Java" checkpoint
when it had 3 units; after 3 content sub-cycles grew it to 14 units, the
brand-new "Arrays" unit in the downstream "Genericos y Colecciones"
section showed locked despite its own section being otherwise fully
played, because `GetLearningPathUseCase` recomputed Fundamentos'
`sectionComplete` as false and discarded the historical approved
checkpoint attempt.

**Architecture:** One-line logic change in
`GetLearningPathUseCase.kt` plus a new regression test. No Room schema
change, no migration, no UI changes — `checkpointRepository.hasApprovedAttempt`
already queries `checkpoint_attempts` directly and is completely
independent of current unit/section state; the bug is purely in how its
result gets combined with `sectionComplete`.

**Tech Stack:** Kotlin, JUnit4, existing hand-written fake pattern
(`FakeContentRepositoryForPath`, `FakeCheckpointRepository`) already used
throughout `GetLearningPathUseCaseTest.kt`.

## Global Constraints

- Do not change `CheckpointRepository`, `hasApprovedAttempt`'s signature,
  or any DAO/entity — this is a pure domain-logic fix in one use case.
- Do not touch the placement-completion branch's semantics: a section
  fully completed via placement (no explicit checkpoint attempt) must
  still require **current** full placement completion to satisfy the
  gate — that branch inherently depends on today's unit state, unlike an
  historical approved attempt, and must keep requiring `sectionComplete`.
- Every existing test in `GetLearningPathUseCaseTest.kt` must continue to
  pass unchanged — this is a targeted fix, not a semantics rewrite of the
  other branches.

---

### Task 1: Make an approved checkpoint attempt permanently satisfy the gate

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`

**Interfaces:**
- Consumes: `CheckpointRepository.hasApprovedAttempt(sectionId, kind): Boolean`
  (unchanged signature, already used).
- Produces: nothing new — `GetLearningPathUseCase`'s public `invoke()`
  signature and `SectionPath`/`UnitProgress` model shapes are unchanged.

- [ ] **Step 1: Change the `checkpointSatisfied` computation**

In `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`,
find:

```kotlin
            val sectionComplete = units.isNotEmpty() && units.all { it.id in completedUnits }
            val checkpointSatisfied = sectionComplete && (
                checkpointRepository.hasApprovedAttempt(section.id, CheckpointKind.REVIEW) ||
                    unitProgress.all { it.completedVia == UnitCompletionSource.PLACEMENT }
                )
```

Replace with:

```kotlin
            val sectionComplete = units.isNotEmpty() && units.all { it.id in completedUnits }
            // An approved checkpoint attempt is a permanent record: once earned, it stays
            // satisfied even if later content-authoring adds new, not-yet-played units to
            // this section (which would otherwise flip sectionComplete back to false and
            // cascade-lock every downstream section's unfinished units). Full placement
            // completion is NOT a permanent record in the same way — it reflects today's
            // unit state, so it still requires sectionComplete.
            val checkpointSatisfied = checkpointRepository.hasApprovedAttempt(section.id, CheckpointKind.REVIEW) ||
                (sectionComplete && unitProgress.all { it.completedVia == UnitCompletionSource.PLACEMENT })
```

- [ ] **Step 2: Add a regression test for the exact bug scenario**

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`,
add this test (anywhere among the other `checkpointSatisfied`-related tests,
e.g. right after `a section unlocks once every unit is complete AND its
checkpoint is approved`):

```kotlin
    @Test
    fun `an approved checkpoint stays satisfied even after new units are added to the section later`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 100, passed = true,
            takenAt = LocalDate.of(2026, 7, 20), failedExerciseIds = emptyList()
        )
        val useCase = GetLearningPathUseCase(repository, checkpointRepository, retryUnlockedUseCase(checkpointRepository))

        val path = useCase()

        assertFalse(path.first().completed)
        assertTrue(path.first().checkpointSatisfied)
        assertTrue(path[1].unlocked)
    }
```

This test models exactly the confirmed live scenario: `s1` (like
Fundamentos) has a checkpoint approved while it only had 1 unit
(`s1-u1`); later, a second unit (`s1-u2`, unplayed) is added — `s1` is no
longer "complete", but its checkpoint must stay satisfied, and `s2` (like
Genericos y Colecciones) must stay unlocked.

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, including the new test and all 12 pre-existing
tests in `GetLearningPathUseCaseTest.kt` still passing unchanged.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt
git commit -m "fix: an approved section checkpoint stays satisfied after new units are added later"
```

---

## After the task: manual on-device QA

Install a clean/in-place build and manually verify on-device:

1. Reopen Ruta and navigate to "Genericos y Colecciones": confirm "Arrays"
   now shows "Toca para jugar" (unlocked) instead of the lock icon —
   this is the exact bug this fix closes.
2. Confirm "Fundamentos de Java" still shows its own units/lock states
   unchanged (this fix does not touch unit-level unlock within a section,
   only cross-section/checkpoint gating).
3. Play the "Arrays" unit to confirm it's now actually playable end to
   end (this was blocked by the bug before this fix).
