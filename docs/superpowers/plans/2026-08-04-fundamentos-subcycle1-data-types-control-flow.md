# Fundamentos de Java — Sub-ciclo 1 (Tipos de Datos + Control de Flujo) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the "Handling Date, Time, Text, Numeric and Boolean Values"
and "Controlling Program Flow" objective gaps in the Fundamentos de Java
section — the first of 3 planned sub-cycles that bring that section to full
1Z0-830 coverage (see `docs/adrs/2026-08-04-1z0-830-roadmap-correction.md`).
Adds 5 new units with first-exposure ladders, per
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md`.

**Architecture:** No engine or UI changes — content-only, same
"add units = edit one JSON pack" architecture already proven across every
Fase 2.3 cycle. This sub-cycle only **adds** units to the existing
`java-fundamentals.json` pack; it does not touch or retrofit the section's
3 existing units (Que es Java?, Estructura de una clase, Tipos/variables/main)
— those are retrofitted in sub-cycles 2 and 3 per the roadmap ADR's split.

**Tech Stack:** kotlinx.serialization JSON content packs (no Kotlin/Compose
code changes in this plan).

**Design docs:**
`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md` (objective detail and
gap analysis this plan closes) and
`docs/adrs/2026-08-04-ladders-content-retrofit-policy.md` (ladders policy
and execution order).

## Global Constraints

- Content prompts/explanations follow the existing style: no accent marks,
  no inverted `¿`/`¡` (e.g. `"Que imprime este codigo?"`, not `"¿Qué
  imprime este código?"`).
- **Ladders policy:** every genuinely new concept gets a full
  `worked_example` (role `intro`) → `guided` → `solo` sequence, sharing one
  `conceptId`, with sequential `pathOrder` across the whole unit (continuing
  the running count, not resetting per concept). Extra practice/flavor
  exercises that don't introduce a new concept stay plain (no `conceptId`/
  `role`) — same convention already used in `streams-collectors`
  (`app/src/main/assets/content/streams.json`).
