# Daily Review Cap and Prioritization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix `GetTodaySessionUseCase`'s uncapped SRS review phase (which dumped 267 overdue questions in one "ESTUDIAR HOY" session) by capping it at 10 items, ordering the pool by SM-2 weakness, and reserving exactly 1 randomly-chosen slot for a 45+-day-stale item so nothing is forgotten forever.

**Architecture:** Push ordering into SQL (`ExerciseDao.getDue` gains an `ORDER BY`), add a new `ExerciseDao.getStale`/`ExerciseRepository.getStaleExercises` query for the aging pool, and add capping + the 1-slot lottery + presentation shuffle to `GetTodaySessionUseCase`, which gains an injectable `Random` for deterministic testing (same DI pattern this codebase already uses for `today: LocalDate`).

**Tech Stack:** Kotlin, Room, Hilt, JUnit4, kotlinx-coroutines-test. Pure engine fix — no content JSON, no `CURRENT_CONTENT_VERSION` change.

## Global Constraints

- `ExerciseDao.getDue`'s `ORDER BY` is exactly: `review_state.repetitions ASC, review_state.lastReviewedAt DESC, review_state.easeFactor ASC, exercises.id ASC` (in that order). No change to its `WHERE` clause or signature.
- New `ExerciseDao.getStale(cutoff: Long): List<ExerciseEntity>` — same `INNER JOIN` as `getDue`, `WHERE review_state.dueDate <= :cutoff`, no `ORDER BY`.
- New `ExerciseRepository.getStaleExercises(cutoff: LocalDate): List<Exercise>`, implemented in `ExerciseRepositoryImpl` by delegating to `exerciseDao.getStale(cutoff.toEpochDay())`. `getDueExercises(today, limit)` keeps its exact current signature and behavior.
- `GetTodaySessionUseCase`'s new signature is exactly:
  ```kotlin
  suspend operator fun invoke(
      today: LocalDate,
      newExercisesLimit: Int = 5,
      dueExercisesLimit: Int = 10,
      staleThresholdDays: Long = 45,
      random: Random = Random.Default
  ): List<Exercise>
  ```
