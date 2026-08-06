# Parallel Streams Unit (Streams y Lambdas) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new "Streams paralelos" unit to the already-shipped Streams
y Lambdas section (`app/src/main/assets/content/streams.json`), closing
the last deferred gap from the Concurrencia retrofit series: parallel
streams from the concurrency angle. This was deliberately moved out of
Concurrencia (Luis's call, `docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`
gap list originally grouped it there) to stay pedagogically adjacent to
the rest of the streams content instead of appearing two sections later.

**Architecture:** Pure content-authoring, append-only — a brand-new unit
with zero pre-existing exercises, so there is no grandfathering concern at
all (unlike every retrofit sub-cycle in this series). Zero Kotlin/Compose
changes beyond bumping `CURRENT_CONTENT_VERSION` in `ContentSeeder.kt`.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell (unchanged
this cycle), JUnit4 for the existing use-case test suite (must stay green
— no test changes needed since no Kotlin code changes).

## Global Constraints

- **Append-only — literal text insertion, never JSON load+dump.** A JSON
  library load+dump reformats every other array in the file (single-line
  arrays become multi-line) even though no values change, which has
  caused real regressions in this project before. Insert the new unit
  object (given verbatim below) as a new array element in `streams.json`'s
  `"units"` array using a text edit (e.g. the Edit tool's exact-string
  replacement), never a JSON parse-and-rewrite. Do not run the file
  through `json.load()` + `json.dump()` for editing purposes (reading it
  with `json.load()` for read-only validation in Task 2 is fine).
- **One-terminal-role rule:** every `conceptId` has exactly one exercise
  with `role: "solo"` or `role: "practice"` — never zero, never two.
- **dependsOn same-unit-only rule:** every id listed in a `dependsOn` array
  must be a `conceptId` that exists elsewhere in the *same* unit's
  `exercises` array.
- **Sequential pathOrder rule:** within the new unit, `pathOrder` values
  across all 6 exercises must be exactly `0, 1, 2, 3, 4, 5` with no gaps
  and no duplicates, in the same order as the array's physical layout.
- **Case-collision rule:** no `mcq`/`fill_blank` exercise's `distractors`
  may differ from its own `answer` only by letter case.
- **No accents in Spanish content** (project-wide convention, verified with
  `LC_ALL=C grep -nP "[\x80-\xFF]"` returning empty on the touched file).
  No voseo — tuteo only.
- **worked_example format:** every `intro`-role exercise has
  `"type": "worked_example"`, `"answer": "ok"`, no `distractors` field, and
  a `code` field showing the illustrative snippet.
- **No `predict_output` for this unit, ever.** Parallel stream execution
  order and thread-safety failures are inherently non-deterministic
  (confirmed empirically during design: an unsynchronized `ArrayList`
  shared across a `parallelStream().forEach()` lost a different, random
  number of elements on every run — see Task 1's factual-verification
  note). Every new exercise in this plan is `mcq` or `worked_example`,
  framed around *why* something is safe/unsafe or *when* to use a
  technique, never around predicting an exact runtime output.
- **Runtime-observable content must be verified against a real JDK, not
  just read.** Every factual claim in this plan (that `parallelStream()`
  uses `ForkJoinPool.commonPool()`, that sharing a non-thread-safe
  `ArrayList` across `forEach()` on a parallel stream loses elements, that
  `collect(Collectors.toList())` produces a complete and correctly
  ordered result even when parallel) was executed against a local JDK 20
  during design and confirmed correct — do not re-derive these from
  documentation alone if you need to modify them; re-run them.
- **Unit placement:** the new unit is appended at `orderIndex: 7`, after
  the existing `streams-optional` (orderIndex 6) — the last unit in the
  section today. `certObjective: "streams-lambdas"`, matching every other
  unit in this file (the file's `examVersion` at the top level already
  covers this — do not add a per-unit `examVersion` field, none of the
  existing units have one).
- **No interview-flavor exercise required.** Not every unit in this
  corpus has one (`streams-collectors` has zero) — this plan
  intentionally does not add one, to keep the new unit focused and
  minimal. If a future cycle wants to add one, it should follow the
  established convention: `conceptId`/`role`/`pathOrder`/`dependsOn` all
  `null` (sorts last), generic company framing, no real brand names.

---

### Task 1: Add the "Streams paralelos" unit

**Files:**
- Modify: `app/src/main/assets/content/streams.json` (append a new unit
  object to the `"units"` array, after the existing `streams-optional`
  unit)

**Interfaces:**
- Produces: concepts `parallel-basics` (pathOrder 0-2, no dependsOn) and
  `parallel-pitfalls` (pathOrder 3-5, `dependsOn: ["parallel-basics"]`).
