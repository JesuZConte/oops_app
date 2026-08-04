# Fundamentos de Java — Sub-ciclo 2 (OOP Núcleo) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the OOP-core objective gap in Fundamentos de Java (inheritance,
polymorphism, abstract classes, special classes, enums — currently absent
everywhere in the app) — the second of 3 planned sub-cycles bringing that
section to full 1Z0-830 coverage. Also retrofits the section's existing
"Estructura de una clase" unit with first-exposure ladders, per
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md`.

**Architecture:** Content-only — one JSON pack edit, one version bump, no
Kotlin/Compose changes. Two kinds of edit happen in the same file:
(a) a **retrofit** of the existing `fund-class-structure` unit — its 7
existing exercises keep their exact `id`/`type`/`prompt`/`answer`/
`distractors`/`explanation` unchanged (so Luis's existing `review_state`
survives), and gain *only* new `conceptId`/`role`/`pathOrder` fields, plus 4
brand-new ladder exercises; (b) **3 new units appended** after
`fund-loops` (orderIndex 8, added in sub-cycle 1).

**Tech Stack:** kotlinx.serialization JSON content packs (no Kotlin/Compose
code changes in this plan).

**Design docs:** `docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`
(objective detail this plan closes) and
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md` (ladders policy,
grandfathering rule).

## Global Constraints

- Content prompts/explanations: no accent marks, no inverted `¿`/`¡`.
- **Ladders policy:** every genuinely new concept gets a full
  `worked_example` (role `intro`) → `guided` → `solo` sequence, sharing one
  `conceptId`, with sequential `pathOrder` across the whole unit. Extra
  practice/flavor exercises that don't introduce a new concept stay plain
  (no `conceptId`/`role`).
- **Grandfathering rule for the retrofit task:** when adding `conceptId`/
  `role`/`pathOrder` to an *existing* exercise, never change its `id`,
  `type`, `prompt`, `code`, `answer`, `distractors`, or `explanation` —
  only add the 3 new fields. This preserves `review_state` exactly, same
  pattern already proven on `streams-14`/`streams-19`/`streams-parsons-02`
  in `app/src/main/assets/content/streams.json`.
- **Grading-safety rule (established the hard way in sub-cycle 1's final
  review):** for `mcq` and `fill_blank` exercises, grading is
  case-insensitive (`answer.trim().equals(userAnswer.trim(), ignoreCase = true)`).
  **No distractor may differ from the `answer` only by capitalization** —
  that grades a wrong answer as correct. Every exercise below was written
  respecting this; if you add or change any exercise, re-check it.
