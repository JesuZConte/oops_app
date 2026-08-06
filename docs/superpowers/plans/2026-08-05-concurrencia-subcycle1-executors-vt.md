# Concurrencia Retrofit Sub-cycle 1 (Threads Lifecycle + Executors + Virtual Threads) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retrofit `app/src/main/assets/content/concurrency.json`'s three
units `conc-threads-lifecycle`, `conc-executors`, `conc-virtual-threads`
with full first-exposure ladder metadata (`conceptId`/`role`/`pathOrder`/
`dependsOn`), and close two real exam-objective gaps identified in
`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`: **CompletableFuture**
(the only genuinely absent Callable/Future-family topic — Callable and
Future themselves are already covered) and **CopyOnWriteArrayList** (the
`java.util.concurrent` collections gap — ConcurrentHashMap is already
covered, but shallowly, and gets one depth exercise added here too). The
fourth unit, `conc-sincronizacion` (needs Semaphore/ReadWriteLock), is
deliberately out of scope — it ships in Sub-cycle 2. `parallel-streams`
content, originally considered for this section, will instead be added as
a new unit to the already-shipped Streams y Lambdas section in a separate
follow-up cycle (decided by Luis: keeps parallel streams pedagogically
adjacent to the rest of streams content, not stranded two sections later).

**Architecture:** Pure content-authoring in one JSON asset file — zero
Kotlin/Compose changes beyond bumping `CURRENT_CONTENT_VERSION` in
`ContentSeeder.kt`. Every pre-existing exercise is grandfathered: only
`conceptId`/`role`/`pathOrder`/`dependsOn` are added to it, and the 7
protected fields (`id`, `type`, `prompt`, `code`, `answer`, `distractors`,
`explanation`) stay byte-identical to preserve each exercise's real
`review_state` (SM-2 spaced-repetition history), keyed by exercise id. New
exercises are added for two new concepts under `conc-executors`
(`completable-future-basics`, `completable-future-combining`) and one new
concept under `conc-virtual-threads` (`copy-on-write-list`), plus new
`intro`/`guided` rungs where an existing concept only had 1-2 existing
exercises to build a ladder around.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell (unchanged
this cycle), JUnit4 for the existing use-case test suite (must stay green
— no test changes needed since no Kotlin code changes).

## Global Constraints

- **Grandfathering rule:** for every pre-existing exercise (id starts with
  `conc-threads-`, `conc-executors-`, or `conc-vt-`), the 7 fields `id`,
  `type`, `prompt`, `code` (where present), `answer`, `distractors` (where
  present), `explanation` must remain byte-identical to the current file.
  Only `conceptId`, `role`, `pathOrder`, and (where applicable) `dependsOn`
  may be added. Never touch these fields' values, whitespace, or order.
- **Literal text insertion only — never JSON load+dump.** A prior sub-cycle
  (Streams) hit a real regression where an implementer used a Python JSON
  library to load, mutate, and re-dump the file: this reformatted every
  other array in the file (single-line arrays became multi-line) even
  though no values changed, and had to be reverted. Every task in this
  plan gives you the exact, complete final `"exercises": [...]` array for
  its unit as literal text — replace the old array with the new one using
  a text edit (e.g. the Edit tool's exact-string replacement), never a
  JSON parse-and-rewrite. Do not run the file through `json.load()` +
  `json.dump()` for editing purposes (reading it with `json.load()` for
  read-only validation in Task 4 is fine).
- **One-terminal-role rule:** every `conceptId` has exactly one exercise
  with `role: "solo"` or `role: "practice"` — never zero, never two.
- **dependsOn same-unit-only rule:** every id listed in a `dependsOn` array
  must be a `conceptId` that exists elsewhere in the *same* unit's
  `exercises` array. Cross-unit `dependsOn` is silently unsatisfiable (the
  session-selection algorithm computes `bornConceptIds` per-unit) and must
  never be used.
- **Sequential pathOrder rule:** within each unit, `pathOrder` values across
  all exercises (existing + new) must be exactly `0, 1, 2, ..., n-1` with
  no gaps and no duplicates.
- **Case-collision rule:** no `mcq`/`fill_blank` exercise's `distractors`
  may differ from its own `answer` only by letter case.
- **No accents in Spanish content** (project-wide convention, verified with
  `LC_ALL=C grep -nP "[\x80-\xFF]"` returning empty on the touched file).
- **Interview-question convention:** not applicable to this plan — no
  interview-flavor exercises are added in this sub-cycle.
- **worked_example format:** every `intro`-role exercise has
  `"type": "worked_example"`, `"answer": "ok"`, no `distractors` field, and
  a `code` field showing the illustrative snippet.
- **Runtime-observable content must be verified against a real JDK, not
  just read.** Every `predict_output` exercise and every factual claim
  about API behavior (`RejectedExecutionException`, `CopyOnWriteArrayList`
  iterator snapshot semantics, `CompletableFuture` chaining) in this plan
  has already been executed against a local JDK 20 during design and
  confirmed correct — do not re-derive these from documentation alone if
  you need to modify them; re-run them.
- **Never use `predict_output` for content whose result depends on thread
  interleaving/scheduling non-determinism.** All predict_output exercises
  in this plan use `.join()`/`.get()` blocking calls with no shared mutable
  state, so their output is deterministic. If any fix or addition touches
  genuinely concurrent (non-deterministic) behavior, express it as an
  `mcq` about *why* it's unsafe, never as an exact predicted output.

---

### Task 1: Retrofit `conc-threads-lifecycle` (ladders only, no exam gap here)

**Files:**
- Modify: `app/src/main/assets/content/concurrency.json` (the
  `conc-threads-lifecycle` unit's `exercises` array only, lines 16-72 in
  the current file)

**Interfaces:**
- Produces: concepts `thread-start-vs-run` (pathOrder 0-3, no dependsOn)
  and `thread-lifecycle-states` (pathOrder 4-7, `dependsOn:
  ["thread-start-vs-run"]`) — both same-unit only, consumed by nothing
  outside this unit.

- [ ] **Step 1: Replace the unit's `exercises` array**

In `app/src/main/assets/content/concurrency.json`, find the
`conc-threads-lifecycle` unit's `"exercises": [ ... ]` array (currently 6
exercises, ids `conc-threads-01` through `conc-threads-06`, exactly as
shown in Global Constraints — copy the current 6 objects verbatim from the
file, do not retype them from memory). Replace the entire array (opening
`[` through closing `]`) with this exact final version — 8 exercises, 2
new intros plus the 6 existing exercises verbatim with `conceptId`/
`role`/`pathOrder`/`dependsOn` added:

