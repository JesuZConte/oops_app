# Localizacion - Sub-cycle 1 (Locale + ResourceBundle) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the brand-new "Localizacion" section, covering its first
sub-cycle (`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`, item 8):
`Locale` (construccion, codigos, locale por defecto) and `ResourceBundle`
(carga de `.properties`, fallback, bundles basados en clase + mensajes
seguros con `MessageFormat`). Formateo (`NumberFormat`/`DateTimeFormatter`)
is explicitly deferred to sub-cycle 2. Design rationale and JDK
verification log:
`docs/superpowers/specs/2026-08-19-localizacion-subcycle1-locale-resourcebundle-design.md`.

**Architecture:** Pure content-authoring plus one minimal, mechanical
Kotlin registration change — same pattern as every prior brand-new-section
cycle in this series (Modulos y Empaquetado sub-cycle 1, I/O y NIO.2
sub-cycle 1). The new content lives entirely in a new file,
`app/src/main/assets/content/localizacion.json`; the only Kotlin diff is
appending its path to `ContentPackRegistry.assetPaths` and bumping
`CURRENT_CONTENT_VERSION` in `ContentSeeder.kt`. Zero grandfathering
concern — every exercise in this brand-new file is new.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell, JUnit4 for
the existing use-case test suite (must stay green) — including
`ContentCorpusLadderConsistencyTest`, which automatically scans every
file in `ContentPackRegistry.assetPaths` (this new file included, once
Task 3 registers it) for `fill_blank` `solo`/`practice` answers that
their own concept's `intro`/`guided` never taught.

## Global Constraints

- **Section identity:** `sectionId: "java-localizacion"`,
  `name: "Localizacion"`, `orderIndex: 8` (sections 1-7 are already
  taken; I/O y NIO.2 is 7). `examVersion: "core"` (matches every existing
  section file except `streams.json`, which uses `"java21"`).
- **`certObjective` is a single value shared by every unit in the file**
  (confirmed pattern across all existing content files). Both units in
  this new file use `"certObjective": "localizacion"`.
- **New file, not an append** — Task 1 uses Write (not Edit) to create
  `app/src/main/assets/content/localizacion.json` from scratch. Task 2
  then uses Edit to insert the second unit before the file's closing
  `]`/`}`. Both tasks must produce valid, complete JSON on their own —
  verify with `python3 -c "import json; json.load(open(...))"` after each.
- **Unit identity:**
  - `locale-basico` / "Locale", `orderIndex: 1`. 3 concepts, 9
    exercises, `pathOrder` 0-8.
  - `resourcebundle-basico` / "ResourceBundle", `orderIndex: 2`. 3
    concepts, 9 exercises, `pathOrder` 0-8 (restarts at 0 — `pathOrder`
    is per-unit, matching every existing multi-unit file in this
    corpus). There is no unit-level `dependsOn` field in this schema —
    sequencing between the two units comes entirely from `orderIndex`
    plus `GetLearningPathUseCase`'s `previousUnitComplete` check.
- **dependsOn chain, both units:** each unit's 3 concepts form a single
  linear chain (concept 2 `dependsOn` concept 1; concept 3 `dependsOn`
  concept 2) — the pedagogical order matters here (construction before
  reading codes/variants before the JVM-wide `setDefault()` effect;
  `.properties` basics before the fallback-chain trap before the
  class-based-bundle/`MessageFormat` alternative).
- **One-terminal-role rule:** every `conceptId` has exactly one exercise
  with `role: "solo"` or `role: "practice"` — never zero, never two.
- **dependsOn same-unit-only rule:** every id listed in a `dependsOn`
  array must be a `conceptId` that exists elsewhere in the *same* unit's
  `exercises` array.
- **Sequential pathOrder rule:** within each unit, `pathOrder` values run
  `0..n-1` with no gaps or duplicates, in the same physical array order.
- **Case-collision rule:** no `mcq`/`fill_blank` exercise's `distractors`
  may differ from its own `answer` only by letter case.
- **Difficulty monotonicity within a concept:** all 3 exercises sharing a
  `conceptId` use the identical `difficulty` value. This plan uses
  `difficulty: 1` for `locale-construccion`, `locale-codigos`,
  `locale-default`, and `resourcebundle-properties-basico`; `2` for
  `resourcebundle-fallback` and `resourcebundle-empaquetado-seguro` (the
  two subtlest concepts in this sub-cycle).
- **fill_blank untaught-identifier rule** (enforced by
  `ContentCorpusLadderConsistencyTest`): a `fill_blank` `solo`'s `answer`
  must appear, case-insensitively, somewhere in its own concept's
  `intro`+`guided` prompt/code/explanation text. This plan has exactly 1
  `fill_blank` solo: `loc-localedefault-solo` -> `setDefault`, literally
  present in `loc-localedefault-intro`'s code
  (`Locale.setDefault(Locale.of("fr", "FR"));`) and
  `loc-localedefault-guided`'s explanation. Verify this explicitly during
  self-review, not just by running the test after the fact.
