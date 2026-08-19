package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.safety.RiskDetection
import java.time.LocalDateTime

data class ChatRoomState(
    val conversationId: Long? = null,
    val conversationCreatedAt: LocalDateTime? = null,
    val messages: List<Message> = emptyList(),
    val pendingComments: List<Message> = emptyList(),
    val input: String = "",
    val replyTarget: ReplyTarget? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val endFlow: EndFlow = EndFlow.NotStarted,
    val riskDetection: RiskDetection? = null,
    val tokenUsagePercent: Int? = null,
    val isTokenUsagePopupExpanded: Boolean = false,
    val isTokenUsageLoading: Boolean = false,
) {
    /** 종료 흐름이 시작된 뒤로는 막는다. 종료 API 가 도는 중에 보내면 저장 여부가 갈린다. */
    val canSend: Boolean
        get() = input.isNotBlank() && !isSending && !isLoading && endFlow == EndFlow.NotStarted

    // isSending(내 전송이 서버 왕복 중)은 여기 포함하지 않는다 — "입력중" 버블은 상대가 답장을
    // 준비 중일 때만 보여야 하고, 내 메시지 전송 중이라는 것과는 다른 신호다. 전송 중 UI 피드백은
    // 입력창의 전송 버튼 비활성화(MessageInputBar의 isSending)로 이미 충분하다.
    val isAwaitingComments: Boolean get() = pendingComments.isNotEmpty()

    /** 보낸 메시지가 있어야 카드를 만들 감정과 요약이 나온다. 종료 단계와 무관한 조건이다. */
    val endPreconditionsMet: Boolean
        get() = conversationId != null && !isSending &&
            messages.any { it.sender == MessageSender.User }

    val canEnd: Boolean get() = endFlow.acceptsEndRequest && endPreconditionsMet
}

data class ReplyTarget(
    val messageId: Long,
    val characterName: String,
    val content: String,
)
