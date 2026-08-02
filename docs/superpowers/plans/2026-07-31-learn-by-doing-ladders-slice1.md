# Learn-by-doing Ladders — Slice 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove the learn-by-doing ladder engine end-to-end on one pilot unit (`streams-collectors`): first-exposure ladders (worked example → guided → solo), a two-phase daily session (review via SRS + Path in authored order), and composition gating — with no Room migration.

**Architecture:** All ladder metadata (`conceptId`, `role`, `pathOrder`, `dependsOn`) rides on `ExerciseContent`, which is serialized into the existing Room `payload` blob — no schema change. The metadata is decoded once in `ExerciseEntity.toDomain(json)` and surfaced on the domain `Exercise`, so `GetTodaySessionUseCase` stays single-source on `ExerciseRepository` and JSON-free. The `worked_example` intro is a non-tracked didactic card that never routes through `SubmitAnswerUseCase`.

**Tech Stack:** Kotlin, kotlinx.serialization, Hilt, Room, Jetpack Compose, JUnit (pure-JVM domain tests).

**Spec:** `docs/superpowers/specs/2026-07-30-learn-by-doing-ladders-slice1-design.md`
**Vision ADR:** `docs/adrs/2026-07-30-self-teaching-path-vision.md`

## Global Constraints

- **No Room migration.** Ladder fields live in the `payload` JSON blob only. Do not touch `ExerciseEntity`, DAOs, `AppDatabase`, or `Migrations.kt`.
- **Pure domain.** `GetTodaySessionUseCase` and domain models import no `android.*`; they must be testable on the JVM.
- **Backward compatibility.** All new fields are optional/defaulted; every existing content pack and existing serialized payload must still parse (`Json { ignoreUnknownKeys = true }` is the app's shared instance).
- **Content language:** Spanish, **no accented characters** (matches existing content).
- **New exercise-type string value:** `"worked_example"`. **Role string values:** `"intro" | "guided" | "solo" | "practice"`.
- **Base package:** `com.zconte.oopsapp`.
- **Reuse the shared `Json`** from `di/SerializationModule.kt` (`provideJson()`), never construct a new one in production code.
- Run JVM tests with `./gradlew testDebugUnitTest`. Confirm compilation of Android UI code with `./gradlew :app:compileDebugKotlin`.

---

### Task 1: Ladder data model — type/role constants + `ExerciseContent` fields + parsing

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseType.kt`
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseRole.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseContent.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt`

**Interfaces:**
- Produces: `ExerciseType.WORKED_EXAMPLE = "worked_example"`; `ExerciseRole` with `INTRO="intro"`, `GUIDED="guided"`, `SOLO="solo"`, `PRACTICE="practice"`; `ExerciseContent` gains `conceptId: String? = null`, `role: String? = null`, `pathOrder: Int? = null`, `dependsOn: List<String> = emptyList()`.

- [ ] **Step 1: Write the failing tests**

Append these two tests inside the `ContentPackParsingTest` class in `app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt` (before the final closing brace):

```kotlin
    @Test
    fun `exercise parses ladder fields conceptId role pathOrder dependsOn`() {
        val raw = """
            {
              "sectionId": "java-streams",
              "name": "Streams y lambdas",
              "orderIndex": 2,
              "examVersion": "java21",
              "units": [
                {
                  "unitId": "streams-collectors",
                  "name": "Collectors avanzados",
                  "certObjective": "streams-lambdas",
                  "orderIndex": 4,
                  "exercises": [
                    {
                      "id": "gb-solo",
                      "type": "fill_blank",
                      "difficulty": 3,
                      "prompt": "Agrupa por longitud:",
                      "code": "stream.collect(Collectors._____(String::length))",
                      "answer": "groupingBy",
                      "explanation": "groupingBy agrupa en un Map.",
                      "conceptId": "collectors-groupingby",
                      "role": "solo",
                      "pathOrder": 2
                    },
                    {
                      "id": "combo-solo",
                      "type": "fill_blank",
                      "difficulty": 4,
                      "prompt": "Particiona y agrupa:",
                      "code": "...",
                      "answer": "x",
                      "explanation": "composicion.",
                      "conceptId": "collectors-partition-then-group",
                      "role": "solo",
                      "pathOrder": 8,
                      "dependsOn": ["collectors-groupingby", "collectors-partitioningby"]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)
        val exercises = pack.units.first().exercises

        assertEquals("collectors-groupingby", exercises[0].conceptId)
        assertEquals("solo", exercises[0].role)
        assertEquals(2, exercises[0].pathOrder)
        assertEquals(emptyList<String>(), exercises[0].dependsOn)
        assertEquals(
            listOf("collectors-groupingby", "collectors-partitioningby"),
            exercises[1].dependsOn
        )
    }

    @Test
    fun `legacy exercise without ladder fields parses with null defaults`() {
        val raw = """
            {
              "sectionId": "java-streams",
              "name": "Streams y lambdas",
              "orderIndex": 2,
              "examVersion": "java21",
              "units": [
                {
                  "unitId": "streams-terminal",
                  "name": "Operaciones terminales",
                  "certObjective": "streams-lambdas",
                  "orderIndex": 1,
                  "exercises": [
                    {
                      "id": "legacy-01",
                      "type": "mcq",
                      "difficulty": 1,
                      "prompt": "Que metodo crea un Stream desde una List?",
                      "answer": "stream",
                      "distractors": ["toStream", "asStream", "of"],
                      "explanation": "List.stream() crea el Stream."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val pack = json.decodeFromString(ContentPack.serializer(), raw)
        val ex = pack.units.first().exercises.first()

        assertEquals(null, ex.conceptId)
        assertEquals(null, ex.role)
        assertEquals(null, ex.pathOrder)
        assertEquals(emptyList<String>(), ex.dependsOn)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.content.ContentPackParsingTest"`
Expected: FAIL — compilation error, `ExerciseContent` has no `conceptId` / `role` / `pathOrder` / `dependsOn`.

- [ ] **Step 3: Add the new type constant**

Edit `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseType.kt` to add the constant:

```kotlin
package com.zconte.oopsapp.domain.model

object ExerciseType {
    const val MCQ = "mcq"
    const val FILL_BLANK = "fill_blank"
    const val PARSONS = "parsons"
    const val PREDICT_OUTPUT = "predict_output"
    const val WORKED_EXAMPLE = "worked_example"
}
```

- [ ] **Step 4: Create the role constants**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseRole.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

/** Role of an exercise within a concept's first-exposure ladder. */
object ExerciseRole {
    const val INTRO = "intro"       // non-tracked worked_example card
    const val GUIDED = "guided"
    const val SOLO = "solo"
    const val PRACTICE = "practice"
}
```

- [ ] **Step 5: Add the ladder fields to `ExerciseContent`**

Edit `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseContent.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseContent(
    val id: String,
    val type: String,
    val difficulty: Int,
    val prompt: String,
    val code: String? = null,
    val answer: String,
    val distractors: List<String> = emptyList(),
    val lines: List<String> = emptyList(),
    val explanation: String,
    // Ladder metadata (payload-only; all optional so legacy content still parses):
    val conceptId: String? = null,
    val role: String? = null,
    val pathOrder: Int? = null,
    val dependsOn: List<String> = emptyList()
)
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.content.ContentPackParsingTest"`
Expected: PASS (the pre-existing tests plus the 2 new ones).

- [ ] **Step 7: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseType.kt \
        app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseRole.kt \
        app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseContent.kt \
        app/src/test/java/com/zconte/oopsapp/data/content/ContentPackParsingTest.kt
git commit -m "feat: add ladder metadata fields to the content schema"
```

---

### Task 2: Enrich domain `Exercise` and decode ladder metadata in the mapping

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/model/Exercise.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/data/repository/ExerciseEntityMappingTest.kt`

**Interfaces:**
- Consumes: `ExerciseContent` fields from Task 1; the shared `Json` from `provideJson()`.
- Produces: domain `Exercise` gains `conceptId: String? = null`, `role: String? = null`, `pathOrder: Int? = null`, `dependsOn: List<String> = emptyList()`; `internal fun ExerciseEntity.toDomain(json: Json): Exercise` populates them from the payload. `ExerciseRepositoryImpl` constructor gains `private val json: Json`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/zconte/oopsapp/data/repository/ExerciseEntityMappingTest.kt`:

```kotlin
package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseEntityMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun entity(payload: String) = ExerciseEntity(
        id = "e1", unitId = "u1", type = "fill_blank",
        payload = payload, difficulty = 2, examVersion = "core"
    )

    @Test
    fun `maps ladder fields from the payload onto the domain exercise`() {
        val payload = """
            {"id":"e1","type":"fill_blank","difficulty":2,"prompt":"p",
             "answer":"a","explanation":"x",
             "conceptId":"collectors-groupingby","role":"solo","pathOrder":2,
             "dependsOn":["dep-a","dep-b"]}
        """.trimIndent()

        val domain = entity(payload).toDomain(json)

        assertEquals("collectors-groupingby", domain.conceptId)
        assertEquals("solo", domain.role)
        assertEquals(2, domain.pathOrder)
        assertEquals(listOf("dep-a", "dep-b"), domain.dependsOn)
    }

    @Test
    fun `legacy payload without ladder fields maps to null defaults`() {
        val payload = """
            {"id":"e1","type":"fill_blank","difficulty":2,"prompt":"p",
             "answer":"a","explanation":"x"}
        """.trimIndent()

        val domain = entity(payload).toDomain(json)

        assertEquals(null, domain.conceptId)
        assertEquals(null, domain.role)
        assertEquals(null, domain.pathOrder)
        assertEquals(emptyList<String>(), domain.dependsOn)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.repository.ExerciseEntityMappingTest"`
Expected: FAIL — `toDomain(json)` is private/absent and `Exercise` has no `conceptId`.

- [ ] **Step 3: Add the ladder fields to the domain `Exercise`**

Edit `app/src/main/java/com/zconte/oopsapp/domain/model/Exercise.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

data class Exercise(
    val id: String,
    val unitId: String,
    val type: String,
    val payload: String,
    val difficulty: Int,
    val examVersion: String = "core",
    val conceptId: String? = null,
    val role: String? = null,
    val pathOrder: Int? = null,
    val dependsOn: List<String> = emptyList()
)
```

- [ ] **Step 4: Decode the payload in the mapping and inject `Json`**

In `app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt`:

(a) Add the import and the `json` constructor parameter:

```kotlin
import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json
```

```kotlin
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val reviewStateDao: ReviewStateDao,
    private val json: Json
) : ExerciseRepository {
```

(b) Update every `it.toDomain()` / `?.toDomain()` call on an `ExerciseEntity` to pass `json` — the three exercise queries:

```kotlin
    override suspend fun getDueExercises(today: LocalDate, limit: Int): List<Exercise> =
        exerciseDao.getDue(today.toEpochDay()).take(limit).map { it.toDomain(json) }

    override suspend fun getExercisesByUnit(unitId: String): List<Exercise> =
        exerciseDao.getByUnit(unitId).map { it.toDomain(json) }

    override suspend fun getExercisesBySection(sectionId: String): List<Exercise> =
        exerciseDao.getBySection(sectionId).map { it.toDomain(json) }
```

(Leave `getReviewState`'s `?.toDomain()` — that is the `ReviewStateEntity.toDomain()`, a different function, untouched.)

(c) Replace the private `ExerciseEntity.toDomain()` extension with an `internal` one that decodes the payload:

```kotlin
internal fun ExerciseEntity.toDomain(json: Json): Exercise {
    val content = json.decodeFromString(ExerciseContent.serializer(), payload)
    return Exercise(
        id = id,
        unitId = unitId,
        type = type,
        payload = payload,
        difficulty = difficulty,
        examVersion = examVersion,
        conceptId = content.conceptId,
        role = content.role,
        pathOrder = content.pathOrder,
        dependsOn = content.dependsOn
    )
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.data.repository.ExerciseEntityMappingTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS. (Hilt injects the already-provided `Json` into `ExerciseRepositoryImpl`; no DI wiring change needed because `provideJson()` exists in `di/SerializationModule.kt`.)

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/model/Exercise.kt \
        app/src/main/java/com/zconte/oopsapp/data/repository/ExerciseRepositoryImpl.kt \
        app/src/test/java/com/zconte/oopsapp/data/repository/ExerciseEntityMappingTest.kt
git commit -m "feat: surface ladder metadata on the domain Exercise via the payload"
```

---

### Task 3: `GetTodaySessionUseCase` — two-phase Path selection

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`
- Test: `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`

**Interfaces:**
- Consumes: enriched `Exercise` (Task 2); `ExerciseRepository.getDueExercises`, `getExercisesByUnit`, `getAnsweredExerciseIds`; `GetCurrentUnitUseCase`; `ExerciseRole`.
- Produces: `GetTodaySessionUseCase.invoke(today, newExercisesLimit=5): List<Exercise>` = due (Phase B) + Path selection (Phase A), where Phase A filters by **concept-born**, orders by `pathOrder`, and gates composition concepts on their `dependsOn`.

- [ ] **Step 1: Write the failing tests**

The existing test file has a local `FakeExerciseRepositoryForSession` and an `exercise(id, unitId)` helper. First, **replace** that helper with one that accepts ladder fields, then add the new tests. In `app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt`:

(a) Replace the existing `exercise` helper line:

```kotlin
    private fun exercise(id: String, unitId: String = "s1-u1") = Exercise(id, unitId, "fill_blank", "{}", 1)
```

with:

```kotlin
    private fun exercise(
        id: String,
        unitId: String = "s1-u1",
        type: String = "fill_blank",
        conceptId: String? = null,
        role: String? = null,
        pathOrder: Int? = null,
        dependsOn: List<String> = emptyList()
    ) = Exercise(id, unitId, type, "{}", 1, "core", conceptId, role, pathOrder, dependsOn)
```

(b) Add these tests inside the `GetTodaySessionUseCaseTest` class:

```kotlin
    private fun currentUnitUseCase(
        contentRepository: FakeContentRepositoryForTodaySession,
        exerciseRepository: ExerciseRepository
    ) = GetCurrentUnitUseCase(
        GetLearningPathUseCase(
            contentRepository, FakeCheckpointRepository(),
            IsCheckpointRetryUnlockedUseCase(FakeCheckpointRepository(), exerciseRepository)
        )
    )

    @Test
    fun `phase A orders new ladder exercises by pathOrder`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(
                    exercise("gb-solo", conceptId = "gb", role = "solo", pathOrder = 2),
                    exercise("gb-intro", type = "worked_example", conceptId = "gb", role = "intro", pathOrder = 0),
                    exercise("gb-guided", conceptId = "gb", role = "guided", pathOrder = 1)
                )
            )
        )
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        val result = useCase(today)

        assertEquals(listOf("gb-intro", "gb-guided", "gb-solo"), result.map { it.id })
    }

    @Test
    fun `a born concept is dropped from phase A including its intro`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        // gb-solo is answered => concept "gb" is born => the whole gb ladder (intro included) is dropped.
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(
                    exercise("gb-intro", type = "worked_example", conceptId = "gb", role = "intro", pathOrder = 0),
                    exercise("gb-guided", conceptId = "gb", role = "guided", pathOrder = 1),
                    exercise("gb-solo", conceptId = "gb", role = "solo", pathOrder = 2),
                    exercise("pb-intro", type = "worked_example", conceptId = "pb", role = "intro", pathOrder = 3),
                    exercise("pb-solo", conceptId = "pb", role = "solo", pathOrder = 4)
                )
            ),
            answeredIds = setOf("gb-solo")
        )
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        val result = useCase(today)

        assertEquals(listOf("pb-intro", "pb-solo"), result.map { it.id })
    }

    @Test
    fun `a composition concept is gated until all its dependencies are born`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        // "gb" is born; "pb" is NOT. Composition "combo" depends on both => must be skipped.
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(
                    exercise("gb-solo", conceptId = "gb", role = "solo", pathOrder = 2),
                    exercise("pb-solo", conceptId = "pb", role = "solo", pathOrder = 4),
                    exercise("combo-solo", conceptId = "combo", role = "solo", pathOrder = 8,
                        dependsOn = listOf("gb", "pb"))
                )
            ),
            answeredIds = setOf("gb-solo")
        )
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        val result = useCase(today)

        // gb is born (dropped); pb is offered; combo is gated out because pb is not born yet.
        assertEquals(listOf("pb-solo"), result.map { it.id })
    }

    @Test
    fun `a composition concept appears once all dependencies are born`() = runTest {
        val contentRepository = FakeContentRepositoryForTodaySession(
            sections = listOf(section("s1", 1)),
            unitsBySection = mapOf("s1" to listOf(unit("s1-u1", "s1", 1))),
            completedUnits = emptyList()
        )
        val exerciseRepository = FakeExerciseRepositoryForSession(
            exercisesByUnit = mapOf(
                "s1-u1" to listOf(
                    exercise("gb-solo", conceptId = "gb", role = "solo", pathOrder = 2),
                    exercise("pb-solo", conceptId = "pb", role = "solo", pathOrder = 4),
                    exercise("combo-solo", conceptId = "combo", role = "solo", pathOrder = 8,
                        dependsOn = listOf("gb", "pb"))
                )
            ),
            answeredIds = setOf("gb-solo", "pb-solo")
        )
        val useCase = GetTodaySessionUseCase(exerciseRepository, currentUnitUseCase(contentRepository, exerciseRepository))

        val result = useCase(today)

        // both deps born => gb & pb dropped, only the composition remains.
        assertEquals(listOf("combo-solo"), result.map { it.id })
    }
