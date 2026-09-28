package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.safety.RiskDetection
import java.time.LocalDateTime

data class ChatRoomState(
    val conversationId: Long? = null,
    val conversationCreatedAt: LocalDateTime? = null,
    val messages: List<Message> = emptyList(),
    /** 서버 id로 교체된 내 메시지의 서버 id → 임시 id. [listKeyOf]가 교체 전후 같은 key를 쓰게 한다. */
    val localKeyByMessageId: Map<Long, Long> = emptyMap(),
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
    /** 서버가 확정한 소진 여부. true면 입력창의 전송 버튼을 잠근다. */
    val isTokenExhausted: Boolean = false,
) {
    /**
     * 종료 흐름이 시작된 뒤로는 막는다. 종료 API 가 도는 중에 보내면 저장 여부가 갈린다.
     *
     * isTokenExhausted 도 여기서 막아야 한다 — 전송 버튼 비활성화는 UI 레이어일 뿐이라, 물리
     * 키보드 엔터 등 버튼을 거치지 않는 경로로 onSend() 가 불려도 이미 소진된 걸 알면서 서버에
     * 요청을 보내면 안 된다.
     */
    val canSend: Boolean
        get() = input.isNotBlank() && !isSending && !isLoading && !isTokenExhausted &&
            endFlow == EndFlow.NotStarted

    // isSending(내 전송이 서버 왕복 중)은 여기 포함하지 않는다 — "입력중" 버블은 상대가 답장을
    // 준비 중일 때만 보여야 하고, 내 메시지 전송 중이라는 것과는 다른 신호다. 전송 중 UI 피드백은
    // 입력창의 전송 버튼 비활성화(MessageInputBar의 isSending)로 이미 충분하다.
    val isAwaitingComments: Boolean get() = pendingComments.isNotEmpty()

    /** 보낸 메시지가 있어야 카드를 만들 감정과 요약이 나온다. 종료 단계와 무관한 조건이다. */
    val endPreconditionsMet: Boolean
        get() = conversationId != null && !isSending &&
            messages.any { it.sender == MessageSender.User }

    val canEnd: Boolean get() = endFlow.acceptsEndRequest && endPreconditionsMet

    /**
     * 다음 답장의 로딩 표시. 그 답장과 같은 id라, 공개되는 순간 같은 아이템에서 내용만 로딩 → 실제
     * 글자로 바뀐다.
     */
    val loadingPlaceholder: Message? get() = pendingComments.firstOrNull()

    /** 화면에 그리는 목록. [messages] 끝에 [loadingPlaceholder]를 붙인다. Compose(메인 스레드)에서만 읽는다. */
    val displayMessages: List<Message> by lazy(LazyThreadSafetyMode.NONE) {
        loadingPlaceholder?.let { messages + it } ?: messages
    }

    /** 목록 key. 임시 id → 서버 id로 바뀌어도 같은 아이템으로 남아 진입 애니메이션이 다시 돌지 않는다. */
    fun listKeyOf(message: Message): Long = localKeyByMessageId[message.id] ?: message.id
}

data class ReplyTarget(
    val messageId: Long,
    val characterName: String,
    val content: String,
)
