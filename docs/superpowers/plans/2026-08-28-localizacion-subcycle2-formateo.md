# Localizacion - Sub-cycle 2 (Formateo) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the second and final sub-cycle of the "Localizacion" section
— `NumberFormat`/`DecimalFormat` and `DateTimeFormatter` formatting — as
2 new units appended to the existing `localizacion.json`, closing the
section (2/2 sub-cycles). Design rationale and JDK-20 verification log:
`docs/superpowers/specs/2026-08-28-localizacion-subcycle2-formateo-design.md`.

**Architecture:** Pure content-authoring. Unlike sub-cycle 1 (a brand-new
file), `app/src/main/assets/content/localizacion.json` already exists and
is already registered in `ContentPackRegistry.assetPaths` — this plan
only appends 2 units to that file's `units` array and bumps
`CURRENT_CONTENT_VERSION`. No registry change, no new file.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell, JUnit4 —
including `ContentCorpusLadderConsistencyTest`, which automatically scans
`localizacion.json` (already registered) for `fill_blank` `solo`/
`practice` answers that their own concept's `intro`/`guided` never taught.

## Global Constraints

- **File and registration:** modify `app/src/main/assets/content/localizacion.json`
  only — Edit, never Write/overwrite. Do NOT touch `ContentPackRegistry.kt`
  (the file is already registered). Only `ContentSeeder.kt`'s
  `CURRENT_CONTENT_VERSION` needs a Kotlin change (Task 3).
- **Unit identity:**
  - `formateo-numeros` / "Formateo de numeros", `orderIndex: 3`. 3
    concepts, 9 exercises, `pathOrder` 0-8 (restarts at 0 — per-unit,
    matching every existing unit in this file).
  - `formateo-fechas-horas` / "Formateo de fechas y horas",
    `orderIndex: 4`. 3 concepts, 9 exercises, `pathOrder` 0-8.
  - `certObjective: "localizacion"` for both (matches the file's existing
    2 units).
- **dependsOn chain, both units:** linear — concept 2 `dependsOn` concept
  1, concept 3 `dependsOn` concept 2, same-unit-only.
- **One-terminal-role rule:** every `conceptId` has exactly one exercise
  with `role: "solo"` or `role: "practice"` — never zero, never two.
- **Sequential pathOrder rule:** within each unit, `pathOrder` values run
  `0..n-1`, no gaps or duplicates, matching array order.
- **Case-collision rule:** no `mcq` distractor may differ from its own
  `answer` only by letter case.
- **Difficulty monotonicity within a concept:** all 3 exercises sharing a
  `conceptId` use the identical `difficulty`. This plan uses `difficulty:
  1` for `numberformat-locale-dependiente`, `numberformat-moneda`,
  `datetimeformatter-estilos-predefinidos`, and
  `datetimeformatter-patron-personalizado`; `2` for the two subtlest
  concepts, `decimalformat-patron-custom` (the JVM-default-symbols
  landmine) and `datetimeformatter-parseo-simetrico` (the format/parse
  symmetry requirement).
- **mcq distractor length-balance rule** (standing since I/O y NIO.2
  sub-cycle 1): every `mcq` answer and its 3 distractors below are full,
  comparable-length sentences — the correct option is never the obvious
  longest one. Verify by eyeball word-count parity during self-review.
- **No `fill_blank` in this sub-cycle** — every exercise below is
  `worked_example` or `mcq`. This is a deliberate consequence of the
  spec's "describe locale-sensitive output in words" decision: a
  `fill_blank` answer built on a locale-formatted value risks an
  invisible-character mismatch (narrow no-break space, etc.) that a
  normal space looks identical to when hand-typed. `mcq` sidesteps this
  because the exact fragile string never has to be typed by anyone —
  only read as one of several full-sentence options.
- **No accented characters anywhere in the file** (project-wide
  convention, verified with a full-file scan in Task 3, range `[À-ÿ]` —
  this also catches `ñ`; write "espanol" not "espa~nol", "anio" not
  "a~no", "ademas"/"tambien"/"segun"/"asi"/"mas" without accents, etc.,
  matching "ejecucion"/"anio"/"espanol" precedent already established in
  this same file's first 2 units). No voseo — tuteo only. **This check
  runs on the raw file text unconditionally, including inside `code`
  field comments** — this is exactly why every date example below uses
  October 5, 2026, not March: German's month name for March is "Marz"
  with an umlaut (a genuine accented character), while "Oktober" has
  none. Verify no exercise text anywhere reintroduces a literal umlaut,
  eszett, or accented Spanish vowel.
- **Currency named explicitly wherever it appears** (USD/EUR/CLP) —
  never left implicit via a bare `$`/`€` symbol, per the spec's
  content-accuracy decision (a bare `$` is ambiguous between USD and
  CLP). In `code`-field comments, write the 3-letter currency code as
  literal text (`EUR`, not `€`) even where the real JDK output uses the
  symbol — this is a deliberate stylization to avoid ever hand-copying
  the euro symbol's real output, which includes an invisible narrow
  no-break space before it that would be silently lost or altered by
  retyping.
- **Locale-sensitive output described in words in exercise prompts/
  answers/explanations, never quoted as an exact string the learner must
  reproduce** — per the same content-accuracy decision. Where a `code`
  field's comment does show a literal formatted value (illustrative
  only, never a graded answer), it uses a value independently confirmed
  free of invisible formatting characters (see the JDK verification
  block in each task) — for anything that isn't confirmed clean (e.g.
  `en_US`'s AM/PM-marked short time), the comment describes the shape in
  words instead of quoting the literal string.
