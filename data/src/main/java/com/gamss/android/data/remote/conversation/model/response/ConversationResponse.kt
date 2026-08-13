package com.gamss.android.data.remote.conversation.model.response

import com.gamss.android.domain.conversation.Conversation
import kotlinx.serialization.Serializable

@Serializable
internal data class ConversationResponse(
    val id: Long? = null,
    val title: String? = null,
)

internal fun ConversationResponse.toDomain(): Conversation? =
    id?.let { Conversation(id = it, title = title) }