- Every unit mixes exam/syntax, code-classification, and interview flavors.
  Interview-flavor prompts use generic company framing ("una consultora IT
  grande", "una empresa de servicios financieros") — never real brand names.
- Every new unit includes a `summary: {text, code}` field. Do not add a
  `summary` to `fund-class-structure` — it already has one, unchanged.
- `ContentSeeder`'s `CURRENT_CONTENT_VERSION`
  (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`)
  must bump from `"8"` to `"9"`.
- `java-fundamentals.json` is already registered in `ContentPackRegistry.kt`
  — no registry edit needed.
- New unit id prefix: `fund-`. `certObjective` for all 3 new units:
  `language-basics`. `orderIndex` 9-11, continuing after `fund-loops` (8).
- No Room schema change, no migration.

---

### Task 1: Retrofit `fund-class-structure` and append 3 new OOP-core units

**Files:**
- Modify: `app/src/main/assets/content/java-fundamentals.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: nothing new — same generic JSON pack parsing already proven
  across every content cycle, including the ladder fields.
- Produces: nothing consumed by later tasks — this plan has only one task.

- [ ] **Step 1: Retrofit the existing `fund-class-structure` unit**

Find the unit with `"unitId": "fund-class-structure"` in
`app/src/main/assets/content/java-fundamentals.json`. Its `exercises` array
currently has 7 objects, in this order: `fund-class-01`, `fund-class-02`,
`fund-class-03`, `fund-class-04`, `fund-class-05`, `fund-class-06`,
`fund-parsons-01`.

**1a. Add these 3 fields to `fund-class-01`** (do not change anything
else in the object): `"conceptId": "constructor-basico", "role": "practice", "pathOrder": 2`

**1b. Add these 3 fields to `fund-class-02`**: `"conceptId": "constructor-basico", "role": "solo", "pathOrder": 3`

**1c. Add these 3 fields to `fund-class-03`**: `"conceptId": "encapsulacion-getter-setter", "role": "practice", "pathOrder": 6`

**1d. Add these 3 fields to `fund-class-04`**: `"conceptId": "encapsulacion-getter-setter", "role": "practice", "pathOrder": 7`

**1e. `fund-class-05` — leave completely unchanged.** It's a standalone
fact (one public class per file) that doesn't belong to either ladder.

**1f. Add these 3 fields to `fund-class-06`**: `"conceptId": "encapsulacion-getter-setter", "role": "practice", "pathOrder": 8`

**1g. Add these 3 fields to `fund-parsons-01`**: `"conceptId": "encapsulacion-getter-setter", "role": "solo", "pathOrder": 9`

**1h. Insert these 4 new exercises into the same `exercises` array** —
`fund-constructor-intro` and `fund-constructor-guided` go *before*
`fund-class-01` (so pathOrder 0-1 precede the retagged pathOrder 2-3 above);
`fund-encapsulacion-intro` and `fund-encapsulacion-guided` go *before*
`fund-class-03` (so pathOrder 4-5 precede the retagged pathOrder 6-9 above):

```json
{
  "id": "fund-constructor-intro",
  "type": "worked_example",
  "difficulty": 1,
  "prompt": "El constructor inicializa un objeto nuevo al crearlo con new",
  "code": "public class Persona {\n    private String nombre;\n\n    public Persona(String nombre) {\n        this.nombre = nombre;\n    }\n}",
  "answer": "ok",
  "explanation": "El constructor tiene el mismo nombre que la clase, no declara tipo de retorno, y se ejecuta automaticamente al usar new Persona(...).",
  "conceptId": "constructor-basico",
  "role": "intro",
  "pathOrder": 0
},
{
  "id": "fund-constructor-guided",
  "type": "mcq",
  "difficulty": 1,
  "prompt": "Que caracteristica distingue a un constructor de un metodo normal?",
  "answer": "No declara tipo de retorno y tiene el mismo nombre que la clase",
  "distractors": ["Siempre debe ser private", "Debe llamarse init", "No puede recibir parametros"],
  "explanation": "Un constructor se identifica por dos reglas: mismo nombre que la clase, y sin tipo de retorno (ni siquiera void).",
  "conceptId": "constructor-basico",
  "role": "guided",
  "pathOrder": 1
},
{
  "id": "fund-encapsulacion-intro",
  "type": "worked_example",
  "difficulty": 2,
  "prompt": "Encapsulacion: campos privados, acceso controlado via getters y setters",
  "code": "private String nombre;\n\npublic String getNombre() {\n    return nombre;\n}\n\npublic void setNombre(String nombre) {\n    this.nombre = nombre;\n}",
  "answer": "ok",
  "explanation": "El field es private (nadie fuera de la clase lo toca directo); el getter expone el valor de forma controlada y el setter permite modificarlo, pudiendo agregar validacion.",
  "conceptId": "encapsulacion-getter-setter",
  "role": "intro",
  "pathOrder": 4
},
{
  "id": "fund-encapsulacion-guided",
  "type": "mcq",
  "difficulty": 2,
  "prompt": "Por que se declara un field como private en vez de public?",
  "answer": "Para controlar el acceso: el objeto decide como se lee y modifica su propio estado, en vez de exponerlo directamente",
  "distractors": ["Porque los fields public no compilan", "Porque private hace que el codigo corra mas rapido", "No hay ninguna razon real, es solo convencion sin efecto"],
  "explanation": "Encapsular con private mas getters/setters deja la puerta abierta a agregar validacion o logica despues, sin romper el resto del codigo que ya usa esos metodos.",
  "conceptId": "encapsulacion-getter-setter",
  "role": "guided",
  "pathOrder": 5
}
```

After this step, `fund-class-structure`'s `exercises` array has 11 objects
in this order: `fund-constructor-intro`, `fund-constructor-guided`,
`fund-class-01`, `fund-class-02`, `fund-encapsulacion-intro`,
`fund-encapsulacion-guided`, `fund-class-03`, `fund-class-04`,
`fund-class-05`, `fund-class-06`, `fund-parsons-01`.

- [ ] **Step 2: Append the 3 new units to the `units` array**

Add these 3 units as the new last elements of `java-fundamentals.json`'s
`units` array (after `fund-loops`, remembering the comma):

```json
    {
      "unitId": "fund-herencia-polimorfismo",
      "name": "Herencia y polimorfismo",
      "certObjective": "language-basics",
      "orderIndex": 9,
      "summary": {
        "text": "La herencia permite que una clase (subclase) reutilice y extienda el comportamiento de otra (superclase) con extends. super llama al constructor o metodos de la superclase. @Override marca que un metodo redefine uno heredado - el compilador verifica que la firma coincida exactamente. Una clase abstract no se puede instanciar directamente; sirve como base que obliga a sus subclases concretas a implementar sus metodos abstractos.",
        "code": "public abstract class Animal {\n    abstract String sonido();\n}\n\npublic class Perro extends Animal {\n    @Override\n    String sonido() {\n        return \"Guau\";\n    }\n}"
      },
      "exercises": [
        {
          "id": "fund-herencia-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "extends permite que una clase reutilice el comportamiento de otra",
          "code": "public class Animal {\n    String nombre;\n    void moverse() { System.out.println(\"Me muevo\"); }\n}\n\npublic class Perro extends Animal {\n    void ladrar() { System.out.println(\"Guau\"); }\n}",
          "answer": "ok",
          "explanation": "Perro hereda nombre y moverse() de Animal automaticamente, y agrega su propio metodo ladrar(). Java solo permite herencia simple: una clase extends de una sola superclase.",
          "conceptId": "herencia-extends",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-herencia-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cuantas clases puede extender directamente una clase en Java?",
          "answer": "Una sola (herencia simple)",
          "distractors": ["Hasta 3, si son abstractas", "Tantas como interfaces implemente", "No hay limite"],
          "explanation": "Java no soporta herencia multiple de clases (a diferencia de interfaces); extends solo acepta una superclase.",
          "conceptId": "herencia-extends",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-herencia-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "class Animal {\n    void sonido() { System.out.println(\"..\"); }\n}\nclass Gato extends Animal {\n}\npublic class Main {\n    public static void main(String[] args) {\n        Gato g = new Gato();\n        g.sonido();\n    }\n}",
          "answer": "..",
          "explanation": "Gato no redefine sonido(), asi que hereda la implementacion de Animal sin cambios.",
          "conceptId": "herencia-extends",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-super-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "super() llama al constructor de la superclase",
          "code": "class Animal {\n    String nombre;\n    Animal(String nombre) { this.nombre = nombre; }\n}\nclass Perro extends Animal {\n    Perro(String nombre) {\n        super(nombre);\n    }\n}",
          "answer": "ok",
          "explanation": "super(nombre) delega al constructor de Animal para inicializar el campo heredado; si no se llama explicitamente, Java intenta llamar al constructor sin argumentos de la superclase.",
          "conceptId": "super-constructor",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-super-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que pasa si Perro no llama a super(nombre) y Animal no tiene un constructor sin argumentos?",
          "answer": "El codigo no compila: Java exige llamar a un constructor valido de la superclase",
          "distractors": ["nombre queda como null automaticamente", "Se lanza una excepcion en tiempo de ejecucion", "Java genera un constructor vacio automaticamente para Animal"],
          "explanation": "Si la superclase no tiene un constructor sin argumentos, la subclase esta obligada a llamar explicitamente a uno de los constructores existentes con super(...).",
          "conceptId": "super-constructor",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-super-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "class Animal {\n    String nombre;\n    Animal(String nombre) {\n        this.nombre = nombre;\n        System.out.println(\"Animal creado: \" + nombre);\n    }\n}\nclass Perro extends Animal {\n    Perro(String nombre) {\n        super(nombre);\n        System.out.println(\"Perro creado\");\n    }\n}\npublic class Main {\n    public static void main(String[] args) {\n        new Perro(\"Rex\");\n    }\n}",
          "answer": "Animal creado: Rex\nPerro creado",
          "explanation": "super(nombre) ejecuta primero el constructor de Animal (que imprime su linea), y luego continua la ejecucion del constructor de Perro.",
          "conceptId": "super-constructor",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-override-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "@Override redefine un metodo heredado; la firma debe coincidir exactamente",
          "code": "class Animal {\n    String sonido() { return \"...\"; }\n}\nclass Perro extends Animal {\n    @Override\n    String sonido() { return \"Guau\"; }\n}",
          "answer": "ok",
          "explanation": "@Override no es obligatorio pero es buena practica: si la firma no coincide con ningun metodo de la superclase, el compilador marca error en vez de crear silenciosamente un metodo nuevo sin relacion.",
          "conceptId": "polimorfismo-override",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-override-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que hace @Override si el metodo no coincide con ningun metodo de la superclase?",
          "answer": "El compilador marca un error, en vez de compilar un metodo nuevo sin relacion",
          "distractors": ["Nada, @Override es solo un comentario sin efecto en el compilador", "Se ejecuta el metodo de la superclase de todas formas", "Convierte el metodo en abstracto automaticamente"],
          "explanation": "@Override activa una verificacion del compilador: si la firma no coincide exactamente con un metodo heredado, es un error de compilacion - evita bugs por firmas mal escritas.",
          "conceptId": "polimorfismo-override",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-override-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "class Animal {\n    String sonido() { return \"...\"; }\n}\nclass Gato extends Animal {\n    @Override\n    String sonido() { return \"Miau\"; }\n}\npublic class Main {\n    public static void main(String[] args) {\n        Animal a = new Gato();\n        System.out.println(a.sonido());\n    }\n}",
          "answer": "Miau",
          "explanation": "Aunque la variable a es de tipo Animal, el objeto real es un Gato; en tiempo de ejecucion Java llama a la version sobreescrita (polimorfismo) segun el tipo real del objeto, no el tipo declarado.",
          "conceptId": "polimorfismo-override",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-abstract-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Una clase abstract no se puede instanciar directamente",
          "code": "abstract class Animal {\n    abstract String sonido();\n}\nclass Perro extends Animal {\n    @Override\n    String sonido() { return \"Guau\"; }\n}\n// new Animal(); ERROR: no se puede instanciar\nAnimal a = new Perro(); // OK",
          "answer": "ok",
          "explanation": "abstract marca que la clase es incompleta a proposito: sirve de base, y sus subclases concretas deben implementar los metodos abstract que declara.",
          "conceptId": "clases-abstractas",
          "role": "intro",
          "pathOrder": 9,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-abstract-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que debe hacer una subclase concreta (no abstracta) de una clase abstract?",
          "answer": "Implementar todos los metodos abstract heredados que no haya implementado ninguna superclase intermedia",
          "distractors": ["Nada especial, los metodos abstract ya tienen implementacion por defecto", "Declararse tambien como abstract obligatoriamente", "Sobreescribir el constructor de la superclase"],
          "explanation": "Un metodo abstract no tiene cuerpo; cualquier subclase que quiera ser instanciable (no abstracta) esta obligada a proveerle una implementacion.",
          "conceptId": "clases-abstractas",
          "role": "guided",
          "pathOrder": 10,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-abstract-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Declara la clase Animal como abstracta:",
          "code": "public _____ class Animal {\n    abstract String sonido();\n}",
          "answer": "abstract",
          "distractors": ["final", "static", "interface"],
          "explanation": "abstract permite declarar metodos sin cuerpo (abstract String sonido();) y evita que la clase se instancie directamente.",
          "conceptId": "clases-abstractas",
          "role": "solo",
          "pathOrder": 11,
          "dependsOn": ["herencia-extends"]
        },
        {
          "id": "fund-herencia-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una consultora IT grande te pregunta: por que Java no permite herencia multiple de clases?",
          "answer": "Para evitar ambiguedades como el problema del diamante (dos superclases con un metodo del mismo nombre y firma pero implementaciones distintas)",
          "distractors": ["Por una limitacion tecnica de la JVM que nunca se pudo resolver", "Porque ninguna clase necesita heredar de mas de una superclase en la practica", "Java si permite herencia multiple de clases, solo que es poco usada"],
          "explanation": "El problema del diamante hace ambiguo que implementacion heredar cuando dos superclases definen el mismo metodo; Java lo evita permitiendo herencia simple de clases (pero herencia multiple de interfaces, donde default methods tienen reglas explicitas de resolucion de conflicto)."
        }
      ]
    },
    {
      "unitId": "fund-clases-especiales",
      "name": "Clases especiales",
      "certObjective": "language-basics",
      "orderIndex": 10,
      "summary": {
        "text": "Una clase final no puede ser heredada - util para tipos inmutables o utilitarios que no deben extenderse. Una inner class (no estatica) vive dentro de otra clase y tiene acceso a sus miembros de instancia; una nested static class es basicamente una clase normal empaquetada dentro de otra, sin acceso implicito a instancias externas. Una anonymous class define e instancia una subclase o implementacion de interfaz en un solo paso, sin nombre, util para implementaciones de un solo uso.",
        "code": "final class Constantes { }\n\nclass Exterior {\n    class Interna { }          // inner class\n    static class Anidada { }  // nested static class\n}"
      },
      "exercises": [
        {
          "id": "fund-final-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "final en una clase impide que se herede",
          "code": "public final class Constantes {\n    static final double PI = 3.14159;\n}\n// class Otra extends Constantes { } ERROR: no se puede extender",
          "answer": "ok",
          "explanation": "final en una clase evita que cualquier otra clase la extienda con extends; se usa para tipos que deben quedar cerrados a modificacion por herencia, como String en la propia biblioteca estandar de Java.",
          "conceptId": "final-classes",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-final-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que pasa si intentas escribir 'class Otra extends Constantes' cuando Constantes es final?",
          "answer": "El codigo no compila",
          "distractors": ["Compila pero lanza una excepcion al ejecutar", "Compila y funciona normalmente, final solo es una sugerencia", "Solo falla si Constantes tiene metodos abstractos"],
          "explanation": "final en una clase es una restriccion verificada en tiempo de compilacion, no una sugerencia - el compilador rechaza cualquier intento de extenderla.",
          "conceptId": "final-classes",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-final-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que la clase String es final en Java?",
          "answer": "Para garantizar que su comportamiento (inmutabilidad, hashing usado en HashMap, etc.) no pueda ser alterado por una subclase",
          "distractors": ["Porque String no tiene metodos que se puedan sobreescribir", "Es una limitacion tecnica de la JVM sin relacion con el diseno", "Para que ocupe menos memoria en tiempo de ejecucion"],
          "explanation": "Si String pudiera extenderse, una subclase podria romper garantias de las que depende medio el ecosistema Java (inmutabilidad, cache de hashCode, seguridad); final la protege.",
          "conceptId": "final-classes",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-nested-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Una inner class (no static) tiene acceso a los miembros de instancia de su clase externa",
          "code": "class Exterior {\n    private int valor = 10;\n\n    class Interna {\n        void mostrar() {\n            System.out.println(valor);\n        }\n    }\n}\nExterior ext = new Exterior();\nExterior.Interna in = ext.new Interna();",
          "answer": "ok",
          "explanation": "Una inner class necesita una instancia de la clase externa para existir (por eso 'ext.new Interna()'), y por eso puede acceder directamente a sus fields de instancia, incluso privados.",
          "conceptId": "nested-inner-classes",
          "role": "intro",
          "pathOrder": 3
        },
        {
          "id": "fund-nested-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "En que se diferencia una nested static class de una inner class (no static)?",
          "answer": "La nested static class no necesita una instancia de la clase externa y no tiene acceso implicito a sus miembros de instancia",
          "distractors": ["No hay ninguna diferencia real entre ambas", "La nested static class no puede tener metodos propios", "Solo la inner class puede declararse dentro de otra clase"],
          "explanation": "static en una clase anidada la desconecta de las instancias de la clase externa - se comporta casi como una clase de nivel superior, solo que empaquetada dentro de otra por organizacion.",
          "conceptId": "nested-inner-classes",
          "role": "guided",
          "pathOrder": 4
        },
        {
          "id": "fund-nested-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Declara Anidada como una clase anidada estatica dentro de Exterior:",
          "code": "class Exterior {\n    _____ class Anidada {\n    }\n}",
          "answer": "static",
          "distractors": ["final", "abstract", "private"],
          "explanation": "static en una clase anidada la independiza de instancias de la clase externa.",
          "conceptId": "nested-inner-classes",
          "role": "solo",
          "pathOrder": 5
        },
        {
          "id": "fund-anonima-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Una anonymous class define e instancia una implementacion en un solo paso, sin nombre",
          "code": "Runnable tarea = new Runnable() {\n    @Override\n    public void run() {\n        System.out.println(\"Ejecutando\");\n    }\n};\ntarea.run();",
          "answer": "ok",
          "explanation": "new Runnable() { ... } crea, en el mismo lugar, una subclase sin nombre que implementa Runnable y la instancia inmediatamente - util cuando la implementacion se usa una sola vez y no merece su propia clase con nombre.",
          "conceptId": "anonymous-classes",
          "role": "intro",
          "pathOrder": 6,
          "dependsOn": ["nested-inner-classes"]
        },
        {
          "id": "fund-anonima-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cuando conviene usar una anonymous class en vez de una clase con nombre?",
          "answer": "Cuando la implementacion se usa una sola vez y no tiene sentido darle un nombre ni reutilizarla",
          "distractors": ["Siempre, es mas eficiente en tiempo de ejecucion que una clase con nombre", "Nunca, Java la desaconseja desde versiones recientes", "Solo cuando la interfaz tiene mas de un metodo abstracto"],
          "explanation": "Una anonymous class agrega ruido si se reutiliza o crece en complejidad; su valor esta en implementaciones cortas y de un solo uso, en el lugar donde se necesitan.",
          "conceptId": "anonymous-classes",
          "role": "guided",
          "pathOrder": 7,
          "dependsOn": ["nested-inner-classes"]
        },
        {
          "id": "fund-anonima-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "interface Saludo {\n    String mensaje();\n}\nSaludo s = new Saludo() {\n    @Override\n    public String mensaje() {\n        return \"Hola anonima\";\n    }\n};\nSystem.out.println(s.mensaje());",
          "answer": "Hola anonima",
          "explanation": "La anonymous class implementa Saludo en el lugar de la instanciacion; s.mensaje() llama a esa implementacion definida ahi mismo.",
          "conceptId": "anonymous-classes",
          "role": "solo",
          "pathOrder": 8,
          "dependsOn": ["nested-inner-classes"]
        },
        {
          "id": "fund-especiales-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una empresa de servicios financieros te pregunta: que ventaja practica tiene una inner class sobre una clase de nivel superior para modelar, por ejemplo, un iterador de una coleccion propia?",
          "answer": "El iterador puede acceder directamente al estado interno de la coleccion (sus fields privados) sin necesitar getters publicos adicionales",
          "distractors": ["Una inner class compila mas rapido que una clase de nivel superior", "Es la unica forma de implementar una interfaz en Java", "No hay ninguna ventaja practica real"],
          "explanation": "Al vivir dentro de la clase contenedora, la inner class comparte su alcance de acceso a fields privados - patron real usado por implementaciones de Iterator dentro de clases de coleccion."
        }
      ]
    },
    {
      "unitId": "fund-enums",
      "name": "Enumeraciones (enum)",
      "certObjective": "language-basics",
      "orderIndex": 11,
      "summary": {
        "text": "Un enum define un conjunto fijo y conocido de constantes con seguridad de tipos - mejor que usar Strings o ints sueltos para representar opciones limitadas (dias de la semana, estados, etc.). Un enum puede tener campos, constructores y metodos como cualquier clase; sus constructores son siempre privados o package-private (nunca se instancia con new fuera del propio enum). values() devuelve un array con todas las constantes en el orden declarado, y valueOf(String) busca una constante por su nombre exacto.",
        "code": "enum Dia {\n    LUNES, MARTES, MIERCOLES;\n}\n\nenum Planeta {\n    MERCURIO(3.3e23), TIERRA(5.9e24);\n\n    final double masa;\n    Planeta(double masa) { this.masa = masa; }\n}"
      },
      "exercises": [
        {
          "id": "fund-enum-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Un enum define un conjunto fijo de constantes con seguridad de tipos",
          "code": "enum Dia {\n    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;\n}\nDia hoy = Dia.LUNES;",
          "answer": "ok",
          "explanation": "A diferencia de usar Strings o ints sueltos, el compilador garantiza que 'hoy' solo puede tener uno de los 7 valores declarados - no hay forma de asignarle un valor invalido.",
          "conceptId": "enum-basico",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-enum-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que ventaja da un enum frente a usar un int para representar un conjunto fijo de opciones?",
          "answer": "El compilador impide asignar un valor que no sea una de las constantes declaradas",
          "distractors": ["Un enum ocupa menos memoria que un int", "Un enum se puede modificar en tiempo de ejecucion, un int no", "No hay ninguna ventaja real, son equivalentes"],
          "explanation": "Con un int cualquier numero compila, aunque no represente una opcion valida; con un enum solo las constantes declaradas son valores posibles - seguridad de tipos.",
          "conceptId": "enum-basico",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-enum-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Como se compara de forma segura si dos variables enum apuntan a la misma constante?",
          "answer": "Con == (las constantes enum son instancias unicas, es seguro compararlas asi)",
          "distractors": ["Solo con .equals(), == nunca funciona con enums", "Con compareTo() unicamente", "No se pueden comparar directamente, hay que convertir a String primero"],
          "explanation": "Cada constante enum es una unica instancia (singleton) creada una vez por la JVM; == compara identidad de objeto, que es seguro y es la forma idiomatica de comparar enums (a diferencia de String, donde == es riesgoso).",
          "conceptId": "enum-basico",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-enum-values",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que devuelve Dia.values()?",
          "answer": "Un array con todas las constantes del enum, en el orden en que fueron declaradas",
          "distractors": ["Un List con las constantes en orden alfabetico", "Un Set sin orden garantizado", "Un solo valor: la primera constante declarada"],
          "explanation": "values() es un metodo generado automaticamente por el compilador para todo enum; devuelve un array ordenado segun la declaracion.",
          "conceptId": "enum-metodos",
          "role": "guided",
          "pathOrder": 3,
          "dependsOn": ["enum-basico"]
        },
        {
          "id": "fund-enum-valueof",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "enum Dia { LUNES, MARTES, MIERCOLES; }\nDia d = Dia.valueOf(\"MARTES\");\nSystem.out.println(d);",
          "answer": "MARTES",
          "explanation": "valueOf(String) busca la constante cuyo nombre coincide exactamente (sensible a mayusculas) y la devuelve; System.out.println(d) imprime el nombre de la constante.",
          "conceptId": "enum-metodos",
          "role": "solo",
          "pathOrder": 4,
          "dependsOn": ["enum-basico"]
        },
        {
          "id": "fund-enum-constructor-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Un enum puede tener campos y un constructor, como cualquier clase",
          "code": "enum Planeta {\n    MERCURIO(3.3e23), TIERRA(5.9e24);\n\n    final double masa;\n\n    Planeta(double masa) {\n        this.masa = masa;\n    }\n}\ndouble m = Planeta.TIERRA.masa;",
          "answer": "ok",
          "explanation": "Cada constante llama al constructor con los argumentos indicados entre parentesis, en el momento en que el enum se carga; masa queda fijo para cada constante.",
          "conceptId": "enum-constructor",
          "role": "intro",
          "pathOrder": 5,
          "dependsOn": ["enum-basico"]
        },
        {
          "id": "fund-enum-constructor-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que modificador de acceso puede tener el constructor de un enum?",
          "answer": "Private (o package-private, sin modificador) - nunca public ni protected",
          "distractors": ["Cualquiera, incluido public", "Debe ser siempre static", "Los enums no pueden tener constructor"],
          "explanation": "Las constantes de un enum son las unicas instancias posibles; permitir un constructor public/protected implicaria poder crear instancias adicionales con new desde afuera, lo cual el lenguaje prohibe explicitamente.",
          "conceptId": "enum-constructor",
          "role": "guided",
          "pathOrder": 6,
          "dependsOn": ["enum-basico"]
        },
        {
          "id": "fund-enum-constructor-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa la asignacion del campo masa dentro del constructor:",
          "code": "enum Planeta {\n    MERCURIO(3.3e23), TIERRA(5.9e24);\n\n    final double masa;\n\n    Planeta(double masa) {\n        this._____ = masa;\n    }\n}",
          "answer": "masa",
          "distractors": ["Planeta", "this", "valor"],
          "explanation": "this.masa = masa asigna el argumento del constructor al campo de instancia masa, igual que en cualquier clase normal.",
          "conceptId": "enum-constructor",
          "role": "solo",
          "pathOrder": 7,
          "dependsOn": ["enum-basico"]
        },
        {
          "id": "fund-enum-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una consultora IT grande te pregunta: por que conviene modelar un conjunto fijo de estados (por ejemplo, ACTIVO/INACTIVO/SUSPENDIDO) con un enum en vez de constantes String sueltas?",
          "answer": "El compilador rechaza valores invalidos en tiempo de compilacion, en vez de descubrir un typo (como ACTIBO) recien en produccion",
          "distractors": ["Un enum consume menos memoria que un String en cualquier caso", "No hay diferencia real, es solo una cuestion de estilo", "Un enum se serializa mas rapido que un String"],
          "explanation": "Con Strings sueltas, un typo como ACTIBO compila igual y falla silenciosamente en tiempo de ejecucion; con un enum, ese mismo error es un error de compilacion inmediato."
        }
      ]
    }
```

- [ ] **Step 3: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
change `private const val CURRENT_CONTENT_VERSION = "8"` to
`private const val CURRENT_CONTENT_VERSION = "9"`.

- [ ] **Step 4: Validate the edited file is valid JSON**

Run: `python3 -m json.tool app/src/main/assets/content/java-fundamentals.json > /dev/null && echo VALID`

- [ ] **Step 5: Validate structure — no duplicate ids, expected counts, ladder integrity, no case-collisions**

Run:
```bash
python3 -c "
import json
d = json.load(open('app/src/main/assets/content/java-fundamentals.json'))
unit_ids = [u['unitId'] for u in d['units']]
ex_ids = [e['id'] for u in d['units'] for e in u['exercises']]
assert len(unit_ids) == len(set(unit_ids)), 'duplicate unit id'
assert len(ex_ids) == len(set(ex_ids)), 'duplicate exercise id'
print('OK', len(unit_ids), 'units,', len(ex_ids), 'exercises')

# case-collision check across the whole file
bad = []
for u in d['units']:
    for e in u['exercises']:
        if e.get('type') in ('mcq', 'fill_blank') and 'distractors' in e:
            ans = e['answer'].strip().lower()
            for dist in e['distractors']:
                if dist.strip().lower() == ans:
                    bad.append((e['id'], dist))
print('case-collisions:', bad)

# ladder well-formedness: every conceptId used by an intro/guided must eventually have a solo or practice
from collections import defaultdict
roles_by_concept = defaultdict(set)
for u in d['units']:
    for e in u['exercises']:
        if e.get('conceptId'):
            roles_by_concept[e['conceptId']].add(e.get('role'))
no_terminal = [c for c, roles in roles_by_concept.items() if not (roles & {'solo', 'practice'})]
print('concepts with no solo/practice (would never be born):', no_terminal)
"
```
Expected: `OK 11 units, 98 exercises` (11 = 8 existing + 3 new; 98 = 62
existing + 4 new retrofit exercises + 32 across the 3 new units),
`case-collisions: []`, and
`concepts with no solo/practice (would never be born): []`.

- [ ] **Step 6: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/assets/content/java-fundamentals.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: retrofit Estructura de una clase + add OOP-core units to Fundamentos de Java (sub-cycle 2)"
```

---

## After the task: manual on-device QA

Install a clean/in-place build and manually verify on-device:

1. Ruta shows Fundamentos de Java with 11 units total, in order, and
   `fund-class-structure` ("Estructura de una clase") still shows
   "Completada" (its retrofit must not reset Luis's existing progress on
   it).
2. From Home, tap "Estudiar hoy" (not unit-play, which strips
   `worked_example` cards) at least twice across different days/sessions if
   possible: since `fund-class-structure` is already fully answered, its
   retrofitted concepts (`constructor-basico`, `encapsulacion-getter-setter`)
   should be immediately "born" (their `solo`/`practice`-role exercises are
   already in `answeredIds`) — confirm the new `fund-constructor-intro` and
   `fund-encapsulacion-intro` worked_example cards do **not** appear for
   Luis (already born), while they would for a fresh install.
3. Play the 3 new units directly from Ruta: confirm `guided`/`solo`
   exercises grade correctly, confirm `predict_output` multi-line answers
   grade correctly (e.g. `fund-super-solo`).
4. Confirm the section's mandatory checkpoint still triggers correctly
   after all 11 units are complete.
