# I/O y NIO.2 — sub-ciclo 2: NIO.2 (Path + Files) — Diseño

**Estado:** Aprobado, pendiente de plan de implementacion.

## Contexto

Segundo y ultimo sub-ciclo de la seccion "I/O y NIO.2". El sub-ciclo 1
(Streams clasicos + Serializacion, `java.io`) ya esta mergeado, QA'd y
pusheado (`d22ee54`), junto con dos fixes de motor que salieron de su QA
en dispositivo (routing de checkpoint bloqueado `c0425ce`, auto-extension
de sesion por unidad `0d5863f`). El ADR
(`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md:139-141`) fija el
alcance de este sub-ciclo:

> - NIO.2 (`java.nio.file`): construccion/traversal de `Path` (`Path.of()`,
>   `Paths.get()`); comparar/normalizar/resolver/relativizar;
>   existencia/tamaño/atributos; operaciones de archivo via `Files`
>   (copy/move/delete, `StandardCopyOption`)

"Traversal de Path" se interpreta aqui como navegar los elementos de una
ruta (`getParent()`, `iterator()`, `subpath()`, etc.), no como recorrer un
arbol de directorios en el filesystem (`Files.walk()`) — ver Fuera de
alcance.

Este sub-ciclo cierra la seccion completa "I/O y NIO.2" (7ma seccion del
roadmap corregido). Aplica la misma regla de distractores balanceados en
largo/detalle establecida en el sub-ciclo 1
([[feedback_mcq_distractor_length_balance]]), sin cambios.

## Verificacion tecnica previa (JDK 20 real, scratch project)

Igual que el sub-ciclo 1, no se confio en memoria para comportamiento
runtime-observable: se escribio y ejecuto un programa Java real contra
OpenJDK 20 antes de fijar el diseño. Hechos confirmados:

1. **`Path.of()`/`Paths.get()` nunca tocan el filesystem al construir un
   `Path`.** Confirmado: `Path.of("/no/such/path/at/all")` no lanza
   excepcion aunque la ruta no exista — un `Path` es una representacion
   sintactica de una ruta, no una prueba de que el archivo exista.
2. **`relativize()` lanza `IllegalArgumentException` si un `Path` es
   absoluto y el otro es relativo.** Confirmado con `Path.of("/a/b")` (
   absoluto) y `Path.of("c/d")` (relativo): `IllegalArgumentException`,
   mensaje real del JDK `"'other' is different type of Path"`. Ambos
   deben ser del mismo "tipo" (ambos absolutos o ambos relativos).
3. **`resolve()` con un argumento absoluto ignora la ruta base y devuelve
   el argumento tal cual.** Confirmado: `Path.of("/a/b").resolve(Path.of(
   "/x/y"))` devolvio `/x/y`, no una concatenacion.
4. **`normalize()` colapsa elementos redundantes `.`/`..` sin tocar el
   filesystem** (no verifica que los directorios intermedios existan).
   Confirmado: `Path.of("/a/b/../c/./d").normalize()` devolvio `/a/c/d`.
5. **`Files.exists()`/`Files.notExists()` nunca lanzan excepcion**, solo
   devuelven `boolean`. Confirmado sobre una ruta inexistente: `exists` =
   `false`, `notExists` = `true` — ninguno lanzo `IOException`.
6. **`Files.delete()` sobre un archivo inexistente lanza
   `NoSuchFileException`**, mientras que **`Files.deleteIfExists()` sobre
   el mismo caso devuelve `false` sin lanzar nada.** Ambos confirmados por
   separado sobre la misma ruta inexistente — contraste directo,
   deliberado, entre las dos APIs.
7. **`Files.copy()`/`Files.move()` lanzan `FileAlreadyExistsException` si
   el destino ya existe y no se especifica ninguna `CopyOption`.**
   Confirmado para ambos metodos por separado (dos archivos temporales
   reales, destino pre-existente).
8. **`Files.copy(..., StandardCopyOption.REPLACE_EXISTING)` resuelve el
   caso anterior sin error** y sobrescribe el contenido del destino.
   Confirmado: la copia con la opcion tuvo exito y el contenido leido del
   destino coincidio con el del origen.
