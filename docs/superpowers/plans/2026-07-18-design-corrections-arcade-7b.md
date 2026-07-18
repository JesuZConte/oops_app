# Correcciones de diseño — Arcade 7b Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the app's Compose UI in line with the `docs/design/design_handoff_oops_arcade/` handoff (sections 7a/7b, definitive), fixing brand wordmark styling, per-card colored shadows, the bottom nav's visual language, and copy/layout gaps on Home/Ruta/Ajustes.

**Architecture:** No new screens, no navigation changes, no Room/data-model changes. All 7 tasks are targeted edits to existing Compose files plus one new file for bottom-nav icons. Every visual value (hex, dp, alpha) is copied verbatim from `docs/design/design_handoff_oops_arcade/README.md` and `Oops Design Directions.dc.html` sections 7a/7b.

**Tech Stack:** Kotlin, Jetpack Compose, Material3. No new dependencies.

## Global Constraints

- Brand name is `OOPs!` everywhere (already correct outside Home; Home's title needs the "!" split into `secondary` color).
- Bottom nav position is **7b (bottom, arcade style)**. Do not build a top nav bar. It was explicitly discarded.
- Per-tab colors: Home = `secondary`, Ruta = `primary`, Ajustes = `tertiary`.
- Light-mode card/pill shadows: hard offset `4dp,4dp,0` in the card/tab's accent color, `2dp` ink (`extended.hardShadowColor` / `#2A2632`) border. Dark-mode: `Modifier.shadow` glow tinted with the accent color, `1dp` `colorScheme.outline` border. This pattern already exists in `ThemedCard` — do not duplicate it elsewhere; reuse `ThemedCard` or replicate its exact `drawBehind`/`shadow` structure for the bottom-nav pill.
- Per-card accent-color assignment (some cards use a *different* role in light vs. dark — copy verbatim, do not assume symmetry):
  - Home STREAK card: `secondary` (light) / `tertiary` (dark).
  - Home XP card: new `PaperAccentAmber` `#F7C331` (light) / `primary` (dark).
  - Home TU RUTA card: `primary` (light and dark — symmetric).
  - Ajustes TEMA card: `secondary` (light and dark — symmetric).
  - Ajustes VERSIÓN card: new `PaperAccentAmber` `#F7C331` (light) / `tertiary` (dark).
- **Resolved scope decision (user, 2026-07-18):** three fields the new mockup implies (`streakRecord`, `xpToday`, per-domain `currentStep`) do **not** exist in the data model and were previously deferred. For this pass, use **static/proxy values** — do not add Room fields, migrations, or new repository methods:
  - Streak subtitle shows the literal static text `"días seguidos · récord 12"`.
  - The functional cup's fill keeps using the existing level-progress proxy (`(uiState.xp % 100) / 100f`) — no change to what drives the fill.
  - Ruta's current-step chip shows the literal static text `"collect() — ahora ▶"`, attached only to the one currently-unlocked line (Streams).
- **Resolved scope decision (user, 2026-07-18):** the functional cup keeps its existing `JavaCupOutline`/`JavaCupFill` coffee-mug palette (terracotta/coffee-brown). This is a deliberate, user-approved deviation from the handoff's ink/amber+tertiary cup mockup — do **not** change `FunctionalCup.kt`'s colors in this plan.
- Use only hex values that appear in `docs/design/design_handoff_oops_arcade/README.md` or are already defined in `Color.kt`. Do not invent new colors.

---

### Task 1: Add the missing "accent amarillo" light-only token

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt`

**Interfaces:**
- Produces: `PaperAccentAmber: Color` — consumed by Task 3 (Home XP card) and Task 7 (Ajustes VERSIÓN card).

- [ ] **Step 1: Add the constant**

Open `app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt`. After the `PaperLockedBorder` line (currently line 30), add:

```kotlin
// Light-only accent used for XP/VERSIÓN card shadows (no dark equivalent — those cards use primary/tertiary in dark)
val PaperAccentAmber = Color(0xFFF7C331)
```

- [ ] **Step 2: Build to confirm no syntax errors**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt
git commit -m "Add PaperAccentAmber token for XP/VERSIÓN card shadows"
```

---

### Task 2: Make ThemedCard's light-mode shadow use the card's accent color

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/components/ThemedCard.kt`

**Interfaces:**
- Consumes: nothing new (existing `accentColor: Color` parameter, currently only wired into the dark-mode glow).
- Produces: light-mode `drawBehind` shadow now uses `accentColor` instead of the fixed ink shadow — every existing and future `ThemedCard` call site's `accentColor` argument now controls both themes' shadow color. Tasks 3 and 7 rely on this.

**Context:** Today `ThemedCard`'s light-mode `drawBehind` block hardcodes `color = extended.hardShadowColor` (ink), ignoring the `accentColor` parameter entirely — only the dark-mode `Modifier.shadow(ambientColor = accentColor, spotColor = accentColor)` uses it. This is the one-line root cause of design-brief item 7 ("sombras en claro: de color por tarjeta, no todas negras"). The border stays ink/outline regardless of theme — do not touch the `.border(...)` block.

- [ ] **Step 1: Change the light-mode shadow color**

In `app/src/main/java/com/zconte/oopsapp/ui/components/ThemedCard.kt`, find:

```kotlin
                .drawBehind {
                    if (!extended.isDark) {
                        val offsetPx = 4.dp.toPx()
                        drawRoundRect(
                            color = extended.hardShadowColor,
                            topLeft = Offset(offsetPx, offsetPx),
                            size = size,
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    }
                }
```

Replace `color = extended.hardShadowColor` with `color = accentColor`:

```kotlin
                .drawBehind {
                    if (!extended.isDark) {
                        val offsetPx = 4.dp.toPx()
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(offsetPx, offsetPx),
                            size = size,
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    }
                }
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Visual sanity check**

Every existing `ThemedCard` call site keeps compiling with its current `accentColor` argument — no call-site changes needed in this task (Tasks 3 and 7 will adjust the *values* passed, not the mechanism). Confirm by searching call sites:

Run: `grep -rn "ThemedCard(" app/src/main/java/com/zconte/oopsapp/ui/`
Expected: `HomeScreen.kt` (3 call sites) and `SettingsScreen.kt` (2 call sites) — no compile errors reported by Step 2.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/components/ThemedCard.kt
git commit -m "ThemedCard: light-mode shadow follows accentColor instead of fixed ink"
```

---

### Task 3: Home screen corrections (wordmark, spectrum stripe, card accents, TU RUTA layout, Ver ruta button, streak subtitle)

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `PaperAccentAmber` (Task 1), `ThemedCard(accentColor, ...)` now honoring light-mode color (Task 2), `OopsTheme.extendedColors.isDark` (existing, from `ExtendedColors.kt`).
- Produces: no new public interfaces — this is a leaf screen.

**Context:** Read the full current file first — this task replaces most of it. Current `HomeScreen.kt` (148 lines): title is a single `Text("OOPs!", ...)`; STREAK card uses `accentColor = MaterialTheme.colorScheme.tertiary` (wrong in both themes per Global Constraints); XP card uses `accentColor = MaterialTheme.colorScheme.primary` (wrong for light); the XP bar fill (`LinearProgressIndicator(color = MaterialTheme.colorScheme.primary)`) is already correct — do not touch it; TU RUTA card is a single `Text("Streams · ${pct}% · Continuar")` line; there is no spectrum stripe; "Ver ruta" is a plain `OutlinedButton` (defaults to primary-colored text).

- [ ] **Step 1: Add the spectrum stripe gradient colors to Color.kt**

Open `app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt`. After the `PaperAccentAmber` line added in Task 1, add:

```kotlin
// Spectrum stripe under the Home wordmark, light-only
val SpectrumStripeColors = listOf(
    Color(0xFFF5533D),
    Color(0xFFF59410),
    Color(0xFFF7C331),
    Color(0xFF4FB03A),
    Color(0xFF2AA5B0),
    Color(0xFF3A72D6),
    Color(0xFFE5427E)
)
```

- [ ] **Step 2: Update imports in HomeScreen.kt**

Open `app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt`. Add these imports (alongside the existing ones):

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Brush
import com.zconte.oopsapp.ui.theme.OopsTheme
import com.zconte.oopsapp.ui.theme.PaperAccentAmber
import com.zconte.oopsapp.ui.theme.SpectrumStripeColors
```

(`ButtonDefaults` is already imported — do not duplicate the import; check before adding.)

- [ ] **Step 3: Replace the title Row with the split wordmark + spectrum stripe**

Find:

```kotlin
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "OOPs!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            LanguageEmblem()
        }
```

Replace with:

```kotlin
        val isDark = OopsTheme.extendedColors.isDark

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row {
                Text(
                    text = "OOPs",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.weight(1f))
            LanguageEmblem()
        }

        if (!isDark) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        brush = Brush.horizontalGradient(SpectrumStripeColors),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
```

Add `import androidx.compose.foundation.layout.Box` and `import androidx.compose.foundation.layout.height` if not already present (`height` is already imported; `Box` is not — add it).

- [ ] **Step 4: Fix the STREAK card's accent color and subtitle**

Find:

```kotlin
        ThemedCard(accentColor = MaterialTheme.colorScheme.tertiary) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FunctionalCup(xpLevelFraction = levelFraction, streakDays = uiState.streak)
                Column {
                    Text(
                        text = "STREAK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.streak.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PressStart2P, fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "días seguidos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
```

Replace with:

```kotlin
        ThemedCard(
            accentColor = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FunctionalCup(xpLevelFraction = levelFraction, streakDays = uiState.streak)
                Column {
                    Text(
                        text = "STREAK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.streak.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PressStart2P, fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "días seguidos · récord 12",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
```

(The `"récord 12"` value is a static placeholder per this plan's Global Constraints — there is no real streak-record tracking yet.)

- [ ] **Step 5: Fix the XP card's accent color**

Find:

```kotlin
        ThemedCard(accentColor = MaterialTheme.colorScheme.primary) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
```

Replace the `ThemedCard(...)` line with:

```kotlin
        ThemedCard(
            accentColor = if (isDark) MaterialTheme.colorScheme.primary else PaperAccentAmber
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
```

Do not touch the `LinearProgressIndicator` below it — its `color = MaterialTheme.colorScheme.primary` fill is already correct in both themes per the brief's item 4.

- [ ] **Step 6: Restructure the TU RUTA card**

Find:

```kotlin
        ThemedCard(
            modifier = Modifier.clickable(onClick = onProgressClick),
            accentColor = MaterialTheme.colorScheme.primary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TU RUTA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Streams · ${(uiState.streamsReadiness * 100).toInt()}% · Continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
```

Replace with:

```kotlin
        ThemedCard(
            modifier = Modifier.clickable(onClick = onProgressClick),
            accentColor = MaterialTheme.colorScheme.primary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TU RUTA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Streams",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(uiState.streamsReadiness * 100).toInt()}% ▶",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { uiState.streamsReadiness },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
```

(`accentColor = MaterialTheme.colorScheme.primary` here is unchanged — TU RUTA is `primary` in both light and dark per Global Constraints, so no `isDark` branch is needed for this card.)

- [ ] **Step 7: Fix the "Ver ruta" button's text color**

Find:

```kotlin
        OutlinedButton(
            onClick = onProgressClick,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Ver ruta", style = MaterialTheme.typography.bodyMedium)
        }
```

Replace with:

```kotlin
        OutlinedButton(
            onClick = onProgressClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Text("Ver ruta", style = MaterialTheme.typography.bodyMedium)
        }
```

- [ ] **Step 8: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Run the app and verify on-device or emulator**

Launch the app, toggle Ajustes → TEMA between Claro/Oscuro, and on Home confirm:
- "OOPs!" title has the "!" in a different (secondary) color from "OOPs".
- A thin 5px rainbow stripe appears under the title row only in light mode (absent in dark).
- STREAK card's shadow/glow uses secondary (light) / tertiary (dark).
- XP card's shadow/glow uses the amber `PaperAccentAmber` (light) / primary (dark).
- TU RUTA card shows "Streams" and "NN% ▶" on one row with a thin progress bar below.
- "Ver ruta" button's text is ink-colored, not blue.
- STREAK subtitle reads "días seguidos · récord 12".

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt app/src/main/java/com/zconte/oopsapp/ui/home/HomeScreen.kt
git commit -m "Home: split wordmark color, spectrum stripe, per-card accent colors, TU RUTA layout, Ver ruta button style"
```

---

### Task 4: Bottom-nav icon set (Home cup, Ruta metro-nodes, Ajustes gear)

**Files:**
- Create: `app/src/main/java/com/zconte/oopsapp/ui/components/NavIcons.kt`

**Interfaces:**
- Produces: `NavHomeIcon(tint: Color, modifier: Modifier = Modifier)`, `NavRouteIcon(tint: Color, modifier: Modifier = Modifier)`, `NavSettingsIcon(tint: Color, modifier: Modifier = Modifier)` — all three consumed by Task 5 (`OopsBottomBar.kt`).

**Context:** The handoff's HTML defines these as hand-drawn SVG line-art (not generic Material icons): a cup outline for Home, two connected dots ("metro map" style) for Ruta, a gear for Ajustes. This codebase already has a precedent for hand-drawn Canvas line-art icons — see `FunctionalCup.kt` (rounded-rect body + arc handle) and `LanguageEmblem.kt` (rounded-rect + arc). Follow that same `Canvas` + `drawRoundRect`/`drawArc`/`drawLine`/`drawCircle` style rather than attempting a literal SVG-path-to-`ImageVector` translation — it is more reliable to write and review, and matches the codebase's established icon-drawing pattern. Per the design brief's own note, the Home cup is explicitly a placeholder pending a real pixel-art sprite ("deja el icono/ilustración detrás de un recurso reemplazable") — these three composables are that replaceable resource.

- [ ] **Step 1: Write `NavIcons.kt`**

Create `app/src/main/java/com/zconte/oopsapp/ui/components/NavIcons.kt`:

```kotlin
package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun NavHomeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        val bodyWidth = size.width * 0.62f
        val bodyHeight = size.height * 0.68f
        val left = 0f
        val top = size.height * 0.14f

        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = stroke
        )
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(left + bodyWidth - 4.dp.toPx(), top + bodyHeight * 0.18f),
            size = Size(bodyWidth * 0.5f, bodyHeight * 0.55f),
            style = stroke
        )
    }
}

