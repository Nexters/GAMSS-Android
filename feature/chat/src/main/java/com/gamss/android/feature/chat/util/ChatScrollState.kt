package com.gamss.android.feature.chat.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.feature.chat.ChatRoomState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 채팅 목록의 "바닥 유지" 동작을 관리한다.
 *
 * - 최초 진입(과거 메시지 로딩 완료) 시 한 번 바닥으로 점프한다.
 * - 내가 보낸 메시지는 목록에 나타나는 즉시(낙관적으로 그려지는 시점) 바닥까지 스크롤한다.
 *   사용자의 명시적 행동이라 성공/실패나 이전 스크롤 위치와 무관하게 항상 따라간다 — 전송
 *   완료(서버 응답)를 기다리지 않는다. 전송이 실패해도 말풍선의 id는 그대로 남아있으므로
 *   [handleNewMessage]가 다시 반응하지 않는다(재전송 때 새 id로 다시 붙으면 또 스크롤한다).
 * - 상대 메시지가 도착했을 때, 도착 직전에 이미 바닥을 보고 있었다면(=직전 마지막 메시지가
 *   화면에 보이고 있었다면) 그대로 따라가며 바닥까지 자동 스크롤한다. 이미 위로 스크롤해
 *   과거를 보고 있었다면 자동 스크롤하지 않고 [newMessageToast]를 채워 안내하며, 그 메시지가
 *   화면에 들어오면(사용자가 스크롤해서 직접 봤으면) 지운다.
 */
internal class ChatScrollState(
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope,
) {
    var newMessageToast by mutableStateOf<Message?>(null)
        private set

    var hasScrolledToInitialBottom by mutableStateOf(false)
        private set

    private var lastSeenMessageId: Long? = null

    val showScrollToBottomButton: Boolean by derivedStateOf {
        listState.canScrollForward && newMessageToast == null
    }

    fun dismissToastAndScrollToBottom(state: ChatRoomState) {
        newMessageToast = null
        scrollToBottom(state)
    }

    fun scrollToBottom(state: ChatRoomState) {
        val index = state.lastItemIndex
        if (index < 0) return
        // 새 아이템 자체의 등장(AnimatedChatMessage)·재배치(Modifier.animateItem())는 이미
        // 애니메이션이 붙어 있다 — 스크롤만 즉시 점프하면 그 사이에서 뚝 끊겨 보이므로 같이 부드럽게 따라간다.
        coroutineScope.launch { listState.animateScrollToItem(index) }
    }

    suspend fun handleInitialLoad(state: ChatRoomState) {
        if (hasScrolledToInitialBottom || state.conversationId == null || state.isLoading) return
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.lastItemIndex)
        }
        lastSeenMessageId = state.messages.lastOrNull()?.id
        hasScrolledToInitialBottom = true
    }

    /**
     * 목록의 마지막 메시지 id가 바뀔 때마다 호출된다 — 내가 보냈거나(낙관적으로 그려진 순간
     * 포함), 상대가 도착했거나, 코멘트가 순차 공개될 때마다.
     */
    fun handleNewMessage(state: ChatRoomState) {
        if (!hasScrolledToInitialBottom) return
        val latest = state.messages.lastOrNull()
        if (latest == null || latest.id == lastSeenMessageId) return

        // 새 메시지가 추가돼도 그 이전 메시지들의 화면상 위치는 바뀌지 않는다 — 그래서 이 메시지가
        // 새 메시지를 반영한 레이아웃 이후에 확인해도, "직전 마지막 메시지가 보이고 있었는지"는
        // 곧 "도착 직전에 바닥을 보고 있었는지"와 같은 뜻이다.
        val wasAtBottom = lastSeenMessageId?.let(::isMessageVisible) ?: true
        lastSeenMessageId = latest.id

        when {
            // 내가 보낸 메시지는 성공/실패나 직전 스크롤 위치와 무관하게 항상 따라간다.
            latest.sender == MessageSender.User -> {
                newMessageToast = null
                scrollToBottom(state)
            }
            // 상대 메시지 도착 직전에 이미 바닥을 보고 있었다면 놓치지 않도록 그대로 따라 내려간다.
            wasAtBottom -> {
                newMessageToast = null
                scrollToBottom(state)
            }
            // 도착한 시점에 이미 화면에 보이는 메시지라면(뷰포트에 여유가 있어 스크롤 없이도
            // 보이는 경우) 안내할 필요가 없다.
            else -> newMessageToast = if (isMessageVisible(latest.id)) null else latest
        }
    }

    /**
     * 화면에 보이는 아이템 목록이 바뀔 때마다(=스크롤할 때마다) 호출된다. 지금 토스트가
     * 가리키는 메시지가 화면에 들어왔으면 지운다. 리스트 맨 끝까지 스크롤하지 않아도, 그
     * 메시지 하나만 보이면 충분하다.
     */
    fun clearToastIfMessageVisible() {
        val toastMessageId = newMessageToast?.id ?: return
        if (isMessageVisible(toastMessageId)) newMessageToast = null
    }

    private fun isMessageVisible(messageId: Long): Boolean =
        listState.layoutInfo.visibleItemsInfo.any { it.key == messageId }
}

/** 가장 최근에 보이던 마지막 아이템의 인덱스. 코멘트 생성 표시(로딩)까지 바닥에 포함시킨다. */
private val ChatRoomState.lastItemIndex: Int
    get() = messages.size - 1 + if (isAwaitingComments) 1 else 0

@Composable
internal fun rememberChatScrollState(
    state: ChatRoomState,
    listState: LazyListState,
): ChatScrollState {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = remember(state.conversationId) { ChatScrollState(listState, coroutineScope) }

    LaunchedEffect(state.conversationId, state.isLoading) {
        scrollState.handleInitialLoad(state)
    }

    LaunchedEffect(state.messages.lastOrNull()?.id, scrollState.hasScrolledToInitialBottom) {
        scrollState.handleNewMessage(state)
    }

    LaunchedEffect(scrollState, listState) {
        // 보이는 아이템 "목록"이 아니라 보이는 범위의 양 끝 인덱스만 본다 — 스크롤 중이면 매
        // 프레임 바뀌는 값이라, visibleItemsInfo 전체를 새 List로 매핑하는 비용을 피한다. 목록은
        // 항상 연속된 범위라 양 끝이 그대로면 그 안의 가시성도 그대로다.
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            visible.firstOrNull()?.index to visible.lastOrNull()?.index
        }.collect { scrollState.clearToastIfMessageVisible() }
    }

    return scrollState
}
