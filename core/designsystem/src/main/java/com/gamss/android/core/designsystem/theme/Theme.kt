package com.gamss.android.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun GamssTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkGamssColors else LightGamssColors
    CompositionLocalProvider(LocalGamssColors provides colors) {
        content()
    }
}

object GamssTheme {
    val colors: GamssColors
        @Composable
        get() = LocalGamssColors.current

    val typography: GamssTypography
        get() = GamssTypography

    val spacing: GamssSpacing
        get() = GamssSpacing

    val radius: GamssRadius
        get() = GamssRadius
}