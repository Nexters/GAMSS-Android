package com.gamss.android.core.ui

import androidx.compose.ui.graphics.vector.ImageVector

data class GamssBottomBarItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)
