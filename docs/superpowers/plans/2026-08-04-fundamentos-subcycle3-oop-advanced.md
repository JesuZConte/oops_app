# Fundamentos de Java — Sub-ciclo 3 (OOP Avanzado) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the last OOP-related objective gap in Fundamentos de Java —
advanced interfaces (default/private/static methods) and functional
interfaces/lambda syntax as a standalone fundamental, not just used
implicitly inside Streams. Third and final sub-cycle bringing this section
to full 1Z0-830 coverage. Also lightly retrofits "Que es Java?" and "Tipos,
variables y el metodo main" with first-exposure ladders, per
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md`.

**Architecture:** Content-only — one JSON pack edit, one version bump, no
Kotlin/Compose changes. Two kinds of edit in the same file: (a) a **light
retrofit** of `fund-what-is-java` (2 new concepts) and `fund-types-and-main`
(1 new concept) — existing exercises keep their exact `id`/`type`/`prompt`/
`answer`/etc. unchanged and only gain `conceptId`/`role`/`pathOrder`; (b)
**2 new units appended** after `fund-enums` (orderIndex 11, added in
sub-cycle 2).

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
- **One-terminal-role rule (learned the hard way in sub-cycle 2's final
  review — this is a hard rule now):** every `conceptId` must have
  **exactly one** exercise with `role: "solo"` or `role: "practice"` — never
  zero (the concept would never be marked "born", permanently blocking
  anything that `dependsOn` it) and never more than one (`GetTodaySessionUseCase`
  marks a concept "born" the moment ANY ONE terminal-role exercise is
  answered, permanently excluding every other unanswered terminal-role
  exercise of that same concept — a real bug found and fixed in sub-cycle 2).
  Every concept below has been designed with exactly one terminal role;
  verify this explicitly in Step 5 below before considering the task done.
- **Grandfathering rule (retrofit task):** when adding `conceptId`/`role`/
  `pathOrder` to an *existing* exercise, never change its `id`, `type`,
  `prompt`, `code`, `answer`, `distractors`, or `explanation` — only add the
  3 new fields.
- **Grading-safety rule:** for `mcq`/`fill_blank`, no distractor may differ
  from the `answer` only by capitalization (case-insensitive grading).
- Every unit mixes exam/syntax, code-classification, and interview flavors.
  Interview prompts use generic company framing ("una consultora IT
  grande", "una empresa de servicios financieros") — never real brand names.
- Every new unit includes a `summary: {text, code}` field. Do not add a
  `summary` to `fund-what-is-java` or `fund-types-and-main` — they already
  have one, unchanged.
- `ContentSeeder`'s `CURRENT_CONTENT_VERSION`
  (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`)
  must bump from `"9"` to `"10"`.
- `java-fundamentals.json` is already registered in `ContentPackRegistry.kt`
  — no registry edit needed.
- New unit id prefix: `fund-`. `certObjective` for both new units:
  `language-basics`. `orderIndex` 12-13, continuing after `fund-enums` (11).
- No Room schema change, no migration.

---

### Task 1: Retrofit `fund-what-is-java` + `fund-types-and-main`, and append 2 new interfaces/lambdas units

