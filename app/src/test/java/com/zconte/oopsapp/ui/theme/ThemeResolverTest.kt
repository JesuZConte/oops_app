package com.zconte.oopsapp.ui.theme

import com.zconte.oopsapp.domain.model.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {

    @Test
    fun `SYSTEM mode follows the system dark-theme flag when it is true`() {
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = true))
    }

    @Test
    fun `SYSTEM mode follows the system dark-theme flag when it is false`() {
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = false))
    }

    @Test
    fun `LIGHT mode is always false regardless of the system flag`() {
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = true))
    }

    @Test
    fun `DARK mode is always true regardless of the system flag`() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemInDarkTheme = false))
    }
}