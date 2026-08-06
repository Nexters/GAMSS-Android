package com.gamss.android.domain.emotion

enum class EmotionCharacter(val displayName: String) {
    JOY("기쁨"),
    ANGER("분노"),
    ANXIETY("불안"),
    QUIRKY("엉뚱"),
    PRICKLY("까칠"),
    WARM("다정"),
    ;

    companion object {
        /** 모델 감정 6종 → 대표 캐릭터의 1:1 매핑. */
        fun fromEmotionLabel(label: EmotionLabel): EmotionCharacter = when (label) {
            EmotionLabel.JOY -> JOY
            EmotionLabel.ANGER -> ANGER
            EmotionLabel.ANXIETY -> ANXIETY
            EmotionLabel.EMBARRASSED -> QUIRKY
            EmotionLabel.HURT -> PRICKLY
            EmotionLabel.SADNESS -> WARM
        }
    }
}
