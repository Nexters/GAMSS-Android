package com.gamss.android.feature.setting

sealed interface SettingSideEffect {
    data class ShowToast(val message: String) : SettingSideEffect
}
