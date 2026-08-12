package com.gamss.android.feature.home

sealed interface HomeSideEffect {
    data object NavigateToSetting : HomeSideEffect

    /** 홈에서 적은 걱정을 들고 새 대화방으로 들어간다. 전송은 대화방이 맡는다. */
    data class StartConversation(val message: String) : HomeSideEffect
}
