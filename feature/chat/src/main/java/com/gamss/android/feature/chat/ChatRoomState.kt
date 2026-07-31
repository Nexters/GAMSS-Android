package com.gamss.android.feature.chat

import com.gamss.android.domain.card.Card
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender

data class ChatRoomState(
    val conversationId: Long? = null,
    val messages: List<Message> = emptyList(),
    /** 아직 노출하지 않은 캐릭터 댓글. 하나씩 [messages] 로 옮긴다. */
    val pendingComments: List<Message> = emptyList(),
    val input: String = "",
    val replyTarget: ReplyTarget? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isEnded: Boolean = false,
    val isFinishing: Boolean = false,
    val showEndConfirm: Boolean = false,
    val card: Card? = null,
    /** 종료는 됐는데 카드만 실패한 상태. 카드 생성만 다시 시도할 수 있어야 한다. */
    val endedButCardFailed: Boolean = false,
) {
    /** 조회 중 전송을 막는다. 뒤늦게 온 조회 결과가 방금 보낸 메시지를 덮어쓴다. */
    val canSend: Boolean get() = input.isNotBlank() && !isSending && !isLoading && !isEnded

    /** 노출 대기 중인 댓글이 있으면 계속 오는 중이라고 알린다. */
    val isReceiving: Boolean get() = isSending || pendingComments.isNotEmpty()

    /** 보낸 메시지가 있어야 카드를 만들 감정과 요약이 나온다. */
    val canEnd: Boolean
        get() = conversationId != null && !isFinishing && !isSending &&
            (!isEnded || endedButCardFailed) &&
            messages.any { it.sender == MessageSender.User }
}

data class ReplyTarget(
    val messageId: Long,
    val characterName: String,
)
