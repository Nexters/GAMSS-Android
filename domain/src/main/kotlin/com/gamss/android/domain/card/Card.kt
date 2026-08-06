package com.gamss.android.domain.card

import com.gamss.android.domain.emotion.EmotionCharacter

/**
 * 대화 종료로 만들어진 감정 카드.
 *
 * @param summary 클라이언트가 보낸 압축본. 카드 제목으로 그대로 노출된다.
 */
data class Card(
    val character: EmotionCharacter,
    val summary: String,
    val message: String,
)

/** 다시 시도해도 결과가 같은 카드 실패. 호출부가 재시도 경로를 접는 기준이다. */
sealed class CardNotRetryableException(cause: Throwable? = null) : IllegalStateException(cause) {

    /** 이미 카드가 있는 대화. 방당 하나뿐이라 다시 만들 수 없다. */
    class AlreadyExists(cause: Throwable? = null) : CardNotRetryableException(cause)

    /** 요약이 비어 카드 제목을 만들 수 없다. 같은 발화로 다시 요약해도 마찬가지다. */
    class NoSummary : CardNotRetryableException()

    /** 분류를 다 돌렸는데도 대표 감정이 없다. 카드에 붙일 캐릭터를 고를 수 없다. */
    class NoEmotion : CardNotRetryableException()
}
