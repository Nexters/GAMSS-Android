package com.gamss.android.core.ui.card

import androidx.annotation.StringRes
import com.gamss.android.core.designsystem.card.GamssEmotionCardCharacter
import com.gamss.android.core.ui.R
import com.gamss.android.domain.emotion.EmotionCharacter

/** 도메인 캐릭터를 감정 카드의 디자인 시스템 캐릭터로 변환한다. */
fun EmotionCharacter.toGamssEmotionCardCharacter(): GamssEmotionCardCharacter = when (this) {
    EmotionCharacter.JOY -> GamssEmotionCardCharacter.JOY
    EmotionCharacter.ANGER -> GamssEmotionCardCharacter.ANGER
    EmotionCharacter.ANXIETY -> GamssEmotionCardCharacter.ANXIETY
    EmotionCharacter.SADNESS -> GamssEmotionCardCharacter.SADNESS
    EmotionCharacter.QUIRKY -> GamssEmotionCardCharacter.QUIRKY
    EmotionCharacter.PRICKLY -> GamssEmotionCardCharacter.PRICKLY
}

/**
 * 감정 카드 제목.
 *
 * 제목은 서버가 준 요약·대사가 아니라 카드 캐릭터로 정해지는 고정 문구다. 요약은 제목 아래
 * 작은 텍스트로 따로 들어간다.
 */
@StringRes
fun EmotionCharacter.cardTitleRes(): Int = when (this) {
    EmotionCharacter.JOY -> R.string.emotion_card_title_joy
    EmotionCharacter.ANGER -> R.string.emotion_card_title_anger
    EmotionCharacter.ANXIETY -> R.string.emotion_card_title_anxiety
    EmotionCharacter.SADNESS -> R.string.emotion_card_title_sadness
    EmotionCharacter.QUIRKY -> R.string.emotion_card_title_quirky
    EmotionCharacter.PRICKLY -> R.string.emotion_card_title_prickly
}
