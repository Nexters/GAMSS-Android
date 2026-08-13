package com.gamss.android.feature.home

sealed interface HomeSideEffect {
    data object NavigateToSetting : HomeSideEffect

    /** 전송은 대화방이 맡는다. */
    data class StartConversation(val message: String) : HomeSideEffect
}