- Consumes: nothing — this is a brand-new, self-contained unit. Does not
  touch any of the 6 existing units in this file.

- [ ] **Step 1: Insert the new unit object**

In `app/src/main/assets/content/streams.json`, find this exact trailing
text (the end of the `streams-optional` unit — its last exercise,
`streams-optional-interview` — followed by the closing of the
`exercises` array, the unit object, the `"units"` array, and the
top-level object; copy it verbatim from the current file, do not retype
from memory):

```json
          "explanation": "Un metodo que devuelve String puede devolver null sin que nada lo indique en la firma, y es facil olvidar el chequeo. Un metodo que devuelve Optional<String> comunica explicitamente 'esto puede no tener valor', y el equipo puede exigir que se maneje. Optional no elimina NPE magicamente (todavia se puede hacer optional.get() sin chequear y fallar), pero hace el riesgo visible."
        }
      ]
    }
  ]
}
```

Replace it with this exact text — the same `streams-optional-interview`
exercise and closing braces, now followed by a comma and the new
`streams-parallel` unit object, then the original closing `]` and `}`:

```json
          "explanation": "Un metodo que devuelve String puede devolver null sin que nada lo indique en la firma, y es facil olvidar el chequeo. Un metodo que devuelve Optional<String> comunica explicitamente 'esto puede no tener valor', y el equipo puede exigir que se maneje. Optional no elimina NPE magicamente (todavia se puede hacer optional.get() sin chequear y fallar), pero hace el riesgo visible."
        }
      ]
    },
    {
      "unitId": "streams-parallel",
      "name": "Streams paralelos",
      "certObjective": "streams-lambdas",
      "orderIndex": 7,
      "summary": {
        "text": "parallelStream() (o stream().parallel()) divide el trabajo de un stream entre varios hilos del ForkJoinPool comun, en vez de procesarlo todo secuencialmente en un solo hilo. Conviene para datasets grandes con calculos independientes entre si (CPU-bound); no conviene para datasets chicos o tareas I/O-bound, donde el costo de coordinar los hilos supera la ganancia. Compartir una coleccion no thread-safe (como ArrayList) dentro de un forEach() paralelo corrompe datos; collect() acumula resultados de forma segura, sin necesitar sincronizacion manual.",
        "code": "List<Integer> cuadrados = numeros.parallelStream()\n    .map(n -> n * n)\n    .collect(Collectors.toList()); // seguro y correcto, incluso en paralelo"
      },
      "exercises": [
        {
          "id": "streams-parallel-basics-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "parallelStream() (o stream().parallel()) divide el trabajo del stream entre varios hilos del ForkJoinPool comun, en vez de procesarlo todo secuencialmente en un solo hilo",
          "code": "List<Integer> numeros = List.of(1, 2, 3, 4, 5, 6, 7, 8);\nint suma = numeros.parallelStream()\n    .mapToInt(n -> n * n)\n    .sum(); // el calculo de cada n*n puede correr en hilos distintos",
          "answer": "ok",
          "explanation": "parallelStream() usa por defecto el ForkJoinPool.commonPool(), cuyo tamano depende de los nucleos disponibles en la maquina. Cada elemento (o un grupo de ellos) puede procesarse en un hilo distinto, y el resultado final se combina automaticamente -- pero el orden en que se procesan los elementos no esta garantizado.",
          "conceptId": "parallel-basics",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "streams-parallel-basics-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Para que tipo de tarea conviene usar un parallel stream?",
          "answer": "Para procesar grandes volumenes de datos con calculos independientes entre si (CPU-bound), donde el trabajo se puede repartir sin coordinacion entre elementos",
          "distractors": ["Para leer archivos o hacer llamadas de red (I/O-bound), donde los hilos igual esperan bloqueados", "Para colecciones pequenas, porque el overhead de paralelizar siempre es despreciable", "Para cualquier stream, porque parallelStream() siempre es mas rapido que stream()"],
          "explanation": "Paralelizar tiene un costo de coordinacion (dividir el trabajo, crear tareas, combinar resultados). Con datasets chicos o tareas I/O-bound (donde el hilo se bloquea esperando, no calculando), ese costo puede superar la ganancia -- por eso conviene medir, no asumir que parallelStream() siempre gana.",
          "conceptId": "parallel-basics",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "streams-parallel-basics-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que el orden de procesamiento de un parallelStream() no esta garantizado, aunque el resultado final (como sum() o collect()) sea correcto?",
          "answer": "Porque los elementos se distribuyen entre varios hilos que corren de forma independiente, y el sistema operativo decide cuando ejecuta cada uno",
          "distractors": ["Porque parallelStream() ordena los elementos al azar antes de procesarlos", "Porque el resultado final tambien es impredecible, no solo el orden de procesamiento", "Porque solo el primer elemento se procesa en paralelo, el resto es secuencial"],
          "explanation": "Operaciones como sum(), count() o collect() con un Collector correcto combinan los resultados parciales de forma que el resultado final es determinista, sin importar en que orden terminaron los hilos -- pero eso no significa que el procesamiento en si haya sido ordenado, solo la combinacion final.",
          "conceptId": "parallel-basics",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "streams-parallel-pitfalls-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Usar una coleccion no thread-safe dentro de un forEach() en un parallel stream puede corromper datos, porque varios hilos escriben a la vez",
          "code": "List<Integer> resultados = new ArrayList<>(); // NO es thread-safe\n\nnumeros.parallelStream()\n    .forEach(n -> resultados.add(n * n)); // PELIGRO: varios hilos llaman add() a la vez\n\n// Correcto: usar collect(), que maneja la combinacion de forma segura\nList<Integer> resultadosSeguro = numeros.parallelStream()\n    .map(n -> n * n)\n    .collect(Collectors.toList());",
          "answer": "ok",
          "explanation": "ArrayList.add() no es thread-safe: cuando varios hilos lo llaman al mismo tiempo desde un forEach() paralelo, pueden corromper la estructura interna de la lista o perder elementos. collect() con un Collector esta disenado especificamente para acumular resultados de un stream (paralelo o no) de forma segura, sin necesitar sincronizacion manual.",
          "conceptId": "parallel-pitfalls",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["parallel-basics"]
        },
        {
          "id": "streams-parallel-pitfalls-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que agregar elementos a una lista compartida dentro de un forEach() de un parallel stream es peligroso, aunque el mismo codigo funcione bien con un stream secuencial?",
          "answer": "Porque en el stream secuencial solo un hilo llama a add() a la vez (sin problema), pero en el paralelo varios hilos pueden llamarlo simultaneamente sobre la misma lista no thread-safe",
          "distractors": ["Porque forEach() no existe en streams paralelos", "Porque las listas nunca pueden usarse dentro de un stream, paralelo o no", "Porque el parallel stream ejecuta forEach() dos veces por cada elemento"],
          "explanation": "El bug no esta en forEach() en si, sino en compartir un objeto mutable no sincronizado entre los hilos que el stream paralelo usa internamente. Con un stream secuencial nunca hay mas de un hilo llamando al codigo del lambda a la vez, asi que el mismo patron 'peligroso' no rompe nada -- la seguridad del codigo dependia, sin que se notara, de que corriera secuencial.",
          "conceptId": "parallel-pitfalls",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["parallel-basics"]
        },
        {
          "id": "streams-parallel-pitfalls-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cual es la forma correcta de acumular resultados de un parallel stream en una lista, evitando el problema de una coleccion compartida no sincronizada?",
          "answer": "Usar collect(Collectors.toList()), que combina los resultados parciales de cada hilo de forma segura",
          "distractors": ["Usar forEach() con una lista sincronizada manualmente con synchronized en cada add()", "Usar forEach() y confiar en que ArrayList maneja la concurrencia internamente", "Convertir el parallel stream a secuencial antes de cada forEach(), perdiendo el paralelismo"],
          "explanation": "collect() esta disenado para trabajar correctamente con streams paralelos: cada hilo acumula resultados parciales en su propia estructura, y al final se combinan sin necesitar sincronizacion manual del programador. Sincronizar manualmente cada add() (opcion synchronized) funcionaria pero anula gran parte de la ganancia de paralelizar, ya que serializa justo la parte que se queria paralelizar.",
          "conceptId": "parallel-pitfalls",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["parallel-basics"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Verify the JSON is well-formed and the diff is append-only**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/streams.json'))"`