```

Keep the file's existing tests unchanged — they must still pass (legacy `conceptId == null` exercises behave as before).

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCaseTest"`
Expected: FAIL — the new ordering/gating assertions fail (current use case ignores `pathOrder`/`dependsOn`/roles).

- [ ] **Step 3: Implement the two-phase selection**

Replace the body of `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt`:

```kotlin
package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.Exercise
import com.zconte.oopsapp.domain.model.ExerciseRole
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import java.time.LocalDate
import javax.inject.Inject

class GetTodaySessionUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val getCurrentUnitUseCase: GetCurrentUnitUseCase
) {
    suspend operator fun invoke(today: LocalDate, newExercisesLimit: Int = 5): List<Exercise> {
        // Phase B — review: everything due today (unaided SRS).
        val due = exerciseRepository.getDueExercises(today, limit = Int.MAX_VALUE)

        // Phase A — Path: advance the current unit in authored order.
        val currentUnit = getCurrentUnitUseCase()
        val new = currentUnit?.let { unit ->
            val unitExercises = exerciseRepository.getExercisesByUnit(unit.id)
            val answeredIds = exerciseRepository
                .getAnsweredExerciseIds(unitExercises.map { it.id })
                .toSet()
            selectPathExercises(unitExercises, answeredIds, newExercisesLimit)
        } ?: emptyList()

        return due + new
    }

    /**
     * Phase-A selection. A concept is "born" once any of its real (non-intro)
     * exercises has been answered; born concepts are dropped entirely (intro
     * card included). Composition concepts (non-empty dependsOn) are gated
     * until all their dependency concepts are born. Legacy exercises
     * (conceptId == null) keep today's behavior: offered when unanswered.
     * Results are ordered by pathOrder (legacy/null last, original order kept).
     */
    private fun selectPathExercises(
        unitExercises: List<Exercise>,
        answeredIds: Set<String>,
        limit: Int
    ): List<Exercise> {
        val bornConceptIds = unitExercises
            .filter { it.conceptId != null && it.role != ExerciseRole.INTRO && it.id in answeredIds }
            .mapNotNull { it.conceptId }
            .toSet()

        val candidates = unitExercises.filter { ex ->
            val concept = ex.conceptId
            if (concept == null) {
                ex.id !in answeredIds
            } else {
                concept !in bornConceptIds && ex.dependsOn.all { it in bornConceptIds }
            }
        }

        return candidates
            .sortedBy { it.pathOrder ?: Int.MAX_VALUE }
            .take(limit)
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.GetTodaySessionUseCaseTest"`
Expected: PASS (the 5 pre-existing tests + the 4 new ones).

