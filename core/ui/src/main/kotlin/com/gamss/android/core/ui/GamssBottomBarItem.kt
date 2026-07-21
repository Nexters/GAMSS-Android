package com.gamss.android.core.ui

import androidx.compose.ui.graphics.vector.ImageVector

data class GamssBottomBarItem<T>(
    val value: T,
    val icon: ImageVector,
    val label: String,
)
