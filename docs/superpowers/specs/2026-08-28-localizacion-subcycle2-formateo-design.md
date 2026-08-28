# Localizacion sub-cycle 2 (Formateo): Design

**Status:** approved by Luis, pending spec self-review and commit.

## Motivation

Sub-cycle 1 shipped `Locale` + `ResourceBundle` (2 units, 6 concepts,
merged `8dea681`, pushed). The section's third and last ADR cluster,
`NumberFormat`/`DateTimeFormatter` ("Formateo"), was explicitly deferred
to this sub-cycle. Shipping it closes the "Localizacion" section
(2/2 sub-cycles).

## Scope

**In scope:**
- `NumberFormat.getInstance()` / `getPercentInstance()` / `getCurrencyInstance()`
- `DecimalFormat` with custom pattern strings + explicit `DecimalFormatSymbols`
- `DateTimeFormatter.ofLocalizedDate()` / `ofLocalizedTime()` with `FormatStyle`
- `DateTimeFormatter.ofPattern(pattern, locale)`
- Format/parse round-trip symmetry with `DateTimeFormatter`

**Out of scope** (unchanged from sub-cycle 1, plus one addition):
`Collator`, `Charset`, `Locale` Unicode extensions (`-u-`),
`ResourceBundle.Control`/`ResourceBundleProvider` SPI, `Locale.Category`,
and (new to this sub-cycle) legacy `Date`/`SimpleDateFormat` — `java.time`
only.

## Structure

2 units, 6 concepts, ~18 exercises — same size as every prior sub-cycle-1.

- **Unit 3 `formateo-numeros`** (orderIndex 3): `numberformat-locale-dependiente`,
  `numberformat-moneda`, `decimalformat-patron-custom`.
- **Unit 4 `formateo-fechas-horas`** (orderIndex 4): `datetimeformatter-estilos-predefinidos`,
  `datetimeformatter-patron-personalizado`, `datetimeformatter-parseo-simetrico`.

Both units `dependsOn` the previous unit in the section, same-unit-only
dependency chains within, sequential `pathOrder` 0..n-1, one terminal role
(`solo` or `practice`) per concept — all per this project's standing
ladder rules.

## Pedagogical pattern: two-locale contrast + third-locale transfer test

Every locale-comparison concept in this sub-cycle uses **two locales in
the `worked_example`/`guided` steps, and holds a third locale back for the
`solo` step as a transfer test** — the learner must predict the third
locale's output from the pattern established by the first two, rather
than being shown all three up front. This was an explicit design decision
(reviewed against a "three locales in the intro" alternative) on the
reasoning that prediction is a stronger test of genuine generalization
than recognition of a memorized triple, and Luis's own gameplay experience
with sub-cycle 1 supports starting narrower and testing transfer.

The two intro locales are `en_US` and `de_DE` throughout (maximally
different: comma/period swap on numbers, full field-order and
month-name differences on dates). `es_CL` is the held-back third locale
in every concept's `solo` step, which doubles as reinforcing "don't
assume Spanish locales are internally consistent" — a fact this project
already learned the hard way in sub-cycle 1 (`es_419` vs `es_CL`
divergence).

This pattern does not apply to `datetimeformatter-parseo-simetrico`,
which is not a locale-comparison concept — it teaches a single-locale
round-trip principle (see below).

## Content-accuracy decisions (from UX-lens review)

Two decisions were made specifically to avoid errors that would only
surface once real users hit the exercise:

1. **Name currencies explicitly, never rely on the symbol alone.**
   `getCurrencyInstance(Locale.US)` and `getCurrencyInstance(es_CL)` both
   render with a bare `$` — showing `$1,234.50` next to `$1.234` without
   naming the currency reads as "same currency, inconsistent formatting"
   rather than "these are two different currencies that happen to share a
   symbol." Every currency exercise names the currency explicitly in its
   text (USD, EUR, CLP), never leaving the reader to infer it from the
   symbol. This is also why `numberformat-moneda`'s intro pair is
   `en_US`/USD vs `de_DE`/EUR (`€` is unambiguous) and `es_CL`/CLP is
   reserved for the transfer test, where the exercise text calls out
   the currency by name.

