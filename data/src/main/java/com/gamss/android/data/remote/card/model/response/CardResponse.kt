package com.gamss.android.data.remote.card.model.response

import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.card.Card
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeParseException

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

/** 목록 응답의 예상 가능한 서버 데이터 오류가 정상 카드까지 가리지 않도록 안전하게 변환한다. */
internal fun CardResponse.toDomainOrNull(): Card? = emotion.toEmotionCharacter()?.let { character ->
    date.toLocalDateOrNull()?.let { parsedDate ->
        Card(
            id = id,
            conversationId = conversationId,
            character = character,
            emotionLabel = emotionLabel,
            summary = summary,
            message = message,
            date = parsedDate,
        )
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (_: DateTimeParseException) {
    null
}