- **Two-locale-contrast + third-locale-transfer-test pattern**: every
  concept's `intro`/`guided` contrasts `Locale.US` and `Locale.GERMANY`
  (maximally different: separator swap on numbers, day/month order and
  language on dates, 12h-vs-24h and marker presence on times). The
  `solo` step holds back `Locale.of("es", "CL")` as a transfer test,
  asking the learner to predict its behavior from the established
  `Locale.US`/`Locale.GERMANY` pattern — **except**
  `datetimeformatter-parseo-simetrico`, which is a single-locale
  round-trip principle; its `solo` step tests recognizing a
  mismatched-formatter parse failure instead of a third-locale
  prediction.
- **No legacy `Date`/`SimpleDateFormat` anywhere** — `java.time` only, no
  exceptions.
- **JDK verification per task, not deferred to a final review only**:
  each of Task 1 and Task 2 includes an explicit step to compile and run
  a verification snippet against a real JDK before writing that task's
  JSON, and the exact values below were already confirmed once during
  planning (OpenJDK 20, `/usr/bin/java`) — re-run them again during
  implementation to confirm the local JDK agrees, per the lesson from
  sub-cycle 1's final review (a snippet's underlying fact was verified
  during design, but the exact shipped `code` field was never re-run
  until the final review, which found it was broken).
- **Content version:** `CURRENT_CONTENT_VERSION` in `ContentSeeder.kt`
  was `"24"` when this plan was written (2026-08-28) — Task 3 re-checks
  the live value and increments by exactly one; do not assume `"24"` is
  still current at execution time.

---

### Task 1: Add Unit `formateo-numeros`

**Files:**
- Modify: `app/src/main/assets/content/localizacion.json` (append a third
  unit to the existing `"units"` array, after `resourcebundle-basico`)

**Interfaces:**
- Consumes: the file exactly as it exists today (2 units,
  `locale-basico` + `resourcebundle-basico`, 18 exercises total) — do not
  touch either existing unit.
- Produces: unit `formateo-numeros` (`orderIndex: 3`, `certObjective:
  "localizacion"`), 3 concepts: `numberformat-locale-dependiente`
  (pathOrder 0-2, no `dependsOn`), `numberformat-moneda` (pathOrder 3-5,
  `dependsOn: ["numberformat-locale-dependiente"]`),
  `decimalformat-patron-custom` (pathOrder 6-8, `dependsOn:
  ["numberformat-moneda"]`).

- [ ] **Step 1: Verify the JDK facts this task's content depends on**

Save this to a scratch file (anywhere outside the repo, e.g. `/tmp/VerifyNumbers.java`) and run it:

```java
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class VerifyNumbers {
    public static void main(String[] args) {
        double n = 1234567.891;
        NumberFormat us = NumberFormat.getInstance(Locale.US);
        NumberFormat de = NumberFormat.getInstance(Locale.GERMANY);
        System.out.println("getInstance US: " + us.format(n));
        System.out.println("getInstance DE: " + de.format(n));
        System.out.println("getInstance CL: " + NumberFormat.getInstance(Locale.of("es", "CL")).format(n));

        System.out.println("currency US: " + NumberFormat.getCurrencyInstance(Locale.US).format(1234.5));
        System.out.println("currency DE: " + NumberFormat.getCurrencyInstance(Locale.GERMANY).format(1234.5));
        System.out.println("currency CL: " + NumberFormat.getCurrencyInstance(Locale.of("es", "CL")).format(1234.5));

        DecimalFormat dfUs = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        DecimalFormat dfDe = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
        System.out.println("DecimalFormat US: " + dfUs.format(n));
        System.out.println("DecimalFormat DE: " + dfDe.format(n));
    }
}
```

Run: `javac VerifyNumbers.java && java VerifyNumbers`

Expected output (confirmed once already on OpenJDK 20, 2026-08-28 — if
your run disagrees, STOP and report the discrepancy before writing any
JSON, do not silently use whichever value you got):

```
getInstance US: 1,234,567.891
getInstance DE: 1.234.567,891
getInstance CL: 1.234.567,891
currency US: $1,234.50
currency DE: 1.234,50 €      (exact symbol/spacing may render oddly in your terminal; the content below never quotes this literal string, see Global Constraints)
currency CL: $1.234
DecimalFormat US: 1,234,567.89
DecimalFormat DE: 1.234.567,89
```

The load-bearing facts for this task's content: (a) `es_CL` matches
`de_DE`'s separator convention (period-for-grouping, comma-for-decimal),
opposite of `en_US`; (b) `es_CL`'s currency (CLP) formats with **zero
decimal places**, unlike USD and EUR's two; (c) `DecimalFormat`'s pattern
string alone does not determine separator characters — an explicit,
locale-aware `DecimalFormatSymbols` does.

- [ ] **Step 2: Insert the third unit**

In `app/src/main/assets/content/localizacion.json`, find this exact
trailing text:

```json
          "conceptId": "resourcebundle-empaquetado-seguro",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["resourcebundle-fallback"]
        }
      ]
    }
  ]
}
```

Replace it with:

```json
          "conceptId": "resourcebundle-empaquetado-seguro",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["resourcebundle-fallback"]
        }
      ]
    },
    {
      "unitId": "formateo-numeros",
      "name": "Formateo de numeros",
      "certObjective": "localizacion",
      "orderIndex": 3,
      "summary": {
        "text": "NumberFormat.getInstance()/getCurrencyInstance() dependen completamente del Locale recibido: los caracteres de separador de miles y de decimales pueden estar invertidos entre locales (en_US usa coma para miles y punto para decimales; de_DE usa punto para miles y coma para decimales). getCurrencyInstance() ademas fija el simbolo, su posicion y la cantidad de decimales segun la moneda real del Locale -- por ejemplo, CLP (peso chileno) no tiene centavos, asi que se muestra sin parte decimal, a diferencia de USD y EUR que muestran dos. DecimalFormat con un patron custom como \"#,##0.00\" solo define la forma del numero (cuantos decimales, donde va la agrupacion de miles); los caracteres exactos de cada separador vienen de un DecimalFormatSymbols explicito -- si se omite, DecimalFormat usa los simbolos del locale por defecto de la JVM en ese momento, que puede no ser el que el codigo pretende usar.",
        "code": "double n = 1234567.891;\nNumberFormat.getInstance(Locale.US).format(n);      // 1,234,567.891\nNumberFormat.getInstance(Locale.GERMANY).format(n); // 1.234.567,891\n\nNumberFormat.getCurrencyInstance(Locale.of(\"es\", \"CL\")).format(1234.5); // $1.234 (CLP, sin decimales)\n\nnew DecimalFormat(\"#,##0.00\"); // usa los simbolos del locale por defecto de la JVM si no se pasa un DecimalFormatSymbols explicito"
      },
      "exercises": [
        {
          "id": "loc-nflocale-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "NumberFormat.getInstance() usa separadores de miles y decimales distintos segun el Locale recibido",
          "code": "double n = 1234567.891;\nNumberFormat us = NumberFormat.getInstance(Locale.US);\nNumberFormat de = NumberFormat.getInstance(Locale.GERMANY);\nSystem.out.println(us.format(n)); // 1,234,567.891\nSystem.out.println(de.format(n)); // 1.234.567,891",
          "answer": "ok",
          "explanation": "NumberFormat.getInstance(locale) usa los caracteres de separador que corresponden a ese Locale. Locale.US usa coma para agrupar miles y punto para los decimales; Locale.GERMANY usa exactamente al reves: punto para agrupar miles y coma para los decimales. Es el mismo numero, formateado con dos convenciones opuestas.",
          "conceptId": "numberformat-locale-dependiente",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "loc-nflocale-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que caracteriza la diferencia entre NumberFormat.getInstance(Locale.US) y NumberFormat.getInstance(Locale.GERMANY) sobre el mismo numero?",
          "answer": "Los caracteres de separador de miles y de decimales estan invertidos entre ambos locales, coma y punto intercambiados",
          "distractors": [
            "Locale.GERMANY redondea el numero a menos cantidad de decimales que Locale.US, ademas de cambiar el separador",
            "Locale.US no agrega ningun separador de miles, mientras que Locale.GERMANY si lo agrega siempre",
            "Ambos locales producen exactamente el mismo texto, ya que getInstance() ignora el Locale recibido"
          ],
          "explanation": "El numero y la cantidad de decimales son los mismos en ambos casos; lo unico que cambia es que caracter representa cada separador. Locale.US usa coma para miles y punto para decimales; Locale.GERMANY usa la combinacion opuesta, punto para miles y coma para decimales.",
          "conceptId": "numberformat-locale-dependiente",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "loc-nflocale-solo",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Segun el patron anterior (Locale.US usa coma para miles y punto para decimales; Locale.GERMANY usa la combinacion opuesta), que separadores esperarias al formatear el mismo numero con NumberFormat.getInstance(Locale.of(\"es\", \"CL\"))?",
          "answer": "Los mismos que Locale.GERMANY: punto para agrupar miles y coma para los decimales",
          "distractors": [
            "Los mismos que Locale.US: coma para agrupar miles y punto para los decimales",
            "Ninguno de los dos patrones: Locale.of(\"es\", \"CL\") no agrega ningun separador de miles",
            "Depende del sistema operativo del dispositivo, no del Locale que reciba el metodo"
          ],
          "explanation": "Locale.of(\"es\", \"CL\") sigue la misma convencion que Locale.GERMANY: punto para agrupar los miles y coma para los decimales, lo opuesto de Locale.US. No hay que memorizar tres convenciones distintas -- hay que reconocer cual de las dos familias sigue cada Locale.",
          "conceptId": "numberformat-locale-dependiente",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "loc-nfmoneda-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "getCurrencyInstance() liga el simbolo, su posicion y la cantidad de decimales a la moneda real del Locale",
          "code": "NumberFormat usd = NumberFormat.getCurrencyInstance(Locale.US);\nNumberFormat eur = NumberFormat.getCurrencyInstance(Locale.GERMANY);\nSystem.out.println(usd.format(1234.5)); // $1,234.50 (USD, dos decimales)\nSystem.out.println(eur.format(1234.5)); // 1.234,50 EUR (dos decimales, simbolo despues del numero)",
          "answer": "ok",
          "explanation": "getCurrencyInstance(locale) no solo cambia el separador de miles/decimales (igual que getInstance()): tambien fija el simbolo de moneda y su posicion segun la moneda real asociada a ese Locale. USD pone el simbolo antes del numero; EUR lo pone despues. Ambas monedas usan dos decimales.",
          "conceptId": "numberformat-moneda",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["numberformat-locale-dependiente"]
        },
        {
          "id": "loc-nfmoneda-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Ademas de cambiar el simbolo, que otra diferencia hay entre getCurrencyInstance(Locale.US) (USD) y getCurrencyInstance(Locale.GERMANY) (EUR) sobre el mismo monto?",
          "answer": "La posicion del simbolo respecto al numero cambia: USD lo pone antes del numero, EUR lo pone despues",
          "distractors": [
            "El monto se redondea a una cantidad distinta de decimales entre USD y EUR, ademas de cambiar el simbolo",
            "getCurrencyInstance() ignora el monto recibido y siempre formatea 0.00 sin importar el Locale",
            "EUR nunca incluye separador de miles, a diferencia de USD que siempre lo incluye"
          ],
          "explanation": "USD y EUR usan la misma cantidad de decimales (dos) sobre el mismo monto; lo que cambia entre ambas es la posicion del simbolo respecto al numero -- USD antes, EUR despues -- ademas del separador de miles/decimales ya visto en el concepto anterior.",
          "conceptId": "numberformat-moneda",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["numberformat-locale-dependiente"]
        },
        {
          "id": "loc-nfmoneda-solo",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Si formateas el mismo monto con NumberFormat.getCurrencyInstance(Locale.of(\"es\", \"CL\")) (CLP, peso chileno), que hace distinta a esta moneda de USD y EUR en el resultado?",
          "answer": "CLP no tiene decimales (no usa centavos), asi que el resultado se muestra sin parte decimal, a diferencia de USD y EUR",
          "distractors": [
            "CLP usa exactamente los mismos dos decimales que USD y EUR, solo cambia el simbolo mostrado en el resultado",
            "CLP redondea el monto siempre hacia arriba antes de formatear, algo que USD y EUR nunca hacen",
            "CLP lanza una excepcion en tiempo de ejecucion si el monto tiene alguna parte decimal distinta de cero"
          ],
          "explanation": "CLP (peso chileno) no tiene una unidad menor que el peso -- no existen centavos -- asi que getCurrencyInstance() para Locale.of(\"es\", \"CL\") formatea sin ninguna parte decimal, a diferencia de USD y EUR que siempre muestran dos. La cantidad de decimales depende de la moneda real, no es un valor fijo del metodo.",
          "conceptId": "numberformat-moneda",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["numberformat-locale-dependiente"]
        },
        {
          "id": "loc-dfpatron-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "El patron de DecimalFormat solo define la forma del numero; los caracteres de separador vienen de un DecimalFormatSymbols explicito",
          "code": "double n = 1234567.891;\nDecimalFormat us = new DecimalFormat(\"#,##0.00\", DecimalFormatSymbols.getInstance(Locale.US));\nDecimalFormat de = new DecimalFormat(\"#,##0.00\", DecimalFormatSymbols.getInstance(Locale.GERMANY));\nSystem.out.println(us.format(n)); // 1,234,567.89\nSystem.out.println(de.format(n)); // 1.234.567,89",
          "answer": "ok",
          "explanation": "El mismo patron \"#,##0.00\" (dos decimales, agrupacion de miles) produce resultados distintos segun el DecimalFormatSymbols que se le pase: el patron fija la forma, el DecimalFormatSymbols fija que caracter representa cada separador. Sin un DecimalFormatSymbols explicito, DecimalFormat usa los simbolos del locale por defecto de la JVM en ese momento.",
          "conceptId": "decimalformat-patron-custom",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["numberformat-moneda"]
        },
        {
          "id": "loc-dfpatron-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que define exactamente el patron \"#,##0.00\" en new DecimalFormat(patron, symbols)?",
          "answer": "La forma del numero (cuantos decimales, donde va la agrupacion de miles), no que caracter representa cada separador",
          "distractors": [
            "Tanto la forma del numero como los caracteres exactos de cada separador, sin necesidad de un DecimalFormatSymbols",
            "Unicamente el Locale completo que se debe usar para formatear el numero de principio a fin",
            "La cantidad maxima de digitos totales permitidos antes de lanzar una excepcion en tiempo de ejecucion"
          ],
          "explanation": "El patron describe la estructura (cuantos decimales, cada cuantos digitos va un separador de agrupacion), pero nunca dice que simbolo usar para cada uno -- eso viene siempre de un DecimalFormatSymbols, ya sea el que se pasa explicitamente o el del locale por defecto de la JVM si no se pasa ninguno.",
          "conceptId": "decimalformat-patron-custom",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["numberformat-moneda"]
        },
        {
          "id": "loc-dfpatron-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Si construyes new DecimalFormat(\"#,##0.00\") SIN pasar un DecimalFormatSymbols explicito, que determina los caracteres de separador que va a usar?",
          "answer": "El locale por defecto de la JVM en ese momento, que puede no ser el que el codigo pretende usar",
          "distractors": [
            "El patron mismo ya fija los caracteres exactos de cada separador, sin depender de ningun locale",
            "Siempre usa coma para miles y punto para decimales, sin importar el locale por defecto de la JVM",
            "El codigo no compila: DecimalFormat exige recibir siempre un DecimalFormatSymbols explicito"
          ],
          "explanation": "Sin un DecimalFormatSymbols explicito, DecimalFormat construye uno internamente a partir del locale por defecto de la JVM en ese momento -- un valor ambiental que ningun Locale visible en el codigo controla. Por eso pasar DecimalFormatSymbols explicitamente es la unica forma segura de garantizar un resultado predecible.",
          "conceptId": "decimalformat-patron-custom",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["numberformat-moneda"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 3: Validate the file parses**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/localizacion.json')); print('OK')"`
