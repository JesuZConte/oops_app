# Concurrencia Retrofit Sub-cycle 2 (Sincronizacion) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retrofit `app/src/main/assets/content/concurrency.json`'s
`conc-sincronizacion` unit with full first-exposure ladder metadata
(`conceptId`/`role`/`pathOrder`/`dependsOn`), and close the section's last
remaining real exam-objective gap identified in
`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`: **Semaphore and
ReadWriteLock** (advanced synchronization primitives beyond
`synchronized`/`ReentrantLock`). **This closes the Concurrencia section
and the entire 5-cycle ladder-retrofit series** started with Fundamentos
de Java.

**Architecture:** Pure content-authoring in one JSON asset file — zero
Kotlin/Compose changes beyond bumping `CURRENT_CONTENT_VERSION` in
`ContentSeeder.kt`. Every pre-existing exercise is grandfathered: only
`conceptId`/`role`/`pathOrder`/`dependsOn` are added to it, and the 7
protected fields (`id`, `type`, `prompt`, `code`, `answer`, `distractors`,
`explanation`) stay byte-identical to preserve each exercise's real
`review_state` (SM-2 spaced-repetition history), keyed by exercise id.
New exercises are added for two new concepts (`semaphore`,
`readwrite-lock`), plus new `intro` rungs for 3 existing concepts that
only had 1-2 existing exercises to build a ladder around.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell (unchanged
this cycle), JUnit4 for the existing use-case test suite (must stay green
— no test changes needed since no Kotlin code changes).

## Global Constraints

- **Grandfathering rule:** for every pre-existing exercise (id starts with
  `conc-sync-`), the 7 fields `id`, `type`, `prompt`, `code` (where
  present), `answer`, `distractors` (where present), `explanation` must
  remain byte-identical to the current file. Only `conceptId`, `role`,
  `pathOrder`, and (where applicable) `dependsOn` may be added. Never
  touch these fields' values, whitespace, or order.
