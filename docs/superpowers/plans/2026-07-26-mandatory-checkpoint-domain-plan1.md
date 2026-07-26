# Mandatory Cumulative Checkpoint — Domain & Persistence (Plan 1 of 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the domain/persistence layer for the mandatory, cumulative,
growing, timed section checkpoint — the gate logic, the rebalanced
acumulative sampling, the retry-unlock rule, and the pacing/results pure
functions — fully unit-tested, with **zero Compose/ViewModel UI changes**.
Plan 2 (a separate, later plan) wires this into `CheckpointViewModel` and
the UI (timer display, results-breakdown screen, Ruta's locked-pending-
checkpoint indicator).

**Architecture:** Additive Room migration (v3→v4: `review_state` gains
`lastReviewedAt`, new `checkpoint_attempt_failures` table) + extended
`CheckpointRepository` (read methods it didn't have before) + a new
gate condition in `GetLearningPathUseCase` + a rewritten
`GetCheckpointSessionUseCase` + one new use case
(`IsCheckpointRetryUnlockedUseCase`) + two new pure pacing/results
functions. Every production file this plan modifies keeps compiling and
behaving correctly for existing callers — new parameters get defaults, so
`CheckpointViewModel`/`PlacementCheckpointViewModel` need no code changes
beyond one mechanical call-site fix (Task 5).

**Tech Stack:** Kotlin, Room (migration + DAO), JUnit4 +
`kotlinx-coroutines-test` (hand-written fakes, no mocking library — same
convention as the rest of `domain/usecase`), plus one new instrumented
`androidTest` migration test following this project's existing
`MigrationTest.kt` pattern.

**Design doc:** `docs/superpowers/specs/2026-07-26-mandatory-cumulative-checkpoint-design.md`

## Global Constraints

- **Zero Compose/UI file changes**, with exactly one narrow exception:
  `CheckpointViewModel.kt`'s call to `getCheckpointSessionUseCase(sectionId)`
  becomes `getCheckpointSessionUseCase(sectionId, LocalDate.now())` (Task 5)
  — required for compilation since the use case's signature changes, not a
  feature addition. No other line in any `ui/` file changes in this plan.
- **No dedicated Room DAO/RepositoryImpl unit tests** — matches this
  project's existing convention (Room-backed code has no JVM-level test
  harness without Robolectric; correctness is verified via the instrumented
  `MigrationTest.kt` for schema/data integrity, and via domain-level tests
  using hand-written fakes for business logic).
- **The new instrumented migration test cannot be executed by an
  implementer or reviewer in this environment** — there is no connected
  Android device/emulator available to run `connectedAndroidTest`. Verify
  it only by `./gradlew :app:compileDebugAndroidTestKotlin` (confirms it
  compiles) and careful review against the two existing migration tests'
  structure in the same file. Actual execution is deferred to Luis's own
  on-device verification, called out in this plan's final QA section.
- **Interim behavior, expected and accepted:** after this plan merges but
  before Plan 2 merges, `CheckpointViewModel` does not yet call
  `IsCheckpointRetryUnlockedUseCase` — a user could still retry a failed
  checkpoint immediately on-device. This use case exists and is fully
  tested here, but wiring it into the ViewModel (with the UI that explains
  the wait) is Plan 2's job — per the design review that split these two
  plans, enforcement must not ship ahead of the UI that explains it.
- **No timer for the placement/skip checkpoint** — the design spec left
  this "a resolver en el plan"; decision: only the voluntary section
  checkpoint gets a timer (Plan 2). `PlacementCheckpointViewModel`,
  `GetPlacementCheckpointSessionUseCase`, and `GetSkippedUnitsUseCase` are
  **not modified** by this plan beyond the mechanical
  `GetLearningPathUseCase` constructor-signature fix (Task 7).
- **Numeric parameters (revisable, but exact for this plan):** checkpoint
  size = floor 8, +2 per section traversed, ceiling 20. Time budget =
  `round(questionCount × 1.8)` minutes (computed in Task 6, not wired to
  any UI yet). Prior/current sampling split: target size ÷ 2 each,
  clamped to what each pool actually has.
- **No backfill/migration for existing on-device data** — reinstalación
  limpia (same criterion as every previous cycle).
- **Two spec decisions need no code at all, confirmed by inspection, not
  built as tasks:** the 68% pass threshold is unchanged
  (`CompleteCheckpointUseCase`'s `PASS_THRESHOLD_PCT` stays 68); the streak
  is already decoupled from checkpoint pass/fail (`UpdateStreakUseCase` is
  called by `CheckpointViewModel` independently of
  `CompleteCheckpointUseCase`'s result, and its own logic is purely
  date-based — nothing here changes that).

---

### Task 1: Migration v3→v4 — `review_state.lastReviewedAt` + `checkpoint_attempt_failures`

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/entity/ReviewStateEntity.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/data/local/entity/CheckpointAttemptFailureEntity.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/dao/CheckpointAttemptDao.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/local/Migrations.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/FailedCheckpointAttempt.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/ReviewState.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/repository/CheckpointRepository.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/repository/CheckpointRepositoryImpl.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/testutil/FakeCheckpointRepository.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`
- Modify: `app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt`

**Interfaces:**
- Consumes: nothing new — pure additive schema/plumbing.
- Produces: `ReviewState.lastReviewedAt: LocalDate` (Task 2 consumes this),
  `CheckpointRepository.hasApprovedAttempt(sectionId, kind): Boolean` and
  `.getLatestFailedAttempt(sectionId, kind): FailedCheckpointAttempt?`
  (Tasks 4 and 7 consume these), `CheckpointRepository.recordAttempt(...,
  failedExerciseIds: List<String> = emptyList())` (Task 3 consumes this).

This task is pure plumbing (no new business logic of its own) —
verification is compilation + the full existing unit test suite (nothing
should break) + the new migration test compiling (not running, see Global
Constraints).

- [ ] **Step 1: Add `lastReviewedAt` to `ReviewStateEntity`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/data/local/entity/ReviewStateEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_state")
data class ReviewStateEntity(
    @PrimaryKey val exerciseId: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: Long,
    val lastReviewedAt: Long = 0L
)
```

- [ ] **Step 2: Add `lastReviewedAt` to the domain `ReviewState` model**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/model/ReviewState.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

import java.time.LocalDate

data class ReviewState(
    val exerciseId: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: LocalDate,
    val lastReviewedAt: LocalDate = LocalDate.EPOCH
)
```

(The default lets every existing `ReviewState(exerciseId = ..., easeFactor
= ..., ...)` construction site across the test suite keep compiling
unchanged — none of them need to specify this field.)

- [ ] **Step 3: Thread `lastReviewedAt` through the entity mapper**

In `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`,
replace the two private mapper functions at the bottom of the file:

```kotlin
private fun ReviewStateEntity.toDomain() = ReviewState(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = LocalDate.ofEpochDay(dueDate)
)

private fun ReviewState.toEntity() = ReviewStateEntity(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = dueDate.toEpochDay()
)
```

with:

```kotlin
private fun ReviewStateEntity.toDomain() = ReviewState(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = LocalDate.ofEpochDay(dueDate),
    lastReviewedAt = LocalDate.ofEpochDay(lastReviewedAt)
)

private fun ReviewState.toEntity() = ReviewStateEntity(
    exerciseId = exerciseId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = dueDate.toEpochDay(),
    lastReviewedAt = lastReviewedAt.toEpochDay()
)
```

- [ ] **Step 4: Create `CheckpointAttemptFailureEntity`**

Create `app/src/main/java/com/zconte/oopsapp/data/local/entity/CheckpointAttemptFailureEntity.kt`:

```kotlin
package com.zconte.oopsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkpoint_attempt_failures")
data class CheckpointAttemptFailureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val exerciseId: String
)
```

- [ ] **Step 5: Extend `CheckpointAttemptDao`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/data/local/dao/CheckpointAttemptDao.kt`:

