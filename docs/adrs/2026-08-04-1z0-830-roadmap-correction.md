# ADR: Corrección del roadmap de contenido contra los objetivos reales de 1Z0-830

**Estado:** Aceptado

## Contexto

El roadmap de 12 secciones registrado en `docs/adrs/2026-07-20-content-structure-sections-checkpoints.md`
y en la memoria de Fase 2.3 se armó a partir del índice de un libro de
**Java 11** ("el usuario tiene el libro OCP Complete Study Guide, edición
Java 11" — cita textual de esa ADR), no de los objetivos reales del examen
que la app dice preparar: **1Z0-830 (Java SE 21)**.

Al planificar el retrofit de escaleras de la sección "Fundamentos de Java"
(ver `docs/adrs/2026-08-04-ladders-content-retrofit-policy.md`), Luis notó
que esa sección (3 unidades: Qué es Java, Estructura de una clase, Tipos/
variables/main) no cubre temas de OOP que sí entran en el examen real
(herencia, clases especiales, enums, interfaces avanzadas). Esto motivó
verificar el temario completo, no solo el de esa sección.

**Verificación de fuentes:** se confirmaron los 10 grupos de objetivos
reales de 1Z0-830 contra dos fuentes independientes — una que cita texto
que parece copiado literalmente de Oracle
([Enthuware](https://enthuware.com/oca-ocp-java-certification-resources/290-ocp-java-21-exam-syllabus)),
y un resumen que Oracle publica en education.oracle.com (recuperado vía
búsqueda; la página en sí no respondió — timeout repetido). Ambas
coinciden en los 10 grupos y en que **JDBC y Security no forman parte del
temario oficial actual** — sí aparecen como capítulos extra en la guía de
estudio Pearson/Sybex, pero esas guías suelen incluir material más allá
del examen. Se descartó también un capítulo de un libro que Luis
encontró (ISBN `9781119619130`) por ser, otra vez, edición Java 11
(exámenes 1Z0-815/816/817) — mismo problema que originó esta ADR.

El detalle específico de sub-objetivos por sección (abajo) lo investigó
Luis directamente, tema por tema, cruzado contra el contenido ya
shippeado.

## Decisión

### 1. Alcance: el examen es el piso, no el techo

`PROJECT-OOPS.md` decía que el examen es la meta final. Se corrige: el
Path cubre el 100% de los objetivos reales de 1Z0-830 como núcleo
obligatorio, más secciones extra marcadas explícitamente como "fuera del
examen, útiles para entrevistas" (JDBC, Annotations, Security, Java
moderno 22-25). Decisión de Luis, explícita.

### 2. Roadmap corregido — 13 secciones

**Núcleo (objetivos reales de 1Z0-830, confirmados):**

1. **Fundamentos de Java**
2. **Generics y Colecciones**
3. **Streams y Lambdas**
4. **Manejo de Excepciones**
5. **Concurrencia**
6. **Modulos y Empaquetado**
7. **I/O y NIO.2**
8. **Localización** — nueva, no existía en el roadmap viejo
9. **Features de Java 21** (sealed classes, records, pattern matching) —
   el objetivo real los mete dentro del grupo "OOP", no son un grupo
   aparte. Se mantienen como sección propia por razones pedagógicas
   (evita una sección 1 gigantesca) — **decisión organizativa, reversible,
   no un objetivo oficial separado.**

**Extra (fuera del examen, valor de entrevista, sin cambio de orden
respecto al roadmap viejo):**

10. JDBC
11. Annotations
12. Security
13. Extra Moderno (Java 22-25)

### 3. Detalle por sección núcleo — sub-objetivos y gap contra lo shippeado

#### 1. Fundamentos de Java
Hoy: 3 unidades (Qué es Java, Estructura de una clase, Tipos/variables/main),
nivel muy básico. Objetivos reales que faltan por completo: **tipos de
datos y texto** (primitivos, wrappers, text blocks, Date-Time API),
**control de flujo** (if/switch, loops, break/continue), y la mayor parte
de **OOP**: herencia/polimorfismo/abstract classes (no cubierto en ningún
lado hoy), clases especiales (final, inner, nested, anonymous), enums,
interfaces avanzadas (default/private/static methods), funcional
interfaces + lambda expressions como fundamento (sintaxis, no solo su uso
dentro de Streams).

#### 2. Generics y Colecciones
Hoy: 4 unidades (Generics, Listas/Sets, Maps/Deques, Comparadores/
inmutables). Objetivos reales que faltan: **bounded type parameters**
(`extends`/`super`), **wildcards** (`?`, `? extends T`, `? super T`) y la
regla PECS, **type erasure**/tipos no reificables, **Sequenced
Collections** (`SequencedCollection`/`SequencedSet`/`SequencedMap`, Java
21), `ConcurrentModificationException`, y **Arrays** (declaración,
multi-dimensionales, clase `Arrays`) — ausente por completo hoy.

#### 3. Streams y Lambdas
Hoy: 4 unidades (creación, intermedias, terminales, collectors — incluye
la unidad piloto de escaleras `streams-collectors`). Objetivos reales que
faltan: **lambda expressions y functional interfaces como tema propio**
(sintaxis, `Predicate`/`Consumer`/`Function`/`Supplier`, method
references) — hoy se usan implícitamente dentro de ejercicios de streams
pero nunca se enseñan como fundamento aparte. **`Optional` no tiene
ninguna unidad** — gap real (creación, `isPresent`/`ifPresent`/`orElse`/
`orElseGet`/`orElseThrow`).

#### 4. Manejo de Excepciones
Hoy: 4 unidades (jerarquía, try-catch-finally+multi-catch,
try-with-resources, personalizadas+encadenamiento). **La sección
mejor cubierta hasta ahora** — coincide de cerca con los objetivos reales.
Gaps menores: regla de override sobre checked exceptions (una subclase no
puede declarar `throws` más amplio que la superclase), caso de compilación
"unreachable catch block"/"unreachable code tras un `throw` definitivo".

#### 5. Concurrencia
Hoy: 4 unidades (threads/ciclo de vida, executors, sincronización, virtual
threads). Objetivos reales que faltan: **`Callable`/`Future`/
`CompletableFuture`** (ejecución async, encadenar tareas dependientes) —
ausente por completo; **`Semaphore`/`ReadWriteLock`** (sincronización
avanzada); cobertura explícita de **`ConcurrentHashMap`/
`CopyOnWriteArrayList`**; parallel streams desde el ángulo de concurrencia.
(Structured concurrency queda diferida a Extra Moderno, como ya estaba
decidido — no es un gap.)

#### 6. Modulos y Empaquetado (sección nueva, autoría desde cero)
- Declaración de módulos: `module-info.java`; directivas `exports`,
  `requires`, `uses`, `provides`, `to` (exports/services calificados)
- Creación y compilación: `--module-source-path`, `-d`; JARs modulares
  (`jar` con `--main-class`/`--module-version`); ejecución con `-p`/`-m`
- Servicios en JPMS: interfaces de servicio + providers; `uses` /
  `provides ... with`
- Migración y compatibilidad: unnamed module, automatic modules, split
  packages, acceso a APIs internas vía reflection/`--add-exports`

#### 7. I/O y NIO.2 (sección nueva, autoría desde cero)
- I/O clásico (`java.io`): consola vía standard streams; byte streams
  (`InputStream`/`OutputStream`) y char streams (`Reader`/`Writer`);
  serialización/deserialización (`Serializable`, `ObjectInputStream`,
  `ObjectOutputStream`)
- NIO.2 (`java.nio.file`): construcción/traversal de `Path` (`Path.of()`,
  `Paths.get()`); comparar/normalizar/resolver/relativizar; existencia/
  tamaño/atributos; operaciones de archivo vía `Files`
  (copy/move/delete, `StandardCopyOption`)

#### 8. Localización (sección nueva, autoría desde cero)
- `Locale`: constructores/builders; códigos de idioma/país/variante;
  locale por defecto y cómo cambiarlo en código
- `ResourceBundle`: carga de `.properties` y bundles basados en clases;
  resolución de claves y fallback; empaquetado seguro de textos/mensajes
- Formateo: números/moneda/porcentajes vía `NumberFormat`; fechas/horas/
  períodos vía `DateTimeFormatter` combinado con locale

#### 9. Features de Java 21 (sección nueva, autoría desde cero)
- **Sealed classes**: `permits` obligatorio (salvo mismo archivo); cada
  subclase permitida debe ser `final`/`sealed`(con su propio
  `permits`)/`non-sealed`; interfaces pueden ser `sealed`/`non-sealed`
  pero nunca `final`; subclases permitidas deben compartir módulo (o
  paquete, sin módulos)
- **Records**: campos `private final` + accesores implícitos (`nombre()`,
  no `getNombre()`), `equals`/`hashCode`/`toString()` generados;
  implícitamente `final`, extiende `java.lang.Record` (no puede extender
  otra clase, sí implementar interfaces); constructor compacto (sin lista
  de parámetros) para validación, sin reasignar `this.campo` manualmente;
  sin campos de instancia adicionales en el cuerpo (solo `static`)
- **Pattern matching**: `instanceof` sin cast explícito + flow scoping
  (ej. válido tras `&&`); `switch` con pattern matching por tipo;
  cláusulas `when` (guards); exhaustividad (switch sobre sealed class no
  necesita `default` si cubre todas las subclases permitidas); record
  patterns (deconstrucción en `instanceof`/`switch`)

### 4. Tamaño del Ciclo 1 (Fundamentos de Java) — split recomendado

El ciclo de retrofit más grande hecho hasta ahora (Manejo de Excepciones)
fue 4 unidades / 23 ejercicios, una sola tarea de subagent-driven-development.
Fundamentos-a-cobertura-completa es 2-3 veces ese tamaño (retrofit de 3
unidades existentes + ~5-6 unidades nuevas para cerrar los gaps de arriba).
Se recomienda **dividir el Ciclo 1 en 2-3 sub-ciclos** en vez de una sola
tarea gigante:

1. Tipos de datos + control de flujo (unidades nuevas)
2. OOP núcleo: herencia, polimorfismo, abstract classes, clases
   especiales, enums (retrofit de "Estructura de una clase" + unidades
   nuevas)
3. OOP avanzado: interfaces (default/private/static) + functional
   interfaces/lambda fundamentals (retrofit de "Qué es Java"/"Tipos,
   variables y main" + unidades nuevas)

El plan de autoría concreto de cada sub-ciclo se decide al planificarlo
(`superpowers:writing-plans`), no en esta ADR.

## Consecuencias

- Se reemplaza la lista de 12 secciones de la ADR de 2026-07-20 por esta
  de 13 — el modelo Sección→Unidad→Checkpoint de esa ADR sigue vigente
  sin cambios.
- `PROJECT-OOPS.md` se corrige en el mismo lote: Purpose (examen como piso,
  no como techo), Learning structure (lista de dominios de referencia
  corregida), Pointers (apunta a esta ADR en vez del roadmap viejo).
- `docs/adrs/2026-08-04-ladders-content-retrofit-policy.md` se corrige
  para reflejar que "retrofit" ahora significa "escaleras + cobertura
  completa de objetivos", no solo agregar `worked_example` a lo que ya
  existe — mismo orden de ciclos, costo honesto.
- El roadmap de secciones extra (10-13) sigue pausado detrás del retrofit
  de las 5 secciones núcleo ya shippeadas, como ya establecía la ADR de
  retrofit.
