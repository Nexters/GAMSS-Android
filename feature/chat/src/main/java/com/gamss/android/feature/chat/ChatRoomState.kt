package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.Message

data class ChatRoomState(
    val conversationId: Long? = null,
    val messages: List<Message> = emptyList(),
    /** 아직 노출하지 않은 캐릭터 댓글. 하나씩 [messages] 로 옮긴다. */
    val pendingComments: List<Message> = emptyList(),
    val input: String = "",
    val replyTarget: ReplyTarget? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
) {
    /** 조회 중 전송을 막는다. 뒤늦게 온 조회 결과가 방금 보낸 메시지를 덮어쓴다. */
    val canSend: Boolean get() = input.isNotBlank() && !isSending && !isLoading

    /** 노출 대기 중인 댓글이 있으면 계속 오는 중이라고 알린다. */
    val isReceiving: Boolean get() = isSending || pendingComments.isNotEmpty()
}

data class ReplyTarget(
    val messageId: Long,
    val characterName: String,
)