- Selection order inside Phase B matters: **take the weakest N before shuffling**, never shuffle-then-take — shuffling only reorders presentation, it must never change which items get selected (this was an explicit, non-negotiable decision in the spec).
- No new table, no new column — no Room schema/migration version bump. `AppDatabase` stays at version 4.
- `GetCheckpointSessionUseCase` is **not modified in this plan**. It keeps calling `getDueExercises(today, Int.MAX_VALUE)` for its due-id `Set` — unaffected by the new `ORDER BY` (order-independent) and untouched by `getStaleExercises`. Verified this is the *only* other caller: `grep -rn "getDueExercises" app/src/main` returns exactly 2 call sites — `GetTodaySessionUseCase` (this plan's Task 3) and `GetCheckpointSessionUseCase`. `IsCheckpointRetryUnlockedUseCase`, `CompleteCheckpointUseCase`, and `GetUnitReviewSessionUseCase` all take `ExerciseRepository` as a constructor dependency but never call `getDueExercises` — the new `ORDER BY` cannot reach them.
- `CURRENT_CONTENT_VERSION` is **not touched**.
- Full spec: `docs/superpowers/specs/2026-08-20-daily-review-cap-prioritization-design.md` (committed `9d690bb`).

---

### Task 1: `ExerciseDao` — weakness `ORDER BY` on `getDue` + new `getStale` query

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`
- Create: `app/src/androidTest/java/com/zconte/oopsapp/data/local/dao/ExerciseDaoTest.kt`

**Interfaces:**
- Produces: `ExerciseDao.getDue(today: Long): List<ExerciseEntity>` (existing signature, now ordered) and `ExerciseDao.getStale(cutoff: Long): List<ExerciseEntity>` (new) — both consumed by Task 2's `ExerciseRepositoryImpl`.

This is pure SQL behavior — Room validates column names at compile time but not `ORDER BY` semantics, and this project has no JVM-level Room test harness (no Robolectric). It does have working **instrumented** Room test infrastructure already (`app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt` uses `Room.databaseBuilder(...).allowMainThreadQueries()` under `@RunWith(AndroidJUnit4::class)`). This task follows that exact precedent with `Room.inMemoryDatabaseBuilder` instead (no migration involved, just a fresh schema). Running it requires a connected device or emulator — same requirement `MigrationTest` already has.

- [ ] **Step 1: Write the failing instrumented test**

Create `app/src/androidTest/java/com/zconte/oopsapp/data/local/dao/ExerciseDaoTest.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zconte.oopsapp.data.local.AppDatabase
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exerciseDao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        unitId = "u1",
        type = "fill_blank",
        payload = "{}",
        difficulty = 1,
        examVersion = "core"
    )

    private fun reviewState(
        exerciseId: String,
        repetitions: Int,
        easeFactor: Double,
        dueDate: Long,
        lastReviewedAt: Long
    ) = ReviewStateEntity(
        exerciseId = exerciseId,
        easeFactor = easeFactor,
        intervalDays = 1,
        repetitions = repetitions,
        dueDate = dueDate,
        lastReviewedAt = lastReviewedAt
    )

    @Test
    fun getDue_ordersByRepetitionsAscendingFirst() = runBlocking {
        exerciseDao.insertAll(listOf(exercise("weak"), exercise("strong")))
        db.reviewStateDao().upsert(reviewState("strong", repetitions = 5, easeFactor = 2.5, dueDate = 100, lastReviewedAt = 50))
        db.reviewStateDao().upsert(reviewState("weak", repetitions = 0, easeFactor = 2.5, dueDate = 100, lastReviewedAt = 50))

        val result = exerciseDao.getDue(today = 100)

        assertEquals(listOf("weak", "strong"), result.map { it.id })
    }

    @Test
    fun getDue_withinSameRepetitionsBucket_mostRecentlyReviewedWinsOverEaseFactor() = runBlocking {
        // Both repetitions = 1 (a fresh pass). "fresh-yesterday" was just reviewed and has a
        // slightly worse-ranking easeFactor than "old-qa-backlog", which hasn't been touched in
        // weeks. Without the lastReviewedAt tiebreaker, easeFactor ASC alone would put
        // "old-qa-backlog" first -- burying next-day reinforcement of newly taught content.
        // This is the bug an advisor review caught before the spec was committed.
        exerciseDao.insertAll(listOf(exercise("fresh-yesterday"), exercise("old-qa-backlog")))
        db.reviewStateDao().upsert(
            reviewState("fresh-yesterday", repetitions = 1, easeFactor = 2.6, dueDate = 100, lastReviewedAt = 99)
        )
        db.reviewStateDao().upsert(
            reviewState("old-qa-backlog", repetitions = 1, easeFactor = 2.5, dueDate = 100, lastReviewedAt = 40)
        )

        val result = exerciseDao.getDue(today = 100)

        assertEquals(listOf("fresh-yesterday", "old-qa-backlog"), result.map { it.id })
    }

    @Test
    fun getDue_withinSameRepetitionsAndLastReviewedAt_easeFactorAscendingBreaksTie() = runBlocking {
        exerciseDao.insertAll(listOf(exercise("harder"), exercise("easier")))
        db.reviewStateDao().upsert(reviewState("easier", repetitions = 2, easeFactor = 2.8, dueDate = 100, lastReviewedAt = 50))
        db.reviewStateDao().upsert(reviewState("harder", repetitions = 2, easeFactor = 1.5, dueDate = 100, lastReviewedAt = 50))

        val result = exerciseDao.getDue(today = 100)

        assertEquals(listOf("harder", "easier"), result.map { it.id })
    }

    @Test
    fun getStale_returnsOnlyItemsDueAtOrBeforeCutoff() = runBlocking {
        exerciseDao.insertAll(listOf(exercise("very-old"), exercise("recent")))
        db.reviewStateDao().upsert(reviewState("very-old", repetitions = 3, easeFactor = 2.5, dueDate = 50, lastReviewedAt = 40))
        db.reviewStateDao().upsert(reviewState("recent", repetitions = 3, easeFactor = 2.5, dueDate = 95, lastReviewedAt = 90))

        val result = exerciseDao.getStale(cutoff = 55)

        assertEquals(listOf("very-old"), result.map { it.id })
    }

    @Test
    fun getStale_includesItemsExactlyAtCutoff() = runBlocking {
        exerciseDao.insertAll(listOf(exercise("exact")))
        db.reviewStateDao().upsert(reviewState("exact", repetitions = 3, easeFactor = 2.5, dueDate = 55, lastReviewedAt = 40))

        val result = exerciseDao.getStale(cutoff = 55)

        assertEquals(listOf("exact"), result.map { it.id })
    }
}
```

- [ ] **Step 2: Run it to confirm it fails to compile**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: FAIL — `getStale` is not a member of `ExerciseDao` (it doesn't exist yet). This is the RED state for this task: adding a new DAO method is a compile-time contract change, so the failing signal here is a compile error rather than a runtime assertion failure.

- [ ] **Step 3: Implement — add the `ORDER BY` and the new query**

Replace the whole file `app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.ExerciseEntity

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises")
    suspend fun clearAll()

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN review_state ON exercises.id = review_state.exerciseId
        WHERE review_state.dueDate <= :today
        ORDER BY review_state.repetitions ASC, review_state.lastReviewedAt DESC,
            review_state.easeFactor ASC, exercises.id ASC
        """
    )
    suspend fun getDue(today: Long): List<ExerciseEntity>

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN review_state ON exercises.id = review_state.exerciseId
        WHERE review_state.dueDate <= :cutoff
        """
    )
    suspend fun getStale(cutoff: Long): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE unitId = :unitId")
    suspend fun getByUnit(unitId: String): List<ExerciseEntity>

    @Query(
        """
        SELECT exercises.* FROM exercises
        INNER JOIN units ON exercises.unitId = units.id
        WHERE units.sectionId = :sectionId
        """
    )
    suspend fun getBySection(sectionId: String): List<ExerciseEntity>
}
```

- [ ] **Step 4: Run the instrumented test suite**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: BUILD SUCCESSFUL, including `ExerciseDaoTest`'s 5 new tests and the pre-existing `MigrationTest`/`ExampleInstrumentedTest`. Requires a connected device or running emulator.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/local/dao/ExerciseDao.kt \
        app/src/androidTest/java/com/zconte/oopsapp/data/local/dao/ExerciseDaoTest.kt
git commit -m "feat: order getDue by SM-2 weakness, add getStale for the aging lottery"
```

