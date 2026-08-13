package com.gamss.android.feature.home

import com.gamss.android.domain.emotion.EmotionCharacter

private val AllCharacters: Set<EmotionCharacter> = EmotionCharacter.entries.toSet()

data class HomeState(
    val isLoading: Boolean = true,
    val nickname: String? = null,
    val input: String = "",
    val isEmotionPickerExpanded: Boolean = false,
    /** 반응할 캐릭터. 기본은 전체 선택이고, 최소 한 종은 남는다. */
    val selectedCharacters: Set<EmotionCharacter> = AllCharacters,
) {
    /** 서버는 반응할 캐릭터가 아니라 제외할 캐릭터를 받는다. */
    val excludedCharacters: Set<EmotionCharacter>
        get() = AllCharacters - selectedCharacters
}
