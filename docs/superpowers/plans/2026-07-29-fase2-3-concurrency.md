# Fase 2.3 — Concurrencia (Content Scaling) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the next section in the content roadmap — Concurrencia — as
real, playable content. Section #5 of 12 in the mapping already frozen in
`docs/superpowers/specs/2026-07-24-fase2-3-content-scaling-design.md`.

**Architecture:** No engine or UI changes — the "add a section = one JSON
pack + register it" architecture is already proven across three prior
cycles (Generics y Colecciones, and Manejo de Excepciones). This plan is
content-only: one new JSON asset, one registration edit, one
content-version bump.

**Tech Stack:** kotlinx.serialization JSON content packs (no Kotlin/Compose
code changes in this plan).

**Design doc:** `docs/superpowers/specs/2026-07-24-fase2-3-content-scaling-design.md`
(section mapping, decided once, covers this cycle too — no new spec
needed for a repeat content-curation cycle).

## Global Constraints

- Content prompts/explanations follow the existing style already in the 4
  packs shipped so far: no accent marks, no inverted `¿`/`¡` (e.g.
  `"Que imprime este codigo?"`, not `"¿Qué imprime este código?"`).
- Every unit mixes **three flavors** of question: exam/syntax (`fill_blank`,
  `predict_output`), code classification (`mcq` over a snippet or concept),
  and interview/judgment (`mcq` framed as "why/when/what problem does this
  solve"). Each of the 4 units below has at least one exercise of each
  flavor.
- **Scope boundary, important**: this section covers **virtual threads**
  (Java 21, core exam material per the roadmap table) but explicitly
  **excludes structured concurrency** — that feature is reserved for
  section #10 ("Extra Moderno", Java 22-25, explicitly labeled as
  non-exam content in the roadmap table). Do not add structured-concurrency
  exercises here.
- `predict_output` exercises in this section must have a **deterministic**
  expected output — avoid any exercise whose "correct" answer depends on
  unsynchronized multi-thread interleaving (which is inherently
  nondeterministic and would make the exercise unfair/unanswerable). Race
  conditions are covered as **conceptual** (`mcq`) exercises instead, never
  as a `predict_output` with a single guaranteed output.
