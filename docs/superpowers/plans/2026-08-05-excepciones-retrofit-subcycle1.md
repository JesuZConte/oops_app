# Manejo de Excepciones Retrofit Sub-ciclo 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retrofit "Manejo de Excepciones" (`exception-handling.json`) with
first-exposure ladders — currently the section has zero `conceptId`/`role`/
`pathOrder` anywhere, unlike every other already-shipped section. Per
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md`, this is the
4th of 5 retrofit cycles and must deliver ladders + full 1Z0-830 objective
coverage in the same pass. Per
`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`, this section is
already the best-covered of the 5 — the only real gaps are: (1) the
override rule for checked exceptions (a subclass method cannot declare a
broader/different checked exception than the method it overrides), and
(2) unreachable code after a definitive `throw`/`return` (a compile
error, not just the already-covered catch-ordering rule).

**Architecture:** Every one of the 23 existing exercises is grandfathered
(only `conceptId`/`role`/`pathOrder`/`dependsOn` added — `id`/`type`/
`prompt`/`code`/`answer`/`distractors`/`explanation` stay byte-identical,
preserving `review_state`). 19 new exercises close the two gaps and give
every concept a `worked_example` intro. No Kotlin/Compose code changes
beyond the content seed version bump.

**Tech Stack:** JSON content pack, loaded generically by
`ContentLoader`/`ContentSeeder`.

## Global Constraints

- **Grandfathering rule:** for every existing exercise, only add
  `conceptId`, `role`, `pathOrder`, and (where specified) `dependsOn`.
  Never change `id`, `type`, `prompt`, `code`, `answer`, `distractors`,
  or `explanation` — not even whitespace — for any of the 23 existing
  exercises listed below.
- **Case-collision rule:** for `mcq`/`fill_blank` exercises, no distractor
  may differ from the answer only by capitalization.
- **One-terminal-role rule:** every `conceptId` must have exactly one
  exercise with `role: "solo"` (or `"practice"`).
- **`dependsOn` is same-unit only:** `bornConceptIds` is computed per-unit
  by the app, so a `dependsOn` referencing a concept defined in a
  different unit is never satisfiable. Every `dependsOn` in this plan
  stays within its own unit — do not add any cross-unit `dependsOn`.
- **`pathOrder` must be sequential within each unit**, starting at 0,
  across ALL exercises in that unit (existing + new), no gaps, no
  duplicates.
- Do not modify `app/src/main/assets/content/exception-handling.json`
  outside the two units this task's scope covers — this is an additive
  restructuring of the `exercises` array per unit, not a rewrite of the
  whole file. When editing, prefer targeted text insertion over a JSON
  load+dump of the whole file — a JSON library round-trip reformats
  every array in the file (turns single-line arrays into multi-line)
  even when no values change, which has caused review failures in prior
  content sub-cycles on this project. If you must use a script to
  generate the new content, apply it as a literal text replacement of
  just the affected unit's `exercises` array, not a whole-file
  load+dump.
- Valid JSON only.

---

### Task 1: Retrofit "Jerarquia de excepciones" and "Try-catch-finally y multi-catch"

**Files:**
- Modify: `app/src/main/assets/content/exception-handling.json`

**Interfaces:**
- Consumes: nothing — this task only touches the first two units in the
  file's `units` array.
- Produces: concepts `throwable-hierarchy`, `checked-vs-unchecked`,
  `override-checked-exceptions` (unit `excep-jerarquia`); `multi-catch`,
  `finally-semantics`, `catch-ordering`, `unreachable-code-after-throw`
  (unit `excep-try-catch-finally`). Task 2's units do not depend on any
  of these.

- [ ] **Step 1: Replace the `excep-jerarquia` unit's `exercises` array**

Find the `excep-jerarquia` unit in `exception-handling.json` (it is the
first unit in the `units` array). Replace its entire `exercises` array
(currently 6 exercises, ids `excep-jerarquia-01` through `-06`) with this
exact array (10 exercises: 3 brand-new + the same 6 existing exercises
now carrying `conceptId`/`role`/`pathOrder`/`dependsOn`, byte-identical
otherwise):

```json
      "exercises": [
        {
          "id": "throwable-hierarchy-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Throwable es la raiz de todo lo que se puede lanzar en Java: se divide en Exception y Error",
          "code": "Throwable\n├── Exception       // errores recuperables de la aplicacion\n│    └── RuntimeException  // subclase: unchecked\n└── Error            // problemas graves del entorno (JVM, memoria)",
          "answer": "ok",
          "explanation": "Throwable tiene dos ramas directas: Exception (errores que la aplicacion deberia poder manejar) y Error (problemas del entorno de ejecucion, como quedarse sin memoria, que normalmente no se intentan recuperar).",
          "conceptId": "throwable-hierarchy",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "excep-jerarquia-01",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Cual es la superclase de todas las excepciones y errores en Java?",
          "answer": "Throwable",
          "distractors": ["Exception", "RuntimeException", "Error"],
          "explanation": "Throwable es la raiz de la jerarquia; Exception y Error son sus dos subclases directas.",
          "conceptId": "throwable-hierarchy",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "excep-jerarquia-06",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que representa un OutOfMemoryError?",
          "answer": "Un problema grave del entorno de ejecucion que normalmente no se debe intentar recuperar",
          "distractors": ["Una excepcion checked que se debe declarar en el metodo", "Un error de logica que el desarrollador debe capturar y reintentar", "Una advertencia del compilador, no un error real"],
          "explanation": "Error indica condiciones anormales del JVM/entorno; la practica recomendada es no capturarlo para intentar arreglarlo en tiempo de ejecucion.",
          "conceptId": "throwable-hierarchy",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "checked-vs-unchecked-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Las excepciones checked (extienden Exception) obligan a declararlas o capturarlas; las unchecked (extienden RuntimeException) no lo exigen",
          "code": "class ErrorDeNegocio extends Exception { }        // checked: debe declararse o capturarse\nclass ErrorDeProgramacion extends RuntimeException { } // unchecked: no lo exige el compilador",
          "answer": "ok",
          "explanation": "El compilador solo revisa las checked exceptions: si un metodo puede lanzarlas, debe declararlas con throws o capturarlas con try-catch. Las unchecked (RuntimeException y sus subclases) no tienen esa exigencia, porque tipicamente representan errores de programacion (como un indice invalido) en vez de condiciones externas recuperables.",
          "conceptId": "checked-vs-unchecked",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["throwable-hierarchy"]
        },
        {
          "id": "excep-jerarquia-02",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que caracteriza a una excepcion unchecked (RuntimeException)?",
          "answer": "El compilador no obliga a declararla ni a capturarla",
          "distractors": ["Siempre representa un error critico del sistema", "No puede ser capturada con catch", "Solo puede lanzarse desde metodos static"],
          "explanation": "Las unchecked extienden RuntimeException y el compilador no exige manejo explicito, a diferencia de las checked.",
          "conceptId": "checked-vs-unchecked",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["throwable-hierarchy"]
        },
        {
          "id": "excep-jerarquia-03",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Declara una clase de excepcion checked que extiende Exception:",
          "code": "public class SaldoInsuficienteException extends _____ {\n}",
          "answer": "Exception",
          "distractors": ["RuntimeException", "Throwable", "Error"],
          "explanation": "Extender Exception (no RuntimeException) hace que la excepcion sea checked, y el compilador exige declararla o capturarla.",
          "conceptId": "checked-vs-unchecked",
          "role": "guided",
          "pathOrder": 5,
          "dependsOn": ["throwable-hierarchy"]
        },
        {
          "id": "excep-jerarquia-04",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "try {\n    Object o = \"texto\";\n    Integer i = (Integer) o;\n} catch (RuntimeException e) {\n    System.out.println(\"capturada: \" + e.getClass().getSimpleName());\n}",
          "answer": "capturada: ClassCastException",
          "explanation": "Castear un String a Integer lanza ClassCastException, que es unchecked (extiende RuntimeException), asi que el catch(RuntimeException e) la captura.",
          "conceptId": "checked-vs-unchecked",
          "role": "guided",
          "pathOrder": 6,
          "dependsOn": ["throwable-hierarchy"]
        },
        {
          "id": "excep-jerarquia-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que Java distingue entre excepciones checked y unchecked?",
          "answer": "Para forzar el manejo explicito de errores recuperables (checked) sin sobrecargar cada firma con errores de programacion (unchecked)",
          "distractors": ["Porque las unchecked son mas lentas en tiempo de ejecucion", "Porque las checked no pueden envolver una causa", "Porque el compilador elimino las checked en versiones recientes"],
          "explanation": "Checked fuerza a manejar condiciones de las que el codigo puede recuperarse; unchecked evita ensuciar cada firma con errores de programacion que no deberian ocurrir si el codigo es correcto.",
          "conceptId": "checked-vs-unchecked",
          "role": "solo",
          "pathOrder": 7,
          "dependsOn": ["throwable-hierarchy"]
        },
        {
          "id": "override-checked-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Un metodo que sobreescribe otro no puede declarar throws mas amplio que el metodo original para excepciones checked",
          "code": "class Base {\n    void leer() throws IOException { }\n}\n\nclass Derivada extends Base {\n    void leer() throws FileNotFoundException { } // OK: subclase de IOException\n    // void leer() throws Exception { } // ERROR: mas amplia que IOException\n}",
          "answer": "ok",
          "explanation": "Cuando un metodo override declara excepciones checked, solo puede declarar la misma excepcion, una subclase mas especifica, o ninguna -nunca una checked mas amplia o distinta que la declarada en el metodo original. Esto preserva el contrato: quien llama al metodo por su tipo declarado (Base) sabe exactamente que checked exceptions puede recibir, sin sorpresas al usar una subclase.",
          "conceptId": "override-checked-exceptions",
          "role": "intro",
          "pathOrder": 8,
          "dependsOn": ["checked-vs-unchecked"]
        },
        {
          "id": "override-checked-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Un metodo Base.procesar() declara throws IOException. Cual de estos overrides en una subclase compila?",
          "answer": "void procesar() throws FileNotFoundException",
          "distractors": ["void procesar() throws Exception", "void procesar() throws SQLException", "void procesar() throws Throwable"],
          "explanation": "FileNotFoundException extiende IOException, asi que es una checked exception mas especifica -permitido. Exception y Throwable son mas amplias que IOException -no permitido. SQLException no tiene relacion de herencia con IOException -tampoco permitido.",
          "conceptId": "override-checked-exceptions",
          "role": "guided",
          "pathOrder": 9,
          "dependsOn": ["checked-vs-unchecked"]
        },
        {
          "id": "override-checked-solo",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que este codigo no compila?",
          "code": "class Base {\n    void conectar() throws IOException { }\n}\n\nclass Derivada extends Base {\n    @Override\n    void conectar() throws Exception { }\n}",
          "answer": "Exception es mas amplia que IOException; un override no puede declarar una checked exception mas general que la del metodo original",
          "distractors": ["Los metodos override no pueden usar la anotacion @Override con throws", "Exception no es una clase valida para throws", "Falta declarar IOException tambien en el override"],
          "explanation": "El override reduce el contrato de excepciones (o lo deja igual), nunca lo amplia -declarar throws Exception rompe esa regla porque Exception incluye muchas mas checked exceptions que las que el metodo original prometia.",
          "conceptId": "override-checked-exceptions",
          "role": "solo",
          "pathOrder": 10,
          "dependsOn": ["checked-vs-unchecked"]
        }
      ]