- Every unit mixes exam/syntax, code-classification, and interview/judgment
  flavors (Fase 2.3 guidance). Interview-flavor prompts use **generic**
  company framing ("una consultora IT grande", "una empresa de servicios
  financieros") — never real brand names, per the retrofit ADR.
- Every unit includes a `summary: {text, code}` field (Unit Summaries / Tips
  feature convention — every unit shipped since 2026-07-31 has one).
- `ContentSeeder`'s `CURRENT_CONTENT_VERSION`
  (`app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt:12`)
  **must** bump from `"7"` to `"8"` for the new units to actually seed on
  devices that already have the app installed. Reseeding wipes and reloads
  only `sections`/`units`/`exercises` — `review_state`/`unit_progress`/
  `checkpoint_attempts` are untouched, so existing progress survives as long
  as no existing id is renamed or removed. This plan only adds new ids.
- `java-fundamentals.json` is already registered in
  `app/src/main/java/com/zconte/oopsapp/data/content/ContentPackRegistry.kt`
  — no registry edit needed, only the version bump.
- New unit id prefix: `fund-`. `certObjective` for all 5 new units:
  `language-basics` (matches the section's 3 existing units). `orderIndex`
  4-8, continuing after the existing units' 1-3.
- No Room schema change, no migration.

---

### Task 1: Author and append the 5 new units to Fundamentos de Java

**Files:**
- Modify: `app/src/main/assets/content/java-fundamentals.json`
- Modify: `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`

**Interfaces:**
- Consumes: nothing new — `ContentLoader`/`ContentSeeder` already parse any
  JSON pack matching the existing `Exercise`/`LearningUnit`/`Section` shape
  generically, including the ladder fields (`conceptId`/`role`/`pathOrder`/
  `dependsOn`) proven by the `streams-collectors` pilot.
- Produces: nothing consumed by later tasks — this plan has only one task.

- [ ] **Step 1: Append the 5 new units to the `units` array**

In `app/src/main/assets/content/java-fundamentals.json`, the `units` array
currently ends after `fund-types-and-main`. Add these 5 units immediately
after it (as the new last elements of the array, so the file's outer
`{"sectionId": ..., "units": [...]}` structure is preserved — remember to
add a comma after the existing last unit's closing `}`):

```json
    {
      "unitId": "fund-primitivos-wrappers",
      "name": "Tipos primitivos y wrappers",
      "certObjective": "language-basics",
      "orderIndex": 4,
      "summary": {
        "text": "Java tiene 8 tipos primitivos (int, double, boolean, char, etc.) que guardan valores directamente y son mas eficientes. Cada uno tiene una clase wrapper equivalente (Integer, Double, Boolean, Character) que lo envuelve en un objeto - necesario para usarlos en colecciones genericas como List<Integer>. El autoboxing convierte un primitivo a su wrapper automaticamente, y el unboxing hace lo inverso. El casting convierte entre tipos numericos: widening (chico a grande) es automatico, narrowing (grande a chico) necesita cast explicito y puede perder datos.",
        "code": "int edad = 30;\nInteger edadObj = edad;      // autoboxing\nint edadPrimitivo = edadObj; // unboxing\n\ndouble precio = 9.99;\nint truncado = (int) precio; // narrowing, pierde el decimal"
      },
      "exercises": [
        {
          "id": "fund-primwrap-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Cada tipo primitivo tiene su clase wrapper equivalente",
          "code": "int edad = 30;          // primitivo, vive en el stack\nInteger edadObj = 30;   // wrapper, es un objeto en el heap",
          "answer": "ok",
          "explanation": "Los primitivos guardan el valor directamente; los wrappers (Integer, Double, Boolean, Character, etc.) son objetos que lo envuelven, necesarios donde Java exige un objeto (como en List<Integer>).",
          "conceptId": "primitivos-wrappers",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-primwrap-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Cual es la clase wrapper del primitivo boolean?",
          "answer": "Boolean",
          "distractors": ["Bool", "boolean", "Bit"],
          "explanation": "Cada primitivo tiene un wrapper con el mismo nombre en mayuscula, salvo int->Integer y char->Character.",
          "conceptId": "primitivos-wrappers",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-primwrap-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Declara una variable wrapper para guardar un double:",
          "code": "_____ precio = 9.99;",
          "answer": "Double",
          "distractors": ["double", "Float", "Number"],
          "explanation": "Double (con mayuscula inicial) es la clase wrapper de double.",
          "conceptId": "primitivos-wrappers",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-autobox-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Autoboxing convierte un primitivo a su wrapper automaticamente",
          "code": "List<Integer> numeros = new ArrayList<>();\nnumeros.add(5);                 // autoboxing: int -> Integer\nint primero = numeros.get(0);   // unboxing: Integer -> int",
          "answer": "ok",
          "explanation": "El compilador inserta la conversion automaticamente; por eso una List<Integer> puede recibir un int directamente con add().",
          "conceptId": "autoboxing",
          "role": "intro",
          "pathOrder": 3
        },
        {
          "id": "fund-autobox-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que ocurre al ejecutar 'Integer x = 5;'?",
          "answer": "Autoboxing: el compilador convierte el int 5 a un objeto Integer",
          "distractors": ["Error de compilacion, no se puede asignar int a Integer", "Se convierte 5 a String primero", "Se crea un array de un elemento"],
          "explanation": "Java autoboxea automaticamente el primitivo al wrapper equivalente cuando el contexto lo requiere.",
          "conceptId": "autoboxing",
          "role": "guided",
          "pathOrder": 4
        },
        {
          "id": "fund-autobox-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "Integer a = 127;\nInteger b = 127;\nSystem.out.println(a == b);",
          "answer": "true",
          "explanation": "Java cachea objetos Integer entre -128 y 127; a y b apuntan al mismo objeto cacheado, asi que == da true. Fuera de ese rango el resultado con == no esta garantizado.",
          "conceptId": "autoboxing",
          "role": "solo",
          "pathOrder": 5
        },
        {
          "id": "fund-primwrap-casting",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "double d = 9.99;\nint i = (int) d;\nSystem.out.println(i);",
          "answer": "9",
          "explanation": "El cast (int) trunca la parte decimal sin redondear; 9.99 se convierte en 9, no en 10."
        },
        {
          "id": "fund-primwrap-precedencia",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "boolean resultado = 5 > 3 && 2 < 1 || 4 == 4;\nSystem.out.println(resultado);",
          "answer": "true",
          "explanation": "&& tiene mayor precedencia que ||, se evalua primero: (5 > 3 && 2 < 1) es false; luego false || (4 == 4) da true. Igual que en aritmetica, donde * y / se evaluan antes que + y -."
        },
        {
          "id": "fund-primwrap-interview",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Una consultora IT grande te pregunta: por que Java tiene primitivos y wrappers en vez de un solo tipo numerico?",
          "answer": "Los primitivos dan mejor rendimiento al evitar el overhead de crear objetos; los wrappers existen porque los generics (como List<T>) solo aceptan objetos, no primitivos",
          "distractors": ["Es un error de diseno heredado que nunca se corrigio", "Los wrappers son mas rapidos que los primitivos en cualquier caso", "Los primitivos fueron eliminados en versiones recientes de Java"],
          "explanation": "Es un balance deliberado: primitivos para eficiencia en operaciones numericas simples, wrappers cuando Java exige un objeto (colecciones, generics)."
        }
      ]
    },
    {
      "unitId": "fund-texto",
      "name": "Texto: String y StringBuilder",
      "certObjective": "language-basics",
      "orderIndex": 5,
      "summary": {
        "text": "String es inmutable: cada operacion que 'modifica' un String en realidad crea uno nuevo. Para concatenar texto repetidamente sin generar objetos de mas, se usa StringBuilder, que si es mutable (append, insert, delete, reverse). Los text blocks (triple comilla) permiten escribir texto multilinea sin escapar comillas ni concatenar con +.",
        "code": "String s = \"hola\";\ns.concat(\" mundo\"); // no modifica s, devuelve un String nuevo que se descarta\n\nStringBuilder sb = new StringBuilder(\"hola\");\nsb.append(\" mundo\"); // sb si cambia\n\nString texto = \"\"\"\n    Linea 1\n    Linea 2\"\"\";"
      },
      "exercises": [
        {
          "id": "fund-string-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "String es inmutable: sus metodos nunca modifican el original",
          "code": "String s = \"hola\";\ns.concat(\" mundo\");\nSystem.out.println(s); // sigue siendo \"hola\"",
          "answer": "ok",
          "explanation": "concat() devuelve un String nuevo; como no se asigna a ninguna variable, se descarta y s no cambia.",
          "conceptId": "string-inmutable",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-string-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que hay que hacer para que 's.concat(\" mundo\")' realmente cambie el valor de s?",
          "answer": "Reasignar el resultado: s = s.concat(\" mundo\");",
          "distractors": ["Nada, concat() modifica s automaticamente", "Llamar a s.mutable() antes", "Usar concat() dos veces seguidas"],
          "explanation": "Como String es inmutable, hay que capturar el resultado reasignando la variable; de lo contrario el String nuevo se descarta.",
          "conceptId": "string-inmutable",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-string-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "String a = \"java\";\nString b = a.toUpperCase();\nSystem.out.println(a + \" \" + b);",
          "answer": "java JAVA",
          "explanation": "toUpperCase() devuelve un String nuevo; a nunca cambia porque String es inmutable.",
          "conceptId": "string-inmutable",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-sb-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "StringBuilder es mutable: sus metodos modifican el mismo objeto",
          "code": "StringBuilder sb = new StringBuilder(\"hola\");\nsb.append(\" mundo\");\nSystem.out.println(sb); // hola mundo",
          "answer": "ok",
          "explanation": "A diferencia de String, append() modifica el mismo objeto StringBuilder en lugar de crear uno nuevo - mas eficiente para concatenar en un loop.",
          "conceptId": "stringbuilder",
          "role": "intro",
          "pathOrder": 3
        },
        {
          "id": "fund-sb-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que conviene usar StringBuilder en vez de String dentro de un loop que concatena muchas veces?",
          "answer": "Porque String crearia un objeto nuevo en cada concatenacion, mientras StringBuilder modifica el mismo objeto",
          "distractors": ["Porque String no permite concatenar dentro de un loop", "Porque StringBuilder es inmutable y mas seguro", "No hay diferencia real de rendimiento"],
          "explanation": "Cada concatenacion de String descarta un objeto intermedio; StringBuilder evita ese costo reutilizando el mismo buffer.",
          "conceptId": "stringbuilder",
          "role": "guided",
          "pathOrder": 4
        },
        {
          "id": "fund-sb-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Completa para invertir el contenido del StringBuilder:",
          "code": "StringBuilder sb = new StringBuilder(\"abc\");\nsb._____();\nSystem.out.println(sb); // cba",
          "answer": "reverse",
          "distractors": ["invert", "flip", "toReverse"],
          "explanation": "reverse() invierte el orden de los caracteres del StringBuilder in-place.",
          "conceptId": "stringbuilder",
          "role": "solo",
          "pathOrder": 5
        },
        {
          "id": "fund-texto-blocks",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo? (asume que no hay espacios extra al final de cada linea)",
          "code": "String texto = \"\"\"\n    Hola\n    Mundo\"\"\";\nSystem.out.println(texto);",
          "answer": "Hola\nMundo",
          "explanation": "Un text block conserva los saltos de linea tal cual, sin necesidad de \\n ni concatenar con +; la indentacion comun a todas las lineas se elimina automaticamente."
        },
        {
          "id": "fund-texto-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una empresa de servicios financieros te pregunta: que problema de rendimiento evita usar StringBuilder en vez de += dentro de un loop de miles de iteraciones?",
          "answer": "Evita crear miles de objetos String intermedios descartables, ya que += con String crea un objeto nuevo en cada iteracion",
          "distractors": ["Evita que el programa se quede sin memoria de forma inmediata en cualquier caso", "Hace que el codigo compile mas rapido", "String += no compila dentro de un loop"],
          "explanation": "Cada += sobre un String genera un objeto descartado inmediatamente; en un loop grande esto genera mucha basura para el garbage collector. StringBuilder evita ese costo modificando el mismo buffer."
        }
      ]
    },
    {
      "unitId": "fund-fechas",
      "name": "Fechas y tiempo (Date-Time API)",
      "certObjective": "language-basics",
      "orderIndex": 6,
      "summary": {
        "text": "El Date-Time API (java.time) reemplaza las clases viejas Date/Calendar. LocalDate guarda solo fecha, LocalTime solo hora, LocalDateTime ambos - ninguno de los tres conoce zona horaria. Duration mide un lapso en horas/minutos/segundos; Period mide un lapso en anios/meses/dias. Instant representa un punto exacto en la linea de tiempo (UTC), y ZonedDateTime agrega zona horaria, relevante para horario de verano (daylight saving time).",
        "code": "LocalDate hoy = LocalDate.now();\nLocalDate cumple = LocalDate.of(2026, 12, 25);\nPeriod faltan = Period.between(hoy, cumple);\n\nZonedDateTime reunion = ZonedDateTime.now(ZoneId.of(\"America/Santiago\"));"
      },
      "exercises": [
        {
          "id": "fund-fecha-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "LocalDate, LocalTime y LocalDateTime guardan fecha y/o hora sin zona horaria",
          "code": "LocalDate fecha = LocalDate.of(2026, 12, 25);\nLocalTime hora = LocalTime.of(14, 30);\nLocalDateTime ambos = LocalDateTime.of(fecha, hora);",
          "answer": "ok",
          "explanation": "LocalDate solo tiene anio/mes/dia, LocalTime solo hora/minuto/segundo, LocalDateTime combina ambos - ninguno sabe en que zona horaria esta.",
          "conceptId": "local-date-time",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-fecha-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que clase usarias para guardar solo la hora de una alarma, sin fecha?",
          "answer": "LocalTime",
          "distractors": ["LocalDate", "Instant", "Duration"],
          "explanation": "LocalTime representa exclusivamente una hora del dia, sin fecha ni zona horaria.",
          "conceptId": "local-date-time",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-fecha-solo",
          "type": "fill_blank",
          "difficulty": 2,
          "prompt": "Crea una fecha para el 25 de diciembre de 2026:",
          "code": "LocalDate cumple = LocalDate._____(2026, 12, 25);",
          "answer": "of",
          "distractors": ["now", "create", "parse"],
          "explanation": "LocalDate.of(anio, mes, dia) construye una fecha especifica; now() devuelve la fecha actual.",
          "conceptId": "local-date-time",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-duracion-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "Duration mide tiempo en horas/minutos/segundos; Period mide en anios/meses/dias",
          "code": "Duration vuelo = Duration.ofHours(3).plusMinutes(30); // 3h 30m\nPeriod faltan = Period.between(LocalDate.now(), LocalDate.of(2026, 12, 25));",
          "answer": "ok",
          "explanation": "Duration es para lapsos basados en tiempo (horas, minutos, segundos); Period es para lapsos basados en calendario (anios, meses, dias) - no son intercambiables.",
          "conceptId": "duration-period",
          "role": "intro",
          "pathOrder": 3
        },
        {
          "id": "fund-duracion-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Necesitas calcular cuantos dias faltan hasta una fecha futura. Que clase usas?",
          "answer": "Period",
          "distractors": ["Duration", "Instant", "LocalTime"],
          "explanation": "Period trabaja con unidades de calendario (anios/meses/dias); Duration trabaja con unidades de tiempo exacto (horas/minutos/segundos).",
          "conceptId": "duration-period",
          "role": "guided",
          "pathOrder": 4
        },
        {
          "id": "fund-duracion-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "Duration d = Duration.ofMinutes(90);\nSystem.out.println(d.toHours() + \"h \" + (d.toMinutes() % 60) + \"m\");",
          "answer": "1h 30m",
          "explanation": "90 minutos equivalen a 1 hora completa (toHours() trunca) mas 30 minutos restantes (toMinutes() % 60).",
          "conceptId": "duration-period",
          "role": "solo",
          "pathOrder": 5
        },
        {
          "id": "fund-fechas-instant",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Cual es la diferencia principal entre Instant y ZonedDateTime?",
          "answer": "Instant es un punto exacto en UTC sin zona horaria; ZonedDateTime agrega una zona horaria especifica, relevante para horario de verano",
          "distractors": ["No hay diferencia, son alias de la misma clase", "Instant incluye zona horaria y ZonedDateTime no", "ZonedDateTime no puede representar fechas pasadas"],
          "explanation": "Instant marca un momento absoluto en la linea de tiempo (como un timestamp UTC); ZonedDateTime interpreta ese momento en una zona horaria concreta, incluyendo ajustes por horario de verano."
        },
        {
          "id": "fund-fechas-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una consultora IT grande te pregunta: por que no conviene guardar solo un LocalDateTime para programar una reunion internacional?",
          "answer": "Porque LocalDateTime no tiene zona horaria: el mismo valor se interpreta distinto segun donde este cada participante",
          "distractors": ["Porque LocalDateTime no permite fechas futuras", "Porque LocalDateTime es mas lento que Date", "No hay ningun problema, LocalDateTime alcanza siempre"],
          "explanation": "Sin zona horaria, un LocalDateTime es ambiguo entre paises; para eventos que involucran distintas zonas horarias conviene ZonedDateTime o Instant."
        }
      ]
    },
    {
      "unitId": "fund-condicionales",
      "name": "Condicionales: if/else y switch",
      "certObjective": "language-basics",
      "orderIndex": 7,
      "summary": {
        "text": "if/else controla el flujo segun una condicion booleana. switch (statement) compara un valor contra varios case; el switch expression moderno (con ->) devuelve un valor directamente y no necesita break, evitando el fall-through accidental. yield se usa dentro de un bloque de switch expression para devolver un valor cuando el case necesita mas de una linea.",
        "code": "int dia = 3;\nString nombre = switch (dia) {\n    case 1, 7 -> \"Fin de semana\";\n    case 2, 3, 4, 5, 6 -> \"Dia habil\";\n    default -> \"Invalido\";\n};"
      },
      "exercises": [
        {
          "id": "fund-switch-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "El switch statement clasico necesita break para evitar fall-through",
          "code": "int dia = 2;\nswitch (dia) {\n    case 1:\n        System.out.println(\"Lunes\");\n        break;\n    case 2:\n        System.out.println(\"Martes\");\n        break;\n    default:\n        System.out.println(\"Otro\");\n}",
          "answer": "ok",
          "explanation": "Sin break, la ejecucion 'cae' al siguiente case (fall-through) aunque no coincida, ejecutando codigo de mas de un case.",
          "conceptId": "switch-statement",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-switch-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que pasa si un switch statement clasico no tiene break en un case que coincide?",
          "answer": "La ejecucion continua (fall-through) hacia el siguiente case, sin evaluar su condicion",
          "distractors": ["El programa no compila", "Se lanza una excepcion en tiempo de ejecucion", "Se ejecuta solo el case que coincidio, igual que con break"],
          "explanation": "Fall-through es el comportamiento por defecto del switch clasico: sin break, sigue ejecutando el codigo de los case siguientes.",
          "conceptId": "switch-statement",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-switch-solo",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "int x = 1;\nswitch (x) {\n    case 1:\n        System.out.println(\"uno\");\n    case 2:\n        System.out.println(\"dos\");\n        break;\n    case 3:\n        System.out.println(\"tres\");\n}",
          "answer": "uno\ndos",
          "explanation": "Como el case 1 no tiene break, la ejecucion cae al case 2 y tambien lo imprime, deteniendose recien en su break.",
          "conceptId": "switch-statement",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-switchexpr-intro",
          "type": "worked_example",
          "difficulty": 3,
          "prompt": "El switch expression moderno devuelve un valor con -> y no necesita break",
          "code": "int dia = 3;\nString tipo = switch (dia) {\n    case 1, 7 -> \"Fin de semana\";\n    case 2, 3, 4, 5, 6 -> \"Dia habil\";\n    default -> \"Invalido\";\n};",
          "answer": "ok",
          "explanation": "Con ->, cada case ejecuta solo su expresion (sin fall-through) y el switch completo se puede asignar directamente a una variable.",
          "conceptId": "switch-expression",
          "role": "intro",
          "pathOrder": 3,
          "dependsOn": ["switch-statement"]
        },
        {
          "id": "fund-switchexpr-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Que palabra clave se usa dentro de un bloque { } de switch expression para devolver un valor?",
          "answer": "yield",
          "distractors": ["return", "break", "give"],
          "explanation": "yield devuelve el valor del bloque del case actual; return terminaria el metodo entero, no solo el switch.",
          "conceptId": "switch-expression",
          "role": "guided",
          "pathOrder": 4,
          "dependsOn": ["switch-statement"]
        },
        {
          "id": "fund-switchexpr-solo",
          "type": "fill_blank",
          "difficulty": 3,
          "prompt": "Completa el switch expression para que devuelva un valor calculado en un bloque:",
          "code": "int puntos = switch (nivel) {\n    case 1 -> 10;\n    case 2 -> {\n        int extra = 5;\n        _____ 20 + extra;\n    }\n    default -> 0;\n};",
          "answer": "yield",
          "distractors": ["return", "break", "value"],
          "explanation": "Dentro de un bloque { } de un case, yield es obligatorio para devolver el valor del switch expression.",
          "conceptId": "switch-expression",
          "role": "solo",
          "pathOrder": 5,
          "dependsOn": ["switch-statement"]
        },
        {
          "id": "fund-cond-ifelse",
          "type": "predict_output",
          "difficulty": 1,
          "prompt": "Que imprime este codigo?",
          "code": "int nota = 65;\nif (nota >= 90) {\n    System.out.println(\"A\");\n} else if (nota >= 70) {\n    System.out.println(\"B\");\n} else {\n    System.out.println(\"C\");\n}",
          "answer": "C",
          "explanation": "65 no cumple ninguna de las dos condiciones anteriores, asi que cae en el else final."
        },
        {
          "id": "fund-cond-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una empresa de servicios financieros te pregunta: por que preferir switch expression sobre if/else encadenados cuando comparas un mismo valor contra muchas opciones?",
          "answer": "Es mas legible, evita el riesgo de fall-through del switch clasico, y el compilador puede verificar exhaustividad en algunos casos (como con enums o sealed types)",
          "distractors": ["Porque if/else no puede comparar mas de dos valores", "Porque switch expression es la unica forma de comparar enteros en Java", "No hay ninguna ventaja real, es solo preferencia de estilo"],
          "explanation": "Ademas de la legibilidad, el switch expression moderno evita bugs de fall-through y, en combinacion con sealed classes/pattern matching, permite verificacion de exhaustividad en tiempo de compilacion."
        }
      ]
    },
    {
      "unitId": "fund-loops",
      "name": "Loops: while, for y control de iteracion",
      "certObjective": "language-basics",
      "orderIndex": 8,
      "summary": {
        "text": "while evalua la condicion antes de cada vuelta; do-while la evalua despues, asi que siempre ejecuta el cuerpo al menos una vez. El for clasico controla inicializacion/condicion/incremento explicitamente; el enhanced for (for-each) recorre una coleccion o array sin necesitar un indice. break corta el loop por completo; continue salta a la siguiente iteracion. Una etiqueta (label) permite que break/continue afecten a un loop externo desde dentro de uno anidado.",
        "code": "outer:\nfor (int i = 0; i < 3; i++) {\n    for (int j = 0; j < 3; j++) {\n        if (j == 1) continue outer;\n        System.out.println(i + \",\" + j);\n    }\n}"
      },
      "exercises": [
        {
          "id": "fund-while-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "while evalua la condicion antes; do-while la evalua despues (ejecuta al menos una vez)",
          "code": "int i = 5;\nwhile (i < 3) {\n    System.out.println(i); // nunca se ejecuta\n}\n\nint j = 5;\ndo {\n    System.out.println(j); // se ejecuta una vez, imprime 5\n} while (j < 3);",
          "answer": "ok",
          "explanation": "while chequea la condicion antes de entrar al cuerpo; do-while entra al cuerpo primero y recien despues chequea, garantizando al menos una ejecucion.",
          "conceptId": "while-dowhile",
          "role": "intro",
          "pathOrder": 0
        },
        {
          "id": "fund-while-guided",
          "type": "mcq",
          "difficulty": 1,
          "prompt": "Que tipo de loop garantiza que el cuerpo se ejecute al menos una vez, incluso si la condicion es falsa desde el inicio?",
          "answer": "do-while",
          "distractors": ["while", "for", "enhanced for"],
          "explanation": "do-while evalua la condicion despues de ejecutar el cuerpo, asi que la primera ejecucion ocurre siempre.",
          "conceptId": "while-dowhile",
          "role": "guided",
          "pathOrder": 1
        },
        {
          "id": "fund-while-solo",
          "type": "predict_output",
          "difficulty": 2,
          "prompt": "Que imprime este codigo?",
          "code": "int i = 10;\ndo {\n    System.out.println(i);\n} while (i < 5);",
          "answer": "10",
          "explanation": "do-while ejecuta el cuerpo una vez antes de chequear la condicion; imprime 10 y luego, como 10 < 5 es false, no repite.",
          "conceptId": "while-dowhile",
          "role": "solo",
          "pathOrder": 2
        },
        {
          "id": "fund-for-intro",
          "type": "worked_example",
          "difficulty": 2,
          "prompt": "El enhanced for recorre una coleccion o array sin necesitar indice",
          "code": "int[] numeros = {10, 20, 30};\nfor (int n : numeros) {\n    System.out.println(n);\n}",
          "answer": "ok",
          "explanation": "El enhanced for (for-each) itera cada elemento directamente, sin declarar ni manejar un indice como en el for clasico.",
          "conceptId": "for-enhanced-for",
          "role": "intro",
          "pathOrder": 3
        },
        {
          "id": "fund-for-guided",
          "type": "mcq",
          "difficulty": 2,
          "prompt": "Por que no se puede usar enhanced for para saltar de a 2 elementos?",
          "answer": "Porque el enhanced for no expone ningun indice ni contador, solo entrega cada elemento en orden",
          "distractors": ["Si se puede, usando la variable del for-each como indice", "Porque enhanced for no funciona con arrays", "Porque enhanced for siempre recorre en orden inverso"],
          "explanation": "El enhanced for oculta el mecanismo de iteracion; para controlar el paso o el indice hay que usar el for clasico.",
          "conceptId": "for-enhanced-for",
          "role": "guided",
          "pathOrder": 4
        },
        {
          "id": "fund-for-solo",
          "type": "fill_blank",
          "difficulty": 1,
          "prompt": "Completa el for clasico para recorrer de 0 a 4:",
          "code": "for (int i = 0; i _____ 5; i++) {\n    System.out.println(i);\n}",
          "answer": "<",
          "distractors": ["<=", "!=", ">"],
          "explanation": "i < 5 recorre 0,1,2,3,4 (5 vueltas); i <= 5 recorreria una vuelta de mas (hasta 5).",
          "conceptId": "for-enhanced-for",
          "role": "solo",
          "pathOrder": 5
        },
        {
          "id": "fund-loop-breakcontinue",
          "type": "predict_output",
          "difficulty": 3,
          "prompt": "Que imprime este codigo?",
          "code": "for (int i = 0; i < 5; i++) {\n    if (i == 2) continue;\n    if (i == 4) break;\n    System.out.println(i);\n}",
          "answer": "0\n1\n3",
          "explanation": "continue salta la impresion cuando i==2 (pero el loop sigue); break corta el loop por completo cuando i==4, asi que 4 nunca se imprime."
        },
        {
          "id": "fund-loop-interview",
          "type": "mcq",
          "difficulty": 3,
          "prompt": "Una consultora IT grande te pregunta: para que sirve una etiqueta (label) como 'outer:' antes de un loop?",
          "answer": "Permite que un break o continue dentro de un loop anidado afecte al loop externo etiquetado, en vez de solo al mas interno",
          "distractors": ["Es solo un comentario, no tiene efecto en la ejecucion", "Convierte el loop en un metodo separado", "Hace que el loop se ejecute en paralelo"],
          "explanation": "Sin etiqueta, break/continue solo afectan al loop mas interno donde aparecen; con una etiqueta (break outer; / continue outer;) se puede controlar un loop exterior especifico desde dentro de uno anidado."
        }
      ]
    }
```

Insert this block right after the closing `}` of the existing
`fund-types-and-main` unit (adding a comma there), and right before the
closing `]` of the `units` array / `}` of the file.

- [ ] **Step 2: Bump the content version**

In `app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt`,
change:

```kotlin
private const val CURRENT_CONTENT_VERSION = "7"
```

to:

```kotlin
private const val CURRENT_CONTENT_VERSION = "8"
```

- [ ] **Step 3: Validate the edited file is valid JSON**

Run: `python3 -m json.tool app/src/main/assets/content/java-fundamentals.json > /dev/null && echo VALID`
Expected: `VALID` printed, no errors.

- [ ] **Step 4: Validate no duplicate exercise/unit ids were introduced**

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
"
```
Expected: `OK 8 units, 62 exercises` (8 units = 3 existing + 5 new; 62
exercises = 21 existing + 41 new), no AssertionError.

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — regression check that the new content and the
version bump don't break anything.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/content/java-fundamentals.json \
        app/src/main/java/com/zconte/oopsapp/data/content/ContentSeeder.kt
git commit -m "content: add data types + control flow units to Fundamentos de Java (sub-cycle 1)"
```

---

## After the task: manual on-device QA

No automated UI/content test covers full playability. Once merged, install
a clean/in-place build and manually verify on-device:

1. Ruta shows Fundamentos de Java with 8 units total (3 existing + 5 new),
   in order: Que es Java?, Estructura de una clase, Tipos/variables/main,
   Tipos primitivos y wrappers, Texto, Fechas y tiempo, Condicionales,
   Loops.
2. From Home, tap "Estudiar hoy" (not "Ver ruta" unit-play, which strips
   `worked_example` cards via `answerableOnly()`): confirm each new
   concept's `worked_example` intro card renders and auto-advances the
   first time, and does not reappear after its `solo` exercise is
   answered ("born"). Separately, play each of the 5 new units directly
   from Ruta to confirm `guided`/`solo` exercises grade correctly there,
   and that `predict_output` multi-line answers grade correctly (e.g.
   `fund-switch-solo`, `fund-loop-breakcontinue`).
3. Confirm "Ver resumen" (Unit Summary / Tips) shows the new `summary`
   text/code for each of the 5 new units.
4. Confirm the section's mandatory checkpoint still triggers correctly
   after all 8 units are complete, and samples questions across the wider
   unit pool.
5. Confirm existing progress (streak/XP/prior review_state on the 3
   original units) is preserved across the version bump (in-place upgrade,
   not clean reinstall, to specifically test this).
