package com.gamss.android.feature.archive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 보관함 시안 기준 화면 폭. */
private val DesignWidth = 402.dp

/** 시안보다 좁은 화면에서만 줄인다. 넓은 화면에서 늘리면 손그림 에셋이 뭉개진다. */
internal fun designScale(availableWidth: Dp): Float = (availableWidth / DesignWidth).coerceAtMost(1f)

internal fun designWidth(scale: Float): Dp = DesignWidth * scale