```kotlin
package com.zconte.oopsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptFailureEntity

@Dao
interface CheckpointAttemptDao {
    @Insert
    suspend fun insert(attempt: CheckpointAttemptEntity): Long

    @Insert
    suspend fun insertFailures(failures: List<CheckpointAttemptFailureEntity>)

    // Returns attempts of every kind (review and placement) for this section -- callers that
    // only want one kind must filter the result by `kind` themselves.
    @Query("SELECT * FROM checkpoint_attempts WHERE sectionId = :sectionId ORDER BY takenAt DESC")
    suspend fun getBySection(sectionId: String): List<CheckpointAttemptEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM checkpoint_attempts WHERE sectionId = :sectionId AND kind = :kind AND passed = 1)")
    suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean

    @Query("SELECT * FROM checkpoint_attempts WHERE sectionId = :sectionId AND kind = :kind ORDER BY id DESC LIMIT 1")
    suspend fun getLatestAttempt(sectionId: String, kind: String): CheckpointAttemptEntity?

    @Query("SELECT exerciseId FROM checkpoint_attempt_failures WHERE attemptId = :attemptId")
    suspend fun getFailedExerciseIds(attemptId: Long): List<String>
}
```

- [ ] **Step 6: Register the new entity and bump the database version**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt`:

```kotlin
package com.zconte.oopsapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zconte.oopsapp.data.local.dao.CheckpointAttemptDao
import com.zconte.oopsapp.data.local.dao.ContentMetaDao
import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.dao.UnitProgressDao
import com.zconte.oopsapp.data.local.dao.UserStatsDao
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptFailureEntity
import com.zconte.oopsapp.data.local.entity.ContentMetaEntity
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.data.local.entity.UnitProgressEntity
import com.zconte.oopsapp.data.local.entity.UserStatsEntity

