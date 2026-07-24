# Fase 2.3 — Generics y Colecciones (Content Scaling) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Relocate the Streams section to its correct final position, and add
the next section in the content roadmap — Genéricos y Colecciones — as real,
playable content, proving the "add a section = one JSON pack + register it"
architecture claim with the first section built after the model was frozen.

**Architecture:** No engine or UI changes — confirmed by code inspection
(see design spec) that `ContentSeeder`, `GetLearningPathUseCase`, and both
checkpoint use cases are fully generic over section count. This plan is
content-only: two JSON edits, one new JSON asset, and a content-version bump.

**Tech Stack:** kotlinx.serialization JSON content packs (no Kotlin/Compose
code changes in this plan).

**Design doc:** `docs/superpowers/specs/2026-07-24-fase2-3-content-scaling-design.md`

## Global Constraints

- Content prompts/explanations follow the existing style already in
  `java-fundamentals.json`/`streams.json`: no accent marks, no inverted `¿`
  (e.g. `"Que imprime este codigo?"`, not `"¿Qué imprime este código?"`).
- Every unit mixes **three flavors** of question (design spec's explicit
  guidance, not just exam/syntax): exam/syntax (`fill_blank`, `parsons`,
  `predict_output`), code classification (`mcq` over a snippet), and
  interview/judgment (`mcq` framed as "why/when/what problem does this
  solve" — not sintaxis recall). Each of the 4 units below has at least one
  exercise of each flavor.
- `ContentSeeder.CURRENT_CONTENT_VERSION` **must** be bumped (from `"3"` to
  `"4"`) for the new section to actually seed on devices that already have
  the app installed — the seeder is a no-op if the stored version already
  matches (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`).
  Reseeding wipes and reloads only `sections`/`units`/`exercises` — it does
  not touch `review_state`/`unit_progress`/`checkpoint_attempts`, so existing
  progress is preserved as long as no existing exercise/unit/section id is
  renamed or removed. This plan only adds new ids and changes one
  `orderIndex` value on an existing section — no id is renamed or removed.
- No Room schema change and no migration — content packs are plain JSON
  assets loaded at runtime; `Section`/`Unit`/`Exercise` tables have no
  fixed-cardinality constraint (verified in the design spec).
- Section id for the new pack: `java-generics-collections` (matches the
  existing `java-fundamentals`/`java-streams` naming convention). Unit id
  prefix: `gencol-`. `certObjective` for all its units:
  `generics-collections` (matches the domain name used in
  `docs/specs/PROJECT-OOPS.md` section 8).
- `examVersion` for the new pack: `"core"` (matches `java-fundamentals.json`
  — Generics/Collections is core exam material, not "extra moderno").

---

### Task 1: Relocate Streams to its final position (orderIndex 2 → 3)

**Files:**
- Modify: `app/src/main/assets/content/streams.json:4`

**Interfaces:**
- Consumes: nothing new.
- Produces: nothing consumed by later tasks in this plan — Task 2's new
  section takes `orderIndex: 2`, independently of this change.

- [ ] **Step 1: Change the section-level `orderIndex`**

In `app/src/main/assets/content/streams.json`, change line 4:

```json
  "orderIndex": 2,
```

to:

```json
  "orderIndex": 3,
```

(Only the section-level `orderIndex` at the top of the file — the
per-unit `orderIndex` values inside `units` are unrelated and unchanged.)

- [ ] **Step 2: Validate the file is still valid JSON**

Run: `python3 -m json.tool app/src/main/assets/content/streams.json > /dev/null && echo VALID`
Expected: `VALID` printed, no errors.

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — this file's `orderIndex` value is not
referenced by any test (all `ContentPackParsingTest`/`ContentMapperTest`
cases use their own synthetic inline JSON, not the real asset file), so no
test should be affected. This run is the regression check.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/content/streams.json
git commit -m "content: relocate Streams section behind Generics y Colecciones (orderIndex 2->3)"
```

---

### Task 2: Add Genéricos y Colecciones section content

**Files:**
- Create: `app/src/main/assets/content/generics-collections.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12,22-25`

**Interfaces:**
- Consumes: the existing content-pack JSON schema (`sectionId`, `name`,
  `orderIndex`, `examVersion`, `units[].{unitId,name,certObjective,orderIndex,exercises[]}`,
  `exercises[].{id,type,difficulty,prompt,code,answer,distractors,lines,explanation}`)
  — unchanged from Fase 2.1/2.2, no new fields needed.
- Produces: nothing consumed by later tasks — this is the last task in the plan.

- [ ] **Step 1: Create the new content pack**

Create `app/src/main/assets/content/generics-collections.json`:

```json
{
  "sectionId": "java-generics-collections",
  "name": "Genericos y Colecciones",
  "orderIndex": 2,
  "examVersion": "core",
  "units": [
    {
      "unitId": "gencol-generics",
      "name": "Generics",
      "certObjective": "generics-collections",
      "orderIndex": 1,
      "exercises": [
        {
          "id": "gencol-generics-01",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Para que sirven los Generics en Java?",
          "answer": "Para detectar errores de tipo en tiempo de compilacion",
          "distractors": ["Para hacer el codigo mas rapido en tiempo de ejecucion", "Para evitar escribir tipos explicitos", "Para permitir herencia multiple"],
          "explanation": "Los generics agregan chequeo de tipos en compilacion, evitando ClassCastException en tiempo de ejecucion."
        },
        {
          "id": "gencol-generics-02",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Declara el parametro de tipo T en esta clase generica:",
          "code": "public class Caja<_____> {\n    private T valor;\n}",
          "answer": "T",
          "distractors": ["Object", "E", "V"],
          "explanation": "El parametro de tipo se declara entre angulos junto al nombre de la clase; T es la convencion para un tipo generico simple."
        },
        {
          "id": "gencol-generics-03",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Declara un metodo generico que solo acepta subtipos de Number:",
          "code": "public <T extends _____> void imprimir(T valor) {\n    System.out.println(valor);\n}",
          "answer": "Number",
          "distractors": ["Object", "Comparable", "Integer"],
          "explanation": "T extends Number acota el tipo generico a Number y sus subtipos (Integer, Double, etc.)."
        },
        {
          "id": "gencol-generics-04",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que significa este wildcard?",
          "code": "List<? extends Number> lista",
          "answer": "Una lista de un subtipo desconocido de Number, de solo lectura para agregar elementos",
          "distractors": ["Una lista que solo acepta Integer", "Una lista mutable de cualquier tipo", "Una lista de Number exacto, sin subtipos"],
          "explanation": "? extends Number es un wildcard acotado superiormente: acepta List<Integer>, List<Double>, etc., pero no permite agregar elementos (salvo null) porque el tipo exacto es desconocido."
        },
        {
          "id": "gencol-generics-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que usarias Generics en vez de simplemente tipar todo como Object?",
          "answer": "Porque Object requeriria casteos manuales y generics detecta errores de tipo en compilacion",
          "distractors": ["Porque Object es mas lento en tiempo de ejecucion", "Porque Object no permite null", "Porque Object no puede usarse en colecciones"],
          "explanation": "Sin generics, sacar un valor de una coleccion de Object requiere castear manualmente, y un error de tipo solo se descubre en tiempo de ejecucion (ClassCastException)."
        },
        {
          "id": "gencol-generics-06",
          "type": "predict_output",
          "difficulty": 1,
          "prompt": "Que imprime este codigo?",
          "code": "List<Integer> numeros = new ArrayList<>();\nnumeros.add(1);\nnumeros.add(2);\nint suma = numeros.get(0) + numeros.get(1);\nSystem.out.println(suma);",
          "answer": "3",
          "explanation": "get(0) devuelve 1 y get(1) devuelve 2; la suma 1 + 2 es 3."
        }
      ]
    },
    {
      "unitId": "gencol-lists-sets",
      "name": "Listas y Sets",
      "certObjective": "generics-collections",
      "orderIndex": 2,
      "exercises": [
        {
          "id": "gencol-listssets-01",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que tipo de coleccion es esta declaracion?",
          "code": "Set<String> nombres = new TreeSet<>();",
          "answer": "Un Set ordenado, sin elementos duplicados",
          "distractors": ["Una lista ordenada, permite duplicados", "Un mapa clave-valor", "Una cola FIFO sin orden"],
          "explanation": "TreeSet implementa Set (sin duplicados) y ademas mantiene los elementos en orden natural."
        },
        {
          "id": "gencol-listssets-02",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Crea un Set que no permite duplicados y no garantiza orden:",
          "code": "Set<String> nombres = new _____<>();",
          "answer": "HashSet",
          "distractors": ["ArrayList", "TreeMap", "LinkedList"],
          "explanation": "HashSet es la implementacion base de Set: sin duplicados, sin orden garantizado."
        },
        {
          "id": "gencol-listssets-03",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo? (el orden de iteracion importa)",
          "code": "Set<String> set = new TreeSet<>();\nset.add(\"banana\");\nset.add(\"manzana\");\nset.add(\"cereza\");\nfor (String s : set) System.out.println(s);",
          "answer": "banana\ncereza\nmanzana",
          "explanation": "TreeSet mantiene los elementos en orden natural (alfabetico para String), sin importar el orden de insercion."
        },
        {
          "id": "gencol-listssets-04",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Set<String> set = new LinkedHashSet<>();\nset.add(\"banana\");\nset.add(\"manzana\");\nset.add(\"cereza\");\nfor (String s : set) System.out.println(s);",
          "answer": "banana\nmanzana\ncereza",
          "explanation": "LinkedHashSet preserva el orden de insercion, a diferencia de HashSet (sin orden) o TreeSet (orden natural)."
        },
        {
          "id": "gencol-listssets-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cuando preferirias LinkedList sobre ArrayList en un caso real?",
          "answer": "Cuando necesitas inserciones/eliminaciones frecuentes al inicio o en medio de la lista",
          "distractors": ["Cuando necesitas acceso aleatorio rapido por indice", "Siempre, porque LinkedList es mas eficiente en memoria", "Nunca, ArrayList siempre es mejor"],
          "explanation": "ArrayList desplaza elementos al insertar/eliminar en medio (O(n)); LinkedList solo reenlaza nodos vecinos (O(1)) una vez ubicada la posicion."
        },
        {
          "id": "gencol-listssets-06",
          "type": "parsons",
          "difficulty": 1,
          "prompt": "Ordena las lineas para crear una lista, agregar dos elementos y recorrerla:",
          "lines": ["List<String> lista = new ArrayList<>();", "lista.add(\"a\");", "lista.add(\"b\");", "for (String s : lista) System.out.println(s);"],
          "answer": "List<String> lista = new ArrayList<>();\nlista.add(\"a\");\nlista.add(\"b\");\nfor (String s : lista) System.out.println(s);",
          "code": "List<String> lista = new ArrayList<>();\nlista.add(\"a\");\nlista.add(\"b\");\nfor (String s : lista) System.out.println(s);",
          "explanation": "Se declara e instancia la lista, se agregan elementos en orden, y luego se recorre con for-each."
        }
      ]
    },
    {
      "unitId": "gencol-maps-deques",
      "name": "Maps y Deques",
      "certObjective": "generics-collections",
      "orderIndex": 3,
      "exercises": [
        {
          "id": "gencol-mapsdeques-01",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que estructura de datos es un Map?",
          "answer": "Una coleccion de pares clave-valor, sin claves duplicadas",
          "distractors": ["Una lista ordenada de elementos", "Un conjunto sin orden ni duplicados", "Una pila LIFO"],
          "explanation": "Map asocia cada clave unica a un valor; put() con una clave existente reemplaza el valor."
        },
        {
          "id": "gencol-mapsdeques-02",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Crea un Map que mantiene las claves ordenadas:",
          "code": "Map<String, Integer> edades = new _____<>();",
          "answer": "TreeMap",
          "distractors": ["HashMap", "ArrayList", "LinkedList"],
          "explanation": "TreeMap ordena las claves segun su orden natural (o un Comparator dado)."
        },
        {
          "id": "gencol-mapsdeques-03",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Map<String, Integer> mapa = new HashMap<>();\nmapa.put(\"a\", 1);\nmapa.put(\"a\", 2);\nSystem.out.println(mapa.get(\"a\"));",
          "answer": "2",
          "explanation": "put() con una clave existente reemplaza el valor anterior; la segunda llamada sobrescribe 1 con 2."
        },
        {
          "id": "gencol-mapsdeques-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que hace este codigo?",
          "code": "Deque<Integer> pila = new ArrayDeque<>();\npila.push(1);\npila.push(2);\nSystem.out.println(pila.pop());",
          "answer": "Usa el Deque como pila (LIFO): imprime 2",
          "distractors": ["Usa el Deque como cola (FIFO): imprime 1", "Lanza una excepcion porque ArrayDeque no soporta push/pop", "Imprime el tamaño de la pila"],
          "explanation": "push() inserta al frente del Deque; pop() remueve y devuelve el frente. Tras push(1) y push(2), el frente es 2."
        },
        {
          "id": "gencol-mapsdeques-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que problema practico resuelve un Deque que una List no resuelve tan bien?",
          "answer": "Insertar y remover eficientemente en ambos extremos, sirviendo como pila o cola",
          "distractors": ["Ordenar elementos automaticamente", "Evitar duplicados", "Acceso aleatorio por indice"],
          "explanation": "Deque (double-ended queue) esta diseñado para operaciones O(1) en ambos extremos, algo que una List generica no garantiza."
        },
        {
          "id": "gencol-mapsdeques-06",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Agrega un elemento al frente de un Deque:",
          "code": "deque._____(\"primero\");",
          "answer": "addFirst",
          "distractors": ["addLast", "add", "offerLast"],
          "explanation": "addFirst() inserta explicitamente al frente; add() y offerLast() insertan al final."
        }
      ]
    },
    {
      "unitId": "gencol-comparators-immutable",
      "name": "Comparadores y colecciones inmutables",
      "certObjective": "generics-collections",
      "orderIndex": 4,
      "exercises": [
        {
          "id": "gencol-comparators-01",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que hace este codigo?",
          "code": "Comparator<Persona> porEdad = Comparator.comparing(Persona::getEdad);",
          "answer": "Crea un Comparator que ordena personas por su edad",
          "distractors": ["Ordena personas por nombre", "Modifica el campo edad de cada persona", "Lanza una excepcion porque getEdad no existe"],
          "explanation": "Comparator.comparing() construye un Comparator a partir de una funcion que extrae la clave de comparacion (aqui, la edad), sin necesidad de implementar Comparable."
        },
        {
          "id": "gencol-comparators-02",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Implementa el metodo de Comparable para ordenar por edad:",
          "code": "public int _____(Persona otra) {\n    return this.edad - otra.edad;\n}",
          "answer": "compareTo",
          "distractors": ["compare", "equals", "hashCode"],
          "explanation": "compareTo(T o) es el metodo que declara la interfaz Comparable<T>."
        },
        {
          "id": "gencol-comparators-03",
          "type": "parsons",
          "difficulty": 2,
          "prompt": "Ordena las lineas para ordenar una lista de nombres usando un Comparator:",
          "lines": ["List<String> nombres = new ArrayList<>(List.of(\"Carlos\", \"Ana\", \"Beto\"));", "nombres.sort(Comparator.naturalOrder());", "System.out.println(nombres);"],
          "answer": "List<String> nombres = new ArrayList<>(List.of(\"Carlos\", \"Ana\", \"Beto\"));\nnombres.sort(Comparator.naturalOrder());\nSystem.out.println(nombres);",
          "code": "List<String> nombres = new ArrayList<>(List.of(\"Carlos\", \"Ana\", \"Beto\"));\nnombres.sort(Comparator.naturalOrder());\nSystem.out.println(nombres);",
          "explanation": "Se crea una lista mutable a partir de una inmutable, se ordena con el comparador de orden natural, y se imprime el resultado."
        },
        {
          "id": "gencol-comparators-04",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "List<String> nombres = new ArrayList<>(List.of(\"Carlos\", \"Ana\", \"Beto\"));\nnombres.sort(Comparator.naturalOrder());\nSystem.out.println(nombres);",
          "answer": "[Ana, Beto, Carlos]",
          "explanation": "Comparator.naturalOrder() ordena Strings alfabeticamente: Ana, Beto, Carlos."
        },
        {
          "id": "gencol-comparators-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que pasa si intentas modificar una lista creada con List.of(...)?",
          "answer": "Lanza UnsupportedOperationException, porque es inmutable",
          "distractors": ["Se modifica normalmente, como un ArrayList", "Lanza NullPointerException", "No compila"],
          "explanation": "List.of() (Java 9+) crea una lista inmutable; cualquier intento de agregar/quitar/modificar lanza UnsupportedOperationException."
        },
        {
          "id": "gencol-comparators-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Te piden elegir entre HashMap y TreeMap para cachear resultados por clave -- cual eliges y por que?",
          "answer": "HashMap, porque el orden no importa para un cache y ofrece acceso O(1) promedio; TreeMap solo si necesitas las claves ordenadas",
          "distractors": ["TreeMap siempre, porque mantiene orden", "Da lo mismo, ambos tienen el mismo rendimiento", "Ninguno, para un cache se deberia usar una List"],
          "explanation": "Un cache tipico solo necesita lookup rapido por clave; TreeMap paga O(log n) por mantener orden que un cache no necesita."
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
private const val CURRENT_CONTENT_VERSION = "3"
```

to:

```kotlin
private const val CURRENT_CONTENT_VERSION = "4"
```

And change the `packAssetPaths` list (lines 22-25):

```kotlin
    private val packAssetPaths = listOf(
        "content/java-fundamentals.json",
        "content/streams.json"
    )
```

to:

```kotlin
    private val packAssetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json"
    )
```

(The list order here does not affect Ruta's displayed order — that comes
from each pack's own `orderIndex` field — but listing them in roadmap order
keeps the file easy to scan.)

- [ ] **Step 3: Validate the new file is valid JSON**

Run: `python3 -m json.tool app/src/main/assets/content/generics-collections.json > /dev/null && echo VALID`
Expected: `VALID` printed, no errors.

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — this is the regression check that the new
content and the version bump don't break anything (no dedicated content
test exists beyond the generic `ContentPackParsingTest`, which doesn't load
real asset files).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/content/generics-collections.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: add Generics y Colecciones section (4 units, 24 exercises)"
```

---

## After both tasks: manual on-device QA

No automated UI/content test covers full playability. Once both tasks are
merged, install a clean build and manually verify on-device (SM-A505G, same
pattern as prior phases):

1. Ruta shows sections in order: Fundamentos de Java → Genericos y
   Colecciones → Streams y lambdas.
2. Play through Genericos y Colecciones: confirm all 4 units are playable,
   confirm each of the 3 question flavors (exam/syntax, code-classification,
   interview) appears somewhere across the section, confirm Parsons and
   predict_output exercises in this new content work as expected.
3. Confirm the section's end-of-section voluntary checkpoint appears after
   finishing all its units.
4. Confirm the placement/skip checkpoint (from Fase 2.1b) correctly offers
   to skip over Generics y Colecciones + prior sections if a later section
   (Streams) is tapped directly.
