# Fase 2.3 — Manejo de Excepciones (Content Scaling) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the next section in the content roadmap — Manejo de
Excepciones — as real, playable content. Section #4 of 12 in the mapping
already frozen in `docs/superpowers/specs/2026-07-24-fase2-3-content-scaling-design.md`.

**Architecture:** No engine or UI changes — the "add a section = one JSON
pack + register it" architecture was already verified by code inspection
in the 2026-07-24 design spec and proven in practice by the Generics y
Colecciones cycle. This plan is content-only: one new JSON asset, one
registration edit, one content-version bump.

**Tech Stack:** kotlinx.serialization JSON content packs (no Kotlin/Compose
code changes in this plan).

**Design doc:** `docs/superpowers/specs/2026-07-24-fase2-3-content-scaling-design.md`
(section mapping, decided once, covers this cycle too — no new spec
needed for a repeat content-curation cycle, per that doc's own "Explicitamente
fuera de alcance" section).

## Global Constraints

- Content prompts/explanations follow the existing style already in
  `java-fundamentals.json`/`streams.json`/`generics-collections.json`: no
  accent marks, no inverted `¿`/`¡` (e.g. `"Que imprime este codigo?"`, not
  `"¿Qué imprime este código?"`).
- Every unit mixes **three flavors** of question (design spec's explicit
  guidance): exam/syntax (`fill_blank`, `predict_output`), code
  classification (`mcq` over a snippet or concept), and interview/judgment
  (`mcq` framed as "why/when/what problem does this solve" — not syntax
  recall). Each of the 4 units below has at least one exercise of each
  flavor.
- `ContentSeeder.CURRENT_CONTENT_VERSION` **must** be bumped (from `"4"` to
  `"5"`) for the new section to actually seed on devices that already have
  the app installed — the seeder is a no-op if the stored version already
  matches (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`).
  Reseeding wipes and reloads only `sections`/`units`/`exercises` — it does
  not touch `review_state`/`unit_progress`/`checkpoint_attempts`, so
  existing progress is preserved as long as no existing exercise/unit/section
  id is renamed or removed. This plan only adds new ids — no existing id
  changes.
- No Room schema change and no migration — content packs are plain JSON
  assets loaded at runtime.
- Section id for the new pack: `java-exception-handling` (matches the
  `java-<topic>` convention: `java-fundamentals`, `java-generics-collections`,
  `java-streams`). `orderIndex: 4` (next after Streams at `orderIndex: 3`,
  per the roadmap table). Unit id prefix: `excep-`. `certObjective` for all
  its units: `exception-handling` (matches the `<topic>-<topic>`/kebab-case
  slug convention used by `language-basics`, `generics-collections`,
  `streams-lambdas`).
- `examVersion` for the new pack: `"core"` (matches the other 3 packs —
  exception handling is core 1Z0-830 material, not "extra moderno").
- Scope, per the roadmap table: covers the book's chapter 5 content
  **excluding Assertions and Localization/Resource Bundles** — those were
  explicitly flagged as low exam value and out of scope for this section.

---

### Task 1: Author and register the Manejo de Excepciones content pack

**Files:**
- Create: `app/src/main/assets/content/exception-handling.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: nothing new — `ContentLoader`/`ContentSeeder` already parse any
  JSON pack matching the existing `Exercise`/`LearningUnit`/`Section` shape
  generically.
- Produces: nothing consumed by later tasks — this plan has only one task.

- [ ] **Step 1: Write the content pack**

Create `app/src/main/assets/content/exception-handling.json`:

```json
{
  "sectionId": "java-exception-handling",
  "name": "Manejo de Excepciones",
  "orderIndex": 4,
  "examVersion": "core",
  "units": [
    {
      "unitId": "excep-jerarquia",
      "name": "Jerarquia de excepciones",
      "certObjective": "exception-handling",
      "orderIndex": 1,
      "exercises": [
        {
          "id": "excep-jerarquia-01",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Cual es la superclase de todas las excepciones y errores en Java?",
          "answer": "Throwable",
          "distractors": ["Exception", "RuntimeException", "Error"],
          "explanation": "Throwable es la raiz de la jerarquia; Exception y Error son sus dos subclases directas."
        },
        {
          "id": "excep-jerarquia-02",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que caracteriza a una excepcion unchecked (RuntimeException)?",
          "answer": "El compilador no obliga a declararla ni a capturarla",
          "distractors": ["Siempre representa un error critico del sistema", "No puede ser capturada con catch", "Solo puede lanzarse desde metodos static"],
          "explanation": "Las unchecked extienden RuntimeException y el compilador no exige manejo explicito, a diferencia de las checked."
        },
        {
          "id": "excep-jerarquia-03",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Declara una clase de excepcion checked que extiende Exception:",
          "code": "public class SaldoInsuficienteException extends _____ {\n}",
          "answer": "Exception",
          "distractors": ["RuntimeException", "Throwable", "Error"],
          "explanation": "Extender Exception (no RuntimeException) hace que la excepcion sea checked, y el compilador exige declararla o capturarla."
        },
        {
          "id": "excep-jerarquia-04",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "try {\n    Object o = \"texto\";\n    Integer i = (Integer) o;\n} catch (RuntimeException e) {\n    System.out.println(\"capturada: \" + e.getClass().getSimpleName());\n}",
          "answer": "capturada: ClassCastException",
          "explanation": "Castear un String a Integer lanza ClassCastException, que es unchecked (extiende RuntimeException), asi que el catch(RuntimeException e) la captura."
        },
        {
          "id": "excep-jerarquia-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que Java distingue entre excepciones checked y unchecked?",
          "answer": "Para forzar el manejo explicito de errores recuperables (checked) sin sobrecargar cada firma con errores de programacion (unchecked)",
          "distractors": ["Porque las unchecked son mas lentas en tiempo de ejecucion", "Porque las checked no pueden envolver una causa", "Porque el compilador elimino las checked en versiones recientes"],
          "explanation": "Checked fuerza a manejar condiciones de las que el codigo puede recuperarse; unchecked evita ensuciar cada firma con errores de programacion que no deberian ocurrir si el codigo es correcto."
        },
        {
          "id": "excep-jerarquia-06",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que representa un OutOfMemoryError?",
          "answer": "Un problema grave del entorno de ejecucion que normalmente no se debe intentar recuperar",
          "distractors": ["Una excepcion checked que se debe declarar en el metodo", "Un error de logica que el desarrollador debe capturar y reintentar", "Una advertencia del compilador, no un error real"],
          "explanation": "Error indica condiciones anormales del JVM/entorno; la practica recomendada es no capturarlo para intentar arreglarlo en tiempo de ejecucion."
        }
      ]
    },
    {
      "unitId": "excep-try-catch-finally",
      "name": "Try-catch-finally y multi-catch",
      "certObjective": "exception-handling",
      "orderIndex": 2,
      "exercises": [
        {
          "id": "excep-trycatch-01",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el catch que atrapa tanto IOException como SQLException en un solo bloque:",
          "code": "try {\n    procesar();\n} catch (_____ e) {\n    manejar(e);\n}",
          "answer": "IOException | SQLException",
          "distractors": ["IOException, SQLException", "IOException & SQLException", "Exception"],
          "explanation": "El multi-catch usa | para separar tipos alternativos en un mismo catch; la variable e queda con un tipo union de ambos."
        },
        {
          "id": "excep-trycatch-02",
          "type": "predict_output",
          "difficulty": 1,
          "prompt": "Que imprime este codigo?",
          "code": "try {\n    System.out.println(\"try\");\n    throw new RuntimeException(\"boom\");\n} catch (RuntimeException e) {\n    System.out.println(\"catch\");\n} finally {\n    System.out.println(\"finally\");\n}",
          "answer": "try\ncatch\nfinally",
          "explanation": "finally siempre se ejecuta despues del catch (o del try si no hubo excepcion), incluso cuando el catch maneja la excepcion sin relanzarla."
        },
        {
          "id": "excep-trycatch-03",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que devuelve la llamada a valor()?",
          "code": "static int valor() {\n    try {\n        return 1;\n    } finally {\n        return 2;\n    }\n}",
          "answer": "2",
          "explanation": "Un return dentro de finally reemplaza cualquier return pendiente del try, aunque esto se considera mala practica."
        },
        {
          "id": "excep-trycatch-04",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que este codigo no compila?",
          "code": "try {\n    riesgo();\n} catch (Exception e) {\n    manejarGenerico(e);\n} catch (IOException e) {\n    manejarIO(e);\n}",
          "answer": "El catch de Exception es mas general y ya capturaria IOException antes de llegar al segundo catch",
          "distractors": ["IOException no puede lanzarse desde riesgo()", "Los catch deben ir en orden alfabetico", "Falta un finally obligatorio despues de los catch"],
          "explanation": "El compilador exige que los catch mas especificos vayan antes que los mas generales, porque un catch inalcanzable es un error de compilacion."
        },
        {
          "id": "excep-trycatch-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cuando conviene usar multi-catch (IOException | SQLException) en vez de dos catch separados?",
          "answer": "Cuando el manejo de ambas excepciones es identico y no necesitas logica distinta por tipo",
          "distractors": ["Siempre, porque es mas rapido en tiempo de ejecucion", "Solo cuando ambas excepciones son unchecked", "Nunca, Java lo desaconseja"],
          "explanation": "Multi-catch evita duplicar codigo cuando el tratamiento es el mismo; si necesitas logica distinta por excepcion, conviene separarlos."
        },
        {
          "id": "excep-trycatch-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que pasa si el bloque try llama a System.exit(0) antes de terminar?",
          "answer": "La JVM termina de inmediato y el bloque finally no se ejecuta",
          "distractors": ["El finally se ejecuta igual porque siempre corre", "Se lanza una excepcion adicional que hay que capturar", "El programa espera a que finally termine antes de salir"],
          "explanation": "System.exit() detiene la JVM inmediatamente; es una de las pocas formas de evitar que finally se ejecute."
        }
      ]
    },
    {
      "unitId": "excep-try-with-resources",
      "name": "Try-with-resources",
      "certObjective": "exception-handling",
      "orderIndex": 3,
      "exercises": [
        {
          "id": "excep-twr-01",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el try-with-resources para cerrar automaticamente el recurso:",
          "code": "try (_____ br = new BufferedReader(new FileReader(\"datos.txt\"))) {\n    return br.readLine();\n}",
          "answer": "BufferedReader",
          "distractors": ["Reader", "Closeable", "File"],
          "explanation": "El tipo declarado debe coincidir con el tipo real del recurso creado; BufferedReader es el tipo mas directo aqui."
        },
        {
          "id": "excep-twr-02",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que interfaz debe implementar una clase para poder usarse en un try-with-resources?",
          "answer": "AutoCloseable",
          "distractors": ["Serializable", "Cloneable", "Runnable"],
          "explanation": "try-with-resources llama automaticamente a close() al final del bloque, y ese metodo viene de AutoCloseable (o su subinterfaz Closeable)."
        },
        {
          "id": "excep-twr-03",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "class Recurso implements AutoCloseable {\n    String nombre;\n    Recurso(String nombre) { this.nombre = nombre; }\n    public void close() { System.out.println(\"cerrando \" + nombre); }\n}\n\ntry (Recurso a = new Recurso(\"A\"); Recurso b = new Recurso(\"B\")) {\n    System.out.println(\"usando recursos\");\n}",
          "answer": "usando recursos\ncerrando B\ncerrando A",
          "explanation": "Los recursos se cierran en orden inverso al que se declararon, asi que B (el ultimo abierto) se cierra antes que A."
        },
        {
          "id": "excep-twr-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que problema practico resuelve try-with-resources frente a cerrar el recurso manualmente en un finally?",
          "answer": "Evita fugas de recursos si el close() manual se olvida o si ocurre una excepcion antes de llegar al finally",
          "distractors": ["Hace que el codigo compile mas rapido", "Permite reutilizar el recurso despues de terminar el bloque", "Elimina la necesidad de manejar excepciones"],
          "explanation": "Cerrar manualmente es propenso a errores humanos y a fugas si se lanza una excepcion antes del cierre; try-with-resources garantiza el cierre automatico."
        },
        {
          "id": "excep-twr-05",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Si tanto el bloque try como el close() automatico lanzan una excepcion, cual se propaga como principal?",
          "answer": "La excepcion del bloque try; la de close() se agrega como suprimida",
          "distractors": ["La de close(), porque se ejecuta despues", "Ambas se lanzan al mismo tiempo y el programa falla sin excepcion clara", "Solo se propaga la que tenga el mensaje mas corto"],
          "explanation": "Java prioriza la excepcion original del try y adjunta la de close() como suppressed exception, accesible via getSuppressed()."
        }
      ]
    },
    {
      "unitId": "excep-personalizadas",
      "name": "Excepciones personalizadas y encadenamiento",
      "certObjective": "exception-handling",
      "orderIndex": 4,
      "exercises": [
        {
          "id": "excep-personalizadas-01",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa el constructor para pasar el mensaje a la clase padre:",
          "code": "public class SaldoInsuficienteException extends RuntimeException {\n    public SaldoInsuficienteException(String mensaje) {\n        _____;\n    }\n}",
          "answer": "super(mensaje)",
          "distractors": ["this(mensaje)", "mensaje = mensaje", "return mensaje"],
          "explanation": "super(mensaje) delega al constructor de la superclase, que guarda el mensaje accesible via getMessage()."
        },
        {
          "id": "excep-personalizadas-02",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el throw para envolver la excepcion original como causa:",
          "code": "catch (SQLException original) {\n    throw new RuntimeException(\"fallo de acceso a datos\", _____);\n}",
          "answer": "original",
          "distractors": ["original.getMessage()", "original.getClass()", "null"],
          "explanation": "Pasar la excepcion original como segundo argumento la encadena como causa, preservando el stack trace original via getCause()."
        },
        {
          "id": "excep-personalizadas-03",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "try {\n    try {\n        throw new IllegalStateException(\"interno\");\n    } catch (IllegalStateException e) {\n        throw new RuntimeException(\"externo\", e);\n    }\n} catch (RuntimeException e) {\n    System.out.println(e.getMessage() + \" / \" + e.getCause().getMessage());\n}",
          "answer": "externo / interno",
          "explanation": "getMessage() devuelve el mensaje de la excepcion externa, y getCause().getMessage() el de la excepcion original encadenada."
        },
        {
          "id": "excep-personalizadas-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cuando conviene crear una excepcion personalizada en vez de usar una generica como RuntimeException?",
          "answer": "Cuando el error representa una condicion especifica del dominio que el codigo llamador necesita distinguir y manejar de forma distinta",
          "distractors": ["Siempre, porque mejora el rendimiento", "Nunca, Java desaconseja crear excepciones propias", "Solo cuando el metodo no puede lanzar checked exceptions"],
          "explanation": "Una excepcion propia le da significado semantico al error y permite que el llamador la capture especificamente, en vez de atrapar una RuntimeException generica que podria ocultar otros bugs."
        },
        {
          "id": "excep-personalizadas-05",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Estas disenando una excepcion para un error de validacion del que el llamador siempre deberia poder recuperarse. Que superclase eliges?",
          "answer": "Exception (checked), para forzar que el llamador maneje explicitamente el caso",
          "distractors": ["Error, porque los errores de validacion son criticos", "RuntimeException (unchecked), para no obligar a manejarla", "Throwable directamente, para maxima flexibilidad"],
          "explanation": "Cuando se espera que el llamador pueda y deba recuperarse del error, una checked exception (extendiendo Exception) refuerza ese contrato en tiempo de compilacion."
        },
        {
          "id": "excep-personalizadas-06",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que es mejor usar throw new RuntimeException(mensaje, original) en vez de solo throw new RuntimeException(mensaje) al relanzar?",
          "answer": "Porque preserva la causa original y su stack trace, facilitando el diagnostico del error real",
          "distractors": ["Porque es mas rapido en tiempo de ejecucion", "Porque evita que el compilador exija un catch", "Porque cambia el tipo de la excepcion a checked"],
          "explanation": "Sin pasar la causa, se pierde el stack trace original y la informacion de por que ocurrio el error real queda oculta."
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
private const val CURRENT_CONTENT_VERSION = "4"
```

to:

```kotlin
private const val CURRENT_CONTENT_VERSION = "5"
```

And change the `packAssetPaths` list:

```kotlin
    private val packAssetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json"
    )
```

to:

```kotlin
    private val packAssetPaths = listOf(
        "content/java-fundamentals.json",
        "content/generics-collections.json",
        "content/streams.json",
        "content/exception-handling.json"
    )
```

(List order here does not affect Ruta's displayed order — that comes from
each pack's own `orderIndex` field — but listing them in roadmap order
keeps the file easy to scan.)

- [ ] **Step 3: Validate the new file is valid JSON**

Run: `python3 -m json.tool app/src/main/assets/content/exception-handling.json > /dev/null && echo VALID`
Expected: `VALID` printed, no errors.

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — this is the regression check that the new
content and the version bump don't break anything (no dedicated content
test exists beyond the generic `ContentPackParsingTest`, which doesn't
load real asset files).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/content/exception-handling.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: add Manejo de Excepciones section (4 units, 23 exercises)"
```

---

## After the task: manual on-device QA

No automated UI/content test covers full playability. Once merged, install
a clean build and manually verify on-device (SM-A505G, same pattern as
prior phases):

1. Ruta shows sections in order: Fundamentos de Java → Genericos y
   Colecciones → Streams y lambdas → Manejo de Excepciones.
2. Play through Manejo de Excepciones: confirm all 4 units are playable,
   confirm each of the 3 question flavors (exam/syntax, code-classification,
   interview) appears somewhere across the section, confirm `predict_output`
   exercises in this new content grade correctly (multi-line expected
   outputs especially — `excep-trycatch-02` and `excep-twr-03` both expect
   multi-line answers).
3. Confirm the section's checkpoint (mandatory as of the checkpoint feature
   merged earlier) appears after finishing all its units, and that it can
   draw cumulative questions from this new section once at least one later
   section exists to check against it (or that this section's own review
   checkpoint works standalone if it's the last one played).
4. Confirm the placement/skip checkpoint correctly offers to skip over
   Manejo de Excepciones (and prior sections) if a later section is added
   in a future cycle and someone jumps ahead.
