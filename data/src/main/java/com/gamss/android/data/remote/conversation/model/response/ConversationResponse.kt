package com.gamss.android.data.remote.conversation.model.response

import com.gamss.android.domain.conversation.Conversation
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

@Serializable
internal data class ConversationResponse(
    val id: Long? = null,
    val title: String? = null,
    val createdAt: String? = null,
)

internal fun ConversationResponse.toDomain(): Conversation? =
    id?.let {
        Conversation(
            id = it,
            title = title,
            createdAt = parseConversationCreatedAt(createdAt),
        )
    }

/**
 * 서버 시각 문자열을 사용자에게 보이는 벽시계 시각으로 바꾼다.
 *
 * 오프셋이 붙어 오면 [zone] 기준으로 옮기고, 없으면 이미 로컬 시각으로 본다. 리스트 응답의 시각
 * 형식이 확정되지 않아 두 형태를 모두 받는다. 파싱에 실패하면 null 이다. 목록 전체를 버리는 것보다
 * 시각만 감추는 쪽이 낫다.
 *
 * [zone] 은 테스트가 고정할 수 있도록 파라미터로 둔다.
 */
internal fun parseConversationCreatedAt(
    raw: String?,
    zone: ZoneId = ZoneId.systemDefault(),
): LocalDateTime? {
    val value = raw?.trim()
    if (value.isNullOrEmpty()) return null

    return try {
        OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDateTime()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
