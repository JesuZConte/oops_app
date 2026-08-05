# Genericos y Colecciones — Sub-ciclo 2 (Sequenced Collections + ConcurrentModificationException) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining Generics y Colecciones objective gaps:
`ConcurrentModificationException` (currently untouched anywhere in the
section) and Sequenced Collections — `SequencedCollection`/`SequencedSet`/
`SequencedMap`, a Java 21 addition (`getFirst`/`getLast`/`addFirst`/
`addLast`/`reversed()`, `firstEntry`/`putFirst` on maps). Second and final
sub-cycle bringing this section to full 1Z0-830 coverage, per
`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`. Also lightly
retrofits "Listas y Sets" with a first-exposure ladder for the new
`ConcurrentModificationException` concept, per
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md`.

**Architecture:** Content-only — one JSON pack edit, one version bump, no
Kotlin/Compose changes. Two kinds of edit: (a) **append-only retrofit** of
`gencol-lists-sets` — 3 brand-new exercises for a concept none of its 6
existing exercises touch, so no existing exercise needs retagging or
`dependsOn` changes (unlike sub-cycle 1's `gencol-generics` retrofit,
which reused existing exercises as ladder terminals); (b) **1 new unit
appended**: `gencol-sequenced-collections`.

**Tech Stack:** kotlinx.serialization JSON content packs (no Kotlin/Compose
code changes in this plan).

**Design docs:** `docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`
(objective detail this plan closes) and
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md` (ladders policy).

## Global Constraints

- Content prompts/explanations: no accent marks, no inverted `¿`/`¡`.
- **Ladders policy:** every genuinely new concept gets a full
  `worked_example` (role `intro`) → `guided` → `solo` sequence, sharing one
  `conceptId`, sequential `pathOrder` within its unit.
- **One-terminal-role rule:** every `conceptId` must have **exactly one**
  exercise with `role: "solo"` or `role: "practice"` — never zero, never
  more than one. Every concept below has exactly one terminal role —
  verify this explicitly in Step 3 before considering the task done.
- **DependsOn-matches-siblings rule (learned from sub-cycle 1's final
  review):** if a concept's `intro`/`guided` carry a `dependsOn`, every
  other exercise in that same concept's ladder (its `solo`) must carry the
  identical `dependsOn` — this plan already applies it correctly
  throughout, verify it in Step 3 too.
- **Grading-safety rule:** for `mcq`/`fill_blank`, no distractor may differ
  from the `answer` only by capitalization (case-insensitive grading).
