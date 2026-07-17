package com.zconte.oopsapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

@Composable
fun FunctionalCup(
    xpLevelFraction: Float,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val fillColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline.let {
        if (it == androidx.compose.ui.graphics.Color.Unspecified) MaterialTheme.colorScheme.onSurface else it
    }
    val steamColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val animatedFill by animateFloatAsState(targetValue = xpLevelFraction.coerceIn(0f, 1f), label = "cupFill")
    val wisps = when {
        streakDays <= 0 -> 0
        streakDays < 3 -> 1
        streakDays < 7 -> 2
        else -> 3
    }
    val infiniteTransition = rememberInfiniteTransition(label = "steam")
    val steamPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "steamPhase"
    )

    Canvas(modifier = modifier.size(width = 56.dp, height = 52.dp)) {
        val cupWidth = size.width * 0.75f
        val cupHeight = size.height * 0.7f
        val cupTop = size.height - cupHeight

        clipRect(0f, cupTop, cupWidth, cupTop + cupHeight) {
            drawRect(
                color = fillColor,
                topLeft = Offset(0f, cupTop + cupHeight * (1f - animatedFill)),
                size = Size(cupWidth, cupHeight * animatedFill)
            )
        }
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(0f, cupTop),
            size = Size(cupWidth, cupHeight),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
        drawArc(
            color = outlineColor,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cupWidth - 6.dp.toPx(), cupTop + cupHeight * 0.2f),
            size = Size(18.dp.toPx(), cupHeight * 0.5f),
            style = Stroke(width = 3.dp.toPx())
        )
        repeat(wisps) { i ->
            val baseX = cupWidth * (0.25f + i * 0.25f)
            val phase = (steamPhase + i * 0.33f) % 1f
            val yOffset = -phase * 18.dp.toPx()
            val alpha = (1f - phase) * 0.5f
            val path = Path().apply {
                moveTo(baseX, cupTop + yOffset)
                cubicTo(
                    baseX - 4.dp.toPx(), cupTop + yOffset - 6.dp.toPx(),
                    baseX + 4.dp.toPx(), cupTop + yOffset - 12.dp.toPx(),
                    baseX, cupTop + yOffset - 18.dp.toPx()
                )
            }
            drawPath(path, color = steamColor.copy(alpha = alpha), style = Stroke(width = 2.dp.toPx()))
        }
    }
}