- `ContentSeeder.CURRENT_CONTENT_VERSION` **must** be bumped (from `"5"` to
  `"6"`) for the new section to actually seed on devices that already have
  the app installed — the seeder is a no-op if the stored version already
  matches (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`).
  Reseeding wipes and reloads only `sections`/`units`/`exercises` — it does
  not touch `review_state`/`unit_progress`/`checkpoint_attempts`, so
  existing progress is preserved as long as no existing exercise/unit/section
  id is renamed or removed. This plan only adds new ids — no existing id
  changes.
- No Room schema change and no migration — content packs are plain JSON
  assets loaded at runtime.
- Section id for the new pack: `java-concurrency` (matches the
  `java-<topic>` convention). `orderIndex: 5` (next after Manejo de
  Excepciones at `orderIndex: 4`). Unit id prefix: `conc-`. `certObjective`
  for all its units: `concurrency` (matches the kebab-case slug convention
  used by `language-basics`, `generics-collections`, `streams-lambdas`,
  `exception-handling`).
- `examVersion` for the new pack: `"core"` (matches the other 4 packs —
  concurrency including virtual threads is core 1Z0-830 material).

---

### Task 1: Author and register the Concurrencia content pack

**Files:**
- Create: `app/src/main/assets/content/concurrency.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: nothing new — `ContentLoader`/`ContentSeeder` already parse any
  JSON pack matching the existing `Exercise`/`LearningUnit`/`Section` shape
  generically.
- Produces: nothing consumed by later tasks — this plan has only one task.

- [ ] **Step 1: Write the content pack**

Create `app/src/main/assets/content/concurrency.json`:

```json
{
  "sectionId": "java-concurrency",
  "name": "Concurrencia",
  "orderIndex": 5,
  "examVersion": "core",
  "units": [
    {
      "unitId": "conc-threads-lifecycle",
      "name": "Threads y ciclo de vida",
      "certObjective": "concurrency",
      "orderIndex": 1,
      "exercises": [
        {
          "id": "conc-threads-01",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual es la diferencia entre llamar start() y run() en un Thread?",
          "answer": "start() crea un nuevo hilo de ejecucion y luego invoca run(); llamar run() directamente ejecuta el codigo en el hilo actual",
          "distractors": ["No hay diferencia, ambos hacen lo mismo", "run() siempre se ejecuta antes que start()", "start() solo funciona con Runnable, no con Thread"],
          "explanation": "start() es el metodo que realmente lanza un nuevo hilo del sistema operativo; run() es solo un metodo normal que, llamado directamente, se ejecuta secuencialmente en el hilo que lo invoca."
        },
        {
          "id": "conc-threads-02",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa la creacion del hilo a partir de la tarea:",
          "code": "Runnable tarea = () -> System.out.println(\"ejecutando\");\nThread hilo = new Thread(_____);\nhilo.start();",
          "answer": "tarea",
          "distractors": ["tarea.run()", "new Runnable(tarea)", "Thread.tarea"],
          "explanation": "El constructor de Thread recibe el Runnable directamente; pasar tarea.run() ejecutaria el codigo de inmediato en el hilo actual, sin crear un hilo nuevo."
        },
        {
          "id": "conc-threads-03",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo (asumiendo que join() no lanza ninguna excepcion)?",
          "code": "Thread hilo = new Thread(() -> System.out.println(\"hilo\"));\nhilo.start();\nhilo.join();\nSystem.out.println(\"principal\");",
          "answer": "hilo\nprincipal",
          "explanation": "join() bloquea el hilo principal hasta que hilo termine, garantizando que \"hilo\" se imprima antes que \"principal\"."
        },
        {
          "id": "conc-threads-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "En que estado queda un hilo que llamo a Thread.sleep(1000)?",
          "answer": "TIMED_WAITING",
          "distractors": ["RUNNABLE", "BLOCKED", "TERMINATED"],
          "explanation": "sleep() con un tiempo definido pone al hilo en TIMED_WAITING hasta que el tiempo expire o sea interrumpido."
        },
        {
          "id": "conc-threads-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que problema practico resuelven los hilos que un programa de un solo hilo no puede resolver bien?",
          "answer": "Permiten que tareas independientes progresen en paralelo o se solapen, sin que una bloquee completamente a las demas",
          "distractors": ["Hacen que cualquier programa use menos memoria", "Eliminan la necesidad de manejar excepciones", "Garantizan que el codigo se ejecute en orden estricto"],
          "explanation": "Sin hilos, una tarea lenta (como I/O o un calculo pesado) bloquea todo el programa; los hilos permiten aprovechar multiples nucleos o no bloquear mientras se espera I/O."
        },
        {
          "id": "conc-threads-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Un hilo que intenta entrar a un bloque synchronized ya ocupado por otro hilo, en que estado queda?",
          "answer": "BLOCKED",
          "distractors": ["WAITING", "TIMED_WAITING", "NEW"],
          "explanation": "BLOCKED es especificamente el estado de esperar para adquirir un lock de un bloque o metodo synchronized; WAITING es para esperas indefinidas como wait() sin timeout."
        }
      ]
    },
    {
      "unitId": "conc-executors",
      "name": "Executors y thread pools",
      "certObjective": "concurrency",
      "orderIndex": 2,
      "exercises": [
        {
          "id": "conc-executors-01",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa la creacion de un pool fijo de 4 hilos:",
          "code": "ExecutorService pool = Executors._____(4);",
          "answer": "newFixedThreadPool",
          "distractors": ["newCachedThreadPool", "newSingleThreadExecutor", "newScheduledThreadPool"],
          "explanation": "newFixedThreadPool(n) crea un pool con exactamente n hilos reutilizables."
        },
        {
          "id": "conc-executors-02",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual es la diferencia principal entre execute() y submit() en ExecutorService?",
          "answer": "submit() devuelve un Future para obtener el resultado o la excepcion; execute() no devuelve nada",
          "distractors": ["execute() es mas rapido porque no crea hilos", "submit() solo acepta Runnable, no Callable", "No hay diferencia real entre ambos"],
          "explanation": "submit() acepta Runnable o Callable y devuelve un Future; execute() (de la interfaz Executor) solo acepta Runnable y no da forma de recuperar resultados o excepciones."
        },
        {
          "id": "conc-executors-03",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa la interfaz que permite que una tarea devuelva un valor y declare excepciones checked:",
          "code": "_____<Integer> tarea = () -> 42;",
          "answer": "Callable",
          "distractors": ["Runnable", "Supplier", "Future"],
          "explanation": "Callable<V> tiene un metodo call() que devuelve V y puede lanzar excepciones checked, a diferencia de Runnable."
        },
        {
          "id": "conc-executors-04",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo (asumiendo que get() no lanza ninguna excepcion)?",
          "code": "ExecutorService pool = Executors.newSingleThreadExecutor();\nFuture<Integer> resultado = pool.submit(() -> 10 + 20);\nSystem.out.println(resultado.get());\npool.shutdown();",
          "answer": "30",
          "explanation": "get() bloquea hasta que la tarea termine y devuelve el resultado del Callable, que aqui es 10 + 20 = 30."
        },
        {
          "id": "conc-executors-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que conviene usar un ExecutorService en vez de crear hilos nuevos manualmente para cada tarea?",
          "answer": "Reutiliza un numero controlado de hilos, evitando el costo de crear y destruir hilos constantemente y limitando el uso de recursos",
          "distractors": ["Porque los pools ejecutan las tareas mas rapido en cualquier caso", "Porque Thread ya no esta disponible en Java 21", "Porque los pools eliminan la necesidad de manejar excepciones"],
          "explanation": "Crear un hilo del sistema operativo tiene costo; un pool reutiliza hilos existentes y limita cuantos corren a la vez, evitando saturar el sistema."
        },
        {
          "id": "conc-executors-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cual es la diferencia entre shutdown() y shutdownNow() en ExecutorService?",
          "answer": "shutdown() deja terminar las tareas ya enviadas y rechaza nuevas; shutdownNow() intenta interrumpir las tareas en ejecucion de inmediato",
          "distractors": ["Son exactamente lo mismo, solo cambia el nombre", "shutdown() detiene el pool inmediatamente sin esperar nada", "shutdownNow() solo funciona con Callable, no con Runnable"],
          "explanation": "shutdown() es un apagado ordenado (espera que las tareas en cola terminen); shutdownNow() intenta detener las tareas activas y devuelve las que quedaban pendientes."
        }
      ]
    },
    {
      "unitId": "conc-sincronizacion",
      "name": "Sincronizacion",
      "certObjective": "concurrency",
      "orderIndex": 3,
      "exercises": [
        {
          "id": "conc-sync-01",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que es una race condition (condicion de carrera)?",
          "answer": "Cuando el resultado de un programa depende del orden impredecible en que varios hilos acceden a datos compartidos",
          "distractors": ["Un error de compilacion por usar hilos sin declarar excepciones", "Una excepcion que se lanza cuando un hilo termina antes que otro", "Un tipo de deadlock que ocurre solo con ReentrantLock"],
          "explanation": "Una race condition ocurre cuando multiples hilos leen/escriben el mismo estado compartido sin sincronizacion, y el resultado final depende del orden de ejecucion."
        },
        {
          "id": "conc-sync-02",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa la palabra clave para que este metodo sea thread-safe:",
          "code": "public _____ void incrementar() {\n    contador++;\n}",
          "answer": "synchronized",
          "distractors": ["volatile", "final", "static"],
          "explanation": "synchronized en un metodo de instancia asegura que solo un hilo a la vez pueda ejecutar el metodo sobre el mismo objeto."
        },
        {
          "id": "conc-sync-03",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que hace este bloque synchronized?",
          "code": "synchronized (lock) {\n    saldo = saldo - monto;\n}",
          "answer": "Asegura que solo un hilo a la vez pueda ejecutar ese bloque mientras tenga el lock sobre el objeto lock",
          "distractors": ["Crea un nuevo hilo para ejecutar el bloque", "Hace que la variable saldo sea inmutable", "Repite la operacion hasta que tenga exito"],
          "explanation": "Un bloque synchronized(objeto) adquiere el monitor de ese objeto antes de ejecutar el bloque, sirviendo como mutex."
        },
        {
          "id": "conc-sync-04",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cuando conviene usar un bloque synchronized en vez de sincronizar todo el metodo?",
          "answer": "Cuando solo una parte del metodo toca datos compartidos, para reducir el tiempo que otros hilos quedan bloqueados",
          "distractors": ["Nunca, sincronizar todo el metodo siempre es mejor", "Solo cuando el metodo es static", "Un bloque synchronized no puede lanzar excepciones"],
          "explanation": "Sincronizar solo la seccion critica minimiza el tiempo que el lock esta tomado, mejorando la concurrencia real del programa."
        },
        {
          "id": "conc-sync-05",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el patron correcto para liberar un ReentrantLock de forma segura:",
          "code": "lock.lock();\ntry {\n    seccionCritica();\n} finally {\n    lock._____();\n}",
          "answer": "unlock",
          "distractors": ["release", "close", "free"],
          "explanation": "unlock() debe llamarse en un finally para garantizar que el lock se libere incluso si seccionCritica() lanza una excepcion."
        },
        {
          "id": "conc-sync-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que ventaja ofrece ReentrantLock sobre synchronized?",
          "answer": "Permite intentar adquirir el lock con un tiempo limite (tryLock) sin bloquear indefinidamente",
          "distractors": ["Es mas rapido en todos los casos posibles", "No requiere liberarse manualmente nunca", "Permite que dos hilos entren a la vez a la seccion critica"],
          "explanation": "tryLock(timeout) permite evitar bloqueos indefinidos, algo que synchronized no ofrece directamente."
        }
      ]
    },
    {
      "unitId": "conc-virtual-threads",
      "name": "Virtual threads y colecciones concurrentes",
      "certObjective": "concurrency",
      "orderIndex": 4,
      "exercises": [
        {
          "id": "conc-vt-01",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que es un virtual thread (Java 21)?",
          "answer": "Un hilo ligero gestionado por la JVM, que no esta atado 1:1 a un hilo del sistema operativo",
          "distractors": ["Un hilo que simula ejecucion sin realmente correr codigo", "Un reemplazo de synchronized que elimina toda necesidad de locks", "Un hilo que solo puede ejecutar codigo asincronico con async/await"],
          "explanation": "Los virtual threads son gestionados por la JVM y multiplexados sobre pocos hilos del sistema operativo (carrier threads), permitiendo crear miles sin el costo de un hilo tradicional."
        },
        {
          "id": "conc-vt-02",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa la creacion y ejecucion de un virtual thread:",
          "code": "Thread.ofVirtual()._____(() -> System.out.println(\"tarea\"));",
          "answer": "start",
          "distractors": ["run", "execute", "submit"],
          "explanation": "Thread.ofVirtual() devuelve un builder; start(Runnable) crea e inicia el virtual thread con esa tarea."
        },
        {
          "id": "conc-vt-03",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que problema practico resuelven los virtual threads frente a los hilos tradicionales (platform threads)?",
          "answer": "Permiten manejar decenas de miles de tareas concurrentes (por ejemplo, conexiones bloqueantes de I/O) sin agotar los recursos del sistema operativo",
          "distractors": ["Hacen que el codigo se ejecute mas rapido en un solo nucleo", "Eliminan la necesidad de sincronizacion entre hilos", "Reemplazan por completo a ExecutorService"],
          "explanation": "Cada platform thread consume memoria y recursos del sistema operativo; los virtual threads son mucho mas baratos, ideal para tareas que pasan la mayor parte del tiempo bloqueadas esperando I/O."
        },
        {
          "id": "conc-vt-04",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que hace Executors.newVirtualThreadPerTaskExecutor()?",
          "answer": "Crea un nuevo virtual thread para cada tarea enviada, en vez de reutilizar un pool fijo de hilos",
          "distractors": ["Crea un unico hilo compartido para todas las tareas", "Limita la ejecucion a un maximo de 4 hilos", "Requiere declarar cada tarea como Callable, nunca Runnable"],
          "explanation": "A diferencia de un pool tradicional de tamano fijo, este executor lanza un virtual thread nuevo y barato por cada tarea, sin necesidad de gestionar un pool limitado."
        },
        {
          "id": "conc-vt-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que usarias ConcurrentHashMap en vez de HashMap cuando varios hilos acceden al mismo mapa?",
          "answer": "Porque HashMap no es thread-safe y puede corromperse o entrar en bucle infinito con acceso concurrente sin sincronizacion externa",
          "distractors": ["Porque HashMap no permite mas de un hilo lector", "Porque ConcurrentHashMap es mas rapido en programas de un solo hilo", "Porque HashMap no permite claves String"],
          "explanation": "HashMap no esta disenado para acceso concurrente y puede corromper su estructura interna; ConcurrentHashMap maneja la sincronizacion internamente sin bloquear todo el mapa."
        },
        {
          "id": "conc-vt-06",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que ventaja tiene AtomicInteger sobre un int normal con incrementos desde varios hilos?",
          "answer": "Sus operaciones como incrementAndGet() son atomicas, evitando la race condition de un incremento no sincronizado",
          "distractors": ["AtomicInteger usa menos memoria que un int", "AtomicInteger no puede usarse dentro de un synchronized", "AtomicInteger convierte el codigo en asincronico automaticamente"],
          "explanation": "contador++ sobre un int normal no es atomico (es leer-modificar-escribir en 3 pasos); AtomicInteger garantiza que esa operacion completa sea atomica sin necesitar un lock explicito."
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Register the new pack and bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
change line 12:

```kotlin
private const val CURRENT_CONTENT_VERSION = "5"
```

to:

```kotlin
private const val CURRENT_CONTENT_VERSION = "6"
```

And change the `packAssetPaths` list:

```kotlin
    private val packAssetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json",
        "content/exception-handling.json"
    )