- **Literal text insertion only — never JSON load+dump.** A JSON
  library load+dump reformats every other array in the file (single-line
  arrays become multi-line) even though no values change, which has
  caused real regressions in this project before. Replace the unit's
  entire `"exercises": [...]` array (given verbatim below) using a text
  edit (e.g. the Edit tool's exact-string replacement), never a JSON
  parse-and-rewrite. Do not run the file through `json.load()` +
  `json.dump()` for editing purposes (reading it with `json.load()` for
  read-only validation in Task 2 is fine).
- **One-terminal-role rule:** every `conceptId` has exactly one exercise
  with `role: "solo"` or `role: "practice"` — never zero, never two.
- **dependsOn same-unit-only rule:** every id listed in a `dependsOn` array
  must be a `conceptId` that exists elsewhere in the *same* unit's
  `exercises` array. Cross-unit `dependsOn` is silently unsatisfiable (the
  session-selection algorithm computes `bornConceptIds` per-unit) and must
  never be used.
- **Sequential pathOrder rule:** within the unit, `pathOrder` values across
  all exercises (existing + new) must be exactly `0, 1, 2, ..., 15` with
  no gaps and no duplicates, in the same order as the array's physical
  layout (the app sorts by `pathOrder` at runtime regardless of array
  order, but keep them aligned for human readability, matching every
  other unit in this corpus).
- **Case-collision rule:** no `mcq`/`fill_blank` exercise's `distractors`
  may differ from its own `answer` only by letter case.
- **No accents in Spanish content** (project-wide convention, verified with
  `LC_ALL=C grep -nP "[\x80-\xFF]"` returning empty on the touched file).
  No voseo — tuteo only ("Tienes", "Quieres", never "Tenes"/"Queres").
- **worked_example format:** every `intro`-role exercise has
  `"type": "worked_example"`, `"answer": "ok"`, no `distractors` field, and
  a `code` field showing the illustrative snippet.
- **Runtime-observable content must be verified against a real JDK, not
  just read.** Every factual claim about `Semaphore.acquire()`/
  `tryAcquire()` blocking behavior and `ReadWriteLock` read-shared/
  write-exclusive semantics in this plan has already been executed
  against a local JDK 20 during design and confirmed correct — do not
  re-derive these from documentation alone if you need to modify them;
  re-run them.
- **Never use `predict_output` for content whose result depends on thread
  interleaving/scheduling non-determinism.** None of the new exercises in
  this plan use `predict_output` — all are `mcq`/`worked_example`,
  intentionally, since Semaphore/ReadWriteLock behavior under real
  concurrency is timing-dependent and unsuitable for exact-output
  grading. If any future edit adds a `predict_output` here, it must not
  depend on thread scheduling.

---

### Task 1: Retrofit `conc-sincronizacion` + close the Semaphore/ReadWriteLock gap

**Files:**
- Modify: `app/src/main/assets/content/concurrency.json` (the
  `conc-sincronizacion` unit's `exercises` array only, currently lines
  348-405 in the file)

**Interfaces:**
- Produces: concepts `race-conditions` (pathOrder 0-2, no dependsOn),
  `synchronized-basics` (pathOrder 3-6, `dependsOn: ["race-conditions"]`),
  `reentrant-lock-basics` (pathOrder 7-9, `dependsOn:
  ["synchronized-basics"]`), `semaphore` (pathOrder 10-12, `dependsOn:
  ["reentrant-lock-basics"]`), `readwrite-lock` (pathOrder 13-15,
  `dependsOn: ["semaphore"]`).
- Consumes: nothing from other units (this is the only unit touched in
  this sub-cycle; `conc-threads-lifecycle`, `conc-executors`,
  `conc-virtual-threads` were already retrofitted in sub-cycle 1 and must
  not be touched here).

- [ ] **Step 1: Replace the unit's `exercises` array**

In `app/src/main/assets/content/concurrency.json`, find the
`conc-sincronizacion` unit's `"exercises": [ ... ]` array (currently 6
exercises, ids `conc-sync-01` through `conc-sync-06` — copy them verbatim
from the current file, do not retype from memory). Replace the entire
array (opening `[` through closing `]`) with this exact final version —
16 exercises, 10 new plus the 6 existing exercises verbatim with ladder
fields added:

```json
      "exercises": [
        {
          "id": "race-conditions-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Una race condition ocurre cuando varios hilos leen y modifican el mismo dato compartido sin coordinacion, y el orden de ejecucion cambia el resultado",
          "code": "// Sin sincronizacion: dos hilos pueden leer el mismo valor de contador\n// antes de que ninguno escriba su incremento, perdiendo una actualizacion\nint contador = 0;\n// hilo A: lee contador (0), suma 1, escribe 1\n// hilo B: lee contador (0) al mismo tiempo, suma 1, escribe 1\n// resultado: contador = 1, no 2 -- se perdio un incremento",
          "answer": "ok",
          "explanation": "Una race condition no es un error que Java detecte ni lance como excepcion: el programa compila y corre, pero el resultado final depende del orden impredecible en que los hilos se intercalan, lo que produce resultados incorrectos de forma intermitente y dificil de reproducir.",
          "conceptId": "race-conditions",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "race-conditions-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que una race condition es mas dificil de detectar que un error de compilacion?",
          "answer": "Porque el programa compila y corre normalmente; el bug solo aparece de forma intermitente segun el orden en que los hilos se intercalan en tiempo de ejecucion",
          "distractors": ["Porque Java no permite compilar codigo con hilos", "Porque siempre lanza una excepcion en el primer intento", "Porque solo ocurre en sistemas con un unico nucleo"],
          "explanation": "A diferencia de un error de sintaxis, una race condition depende del scheduling del sistema operativo: puede pasar desapercibida en pruebas y aparecer solo en produccion bajo carga, cuando el timing de los hilos cambia.",
          "conceptId": "race-conditions",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "conc-sync-01",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que es una race condition (condicion de carrera)?",
          "answer": "Cuando el resultado de un programa depende del orden impredecible en que varios hilos acceden a datos compartidos",
          "distractors": ["Un error de compilacion por usar hilos sin declarar excepciones", "Una excepcion que se lanza cuando un hilo termina antes que otro", "Un tipo de deadlock que ocurre solo con ReentrantLock"],
          "explanation": "Una race condition ocurre cuando multiples hilos leen/escriben el mismo estado compartido sin sincronizacion, y el resultado final depende del orden de ejecucion.",
          "conceptId": "race-conditions",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "synchronized-basics-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "La palabra clave synchronized asegura que solo un hilo a la vez pueda ejecutar ese metodo o bloque, evitando la race condition",
          "code": "public synchronized void incrementar() {\n    contador++;  // ahora solo un hilo a la vez puede ejecutar esta linea\n}",
          "answer": "ok",
          "explanation": "synchronized adquiere un lock (el monitor del objeto) antes de entrar, y lo libera automaticamente al salir del metodo o bloque, incluso si se lanza una excepcion. Mientras un hilo tiene el lock, cualquier otro que intente entrar queda BLOCKED esperando.",
          "conceptId": "synchronized-basics",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["race-conditions"]
        },
        {
          "id": "conc-sync-02",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa la palabra clave para que este metodo sea thread-safe:",
          "code": "public _____ void incrementar() {\n    contador++;\n}",
          "answer": "synchronized",
          "distractors": ["volatile", "final", "static"],
          "explanation": "synchronized en un metodo de instancia asegura que solo un hilo a la vez pueda ejecutar el metodo sobre el mismo objeto.",
          "conceptId": "synchronized-basics",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["race-conditions"]
        },
        {
          "id": "conc-sync-03",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que hace este bloque synchronized?",
          "code": "synchronized (lock) {\n    saldo = saldo - monto;\n}",
          "answer": "Asegura que solo un hilo a la vez pueda ejecutar ese bloque mientras tenga el lock sobre el objeto lock",
          "distractors": ["Crea un nuevo hilo para ejecutar el bloque", "Hace que la variable saldo sea inmutable", "Repite la operacion hasta que tenga exito"],
          "explanation": "Un bloque synchronized(objeto) adquiere el monitor de ese objeto antes de ejecutar el bloque, sirviendo como mutex.",
          "conceptId": "synchronized-basics",
          "role": "guided",
          "pathOrder": 5,
          "dependsOn": ["race-conditions"]
        },
        {
          "id": "conc-sync-04",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cuando conviene usar un bloque synchronized en vez de sincronizar todo el metodo?",
          "answer": "Cuando solo una parte del metodo toca datos compartidos, para reducir el tiempo que otros hilos quedan bloqueados",
          "distractors": ["Nunca, sincronizar todo el metodo siempre es mejor", "Solo cuando el metodo es static", "Un bloque synchronized no puede lanzar excepciones"],
          "explanation": "Sincronizar solo la seccion critica minimiza el tiempo que el lock esta tomado, mejorando la concurrencia real del programa.",
          "conceptId": "synchronized-basics",
          "role": "solo",
          "pathOrder": 6,
          "dependsOn": ["race-conditions"]
        },
        {
          "id": "reentrant-lock-basics-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "ReentrantLock ofrece lo mismo que synchronized pero de forma explicita: hay que llamar lock() y unlock() manualmente, siempre en un try/finally",
          "code": "private final ReentrantLock lock = new ReentrantLock();\n\nvoid incrementar() {\n    lock.lock();\n    try {\n        contador++;\n    } finally {\n        lock.unlock();  // se libera siempre, incluso si hay una excepcion\n    }\n}",
          "answer": "ok",
          "explanation": "A diferencia de synchronized, que libera el lock automaticamente, ReentrantLock exige liberarlo manualmente -- por eso siempre va en un bloque finally, para garantizar que se libere incluso si el codigo protegido lanza una excepcion.",
          "conceptId": "reentrant-lock-basics",
          "role": "intro",
          "pathOrder": 7,
          "dependsOn": ["synchronized-basics"]
        },
        {
          "id": "conc-sync-05",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el patron correcto para liberar un ReentrantLock de forma segura:",
          "code": "lock.lock();\ntry {\n    seccionCritica();\n} finally {\n    lock._____();\n}",
          "answer": "unlock",
          "distractors": ["release", "close", "free"],
          "explanation": "unlock() debe llamarse en un finally para garantizar que el lock se libere incluso si seccionCritica() lanza una excepcion.",
          "conceptId": "reentrant-lock-basics",
          "role": "guided",
          "pathOrder": 8,
          "dependsOn": ["synchronized-basics"]
        },
        {
          "id": "conc-sync-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que ventaja ofrece ReentrantLock sobre synchronized?",
          "answer": "Permite intentar adquirir el lock con un tiempo limite (tryLock) sin bloquear indefinidamente",
          "distractors": ["Es mas rapido en todos los casos posibles", "No requiere liberarse manualmente nunca", "Permite que dos hilos entren a la vez a la seccion critica"],
          "explanation": "tryLock(timeout) permite evitar bloqueos indefinidos, algo que synchronized no ofrece directamente.",
          "conceptId": "reentrant-lock-basics",
          "role": "solo",
          "pathOrder": 9,
          "dependsOn": ["synchronized-basics"]
        },
        {
          "id": "semaphore-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Semaphore controla cuantos hilos pueden acceder a un recurso al mismo tiempo, no solo uno como synchronized",
          "code": "Semaphore semaforo = new Semaphore(3); // maximo 3 hilos a la vez\n\nvoid usarRecurso() throws InterruptedException {\n    semaforo.acquire(); // espera si ya hay 3 hilos dentro\n    try {\n        // seccion con hasta 3 hilos simultaneos permitidos\n    } finally {\n        semaforo.release();\n    }\n}",
          "answer": "ok",
          "explanation": "Un Semaphore mantiene un contador de 'permisos' disponibles; acquire() toma uno (bloqueando si no queda ninguno) y release() lo devuelve. Con permisos=1 se comporta como un lock exclusivo; con mas de 1 permite un numero limitado de accesos concurrentes, util para limitar conexiones a una base de datos o llamadas a una API externa.",
          "conceptId": "semaphore",
          "role": "intro",
          "pathOrder": 10,
          "dependsOn": ["reentrant-lock-basics"]
        },
        {
          "id": "semaphore-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Tienes un pool de 10 conexiones a una base de datos y quieres evitar que mas de 10 hilos las usen a la vez. Que estructura de sincronizacion es la mas adecuada?",
          "answer": "Semaphore(10): permite hasta 10 hilos simultaneos, ni mas ni menos",
          "distractors": ["synchronized: solo permite 1 hilo a la vez, muy restrictivo para este caso", "ReentrantLock: igual que synchronized, solo protege un recurso a la vez", "AtomicInteger: sirve para contadores, no para limitar acceso concurrente"],
          "explanation": "synchronized y ReentrantLock son binarios (1 hilo a la vez); Semaphore(n) generaliza esa idea a n hilos simultaneos, exactamente el caso de un pool con capacidad limitada.",
          "conceptId": "semaphore",
          "role": "guided",
          "pathOrder": 11,
          "dependsOn": ["reentrant-lock-basics"]
        },
        {
          "id": "semaphore-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Si un Semaphore tiene 2 permisos y ambos estan tomados, que pasa cuando un tercer hilo llama a acquire()?",
          "answer": "El hilo queda bloqueado (esperando) hasta que otro hilo llame a release() y libere un permiso",
          "distractors": ["Se lanza una excepcion inmediatamente", "acquire() devuelve false y el hilo continua sin el permiso", "El semaforo aumenta automaticamente su limite a 3"],
          "explanation": "acquire() es bloqueante por defecto: si no hay permisos disponibles, el hilo espera hasta que otro los libere con release(). Existe tryAcquire() como alternativa no bloqueante que devuelve false en vez de esperar.",
          "conceptId": "semaphore",
          "role": "solo",
          "pathOrder": 12,
          "dependsOn": ["reentrant-lock-basics"]
        },
        {
          "id": "readwrite-lock-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "ReadWriteLock permite que muchos hilos lean a la vez, pero solo uno puede escribir, y nunca a la vez que se lee",
          "code": "ReadWriteLock rwLock = new ReentrantReadWriteLock();\n\nvoid leer() {\n    rwLock.readLock().lock();\n    try {\n        // muchos hilos pueden ejecutar esto a la vez\n    } finally {\n        rwLock.readLock().unlock();\n    }\n}\n\nvoid escribir() {\n    rwLock.writeLock().lock();\n    try {\n        // solo un hilo, y ningun lector, puede ejecutar esto\n    } finally {\n        rwLock.writeLock().unlock();\n    }\n}",
          "answer": "ok",
          "explanation": "Cuando los datos se leen mucho mas de lo que se escriben, un ReentrantLock normal es innecesariamente restrictivo: bloquea incluso a lectores que no se pisarian entre si. ReadWriteLock separa el permiso de lectura (compartido entre varios hilos) del de escritura (exclusivo, y excluyente con cualquier lectura en curso).",
          "conceptId": "readwrite-lock",
          "role": "intro",
          "pathOrder": 13,
          "dependsOn": ["semaphore"]
        },
        {
          "id": "readwrite-lock-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Mientras un hilo tiene el readLock() de un ReadWriteLock, que puede pasar?",
          "answer": "Otros hilos pueden tambien adquirir el readLock() al mismo tiempo, pero ningun hilo puede adquirir el writeLock() hasta que todos los lectores lo liberen",
          "distractors": ["Ningun otro hilo puede leer ni escribir hasta que se libere", "Otros hilos pueden escribir libremente sin restriccion", "El readLock() se convierte automaticamente en writeLock() para el mismo hilo"],
          "explanation": "El readLock() es compartido: multiples hilos pueden tenerlo simultaneamente. El writeLock() es exclusivo y ademas excluye a los lectores -- debe esperar a que todos los readLock() activos se liberen antes de poder adquirirse.",
          "conceptId": "readwrite-lock",
          "role": "guided",
          "pathOrder": 14,
          "dependsOn": ["semaphore"]
        },
        {
          "id": "readwrite-lock-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que conviene usar ReadWriteLock en vez de un ReentrantLock normal para una cache que se lee constantemente y se escribe rara vez?",
          "answer": "Porque permite que muchos hilos lean la cache al mismo tiempo sin bloquearse entre si, mejorando el rendimiento cuando las lecturas son mucho mas frecuentes que las escrituras",
          "distractors": ["Porque ReadWriteLock no requiere liberarse manualmente", "Porque ReentrantLock no puede usarse dentro de metodos que leen datos", "Porque ReadWriteLock es mas rapido incluso cuando se escribe todo el tiempo"],
          "explanation": "Un ReentrantLock normal serializa TODOS los accesos, incluso entre lectores que no modifican nada. Cuando las lecturas dominan, eso desperdicia paralelismo real; ReadWriteLock lo recupera dejando que los lectores no se bloqueen entre si, mientras sigue protegiendo contra condiciones de carrera durante la escritura.",
          "conceptId": "readwrite-lock",
          "role": "solo",
          "pathOrder": 15,
          "dependsOn": ["semaphore"]
        }
      ]
```