- [ ] **Step 5: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetTodaySessionUseCaseTest.kt
git commit -m "feat: two-phase Path selection with concept-born filter and composition gating"
```

---

### Task 4: Exclude `worked_example` cards from answerable-question consumers

The intro cards are seeded into the `exercises` table like any other row, so every consumer that iterates "all exercises in a unit/section" as **answerable questions** would break on them. Most critically, `MarkUnitProgressUseCase` marks a unit complete only when *every* exercise is answered — intro cards never are, so the pilot unit could never complete and its checkpoint would never unlock (the classic "Path stalls" failure). The `getDue` path is already safe (it inner-joins `review_state`, which intro cards never have), so only the iterate-all-exercises consumers need the filter.

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseFilters.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCase.kt:13`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCase.kt:10`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCase.kt:14`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCase.kt:32-33`
- Modify: `app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt:54`
- Test: `MarkUnitProgressUseCaseTest.kt`, `GetUnitSessionUseCaseTest.kt`, `GetPlacementCheckpointSessionUseCaseTest.kt`, `GetCheckpointSessionUseCaseTest.kt`

**Interfaces:**
- Consumes: `ExerciseType.WORKED_EXAMPLE` (Task 1); domain `Exercise`.
- Produces: `fun List<Exercise>.answerableOnly(): List<Exercise>` (excludes `worked_example`). `GetTodaySessionUseCase` deliberately does **not** use it (Phase A needs the intro cards).

- [ ] **Step 1: Write the failing tests**

Add to `MarkUnitProgressUseCaseTest` (inside the class):

```kotlin
    @Test
    fun `worked_example cards are excluded from the completion check`() = runTest {
        val exerciseRepository = FakeExerciseRepositoryForUnitProgress(
            exercisesByUnit = mapOf(
                "unit-1" to listOf(
                    Exercise("intro-1", "unit-1", "worked_example", "{}", 1),
                    exercise("solo-1")
                )
            ),
            answeredIds = setOf("solo-1") // the intro is never answered
        )
        val contentRepository = FakeContentRepositoryForUnitProgress()
        val useCase = MarkUnitProgressUseCase(exerciseRepository, contentRepository)

        useCase("unit-1", today)

        assertEquals(listOf("unit-1"), contentRepository.markedComplete)
    }
