# Modulos y Empaquetado — sub-ciclo 2: Servicios en JPMS + Migracion y compatibilidad — Diseño

**Estado:** Aprobado, pendiente de plan de implementacion.

## Contexto

Sub-ciclo 1 (`372526e`, merged y QA'd en dispositivo) cubrio la seccion
`java-modules-packaging` con 2 unidades: "Declaracion de modulos"
(sintaxis de `module-info.java`: `module`/`exports`/`requires`/
`exports...to`/`uses`/`provides...with` como declaracion) y "Compilacion
y ejecucion" (`--module-source-path`, JARs modulares, `-p`/`-m`).

El ADR original (`docs/adrs/2026-08-04-1z0-830-roadmap-correction.md:123-131`)
fija el alcance completo de la seccion en 4 bloques; sub-ciclo 1 cubrio
los primeros dos. Este sub-ciclo cubre los 2 restantes:

> - Servicios en JPMS: interfaces de servicio + providers; `uses` /
>   `provides ... with`
> - Migracion y compatibilidad: unnamed module, automatic modules, split
>   packages, acceso a APIs internas via reflection/`--add-exports`

El plan de sub-cycle 1 diferio explicitamente el patron runtime de
`ServiceLoader` (incluyendo `ServiceConfigurationError`) a este sub-ciclo,
y dejo una restriccion de nombre pendiente: la unidad de servicios debe
llamarse de forma que las 2 referencias ya existentes en el texto de
sub-ciclo 1 ("la unidad de Servicios en JPMS") sigan siendo correctas.
Este spec fija ese nombre.

## Verificacion tecnica previa (JDK 20 real, scratch project)

Antes de fijar el diseño se corrieron escenarios reales de compilacion y
ejecucion (mismo enfoque de sub-cycle 1, que advisor senalo como el
dominio de contenido de mayor riesgo del proyecto por ser
mayormente comportamiento de compilador/launcher, no semantica de
lenguaje razonable en abstracto). Hechos confirmados:

1. **`ServiceLoader.load(Class)`** descubre providers declarados via
   `provides ... with` en el module path; la iteracion es lazy.
2. **Un provider necesita constructor publico sin argumentos O un metodo
   estatico publico `provider()`** — ambos patrones funcionan
   (confirmado con dos providers reales, uno de cada tipo).
3. **Si el provider no tiene ninguno de los dos, es un error de
   COMPILACION**, no de runtime — `javac` lo rechaza al compilar la
   clausula `provides ... with` de `module-info.java`:
   `"the no arguments constructor of the service implementation is not
   public: <Clase>"`.
4. **Si el modulo consumidor omite `uses`, el codigo compila sin error**
   (el compilador no cruza `ServiceLoader.load()` contra las
   declaraciones `uses`) **pero falla en RUNTIME** con
   `ServiceConfigurationError`:
   `"<Servicio>: module <consumidor> does not declare `uses`"`.
   (Re-confirmado; ya se habia verificado en sub-cycle 1 para informar
   este sub-ciclo.)
5. **Codigo en el classpath (no en el module path) vive en el "unnamed
   module"**; un modulo nombrado no puede hacerle `requires` por nombre
   — `javac` falla con `"module not found: <nombre>"`.
6. **Un JAR plano (sin `module-info.class`) puesto en el module path se
   convierte en automatic module.** Su nombre sale del atributo
   `Automatic-Module-Name` del manifest si existe; si no, se deriva del
   nombre del archivo. Confirmado con un JAR real: `foo-bar-utils-2.5.jar`
   sin manifest especial → `jar --describe-module` reporta
   `foo.bar.utils@2.5 automatic` (guiones → puntos, sufijo de version
   separado). Con `Automatic-Module-Name: com.foobar.custom` en el
   manifest, el nombre derivado se ignora y se usa ese.
7. **Un automatic module exporta todos sus paquetes implicitamente** y
   puede ser `requires`do por nombre desde un modulo nombrado sin que el
   automatic module declare nada — confirmado compilando y ejecutando un
   modulo nombrado real que hace `requires foo.bar.utils` (renombrado via
   manifest) y usa una clase del JAR plano sin problema.
8. **Split packages entre modulos separados (no compilados juntos) fallan
   en RUNTIME**, no en compilacion — confirmado poniendo dos JARs
   automatic-module distintos con el mismo paquete Java (`com.foo.bar`)
   en el mismo module path y forzando su resolucion conjunta:
   `java.lang.module.ResolutionException: Module gadget.lib contains
   package com.foo.bar, module foo.bar.utils exports package com.foo.bar
   to gadget.lib`. **Contraste didactico deliberado** con el caso ya
   enseñado en sub-cycle 1 (split package dentro de un mismo
   `--module-source-path`, que falla en COMPILACION con un error de
   `javac`) — mismo problema conceptual, momento de falla distinto segun
   si los modulos se compilan juntos o se resuelven ya compilados.
9. **Acceso reflexivo profundo (`setAccessible(true)`) a un paquete no
   exportado falla por defecto** con `IllegalAccessException`:
   `"class <X> (in module <A>) cannot access class <Y> (in module <B>)
   because module <B> does not export <paquete> to module <A>"`.
   `--add-opens <modulo>/<paquete>=<modulo-destino>` en la linea de
   comandos de `java` (no de `javac`) resuelve el acceso reflexivo sin
   que el modulo origen necesite declarar `exports` ni `opens` — verificado
   ejecutando el mismo codigo con y sin la flag.

Todos estos hechos quedan re-verificados (no solo citados de memoria) al
momento de autoria de cada ejercicio, siguiendo la leccion estandar del
proyecto de construir y ejecutar un scratch project real contra un JDK
antes de escribir texto de ejercicio para contenido de este dominio.

## Diseño de unidades

### Unidad 1: "Servicios en JPMS"

`unitId: mod-servicios`, `orderIndex: 3` (sigue a `mod-compilacion`),
`certObjective: modules-packaging` (mismo valor que las 2 unidades de
sub-cycle 1 — confirmado en sub-cycle 1 que `certObjective` es un valor
por SECCION, no por unidad). Nombre elegido para que las referencias
textuales existentes ("la unidad de Servicios en JPMS") sigan siendo
correctas.

3 conceptos, ~9 ejercicios (misma proporcion que "Compilacion y
ejecucion": 3 conceptos × 3 ejercicios):

1. **`serviceloader-basic-loading`** — `ServiceLoader.load(Interfaz.class)`
   devuelve un `ServiceLoader<T>` iterable; cada iteracion instancia (de
   forma lazy) los providers declarados via `provides ... with` en algun
   modulo del module path que el consumidor puede ver.
2. **`provider-factory-method`** — un provider valido necesita constructor
   publico sin argumentos O metodo estatico publico `provider()` que
   retorne una instancia del servicio; si no tiene ninguno de los dos,
   `javac` rechaza la clausula `provides ... with` en tiempo de
   compilacion (hecho #3 arriba).
3. **`missing-uses-runtime-error`** — un modulo consumidor que omite
   `uses` compila sin problema pero `ServiceLoader.load()` lanza
   `ServiceConfigurationError` en runtime (hecho #4 arriba) — refuerza el
   contraste compilacion-vs-runtime que atraviesa varios conceptos de este
   sub-ciclo.

### Unidad 2: "Migracion y compatibilidad"

`unitId: mod-migracion`, `orderIndex: 4`, mismo `certObjective`.

4 conceptos, ~12 ejercicios:

1. **`unnamed-module-classpath`** — codigo en el classpath vive en el
   unnamed module; un modulo nombrado no puede hacerle `requires` por
   nombre (hecho #5).
2. **`automatic-modules-naming`** — un JAR plano en el module path se
   vuelve automatic module; nombre desde `Automatic-Module-Name` del
   manifest o derivado del nombre del archivo (hecho #6); exporta todo
   implicitamente y puede requerirse por nombre desde un modulo nombrado
   (hecho #7).
3. **`split-packages-migration`** — dos modulos separados (ya compilados,
   no compilados juntos) con el mismo paquete fallan en RUNTIME con
   `ResolutionException` (hecho #8) — contraste explicito con el caso de
   compilacion ya visto en sub-cycle 1.
4. **`add-opens-reflection`** — acceso reflexivo profundo a un paquete no
   exportado falla con `IllegalAccessException` por defecto;
   `--add-opens <modulo>/<paquete>=<destino>` lo habilita en runtime sin
   tocar `module-info.java` (hecho #9).

**Total sub-ciclo 2:** 7 conceptos, 21 ejercicios, 2 unidades — tamaño
comparable a sub-cycle 1 (8 conceptos, 24 ejercicios).

## Fuera de alcance (deliberado)

- `--illegal-access`: flag transicional de JDK 9-16, removido desde
  JDK 16; la app apunta a Java SE 21, se excluye por obsoleto.
- `--add-exports` como concepto separado de `--add-opens`: el ADR los
  menciona juntos ("reflection/`--add-exports`"); decision de este spec
  es que el concepto `add-opens-reflection` cubre el escenario de
  migracion mas comun y representativo (acceso reflexivo profundo desde
  codigo legado). `--add-exports` (acceso NO reflexivo, en tiempo de
  compilacion, a un paquete no exportado) queda fuera de este sub-ciclo;
  si se decide cubrirlo, es contenido para un ciclo futuro, no una
  adicion a evaluar durante el plan de este.
- `jlink`/custom runtime images: no mencionado en el ADR para esta
  seccion: no aplica a este sub-ciclo.

## Cambios de codigo requeridos

Ninguno nuevo mas alla de lo ya hecho en sub-cycle 1: la seccion
`java-modules-packaging` y su registro en `ContentPackRegistry.assetPaths`
ya existen. Este sub-ciclo solo agrega 2 unidades (via texto exacto, no
load+dump) al mismo archivo `modules-packaging.json`, y bump de
`CURRENT_CONTENT_VERSION` (18 → 19).

## Reglas estandar aplicables (recordatorio, no nuevas)

Las mismas reglas duras de todo el proyecto: sin colision de mayusculas
entre distractores y respuesta; un solo ejercicio solo/practice terminal
por concepto; `dependsOn` solo dentro de la misma unidad; `pathOrder`
secuencial 0..n-1 por unidad; sin tildes en español (`LC_ALL=C grep -nP
"[\x80-\xFF]"`); sin voseo. `predict_output` no se usa para texto de
error de compilador/JVM (coincidencia exacta no generaliza) — los 4
escenarios de error de este sub-ciclo (constructor no publico,
`ServiceConfigurationError`, `ResolutionException`,
`IllegalAccessException`) se enseñan como `mcq`, citando el texto real
del JDK solo dentro de `explanation`.

## QA

Igual que sub-cycle 1: revision SDD completa (implementer + reviewer por
tarea, revision final opus de toda la rama), merge a main local, y QA
manual en dispositivo jugando ambas unidades completas antes de dar el
ciclo por cerrado — la automatizacion via `adb input tap` demostro no ser
confiable para responder ejercicios en sub-cycle 1 (taps que no
registraban o caian en la opcion equivocada); se reserva `adb` solo para
navegacion/capturas de pantalla, y el QA de contestar ejercicios se hace
jugando manualmente.