- [ ] **Step 2: Verify the JSON is well-formed and the diff is grandfathering-safe**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/concurrency.json'))"`
Expected: no output (valid JSON, no exception).

Run: `git diff app/src/main/assets/content/concurrency.json` and confirm
every changed line for `conc-sync-01` through `conc-sync-06` is purely an
*addition* (`conceptId`/`role`/`pathOrder`/`dependsOn` lines) — no
existing line's `id`/`type`/`prompt`/`code`/`answer`/`distractors`/
`explanation` value changed, and the other 3 units
(`conc-threads-lifecycle`, `conc-executors`, `conc-virtual-threads`) are
completely untouched.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/concurrency.json
git commit -m "content: retrofit conc-sincronizacion and add Semaphore/ReadWriteLock concepts"
```

---

### Task 2: Whole-corpus validation and content version bump

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Read-only validation: `app/src/main/assets/content/concurrency.json`

**Interfaces:**
- Consumes: the final state of `concurrency.json` after Task 1.
- Produces: `CURRENT_CONTENT_VERSION = "16"`, which is what triggers
  `ContentSeeder.seedIfNeeded()` to re-seed the database on next app
  launch — required for this content to actually reach a device.

- [ ] **Step 1: Write and run a full validation script**

Run this script (adjust nothing — it encodes every Global Constraint,
using embedded literal copies of the pre-existing exercises rather than
`git show HEAD~N` for the grandfathering check, since that's fragile if a
fix round adds extra commits — a technique already used successfully in
sub-cycle 1's Task 4):

```bash
python3 << 'EOF'
import json