- **mcq distractor length-balance rule** (standing since I/O y NIO.2
  sub-cycle 1, see `feedback_mcq_distractor_length_balance` memory):
  every `mcq` answer and its 3 distractors below are full,
  comparable-length sentences — the correct option must never be
  identifiable just by being the longest/most detailed one. Does not
  apply to `fill_blank` (its distractors are never rendered in the UI).
  Verify explicitly per `mcq` exercise during self-review (eyeball
  word-count parity), not just structurally.
- **No accents in Spanish content** (project-wide convention, verified
  with a full-file accented-character scan in Task 3, range `[À-ÿ]` —
  this also catches `ñ`, so no word in this plan's content uses it). No
  voseo — tuteo only.
- **worked_example format:** every `intro`-role exercise has
  `"type": "worked_example"`, `"answer": "ok"`, no `distractors` field,
  and a `code` field showing the illustrative snippet.
- **No `predict_output` in this sub-cycle.** Every exercise below is
  `worked_example`, `mcq`, or `fill_blank`; the exception scenarios
  (`IllformedLocaleException`, `MissingResourceException`) are taught as
  `mcq`, not as exact-output prediction.
- **Any example depending on the JVM default locale must fix that locale
  explicitly inside the snippet** (`Locale.setDefault(...)` before the
  relevant example), per the spec's dedicated content rule — the
  fallback-chain behavior below is only deterministic if the default
  locale is pinned; leaving it ambient would make the shown result
  depend on whatever environment renders the example.
- **Runtime-observable content must be verified against a real JDK, not
  just read.** Every factual claim below was executed against a local
  JDK 20 during design (full log in the design spec) — do not re-derive
  from documentation alone if you need to modify anything; re-run it.
  Confirmed exact behaviors used verbatim below:
  - `new Locale(String, String)` is deprecated (real compiler warning)
    since `Locale.of(String, String)` (Java 19+); both produce an
    equivalent `Locale` (`equals()` is `true`).
  - On `Locale.of("es", "CL")`: `toString()` = `"es_CL"` (underscore,
    Java's internal format), `toLanguageTag()` = `"es-CL"` (hyphen,
    BCP 47 format).
  - `Locale.Builder` with a malformed language or region (e.g.
    `setRegion("XX1")`) throws `IllformedLocaleException` **at
    `.build()` time (runtime), not at compile time**.
  - `getVariant()` on a `Locale` built without a variant returns `""`
    (empty string), never `null`.
  - `Locale.setDefault()` changes the default locale for the **entire
    JVM**, not just the calling thread or method — confirmed: a
    `setDefault()` call in one place is visible to `Locale.getDefault()`
    called from anywhere else afterward.
  - A missing key in `ResourceBundle.getString()` throws
    `MissingResourceException` at runtime.
  - **`ResourceBundle.getBundle()`'s fallback chain is: requested locale
    chain -> DEFAULT locale chain -> base bundle (no suffix) — NOT
    "requested locale chain -> base bundle" directly.** Confirmed
    concretely: with `Messages.properties` (`greeting=Hello`) and
    `Messages_es.properties` (`greeting=Hola`) present, forcing
    `Locale.setDefault(Locale.of("es"))` and requesting the bundle with
    `Locale.of("de", "DE")` (no `Messages_de*` file exists) returned
    `"Hola"` — the DEFAULT locale's chain was consulted before the base
    bundle, even though `de_DE` has zero relation to `es`. Forcing the
    default to `Locale.of("en", "US")` (no `Messages_en*` file either)
    in the same setup correctly fell through to the base bundle
    (`"Hello"`).
  - A class extending `ListResourceBundle` and implementing
    `getContents()` resolves through the exact same
    `ResourceBundle.getBundle(name)` call as a `.properties` file — no
    special-cased call needed.
  - `MessageFormat.format(pattern, args...)` with positional placeholders
    (`{0}`, `{1}`) lets the same call site produce correct output
    regardless of a pattern's per-locale word order (confirmed with a
    Spanish pattern `"{0} tiene {1} mensajes nuevos"` and an English
    pattern `"{0} has {1} new messages"`, same arguments, correct output
    for both) — unlike direct string concatenation, which bakes a fixed
    word order into the code itself.

---

### Task 1: Create the new section file with Unit A (Locale)

**Files:**
- Create: `app/src/main/assets/content/localizacion.json`

