package com.gamss.android.data.remote.conversation.model.response

import com.gamss.android.domain.conversation.ConversationDetail
import kotlinx.serialization.Serializable

@Serializable
internal data class ConversationDetailResponse(
    val conversation: ConversationResponse,
    val messages: List<ConversationMessage> = emptyList(),
)

internal fun ConversationDetailResponse.toDomain(): ConversationDetail? =
    conversation.toDomain()?.let { conversation ->
        ConversationDetail(
            conversation = conversation,
            messages = messages.map(ConversationMessage::toDomain),
        )
    }
