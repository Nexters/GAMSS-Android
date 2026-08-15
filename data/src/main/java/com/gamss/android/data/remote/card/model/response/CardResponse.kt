package com.gamss.android.data.remote.card.model.response

import com.gamss.android.domain.card.Card
import com.gamss.android.data.remote.emotion.toEmotionCharacter
import kotlinx.serialization.Serializable

@Serializable
internal data class CardResponse(
    val id: Long,
    val conversationId: Long,
    val emotion: String,
    val summary: String,
    val message: String,
)

internal fun CardResponse.toDomain(): Card = Card(
    id = id,
    conversationId = conversationId,
    character = checkNotNull(emotion.toEmotionCharacter()) { "Unknown card emotion: $emotion" },
    summary = summary,
    message = message,
)
