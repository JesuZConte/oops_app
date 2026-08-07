# Modulos y Empaquetado - Sub-cycle 2 (Servicios en JPMS + Migracion y compatibilidad) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the second half of the "Modulos y Empaquetado" section's
scope (`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`, item 6):
the `ServiceLoader` runtime pattern (`ServiceLoader.load()`, provider
instantiation rules, `ServiceConfigurationError`) and migration/
compatibility topics (unnamed module, automatic modules, split packages,
`--add-opens` for reflective access). Sub-cycle 1 (`372526e`, merged and
on-device QA'd) covered declaration syntax and compilation/packaging;
this sub-cycle adds 2 more units to the same section, bringing it to 4
units / 45 exercises total.

**Architecture:** Pure content-authoring, append-only edits to the
existing `app/src/main/assets/content/modules-packaging.json` (created in
sub-cycle 1) plus a one-line content-version bump. No new Kotlin file and
no `ContentPackRegistry` change — the section is already registered.
Zero grandfathering concern: every exercise added by this plan is new.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell, JUnit4 for
the existing use-case test suite (must stay green).

## Global Constraints

- **File is append-only in this plan.** Task 1 and Task 2 each insert one
  new unit object into the existing `"units"` array of
  `app/src/main/assets/content/modules-packaging.json`, via exact-string
  `Edit`, never a JSON load+dump (rule inherited from sub-cycle 1 —
  regenerating the file from a parsed+re-serialized structure risks
  silently reordering keys or altering the 2 existing units byte-for-byte
  even when semantically equivalent).
- **Unit identity:**
  - `mod-servicios` / "Servicios en JPMS", `orderIndex: 3`,
    `certObjective: "modules-packaging"` (same value shared by every unit
    in this file — confirmed in sub-cycle 1 that `certObjective` is a
    per-SECTION value, not per-unit). 3 concepts, 9 exercises,
    `pathOrder` 0-8.
  - `mod-migracion` / "Migracion y compatibilidad", `orderIndex: 4`, same
    `certObjective`. 4 concepts, 12 exercises, `pathOrder` 0-11
    (restarts at 0 — `pathOrder` is per-unit, matching every existing
    multi-unit file in this corpus).
  - The exact name "Servicios en JPMS" is load-bearing: 2 existing
    exercises in `mod-declaracion` (sub-cycle 1) already reference "la
    unidad de Servicios en JPMS" by that literal text.
- **One-terminal-role rule:** every `conceptId` has exactly one exercise
  with `role: "solo"` or `role: "practice"` — never zero, never two.
- **dependsOn same-unit-only rule:** every id listed in a `dependsOn`
  array must be a `conceptId` that exists elsewhere in the *same* unit's
  `exercises` array.
- **Sequential pathOrder rule:** within each unit, `pathOrder` values run
  `0..n-1` with no gaps or duplicates, in the same order as the array's
  physical layout.
- **Case-collision rule:** no `mcq`/`fill_blank` exercise's `distractors`
  may differ from its own `answer` only by letter case.
- **Difficulty monotonicity within a concept:** all exercises sharing a
  `conceptId` use the same `difficulty` value (sub-cycle 1's final review
  flagged non-monotonic difficulty within one concept as a Minor finding
  — avoid it from the start this time by keeping difficulty uniform per
  concept).
- **No accents in Spanish content** (project-wide convention, verified
  with a full-file accented-character scan in Task 3, range `[À-ÿ]` —
  this also catches `ñ`, so avoid words containing it, e.g. prefer
  "codigo legado" over any phrasing needing "año"/"diseño"/similar). No
  voseo — tuteo only.
- **worked_example format:** every `intro`-role exercise has
  `"type": "worked_example"`, `"answer": "ok"`, no `distractors` field,
  and a `code` field showing the illustrative snippet.
- **No `predict_output` in this sub-cycle**, same reasoning as sub-cycle
  1: raw compiler/JVM error text is unsuitable for `predict_output`'s
  free-text exact-match grading. Every error-scenario exercise below is
  `mcq`, framed around the conceptual reason/consequence, quoting the
  verified error text only inside `explanation` (in English, since
  that's the actual JDK output — matching the established precedent of
  keeping verbatim JDK-produced text untranslated inside otherwise-
  Spanish explanations).
- **Runtime/compiler-observable content must be verified against a real
  JDK, not just read.** Every factual claim below was executed against a
  local JDK 20 during design (see
  `docs/superpowers/specs/2026-08-07-modulos-empaquetado-subcycle2-servicios-migracion-design.md`
  for the full verification log) — do not re-derive these from
  documentation alone if you need to modify them; re-run them. Confirmed
  exact behaviors and error text used verbatim below:
  - A provider class with neither a public no-arg constructor nor a
    public static `provider()` method fails at **compile time**:
    `javac` rejects the `provides ... with` clause with `"the no
    arguments constructor of the service implementation is not public:
    <Class>"`.
  - A consumer module that omits `uses` for a service it loads via
    `ServiceLoader.load()` compiles cleanly but fails at **runtime**
    with `java.util.ServiceConfigurationError: <Service>: module
    <consumer> does not declare \`uses\``.
  - Named-module `requires` on classpath-only (unnamed-module) code
    fails at compile time: `javac` reports `"module not found:
    <name>"`.
  - A plain JAR with no `module-info.class` becomes an automatic module
    on the module path; its name comes from the manifest's
    `Automatic-Module-Name` attribute if present, otherwise derived from
    the filename (hyphens to dots, trailing version-like suffix split
    off) — confirmed with a real jar: `foo-bar-utils-2.5.jar` (no
    special manifest) → `jar --describe-module` reports `foo.bar.utils@2.5
    automatic`. It exports all its packages implicitly and can be
    `requires`d by name from a named module.
  - Two separately-built modules on the module path sharing a package
    (split package) fail to resolve at **runtime**, not compile time —
    confirmed forcing resolution of two automatic-module jars sharing
    package `com.foo.bar`: `java.lang.module.ResolutionException: Module
    gadget.lib contains package com.foo.bar, module foo.bar.utils
    exports package com.foo.bar to gadget.lib`. This contrasts with the
    same conceptual conflict already taught in sub-cycle 1, which fails
    at **compile time** (`javac`) when both modules are compiled
    together from one `--module-source-path` tree.
  - Deep reflective access (`setAccessible(true)`) into a class in a
    non-exported package fails by default with `java.lang.
    IllegalAccessException: class <A> (in module <M1>) cannot access
    class <B> (in module <M2>) because module <M2> does not export
    <package> to module <M1>`. `--add-opens <module>/<package>=<target-
    module>` on the `java` command line (not `javac`) grants that access
    at runtime without the source module declaring `exports` or `opens`.

---

### Task 1: Add Unit C (Servicios en JPMS)

**Files:**
- Modify: `app/src/main/assets/content/modules-packaging.json` (append a
  third unit to the existing `"units"` array)

**Interfaces:**
- Consumes: the file as it exists after sub-cycle 1 — do not touch the
  `mod-declaracion` or `mod-compilacion` units or any of their 24
  exercises.
- Produces: unit `mod-servicios` (`orderIndex: 3`, `certObjective:
  "modules-packaging"`), 3 concepts: `serviceloader-basic-loading`
  (pathOrder 0-2, no `dependsOn`), `provider-factory-method` (pathOrder
  3-5, `dependsOn: ["serviceloader-basic-loading"]`),
  `missing-uses-runtime-error` (pathOrder 6-8, `dependsOn:
  ["serviceloader-basic-loading"]`).

- [ ] **Step 1: Insert the third unit**

In `app/src/main/assets/content/modules-packaging.json`, find this exact
trailing text (copy it verbatim from the current file, do not retype
from memory):

```json
          "dependsOn": ["modular-jar-packaging"]
        }
      ]
    }
  ]
}
```

Replace it with this exact text — the same closing exercise and unit
close, now followed by a comma and the new `mod-servicios` unit object,
then the original closing `]` and `}`:

```json
          "dependsOn": ["modular-jar-packaging"]
        }
      ]
    },
    {
      "unitId": "mod-servicios",
      "name": "Servicios en JPMS",
      "certObjective": "modules-packaging",
      "orderIndex": 3,
      "summary": {
        "text": "ServiceLoader.load(Interfaz.class) descubre en runtime las implementaciones que algun modulo declaro con provides ... with, instanciandolas de forma lazy durante la iteracion. Un provider valido necesita constructor publico sin argumentos o un metodo estatico publico provider(); si no tiene ninguno de los dos, javac lo rechaza en tiempo de compilacion. En cambio, si el modulo consumidor omite uses para el servicio, el codigo compila igual pero ServiceLoader.load() lanza ServiceConfigurationError recien en tiempo de ejecucion.",
        "code": "module com.example.modg {\n    requires com.example.modf;\n    provides com.example.modf.api.Service with com.example.modg.impl.ServiceImpl;\n}\n\nmodule com.example.modh {\n    requires com.example.modf;\n    uses com.example.modf.api.Service;\n}\n\nServiceLoader<Service> loader = ServiceLoader.load(Service.class);\nfor (Service s : loader) {\n    System.out.println(s.name());\n}"
      },
      "exercises": [
        {
          "id": "mod-serv-loading-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "ServiceLoader.load(Interfaz.class) descubre en el module path las implementaciones declaradas con provides ... with, y las devuelve como un ServiceLoader iterable",
          "code": "module com.example.modh {\n    requires com.example.modf;\n    uses com.example.modf.api.Service;\n}\n\n// codigo Java:\nServiceLoader<Service> loader = ServiceLoader.load(Service.class);\nfor (Service s : loader) {\n    System.out.println(s.name());\n}",
          "answer": "ok",
          "explanation": "ServiceLoader.load(Service.class) busca, entre los modulos visibles en el module path, todas las implementaciones que algun modulo declaro con provides Service with .... La iteracion es lazy: cada implementacion se instancia recien cuando el for la alcanza, no todas de una al llamar load(). Verificado con un JDK real: este codigo imprime el nombre de cada provider encontrado.",
          "conceptId": "serviceloader-basic-loading",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "mod-serv-loading-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Un modulo declara uses com.example.api.Service; y llama a ServiceLoader.load(Service.class). De donde saca ServiceLoader las implementaciones que puede devolver?",
          "answer": "De cualquier modulo visible en el module path que haya declarado provides Service with <Implementacion>",
          "distractors": ["Solo de clases que esten en el mismo paquete que la interfaz Service", "De un archivo de configuracion META-INF/services que hay que crear manualmente, ademas del provides en module-info.java", "De cualquier clase del module path que implemente Service, sin necesidad de que ningun modulo declare provides"],
          "explanation": "En JPMS (a diferencia del mecanismo clasico de ServiceLoader sobre el classpath, que si usaba archivos META-INF/services), el registro de providers es declarativo via provides ... with en module-info.java. ServiceLoader.load() recorre los modulos resueltos y arma la lista de providers a partir de esas declaraciones, no de un archivo de configuracion ni de un escaneo generico de clases.",
          "conceptId": "serviceloader-basic-loading",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "mod-serv-loading-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cual de estas afirmaciones sobre la iteracion de un ServiceLoader<T> es correcta?",
          "answer": "Cada implementacion se instancia de forma lazy, recien cuando la iteracion la alcanza, no todas al momento de llamar a load()",
          "distractors": ["Todas las implementaciones se instancian inmediatamente al llamar a ServiceLoader.load(), antes de iterar", "El orden de iteracion sigue el orden alfabetico del nombre de la clase implementadora", "ServiceLoader cachea las instancias entre distintas llamadas a load(), reutilizandolas siempre"],
          "explanation": "load() devuelve un ServiceLoader que aun no instancio nada; cada paso del for (o iterator().next()) es el que efectivamente crea la siguiente instancia de provider. Esto evita el costo de instanciar providers que nunca se terminan usando.",
          "conceptId": "serviceloader-basic-loading",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "mod-serv-factory-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Un provider valido necesita un constructor publico sin argumentos, o un metodo estatico publico provider() que retorne una instancia del servicio",
          "code": "// opcion 1: constructor publico sin argumentos\npublic class ServiceImpl implements Service {\n    public ServiceImpl() {}\n    public String name() { return \"impl A\"; }\n}\n\n// opcion 2: metodo estatico provider()\npublic class StaticFactoryImpl implements Service {\n    private StaticFactoryImpl() {}\n    public static Service provider() {\n        return new StaticFactoryImpl();\n    }\n    public String name() { return \"impl B\"; }\n}",
          "answer": "ok",
          "explanation": "ServiceLoader instancia cada provider llamando a su constructor publico sin argumentos, o, si el provider expone un metodo estatico publico provider() que retorna una instancia del servicio, llamando a ese metodo en su lugar (util cuando el constructor real es privado, por ejemplo para forzar un singleton). Verificado con un JDK real: ambos patrones funcionan igual desde el punto de vista de quien consume el servicio.",
          "conceptId": "provider-factory-method",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["serviceloader-basic-loading"]
        },
        {
          "id": "mod-serv-factory-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una clase que implementa un servicio tiene su unico constructor marcado como private, y no define ningun metodo estatico provider(). Que pasa si un module-info.java declara provides Service with EsaClase;?",
          "answer": "Error de compilacion: javac rechaza la clausula provides ... with porque el constructor sin argumentos de la implementacion no es publico",
          "distractors": ["Compila sin problema, ServiceLoader usa reflection para saltarse el private en tiempo de ejecucion", "Compila, pero ServiceLoader.load() lanza una excepcion en runtime al intentar instanciarla", "Compila solo si la clase tiene un metodo main(), que ServiceLoader usa como entrada alternativa"],
          "explanation": "Verificado con javac: el mensaje exacto es 'the no arguments constructor of the service implementation is not public: <Clase>', y aparece como error de COMPILACION sobre la linea del provides ... with en module-info.java -- no es un problema que aparezca recien al ejecutar.",
          "conceptId": "provider-factory-method",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["serviceloader-basic-loading"]
        },
        {
          "id": "mod-serv-factory-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que puede convenir que una implementacion de servicio use un metodo estatico provider() en vez de un constructor publico sin argumentos?",
          "answer": "Permite controlar la creacion de la instancia (por ejemplo devolver siempre el mismo singleton) sin exponer un constructor publico",
          "distractors": ["Es obligatorio: ServiceLoader nunca acepta un constructor publico sin argumentos, solo provider()", "Hace que la implementacion no necesite declararse en ningun provides ... with", "Permite que el servicio tenga mas de una implementacion activa al mismo tiempo dentro del mismo modulo"],
          "explanation": "provider() es un punto de control: la clase puede tener constructor privado y decidir internamente que instancia devolver (por ejemplo, reutilizar siempre la misma), algo que un constructor publico sin argumentos no permite expresar.",
          "conceptId": "provider-factory-method",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["serviceloader-basic-loading"]
        },
        {
          "id": "mod-serv-usesmissing-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Un modulo que llama a ServiceLoader.load(Service.class) sin declarar uses Service; compila sin problema, pero falla en tiempo de ejecucion",
          "code": "module com.example.modl {\n    requires com.example.modf;\n    // falta: uses com.example.modf.api.Service;\n}\n\n// codigo Java (compila bien):\nServiceLoader<Service> loader = ServiceLoader.load(Service.class);\nfor (Service s : loader) {\n    System.out.println(s.name());\n}",
          "answer": "ok",
          "explanation": "javac no cruza las llamadas a ServiceLoader.load() contra las declaraciones uses de module-info.java, asi que este codigo compila sin ningun error. El problema aparece recien en tiempo de ejecucion: verificado con un JDK real, ServiceLoader.load(Service.class) lanza java.util.ServiceConfigurationError con el mensaje 'Service: module com.example.modl does not declare uses' apenas se intenta usar el ServiceLoader.",
          "conceptId": "missing-uses-runtime-error",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["serviceloader-basic-loading"]
        },
        {
          "id": "mod-serv-usesmissing-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Un modulo omite la declaracion uses Service; mientras que en su codigo Java llama a ServiceLoader.load(Service.class). Que pasa al compilar y ejecutar ese modulo?",
          "answer": "Compila sin errores; falla en tiempo de ejecucion con ServiceConfigurationError al llamar a ServiceLoader.load()",
          "distractors": ["No compila: javac exige que toda llamada a ServiceLoader.load() tenga su uses correspondiente en module-info.java", "Compila y ejecuta sin problema, uses es opcional si el modulo no provee ninguna implementacion propia", "Compila, pero el ServiceLoader devuelve una lista vacia sin lanzar ninguna excepcion"],
          "explanation": "uses es una declaracion de intencion que solo el runtime del sistema de modulos verifica, no el compilador. Por eso el error no aparece hasta ejecutar: ServiceLoader.load() revisa en runtime si el modulo que llama declaro uses para ese servicio, y si no, lanza ServiceConfigurationError inmediatamente (no devuelve una lista vacia).",
          "conceptId": "missing-uses-runtime-error",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["serviceloader-basic-loading"]
        },
        {
          "id": "mod-serv-usesmissing-solo",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Completa la declaracion que falta para que ServiceLoader.load(Service.class) funcione en runtime sin lanzar ServiceConfigurationError:",
          "code": "module com.example.modl {\n    requires com.example.modf;\n    _____ com.example.modf.api.Service;\n}",
          "answer": "uses",
          "distractors": ["requires", "provides", "exports"],
          "explanation": "uses com.example.modf.api.Service; declara que este modulo consume ese servicio, habilitando a ServiceLoader.load() a buscarlo en runtime sin lanzar ServiceConfigurationError. requires ya esta declarado (necesario para ver el paquete de la interfaz Service), pero no alcanza: uses es una declaracion aparte, especifica para el mecanismo de servicios.",
          "conceptId": "missing-uses-runtime-error",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["serviceloader-basic-loading"]
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
confirm the *only* change is the insertion of the new `mod-servicios`
unit object — every byte of the `mod-declaracion` and `mod-compilacion`
units is untouched (diff shows only additions, zero deletions, except
possibly the single character changing a trailing comma at the insertion
point).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/modules-packaging.json
git commit -m "content: add Servicios en JPMS unit to Modulos y Empaquetado"
```

---

### Task 2: Add Unit D (Migracion y compatibilidad)

**Files:**
- Modify: `app/src/main/assets/content/modules-packaging.json` (append a
  fourth unit to the existing `"units"` array)

**Interfaces:**
- Consumes: the file as it exists after Task 1 — do not touch any of the
  first 3 units or their 33 exercises.
- Produces: unit `mod-migracion` (`orderIndex: 4`, `certObjective:
  "modules-packaging"`), 4 concepts: `unnamed-module-classpath`
  (pathOrder 0-2, no `dependsOn`), `automatic-modules-naming` (pathOrder
  3-5, `dependsOn: ["unnamed-module-classpath"]`),
  `split-packages-migration` (pathOrder 6-8, `dependsOn:
  ["automatic-modules-naming"]`), `add-opens-reflection` (pathOrder 9-11,
  `dependsOn: ["unnamed-module-classpath"]`).

- [ ] **Step 1: Insert the fourth unit**

In `app/src/main/assets/content/modules-packaging.json`, find this exact
trailing text (the end of the `mod-servicios` unit added in Task 1 —
copy it verbatim from the current file, do not retype from memory):

```json
          "conceptId": "missing-uses-runtime-error",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["serviceloader-basic-loading"]
        }
      ]
    }
  ]
}
```

Replace it with this exact text:

```json
          "conceptId": "missing-uses-runtime-error",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["serviceloader-basic-loading"]
        }
      ]
    },
    {
      "unitId": "mod-migracion",
      "name": "Migracion y compatibilidad",
      "certObjective": "modules-packaging",
      "orderIndex": 4,
      "summary": {
        "text": "Codigo en el classpath clasico vive en el unnamed module, que no tiene nombre y por lo tanto no puede recibirse via requires desde un modulo con nombre. Un JAR sin module-info.class puesto en el module path se convierte en automatic module: su nombre sale de Automatic-Module-Name en el manifest o se deriva del nombre del archivo, y exporta todos sus paquetes automaticamente. Dos modulos distintos que comparten un paquete Java no pueden resolverse juntos (paquete dividido); si se compilan juntos con --module-source-path el error es de compilacion, si ya vienen compilados por separado el error aparece al resolver modulos en tiempo de ejecucion. --add-opens en la linea de comandos de java habilita acceso reflexivo profundo a un paquete no exportado, sin modificar ningun module-info.java.",
        "code": "// automatic module derivado de un jar legado:\n// foo-bar-utils-2.5.jar -> foo.bar.utils@2.5 automatic\n\n// acceso reflexivo a un paquete no exportado, habilitado en runtime:\njava --add-opens com.example.modn/com.example.modn.internal=com.example.modo -p out -m com.example.modo/com.example.modo.Main"
      },
      "exercises": [
        {
          "id": "mod-migr-unnamed-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "El codigo que se ejecuta desde el classpath clasico (no desde el module path) vive en el unnamed module: no tiene nombre y no puede ser referenciado por requires desde un modulo con nombre",
          "code": "// build/lib con clases compiladas SIN module-info.java, puesta en -classpath (no -p):\njava -cp legacy.jar com.example.Main\n\n// intentar esto desde un modulo con nombre falla:\nmodule com.example.app {\n    requires com.foo.bar; // com.foo.bar esta en el classpath, no en el module path\n}",
          "answer": "ok",
          "explanation": "Cualquier clase cargada por el classloader de aplicacion desde el classpath clasico (-cp / -classpath, sin -p) pertenece al unnamed module. El unnamed module no tiene un nombre declarable, asi que ningun modulo con nombre puede hacerle requires -- no hay un identificador que poner despues de la palabra requires. Verificado con un JDK real: javac rechaza ese requires con 'module not found'.",
          "conceptId": "unnamed-module-classpath",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "mod-migr-unnamed-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Un modulo con nombre declara requires com.foo.bar;, pero com.foo.bar.jar esta puesto en el classpath clasico (-cp), no en el module path (-p). Que pasa al compilar?",
          "answer": "Error de compilacion: javac no encuentra ningun modulo llamado com.foo.bar, porque el codigo en el classpath pertenece al unnamed module, sin ese nombre",
          "distractors": ["Compila normal: javac busca automaticamente en el classpath si no encuentra el modulo en el module path", "Compila, pero falla en runtime con ClassNotFoundException", "Compila solo si el jar tambien se agrega al module path ademas del classpath"],
          "explanation": "requires necesita el nombre de un modulo real, resuelto desde el module path. El classpath clasico no produce modulos con nombre (todo cae en el unnamed module, sin nombre), asi que requires com.foo.bar; no tiene forma de resolverse: javac falla con 'module not found: com.foo.bar', sin importar que las clases existan en el classpath.",
          "conceptId": "unnamed-module-classpath",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "mod-migr-unnamed-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual de estas afirmaciones sobre el unnamed module es correcta?",
          "answer": "Agrupa todo el codigo cargado desde el classpath clasico; un modulo con nombre no puede declarar requires sobre el, porque no tiene un nombre que referenciar",
          "distractors": ["Es un modulo mas, con nombre 'unnamed', que cualquier otro modulo puede requerir escribiendo requires unnamed;", "Solo existe si la aplicacion no usa modulos en absoluto; desaparece apenas se declara un module-info.java en cualquier parte", "Reemplaza automaticamente al module path si ambos estan presentes al mismo tiempo"],
          "explanation": "El unnamed module coexiste con los modulos con nombre: el classpath clasico y el module path pueden usarse a la vez. Lo distintivo del unnamed module es que no tiene nombre para ser el destino de un requires; el propio unnamed module si puede leer todos los modulos con nombre (para dar compatibilidad hacia codigo legado), pero esa lectura no es reciproca.",
          "conceptId": "unnamed-module-classpath",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "mod-migr-auto-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un JAR sin module-info.class puesto en el module path (no en el classpath) se convierte en un automatic module: JPMS le asigna un nombre y lo trata como si exportara todos sus paquetes",
          "code": "// foo-bar-utils-2.5.jar no tiene module-info.class\n// puesto en el module path:\njava -p mods -m com.example.app/com.example.app.Main\n\n// jar --describe-module reporta:\n// foo.bar.utils@2.5 automatic",
          "answer": "ok",
          "explanation": "Verificado con un JDK real: al empaquetar codigo legado (sin module-info.java) en foo-bar-utils-2.5.jar y ponerlo en el module path, jar --describe-module reporta 'foo.bar.utils@2.5 automatic' -- guiones del nombre de archivo se convierten en puntos, y el sufijo de version (2.5) se separa como version del modulo. Un automatic module exporta todos sus paquetes automaticamente, permitiendo que codigo legado se use desde modulos con nombre sin modificarlo.",
          "conceptId": "automatic-modules-naming",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["unnamed-module-classpath"]
        },
        {
          "id": "mod-migr-auto-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Pones un JAR llamado data-utils-3.1.jar (sin module-info.class, sin atributo Automatic-Module-Name en su manifest) en el module path. Que nombre de modulo se le asigna?",
          "answer": "data.utils, derivado del nombre del archivo (guiones a puntos, sufijo de version separado)",
          "distractors": ["unnamed, el mismo nombre generico para cualquier automatic module", "El nombre del primer paquete Java que contenga el jar, sin importar el nombre del archivo", "No se le asigna ningun nombre: los automatic modules no pueden ser requeridos por nombre, solo por classpath"],
          "explanation": "Sin un module-info.class ni un atributo Automatic-Module-Name en el manifest, JPMS deriva el nombre del automatic module del nombre del archivo jar: reemplaza guiones por puntos y separa un sufijo que parezca numero de version. Verificado con un JDK real: data-utils-3.1.jar produce el modulo data.utils@3.1, y SI puede ser requerido por nombre (requires data.utils;) desde un modulo con nombre.",
          "conceptId": "automatic-modules-naming",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["unnamed-module-classpath"]
        },
        {
          "id": "mod-migr-auto-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "El manifest de un JAR legado incluye esta linea para fijar explicitamente el nombre de su automatic module en vez de dejar que se derive del nombre del archivo:",
          "code": "_____: com.foobar.custom",
          "answer": "Automatic-Module-Name",
          "distractors": ["Module-Name", "Main-Class", "Module-Version"],
          "explanation": "El atributo Automatic-Module-Name en el manifest le da a una libreria legada un nombre de modulo estable y elegido a proposito, en vez de depender del nombre del archivo jar (que puede cambiar entre versiones o distribuciones). Verificado con un JDK real: con esta linea en el manifest, jar --describe-module reporta el nombre elegido (com.foobar.custom) en vez del derivado del archivo.",
          "conceptId": "automatic-modules-naming",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["unnamed-module-classpath"]
        },
        {
          "id": "mod-migr-split-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Dos modulos separados (por ejemplo dos automatic modules) que contienen el mismo paquete Java no pueden resolverse juntos en el module path: JPMS lo rechaza en tiempo de ejecucion",
          "code": "// foo-bar-utils-2.5.jar contiene el paquete com.foo.bar\n// gadget-lib-1.0.jar TAMBIEN contiene el paquete com.foo.bar\n// ambos puestos juntos en el module path:\n\njava --module-path \"foo-bar-utils-2.5.jar:gadget-lib-1.0.jar\" ...\n\n// java.lang.module.ResolutionException:\n// Module gadget.lib contains package com.foo.bar,\n// module foo.bar.utils exports package com.foo.bar to gadget.lib",
          "answer": "ok",
          "explanation": "JPMS no permite que el mismo paquete exista en mas de un modulo resuelto simultaneamente (paquete dividido o 'split package'). Verificado con un JDK real: al poner dos jars que comparten el paquete com.foo.bar en el mismo module path y forzar su resolucion, la JVM falla al iniciar con java.lang.module.ResolutionException, nombrando ambos modulos y el paquete en conflicto.",
          "conceptId": "split-packages-migration",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["automatic-modules-naming"]
        },
        {
          "id": "mod-migr-split-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Dos JARs legados distintos, ambos convertidos en automatic modules, resultan tener el mismo paquete Java adentro. Se ponen juntos en el module path de una aplicacion que los necesita a ambos. Que pasa al iniciar la JVM?",
          "answer": "Falla al iniciar: java.lang.module.ResolutionException, porque el mismo paquete no puede existir en dos modulos resueltos a la vez",
          "distractors": ["Arranca sin problema; JPMS combina el contenido de ambos paquetes en uno solo, tomando ambos jars", "Arranca, pero solo el primero de los dos jars listado en --module-path queda activo para ese paquete", "Falla en tiempo de compilacion, antes de siquiera llegar a ejecutar java"],
          "explanation": "El conflicto de paquete dividido se detecta durante la resolucion de modulos, que ocurre al arrancar la JVM (java, no javac) -- no hay paso de compilacion involucrado cuando los jars ya vienen compilados de antes. Verificado con un JDK real: el error es java.lang.module.ResolutionException, y aparece antes de que el programa llegue a ejecutar cualquier linea de codigo.",
          "conceptId": "split-packages-migration",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["automatic-modules-naming"]
        },
        {
          "id": "mod-migr-split-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Compilas varios modulos juntos desde un unico arbol de codigo fuente con --module-source-path, y dos de ellos declaran el mismo paquete Java. Comparado con el caso de dos JARS YA COMPILADOS con el mismo paquete puestos juntos en el module path, en que momento falla cada escenario?",
          "answer": "El caso de --module-source-path falla en tiempo de compilacion (javac); el caso de los dos jars ya compilados falla en tiempo de ejecucion (al resolver modulos)",
          "distractors": ["Ambos casos fallan siempre en tiempo de compilacion, sin importar si los modulos ya estaban compilados", "Ambos casos fallan siempre en tiempo de ejecucion, incluso compilando todo junto con --module-source-path", "El caso de --module-source-path no falla nunca, solo el de jars ya compilados falla"],
          "explanation": "Es el mismo problema conceptual (paquete dividido entre modulos) pero el momento de deteccion depende de cuando el sistema de modulos ve ambos modulos juntos: si se compilan juntos con --module-source-path, javac detecta el conflicto de una vez. Si cada modulo ya viene compilado por separado (por ejemplo dos automatic modules) y recien se juntan en el module path al ejecutar, el conflicto se detecta en la resolucion de modulos al arrancar java, con java.lang.module.ResolutionException.",
          "conceptId": "split-packages-migration",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["automatic-modules-naming"]
        },
        {
          "id": "mod-migr-addopens-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "El acceso reflexivo profundo (setAccessible) a una clase de un paquete no exportado falla por defecto; --add-opens en la linea de comandos de java lo habilita sin tocar module-info.java",
          "code": "// sin --add-opens:\n// java.lang.IllegalAccessException: class com.example.modo.Main (in module com.example.modo)\n// cannot access class com.example.modn.internal.Secret (in module com.example.modn)\n// because module com.example.modn does not export com.example.modn.internal to module com.example.modo\n\n// con la flag agregada al comando java:\njava --add-opens com.example.modn/com.example.modn.internal=com.example.modo -p out -m com.example.modo/com.example.modo.Main",
          "answer": "ok",
          "explanation": "setAccessible(true) sobre un campo o constructor de una clase en un paquete no exportado falla con IllegalAccessException, aunque el modulo que reflexiona tenga requires sobre el otro modulo. --add-opens <modulo-origen>/<paquete>=<modulo-destino> en la linea de comandos de java habilita ese acceso reflexivo especificamente para el modulo destino indicado, sin que el modulo origen necesite declarar exports ni opens en su propio module-info.java. Verificado con un JDK real: el mismo codigo falla sin la flag y funciona identico con ella.",
          "conceptId": "add-opens-reflection",
          "role": "intro",
          "pathOrder": 9,
          "dependsOn": ["unnamed-module-classpath"]
        },
        {
          "id": "mod-migr-addopens-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Un modulo usa reflection (Class.forName + setAccessible) para acceder a una clase de un paquete que el modulo destino NO exporta. Sin ninguna flag adicional, que pasa al ejecutar?",
          "answer": "Falla en runtime con IllegalAccessException: el paquete no esta exportado al modulo que intenta el acceso reflexivo",
          "distractors": ["Funciona sin problema: setAccessible(true) siempre puede saltarse las reglas de modulos, exportado o no", "No compila, javac detecta el uso de reflection sobre un paquete no exportado", "Funciona solo la primera vez que se ejecuta, y falla en ejecuciones posteriores"],
          "explanation": "JPMS aplica encapsulamiento fuerte incluso frente a reflection: setAccessible(true) ya no basta por si solo si el paquete no fue exportado (ni abierto con opens) al modulo que reflexiona. El error es de runtime (no de compilacion, porque javac no analiza el argumento String de Class.forName), y es exactamente IllegalAccessException con el modulo y paquete en conflicto nombrados en el mensaje.",
          "conceptId": "add-opens-reflection",
          "role": "guided",
          "pathOrder": 10,
          "dependsOn": ["unnamed-module-classpath"]
        },
        {
          "id": "mod-migr-addopens-solo",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Completa la flag de linea de comandos que habilita acceso reflexivo profundo del modulo com.example.modo al paquete com.example.modn.internal del modulo com.example.modn, sin modificar ningun module-info.java:",
          "code": "java _____ com.example.modn/com.example.modn.internal=com.example.modo -p out -m com.example.modo/com.example.modo.Main",
          "answer": "--add-opens",
          "distractors": ["--add-exports", "--add-reads", "--add-modules"],
          "explanation": "--add-opens <modulo>/<paquete>=<modulo-destino> es especificamente para habilitar acceso reflexivo profundo (setAccessible) sin tocar el codigo fuente ni el module-info.java del modulo origen -- es la herramienta pensada para migrar codigo legado que depende de reflection sobre APIs internas, sin poder (o querer) modificar el modulo que las contiene.",
          "conceptId": "add-opens-reflection",
          "role": "solo",
          "pathOrder": 11,
          "dependsOn": ["unnamed-module-classpath"]
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
confirm the *only* change is the insertion of the new `mod-migracion`
unit object.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/modules-packaging.json
git commit -m "content: add Migracion y compatibilidad unit to Modulos y Empaquetado"
```

---

### Task 3: Validate the whole file and bump content version

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Read-only validation: `app/src/main/assets/content/modules-packaging.json`

**Interfaces:**
- Consumes: the final state of `modules-packaging.json` after Task 2 (4
  units, 45 exercises total).
- Produces: `CURRENT_CONTENT_VERSION` bumped by one from whatever it is
  at dispatch time (check `ContentSeeder.kt`'s current value first — do
  not assume a specific number; it was `"18"` at plan-writing time, but
  other cycles may have landed between this plan being written and
  executed). No `ContentPackRegistry` change needed — the section is
  already registered from sub-cycle 1.

- [ ] **Step 1: Write and run a full validation script**

```bash
python3 << 'EOF'
import json, re

path = "app/src/main/assets/content/modules-packaging.json"
data = json.load(open(path))

assert data["sectionId"] == "java-modules-packaging"
assert data["orderIndex"] == 6
assert data["examVersion"] == "core"

all_units = {u["unitId"]: u for u in data["units"]}
expected_units = {"mod-declaracion", "mod-compilacion", "mod-servicios", "mod-migracion"}
assert set(all_units.keys()) == expected_units, f"unexpected units: {list(all_units.keys())}"

expected_counts = {"mod-declaracion": 15, "mod-compilacion": 9, "mod-servicios": 9, "mod-migracion": 12}
expected_order = {"mod-declaracion": 1, "mod-compilacion": 2, "mod-servicios": 3, "mod-migracion": 4}
all_exercises = []
for uid, expected in expected_counts.items():
    unit = all_units[uid]
    exercises = unit["exercises"]
    assert len(exercises) == expected, f"{uid}: expected {expected} exercises, got {len(exercises)}"
    assert unit["certObjective"] == "modules-packaging", f"{uid}: unexpected certObjective {unit['certObjective']}"
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
print(f"Total exercises across all 4 units: {total}")
assert total == 45
EOF
```

Expected: `Unit counts, certObjective, and orderIndex all correct.` then
`One-terminal-role, dependsOn, sequential pathOrder, and difficulty-
monotonicity rules all passed.` then `No accented characters, no
predict_output.` then four reachability lines (one per unit), then
`Total exercises across all 4 units: 45`, no assertion errors.

- [ ] **Step 2: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find the current `CURRENT_CONTENT_VERSION` value and increment it by one
(check the file first — do not assume a specific starting number).

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still passing. No test
changes are needed for this task — confirm during review that
`ContentMapperTest.kt` and `ContentPackParsingTest.kt` (which use inline
fixtures, not the real asset files) are unaffected.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: bump content version for Servicios en JPMS and Migracion y compatibilidad units"
```

---

## After the plan: manual on-device QA

Install a clean/in-place build and manually verify on-device — per the
lesson from sub-cycle 1, answer exercises by playing manually rather than
via `adb input tap` automation (taps proved unreliable, intermittently
landing on the wrong option or not registering); reserve `adb` for
navigation/screenshots only.

1. Confirm "Servicios en JPMS" and "Migracion y compatibilidad" appear as
   the 3rd and 4th units of "Modulos y Empaquetado", after "Compilacion y
   ejecucion", gated consistently with the app's existing unit-gating
   rules.
2. Play through "Servicios en JPMS": confirm `provider-factory-method`
   and `missing-uses-runtime-error` both unlock as soon as
   `serviceloader-basic-loading`'s solo exercise is answered (they share
   the same `dependsOn`, so both should become reachable together, not
   sequentially).
3. Play through "Migracion y compatibilidad": confirm
   `automatic-modules-naming` and `add-opens-reflection` both unlock
   together once `unnamed-module-classpath`'s solo exercise is answered
   (same shared-`dependsOn` shape), and that `split-packages-migration`
   stays locked until `automatic-modules-naming`'s solo exercise is
   answered.
4. Confirm all `worked_example` intro cards render their multi-line code
   blocks without truncation, including the two-module-declaration
   snippet in `mod-serv-loading-intro`'s summary and the 3-line error
   message quoted in `mod-migr-split-intro`.
5. Confirm the 3 `fill_blank` exercises (`mod-serv-usesmissing-solo`,
   `mod-migr-auto-solo`, `mod-migr-addopens-solo`) render correctly with
   the keyboard open, no regression of the keyboard-layout fix.
6. Play through both units fully and confirm the whole section (45
   exercises across all 4 units) can be completed end to end without
   getting stranded.