```json
      "exercises": [
        {
          "id": "thread-start-vs-run-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "start() lanza un hilo nuevo del sistema operativo; run() llamado directamente solo ejecuta ese codigo en el hilo actual, sin crear concurrencia",
          "code": "Thread hilo = new Thread(() -> System.out.println(\"nuevo hilo\"));\nhilo.start();  // crea un hilo del SO y ejecuta run() ahi\n// hilo.run();  // si se llamara asi en vez de start(), NO crea hilo nuevo",
          "answer": "ok",
          "explanation": "start() es el metodo que realmente lanza un nuevo hilo del sistema operativo y luego invoca run() en el. Si en cambio llamas run() directamente, no se crea ningun hilo nuevo: el codigo se ejecuta secuencialmente en el hilo que hizo la llamada, como cualquier metodo normal.",
          "conceptId": "thread-start-vs-run",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "conc-threads-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que problema practico resuelven los hilos que un programa de un solo hilo no puede resolver bien?",
          "answer": "Permiten que tareas independientes progresen en paralelo o se solapen, sin que una bloquee completamente a las demas",
          "distractors": ["Hacen que cualquier programa use menos memoria", "Eliminan la necesidad de manejar excepciones", "Garantizan que el codigo se ejecute en orden estricto"],
          "explanation": "Sin hilos, una tarea lenta (como I/O o un calculo pesado) bloquea todo el programa; los hilos permiten aprovechar multiples nucleos o no bloquear mientras se espera I/O.",
          "conceptId": "thread-start-vs-run",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "conc-threads-01",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual es la diferencia entre llamar start() y run() en un Thread?",
          "answer": "start() crea un nuevo hilo de ejecucion y luego invoca run(); llamar run() directamente ejecuta el codigo en el hilo actual",
          "distractors": ["No hay diferencia, ambos hacen lo mismo", "run() siempre se ejecuta antes que start()", "start() solo funciona con Runnable, no con Thread"],
          "explanation": "start() es el metodo que realmente lanza un nuevo hilo del sistema operativo; run() es solo un metodo normal que, llamado directamente, se ejecuta secuencialmente en el hilo que lo invoca.",
          "conceptId": "thread-start-vs-run",
          "role": "guided",
          "pathOrder": 2
        },
        {
          "id": "conc-threads-02",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa la creacion del hilo a partir de la tarea:",
          "code": "Runnable tarea = () -> System.out.println(\"ejecutando\");\nThread hilo = new Thread(_____);\nhilo.start();",
          "answer": "tarea",
          "distractors": ["tarea.run()", "new Runnable(tarea)", "Thread.tarea"],
          "explanation": "El constructor de Thread recibe el Runnable directamente; pasar tarea.run() ejecutaria el codigo de inmediato en el hilo actual, sin crear un hilo nuevo.",
          "conceptId": "thread-start-vs-run",
          "role": "solo",
          "pathOrder": 3
        },
        {
          "id": "thread-lifecycle-states-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un thread pasa por varios estados durante su vida: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING y TERMINATED",
          "code": "NEW           // creado, aun no llamo start()\nRUNNABLE      // ejecutandose o listo para ejecutar\nBLOCKED       // esperando entrar a un synchronized ocupado por otro hilo\nWAITING       // esperando indefinidamente (wait() sin timeout, join() sin timeout)\nTIMED_WAITING // esperando con limite de tiempo (sleep(), wait(timeout))\nTERMINATED    // termino su ejecucion",
          "answer": "ok",
          "explanation": "BLOCKED, WAITING y TIMED_WAITING se confunden facilmente: BLOCKED es especificamente esperar para entrar a un synchronized ocupado; WAITING es una espera indefinida (sin plazo); TIMED_WAITING es una espera con limite de tiempo, como Thread.sleep(ms) o join(ms).",
          "conceptId": "thread-lifecycle-states",
          "role": "intro",
          "pathOrder": 4,
          "dependsOn": ["thread-start-vs-run"]
        },
        {
          "id": "conc-threads-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "En que estado queda un hilo que llamo a Thread.sleep(1000)?",
          "answer": "TIMED_WAITING",
          "distractors": ["RUNNABLE", "BLOCKED", "TERMINATED"],
          "explanation": "sleep() con un tiempo definido pone al hilo en TIMED_WAITING hasta que el tiempo expire o sea interrumpido.",
          "conceptId": "thread-lifecycle-states",
          "role": "guided",
          "pathOrder": 5,
          "dependsOn": ["thread-start-vs-run"]
        },
        {
          "id": "conc-threads-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Un hilo que intenta entrar a un bloque synchronized ya ocupado por otro hilo, en que estado queda?",
          "answer": "BLOCKED",
          "distractors": ["WAITING", "TIMED_WAITING", "NEW"],
          "explanation": "BLOCKED es especificamente el estado de esperar para adquirir un lock de un bloque o metodo synchronized; WAITING es para esperas indefinidas como wait() sin timeout.",
          "conceptId": "thread-lifecycle-states",
          "role": "guided",
          "pathOrder": 6,
          "dependsOn": ["thread-start-vs-run"]
        },
        {
          "id": "conc-threads-03",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo (asumiendo que join() no lanza ninguna excepcion)?",
          "code": "Thread hilo = new Thread(() -> System.out.println(\"hilo\"));\nhilo.start();\nhilo.join();\nSystem.out.println(\"principal\");",
          "answer": "hilo\nprincipal",
          "explanation": "join() bloquea el hilo principal hasta que hilo termine, garantizando que \"hilo\" se imprima antes que \"principal\".",
          "conceptId": "thread-lifecycle-states",
          "role": "solo",
          "pathOrder": 7,
          "dependsOn": ["thread-start-vs-run"]
        }
      ]
```

- [ ] **Step 2: Verify the JSON is well-formed and the diff is grandfathering-safe**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/concurrency.json'))"`
Expected: no output (valid JSON, no exception).

