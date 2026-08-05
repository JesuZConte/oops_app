# Streams y Lambdas Sub-ciclo 1: Interfaces Funcionales Estandar + Optional Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the two remaining gaps in the "Streams y lambdas" section
identified in `docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`: the
built-in functional interfaces of `java.util.function`
(`Predicate`/`Consumer`/`Function`/`Supplier` + method references) and
`Optional`. Lambda syntax/SAM/functional-interface fundamentals are
already covered by the "Interfaces funcionales y lambdas" unit shipped in
Fundamentos sub-cycle 3 — this sub-cycle does not duplicate that content.

**Architecture:** Pure content addition — two new units appended to
`app/src/main/assets/content/streams.json`
(`streams-functional-interfaces` at `orderIndex: 5`, `streams-optional`
at `orderIndex: 6`), each with a full first-exposure ladder
(`worked_example` intro -> `guided` -> `solo`) per concept plus one
open-ended `mcq` "interview" question, following the exact pattern
already used by the `streams-collectors` unit in the same file. No
existing exercises are touched (no retrofit needed — none of the 4
existing Streams units contain any functional-interfaces-beyond-lambdas
or `Optional` content). No Kotlin/Compose code changes are needed beyond
bumping the content seed version.

**Tech Stack:** Kotlin, JUnit4, JSON content packs loaded generically by
`ContentLoader`/`ContentSeeder`.

## Global Constraints

- **Case-collision rule:** for `mcq`/`fill_blank` exercises, no distractor
  may differ from the answer only by capitalization — grading is
  case-insensitive.
- **One-terminal-role rule:** every `conceptId` introduced in this plan
  must have exactly one exercise with `role: "solo"` (or `"practice"`) —
  never zero, never more than one.
- **Ladder ordering:** within each unit, `pathOrder` must be assigned
  sequentially across ALL exercises (laddered and interview) starting at
  0, with no gaps and no duplicates, matching the existing
  `streams-collectors` unit's pattern.
- **`dependsOn` correctness:** a concept whose ladder should only unlock
  after a prior concept is born must declare `dependsOn: ["<prior
  conceptId>"]` on every one of its exercises (intro, guided, solo alike).
- Do not modify any existing exercise, unit, or field in
  `app/src/main/assets/content/streams.json` — this is an append-only
  change.
- Valid JSON only: no trailing commas, exact key names matching sibling
  exercises in the same file (`id`, `type`, `difficulty`, `prompt`,
  `code`, `answer`, `distractors`, `explanation`, `conceptId`, `role`,
  `pathOrder`, `dependsOn`).

---

### Task 1: Add the "Interfaces funcionales estandar" unit

**Files:**
- Modify: `app/src/main/assets/content/streams.json`

**Interfaces:**
- Consumes: nothing — pure JSON append.
- Produces: unit `streams-functional-interfaces` with concepts
  `predicate-consumer`, `function-supplier`, `method-references`, which
  Task 2's `streams-optional` unit does NOT depend on (the two units are
  independent).

- [ ] **Step 1: Locate the insertion point**

Open `app/src/main/assets/content/streams.json`. Find the end of the
`streams-collectors` unit — it is the last unit in the `units` array. Its
final exercise is `streams-parsons-02`, and the unit's own closing looks
like this (currently around line 389-391):

```json
        }
      ]
    }
  ]
}
```

The outer structure is: `streams-parsons-02`'s closing `}`, then `]`
closing that unit's `exercises` array, then `}` closing the
`streams-collectors` unit object, then `]` closing the top-level `units`
array, then `}` closing the whole file.

- [ ] **Step 2: Insert the new unit**

Change:

```json
        }
      ]
    }
  ]
}
```