**Files:**
- Modify: `app/src/main/assets/content/java-fundamentals.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: nothing new — same generic JSON pack parsing already proven.
- Produces: nothing — this plan has only one task.

- [ ] **Step 1: Retrofit `fund-what-is-java`**

Find the unit with `"unitId": "fund-what-is-java"`. Its `exercises` array
currently has 7 objects: `fund-whatis-01` through `fund-whatis-06`, then
`fund-predict-01`.

**1a. Add these 3 fields to `fund-whatis-01`** (do not change anything
else): `"conceptId": "jdk-jvm-jre", "role": "solo", "pathOrder": 5`

**1b. Add these 3 fields to `fund-whatis-03`**: `"conceptId": "compilacion-bytecode", "role": "solo", "pathOrder": 2`

**1c. Leave `fund-whatis-02`, `fund-whatis-04`, `fund-whatis-05`,
`fund-whatis-06`, `fund-predict-01` completely unchanged.**

**1d. Insert these 4 new exercises into the array** — `fund-compilacion-intro`
and `fund-compilacion-guided` go *before* `fund-whatis-03` (so pathOrder 0-1
precede pathOrder 2); `fund-jdkjvm-intro` and `fund-jdkjvm-guided` go
*before* `fund-whatis-01` (so pathOrder 3-4 precede pathOrder 5) — note
`fund-whatis-01` is the *first* exercise in the array, so these 2 new ones
become the new first elements:

```json
{
  "id": "fund-jdkjvm-intro",
  "type": "worked_example",
  "difficulty": 1,
  "prompt": "El JDK incluye herramientas de desarrollo; el JRE solo lo necesario para ejecutar",
  "code": "JDK = JRE + herramientas (javac, jshell, etc.)\nJRE = JVM + librerias estandar (sin compilador)",
  "answer": "ok",
  "explanation": "Si solo vas a ejecutar programas Java ya compilados, alcanza con el JRE; para desarrollar (compilar codigo propio) hace falta el JDK.",
  "conceptId": "jdk-jvm-jre",
  "role": "intro",
  "pathOrder": 3
},
{
  "id": "fund-jdkjvm-guided",
  "type": "mcq",
  "difficulty": 2,
  "prompt": "Si solo necesitas ejecutar un .jar ya compilado, sin escribir ni compilar codigo, que es suficiente instalar?",
  "answer": "El JRE (no hace falta el JDK completo)",
  "distractors": ["El JDK completo, siempre es obligatorio", "Solo el compilador javac, sin el resto del JRE", "No se puede ejecutar Java sin instalar herramientas de desarrollo"],
  "explanation": "El JRE trae la JVM y las librerias necesarias para ejecutar; el JDK agrega herramientas de desarrollo (como javac) que no hacen falta si solo vas a correr programas ya compilados.",
  "conceptId": "jdk-jvm-jre",
  "role": "guided",
  "pathOrder": 4
},
{
  "id": "fund-compilacion-intro",
  "type": "worked_example",
  "difficulty": 1,
  "prompt": "javac compila el codigo fuente a bytecode; la JVM ejecuta ese bytecode",
  "code": "javac Main.java   // genera Main.class (bytecode)\njava Main          // la JVM ejecuta Main.class",
  "answer": "ok",
  "explanation": "El bytecode es un formato intermedio independiente del sistema operativo; por eso el mismo Main.class corre en cualquier maquina que tenga una JVM instalada, sin recompilar.",
  "conceptId": "compilacion-bytecode",
  "role": "intro",
  "pathOrder": 0
},
{
  "id": "fund-compilacion-guided",
  "type": "mcq",
  "difficulty": 2,
  "prompt": "Por que el bytecode generado por javac es independiente del sistema operativo?",
  "answer": "Porque no es codigo maquina nativo: es un formato intermedio que interpreta la JVM, y hay una JVM distinta para cada plataforma",
  "distractors": ["Porque javac genera codigo maquina para todas las plataformas a la vez", "Porque el bytecode se convierte a texto plano legible", "No es realmente independiente, solo funciona en la plataforma donde se compilo"],
  "explanation": "La JVM es la capa que traduce ese bytecode generico a instrucciones especificas de cada sistema operativo/procesador - por eso 'write once, run anywhere'.",
  "conceptId": "compilacion-bytecode",
  "role": "guided",
  "pathOrder": 1
}
```

**Exact placement:** the array's final order must be: `fund-jdkjvm-intro`,
`fund-jdkjvm-guided`, `fund-compilacion-intro`, `fund-compilacion-guided`,
`fund-whatis-01` (now retagged), `fund-whatis-02` (untouched),
`fund-compilacion... ` — wait, simplest correct approach: **insert all 4
new exercises at the very start of the array, in this exact order:
`fund-compilacion-intro`, `fund-compilacion-guided`, `fund-jdkjvm-intro`,
`fund-jdkjvm-guided`** — then the original 7 exercises follow in their
original order (with `fund-whatis-01` and `fund-whatis-03` carrying their
new fields from steps 1a/1b). The `pathOrder` values (0,1 for compilacion;
3,4 for jdkjvm; 2 and 5 on the retagged originals) are what determines ladder
sequencing for the engine — array position does not need to match
`pathOrder` order, but placing the 4 new ones first keeps the file readable.

- [ ] **Step 2: Retrofit `fund-types-and-main`**

Find the unit with `"unitId": "fund-types-and-main"`. Its `exercises` array
has 7 objects: `fund-main-01` through `fund-main-05`, `fund-parsons-02`,
`fund-predict-02`.

**2a. Add these 3 fields to `fund-main-05`** (do not change anything else):
`"conceptId": "metodo-main", "role": "guided", "pathOrder": 1`

**2b. Add these 3 fields to `fund-main-01`**: `"conceptId": "metodo-main", "role": "solo", "pathOrder": 2`

**2c. Leave `fund-main-02`, `fund-main-03`, `fund-main-04`,
`fund-parsons-02`, `fund-predict-02` completely unchanged.**

**2d. Insert this 1 new exercise at the start of the array** (before
`fund-main-01`):

```json
{
  "id": "fund-metodomain-intro",
  "type": "worked_example",
  "difficulty": 1,
  "prompt": "El metodo main es el punto de entrada de un programa Java",
  "code": "public static void main(String[] args) {\n    System.out.println(\"Hola\");\n}",
  "answer": "ok",
  "explanation": "La JVM busca exactamente esta firma para arrancar el programa: public, static, void, main, y un array de String como parametro. Sin ella, java no sabe por donde empezar a ejecutar.",
  "conceptId": "metodo-main",
  "role": "intro",
  "pathOrder": 0
}
```

- [ ] **Step 3: Append the 2 new units to the `units` array**

Add these 2 units as the new last elements of `java-fundamentals.json`'s
`units` array (after `fund-enums`, remembering the comma):

```json
    {
      "unitId": "fund-interfaces-avanzadas",
      "name": "Interfaces avanzadas",
      "certObjective": "language-basics",
      "orderIndex": 12,
      "summary": {
        "text": "Desde Java 8, las interfaces pueden tener default methods (con cuerpo, heredado por las clases que implementan la interfaz) y private methods (para compartir codigo entre default methods sin exponerlo publicamente). Tambien pueden tener static methods, invocados directamente sobre la interfaz (InterfaceName.metodo()), nunca sobre una instancia. Si una clase implementa dos interfaces con un default method de igual firma, debe sobreescribirlo explicitamente para resolver el conflicto.",
        "code": "interface Formateador {\n    String formatear(String texto);\n\n    default String formatearMayuscula(String texto) {\n        return formatear(texto).toUpperCase();\n    }\n\n    static Formateador identidad() {\n        return texto -> texto;\n    }\n}"
      },
      "exercises": [
        {
          "id": "fund-default-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un default method en una interfaz tiene cuerpo y se hereda automaticamente",
          "code": "interface Formateador {\n    String formatear(String texto);\n\n    default String formatearMayuscula(String texto) {\n        return formatear(texto).toUpperCase();\n    }\n}",
          "answer": "ok",
          "explanation": "Antes de Java 8, agregar un metodo nuevo a una interfaz rompia todas las clases que ya la implementaban. default permite agregar metodos con una implementacion por defecto sin forzar a las clases existentes a implementarlo.",
          "conceptId": "interfaz-default-method",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-default-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que se agregaron los default methods a las interfaces en Java 8?",
          "answer": "Para poder agregar metodos nuevos a una interfaz existente sin romper las clases que ya la implementan",
          "distractors": ["Para que las interfaces puedan tener estado (fields)", "Para reemplazar completamente a las clases abstractas", "Es un requisito del compilador desde Java 8, sin relacion con compatibilidad"],
          "explanation": "Sin default, agregar un metodo abstracto nuevo a una interfaz publica (como las de la biblioteca estandar) obligaria a todas sus implementaciones existentes a agregarlo tambien, rompiendo compatibilidad hacia atras.",
          "conceptId": "interfaz-default-method",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-default-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "interface Formateador {\n    String formatear(String texto);\n\n    default String formatearMayuscula(String texto) {\n        return formatear(texto).toUpperCase();\n    }\n}\n\nFormateador f = texto -> texto + \"!\";\nSystem.out.println(f.formatearMayuscula(\"hola\"));",
          "answer": "HOLA!",
          "explanation": "El lambda implementa formatear() devolviendo texto + \"!\"; formatearMayuscula() llama a formatear(\"hola\") (que da \"hola!\") y lo pasa a toUpperCase(), dando HOLA!.",
          "conceptId": "interfaz-default-method",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-private-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Un private method en una interfaz comparte codigo entre default methods, sin exponerlo",
          "code": "interface Validador {\n    boolean esValido(String texto);\n\n    private String limpiar(String texto) {\n        return texto.trim().toLowerCase();\n    }\n\n    default boolean esValidoLimpio(String texto) {\n        return esValido(limpiar(texto));\n    }\n}",
          "answer": "ok",
          "explanation": "private methods (Java 9+) permiten factorizar codigo comun usado por varios default methods, sin que ese codigo forme parte del contrato publico de la interfaz - no pueden ser implementados ni sobreescritos por las clases que la implementan.",
          "conceptId": "interfaz-private-method",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["interfaz-default-method"]
        },
        {
          "id": "fund-private-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Puede una clase que implementa una interfaz sobreescribir uno de sus private methods?",
          "answer": "No, los private methods de una interfaz no son parte de su contrato publico y no son visibles ni sobreescribibles desde afuera",
          "distractors": ["Si, cualquier metodo de una interfaz puede sobreescribirse", "Solo si la clase esta en el mismo paquete que la interfaz", "Si, pero unicamente usando la palabra clave super"],
          "explanation": "Los private methods existen solo para uso interno de la propia interfaz (compartir codigo entre sus default/static methods); son invisibles fuera de la interfaz, igual que un private method de una clase.",
          "conceptId": "interfaz-private-method",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["interfaz-default-method"]
        },
        {
          "id": "fund-private-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que Validador declara limpiar() como private en vez de default?",
          "answer": "Porque limpiar() es un detalle de implementacion interno, no algo que las clases que implementan Validador deban conocer o poder sobreescribir",
          "distractors": ["Porque private es obligatorio para todo metodo auxiliar en una interfaz", "No hay diferencia real entre declararlo private o default en este caso", "Porque los default methods no pueden llamar a otros metodos de la interfaz"],
          "explanation": "private mantiene limpiar() como un detalle interno reutilizado por default methods, sin exponerlo como parte del contrato publico de la interfaz - exactamente el mismo motivo por el que se usan private methods dentro de una clase.",
          "conceptId": "interfaz-private-method",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["interfaz-default-method"]
        },
        {
          "id": "fund-static-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un static method de una interfaz se invoca sobre la interfaz misma, nunca sobre una instancia",
          "code": "interface Formateador {\n    String formatear(String texto);\n\n    static Formateador identidad() {\n        return texto -> texto;\n    }\n}\n\nFormateador f = Formateador.identidad();",
          "answer": "ok",
          "explanation": "Igual que un static method de una clase, se llama con InterfaceName.metodo() - no a traves de una instancia. Es comun usarlo como factory method que devuelve una implementacion por defecto.",
          "conceptId": "interfaz-static-method",
          "role": "intro",
          "pathOrder": 6
        },
        {
          "id": "fund-static-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Como se invoca un static method declarado en una interfaz Formateador?",
          "answer": "Formateador.metodo(), directamente sobre el nombre de la interfaz",
          "distractors": ["instancia.metodo(), como cualquier metodo de instancia", "Solo puede llamarse desde dentro de otra interfaz", "new Formateador().metodo()"],
          "explanation": "Los static methods de interfaz se comportan igual que los static methods de una clase: se invocan sobre el tipo, no sobre un objeto - de hecho una interfaz no puede instanciarse directamente con new.",
          "conceptId": "interfaz-static-method",
          "role": "guided",
          "pathOrder": 7
        },
        {
          "id": "fund-static-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa la invocacion del static method identidad():",
          "code": "Formateador f = _____.identidad();",
          "answer": "Formateador",
          "distractors": ["f", "new Formateador()", "this"],
          "explanation": "Un static method se invoca sobre el nombre del tipo (la interfaz), no sobre una variable ni con new.",
          "conceptId": "interfaz-static-method",
          "role": "solo",
          "pathOrder": 8
        },
        {
          "id": "fund-interfaces-conflicto",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una clase implementa dos interfaces, ambas con un default method llamado procesar() con la misma firma. Que pasa si la clase no sobreescribe procesar()?",
          "answer": "El codigo no compila: la clase debe sobreescribir procesar() explicitamente para resolver el conflicto",
          "distractors": ["Java elige automaticamente el de la primera interfaz declarada en implements", "Se lanza una excepcion en tiempo de ejecucion la primera vez que se llama a procesar()", "Ambos default methods se ejecutan en el orden en que fueron declarados"],
          "explanation": "A diferencia de la herencia de clases (donde solo hay una superclase), una clase puede implementar varias interfaces; si dos aportan el mismo default method, Java no elige por vos - exige que la clase lo sobreescriba explicitamente para evitar ambiguedad."
        },
        {
          "id": "fund-interfaces-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una consultora IT grande te pregunta: por que las interfaces con default methods no reemplazan a las clases abstractas?",
          "answer": "Porque una interfaz no puede tener estado (fields de instancia) ni constructores, mientras que una clase abstracta si - cada una resuelve un problema de diseno distinto",
          "distractors": ["Si las reemplazan completamente, son intercambiables en cualquier caso", "Porque las interfaces no pueden tener metodos con cuerpo", "Porque una clase solo puede implementar una interfaz, igual que con la herencia"],
          "explanation": "Las interfaces con default methods agregan comportamiento compartido sin estado; las clases abstractas pueden combinar comportamiento y estado (fields), y participan en la jerarquia de herencia simple - son herramientas complementarias, no sustitutas una de la otra."
        }
      ]
    },
    {
      "unitId": "fund-interfaces-funcionales-lambda",
      "name": "Interfaces funcionales y lambdas",
      "certObjective": "language-basics",
      "orderIndex": 13,
      "summary": {
        "text": "Una interfaz funcional tiene exactamente un metodo abstracto (Single Abstract Method, SAM) - puede tener default/static methods ademas, pero solo uno abstracto. La anotacion @FunctionalInterface no es obligatoria, pero le pide al compilador que verifique esa regla. Una lambda expression implementa ese unico metodo abstracto de forma compacta: (parametros) -> cuerpo. El compilador infiere el tipo de los parametros a partir del contexto; tambien se puede usar var explicitamente en la lista de parametros. Una lambda de una sola expresion la devuelve implicitamente; una lambda con bloque { } (statement lambda) necesita return explicito si devuelve un valor.",
        "code": "@FunctionalInterface\ninterface Operacion {\n    int aplicar(int a, int b);\n}\n\nOperacion suma = (a, b) -> a + b;\nOperacion sumaConVar = (var a, var b) -> a + b;\nOperacion sumaBloque = (a, b) -> {\n    int resultado = a + b;\n    return resultado;\n};"
      },
      "exercises": [
        {
          "id": "fund-funcional-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Una interfaz funcional tiene exactamente un metodo abstracto",
          "code": "@FunctionalInterface\ninterface Operacion {\n    int aplicar(int a, int b);\n}",
          "answer": "ok",
          "explanation": "Solo puede tener UN metodo abstracto (Single Abstract Method); puede tener ademas default/static/private methods sin problema, ya que esos no cuentan como abstractos.",
          "conceptId": "interfaz-funcional",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-funcional-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que hace la anotacion @FunctionalInterface sobre una interfaz?",
          "answer": "Le pide al compilador que verifique que la interfaz tenga exactamente un metodo abstracto, marcando error si no es asi",
          "distractors": ["Convierte automaticamente la interfaz en una clase", "Es obligatoria para poder usar lambdas con esa interfaz", "Permite que la interfaz tenga mas de un metodo abstracto"],
          "explanation": "@FunctionalInterface es opcional - una interfaz con un solo metodo abstracto es funcional de todas formas - pero documenta la intencion y hace que el compilador te avise si accidentalmente agregas un segundo metodo abstracto.",
          "conceptId": "interfaz-funcional",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-funcional-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una interfaz tiene un metodo abstracto y ademas dos default methods. Sigue siendo una interfaz funcional?",
          "answer": "Si, porque solo cuenta la cantidad de metodos abstractos (uno); los default methods no afectan esa cuenta",
          "distractors": ["No, una interfaz funcional no puede tener default methods", "No, deja de ser funcional en cuanto tiene mas de un metodo en total", "Solo si los default methods estan marcados como @FunctionalInterface tambien"],
          "explanation": "La regla de interfaz funcional cuenta unicamente metodos abstractos; default, static y private methods pueden coexistir libremente sin romper esa condicion.",
          "conceptId": "interfaz-funcional",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-lambdasintaxis-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Una lambda implementa el metodo abstracto de una interfaz funcional: (parametros) -> cuerpo",
          "code": "Operacion suma = (a, b) -> a + b;\nint resultado = suma.aplicar(2, 3); // 5",
          "answer": "ok",
          "explanation": "El compilador infiere que a y b son int a partir de la firma de aplicar(int, int) en Operacion - no hace falta declarar el tipo explicitamente.",
          "conceptId": "lambda-sintaxis",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["interfaz-funcional"]
        },
        {
          "id": "fund-lambdasintaxis-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "De donde infiere el compilador el tipo de los parametros de una lambda como (a, b) -> a + b?",
          "answer": "De la firma del metodo abstracto de la interfaz funcional a la que se asigna la lambda",
          "distractors": ["Siempre asume que son Object", "Del tipo de la primera variable usada en el cuerpo", "Las lambdas nunca pueden omitir el tipo de sus parametros"],
          "explanation": "El compilador mira el contexto: si Operacion.aplicar(int, int) es el metodo que la lambda implementa, a y b se infieren como int automaticamente.",
          "conceptId": "lambda-sintaxis",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["interfaz-funcional"]
        },
        {
          "id": "fund-lambdasintaxis-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "Operacion resta = (a, b) -> a - b;\nSystem.out.println(resta.aplicar(10, 4));",
          "answer": "6",
          "explanation": "La lambda implementa aplicar(int a, int b) devolviendo a - b; con a=10 y b=4, el resultado es 6.",
          "conceptId": "lambda-sintaxis",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["interfaz-funcional"]
        },
        {
          "id": "fund-lambdavar-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Se puede usar var explicitamente en los parametros de una lambda",
          "code": "Operacion suma = (var a, var b) -> a + b;\n// equivalente a (a, b) -> a + b",
          "answer": "ok",
          "explanation": "var en parametros de lambda (Java 11+) es util sobre todo para poder agregar anotaciones a los parametros; el tipo real se sigue infiriendo igual que sin var - no se puede mezclar var con parametros sin tipo en la misma lambda (o todos usan var, o ninguno).",
          "conceptId": "lambda-var-params",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["lambda-sintaxis"]
        },
        {
          "id": "fund-lambdavar-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que regla aplica si queres usar var en los parametros de una lambda con mas de un parametro?",
          "answer": "Todos los parametros deben usar var, no se puede mezclar var con parametros sin tipo declarado",
          "distractors": ["Solo el primer parametro puede usar var", "No hay ninguna restriccion, se puede mezclar libremente", "var solo esta permitido si la lambda tiene un unico parametro"],
          "explanation": "Java exige consistencia: (var a, b) -> ... no compila - o todos los parametros declaran var, o ninguno lo hace.",
          "conceptId": "lambda-var-params",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["lambda-sintaxis"]
        },
        {
          "id": "fund-lambdavar-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cual de estas lambdas usa var correctamente?",
          "answer": "(var a, var b) -> a + b",
          "distractors": ["(var a, b) -> a + b", "(a, var b) -> a + b", "(var a, var Integer b) -> a + b"],
          "explanation": "Si se usa var en alguno de los parametros, todos deben usar var - mezclar var con tipo implicito o con un tipo explicito distinto no compila.",
          "conceptId": "lambda-var-params",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["lambda-sintaxis"]
        },
        {
          "id": "fund-statementlambda-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Una statement lambda (con bloque { }) necesita return explicito para devolver un valor",
          "code": "Operacion suma = (a, b) -> {\n    int resultado = a + b;\n    return resultado;\n};\n// distinto de: (a, b) -> a + b; (expression lambda, return implicito)",
          "answer": "ok",
          "explanation": "Con { }, el cuerpo es un bloque de statements como el de cualquier metodo - si el metodo abstracto devuelve un valor, hace falta un return explicito dentro del bloque; sin { }, una sola expresion se devuelve implicitamente.",
          "conceptId": "statement-lambda",
          "role": "intro",
          "pathOrder": 9,
          "dependsOn": ["lambda-sintaxis"]
        },
        {
          "id": "fund-statementlambda-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que este codigo no compila: Operacion suma = (a, b) -> { a + b; };",
          "answer": "Porque al usar bloque { }, hace falta un return explicito para devolver el valor; a + b; sola es solo una expresion-statement que no retorna nada",
          "distractors": ["Porque las statement lambdas no pueden usar operadores aritmeticos", "Porque falta declarar el tipo de a y b explicitamente", "El codigo si compila sin problemas"],
          "explanation": "Sin return, el bloque no devuelve ningun valor, pero aplicar() esta declarado para devolver int - eso es un error de compilacion, distinto de omitir return en una expression lambda sin llaves (donde el valor se devuelve implicitamente).",
          "conceptId": "statement-lambda",
          "role": "guided",
          "pathOrder": 10,
          "dependsOn": ["lambda-sintaxis"]
        },
        {
          "id": "fund-statementlambda-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "Operacion multiplicar = (a, b) -> {\n    if (a == 0 || b == 0) {\n        return 0;\n    }\n    return a * b;\n};\nSystem.out.println(multiplicar.aplicar(3, 0));",
          "answer": "0",
          "explanation": "Con a=3 y b=0, la condicion a==0 || b==0 es true (b es 0), asi que el bloque retorna 0 tempranamente sin llegar a la multiplicacion.",
          "conceptId": "statement-lambda",
          "role": "solo",
          "pathOrder": 11,
          "dependsOn": ["lambda-sintaxis"]
        },
        {
          "id": "fund-lambda-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una empresa de servicios financieros te pregunta: que ventaja practica tiene usar una lambda en vez de una anonymous class para implementar una interfaz funcional?",
          "answer": "Es mas concisa (sin boilerplate de new Interfaz() { ... }) y evita crear una clase interna completa solo para un metodo",
          "distractors": ["Una lambda es mas rapida en tiempo de ejecucion en cualquier caso", "Una anonymous class no puede implementar interfaces funcionales", "No hay ninguna diferencia real entre ambas"],
          "explanation": "Ambas logran el mismo resultado funcional, pero la lambda es sintaxis compacta pensada especificamente para interfaces de un solo metodo abstracto - reduce ruido visual sin cambiar la semantica basica."
        }
      ]
    }
