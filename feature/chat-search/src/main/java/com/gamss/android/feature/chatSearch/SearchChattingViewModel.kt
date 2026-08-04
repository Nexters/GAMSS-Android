package com.gamss.android.feature.chatSearch

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.chat.ChattingRoomSearchQuery
import com.gamss.android.domain.chat.SearchChattingRoomsUseCase
import com.gamss.android.domain.model.SessionExpiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SearchChattingViewModel @Inject constructor(
    private val searchChattingRooms: SearchChattingRoomsUseCase,
) : ViewModel(), ContainerHost<SearchChattingState, SearchChattingSideEffect> {

    override val container = container<SearchChattingState, SearchChattingSideEffect>(
        SearchChattingState(),
    )

    fun onKeywordChanged(keyword: String) = intent {
        reduce { state.copy(keyword = keyword) }
    }

    fun search() = intent {
        val keyword = state.keyword.trim()
        if (keyword.isEmpty() || state.isLoading) return@intent

        reduce {
            state.copy(
                keyword = keyword,
                rooms = emptyList(),
                isLoading = true,
                isAppending = false,
                hasSearched = true,
                page = ChattingRoomSearchQuery.DEFAULT_PAGE,
                canLoadMore = false,
            )
        }

        when (val result = searchChattingRooms(ChattingRoomSearchQuery(keyword = keyword))) {
            is AppResult.Success -> {
                val data = result.data
                reduce {
                    state.copy(
                        rooms = data.rooms,
                        isLoading = false,
                        page = data.page,
                        canLoadMore = data.hasNextPage,
                    )
                }
            }

            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(SearchChattingSideEffect.ShowToast(result.throwable.toSearchFailureMessage()))
            }
        }
    }

    fun loadNextPage() = intent {
        if (!state.canLoadMore || state.isLoading || state.isAppending) return@intent

        val nextPage = state.page + 1
        reduce { state.copy(isAppending = true) }

        when (
            val result = searchChattingRooms(
                ChattingRoomSearchQuery(
                    keyword = state.keyword,
                    page = nextPage,
                ),
            )
        ) {
            is AppResult.Success -> {
                val data = result.data
                reduce {
                    state.copy(
                        rooms = state.rooms + data.rooms,
                        isAppending = false,
                        page = data.page,
                        canLoadMore = data.hasNextPage,
                    )
                }
            }

            is AppResult.Failure -> {
                reduce { state.copy(isAppending = false) }
                postSideEffect(SearchChattingSideEffect.ShowToast(result.throwable.toSearchFailureMessage()))
            }
        }
    }

    private fun Throwable.toSearchFailureMessage(): String = when (this) {
        is SessionExpiredException -> "세션이 만료되었어요. 다시 로그인해 주세요"
        is ApiException.Network -> "네트워크 연결을 확인해 주세요"
        is ApiException.Http -> message ?: "채팅방 검색에 실패했어요"
        else -> "채팅방 검색에 실패했어요"
    }
}
