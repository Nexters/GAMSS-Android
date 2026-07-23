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
        /**
         * 모델 감정 라벨(분노/불안/당황/기쁨/상처/슬픔) → 대표 캐릭터의 1:1 매핑.
         * 모델은 항상 이 6종 중 하나만 출력하므로 전체 대응이며, 그 외 라벨은 계약 위반이라 예외.
         */
        fun fromEmotionLabel(label: String): EmotionCharacter = when (label) {
            "기쁨" -> JOY
            "분노" -> ANGER
            "불안" -> ANXIETY
            "당황" -> QUIRKY
            "상처" -> PRICKLY
            "슬픔" -> WARM
            else -> error("알 수 없는 감정 라벨: $label")
        }
    }
}
