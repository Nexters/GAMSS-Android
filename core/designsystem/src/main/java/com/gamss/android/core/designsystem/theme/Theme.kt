package com.gamss.android.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun GamssTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkGamssColors else LightGamssColors
    val materialColorScheme = if (darkTheme) {
        darkColorScheme(background = colors.gray025)
    } else {
        lightColorScheme(background = colors.gray025)
    }

    CompositionLocalProvider(LocalGamssColors provides colors) {
        MaterialTheme(colorScheme = materialColorScheme, content = content)
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

    val sticker: GamssStickerColors
        get() = GamssStickerColors
}
