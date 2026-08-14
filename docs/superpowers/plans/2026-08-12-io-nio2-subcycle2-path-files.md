# I/O y NIO.2 - Sub-cycle 2 (Path + Files) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close out the "I/O y NIO.2" section by adding its second half —
`java.nio.file` (`Path` construction/traversal/comparison/resolve/
relativize, plus `Files` existence/attributes/copy/move/delete/
`StandardCopyOption`) — to the existing `io-nio2.json` content file.
Design rationale and JDK verification log:
`docs/superpowers/specs/2026-08-12-io-nio2-subcycle2-path-files-design.md`.

**Architecture:** Pure content authoring, no Kotlin changes beyond a
one-line version bump. `app/src/main/assets/content/io-nio2.json`
already exists (created by sub-cycle 1) and is already registered in
`ContentPackRegistry.assetPaths` — this plan only appends two more units
to its existing `units` array and bumps `CURRENT_CONTENT_VERSION`. Unit
unlocking within a section is driven purely by `orderIndex` sequencing
(`GetLearningPathUseCase.kt:30`, `previousUnitComplete`) — there is no
unit-level `dependsOn` field in the schema, only the per-exercise
`conceptId`-level one already used within each unit.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell, JUnit4 —
must stay green including `ContentCorpusLadderConsistencyTest`, which
automatically re-scans `io-nio2.json` (already in
`ContentPackRegistry.assetPaths`) once this plan's exercises land in it.

## Global Constraints

- **File, not registry:** `app/src/main/assets/content/io-nio2.json`
  already exists with 2 units (`io-streams-clasicos`, `io-serializacion`)
  and is already listed in `ContentPackRegistry.assetPaths` — do **not**
  touch `ContentPackRegistry.kt` in this plan, and do not create a new
  file. Task 1 and Task 2 both use Edit to append a unit to the existing
  `units` array.
- **Unit identity:**
  - `nio2-path-basico` / "Path basico", `orderIndex: 3`. 3 concepts, 9
    exercises, `pathOrder` 0-8 (restarts at 0 — `pathOrder` is per-unit).
  - `nio2-files-operaciones` / "Files y operaciones", `orderIndex: 4`. 3
    concepts, 9 exercises, `pathOrder` 0-8.
  - Both use `"certObjective": "io-nio2"`, matching the file's first two
    units.
- **dependsOn chain, both units:** each unit's 3 concepts form a single
  linear chain (concept 2 `dependsOn` concept 1; concept 3 `dependsOn`
  concept 2), same convention as the file's first two units. `dependsOn`
  only ever references a `conceptId` in the *same* unit — there is no
  cross-unit or unit-level `dependsOn` field; unit-to-unit sequencing
  (`nio2-files-operaciones` playable only after `nio2-path-basico` is
  complete) comes for free from `orderIndex` 3 vs 4 within the section.
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
  `difficulty: 1` for `path-construccion-traversal` and
  `files-existencia-atributos`, `2` for `path-comparacion-normalizacion`,
  `path-resolve-relativize`, and `files-copy-move`, `3` for
  `files-standardcopyoption-delete` (combines two behaviors —
  `REPLACE_EXISTING` and the `delete()`/`deleteIfExists()` contrast).
