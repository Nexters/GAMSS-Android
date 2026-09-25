package com.gamss.android.feature.chat.component

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.modifier.noRippleCombinedClickable
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.feature.chat.ChattingListActions
import com.gamss.android.feature.chat.ChattingListState
import com.gamss.android.feature.chat.R
import com.gamss.android.feature.chat.SearchFailureReason
import com.gamss.android.feature.chat.toSearchFailureReason

/**
 * 검색 모드 화면. 검색어 입력창과 검색 결과만 그린다.
 *
 * 진행 중인 대화 목록은 여기서 그리지 않는다. 검색 모드에 들어온 직후에는 검색어가 없으므로
 * 결과 영역을 비워 두고, 검색을 실행한 뒤에만 결과를 보여준다.
 */
@Composable
internal fun ChattingSearchContent(
    state: ChattingListState,
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    actions: ChattingListActions,
    onKeywordChanged: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var scrolledGeneration by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(state.search.searchGeneration) {
        if (state.search.hasSearched && state.search.searchGeneration != scrolledGeneration) {
            listState.scrollToItem(0)
            scrolledGeneration = state.search.searchGeneration
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchInput(
            keyword = state.search.keyword,
            onKeywordChanged = onKeywordChanged,
            onSearch = onSearch,
            onCancel = onCancel,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (state.search.hasSearched) {
                SearchResultContent(
                    chattingRooms = chattingRooms,
                    state = state,
                    actions = actions,
                    listState = listState,
                    onRetry = onSearch,
                )
            }
        }
    }
}

@Composable
private fun SearchInput(
    keyword: TextFieldValue,
    onKeywordChanged: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(start = 18.dp, end = 18.dp, bottom = 12.dp)),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatListSearchInputField(
            keyword = keyword,
            onKeywordChanged = onKeywordChanged,
            onSearch = onSearch,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        Text(
            text = stringResource(R.string.chatting_list_search_cancel),
            style = GamssTheme.typography.body4Medium,
            color = GamssTheme.colors.gray950,
            modifier = Modifier.noRippleCombinedClickable(
                onClick = {
                    keyboardController?.hide()
                    onCancel()
                },
                onLongClick = null
            ),
        )
    }
}

@Composable
private fun SearchResultContent(
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    state: ChattingListState,
    actions: ChattingListActions,
    listState: LazyListState,
    onRetry: () -> Unit,
) {
    when (val refresh = chattingRooms.loadState.refresh) {
        is LoadState.Loading -> LoadingContent()
        is LoadState.Error -> ErrorContent(
            messageRes = refresh.error.toSearchFailureReason().messageRes,
            onRetry = onRetry,
        )

        is LoadState.NotLoading -> if (chattingRooms.itemCount == 0) {
            SearchEmptyResult()
        } else {
            SearchResultList(
                chattingRooms = chattingRooms,
                listState = listState,
                state = state,
                actions = actions,
            )
        }
    }
}

@Composable
private fun SearchEmptyResult() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.image_search_result_empty),
                contentDescription = stringResource(R.string.chatting_list_search_empty),
                contentScale = ContentScale.Fit,
            )
            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = stringResource(R.string.chatting_list_search_empty),
                style = GamssTheme.typography.subtitle2.copy(color = GamssTheme.colors.gray900)
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.chatting_list_search_empty_subTitle),
                style = GamssTheme.typography.body4Regular.copy(color = GamssTheme.colors.gray600)
            )
        }
    }
}

@Preview(name = "Search Empty", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun SearchEmptyResultPreview() {
    GamssTheme(darkTheme = false) {
        SearchEmptyResult()
    }
}

@Composable
private fun SearchResultList(
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    listState: LazyListState,
    state: ChattingListState,
    actions: ChattingListActions,
) {
    ConversationList(
        chattingRooms = chattingRooms,
        state = state,
        actions = actions,
        listState = listState,
        modifier = Modifier.fillMaxSize(),
        appendContent = {
            when (val append = chattingRooms.loadState.append) {
                is LoadState.Loading -> item { AppendLoadingItem() }
                is LoadState.Error -> item {
                    AppendErrorItem(
                        messageRes = append.error.toSearchFailureReason().messageRes,
                        onRetry = chattingRooms::retry,
                    )
                }

                is LoadState.NotLoading -> Unit
            }
        },
    )
}

@Composable
private fun AppendLoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppendErrorItem(
    @StringRes messageRes: Int,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(messageRes))
        GamssButton(
            label = stringResource(R.string.chatting_list_search_retry),
            onClick = onRetry
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    @StringRes messageRes: Int,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(messageRes))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.chatting_list_search_retry))
        }
    }
}

private val SearchFailureReason.messageRes: Int
    @StringRes get() = when (this) {
        SearchFailureReason.NETWORK -> R.string.chatting_list_search_network_error
        SearchFailureReason.INVALID_INPUT -> R.string.chatting_list_search_invalid_input
        SearchFailureReason.UNKNOWN -> R.string.chatting_list_search_unknown_error
    }
