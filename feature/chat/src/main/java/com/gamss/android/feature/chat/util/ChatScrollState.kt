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
 * - 내가 메시지를 보내면(전송 완료 시점) 항상 바닥까지 스크롤한다. 사용자의 명시적 행동이라
 *   상대 메시지 도착 시와 달리 자동 스크롤을 유지한다.
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

    // 내 전송 완료로 인한 자동 스크롤이 방금 반영한 메시지 id. handleNewMessage 가 같은 메시지를
    // 또 새 메시지로 처리해 토스트를 잠깐 띄웠다 지우는 걸 막는다.
    private var lastAutoScrolledMessageId: Long? = null

    // handleSendingChanged 가 true→false 전환만 골라내는 데 쓰는 직전 값.
    private var wasSending = false

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
        coroutineScope.launch { listState.scrollToItem(index) }
    }

    suspend fun handleInitialLoad(state: ChatRoomState) {
        if (hasScrolledToInitialBottom || state.conversationId == null || state.isLoading) return
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.lastItemIndex)
        }
        lastSeenMessageId = state.messages.lastOrNull()?.id
        hasScrolledToInitialBottom = true
    }

    /** [state]의 isSending 값이 바뀔 때마다 호출한다. true→false 전환일 때만 전송 완료 처리를 한다. */
    suspend fun handleSendingChanged(state: ChatRoomState) {
        val isSending = state.isSending
        if (wasSending && !isSending) {
            handleSendConcluded(state)
        }
        wasSending = isSending
    }

    // 스크롤(애니메이션이라 시간이 걸림)보다 커서 갱신이 먼저 반영돼야, 그 사이 handleNewMessage
    // 가 같은 메시지를 놓치고 토스트를 잠깐 띄우는 경합을 막을 수 있다.
    private suspend fun handleSendConcluded(state: ChatRoomState) {
        if (!hasScrolledToInitialBottom) return
        lastAutoScrolledMessageId = state.messages.lastOrNull()?.id
        newMessageToast = null
        val index = state.lastItemIndex
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    /** 상대 메시지가 하나씩 도착할 때마다(코멘트 순차 공개 포함) 호출된다. */
    fun handleNewMessage(state: ChatRoomState) {
        if (!hasScrolledToInitialBottom) return
        val latest = state.messages.lastOrNull()
        if (latest == null || latest.id == lastSeenMessageId) return

        // 새 메시지가 추가돼도 그 이전 메시지들의 화면상 위치는 바뀌지 않는다 — 그래서 이 메시지가
        // 새 메시지를 반영한 레이아웃 이후에 확인해도, "직전 마지막 메시지가 보이고 있었는지"는
        // 곧 "도착 직전에 바닥을 보고 있었는지"와 같은 뜻이다.
        val wasAtBottom = lastSeenMessageId?.let(::isMessageVisible) ?: true
        lastSeenMessageId = latest.id

        // 내 전송으로 이미 자동 스크롤된 메시지거나 내가 보낸 메시지면 토스트/자동 스크롤 대상이
        // 아니다 — 기존 토스트가 있다면 건드리지 않고 그대로 둔다.
        val isToastCandidate = latest.id != lastAutoScrolledMessageId && latest.sender != MessageSender.User
        if (isToastCandidate && wasAtBottom) {
            // 이미 바닥을 보고 있었다면 새 메시지를 놓치지 않도록 그대로 따라 내려간다.
            newMessageToast = null
            scrollToBottom(state)
        } else if (isToastCandidate) {
            // 도착한 시점에 이미 화면에 보이는 메시지라면(뷰포트에 여유가 있어 스크롤 없이도
            // 보이는 경우) 안내할 필요가 없다.
            newMessageToast = if (isMessageVisible(latest.id)) null else latest
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

    LaunchedEffect(state.isSending) {
        scrollState.handleSendingChanged(state)
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
