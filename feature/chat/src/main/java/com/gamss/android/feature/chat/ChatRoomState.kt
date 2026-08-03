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
    /** 조회 중 전송을 막는다. 뒤늦게 온 조회 결과가 방금 보낸 메시지를 덮어쓴다. */
    val canSend: Boolean get() = input.isNotBlank() && !isSending && !isLoading

    val isReceiving: Boolean get() = isSending || pendingComments.isNotEmpty()
}

data class ReplyTarget(
    val messageId: Long,
    val characterName: String,
)
