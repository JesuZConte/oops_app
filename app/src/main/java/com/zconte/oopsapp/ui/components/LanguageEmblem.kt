package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LanguageEmblem(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(32.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                ),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        val cupWidth = size.width * 0.5f
        val cupHeight = size.height * 0.4f
        val left = (size.width - cupWidth) / 2f
        val top = size.height * 0.42f

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(cupWidth, cupHeight),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(left + cupWidth - 3.dp.toPx(), top + cupHeight * 0.15f),
            size = Size(cupWidth * 0.35f, cupHeight * 0.6f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}