```

Add to `GetUnitSessionUseCaseTest` (inside the class):

```kotlin
    @Test
    fun `worked_example cards are excluded from a unit replay session`() = runTest {
        val intro = Exercise("intro-1", "unit-1", "worked_example", "{}", 1)
        val solo = Exercise("solo-1", "unit-1", "fill_blank", "{}", 1)
        val repository = FakeExerciseRepositoryForUnitSession(mapOf("unit-1" to listOf(intro, solo)))
        val useCase = GetUnitSessionUseCase(repository)

        val result = useCase("unit-1")

        assertEquals(listOf("solo-1"), result.map { it.id })
    }
```

Add to `GetPlacementCheckpointSessionUseCaseTest` (inside the class):

```kotlin
    @Test
    fun `worked_example cards are never sampled as placement questions`() = runTest {
        val pool = listOf(
            Exercise("u1-intro", "u1", "worked_example", "{}", 1),
            Exercise("u1-q1", "u1", "mcq", "{}", 1),
            Exercise("u1-q2", "u1", "mcq", "{}", 1)
        )
        val repository = FakeExerciseRepositoryForPlacementSession(mapOf("u1" to pool))
        val useCase = GetPlacementCheckpointSessionUseCase(repository)

        val result = useCase(listOf("u1"))

        assertTrue(result.none { it.id == "u1-intro" })
    }