@Composable
fun NavRouteIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        val dotRadius = 2.dp.toPx()
        val topLeft = Offset(size.width * 0.25f, size.height * 0.2f)
        val bottomRight = Offset(size.width * 0.75f, size.height * 0.8f)

        drawCircle(color = tint, radius = dotRadius, center = topLeft)
        drawCircle(color = tint, radius = dotRadius, center = bottomRight)
        drawLine(color = tint, start = topLeft, end = Offset(topLeft.x, size.height * 0.5f), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(topLeft.x, size.height * 0.5f), end = Offset(bottomRight.x, size.height * 0.5f), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(bottomRight.x, size.height * 0.5f), end = bottomRight, strokeWidth = stroke.width, cap = stroke.cap)
    }
}

@Composable
fun NavSettingsIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        val gearRadius = size.minDimension * 0.16f
        val tickInner = size.minDimension * 0.34f
        val tickOuter = size.minDimension * 0.46f

        drawCircle(color = tint, radius = gearRadius, center = center, style = stroke)

        val angles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
        angles.forEach { degrees ->
            val radians = Math.toRadians(degrees)
            val dx = kotlin.math.cos(radians).toFloat()
            val dy = kotlin.math.sin(radians).toFloat()
            drawLine(
                color = tint,
                start = Offset(center.x + dx * tickInner, center.y + dy * tickInner),
                end = Offset(center.x + dx * tickOuter, center.y + dy * tickOuter),
                strokeWidth = stroke.width,
                cap = stroke.cap
            )
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/components/NavIcons.kt
git commit -m "Add hand-drawn nav icons: cup (Home), metro-nodes (Ruta), gear (Ajustes)"
```

---

### Task 5: Rewrite OopsBottomBar with arcade pill styling

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/navigation/OopsBottomBar.kt`

**Interfaces:**
- Consumes: `NavHomeIcon`, `NavRouteIcon`, `NavSettingsIcon` from Task 4; `OopsTheme.extendedColors` (`isDark`, `hardShadowColor`) from `ExtendedColors.kt`/`Theme.kt`; `OopsDestinations.HOME/PROGRESS/SETTINGS` (existing, from `OopsDestinations.kt` — do not change).
- Produces: `OopsBottomBar(navController: NavHostController, currentRoute: String?)` — same public signature as today, called from `MainActivity.kt`. Do not change the signature; `MainActivity.kt` needs no edits in this task.

**Context:** Current file uses default Material3 `NavigationBar`/`NavigationBarItem` with generic `Icons.Filled.*`. Target per handoff section 7b: no default `NavigationBar` chrome — a custom row of tabs; the **active** tab renders as a colored pill (per-tab color from Global Constraints) with white icon/label, `2dp` ink border + `4dp,4dp,0` hard shadow in light mode, or a colored glow in dark mode (no ink border, `Modifier.shadow` with `ambientColor`/`spotColor` = tab color, similar to `ThemedCard`'s dark-mode branch); the **inactive** tabs are transparent with ink icon/label (light) or `onSurface`-ish muted icon/label (dark). Labels use `PressStart2P`. Read `docs/design/design_handoff_oops_arcade/README.md` for the exact muted-label color role if unclear — but the following code uses ordinary `colorScheme` roles for inactive state, which is intended to be visually close without inventing new hex constants.

Two fidelity details that are easy to drop when replacing `NavigationBar` with a bare `Surface`, so they are called out explicitly and included in the code below: (1) `NavigationBar` applies the system navigation-bar inset internally — a plain `Surface`/`Row` does not, so `navigationBarsPadding()` must be added explicitly or the bar's content draws under the gesture/button area; (2) the handoff's 7b container spec includes a top border separating the bar from the screen content above it (`3dp` ink in light, `1dp` outline in dark) — add it via `drawBehind`/`drawLine` on the `Surface`, since `Surface` has no built-in top-border parameter.

- [ ] **Step 1: Rewrite the file**

Replace the full contents of `app/src/main/java/com/zconte/oopsapp/navigation/OopsBottomBar.kt` with:

```kotlin
package com.zconte.oopsapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.zconte.oopsapp.ui.components.NavHomeIcon
import com.zconte.oopsapp.ui.components.NavRouteIcon
import com.zconte.oopsapp.ui.components.NavSettingsIcon
import com.zconte.oopsapp.ui.theme.OopsTheme
import com.zconte.oopsapp.ui.theme.PressStart2P

private data class BottomBarDestination(
    val route: String,
    val label: String,
    val icon: @Composable (tint: Color) -> Unit,
    val accentColor: @Composable () -> Color
)

@Composable
fun OopsBottomBar(navController: NavHostController, currentRoute: String?) {
    val destinations = listOf(
        BottomBarDestination(
            route = OopsDestinations.HOME,
            label = "Home",
            icon = { tint -> NavHomeIcon(tint = tint) },
            accentColor = { MaterialTheme.colorScheme.secondary }
        ),
        BottomBarDestination(
            route = OopsDestinations.PROGRESS,
            label = "Ruta",
            icon = { tint -> NavRouteIcon(tint = tint) },
            accentColor = { MaterialTheme.colorScheme.primary }
        ),
        BottomBarDestination(
            route = OopsDestinations.SETTINGS,
            label = "Ajustes",
            icon = { tint -> NavSettingsIcon(tint = tint) },
            accentColor = { MaterialTheme.colorScheme.tertiary }
        )
    )

    val extended = OopsTheme.extendedColors
    val topBorderColor = if (extended.isDark) MaterialTheme.colorScheme.outline else extended.hardShadowColor
    val topBorderWidth = if (extended.isDark) 1.dp else 3.dp

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.drawBehind {
            drawLine(
                color = topBorderColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = topBorderWidth.toPx()
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                BottomBarTab(
                    label = destination.label,
                    selected = selected,
                    accentColor = destination.accentColor(),
                    icon = destination.icon,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomBarTab(
    label: String,
    selected: Boolean,
    accentColor: Color,
    icon: @Composable (tint: Color) -> Unit,
    onClick: () -> Unit
) {
    val extended = OopsTheme.extendedColors
    val shape = RoundedCornerShape(12.dp)
    val iconColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val labelColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier
                        .drawBehind {
                            if (!extended.isDark) {
                                val offsetPx = 4.dp.toPx()
                                drawRoundRect(
                                    color = extended.hardShadowColor,
                                    topLeft = Offset(offsetPx, offsetPx),
                                    size = size,
                                    cornerRadius = CornerRadius(12.dp.toPx())
                                )
                            }
                        }
                        .then(
                            if (extended.isDark) {
                                Modifier.shadow(elevation = 10.dp, shape = shape, ambientColor = accentColor, spotColor = accentColor)
                            } else {
                                Modifier
                            }
                        )
                        .background(accentColor, shape)
                        .border(
                            width = if (extended.isDark) 0.dp else 2.dp,
                            color = extended.hardShadowColor,
                            shape = shape
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        icon(iconColor)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PressStart2P, fontSize = 8.sp),
            color = labelColor,
            maxLines = 1
        )
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `Arrangement`/`Column`/`Row` fully-qualified references cause ambiguity or unused-import warnings, resolve by adding direct imports (`androidx.compose.foundation.layout.Arrangement`) instead of the inline fully-qualified calls — either form is acceptable, prefer clean imports.

- [ ] **Step 3: Run the app and verify on-device or emulator**

Confirm: the bottom nav shows 3 tabs (Home/Ruta/Ajustes) with the custom cup/metro-nodes/gear icons (not generic Material icons); the active tab renders as a colored pill (magenta/Home, blue/Ruta, amber/Ajustes) with white icon+label in Press Start 2P; light mode shows a hard ink-bordered shadow on the active pill plus a top border separating the bar from content above, dark mode shows a colored glow with no ink border plus a subtle outline top border; inactive tabs are unstyled/transparent with ink/onSurface icons and muted onSurfaceVariant labels. Confirm the bar is not overlapped by the system gesture/button area at the bottom of the screen (the `navigationBarsPadding()` added in Step 1 should prevent this — if the bar still looks cramped against the system bar, that padding is missing or misapplied). Confirm no label ("Ajustes" is the longest) wraps to two lines or gets clipped inside its pill at `8.sp` Press Start 2P — if it does, reduce `fontSize` until all three labels fit on one line. Tap all three tabs and confirm navigation still works exactly as before (this task must not regress the earlier `popUpTo`/`launchSingleTop`/`restoreState` navigation fix — the dispatch logic is unchanged from the original file).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/navigation/OopsBottomBar.kt
git commit -m "Bottom nav: arcade pill styling, per-tab accent colors, custom icons, Press Start 2P labels"
```

---

### Task 6: Ruta current-step chip

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: existing `RouteLine` data class (add one field), `OopsTheme.extendedColors.isDark`.
- Produces: no new public interfaces — leaf screen.

**Context:** Per the resolved scope decision, this is a **static placeholder chip**, not real per-skill progress tracking — it appears only under the one currently-unlocked line (Streams), with the literal text `"collect() — ahora ▶"`, styled in that line's color (primary/blue, since Streams' `color` is already `MaterialTheme.colorScheme.primary`).

- [ ] **Step 1: Add the light-mode chip background color**

Open `app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt`. After the `SpectrumStripeColors` list added in Task 3, add:

```kotlin
// Ruta current-step chip background, light mode only (dark mode uses a low-alpha tint of the line's own color)
val RouteChipBackgroundLight = Color(0xFFE4EDFB)
```

- [ ] **Step 2: Add a `currentStepLabel` field to `RouteLine` and pass it for Streams**

Open `app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt`. Find:

```kotlin
private data class RouteLine(
    val label: String,
    val statusLine: String,
    val color: Color?,
    val locked: Boolean,
    val lockedHint: String? = null
)
```

Replace with:

```kotlin
private data class RouteLine(
    val label: String,
    val statusLine: String,
    val color: Color?,
    val locked: Boolean,
    val lockedHint: String? = null,
    val currentStepLabel: String? = null
)
```

Find the Streams entry in the `lines` list:

```kotlin
        RouteLine(
            label = "STREAMS · L1",
            statusLine = "${(streamsReadiness * 100).toInt()}% dominado",
            color = MaterialTheme.colorScheme.primary,
            locked = false
        ),
```

Replace with:

```kotlin
        RouteLine(
            label = "STREAMS · L1",
            statusLine = "${(streamsReadiness * 100).toInt()}% dominado",
            color = MaterialTheme.colorScheme.primary,
            locked = false,
            currentStepLabel = "collect() — ahora ▶"
        ),
```

(The other two entries — Collections, SQL/JDBC — are locked and get no `currentStepLabel`, so they default to `null`.)

- [ ] **Step 3: Render the chip in `RouteLineRow`**

Add these imports to `ProgressScreen.kt`:

```kotlin
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.zconte.oopsapp.ui.theme.RouteChipBackgroundLight
```

Find:

```kotlin
            if (line.locked && line.lockedHint != null) {
                Text(
                    text = "🔒 ${line.lockedHint}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.lockedText
                )
            }
        }
    }
}
```

Replace with:

```kotlin
            if (line.locked && line.lockedHint != null) {
                Text(
                    text = "🔒 ${line.lockedHint}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.lockedText
                )
            }
            if (!line.locked && line.currentStepLabel != null && line.color != null) {
                val chipShape = RoundedCornerShape(10.dp)
                Text(
                    text = line.currentStepLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (extended.isDark) line.color else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .background(
                            color = if (extended.isDark) line.color.copy(alpha = 0.14f) else RouteChipBackgroundLight,
                            shape = chipShape
                        )
                        .border(
                            width = if (extended.isDark) 1.dp else 2.dp,
                            color = line.color,
                            shape = chipShape
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run the app and verify on-device or emulator**

Open Ruta. Confirm the Streams line shows a "collect() — ahora ▶" chip below its status text, in blue (primary), with the light-mode pale-blue background + 2dp border or dark-mode low-alpha tint + 1dp border. Confirm the two locked lines show no chip (only their existing 🔒 hint).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/theme/Color.kt app/src/main/java/com/zconte/oopsapp/ui/progress/ProgressScreen.kt
git commit -m "Ruta: add static current-step chip for the in-progress domain"
```

---

### Task 7: Ajustes screen corrections (chunky radios, card accent colors)

**Files:**
- Modify: `app/src/main/java/com/zconte/oopsapp/ui/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `PaperAccentAmber` (Task 1), `ThemedCard` light-mode-accent fix (Task 2), `OopsTheme.extendedColors.isDark`.
- Produces: no new public interfaces — leaf screen.

**Context:** Current file uses a default Material3 `RadioButton` inside `ThemeOptionRow`, and both cards use the wrong `accentColor` per Global Constraints (TEMA currently `primary`, should be `secondary` both themes; VERSIÓN currently `tertiary`, should be `PaperAccentAmber` light / `tertiary` dark). The chunky radio per the handoff is a ring (ink border, or accent-colored + glow when selected) with an inner accent-colored dot when selected — not the default Material3 filled-circle radio.

- [ ] **Step 1: Update imports**

Open `app/src/main/java/com/zconte/oopsapp/ui/settings/SettingsScreen.kt`. Remove the `androidx.compose.material3.RadioButton` import and add:

```kotlin
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.zconte.oopsapp.ui.theme.OopsTheme
import com.zconte.oopsapp.ui.theme.PaperAccentAmber
```

(`padding` and `height`/`Modifier` are already imported — do not duplicate.)

- [ ] **Step 2: Fix the TEMA card's accent color**

Find:

```kotlin
        ThemedCard(accentColor = MaterialTheme.colorScheme.primary) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TEMA",
```

Replace the `ThemedCard(...)` line with:

```kotlin
        ThemedCard(accentColor = MaterialTheme.colorScheme.secondary) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TEMA",
```

(TEMA is `secondary` in both light and dark per Global Constraints — no `isDark` branch needed.)

- [ ] **Step 3: Fix the VERSIÓN card's accent color**

Find:

```kotlin
        ThemedCard(accentColor = MaterialTheme.colorScheme.tertiary) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "VERSIÓN",
```

Replace with:

```kotlin
        ThemedCard(
            accentColor = if (OopsTheme.extendedColors.isDark) MaterialTheme.colorScheme.tertiary else PaperAccentAmber
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "VERSIÓN",
```

- [ ] **Step 4: Replace `ThemeOptionRow`'s `RadioButton` with a chunky radio**

Find:

```kotlin
@Composable
private fun ThemeOptionRow(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selectedMode == mode, onClick = { onSelect(mode) })
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

Replace with:

```kotlin
@Composable
private fun ThemeOptionRow(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val selected = selectedMode == mode
    val extended = OopsTheme.extendedColors
    val accent = MaterialTheme.colorScheme.secondary
    val ringColor = if (selected) accent else extended.hardShadowColor.takeIf { !extended.isDark } ?: MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .then(
                    if (selected && extended.isDark) {
                        Modifier.shadow(elevation = 6.dp, shape = CircleShape, ambientColor = accent, spotColor = accent)
                    } else {
                        Modifier
                    }
                )
                .border(width = 2.dp, color = ringColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(accent, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

Add these imports if not already present: `androidx.compose.foundation.background`, `androidx.compose.foundation.layout.Box`, `androidx.compose.foundation.layout.Spacer`, `androidx.compose.foundation.layout.width`, `androidx.compose.ui.Alignment` (already imported — verify, do not duplicate).

- [ ] **Step 5: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Run the app and verify on-device or emulator**

Open Ajustes. Confirm: TEMA card's shadow/glow is magenta (secondary) in both themes; VERSIÓN card's shadow/glow is amber `PaperAccentAmber` (light) / tertiary-amber glow (dark); the three theme radios render as a ring with an accent-colored dot when selected (not the default Material3 filled-dot radio), and tapping each still switches the theme correctly.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zconte/oopsapp/ui/settings/SettingsScreen.kt
git commit -m "Ajustes: chunky radio buttons, fix TEMA/VERSIÓN card accent colors"
```

---

## Final Steps (after all 7 tasks)

1. Dispatch the final whole-branch code review (per subagent-driven-development) covering the full diff across all 7 tasks.
2. Update `docs/CHANGELOG.md` with a new dated section summarizing: brand wordmark split-color, bottom nav arcade restyle (custom icons, per-tab pill colors, Press Start 2P labels), per-card colored light-mode shadows (`ThemedCard` fix), Home TU RUTA restructure + spectrum stripe + "Ver ruta" button fix, Ruta current-step chip (static), Ajustes chunky radios + card accent fixes — and note the two resolved scope decisions (static/proxy values for récord/xpToday/currentStep; kept the Java-coffee cup palette as a deliberate deviation from the handoff).
3. Use `superpowers:finishing-a-development-branch` to merge/push per the user's choice.