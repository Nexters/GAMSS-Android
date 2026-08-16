package com.gamss.android.feature.chatSearch

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSearchException
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.conversation.chattingsearch.SearchChattingRoomsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SearchChattingViewModel @Inject constructor(
    private val searchChattingRoomsUseCase: SearchChattingRoomsUseCase,
) : ViewModel(), ContainerHost<SearchChattingState, SearchChattingSideEffect> {

    override val container = container<SearchChattingState, SearchChattingSideEffect>(
        SearchChattingState(),
    )

    private val searchResults = MutableStateFlow<Flow<PagingData<ChattingRoomSummary>>?>(null)

    val chattingRooms: Flow<PagingData<ChattingRoomSummary>> = searchResults
        .filterNotNull()
        .flatMapLatest { it }
        .cachedIn(viewModelScope)

    fun onKeywordChanged(keyword: TextFieldValue) = intent {
        reduce { state.copy(keyword = keyword) }
    }

    fun resetSearch() = intent {
        searchResults.value = null
        reduce { SearchChattingState() }
    }

    fun search() = intent {
        val keyword = state.keyword.text.trim()
        val nextGeneration = state.searchGeneration + 1

        when (val result = searchChattingRoomsUseCase(keyword)) {
            is AppResult.Success -> {
                reduce {
                    state.copy(
                        hasSearched = true,
                        searchGeneration = nextGeneration,
                    )
                }
                searchResults.value = result.data
            }

            is AppResult.Failure -> {
                searchResults.value = flowOf(PagingData.empty<ChattingRoomSummary>())
                reduce {
                    state.copy(
                        hasSearched = false
                    )
                }
                postSideEffect(
                    SearchChattingSideEffect.SearchFailure(result.throwable.toSearchFailureReason()),
                )
            }
        }
    }
}

internal fun Throwable.toSearchFailureReason(): SearchFailureReason = when (this) {
    is ChattingRoomSearchException.InvalidKeyword -> SearchFailureReason.INVALID_INPUT
    is ApiException.Network -> SearchFailureReason.NETWORK
    else -> SearchFailureReason.UNKNOWN
}
