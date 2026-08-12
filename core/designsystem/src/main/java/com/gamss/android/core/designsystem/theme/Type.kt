package com.gamss.android.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.gamss.android.core.designsystem.R

internal val pretendardFamily = FontFamily(
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_regular, FontWeight.Normal),
)

// 디자인은 픽셀체(Moneygraphy Pixel)와 손글씨체(BM kkubulim)를 쓰지만 폰트 파일을 아직 받지 못했다.
// 폰트가 들어오면 아래 두 값만 실제 FontFamily 로 교체하면 된다.
internal val pixelFamily = pretendardFamily
internal val handwritingFamily = pretendardFamily

object GamssFontSize {
    val fontSize25 = 10.sp
    val fontSize50 = 12.sp
    val fontSize75 = 14.sp
    val fontSize100 = 16.sp
    val fontSize200 = 18.sp
    val fontSize300 = 20.sp
    val fontSize400 = 24.sp
    val fontSize500 = 28.sp
    val fontSize600 = 32.sp
    val fontSize700 = 36.sp
    val fontSize800 = 42.sp
    val fontSize900 = 48.sp
}

object GamssLineHeight {
    val lineHeight025 = 14.sp
    val lineHeight050 = 18.sp
    val lineHeight075 = 20.sp
    val lineHeight100 = 24.sp
    val lineHeight200 = 28.sp
    val lineHeight300 = 32.sp
    val lineHeight400 = 36.sp
    val lineHeight500 = 40.sp
    val lineHeight600 = 48.sp
    val lineHeight700 = 56.sp
    val lineHeight800 = 64.sp
}

object GamssLetterSpacing {
    val letterSpacing025 = (-0.4).sp
    val letterSpacing050 = (-0.2).sp
    val letterSpacing100 = (-0.15).sp
    val letterSpacing150 = (-0.1).sp
    val letterSpacing200 = 0.sp
    val letterSpacing300 = 1.sp
}

object GamssFontWeight {
    val fontWeight100 = FontWeight.Normal
    val fontWeight200 = FontWeight.Medium
    val fontWeight300 = FontWeight.SemiBold
    val fontWeight400 = FontWeight.Bold
}

private fun gamssTextStyle(
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit,
    fontFamily: FontFamily = pretendardFamily,
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

object GamssTypography {
    val display1 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight400,
        fontSize = GamssFontSize.fontSize900,
        lineHeight = GamssLineHeight.lineHeight700,
        letterSpacing = GamssLetterSpacing.letterSpacing025,
    )
    val display2 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight400,
        fontSize = GamssFontSize.fontSize700,
        lineHeight = GamssLineHeight.lineHeight500,
        letterSpacing = GamssLetterSpacing.letterSpacing050,
    )
    val title1 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight400,
        fontSize = GamssFontSize.fontSize500,
        lineHeight = GamssLineHeight.lineHeight300,
        letterSpacing = GamssLetterSpacing.letterSpacing050,
    )
    val title2 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight400,
        fontSize = GamssFontSize.fontSize400,
        lineHeight = GamssLineHeight.lineHeight200,
        letterSpacing = GamssLetterSpacing.letterSpacing100,
    )
    val title3 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight400,
        fontSize = GamssFontSize.fontSize300,
        lineHeight = GamssLineHeight.lineHeight100,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val title4 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight400,
        fontSize = GamssFontSize.fontSize200,
        lineHeight = GamssLineHeight.lineHeight100,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val title5 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight400,
        fontSize = GamssFontSize.fontSize100,
        lineHeight = GamssLineHeight.lineHeight075,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val subtitle1 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight300,
        fontSize = GamssFontSize.fontSize300,
        lineHeight = GamssLineHeight.lineHeight200,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val subtitle2 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight300,
        fontSize = GamssFontSize.fontSize200,
        lineHeight = GamssLineHeight.lineHeight100,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val subtitle3 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight300,
        fontSize = GamssFontSize.fontSize100,
        lineHeight = GamssLineHeight.lineHeight100,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val subtitle4 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight300,
        fontSize = GamssFontSize.fontSize75,
        lineHeight = GamssLineHeight.lineHeight075,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body1Medium = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize300,
        lineHeight = GamssLineHeight.lineHeight300,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body2Medium = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize200,
        lineHeight = GamssLineHeight.lineHeight200,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body3Medium = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize100,
        lineHeight = GamssLineHeight.lineHeight100,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body4Medium = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize75,
        lineHeight = GamssLineHeight.lineHeight075,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body5Medium = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize50,
        lineHeight = GamssLineHeight.lineHeight050,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body6Medium = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize25,
        lineHeight = GamssLineHeight.lineHeight025, // Figma상 lineHeight020 확인 필요
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body1Regular = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize300,
        lineHeight = GamssLineHeight.lineHeight300,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body2Regular = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize200,
        lineHeight = GamssLineHeight.lineHeight200,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body3Regular = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize100,
        lineHeight = GamssLineHeight.lineHeight100,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body4Regular = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize75,
        lineHeight = GamssLineHeight.lineHeight075,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body5Regular = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize50,
        lineHeight = GamssLineHeight.lineHeight050,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val body6Regular = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize25,
        lineHeight = GamssLineHeight.lineHeight025,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val paragraph1 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize100,
        lineHeight = GamssLineHeight.lineHeight300,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val paragraph2 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize75,
        lineHeight = GamssLineHeight.lineHeight200,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val paragraph3 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize50,
        lineHeight = GamssLineHeight.lineHeight100,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val caption1 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize75,
        lineHeight = GamssLineHeight.lineHeight075,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val caption2 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize50,
        lineHeight = GamssLineHeight.lineHeight050,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val caption3 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize25,
        lineHeight = GamssLineHeight.lineHeight025,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )
    val caption4 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight100,
        fontSize = GamssFontSize.fontSize25,
        lineHeight = GamssLineHeight.lineHeight025,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
    )

    /** 홈 인사말처럼 픽셀체로 찍는 문구. [title2] 와 크기는 같고 패밀리만 다르다. */
    val pixelTitle2 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight400,
        fontSize = GamssFontSize.fontSize400,
        lineHeight = GamssLineHeight.lineHeight200,
        letterSpacing = GamssLetterSpacing.letterSpacing100,
        fontFamily = pixelFamily,
    )

    /** 포스트잇에 손으로 적은 듯한 문구. */
    val handwritingBody4 = gamssTextStyle(
        fontWeight = GamssFontWeight.fontWeight200,
        fontSize = GamssFontSize.fontSize75,
        lineHeight = GamssLineHeight.lineHeight075,
        letterSpacing = GamssLetterSpacing.letterSpacing150,
        fontFamily = handwritingFamily,
    )
}
