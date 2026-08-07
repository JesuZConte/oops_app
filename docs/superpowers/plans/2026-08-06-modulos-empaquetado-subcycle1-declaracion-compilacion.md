# Modulos y Empaquetado - Sub-cycle 1 (Declaracion + Compilacion) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the brand-new "Modulos y Empaquetado" section (JPMS —
Java Platform Module System), the first entirely new section in the
new-sections roadmap (`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`,
item 6). This sub-cycle covers the first half of that item's scope:
module declaration syntax (`module-info.java`, `exports`, `requires`,
`exports ... to`, `uses`/`provides ... with` as declaration syntax) and
compilation/packaging/execution (`--module-source-path`, `-d`, modular JAR
creation with `--main-class`/`--module-version`, running with `-p`/`-m`).
The runtime `ServiceLoader` pattern and migration/compatibility topics
(unnamed module, automatic modules, split packages, `--add-exports`) are
explicitly deferred to sub-cycle 2.

**Architecture:** Pure content-authoring plus one minimal, mechanical
Kotlin registration change — the first time in this content series that a
brand-new section (not a retrofit or an append to an existing file)
requires touching Kotlin at all. The new content lives entirely in a new
file, `app/src/main/assets/content/modules-packaging.json`; the only
Kotlin diff is appending its path to
`ContentPackRegistry.assetPaths` and bumping `CURRENT_CONTENT_VERSION` in
`ContentSeeder.kt`. There is zero grandfathering concern anywhere in this
plan — every exercise in this brand-new file is new, so no task needs to
preserve pre-existing `id`/`type`/`prompt`/`code`/`answer`/`distractors`/
`explanation` values the way every retrofit sub-cycle in this series did.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell, JUnit4 for
the existing use-case test suite (must stay green — the only Kotlin
change is a one-line list append and a version constant bump, neither of
which any existing test asserts a specific value against, per the
`ContentPackRegistry`/`ContentMapperTest`/`ContentPackParsingTest` check
done during design).

## Global Constraints

- **Section identity:** `sectionId: "java-modules-packaging"`,
  `name: "Modulos y Empaquetado"`, `orderIndex: 6` (sections 1-5 are
  already taken: fundamentals=1, generics-collections=2, streams=3,
  exception-handling=4, concurrency=5), `examVersion: "core"` (JPMS is
  Java 9+, and 4 of the 5 existing section files already use `"core"`;
  only `streams.json` uses `"java21"`).
- **`certObjective` is a single value shared by every unit in the file**
  (confirmed by inspecting all 5 existing content files — e.g. every unit
  in `concurrency.json` has `"certObjective": "concurrency"`, not a
  per-unit value). Every unit in this new file uses
  `"certObjective": "modules-packaging"`.
- **New file, not an append** — Task 1 uses Write (not Edit) to create
  `app/src/main/assets/content/modules-packaging.json` from scratch. Task
  2 then uses Edit to insert the second unit before the file's closing
  `]`/`}`. Both tasks must produce valid, complete JSON on their own —
  verify with `python3 -c "import json; json.load(open(...))"` after each.
- **One-terminal-role rule:** every `conceptId` has exactly one exercise
  with `role: "solo"` or `role: "practice"` — never zero, never two.
- **dependsOn same-unit-only rule:** every id listed in a `dependsOn`
  array must be a `conceptId` that exists elsewhere in the *same* unit's
  `exercises` array.
- **Sequential pathOrder rule:** within each unit, `pathOrder` values run
  `0..n-1` with no gaps or duplicates, in the same order as the array's
  physical layout. Unit A (`mod-declaracion`) has 15 exercises
  (`pathOrder` 0-14); Unit B (`mod-compilacion`) has 9 exercises
  (`pathOrder` 0-8) — `pathOrder` restarts at 0 per unit, matching every
  existing multi-unit file in this corpus.
- **Case-collision rule:** no `mcq`/`fill_blank` exercise's `distractors`
  may differ from its own `answer` only by letter case.
- **No accents in Spanish content** (project-wide convention, verified
  with a full-file accented-character scan in Task 3). No voseo — tuteo
  only (e.g. "Completa", not "Completá").
- **worked_example format:** every `intro`-role exercise has
  `"type": "worked_example"`, `"answer": "ok"`, no `distractors` field,
  and a `code` field showing the illustrative snippet.