```

- [ ] **Step 2: Replace the `excep-try-catch-finally` unit's `exercises` array**

Find the `excep-try-catch-finally` unit (second unit in `units`). Replace
its entire `exercises` array (currently 6 exercises, ids
`excep-trycatch-01` through `-06`) with this exact array (13 exercises: 7
brand-new + the same 6 existing exercises now carrying
`conceptId`/`role`/`pathOrder`, byte-identical otherwise):

```json
      "exercises": [
        {
          "id": "multi-catch-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "catch (TipoA | TipoB e) atrapa varios tipos de excepcion en un mismo bloque, siempre que ninguno sea subtipo del otro",
          "code": "try {\n    procesar();\n} catch (IOException | SQLException e) {\n    manejar(e);\n}",
          "answer": "ok",
          "explanation": "El multi-catch usa | para listar tipos alternativos en un solo catch, evitando duplicar el mismo manejo en bloques separados. La variable e queda con un tipo union de ambos -no se puede reasignar a un tipo mas especifico dentro del catch, y ninguno de los tipos puede ser subclase del otro (seria redundante).",
          "conceptId": "multi-catch",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "excep-trycatch-01",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el catch que atrapa tanto IOException como SQLException en un solo bloque:",
          "code": "try {\n    procesar();\n} catch (_____ e) {\n    manejar(e);\n}",
          "answer": "IOException | SQLException",
          "distractors": ["IOException, SQLException", "IOException & SQLException", "Exception"],
          "explanation": "El multi-catch usa | para separar tipos alternativos en un mismo catch; la variable e queda con un tipo union de ambos.",
          "conceptId": "multi-catch",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "excep-trycatch-05",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cuando conviene usar multi-catch (IOException | SQLException) en vez de dos catch separados?",
          "answer": "Cuando el manejo de ambas excepciones es identico y no necesitas logica distinta por tipo",
          "distractors": ["Siempre, porque es mas rapido en tiempo de ejecucion", "Solo cuando ambas excepciones son unchecked", "Nunca, Java lo desaconseja"],
          "explanation": "Multi-catch evita duplicar codigo cuando el tratamiento es el mismo; si necesitas logica distinta por excepcion, conviene separarlos.",
          "conceptId": "multi-catch",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "finally-semantics-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "finally se ejecuta siempre despues de try/catch, haya o no excepcion -salvo que la JVM termine antes",
          "code": "try {\n    System.out.println(\"try\");\n} finally {\n    System.out.println(\"finally\"); // siempre se imprime\n}",
          "answer": "ok",
          "explanation": "finally corre incluso si el try termina normalmente, si una excepcion fue capturada, o si una excepcion no capturada se esta propagando -es el lugar para liberar recursos que deben cerrarse pase lo que pase. La unica forma comun de saltarselo es terminar la JVM abruptamente, como con System.exit().",
          "conceptId": "finally-semantics",
          "role": "intro",
          "pathOrder": 3
        },
        {
          "id": "excep-trycatch-02",
          "type": "predict_output",
          "difficulty": 1,
          "prompt": "Que imprime este codigo?",
          "code": "try {\n    System.out.println(\"try\");\n    throw new RuntimeException(\"boom\");\n} catch (RuntimeException e) {\n    System.out.println(\"catch\");\n} finally {\n    System.out.println(\"finally\");\n}",
          "answer": "try\ncatch\nfinally",
          "explanation": "finally siempre se ejecuta despues del catch (o del try si no hubo excepcion), incluso cuando el catch maneja la excepcion sin relanzarla.",
          "conceptId": "finally-semantics",
          "role": "guided",
          "pathOrder": 4
        },
        {
          "id": "excep-trycatch-03",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que devuelve la llamada a valor()?",
          "code": "static int valor() {\n    try {\n        return 1;\n    } finally {\n        return 2;\n    }\n}",
          "answer": "2",
          "explanation": "Un return dentro de finally reemplaza cualquier return pendiente del try, aunque esto se considera mala practica.",
          "conceptId": "finally-semantics",
          "role": "guided",
          "pathOrder": 5
        },
        {
          "id": "excep-trycatch-06",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Que pasa si el bloque try llama a System.exit(0) antes de terminar?",
          "answer": "La JVM termina de inmediato y el bloque finally no se ejecuta",
          "distractors": ["El finally se ejecuta igual porque siempre corre", "Se lanza una excepcion adicional que hay que capturar", "El programa espera a que finally termine antes de salir"],
          "explanation": "System.exit() detiene la JVM inmediatamente; es una de las pocas formas de evitar que finally se ejecute.",
          "conceptId": "finally-semantics",
          "role": "solo",
          "pathOrder": 6
        },
        {
          "id": "catch-ordering-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Los catch mas especificos deben ir antes que los mas generales -un catch inalcanzable es un error de compilacion",
          "code": "try {\n    riesgo();\n} catch (IOException e) {      // especifico primero\n    manejarIO(e);\n} catch (Exception e) {        // general despues: OK\n    manejarGenerico(e);\n}",
          "answer": "ok",
          "explanation": "Si un catch mas general (como Exception) fuera antes que uno mas especifico (como IOException), el general capturaria todo primero y el segundo catch jamas se alcanzaria -Java lo detecta en tiempo de compilacion y lo rechaza, en vez de dejarlo como codigo muerto silencioso.",
          "conceptId": "catch-ordering",
          "role": "intro",
          "pathOrder": 7
        },
        {
          "id": "catch-ordering-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cual de estos ordenes de catch compila sin errores?",
          "answer": "catch (NumberFormatException e) {...} catch (RuntimeException e) {...}",
          "distractors": ["catch (RuntimeException e) {...} catch (NumberFormatException e) {...}", "catch (Exception e) {...} catch (RuntimeException e) {...}", "catch (Throwable e) {...} catch (Exception e) {...}"],
          "explanation": "NumberFormatException es subclase de RuntimeException, asi que debe ir primero (mas especifico); los otros tres ejemplos ponen el tipo mas general antes que uno mas especifico relacionado, lo cual el compilador rechaza como catch inalcanzable.",
          "conceptId": "catch-ordering",
          "role": "guided",
          "pathOrder": 8
        },
        {
          "id": "excep-trycatch-04",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Por que este codigo no compila?",
          "code": "try {\n    riesgo();\n} catch (Exception e) {\n    manejarGenerico(e);\n} catch (IOException e) {\n    manejarIO(e);\n}",
          "answer": "El catch de Exception es mas general y ya capturaria IOException antes de llegar al segundo catch",
          "distractors": ["IOException no puede lanzarse desde riesgo()", "Los catch deben ir en orden alfabetico", "Falta un finally obligatorio despues de los catch"],
          "explanation": "El compilador exige que los catch mas especificos vayan antes que los mas generales, porque un catch inalcanzable es un error de compilacion.",
          "conceptId": "catch-ordering",
          "role": "solo",
          "pathOrder": 9
        },
        {
          "id": "unreachable-code-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "El codigo despues de un throw (o return) incondicional dentro del mismo bloque es inalcanzable, y no compila",
          "code": "void metodo() {\n    throw new RuntimeException(\"error\");\n    System.out.println(\"nunca se ejecuta\"); // ERROR: unreachable statement\n}",
          "answer": "ok",
          "explanation": "Un throw incondicional termina la ejecucion del metodo en ese punto; el compilador detecta que cualquier codigo despues, en el mismo flujo, jamas se ejecutaria y lo marca como error de compilacion (unreachable statement), no solo una advertencia.",
          "conceptId": "unreachable-code-after-throw",
          "role": "intro",
          "pathOrder": 10,
          "dependsOn": ["catch-ordering"]
        },
        {
          "id": "unreachable-code-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Este metodo no compila. Por que?",
          "code": "int calcular(int n) {\n    if (n < 0) {\n        throw new IllegalArgumentException(\"negativo\");\n    } else {\n        return n * 2;\n    }\n    System.out.println(\"listo\");\n}",
          "answer": "La ultima linea es inalcanzable: tanto el if como el else terminan el metodo (throw o return), asi que nunca se llega a ese println",
          "distractors": ["Falta un return final despues del if-else", "IllegalArgumentException debe declararse con throws", "El else no puede usar return dentro de un if-else con throw"],
          "explanation": "Cuando TODAS las ramas de un if-else terminan de forma definitiva (throw o return), cualquier codigo despues del if-else completo es inalcanzable, igual que despues de un throw suelto -el compilador lo rechaza como unreachable statement.",
          "conceptId": "unreachable-code-after-throw",
          "role": "guided",
          "pathOrder": 11,
          "dependsOn": ["catch-ordering"]
        },
        {
          "id": "unreachable-code-solo",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que este codigo no compila?",
          "code": "void procesar() {\n    return;\n    int x = 5;\n}",
          "answer": "return termina el metodo inmediatamente; la declaracion int x = 5 despues es codigo inalcanzable",
          "distractors": ["No se puede declarar una variable local sin usarla", "Falta inicializar x antes del return", "return no puede ser la primera instruccion de un metodo"],
          "explanation": "Igual que con throw, un return incondicional termina el flujo del metodo en ese punto -cualquier instruccion despues, en el mismo bloque, es inalcanzable y produce un error de compilacion, no una simple advertencia.",
          "conceptId": "unreachable-code-after-throw",
          "role": "solo",
          "pathOrder": 12,
          "dependsOn": ["catch-ordering"]
        }
      ]