2. **Describe locale-sensitive output in words, not exact fragile
   strings.** Several real JDK outputs contain invisible formatting
   characters — a narrow no-break space (`U+202F`) before "PM"-equivalent
   markers in some locales, a no-break space (`U+00A0`) before `%` in
   `de_DE`. These are indistinguishable from a normal space when read or
   hand-typed into JSON, so any exercise whose correct answer is a
   fill-in-the-blank exact string built on one of these outputs is a
   silent authoring trap: a normal space substituted in by hand looks
   identical but fails a byte-exact check. To sidestep this entirely,
   exercises describe the shape of locale-sensitive output in words
   ("sin separador de miles", "sin decimales", "orden dia-mes-anio", "24
   horas sin marcador AM/PM") rather than asking the learner to reproduce
   an exact punctuation-sensitive string. Where an exact string is shown
   (e.g. as the worked example's demonstrated output), it is presented
   as read-only reference text, never as the literal expected answer of
   an exercise the learner types into.

## Verified facts driving the content (JDK 20, `/usr/bin/java`, OpenJDK 20+36-2344)

All facts below were independently compiled and run (`Verify.java`,
`VerifyDefault.java`), not assumed from general knowledge — continuing
the practice sub-cycle 1's final review established as mandatory after
finding a real bug that general knowledge alone would have missed.

- **Grouping/decimal separators swap between `en_US` and `de_DE`**:
  `NumberFormat.getInstance()` on `1234567.891` gives `1,234,567.891` in
  `en_US` and `1.234.567,891` in `de_DE` — period and comma are fully
  swapped, not just reformatted.
- **`es_CL` matches `de_DE`'s separator convention** (period for
  grouping, comma for decimal) — this is the transfer-test answer for
  `numberformat-locale-dependiente`.
- **Percent formatting**: `de_DE`'s `getPercentInstance()` inserts a
  space-like character before `%` that `en_US`/`es_CL` do not — described
  in words ("con un espacio antes del simbolo de porcentaje"), never
  quoted as an exact string, per the decision above.
- **Currency formatting ties symbol AND fraction-digit count to the
  Locale**: `getCurrencyInstance(Locale.US)` on `1234.5` gives
  `$1,234.50` (USD, 2 decimals); `getCurrencyInstance(Locale.GERMANY)`
  gives `1.234,50 €` (EUR, 2 decimals, symbol after the number);
  `getCurrencyInstance(es_CL)` gives `$1.234` (CLP, **zero decimals** —
  CLP has no minor currency unit). The zero-decimals fact is the
  transfer-test's core insight and is called out by name ("CLP no tiene
  centavos") rather than left to be inferred from the symbol.
- **`DecimalFormat` needs explicit, locale-aware
  `DecimalFormatSymbols`** — a pattern string like `"#,##0.00"` only
  defines digit grouping *shape*, not which characters mean "grouping"
  vs "decimal"; those come from whatever `DecimalFormatSymbols` instance
  is passed (or the JVM default if none is passed). Verified: the same
  pattern with `DecimalFormatSymbols.getInstance(Locale.US)` vs
  `.getInstance(Locale.GERMANY)` produces the same US/DE separator swap
  as `NumberFormat.getInstance()` above.
- **Landmine reconfirmed from design-phase verification**: constructing
  `new DecimalFormat("#,##0.00")` with no explicit symbols does **not**
  use whatever locale you might expect from context — it silently uses
  `DecimalFormatSymbols.getInstance()` with the JVM's own default locale,
  which in this dev environment is `es_419` and (surprisingly, verified
  independently) formats in **`en_US` style** despite being a Spanish
  locale code. This exact landmine is content for
  `decimalformat-patron-custom`'s `guided`/`solo` steps: the lesson is
  "always pass `DecimalFormatSymbols` explicitly; never rely on the
  ambient default," directly reusing the standing project rule from
  sub-cycle 1 about never leaving a default-locale dependency implicit.
- **`ofLocalizedDate(FormatStyle.LONG)`** on 2026-03-05: `en_US` →
  "March 5, 2026"; `de_DE` → "5. März 2026" (day-first, period after
  day, German month name); `es_CL` → "5 de marzo de 2026" (day-first,
  "de" connectors, Spanish month name) — the transfer test is predicting
  the day-first order and Spanish month name, not memorizing an exact
  string.
- **`ofLocalizedDate(FormatStyle.SHORT)`** on the same date: `en_US` →
  month/day/2-digit-year with `/`; `de_DE` → day.month.2-digit-year with
  `.`; `es_CL` → day-month-2-digit-year with `-` — field order and
  separator both vary; described in words in the exercise, not quoted
  byte-exact.
- **`ofLocalizedTime(FormatStyle.SHORT)`** on 14:30: `en_US` → 12-hour
  with AM/PM marker; `de_DE` → 24-hour, no AM/PM marker at all — this
  contrast (12h-with-marker vs 24h-no-marker) is the core teaching point
  and is described in words for exactly this reason (the `es_CL` and
  `de_DE` AM/PM-adjacent renderings both involve the invisible-space
  landmine noted above).
- **`ofPattern(pattern, locale)`**: the pattern string controls *layout*
  (which fields, in what order, with what literal punctuation) but the
  `Locale` argument controls *vocabulary* (month names, day names) —
  verified with `"MMMM d, yyyy"` across `en_US`/`de_DE`/`es_CL`: the
  field order stays exactly as the pattern dictates (month name first)
  in all three, but the month name itself changes language. This
  isolates the pattern-vs-locale distinction cleanly since the pattern
  is held constant and only the locale varies.
- **Format/parse round-trip symmetry**: formatting a `LocalDate` with a
  given `DateTimeFormatter` (locale included) and then parsing that same
  string back with the *same* formatter reproduces the original date
  exactly (verified: `d 'de' MMMM 'de' yyyy` in `es_CL`, round-tripped
  correctly). This is `datetimeformatter-parseo-simetrico`'s core
  concept — a single-locale principle, not a cross-locale comparison:
  the formatter used to parse must match the one used to format (same
  pattern, same locale), or parsing throws `DateTimeParseException`.
  This concept's `solo` step tests recognizing *that mismatch failure*,
  not predicting a third locale's output.

## Per-concept exercise ladder (worked_example → guided → solo)

**Unit 3 — `formateo-numeros`**

1. `numberformat-locale-dependiente`: `worked_example` shows
   `NumberFormat.getInstance(Locale.US)` vs `.getInstance(Locale.GERMANY)`
   on the same double, side by side, calling out the separator swap by
   name. `guided` has the learner apply the same call with a given
   Locale and pick/complete the grouping behavior. `solo` (mcq or
   fill_blank in words, not exact string) asks the learner to predict
   whether `es_CL`'s output uses `en_US`-style or `de_DE`-style
   separators, testing the transfer.
2. `numberformat-moneda`: `worked_example` shows
   `getCurrencyInstance(Locale.US)` (USD, 2 decimals) vs
   `getCurrencyInstance(Locale.GERMANY)` (EUR, 2 decimals, symbol
   position differs), currency named explicitly in the text. `guided`
   reinforces reading a currency-formatted value. `solo` transfer-tests
   `es_CL`/CLP, asking the learner to predict/recognize "zero decimal
   places" as the distinguishing fact, currency named explicitly.
3. `decimalformat-patron-custom`: `worked_example` shows the same
   pattern `"#,##0.00"` with explicit `DecimalFormatSymbols` for two
   locales, producing the separator-swapped outputs. `guided` has the
   learner reason about what changes if the symbols are omitted.
   `solo` tests the landmine directly: given `new DecimalFormat(pattern)`
   with no explicit symbols, what determines its output (correct answer:
   the JVM default locale, not any locale visible in the code) — this is
   a comprehension check on the landmine, not a locale-prediction
   transfer test, since the landmine's whole point is that no locale is
   visible in the code at all.

**Unit 4 — `formateo-fechas-horas`**

4. `datetimeformatter-estilos-predefinidos`: `worked_example` shows
   `ofLocalizedDate(FormatStyle.LONG)` for `en_US` and `de_DE`, calling
   out day-first order and language change in words. `guided`
   reinforces with `FormatStyle.SHORT`. `solo` transfer-tests `es_CL`,
   asking the learner to predict day-first order and Spanish month name
   from the established pattern.
5. `datetimeformatter-patron-personalizado`: `worked_example` shows
   `ofPattern("MMMM d, yyyy", locale)` for `en_US` and `de_DE`,
   explicitly contrasting "pattern controls layout, locale controls
   vocabulary." `guided` has the learner apply the same pattern to a new
   locale and predict what changes (vocabulary) vs what doesn't (field
   order). `solo` transfer-tests `es_CL`, predicting the Spanish month
   name with the same field order as the pattern dictates.
