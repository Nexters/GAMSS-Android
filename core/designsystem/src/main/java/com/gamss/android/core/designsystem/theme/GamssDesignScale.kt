package com.gamss.android.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 시안이 그려진 기준 화면 폭. 손그림 에셋의 크기와 여백이 모두 이 폭 위에서 정해졌다. */
val GamssDesignWidth = 402.dp

/** 시안보다 좁은 화면에서만 줄인다. 넓은 화면에서 늘리면 손그림 에셋이 뭉개진다. */
fun designScale(availableWidth: Dp): Float = (availableWidth / GamssDesignWidth).coerceAtMost(1f)

fun designWidth(scale: Float): Dp = GamssDesignWidth * scale