**Interfaces:**
- Consumes: nothing (brand-new file).
- Produces: a valid `ContentPack`-shaped JSON file (`sectionId`, `name`,
  `orderIndex`, `examVersion`, `units`) containing exactly one unit,
  `locale-basico`, with 3 concepts: `locale-construccion` (pathOrder 0-2,
  no `dependsOn`), `locale-codigos` (pathOrder 3-5, `dependsOn:
  ["locale-construccion"]`), `locale-default` (pathOrder 6-8, `dependsOn:
  ["locale-codigos"]`).

- [ ] **Step 1: Write the file**

Create `app/src/main/assets/content/localizacion.json` with exactly this
content:

```json
{
  "sectionId": "java-localizacion",
  "name": "Localizacion",
  "orderIndex": 8,
  "examVersion": "core",
  "units": [
    {
      "unitId": "locale-basico",
      "name": "Locale",
      "certObjective": "localizacion",
      "orderIndex": 1,
      "summary": {
        "text": "Locale.of(idioma, pais) es la forma recomendada de construir un Locale desde Java 19+; el constructor new Locale(idioma, pais) sigue compilando pero esta deprecado. Locale.Builder permite construir Locale con mas control, y lanza IllformedLocaleException en tiempo de ejecucion (al llamar build()) si algun componente esta mal formado. toString() usa guion bajo (es_CL) mientras que toLanguageTag() usa guion (es-CL, formato BCP 47). getLanguage()/getCountry()/getVariant() leen los componentes de un Locale ya construido (getVariant() devuelve un String vacio, no null, si no se especifico variante). Locale.setDefault() cambia el locale por defecto de toda la JVM, no solo del hilo o metodo donde se llama.",
        "code": "Locale es = Locale.of(\"es\", \"CL\");\nSystem.out.println(es.toString());       // es_CL\nSystem.out.println(es.toLanguageTag());  // es-CL\n\nLocale.setDefault(Locale.of(\"fr\", \"FR\"));\n// Locale.getDefault() ahora devuelve fr_FR en toda la JVM"
      },
      "exercises": [
        {
          "id": "loc-localeconstruccion-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Locale.of() es la forma recomendada de construir un Locale desde Java 19+; new Locale(...) sigue compilando pero esta deprecado",
          "code": "Locale a = Locale.of(\"es\", \"CL\");\nLocale b = new Locale(\"es\", \"CL\"); // deprecado, sigue compilando\nSystem.out.println(a.equals(b));         // true, mismo Locale\nSystem.out.println(a.toString());        // es_CL\nSystem.out.println(a.toLanguageTag());   // es-CL",
          "answer": "ok",
          "explanation": "Locale.of(idioma, pais) es la forma recomendada desde Java 19+; new Locale(idioma, pais) sigue compilando (con un warning de API deprecada) y produce un Locale equivalente. toString() usa guion bajo (formato interno de Java, es_CL); toLanguageTag() usa guion (formato estandar BCP 47, es-CL) -- son representaciones de texto distintas del mismo Locale.",
          "conceptId": "locale-construccion",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "loc-localeconstruccion-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Por que se prefiere Locale.of(idioma, pais) sobre new Locale(idioma, pais) desde Java 19?",
          "answer": "Porque new Locale(idioma, pais) esta deprecado desde Java 19, aunque sigue compilando y produce un Locale equivalente",
          "distractors": [
            "Porque new Locale(idioma, pais) ya no existe en el JDK, asi que ese codigo directamente no compilaria",
            "Porque Locale.of() es mas rapido en tiempo de ejecucion que el constructor tradicional equivalente",
            "Porque new Locale(idioma, pais) exige indicar siempre una variante ademas del idioma y el pais"
          ],
          "explanation": "El constructor new Locale(idioma, pais) fue marcado deprecado a partir de Java 19, cuando se agrego Locale.of() como reemplazo recomendado. El constructor sigue compilando (solo genera un warning) y ambas formas producen un Locale equivalente segun equals().",
          "conceptId": "locale-construccion",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "loc-localeconstruccion-solo",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Sobre Locale.of(\"es\", \"CL\"), cual es la diferencia entre toString() y toLanguageTag()?",
          "answer": "toString() usa guion bajo (es_CL, formato interno de Java); toLanguageTag() usa guion (es-CL, formato estandar BCP 47)",
          "distractors": [
            "toString() y toLanguageTag() devuelven exactamente el mismo texto, son solo dos nombres para el mismo metodo",
            "toString() incluye idioma y pais; toLanguageTag() incluye unicamente el idioma, sin el pais",
            "toLanguageTag() lanza una excepcion si el Locale no tiene una region asignada de forma explicita"
          ],
          "explanation": "Ambos metodos representan el mismo Locale como texto, pero con formatos distintos: toString() usa el formato interno de Java (guion bajo entre componentes, es_CL), mientras que toLanguageTag() usa el formato estandar BCP 47 (guion, es-CL), pensado para interoperar con otras plataformas.",
          "conceptId": "locale-construccion",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "loc-localecodigos-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "getLanguage()/getCountry() leen los componentes de un Locale ya construido; Locale.Builder lanza IllformedLocaleException en tiempo de ejecucion ante un valor mal formado",
          "code": "Locale es = Locale.of(\"es\", \"CL\");\nSystem.out.println(es.getLanguage());  // es\nSystem.out.println(es.getCountry());   // CL\nSystem.out.println(es.getVariant());   // \"\" (vacio, no null)\n\ntry {\n    new Locale.Builder().setRegion(\"XX1\").build();\n} catch (IllformedLocaleException e) {\n    System.out.println(\"invalido: \" + e.getMessage());\n}",
          "answer": "ok",
          "explanation": "getLanguage()/getCountry()/getVariant() simplemente leen los componentes ya guardados en el Locale (getVariant() devuelve un String vacio, no null, cuando no se especifico variante). Locale.Builder valida cada componente recien al llamar build(): un valor mal formado como \"XX1\" (una region de 3 caracteres invalida) lanza IllformedLocaleException en tiempo de ejecucion, no un error de compilacion.",
          "conceptId": "locale-codigos",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["locale-construccion"]
        },
        {
          "id": "loc-localecodigos-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que pasa al ejecutar new Locale.Builder().setRegion(\"XX1\").build(), sabiendo que \"XX1\" no es un codigo de region valido?",
          "answer": "Se lanza IllformedLocaleException en tiempo de ejecucion, al llamar build(), no en tiempo de compilacion",
          "distractors": [
            "El codigo no compila, ya que el compilador valida el formato de la region contra una lista fija de valores",
            "build() devuelve un Locale con region vacia, sin lanzar ninguna excepcion en ningun momento",
            "Se lanza IllegalArgumentException, ya que Builder valida sus parametros como cualquier setter comun"
          ],
          "explanation": "Locale.Builder no valida nada hasta que se llama build(): recien ahi revisa que cada componente tenga un formato valido. \"XX1\" no es un codigo de region ISO valido (3 caracteres en vez de 2, o un codigo numerico de 3 digitos), asi que build() lanza IllformedLocaleException -- un error en tiempo de ejecucion, no de compilacion.",
          "conceptId": "locale-codigos",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["locale-construccion"]
        },
        {
          "id": "loc-localecodigos-solo",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que devuelve getVariant() sobre un Locale construido solo con idioma y pais (sin variante)?",
          "answer": "Un String vacio (\"\"), ya que variant es opcional y no fue especificado al construir el Locale",
          "distractors": [
            "null, porque variant nunca se inicializa si no se especifico explicitamente al construir el Locale",
            "Lanza NoSuchElementException, ya que no hay ningun valor de variant disponible para devolver",
            "El mismo valor que getLanguage(), ya que Locale usa el idioma como variant por defecto"
          ],
          "explanation": "getVariant() nunca devuelve null: si no se especifico una variante al construir el Locale, devuelve un String vacio (\"\"). Es un detalle importante para evitar un NullPointerException innecesario al trabajar con el valor devuelto.",
          "conceptId": "locale-codigos",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["locale-construccion"]
        },
        {
          "id": "loc-localedefault-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Locale.setDefault() cambia el locale por defecto de toda la JVM, no solo del hilo o metodo donde se llama",
          "code": "Locale original = Locale.getDefault();\nLocale.setDefault(Locale.of(\"fr\", \"FR\"));\n// en cualquier parte del programa, desde ahora:\nSystem.out.println(Locale.getDefault()); // fr_FR",
          "answer": "ok",
          "explanation": "Locale.setDefault() no afecta solo al metodo o hilo donde se llama: cambia el locale por defecto de toda la JVM. Cualquier codigo que llame Locale.getDefault() despues de ese punto, sin importar en que parte del programa se ejecute, ve el nuevo valor.",
          "conceptId": "locale-default",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["locale-codigos"]
        },
        {
          "id": "loc-localedefault-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Si un metodo A llama Locale.setDefault(Locale.of(\"fr\", \"FR\")) y luego un metodo B, en otra parte del programa, llama Locale.getDefault(), que locale ve B?",
          "answer": "fr_FR -- setDefault() cambia el locale por defecto de toda la JVM, sin importar que metodo o hilo lo consulte despues",
          "distractors": [
            "El locale original, ya que el cambio hecho por A solo aplica dentro del propio metodo A",
            "El locale original, ya que el cambio hecho por A solo aplica al hilo donde se llamo setDefault()",
            "Depende de si A y B se ejecutan en el mismo hilo; si no, B sigue viendo el locale original"
          ],
          "explanation": "Locale.setDefault() cambia un valor global de la JVM, no un valor local al metodo ni al hilo que lo invoca. Por eso B, sin importar donde este ni en que hilo corra, ve fr_FR al llamar Locale.getDefault() despues de que A hizo el cambio.",
          "conceptId": "locale-default",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["locale-codigos"]
        },
        {
          "id": "loc-localedefault-solo",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa el metodo que cambia el locale por defecto de toda la JVM:",
          "code": "Locale.____(Locale.of(\"fr\", \"FR\"));",
          "answer": "setDefault",
          "distractors": ["setLocale", "changeDefault", "updateDefault"],
          "explanation": "Locale.setDefault(Locale) es el metodo que cambia el locale por defecto de toda la JVM; Locale.getDefault() lo lee.",
          "conceptId": "locale-default",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["locale-codigos"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Validate the file parses**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/localizacion.json')); print('OK')"`
