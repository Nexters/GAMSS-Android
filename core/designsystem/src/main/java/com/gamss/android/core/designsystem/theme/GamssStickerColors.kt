package com.gamss.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 종이 보드 위 장식(포스트잇, 테이프, 쪽지)에 쓰는 일러스트 색.
 *
 * 라이트/다크로 갈리지 않는 고정 색이라 [GamssColors] 가 아니라 별도 object 로 둔다.
 */
object GamssStickerColors {
    val stickyYellow = Color(0xFFFFEFC5)
    val stickyShadow = Color(0xFFEAEAEA)
    val tapeSkyblue = Color(0xFFDCF2FF)
    val handwriting = Color(0xFFFF8D4B)
    val slipBorder = Color(0xFFFF5E5E)
    val slipOutline = Color(0xFFC8C8C8)
}