To (adding a comma after the `streams-collectors` unit's closing `}`,
then the new unit, before the `units` array's closing `]`):

```json
        }
      ]
    },
    {
      "unitId": "streams-functional-interfaces",
      "name": "Interfaces funcionales estandar",
      "certObjective": "streams-lambdas",
      "orderIndex": 5,
      "summary": {
        "text": "java.util.function define interfaces funcionales listas para usar, en vez de crear las tuyas propias para cada caso comun. Predicate<T> representa una condicion (test(T): boolean), Consumer<T> representa una accion sobre un valor sin devolver nada (accept(T): void), Function<T,R> transforma un valor de un tipo a otro (apply(T): R), y Supplier<T> provee un valor sin recibir argumentos (get(): T). Method references (Clase::metodo) son una forma mas corta de escribir una lambda que solo llama a un metodo existente.",
        "code": "Predicate<String> esVacio = String::isEmpty;\nConsumer<String> imprimir = System.out::println;\nFunction<String, Integer> largo = String::length;\nSupplier<String> saludo = () -> \"Hola\";"
      },
      "exercises": [
        {
          "id": "streams-predicate-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Predicate<T> representa una condicion; Consumer<T> representa una accion sin devolver valor",
          "code": "Predicate<Integer> esPar = n -> n % 2 == 0;\nesPar.test(4); // true\n\nConsumer<String> imprimir = s -> System.out.println(s);\nimprimir.accept(\"Hola\"); // imprime Hola",
          "answer": "ok",
          "explanation": "Predicate<T> tiene un unico metodo abstracto test(T): boolean, evalua una condicion. Consumer<T> tiene accept(T): void, ejecuta una accion sobre el valor, sin devolver nada. Ambas son interfaces funcionales de java.util.function, listas para usar sin declararlas vos mismo.",
          "conceptId": "predicate-consumer",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "streams-predicate-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual es la diferencia principal entre Predicate<T> y Consumer<T>?",
          "answer": "Predicate devuelve un boolean (evalua una condicion); Consumer no devuelve nada (ejecuta una accion)",
          "distractors": ["Predicate recibe dos argumentos y Consumer recibe uno", "No hay diferencia real, son la misma interfaz con otro nombre", "Consumer solo funciona con Strings"],
          "explanation": "test(T): boolean vs accept(T): void es la diferencia central. Predicate se usa para filtrar/evaluar, Consumer para actuar sobre un valor (como imprimir o guardar).",
          "conceptId": "predicate-consumer",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "streams-predicate-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Predicate<Integer> esMayorDeCinco = n -> n > 5;\nSystem.out.println(esMayorDeCinco.test(3));\nSystem.out.println(esMayorDeCinco.test(8));",
          "answer": "false\ntrue",
          "explanation": "test(3) evalua 3 > 5, que es false; test(8) evalua 8 > 5, que es true.",
          "conceptId": "predicate-consumer",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "streams-function-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Function<T,R> transforma un valor de tipo T a tipo R; Supplier<T> provee un valor sin recibir nada",
          "code": "Function<String, Integer> largo = s -> s.length();\nlargo.apply(\"hola\"); // 4\n\nSupplier<Double> aleatorio = () -> Math.random();\naleatorio.get(); // un valor random",
          "answer": "ok",
          "explanation": "Function<T,R> tiene apply(T): R, recibe un valor y devuelve otro, posiblemente de otro tipo. Supplier<T> tiene get(): T, no recibe nada, solo provee un valor (util para valores calculados de forma perezosa).",
          "conceptId": "function-supplier",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["predicate-consumer"]
        },
        {
          "id": "streams-function-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que interfaz usarias para representar 'una operacion que no recibe argumentos pero devuelve un valor'?",
          "answer": "Supplier<T>",
          "distractors": ["Function<T,R>", "Consumer<T>", "Predicate<T>"],
          "explanation": "Supplier<T> es la unica de las cuatro interfaces basicas que no recibe ningun argumento. Su unico metodo, get(), solo produce un valor.",
          "conceptId": "function-supplier",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["predicate-consumer"]
        },
        {
          "id": "streams-function-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Function<Integer, Integer> alCuadrado = n -> n * n;\nSystem.out.println(alCuadrado.apply(5));",
          "answer": "25",
          "explanation": "apply(5) ejecuta la lambda con n=5, devolviendo 5 * 5 = 25.",
          "conceptId": "function-supplier",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["predicate-consumer"]
        },
        {
          "id": "streams-methodref-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un method reference (Clase::metodo) es una forma mas corta de escribir una lambda que solo llama a un metodo existente",
          "code": "Function<String, Integer> largo1 = s -> s.length();     // lambda\nFunction<String, Integer> largo2 = String::length;      // method reference, equivalente",
          "answer": "ok",
          "explanation": "String::length es equivalente a s -> s.length(). Java infiere que el primer parametro de la lambda se convierte en el receptor de la llamada al metodo. Es puramente sintaxis mas corta, el comportamiento es identico.",
          "conceptId": "method-references",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["function-supplier"]
        },
        {
          "id": "streams-methodref-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cuando conviene usar un method reference en vez de una lambda equivalente?",
          "answer": "Cuando la lambda solo llama a un metodo existente sin logica adicional - el method reference es mas legible en ese caso",
          "distractors": ["Siempre, method reference es obligatorio desde Java 8", "Nunca, las lambdas son mas rapidas en tiempo de ejecucion", "Solo cuando el metodo es static"],
          "explanation": "Method references (static, de instancia, o de constructor) son azucar sintactico para lambdas que se limitan a invocar un metodo existente. Si la lambda tiene logica adicional (varias lineas, condicionales), no se puede reemplazar por un method reference.",
          "conceptId": "method-references",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["function-supplier"]
        },
        {
          "id": "streams-methodref-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el method reference equivalente a 's -> System.out.println(s)':",
          "code": "Consumer<String> imprimir = _____;",
          "answer": "System.out::println",
          "distractors": ["System.out.println", "System::out::println", "println::System.out"],
          "explanation": "System.out::println referencia el metodo println() de instancia sobre el objeto System.out. La sintaxis es objeto::metodo para method references de instancia sobre un objeto concreto.",
          "conceptId": "method-references",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["function-supplier"]
        },
        {
          "id": "streams-functional-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una consultora IT grande te pregunta: por que usar Predicate/Function/Consumer/Supplier en vez de crear tus propias interfaces funcionales para cada caso?",
          "answer": "Porque son parte de la biblioteca estandar (java.util.function), evitan reinventar lo mismo, y son reconocidas por cualquier desarrollador Java sin explicacion adicional",
          "distractors": ["Porque Java no permite crear interfaces funcionales propias", "Porque las interfaces propias no pueden usarse con lambdas", "No hay ninguna ventaja real, es solo preferencia personal"],
          "explanation": "Reutilizar las interfaces estandar de java.util.function mejora la legibilidad y reduce codigo repetido. Crear una interfaz propia solo tiene sentido cuando el caso no encaja en ninguna variante estandar (BiFunction, UnaryOperator, etc.) o cuando el nombre del metodo importa para la legibilidad del dominio.",
          "pathOrder": 9
        }
      ]
    }
  ]
}
```

Note that the interview exercise (`streams-functional-interview`) has no
`conceptId`/`role`/`dependsOn` — it is a plain, always-available `mcq`,
matching the pattern of open-ended questions elsewhere in the corpus.

- [ ] **Step 3: Validate JSON syntax**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/streams.json'))"`
Expected: no output, exit code 0 (valid JSON).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/content/streams.json
git commit -m "content: add Interfaces funcionales estandar unit to Streams y lambdas"
```

---

### Task 2: Add the "Optional" unit

**Files:**
- Modify: `app/src/main/assets/content/streams.json`

**Interfaces:**
- Consumes: nothing — independent of Task 1's unit (no shared
  `conceptId`, no cross-unit `dependsOn`).
- Produces: unit `streams-optional` with concepts `optional-creacion`,
  `optional-orElse`, `optional-map`.

- [ ] **Step 1: Locate the insertion point**

After Task 1, the `streams-functional-interfaces` unit is now the last
unit in the `units` array. Find its closing:

```json
        }
      ]
    }
  ]
}
```

(This is the closing of `streams-functional-interview`'s `}`, then `]`
closing that unit's `exercises`, then `}` closing the
`streams-functional-interfaces` unit, then `]` closing `units`, then `}`
closing the file.)

- [ ] **Step 2: Insert the new unit**

Change:

```json
        }
      ]
    }
  ]
}
```

To:

```json
        }
      ]
    },
    {
      "unitId": "streams-optional",
      "name": "Optional",
      "certObjective": "streams-lambdas",
      "orderIndex": 6,
      "summary": {
        "text": "Optional<T> envuelve un valor que puede o no estar presente, para evitar NullPointerException y hacer explicito en la firma de un metodo que el resultado puede faltar. Se crea con Optional.of() (valor no nulo garantizado), Optional.ofNullable() (puede ser null), o Optional.empty(). isPresent()/isEmpty() chequean si hay valor; orElse()/orElseGet()/orElseThrow() dan una alternativa si no lo hay; map() transforma el valor solo si esta presente, sin necesitar un if explicito.",
        "code": "Optional<String> nombre = Optional.ofNullable(buscarNombre());\n\nif (nombre.isPresent()) {\n    System.out.println(nombre.get());\n}\n\nString resultado = nombre.orElse(\"Desconocido\");"
      },
      "exercises": [
        {
          "id": "streams-optcreate-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Optional envuelve un valor que puede o no estar presente",
          "code": "Optional<String> conValor = Optional.of(\"Hola\");       // nunca null, o lanza NPE\nOptional<String> puedeSerNull = Optional.ofNullable(null); // OK, queda vacio\nOptional<String> vacio = Optional.empty();\n\nconValor.isPresent(); // true\nvacio.isPresent();    // false",
          "answer": "ok",
          "explanation": "Optional.of() exige un valor no nulo (lanza NullPointerException si le pasas null). Optional.ofNullable() acepta null y crea un Optional vacio en ese caso. Optional.empty() crea directamente un Optional sin valor.",
          "conceptId": "optional-creacion",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "streams-optcreate-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que pasa si llamas a Optional.of(null)?",
          "answer": "Se lanza NullPointerException inmediatamente",
          "distractors": ["Se crea un Optional vacio, igual que ofNullable", "Compila pero falla recien al llamar a get()", "Devuelve null directamente, sin envolver nada"],
          "explanation": "Optional.of() esta pensado para casos donde el valor NUNCA deberia ser null. Si lo es, es un error de programacion, y Optional.of() lo detecta inmediatamente en vez de esconderlo. Para valores que legitimamente pueden ser null, se usa ofNullable().",
          "conceptId": "optional-creacion",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "streams-optcreate-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Optional<String> valor = Optional.ofNullable(null);\nSystem.out.println(valor.isPresent());\nSystem.out.println(valor.isEmpty());",
          "answer": "false\ntrue",
          "explanation": "ofNullable(null) crea un Optional vacio; isPresent() da false (no hay valor) e isEmpty() da true (esta vacio) - son metodos complementarios, isEmpty() se agrego en Java 11.",
          "conceptId": "optional-creacion",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "streams-optorelse-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "orElse()/orElseGet()/orElseThrow() dan una alternativa cuando el Optional esta vacio",
          "code": "Optional<String> nombre = Optional.empty();\n\nString a = nombre.orElse(\"Desconocido\");           // valor por defecto fijo\nString b = nombre.orElseGet(() -> calcularDefault()); // valor calculado perezosamente\nnombre.orElseThrow(() -> new RuntimeException(\"Sin nombre\")); // lanza si esta vacio",
          "answer": "ok",
          "explanation": "orElse() siempre evalua su argumento (aunque el Optional tenga valor). orElseGet() solo ejecuta el Supplier si esta vacio (mas eficiente si calcular el default es costoso). orElseThrow() lanza la excepcion provista en vez de devolver un valor por defecto.",
          "conceptId": "optional-orElse",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["optional-creacion"]
        },
        {
          "id": "streams-optorelse-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que orElseGet() puede ser mas eficiente que orElse() en algunos casos?",
          "answer": "Porque orElseGet() solo ejecuta su Supplier si el Optional esta vacio; orElse() siempre evalua su argumento, tenga valor o no",
          "distractors": ["No hay diferencia real de eficiencia entre ambos", "orElseGet() es mas lento porque usa una interfaz funcional", "orElse() nunca se ejecuta si el Optional tiene valor"],
          "explanation": "Si el argumento de orElse() es una llamada costosa (por ejemplo, una consulta a base de datos), esa llamada se ejecuta SIEMPRE, incluso cuando no hace falta. orElseGet() evita ese costo evaluando el Supplier solo cuando realmente esta vacio.",
          "conceptId": "optional-orElse",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["optional-creacion"]
        },
        {
          "id": "streams-optorelse-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Optional<String> nombre = Optional.of(\"Ana\");\nString resultado = nombre.orElse(\"Desconocido\");\nSystem.out.println(resultado);",
          "answer": "Ana",
          "explanation": "Como el Optional tiene un valor presente (Ana), orElse() devuelve ese valor directamente, ignorando el argumento por defecto.",
          "conceptId": "optional-orElse",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["optional-creacion"]
        },
        {
          "id": "streams-optmap-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "map() transforma el valor de un Optional solo si esta presente, sin necesitar un if explicito",
          "code": "Optional<String> nombre = Optional.of(\"ana\");\nOptional<Integer> largo = nombre.map(String::length); // Optional[3]\n\nOptional<String> vacio = Optional.empty();\nOptional<Integer> largoVacio = vacio.map(String::length); // Optional vacio, no lanza nada",
          "answer": "ok",
          "explanation": "map() aplica la funcion solo si hay valor presente. Si el Optional esta vacio, map() devuelve otro Optional vacio sin ejecutar la funcion, evitando tener que chequear isPresent() manualmente antes de transformar.",
          "conceptId": "optional-map",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["optional-creacion"]
        },
        {
          "id": "streams-optmap-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que pasa si llamas a map() sobre un Optional vacio?",
          "answer": "Devuelve otro Optional vacio, sin ejecutar la funcion de transformacion",
          "distractors": ["Lanza NoSuchElementException", "Ejecuta la funcion igual, pasandole null", "Devuelve Optional.of(null)"],
          "explanation": "map() esta disenado para encadenar transformaciones sin romper con un Optional vacio. Si no hay valor, simplemente propaga el vacio hacia el resultado, sin intentar ejecutar la funcion.",
          "conceptId": "optional-map",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["optional-creacion"]
        },
        {
          "id": "streams-optmap-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Optional<String> nombre = Optional.of(\"maria\");\nint largo = nombre.map(String::length).orElse(0);\nSystem.out.println(largo);",
          "answer": "5",
          "explanation": "map(String::length) transforma \"maria\" (5 caracteres) en Optional[5]. orElse(0) devuelve ese valor presente, 5, sin usar el default.",
          "conceptId": "optional-map",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["optional-creacion"]
        },
        {
          "id": "streams-optional-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una empresa de servicios financieros te pregunta: que problema real resuelve Optional que no se podia resolver ya con un chequeo de null tradicional?",
          "answer": "Hace explicito en la firma del metodo que el resultado puede faltar, obligando al que llama a manejarlo - un chequeo de null es facil de olvidar porque el compilador no lo exige",
          "distractors": ["Optional hace que el codigo corra mas rapido que un if de null", "Optional elimina completamente la posibilidad de NullPointerException en toda la aplicacion", "No resuelve ningun problema nuevo, es solo una forma distinta de escribir lo mismo"],
          "explanation": "Un metodo que devuelve String puede devolver null sin que nada lo indique en la firma, y es facil olvidar el chequeo. Un metodo que devuelve Optional<String> comunica explicitamente 'esto puede no tener valor', y el equipo puede exigir que se maneje. Optional no elimina NPE magicamente (todavia se puede hacer optional.get() sin chequear y fallar), pero hace el riesgo visible.",
          "pathOrder": 9
        }
      ]
    }
  ]
}
```

- [ ] **Step 3: Validate JSON syntax**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/streams.json'))"`
Expected: no output, exit code 0 (valid JSON).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/content/streams.json
git commit -m "content: add Optional unit to Streams y lambdas"
```

---

### Task 3: Validate the whole corpus and bump the content seed version

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: `app/src/main/assets/content/streams.json` (as modified by
  Tasks 1-2) plus every other content pack file already registered in
  `ContentPackRegistry`.
- Produces: `CURRENT_CONTENT_VERSION = "13"`, which forces
  `ContentSeeder.seedIfNeeded()` to re-seed the Room database on next app
  launch (existing installs pick up the two new units).

- [ ] **Step 1: Run the reachability/consistency validation script**

This validates every hard content rule established across this project's
prior sub-cycles against the FULL `streams.json` corpus (not just the two
new units): no case-collisions between any answer and its distractors, no
concept with zero or more-than-one terminal (`solo`/`practice`) exercise,
and no `dependsOn` referencing a `conceptId` that doesn't exist anywhere
in the file.

Run:

```bash
python3 - <<'PYEOF'
import json
from collections import defaultdict

