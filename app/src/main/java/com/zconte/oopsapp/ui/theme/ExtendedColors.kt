package com.zconte.oopsapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class OopsExtendedColors(
    val codeBackground: Color,
    val codeText: Color,
    val codeKeyword: Color,
    val codeType: Color,
    val success: Color,
    val lockedBackground: Color,
    val lockedBorder: Color,
    val lockedText: Color,
    val hardShadowColor: Color,
    val isDark: Boolean
)

val DarkExtendedColors = OopsExtendedColors(
    codeBackground = CodeBlockBackgroundDark,
    codeText = CodeBlockText,
    codeKeyword = CodeBlockKeyword,
    codeType = CodeBlockType,
    success = NeonSuccess,
    lockedBackground = NeonLocked,
    lockedBorder = NeonOutline,
    lockedText = NeonOnSurfaceMuted,
    hardShadowColor = Color.Transparent,
    isDark = true
)

val LightExtendedColors = OopsExtendedColors(
    codeBackground = CodeBlockBackgroundLight,
    codeText = CodeBlockText,
    codeKeyword = CodeBlockKeyword,
    codeType = CodeBlockType,
    success = PaperSuccess,
    lockedBackground = PaperLockedBackground,
    lockedBorder = PaperLockedBorder,
    lockedText = PaperOnSurfaceMuted,
    hardShadowColor = PaperInk,
    isDark = false
)

val LocalOopsExtendedColors = staticCompositionLocalOf { DarkExtendedColors }