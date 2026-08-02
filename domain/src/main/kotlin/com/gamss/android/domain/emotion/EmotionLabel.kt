package com.gamss.android.domain.emotion

/**
 * 모델이 분류하는 6개 감정. 선언 순서가 모델 출력 인덱스(id2label)와 같아,
 * 데이터 계층의 라벨 목록을 이 enum 에서 파생시켜 문자열 중복과 순서 불일치를 막는다.
 */
enum class EmotionLabel(val koLabel: String) {
    ANGER("분노"),
    ANXIETY("불안"),
    EMBARRASSED("당황"),
    JOY("기쁨"),
    HURT("상처"),
    SADNESS("슬픔"),
    ;

    companion object {
        fun fromKoLabel(koLabel: String): EmotionLabel =
            entries.firstOrNull { it.koLabel == koLabel }
                ?: error("알 수 없는 감정 라벨: $koLabel")
    }
}