```

- [ ] **Step 4: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
change `private const val CURRENT_CONTENT_VERSION = "9"` to
`private const val CURRENT_CONTENT_VERSION = "10"`.

- [ ] **Step 5: Validate — JSON validity, no duplicate ids, no case-collisions, no orphaned concepts, no multi-terminal concepts**

Run:
```bash
python3 -c "
import json
from collections import defaultdict
d = json.load(open('app/src/main/assets/content/java-fundamentals.json'))
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
Expected: `OK 13 units, <count> exercises` (13 = 11 existing + 2 new),
`case-collisions: []`, `concepts with zero terminal roles: []`,
`concepts with more than one terminal role: {}`, `dangling dependsOn refs: []`.

- [ ] **Step 6: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/assets/content/java-fundamentals.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: retrofit Que es Java?/Tipos-variables-main + add interfaces/lambda units to Fundamentos de Java (sub-cycle 3)"
```

---

## After the task: manual on-device QA

Install a clean/in-place build and manually verify on-device:

1. Ruta shows Fundamentos de Java with 13 units total, in order, and
   `fund-what-is-java` / `fund-types-and-main` still show "Completada"
   (retrofit must not reset Luis's existing progress on them).
2. Replay `fund-what-is-java` and `fund-types-and-main` from Ruta: confirm
   the answerable-exercise count drops by exactly the number of new
   `worked_example` intros added to each (3 for `fund-what-is-java`: wait,
   only 2 intros were added there — `fund-compilacion-intro` and
   `fund-jdkjvm-intro` — so 7+4-2 = 9 answerable; for `fund-types-and-main`,
   1 intro added, so 7+1-1 = 7 answerable).
3. Play the 2 new units directly from Ruta: confirm `guided`/`solo`
   exercises grade correctly, confirm `predict_output` answers grade
   correctly (e.g. `fund-default-solo`, `fund-lambdasintaxis-solo`,
   `fund-statementlambda-solo`).
4. Confirm the section's mandatory checkpoint still triggers correctly
   after all 13 units are complete. **This closes Fundamentos de Java** —
   after this, the next content cycle per the retrofit ADR's order is
   Generics y Colecciones.