- **fill_blank untaught-identifier rule** (enforced by
  `ContentCorpusLadderConsistencyTest`): a `fill_blank` `solo`'s `answer`
  must appear, case-insensitively, somewhere in its own concept's
  `intro`+`guided` prompt/code/explanation text. This plan has 2
  `fill_blank` solos: `nio2-pathbasics-solo` -> `getParent` (present in
  `nio2-pathbasics-intro`'s code) and `nio2-filesoption-solo` ->
  `deleteIfExists` (present in `nio2-filesoption-intro`'s code). Verify
  both explicitly during self-review.
- **mcq distractor length-balance rule** (standing since sub-cycle 1, see
  `feedback_mcq_distractor_length_balance` memory): every `mcq` answer
  and its 3 distractors are full, comparable-length sentences — the
  correct option must never be identifiable just by being the
  longest/most detailed one. This rule does **not** apply to `fill_blank`
  (its distractors are never rendered in the UI). Verify explicitly per
  `mcq` exercise during self-review (eyeball word-count parity), not just
  structurally.
- **No accents in Spanish content** (project-wide convention). Verify
  with a full-file accented-character scan in Task 3, range `[À-ÿ]` —
  this also catches `ñ`; no word in this plan's content uses it (e.g.
  "anio" not "año" is simply avoided by not needing that word at all).
  No voseo — tuteo only.
- **worked_example format:** every `intro`-role exercise has
  `"type": "worked_example"`, `"answer": "ok"`, no `distractors` field,
  and a `code` field showing the illustrative snippet.
- **No `predict_output` in this sub-cycle.** Every exercise below is
  `worked_example` or `mcq`, except the 2 `fill_blank` solos noted above.
- **Runtime-observable content must be verified against a real JDK, not
  just read.** Every factual claim below was executed against a local
  JDK 20 during design (full log in the design spec) — do not re-derive
  from documentation alone if you need to modify anything; re-run it.
  Confirmed exact behaviors used verbatim below:
  - `Path.of()`/`Paths.get()` never touch the filesystem — no exception
    for a path that does not exist on disk. `Path.of("/x/y").equals(
    Paths.get("/x/y"))` is `true`; the two factory methods are
    interchangeable.
  - On `Path.of("/datos/reportes/2026/informe.txt")`: `getFileName()` =
    `informe.txt`, `getParent()` = `/datos/reportes/2026`, `getRoot()` =
    `/`, `getNameCount()` = `4`, `getName(0)` = `datos`.
  - `Path` `equals()`/`compareTo()` are purely syntactic (string-based),
    with **no implicit normalization**: `Path.of("/a/./b").equals(
    Path.of("/a/b"))` is `false`, but `Path.of("/a/./b").normalize(
    ).equals(Path.of("/a/b"))` is `true`.
  - `normalize()` collapses redundant `.`/`..` elements without touching
    the filesystem: `Path.of("/a/b/../c/./d").normalize()` = `/a/c/d`.
  - `resolve()` with an absolute argument discards the base entirely and
    returns the argument as-is: `Path.of("/datos/reportes").resolve(
    Path.of("/x/y"))` = `/x/y` (not a concatenation). With a *relative*
    argument it concatenates normally:
    `Path.of("/datos/reportes").resolve(Path.of("2026/informe.txt"))` =
    `/datos/reportes/2026/informe.txt`.
  - `relativize()` requires both paths to be the same "type" (both
    absolute or both relative) or throws `IllegalArgumentException`
    (confirmed message: `"'other' is different type of Path"`).
    `Path.of("/datos/reportes").relativize(Path.of(
    "/datos/reportes/2026/informe.txt"))` = `2026/informe.txt` (valid
    case, both absolute).
  - `Files.exists()`/`Files.notExists()` never throw — both return a
    plain `boolean`, confirmed on a nonexistent path (`exists` = `false`,
    `notExists` = `true`).
  - `Files.copy(origen, destino)` and `Files.move(origen, destino)` both
    throw `FileAlreadyExistsException` if `destino` already exists and no
    `CopyOption` is given — confirmed for both methods independently.
  - `Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING)`
    succeeds and overwrites `destino`'s content — confirmed, resolves the
    previous bullet's failure case.
  - `Files.delete()` on a nonexistent path throws `NoSuchFileException`;
    `Files.deleteIfExists()` on the same nonexistent path returns `false`
    without throwing — confirmed for both, same path.
- **Process rules from the design spec's "Extra notes" section** (Luis
  added these directly, apply to this and every future plan unless he
  says otherwise):
  - If this plan runs inside a worktree (via
    subagent-driven-development), watch for stray commits landing on
    `main` instead — by a subagent or by the controller itself — per
    `feedback_subagent_worktree_commit_care` memory. Verify the branch
    before every commit in Tasks 1-3.
  - Use TDD wherever there is an actual assertion to write. This plan is
    pure content authoring (JSON) plus a one-line version bump, so there
    is no application code to unit-test — the Python validation script
    in Task 3 Step 1 **is** the test-first check for the content itself:
    it must be written and run (and must fail before Tasks 1-2 exist,
    pass after) rather than treated as an afterthought.
  - Apply clean code / SOLID / DRY / design patterns where it actually
    makes sense — for this plan, that means: no duplicated validation
    logic between Task 3's script and `ContentCorpusLadderConsistencyTest`
    (the script checks the same rules as a pre-flight sanity check, not a
    replacement for the real Kotlin test), and no restructuring of
    `ContentSeeder.kt`/`ContentPackRegistry.kt` beyond the single version
    bump this task needs.
  - If any inherited bad practice is noticed while touching these files
    (e.g. in `ContentSeeder.kt`), do not silently repeat it or fix it out
    of scope — note it as technical debt in the task's completion report
    instead.

---

### Task 1: Add Unit 3 (Path basico) to the existing content file

**Files:**
- Modify: `app/src/main/assets/content/io-nio2.json` (append a third unit
  to the existing `"units"` array — do not touch the two existing units)

