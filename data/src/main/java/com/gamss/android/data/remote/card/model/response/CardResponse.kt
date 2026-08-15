package com.gamss.android.data.remote.card.model.response

import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable

@Serializable
internal data class CardResponse(
    val emotion: String? = null,
    val summary: String,
    val message: String,
)

/** 목록 응답의 감정 문자열이 알 수 없는 값이면 그 카드는 표시하지 않는다. */
internal fun CardResponse.toDomainOrNull(): Card? =
    emotion.toEmotionCharacter()?.let(::toDomain)

/**
 * 서버는 우리가 보낸 감정을 그대로 대표 감정으로 쓰므로 응답 문자열을 다시 해석하지 않고
 * 보낸 캐릭터를 그대로 쓴다. 해석 실패로 카드를 잃는 경로를 없앤다.
 */
internal fun CardResponse.toDomain(character: EmotionCharacter): Card = Card(
    character = character,
    summary = summary,
    message = message,
)
