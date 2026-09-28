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
 * - 최초 진입 시 한 번 바닥으로 점프한다.
 * - 내가 보낸 메시지는 말풍선이 그려지는 즉시 항상 바닥까지 스크롤한다.
 * - 답장 로딩 표시가 나타나거나 실제 글자로 바뀌거나 상대 메시지가 도착하면, 직전에 바닥을 보고
 *   있었을 때만 따라 내려간다. 위를 보고 있었다면 도착한 메시지를 [newMessageToast]로 안내하고,
 *   그 메시지가 화면에 들어오면 지운다.
 *
 * 메시지는 [ChatRoomState.listKeyOf] 기준으로 구분한다. 목록 key와 같아야 가시성 판정이 맞고,
 * 내 메시지가 임시 id → 서버 id로 바뀌어도 새 메시지로 보지 않는다.
 */
internal class ChatScrollState(
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope,
) {
    var newMessageToast by mutableStateOf<Message?>(null)
        private set

    var hasScrolledToInitialBottom by mutableStateOf(false)
        private set

    private var lastSeenItem: BottomItem? = null

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
        // 부드러운 스크롤은 목록 전체를 매 프레임 다시 배치해, 등장 애니메이션과 겹치면 프레임이 밀렸다
        // (벤치마크 트레이스). 한 번에 점프하고 움직임은 새 말풍선의 등장 애니메이션에 맡긴다.
        coroutineScope.launch { listState.scrollToItem(index) }
    }

    suspend fun handleInitialLoad(state: ChatRoomState) {
        if (hasScrolledToInitialBottom || state.conversationId == null || state.isLoading) return
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.lastItemIndex)
        }
        lastSeenItem = state.bottomItem
        hasScrolledToInitialBottom = true
    }

    /**
     * 목록의 마지막 아이템이 바뀔 때마다 호출된다 — 내가 보냈을 때, 답장 로딩 표시가 나타났을 때,
     * 그 로딩이 실제 답장으로 공개됐을 때.
     */
    fun handleNewMessage(state: ChatRoomState) {
        if (!hasScrolledToInitialBottom) return
        val latest = state.bottomItem ?: return
        if (latest == lastSeenItem) return

        // 새 메시지가 추가돼도 그 이전 메시지들의 화면상 위치는 바뀌지 않는다 — 그래서 이 메시지가
        // 새 메시지를 반영한 레이아웃 이후에 확인해도, "직전 마지막 메시지가 보이고 있었는지"는
        // 곧 "도착 직전에 바닥을 보고 있었는지"와 같은 뜻이다.
        val wasAtBottom = lastSeenItem?.key?.let(::isMessageVisible) ?: true
        lastSeenItem = latest

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
            // 로딩 표시는 안내할 메시지가 아니다.
            latest.isLoading -> Unit
            // 도착한 시점에 이미 화면에 보이는 메시지라면(뷰포트에 여유가 있어 스크롤 없이도
            // 보이는 경우) 안내할 필요가 없다.
            else -> newMessageToast = if (isMessageVisible(latest.key)) null else state.messages.last()
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

/**
 * 목록 맨 아래 아이템. 로딩 표시와 공개된 답장은 key가 같으므로 [isLoading]까지 비교해야
 * 로딩 → 실제 글자로 바뀌는 순간도 새 아이템으로 잡힌다.
 */
private data class BottomItem(val key: Long, val isLoading: Boolean, val sender: MessageSender)

private val ChatRoomState.bottomItem: BottomItem?
    get() = displayMessages.lastOrNull()?.let {
        BottomItem(listKeyOf(it), isLoading = it.id == loadingPlaceholder?.id, it.sender)
    }

private val ChatRoomState.lastItemIndex: Int
    get() = displayMessages.lastIndex

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

    LaunchedEffect(state.bottomItem, scrollState.hasScrolledToInitialBottom) {
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