Expected: `OK`, no exception.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/content/localizacion.json
git commit -m "content: add formateo-numeros unit to Localizacion"
```

---

### Task 2: Add Unit `formateo-fechas-horas`

**Files:**
- Modify: `app/src/main/assets/content/localizacion.json` (append a
  fourth unit, after `formateo-numeros`)

**Interfaces:**
- Consumes: the file exactly as Task 1 left it — do not touch any of the
  first 3 units or their 27 exercises.
- Produces: unit `formateo-fechas-horas` (`orderIndex: 4`,
  `certObjective: "localizacion"`), 3 concepts:
  `datetimeformatter-estilos-predefinidos` (pathOrder 0-2, no
  `dependsOn`), `datetimeformatter-patron-personalizado` (pathOrder 3-5,
  `dependsOn: ["datetimeformatter-estilos-predefinidos"]`),
  `datetimeformatter-parseo-simetrico` (pathOrder 6-8, `dependsOn:
  ["datetimeformatter-patron-personalizado"]`).

- [ ] **Step 1: Verify the JDK facts this task's content depends on**

Save this to a scratch file and run it:

```java
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;

public class VerifyDates {
    public static void main(String[] args) {
        LocalDate fecha = LocalDate.of(2026, 10, 5);
        LocalTime hora = LocalTime.of(14, 30);
        Locale US = Locale.US;
        Locale DE = Locale.GERMANY;
        Locale CL = Locale.of("es", "CL");

        System.out.println("LONG US: " + fecha.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(US)));
        System.out.println("LONG DE: " + fecha.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(DE)));
        System.out.println("LONG CL: " + fecha.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(CL)));

        System.out.println("TIME SHORT DE: " + hora.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(DE)));
        System.out.println("TIME SHORT CL: " + hora.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(CL)));

        System.out.println("ofPattern US: " + fecha.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", US)));
        System.out.println("ofPattern DE: " + fecha.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", DE)));
        System.out.println("ofPattern CL: " + fecha.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", CL)));

        DateTimeFormatter esFmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", CL);
        String rendered = fecha.format(esFmt);
        System.out.println("rendered: " + rendered);
        System.out.println("round trip equals original: " + LocalDate.parse(rendered, esFmt).equals(fecha));

        DateTimeFormatter usFmt = DateTimeFormatter.ofPattern("MMMM d, yyyy", US);
        try {
            LocalDate.parse(rendered, usFmt);
            System.out.println("mismatched parse: NO EXCEPTION (unexpected)");
        } catch (DateTimeParseException e) {
            System.out.println("mismatched parse threw: " + e.getClass().getSimpleName());
        }
    }
}
```

Run: `javac VerifyDates.java && java VerifyDates`

Expected output (confirmed once already on OpenJDK 20, 2026-08-28 — if
your run disagrees, STOP and report before writing any JSON):

```
LONG US: October 5, 2026
LONG DE: 5. Oktober 2026
LONG CL: 5 de octubre de 2026
TIME SHORT DE: 14:30
TIME SHORT CL: 14:30
ofPattern US: October 5, 2026
ofPattern DE: Oktober 5, 2026
ofPattern CL: octubre 5, 2026
rendered: 5 de octubre de 2026
round trip equals original: true
mismatched parse threw: DateTimeParseException
```

Load-bearing facts for this task's content: (a) `Locale.US`'s
`ofLocalizedDate(LONG)` puts the month first, in English;
`Locale.GERMANY`'s puts the day first, in German (`Oktober`, no umlaut —
confirmed clean of accented characters, safe to quote literally); (b)
`Locale.of("es", "CL")` follows `Locale.GERMANY`'s day-first date order
AND its 24-hour/no-AM-PM-marker time convention, on both axes, not
`Locale.US`'s; (c) `ofPattern(pattern, locale)` keeps the pattern's field
order and punctuation identical across all 3 locales — only the month's
language changes; (d) formatting then parsing with the *same*
`DateTimeFormatter` round-trips exactly; parsing with a *different*
formatter/locale than the one that produced the string throws
`DateTimeParseException`.

- [ ] **Step 2: Insert the fourth unit**

In `app/src/main/assets/content/localizacion.json`, find this exact
trailing text (the end of Task 1's `formateo-numeros` unit):

```json
          "conceptId": "decimalformat-patron-custom",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["numberformat-moneda"]
        }
      ]
    }
  ]
}
```

Replace it with:

```json
          "conceptId": "decimalformat-patron-custom",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["numberformat-moneda"]
        }
      ]
    },
    {
      "unitId": "formateo-fechas-horas",
      "name": "Formateo de fechas y horas",
      "certObjective": "localizacion",
      "orderIndex": 4,
      "summary": {
        "text": "DateTimeFormatter.ofLocalizedDate()/ofLocalizedTime() con un FormatStyle y withLocale() cambian dos cosas independientes segun el Locale: el orden/idioma de la fecha, y si la hora usa 12 o 24 horas (con o sin marcador AM/PM). Locale.US usa mes primero y 12 horas con marcador; Locale.GERMANY usa dia primero y 24 horas sin marcador. ofPattern(patron, locale) separa dos responsabilidades: el patron fija el orden y la puntuacion de los campos, el Locale fija el idioma de nombres como el mes -- el patron nunca cambia entre locales, solo el idioma de lo que se muestra. Para que un texto formateado se pueda volver a parsear exactamente a la fecha original, format() y parse() deben usar el mismo DateTimeFormatter (mismo patron, mismo Locale); parsear con un formatter distinto al que genero el texto lanza DateTimeParseException.",
        "code": "LocalDate fecha = LocalDate.of(2026, 10, 5);\n\nDateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.US).format(fecha);      // October 5, 2026\nDateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.GERMANY).format(fecha); // 5. Oktober 2026\n\nDateTimeFormatter fmt = DateTimeFormatter.ofPattern(\"d 'de' MMMM 'de' yyyy\", Locale.of(\"es\", \"CL\"));\nString texto = fecha.format(fmt);\nLocalDate.parse(texto, fmt).equals(fecha); // true -- mismo formatter para format() y parse()"
      },
      "exercises": [
        {
          "id": "loc-dtfestilos-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "ofLocalizedDate()/ofLocalizedTime() con withLocale() cambian el orden/idioma de la fecha y si la hora usa 12 o 24 horas",
          "code": "LocalDate fecha = LocalDate.of(2026, 10, 5);\nLocalTime hora = LocalTime.of(14, 30);\n\nDateTimeFormatter fechaUS = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.US);\nDateTimeFormatter fechaDE = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.GERMANY);\nSystem.out.println(fecha.format(fechaUS)); // October 5, 2026 (mes primero, en ingles)\nSystem.out.println(fecha.format(fechaDE)); // 5. Oktober 2026 (dia primero, en aleman)\n\nDateTimeFormatter horaUS = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.US);\nDateTimeFormatter horaDE = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.GERMANY);\nSystem.out.println(hora.format(horaUS)); // formato de 12 horas, con marcador AM/PM\nSystem.out.println(hora.format(horaDE)); // 14:30 (formato de 24 horas, sin marcador)",
          "answer": "ok",
          "explanation": "El Locale controla dos cosas independientes al formatear fecha y hora: el orden de los campos de la fecha (junto con el idioma de nombres como el mes), y si la hora se muestra en formato de 12 horas con marcador AM/PM o en formato de 24 horas sin marcador. Locale.US usa mes-primero y 12 horas con marcador; Locale.GERMANY usa dia-primero y 24 horas sin marcador.",
          "conceptId": "datetimeformatter-estilos-predefinidos",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "loc-dtfestilos-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Ademas del idioma del nombre del mes, que par de diferencias adicionales hay entre Locale.US y Locale.GERMANY al usar ofLocalizedDate(LONG) y ofLocalizedTime(SHORT) juntos?",
          "answer": "El orden de los campos de fecha cambia (mes primero vs dia primero), y el formato de hora cambia (12 horas con marcador vs 24 horas sin marcador)",
          "distractors": [
            "Solo cambia el orden de los campos de fecha (mes primero vs dia primero); el formato de la hora es siempre el mismo entre ambos locales, sin diferencia",
            "Solo cambia el formato de hora (12 horas con marcador vs 24 horas sin marcador); el orden de los campos de fecha es siempre el mismo entre ambos",
            "No hay ninguna otra diferencia real ademas del idioma: ambos locales terminan mostrando exactamente el mismo orden de fecha y el mismo formato de hora"
          ],
          "explanation": "Locale.US y Locale.GERMANY difieren en dos ejes independientes: el orden de la fecha (mes-primero vs dia-primero) y el formato de la hora (12 horas con marcador AM/PM vs 24 horas sin marcador). Ninguno de los dos ejes depende del otro -- cada Locale fija ambos por separado.",
          "conceptId": "datetimeformatter-estilos-predefinidos",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "loc-dtfestilos-solo",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Si Locale.of(\"es\", \"CL\") sigue el mismo patron que Locale.GERMANY en estos dos aspectos, que esperarias al formatear la misma fecha y hora con ese Locale?",
          "answer": "Dia primero en la fecha (con el mes en espanol), y hora en formato de 24 horas sin marcador AM/PM",
          "distractors": [
            "Mes primero en la fecha (con el mes en espanol), y hora en formato de 12 horas con marcador AM/PM",
            "Dia primero en la fecha, pero hora en formato de 12 horas con marcador AM/PM, igual que Locale.US",
            "Mes primero en la fecha, igual que Locale.US, pero hora en formato de 24 horas sin marcador"
          ],
          "explanation": "Locale.of(\"es\", \"CL\") sigue la misma convencion que Locale.GERMANY en ambos ejes: dia primero en la fecha (con el nombre del mes en espanol) y hora en formato de 24 horas sin marcador AM/PM. No hay que memorizar un tercer patron -- hay que reconocer que este Locale cae en la misma familia que Locale.GERMANY, no en la de Locale.US.",
          "conceptId": "datetimeformatter-estilos-predefinidos",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "loc-dtfpatron-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "ofPattern(patron, locale) separa dos responsabilidades: el patron fija orden/puntuacion, el Locale fija el idioma",
          "code": "LocalDate fecha = LocalDate.of(2026, 10, 5);\nDateTimeFormatter us = DateTimeFormatter.ofPattern(\"MMMM d, yyyy\", Locale.US);\nDateTimeFormatter de = DateTimeFormatter.ofPattern(\"MMMM d, yyyy\", Locale.GERMANY);\nSystem.out.println(fecha.format(us)); // October 5, 2026\nSystem.out.println(fecha.format(de)); // Oktober 5, 2026",
          "answer": "ok",
          "explanation": "El patron \"MMMM d, yyyy\" fija el orden de los campos y su puntuacion exacta (mes-dia-anio, con esa coma) igual en ambos casos -- eso nunca cambia. Lo unico que cambia entre Locale.US y Locale.GERMANY es el idioma del nombre del mes, que viene del Locale, no del patron.",
          "conceptId": "datetimeformatter-patron-personalizado",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["datetimeformatter-estilos-predefinidos"]
        },
        {
          "id": "loc-dtfpatron-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "En DateTimeFormatter.ofPattern(patron, locale), que controla el patron (\"MMMM d, yyyy\") y que controla el Locale?",
          "answer": "El patron controla el orden y la puntuacion de los campos; el Locale controla el idioma de nombres como el mes",
          "distractors": [
            "El patron controla el idioma de los nombres; el Locale controla el orden y la puntuacion de los campos",
            "Ambos controlan lo mismo: el orden, la puntuacion y el idioma vienen siempre fijados por el patron",
            "El Locale no tiene ningun efecto sobre ofPattern(), solo afecta a ofLocalizedDate()/ofLocalizedTime()"
          ],
          "explanation": "El patron es la parte fija de la plantilla: que campos aparecen, en que orden, con que puntuacion literal. El Locale solo decide el idioma de lo que ese patron produce -- por ejemplo, el nombre del mes -- sin alterar el orden ni la puntuacion que el patron ya definio.",
          "conceptId": "datetimeformatter-patron-personalizado",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["datetimeformatter-estilos-predefinidos"]
        },
        {
          "id": "loc-dtfpatron-solo",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Si aplicas el mismo patron \"MMMM d, yyyy\" con Locale.of(\"es\", \"CL\") a la fecha del 5 de octubre de 2026, que resultado esperas?",
          "answer": "octubre 5, 2026 -- el mismo orden y puntuacion que el patron, con el nombre del mes en espanol",
          "distractors": [
            "5 de octubre, 2026 -- ofPattern() cambia el orden de los campos automaticamente segun el idioma del Locale",
            "October 5, 2026 -- ofPattern() ignora por completo el Locale recibido como segundo argumento",
            "05/10/2026 -- ofPattern() reemplaza el patron dado por el formato corto propio del Locale recibido"
          ],
          "explanation": "El patron \"MMMM d, yyyy\" mantiene exactamente el mismo orden y la misma puntuacion sin importar el Locale -- mes, luego dia, coma, luego anio. Lo unico que cambia con Locale.of(\"es\", \"CL\") es que el nombre del mes se muestra en espanol (\"octubre\"), igual que cambio a aleman con Locale.GERMANY.",
          "conceptId": "datetimeformatter-patron-personalizado",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["datetimeformatter-estilos-predefinidos"]
        },
        {
          "id": "loc-dtfparseo-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "format() y parse() deben usar el mismo DateTimeFormatter para que el texto se reconstruya exactamente a la fecha original",
          "code": "DateTimeFormatter fmt = DateTimeFormatter.ofPattern(\"d 'de' MMMM 'de' yyyy\", Locale.of(\"es\", \"CL\"));\nLocalDate original = LocalDate.of(2026, 10, 5);\nString texto = original.format(fmt); // 5 de octubre de 2026\nLocalDate reconstruida = LocalDate.parse(texto, fmt);\nSystem.out.println(reconstruida.equals(original)); // true",
          "answer": "ok",
          "explanation": "El mismo DateTimeFormatter (mismo patron, mismo Locale) que genero el texto tambien lo interpreta correctamente al parsear, sin ninguna ambiguedad -- por eso reconstruida.equals(original) da true. Si el texto se parseara con un formatter distinto (otro patron u otro Locale), esa garantia desaparece.",
          "conceptId": "datetimeformatter-parseo-simetrico",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["datetimeformatter-patron-personalizado"]
        },
        {
          "id": "loc-dtfparseo-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que original.format(fmt) seguido de LocalDate.parse(texto, fmt) con el MISMO fmt reconstruye exactamente la fecha original?",
          "answer": "Porque el mismo patron y el mismo Locale que generaron el texto tambien lo interpretan al parsear, sin ambiguedad",
          "distractors": [
            "Porque LocalDate.parse() siempre reconstruye la fecha original sin importar que formatter se use para parsear",
            "Porque LocalDate ordena internamente los campos, sin depender de ningun formatter para parsear el texto",
            "Porque el patron usado incluye el anio completo, y eso alcanza para que cualquier parseo sea correcto"
          ],
          "explanation": "La garantia no viene de LocalDate ni del patron por si solos: viene de usar el mismo DateTimeFormatter (mismo patron, mismo Locale) tanto para generar el texto como para interpretarlo de vuelta. Ese formatter sabe exactamente que forma y que idioma esperar, porque es el mismo que los produjo.",
          "conceptId": "datetimeformatter-parseo-simetrico",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["datetimeformatter-patron-personalizado"]
        },
        {
          "id": "loc-dtfparseo-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Si formateas una fecha con DateTimeFormatter.ofPattern(\"d 'de' MMMM 'de' yyyy\", Locale.of(\"es\", \"CL\")) y luego intentas parsear ese mismo texto con DateTimeFormatter.ofPattern(\"MMMM d, yyyy\", Locale.US) (patron y Locale distintos), que ocurre?",
          "answer": "Se lanza DateTimeParseException, porque el texto generado no coincide con la forma que el segundo formatter espera leer",
          "distractors": [
            "El parseo funciona igual, ya que LocalDate.parse() reconoce automaticamente cualquier formato de fecha",
            "Se reconstruye la fecha original correctamente, pero con el nombre del mes traducido al ingles",
            "Se reconstruye una fecha distinta a la original, sin lanzar ninguna excepcion en tiempo de ejecucion"
          ],
          "explanation": "LocalDate.parse() no adivina el formato: exige que el texto coincida con la forma exacta que el formatter recibido espera (mismo orden de campos, misma puntuacion, mismo idioma de nombres). Un patron y Locale distintos a los que generaron el texto no coinciden con esa forma, asi que se lanza DateTimeParseException en vez de reconstruir cualquier fecha.",
          "conceptId": "datetimeformatter-parseo-simetrico",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["datetimeformatter-patron-personalizado"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 3: Validate the file parses**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/localizacion.json')); print('OK')"`
Expected: `OK`, no exception.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/content/localizacion.json
git commit -m "content: add formateo-fechas-horas unit to Localizacion"
```

---

### Task 3: Validate the whole file, bump content version, run full suite

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
  (bump `CURRENT_CONTENT_VERSION` only)
- Read-only validation: `app/src/main/assets/content/localizacion.json`
- No change needed to `ContentPackRegistry.kt` — the file is already
  registered.

**Interfaces:**
- Consumes: the final state of `localizacion.json` after Task 2 (4
  units, 36 exercises total).
- Produces: `CURRENT_CONTENT_VERSION` bumped by one from whatever it is
  at dispatch time (check `ContentSeeder.kt`'s current value first — do
  not assume `"24"`; other cycles may have landed between this plan
  being written and executed).

- [ ] **Step 1: Write and run a full validation script**

```bash
python3 << 'EOF'
import json, re

path = "app/src/main/assets/content/localizacion.json"
data = json.load(open(path))

assert data["sectionId"] == "java-localizacion"
assert data["orderIndex"] == 8
assert data["examVersion"] == "core"

all_units = {u["unitId"]: u for u in data["units"]}
expected_units = {"locale-basico", "resourcebundle-basico", "formateo-numeros", "formateo-fechas-horas"}
assert set(all_units.keys()) == expected_units, f"unexpected units: {list(all_units.keys())}"

expected_counts = {"locale-basico": 9, "resourcebundle-basico": 9, "formateo-numeros": 9, "formateo-fechas-horas": 9}
expected_order = {"locale-basico": 1, "resourcebundle-basico": 2, "formateo-numeros": 3, "formateo-fechas-horas": 4}
all_exercises = []
for uid, expected in expected_counts.items():
    unit = all_units[uid]
    exercises = unit["exercises"]
    assert len(exercises) == expected, f"{uid}: expected {expected} exercises, got {len(exercises)}"
    assert unit["certObjective"] == "localizacion", f"{uid}: unexpected certObjective {unit['certObjective']}"
    assert unit["orderIndex"] == expected_order[uid], f"{uid}: unexpected orderIndex {unit['orderIndex']}"
    all_exercises.append((uid, exercises))
print("Unit counts, certObjective, and orderIndex all correct.")

# Case-collision rule (whole file).
for uid, exercises in all_exercises:
    for e in exercises:
        ans = e.get("answer")
        for d in e.get("distractors", []):
            assert not (isinstance(ans, str) and d.lower() == ans.lower() and d != ans), \
                f"{uid}/{e['id']}: distractor '{d}' differs from answer only by case"

# One-terminal-role rule + dependsOn same-unit-only rule + sequential pathOrder + difficulty monotonicity, per unit.
for uid, exercises in all_exercises:
    concept_ids = {e["conceptId"] for e in exercises if e.get("conceptId")}
    terminal_counts = {}
    orders = []
    difficulty_by_concept = {}
    for e in exercises:
        orders.append(e.get("pathOrder"))
        if e.get("role") in ("solo", "practice"):
            cid = e["conceptId"]
            terminal_counts[cid] = terminal_counts.get(cid, 0) + 1
        for dep in e.get("dependsOn", []):
            assert dep in concept_ids, f"{uid}/{e['id']}: dependsOn '{dep}' not a concept in this unit"
        cid = e.get("conceptId")
        if cid:
            difficulty_by_concept.setdefault(cid, set()).add(e["difficulty"])
    for cid in concept_ids:
        assert terminal_counts.get(cid) == 1, f"{uid}/concept '{cid}' has {terminal_counts.get(cid, 0)} terminal exercises, expected 1"
    assert orders == list(range(len(orders))), f"{uid}: pathOrder not sequential 0..n-1: {sorted(orders)}"
    for cid, diffs in difficulty_by_concept.items():
        assert len(diffs) == 1, f"{uid}/concept '{cid}' has non-uniform difficulty: {diffs}"
print("One-terminal-role, dependsOn, sequential pathOrder, and difficulty-monotonicity rules all passed.")

# No accented characters (whole-file check) and no predict_output/fill_blank in the 2 new units.
raw = open(path, encoding="utf-8").read()
accented = re.findall(r"[À-ÿ]", raw)
assert not accented, f"found accented characters: {accented}"
new_unit_ids = {"formateo-numeros", "formateo-fechas-horas"}
disallowed_types = [e["id"] for uid, exercises in all_exercises for e in exercises
                     if uid in new_unit_ids and e.get("type") not in ("worked_example", "mcq")]
assert not disallowed_types, f"only worked_example/mcq allowed in the new units, found others: {disallowed_types}"
print("No accented characters; new units contain only worked_example/mcq as planned.")

# Reachability simulation per unit, matching GetTodaySessionUseCase.selectPathExercises exactly.
for uid, exercises in all_exercises:
    answered = set()
    sessions = 0
    limit = 5
    while len(answered) < len(exercises):
        born = {e["conceptId"] for e in exercises
                if e.get("conceptId") and e.get("role") in ("solo", "practice") and e["id"] in answered}
        candidates = [e for e in exercises if e["id"] not in answered and
                      (e.get("conceptId") is None or
                       (e["conceptId"] not in born and all(d in born for d in e.get("dependsOn", []))))]
        assert candidates, f"{uid}: STRANDED after {sessions} sessions, unreached: {[e['id'] for e in exercises if e['id'] not in answered]}"
        candidates.sort(key=lambda e: e.get("pathOrder") if e.get("pathOrder") is not None else 10**9)
        for e in candidates[:limit]:
            answered.add(e["id"])
        sessions += 1
    print(f"{uid}: drains cleanly in {sessions} sessions ({len(exercises)} exercises)")

total = sum(len(ex) for _, ex in all_exercises)
print(f"Total exercises across all 4 units: {total}")
assert total == 36
EOF
```

Expected: `Unit counts, certObjective, and orderIndex all correct.` then
`One-terminal-role, dependsOn, sequential pathOrder, and difficulty-
monotonicity rules all passed.` then `No accented characters; new units
contain only worked_example/mcq as planned.` then four reachability
lines (one per unit), then `Total exercises across all 4 units: 36`, no
assertion errors.

- [ ] **Step 2: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find the current `CURRENT_CONTENT_VERSION` value and increment it by
one (check the file first — do not assume `"24"`).

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest --rerun-tasks`
Expected: BUILD SUCCESSFUL, all existing tests still passing, including
`ContentCorpusLadderConsistencyTest` picking up the 2 new units
automatically (no test file changes needed for this task). Use
`--rerun-tasks` — content-only JSON edits are not tracked as a Gradle
test input, so a bare run can silently report a stale `UP-TO-DATE`
result.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: register formateo units, bump content version, close Localizacion section"
```

---

## After the plan: manual on-device QA

Install a clean/in-place build (in-place is fine — the
`CURRENT_CONTENT_VERSION` bump triggers an automatic re-seed) and verify
on-device. `adb input tap` automation is not reliable for answering
exercises (only for navigation/screenshots) — the controller navigates
screens/worked_example CONTINUAR taps, the human answers every graded
exercise. Play both new units end to end (18 exercises total),
confirming: `worked_example` intros render before their guided/solo
steps; the `dependsOn` chain unlocks concepts one at a time within each
unit; `formateo-numeros` stays locked until `resourcebundle-basico` is
fully complete, and `formateo-fechas-horas` stays locked until
`formateo-numeros` is fully complete (`orderIndex`-driven unit unlock);
every `solo` transfer-test question reads clearly on a real device
(these are the longest/most information-dense mcq prompts in this
sub-cycle, worth specifically checking they don't get cut off or wrap
awkwardly); the new units appear correctly positioned in Ruta right
after `resourcebundle-basico`; this closes the "Localizacion" section
(2/2 sub-cycles) — no further sub-cycle is pending for this section.
