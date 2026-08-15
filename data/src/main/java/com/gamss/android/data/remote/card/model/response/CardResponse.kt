package com.gamss.android.data.remote.card.model.response

import com.gamss.android.domain.card.Card
import com.gamss.android.data.remote.emotion.toEmotionCharacter
import kotlinx.serialization.Serializable

@Serializable
internal data class CardResponse(
    val id: Long? = null,
    val conversationId: Long? = null,
    val emotion: String? = null,
    val summary: String? = null,
    val message: String? = null,
)

/** 서버가 필수 필드를 누락하거나 미지 감정을 보내면 잘못된 카드를 만들지 않는다. */
internal fun CardResponse.toDomainOrNull(): Card? {
    val cardId = id ?: return null
    val conversationId = conversationId ?: return null
    val character = emotion.toEmotionCharacter() ?: return null
    val summary = summary ?: return null
    val message = message ?: return null

    return Card(
        id = cardId,
        conversationId = conversationId,
        character = character,
        summary = summary,
        message = message,
    )
}
