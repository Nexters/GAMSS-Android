package com.gamss.android.domain.emotion

/**
 * 카드에서 사용자 감정을 대표하는 캐릭터.
 *
 * enum 이름(JOY 등)은 내부용이다. 서버 `emotionType` 문자열 대응은 현재 "JOY"/"ANGER" 만 확인됐고,
 * 나머지 넷은 서버 연동 시점에 확인해야 한다 — 지금은 서버 직렬화 문자열을 하드코딩하지 않는다.
 */
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
