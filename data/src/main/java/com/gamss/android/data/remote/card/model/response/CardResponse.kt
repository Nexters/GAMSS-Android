package com.gamss.android.data.remote.card.model.response

import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.card.Card
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
internal data class CardResponse(
    val id: Long,
    val conversationId: Long,
    val emotion: String,
    val emotionLabel: String,
    val summary: String,
    val message: String,
    val date: String,
)

internal fun CardResponse.toDomain(): Card = Card(
    id = id,
    conversationId = conversationId,
    character = requireNotNull(emotion.toEmotionCharacter()) { "Unknown card emotion=$emotion" },
    emotionLabel = emotionLabel,
    summary = summary,
    message = message,
    date = LocalDate.parse(date),
)