Run: `git diff app/src/main/assets/content/concurrency.json` and confirm
every changed line for `conc-threads-01` through `conc-threads-06` is
purely an *addition* (`conceptId`/`role`/`pathOrder`/`dependsOn` lines) —
no existing line's `id`/`type`/`prompt`/`code`/`answer`/`distractors`/
`explanation` value changed, and the other 3 units (`conc-executors`,
`conc-sincronizacion`, `conc-virtual-threads`) are completely untouched.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/concurrency.json
git commit -m "content: retrofit conc-threads-lifecycle with first-exposure ladder metadata"
```

---

### Task 2: Retrofit `conc-executors` + close the CompletableFuture gap

**Files:**
- Modify: `app/src/main/assets/content/concurrency.json` (the
  `conc-executors` unit's `exercises` array only, currently lines 83-140
  in the pre-Task-1 file — re-locate it after Task 1's edit, since Task 1
  shifted line numbers)

**Interfaces:**
- Produces: concepts `executor-basics` (pathOrder 0-2, no dependsOn),
  `callable-future-basics` (pathOrder 3-6, `dependsOn:
  ["executor-basics"]`), `executor-shutdown` (pathOrder 7-9, `dependsOn:
  ["executor-basics"]`), `completable-future-basics` (pathOrder 10-12,
  `dependsOn: ["callable-future-basics"]`), `completable-future-combining`
  (pathOrder 13-15, `dependsOn: ["completable-future-basics"]`).
- Consumes: nothing from Task 1 (different unit — `dependsOn` never
  crosses units).

- [ ] **Step 1: Replace the unit's `exercises` array**

In `app/src/main/assets/content/concurrency.json`, find the
`conc-executors` unit's `"exercises": [ ... ]` array (currently 6
exercises, ids `conc-executors-01` through `conc-executors-06` — copy them
verbatim from the current file, do not retype from memory). Replace the
entire array with this exact final version — 16 exercises, 10 new plus the
6 existing exercises verbatim with ladder fields added:

```json
      "exercises": [
        {
          "id": "executor-basics-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Crear un hilo nuevo por cada tarea es costoso; un ExecutorService reutiliza un pool de hilos ya creados",
          "code": "// Sin pool: crea y destruye un hilo nuevo por cada tarea (costoso)\nnew Thread(tarea).start();\n\n// Con pool: reutiliza hilos ya creados\nExecutorService pool = Executors.newFixedThreadPool(4);\npool.execute(tarea);",
          "answer": "ok",
          "explanation": "Cada Thread nuevo consume memoria y tiempo de creacion en el sistema operativo. Un ExecutorService mantiene un conjunto fijo de hilos listos para reutilizar, evitando ese costo repetido tarea por tarea.",
          "conceptId": "executor-basics",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "conc-executors-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que conviene usar un ExecutorService en vez de crear hilos nuevos manualmente para cada tarea?",
          "answer": "Reutiliza un numero controlado de hilos, evitando el costo de crear y destruir hilos constantemente y limitando el uso de recursos",
          "distractors": ["Porque los pools ejecutan las tareas mas rapido en cualquier caso", "Porque Thread ya no esta disponible en Java 21", "Porque los pools eliminan la necesidad de manejar excepciones"],
          "explanation": "Crear un hilo del sistema operativo tiene costo; un pool reutiliza hilos existentes y limita cuantos corren a la vez, evitando saturar el sistema.",
          "conceptId": "executor-basics",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "conc-executors-01",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa la creacion de un pool fijo de 4 hilos:",
          "code": "ExecutorService pool = Executors._____(4);",
          "answer": "newFixedThreadPool",
          "distractors": ["newCachedThreadPool", "newSingleThreadExecutor", "newScheduledThreadPool"],
          "explanation": "newFixedThreadPool(n) crea un pool con exactamente n hilos reutilizables.",
          "conceptId": "executor-basics",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "callable-future-basics-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Callable<V> es como Runnable pero devuelve un valor (y puede lanzar excepciones checked); submit() lo ejecuta y devuelve un Future para recuperar ese valor",
          "code": "Callable<Integer> tarea = () -> 10 + 20;\nFuture<Integer> resultado = pool.submit(tarea);\nInteger valor = resultado.get(); // bloquea hasta que la tarea termine",
          "answer": "ok",
          "explanation": "Runnable.run() no devuelve nada y no puede declarar excepciones checked; Callable<V>.call() devuelve V y puede lanzar checked exceptions. submit() acepta ambos, pero solo con Callable el Future.get() trae un valor util.",
          "conceptId": "callable-future-basics",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["executor-basics"]
        },
        {
          "id": "conc-executors-03",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa la interfaz que permite que una tarea devuelva un valor y declare excepciones checked:",
          "code": "_____<Integer> tarea = () -> 42;",
          "answer": "Callable",
          "distractors": ["Runnable", "Supplier", "Future"],
          "explanation": "Callable<V> tiene un metodo call() que devuelve V y puede lanzar excepciones checked, a diferencia de Runnable.",
          "conceptId": "callable-future-basics",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["executor-basics"]
        },
        {
          "id": "conc-executors-02",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual es la diferencia principal entre execute() y submit() en ExecutorService?",
          "answer": "submit() devuelve un Future para obtener el resultado o la excepcion; execute() no devuelve nada",
          "distractors": ["execute() es mas rapido porque no crea hilos", "submit() solo acepta Runnable, no Callable", "No hay diferencia real entre ambos"],
          "explanation": "submit() acepta Runnable o Callable y devuelve un Future; execute() (de la interfaz Executor) solo acepta Runnable y no da forma de recuperar resultados o excepciones.",
          "conceptId": "callable-future-basics",
          "role": "guided",
          "pathOrder": 5,
          "dependsOn": ["executor-basics"]
        },
        {
          "id": "conc-executors-04",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo (asumiendo que get() no lanza ninguna excepcion)?",
          "code": "ExecutorService pool = Executors.newSingleThreadExecutor();\nFuture<Integer> resultado = pool.submit(() -> 10 + 20);\nSystem.out.println(resultado.get());\npool.shutdown();",
          "answer": "30",
          "explanation": "get() bloquea hasta que la tarea termine y devuelve el resultado del Callable, que aqui es 10 + 20 = 30.",
          "conceptId": "callable-future-basics",
          "role": "solo",
          "pathOrder": 6,
          "dependsOn": ["executor-basics"]
        },
        {
          "id": "executor-shutdown-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un ExecutorService sigue vivo despues de que sus tareas terminan; hay que cerrarlo explicitamente con shutdown() o shutdownNow()",
          "code": "ExecutorService pool = Executors.newFixedThreadPool(2);\npool.submit(() -> System.out.println(\"tarea\"));\npool.shutdown(); // deja terminar tareas en curso, rechaza nuevas, y el programa puede salir",
          "answer": "ok",
          "explanation": "Si nunca llamas shutdown(), el pool sigue con hilos vivos esperando trabajo, y la JVM puede no terminar el programa. shutdown() es el cierre ordenado normal; shutdownNow() fuerza la interrupcion de las tareas en curso.",
          "conceptId": "executor-shutdown",
          "role": "intro",
          "pathOrder": 7,
          "dependsOn": ["executor-basics"]
        },
        {
          "id": "executor-shutdown-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que pasa si envias una tarea con submit() despues de haber llamado shutdown()?",
          "answer": "Se lanza RejectedExecutionException; shutdown() no acepta tareas nuevas, solo termina las que ya estaban en curso",
          "distractors": ["La tarea se encola y se ejecuta cuando el pool vuelva a estar libre", "La tarea se ejecuta igual, en el hilo que llama a submit()", "No pasa nada, shutdown() se ignora si hay tareas nuevas"],
          "explanation": "shutdown() marca el executor para rechazar nuevo trabajo; cualquier submit()/execute() posterior lanza RejectedExecutionException, a diferencia de shutdownNow() que ademas intenta interrumpir lo que esta corriendo.",
          "conceptId": "executor-shutdown",
          "role": "guided",
          "pathOrder": 8,
          "dependsOn": ["executor-basics"]
        },
        {
          "id": "conc-executors-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cual es la diferencia entre shutdown() y shutdownNow() en ExecutorService?",
          "answer": "shutdown() deja terminar las tareas ya enviadas y rechaza nuevas; shutdownNow() intenta interrumpir las tareas en ejecucion de inmediato",
          "distractors": ["Son exactamente lo mismo, solo cambia el nombre", "shutdown() detiene el pool inmediatamente sin esperar nada", "shutdownNow() solo funciona con Callable, no con Runnable"],
          "explanation": "shutdown() es un apagado ordenado (espera que las tareas en cola terminen); shutdownNow() intenta detener las tareas activas y devuelve las que quedaban pendientes.",
          "conceptId": "executor-shutdown",
          "role": "solo",
          "pathOrder": 9,
          "dependsOn": ["executor-basics"]
        },
        {
          "id": "completable-future-basics-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "CompletableFuture.supplyAsync() ejecuta una tarea en otro hilo y permite encadenar que hacer con el resultado, sin bloquear con get()",
          "code": "CompletableFuture<Integer> futuro = CompletableFuture.supplyAsync(() -> 10 + 20);\nfuturo.thenAccept(valor -> System.out.println(\"resultado: \" + valor));",
          "answer": "ok",
          "explanation": "supplyAsync(Supplier<T>) inicia una tarea asincronica en el ForkJoinPool comun (o un Executor que le pases) y devuelve un CompletableFuture<T>. thenAccept() registra que hacer con el valor cuando este listo, sin necesitar llamar get() y bloquear el hilo actual.",
          "conceptId": "completable-future-basics",
          "role": "intro",
          "pathOrder": 10,
          "dependsOn": ["callable-future-basics"]
        },
        {
          "id": "completable-future-basics-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual es la diferencia principal entre Future y CompletableFuture?",
          "answer": "CompletableFuture permite encadenar acciones (thenApply, thenAccept, etc.) sobre el resultado sin bloquear; Future solo permite bloquear con get() para obtener el valor",
          "distractors": ["Future es mas nuevo y reemplaza a CompletableFuture", "CompletableFuture no puede usarse con ExecutorService", "No hay diferencia, CompletableFuture es solo un alias de Future"],
          "explanation": "Future.get() es la unica forma de acceder al resultado, y bloquea el hilo llamador. CompletableFuture implementa Future pero ademas ofrece metodos como thenApply/thenAccept/thenCompose para reaccionar al resultado de forma no bloqueante y componer varias tareas asincronicas.",
          "conceptId": "completable-future-basics",
          "role": "guided",
          "pathOrder": 11,
          "dependsOn": ["callable-future-basics"]
        },
        {
          "id": "completable-future-basics-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "CompletableFuture<Integer> futuro = CompletableFuture.supplyAsync(() -> 5)\n    .thenApply(n -> n * 2)\n    .thenApply(n -> n + 1);\nSystem.out.println(futuro.join());",
          "answer": "11",
          "explanation": "thenApply() transforma el resultado y devuelve un nuevo CompletableFuture: 5 -> *2 = 10 -> +1 = 11. join() bloquea y devuelve el valor final (equivalente a get(), pero sin excepcion checked).",
          "conceptId": "completable-future-basics",
          "role": "solo",
          "pathOrder": 12,
          "dependsOn": ["callable-future-basics"]
        },
        {
          "id": "completable-future-combining-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "thenCompose() encadena una tarea que depende del resultado anterior; thenCombine() une dos CompletableFuture independientes que corren en paralelo",
          "code": "// thenCompose: la segunda tarea NECESITA el resultado de la primera\nCompletableFuture<Integer> dependiente = CompletableFuture.supplyAsync(() -> 5)\n    .thenCompose(n -> CompletableFuture.supplyAsync(() -> n * 10));\n\n// thenCombine: ambas tareas corren independientes, se combinan al final\nCompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> 5);\nCompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> 10);\nCompletableFuture<Integer> combinado = a.thenCombine(b, (x, y) -> x + y);",
          "answer": "ok",
          "explanation": "thenCompose recibe una funcion que devuelve OTRO CompletableFuture, usado cuando la siguiente tarea asincronica depende del resultado de la anterior (evita terminar con CompletableFuture<CompletableFuture<T>>). thenCombine toma dos CompletableFuture independientes (que pueden correr en paralelo) y los combina cuando ambos terminan.",
          "conceptId": "completable-future-combining",
          "role": "intro",
          "pathOrder": 13,
          "dependsOn": ["completable-future-basics"]
        },
        {
          "id": "completable-future-combining-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que thenCompose() evita terminar con un CompletableFuture<CompletableFuture<T>>?",
          "answer": "Porque 'aplana' el resultado: si la funcion que le pasas devuelve un CompletableFuture<T>, thenCompose() devuelve directamente CompletableFuture<T>, no uno anidado",
          "distractors": ["Porque ejecuta la segunda tarea en el mismo hilo que la primera", "Porque thenCompose() no permite que la segunda tarea sea asincronica", "Porque convierte automaticamente cualquier valor en un CompletableFuture, incluso si ya era uno"],
          "explanation": "thenApply() con una funcion que devuelve CompletableFuture<T> te dejaria con CompletableFuture<CompletableFuture<T>>. thenCompose() esta pensado exactamente para ese caso: aplana el resultado en un solo nivel, como flatMap en Streams/Optional.",
          "conceptId": "completable-future-combining",
          "role": "guided",
          "pathOrder": 14,
          "dependsOn": ["completable-future-basics"]
        },
        {
          "id": "completable-future-combining-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Tenes dos CompletableFuture<Integer> independientes, precioA y precioB, que consultan dos servicios distintos. Queres sumar ambos precios apenas los dos terminen. Que metodo usas?",
          "answer": "precioA.thenCombine(precioB, (a, b) -> a + b)",
          "distractors": ["precioA.thenCompose(a -> precioB)", "precioA.thenApply(a -> precioB.get() + a)", "CompletableFuture.supplyAsync(() -> precioA + precioB)"],
          "explanation": "thenCombine() es exactamente para dos CompletableFuture independientes que no dependen uno del otro: espera a que ambos terminen y aplica la funcion con los dos valores. thenCompose() seria para cuando el segundo depende del primero. Llamar precioB.get() dentro de thenApply funcionaria pero bloquearia un hilo innecesariamente, perdiendo la ventaja de la composicion asincronica.",
          "conceptId": "completable-future-combining",
          "role": "solo",
          "pathOrder": 15,
          "dependsOn": ["completable-future-basics"]
        }
      ]