```

Add to `GetCheckpointSessionUseCaseTest` (inside the class):

```kotlin
    @Test
    fun `worked_example cards are never sampled into a checkpoint`() = runTest {
        val currentPool = listOf(Exercise("s1-intro", "s1-unit", "worked_example", "{}", 1)) +
            (1..15).map { exercise("s1-ex-$it", "s1-unit") }
        val exerciseRepository = FakeExerciseRepositoryForCheckpoint(bySection = mapOf("s1" to currentPool))
        val contentRepository = FakeContentRepositoryForCheckpoint(listOf(section("s1", 1)))
        val useCase = GetCheckpointSessionUseCase(exerciseRepository, contentRepository)

        val result = useCase("s1", today)

        assertTrue(result.none { it.id == "s1-intro" })
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetUnitSessionUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetPlacementCheckpointSessionUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCaseTest"`
Expected: FAIL — `answerableOnly` is unresolved and the intro cards leak into the results.

- [ ] **Step 3: Create the filter helper**

Create `app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseFilters.kt`:

```kotlin
package com.zconte.oopsapp.domain.model

/**
 * Exercises that expect an answer. Excludes non-tracked didactic cards
 * (worked_example intros), which are seeded into the exercises table but must
 * never be counted toward unit completion or sampled as assessment questions.
 * Note: GetTodaySessionUseCase does NOT use this — Phase A must surface intros.
 */
fun List<Exercise>.answerableOnly(): List<Exercise> =
    filter { it.type != ExerciseType.WORKED_EXAMPLE }
```

- [ ] **Step 4: Apply the filter at each answerable consumer**

`MarkUnitProgressUseCase.kt` — change the first line of `invoke`:

```kotlin
        val exercises = exerciseRepository.getExercisesByUnit(unitId).answerableOnly()
```

`GetUnitSessionUseCase.kt` — change the body:

```kotlin
    suspend operator fun invoke(unitId: String): List<Exercise> =
        exerciseRepository.getExercisesByUnit(unitId).answerableOnly()
```

`GetPlacementCheckpointSessionUseCase.kt` — change the `pool` line:

```kotlin
        val pool = skippedUnitIds.flatMap { exerciseRepository.getExercisesByUnit(it) }.answerableOnly()
```

`GetCheckpointSessionUseCase.kt` — change the two pool lines:

```kotlin
        val currentPool = exerciseRepository.getExercisesBySection(sectionId).answerableOnly()
        val priorPool = sections.take(currentIndex)
            .flatMap { exerciseRepository.getExercisesBySection(it.id) }
            .answerableOnly()
```

`CompleteCheckpointUseCase.kt` — change the `forEach` source inside `unlockSkippedUnits`:

```kotlin
            exerciseRepository.getExercisesByUnit(unitId).answerableOnly().forEach { exercise ->
```

Add `import com.zconte.oopsapp.domain.model.answerableOnly` to each of the five files.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.zconte.oopsapp.domain.usecase.MarkUnitProgressUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetUnitSessionUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetPlacementCheckpointSessionUseCaseTest" --tests "com.zconte.oopsapp.domain.usecase.GetCheckpointSessionUseCaseTest"`
Expected: PASS (new tests + all pre-existing ones in those files).

- [ ] **Step 6: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/domain/model/ExerciseFilters.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCase.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCase.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCase.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCase.kt \
        app/src/main/java/com/zconte/oopsapp/domain/usecase/CompleteCheckpointUseCase.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/MarkUnitProgressUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetUnitSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetPlacementCheckpointSessionUseCaseTest.kt \
        app/src/test/java/com/zconte/oopsapp/domain/usecase/GetCheckpointSessionUseCaseTest.kt
git commit -m "fix: exclude worked_example cards from completion and checkpoint sampling"
```

---

### Task 5: `worked_example` intro card in the session UI

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`

**Interfaces:**
- Consumes: `ExerciseType.WORKED_EXAMPLE` (Task 1); the existing `ExerciseAnswerCard(state, onSubmit, onNext)` and `CodeBlock`.
- Produces: for a `worked_example` exercise, `ExerciseAnswerCard` renders code + explanation + a single "CONTINUAR" button that calls `onNext()` and never `onSubmit()` (so it never routes through `SubmitAnswerUseCase`).

**Note:** No unit test (UI is Level 2, not started per the testing ADR). Verified by compilation + manual QA.

- [ ] **Step 1: Add the worked-example early branch**

At the top of the `ExerciseAnswerCard` composable body, `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`, immediately after `val exercise = state.exercise` (the first line that reads the exercise off `state`), add an early branch that short-circuits the normal answer flow:

```kotlin
    if (exercise.type == ExerciseType.WORKED_EXAMPLE) {
        WorkedExampleCard(
            exercise = exercise,
            currentIndex = state.currentIndex,
            totalExercises = state.totalExercises,
            onNext = onNext,
            modifier = modifier
        )
        return
    }
```

If the composable does not already bind `val exercise = state.exercise` at the top, add that line first (mirroring how the rest of the function reads `state.exercise`). Ensure `import com.zconte.oopsapp.domain.model.ExerciseType` is present.

- [ ] **Step 2: Add the `WorkedExampleCard` composable**

Add this private composable at the bottom of the same file (it reuses `CodeBlock`, already imported/used in this file, and the same `Button`/`ButtonDefaults`/`MaterialTheme` imports already present):

```kotlin
@Composable
private fun WorkedExampleCard(
    exercise: com.zconte.oopsapp.domain.model.ExerciseContent,
    currentIndex: Int,
    totalExercises: Int,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Ejemplo $currentIndex de $totalExercises",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = exercise.prompt, style = MaterialTheme.typography.titleMedium)
        exercise.code?.let { code ->
            CodeBlock(code = code, filledAnswer = null, modifier = Modifier.fillMaxWidth())
        }
        Text(text = exercise.explanation, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("CONTINUAR")
        }
    }
}
```

Confirm these imports exist at the top of the file (add any that are missing):
`androidx.compose.foundation.layout.Arrangement`, `androidx.compose.foundation.layout.Column`, `androidx.compose.foundation.layout.fillMaxWidth`, `androidx.compose.foundation.layout.spacedBy` (via `Arrangement.spacedBy`), `androidx.compose.material3.Button`, `androidx.compose.material3.ButtonDefaults`, `androidx.compose.material3.MaterialTheme`, `androidx.compose.material3.Text`, `androidx.compose.runtime.Composable`, `androidx.compose.ui.Modifier`, `androidx.compose.ui.unit.dp`.

- [ ] **Step 2b: Verify `CodeBlock`'s parameter name**

`CodeBlock` is already called elsewhere in this file as `CodeBlock(code = code, filledAnswer = ..., modifier = ...)`. Confirm the call above matches that exact signature; if `filledAnswer` is non-nullable in `CodeBlock`, pass the same "no answer" value the file already uses for non-fill-blank types instead of `null`.

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt
git commit -m "feat: render worked_example intro cards in the session"
```

---

### Task 6: Pilot content — re-author `streams-collectors` as ladders

**Files:**
- Modify: `app/src/main/assets/content/streams.json` (the `streams-collectors` unit only)

**Interfaces:**
- Consumes: the schema from Task 1 (`conceptId`, `role`, `pathOrder`, `dependsOn`, `type: "worked_example"`).
- Produces: the pilot unit with 2 base ladders (`collectors-groupingby`, `collectors-partitioningby`) and 1 composition concept (`collectors-partition-then-group`).

**Grandfather note:** existing ids `streams-14`, `streams-19`, `streams-parsons-02` are **preserved** (so any live `ReviewState` is kept) and tagged as the `solo`/`practice` of their concept. `streams-19` (groupingBy) becomes `collectors-groupingby`'s `solo`.

- [ ] **Step 1: Replace the `streams-collectors` unit's `exercises` array**

In `app/src/main/assets/content/streams.json`, keep the unit's `unitId`/`name`/`certObjective`/`orderIndex`/`summary` as-is, and replace its `exercises` array with the following (Spanish, no accents). `pathOrder` is unit-global; concepts are contiguous; each concept's `intro` comes first.

```json
      "exercises": [
        {
          "id": "collectors-gb-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Agrupar una lista por una propiedad con Collectors.groupingBy",
          "code": "Map<String, List<User>> porDepto = users.stream()\n    .collect(Collectors.groupingBy(User::getDepartment));\n// { \"Ventas\": [ana, luis], \"IT\": [sofia] }",
          "answer": "ok",
          "explanation": "groupingBy devuelve un Map: cada clave es el criterio y su valor es la lista de elementos de ese grupo.",
          "conceptId": "collectors-groupingby",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "collectors-gb-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Completa el metodo para agrupar por departamento (pista: agrupar -> grouping...):",
          "code": "users.stream().collect(Collectors._____(User::getDepartment))",
          "answer": "groupingBy",
          "distractors": ["partitioningBy"],
          "explanation": "groupingBy agrupa por una clave derivada; partitioningBy solo divide en true/false.",
          "conceptId": "collectors-groupingby",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "streams-19",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Agrupa los elementos por una propiedad derivada:",
          "code": "stream.collect(Collectors._____(String::length))",
          "answer": "groupingBy",
          "distractors": ["joining", "toMap", "partitioningBy"],
          "explanation": "Collectors.groupingBy() agrupa elementos en un Map segun la funcion clasificadora.",
          "conceptId": "collectors-groupingby",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "collectors-pb-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Dividir en dos grupos con Collectors.partitioningBy",
          "code": "Map<Boolean, List<User>> porSueldo = users.stream()\n    .collect(Collectors.partitioningBy(u -> u.getSalary() > 1000));\n// { false: [...], true: [...] }",
          "answer": "ok",
          "explanation": "partitioningBy recibe un predicado y devuelve un Map con exactamente dos claves: false y true.",
          "conceptId": "collectors-partitioningby",
          "role": "intro",
          "pathOrder": 3
        },
        {
          "id": "collectors-pb-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Completa el metodo para dividir por un predicado (pista: dividir en 2 -> partitioning...):",
          "code": "users.stream().collect(Collectors._____(u -> u.getSalary() > 1000))",
          "answer": "partitioningBy",
          "distractors": ["groupingBy"],
          "explanation": "partitioningBy divide en true/false; groupingBy agruparia por el valor devuelto.",
          "conceptId": "collectors-partitioningby",
          "role": "guided",
          "pathOrder": 4
        },
        {
          "id": "collectors-pb-solo",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Divide los numeros en pares e impares:",
          "code": "numeros.stream().collect(Collectors._____(n -> n % 2 == 0))",
          "answer": "partitioningBy",
          "distractors": ["groupingBy", "toMap", "joining"],
          "explanation": "partitioningBy(predicado) devuelve un Map<Boolean, List<...>> con las claves false y true.",
          "conceptId": "collectors-partitioningby",
          "role": "solo",
          "pathOrder": 5
        },
        {
          "id": "collectors-combo-intro",
          "type": "worked_example",
          "difficulty": 4,
          "prompt": "Combinar particion y agrupacion (el caso de la entrevista)",
          "code": "users.stream().collect(\n    Collectors.partitioningBy(u -> u.getSalary() > X,\n        Collectors.groupingBy(User::getDepartment)));",
          "answer": "ok",
          "explanation": "partitioningBy acepta un segundo Collector aguas abajo: primero divide por sueldo y dentro de cada mitad agrupa por departamento.",
          "conceptId": "collectors-partition-then-group",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["collectors-groupingby", "collectors-partitioningby"]
        },
        {
          "id": "collectors-combo-solo",
          "type": "parsons",
          "difficulty": 4,
          "prompt": "Ordena las lineas: divide por sueldo (> X) y dentro de cada grupo agrupa por departamento:",
          "lines": [
            "users.stream().collect(",
            "Collectors.partitioningBy(u -> u.getSalary() > X,",
            "Collectors.groupingBy(User::getDepartment)));"
          ],
          "answer": "users.stream().collect(\nCollectors.partitioningBy(u -> u.getSalary() > X,\nCollectors.groupingBy(User::getDepartment)));",
          "code": "users.stream().collect(\nCollectors.partitioningBy(u -> u.getSalary() > X,\nCollectors.groupingBy(User::getDepartment)));",
          "explanation": "partitioningBy con un groupingBy aguas abajo particiona y luego agrupa dentro de cada particion.",
          "conceptId": "collectors-partition-then-group",
          "role": "solo",
          "pathOrder": 7,
          "dependsOn": ["collectors-groupingby", "collectors-partitioningby"]
        },
        {
          "id": "streams-14",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Une los elementos del stream en un String separado por coma:",
          "code": "stream.collect(Collectors._____(\", \"))",
          "answer": "joining",
          "distractors": ["toList", "toSet", "groupingBy"],
          "explanation": "Collectors.joining() concatena Strings con el separador dado.",
          "conceptId": "collectors-joining",
          "role": "solo",
          "pathOrder": 8
        },
        {
          "id": "streams-parsons-02",
          "type": "parsons",
          "difficulty": 3,
          "prompt": "Ordena las lineas para pasar las palabras a mayusculas y juntarlas en una lista:",
          "lines": ["palabras.stream()", ".map(String::toUpperCase)", ".collect(Collectors.toList())"],
          "answer": "palabras.stream()\n.map(String::toUpperCase)\n.collect(Collectors.toList())",
          "code": "palabras.stream()\n.map(String::toUpperCase)\n.collect(Collectors.toList())",
          "explanation": "map() transforma cada elemento y collect(Collectors.toList()) los acumula en una lista nueva.",
          "conceptId": "collectors-tolist",
          "role": "solo",
          "pathOrder": 9
        }
      ]
```

- [ ] **Step 2: Validate the JSON**

Run: `python3 -m json.tool app/src/main/assets/content/streams.json > /dev/null && echo OK`
Expected: `OK` (no parse error).

- [ ] **Step 3: Check for accented characters in the file**

Run: `LC_ALL=C grep -nP "[\x80-\xFF]" app/src/main/assets/content/streams.json || echo "no accents"`
Expected: `no accents`.

- [ ] **Step 4: Run the full JVM suite as a regression check**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/content/streams.json
git commit -m "content: re-author streams-collectors as first-exposure ladders"
```

---

> **Amendment (post-implementation, final review):** `CURRENT_CONTENT_VERSION` bump added to `ContentSeeder.kt` (was missed in the original Task 6 scope) so this slice's content actually reaches an existing install. Manual QA step 4 rewritten below to test that path directly (in-place upgrade) instead of a reinstall, which would have destroyed the very `ReviewState` step 4 needs to verify.

## Manual QA (on device, after all tasks)

The engine re-seeds content when `ContentSeeder.CURRENT_CONTENT_VERSION` changes (bumped to `"7"` for this slice) — this fires on a fresh install, AND on an in-place app upgrade over an existing install (the common real-world path: existing `review_state`/`unit_progress` rows survive, only section/unit/exercise content rows are replaced). Two distinct scenarios below exercise the two paths.

1. **Clean install → ladder from scratch.** Reach `streams-collectors` in Ver Ruta. In "Estudiar Hoy", the groupingBy concept appears as: worked-example intro (code + explanation + CONTINUAR, no scoring) → guided mcq (with hint, 2 options) → solo fill_blank. No penalty on the intro.
2. **Composition gating.** The interview-case composition (`collectors-partition-then-group`) does **not** appear until both `groupingBy` and `partitioningBy` have been answered at least once; after that, it appears.
3. **Born concept drops its intro.** After answering a concept's solo, re-entering "Estudiar Hoy" does not show that concept's worked-example intro again.
4. **Grandfather (in-place upgrade, existing progress).** Install the previous build (content version 6), answer `streams-19` (groupingBy's exercise under the old, non-ladder content), then install this build (content version 7) over it **without clearing app data**. Confirm the re-seed happens (Ver Ruta shows the new ladder content) and `streams-19`'s prior `ReviewState` is preserved (it does not reset to a fresh due date) — and confirm the `collectors-groupingby` intro/guided are skipped (the concept is already born from the old answer), so the player is not sent back through scaffolding they already earned past.
5. **`worked_example` never schedules a review.** An intro card advances with CONTINUAR and never shows a correct/incorrect result.

## Out of scope for this plan (tracked)

- Per-**day** new-material cap + "nothing new today" state → **slice 1b**.
- Re-authoring the other 18 units → slice 2+.
- The other pilot-unit concepts (`collectors-joining`, `collectors-tolist`) are tagged as single-`solo` concepts for consistency but are not full ladders; that is intentional for slice 1.