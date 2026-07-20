package com.zconte.oopsapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
        modifier = Modifier.drawWithContent {
            // Surface draws its own background after this modifier in the chain, so a plain
            // drawBehind line at y=0 would be painted first and then fully covered by that
            // background fill. Drawing after drawContent() ensures the border renders on top.
            drawContent()
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
            horizontalArrangement = Arrangement.SpaceEvenly
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
