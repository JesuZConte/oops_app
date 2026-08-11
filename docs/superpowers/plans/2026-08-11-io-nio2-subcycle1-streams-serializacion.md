# I/O y NIO.2 - Sub-cycle 1 (Streams clasicos + Serializacion) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the brand-new "I/O y NIO.2" section, covering the first
half of its scope (`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`,
item 7): classic `java.io` — standard streams (`System.in`/`out`/`err`),
byte streams (`InputStream`/`OutputStream`), char streams
(`Reader`/`Writer`), and serialization
(`Serializable`/`ObjectInputStream`/`ObjectOutputStream`,
`transient`/`serialVersionUID`). NIO.2 (`java.nio.file`: `Path`, `Files`)
is explicitly deferred to sub-cycle 2. Design rationale and JDK
verification log:
`docs/superpowers/specs/2026-08-11-io-nio2-subcycle1-streams-serializacion-design.md`.

**Architecture:** Pure content-authoring plus one minimal, mechanical
Kotlin registration change — same pattern as Modulos y Empaquetado
sub-cycle 1, the only prior brand-new-section cycle in this series. The
new content lives entirely in a new file,
`app/src/main/assets/content/io-nio2.json`; the only Kotlin diff is
appending its path to `ContentPackRegistry.assetPaths` and bumping
`CURRENT_CONTENT_VERSION` in `ContentSeeder.kt`. Zero grandfathering
concern — every exercise in this brand-new file is new.

**Tech Stack:** JSON content pack, Kotlin/Room/Hilt app shell, JUnit4 for
the existing use-case test suite (must stay green) — including the new
`ContentCorpusLadderConsistencyTest`, which automatically scans every
file in `ContentPackRegistry.assetPaths` (this new file included, once
Task 3 registers it) for `fill_blank` `solo`/`practice` answers that
their own concept's `intro`/`guided` never taught.

## Global Constraints

- **Section identity:** `sectionId: "java-io-nio2"`,
  `name: "I/O y NIO.2"`, `orderIndex: 7` (sections 1-6 are already taken;
  Modulos y Empaquetado is 6). `examVersion: "core"` (matches 5 of 6
  existing section files; only `streams.json` uses `"java21"`).
- **`certObjective` is a single value shared by every unit in the file**
  (confirmed pattern across all 6 existing content files). Both units in
  this new file use `"certObjective": "io-nio2"`.
- **New file, not an append** — Task 1 uses Write (not Edit) to create
  `app/src/main/assets/content/io-nio2.json` from scratch. Task 2 then
  uses Edit to insert the second unit before the file's closing `]`/`}`.
  Both tasks must produce valid, complete JSON on their own — verify with
  `python3 -c "import json; json.load(open(...))"` after each.
- **Unit identity:**
  - `io-streams-clasicos` / "Streams clasicos", `orderIndex: 1`. 3
    concepts, 9 exercises, `pathOrder` 0-8.
  - `io-serializacion` / "Serializacion", `orderIndex: 2`. 3 concepts, 9
    exercises, `pathOrder` 0-8 (restarts at 0 — `pathOrder` is per-unit,
    matching every existing multi-unit file in this corpus).
- **dependsOn chain, both units:** each unit's 3 concepts form a single
  linear chain (concept 2 `dependsOn` concept 1; concept 3 `dependsOn`
  concept 2) — the pedagogical order matters here (byte streams before
  char streams so the `-1`-vs-`null` EOF contrast lands; `Serializable`
  basics before `writeObject`/`readObject` mechanics before
  `transient`/`serialVersionUID` nuance).
- **One-terminal-role rule:** every `conceptId` has exactly one exercise
  with `role: "solo"` or `role: "practice"` — never zero, never two.
- **dependsOn same-unit-only rule:** every id listed in a `dependsOn`
  array must be a `conceptId` that exists elsewhere in the *same* unit's
  `exercises` array.
- **Sequential pathOrder rule:** within each unit, `pathOrder` values run
  `0..n-1` with no gaps or duplicates, in the same physical array order.
- **Case-collision rule:** no `mcq`/`fill_blank` exercise's `distractors`
  may differ from its own `answer` only by letter case.