- **No `predict_output` in this sub-cycle.** Every runtime-observable or
  compiler-observable claim here (exact `javac`/`jar` error and metadata
  text) was JDK-verified during design (see below), but raw compiler
  error text is unsuitable for `predict_output`'s free-text exact-match
  grading — it embeds source paths/line numbers that don't generalize,
  unlike a program's stdout. Every exercise testing an error scenario is
  `mcq`, framed around the *conceptual reason/consequence*, quoting the
  verified error text only inside the `explanation` field (in English,
  since that's the actual compiler output — matching the established
  precedent of keeping verbatim JDK-produced text like `Thread.State`
  names untranslated inside otherwise-Spanish explanations).
- **Runtime/compiler-observable content must be verified against a real
  JDK, not just read.** Every factual claim below was executed against a
  local JDK 20 during design in a throwaway multi-module scratch project
  (`javac --module-source-path`/`-d`, `java -p`/`-m`, `jar --create
  --main-class --module-version`, `jar --describe-module`) — do not
  re-derive these from documentation alone if you need to modify them;
  re-run them. Confirmed exact behaviors and error text:
  - Missing `exports`: `javac` fails with `package X is not visible` /
    `(package X is declared in module Y, which does not export it)`.
  - Qualified `exports ... to` with an unlisted consumer module: same
    error, but the parenthetical ends `which does not export it to
    module Z` (names the specific unlisted module).
  - A qualified `exports ... to` consumer that IS listed compiles
    successfully.
  - `--module-source-path <dir> -d <outdir>` compiles a multi-module
    source tree in one `javac` invocation; each module's compiled
    classes land in `<outdir>/<module-name>/`.
  - `jar --create --file X.jar --main-class C --module-version V -C
    <classdir> .` produces a modular JAR; `jar --describe-module` on the
    result shows `main-class C` and `name@V` in its output.
  - `java -p <dir> -m <module>/<Class>` and, when `--main-class` was
    embedded at JAR-creation time, `java -p <dir> -m <module>` (no class
    name) both run the program identically.
  - `uses`/`provides ... with` are declaration-only in `module-info.java`
    — no runtime behavior is exercised by this sub-cycle's content (the
    `ServiceLoader` runtime pattern, including the `ServiceConfigurationError`
    thrown when a consumer module omits `uses`, is reserved for sub-cycle
    2's content, not referenced here beyond the declaration syntax).

---

### Task 1: Create the new section file with Unit A (Declaracion de modulos)

**Files:**
- Create: `app/src/main/assets/content/modules-packaging.json`

**Interfaces:**
- Produces: section `java-modules-packaging` (`orderIndex: 6`,
  `examVersion: "core"`) containing one unit so far, `mod-declaracion`
  (`orderIndex: 1`, `certObjective: "modules-packaging"`), with 5
  concepts: `module-info-basics` (pathOrder 0-2, no `dependsOn`),
  `exports-basic` (pathOrder 3-5, `dependsOn: ["module-info-basics"]`),
  `requires-basic` (pathOrder 6-8, `dependsOn: ["module-info-basics"]`),
  `exports-to-qualified` (pathOrder 9-11, `dependsOn: ["exports-basic"]`),
  `uses-provides-syntax` (pathOrder 12-14, `dependsOn:
  ["requires-basic"]`).
- Consumes: nothing — this file does not exist yet.

- [ ] **Step 1: Write the file**

Create `app/src/main/assets/content/modules-packaging.json` with exactly
this content:

```json
{
  "sectionId": "java-modules-packaging",
  "name": "Modulos y Empaquetado",
  "orderIndex": 6,
  "examVersion": "core",
  "units": [
    {
      "unitId": "mod-declaracion",
      "name": "Declaracion de modulos",
      "certObjective": "modules-packaging",
      "orderIndex": 1,
      "summary": {
        "text": "Un modulo es una unidad de codigo con su propia identidad, declarada en un archivo module-info.java en la raiz de su arbol de codigo fuente. exports expone un paquete a cualquier modulo que declare requires sobre este modulo; sin exports, ese paquete es invisible fuera del modulo aunque sus clases sean public. exports ... to restringe esa visibilidad a modulos nombrados especificamente. uses y provides ... with declaran, a nivel de modulo, que este consume o implementa un servicio (el mecanismo real de descubrimiento en runtime se ve en la unidad de Servicios en JPMS).",
        "code": "module com.example.moda {\n    exports com.example.moda.api;\n    exports com.example.moda.internal to com.example.modb;\n    requires com.example.otro;\n}"
      },
      "exercises": [
        {
          "id": "mod-decl-basics-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "module-info.java declara un modulo: su nombre y que expone o requiere de otros modulos",
          "code": "module com.example.app {\n}",
          "answer": "ok",
          "explanation": "module-info.java va en la raiz del arbol de codigo fuente del modulo (junto a los paquetes, no dentro de uno). El nombre del modulo (com.example.app aqui) sigue por convencion el mismo estilo de nombres reverse-DNS que los paquetes, aunque tecnicamente puede ser cualquier identificador valido separado por puntos.",
          "conceptId": "module-info-basics",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "mod-decl-basics-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Donde debe ubicarse el archivo module-info.java de un modulo?",
          "answer": "En la raiz del arbol de codigo fuente del modulo, junto a sus paquetes de primer nivel",
          "distractors": ["Dentro de cada paquete que quiera exportar", "En un paquete especial llamado module-info", "En el classpath, fuera de cualquier estructura de paquetes"],
          "explanation": "module-info.java vive en la raiz del codigo fuente del modulo (por ejemplo src/com.example.app/module-info.java junto a src/com.example.app/com/...), no dentro de un paquete. El compilador lo busca ahi para identificar que ese arbol de fuentes forma un modulo.",
          "conceptId": "module-info-basics",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "mod-decl-basics-solo",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa la declaracion del modulo:",
          "code": "_____ com.example.app {\n}",
          "answer": "module",
          "distractors": ["package", "requires", "class"],
          "explanation": "La palabra clave module abre la declaracion de un modulo en module-info.java, seguida del nombre del modulo y un bloque con las directivas (exports, requires, etc.).",
          "conceptId": "module-info-basics",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "mod-decl-exports-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "exports expone un paquete a cualquier modulo que declare requires sobre este modulo; sin exports, el paquete es invisible aunque sus clases sean public",
          "code": "module com.example.moda {\n    exports com.example.moda.api; // publico para quien haga requires com.example.moda\n    // com.example.moda.internal NO esta exportado: invisible fuera del modulo\n}",
          "answer": "ok",
          "explanation": "exports opera a nivel de paquete, no de clase: aunque todas las clases de com.example.moda.internal sean public, ningun otro modulo puede verlas porque el paquete mismo no fue exportado. Esto es 'encapsulamiento fuerte': public ya no significa accesible desde cualquier lugar, sino accesible dentro del modulo (y fuera, solo si el paquete fue exportado).",
          "conceptId": "exports-basic",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["module-info-basics"]
        },
        {
          "id": "mod-decl-exports-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Un modulo B hace requires sobre un modulo A. A tiene una clase publica en un paquete que NO exporto. Que pasa si B intenta usar esa clase?",
          "answer": "No compila: el paquete no es visible fuera de A aunque la clase sea public, porque no fue exportado",
          "distractors": ["Compila sin problema porque la clase es public", "Compila pero lanza una excepcion en tiempo de ejecucion", "No compila porque falta un requires, no por el exports"],
          "explanation": "requires solo establece que B puede leer (depende de) A; no vuelve visibles los paquetes de A. Sin exports en A, javac rechaza el import con un error de que el paquete no es visible, sin importar que la clase sea public.",
          "conceptId": "exports-basic",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["module-info-basics"]
        },
        {
          "id": "mod-decl-exports-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Compilas un modulo consumidor que importa una clase de un paquete que el modulo proveedor no exporta. Cual es el resultado?",
          "answer": "Error de compilacion: el paquete existe en el otro modulo pero no fue exportado, por lo que no es visible",
          "distractors": ["Error de compilacion generico de import no encontrado, igual que si la clase no existiera", "Advertencia (warning), pero compila igual", "Funciona en compilacion pero falla al ejecutar con ClassNotFoundException"],
          "explanation": "Verificado con javac: el mensaje exacto es 'package X is not visible (package X is declared in module Y, which does not export it)'. Es un error especifico sobre visibilidad de modulos, distinto de un import roto por una clase que no existe -- javac sabe perfectamente que el paquete existe, pero no es accesible desde otro modulo sin exports.",
          "conceptId": "exports-basic",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["module-info-basics"]
        },
        {
          "id": "mod-decl-requires-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "requires declara que un modulo depende de (lee a) otro modulo; sin requires, ni siquiera un paquete exportado es visible",
          "code": "module com.example.modb {\n    requires com.example.moda; // ahora modb puede ver lo que moda exporta\n}",
          "answer": "ok",
          "explanation": "requires establece la relacion de lectura entre modulos: modb 'requires' moda significa que modb puede acceder a los paquetes que moda exporta. Sin este requires, aunque moda exporte el paquete perfectamente, modb no puede importarlo -- exports y requires son las dos mitades necesarias de la visibilidad entre modulos.",
          "conceptId": "requires-basic",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["module-info-basics"]
        },
        {
          "id": "mod-decl-requires-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Un modulo A exporta un paquete correctamente. Un modulo B quiere usar una clase de ese paquete pero olvido declarar requires A. Que pasa?",
          "answer": "No compila: exportar no basta, el modulo consumidor tambien debe declarar requires sobre el modulo que exporta",
          "distractors": ["Compila igual, porque el paquete ya es publico gracias a exports", "Compila pero falla en runtime al cargar la clase", "Solo falla si A tambien exporta a otros modulos"],
          "explanation": "exports y requires son complementarios: exports abre la puerta desde el lado del proveedor, requires declara del lado del consumidor que quiere entrar por esa puerta. Falta cualquiera de los dos y el import no compila.",
          "conceptId": "requires-basic",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["module-info-basics"]
        },
        {
          "id": "mod-decl-requires-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual de estas afirmaciones sobre requires es correcta?",
          "answer": "requires declara una dependencia de lectura hacia otro modulo; es necesario incluso si el paquete que se quiere usar ya fue exportado por el otro modulo",
          "distractors": ["requires reemplaza la necesidad de exports en el modulo proveedor", "requires solo es necesario si el modulo proveedor no exporta nada", "requires expone automaticamente los paquetes del modulo que lo declara a otros modulos"],
          "explanation": "requires no expone nada del modulo que lo declara (eso es trabajo de exports); solo declara que este modulo depende de otro y puede leer lo que ese otro modulo exporta. Son direcciones opuestas: exports mira hacia afuera (que expongo), requires mira hacia adentro (que necesito).",
          "conceptId": "requires-basic",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["module-info-basics"]
        },
        {
          "id": "mod-decl-exportsto-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "exports ... to restringe la visibilidad de un paquete a una lista especifica de modulos, en vez de exponerlo a cualquiera que haga requires",
          "code": "module com.example.moda {\n    exports com.example.moda.api to com.example.modb; // solo modb puede verlo\n}",
          "answer": "ok",
          "explanation": "Con exports simple, cualquier modulo que declare requires com.example.moda puede ver el paquete exportado. Con exports ... to, solo los modulos nombrados explicitamente (com.example.modb aqui) pueden verlo, aunque otros tambien declaren requires com.example.moda -- es una forma de exponer una API interna solo a colaboradores especificos, sin abrirla a todo el mundo.",
          "conceptId": "exports-to-qualified",
          "role": "intro",
          "pathOrder": 9,
          "dependsOn": ["exports-basic"]
        },
        {
          "id": "mod-decl-exportsto-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Un modulo A declara exports com.example.api to com.example.modb. Un modulo C (distinto de modb) declara requires com.example.a. Puede C usar clases de com.example.api?",
          "answer": "No: exports ... to solo hace visible el paquete para los modulos listados, aunque C tenga requires sobre A",
          "distractors": ["Si, porque requires siempre basta si el paquete fue exportado a alguien", "Si, pero solo si C tambien exporta algo de vuelta a A", "No compila ningun modulo, ni siquiera modb, porque exports ... to esta mal usado"],
          "explanation": "La lista de to es exhaustiva: solo esos modulos nombrados ven el paquete. requires en C establece que C puede leer A, pero no vuelve visible un paquete que A decidio exportar solo a modb -- son dos mecanismos independientes (lectura de modulo vs visibilidad de paquete).",
          "conceptId": "exports-to-qualified",
          "role": "guided",
          "pathOrder": 10,
          "dependsOn": ["exports-basic"]
        },
        {
          "id": "mod-decl-exportsto-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Compilas un modulo C que no aparece en la lista to de un exports calificado de otro modulo, e importa una clase de ese paquete. Cual es el resultado?",
          "answer": "Error de compilacion: el paquete no es visible para C, aunque si lo sea para los modulos listados en el to",
          "distractors": ["Compila para todos los modulos igual, to solo afecta documentacion", "Falla en runtime, no en compilacion", "Compila solo si C tiene mas de un requires"],
          "explanation": "Verificado con javac: el mensaje de error para un exports calificado dice explicitamente 'which does not export it to module C' -- distinto del mensaje generico de un exports no calificado ('which does not export it'), porque javac sabe que el paquete SI se exporta, solo que no a este modulo especifico.",
          "conceptId": "exports-to-qualified",
          "role": "solo",
          "pathOrder": 11,
          "dependsOn": ["exports-basic"]
        },
        {
          "id": "mod-decl-usesprovides-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "uses declara que un modulo consume un servicio (una interfaz); provides ... with declara que un modulo ofrece una implementacion concreta de ese servicio",
          "code": "// modulo que ofrece el servicio\nmodule com.example.modg {\n    requires com.example.modf;\n    provides com.example.modf.api.Service with com.example.modg.impl.ServiceImpl;\n}\n\n// modulo que consume el servicio\nmodule com.example.modh {\n    requires com.example.modf;\n    uses com.example.modf.api.Service;\n}",
          "answer": "ok",
          "explanation": "uses y provides ... with son solo declaraciones a nivel de module-info.java: anuncian la intencion de consumir o proveer un servicio, pero no ejecutan nada por si solas. El mecanismo que realmente busca las implementaciones en tiempo de ejecucion (ServiceLoader) se ve en la unidad de Servicios en JPMS -- aqui el foco es reconocer la sintaxis de declaracion.",
          "conceptId": "uses-provides-syntax",
          "role": "intro",
          "pathOrder": 12,
          "dependsOn": ["requires-basic"]
        },
        {
          "id": "mod-decl-usesprovides-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que declara la directiva provides com.example.api.Service with com.example.impl.ServiceImpl; en un module-info.java?",
          "answer": "Que este modulo ofrece ServiceImpl como una implementacion concreta de la interfaz de servicio Service",
          "distractors": ["Que este modulo requiere que exista una implementacion de Service en otro modulo", "Que ServiceImpl reemplaza a Service en todo el programa", "Que este modulo exporta el paquete de ServiceImpl automaticamente"],
          "explanation": "provides X with Y declara: 'este modulo provee una implementacion (Y) para el servicio/interfaz (X)'. No exporta automaticamente el paquete de la implementacion (Y puede quedar sin exportar, ya que solo el mecanismo de descubrimiento de servicios necesita acceder a ella).",
          "conceptId": "uses-provides-syntax",
          "role": "guided",
          "pathOrder": 13,
          "dependsOn": ["requires-basic"]
        },
        {
          "id": "mod-decl-usesprovides-solo",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Completa la declaracion: este modulo consume el servicio Service, sin proveer ninguna implementacion propia",
          "code": "module com.example.modh {\n    requires com.example.modf;\n    _____ com.example.modf.api.Service;\n}",
          "answer": "uses",
          "distractors": ["requires", "provides", "exports"],
          "explanation": "uses declara que este modulo consume (busca implementaciones de) el servicio Service en tiempo de ejecucion, via el mecanismo de ServiceLoader -- sin uses, intentar buscar el servicio en runtime falla (se ve en detalle en la proxima unidad).",
          "conceptId": "uses-provides-syntax",
          "role": "solo",
          "pathOrder": 14,
          "dependsOn": ["requires-basic"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Verify the JSON is well-formed**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/modules-packaging.json'))"`