path = "app/src/main/assets/content/concurrency.json"
data = json.load(open(path))

all_units = {u["unitId"]: u for u in data["units"]}
touched_unit = "conc-sincronizacion"
untouched_units = {"conc-threads-lifecycle", "conc-executors", "conc-virtual-threads"}
assert {touched_unit} | untouched_units == set(all_units), \
    f"unexpected unit set: {set(all_units)}"

# 1. Grandfathering: every pre-existing exercise id in conc-sincronizacion
#    keeps its 7 protected fields byte-identical to its ORIGINAL state
#    (copied verbatim below from the file as it existed before this plan).
protected_fields = ["id", "type", "prompt", "code", "answer", "distractors", "explanation"]
before_exercises_raw = [
    {"id": "conc-sync-01", "type": "mcq", "difficulty": 2, "prompt": "Que es una race condition (condicion de carrera)?", "answer": "Cuando el resultado de un programa depende del orden impredecible en que varios hilos acceden a datos compartidos", "distractors": ["Un error de compilacion por usar hilos sin declarar excepciones", "Una excepcion que se lanza cuando un hilo termina antes que otro", "Un tipo de deadlock que ocurre solo con ReentrantLock"], "explanation": "Una race condition ocurre cuando multiples hilos leen/escriben el mismo estado compartido sin sincronizacion, y el resultado final depende del orden de ejecucion."},
    {"id": "conc-sync-02", "type": "fill_blank", "difficulty": 1, "prompt": "Completa la palabra clave para que este metodo sea thread-safe:", "code": "public _____ void incrementar() {\n    contador++;\n}", "answer": "synchronized", "distractors": ["volatile", "final", "static"], "explanation": "synchronized en un metodo de instancia asegura que solo un hilo a la vez pueda ejecutar el metodo sobre el mismo objeto."},
    {"id": "conc-sync-03", "type": "mcq", "difficulty": 2, "prompt": "Que hace este bloque synchronized?", "code": "synchronized (lock) {\n    saldo = saldo - monto;\n}", "answer": "Asegura que solo un hilo a la vez pueda ejecutar ese bloque mientras tenga el lock sobre el objeto lock", "distractors": ["Crea un nuevo hilo para ejecutar el bloque", "Hace que la variable saldo sea inmutable", "Repite la operacion hasta que tenga exito"], "explanation": "Un bloque synchronized(objeto) adquiere el monitor de ese objeto antes de ejecutar el bloque, sirviendo como mutex."},
    {"id": "conc-sync-04", "type": "mcq", "difficulty": 3, "prompt": "Cuando conviene usar un bloque synchronized en vez de sincronizar todo el metodo?", "answer": "Cuando solo una parte del metodo toca datos compartidos, para reducir el tiempo que otros hilos quedan bloqueados", "distractors": ["Nunca, sincronizar todo el metodo siempre es mejor", "Solo cuando el metodo es static", "Un bloque synchronized no puede lanzar excepciones"], "explanation": "Sincronizar solo la seccion critica minimiza el tiempo que el lock esta tomado, mejorando la concurrencia real del programa."},
    {"id": "conc-sync-05", "type": "fill_blank", "difficulty": 2, "prompt": "Completa el patron correcto para liberar un ReentrantLock de forma segura:", "code": "lock.lock();\ntry {\n    seccionCritica();\n} finally {\n    lock._____();\n}", "answer": "unlock", "distractors": ["release", "close", "free"], "explanation": "unlock() debe llamarse en un finally para garantizar que el lock se libere incluso si seccionCritica() lanza una excepcion."},
    {"id": "conc-sync-06", "type": "mcq", "difficulty": 3, "prompt": "Que ventaja ofrece ReentrantLock sobre synchronized?", "answer": "Permite intentar adquirir el lock con un tiempo limite (tryLock) sin bloquear indefinidamente", "distractors": ["Es mas rapido en todos los casos posibles", "No requiere liberarse manualmente nunca", "Permite que dos hilos entren a la vez a la seccion critica"], "explanation": "tryLock(timeout) permite evitar bloqueos indefinidos, algo que synchronized no ofrece directamente."},
]
before_exercises = {e["id"]: e for e in before_exercises_raw}
assert len(before_exercises) == 6, f"expected 6 pre-existing exercises, got {len(before_exercises)}"