- **Difficulty monotonicity within a concept:** all 3 exercises sharing a
  `conceptId` use the identical `difficulty` value. This plan uses
  `difficulty: 1` for `consola-standard-streams`, `2` for
  `byte-streams-basicos`/`char-streams-basicos`/`serializable-basico`/
  `object-streams`, `3` for `serialversionuid-transient` (the subtlest
  concept in this sub-cycle).
- **fill_blank untaught-identifier rule** (new standing rule, enforced by
  `ContentCorpusLadderConsistencyTest`): a `fill_blank` `solo`'s `answer`
  must appear, case-insensitively, somewhere in its own concept's
  `intro`+`guided` prompt/code/explanation text. Only 2 of this
  sub-cycle's 6 solos are `fill_blank`
  (`io-consola-solo` → `InputStreamReader`, literally present in
  `io-consola-guided`'s explanation; `io-objstreams-solo` → `writeObject`,
  literally present in `io-objstreams-intro`'s code) — verify this
  explicitly for both during self-review, not just by running the test
  after the fact.
- **New standing rule this cycle: mcq distractors must be balanced in
  length/detail against the correct answer** (Luis's feedback, see
  `feedback_mcq_distractor_length_balance` memory) — every `mcq` answer
  and all 3 of its distractors below were written as full, comparable-length
  sentences; the correct option must never be identifiable just by being
  the longest/most detailed one. Verify this explicitly per exercise
  during self-review (eyeball word-count parity), not just structurally.
- **No accents in Spanish content** (project-wide convention, verified
  with a full-file accented-character scan in Task 3, range `[À-ÿ]` —
  this also catches `ñ`, so no word in this plan's content uses it). No
  voseo — tuteo only.
- **worked_example format:** every `intro`-role exercise has
  `"type": "worked_example"`, `"answer": "ok"`, no `distractors` field,
  and a `code` field showing the illustrative snippet.
- **No `predict_output` in this sub-cycle.** Every exercise below is
  `worked_example`, `mcq`, or `fill_blank`; the exact exception-message
  scenarios (`NotSerializableException`, `InvalidClassException` x2) are
  taught as `mcq`, quoting verified JDK text only inside `explanation`.
- **Runtime-observable content must be verified against a real JDK, not
  just read.** Every factual claim below was executed against a local
  JDK 20 during design (full log in the design spec) — do not re-derive
  from documentation alone if you need to modify anything; re-run it.
  Confirmed exact behaviors and error text used verbatim below:
  - `try-with-resources` with multiple resources closes them in **reverse
    declaration order**, regardless of the order they were used inside
    the block.
  - `InputStream.read()` returns an `int`: the byte read, or `-1` at end
    of stream (never `null` — the return type is a primitive).
  - `BufferedReader.readLine()` returns `null` at end of file (it returns
    a `String` reference, so `null` is possible, unlike `read()`'s `-1`).
  - Writing an object of a class that does NOT implement `Serializable`
    throws `java.io.NotSerializableException` at the `writeObject()` call
    (runtime, not a compile error — `writeObject(Object)` takes a plain
    `Object`), with the class's fully-qualified name as the message.
  - When a `Serializable` subclass has a non-`Serializable` superclass
    with no accessible no-arg constructor, `writeObject()` succeeds; the
    failure happens on `readObject()`, which throws
    `java.io.InvalidClassException` with message `"<Class>; no valid
    constructor"`.
  - Calling `writeObject()` twice with the *same* object reference, then
    `readObject()` twice, yields `a == b` (`true`) on the two results —
    `ObjectOutputStream` back-references an already-written object
    instead of duplicating it.
  - A `transient` field deserializes to its type's default value (`0`,
    `null`, `false`) regardless of what it held when serialized.
  - A `static` field is never serialized at all — after deserializing, code
    simply sees the class's *current* value, unrelated to either the
    value at serialization time or any value in between.
  - A `serialVersionUID` mismatch between the writer and reader classes
    throws `InvalidClassException` with message `"<Class>; local class
    incompatible: stream classdesc serialVersionUID = <old>, local class
    serialVersionUID = <new>"`, citing both values.

---

### Task 1: Create the new section file with Unit A (Streams clasicos)

**Files:**
- Create: `app/src/main/assets/content/io-nio2.json`

**Interfaces:**
- Consumes: nothing (brand-new file).
- Produces: a valid `ContentPack`-shaped JSON file (`sectionId`,
  `name`, `orderIndex`, `examVersion`, `units`) containing exactly one
  unit, `io-streams-clasicos`, with 3 concepts:
  `consola-standard-streams` (pathOrder 0-2, no `dependsOn`),
  `byte-streams-basicos` (pathOrder 3-5, `dependsOn:
  ["consola-standard-streams"]`), `char-streams-basicos` (pathOrder 6-8,
  `dependsOn: ["byte-streams-basicos"]`).

- [ ] **Step 1: Write the file**

Create `app/src/main/assets/content/io-nio2.json` with exactly this
content:

```json
{
  "sectionId": "java-io-nio2",
  "name": "I/O y NIO.2",
  "orderIndex": 7,
  "examVersion": "core",
  "units": [
    {
      "unitId": "io-streams-clasicos",
      "name": "Streams clasicos",
      "certObjective": "io-nio2",
      "orderIndex": 1,
      "summary": {
        "text": "En Java, la consola se accede via los standard streams: System.out y System.err ya son PrintStream, pero System.in es un InputStream crudo que hay que envolver (InputStreamReader + BufferedReader) para leer texto linea por linea. Los byte streams (InputStream/OutputStream, tipicamente FileInputStream/FileOutputStream) trabajan con bytes crudos y read() devuelve -1 al llegar al final. Los char streams (Reader/Writer, tipicamente FileReader/FileWriter) trabajan con texto, y metodos como readLine() devuelven null al llegar al final. Un try-with-resources con varios recursos los cierra siempre en orden inverso al de declaracion.",
        "code": "BufferedReader consola = new BufferedReader(new InputStreamReader(System.in));\ntry (FileInputStream bytes = new FileInputStream(\"datos.bin\");\n     BufferedReader texto = new BufferedReader(new FileReader(\"notas.txt\"))) {\n    int b = bytes.read();            // -1 al final\n    String linea = texto.readLine(); // null al final\n}"
      },
      "exercises": [
        {
          "id": "io-consola-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "System.out y System.err ya son PrintStream; System.in en cambio es un InputStream crudo de bytes",
          "code": "System.out.println(\"mensaje normal\");\nSystem.err.println(\"mensaje de error\");\nint primerByte = System.in.read();\nSystem.out.println(\"byte leido: \" + primerByte);",
          "answer": "ok",
          "explanation": "System.out/System.err ya son objetos PrintStream, por eso print()/println() funcionan directo sobre ellos. System.in en cambio es un InputStream crudo: su metodo read() devuelve un int con el siguiente byte leido (o -1 si se llego al final del flujo), no una linea de texto.",
          "conceptId": "consola-standard-streams",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "io-consola-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Por que no se puede llamar directo a un metodo tipo leerLinea() sobre System.in, y hace falta envolverlo en otra clase?",
          "answer": "Porque System.in es un InputStream crudo, sin ningun metodo propio para leer una linea de texto completa",
          "distractors": [
            "Porque System.in queda cerrado por defecto hasta que se abre explicitamente con un metodo especial",
            "Porque leer texto de la consola necesita un permiso adicional que se solicita mediante un envoltorio",
            "Porque System.in solo puede usarse dentro de un bloque try-with-resources, nunca fuera de el"
          ],
          "explanation": "InputStream define read() a nivel de bytes. Para leer texto linea por linea hace falta subir de nivel: InputStreamReader adapta esos bytes a caracteres, y BufferedReader agrega por encima el metodo readLine() que devuelve una linea completa.",
          "conceptId": "consola-standard-streams",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "io-consola-solo",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa para poder leer una linea de texto completa desde la consola:",
          "code": "BufferedReader br = new BufferedReader(new _____(System.in));\nString linea = br.readLine();",
          "answer": "InputStreamReader",
          "distractors": ["BufferedInputStream", "FileReader", "StringReader"],
          "explanation": "InputStreamReader es el adaptador que convierte el InputStream crudo de System.in en un Reader de caracteres; BufferedReader se apoya en ese Reader para ofrecer readLine().",
          "conceptId": "consola-standard-streams",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "io-bytestreams-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "InputStream/OutputStream leen y escriben bytes crudos; try-with-resources cierra varios recursos en orden inverso al de declaracion",
          "code": "try (FileOutputStream out = new FileOutputStream(\"datos.bin\");\n     FileInputStream in = new FileInputStream(\"datos.bin\")) {\n    out.write(65);\n    int leido = in.read();\n    System.out.println(leido);\n} // se cierran en este orden: in, luego out",
          "answer": "ok",
          "explanation": "FileOutputStream/FileInputStream son las implementaciones tipicas de OutputStream/InputStream sobre archivos, trabajando siempre con bytes (no texto). Cuando un try-with-resources declara varios recursos, Java los cierra en orden INVERSO al de declaracion: aqui se cerraria primero in, despues out.",
          "conceptId": "byte-streams-basicos",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["consola-standard-streams"]
        },
        {
          "id": "io-bytestreams-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "En 'try (FileOutputStream out = ...; FileInputStream in = ...) { ... }', si ambos close() lanzaran una excepcion, cual se cierra primero?",
          "answer": "El recurso 'in', el ultimo declarado, se cierra primero; 'out' se cierra despues, en orden inverso a como fueron declarados",
          "distractors": [
            "El recurso 'out', el primero declarado, se cierra primero; 'in' se cierra despues, en el mismo orden de declaracion",
            "Los dos se cierran al mismo tiempo, en paralelo, sin un orden garantizado entre ellos",
            "El orden de cierre depende del orden en que se usaron dentro del bloque, no del de declaracion"
          ],
          "explanation": "Java garantiza que los recursos de un try-with-resources se cierran en orden inverso al de su declaracion, sin importar el orden en que se usaron dentro del bloque: el ultimo declarado es el primero en cerrarse.",
          "conceptId": "byte-streams-basicos",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["consola-standard-streams"]
        },
        {
          "id": "io-bytestreams-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que devuelve InputStream.read() al llegar al final del archivo?",
          "answer": "Devuelve -1 como valor int, una marca fuera del rango normal de bytes",
          "distractors": [
            "Devuelve null, porque read() entrega una referencia a los bytes leidos en cada llamada",
            "Lanza una EOFException que hay que capturar para detectar el fin del archivo",
            "Devuelve 0, indicando que ya no quedan bytes disponibles para seguir leyendo"
          ],
          "explanation": "InputStream.read() devuelve un int: el byte leido (entre 0 y 255) o -1 si se llego al final del flujo. Como el tipo de retorno es un primitivo (no una clase), nunca puede devolver null.",
          "conceptId": "byte-streams-basicos",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["consola-standard-streams"]
        },
        {
          "id": "io-charstreams-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Reader/Writer trabajan con caracteres (texto), a diferencia de InputStream/OutputStream que trabajan con bytes crudos",
          "code": "try (BufferedReader br = new BufferedReader(new FileReader(\"notas.txt\"))) {\n    String linea;\n    while ((linea = br.readLine()) != null) {\n        System.out.println(linea);\n    }\n}",
          "answer": "ok",
          "explanation": "FileReader/FileWriter son las implementaciones tipicas de Reader/Writer sobre archivos, pensadas para texto (no bytes crudos). BufferedReader.readLine() devuelve cada linea como String, y devuelve null al llegar al final del archivo -- distinto del -1 que devuelve InputStream.read() en el caso de byte streams.",
          "conceptId": "char-streams-basicos",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["byte-streams-basicos"]
        },
        {
          "id": "io-charstreams-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cual es la diferencia principal entre usar InputStream/OutputStream y usar Reader/Writer para trabajar con un archivo?",
          "answer": "InputStream/OutputStream trabajan con bytes crudos sin interpretar; Reader/Writer trabajan con caracteres de texto, aplicando la codificacion correspondiente",
          "distractors": [
            "InputStream/OutputStream son mas lentos que Reader/Writer, sin ninguna diferencia real en que datos manejan",
            "Reader/Writer solo funcionan con archivos, mientras que InputStream/OutputStream tambien sirven para leer de la consola",
            "No hay diferencia real entre ambos pares de clases, son simplemente dos nombres distintos para lo mismo"
          ],
          "explanation": "InputStream/OutputStream operan a nivel de bytes crudos, sin ninguna nocion de texto o codificacion. Reader/Writer trabajan con caracteres, aplicando una codificacion (como UTF-8) para convertir entre bytes y texto -- por eso son la opcion correcta para archivos de texto.",
          "conceptId": "char-streams-basicos",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["byte-streams-basicos"]
        },
        {
          "id": "io-charstreams-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que devuelve BufferedReader.readLine() al llegar al final del archivo?",
          "answer": "Devuelve null, a diferencia del -1 que devuelve InputStream.read() en el caso de byte streams",
          "distractors": [
            "Devuelve -1, igual que InputStream.read(), ya que ambos usan la misma convencion de fin de flujo",
            "Lanza una IOException que hay que capturar para detectar que no quedan mas lineas",
            "Devuelve un String vacio, ya que readLine() siempre entrega una referencia valida a un String"
          ],
          "explanation": "readLine() devuelve un String (una referencia), asi que puede devolver null cuando no hay mas lineas -- distinto de read() en byte streams, que devuelve un int primitivo y usa -1 (no null, que no aplica a un primitivo) para marcar el fin.",
          "conceptId": "char-streams-basicos",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["byte-streams-basicos"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Validate the file parses**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/io-nio2.json')); print('OK')"`
Expected: `OK`, no exception.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/io-nio2.json
git commit -m "content: add I/O y NIO.2 section with Streams clasicos unit"
```

---

### Task 2: Add Unit B (Serializacion)

**Files:**
- Modify: `app/src/main/assets/content/io-nio2.json` (append a second
  unit to the existing `"units"` array)

**Interfaces:**
- Consumes: the file exactly as Task 1 left it — do not touch the
  `io-streams-clasicos` unit or any of its 9 exercises.
- Produces: unit `io-serializacion` (`orderIndex: 2`, `certObjective:
  "io-nio2"`), 3 concepts: `serializable-basico` (pathOrder 0-2, no
  `dependsOn`), `object-streams` (pathOrder 3-5, `dependsOn:
  ["serializable-basico"]`), `serialversionuid-transient` (pathOrder 6-8,
  `dependsOn: ["object-streams"]`).

- [ ] **Step 1: Insert the second unit**

In `app/src/main/assets/content/io-nio2.json`, find this exact trailing
text (copy it verbatim from the file Task 1 produced, do not retype from
memory):

```json
          "dependsOn": ["byte-streams-basicos"]
        }
      ]
    }
  ]
}
```

Replace it with this exact text — the same closing exercise and unit
close, now followed by a comma and the new `io-serializacion` unit
object, then the original closing `]` and `}`:

```json
          "dependsOn": ["byte-streams-basicos"]
        }
      ]
    },
    {
      "unitId": "io-serializacion",
      "name": "Serializacion",
      "certObjective": "io-nio2",
      "orderIndex": 2,
      "summary": {
        "text": "Serializable es una interfaz marcadora que habilita ObjectOutputStream.writeObject()/ObjectInputStream.readObject() sobre una clase. Si la superclase no es Serializable y no tiene constructor sin argumentos, la escritura funciona pero la lectura falla con InvalidClassException. Escribir la misma referencia varias veces preserva su identidad al leer (no la duplica). Los campos transient vuelven al valor por defecto al deserializar; los campos static nunca viajan, ya que pertenecen a la clase, no al objeto. Un serialVersionUID que no coincide entre escritura y lectura tambien lanza InvalidClassException.",
        "code": "class Sesion implements Serializable {\n    private static final long serialVersionUID = 1L;\n    String usuario;\n    transient String tokenTemporal; // vuelve a null al leer\n    static int sesionesActivas;    // nunca viaja\n}"
      },
      "exercises": [
        {
          "id": "io-serial-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Serializable es una interfaz marcadora (sin metodos) que habilita que ObjectOutputStream pueda escribir un objeto",
          "code": "class Punto implements Serializable {\n    int x;\n    int y;\n}\n\nPunto p = new Punto();\np.x = 3;\np.y = 4;\n// ObjectOutputStream.writeObject(p) funciona porque Punto implementa Serializable",
          "answer": "ok",
          "explanation": "Serializable no declara ningun metodo -- es solo una marca que le dice a la JVM que los objetos de esa clase pueden convertirse a una secuencia de bytes. Si una clase NO implementa Serializable, ObjectOutputStream.writeObject() sobre un objeto suyo lanza NotSerializableException con el nombre de la clase como mensaje.",
          "conceptId": "serializable-basico",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "io-serial-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que pasa si se intenta escribir con ObjectOutputStream.writeObject() un objeto de una clase que NO implementa Serializable?",
          "answer": "Se lanza NotSerializableException en tiempo de ejecucion, con el nombre de la clase como mensaje",
          "distractors": [
            "El compilador rechaza el codigo, ya que writeObject() exige Serializable como tipo del parametro",
            "El objeto se escribe igual, pero al leerlo con readObject() se recuperan solo los campos publicos",
            "Se lanza IllegalArgumentException, ya que el objeto no cumple con el contrato esperado"
          ],
          "explanation": "writeObject() recibe un Object generico -- no hay error de compilacion. El chequeo de si la clase es Serializable ocurre en tiempo de ejecucion, dentro del propio writeObject(), y si falla lanza NotSerializableException con el nombre de la clase como mensaje.",
          "conceptId": "serializable-basico",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "io-serial-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Una clase Hijo implementa Serializable, pero su superclase NO implementa Serializable y no tiene constructor publico sin argumentos. Que pasa al escribir y despues leer un objeto Hijo?",
          "answer": "La escritura (writeObject) funciona sin problema; la lectura (readObject) falla con InvalidClassException porque no encuentra un constructor valido para la superclase",
          "distractors": [
            "La escritura falla de entrada con NotSerializableException, ya que la superclase no serializable lo impide",
            "Tanto la escritura como la lectura funcionan bien, ya que Hijo si implementa Serializable correctamente",
            "La lectura funciona, pero los campos heredados de la superclase quedan siempre en null sin lanzar ningun error"
          ],
          "explanation": "Cuando la superclase no es Serializable, la JVM necesita poder invocar su constructor sin argumentos durante la deserializacion (para inicializarla como si fuera un objeto nuevo). writeObject() no necesita nada de eso, por lo que escribe sin problema; recien readObject() falla con InvalidClassException: no valid constructor.",
          "conceptId": "serializable-basico",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "io-objstreams-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "ObjectOutputStream.writeObject()/ObjectInputStream.readObject() escriben y leen el grafo completo de un objeto; escribir la misma referencia dos veces preserva identidad al leer",
          "code": "Punto p = new Punto();\np.x = 1;\ntry (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(\"p.ser\"))) {\n    oos.writeObject(p);\n    oos.writeObject(p); // la misma referencia, otra vez\n}\ntry (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(\"p.ser\"))) {\n    Punto a = (Punto) ois.readObject();\n    Punto b = (Punto) ois.readObject();\n    System.out.println(a == b); // true\n}",
          "answer": "ok",
          "explanation": "ObjectOutputStream detecta que ya escribio esa referencia exacta y, la segunda vez, guarda solo una marca de 'ya escrito' en vez de duplicar los datos. Al leer, ObjectInputStream reconstruye un unico objeto y devuelve la misma referencia las dos veces: a == b da true.",
          "conceptId": "object-streams",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["serializable-basico"]
        },
        {
          "id": "io-objstreams-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Si se llama oos.writeObject(p) dos veces seguidas sobre la misma referencia p, y despues se lee dos veces con readObject(), que relacion tienen los dos objetos leidos?",
          "answer": "Son la misma referencia (==), porque ObjectOutputStream no duplica un objeto que ya escribio antes",
          "distractors": [
            "Son dos objetos distintos pero con los mismos valores de campos (equals() daria true, == daria false)",
            "El segundo readObject() lanza una excepcion, ya que el stream no permite leer el mismo objeto dos veces",
            "Son dos objetos distintos y con valores diferentes, ya que cada writeObject() genera una copia nueva"
          ],
          "explanation": "ObjectOutputStream mantiene una tabla interna de que referencias ya escribio en ese stream. La segunda vez que ve la misma referencia, no vuelve a serializar los datos: guarda una referencia de vuelta. Al leer, ObjectInputStream reconstruye un solo objeto y ambas lecturas devuelven esa misma referencia.",
          "conceptId": "object-streams",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["serializable-basico"]
        },
        {
          "id": "io-objstreams-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el metodo que hay que llamar para escribir un objeto completo en un ObjectOutputStream:",
          "code": "Punto p = new Punto();\noos._____(p);",
          "answer": "writeObject",
          "distractors": ["write", "writeObj", "save"],
          "explanation": "writeObject() es el metodo de ObjectOutputStream que serializa el grafo completo de un objeto (a diferencia de write(), que existe pero opera a nivel de bytes crudos, heredado de OutputStream).",
          "conceptId": "object-streams",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["serializable-basico"]
        },
        {
          "id": "io-uidtransient-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "transient excluye un campo de la serializacion (vuelve al valor por defecto al leer); un campo static queda afuera siempre, sin marcarlo",
          "code": "class Sesion implements Serializable {\n    private static final long serialVersionUID = 1L;\n    String usuario;\n    transient String tokenTemporal;\n    static int sesionesActivas = 0;\n}\n// tokenTemporal viaja como null al deserializar, aunque tuviera un valor al serializar\n// sesionesActivas nunca viaja: pertenece a la clase, no al objeto",
          "answer": "ok",
          "explanation": "transient le dice a la serializacion 'salta este campo': al deserializar, ese campo vuelve al valor por defecto de su tipo (null para String, 0 para numericos, false para boolean). Los campos static ni siquiera hace falta marcarlos: pertenecen a la clase, no a ningun objeto, asi que la serializacion (que opera sobre instancias) los ignora por completo.",
          "conceptId": "serialversionuid-transient",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["object-streams"]
        },
        {
          "id": "io-uidtransient-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una clase tiene un campo 'static int contador'. Se serializa un objeto, despues se cambia el valor de contador, y despues se deserializa ese mismo objeto. Que valor tiene contador en el objeto leido?",
          "answer": "El valor actual de la clase en el momento de leer, ya que los campos static nunca viajan dentro de los datos serializados",
          "distractors": [
            "El valor que tenia contador en el momento exacto de serializar el objeto, guardado junto con el resto de los datos",
            "El valor por defecto del tipo (0), igual que pasaria con un campo transient de tipo int",
            "Depende de si el campo static fue marcado como transient o no antes de serializar el objeto"
          ],
          "explanation": "Un campo static es una propiedad de la clase, no del objeto -- la serializacion opera sobre el estado de instancias, asi que nunca toca campos static (marcarlos transient no cambia nada, ya de por si estan fuera). Por eso, tras deserializar, el codigo simplemente ve el valor actual de la clase en ese momento, sin relacion con ningun momento de la serializacion.",
          "conceptId": "serialversionuid-transient",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["object-streams"]
        },
        {
          "id": "io-uidtransient-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una clase se serializa con 'private static final long serialVersionUID = 1L'. Despues se cambia ese valor a 2L en el codigo y se intenta leer los bytes viejos (serializados con 1L). Que pasa?",
          "answer": "Se lanza InvalidClassException, con un mensaje que cita ambos valores de serialVersionUID (el del stream y el de la clase actual)",
          "distractors": [
            "La lectura funciona igual, ya que serialVersionUID solo se usa para documentacion y no se valida en runtime",
            "Se lanza ClassNotFoundException, ya que la clase con ese UID especifico ya no existe en el classpath",
            "La lectura funciona, pero todos los campos del objeto vuelven a sus valores por defecto en vez de los originales"
          ],
          "explanation": "serialVersionUID identifica una version compatible de la clase para deserializar. Si el valor guardado en los bytes no coincide con el de la clase actual, ObjectInputStream lanza InvalidClassException con un mensaje del tipo 'local class incompatible: stream classdesc serialVersionUID = 1, local class serialVersionUID = 2', citando ambos valores.",
          "conceptId": "serialversionuid-transient",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["object-streams"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Validate the file parses**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/io-nio2.json')); print('OK')"`
Expected: `OK`, no exception.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/content/io-nio2.json
git commit -m "content: add Serializacion unit to I/O y NIO.2"
```

---

### Task 3: Register the section, validate the whole file, bump content version

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`
- Read-only validation: `app/src/main/assets/content/io-nio2.json`

**Interfaces:**
- Consumes: the final state of `io-nio2.json` after Task 2 (2 units, 18
  exercises total).
- Produces: the new section registered and loadable;
  `CURRENT_CONTENT_VERSION` bumped by one from whatever it is at dispatch
  time (check `ContentSeeder.kt`'s current value first — do not assume a
  specific number; it was `"21"` at plan-writing time, but other cycles
  may have landed between this plan being written and executed).

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
        "content/concurrency.json",
        "content/modules-packaging.json"
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
        "content/modules-packaging.json",
        "content/io-nio2.json"
    )
}
```

This is the only change in this file — nothing else in
`ContentPackRegistry.kt` is touched.

- [ ] **Step 2: Write and run a full validation script**

```bash
python3 << 'EOF'
import json, re

path = "app/src/main/assets/content/io-nio2.json"
data = json.load(open(path))

assert data["sectionId"] == "java-io-nio2"
assert data["orderIndex"] == 7
assert data["examVersion"] == "core"

all_units = {u["unitId"]: u for u in data["units"]}
assert set(all_units.keys()) == {"io-streams-clasicos", "io-serializacion"}, f"unexpected units: {list(all_units.keys())}"

expected_counts = {"io-streams-clasicos": 9, "io-serializacion": 9}
expected_order = {"io-streams-clasicos": 1, "io-serializacion": 2}
all_exercises = []
for uid, expected in expected_counts.items():
    unit = all_units[uid]
    exercises = unit["exercises"]
    assert len(exercises) == expected, f"{uid}: expected {expected} exercises, got {len(exercises)}"
    assert unit["certObjective"] == "io-nio2", f"{uid}: unexpected certObjective {unit['certObjective']}"
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

# fill_blank untaught-identifier rule (same check as ContentCorpusLadderConsistencyTest).
for uid, exercises in all_exercises:
    by_concept = {}
    for e in exercises:
        if e.get("conceptId"):
            by_concept.setdefault(e["conceptId"], []).append(e)
    for cid, exs in by_concept.items():
        taught = " ".join(f"{e['prompt']} {e.get('code','')} {e['explanation']}" for e in exs if e.get("role") in ("intro", "guided")).lower()
        for e in exs:
            if e["type"] == "fill_blank" and e.get("role") in ("solo", "practice"):
                token = e["answer"].strip().rstrip("()").lower()
                assert token in taught, f"{uid}/{e['id']}: answer '{e['answer']}' never taught in its own concept's intro/guided"
print("fill_blank untaught-identifier rule passed.")

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
print(f"Total exercises across both units: {total}")
assert total == 18
EOF
```

Expected: `Unit counts, certObjective, and orderIndex all correct.` then
`One-terminal-role, dependsOn, sequential pathOrder, and difficulty-
monotonicity rules all passed.` then `fill_blank untaught-identifier rule
passed.` then `No accented characters, no predict_output.` then two
reachability lines (one per unit), then
`Total exercises across both units: 18`, no assertion errors.

- [ ] **Step 3: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find the current `CURRENT_CONTENT_VERSION` value and increment it by one
(check the file first — do not assume a specific starting number).

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still passing, including
`ContentCorpusLadderConsistencyTest` picking up this new file
automatically via `ContentPackRegistry.assetPaths` (no test file changes
needed for this task).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: register I/O y NIO.2 section and bump content version"
```

---

## After the plan: manual on-device QA

Install a clean/in-place build and manually verify on-device — per the
standing project lesson, `adb input tap` automation is not reliable for
answering exercises (only for navigation/screenshots). Play both units
end to end (18 exercises total), confirming: `worked_example` intros
render before their guided/solo steps; the `dependsOn` chain unlocks
concepts one at a time within each unit; both `fill_blank` solos
(`InputStreamReader`, `writeObject`) grade correctly when typed; the new
section appears correctly positioned in Ruta right after Modulos y
Empaquetado.