6. `datetimeformatter-parseo-simetrico`: `worked_example` shows a
   single-locale (`es_CL`) format-then-parse round trip succeeding.
   `guided` reinforces with a different pattern/locale pairing, same
   principle. `solo`/`practice` (terminal) tests recognizing that
   parsing a string with a *different* formatter/locale than the one
   that produced it throws `DateTimeParseException` — a comprehension
   check on the symmetry requirement, not a locale-prediction task.

## Standards checklist (unchanged, reconfirmed for this sub-cycle)

- One terminal role (`solo`/`practice`) per concept, never `guided`.
- `dependsOn` is same-unit-only.
- Sequential `pathOrder` 0..n-1 within each unit.
- No case-collision between `mcq`/`fill_blank` distractors and the
  correct answer.
- `mcq` distractor length-balance: distractors must match the answer's
  length/detail; the correct answer must never be the obvious longest
  option (does not apply to `fill_blank`, which has no rendered
  distractors).
- Difficulty monotonicity per concept (`worked_example` easiest →
  `solo`/`practice` hardest).
- No accented characters anywhere in Spanish content, including
  section/unit/concept display names (matching "Localizacion",
  "Modulos", "Genericos" precedent already in the corpus).
- No voseo.
- Every locale-dependent literal in an exercise's `code` field must be
  produced by an explicit `Locale` argument — never rely on or imply an
  ambient JVM default, per the standing rule from sub-cycle 1.

## QA approach for the implementation plan

Per the lesson from sub-cycle 1's final review (a `ListResourceBundle`
snippet's underlying *fact* was JDK-verified during design, but the
*exact shipped snippet* was never re-run until the final whole-branch
review, which found it was broken due to a missing `public` modifier):
this sub-cycle's implementation plan must require that every
`worked_example`'s exact `code` field be compiled and run against a real
JDK before that task is marked complete — not deferred to the final
review as the only checkpoint. The final whole-branch review should still
independently re-verify all snippets (defense in depth), but per-task
verification during authoring is the primary gate this time.

## Content version

`CURRENT_CONTENT_VERSION` in `ContentSeeder.kt` is currently `"24"`
(reconfirmed 2026-08-28, working tree clean at `8726655`). The
implementation plan must re-verify this value again at write time and
bump it by one as part of registering the new content.
