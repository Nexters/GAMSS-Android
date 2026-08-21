package com.gamss.android.data.remote.card.model.response

import com.gamss.android.data.local.card.model.CardEntity
import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 문자열 필드에 기본값을 둔다. gamssJson 의 coerceInputValues 는 선언된 기본값이 있을 때만
 * null 을 그 값으로 바꾼다. 기본값이 없으면 카드 한 장의 null 하나로 목록 전체가 예외로 죽어,
 * [toDomainOrNull] 이 약속한 "한 장만 버리고 나머지는 살린다" 가 아예 작동하지 못한다.
 */
@Serializable
internal data class CardResponse(
    val id: Long,
    val conversationId: Long,
    val emotion: String = "",
    val emotionLabel: String = "",
    val summary: String = "",
    val message: String = "",
    val date: String = "",
)

/**
 * 생성 응답을 변환한다. POST 는 이미 성공한 뒤라 여기서 실패로 돌리면 서버엔 카드가 있는데
 * 클라만 실패로 보고, 재시도는 CARD_ALREADY_EXISTS 로 막혀 그 카드에 영영 닿지 못한다.
 * 그래서 매핑하지 못한 값은 요청에 쓴 값으로 메운다.
 */
internal fun CardResponse.toDomain(
    requestedCharacter: EmotionCharacter,
    fallbackDate: LocalDate,
): Card = Card(
    id = id,
    conversationId = conversationId,
    character = emotion.toEmotionCharacter() ?: requestedCharacter,
    emotionLabel = emotionLabel,
    summary = summary,
    message = message,
    date = date.toLocalDateOrNull() ?: fallbackDate,
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

/** 감정과 날짜를 서버가 준 원문 그대로 둔다. 캐시를 읽을 때 같은 규칙으로 다시 해석한다. */
internal fun CardResponse.toEntity(): CardEntity =
    CardEntity(
        id = id,
        conversationId = conversationId,
        emotion = emotion,
        emotionLabel = emotionLabel,
        summary = summary,
        message = message,
        date = date,
    )

internal fun String.toLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (_: DateTimeParseException) {
    null
}
