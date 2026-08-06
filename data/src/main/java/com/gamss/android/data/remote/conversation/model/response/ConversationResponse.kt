package com.gamss.android.data.remote.conversation.model.response

import com.gamss.android.domain.conversation.Conversation
import kotlinx.serialization.Serializable

/**
 * 대화방(서버 응답). `title` 은 아직 지정하지 않았으면 null 이다.
 * 제목 지정 응답에서는 본문을 읽지 않아, 서버가 필드를 빼도 파싱이 깨지지 않도록 전부 선택 항목으로 둔다.
 */
@Serializable
internal data class ConversationResponse(
    val id: Long? = null,
    val title: String? = null,
)

internal fun ConversationResponse.toDomain(): Conversation? =
    id?.let { Conversation(id = it, title = title) }