```

- [ ] **Step 3: Validate JSON syntax**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/exception-handling.json'))"`
Expected: no output, exit code 0.

- [ ] **Step 4: Verify grandfathering (no existing exercise content changed)**

Run this to confirm the 12 existing exercises across these two units kept
their exact `prompt`/`code`/`answer`/`distractors`/`explanation`:

```bash
python3 - <<'PYEOF'
import json
d = json.load(open('app/src/main/assets/content/exception-handling.json'))
by_id = {e['id']: e for u in d['units'] for e in u['exercises']}
expected_answers = {
    "excep-jerarquia-01": "Throwable",
    "excep-jerarquia-02": "El compilador no obliga a declararla ni a capturarla",
    "excep-jerarquia-03": "Exception",
    "excep-jerarquia-04": "capturada: ClassCastException",
    "excep-jerarquia-05": "Para forzar el manejo explicito de errores recuperables (checked) sin sobrecargar cada firma con errores de programacion (unchecked)",
    "excep-jerarquia-06": "Un problema grave del entorno de ejecucion que normalmente no se debe intentar recuperar",
    "excep-trycatch-01": "IOException | SQLException",
    "excep-trycatch-02": "try\ncatch\nfinally",
    "excep-trycatch-03": "2",
    "excep-trycatch-04": "El catch de Exception es mas general y ya capturaria IOException antes de llegar al segundo catch",
    "excep-trycatch-05": "Cuando el manejo de ambas excepciones es identico y no necesitas logica distinta por tipo",
    "excep-trycatch-06": "La JVM termina de inmediato y el bloque finally no se ejecuta",
}
for eid, ans in expected_answers.items():
    assert by_id[eid]['answer'] == ans, f"{eid} answer changed!"
print("All 12 existing exercises verified unchanged.")
PYEOF
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/content/exception-handling.json
git commit -m "content: retrofit Jerarquia de excepciones and Try-catch-finally with ladders, add override-checked-exceptions and unreachable-code-after-throw"
```

