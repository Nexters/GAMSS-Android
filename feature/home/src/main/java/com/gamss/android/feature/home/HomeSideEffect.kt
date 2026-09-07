package com.gamss.android.feature.home

sealed interface HomeSideEffect {
    data object NavigateToSetting : HomeSideEffect

    data class ShowToast(val message: String) : HomeSideEffect
}
