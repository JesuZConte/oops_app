package com.zconte.oopsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zconte.oopsapp.ui.theme.JetBrainsMono
import com.zconte.oopsapp.ui.theme.OopsTheme

private val CodeKeywords = setOf(
    "collect", "filter", "map", "forEach", "sorted", "limit", "skip", "reduce",
    "anyMatch", "allMatch", "noneMatch", "joining", "flatMap", "range", "min", "max",
    "groupingBy", "stream", "distinct", "count", "toList"
)
private val CodeTypes = setOf("List", "Stream", "IntStream", "Optional", "Collectors", "Comparator")
private const val BLANK_ID = "blank"

@Composable
fun CodeBlock(code: String, filledAnswer: String? = null, modifier: Modifier = Modifier) {
    val extended = OopsTheme.extendedColors
    val primary = MaterialTheme.colorScheme.primary

    val annotated = remember(code, extended, primary, filledAnswer) {
        highlightCode(code, extended, primary, filledAnswer)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(extended.codeBackground)
            .padding(16.dp)
    ) {
        Text(
            text = annotated,
            color = extended.codeText,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            lineHeight = 22.sp,
            inlineContent = mapOf(
                BLANK_ID to androidx.compose.foundation.text.InlineTextContent(
                    placeholder = Placeholder(
                        width = 4.em,
                        height = 1.1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .dashedBorder(width = 1.5.dp, color = primary, cornerRadius = 6.dp)
                            .background(primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    )
                }
            )
        )
    }
}

private fun highlightCode(
    code: String,
    extended: com.zconte.oopsapp.ui.theme.OopsExtendedColors,
    primary: Color,
    filledAnswer: String?
): AnnotatedString =
    buildAnnotatedString {
        val blankRegex = Regex("_{5,}")
        val tokenRegex = Regex("[A-Za-z]+")
        var index = 0
        while (index < code.length) {
            val blank = blankRegex.find(code, index)
            val token = tokenRegex.find(code, index)
            val next = sequenceOf(blank, token).filterNotNull().minByOrNull { it.range.first }
            if (next == null) {
                append(code.substring(index))
                break
            }
            if (next.range.first > index) {
                append(code.substring(index, next.range.first))
            }
            when {
                next === blank && filledAnswer != null -> {
                    withStyle(
                        SpanStyle(color = primary, background = primary.copy(alpha = 0.15f))
                    ) { append(filledAnswer) }
                }
                next === blank -> appendInlineContent(BLANK_ID, next.value)
                next.value in CodeKeywords -> withStyle(SpanStyle(color = extended.codeKeyword)) { append(next.value) }
                next.value in CodeTypes -> withStyle(SpanStyle(color = extended.codeType)) { append(next.value) }
                else -> append(next.value)
            }
            index = next.range.last + 1
        }
    }

private fun Modifier.dashedBorder(width: Dp, color: Color, cornerRadius: Dp): Modifier = drawWithContent {
    drawContent()
    drawRoundRect(
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}