```

- [ ] **Step 2: Verify the JSON is well-formed and the diff is grandfathering-safe**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/concurrency.json'))"`
Expected: no output.

Run: `git diff app/src/main/assets/content/concurrency.json` (this task's
commit only) and confirm every changed line for `conc-executors-01`
through `conc-executors-06` is purely an *addition*, and no other unit is
touched.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/concurrency.json
git commit -m "content: retrofit conc-executors and add CompletableFuture concepts"
```

---

### Task 3: Retrofit `conc-virtual-threads` + close the CopyOnWriteArrayList gap

**Files:**
- Modify: `app/src/main/assets/content/concurrency.json` (the
  `conc-virtual-threads` unit's `exercises` array only)

**Interfaces:**
- Produces: concepts `virtual-threads-basics` (pathOrder 0-2, no
  dependsOn), `virtual-threads-usage` (pathOrder 3-5, `dependsOn:
  ["virtual-threads-basics"]`), `concurrent-hashmap-basics` (pathOrder 6-8,
  no dependsOn), `atomic-basics` (pathOrder 9-11, no dependsOn),
  `copy-on-write-list` (pathOrder 12-14, `dependsOn:
  ["concurrent-hashmap-basics"]`).
- Consumes: nothing from Tasks 1-2 (different unit).

- [ ] **Step 1: Replace the unit's `exercises` array**

In `app/src/main/assets/content/concurrency.json`, find the
`conc-virtual-threads` unit's `"exercises": [ ... ]` array (currently 6
exercises, ids `conc-vt-01` through `conc-vt-06` — copy them verbatim from
the current file, do not retype from memory). Replace the entire array
with this exact final version — 15 exercises, 9 new plus the 6 existing
exercises verbatim with ladder fields added:

```json
      "exercises": [
        {
          "id": "virtual-threads-basics-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un virtual thread es un hilo liviano gestionado por la JVM (no 1:1 con un hilo del sistema operativo), pensado para crear miles sin agotar recursos",
          "code": "// Platform thread tradicional: 1:1 con un hilo del SO, costoso crear miles\nThread hiloTradicional = new Thread(tarea);\n\n// Virtual thread: gestionado por la JVM, barato crear miles\nThread hiloVirtual = Thread.ofVirtual().unstarted(tarea);",
          "answer": "ok",
          "explanation": "Los platform threads (los Thread de siempre) estan atados 1:1 a un hilo del sistema operativo, un recurso limitado y costoso. Los virtual threads (Java 21) son gestionados por la JVM y multiplexados sobre pocos carrier threads, permitiendo crear cientos de miles sin agotar memoria ni el sistema operativo.",
          "conceptId": "virtual-threads-basics",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "conc-vt-01",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que es un virtual thread (Java 21)?",
          "answer": "Un hilo ligero gestionado por la JVM, que no esta atado 1:1 a un hilo del sistema operativo",
          "distractors": ["Un hilo que simula ejecucion sin realmente correr codigo", "Un reemplazo de synchronized que elimina toda necesidad de locks", "Un hilo que solo puede ejecutar codigo asincronico con async/await"],
          "explanation": "Los virtual threads son gestionados por la JVM y multiplexados sobre pocos hilos del sistema operativo (carrier threads), permitiendo crear miles sin el costo de un hilo tradicional.",
          "conceptId": "virtual-threads-basics",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "conc-vt-03",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que problema practico resuelven los virtual threads frente a los hilos tradicionales (platform threads)?",
          "answer": "Permiten manejar decenas de miles de tareas concurrentes (por ejemplo, conexiones bloqueantes de I/O) sin agotar los recursos del sistema operativo",
          "distractors": ["Hacen que el codigo se ejecute mas rapido en un solo nucleo", "Eliminan la necesidad de sincronizacion entre hilos", "Reemplazan por completo a ExecutorService"],
          "explanation": "Cada platform thread consume memoria y recursos del sistema operativo; los virtual threads son mucho mas baratos, ideal para tareas que pasan la mayor parte del tiempo bloqueadas esperando I/O.",
          "conceptId": "virtual-threads-basics",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "virtual-threads-usage-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Executors.newVirtualThreadPerTaskExecutor() crea un virtual thread nuevo por cada tarea, en vez de reutilizar un pool fijo",
          "code": "try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {\n    executor.submit(() -> System.out.println(\"tarea 1\"));\n    executor.submit(() -> System.out.println(\"tarea 2\"));\n}",
          "answer": "ok",
          "explanation": "A diferencia de newFixedThreadPool(4), que limita a 4 hilos reutilizados, este executor lanza un virtual thread barato por cada tarea enviada - no hay limite fijo de pool porque los virtual threads son mucho mas livianos que los platform threads.",
          "conceptId": "virtual-threads-usage",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["virtual-threads-basics"]
        },
        {
          "id": "conc-vt-02",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa la creacion y ejecucion de un virtual thread:",
          "code": "Thread.ofVirtual()._____(() -> System.out.println(\"tarea\"));",
          "answer": "start",
          "distractors": ["run", "execute", "submit"],
          "explanation": "Thread.ofVirtual() devuelve un builder; start(Runnable) crea e inicia el virtual thread con esa tarea.",
          "conceptId": "virtual-threads-usage",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["virtual-threads-basics"]
        },
        {
          "id": "conc-vt-04",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que hace Executors.newVirtualThreadPerTaskExecutor()?",
          "answer": "Crea un nuevo virtual thread para cada tarea enviada, en vez de reutilizar un pool fijo de hilos",
          "distractors": ["Crea un unico hilo compartido para todas las tareas", "Limita la ejecucion a un maximo de 4 hilos", "Requiere declarar cada tarea como Callable, nunca Runnable"],
          "explanation": "A diferencia de un pool tradicional de tamano fijo, este executor lanza un virtual thread nuevo y barato por cada tarea, sin necesidad de gestionar un pool limitado.",
          "conceptId": "virtual-threads-usage",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["virtual-threads-basics"]
        },
        {
          "id": "concurrent-hashmap-basics-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "ConcurrentHashMap permite que varios hilos lean y escriban el mismo mapa de forma segura, sin bloquear todo el mapa como haria sincronizar un HashMap",
          "code": "Map<String, Integer> mapa = new ConcurrentHashMap<>();\nmapa.put(\"clave\", 1);\nmapa.computeIfAbsent(\"otra\", k -> 0);",
          "answer": "ok",
          "explanation": "HashMap no es thread-safe: acceso concurrente sin sincronizacion externa puede corromper su estructura interna. ConcurrentHashMap maneja la sincronizacion internamente usando bloqueos finos, permitiendo que varios hilos operen sobre partes distintas del mapa a la vez sin bloquear todo el mapa como haria un synchronized(mapa) global.",
          "conceptId": "concurrent-hashmap-basics",
          "role": "intro",
          "pathOrder": 6
        },
        {
          "id": "concurrent-hashmap-basics-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que conviene usar mapa.computeIfAbsent(clave, k -> nuevoValor()) en vez de un if (!mapa.containsKey(clave)) { mapa.put(...) } manual en un ConcurrentHashMap compartido entre hilos?",
          "answer": "Porque computeIfAbsent() es atomica: evita la race condition donde dos hilos verifican containsKey() como false al mismo tiempo y ambos terminan poniendo o sobreescribiendo el valor",
          "distractors": ["Porque containsKey() no existe en ConcurrentHashMap", "Porque computeIfAbsent() es mas rapido en programas de un solo hilo", "Porque put() no esta permitido dentro de un ConcurrentHashMap"],
          "explanation": "El patron 'verificar y luego actuar' (check-then-act) no es atomico: entre el containsKey() y el put(), otro hilo puede intercalarse. computeIfAbsent() (y merge(), compute(), putIfAbsent()) ejecutan la verificacion y la escritura como una sola operacion atomica sobre esa clave, evitando la race condition.",
          "conceptId": "concurrent-hashmap-basics",
          "role": "guided",
          "pathOrder": 7
        },
        {
          "id": "conc-vt-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que usarias ConcurrentHashMap en vez de HashMap cuando varios hilos acceden al mismo mapa?",
          "answer": "Porque HashMap no es thread-safe y puede corromperse o entrar en bucle infinito con acceso concurrente sin sincronizacion externa",
          "distractors": ["Porque HashMap no permite mas de un hilo lector", "Porque ConcurrentHashMap es mas rapido en programas de un solo hilo", "Porque HashMap no permite claves String"],
          "explanation": "HashMap no esta disenado para acceso concurrente y puede corromper su estructura interna; ConcurrentHashMap maneja la sincronizacion internamente sin bloquear todo el mapa.",
          "conceptId": "concurrent-hashmap-basics",
          "role": "solo",
          "pathOrder": 8
        },
        {
          "id": "atomic-basics-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "AtomicInteger ofrece operaciones como incrementAndGet() que son atomicas, evitando la race condition de un int normal incrementado desde varios hilos",
          "code": "AtomicInteger contador = new AtomicInteger(0);\ncontador.incrementAndGet(); // atomico: leer + sumar 1 + escribir, sin interrupcion de otro hilo",
          "answer": "ok",
          "explanation": "contador++ sobre un int normal es en realidad 3 pasos (leer, sumar, escribir) que no son atomicos: dos hilos pueden leer el mismo valor antes de que ninguno escriba, perdiendo un incremento. AtomicInteger implementa esa operacion completa de forma atomica usando soporte de hardware (CAS), sin necesitar un lock explicito.",
          "conceptId": "atomic-basics",
          "role": "intro",
          "pathOrder": 9
        },
        {
          "id": "atomic-basics-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que hace contador.compareAndSet(5, 10) en un AtomicInteger?",
          "answer": "Si el valor actual es 5, lo cambia a 10 y devuelve true; si no es 5, no hace nada y devuelve false",
          "distractors": ["Suma 5 y luego 10 al valor actual", "Siempre establece el valor en 10, sin importar el valor actual", "Lanza una excepcion si el valor actual no es 5"],
          "explanation": "compareAndSet(esperado, nuevo) es la operacion atomica base (CAS: compare-and-swap) detras de metodos como incrementAndGet(): compara el valor actual con el esperado, y solo si coinciden lo reemplaza, todo en un solo paso atomico sin lock.",
          "conceptId": "atomic-basics",
          "role": "guided",
          "pathOrder": 10
        },
        {
          "id": "conc-vt-06",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que ventaja tiene AtomicInteger sobre un int normal con incrementos desde varios hilos?",
          "answer": "Sus operaciones como incrementAndGet() son atomicas, evitando la race condition de un incremento no sincronizado",
          "distractors": ["AtomicInteger usa menos memoria que un int", "AtomicInteger no puede usarse dentro de un synchronized", "AtomicInteger convierte el codigo en asincronico automaticamente"],
          "explanation": "contador++ sobre un int normal no es atomico (es leer-modificar-escribir en 3 pasos); AtomicInteger garantiza que esa operacion completa sea atomica sin necesitar un lock explicito.",
          "conceptId": "atomic-basics",
          "role": "solo",
          "pathOrder": 11
        },
        {
          "id": "copy-on-write-list-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "CopyOnWriteArrayList crea una copia completa del arreglo interno en cada escritura, pensado para listas que se leen mucho y se escriben poco entre varios hilos",
          "code": "List<String> lista = new CopyOnWriteArrayList<>();\nlista.add(\"a\"); // crea una copia interna nueva del arreglo con \"a\" agregado\nfor (String s : lista) {\n    System.out.println(s); // iterar es seguro incluso si otro hilo modifica la lista al mismo tiempo\n}",
          "answer": "ok",
          "explanation": "Cada add()/remove() en CopyOnWriteArrayList copia todo el arreglo interno, lo que hace las escrituras costosas, pero a cambio la iteracion nunca lanza ConcurrentModificationException y nunca necesita sincronizarse: cada hilo que itera ve una foto (snapshot) del arreglo tal como estaba al empezar a iterar, sin importar que otro hilo modifique la lista en simultaneo.",
          "conceptId": "copy-on-write-list",
          "role": "intro",
          "pathOrder": 12,
          "dependsOn": ["concurrent-hashmap-basics"]
        },
        {
          "id": "copy-on-write-list-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que CopyOnWriteArrayList conviene cuando muchos hilos leen la lista y pocos la modifican, pero es mala eleccion si se escribe muy seguido?",
          "answer": "Porque cada escritura copia todo el arreglo interno, lo cual es costoso si hay muchas escrituras; las lecturas en cambio son rapidas porque no necesitan sincronizacion",
          "distractors": ["Porque CopyOnWriteArrayList no permite mas de un hilo lector a la vez", "Porque las escrituras son gratis pero las lecturas requieren sincronizacion explicita", "Porque la lista tiene un limite fijo de elementos que no se puede superar"],
          "explanation": "El costo de CopyOnWriteArrayList esta concentrado en la escritura (copiar todo el arreglo), no en la lectura. Con pocas escrituras y muchas lecturas concurrentes, ese costo se paga poco y se gana mucho al no necesitar sincronizar cada lectura; con escrituras frecuentes, copiar todo el arreglo una y otra vez se vuelve caro.",
          "conceptId": "copy-on-write-list",
          "role": "guided",
          "pathOrder": 13,
          "dependsOn": ["concurrent-hashmap-basics"]
        },
        {
          "id": "copy-on-write-list-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Un hilo itera sobre una List con un for-each mientras otro hilo le hace add() al mismo tiempo. Con un ArrayList normal esto lanza ConcurrentModificationException. Que pasa con un CopyOnWriteArrayList en el mismo escenario?",
          "answer": "No lanza excepcion: el hilo que itera sigue viendo la version (snapshot) de la lista que tenia al empezar a iterar, sin ver el elemento agregado por el otro hilo",
          "distractors": ["Tambien lanza ConcurrentModificationException, igual que ArrayList", "El programa queda bloqueado (deadlock) hasta que termine la iteracion", "El elemento agregado aparece inmediatamente en la iteracion en curso"],
          "explanation": "Como cada escritura crea un arreglo interno nuevo, el iterador de CopyOnWriteArrayList sigue apuntando al arreglo viejo (el snapshot tomado al crear el iterador) y nunca lanza ConcurrentModificationException, pero tampoco refleja cambios hechos durante esa iteracion en curso.",
          "conceptId": "copy-on-write-list",
          "role": "solo",
          "pathOrder": 14,
          "dependsOn": ["concurrent-hashmap-basics"]
        }
      ]