Expected: no output (valid JSON, no exception).

Run: `git diff app/src/main/assets/content/streams.json` and confirm the
*only* change is the insertion of the new `streams-parallel` unit object
— every byte of the 6 pre-existing units (`streams-creation`,
`streams-intermediate`, `streams-terminal`, `streams-collectors`,
`streams-functional-interfaces`, `streams-optional`) is untouched (the
diff should show only additions, zero deletions, except possibly the
single character changing the old last unit's trailing context if your
insertion point required editing a comma — verify no exercise inside any
existing unit changed).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/streams.json
git commit -m "content: add Streams paralelos unit (parallel streams from the concurrency angle)"
```

---

### Task 2: Whole-corpus validation and content version bump

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Read-only validation: `app/src/main/assets/content/streams.json`

**Interfaces:**
- Consumes: the final state of `streams.json` after Task 1.
- Produces: `CURRENT_CONTENT_VERSION`, bumped by one from whatever it is
  at dispatch time (check `ContentSeeder.kt`'s current value first — do
  not assume a specific number, since other cycles may have landed
  between this plan being written and executed).

- [ ] **Step 1: Write and run a full validation script**

```bash
python3 << 'EOF'
import json, re

path = "app/src/main/assets/content/streams.json"
data = json.load(open(path))

