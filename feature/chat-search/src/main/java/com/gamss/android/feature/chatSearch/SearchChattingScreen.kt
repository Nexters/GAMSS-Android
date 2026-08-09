package com.gamss.android.feature.chatSearch

import android.widget.Toast
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.gamss.android.domain.chattingsearch.ChattingRoomSummary
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SearchChattingScreen(
    viewModel: SearchChattingViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val chattingRooms = viewModel.chattingRooms.collectAsLazyPagingItems()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is SearchChattingSideEffect.SearchFailure ->
                Toast.makeText(context, sideEffect.reason.toMessage(), Toast.LENGTH_SHORT).show()
        }
    }

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

    // 화면 회전 등 구성 변경으로 Composition이 다시 만들어져도 이미 처리한 검색에 대해서는
    // 스크롤을 맨 위로 되돌리지 않도록, 마지막으로 스크롤을 적용한 generation을 별도로 기억한다.
    var scrolledGeneration by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(state.searchGeneration) {
        if (state.hasSearched && state.searchGeneration != scrolledGeneration) {
            listState.scrollToItem(0)
            scrolledGeneration = state.searchGeneration
        }
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
        MessageContent(message = "검색어를 입력해 채팅방을 찾아보세요", contentPadding = contentPadding)
        return
    }

    when (chattingRooms.loadState.refresh) {
        is LoadState.Loading -> LoadingContent(contentPadding)
        is LoadState.Error -> ErrorContent(
            message = SearchFailureReason.UNKNOWN.toMessage(),
            contentPadding = contentPadding,
            onRetry = chattingRooms::retry,
        )

        is LoadState.NotLoading -> if (chattingRooms.itemCount == 0) {
            MessageContent(message = "검색 결과가 없어요", contentPadding = contentPadding)
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

        when (chattingRooms.loadState.append) {
            is LoadState.Loading -> item { AppendLoadingItem() }
            is LoadState.Error -> item {
                AppendErrorItem(
                    message = SearchFailureReason.UNKNOWN.toMessage(),
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
private fun MessageContent(message: String, contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
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

private fun SearchFailureReason.toMessage(): String =
    when (this) {
        SearchFailureReason.NETWORK -> "네트워크 연결을 확인해 주세요"
        SearchFailureReason.INVALID_INPUT -> "최소 2글자 이상 입력해주세요"
        SearchFailureReason.UNKNOWN -> "채팅방 검색에 실패했어요"
    }