Expected: no output (valid JSON, no exception).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/modules-packaging.json
git commit -m "content: add Modulos y Empaquetado section with Declaracion de modulos unit"
```

---

### Task 2: Add Unit B (Compilacion y ejecucion)

**Files:**
- Modify: `app/src/main/assets/content/modules-packaging.json` (append a
  second unit to the `"units"` array created in Task 1)

**Interfaces:**
- Consumes: the file created in Task 1 — do not touch the
  `mod-declaracion` unit or any of its 15 exercises.
- Produces: unit `mod-compilacion` (`orderIndex: 2`, `certObjective:
  "modules-packaging"`), 3 concepts: `module-source-path-compile`
  (pathOrder 0-2, no `dependsOn`), `modular-jar-packaging` (pathOrder 3-5,
  `dependsOn: ["module-source-path-compile"]`), `running-modules`
  (pathOrder 6-8, `dependsOn: ["modular-jar-packaging"]`).

- [ ] **Step 1: Insert the second unit**

In `app/src/main/assets/content/modules-packaging.json`, find this exact
trailing text (the end of the `mod-declaracion` unit's last exercise,
followed by the closing of the `exercises` array, the unit object, the
`"units"` array, and the top-level object — copy it verbatim from the
current file, do not retype from memory):

```json
          "conceptId": "uses-provides-syntax",
          "role": "solo",
          "pathOrder": 14,
          "dependsOn": ["requires-basic"]
        }
      ]
    }
  ]
}
```

Replace it with this exact text — the same closing exercise and braces,
now followed by a comma and the new `mod-compilacion` unit object, then
the original closing `]` and `}`:

```json
          "conceptId": "uses-provides-syntax",
          "role": "solo",
          "pathOrder": 14,
          "dependsOn": ["requires-basic"]
        }
      ]
    },
    {
      "unitId": "mod-compilacion",
      "name": "Compilacion y ejecucion",
      "certObjective": "modules-packaging",
      "orderIndex": 2,
      "summary": {
        "text": "javac --module-source-path <dir> -d <salida> compila un arbol de codigo fuente organizado en modulos (una carpeta por modulo, cada una con su module-info.java). jar --create --main-class <Clase> --module-version <version> empaqueta un modulo compilado en un JAR modular, embebiendo su clase principal y version. java --module-path <dir> --module <modulo>/<Clase> (o -p/-m) ejecuta un modulo desde el module path; si el JAR ya tiene --main-class embebido, alcanza con --module <modulo> sin repetir la clase.",
        "code": "javac --module-source-path src -d out $(find src -name \"*.java\")\njar --create --file mods/app.jar --main-class com.example.app.Main --module-version 1.0 -C out/com.example.app .\njava -p mods -m com.example.app"
      },
      "exercises": [
        {
          "id": "mod-comp-sourcepath-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "--module-source-path le indica a javac donde esta el arbol de codigo fuente organizado en modulos (una carpeta por modulo); -d indica donde escribir las clases compiladas",
          "code": "// estructura de carpetas:\n// src/com.example.moda/module-info.java\n// src/com.example.moda/com/example/moda/api/Greeter.java\n// src/com.example.modb/module-info.java\n// src/com.example.modb/com/example/modb/Main.java\n\njavac --module-source-path src -d out src/com.example.moda/module-info.java src/com.example.moda/com/example/moda/api/Greeter.java src/com.example.modb/module-info.java src/com.example.modb/com/example/modb/Main.java",
          "answer": "ok",
          "explanation": "--module-source-path apunta a la carpeta que contiene una subcarpeta por cada modulo (el nombre de la subcarpeta debe coincidir con el nombre del modulo declarado en su module-info.java). -d out le dice a javac donde generar la salida compilada, tambien organizada en una subcarpeta por modulo dentro de out. Verificado compilando este ejemplo exacto con un JDK real: compila sin errores y produce out/com.example.moda/ y out/com.example.modb/.",
          "conceptId": "module-source-path-compile",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "mod-comp-sourcepath-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Al compilar varios modulos con --module-source-path, que debe coincidir con el nombre de cada modulo declarado en su module-info.java?",
          "answer": "El nombre de la subcarpeta de ese modulo dentro del directorio pasado a --module-source-path",
          "distractors": ["El nombre del archivo .jar final", "El nombre del paquete raiz, que puede ser distinto del nombre del modulo", "Nada; javac detecta el nombre del modulo automaticamente del contenido del archivo"],
          "explanation": "javac usa la estructura de carpetas para saber que codigo fuente pertenece a cada modulo: espera una subcarpeta cuyo nombre sea exactamente el nombre del modulo (el mismo que aparece despues de la palabra module en su module-info.java), conteniendo ese module-info.java y los paquetes del modulo.",
          "conceptId": "module-source-path-compile",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "mod-comp-sourcepath-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el comando para compilar el arbol de modulos en src/ hacia la carpeta out/:",
          "code": "javac _____ src -d out $(find src -name \"*.java\")",
          "answer": "--module-source-path",
          "distractors": ["--source-path", "--module-path", "--classpath"],
          "explanation": "--module-source-path es el flag que le dice a javac que el codigo fuente esta organizado en multiples modulos bajo ese directorio. --module-path (usado en compilacion y ejecucion) apunta a modulos ya compilados/empaquetados, no a codigo fuente.",
          "conceptId": "module-source-path-compile",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "mod-comp-jar-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "jar --create empaqueta las clases compiladas de un modulo en un JAR modular; --main-class y --module-version embeben metadatos en su module-info.class",
          "code": "jar --create --file mods/app.jar \\\n    --main-class com.example.app.Main \\\n    --module-version 1.0 \\\n    -C out/com.example.app .",
          "answer": "ok",
          "explanation": "-C out/com.example.app . le dice a jar que entre a esa carpeta y empaquete todo su contenido (las .class compiladas, incluyendo module-info.class) como si fuera la raiz del jar. --main-class embebe la clase con main() para no tener que especificarla al ejecutar; --module-version embebe un numero de version consultable con jar --describe-module. Verificado con un JDK real: jar --describe-module sobre el jar resultante muestra 'main-class com.example.app.Main' y el modulo con su version.",
          "conceptId": "modular-jar-packaging",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["module-source-path-compile"]
        },
        {
          "id": "mod-comp-jar-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que logra pasar --main-class al crear un JAR modular con jar --create?",
          "answer": "Embebe la clase con el metodo main() en los metadatos del modulo, para poder ejecutar el jar sin repetir el nombre de la clase",
          "distractors": ["Hace que ese sea el unico main() permitido en todo el JAR, eliminando otras clases con main()", "Reemplaza la necesidad de declarar exports en el module-info.java", "Convierte automaticamente el JAR en ejecutable con doble clic, sin necesitar java -m"],
          "explanation": "--main-class solo embebe metadata (que clase tiene el main() a usar por defecto). Con esa metadata presente, java -p <dir> -m <modulo> alcanza para ejecutar sin especificar <modulo>/<Clase>; sin ella, hay que nombrar la clase explicitamente.",
          "conceptId": "modular-jar-packaging",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["module-source-path-compile"]
        },
        {
          "id": "mod-comp-jar-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Para que sirve --module-version al crear un JAR modular?",
          "answer": "Embebe un numero de version en los metadatos del modulo, consultable luego (por ejemplo con jar --describe-module)",
          "distractors": ["Fija que version minima de Java puede ejecutar el jar", "Determina el orden en que se cargan los modulos en el module path", "Reemplaza la necesidad de --main-class si el jar tiene una sola clase"],
          "explanation": "--module-version es puramente informativo/metadata: no afecta la resolucion de modulos en tiempo de ejecucion (a diferencia de otros sistemas de gestion de dependencias). Verificado con jar --describe-module: el modulo aparece listado como 'nombre@version'.",
          "conceptId": "modular-jar-packaging",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["module-source-path-compile"]
        },
        {
          "id": "mod-comp-run-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "java -p (--module-path) indica donde buscar los modulos compilados o empaquetados; -m (--module) indica cual ejecutar. Si el jar tiene --main-class embebido, no hace falta repetir la clase",
          "code": "// con clase explicita:\njava -p mods -m com.example.app/com.example.app.Main\n\n// si el jar fue creado con --main-class, alcanza con:\njava -p mods -m com.example.app",
          "answer": "ok",
          "explanation": "-p (o --module-path) apunta a una carpeta con jars modulares o modulos compilados sueltos; -m (o --module) especifica que modulo ejecutar, opcionalmente seguido de /NombreClase si el modulo no tiene --main-class embebido. Verificado con un JDK real: ambas formas del comando ejecutan exactamente el mismo programa cuando el jar fue creado con --main-class.",
          "conceptId": "running-modules",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["modular-jar-packaging"]
        },
        {
          "id": "mod-comp-run-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Ejecutas java -p mods -m com.example.app, sin especificar la clase, y el jar de com.example.app NO fue creado con --main-class. Que pasa?",
          "answer": "Falla al iniciar: java no sabe que clase ejecutar sin --main-class embebido ni la clase especificada explicitamente",
          "distractors": ["Ejecuta la primera clase con main() que encuentre en el jar, en orden alfabetico", "Ejecuta module-info.class como si fuera el punto de entrada", "Funciona igual, java busca automaticamente en todos los jars del module path"],
          "explanation": "java necesita saber explicitamente que clase tiene el main() a ejecutar: eso viene de --main-class embebido al crear el jar, o especificando modulo/Clase en la linea de comandos (java -p mods -m com.example.app/com.example.app.Main). Sin ninguna de las dos, no hay forma de que java adivine cual es el punto de entrada.",
          "conceptId": "running-modules",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["modular-jar-packaging"]
        },
        {
          "id": "mod-comp-run-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el comando para ejecutar el modulo com.example.app desde la carpeta mods/, especificando su clase principal:",
          "code": "java -p mods _____ com.example.app/com.example.app.Main",
          "answer": "-m",
          "distractors": ["-cp", "--module-source-path", "-jar"],
          "explanation": "-m (equivalente a --module) indica que modulo ejecutar y, opcionalmente, con que clase (modulo/Clase). -cp es para el classpath clasico (sin modulos); --module-source-path es solo para compilacion, no para ejecucion; -jar ejecuta un jar por classpath tradicional, ignorando el sistema de modulos.",
          "conceptId": "running-modules",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["modular-jar-packaging"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Verify the JSON is well-formed and the diff is append-only**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/modules-packaging.json'))"`
Expected: no output (valid JSON, no exception).

Run: `git diff app/src/main/assets/content/modules-packaging.json` and
confirm the *only* change is the insertion of the new `mod-compilacion`
unit object — every byte of the `mod-declaracion` unit from Task 1 is
untouched (diff shows only additions, zero deletions, except possibly the
single character changing a trailing comma at the insertion point).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/modules-packaging.json
git commit -m "content: add Compilacion y ejecucion unit to Modulos y Empaquetado"
```

---

### Task 3: Register the section, validate the whole file, bump content version

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Read-only validation: `app/src/main/assets/content/modules-packaging.json`

**Interfaces:**
- Consumes: the final state of `modules-packaging.json` after Task 2.
- Produces: the new section registered and loadable; `CURRENT_CONTENT_VERSION`
  bumped by one from whatever it is at dispatch time (check
  `ContentSeeder.kt`'s current value first — do not assume a specific
  number; it was `"17"` at plan-writing time, but other cycles may have
  landed between this plan being written and executed).

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
        "content/concurrency.json"
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
        "content/modules-packaging.json"
    )
}
```

This is the only change in this file — nothing else in
`ContentPackRegistry.kt` is touched.

- [ ] **Step 2: Write and run a full validation script**

```bash
python3 << 'EOF'
import json, re

path = "app/src/main/assets/content/modules-packaging.json"
data = json.load(open(path))

assert data["sectionId"] == "java-modules-packaging"
assert data["orderIndex"] == 6
assert data["examVersion"] == "core"

all_units = {u["unitId"]: u for u in data["units"]}
assert set(all_units.keys()) == {"mod-declaracion", "mod-compilacion"}, f"unexpected units: {list(all_units.keys())}"

expected_counts = {"mod-declaracion": 15, "mod-compilacion": 9}
all_exercises = []
for uid, expected in expected_counts.items():
    unit = all_units[uid]
    exercises = unit["exercises"]
    assert len(exercises) == expected, f"{uid}: expected {expected} exercises, got {len(exercises)}"
    assert unit["certObjective"] == "modules-packaging", f"{uid}: unexpected certObjective {unit['certObjective']}"
    all_exercises.append((uid, exercises))
assert all_units["mod-declaracion"]["orderIndex"] == 1
assert all_units["mod-compilacion"]["orderIndex"] == 2
print("Unit counts, certObjective, and orderIndex all correct.")

# Case-collision rule (whole file).
for uid, exercises in all_exercises:
    for e in exercises:
        ans = e.get("answer")
        for d in e.get("distractors", []):
            assert not (isinstance(ans, str) and d.lower() == ans.lower() and d != ans), \
                f"{uid}/{e['id']}: distractor '{d}' differs from answer only by case"

# One-terminal-role rule + dependsOn same-unit-only rule + sequential pathOrder, per unit.
for uid, exercises in all_exercises:
    concept_ids = {e["conceptId"] for e in exercises if e.get("conceptId")}
    terminal_counts = {}
    orders = []
    for e in exercises:
        orders.append(e.get("pathOrder"))
        if e.get("role") in ("solo", "practice"):
            cid = e["conceptId"]
            terminal_counts[cid] = terminal_counts.get(cid, 0) + 1
        for dep in e.get("dependsOn", []):
            assert dep in concept_ids, f"{uid}/{e['id']}: dependsOn '{dep}' not a concept in this unit"
    for cid in concept_ids:
        assert terminal_counts.get(cid) == 1, f"{uid}/concept '{cid}' has {terminal_counts.get(cid, 0)} terminal exercises, expected 1"
    assert orders == list(range(len(orders))), f"{uid}: pathOrder not sequential 0..n-1: {sorted(orders)}"
print("One-terminal-role, dependsOn, and sequential pathOrder rules all passed.")

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
EOF
```

Expected: `Unit counts, certObjective, and orderIndex all correct.` then
`One-terminal-role, dependsOn, and sequential pathOrder rules all
passed.` then `No accented characters, no predict_output.` then two
reachability lines (one per unit), no assertion errors.

- [ ] **Step 3: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find the current `CURRENT_CONTENT_VERSION` value and increment it by one
(check the file first — do not assume a specific starting number).

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still passing. No test
changes are needed for this task — confirm during review that
`ContentMapperTest.kt` and `ContentPackParsingTest.kt` (which use inline
fixtures, not the real asset files) are unaffected.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: register Modulos y Empaquetado section and bump content version"
```

---

## After the plan: manual on-device QA

Install a clean/in-place build and manually verify on-device (adb):

1. Confirm "Modulos y Empaquetado" appears as a new 6th section on the
   learning path screen, after "Concurrencia", in locked/unlocked state
   consistent with the app's existing section-gating rules.
2. Play through "Declaracion de modulos": confirm each of the 5 concepts'
   `guided`/`solo` exercises only becomes reachable per its `dependsOn`
   (e.g. `exports-to-qualified` exercises should stay locked until
   `exports-basic`'s solo exercise is answered).
3. Confirm all `worked_example` intro cards render their multi-line code
   blocks (including the ones with embedded `//` comments and blank
   lines separating two module declarations) without truncation or
   layout issues, and that they auto-advance without being graded (per
   this project's established `worked_example` behavior).
4. Confirm the two `fill_blank` exercises in "Compilacion y ejecucion"
   (`mod-comp-sourcepath-solo`, `mod-comp-run-solo`) render the answer
   input box with the question and code block still visible above it
   with the keyboard open (regression check for the keyboard-layout fix
   already shipped in this project,
   `app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`).
5. Play through "Compilacion y ejecucion" fully and confirm the whole
   new section (24 exercises across both units) can be completed end to
   end without getting stranded.