9. **Traversal de `Path`**: sobre `Path.of("/a/b/c/d")`, confirmado:
   `getNameCount()` = 4, `getName(0)` = `a` (los indices de `getName()`
   son relativos a los elementos del nombre, el root no cuenta como
   elemento), `subpath(1, 3)` = `b/c` (rango semi-abierto, igual que
   `List.subList`), `getRoot()` = `/`, `getFileName()` = `d`,
   `getParent()` = `/a/b/c`.
10. **Un `Path` relativo tiene `getRoot()` nulo.** Confirmado:
    `Path.of("a/b").getRoot()` devolvio `null` — forma directa de
    distinguir un `Path` relativo de uno absoluto en codigo, sin parsear
    el string.

Todos los hechos anteriores fueron re-verificados ejecutando codigo real
(no citados de memoria), y se re-verificaran de nuevo al momento de
autoria de cada ejercicio.

## Diseño de unidades

### Unidad 1: "Path basico"

`unitId: nio2-path-basico`, `orderIndex: 3`, `certObjective: io-nio2`.

3 conceptos, ~9 ejercicios:

1. **`path-construccion-traversal`** — `Path.of()` y `Paths.get()` son
   equivalentes (ambos construyen un `Path`, ninguno toca el filesystem,
   hecho #1); metodos de traversal sobre un `Path` ya construido:
   `getFileName()`, `getParent()`, `getRoot()`, `getNameCount()`,
   `getName(indice)`, `subpath(inicio, fin)`, `iterator()` (hecho #9). El
   contraste `getRoot()` nulo en un `Path` relativo (hecho #10) se enseña
   aqui como forma idiomatica de distinguir absoluto vs relativo.
2. **`path-comparacion-normalizacion`** — la comparacion de `Path`
   (`equals()`, `compareTo()`) es puramente sintactica sobre el string de
   la ruta, no verifica que el archivo exista ni resuelve symlinks;
   `normalize()` colapsa elementos `.`/`..` redundantes sin tocar el
   filesystem (hecho #4) — contraste explicito con "normalizar" en el
   sentido de resolver una ruta real, que NIO.2 no hace en este metodo.
3. **`path-resolve-relativize`** — `resolve()` combina una ruta base con
   otra, pero si el argumento es absoluto ese argumento gana por completo
   (hecho #3) — trampa clasica, contraintuitiva para quien espera una
   concatenacion tipo string; `relativize()` calcula la ruta relativa
   entre dos `Path`, pero exige que ambos sean del mismo tipo (ambos
   absolutos o ambos relativos) o lanza `IllegalArgumentException` (hecho
   #2).

### Unidad 2: "Files y operaciones"

`unitId: nio2-files-operaciones`, `orderIndex: 4`, `certObjective:
io-nio2`, `dependsOn` la unidad 1 completa (misma convencion que
`io-serializacion` dependiendo de `io-streams-clasicos` en el sub-ciclo
1 — `Files` opera sobre los `Path` que la unidad 1 enseña a construir).

3 conceptos, ~9 ejercicios:

1. **`files-existencia-atributos`** — `Files.exists()`/`Files.notExists()`
   nunca lanzan excepcion, solo devuelven `boolean` (hecho #5) —
   contraste implicito con `File.exists()` (API antigua, tampoco lanza,
   pero se enfatiza aqui porque el examen suele probar si el alumno cree
   que puede fallar); `Files.isDirectory()`/`Files.isRegularFile()`;
   `Files.size()` devuelve bytes como `long`.
2. **`files-copy-move`** — `Files.copy(origen, destino)` y
   `Files.move(origen, destino)` lanzan `FileAlreadyExistsException` si el
   destino ya existe y no se especifica ninguna opcion (hecho #7) — mismo
   patron recurrente de "falla en tiempo de ejecucion, no de compilacion"
   ya visto en Modulos y Empaquetado y en `io-serializacion`.
3. **`files-standardcopyoption-delete`** —
   `StandardCopyOption.REPLACE_EXISTING` habilita sobrescribir sin error
   (hecho #8), resolviendo directamente el caso del concepto anterior;
   `Files.delete()` lanza `NoSuchFileException` sobre un archivo
   inexistente, mientras `Files.deleteIfExists()` sobre el mismo caso
   devuelve `false` sin lanzar (hecho #6) — mismo patron de contraste ya
   usado en `char-streams-basicos` (`BufferedReader.readLine()` devuelve
   `null` en EOF vs `InputStream.read()` devuelve `-1`).

**Total sub-ciclo 2:** 6 conceptos, ~18 ejercicios, 2 unidades — mismo
tamaño que el sub-ciclo 1, y con eso la seccion "I/O y NIO.2" completa
queda en 4 unidades / 12 conceptos / ~36 ejercicios.

## Fuera de alcance (deliberado)

- **`Files.walk()`/`Files.list()`/`DirectoryStream`** (recorrido de arbol
  de directorios): el ADR usa "traversal" en el sentido de navegar los
  elementos sintacticos de un `Path` ya construido (hecho #9), no de
  recorrer el filesystem real. Fuera del alcance declarado.
- **`WatchService`** (monitoreo de cambios en el filesystem): no
  mencionado en el ADR, uso real de bajo nivel poco frecuente en el
  examen.
- **`FileSystem`/`FileSystems`** (acceso a filesystems alternativos,
  zip como filesystem, etc.): API avanzada, no mencionada en el ADR.
- **`BasicFileAttributes`/`Files.readAttributes()` detallado** (creacion,
  ultimo acceso, permisos POSIX, etc.): el ADR dice "existencia/tamaño/
  atributos" en terminos generales; se cubre con `exists`/`size`/
  `isDirectory`/`isRegularFile`, que son los atributos efectivamente
  preguntados en el examen 1Z0-830. Atributos POSIX especificos quedan
  fuera.
- **`Files.createFile()`/`createDirectory()`/`createTempFile()`**: el ADR
  no menciona creacion de archivos/directorios como objetivo, solo
  copy/move/delete. Se usan internamente solo como fixtures de
  verificacion tecnica (este documento), no como contenido enseñado.
- **`ATOMIC_MOVE`/`COPY_ATTRIBUTES`** (otras constantes de
  `StandardCopyOption` mas alla de `REPLACE_EXISTING`): el ADR dice
  "StandardCopyOption" en general, pero `REPLACE_EXISTING` es la unica
  con presencia real consistente en el examen; las otras dos quedan fuera
  para no diluir el concepto con 3 constantes de peso desigual.

## Cambios de codigo requeridos

Mismo archivo del sub-ciclo 1: se agregan las 2 unidades nuevas a
`app/src/main/assets/content/io-nio2.json` (la seccion `java-io-nio2` ya
esta registrada en `ContentPackRegistry.assetPaths`, no requiere linea
nueva). Bump de `CURRENT_CONTENT_VERSION` (22 → 23).

## Reglas estandar aplicables (recordatorio, sin cambios respecto al sub-ciclo 1)

Las reglas duras de siempre: sin colision de mayusculas entre
distractores y respuesta; un solo ejercicio solo/practice terminal por
concepto; `dependsOn` solo dentro de la misma unidad y identico entre
`intro`/`guided`/el terminal de un mismo concepto; `pathOrder` secuencial
0..n-1 por unidad; sin tildes en español (`LC_ALL=C grep -nP
"[\x80-\xFF]"`); sin voseo; para `fill_blank` `solo`/`practice`, la
respuesta no puede exigir recordar un identificador que el `intro`/
`guided` del mismo concepto nunca mostro. Distractores de `mcq`
balanceados en largo/detalle contra la respuesta correcta
([[feedback_mcq_distractor_length_balance]]) — se autoescribe cada
ejercicio con esto en mente desde el principio. `predict_output` no se
usa para texto de excepcion/mensaje de error exacto; los escenarios de
excepcion de este sub-ciclo (`IllegalArgumentException` de `relativize()`,
`FileAlreadyExistsException` x2, `NoSuchFileException`) se enseñan como
`mcq`, citando el texto real del JDK solo dentro de `explanation`.

## QA

Igual que los ciclos anteriores: revision SDD completa (implementer +
reviewer por tarea, revision final opus de toda la rama), merge a main
local, y QA manual en dispositivo jugando ambas unidades completas antes
de dar el ciclo (intentar en forma automatizada de ser posible) — y con el, 
la seccion completa "I/O y NIO.2" — por
cerrado. `adb input tap` sigue reservado solo para navegacion/capturas de
pantalla, nunca para responder ejercicios.

## Extra notes
Cuidado con los subagentes y con el agente mismo: si se usa worktree, no committear a main. 
Usar TDD siempre que sea posible.
Clean code, SOLID, DRY, aplicar patrones de diseño cuando sea posible. 
Si se observan malas prácticas heredadas, no repetirlas y anotar como deuda técnica.