@Database(
    entities = [
        SectionEntity::class, UnitEntity::class, ExerciseEntity::class,
        ReviewStateEntity::class, UserStatsEntity::class,
        UnitProgressEntity::class, CheckpointAttemptEntity::class, ContentMetaEntity::class,
        CheckpointAttemptFailureEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sectionDao(): SectionDao
    abstract fun unitDao(): UnitDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun reviewStateDao(): ReviewStateDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun unitProgressDao(): UnitProgressDao
    abstract fun checkpointAttemptDao(): CheckpointAttemptDao
    abstract fun contentMetaDao(): ContentMetaDao
}
```

- [ ] **Step 7: Add `MIGRATION_3_4`**

In `app/src/main/java/com/zconte/oopsapp/data/local/Migrations.kt`, append
after `MIGRATION_2_3`:

```kotlin

/**
 * Adds lastReviewedAt to review_state (needed by the mandatory-checkpoint retry gate: "has this
 * exercise been re-answered since the last failed attempt?") and a new checkpoint_attempt_failures
 * table (which exercises were failed in a given attempt, so a retry knows what to check for
 * re-exposure). Pre-existing review_state rows default lastReviewedAt to the epoch (well before
 * any real attempt date), which is intentional -- see design spec, no backfill needed.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE review_state ADD COLUMN lastReviewedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS checkpoint_attempt_failures (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                attemptId INTEGER NOT NULL,
                exerciseId TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
```

- [ ] **Step 8: Register the migration in Hilt**

In `app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt`, add the
import alongside the existing migration imports:

```kotlin
import com.zconte.oopsapp.data.local.MIGRATION_3_4
```

and change:

```kotlin
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

to:

```kotlin
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
```

- [ ] **Step 9: Create the `FailedCheckpointAttempt` domain model**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/FailedCheckpointAttempt.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

import java.time.LocalDate

data class FailedCheckpointAttempt(
    val takenAt: LocalDate,
    val failedExerciseIds: List<String>
)
```

- [ ] **Step 10: Extend `CheckpointRepository`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/repository/CheckpointRepository.kt`:

```kotlin
package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.FailedCheckpointAttempt
import java.time.LocalDate

interface CheckpointRepository {
    suspend fun recordAttempt(
        sectionId: String,
        kind: String,
        scorePct: Int,
        passed: Boolean,
        takenAt: LocalDate,
        failedExerciseIds: List<String> = emptyList()
    )

    suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean

    suspend fun getLatestFailedAttempt(sectionId: String, kind: String): FailedCheckpointAttempt?
}
```

- [ ] **Step 11: Implement the new `CheckpointRepository` methods**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/data/repository/CheckpointRepositoryImpl.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.CheckpointAttemptDao
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptEntity
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptFailureEntity
import com.zconte.oopsapp.domain.model.FailedCheckpointAttempt
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate
import javax.inject.Inject

class CheckpointRepositoryImpl @Inject constructor(
    private val checkpointAttemptDao: CheckpointAttemptDao
) : CheckpointRepository {

    override suspend fun recordAttempt(
        sectionId: String,
        kind: String,
        scorePct: Int,
        passed: Boolean,
        takenAt: LocalDate,
        failedExerciseIds: List<String>
    ) {
        val attemptId = checkpointAttemptDao.insert(
            CheckpointAttemptEntity(
                sectionId = sectionId,
                kind = kind,
                scorePct = scorePct,
                passed = passed,
                takenAt = takenAt.toEpochDay()
            )
        )
        if (failedExerciseIds.isNotEmpty()) {
            checkpointAttemptDao.insertFailures(
                failedExerciseIds.map { CheckpointAttemptFailureEntity(attemptId = attemptId, exerciseId = it) }
            )
        }
    }

    override suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean =
        checkpointAttemptDao.hasApprovedAttempt(sectionId, kind)

    override suspend fun getLatestFailedAttempt(sectionId: String, kind: String): FailedCheckpointAttempt? {
        val latest = checkpointAttemptDao.getLatestAttempt(sectionId, kind) ?: return null
        if (latest.passed) return null
        val failedIds = checkpointAttemptDao.getFailedExerciseIds(latest.id)
        return FailedCheckpointAttempt(takenAt = LocalDate.ofEpochDay(latest.takenAt), failedExerciseIds = failedIds)
    }
}
```

- [ ] **Step 12: Update the shared test fake to match the new interface**

Replace the full content of
`app/src/test/java/com/zconte/oopsapp/testutil/FakeCheckpointRepository.kt`:

```kotlin
package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.model.FailedCheckpointAttempt
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import java.time.LocalDate

class FakeCheckpointRepository : CheckpointRepository {

    data class RecordedAttempt(
        val sectionId: String,
        val kind: String,
        val scorePct: Int,
        val passed: Boolean,
        val takenAt: LocalDate,
        val failedExerciseIds: List<String>
    )

    val recordedAttempts = mutableListOf<RecordedAttempt>()

    override suspend fun recordAttempt(
        sectionId: String,
        kind: String,
        scorePct: Int,
        passed: Boolean,
        takenAt: LocalDate,
        failedExerciseIds: List<String>
    ) {
        recordedAttempts.add(RecordedAttempt(sectionId, kind, scorePct, passed, takenAt, failedExerciseIds))
    }

    override suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean =
        recordedAttempts.any { it.sectionId == sectionId && it.kind == kind && it.passed }

    override suspend fun getLatestFailedAttempt(sectionId: String, kind: String): FailedCheckpointAttempt? {
        val latest = recordedAttempts
            .filter { it.sectionId == sectionId && it.kind == kind }
            .maxByOrNull { it.takenAt }
            ?: return null
        if (latest.passed) return null
        return FailedCheckpointAttempt(latest.takenAt, latest.failedExerciseIds)
    }
}
```

(This is a behaviorally-accurate fake, not a dumb recorder — it derives
`hasApprovedAttempt`/`getLatestFailedAttempt` from the same
`recordedAttempts` history a test seeds via `recordAttempt(...)`, exactly
like the real `CheckpointRepositoryImpl` derives them from its DB rows.)

- [ ] **Step 13: Update `CompleteCheckpointUseCaseTest`'s own private fake**

`CompleteCheckpointUseCaseTest.kt` has its own private
`FakeCheckpointRepository` (not the shared `testutil` one). In
`app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`,
replace:

```kotlin
private class FakeCheckpointRepository : CheckpointRepository {
    data class RecordedAttempt(val sectionId: String, val kind: String, val scorePct: Int, val passed: Boolean)
    val recorded = mutableListOf<RecordedAttempt>()

    override suspend fun recordAttempt(sectionId: String, kind: String, scorePct: Int, passed: Boolean, takenAt: LocalDate) {
        recorded.add(RecordedAttempt(sectionId, kind, scorePct, passed))
    }
}
```

with:

```kotlin
private class FakeCheckpointRepository : CheckpointRepository {
    data class RecordedAttempt(
        val sectionId: String,
        val kind: String,
        val scorePct: Int,
        val passed: Boolean,
        val failedExerciseIds: List<String>
    )
    val recorded = mutableListOf<RecordedAttempt>()

    override suspend fun recordAttempt(
        sectionId: String,
        kind: String,
        scorePct: Int,
        passed: Boolean,
        takenAt: LocalDate,
        failedExerciseIds: List<String>
    ) {
        recorded.add(RecordedAttempt(sectionId, kind, scorePct, passed, failedExerciseIds))
    }

    override suspend fun hasApprovedAttempt(sectionId: String, kind: String): Boolean = false

    override suspend fun getLatestFailedAttempt(sectionId: String, kind: String): com.zconte.oopsapp.domain.model.FailedCheckpointAttempt? = null
}
```

(This file's existing 6 tests only assert on `recorded.size` and
`recorded.first().passed` — never construct a `RecordedAttempt` by
position — so adding a field does not break them. `hasApprovedAttempt`/
`getLatestFailedAttempt` are stubbed since nothing in this file's tests
exercises them; Task 3 adds a real assertion on `failedExerciseIds`.)

- [ ] **Step 14: Add the migration test (write-only, cannot be executed here)**

In `app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt`,
add the import alongside the existing ones:

```kotlin
import com.zconte.oopsapp.data.local.entity.CheckpointAttemptFailureEntity
```

Then add a new test method after `migrate2To3_addsCompletedViaColumnDefaultingToPlayed`:

```kotlin

    @Test
    fun migrate3To4_addsLastReviewedAtAndTheAttemptFailuresTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration-test-3-4"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)

        // 1. Build a v3-shaped database file directly with raw SQL (pre-migration shape: no
        // lastReviewedAt column, no checkpoint_attempt_failures table yet), with a pre-existing
        // review_state row and a pre-existing checkpoint attempt representing prior user data.
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).apply {
            execSQL("CREATE TABLE sections (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, orderIndex INTEGER NOT NULL, examVersion TEXT NOT NULL)")
            execSQL("CREATE TABLE units (id TEXT NOT NULL PRIMARY KEY, sectionId TEXT NOT NULL, name TEXT NOT NULL, certObjective TEXT NOT NULL, orderIndex INTEGER NOT NULL)")
            execSQL("CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, unitId TEXT NOT NULL, type TEXT NOT NULL, payload TEXT NOT NULL, difficulty INTEGER NOT NULL, examVersion TEXT NOT NULL)")
            execSQL("CREATE TABLE unit_progress (unitId TEXT NOT NULL PRIMARY KEY, completed INTEGER NOT NULL, completedAt INTEGER, completedVia TEXT NOT NULL DEFAULT 'played')")
            execSQL("CREATE TABLE checkpoint_attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sectionId TEXT NOT NULL, kind TEXT NOT NULL, scorePct INTEGER NOT NULL, passed INTEGER NOT NULL, takenAt INTEGER NOT NULL)")
            execSQL("CREATE TABLE content_meta (configKey TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
            execSQL("CREATE TABLE `review_state` (`exerciseId` TEXT NOT NULL, `easeFactor` REAL NOT NULL, `intervalDays` INTEGER NOT NULL, `repetitions` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`))")
            execSQL("CREATE TABLE `user_stats` (`id` INTEGER NOT NULL, `streak` INTEGER NOT NULL, `xp` INTEGER NOT NULL, `lastStudyDate` INTEGER, PRIMARY KEY(`id`))")
            execSQL("INSERT INTO review_state (exerciseId, easeFactor, intervalDays, repetitions, dueDate) VALUES ('fund-ex-1', 2.6, 6, 2, 19000)")
            execSQL(
                "INSERT INTO checkpoint_attempts (sectionId, kind, scorePct, passed, takenAt) VALUES " +
                    "('java-fundamentals', 'review', 80, 1, 19000)"
            )
            version = 3
            close()
        }

        // 2. Open via a real Room-managed AppDatabase with all three migrations registered.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                // Pre-existing review_state survived and defaulted lastReviewedAt to the epoch.
                val reviewState = db.reviewStateDao().getByExerciseId("fund-ex-1")
                assertNotNull("review_state must survive the migration", reviewState)
                assertEquals(2.6, reviewState!!.easeFactor, 0.0001)
                assertEquals(0L, reviewState.lastReviewedAt)

                // The pre-existing attempt survived and has no failure rows (it predates this
                // migration and was never associated with per-question failures).
                val attempts = db.checkpointAttemptDao().getBySection("java-fundamentals")
                assertEquals(1, attempts.size)
                val failures = db.checkpointAttemptDao().getFailedExerciseIds(attempts.first().id)
                assertTrue("no failure rows exist for a pre-migration attempt", failures.isEmpty())

                // The new table accepts real inserts going forward.
                db.checkpointAttemptDao().insertFailures(
                    listOf(CheckpointAttemptFailureEntity(attemptId = attempts.first().id, exerciseId = "fund-ex-1"))
                )
                assertEquals(listOf("fund-ex-1"), db.checkpointAttemptDao().getFailedExerciseIds(attempts.first().id))
            }
        } finally {
            db.close()
        }
    }
```

- [ ] **Step 15: Verify everything compiles (main + test + androidTest source sets)**

Run: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL. This is the only verification available for
the new migration test in this environment (see Global Constraints) — do
not attempt `connectedAndroidTest`, there is no device/emulator here.

- [ ] **Step 16: Run the full existing JVM unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all pre-existing tests still pass (this task
changes no business logic, only adds fields/methods with safe defaults —
zero test should need behavior changes here; `CompleteCheckpointUseCaseTest`'s
6 existing tests must all still pass unchanged).

- [ ] **Step 17: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/local/entity/ReviewStateEntity.kt \
        app/src/main/java/com/zconte/oopsapp/data/local/entity/CheckpointAttemptFailureEntity.kt \
        app/src/main/java/com/zconte/oopsapp/data/local/dao/CheckpointAttemptDao.kt \
        app/src/main/java/com/zconte/oopsapp/data/local/AppDatabase.kt \
        app/src/main/java/com/zconte/oopsapp/data/local/Migrations.kt \
        app/src/main/java/com/zconte/oopsapp/di/DatabaseModule.kt \
        app/src/main/java/com/zconte/oopsapp/domain/model/FailedCheckpointAttempt.kt \
        app/src/main/java/com/zconte/oopsapp/domain/model/ReviewState.kt \
        app/src/main/java/com/zconte/oopsapp/domain/repository/CheckpointRepository.kt \
        app/src/main/java/com/zconte/oopsapp/data/repository/CheckpointRepositoryImpl.kt \
        app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt \
        app/src/test/java/com/zconte/oopsapp/testutil/FakeCheckpointRepository.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt \
        app/src/androidTest/java/com/zconte/oopsapp/data/local/MigrationTest.kt
git commit -m "feat: add review_state.lastReviewedAt and checkpoint_attempt_failures (migration v3->v4)"
```

---

### Task 2: `SubmitAnswerUseCase` stamps `lastReviewedAt`

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCase.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt`

**Interfaces:**
- Consumes: `ReviewState.lastReviewedAt` (Task 1).
- Produces: every `ReviewState` written via `SubmitAnswerUseCase` now
  carries today's date in `lastReviewedAt` — consumed by Task 4's retry
  gate.

- [ ] **Step 1: Write the failing assertions**

Replace the full content of
`app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForAnswer : ExerciseRepository {
    val states = mutableMapOf<String, ReviewState>()

    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = emptyList()
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = states[exerciseId]
    override suspend fun saveReviewState(state: ReviewState) {
        states[state.exerciseId] = state
    }
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> =
        states.keys.filter { it in exerciseIds }
}

class SubmitAnswerUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `creates a default review state on first answer, applies SM-2 and stamps lastReviewedAt`() = runTest {
        val repository = FakeExerciseRepositoryForAnswer()
        val useCase = SubmitAnswerUseCase(repository)

        val result = useCase("ex-1", quality = 4, today = today)

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(today.plusDays(1), result.dueDate)
        assertEquals(today, result.lastReviewedAt)
        assertEquals(result, repository.states["ex-1"])
    }

    @Test
    fun `reuses the existing review state on later answers and re-stamps lastReviewedAt`() = runTest {
        val repository = FakeExerciseRepositoryForAnswer().apply {
            states["ex-1"] = ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1,
                repetitions = 1, dueDate = today, lastReviewedAt = today.minusDays(6)
            )
        }
        val useCase = SubmitAnswerUseCase(repository)

        val result = useCase("ex-1", quality = 4, today = today)

        assertEquals(2, result.repetitions)
        assertEquals(6, result.intervalDays)
        assertEquals(today, result.lastReviewedAt)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCaseTest"`
Expected: FAIL — `assertEquals(today, result.lastReviewedAt)` fails
because `SubmitAnswerUseCase` doesn't stamp it yet (it stays at the
`LocalDate.EPOCH` default).

- [ ] **Step 3: Stamp `lastReviewedAt` in `SubmitAnswerUseCase`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.srs.SchedulerSm2
import java.time.LocalDate
import javax.inject.Inject

class SubmitAnswerUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(exerciseId: String, quality: Int, today: LocalDate): ReviewState {
        val current = exerciseRepository.getReviewState(exerciseId)
            ?: ReviewState(
                exerciseId = exerciseId, easeFactor = 2.5, intervalDays = 0,
                repetitions = 0, dueDate = today
            )
        val updated = SchedulerSm2.review(current, quality, today).copy(lastReviewedAt = today)
        exerciseRepository.saveReviewState(updated)
        return updated
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.SubmitAnswerUseCaseTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — regression check, since `SubmitAnswerUseCase`
is used by several ViewModels.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/SubmitAnswerUseCaseTest.kt
git commit -m "feat: stamp lastReviewedAt on every SM-2 answer submission"
```

---

### Task 3: `CompleteCheckpointUseCase` threads `failedExerciseIds`

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`

**Interfaces:**
- Consumes: `CheckpointRepository.recordAttempt(..., failedExerciseIds)` (Task 1).
- Produces: `CompleteCheckpointUseCase.invoke(..., failedExerciseIds: List<String> = emptyList())`
  — Plan 2's `CheckpointViewModel` will pass the real failed ids once it
  starts tracking them; existing callers (unchanged in this plan) keep
  compiling via the default.

- [ ] **Step 1: Write the failing test**

In `app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt`,
add this test after `` `fails below the 68 percent threshold` ``:

```kotlin

    @Test
    fun `failed exercise ids are threaded through to the recorded attempt`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        val useCase = CompleteCheckpointUseCase(
            checkpointRepository, FakeContentRepositoryForComplete(), FakeExerciseRepositoryForComplete()
        )

        useCase(
            "s1", CheckpointKind.REVIEW, correctCount = 6, totalCount = 12, today = today,
            failedExerciseIds = listOf("ex-3", "ex-7")
        )

        assertEquals(listOf("ex-3", "ex-7"), checkpointRepository.recorded.first().failedExerciseIds)
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCaseTest"`
Expected: FAIL — compile error, `invoke` doesn't yet accept
`failedExerciseIds`.

- [ ] **Step 3: Add the parameter and thread it through**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CheckpointResult
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

private const val PASS_THRESHOLD_PCT = 68
private const val SEED_EASE_FACTOR = 2.5
private const val SEED_INTERVAL_DAYS = 1
private const val SEED_REPETITIONS = 1

/**
 * Pure grading so callers (e.g. a checkpoint ViewModel) can predict pass/fail from the same
 * correct/total counts before committing any side effects, without duplicating the formula.
 */
fun computeCheckpointResult(correctCount: Int, totalCount: Int): CheckpointResult {
    val scorePct = if (totalCount == 0) 0 else (correctCount * 100) / totalCount
    return CheckpointResult(scorePct, scorePct >= PASS_THRESHOLD_PCT)
}

class CompleteCheckpointUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository,
    private val contentRepository: ContentRepository,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(
        sectionId: String,
        kind: String,
        correctCount: Int,
        totalCount: Int,
        today: LocalDate,
        skippedUnitIds: List<String> = emptyList(),
        failedExerciseIds: List<String> = emptyList()
    ): CheckpointResult {
        val result = computeCheckpointResult(correctCount, totalCount)
        checkpointRepository.recordAttempt(sectionId, kind, result.scorePct, result.passed, today, failedExerciseIds)

        if (kind == CheckpointKind.PLACEMENT && result.passed) {
            unlockSkippedUnits(skippedUnitIds, today)
        }

        return result
    }

    private suspend fun unlockSkippedUnits(skippedUnitIds: List<String>, today: LocalDate) {
        skippedUnitIds.forEach { unitId ->
            contentRepository.markUnitCompleted(unitId, today, UnitCompletionSource.PLACEMENT)
            exerciseRepository.getExercisesByUnit(unitId).forEach { exercise ->
                if (exerciseRepository.getReviewState(exercise.id) == null) {
                    exerciseRepository.saveReviewState(
                        ReviewState(
                            exerciseId = exercise.id,
                            easeFactor = SEED_EASE_FACTOR,
                            intervalDays = SEED_INTERVAL_DAYS,
                            repetitions = SEED_REPETITIONS,
                            dueDate = today.plusDays(1)
                        )
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.CompleteCheckpointUseCaseTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCaseTest.kt
git commit -m "feat: thread failedExerciseIds through CompleteCheckpointUseCase"
```

---

### Task 4: `IsCheckpointRetryUnlockedUseCase` (the retry-gate, new)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/IsCheckpointRetryUnlockedUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/IsCheckpointRetryUnlockedUseCaseTest.kt`

**Interfaces:**
- Consumes: `CheckpointRepository.getLatestFailedAttempt` (Task 1),
  `ExerciseRepository.getReviewState` (existing), `ReviewState.lastReviewedAt`
  (Task 1/2).
- Produces: `class IsCheckpointRetryUnlockedUseCase { suspend operator fun
  invoke(sectionId: String, kind: String = CheckpointKind.REVIEW): Boolean
  }` — consumed by Plan 2's `CheckpointViewModel`, not by anything in this
  plan.

**The single most important test in this task** is the "same-day
re-exposure must NOT unlock" case below. The retry-gate's unlock
predicate must use **strict `>`** (`lastReviewedAt > attempt.takenAt`),
never `>=` — the voluntary checkpoint itself writes `review_state` (via
`SubmitAnswerUseCase`, called by `CheckpointViewModel.submitAnswer` on
every question, pass or fail) for exactly the exercises you just failed,
on the **same day** as the failed attempt. With `>=`, the gate would
self-satisfy the instant the failed attempt ends, and never actually
block anything.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/IsCheckpointRetryUnlockedUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.ReviewState
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
import com.zconte.oopsapp.testutil.FakeExerciseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IsCheckpointRetryUnlockedUseCaseTest {

    private val attemptDay = LocalDate.of(2026, 7, 26)

    @Test
    fun `unlocked when there is no attempt on record at all`() = runTest {
        val useCase = IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), FakeExerciseRepository())

        assertTrue(useCase("s1"))
    }

    @Test
    fun `unlocked when the latest attempt was passed`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 80, passed = true, takenAt = attemptDay, failedExerciseIds = emptyList()
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository())

        assertTrue(useCase("s1"))
    }

    @Test
    fun `unlocked when the latest failed attempt had no failed exercises`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay, failedExerciseIds = emptyList()
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository())

        assertTrue(useCase("s1"))
    }

    @Test
    fun `locked when a failed exercise has never been reviewed again`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay,
            failedExerciseIds = listOf("ex-1")
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, FakeExerciseRepository())

        assertFalse(useCase("s1"))
    }

    @Test
    fun `locked when a failed exercise was reviewed again on the SAME day as the failed attempt`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay,
            failedExerciseIds = listOf("ex-1")
        )
        // This is exactly what the failed checkpoint attempt itself just wrote via SubmitAnswerUseCase
        // -- same-day re-exposure must NOT count, or the gate would self-satisfy instantly.
        val exerciseRepository = FakeExerciseRepository()
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1, repetitions = 0,
                dueDate = attemptDay.plusDays(1), lastReviewedAt = attemptDay
            )
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository)

        assertFalse(useCase("s1"))
    }

    @Test
    fun `unlocked once every failed exercise was reviewed again on a later day`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay,
            failedExerciseIds = listOf("ex-1", "ex-2")
        )
        val exerciseRepository = FakeExerciseRepository()
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1, repetitions = 1,
                dueDate = attemptDay.plusDays(2), lastReviewedAt = attemptDay.plusDays(1)
            )
        )
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-2", easeFactor = 2.5, intervalDays = 1, repetitions = 1,
                dueDate = attemptDay.plusDays(2), lastReviewedAt = attemptDay.plusDays(1)
            )
        )
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository)

        assertTrue(useCase("s1"))
    }

    @Test
    fun `locked when only some of the failed exercises were reviewed again`() = runTest {
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 50, passed = false, takenAt = attemptDay,
            failedExerciseIds = listOf("ex-1", "ex-2")
        )
        val exerciseRepository = FakeExerciseRepository()
        exerciseRepository.saveReviewState(
            ReviewState(
                exerciseId = "ex-1", easeFactor = 2.5, intervalDays = 1, repetitions = 1,
                dueDate = attemptDay.plusDays(2), lastReviewedAt = attemptDay.plusDays(1)
            )
        )
        // ex-2 never reviewed again.
        val useCase = IsCheckpointRetryUnlockedUseCase(checkpointRepository, exerciseRepository)

        assertFalse(useCase("s1"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.IsCheckpointRetryUnlockedUseCaseTest"`
Expected: FAIL — compile error, `IsCheckpointRetryUnlockedUseCase` is
unresolved.

- [ ] **Step 3: Implement `IsCheckpointRetryUnlockedUseCase`**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/IsCheckpointRetryUnlockedUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import javax.inject.Inject

class IsCheckpointRetryUnlockedUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(sectionId: String, kind: String = CheckpointKind.REVIEW): Boolean {
        val lastFailed = checkpointRepository.getLatestFailedAttempt(sectionId, kind) ?: return true
        if (lastFailed.failedExerciseIds.isEmpty()) return true

        return lastFailed.failedExerciseIds.all { exerciseId ->
            val state = exerciseRepository.getReviewState(exerciseId)
            // Strict '>': re-exposure on the SAME day as the failed attempt does not count -- that
            // is exactly what the failed attempt's own answers just wrote via SubmitAnswerUseCase.
            state != null && state.lastReviewedAt > lastFailed.takenAt
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.IsCheckpointRetryUnlockedUseCaseTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/IsCheckpointRetryUnlockedUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/IsCheckpointRetryUnlockedUseCaseTest.kt
git commit -m "feat: add IsCheckpointRetryUnlockedUseCase, the mandatory-checkpoint retry gate"
```

---

### Task 5: `GetCheckpointSessionUseCase` — acumulativo, creciente, sesgado a vencidos

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCase.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`
  (one-line call-site fix only, see Global Constraints)

**Interfaces:**
- Consumes: `ExerciseRepository.getExercisesBySection`, `.getDueExercises`
  (existing, unchanged).
- Produces: `checkpointSize(sectionsTraversed: Int): Int` (top-level pure
  function, exposed for direct testing) and
  `GetCheckpointSessionUseCase.invoke(sectionId: String, today: LocalDate):
  List<Exercise>` (signature changed — gained `today`). Nothing in a later
  task in this plan consumes either.

- [ ] **Step 1: Write the failing tests**

Replace the full content of
`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt`:

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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseRepositoryForCheckpoint(
    private val bySection: Map<String, List<Exercise>> = emptyMap(),
    private val due: List<Exercise> = emptyList()
) : ExerciseRepository {
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> = due.take(limit)
    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> = emptyList()
    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> = bySection[sectionId] ?: emptyList()
    override suspend fun getReviewState(exerciseId: String): ReviewState? = null
    override suspend fun saveReviewState(state: ReviewState) {}
    override suspend fun getAnsweredExerciseIds(exerciseIds: List<String>): List<String> = emptyList()
}

private class FakeContentRepositoryForCheckpoint(
    private val sections: List<Section>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = emptyList()
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetCheckpointSessionUseCaseTest {

    private val today = LocalDate.of(2026, 7, 26)

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "mcq", "{}", 1)

    @Test
    fun `checkpointSize starts at the floor for the first section`() {
        assertEquals(8, checkpointSize(sectionsTraversed = 1))
    }

    @Test
    fun `checkpointSize grows by 2 per section traversed`() {
        assertEquals(10, checkpointSize(sectionsTraversed = 2))
        assertEquals(16, checkpointSize(sectionsTraversed = 5))
    }

    @Test
    fun `checkpointSize is capped at the ceiling`() {
        assertEquals(20, checkpointSize(sectionsTraversed = 7))
        assertEquals(20, checkpointSize(sectionsTraversed = 50))
    }

    @Test
    fun `the first section draws entirely from its own pool, sized at the floor`() = runTest {
        val currentPool = (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(bySection = mapOf("s1" to currentPool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1", today)

        assertEquals(8, result.size)
        assertTrue(result.all { it.id.startsWith("s1-ex-") })
    }

    @Test
    fun `a later section mixes in roughly half from earlier sections`() = runTest {
        val s1Pool = (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val s2Pool = (1..20).map { exercise("s2-ex-$it", "s2-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(bySection = mapOf("s1" to s1Pool, "s2" to s2Pool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1), section("s2", 2)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s2", today)

        // sectionsTraversed = 2 -> size 10; half-and-half split -> 5 prior, 5 current.
        assertEquals(10, result.size)
        assertEquals(5, result.count { it.id.startsWith("s1-ex-") })
        assertEquals(5, result.count { it.id.startsWith("s2-ex-") })
    }

    @Test
    fun `due prior exercises are preferred over non-due ones when sampling earlier sections`() = runTest {
        val duePool = (1..5).map { exercise("s1-due-$it", "s1-unit") }
        val restPool = (1..20).map { exercise("s1-rest-$it", "s1-unit") }
        val s2Pool = (1..20).map { exercise("s2-ex-$it", "s2-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(
            bySection = mapOf("s1" to duePool + restPool, "s2" to s2Pool),
            due = duePool
        )
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1), section("s2", 2)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s2", today)

        // sectionsTraversed = 2 -> size 10 -> 5 prior. There are exactly 5 due items, so all 5
        // must be used before any non-due prior item.
        val priorInResult = result.filter { it.id.startsWith("s1-") }
        assertEquals(5, priorInResult.size)
        assertTrue(priorInResult.all { it.id.startsWith("s1-due-") })
    }

    @Test
    fun `a small section pool is capped, not padded past what exists`() = runTest {
        val s1Pool = listOf(exercise("s1-ex-1", "s1-unit"), exercise("s1-ex-2", "s1-unit"))
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(bySection = mapOf("s1" to s1Pool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1", today)

        assertEquals(2, result.size)
    }

    @Test
    fun `an unknown section id yields no session`() = runTest {
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint()
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("does-not-exist", today)

        assertTrue(result.isEmpty())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCaseTest"`
Expected: FAIL — compile error (`checkpointSize` unresolved, `invoke`
doesn't yet take a `today` parameter).

- [ ] **Step 3: Rewrite `GetCheckpointSessionUseCase`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

private const val SIZE_FLOOR = 8
private const val SIZE_GROWTH_PER_SECTION = 2
private const val SIZE_CEILING = 20

/**
 * How many questions a section's mandatory checkpoint has, given how many sections the player
 * has traversed (1-based, including the section the checkpoint is for). Grows toward the real
 * exam's scale without becoming it -- see the design spec's floor/growth/ceiling decision.
 */
fun checkpointSize(sectionsTraversed: Int): Int =
    (SIZE_FLOOR + SIZE_GROWTH_PER_SECTION * (sectionsTraversed - 1)).coerceAtMost(SIZE_CEILING)

class GetCheckpointSessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(sectionId: String, today: LocalDate): List<Exercise> {
        val sections = contentRepository.getSections().sortedBy { it.orderIndex }
        val currentIndex = sections.indexOfFirst { it.id == sectionId }
        if (currentIndex < 0) return emptyList()

        val targetSize = checkpointSize(sectionsTraversed = currentIndex + 1)

        val currentPool = exerciseRepository.getExercisesBySection(sectionId)
        val priorPool = sections.take(currentIndex).flatMap { exerciseRepository.getExercisesBySection(it.id) }

        val priorTargetSize = (targetSize / 2).coerceAtMost(priorPool.size)
        val currentTargetSize = (targetSize - priorTargetSize).coerceAtMost(currentPool.size)

        val dueIds = exerciseRepository.getDueExercises(today, Int.MAX_VALUE).map { it.id }.toSet()
        val (duePrior, restPrior) = priorPool.partition { it.id in dueIds }
        val priorSample = (duePrior.shuffled() + restPrior.shuffled()).take(priorTargetSize)

        val currentSample = currentPool.shuffled().take(currentTargetSize)

        return (currentSample + priorSample).shuffled()
    }
}
```

- [ ] **Step 4: Fix the one call site (mechanical, required for compilation)**

In `app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt`,
in the `init` block, replace:

```kotlin
            val queue = getCheckpointSessionUseCase(sectionId)
```

with:

```kotlin
            val queue = getCheckpointSessionUseCase(sectionId, LocalDate.now())
```

(`LocalDate` is already imported in this file.)

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCaseTest"`
Expected: PASS (9 tests).

- [ ] **Step 6: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt \
        app/src/main/java/com/zconte/oopsapp/ui/checkpoint/CheckpointViewModel.kt
git commit -m "feat: rebalance GetCheckpointSessionUseCase to a cumulative, growing, due-biased sample"
```

---

### Task 6: Checkpoint pacing and results pure functions (time budget, section breakdown)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/CheckpointTimeBudget.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/CheckpointTimeBudgetTest.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/usecase/ComputeCheckpointSectionBreakdown.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/ComputeCheckpointSectionBreakdownTest.kt`

**Interfaces:**
- Consumes: nothing (pure functions, no repository dependencies).
- Produces: `computeCheckpointTimeBudgetSeconds(questionCount: Int): Int`
  and `computeCheckpointSectionBreakdown(results: List<Pair<Exercise,
  Boolean>>, unitsById: Map<String, LearningUnit>, sectionsById:
  Map<String, Section>): List<CheckpointSectionBreakdown>` — both consumed
  only by Plan 2's `CheckpointViewModel`/UI, not by anything in this plan.

- [ ] **Step 1: Write the failing time-budget tests**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/CheckpointTimeBudgetTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckpointTimeBudgetTest {

    @Test
    fun `10 questions get an 18 minute budget`() {
        assertEquals(18 * 60, computeCheckpointTimeBudgetSeconds(questionCount = 10))
    }

    @Test
    fun `20 questions get a 36 minute budget`() {
        assertEquals(36 * 60, computeCheckpointTimeBudgetSeconds(questionCount = 20))
    }

    @Test
    fun `8 questions round to the nearest minute`() {
        // 8 * 1.8 = 14.4 minutes -> rounds to 14.
        assertEquals(14 * 60, computeCheckpointTimeBudgetSeconds(questionCount = 8))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.CheckpointTimeBudgetTest"`
Expected: FAIL — `computeCheckpointTimeBudgetSeconds` is unresolved.

- [ ] **Step 3: Implement the time-budget function**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/CheckpointTimeBudget.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import kotlin.math.roundToInt

private const val MINUTES_PER_QUESTION = 1.8

/**
 * Total time budget for a checkpoint attempt, in seconds -- the real 1Z0-830's pace (50 questions
 * / 90 minutes = 1.8 min/question), applied as one lump sum rather than a per-question timer so a
 * parsons/predict_output question (which naturally takes longer to read) isn't penalized against
 * an mcq.
 */
fun computeCheckpointTimeBudgetSeconds(questionCount: Int): Int =
    (questionCount * MINUTES_PER_QUESTION).roundToInt() * 60
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.CheckpointTimeBudgetTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Write the failing section-breakdown tests**

Create `app/src/test/java/com/zconte/oopsapp/domain/usecase/ComputeCheckpointSectionBreakdownTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeCheckpointSectionBreakdownTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String) = LearningUnit(id, sectionId, id, "objective", 1)
    private fun exercise(id: String, unitId: String) = Exercise(id, unitId, "mcq", "{}", 1)

    @Test
    fun `groups correctness by section and sorts by section order`() {
        val unitsById = mapOf(
            "s1-u1" to unit("s1-u1", "s1"),
            "s2-u1" to unit("s2-u1", "s2")
        )
        val sectionsById = mapOf(
            "s1" to section("s1", 1),
            "s2" to section("s2", 2)
        )
        val results = listOf(
            exercise("s1-ex-1", "s1-u1") to true,
            exercise("s1-ex-2", "s1-u1") to false,
            exercise("s2-ex-1", "s2-u1") to true,
            exercise("s2-ex-2", "s2-u1") to true,
            exercise("s2-ex-3", "s2-u1") to false
        )

        val breakdown = computeCheckpointSectionBreakdown(results, unitsById, sectionsById)

        assertEquals(2, breakdown.size)
        assertEquals("s1", breakdown[0].section.id)
        assertEquals(1, breakdown[0].correct)
        assertEquals(2, breakdown[0].total)
        assertEquals("s2", breakdown[1].section.id)
        assertEquals(2, breakdown[1].correct)
        assertEquals(3, breakdown[1].total)
    }

    @Test
    fun `an exercise whose unit or section cannot be resolved is silently excluded`() {
        val results = listOf(Exercise("orphan-ex", "unknown-unit", "mcq", "{}", 1) to true)

        val breakdown = computeCheckpointSectionBreakdown(results, unitsById = emptyMap(), sectionsById = emptyMap())

        assertEquals(0, breakdown.size)
    }
}
```

- [ ] **Step 6: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.ComputeCheckpointSectionBreakdownTest"`
Expected: FAIL — `computeCheckpointSectionBreakdown` is unresolved.

- [ ] **Step 7: Implement the section-breakdown function**

Create `app/src/main/java/com/zconte/oopsapp/domain/usecase/ComputeCheckpointSectionBreakdown.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section

data class CheckpointSectionBreakdown(
    val section: Section,
    val correct: Int,
    val total: Int
)

/**
 * Per-section accuracy breakdown for a checkpoint result screen -- so a mandatory gate isn't a
 * black box. Reuses data the caller already has (which unit each answered exercise belongs to,
 * which section each unit belongs to) rather than requiring new persistence.
 */
fun computeCheckpointSectionBreakdown(
    results: List<Pair<Exercise, Boolean>>,
    unitsById: Map<String, LearningUnit>,
    sectionsById: Map<String, Section>
): List<CheckpointSectionBreakdown> =
    results
        .mapNotNull { (exercise, correct) ->
            val unit = unitsById[exercise.unitId] ?: return@mapNotNull null
            val section = sectionsById[unit.sectionId] ?: return@mapNotNull null
            section to correct
        }
        .groupBy({ it.first }, { it.second })
        .map { (section, corrects) ->
            CheckpointSectionBreakdown(section = section, correct = corrects.count { it }, total = corrects.size)
        }
        .sortedBy { it.section.orderIndex }
```

- [ ] **Step 8: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.ComputeCheckpointSectionBreakdownTest"`
Expected: PASS (2 tests).

- [ ] **Step 9: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/CheckpointTimeBudget.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/CheckpointTimeBudgetTest.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/ComputeCheckpointSectionBreakdown.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/ComputeCheckpointSectionBreakdownTest.kt
git commit -m "feat: add pure checkpoint time-budget and section-breakdown functions"
```

---

### Task 7: `GetLearningPathUseCase` — mandatory checkpoint gate + placement equivalence

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`
- Modify: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`
- Modify (mechanical, add a `FakeCheckpointRepository()` argument to every
  `GetLearningPathUseCase(...)` construction — one new required
  constructor parameter):
  `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt`,
  `GetTodaySessionUseCaseTest.kt`, `GetSkippedUnitsUseCaseTest.kt`,
  `app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt`,
  `app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt`

**Interfaces:**
- Consumes: `CheckpointRepository.hasApprovedAttempt` (Task 1).
- Produces: `SectionPath.checkpointSatisfied: Boolean` (new field) and
  `GetLearningPathUseCase`'s new `checkpointRepository` constructor
  parameter — nothing in this plan consumes either further; Plan 2's
  `ProgressScreen`/`HomeViewModel` will read `checkpointSatisfied` to
  render the locked-pending-checkpoint state.

**This is the task with the widest ripple** — `GetLearningPathUseCase`'s
constructor gains a required parameter, which 6 existing test files
construct directly. Two of those files have an assertion whose premise
changes (a section no longer unlocks on units-alone); the other four need
only the mechanical parameter addition. Read each replacement carefully —
they are not all identical.

**Behavior notes for Plan 2 (document only, no code here):**
`SectionPath.completed` keeps its exact current meaning ("every unit in
this section is complete") — unchanged, so `ProgressScreen`'s existing
`if (sectionPath.completed) { CheckpointRow(...) }` continues to work
without any code change: the voluntary-checkpoint entry point still
appears the moment units finish, exactly as before. What changes is
`SectionPath.unlocked` for the *next* section, which now additionally
requires `checkpointSatisfied` — so the next section's units render
locked (🔒) until the checkpoint is passed, using entirely existing
`ProgressScreen` styling, with no code change needed there either.
`GetCurrentUnitUseCase` (unchanged in this task) will return `null` once
a section's units are done but its checkpoint isn't passed yet (there's
no next unlocked unit to serve) — `GetTodaySessionUseCase` already
handles a `null` current unit gracefully (falls back to due-only
reviews), so "Estudiar hoy" simply offers no new content until the
checkpoint is taken, which is the intended behavior of a mandatory gate.

- [ ] **Step 1: Add `checkpointSatisfied` to `SectionPath`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

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
    val checkpointSatisfied: Boolean
)
```

- [ ] **Step 2: Write the failing/updated `GetLearningPathUseCaseTest` tests**

Replace the full content of
`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.CompletedUnit
import com.zconte.oopsapp.domain.model.LearningUnit
import com.zconte.oopsapp.domain.model.Section
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.repository.ContentRepository
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeContentRepositoryForPath(
    private val sections: List<Section>,
    private val unitsBySection: Map<String, List<LearningUnit>>,
    private val completedUnits: List<CompletedUnit>
) : ContentRepository {
    override suspend fun getSections(): List<Section> = sections
    override suspend fun getUnitsBySection(sectionId: String): List<LearningUnit> = unitsBySection[sectionId] ?: emptyList()
    override suspend fun getCompletedUnits(): List<CompletedUnit> = completedUnits
    override suspend fun markUnitCompleted(unitId: String, completedAt: LocalDate, via: String) {}
}

class GetLearningPathUseCaseTest {

    private fun section(id: String, order: Int) = Section(id, id, order, "core")
    private fun unit(id: String, sectionId: String, order: Int) = LearningUnit(id, sectionId, id, "objective", order)
    private fun played(unitId: String) = CompletedUnit(unitId, UnitCompletionSource.PLAYED)

    @Test
    fun `first section and its first unit are always unlocked`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = emptyList()
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository())

        val path = useCase()

        assertTrue(path.first().unlocked)
        assertTrue(path.first().units[0].unlocked)
        assertFalse(path.first().units[1].unlocked)
    }

    @Test
    fun `a unit unlocks once the previous unit in the same section is completed`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2))),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository())

        val path = useCase()

        assertTrue(path.first().units[1].unlocked)
    }

    @Test
    fun `a section unlocks once every unit is complete AND its checkpoint is approved`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 80, passed = true,
            takenAt = LocalDate.of(2026, 7, 20), failedExerciseIds = emptyList()
        )
        val useCase = GetLearningPathUseCase(repository, checkpointRepository)

        val path = useCase()

        assertTrue(path.first().completed)
        assertTrue(path.first().checkpointSatisfied)
        assertTrue(path[1].unlocked)
        assertEquals("s2", path[1].section.id)
    }

    @Test
    fun `a section stays locked when units are done but the checkpoint has not been approved`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository())

        val path = useCase()

        assertTrue(path.first().completed)
        assertFalse(path.first().checkpointSatisfied)
        assertFalse(path[1].unlocked)
    }

    @Test
    fun `a section fully completed via placement satisfies the checkpoint gate without an explicit attempt`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(CompletedUnit("s1-u1", UnitCompletionSource.PLACEMENT))
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository())

        val path = useCase()

        assertTrue(path.first().checkpointSatisfied)
        assertTrue(path[1].unlocked)
    }

    @Test
    fun `a section only partially completed via placement still requires an approved checkpoint`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(
                CompletedUnit("s1-u1", UnitCompletionSource.PLACEMENT),
                CompletedUnit("s1-u2", UnitCompletionSource.PLAYED)
            )
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository())

        val path = useCase()

        assertTrue(path.first().completed)
        assertFalse(path.first().checkpointSatisfied)
        assertFalse(path[1].unlocked)
    }

    @Test
    fun `a section stays locked while the previous section has incomplete units`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1), section("s2", 2)),
            unitsBySection = mapOf(
                "s1" to listOf(unit("s1-u1", "s1", 1), unit("s1-u2", "s1", 2)),
                "s2" to listOf(unit("s2-u1", "s2", 1))
            ),
            completedUnits = listOf(played("s1-u1"))
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository())

        val path = useCase()

        assertFalse(path[1].unlocked)
    }

    @Test
    fun `a unit completed via a placement checkpoint surfaces that source`() = runTest {
        val repository = FakeContentRepositoryForPath(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = listOf(CompletedUnit("s1-u1", UnitCompletionSource.PLACEMENT))
        )
        val useCase = GetLearningPathUseCase(repository, FakeCheckpointRepository())

        val path = useCase()

        assertEquals(UnitCompletionSource.PLACEMENT, path.first().units[0].completedVia)
    }
}
```

- [ ] **Step 3: Run to verify the new/changed tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetLearningPathUseCaseTest"`
Expected: FAIL — compile error, `GetLearningPathUseCase` doesn't yet
accept a `CheckpointRepository`, `SectionPath` doesn't yet have
`checkpointSatisfied`.

- [ ] **Step 4: Implement the gate in `GetLearningPathUseCase`**

Replace the full content of
`app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.model.UnitCompletionSource
import com.zconte.oopsapp.domain.model.UnitProgress
import com.zconte.oopsapp.domain.repository.CheckpointRepository
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

class GetLearningPathUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
    private val checkpointRepository: CheckpointRepository
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
            previousSectionFullyDone = checkpointSatisfied

            SectionPath(section, sectionUnlocked, unitProgress, sectionComplete, checkpointSatisfied)
        }
    }
}
```

- [ ] **Step 5: Fix the 5 other test files (mechanical constructor-parameter addition)**

Each of these constructs `GetLearningPathUseCase(<contentRepositoryFakeVar>)`
directly. Add the import
`import com.zconte.oopsapp.testutil.FakeCheckpointRepository` (if not
already present) and a second constructor argument, `FakeCheckpointRepository()`,
to every call. None of these files' *assertions* need to change — every
fixture in them spans only a single section, so the new cross-section
gate never engages.

`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt`
— 4 of its 5 `GetLearningPathUseCase(repository)` calls need only the
mechanical fix. The 5th (in
`` `current unit crosses into the next section once the previous one is fully complete` ``)
spans two sections and its assertion depends on the gate — replace that
one test's body:

```kotlin
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
        val checkpointRepository = FakeCheckpointRepository()
        checkpointRepository.recordAttempt(
            "s1", CheckpointKind.REVIEW, scorePct = 80, passed = true,
            takenAt = LocalDate.of(2026, 7, 20), failedExerciseIds = emptyList()
        )
        val useCase = GetCurrentUnitUseCase(GetLearningPathUseCase(repository, checkpointRepository))

        val current = useCase()

        assertEquals("s2-u1", current?.id)
    }
```

with the corresponding new imports at the top of the file:

```kotlin
import com.zconte.oopsapp.domain.model.CheckpointKind
import com.zconte.oopsapp.testutil.FakeCheckpointRepository
```

For this file's other 4 tests (first-unit, advances-within-section,
placement-skip, no-current-unit-once-complete — all single-section
fixtures), change every remaining
`GetCurrentUnitUseCase(GetLearningPathUseCase(repository))` call to
`GetCurrentUnitUseCase(GetLearningPathUseCase(repository, FakeCheckpointRepository()))`.

`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`
— all 5 tests use single-section fixtures; every
`GetTodaySessionUseCase(exerciseRepository, GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository)))`
call becomes
`GetTodaySessionUseCase(exerciseRepository, GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository())))`.
Add `import com.zconte.oopsapp.testutil.FakeCheckpointRepository`.

`app/src/test/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCaseTest.kt`
— all 5 tests read only `UnitProgress.completed` (never `.unlocked` at
the section level), so none of their assertions are affected. Every
`GetSkippedUnitsUseCase(GetLearningPathUseCase(repository))` call becomes
`GetSkippedUnitsUseCase(GetLearningPathUseCase(repository, FakeCheckpointRepository()))`.
Add `import com.zconte.oopsapp.testutil.FakeCheckpointRepository`.

`app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt`
— inside the `buildViewModel` helper, the line
`GetTodaySessionUseCase(exerciseRepository, GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository)))`
becomes
`GetTodaySessionUseCase(exerciseRepository, GetCurrentUnitUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository())))`.
Add `import com.zconte.oopsapp.testutil.FakeCheckpointRepository`. No test
in this file spans more than one section, so no assertion changes.

`app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt`
— inside the `buildViewModel` helper, the line
`GetSkippedUnitsUseCase(GetLearningPathUseCase(contentRepository))`
becomes
`GetSkippedUnitsUseCase(GetLearningPathUseCase(contentRepository, FakeCheckpointRepository()))`.
This file already imports `FakeCheckpointRepository` (it's one of the
constructor parameters `buildViewModel` already takes for
`CompleteCheckpointUseCase`), so no new import is needed. No test in this
file spans more than one section (every fixture is one section with 1-2
units), so no assertion changes.

- [ ] **Step 6: Run each affected test file to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetLearningPathUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetCurrentUnitUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetSkippedUnitsUseCaseTest" --tests "com.zconte.oopsapp.ui.session.SessionViewModelTest" --tests "com.zconte.oopsapp.ui.checkpoint.PlacementCheckpointViewModelTest"`
Expected: PASS (all).

- [ ] **Step 7: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — this is the widest-ripple task in the plan,
run the whole suite as the final regression check.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/model/SectionPath.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetLearningPathUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCurrentUnitUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetSkippedUnitsUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/ui/session/SessionViewModelTest.kt \
        app/src/test/java/com/zconte/oopsapp/ui/checkpoint/PlacementCheckpointViewModelTest.kt
git commit -m "feat: gate section unlock on an approved checkpoint (or placement equivalence)"
```

---

## After all tasks: what Plan 2 picks up

This plan has exactly one user-visible effect, active immediately on
merge with no Plan 2 dependency: once a section's units are all done, the
next section now stays locked (🔒, using existing `ProgressScreen`
styling, zero UI code changed) until that section's checkpoint is
approved. The existing "CHECKPOINT" row and the current, untouched
`CheckpointScreen`/`CheckpointViewModel` still work end-to-end today — no
timer, no growing/cumulative content yet applied to what the *screen*
requests (that lands the moment Task 5's `CheckpointViewModel` call-site
fix ships, which is part of *this* plan), no retry-wait enforcement, and
no results breakdown. Passing the checkpoint (at whatever score
`CheckpointScreen` already shows) unlocks the next section correctly.

Everything else built here — `IsCheckpointRetryUnlockedUseCase`, the
time-budget function, the section-breakdown function — is fully tested
but **unused by any ViewModel** until Plan 2 wires it in alongside the
timer display, the results-breakdown screen, and the Ruta indicator that
distinguishes "tap to take the checkpoint" from "you can retry once
you've re-studied your misses."

No manual on-device QA checklist is required for this plan specifically —
reinstall is not needed (no existing behavior a user would notice changes
besides the new lock, which only engages the next time a section is
finished). Confirming the full test suite passes is sufficient; Plan 2's
own QA checklist will cover the end-to-end mandatory-checkpoint experience
manually on-device.
