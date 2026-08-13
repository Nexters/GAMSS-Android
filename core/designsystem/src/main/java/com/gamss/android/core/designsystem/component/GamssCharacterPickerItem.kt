package com.gamss.android.core.designsystem.component

import androidx.compose.runtime.Immutable

@Immutable
data class GamssCharacterPickerItem<T>(
    val value: T,
    val label: String,
    val selected: Boolean,
)