---

### Task 2: Retrofit "Try-with-resources" and "Excepciones personalizadas y encadenamiento"

**Files:**
- Modify: `app/src/main/assets/content/exception-handling.json`

**Interfaces:**
- Consumes: nothing — independent of Task 1's units (no shared
  `conceptId`, no cross-unit `dependsOn`).
- Produces: concepts `try-with-resources-basics`,
  `resource-closing-order`, `suppressed-exceptions` (unit
  `excep-try-with-resources`); `custom-exceptions-basics`,
  `exception-chaining` (unit `excep-personalizadas`).

- [ ] **Step 1: Replace the `excep-try-with-resources` unit's `exercises` array**

Find the `excep-try-with-resources` unit (third unit in `units`). Replace
its entire `exercises` array (currently 5 exercises, ids `excep-twr-01`
through `-05`) with this exact array (10 exercises: 5 brand-new + the
same 5 existing exercises now carrying `conceptId`/`role`/`pathOrder`/
`dependsOn`, byte-identical otherwise):

```json
      "exercises": [
        {
          "id": "twr-basics-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "try-with-resources cierra automaticamente cualquier recurso que implemente AutoCloseable, sin necesitar un finally manual",
          "code": "try (BufferedReader lector = new BufferedReader(new FileReader(\"datos.txt\"))) {\n    System.out.println(lector.readLine());\n} // lector.close() se llama automaticamente aqui",
          "answer": "ok",
          "explanation": "Cualquier clase que implemente AutoCloseable (o su subinterfaz Closeable) puede declararse dentro de los parentesis de un try-with-resources; Java garantiza que close() se llama al salir del bloque, exitosamente o por excepcion, sin necesitar un finally escrito a mano.",
          "conceptId": "try-with-resources-basics",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "excep-twr-02",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que interfaz debe implementar una clase para poder usarse en un try-with-resources?",
          "answer": "AutoCloseable",
          "distractors": ["Serializable", "Cloneable", "Runnable"],
          "explanation": "try-with-resources llama automaticamente a close() al final del bloque, y ese metodo viene de AutoCloseable (o su subinterfaz Closeable).",
          "conceptId": "try-with-resources-basics",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "excep-twr-01",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el try-with-resources para cerrar automaticamente el recurso:",
          "code": "try (_____ br = new BufferedReader(new FileReader(\"datos.txt\"))) {\n    return br.readLine();\n}",
          "answer": "BufferedReader",
          "distractors": ["Reader", "Closeable", "File"],
          "explanation": "El tipo declarado debe coincidir con el tipo real del recurso creado; BufferedReader es el tipo mas directo aqui.",
          "conceptId": "try-with-resources-basics",
          "role": "guided",
          "pathOrder": 2
        },
        {
          "id": "excep-twr-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que problema practico resuelve try-with-resources frente a cerrar el recurso manualmente en un finally?",
          "answer": "Evita fugas de recursos si el close() manual se olvida o si ocurre una excepcion antes de llegar al finally",
          "distractors": ["Hace que el codigo compile mas rapido", "Permite reutilizar el recurso despues de terminar el bloque", "Elimina la necesidad de manejar excepciones"],
          "explanation": "Cerrar manualmente es propenso a errores humanos y a fugas si se lanza una excepcion antes del cierre; try-with-resources garantiza el cierre automatico.",
          "conceptId": "try-with-resources-basics",
          "role": "solo",
          "pathOrder": 3
        },
        {
          "id": "closing-order-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Cuando hay varios recursos en un try-with-resources, se cierran en orden inverso al que se declararon",
          "code": "try (RecursoA a = new RecursoA(); RecursoB b = new RecursoB()) {\n    usar(a, b);\n}\n// se cierra primero b, despues a",
          "answer": "ok",
          "explanation": "El orden de cierre es LIFO (ultimo en abrir, primero en cerrar): si declaras a y luego b, b se cierra primero. Esto importa cuando un recurso depende de otro seguir abierto durante su propio close(), como una conexion que depende de un stream subyacente.",
          "conceptId": "resource-closing-order",
          "role": "intro",
          "pathOrder": 4,
          "dependsOn": ["try-with-resources-basics"]
        },
        {
          "id": "closing-order-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Declaras try (RecursoX x = ...; RecursoY y = ...; RecursoZ z = ...) { ... }. En que orden se cierran los recursos?",
          "answer": "Z, luego Y, luego X",
          "distractors": ["X, luego Y, luego Z", "El orden no esta garantizado", "Se cierran todos al mismo tiempo"],
          "explanation": "El cierre es en orden inverso a la declaracion (LIFO): el ultimo recurso declarado (Z) es el primero en cerrarse.",
          "conceptId": "resource-closing-order",
          "role": "guided",
          "pathOrder": 5,
          "dependsOn": ["try-with-resources-basics"]
        },
        {
          "id": "excep-twr-03",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "class Recurso implements AutoCloseable {\n    String nombre;\n    Recurso(String nombre) { this.nombre = nombre; }\n    public void close() { System.out.println(\"cerrando \" + nombre); }\n}\n\ntry (Recurso a = new Recurso(\"A\"); Recurso b = new Recurso(\"B\")) {\n    System.out.println(\"usando recursos\");\n}",
          "answer": "usando recursos\ncerrando B\ncerrando A",
          "explanation": "Los recursos se cierran en orden inverso al que se declararon, asi que B (el ultimo abierto) se cierra antes que A.",
          "conceptId": "resource-closing-order",
          "role": "solo",
          "pathOrder": 6,
          "dependsOn": ["try-with-resources-basics"]
        },
        {
          "id": "suppressed-exceptions-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "Si el bloque try y el close() automatico lanzan una excepcion, la del try se propaga y la de close() queda como suprimida",
          "code": "try {\n    // lanza excepcion A\n} catch (Exception e) {\n    e.getSuppressed(); // aca estaria la excepcion B que lanzo close(), si tambien fallo\n}",
          "answer": "ok",
          "explanation": "Java no descarta la excepcion de close() cuando el try ya fallo -la adjunta como 'suppressed' a la excepcion principal, accesible via getSuppressed(), en vez de reemplazar o perder la excepcion original del try.",
          "conceptId": "suppressed-exceptions",
          "role": "intro",
          "pathOrder": 7,
          "dependsOn": ["try-with-resources-basics"]
        },
        {
          "id": "suppressed-exceptions-guided",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "El bloque try lanza una excepcion, y el close() automatico del recurso tambien lanza otra. Que pasa con la excepcion de close()?",
          "answer": "Se agrega como suppressed a la excepcion del try, no se pierde ni se lanza por separado",
          "distractors": ["Reemplaza a la excepcion del try como la que se propaga", "Se ignora silenciosamente", "Se lanza una tercera excepcion que combina ambas"],
          "explanation": "getSuppressed() en la excepcion del try devuelve un array con cualquier excepcion adicional que haya ocurrido durante el cierre automatico de los recursos, sin descartarla.",
          "conceptId": "suppressed-exceptions",
          "role": "guided",
          "pathOrder": 8,
          "dependsOn": ["try-with-resources-basics"]
        },
        {
          "id": "excep-twr-05",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Si tanto el bloque try como el close() automatico lanzan una excepcion, cual se propaga como principal?",
          "answer": "La excepcion del bloque try; la de close() se agrega como suprimida",
          "distractors": ["La de close(), porque se ejecuta despues", "Ambas se lanzan al mismo tiempo y el programa falla sin excepcion clara", "Solo se propaga la que tenga el mensaje mas corto"],
          "explanation": "Java prioriza la excepcion original del try y adjunta la de close() como suppressed exception, accesible via getSuppressed().",
          "conceptId": "suppressed-exceptions",
          "role": "solo",
          "pathOrder": 9,
          "dependsOn": ["try-with-resources-basics"]
        }
      ]
```

