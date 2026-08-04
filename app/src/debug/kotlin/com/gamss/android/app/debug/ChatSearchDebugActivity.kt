package com.gamss.android.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.gamss.android.app.ui.theme.GamssTheme
import com.gamss.android.data.remote.chattingRoomSearch.model.response.ChattingRoomSearchResponse
import com.gamss.android.domain.chat.ChattingRoomSearch
import com.gamss.android.domain.chat.ChattingRoomSummary
import com.gamss.android.feature.chatSearch.SearchChattingContent
import com.gamss.android.feature.chatSearch.SearchChattingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 채팅방 검색 디버그/검증 전용 화면. 프로덕션 아님.
 * app/src/debug/assets/chat_search_dummy_pages.json 을 사용해 수동 pagination 동작을 확인한다.
 */
class ChatSearchDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dummyPages = assets.open(DUMMY_ASSET_NAME).use { inputStream ->
            json.decodeFromString<ChatSearchDummyPages>(
                inputStream.bufferedReader().readText(),
            ).pages.map { it.toDomain() }
        }

        setContent {
            GamssTheme {
                val coroutineScope = rememberCoroutineScope()
                var state by remember {
                    mutableStateOf(SearchChattingState(keyword = DEFAULT_KEYWORD))
                }

                fun search() {
                    val keyword = state.keyword.trim()
                    if (keyword.isEmpty() || state.isLoading) return

                    coroutineScope.launch {
                        state = state.copy(
                            keyword = keyword,
                            rooms = emptyList(),
                            isLoading = true,
                            isAppending = false,
                            hasSearched = true,
                            page = 0,
                            canLoadMore = false,
                        )
                        delay(DEBUG_NETWORK_DELAY_MS)

                        val firstPage = dummyPages.firstOrNull()
                        state = state.copy(
                            rooms = firstPage?.rooms.orEmpty(),
                            isLoading = false,
                            page = firstPage?.page ?: 0,
                            canLoadMore = firstPage?.hasNextPage ?: false,
                        )
                    }
                }

                fun loadNextPage() {
                    if (!state.canLoadMore || state.isLoading || state.isAppending) return

                    coroutineScope.launch {
                        val nextPage = state.page + 1
                        state = state.copy(isAppending = true)
                        delay(DEBUG_NETWORK_DELAY_MS)

                        val page = dummyPages.firstOrNull { it.page == nextPage }
                        state = state.copy(
                            rooms = state.rooms + page?.rooms.orEmpty(),
                            isAppending = false,
                            page = page?.page ?: state.page,
                            canLoadMore = page?.hasNextPage ?: false,
                        )
                    }
                }

                SearchChattingContent(
                    state = state,
                    onKeywordChanged = { state = state.copy(keyword = it) },
                    onSearch = ::search,
                    onLoadNextPage = ::loadNextPage,
                )
            }
        }
    }

    private fun ChattingRoomSearchResponse.toDomain(): ChattingRoomSearch =
        ChattingRoomSearch(
            rooms = content.map {
                ChattingRoomSummary(
                    conversationId = it.conversationId,
                    title = it.title,
                    status = it.status,
                    createdAt = it.createdAt,
                )
            },
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        )

    @Serializable
    private data class ChatSearchDummyPages(
        val pages: List<ChattingRoomSearchResponse>,
    )

    private companion object {
        const val DUMMY_ASSET_NAME = "chat_search_dummy_pages.json"
        const val DEFAULT_KEYWORD = "감정"
        const val DEBUG_NETWORK_DELAY_MS = 500L

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}
