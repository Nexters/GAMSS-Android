package com.gamss.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Base
internal val White = Color(0xFFFFFFFF)
internal val Black = Color(0xFF000000)
internal val Gray1000 = Color(0xFF1A1C20)

// Gray - Light
internal val GrayLight025 = Color(0xFFFDFEFF)
internal val GrayLight050 = Color(0xFFF7F9FB)
internal val GrayLight075 = Color(0xFFEFF2F6)
internal val GrayLight100 = Color(0xFFE7ECF1)
internal val GrayLight200 = Color(0xFFDCE0E6)
internal val GrayLight300 = Color(0xFFC2C7D1)
internal val GrayLight400 = Color(0xFFAAAFBD)
internal val GrayLight500 = Color(0xFF9094A3)
internal val GrayLight600 = Color(0xFF797C8B)
internal val GrayLight700 = Color(0xFF5A5D68)
internal val GrayLight800 = Color(0xFF474951)
internal val GrayLight900 = Color(0xFF303136)
internal val GrayLight950 = Color(0xFF1E1F22)

/**
 * 카드를 이미지로 내보낼 때 카드 바깥 여백에 깔 색.
 *
 * `GamssImageCard` 가 카드 안쪽을 [LightGamssColors] 로 고정하므로 이 색도 테마에 따라 반전하면
 * 안 된다. `GamssTheme.colors.gray950` 을 쓰면 다크에서 밝은 색으로 뒤집혀 밝은 카드 뒤에
 * 밝은 배경이 깔린다. 그래서 라이트 톤을 그대로 노출한다.
 */
val GamssCardExportBackground: Color = GrayLight950

// Gray - Dark
internal val GrayDark025 = Color(0xFF1E1F22)
internal val GrayDark050 = Color(0xFF24262B)
internal val GrayDark075 = Color(0xFF2A2D33)
internal val GrayDark100 = Color(0xFF30343B)
internal val GrayDark200 = Color(0xFF3B4048)
internal val GrayDark300 = Color(0xFF4B505B)
internal val GrayDark400 = Color(0xFF676C78)
internal val GrayDark500 = Color(0xFF8A909C)
internal val GrayDark600 = Color(0xFFB0B5C0)
internal val GrayDark700 = Color(0xFFCDD2DB)
internal val GrayDark800 = Color(0xFFE1E5EB)
internal val GrayDark900 = Color(0xFFF2F5F8)
internal val GrayDark950 = Color(0xFFFCFDFE)

// Chromatic - Light
internal val ChromaticLightRed = Color(0xFFEF3535)
internal val ChromaticLightApricot = Color(0xFFEC462B)
internal val ChromaticLightYellow = Color(0xFFF1B235)
internal val ChromaticLightGreen = Color(0xFF36AF48)
internal val ChromaticLightTeal = Color(0xFF36B8BD)
internal val ChromaticLightBlue = Color(0xFF376CE8)
internal val ChromaticLightPurple = Color(0xFFA425E3)

// Chromatic - Dark
internal val ChromaticDarkRed = Color(0xFFFF4444)
internal val ChromaticDarkApricot = Color(0xFFF15D47)
internal val ChromaticDarkYellow = Color(0xFFFFC552)
internal val ChromaticDarkGreen = Color(0xFF3BDB98)
internal val ChromaticDarkTeal = Color(0xFF44DFE5)
internal val ChromaticDarkBlue = Color(0xFF5588FF)
internal val ChromaticDarkPurple = Color(0xFFCC6DFC)

@Immutable
data class GamssColors(
    // 기기 다크 모드가 아니라 GamssTheme 이 고른 팔레트를 가리킨다. 셸이 라이트로 고정된 동안
    // isSystemInDarkTheme() 을 직접 보면 테마와 어긋나므로, 다크 분기는 이 값으로 판단한다.
    val isDark: Boolean,
    val white: Color,
    val black: Color,
    // Figma 의 Gray/Gray1000. 라이트와 다크가 같은 값이라 gray025~gray950 과 달리 반전하지 않는다.
    val gray1000: Color,
    // 화면 최하단 배경. gray025 와 달리 라이트에서 순백, 다크에서 순검정으로 반전한다.
    val background: Color,
    val gray025: Color,
    val gray050: Color,
    val gray075: Color,
    val gray100: Color,
    val gray200: Color,
    val gray300: Color,
    val gray400: Color,
    val gray500: Color,
    val gray600: Color,
    val gray700: Color,
    val gray800: Color,
    val gray900: Color,
    val gray950: Color,
    val red: Color,
    val apricot: Color,
    val yellow: Color,
    val green: Color,
    val teal: Color,
    val blue: Color,
    val purple: Color,
)

val LightGamssColors = GamssColors(
    isDark = false,
    white = White,
    black = Black,
    gray1000 = Gray1000,
    background = White,
    gray025 = GrayLight025,
    gray050 = GrayLight050,
    gray075 = GrayLight075,
    gray100 = GrayLight100,
    gray200 = GrayLight200,
    gray300 = GrayLight300,
    gray400 = GrayLight400,
    gray500 = GrayLight500,
    gray600 = GrayLight600,
    gray700 = GrayLight700,
    gray800 = GrayLight800,
    gray900 = GrayLight900,
    gray950 = GrayLight950,
    red = ChromaticLightRed,
    apricot = ChromaticLightApricot,
    yellow = ChromaticLightYellow,
    green = ChromaticLightGreen,
    teal = ChromaticLightTeal,
    blue = ChromaticLightBlue,
    purple = ChromaticLightPurple,
)

val DarkGamssColors = GamssColors(
    isDark = true,
    white = White,
    black = Black,
    gray1000 = Gray1000,
    background = Black,
    gray025 = GrayDark025,
    gray050 = GrayDark050,
    gray075 = GrayDark075,
    gray100 = GrayDark100,
    gray200 = GrayDark200,
    gray300 = GrayDark300,
    gray400 = GrayDark400,
    gray500 = GrayDark500,
    gray600 = GrayDark600,
    gray700 = GrayDark700,
    gray800 = GrayDark800,
    gray900 = GrayDark900,
    gray950 = GrayDark950,
    red = ChromaticDarkRed,
    apricot = ChromaticDarkApricot,
    yellow = ChromaticDarkYellow,
    green = ChromaticDarkGreen,
    teal = ChromaticDarkTeal,
    blue = ChromaticDarkBlue,
    purple = ChromaticDarkPurple,
)

val LocalGamssColors = staticCompositionLocalOf { LightGamssColors }
