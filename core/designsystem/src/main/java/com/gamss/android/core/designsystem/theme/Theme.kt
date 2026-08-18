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
    // 디자인 시스템에 primary 토큰이 없어 Primary 버튼과 같은 gray950 을 쓴다.
    // CircularProgressIndicator 등 Material 기본값이 이 색을 참조한다.
    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            background = colors.gray025,
            primary = colors.gray950,
            onPrimary = colors.gray025,
        )
    } else {
        lightColorScheme(
            background = colors.gray025,
            primary = colors.gray950,
            onPrimary = colors.gray025,
        )
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
