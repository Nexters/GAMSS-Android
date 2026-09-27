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
 * 진입시 표시되는 메시지는 그대로 표시하고, 그 이후 새로 나타난 메시지만 진입 애니메이션
 * 대상으로 관리한다.
 *
 * LazyColumn의 각 아이템은 부모 컴포저블과 별도로, 화면에 실제로 배치될 때(레이아웃 단계)
 * 지연 컴포지션된다. 부모의 SideEffect는 그보다 먼저(컴포지션 단계) 실행되므로, "부모가
 * SideEffect로 표시 여부를 기록하고 자식이 그 값을 읽는" 예전 방식은 부모가 항상 먼저 기록을
 * 끝내버려 자식이 "이미 표시된 메시지"로 잘못 판정하는 경쟁 상태가 있었다(신규 메시지가
 * 한 번도 애니메이션되지 않던 원인). [claimShouldAnimate]는 아이템 자신의 첫 컴포지션
 * 시점(=remember(messageId) 블록 안)에서 판정과 기록을 원자적으로 함께 수행해 이 경쟁을
 * 없앤다.
 */
internal class ChatMessageAnimation {
    private var hasCapturedInitial = false
    private val seenMessageIds = mutableSetOf<Long>()

    /** conversationId/isLoading가 확정된 이후 딱 한 번만 true로 굳는다(멱등이라 여러 번 불러도 안전). */
    fun markInitialLoadComplete() {
        hasCapturedInitial = true
    }

    /**
     * 이 messageId가 이번 화면 생애주기에서 처음 컴포지션되는 것인지 판정과 동시에 기록한다.
     * [AnimatedChatMessage]의 remember(messageId) 블록 안에서 아이템당 정확히 한 번만
     * 호출돼야 한다.
     */
    fun claimShouldAnimate(messageId: Long): Boolean {
        val isNew = hasCapturedInitial && messageId !in seenMessageIds
        seenMessageIds += messageId
        return isNew
    }
}

@Composable
internal fun rememberChatMessageAnimationState(
    conversationId: Long?,
    isLoading: Boolean,
): ChatMessageAnimation {
    val animationState = remember(conversationId) { ChatMessageAnimation() }
    if (conversationId != null && !isLoading) {
        animationState.markInitialLoadComplete()
    }
    return animationState
}

/**
 * 아래(입력창 쪽)에서 카카오톡처럼 밀려 올라오며 나타난다. 레이아웃에 아직 배치되지 않은
 * 신규 아이템의 실제 위치를 알 수 없어 거리를 추정하면 대부분 자기 높이만큼만 튀어 오르는
 * 것처럼 보이므로, [MessageSlideDistance]로 고정해 항상 일관되게 올라오게 한다.
 *
 * [modifier]로 호출부(LazyColumn)가 `Modifier.animateItem()`을 넘긴다 — animateItem()(아이템
 * 슬롯 재배치)과 여기 AnimatedVisibility(이 콘텐츠만의 등장 연출)를 같은 노드에 얹으면 서로의
 * 크기·투명도 측정이 얽힌다. 바깥 Box에 modifier(animateItem)를 두고, 안쪽 AnimatedVisibility는
 * 자기 모디파이어 없이 콘텐츠 등장만 전담하도록 분리해야 두 움직임이 같이 자연스럽다.
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
                // 페이드는 슬라이드보다 짧게 가져가, 다 올라오기 전에 내용이 먼저 읽히게 한다.
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

/**
 * 새 아이템이 끼어들며 기존 메시지들이 위로 밀려나는 재배치·페이드를 [AnimatedChatMessage]의
 * 슬라이드/페이드와 같은 지속시간·이징으로 맞춘다. 한쪽만 다른 커브(예: 기본 spring)를 쓰면
 * 같은 순간 시작해도 서로 다르게 끝나 두 움직임이 따로 노는 것처럼 보인다.
 */
internal fun LazyItemScope.chatMessageItemAnimation(): Modifier = Modifier.animateItem(
    fadeInSpec = tween(MESSAGE_FADE_DURATION_MILLIS, easing = LinearOutSlowInEasing),
    placementSpec = tween(MESSAGE_SLIDE_DURATION_MILLIS, easing = LinearOutSlowInEasing),
    fadeOutSpec = tween(MESSAGE_FADE_DURATION_MILLIS, easing = LinearOutSlowInEasing),
)

// 입력창의 실제 좌표까지 추적하는 대신, "말풍선이 아래에서 날아 올라오는" 느낌이 나도록 충분히
// 큰 고정 거리를 쓴다 — 화면/스크롤 상태와 무관하게 항상 같은 궤적으로 움직여 일관되다.
private val MessageSlideDistance = 180.dp
private const val MESSAGE_SLIDE_DURATION_MILLIS = 420
private const val MESSAGE_FADE_DURATION_MILLIS = 260