exercises = all_units[touched_unit]["exercises"]
assert len(exercises) == 16, f"expected 16 exercises in {touched_unit}, got {len(exercises)}"

new_ids = []
for e in exercises:
    if e["id"] in before_exercises:
        old = before_exercises[e["id"]]
        for f in protected_fields:
            assert e.get(f) == old.get(f), \
                f"{e['id']}: field '{f}' changed (grandfathering violation)"
    else:
        new_ids.append(e["id"])
assert len(new_ids) == 10, f"expected 10 new exercises, got {len(new_ids)}: {new_ids}"

print(f"conc-sincronizacion grandfathering OK: {len(exercises)} exercises ({len(new_ids)} new).")

# 2. The 3 untouched units must have exactly the same exercise counts as
#    sub-cycle 1 left them (a full byte-for-byte check isn't needed here
#    since no task in THIS plan touches them at all -- a count check is
#    enough to catch an accidental edit).
for uid, expected_count in [("conc-threads-lifecycle", 8), ("conc-executors", 16), ("conc-virtual-threads", 15)]:
    actual = len(all_units[uid]["exercises"])
    assert actual == expected_count, f"{uid}: expected {expected_count} exercises (unchanged from sub-cycle 1), got {actual}"
print("Untouched units' exercise counts unchanged from sub-cycle 1 (8/16/15).")
EOF
```

Expected: `conc-sincronizacion grandfathering OK: 16 exercises (10 new).`
followed by `Untouched units' exercise counts unchanged from sub-cycle 1
(8/16/15).`, no assertion errors.

- [ ] **Step 2: Run the remaining standing-rule checks (case-collision, one-terminal-role, dependsOn, pathOrder, accents, reachability)**

```bash
python3 << 'EOF'
import json, re