```

- [ ] **Step 2: Verify the JSON is well-formed and the diff is grandfathering-safe**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/concurrency.json'))"`
Expected: no output.

Run: `git diff app/src/main/assets/content/concurrency.json` (this task's
commit only) and confirm every changed line for `conc-vt-01` through
`conc-vt-06` is purely an *addition*, and no other unit is touched.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/concurrency.json
git commit -m "content: retrofit conc-virtual-threads and add CopyOnWriteArrayList concept"
```

---

### Task 4: Whole-corpus validation and content version bump

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Read-only validation: `app/src/main/assets/content/concurrency.json`

**Interfaces:**
- Consumes: the final state of `concurrency.json` after Tasks 1-3.
- Produces: `CURRENT_CONTENT_VERSION = "15"`, which is what triggers
  `ContentSeeder.seedIfNeeded()` to re-seed the database on next app
  launch — required for this content to actually reach a device.

- [ ] **Step 1: Write and run a full validation script**

Run this script (adjust nothing — it encodes every Global Constraint plus
a grandfathering check against the literal pre-Task-1 exercise objects
copied straight from this plan, not derived from git history commit
counting — robust even if a fix round added extra commits during any
task's review loop — and a reachability simulation matching the exact
algorithm in `GetTodaySessionUseCase.kt`):

```bash
python3 << 'EOF'
import json