- [ ] **Step 2: Replace the `excep-personalizadas` unit's `exercises` array**

Find the `excep-personalizadas` unit (fourth/last unit in `units`).
Replace its entire `exercises` array (currently 6 exercises, ids
`excep-personalizadas-01` through `-06`) with this exact array (8
exercises: 2 brand-new + the same 6 existing exercises now carrying
`conceptId`/`role`/`pathOrder`, byte-identical otherwise):

```json
      "exercises": [
        {
          "id": "custom-exceptions-basics-intro",
          "type": "worked_example",
          "difficulty": 1,
          "prompt": "Una excepcion personalizada extiende Exception (checked) o RuntimeException (unchecked) y normalmente pasa el mensaje al constructor padre",
          "code": "public class SaldoInsuficienteException extends RuntimeException {\n    public SaldoInsuficienteException(String mensaje) {\n        super(mensaje);\n    }\n}",
          "answer": "ok",
          "explanation": "Crear tu propia excepcion le da significado semantico al error (el llamador puede distinguirla de otras) y te permite agregar datos propios. super(mensaje) delega el mensaje a Throwable, donde queda accesible via getMessage().",
          "conceptId": "custom-exceptions-basics",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "excep-personalizadas-01",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa el constructor para pasar el mensaje a la clase padre:",
          "code": "public class SaldoInsuficienteException extends RuntimeException {\n    public SaldoInsuficienteException(String mensaje) {\n        _____;\n    }\n}",
          "answer": "super(mensaje)",
          "distractors": ["this(mensaje)", "mensaje = mensaje", "return mensaje"],
          "explanation": "super(mensaje) delega al constructor de la superclase, que guarda el mensaje accesible via getMessage().",
          "conceptId": "custom-exceptions-basics",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "excep-personalizadas-04",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Cuando conviene crear una excepcion personalizada en vez de usar una generica como RuntimeException?",
          "answer": "Cuando el error representa una condicion especifica del dominio que el codigo llamador necesita distinguir y manejar de forma distinta",
          "distractors": ["Siempre, porque mejora el rendimiento", "Nunca, Java desaconseja crear excepciones propias", "Solo cuando el metodo no puede lanzar checked exceptions"],
          "explanation": "Una excepcion propia le da significado semantico al error y permite que el llamador la capture especificamente, en vez de atrapar una RuntimeException generica que podria ocultar otros bugs.",
          "conceptId": "custom-exceptions-basics",
          "role": "guided",
          "pathOrder": 2
        },
        {
          "id": "excep-personalizadas-05",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Estas disenando una excepcion para un error de validacion del que el llamador siempre deberia poder recuperarse. Que superclase eliges?",
          "answer": "Exception (checked), para forzar que el llamador maneje explicitamente el caso",
          "distractors": ["Error, porque los errores de validacion son criticos", "RuntimeException (unchecked), para no obligar a manejarla", "Throwable directamente, para maxima flexibilidad"],
          "explanation": "Cuando se espera que el llamador pueda y deba recuperarse del error, una checked exception (extendiendo Exception) refuerza ese contrato en tiempo de compilacion.",
          "conceptId": "custom-exceptions-basics",
          "role": "solo",
          "pathOrder": 3
        },
        {
          "id": "exception-chaining-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Al envolver una excepcion de bajo nivel en una de mas alto nivel, pasala como causa para no perder la traza original",
          "code": "catch (SQLException original) {\n    throw new RuntimeException(\"fallo de acceso a datos\", original);\n}\n// mas tarde: e.getCause() devuelve la excepcion SQLException original",
          "answer": "ok",
          "explanation": "El segundo argumento del constructor de una excepcion es la causa (Throwable). Encadenarla preserva el stack trace original, accesible via getCause() -sin esto, se pierde informacion clave para diagnosticar el error real.",
          "conceptId": "exception-chaining",
          "role": "intro",
          "pathOrder": 4
        },
        {
          "id": "excep-personalizadas-02",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa el throw para envolver la excepcion original como causa:",
          "code": "catch (SQLException original) {\n    throw new RuntimeException(\"fallo de acceso a datos\", _____);\n}",
          "answer": "original",
          "distractors": ["original.getMessage()", "original.getClass()", "null"],
          "explanation": "Pasar la excepcion original como segundo argumento la encadena como causa, preservando el stack trace original via getCause().",
          "conceptId": "exception-chaining",
          "role": "guided",
          "pathOrder": 5
        },
        {
          "id": "excep-personalizadas-03",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "try {\n    try {\n        throw new IllegalStateException(\"interno\");\n    } catch (IllegalStateException e) {\n        throw new RuntimeException(\"externo\", e);\n    }\n} catch (RuntimeException e) {\n    System.out.println(e.getMessage() + \" / \" + e.getCause().getMessage());\n}",
          "answer": "externo / interno",
          "explanation": "getMessage() devuelve el mensaje de la excepcion externa, y getCause().getMessage() el de la excepcion original encadenada.",
          "conceptId": "exception-chaining",
          "role": "guided",
          "pathOrder": 6
        },
        {
          "id": "excep-personalizadas-06",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que es mejor usar throw new RuntimeException(mensaje, original) en vez de solo throw new RuntimeException(mensaje) al relanzar?",
          "answer": "Porque preserva la causa original y su stack trace, facilitando el diagnostico del error real",
          "distractors": ["Porque es mas rapido en tiempo de ejecucion", "Porque evita que el compilador exija un catch", "Porque cambia el tipo de la excepcion a checked"],
          "explanation": "Sin pasar la causa, se pierde el stack trace original y la informacion de por que ocurrio el error real queda oculta.",
          "conceptId": "exception-chaining",
          "role": "solo",
          "pathOrder": 7
        }
      ]
```

