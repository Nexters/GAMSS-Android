package com.gamss.android.data.remote.card.model.response

import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.card.CardEntry
import kotlinx.serialization.Serializable

@Serializable
internal data class CardCalendarResponse(
    val date: String,
    /** 카드 생성순이며 같은 감정이 여러 번 올 수 있다. */
    val emotions: List<String> = emptyList(),
)

internal fun CardCalendarResponse.toDomain(): List<CardEntry> {
    val createdDate = date.toLocalDateOrNull() ?: return emptyList()
    return emotions.mapNotNull { it.toEmotionCharacter() }
        .mapIndexed { index, character ->
            CardEntry(date = createdDate, indexInDate = index, character = character)
        }
}
