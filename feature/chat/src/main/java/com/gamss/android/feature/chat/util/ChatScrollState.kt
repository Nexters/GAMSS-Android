package com.gamss.android.feature.chat.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * - 내가 메시지를 보내면(전송 완료 시점) 항상 바닥까지 스크롤한다. 사용자의 명시적 행동이라
 *   상대 메시지 도착 시와 달리 자동 스크롤을 유지한다.
 * - 상대 메시지는 자동 스크롤하지 않는다. 바닥이 아닌 상태에서 도착하면 [newMessageToast]를
 *   채워 화면에서 안내하고, 이미 바닥이면 무시한다.
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

    // 내 전송 완료로 인한 자동 스크롤이 방금 반영한 메시지 id. handleNewMessage 가 같은 메시지를
    // 또 새 메시지로 처리해 토스트를 잠깐 띄웠다 지우는 걸 막는다.
    private var lastAutoScrolledMessageId: Long? = null

    val showScrollToBottomButton: Boolean
        get() = listState.canScrollForward && newMessageToast == null

    fun dismissToastAndScrollToBottom(state: ChatRoomState) {
        newMessageToast = null
        scrollToBottom(state)
    }

    fun scrollToBottom(state: ChatRoomState) {
        val index = state.lastItemIndex
        if (index < 0) return
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

    // 스크롤(애니메이션이라 시간이 걸림)보다 커서 갱신이 먼저 반영돼야, 그 사이 handleNewMessage
    // 가 같은 메시지를 놓치고 토스트를 잠깐 띄우는 경합을 막을 수 있다.
    suspend fun handleSendConcluded(state: ChatRoomState) {
        if (!hasScrolledToInitialBottom) return
        lastAutoScrolledMessageId = state.messages.lastOrNull()?.id
        newMessageToast = null
        val index = state.lastItemIndex
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    /** 상대 메시지가 하나씩 도착할 때마다(코멘트 순차 공개 포함) 호출된다. */
    fun handleNewMessage(state: ChatRoomState) {
        if (!hasScrolledToInitialBottom) return
        val latest = state.messages.lastOrNull()
        if (latest == null || latest.id == lastSeenMessageId) return
        lastSeenMessageId = latest.id

        // 내 전송으로 이미 자동 스크롤된 메시지거나 내가 보낸 메시지면 토스트 대상이 아니다 —
        // 기존 토스트가 있다면 건드리지 않고 그대로 둔다.
        val isToastCandidate = latest.id != lastAutoScrolledMessageId && latest.sender != MessageSender.User
        if (isToastCandidate) {
            newMessageToast = if (listState.canScrollForward) latest else null
        }
    }

    fun clearToastIfAtBottom() {
        if (!listState.canScrollForward) newMessageToast = null
    }
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

    var wasSending by remember(state.conversationId) { mutableStateOf(false) }
    LaunchedEffect(state.isSending) {
        if (wasSending && !state.isSending) {
            scrollState.handleSendConcluded(state)
        }
        wasSending = state.isSending
    }

    LaunchedEffect(state.messages.lastOrNull()?.id, scrollState.hasScrolledToInitialBottom) {
        scrollState.handleNewMessage(state)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollForward }.collect { scrollState.clearToastIfAtBottom() }
    }

    return scrollState
}
