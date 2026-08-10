package com.gamss.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Base
internal val White = Color(0xFFFFFFFF)
internal val Black = Color(0xFF000000)

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
    val white: Color,
    val black: Color,
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
    white = White,
    black = Black,
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
    white = White,
    black = Black,
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
