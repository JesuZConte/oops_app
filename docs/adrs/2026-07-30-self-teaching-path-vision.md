# ADR — Oops! as a self-sufficient Path (not a "book companion")

**Date:** 2026-07-30
**Status:** Accepted
**Supersedes:** the "book companion" premise of the original vision. As a
result of this ADR, `docs/specs/PROJECT-OOPS.md` was **rewritten as a
constitution** (2026-07-31) to reflect the self-sufficient Path vision — it no
longer contradicts it. This also reverses the product note in the Tips spec
(`docs/superpowers/specs/2026-07-29-unit-summary-tips-design.md`, lines 22-24)
that said *"assumes the player has the book at hand… Oops! is a practice
companion, not a replacement for the book."*

---

## Context

The original vision framed Oops! as a **practice companion** that assumes the
player studies the theory elsewhere (a reference book) and uses the app to
train *recall* of the API. The Tips feature added a "minimal safety net" but
explicitly kept that premise.

Luis (product owner and target user) rejects that premise. The problem that
started the app is personal and concrete: being able to prepare for interviews
**without depending on a book that not everyone has**. The guiding case is a
real interview question he couldn't solve:

> "From a list of users, split those earning > X from those earning ≤ X, and
> also group them by department."

Solving that is not *recalling a method name*: it's **composing** knowledge. An
app that only tests what you already know trains neither that skill nor
learning from scratch.

The product reference is **Duolingo**: following its full Path gets you to a
real level (~B2) without studying elsewhere. Oops! wants to be that for Java,
with the **OCP Java SE 21 exam (1Z0-830)** as the final goal (the equivalent of
sitting the IELTS). In the future, scale the same engine to Algorithms, Spring
Boot, Design Patterns, and other languages.

## Decision

Oops! stops being a "book companion" and becomes a **self-sufficient Path**: a
player with no external material can learn every topic from scratch inside the
app and arrive ready for the exam.

### 1. Pedagogy: learn by doing (Duolingo model), not read-then-practice

Learning happens **by doing**, not by reading a long lesson. A new concept is
introduced *inside* the exercise, with scaffolding that starts nearly solved
and is gradually removed. Theory lives in short supporting pills (the
"Tips"/summaries), not in a book chapter.

### 2. First-exposure ladders (hybrid model "C")

The first time a new concept appears, it is not asked cold: it enters through a
short **authored ladder** (2-3 steps) of decreasing scaffolding:

1. **Worked example** — you only read the pattern + a trivial interaction
   (impossible to fail); it fixes the pattern.
2. **Guided** — you fill the key blank with a hint and few options.
3. **Solo** — full problem, no hint.

From then on the concept enters the normal SRS pool **without scaffolding**.
The ladder is only the *birth* of the concept, once per concept. We do not
re-author all content ×5: only the first exposure.

### 3. Two phases in the daily session (separate learning from reviewing)

Today the session mixes "learning" and "reviewing" in one bag. They are
separated, just as Duolingo separates advancing the Path from strengthening
what you've seen:

- **Phase A — Learn (the Path):** you advance the current unit in **strict
  authored order** (the ladders). Driven by your *position in the Path*, not by
  SRS. With a **cap on new material per day**.
- **Phase B — Review (the usual SRS):** a concept enters the SM-2 pool **when
  it graduates from its ladder**; it reappears by `dueDate`, without
  scaffolding, freely mixed with concepts from any unit. That unordered mix is
  *desirable* here: it trains recall and **composition** under pressure —
  exactly the interview case.

The Section → Unit → Checkpoint structure (Fase 2.1) **is already a Path**; the
checkpoints are untouched (they remain cumulative, unaided assessments).

### 4. Composition is a first-class citizen

The interview case is the success criterion. The data model supports, from day
one, "a concept depends on X and Y", so composition units (combining
`groupingBy` + `partitioningBy`, etc.) build on pieces already born separately —
even if composition content lands in a later slice.

### 5. "Tips"/summaries serve a dual role

The `summary` field (text + code) already being implemented is kept and serves
twice: as first-exposure Path material and as **optional reference** accessible
from the Path screen to review the theory. The in-progress work is not
discarded; it is generalized.

## Consequences

- **`GetTodaySessionUseCase` changes** from `due + N random-new` to
  `due (SRS) + next-Path-steps-in-order` with a daily cap. The change is
  bounded: the use case **already** pulls new items from the current unit.
- **New exercise type** "worked example" (Step 1 of the ladder).
- **Mandatory migration:** existing content carries Luis's live SM-2 progress.
  Already-answered exercises are **grandfathered** as *already born* — they are
  not sent back through a ladder. This is decided and written explicitly, it
  does not emerge on its own.
- **Large, deferred content cost:** re-authoring the 19 existing units as
  ladders is the biggest cost; it goes **section by section** after the engine
  is proven, not in the first slice.

## Decomposition

This vision is too large for a single spec. It executes like Fase 2.1:

- **This ADR** — the vision shift + the model (two phases + ladders).
- **Slice 1 (separate spec)** — ladder data model + "worked example" type +
  `GetTodaySession` rework + migration + composition hook, proven end-to-end on
  **one pilot unit of Streams/Collectors** (where the interview case lives) and
  QA'd on device. Spec:
  `docs/superpowers/specs/2026-07-30-learn-by-doing-ladders-slice1-design.md`.
- **Slices 2+** — re-author the 19 existing units as ladders, section by
  section, + the composition units.