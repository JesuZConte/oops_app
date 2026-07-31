# Oops! — Project Constitution

> The name plays on two meanings: **OOP** (Object-Oriented Programming, Java's
> core paradigm) and **oops** (the moment you fail an exercise and the system
> reschedules it for tomorrow — the SRS mechanic itself).

This document is the project's **durable source of truth**: the *why*, the
philosophy, and the constraints that rarely change. The technical detail that
*does* evolve (data model, SRS engine, content format, build plans) lives —
always current — in the ADRs, specs, roadmap, and the code itself; see
**Pointers** at the end. If anything here contradicts a newer ADR, the ADR
wins and this document gets corrected.

---

## Purpose and goal

Oops! is a **self-sufficient, Duolingo-style Path for learning Java**: a player
with no external material can learn every topic from scratch inside the app and
arrive ready to sit the **Oracle Certified Professional: Java SE 21 Developer
certification (exam 1Z0-830)**.

The guiding analogy is Duolingo → IELTS: following the full Path takes you to a
real level without studying anywhere else; the exam is the final validation.
In the future the same engine scales to other syllabi (Algorithms, Spring Boot,
Design Patterns, other languages).

The exam is taken with no IDE, no compiler, and no documentation, so training
**active recall** and **composition** of the Java API is exactly the skill the
app builds.

## Product philosophy

- **Self-sufficient, not a book companion.** The app teaches from scratch; it
  does not assume you have a reference book at hand. (This is a deliberate
  reversal of the original vision — see
  `docs/adrs/2026-07-30-self-teaching-path-vision.md`.)
- **Learn by doing, not read-then-practice.** A new concept is introduced
  *inside* the exercise, with scaffolding that starts nearly solved and is
  gradually removed (first-exposure ladders). Theory lives in short supporting
  pills, not book chapters.
- **Composition is the real skill.** Success is not recalling a method name but
  combining pieces to solve a new problem (the interview case: split by salary
  + group by department). The model treats composition as a first-class
  citizen.
- **Learning and reviewing are two phases.** Advancing the Path (authored
  order) is separate from reviewing what you've already seen (unordered
  SRS/SM-2, which trains recall and composition under pressure).
- **Start simple, add over time.** The goal is not an app that "knows
  everything" from day one.

## Product direction: bilingual (EN + ES)

The app targets an **English + Spanish** audience to reach more learners. This
is a stated direction, not yet built: today exercise content is Spanish-only
(no accents, for consistency with existing content). Full EN+ES content and UI
i18n is a future effort. **Project documentation is written in English.**

## Hard constraints

- **Platform:** Android only. Kotlin. Jetpack Compose + Navigation.
- **Local-first:** Room (SQLite, local), no network. Everything works offline.
- **DI:** Hilt. **Serialization:** kotlinx.serialization for JSON content
  packs.
- **Pure, testable domain:** the SRS engine and use cases are pure Kotlin,
  **importing no `android.*`**, so they run 100% without an emulator (JUnit
  tests).
- **Content as JSON packs** in `assets/content/*.json`: adding a domain means
  adding a file, not touching the app.
- **`ExerciseEntity.payload` is an opaque JSON blob:** the different exercise
  types (and new fields like ladders/summaries) coexist **without migrating**
  the Room schema.
- **One ViewModel per screen;** UI state as an immutable `StateFlow`.
- **Base package:** `com.zconte.oopsapp`. Identifiers in English; exercise
  content currently in Spanish (see bilingual direction above).

## Non-goals (deliberately deferred)

- Real code execution / embedded compiler (sandbox).
- Backend and cross-device sync.
- AI exercise generation.
- iOS — out of scope, Android only.

## Learning structure

**Section → Unit → Checkpoint** hierarchy (Duolingo-style). Each unit maps to a
real exam objective (`certObjective`); progress is grouped by those objectives
to show *readiness*. Checkpoints are cumulative, unaided assessments that gate
progression.

Reference domains (1Z0-830, Java SE 21): Language fundamentals & OOP · Generics
& collections · Streams & lambdas · Exception handling · Concurrency (virtual
threads) · Modules · JDBC & NIO.2 · Java 21 features (records, sealed classes,
pattern matching, text blocks).

Graduation feature (future): an adaptive, Duolingo-Test-style mock that
estimates real readiness to sit the certification.

## Pointers (where the living detail lives)

- **Teaching vision and ladder model:**
  `docs/adrs/2026-07-30-self-teaching-path-vision.md` + slice-1 spec
  `docs/superpowers/specs/2026-07-30-learn-by-doing-ladders-slice1-design.md`.
- **Section/Unit/Checkpoint structure:**
  `docs/adrs/2026-07-20-content-structure-sections-checkpoints.md`.
- **Content and phase roadmap:**
  `docs/specs/2026-07-20-fase2-content-roadmap.md`.
- **Data model, SM-2 engine, pack format:** the code is the source
  (`domain/srs/`, `data/local/`, `data/content/`) plus the specs in
  `docs/superpowers/specs/`.
- **Decision history and work rounds:** `docs/CHANGELOG.md`, the ADRs in
  `docs/adrs/`, and the git history.
- **Testing strategy:**
  `docs/adrs/2026-07-24-viewmodel-and-smoke-testing-strategy.md`.