- [ ] **Step 3: Validate JSON syntax**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/content/exception-handling.json'))"`
Expected: no output, exit code 0.

- [ ] **Step 4: Verify grandfathering (no existing exercise content changed)**

```bash
python3 - <<'PYEOF'
import json
d = json.load(open('app/src/main/assets/content/exception-handling.json'))
by_id = {e['id']: e for u in d['units'] for e in u['exercises']}
expected_answers = {
    "excep-twr-01": "BufferedReader",
    "excep-twr-02": "AutoCloseable",
    "excep-twr-03": "usando recursos\ncerrando B\ncerrando A",
    "excep-twr-04": "Evita fugas de recursos si el close() manual se olvida o si ocurre una excepcion antes de llegar al finally",
    "excep-twr-05": "La excepcion del bloque try; la de close() se agrega como suprimida",
    "excep-personalizadas-01": "super(mensaje)",
    "excep-personalizadas-02": "original",
    "excep-personalizadas-03": "externo / interno",
    "excep-personalizadas-04": "Cuando el error representa una condicion especifica del dominio que el codigo llamador necesita distinguir y manejar de forma distinta",
    "excep-personalizadas-05": "Exception (checked), para forzar que el llamador maneje explicitamente el caso",
    "excep-personalizadas-06": "Porque preserva la causa original y su stack trace, facilitando el diagnostico del error real",
}
for eid, ans in expected_answers.items():
    assert by_id[eid]['answer'] == ans, f"{eid} answer changed!"
print("All 11 existing exercises verified unchanged.")
PYEOF
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/content/exception-handling.json
git commit -m "content: retrofit Try-with-resources and Excepciones personalizadas with ladders, add resource-closing-order and suppressed-exceptions detail"
```