d = json.load(open('app/src/main/assets/content/streams.json'))

unit_ids = [u['unitId'] for u in d['units']]
ex_ids = [e['id'] for u in d['units'] for e in u['exercises']]
assert len(unit_ids) == len(set(unit_ids)), 'duplicate unitId'
assert len(ex_ids) == len(set(ex_ids)), 'duplicate exercise id'
print('OK:', len(unit_ids), 'units,', len(ex_ids), 'exercises')

bad = []
for u in d['units']:
    for e in u['exercises']:
        if e.get('type') in ('mcq', 'fill_blank') and 'distractors' in e:
            ans = e['answer'].strip().lower()
            for dist in e['distractors']:
                if dist.strip().lower() == ans:
                    bad.append((e['id'], dist))
print('case-collisions (must be empty):', bad)
assert not bad

roles = defaultdict(set)
terms = defaultdict(int)
for u in d['units']:
    for e in u['exercises']:
        if e.get('conceptId'):
            roles[e['conceptId']].add(e.get('role'))
            if e.get('role') in ('solo', 'practice'):
                terms[e['conceptId']] += 1

zero_terminal = [c for c, r in roles.items() if not (r & {'solo', 'practice'})]
multi_terminal = {c: n for c, n in terms.items() if n > 1}
print('zero-terminal concepts (must be empty):', zero_terminal)
print('multi-terminal concepts (must be empty):', multi_terminal)
assert not zero_terminal
assert not multi_terminal

