package com.gamss.android.core.designsystem.modifier

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 검정 25% 불투명도 그림자를 얹는다. 플로팅 토스트·버튼처럼 리스트 위에 떠 있는 요소에 쓴다.
 */
fun Modifier.gamssShadow(
    shape: Shape = CircleShape,
    elevation: Dp = 8.dp,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = ShadowColor,
    spotColor = ShadowColor,
)

private val ShadowColor = Color.Black.copy(alpha = 0.25f)