---

### Task 3: Validate the whole corpus and bump the content seed version

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: `app/src/main/assets/content/exception-handling.json` (as
  modified by Tasks 1-2) plus every other content pack file already
  registered in `ContentPackRegistry`.
- Produces: `CURRENT_CONTENT_VERSION = "14"`.

- [ ] **Step 1: Run the reachability/consistency validation script**

```bash
python3 - <<'PYEOF'
import json
from collections import defaultdict

d = json.load(open('app/src/main/assets/content/exception-handling.json'))

unit_ids = [u['unitId'] for u in d['units']]
ex_ids = [e['id'] for u in d['units'] for e in u['exercises']]
assert len(unit_ids) == len(set(unit_ids)), 'duplicate unitId'
assert len(ex_ids) == len(set(ex_ids)), 'duplicate exercise id'
assert len(ex_ids) == 42, f'expected 42 exercises, got {len(ex_ids)}'
print('OK:', len(unit_ids), 'units,', len(ex_ids), 'exercises')

bad = []
for u in d['units']:
    for e in u['exercises']:
        if e.get('type') in ('mcq', 'fill_blank') and 'distractors' in e:
            ans = e['answer'].strip().lower()
            for dist in e['distractors']:
                if dist.strip().lower() == ans:
                    bad.append((e['id'], dist))
print('case-collisions (must be empty):', bad)
assert not bad

roles = defaultdict(set)
terms = defaultdict(int)
concept_unit = {}
for u in d['units']:
    for e in u['exercises']:
        if e.get('conceptId'):
            roles[e['conceptId']].add(e.get('role'))
            concept_unit[e['conceptId']] = u['unitId']
            if e.get('role') in ('solo', 'practice'):
                terms[e['conceptId']] += 1

zero_terminal = [c for c, r in roles.items() if not (r & {'solo', 'practice'})]
multi_terminal = {c: n for c, n in terms.items() if n > 1}
print('zero-terminal concepts (must be empty):', zero_terminal)
print('multi-terminal concepts (must be empty):', multi_terminal)
assert not zero_terminal
assert not multi_terminal

all_concepts = set(roles.keys())
dangling = []
cross_unit = []
for u in d['units']:
    for e in u['exercises']:
        for dep in e.get('dependsOn', []):
            if dep not in all_concepts:
                dangling.append((e['id'], dep))
            elif concept_unit[dep] != u['unitId']:
                cross_unit.append((e['id'], dep))
print('dangling dependsOn (must be empty):', dangling)
print('cross-unit dependsOn (must be empty):', cross_unit)
assert not dangling
assert not cross_unit

print('ALL CHECKS PASSED')
PYEOF
```