path = "app/src/main/assets/content/concurrency.json"
data = json.load(open(path))

touched_units = {"conc-threads-lifecycle", "conc-executors", "conc-virtual-threads"}
untouched_units = {"conc-sincronizacion"}

all_units = {u["unitId"]: u for u in data["units"]}
assert touched_units | untouched_units == set(all_units), \
    f"unexpected unit set: {set(all_units)}"

# 1. Grandfathering: every pre-existing exercise id in the 3 touched units
#    keeps its 7 protected fields byte-identical to its ORIGINAL state
#    (copied verbatim below from the file as it existed before this plan).
protected_fields = ["id", "type", "prompt", "code", "answer", "distractors", "explanation"]
before_exercises_raw = [
    {"id": "conc-threads-01", "type": "mcq", "difficulty": 2, "prompt": "Cual es la diferencia entre llamar start() y run() en un Thread?", "answer": "start() crea un nuevo hilo de ejecucion y luego invoca run(); llamar run() directamente ejecuta el codigo en el hilo actual", "distractors": ["No hay diferencia, ambos hacen lo mismo", "run() siempre se ejecuta antes que start()", "start() solo funciona con Runnable, no con Thread"], "explanation": "start() es el metodo que realmente lanza un nuevo hilo del sistema operativo; run() es solo un metodo normal que, llamado directamente, se ejecuta secuencialmente en el hilo que lo invoca."},
    {"id": "conc-threads-02", "type": "fill_blank", "difficulty": 1, "prompt": "Completa la creacion del hilo a partir de la tarea:", "code": "Runnable tarea = () -> System.out.println(\"ejecutando\");\nThread hilo = new Thread(_____);\nhilo.start();", "answer": "tarea", "distractors": ["tarea.run()", "new Runnable(tarea)", "Thread.tarea"], "explanation": "El constructor de Thread recibe el Runnable directamente; pasar tarea.run() ejecutaria el codigo de inmediato en el hilo actual, sin crear un hilo nuevo."},
    {"id": "conc-threads-03", "type": "predict_output", "difficulty": 2, "prompt": "Que imprime este codigo (asumiendo que join() no lanza ninguna excepcion)?", "code": "Thread hilo = new Thread(() -> System.out.println(\"hilo\"));\nhilo.start();\nhilo.join();\nSystem.out.println(\"principal\");", "answer": "hilo\nprincipal", "explanation": "join() bloquea el hilo principal hasta que hilo termine, garantizando que \"hilo\" se imprima antes que \"principal\"."},
    {"id": "conc-threads-04", "type": "mcq", "difficulty": 2, "prompt": "En que estado queda un hilo que llamo a Thread.sleep(1000)?", "answer": "TIMED_WAITING", "distractors": ["RUNNABLE", "BLOCKED", "TERMINATED"], "explanation": "sleep() con un tiempo definido pone al hilo en TIMED_WAITING hasta que el tiempo expire o sea interrumpido."},
    {"id": "conc-threads-05", "type": "mcq", "difficulty": 2, "prompt": "Que problema practico resuelven los hilos que un programa de un solo hilo no puede resolver bien?", "answer": "Permiten que tareas independientes progresen en paralelo o se solapen, sin que una bloquee completamente a las demas", "distractors": ["Hacen que cualquier programa use menos memoria", "Eliminan la necesidad de manejar excepciones", "Garantizan que el codigo se ejecute en orden estricto"], "explanation": "Sin hilos, una tarea lenta (como I/O o un calculo pesado) bloquea todo el programa; los hilos permiten aprovechar multiples nucleos o no bloquear mientras se espera I/O."},
    {"id": "conc-threads-06", "type": "mcq", "difficulty": 3, "prompt": "Un hilo que intenta entrar a un bloque synchronized ya ocupado por otro hilo, en que estado queda?", "answer": "BLOCKED", "distractors": ["WAITING", "TIMED_WAITING", "NEW"], "explanation": "BLOCKED es especificamente el estado de esperar para adquirir un lock de un bloque o metodo synchronized; WAITING es para esperas indefinidas como wait() sin timeout."},
    {"id": "conc-executors-01", "type": "fill_blank", "difficulty": 1, "prompt": "Completa la creacion de un pool fijo de 4 hilos:", "code": "ExecutorService pool = Executors._____(4);", "answer": "newFixedThreadPool", "distractors": ["newCachedThreadPool", "newSingleThreadExecutor", "newScheduledThreadPool"], "explanation": "newFixedThreadPool(n) crea un pool con exactamente n hilos reutilizables."},
    {"id": "conc-executors-02", "type": "mcq", "difficulty": 2, "prompt": "Cual es la diferencia principal entre execute() y submit() en ExecutorService?", "answer": "submit() devuelve un Future para obtener el resultado o la excepcion; execute() no devuelve nada", "distractors": ["execute() es mas rapido porque no crea hilos", "submit() solo acepta Runnable, no Callable", "No hay diferencia real entre ambos"], "explanation": "submit() acepta Runnable o Callable y devuelve un Future; execute() (de la interfaz Executor) solo acepta Runnable y no da forma de recuperar resultados o excepciones."},
    {"id": "conc-executors-03", "type": "fill_blank", "difficulty": 2, "prompt": "Completa la interfaz que permite que una tarea devuelva un valor y declare excepciones checked:", "code": "_____<Integer> tarea = () -> 42;", "answer": "Callable", "distractors": ["Runnable", "Supplier", "Future"], "explanation": "Callable<V> tiene un metodo call() que devuelve V y puede lanzar excepciones checked, a diferencia de Runnable."},
    {"id": "conc-executors-04", "type": "predict_output", "difficulty": 2, "prompt": "Que imprime este codigo (asumiendo que get() no lanza ninguna excepcion)?", "code": "ExecutorService pool = Executors.newSingleThreadExecutor();\nFuture<Integer> resultado = pool.submit(() -> 10 + 20);\nSystem.out.println(resultado.get());\npool.shutdown();", "answer": "30", "explanation": "get() bloquea hasta que la tarea termine y devuelve el resultado del Callable, que aqui es 10 + 20 = 30."},
    {"id": "conc-executors-05", "type": "mcq", "difficulty": 2, "prompt": "Por que conviene usar un ExecutorService en vez de crear hilos nuevos manualmente para cada tarea?", "answer": "Reutiliza un numero controlado de hilos, evitando el costo de crear y destruir hilos constantemente y limitando el uso de recursos", "distractors": ["Porque los pools ejecutan las tareas mas rapido en cualquier caso", "Porque Thread ya no esta disponible en Java 21", "Porque los pools eliminan la necesidad de manejar excepciones"], "explanation": "Crear un hilo del sistema operativo tiene costo; un pool reutiliza hilos existentes y limita cuantos corren a la vez, evitando saturar el sistema."},
    {"id": "conc-executors-06", "type": "mcq", "difficulty": 3, "prompt": "Cual es la diferencia entre shutdown() y shutdownNow() en ExecutorService?", "answer": "shutdown() deja terminar las tareas ya enviadas y rechaza nuevas; shutdownNow() intenta interrumpir las tareas en ejecucion de inmediato", "distractors": ["Son exactamente lo mismo, solo cambia el nombre", "shutdown() detiene el pool inmediatamente sin esperar nada", "shutdownNow() solo funciona con Callable, no con Runnable"], "explanation": "shutdown() es un apagado ordenado (espera que las tareas en cola terminen); shutdownNow() intenta detener las tareas activas y devuelve las que quedaban pendientes."},
    {"id": "conc-vt-01", "type": "mcq", "difficulty": 2, "prompt": "Que es un virtual thread (Java 21)?", "answer": "Un hilo ligero gestionado por la JVM, que no esta atado 1:1 a un hilo del sistema operativo", "distractors": ["Un hilo que simula ejecucion sin realmente correr codigo", "Un reemplazo de synchronized que elimina toda necesidad de locks", "Un hilo que solo puede ejecutar codigo asincronico con async/await"], "explanation": "Los virtual threads son gestionados por la JVM y multiplexados sobre pocos hilos del sistema operativo (carrier threads), permitiendo crear miles sin el costo de un hilo tradicional."},
    {"id": "conc-vt-02", "type": "fill_blank", "difficulty": 2, "prompt": "Completa la creacion y ejecucion de un virtual thread:", "code": "Thread.ofVirtual()._____(() -> System.out.println(\"tarea\"));", "answer": "start", "distractors": ["run", "execute", "submit"], "explanation": "Thread.ofVirtual() devuelve un builder; start(Runnable) crea e inicia el virtual thread con esa tarea."},
    {"id": "conc-vt-03", "type": "mcq", "difficulty": 2, "prompt": "Que problema practico resuelven los virtual threads frente a los hilos tradicionales (platform threads)?", "answer": "Permiten manejar decenas de miles de tareas concurrentes (por ejemplo, conexiones bloqueantes de I/O) sin agotar los recursos del sistema operativo", "distractors": ["Hacen que el codigo se ejecute mas rapido en un solo nucleo", "Eliminan la necesidad de sincronizacion entre hilos", "Reemplazan por completo a ExecutorService"], "explanation": "Cada platform thread consume memoria y recursos del sistema operativo; los virtual threads son mucho mas baratos, ideal para tareas que pasan la mayor parte del tiempo bloqueadas esperando I/O."},
    {"id": "conc-vt-04", "type": "mcq", "difficulty": 3, "prompt": "Que hace Executors.newVirtualThreadPerTaskExecutor()?", "answer": "Crea un nuevo virtual thread para cada tarea enviada, en vez de reutilizar un pool fijo de hilos", "distractors": ["Crea un unico hilo compartido para todas las tareas", "Limita la ejecucion a un maximo de 4 hilos", "Requiere declarar cada tarea como Callable, nunca Runnable"], "explanation": "A diferencia de un pool tradicional de tamano fijo, este executor lanza un virtual thread nuevo y barato por cada tarea, sin necesidad de gestionar un pool limitado."},
    {"id": "conc-vt-05", "type": "mcq", "difficulty": 2, "prompt": "Por que usarias ConcurrentHashMap en vez de HashMap cuando varios hilos acceden al mismo mapa?", "answer": "Porque HashMap no es thread-safe y puede corromperse o entrar en bucle infinito con acceso concurrente sin sincronizacion externa", "distractors": ["Porque HashMap no permite mas de un hilo lector", "Porque ConcurrentHashMap es mas rapido en programas de un solo hilo", "Porque HashMap no permite claves String"], "explanation": "HashMap no esta disenado para acceso concurrente y puede corromper su estructura interna; ConcurrentHashMap maneja la sincronizacion internamente sin bloquear todo el mapa."},
    {"id": "conc-vt-06", "type": "mcq", "difficulty": 2, "prompt": "Que ventaja tiene AtomicInteger sobre un int normal con incrementos desde varios hilos?", "answer": "Sus operaciones como incrementAndGet() son atomicas, evitando la race condition de un incremento no sincronizado", "distractors": ["AtomicInteger usa menos memoria que un int", "AtomicInteger no puede usarse dentro de un synchronized", "AtomicInteger convierte el codigo en asincronico automaticamente"], "explanation": "contador++ sobre un int normal no es atomico (es leer-modificar-escribir en 3 pasos); AtomicInteger garantiza que esa operacion completa sea atomica sin necesitar un lock explicito."},
]
before_exercises = {e["id"]: e for e in before_exercises_raw}
assert len(before_exercises) == 18, f"expected 18 pre-existing exercises, got {len(before_exercises)}"