---

### Task 2: `ExerciseRepository`/`ExerciseRepositoryImpl` — `getStaleExercises`, and keep every fake compiling

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`
- Create: `app/src/test/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImplTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/testutil/FakeExerciseRepository.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitReviewSessionUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`

**Interfaces:**
- Consumes: `ExerciseDao.getStale(cutoff: Long): List<ExerciseEntity>` (Task 1).
- Produces: `ExerciseRepository.getStaleExercises(cutoff: LocalDate): List<Exercise>` — consumed by Task 3's `GetTodaySessionUseCase`, and by `FakeExerciseRepositoryForSession` (this task's Step 4), which Task 3 extends with test cases but does not need to touch the fake's shape again.

`ExerciseRepository` is implemented by 10 classes in this codebase: `ExerciseRepositoryImpl` (production) plus 9 hand-rolled test fakes (`FakeExerciseRepository` in `testutil`, and one private fake each in the 8 use-case test files listed above, including `GetTodaySessionUseCaseTest.kt`). Adding a method to the interface breaks compilation everywhere until every one of them gets it — Kotlin does not allow a concrete class to leave an interface member unimplemented, so this cannot be split across two tasks without leaving the test source set red in between. `GetTodaySessionUseCaseTest.kt`'s fake (`FakeExerciseRepositoryForSession`) gets the one **real** implementation (configurable `stale` list + cutoff capture, since Task 3's tests need it); the other 8 get a one-line stub, since none of their tests exercise `GetTodaySessionUseCase`.

- [ ] **Step 1: Write the failing repository test**

Create `app/src/test/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImplTest.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseDaoForRepository(
    private val due: List<ExerciseEntity> = emptyList(),
    private val stale: List<ExerciseEntity> = emptyList()
) : ExerciseDao {
    var lastStaleCutoff: Long? = null

    override suspend fun insertAll(exercises: List<ExerciseEntity>) {}
    override suspend fun clearAll() {}
    override suspend fun getDue(today: Long): List<ExerciseEntity> = due
    override suspend fun getStale(cutoff: Long): List<ExerciseEntity> {
        lastStaleCutoff = cutoff
        return stale
    }
    override suspend fun getByUnit(unitId: String): List<ExerciseEntity> = emptyList()
    override suspend fun getBySection(sectionId: String): List<ExerciseEntity> = emptyList()
}

