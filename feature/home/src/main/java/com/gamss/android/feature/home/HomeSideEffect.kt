package com.gamss.android.feature.home

sealed interface HomeSideEffect {
    data class ShowToast(val message: String) : HomeSideEffect
    data object NavigateToSetting : HomeSideEffect
}
