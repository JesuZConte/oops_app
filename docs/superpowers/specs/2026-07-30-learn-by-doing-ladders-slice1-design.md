# Learn-by-doing ladders — Slice 1 (pilot) — Design

**Status:** Approved, pending implementation plan.
**Vision ADR:** `docs/adrs/2026-07-30-self-teaching-path-vision.md`.
**Scope of this slice:** prove the full engine end-to-end on **one pilot unit**
(`streams-collectors`, "Collectors avanzados"), QA'd on device. It does NOT
re-author the other 18 units (that is slice 2+).

## Context

The ADR decides the shift to a self-sufficient, Duolingo-style Path: learn by
doing, with **first-exposure ladders** (model C) and a daily session in **two
phases** (Learn the Path / Review with SRS). This slice builds the minimal
mechanism that makes that real in a single unit, including the **composition**
demonstration (the interview case: `groupingBy` + `partitioningBy`).

Relevant code state:
- `GetTodaySessionUseCase` already pulls "new" items from the **current unit**
  (`getExercisesByUnit`), filtering out already-answered ones. The rework is to
  *order* that new set as a ladder + decide graduation, not to rewrite the
  engine.
- `ExerciseEntity.payload` is an **opaque JSON blob** (constitution: hard
  constraints). Ladder fields travel **inside the payload** → **no Room
  migration**, same as the in-progress `summary` field.
- The unit-level `summary` field (in-progress work, `UnitSummaryPack`) is kept
  with a dual role (reference on the Path screen + unit intro); this slice does
  not redesign it, only assumes it exists.

## 1. Data model: concept + ladder

Each unit gains an optional `concepts` list (concept metadata), and each
exercise references its concept and its role in the ladder. All inside the
content JSON — **no Room changes, no migration**.

### `ContentPack.kt`

```kotlin
@Serializable
data class ConceptPack(
    val conceptId: String,
    val name: String,
    val dependsOn: List<String> = emptyList()  // COMPOSITION hook
)

@Serializable
data class UnitPack(
    val unitId: String,
    val name: String,
    val certObjective: String,
    val orderIndex: Int,
    val summary: UnitSummaryPack? = null,       // unchanged (dual role)
    val concepts: List<ConceptPack> = emptyList(),  // NEW, optional
    val exercises: List<ExerciseContent>
)
```

### `ExerciseContent` (travels in the payload, no new column)

```kotlin
@Serializable
data class ExerciseContent(
    val id: String,
    val type: String,               // + new value "worked_example"
    val difficulty: Int,
    val prompt: String,
    val code: String? = null,
    val answer: String,
    val distractors: List<String> = emptyList(),
    val lines: List<String> = emptyList(),
    val explanation: String,
    // NEW (all optional → legacy content still parses):
    val conceptId: String? = null,  // which concept it belongs to
    val role: String? = null,       // "intro" | "guided" | "solo" | "practice"
    val orderInConcept: Int? = null // order within the ladder
)
```

**A concept's ladder** = the unit's exercises sharing the same `conceptId`,
ordered by `orderInConcept`: `intro` → `guided`(one or more) → `solo`.
`practice` = extra exercises for the concept that appear **only** in review
(Phase B) after graduation.

**Legacy:** an exercise with `conceptId == null` (all current content except
the pilot) is treated as a **standalone, already-born** item — it never goes
through a ladder, it enters the SRS pool directly as today.

## 2. Graduation rule and entry into SRS

- The **`intro` and `guided`** steps are **one-time** scaffolding: they show in
  Phase A, and when answered they are marked "consumed" but **do NOT create a
  `ReviewState`** → they never reappear as due in Phase B.
- The **`solo`** step (and `practice`) DO create a `ReviewState` with normal
  SM-2 when answered.
- **Graduation = when the `solo` step is *answered* for the first time, pass or
  fail.** Failing the `solo` is a normal SM-2 fail: it reschedules for tomorrow
  (the usual "oops" mechanic), but it still graduates. A concept is "born" once
  its `solo` received its first answer (equivalently: once a `ReviewState`
  exists for it — the same condition the grandfathering in §5 uses).

Implementation: `SubmitAnswerUseCase` creates/updates a `ReviewState` **only**
if `role ∈ {solo, practice, null-legacy}`. For `intro`/`guided` it just records
that the item was answered (reusing the existing "answeredIds" mechanism that
already filters new items), without scheduling a review.

## 3. Session engine: `GetTodaySessionUseCase`

Today: `due + new.take(N)` where `new` = unanswered exercises of the current
unit, in no guaranteed order.

New: `due (Phase B) + pathNext (Phase A)` where `pathNext` respects the ladder:

1. **Phase B (review):** `getDueExercises(today)` — unchanged. These already
   exclude `intro`/`guided` because they never had a `ReviewState`.
2. **Phase A (Path):** from the current unit, take the **unanswered**
   exercises, order them by `(conceptId in the order of the concepts list, then
   orderInConcept)`, and take the first **K** (cap on new material per day,
   `newExercisesLimit`, already an existing parameter).
   - A **composition** concept (`dependsOn` non-empty) is **skipped** in Phase A
     until **all** its `dependsOn` are "born" (have a `ReviewState`). This way
     the composed problem appears only once you master the pieces.

`SubmitAnswer` and the SRS underneath are unchanged except for the §2 rule.

## 4. New exercise type: `worked_example` (Step 1 "intro")