private class NoOpReviewStateDao : ReviewStateDao {
    override suspend fun upsert(state: ReviewStateEntity) {}
    override suspend fun getByExerciseId(exerciseId: String): ReviewStateEntity? = null
    override suspend fun getExistingIds(exerciseIds: List<String>): List<String> = emptyList()
}

class ExerciseRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun entity(id: String) = ExerciseEntity(
        id = id,
        unitId = "u1",
        type = "fill_blank",
        payload = """{"id":"$id","type":"fill_blank","difficulty":1,"prompt":"p","answer":"a","explanation":"e"}""",
        difficulty = 1,
        examVersion = "core"
    )

    @Test
    fun `getStaleExercises delegates to the DAO with the cutoff converted to epoch day`() = runTest {
        val exerciseDao = FakeExerciseDaoForRepository(stale = listOf(entity("stale-1")))
        val repository = ExerciseRepositoryImpl(exerciseDao, NoOpReviewStateDao(), json)

        val cutoff = LocalDate.of(2026, 7, 1)
        val result = repository.getStaleExercises(cutoff)

        assertEquals(listOf("stale-1"), result.map { it.id })
        assertEquals(cutoff.toEpochDay(), exerciseDao.lastStaleCutoff)
    }

    @Test
    fun `getDueExercises still delegates to getDue and respects the limit, unchanged`() = runTest {
        val exerciseDao = FakeExerciseDaoForRepository(due = listOf(entity("due-1"), entity("due-2")))
        val repository = ExerciseRepositoryImpl(exerciseDao, NoOpReviewStateDao(), json)

        val result = repository.getDueExercises(LocalDate.of(2026, 7, 1), limit = 1)

        assertEquals(listOf("due-1"), result.map { it.id })
    }
}
```

- [ ] **Step 2: Run it to confirm it fails to compile**

Run: `./gradlew :app:compileDebugUnitTestKotlin`
Expected: FAIL — `getStaleExercises` is unresolved on `ExerciseRepositoryImpl`/`ExerciseRepository` (neither has it yet; Task 1 already added `getStale` to `ExerciseDao`, so `FakeExerciseDaoForRepository` itself compiles fine). At this point the interface hasn't changed yet, so this is the only error — the cascading "class X does not implement member" errors from the other 8 fakes only appear once Step 3 adds the method to the interface, and are fixed in Step 4.

- [ ] **Step 3: Add the method to the interface and implement it**

In `app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt`, replace the whole file:

```kotlin
package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import java.time.LocalDate

interface ExerciseRepository {
    suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise>
    suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise>
    suspend fun getExercisesByUnit(unitId: String): List<Exercise>
    suspend fun getExercisesBySection(sectionId: String): List<Exercise>
    suspend fun getReviewState(exerciseId: String): ReviewState?
    suspend fun saveReviewState(state: ReviewState)
    suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String>
}
```

In `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`, add the implementation right after `getDueExercises`:

```kotlin
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> =
        exerciseDao.getDue(today.toEpochDay()).take(limit).map { it.toDomain(json) }

    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> =
        exerciseDao.getStale(cutoff.toEpochDay()).map { it.toDomain(json) }
