package com.gamss.android.feature.chat

import com.gamss.android.core.designsystem.card.GamssEmotionCardCharacter
import com.gamss.android.domain.emotion.EmotionCharacter

/** 도메인 캐릭터를 감정 카드에서 사용하는 디자인 시스템 캐릭터로 변환한다. */
internal fun EmotionCharacter.toGamssEmotionCardCharacter(): GamssEmotionCardCharacter = when (this) {
    EmotionCharacter.JOY -> GamssEmotionCardCharacter.JOY
    EmotionCharacter.ANGER -> GamssEmotionCardCharacter.ANGER
    EmotionCharacter.ANXIETY -> GamssEmotionCardCharacter.ANXIETY
    EmotionCharacter.SADNESS -> GamssEmotionCardCharacter.SADNESS
    EmotionCharacter.QUIRKY -> GamssEmotionCardCharacter.QUIRKY
    EmotionCharacter.PRICKLY -> GamssEmotionCardCharacter.PRICKLY
}
