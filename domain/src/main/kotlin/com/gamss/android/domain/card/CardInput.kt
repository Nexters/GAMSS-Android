package com.gamss.android.domain.card

import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.emotion.EmotionLabel

/** 카드 생성 입력: 대표 감정, 그 감정을 대표하는 캐릭터, 압축 요약. */
data class CardInput(
    val emotion: EmotionLabel,
    val character: EmotionCharacter,
    val summary: String?,
)
