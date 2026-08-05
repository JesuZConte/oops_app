# Genericos y Colecciones — Sub-ciclo 1 (Generics Avanzados + Arrays) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the first half of the Generics y Colecciones objective gap:
bounded type parameters, wildcards (`? extends`/`? super`) and the PECS
rule, type erasure/non-reifiable types, and Arrays (declaration,
multi-dimensional, the `Arrays` utility class) — all currently absent or
only lightly touched. First of 2 planned sub-cycles bringing this section
to full 1Z0-830 coverage, per
`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`. Also retrofits the
existing "Generics" unit with first-exposure ladders, per
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md` — this section's
first retrofit (unlike Fundamentos, none of its units have any ladder
metadata yet).

**Architecture:** Content-only — one JSON pack edit
(`app/src/main/assets/content/generics-collections.json`), one version
bump, no Kotlin/Compose changes. Two kinds of edit: (a) a **retrofit** of
the existing `gencol-generics` unit — 3 of its 6 existing exercises
(`gencol-generics-02`, `-03`, `-04`) already lightly cover bounded types
and the upper-bound wildcard; they keep their exact `id`/`type`/`prompt`/
`answer`/etc. unchanged and gain only `conceptId`/`role`/`pathOrder`,
becoming the `solo` step of a fresh `intro`→`guided` pair; 2 fully new
concepts (`? super`/PECS, type erasure) are added with complete ladders;
`gencol-generics-01`/`-05`/`-06` are left untouched; (b) **1 new unit
appended**: `gencol-arrays`.

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
- **One-terminal-role rule (hard rule, from a real bug found in Fundamentos
  sub-cycle 2):** every `conceptId` must have **exactly one** exercise with
  `role: "solo"` or `role: "practice"` — never zero (the concept would
  never be marked "born", permanently blocking anything `dependsOn` it) and
  never more than one (`GetTodaySessionUseCase` marks a concept "born" the
  moment ANY ONE terminal-role exercise is answered, permanently excluding
  every other unanswered terminal-role exercise of that same concept).
  Every concept below has exactly one terminal role — verify this
  explicitly in Step 4 before considering the task done.
- **Grandfathering rule (retrofit task):** when adding `conceptId`/`role`/
  `pathOrder` to an *existing* exercise (`gencol-generics-02`, `-03`,
  `-04`), never change its `id`, `type`, `prompt`, `code`, `answer`,
  `distractors`, or `explanation` — only add the 3 new fields.
  `gencol-generics-01`, `-05`, `-06` must be left completely untouched.
- **Grading-safety rule:** for `mcq`/`fill_blank`, no distractor may differ
  from the `answer` only by capitalization (case-insensitive grading).
- Every unit mixes exam/syntax, code-classification, and interview flavors.
  Interview prompts use generic company framing ("una consultora IT
  grande", "una empresa de servicios financieros") — never real brand
  names.
- The new unit includes a `summary: {text, code}` field. Do not add a
  `summary` to `gencol-generics` — it already has one, unchanged.
- `ContentSeeder`'s `CURRENT_CONTENT_VERSION`
  (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`)
  must bump from `"10"` to `"11"`.
- `generics-collections.json` is already registered in
  `ContentPackRegistry.kt` — no registry edit needed.