```

to:

```kotlin
    private val packAssetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json",
        "content/exception-handling.json",
        "content/concurrency.json"
    )
```

(List order here does not affect Ruta's displayed order — that comes from
each pack's own `orderIndex` field — but listing them in roadmap order
keeps the file easy to scan.)

- [ ] **Step 3: Validate the new file is valid JSON**

Run: `python3 -m json.tool app/src/main/assets/content/concurrency.json > /dev/null && echo VALID`
Expected: `VALID` printed, no errors.

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — this is the regression check that the new
content and the version bump don't break anything (no dedicated content
test exists beyond the generic `ContentPackParsingTest`, which doesn't
load real asset files).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/content/concurrency.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: add Concurrencia section (4 units, 24 exercises)"
```

---

## After the task: manual on-device QA

No automated UI/content test covers full playability. Once merged, install
a clean build and manually verify on-device (SM-A505G, same pattern as
prior phases):

1. Ruta shows sections in order: Fundamentos de Java → Genericos y
   Colecciones → Streams y lambdas → Manejo de Excepciones → Concurrencia.
2. Play through Concurrencia: confirm all 4 units are playable, confirm
   each of the 3 question flavors appears somewhere across the section,
   confirm the `predict_output` exercises (join ordering, Future.get())
   grade correctly.
3. Confirm the section's checkpoint appears after finishing all its units.
4. Confirm the placement/skip checkpoint correctly offers to skip over
   Concurrencia (and prior sections) if a later section is added in a
   future cycle and someone jumps ahead.
