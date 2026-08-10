package com.gamss.android.data.remote.card.model.response

import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable

@Serializable
internal data class CardResponse(
    val summary: String,
    val message: String,
)

/**
 * 서버는 우리가 보낸 감정을 그대로 대표 감정으로 쓰므로 응답 문자열을 다시 해석하지 않고
 * 보낸 캐릭터를 그대로 쓴다. 해석 실패로 카드를 잃는 경로를 없앤다.
 */
internal fun CardResponse.toDomain(character: EmotionCharacter): Card = Card(
    character = character,
    summary = summary,
    message = message,
)