- **Content:** reuses `prompt` (framing), `code` (the full worked example,
  using the existing `CodeBlock`), and `explanation` (why). `answer` holds the
  trivial confirmation (e.g. an "obvious" option out of 2) so there is *one*
  impossible-to-fail interaction that fixes the pattern.
- **UI:** a new branch in the session screen that shows code + text + a single
  confirmation tap → always "correct" → next. No penalty, no SRS.
- Reuses existing components (`CodeBlock`); it is not a separate new screen,
  it's one more exercise type within the session.

## 5. Migration / grandfathering of Luis's live progress

Luis already has real SM-2 progress on `streams-collectors` exercises. When
re-authoring that unit as ladders, he must **not** be sent back through the
`intro`/`guided` steps of concepts he already practiced.

**Grandfather rule:** a concept is considered **"already born"** for the player
if a `ReviewState` exists for any `solo`/`practice` exercise of that concept (or
for a legacy exercise now mapped to that concept). For an already-born concept,
Phase A **skips** its `intro`/`guided` steps.

- The existing `streams-collectors` exercises are **tagged** with their
  `conceptId`/`role` (the existing ones become the `solo`/`practice` of their
  concept), keeping their `id` → keeping their `ReviewState`. They are not
  duplicated and progress is not reset.
- Clean install (no `ReviewState`): no concept is born → full ladders from
  scratch. This is the path that validates "learning from scratch."

No Room migration: all of this is domain logic over data that already exists
(`ReviewState` per exercise + `conceptId` in payload).

## 6. Pilot content (`streams-collectors`)

Re-author **only this unit** with at least:
- 2 base concepts with a full ladder (`intro`→`guided`→`solo`):
  `collectors-groupingby` and `collectors-partitioningby`.
- 1 **composition** concept `collectors-partition-then-group` with
  `dependsOn: ["collectors-groupingby", "collectors-partitioningby"]`, which
  materializes the interview case (split by salary + group by department). Short
  ladder or straight to `solo`, but **only** available once its two
  dependencies are born.
- Tag the unit's existing exercises with `conceptId`/`role`.

## Scope

**Included:**
- `ConceptPack` + `conceptId`/`role`/`orderInConcept` fields on
  `ExerciseContent` (all optional, no Room migration).
- `worked_example` exercise type (content + session UI branch).
- `GetTodaySessionUseCase` rework (ladder order + cap + composition gating by
  `dependsOn`).
- Graduation rule in `SubmitAnswerUseCase` (do not schedule `intro`/`guided`).
- Grandfather rule for existing progress.
- Re-author **only** `streams-collectors` as the pilot (incl. 1 composition).

**Explicitly out of scope:**
- Re-authoring the other 18 units (slice 2+, section by section).
- Room migration / new columns (deliberately avoided).
- Redesign of `summary`/Tips (kept as-is, dual role).
- Long ladders (>3-4 steps) or level-adaptive scaffolding (model B from the
  brainstorm, rejected in favor of C).
- Path progress analytics, a new visual "tree" on the Path screen.

## Testing (pure Kotlin, Level 1 of the testing ADR)

- **`GetTodaySessionUseCase`** (new behavior, with `FakeExerciseRepository`):
  - Phase A orders by concept→`orderInConcept` (not random).
  - Respects `newExercisesLimit` (new-material cap).
  - A composition concept is **skipped** while a dependency is missing, and
    **appears** once all are born.
  - Consumed `intro`/`guided` do not reappear; `solo` items remain for Phase B.
- **`SubmitAnswerUseCase`**: `intro`/`guided` → no `ReviewState`;
  `solo`/`practice`/legacy → `ReviewState` (normal SM-2). Graduation on
  first answer to the `solo`.
- **Grandfather**: with a pre-existing `ReviewState` for a concept, Phase A
  skips its `intro`/`guided`; without it, includes them.
- **Content parsing** (`ContentPackParsingTest`): a pack with `concepts` +
  exercises carrying `conceptId`/`role`/`orderInConcept` parses; a legacy pack
  without those fields still parses (backward compat).
- **Content**: pilot JSON validated (`python3 -m json.tool`) + full suite as
  regression.
- **UI**: no Compose test (Level 2 not started). Manual QA on device: the
  `groupingBy` ladder shows (worked intro → guided → solo); the composition
  problem does **not** appear until its two pieces are practiced and **does**
  appear after; Luis's existing SM-2 progress is not reset (grandfather).

## Recorded decisions

| Decision | Chosen | Date |
|---|---|---|
| Teaching depth | B: self-sufficient lesson per unit | 2026-07-30 |
| Pedagogical model | Learn by doing (Duolingo), not read→practice | 2026-07-30 |
| Scaffolding | C: short authored ladder on 1st exposure, then SRS | 2026-07-30 |
| SRS vs Path | Two phases: Learn (ordered) + Review (SM-2) | 2026-07-30 |
| Role of Tips/`summary` | Dual role (intro step + reference on Path screen) | 2026-07-30 |
| Graduation into SRS | On *first answer* to the `solo` (pass or fail) | 2026-07-30 |
| Persistence of ladder fields | In JSON payload, no Room migration | 2026-07-30 |
| Composition | First-class via `dependsOn`, gated in Phase A | 2026-07-30 |
| Pilot unit | `streams-collectors` (the interview case) | 2026-07-30 |
| Retrofit of remaining 18 units | Out of scope (slice 2+) | 2026-07-30 |