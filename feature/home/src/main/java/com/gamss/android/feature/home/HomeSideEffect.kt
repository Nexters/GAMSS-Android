package com.gamss.android.feature.home

sealed interface HomeSideEffect {
    data object NavigateToSetting : HomeSideEffect

    /** 대화는 이미 만들어졌다. 대화방은 이 id 로 조회만 한다. */
    data class OpenConversation(
        val conversationId: Long,
        val navigationGeneration: Long = 0L,
    ) : HomeSideEffect

    data class ShowToast(val message: String) : HomeSideEffect
}