Expected: `ALL CHECKS PASSED` printed, `42` exercises (23 grandfathered +
19 new), all listed sets empty. If any assertion fails, stop and report
— do not proceed to Step 2 with a failing corpus.

- [ ] **Step 2: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
find:

```kotlin
private const val CURRENT_CONTENT_VERSION = "13"
```

Replace with:

```kotlin
private const val CURRENT_CONTENT_VERSION = "14"
```

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests pass unchanged.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "chore: bump content seed version so the Manejo de Excepciones ladder retrofit reaches existing installs"
```

---

## After the task: manual on-device QA

Install a clean/in-place build and manually verify on-device:

1. Reopen Ruta, navigate to "Manejo de Excepciones": confirm all 4 units
   still show "Completada" (grandfathering preserved `review_state`) if
   they were already played, or unlocked/locked correctly per the
   normal section-unlock rules if not yet played.
2. Replay "Jerarquia de excepciones": confirm the exercise count matches
   the grandfathering prediction (10 total minus however many
   `worked_example` intros were already-born concepts for this install —
   3 new intros expected excluded if the unit was already fully
   completed before this retrofit), and that a retrofitted exercise
   (e.g. `excep-jerarquia-01`) still grades correctly.
3. Play through at least one of the brand-new concepts end-to-end (e.g.
   `override-checked-exceptions` in "Jerarquia de excepciones", or
   `unreachable-code-after-throw` in "Try-catch-finally y multi-catch")
   to confirm the new ladder unlocks and grades correctly.
4. Confirm no crash and that streak/XP/prior progress in other sections
   (Fundamentos, Genericos y Colecciones, Streams y lambdas) is
   unaffected.
5. If "Manejo de Excepciones" had an already-approved checkpoint from a
   prior QA session, confirm it still shows satisfied despite the new
   units' exercise-count growth (exercises unlocking within an already
   partially-played unit is not the same case the checkpoint-permanence
   fix targets, but verify it isn't broken either).