- Every unit mixes exam/syntax, code-classification, and interview flavors.
  Interview prompts use generic company framing ("una consultora IT
  grande", "una empresa de servicios financieros") — never real brand
  names.
- The new unit includes a `summary: {text, code}` field. Do not add a
  `summary` to `gencol-lists-sets` — it already has one, unchanged.
- `ContentSeeder`'s `CURRENT_CONTENT_VERSION`
  (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`)
  must bump from `"11"` to `"12"`.
- `generics-collections.json` is already registered in
  `ContentPackRegistry.kt` — no registry edit needed.
- New unit id prefix: `gencol-`. `certObjective`: `generics-collections`.
  `orderIndex` 6, appended after `gencol-arrays` (5).
- No Room schema change, no migration.
- **Grandfathering is not a concern for this task's retrofit** — the 3 new
  `gencol-lists-sets` exercises are pure additions, no existing exercise's
  id/type/content/fields are touched at all. Still, do not modify any of
  the 6 existing `gencol-listssets-*` exercises.

---

### Task 1: Retrofit `gencol-lists-sets` and append the `gencol-sequenced-collections` unit

**Files:**
- Modify: `app/src/main/assets/content/generics-collections.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: nothing new — same generic JSON pack parsing already proven.
- Produces: nothing — this plan has only one task.

- [ ] **Step 1: Append 3 new exercises to `gencol-lists-sets`**

Find the unit with `"unitId": "gencol-lists-sets"`. Its `exercises` array
currently has 6 objects ending with `gencol-listssets-06`. Append these 3
new exercises to the END of that array (after `gencol-listssets-06`,
remembering the comma):

```json
{
  "id": "gencol-cme-intro",
  "type": "worked_example",
  "difficulty": 2,
  "prompt": "Modificar una coleccion mientras la recorres con un for-each lanza ConcurrentModificationException",
  "code": "List<String> nombres = new ArrayList<>(List.of(\"Ana\", \"Bob\", \"Carla\"));\nfor (String n : nombres) {\n    if (n.equals(\"Bob\")) {\n        nombres.remove(n); // ConcurrentModificationException\n    }\n}",
  "answer": "ok",
  "explanation": "El for-each usa un Iterator internamente; modificar la coleccion directamente (no a traves del iterator) invalida su estado interno y la siguiente llamada a next() detecta el cambio y lanza la excepcion, en vez de fallar silenciosamente.",
  "conceptId": "concurrent-modification",
  "role": "intro",
  "pathOrder": 0
},
{
  "id": "gencol-cme-guided",
  "type": "mcq",
  "difficulty": 3,
  "prompt": "Como se puede eliminar elementos de una lista de forma segura mientras se recorre?",
  "answer": "Usando el metodo remove() del propio Iterator (o Collection.removeIf()), no el remove() de la lista directamente",
  "distractors": ["No hay forma segura de eliminar elementos durante un recorrido", "Usando un for clasico con indice siempre evita el problema sin excepciones", "Convirtiendo la lista a un array antes de recorrerla soluciona el problema automaticamente"],
  "explanation": "Iterator.remove() (o removeIf(), que lo usa internamente) actualiza el estado interno del iterador de forma consistente; remove() de la coleccion, en cambio, no le avisa al iterator activo, causando la inconsistencia detectada.",
  "conceptId": "concurrent-modification",
  "role": "guided",
  "pathOrder": 1
},
{
  "id": "gencol-cme-solo",
  "type": "predict_output",
  "difficulty": 3,
  "prompt": "Que imprime este codigo?",
  "code": "List<Integer> numeros = new ArrayList<>(List.of(1, 2, 3));\ntry {\n    for (Integer n : numeros) {\n        if (n == 2) numeros.remove(n);\n    }\n} catch (ConcurrentModificationException e) {\n    System.out.println(\"capturada\");\n}",
  "answer": "capturada",
  "explanation": "Al remover 2 directamente de la lista durante el for-each, el iterator detecta la modificacion no autorizada en la siguiente llamada a next() y lanza ConcurrentModificationException, que el catch captura.",
  "conceptId": "concurrent-modification",
  "role": "solo",
  "pathOrder": 2
}
```

- [ ] **Step 2: Append the `gencol-sequenced-collections` unit**

Add this unit as the new last element of `generics-collections.json`'s
`units` array (after `gencol-arrays`, remembering the comma):

```json
    {
      "unitId": "gencol-sequenced-collections",
      "name": "Sequenced Collections",
      "certObjective": "generics-collections",
      "orderIndex": 6,
      "summary": {
        "text": "Java 21 agrego las interfaces SequencedCollection, SequencedSet y SequencedMap, que dan una forma uniforme de trabajar con el primer y ultimo elemento de cualquier coleccion con orden definido - List, LinkedHashSet, LinkedHashMap, etc. Los metodos comunes son getFirst()/getLast(), addFirst()/addLast(), removeFirst()/removeLast(), y reversed(), que devuelve una vista invertida de la coleccion sin copiar los datos.",
        "code": "List<Integer> numeros = new ArrayList<>(List.of(1, 2, 3));\nnumeros.getFirst(); // 1\nnumeros.getLast();  // 3\nnumeros.addFirst(0); // [0, 1, 2, 3]\n\nList<Integer> invertida = numeros.reversed(); // vista, no copia"
      },
      "exercises": [
        {
          "id": "gencol-sequenced-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "SequencedCollection da acceso uniforme al primer y ultimo elemento de cualquier coleccion ordenada",
          "code": "List<Integer> numeros = new ArrayList<>(List.of(1, 2, 3));\nnumeros.getFirst(); // 1\nnumeros.getLast();  // 3\nnumeros.addFirst(0); // [0, 1, 2, 3]\nnumeros.removeLast(); // [0, 1, 2]",
          "answer": "ok",
          "explanation": "Antes de Java 21, List no tenia un metodo directo para el primer/ultimo elemento (habia que usar get(0) o get(size()-1), propenso a errores); SequencedCollection unifica esa operacion para List, Deque, LinkedHashSet y otras colecciones con orden definido.",
          "conceptId": "sequenced-basico",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "gencol-sequenced-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Antes de Java 21, como se obtenia el ultimo elemento de una List sin SequencedCollection?",
          "answer": "lista.get(lista.size() - 1), propenso a errores de indice",
          "distractors": ["lista.getLast(), que ya existia desde Java 8", "No habia ninguna forma de acceder al ultimo elemento", "lista.last(), un metodo heredado de Collection"],
          "explanation": "getLast()/getFirst() son nuevos de Java 21 (SequencedCollection); antes, la unica forma directa era calcular el ultimo indice manualmente con size() - 1.",
          "conceptId": "sequenced-basico",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "gencol-sequenced-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "List<String> nombres = new ArrayList<>(List.of(\"Ana\", \"Bob\"));\nnombres.addFirst(\"Zoe\");\nSystem.out.println(nombres);",
          "answer": "[Zoe, Ana, Bob]",
          "explanation": "addFirst() inserta el elemento al comienzo de la lista, desplazando el resto; el resultado queda con Zoe como primer elemento.",
          "conceptId": "sequenced-basico",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "gencol-reversed-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "reversed() devuelve una vista invertida de la coleccion, sin copiar los datos",
          "code": "List<Integer> numeros = new ArrayList<>(List.of(1, 2, 3));\nList<Integer> invertida = numeros.reversed(); // [3, 2, 1], vista\n\nnumeros.addLast(4);\nSystem.out.println(invertida); // [4, 3, 2, 1] - refleja el cambio",
          "answer": "ok",
          "explanation": "reversed() no crea una copia invertida: devuelve una vista que sigue conectada a la coleccion original - cualquier cambio en una se refleja en la otra, en ambas direcciones.",
          "conceptId": "sequenced-reversed",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["sequenced-basico"]
        },
        {
          "id": "gencol-reversed-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Si modificas la lista invertida devuelta por reversed(), que pasa con la lista original?",
          "answer": "Tambien se modifica, porque reversed() devuelve una vista conectada a la coleccion original, no una copia",
          "distractors": ["No pasa nada, son colecciones completamente independientes", "Se lanza una excepcion, la vista invertida es de solo lectura", "Solo se modifica si se llama a un metodo especial de sincronizacion"],
          "explanation": "reversed() es una vista (como subList() o keySet()), no una copia - las modificaciones se propagan en ambas direcciones porque ambas referencian la misma estructura de datos subyacente.",
          "conceptId": "sequenced-reversed",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["sequenced-basico"]
        },
        {
          "id": "gencol-reversed-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "List<Integer> numeros = new ArrayList<>(List.of(1, 2, 3));\nList<Integer> invertida = numeros.reversed();\ninvertida.addFirst(4);\nSystem.out.println(numeros);",
          "answer": "[1, 2, 3, 4]",
          "explanation": "addFirst(4) sobre la vista invertida agrega 4 al principio de esa vista, lo cual equivale a agregarlo al FINAL de la lista original - reversed() invierte tambien el significado de first/last.",
          "conceptId": "sequenced-reversed",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["sequenced-basico"]
        },
        {
          "id": "gencol-seqmap-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "SequencedMap agrega firstEntry()/lastEntry() y putFirst()/putLast() a los Maps ordenados",
          "code": "LinkedHashMap<String, Integer> edades = new LinkedHashMap<>();\nedades.put(\"Ana\", 30);\nedades.put(\"Bob\", 25);\n\nedades.firstEntry(); // Ana=30\nedades.putFirst(\"Zoe\", 40); // Zoe queda primero",
          "answer": "ok",
          "explanation": "SequencedMap (implementada por LinkedHashMap y TreeMap) da acceso directo a la primera y ultima entrada, y permite insertar explicitamente al principio o al final - antes de Java 21 no habia forma directa de hacerlo.",
          "conceptId": "sequenced-map",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["sequenced-basico"]
        },
        {
          "id": "gencol-seqmap-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que interfaz implementa LinkedHashMap desde Java 21 que le agrega firstEntry()/lastEntry()?",
          "answer": "SequencedMap",
          "distractors": ["SequencedCollection", "SequencedSet", "Comparable"],
          "explanation": "SequencedMap es la version para Maps de la familia Sequenced*; SequencedCollection y SequencedSet son para List/Deque y Set respectivamente.",
          "conceptId": "sequenced-map",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["sequenced-basico"]
        },
        {
          "id": "gencol-seqmap-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa para insertar 'Zoe' al principio del Map manteniendo el orden:",
          "code": "LinkedHashMap<String, Integer> edades = new LinkedHashMap<>();\nedades._____(\"Zoe\", 40);",
          "answer": "putFirst",
          "distractors": ["put", "addFirst", "insertFirst"],
          "explanation": "putFirst() (de SequencedMap) inserta la entrada al comienzo del orden de iteracion; put() normal solo agrega al final si la clave es nueva.",
          "conceptId": "sequenced-map",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["sequenced-basico"]
        },
        {
          "id": "gencol-sequenced-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una consultora IT grande te pregunta: que problema real resuelve SequencedCollection que existia antes de Java 21?",
          "answer": "Antes, cada tipo de coleccion tenia su propia forma (o ninguna forma directa) de acceder al primer/ultimo elemento; SequencedCollection unifica esa operacion con una sola API consistente",
          "distractors": ["Resuelve problemas de rendimiento en colecciones muy grandes", "Permite que las colecciones sean thread-safe automaticamente", "Reemplaza completamente a List y Map con una sola interfaz"],
          "explanation": "El problema no era de rendimiento ni de concurrencia, sino de consistencia de API: List tenia get(0)/get(size()-1), Deque tenia getFirst()/getLast(), y Set/Map no tenian nada directo - SequencedCollection/Set/Map dan una interfaz comun."
        }
      ]
    }
```

- [ ] **Step 3: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
change `private const val CURRENT_CONTENT_VERSION = "11"` to
`private const val CURRENT_CONTENT_VERSION = "12"`.

- [ ] **Step 4: Validate — JSON validity, no duplicate ids, no case-collisions, no orphaned/multi-terminal concepts, no dangling dependsOn**

Run:
```bash
python3 -c "
import json
from collections import defaultdict
d = json.load(open('app/src/main/assets/content/generics-collections.json'))
unit_ids = [u['unitId'] for u in d['units']]
ex_ids = [e['id'] for u in d['units'] for e in u['exercises']]
assert len(unit_ids) == len(set(unit_ids)), 'duplicate unit id'
assert len(ex_ids) == len(set(ex_ids)), 'duplicate exercise id'
print('OK', len(unit_ids), 'units,', len(ex_ids), 'exercises')

bad = []
for u in d['units']:
    for e in u['exercises']:
        if e.get('type') in ('mcq','fill_blank') and 'distractors' in e:
            ans = e['answer'].strip().lower()
            for dist in e['distractors']:
                if dist.strip().lower() == ans:
                    bad.append((e['id'], dist))
print('case-collisions:', bad)

roles_by_concept = defaultdict(set)
terminal_counts = defaultdict(int)
for u in d['units']:
    for e in u['exercises']:
        if e.get('conceptId'):
            roles_by_concept[e['conceptId']].add(e.get('role'))
            if e.get('role') in ('solo', 'practice'):
                terminal_counts[e['conceptId']] += 1
no_terminal = [c for c, roles in roles_by_concept.items() if not (roles & {'solo','practice'})]
multi_terminal = {c: n for c, n in terminal_counts.items() if n > 1}
print('concepts with zero terminal roles:', no_terminal)
print('concepts with more than one terminal role:', multi_terminal)

all_concepts = set(roles_by_concept.keys())
missing_deps = []
for u in d['units']:
    for e in u['exercises']:
        for dep in e.get('dependsOn', []):
            if dep not in all_concepts:
                missing_deps.append((e['id'], dep))
print('dangling dependsOn refs:', missing_deps)
"
```
Expected: `OK 6 units, 60 exercises` (6 = 5 existing + 1 new; 60 = 47
existing + 3 new retrofit exercises + 10 in the new unit),
`case-collisions: []`, `concepts with zero terminal roles: []`,
`concepts with more than one terminal role: {}`, `dangling dependsOn refs: []`.

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/content/generics-collections.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: add ConcurrentModificationException + Sequenced Collections to Genericos y Colecciones (sub-cycle 2)"
```

---

## Post-review corrections (applied after this plan was executed)

The final whole-branch review, verified by actually executing the code on
a real JDK 21, found the CME exercises didn't do what they claimed:

1. **(Critical) Neither CME code sample actually throws.** Both hit the
   classic "removing the second-to-last element" ArrayList trap: after
   `remove()`, the iterator's `cursor == size`, so `hasNext()` returns
   `false` and the loop exits *before* the next `next()` call would have
   detected the modification and thrown. This is exactly the trap the
   1Z0-830 exam tests, and the original content taught the wrong side of
   it. Fixed by using a 4th list element so the removed element is no
   longer second-to-last:
   - `gencol-cme-intro`: list becomes `["Ana", "Bob", "Carla", "Dora"]`
     (still removes `"Bob"`) — now genuinely throws (uncaught, since this
     is a worked_example, not asserted by a grader).
   - `gencol-cme-solo`: list becomes `List.of(1, 2, 3, 4)` (still removes
     `2`) — now genuinely throws, caught, prints `"capturada"` as the
     exercise's `answer` already claimed.
2. **(Important) Fresh-install ordering was inverted.** The CME ladder's
   `pathOrder` (0,1,2) sorted *before* the 6 pre-existing
   `gencol-listssets-*` exercises, which have no `pathOrder` at all
   (`null` sorts last, per `pathOrder ?: Int.MAX_VALUE`) — a fresh
   install's first-ever exercise in this unit would have been the
   hardest new concept, not a basic List/Set fundamental. Fixed by
   giving the 6 existing exercises explicit `pathOrder` 0-5 (in their
   existing order — this only adds the `pathOrder` field, nothing else
   about them changes, so `review_state`/grandfathering is unaffected)
   and shifting the CME ladder to `pathOrder` 6, 7, 8.

Two Minor findings from the same review were left unfixed (deferred):
`gencol-seqmap-intro`'s summary overstates that `SequencedMap` always
supports positional insertion (`TreeMap.putFirst()` actually throws
`UnsupportedOperationException`, since a sorted map can't honor a
caller-chosen position); `gencol-sequenced-guided`'s explanation slightly
overstates when `getLast()` first became available. Both are
explanation-text polish, not incorrect answers.

---

## After the task: manual on-device QA

Install a clean/in-place build and manually verify on-device:

1. Ruta shows Genericos y Colecciones with 6 units total, in order, and
   `gencol-lists-sets` ("Listas y Sets") still shows "Completada" (this
   retrofit is append-only, but confirm anyway).
2. Replay `gencol-lists-sets` from Ruta: confirm the answerable-exercise
   count is 8 (9 total exercises − 1 new `worked_example` intro).
   Confirm `guided`/`solo` exercises grade correctly.
3. Play the new `gencol-sequenced-collections` unit directly from Ruta:
   confirm `guided`/`solo` exercises grade correctly, confirm
   `predict_output` answers grade correctly (e.g.
   `gencol-reversed-solo`).
4. Confirm the section's mandatory checkpoint still triggers correctly
   after all 6 units are complete. **This closes Genericos y Colecciones**
   — after this, the next section in the retrofit ADR's order is Streams y
   Lambdas.
