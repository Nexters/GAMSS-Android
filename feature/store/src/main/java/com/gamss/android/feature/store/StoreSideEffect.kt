package com.gamss.android.feature.store

sealed interface StoreSideEffect {
    data object NavigateToSetting : StoreSideEffect
}
