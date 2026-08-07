package com.gamss.android.feature.chatSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.model.SessionExpiredException
import org.orbitmvi.orbit.compose.collectAsState

@Composable
fun SearchChattingScreen(
    viewModel: SearchChattingViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val chattingRooms = viewModel.chattingRooms.collectAsLazyPagingItems()

    SearchChattingContent(
        state = state,
        chattingRooms = chattingRooms,
        onKeywordChanged = viewModel::onKeywordChanged,
        onSearch = viewModel::search,
    )
}

@Composable
fun SearchChattingContent(
    state: SearchChattingState,
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    onKeywordChanged: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.searchGeneration) {
        if (state.hasSearched) listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            SearchTopBar(
                state = state,
                onKeywordChanged = onKeywordChanged,
                onSearch = onSearch,
            )
        },
    ) { innerPadding ->
        SearchResultContent(
            state = state,
            chattingRooms = chattingRooms,
            listState = listState,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun SearchTopBar(
    state: SearchChattingState,
    onKeywordChanged: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "채팅방 검색",
            style = MaterialTheme.typography.titleLarge,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.keyword,
                onValueChange = onKeywordChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("검색어") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
            Button(
                onClick = onSearch,
                enabled = state.canSearch,
            ) {
                Text("검색")
            }
        }
    }
}

@Composable
private fun SearchResultContent(
    state: SearchChattingState,
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    listState: LazyListState,
    contentPadding: PaddingValues,
) {
    if (!state.hasSearched) {
        EmptyContent(contentPadding)
        return
    }

    when (val refresh = chattingRooms.loadState.refresh) {
        is LoadState.Loading -> LoadingContent(contentPadding)
        is LoadState.Error -> ErrorContent(
            message = refresh.error.toSearchFailureMessage(),
            contentPadding = contentPadding,
            onRetry = chattingRooms::retry,
        )

        is LoadState.NotLoading -> if (chattingRooms.itemCount == 0) {
            EmptyContent(contentPadding)
        } else {
            SearchResultList(
                chattingRooms = chattingRooms,
                listState = listState,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun SearchResultList(
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    listState: LazyListState,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(
            count = chattingRooms.itemCount,
            key = chattingRooms.itemKey { it.conversationId },
        ) { index ->
            chattingRooms[index]?.let { room ->
                ListItem(
                    headlineContent = { Text(room.title) },
                    supportingContent = { Text("${room.status} · ${room.createdAt}") },
                )
                HorizontalDivider()
            }
        }

        when (val append = chattingRooms.loadState.append) {
            is LoadState.Loading -> item { AppendLoadingItem() }
            is LoadState.Error -> item {
                AppendErrorItem(
                    message = append.error.toSearchFailureMessage(),
                    onRetry = chattingRooms::retry,
                )
            }

            is LoadState.NotLoading -> Unit
        }
    }
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
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message)
        Button(onClick = onRetry) {
            Text("재시도")
        }
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "대화방 데이터가 없어요",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message)
        Button(onClick = onRetry) {
            Text("재시도")
        }
    }
}

private fun Throwable.toSearchFailureMessage(): String = when (this) {
    is SessionExpiredException -> "세션이 만료되었어요. 다시 로그인해 주세요"
    is ApiException.Network -> "네트워크 연결을 확인해 주세요"
    is ApiException.Http -> message ?: "채팅방 검색에 실패했어요"
    else -> "채팅방 검색에 실패했어요"
}
