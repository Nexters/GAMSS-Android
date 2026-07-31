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

/** 이미 카드가 있는 대화. 방당 하나뿐이라 다시 만들 수 없다. */
class CardAlreadyExistsException(cause: Throwable? = null) : IllegalStateException(cause)
