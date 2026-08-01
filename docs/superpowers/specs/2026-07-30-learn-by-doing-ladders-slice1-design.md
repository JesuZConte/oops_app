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

## 1. Data model: ladder fields on the exercise (payload-only)

> **Design refinement (2026-07-31).** The original draft put concept metadata
> in a unit-level `ConceptPack` list on `UnitPack`. Dropped in favor of
> **payload-only**: all ladder data rides on `ExerciseContent` (and therefore
> the Room `payload` blob). Two reasons: (1) the only concept-level datum slice
> 1 needs is `dependsOn`, which can ride the exercises that declare it — no
> concept *names/descriptions* are shown anywhere in slice 1; (2) it keeps
> `GetTodaySessionUseCase` **single-source on `ExerciseRepository`** and avoids
> adding a `ContentRepository` method that would break every fake's
> compilation. If a future Path UI needs concept names, revisit then.

All ladder data is inside the content JSON — **no Room changes, no migration**.
`UnitPack` is **unchanged** (still just `summary` from the Tips feature).

### `ExerciseContent` (the payload; no new Room column)

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
    // NEW (all optional/defaulted → legacy content still parses):
    val conceptId: String? = null,          // which concept it belongs to
    val role: String? = null,               // "intro" | "guided" | "solo" | "practice"
    val pathOrder: Int? = null,             // global order within the unit's Path
    val dependsOn: List<String> = emptyList() // conceptIds this concept requires (COMPOSITION)
)
```

**Why `pathOrder` (unit-global) and not a per-concept index:** `ExerciseDao`'s
`getByUnit` is `SELECT * FROM exercises WHERE unitId = :unitId` with **no
`ORDER BY`** — DB row order is not a contract. A single unit-global `pathOrder`
gives Phase A one deterministic sort key; authored so that within a concept
`intro < guided < solo`, and concepts are contiguous.

**A concept's ladder** = the unit's exercises sharing the same `conceptId`,
in `pathOrder` order: `intro` → `guided`(one or more) → `solo`.
`practice` = extra exercises for the concept, no different from `solo` for
scheduling purposes. `dependsOn` is authored on the composition concept's
exercises (they share the same value).

**Legacy:** an exercise with `conceptId == null` (all current content except
the pilot) is treated as a **standalone, already-born** item — it never goes
through a ladder, it enters the SRS pool directly as today.

## 2. Graduation rule and entry into SRS

> **Design refinement (2026-07-31).** The original draft made both `intro` and
> `guided` "consumed but never scheduled." Verified this is unrepresentable
> without a Room migration: `getAnsweredExerciseIds` is backed by
> `reviewStateDao.getExistingIds` — i.e. "answered" **is** "has a
> `ReviewState`", there is no separate answer log. Resolution (matching Luis's
> own mental model of the loop): only the `intro` is special; `guided`/`solo`
> are ordinary SRS exercises. No migration.

- The **`intro`** (`worked_example`) is a **non-tracked didactic card**: it is
  never answered for score, never creates a `ReviewState`, is never "mastered".
  It is shown in Phase A as the lead-in for an **unborn** concept and simply
  disappears once the concept is born (see below). If the player quits before
  answering any real exercise of the concept, the card shows again next
  time — cheap and pedagogically harmless.
- The **`guided`, `solo`, and `practice`** steps are **ordinary SRS
  exercises**: answering creates/updates a `ReviewState` with normal SM-2. They
  follow the usual loop — wrong reschedules for tomorrow, ~3 corrects parks the
  item ("mastered"). A `guided` step may reappear in review; because it is
  easy, it is mastered quickly and self-retires. This is acceptable and avoids
  the migration.
- **A concept is "born"** for the player the first time **any** of its real
  (`guided`/`solo`/`practice`) exercises gets a `ReviewState`. Same condition
  the grandfathering in §5 uses.

Implementation: `SubmitAnswerUseCase` is unchanged for `guided`/`solo`/
`practice`/legacy (normal SM-2). The `intro` card never routes through
`SubmitAnswerUseCase` at all — the session screen advances past it without
submitting (see §4). So there is **no special scheduling branch** in the use
case; the only new logic lives in the session engine's Phase-A selection.

## 3. Session engine: `GetTodaySessionUseCase`

Today: `due + new.take(N)` where `new` = unanswered exercises of the current
unit, in no guaranteed order.

New: `due (Phase B) + pathNext (Phase A)` where `pathNext` respects the ladder:

1. **Phase B (review):** `getDueExercises(today)` — unchanged.
2. **Phase A (Path):** group the current unit's exercises by `conceptId`; a
   concept is **born** when any of its real (non-`intro`) exercises has a
   `ReviewState` (via `getAnsweredExerciseIds`). Drop **all** exercises of born
   concepts — the `intro` card included (this is why the filter must be
   *concept-born*, not *exercise-answered*: the `intro` is never tracked, so an
   answered-based filter would keep it forever). From the remaining unborn
   concepts, sort by `pathOrder` and take the first **K** (`newExercisesLimit`,
   the existing per-session cap — see the note below on the per-day cap).
   - A **composition** concept (its exercises carry a non-empty `dependsOn`) is
     **skipped** until **all** its `dependsOn` conceptIds are born. This way the
     composed problem appears only once you master the pieces.

> **Scope note — per-day cap deferred to slice 1b.** `newExercisesLimit` caps
> new material **per session**, not per day. Opening "Estudiar Hoy" multiple
> times a day would keep serving the next concepts with no daily ceiling,
> breaking spaced pacing. Designing a per-day cap needs a concept-birth-date
> mechanism that does not exist today (`ReviewState` stores `lastReviewedAt`/
> `dueDate`, not a birth date). Deferred to **slice 1b** (its own design);
> slice 1 keeps the existing per-session cap. Reviews stay available any number
> of times per day.

`SubmitAnswer` and the SRS underneath are unchanged.

## 4. New exercise type: `worked_example` (Step 1 "intro")

- **Content:** reuses `prompt` (framing), `code` (the full worked example,
  using the existing `CodeBlock`), and `explanation` (why). `answer` holds the
  trivial confirmation (e.g. an "obvious" option out of 2) so there is *one*
  impossible-to-fail interaction that fixes the pattern.
- **UI:** a new branch in the session screen that shows code + text + a single
  confirmation tap → advances to the next item. **It does not call
  `SubmitAnswerUseCase`** (no score, no penalty, no `ReviewState`, no SRS).
- Reuses existing components (`CodeBlock`); it is not a separate new screen,
  it's one more exercise type within the session.

## 5. Migration / grandfathering of Luis's live progress

Luis already has real SM-2 progress on `streams-collectors` exercises. When
re-authoring that unit as ladders, he must **not** be sent back through the
`intro` card (or the early `guided` steps) of concepts he already practiced.

**Grandfather rule:** a concept is considered **"already born"** for the player
if a `ReviewState` exists for any of its real (`guided`/`solo`/`practice`)
exercises (or for a legacy exercise now mapped to that concept). For an
already-born concept, Phase A skips it entirely — including its `intro` card.

- The existing `streams-collectors` exercises are **tagged** with their
  `conceptId`/`role` (the existing ones become the `solo`/`practice` of their
  concept), keeping their `id` → keeping their `ReviewState`. They are not
  duplicated and progress is not reset.
- Clean install (no `ReviewState`): no concept is born → full ladders from
  scratch. This is the path that validates "learning from scratch."

No Room migration: all of this is domain logic over data that already exists
(`ReviewState` per exercise + `conceptId` in payload).

### 5b. `worked_example` cards must be excluded from answerable-question consumers

The `intro` cards are seeded into the `exercises` table like any other row, so
every consumer that iterates "all exercises in a unit/section" **as answerable
questions** must exclude them (`type == "worked_example"`), or they regress:

- **`MarkUnitProgressUseCase`** (highest risk): marks a unit complete only when
  *every* exercise is answered. Intro cards are never answered → the pilot unit
  could never complete → its checkpoint never unlocks → the Path stalls.
- **`GetCheckpointSessionUseCase`** / **`GetPlacementCheckpointSessionUseCase`**:
  would sample an intro card (with a dummy `answer`) as an assessment question.
- **`CompleteCheckpointUseCase`** (placement unlock): would seed a `ReviewState`
  onto an intro card, pushing it into the due pool forever.
- **`GetUnitSessionUseCase`** (unit replay): a replay is answerable questions.

The Phase-B due path is already safe — `getDue` inner-joins `review_state`,
which intro cards never have. A single `List<Exercise>.answerableOnly()` helper
(filters `worked_example`) is applied at those consumers; `GetTodaySessionUseCase`
deliberately does **not** use it (Phase A must surface the intro cards).

## 6. Pilot content (`streams-collectors`)

Re-author **only this unit** with at least:
- 2 base concepts with a full ladder (`intro`→`guided`→`solo`):
  `collectors-groupingby` and `collectors-partitioningby`.
- 1 **composition** concept `collectors-partition-then-group` whose exercises
  carry `dependsOn: ["collectors-groupingby", "collectors-partitioningby"]`,
  which materializes the interview case (split by salary + group by
  department). Short ladder or straight to `solo`, but **only** available once
  its two dependencies are born.
- Tag the unit's existing exercises with `conceptId`/`role`/`pathOrder`
  (existing ids preserved → their `ReviewState` preserved).

## Scope

**Included:**
- `conceptId`/`role`/`pathOrder`/`dependsOn` fields on `ExerciseContent`
  (all optional/defaulted, no Room migration; payload-only, no `ConceptPack`).
- `worked_example` exercise type (content + session UI branch; non-tracked
  didactic card).
- `GetTodaySessionUseCase` rework (Phase-A concept-born check + `pathOrder`
  ladder order + composition gating by `dependsOn`).
- `answerableOnly()` filter (excludes `worked_example`) applied to the
  answerable-question consumers (§5b): unit-completion + checkpoint sampling.
- Decode ladder metadata (`conceptId`/`role`/`pathOrder`/`dependsOn`) once in
  the `ExerciseEntity.toDomain()` mapping; enrich the domain `Exercise` model
  with those nullable/defaulted fields (keeps use cases JSON-free, no Room
  migration).
- Re-author **only** `streams-collectors` as the pilot (incl. 1 composition).

**Explicitly out of scope:**
- Per-**day** new-material cap + "nothing new today" state → **slice 1b** (see
  the scope note in §3). Slice 1 keeps the per-session cap.
- Re-authoring the other 18 units (slice 2+, section by section).
- Room migration / new columns (deliberately avoided).
- Redesign of `summary`/Tips (already shipped 2026-07-31; kept as-is, dual
  role).
- Long ladders (>3-4 steps) or level-adaptive scaffolding (model B from the
  brainstorm, rejected in favor of C).
- The "mastery check at 3 corrects" visual (a separable progress-display idea).
- Path progress analytics, a new visual "tree" on the Path screen.

## Testing (pure Kotlin, Level 1 of the testing ADR)

- **`GetTodaySessionUseCase`** (new behavior, with `FakeExerciseRepository`):
  - Phase A orders unborn-concept exercises by `pathOrder` (not random), with
    each concept's `intro` card at the front of its run.
  - Respects `newExercisesLimit` (per-session cap).
  - A composition concept is **skipped** while a dependency is missing, and
    **appears** once all are born.
  - A born concept is dropped from Phase A entirely (including its `intro`);
    its real exercises are surfaced only via Phase B when due.
- **Enriched `Exercise` mapping**: an `ExerciseEntity` whose payload carries
  `conceptId`/`role`/`pathOrder`/`dependsOn` maps to a domain `Exercise` with
  those fields populated; a legacy payload without them maps to
  `null`/`emptyList`.
- **Content parsing** (`ContentPackParsingTest`): an exercise carrying
  `conceptId`/`role`/`pathOrder`/`dependsOn` parses; a legacy exercise without
  those fields still parses (backward compat).
- **Content**: pilot JSON validated (`python3 -m json.tool`) + full suite as
  regression.
- **UI**: no Compose test (Level 2 not started). Manual QA on device: the
  `groupingBy` ladder shows (worked intro → guided → solo); the `intro` card
  advances without scoring; the composition problem does **not** appear until
  its two pieces are practiced and **does** appear after; Luis's existing SM-2
  progress is not reset (grandfather).

Note: `SubmitAnswerUseCase` needs **no** new test — it is unchanged (the
`intro` card never routes through it; `guided`/`solo`/`practice` are ordinary
SM-2 submissions already covered).

## Recorded decisions

| Decision | Chosen | Date |
|---|---|---|
| Teaching depth | B: self-sufficient lesson per unit | 2026-07-30 |
| Pedagogical model | Learn by doing (Duolingo), not read→practice | 2026-07-30 |
| Scaffolding | C: short authored ladder on 1st exposure, then SRS | 2026-07-30 |
| SRS vs Path | Two phases: Learn (ordered) + Review (SM-2) | 2026-07-30 |
| Role of Tips/`summary` | Dual role (intro step + reference on Path screen) | 2026-07-30 |
| Scaffolding tracking | `intro` = non-tracked card; `guided`/`solo` = normal SRS (no migration) | 2026-07-31 |
| "Born" definition | A real (`guided`/`solo`/`practice`) exercise has a `ReviewState` | 2026-07-31 |
| Reading ladder metadata | Decode payload once in `toDomain()`, enrich `Exercise` | 2026-07-31 |
| Per-day new cap | Deferred to slice 1b (needs birth-date mechanism) | 2026-07-31 |
| Persistence of ladder fields | In JSON payload, no Room migration | 2026-07-30 |
| Composition | First-class via `dependsOn`, gated in Phase A | 2026-07-30 |
| Pilot unit | `streams-collectors` (the interview case) | 2026-07-30 |
| Retrofit of remaining 18 units | Out of scope (slice 2+) | 2026-07-30 |