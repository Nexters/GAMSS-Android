package com.gamss.android.feature.chat.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.gamss.android.domain.conversation.Message

/**
 * 진입시 표시되는 메시지는 그대로 표시하고, 그 이후 목록에 추가된 메시지만 진입 애니메이션 대상으로 관리한다.
 */
internal class ChatMessageAnimation {
    private val displayedMessageIds = mutableSetOf<Long>()
    private var hasCapturedInitialMessages = false

    // markDisplayed()는 SideEffect로 리컴포지션마다 불린다 — 입력칸 타이핑처럼 메시지 목록과
    // 무관한 리컴포지션에서도 매번 호출되므로, 직전과 같은 목록이면(참조 동일) 다시 훑지 않는다.
    private var lastMarkedMessageIds: List<Long>? = null

    fun shouldAnimate(messageId: Long): Boolean =
        hasCapturedInitialMessages && messageId !in displayedMessageIds

    fun markDisplayed(messageIds: List<Long>) {
        if (messageIds === lastMarkedMessageIds) return
        lastMarkedMessageIds = messageIds
        displayedMessageIds += messageIds
        hasCapturedInitialMessages = true
    }
}

@Composable
internal fun rememberChatMessageAnimationState(
    conversationId: Long?,
    isLoading: Boolean,
    messages: List<Message>,
): ChatMessageAnimation {
    val animationState = remember(conversationId) { ChatMessageAnimation() }
    // messages 참조가 그대로면(메시지와 무관한 리컴포지션) 새로 매핑하지 않는다 — 위 참조
    // 비교와 짝을 이뤄야 하므로, 매핑 결과도 messages가 바뀔 때만 새 리스트가 된다.
    val messageIds = remember(messages) { messages.map(Message::id) }

    SideEffect {
        if (conversationId != null && !isLoading) {
            animationState.markDisplayed(messageIds)
        }
    }

    return animationState
}

/** 메시지 목록 하단(입력창과 맞닿는 위치)에서 최종 말풍선 위치까지 메시지를 올려 보낸다. */
@Composable
internal fun AnimatedChatMessage(
    messageId: Long,
    shouldAnimate: Boolean,
    listState: LazyListState,
    content: @Composable () -> Unit,
) {
    if (!shouldAnimate) {
        content()
        return
    }

    val visibilityState = remember(messageId) {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visibilityState,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = MESSAGE_ENTER_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetY = { messageHeight ->
                listState.offsetFromInput(messageId, messageHeight)
            },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = MESSAGE_ENTER_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        ),
    ) {
        content()
    }
}

private fun LazyListState.offsetFromInput(messageId: Long, messageHeight: Int): Int {
    val messageItem = layoutInfo.visibleItemsInfo.firstOrNull { it.key == messageId }
        ?: return messageHeight
    return (layoutInfo.viewportEndOffset - messageItem.offset).coerceAtLeast(messageHeight)
}

private const val MESSAGE_ENTER_DURATION_MILLIS = 260
