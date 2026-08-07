package com.gamss.android.feature.chatSearch

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gamss.android.domain.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.chattingsearch.SearchChattingRoomsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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

    private val searchRequests = MutableStateFlow<SearchRequest?>(null)

    val chattingRooms: Flow<PagingData<ChattingRoomSummary>> = searchRequests
        .filterNotNull()
        .flatMapLatest { request -> searchChattingRoomsUseCase(request.keyword) }
        .cachedIn(viewModelScope)

    fun onKeywordChanged(keyword: TextFieldValue) = intent {
        reduce { state.copy(keyword = keyword) }
    }

    fun search() = intent {
        val keyword = state.keyword.text.trim()
        if (keyword.length < MIN_SEARCH_KEYWORD_LENGTH) return@intent

        val nextGeneration = state.searchGeneration + 1
        reduce {
            state.copy(
                keyword = TextFieldValue(
                    text = keyword,
                    selection = TextRange(keyword.length),
                ),
                hasSearched = true,
                searchGeneration = nextGeneration,
            )
        }
        searchRequests.value = SearchRequest(keyword, nextGeneration)
    }

    private data class SearchRequest(
        val keyword: String,
        val generation: Long,
    )
}
