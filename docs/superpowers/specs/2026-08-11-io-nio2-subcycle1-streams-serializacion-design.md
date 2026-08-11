# I/O y NIO.2 — sub-ciclo 1: Streams clasicos + Serializacion — Diseño

**Estado:** Aprobado, pendiente de plan de implementacion.

## Contexto

Primera seccion nueva desde cero de esta serie despues de Modulos y
Empaquetado (ambos sub-ciclos completos, QA'd y pusheados). El ADR
(`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md:133-141`) fija el
alcance completo de la seccion en 2 bloques:

> - I/O clasico (`java.io`): consola via standard streams; byte streams
>   (`InputStream`/`OutputStream`) y char streams (`Reader`/`Writer`);
>   serializacion/deserializacion (`Serializable`, `ObjectInputStream`,
>   `ObjectOutputStream`)
> - NIO.2 (`java.nio.file`): construccion/traversal de `Path` (`Path.of()`,
>   `Paths.get()`); comparar/normalizar/resolver/relativizar;
>   existencia/tamaño/atributos; operaciones de archivo via `Files`
>   (copy/move/delete, `StandardCopyOption`)

Igual que Modulos y Empaquetado, se divide en 2 sub-ciclos tematicamente
distintos: sub-ciclo 1 (este spec) cubre `java.io` completo; sub-ciclo 2
cubrira `java.nio.file`.

**Nueva regla de contenido aplicable desde este ciclo en adelante**
(feedback de Luis, 2026-08-11, jugando Modulos y Empaquetado): los
distractores de `mcq`/`fill_blank` deben tener largo y nivel de detalle
comparable a la respuesta correcta — la respuesta correcta no debe ser
identificable solo por ser la opcion mas larga/detallada. Se aplica a
cada ejercicio de este sub-ciclo en adelante; el corpus existente queda
pendiente de auditoria aparte (no bloqueante para este ciclo).

## Verificacion tecnica previa (JDK 20 real, scratch project)

Antes de fijar el diseño se corrieron escenarios reales de compilacion y
ejecucion, siguiendo la practica estandar del proyecto de no confiar en
memoria para comportamiento runtime-observable. Hechos confirmados:

1. **try-with-resources cierra los recursos en orden INVERSO al de
   declaracion.** Confirmado con 3 recursos (`A`, `B`, `C` declarados en
   ese orden): el orden de cierre real fue `C`, `B`, `A`.
2. **Un campo `transient` se deserializa al valor por defecto de su tipo**
   (`0` para `int`, `null` para referencias, `false` para `boolean`), no
   al valor que tenia al momento de serializar. Confirmado: un `int
   transient` con valor `30` al serializar volvio como `0` al
   deserializar.
3. **Un campo `static` nunca se serializa.** Confirmado: se modifico el
   valor del campo estatico DESPUES de serializar pero ANTES de
   deserializar, y el objeto leido reflejo el valor actual de la clase en
   ese momento, no ninguno de los dos valores relacionados con el momento
   de la escritura — la conclusion pedagogica es simplemente "los campos
   estaticos quedan completamente fuera de la serializacion".
4. **Una superclase no-`Serializable` sin constructor publico sin
   argumentos permite la ESCRITURA sin error, pero la LECTURA falla.**
   Confirmado: `writeObject()` completo sin excepcion; `readObject()`
   lanzo `java.io.InvalidClassException` con mensaje `"<Clase>; no valid
   constructor"`. Distincion didactica importante: el error aparece en
   deserializacion, no en serializacion.
5. **Un mismatch de `serialVersionUID` entre la clase que escribio los
   bytes y la clase que los lee lanza `InvalidClassException`.** Mensaje
   exacto confirmado: `"<Clase>; local class incompatible: stream
   classdesc serialVersionUID = <viejo>, local class serialVersionUID =
   <nuevo>"`.
6. **Escribir la MISMA referencia de objeto dos veces con
   `writeObject()` preserva identidad al leer.** Confirmado: tras
   `oos.writeObject(p); oos.writeObject(p);` y leer dos veces con
   `readObject()`, `a == b` fue `true` — el stream detecta que ya escribio
   ese objeto y guarda una referencia de vuelta en lugar de duplicar los
   datos.

Todos los hechos anteriores fueron re-verificados ejecutando codigo real
contra OpenJDK 20 (no citados de memoria), y se re-verificaran de nuevo
al momento de autoria de cada ejercicio.

## Diseño de unidades

### Unidad 1: "Streams clasicos"

`unitId: io-streams-clasicos`, `orderIndex: 1`, `certObjective: io-nio2`.

3 conceptos, ~9 ejercicios:

1. **`consola-standard-streams`** — `System.in`/`System.out`/`System.err`
   son los 3 standard streams; `System.out`/`err` son `PrintStream`,
   `System.in` es un `InputStream` crudo (se envuelve tipicamente en
   `Scanner` o un `Reader` para leer texto linea a linea).
2. **`byte-streams-basicos`** — `InputStream`/`OutputStream` leen/escriben
   bytes crudos; `FileInputStream`/`FileOutputStream` son la
   implementacion tipica sobre archivos; try-with-resources cierra
   multiples recursos en orden inverso al de declaracion (hecho #1).
3. **`char-streams-basicos`** — `Reader`/`Writer` (y sus implementaciones
   `FileReader`/`FileWriter`) trabajan con caracteres/texto, no bytes
   crudos; `BufferedReader.readLine()` para lectura linea a linea,
   devuelve `null` al llegar a fin de archivo (contraste explicito con
   byte streams, que devuelven `-1` en `read()` al llegar a EOF).

### Unidad 2: "Serializacion"

`unitId: io-serializacion`, `orderIndex: 2`, `certObjective: io-nio2`.

3 conceptos, ~9-10 ejercicios:

1. **`serializable-basico`** — `Serializable` es una interfaz marcadora
   (sin metodos); habilita que `ObjectOutputStream` pueda escribir un
   objeto. Una superclase no-serializable sin constructor publico sin
   argumentos compila y escribe bien, pero falla en la LECTURA con
   `InvalidClassException` (hecho #4) — refuerza el patron ya visto en
   Modulos y Empaquetado de "compila/escribe bien, falla despues" como
   categoria de trampa recurrente en el examen.
2. **`object-streams`** — `ObjectOutputStream.writeObject()` /
   `ObjectInputStream.readObject()` para escribir/leer el grafo completo
   de un objeto; escribir la misma referencia dos veces preserva
   identidad al leer, no la duplica (hecho #6).
3. **`serialversionuid-transient`** — `transient` excluye un campo
   especifico de la serializacion (vuelve al valor por defecto al leer,
   hecho #2); un campo `static` queda fuera siempre, sin marcarlo (hecho
   #3); un mismatch de `serialVersionUID` entre escritura y lectura lanza
   `InvalidClassException` con un mensaje que cita ambos valores (hecho
   #5).

**Total sub-ciclo 1:** 6 conceptos, ~18-19 ejercicios, 2 unidades —
tamaño comparable al sub-ciclo 1 de Modulos y Empaquetado (8
conceptos/24 ejercicios, algo mas grande) o al de Streams y Lambdas
sub-ciclo 1 (6 unidades/51 ejercicios en total, pero repartidos en mas
unidades); este sub-ciclo se mantiene deliberadamente en el rango de 2
unidades para no repetir el patron de Fundamentos (que tuvo que
sub-dividirse por ser demasiado grande de entrada).

## Fuera de alcance (deliberado)

- `Externalizable` (control manual total de la serializacion via
  `writeExternal`/`readExternal`): no mencionado en el ADR; variante
  avanzada de `Serializable` con uso real mucho menos comun en el examen.
- `PrintWriter`, `BufferedWriter`, `DataInputStream`/`DataOutputStream` y
  otras clases decoradoras especificas de `java.io`: el ADR habla de
  "byte streams" y "char streams" como conceptos, no de agotar cada clase
  concreta de la jerarquia — `FileInputStream`/`FileOutputStream` y
  `FileReader`/`FileWriter` alcanzan para enseñar los conceptos de forma
  representativa.
- `Console` (`System.console()`): API de consola interactiva de bajo uso
  real, no mencionada en el ADR.
- NIO.2 (`Path`/`Files`): explicitamente sub-ciclo 2, no este.

## Cambios de codigo requeridos

Nueva seccion, mismo patron que Modulos y Empaquetado sub-ciclo 1: nuevo
archivo `app/src/main/assets/content/io-nio2.json`
(`sectionId: java-io-nio2`, `orderIndex: 7`, `examVersion: core`), mas
una linea agregada a `ContentPackRegistry.assetPaths`. Bump de
`CURRENT_CONTENT_VERSION` (21 → 22).

## Reglas estandar aplicables (recordatorio, mas la nueva de este ciclo)

Las reglas duras de siempre: sin colision de mayusculas entre
distractores y respuesta; un solo ejercicio solo/practice terminal por
concepto; `dependsOn` solo dentro de la misma unidad y identico entre
`intro`/`guided`/el terminal de un mismo concepto; `pathOrder` secuencial
0..n-1 por unidad; sin tildes en español (`LC_ALL=C grep -nP
"[\x80-\xFF]"`); sin voseo; para `fill_blank` `solo`/`practice`, la
respuesta no puede exigir recordar un identificador que el `intro`/
`guided` del mismo concepto nunca mostro (regla nueva de la sesion
anterior, ver `ContentCorpusLadderConsistencyTest`). **Mas la nueva regla
de este ciclo: distractores balanceados en largo/detalle contra la
respuesta correcta** (ver arriba) — se autoescribe cada ejercicio con
esto en mente desde el principio, no como pasada de correccion posterior.
`predict_output` no se usa para texto de excepcion/mensaje de error
exacto (coincidencia exacta no generaliza bien como pregunta); los 3
escenarios de excepcion de este sub-ciclo (`InvalidClassException` x2,
comportamiento de EOF) se enseñan como `mcq`, citando el texto real del
JDK solo dentro de `explanation`.

## QA

Igual que los ciclos anteriores: revision SDD completa (implementer +
reviewer por tarea, revision final opus de toda la rama), merge a main
local, y QA manual en dispositivo jugando ambas unidades completas antes
de dar el ciclo por cerrado. `adb input tap` sigue reservado solo para
navegacion/capturas de pantalla, nunca para responder ejercicios.