- New unit id prefix: `gencol-`. `certObjective`: `generics-collections`
  (matches the section's existing units). `orderIndex` 5, appended after
  `gencol-comparators-immutable` (4) — Arrays is a peer topic to the
  existing units, not a prerequisite for any of them, so appending avoids
  touching the other 3 units' `orderIndex` at all.
- No Room schema change, no migration.

---

### Task 1: Retrofit `gencol-generics` and append the `gencol-arrays` unit

**Files:**
- Modify: `app/src/main/assets/content/generics-collections.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: nothing new — same generic JSON pack parsing already proven
  across every content cycle, including ladder fields.
- Produces: nothing — this plan has only one task.

- [ ] **Step 1: Retrofit `gencol-generics`**

Find the unit with `"unitId": "gencol-generics"` in
`app/src/main/assets/content/generics-collections.json`. Its `exercises`
array currently has 6 objects in order: `gencol-generics-01` through
`gencol-generics-06`.

**1a. Add these 3 fields to `gencol-generics-02`** (do not change anything
else): `"conceptId": "generics-declaracion", "role": "solo", "pathOrder": 2`

**1b. Add these 3 fields to `gencol-generics-03`**: `"conceptId": "generics-bounded", "role": "solo", "pathOrder": 5`

**1c. Add these 3 fields to `gencol-generics-04`**: `"conceptId": "generics-wildcard-extends", "role": "solo", "pathOrder": 8`

**1d. Leave `gencol-generics-01`, `gencol-generics-05`, `gencol-generics-06`
completely unchanged.**

**1e. Insert these 13 new exercises into the array**, positioned exactly as
follows relative to the 6 existing ones (comments below mark where each
group goes — do not include the comments in the actual JSON):

```
gencol-declaracion-intro       <- new, before gencol-generics-01
gencol-declaracion-guided      <- new, before gencol-generics-01
gencol-generics-01             <- existing, untouched
gencol-generics-02             <- existing, retagged (1a)
gencol-bounded-intro           <- new, after gencol-generics-02
gencol-bounded-guided          <- new, after gencol-generics-02
gencol-generics-03             <- existing, retagged (1b)
gencol-wildcardext-intro       <- new, after gencol-generics-03
gencol-wildcardext-guided      <- new, after gencol-generics-03
gencol-generics-04             <- existing, retagged (1c)
gencol-wildcardsuper-intro     <- new, after gencol-generics-04
gencol-wildcardsuper-guided    <- new
gencol-wildcardsuper-solo      <- new
gencol-erasure-intro           <- new
gencol-erasure-guided          <- new
gencol-erasure-solo            <- new
gencol-generics-interview      <- new
gencol-generics-05             <- existing, untouched
gencol-generics-06             <- existing, untouched
```

The 13 new exercise objects, in the order listed above:

```json
{
  "id": "gencol-declaracion-intro",
  "type": "worked_example",
  "difficulty": 1,
  "prompt": "Los Generics permiten declarar un parametro de tipo que la clase usa internamente",
  "code": "class Caja<T> {\n    private T contenido;\n\n    void guardar(T valor) {\n        contenido = valor;\n    }\n\n    T obtener() {\n        return contenido;\n    }\n}\n\nCaja<String> caja = new Caja<>();",
  "answer": "ok",
  "explanation": "T es un parametro de tipo: un marcador de posicion que se reemplaza por un tipo real (String, Integer, etc.) cuando se usa la clase. Caja<String> hace que guardar()/obtener() trabajen especificamente con String, con chequeo de tipos en compilacion.",
  "conceptId": "generics-declaracion",
  "role": "intro",
  "pathOrder": 0
},
{
  "id": "gencol-declaracion-guided",
  "type": "mcq",
  "difficulty": 2,
  "prompt": "Que gana Caja<T> frente a una version de Caja que use Object internamente?",
  "answer": "Chequeo de tipos en tiempo de compilacion, sin necesitar castear al sacar el valor",
  "distractors": ["Mejor rendimiento en tiempo de ejecucion", "La posibilidad de guardar cualquier tipo, cosa que Object no permite", "Object y T son exactamente lo mismo, no hay ninguna diferencia real"],
  "explanation": "Con Object, obtener() devolveria Object y habria que castear manualmente (riesgo de ClassCastException en runtime); con T, el compilador ya sabe el tipo real y no hace falta castear.",
  "conceptId": "generics-declaracion",
  "role": "guided",
  "pathOrder": 1
},
{
  "id": "gencol-bounded-intro",
  "type": "worked_example",
  "difficulty": 2,
  "prompt": "T extends Number acota el tipo generico a Number y sus subtipos",
  "code": "public <T extends Number> double sumar(T a, T b) {\n    return a.doubleValue() + b.doubleValue();\n}\n\nsumar(3, 4);      // T = Integer, OK\nsumar(3.5, 4.5);  // T = Double, OK\n// sumar(\"a\", \"b\"); ERROR: String no extiende Number",
  "answer": "ok",
  "explanation": "Sin el bound, T podria ser cualquier tipo y no se podria llamar a doubleValue() (que solo existe en Number y sus subclases); T extends Number restringe los tipos aceptados y habilita usar los metodos de Number dentro del metodo.",
  "conceptId": "generics-bounded",
  "role": "intro",
  "pathOrder": 3,
  "dependsOn": ["generics-declaracion"]
},
{
  "id": "gencol-bounded-guided",
  "type": "mcq",
  "difficulty": 2,
  "prompt": "Por que hace falta declarar T extends Number para poder llamar a a.doubleValue() dentro del metodo?",
  "answer": "Porque sin el bound, T podria ser cualquier tipo, y el compilador no puede garantizar que tenga el metodo doubleValue()",
  "distractors": ["No hace falta, doubleValue() esta disponible en cualquier tipo generico", "Porque T extends Number hace que el metodo sea mas rapido", "Porque sin bound, T seria automaticamente String"],
  "explanation": "El compilador solo permite llamar a metodos que sabe que el tipo tiene garantizados; con T sin bound, el unico tipo garantizado es Object. T extends Number garantiza los metodos de Number, incluido doubleValue().",
  "conceptId": "generics-bounded",
  "role": "guided",
  "pathOrder": 4,
  "dependsOn": ["generics-declaracion"]
},
{
  "id": "gencol-wildcardext-intro",
  "type": "worked_example",
  "difficulty": 3,
  "prompt": "? extends Number acepta una lista de Number o cualquier subtipo, de solo lectura",
  "code": "void imprimirTotal(List<? extends Number> lista) {\n    double total = 0;\n    for (Number n : lista) {\n        total += n.doubleValue();\n    }\n    System.out.println(total);\n}\n\nimprimirTotal(List.of(1, 2, 3));       // List<Integer>, OK\nimprimirTotal(List.of(1.5, 2.5));      // List<Double>, OK",
  "answer": "ok",
  "explanation": "? extends Number acepta List<Integer>, List<Double>, etc. - cualquier subtipo de Number- pero el metodo solo puede LEER elementos como Number, nunca agregar (salvo null), porque el compilador no sabe el subtipo exacto.",
  "conceptId": "generics-wildcard-extends",
  "role": "intro",
  "pathOrder": 6,
  "dependsOn": ["generics-bounded"]
},
{
  "id": "gencol-wildcardext-guided",
  "type": "mcq",
  "difficulty": 3,
  "prompt": "Por que no se puede hacer lista.add(5) sobre un List<? extends Number>?",
  "answer": "Porque el compilador no sabe si la lista real es List<Integer>, List<Double>, etc., y agregar el tipo incorrecto romperia esa lista concreta",
  "distractors": ["Porque List<? extends Number> siempre es inmutable", "Si se puede, add() funciona igual que en cualquier List", "Porque Number no tiene un metodo add()"],
  "explanation": "Si el compilador permitiera lista.add(5) sobre un List<? extends Number> que en realidad es un List<Double>, estarias agregando un Integer a una lista de Double - eso rompe la seguridad de tipos que generics garantiza.",
  "conceptId": "generics-wildcard-extends",
  "role": "guided",
  "pathOrder": 7,
  "dependsOn": ["generics-bounded"]
},
{
  "id": "gencol-wildcardsuper-intro",
  "type": "worked_example",
  "difficulty": 3,
  "prompt": "? super Integer acepta una lista de Integer o cualquier supertipo, para escribir",
  "code": "void agregarNumeros(List<? super Integer> lista) {\n    lista.add(1);\n    lista.add(2);\n}\n\nagregarNumeros(new ArrayList<Integer>()); // OK\nagregarNumeros(new ArrayList<Number>());  // OK\nagregarNumeros(new ArrayList<Object>());  // OK",
  "answer": "ok",
  "explanation": "? super Integer acepta List<Integer>, List<Number>, List<Object> - cualquier supertipo de Integer- y permite agregar Integer con seguridad, porque cualquiera de esos tipos puede contener un Integer. Lo que no se garantiza es que leer devuelva especificamente un Integer (solo Object).",
  "conceptId": "generics-wildcard-super-pecs",
  "role": "intro",
  "pathOrder": 9,
  "dependsOn": ["generics-wildcard-extends"]
},
{
  "id": "gencol-wildcardsuper-guided",
  "type": "mcq",
  "difficulty": 3,
  "prompt": "Que dice la regla PECS (Producer Extends, Consumer Super) sobre cuando usar cada wildcard?",
  "answer": "Usa extends cuando la coleccion produce datos (solo la lees), y super cuando la coleccion consume datos (solo agregas a ella)",
  "distractors": ["Usa extends para escribir y super para leer, al reves de lo que suena", "PECS dice que nunca hay que usar wildcards, siempre el tipo exacto", "extends y super son intercambiables, PECS es solo una convencion de estilo sin efecto real"],
  "explanation": "PECS es una regla mnemotecnica: si tu codigo actua como Productor (extrae/lee elementos), usa extends; si actua como Consumidor (agrega/escribe elementos), usa super. Mezclarlos al reves haria que el compilador rechace operaciones validas.",
  "conceptId": "generics-wildcard-super-pecs",
  "role": "guided",
  "pathOrder": 10,
  "dependsOn": ["generics-wildcard-extends"]
},
{
  "id": "gencol-wildcardsuper-solo",
  "type": "predict_output",
  "difficulty": 3,
  "prompt": "Que imprime este codigo?",
  "code": "List<Object> objetos = new ArrayList<>();\nList<? super Integer> lista = objetos;\nlista.add(42);\nSystem.out.println(objetos.get(0));",
  "answer": "42",
  "explanation": "objetos y lista apuntan a la misma lista en memoria; lista.add(42) agrega el Integer 42 (valido porque List<Object> es un supertipo aceptado por ? super Integer), y objetos.get(0) lo recupera como Object.",
  "conceptId": "generics-wildcard-super-pecs",
  "role": "solo",
  "pathOrder": 11,
  "dependsOn": ["generics-wildcard-extends"]
},
{
  "id": "gencol-erasure-intro",
  "type": "worked_example",
  "difficulty": 3,
  "prompt": "Type erasure: en tiempo de ejecucion, la informacion del tipo generico desaparece",
  "code": "List<String> strings = new ArrayList<>();\nList<Integer> ints = new ArrayList<>();\n\nSystem.out.println(strings.getClass() == ints.getClass()); // true",
  "answer": "ok",
  "explanation": "El compilador usa los tipos generics solo para chequear en compilacion; al compilar a bytecode, borra esa informacion (erasure) y ambas listas terminan siendo la misma clase ArrayList en tiempo de ejecucion - por eso List<String> y List<Integer> comparten la misma Class.",
  "conceptId": "generics-type-erasure",
  "role": "intro",
  "pathOrder": 12,
  "dependsOn": ["generics-declaracion"]
},
{
  "id": "gencol-erasure-guided",
  "type": "mcq",
  "difficulty": 3,
  "prompt": "Por que no se puede escribir new T[10] dentro de una clase generica Caja<T>?",
  "answer": "Porque en tiempo de ejecucion T ya no existe (fue borrado por type erasure), y la JVM necesita saber el tipo real del array para crearlo",
  "distractors": ["Porque los arrays no pueden guardar tipos genericos bajo ninguna circunstancia", "Es solo una limitacion arbitraria del compilador sin motivo tecnico real", "Porque T siempre se convierte en int, que no es un tipo de objeto"],
  "explanation": "Un array en Java conoce su tipo de componente en tiempo de ejecucion (para chequeos como ArrayStoreException); como T se borra por erasure, la JVM no tendria forma de saber que tipo de array crear - por eso T se considera no reificable.",
  "conceptId": "generics-type-erasure",
  "role": "guided",
  "pathOrder": 13,
  "dependsOn": ["generics-declaracion"]
},
{
  "id": "gencol-erasure-solo",
  "type": "mcq",
  "difficulty": 3,
  "prompt": "Que significa que un tipo generico como T sea no reificable (non-reifiable)?",
  "answer": "Que su informacion de tipo no esta completamente disponible en tiempo de ejecucion, a diferencia de un tipo concreto como String",
  "distractors": ["Que no se puede usar nunca dentro de una clase generica", "Que el tipo cambia dinamicamente segun el valor asignado", "Que solo puede usarse con tipos primitivos"],
  "explanation": "Reificable significa que el tipo existe completamente en runtime; por type erasure, T (y en general los tipos parametrizados como List<String>) pierden esa informacion detallada en tiempo de ejecucion - de ahi vienen restricciones como no poder hacer instanceof List<String> o new T[].",
  "conceptId": "generics-type-erasure",
  "role": "solo",
  "pathOrder": 14,
  "dependsOn": ["generics-declaracion"]
},
{
  "id": "gencol-generics-interview",
  "type": "mcq",
  "difficulty": 3,
  "prompt": "Una consultora IT grande te pregunta: por que Java implementa generics con type erasure en vez de mantener la informacion de tipo en tiempo de ejecucion (como hacen otros lenguajes)?",
  "answer": "Para mantener compatibilidad con codigo Java anterior a los generics (Java 5), que compilaba sin parametros de tipo y debia seguir funcionando con las mismas clases (como ArrayList)",
  "distractors": ["Porque la JVM no puede representar tipos genericos bajo ninguna circunstancia tecnica", "Fue un error de diseno que Java nunca pudo corregir en versiones posteriores", "Porque type erasure hace que el codigo compile mas rapido, sin otra razon"],
  "explanation": "Generics se agrego en Java 5 sobre un lenguaje y una JVM que ya existian; erasure permitio que ArrayList (generica) fuera binariamente compatible con el bytecode ya compilado de versiones anteriores que usaban ArrayList sin tipos parametrizados."
}
```

- [ ] **Step 2: Append the `gencol-arrays` unit**

Add this unit as the new last element of `generics-collections.json`'s
`units` array (after `gencol-comparators-immutable`, remembering the
comma):

```json
    {
      "unitId": "gencol-arrays",
      "name": "Arrays",
      "certObjective": "generics-collections",
      "orderIndex": 5,
      "summary": {
        "text": "Un array es una estructura de tamano fijo que guarda elementos del mismo tipo, indexados desde 0. Se puede declarar multi-dimensional (array de arrays), donde cada fila puede incluso tener un largo distinto (arrays irregulares/jagged). La clase utilitaria Arrays ofrece metodos estaticos como sort(), binarySearch(), equals(), fill() y asList() para trabajar con arrays sin escribir loops manuales.",
        "code": "int[] numeros = {5, 3, 1, 4, 2};\nArrays.sort(numeros); // [1, 2, 3, 4, 5]\n\nint[][] matriz = new int[2][3]; // 2 filas, 3 columnas\nmatriz[0][1] = 10;"
      },
      "exercises": [
        {
          "id": "gencol-arrdecl-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Un array tiene tamano fijo, definido al crearlo, y se indexa desde 0",
          "code": "int[] numeros = new int[5];  // 5 elementos, todos en 0\nnumeros[0] = 10;\nnumeros[4] = 50;\n\nint[] literal = {1, 2, 3};   // tamano 3, inicializado directo",
          "answer": "ok",
          "explanation": "El tamano se fija al crear el array (new int[5]) y no puede cambiar despues - a diferencia de una List, que crece dinamicamente. El primer elemento es indice 0, el ultimo es tamano-1.",
          "conceptId": "arrays-declaracion",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "gencol-arrdecl-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que pasa si accedes a numeros[5] en un array int[] numeros = new int[5]?",
          "answer": "Se lanza ArrayIndexOutOfBoundsException en tiempo de ejecucion",
          "distractors": ["Devuelve 0 automaticamente, sin error", "El array crece automaticamente para incluir el indice 5", "Es un error de compilacion, no de ejecucion"],
          "explanation": "Los indices validos van de 0 a tamano-1 (0 a 4 en un array de 5); acceder fuera de ese rango compila bien pero lanza una excepcion en runtime, porque el tamano solo se conoce con certeza en ejecucion.",
          "conceptId": "arrays-declaracion",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "gencol-arrdecl-solo",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Declara un array de 3 Strings vacio (sin inicializar valores):",
          "code": "String[] nombres = new String[_____];",
          "answer": "3",
          "distractors": ["3.0", "\"3\"", "three"],
          "explanation": "new String[3] crea un array de 3 posiciones, todas inicializadas en null (el valor por defecto para tipos de objeto).",
          "conceptId": "arrays-declaracion",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "gencol-arrmulti-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un array multidimensional es un array de arrays",
          "code": "int[][] matriz = new int[2][3]; // 2 filas, 3 columnas\nmatriz[0][1] = 10;\nmatriz[1][2] = 20;\n\nint[][] irregular = new int[2][];\nirregular[0] = new int[3];\nirregular[1] = new int[5]; // filas de distinto largo (jagged array)",
          "answer": "ok",
          "explanation": "int[][] matriz declara un array cuyos elementos son, a su vez, arrays de int. Java no exige que todas las filas tengan el mismo largo - eso se llama un array irregular o jagged array.",
          "conceptId": "arrays-multidimensional",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["arrays-declaracion"]
        },
        {
          "id": "gencol-arrmulti-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que es un jagged array (array irregular) en Java?",
          "answer": "Un array multidimensional donde las filas (sub-arrays) tienen distinto largo entre si",
          "distractors": ["Un array que lanza una excepcion al crearse", "Un array que solo puede contener numeros negativos", "Un sinonimo de array multidimensional, sin diferencia real"],
          "explanation": "A diferencia de una matriz matematica estricta, en Java cada fila de un array 2D es su propio array independiente, asi que pueden tener largos distintos - eso es lo que hace irregular al array.",
          "conceptId": "arrays-multidimensional",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["arrays-declaracion"]
        },
        {
          "id": "gencol-arrmulti-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "int[][] matriz = {\n    {1, 2, 3},\n    {4, 5}\n};\nSystem.out.println(matriz[1].length);",
          "answer": "2",
          "explanation": "matriz[1] es la segunda fila, {4, 5}, que tiene 2 elementos; matriz[1].length accede al largo de esa fila especifica, no de toda la matriz.",
          "conceptId": "arrays-multidimensional",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["arrays-declaracion"]
        },
        {
          "id": "gencol-arrutil-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "La clase Arrays ofrece metodos estaticos utiles para trabajar con arrays",
          "code": "int[] numeros = {5, 3, 1, 4, 2};\nArrays.sort(numeros);                    // [1, 2, 3, 4, 5]\nint pos = Arrays.binarySearch(numeros, 3); // busca 3 en un array ordenado\nboolean iguales = Arrays.equals(numeros, new int[]{1,2,3,4,5});",
          "answer": "ok",
          "explanation": "Arrays.sort() ordena in-place; Arrays.binarySearch() requiere que el array ya este ordenado, o el resultado es indefinido; Arrays.equals() compara contenido elemento por elemento (a diferencia de ==, que compararia referencias).",
          "conceptId": "arrays-clase-utilitaria",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["arrays-declaracion"]
        },
        {
          "id": "gencol-arrutil-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que precondicion necesita Arrays.binarySearch() para funcionar correctamente?",
          "answer": "El array debe estar ordenado de antemano (por ejemplo, con Arrays.sort())",
          "distractors": ["Ninguna, funciona igual de bien con cualquier array", "El array debe tener un numero par de elementos", "El array debe contener solo valores positivos"],
          "explanation": "La busqueda binaria depende de que los elementos esten ordenados para descartar mitades del array en cada paso; sobre un array desordenado, el resultado no esta garantizado y puede ser incorrecto.",
          "conceptId": "arrays-clase-utilitaria",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["arrays-declaracion"]
        },
        {
          "id": "gencol-arrutil-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "int[] a = {1, 2, 3};\nint[] b = {1, 2, 3};\nSystem.out.println(a == b);\nSystem.out.println(Arrays.equals(a, b));",
          "answer": "false\ntrue",
          "explanation": "a y b son arrays distintos en memoria, asi que == (comparacion de referencia) da false; Arrays.equals() compara el contenido elemento por elemento, y ambos arrays tienen los mismos valores, asi que da true.",
          "conceptId": "arrays-clase-utilitaria",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["arrays-declaracion"]
        },
        {
          "id": "gencol-arrays-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una empresa de servicios financieros te pregunta: por que elegirias un array en vez de un ArrayList para guardar un conjunto de datos de tamano fijo y conocido de antemano?",
          "answer": "Un array primitivo evita el overhead de autoboxing y tiene un poco menos de overhead de memoria que un ArrayList, cuando el tamano ya es fijo y conocido",
          "distractors": ["Porque un ArrayList no puede guardar numeros bajo ninguna circunstancia", "No hay ninguna diferencia real de rendimiento entre ambos", "Porque los arrays permiten agregar elementos mas facil que un ArrayList"],
          "explanation": "Cuando el tamano es fijo y se conocen los tipos primitivos que se van a guardar, un array evita el autoboxing (int[] guarda ints directos, no Integer) y el overhead estructural de una lista dinamica - un ArrayList<E> brilla cuando el tamano es variable o se necesitan los metodos de Collections."
        }
      ]
    }
```

- [ ] **Step 3: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
change `private const val CURRENT_CONTENT_VERSION = "10"` to
`private const val CURRENT_CONTENT_VERSION = "11"`.

- [ ] **Step 4: Validate — JSON validity, no duplicate ids, no case-collisions, no orphaned/multi-terminal concepts**

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
Expected: `OK 5 units, 47 exercises` (5 = 4 existing + 1 new; 47 = 24
existing + 13 new retrofit exercises + 10 in the new unit),
`case-collisions: []`, `concepts with zero terminal roles: []`,
`concepts with more than one terminal role: {}`, `dangling dependsOn refs: []`.

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/content/generics-collections.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: retrofit Generics + add Arrays unit to Genericos y Colecciones (sub-cycle 1)"
```

---

## After the task: manual on-device QA

Install a clean/in-place build and manually verify on-device:

1. Ruta shows Genericos y Colecciones with 5 units total, in order, and
   `gencol-generics` ("Generics") still shows "Completada" (retrofit must
   not reset Luis's existing progress on it).
2. Replay `gencol-generics` from Ruta: confirm the answerable-exercise
   count is 14 (19 total exercises − 5 new `worked_example` intros, one
   per concept: declaracion, bounded, wildcard-extends,
   wildcard-super-pecs, erasure). Confirm `guided`/`solo` exercises grade
   correctly.
3. Play the new `gencol-arrays` unit directly from Ruta: confirm
   `guided`/`solo` exercises grade correctly, confirm `predict_output`
   multi-line answers grade correctly (e.g. `gencol-arrutil-solo`).
4. Confirm the section's mandatory checkpoint still triggers correctly
   after all 5 units are complete.
