package com.gamss.android.data.remote.card.model.response

import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.card.Card
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
    return idsOrNull()?.let { (cardId, cardConversationId) ->
        textOrNull()?.let { (cardSummary, cardMessage) ->
            emotion.toEmotionCharacter()?.let { character ->
                Card(
                    id = cardId,
                    conversationId = cardConversationId,
                    character = character,
                    summary = cardSummary,
                    message = cardMessage,
                )
            }
        }
    }
}

private fun CardResponse.idsOrNull(): Pair<Long, Long>? =
    id?.let { cardId -> conversationId?.let { cardId to it } }

private fun CardResponse.textOrNull(): Pair<String, String>? =
    summary?.let { cardSummary -> message?.let { cardSummary to it } }
