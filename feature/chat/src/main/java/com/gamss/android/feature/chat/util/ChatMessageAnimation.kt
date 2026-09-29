package com.gamss.android.feature.chat.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 진입 시 이미 있던 메시지는 그대로 두고, 그 이후 새로 나타난 메시지만 진입 애니메이션 대상으로 삼는다.
 *
 * LazyColumn 아이템은 부모보다 늦게(레이아웃 단계에서) 컴포지션되므로, 화면에 보이지 않는 동안(스크롤을
 * 올려 과거를 보는 중) 도착한 메시지는 아직 한 번도 컴포지션되지 않은 상태다. 그런 메시지는
 * [markAlreadySeen]으로 도착 시점에 바로 "본 메시지" 처리해 둔다 — 그래야 나중에 스크롤해서 처음
 * 컴포지션될 때도 애니메이션 없이 바로 보인다. 화면에 보이는 동안 도착한 메시지는 아무 것도 하지 않고
 * 두면, 자신의 첫 컴포지션에서 [claimShouldAnimate]가 판정과 기록을 함께 한다.
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

    /**
     * 아직 컴포지션 전인 메시지를 "본 메시지"로 기록해 나중에 처음 그려져도 애니메이션되지 않게 한다.
     * 화면 밖에 도착한 메시지는 도착 순간을 놓쳤으므로, 스크롤로 뒤늦게 보일 때 새로 온 것처럼 움직이면 안 된다.
     */
    fun markSeen(messageIds: List<Long>) {
        seenMessageIds += messageIds
    }

    /**
     * 화면 밖에서 도착한 메시지들이다. 아직 한 번도 컴포지션되지 않았어도, 나중에 스크롤해서 처음
     * 보일 때 애니메이션이 재생되지 않도록 미리 "본 메시지"로 기록해 둔다.
     */
    fun markAlreadySeen(messageIds: Collection<Long>) {
        seenMessageIds += messageIds
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
 * 슬라이드·페이드를 graphicsLayer 하나에서 그리기 단계로만 처리한다 — 매 프레임 재배치가 없고,
 * [CompositingStrategy.ModulateAlpha]라 페이드 중에도 오프스크린 레이어를 만들지 않는다(트레이스상
 * 기본 전략의 오프스크린 합성이 GPU 부담이었다).
 *
 * 기존 메시지의 재배치(Modifier.animateItem)는 일부러 붙이지 않는다 — 새 메시지마다 보이는 아이템
 * 전부가 함께 움직여, 벤치마크에서 develop 대비 프레임 밀림이 약 2배로 늘었다.
 */
@Composable
internal fun AnimatedChatMessage(
    messageId: Long,
    animationState: ChatMessageAnimation,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shouldAnimate = remember(messageId) { animationState.claimShouldAnimate(messageId) }
    if (!shouldAnimate) {
        Box(modifier = modifier) { content() }
        return
    }

    // 경과 시간(0→1)만 선형으로 흘리고, 슬라이드·페이드는 각자 길이에 맞춰 이징을 적용한다.
    val elapsed = remember(messageId) { Animatable(0f) }
    LaunchedEffect(messageId) {
        elapsed.animateTo(1f, tween(MESSAGE_SLIDE_DURATION_MILLIS, easing = LinearEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            val slide = LinearOutSlowInEasing.transform(elapsed.value)
            // 슬라이드보다 짧게 가져가 다 올라오기 전에 내용이 먼저 보이게 한다.
            val fade = LinearOutSlowInEasing.transform((elapsed.value * FADE_TO_SLIDE_RATIO).coerceAtMost(1f))
            translationY = (1f - slide) * MessageSlideDistance.toPx()
            alpha = fade
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
    ) {
        content()
    }
}

// 아직 배치되지 않은 아이템은 실제 위치를 알 수 없어, 항상 같은 궤적이 되도록 고정 거리를 쓴다.
private val MessageSlideDistance = 180.dp
private const val MESSAGE_SLIDE_DURATION_MILLIS = 420
private const val MESSAGE_FADE_DURATION_MILLIS = 260
private const val FADE_TO_SLIDE_RATIO = MESSAGE_SLIDE_DURATION_MILLIS.toFloat() / MESSAGE_FADE_DURATION_MILLIS
