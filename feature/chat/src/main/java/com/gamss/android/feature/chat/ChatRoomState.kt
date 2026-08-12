package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.safety.RiskDetection

data class ChatRoomState(
    val conversationId: Long? = null,
    val messages: List<Message> = emptyList(),
    val pendingComments: List<Message> = emptyList(),
    val input: String = "",
    val replyTarget: ReplyTarget? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val endFlow: EndFlow = EndFlow.NotStarted,
    val riskDetection: RiskDetection? = null,
) {
    /** 종료 흐름이 시작된 뒤로는 막는다. 종료 API 가 도는 중에 보내면 저장 여부가 갈린다. */
    val canSend: Boolean
        get() = input.isNotBlank() && !isSending && !isLoading && endFlow == EndFlow.NotStarted

    val isAwaitingComments: Boolean get() = isSending || pendingComments.isNotEmpty()

    /** 보낸 메시지가 있어야 카드를 만들 감정과 요약이 나온다. 종료 단계와 무관한 조건이다. */
    val endPreconditionsMet: Boolean
        get() = conversationId != null && !isSending &&
            messages.any { it.sender == MessageSender.User }

    val canEnd: Boolean get() = endFlow.acceptsEndRequest && endPreconditionsMet
}

data class ReplyTarget(
    val messageId: Long,
    val characterName: String,
)
