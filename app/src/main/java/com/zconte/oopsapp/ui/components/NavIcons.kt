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