**Interfaces:**
- Consumes: the file exactly as sub-cycle 1 left it (2 units, 18
  exercises) — do not modify `io-streams-clasicos` or `io-serializacion`.
- Produces: unit `nio2-path-basico` (`orderIndex: 3`, `certObjective:
  "io-nio2"`), 3 concepts: `path-construccion-traversal` (pathOrder 0-2,
  no `dependsOn`), `path-comparacion-normalizacion` (pathOrder 3-5,
  `dependsOn: ["path-construccion-traversal"]`),
  `path-resolve-relativize` (pathOrder 6-8, `dependsOn:
  ["path-comparacion-normalizacion"]`).

- [ ] **Step 1: Insert the third unit**

In `app/src/main/assets/content/io-nio2.json`, find this exact trailing
text (copy it verbatim from the current file, do not retype from
memory):

```json
          "conceptId": "serialversionuid-transient",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["object-streams"]
        }
      ]
    }
  ]
}
```

Replace it with this exact text — the same closing exercise and unit
close, now followed by a comma and the new `nio2-path-basico` unit
object, then the original closing `]` and `}`:

```json
          "conceptId": "serialversionuid-transient",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["object-streams"]
        }
      ]
    },
    {
      "unitId": "nio2-path-basico",
      "name": "Path basico",
      "certObjective": "io-nio2",
      "orderIndex": 3,
      "summary": {
        "text": "Path.of() y Paths.get() son equivalentes y nunca tocan el filesystem al construir un Path: es solo una representacion sintactica de una ruta. Sobre un Path ya construido se puede navegar su estructura con getFileName(), getParent(), getRoot(), getNameCount() y getName(indice); un Path relativo tiene getRoot() nulo. equals()/compareTo() comparan el string de la ruta sin normalizar implicitamente, y normalize() colapsa elementos '.'/'..' redundantes sin verificar que existan. resolve() combina una ruta base con otra, pero si el argumento es absoluto ese argumento gana por completo, ignorando la base; relativize() calcula la ruta relativa entre dos Path, pero exige que ambos sean del mismo tipo (los dos absolutos o los dos relativos), o lanza IllegalArgumentException.",
        "code": "Path p = Path.of(\"/datos/reportes/2026/informe.txt\");\np.getFileName();   // informe.txt\np.getParent();      // /datos/reportes/2026\np.getRoot();        // /\n\nPath base = Path.of(\"/datos/reportes\");\nbase.resolve(Path.of(\"/x/y\"));            // /x/y (ignora la base)\nbase.relativize(Path.of(\"/datos/reportes/2026/informe.txt\")); // 2026/informe.txt"
      },
      "exercises": [
        {
          "id": "nio2-pathbasics-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Path.of() nunca toca el filesystem al construir un Path; sobre un Path ya construido se puede navegar su estructura",
          "code": "Path p = Path.of(\"/datos/reportes/2026/informe.txt\");\nSystem.out.println(p.getFileName());  // informe.txt\nSystem.out.println(p.getParent());     // /datos/reportes/2026\nSystem.out.println(p.getRoot());       // /\nSystem.out.println(p.getNameCount());  // 4\nSystem.out.println(p.getName(0));      // datos",
          "answer": "ok",
          "explanation": "Path.of() (equivalente a Paths.get()) construye un Path sin verificar en ningun momento que la ruta exista de verdad en el disco -- es pura representacion sintactica. getFileName()/getParent()/getRoot() navegan la estructura de la ruta ya construida; getNameCount() y getName(indice) cuentan y acceden a los elementos del nombre (el root no cuenta como uno de ellos, por eso getName(0) es 'datos' y no la raiz).",
          "conceptId": "path-construccion-traversal",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "nio2-pathbasics-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que pasa al ejecutar Path.of(\"/no/existe/en/verdad.txt\") si esa ruta no existe en el filesystem?",
          "answer": "No pasa nada especial: Path.of() solo construye una representacion sintactica de la ruta, sin verificar en ningun momento que exista",
          "distractors": [
            "Se lanza NoSuchFileException, ya que Path.of() siempre verifica la existencia real del archivo antes de construir el objeto",
            "Se lanza IOException, porque toda operacion de la API NIO.2 requiere acceso valido y previo al filesystem",
            "El metodo devuelve null, ya que no es posible construir un Path para algo que todavia no existe"
          ],
          "explanation": "Path.of()/Paths.get() son operaciones puramente sintacticas: parsean el string de la ruta y arman el objeto Path, sin ninguna llamada al filesystem. La verificacion real de existencia queda a cargo de otra API (Files.exists()), que se ve en la unidad siguiente.",
          "conceptId": "path-construccion-traversal",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "nio2-pathbasics-solo",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa el metodo que devuelve el path padre (todos los elementos de la ruta menos el ultimo):",
          "code": "Path p = Path.of(\"/datos/reportes/informe.txt\");\nPath padre = p._____();",
          "answer": "getParent",
          "distractors": ["getRoot", "getFileName", "subpath"],
          "explanation": "getParent() devuelve un nuevo Path con todos los elementos de la ruta original excepto el ultimo -- en este ejemplo, /datos/reportes. getRoot() devuelve solo la raiz (/), getFileName() devuelve solo el ultimo elemento (informe.txt).",
          "conceptId": "path-construccion-traversal",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "nio2-pathcompare-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "equals() compara el string de la ruta sin normalizar implicitamente; normalize() colapsa elementos '.'/'..' redundantes sin tocar el filesystem",
          "code": "Path a = Path.of(\"/a/./b\");\nPath b = Path.of(\"/a/b\");\nSystem.out.println(a.equals(b));              // false\nSystem.out.println(a.normalize().equals(b));  // true\n\nPath messy = Path.of(\"/a/b/../c/./d\");\nSystem.out.println(messy.normalize());        // /a/c/d",
          "answer": "ok",
          "explanation": "equals() (y compareTo()) comparan la representacion textual de la ruta elemento por elemento, sin aplicar ninguna normalizacion automatica: /a/./b y /a/b son literalmente distintos hasta que se llama normalize() de forma explicita. normalize() colapsa '.'/'..' redundantes usando solo el string de la ruta, sin verificar que los directorios intermedios existan realmente.",
          "conceptId": "path-comparacion-normalizacion",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["path-construccion-traversal"]
        },
        {
          "id": "nio2-pathcompare-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que hace exactamente Path.normalize()?",
          "answer": "Colapsa elementos redundantes '.'/'..' de la ruta usando solo el string, sin verificar en ningun momento que los directorios intermedios existan",
          "distractors": [
            "Resuelve symlinks y verifica contra el filesystem real que cada elemento intermedio de la ruta exista",
            "Ordena alfabeticamente los elementos de la ruta para dejarla en un formato canonico y comparable",
            "Lanza una excepcion si la ruta contiene elementos '.' o '..' que no pueden resolverse de forma segura"
          ],
          "explanation": "normalize() es una operacion puramente sintactica sobre el string de la ruta: elimina elementos '.' (referencia al directorio actual) y resuelve '..' (sube un nivel) contra los elementos anteriores del mismo Path, sin ninguna consulta al filesystem real ni resolucion de symlinks.",
          "conceptId": "path-comparacion-normalizacion",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["path-construccion-traversal"]
        },
        {
          "id": "nio2-pathcompare-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Path.of(\"/a/./b\").equals(Path.of(\"/a/b\")), sin llamar normalize() antes de comparar, que resultado da?",
          "answer": "false, porque equals() compara los elementos literales del string de cada ruta, sin normalizar implicitamente antes de comparar",
          "distractors": [
            "true, porque ambos Path representan logicamente el mismo directorio final una vez resuelto el elemento '.'",
            "Se lanza IllegalArgumentException, porque las dos rutas tienen formatos distintos y no pueden compararse directamente",
            "El resultado depende del sistema operativo donde corre la JVM, ya que cada uno normaliza las rutas de forma distinta"
          ],
          "explanation": "equals() nunca normaliza de forma implicita: compara el string de la ruta elemento por elemento tal cual fue construido. /a/./b y /a/b tienen distinta cantidad de elementos literales ('.', 'a', 'b' vs 'a', 'b'), asi que equals() da false hasta que se llama normalize() explicitamente sobre alguno de los dos.",
          "conceptId": "path-comparacion-normalizacion",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["path-construccion-traversal"]
        },
        {
          "id": "nio2-pathresolve-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "resolve() con un argumento absoluto ignora la base y devuelve el argumento tal cual; relativize() exige que ambos Path sean del mismo tipo",
          "code": "Path base = Path.of(\"/datos/reportes\");\nbase.resolve(Path.of(\"2026/informe.txt\"));  // /datos/reportes/2026/informe.txt (relativo: concatena)\nbase.resolve(Path.of(\"/x/y\"));               // /x/y (absoluto: ignora la base por completo)\n\nPath target = Path.of(\"/datos/reportes/2026/informe.txt\");\nbase.relativize(target);  // 2026/informe.txt (ambos absolutos: funciona)",
          "answer": "ok",
          "explanation": "resolve() concatena normalmente cuando el argumento es relativo, pero si el argumento ya es absoluto, ese argumento gana por completo y la base se descarta -- resolve() nunca intenta 'anclar' un Path absoluto a otra base. relativize() calcula la ruta relativa entre dos Path, pero solo si ambos son del mismo tipo (los dos absolutos o los dos relativos); mezclar tipos lanza IllegalArgumentException, como se ve en el siguiente ejercicio.",
          "conceptId": "path-resolve-relativize",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["path-comparacion-normalizacion"]
        },
        {
          "id": "nio2-pathresolve-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Si base = Path.of(\"/datos/reportes\") y other = Path.of(\"/x/y\") (un Path absoluto), que devuelve base.resolve(other)?",
          "answer": "/x/y -- resolve() devuelve el argumento tal cual cuando es absoluto, ignorando la base por completo",
          "distractors": [
            "/datos/reportes/x/y -- resolve() concatena siempre la base con el argumento, sea este absoluto o relativo",
            "Se lanza IllegalArgumentException, porque no se puede pasar un Path absoluto como argumento de resolve()",
            "/datos/reportes -- resolve() ignora el argumento por completo cuando este ya es un Path absoluto"
          ],
          "explanation": "Cuando el argumento de resolve() es un Path absoluto, ese argumento gana por completo: resolve() lo devuelve tal cual, sin ninguna combinacion con la base. Solo cuando el argumento es relativo, resolve() lo concatena al final de la base.",
          "conceptId": "path-resolve-relativize",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["path-comparacion-normalizacion"]
        },
        {
          "id": "nio2-pathresolve-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Path abs = Path.of(\"/a/b\"); Path rel = Path.of(\"c/d\"); abs.relativize(rel); que ocurre al ejecutar esta ultima linea?",
          "answer": "Se lanza IllegalArgumentException, porque relativize() exige que ambos Path sean del mismo tipo (los dos absolutos o los dos relativos)",
          "distractors": [
            "Devuelve un Path mixto que combina ambas rutas, dejando que el codigo que lo use decida como interpretarlo",
            "Convierte 'rel' a absoluto de forma automatica antes de calcular la ruta relativa entre ambos",
            "Devuelve un Path vacio, ya que no existe ninguna relacion posible entre una ruta absoluta y una relativa"
          ],
          "explanation": "relativize() calcula la ruta que hay que recorrer desde un Path hasta otro, pero solo sabe hacerlo si ambos son del mismo 'tipo': los dos absolutos o los dos relativos. Mezclar un Path absoluto con uno relativo lanza IllegalArgumentException (mensaje real del JDK: \"'other' is different type of Path\") en vez de intentar adivinar una conversion.",
          "conceptId": "path-resolve-relativize",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["path-comparacion-normalizacion"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Validate the file parses**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/io-nio2.json')); print('OK')"`
