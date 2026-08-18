package com.gamss.android.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

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

    CompositionLocalProvider(
        LocalGamssColors provides colors,
        LocalIsGamssDarkTheme provides darkTheme,
    ) {
        MaterialTheme(colorScheme = materialColorScheme, content = content)
    }
}

/**
 * [isSystemInDarkTheme]는 [GamssTheme]의 [darkTheme] 오버라이드와 무관하게 실제 기기 설정을 그대로
 * 반환한다. `GamssTheme(darkTheme = false)`로 라이트를 강제한 화면에서도 컴포넌트가 직접
 * [isSystemInDarkTheme]를 부르면 그 강제가 씹힌다 — 다크/라이트로 갈리는 에셋(로고 등)을 고르는
 * 컴포넌트는 이 값 대신 이걸 읽어야 한다.
 */
private val LocalIsGamssDarkTheme = compositionLocalOf { false }

object GamssTheme {
    val colors: GamssColors
        @Composable
        get() = LocalGamssColors.current

    val isDarkTheme: Boolean
        @Composable
        get() = LocalIsGamssDarkTheme.current

    val typography: GamssTypography
        get() = GamssTypography

    val spacing: GamssSpacing
        get() = GamssSpacing

    val radius: GamssRadius
        get() = GamssRadius

    val sticker: GamssStickerColors
        get() = GamssStickerColors
}
