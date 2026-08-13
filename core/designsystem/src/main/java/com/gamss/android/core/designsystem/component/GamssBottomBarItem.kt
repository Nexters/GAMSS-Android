package com.gamss.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
data class GamssBottomBarItem<T>(
    val value: T,
    @param:DrawableRes @get:DrawableRes val iconRes: Int,
    val label: String,
)