path = "app/src/main/assets/content/concurrency.json"
data = json.load(open(path))
unit = next(u for u in data["units"] if u["unitId"] == "conc-sincronizacion")
exercises = unit["exercises"]

# Case-collision rule.
for e in exercises:
    ans = e.get("answer")
    for d in e.get("distractors", []):
        assert not (isinstance(ans, str) and d.lower() == ans.lower() and d != ans), \
            f"{e['id']}: distractor '{d}' differs from answer only by case"

# One-terminal-role rule + dependsOn same-unit-only rule + sequential pathOrder.
concept_ids = {e["conceptId"] for e in exercises if e.get("conceptId")}
terminal_counts = {}
orders = []
for e in exercises:
    orders.append(e.get("pathOrder"))
    if e.get("role") in ("solo", "practice"):
        cid = e["conceptId"]
        terminal_counts[cid] = terminal_counts.get(cid, 0) + 1
    for dep in e.get("dependsOn", []):
        assert dep in concept_ids, f"{e['id']}: dependsOn '{dep}' not a concept in this unit"
for cid in concept_ids:
    assert terminal_counts.get(cid) == 1, f"concept '{cid}' has {terminal_counts.get(cid, 0)} terminal exercises, expected 1"
assert orders == list(range(len(orders))), f"pathOrder not sequential 0..n-1: {sorted(orders)}"