# 2. The untouched unit must be completely unchanged (whole-unit equality,
#    not just exercises) — no task in this plan should have touched it.
sincronizacion_original = {
    "unitId": "conc-sincronizacion",
    "name": "Sincronizacion",
    "certObjective": "concurrency",
    "orderIndex": 3,
    "summary": {
        "text": "Una race condition ocurre cuando varios hilos leen y modifican el mismo dato compartido sin coordinacion, y el resultado final depende del orden en que se intercalen. La palabra clave synchronized asegura que solo un hilo a la vez ejecute ese metodo o bloque. ReentrantLock ofrece lo mismo pero de forma explicita, con la ventaja de poder intentar el lock con timeout; siempre se libera en un finally.",
        "code": "private final ReentrantLock lock = new ReentrantLock();\n\nvoid incrementar() {\n    lock.lock();\n    try {\n        contador++;\n    } finally {\n        lock.unlock();\n    }\n}"
    },
    "exercises": [
        {"id": "conc-sync-01", "type": "mcq", "difficulty": 2, "prompt": "Que es una race condition (condicion de carrera)?", "answer": "Cuando el resultado de un programa depende del orden impredecible en que varios hilos acceden a datos compartidos", "distractors": ["Un error de compilacion por usar hilos sin declarar excepciones", "Una excepcion que se lanza cuando un hilo termina antes que otro", "Un tipo de deadlock que ocurre solo con ReentrantLock"], "explanation": "Una race condition ocurre cuando multiples hilos leen/escriben el mismo estado compartido sin sincronizacion, y el resultado final depende del orden de ejecucion."},
        {"id": "conc-sync-02", "type": "fill_blank", "difficulty": 1, "prompt": "Completa la palabra clave para que este metodo sea thread-safe:", "code": "public _____ void incrementar() {\n    contador++;\n}", "answer": "synchronized", "distractors": ["volatile", "final", "static"], "explanation": "synchronized en un metodo de instancia asegura que solo un hilo a la vez pueda ejecutar el metodo sobre el mismo objeto."},
        {"id": "conc-sync-03", "type": "mcq", "difficulty": 2, "prompt": "Que hace este bloque synchronized?", "code": "synchronized (lock) {\n    saldo = saldo - monto;\n}", "answer": "Asegura que solo un hilo a la vez pueda ejecutar ese bloque mientras tenga el lock sobre el objeto lock", "distractors": ["Crea un nuevo hilo para ejecutar el bloque", "Hace que la variable saldo sea inmutable", "Repite la operacion hasta que tenga exito"], "explanation": "Un bloque synchronized(objeto) adquiere el monitor de ese objeto antes de ejecutar el bloque, sirviendo como mutex."},
        {"id": "conc-sync-04", "type": "mcq", "difficulty": 3, "prompt": "Cuando conviene usar un bloque synchronized en vez de sincronizar todo el metodo?", "answer": "Cuando solo una parte del metodo toca datos compartidos, para reducir el tiempo que otros hilos quedan bloqueados", "distractors": ["Nunca, sincronizar todo el metodo siempre es mejor", "Solo cuando el metodo es static", "Un bloque synchronized no puede lanzar excepciones"], "explanation": "Sincronizar solo la seccion critica minimiza el tiempo que el lock esta tomado, mejorando la concurrencia real del programa."},
        {"id": "conc-sync-05", "type": "fill_blank", "difficulty": 2, "prompt": "Completa el patron correcto para liberar un ReentrantLock de forma segura:", "code": "lock.lock();\ntry {\n    seccionCritica();\n} finally {\n    lock._____();\n}", "answer": "unlock", "distractors": ["release", "close", "free"], "explanation": "unlock() debe llamarse en un finally para garantizar que el lock se libere incluso si seccionCritica() lanza una excepcion."},
        {"id": "conc-sync-06", "type": "mcq", "difficulty": 3, "prompt": "Que ventaja ofrece ReentrantLock sobre synchronized?", "answer": "Permite intentar adquirir el lock con un tiempo limite (tryLock) sin bloquear indefinidamente", "distractors": ["Es mas rapido en todos los casos posibles", "No requiere liberarse manualmente nunca", "Permite que dos hilos entren a la vez a la seccion critica"], "explanation": "tryLock(timeout) permite evitar bloqueos indefinidos, algo que synchronized no ofrece directamente."}
    ]
}
assert all_units["conc-sincronizacion"] == sincronizacion_original, \
    "conc-sincronizacion must be completely untouched in this sub-cycle"