Expected: `OK`, no exception.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/io-nio2.json
git commit -m "content: add Path basico unit to I/O y NIO.2"
```

---

### Task 2: Add Unit 4 (Files y operaciones)

**Files:**
- Modify: `app/src/main/assets/content/io-nio2.json` (append a fourth
  unit to the existing `"units"` array)

**Interfaces:**
- Consumes: the file exactly as Task 1 left it (3 units, 27 exercises) —
  do not touch any of the first three units.
- Produces: unit `nio2-files-operaciones` (`orderIndex: 4`,
  `certObjective: "io-nio2"`), 3 concepts: `files-existencia-atributos`
  (pathOrder 0-2, no `dependsOn`), `files-copy-move` (pathOrder 3-5,
  `dependsOn: ["files-existencia-atributos"]`),
  `files-standardcopyoption-delete` (pathOrder 6-8, `dependsOn:
  ["files-copy-move"]`).

- [ ] **Step 1: Insert the fourth unit**

In `app/src/main/assets/content/io-nio2.json`, find this exact trailing
text (copy it verbatim from the file Task 1 produced, do not retype from
memory):

```json
          "conceptId": "path-resolve-relativize",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["path-comparacion-normalizacion"]
        }
      ]
    }
  ]
}
```

Replace it with this exact text — the same closing exercise and unit
close, now followed by a comma and the new `nio2-files-operaciones` unit
object, then the original closing `]` and `}`:

```json
          "conceptId": "path-resolve-relativize",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["path-comparacion-normalizacion"]
        }
      ]
    },
    {
      "unitId": "nio2-files-operaciones",
      "name": "Files y operaciones",
      "certObjective": "io-nio2",
      "orderIndex": 4,
      "summary": {
        "text": "Files.exists()/notExists() nunca lanzan excepcion, solo devuelven boolean; Files.isDirectory()/isRegularFile()/size() consultan atributos basicos del archivo. Files.copy(origen, destino) y Files.move(origen, destino) lanzan FileAlreadyExistsException si el destino ya existe y no se especifica ninguna opcion. StandardCopyOption.REPLACE_EXISTING habilita sobrescribir sin error. Files.delete() lanza NoSuchFileException si el archivo no existe, mientras que Files.deleteIfExists() en el mismo caso devuelve false sin lanzar nada.",
        "code": "Files.exists(p);      // false si no existe, nunca lanza\nFiles.copy(origen, destino);                                     // FileAlreadyExistsException si destino ya existe\nFiles.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING); // sobrescribe sin error\nFiles.delete(p);           // NoSuchFileException si no existe\nFiles.deleteIfExists(p);  // false si no existe, no lanza nada"
      },
      "exercises": [
        {
          "id": "nio2-filesattrs-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Files.exists()/notExists() nunca lanzan excepcion, solo devuelven boolean; Files.isDirectory()/isRegularFile()/size() consultan atributos basicos",
          "code": "Path p = Path.of(\"/tmp/reporte.txt\");\nSystem.out.println(Files.exists(p));       // false si no existe\nSystem.out.println(Files.notExists(p));    // true si no existe\nif (Files.exists(p)) {\n    System.out.println(Files.isRegularFile(p));\n    System.out.println(Files.size(p));  // long, en bytes\n}",
          "answer": "ok",
          "explanation": "Files.exists()/notExists() son simples consultas: devuelven boolean sin lanzar ninguna excepcion, incluso si la ruta no existe. isDirectory()/isRegularFile() distinguen el tipo de entrada del filesystem, y size() devuelve el tamano en bytes como long -- todas estas consultas asumen que el Path pasado ya fue construido (Path.of() nunca verifico nada).",
          "conceptId": "files-existencia-atributos",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "nio2-filesattrs-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que pasa al llamar Files.exists(p) sobre una ruta que no existe en el filesystem?",
          "answer": "Devuelve false -- Files.exists() nunca lanza ninguna excepcion, solo consulta y devuelve un boolean",
          "distractors": [
            "Se lanza NoSuchFileException, ya que exists() verifica la ruta y falla si no la encuentra",
            "Se lanza una IOException marcada, que obliga a envolver la llamada en un try-catch",
            "Devuelve null, ya que no es posible determinar con certeza si el archivo existe o no"
          ],
          "explanation": "Files.exists() (y su contraparte Files.notExists()) estan pensados como consultas simples: siempre devuelven un boolean, sin excepciones de por medio, incluso si la ruta no existe o no es accesible.",
          "conceptId": "files-existencia-atributos",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "nio2-filesattrs-solo",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Si Files.exists(p) devuelve true en este instante, que garantiza esto sobre una operacion posterior como Files.copy(p, destino)?",
          "answer": "Nada -- exists() solo refleja el estado del archivo en el momento exacto en que se llamo, no en operaciones futuras",
          "distractors": [
            "Garantiza que la operacion posterior sobre esa misma ruta va a funcionar sin lanzar ninguna excepcion",
            "Garantiza que el archivo queda bloqueado para otros procesos hasta que termine la siguiente operacion",
            "Garantiza que Files.size(p) sobre la misma ruta va a devolver un valor mayor a cero bytes"
          ],
          "explanation": "exists() es una foto instantanea: el archivo podria borrarse, moverse o modificarse entre esa llamada y la siguiente operacion, asi que ninguna operacion posterior queda garantizada por un exists() previo -- por eso Files.copy()/move()/delete() siguen pudiendo lanzar sus propias excepciones aunque exists() haya dado true poco antes.",
          "conceptId": "files-existencia-atributos",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "nio2-filescopy-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Files.copy(origen, destino) y Files.move(origen, destino) lanzan FileAlreadyExistsException si el destino ya existe y no se especifica ninguna opcion",
          "code": "Path origen = Path.of(\"/tmp/reporte.txt\");\nPath destino = Path.of(\"/tmp/reporte_copia.txt\"); // ya existe\ntry {\n    Files.copy(origen, destino);\n} catch (FileAlreadyExistsException e) {\n    System.out.println(\"ya existe: \" + e.getFile());\n}",
          "answer": "ok",
          "explanation": "Sin ninguna CopyOption, tanto Files.copy() como Files.move() se niegan a sobrescribir un destino que ya existe: lanzan FileAlreadyExistsException en tiempo de ejecucion. La opcion para permitir la sobrescritura se ve en el siguiente concepto.",
          "conceptId": "files-copy-move",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["files-existencia-atributos"]
        },
        {
          "id": "nio2-filescopy-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que pasa al llamar Files.copy(origen, destino) si destino ya existe y no se especifica ninguna CopyOption?",
          "answer": "Se lanza FileAlreadyExistsException en tiempo de ejecucion, sin copiar nada",
          "distractors": [
            "El destino se sobrescribe en silencio, reemplazando por completo su contenido anterior",
            "Java le agrega un sufijo automatico al nombre, como 'reporte_copia_1.txt', para evitar el conflicto",
            "El metodo no hace nada y retorna sin error, dejando el destino exactamente como estaba antes"
          ],
          "explanation": "Files.copy() sin opciones adicionales es intencionalmente conservador: si el destino ya existe, lanza FileAlreadyExistsException en vez de arriesgarse a sobrescribir datos por accidente. No hay renombrado automatico ni sobrescritura silenciosa -- hace falta pedirlo explicitamente.",
          "conceptId": "files-copy-move",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["files-existencia-atributos"]
        },
        {
          "id": "nio2-filescopy-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Mismo escenario, pero con Files.move(origen, destino) en vez de copy(), si destino ya existe y no se especifica ninguna opcion. Que pasa?",
          "answer": "Tambien se lanza FileAlreadyExistsException -- move() sigue exactamente la misma regla que copy() frente a un destino existente",
          "distractors": [
            "move() nunca falla por esto, ya que desplaza el archivo en vez de duplicarlo y no necesita espacio extra",
            "move() sobrescribe el destino en silencio por defecto, a diferencia de copy() que si es conservador",
            "Se lanza una excepcion distinta y especifica de move(), sin relacion con la de copy()"
          ],
          "explanation": "Files.move() comparte el mismo comportamiento por defecto que Files.copy(): sin una CopyOption como REPLACE_EXISTING, ambos se niegan a sobrescribir un destino existente y lanzan la misma FileAlreadyExistsException. No existe una excepcion separada especifica para move().",
          "conceptId": "files-copy-move",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["files-existencia-atributos"]
        },
        {
          "id": "nio2-filesoption-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "StandardCopyOption.REPLACE_EXISTING habilita sobrescribir sin error; Files.delete() lanza NoSuchFileException si no existe, deleteIfExists() en el mismo caso devuelve false",
          "code": "Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING); // sobrescribe sin lanzar nada\n\ntry {\n    Files.delete(Path.of(\"/tmp/no_existe.txt\"));\n} catch (NoSuchFileException e) {\n    System.out.println(\"no existia: \" + e.getFile());\n}\n\nboolean borrado = Files.deleteIfExists(Path.of(\"/tmp/no_existe.txt\"));\nSystem.out.println(borrado);  // false, sin excepcion",
          "answer": "ok",
          "explanation": "StandardCopyOption.REPLACE_EXISTING resuelve directamente el caso visto en el concepto anterior: con esa opcion, Files.copy()/move() sobrescriben el destino sin lanzar FileAlreadyExistsException. Para borrar, Files.delete() es estricto (lanza NoSuchFileException si no existe), mientras que Files.deleteIfExists() es su version tolerante: devuelve false en el mismo caso, sin lanzar nada -- se elige uno u otro segun si la ausencia del archivo es un error real o un caso esperado.",
          "conceptId": "files-standardcopyoption-delete",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["files-copy-move"]
        },
        {
          "id": "nio2-filesoption-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cual de estas lineas permite que Files.copy() sobrescriba un destino que ya existe, sin lanzar FileAlreadyExistsException?",
          "answer": "Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);",
          "distractors": [
            "Files.copy(origen, destino, StandardOpenOption.WRITE);",
            "Files.copy(origen, destino, true);",
            "Files.copy(destino, origen, StandardCopyOption.REPLACE_EXISTING);"
          ],
          "explanation": "StandardCopyOption.REPLACE_EXISTING es la constante correcta para permitir la sobrescritura. StandardOpenOption es para abrir streams, no para copy(); un boolean no es un CopyOption valido en la firma de Files.copy(); e invertir origen/destino sobrescribiria el archivo equivocado.",
          "conceptId": "files-standardcopyoption-delete",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["files-copy-move"]
        },
        {
          "id": "nio2-filesoption-solo",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Completa el metodo que borra un archivo sin lanzar excepcion si no existe, devolviendo un boolean en su lugar:",
          "code": "boolean borrado = Files._____(ruta);",
          "answer": "deleteIfExists",
          "distractors": ["delete", "remove", "tryDelete"],
          "explanation": "Files.deleteIfExists() borra el archivo si existe y devuelve true; si no existe, devuelve false sin lanzar nada. Files.delete() en cambio es estricto: lanza NoSuchFileException si la ruta no existe.",
          "conceptId": "files-standardcopyoption-delete",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["files-copy-move"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Validate the file parses**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/io-nio2.json')); print('OK')"`
Expected: `OK`, no exception.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/io-nio2.json
git commit -m "content: add Files y operaciones unit to I/O y NIO.2"
```

---

### Task 3: Validate the whole file, bump content version, run the full suite

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Read-only validation: `app/src/main/assets/content/io-nio2.json`

**Interfaces:**
- Consumes: the final state of `io-nio2.json` after Task 2 (4 units, 36
  exercises total).
- Produces: `CURRENT_CONTENT_VERSION` bumped by one from whatever it is
  at dispatch time (it was `"22"` at plan-writing time — check the file
  first, do not assume).

- [ ] **Step 1: Write and run a full validation script**

```bash
python3 << 'EOF'
import json, re

path = "app/src/main/assets/content/io-nio2.json"
data = json.load(open(path))

assert data["sectionId"] == "java-io-nio2"
assert data["orderIndex"] == 7
assert data["examVersion"] == "core"

all_units = {u["unitId"]: u for u in data["units"]}
expected_units = {
    "io-streams-clasicos": 9, "io-serializacion": 9,
    "nio2-path-basico": 9, "nio2-files-operaciones": 9,
}
assert set(all_units.keys()) == set(expected_units.keys()), f"unexpected units: {list(all_units.keys())}"

expected_order = {"io-streams-clasicos": 1, "io-serializacion": 2, "nio2-path-basico": 3, "nio2-files-operaciones": 4}
all_exercises = []
for uid, expected in expected_units.items():
    unit = all_units[uid]
    exercises = unit["exercises"]
    assert len(exercises) == expected, f"{uid}: expected {expected} exercises, got {len(exercises)}"
    assert unit["certObjective"] == "io-nio2", f"{uid}: unexpected certObjective {unit['certObjective']}"
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
print(f"Total exercises across all four units: {total}")
assert total == 36
EOF
```

Expected: `Unit counts, certObjective, and orderIndex all correct.` then
`One-terminal-role, dependsOn, sequential pathOrder, and difficulty-
monotonicity rules all passed.` then `fill_blank untaught-identifier rule
passed.` then `No accented characters, no predict_output.` then four
reachability lines (one per unit), then
`Total exercises across all four units: 36`, no assertion errors.

- [ ] **Step 2: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find the current `CURRENT_CONTENT_VERSION` value and increment it by one
(check the file first — do not assume a specific starting number).

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest --rerun-tasks`
Expected: BUILD SUCCESSFUL, all existing tests still passing, including
`ContentCorpusLadderConsistencyTest` picking up the two new units in this
file automatically (no test file changes needed for this task). Use
`--rerun-tasks` — content-only JSON edits are not tracked as a Gradle
test input, so a bare run can silently report a stale `UP-TO-DATE` result
(known project pitfall, see `project_io_nio2_subcycle1_status` memory).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: register I/O y NIO.2 sub-cycle 2 and bump content version"
```

---

## After the plan: on-device QA

Install a clean/in-place build and verify on-device. Play both new units
end to end (18 exercises total: 9 for `nio2-path-basico`, 9 for
`nio2-files-operaciones`), confirming: `worked_example` intros render
before their guided/solo steps; the `dependsOn` chain unlocks concepts
one at a time within each unit; `nio2-files-operaciones` stays locked
until `nio2-path-basico` is fully complete (`orderIndex`-driven unit
unlock, no explicit dependsOn field involved); both `fill_blank` solos
(`getParent`, `deleteIfExists`) grade correctly when typed; the section
"I/O y NIO.2" shows all 4 units and is fully playable end to end,
matching the sub-cycle 1 pattern already confirmed on-device.

Per the spec's QA note, attempt automated verification where it's
actually reliable (e.g. direct sqlite inspection of `review_state` rows
after a play-through, matching the technique already used for the
session-unit-extension fix's QA) on top of the manual play-through —
`adb input tap` remains reserved only for navigation/screenshots, never
for answering exercises.

This closes the "I/O y NIO.2" section completely (4 units / 12 concepts
/ 36 exercises). Update `project_1z0_830_roadmap_correction_status`
memory's "next up" pointer once this is merged, QA'd, and pushed — next
in the roadmap is Localización.