all_units = {u["unitId"]: u for u in data["units"]}
assert "streams-parallel" in all_units, "streams-parallel unit not found"
assert len(all_units) == 7, f"expected 7 units total, got {len(all_units)}"

# 1. The new unit has exactly 6 exercises, all new (no grandfathering to check --
#    this is a brand-new unit).
unit = all_units["streams-parallel"]
exercises = unit["exercises"]
assert len(exercises) == 6, f"expected 6 exercises, got {len(exercises)}"
assert unit["orderIndex"] == 7, f"expected orderIndex 7, got {unit['orderIndex']}"
assert unit["certObjective"] == "streams-lambdas", f"unexpected certObjective: {unit['certObjective']}"

# 2. The 6 pre-existing units must be byte-for-byte unchanged from before this
#    plan (exercise counts as a fast proxy check).
expected_counts = {
    "streams-creation": 3, "streams-intermediate": 8, "streams-terminal": 10,
    "streams-collectors": 10, "streams-functional-interfaces": 10, "streams-optional": 10,
}
for uid, expected in expected_counts.items():
    actual = len(all_units[uid]["exercises"])
    assert actual == expected, f"{uid}: expected {expected} exercises (unit should be untouched), got {actual}"
print("Pre-existing units' exercise counts unchanged; streams-parallel has 6 exercises at orderIndex 7.")

# 3. Case-collision rule.
for e in exercises:
    ans = e.get("answer")
    for d in e.get("distractors", []):
        assert not (isinstance(ans, str) and d.lower() == ans.lower() and d != ans), \
            f"{e['id']}: distractor '{d}' differs from answer only by case"

# 4. One-terminal-role rule + dependsOn same-unit-only rule + sequential pathOrder.
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

# 5. No accented characters (whole-file check) and no predict_output in the new unit.
raw = open(path, encoding="utf-8").read()
accented = re.findall(r"[À-ÿ]", raw)
assert not accented, f"found accented characters: {accented}"
predict_output_ids = [e["id"] for e in exercises if e.get("type") == "predict_output"]
assert not predict_output_ids, f"predict_output not allowed in this unit (non-deterministic content): {predict_output_ids}"

print(f"All standing-rule checks passed. {len(exercises)} exercises, {len(concept_ids)} concepts, 0 predict_output.")

# 6. Reachability simulation, matching GetTodaySessionUseCase.selectPathExercises exactly.
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
print(f"streams-parallel: drains cleanly in {sessions} sessions ({len(exercises)} exercises)")
EOF
```

Expected: `Pre-existing units' exercise counts unchanged; streams-parallel
has 6 exercises at orderIndex 7.` then `All standing-rule checks passed. 6
exercises, 2 concepts, 0 predict_output.` then `streams-parallel: drains
cleanly in 2 sessions (6 exercises)`, no assertion errors.

- [ ] **Step 2: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find the current `CURRENT_CONTENT_VERSION` value and increment it by one
(check the file first — do not assume a specific starting number).

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still passing (no test
changes needed — this task touches only a constant and a content asset).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: bump content version for Streams paralelos unit"
```

---

## After the plan: manual on-device QA

Install a clean/in-place build and manually verify on-device (adb):

1. Navigate to "Streams y Lambdas": confirm "Streams paralelos" appears as
   a new 7th unit, correctly locked/unlocked per the section's existing
   gating (it follows "Optional", so should unlock once that unit and any
   preceding checkpoint are satisfied — check current state on the QA
   device rather than assuming).
2. Play through the unit: confirm `parallel-pitfalls` only becomes
   reachable after `parallel-basics`'s solo exercise is answered
   (`dependsOn` gating).
3. Confirm both new `worked_example` intro cards render their code blocks
   correctly (multi-line snippets with comments).
4. Confirm all 4 new `mcq` exercises grade correctly for both a correct
   and an incorrect selection at least once.
5. Confirm the keyboard-layout fix from earlier in this project
   (`app/src/main/java/com/zconte/oopsapp/ui/components/ExerciseAnswerCard.kt`)
   is irrelevant here (this unit has no `fill_blank`/`predict_output`
   exercises, so no text input field appears) — nothing to verify on that
   front for this specific unit, noted only to avoid mistakenly assuming
   a gap in QA coverage.
