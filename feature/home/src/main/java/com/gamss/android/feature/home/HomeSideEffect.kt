package com.gamss.android.feature.home

import com.gamss.android.domain.emotion.EmotionCharacter

sealed interface HomeSideEffect {
    data object NavigateToSetting : HomeSideEffect

    /** 전송은 대화방이 맡는다. */
    data class StartConversation(
        val message: String,
        val excludeCharacters: Set<EmotionCharacter>,
    ) : HomeSideEffect

    data class ShowToast(val message: String) : HomeSideEffect
}
