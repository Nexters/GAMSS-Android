package com.gamss.android.feature.chat.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/**
 * 진입시 표시되는 메시지는 그대로 표시하고, 그 이후 목록에 추가된 메시지만 진입 애니메이션 대상으로 관리한다.
 */
internal class ChatMessageAnimation {
    private val displayedMessageIds = mutableSetOf<Long>()
    private var hasCapturedInitialMessages = false

    fun shouldAnimate(messageId: Long): Boolean =
        hasCapturedInitialMessages && messageId !in displayedMessageIds

    fun markDisplayed(messageIds: List<Long>) {
        displayedMessageIds += messageIds
        hasCapturedInitialMessages = true
    }
}

@Composable
internal fun rememberChatMessageAnimationState(
    conversationId: Long?,
    isLoading: Boolean,
    messageIds: List<Long>,
): ChatMessageAnimation {
    val animationState = remember(conversationId) { ChatMessageAnimation() }

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
        // slide 와 fade 를 동일한 스프링 스펙으로 묶어, 두 효과가 서로 다른 타이밍으로 끝나며
        // 끊겨 보이던 문제를 없애고 하나의 유려한 움직임으로 이어지게 한다.
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = MessageEnterDampingRatio,
                stiffness = MessageEnterStiffness,
            ),
            initialOffsetY = { messageHeight ->
                listState.offsetFromInput(messageId, messageHeight)
            },
        ) + fadeIn(
            animationSpec = spring(
                dampingRatio = MessageEnterDampingRatio,
                stiffness = MessageEnterStiffness,
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

private const val MessageEnterDampingRatio = Spring.DampingRatioLowBouncy
private const val MessageEnterStiffness = Spring.StiffnessMediumLow