```

- [ ] **Step 4: Add a no-op override to every other fake so the module compiles**

Each of these 8 files gets one new override added to its private/public fake class (alongside its existing `getDueExercises` override). None of these fakes need real stale-exercise behavior — their tests don't exercise `GetTodaySessionUseCase`.

`app/src/test/java/com/zconte/oopsapp/testutil/FakeExerciseRepository.kt` — add to `FakeExerciseRepository`:
```kotlin
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
```

`app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt` — add to `FakeExerciseRepositoryForAnswer`:
```kotlin
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
```

`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitReviewSessionUseCaseTest.kt` — add to `FakeExerciseRepositoryForUnitReview`:
```kotlin
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
```

`app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt` — add to `FakeExerciseRepositoryForComplete`:
```kotlin
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
```

`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt` — add to `FakeExerciseRepositoryForCheckpoint`:
```kotlin
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
```

`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt` — add to `FakeExerciseRepositoryForPlacementSession`:
```kotlin
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
```

`app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt` — add to `FakeExerciseRepositoryForUnitProgress`:
```kotlin
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
```

`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt` — add to `FakeExerciseRepositoryForUnitSession`:
```kotlin
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> = emptyList()
```

`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt` — this fake needs real, configurable behavior (Task 3's tests drive it), not a stub. Replace `FakeExerciseRepositoryForSession` in full:

```kotlin
private class FakeExerciseRepositoryForSession(
    private val due: List<Exercise> = emptyList(),
    private val exercisesByUnit: Map<String, List<Exercise>> = emptyMap(),
    private val answeredIds: Set<String> = emptySet(),
    private val stale: List<Exercise> = emptyList()
) : ExerciseRepository {
    val savedStates = mutableListOf<ReviewState>()
    var lastStaleCutoff: LocalDate? = null

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
    override suspend fun getStaleExercises(cutoff: LocalDate): List<Exercise> {
        lastStaleCutoff = cutoff
        return stale
    }
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
```

- [ ] **Step 5: Run the unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — the whole module (all 10 `ExerciseRepository` implementors) compiles again and every existing test still passes, plus the 2 new `ExerciseRepositoryImplTest` tests. This is the task's real acceptance gate — unlike a version of this task that left `FakeExerciseRepositoryForSession` unimplemented, there is no known-red state left over for Task 3 to inherit.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/repository/ExerciseRepository.kt \
        app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt \
        app/src/test/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImplTest.kt \
        app/src/test/java/com/zconte/oopsapp/testutil/FakeExerciseRepository.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitReviewSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt
git commit -m "feat: add ExerciseRepository.getStaleExercises"
```

---

### Task 3: `GetTodaySessionUseCase` — cap, weakness selection, aging lottery, presentation shuffle

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`

**Interfaces:**
- Consumes: `ExerciseRepository.getStaleExercises(cutoff: LocalDate): List<Exercise>` (Task 2, including the real `FakeExerciseRepositoryForSession` support added in that task's Step 4), `ExerciseRepository.getDueExercises(today: LocalDate, limit: Int): List<Exercise>` (existing, now weakness-ordered by Task 1's `ORDER BY` — this use case trusts that ordering, it does not re-sort).
- Produces: `GetTodaySessionUseCase.invoke(today, newExercisesLimit, dueExercisesLimit, staleThresholdDays, random): List<Exercise>` — this is the plan's final consumer-facing surface; nothing later depends on it.

The randomness in these tests is genuinely deterministic: every seeded expected value below was produced by actually running the exact call sequence (`staleCandidates.shuffled(random).firstOrNull()` first, then later `combined.shuffled(random)` on the **same, now-advanced** `random` instance — not two independent `Random(seed)` instances) against this project's real Kotlin stdlib, via a throwaway test executed and then deleted. These are not hand-guessed values.

`FakeExerciseRepositoryForSession` already has its `stale`/`getStaleExercises`/`lastStaleCutoff` support from Task 2 Step 4 — this task only adds imports and test cases to the file, it does not touch the fake's shape again.

- [ ] **Step 1: Add imports needed for the new tests**

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`, add these imports alongside the existing ones at the top of the file (the file currently only imports `org.junit.Assert.assertEquals`, not `assertTrue`):

```kotlin
import kotlin.random.Random
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
```

- [ ] **Step 2: Update the one existing test whose exact due-order assertion the new shuffle breaks**

Replace the `session lists due exercises before new ones` test:

```kotlin
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
            GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository(), IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), exerciseRepository)))
        )

        val result = useCase(today)

        // Fase B's internal presentation order is now randomized (see the seeded
        // presentation-order test in this file) -- this test only guards the invariant that
        // Fase B (review) precedes Fase A (new), not the relative order of due-1 vs due-2.
        assertEquals(setOf("due-1", "due-2"), result.take(2).map { it.id }.toSet())
        assertEquals("new-1", result[2].id)
    }
```

- [ ] **Step 3: Write the new failing tests**

Add these tests to the same file (anywhere after the existing tests, before the closing `}`):

```kotlin
    @Test
    fun `Fase B is capped and, with no stale candidates, keeps only the weakest N due items`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        // The fake stands in for the DAO, which Task 1 already verified orders weakest-first --
        // this list is pre-ordered exactly as getDue would return it.
        val dueOrderedByWeakness = (1..12).map { exercise("due-$it") }
        val exerciseRepository = FakeExerciseRepositoryForSession(due = dueOrderedByWeakness)
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        val result = useCase(today, dueExercisesLimit = 10)

        assertEquals(10, result.size)
        assertEquals((1..10).map { "due-$it" }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun `an aged candidate that is also due wins its slot without appearing twice`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val due = (1..9).map { exercise("due-$it") }
        val exerciseRepository = FakeExerciseRepositoryForSession(
            due = due,
            stale = listOf(exercise("due-5"))
        )
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        val result = useCase(today, dueExercisesLimit = 10, random = Random(0L))

        assertEquals(9, result.size)
        assertEquals((1..9).map { "due-$it" }.toSet(), result.map { it.id }.toSet())
        assertEquals(1, result.count { it.id == "due-5" })
    }

    @Test
    fun `different Random seeds can pick different stale candidates for the aged slot`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val due = (1..9).map { exercise("due-$it") }
        val stale = listOf(exercise("stale-a"), exercise("stale-b"))

        val repoA = FakeExerciseRepositoryForSession(due = due, stale = stale)
        val resultA = GetTodaySessionUseCase(repoA, currentUnitUseCase(contentRepository, repoA))(
            today, dueExercisesLimit = 10, random = Random(0L)
        )
        assertTrue("seed 0 must pick stale-a", resultA.any { it.id == "stale-a" })
        assertTrue("seed 0 must not also include stale-b", resultA.none { it.id == "stale-b" })

        val repoB = FakeExerciseRepositoryForSession(due = due, stale = stale)
        val resultB = GetTodaySessionUseCase(repoB, currentUnitUseCase(contentRepository, repoB))(
            today, dueExercisesLimit = 10, random = Random(1L)
        )
        assertTrue("seed 1 must pick stale-b", resultB.any { it.id == "stale-b" })
        assertTrue("seed 1 must not also include stale-a", resultB.none { it.id == "stale-a" })
    }

    @Test
    fun `presentation order of the combined Fase B set is shuffled, not left in weakness order`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val due = (1..9).map { exercise("due-$it") }
        val stale = listOf(exercise("stale-a"), exercise("stale-b"))
        val exerciseRepository = FakeExerciseRepositoryForSession(due = due, stale = stale)
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        // Deliberately not pinned to the exact permutation Random(0L) produces -- that would
        // couple this test to Kotlin's shuffle algorithm and to the implementation's precise
        // sequence of random consumption (e.g. it would break if agedPick selection changed from
        // .shuffled(random).firstOrNull() to random.nextInt(size), with no real regression).
        // What must hold is: the full set is present (weakness selection is untouched), and the
        // order actually changed from plain weakness order (the shuffle ran at all). The
        // "different Random seeds pick different aged-slot winners" test above already proves
        // random is genuinely threaded through the call.
        val result = useCase(today, dueExercisesLimit = 10, random = Random(0L))
        val weaknessOrderWithAgedPickAppended = due.map { it.id } + "stale-a"

        assertEquals(10, result.size)
        assertEquals((due.map { it.id } + "stale-a").toSet(), result.map { it.id }.toSet())
        assertNotEquals(weaknessOrderWithAgedPickAppended, result.map { it.id })
    }

    @Test
    fun `Fase B still precedes Fase A once capping and the aged slot are in play`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val due = (1..9).map { exercise("due-$it") }
        val stale = listOf(exercise("stale-a"), exercise("stale-b"))
        val exerciseRepository = FakeExerciseRepositoryForSession(
            due = due,
            stale = stale,
            exercisesByUnit = mapOf("s1-u1" to listOf(exercise("new-1")))
        )
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        val result = useCase(today, dueExercisesLimit = 10, random = Random(0L))

        assertEquals(11, result.size)
        assertEquals("new-1", result.last().id)
        assertTrue("no Fase A id may appear before the end", result.dropLast(1).none { it.id == "new-1" })
    }

    @Test
    fun `the stale cutoff passed to the repository is today minus staleThresholdDays`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession()
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        useCase(today, staleThresholdDays = 45)

        assertEquals(today.minusDays(45), exerciseRepository.lastStaleCutoff)
    }
```

(The `lastReviewedAt`-protects-fresh-content case from the spec's Testing section is already covered at the SQL level by Task 1's `getDue_withinSameRepetitionsBucket_mostRecentlyReviewedWinsOverEaseFactor` test — this use case trusts the DAO's ordering rather than re-sorting, so re-testing it here against a fake that bypasses SQL entirely would not exercise the real behavior.)

- [ ] **Step 4: Run the tests to confirm they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCaseTest"`
Expected: FAIL to compile — `dueExercisesLimit`, `staleThresholdDays`, and `random` are not parameters of `GetTodaySessionUseCase.invoke` yet (the fake itself already compiles fine, since Step 1 already gave it `getStaleExercises`). Fixed in Step 5.

- [ ] **Step 5: Implement**

Replace the whole file `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

class GetTodaySessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val getCurrentUnitUseCase: GetCurrentUnitUseCase
) {
    suspend operator fun invoke(
        today: LocalDate,
        newExercisesLimit: Int = 5,
        dueExercisesLimit: Int = 10,
        staleThresholdDays: Long = 45,
        random: Random = Random.Default
    ): List<Exercise> {
        // Phase B -- review: the due backlog is already weakness-ordered by ExerciseDao.getDue's
        // ORDER BY. Cap it, reserve 1 slot for a randomly-picked item stale enough that weakness
        // ranking alone would never resurface it, then shuffle only the presentation order.
        val staleCutoff = today.minusDays(staleThresholdDays)
        val staleCandidates = exerciseRepository.getStaleExercises(staleCutoff)
        val agedPick = staleCandidates.shuffled(random).firstOrNull()

        val weaknessSlots = dueExercisesLimit - if (agedPick != null) 1 else 0
        val dueByWeakness = exerciseRepository.getDueExercises(today, limit = Int.MAX_VALUE)
            .filter { it.id != agedPick?.id }
            .take(weaknessSlots)

        val review = (listOfNotNull(agedPick) + dueByWeakness).shuffled(random)

        // Phase A -- Path: advance the current unit in authored order, never shuffled.
        val currentUnit = getCurrentUnitUseCase()
        val new = currentUnit?.let { unit ->
            val unitExercises = exerciseRepository.getExercisesByUnit(unit.id)
            val answeredIds = exerciseRepository
                .getAnsweredExerciseIds(unitExercises.map { it.id })
                .toSet()
            selectPathCandidates(unitExercises, answeredIds).take(newExercisesLimit)
        } ?: emptyList()

        return review + new
    }
}
```

- [ ] **Step 6: Run the tests to confirm they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCaseTest"`
Expected: BUILD SUCCESSFUL — all existing tests plus the 6 new ones pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt
git commit -m "feat: cap and prioritize the daily review session (Fase B)"
```

---

### Task 4: Full-suite verification and on-device QA handoff

**Files:** none (verification only).

**Interfaces:** none — this task consumes the finished feature end-to-end, nothing depends on it.

- [ ] **Step 1: Run the full JVM unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, 0 failures — this is the full regression check, including `GetCheckpointSessionUseCaseTest` (confirming `GetCheckpointSessionUseCase` truly needed no changes, per the Global Constraints).

- [ ] **Step 2: Run the instrumented suite**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: BUILD SUCCESSFUL — includes Task 1's `ExerciseDaoTest` alongside the pre-existing `MigrationTest`/`ExampleInstrumentedTest`. Requires a connected device or emulator.

- [ ] **Step 3: Full clean build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: On-device QA (manual, per the spec's QA section)**

With the real device's accumulated review backlog:
1. Install the build and tap "ESTUDIAR HOY". Confirm the session size is now bounded (Fase B ≤ 10 + Fase A ≤ 5, so ≤ 15 total) instead of 280+.
2. Pull the device DB (`adb shell run-as com.zconte.oopsapp cat .../oops.db` per this project's established sqlite QA methodology) and compare the `repetitions`/`easeFactor` of the exercises that appeared in the session against rows that didn't — the selected ones should skew toward lower `repetitions` and lower `easeFactor`.
3. Play the same session on 2-3 consecutive days and confirm the backlog visibly drains (fewer overdue rows each day) rather than resetting.
4. The 45-day aging slot cannot be observed on the real device yet — per the spec, the oldest real backlog item is currently only ~21 days overdue. This mechanism is already covered by Task 3's seeded unit tests; real-device confirmation is deferred until backlog items naturally cross 45 days.

No commit for this task — it's verification of Tasks 1-3's combined result, already committed.
