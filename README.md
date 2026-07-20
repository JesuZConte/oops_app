# OOPs!

App de Android para estudiar Java a diario con repetición espaciada (SM-2), pensada para preparar la certificación **Oracle Certified Professional: Java SE 21 Developer (1Z0-830)** — y para mantener el dominio de Java al día más allá del examen.

El nombre juega con dos sentidos: **OOP** (Object-Oriented Programming, el paradigma central de Java) y **oops** (el momento de fallar un ejercicio y que el sistema te lo vuelva a agendar mañana — la mecánica misma de la repetición espaciada).

## Qué hace

- Sesiones diarias cortas, con ejercicios que vencen según el algoritmo SM-2 (repetición espaciada clásica).
- Racha, XP y progreso por dominio del temario del examen, 100% local (sin red, sin backend).
- Tema visual "Arcade Neón-Pixel": claro y oscuro, con estética retro/pixel.

## Stack

- Kotlin + Jetpack Compose + Navigation Compose
- Room (persistencia local) + Hilt (DI)
- kotlinx.serialization (contenido de ejercicios en JSON, `assets/content/`)
- JUnit para el motor SRS y los use cases (Kotlin puro, sin dependencias de Android)

## Construir y correr

```bash
./gradlew :app:installDebug   # build + instalar en un dispositivo/emulador conectado
./gradlew test                # tests unitarios
./gradlew :app:assembleRelease
```

`minSdk = 26`, `compileSdk = 36`.

## Documentación

- **[`docs/specs/PROJECT-OOPS.md`](docs/specs/PROJECT-OOPS.md)** — fuente de verdad del proyecto: visión, arquitectura, modelo de datos, motor SM-2 y estado actual.
- **[`docs/CHANGELOG.md`](docs/CHANGELOG.md)** — historial de cambios por ronda de trabajo.
- **`docs/adrs/`** — decisiones de arquitectura y producto.
- **`docs/specs/`** — specs de diseño técnico previos a cada plan de implementación.