# No accented characters (project-wide convention) -- whole-file check.
raw = open(path, encoding="utf-8").read()
accented = re.findall(r"[À-ÿ]", raw)
assert not accented, f"found accented characters: {accented}"

print(f"All standing-rule checks passed. {len(exercises)} exercises, {len(concept_ids)} concepts.")

# Reachability simulation, matching GetTodaySessionUseCase.selectPathExercises exactly.
answered = set()
sessions = 0
limit = 5
while len(answered) < len(exercises):
    born = {e["conceptId"] for e in exercises
            if e.get("conceptId") and e.get("role") in ("solo", "practice") and e["id"] in answered}
    candidates = [e for e in exercises if e["id"] not in answered and
                  (e.get("conceptId") is None or
                   (e["conceptId"] not in born and all(d in born for d in e.get("dependsOn", []))))]
    assert candidates, f"STRANDED after {sessions} sessions, unreached: {[e['id'] for e in exercises if e['id'] not in answered]}"
    candidates.sort(key=lambda e: e.get("pathOrder") if e.get("pathOrder") is not None else 10**9)
    for e in candidates[:limit]:
        answered.add(e["id"])
    sessions += 1
print(f"conc-sincronizacion: drains cleanly in {sessions} sessions ({len(exercises)} exercises)")
EOF
```

Expected: `All standing-rule checks passed. 16 exercises, 5 concepts.`
followed by `conc-sincronizacion: drains cleanly in 5 sessions (16 exercises)`,
no assertion errors.

- [ ] **Step 3: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find:

```kotlin
private const val CURRENT_CONTENT_VERSION = "15"
```

Replace with:

```kotlin
private const val CURRENT_CONTENT_VERSION = "16"
```

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still passing (no test
changes needed — this task touches only a constant and a content asset).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: bump content version to 16 for Concurrencia sub-cycle 2"
```

---

## After the plan: manual on-device QA

Install a clean/in-place build and manually verify on-device (adb):

1. Navigate to "Concurrencia" > "Sincronizacion": confirm the new intro
   cards render correctly and the unit is still fully playable end to
   end, including the pre-existing exercises now wrapped in ladders.
2. Play through far enough to reach the new `semaphore` concept — confirm
   the `dependsOn` gating works (it only appears after
   `reentrant-lock-basics`'s solo exercise is answered).
3. Continue to `readwrite-lock` — confirm it only appears after
   `semaphore`'s solo exercise is answered.
4. Confirm review_state (SM-2 spaced-repetition) for any exercise already
   answered before this update is unaffected — i.e. it does not reappear
   as "new" content, only as a scheduled review if due.
5. Confirm "Virtual threads y colecciones concurrentes" — now reachable
   for the first time since sub-cycle 1, since it was gated behind this
   unit in section order — unlocks correctly and its content (including
   the `copy-on-write-list` concept added in sub-cycle 1, never
   on-device-verified) renders and grades correctly.
6. **This closes the Concurrencia section and the entire 5-cycle
   retrofit series** — after this QA pass, update project memory to
   reflect that.
