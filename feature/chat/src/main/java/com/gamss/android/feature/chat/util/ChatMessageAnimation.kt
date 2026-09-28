package com.gamss.android.feature.chat.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 진입 시 이미 있던 메시지는 그대로 두고, 그 이후 새로 나타난 메시지만 진입 애니메이션 대상으로 삼는다.
 *
 * LazyColumn 아이템은 부모보다 늦게(레이아웃 단계에서) 컴포지션되므로, 판정과 기록은 아이템 자신의
 * 첫 컴포지션에서 [claimShouldAnimate]로 함께 한다.
 */
internal class ChatMessageAnimation {
    private var hasCapturedInitial = false
    private val seenMessageIds = mutableSetOf<Long>()

    /**
     * 초기 로딩이 끝난 뒤 불린다. 로딩된 메시지는 이 시점엔 아직 컴포지션 전이므로 [initialMessageIds]로
     * 미리 "본 메시지"로 기록해 둬야 진입 시 애니메이션되지 않는다. 처음 한 번만 반영된다.
     */
    fun markInitialLoadComplete(initialMessageIds: () -> List<Long>) {
        if (hasCapturedInitial) return
        seenMessageIds += initialMessageIds()
        hasCapturedInitial = true
    }

    /** 이 messageId가 처음 컴포지션되는지 판정하고 기록한다. 아이템당 한 번만 불러야 한다. */
    fun claimShouldAnimate(messageId: Long): Boolean = hasCapturedInitial && seenMessageIds.add(messageId)
}

@Composable
internal fun rememberChatMessageAnimationState(
    conversationId: Long?,
    isLoading: Boolean,
    initialMessageIds: () -> List<Long>,
): ChatMessageAnimation {
    val animationState = remember(conversationId) { ChatMessageAnimation() }
    if (conversationId != null && !isLoading) {
        animationState.markInitialLoadComplete(initialMessageIds)
    }
    return animationState
}

/**
 * 새 메시지를 입력창 쪽에서 [MessageSlideDistance]만큼 밀어 올리며 나타낸다.
 *
 * [modifier]에는 호출부가 [chatMessageItemAnimation]을 넘긴다. animateItem과 AnimatedVisibility를
 * 같은 노드에 얹으면 측정이 얽히므로, 바깥 Box와 안쪽 AnimatedVisibility로 나눠 둔다.
 */
@Composable
internal fun AnimatedChatMessage(
    messageId: Long,
    animationState: ChatMessageAnimation,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shouldAnimate = remember(messageId) { animationState.claimShouldAnimate(messageId) }

    Box(modifier = modifier) {
        if (!shouldAnimate) {
            content()
            return@Box
        }

        val visibilityState = remember(messageId) {
            MutableTransitionState(false).apply { targetState = true }
        }
        val slideDistancePx = with(LocalDensity.current) { MessageSlideDistance.roundToPx() }
        AnimatedVisibility(
            visibleState = visibilityState,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = MESSAGE_SLIDE_DURATION_MILLIS,
                    easing = LinearOutSlowInEasing,
                ),
                initialOffsetY = { slideDistancePx },
            ) + fadeIn(
                // 슬라이드보다 짧게 가져가 다 올라오기 전에 내용이 먼저 보이게 한다.
                animationSpec = tween(
                    durationMillis = MESSAGE_FADE_DURATION_MILLIS,
                    easing = LinearOutSlowInEasing,
                ),
            ),
        ) {
            content()
        }
    }
}

/** 기존 메시지가 밀려나는 재배치를 [AnimatedChatMessage]와 같은 지속시간·이징으로 맞춘다. */
internal fun LazyItemScope.chatMessageItemAnimation(): Modifier = Modifier.animateItem(
    fadeInSpec = tween(MESSAGE_FADE_DURATION_MILLIS, easing = LinearOutSlowInEasing),
    placementSpec = tween(MESSAGE_SLIDE_DURATION_MILLIS, easing = LinearOutSlowInEasing),
    fadeOutSpec = tween(MESSAGE_FADE_DURATION_MILLIS, easing = LinearOutSlowInEasing),
)

// 아직 배치되지 않은 아이템은 실제 위치를 알 수 없어, 항상 같은 궤적이 되도록 고정 거리를 쓴다.
private val MessageSlideDistance = 180.dp
private const val MESSAGE_SLIDE_DURATION_MILLIS = 420
private const val MESSAGE_FADE_DURATION_MILLIS = 260