total_exercises = 0
new_ids = []
for unit_id in touched_units:
    unit = all_units[unit_id]
    exercises = unit["exercises"]
    total_exercises += len(exercises)
    for e in exercises:
        if e["id"] in before_exercises:
            old = before_exercises[e["id"]]
            for f in protected_fields:
                assert e.get(f) == old.get(f), \
                    f"{e['id']}: field '{f}' changed (grandfathering violation)"
        else:
            new_ids.append(e["id"])

assert total_exercises == 39, f"expected 39 exercises across the 3 touched units, got {total_exercises}"
assert len(new_ids) == 21, f"expected 21 new exercises, got {len(new_ids)}: {new_ids}"

# 3. Case-collision rule.
for unit_id in touched_units:
    for e in all_units[unit_id]["exercises"]:
        ans = e.get("answer")
        for d in e.get("distractors", []):
            assert not (isinstance(ans, str) and d.lower() == ans.lower() and d != ans), \
                f"{e['id']}: distractor '{d}' differs from answer only by case"

# 4. One-terminal-role rule + dependsOn same-unit-only rule + sequential pathOrder.
for unit_id in touched_units:
    exercises = all_units[unit_id]["exercises"]
    concept_ids = {e["conceptId"] for e in exercises if e.get("conceptId")}
    terminal_counts = {}
    orders = []
    for e in exercises:
        orders.append(e.get("pathOrder"))
        if e.get("role") in ("solo", "practice"):
            cid = e["conceptId"]
            terminal_counts[cid] = terminal_counts.get(cid, 0) + 1
        for dep in e.get("dependsOn", []):
            assert dep in concept_ids, f"{unit_id}/{e['id']}: dependsOn '{dep}' not a concept in this unit"
    for cid in concept_ids:
        assert terminal_counts.get(cid) == 1, f"{unit_id}: concept '{cid}' has {terminal_counts.get(cid, 0)} terminal exercises, expected 1"
    assert orders == list(range(len(orders))), f"{unit_id}: pathOrder not sequential 0..n-1: {sorted(orders)}"

# 5. No accented characters (project-wide convention).
import re
raw = open(path, encoding="utf-8").read()
accented = re.findall(r"[À-ÿ]", raw)
assert not accented, f"found accented characters: {accented}"

print(f"All checks passed. {total_exercises} exercises across 3 units ({len(new_ids)} new).")

# 6. Reachability simulation, matching GetTodaySessionUseCase.selectPathExercises exactly.
def simulate(unit_id):
    exercises = all_units[unit_id]["exercises"]
    answered = set()
    sessions = 0
    limit = 5
    while len(answered) < len(exercises):
        born = {e["conceptId"] for e in exercises
                if e.get("conceptId") and e.get("role") in ("solo", "practice") and e["id"] in answered}
        candidates = [e for e in exercises if e["id"] not in answered and
                      (e.get("conceptId") is None or
                       (e["conceptId"] not in born and all(d in born for d in e.get("dependsOn", []))))]
        assert candidates, f"{unit_id}: STRANDED after {sessions} sessions, unreached: {[e['id'] for e in exercises if e['id'] not in answered]}"
        candidates.sort(key=lambda e: e.get("pathOrder") if e.get("pathOrder") is not None else 10**9)
        for e in candidates[:limit]:
            answered.add(e["id"])
        sessions += 1
    print(f"{unit_id}: drains cleanly in {sessions} sessions ({len(exercises)} exercises)")

for unit_id in touched_units:
    simulate(unit_id)
EOF
```

Expected: `All checks passed. 39 exercises across 3 units (21 new).`
followed by three "drains cleanly" lines, no assertion errors.

- [ ] **Step 2: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find:

```kotlin
private const val CURRENT_CONTENT_VERSION = "14"
```

Replace with:

```kotlin
private const val CURRENT_CONTENT_VERSION = "15"
```

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still passing (no test
changes needed — this task touches only a constant and a content asset).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: bump content version to 15 for Concurrencia sub-cycle 1"
```

---

## After the plan: manual on-device QA

Install a clean/in-place build and manually verify on-device (adb):

1. Navigate to "Concurrencia" > "Threads y ciclo de vida": confirm the two
   new intro cards ("start() lanza un hilo nuevo..." and "Un thread pasa
   por varios estados...") render correctly and the unit is still fully
   playable end to end, including the pre-existing exercises now wrapped
   in ladders.
2. Play through "Executors y thread pools" far enough to reach the new
   CompletableFuture concepts (`completable-future-basics`,
   `completable-future-combining`) — confirm the `dependsOn` gating works
   (they only appear after `callable-future-basics`'s solo exercise is
   answered) and the predict_output exercise (`completable-future-basics-solo`)
   grades "11" as correct.
3. Play through "Virtual threads y colecciones concurrentes" far enough to
   reach `copy-on-write-list` — confirm it only appears after
   `concurrent-hashmap-basics`'s solo exercise is answered (cross-concept
   `dependsOn` within the same unit).
4. Confirm review_state (SM-2 spaced-repetition) for any exercise you had
   already answered before this update (from a prior playthrough of this
   unit) is unaffected — i.e. it does not reappear as "new" content, only
   as a scheduled review if due.