Expected: `OK`, no exception.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/localizacion.json
git commit -m "content: add Localizacion section with Locale unit"
```

---

### Task 2: Add Unit B (ResourceBundle)

**Files:**
- Modify: `app/src/main/assets/content/localizacion.json` (append a
  second unit to the existing `"units"` array)

**Interfaces:**
- Consumes: the file exactly as Task 1 left it — do not touch the
  `locale-basico` unit or any of its 9 exercises.
- Produces: unit `resourcebundle-basico` (`orderIndex: 2`,
  `certObjective: "localizacion"`), 3 concepts:
  `resourcebundle-properties-basico` (pathOrder 0-2, no `dependsOn`),
  `resourcebundle-fallback` (pathOrder 3-5, `dependsOn:
  ["resourcebundle-properties-basico"]`),
  `resourcebundle-empaquetado-seguro` (pathOrder 6-8, `dependsOn:
  ["resourcebundle-fallback"]`).

- [ ] **Step 1: Insert the second unit**

In `app/src/main/assets/content/localizacion.json`, find this exact
trailing text (copy it verbatim from the file Task 1 produced, do not
retype from memory):

```json
          "conceptId": "locale-default",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["locale-codigos"]
        }
      ]
    }
  ]
}
```

Replace it with this exact text — the same closing exercise and unit
close, now followed by a comma and the new `resourcebundle-basico` unit
object, then the original closing `]` and `}`:

```json
          "conceptId": "locale-default",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["locale-codigos"]
        }
      ]
    },
    {
      "unitId": "resourcebundle-basico",
      "name": "ResourceBundle",
      "certObjective": "localizacion",
      "orderIndex": 2,
      "summary": {
        "text": "ResourceBundle.getBundle(nombre, locale) carga un archivo .properties correspondiente como PropertyResourceBundle; una clave inexistente lanza MissingResourceException. El fallback real NO es solo \"locale pedido -> bundle base\": es \"cadena del locale pedido -> cadena del locale POR DEFECTO -> bundle base\". Una clase que extiende ListResourceBundle e implementa getContents() se resuelve por el mismo getBundle(), como alternativa a un archivo .properties. MessageFormat.format() con placeholders posicionales ({0}, {1}) es mas seguro que concatenar strings directamente, porque el orden de las palabras vive en el patron (que puede variar por idioma), no en el codigo que llama a format().",
        "code": "ResourceBundle rb = ResourceBundle.getBundle(\"Messages\", locale);\nrb.getString(\"greeting\"); // PropertyResourceBundle por debajo\n\nLocale.setDefault(Locale.of(\"es\"));\nResourceBundle rb2 = ResourceBundle.getBundle(\"Messages\", Locale.of(\"de\", \"DE\"));\nrb2.getString(\"greeting\"); // \"Hola\", via el locale POR DEFECTO -- no el bundle base\n\nMessageFormat.format(\"{0} tiene {1} mensajes nuevos\", \"Luis\", 5);"
      },
      "exercises": [
        {
          "id": "loc-rbproperties-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "ResourceBundle.getBundle() carga un archivo .properties como PropertyResourceBundle; una clave inexistente lanza MissingResourceException",
          "code": "// Messages.properties: greeting=Hello\nResourceBundle rb = ResourceBundle.getBundle(\"Messages\", Locale.of(\"en\", \"US\"));\nString saludo = rb.getString(\"greeting\"); // Hello\n\ntry {\n    rb.getString(\"no_existe\");\n} catch (MissingResourceException e) {\n    System.out.println(\"falta la clave: \" + e.getKey());\n}",
          "answer": "ok",
          "explanation": "ResourceBundle.getBundle(nombre, locale) busca el archivo .properties correspondiente y lo carga como un PropertyResourceBundle por debajo. Pedir una clave que no existe en el bundle lanza MissingResourceException en tiempo de ejecucion, citando el bundle y la clave buscada.",
          "conceptId": "resourcebundle-properties-basico",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "loc-rbproperties-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que pasa al llamar rb.getString(\"clave_que_no_existe\") sobre un ResourceBundle valido que no tiene esa clave?",
          "answer": "Se lanza MissingResourceException en tiempo de ejecucion, citando el bundle y la clave que se busco",
          "distractors": [
            "Devuelve null, ya que getString() maneja una clave ausente igual que un Map comun lo haria",
            "Devuelve un String vacio (\"\") como valor por defecto para cualquier clave no encontrada",
            "Se lanza NoSuchElementException, igual que ocurriria con un Iterator ya agotado"
          ],
          "explanation": "ResourceBundle.getString() nunca devuelve null ni un valor por defecto silencioso: si la clave no existe en el bundle (ni en ninguno de sus fallbacks), lanza MissingResourceException citando el nombre del bundle y la clave buscada.",
          "conceptId": "resourcebundle-properties-basico",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "loc-rbproperties-solo",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "ResourceBundle.getBundle(\"Messages\", locale) internamente carga que tipo de bundle cuando el recurso encontrado es un archivo Messages.properties?",
          "answer": "Un PropertyResourceBundle, la implementacion de ResourceBundle pensada especificamente para archivos .properties",
          "distractors": [
            "Un ListResourceBundle, la misma clase que se usa para bundles basados en codigo Java",
            "Un objeto Properties comun, sin ninguna relacion con la jerarquia de clases de ResourceBundle",
            "Un HashMap<String,String> construido directo desde el archivo, sin pasar por ResourceBundle"
          ],
          "explanation": "PropertyResourceBundle es la subclase de ResourceBundle que sabe leer archivos .properties. ListResourceBundle es la otra subclase estandar, pero se usa para bundles definidos en codigo Java, no en archivos de texto.",
          "conceptId": "resourcebundle-properties-basico",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "loc-rbfallback-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "El fallback de ResourceBundle.getBundle() recurre primero al locale POR DEFECTO antes de caer al bundle base -- no salta directo del locale pedido al bundle base",
          "code": "// Messages.properties: greeting=Hello\n// Messages_es.properties: greeting=Hola\nLocale.setDefault(Locale.of(\"es\"));\n\nResourceBundle rb = ResourceBundle.getBundle(\"Messages\", Locale.of(\"de\", \"DE\"));\nSystem.out.println(rb.getString(\"greeting\")); // \"Hola\" -- NO \"Hello\"",
          "answer": "ok",
          "explanation": "No existe ningun archivo Messages_de ni Messages_de_DE, pero el resultado NO es el del bundle base (\"Hello\"). El fallback real es: cadena del locale pedido (de_DE, de) -> cadena del locale POR DEFECTO (es) -> bundle base. Como el locale por defecto es es y Messages_es.properties existe, ese valor (\"Hola\") se encuentra antes de llegar al bundle base, aunque de_DE no tenga ninguna relacion con es.",
          "conceptId": "resourcebundle-fallback",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["resourcebundle-properties-basico"]
        },
        {
          "id": "loc-rbfallback-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Con Locale.setDefault(Locale.of(\"es\")) activo y Messages_es.properties presente (greeting=Hola), se pide ResourceBundle.getBundle(\"Messages\", Locale.of(\"de\", \"DE\")) sin que exista ningun archivo Messages_de*. Que devuelve getString(\"greeting\")?",
          "answer": "\"Hola\" -- el fallback recurre primero al locale por defecto (es) antes de caer al bundle base, aunque de_DE no tenga relacion con es",
          "distractors": [
            "\"Hello\" -- el fallback de ResourceBundle salta directo del locale pedido al bundle base, sin pasar por el locale por defecto",
            "Se lanza MissingResourceException, ya que no existe ningun archivo Messages_de ni Messages_de_DE registrado en ningun lugar del proyecto",
            "Depende del sistema operativo, ya que el fallback usa el locale configurado en el dispositivo, no el de la JVM"
          ],
          "explanation": "El algoritmo de fallback de ResourceBundle agota primero la cadena del locale pedido (de_DE, luego de), y si no encuentra nada, agota tambien la cadena del locale POR DEFECTO (es) antes de llegar al bundle base. Como Messages_es.properties existe y tiene la clave, ese valor gana.",
          "conceptId": "resourcebundle-fallback",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["resourcebundle-properties-basico"]
        },
        {
          "id": "loc-rbfallback-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "En el mismo escenario anterior, si en cambio se fuerza Locale.setDefault(Locale.of(\"en\", \"US\")) (sin ningun archivo Messages_en*), que devuelve ahora getString(\"greeting\") al pedir el bundle con Locale.of(\"de\", \"DE\")?",
          "answer": "\"Hello\" -- al agotar tambien la cadena del locale por defecto sin encontrar coincidencia, recien ahi se cae al bundle base",
          "distractors": [
            "\"Hola\" -- el resultado no cambia, porque Messages_es.properties sigue existiendo en el proyecto de todas formas",
            "Se lanza MissingResourceException, ya que ni el locale pedido ni el locale por defecto tienen un archivo especifico",
            "Un String vacio (\"\") -- ResourceBundle devuelve vacio cuando ni el locale pedido ni el por defecto coinciden"
          ],
          "explanation": "Con el locale por defecto en en_US (sin archivo Messages_en ni Messages_en_US), tanto la cadena del locale pedido como la del locale por defecto se agotan sin encontrar la clave. Recien en ese punto el algoritmo cae al bundle base (Messages.properties), devolviendo \"Hello\".",
          "conceptId": "resourcebundle-fallback",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["resourcebundle-properties-basico"]
        },
        {
          "id": "loc-rbseguro-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "ListResourceBundle ofrece bundles basados en clase como alternativa a .properties; MessageFormat con placeholders es mas seguro que concatenar strings entre idiomas",
          "code": "class ClassMessages extends ListResourceBundle {\n    protected Object[][] getContents() {\n        return new Object[][] { {\"greeting\", \"Hi from class\"} };\n    }\n}\nResourceBundle rb = ResourceBundle.getBundle(\"ClassMessages\");\nrb.getString(\"greeting\"); // \"Hi from class\"\n\nString patronEs = \"{0} tiene {1} mensajes nuevos\";\nString patronEn = \"{0} has {1} new messages\";\nMessageFormat.format(patronEs, \"Luis\", 5); // \"Luis tiene 5 mensajes nuevos\"\nMessageFormat.format(patronEn, \"Luis\", 5); // \"Luis has 5 new messages\"",
          "answer": "ok",
          "explanation": "Una clase que extiende ListResourceBundle e implementa getContents() se resuelve por el mismo ResourceBundle.getBundle(), sin llamadas especiales. MessageFormat.format() con placeholders posicionales ({0}, {1}) deja que el orden de las palabras viva en el patron (tipicamente cargado desde un ResourceBundle por locale), en vez de fijarlo en el codigo -- por eso el mismo llamado a format() funciona igual de bien con un patron en espanol o en ingles.",
          "conceptId": "resourcebundle-empaquetado-seguro",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["resourcebundle-fallback"]
        },
        {
          "id": "loc-rbseguro-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Una clase extiende ListResourceBundle e implementa getContents() devolviendo pares clave-valor. Como se resuelve esta clase al llamar ResourceBundle.getBundle(nombre)?",
          "answer": "Igual que un archivo .properties -- ResourceBundle.getBundle() la resuelve como cualquier otro bundle, sin necesidad de codigo adicional",
          "distractors": [
            "Nunca se resuelve automaticamente -- hay que instanciarla manualmente con new y pasarla a otro metodo distinto",
            "Solo se resuelve si el nombre de la clase termina con el sufijo Properties, no con Messages ni otro nombre",
            "Se resuelve, pero unicamente si tambien existe un archivo .properties con el mismo nombre base de respaldo"
          ],
          "explanation": "ListResourceBundle es una de las dos subclases estandar de ResourceBundle (junto a PropertyResourceBundle). ResourceBundle.getBundle(nombre) la busca y resuelve exactamente igual que buscaria un archivo .properties, sin ningun paso adicional ni convencion de nombre especial.",
          "conceptId": "resourcebundle-empaquetado-seguro",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["resourcebundle-fallback"]
        },
        {
          "id": "loc-rbseguro-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que usar MessageFormat.format(\"{0} tiene {1} mensajes nuevos\", nombre, cantidad) es mas seguro para varios idiomas que concatenar directamente nombre + \" tiene \" + cantidad + \" mensajes nuevos\"?",
          "answer": "Porque el patron controla el orden de las palabras y cambia por idioma, no el codigo que llama a format()",
          "distractors": [
            "Porque MessageFormat.format() es mas rapido en tiempo de ejecucion que la concatenacion de strings equivalente",
            "Porque la concatenacion de strings no permite insertar numeros dentro de un mensaje, solo texto plano",
            "Porque MessageFormat.format() traduce automaticamente el contenido al idioma del Locale por defecto, sin intervencion del codigo que lo invoca"
          ],
          "explanation": "El codigo que llama a MessageFormat.format() con placeholders posicionales no cambia entre idiomas: lo que cambia es el patron (tipicamente cargado desde un ResourceBundle por locale), que puede reordenar {0} y {1} segun el idioma. Concatenar strings directamente fija el orden de las palabras en el propio codigo Java, sin forma de adaptarlo por locale sin tocar codigo.",
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

- [ ] **Step 2: Validate the file parses**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/localizacion.json')); print('OK')"`
Expected: `OK`, no exception.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/localizacion.json
git commit -m "content: add ResourceBundle unit to Localizacion"
```

---

### Task 3: Register the section, validate the whole file, bump content version

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Read-only validation: `app/src/main/assets/content/localizacion.json`

**Interfaces:**
- Consumes: the final state of `localizacion.json` after Task 2 (2
  units, 18 exercises total).
- Produces: the new section registered and loadable;
  `CURRENT_CONTENT_VERSION` bumped by one from whatever it is at
  dispatch time (check `ContentSeeder.kt`'s current value first — do not
  assume a specific number; it was `"23"` at plan-writing time, but
  other cycles may have landed between this plan being written and
  executed).

- [ ] **Step 1: Register the new content pack**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt`,
change:

```kotlin
object ContentPackRegistry {
    val assetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json",
        "content/exception-handling.json",
        "content/concurrency.json",
        "content/modules-packaging.json",
        "content/io-nio2.json"
    )
}
```

to:

```kotlin
object ContentPackRegistry {
    val assetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json",
        "content/exception-handling.json",
        "content/concurrency.json",
        "content/modules-packaging.json",
        "content/io-nio2.json",
        "content/localizacion.json"
    )
}
```

This is the only change in this file — nothing else in
`ContentPackRegistry.kt` is touched.

- [ ] **Step 2: Write and run a full validation script**

```bash
python3 << 'EOF'
import json, re

path = "app/src/main/assets/content/localizacion.json"
data = json.load(open(path))

assert data["sectionId"] == "java-localizacion"
assert data["orderIndex"] == 8
assert data["examVersion"] == "core"

all_units = {u["unitId"]: u for u in data["units"]}
assert set(all_units.keys()) == {"locale-basico", "resourcebundle-basico"}, f"unexpected units: {list(all_units.keys())}"

expected_counts = {"locale-basico": 9, "resourcebundle-basico": 9}
expected_order = {"locale-basico": 1, "resourcebundle-basico": 2}
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

# fill_blank untaught-identifier rule (same check as ContentCorpusLadderConsistencyTest).
for uid, exercises in all_exercises:
    by_concept = {}
    for e in exercises:
        if e.get("conceptId"):
            by_concept.setdefault(e["conceptId"], []).append(e)
    for cid, exs in by_concept.items():
        taught = " ".join(f"{e['prompt']} {e.get('code','')} {e['explanation']}" for e in exs if e.get("role") in ("intro", "guided")).lower()
        for e in exs:
            if e["type"] == "fill_blank" and e.get("role") in ("solo", "practice"):
                token = e["answer"].strip().rstrip("()").lower()
                assert token in taught, f"{uid}/{e['id']}: answer '{e['answer']}' never taught in its own concept's intro/guided"
print("fill_blank untaught-identifier rule passed.")

# No accented characters (whole-file check) and no predict_output anywhere in this file.
raw = open(path, encoding="utf-8").read()
accented = re.findall(r"[À-ÿ]", raw)
assert not accented, f"found accented characters: {accented}"
predict_output_ids = [e["id"] for uid, exercises in all_exercises for e in exercises if e.get("type") == "predict_output"]
assert not predict_output_ids, f"predict_output not allowed in this sub-cycle: {predict_output_ids}"
print("No accented characters, no predict_output.")

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
print(f"Total exercises across both units: {total}")
assert total == 18
EOF
```

Expected: `Unit counts, certObjective, and orderIndex all correct.` then
`One-terminal-role, dependsOn, sequential pathOrder, and difficulty-
monotonicity rules all passed.` then `fill_blank untaught-identifier rule
passed.` then `No accented characters, no predict_output.` then two
reachability lines (one per unit), then
`Total exercises across both units: 18`, no assertion errors.

- [ ] **Step 3: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find the current `CURRENT_CONTENT_VERSION` value and increment it by one
(check the file first — do not assume a specific starting number).

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest --rerun-tasks`
Expected: BUILD SUCCESSFUL, all existing tests still passing, including
`ContentCorpusLadderConsistencyTest` picking up this new file
automatically via `ContentPackRegistry.assetPaths` (no test file changes
needed for this task). Use `--rerun-tasks` — content-only JSON edits are
not tracked as a Gradle test input, so a bare run can silently report a
stale `UP-TO-DATE` result.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: register Localizacion section and bump content version"
```

---

## After the plan: manual on-device QA

Install a clean/in-place build (in-place is fine — the
`CURRENT_CONTENT_VERSION` bump triggers an automatic re-seed) and verify
on-device. `adb input tap` automation is not reliable for answering
exercises (only for navigation/screenshots) — the controller navigates
screens/worked_example CONTINUAR taps, the human answers every graded
exercise. Play both units end to end (18 exercises total), confirming:
`worked_example` intros render before their guided/solo steps; the
`dependsOn` chain unlocks concepts one at a time within each unit;
`resourcebundle-basico` stays locked until `locale-basico` is fully
complete (`orderIndex`-driven unit unlock, no `dependsOn` field
involved); the one `fill_blank` solo (`setDefault`) grades correctly
when typed; the new section appears correctly positioned in Ruta right
after I/O y NIO.2.