all_concepts = set(roles.keys())
dangling = []
for u in d['units']:
    for e in u['exercises']:
        for dep in e.get('dependsOn', []):
            if dep not in all_concepts:
                dangling.append((e['id'], dep))
print('dangling dependsOn (must be empty):', dangling)
assert not dangling

print('ALL CHECKS PASSED')
PYEOF
```

Expected: `ALL CHECKS PASSED` printed at the end, all listed sets empty.
If any assertion fails, stop and report — do not proceed to Step 2 with a
failing corpus.

- [ ] **Step 2: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find:

```kotlin
private const val CURRENT_CONTENT_VERSION = "12"
```

Replace with:

```kotlin
private const val CURRENT_CONTENT_VERSION = "13"
```

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests pass unchanged (this task
does not modify any use case or Kotlin logic, only content data and the
version bump).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "chore: bump content seed version so the Streams functional-interfaces/Optional units reach existing installs"
```

---

## After the task: manual on-device QA

Install a clean/in-place build and manually verify on-device:

1. Reopen Ruta, navigate to "Streams y lambdas": confirm two new units
   appear after "Collectors avanzados" — "Interfaces funcionales
   estandar" and "Optional".
2. Play "Interfaces funcionales estandar" end to end: confirm each
   concept's ladder appears in order (intro shown non-graded and
   auto-advances, then guided, then solo), and the interview question
   appears among the session's exercises.
3. Play "Optional" end to end: same ladder-order check for its 3
   concepts.
4. Confirm no existing Streams content (Creacion de streams, Operaciones
   intermedias, Operaciones terminales, Collectors avanzados) changed
   behavior or appearance.
5. Confirm the checkpoint-satisfied-permanence fix still holds: if
   "Streams y lambdas" had an already-approved checkpoint from a prior
   QA session, confirm it still shows satisfied despite the 2 new units
   (this exercises the fix from
   `docs/superpowers/plans/2026-08-05-checkpoint-satisfied-permanent-fix.md`
   under real new-content growth again).
6. If any prior on-device QA install completed "Streams y lambdas" via
   placement only (no approved review checkpoint), confirm that install
   now shows "Manejo de Excepciones" and "Concurrencia" units re-locked
   until the 2 new units here are played — this is an accepted,
   code-documented consequence of adding content to an already-completed
   section (see the comment on `checkpointSatisfied` in
   `GetLearningPathUseCase.kt`), not a regression. If the checkpoint was
   an approved review attempt instead, it stays permanently unlocked per
   the checkpoint-satisfied-permanence fix (see step 5).
