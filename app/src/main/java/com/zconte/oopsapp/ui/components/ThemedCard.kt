package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zconte.oopsapp.ui.theme.OopsTheme

@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val extended = OopsTheme.extendedColors
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
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
            .then(
                if (extended.isDark) {
                    Modifier.shadow(elevation = 12.dp, shape = shape, ambientColor = accentColor, spotColor = accentColor)
                } else {
                    Modifier
                }
            )
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(
                width = if (extended.isDark) 1.dp else 2.dp,
                color = if (extended.isDark) MaterialTheme.colorScheme.outline else extended.hardShadowColor,
                shape = shape
            )
            .padding(16.dp),
        content = content
    )
}