package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.Message

data class ChatRoomState(
    val conversationId: Long? = null,
    val messages: List<Message> = emptyList(),
    val pendingComments: List<Message> = emptyList(),
    val input: String = "",
    val replyTarget: ReplyTarget? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
) {
    val canSend: Boolean get() = input.isNotBlank() && !isSending && !isLoading

    val isAwaitingComments: Boolean get() = isSending || pendingComments.isNotEmpty()
}

data class ReplyTarget(
    val messageId: Long,
    val characterName: String,
)
